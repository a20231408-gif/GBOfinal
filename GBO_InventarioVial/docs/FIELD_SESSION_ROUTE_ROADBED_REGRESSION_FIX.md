# Sesión de campo: ruta y calzada predeterminadas

Base: fuente actual recibida, posterior a la corrección de Configuration Cache.
No se sustituye por una versión anterior.

## Causa raíz y recorrido de los datos

`SessionDialog` selecciona el código de ruta del catálogo del proyecto y el
código de calzada existente. `FieldController.startSession` guarda ambos en
`field_sessions.road` y `field_sessions.roadbed`; el tramo proviene de la ruta
elegida. `InventoryDao.currentSession/observeSession` recuperan la sesión que
no tiene `endTime`. La selección se almacena correctamente: el defecto estaba
en la inicialización de los formularios, no en la autenticación ni en Room.

En `SignalizationFormScreen` y `AssetFormScreen`, la ruta se inicializaba así:

```kotlin
mutableStateOf(draftRecord?.routeCode.orEmpty())
```

Para una ficha nueva, `draftRecord` es nulo y la ruta siempre quedaba vacía.
La calzada aún consultaba `prior?.roadbed ?: fieldSession?.roadbed`, de ahí la
asimetría descrita. El mismo código defectuoso ya aparece en el ZIP integrado
original disponible, leído solo como referencia; no se ha usado para reemplazar
archivos. No hay historial Git con el que atribuir un commit específico.

Además, la sesión se observaba mediante un StateFlow cuyo valor inicial es
nulo. Los estados `rememberSaveable` y un `LaunchedEffect(id)` podían fijar
valores vacíos antes de recibir la primera emisión de Room. En pavimentos, la
nueva muestra dependía de la última muestra del inspector y el diálogo elegía
la primera ruta del catálogo si estaba vacía. SCAP creaba el registro vinculado
con ruta y calzada vacías y ofrecía un botón para copiar la ruta manualmente.

## Solución y precedencia

`FieldSessionDefaults` define una única precedencia para los valores iniciales:
un registro recuperado conserva sus valores propios, incluidos los vacíos;
para una ficha nueva se usa la sesión activa; sin ella no se inventa una ruta.
Los códigos seleccionados se conservan, y el tramo de una nueva ficha se deriva
del mismo catálogo de rutas, sin confundir ruta, eje, calzada o carril.

`rememberFieldCaptureContext` espera la lectura de Room antes de construir los
estados de los formularios generales. El par ruta/calzada se captura junto una
sola vez; no se aplica un efecto que reescriba los campos cuando el ingeniero
los edita. Se conserva `rememberSaveable` para los valores de la ficha.

Los formatos SIC independientes leen la sesión desde Room al crear su estado,
después de intentar cargar su registro. Durante la carga no ofrecen campos
editables que pudieran ser sobrescritos por la respuesta asíncrona. Incluso un
registro base sin extensión técnica conserva su ubicación propia. Para los
guardados independientes, también se conserva el identificador de la sesión
del registro recuperado, o el de la sesión capturada al crear la ficha, en lugar
de reasignarlo al recibir otra emisión de la sesión observable.

La continuidad de progresiva solo se reutiliza si coinciden sesión, ruta y
calzada. Una edición manual de la ficha anterior no reemplaza la ruta/calzada
elegidas para las siguientes fichas de la sesión ni arrastra su PR a otra vía.

SCAP inicializa únicamente la inspección nueva, sus campos existentes y su
registro vial vinculado. Las dos variantes, LOSA y MODULAR, reciben el mismo
par; no se inventa una progresiva ni una observación GNSS. La edición manual de
calzada antes de confirmar la ubicación SIC actualiza el registro vinculado,
igual que la edición de ruta. Una ubicación con PR/SIC confirmado conserva las
reglas contractuales anteriores y se modifica mediante su acción explícita.

Pavimentos consulta la sesión dentro de la transacción que crea una muestra.
La continuidad y dimensiones anteriores se reutilizan de una muestra de la
misma ruta/calzada cuando estos valores están seleccionados en la sesión.
Se conserva la lógica de numeración, superficie y permisos del inspector. El
diálogo no sustituye una ruta vacía por la primera del catálogo al reabrirla.

No se añade persistencia: siguen vigentes las filas de sesión, su finalización
y las preferencias de continuidad existentes. No se cambia el inicio/cierre
de sesión, autenticación, TABLET_ADMIN, sincronización ni exportación.

El catálogo actual de calzadas es compartido (`UC`, `UD`, `CD`, `A1`, `A2`);
no define una lista distinta por ruta. Se conserva el código seleccionado sin
traducirlo a Derecho/Izquierdo del carril ni a `EJE` de la geometría. Las pruebas
usan los códigos del catálogo en los formularios y comprueban también que el
resolver conserva literalmente valores de calzada sin interpretar su nombre.

## Archivos

- Nuevo: `app/src/main/java/com/tuempresa/inventariovial/field/FieldSessionDefaults.kt`.
- Formularios: `MainActivity.kt`, `sic/SicFieldWorkspace.kt`.
- Pavimentos: `pavement/PavementHeader.kt`, `pavement/PavementRepository.kt`, `pavement/ui/PavementDialogs.kt`.
- SCAP: `scap/data/ScapRepository.kt`, `scap/ui/ScapWorkspace.kt`.
- Pruebas nuevas: `FieldSessionDefaultsTest.kt`, `FieldSessionFormsTest.kt`.
- Pruebas existentes ajustadas: `SicUiParityTest.kt` prepara WorkManager, aísla el usuario simulado y espera que exista el botón habilitado tras la lectura asíncrona; `TabletUxUiTest.kt` prepara WorkManager y espera el formulario avanzando el looper simulado. Se conservan todas sus aserciones.
- Evidencia reproducible: `tools/verify_field_session_defaults.py` y `docs/field-session-defaults-evidence/`.

Las rutas abreviadas de producción de esta lista están dentro de
`app/src/main/java/com/tuempresa/inventariovial/`; las pruebas están dentro de
`app/src/test/java/com/tuempresa/inventariovial/`.

## Verificación

`assembleDebug`: **BUILD SUCCESSFUL**, APK empaquetada correctamente. La
auditoría de la APK debug confirma el package existente, `versionCode = 14`,
firma debug existente válida y alineación ZIP. El certificado debug no es el
certificado histórico de producción; este artefacto es de validación técnica.

Resultado final de `testDebugUnitTest`: **389 pruebas, 374 aprobadas y 15
fallidas**. Las nueve pruebas de regresión nuevas pasan. La comparación por
nombre de caso con la línea base confirma **cero fallos nuevos**, cero casos
anteriores faltantes y ningún cambio de resultado en los 380 casos previos.

| Ejecución | Total | Aprobadas | Fallidas | Fallos nuevos |
| --- | ---: | ---: | ---: | ---: |
| Línea base recibida | 380 | 365 | 15 | — |
| Fuente corregida | 389 | 374 | 15 | 0 |

La tarea Gradle de tests termina con **BUILD FAILED** por esos mismos 15
casos previos; no se presenta la suite completa como totalmente aprobada.
Los casos fallidos pertenecen a Engineering2Data/Screen/Ux, migraciones de
InventoryRoom/PrSuggestion/RoadAxis/RoundTwo/ScapStorage/Survey, dos casos de
RoadReferenceDelivery, ScapExport/ScapFullExport y dos de UserAuthentication.
`final-tests.json` contiene los nombres exactos, sus estados y la comparación.

Las 12 pruebas existentes de `SicUiParityTest` y `TabletUxUiTest` pasan juntas
después de ajustar sus esperas y preparación: **BUILD SUCCESSFUL** en 4m 45s.
Los resultados individuales están en `existing-ui-verification.json`; las
aserciones de Faja, reapertura, anchos vinculados, SIC-18A, ubicación del
elemento, tablas, fotografías y navegación se mantienen.

La línea base disponible de esta misma versión fuente tiene 380 pruebas:
365 aprobadas y 15 fallidas. Se comprobó mediante hashes que el código Android
recibido en esta tarea coincide exactamente con el de aquella ejecución. Los
resultados previos se copiaron a la carpeta de evidencias de esta corrección,
sin reemplazar los informes de la tarea anterior.

El primer intento de compilación terminó antes de ejecutar tests por falta de
memoria nativa de la JVM. Se mantiene el log y se reintenta con límites de
memoria/JIT de la línea de comandos, sin modificar `gradle.properties`.

Las siete pruebas nuevas de datos y las 36 pruebas seleccionadas existentes de
rutas, SCAP y pavimentos pasaron en la primera ejecución que logró compilar.
Las dos pruebas nuevas de interfaz también pasaron. Su preparación Robolectric
inicializa WorkManager, avanza el looper y proporciona un ANDROID_ID sintético
al dispositivo simulado. No se modificó código de producción, autenticación
ni identificación de tablets para resolver esas limitaciones del entorno de
pruebas.

| Caso | Comprobación disponible |
| --- | --- |
| A/B | Dos sesiones iniciadas por el controlador real: PE-22A/CD y PE-28H/A2. Ambos códigos aparecen juntos en los formularios. El resolver además conserva literalmente Derecha/Izquierda, sin reinterpretarlos como carriles. |
| C | Registros consecutivos de SCAP y de cada superficie de pavimento conservan el par de la sesión. |
| D | Edición y guardado de otra ruta/calzada en SCAP, pavimentos y borrador de señalización; las otras filas y la sesión quedan iguales, y la ficha siguiente vuelve a los valores de la sesión. |
| E | Reapertura del borrador con ruta/calzada propias; valores vacíos de registros recuperados y ubicación SCAP confirmada también se conservan. |
| F | Navegación entre doce formularios reales, con retorno al inicio y nueva entrada, en ambas sesiones. Creación en repositorios SCAP y pavimentos dentro de una misma sesión Room. |
| G | Código de ruta, entidad del catálogo, ítem y tramo visible corresponden a la ruta seleccionada. |

Los doce formularios de interfaz comprobados son señalización vertical y
horizontal; alcantarillas, cunetas, badenes, túneles, muros y derecho de vía;
y SIC-08, SIC-09, SIC-13 y SIC-34. SCAP LOSA/MODULAR y las superficies de
pavimento se comprueban mediante sus repositorios reales y Room.

La verificación de alcance compara 610 hashes iniciales. Solo cambiaron los
siete archivos de producción previstos y las dos pruebas de interfaz
indicadas; se añadieron el resolver y las dos clases de pruebas.
Esquemas/migraciones Room, autenticación, TABLET_ADMIN,
backend, Apps Script, archivos Gradle y la solución de Configuration Cache
conservan sus hashes.

`adb devices -l` no mostró dispositivos conectados. La validación de interfaz
disponible usa Compose real con Robolectric SDK 34, no una tablet física.

Comandos de compilación/pruebas usados, con Java de Android Studio:

```powershell
$env:JAVA_HOME='D:\Android Studio\jbr'
$taskCommon=@(
  '--no-configuration-cache','--no-daemon','--max-workers=1',
  '-Dorg.gradle.jvmargs=-Xmx1024m -XX:ActiveProcessorCount=2 -XX:ReservedCodeCacheSize=128m -XX:TieredStopAtLevel=1 -Dfile.encoding=UTF-8',
  '-Pkotlin.compiler.execution.strategy=in-process')
.\gradlew.bat assembleDebug @taskCommon
.\gradlew.bat testDebugUnitTest @taskCommon
python tools/verify_field_session_defaults.py tests
python tools/verify_field_session_defaults.py scope
```

`--no-configuration-cache` es solo una opción de estas ejecuciones de
validación. No se desactiva la propiedad del proyecto ni se modifica su
corrección anterior. Los límites de memoria/JIT permiten compilar en el equipo
actual sin cambiar archivos Gradle.

La primera suite completa terminó con 389 pruebas y 19 fallos: los 15 previos
y cuatro de preparación/sincronización de interfaz. Las trazas identificaron
una búsqueda del botón antes de su aparición, dos intentos de autenticar otro
usuario porque la prueba nueva dejaba abierta su sesión simulada, y una falta
de inicialización de WorkManager en la prueba de tablet con la configuración
actual del equipo. Se conserva esa ejecución en
`full-suite-before-ui-waits.log` y `full-suite-before-ui-waits.json`.

La clase nueva ahora cierra su usuario simulado y limpia sus datos de prueba.
Las dos clases existentes esperan la carga sin eliminar aserciones; la prueba
de tablet prepara WorkManager. Estos ajustes afectan únicamente a tests. La
comparación final se repitió después de validar las nueve pruebas nuevas y las
doce existentes de interfaz. La ejecución de estas últimas también inicializa
WorkManager por clase, sin depender de otras pruebas.

Evidencias: `assemble-debug.log`, `debug-apk-identity.json`,
`forms-verification.json`, `existing-ui-verification.json`, `final-tests.log`, `final-tests.json` y
`scope-verification.json`, dentro de `docs/field-session-defaults-evidence/`.

## Identidad y límites

Se conserva `applicationId = com.tuempresa.inventariovial`, la configuración de
firma histórica y su certificado esperado. No hay cambios de esquema Room ni
migraciones. No se generan claves ni se modifican archivos de configuración
privados o de Apps Script. La corrección de Configuration Cache permanece
intacta.

No se ha generado una nueva APK release destinada a actualizar la instalada.
El código fuente conserva `versionCode = 14`. Antes de producir y distribuir
un nuevo release de esta corrección, corresponde incrementar sobre 14 y validar
el certificado histórico. Falta la configuración de firma release en este
equipo (`keystore.properties` y las cuatro variables alternativas no están
disponibles); no se ha sustituido por otra clave. `server.local.properties`
sí existe y se ha mantenido sin cambios.
