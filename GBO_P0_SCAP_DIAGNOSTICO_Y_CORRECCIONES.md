# GBO P0 — Puentes/SCAP: diagnóstico y correcciones

30/10/2026, Lima. Base local `cfb4951`, rama `codex/observaciones-campo-asus-20261030`. Fuentes 1.4.18 (32), Room 21. Se leyeron el consolidado y los dos informes originales de Work. Sus commits y archivos permanecen conservados. No se cambió versión, dependencias, firma, raíz, cuenta Google, configuración privada ni producción.

**No se declara corregida la entrega de Flavio.** No se dispone de evidencia comparada de las tablets ni de una comprobación real de ACK y archivos en el destino esperado. Los resultados siguientes son locales.

## 1. Causas confirmadas y recorrido inspeccionado

### Persistencia, cola y confirmación

1. `ScapController` conserva ediciones con journal y las escribe con un ejecutor IO. El cierre en `ScapRepository.complete` valida datos/consentimiento, persiste COMPLETE y solicita revisión de entrega dentro de una transacción Room.
2. `AutomaticSync.request` conserva identidad e historial anterior. Solo dispositivo no genera nueva entrega. El señalizador agrupa solicitudes; el drenaje se agrega con APPEND_OR_REPLACE, sin cancelar la carga activa. WorkManager exige conexión, usa backoff exponencial y dispone de recuperación periódica de 15 minutos.
3. `AutomaticSyncDrain` recupera estados demostrablemente reparables, comprueba configuración, consulta capacidades y prepara la revisión. V21 debe acreditar protocolo 3, rutas V4, numeración estable y categorías. SCAP oficial requiere nombres V2 y panel compatible. Un nuevo drenaje negocia otra vez.
4. La revisión preparada y los stages verificados se conservan ante respuesta perdida. Los antecesores no confirmados se reejecutan con su snapshot exacto; no se cambia su base ni se inventa confirmación. BUSY conserva datos y detiene el recorrido de la cola para no insistir con todas las fichas contra el mismo bloqueo.
5. Solo un ACK íntegro con identidad/revisión/hash/ruta y todos los archivos y aportes SIC esperados permite confirmar. Un stage, una reserva, 100% de archivos procesados o un ACK anterior no acreditan entrega actual. Los videos tienen confirmación separada.

### Defectos locales confirmados y corregidos

- Un ERROR inactivo sin reintento programado se clasificaba como VERIFYING. Ahora pide atención y ofrece reintentar/asistencia; una revisión UPLOADING interrumpida o un supuesto SYNCED sin ACK válido sigue pendiente de verificación.
- PREPARING activo se agrupaba bajo el estado general de subida. Ahora tiene estado visible Preparando entrega y mensaje Preparando archivos; no se presenta como transferencia terminada.
- Los contadores comprobables de staging estaban dentro de detalles técnicos de la tarjeta. Ahora pueden mostrarse directamente mientras el trabajador y la revisión coinciden. No se muestran como ACK ni se usa progreso de otra revisión o de un trabajador detenido.
- El aviso «panel oficial SCAP requiere nombres fotográficos V2» no tenía explicación específica. Ahora indica revisar versión/configuración legítima de la instalación y conservar los datos. No se atribuye automáticamente al servidor ni a permisos.
- HTTP 401/403, tanto al consultar capacidades como al enviar, se envolvían como fallo de conexión y podían reintentarse como transitorios. Ahora generan rechazo explícito HTTP_401/HTTP_403 no transitorio; el drenaje existente lo clasifica como BLOCKED y la UI solicita revisar acceso/configuración. No se cambió autenticación, tokens ni permisos. Un 503 sigue siendo fallo recuperable de comunicación. Esto prueba una clasificación incorrecta del cliente, no demuestra que las tablets hayan recibido esos códigos.

### Carpetas PUENTE 1, 2, 3

Origen confirmado en la referencia local V21 `backend/referencia-v21/SyncV3.gs`, función elementLocation: el nombre se construye como `p.family + ' ' + sequence`; para SCAP la familia es PUENTE. El binding durable conserva UUID/número/folderId/logicalPath. Otros contratos históricos también validan la forma PUENTE n.

El nombre real se conserva en `ScapInspectionEntity.bridgeName`, asociado a la edición `singularityName`; `bridgeCode` es independiente. Android ya usa esos campos en el título legible mediante `PublicLocalFolderNames` y `DriveDeliveryPresentation.identity`. La prueba existente de nombres reales pasa sin generar nuevas carpetas ni reservas. El payload V4 de entrega no establece bridgeName como autoridad del nombre remoto; añadirlo unilateralmente no cambiaría elementLocation.

No se renombraron carpetas ni se alteró el payload congelado. Incorporar nombres reales al nombre remoto requiere diseño versionado y autorización de servidor/Drive, preservando bindings, rutas históricas, reservas y ACK. La referencia local no acredita que el despliegue remoto actual sea idéntico; no se consultó producción.

### SIC asociados

`AutomaticDelivery` incorpora a sicRows solo previews SIC-17/17A/17B completos. Los incompletos se describen en pendingSic; no se inventan filas. V21 consolida esos aportes en `<RUTA>/EXCEL_SIC`, separados de los archivos del padre SCAP. `sicFiles` en el ACK acredita la consolidación; `retainedSicFormats` puede conservar aportes previos.

Se añadió aviso visible de cantidad de aportes SIC pendientes cuando corresponde a la revisión local. Un respaldo SCAP confirmado no se presenta como completitud contractual de esos pendientes. No se añadió generación ficticia ni se afirmó que todos los SIC deban aparecer como archivos individuales en la carpeta PUENTE n. Para cada ficha reportada hace falta comparar preview, pendingSic, sicRows, sicFiles y ruta real antes de clasificarla como omitida.

## 2. Hipótesis aún no verificadas y bloqueos

No se conocen las versiones instaladas, flags de compilación, capacidades observadas, restricciones de batería/red, sesión, autorizaciones, estado de cola ni recibos de ambas tablets. Una diferencia en cualquiera de esos puntos podría explicar resultados distintos, pero no está demostrada.

En ASUS faltan `server.local.properties`, `drive.local.properties` y `keystore.properties`, conforme a la auditoría precedente. No se restauraron ni inventaron. El flag V2 queda false por defecto; esto explica los bloqueos de los ensayos locales, no acredita la configuración de la tablet de Flavio. Debug no habilita servicios reales ni sustituye Release histórica.

No se dispone de tiempos remotos independientes ni trazas de las entregas reportadas. No se atribuyen demoras a Apps Script, ScriptLock, Google, permisos o tamaño de fotografías sin medición. Los límites conocidos de V21 siguen siendo 80 archivos, 24 MiB por staging y 160 MiB por revisión; no se alteraron.

## 3. Cambios implementados y contadores

La intervención se limita a clasificación HTTP y presentación de estados/progreso/pendientes. No modifica la generación de revisiones, el guardado, los reintentos de ACK perdido, la validez del ACK, las reservas ni los contratos de archivos.

Estados visibles: guardado local, espera de conexión/turno, preparación, transferencia de archivos con cantidad real disponible, verificación final, confirmado y atención por error. No hay estimación de tiempo restante ni porcentaje de una entrega completa. El porcentaje existente se identifica exclusivamente como staging de archivos.

Los ámbitos no eran iguales:

| Pantalla | Ámbito y criterio |
|---|---|
| DashboardDeliveryCounts | Cola registrada, excluye DRAFT; usa el recibo básico de SyncIntentPresentation, sin cargar todos los archivos ni la revisión local de cada ficha |
| AutomaticSyncSummary | Resumen de cola y trabajador; no equivale a auditoría de archivos actuales |
| Entregas Drive / Mis entregas | Todas las entradas de la cola, incluidos borradores; consulta identidad local y exige ACK completo de la revisión vigente |
| Mis registros | Inventario local, incluidos registros sin outbox; su filtro Enviados usa confirmación estricta |

Se añadieron textos explicativos en el dashboard, resumen y diálogo. No se cambiaron silenciosamente sus ámbitos ni se fabricó igualdad entre conteos de fichas, archivos y aportes SIC. Unificarlos totalmente requeriría una proyección común con revisiones locales, fuera del ajuste mínimo de esta intervención.

### Instrumentación de rendimiento

Se inspeccionó y reutilizó `SyncPerformance`, ya conectado al drenaje y al transporte: PREPARE/RESERVE/CAPABILITIES, PHOTO_PREPARE/FILE_PREPARE, PHOTO_SEND/FILE_SEND, espera de stage/commit y comprobación ACK. Es numérica, acotada y con listas permitidas; no acepta UUID, nombres, rutas, URLs, tokens ni texto arbitrario de errores.

PREPARE mide trabajo local; SEND mide escritura de petición; WAIT_ACK y HTTP incluyen red y servidor. Las fases se solapan y no se suman para deducir duración total ni tiempo de bloqueo remoto. El tiempo desde el primer intento incluye espera/offline, y no se etiqueta como subida efectiva. No se midieron entregas Google ni se instrumentó el servidor. No se introdujeron compresión destructiva, paralelismo ni otro protocolo. La prueba de límites/privacidad de métricas y el preview local aprobaron; el ensayo de carga a dos bases simuladas requiere el perfil V2 legítimo y sigue bloqueado.

## 4. Archivos modificados

- `app/src/main/java/com/tuempresa/inventariovial/core/AutomaticDelivery.kt`: diferenciar HTTP 401/403 de comunicación transitoria.
- `app/src/main/java/com/tuempresa/inventariovial/core/DriveDeliveryPresentation.kt`: preparación, error inactivo, causas V2/autorización y progreso visible condicionado.
- `app/src/main/java/com/tuempresa/inventariovial/core/DriveDeliveryCard.kt`: progreso, guardado local y aviso SIC pendiente.
- `app/src/main/java/com/tuempresa/inventariovial/core/AutomaticSyncPanel.kt`: explicación del resumen y preparación local.
- `app/src/main/java/com/tuempresa/inventariovial/core/DriveSyncCenter.kt`: ámbito del diálogo.
- `app/src/main/java/com/tuempresa/inventariovial/dashboard/DashboardScreen.kt`: ámbito del dashboard.
- `app/src/test/java/com/tuempresa/inventariovial/DriveDeliveryPresentationTest.kt`: tres nuevas regresiones de estado, progreso y perfiles simulados diferentes.
- `app/src/test/java/com/tuempresa/inventariovial/DriveDeliveryCardUiTest.kt`: visibilidad del staging final sin afirmar ACK.
- `app/src/test/java/com/tuempresa/inventariovial/ScapHttpAuthorizationP0Test.kt`: dos pruebas nuevas, HTTP simulados sin red.
- Este informe. No se modificaron archivos privados, backend ni el trabajo consolidado de filtros/descarte/longitud.

Las diez imágenes PNG no rastreadas encontradas al iniciar se conservaron; no se incorporaron a la intervención ni a respaldos versionables. No se hizo commit ni push en esta tarea. Evidencias y parche local quedan bajo `tmp/p0-scap-20261030/`, ignorado por Git.

## 5. Resultados de pruebas

Primera pasada: 79 casos, 73 aprobados y 6 fallidos. Se conservaron XML/log. Incluía los tres bloqueos anteriores, el ensayo de carga V2, un fallo SQLite al abrir base con ruta temporal larga y una incompatibilidad del texto Preparando archivos introducida en la presentación. Se restauró ese texto del producto sin alterar su prueba ni relajar condiciones; se acortó únicamente la ruta temporal del ejecutor.

Pasada ampliada posterior: **81 casos, 77 aprobados, 4 fallidos, cero errores y omitidas**. Pasan presentación (11), tarjeta UI (3), listas (6), listas UI (3), cola (21), nombres (4), SCAP Drive (5), transporte fotográfico (6), autorización HTTP (2), dos ensayos de rendimiento y las regresiones SCAP independientes del perfil. SQLite/reapertura y texto de preparación pasan. XML en `xml-ampliada/`, resumen en `resumen-ampliada.json` y log en `pruebas-final.log`.

Verificación final: **assembleDebug PASS y 23/23 pruebas PASS**, cero fallos/errores/omitidas, en 2 min 6 s para la operación conjunta con compilación. Incluye presentación (11), tarjeta UI (4), autorización HTTP (2) y dashboard UI (6). La nueva prueba visual verifica que 16/16 procesados es visible sin abrir detalles y no se convierte en 16/16 confirmados. Evidencias en `verificacion-final.log` y `xml-verificacion-final/`.

Consolidado por clase/método, sin contar repeticiones: **88 casos distintos, 84 aprobados y 4 fallidos**; `casos-consolidados.json` conserva el resultado más reciente de cada caso. No se ejecutaron pruebas contra Google ni en dispositivos reales. No se desactivaron flags ni se modificaron aserciones históricas para obtener PASS.

APK Debug local nueva: SHA-256 `4784af2509eca412391d8b3d247d80dfd7c79d0b56366728b12fa54bfcb66c08`, mantenida fuera de Git, sin instalación ni distribución. La candidata Debug previa y su hash constan en la auditoría anterior; no es una Release ni una candidata autorizada para campo.

## 6. Fallos pendientes

1. `PavementAnnotationSync147Test.rigidAnnotationsAndPhotoSurviveOfflineBusyLostAckAndAuthorizedUpdate`: exige V2 habilitado.
2. `ScapFinalP0Test.actualPreparationFailureIsActionableAndDoesNotLoseTheSnapshot`: esperaba BLOCKED en reserva V2; obtiene UNCONFIGURED con el perfil ausente.
3. `SyncRecovery10DTest.scapV20BlockRecoversWithFreshV21CapabilitiesAndLostAckNeverRestagesSixteenFiles`: PENDING_VALIDATION por panel oficial que exige nombres V2; no llega al ACK perdido esperado.
4. `HotfixPerformance146Test.load`: exige configuración V2 ordinaria. El ensayo integral de dos bases/tablets simuladas no se declara aprobado.

Los cinco fallos históricos de AutomaticDocuments149Test/Engineering2DataTest no fueron reejecutados. No hay PASS de suite completa. La nueva prueba de dos instalaciones compara proyecciones puras de un perfil bloqueado frente a un ACK completo; no sustituye el ensayo integral de dos tablets ni modifica BuildConfig.

## 7. Validación en tablet con datos ficticios

Sin instalación automática y antes de autorizar escrituras:

1. Registrar por canal privado versión/build de ambas tablets, estado de destino, batería/ahorro, permisos pertinentes y red. No exportar claves, tokens, endpoints, fotos ni bases reales. Conservar cantidades y evidencias locales; no desinstalar ni limpiar datos.
2. Crear una ficha ficticia identificada como prueba en Solo dispositivo. Guardar/cerrar, volver a abrir y comprobar nombre real, fotos originales y exportaciones. Solo dispositivo debe seguir sin envío.
3. Comprobar preparación, documento SCAP y cada SIC esperado: completos frente a provisionales/pendingSic. Abrir Excel/PDF en el dispositivo; no rellenar campos reales por conveniencia.
4. Para la comparación de estados, recoger diagnóstico agregado de cola/métricas y motivo literal saneado. Comparar V2/panel/capacidades sin asumir que el login de Usuarios concede acceso a Drive; son configuraciones distintas.

Solo después de autorización independiente de instalación y de un destino de PRUEBAS:

5. Instalar la candidata con identidad/firma compatible, sin sustituir Release por Debug ni cambiar la tablet de campo a otra raíz. Autorizar explícitamente una ficha ficticia hacia el destino aislado.
6. Con varias fotografías y SIC válidos, probar offline→online, BUSY, pérdida de respuesta y reintento. Confirmar mismo UUID/revisión/hash/binding y que no se duplican archivos; documentar fases sin porcentajes inventados. No provocar fallos sobre V21 productivo.
7. Verificar 16/16 procesados como staging: mientras falta ACK debe decir verificando, no entregado. Verificar rechazo de autorización como atención, sin eliminar la ficha.
8. Exigir ACK íntegro de revisión vigente y contrastar todos los archivos en la carpeta acreditada, más sicFiles en EXCEL_SIC y videos por separado. Una edición posterior no debe reutilizar la confirmación anterior.
9. Repetir con ambas tablets bajo condiciones comparables. Solo entonces evaluar si el problema reportado está resuelto; conservar los fallos y sus diferencias.

## 8. Autorizaciones necesarias

Recuperar configuración privada/firma por el canal seguro ya documentado; validar Release sin bypass. No se solicitó ni mostró ninguna credencial. Una Debug local no acredita conexión ni autoriza distribuir cambios con la numeración histórica.

Requieren autorización separada: cualquier consulta privada a las tablets o a Google, subidas reales o de prueba, instalación/distribución, renombrado/migración de carpetas, cambio de cuenta/raíz/propiedades/permisos, intervención en Usuarios y despliegue de Apps Script. Antes de modificar servidor se necesita snapshot autorizado del despliegue vigente, inventario de bindings/ACK y diseño compatible de nombres. No publicar referencias saneadas ni propuestas como sustituto de V21.

Próximo paso operativo: recuperar configuración legítima, repetir los cuatro ensayos bloqueados y obtener evidencia saneada de ambas tablets. Las correcciones locales aquí probadas no permiten afirmar una entrega real resuelta.
