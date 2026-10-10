# Corrección de Gradle Configuration Cache

Fecha: 2026-10-09. Base: la versión actual recibida del proyecto, Gradle 9.6.0,
`applicationId = com.tuempresa.inventariovial`, `versionCode = 14`.

## Causa exacta

La tarea `:app:validateReleaseConfiguration` era un `DefaultTask` cuya acción
`doLast` se declaraba en `app/build.gradle.kts`. Al leer propiedades de nivel
superior del script (`gboValidationBuild`, `releaseSigningReady`,
`serverProperties`, `driveProperties`, `releaseStore`, contraseñas, alias y
`expectedReleaseCertSha256`), el compilador Kotlin generaba una referencia al
script exterior. El informe de Gradle identifica exactamente este recorrido:

```text
:app:validateReleaseConfiguration
  -> actions
  -> AbstractTask$TaskActionWrapper.action
  -> Build_gradle$releaseConfigurationCheck$1$1.this$0
  -> objeto del script Kotlin DSL
```

El error era `cannot serialize Gradle script object references as these are not
supported with the configuration cache`. El diagnóstico original se conserva
en `gradle-configuration-cache-evidence/original-cache-diagnostic.json` y el
script anterior en `app-build-before.gradle.kts.txt` de ese mismo directorio.
El origen es la captura del script; obtener previamente un `File` mediante
`rootProject.file(...)` no explica por sí solo el error.

## Implementación

Archivos de build modificados o añadidos:

- `app/build.gradle.kts`: importación y registro tipado de la tarea; se sustituye
  únicamente el bloque de validación y se retiran sus imports de criptografía.
- `buildSrc/build.gradle.kts`: plugin Java y compilación con `release = 11`, sin
  nuevas dependencias externas.
- `buildSrc/src/main/java/com/gbo/build/ValidateReleaseConfigurationTask.java`:
  clase independiente, propiedades administradas por Gradle y `@TaskAction`.

Antes, `tasks.register(...) { doLast { ... } }` ejecutaba una lambda enlazada al
script. Ahora `tasks.register<ValidateReleaseConfigurationTask>(...)` configura
valores ya resueltos durante la fase de configuración. La acción posterior
accede solo a sus propias propiedades. No accede a `Project`, `rootProject`,
extensiones Android, `Gradle`, configuraciones ni funciones del script. Los
objetos `KeyStore`, clave privada y certificado se crean como variables locales
durante la ejecución; no forman parte del estado serializado de la tarea.

Entradas y anotaciones:

| Propiedades | Tipo y anotación | Uso |
| --- | --- | --- |
| validationBuild, signingConfigured, userServiceConfigured, driveConfigured | `Property<Boolean>`, `@Input` | Mismas condiciones previas; no se transmite el endpoint ni el token a la tarea |
| expectedCertificateSha256 | `Property<String>`, `@Input` | Huella pública existente |
| keystoreFile | `RegularFileProperty`, `@Optional`, `@InputFile`, `@PathSensitive(NONE)` | Archivo existente; si está ausente se conserva el mensaje de bloqueo de firma |
| storePassword, keyAlias, keyPassword | `Property<String>`, `@Internal` | Apertura de la misma keystore y clave; no se convierten en fingerprints de entradas ni outputs |

La tarea no declara outputs y siempre se ejecuta. `@DisableCachingByDefault`
se refiere al **Build Cache de resultados**, no a Configuration Cache; no se
deshabilita Configuration Cache. `@Internal` tampoco excluye los valores del
estado serializado: los secretos de firma siguen formando parte del cache de
configuración local cifrado por Gradle, como ocurre con la configuración de
firma Android existente. No se afirma que `@Internal` sea un mecanismo de
ocultación. La resolución de variables de entorno, propiedades Gradle y archivos
locales continúa en la fase de configuración, con la precedencia original.
Véanse los requisitos oficiales sobre [tipos y secretos en Configuration
Cache](https://docs.gradle.org/current/userguide/configuration_cache_requirements.html)
y la [serialización del estado de las tareas](https://docs.gradle.org/current/userguide/configuration_cache.html).

Se mantienen, en el mismo orden, los controles siguientes:

1. Rechazo de builds con `gboValidationBuild`.
2. Archivo de firma existente, contraseñas y alias presentes.
3. URL de User Service con prefijo `https://`.
4. URL de Drive con prefijo `https://` y token no vacío.
5. Apertura de la keystore histórica y existencia de una entrada de clave.
6. Apertura de la clave con su contraseña y comprobación `PrivateKey`.
7. Certificado presente, SHA-256 idéntico al esperado y rechazo de Android Debug.

Se conservan los mensajes de bloqueo. Los errores al abrir la keystore o la
clave no incluyen la excepción subyacente, contraseñas ni alias. Los buffers
temporales de contraseñas se borran después de usarlos. La dependencia desde
`preReleaseBuild`, `signingConfigs`, `verifyReleaseIdentity` y su conexión a
`assembleRelease` permanecen intactas.

## Pruebas con y sin Configuration Cache

Los comandos se ejecutaron con el wrapper Gradle 9.6.0 y el JDK local de Android
Studio. A todos se añadieron los mismos límites de recursos:

```powershell
--no-daemon --max-workers=1 '-Dorg.gradle.jvmargs=-Xmx1536m -XX:ActiveProcessorCount=2 -Dfile.encoding=UTF-8' '-Pkotlin.compiler.execution.strategy=in-process'
```

No se utilizó `--configuration-cache-problems=warn`, exclusión de tareas ni
desactivación global de Configuration Cache. `gradle.properties` conserva
`org.gradle.configuration-cache=true`.

| Comando | Resultado | Evidencia en gradle-configuration-cache-evidence/ |
| --- | --- | --- |
| Antes: `gradlew validateReleaseConfiguration --configuration-cache` | Dos fallos: firma ausente y captura del script; cache descartado con 1 problema | original-validation.log |
| Después: `gradlew help --configuration-cache`, primera vez | `BUILD SUCCESSFUL`; cache almacenado | help-cache-first.log |
| Mismo comando, segunda vez | `BUILD SUCCESSFUL`; `Reusing configuration cache` y `Configuration cache entry reused` | help-cache-second.log |
| `gradlew validateReleaseConfiguration --configuration-cache`, primera vez | Bloqueo por firma ausente; cache almacenado sin problema de serialización | validation-cache-first.log |
| Mismo comando, segunda vez | Mismo bloqueo de firma; acción ejecutada y cache reutilizado | validation-cache-second.log |
| `gradlew assembleRelease --configuration-cache --rerun-tasks` | `BUILD FAILED` por firma ausente; cache del grafo release almacenado sin problemas | release-cache.log |
| `gradlew assembleRelease --no-configuration-cache --rerun-tasks` | `BUILD FAILED` por el mismo requisito de firma | release-no-cache.log |
| `gradlew validateReleaseConfiguration --configuration-cache -PgboValidationBuild=true` | Rechazo esperado `PRODUCTION_BLOCKED`; cache almacenado | validation-build-blocked.log |

La prueba de la tarea real, además de `help`, demuestra que su acción se puede
serializar y reconstruir conservando el bloqueo. Las comprobaciones de apertura
de clave y certificado se mantienen en el código, pero **no se ejecutaron con
una keystore histórica válida**, porque no está configurada en este equipo.

## Suite y alcance

Se ejecutó una línea base nueva, antes de editar el build:
`gradlew testDebugUnitTest --no-configuration-cache`. Resultado: 380 pruebas,
365 aprobadas y 15 fallidas. El comando devuelve fallo por esas pruebas;
no se presenta como una suite completamente aprobada.

Después del cambio se repitió exactamente
`gradlew testDebugUnitTest --no-configuration-cache`, sin el flag de build de
validación. Resultado: **380 pruebas, 365 aprobadas y los mismos 15 fallos**.
La comparación por `classname#testcase` confirma **0 fallos nuevos, 0 casos
faltantes y 0 resultados cambiados**. El baseline tardó 3m 11s y la ejecución
final 6m 1s; ambas terminaron `BUILD FAILED` por esos 15 fallos existentes.
No se modifican tests funcionales ni expectativas. Evidencias:
`baseline-tests.log`, `baseline-tests.json`, `final-tests.log` y
`final-tests.json`.

Se registraron hashes de 608 archivos recibidos. La comprobación reproducible
`tools/verify_gradle_cache_fix.py scope` identifica únicamente
`app/build.gradle.kts` como modificado dentro de ese conjunto. No hay archivos
añadidos en los directorios protegidos. Código y tests Android, esquemas Room,
backend, Google Apps Script y wrapper Gradle permanecen idénticos. Los JSON de
línea base y comparación contienen identidades individuales de las pruebas;
los logs conservan los resultados reales. La misma comprobación confirma que
el contenido de `app/build.gradle.kts` fuera de los imports de la tarea y del
bloque de registro de `releaseConfigurationCheck` permanece idéntico.
`scope-verification.json` registra este resultado y `cache-verification.json`
resume las siete ejecuciones posteriores del mecanismo de validación/cache.

El único script auxiliar nuevo es `tools/verify_gradle_cache_fix.py`; registra
resultados XML y comprueba hashes, sin editar tests, leer configuración privada
ni alterar Gradle. El resto de archivos añadidos son este informe y sus
evidencias.

## Identidad y límites

Se conserva el certificado esperado SHA-256:
`0da8a2cbcf6bce5554526c80de08878d077af1542e488292b3869cdd8ee17315`.
Esta huella es el requisito de identidad existente, no una verificación de un
nuevo APK. No se genera una keystore ni se modifica la política release.

El `versionCode` final es **14**, igual al de la fuente recibida. La política
existente exige incrementar cada APK distribuida; no documenta una distribución
de esta nueva build que justifique incrementar únicamente por este cambio de
script. El applicationId, versionName, signingConfig y pin de certificado no
cambian.

**El requisito de release `BUILD SUCCESSFUL` sigue pendiente.** En este equipo
faltan las cuatro propiedades `GBO_RELEASE_*` y la keystore histórica referida;
también falta `server.local.properties` con `SERVER_BASE_URL` autorizado. El
primer bloqueo se produce en la firma; no se ha saltado para comprobar los
controles siguientes. La configuración privada de Drive existente se conserva.

No existe `app/build/outputs/apk/release/app-release.apk` generado por estos
comandos. Por tanto, no se ejecutan `apksigner` ni `zipalign` sobre un APK anterior
o debug como sustituto. No hay certificado de un nuevo release verificado ni
se declara un release listo para distribución.

Después de configurar localmente la firma histórica y el servidor autorizado,
se deben repetir **ambos** comandos de `assembleRelease` de la tabla. El gate
existente `verifyReleaseIdentity` debe validar package, versión y certificado.
Sobre el nuevo APK, ejecutar además con Build Tools del SDK:

```powershell
apksigner verify --verbose --print-certs app/build/outputs/apk/release/app-release.apk
zipalign -c -v 4 app/build/outputs/apk/release/app-release.apk
```

No se incluyen valores privados, archivos locales, cache de configuración ni
keystore en un entregable fuente. No se ha generado un nuevo source ZIP.

No hay commit ni push. Ninguna configuración privada se incorpora a este
informe ni a sus evidencias.
