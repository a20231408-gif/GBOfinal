# Entrega de ronda 2 — GBO Inventario Vial

Fecha: 28 de septiembre de 2026. Trabajo sobre el repositorio actual, sin commit ni push.

Los estados distinguen implementación local, pruebas ejecutadas, comprobaciones no realizadas y decisiones/despliegues que corresponden al cliente. Las demostraciones contienen elementos y fotografías sintéticos; no constituyen inventario de campo.

## A. Archivos modificados

IMPLEMENTADO: 56 archivos de código, esquema, pruebas y este informe, respecto al estado Git previo de la ronda. Inventario completo:

- [app/schemas/com.tuempresa.inventariovial.data.database.InventoryDatabase/11.json](D:/Downloads/GBO_InventarioVial/app/schemas/com.tuempresa.inventariovial.data.database.InventoryDatabase/11.json)
- [app/src/androidTest/java/com/tuempresa/inventariovial/InventoryMigrationTest.kt](D:/Downloads/GBO_InventarioVial/app/src/androidTest/java/com/tuempresa/inventariovial/InventoryMigrationTest.kt)
- [app/src/androidTest/java/com/tuempresa/inventariovial/UserSecurityDeviceTest.kt](D:/Downloads/GBO_InventarioVial/app/src/androidTest/java/com/tuempresa/inventariovial/UserSecurityDeviceTest.kt)
- [app/src/main/java/com/tuempresa/inventariovial/DrivePathPlanner.kt](D:/Downloads/GBO_InventarioVial/app/src/main/java/com/tuempresa/inventariovial/DrivePathPlanner.kt)
- [app/src/main/java/com/tuempresa/inventariovial/DriveUploadWorker.kt](D:/Downloads/GBO_InventarioVial/app/src/main/java/com/tuempresa/inventariovial/DriveUploadWorker.kt)
- [app/src/main/java/com/tuempresa/inventariovial/MainActivity.kt](D:/Downloads/GBO_InventarioVial/app/src/main/java/com/tuempresa/inventariovial/MainActivity.kt)
- [app/src/main/java/com/tuempresa/inventariovial/RecordHistoryScreen.kt](D:/Downloads/GBO_InventarioVial/app/src/main/java/com/tuempresa/inventariovial/RecordHistoryScreen.kt)
- [app/src/main/java/com/tuempresa/inventariovial/camera/PhotoCaptureMetadata.kt](D:/Downloads/GBO_InventarioVial/app/src/main/java/com/tuempresa/inventariovial/camera/PhotoCaptureMetadata.kt)
- [app/src/main/java/com/tuempresa/inventariovial/camera/PhotoExif.kt](D:/Downloads/GBO_InventarioVial/app/src/main/java/com/tuempresa/inventariovial/camera/PhotoExif.kt)
- [app/src/main/java/com/tuempresa/inventariovial/camera/PhotoStampService.kt](D:/Downloads/GBO_InventarioVial/app/src/main/java/com/tuempresa/inventariovial/camera/PhotoStampService.kt)
- [app/src/main/java/com/tuempresa/inventariovial/data/dao/InventoryDao.kt](D:/Downloads/GBO_InventarioVial/app/src/main/java/com/tuempresa/inventariovial/data/dao/InventoryDao.kt)
- [app/src/main/java/com/tuempresa/inventariovial/data/database/InventoryDatabase.kt](D:/Downloads/GBO_InventarioVial/app/src/main/java/com/tuempresa/inventariovial/data/database/InventoryDatabase.kt)
- [app/src/main/java/com/tuempresa/inventariovial/data/database/RoundTwoMigration.kt](D:/Downloads/GBO_InventarioVial/app/src/main/java/com/tuempresa/inventariovial/data/database/RoundTwoMigration.kt)
- [app/src/main/java/com/tuempresa/inventariovial/data/entity/DeliveryBatchEntity.kt](D:/Downloads/GBO_InventarioVial/app/src/main/java/com/tuempresa/inventariovial/data/entity/DeliveryBatchEntity.kt)
- [app/src/main/java/com/tuempresa/inventariovial/data/entity/FieldPrEntity.kt](D:/Downloads/GBO_InventarioVial/app/src/main/java/com/tuempresa/inventariovial/data/entity/FieldPrEntity.kt)
- [app/src/main/java/com/tuempresa/inventariovial/data/entity/InventoryRecordEntity.kt](D:/Downloads/GBO_InventarioVial/app/src/main/java/com/tuempresa/inventariovial/data/entity/InventoryRecordEntity.kt)
- [app/src/main/java/com/tuempresa/inventariovial/data/entity/PhotoEntity.kt](D:/Downloads/GBO_InventarioVial/app/src/main/java/com/tuempresa/inventariovial/data/entity/PhotoEntity.kt)
- [app/src/main/java/com/tuempresa/inventariovial/delivery/DeliveryMetadata.kt](D:/Downloads/GBO_InventarioVial/app/src/main/java/com/tuempresa/inventariovial/delivery/DeliveryMetadata.kt)
- [app/src/main/java/com/tuempresa/inventariovial/delivery/DeliveryPhotoNameBuilder.kt](D:/Downloads/GBO_InventarioVial/app/src/main/java/com/tuempresa/inventariovial/delivery/DeliveryPhotoNameBuilder.kt)
- [app/src/main/java/com/tuempresa/inventariovial/delivery/DeliveryReviewPanel.kt](D:/Downloads/GBO_InventarioVial/app/src/main/java/com/tuempresa/inventariovial/delivery/DeliveryReviewPanel.kt)
- [app/src/main/java/com/tuempresa/inventariovial/delivery/DeliverySync.kt](D:/Downloads/GBO_InventarioVial/app/src/main/java/com/tuempresa/inventariovial/delivery/DeliverySync.kt)
- [app/src/main/java/com/tuempresa/inventariovial/delivery/RouteDelivery.kt](D:/Downloads/GBO_InventarioVial/app/src/main/java/com/tuempresa/inventariovial/delivery/RouteDelivery.kt)
- [app/src/main/java/com/tuempresa/inventariovial/field/CaptureToolsUi.kt](D:/Downloads/GBO_InventarioVial/app/src/main/java/com/tuempresa/inventariovial/field/CaptureToolsUi.kt)
- [app/src/main/java/com/tuempresa/inventariovial/field/CulvertPhotos.kt](D:/Downloads/GBO_InventarioVial/app/src/main/java/com/tuempresa/inventariovial/field/CulvertPhotos.kt)
- [app/src/main/java/com/tuempresa/inventariovial/field/EngineeringEditScreen.kt](D:/Downloads/GBO_InventarioVial/app/src/main/java/com/tuempresa/inventariovial/field/EngineeringEditScreen.kt)
- [app/src/main/java/com/tuempresa/inventariovial/field/FieldController.kt](D:/Downloads/GBO_InventarioVial/app/src/main/java/com/tuempresa/inventariovial/field/FieldController.kt)
- [app/src/main/java/com/tuempresa/inventariovial/field/FieldPrPanel.kt](D:/Downloads/GBO_InventarioVial/app/src/main/java/com/tuempresa/inventariovial/field/FieldPrPanel.kt)
- [app/src/main/java/com/tuempresa/inventariovial/field/LocationSuggestionPanel.kt](D:/Downloads/GBO_InventarioVial/app/src/main/java/com/tuempresa/inventariovial/field/LocationSuggestionPanel.kt)
- [app/src/main/java/com/tuempresa/inventariovial/gis/KmlExporter.kt](D:/Downloads/GBO_InventarioVial/app/src/main/java/com/tuempresa/inventariovial/gis/KmlExporter.kt)
- [app/src/main/java/com/tuempresa/inventariovial/model/form/InventorySaveRequest.kt](D:/Downloads/GBO_InventarioVial/app/src/main/java/com/tuempresa/inventariovial/model/form/InventorySaveRequest.kt)
- [app/src/main/java/com/tuempresa/inventariovial/repository/EngineeringEdits.kt](D:/Downloads/GBO_InventarioVial/app/src/main/java/com/tuempresa/inventariovial/repository/EngineeringEdits.kt)
- [app/src/main/java/com/tuempresa/inventariovial/road/AssetOrderScreen.kt](D:/Downloads/GBO_InventarioVial/app/src/main/java/com/tuempresa/inventariovial/road/AssetOrderScreen.kt)
- [app/src/main/java/com/tuempresa/inventariovial/road/FieldPrRepository.kt](D:/Downloads/GBO_InventarioVial/app/src/main/java/com/tuempresa/inventariovial/road/FieldPrRepository.kt)
- [app/src/main/java/com/tuempresa/inventariovial/road/LocationSuggestionEngine.kt](D:/Downloads/GBO_InventarioVial/app/src/main/java/com/tuempresa/inventariovial/road/LocationSuggestionEngine.kt)
- [app/src/main/java/com/tuempresa/inventariovial/scap/data/ScapRepository.kt](D:/Downloads/GBO_InventarioVial/app/src/main/java/com/tuempresa/inventariovial/scap/data/ScapRepository.kt)
- [app/src/main/java/com/tuempresa/inventariovial/scap/export/ScapDelivery.kt](D:/Downloads/GBO_InventarioVial/app/src/main/java/com/tuempresa/inventariovial/scap/export/ScapDelivery.kt)
- [app/src/main/java/com/tuempresa/inventariovial/scap/ui/ScapMedia.kt](D:/Downloads/GBO_InventarioVial/app/src/main/java/com/tuempresa/inventariovial/scap/ui/ScapMedia.kt)
- [app/src/main/java/com/tuempresa/inventariovial/viewmodel/InventoryViewModel.kt](D:/Downloads/GBO_InventarioVial/app/src/main/java/com/tuempresa/inventariovial/viewmodel/InventoryViewModel.kt)
- [app/src/test/java/com/tuempresa/inventariovial/AssetOrderingTest.kt](D:/Downloads/GBO_InventarioVial/app/src/test/java/com/tuempresa/inventariovial/AssetOrderingTest.kt)
- [app/src/test/java/com/tuempresa/inventariovial/DeliveryMetadataTest.kt](D:/Downloads/GBO_InventarioVial/app/src/test/java/com/tuempresa/inventariovial/DeliveryMetadataTest.kt)
- [app/src/test/java/com/tuempresa/inventariovial/DeliverySyncTest.kt](D:/Downloads/GBO_InventarioVial/app/src/test/java/com/tuempresa/inventariovial/DeliverySyncTest.kt)
- [app/src/test/java/com/tuempresa/inventariovial/Engineering2DataTest.kt](D:/Downloads/GBO_InventarioVial/app/src/test/java/com/tuempresa/inventariovial/Engineering2DataTest.kt)
- [app/src/test/java/com/tuempresa/inventariovial/EngineeringUxTest.kt](D:/Downloads/GBO_InventarioVial/app/src/test/java/com/tuempresa/inventariovial/EngineeringUxTest.kt)
- [app/src/test/java/com/tuempresa/inventariovial/InventoryRoomMigrationTest.kt](D:/Downloads/GBO_InventarioVial/app/src/test/java/com/tuempresa/inventariovial/InventoryRoomMigrationTest.kt)
- [app/src/test/java/com/tuempresa/inventariovial/LocationSuggestionScreenTest.kt](D:/Downloads/GBO_InventarioVial/app/src/test/java/com/tuempresa/inventariovial/LocationSuggestionScreenTest.kt)
- [app/src/test/java/com/tuempresa/inventariovial/LocationSuggestionTest.kt](D:/Downloads/GBO_InventarioVial/app/src/test/java/com/tuempresa/inventariovial/LocationSuggestionTest.kt)
- [app/src/test/java/com/tuempresa/inventariovial/PrSuggestionStorageTest.kt](D:/Downloads/GBO_InventarioVial/app/src/test/java/com/tuempresa/inventariovial/PrSuggestionStorageTest.kt)
- [app/src/test/java/com/tuempresa/inventariovial/RoadAxisStorageTest.kt](D:/Downloads/GBO_InventarioVial/app/src/test/java/com/tuempresa/inventariovial/RoadAxisStorageTest.kt)
- [app/src/test/java/com/tuempresa/inventariovial/RoundTwoStorageTest.kt](D:/Downloads/GBO_InventarioVial/app/src/test/java/com/tuempresa/inventariovial/RoundTwoStorageTest.kt)
- [app/src/test/java/com/tuempresa/inventariovial/RouteDeliveryTest.kt](D:/Downloads/GBO_InventarioVial/app/src/test/java/com/tuempresa/inventariovial/RouteDeliveryTest.kt)
- [app/src/test/java/com/tuempresa/inventariovial/ScapStorageTest.kt](D:/Downloads/GBO_InventarioVial/app/src/test/java/com/tuempresa/inventariovial/ScapStorageTest.kt)
- [app/src/test/java/com/tuempresa/inventariovial/SurveyStorageTest.kt](D:/Downloads/GBO_InventarioVial/app/src/test/java/com/tuempresa/inventariovial/SurveyStorageTest.kt)
- [app/src/test/java/com/tuempresa/inventariovial/UserAuthenticationTest.kt](D:/Downloads/GBO_InventarioVial/app/src/test/java/com/tuempresa/inventariovial/UserAuthenticationTest.kt)
- [docs/ENTREGA_RONDA_2.md](D:/Downloads/GBO_InventarioVial/docs/ENTREGA_RONDA_2.md)
- [google-apps-script/Code.gs](D:/Downloads/GBO_InventarioVial/google-apps-script/Code.gs)
- [google-apps-script/Code.test.cjs](D:/Downloads/GBO_InventarioVial/google-apps-script/Code.test.cjs)

Los artefactos generados están en `output/`; no sustituyen los originales de captura. `road_reference.json`, geometría, RoadMatcher, LinearReferenceEngine.previousPr, RoadOrdering, formatos SIC y cálculos SCAP se reutilizan. Hash de la referencia conservado: `2eef92f7112bebb32604263ea262c42267b26eac435de5109fe2cf0c3945a3e4`.

## B. Versión Room antes/después

IMPLEMENTADO / PROBADO: 10 → 11. Esquema exportado en `app/schemas/com.tuempresa.inventariovial.data.database.InventoryDatabase/11.json`.

## C. Migraciones

IMPLEMENTADO / PROBADO: `MIGRATION_10_11`, aditiva, con campos de sugerencia y GNSS fotográfico, tablas `field_prs` y `delivery_batches` e índices. Sin migración destructiva. La prueba crea una base v10 con datos en todas sus tablas y compara todas las columnas anteriores después de migrar, incluyendo fotos, registros, SCAP y seguridad. Los campos GNSS históricos quedan nulos y su fuente es `UNAVAILABLE_LEGACY`. Se conservan las cadenas de migraciones previas.

## D. Autodetección de ruta

IMPLEMENTADO / PROBADO: un registro nuevo comienza sin ruta contractual; el matcher existente evalúa las seis rutas. La tarjeta muestra candidata, confianza, distancia al eje y precisión. La ruta se aplica únicamente al confirmar (`GNSS_CONFIRMED`) o al introducirla manualmente. Con ruta existente, esta restringe la búsqueda; una discrepancia fuerte muestra advertencia, Conservar y Revisar. Coordenadas técnicas y posición sobre eje se guardan sin confirmaciones adicionales.

## E. Ruta ambigua

IMPLEMENTADO / PROBADO: se muestran las candidatas cuando el matcher global no resuelve rutas próximas. El botón de confirmación permanece deshabilitado hasta seleccionar una. Se preservan GNSS y `routeMatchAmbiguous=true`; la elección no confirma automáticamente.

## F. PR con catálogo

IMPLEMENTADO / PROBADO: reutiliza RoadPr, catálogo y previousPr. Valida las coordenadas de PR sobre el mismo eje y segmento, con tolerancia de 5 m para su medida; respeta ruta/calzada contractual aunque el eje sea EJE. Busca el PR anterior en sentido creciente y resta su chainage. Prueba: PR 0010 a 10000, PR 0015 a 15000, elemento a 12650 → **0010 + 2650 m**. Inicio y fin se calculan y confirman por separado. Si no hay PR anterior válido, no se inventa uno.

## G. Sin catálogo PR

IMPLEMENTADO / PROBADO: la referencia suministrada conserva `prs: []`. Se muestra «Autodetección de PR no disponible: falta catálogo georreferenciado de puntos de referencia». Continúan la sugerencia de ruta y los campos manuales. No se generan PR a partir de kilómetros geométricos, TDR, dkm ni fechas.

PENDIENTE CLIENTE: entregar el catálogo oficial georreferenciado con ruta, calzada, código y medida coherente con el eje. No se considera `axisMeasureM` una progresiva contractual.

## H. Registrar PR

IMPLEMENTADO / PROBADO: botón REGISTRAR PR en ubicación y edición del historial. Usa selección existente de calzada; exige código de cuatro dígitos, ruta, tipo y GNSS reciente. Guarda coordenadas originales, proyección, precisión, altitud y `FIELD_CAPTURED`. Rechaza duplicados ruta/calzada/código. Tipos: Marca/Poste, Marca/Concreto, Marca/Roca, Marca/Piedra y Virtual; no había catálogo SIC-03 de tipos incluido. RECALCULAR UBICACIONES actualiza sugerencias, sin modificar valores contractuales de registros previos.

NO PROBADO: levantamiento físico de hitos con el receptor del cliente.

## I. Confirmación Ruta + PR + Distancia

IMPLEMENTADO / PROBADO: una sola tarjeta y acción CONFIRMAR UBICACIÓN para aplicar el bloque disponible; EDITAR permite corrección manual. Si falta PR, confirmar ruta mantiene los valores manuales de PR/distancia. En elementos lineales hay una tarjeta por extremo. Se impide aplicar al fin un PR de otra ruta.

## J. Trazabilidad sugerencia / corrección manual

IMPLEMENTADO / PROBADO: se reutilizan routeCode/startPrCode/startDistanceM y sus equivalentes de fin como valores contractuales; no se duplican. Los campos suggested se guardan aparte con fuentes MANUAL, GNSS_CONFIRMED, GNSS_PR_CONFIRMED y MANUAL_OVERRIDE. La fuente anterior GNSS_PR_SUGGESTION_CONFIRMED sigue siendo compatible. Ejemplo probado: sugerencia 0010 + 2650, manual 0015 + 200; ambos se conservan. El historial muestra fuentes y sugerencia guardada. Al reanudar o editar se conserva la auditoría; una nueva confirmación explícita puede actualizarla.

## K. Estructura Drive

IMPLEMENTADO / PROBADO LOCALMENTE: captura en Room → orden geométrico → revisión → PREPARAR ENTREGA → SINCRONIZAR. Reutiliza DriveFolderRouter, endpoint/token existentes, DriveUploadWorker y WorkManager. Estructura: `RUTA/SIB/ELEMENTO N/fotos + metadata.json`; Excel SIC en `RUTA/SIB`.

Una captura tardía **antes de sincronizar** invalida la preparación; volver a preparar incluye el nuevo elemento y recalcula números sin cambiar UUID, asociaciones ni originales. El lote iniciado queda congelado y los reintentos usan sus mismas copias. Una edición/captura **después de iniciar la entrega** se bloquea como conflicto de versión para evitar renombrar o sobrescribir lo entregado.

NO PROBADO: subida contra Drive real ni ejecución en Apps Script desplegado. La configuración local de endpoint y token existe; no se imprimió ni modificó.

PENDIENTE CLIENTE: actualizar el despliegue existente de Code.gs y validar la entrega remota. Las revisiones posteriores a un lote iniciado requieren acordar una nueva versión de entrega; esta ronda no ofrece un flujo para reemplazar el lote congelado. SCAP conserva su sincronización específica por puente/inspección/revisión, sin mezclarlo con el uploader SIC ni renombrar sus carpetas existentes a PUENTE N.

## L. Ejemplo de paths generados

PROBADO: `output/DEMO-entrega-PE-22A.zip`, generado por RouteDelivery con eje real PE-22A y elementos/fotos sintéticos, contiene exactamente:

```text
PE-22A/SIB-02/SIC-18.xlsx
PE-22A/SIB-02/ALCANTARILLA 1/001_F01.jpg
PE-22A/SIB-02/ALCANTARILLA 1/002_F02.jpg
PE-22A/SIB-02/ALCANTARILLA 1/metadata.json
PE-22A/SIB-02/ALCANTARILLA 2/003_F01.jpg
PE-22A/SIB-02/ALCANTARILLA 2/metadata.json
LEEME.txt
```

No es evidencia de una subida real.

## M. Correlativo general de fotos

IMPLEMENTADO / PROBADO: `deliveryPhotoSequence` se deriva para la entrega por ruta, primero por axisMeasureM; SIB/familia y UUID desempatan, luego photoIndex. Sin medida geométrica se usa el orden de respaldo existente. `photoIndex`, UUID, asociaciones y nombres históricos guardados no se renumeran. Prueba A=10 km, B=15, C=20 y D tardío=17 → A/B/D/C con ALC-1/2/3/4.

## N. LEGACY_COMPATIBLE

IMPLEMENTADO / PROBADO: generador único DeliveryPhotoNameBuilder; formato correlativo_ruta_tipo_lado_ubicación.jpg con datos actuales. Ejemplo: `1_PE-22A_ALC_D_0040+684.jpg`. No se atribuye esta convención al TDR ni se usa LI-100 como ruta.

PENDIENTE CLIENTE: ingeniería debe confirmar si desea esta convención histórica; el ejemplo supera 20 caracteres y no se permite sincronizarlo así.

## O. FOLDER_COMPACT

IMPLEMENTADO / PROBADO: opción predeterminada `001_F01.jpg`, `002_F02.jpg`, `003_F01.jpg`. Contexto en carpetas y metadata, con correlativo general e índice local. Los archivos originales no se renombran.

## P. Límite de 20 caracteres

IMPLEMENTADO / PROBADO: FileNameComplianceValidator cuenta la extensión, rechaza componentes inseguros y avisa cuando el nombre excede 20 caracteres. UI y sincronización bloquean nombres incompatibles; Apps Script vuelve a validarlos. No hay truncamiento silencioso. Cambiar a FOLDER_COMPACT y preparar de nuevo resuelve el conflicto habitual del modo histórico.

## Q. EXIF

IMPLEMENTADO / PROBADO: JPG procesado escribe/preserva GPSLatitude/Ref, GPSLongitude/Ref, GPSAltitude/Ref y DateTimeOriginal cuando existen. ImageDescription contiene ruta, SIB, SIC, código corto, PR/distancia, axis y UUID de registro/foto. UserComment del watermark se conserva y originalPath permanece intacto, incluso si una importación ya contenía el sello.

GPSHPositioningError se escribe cuando la ExifInterface disponible lo admite. En SDK 28 no se admite ese tag; la precisión permanece en Room, metadata y payload. NO PROBADO: escritura de ese tag en otro Android compatible ni EXIF en cámara física.

## R. metadata.json

IMPLEMENTADO / PROBADO: uno por elemento, con identidad, familia, SIC/SIB, ubicación contractual, sugerencia, fuentes, posición geométrica, GNSS del registro y array de fotos con nombres, correlativos y SHA-256. Se genera mediante campos explícitos; no incluye rutas privadas ni credenciales. Los hashes de la demo fueron verificados contra los JPG contenidos. Ejemplo separado: `output/metadata-DEMO-PE-22A.json`.

## S. GNSS propio de cada foto

IMPLEMENTADO / PROBADO: muestra independiente al finalizar la cámara; acepta fix válido dentro de 30 s del instante de captura registrado. Si no llega fix, la foto se conserva con campos nulos y aviso no bloqueante. Importaciones leen EXIF propio; no usan el GNSS actual como si fuese el de la imagen. Fotos históricas sin dato quedan UNAVAILABLE_LEGACY. Dos fotos de un mismo elemento conservan coordenadas diferentes. La ubicación del elemento nunca rellena automáticamente ambas. Se integra en captura SIC y SCAP; el manifiesto SCAP también conserva los campos propios, sin cambiar sus cálculos ni su protocolo.

NO PROBADO: precisión y latencia del hardware GNSS/cámara real.

## T. Apps Script

IMPLEMENTADO / PROBADO LOCALMENTE: añade uploadDeliveryFile y anuncio deliveryProtocol=1 en doGet, manteniendo autenticación y handlers SIC/SCAP. Valida ruta, SIB, elemento, nombres, UUID, MIME, hashes y metadata; rechaza traversal y material privado. El trabajador comprueba la capacidad del servidor antes de enviar para evitar que un despliegue antiguo trate el lote como una subida legacy.

PENDIENTE CLIENTE: actualizar el **despliegue existente** con `google-apps-script/Code.gs`, conservando su configuración vigente de raíz/token. Sin ese despliegue, el nuevo lote se bloquea con aviso antes del primer archivo.

## U. Idempotencia

IMPLEMENTADO / PROBADO: copia local inmutable y hash por archivo; identidad remota por UUID de foto, hash, carpeta y nombre. LockService y propiedades resuelven la carpeta lógica y evitan colisiones. Pruebas de respuesta perdida, subida parcial, reintento con mismos bytes y nombres, hash alterado, recibo incorrecto y servidor antiguo. Dos UUID diferentes no pueden adueñarse del mismo nombre. No se crean sufijos (1)/(2) por retry.

NO PROBADO: concurrencia/reintentos contra el servicio real; las pruebas usan transporte y Drive simulados.

## V. KML

IMPLEMENTADO / PROBADO: se conserva el eje amarillo ff00ffff ancho 5 y borde negro ff000000 ancho 8, con ambas LineString exactamente iguales. INICIO verde y FIN rojo a escala 1.4; colores de inventario, nombres cortos y LEYENDA lateral conservados. ExtendedData incorpora ubicación confirmada/sugerida y fuentes. Las seis rutas y DEMO se empaquetaron y analizaron como XML; se verificaron estilos, geometría duplicada y CRC ZIP.

NO PROBADO: apariencia final en el cliente concreto de Google Earth. Artefacto: `output/GBO_ejes_KML_KMZ.zip`.

## W. ScreenOverlay

IMPLEMENTADO / PROBADO: ninguno de los siete KMZ de entrega contiene ScreenOverlay. LEYENDA incluye EJE, ALC, SV, P, CUN, BAD y ELM; Document.description ofrece respaldo textual. legend.png sigue como recurso opcional, sin dependencia funcional.

## X. Pruebas Android

PROBADO: **196 pruebas, 0 fallos, 0 errores, 0 omitidas** mediante testDebugUnitTest; incluyen Compose/Robolectric, migraciones, matcher/PR, identidad y orden, EXIF, metadata, lotes, reintentos, SIC, SCAP y autenticación. compileDebugAndroidTestKotlin también completó correctamente. Informe HTML: `app/build/reports/tests/testDebugUnitTest/index.html`.

PROBADO adicional: **17 pruebas del servicio de usuarios**, incluyendo credenciales, roles, revocación, bloqueo e integridad de auditoría.

NO PROBADO: instrumentación ejecutada en dispositivo/emulador; `adb devices -l` no encontró dispositivos. Los tests compilados no se presentan como pruebas físicas.

## Y. Pruebas Apps Script

PROBADO: **21/21**, 0 fallos: ruta/SIB/elemento, múltiples fotos, dos elementos, metadata, SIC, SCAP, reintentos, hashes, token inválido, traversal, conflictos y bloqueo concurrente. Comando: `node --test google-apps-script/Code.test.cjs`.

## Z. assembleDebug

PROBADO: `gradlew.bat testDebugUnitTest assembleDebug compileDebugAndroidTestKotlin --console=plain` terminó BUILD SUCCESSFUL. APK generado: `output/GBO_InventarioVial_Ronda2-debug.apk` (22 183 240 bytes).

SHA-256: `d7e96c9e3ebf40e31d186b5ddca0465d4a6643cbd6ff1b7146efe71c7b44455b`.

Evidencias y hashes de artefactos: `output/validacion-ronda2.json`. Cambios disponibles para revisión humana, sin commit ni push.
