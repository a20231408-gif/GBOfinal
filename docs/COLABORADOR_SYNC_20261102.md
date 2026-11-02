# Sincronización y estados Drive — 02/11/2026

Base: `3e631e8`, `codex/observaciones-campo-asus-20261030` (integración vigente).
Rama independiente: `codex/urgente-scap-sincronizacion-20261102`.
Alcance: presentación del cliente y fixtures ficticios. Sin APK, instalación, HTTP a GBO,
credenciales, datos de campo, cambios de Apps Script, endpoint o protocolo.

## Causas locales y corrección

La presentación no tenía un estado propio de espera de conexión: QUEUED sin Internet
mostraba «Pendiente de subir» y un UPLOADING interrumpido «Verificando entrega».
El detector comprobaba INTERNET, que describe una capacidad de la red, sin VALIDATED:
un portal cautivo podía presentarse como conectado. La observación es orientativa;
no garantiza acceso HTTP a V21 y no altera las restricciones de WorkManager.

«Sincronizar ahora» contaba solo solicitudes nuevas. Una entrega QUEUED ya existente
no suma, aunque se emita la señal para drenar la cola. El mensaje de resultado aparecía
después de todas las tarjetas, fuera de la primera vista. La UI debe mostrar junto al
botón la programación, el resultado y el estado observado de Android. Ninguno acredita
bytes enviados ni ACK. Una recuperación periódica en cola tampoco prueba envío activo.

Los contadores persistidos de progreso sobreviven al intento; fuera de un trabajador
activo se identifican como último progreso guardado. Los porcentajes existentes se
calculan por archivos verificados en staging, nunca por tiempo, bytes supuestos o ACK
final. Se omiten muestras con total desconocido, revisión ajena o contadores imposibles.
El fixture de contador inválido 4/3 reproducía 133 % en la base; no demuestra que una
tablet de campo haya guardado ese dato. Se añade la guarda de presentación sin alterar
el contador persistido ni los eventos del transporte.

Estados del detalle Drive/SCAP: Esperando conexión, Pendiente de envío, Preparando entrega,
Enviando, Confirmando archivos, Requiere atención y Entregado con ACK. Los errores
permanentes/validación/conflictos conservan prioridad sobre la falta de conexión.

## Investigación sin cambios de contrato

| Área | Evidencia local y límite |
|---|---|
| WorkManager | Señal única REPLACE de 1 s; drain APPEND_OR_REPLACE con CONNECTED y demora aleatoria 1–5 s; recuperación KEEP cada 15 min. No se cancela una subida al pulsar sincronizar. UI observa también la señal. Android decide el inicio real. |
| Reintentos | Backoff exponencial de 30 s; drain solicita retry hasta intento 4; ERROR pendiente puede volver por recuperación periódica. BUSY/transporte son temporales. Rechazo no retryable queda BLOCKED; identidad/revisión queda CONFLICT. No se cambian estas políticas. |
| Outbox | UUID, revisión, preparación y recibos persistidos en Room. Un estado UPLOADING guardado no prueba actividad: se exige WorkInfo RUNNING con activeKey/activeRevision. |
| Sesión | AutomaticSyncDrain/AutomaticDriveHttp no consultan la sesión AuthGraph activa; usan configuración Drive de la instalación y la ficha persistida. SCAP usa roadRecordId como identityId. Esto no acredita permisos equivalentes de dos instalaciones ni identifica la causa real del reporte. |
| Idempotencia | Preparación congelada; journal de staging por endpoint/key/revisión/hash; replay del mismo commit tras pérdida de respuesta. Los mocks comprueban ausencia de restaging/duplicación; la idempotencia real necesita V21. |
| ACK | Se conserva AutomaticDelivery.validAck sin cambios. Se exigen identidad, revisión, hash, carpeta, rutas y todos los archivos/aportes requeridos. Staging completo sin ACK final no confirma entrega. |
| En la carga / Mis entregas | Ambas seleccionan las mismas filas de esta tablet. En la carga contiene las no confirmadas; Mis entregas también conserva confirmadas y ofrece filtros. No existe filtro por usuario autenticado; no se introduce uno. |
| Nombres | PublicDeliveryNames solo solicita nombres descriptivos si el backend anuncia publicNamingVersion=1 y capacidades V2. Reservas y preparaciones anteriores se reutilizan. El título legible local no acredita el nombre remoto. No se renombran históricos ni se fuerza una negociación. |
| BRIDGE_IDENTITY_CONFLICT | Se conserva la política existente: conflicto permanente incluso con retryable=true, sin reenvío manual/automático; conservar evidencias y solicitar conciliación. |

## Pruebas

**78/78 casos únicos PASS**, diez clases, cero fallos/errores/omitidos; Gradle 58 s.
Windows, JBR 25.0.3, Gradle 9.6.0, Compose/Robolectric SDK 34; perfil V2 explícito,
sin endpoints/tokens configurados. Se ejecutó solo `testDebugUnitTest`; no assemble,
bundle, generación de APK, emulador, instalación ni ejecución contra GBO.

| Clase | Casos PASS |
|---|---:|
| SyncVisibility38Test | 8 |
| SyncActions38UiTest | 3 |
| SyncRecovery10DTest | 10 |
| BridgeIdentityConflictTest | 6 |
| DriveDeliveryPresentationTest | 11 |
| DriveDeliveryListsTest | 6 |
| DriveDeliveryListsUiTest | 3 |
| DriveDeliveryCardUiTest | 5 |
| AutomaticSyncRegressionTest | 21 |
| PublicDeliveryNamesTest | 5 |

Doce casos nuevos: ocho de visibilidad, tres UI y uno de transporte/cambio de usuario.
Los tests UI previos solo actualizan las etiquetas esperadas; se mantienen aserciones de
ACK, búsquedas, filtros y reintentos. No se suman ejecuciones repetidas.

Antes de la corrección, SyncVisibility38Test: 2 PASS / 3 FAIL esperados (estado sin
conexión, contador 133 %, etiqueta explícita de ACK). El primer intento de selección
tuvo dos referencias incorrectas en los fixtures, corregidas antes de ejecutar tests.
La selección posterior tuvo 76/77 PASS: SQLite no pudo abrir una ruta de 263 caracteres.
Se repitió con `C:\tmp\gbo38`, sin cambiar la prueba de reapertura ni sus aserciones,
y un caso adicional que acredita cero solicitudes nuevas con una cola existente:
78/78 PASS. `git diff --check`: PASS. Cero `.apk` en los outputs de compilación.
Logs y XML de rojo, ruta larga y final se conservaron por separado en la entrega local.

Comando reproducible (solo mocks; temporal corto necesario en Windows):

```powershell
$env:JAVA_HOME='C:\Program Files\Android\Android Studio\jbr'
New-Item -ItemType Directory -Force C:\tmp\gbo38 | Out-Null
$clases=@('SyncVisibility38Test','SyncActions38UiTest','SyncRecovery10DTest',
    'BridgeIdentityConflictTest','DriveDeliveryPresentationTest','DriveDeliveryListsTest',
    'DriveDeliveryListsUiTest','DriveDeliveryCardUiTest','AutomaticSyncRegressionTest',
    'PublicDeliveryNamesTest')
$filtros=foreach($clase in $clases){'--tests';"com.tuempresa.inventariovial.$clase"}
.\gradlew.bat '-Dgbo.migration.temp=C:\tmp\gbo38' '-PgboValidationBuild=true' `
    :app:testDebugUnitTest --init-script tools/migracion/pruebas.init.gradle `
    --init-script tools/validacion/v2-mocks.init.gradle @filtros
```

Fixtures: dos ACK completos y una entrega pendiente; sin Internet; trabajo en cola;
transporte interrumpido; ACK incompleto/correcto; conflicto de identidad; usuario distinto;
reintento con snapshot idéntico y sin duplicar staging. Solo mocks e interceptores locales.

## Aviso al integrador y aceptación pendiente

**La discrepancia real entre ingenieros y la conciliación de BRIDGE_IDENTITY_CONFLICT
requieren necesariamente acceso operativo al servidor GBO/V21 para diagnosticarse y
resolverse si el rechazo procede del servidor. Esta corrección no afirma resolverlos.**
La auditoría previa `AUDITORIA_BACKEND_OPERATIVA_20261101.md` registra autorización OAuth
pendiente; es evidencia histórica, no una comprobación HTTP nueva de esta intervención.

Pruebas necesarias con autorización y datos sintéticos en dispositivo:

1. Comparar versión/flags y configuración legítima de las dos instalaciones sin publicar
   secretos; verificar sesión y ficha persistida antes/después de cambiar usuario.
2. Modo avión, portal cautivo, recuperación de Internet y app en segundo plano/Doze:
   observar señal, espera, envío y recuperación sin prometer inicio inmediato.
3. Consultar capacidades V21, permisos/identidad ejecutora, raíz y reservas; conservar
   privadamente códigos HTTP y respuestas, key/identityId/bridgeCode/revisión/hash/ACK.
4. Perder respuesta de commit y repetir con la misma preparación: verificar archivos/IDs
   remotos y un solo resultado lógico. Contrastar ACK incompleto y completo.
5. Si hay conflicto, conciliar bindings/registros remotos con el paquete histórico;
   no cambiar UUID, crear otra ficha, renumerar ni renombrar para sortearlo.
6. Confirmar que nombres de nuevas entregas siguen la capacidad negociada y que los
   históricos conservan nombre/ID; comprobar las dos listas con dos ACK y una pendiente.

No se ejecutan estas pruebas físicas ni se declara GO productivo desde los mocks.
No se modifican módulos de selección/descarte, SCAP editor, Room/migraciones, versión,
dependencias, backend ni contrato ACK. PR sin merge hacia integración.
