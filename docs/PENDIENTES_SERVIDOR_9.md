# Cambios del servidor preparados para Prompt 9B

Estado: parche local preparado; **sin despliegue ni aceptación remota**. No editar ni eliminar las asociaciones existentes de `._GBO_SYNC_V3`.

## Archivos

Actualizar en un despliegue de prueba `Code.gs` y `SyncV3Core.js` (como `.gs`); conservar `SyncV3.gs` actual. Agregar `VideoV1Core.js` como `VideoV1Core.gs` y `VideoV1.gs`. El parche publicado inicialmente está en `3ee22ec`. Mantener ROOT_FOLDER_ID/token reales en la configuración del propietario; no copiar placeholders sobre valores de producción. No imprimir tokens ni URI de sesiones de carga.

El `doPost` existente autentica el token antes de enrutar `beginVideoV1` y `finishVideoV1`. Los archivos nuevos reutilizan el bloqueo ScriptLock y el servicio avanzado Drive v3. Se necesita autorización de Drive y UrlFetch para iniciar sesiones, además del OAuth del propietario. Confirmar autorización y scopes en el entorno de prueba antes de activar.

## Fotos numeradas

El GET anuncia `photoNamingVersion=1`; el nuevo core valida categorías y hash del UUID, acepta prefijos de 3…10 dígitos y el nombre legado. Integra el perfil en el contentHash. Rechaza cambiar el nombre de una foto previamente vinculada; los fileId se conservan en ediciones/reintentos. No hay tarea de renombrado masivo.

Después de probar, Android puede habilitar `GBO_PHOTO_NUMBERS_ENABLED=true` en `server.local.properties` para una compilación configurada. Por defecto false. Los snapshots ya preparados conservan su contenido exacto; no se rehacen silenciosamente para aplicar otro perfil.

El prefijo se reserva localmente por elemento y UUID. Distintos operadores pueden usar el mismo prefijo con distinto hash; no colisionan. Un contador remoto global o etiquetas de una base externa requerirían un nuevo contrato/migración explícitos, no implementados ni ejecutados.

## Videos

1. Desplegar los cuatro archivos anteriores en un servidor de prueba con Drive v3 avanzado.
2. Mantener `GBO_VIDEO_V1_ENABLED` ausente/false inicialmente. El GET debe anunciar `videoProtocol=0`; Android muestra dependencia del backend.
3. Activar la propiedad en prueba; GET debe anunciar 1. Activar Android `GBO_VIDEO_REMOTE_ENABLED=true` únicamente para la aceptación coordinada. La APK Debug de validación mantiene false incluso si hay propiedades locales.
4. Finalizar y sincronizar una ficha de prueba. `beginVideoV1` recibe parentKey/revisión/UUID/categoría/MIME/tamaño/SHA, reserva fileId en `._GBO_SYNC_V3/VIDEO_V1` y crea `VIDEOS/<categoría>/V_<hashUUID>.<ext>` dentro de la carpeta ya vinculada.
5. Android consulta offset y transmite bloques binarios directamente a una URI HTTPS de `www.googleapis.com/upload/drive/v3/files…`, sin exponer OAuth del propietario y sin Base64 del video. La URI es un secreto temporal que se conserva solo en almacenamiento privado.
6. `finishVideoV1` compara propietario, carpeta, MIME, tamaño y `sha256Checksum` devuelto por Drive. Si Drive no proporciona el checksum, rechaza la confirmación; el cliente conserva original y error, sin afirmar sincronización. No descarga un blob gigante en Apps Script.
7. Probar pérdida de respuesta inicial/final, corte a mitad de bloque, reinicio de app, caducidad/404, dos videos de una categoría, varias categorías, UUID reutilizado con contenido distinto y ficha aún no finalizada. Verificar un único fileId por UUID.

El retiro es local y conserva originales/remotos; no elimina evidencia remota. No existe renumeración/movimiento de carpetas por cambios del orden visual. Una migración de numeración Drive debe diseñarse y aprobarse por separado.

## Límites revisados

El staging fotográfico actual admite 24 MiB por archivo y hasta 160 MiB por revisión. No se reutiliza esa vía Base64 para videos. El límite de producto se fija en 32 MiB por video y bloques de 512 KiB. No es el máximo de Drive: limita captura/almacenamiento y facilita la aceptación inicial. El número de videos queda sujeto al espacio local/Drive disponible.

Google documenta [sesiones reanudables, bloques múltiplos de 256 KiB, respuesta 308 y caducidad de una semana](https://developers.google.com/workspace/drive/api/guides/manage-uploads). Apps Script documenta [6 minutos por ejecución y 50 MB por llamada UrlFetch](https://developers.google.com/apps-script/guides/services/quotas). Drive expone [SHA-256 de archivos binarios, si está disponible](https://developers.google.com/workspace/drive/api/reference/rest/v3/files). El parche renueva sesiones después de seis días o cuando el cliente informa caducidad, siempre sobre el ID reservado.

## Pruebas realmente realizadas

Pruebas Node del motor y adapter Apps Script con servicios simulados, junto con regresiones de categorías/fotos/Excel/ediciones. Pruebas JVM del cliente: persistencia, límites, reanudación, idempotencia, ACK falso, original modificado, retiro y flag deshabilitado. No constituyen aceptación OAuth/Drive remota. Las cuotas diarias y permisos de la cuenta del propietario deben probarse antes de habilitar producción.
