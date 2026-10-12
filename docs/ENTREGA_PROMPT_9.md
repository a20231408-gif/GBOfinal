# Prompt 9 — entrega parcial: corrección de los cierres

Fecha: 12/10/2026 (America/Lima). Rama: `codex/ajustes-campo-9`.
Partida verificada: `codex/integracion-real-8B`, `a55ed8e`, árbol limpio.

## Alcance y puerta de aceptación

Esta entrega se limita a la fase 1 del Bloque A. **No es una versión aprobada para campo ni la implementación completa del Prompt 9.** El código del defecto se reprodujo y corrigió con Canvas nativo en Robolectric. La aceptación en Android/tablet continúa pendiente: no había dispositivos ADB conectados ni AVD instalados; el emulador aislado preparado para esta tarea no completó su arranque por software. El controlador de aceleración no está instalado. No se instalaron drivers ni se modificó Windows.

Se respeta la prioridad solicitada: no continuar con condiciones, catálogos, nuevos listados, finalización, GNSS/KMZ o videos hasta cerrar la aceptación instrumentada de los dos recorridos. Las pruebas instrumentadas se entregan compiladas, pero **no ejecutadas**. No hay Logcat de la aplicación ni validación física en esta entrega.

## Error reproducido y causa real

`LocalPhotoImage`, en `field/InventoryPhotoGallery.kt`, empleaba:

```kotlin
DisposableEffect(bitmap) { onDispose { bitmap?.recycle() } }
```

`bitmap` es una propiedad delegada a estado. Cuando la decodificación cambia el estado de `null` a una imagen, Compose descarta el efecto anterior. Su callback lee el estado actual y recicla la imagen recién cargada, que `Image` acaba de entregar al renderizador. El dibujo posterior lanza:

```text
java.lang.RuntimeException: Canvas: trying to use a recycled bitmap
android.graphics.BaseCanvas.throwIfCannotDraw
android.graphics.RecordingCanvas.throwIfCannotDraw
androidx.compose.ui.graphics.AndroidCanvas.drawImageRect
androidx.compose.ui.graphics.painter.BitmapPainter.onDraw
```

Se obtuvo este fallo **antes de editar producción**, primero en la miniatura y luego en los recorridos completos `RecordDetailScreen → VER FOTOGRAFÍAS` y `SicExportScreen` con una fotografía seleccionada. Las trazas son de pruebas JVM con Android/Canvas nativos, no de una tablet. La prueba anterior de navegación pasaba porque verificaba principalmente semántica y no esperaba la decodificación antes de dibujar.

Se eliminó el reciclado manual de miniaturas usadas por Compose. Se mantiene la decodificación en IO, orientación EXIF, carga local y muestreo acotado a 480 píxeles en revisión. El recolector libera la imagen cuando el estado y el renderizador dejan de referenciarla. Además, el panel de detalle coloca sus títulos y galerías en una `Column`, evitando superposición de varios hijos dentro del mismo elemento de `LazyColumn`.

Evidencia anterior: `campo-9/evidencia/ANTES_MINIATURA.xml`, `ANTES_DETALLE.xml` y `ANTES_EXPORTACIONES_Y_DETALLE_INICIAL.xml`. Este último conserva también un fallo inicial del fixture por tamaño de pantalla, identificado como timeout; no se presenta ese timeout como causa del cierre. El detalle se reprodujo después con dimensiones de tablet y tiene su traza separada.

## Integridad y compatibilidad

- Afecta la revisión de fotografías de los SIC que utilizan el componente compartido; los fixtures completos son SIC-18 con categoría histórica nula. No cambia campos ni significado de ningún SIC.
- Los catálogos de condición, las categorías fotográficas y la exportación contractual existentes permanecen en su implementación anterior. No se completaron las fases 2/3.
- No modifica UUID, Room 18, índices, revisiones, colas WorkManager, ACK, nombres ni asociaciones de Drive. Abrir las pantallas continúa siendo una consulta local.
- No modifica los dos proyectos Apps Script, ni ejecuta entregas reales o borra archivos remotos. Se mantienen `syncProtocol=3`, `pathVersion=4`, `photoCategoryVersion=1`, numeración estable y entrega técnica sin GPS.
- No incorpora videos ni habilita su subida. Tampoco entrega un parche de videos: la auditoría y ampliación corresponden al Bloque C pendiente.
- No cambia firma histórica, keystore, applicationId, versionCode, usuarios ni APK de campo. No se generó Release ni se instaló/desinstaló la aplicación de la tablet.

## Estado de los requisitos restantes

| Requisito | Estado en esta entrega |
|---|---|
| Condición visual reutilizable en todos los formatos | Pendiente, sin cambios |
| Categorías predeterminadas verificadas por formato | Pendiente; se conserva catálogo anterior |
| Videos Android, persistencia, cola y servidor | Pendiente y deshabilitados |
| Prefijos numéricos de fotos/videos | Pendiente; nombres históricos conservados |
| Listados equivalentes en todas las familias | Pendiente; listados anteriores conservados |
| Ordenamiento GNSS/KMZ adicional | Pendiente; lógica anterior conservada |
| Revisar/finalizar en todos los módulos | Pendiente; validaciones anteriores conservadas |
| Ayuda GNSS, navegación y sesión roja | Pendiente |
| Guía de la carpeta técnica Drive | Incluida en `GUIA_BREVE_INGENIEROS.md` |
| Pruebas Android y físicas de los dos recorridos | Pendientes |

## Validación, APK y commits

Los resultados definitivos se registran en `MATRIZ_VALIDACION_9.md`: 54 pruebas distintas aprobadas entre la ejecución inicial (53/54) y la repetición focalizada del timeout del test (1/1). Debug y APK instrumentada compilan correctamente. La compilación usa `-PgboValidationBuild=true` para excluir los endpoints y credenciales locales sin editar sus archivos. La APK resultante es para pruebas locales; no permite validar una entrega real a Drive.

APK Debug: `app/build/outputs/apk/debug/app-debug.apk`.
APK instrumentada: `app/build/outputs/apk/androidTest/debug/app-debug-androidTest.apk`.

Commit de corrección y regresiones: `e2b3b5a`. Otro commit conserva esta entrega, matriz y guías. Para obtener el commit final exacto del checkout publicado: `git rev-parse HEAD`. El hash se informa también en la respuesta de entrega; el documento no intenta incluir su propio hash.

Durante la ejecución aparecieron archivos en `app/release/` ajenos a los comandos Debug de esta tarea. Se conservaron sin modificarlos, revisarlos ni incluirlos en esta entrega; esta tarea no ejecutó `assembleRelease`.

Riesgo pendiente: el comportamiento de renderizado, rotación, memoria y archivos reales de la tablet requiere ejecución instrumentada y física. La evidencia JVM confirma un defecto compartido, pero no excluye otros cierres dependientes de datos o dispositivo. El siguiente paso es ejecutar `FieldCrashes9DeviceTest` en un emulador operativo aislado, revisar Logcat y completar la lista física. No avanzar al Bloque B/C con esa aceptación abierta.
