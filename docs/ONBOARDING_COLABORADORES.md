# Incorporación a GBO

Base funcional común: **b3ed30eb19340d26abc909387964a1469b46e7f1**, GBO 1.4.22 (36).
Rama de integración: `codex/observaciones-campo-asus-20261030`.
El commit documental que añade esta guía es posterior; comprobar el SHA remoto antes de trabajar.
Leer `ESTADO_PARA_CONTINUAR.md`, `TAREAS_EQUIPO_GBO.md` y el informe de avance del 01/11.

## Obtener fuente y crear rama propia

```powershell
git clone --single-branch --branch codex/observaciones-campo-asus-20261030 https://github.com/a20231408-gif/GBOfinal.git C:\GBO
Set-Location C:\GBO
git fetch origin
git rev-parse HEAD
git ls-remote origin refs/heads/codex/observaciones-campo-asus-20261030
git merge-base --is-ancestor b3ed30eb19340d26abc909387964a1469b46e7f1 HEAD
git switch -c codex/colaborador-locales
```

Elegir uno de los nombres de rama del archivo de coordinación, añadiendo sufijo personal si existe.
No trabajar en main ni publicar en la rama de integración. No force push ni secretos en URLs.

## Android local sin acceso productivo

Instalar Git for Windows y Android Studio con JBR compatible (ASUS usa Java 25), Android SDK
37, Build Tools y Platform Tools. Mantener las versiones fijadas por el proyecto: wrapper
Gradle 9.6.0. Node.js es necesario para las pruebas backend. Abrir C:\GBO en Android Studio.
Crear `local.properties` con sdk.dir propio; ejemplo en `config-ejemplos/`.

```powershell
$env:JAVA_HOME='C:\Program Files\Android\Android Studio\jbr'
.\gradlew.bat --version
.\gradlew.bat '-PgboValidationBuild=true' :app:assembleDebug
.\gradlew.bat '-PgboValidationBuild=true' :app:testDebugUnitTest --init-script tools/migracion/pruebas.init.gradle --tests 'com.tuempresa.inventariovial.LocalCopyPresentationTest' --tests 'com.tuempresa.inventariovial.SicContributionPolicyTest'
```

El perfil existente gboValidationBuild omite endpoints y tokens locales: no copiar propiedades
productivas para estas tareas. Genera un Debug de validación, **no una candidata para tablets**.
Los flags quedan por defecto false, incluido V2: la selección histórica con perfil V2=true
no se acredita con este perfil. Añadir fixtures con transporte simulado y configuración explícita
según el contrato que se pruebe; no desactivar aserciones para aprobar fixtures incompatibles.
No es necesario keystore histórico: Debug usa la firma local generada por Android.
Las APK oficiales previamente preservadas están fuera del build y no deben reemplazarse con este Debug.
XML en `app/build/test-results/testDebugUnitTest/`; conservar resultados antes de otra ejecución.

## Backend sin Google

```powershell
node --test backend/candidato-original-reanudable/tests/DeliveryRootRouting.test.cjs backend/candidato-original-reanudable/tests/AuthorizedRootGuard.test.cjs backend/candidato-original-reanudable/tests/BridgeIdentity.test.cjs
```

Usa raíces ficticias en memoria; no despliega ni hace HTTP. `PreparedOriginal.test.cjs` requiere
un paquete privado y **no forma parte del arranque autónomo**. No pedirlo al colaborador común:
probar el módulo y adaptador con RootGuardMock y MockActual. Algunas regresiones de workbooks
requieren Python en PATH. Node/mocks y Robolectric no acreditan Drive ni validación física.

## Entregar un PR

```powershell
git status --short
git diff --check
git add -- <archivos-del-frente-revisados>
git diff --cached --name-status
git commit -m 'fix: problema reproducido y corregido'
git push -u origin codex/colaborador-locales
```

En GitHub abrir Compare & pull request con **base codex/observaciones-campo-asus-20261030**,
no main. Describir problema, cambio, pruebas efectivamente ejecutadas y pendientes. Revisión
del mantenedor antes de integrar; si el colaborador no tiene escritura, acordar fork/PR sin
compartir credenciales. No publicar APK, bundles, propiedades privadas, bases, fotos, logs reales
o el manual privado. No usar tablet personal, acceso Google ni datos reales como dependencia.
La matriz física vigente está en VALIDACION_MANUAL_GBO_20261101.md; no todos los casos están
aprobados. Producción sigue en el último estado acreditado V21.

Para regresiones que exigen nombres V2, usar el perfil explícito sin credenciales:

```powershell
.\gradlew.bat '-PgboValidationBuild=true' :app:testDebugUnitTest --init-script tools/migracion/pruebas.init.gradle --init-script tools/validacion/v2-mocks.init.gradle --tests '*SyncRecovery10DTest'
```

El init-script exige gboValidationBuild=true y habilita solo ese flag; no carga endpoints ni
tokens. No usar el Debug resultante como APK de campo. El Run ordinario no cambia.
