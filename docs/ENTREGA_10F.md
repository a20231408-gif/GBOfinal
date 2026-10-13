# Entrega 10F — videos SCAP y distribución controlada

Base aprobada: `0f1f5b1f2226808743caa301c883de6e3e3f6ff3` (10E). Continuación: `codex/cierre-videos-scap-10F`.

## Alcance y cambios

Se revisó únicamente el cliente de videos y la compatibilidad de distribución. No se modificaron fotografías V2, formularios SIC/SCAP, Pavimentos, orientación, cálculos, numeración ni sincronización fotográfica. No se cambian Room, preferencias, firma, versionCode ni versionName. No hay despliegue Apps Script, instalación en tablet ni Release nueva.

Dos defectos de videos quedaron reproducidos por pruebas antes de corregirse:

- VideoHttp aceptaba una respuesta HTTP 200 con `ok=false`, `videoProtocol=1` y `videoScope=SCAP_ONLY`. Ahora exige también `ok=true`.
- VideoSync guardaba `Exception.message` en Room y la pantalla lo mostraba. Una excepción de red puede incluir la URL privada de la sesión reanudable. Ahora las excepciones guardan mensajes públicos fijos, sin URL ni contenido arbitrario de la respuesta. El original, identidad y cola permanecen conservados.

Cambios de implementación: únicamente `media/VideoSync.kt`; se permite inyectar endpoint/token ficticios a las pruebas manteniendo los valores normales de DriveConfig. Se agrega `VideoClosure10FTest.kt`, este informe, una matriz y evidencia. No se reescribió el cliente ni se creó una APK/variante especial.

## Flujo existente revisado

| Etapa | Comprobación / límite |
|---|---|
| Captura | CaptureVideo de Android hacia FileProvider privado en `videos/capture`; cancelación no registra una entrega. Cámara/codec reales pendientes de tablet. |
| Importación | SAF permite varios videos; copia a archivos privados, valida pista de video, MIME MP4/WebM/3GP y máximo 32 MiB. Prueba local de importación usa inspector simulado; el codec real se comprueba físicamente. |
| Persistencia y asociación | Base aditiva `field_video_9.db`, UUID de media, parentKey SCAP, categoría/etiqueta, hash y original. Reapertura offline conserva datos. No modifica inventario Room. |
| Vista y reproducción | Panel por categoría, contador separado, miniatura con MediaMetadataRetriever y reproducción local con VideoView/MediaController. La aceptación audiovisual real se realiza en tablet. |
| Reanudación | Sesión HTTPS limitada a www.googleapis.com, consulta offset, chunks de 512 KiB y máximo de 32 MiB. Recupera respuesta perdida y sesión caducada manteniendo mediaId/fileId. |
| ACK | Solo SYNCED con ok, complete, videoProtocol, mediaId, parentKey, tamaño, SHA-256 y fileId correctos. Requiere ficha padre previamente confirmada. |
| Duplicados | Reintentos del mismo mediaId conservan fileId y no duplican bytes ya recibidos. El backend reserva ID antes de crear el archivo. Importar intencionadamente dos veces el mismo archivo crea dos evidencias con UUID diferentes; no existe deduplicación global por contenido. |
| Retiro | Original e historial se conservan; el worker no resucita un video retirado. No se borra automáticamente el archivo remoto. |

La verificación de reanudación HTTP usa el transporte real OkHttp con todos los intercambios interceptados: pérdida de respuesta de chunk y de ACK final, consulta de offset, misma identidad/categoría/fileId y ACK completo posterior. No escribe en servicios reales. El adaptador Apps Script se prueba en Node con servicios simulados y sin editarlo.

## Capacidad real y activación coordinada

El GET real de esta ejecución anuncia **videoProtocol=0, videoScope=SCAP_ONLY**. La configuración ordinaria conserva **GBO_VIDEO_REMOTE_ENABLED=false**, fotografías V2=true y ACCESS_CONTROL_ENABLED=false. Tener el cliente dentro de la APK no implica tener videos remotos habilitados.

Para activar en una etapa autorizada:

1. Guardar el valor previo de la propiedad de producción `GBO_VIDEO_V1_ENABLED`. El propietario debe comprobar que el despliegue publicado ya contiene Code.gs, VideoV1.gs, VideoV1Core.js como archivo .gs, SyncV3 y el servicio avanzado Drive. Capacidad 0 no permite distinguir si solo falta activar la propiedad o si falta un módulo; no se presume el contenido del despliegue real.
2. Cuando el propietario lo autorice, establecer `GBO_VIDEO_V1_ENABLED=true` en ese mismo Apps Script. **Este trabajo no lo hace.** Confirmar por GET `ok=true`, videoProtocol=1 y videoScope=SCAP_ONLY; mantener V3/V4 para la ficha padre. Si sigue en 0, el propietario debe revisar esas dependencias antes de habilitar el cliente.
3. Solo después, configurar privadamente `GBO_VIDEO_REMOTE_ENABLED=true` en `server.local.properties`. Seleccionar `app` / `debug` y pulsar Run normal. No hay flags de prueba ni otra APK especial. Con bandera false, la APK actual no comenzará a subir videos aunque el servidor cambie a 1; requiere esa configuración ordinaria y recompilación coordinadas.
4. Validar un Puente/SCAP ficticio con padre confirmado y videos de prueba. No usar ni modificar fichas reales de ingenieros.

Reversión: el propietario restaura el valor previo de `GBO_VIDEO_V1_ENABLED` o lo fija false y confirma videoProtocol=0. Para desactivar también el cliente, fija privadamente GBO_VIDEO_REMOTE_ENABLED=false y actualiza con Run normal usando la misma firma. Las colas y originales permanecen; no retirar evidencias, borrar sesiones, desinstalar ni vaciar bases. Una transferencia de chunks ya iniciada puede alcanzar Drive tras apagar la propiedad; el gate se comprueba al iniciar cada intento, y la confirmación seguirá rechazada hasta reactivación. No prometer cancelación remota instantánea ni borrado de un binario parcial.

## Prueba física concreta

Con la configuración actual, probar únicamente lo local en un **Puente SCAP ficticio autorizado**:

1. Antes y después de actualizar con Run, anotar código de tablet y conteo de fichas pendientes. Mantener el mismo usuario Android y la misma clave de firma. Si aparece incompatibilidad, detenerse sin desinstalar/borrar datos.
2. Sin conexión, grabar un video corto e importar otro MP4 pequeño en una categoría concreta del puente. Verificar que no aparecen en SIC ni Pavimentos, reproducir ambos y comprobar imagen/audio y categoría.
3. Cerrar y volver a abrir la aplicación. Comprobar el identificador abreviado de cada tarjeta, categoría, originales reproducibles y cola. Ahora deberán permanecer pendientes del backend; **no deben figurar sincronizados**.
4. Tras la activación coordinada anterior, confirmar la ficha padre. Reintentar videos, interrumpir conexión durante un envío y restaurarla. Verificar un archivo remoto por esa evidencia en la carpeta VIDEOS/categoría de ese Puente ficticio y estado “Sincronizado · confirmado por servidor”. Conservar el enlace/ID del archivo Drive para comparar tras reintentar; la pantalla muestra identificador abreviado, no el UUID completo ni fileId. La asociación completa mediaId/fileId y exigencia del ACK se acreditan en las pruebas técnicas focalizadas.
5. Volver a reintentar/reabrir y comprobar que la evidencia confirmada no se duplica. Retirar únicamente un video ficticio para verificar conservación del original/historial; no eliminar contenido existente de Drive.

## Compatibilidad y versión futura

Un solo módulo app contiene SIC, SCAP, Pavimentos, fotografías V2 y cliente de videos. Debug normal usa los archivos privados existentes. Credenciales/configuraciones/keystores/APK permanecen fuera de Git; no se publican PIN, token ni URL de sesión. La APK necesariamente contiene la configuración de conexión: distribuirla exclusivamente al grupo controlado previsto.

Se mantienen applicationId `com.tuempresa.inventariovial`, versionCode **16** y versionName **1.4.2**. Candidato para una futura actualización autorizada: **17 / 1.4.3**, condicionado a comprobar que 17 supera el mayor versionCode ya distribuido. No se aplica todavía. Antes de distribuir Release, ejecutar la validación de configuración/firma histórica, aprobar la prueba física y decidir el control de acceso.

Debug usa el certificado documentado en 10E, distinto del Release histórico de `app/release/`. No puede actualizar una instalación firmada con ese Release. No hay tablet conectada por ADB y no se certifica la firma instalada actualmente. El directorio Release preexistente permanece intacto y fuera del commit.

ACCESS_CONTROL_ENABLED permanece **false**. La autorización de tablet por defecto es LocalDeviceAuthorizationProvider (demostración); ServerDeviceAuthorizationProvider es un stub Unavailable. No se ha validado una autorización efectiva de tablet contra producción. Activar simplemente esa bandera no integra ese servicio y puede bloquear instalaciones; queda pendiente para distribución protegida. No se modificó unilateralmente la configuración.

## Resultado de cierre

**20/20 pruebas focalizadas aprobadas:** 17 Android/JUnit y 3 del adaptador Node. Se ejecutaron 10 métodos de videos de VideoContinuation9Test (se excluyó el método fotográfico), 3 de VideoClosure10FTest, 3 de DeviceFingerprintTest y únicamente el método de recreación de identidad/Room de PhotoNamesIntegration10ETest. Las dos reproducciones iniciales fallidas quedan registradas y corregidas; no se repitieron baterías aprobadas de fotografía/formularios.

**Debug ordinaria compilada correctamente** con los servicios privados existentes. Archivo: `app/build/outputs/apk/debug/app-debug.apk`. SHA-256: `7e04f4450ed8f8ccd1c8b7c04c316bb955278a9032749aa6c8f51268d763dcdb`. Certificado Debug verificado: `d2e54cb086aa731ef046be300977d0b16c0d8f66fdcb08d0d9cfd2038bf0d872`, igual a 10E. Los módulos SIC/SCAP/Pavimentos/V2/VideoHttp/VideoSync/VideoWorker están presentes en el DEX de la única APK ordinaria. BuildConfig compilado acredita V2=true, videos remotos=false y control de acceso=false, sin exportar valores privados.

Para una futura Release autorizada, la tarea existente `:app:validateReleaseConfiguration` comprueba servicios, clave privada y certificado histórico esperado antes de `preReleaseBuild`, sin fallback Debug. No se ejecutó compilación Release ni se abrió/modificó su keystore en 10F; la validación final de esa clave queda pendiente para dicha etapa. La inspección de la APK Release preexistente confirma el certificado histórico `0da8a2cbcf6bce5554526c80de08878d077af1542e488292b3869cdd8ee17315`.

Resultados y bloqueos finales: `MATRIZ_10F.md` y `campo-10f/evidencia/`. La revisión de privacidad cubre archivos de texto rastreados en HEAD y las adiciones 10F, sin revisar todo el historial Git. Las pruebas simuladas preparan la validación; no equivalen a aceptación física ni a aprobación general de distribución de producción.
