# Dependencias backend 10D

Sincronización V3: la recuperación usa stageRecordV3 y commitRecordV3 existentes y recibos idempotentes; no requiere publicar otro endpoint. La existencia de archivos no confirma revisión. Se preservan las validaciones de archivos, hashes, UUID, carpeta, ruta, revisión y agregado Excel.

Según información del propietario, GBO_Inventario anuncia syncProtocol=3, pathVersion=4, photoCategoryVersion=1, photoNamingVersion=1, readablePhotoNamingVersion=0, technicalRevisionStorage=1, videoProtocol=0, inventoryLifecycleVersion=0, videoScope=SCAP_ONLY. Esto no es una consulta nueva al servicio publicado.

## Nombres V2 y video

La reserva de nombre representativo exige readablePhotoNamingVersion=2 y el flag local explícito. Mantener GBO_PHOTO_NAMES_V2_ENABLED=false en la APK ordinaria y no cambiar bindings/fotos históricos. La activación de la capacidad publicada corresponde al propietario; comprobar una ficha ficticia antes de habilitar en campo.

Video exige videoProtocol=1 y padre SCAP confirmado. Mantener GBO_VIDEO_REMOTE_ENABLED=false y alcance SCAP_ONLY. No activar carga desde otros SIC o Pavimentos.

## Anulación

Se conservan ANULACION_CONTROLADA_10, PENDIENTES_BACKEND_10C e InventoryAuthorization10C.js. La propuesta de contrato no acredita integración con el servicio publicado. Token compartido Drive, ADMIN o TABLET_ADMIN no autorizan anulación contractual.

Dependencia exacta: código fuente efectivo y revisión desplegada de GBO - Servicio de Usuarios, su router, verificador PIN/rate limit, usuarios/tablets activos, almacén de permisos explícitos y adaptador seguro de concesiones. Sin esa fuente no se inventa autoridad ni se habilita InventoryLifecyclePolicy.

Después de obtenerla: integrar InventoryAuthorization10C.gs en Servicio de Usuarios; completar google-apps-script/Code.gs, SyncV3.gs, SyncV3Core.js (exportado como .gs al servicio) y un motor lifecycle con journal/rollback/ACK, tombstone/restauración, consolidado Excel de todos los operadores (incluido cero activos), feed incremental, conflictos de revisión y numeración visible posterior al ACK. Android necesita intención offline durable, motivo, concesión individual y cliente de feed. UUID, folderId y bindings históricos permanecen estables.

No se desplegó ni modificó Apps Script publicado, ni se cambiaron permisos o credenciales reales. La anulación remota sigue pendiente y deshabilitada; el descarte reversible local de 10C se conserva.

Consulta de lectura al conector Google Drive (13/10/2026): búsqueda «GBO Servicio de Usuarios» sin resultados; «GBO Usuarios» encontró únicamente la hoja «GBO - Usuarios Inventario Vial». No se leyó su tabla de usuarios/PIN ni se obtuvo la fuente del proyecto Apps Script o la revisión desplegada. La hoja no acredita el router ni la autoridad requerida.
