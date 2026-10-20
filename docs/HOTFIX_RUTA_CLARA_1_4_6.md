# GBO Ruta Clara 1.4.6 (20) — integridad y sincronización multitablet

Fecha: 15/10/2026. Proyecto `D:\Downloads\GBO_PRUEBAS_8B`. Rama `codex/observaciones-campo-manual-10I`, base `5de1242581dc460ea38d45006020d022f8d4da78`. Trabajo local; sin commit, push, instalación ni distribución. **No apta para distribución general** hasta cerrar la incidencia operativa de entregas demoradas. Ampliación de rendimiento: `docs/RENDIMIENTO_SINCRONIZACION_1_4_6.md`.

## 1. Causas confirmadas

**UNIQUE SIC-22, local:** `saveSic22()` conservaba el UUID de un borrador mediante `insertOrCompleteDraft()`, pero después ejecutaba `insertSic22()` con ABORT aun cuando sus detalles ya existían. La PK es `recordId`. La transacción abortaba y no completaba la ficha. El patrón estaba también en SIC-17/18/19/20/21/23 y en el checkpoint anterior al rediseño. No es un problema del usuario GBO ni de Drive. La reinserción ABORT de fotos existentes era otra colisión posible después de resolver la de detalles.

**Supuesto Badén, local:** `InventoryWorkspace.onResumeDraft` reconocía SIC-21, pero no SIC-22, y enviaba los demás casos al fallback FORD. Eso abre el formulario incorrecto. Además, el guardado del borrador antes de este hotfix podía actualizar `sicCode`/`assetType` del mismo UUID; por ello no puede afirmarse que siempre fuese solo un rótulo ni que necesariamente se creara una segunda ficha.

**BUSY, servidor/cliente:** las fuentes locales usan ScriptLock global y `tryLock(30000)` para commit, staging y nombres V2. Un rechazo BUSY significa que esa operación no consiguió ese lock. El cliente ya tenía backoff exponencial y conservaba snapshots; mostraba un mensaje genérico de rechazo y dependía del indicador `retryable`. El encolado con APPEND_OR_REPLACE podía acumular trabajos al pulsar varias veces. No se ha medido qué operación concreta ocupó el lock de producción ni se ha atribuido a compartir usuario. Quince pendientes no demuestran pérdida ni equivalen a quince fallos definitivos: hay que consultar cada estado/ACK.

## 2. Separación Android y Apps Script

Android corrige claves, navegación, protección de identidad, presentación y reintento/coalescencia. El backend local mantiene su exclusión global para proteger recursos compartidos. No se modificaron las fuentes productivas ni se accedió al servidor publicado. **Paridad entre publicado y local: no verificada**, por la restricción de acceso a producción. Las capacidades anunciadas anteriormente no acreditan igualdad de fuentes.

Hay una propuesta completa en `docs/hotfix-1.4.6/backend-propuesto`, separada de 10H/10K: medición de locks y validación pura de staging antes del lock. No se recomienda desplegarla hasta contrastar la versión publicada y medirla con autorización. No cambia el motor transaccional ni promete resolver toda la saturación.

## 3. Señal original y fotografías

No se ha consultado la base ni el almacenamiento de los dispositivos de campo. Estado real del UUID, bytes originales, fotos, outbox y ACK: **pendiente de inspección autorizada**. Ninguna ficha real se ha borrado, reparado o reenviado en esta tarea. Las pruebas usan bases y fotos ficticias.

El error de PK por sí solo aborta la transacción de esa finalización; no demuestra pérdida de los datos anteriores. Los originales y preparaciones deben respaldarse y verificarse en el dispositivo afectado antes de recuperar la ficha.

## 4. Cómo distinguir el supuesto Badén

Comparar, de manera consultiva sobre una copia consistente: `inventory_records.id/sicCode/assetType/status/updatedAt`, detalles `sic22_details` y `sic20_details`, fotos por `recordId`, JSON privado `inventory_drafts/<UUID>.json`, outbox `SIC/<UUID>`, ACK y binding remoto si existe.

- Mismo UUID, padre SIC-22 y detalles SIC-22: navegación/presentación incorrecta.
- Mismo UUID, padre SIC-20 con detalles o JSON SIC-22: posible reclasificación local previa; conservar ambas evidencias y conciliar.
- UUID distintos: comprobar sus relaciones y revisiones antes de concluir que hay un duplicado.

El hotfix bloquea detalles contradictorios en vez de convertir o eliminar automáticamente una ficha histórica. No migra ni reclasifica datos reales.

## 5. Correcciones implementadas

- Detalles existentes SIC-17 a SIC-23: actualización dentro de la misma transacción; inserción ABORT solo para detalles nuevos. No se usa REPLACE, no se borra el padre ni se activan cascadas.
- Fotos de finalización: conserva UUID, índice, fechas y referencias/ACK remotos de las existentes; añade las nuevas con índices libres. La retirada/reemplazo explícitos siguen en el editor autorizado.
- Guarda UUID/tipo SIC/subtipo del registro recuperado. Rechaza un detalle incompatible antes de guardar datos o fotos. Borradores de otro tipo no sobrescriben el JSON guardado. Detecta detalles históricos contradictorios y pide revisión.
- SIC-22 continúa como señal vertical; SIC-21 distingue marcas, tachas y seguridad; SIC-20 distingue badén, muro y túnel. Desconocidos muestran error conservando evidencia.
- BUSY es transitorio incluso si falta `retryable`; conserva revisión, preparación y ACK anterior. Un bloqueo histórico que diga `(BUSY)` y tenga revisión válida pendiente se recupera sin regenerar su identidad ni preparación.
- Una petición repetida de la misma revisión no genera otra revisión. WorkManager usa trabajo único KEEP, restricciones de red, backoff exponencial de 30 s y dispersión de 1–5 s. Tras el límite de reintentos inmediatos queda la recuperación periódica de 15 minutos. Android puede diferir su ejecución. No se cancela una subida en curso ni se borran los trabajos anteriores ya encolados.
- La comprobación manual V2 y su texto de compilación salen del panel habitual compartido por SIC/SCAP/Pavimentos. Se conserva la herramienta en el diagnóstico de estabilidad ya existente. No se desactiva V2, la reserva, el control de compatibilidad ni el ACK.
- Mensajes del operador: «Subiendo fotografías», «Esperando para reintentar» y «Entrega confirmada». Los conflictos reales siguen exigiendo conciliación; no se reenvían como si fueran BUSY.
- Rendimiento: reserva V2 ligera para SIC/Pavimentos, capacidades negociadas por vaciado, pausa del lote ante pérdida de comunicación/BUSY global, programación de fichas independientes aunque otra esté en conflicto, contador de ACK coherente y diagnóstico agregado de estados/archivos/tiempos. Detalles y límites en el informe adicional.

## 6. Pruebas de concurrencia y regresión

Alcance: Room ficticio, fotografías sintéticas, transporte simulado, Drive/LockService simulados. No se repite la auditoría general 1.4.5 ni se prueba con datos de ingenieros.

Los tests Node `Hotfix146.test.cjs` y `HotfixLoad146.test.cjs` verifican dos UUID/operadores, BUSY intercalado, reservas V2, filas en el mismo consolidado y orden por progresiva, staging/commit idempotentes, SCAP y Pavimentos, y la propuesta sin debilitar el motor. **9/9 aprobados**, incluidas dos cargas de 17 SIC + 11 muestras: 28/28 ACK, sin duplicación de IDs. Son simulaciones reproducibles; no son dos tablets reales conectadas ni una medición del rendimiento de Google.

Android: comprobadas reapertura/finalización SIC-22, fotos, edición autorizada, revisión rechazada y posterior edición, ACK anterior, Room independiente por dispositivo, navegación real Compose, ocultación de controles técnicos, orden y migraciones. Pasaron los cinco casos ordinarios V2. La carga HTTP interceptada confirmó 28/28 en 63,469 s, con 78 archivos únicos, 79 llamadas staging, 29 commits y 8 GET de capacidades para 8 vaciados (incluidos cortes y BUSY). Se preservaron los originales.

La carga de Pavimentos usa el renderizador real sobre bitmaps y un wrapper PDF raster **solo de pruebas**: el runtime Robolectric devuelve handle nativo 0 en un PdfDocument recién creado. El probe sin ese wrapper confirmó la limitación del entorno; **PDF nativo en dispositivo no verificado**. El exportador de producción sigue intacto. No se declara cerrado el caso real 1/11 con esta simulación.

Se conserva evidencia de fallos iniciales: tres casos nuevos fallaron por comparar Long con un cero Int en el doble del servidor (confirmado por bytecode; corregido a 0L). Dos casos de Pavimentos fallaron por el runtime PDF sin implementación. Se repitieron los casos afectados; los fallos no se ocultaron cambiando producción ni eliminando pruebas.

Balance por fases: **53 comprobaciones Android distintas con último resultado aprobado, sin casos omitidos**. De ellas, 52 comprueban comportamiento y una es la sonda de disponibilidad del PDF del runtime, que confirma una limitación y no una aceptación nativa. Integridad/BUSY/UI/recuperación: 24; orden, ediciones relacionadas y migraciones: 20; V2 ordinario: 5; rendimiento/diagnóstico: 3; sonda PDF: 1. Los casos de carga/preview de Pavimentos tienen la limitación del wrapper descrita. CSV, XML por fase, cinco eventos de fallo inicial y logs están en `docs/hotfix-1.4.6/evidencia`. No es una repetición de toda la batería 1.4.5.

## 7. Candidata y compatibilidad

**Compilación completada:** `assembleRelease`, `lintVitalRelease`, validación de configuración y `verifyReleaseIdentity` aprobados. APK ordinaria **1.4.6 (20)** en `output/GBO_RUTA_CLARA_HOTFIX_1.4.6_20/GBO_Ruta_Clara_1.4.6_20_CANDIDATA.apk`, 26.241.300 bytes.

- SHA-256 APK: `ae61c79fefdb6e02d9af7a6ffdf7abbaa26a710d42e2ba75fd67db8affbd4ea0`.
- Certificado histórico SHA-256: `0da8a2cbcf6bce5554526c80de08878d077af1542e488292b3869cdd8ee17315`.
- Mismo paquete `com.tuempresa.inventariovial`; firmas v2/v3, alineación 4/16 KiB, sin debuggable, testOnly ni splits. Solo cambian versionCode/versionName en el manifiesto frente a 19.
- Configuración ordinaria V2 habilitada y videos remotos conservados; las clases, el PDF raster y el token ficticio de las pruebas no están en la APK. Las configuraciones privadas conservan sus hashes.
- **Identidad del APK validada no equivale a aprobación de distribución**: la demora real y el PDF nativo físico siguen pendientes.

No se cambia Room 19, sus migraciones, claves, usuarios, token, URL, raíz ni identidad de tablet. La candidata 1.4.5 (19) mantiene su SHA-256 `916a9f7250cd301fb789ae613b5cecc9e93176c142479ec47d91d0cfa6f181d4` y sus avances se conservan. No se instaló ni distribuyó nada. El informe de firma, configuración pública, diferencias de manifiesto y mapping están en `validacion/` junto al APK.

Respaldo anterior a las ediciones: `tmp/hotfix-1.4.6-20261015/fuentes/` conserva la estructura de fuentes, además del ZIP, estado Git, hashes de configuración privada, copia del APK construido 19 y mapping. Los secretos no se copian a los parches ni al informe.

## 8. Colaboración, cuentas y capacidad de Drive

Cada dispositivo tiene Room, archivos y cola propios. Iniciar la misma cuenta GBO en dos tablets no comparte esas bases ni convierte fichas diferentes en una edición compartida. Se generan UUID; para registros nuevos distintos se conserva una identidad independiente en el servidor. El operador/dispositivo de SIC se asocian al recorrido `field_sessions` y su `sessionId`; los borradores crudos tienen propietario. Los formatos más recientes mantienen sus propios campos de responsable. No debe interpretarse que todos los formatos históricos tengan un propietario por ficha idéntico.

Dos usuarios pueden aportar fichas distintas a una carretera/consolidado. El backend ordena dentro de su ámbito de ruta/formato por calzada, sentido, PR numérico + distancia y UUID para desempatar. El número estable de carpeta no se recalcula al entrar una progresiva anterior. Dos operadores editando el mismo UUID/base pueden producir conflicto de revisión; esta arquitectura no fusiona ediciones ni descarga automáticamente una base Room compartida. Se recomiendan cuentas individuales y un responsable por ficha para trazabilidad.

«Consultar capacidad fotográfica» era una consulta del protocolo `readablePhotoNamingVersion=2` y los contratos V3/V4; **no consulta almacenamiento disponible**. Su funcionalidad se mantiene bajo «Verificar compatibilidad de fotografías con el servidor», en diagnóstico.

La identidad Google de ejecución depende del despliegue: “ejecutar como yo” ejecuta como el publicador; “como usuario que accede” usa esa identidad Google, que no debe confundirse con usuario/PIN GBO. No se ha verificado la opción del despliegue real. [Documentación oficial](https://developers.google.com/apps-script/guides/web).

Compartir una carpeta de Mi unidad desde una cuenta con más espacio no traslada por sí solo la propiedad/cuota de los archivos creados por otra cuenta: hay que comprobar quién crea y posee cada archivo y la identidad que ejecuta el script. La cuota de Drive y sus reglas de propiedad se consultan en la [ayuda oficial de almacenamiento](https://support.google.com/drive/answer/6374270?hl=en). Una unidad compartida de Workspace tiene propiedad de la organización y almacenamiento compartido, un escenario distinto que requiere validar compatibilidad y permisos del backend antes de migrar: [documentación oficial](https://developers.google.com/workspace/drive/api/guides/manage-shareddrives).

No se cambió propietario, permisos, cuenta, `/exec`, despliegue, ScriptProperties ni ROOT_FOLDER_ID. La protección ScriptLock es global al script según la [referencia oficial](https://developers.google.com/apps-script/reference/lock/lock-service), independiente del nombre de usuario GBO.

## 9. Riesgos y pendientes

Pendientes de aprobación: inspección consultiva y respaldo de las fichas afectadas, contraste de fuentes publicadas, medición real de locks, prueba física de actualización compatible y prueba autorizada de dos dispositivos con fichas ficticias. No se puede certificar el estado de las 15 entregas ni ausencia de duplicados reales sin revisarlas. La APK mejora recuperación; no garantiza que el backend deje de responder BUSY ni reduce el trabajo de consolidación de producción.

Los pendientes de videos 10H y columnas OBSERVACIONES 10K no se despliegan ni se incluyen silenciosamente en esta propuesta. Las correcciones locales de clave/identidad no dependen de activarlos.

## 10. Recuperación y aprobación segura

1. No seguir guardando la ficha sospechosa como Badén, no descartarla ni crear otra para reemplazarla. Conservar el dispositivo, sus fotos y cola.
2. Con autorización del propietario, registrar identidad de la app instalada (paquete, versión y certificado), UUID de ambas vistas, revisión local/confirmada y mensaje de Detalles Drive. No enviar PIN ni tokens. Si no hay UUID visible, obtenerlo por diagnóstico consultivo de la copia local; no desinstalar ni usar “borrar datos”.
3. Respaldar de forma consistente Room y WAL, originales, videos, borradores, staging/journals, configuración e identidad en custodia privada. Sin permisos para una copia privada de una Release no depurable, detener la extracción y acordar un método compatible; no prometer `run-as` en una Release.
4. Inspeccionar una copia para clasificar el caso según el apartado 4. Si sigue siendo SIC-22 coherente, la candidata permite continuar la misma ficha y finalizarla. Si ya hay tipos contradictorios, preparar una reparación individual revisable que conserve UUID, detalles originales, fotos, revisión y ACK; **no ejecutar SQL ni reclasificación automática** con este informe.
5. Autorizar explícitamente la actualización 20 solo si la firma instalada es compatible; una Debug con otro certificado no se actualiza con la Release histórica. Para una inspección local, activar modo avión antes de actualizar/abrir: el inicio normal programa automáticamente las entregas existentes cuando hay conexión. No desinstalar para forzar compatibilidad. Conservar la APK previa; Android no permite volver de 20 a 19 mediante actualización ordinaria, por lo que revertir después requiere otra versión compatible superior y revisión del estado.
6. Tras actualizar y con autorización para enviar esa ficha, comprobar tipo SIC-22, fotos, finalización y el mismo UUID; conectar red y esperar/reintentar BUSY. Exigir ACK con revisión, hash, carpeta, archivos/fotos y fila SIC. Un archivo generado localmente no es una entrega confirmada.
7. Probar dos fichas ficticias en dispositivos autorizados, primero misma cuenta y luego cuentas individuales, verificar consolidado con dos filas y ausencia de recursos duplicados. No usar fichas de ingenieros para pruebas de escritura.
8. Distribuir solo después de aprobación del propietario sobre esos resultados y el plan de backend que corresponda.
