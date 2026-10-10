# Mapeo SCAP → SIC-17 / SIC-17A / SIC-17B

Actualizado 2026-10-06. Contrato, catálogos, persistencia y validación: [informe de flujo](PUENTES_SIC_FLUJO_2026-10-06.md).

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

## Correspondencias que no se aplican

Vías de tránsito ≠ equivalencia aprobada de carriles; alineamiento SCAP no tiene tabla de equivalencias; anchos parciales no se suman para tablero/superestructura; UTM Este/Norte no se copia como latitud/longitud ni se convierte sin transformación validada. Categoría/tipo SCAP, índice/porcentajes de deterioro, materiales y códigos de filas estructurales no se convierten por índice del selector o por primera fila. Cargas actual/futura/sobreesfuerzo no se usan como capacidad máxima. Croquis, defectos y perfil conservan el detalle SCAP sin nuevos destinos SIC.

