# SIC UI PARITY REPORT

## Resultado

Los nuevos SIC ahora utilizan los mismos componentes y el mismo scaffold que los formularios históricos. La referencia real es **AssetFormScreen(CULVERT): Registrar Alcantarilla · SIC-18 · Obras de drenaje**, recibida en la entrega v12. El trabajo se hizo sobre ese código más reciente, comparado por hash contra el ZIP entregado; no se volvió a una versión anterior.

`LegacyInventoryFormScaffold` es la extracción del LazyColumn y sus primeras filas de AssetFormScreen. Alcantarillas, Badenes, Muros, Túneles, Cunetas/drenaje, Puentes y derecho de vía ya consumían AssetFormScreen y ahora pasan por ese mismo contenedor extraído. Señalización consume la extracción conservando sus campos y handlers. No se creó un Scaffold Material nuevo ni un design system paralelo.

## Composables realmente compartidos

| Componente | Procedencia / uso |
|---|---|
| LegacyInventoryFormScaffold | LazyColumn histórico: borrador arriba, Volver, título/subtítulo, margen/espaciado originales |
| SurveyHeader | Implementación existente sin modificaciones: catálogo Ruta, ítem/tramo, Calzada, código, sentido e Iniciar otro recorrido |
| ChoiceSelector / ProjectRouteFields | Mismos catálogos y controles de la cabecera histórica |
| LegacyInitialGpsButton | Botón extraído de AssetFormScreen; históricos conservan sus handlers de adquisición |
| LegacyLocationSection | Bloques históricos de PR/distancia, LocationSuggestionPanel y FieldPrPanel, en vertical |
| LocationSummaryCard / FinalLocationCapture | Componentes actuales existentes de GNSS/posición final |
| TrackCapturePanel | Recorrido existente para elementos lineales; no se crea otro capturador |
| LegacyInventoryPhotos | Cuerpo real de CulvertPhotos parametrizado por PhotoCategoryPolicy; cámara, importación, miniaturas, preview y borrado iguales |
| CulvertPhotos / Sic18Fields | Adaptador SIC-18 conserva categorías y campos base; 18A queda embebido |
| LegacyInventoryReviewButton | Botón final de AssetFormScreen, extraído sin rediseño |
| LegacyRecordReviewDialog / RecordReviewSection | Diálogo real extraído de SaveWarningsDialog; históricos y nuevos consumen la misma revisión |

`SurveyPreferences.remember(request)` delega a una sobrecarga que recibe SurveyPosition, usando exactamente el mismo almacén de continuidad. Los nuevos SIC pueden registrar su posición sin inventar un detalle contractual de Alcantarilla. Iniciar otro recorrido conserva su reset existente. El sentido y los valores crudos de los nuevos SIC siguen en el JSON existente de SicExtensionEntity; no se agrega un modelo Room.

## Código de SicFieldWorkspace que dejó de imponer UI

Se retiró su Box/ancho máximo de 1000 dp, su LazyColumn propio, la card Identificación, cards nuevas de Ubicación inicio/fin, cards Datos del elemento/Anchos, columnas adaptativas, sección fotográfica específica, revisión inline y botones de borrador/finalización al final. Esos bloques ya no renderizan el formulario principal.

SicFieldWorkspace mantiene estado, recuperación, repositorio, validación contractual, borradores, UUID, anchos vinculados y exportación. Delega la presentación a LegacySicElementForm, que inserta únicamente campos y callbacks en el contenedor histórico compartido. InventoryFieldRows permanece disponible para el dashboard, no para los campos de captura nuevos.

## Nuevos SIC y navegación

- Faja: Registrar Faja / SIC-08 · SIC-09; cabecera histórica; Características de faja (Faja, Tipo de faja, Sentido del tránsito); Ancho de faja (útil/total). Los anchos siguen siendo opcionales y completables después.
- Punto Crítico: Registrar Punto Crítico / SIC-13 · Puntos críticos; misma cabecera/ubicación; Lado y Clase.
- Accidente: Registrar Accidente / SIC-34 · Accidentes de tránsito; misma cabecera/ubicación; Faja, Tipo, Nivel y Descripción.
- SIC-09 vinculado abre la Faja principal 08/09. Los anchos históricos sin vínculo siguen accesibles sin adivinar relaciones.
- SIC-18A abre la Alcantarilla y se completa dentro del mismo formulario. Si hay condición 3, el botón histórico expande función, ubicación/tipo de falla estructural y estado/causa funcional, en vertical. No duplica encabezado, PR, clase/tipo/vanos ni fotografías.
- El historial orientado a elementos y los Excel separados SIC-08/SIC-09 y SIC-18/SIC-18A se conservan.

Los nuevos campos usan los controles actuales ChoiceSelector/OutlinedTextField. LocationSummaryCard y los bloques de sugerencias GNSS permanecen porque ya los utilizan las pantallas históricas de la versión actual. No se copiaron colores ni tipografías para simular paridad.

## Room, exportadores y funcionalidades protegidas

Room **15**, sin nueva migración. SicExtensionEntity, sourceRecordId, UUID, SicFieldRepository, schemas, InventoryDatabase, migraciones, exportadores/XLSX, historial, Drive/ACK, backend y Apps Script permanecen sin cambios en esta corrección. Las relaciones y transacciones unificadas anteriores siguen vigentes.

De 586 archivos recibidos registrados, 579 mantienen el mismo hash. Solo cambiaron seis archivos de composición/cabecera/revisión/fotos/continuidad y app/build.gradle.kts. Los archivos nuevos de aplicación son LegacyInventoryForm.kt, LegacySicElementForm.kt y SicUiParityTest.kt. La evidencia enumera cada excepción y conserva un diff de la versión recibida.

MAN_6 mantiene sus **207 assets**: hashes del source y APK comprobados. También se conservaron autenticación, dispositivo/huella, controles de firma, SCAP/Puentes y Pavimentos. No hubo subida Drive ni instalación física durante esta tarea.

## Paridad visual y checklist

[SIC_UI_PARITY.md](SIC_UI_PARITY.md) contiene el checklist completo, con Sí para Faja, Punto Crítico y Accidente en los 13 puntos. Se generaron A/B/C/D y capturas adicionales de 18A embebido y campos/acciones de Faja. La revisión modal se valida por sus nodos Compose y el consumo del mismo código; la captura adicional F muestra la ventana del formulario, no la ventana separada del diálogo.

Las cuatro primeras capturas provienen de las pantallas Compose de producción en Robolectric API 34 con gráficos nativos, 800 × 1200 px mdpi, dibujando la vista Android real sobre Canvas. PixelCopy no completó en este host; esta captura directa reemplaza únicamente el mecanismo de captura, no la UI. No son imágenes conceptuales ni pruebas físicas.

Comparación automática: botones superiores y bloque Ruta/Calzada/código/sentido/otro recorrido **idénticos píxel por píxel** contra Alcantarilla. Origen/altura del título y medidas de los controles comunes coinciden. El texto del título/subtítulo y los campos técnicos varían según elemento; el TrackCapturePanel se muestra cuando el elemento es lineal.

## Tests

Baseline recibido: **362** identidades, 347 PASSED / 15 FAILED. Se registró el informe verificado de v12 y se comparó su source contra el ZIP v12; build había sido limpiado antes de esta tarea.

Ejecución final completa: **370** casos; **355 PASSED / 15 FAILED / 0 SKIPPED**.
**newFailures = 0**. Ninguna identidad baseline faltante: 0. Los mismos 15 fallos históricos están identificados en full-test-results.json.

Las ocho pruebas añadidas pasan: captura de Alcantarilla canónica; Faja legacy y revisión; Punto Crítico legacy; Accidente legacy; catálogo de Ruta/chips/reinicio; guardar-salir-reabrir Faja conservando UUID/sentido/texto crudo/anchos; destino 18A dentro de Alcantarilla; destino 09 dentro de Faja 08/09. Los tests existentes de exportación, relaciones unificadas, fotos, historial y carpetas/ACK siguen pasando. Además, el verificador nuevo compara hashes exactos de exportadores, formatos y repositorio contra la base recibida.

`gradlew testDebugUnitTest --no-configuration-cache -PgboValidationBuild=true` devuelve BUILD FAILED por los 15 fallos previos; no se presenta como una suite totalmente verde. La primera ejecución de los tests nuevos detectó dos problemas del fixture (espera de carga/aislamiento entre casos), corregidos y verificados en la ejecución final.

## Build, identidad y entrega

`gradlew assembleDebug --no-configuration-cache --rerun-tasks -PgboValidationBuild=true`: **BUILD SUCCESSFUL**. El log demuestra el build completo. El flag de validación omite endpoints/tokens locales sin modificar sus archivos privados; el APK es de pruebas y no una actualización de campo.

applicationId: `com.tuempresa.inventariovial`; versionCode **12 → 13**; versionName `1.4.0-sic-production`; minSdk 26.

El único cambio semántico del script Gradle es versionCode. Firma de producción, certificado esperado, fail closed y gates se mantienen. No se generó keystore ni release; la firma histórica de producción sigue pendiente de su clave/configuración local.

APK: `GBO_InventarioVial_SIC_UI_PARITY_DEBUG_v13.apk`.
Tamaño: 39565293 bytes.
SHA-256 APK: `ac03782a8267176119c9425c8b21934f6b2687f7ea374286b324233849300b03`.
Certificado debug SHA-256: `d2e54cb086aa731ef046be300977d0b16c0d8f66fdcb08d0d9cfd2038bf0d872`, idéntico al debug v12.
apksigner Verified, APK completo sin split/test, zipalign y alineación 16 KiB: PASS. Integridad ZIP y 207 assets de señales: PASS.

Entrega: GBO_InventarioVial_SIC_UI_PARITY_SOURCE.zip; APK debug v13; SIC_UI_PARITY_REPORT.md; LEGACY_FIELD_UI_REFERENCE.md; SIC_UI_PARITY.md; capturas y hashes.json. Source excluye configuración privada, claves y binarios APK; contiene las pruebas y evidencia de esta iteración. Reportes anteriores se conservan como históricos; el verificador/empaquetador de esta versión son verify_sic_ui_parity.py y package_sic_ui_parity.py.

Pendiente: prueba de instalación/actualización, cámara y GNSS físicos en tablets. No commit. No push.
