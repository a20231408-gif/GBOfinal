# Parallel B · Inventario vial / Puentes / SCAP / SIC

Entrega sobre el mismo repositorio base de GBO_InventarioVial, sin commit ni push. Paquete: `GBO_PARALLEL_B_INVENTARIO_PUENTES.zip`.

## Cambios para campo

- PUENTES comienza con TIPO DE PUENTE: MODULAR / LOSA. Cada tipo tiene modelo de captura, catálogo, mapping y estrategia de exportación propios.
- Guardar borrador conserva datos incompletos, incluso texto numérico todavía en edición. Finalizar ficha es una acción independiente que valida y muestra pendientes con navegación a sección.
- SCAP muestra encabezado general, completos/pendientes, catálogos de G de ambas fuentes, metrado manual, unidades y factores reales. Permite elementos manuales con código independiente, seis calificaciones de G y fotos vinculadas. No aplica conserva información y permite restaurarla.
- DATOS PARA SIC mantiene completitud SIC-17/17A/17B, reutilización de SCAP y captura manual de faltantes. PR temporal usa MANUAL_PENDING_SIC3 y se conserva al reabrir.
- SIC-22 busca por código/nombre/descripción, permite código manual con procedencia explícita y SIN CÓDIGO IDENTIFICABLE con código nulo. Informativa permite elegir hito/poste kilométrico con `3 / I-2A` y número en su columna existente; no pide ubicación final. La edición conserva finales históricos.
- Alcantarilla conserva categorías panorámica/entrada/salida, con múltiples fotos. Otras fichas sin categoría justificada usan GENERAL. La política queda disponible para CORE.
- Revisión previa SIC/SCAP muestra miniaturas con proporción y pendientes. El encabezado SIC corporativo utiliza texto y logos del archivo SIC17 adjunto; SIC-18, SIC-19 y los demás mantienen sus títulos y columnas.
- Exportación SCAP usa la plantilla original correcta, fotos centradas con FIT_CENTER y orientación EXIF. Conserva nombres/orden de hojas, estilos, merges, dimensiones, logos y fórmulas derivadas. Bloquea excedentes que perderían información; los borradores no se pierden.

## Fuentes y mapping

Antes de modificar la app se extrajeron hojas, campos/celdas, secciones, listas/validaciones, merges, dimensiones, imágenes, encabezados y fórmulas de los tres documentos. Los originales se copiaron sin rescribirlos.

| Tipo | Documento y evidencia | Catálogo / mapping |
| --- | --- | --- |
| LOSA | `docs/reference/FICHA SCAP - GBO modelo - LOSA.xlsx`; `docs/SCAP_LOSA_FIELD_MAP.json` | `scap_slab_catalog.json`, `tools/scap_slab_field_map.json`; 112 opciones AUXILIAR y 140 campos operativos |
| MODULAR | `docs/reference/FICHA SCAP - GBO modelo - MODULAR.xlsx`; `docs/SCAP_MODULAR_FIELD_MAP.json` | `scap_modular_catalog.json`, `tools/scap_modular_field_map.json`; 112 opciones AUXILIAR y 140 campos operativos |
| SIC general | `docs/reference/SIC17_MO-104.xlsx`; `docs/SIC_WORKBOOK_HEADER_MAP.json` | `SicWorkbookHeaderWriter`; texto contractual y dos logos originales |

Ambas plantillas SCAP tienen nueve hojas. Sus bloques de F.1/G.2 y elementos habituales son diferentes. `SCAP_EXPORT_CONTRACT.md` describe coordenadas y límites exactos; `PARALLEL_B_SCAP_CAPTURE.md` describe captura, fuentes y reglas. Se conserva `und` de G Modular y `und.` de AUXILIAR como valores distintos. Los factores 0.2/0.4/0.6/0.8/1 se guardan como valores, nunca como índices de selector.

Seis campos de MODULAR admiten los literales observados exactamente en la fuente, junto con el número válido de su tipo: Sin Informacion, SIN DATOS, N/A y 5 pulg. No se inventan catálogos ni conversiones de unidades. Esos valores tienen trazabilidad a la celda de origen.

## Archivos y revisión

`PARALLEL_B_FILES.json` contiene la lista exacta de archivos de app cambiados/agregados y sus hashes. `PARALLEL_B_CHANGES.diff` es un unified diff contra la copia previa; el directorio no tiene metadatos Git.

Además de los archivos de app, se agregaron las fuentes canónicas, mapas/documentos de esta rama, `tools/extract_parallel_b_templates.py`, `tools/extract_parallel_b_capture_catalog.ps1`, `tools/generate_scap_export_maps.py`, `tools/verify_parallel_b_exports.py` y `tools/package_parallel_b.py`.

No se modificaron router Drive, WorkManager, KMZ, PavementWorkspace ni fórmulas PCI/URCI. `PARALLEL_B_INTEGRATION.md` contiene SQL completo, versión Room, cambios mínimos MainActivity, contratos para CORE, archivos compartidos y conflictos esperados.

## Pruebas y Gradle

Baseline ejecutado **antes de modificar la app**: 249 tests, 15 fallos previos, `assembleDebug` exitoso, Room v12. Identidades de cada fallo: `PARALLEL_B_BASELINE.json`.

Pruebas nuevas:

| Suite | Cobertura |
| --- | --- |
| ParallelBScapCaptureTest | Tipos/modelos, catálogo fuente completo, guardar/reabrir borrador, finalizar pendientes, G manual/metrado/unidad/factor/fotos, SIC incompleto, PR pendiente SIC3, coordenadas manuales y literales de origen |
| ScapCanonicalExportTest | Template correcto por tipo, exportaciones LOSA/MODULAR, hojas/estilos/geometría/fórmulas/logos, filas manuales, límites, portrait/landscape/EXIF y números/literales |
| ParallelBInventoryPolicyTest | Buscador nombre/código/acentos, manual, sin código, hito puntual/número Excel, completitud y categorías |
| ParallelBSignalStorageTest | Persistencia/reapertura de source, descripción, observación y final histórico |
| ParallelBMigrationTest | SQL v12→13 conserva código previo y metadata con defaults; código nulo sin identificación |
| ParallelBSicHeaderTest | Encabezado común y logos; cada SIC complementario conserva título, columnas y datos |

Los tests existentes se mantienen; se ajustan únicamente fixtures afectados por nuevas reglas explícitas: v13/setup migración, puente LINEAR según fuente adjunta y código sintético I-1A del catálogo para exportación final. No se relajan las aserciones de proporción/fórmulas para forzar éxito.

Resultado final: **276 tests, 15 fallos previos, cero fallos nuevos; assembleDebug exitoso**. Las identidades de fallos y comparación exacta están en `PARALLEL_B_VALIDATION.json`. El ZIP contiene logs de `gradlew testDebugUnitTest` y `gradlew assembleDebug`, además de logs baseline. Se añadieron 27 tests; el empaquetador exige cero fallos nuevos y assembleDebug exitoso.

Se reabrieron los XLSX sintéticos mediante lectura OOXML y openpyxl; la comparación independiente está en `PARALLEL_B_EXPORT_VERIFICATION.json`. Se renderizaron encabezados SIC-17/SIC-18 y SCAP LOSA/MODULAR para inspección visual. Las pruebas verifican la geometría de fotos y sus anchors, proporción/centrado y EXIF; la prueba fotográfica también produce una ficha sintética para revisión visual.

## Límites de entrega

Las fuentes contienen fórmulas/nombres definidos `#REF!` previos: se conservan y se advierten; requieren corrección del responsable de formato. No se inventa la definición/unidad pendiente de capacidad máxima en SIC-17B. Los excedentes de repetibles requieren una continuación de plantilla aprobada; no se cambia arbitrariamente la estructura canónica.

La navegación de pendientes abre la sección identificada; no enfoca automáticamente cada control en una sección larga. Inspecciones históricas sin tipo conservan su formato histórico. Campos sin marca obligatoria no se convierten en requisitos inventados de cierre: quedan pendientes visibles para revisión, mientras identificación mínima, datos suministrados y evaluación de G se validan estrictamente.

La compilación y pruebas locales no equivalen a una prueba física de cámara/GNSS ni a recálculo de las fórmulas contractuales en Microsoft Excel. Los artefactos de prueba son datos sintéticos. La entrega incluye fuentes y código, sin credenciales locales ni APK con esas credenciales incrustadas.
