# Matriz de validación Prompt 10

Los resultados de mocks/Robolectric se clasifican como locales, nunca como validación física de Drive. No se repiten pruebas que ya pasaron salvo cambio posterior directamente relacionado.

| Área | Evidencia | Estado |
|---|---|---|
| JPEG/PNG/WebP, contenedor final, EXIF, originales, corruptos | PhotoTransport10Test 6 + AutomaticSyncRegressionTest 3; bloque-a-corregido.log | Aprobado local |
| Recuperación NOT_JPEG, cola, ACK, sin GPS | AutomaticSyncWithoutGpsTest 10; bloque-a-corregido.log | Aprobado local |
| Video solo SCAP, historia/reintentos | VideoContinuation9Test 11 | Aprobado local |
| Puentes panel y FOTO/VIDEO horizontal/vertical, pavimentos fotos | BridgeMedia10ScreenTest 2 + PavementParallelCUiTest 2; interfaz-a.log | Aprobado local |
| Rechazo PNG, recuperación misma identidad/revisión, ACK/reintento | prueba-drive-real-ficticia.json | Aprobado Drive real, exclusivamente ficha ficticia |
| Archivo recibido: JPEG + SHA-256 + ausencia duplicado en reintento | Foto recibida y reporte de descarga independiente | Aprobado Drive real, ficha ficticia |
| Tres revisiones de tablet bloqueadas | 1791844103713, 1791844288555, 1791848119143 | Pendiente UUID, archivos y tablet; no declaradas recuperadas |
| Cámara física / selección URI / múltiples originales en tablet | PRUEBAS_TABLET_10 | Pendiente aceptación física |
| HEIC Android | PhotoFormats10InstrumentedTest + fixture propia photo10.heic | Preparado, no ejecutado; Android 9+ y soporte del dispositivo |
| Familias y bindings V4 | DriveFamilies2ATest 8; android-b.log | Aprobado local |
| Ediciones históricas, baseRevision/Excel remoto/fotos | SyncedRecordEdits2BTest 10; android-b.log | Aprobado local |
| Reserva nombres/persistencia/identidad/backend viejo | ReadablePhotoNames10Test 3 inicialmente + cuarta prueba de capacidad separada; cierre-focalizado.log | Aprobado local: cuatro pruebas de nombres y una de bloqueo, 5/5 al cierre |
| Catálogo, límite 20, reservas concurrentes, nombres históricos, staging/ACK/rollback/Excel/SCAP | Field10.test.cjs 7; backend-b-final-focalizado.log | Aprobado local |
| Regresiones backend directamente relacionadas | backend-b-corregido.log: 84/85; el único fallo restante de comparación VM resuelto, Field10 final 7/7 | 85 escenarios aprobados entre ejecuciones; no una sola corrida final de 85 |
| Anuncio V2 sin romper V1 | compatibilidad-b.log | 1/1 aprobado local |
| Token compartido no autoriza anular/restaurar | anulacion-c.log | 1/1 aprobado local, cero escrituras |
| Llamada directa al cambio de estado local | InventoryLifecycle10Test; cierre-focalizado.log | Aprobado local: cuatro pruebas de nombres y una de bloqueo, 5/5 al cierre |
| Anular segundo de tres, excluir fila, restaurar, offline, conflicto dos operadores, permisos revocados | ANULACION_CONTROLADA_10 | Bloqueado: falta autorización individual; función deshabilitada |
| Estructura REVISIONES existente | Auditoría de nombres/IDs de PE-3N, sin modificar ni descargar inventario | Comprobado solo lectura; contiene respaldos Excel/JSON |
| Migración y activación nombres V2 en producción | ESTRUCTURA_DRIVE_10 / CAMBIOS_APPS_SCRIPT_10 | Preparado, no ejecutado ni desplegado |
| APK Debug con configuración local real | apk-debug-10.json | BUILD SUCCESSFUL; APK y AndroidTest compiladas, sin ejecución física |
| Firma/versionCode/Release | 16 / 1.4.2; solo tareas Debug | Sin cambio; Release no generado |

Fallo inicial A: iconos de dependencia ausente; corregido con recursos vectoriales locales. Fallos iniciales B: contexto perdido al extraer asignador, expectativas históricas y comparación de arrays entre contextos VM; registros conservados. No se ocultan fallos ni se usa el mock como sustituto de la prueba real. Logs/XML bajo `campo-10/evidencia/`.
