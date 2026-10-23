# Compatibilidad y alcance — candidata documental 1.4.9 (23)

## Identidad anterior comprobada

`D:\Downloads\GBO Inventario 1.4.9.apk` y `app/release/app-release.apk` contienen los mismos bytes: SHA-256 `2ddd96bb774eab66feea2210a78d08b36e525dc7f145263b781b1a1639f4a816`. Su identidad interna es **1.4.8 (22), Release, com.tuempresa.inventariovial**. Cambiar el nombre externo no cambia Android. El certificado es el histórico `0da8a2cbcf6bce5554526c80de08878d077af1542e488292b3869cdd8ee17315`.

No se encontró code 23 en las seis APK de Descargas ni en los directorios conocidos de salida del proyecto. Esto no prueba qué pudo distribuirse fuera de esta computadora. No se consultaron ni modificaron tablets.

Se conservó un respaldo de las fuentes/recursos iniciales, Gradle, esquemas y APK en `tmp/exportaciones-1.4.9-20261016/respaldo`. También se mantienen las candidatas anteriores de output, incluida otra 1.4.8 (22) con SHA-256 `21c1893d2a511ee04636b5fe2a9d3ffccd2c466a34a8d9e4b4b80042ab795514`.

## Decisión de compatibilidad

- Room permanece en 20, sin migración destructiva; se reutilizan las tablas de solicitudes y archivos de 1.4.8. Los disparadores de la outbox local registran únicamente nuevas inserciones/modificaciones; no recorren históricos al abrir.
- Se conservan los exportadores definitivos y la entrada contractual sicRows. Los XLSX provisionales son derivados locales separados con PENDIENTES, sin introducir filas incompletas en Drive.
- Las rutas nuevas se derivan de la familia V4 y, cuando existe, de un ACK confirmado guardado. Una reserva de nombre sin ACK no es confirmación.
- Sin ACK: identidad provisional UUID. Pavimentos usa el contrato V20 `RUTA/EVALUACION_SUPERFICIAL/TRAMO/SUPERFICIE`, y SCAP se vincula al puente padre de `RUTA/PUENTES`.
- No se mueven archivos públicos anteriores. Las fotografías, croquis y videos se direccionan por identidad y hash; los documentos van por revisión.
- Se conserva la preferencia explícita de desactivación existente; donde no existe preferencia, el nuevo valor predeterminado es activado. Se informa privacidad y almacenamiento en la interfaz.
- Trabajo local sin condición de red; Drive conserva sus validaciones y su cola independiente.

## Comprobación en curso

La primera compilación detectó un tipo de flujo incorrecto en la nueva vista unificada, corregido utilizando observeHistory (incluye fotos y detalles). Las verificaciones de contenido, recuperación y regresión se registrarán en RESULTADOS.md. La visibilidad en Descargas y la apertura en aplicaciones Android requieren prueba física; no se considerarán demostradas por Robolectric.
