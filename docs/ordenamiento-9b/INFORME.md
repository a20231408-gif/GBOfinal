# Corrección focalizada de AssetOrderingTest — 12/10/2026

Se resolvieron exclusivamente los seis fallos pendientes de la matriz. La causa comprobada es la incompatibilidad de sus expectativas con el contrato introducido en `99b15aae88b2e69cb28a4715c87acb854fe6e12b` (`feat(drive): familias sin SIB, identidad estable y orden tecnico (2A)`). Ese cambio sustituyó deliberadamente las carpetas SIB y los identificadores derivados del contador por familias V4 e identidad estable. No se modificó código Android de producción ni Apps Script para hacer pasar estas pruebas.

| Prueba histórica fallida | Causa y comprobación corregida |
|---|---|
| `retroactiveCulvertRenumbersOnlyItsFamilyAndKeepsIdentityAndPhotos` | Esperaba orden geométrico y renumeración remota. Ahora comprueba desempate técnico por UUID, geometría independiente, igualdad de fichas/fotos y conservación del número confirmado aunque cambie `sequence`. |
| `mixedFamiliesUseSeparateCountersAndIgnoreCaptureTimeAndManualPr` | Ignoraba PR contractual numérico. Ahora comprueba PR, contadores por familia e identidad independiente del contador, fecha y geometría. |
| `fallbackIsExplicitAndDoesNotMixContractualAndGeometricOrigins` | Esperaba desempate por fecha. Ahora comprueba UUID determinista, saneamiento NaN y conservación de metadatos GNSS/manuales y originales. |
| `tiesAreDeterministicAndRouteSibFamilyDefineCountersNotRoadbed` | Esperaba contadores separados por SIB y códigos derivados de ellos. Ahora comprueba ruta/familia, ausencia de SIB, identidad estable, determinismo y exclusión de anulados. |
| `folderPlanUsesCurrentOrderAndRealPhotoIndexWithoutMutatingAnything` | Esperaba `SIB-02/ALCANTARILLA N` y renombrado al insertar. Ahora comprueba familia/UUID/categoría V4, índices fotográficos reales, conservación de fotos y ruta al insertar. Este plan es una previsualización local; no asigna números remotos. |
| `catalogUsesDetailClassesAndReportsUnknownsWithoutInventingSib` | Esperaba familias antiguas para SIC-19 clases 12/13 y bloqueo por SIB desconocido. Ahora comprueba ZC/CB y fallback OTROS/UUID con advertencia ELM. |

Se conservaron las dos pruebas restantes de AssetOrderingTest. Los bindings persistidos y las rutas reales de entrega también están cubiertos por las regresiones existentes; no se restauraron reglas antiguas que renombrarían identidades Drive V4.

## Validación focalizada

Una ejecución: **40 pruebas, 40 aprobadas, 0 fallos, 0 errores, 0 omitidas**; Gradle `BUILD SUCCESSFUL`, 2 min 52 s.

| Clase | Pruebas aprobadas |
|---|---:|
| AssetOrderingTest | 8 |
| DriveFamilies2ATest | 8 |
| SyncedRecordEdits2BTest | 10 |
| RoadPositionTest | 6 |
| DriveAssetPathRouterTest | 4 |
| FieldContinuation9Test (numeración, segmentos/inversión y ambigüedad) | 3 |
| DeliveryMetadataTest (fotos y metadatos auditados) | 1 |

Comando reproducible, con servicios simulados y sin credenciales locales:

```powershell
$env:JAVA_HOME='D:\Android Studio\jbr'
.\gradlew.bat :app:testDebugUnitTest --tests '*AssetOrderingTest' --tests '*DriveFamilies2ATest' --tests '*SyncedRecordEdits2BTest' --tests '*RoadPositionTest' --tests '*DriveAssetPathRouterTest' --tests '*FieldContinuation9Test.displayNumbersSurviveReorderingAndNewElements' --tests '*FieldContinuation9Test.localGeometryAccumulatesSegmentsWithoutStraightLineGapsAndReverses' --tests '*FieldContinuation9Test.selfCrossingAndParallelGeometryRequireManualReview' --tests '*DeliveryMetadataTest.separatePhotoFixesAndAuditedMetadataNeverCopyRecordLocationOrPrivatePaths' -PgboValidationBuild=true --console=plain
```

Evidencia: `evidencia/pruebas-focalizadas.log` y siete XML JUnit. Los fixtures históricos regenerados por las pruebas fueron restaurados; sus cambios automáticos no forman parte de esta entrega. No se repitió la suite completa ni las instrumentadas. Los resultados históricos fallidos siguen en `../campo-9b/evidencia/cierre`.

## Debug con servicios locales

`drive.local.properties`, `server.local.properties` y `local.properties` están ignorados por Git. Se comprobó presencia de endpoints HTTPS y token sin imprimir sus valores. La comprobación sin secretos queda en `evidencia/configuracion-local-sin-secretos.json`. Esta comprobación no acredita acceso ni aceptación del servidor real.

```powershell
$env:JAVA_HOME='D:\Android Studio\jbr'
.\gradlew.bat :app:assembleDebug -PgboValidationBuild=false --console=plain
```

Este modo carga los servicios de esta máquina. `-PgboValidationBuild=true` omite esa configuración y se utiliza para las pruebas. El APK conectado permanece local e ignorado; no se sube a GitHub. El log bruto de su compilación permanece en `app/build/compilacion-debug-servicios-local.log`; solo se publica un resumen sin configuración.

Resultado: **BUILD SUCCESSFUL en 1 min 13 s**. Se comprobó en BuildConfig la presencia de endpoint Drive, token y endpoint de usuarios sin mostrar sus valores. APK: `app/build/outputs/apk/debug/app-debug.apk`, **40,190,144 bytes**. SHA-256: `cf43cf4f58544b3cb1ca2c2d5720945d8fd10fa77e516633fb9d79dfe67c3c1e`. Resumen: `evidencia/compilacion-debug-resumen.json`. Este APK reemplaza al artefacto histórico sin servicios del cierre 9B.

La configuración actual mantiene `ACCESS_CONTROL_ENABLED=false`: el endpoint de usuarios existe, pero el control de acceso no está activado. También conserva desactivados los prefijos nuevos de fotografías y videos remotos, pendientes del backend. No se editaron archivos de configuración local, firma, `applicationId`, `versionCode=16` ni `versionName=1.4.2`. No se generó Release ni se desplegó Apps Script.

## Prueba en tablet pendiente

1. Copiar `app/build/outputs/apk/debug/app-debug.apk` a la tablet y abrirlo. Instalar como actualización si el certificado es compatible; ante firma incompatible, conservar los datos de campo y usar una tablet/perfil de validación compatible.
2. Conservar una copia de los datos de campo antes de la validación. Revisar históricos, UUID, números locales y fotografías; insertar un elemento anterior y comprobar que las identidades existentes no cambian.
3. Importar el KMZ, revisar segmentos/sentido y contrastar orden visual con PR contractual, sin confundir número local con número Drive confirmado.
4. Con una ficha de prueba identificada, verificar entrega y ACK de Drive V4 con el servicio configurado; no aceptar como sincronizada antes del ACK. La conectividad real y la aceptación en tablet quedan pendientes del propietario.

Esta corrección no requiere actualización de backend. Los parches ya preparados del Prompt 9B siguen pendientes por separado: `google-apps-script/Code.gs`, `SyncV3Core.js`, `VideoV1Core.js` y `VideoV1.gs`, según `../PENDIENTES_SERVIDOR_9.md`. No se modificaron ni desplegaron en esta ejecución.
