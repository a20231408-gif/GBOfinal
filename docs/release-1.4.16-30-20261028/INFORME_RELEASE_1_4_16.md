# GBO Ruta Clara — Release candidata 1.4.16 (30)

28/10/2026. **Compilación PASS. 100 pruebas críticas PASS, 0 fallos, 0 errores y 0 omitidas. GO para prueba física de actualización; todavía no es validación física ni aprobación general de campo.**

APK: `D:\Downloads\GBO_PRUEBAS_8B\output\GBO_RUTA_CLARA_1.4.16_30_RELEASE_CANDIDATA\GBO_Ruta_Clara_1.4.16_30_RELEASE_CANDIDATA.apk`

ApplicationId: `com.tuempresa.inventariovial`. Versión 1.4.16, versionCode 30. El propietario confirmó que no existe una 30 o superior instalada/distribuida. Firma Release v2/v3 válida, idéntica a la Release 1.4.15 (29). Certificado SHA-256: `0da8a2cbcf6bce5554526c80de08878d077af1542e488292b3869cdd8ee17315`. APK SHA-256: **`c098bf312a993de51b9dd2177e573287487e2c2fcb9ef7214510920f2ee92929`**. Alineación normal y 16 KiB verificadas; APK completa, no debug, no test, no split.

## Cambios respecto de 1.4.15

Las fuentes Android actuales coincidían exactamente con las compiladas en 1.4.15 (29). Se conservaron todos sus P0 y se cambió exclusivamente versionName/versionCode. No se añadieron optimizaciones de guardado ni sincronización. Los 15 campos efectivos de configuración ajenos a versión coinciden por hash con los anteriores; no se muestran sus valores. Room 20, esquemas y migraciones siguen intactos. Los assets de aplicación y las librerías nativas coinciden con la APK 29; los perfiles DEX generados se excluyen de esa comparación binaria.

Se mantienen SCAP (cierre, navegación a pendientes, borradores y recuperación), SIC-17/17A/17B y consolidados por ruta, exportación local con 54 fotos/3 croquis, panel 20, persistencia y calificaciones válidas, Entregas Drive con ACK actual e historial. Los seis porcentajes G.1 nuevos parten de cero donde es válido; F.1 sigue siendo categórico, no recibe ceros falsos ni se sobrescriben históricos. No cambian escalas ni se presenta una evaluación incompleta como condición contractual válida.

## Verificación

Se ejecutaron las 16 clases críticas, 100 casos, con JVM/Robolectric, Room, Compose y transportes controlados. Detalle en evidencias/PRUEBAS.json. Validación Release normal y assembleRelease aprobados, sin gboValidationBuild. Se comprobó firma sobre la APK anterior real y la nueva; la app actualmente instalada en una tablet no fue inspeccionada.

Primer intento: 94 casos pasaron y 6 fallaron al abrir SQLite en rutas temporales Windows de hasta 292 caracteres. Se cambió únicamente el directorio temporal del arnés a D:/t116_30 y se repitieron esos seis casos: todos pasaron. Los 16 casos adicionales respecto del informe previo de 84 son métodos heredados ya existentes de AutomaticSyncRegressionTest, ejecutados al seleccionar su clase completa. No se modificó código Android para resolver el entorno.

Sin cambios en Apps Script (V21 según la publicación anterior), configuración privada, contratos, usuarios o Drive. No hubo llamadas al backend, instalación, desinstalación, borrado, distribución, commit ni push. Se preservaron APK anteriores y un respaldo reversible del código previo al incremento de versión. Advertencias no bloqueantes ya existentes de JDK/Kotlin/Conscrypt en los registros; no hubo correcciones funcionales ajenas.

Pendiente: actualización física, apertura real de archivos en la tablet y ACK de la prueba de entrega real. Las simulaciones no prueban Google. La lentitud al guardar SCAP y enviar archivos se investigará después, como solicitó el propietario.

## Prueba física breve

1. Conservar las copias locales y abrir este APK sobre la instalación Release compatible. Elegir Actualizar, sin desinstalar ni borrar almacenamiento. Si Android indica incompatibilidad, detenerse y comunicar el mensaje.
2. Primera apertura preferentemente sin conexión: comprobar 1.4.16, sesión/configuración, registros previos, fotos, croquis y pendientes. No recrear fichas.
3. Abrir y reabrir un borrador SCAP; comprobar calificaciones y edición conservadas, SIC-17/17A/17B y los Excel locales. Revisar Entregas Drive y su historial sin perder ACK anteriores.
4. El reintento Drive se mantiene como comprobación separada y coordinada. La cola puede procesar automáticamente otros pendientes cuando se conecta la tablet. No considerar porcentajes de subida o staging como confirmación final.
