# Actualización manual del consolidado SIC

No se ha desplegado Apps Script ni modificado propiedades, usuarios, roles o datos de producción durante 10I.

## Cambio aprobado de formato

El propietario confirmó que Observaciones debe aparecer como última columna, con encabezado OBSERVACIONES, en los SIC cuyo formulario tiene ese campo. Los exportadores locales actuales no incluían esa columna. Se agrega al extremo derecho, manteniendo el orden y los valores de las columnas anteriores. El texto de cada fila se obtiene literalmente del registro, sin prefijos ni inferencia desde el informe HTML. Se escribe como inlineStr y no como fórmula de Excel.

Se aplica a SIC-08, 09, 13, 17, 18, 19, 20, 21, 22, 23 y 34. SIC-18A es un diagnóstico complementario sin campo independiente de Observaciones y no recibe una columna nueva. Las proyecciones SCAP/SIC-17A/SIC-17B y los SIC de Pavimentos no cambian sus columnas por esta mejora. Un consolidado SIC-17 puede contener filas SCAP sin observación en esa columna y filas del SIC principal con su texto.

La estructura se amplía en una columna a petición del propietario. Antes de distribuir, este debe validar visualmente el resultado contra la plantilla aprobada; no se declara una equivalencia contractual externa nueva por la sola prueba de software.

## Archivos completos

| Archivo del repositorio | Actualización en GBO_Inventario |
| --- | --- |
| google-apps-script/SicObservationsV1Core.js | Agregar como SicObservationsV1Core.gs |
| google-apps-script/SyncV3Core.js | Sustituir el motor existente conservando su nombre de archivo .gs |
| google-apps-script/SyncV3.gs | Actualizar el adaptador completo existente |
| google-apps-script/Code.gs | Incorporar únicamente la capacidad GET indicada abajo, no sustituir configuración publicada |

Los .test.cjs no se instalan en Apps Script. No duplicar variables globales copiando el mismo motor con otro nombre de archivo.

## Procedimiento

1. Guardar una copia privada del código y versión publicados. Preservar el ROOT_FOLDER_ID real, token, URL /exec, propiedades y servicio avanzado Drive. En 10H se comprobó que la raíz publicada difiere de la referencia local de Code.gs; no reemplazarla con esa referencia.
2. Actualizar los tres módulos completos indicados en el mismo proyecto GBO_Inventario. No cambiar el servicio independiente GBO - Servicio de Usuarios.
3. En el objeto doGet de Code.gs, agregar `sicObservationsVersion: typeof SicObservationsV1!=="undefined" ? 1 : 0,` conservando el resto de capacidades. No se agregan ScriptProperties.
4. El propietario publica manualmente una nueva versión de la implementación existente. Esta ejecución no realiza ese paso. Confirmar GET sicObservationsVersion=1, V3/V4 y las capacidades de fotos/videos ya aprobadas.
5. Antes de distribuir la candidata, entregar únicamente registros ficticios autorizados: dos elementos con textos diferentes, editar uno sin cambiar UUID, verificar la misma fila consolidada y reintentar una revisión sin duplicarla. Comprobar también una observación vacía y un texto que comienza con '=' como texto, no fórmula. Abrir el Excel final para validar encabezado, ancho y área de impresión.

## Compatibilidad y conservación

El adaptador normaliza la firma de plantilla solo respecto de una última columna OBSERVACIONES. Las restantes columnas, estilos y encabezados siguen protegidos por la comparación de firmas. Un desplazamiento, duplicado o conflicto ajeno a esta columna se rechaza, sin adoptar otra plantilla silenciosamente.

El consolidado elige la plantilla con Observaciones cuando mezcla filas antiguas y nuevas. Las filas históricas sin esa celda quedan en blanco, conservando sus datos anteriores; sus observaciones originales siguen en sus metadatos y aparecen al exportar o revisar desde el nuevo cliente. No se reescriben masivamente fichas antiguas para poblar la nueva columna.

Si una fila ya tenía una observación confirmada y luego la actualiza un cliente antiguo cuyo XLSX carece de la columna, el servidor conserva su celda confirmada y actualiza las demás celdas del UUID. Guarda la plantilla anterior como metadato técnico de esa fila para conservarla también en reintentos y ediciones antiguas sucesivas. Un cliente nuevo que envía explícitamente la celda vacía sí puede vaciarla. El contador de revisiones, hash del contenido enviado y fileId del consolidado mantienen su protocolo existente.

Si ya existía el último encabezado fuera del rango formateado, se reutiliza: se integra el ancho y se amplían dimensión/área de impresión sin crear otro encabezado. Los registros, archivos, fórmulas y posiciones previas no se desplazan. No se agrega una fórmula a las observaciones.

## Reversión

Conservar copia privada de la implementación anterior y los archivos entregados. Para revertir la candidata Android, usar únicamente una versión compatible autorizada, sin borrar datos ni forzar un descenso de versionCode. Los clientes anteriores pueden seguir enviando su formato al backend actualizado.

No retirar el módulo de conciliación después de recibir formatos mixtos: una implementación anterior volvería a rechazar sus firmas de plantilla. Ante un problema, suspender la distribución y mantener la cola local; cualquier reversión de servidor debe evaluarse sobre copias de prueba, conservando la última versión y los metadatos. No borrar JSON técnicos, consolidaciones o evidencias como procedimiento de reversión.

Los nombres legibles de videos 10H son una actualización independiente, documentada en CAMBIOS_APPS_SCRIPT_10H.md. Su capacidad real se consultará y registrará para el cierre 10I; no se presupone que ya fue publicada.
