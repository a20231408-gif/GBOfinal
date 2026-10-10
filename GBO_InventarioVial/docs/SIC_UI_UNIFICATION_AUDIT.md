# Auditoría previa de UI SIC

Base: workspace actual posterior a la corrección de producción, Room 15, versionCode 11. Auditoría de código Compose y comportamiento de callbacks antes de editar; no captura de tablet física. Se conserva evidencia de hashes y baseline exacto en ui-unification-evidence.

| Dimensión | Alcantarillas SIC-18 / Señalización SIC-22 | SicFieldWorkspace anterior | Decisión |
|---|---|---|---|
| Encabezado | Registrar elemento; SIC como subtítulo | Código SIC antes del nombre | Objeto primero, código secundario |
| Navegación | Home por RoadAssetType; formularios específicos | Cinco entradas separadas incluso formatos hermanos | Fajas agrupa 08/09; Alcantarillas integra 18A |
| Ubicación | SurveyHeader, PR/distancia, LocationSummaryCard | Inputs genéricos mezclados con técnica | Secciones Ubicación inicio/fin; resumen GNSS colapsado |
| GNSS | Resumen colapsable y captura/sugerencias | PR contractual; no captura propia | Reutilizar LocationSummaryCard y capturador de ubicación |
| Títulos | SectionTitle | Labels genéricos sin secciones | Reutilizar SectionTitle y cards compartidas |
| Espaciado | 18 dp lista, controles material | 12 dp y dos columnas fijas | Columnas adaptativas; ancho máximo en tablets |
| Cards | Condición, fotos y revisión | Campos planos | Secciones coherentes sin cambiar colores del tema |
| Dropdowns | ChoiceSelector (chips/lista) | SicCodeDropdown con implementación propia | Adaptar códigos a ChoiceSelector existente |
| Botones | Borrador / Revisar y guardar | Borrador / Revisar / finalizar | Borrador / Revisar y finalizar / Exportar |
| Fotos | PhotoCaptureCard, CulvertPhotos, preview y confirmación | Solo SIC18A; resto sin bloque | Reutilizar captura/galería; fotos 18 y 18A únicas |
| Guardar | Borrador crudo y final con validación | SicFieldRepository, estado DRAFT/COMPLETE | Mantener contratos; agregar guardado agregado transaccional |
| Revisión | RecordReviewSection compartido | Revisión solo datos de formato | Ubicación + técnica + fotos + pendientes |
| Historial | Fila de inventario; 18A ya comparte UUID | 08 y 09 generan filas distintas | Agrupar por sourceRecordId; conservar registros sin relación |
| Exportación | SIC separados y entrega RouteDelivery | Uno por pantalla | Botón por elemento, archivos contractuales separados |
| Drive | 18A ya usa carpeta 18; 09 carpeta propia | Familias correctas, duplicación de faja vinculada | 09 vinculado en carpeta 08; ACK y payload sin cambios |

No se cambia Room: sic_extension_details.sourceRecordId ya relaciona 09→08; 18A usa UUID de 18. Los SIC09 históricos sin vínculo deben seguir visibles y exportables; no se infieren relaciones por PR ni se eliminan registros.

SIC-18 actual se mantiene como base: Sic18Fields, ConditionSelector, CulvertPhotos, guardado y validación existentes. El detalle adicional se adapta a ese flujo y no reemplaza la ficha por una UI genérica.

Baseline registrado desde XML actuales: 347 casos, 332 PASSED, 15 FAILED. Se compararán identidades, sin aceptar nuevos fallos ni fijar un total posterior que ignore los tests añadidos. No se generará release en esta iteración; signing y applicationId quedan protegidos.
