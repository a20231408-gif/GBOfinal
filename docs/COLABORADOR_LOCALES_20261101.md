# Copia válida accesible tras una regeneración fallida

Responsable: Colaborador A, rama `codex/colaborador-locales`.
Base comprobada: `dda0e2398933d8853e963fa95361a81aaddd351d`, rama de integración
`codex/observaciones-campo-asus-20261030`. GBO 1.4.22 (36).

## Problema y reproducción

En `LocalFilesList`, una regeneración más reciente sin archivos READY ocultaba
la última copia válida del listado principal. El error y el botón de reintento
seguían visibles, pero había que abrir el historial para encontrar el documento.

Fixture sintético reproducible en `LocalFiles148UiTest`:

1. Una copia FULL está READY y contiene `conservado.pdf`.
2. La regeneración FULL_LAYOUT_V4 de esa misma revisión está FAILED sin archivos.
3. Abrir `LocalFilesList`: debe aparecer el error, el reintento y el PDF conservado.
4. Mientras el reemplazo está WRITING, debe seguir disponible el PDF anterior.
5. Al publicar el reemplazo READY, este ocupa el listado principal; ambos documentos
   siguen accesibles en el historial. El trabajo anterior y su fila se conservan,
   y la consulta no genera una intención de Drive.

La nueva prueba falló en el paso 3 contra el código original: `assertExists`
no encontró `ARCHIVO GUARDADO · conservado.pdf`.

## Corrección

`LocalFilesList` reutiliza `LocalCopyPresentation.currentFiles`, ya usado en
`UnifiedRecordsScreen`: muestra la generación más reciente que tenga archivos
READY de cada categoría. El estado del intento más reciente sigue visible.
Se mantienen los filtros por trabajos/familia, la deduplicación por URI y el
historial explícito. No se modifica `LocalCopies.kt` ni la persistencia.

## Validación ejecutada el 01/11/2026

Windows 11, Android Studio AI-261.26222.65.2614.16379836, JBR 25.0.3,
wrapper Gradle 9.6.0 y SDK local (plataforma android-37.0, Build Tools 36.0.0).
`local.properties` y la configuración del IDE son locales e ignorados por Git.

| Ejecución | Resultado |
|---|---|
| Compilación inicial `:app:assembleDebug -PgboValidationBuild=true` | PASS |
| Primer intento de las dos clases iniciales, temporal largo | FAIL: 4 PASS / 20 FAIL por SQLiteCantOpenDatabaseException; rutas de 271–283 caracteres |
| Mismas clases y código original, con temporal corto | PASS: LocalCopyPresentationTest 4/4 y LocalFiles148Test 20/20 |
| Nueva regresión de UI contra código original | FAIL esperado: 1/1, documento conservado ausente |
| Compilación Debug con la corrección | PASS |
| Selección final con la corrección | PASS: LocalCopyPresentationTest 4/4, LocalFiles148Test 20/20, LocalFiles148UiTest 5/5 |
| `git diff --check` | PASS |

Selección final: **29/29**, cero fallos, errores u omitidos; Gradle 30 s.
No sumar ejecuciones repetidas. Las dos clases iniciales no se modificaron.
El temporal corto utiliza el parámetro existente del init-script, sin cambiar
pruebas, aserciones ni dependencias:

```powershell
$env:JAVA_HOME='C:\Program Files\Android\Android Studio\jbr'
$tempPruebas=Join-Path $env:TEMP 'gbo-locales-tests'
New-Item -ItemType Directory -Force $tempPruebas | Out-Null
.\gradlew.bat "-Dgbo.migration.temp=$tempPruebas" '-PgboValidationBuild=true' :app:assembleDebug :app:testDebugUnitTest --init-script tools/migracion/pruebas.init.gradle --tests com.tuempresa.inventariovial.LocalCopyPresentationTest --tests com.tuempresa.inventariovial.LocalFiles148Test --tests com.tuempresa.inventariovial.LocalFiles148UiTest
```

Los XML y logs de cada ejecución se conservaron por separado en la entrega local.
La UI se probó con Compose/Robolectric SDK 34 y gráficos nativos; no se ejecutó un
emulador ni se generaron capturas de dispositivo. La APK de validación no se instaló
ni se publica. No acredita apertura con visores reales ni validación física.

## Pendientes de aceptación física

P04/P15/P17: comprobar en el dispositivo autorizado el acceso al archivo conservado
tras un fallo, el reemplazo al completarse la copia y el historial, además de abrir,
compartir y localizar con aplicaciones reales. P01–P17 siguen sin ejecución física.
Backend, protocolo, identidades, hashes, ACK, configuración productiva, Room,
migraciones, firma, versión y dependencias Gradle permanecen sin cambios.
