# Corrección de identidad estable de tablet

## Problema corregido
La huella anterior era `SHA-256(ANDROID_ID + installation_uuid aleatorio)`. Si se borraban datos o se reinstalaba la app, `installation_uuid` cambiaba y el servicio central interpretaba la misma tablet como un dispositivo diferente, devolviendo `DEVICE_REVOKED`.

## Solución
La huella ahora es determinística:

`SHA-256("gbo-inventario-vial-device-v2:" + ANDROID_ID)`

Ya no depende de SharedPreferences ni de UUID aleatorios.

## Compatibilidad con tablets ya provisionadas
El backend de usuarios incluye una migración automática de una sola vez: si la nueva huella no existe pero el token de tablet coincide con exactamente una tablet legacy activa, transfiere el vínculo a `android-id-v2`, desactiva la huella antigua y conserva el mismo token.

Una tablet explícitamente revocada no se reactiva automáticamente.

## Importante
Para que la migración funcione, actualizar también los archivos del Apps Script de `backend/user-service` en la implementación del servicio de usuarios y publicar una nueva versión sobre la misma URL `/exec`.
