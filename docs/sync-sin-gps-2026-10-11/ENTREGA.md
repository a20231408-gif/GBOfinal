# GBO — entrega técnica sin GPS

Fecha: 2026-10-11. Rama: `codex/sincronizacion-sin-gps`, basada en el checkpoint `85d0659`.

## Resultado

Android permite preparar y confirmar entrega técnica de fichas validadas con coordenadas nulas. SIC-08/SIC-09 entran automáticamente a la cola al validar; la georreferenciación externa pendiente ya no las bloquea. El backend local fue adaptado, pero el endpoint actualmente configurado no anuncia protocolo 3. **No se realizaron subidas reales ni despliegues.**

## Cambios

- `sic/SicFieldRepository.kt`: elimina el bloqueo GPS después de validar; conserva coordenadas ausentes como null, también para nuevas fichas genéricas, sin sustituirlas por 0/0 ni borrar coordenadas históricas.
- `core/AutomaticDelivery.kt`: separa entrega técnica y georreferenciación. Usa eje disponible o PR/distancia contractual para ordenar sin inventar GPS. Metadata incluye coordenadas nulas, observaciones y estado externo pendiente; conserva SIB, formatos Excel y fotografías JPEG.
- `core/AutomaticSync.kt`: recupera solamente estados bloqueados con el mensaje histórico «Entrega pendiente de georreferenciación externa», siempre que la ficha siga validada/cerrada. Conserva UUID, revisión, recibo anterior y consentimiento. Excluye borradores antes de ordenarlos. Reutiliza la preparación de la revisión al reintentar; guarda ACK en transacción y rechaza confirmaciones incompletas. Mantiene WorkManager con conexión obligatoria, backoff exponencial y recuperación periódica.
- `data/dao/InventoryDao.kt`: actualiza estado Excel técnico solo después del recibo confirmado. El worker confirma IDs de fotografías y estado de la cola; no usa el estado del servicio de usuarios como confirmación de Drive.
- `core/AutomaticSyncPanel.kt`: añade una línea en el detalle existente indicando georreferenciación submétrica pendiente. No hubo rediseño.
- `google-apps-script/Code.gs`, `SyncV3Core.js`, `SyncV3.gs`: capacidad explícita sin GPS, identificación de raíz, recibos con estados separados, hash de contenido técnico, orden por PR compatible con IDs persistentes. Se conservan staging, locks, archivo de revisiones, rollback y consolidación por UUID.

La entrega por PR requiere PR/distancia válidos. No atribuye precisión submétrica a las coordenadas de la tablet. Esta tarea no añade captura, vinculación ni certificación de georreferenciación externa. Las carpetas ya asignadas conservan su secuencia e identidad; las nuevas entregas por PR no renumeran entregas previas.

## Validación aprobada

Ejecución final: **39 pruebas Android, 0 fallos, 0 errores, 0 omitidas**, más **assembleDebug aprobado**.

| Clase focalizada | Pruebas | Resultado |
| --- | ---: | --- |
| AutomaticSyncWithoutGpsTest | 7 | Aprobadas |
| AutomaticSyncRegressionTest | 21 | Aprobadas |
| Sic0809CaptureTest | 7 | Aprobadas |
| Sic0809ScreenTest | 4 | Aprobadas |

Los siete casos nuevos cubren cola offline/reapertura, SIC-08/09 con fotografías y Excel sin coordenadas, pérdida de ACK después del commit y reintento con mismos IDs, borrador incompleto excluido, cuatro bloqueos antiguos GPS recuperados sin perder identidad, backend incompatible sin POST, recibo incompleto sin confirmación y WorkManager en espera offline con conexión requerida/backoff.

Backend: **60 pruebas locales aprobadas, 0 fallos** entre `Code.test.cjs` y `SyncV3.test.cjs`. Incluyen guardas de capacidad, locks, revisiones, rollback, duplicados y procesamiento de los payloads/JPEG/XLSX reales generados por Android, con IDs de Drive **simulados**. Se comprobó igualdad del hash Android/JavaScript.

Evidencia conservada (los logs crudos y las carpetas de resultados permanecen locales; Git incluye `VALIDACION.json` y fixtures sintéticas necesarias para las pruebas):

- `focused-debug-final.log` y `final-results/android/`: resultados finales completos.
- `backend-tests-verified.log` y `backend-results/`: pruebas y recibos simulados del motor Apps Script.
- `examples/SIC-08/` y `examples/SIC-09/`: payloads con bytes reales de foto/Excel y coordenadas nulas. Los recibos de estos directorios son simulados.
- `focused-debug.log`, `focused-debug-retry.log`, `initial-results/` y `retry-results/`: ejecuciones fallidas iniciales conservadas. Se corrigió una ordenación prematura de borradores; las consultas de prueba Room se movieron al hilo IO y la expectativa SCAP se cambió a preservar su estado inicial real. No se modificó SCAP para hacer pasar ese test.
- `before/`: fuentes previas del backend preservadas; `backend-changes.patch`: diferencias locales.

Para que las pruebas Node puedan ejecutarse sin depender de otros artefactos anteriores, se incorporan las seis fixtures sintéticas SIC-17 usadas por `SyncV3.test.cjs`, bajo `docs/automatic-sync-v16/examples/inputs/{A,B,C,D,B_EDIT,OTHER}/SIC-17.xlsx`. Fueron generadas por la prueba Android de regresión existente; su inclusión es una dependencia de esta entrega, no una nueva modificación de formatos SIC ni funcionalidad SCAP.

No se ejecutó la suite completa. La clase focalizada existente incluye su comprobación histórica de columnas/migración; pasó en la ejecución final. No se declaran aprobadas otras auditorías o migraciones completas.

Comando de Android utilizado:

```powershell
.\gradlew.bat testDebugUnitTest --tests '*AutomaticSyncWithoutGpsTest' --tests '*AutomaticSyncRegressionTest' --tests '*Sic0809CaptureTest' --tests '*Sic0809ScreenTest' assembleDebug '-PgboValidationBuild=true' '-Pksp.incremental=false' --no-daemon --console=plain --no-configuration-cache
```

Comando backend (ejecutar después de generar las fixtures Android):

```powershell
$env:GBO_SYNC_TEST_OUTPUT = Join-Path (Get-Location) 'docs/sync-sin-gps-2026-10-11/backend-results'
node --test google-apps-script/SyncV3.test.cjs google-apps-script/Code.test.cjs
```

## Artefacto

`app/build/outputs/apk/debug/app-debug.apk`, SHA-256 `5EE9C473101A6C66814C649076A6FE7F85E0825AF66BABF7A1AA91C0CEEF823C`.

APK Debug de validación, con endpoints/credenciales locales omitidos; no está configurada para subir a Drive ni para reemplazar un Release firmado. VersionCode 16, versionName 1.4.2. No se cambió applicationId ni firma histórica. El siguiente Release que deba actualizar también instalaciones 16 necesita versionCode 17.

## Backend real y pendientes

GET del endpoint configurado localmente: `ok=true`, versión `v6.2 · encarpetado estricto por elemento + SCAP v2`, sin capacidades V3. Se verificó separación entre URL Drive y URL usuarios sin exponer sus valores privados. No se certificó la URL dentro de la APK instalada.

PRUEBA_FINAL existe: folderId `1u7II1gvhSIHBwVxFjnx6VsAqa3O_jTIi`; su relación con producción no pudo comprobarse porque el despliegue actual no identifica su raíz efectiva. No se creó la subcarpeta ni se escribió a Drive. Pendientes: confirmación del árbol productivo, subcarpeta aislada, proyecto/despliegue independiente compatible, configuración de APK de prueba y prueba física A–H con IDs **reales**, apertura de archivos y reintento sin duplicados.

El procedimiento completo y los pasos que el responsable debe autorizar/ejecutar están en [DESPLIEGUE_PRUEBAS.md](DESPLIEGUE_PRUEBAS.md). Paquete preparado: `deployment-test/`, con marcadores y sin credenciales.

## Recuperación y límites

El checkpoint previo preserva SIC-08/SIC-09 físicamente validados, Drive, SCAP y pavimentos. Esta entrega se prepara para un commit selectivo de sincronización sin GPS, pruebas y documentación. Se incluyen completos los tres archivos del backend Drive y sus dos archivos de pruebas, que ya tenían una base previa no registrada en Git: esa base es requerida para V3 y no se atribuye toda al Prompt 1. El diff específico está conservado en `backend-changes.patch`; las copias `before/` permanecen locales. Los demás archivos preexistentes no registrados se conservaron fuera del commit. `buildSrc/src/main/java/com/gbo/build/ValidateReleaseConfigurationTask.java` permanece registrado. El hash final se registra en `docs/CODEX_HANDOFF.md`.

Se revisaron los 13 archivos fuente/pruebas candidatos; sin coincidencias de patrones de secretos operativos. API_TOKEN en el backend es un marcador y las credenciales de pruebas son ficticias. Las propiedades locales privadas siguen ignoradas y no se copiaron al paquete ni al informe. La comprobación por patrones no sustituye revisión individual antes de hacer stage de otros archivos preexistentes.

No se modificó `backend/user-service/`, autenticación, firma, applicationId ni configuración privada. No se usaron reset/clean ni se eliminaron archivos. Tras esta validación se detienen nuevas modificaciones funcionales; puede continuarse con la preparación autorizada del entorno aislado.
