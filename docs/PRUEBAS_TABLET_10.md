# Instalación y prueba en tablet — Debug

## Instalar sin perder evidencia

APK: `app/build/outputs/apk/debug/app-debug.apk`. Compilación: `JAVA_HOME=D:\Android Studio\jbr`; `./gradlew.bat :app:assembleDebug -PgboValidationBuild=false --console=plain`. Usa drive.local.properties y server.local.properties privados e ignorados por Git. No enviar esos archivos ni tokens. Nombres V2 y video remoto permanecen false.

Registrar modelo, versión Android, versión instalada y certificado de firma antes de instalar. Mantener respaldos autorizados de originales/exportaciones; no borrar almacenamiento ni usar `pm clear`. APK 16 / 1.4.2 conserva firma Debug habitual. Una tablet con Release de firma distinta no acepta Debug como actualización: **no desinstalar para forzarla**. Usar tablet de pruebas sin inventario o detenerse hasta preparar una actualización compatible autorizada. No se cambia firma Release ni versionCode aquí.

Con depuración USB autorizada, comprobar dispositivo con `adb devices`, y solo si firma compatible o tablet de pruebas instalar `adb install -r app/build/outputs/apk/debug/app-debug.apk`. Alternativa: copiar APK y abrir instalador del dispositivo; detenerse si indica incompatibilidad. No ejecutar la batería instrumentada completa contra una tablet de ingenieros: incluye otros tests anteriores para emuladores descartables.

## Obtener el UUID bloqueado de forma segura

Abrir la ficha afectada, panel **Detalles de entrega Drive / reintentar**, copiar texto seleccionable **Identidad: SIC/UUID** (o SCAP/UUID/PAVEMENT/UUID) y revisión local. Registrar solo UUID/tipo/ruta/PR/revisión/mensaje y formato original; evitar URLs/tokens o datos personales. Buscar revisiones 1791844103713, 1791844288555 y 1791848119143, confirmada 0. PE-3N/ALCANTARILLAS/ALCANTARILLA 1 no es el error.

Si la APK anterior no muestra identidad, ubicar la ficha por revisión/panel y anotar referencia visible; no manipular base Room ni extraer almacenamiento privado sin respaldo y autorización. Instalar actualización Debug solo cuando sea compatible, entonces copiar la identidad nueva. No borrar ficha ni recapturar sus fotos. Hasta autorización específica, las pruebas de escritura deben usar solo una ficha completamente ficticia, no esas tres reales.

## Secuencia de prueba ficticia

1. Crear ficha claramente marcada GBO_PRUEBA_10, una identidad ficticia nueva para prueba tablet. Capturar JPEG con cámara y añadir PNG/WebP/HEIC seleccionables. Conservar sus originales; orientación vertical/horizontal y EXIF. En HEIC confirmar Android 9+ y decodificador disponible; si es ilegible, esperar error local y original conservado.
2. Varias fotos por categoría; revisar previews/orientación, poner offline y volver online. Entrega pendiente hasta ACK; registrar UUID/revisión/baseRevision. Confirmar que errores corruptos no producen estado sincronizado.
3. La recuperación NOT_JPEG ya se reprodujo con ficha ficticia en Drive. Para una ficha real bloqueada, esperar UUID y autorización concreta; luego reintentar una vez y verificar misma identidad/revisión, backups preparados anteriores y ACK, sin recapturar. No borrar la preparación rechazada ni el original.
4. Verificar en Drive foto decodificable, tamaño y SHA-256 de copia enviada/recibida; reintentar entrega y comprobar mismos IDs, sin duplicar. No borrar la ficha de prueba ni otros datos.
5. Pavimentos/SIC: solo fotos. Puentes SCAP: categorías predeterminadas aunque vacías, FOTO/VIDEO juntos, contadores independientes, galería y reproducción, múltiples archivos y retiro/reemplazo con historial. Probar ambos giros de tablet. Videos no se envían al servidor actual y nunca por ruta fotográfica.
6. Nombres legibles y nuevas revisiones técnicas: **no probar como activadas ahora**; necesitan despliegue coordinado del backend y bandera Android. Seguir CAMBIOS_APPS_SCRIPT_10, usando solo ficha ficticia.
7. Anular/restaurar: botón deshabilitado con motivo; no existe aún exclusión global de Excel. No intentar editar una ficha de otro ingeniero para simularla.

Prueba instrumentada de HEIC aislada (solo archivos temporales propios, sin base ni red): compilar `:app:assembleDebugAndroidTest` y ejecutar exclusivamente `com.tuempresa.inventariovial.PhotoFormats10InstrumentedTest` en dispositivo de pruebas compatible; no seleccionar todas las pruebas. Fixture propia 96×64 en assets; compilación no equivale a prueba física aprobada.

Enviar resultados con modelo/Android, UUID, revisión local/ACK, formatos, orientación, mensaje y fileId si corresponde. Ninguna captura debe exponer credenciales.
