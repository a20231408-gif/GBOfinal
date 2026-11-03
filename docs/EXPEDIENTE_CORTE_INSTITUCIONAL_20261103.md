# Expediente de autorización del corte institucional — 03/11/2026

Estado: PREPARADO PARA AUTORIZACIÓN ESCALONADA; NO APLICADO; NO-GO distribución. Checkpoint verificado local/remoto `4a592e35051bcbd761389e9db75f08f0b85bbf2a`, rama `codex/scap-pavimentos-cierre-20261103`, PR #11 draft con dependencias #10/#9/#8. Sin merge. Este expediente no concede autorización.

## Código exacto y versión

Paquete privado `paquete-final-cache-no-activar`, ensamblado desde captura inmutable de V21 real: 14 archivos, hashes revisados sin recompilar ni repetir pruebas. Manifest SHA256 `d26e19de83ad867561d9215ab0c4bada4f4e60db9b7055504a08797ce845e161`.

- REEMPLAZAR: Código.gs, PhotoNamesV2Core.gs, SyncV3.gs, SyncV3Core.gs, VideoV1.gs (cinco archivos; ver inventario de operaciones exacto privado).
- AÑADIR: AuthorizedRootGuard.gs, DeliveryRootRouting.gs, NewRootAdmission.gs, PublicDeliveryNames.gs.
- CONSERVAR: appsscript.json, ScapPhotoPanelV2.gs, SicObservationsV1Core.gs, VideoNamesV2Core.gs, VideoV1Core.gs.
- Preservar también PreflightReadonly.gs y PreflightReadonlyPaged.gs del editor. No subir todo un directorio mediante clasp eliminando estos lectores. `Codigo.gs` del paquete sustituye al único `Código.gs` original; no añadir otro router.

Corrección a la anotación previa: el inventario exacto contiene CINCO reemplazos, cuatro adiciones y cinco archivos conservados (14), no seis reemplazos. La fuente candidata está versionada en `backend/candidato-original-reanudable/`; el router completo con configuración histórica permanece privado.

V21 activa. Propuesta: V22 si sigue libre al ejecutar; si cambió el historial o deployment, detener y revisar. Mismo proyecto original, deployment ID y URL /exec Android, especificados en anexo privado. No crear otro servidor/deployment. No cambiar ROOT_FOLDER_ID literal, Users, credenciales, scopes ni permisos.

## Propiedades propuestas (ninguna aplicada)

| Propiedad | Cambio aprobado que se solicitará |
|---|---|
| GBO_ROOT_ROUTING_CONFIG | Crear configuración versionada con raíz histórica READ_ONLY, destino institucional exacto y ancestros, inventario/hash, modo DRAIN_ONLY, época y cuenta ejecutora esperada |
| GBO_ROOT_ROUTING_ENABLED | Crear true solamente al instalar el corte aprobado con CONFIG válida; control explícito del orden |
| GBO_RESUMABLE_ROOT_ID | Crear enlace al ID histórico literal; activa compatibilidad reanudable sin trasladar la raíz |
| GBO_AUTHORIZED_ROOT_ANCESTOR_IDS | Crear [] para la raíz histórica compartida, cuya identidad/propietario se validan por oldRootReadOnly; NO sustituye la cadena obligatoria del destino nuevo |
| GBO_PUBLIC_NAMES_ENABLED | Crear false: conservar negociación original; no activar un cambio adicional de nomenclatura remota |
| GBO_ROOT_ROUTING_ACTIVATED | NO crear manualmente. Lo escribe el backend antes de su primera mutación autorizada; después es una cerca durable que no debe retirarse |

Estas seis claves estaban ausentes en el respaldo de 50 propiedades; volver a comparar el estado antes de aplicar, sin sobrescribir cambios ajenos. Preservar las demás propiedades completas. Epoch privado ya preparado; activatedAt se fija UNA vez al instante UTC del corte autorizado, no usar una fecha de preparación antigua. No cambiar epoch/timestamp después de activación. El cambio DRAIN_ONLY→OPEN se hará solo bajo las condiciones de aceptación siguientes.

## Identidad, URL y destino

Cuenta GBO acreditada en sesión/editor y en lector real Drive.About; destino y dos ancestros son propiedad GBO, con capacidades de escritura y sin edición general. URL definitiva PROPUESTA: la misma /exec que usa APK44, sin /dev ni endpoint de ensayos. El anexo privado contiene URL/deployment/scriptId exactos. USER_DEPLOYING y «Yo» NO identifican por sí solos el ejecutor actual del endpoint; no afirmar que está acreditado.

Tras autorización: publicar candidato en el deployment original inicialmente DRAIN_ONLY. GET de capacidades debe dar rootGuardReady=true y rootRoutingVersion=2/epoch esperados: esa rama ejecuta gboAssertInstitutionalExecutor mediante Drive.About y compara con la identidad institucional configurada. Si no coincide, no habilitar OPEN ni enviar fichas. No crear otro deployment para salvar el bloqueo. Un consentimiento de Google lo completa el propietario oficialmente.

## Históricos y los 13 bindings

Inventario real 16/16 estable: 207 journals COMPLETE, 207 records y 220 bindings. Todos los ACK comparados coinciden en key/revisión/hash y carpeta de binding. Los 13 bindings sin record están individualizados en CONCILIACION_BINDINGS_PRIVADA.json con acción PRESERVE_OLD_ROOT_NO_ADOPTION. Son reservas/identidades históricas, no fichas declaradas descartables. Ninguno se borra, libera, adopta, reintenta o migra. Una solicitud con su identidad queda vinculada a la raíz antigua y bloqueada por HISTORICAL_ROOT_READ_ONLY. Duplicados, conflicto SCAP, autoridad ilegible o identidad repartida bloquean la entrega afectada sin inventar IDs.

Todas las nuevas capturas admitidas requieren sobre de enrutamiento v2, epoch correcta, fecha original posterior al corte y baseRevision=0, sin evidencia histórica en ninguna raíz. Reserva/pin durable anterior a efectos fija la raíz. No reemplazar una fecha antigua por la actual. Clientes antiguos y colas históricas pueden quedar BLOQUEADOS; esto afecta disponibilidad y debe aceptarse expresamente. No es una migración transparente para todas las tablets.

## Secuencia autorizable y recuperación

1. Comparación de código/editor, V21/deployment y propiedades contra capturas privadas; si cambió algo, detener el corte. Ratificar cadena/propiedad/permisos y destino vacío. Pausa operativa de sincronización de otros clientes acreditada por el responsable.
2. Respaldo privado inmediato de fuente/configuración afectada, sin datos en GitHub. Instalar solo archivos revisados; crear V22 disponible y actualizar el MISMO deployment. CONFIG DRAIN_ONLY e identidad/tiempo correctos antes de habilitar routing. Ninguna carga en esta fase.
3. GET de capacidades acredita la guarda ejecutora y protocolo. Ejecutar gboWarmRootRegistryReadCache hasta complete=true, con presupuesto acotado: solo ScriptCache desechable, sin Drive/propiedades/pins. Verificar estabilidad frente al inventario; caché perdida se reconstruye y nunca autoriza omitir filas.
4. Antes de OPEN: aceptación física del propietario, copia vigente del teléfono, cola/reservas/intentos elegibles revisados, identidades/rutas ficticias ausentes y resto de clientes en pausa. OPEN no es una lista de dispositivos autorizados: si no puede aislarse operativamente, no abrirlo.
5. Cambiar solo modo a OPEN para el ensayo aprobado. Tras prueba, mantener distribución suspendida hasta dictamen; ante fallo volver a DRAIN_ONLY compatible y conservar evidencia.

Antes de primera marca de activación o efecto nuevo: se puede restaurar deployment V21 y las propiedades exactas respaldadas tras comprobar ausencia de mutaciones. Después de GBO_ROOT_ROUTING_ACTIVATED o cualquier efecto nuevo: NO volver a V21 ni desactivar routing/retirar la marca. DRAIN_ONLY permite reanudación conocida y ACK idempotente, bloquea admisión nueva; mantener journals, staging, pins y raíces, y usar corrección compatible revisada si fuese necesaria. BUSY/lectura incompleta no dan ACK; hashes/identidad inválidos no se fuerzan; un PENDING conserva operaciones/IDs y se reanuda únicamente por contrato. No borrar pruebas ni activar eliminación administrativa.

## Prueba real mínima, aún NO autorizada ni ejecutada

Solo teléfono de pruebas expresamente autorizado (OPPO si cumple aislamiento); excluir Xiaomi principal. La aceptación offline puede crear trabajo pendiente: volver a revisar la cola antes de conectar, sin cancelar, editar o reenviar registros previos. Outbox=0 anterior a las pruebas no acredita aislamiento posterior.

Con catálogo permitido, progresivas/elementos ficticios únicos previamente comprobados, capturados DESPUÉS del corte: dos alcantarillas en una carretera para SIC-18/SIC-18A, una SCAP con fotos/croquis y una muestra Pavimentos. Procesar una ficha por vez. Vídeo únicamente si la funcionalidad efectiva lo admite y se incluye expresamente; de lo contrario queda sin GO de vídeo. No introducir carreteras arbitrarias ni confundir etiqueta PRUEBA con prueba de inexistencia.

Antes de escribir: inventariar destinos y consolidados, conservar copia/IDs/hash/filas de cualquier consolidado existente. Si no puede protegerse, detener. Verificar tras cada entrega: ACK completo/persistido con UUID/key/revisión/contentHash y archivos; SHA256 y tamaño de bytes descargados; nombres/rutas; carpeta bajo destino institucional; propietario efectivo GBO de TODOS los nuevos archivos (no solo carpeta); filas previas y ambas filas SIC-18/18A, sin duplicados ni cambio ajeno. Un único replay de la misma revisión/bytes por entrega debe devolver mismo ACK/IDs sin duplicación. Sin ACK o dueño GBO: NO-GO. No borrar datos de prueba después.

OPEN no limita por sí solo las cargas a ese teléfono. Si no se acredita pausa de los demás clientes, el ensayo conectado permanece bloqueado aunque se autorice la publicación DRAIN_ONLY.

## Evidencia, checkpoints y aprobación

117/117 Android y 76/76 backend ya acreditados, no repetidos. Aceptación física APK44 pendiente; no se generó otra APK. Fuente/estado recuperables en PR11 sin merge. Evidencias, manifest completo, propiedades, IDs/UUID reales y plan individual de 13 bindings solo en almacenamiento privado ASUS. Referencia: docs/PROGRESO_ACUMULADO_GBO.md.

La autorización solicitada es UNA operación escalonada condicionada: fuente/V22/deployment original + propiedades enumeradas + verificación DRAIN_ONLY/cache; OPEN y cuatro fichas/replays únicamente si aceptación/identidad/permisos/aislamiento se acreditan. Si falta una condición, detener solo la fase dependiente. Excluye distribución general, permisos, ROOT literal, Users, credenciales, migración/eliminación y Xiaomi principal.
