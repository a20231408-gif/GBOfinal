# SCAP V2 — fuentes actuales descargadas mediante clasp

Proyecto único: **GBO_Inventario**. Node 24.19.0, npm 11.17.0 y clasp 3.4.1. Sesión Google autorizada, sin leer, copiar ni imprimir el archivo de autenticación. No se creó otro proyecto Google.

Se localizaron cuatro despliegues y veinte versiones antes del cambio. El despliegue configurado en `DRIVE_WEB_APP_URL` corresponde a V20. Sus identificadores y URL se conservan exclusivamente en la carpeta privada de trabajo, fuera de Git. Se descargaron el HEAD y la versión productiva V20 por separado.

## Paridad demostrada

Los ocho módulos y el manifiesto del HEAD coinciden textualmente con V20 desplegada y con las nueve copias proporcionadas. No había correcciones remotas adicionales ni cambios pendientes distintos de los revisados. Los hashes están en evidencias/COMPARACION_HEAD_V20_Y_COPIAS.json. Se comparó sin confundir diferencias CRLF/LF con cambios funcionales.

**El nombre real del módulo remoto es PhotoNamesV2Core (plural).** La copia entregada manualmente se llamaba PhotoNameV2Core (singular), pero su contenido coincide. Se conserva el plural del servidor y no se añade una segunda copia.

## Cambios acotados

- Crear ScapPhotoPanelV2: catálogo único de las veinte categorías oficiales.
- PhotoNamesV2Core: validar/reservar y nombrar esas categorías SCAP/PUENTE.
- SyncV3Core: categoría, carpeta y exigencia de nombres V2; conservar commit/canonical/validate/mergeSheet/compareTechnical/allocateNumber.
- VideoNamesV2Core: categorías y nombres equivalentes, respetando bindings históricos.
- Código: añadir solo una propiedad `scapPhotoPanelVersion` condicionada al catálogo y los tres cores compatibles, Drive, V3 y nombres V2 habilitados.

Sin modificaciones en SyncV3, VideoV1, VideoV1Core, SicObservationsV1Core ni appsscript.json. Se mantienen los 44 globales anteriores y se añade únicamente ScapPhotoPanelV2, sin duplicados. No se alteran ScriptLock, contratos ACK, hashes, UUID, revisiones, numeración, consolidados ni Observaciones. No se integraron optimizaciones generales de velocidad.

Clasp actualiza el conjunto de fuentes mediante updateContent. Se prepara el conjunto COMPLETO de diez archivos (nueve módulos y manifiesto): cuatro módulos existentes cambian, uno se añade y los otros cinco archivos se conservan. Excluir módulos ajenos del envío podría eliminarlos; por eso se verifican inventario, contenido y manifiesto antes y después. No se utiliza --force.

## 19 pruebas locales aprobadas

18 regresiones cargaron los módulos completos recién descargados y luego los integrados: veinte categorías; 54 fotografías JPEG y 3 croquis; SCAP.xlsx y manifiesto; SIC-17/17A/17B; SHA-256 y ACK por archivo; reintento idempotente; BUSY; recuperación de revisión; números estables; datos históricos y dependencia ausente. Una comparación de funciones detectó inicialmente solo saltos CRLF/LF del paquete: se conservaron los LF descargados y las 18 pruebas finales pasaron.

Una prueba adicional recorre los adaptadores completos beginVideoV1/finishVideoV1 para las veinte categorías, simula la transferencia binaria en memoria, rechaza bytes con hash incorrecto y comprueba ACK completo, IDs y ausencia de duplicados; comprueba también UPSTREAM histórico. Es una simulación local, no una subida real de video.

**No se enviaron fixtures a Google ni existe todavía ACK Google de una entrega SCAP comprobada.** El único GET real de capacidades fue de lectura: V3/path4, fotos V2, videos 1/SCAP_ONLY/nombres2; panel V2 ausente en la versión productiva anterior. Los tiempos locales no prueban rendimiento de Google.

La APK 1.4.15 (29) ya entiende el panel 2. No se cambió Android ni se compiló otra APK. F2 de SCAP.xlsx conserva sus 32 espacios contractuales: se incluyen los 54 originales y el manifiesto; el HTML de continuación permanece local. Límites sin cambios: 80 archivos, 24 MiB por staging y 160 MiB por revisión; el fixture de 59 archivos y tres filas SIC está dentro de ellos. Los tamaños de las fichas reales no se comprobaron aquí.
