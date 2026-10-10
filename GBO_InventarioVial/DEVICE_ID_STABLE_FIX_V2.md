# Huella estable v2.1 — corrección de DEVICE_REVOKED falso

La primera corrección era demasiado estricta: solo migraba desde registros `legacy-random-v1` o sin `scheme`.
Eso podía devolver `DEVICE_REVOKED` si la misma tablet ya había pasado por una migración anterior,
si una variante APK/debug produjo otra huella estable, o si el registro previo ya estaba marcado `android-id-v2`.

## Nuevo comportamiento
- Coincidencia exacta activa + token correcto: acceso normal.
- Coincidencia exacta marcada `revokedExplicitly=true`: bloqueada, nunca se reactiva.
- Si la huella actual no coincide, el servidor busca EXACTAMENTE UN registro activo con el mismo token de 256 bits,
  independientemente del esquema anterior, y rebindea la identidad a la huella actual.
- El registro anterior queda inactivo y enlazado con `migratedTo`.
- Las revocaciones futuras quedan marcadas con `revokedExplicitly=true` y `revokedAt`.

## Verificación del despliegue
El GET del Apps Script de usuarios ahora devuelve:
`users-v2.1-stable-device-rebind`.

Después de desplegar Security.gs y Code.gs como nueva versión de la MISMA implementación de usuarios,
abre su URL `/exec` en navegador y verifica esa versión antes de probar la tablet.
