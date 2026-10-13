# Dependencias backend 10C — no desplegadas

## Pavimentos y borradores locales

No requieren desplegar Apps Script. La corrección utiliza el mismo protocolo de entrega JPEG, staging, UUID, revisión, baseRevision y ACK del Prompt 10. Los originales nuevos/importados quedan locales; la copia final pasa por DeliveryJpeg. El descarte local no comunica ningún comando remoto.

## Autoridad individual preparada en repositorio

Archivo **google-apps-script/InventoryAuthorization10C.js**: motor de contrato propuesto para cargar como InventoryAuthorization10C.gs en GBO - Servicio de Usuarios, después de integrar y revisar su fuente existente. **No copiar como función activa al proyecto de Drive ni asumir que el archivo por sí solo habilita anulación.** InventoryAuthorization10C.test.cjs demuestra siete escenarios con almacén de autoridad simulado, no autenticación del servicio real.

Las nuevas acciones propuestas del servicio son authenticateInventoryV1 e introspectInventoryV1. Primera: tabletToken/huella y userId/PIN, destino kind/recordId/routeCode. El servicio debe verificar tablet y usuario activos y PIN con su algoritmo/rate limit existentes, sin registrar el PIN. Se requiere concesión explícita de inventoryScopes (INVENTORY_ANNUL/RESTORE) e inventoryRoutes en fuente autoritativa. ADMIN o TABLET_ADMIN no equivale a ese permiso. No asignar automáticamente a todos los administradores.

Contrato de adaptador requerido: verifyTablet, verifyPin, secureToken, hash, putGrant, getGrant, audit, verifyBackend, tabletByFingerprint, user, now. Todos leen la autoridad del servidor; no toman rol/permisos desde Android. secureToken debe usar el generador criptográfico existente, 32 bytes / base64url 43 caracteres; guardar solo SHA-256 del token, no el secreto. Expira a 10 minutos y queda ligado a una ficha/ruta/tablet/usuario. Falta el adaptador concreto porque la fuente del servicio publicado no está incluida en el repositorio.

Introspección únicamente servidor-a-servidor: credencial de backend separada del token Drive, en propiedades privadas de ambos proyectos. Android solo tiene la concesión individual y nunca esa credencial interna. El backend llama a una URL de usuarios fija/configurada privadamente, nunca a una URL suministrada en el payload. Verifica que la respuesta de introspección coincide en opId/operación/UUID/ruta/tablet/usuario/baseRevision; volver a verificar revocación y permiso en cada intento. Rechazo o servicio caído conserva cola y no escribe inventario. La respuesta de autorización **no es un ACK de anulación**.

Actualmente ningún doGet anuncia inventoryAuthorizationVersion=1/inventoryLifecycleVersion=1, ni existe ruta Android de activación. Antes de integrar, revisar fuente real y migración de permisos de usuarios con el propietario. Solo preparar cambios en repositorio está autorizado aquí; no desplegar ni cambiar permisos reales.

## Operación de inventario todavía pendiente

Diseño compatible con ANULACION_CONTROLADA_10:

1. Android pide confirmación y motivo obligatorio, guarda intención durable con opId, kind/UUID, baseRevision, usuario/tablet/fecha y estado pendiente. El permiso individual se comprueba por servidor al procesarla; la ausencia/revocación no se sustituye por token Drive. No dar por enviado al guardar offline.
2. Backend bajo ScriptLock autentica e inspecciona la revisión vigente. OpId persistente devuelve el mismo resultado en reintento; si una operación previa quedó pendiente, recupera el journal antes de permitir otra. Validar concesión antes de cualquier modificación.
3. Preparar respaldo por IDs de registro, binding, agregado y Excel actual. Tombstone reversible con motivo/auditoría y revisión monotónica; ignorar el registro anulado únicamente en inventario activo. Prohibir commit de una edición anterior que restaure implícitamente el tombstone.
4. Consolidar desde todos los registros remotos de esa ruta/familia; preservar filas ajenas, encabezados/columnas/OOXML/estilos. Excluir exactamente el UUID afectado y sus filas contractuales. Con cero activos, generar plantilla con cabeceras; resolver EMPTY_AGGREGATE actual antes de activar. Sin datos autoritativos suficientes, abortar en vez de sobrescribir con Excel de una tablet.
5. Commit transaccional registro/agregado/Excel + journal y ACK de lifecycle. Fallo parcial: rollback por ID, hashes y revisión de respaldo; evitar sobreescribir una revisión concurrente. Restaurar es otra operación autorizada, con baseRevision vigente y conservación de historial.
6. Tras ACK, recalcular número **visible** activo 1,2, sin cambiar UUID, binding, número histórico ni carpetas Drive. Mantener orden KMZ/PR actual. Las otras tablets necesitan feed incremental con tombstones/ACK y consulta al sincronizar; no existe actualización global instantánea hoy.

Pruebas faltantes: segundo de tres y las otras dos filas intactas, cero activos, restaurar, cierre offline, ACK perdido, conflicto dos operadores, edición antigua vs anulación, administrador con permiso y sin permiso, tablet revocada, PIN/token incorrectos, preservación OOXML, UUID y folderId estables, numeración después del ACK. Las siete pruebas del contrato de autorización no sustituyen estos escenarios.

Archivos que necesitarán integración futura: fuente real del Servicio de Usuarios (router/capacidades/store de permisos y concesiones), nuevo InventoryAuthorization10C.gs; en GBO_Inventario, Code.gs, SyncV3.gs y SyncV3Core.gs más motor lifecycle y pruebas transaccionales. Preservar ROOT_FOLDER_ID, API_TOKEN, URL publicada, permisos existentes y almacenamiento técnico del Prompt 10. Android necesita cola/cliente de concesión/feed y UI de motivo antes de habilitar InventoryLifecyclePolicy.

**Estado final: anulación/restauración remota y actualización Excel no terminadas y deshabilitadas.** No se ejecutan movimientos, borrados ni renumeraciones remotas en esta entrega.
