# Recuperar Android sin depender de la conversación

## Herramientas y fuente

Mantener versiones fijadas, no actualizar por iniciativa del IDE: Android Studio 2026.2.2, JBR Java 25, Gradle Wrapper 9.6.0 (SHA de distribución fijado), AGP 9.5.0, Kotlin 2.2.10, KSP 2.3.6, Compose BOM 2026.02.01, Room 2.8.5 y WorkManager 2.11.2. Compile/target SDK 37, min SDK 26, compatibilidad fuente/bytecode Java 11. ASUS dispone de plataforma `android-37.0`, Build Tools 36.0.0 y Platform Tools. JDK ejecutor 25 y bytecode Java 11 son configuraciones distintas, no una contradicción. Node 24.22.1/Python 3.12.10 se usaron para herramientas locales backend; no son necesarios para copiar el paquete revisado en el editor manualmente.

```powershell
git clone --single-branch --branch codex/scap-pavimentos-cierre-20261103 https://github.com/a20231408-gif/GBOfinal.git C:\GBO_RECUPERADO
Set-Location C:\GBO_RECUPERADO
git rev-parse HEAD
git ls-remote origin refs/heads/codex/scap-pavimentos-cierre-20261103
git merge-base --is-ancestor f7ff3e0062899602cc919cebb94739e972cd1732 HEAD
git switch -c codex/continuidad-nombre-fecha
```

Registrar el SHA remoto recibido del mantenedor. No modificar main, no fusionar PR. Alternativa sin red: verificar el SHA-256 del bundle recibido y `git bundle verify RUTA.bundle`, clonar el bundle con la misma rama y comprobar su tip contra el manifiesto. Un bundle privado no se sube a GitHub.

## Compilar sin secretos ni producción

Crear `local.properties` ignorado, con `sdk.dir` del SDK de esa laptop. SDK Manager permite instalar la plataforma necesaria. Configurar el JDK de Gradle al JBR de Android Studio.

```powershell
$env:JAVA_HOME='C:\Program Files\Android\Android Studio\jbr'
.\gradlew.bat --version
.\gradlew.bat '-PgboValidationBuild=true' :app:compileDebugKotlin :app:compileDebugJavaWithJavac --console=plain
```

Si faltan dependencias cacheadas, se necesita Internet para Maven/Gradle, no acceso a Google productivo. Con el cache completo puede añadirse `--offline`. El perfil de validación omite endpoints/tokens, usa la firma Debug local y NO sirve para actualizar dispositivos de campo. Los ejemplos de raíz `server.local.properties.example`, `drive.local.properties.example` y `config-ejemplos/` no contienen configuración productiva. No deducir flags contractuales de un ejemplo vacío.

No se requiere tablet, bases reales, keystore histórico ni cuenta GBO para editar y compilar fuentes con ese perfil. Para futuras regresiones V2 usar el init-script existente `tools/validacion/v2-mocks.init.gradle` junto con gboValidationBuild; no cambiar flags de producción para aprobar fixtures. Las 61 pruebas ya acreditadas no se repiten por transferir archivos.

## Configuración original y firmas

Solo un responsable autorizado recibe `PRIVADO/configuracion/portatil/{server.local.properties,drive.local.properties,keystore.properties}`. Copiarlos en la raíz del clon, siempre ignorados. El keystore Release se coloca en `.private-signing/historical-release.keystore`; la configuración portátil cambia únicamente su ruta y conserva privadamente los demás valores. No copiar `local.properties` de ASUS: crear el de la nueva laptop.

La firma Debug histórica y la Release son diferentes:

| Uso | Certificado SHA-256 esperado |
|---|---|
| Debug OPPO/Xiaomi autorizados | d2e54cb086aa731ef046be300977d0b16c0d8f66fdcb08d0d9cfd2038bf0d872 |
| Release histórica | 0da8a2cbcf6bce5554526c80de08878d077af1542e488292b3869cdd8ee17315 |

Para seleccionar la copia legítima Debug sin sobrescribir el keystore personal del compañero, usar el auxiliar de este kit:

```powershell
$env:GBO_DEBUG_KEYSTORE_PATH='C:\GBO_KIT\PRIVADO\firmas\huawei-debug.keystore'
.\gradlew.bat --init-script 'C:\GBO_KIT\PUBLICO\debug-signing.init.gradle' :app:signingReport --console=plain
```

Ese init-script cambia solo el archivo de firma Debug para la invocación; no contiene credenciales, no modifica el repositorio ni la Release. Comparar el certificado del bloque Debug antes de empaquetar. Si no coincide, detenerse. No sustituir Release por Debug ni al revés; no desinstalar para salvar incompatibilidad.

Cuando exista autorización para compilar una candidata nueva, el mismo init-script puede acompañar `:app:assembleDebug` SIN gboValidationBuild y con configuración legítima. Antes de una Release ejecutar `:app:validateReleaseConfiguration` y verificar su certificado. **Esta entrega no ejecutó otra compilación APK ni Release**. No garantizar APK byte idéntica al recompilar con otro entorno: verificar identidad/configuración/firma y generar un SHA nuevo; la APK autorizada existente tiene su hash propio.

## Comprobación limpia realizada

Se clonó GitHub en una carpeta nueva, se restauraron exclusivamente configuración legítima y ruta SDK, y se comprobó que Git ignora todas las propiedades y la firma. `:app:compileDebugKotlin :app:compileDebugJavaWithJavac --offline --console=plain` PASS, 13 tareas ejecutadas. Se usó SDK/JBR/cache global disponible de ASUS; no se copiaron salidas de compilación del repositorio original. Las advertencias existentes se conservaron en el log privado; no se modificó código para silenciarlas.

El APK actual es 1.4.32 (46), package `com.tuempresa.inventariovial`. Ruta privada y SHA en ESTADO_Y_RECURSOS.md. No generar otra versión ni otra APK si esta funciona. Instalación/distribución siempre exige autorización del propietario, identificación, respaldo vigente, actualización conservando datos y aislamiento; no repetir automáticamente el procedimiento del OPPO en otro dispositivo.
