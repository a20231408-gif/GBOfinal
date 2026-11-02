# Colaborador B — validación focalizada Android (01/11/2026)

Base: `dda0e2398933d8853e963fa95361a81aaddd351d` en `codex/observaciones-campo-asus-20261030`. Rama: `codex/colaborador-android-contratos`.

## Defecto reproducible

`AutomaticDelivery.validAck` aceptaba cualquier `logicalPath` de un archivo de Pavimentos que comenzara por la carpeta de la muestra. Un ACK ficticio para `HOJAS_CAMPO/HC_002.pdf`, con el mismo `key`, revisión, hash y `requestedPath`, pero cuya ruta final era `.../MUESTRA 7/DATOS_APP/HC_007.pdf`, cumplía el predicado anterior. Esto podía confirmar una entrega cuyo archivo terminó en una subcarpeta distinta de la solicitada.

La corrección comprueba la ruta final completa después del renombrado de muestra que aplica el servidor (`HC_002`/`M_002` a `HC_007`/`M_007`, y prefijo fotográfico `M007_` cuando corresponde). Acepta las dos formas fotográficas previstas por el contrato V2 y la etiqueta pública de carpeta. No modifica el contenido preparado, su hash, el JSON de transporte ni el backend.

`PavementAckPathContractTest` usa únicamente UUID, hash, carpeta y rutas ficticios. Cubre ACK válido, subcarpeta incorrecta, nombre fotográfico incorrecto y etiqueta pública; comprueba conservación del snapshot, UUID, revisión y hash. `SyncRecovery10DTest` cubre que un ACK incompleto no eleva la revisión sincronizada y conserva el preparado para reintento.

## Entorno y resultados iniciales (superados por la continuación)

- Repositorio clonado en el SHA de referencia; rama asignada creada. Android Studio con JBR 25, SDK Android 37 y Build Tools 36 localizados. `local.properties` solo contiene `sdk.dir` local y está excluido de Git.
- Perfil de validación: `-PgboValidationBuild=true`, sin endpoints ni tokens. No se usaron datos reales ni acceso a Drive.
- `:app:assembleDebug`: **BLOCKED**. Desde el proceso de comandos de Codex, Gradle falla antes de compilar por `java.io.IOException: Unable to establish loopback connection` al abrir el selector NIO de Java. Una ejecución independiente mediante el Programador de tareas avanzó hasta `:app:compileDebugKotlin`, pero Windows la terminó con resultado `-1073741510` antes de producir APK o resultado de compilación. No se declara PASS.
- `PavementAckPathContractTest`, `SicContributionPolicyTest` y `SyncRecovery10DTest`: **BLOCKED**, porque la compilación previa no terminó. No se declara PASS.
- `git diff --check`: PASS.

## Alcance y pendientes

Esta entrega cubre de forma focalizada la verificación de rutas de ACK de Pavimentos y conservación de identidad, revisión, hash y snapshot ante un ACK inválido. La inspección del código no reemplaza la ejecución de los casos físicos P01–P17; siguen pendientes los filtros, búsqueda, nuevo tramo/sesión, descarte/restauración, longitud 10 m, autoguardado, consolidación, daño 26/PCI, SIC-17 y conflicto SCAP en dispositivo. Se requiere repetir el Debug y las pruebas focalizadas en una terminal Windows ordinaria o en CI antes de integrar. No hay GO productivo.

## Continuación del PR borrador #1 — 01/11/2026

El propietario informó que actualizó la configuración de AGP en Android Studio, compiló el proyecto y abrió GBO en su tablet. **Es validación informada por el usuario:** no se recibió SHA de la compilación, resultado de las pruebas funcionales ni evidencia de que el binario de la tablet contenga este PR. No se cuenta como compilación ni prueba ejecutada por Codex.

Revisión inicial: rama limpia al iniciar en `b337fc51ad912a20d86f16020366b5c915425b9e`; PR #1 abierto en borrador, base y rama correctas, tres archivos de la primera entrega. JBR 25.0.4, Gradle Wrapper 9.6.0, SDK 37, Build Tools 36.0.0 y AGP 9.5.0 detectados. El perfil `gboValidationBuild=true` sigue omitiendo credenciales. El proceso de comandos de Codex sufrió `java.io.IOException: Unable to establish loopback connection` de Java NIO; una ejecución local independiente permitió terminar la compilación y las pruebas descritas abajo. No se cambió AGP, Gradle ni `app/build.gradle.kts`.

Se añadió una regresión a `PavementPdfDeliveryIntegrationTest`: con Room en memoria y transporte simulado, un ACK de Pavimentos con `logicalPath` erróneo debe dejar el outbox en `ERROR`, `syncedRevision=0`, sin `receiptJson`, y mantener el preparado exacto, la muestra, daños y bytes de la foto. Un segundo intento con ACK válido debe confirmar esa misma revisión sin regenerar ni alterar los datos locales. El caso **pasó** en la primera corrida ejecutada. No se modificó código de producción en esta continuación.

### Ejecución real de Debug y pruebas

- `:app:assembleDebug -PgboValidationBuild=true`: **PASS**, `BUILD SUCCESSFUL in 30s`, 42 tareas. Log local ignorado por Git: `.gradle/gbo-cont-assemble.log`; código de salida 0. Esta es una compilación de validación sin credenciales, no un APK oficial.
- Primera selección de cinco clases: **21/23 PASS**, 2 FAIL. `PavementPdfDeliveryIntegrationTest` 2/2 (incluida la nueva conservación y reintento), `SicContributionPolicyTest` 2/2 y `BridgeIdentityConflictTest` 6/6. `PavementAckPathContractTest` 3/4; el fixture omitía `family` para la etiqueta pública y se corrigió. `SyncRecovery10DTest` 8/9; el caso restante exige nombres de foto V2, desactivados expresamente por `gboValidationBuild=true`. No se cambió ni debilitó ese caso histórico.
- Una repetición posterior del ejecutor devolvió `EOFException` sin XML nuevo. Tras detener los daemons y ejecutar `cleanTestDebugUnitTest` sin caché de configuración, `PavementAckPathContractTest` y `PavementPdfDeliveryIntegrationTest` terminaron **PASS** (`BUILD SUCCESSFUL in 1m 9s`, código 0). El log local es `.gradle/gbo-cont-tests-final.log`.
- El propietario ejecutó `Dashboard6PersistenceTest` desde Android Studio: **3/4 PASS, 1 FAIL** antes de la corrección del fixture. El XML generado registra el fallo de identidad en `legacyDraftReopensWithPhotosObservationsAndNoInventedSession`.

### Conflicto de UUID del Dashboard

La ficha histórica de la prueba se creaba con `testRecord(..., sic="SIC-22")`; esa ayuda fija `assetType="DRENAJE"`. La solicitud de guardado usa el mismo UUID con `SIC-22/VERTICAL`. `sameInventoryIdentity` rechaza esa reutilización entre tipos, como debe hacerlo. El mismo fixture y la misma validación están presentes en `codex/observaciones-campo-asus-20261030`: **defecto del fixture, no de la persistencia**.

Se corrigió el registro ficticio a `SIC-22/VERTICAL`. El caso ahora verifica primero que un intento `SIC-22/DRENAJE` con el mismo UUID se rechaza por la guarda de identidad y deja intactos el registro y el borrador; después verifica UUID, observaciones, fecha de creación, ausencia de sesión histórica inventada, texto parcial, referencia/categoría de foto y bytes originales. No se alteró `InventoryDraftStore` ni la guarda de identidad. La repetición posterior **pasó 4/4**; el XML contiene `failures="0"` y `errors="0"`. En la misma corrida, `PavementAckPathContractTest` **4/4** y `PavementPdfDeliveryIntegrationTest` **2/2**: **10/10 PASS**, `BUILD SUCCESSFUL in 1m 59s`, salida 0. Tras reforzar la aserción del mensaje de UUID, Dashboard se repitió aislado y pasó **4/4** (`BUILD SUCCESSFUL in 1m 32s`, salida 0). Los logs locales son `.gradle/gbo-cont-tests-dashboard.log` y `.gradle/gbo-cont-tests-dashboard-isolated.log`.

### Matriz de observaciones

| # | Observación | Estado de esta iteración | Evidencia y límite |
|---|---|---|---|
| 1 | Filtros fecha/estado/orden | BLOQUEADO POR DEPENDENCIA | `RecordDateFilterTest` y pruebas de listados existen; la pantalla unificada es del frente A. Lectura realizada, sin cambio de sus archivos. P05–P06 pendientes en tablet. |
| 2 | Búsqueda de fichas | BLOQUEADO POR DEPENDENCIA | `DriveDeliveryListsUiTest` cubre búsqueda de historial; la búsqueda general requiere revisión con A. Validación física pendiente. |
| 3 | Nuevo tramo, cancelación, sesión | IMPLEMENTADO, PENDIENTE DE VALIDACIÓN FÍSICA | `FieldSessionFormsTest` verifica ruta obligatoria, cancelación y conservación de sesión/outbox. Sin cambio. |
| 4 | Descarte y restauración | IMPLEMENTADO, PENDIENTE DE VALIDACIÓN FÍSICA | `LocalDraftDiscard10CTest` verifica rollback, fotos, UUID y ausencia de envío. Sin cambio. |
| 5 | 42+995 a 43+005 = 10 m | CORRECTO, SIN CAMBIOS NECESARIOS | `ContractualLength111Test` y `LinearLengthDeliveryTest` contienen el caso, persistencia y exportación; no se reejecutaron aquí. P08 pendiente. |
| 6 | Autoguardado Pavimentos | IMPLEMENTADO, PENDIENTE DE VALIDACIÓN FÍSICA | Pruebas de durabilidad y persistencia de Pavimentos existentes; no se reprodujo un defecto nuevo. P11 pendiente. |
| 7 | Consolidación al finalizar unidades | IMPLEMENTADO, PENDIENTE DE VALIDACIÓN FÍSICA | `SicLocalConsolidationTest` y pruebas de cierre existen. Cualquier cambio de consolidación remota corresponde coordinarlo con C. P13 pendiente. |
| 8 | Daño 26 y severidades | IMPLEMENTADO, PENDIENTE DE VALIDACIÓN FÍSICA | `FieldCleanupAndConsolidationTest` y pruebas de juntas cubren L/M/H; no se modificó el motor. P12 pendiente. |
| 9 | Conservación PCI | CORRECTO, SIN CAMBIOS NECESARIOS | `PavementFinalIntegrityTest` comprueba que anotaciones no alteran PCI ni Excel; no se reejecutó aquí. Validación física pendiente. |
| 10 | SIC-17 obligatorio; 17A/B deshabilitados | PRUEBA FOCALIZADA PASS, VALIDACIÓN FÍSICA PENDIENTE | `SicContributionPolicyTest` 2/2 en la primera selección; P03 pendiente. |
| 11 | Identidad SCAP y conflicto | PRUEBA FOCALIZADA PASS, VALIDACIÓN FÍSICA PENDIENTE | `BridgeIdentityConflictTest` 6/6; conflicto real requiere conciliación autorizada. P02 pendiente. |
| 12 | Revisión, UUID, hash y ACK | CORREGIDO Y PROBADO EN MOCKS | Ruta de ACK de Pavimentos; `PavementAckPathContractTest` 4/4 y regresión integrada de conservación/reintento PASS. Validación física pendiente. |
| 13 | Estado local y reintentos | PRUEBA FOCALIZADA PASS, VALIDACIÓN FÍSICA PENDIENTE | Regresión de outbox y reintento con ACK inválido PASS. `SyncRecovery10DTest` 8/9 bajo perfil sin nombres V2; el noveno caso exige ese flag. |

**CORREGIDO Y PROBADO:** ACK de Pavimentos y fixture de reapertura histórica del Dashboard en las selecciones indicadas. El caso de Dashboard pertenece a los datos ficticios de prueba: no se detectó defecto en la lógica de persistencia. Las pruebas de otras filas que no aparecen como ejecutadas se basan en inspección y cobertura existente, no en nueva validación funcional.

### Validación pendiente y siguiente bloque

En Android Studio, abrir esta rama en el commit del PR y repetir `Dashboard6PersistenceTest` con `gboValidationBuild=true`. Para repetir la selección completa de B, ejecutar `PavementAckPathContractTest`, `PavementPdfDeliveryIntegrationTest`, `SicContributionPolicyTest` y `BridgeIdentityConflictTest`. `SyncRecovery10DTest` contiene un caso que requiere nombres V2 y debe validarse en su perfil correspondiente. Conservar los resultados. El Debug de validación sin credenciales no debe instalarse como candidata de campo.

En la tablet, tras el procedimiento de respaldo/autorización del equipo, verificar P02, P03, P08, P11–P13 y luego P05–P06; la apertura de la app ya informada no acredita esos casos. Siguiente bloque recomendado para B: revisar las respuestas de staging y reserva con mocks para asegurar que una confirmación parcial o de identidad distinta no cambie el outbox ni las asociaciones locales. Si exige JSON/backend compartido, coordinar con C antes de cambiarlo. PR permanece en borrador; sin merge ni GO productivo.

## Actualización sobre la integración vigente — 01/11/2026

Se incorporó `bdf9ac6fe05132830b6b27aa8cc49c6b8d9cb97d` de `origin/codex/observaciones-campo-asus-20261030` mediante merge normal en la rama B. No hubo conflictos de Git (`git ls-files -u` vacío); `git diff --cached --check` pasó. Los cambios de A y de formularios de la integración quedaron intactos. Este merge no cambia el backend ni la firma, y conserva la corrección estricta de `AutomaticDelivery.validAck`, el fixture Dashboard y las pruebas de conservación.

Sobre el árbol combinado se ejecutó `:app:assembleDebug` con `-PgboValidationBuild=true` y `tools/validacion/v2-mocks.init.gradle`: **PASS**, `BUILD SUCCESSFUL in 3m 8s`, 42 tareas, salida 0. Es el Debug de validación sin credenciales; no es APK oficial ni candidata instalable.

Se ejecutaron seis clases con `tools/migracion/pruebas.init.gradle` y el perfil V2 de mocks, siempre con `gboValidationBuild=true`: `Dashboard6PersistenceTest` **4/4**, `PavementAckPathContractTest` **4/4**, `PavementPdfDeliveryIntegrationTest` **2/2**, `SicContributionPolicyTest` **2/2**, `BridgeIdentityConflictTest` **6/6** y `SyncRecovery10DTest` **9/9**. Total **27/27 PASS**, cero fallos, errores u omitidos; `BUILD SUCCESSFUL in 2m 36s`, salida 0. Los XML locales son `app/build/test-results/testDebugUnitTest/TEST-*.xml`; logs locales ignorados por Git: `.gradle/gbo-pr1-assemble.log` y `.gradle/gbo-pr1-tests.log`. El noveno caso de recuperación ahora se acredita bajo el flag V2 explícito sin credenciales.

La rama de integración documenta por separado una selección anterior de **103/103 PASS**; no se repitió en esta actualización ni se suma a los 27 casos anteriores. Su matriz manual vigente (`docs/VALIDACION_MANUAL_GBO_20261101.md`) incluye resultados reportados limitados y pendientes físicos. La matriz anterior de este documento describe el estado del PR antes de incorporar esa integración. El PR se entrega listo para revisión, sin merge. Continúan sin acreditar una candidata oficial, backend/Drive productivos y aceptación física completa; no hay GO productivo.
