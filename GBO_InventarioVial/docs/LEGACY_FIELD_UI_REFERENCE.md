# Referencia real de captura histórica

Auditoría previa a los cambios sobre la entrega UI v12. Se verificaron los 586 hashes recibidos contra su ZIP; baseline: 362 pruebas, 347 aprobadas y 15 fallos históricos. La carpeta build había sido limpiada; las identidades se toman del informe de ejecución verificado de v12.

La referencia canónica es **AssetFormScreen(CULVERT): Registrar Alcantarilla · SIC-18**, en `app/src/main/java/com/tuempresa/inventariovial/MainActivity.kt`. No se reconstruye desde imágenes. Su LazyColumn usa margen horizontal 24 dp, contenido superior 24/inferior 40 dp y separación 18 dp. El botón de borrador precede a Volver y al título. Estos valores se trasladan con el bloque existente, sin rediseño.

| Composable / bloque recibido | Archivo dentro de app/src/main/java/com/tuempresa/inventariovial | Uso real | Consumidores recibidos |
|---|---|---|---|
| AssetFormScreen | MainActivity.kt | Raíz, LazyColumn, navegación, borrador, campos, guardar | SIC-17 Puentes, SIC-18 Alcantarillas, SIC-19 Cunetas/drenaje, SIC-20 Badenes/Muros/Túneles, SIC-23 derecho de vía |
| SignalizationFormScreen | MainActivity.kt | LazyColumn con la misma cabecera, navegación y orden vertical | SIC-21 horizontal / SIC-22 vertical |
| SurveyHeader | field/SurveyHeader.kt | Ruta, Calzada, sentido, reinicio de continuidad | AssetFormScreen y SignalizationFormScreen |
| ChoiceSelector | MainActivity.kt | Catálogos y chips/dropdown según cantidad de opciones | Todos los formularios históricos y campos técnicos |
| ProjectRouteFields | field/ProjectRouteFields.kt | Ítem/tramo derivados del catálogo de Ruta | Cabecera SurveyHeader, sesión |
| TrackCapturePanel | field/CaptureToolsUi.kt | INICIAR/FINALIZAR RECORRIDO, track, posición final | Elementos históricos lineales |
| Actualizar GPS inicial | MainActivity.kt: bloque AssetFormScreen | Botón OutlinedButton con adquisición TabletLocationProvider y permisos | AssetFormScreen; equivalente del elemento en SIC-22 |
| LocationSummaryCard | core/FieldCommonUi.kt | GNSS compacto actual; detalles expandibles | Formularios históricos actuales y otros módulos |
| SectionTitle + LocationSuggestionPanel + FieldPrPanel + PR/distancia verticales | MainActivity.kt, field/LocationSuggestionPanel.kt, field/FieldPrPanel.kt | Inicio, fin, confirmación GNSS, catálogo contractual PR | AssetFormScreen, SignalizationFormScreen |
| FinalLocationCapture | RecordHistoryScreen.kt | Posición final con proveedor/permisos actuales | Históricos lineales y PR de campo |
| Sic18Fields | MainActivity.kt | Clase/tipo/vanos/dimensiones/condiciones; ChoiceSelector y ConditionSelector | Alcantarillas SIC-18; edición SIC-18/18A |
| Sic19Fields | MainActivity.kt | Campos técnicos de drenaje | SIC-19 |
| Sic20Fields | MainActivity.kt | Campos técnicos según clase 12/13/14 | Badenes/Muros/Túneles SIC-20 |
| Sic22Fields | MainActivity.kt | Señalización vertical y selector MAN_6 | SIC-22 |
| CulvertPhotos | field/CulvertPhotos.kt | Categorías, cámara, importación, miniaturas, preview y borrado confirmado | Alcantarillas SIC-18 y edición SIC-18/18A |
| InventoryPhotoGallery + PhotoCaptureCard | field/InventoryPhotoGallery.kt, core/FieldCommonUi.kt | Cámara/importación, miniaturas, preview, eliminar | Otros históricos y módulos actuales |
| SaveWarningsDialog + RecordReviewSection | field/FieldUi.kt, core/FieldCommonUi.kt | Revisión modal: resumen, fotos, avisos, Confirmar y guardar/Revisar | Guardado del ViewModel en históricos |
| Revisar y finalizar | MainActivity.kt: bloque AssetFormScreen | Botón final de ancho completo, 72 dp | AssetFormScreen |

## Extracción prevista y procedencia

`LegacyInventoryFormScaffold` sale del LazyColumn y sus tres primeras filas en AssetFormScreen. `LegacyLocationSection` sale de sus bloques de PR/distancia y sugerencia GNSS. `LegacyInventoryReviewButton` sale de su botón final. `LegacyRecordReviewDialog` sale del AlertDialog de SaveWarningsDialog. `LegacyInventoryPhotos` sale del cuerpo de CulvertPhotos, parametrizando solamente SIC/categorías existentes. SurveyHeader, ChoiceSelector, TrackCapturePanel, FieldPrPanel, LocationSummaryCard, FinalLocationCapture, PhotoCaptureCard, Sic18Fields y selector MAN_6 se consumen directamente.

No se cambia Room, los formatos contractuales, exportadores ni Drive. No se refactorizan todas las pantallas: Alcantarilla y las pantallas que ya comparten AssetFormScreen consumen la extracción; Señalización reutiliza el contenedor y mantiene sus campos/manipuladores originales.
