# Entrega completa para colaborador — GBO Inventario Vial

## Código que debe descargar

- Repositorio: `https://github.com/a20231408-gif/GBOfinal`
- Rama: `codex/dashboard-sesion-6`
- Checkpoint funcional de Prompt 6: `3afcb57c156dcc9489c2eba8a5984005465d37f9`
- El hash final publicado se comprueba después de clonar con `git rev-parse HEAD`; debe coincidir con `origin/codex/dashboard-sesion-6` y con el informe final de esta entrega.

```powershell
git clone --branch codex/dashboard-sesion-6 --single-branch https://github.com/a20231408-gif/GBOfinal.git GBO_CLON_VERIFICACION_6
cd GBO_CLON_VERIFICACION_6
git rev-parse HEAD
```

La rama contiene en orden los checkpoints de SIC-08/09 y Prompts 1–6. No iniciar trabajo desde `main` ni rehacer esas etapas.

## Abrir y compilar

1. Abrir la carpeta raíz clonada en Android Studio.
2. Usar el JDK incluido en Android Studio. La validación de esta entrega se hizo con `D:\Android Studio\jbr`; el código compila con compatibilidad Java 11.
3. Instalar Android SDK Platform 37 y Build Tools recientes. Definir `sdk.dir` en un `local.properties` local o `ANDROID_HOME`.
4. Sin credenciales, validar con:

```powershell
.\gradlew.bat :app:assembleDebug -PgboValidationBuild=true
```

5. Para desarrollo local ordinario, copiar los archivos `.example` a sus nombres sin `.example` y completar únicamente valores autorizados. No agregar esos archivos reales a Git.

Las dependencias externas se descargan desde Google Maven, Maven Central y Gradle Plugin Portal. El Gradle Wrapper fijado es 9.6.0 y verifica su SHA-256. La aplicación usa AGP 9.4.1, Kotlin 2.2.10, KSP 2.2.10-2.0.2 y Compose BOM 2026.02.01.

## Pruebas

Ejecutar únicamente pruebas focalizadas de la función en desarrollo. Para repetir las del dashboard:

```powershell
.\gradlew.bat :app:testDebugUnitTest --tests "*Dashboard6*" -PgboValidationBuild=true
```

La suite completa no forma parte de esta transferencia. Antes de un Release debe configurarse la firma histórica fuera de Git y ejecutarse la validación de identidad prevista por `buildSrc` y `tools/apk_identity_audit.ps1`.

## Versiones y contratos conservados

- applicationId: `com.tuempresa.inventariovial`
- versionCode: 16
- versionName: 1.4.2
- Room: 18, con `photo_history` y esquemas versionados hasta `18.json`
- Drive: syncProtocol 3, pathVersion 4 y PhotoCategoryVersion 1
- Drive V4: jerarquía ruta/familia/elemento numerado, sin carpetas SIB nuevas

El backend está en `google-apps-script/`: `Code.gs`, `SyncV3.gs`, `SyncV3Core.js` y pruebas Node. Incluye entrega técnica sin GPS, identidad y revisión estable, control de concurrencia 2B, consolidación sin sobrescribir otras tablets, familias sin SIB y categorías fotográficas. No mezclar este Apps Script con el servicio de usuarios.

## Configuración privada excluida

No están en Git: `local.properties`, `drive.local.properties`, `server.local.properties`, `keystore.properties`, keystores/certificados, contraseñas, tokens, APK/AAB, caches ni carpetas `build`. Los archivos `drive.local.properties.example` y `server.local.properties.example` documentan nombres de propiedades sin valores reales.

Para pruebas Drive se requiere un despliegue independiente del Apps Script con protocolo 3, pathVersion 4 y PhotoCategoryVersion 1, URL `/exec`, token propio y raíz de Drive aislada. La última comprobación histórica del despliegue de pruebas fue pathVersion 3; por ello la subida real de los cambios 2A–6 sigue pendiente hasta actualizar o crear un despliegue aislado compatible. No usar ni publicar producción.

## Estado de desarrollo

1. SIC-08/09: captura independiente y checkpoint protegido.
2. Prompt 1: guardado y sincronización técnica sin GPS, cola offline/reintentos y ACK verificable.
3. Prompt 2A: rutas/familias/elementos numerados, sin SIB para V4, orden técnico y consolidación.
4. Prompt 2B: edición por UUID/fileId, revisiones, conflictos, idempotencia e historial remoto.
5. Prompt 3: GPS opcional, observaciones, validaciones PR/distancia y formularios simplificados.
6. Prompt 4: SIC-18A, longitudes sin equivalencias inventadas, materiales y posiciones.
7. Prompt 5: captura múltiple, categorías, galería, reemplazo/retiro e historial `photo_history`.
8. Prompt 6: dashboard, sesión, navegación, borradores diarios/históricos y orden por progresiva.

La trazabilidad detallada está en `docs/CODEX_HANDOFF.md` y en las carpetas de entrega por etapa. Las matrices SIC están en `docs/drive-familias-2a/MATRIZ_SIC.md`, `docs/formularios-sic-3/MATRIZ_SIC.md`, `docs/correcciones-tecnicas-4/` y `docs/fotografias-5/MATRIZ.md`.

## Pendientes y riesgos

- Prompt 6 no fue validado físicamente en tablet; quedan rotación, fuente ampliada, lector de pantalla y navegación real por todas las herramientas.
- No se hizo una subida Drive real de 2A–6 contra pathVersion 4. El Apps Script aislado de pruebas debe actualizarse antes.
- SCAP usa tres plantillas XLSX oficiales grandes y tres mapas JSON registrados en Git; no reemplazarlos por archivos vacíos.
- `app/src/main/assets/road_reference.json` es el archivo rastreado más grande del código y forma parte del catálogo vial.
- No cambiar firma histórica, applicationId, versionCode, Room ni contratos Drive como parte del Prompt 7.
- Los datos de varias tablets requieren respetar UUID, `baseRevision`, ACK y conflictos existentes; no reconstruir el consolidado remoto desde una sola tablet.

## Siguiente encargo

Continuar con [PROMPT_7_COLABORADOR.md](PROMPT_7_COLABORADOR.md): flujo posterior a guardar y accesos directos a ficha, fotografías y Excel real. Se debe reutilizar la persistencia, revisión, exportación y sincronización ya presentes. No rehacer Prompts 1–6.
