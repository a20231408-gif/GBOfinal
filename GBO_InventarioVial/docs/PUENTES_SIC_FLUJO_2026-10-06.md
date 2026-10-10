# Puentes: captura SCAP → SIC-17 / SIC-17A / SIC-17B

## Resultado y auditoría inicial

La sección **Datos para SIC** permite revisar datos heredados, completar información exclusiva SIC y consultar el estado por ficha. Identificación y revisión conservan el acceso al mismo panel. Los cambios se guardan localmente antes de exportar.

Antes: el mapper reconstruía el PR a partir de la progresiva, y cualquier edición SCAP podía reemplazar la ubicación vial. Las tarjetas de exportación enumeraban campos sin controles de captura. SIC-17A/B ya tenían entidades Room y catálogos, pero no estaban enlazados al flujo SCAP. Los faltantes técnicos se mezclaban con datos que el operador podía completar.

Después: la ubicación contractual vinculada por `roadRecordId` es la fuente única de PR/distancia y calzada. Las fichas A/B utilizan las entidades existentes. `BridgeSicContract` comparte campos, validación y completitud entre UI y exportador. La lista de faltantes se calcula desde los valores persistidos y disminuye al completar cada dato válido. El exportador bloquea los campos obligatorios incompletos e identifica los opcionales por separado.

## Fuente y límites contractuales

Fuente primaria: [MTC, Manual de Inventarios Viales IV-2014/2015](https://portal.mtc.gob.pe/transportes/caminos/normas_carreteras/MTC%20NORMAS/ARCH_PDF/MAN_8%20IV-2014_2015.pdf), fichas pp. 207–212 y tipos pp. 242–243. Se conservan las definiciones previas de `SicCatalogRepository` y `SupplementaryForms`, contrastadas con la auditoría existente `ARCHITECTURE_REVIEW.md`. El visor remoto del PDF no respondió y su descarga directa fue rechazada por el servidor; se verificó el contenido indexado de la fuente MTC. No se recibió un nuevo archivo contractual SIC ni imágenes de las capturas: los casos A/B/C se reprodujeron desde el texto de la solicitud.

No se establece una equivalencia nueva entre catálogos SCAP y SIC. Los campos sin correspondencia inequívoca se capturan expresamente con el catálogo SIC. Se mantienen los códigos originales, incluido **3** tanto para Concreto pobre como para Acero: Room conserva el código y una descripción adicional; el Excel lleva el código original. Un histórico con solo `3`, sin descripción, requiere selección explícita. Los históricos con descripción completa se conservan y proyectan al código.

`maximumCapacity` queda como `UNRESOLVED_CONTRACT_MAPPING`: la fuente no resuelve la unidad ni la discrepancia con sobrecarga de diseño. No aparece como faltante del operador ni como entrada normal. El Excel conserva su columna vacía; los valores históricos permanecen en Room sin reinterpretación. `designLoad` conserva su denominación textual. El diagnóstico existe también en la pantalla SIC-17B independiente.

## Campos capturables

**SIC-17A:** nombre, código, año, departamento político, provincia, distrito, poblado, latitud/longitud WGS84, altitud, número de carriles entero > 0, ancho de calzada/vereda/tablero/superestructura en metros y alineamiento. El teclado de coordenadas permite el signo negativo y se validan los rangos geográficos. Los cuatro campos antes inaccesibles son `lanes`, `deckWidthM`, `superstructureWidthM`, `alignmentCode`. Los datos homólogos SCAP aparecen con procedencia; Revisar valor SIC crea un override independiente y Restaurar SCAP vuelve a la herencia.

**SIC-17B:** código, sobrecarga de diseño, superficie de desgaste, contención vehicular, luz principal, condición de borde, sección transversal, número de vigas entero ≥ 0, material de losa/vigas, elevación/material/cimentación de estribos y pilares, comentarios multilínea. No se toma automáticamente el primer vano, estribo o pilar. El inspector confirma la clasificación global y puede usar 0 — No aplica donde el catálogo lo permite.

**SIC-17:** ubicación vinculada, clase/tipo dependiente, inventariado, condiciones estructural/funcional, servicio, singularidad, identidad, dimensiones, fecha y número de vanos. Si existe ubicación se muestra sin repetir inputs; Modificar ubicación permite una edición explícita y atómica. Si falta, se usan los mismos catálogos de rutas y calzadas. La ubicación final se captura expresamente y no se inventa. El PR admite 1–4 dígitos y se guarda con cuatro; la distancia admite decimales no negativos.

## Tabla exacta de proyección

| Origen | Destino | Operación / límites |
|---|---|---|
| InventoryRecordEntity.routeCode / roadbedCode | 17.routeCode / roadbedCode | Ubicación vinculada; calzada histórica sic17.roadbedCode solo cuando la vinculada está vacía. |
| InventoryRecordEntity.startPrCode / startDistanceM | 17.startPrCode / startDistanceM | Herencia directa de la pareja válida. No reconstrucción desde progresiva al exportar. |
| InventoryRecordEntity.endPrCode / endDistanceM | 17.endPrCode / endDistanceM | Herencia directa; no final calculado. |
| bridgeCode | 17/17A/17B.bridgeCode | Identificador documental; UUID como vínculo persistente. |
| lastInspection | 17.surveyDate | Fecha dd/MM/aaaa válida. |
| totalLengthM | 17.dimension1LengthM | Metros. |
| lowerClearanceM / upperClearanceM | 17.dimension2LowerHeightM / dimension3UpperHeightM | Alturas libres en metros. |
| singularityName | 17.singularityName | Nombre; no deduce código. |
| spans.count | 17.spans | Número de filas; se muestra heredado y admite revisión SIC. |
| bridgeName / constructionYear | 17A.bridgeName / constructionYear | Textos homólogos / año válido. |
| politicalDepartment / province / district / nearbyTown | 17A.department / province / district / nearbyTown | Departamento político, no departamento vial. |
| GNSS.latitude / GNSS.longitude | 17A.latitude / longitude | WGS84 capturado en inspección; no (0,0) inicial del registro. |
| altitudeM | 17A.altitude | Altitud en msnm. |
| roadwayWidthM / sidewalkWidthM | 17A.roadwayWidthM / sidewalkWidthM | Anchos homólogos; no sumas. |
| designLoad / mainSpanM | 17B.designLoad / mainSpanM | Texto de sobrecarga / luz principal en metros. |
| sic17.* | Campos SIC-17 correspondientes | Captura SIC, códigos de catálogo. Ubicación en InventoryRecord. |
| Sic17AEntity.* / Sic17BEntity.* | Columna SIC A/B correspondiente | Captura SIC, prevalece sobre herencia; marcadores permiten distinguir borrado explícito de ausencia. |

La progresiva SCAP solo inicializa un registro sin PR al ingresar explícitamente ese dato, con procedencia `SCAP_PROGRESSIVE`. Nunca reemplaza un PR existente o una ubicación `SIC_CONFIRMED`. Cambiar la ubicación SIC no altera los valores SCAP de ruta/progresiva.

Las coordenadas A se exportan como números con visualización de nueve decimales. El cambio del escritor compartido se activa solo para las columnas WGS84 de la proyección SIC-17A; los formatos SIC18–23 conservan su salida.

## Correspondencias que no se aplican

Vías de tránsito ≠ equivalencia aprobada de carriles; alineamiento SCAP no tiene tabla de equivalencias; anchos parciales no se suman para tablero/superestructura; UTM Este/Norte no se copia como latitud/longitud ni se convierte sin transformación validada. Categoría/tipo SCAP, índice/porcentajes de deterioro, materiales y códigos de filas estructurales no se convierten por índice del selector o por primera fila. Cargas actual/futura/sobreesfuerzo no se usan como capacidad máxima. Croquis, defectos y perfil conservan el detalle SCAP sin nuevos destinos SIC.

## Catálogos exactos usados

Todos los siguientes son los catálogos SIC existentes. Fuente contractual: MTC, páginas indicadas arriba; fuente de implementación indicada en cada grupo. La ruta usa `ProjectRouteCatalog` (catálogo de proyecto), no un catálogo nuevo.

### SIC-17 — SicCatalogRepository, tipos p. 242

Calzada: UC, UD, CD, A1, A2.

| Catálogo | Código y descripción preservados |
|---|---|
| sic17.class.0 | 01 - Puente Definitivo; 02 - Puente Provisional; 03 - Estructura Artesanal; 04 - Puente Histórico |
| sic17.type.0 | 1 - Losa; 2 - Losa con Vigas; 3 - Celular estilo Alcantarilla; 4 - Pórtico; 5 - Reticulado; 6 - Arco; 7 - Atirantado; 8 - Colgante; 9 - Otro |
| sic17.type.1 | 1 - Modular; 2 - Yawata; 3 - Otro |
| sic17.type.2 | 1 - Vigas de Troncos de Madera; 2 - Vigas de Rieles de Ferrocarril; 3 - Otro |
| sic17.type.3 | 1 - Mampostería de Piedra; 2 - Otro |
| sic17.type.4 | 1 - Otro |
| sic17.inventoried.0 | S - Sí; N - No |
| sic17.structural.0 | 1 - Buena; 2 - Regular; 3 - Mala |
| sic17.functional.0 | 1 - Buena · limpia; 2 - Regular · parcialmente obstruida; 3 - Mala · totalmente obstruida |
| sic17.service.0 | 0 - Fuera de servicio; 1 - Vehicular; 2 - Ferroviario; 3 - Peatonal; 4 - Otro |
| sic17.singularity.0 | 1 - Río; 2 - Quebrada; 3 - Canal; 4 - Camino; 5 - Vía Férrea; 6 - Otro |

### SIC-17A/B — SupplementaryForms, fichas pp. 207–212 y tipos pp. 242–243

| Campo | Código y descripción preservados |
|---|---|
| alignmentCode (Alineamiento) | 1 - Recto; 2 - Curvo; 3 - Esviado; 4 - Otro |
| wearingSurface (Superficie de desgaste) | 1 - Asfalto; 2 - Concreto de la losa; 3 - Concreto pobre; 3 - Acero; 4 - Madera; 5 - Afirmado; 6 - Otro |
| vehicleRestraintCode (Contención vehicular) | 0 - No presenta; 1 - Barrera; 2 - Guardavía; 3 - Baranda de concreto; 4 - Baranda de acero; 5 - Baranda combinada; 6 - Otro |
| boundaryCode (Condición de borde) | 1 - Simplemente apoyado; 2 - Continuo; 3 - Aporticado; 4 - Colgado; 5 - Sobre el terreno; 6 - Otro |
| crossSectionCode (Sección transversal) | 1 - Losa construida in situ; 2 - Losa prefabricada; 3 - Losa y vigas in situ; 4 - Losa y vigas prefabricadas de concreto; 5 - Losa y vigas de acero; 6 - Cajón de concreto; 7 - Cajón de acero; 8 - Otro |
| slabMaterialCode (Material losa) | 0 - No aplica; 1 - Concreto armado; 2 - Concreto preesforzado; 3 - Acero estructural; 4 - Acero corrugado; 5 - Madera; 6 - Piedra; 7 - Otro |
| beamMaterialCode (Material vigas) | 0 - No aplica; 1 - Concreto armado; 2 - Concreto preesforzado; 3 - Acero estructural; 4 - Acero corrugado; 5 - Madera; 6 - Piedra; 7 - Otro |
| abutmentElevationCode (Estribos: elevación) | 0 - No aplica; 1 - Gravedad; 2 - Muro en voladizo; 3 - Muro con contrafuertes; 4 - Pórtico; 5 - Cepa; 6 - Otro |
| abutmentMaterialCode (Estribos: material) | 0 - No aplica; 1 - Concreto armado; 2 - Acero; 3 - Madera; 4 - Piedra; 5 - Otro |
| abutmentFoundationCode (Estribos: cimentación) | 0 - No aplica; 1 - Zapata; 2 - Cajón; 3 - Pilotes; 4 - Otro |
| pierElevationCode (Pilares: elevación) | 0 - No aplica; 1 - Columna; 2 - Muro; 3 - Pórtico; 4 - Cepa; 5 - Celosía; 6 - Otro |
| pierMaterialCode (Pilares: material) | 0 - No aplica; 1 - Concreto armado; 2 - Acero; 3 - Madera; 4 - Piedra; 5 - Otro |
| pierFoundationCode (Pilares: cimentación) | 0 - No aplica; 1 - Zapata; 2 - Cajón; 3 - Pilotes; 4 - Otro |

Se mantiene el 0 — No aplica de las fichas estructurales aunque la tabla descriptiva de tipos indica rangos sin cero; esta discrepancia ya estaba documentada. Contención 0 significa No presenta. No se renumera superficie ni se crea una unidad de capacidad.

## Estado y exportación

`SicCompleteness` es un cálculo puro con listas complete/missing/notApplicable/unresolved/requiredMissing. Completo significa obligatorios válidos; opcionales vacíos se señalan aparte. SIC-17B usa Captura completa si aún existe un pendiente contractual. Nunca se presenta un pendiente contractual como responsabilidad del operador.

Obligatorios 17: todas las columnas excepto nombre de singularidad. Obligatorios A: identidad, WGS84, altitud, carriles, cuatro anchos y alineamiento; año/localidad son opcionales. Obligatorios B: identidad, superficie, contención, luz, borde, sección, vigas y todos los códigos estructurales; sobrecarga/comentarios opcionales y capacidad bloqueada. Se puede guardar un borrador inválido, pero no se cuenta como completo ni exporta un valor numérico/código inválido.

## Persistencia, migración y Drive

Room permanece en **versión 12**, sin nuevas tablas, columnas ni migración. Se reutilizan `sic17a_details`, `sic17b_details`, `inventory_records` y `scap_values`, unidos por UUID. `ScapInspectionSnapshot` añade relaciones de lectura. Las anotaciones de procedencia/override son claves en la tabla KV existente. Las escrituras parciales preservan los demás campos, incluida capacidad histórica. No hay fallback destructivo.

Una prueba abre un esquema real publicado v11 con ubicación, fotos y datos A/B históricos y ejecuta la migración existente 11→12, verificando conservación antes y después de editar. Otra prueba verifica que la preparación local de entrega no altera el snapshot, cierra/reabre una base persistente y compara cada dato con su celda Excel. Durante la captura comprueba individualmente que desaparezca el warning correspondiente.

La preparación Drive sigue usando su contrato existente (SCAP, fotos, croquis, manifest); no borra entidades SIC ni genera archivos/payloads nuevos. Los Excel SIC se generan mediante la acción existente de cada ficha. No se realizó una subida real a Drive ni una prueba en tablet física.

## Archivos y alcance

Modificados:

- `app/src/main/java/com/tuempresa/inventariovial/export/SicExcelWriter.kt` (precisión visual WGS84 solo para SIC-17A).
- `app/src/test/java/com/tuempresa/inventariovial/Engineering2DataTest.kt`
- `app/src/test/java/com/tuempresa/inventariovial/Engineering2ScreenTest.kt`
- `app/src/main/java/com/tuempresa/inventariovial/field/FieldUi.kt`
- `app/src/main/java/com/tuempresa/inventariovial/MainActivity.kt`
- `app/src/main/java/com/tuempresa/inventariovial/scap/ui/ScapController.kt`
- `app/src/main/java/com/tuempresa/inventariovial/scap/data/ScapDao.kt`
- `app/src/main/java/com/tuempresa/inventariovial/scap/ui/ScapExportPanel.kt`
- `app/src/test/java/com/tuempresa/inventariovial/ScapExportTest.kt`
- `app/src/main/java/com/tuempresa/inventariovial/scap/data/ScapRepository.kt`
- `app/src/main/java/com/tuempresa/inventariovial/scap/export/ScapSicExporter.kt`
- `app/src/main/java/com/tuempresa/inventariovial/scap/ui/ScapSicSupplementPanel.kt`
- `app/src/main/java/com/tuempresa/inventariovial/scap/domain/ScapToSicMapper.kt`
- `app/src/main/java/com/tuempresa/inventariovial/scap/ui/ScapWorkspace.kt`
- `app/src/main/java/com/tuempresa/inventariovial/supplementary/SupplementaryForms.kt`

Nuevos:

- `app/src/main/java/com/tuempresa/inventariovial/scap/domain/BridgeSicContract.kt`
- `app/src/main/java/com/tuempresa/inventariovial/scap/ui/ScapBridgeSicPanel.kt`
- `app/src/test/java/com/tuempresa/inventariovial/BridgeSicFlowTest.kt`
- `app/src/test/java/com/tuempresa/inventariovial/BridgeSicUiTest.kt`
- `docs/PUENTES_SIC_FLUJO_2026-10-06.md`

También se actualiza `docs/SCAP_TO_SIC_MAPPING.md`. `MainActivity.kt` solo corrige el identificador del tema partido por espacios que impedía compilar. `FieldUi.kt` únicamente restringe capacidad sin definición en SIC17B; el bloque SIC18A se conserva. `SupplementaryForms.kt` cambia tres etiquetas B, preservando todos los catálogos. No se modificaron fuentes de pavimentos, esquema Room, plantilla SCAP, catálogos SIC18–23, autenticación ni contratos Drive.

## Pruebas

Nuevos: `BridgeSicFlowTest` (5 casos: ubicación, persistencia/exportación, overrides, migración, warnings) y `BridgeSicUiTest` (2 casos Compose: captura real y ubicación sin inputs duplicados). Se actualizan solo los casos SIC de `Engineering2DataTest`, `Engineering2ScreenTest` y `ScapExportTest` para el nuevo mínimo de exportación y navegación.

Resultados finales: consultar `output/sic-bridge-test-summary.txt` y `output/sic-bridge-full.log`. La referencia previa es `output/pavement-test-summary.txt` (230 pruebas, 15 fallos). El subconjunto final SIC ejecutó **10 pruebas, 0 fallos**, y `assembleDebug` terminó **BUILD SUCCESSFUL** (registro `output/sic-bridge-focused.log`; XML preservados en `output/sic-bridge-focused-results`). La suite completa final ejecutó **237 pruebas: 222 pasaron y 15 fallaron**; el comando terminó con código 1 por tests. Los nombres de los 15 fallos coinciden exactamente con la referencia previa, sin fallos nuevos. `assembleDebug` en esa ejecución quedó UP-TO-DATE. El log completo terminó BUILD FAILED exclusivamente por la tarea de tests. Los 25 tests de pavimentos también pasan. La ejecución independiente final `gradlew assembleDebug` terminó con código **0 / BUILD SUCCESSFUL**, registrada en `output/sic-bridge-assemble.log`.

## Fallos de referencia fuera de esta corrección

El informe previo `output/pavement-test-summary.txt` registra 15 fallos: expectativa de versión 11 en Engineering2DataTest; control F.2 no encontrado en Engineering2ScreenTest; builders de tests de migración sin la ruta completa hacia v12 (EngineeringUx, InventoryRoomMigration, PrSuggestionStorage, RoadAxisStorage, RoundTwoStorage, ScapStorage, SurveyStorage y UserAuthentication); dos expectativas de conteo de capas en RoadReferenceDelivery; dos verificaciones previas de plantilla/perfil SCAP en ScapExport/ScapFullExport; ANDROID_ID ausente en UserAuthentication. La comparación final por nombre exacto queda en `output/sic-bridge-test-summary.txt`. Se conserva la plantilla contractual y no se amplía esta entrega para modificar esas áreas.

## Checklist de tablet (pendiente de ejecución física)

1. Crear/abrir puente.
2. Completar SCAP.
3. Ir a Datos para SIC.
4. Abrir SIC-17.
5. Confirmar PR y distancia heredados; revisar ubicación final.
6. Abrir SIC-17A.
7. Completar sus campos, incluidos carriles, tablero, superestructura y alineamiento.
8. Volver al resumen.
9. Confirmar Completo; revisar opcionales que aún estén vacíos.
10. Abrir SIC-17B.
11. Completar campos disponibles, clasificaciones globales y No aplica pertinente.
12. Esperar Guardado local.
13. Cerrar app.
14. Reabrir.
15. Confirmar persistencia, procedencia y descripción de superficie 3.
16. Generar SIC-17.
17. Generar SIC-17A.
18. Generar SIC-17B.
19. Abrir los Excel.
20. Comparar cada valor con lo ingresado, incluidos códigos y comentarios multilínea.
21. Confirmar que no queden warnings USER INPUT falsos; completar faltantes legítimos.
22. Confirmar capacidad máxima solo en diagnóstico, sin unidad ni valor inventado; WGS84 separado de UTM.

APK de revisión: `output/GBO_InventarioVial_PUENTES_SIC_2026-10-06-debug.apk`. Diff de código: `output/sic-bridge-audit/changes.diff`; archivos: `files-changed.txt`; hashes y metadatos: `protected-verification.json` y `artifact.json` en esa misma carpeta.

No se hizo commit ni push.
