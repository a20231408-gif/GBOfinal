"""Generate reviewable reports from the real baseline and final evidence, without accessing secrets."""
from pathlib import Path
import json,hashlib
R=Path(__file__).resolve().parents[1];D=R/'docs';E=D/'tablet-admin-stability-scap-evidence'
def write(name,text): (D/name).write_text(text.strip()+'\n',encoding='utf-8')
def sha(p):return hashlib.sha256(p.read_bytes()).hexdigest()
baseline=json.loads((E/'baseline.json').read_text(encoding='utf-8'))
final=json.loads((E/'final-tests.json').read_text(encoding='utf-8')) if (E/'final-tests.json').exists() else None
before=json.loads((E/'source-before.json').read_text(encoding='utf-8'))
changes=[name for name,value in before.items() if (R/name).is_file() and sha(R/name)!=value]
new=[str(p.relative_to(R)).replace('\\','/') for folder in ['app/src','backend'] for p in (R/folder).rglob('*') if p.is_file() and str(p.relative_to(R)).replace('\\','/') not in before]
schema_changed=[name for name in changes if name.startswith('app/schemas/')]
assert not schema_changed, schema_changed
failures='\n'.join('- `'+name+'`' for name,state in baseline['cases'].items() if state=='FAILED')
test_summary=f"Baseline real: {baseline['total']} pruebas; {baseline['passed']} PASS y {baseline['failed']} FAIL."
if final:
    test_summary+=f" Última ejecución registrada: {final['total']} pruebas; {final['passed']} PASS y {final['failed']} FAIL; nuevos fallos: {len(final['newFailures'])}; identidades anteriores ausentes: {len(final['missingBaselineCases'])}."
    if final['newFailures']:test_summary+=' VALIDACIÓN EN CURSO: todavía no es una entrega aprobada.'
build=(E/'assemble-debug.log').read_text(encoding='utf-8',errors='replace') if (E/'assemble-debug.log').exists() else ''
build_result='BUILD SUCCESSFUL' if 'BUILD SUCCESSFUL' in build else 'PENDIENTE: todavía no hay evidencia de assembleDebug final.'
identity=json.loads((E/'debug-identity.json').read_text(encoding='utf-8-sig')) if (E/'debug-identity.json').exists() else []
if isinstance(identity,dict):identity=[identity]
apk_info='Identidad APK pendiente.'
verification=json.loads((E/'artifact-verification.json').read_text(encoding='utf-8')) if (E/'artifact-verification.json').exists() else {}
security_info='Verificación de secretos pendiente.'
if verification.get('knownLocalSecretsAbsent') and verification.get('serverMasterKeyAbsentFromDex'):
    security_info='Verificado: APK sin referencia a SERVER_RANDOM_KEY en DEX, sin código Apps Script/provisioning Owner, endpoints privados de BuildConfig vacíos y sin valores de credenciales operativas disponibles localmente. ZIP excluye configuraciones privadas/keystores y compara sus contenidos contra esos valores sin imprimirlos. Las claves del servidor que no existen en este PC no se descargaron ni inventaron.'
if identity:
    i=identity[0];apk_info=f"APK: {i['filename']}; {i['bytes']} bytes; SHA-256 `{i['apkSha256']}`; certificado debug `{i['certificateSha256'][0]}`; versionCode {i['versionCode']}."
write('TABLET_ADMIN_IMPLEMENTATION_REPORT.md',f'''
# TABLET_ADMIN · estabilidad · SCAP — reporte de esta iteración

1. **Base exacta:** checkout recibido `D:/Downloads/GBO_InventarioVial_INTEGRADO_FINAL_SOURCE/GBO_InventarioVial`, versión más reciente de paridad SIC v13. Snapshot de los 587 archivos recibidos y SHA-256 en `tablet-admin-stability-scap-evidence/source-before.json`. No se volvió a un APK/ZIP anterior ni a otra rama. No existe repositorio Git en este checkout.
2. **versionCode:** 13 → 14; `versionName=1.4.0-sic-production` conservado. El DEBUG v13 entregado antes era SHA-256 `ac03782a8267176119c9425c8b21934f6b2687f7ea374286b324233849300b03`. El máximo conocido era 13; no se reutiliza.
3. **Room:** 15 → 15. Todos los archivos `app/schemas` conservan el SHA-256 recibido.
4. **Migración nueva:** no. Se reutiliza `scap_values` para estado, nombres, códigos, descripción global, fechas y metadata de entrega. `TABLET_ADMIN` es una capacidad explícita en servidor; Android conserva únicamente una pista local para mostrar el botón.
5. **TABLET_ADMIN:** PIN del usuario logueado, tablet válida, usuario activo y permiso vivo en servidor. Sesión exclusiva `TABLET_ADMIN_ONLY`, 900 segundos, token en RAM en Android y hash en servidor. Cada operación verifica nuevamente identidad, vigencia, scope, revisión del usuario y permiso. No concede administración de usuarios ni acceso a propiedades del script. Alta/revocación/reactivación/rotación son centrales, bajo `LockService`; self-revoke bloqueado tanto en UI como servidor. Alias ajeno a autenticación; lista con campos permitidos sin hashes. Historial visible con huella abreviada. Se conservan hashes al revocar y reactivar. Limitación 5 intentos de autorización/5 minutos por tablet y 60 operaciones/minuto por sesión de tablet, separada del login normal.
6. **User Service:** `users-v2.4-tablet-admin`; archivos modificados `Code.gs`, `Users.gs`; nuevo `TabletAdmin.gs`, tests y README actualizados. Acciones: `authenticateTabletAdmin`, `listTablets`, `provisionTabletAdmin`, `revokeTabletAdmin`, `reactivateTabletAdmin`, `rotateTabletToken`, `getTabletAudit`. Owner ejecuta `applyTabletAdminPermission()` mediante propiedad temporal con `userId/enabled/reason`; no está en doPost. **Owner debe actualizar manualmente el deployment existente** siguiendo `TABLET_ADMIN_SERVER_DEPLOY.md`. No se desplegó desde esta tarea. Compatibilidad de acciones anteriores y `TABLET_<deviceId>` conservada. El suministro del nuevo token a la tablet destino se verifica online con la misma URL y huella antes de reemplazar su credencial cifrada; no hay alta automática.
7. **Seguridad de entrega:** {security_info} La evidencia está en `tablet-admin-stability-scap-evidence/artifact-verification.json`. Los tokens generados por alta/rotación solo aparecen una vez en memoria del panel; se descartan al cerrarlo. Las credenciales propias de la tablet continúan protegidas por Keystore. No se generó keystore ni se cambió applicationId, signingConfig o SHA esperado de release. {apk_info}
8. **Estabilidad:** sesión normal se bloquea después de 15 minutos en background, conserva la ficha y pide el PIN; no hay timeout por uso continuo. Admin usuarios conserva 5 minutos, TABLET_ADMIN 15 minutos. Ninguna expiración mata proceso/Activity. No hay evidencia de causa de cierres en tablet: falta prueba física y última salida real. Se agregan `ApplicationExitInfo` cuando está disponible y fallback local sanitizado, diagnóstico JSON acotado a 64 eventos. Cache de señales/miniaturas acotada, reducción de fotos Excel y transferencia por flujo de JPEG/Excel. Mapa usa miniatura limitada. Detalle en `SESSION_LIFECYCLE_AUDIT.md` y `APP_EXIT_DIAGNOSTIC_REPORT.md`.
9. **SCAP:** fuente física original: LOSA 17 elementos F.1, MODULAR 10. F.1 agrega descripción global y nombres/códigos editables sin perder identidad interna; UNSET/PRESENT/NOT_APPLICABLE. G.1 proyecta la misma colección exceptuando No aplica; cantidades/calificaciones vacías no toman ejemplos. Manuales y No aplica/reactivar preservan datos. Apoyos 1 y 2 iniciales, LEFT/RIGHT, apoyo 3+ y continuación exportada. F.2 es únicamente panel fotográfico en las fichas LOSA/MODULAR: 31 nombres únicos de los 32 ejemplos de cada XLSX, nombre/desc/fecha editables, cámara/importar/reemplazar/preview/remover/reordenar. El flujo fotográfico histórico queda compatible para fichas sin tipo canónico.
10. **Pendientes clicables:** `ScapFieldRoute(section,subsection,repeatableItemId,fieldKey)` y UUID estable; cambio de sección, posición de lista, `BringIntoViewRequester`, foco en texto y resaltado 1.8 s. Rutas emitidas junto al campo, sin extraer destinos de mensajes. Nuevos tests con botones reales del editor para A, apoyo 2, F.1, F.2 y G.1; capturas bajo `tablet-admin-stability-scap-evidence/screenshots`.
11. **Export incompleto:** warnings MISSING_VALUE y confirmación; entradas faltantes vacías. INVALID_VALUE que impediría datos válidos bloquea entrega, pero conserva texto crudo en borrador. Null se interpreta como cero exclusivamente al calcular; Excel conserva null. Suma distinta de 100% es warning, sin resultado global inventado. La fuente mantiene sus fórmulas y referencias #REF! originales. Continuaciones amplían el área de impresión del libro generado, conservando los rangos anteriores y todos los demás defined names; fuentes XLSX intactas.
12. **Drive incompleto:** confirmación requerida; metadata `completion.status=INCOMPLETE` y `uploadedWithWarnings=true`, separada de syncStatus. DRAFT/IN_PROGRESS pueden enviarse con revisión explícita, no se congelan al ACK. Se conserva UUID, fileId/folderId, fotos y versiones. Entrega con ubicación completa conserva v2; respaldo v1 existente para ubicación incompleta, destino mostrado explícitamente, sin inventar ruta. Ningún contrato remoto se cambió/desplegó. DEBUG validation carece de endpoint/token, por lo que la subida remota real queda en checklist físico de entorno configurado.
13. **Tests:** {test_summary} User Service: baseline 21 escenarios PASS, final 41 escenarios PASS (runner Node 1 archivo, 0 FAIL), `tablet-admin-tests.log`. Cambios deliberados de expectativas SCAP documentados abajo; no se quitaron identidades de tests. Logs reales baseline y final, JSON por identidad y evidencia de plantilla conservados.
14. **assembleDebug:** `{build_result}`, comando `gradlew.bat assembleDebug --no-configuration-cache --rerun-tasks -PgboValidationBuild=true` con JDK existente de Android Studio.
15. **Limitaciones:** aún se requiere deployment Owner, validación 30–60 minutos/actualización in-place en la tablet real y revisión Excel de las inconsistencias originales. DEBUG solo validación, no reemplaza release corporativa. La keystore release histórica no está disponible; no se creó otra. Límites físicos conservados para 31 filas G.1, 32 registros F.2, dos tramos, tres pilares, una junta, 15 puntos de perfil y un croquis por tipo; no se silencian excedentes. No commit, push ni despliegue automático.

## Identidades FAIL anteriores

{failures}

## Cambios deliberados de tests

Se mantienen nombres/identidades de los tests. Expectativas actualizadas: defaults de F.1 frente a filas de ejemplo G.1 (incluye mapeo MODULAR 510/504), fichas con 17/10 elementos frente a `.single()`, calificaciones/missing como warnings, null calculable como 0, continuación F.1 frente a rechazo de grupo y revisión/revisión temporal explícita antes del ACK. Siguen comprobándose límites técnicos, datos malformados, tipo incompatible, repetición de bytes ZIP, propiedad de fotos y ACK antiguo. La prueba existente de fotos de fichas históricas mantiene su contrato.

## Archivos fuente cambiados / nuevos

'''+ '\n'.join('- `'+name+'`' for name in changes+new))
write('TABLET_ADMIN_SERVER_DEPLOY.md','''
# Actualización manual Owner del User Service

El usuario no autorizó despliegue automático: esta entrega contiene código revisable; el servicio remoto sigue sin modificaciones hechas por Codex.

1. Owner abre **el proyecto existente del User Service**, separado del script de Drive. Respalda versión de Apps Script, Sheet privada y propiedades mediante sus medios seguros. No reinicia Users/Meta/Audit ni bootstrap.
2. Copia los archivos completos `Code.gs`, `Users.gs`, `Security.gs`, `Auth.gs`, `Audit.gs`, `Crypto.gs` y nuevo `TabletAdmin.gs` de `backend/user-service`. Mantiene la API Sheets avanzada y el manifiesto existente incluido. `SERVER_RANDOM_KEY`, `USERS_SPREADSHEET_ID`, hashes `TABLET_<deviceId>`, sesiones y claves existentes permanecen en el servidor; no sustituir la clave.
3. Revisa y actualiza **el deployment web existente** a una nueva versión, manteniendo URL HTTPS `/exec`, propietario y acceso requerido por el contrato anterior. GET debe devolver `users-v2.4-tablet-admin`. Prueba primero en entorno controlado con huellas reales autorizadas.
4. Obtiene el UUID de un usuario activo desde la hoja privada. Añade Script Property temporal `TABLET_ADMIN_PERMISSION_REQUEST` con JSON `{"userId":"UUID_EXISTENTE","enabled":true,"reason":"Responsable tablets campo"}`. Ejecuta **applyTabletAdminPermission** desde el editor Owner. Comprueba auditoría y eliminación de la propiedad temporal. ADMIN por sí solo no concede esta capacidad.
5. Sincroniza usuarios en la tablet administradora. `tabletAdminUserIds` viaja también con `changed=false`, por lo que no se necesita editar el usuario ni incrementar Meta solo para reflejar el permiso. El botón es una pista; la siguiente operación siempre consulta permiso vivo del servidor.
6. Usuario logueado abre ADMINISTRACIÓN DE TABLETS, reingresa PIN, pega la huella de otra tablet (64 hex) y alias. Alta genera token mediante HMAC y clave de 256 bits existente; servidor guarda exclusivamente hash. El token se muestra una sola vez; copiarlo para configurar **la tablet destino**, sin pegarlo en tickets/logs/JSON de auditoría.
7. Para retirar capacidad, Owner usa la misma propiedad con `enabled=false` y razón, ejecuta la función y comprueba evento `TABLET_ADMIN_PERMISSION_REVOKED`. Las sesiones del usuario se invalidan; todas las operaciones revisan además permiso vivo. No existe acción HTTP que conceda o retire permisos.
8. Revocación conserva hash/historial y elimina sesiones del destino. Reactivación conserva token, elimina revocación explícita. Rotación conserva identidad/alias, cambia hash e invalida ambos tipos de sesión. En el login de la tablet destino: **Actualizar token autorizado de esta tablet** → nuevo token → Verificar y actualizar. El servidor prueba el nuevo token con la misma huella y URL; solo entonces se cifra/persiste. Token erróneo o tablet revocada no reemplazan la credencial. No borrar datos ni desinstalar.
9. Ejecuta los 18 pasos administrativos del checklist; no considerar probado el servidor publicado solo porque los mocks Node pasan.

Acciones nuevas de doPost: authenticateTabletAdmin(userId,pin), listTablets(tabletAdminToken), provisionTabletAdmin(tabletAdminToken,targetDeviceId,alias), revokeTabletAdmin/reactivateTabletAdmin/rotateTabletToken(tabletAdminToken,targetDeviceId), getTabletAudit(tabletAdminToken,targetDeviceId). Todas reciben tabletToken + deviceId del cliente. JSON con campos desconocidos se rechaza. USER ADMIN y TABLET ADMIN no comparten token.

Auditoría central usa las 5 columnas existentes. Para eventos de tablet, targetUserId contiene alias; la UI muestra fecha, actor, acción, alias y huella abreviada. Permisos Owner usan targetUserId como UUID del usuario y device vacío. Eventos: TABLET_PROVISIONED/REVOKED/REACTIVATED/TOKEN_ROTATED, TABLET_ADMIN_PERMISSION_GRANTED/REVOKED, TABLET_ADMIN_AUTH_SUCCESS/FAILURE. Nunca tokens/hashes/PIN.

Rollback manual: volver el deployment a su versión respaldada y conservar propiedades/Sheet. El APK mantiene login/caché y API anterior; las acciones TABLET_ADMIN no funcionarán hasta instalar backend nuevo. La pista de permiso no sustituye autorización remota.

Se conserva además el control local previo de DeviceAccessManager. Si una build existente habilita ACCESS_CONTROL_ENABLED, mantiene su proveedor/lista autorizada y cache firmada: el alta central de TABLET_ADMIN no evita ese control ni modifica allowed_devices.json. La build de validación conserva las opciones de validación recibidas; todas las operaciones privilegiadas siguen requiriendo tabletToken válido y autorización remota viva.
''')
write('SESSION_LIFECYCLE_AUDIT.md','''
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
''')
write('APP_EXIT_DIAGNOSTIC_REPORT.md','''
# Diagnóstico y memoria

No se dispone de historial de salida de una tablet física en esta tarea. No se afirma que el problema observado fuera CRASH, ANR, LOW_MEMORY o timeout. El reporte pasa a registrar evidencia para la próxima reproducción.

En Android API 30+ se consulta **una** salida reciente de este paquete mediante ActivityManager.getHistoricalProcessExitReasons. Se conservan timestamp, reasonCode, reason y PSS/RSS **muestreados**. Se clasifican CRASH (Java/native), ANR, LOW_MEMORY, EXCESSIVE_RESOURCE_USAGE, USER_REQUESTED, USER_STOPPED, SIGNALED, OTHER; UNKNOWN indica «Causa no disponible.». USER_REQUESTED y USER_STOPPED se distinguen. No se leen descripción arbitraria del sistema ni trace de proceso. Referencias oficiales: [ApplicationExitInfo](https://developer.android.com/reference/android/app/ApplicationExitInfo) y [ActivityManager](https://developer.android.com/reference/android/app/ActivityManager#getHistoricalProcessExitReasons(java.lang.String,%20int,%20int)).

Android anterior o historial ausente: timestamp/version/trim/uncaught locales. Una uncaught real permite indicar CRASH con source=LOCAL_UNCAUGHT_HANDLER; ausencia de ese evento conserva causa desconocida. Handler registra y **delega al handler original**; no absorbe ni reinicia el proceso. Registros acotados a 64, stack máximo 24 frames sanitizados, clase de excepción, versión, Android, fabricante/modelo, módulo y memoria. Nunca Throwable.message/argumentos/request body/OS description/trace, PIN, tokens, hash, nombres de fichas, usuarios, fotos o coordenadas precisas. Pantalla DIAGNÓSTICO DE ESTABILIDAD y export FileProvider `gbo_diagnostics_YYYYMMDD.json`.

Riesgos de memoria encontrados por inspección de código, sin atribución causal al cierre físico:

- SCAP reunía ByteArray de **todas** las fotos JPEG a resolución original, además de workbook/ZIP. Ahora fotos originales JPEG y XLSX se transfieren al ZIP por flujo; imágenes de Excel se preparan individualmente con lado máximo 1600, conservando originales. PNG/WebP heredados se convierten individualmente para el contrato JPEG. Croquis limitados a 1200. Resolución reducida mantiene proporción/EXIF para Excel.
- Selector de 207 señales ya era lazy y cache 4 MiB; decodificación ahora muestrea a 256 para miniatura. Los 207 assets y sus hashes no cambian.
- Mapa usaba sample fijo 4, insuficiente para limitar cámaras de resolución alta: ahora vista previa usa miniatura de lado máximo 480.
- Miniaturas compartidas: LruCache 4 MiB, rechaza un elemento >1 MiB; keys por archivo/mtime/tamaño. Se libera cache en trim/low-memory, sin reciclar bitmaps aún mostrados ni tocar datos persistentes.
- Vista previa grande existente se solicita al abrir; pantallas lazy retiran referencias al salir. Mapas Canvas usan muestreo de geometría; imágenes KMZ son pequeñas y se reciclan. No se hicieron cambios de cálculo PCI/URCI ni se atribuyeron problemas a PDF/KMZ sin evidencia.

Pruebas nuevas: exclusión de mensajes secretos, bounded 64 eventos, fallback honesto, razón USER_REQUESTED/USER_STOPPED distinta, expiración scoped sin logout, JPEG original sin cambios y export de ambas plantillas. La reproducción 30–60 min y lectura real de ApplicationExitInfo están **pendientes en tablet**; no pueden reemplazarse por Robolectric.

Durante la validación local, una JVM de tests no pudo reservar memoria nativa en este PC. Se conserva el log de esa ejecución intermedia, se cerraron únicamente los procesos Gradle/Kotlin de esta tarea y se volvió a ejecutar con un solo worker, JVM 1 GiB y compilación en proceso. Ese fallo del host no es evidencia de una causa de cierres Android. No se incluyen hs_err/replay ni dumps completos del host en la entrega.
''')
rows=[]
for kind,asset in [('LOSA','scap_slab_catalog.json'),('MODULAR','scap_modular_catalog.json')]:
    data=json.loads((R/'app/src/main/assets'/asset).read_text(encoding='utf-8'))
    rows.append(f"## {kind}\n\nFuente SHA-256 `{data['sourceSha256']}`. {len(data['canonicalElements'])} elementos reales F.1.\n\n| ID plantilla | Código F.1 | Código G.1 | Nombre | F.1 fila | G.1 fila |\n| --- | --- | --- | --- | --- | --- |")
    rows += [f"| {x['stableId']} | {x['templateCode']} | {x['exportCode']} | {x['defaultName']} | {x['globalRow']} | {x.get('statisticalRow') or 'sin fila de ejemplo'} |" for x in data['canonicalElements']]
write('SCAP_BRIDGE_F_G_AUDIT.md','''
# Auditoría directa F.1 / F.2 / G.1

Se leyeron directamente los XLSX indicados por el usuario, sin reconstruirlos ni guardar con openpyxl. Fuente de listas: filas reales F.1; sourceSha256 verificado contra assets. Proyecto semántico F.1 Condición global, F.2 Panel fotográfico, G.1 Condición estadística. Nombres físicos intactos.

'''+ '\n'.join(rows)+'''

F.1 es la colección persistida; G.1 deriva aplicabilidad de esa misma colección. UNSET está incluido; NOT_APPLICABLE queda oculto sin borrar valores. Códigos originales y códigos de exportación son separados para no perder correspondencia; nombre observado y descripción global son datos de campo. No se copian cantidades/porcentajes de ejemplo a ficha nueva. Unidad/factor base provienen del G de la fuente cuando hay fila inequívoca; para los tres elementos LOSA omitidos en G se conserva la referencia AUXILIAR del catálogo ya recibido, documentada como fuente auxiliar.

F.2: 32 captions de ejemplo en D11:D42 por fuente; texto antes de dos puntos, 31 nombres únicos (una tubería repetida). Defaults disponibles en selector; registros reales se crean explícitamente, sin inventar 32 fotografías obligatorias. Se mantiene el texto/typos de fuente. Metadata y foto vinculados a UUID estable. Caption exportado NOMBRE: DESCRIPCIÓN, o solo nombre si falta descripción.
''')
write('SCAP_TEMPLATE_INCONSISTENCIES.md','''
# Inconsistencias de las fuentes originales

1. Ambas fuentes nombran físicamente `F.1- CONDICION GLOBAL`, pero C8 dice F.2. La UI usa F.1; se mantienen nombre físico y celda original de título, sin modificar assets.
2. LOSA: F.1 tiene 17 elementos; G contiene 14 ejemplos. Omite 216, 407 y 510. No se eliminan de F.1; G.1 muestra los 17 salvo No aplica. El catálogo auxiliar existente aporta unidad/factor para los tres omitidos; cantidades y ratings quedan vacíos.
3. MODULAR: «Muro de Concreto Armado en accesos» es 510 en F.1 y 504 en G.1. Coincidencia inequívoca de nombre y posición de grupo; stable/templateCode=510, exportCode=504 para G.1. F.1 conserva 510 por default; no se cambia la identidad interna ni el archivo original.
4. Hay 32 captions fotográficos y 31 nombres únicos; se deduplica el selector, no se impide repetir tipo en registros. Fuente contiene «SUPERFICIE DE RODARURA»; se conserva como default editable.
5. Existen defined names/referencias #REF! y fórmulas que no producen un agregado válido con datos vacíos o contribución máxima cero. No se corrigen fórmulas normativas sin definición de ingeniería. Aplicación muestra condición pendiente; Excel debe recalcular y ser revisado por el responsable del formato.
6. La plantilla tiene dos espacios de apoyo y pocas filas por grupo F.1. Continuaciones se crean copiando filas/bloques existentes **después** del contenido original. Los rangos definidos originales permanecen; `_xlnm.Print_Area` del generado se amplía para incluir lo adicional. Es una extensión técnica explícita; las fuentes originales, nombres de hojas, logos, estilos, geometría y demás defined names no se alteran.
7. En LOSA, las fórmulas compartidas O/P llegan hasta la fila 36; las filas 37–44 conservan entradas de unidad por lookup #REF!, pero carecen de esas fórmulas derivadas O/P. El límite de 31 filas de entrada no garantiza un agregado Excel válido para todos los manuales posibles. No se reconstruyeron fórmulas normativas; cualquier entrega que use esas filas necesita revisión del responsable del formato. Las 17 filas iniciales LOSA y el manual probado permanecen dentro del bloque con fórmulas.
''')
write('SCAP_F1_F2_G1_CORRECTION_REPORT.md',f'''
# Corrección SCAP de esta iteración

F.1 inicial persistido: LOSA 17 / MODULAR 10. Cada elemento mantiene UUID/ID estable, templateCode/exportCode/defaultName, customName/customCode/globalDescription y applicability en scap_values. F.1 permite nombre, código de impresión y descripción observada; plantilla solo No aplica, manuales se retiran con confirmación conservando fotos/observaciones. General observations permanecen accesibles.

G.1 usa la misma colección, incluyendo UNSET. Permite código de exportación, nombre, metrado, unidad/factor y porcentajes 0–5. Null inicial en todas las calificaciones, null → 0 solo en operación matemática, no en almacenamiento ni XLSX. Suma ≠100% no bloquea guardar/export/Drive; invalida agregado. Todo vacío no produce clasificación inventada. Reactivación conserva datos. Manual agrega simultáneamente F.1/G.1, sin catálogo duplicado independiente.

Factor de importancia explícitamente vacío permanece pendiente: el modelo numérico legado no convierte ese vacío en un factor efectivo 0 para calcular ni lo muestra como tal. Códigos o fecha fotográfica que el usuario vacíe también se identifican como pendientes. El código para cada hoja sigue editable sin modificar el ID/UUID lógico.

C.5: apoyos base 1/2, dropdown Izquierda/Derecha con LEFT/RIGHT; +Apoyo3 conserva la numeración; soporte adicional se imprime en bloque clonado. La UI no permite retirar apoyos base de nuevas fichas canónicas.

F.2 canónico: solo panel fotográfico, encabezado NRO/FECHA/DESCRIPCIÓN/DIGITAL; Nombre/Tipo editable con defaults completos extraídos, descripción, fecha capture/editada, imagen. Nuevo registro vacío guardable, cámara/seleccionar/reemplazar/remover/preview/reordenar; reemplazo conserva UUID y originales. Caption NOMBRE: DESCRIPCIÓN; relación de aspecto completa, sin recorte ni estiramiento. Se conserva el camino histórico para fichas anteriores sin tipo LOSA/MODULAR.

Pendientes: id, label, severity, code MISSING_VALUE/INVALID_VALUE y ScapFieldRoute; UUID/clave. Clic cambia sección, desplaza lista y campo, enfoca cuando es texto y resalta 1.8 s. No aplica excluye pendientes dependientes y G.1. No se generan destinos parseando texto de errores.

Entrega: DRAFT siempre guardable si persistible; valores técnicos inválidos conservan texto pero bloquean export. Falta de datos requiere confirmación de export/Drive. Metadata local Excel revision y completion.status, syncStatus separado. ACK de revisión vieja nunca borra edits. Destino completo v2 conservado; destino incompleto v1 existente con advertencia/destino visible. No hubo despliegue remoto. Una ficha incompleta subida mantiene edición local y referencias Drive.

Pruebas: {test_summary} Evidencia JUnit/XML, captions de source, ambas fuentes por hash, dimensiones/merges/styles/defined names, continuidad UUID/ratings/supports, blanks y JPEG original. Validación física en Excel/tablet pendiente.

Límites técnicos de plantilla que siguen vigentes: 31 elementos G.1, 32 registros fotográficos, dos tramos, tres pilares, una junta, 15 puntos de perfil, un croquis/tipo. Los límites se muestran como errores técnicos explícitos, no se truncarán registros silenciosamente.
''')
steps=[
'Iniciar sesión normal en la tablet administradora, sin borrar datos.',
'Comprobar botón TABLET_ADMIN solo con permiso explícito; ADMIN sin permiso no basta.',
'Abrir panel, reingresar PIN; comprobar autorización 15 min y usuario actual sin pedir username.',
'En tablet destino copiar huella completa de 64 hex y registrarla con alias en administradora.',
'Copiar token mostrado una sola vez; cerrar aviso y comprobar que no existe Ver token posteriormente.',
'Configurar URL existente y token en tablet nueva; no tocar identidad ni borrar registros.',
'Sincronizar usuarios; comprobar login normal offline después de sync válido.',
'Listar tablets: alias, activa/revocada, huella abreviada; ninguna columna token/hash.',
'Revocar destino con confirmación; no revocar la administradora desde su sesión.',
'Sincronizar destino: debe rechazar DEVICE_REVOKED y conservar Room/fotos.',
'Reactivar destino con confirmación.',
'Con el mismo token anterior comprobar acceso restaurado y datos conservados.',
'Rotar token del destino con confirmación; copiar nuevo token una vez.',
'Comprobar rechazo del token anterior y de sus sesiones administrativas.',
'En login destino usar Actualizar token autorizado, nuevo token y verificación online; comprobar acceso con misma huella.',
'Abrir historial y comprobar fechas/actor/alias/acción/huella abreviada, sin secretos.',
'Owner retira permiso mediante applyTabletAdminPermission en editor; no desde APK.',
'Próxima operación privilegiada debe fallar y cerrar panel; sesión normal debe seguir. Probar también TTL sin matar app.',
'Usar app continuamente 30–60 minutos y anotar horas/cualquier salida real.',
'Tomar/importar/reemplazar/preview varias fotos grandes; alternar módulos sin perder borradores.',
'Abrir selector de 207 señales y recorrerlo; comprobar imágenes y scroll fluido.',
'Abrir mapas offline y vista previa de fotos; mover/zoom sin Internet.',
'Crear/editar borrador PCI y URCI; verificar cálculos previos conservados.',
'Crear SCAP y alternar A–G; guardar, cerrar/abrir y retomar.',
'Enviar app a background por poco tiempo y luego por más de 15 minutos.',
'Volver: comprobar PIN por timeout largo y ficha retenida; no cierre de Activity.',
'Bloquear pantalla con ficha en curso.',
'Desbloquear y comprobar draft/fotos/UUID.',
'Abrir otra aplicación y cámara/selector externo.',
'Volver y revisar valores originales sin inventar ceros.',
'Abrir diagnóstico; leer última salida/reasonCode real o Causa no disponible.',
'Exportar gbo_diagnostics_YYYYMMDD.json; revisar que no incluya PIN/tokens/nombres/coords.',
'Crear nueva ficha tipo LOSA, confirmar selección y fuente correcta.',
'Abrir C.5: Apoyo 1 y 2 ya deben ser visibles sin añadir.',
'Elegir Izquierda/Derecha; cerrar/reabrir y verificar LEFT/RIGHT conservado.',
'Añadir Apoyo 3, completar algo, guardar/reabrir; exportar continuación.',
'F.1 debe contener 17 elementos reales de fuente, inicialmente Sin completar.',
'Editar nombre/código/descripcion global; añadir manual; comprobar exportación.',
'G.1 debe mostrar los 17 incluso UNSET, más manuales aplicables.',
'Marcar un elemento No aplica: desaparece G.1 y sus pendientes.',
'Reactivar: vuelve con metrado/calificaciones/descripcion anteriores.',
'G.1 ingresar 90 en nivel 1 y 10 en nivel 2; dejar resto vacío; revisar cálculo.',
'Exportar SCAP confirmando faltantes cuando corresponda.',
'Abrir Excel: null vacíos, 90/10 reales, nombres físicos/logos/formulas/estilos correctos.',
'F.2 selector: revisar todos los nombres extraídos, incluidos ejemplos no abreviados.',
'Editar nombre de foto; comprobar campo y caption exportado.',
'Añadir registro, descripcion/fecha, tomar/importar; preview, reemplazar/remover/reordenar.',
'Exportar fotos vertical/horizontal; comprobar completas sin stretch/crop y fechas.',
'Dejar al menos diez datos faltantes y guardar borrador.',
'Abrir Ver pendientes: contar warnings separados de valores inválidos.',
'Tocar pendientes A, Apoyo2.Material, F.1 descripción, F.2 foto, G.1 metrado.',
'Cada clic cambia sección y lleva al UUID/campo exacto, foco de texto y highlight breve.',
'Exportar de todos modos; revisar entradas faltantes vacías y completion INCOMPLETE.',
'Subir de todos modos en build configurada, revisar destino mostrado; ACK/sync separado, editar luego manteniendo UUID/fotos/Drive y versión anterior.'
]
assert len(steps)==54
write('FINAL_TABLET_CHECKLIST_THIS_ITERATION.md','''
# Validación física pendiente — esta iteración

Todos los pasos están **pendientes de ejecución por el usuario en tablet**. Registrar modelo, Android, certificado de instalación actual, hora, versión 14, resultado PASS/FAIL, diagnóstico si falla y evidencia. APK debug validation no incorpora configuración Drive; para subida real se requiere build/entorno configurado conforme a política existente. No desinstalar para simular actualización; no sustituir firma corporativa por debug.

'''+ '\n'.join(f'{i}. [ ] {step}' for i,step in enumerate(steps,1))+'''

Repetir pasos 33–54 en **MODULAR**: F.1 y G.1 iniciales 10, verificar mapeo Muro F.1 510/G.1 504 y restore de No aplica. Repetir recuperación tras process death con borrador ya marcado Guardado local. En normal/administración comprobar timeout sin pérdida de datos. No marcar PASS por inspección de código.

Adicionales negativos: PIN incorrecto, usuario inactivo, ENGINEER sin permiso, ADMIN sin permiso, huella nohex/63/65 caracteres, duplicada, self-revoke, token de usuarios en endpoints tablets y viceversa, retirado permiso durante sesión, token rotado erróneo en destino conserva credencial cifrada previa. No publicar capturas con token visible.
''')
write('SCAP_BRIDGE_TABLET_CHECKLIST.md','''
# Checklist SCAP LOSA/MODULAR en tablet y Excel

Estado: validación física pendiente. Ejecutar los pasos 33–54 de FINAL_TABLET_CHECKLIST_THIS_ITERATION.md en **cada tipo**; registrar resultados por separado.

- [ ] LOSA 17 F.1 y 17 G.1 iniciales UNSET; MODULAR 10/10. Estado No aplica resta uno, restore mantiene valores.
- [ ] Elemento manual crea una fila común en F.1/G.1; editar nombre/código/descripcion, No aplica/restore, remover con confirmación sin eliminar archivos.
- [ ] Apoyos 1/2 iniciales; dropdown LEFT/RIGHT traducido; Apoyo3 persiste y aparece en continuación Excel. No ejemplos falsos copiados.
- [ ] Seis calificaciones null al inicio; solo 90/10 ingresados, resto vacío en almacenamiento/Excel y cero solo al calcular. Suma 99 advierte, deja guardar/export/subir confirmados; no agregado inventado.
- [ ] F.2 canónico es único panel: 31 nombres únicos de 32 captions, editable, +registro, descripcion/fecha, preview/cámara/import/replace/remover/reordenar. Caption y aspect ratio correctos. Sin apartado Condición Global en F.2 ni tab FOTO separado.
- [ ] Pendientes A/C.5 soporte2/F.1/F.2/G.1 desplazan al UUID/campo; resaltado 1–2 s y foco en texto. NA no genera pendientes dependientes.
- [ ] Diez faltantes permiten DRAFT y XLSX vacíos; confirmación requerida en Drive, completion INCOMPLETE/sync separados, uploadedWithWarnings, posterior edición conserva UUID y relación Drive/revisiones.
- [ ] Archivo generado conserva hojas/nombres/logos/styles/merges/columnas/alturas/formulas; solo se amplía Print_Area para continuaciones y se reemplazan entradas/imágenes de usuario. Verificación visual/recalculo real en Excel aún pendiente.
- [ ] Draft guardado sobrevive cerrar/volver/process death sin perder fotos. Escenario SDK viejo muestra causa desconocida sin evidencia; diagnóstico nunca contiene los datos de la ficha.

Revisar SCAP_TEMPLATE_INCONSISTENCIES.md: C8 fuente dice F.2 global, G LOSA omite tres elementos, Muro MODULAR 510/504 y #REF! existentes. No confundir esas inconsistencias de fuente con corrupción del archivo.
''')
print('Generated 9 requested reports/checklists; tests:',test_summary)
