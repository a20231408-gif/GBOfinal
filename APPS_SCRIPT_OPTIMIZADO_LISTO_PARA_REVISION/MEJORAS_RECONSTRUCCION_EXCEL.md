# Rendimiento: costo demostrado y mejoras pendientes

## Hechos del código y de las pruebas

`commitRecordV3` adquiere el ScriptLock global antes de crear `v3Store`. El almacén enumera journals y recupera PENDING antes de leer recibos. La validación vuelve a leer/verificar archivos staged y descomprime cada XLSX entrante. Después, el motor lee todos los manifests y para cada formato/ámbito afectado reúne las filas, descomprime sus libros de origen, concilia Observaciones, ordena y vuelve a comprimir el consolidado. Copias de protección, escritura/verificación de archivos, metadatos, journal y ACK se completan antes de liberar el lock.

Una nueva fila por commit en un ámbito con N filas implica **N(N+1)/2 descompresiones para los agregados**, más N validaciones de la fila entrante, sin contar reintentos o varios formatos. Se comprobó con cinco commits ficticios: **20 unzip y 5 zip**, en referencia y propuesta. Es un recuento de llamadas del motor real local; no una medida de segundos de Google ni un benchmark de CPU en tablets.

Además, `v3Store` se crea en staging, reserva y consultas de padre de video; vuelve a enumerar journals. Hay lecturas repetidas de un mismo registro en validación/commit/begin y enumeraciones de bindings/manifestaciones. Los manifests conservan XLSX base64, por lo que crecer la colección puede aumentar lectura, parseo y memoria. El motor también contiene `if(all.length>1000)`: al superar ese umbral rechaza commits, incluso antes de distinguir una actualización. No se elimina ese límite ni se promete capacidad ilimitada.

VideoV1 usa el mismo ScriptLock en begin/finish; su apertura de sesión resumible y verificaciones pueden competir con SIC/Pavimentos. La propuesta no instrumenta esas fases. La incidencia 2/17 SIC y 1/11 muestras no queda atribuida automáticamente a una de estas rutas: faltan ejecuciones reales y diagnóstico del intervalo.

## Qué está implementado

Validación pura de staging antes del lock y medición segura en SyncV3. Reduce solo trabajo protegido de esa validación; no se ha acreditado ahorro real ni evita que las tablets esperen una reconstrucción de Excel. Los 28/28 ACK de la carga local previa prueban recuperación e idempotencia en un simulador; sus milisegundos no representan Apps Script. No repetirlos como si fueran un SLA para Starlink.

## Secuencia recomendada de optimización

| Mejora posible | Beneficio esperado y límite | Garantías y evidencia necesarias |
|---|---|---|
| Medir primero sobre versión contrastada | Separa preparación, lock ocupado, recuperación, XLSX y escritura. | Prueba aislada autorizada, tiempos reales y tasa de ACK; la medición propuesta sola no explica video ni red. |
| Reutilizar el libro entrante ya validado | Evita descomprimir dos veces la misma fila actual. Ahorro acotado; no resuelve el crecimiento del resto. | Coincidencia de dueño/revisión/formato/hash; conservar selección/herencia Observaciones y plantilla. Probar libro incompatible y reintento perdido. No implementado. |
| Memoización por ejecución de lecturas inmutables | Evita consultas/parseos repetidos de un registro. | Caché únicamente de esa ejecución, después de recuperar journals; invalidación en cada escritura y recuperación, sin compartir objetos mutables accidentalmente. No usar caché global como autoridad. No implementado. |
| Cachear fila XML y plantilla normalizadas por fuente | Reduce descompresión de N libros históricos en cada entrega, sin cambiar ACK ni orden. Probable mejora estructural más útil si los datos reales confirman ese costo. | Caché durable derivada, con versión y hash del XLSX original, plantilla/estilos/Observaciones, identidad y revisión. Recuperación mediante origen, fallback para históricos y actualización dentro del mismo journal. Nunca perder notas ni verificar solo el cache. No implementado. |
| Índices derivados por ámbito / transacciones pendientes | Reduce escanear toda la colección y todos los journals completos. | Índice reconstruible, versionado, consistente con manifest/journal; fallo o ausencia debe volver a lectura segura. No saltarse una recuperación PENDING para ahorrar tiempo. No implementado. |
| Consolidar varias entregas por generación | Podría evitar una reconstrucción por cada ficha cuando llegan varias. | Diseño separado: cola durable, generación y prueba de concurrencia. **No emitir el ACK final actual antes de producir/verificar el Excel que acredita.** Si se separan archivos y consolidado, requiere capacidad/protocolo y cliente compatibles, no un cambio transparente. No implementado. |
| Paralelismo por carretera/formato | Posible solo tras rediseñar autoridad y recuperación por recursos. | LockService actual es global; no inventar locks nominales ni usar usuario/tablet como lock. Requiere fencing/recursos compartidos/diarios y protección de reservas globales. No quitar el lock para acelerar una demostración. No implementado. |

No reducir `tryLock(30000)`, omitir hash/lectura de confirmación, saltarse validación de reintentos o responder SYNCED por staging como atajo. Tampoco escribir el consolidado fuera del lock sin comprobar una generación/fencing que impida publicar una vista obsoleta. Conservar los fileId y bindings, incluida recuperación frente a respuesta perdida.

## Prueba de rendimiento real necesaria, en entorno aislado

Usar dos tablets/clientes y dos usuarios GBO ficticios contra una copia autorizada del proyecto y raíz de pruebas, sin cambiar el /exec de campo. Una misma raíz de prueba debe tener un solo proyecto escritor; otro proyecto tendría otro ScriptLock. Ensayar primero 2 SIC con fotos y una muestra por cliente; ampliar de forma acotada al escenario 17 SIC/11 muestras con tamaños representativos. Medir antes/después bajo las mismas condiciones, incluir intervalos sin Internet, BUSY e interrupción tras escritura.

Registrar: tiempo hasta ACK por familia, percentiles con tamaño de muestra explícito, archivos/bytes y reintentos, wait/held/recuperación/unpack/pack/escritura cuando existan. Comparar último ACK/revisión, filas y hashes/IDs, no solo progreso 3/6. Obtener estado agregado Android antes/después y ejecuciones sanitizadas Apps Script del mismo intervalo. No sumar fases anidadas ni confundir lock ocupado con tiempo de red.

Solo después de esa evidencia seleccionar una mejora estructural y preparar su código sobre producción verificada. En este paquete **no hay todavía una optimización implementada del costo de reconstrucción de Excel**.
