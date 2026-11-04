# Candidato local para GBO_Inventario original — NO DESPLEGADO

## Vigencia 04/11/2026: prevalece el expediente de corte

[Expediente actual](../../docs/EXPEDIENTE_CORTE_INSTITUCIONAL_20261103.md): Hito A cerrado sin cambios de código. Inventario 16/16 anterior conciliado y caché descartable implementada; los apartados históricos siguientes que indican inventario inexistente, vídeo no enrutado o ausencia de caché han quedado superados. La latencia real de caché, identidad runtime y entrega real siguen pendientes. B=DRAIN_ONLY y C=OPEN requieren autorizaciones distintas y pausa de clientes. Antes de marca/pin/efecto, rollback condicionado al respaldo inmediato; después, únicamente recuperación compatible. No aplicar las instrucciones históricas de rollback sin estas guardas.

## Cierre histórico — 03/11/2026, propuesta sin activar

Este apartado sustituye las restricciones antiguas de vídeo y admisión descritas abajo.
La base contractual se volvió a descargar mediante GET: los diez archivos de V21 coinciden
con sus capturas anteriores y con el editor, que solo añade el auditor de lectura.
El empaquetador verifica los hashes de la captura V21 y conserva esos módulos, manifest,
autenticación y literal ROOT original.
Nunca hace push, run ni deploy. Exige un directorio de salida nuevo y verifica cada
transformación del adaptador original de vídeo antes de generar el paquete privado.

- `NewRootAdmission.gs` exige ejecutor Drive institucional, propietario institucional de
  destino/ancestros, Mi unidad, permisos verificables y ausencia de edición pública.
- El corte anuncia `rootRoutingVersion=2`, un `rootEpoch` público de 32 hex y
  `rootActivatedAt`. El cliente añade el `captureCreatedAt` persistido de la ficha.
  Una identidad desconocida anterior al corte, sin estos datos, con otra época, con
  reloj futuro mayor de cinco minutos o con baseRevision distinta de cero se bloquea
  **antes de crear carpetas, reservas o staging**. Una edición no rejuvenece una ficha.
  Es admisión dentro del contrato de subida autenticado, no autoridad administrativa
  ni prueba criptográfica del reloj del dispositivo. Los relojes deben estar correctos.
- Registros, reservas y bindings existentes conservan su raíz. Un SCAP comparte la
  identidad del puente. Ambigüedad entre raíces, metadatos duplicados o ilegibles y
  conflictos de código se bloquean; nunca se inventa una asociación.
- La configuración opcional `oldRootReadOnly:{name,ownerEmail}` permite leer una raíz
  histórica compartida que pertenece a otra cuenta sin fingir acceso a sus ancestros.
  Comprueba ID literal, nombre, propietario, tipo y papelera; **rechaza todas sus
  operaciones de entrega, incluidos vídeo y replay**. No cambia pendientes ni ACK.
  Este es el modo preparado para el corte; reanudar históricos requerirá otra revisión.
- Vídeo nuevo sigue exclusivamente al SCAP confirmado de la misma raíz. El adaptador
  ya no llama `v3Store()` para leer el padre. Verifica revisión, ACK, journal y cadena
  de carpeta antes de escribir. PENDING devuelve BUSY sin recuperación. Conserva
  nombres V1/V2, ID binario, sesiones reanudables y checksum de V21.
- `DRAIN_ONLY` termina entregas reanudables y vídeos ya vinculados; bloquea nuevas
  revisiones, identidades y vídeos. La marca de activación se persiste bajo ScriptLock
  **antes de la primera escritura Drive**, con raíces, época, instante y política de
  históricos inmutables. Desactivar o sustituirlos después se rechaza.
- Las rutas legacy se rechazan durante el modo dual. No hay borrado administrativo,
  migración, recuperación legacy automática ni modificación del Servicio de Usuarios.

Configuración adicional privada: `admission:{version:1,epoch,activatedAt,executorEmail}`
dentro de `GBO_ROOT_ROUTING_CONFIG`. No rellenar `inventoryReviewed` ni su SHA con
resultados simulados. El destino operativo y sus IDs no se incorporan a este módulo.
`rootFolderId` del GET conserva el ancla histórica del contrato V21; no se debe usar
para inferir el destino de una entrega enrutada.

Android incorpora únicamente metadatos de transporte negociados; con V21 se eliminan
del sobre HTTP. No cambian canonical hash, UUID, revisión, ACK ni Room. Preparaciones
históricas sin fecha no reciben una fecha inventada. La APK 1.4.29 sigue intacta; este
contrato necesita una única sucesora, que se empaquetará al fijar el endpoint institucional.

**Limitaciones reales:** inventario institucional y desplegador aún no acreditados.
El enrutador lee completamente los registros bajo bloqueo, con presupuesto cooperativo
de 85 segundos; no hay índice/cache validado. El coste sobre el volumen real debe
medirse antes de activar. Una llamada Drive individual puede exceder ese presupuesto.
Las pruebas en memoria no acreditan latencia, permisos ni propiedad en Google.

63 casos backend únicos PASS y 41 Android únicos PASS en la selección afectada.
Las pruebas de enrutamiento/vídeo usan fuente pública y mocks sin cuenta Google; la
prueba `PreparedOriginal.test.cjs` recibe explícitamente `GBO_PREPARED_ORIGINAL_DIR`
apuntando al paquete privado generado desde la captura autorizada, siempre en una VM.
El procedimiento único y el estado para retomar están en
`docs/PROGRESO_ACUMULADO_GBO.md`. **NO-GO productivo**; no se cambió Google.

## Propuesta vigente de coexistencia — 01/11/2026, NO ACTIVAR

`DeliveryRootRouting.gs` añade un modo opcional y cerrado por defecto. **No se cambia
ROOT_FOLDER_ID**: sigue identificando la raíz original, junto con su guarda y
GBO_RESUMABLE_ROOT_ID. La raíz nueva se declara por identidad exacta en configuración
privada separada. No mover/copiar registros, bindings, recibos, fotos ni journals.

- Bajo el mismo ScriptLock, lee íntegramente los dos registros mediante iteradores, sin
  límite de 100 ni creación de carpetas durante la selección. Si no puede leer o resolver
  identidad, bloquea; nunca interpreta un fallo como ausencia. El presupuesto de lectura
  es cooperativo, no garantiza duración de una llamada Drive individual.
- Key existente, UUID padre, binding, reserva, staging o journal mantienen al propietario
  en su raíz. Una identidad no presente en ninguna usa el destino nuevo. SIC y SCAP del
  mismo puente permanecen juntos, aunque tengan recordId distintos. bridgeCode se comprueba
  globalmente; identidad repartida entre raíces se rechaza y requiere revisión.
- Un pin `root_route_v1_<hash(key)>.json` conserva la asociación key/UUID/raíz para las
  solicitudes antiguas de staging que solo llevan key/revisión. No contiene ACK ni copia
  registros. Android nuevo incluye también UUID padre en el sobre de staging, sin cambiar
  el contenido congelado ni el hash. Un SCAP antiguo sin pin/registro/journal y sin UUID no
  se adivina: `ROOT_ROUTE_RESERVATION_REQUIRED`; conciliar/actualizar cliente con respaldo.
- Preparaciones y archivos se validan en la raíz elegida. Cada raíz conserva sus
  consolidados EXCEL_SIC; **no se presenta una unión ficticia entre raíces**. Los nombres
  descriptivos siguen requiriendo capacidad/flag explícitos y respetan reservas previas.
- PENDING reanudable continúa su journal e IDs; un PENDING histórico sin versión no se
  adopta. Conflictos de bridgeCode, UUID ausente, registro ilegible o doble autoridad se
  conservan para conciliación. El motor transaccional y canonical hash no se relajaron.

Configuración propuesta, **no establecida en Google**:

- `GBO_ROOT_ROUTING_ENABLED` exactamente `true` solo tras aprobación específica del corte.
- `GBO_ROOT_ROUTING_CONFIG` JSON: `version:1`, `oldRootId` igual al literal original,
  `newRoot:{id,parentId,name,parentName,ancestorIds}`, `mode:"OPEN"|"DRAIN_ONLY"`,
  `inventoryReviewed:true`, `inventorySha256` de 64 hex del inventario privado conciliado.
  Este marcador exige una revisión humana real; no verifica por sí solo que Google fue auditado.
  La raíz nueva pasa las mismas comprobaciones de tipo, padres, nombre y escritura. Ninguna
  raíz puede contener a la otra. No hay valores operativos en esta fuente versionable.
- En la primera reserva/staging enrutada, `GBO_ROOT_ROUTING_ACTIVATED` conserva la pareja
  de IDs. Deshabilitar el modo o sustituir raíces después queda bloqueado en este candidato.

**Límites que impiden declarar listo el corte:** modo dual admite únicamente reserva V2,
staging y commit V3. APIs legacy y vídeo están bloqueadas explícitamente; no están enrutadas
ni acreditadas. Debe inventariarse su uso/pendientes antes de activar. La guarda no soluciona
un cliente con preparación SCAP antigua sin UUID/pin. El fencing de PENDING del adaptador
transaccional sigue abarcando la raíz seleccionada por protección de consolidados: otras
entregas de esa misma raíz pueden recibir BUSY; no prometer aislamiento por ficha.

**Rollback tras usar ambas raíces:** poner este candidato compatible en `DRAIN_ONLY`
mediante ventana autorizada; permite replay exacto de ACK y terminar el PENDING reanudable
de la revisión exacta, bloqueando identidades/revisiones/reservas nuevas. Conservar ambas
raíces accesibles y todos sus metadatos. **No volver a V21 ni quitar el módulo a ciegas:**
V21 no conoce la raíz nueva ni respeta el marcador de activación. El procedimiento antiguo
de volver a V21 solo resulta aplicable antes de crear autoridades nuevas y tras conciliación.
El marcador no puede impedir que un operador despliegue manualmente código antiguo.

El empaquetador conserva manifest/URL/router/autenticación y añade el control después de
autenticar. El GET solo anuncia continuidad con configuración preparada válida; readiness
no sustituye inventario, permisos reales, aprobación, ACK operativo o prueba física.

Los apartados siguientes conservan el historial de la propuesta de una sola raíz y sus
resultados por fecha; se subordinan a este modo opcional y sus restricciones.

## Actualización prioritaria: incidencia de destino, 30 de octubre

`AuthorizedRootGuard.gs` exige cinco propiedades privadas: `GBO_AUTHORIZED_ROOT_ID`,
`GBO_AUTHORIZED_ROOT_PARENT_ID`, `GBO_AUTHORIZED_ROOT_NAME`, `GBO_AUTHORIZED_ROOT_PARENT_NAME`
y `GBO_AUTHORIZED_ROOT_ANCESTOR_IDS` (array JSON desde padre directo hasta contenedor superior,
excluyendo Mi unidad; sin duplicados, máximo 16 niveles).
El ID aprobado debe coincidir con `ROOT_FOLDER_ID`. Antes de escribir comprueba con Drive
el ID, tipo carpeta, papelera, nombre, padre exacto, todos los enlaces de ancestros
hasta Mi unidad de la cuenta ejecutora y capacidades `canEdit`/`canAddChildren`.
Rechaza el alias `root`, la raíz real de Mi unidad y cualquier sustitución o identidad
incompleta. No busca carpetas por nombre, no crea destino alternativo ni archivo de prueba.
Un error de lectura es reintentable; una identidad o permiso incorrectos bloquean.

El empaquetador añade la guarda al router después de autenticar, cubre también rutas
legacy y cachés, reemplaza las entradas de raíz de Código.gs y VideoV1.gs y conserva
manifest, autenticación y raíz literal original. `SyncV3.gs` la usa además directamente
antes de crear el registro/staging. GET anuncia `rootGuardVersion` y `rootGuardReady`;
solo anuncia continuidad/nombres públicos si la guarda está satisfecha.
El núcleo de transacciones no se cambió. **Nada de esto está desplegado.**

La ruta solicitada es `Mi unidad / 05 V CAMPO / 1.- ARCHIVOS CAMPO APK`.
Se observó su ID real mediante navegación desde la cuenta GBO, pero el usuario confirmó
después **usar el enlace que envió aunque corresponde a otra carpeta**. El destino elegido
es ahora `1.- ARCHIVOS CAMPO`, sin APK; esta elección sustituye la ruta inicial.
No configurar un ID por parecido ni cambiar de raíz ahora: falta conciliar pendientes.
La incidencia completa con IDs permanece exclusivamente en el informe privado local
`GBO_CORRECCION_DESTINO_Y_LIMPIEZA.md`, excluido de Git.

Las instrucciones antiguas siguientes describen la base del candidato, no autorizan un
cambio de raíz. Previamente al corte se necesita inventario operativo completo y estable,
conciliación de entregas preparadas/reservas/revisiones/ACK de la raíz anterior, confirmación
del destino y autorización expresa. La misma URL no hace intercambiables dos raíces:
no se pueden copiar ACK ni adoptar históricos automáticamente. El rollback de código a
V21 tampoco revierte por sí solo el destino ni resuelve transacciones nuevas pendientes.

Prueba focalizada inicial: **26/26 PASS, 0 fallos, 0 omitidas, 39,986 s**:
7 guardas, 5 paquete original y 14 continuidad del candidato original, exclusivamente
en memoria/mocks con fixtures ficticios. No acredita HTTP productivo, permisos efectivos
del runtime original ni piloto Android. Log privado en el directorio de la incidencia.
Se añadió después comprobación de cadena anidada al confirmar el enlace distinto:
**27/27 PASS**, 0 fallos/omitidas, 39,199 s (8 guarda, 5 paquete y 14 continuidad).
No usar el paquete inicial de cuatro
propiedades como candidato final.

Este directorio es fuente para el proyecto original, no otro servidor ni otra copia de Apps Script. Mantener la URL del despliegue versionado original, su ejecutor, token legítimo y `ROOT_FOLDER_ID` actuales. No usar módulos de ensayo, Ensayo.gs, Auditoria.gs ni propiedades/tokens ficticios.

Reemplazos preparados: `SyncV3.gs`, `SyncV3Core.gs`, `PhotoNamesV2Core.gs`; añadir `PublicDeliveryNames.gs`. Conservar el resto de módulos y el manifest original. Las guardas de la copia aislada se sustituyeron por `GBO_RESUMABLE_ROOT_ID`, que debe coincidir exactamente con la raíz operativa existente. La inyección de fallos de ensayo queda inerte. Una propiedad ausente o distinta bloquea antes de escribir.

El núcleo conserva protocolo 3, pathVersion 4, UUID, revisión, reservas V2, SHA, journal y ACK estricto. Presupuesto cooperativo de 85 s antes de operaciones; una llamada individual de Drive puede demorarse y no es cancelable desde Apps Script. No se promete un límite duro de seis minutos para una llamada externa. Una continuación devuelve `COMMIT_IN_PROGRESS`, retryable, sin ACK; el cliente preserva el paquete y los IDs de staging. El journal y la intención de cada ID se persisten antes de crear; el replay reconcilia los resultados y evita duplicados. Una transacción pendiente protege los consolidados con BUSY.

**Journals anteriores sin continuationVersion:** se conservan y bloquean con `LEGACY_RECOVERY_REQUIRES_REVIEW`, sin intentar la adopción automática que solo fue autorizada para el fixture de pruebas. Antes del corte se necesita inventario privado de PENDING y de las revisiones/ACK. No cambiar de raíz ni borrar/finalizar journals para sortear este bloqueo.

Nombres descriptivos: negociación opcional `publicNamingVersion=1`, compatible con V2. `GBO_PUBLIC_NAMES_ENABLED=true` habilita la aceptación; no activarla hasta validar los clientes destinatarios. Nuevos bindings pueden recibir sufijo descriptivo; bindings y fotos ya reservados conservan nombre, número, carpeta e identidad. Las etiquetas se incluyen en el canonical hash. El cliente congela la reserva y rechaza otro destino. Tras rollback a código sin esta capacidad, Android retiene las nuevas entregas descriptivas en lugar de enviarlas a ciegas. Validar interoperabilidad antes de habilitarlas en tablets antiguas; no se acredita esa validación física con mocks.

## Corte preparado, requiere autorización explícita

1. Volver a obtener la fuente y configuración exactas del original y verificar el respaldo privado. Snapshot local de la intervención anterior recopiado en `tmp/entrega-20261030/backend-privado/cierre-final-original/`; el manifest privado identifica procedencia y limitaciones. No se afirma una nueva descarga íntegra de la versión 21.
2. Verificar que el despliegue registrado sigue en versión 21 y que la URL coincide con la APK. La revisión UI actual confirmó ejecutor GBO; no se guardó código ni se modificó el despliegue.
3. Comprobar PENDING, bindings, ACK y accesibilidad de la raíz actual. Resolver cada journal anterior mediante un plan revisado. Si quedan pendientes no comprendidos, **NO-GO**.
4. Con autorización de producción, sustituir solo los módulos indicados en el original. Establecer `GBO_RESUMABLE_ROOT_ID` con el valor legítimo actual, sin cambiar ROOT ni token. Dejar nombres descriptivos deshabilitados inicialmente si la compatibilidad de las tablets no está acreditada.
5. En el `doGet` existente añadir `commitContinuationVersion:1` condicionado a la raíz aprobada. Anunciar `publicNamingVersion:1` únicamente si `GBO_PUBLIC_NAMES_ENABLED` es exactamente `true`. No anunciar capacidades de Pavimentos que el original no implemente. No añadir tokens ni configuración privada a las capacidades; el empaquetador conserva el resto de campos existentes por compatibilidad.
6. Crear una versión del **mismo proyecto** y editar el **mismo despliegue** para usarla, conservando URL, identidad ejecutora y acceso. No crear nueva implementación ni proyecto. Verificar capacidades reales y una operación ficticia pequeña autorizada con ACK, pérdida de respuesta y replay; no usar fichas reales ni repetir el ensayo masivo acreditado sin motivo.
7. Piloto de actualización Android sobre la instalación original: firma histórica, versión superior y datos conservados. Requiere aprobación de instalación; no desinstalar ni limpiar datos.

## Rollback

Conservar versión 21 y los valores previos en el respaldo privado. Ante defecto del corte, detener nuevos envíos y revertir **el mismo despliegue** a versión 21. No borrar journals, staging o registros creados por el candidato. Si existe un journal reanudable PENDING o un nuevo binding descriptivo, la versión 21 no garantiza recuperarlo: mantener la cola y reconciliar o volver al candidato corregido antes de liberar envíos. No usar rollback para ocultar un ACK ni copiarlo a otra raíz.

## Pruebas locales

`GBO_RESUMABLE_SOURCE=original node --test backend/aislado-commit-resumible/tests/Continuation.test.cjs` (en Windows establecer la variable de entorno en el proceso; el mock necesita Python real en PATH). Usa exclusivamente datos ficticios y mocks. `tests/PublicNamesContract.json` es el contrato ficticio generado y consumido por Kotlin, sin endpoint, token o datos de campo. No equivale a prueba HTTP del despliegue operativo.

La carpeta definitiva solicitada no se configura aquí: el acceso público de escritura observado y el inventario de pendientes deben resolverse por separado, con autorización para cualquier cambio de permisos o raíz.

El empaquetador `preparar_paquete_privado.py --original-dir <respaldo-privado> --output-dir <destino-privado-nuevo>` produce el conjunto completo sin despliegue, conserva la raíz literal del original y exige que coincida con su GET operativo guardado. El destino debe quedar fuera de Git; no sobrescribe un paquete existente. Las tres pruebas de `tests/PreparedOriginal.test.cjs` cargan ese paquete con la raíz reemplazada **solo dentro del mock**, conservan el router/autenticación y comprueban sus capacidades. Establecer `GBO_PREPARED_ORIGINAL_DIR` únicamente para esa ejecución local; no solicita credenciales ni publica contenidos.
