# Qué se mide y qué está demostrado

**Google: cero ejecuciones realizadas por Codex. Tiempo real del servidor, p50/p95, throughput y efecto en campo: NO MEDIDOS.** El paquete no se publicó, no se consultó una URL productiva ni se escribió en Drive. La carpeta autorizada se verificará con el propietario al ejecutar la copia; no se afirma haber comprobado su acceso desde Google aquí.

## Evidencia disponible

1. Antecedente conservado: **95 casos distintos aprobados**, 71 regresiones y 24 casos comparativos de V20/borradores/candidato instrumentado. `evidencia/ANTERIOR_RESUMEN_PRUEBAS.json`. No se repitió la auditoría Android ni se suman esos casos como nuevos.
2. Pruebas nuevas: **23 casos distintos**, 22 de aislamiento/contrato y un recorrido integrado BASE/CANDIDATO/UPGRADE con once padres por variante (ensayo corto más una alta adicional por familia). Ver `evidencia/validacion-entrega.tap`, su reejecución focalizada `ejecutor-upgrade-final.tap` y `RESUMEN_EJECUTOR_LOCAL.json`. El TAP del primer intento conserva un fallo del ejecutor: una petición stale reutilizaba staging de revisión 4 y obtenía correctamente `STAGE_OWNERSHIP_CONFLICT`; se corrigió la petición ficticia para aportar sus propios bytes. No se cambió el motor para hacerla pasar. Ejecutan las copias del ZIP, barreras negativas y servicios Google simulados. Incluyen SIC, SCAP, Pavimentos, V2, literal/herencia/borrado Observaciones, rollback, SHA, replay y video. El video local es binario opaco, no prueba de un archivo reproducible; para Google se exige un video ficticio real de varios bloques.
3. La comparación anterior de cinco altas ahorró 20→15 unzip y, en dos muestras de cinco filas, 25→15 unzip. Medianas locales 5,710→4,648 segundos para aquel lote Node/Python: **no son tiempos Google** y no predicen el caso de campo 2/17 o 1/11.
4. En el escenario nuevo de once padres se midieron **75→60 unzip** (20% menos) con iguales hashes de archivos activos y partes Excel. El ensayo UPGRADE vuelve a cargar las fuentes candidatas sobre los objetos/propiedades generados con las fuentes BASE, sin recrear IDs. Estos resultados siguen siendo locales. No se comparan tiempos de escenarios distintos ni se convierten operaciones del simulador en cuotas Google.

## Instrumentación real preparada

| Medida | Captura y significado |
|---|---|
| Espera ScriptLock | Alrededor de cada `tryLock(30000)` original: `locks[].waitMs`, adquirido sí/no. No sustituye ni elimina ScriptLock. |
| Tiempo bajo lock | Desde adquisición hasta liberación real: `locks[].heldMs`. Incluye comprobaciones de seguridad de ensayo hechas durante ese intervalo. |
| Preparación/staging | Acción `stageRecordV3`, businessMs, decode/SHA y apertura/recuperación del store; diferencia respecto al lock muestra la validación trasladada. |
| Validación | Fase `validation` alrededor de validate, incluyendo hashes/plantilla y descompresión inicial. |
| Descompresión/compresión | `cpu.unzip`/`cpu.zip`: número y ms de invocaciones reales de Utilities; `store.unpackWorkbook`/packWorkbook incluye trabajo adicional. |
| Consolidación Excel | `consolidation`, desde preparación de formatos hasta antes de begin; lecturas históricas, conciliación, orden, merge y zip/hash. |
| Archivos/documentos | `transaction.file`, lectura/verificación SHA y operaciones Drive observadas. No significa ACK padre. |
| Publicación Excel | `transaction.aggregate`: escritura, bindings y comprobación posterior. |
| Commit/ACK | transaction.save/finish; `businessMs` hasta que el handler devuelve el ACK V20. `totalServerMs` añade barrera, validación de ámbito y liberación de lease. |
| Red y tiempo hasta confirmación | `clientMs` por petición; `totalPrepareStageAckMs` por ficha desde reserva hasta recibir ACK válido, con sus reintentos. Incluye red/colas/servicio; no equivale solo a CPU. |
| Video | Apertura de sesión, PUT/range/offset y finish separados del ACK del padre. No se guarda sessionUrl. |
| Drive | `backend.*`: invocaciones del código funcional; `guard.metadata.get`: verificaciones extra de ascendencia. Son llamadas observadas a métodos de servicio, **no un contador garantizado de RPC físicos ni de facturación**: DriveApp puede aplicar cachés internas. `Drive.Files.*` identifica las llamadas explícitas al servicio avanzado. |

Las fases están anidadas: **no sumar sus milisegundos como si fueran intervalos excluyentes**. Los archivos de snapshot/descarga y la reserva inicial de fixtures son diagnósticos/preparación, se excluyen del throughput normal. La propia barrera añade lecturas Drive y propiedades; debe medirse por separado. Se entrega igual en ambas variantes, pero esos costos dependen del número de llamadas del flujo. No se extrapola un porcentaje del ensayo protegido a producción sin contrastar los logs.

Cada respuesta mantiene el ACK y añade `_gboTest.metrics`; se registra además `GBO_TEST_METRICS` en Ejecuciones. No se incluyen payload, fotos, coordenadas, usuario real, token o URL. La capa candidata conserva su log numérico previo `GBO_V3_LOCK`, no una segunda optimización. Los resultados JSON separan familia, cliente y escenario NORMAL, FIXTURE_ALLOCATION, FORCED_BUSY, DISCARDED_ACK, IDEMPOTENT_REPLAY y LOCAL_COPY_DUPLICATE_JOB. Los errores de transporte quedan registrados y no se transforman en éxito.

El snapshot final usa los checksums binarios publicados por Drive y descarga bytes solo para manifests, journals y libros (o si no hay checksum); excluye STAGING/REVISIONS del inventario activo. Las lecturas diagnósticas no se suman a los tiempos de commit. Los respaldos se conservan y no se borran para aparentar ausencia de duplicados.

## Lectura de resultados Google

El comparador produce p50/p95, n, totales de wait/held, fases y operaciones por acción/familia/escenario. Revisa también las duraciones por ficha, revisiones y número de archivos. Una muestra con tres archivos no equivale a tres fichas. Un archivo recibido sin commit no es una entrega confirmada.

El ensayo corto tiene pocas observaciones y solo un video; sus percentiles son descriptivos. Usar la misma carga 30 por familia y el mismo video/JPEG en cada variante; repetir al menos tres pares alternando BASE/CANDIDATO, con nuevas corridas. Comparar ejecuciones de Google con igual cuenta, configuración y perfil de red. Etiquetar cada pausa/cuota/BUSY artificial; no ocultar reintentos fallidos. La carga creciente permite observar el costo de releer libros históricos; no implementa su caché durable.

Las ejecuciones que terminan por una excepción, timeout, bloqueo de seguridad o comparación fallida mantienen estado INCOMPLETE. El comparador no las aprueba. Conservar `eventos.jsonl` y revisar Ejecuciones Google para distinguir cuotas, errores Drive, tiempo de lock y procesos cancelados. No se necesita compartir tokens, URL privada ni ScriptProperties productivas para revisar las cifras agregadas.
