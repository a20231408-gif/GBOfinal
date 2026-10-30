# Equivalencias revisadas SCAP / SIC-17 / SIC-17A / SIC-17B

Fuentes: ScapToSicMapper, BridgeSicContract, ScapSicSupplement, ScapRepository y los mapas de plantilla SCAP existentes. Son proyecciones de datos GUARDADOS, recalculadas antes de exportar, no sincronización bidireccional ni nuevos registros. Las unidades y campos se conservan; valores vacíos siguen pendientes.

| Origen guardado | Destino | Regla |
|---|---|---|
| Registro vial vinculado routeCode, roadbedCode | SIC-17 ruta/calzada | Prevalece la ubicación SIC confirmada; una edición arbitraria de SCAP no reemplaza PR ya confirmado. |
| Registro vial startPrCode/startDistanceM/endPrCode/endDistanceM | SIC-17 PR inicial/final y metros | Copia de campos equivalentes, no conversión a longitud acumulada. |
| inspection/bridgeCode, con respaldo inspection.bridgeCode | SIC-17/17A/17B código de puente | Identidad física del mismo puente, sin crear correlativos ni renumerar. |
| bridgeName, con respaldo inspection.bridgeName (B refleja singularityName) | SIC-17A nombre del puente | Nombre guardado; categoría A «PUENTE» no se inventa como nombre propio. |
| totalLengthM | SIC-17 dimension1LengthM | Longitud total del puente, metros. No longitud de acceso ni cálculo entre PR. |
| lowerClearanceM / upperClearanceM | SIC-17 dimension2LowerHeightM / dimension3UpperHeightM | Gálibos correspondientes, metros; no altura de estribo/pilar. |
| lastInspection | SIC-17 surveyDate | Fecha de inspección, no fecha de modificación o subida. |
| spans.count | SIC-17 spans | Transformación existente de cantidad de tramos; no suma de luces. |
| constructionYear, politicalDepartment, province, district, nearbyTown | SIC-17A homólogos | Correspondencia administrativa explícita; no deducción por coordenadas. |
| latitude / longitude canónicas; GNSS de inspección si faltan | SIC-17A latitud/longitud | Grados WGS84. No se convierte UTM de plantillas históricas. |
| altitudeM | SIC-17A altitude | Valor explícito de altura, no una cota inferida. |
| roadwayWidthM / sidewalkWidthM | SIC-17A homólogos | Anchos específicos correspondientes; no se usa ancho total por tener igual unidad. Se conserva política de aplicabilidad de la ficha. |
| designLoad / mainSpanM | SIC-17B homólogos | Sobrecarga de diseño / luz principal explícitas; no capacidad máxima ni longitud total. |
| Suplemento sic17 y entidades sic17a/sic17b | Sus campos SIC | Valores manuales, códigos contractuales y ediciones tienen prioridad. Una edición explícita vacía no se repuebla silenciosamente. |

Permanecen independientes: ancho total de tablero y superestructura frente a calzada/vereda; carriles frente a vías de tránsito; longitudes de cada tramo/acceso; alturas de subestructuras frente a gálibos; máxima capacidad frente a sobrecarga; clasificaciones SIC globales frente a códigos de elementos SCAP; condición estructural/funcional frente al resultado estadístico G.1; singularidad salvada frente a nombre del puente; UTM frente a GNSS. Capacidad máxima conserva su discrepancia contractual documentada y no se inventa.

Si una edición SIC difiere del origen SCAP, se conserva y aparece REVISAR; el botón Restaurar SCAP existente sigue siendo una decisión explícita. No hay bucles de copia: editar SIC no cambia SCAP, ni exportar escribe valores heredados en otras tablas. Las pestañas observan la misma inspección y no crean IDs.

Pruebas: una longitud SCAP 18.75 se revisa manualmente a SIC 20.5; al cambiar SCAP a 19.25 se conserva SIC 20.5 con advertencia. Código común llega a los tres formatos; editar poblado en SIC-17A, pasar a SIC-17B y volver conserva el dato en Room, con una sola inspección. Pruebas locales, no validación física.
