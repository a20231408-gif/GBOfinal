# Integración de Parallel B · Inventario vial / Puentes / SCAP / SIC

Se trabajó sobre el checkout base de `GBO_FIX`. No contiene `.git`; no se creó commit ni se hizo push. `PARALLEL_B_CHANGES.diff` compara el código con la copia previa, y `PARALLEL_B_FILES.json` enumera archivos de la app modificados/agregados con sus hashes.

## Room

Base registrada: v12. Esta entrega independiente usa **v13**, con `InventoryDatabase.MIGRATION_12_13`, registrada en el builder. La integración de ramas debe asignar la versión final y encadenar todos los cambios de CORE/Pavimentos, sin reutilizar esta versión para SQL incompatible ni emplear migración destructiva.

SQL completo de esta rama:

```sql
ALTER TABLE sic22_details ADD COLUMN signalCodeSource TEXT NOT NULL DEFAULT 'CATALOG';
ALTER TABLE sic22_details ADD COLUMN signalDescription TEXT NOT NULL DEFAULT '';
ALTER TABLE sic22_details ADD COLUMN signalObservation TEXT NOT NULL DEFAULT '';
```

No se reescriben códigos ni ubicaciones históricos. `CATALOG` mantiene compatibilidad para registros anteriores; un código histórico ajeno al catálogo se muestra pendiente de revisión al exportar. El esquema exportado está en `app/schemas/com.tuempresa.inventariovial.data.database.InventoryDatabase/13.json`.

SCAP no añade tablas/columnas: guarda el tipo como `scap_values(ownerId='inspection', key='bridgeType', value='MODULAR'|'SLAB', source='USER_SELECTED')`. Las inspecciones históricas sin tipo siguen usando su formato histórico; no se infiere una clasificación para ellas. Los metadatos de elemento manual usan `elementSource=MANUAL`; sus valores y fotos permanecen al marcarlo ausente.

La ubicación SIC parcial reside en claves `sicLocation.*`. PR y distancia temporal usan `source=MANUAL_PENDING_SIC3`. Cuando se confirma una ubicación completa, se reutiliza el registro vial vinculado; CORE/SIC-3 puede sustituir su procedencia conservando valores. No se implementa SIC-3.

## Contratos que CORE debe consumir

| Modelo / componente | Uso |
| --- | --- |
| `ScapBridgeType`, `ScapCaptureModels` | Tipo explícito y modelo de captura por formato |
| `ScapCatalog.forSnapshot` | Campos, opciones y catálogos obtenidos de la fuente correspondiente |
| `ScapCompletion`, `ScapPendingField` | Sección/campo/propietario/clave para navegar a pendientes |
| `ScapRepository.saveDraft` | Persistencia parcial independiente de finalización |
| `BridgeSicContract.assess` | Completitud SIC-17/17A/17B y valores reutilizados |
| `SignalCodeSource`, `SignalIdentificationPolicy` | CATALOG, MANUAL, UNIDENTIFIABLE; código nulo cuando no es identificable |
| `EndLocationPolicy.locationMode` | Matriz POINT/LINEAR documentada en JSON |
| `PhotoCategoryPolicy` | Categorías; alcantarilla panorámica/entrada/salida; varias fotos por categoría |
| `InventoryPhotoReview(paths)` | Miniaturas horizontales y visor de foto con decoder actual |
| `SicWorkbookHeaderWriter` | Encabezado y logos comunes, manteniendo columnas/títulos propios de cada SIC |

`PHOTO_CATEGORY_POLICY_INVENTORY.json` usa claves de fotografía ya existentes (`CULVERT_PANORAMIC`, `CULVERT_INLET`, `CULVERT_OUTLET`) y sus nombres PANORAMICA/ENTRADA/SALIDA. Los demás elementos usan GENERAL cuando no existe categoría justificada. SCAP conserva su catálogo fotográfico existente. No hay nuevo router de Drive.

La metadata de señal está en Room. No se añaden columnas inventadas al SIC-22: el código manual ocupa su columna existente; UNIDENTIFIABLE exporta vacío; el número de hito ocupa la columna existente. CORE puede integrar descripción/observación/procedencia en sus metadatos de entrega.

## Archivos compartidos y conflictos

Cambios de `MainActivity.kt` limitados al wrapper del formulario SIC-22 y aceptación de ausencia explícita de código identificable. `InventoryViewModel`/`EngineeringEdits` persisten metadata de señal y conservan ubicación final histórica al editar elementos puntuales.

Compartidos modificados: `app/build.gradle.kts` (copias de plantillas y mapas), `InventoryDatabase.kt`, `Sic22Entity.kt`, `InventoryViewModel.kt`, `MainActivity.kt`, `TrafficSignCatalog.kt`, `EndLocationPolicy.kt`, `CaptureValidation.kt`, `FieldUi.kt`, `CaptureSummary.kt`, `InventoryPhotoGallery.kt`, `EngineeringEdits.kt`, `SicExportScreen.kt`, `SicExcelWriter.kt`, `SicExportFormat.kt`, `LocalSicExport.kt` y archivos SCAP. La lista exacta está en `PARALLEL_B_FILES.json`.

La prueba `PavementModuleTest` solo agrega la nueva migración en su setup Room; no cambia implementación, fórmulas ni expectativas de Pavimentos. Los demás ajustes de tests existentes actualizan v13, el modo LINEAR de puente según SIC17 adjunto y el código sintético `I-1` a `I-1A` existente en catálogo para el ensayo de exportación final.

Conflictos esperados: versión/esquema/migraciones Room, constructor de entidad SIC22, serialización de metadata, UI de fotos compartida y wrapper SIC22 en MainActivity. No se modificaron Drive router, WorkManager, KMZ, PavementWorkspace ni fórmulas PCI/URCI. `ScapDelivery` solo selecciona exportador por tipo y sube el número de caché local del paquete a 5 para evitar reutilizar una exportación de plantilla histórica; conserva protocolo y rutas.

## Plantillas y límites

Integrar también los XLSX canónicos byte a byte de `docs/reference`, los catálogos en `app/src/main/assets`, mapas operativos en `tools`, y logos originales en `app/src/main/resources/sic_header`. Gradle los incorpora sin depender de cambios no publicados de otras ramas.

`SCAP_EXPORT_CONTRACT.md` describe coordenadas, repetibles y límites. La exportación bloquea excedentes que perderían información; el borrador conserva todos los datos. Ambas fuentes contienen referencias `#REF!` preexistentes que se mantienen y se advierten. La verificación estructural y visual no establece que esas fórmulas de origen produzcan resultados válidos en Excel.

El ZIP excluye cachés, builds, configuración local con credenciales y logs ajenos a esta verificación. Incluye el repositorio base con cambios y las evidencias Gradle. No incluye un APK con credenciales de configuración local incrustadas.
