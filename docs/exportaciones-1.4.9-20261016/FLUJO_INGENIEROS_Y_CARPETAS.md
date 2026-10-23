# Uso de los archivos locales — 1.4.9 (23)

## Mientras registra

GBO conserva primero los datos originales dentro de la aplicación. Después de un guardado confirmado agrupa cambios próximos antes de preparar archivos en segundo plano. No hace falta entrar a Revisión ni pulsar Exportar. Android puede demorar la ejecución por batería o falta de almacenamiento: compruebe el estado mostrado en Mis registros, no solo el mensaje de guardado de la ficha.

La opción está activada por defecto donde no se eligió antes una preferencia. Una desactivación explícita anterior se conserva. Herramientas y ajustes → Copias locales permite cambiarla y explica que Descargas ocupa espacio y puede ser visible para otras aplicaciones o usuarios de la tablet. Los registros históricos no se exportan en bloque al actualizar; se incorporan de forma explícita o al abrirlos/modificarlos según la opción vigente.

Mis registros reúne SIC, puentes SCAP y las superficies de Pavimentos. Filtre por familia, carretera, progresiva o estado. Cada identidad presenta por separado captura, Drive y copia local. Abra los documentos con Abrir archivo; si hay varios, el selector identifica el archivo y su revisión. Compartir utiliza permisos de lectura temporales. Ver ubicación muestra la ruta y abre Descargas cuando Android lo permite; no promete seleccionar una carpeta exacta.

## Borrador y documento oficial

- SIC incompleto: XLSX PROVISIONAL con lo conocido y hoja PENDIENTES. Un texto que empieza por = permanece como texto. Los campos desconocidos quedan vacíos.
- SCAP incompleto: XLSX SCAP provisional y los tres SIC provisionales aplicables; JSON, originales, croquis de todos los tipos y videos disponibles se conservan. Si un error estructural impide la plantilla SCAP canónica, un XLSX provisional de captura conserva las secciones y explica la limitación.
- Pavimentos: PDF identificado como Borrador mientras no se cierre, Excel de datos y JSON; las geometrías y referencias 22/26 siguen en sus exportadores existentes. Los resultados no calculables conservan sus motivos pendientes.
- Al completar una revisión se genera el documento contractual que corresponda por separado. Un provisional local nunca se agrega como fila SIC válida al consolidado Drive.

La copia es documental. No es un mecanismo automático de restauración de la base Room.

## Carpetas nuevas

Sin ACK, se usa una identidad provisional legible con UUID, nunca un número de carpeta supuesto:

```text
Download/GBO_Ruta_Clara/
  PE-3N/
    ALCANTARILLAS/ALCANTARILLA_UUID_<uuid>/
      REVISIONES/R_<revision>/SIC-18[_PROVISIONAL].xlsx
      REVISIONES/R_<revision>/FICHA.json
      REVISIONES/R_<revision>/MANIFIESTO_LOCAL.json
      FOTOGRAFIAS/<categoria>/<id>_<hash>.jpg
    PUENTES/PUENTE_UUID_<uuid-inspeccion>/
      REVISIONES/R_<revision>/SCAP[_PROVISIONAL].xlsx
      REVISIONES/R_<revision>/SIC-17[_PROVISIONAL].xlsx
      REVISIONES/R_<revision>/SIC-17A[_PROVISIONAL].xlsx
      REVISIONES/R_<revision>/SIC-17B[_PROVISIONAL].xlsx
      REVISIONES/R_<revision>/INSPECCION.json
      FOTOGRAFIAS/...  CROQUIS/...  VIDEOS/...
    EVALUACION_SUPERFICIAL/<tramo>/RIGIDO/MUESTRA_UUID_<uuid>/
      REVISIONES/R_<revision>/Inspeccion[_BORRADOR].pdf
      REVISIONES/R_<revision>/Datos.xlsx
      REVISIONES/R_<revision>/Muestra.json
      FOTOGRAFIAS/...
```

Los componentes locales se sanean para proteger nombres de archivo; por ejemplo `MUESTRA 42` del ACK se representa como `MUESTRA_42`. El manifiesto conserva la ruta remota lógica exacta y la identidad de carpeta recibidas. Las familias especiales SIC usan el enrutador V4 (por ejemplo SEÑALIZACION_Y_SEGURIDAD_VIAL/MARCAS), no una familia genérica inventada.

Con ACK guardado se utiliza esa carpeta lógica para las copias nuevas. Una reserva V2 previa al ACK no basta. Las copias antiguas permanecen donde estaban; no hay renombrado ni traslado automático. Drive conserva sus propios archivos, consolidados EXCEL_SIC, números estables y la organización SCAP existente. Las revisiones y medios compartidos locales son una adaptación documental, no una réplica byte a byte de las carpetas del servidor.

Los medios originales se direccionan por UUID/hash fuera del directorio de documentos de cada revisión y se verifican por SHA-256. Si solo cambia texto, no se recodifican ni se publican otra vez los mismos medios. Las modificaciones exclusivamente de videos conservan una subrevisión documental local separada. El ACK de la ficha padre no confirma por sí solo los videos; estos mantienen sus confirmaciones independientes. Los originales privados no se eliminan.

## PR y Drive

Para preparar la entrega SCAP se mantienen la ruta del proyecto y el código PR con distancia compatibles con el ordenamiento técnico. Un texto libre de progresiva no sustituye esa ubicación SIC del puente. Si falta o es inválida, aparece la explicación, Ir a ubicación y acceso a la copia local. Los pendientes de gabinete aceptados pueden permanecer; no se inventa un PR ni una posición para evitarlos.

Sin Internet, con BUSY o sin PR, la copia local continúa por una cola independiente. Abrir un archivo ya verificado no espera a Drive. Drive se considera confirmado únicamente por su ACK; la cantidad de archivos preparados no confirma una entrega.

## Límites y fallos

Cada copia admite hasta 512 MiB y 1000 archivos. Las revisiones publicadas no se limpian automáticamente: controle el espacio disponible. En Android 8/9 se requiere elegir una carpeta local con el selector de Android una vez. En Android 10 o posterior se publica por MediaStore en Descargas.

«Actualizando archivos locales…» aún no significa lista. «Archivos locales actualizados» exige verificar los archivos previstos. Una exportación parcial o fallida conserva Room y los originales, y ofrece reintento. Si se movió o retiró una copia pública, GBO comprueba el acceso antes de abrirla y permite recuperar una copia conservada; no debe confundirse con pérdida de la ficha privada.

Para recuperar un archivo público movido o sin acceso, use las acciones del selector de documentos o Herramientas y ajustes → Copias locales → Abrir archivo → Recuperar copia en Descargas. Esto usa el original conservado y no exige entrar al formulario. El estado de copia corresponde a la última verificación; GBO vuelve a comprobar acceso e integridad al abrir.
