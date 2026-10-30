# Reparación del ensayo BASE — helpers v2

**Prioridad: diagnosticar la corrida existente, conservarla y continuar solo si se concilia.** No iniciar otra carga grande como primer paso.

Esta entrega reemplaza las instrucciones anteriores únicamente para recuperar el ensayo incompleto. Los ocho módulos de negocio BASE y los ocho del candidato están **sin cambios**. No se modifica GBO_Inventario, su URL, APK, Servicio de Usuarios, raíz ni propiedades productivas. Codex no publicó ni ejecutó pruebas Google.

## Qué sabemos

Se localizaron y leyeron los archivos reales en la carpeta descomprimida de Descargas: 119 eventos, 59 OK, 52 BUSY y 8 fallos de transporte. Los ocho padres guardados pasan la comprobación local de ACK, canonical y fixtures. El evento 119 es el único OK sin métricas. **El ejecutor anterior no guardó el ACK de ese evento ni el error posterior; no es posible afirmar cuál aserción falló.** Ver `INFORME_DIAGNOSTICO_BASE_V20.md`.

## 1. Actualizar solo la copia de PRUEBAS (acción del propietario)

1. Detener los lanzadores. Comprobar que no haya ejecuciones activas y que `inflight=0`, como ya se observó. **No poner el contador a cero manualmente.** Si vuelve a ser positivo, detenerse y diagnosticar; no continuar con este procedimiento.
2. Conservar el ZIP anterior y los tres archivos originales de la corrida. El nuevo ejecutor los trata como entrada de solo lectura y escribe sus intentos en otra ubicación.
3. Abrir exclusivamente **GBO_Inventario_PRUEBAS_V20**. Conservar un respaldo privado de los dos helpers actuales. Copiar el contenido completo de **`BASE_V20_PRUEBAS/TestIsolation.gs` y `BASE_V20_PRUEBAS/TestHttp.gs`** sobre esos mismos dos archivos del editor.
4. **No reemplazar Codigo, SyncV3, SyncV3Core, TestVariant, appsscript.json ni ninguno de los otros módulos.** No crear archivos duplicados ni pegar las referencias `.gs.txt`.
5. Dejar `TestVariant=BASE`. **No ejecutar** `gboTestStartNewRun`, `gboTestResumePreviousBase`, limpieza, cambios de epoch, reservas, contadores o propiedades. La corrida, raíz, credencial y `GBO_TEST_RUN` se conservan.
6. El propietario actualiza **la implementación existente de PRUEBAS** a una versión que contenga los dos helpers nuevos, conservando su URL de pruebas. No abrir Administrar implementaciones del inventario productivo.
7. El GET de la copia debe anunciar `_gboTest.harnessRevision=2`, `guardPolicy=FRESH_WRITES_LOCKED_READ_CACHE_V2`, `inflight=0` y la misma corrida. El lanzador verifica runId, epoch, variante, carpeta y proyecto sin mostrarlos completos. Si sigue apareciendo el helper anterior, se detiene antes de enviar solicitudes de datos.

Los dos helpers son idénticos en las carpetas BASE y CANDIDATO. Cuando corresponda ensayar el candidato, deberá usar exactamente esta misma revisión; no se instala ahora por esta reparación.

## 2. Diagnóstico corto, sin cargas

1. Descomprimir este ZIP en **otra carpeta**, sin sobreescribir la carpeta del ensayo anterior. Abrir `ejecutor/EJECUTAR_GOOGLE.ps1` con PowerShell.
2. Elegir **D** (también es el valor predeterminado). Indicar la carpeta **original de la corrida BASE**, la que contiene juntos `resultado.json`, `eventos.jsonl` y `avance.json`. No indicar la carpeta superior ni una copia con archivos mezclados.
3. Proporcionar el ID y URL del proyecto **de pruebas existente** y su credencial exclusiva con entrada oculta. No se pide la credencial productiva. En D no es necesario localizar todavía el video.
4. El cliente verifica localmente los tres archivos, firma de fixtures, foto, payloads, UUID, revisión, hash canónico y ACK. Después realiza GET, **una inspección de recibos/journals** y otro GET para reconfirmar la barrera. No llama a v3Store, stage, commit, reservas, recuperación ni video.
5. La inspección mantiene UserLock del propietario y exige cero solicitudes activas. No incrementa/reinicia `inflight`. Verifica los manifests, journals COMPLETE, carpeta, filas y checksums vivos de los archivos; no descarga las fotografías ni reconstruye Excel. Si falta un checksum, es ambiguo o hay PENDING, **no concede permiso para reanudar**.
6. Revisar `RESULTADOS_REPARACION/diagnostico-…/diagnostico.json`: `safeToResume=true` y todos los checks verdaderos. El texto en consola muestra etapas, contadores y condiciones; no token, URL ni IDs completos. El JSON de diagnóstico también usa huellas cortas para los registros.

No se promete una duración Google no medida: son dos lecturas de capacidades y una consulta acotada, sin cargas pesadas. Si falla, conservar su condición y resultados. `LIVE_MANIFEST_RECONCILIATION_FAILED` exige revisar los checks concretos; no se oculta con un ACK inventado. Un ACK **almacenado** con `alreadyExists=false` es normal para el primer commit; el replay HTTP sí debe devolver true.

## 3. Reanudar únicamente después de D aprobado

1. Volver a abrir el lanzador y elegir **R**. Indicar la misma carpeta original y el **mismo video ficticio original**. La corrida anterior informa 529833 bytes, WebM; no regenerarlo para sustituirlo. Se verifica su SHA completo internamente, sin imprimirlo.
2. R repite inmediatamente el diagnóstico; **no confía solo en un certificado antiguo**. Si cambió runId/epoch/carpeta, fixtures, foto, video, carga, revisión, ACK, journal o archivo vivo, se detiene antes de nuevas cargas. Tampoco reinicia inflight.
3. Con permiso, evita reenviar los ocho padres, no repite BUSY provocado ni la preparación inicial. Repite una vez la comprobación idempotente de Pavimentos que se detuvo en el evento 119; exige identidad/revisión/hash, `alreadyExists === true`, carpeta/archivos iguales y telemetría v2 vinculada a esa solicitud.
4. Si ese ACK vuelve a ser incompatible o llega sin métricas, se conserva la condición exacta y se detiene **antes** de las ediciones y del video. No se cambia el contrato ni se rellena el flag.
5. Solo tras pasar esa comprobación continúa con las dos ediciones de Observaciones, el video y la conciliación final que faltaban. Esas etapas sí escriben en la subraíz ficticia autorizada. No se ejecutan automáticamente por Codex ni en modo D.
6. Guarda el intento en `RESULTADOS_REPARACION/BASE_<misma corrida>/reanudaciones/<intento nuevo>/`, con nuevos `resultado.json`, `eventos.jsonl`, `avance.json` y archivos de evidencia. Los originales permanecen intactos. Si se requiere continuar otra vez desde un intento nuevo, seleccionar **ese intento** como fuente; nunca mezclar su resultado con el avance original.

Una revisión confirmada en vivo que el avance no pueda explicar, un commit de resultado incierto o un journal PENDING bloquean la reanudación automática: se necesita conciliación específica, no forzar contadores ni borrar archivos. La recuperación soportada parte de padres cuyo ACK durable está en avance; no inventa preparaciones de transacciones desconocidas.

## Lectura de fallos y progreso

- `HARNESS_METRICS_MISSING`: hubo respuesta JSON pero falta la telemetría. El evento conserva checks seguros del ACK; confirmar versión/ejecución del helper, sin atribuirlo automáticamente a la transacción.
- `ACK_ALREADY_EXISTS_NOT_TRUE`, `ACK_FOLDER_MISMATCH`, `ACK_IDENTITY_MISMATCH`, `ACK_REVISION_MISMATCH`, `ACK_CONTENT_HASH_MISMATCH`: incompatibilidad concreta. No se corrige el ACK en el cliente.
- `HARNESS_RESPONSE_BINDING_MISMATCH`: respuesta no vinculada a run/variante/request de esta solicitud. Se detiene.
- `TRANSPORT_UNCERTAIN_REQUIRES_DIAGNOSTIC`: timeout/red/JSON incierto. No se repite una carga pesada a ciegas; se exige nueva lectura de conciliación. El timeout nuevo es 300 s, acotado/configurable, no una promesa de rapidez.
- `FIXTURE_SIGNATURE_MISMATCH`, `VIDEO_FIXTURE_CHANGED`, `RUN_OR_EPOCH_CHANGED`: no es seguro reutilizar el avance.
- `failure.stage`, `failure.condition`, `failure.checks` y, si es una aserción genérica, `failure.sourceLocation` identifican el punto sin guardar mensajes crudos con valores privados. Las huellas de solicitud permiten correlacionar cliente y log Google.

La consola informa **padres confirmados**, familia, etapa, reintentos y duración; no confunde archivos con fichas. Si un cliente falla, se dejan terminar las solicitudes ya iniciadas del otro antes de cerrar el informe y se bloquean nuevas solicitudes de esa cola.

## Medición y comparación

Los tiempos de la corrida incompleta corresponden al aislamiento **v1**. La continuación marca `recoveryContinuation=true`; no es una línea base comparable limpia contra el candidato. Los eventos conservados mantienen su orden y se distinguen de los nuevos `harnessRevision=2`. `comparar.py` rechaza mezclar v1/v2 o una continuación con un benchmark nuevo.

No repetir ahora la carga completa para “mejorar la estadística”. Primero terminar la corrida o conocer el fallo. Cuando el propietario decida medir el candidato, se necesitará un par BASE/CANDIDATO con los mismos helpers v2, fixtures, carga y condiciones. No se extrapola el ahorro local de consultas a rendimiento productivo.
