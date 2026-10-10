# Nuevos SIC y selector visual — reporte de entrega

Base exclusiva: `GBO_InventarioVial_INTEGRADO_FINAL_SOURCE.zip`, integración A+B+C. Se verificó que las fuentes Android del directorio de trabajo coincidían con el ZIP antes de modificarlas. No se usaron ramas antiguas, no hubo commit ni push. Fecha: 8 de octubre de 2026.

## Resultado funcional

Se integraron SIC-08, SIC-09, SIC-13, SIC-34 y el complemento SIC-18A. Dashboard agrupado, borradores persistentes en Room, reapertura desde historial, revisión CORE antes de finalizar, consulta de finalizados y exportación XLSX offline con abrir/compartir.

Los campos compartidos se precargan desde el recorrido actual cuando hay contexto confiable de la misma sesión/ruta/calzada. Se reutilizan inicio/fin/fecha de una captura nueva finalizada cuando el contexto sigue siendo compatible. No se convierten datos incompletos de borrador en un contexto confirmado. SIC-09 hereda por UUID de un SIC-08; si existe una sola faja compatible del contexto se selecciona automáticamente, y con varias permite elegir. La ficha fuente no se modifica. SIC-18A selecciona o reutiliza el SIC-18 padre y comparte UUID, carpeta y fotos; condición 3 muestra sugerencia sin crear el complemento automáticamente.

SIC-09 conserva el texto y precisión de los anchos; exporta valores numéricos con formato `0.0`. Ancho total menor que útil permite borrador pero bloquea finalización/exportación. Decimales parcialmente escritos se conservan en borrador y no se finalizan hasta quedar completos.

## Columnas contractuales

Orden exacto de la tabla. Los pares PR/Distancia se agrupan bajo ubicación inicio/fin; no se añaden UUID, source, inspector ni estados internos.

| Formato | Columnas en orden | Cantidad |
|---|---|---:|
| SIC-08 | Ruta; Calzada; Faja; Código PR inicio; Distancia inicio; Código PR fin; Distancia fin; Tipo; Sentido del tránsito; Fecha | 10 |
| SIC-09 | Ruta; Calzada; Faja; Código PR inicio; Distancia inicio; Código PR fin; Distancia fin; Ancho útil (m); Ancho total (m); Fecha | 10 |
| SIC-13 | Ruta; Calzada; Código PR inicio; Distancia inicio; Código PR fin; Distancia fin; Lado; Clase; Fecha | 9 |
| SIC-34 | Ruta; Calzada; Faja; Código PR inicio; Distancia inicio; Código PR fin; Distancia fin; Tipo; Nivel; Descripción; Fecha | 11 |
| SIC-18A | Ruta; Calzada; Código PR inicio; Distancia inicio; Clase; Tipo; Ojos / Vanos; Función; Ubicación de Falla; Tipo de Falla; Estado; Causa probable; Fecha | 13 |

Se reutilizan `SicExcelWriter` y `SicWorkbookHeaderWriter`, con ambos logos originales. El encabezado compartido se adapta a nueve/diez columnas sin agregar columnas a la tabla; mantiene el comportamiento previo para once o más. Los cinco ejemplos XLSX sintéticos se abrieron con openpyxl: cantidad de columnas, fecha numérica, precisión de anchos, formato `0.0`, dos logos, relaciones ZIP válidas y merges dentro de la tabla sin solapamientos. Evidencia: `sic-new-evidence/workbook-validation.json`.

## Mappings y discrepancias

- SIC-08: Tipo 1 Tránsito, 2 Berma, 3 Peaje, 4 Cruce, 5 Localidad, 6 Paradero; Sentido C/D/A.
- SIC-13: Lado D/I; Clase 15/16/17/17A/17B/17C/17D. Ayuda breve colapsable por clase. Para 17D se utiliza “Zonas de Riesgo Probable” del gráfico; la Tabla III.13 dice “Riesgo Elevado”. El gráfico no incluye la Faja mencionada en la descripción: no se agrega esa columna.
- SIC-34: Tipo 1/2/3/4/5/7; Nivel 1 Bajo, 2 Medio, 3 Alto, con explicación textual. Descripción libre y multilínea, opcional según características del formato. La búsqueda completa de MAN_8 no resolvió el 5 duplicado de problemas geotécnicos/geológicos. Se persiste `ACCIDENT_TYPE_SOURCE_CONFLICT`, borrador permitido y finalización/exportación bloqueadas. No se inventa 6.
- SIC-18A: Función 1–4; Ubicación falla 1–5; Tipo falla 1–7; Estado 1–3; Causa 1–4. Clase/Tipo/Ojos-Vanos se heredan del padre. Se conserva tipo 4 Otro artesanal del SIC-18/Tabla III.24 aunque el gráfico SIC-18A lo omita.

La matriz completa de campos, fuentes, obligatoriedad y páginas está en `SIC_08_09_13_18A_34_SOURCE_MAP.md`; las discrepancias en `MAN8_SOURCE_INCONSISTENCIES.md` y `SIC34_TYPE_CODE_PENDING_DECISION.md`.

## Room e identidad

Room final **15**, migración explícita `MIGRATION_14_15`; `app/schemas/...InventoryDatabase/15.json` incluido. Se necesitó una tabla extensible porque los detalles de inventario existentes eran entidades tipadas sin un JSON genérico para estos SIC. La nueva `sic_extension_details` guarda texto crudo de borrador, formato, estado, operador y referencia fuente. Evita reutilizar JSON exclusivo de SCAP/Pavimentos y conserva `sic18a_details` para compatibilidad.

`recordId` es PK y FK al inventario; `sourceRecordId` es FK real con índice. SIC-09 usa UUID de SIC-08. SIC-18A usa el mismo UUID de SIC-18 y la referencia al padre. No hay migración destructiva. Las migraciones 12→13 y 13→14 y los schemas existentes 4–14 se compararon con el ZIP original. Las pruebas migran toda la información de todas las tablas/columnas de 14 a 15 y reabren la base; también se mantiene el recorrido integrado 12→13→14→15.

## Señales y memoria

**207 imágenes WebP, 207 verificadas, cero códigos sin imagen** en el catálogo existente. Todos los gráficos proceden de MAN_6; no se incluyó el PDF en runtime ni se generaron señales artificiales. Manifest por código con páginas, rectángulos, método, dimensiones y hash. Auditoría HTML autónoma con imágenes incrustadas para revisión humana offline. I-2A muestra la variante Nacional y sigue puntual.

`TrafficSignVisualSelector` es reutilizable y se utiliza en `SignalCodeFields` para los flujos de captura/edición SIC-22 existentes. Búsqueda por código/nombre/descripción, LazyColumn, thumbnails ≤320 px, decodificación asíncrona, caché de 4 MiB y descripción accesible. Manual/sin identificar no toman imágenes por coincidencia textual. Detalles en `SIGNAL_ASSET_EXTRACTION.md`.

## Drive y archivos

Se registran CALZADA_FAJAS, PUNTOS_CRITICOS y ACCIDENTES usando `DriveAssetPathRouter`/`DrivePaths`; SIC-18A.xlsx se incorpora a la carpeta del padre. Los borradores no se exportan como documentos finalizados. Se conservan token, protocolo 1/pathVersion 2, validación de ACK, hashes, idempotencia e identidad persistente. No se modificó `DeliverySync`; el dashboard lee su resultado confirmado, sin asumir que un archivo local ya está sincronizado.

El servidor incluido registra los cinco códigos y tres familias en `google-apps-script/Code.gs`. **No se desplegó Apps Script ni se realizaron envíos remotos.** El administrador debe aplicar esas dos listas al servidor autorizado antes de probar el envío real. `GeneratedFileIndex` reconoce los cinco formatos en las ubicaciones existentes y conserva la verificación de recibos para estado Drive.

## Validación y evidencias

El baseline previo se ejecutó antes de modificar producción: 323 total, 308 passed, 15 failed. La identidad exacta de cada fallo está en `sic-new-evidence/baseline.json` y sus XML en `baseline-tests`.

| Validación | Total | Passed | Failed | Baseline failures | New failures |
|---|---:|---:|---:|---:|---:|
| Baseline antes de producción | 323 | 308 | 15 | 15 | 0 |
| Suite final | 347 | 332 | 15 | 15 | 0 |
| Pruebas nuevas | 24 | 24 | 0 | 0 | 0 |
| User Service | 21 | 21 | 0 | 0 | 0 |
| Apps Script | 39 | 39 | 0 | 0 | 0 |

`gradlew testDebugUnitTest --no-configuration-cache`: ejecutado íntegramente; Gradle termina BUILD FAILED por los 15 fallos históricos autorizados, con las mismas identidades y cero fallos nuevos. No se deshabilitaron pruebas históricas. Se actualizaron únicamente cadenas de migración y expectativas de la versión final en las pruebas que antes pasaban, y se conserva el test de los siete contratos XLSX originales separado de los nuevos formatos.

`gradlew assembleDebug --no-configuration-cache --rerun-tasks`: **BUILD SUCCESSFUL**. APK debug verificado: clases de los nuevos formatos y módulos originales, 207 assets con hashes del manifest, plantillas canónicas SCAP LOSA/MODULAR, sin PDF runtime y sin credenciales privadas.

APK SHA-256: `522023f89f48f459aee66a74132aa4c57c62a28882ac9ac5eeee706ec40eabbb`.


Comandos Gradle ejecutados con JAVA_HOME del JBR instalado y `-PgboValidationBuild=true` para no incluir endpoints/credenciales privados de esta máquina. Las líneas de validación y sus logs finales se registran en `sic-new-evidence`. El APK debug de validación conserva el flujo de autenticación; para usarlo en campo se necesita el usuario autorizado/caché existente. No se creó keystore ni se firmó release.

## Alcance protegido y limitaciones

No se modificaron fuentes de User Service, autenticación/token/device, cálculos PCI/URCI, SCAP LOSA/MODULAR, SIC-17A/17B ni archivos de KMZ. `CleanPhotoPolicy`, cámara y categorías existentes se reutilizan. El catálogo compartido solo añade las familias necesarias para integrar la navegación/entrega de los nuevos formatos. La comparación de hashes y el inventario de cambios están en `sic-new-evidence/changed-files.json`.

No se ejecutó la aceptación en tablet física, ni cámara física ni sincronización con Drive real. Estas comprobaciones están pendientes del usuario en `SIC_NEW_FORMATS_TABLET_CHECKLIST.md`. Las pruebas de selector/scroll/memoria son Compose/Robolectric y caché, no una medición del dispositivo físico. La equivalencia contractual del tipo geotécnico SIC-34 sigue pendiente conforme al pedido.

## Archivos modificados y añadidos

Archivos existentes modificados (26):

- `app/src/androidTest/java/com/tuempresa/inventariovial/SicExportScreenTest.kt`
- `app/src/main/java/com/tuempresa/inventariovial/DriveFolderRouter.kt`
- `app/src/main/java/com/tuempresa/inventariovial/MainActivity.kt`
- `app/src/main/java/com/tuempresa/inventariovial/RecordHistoryScreen.kt`
- `app/src/main/java/com/tuempresa/inventariovial/SicExportScreen.kt`
- `app/src/main/java/com/tuempresa/inventariovial/core/GeneratedFileIndex.kt`
- `app/src/main/java/com/tuempresa/inventariovial/data/dao/InventoryDao.kt`
- `app/src/main/java/com/tuempresa/inventariovial/data/database/InventoryDatabase.kt`
- `app/src/main/java/com/tuempresa/inventariovial/data/entity/InventoryRecordForExport.kt`
- `app/src/main/java/com/tuempresa/inventariovial/data/entity/InventoryRecordWithPhotos.kt`
- `app/src/main/java/com/tuempresa/inventariovial/data/entity/InventorySnapshot.kt`
- `app/src/main/java/com/tuempresa/inventariovial/delivery/RouteDelivery.kt`
- `app/src/main/java/com/tuempresa/inventariovial/export/LocalSicExport.kt`
- `app/src/main/java/com/tuempresa/inventariovial/export/SicExcelWriter.kt`
- `app/src/main/java/com/tuempresa/inventariovial/export/SicExportFormat.kt`
- `app/src/main/java/com/tuempresa/inventariovial/export/SicWorkbookHeaderWriter.kt`
- `app/src/main/java/com/tuempresa/inventariovial/field/FieldUi.kt`
- `app/src/main/java/com/tuempresa/inventariovial/field/SignalCodeFields.kt`
- `app/src/main/java/com/tuempresa/inventariovial/road/AssetPresentationCatalog.kt`
- `app/src/test/java/com/tuempresa/inventariovial/BridgeSicFlowTest.kt`
- `app/src/test/java/com/tuempresa/inventariovial/IntegratedMigration12To14Test.kt`
- `app/src/test/java/com/tuempresa/inventariovial/PavementModuleTest.kt`
- `app/src/test/java/com/tuempresa/inventariovial/PavementParallelCTest.kt`
- `app/src/test/java/com/tuempresa/inventariovial/SicExcelWriterTest.kt`
- `google-apps-script/Code.gs`
- `google-apps-script/Code.test.cjs`

Fuentes/herramientas nuevas:

- `app/src/main/java/com/tuempresa/inventariovial/data/entity/SicExtensionEntity.kt`
- `app/src/main/java/com/tuempresa/inventariovial/field/TrafficSignVisualSelector.kt`
- `app/src/main/java/com/tuempresa/inventariovial/sic/SicCaptureContext.kt`
- `app/src/main/java/com/tuempresa/inventariovial/sic/SicDeliveryStatus.kt`
- `app/src/main/java/com/tuempresa/inventariovial/sic/SicFieldFormats.kt`
- `app/src/main/java/com/tuempresa/inventariovial/sic/SicFieldRepository.kt`
- `app/src/main/java/com/tuempresa/inventariovial/sic/SicFieldWorkspace.kt`
- `app/src/test/java/com/tuempresa/inventariovial/SicMigration14To15Test.kt`
- `app/src/test/java/com/tuempresa/inventariovial/SicNewFormatsTest.kt`
- `app/src/test/java/com/tuempresa/inventariovial/SignalAssetCatalogTest.kt`
- `app/src/test/java/com/tuempresa/inventariovial/TrafficSignVisualSelectorTest.kt`
- `tools/extract_signal_assets.py`
- `tools/package_sic_new.py`
- `tools/signal_crop_overrides.json`
- `tools/validate_sic_new.py`

Además: schema 15.json; 207 WebP y manifest; documentos solicitados, logs y XML de evidencia. El inventario completo con hashes queda en `sic-new-evidence/packaged-source-files.json`.

