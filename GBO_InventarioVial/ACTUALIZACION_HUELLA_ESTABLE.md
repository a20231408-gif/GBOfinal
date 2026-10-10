# Actualización obligatoria — huella estable de tablet

Esta versión corrige el problema por el que una reinstalación podía producir `DEVICE_REVOKED`.

## Qué cambia en Android
La huella deja de usar `installation_uuid` aleatorio. Ahora es estable para la misma tablet/usuario Android/firma de aplicación:

`SHA-256("gbo-inventario-vial-device-v2:" + ANDROID_ID)`

## Compatibilidad con tablets ya registradas
El servicio central de usuarios incorpora migración automática de una sola vez. Si la nueva huella aún no existe pero el mismo token secreto pertenece exactamente a una tablet legacy activa:

1. desactiva la huella antigua;
2. crea la nueva identidad `android-id-v2` con el mismo token;
3. registra `TABLET_ID_MIGRATED`;
4. continúa la sincronización sin pedir un token nuevo.

Una tablet revocada explícitamente no se reactiva.

## PASO OBLIGATORIO antes de instalar la APK
Actualizar el **Apps Script del SERVICIO DE USUARIOS** (no el Code.gs de Drive) usando los archivos de `backend/user-service/`, como mínimo:

- `Security.gs`
- `Code.gs`

Después: **Implementar → Gestionar implementaciones → Editar → Nueva versión → Implementar**, conservando la misma URL `/exec` del servicio de usuarios.

Luego compilar/instalar la app sobre la existente. La primera sincronización migra automáticamente la identidad anterior si el token sigue siendo válido.
