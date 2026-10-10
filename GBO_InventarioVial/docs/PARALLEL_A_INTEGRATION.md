# PARALLEL A — contrato de integración con ramas B / C

## Colisiones probables

| Archivo | Cambio A | Acción al integrar |
|---|---|---|
| `MainActivity.kt` | Solo `CoreFieldTools(it)` después de `DriveStatusPanel(it)` | Conservar línea y evitar sobrescribir pantallas B/C. |
| `data/dao/InventoryDao.kt` | `observeAllDeliveries()` de solo lectura | Fusionar query si B/C agregan consultas. |
| `delivery/DeliverySync.kt` | `DriveSyncClock.attempt/success` tras ACK | Mantener el protocolo existente; volver a incluir telemetría tras merge. |
| `road/RoadReferenceRepository.kt` | Aplica overlay KMZ para ejes de ruta seleccionados con fallback | Unificar con cualquier carga dinámica B/C; validar PR y CRS. |
| `res/xml/file_paths.xml` | Nuevos paths FileProvider | Fusionar, no reemplazar configuraciones existentes. |

Nuevas rutas `core/*` y `road/KmzRouteImport.kt` son archivos nuevos, con colisión improbable.

## Room

- Versión **12** sin modificar.
- **Tablas, columnas, índices, SQL de migración propuesto: ninguno.**
- Se añadió una query `SELECT * FROM delivery_batches ORDER BY createdAt DESC` a `InventoryDao` sin alterar el esquema.
- No se creó `GeneratedFile` en Room porque `GeneratedFileIndex` indexa exportaciones ya presentes y sus URI; cuando B/C requieran un registro duradero, coordinar una migración conjunta después del merge.

## Contratos de CORE

### DriveAssetPathRouter

`DriveAssetPathRouter.path(AssetPathRequest(route,assetFamily,assetSequence,photoCategory,documentType,pavementSurface))` devuelve una ruta lógica, nunca crea carpetas remotas ni cambia entradas anteriores. `photoFolder(...)` usa `GENERAL` si no existe categoría explícita; `PhotoCategory` viene de UI o de `PhotoEntity.photoCategory` (ya existente en Room). Ejemplo `PE-22A/ALCANTARILLAS/ALCANTARILLA_001/ENTRADA`.

**Acción B/C:** suministrar IDs de elementos estables por ruta y nomenclatura familiar (no inferir de filenames); modificar los adaptadores del backend Drive para aceptar ruta lógica nueva con un modo de compatibilidad SIB, migrar lotes revisados/versionados. No modificar `DeliveryNamingPolicy` sin test de compatibilidad.

### DriveSyncCenter / SyncOverview

`UserSyncState`, `SyncOverview.status/canRetry/counts`; `DriveDeliveryRetry.enqueue` solamente admite lotes `QUEUED` o `ERROR` con manifiesto local y configura WorkManager `NetworkType.CONNECTED`; `BLOCKED` o `PREPARED` **no** son reintentados automáticamente. `DriveSyncClock` registra intento/ACK de SIC en SharedPreferences.

**Acción B:** expone estados del controlador SCAP y sus ACK de manera independiente. **Acción C:** estados de entrega por pavimento. El centro actual es intencionadamente conservador.

### GeneratedFileIndex

`GeneratedFile` describe id, familia, ruta local, MIME, timestamp y sync, sin duplicar Room. `discover(context)` encuentra exportaciones en cache y entregas `filesDir/route_deliveries`; `open/share` generan `content://` con FileProvider. Incluir rutas conocidas al integrar nuevos exportadores.

### PhotoCaptureCard / PhotoPreview

`PhotoCaptureCard(title,photos,onTakePhoto(cameraId),onSelectFile,onDelete)`: callbacks del módulo mantienen transacciones Room y galería. `PhotoPreview(file,onClose)` abre imagen sin modificar el formulario. No se agregó un stack UVC/Wi-Fi. `FieldCameraCatalog` usa Camera2 `LENS_FACING_EXTERNAL` solo si Android la expone.

**Acción B/C:** adaptar composables de fotos existentes para aceptar N fotos y categoría explícita, transferir fotos válidas al repositorio actual, conservar EXIF + metadata Room, no obligar sello visual nuevo.

### LocationSummaryCard / RecordReviewSection

`LocationSummaryCard(latitude,longitude,accuracyM,pr,route,source,technicalActions)` y `RecordReviewSection(title,fields,photos,completion)` son composables sin políticas de formularios. `CompletionResult` distingue guardado de borradores y finalización; consumidores aportan sus propias reglas de calidad/mapping.

### KMZ por SAF

`KmzRouteParser.parseKmz` abre únicamente KML del ZIP (sin escritura arbitraria) y valida coordenadas, tamaño, DOCTYPE y cantidad. `KmzGeometryStore.stage(uri,route,source)` prepara; `activate(preview)` conserva KML con SHA y cambia puntero solo con confirmación. `RoadReferenceRepository.loadFromAssets()` combina segmentos activados de cada ruta, conserva demás rutas, y retrocede al asset si falla la carga. No altera `road_reference.json` ni hace sincronización remota.

**Precaución:** el kilometraje `chainageM` de KMZ se calcula desde 0 por segmento. Para rutas con PR/cadena contractual, hay que validar origen/sentido/eje antes de aprobar la geometría; las ramas no deben asumir equivalencia con PR. Los archivos se conservan en `filesDir/road_geometry_versions/<route>/<sha256>.kml`.

### Fotos limpias

`CleanPhotoPolicy.original(PhotoForDelivery)` devuelve el JPG original sin recomprimir/estampar y `byCategory` agrupa fotos N:1. **No está conectado a la exportación heredada `RequiredWatermark`**; integrarlo exige modificar las expectativas de entrega e informar a Apps Script y a los tests sobre marca/logo. No eliminar la copia original ni metadata Room/JSON.

## Riesgos críticos para integración y campo

1. No reemplazar carpetas Drive SIB publicadas ni omitir revisión del manifiesto; cero migraciones destructivas.
2. No instalar esta rama como actualización sin `assembleDebug`, firma anterior y prueba de upgrade de base v12.
3. No tratar `QUEUED` como `SYNCED`: solo un ACK remoto puede establecer sincronizado.
4. No tratar geometría KML importada como PR oficial sin contrastar eje, progresivas y orientación.
5. No considerar backup completo antes de que datos + fotos hayan sido recibidos en una nube accesible fuera de una tablet perdida.

## Paso a paso

1. Integrar cambios mínimos de los cinco archivos existentes y conservar `core/*`.
2. Ejecutar Android/Gradle con red/cache y comprobar 249 baseline + nuevas pruebas, sin fallos nuevos respecto de los 15 anteriores.
3. Probar rutas Drive históricas y protocolo `deliveryProtocol` con copia de staging del Apps Script.
4. B/C adoptan `PhotoCategory`, `PhotoCaptureCard`, `RecordReviewSection`, `LocationSummaryCard`, `GeneratedFileIndex` mediante adaptadores propios.
5. Planear en conjunto una nueva versión de carpeta y entrega sin pérdida de trazabilidad; no cambiar la v12 de Room de forma aislada.
6. Testear cámara integrada/externa real y reconexión después de caídas de Starlink; simular pérdida de red y verificar que el registro original permanece local.
