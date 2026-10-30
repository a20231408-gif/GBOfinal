# Límites V21 y evolución necesaria — propuesta, no desplegada

Se revisó la copia privada verificada de la versión inmutable V21. No hubo conexiones ni escrituras a Google en este encargo.

## Lo compatible hoy

- El ZIP local no es el objeto enviado a Drive. Su tamaño no determina por sí solo la admisibilidad de la entrega.
- SyncV3 acepta hasta 80 archivos del padre, 24 MiB de bytes decodificados por archivo de staging y 160 MiB por revisión (archivos del padre más XLSX de sicRows). Conserva manifiesto completo, identidad, revisión, hashes, reservas y ACK.
- Los videos SCAP siguen su protocolo V1 independiente y reanudable: hasta 32 MiB por video, bloques de 512 KiB, ACK de video separado y padre confirmado primero. Una copia ZIP de más de 512 MiB debida a muchos videos individuales compatibles puede ser compatible con V21: no requiere enviar ese ZIP al servidor.
- Un conjunto de fotos/croquis/documentos que exceda 160 MiB, un archivo de staging mayor de 24 MiB o más de 80 archivos del padre requiere evolución del protocolo. Android detiene ese envío con una causa explícita; conserva íntegros originales, revisión y copia local. No comprime destructivamente ni confirma una parte.
- SIC incompleto: copia local provisional con vacíos; el escritor remoto estricto sigue excluyendo aportes SIC incompletos de sicRows y conserva los anteriores mediante el contrato existente. El ACK del padre no acredita que esos SIC pendientes estén completos. La UI conserva la advertencia. No se envía el consolidado local como sustituto del global.

## Por qué no fraccionar en revisiones ordinarias

La revisión actual representa un manifiesto completo y removeMissingOwnedFiles=true. Enviar sucesivas revisiones parciales podría retirar archivos anteriores del manifiesto. Tampoco se puede fabricar un ACK conjunto o aumentar un límite sin revisar cuotas y recuperación.

## Propuesta mínima versionada, pendiente de autorización y desarrollo

1. Añadir una capacidad explícita de adjuntos grandes; mantener V21 sin cambios para APK antiguas. Negociación obligatoria; fallback actual sin pérdida.
2. Sesión transaccional por key/revision/contentHash/baseRevision. Manifiesto esperado íntegro, tamaño/hash por archivo y fragmentos acotados; reservas estables por UUID. Transporte reanudable del binario directamente a Drive mediante un flujo equivalente al de videos, sin POST gigante ni base64 de cientos de MiB.
3. Staging aislado de la revisión publicada. Repetir cada bloque/archivo o perder respuesta debe devolver la misma sesión/offset/archivo, sin crear otro. Límites explícitos de cuota y duración; journal persistente.
4. Finalización con ScriptLock: comprobar todos los archivos y hashes esperados, baseRevision, reservas y consolidación; publicar el manifiesto completo de forma recuperable. ACK final único del padre únicamente después de completar el journal. Antes de él no retirar archivos vigentes. Los archivos de una transacción incompleta se conservan para recuperación, no se borran automáticamente.
5. Los SIC provisionales deben ser adjuntos documentales separados y declarados PROVISIONAL, sin introducir filas incompletas en consolidados contractuales ni borrar celdas históricas. Una edición completa posterior actualiza la misma identidad.
6. Pruebas necesarias antes de publicar: límites exactos y superiores; dos tablets; BUSY en cada fase; pérdida de respuesta en reserva, bloque y commit; reinicio entre journal y publicación; revisión competidora; disco/cuota agotada; hash incorrecto; recuperación de sesión y ausencia de duplicados; paridad contractual de APK antiguas. Comparar Google real en raíz aislada autorizada.

Esta propuesta no cambia Apps Script, no habilita capacidades ficticias y no constituye un parche publicable. Si la ficha real supera los límites del padre, sigue pendiente ese trabajo Android + backend y el ensayo Google autorizado. Si el tamaño proviene de videos válidos separados, corresponde verificar su flujo existente y sus ACK, no modificar límites del padre.
