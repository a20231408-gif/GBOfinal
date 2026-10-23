# Carpetas públicas legibles — GBO Android — 20/10/2026

Estado: **implementado en fuentes; 45 casos locales distintos aprobados**. No se compiló, instaló ni distribuyó una APK nueva. La candidata 1.4.11 (25) ya entregada se conserva y todavía no incluye esta mejora.

## Origen y campos comprobados

`localfiles/LocalDocumentLocation.kt:resolve` generaba el destino público a partir de `route/family/label_UUID_recordId`, o de `receipt.logicalPath` después de un ACK. `LocalExportNames.safe` reemplazaba espacios/acentos por identificadores ASCII. El directorio privado `LocalExportBuilder.work` es independiente: `files/local_exports/<job.id>`.

En SCAP, **sección B → `singularityName`**, propietario `inspection`, es el campo que edita el operador. `ScapRepository.setField` lo refleja en el valor `bridgeName` y la columna **`ScapInspectionEntity.bridgeName`**. La copia pública usa ese nombre B y `inspection.bridgeCode`, si ya existe. No se usa la categoría de la sección A como nombre propio ni se crea un campo nuevo. Para registros históricos sin la clave B se consulta la columna existente, descartando la categoría genérica PUENTE.

El modelo actual SIC-20, tanto formulario como entidad, no tiene un campo de nombre propio para Túnel, Muro o Badén. Por eso se usa su ruta/PR, sin inventar un nombre ni tomarlo de Observaciones. La representación de PR solo formatea el extremo inicial guardado; no calcula ni modifica longitudes entre PR.

## Convención implementada

Los contenedores por ruta/familia, REVISIONES/R_<revisión> y las subcarpetas de medios se conservan. El cambio afecta al directorio público identificable de la ficha.

| Datos existentes | Carpeta pública de ejemplo |
|---|---|
| SCAP: código real 001 y nombre B Correviento | `Puente 001 - Correviento` |
| SCAP: nombre B, sin código | `Puente - Correviento` |
| Segundo registro con igual nombre/código visible | `Puente - Correviento (2)` |
| SCAP sin nombre pero con código real | `Puente 001` |
| SCAP sin nombre ni código, con ubicación | `Puente - PE-28H - PR 10+060` |
| Sin identificación ni ubicación | `Puente - Sin referencia` |
| Muro / Badén / Túnel | `Muro - PE-28C - PR 10+060`, `Badén - PE-28C - PR 12+150`, `Túnel - PE-28C - PR 10+060` |
| Muestra rígida: tramo y muestra guardados | `Pavimento rígido - Tramo 3 - Muestra 2` |
| Otras superficies | `Pavimento asfáltico - Tramo 3 - Muestra 2`, `Pavimento no pavimentado - Tramo 3 - Muestra 2` |
| Exportación manual sin contexto de una ficha individual | `Exportación - Resumen` (nombre del archivo existente) |
| Foto individual sin identidad de ficha | `Fotografía individual`, `Fotografía individual (2)` |

En SIC-17 con detalle SIC-17A histórico se usa `Sic17AEntity.bridgeName` y su `bridgeCode` real (por ejemplo, `Puente P-002 - Santa Rosa`). Si el detalle no tiene código se conserva el código de SIC-17. `Sic17Entity.singularityName` describe la singularidad cruzada y no se usa como nombre del puente.

No se inventa un 001/002 contractual. El código aparece solo si ya está guardado. Los sufijos de colisión (2), (3), etc., son reservas locales persistentes por dispositivo; no son números de carpeta Drive, numeración del inventario ni códigos contractuales.

## Repetición, coincidencias y cambio de nombre

`PublicLocalFolderNames` conserva una asociación de UUID interno/recordKey, contenedor, nombre normalizado y nombre reservado. Usa el espacio de claves `LOCAL_PUBLIC_FOLDER_V2/…` en la tabla de metadatos local ya existente `device_delivery_choices`, con transacción Room; no añade entidades, columnas ni migración. Los destinos DEVICE/DRIVE y ajustes existentes se consultan por sus claves originales y no cambian.

La misma identidad con el mismo nombre reutiliza la reserva, incluso tras reabrir Room. Dos identidades con nombres coincidentes reciben carpetas distintas; también se consideran las rutas publicadas y planes congelados anteriores. La comparación protege diferencias de mayúsculas/minúsculas y Unicode normalizado. Una reserva dañada bloquea la asignación, sin apropiarse del destino de otra ficha.

Si se edita el nombre y se guarda, la siguiente copia de esa revisión usa el nombre actualizado y conserva las carpetas/archivos antiguos. No se renombra ni elimina un directorio histórico. Los planes ya congelados, especialmente los pendientes/interrumpidos, siguen utilizando sus rutas y URI originales para recuperarse.

Para regenerar explícitamente una ficha antigua: abrir sus archivos locales y pulsar **Guardar en Descargas**, **Exportar fotografías** o **Guardar paquete ZIP**. Si su copia anterior ya estaba finalizada/parcial/fallida y tenía nomenclatura anterior, se crea una edición local de copia con alcance `<scope>_READABLE_V2`, preservando el trabajo y manifiesto anterior. Se mantiene la fórmula original de identificación del trabajo; el nuevo alcance distingue esta regeneración, sin cambiar recordKey, UUID ni revisión de inventario. Repetir la acción reutiliza esa edición. Las etiquetas Documentos y medios/Fotografías/Paquete ZIP se conservan.

Los archivos independientes ya congelados sin contexto de ficha conservan su destino; los nombres nuevos se aplican al generar nuevas copias. No se escanea ni regenera masivamente el histórico al abrir la aplicación.

## Caracteres e integridad

Los nombres de directorio conservan ñ y vocales acentuadas, se normalizan a NFC y eliminan controles/separadores y caracteres inválidos en sistemas habituales. Se evitan nombres vacíos, puntos finales y nombres reservados como CON/NUL. Cada nombre visible se limita a **96 bytes UTF-8**, incluyendo el sufijo de colisión; el dato completo de la ficha permanece intacto. Los contenedores técnicos siguen usando el saneamiento existente.

No se modificaron `SharedDocumentStore`, comprobaciones de propietario, escritura pendiente/publicación, verificación tamaño/SHA-256, rutas de originales, nombres de fotos/videos, staging, WorkManager ni protocolos remotos. El contenido del nuevo manifiesto documental local refleja la nueva ruta y recibe su SHA-256 normal; no se pretende conservar un hash de contenido después de modificar una ruta dentro de ese manifiesto. Los manifiestos históricos permanecen congelados. Los hashes de fotografías, videos y archivos originales siguen verificándose sin excepciones.

El nombre público deja de depender del ACK. `remoteLogicalPath`, folderId y revisión confirmada se conservan como evidencia dentro del manifiesto local. No se renombra ni escribe ninguna carpeta Drive, ni se cambia un ACK o un payload remoto. El cálculo actual de longitud entre PR no se modificó.

## Pruebas y conservación

Se ejecutó `:app:testDebugUnitTest` con filtros focalizados, configuración local ordinaria, sin `gboValidationBuild` y sin tareas de ensamblado APK. Los cuatro lotes finalizaron con BUILD SUCCESSFUL. Se cuentan casos distintos, no las repeticiones entre lotes:

| Clase | Casos aprobados |
|---|---:|
| PublicLocalNamesTest | 11 |
| AutomaticDocuments149Test (selección relacionada) | 3 |
| LocalCopyFinalTest | 4 |
| LocalFiles148MediaStoreTest (incluye variantes SDK) | 7 |
| LocalFiles148Test | 16 |
| Sic20DraftDetail111Test | 4 |
| **Total distinto** | **45** |

Cobertura nueva: nombre B y código real SCAP; SIC-17A histórico; puente sin nombre; dos UUID con igual nombre; ocho reservas concurrentes y reapertura Room; cambio de nombre conservando copia/ACK anterior; reexportación sin escrituras nuevas; cancelación después de escribir y antes de publicar con recuperación verificada; asociación de fotografías, croquis, video y XLSX; Muro/Badén/Túnel con PR; muestra rígida con PDF/XLSX; ñ/tildes/caracteres inválidos/nombres largos/colisiones truncadas; regeneración histórica FULL/PHOTOS/ZIP sin alterar el plan anterior; reserva dañada rechazada; exportaciones independientes y fotos individuales. La prueba MediaStore mantiene el rechazo de archivos de otro propietario con rutas Unicode en SDK 29 y 34.

Las pruebas utilizan Room/repositorios reales sobre JVM/Robolectric, almacenamiento externo simulado y un adaptador de PdfDocument. La presencia y asociación de PDF se comprueban localmente; esto no acredita su representación física en Android ni una entrega Google. No hubo pruebas en dispositivo, subida Google ni cambio productivo en este encargo.

Resultados por caso: [PRUEBAS_LOCALES.json](nombres-locales-20261020/PRUEBAS_LOCALES.json). Conservación: [INTEGRIDAD.json](nombres-locales-20261020/INTEGRIDAD.json). Se compararon 779 fuentes/configuración de proyecto del estado inicial: únicamente cambiaron los cuatro archivos locales existentes y dos pruebas previstas; se añadieron la política y su prueba. Los seis originales tienen respaldo byte por byte en `tmp/nombres-locales-20261020/FUENTES_ANTES.zip`. Las cinco propiedades privadas y los doce APK de `output` conservaron sus hashes; no se añadieron APK. No se publican valores ni hashes de propiedades privadas.

Se conservan el hotfix SIC-20, la versión/configuración de compilación, todas las propiedades privadas y los APK anteriores. Respaldo reversible y logs en `tmp/nombres-locales-20261020/`. Evidencias resumidas en `docs/nombres-locales-20261020/`.

Archivos de aplicación: `PublicLocalFolderNames.kt` (nuevo), `LocalDocumentLocation.kt`, `LocalExportBuilder.kt`, `LocalCopies.kt` y la compatibilidad de etiquetas de `LocalCopyPresentation.kt`, todos bajo `app/src/main/java/com/tuempresa/inventariovial/localfiles/`. Pruebas nuevas: `PublicLocalNamesTest.kt`; actualizadas: `AutomaticDocuments149Test.kt` y `LocalFiles148MediaStoreTest.kt`.

## Riesgos y dictamen

- Falta verificar visibilidad y nombres en Archivos/Descargas de una tablet real. Los proveedores simulados no acreditan esa interfaz ni el comportamiento particular de cada fabricante.
- En Android8/9 se conserva el destino local autorizado mediante SAF; permisos y normalizaciones del proveedor pueden impedir una publicación, que debe quedar fallida/parcial en lugar de completa.
- Las carpetas de nombres anteriores se conservan deliberadamente. Un cambio de nombre produce una carpeta documental nueva, sin limpieza automática. Borrar datos Android o manipular carpetas fuera de GBO queda fuera de esta reconciliación; no debe hacerse para resolver una colisión.
- Los nombres demasiado largos se acortan solo en el directorio. Dos nombres truncados iguales se desambiguan con una reserva persistente.

**GO para integrar las fuentes en la siguiente candidata autorizada. NO-GO para afirmar validación física o aprobación de campo de esta mejora.** No hay defecto conocido en las pruebas focalizadas que bloquee esa integración. Antes de compilar deberá confirmarse nuevamente la disponibilidad de 1.4.11 (25), sin sobrescribir la candidata existente. No se hizo commit ni push.

Comprobación física pendiente, una vez autorizada la compilación e instalación compatible: con una ficha ficticia, guardar la copia y abrirla desde Archivos/Descargas; comprobar el nombre y abrir PDF/XLSX/foto/video disponibles; repetir la exportación y verificar que reutiliza el destino; editar y guardar el nombre para comprobar la nueva carpeta y la conservación de la anterior. Para una ficha histórica, usar Guardar en Descargas y comprobar que conserva la carpeta previa. No borrar datos ni forzar nuevas entregas Drive para esta comprobación.
