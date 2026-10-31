# Candidato local para GBO_Inventario original — NO DESPLEGADO

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
