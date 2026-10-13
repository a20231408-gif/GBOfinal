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
| Restauración de navegación y formulario incompleto sin nueva ficha/revisión | FormRotation10DTest | En ejecución local |
| Edición de alcantarilla restaura texto y conserva datos finales históricos/Excel | FormRotation10DTest | Aprobado local (Robolectric; datos ficticios) |
| Vista previa restaurada, EXIF y original intacto | FormRotation10DTest | En ejecución local |
| Fotos por daño de Pavimentos, panel común y offline | Pruebas focalizadas 10C | Pendiente verificación 10D |
| Room 18→19 mantiene fotos/UUID/cierre históricos | Prueba focalizada 10C | Pendiente verificación 10D |
| Cliente V2 y ausencia de capacidad: reserva y ACK seguros | ReadablePhotoNames10Test | Pendiente verificación 10D |
| Video solo SCAP: tarjeta compartida y capacidad remota desactivada | BridgeMedia10ScreenTest | Pendiente verificación 10D |
| Huawei MatePad 12 X LRT-W09 / HarmonyOS 4.3.0: giro, cámara externa e interna, galería, entrega Drive | PRUEBAS_TABLET_10D | Pendiente físico |
| Anulación/restauración/Excel y autorización real | PENDIENTES_BACKEND_10D | Deshabilitado; no aprobado |
| Debug con servicios locales privados y firma existente | Evidencia APK | Pendiente compilación final |

Los intentos de compilación fallidos y la interrupción por RAM se guardan como evidencia. No se declaran como pruebas funcionales ejecutadas.
