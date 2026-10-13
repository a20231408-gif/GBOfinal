# Entrega Prompt 10 — 12/10/2026

Rama: `codex/correcciones-campo-10`. Base: `44453aecb2ac03e39124ab39ad7b9724e50e9d80`, último publicado del Prompt 9. Bloque A publicado: `cd877b2`; Bloque B: `2ef12a5`; protección C: `3a2f8f4`. El commit final de documentación es HEAD de esta rama; verificar con `git rev-parse HEAD` y `git ls-remote origin refs/heads/codex/correcciones-campo-10`.

## Disponible para probar ahora

La APK Debug convierte la copia de transporte a JPEG verificable, conserva originales y EXIF y calcula SHA-256 sobre los bytes enviados. Recupera la preparación de una revisión rechazada explícitamente con NOT_JPEG, conservando UUID/revisión/baseRevision y respaldando la preparación rechazada. Una respuesta sin ACK válido nunca confirma la ficha. No desactiva ninguna validación del servidor.

Pavimentos y SIC muestran fotografías, sin videos. Puentes SCAP tiene una tarjeta por categoría real, FOTO/VIDEO en la misma fila, contadores independientes, contenidos locales, vista previa e historial. Video remoto sigue desactivado. Controles comprobados en Compose vertical/horizontal; aún falta aceptación en la tablet física.

NOT_JPEG nace en `SyncV3Core.validate` → `v3Store.isJpeg`: staging recuperado y hash comprobado, antes de escritura definitiva. Se reprodujeron PNG etiquetado `.jpg` y JPEG decodificable con trailer tras EOI. El transporte anterior podía reutilizar preparaciones anteriores y confiar en una copia cuyo contenedor no cumplía SOI/EOI. Ahora se comprueban MIME por decodificación, marcadores, archivo completo y copia final; el nombre no determina el formato. **La causa exacta de las tres fichas de campo no se conoce sin su UUID y bytes.** JPEG/PNG/WebP, EXIF y recuperación fueron probados localmente en Robolectric Android 34; HEIC depende del decodificador del dispositivo, con prueba instrumentada preparada y no ejecutada. No se ha confirmado el modelo/Android de la tablet.

Prueba real autorizada: ficha ficticia bajo `GBO_PRUEBA_10`; PNG rechazado, JPEG de la misma identidad/revisión aceptado, reintento con los mismos IDs. Descarga independiente de Drive: JPEG de 726 bytes, SHA-256 igual al enviado. No se escribió ni se borró inventario real; no se borró tampoco la ficha ficticia. Evidencia en `campo-10/evidencia/prueba-drive-real-ficticia.json`.

Las revisiones 1791844103713, 1791844288555 y 1791848119143 siguen sin prueba física. PE-3N/ALCANTARILLAS/ALCANTARILLA 1 es una entrega exitosa y solo se inspeccionó su estructura.

## Preparado, requiere backend nuevo

Bloque B incluye el parche de reservas centrales por UUID de foto y nombres `NNN_FAMelemento_CAT.jpg` de máximo 20 caracteres. Mantiene nombres históricos y preparaciones ya existentes. Nuevos respaldos se ubicarán bajo `._GBO_SYNC_V3`; consolidados permanecen en `RUTA/EXCEL_SIC`. No se movió ningún archivo remoto. La activación es coordinada y opt-in, actualmente deshabilitada en Android. La capacidad V2 se anuncia separadamente de V1 para conservar clientes anteriores. Véanse CAMBIOS_APPS_SCRIPT_10 y ESTRUCTURA_DRIVE_10.

## Bloque C pendiente y protegido

No se implementó la anulación global/Excel/restauración/renumeración tras anular: falta una autorización individual de inventario verificable. Las credenciales compartidas Drive y los permisos TABLET_ADMIN_ONLY no acreditan esa facultad. No se modificó Servicio de Usuarios. Se deshabilita la transición local anterior que podía aparentar una anulación remota y se protege también la llamada directa al ViewModel. El parche del servidor rechaza esos comandos antes de escribir. No existe una cola de anulación funcional ni se afirma que el Excel se excluya hoy. Los registros históricos ya anulados no se cambian. Diseño y condiciones de integración en ANULACION_CONTROLADA_10.

UUID, números históricos, bindings y ordenamiento KMZ permanecen separados de nombres visibles. No se modifica el algoritmo de ordenamiento aprobado en Prompt 9. Familias y ediciones sincronizadas mantienen sus regresiones aprobadas; no se repite toda la batería previa.

## Validación y compilación

Resultados detallados y distinción local/Drive real/pendiente en MATRIZ_VALIDACION_10. Compilación Debug y AndroidTest **BUILD SUCCESSFUL** (1 min 36 s), sin ejecutar pruebas físicas. Huella y configuración verificada en `campo-10/evidencia/apk-debug-10.json`. SHA-256 APK: `51407d5166f98b0effcb5b950bc2f6daeec0d4c5f0edb463a8e14c0a835e8a36`. APK local: `app/build/outputs/apk/debug/app-debug.apk`; servicios reales incluidos desde archivos locales ignorados. No se publican APK ni credenciales en Git. Versión conserva 16 / 1.4.2 y firma Debug habitual; no se generó Release ni se cambió firma histórica.

Instalación sin borrar datos y pruebas físicas en PRUEBAS_TABLET_10. Un Debug con firma distinta no puede reemplazar la APK Release de campo: detenerse ante incompatibilidad y usar una tablet de pruebas. No desinstalar una APK que contiene evidencia local para forzar la instalación.

## Trazado del transporte auditado

| Etapa | Hallazgo/control |
|---|---|
| Cámara | FieldTakePicture usa proveedor Android o Camera2 seleccionado; escritura URI privada, sin evidencia de fallo específico de la tablet |
| Galería/URI | PhotoFilePicker copia bytes con ContentResolver; puede dar extensión .jpg a PNG/HEIC sin convertir, por eso no se confía en la extensión |
| Original privado | Conservado; conversión se hace en copia separada de DeliveryJpeg |
| Compresión/EXIF | Decodificación real, JPEG 95 para formatos no JPEG, conserva orientación/GPS/fechas; JPEG válido no se recomprime innecesariamente |
| Preparación/hash | Comprueba contenedor final tras EXIF, SHA-256 de los bytes preparados; original intacto y preparación rechazada respaldada |
| Base64/payload | Base64 NO_WRAP de los mismos bytes; se verifica hash antes de staging y MIME JPEG exige contenedor íntegro |
| Backend staging | Decodifica Base64, comprueba SHA-256/tamaño y propiedad/ubicación del staging |
| Commit | NOT_JPEG valida SOI/EOI antes de escritura definitiva; ACK verificado antes de syncedRevision |
| WorkManager | Recupera solo rechazo explícito actual, misma identidad/revisión, reintento idempotente; no recompone snapshot con ACK perdido |
| Videos | Almacenamiento/sincronización separados, limitado a SCAP; nunca se convierte video en foto |

La huella no contiene credenciales. Auditoría de documentos/fuentes agregados no detectó valores privados y confirmó configuraciones/APK fuera de Git. ACCESS_CONTROL_ENABLED conserva false del archivo local: la APK no acredita una autorización administrativa individual; por ello C continúa bloqueado.
