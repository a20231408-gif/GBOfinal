# PARALLEL A — CORE / plataforma común

**Base:** `GBO_USUARIOS_CORREGIDO_2026-10-07(1).zip`. **Entrega:** `GBO_PARALLEL_A_CORE.zip`. Fecha: 2026-10-07/08.

## Estado y alcance real

Se incorporó infraestructura transversal, no una reescritura de SIC, SCAP o Pavimentos. Las operaciones críticas de Usuarios y su backend corregido permanecen sin cambios. **Este cambio aún NO es una APK compilada ni validada en una tablet.** No hay credenciales de servidor añadidas ni migraciones destructivas.

### Cambios a archivos existentes

- `app/src/main/java/com/tuempresa/inventariovial/MainActivity.kt`: **una entrada mínima** (`CoreFieldTools`) en Inicio, junto al estado Drive previo.
- `app/src/main/java/com/tuempresa/inventariovial/data/dao/InventoryDao.kt`: nueva consulta de solo lectura `observeAllDeliveries()`. Sin alteración de tablas.
- `app/src/main/java/com/tuempresa/inventariovial/delivery/DeliverySync.kt`: registra en preferencias el último intento y ACK exitoso de lotes SIC. No altera el protocolo HTTP/Drive.
- `app/src/main/java/com/tuempresa/inventariovial/road/RoadReferenceRepository.kt`: combina la geometría KMZ activada de una ruta con los ejes originales de las demás rutas, con fallback al JSON incluido si la versión está corrupta o ausente.
- `app/src/main/res/xml/file_paths.xml`: añade rutas seguras para compartir exportaciones internas y paquetes de entrega.

### Nuevos componentes

- `core/DriveAssetPathRouter.kt`: esquema canónico ruta/familia/elemento/(superficie)/categoría/(documento), con entrada `AssetPathRequest` y `PhotoCategory`, sin inferir categoría del nombre del JPG. No altera las carpetas SIC/SIB ya publicadas.
- `core/SyncOverview.kt`: estados legibles, reglas de reintento conservadoras, `ValidationIssue` y `CompletionResult` (guardar borrador separado de finalizar).
- `core/DriveSyncCenter.kt`: vista de pendientes/error, entregas históricas, último intento y última confirmación SIC, reintento controlado de lotes revisados; no envía SCAP ni lotes en `PREPARED`/`BLOCKED`.
- `core/GeneratedFileIndex.kt`: índice de Excel/PDF/ZIP en ubicaciones de exportación conocidas, apertura y uso compartido mediante `FileProvider` y URI `content://`, sin rutas `file://`.
- `core/CoreFieldTools.kt`: botones en Inicio para Centro Drive, Archivos generados e Importar KMZ. SAF para seleccionar KMZ y confirmación separada antes de activar nueva versión.
- `core/FieldCommonUi.kt`: `PhotoCaptureCard`, `PhotoPreview`, miniaturas, selector/descubrimiento de cámaras Camera2, `LocationSummaryCard`, `ValidationSummaryCard`, `RecordReviewSection`; contratos para consumo por B/C, sin editar los formularios técnicos.
- `core/CleanPhotoPolicy.kt`: política de imágenes ORIGINALES sin alterar los píxeles, con metadata externa explícita, para que B/C migren sus entregas una vez aprobado el cambio de contrato.
- `road/KmzRouteImport.kt`: importador KMZ/KML seguro, con límites, XML sin DTD/entidades externas, geometría por ruta, SHA-256, versiones persistentes, activación explícita y fallback.

### Tests añadidos (7 archivos)

1. `core/DriveAssetPathRouterTest.kt`: una carpeta por elemento, categorías independientes, pavimento, rechazo de `../` y componentes no válidos.
2. `core/SyncOverviewTest.kt`: estados, errores reintentables, `BLOCKED`, borrador vs finalizar.
3. `core/CleanPhotoPolicyTest.kt`: N fotos por categoría, grupos separados, imagen original sin alterar.
4. `core/KmzRouteParserTest.kt`: ZIP/KML válido, hash, geometría, rechazo de entrada inválida y XXE.
5. `core/KmzGeometryStoreTest.kt`: versiones, activación y KML inválido no reemplaza versión activa.
6. `core/GeneratedFileIndexTest.kt`: descubre exportaciones y excluye archivos que no deben compartirse.
7. `core/FieldCameraCatalogTest.kt`: enumeración segura con Camera2 (Robolectric).

### Resultado de pruebas ejecutadas

- `./gradlew --offline testDebugUnitTest`: **NO ejecutable**. El wrapper requiere Gradle `9.6.0`; esta imagen no tiene la distribución ni acceso DNS a `services.gradle.org`. El intento terminó con `java.net.UnknownHostException: services.gradle.org`. Por ello NO se puede afirmar que los 15 fallos sigan siendo 15 ni que no haya regresiones Android.
- `./gradlew assembleDebug`: **NO ejecutable**, mismo bloqueo del wrapper; no se generó APK.
- Kotlin JVM independiente (`kotlinc` 1.9, stubs de Context para clases sin SDK): **PASS**, comprobaciones funcionales sobre router, sync, borradores, lectura válida de KMZ, rechazo de inputs inválidos y entidades XML externas. **No sustituye la compilación Android ni los tests JUnit**.
- `node backend/user-service/tests/service.test.cjs`: **PASS, 21/21**, sobre el backend de Usuarios intacto.
- No se realizaron pruebas instrumentadas con tablets, cámara física, USB, Starlink, Drive publicado ni el Apps Script de producción.

### Baseline histórico de la auditoría previa

El baseline reportado por el equipo, antes de la presente rama, es **249 tests, 15 fallos conocidos y `assembleDebug` exitoso**. **No ha sido posible volver a verificar este baseline en este entorno**. La auditoría anterior identificó los siguientes 15 tests como los existentes, no como fallos nuevos:

1. `Engineering2DataTest.sevenPiersFixedStructuresAndObservationsSurviveReopenWithoutMaterializingReminders`
2. `Engineering2ScreenTest.f2ShowsPendingDefaultsAndEditingAnObservationPersistsWithoutAssertingPresence`
3. `EngineeringUxTest.versionSixMigrationKeepsAllHistoricalDataAndPromotesLegacyJoint`
4. `InventoryRoomMigrationTest.migrationsFromEveryReleasedVersionPreserveRecordsPhotosAndSic23`
5. `PrSuggestionStorageTest.migrationNineToTenRetainsRecordsPhotosAndStartSourceAndDefaultsEndToManual`
6. `RoadAxisStorageTest.realVersionEightMigrationPreservesManualPrPhotosAndDetailsAndNewFieldsStartNull`
7. `RoadReferenceDeliveryTest.categoryStylesShortNamesAndExtendedDataPreserveManualAndGeometricValues`
8. `RoadReferenceDeliveryTest.compatibleAxisHasYellowTopBlackBorderIdenticalGeometryAndSidebarLegend`
9. `RoundTwoStorageTest.migration10to11PreservesEveryColumnOfEveryLegacyTableIncludingSecurityAndScap`
10. `ScapExportTest.runtimeIncludesExactTemplateAndExportPreservesSheetsStylesGeometryAndCalculationFormulas`
11. `ScapFullExportTest.SCAP_COMPLETO_TEST_reopensWithAllFieldsSevenDistinctPiersAndOriginalGeometry`
12. `ScapStorageTest.versionFiveMigrationPreservesSicRecordsPhotosMetadataAndNewCulvertFields`
13. `SurveyStorageTest.versionFourUpgradeRetainsOldCulvertDimensionsAndMarksNewFieldsUnknown`
14. `UserAuthenticationTest.fieldSessionOperatorComesFromAuthenticatedUser`
15. `UserAuthenticationTest.migration7to8PreservesEveryExistingTable`

Esta rama no ha alterado esos tests, las migraciones de Room ni los componentes de autenticación.

## Limitaciones / trabajo que NO está terminado

- **No se ha activado el nuevo router de carpetas en entregas existentes:** el backend Drive actual consume `SIB` y carpetas contractuales; imponer una ruta diferente rompería compatibilidad. B/C deben conectar categorías y elementos explícitos luego de acordar compatibilidad con Apps Script y migración de archivos existentes.
- **Centro de sincronización:** cubre entregas SIC revisadas, no sincronización SCAP/Pavimentos. No representa como ACK remoto a fotos que simplemente están guardadas en Room.
- **Backup ante pérdida de tablet:** sigue siendo **local primero** y sincronización offline-aware mediante WorkManager para entregas autorizadas. No se habilitó una subida nueva sin revisión ni un backup cloud continuo adicional; requiere contrato remoto/seguridad y participación B/C.
- **Fotos limpias:** la nueva política existe pero los flujos heredados usan `RequiredWatermark` y el backend/documentación anteriores aún exigen sello corporativo. **El requisito de exportar todas las fotos sin marcas visibles NO está implementado para los formatos heredados**: cambiarlo unilateralmente podría romper entregas; se requiere actualizar contrato + pruebas de aceptación simultáneamente.
- **PhotoCaptureCard:** enumera las cámaras que Camera2 expone y transmite `cameraId` al host. No controla por sí solo el selector de lentes de las actividades Android externas; B/C deben integrar captura Camera2/CameraX específica si quieren abrir una cámara USB concreta.
- **KMZ:** la geometría importada lleva medida geométrica acumulada desde el primer vértice, **no reemplaza la red de PR contractual** y no agrega PR al KML. Debe verificarse alineación de abscisas y rutas en tablet antes de utilizar para inventario oficial. No se incorporó Google Earth embebido.
- **Archivos generados:** se descubren mediante índice de carpetas conocidas; las nuevas salidas de B/C deberán registrar/ubicar sus PDF/Excel en una de ellas o ampliar el adaptador. DocumentsUI no permite abrir necesariamente directorios privados de apps Android.
- **GNSS/revisión/validación:** componentes reutilizables creados, pero no insertados en formularios SIC/SCAP/Pavimentos para no generar conflictos entre ramas.
- **Room:** ninguna tabla nueva, ninguna columna ni índice, versión 12 intacta, ninguna migración nueva ni `fallbackToDestructiveMigration`.

## Antes de integrar / instalar

1. Ejecutar `./gradlew testDebugUnitTest` y comparar el **conjunto exacto de nombres** de fallos con el baseline de 15; cualquier fallo extra requiere corrección.
2. Ejecutar `./gradlew assembleDebug` y pruebas de instalación de actualización firmada con la **misma clave** de la versión de Usuarios ya probada. No borrar los datos de aplicación.
3. Probar Inicio → Herramientas de campo → Drive/Archivos/KMZ en tablet y validar KML de ruta y distancias antes de usar una nueva geometría.
4. Integrar gradualmente adapters B/C, respaldar base Room y revisar Drive sin reescribir rutas históricas.
