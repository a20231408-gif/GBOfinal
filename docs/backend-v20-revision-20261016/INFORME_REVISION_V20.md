# GBO_Inventario V20: revisión de los dos borradores

Fecha: 16/10/2026. Exclusivamente backend de inventario. **Propuesta para revisión; no publicada ni aprobada para producción.** Android 1.4.7 no se modifica.

## Resultado

Los ocho originales del editor fueron localizados en `D:/Downloads`. El propietario confirma que no cambiaron después de publicar V20 el 15/10/2026 a las 07:07. Se acepta esa procedencia declarada; no se consultó por API la instantánea numérica ni se accedió al proyecto Google. Los SHA-256 exactos y tamaños están en `evidencia/inventario-originales.json`; las copias íntegras privadas de trabajo permanecen en `tmp/backend-v20-revision-20261016/originales-v20`, ignorado por Git.

Los siete módulos `SyncV3`, `SyncV3Core`, `PhotoNamesV2Core`, `SicObservationsV1Core`, `VideoV1`, `VideoV1Core` y `VideoNamesV2Core` coinciden con las referencias locales al ignorar líneas vacías y espacios finales. **No son byte a byte iguales**: los hashes originales se conservan por separado. Esta comparación resuelve la anterior incertidumbre sobre la disponibilidad de `SicObservationsV1.plain` (10K) y los módulos de nombres de video 10H en las fuentes aportadas.

`Codigo.gs` conserva la lógica local de despacho. Sus diferencias revisadas son obtener el token mediante `PropertiesService.getScriptProperties().getProperty("GBO_DRIVE_API_TOKEN")` y ubicar los anuncios `readableVideoNamingVersion`/`sicObservationsVersion` como campos separados de la misma respuesta. La prueba con valores ficticios acredita autenticación y despacho a los handlers correctos. **No se reemplaza ni se entrega una copia instalable de Codigo.gs.** No se lee ni imprime el valor real de una propiedad. El diff entregado redacta los literales privados.

## Qué contiene el candidato de esta revisión

| Archivo completo | Contenido y decisión |
|---|---|
| `candidato-no-publicar/SyncV3.gs` | Instrumentación anterior de fases/lock + el único traslado de decode/SHA-256 de staging antes del lock. Se incorpora la comprobación `typeof f.key` del nuevo borrador. No se añade otra implementación de staging. |
| `candidato-no-publicar/SyncV3Core.gs` | Borrador recibido de reutilización del workbook entrante ya validado. No se añade caché durable ni se cambia la transacción. |
| `diferencias/SyncV3.diff`, `SyncV3Core.diff` | Deltas contra los originales aportados, normalizando solo blancos de presentación. |

No copiar simultáneamente variantes antiguas, borradores y candidato: duplicaría funciones globales. Los otros seis .gs permanecen en su V20. La propuesta anterior queda conservada en su carpeta, sin sobrescribirla; su afirmación de que no acelera Excel sigue siendo cierta para aquella versión, pero este segundo candidato sí evita una descompresión entrante por fila reutilizable.

## Equivalencia y límites encontrados

**Staging válido:** mismos SHA-256, bytes persistidos y relectura de confirmación, nombre técnico, `stageFileId` y ACK. Decode/hash puro sale del lock; todas las lecturas/escrituras de Drive y recuperación de journals permanecen dentro. No cambia `tryLock(30000)` ni se permite escribir con BUSY. Un staging sigue sin ser la entrega definitiva.

**No hay equivalencia estricta para todos los errores inválidos:**

- Archivo con hash incorrecto y lock ocupado: V20 responde BUSY; los borradores/candidato pueden responder `HASH_OR_FILE_SIZE` sin pedir el lock. Esto evita reintentar un archivo que ya se conoce inválido. La respuesta de una solicitud válida bajo BUSY sigue siendo BUSY/retryable.
- `file.key` no textual: V20 podía devolver `STAGE_COMMUNICATION_ERROR`/retryable; el nuevo guard devuelve `INVALID_STAGE`/no retryable.
- Un rechazo previo al lock no ejecuta recuperación PENDING en esa petición. La siguiente petición válida que adquiere el lock sí conserva la recuperación normal antes de leer recibos.
- Para bytes vacíos o mayores de 24 MiB, el borrador calcula SHA antes de rechazar por tamaño; V20 podía cortar por tamaño antes de calcularlo. Un BUSY válido también paga ahora decode/hash fuera del lock. No se presenta como ahorro de CPU total o de red.

Estos cambios de precedencia/errores deben aceptarse expresamente en la revisión del parche. No se afirma igualdad universal de respuestas. Ver `evidencia/diferencias-respuestas-invalidas.json`.

**Workbook entrante:** `validate()` sigue decodificando y comprobando SHA-256, plantilla, única fila y hash canónico antes de la reutilización. El mapa se crea dentro del commit y contiene libros reconstruidos por el servidor, no un `workbook` confiado al cliente. La clave usa UUID de fila, formato y SHA; además se exige `ownerKey === p.key`. Los libros de otro dueño, formatos retenidos e históricos sin entrada coincidente siguen el camino original. No se persiste el mapa ni se elimina validación en el replay.

`inherit`, la selección de plantilla, comparación de firmas, normalización `plain`, extensión de impresión y merge se ejecutan igual después de obtener el libro crudo. El código no modifica en sitio ese libro al conciliar Observaciones. Se probaron mezcla de históricos con/sin notas, edición legacy que retiene nota anterior y borrado explícito con columna presente/vacía. No se cachea una nota efectiva de otra revisión. Las partes XLSX no modificadas conservan su contenido.

**Transacciones y compatibilidad:** conservación de UUID, reservas/numeración estable, folderId/fileId, revisiones/base, rechazo stale/conflict, orden técnico A/B/D/C sin renumerar carpetas, fotos V2 y padres SCAP. Se inyectaron interrupciones tras archivo, agregado y manifiesto dejando PENDING: el almacén real del simulador restaura antes de exponer recibos y el reintento termina con las identidades originales. Si la interrupción sucede después de persistir COMPLETE, devuelve el recibo ya confirmado al reintentar. Sin cambio de journal ni de significado del ACK.

El ensayo de video verifica nombre 10H, identidad reservada, begin perdido, checksum erróneo/correcto, finish y reserva histórica que no se renombra, con servicios Google simulados. No sube un video real. El candidato conserva `v3Store()` sin argumento para la consulta de padre de VideoV1.

El JSON con `_gboJointAnnotations` de Android 1.4.7 se conserva como archivo opaco con sus bytes/hash y revisión. Se prueban las tres superficies Pavimentos y varias filas por muestra. Para cobertura del contrato formato/superficie, los casos sintéticos de SIC-15/16 reutilizan un XLSX de cabecera de dos filas de SIC-14: **no certifican las plantillas contractuales reales SIC-15/16**. La regresión SIC-14 sí usa el archivo existente generado por Android. No se recompila, instala ni modifica Android.

## Pruebas y mediciones

**95 casos distintos aprobados en su resultado final; 0 fallos pendientes.** Son 71 regresiones existentes sobre las copias originales y 24 casos de revisión (los escenarios comparativos ejecutan V20, solo staging, ambos borradores e integrado instrumentado). Se conservan los fallos iniciales del arnés y las reejecuciones focalizadas. Ver `evidencia/RESUMEN_PRUEBAS.json` y los TAP; no se suman reejecuciones como casos nuevos.

| Ensayo local | V20 | Integrado | Alcance |
|---|---:|---:|---|
| Cinco altas, una fila: unzip / zip | 20 / 5 | 15 / 5 | 25% menos unzip; mismas escrituras lógicas Drive. |
| Mediana local del lote, n=3 | 5,710 s | 4,648 s | Node + Python ZIP + dobles, sin latencia Google. |
| Dos muestras de cinco filas: unzip / zip | 25 / 2 | 15 / 2 | 40% menos unzip; fixture sintética. |
| 20 staging de 256 KiB: mediana lock ocupado | 1,879 s | 1,216 s | Decode/hash trasladados; sin competencia Google real. |
| 20 staging: mediana total local | 1,880 s | 1,847 s | CPU total similar; no se promete menor espera de campo. |

Detalles, rangos, perfiles, ruido de medición y recuentos en `MEDICIONES_Y_LIMITES.md` y `evidencia/mediciones-locales.json`. **Ejecuciones/tiempos reales Google: 0 / no medidos.** Incluso el perfil de commits cuyo código efectivo no cambia varía de tiempo en la estación; los milisegundos locales no permiten atribuir toda la diferencia a la optimización.

Los logs conservan los intentos iniciales, no solo el último resultado. Los primeros nueve fallos de la nueva suite correspondieron a fixtures sin `photoNamingVersion=2`/categoría requerida y a contar copias de respaldo `record_*.json` como manifiestos activos. Se corrigió únicamente el arnés y se repitieron los casos afectados. Los once casos inicialmente aprobados no se repitieron por ello. Un último caso V2 se volvió a ejecutar después de completar su categoría. Los .gs candidatos no fueron alterados para hacer pasar esos ensayos.

Los ZIP del arnés nuevo usan fecha fija para comparar bytes y SHA de forma reproducible. La igualdad local de bytes/ACK no promete que `Utilities.zip` de dos ejecuciones Google produzca el mismo contenedor binario: las partes XML y el ACK de la transacción real deben comprobarse allí. Cada implementación mantiene la verificación de los bytes que ella misma produce y publica.

## Pendiente antes de publicar

1. Revisar/aceptar el delta exacto y la precedencia distinta de errores de staging. Confirmar la versión 20 asignada a la implementación desde la consola del propietario; no se necesita compartir URL ni IDs privados.
2. Aportar/revisar `appsscript.json` y versiones de servicios/librerías si las hubiera. No se recibieron con los ocho .gs. Los módulos compilan como JavaScript local; ello no verifica scopes, ejecución como propietario ni servicios habilitados en Google.
3. Prueba real autorizada en **proyecto y raíz ficticia aislados**, con dos clientes, mismas fuentes V20 y después candidato, SIC/SCAP/Pavimentos, fotos V2, video, plantillas reales, notas históricas, ediciones, pérdidas de ACK y ejecuciones simultáneas. Un solo proyecto escritor por raíz: dos proyectos tendrían locks distintos.
4. Medir tiempos Google de espera/ocupación/fases, p50/p95 con tamaño de muestra, total hasta ACK y conteos por familia/archivo. La telemetría propuesta no mide el binario video ni toda la red Android. Las fases anidadas no se suman como tiempos excluyentes.

**Decisión:** candidato apto para revisión y prueba aislada; no aprobado aún para producción ni demostrado como solución a la incidencia 2/17 SIC y 1/11 Pavimentos. El ahorro de unzip está demostrado localmente; su efecto real sobre esas esperas sigue pendiente. La segunda propuesta de históricos está en `PROPUESTA_2_HISTORICOS_NO_IMPLEMENTADA.md` y no se implementó.

No hubo llamadas a Google, escrituras de inventario real, publicaciones, cambios de propiedades/raíz/URL/permisos, modificaciones del servicio de usuarios, commit ni push.
