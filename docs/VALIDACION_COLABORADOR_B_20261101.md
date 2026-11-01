# Colaborador B — validación focalizada Android (01/11/2026)

Base: `dda0e2398933d8853e963fa95361a81aaddd351d` en `codex/observaciones-campo-asus-20261030`. Rama: `codex/colaborador-android-contratos`.

## Defecto reproducible

`AutomaticDelivery.validAck` aceptaba cualquier `logicalPath` de un archivo de Pavimentos que comenzara por la carpeta de la muestra. Un ACK ficticio para `HOJAS_CAMPO/HC_002.pdf`, con el mismo `key`, revisión, hash y `requestedPath`, pero cuya ruta final era `.../MUESTRA 7/DATOS_APP/HC_007.pdf`, cumplía el predicado anterior. Esto podía confirmar una entrega cuyo archivo terminó en una subcarpeta distinta de la solicitada.

La corrección comprueba la ruta final completa después del renombrado de muestra que aplica el servidor (`HC_002`/`M_002` a `HC_007`/`M_007`, y prefijo fotográfico `M007_` cuando corresponde). Acepta las dos formas fotográficas previstas por el contrato V2 y la etiqueta pública de carpeta. No modifica el contenido preparado, su hash, el JSON de transporte ni el backend.

`PavementAckPathContractTest` usa únicamente UUID, hash, carpeta y rutas ficticios. Cubre ACK válido, subcarpeta incorrecta, nombre fotográfico incorrecto y etiqueta pública; comprueba conservación del snapshot, UUID, revisión y hash. `SyncRecovery10DTest` cubre que un ACK incompleto no eleva la revisión sincronizada y conserva el preparado para reintento.

## Entorno y resultados

- Repositorio clonado en el SHA de referencia; rama asignada creada. Android Studio con JBR 25, SDK Android 37 y Build Tools 36 localizados. `local.properties` solo contiene `sdk.dir` local y está excluido de Git.
- Perfil de validación: `-PgboValidationBuild=true`, sin endpoints ni tokens. No se usaron datos reales ni acceso a Drive.
- `:app:assembleDebug`: **BLOCKED**. Desde el proceso de comandos de Codex, Gradle falla antes de compilar por `java.io.IOException: Unable to establish loopback connection` al abrir el selector NIO de Java. Una ejecución independiente mediante el Programador de tareas avanzó hasta `:app:compileDebugKotlin`, pero Windows la terminó con resultado `-1073741510` antes de producir APK o resultado de compilación. No se declara PASS.
- `PavementAckPathContractTest`, `SicContributionPolicyTest` y `SyncRecovery10DTest`: **BLOCKED**, porque la compilación previa no terminó. No se declara PASS.
- `git diff --check`: PASS.

## Alcance y pendientes

Esta entrega cubre de forma focalizada la verificación de rutas de ACK de Pavimentos y conservación de identidad, revisión, hash y snapshot ante un ACK inválido. La inspección del código no reemplaza la ejecución de los casos físicos P01–P17; siguen pendientes los filtros, búsqueda, nuevo tramo/sesión, descarte/restauración, longitud 10 m, autoguardado, consolidación, daño 26/PCI, SIC-17 y conflicto SCAP en dispositivo. Se requiere repetir el Debug y las pruebas focalizadas en una terminal Windows ordinaria o en CI antes de integrar. No hay GO productivo.

## Continuación del PR borrador #1 — 01/11/2026

El propietario informó que actualizó la configuración de AGP en Android Studio, compiló el proyecto y abrió GBO en su tablet. **Es validación informada por el usuario:** no se recibió SHA de la compilación, resultado de las pruebas funcionales ni evidencia de que el binario de la tablet contenga este PR. No se cuenta como compilación ni prueba ejecutada por Codex.

Revisión actual: rama limpia al iniciar en `b337fc51ad912a20d86f16020366b5c915425b9e`; PR #1 abierto en borrador, base y rama correctas, tres archivos de la primera entrega. JBR 25.0.4, Gradle Wrapper 9.6.0, SDK 37, Build Tools 36.0.0 y AGP 9.5.0 detectados. El perfil `gboValidationBuild=true` sigue omitiendo credenciales. `:app:assembleDebug` y la selección de pruebas Android volvieron a detenerse **antes de compilar fuentes** por `java.io.IOException: Unable to establish loopback connection` de Java NIO al iniciarse desde Codex. Se trata de una limitación del entorno de ejecución observado; no hay mensaje de error de fuente ni resultado de prueba. `gradlew --version` sí finalizó. No se cambió AGP, Gradle ni `app/build.gradle.kts`.

Se añadió una regresión a `PavementPdfDeliveryIntegrationTest`: con Room en memoria y transporte simulado, un ACK de Pavimentos con `logicalPath` erróneo debe dejar el outbox en `ERROR`, `syncedRevision=0`, sin `receiptJson`, y mantener el preparado exacto, la muestra, daños y bytes de la foto. Un segundo intento con ACK válido debe confirmar esa misma revisión sin regenerar ni alterar los datos locales. Es una prueba **añadida, pendiente de ejecución**; no se atribuye PASS. No se modificó código de producción en esta continuación.

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
| 10 | SIC-17 obligatorio; 17A/B deshabilitados | CORRECTO, SIN CAMBIOS NECESARIOS | `SicContributionPolicyTest` comprueba cuatro perfiles y snapshot intacto; selección actual bloqueada por el entorno. P03 pendiente. |
| 11 | Identidad SCAP y conflicto | IMPLEMENTADO, PENDIENTE DE VALIDACIÓN FÍSICA | `BridgeIdentityConflictTest` cubre UUID estable, bloqueo de reintento y presentación; conflicto real requiere conciliación autorizada. P02 pendiente. |
| 12 | Revisión, UUID, hash y ACK | DEFECTO DETECTADO, PENDIENTE | El PR corrige la ruta de ACK de Pavimentos; `PavementAckPathContractTest` y la nueva regresión comprueban conservación, pero su ejecución actual está bloqueada. |
| 13 | Estado local y reintentos | DEFECTO DETECTADO, PENDIENTE | Nueva regresión de outbox/reintento con ACK inválido. `SyncRecovery10DTest` ya cubre reanudación; falta ejecutar selección actual. |

**CORREGIDO Y PROBADO:** ningún caso nuevo en esta iteración, porque la selección Android no terminó. Los estados de código existente se basan en inspección y pruebas históricas documentadas, no en una nueva validación funcional. No se detectó otra falla de fuente demostrada que justifique tocar archivos fuera de B.

### Validación pendiente y siguiente bloque

En Android Studio, abrir esta rama en el commit del PR y ejecutar Debug con `gboValidationBuild=true` y las clases `PavementAckPathContractTest`, `PavementPdfDeliveryIntegrationTest`, `SicContributionPolicyTest`, `BridgeIdentityConflictTest` y `SyncRecovery10DTest`. Conservar el resultado y confirmar que se ejecutó el nuevo caso de ACK erróneo. El Debug de validación sin credenciales no debe instalarse como candidata de campo.

En la tablet, tras el procedimiento de respaldo/autorización del equipo, verificar P02, P03, P08, P11–P13 y luego P05–P06; la apertura de la app ya informada no acredita esos casos. Siguiente bloque recomendado para B: revisar las respuestas de staging y reserva con mocks para asegurar que una confirmación parcial o de identidad distinta no cambie el outbox ni las asociaciones locales. Si exige JSON/backend compartido, coordinar con C antes de cambiarlo. PR permanece en borrador; sin merge ni GO productivo.
