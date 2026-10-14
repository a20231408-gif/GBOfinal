# Parche de Observaciones sin formato adicional

No publicado. Continúa la conciliación de Observaciones preparada en 10I. No requiere cambiar ROOT_FOLDER_ID, tokens, URL /exec, usuarios ni ScriptProperties. No ejecutar borrados ni reconstrucciones masivas de consolidados.

## Archivos completos listos para copiar

En docs/parche-apps-script-10k:

- SicObservationsV1Core.gs: módulo completo; conserva texto y compatibilidad antigua/nueva y normaliza solo la columna final OBSERVACIONES.
- SyncV3.gs: adaptador completo; aplica la normalización después de reunir las filas, de modo que también cubra filas antiguas conservadas.
- SyncV3Core.gs: motor completo de 10I, sin cambios funcionales adicionales en esta etapa; necesario si todavía no se publicó la conciliación de formatos mixtos de 10I.

Los archivos corresponden a google-apps-script/SicObservationsV1Core.js, SyncV3.gs y SyncV3Core.js del mismo commit. Los .test.cjs no se copian al servidor. Mantener un solo módulo con cada nombre/global, sin duplicar archivos antiguos y nuevos.

## Comportamiento

OBSERVACIONES queda al extremo derecho, con texto literal y estilo Normal: sin relleno, bordes, negrita, formato numérico, alineación o ajuste agregados. No define ancho personalizado de columna. Se retira esa columna del rango de autoFilter, conservando los filtros de las columnas contractuales; si existía un filtro únicamente de Observaciones se retira. No se crea un filtro nuevo.

Las celdas de las demás columnas y la tabla de estilos permanecen intactas. Se conservan fórmulas y estructura contractual, UUID, filas y recibos. La dimensión y área de impresión incluyen la columna adicional, sin desplazar el encabezado corporativo, logos ni fusiones contractuales. En Android se escribe inlineStr, incluso para cadenas que comienzan con '='; no se introducen fórmulas para Observaciones.

La normalización final también quita el estilo heredado de las filas provenientes de clientes 10I. Las filas y observaciones conocidas de una actualización hecha por cliente antiguo se conservan conforme al motor 10I. No se cambia el protocolo ACK V3/V4.

## Actualización manual por el propietario

1. Respaldar privadamente fuentes y versión publicadas, configuración y metadatos de Drive; preservar todos sus IDs y permisos. No copiar secretos a Git ni a estos informes.
2. Revisar las diferencias con la implementación realmente publicada. Actualizar los tres módulos completos en el proyecto de inventario, sin cambiar el servicio de Usuarios. El adaptador y el módulo Observations deben actualizarse juntos porque el adaptador llama a plain.
3. Si Code.gs publicado todavía no tiene la capacidad de 10I, incorporar únicamente `sicObservationsVersion: typeof SicObservationsV1!=="undefined" ? 1 : 0,` en su respuesta GET, conservando los valores reales publicados. **No reemplazar Code.gs completo con la referencia del repositorio**, cuya configuración puede diferir. No se añade una bandera.
4. El propietario publica manualmente una versión de la implementación existente cuando lo apruebe. Esta tarea no hace ese paso.
5. Con datos ficticios y un ámbito expresamente autorizado, comprobar archivo local y consolidado: encabezado y texto sin relleno/bordes, filtro termina en la columna contractual anterior, literal '=1+1', vacío, edición de un UUID y actualización con filas de un cliente anterior. Comprobar que no cambian otras columnas, logos, fórmulas ni fileId. Una aprobación Node no acredita una entrega a producción.

Los libros existentes no se reescriben al copiar el código. La normalización se aplicará al siguiente consolidado que el flujo genere; no hay una limpieza o migración automática masiva. Conservar los libros anteriores en el respaldo.

## Recuperación

Si aparece un problema, suspender la distribución y conservar colas locales. Revertir el cambio de estilo únicamente sobre una versión que ya soporte Observaciones y las mismas firmas de plantilla; retirar toda la conciliación 10I después de aceptar formatos mixtos volvería a causar conflictos. No borrar bindings, journals o ._GBO_SYNC_V3 para revertir el aspecto del Excel.
