# Correcciones funcionales de campo — 07/10/2026

Las seis decisiones de ingeniería fueron aplicadas al repositorio actual. El estado inicial no contiene una carpeta Git operativa; se guardaron copias anteriores y un diff legible en `output/tablet-ux-2026-10-07`. No se hizo commit ni push.

## Archivos modificados o agregados

- `app/src/main/java/com/tuempresa/inventariovial/MainActivity.kt`
- `app/src/main/java/com/tuempresa/inventariovial/RecordHistoryScreen.kt`
- `app/src/main/java/com/tuempresa/inventariovial/camera/PhotoUtils.kt`
- `app/src/main/java/com/tuempresa/inventariovial/export/SicExcelWriter.kt`
- `app/src/main/java/com/tuempresa/inventariovial/export/SicExportFormat.kt`
- `app/src/main/java/com/tuempresa/inventariovial/field/CaptureSummary.kt`
- `app/src/main/java/com/tuempresa/inventariovial/field/SurveyPreferences.kt`
- `app/src/main/java/com/tuempresa/inventariovial/gis/KmlExporter.kt`
- `app/src/main/java/com/tuempresa/inventariovial/pavement/PavementDao.kt`
- `app/src/main/java/com/tuempresa/inventariovial/pavement/PavementRepository.kt`
- `app/src/main/java/com/tuempresa/inventariovial/pavement/PavementScale.kt`
- `app/src/main/java/com/tuempresa/inventariovial/validation/CaptureValidation.kt`
- `app/src/main/java/com/tuempresa/inventariovial/validation/EndLocationPolicy.kt`
- `app/src/main/java/com/tuempresa/inventariovial/viewmodel/InventoryViewModel.kt`
- `app/src/main/java/com/tuempresa/inventariovial/pavement/ui/PavementDialogs.kt`
- `app/src/main/java/com/tuempresa/inventariovial/pavement/ui/PavementScheme.kt`
- `app/src/main/java/com/tuempresa/inventariovial/pavement/ui/PavementWorkspace.kt`
- `app/src/main/java/com/tuempresa/inventariovial/pavement/PavementHistory.kt`
- `app/src/main/java/com/tuempresa/inventariovial/pavement/ui/PavementDamageTable.kt`
- `app/src/main/java/com/tuempresa/inventariovial/field/InventoryPhotoGallery.kt`
- `app/src/test/java/com/tuempresa/inventariovial/TabletUxDataTest.kt`
- `app/src/test/java/com/tuempresa/inventariovial/TabletUxUiTest.kt`
- `app/src/test/java/com/tuempresa/inventariovial/PavementModuleTest.kt`
- `app/src/test/java/com/tuempresa/inventariovial/SicExcelWriterTest.kt`
- `app/src/test/java/com/tuempresa/inventariovial/EngineeringDecisionsTest.kt`

## Fecha e historial de pavimentos

`PavementRepository.newSample` asigna `LocalDate.now()` una sola vez al crear el UUID. Room persiste `PavementSampleEntity.fecha`. `saveSample` conserva la fecha y `createdAt` originales aunque el objeto enviado intente cambiarlos. La cabecera permite consultar la fecha de creación, sin input manual.

`PavementDao.observeSamples` carga todo el historial mediante Flow y ordena por fecha/createdAt/consecutivo. `PavementHistory.visible` aplica exclusivamente superficie, tramo/ruta e inspector; ya no existen Desde/Hasta ni un filtro implícito de hoy. Las tarjetas muestran fecha dd/MM/yyyy y texto Borrador / En progreso o Cerrada. Continuar abre el mismo UUID del creador mientras está abierto; Ver mantiene la política actual para cerradas o de otros inspectores. Daños, fotos, dimensiones y cálculo se consultan por ese UUID, sin crear otra muestra al cambiar el día. No se inventaron estados de sincronización que el modelo no persiste.

## Fotografías del inventario

Los dos formularios de captura reutilizan `InventoryPhotoGallery`. Cada archivo tiene miniatura, Ver y Quitar. La miniatura y Ver abren un Dialog con la imagen completa y Cerrar; solo se cambia el estado local de preview, conservando los campos del formulario.

`LocalPhotoImage` decodifica en Dispatchers.IO usando el cargador existente `loadCorrectlyOrientedBitmap`. Miniaturas: lado máximo 480 px; preview: 1600 px. Se aplica EXIF, incluyendo rotaciones y reflejos, y se libera el bitmap al retirar el componente. Quitar retira solamente la ruta seleccionada de la lista del registro; no borra archivos externos ni otras imágenes. La prueba utiliza dos JPEG reales y comprueba que ambos originales siguen existiendo. La persistencia de fotos sigue usando PhotoEntity; Room conserva sus asociaciones al reabrir.

## SIC-22 puntual y compatibilidad

En `SignalizationFormScreen` se ocultan por completo Ubicación fin, sugerencia GNSS final, PR/distancia final y captura final; las etiquetas existentes pasan a Ubicación/GPS del elemento. El historial y su edición también ocultan los campos finales. `requiresEndLocation` devuelve false para SIC-22, incluso si un registro antiguo contiene un assetType de otro elemento.

`pointCapture` normaliza únicamente la solicitud transitoria SIC-22 y elimina sus valores finales antes de validar/crear. Se aplica en ViewModel, validación y preferencias; las solicitudes de otros SIC conservan sus reglas. La edición histórica mantiene los campos finales almacenados pero no los exige ni los muestra. No se borraron columnas ni datos históricos. El orden geométrico ya usa la ubicación inicial; la geometría KML es puntual y ahora tampoco entrega metadatos finales SIC-22. SIC-21 conserva sus reglas longitudinales.

## XLSX SIC-22

El workbook se construye dinámicamente; no se modificó una plantilla contractual. Antes había 14 columnas, con dos parejas Código PR/Distancia (inicio y fin). Ahora hay exactamente 12 columnas físicas:

Carretera | Calzada | Código PR | Distancia (m) | Lado | Clase | Tipo | Material | Código de señal | Número del poste kilométrico | Condición | Fecha.

Se eliminan los valores finales, la celda del bloque final y el merge E12:F12. El único encabezado de ubicación es C12:D12, Ubicación del elemento. Los merges del título, el autofiltro, anchos y área de impresión siguen el número real de columnas. Los demás formatos mantienen su estructura; se prueban los siete paquetes XLSX.

## Regla, coordenadas y gestos

`PavementScale.canvasY(metres) = (largo - metres) * pixelsPerMetre`; `metresY(canvasY)` es su inversa. `PavementViewport` agrega el mismo origen y escala al dibujo, toque y movimiento. Grid, daños, límites, regla y etiquetas se dibujan dentro de una sola traducción Canvas, incluyendo pan horizontal/vertical. El eje de la regla coincide con x=0 del borde izquierdo; los números quedan alineados a la derecha, a 14 px del borde. Se mantiene 0 abajo y aumento de Y hacia arriba.

Las etiquetas usan densidad visual: 1 m si una separación de 1 m permite el espacio del texto; 2 m si lo permite 2 m; en caso contrario 5 m o múltiplos de 5 para muestras muy largas. Se dibujan solamente ticks dentro del intervalo visible, evitando etiquetas superpuestas cerca de extremos. Pinch conserva el punto del gesto y comparte la traducción con la regla; Ajustar restablece zoom/pan. Zoom/pan no escribe coordenadas ni recalcula medidas. Mover guarda únicamente el daño explícitamente seleccionado y conserva las medidas existentes según la política anterior.

## Tabla de daños

`PavementDamageTable`: Ítem | X | Y | Falla | Severidad. Una fila por daño; Ítem es el índice humano de la lista, el UUID permanece como clave real. X/Y vienen directamente de x1/y1, las mismas coordenadas del esquema. La presentación usa dos decimales, sin redondear datos persistidos. Nombres largos admiten dos líneas y ellipsis; el detalle permite leer el nombre completo.

El encabezado está fuera de LazyColumn y permanece visible durante su scroll. La tabla tiene alto acotado para que no crezca indefinidamente dentro del panel. La selección resalta fila y geometría; el detalle conserva Cantidad (dos decimales), observación y las acciones Modificar, Mover en esquema, Tomar fotografía, Seleccionar imagen, Eliminar y galería. Modificar/Mover reciben el objeto seleccionado por UUID, comprobado con Compose. La cantidad se mantiene en el detalle para conservar legibilidad en el panel lateral de tablet. Seleccionar desde la grilla hacia la tabla no se agregó; tocar la grilla conserva la captura existente.

## Room y cálculos

Room anterior/final: **12 / 12**. No hubo migración ni cambio de entidades/schema ni destructive migration. Se verificaron 55 hashes de archivos protegidos: cero cambios, incluidos SCAP, autenticación, InventoryDatabase y los modelos/reglas/calculador PCI/URCI. No se modificaron curvas, fórmulas, densidades ni valores deduct. Los tests comparan el resultado antes/después de reabrir y formatear; los tests previos de cálculo, movimiento y reportes siguen pasando.

## Pruebas

Pruebas nuevas:

- `com.tuempresa.inventariovial.TabletUxDataTest.singleLocationSic22AndLegacyEndFieldsPhotosSurviveRoomReopen`
- `com.tuempresa.inventariovial.TabletUxDataTest.formattingPreservesPrecisionQuantityAndCalculator`
- `com.tuempresa.inventariovial.TabletUxDataTest.systemDatePersistsAndHistoricalResumeKeepsAssociationsAndCalculation`
- `com.tuempresa.inventariovial.TabletUxDataTest.verticalIgnoresLegacyEndValidationWithoutChangingHorizontalPolicy`
- `com.tuempresa.inventariovial.TabletUxDataTest.rulerUsesOneTransformAtEveryZoomPanAndAdaptsToDensity`
- `com.tuempresa.inventariovial.TabletUxDataTest.sic22WorkbookHasExactlyTwelveColumnsOneLocationAndNoLegacyEnd`
- `com.tuempresa.inventariovial.TabletUxDataTest.historyOrdersCreationAndRespectsFiltersAndClosedRights`
- `com.tuempresa.inventariovial.TabletUxDataTest.thumbnailHonorsExifBoundsWithoutRewritingOriginal`
- `com.tuempresa.inventariovial.TabletUxUiTest.historicCardContinuesExactIdAndClosedCardUsesView`
- `com.tuempresa.inventariovial.TabletUxUiTest.tableHasOneRowPerDamageAndSelectionKeepsRealId`
- `com.tuempresa.inventariovial.TabletUxUiTest.verticalFormUsesElementLocationAndDoesNotRenderEndBlock`
- `com.tuempresa.inventariovial.TabletUxUiTest.twoPhotosPreviewCloseRemovalPreserveOtherAndForm`

Actualizadas: `PavementModuleTest.rulerAndDamageUseSameBottomLeftTransform` acepta el paso adaptativo de 2 m; `SicExcelWriterTest` y `EngineeringDecisionsTest` ajustan únicamente el contrato SIC-22 a 12 columnas y sus nuevos índices.

Comando enfocado: `gradlew.bat testDebugUnitTest --offline --console=plain --tests '*TabletUx*' --tests '*Pavement*' --tests '*SicExcelWriterTest*' --tests '*EngineeringDecisionsTest*'`. Resultado: **49 pruebas, 0 fallos, 0 errores, BUILD SUCCESSFUL**. Logs y XML preservados en `focused.log` y `focused-results/`.

Suite completa: `gradlew.bat testDebugUnitTest --offline --console=plain` terminó con **BUILD FAILED**, 249 pruebas, 15 fallos, 0 errores y 0 omitidas. Pasaron 234 pruebas. Los nombres de los 15 fallos son exactamente iguales a `output/sic-bridge-test-summary.txt` (línea base de 237 pruebas / 15 fallos): **0 fallos nuevos**. No se presenta la suite completa como verde.

Comando obligatorio `gradlew.bat assembleDebug --offline --console=plain`: **exit 0, BUILD SUCCESSFUL in 26s**, 40 tareas (4 ejecutadas y 36 actualizadas). APK generado y copiado a `output/GBO_InventarioVial_TABLET_UX_2026-10-07-debug.apk` (23.526.699 bytes).

SHA-256: `d79ac6003d698b11a8b523cf63f73466338d4a83adc35933b6a8f0c918ed9027`.

Evidencia: `output/tablet-ux-2026-10-07/full-test.log`, `assemble.log`, `test-summary.txt`, `full-summary.json`, `focused-results/`, `changes.diff`, `protected-hashes.json`. Se incluye `SIC22_EJEMPLO_SINTETICO.xlsx`, producido por la prueba del exportador, para inspeccionar los 12 encabezados; sus datos son sintéticos.

Fallos previos pendientes (no introducidos por esta corrección):

- `com.tuempresa.inventariovial.Engineering2DataTest.sevenPiersFixedStructuresAndObservationsSurviveReopenWithoutMaterializingReminders`
- `com.tuempresa.inventariovial.Engineering2ScreenTest.f2ShowsPendingDefaultsAndEditingAnObservationPersistsWithoutAssertingPresence`
- `com.tuempresa.inventariovial.EngineeringUxTest.versionSixMigrationKeepsAllHistoricalDataAndPromotesLegacyJoint`
- `com.tuempresa.inventariovial.InventoryRoomMigrationTest.migrationsFromEveryReleasedVersionPreserveRecordsPhotosAndSic23`
- `com.tuempresa.inventariovial.PrSuggestionStorageTest.migrationNineToTenRetainsRecordsPhotosAndStartSourceAndDefaultsEndToManual`
- `com.tuempresa.inventariovial.RoadAxisStorageTest.realVersionEightMigrationPreservesManualPrPhotosAndDetailsAndNewFieldsStartNull`
- `com.tuempresa.inventariovial.RoadReferenceDeliveryTest.categoryStylesShortNamesAndExtendedDataPreserveManualAndGeometricValues`
- `com.tuempresa.inventariovial.RoadReferenceDeliveryTest.compatibleAxisHasYellowTopBlackBorderIdenticalGeometryAndSidebarLegend`
- `com.tuempresa.inventariovial.RoundTwoStorageTest.migration10to11PreservesEveryColumnOfEveryLegacyTableIncludingSecurityAndScap`
- `com.tuempresa.inventariovial.ScapExportTest.runtimeIncludesExactTemplateAndExportPreservesSheetsStylesGeometryAndCalculationFormulas`
- `com.tuempresa.inventariovial.ScapFullExportTest.SCAP_COMPLETO_TEST_reopensWithAllFieldsSevenDistinctPiersAndOriginalGeometry`
- `com.tuempresa.inventariovial.ScapStorageTest.versionFiveMigrationPreservesSicRecordsPhotosMetadataAndNewCulvertFields`
- `com.tuempresa.inventariovial.SurveyStorageTest.versionFourUpgradeRetainsOldCulvertDimensionsAndMarksNewFieldsUnknown`
- `com.tuempresa.inventariovial.UserAuthenticationTest.fieldSessionOperatorComesFromAuthenticatedUser`
- `com.tuempresa.inventariovial.UserAuthenticationTest.migration7to8PreservesEveryExistingTable`

Se agrupan en infraestructura de migraciones antiguas de pruebas, expectativas previas de SCAP/KML e identificación Android en Robolectric. Los mensajes completos quedan en test-summary.txt. Se preserva el alcance pedido: no se altera SCAP ni autenticación para corregirlos.


## Limitaciones y revisión física

La validación física con GNSS, cámara, gestos y dimensiones reales de la tablet está pendiente de tu revisión. Robolectric verifica persistencia, UI, EXIF y transformaciones; no sustituye esa comprobación. Los fallos anteriores de la suite completa se documentan separadamente, sin ampliar esta tarea a SCAP/autenticación o sus migraciones. No se corrigió contenido técnico ni se interpretaron severidades.

## Checklist exacta solicitada para la tablet

PAVIMENTOS — HISTORIAL

1. Abrir Fallas en pavimentos.
2. Confirmar que NO aparecen:
   Desde
   Hasta.
3. Crear nueva muestra.
4. Confirmar fecha automática.
5. Cerrar app.
6. Cambiar/reabrir otro día o simular registro histórico.
7. Confirmar que aparece en lista.
8. Pulsar Continuar.
9. Confirmar que abre la misma muestra.

FOTOS INVENTARIO

10. Crear registro SIC.
11. Tomar 2 fotografías.
12. Confirmar dos miniaturas.
13. Abrir fotografía 1.
14. Cerrar preview.
15. Quitar fotografía 2.
16. Confirmar que fotografía 1 permanece.

SIC-22

17. Abrir Señalización vertical.
18. Confirmar que existe SOLO ubicación del elemento.
19. Confirmar que NO aparece Ubicación fin.
20. Guardar una señal.
21. Exportar SIC-22.
22. Abrir Excel.
23. Confirmar que no existe bloque/columnas de ubicación final.

PAVIMENTOS — REGLA

24. Crear muestra de 50 m.
25. Abrir esquema.
26. Confirmar regla pegada al borde izquierdo.
27. Vista completa:
    referencias cada ~5 m.
28. Hacer zoom.
29. Confirmar referencias cada 1 m.
30. Desplazarse con pan.
31. Confirmar que regla y grilla siguen alineadas.
32. Revisar un daño Y=7 m.
33. Confirmar alineación exacta con 7 m.

PAVIMENTOS — TABLA

34. Crear al menos 5 daños.
35. Confirmar tabla:
    Ítem | X | Y | Falla | Severidad.
36. Comparar X/Y contra esquema.
37. Seleccionar una fila.
38. Modificar.
39. Mover en esquema.
40. Confirmar que sigue siendo el mismo daño.
41. Confirmar que PCI/URCI no cambió por la nueva UI.
