# Diagnóstico de altas de usuarios

## Resultado y límites

La observación de que una persona no puede crear más usuarios mientras otra sí puede no demuestra por sí sola un límite de cuentas. El código publicado de GBO - Servicio de Usuarios no está incluido en el proyecto; no se atribuye al servidor una causa que no se pudo verificar. No se crean cuentas reales, modifican roles ni eliminan usuarios.

El login de usuario y PIN se conserva. ADMIN y OPERADOR no tienen las mismas acciones: Inicio ofrece Administración a ADMIN; las operaciones de AdminUsersRepository exigen requireAdmin. Crear usuarios requiere Internet y un token de autorización obtenido al introducir el PIN en Autorizar cambios online. Ese token vence como máximo a los cinco minutos y se olvida al salir de administración o pasar a segundo plano. ACCESS_CONTROL_ENABLED=false no elimina estas comprobaciones.

## Hallazgo verificable de interfaz

La pantalla colocaba Nuevo usuario, Sincronizar usuarios y Auditoría en una sola Row sin ajuste de línea. En anchuras pequeñas o con texto grande, el conjunto puede exceder el panel. Se sustituyó por FlowRow, al igual que el grupo de acciones de cada usuario. Los callbacks, roles y controles de autorización permanecen iguales. Esta corrección de accesibilidad no prueba que fuera la causa del incidente de alta comunicado.

## Contrato existente

- Nuevo usuario recoge username, displayName, role y un verificador PBKDF2 de PIN, no un alta local de emergencia.
- Usuario: normalización a minúsculas, entre 3 y 64 caracteres [a-z0-9._-]. Nombre visible: no vacío, hasta 120 caracteres. PIN numérico de 6 a 12 dígitos y confirmación coincidente.
- AdminUsersRepository llama createUser con adminToken vigente mediante HttpsUserService. El transporte agrega token de tablet y huella persistente hacia el /exec privado provisionado; no usa SERVER_BASE_URL como URL del servicio de usuarios.
- El servidor puede responder DUPLICATE_USERNAME, INVALID_REQUEST, ADMIN_REQUIRED, SESSION_EXPIRED, DEVICE_REVOKED, RATE_LIMITED u otros errores traducidos por el cliente. La unicidad y UUID nuevos del servidor no pueden auditarse sin ese código o una respuesta real autorizada.
- No hay límite de veinte altas en el repositorio Android. La lista de veinte dispositivos del proveedor de demostración corresponde a otro mecanismo y no limita usuarios.
- UserSyncRepository acepta snapshots de entre 1 y 1000 usuarios y al menos un ADMIN activo. Ese es un límite de sincronización del cliente, no prueba del límite de creación del backend. No se amplía sin un caso real y contrato acordado.
- Puede existir alta confirmada en el servicio seguida de error al actualizar la caché; el mensaje indica Cambio guardado en el servicio central y falta actualizar. En ese caso sincronizar, sin repetir el alta y provocar un duplicado.

## Evidencia mínima para el incidente real

Registrar versión, rol efectivo, orientación/ancho de pantalla, si se ve Nuevo usuario, si se pulsó Autorizar cambios online hace menos de cinco minutos, conectividad, cantidad aproximada de usuarios y mensaje exacto del intento. Omitir PIN, token, nombres personales y credenciales. Una captura del error anonimizada y esos datos bastan para decidir el siguiente diagnóstico, sin crear cuentas de producción adicionales.

Estado del incidente de límite o rechazo: **no reproducido con las fuentes disponibles**. Estado de accesibilidad del grupo de acciones: corregido y comprobado con el componente real a 260 dp; los tres botones quedan dentro del panel y mantienen sus callbacks. Un segundo caso verifica el rechazo a OPERADOR, la autorización online de ADMIN, la ausencia de un tope Android de veinte altas y el vencimiento del token. Ambos casos aprobaron con servicio ficticio aislado; no prueban el backend publicado. El manual conserva el proceso actual y no anuncia permisos nuevos.
