# Auditoría real de sesiones y ciclo de vida

Se revisaron AuthGraph, AuthSessionManager, AuthRepository, AdminUsersRepository, TabletAdminRepository, pantallas administrativas, MainActivity y el código de cámara. Búsqueda de finishAffinity, finishAndRemoveTask, killProcess, exitProcess, System.exit, Handler/postDelayed, logout y servicios/locks.

| Sesión | Regla | Resultado |
| --- | --- | --- |
| Usuario de trabajo | 15 minutos **en background**, evaluados con elapsedRealtime al volver | `sessions.lock()`, mensaje «Tu sesión venció. Inicia sesión nuevamente.», diálogo de PIN sobre contenido retenido |
| Usuario trabajando en foreground | Sin timeout por uso continuo | Puede seguir registrando |
| Administración de usuarios | 300 s, anterior | Acción requiere nuevo PIN; no mata la app |
| TABLET_ADMIN | 900 s, servidor + reloj monotónico Android | Cierra solo panel, conserva sesión normal y muestra «Sesión de administración vencida.» |
| Salida a background | Descarta ambos tokens administrativos en RAM | Reautorización al volver al panel; no borra credenciales propias cifradas |
| Usuario desactivado/tablet revocada | Bloqueo central y marca closing en caché | Se conserva trabajo abierto; salida normal al llegar a inicio y terminar recorrido |

No se encontró código que mate proceso/MainActivity por timeout. Los `finish()` hallados están en FieldCameraActivity al cancelar o terminar captura: devuelven resultado y liberan cámara, no cierran la aplicación completa. HandlerThread de cámara se cierra en onDestroy; no hay temporizador que mate aplicación.

Los borradores existentes de SIC se mantienen en InventoryDraftStore con protección del usuario y referencias a archivos; SCAP persiste cada cambio crudo mediante canal del controlador/Room, y Guardar borrador vacía el trabajo enviado. Pavimentos conserva su almacenamiento local. Este cambio no modifica modelos Room, archivos de fotos ni el control de usuario propietario. Cambios todavía pendientes en el canal antes de un kill instantáneo no pueden garantizarse; la UI muestra Guardando/Guardado/Error y reintento. No se agregó auto-reinicio, wake lock, exclusión de batería ni servicio de foreground para ocultar fallos.

Un bloqueo de sesión es distinto de salida del proceso. No hay evidencia física de timeout que provoque el cierre reportado; debe correlacionarse el próximo evento real con diagnóstico, hora y módulo, según checklist. No se atribuye una causa sin ApplicationExitInfo/exception/ANR real.
