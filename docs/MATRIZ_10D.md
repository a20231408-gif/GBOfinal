# Matriz de validación 10D

Solo pruebas focalizadas. No inferir resultados físicos a partir de Robolectric.

| Caso | Evidencia | Estado al preparar la entrega |
|---|---|---|
| ACK HTTP final perdido: reutiliza todos los stageFileId verificados y repite commit, sin duplicar archivos | SyncRecovery10DTest | Aprobado local (Robolectric; datos ficticios) |
| ACK completo vs incompleto: solo el completo confirma revisión | SyncRecovery10DTest | Aprobado local (Robolectric; datos ficticios) |
| Edición durante entrega: UUID igual, ACK anterior como base y revisión nueva pendiente | SyncRecovery10DTest | Aprobado local (Robolectric; datos ficticios) |
| Cancelación por Android: conserva preparación y deja de aparentar subida activa | SyncRecovery10DTest | Aprobado local (Robolectric; datos ficticios) |
| Offline/backoff/worker inactivo y progreso real de archivos | SyncRecovery10DTest + WorkManager | Aprobado local (Robolectric; datos ficticios) |
| Campos comunes de alta/edición SIC-17 a SIC-23; alcantarilla sin fin/sugerencia GNSS | FormRotation10DTest | Aprobado local (Robolectric; datos ficticios) |
| Restauración de navegación y formulario incompleto sin nueva ficha/revisión | FormRotation10DTest | Aprobado local (Robolectric; datos ficticios) |
| Edición de alcantarilla restaura texto y conserva datos finales históricos/Excel | FormRotation10DTest | Aprobado local (Robolectric; datos ficticios) |
| Vista previa restaurada, EXIF y original intacto | FormRotation10DTest | Aprobado local (Robolectric; datos ficticios) |
| Fotos por daño de Pavimentos, panel común y offline | Pruebas focalizadas 10C | Aprobado local; cámara, captura y Drive físicos pendientes |
| Room 18→19 mantiene fotos/UUID/cierre históricos | Prueba focalizada 10C | Aprobado local; cámara, captura y Drive físicos pendientes |
| Cliente V2 y ausencia de capacidad: reserva y ACK seguros | ReadablePhotoNames10Test | Aprobado local; cámara, captura y Drive físicos pendientes |
| Video solo SCAP: tarjeta compartida y capacidad remota desactivada | BridgeMedia10ScreenTest | Aprobado local; cámara, captura y Drive físicos pendientes |
| Huawei MatePad 12 X LRT-W09 / HarmonyOS 4.3.0: giro, cámara externa e interna, galería, entrega Drive | PRUEBAS_TABLET_10D | Pendiente físico |
| Anulación/restauración/Excel y autorización real | PENDIENTES_BACKEND_10D | Deshabilitado; no aprobado |
| Debug V2 con servicios locales privados, versión 1.4.2 (16) y firma Debug existente | apk-debug-10d-v2-test.json | Compilación y firma aprobadas; activación/prueba remota V2 pendientes |
| Debug ordinario con V2/video desactivados | apk-debug-10d.json | Compilación, configuración, versión y firma aprobadas; tablet pendiente |

Los intentos de compilación fallidos y la interrupción por RAM se guardan como evidencia. No se declaran como pruebas funcionales ejecutadas.

Resultado acumulado por caso único: **22 aprobados / 0 fallos pendientes**. Se conserva cada intento fallido y se toma la última evidencia de cada caso en campo-10d/evidencia/resultados-10d.json. Los 5 casos de sincronización y los 2 casos de campos/edición no se repitieron después de aprobarse. La restauración de navegación y vista previa se aprobó en final-xml; la transformación Camera2 comprueba 32 combinaciones dentro de un caso.

El caso UI del panel general de Pavimentos falló inicialmente por WorkManager ausente en su Application sintética. Tras inicializarlo en el montaje, se repitió solo ese caso y pasó (pavimentos-ui-corregido.xml). La app usa el inicializador de WorkManager y lo consulta al arrancar GboApplication. No se corrigió lógica de Pavimentos a partir de ese fallo.

Pavimentos: 5 casos de asociaciones, historial, persistencia/reapertura, migración y ACK más 1 caso UI. La prueba de ACK utiliza respuestas ficticias y el contenedor PDF simulado de 10C por limitación JVM/Windows; no aprueba PDF o Drive reales. Nombres V2: 3 casos de reserva estable, rechazo de identidad/categoría/longitud/duplicado/revisión y capacidad ausente. Video: 2 casos de alcance y espera de padre/capacidad, más 1 de tarjeta compartida; reproducción y captura reales pendientes.

El propietario informó «Drive: Sincronizado» en la ficha consultada. Es una observación suya sin UUID/revisiones; no es prueba remota realizada por esta ejecución ni identifica la causa exacta del caso anterior.
