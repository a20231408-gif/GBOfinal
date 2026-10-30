# Candidata solicitada 1.4.14 (28) — entrega bloqueada

Fecha: 26/10/2026. Proyecto: `D:\Downloads\GBO_PRUEBAS_8B`.

**NO-GO para generar y entregar esta candidata bajo los criterios solicitados.** No se ejecutó `assembleRelease`: se detectó y reprodujo un bloqueo de exportación en la comprobación previa. No es un fallo del compilador. No se produjo una APK 1.4.14 ni un SHA-256 nuevo. No se cambió la versión del proyecto, que continúa en 1.4.13 (27).

## Bloqueo demostrado

Un puente SCAP válido permite crear el consolidado SIC-17. Si en la misma ruta existe además un SIC-17 independiente válido, su definición añade OBSERVACIONES y no coincide con la plantilla de la proyección SCAP. `SicRouteWorkbook.merge()` compara estilos, columnas y encabezado y rechaza la mezcla:

> Plantillas de SIC-17 incompatibles. Se conserva el consolidado anterior; requiere conciliar las plantillas de origen.

La prueba `SicConsolidationDurabilityTest.incompatibleHistoricalSic17TemplatesFailClearlyAndPreservePreviousWorkbook` reproduce este caso con Room y datos ficticios, comprueba el rechazo y verifica que el consolidado anterior permanece byte a byte intacto. No se demuestra pérdida de fichas; sí una exportación que no puede incorporar todos los registros válidos de esa ruta. Por tanto, no se puede declarar satisfecha la validación crítica de exportaciones.

Fuentes involucradas:

- `app/src/main/java/com/tuempresa/inventariovial/export/SicExcelWriter.kt`: incorpora OBSERVACIONES en el SIC independiente.
- `app/src/main/java/com/tuempresa/inventariovial/scap/export/ScapSicExporter.kt`: utiliza las columnas de la proyección SCAP.
- `app/src/main/java/com/tuempresa/inventariovial/export/SicRouteWorkbook.kt`: rechaza firmas de plantilla incompatibles antes de publicar.

No se retiró esta protección ni se eliminó una columna para forzar el resultado. La conciliación debe definir una estructura SIC-17 común, conservar Observaciones y las columnas contractuales y volver a probar la ruta mixta, edición, históricos y recuperación. La equivalencia exacta con el consolidado remoto sigue pendiente de una referencia de procedencia confirmada. No se inició esa corrección funcional en este encargo de compilación.

## Pruebas y alcance

- **Ejecución nueva:** cinco pruebas de `SicConsolidationDurabilityTest`, cero fallos/errores/omisiones. Incluyen persistencia Room, publicación automática local, datos SIC/Pavimentos y rechazo seguro de la mezcla. El PASS de una prueba negativa significa que la protección funciona; no que se haya corregido el bloqueo funcional.
- **Evidencia previa conservada:** 73 casos Android y tres casos Node aprobados en `docs/sic-consolidados-20261026/evidencias/RESULTADOS.json`. Las 14 fuentes y archivos de pruebas enumerados en su manifiesto SHA-256 coinciden con los actuales. No se contabilizan esas pruebas como ejecutadas nuevamente.
- Esa evidencia previa cubre los tres XLSX, tres puentes en una misma ruta, edición sin duplicados, orden PR/metros, reapertura, publicación controlada en EXCEL_SIC mediante proveedores Android y recuperación. No es una prueba física en tablet ni una operación Google.
- Las correcciones anteriores de SCAP, SIC-20, Pavimentos y Entregas Drive se conservaron. No se repitió una auditoría general ni se modificaron sus fuentes.

## Numeración y firma

Se leyeron los manifiestos de las 15 APK de `output`: el mayor código encontrado es **27 / 1.4.13**. No hay un artefacto local 28 o superior en ese inventario; esto no acredita el inventario de dispositivos externos. La numeración prevista 1.4.14 (28) no se utilizó.

Se verificó de nuevo la candidata anterior:

- Archivo: `output/GBO_RUTA_CLARA_1.4.13_27_RELEASE_CANDIDATA/GBO_Ruta_Clara_1.4.13.apk`.
- ApplicationId: `com.tuempresa.inventariovial`.
- Firma válida, no depurable, certificado histórico SHA-256: `0da8a2cbcf6bce5554526c80de08878d077af1542e488292b3869cdd8ee17315`.
- SHA-256 del APK **anterior**, no de una nueva entrega: `e76ce0076af96396d129670a5de118de0b56c603c2dbba73942810952e1dd4bd`.

No se presenta esa APK anterior como si incluyera los últimos cambios de consolidación local. La firma de una nueva APK queda sin verificar porque no se generó.

## Backend y datos

Las 20 categorías SCAP continúan protegidas por capacidad: requieren `scapPhotoPanelVersion >= 2` y las capacidades de nombres correspondientes. En la última consulta documentada, del 23/10/2026, el servidor no anunciaba esa capacidad. No se consultó de nuevo ni se autorizó el envío incompatible. Fotos y videos deben conservarse localmente y la aplicación no debe anunciarlos entregados sin ACK.

Se mantienen pendientes la compatibilidad desplegada de esas categorías y la equivalencia exacta con los XLSX remotos. No se modificaron Apps Script, Google Drive, contratos, credenciales, configuración privada ni datos de usuarios. No se instaló, distribuyó, hizo commit o push.

**Siguiente paso necesario:** resolver y validar la plantilla SIC-17 mixta antes de reanudar la compilación de entrega. No instalar otra candidata como resultado de este informe.
