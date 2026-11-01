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
