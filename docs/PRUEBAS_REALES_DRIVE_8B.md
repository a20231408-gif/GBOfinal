# Procedimiento reproducible y pendientes — Prompt 8B

## Condiciones y evidencias existentes

Leer [entrega](ENTREGA_8B.md), [matriz](MATRIZ_VALIDACION_8B.md) y [registro de archivos](integracion-real-8b/ARCHIVOS_DRIVE.md) antes de repetir: cada ejecución crea UUID nuevos y deja archivos ficticios en la raíz definitiva autorizada `1FQBVRD3BK1rOBfC8NdRzPQt3fvU40V23`. No borrar ni modificar registros de campo. No volver a ejecutar casos aprobados sin un cambio o incidencia concreta. No cambiar Apps Script ni publicar otro endpoint.

Configurar `DRIVE_WEB_APP_URL` y `DRIVE_API_TOKEN` únicamente en el archivo local ignorado `drive.local.properties`. No incluir credenciales en argumentos, documentación, capturas, logs o chat. Para usuarios, completar de forma local `server.local.properties` con el responsable del servicio independiente; no modificar ese proyecto desde esta tarea.

Debug conectado exige compilar **sin** `-PgboValidationBuild=true`. La APK conectada contiene configuración privada: no subirla ni compartirla. La APK de validación final es distinta y tiene los tres valores token/endpoint Drive/endpoint usuarios vacíos.

## Selección de dispositivo y compilación

En este equipo se comprobó la Samsung SM-T733 secundaria, pero su firma instalada no coincide con Debug. No utilizar desinstalación, wipe-data, instalación forzada ni borrado de Room para resolverlo. Se creó un emulador nuevo `GBO_8B_Controlado` (API 35); las pruebas se ejecutaron únicamente en `emulator-5558`. No usar un emulador que contenga datos anteriores con la configuración real: el arranque puede despertar entregas persistidas.

Los comandos siguientes se muestran para el entorno observado. Confirmar el serial del emulador antes de ejecutar; no copiar su selección a una tablet. Preparar la variable `$adb8b` con la ruta de platform-tools. JDK observado: `C:\Program Files\Android\Android Studio\jbr`.

```powershell
$env:JAVA_HOME='C:\Program Files\Android\Android Studio\jbr'
$env:JAVA_TOOL_OPTIONS='-Djdk.net.unixdomain.tmpdir=C:\PROYECTOS\GBO_FINAL\GBOfinal\tmp'
$adb8b='C:\Users\CORE I7\AppData\Local\Android\Sdk\platform-tools\adb.exe'
& $adb8b devices -l
.\gradlew.bat :app:assembleDebug :app:assembleDebugAndroidTest
& $adb8b -s emulator-5558 install -r app/build/outputs/apk/debug/app-debug.apk
& $adb8b -s emulator-5558 install -r app/build/outputs/apk/androidTest/debug/app-debug-androidTest.apk
```

Esperar a que termine la compilación antes de instalar; el primer intento de este trabajo instaló un test APK anterior durante regeneración. Verificar la identidad con `tools/apk_identity_audit.ps1`; applicationId, versionCode y firma deben mantenerse. El GET tiene que coincidir con la raíz autorizada y capacidades publicadas antes de cualquier escritura. El test principal impone esa comprobación.

## Casos reales opt-in

```powershell
& $adb8b -s emulator-5558 shell am instrument -w -e gboDrive8B true -e class 'com.tuempresa.inventariovial.DriveV4RealIntegration8BTest#missingAndIncorrectTokenAreRejectedByRealEndpoint' com.tuempresa.inventariovial.test/androidx.test.runner.AndroidJUnitRunner
& $adb8b -s emulator-5558 shell am instrument -w -e gboDrive8B true -e class 'com.tuempresa.inventariovial.DriveV4RealIntegration8BTest#realSicPhotosEditTwoStoresRetryConflictAndOfflinePersistence' com.tuempresa.inventariovial.test/androidx.test.runner.AndroidJUnitRunner
```

El segundo caso crea dos bases Room independientes y nuevos UUID en PE-22A. Exporta archivos reales, usa `AutomaticDriveHttp` y comprueba ACK/revisiones/conflicto; el wrapper solo provoca pérdida local de una respuesta ya recibida del servidor. No existe backend mock. Sus archivos de evidencia son `8b-capabilities-auth.json` y `8b-evidence.json` en el directorio externo de la app. Rechazo o timeout detiene la prueba: conservar evidencia y resolver únicamente la causa, sin bucles ilimitados.

Caso offline, en dos procesos y **solo emulador desechable**:

```powershell
& $adb8b -s emulator-5558 shell am instrument -w -e gboDrive8B true -e gboOfflinePhase create -e class 'com.tuempresa.inventariovial.DriveV4Offline8BTest#createValidatedRecordAndPhotoWithoutNetwork' com.tuempresa.inventariovial.test/androidx.test.runner.AndroidJUnitRunner
& $adb8b -s emulator-5558 shell am force-stop com.tuempresa.inventariovial
& $adb8b -s emulator-5558 shell am instrument -w -e gboDrive8B true -e gboOfflinePhase recover -e class 'com.tuempresa.inventariovial.DriveV4Offline8BTest#reopenWithoutNetworkThenRecoverThroughExistingWorkManager' com.tuempresa.inventariovial.test/androidx.test.runner.AndroidJUnitRunner
```

La primera fase desactiva Wi-Fi/datos y deja la red desactivada. La segunda verifica persistencia sin Internet, restablece red y espera hasta 180 s al worker existente, sin drenaje manual. Si falla entre fases, restablecer solo la conectividad del emulador de pruebas (`svc wifi enable`, `svc data enable`) y conservar la evidencia. No reiniciar create para resolver una recuperación del UUID existente.

Recuperar evidencia, sin credenciales:

```powershell
& $adb8b -s emulator-5558 pull /sdcard/Android/data/com.tuempresa.inventariovial/files/8b-evidence.json tmp/8b-evidence.json
& $adb8b -s emulator-5558 pull /sdcard/Android/data/com.tuempresa.inventariovial/files/8b-capabilities-auth.json tmp/8b-capabilities-auth.json
& $adb8b -s emulator-5558 pull /sdcard/Android/data/com.tuempresa.inventariovial/files/8b-offline-worker.json tmp/8b-offline-worker.json
```

Leer los archivos remotos identificados por ACK con Google Drive: comprobar carpeta de elemento/categoría, archivo global en EXCEL_SIC, MIME XLSX, hash de bytes y abrir ZIP/OOXML. Verificar filas y valores, no solo nombre/extensión. Comparar píxeles con JPEG local y hash original antes/después; la política existente agrega EXIF a la copia. En este trabajo se hizo con el plugin Google Drive y lectores ZIP/XML/PIL locales; evidencia saneada versionada, bytes descargados y originales solo en tmp ignorado.

Negativas HTTP ya ejecutadas: staging con token local válido y archivo `{}` cuyo hash declarado es cero → HASH_OR_FILE_SIZE; staging syncProtocol 2 → INVALID_STAGE. El UUID ficticio figura en NEGATIVAS_HTTP. No se consumó un registro final. No imprimir ni versionar el cuerpo autenticado.

## Comprobaciones locales focalizadas ejecutadas

```powershell
& $adb8b -s emulator-5558 shell am instrument -w -e class 'com.tuempresa.inventariovial.ScapDriveDeviceTest#realAndroidBundleAndAckTransaction,com.tuempresa.inventariovial.ScapAndroidExportTest#exportsAndReopensRealTemplateOnAndroid' com.tuempresa.inventariovial.test/androidx.test.runner.AndroidJUnitRunner
.\gradlew.bat :app:testDebugUnitTest --tests 'com.tuempresa.inventariovial.PavementReportTest.rendersSampleReportAndExcelWithoutCurves' -PgboValidationBuild=true
.\gradlew.bat :app:assembleDebug -PgboValidationBuild=true
```

El nombre `realAndroidBundleAndAckTransaction` no acredita ACK remoto sin su opción de red: aquí se ejecutó únicamente exportación Android local. No activar el caso histórico `scapDriveE2E` como sustituto de comprobar el flujo automático V4 actual. Resultado: SCAP 2/2, pavimentos 1/1 y Debug validación aprobado. Conservar una copia privada del APK conectado antes del build de validación.

## Procedimiento físico pendiente

Una vez disponible instalación compatible en dispositivo secundario limpio o con firma correcta:

1. Abrir un SIC ficticio y guardar incompleto como borrador; volver a Mis registros y al detalle; finalizar con mismo UUID. Observar que solo el ACK de la revisión vigente lo marca sincronizado.
2. Capturar e importar imágenes de prueba desde la UI, clasificar ENTRADA/SALIDA/GENERAL y reabrir galería tras cerrar la app. Anotar UUID/orden/categoría y verificar archivos de la misma inspección en Drive.
3. Desde el detalle, generar y abrir/compartir XLSX con un receptor real. Confirmar lectura, permisos URI y que el Excel local no sustituye el global. Rotar vertical/horizontal en detalle, ficha y galería; volver al inventario ordenado y Mis registros.
4. Crear una inspección SCAP marcada PRUEBA 8B con nombre/código único, ubicación técnica válida de catálogo y fotos asociadas. Completar la ficha original con los valores que exija su validación, revisar advertencias sin desactivar reglas, finalizar y comprobar exportación/ACK/archivos remotos de esa inspección. Volver desde inventario ordenado a sus secciones y detalle; rotar. No usar coordenadas inventadas como GPS observado.
5. Crear muestra de pavimentos con ruta/tramo/calzada de catálogo e inspector de pruebas autenticado. Registrar daños/fotos válidos, cerrar muestra y comprobar PDF/XLSX y entrega automática cuando corresponda. Volver a la misma muestra y Mis registros; rotar. No declarar PCI calculable si faltan curvas.
6. Ensayar edición durante una entrega pendiente y durante POST en vuelo; registrar revisión inicial/nueva, cola y ACK, y asegurar que ACK anterior no confirma nueva revisión. Ante conflicto conservar datos; resolver mediante flujo existente con base remota válida y verificar la fila final. Ensayar un corte temporal durante transmisión en entorno secundario, sin tocar otros dispositivos.
7. Para revocación, configurar el servicio de usuarios con su responsable, autenticar una tablet de prueba y comprobar qué sucede al revocarla mientras mantiene token Drive compartido. Registrar si el backend impide efectivamente nuevos POST; si no, documentar brecha antes de producción. Un token incorrecto rechazado no acredita revocación individual. Comprobar token histórico solo mediante credencial local segura separada, nunca por chat.

Registrar resultado por caso con los estados de la matriz, revisión, UUID, IDs y evidencia saneada. Los pasos anteriores siguen pendientes; no son instrucciones para desplegar producción ni iniciar Prompt 9.
