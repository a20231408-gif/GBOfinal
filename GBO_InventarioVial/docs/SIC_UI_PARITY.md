# Paridad de la captura histórica

Referencia canónica: **Registrar Alcantarilla · SIC-18**, mediante `AssetFormScreen(CULVERT)` de la versión recibida v12. La extracción preserva su contenedor y controles; los consumidores comparten el mismo `LegacyInventoryFormScaffold`. No se diseñó un formulario nuevo mirando imágenes.

## Capturas reales

Estas capturas provienen de las pantallas de producción Compose ejecutadas en pruebas Robolectric API 34, 800 × 1200 px, mdpi, gráficos nativos. Se dibuja la vista Android real sobre un Bitmap/Canvas nativo. PixelCopy no completó en este host; no se fabricaron imágenes ni se usaron bocetos como evidencia. No representan pruebas en una tablet física.

| Captura | Pantalla real | Raíz y cabecera |
|---|---|---|
| [A · Alcantarilla](sic-ui-parity-evidence/screenshots/A_Alcantarilla_SIC18.png) | AssetFormScreen(CULVERT) | LegacyInventoryFormScaffold + SurveyHeader |
| [B · Faja](sic-ui-parity-evidence/screenshots/B_Faja_SIC08_09.png) | SicFieldWorkspace → LegacySicElementForm | LegacyInventoryFormScaffold + SurveyHeader |
| [C · Punto Crítico](sic-ui-parity-evidence/screenshots/C_PuntoCritico_SIC13.png) | SicFieldWorkspace → LegacySicElementForm | LegacyInventoryFormScaffold + SurveyHeader |
| [D · Accidente](sic-ui-parity-evidence/screenshots/D_Accidente_SIC34.png) | SicFieldWorkspace → LegacySicElementForm | LegacyInventoryFormScaffold + SurveyHeader |
| [E · SIC-18A embebido](sic-ui-parity-evidence/screenshots/E_SIC18A_embebido_legacy.png) | Destino SIC-18A → Alcantarilla, detalle dentro de la misma lista | Sic18Fields + CulvertSupplementFields |
| [F · Campos y botón final](sic-ui-parity-evidence/screenshots/F_Faja_revision_legacy.png) | Faja: características, anchos, fotografías y botón de revisión | LegacySicElementForm + LegacyInventoryReviewButton |

La comparación automática está en [visual-parity.json](sic-ui-parity-evidence/visual-parity.json), junto con hashes PNG y las medidas `.bounds.json`. Los botones de borrador/Volver y el bloque Ruta/Calzada/sentido/otro recorrido coinciden píxel por píxel con A. El título mantiene origen y altura; texto y ancho varían con el nombre del elemento. La diferencia de ubicación vertical después de la cabecera responde al `TrackCapturePanel` existente, aplicable a elementos lineales; Alcantarilla es puntual.

F captura la ventana del formulario, mostrando sus campos y botón final. El diálogo de revisión vive en una ventana separada y se valida en la prueba Compose por su título, controles y confirmación deshabilitada con datos pendientes; no se presenta F como captura de ese diálogo.

`LocationSummaryCard` y `LocationSuggestionPanel` se conservan porque ya forman parte de la versión actual de las pantallas históricas. Se eliminaron las nuevas cards de Identificación, Ubicación inicio/fin, Datos del elemento y Anchos de la presentación genérica. Los campos técnicos van en vertical, sin dos columnas ni ancho máximo propio.

## Checklist obligatorio

“Sí” significa consumo del mismo componente y flujo, no una copia de colores o tamaños.

| Comprobación | Faja 08/09 | Punto Crítico 13 | Accidente 34 | Código común / evidencia |
|---|---|---|---|---|
| Guardar borrador igual y arriba | Sí | Sí | Sí | LegacyInventoryFormScaffold; píxeles idénticos |
| Volver igual | Sí | Sí | Sí | Misma segunda fila del contenedor; píxeles idénticos |
| Título con mismo estilo | Sí | Sí | Sí | Misma llamada Text extraída de AssetFormScreen; origen/altura idénticos |
| Subtítulo con mismo estilo | Sí | Sí | Sí | Mismo Text/bodyLarge del contenedor |
| Ruta con mismo selector/catálogo | Sí | Sí | Sí | SurveyHeader → ChoiceSelector → ProjectRouteCatalog; prueba de selección real |
| Calzada con mismo selector | Sí | Sí | Sí | SurveyHeader → catálogo SicCatalogRepository y opción Otro |
| Código de calzada con mismo control | Sí | Sí | Sí | OutlinedTextField existente dentro de SurveyHeader; píxeles idénticos |
| Sentido con mismos chips | Sí | Sí | Sí | ChoiceSelector existente; selección creciente/decreciente probada |
| Recorrido con mismo botón | Sí | Sí | Sí | SurveyHeader → Iniciar otro recorrido/SurveyPreferences.reset; TrackCapturePanel existente |
| Ubicación con misma UI | Sí | Sí | Sí | LegacyInitialGpsButton, LocationSummaryCard, LegacyLocationSection, LocationSuggestionPanel, FieldPrPanel, FinalLocationCapture |
| Fotos con misma UI | Sí | Sí | Sí | LegacyInventoryPhotos extraído del cuerpo de CulvertPhotos; PhotoCaptureCard/PhotoPreview y contratos de captura existentes |
| Revisión con misma UI | Sí | Sí | Sí | LegacyRecordReviewDialog extraído de SaveWarningsDialog; RecordReviewSection |
| Botones finales iguales | Sí | Sí | Sí | LegacyInventoryReviewButton extraído del footer de AssetFormScreen; Confirmar y guardar/Revisar del diálogo histórico |

Las categorías son las definidas por `PhotoCategoryPolicy`: General para nuevos SIC; Panorámica/Entrada/Salida para Alcantarillas. Esta diferencia corresponde al contenido técnico; cámara, importación, miniaturas, preview y borrado confirmado usan la misma implementación.

## Composición y datos conservados

- Faja mantiene una sola ficha 08/09, un UUID principal y `sourceRecordId` del ancho; los anchos pueden completarse después. Un destino SIC-09 vinculado abre esa misma Faja.
- Punto Crítico aporta Lado/Clase. Accidente aporta Faja/Tipo/Nivel/Descripción. Los controles técnicos usan ChoiceSelector/OutlinedTextField existentes.
- SIC-18A se expande en Alcantarilla cuando hay condición 3. No repite Ruta, Calzada, PR, dimensiones, clase/tipo/vanos ni fotos. Los antecedentes y Excel contractuales separados se conservan.
- SicFieldWorkspace administra estado, borradores, repositorio, validación y exportación; su renderizado genérico anterior se retiró. LegacySicElementForm solo inserta contenido técnico/callbacks en el formulario histórico.
- Room permanece en 15; sin migración. SicFieldRepository, SicExtensionEntity, exportadores, modelos contractuales, historial y Drive permanecen sin modificaciones en esta tarea.

## Alcance de la validación

Capturas, navegación, conservación de UUID/texto crudo/sentido/anchos, revisión y destinos unificados se verifican en Compose/Room local. La suite completa y el build final se documentan en [SIC_UI_PARITY_REPORT.md](SIC_UI_PARITY_REPORT.md). Cámara/GNSS físicos, instalación y revisión en dispositivos reales siguen pendientes; no se declaró una actualización de campo.
