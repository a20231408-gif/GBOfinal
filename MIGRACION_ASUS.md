# Migración Huawei → ASUS TUF F16

Estado: preparación local; **sin commit ni push y sin rama publicada todavía**.

Preparación iniciada el 29 y comprobaciones continuadas el 30/10/2026 (Lima). Se conserva el nombre de rama solicitado. El inventario revisable está en `docs/migracion-asus-20261029/ARCHIVOS_PROPUESTOS.md`.

## Identidad de la base

- Repositorio: https://github.com/a20231408-gif/GBOfinal
- Visibilidad comprobada: **público**. Antes de publicar código empresarial, el propietario debe decidir si acepta esa visibilidad o autoriza un cambio separado a privado.
- Rama preparada: `codex/migracion-asus-20261029`.
- Commit base: `607d499300859321aa7a282b10ee7dfde17d14f8`; coincide con `origin/checkpoint/gbo-2026-10-23`.
- Commit de migración: pendiente de autorización; se comunicará su hash completo después de crearlo. Antes de eso, el contenido propuesto se identifica por su inventario y árbol Git, no como un commit ya publicado.
- Checkpoint conservado: tag anotado `checkpoint-pre-redisenio-visual-2026-10-14`, commit `5de1242581dc460ea38d45006020d022f8d4da78`.
- La rama histórica `codex/observaciones-campo-manual-10I` ya apuntaba a `aab9cf95f4f049418bbf1ebf2563f33373e843a5` antes de la migración. No se movió ni se intentó devolverla al checkpoint.
- `main` remoto se conserva; no existía una rama local main y no se creará ni cambiará.

## Versión y contenido

Fuentes Android **1.4.18 (32)**, `com.tuempresa.inventariovial`, Room 21. Se conservan las candidatas 1.4.17 (31) y 1.4.18 (32) fuera de Git. La 32 incorporó: exportaciones SIC-17/17A/17B incompletas y consolidados locales por ruta; SCAP de 20 categorías, calificaciones y recuperación; copias locales/ZIP grandes y acceso a archivos; SIC-20 y Pavimentos/PDF; cola e historial Drive; SIC de Pavimentos sin PR obligatorio; cronología local; varias juntas 26 con global PCI manual independiente.

Los cambios de migración no modifican funcionalidades Android, versión, firma, UUID, contratos ni datos. Solo preparan documentación, exclusiones de Git, ejemplos sin identificadores privados y rutas de herramientas/salidas de tests.

Estructura necesaria: `app/src`, `app/schemas` (históricos y 21), `docs/reference` (tres XLSX SCAP usados por Gradle), `tools/*field_map.json`, `buildSrc`, `gradle`, wrapper (`gradlew`, `gradlew.bat`, `gradle-wrapper.jar`), Gradle Kotlin DSL y catálogos de versiones. No se sustituyen los XLSX contractuales ni los recursos binarios por omisión general de ZIP/JSON.

## Herramientas comprobadas en Huawei

- Windows; Intel Core i5-1155G7 y 8 GB RAM (hardware declarado por el propietario). GPU Intel Iris Xe.
- Android Studio **AI-261.26222.65.2614.16379836**, directorio de datos AndroidStudio2026.1.4.
- JBR/OpenJDK **25.0.3**, build `25.0.3+-15898627-b508.16`.
- Gradle Wrapper **9.6.0**; SHA-256 de distribución fijado en `gradle/wrapper/gradle-wrapper.properties`.
- Android Gradle Plugin **9.4.1**; Kotlin/Compose plugin **2.2.10**; KSP **2.2.10-2.0.2**; Compose BOM **2026.02.01**.
- compileSdk/targetSdk **37**, minSdk **26**; plataforma instalada `android-37.0`; Build Tools instaladas 34.0.0, 35.0.0 y 36.0.0. El proyecto deja que AGP elija Build Tools.
- Pruebas locales seleccionadas: Robolectric 4.16.1 sobre SDK **28 y 34**; requiere ambos JAR instrumentados, aparte del SDK de compilación. El ejecutor reúne copias verificadas de ambos en una caché plana privada para el modo offline.
- Git 2.51.2.windows.1; Node.js v24.19.0 para pruebas opcionales del backend; Python 3.12+ para herramientas portables (el benchmark usa solo biblioteca estándar y APIs Windows).

Las rutas de JDK/SDK son específicas de cada laptop: no se trasladan como configuración global del proyecto.

## Clonar en ASUS (solo DESPUÉS de publicar con autorización)

Utilizar una ruta corta fuera de OneDrive, por ejemplo `C:\GBO`. No sobrescribir carpetas existentes.

```powershell
git clone --branch codex/migracion-asus-20261029 https://github.com/a20231408-gif/GBOfinal.git C:\GBO
Set-Location C:\GBO
git status --short --branch
git rev-parse HEAD
git rev-parse 'checkpoint-pre-redisenio-visual-2026-10-14^{}'
```

Comparar HEAD con el hash de migración aprobado. Antes del push esa rama todavía NO se puede clonar desde GitHub. No usar el commit base 607d499 como si incluyera los cambios locales 1.4.18.

Instalar Android Studio/JBR, SDK 37 y Build Tools compatibles mediante su SDK Manager; aceptar las licencias en la ASUS. Para las pruebas instalar/cargar SDK 34 y permitir inicialmente la descarga pública de dependencias Robolectric. No copiar caches como si fueran fuentes necesarias.

## Configuración privada: traslado separado

**Nunca GitHub, chat, correo sin protección ni repositorios de ejemplos.** El respaldo de Huawei incluye un archivo local privado con ACL restringida al usuario y SYSTEM. No es un ZIP cifrado: para trasladarlo usar un medio cifrado o canal seguro autenticado. El propietario conserva las contraseñas en su gestor privado; no se solicitan aquí.

Transferir únicamente lo necesario:

1. `server.local.properties`: servicio de usuarios y flags existentes. Copiar valores reales por el canal privado; no crear otro login ni modificar el Servicio de Usuarios.
2. `drive.local.properties`: URL /exec y credencial existentes. No sustituirlos por los ejemplos vacíos ni cambiar despliegues.
3. Keystore histórico y `keystore.properties`: conservar alias y contraseñas privados. En la ASUS cambiar únicamente `GBO_RELEASE_STORE_FILE` para resolver la ubicación nueva; preferir una ruta local relativa bajo una carpeta ignorada. No generar otra clave.
4. Código/configuración privados del backend y paquetes Google de prueba, si se continuará ese trabajo. Conservar sus respaldos separados de Git.
5. Opcional: `debug.keystore` de Huawei, mediante el mismo canal seguro, solo si es imprescindible actualizar una instalación Debug previa. La firma Debug por defecto cambia entre PCs y no sustituye la firma histórica Release.

Respaldo privado en Huawei: `D:\Downloads\GBO_RESPALDO_MIGRACION_20261029_041941Z`. `GBO_FUENTES_GIT_Y_CONFIG_PRIVADO.zip` conserva fuentes, historia Git, configuración y firma; `TRASLADO_PRIVADO_ASUS_NO_SUBIR_A_GITHUB.zip` reúne la configuración necesaria, clave, referencia privada y configuraciones de backend. `APKS_31_32_NO_SUBIR_A_GITHUB.zip` conserva ambas candidatas, incluida la 32 cuyo nombre local actual es `GBO_Ruta_Clara_1.4.18.apk`. Ninguno está cifrado: las ACL locales restringidas no sustituyen el cifrado durante el traslado. No subir estos archivos al repositorio.

Crear `local.properties` nuevo con `sdk.dir` de la ASUS; no copiar la ruta de Huawei. Si ya existen archivos locales, comparar sus hashes localmente y conservar ambas copias antes de reemplazar. No imprimir contenidos en consola.

Configurar para la terminal:

```powershell
$env:JAVA_HOME = '<ruta del JBR instalado en ASUS>'
$env:ANDROID_SDK_ROOT = '<ruta del Android SDK en ASUS>'
$env:PATH = "$env:JAVA_HOME\bin;$env:ANDROID_SDK_ROOT\platform-tools;$env:PATH"
```

Ejemplo local no secreto: `sdk.dir=C:/ruta/al/Android/Sdk`. Las propiedades privadas usan las claves descritas en `*.properties.example`; dichos ejemplos no contienen endpoints ni tokens reales. Si falta configuración, una Debug puede compilar y quedar sin conexión; no equivale a haber verificado los servicios reales.

Clasp debe autorizarse de nuevo en la ASUS cuando se necesite: **no trasladar `.clasprc.json` ni tokens OAuth**. Mantener `.clasp.json`/Script ID en configuración privada. Ningún comando de este documento publica Apps Script.

## Compilar y verificar

Configuración ordinaria, sin `gboValidationBuild` ni flags retirados de fotografías:

```powershell
.\gradlew.bat :app:assembleDebug
.\gradlew.bat :app:validateReleaseConfiguration
```

Para Release se necesita el keystore histórico y la configuración legítima completa:

```powershell
.\gradlew.bat :app:assembleRelease
```

La tarea existente `verifyReleaseIdentity` verifica paquete, versionCode y certificado con Android Build Tools. Certificado histórico SHA-256 público: `0da8a2cbcf6bce5554526c80de08878d077af1542e488292b3869cdd8ee17315`. No instalar ni distribuir automáticamente. Una compilación nueva no garantiza APK idéntica en bytes: influyen herramientas, configuración local, firmas Debug y artefactos generados. Verificar identidad y SHA de cada APK.

Los auxiliares antiguos `tools/verificar_*_20261019.py` que compilan contra `worker-args-original.txt` requieren caches/artefactos históricos que no se publican. No son el procedimiento para reconstruir GBO en ASUS: usar Gradle y el ejecutor portable de esta migración.

## Backend: desplegado frente a preparado

- GBO_Inventario desplegado conocido: **V21**, SCAP panel 2. Su copia íntegra y configuración son un respaldo privado independiente; Git no las sustituye.
- `backend/referencia-v21/` conserva los ocho módulos puros y el manifiesto de esa exportación inmutable; `Codigo.gs.example` contiene su lógica completa con la raíz privada omitida. `PROCEDENCIA.json` registra hashes y qué archivo fue saneado. Es la referencia identificada como V21, no una carpeta desde la que publicar.
- `google-apps-script/Code.gs` y carpetas históricas son referencias, no una autorización para reemplazar producción; identificadores privados de esas referencias se sustituyeron por marcadores.
- Fuentes puras V21 de las pruebas SCAP se conservan; los paquetes Google que contenían una raíz real de pruebas llevan copias `.example` completas, con raíz omitida. Los originales configurados permanecen en Huawei/respaldo privado y deben restaurarse de forma segura si se necesita aquel ensayo Google. Las plantillas no son un proyecto Google listo para publicar.
- La simulación local usa una raíz ficticia de memoria, no una carpeta Google.
- Optimizaciones de SyncV3/SyncV3Core y consolidación cronológica de Pavimentos continúan como **propuestas NO PUBLICADAS**. La cronología remota requiere conciliación histórica y prueba Google; V21 actual admite el respaldo SIC documental individual según los ensayos locales, no el nuevo consolidado por fecha.
- No modificar GBO - Servicio de Usuarios, ROOT_FOLDER_ID, URL, propiedades ni permisos para migrar de PC.

No se descargaron ni se certificaron los valores de ScriptProperties de Google en esta migración. El respaldo administrativo de esas propiedades corresponde al propietario, separado del repositorio y sin publicarlas. La referencia local de V21 no sustituye una descarga autorizada del proyecto vigente antes de trabajar de nuevo en el servidor.

## Pruebas y limitaciones conocidas

La candidata 32 anterior a la migración tuvo 94 casos focalizados Android/Room/Compose y 6 simulaciones backend aprobados. Cinco casos históricos de AutomaticDocuments149Test/Engineering2DataTest seguían sin conciliar; no se declara PASS de toda la suite ni aprobación de campo. Permanecen documentados los tres ejemplos XLSX ficticios regenerados por un test anterior; sus salidas se aislaron. No se borraron datos reales.

Selección reproducible de esta migración: `tools/migracion/tests_criticos.json`. El ejecutor `tools/migracion/benchmark_windows.py` utiliza rutas locales y opciones temporales de memoria; no cambia funcionalidades ni omite verificaciones de configuración. Las pruebas locales no acreditan entregas Google ni PDF físico en tablet.

La prueba histórica instrumentada `DriveV4RealIntegration8BTest` conserva el opt-in `gboDrive8B=true` y ahora exige además `gboAuthorizedTestRoot` antes de hacer peticiones. Se retiró el identificador incrustado. No activarla en las tablets de campo: necesita un proyecto y raíz de pruebas autorizados, configuración privada y revisión independiente. Esta migración solo comprueba compilación, sin ejecutarla.

Los paquetes históricos Google que requieren originales/configuraciones privadas no se presentan como ensayos ejecutables de inmediato en un clon público. Sus copias `.example` y fuentes se conservan; el procedimiento ordinario de compilación Android y las pruebas seleccionadas no dependen de esas configuraciones.

Los manifiestos/hashes de entregas históricas describen los paquetes originales, no las copias públicas saneadas. Conservar aquellos paquetes en el respaldo privado; regenerar sus hashes después de cualquier preparación futura, sin interpretar una plantilla como despliegue aprobado.

## Benchmark Huawei / comparación ASUS

Mediciones reales en la Huawei; tiempos de pared del proceso Gradle completo:

| Fase | Resultado | Tiempo | Pico RSS aproximado | Pico memoria privada comprometida |
| --- | --- | --- | --- | --- |
| Debug con salidas frías | PASS | 402.02 s | 2646.9 MiB | 3075.8 MiB |
| Debug incremental, comentario | PASS | 196.59 s | 2199.0 MiB | 2610.6 MiB |
| 52 pruebas, compilación caliente | PASS | 132.62 s | 1922.4 MiB | 2768.0 MiB |
| Configuración Release real | PASS | 18.27 s | 566.2 MiB | 664.9 MiB |
| Compilar tests Android, sin ejecutarlos | PASS | 47.06 s | 1305.8 MiB | 1360.3 MiB |

Las 52 pruebas terminaron sin fallos, errores ni omitidas. La primera tentativa duró 429,95 s: 32 aprobadas y 20 fallidas porque el ejecutor offline solo ofrecía el JAR de Android 34 a pruebas que usaban Android 28. Se preservaron logs/XML/JSON; se corrigió únicamente la caché del ejecutor, usando ambos JAR ya disponibles. La repetición exitosa tuvo compilación de tests caliente; no se presenta como tiempo de una primera compilación de tests.

No se repitió `assembleRelease` durante la migración: el ensamblado anterior de la candidata 32 ya pasó en 4.047,7 s tras un fallo inicial de memoria nativa de R8. Se revalidó su configuración en la copia aislada, y se verificaron nuevamente paquete, versión, firma histórica y alineación 16 KiB del APK conservado. Sus 609 entradas de fuentes de ejecución/compilación registradas coinciden sin cambios; las 641 entradas auditadas al iniciar la migración también permanecen intactas.

Release conservada: **1.4.18 (32)**, `com.tuempresa.inventariovial`; SHA-256 `3fe18f61f1b3455e51553b263292e40dc7d593cd7df5c6ad2c58e848c2a3edc7`. No se generó una nueva candidata ni se instaló nada.

Advertencias conservadas: Kotlin/KSP comunica fallback interno de target JDK 25 a 24; el proyecto mantiene su configuración JVM 11. También aparecen advertencias de acceso nativo de Conscrypt y APIs obsoletas. No se cambiaron versiones de herramientas durante la migración.

Detalles de cada fase y recuentos por clase: `docs/migracion-asus-20261029/BENCHMARK_HUAWEI.json`. Logs y XML completos permanecen privados en `D:\GBO_MIG_20261029\measurements`, incluida la tentativa fallida.

Procedimiento: exportación nueva del árbol propuesto a una carpeta corta aislada, sin `app/build` ni caches del proyecto. No se ejecuta `clean` en Huawei ni se borra su proyecto. Se reutiliza la cache global de dependencias y se trabaja offline; por tanto, «limpia» significa salidas de compilación frías y dependencias calientes, no primera instalación ni indexación de Android Studio.

El perfil conservador limita a 1 worker, 2 procesadores visibles y heap Gradle de 2048 MiB; repetir exactamente ese perfil en ASUS para una comparación controlada. Un ensayo posterior con todos los procesadores debe informarse por separado. El muestreo de memoria suma working set observado de los procesos descendientes; no mide RAM exclusiva y puede contar páginas compartidas dos veces.

El incremental agrega/restaura un comentario únicamente en la copia aislada; no modifica la lógica ni el proyecto Huawei. Las pruebas usan caches Robolectric existentes; si faltan, realizar una preparación online fuera de las mediciones y repetir offline. Un fallo se conserva como fallo, sin inventar tiempos ni éxito.

Para repetir en ASUS, después de clonar el commit aprobado y configurar el proyecto, calentar dependencias y compilar previamente los tests fuera de la medición (si ya están disponibles, no descargar):

```powershell
python tools/migracion/benchmark_windows.py --project C:\GBO --report C:\GBO_MEDICIONES_PREPARACION --phase tests --allow-downloads
```

Este calentamiento solo ejecuta las clases locales seleccionadas. No ejecuta instrumentación ni los ensayos Google. Si falta una dependencia, resolver su instalación antes de medir, conservando el resultado fallido.

Crear una exportación NUEVA del mismo commit. No reutilizar una carpeta con `app/build`, ni ejecutar `clean` sobre la copia principal:

```powershell
$gboBench = 'C:\GBO_MIG_ASUS_20261029'
if (Test-Path -LiteralPath $gboBench) { throw 'Elegir otra carpeta nueva; no borrar la existente.' }
New-Item -ItemType Directory -Path "$gboBench\source"
git -C C:\GBO archive --format=tar --output="$gboBench\fuentes.tar" HEAD
tar -xf "$gboBench\fuentes.tar" -C "$gboBench\source"
```

Copiar a esa exportación los archivos locales ya configurados en ASUS, únicamente después de comprobar que no existen: `local.properties`, `server.local.properties` y `drive.local.properties`. Para `release-check`, restaurar además la firma y `keystore.properties` con una ruta válida local. No copiar carpetas `build`, cachés del proyecto, endpoints nuevos ni valores a la consola. El ejecutor escribe evidencias únicamente dentro de la exportación/mediciones.

Ejecutar secuencialmente con el mismo JBR y el perfil del script:

```powershell
python C:\GBO\tools\migracion\benchmark_windows.py --project "$gboBench\source" --report "$gboBench\mediciones" --phase cold
python C:\GBO\tools\migracion\benchmark_windows.py --project "$gboBench\source" --report "$gboBench\mediciones" --phase incremental
python C:\GBO\tools\migracion\benchmark_windows.py --project "$gboBench\source" --report "$gboBench\preparacion-tests" --phase tests
python C:\GBO\tools\migracion\benchmark_windows.py --project "$gboBench\source" --report "$gboBench\mediciones" --phase tests
python C:\GBO\tools\migracion\benchmark_windows.py --project "$gboBench\source" --report "$gboBench\mediciones" --phase release-check
```

El primer `tests` de la exportación prepara su compilación y se informa aparte. El segundo mide la suite con compilación caliente; `Test` se fuerza a ejecutar, por lo que no se mide un simple `UP-TO-DATE`. Separar descargas, compilación de tests y ejecución evita comparar condiciones distintas. La comprobación opcional `--phase instrumentation-compile` compila tests Android, **no los ejecuta ni instala APK**. Las mediciones excluyen indexación del IDE, tiempo de red Google y operaciones de dispositivos; otras aplicaciones abiertas no fueron controladas. No atribuir toda la RAM del equipo a Gradle.

## Continuar con Codex

Abrir `C:\GBO` como proyecto local en Codex y Android Studio. Pedir primero lectura de este documento, estado Git y problemas pendientes. Confirmar firma/configuración privada local sin mostrar valores. No distribuir cambios nuevos reutilizando una numeración ya distribuida ni instalar sobre una firma diferente desinstalando la aplicación. Reconstruir la misma versión para verificar herramientas no autoriza distribuirla.

No trasladar bases de datos/tablets mediante Git. La identidad de las tablets vive en sus datos Android; no se cambia por clonar el proyecto en otra laptop. Autorizar ADB de la ASUS manualmente si se usa después, sin desinstalar GBO ni resetear su identidad.

## Control de publicación

El contenido propuesto pasó la revisión de 1.485 archivos y 789 entradas comprimidas: cero hallazgos Gitleaks y cero coincidencias con los valores privados conocidos, incluidos metadatos de proyecto/despliegue de 19 archivos privados. Se revisó también el contenido ya rastreado, no solo .gitignore. Esto no constituye una garantía universal frente a cualquier secreto desconocido.

Gitleaks 8.30.1 revisó el historial: un hallazgo fue prosa sin credencial literal. La búsqueda exacta de los secretos actuales en 1.930 blobs históricos no encontró coincidencias. Sí hay identificadores privados de carpetas/antiguos despliegues en el historial público; ocho blobs contienen el ID de la raíz V21, incluyendo una prueba histórica. Un ID no es una credencial ni concede acceso por sí solo, pero sanear el árbol actual no lo retira del historial. No se reescribió historia, rotaron credenciales ni cambiaron permisos. El propietario debe decidir la visibilidad empresarial antes de autorizar el push; cambiarla sería una acción separada.

No se ha creado commit ni push. Antes de aprobar se entregará inventario de nuevos/modificados/retirados solo del índice, exclusiones, resultados de seguridad, pruebas/benchmark y árbol Git propuesto. Los archivos privados o descartados del índice se conservan en disco y en el respaldo. El propietario debe autorizar expresamente commit y publicación únicamente de esta rama. No se tocará main ni se usará force push.
