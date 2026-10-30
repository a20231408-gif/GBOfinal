# Ensayo BASE incompleto: hallazgos y corrección del arnés

Fuente: los tres archivos reales localizados en la carpeta descomprimida de Descargas, leídos sin modificarlos; hashes conservados. No se ejecutó una consulta a Google por Codex. La declaración `inflight=0` y ejecuciones Completada procede del propietario, no de una nueva comprobación remota.

## Confirmado con la evidencia

- 119 eventos: **59 OK, 52 BUSY y 8 TRANSPORT_FAILED**. Los ocho padres están en avance; no hay legacyEdit, blankEdit ni VIDEO confirmados en ese avance.
- El evento 118, testSnapshot, terminó OK. El 119, LOCAL_COPY_DUPLICATE_JOB, terminó OK a los **88487,7 ms** y es el único OK sin métricas registradas.
- El resultado termina INCOMPLETE y no contiene `failure`, stack ni el ACK del 119. El ejecutor guardaba solo ok/code/tiempo/métricas, no los campos decisivos de ese ACK.
- Resultado y JSONL coinciden salvo dos marcas `responseDiscardedByClient` que el ejecutor anterior añadía al objeto en memoria después de escribir su línea. No hay otras diferencias; esa divergencia de marcas no explica la parada.
- Los ocho ACK de avance pasan comprobación local de contrato, identidad, revisión y hash; sus payloads pasan canonical y firma de fixtures originales. **Esto no sustituye consultar los manifests y checksums vivos.**

## Punto de parada y causa que todavía no puede afirmarse

Después de escribir el evento 119, el código ejecutaba `ackFor`, luego exigía `duplicate.alreadyExists === true` y `duplicate.folderId === original.ack.folderId`, antes de iniciar legacyEdit. Cualquier fallo en identidad/revisión/hash/protocolo/archivos/estado, flag o carpeta podía lanzar una aserción. El catch superior solo mostraba un mensaje genérico; finally guardaba INCOMPLETE sin registrar la condición. **Esa pérdida de diagnóstico en el ejecutor es un defecto confirmado. La aserción concreta que falló no está recuperable.**

Para el 119 no podemos verificar retrospectivamente alreadyExists, folderId, key, revision ni contentHash: no se guardaron. El ACK original de TEST_PAV_B en avance tiene alreadyExists=false, revisión 1, carpeta presente y contentHash consistente; eso es válido para una primera entrega y no demuestra qué devolvió el replay.

En los fuentes locales entregados, TestHttp añade `_gboTest.metrics` a toda respuesta normal, y SyncV3Core devuelve alreadyExists=true cuando encuentra la misma key/revisión/hash. La respuesta sin métricas **no concuerda con ese camino normal**, pero los archivos locales no demuestran qué versión/ruta respondió esa petición concreta. Puede ser respuesta de otra envoltura/versión, respuesta inesperada o problema de observabilidad/transporte. No se elige una causa sin el cuerpo/ejecución correspondiente. El nuevo requestFingerprint, marca v2 y checks de ACK permitirán distinguirlo sin datos privados.

## Rendimiento: aislamiento v1 domina estos commits

| Secuencia/familia | Servidor | Bajo ScriptLock | Consultas guard | Tiempo guard | Fracción del servidor |
|---|---:|---:|---:|---:|---:|
| 80 Pavimentos | 174,522 s | 170,795 s | 1030 | 117,976 s | 67,60% |
| 108 SCAP | 172,803 s | 166,934 s | 947 | 118,829 s | 68,77% |

En el evento 80, `cpu.unzip` registra cuatro llamadas/12 ms y `store.unpackWorkbook` cuatro/118 ms; son fases diferentes/anidadas. SCAP no registra unzip. Son cifras de los archivos recibidos, no una estimación. No explican por sí solas todo el resto del tiempo. No se resta guard aritméticamente para predecir producción ni se atribuyen todos los BUSY al backend productivo.

El guard v1 recorría otra vez la misma cadena de padres desde getId, getName, getDescription, getBlob, listados, acceso por ID y mutaciones. getId incluso causaba validación cuando otro guard solo necesitaba obtener ese mismo ID para validarlo. El costo crecía con los niveles y las consultas repetidas. **Ese mecanismo y las cifras del log confirman la sobrecarga del ensayo.**

Siete fallos de transporte duraron aproximadamente 180 s, cerca del timeout de 180000 ms del cliente viejo; otro duró unos 90 s. El log viejo agrupaba timeout, HTTP, JSON y red bajo TRANSPORT_FAILED. La coincidencia temporal es compatible con timeout del cliente, pero no confirma la causa de los ocho fallos. Una ejecución Completada en Google tampoco acredita que el cliente recibió un ACK válido.

## Cambio de aislamiento, sin tocar transacciones

- getId entrega solo el identificador opaco del objeto ya obtenido; no consulta Drive. No permite por sí mismo leer contenido o escribir.
- Caché de pertenencia **solo para lecturas**, local a una solicitud mientras se posee ScriptLock, o durante el diagnóstico exclusivo con UserLock/inflight=0 y mutaciones bloqueadas. Se limpia al iniciar/finalizar y antes/después de cada intento de mutación, incluso si falla.
- **Cada escritura** de archivo/carpeta, copia, movimiento, update avanzado, create y apertura de sesión reanudable hace una cadena fresca completa de fuente/destino. No confía en caché de lectura. Comprueba además que la propia subraíz sigue siendo hija directa de la raíz autorizada. Se conservan rechazos a externos, otras corridas, shortcuts, huérfanos, ciclos y múltiples padres.
- No se añade CacheService, caché global entre solicitudes, “validado una vez” para escrituras ni reducción del ScriptLock de V20. No se modifican journals, ACK, UUID o revisiones. Las mutaciones que cambian pertenencia invalidan la caché igual que las demás.
- Continúa el requisito de un único proyecto escritor y ningún movimiento manual/externo durante la corrida. Drive no permite una transacción atómica entre comprobar ascendencia y escribir; ese límite ya existía. El nuevo caché no se usa para autorizar escrituras y las pruebas mueven objetos/subraíz después de una lectura para comprobar que la siguiente mutación falla antes de escribir.

La comparación local de tres commits representativos dio **1793→744 consultas guard**, mismas escrituras funcionales, ACK y unzip. Es un conteo con servicios simulados, no una medición Google después del cambio. No se afirma un porcentaje de mejora productiva.

## Qué prepara la recuperación

El modo D lee los recibos/journals directamente, **sin llamar a v3Store**, que podría crear o recuperar. No escribe en Drive ni en propiedades. Distingue un receipt original false válido de un replay que debe ser true. Bloquea si hay PENDING, divergencia, inflight activo, cambio de corrida o falta de checksum.

R repite D y exige el mismo video/fixtures; copia el avance a un intento nuevo, conserva la evidencia fuente y no vuelve a subir los ocho padres. Primero comprueba el replay del 119 con todos sus campos y telemetría vinculada. Solo si pasa continúa las etapas pendientes. Un timeout ya no genera reenvíos pesados a ciegas: se detiene y exige conciliación. Los dos clientes se drenan antes de cerrar el informe, evitando el antiguo riesgo de Promise.all rechazado mientras otra tarea seguía activa.

**Estado actual:** evidencia local conciliada; condiciones y recuperación probadas localmente; conciliación viva y reanudación Google pendientes de ejecución explícita por el propietario. Ninguna decisión de publicación productiva se deriva de este ensayo.
