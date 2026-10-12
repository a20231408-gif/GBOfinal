# Matriz de validación — Prompt 9 parcial

Fecha: 12/10/2026. **Preparada/compilada no equivale a ejecutada o aprobada.**

## Evidencia previa

| Prueba | Resultado sobre código anterior |
|---|---|
| `OperatorFlow7ScreenTest.detailNavigatesSheetGalleryExcelAndBackUsingSameUuid` | Pasa; no detectaba el dibujo tardío de una imagen reciclada |
| `PhotoDrawing9Test.decodedThumbnailCanBeDrawn` | Falla: Canvas intentando usar un bitmap reciclado |
| `PhotoDrawing9Test.advancedExportsDrawsSelectedPhoto` | Falla: mismo error de bitmap reciclado |
| `PhotoDrawing9Test.detailGalleryDrawsHistoricalPhoto`, tamaño tablet | Falla: mismo error de bitmap reciclado |

Antes de modificar producción se guardaron los XML en `campo-9/evidencia`. Los intentos iniciales de captura con PixelCopy y del fixture de detalle en tamaño teléfono produjeron timeouts del entorno/prueba, corregidos antes de reproducir el defecto; no se contabilizan como crashes de producto.

## Verificación posterior

| Grupo | Resultado ejecutado |
|---|---|
| `PhotoDrawing9Test`: miniatura, detalle con 0/1/3 fotos, histórica nula, faltante, reapertura, ampliación, horizontal y exportación vacía | 7/7 escenarios aprobados en la primera ejecución |
| `PhotoDrawing9Test.advancedExportsDrawsSelectedPhoto` | Timeout inicial esperando una imagen fuera del área visible; se corrigió el desplazamiento del test y la repetición pasó 1/1, incluido dibujo y recomposición |
| `OperatorFlow7ScreenTest` | 4/4: navegación, Excel local, finalización/cola y asociación SCAP |
| `PhotoCategories5Test` | 19/19: categorías, originales, historial y persistencia |
| `PhotoCategories5ScreenTest` | 2/2: controles fotográficos y retiro confirmado |
| `AutomaticSyncRegressionTest` | 21/21: SIC, SCAP, Excel, ACK simulado, revisiones/conflictos y recuperación de cola offline |
| `assembleDebug` y `assembleDebugAndroidTest`, configuración de validación | BUILD SUCCESSFUL |

**54 pruebas distintas con resultado aprobado, distribuido entre dos ejecuciones.** La primera terminó con 53 aprobadas y 1 timeout de visibilidad del test; no se presenta como una suite íntegramente exitosa. La repetición focalizada de esa única prueba pasó. Los XML de ambas ejecuciones se conservan en `campo-9/evidencia/POSTERIOR_PRIMERA_EJECUCION` y `POSTERIOR_EXPORTACIONES.xml`.

Las capturas `campo-9/capturas/` proceden de Canvas nativo en Robolectric, con fotografías sintéticas; **no son evidencia de tablet/emulador Android**. Los archivos que las suites antiguas regeneran en `docs/automatic-sync-v16` y `docs/fotografias-5` se restauraron para no mezclar fixtures cambiantes con el parche.

Comando de regresión focalizada ejecutado:

```powershell
.\gradlew.bat :app:testDebugUnitTest --tests '*PhotoDrawing9Test' --tests '*OperatorFlow7ScreenTest' --tests '*PhotoCategories5Test' --tests '*PhotoCategories5ScreenTest' --tests '*AutomaticSyncRegressionTest' -PgboValidationBuild=true --console=plain
```

Repetición y compilación final ejecutadas:

```powershell
.\gradlew.bat :app:testDebugUnitTest --tests '*PhotoDrawing9Test.advancedExportsDrawsSelectedPhoto' :app:assembleDebug :app:assembleDebugAndroidTest -PgboValidationBuild=true --console=plain
```

SHA-256 APK Debug: `e84f08d6ed5337790f901dc3eeb7c0af082ea5532fbba671a9cb9495fb66c062` (40 507 663 bytes).
SHA-256 APK instrumentada: `e387a530e7be08eb984361d757f8ef6fa98938dffc91bee952abf84f5a657b0a` (1 421 953 bytes).

No se ejecutó la suite completa del repositorio, ni red real, entrega remota, permisos SAF reales, memoria prolongada, reinicio de la APK, ni ninguna prueba física.

## Android instrumentado

`FieldCrashes9DeviceTest`: seis escenarios preparados para galería desde historial (0/1/3 fotos, original faltante) y exportación (vacía/con fotos). Exige una APK sin endpoints. No elimina tablas ni datos; inserta fixtures sintéticos con UUID únicos. El escenario de base vacía se omite si ya existen registros: ejecutarlo primero en un emulador nuevo. **No ejecutado** en esta entrega.

```powershell
$env:JAVA_HOME='D:\Android Studio\jbr'
.\gradlew.bat :app:assembleDebug :app:assembleDebugAndroidTest -PgboValidationBuild=true --console=plain
# Elegir el serial de un emulador NUEVO; no usar la tablet de campo.
$adb9='D:\Android\Sdk\platform-tools\adb.exe'
$serial9='SERIAL_EMULADOR_AISLADO'
& $adb9 -s $serial9 install -r app/build/outputs/apk/debug/app-debug.apk
& $adb9 -s $serial9 install -r app/build/outputs/apk/androidTest/debug/app-debug-androidTest.apk
& $adb9 -s $serial9 shell am instrument -w -e class 'com.tuempresa.inventariovial.FieldCrashes9DeviceTest#advancedExportsWithoutRecords' com.tuempresa.inventariovial.test/androidx.test.runner.AndroidJUnitRunner
& $adb9 -s $serial9 shell am instrument -w -e class 'com.tuempresa.inventariovial.FieldCrashes9DeviceTest' com.tuempresa.inventariovial.test/androidx.test.runner.AndroidJUnitRunner
& $adb9 -s $serial9 logcat -d -b crash
```

No usar desinstalación, `pm clear` ni borrado de Room para resolver incompatibilidades de firma. Las capturas instrumentadas se guardan en el directorio externo de archivos de la aplicación con prefijo `p9-`.

## Lista física — todo pendiente

Repetir en vertical y horizontal; registrar modelo, API, APK/hash, fecha, operador, resultado y Logcat/diagnóstico si falla. Instalar únicamente por un procedimiento aprobado que conserve la firma y los datos de la tablet; esta APK Debug no debe sustituir una APK firmada con otro certificado.

- [ ] Mis registros → detalle por UUID → fotografías: sin fotos, una y varias, categorías históricas nulas.
- [ ] Ampliar, cerrar, volver y reabrir la galería repetidamente; rotar durante la carga.
- [ ] Archivo original o miniatura faltante: mensaje visible y retorno disponible.
- [ ] Repetir con y sin conexión; comprobar que las fotografías siguen disponibles tras reinicio.
- [ ] Herramientas y ajustes → exportaciones avanzadas con base vacía en dispositivo de pruebas.
- [ ] Exportaciones con registros/fotos y copia local anterior; editar encabezado, volver y reabrir.
- [ ] Generar Excel/ZIP, abrir selector de Android, cancelar, volver a guardar y denegar acceso al destino.
- [ ] Comparar UUID, datos, número de fotografías, originales, revisiones y entregas pendientes antes/después.
- [ ] Validar ACK automático SIC/fotos/Excel con la configuración V4 autorizada; no marcar copia local como ACK.
- [ ] Comprobar SCAP, pavimentos, usuarios, sesión y conflictos con datos de prueba.

Condiciones, categorías nuevas, videos, numeración nueva, listados adicionales, GNSS/KMZ y finalización ampliada: **no implementados ni aprobados en Prompt 9**. Las regresiones de funciones anteriores no constituyen aceptación de estas ampliaciones.
