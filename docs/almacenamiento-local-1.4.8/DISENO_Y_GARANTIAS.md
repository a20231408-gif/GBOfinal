# Almacenamiento accesible 1.4.8

## Persistencia y destino

Room conserva su función original. La migración aditiva 19→20 crea tres tablas: elección de destino por ficha, solicitudes de copia local y archivos/URI verificados. No altera ni borra filas de las tablas históricas. La solicitud local se inserta dentro de la transacción de cierre existente, aunque no haya Internet. No se inicializan ni exportan automáticamente registros históricos al actualizar.

La opción automática comienza desactivada. Activarla afecta próximos cierres y advierte sobre espacio y acceso de otras aplicaciones/usuarios. «Solo dispositivo» fuerza la copia local al cerrar, incluso con la opción general desactivada. La elección es **por ficha**, para no aplicar por sorpresa un cambio de sesión a otros ingenieros/registros. Sin elección, se conserva Drive como predeterminado. No se cancela trabajo ni se modifica un ACK existente: el cambio inicial solo se permite en borrador antes de tener outbox Drive. El envío posterior de una ficha «Solo dispositivo» exige confirmación de un usuario ADMIN del login existente; no se agrega otro login ni autorización productiva nueva.

Las barreras de destino se aplican al alta de outbox, sincronización manual/global, envío al servidor independiente si estuviera habilitado y videos SCAP. No cambian HTTP, hashes, reservas, numeración, preparación ni validación de ACK de Drive. Un registro con cola anterior no puede convertirse a «Solo dispositivo» para detenerla.

## Ubicación compartida y atomicidad

Android 10+: `MediaStore.Downloads`, ruta relativa `Download/GBO_Ruta_Clara/<SIC|SCAP|PAVIMENTOS>/<ruta>/<elemento_UUID>/R_<revision>/`. Nombres legibles y componentes acotados; UUID/revisión separan identidades y ediciones. El número local de muestra es etiqueta, no sustituye la numeración contractual del servidor.

Se inserta con IS_PENDING=1, registra el URI real, escribe por streaming y relee tamaño/SHA-256. Se publica con IS_PENDING=0 y comprueba de nuevo. Solo entonces el archivo tiene estado READY. La búsqueda incluye pendientes, evitando que un reinicio entre insert y registro de URI cree otra copia. La recuperación distingue bytes completos aún pendientes de un archivo realmente publicado. Una copia válida anterior con contenido distinto no se sobrescribe.

Android 8/9: Downloads no está disponible. Se requiere elegir explícitamente una carpeta del proveedor de almacenamiento local mediante SAF y conservar su permiso. Se escribe un documento temporal y se renombra al nombre final antes de marcarlo listo. No se finge que esa carpeta está en Download si el usuario eligió otra ubicación. Acceso denegado/permiso revocado deja la ficha y originales intactos y ofrece reintento. No se usa MANAGE_EXTERNAL_STORAGE ni acceso a Android/data de otras apps.

MediaStore puede eliminar pendientes caducados según Android; GBO conserva el origen y el plan privado y puede reservar de nuevo cuando ya no existe ese pendiente. No se eliminan copias anteriores válidas para recuperar espacio automáticamente.

## Qué se genera

- **SIC:** exportadores contractuales existentes por elemento, complementos disponibles, fotografías originales y metadatos. Un error contractual no se convierte en un Excel inventado: se informa y se conserva lo válido.
- **SCAP:** Excel del exportador actual, aportes SIC disponibles, croquis originales, fotografías y videos no retirados. Los videos se copian como archivos sin serializar ni modificar la sesión resumible. Su copia individual también está disponible en el panel de videos.
- **Pavimentos:** PDF actual (incluye croquis 22/26), Excel de datos, JSON nativo y fotos originales. Los aportes SIC se generan solo cuando sus campos contractuales están completos. No se cambian curvas, mediciones, mapeos ni filas por anotaciones visuales.
- **ZIP opcional:** utiliza esos archivos y un manifiesto local con hashes, revisión y faltantes. No es un respaldo restaurable de Room.

Las fotografías se copian al conjunto documental en Descargas, con resolución/calidad/EXIF originales. No se escribe además otra copia en MediaStore.Images/Galería. Se evita así una segunda duplicación automática y se conserva la organización de la entrega; compartir la foto y guardar una copia individual son acciones separadas. Los nombres locales no dependen de una reserva remota V2.

## Coste y recuperación

Cola WorkManager sin requisito de red, procesamiento de tres fichas por tanda, continuación de trabajo y recuperación periódica/al iniciar. Plan de archivos congelado antes de publicar, claves deterministas y SHA por archivo. La generación privada usa AtomicFile; las fotos/videos se transfieren por streaming. Límite de 512 MiB y 1000 archivos por entrega, comprobación orientativa de espacio libre y comprobación de errores reales del proveedor. Una copia no completa nunca se anuncia como completa.

Un cierre puede preceder a la preparación efectiva. Si se vuelve a editar la ficha antes de congelar su plan y la revisión ya cambió, GBO informa el problema y exige exportar la revisión actual; no fabrica la anterior ni sobreescribe una copia válida. Una vez congelado el plan, los documentos privados preparados permiten reintentar la misma revisión. Si falta/cambió un original, se identifica el archivo y se conserva el resto como copia parcial.

Desactivar el ajuste evita nuevas solicitudes automáticas; no borra copias ni cancela solicitudes ya creadas. Los históricos se seleccionan mediante una acción con recuento y confirmación, máximo veinte revisiones por lote. Sus faltantes no cambian el estado de las fichas o de Drive.

## Abrir, compartir y guardar como

Las acciones usan URI reales MediaStore/SAF o FileProvider para compartir un original privado, con permiso temporal de lectura. Antes de abrir/compartir un documento registrado se comprueban acceso y hash. Si el usuario lo eliminó, movió o perdió permiso, se informa y se ofrece recuperarlo desde el original conservado. Guardar como mantiene el selector Android; se registra solo después de verificar el destino y se intenta conservar el permiso cuando el proveedor lo permite.

«Ver ubicación» abre el administrador de Descargas cuando existe; no promete abrir exactamente una subcarpeta. Para destinos SAF/Android 8/9 ofrece indicaciones de la carpeta elegida. PDF/XLSX requieren un lector compatible instalado; una ausencia de lector produce mensaje, no una confirmación falsa.

## Fuentes Android consultadas

- [Almacenamiento compartido y Downloads](https://developer.android.com/training/data-storage/shared/media): Downloads desde Android 10 y acceso a archivos propios sin permiso amplio.
- [Documentos y SAF](https://developer.android.com/training/data-storage/shared/documents-files): selección de documentos/carpetas y permisos persistentes sujetos al proveedor.
- [IS_PENDING](https://developer.android.com/reference/android/provider/MediaStore.MediaColumns#IS_PENDING), [consulta de pendientes](https://developer.android.com/reference/android/provider/MediaStore#QUERY_ARG_MATCH_PENDING): publicación y recuperación de inserciones incompletas.

Las pruebas de proveedor simulado verifican esas llamadas, pero no certifican la visibilidad real en los administradores de archivos de cada tablet.
