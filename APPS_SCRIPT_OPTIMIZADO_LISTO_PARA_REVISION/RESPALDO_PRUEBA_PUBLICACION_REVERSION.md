# Procedimiento futuro - requiere revisión y autorización del propietario

Este documento no ejecuta acciones. Por ahora no se publica, no se cambia URL, ScriptProperties, raíz, identidad de ejecución, scopes, permisos ni archivos de producción. El único proyecto permitido es GBO_Inventario; Servicio de Usuarios queda fuera.

## Puertas de aprobación

| Puerta | Estado actual | Evidencia para avanzar |
|---|---|---|
| Fuentes publicadas exactas | Pendiente | Lista de ARCHIVOS_PUBLICADOS_REQUERIDOS, redactada; versión realmente asignada al endpoint de las tablets. |
| Comparación a tres vías | Solo local/propuesta completada | Revisar cada diferencia publicada; resolver dependencia `plain`/10K y diarios video/10H. |
| Pruebas locales | Resultados en evidencia/RESUMEN_PRUEBAS.json | Todos los casos seleccionados aprobados sobre los archivos concretos que se integrarán. |
| Prueba real aislada / rendimiento | Pendiente | Dos clientes con datos ficticios, misma raíz de prueba, ACK/filas/medios e interrupciones verificadas, tiempos Google medidos. |
| Publicación | No autorizada ni realizada | Propietario aprueba merge final, respaldo consistente y ventana de prueba. |

## 1. Respaldo recuperable

1. Consultar y anotar privadamente la implementación existente y versión que atiende a las tablets, modalidad de ejecución/acceso y servicios. Guardar las fuentes exactas y manifiesto de esa versión, además del borrador actual del editor si es distinto. Identificar el punto de reversión y comprobar que soporta los journals/nombres ya aceptados; no elegir simplemente una versión antigua por fecha.
2. El propietario mantiene sus credenciales en su custodia privada existente; no adjuntar valores de ScriptProperties, tokens, correos ni URLs al paquete/Git. Registrar solo confirmación de que seguirán intactos. No se necesita reinicializarlos.
3. Antes de una futura publicación, coordinar una ventana sin escritores: las tablets conservan sus colas y trabajan offline; esperar que terminen las ejecuciones activas. No desinstalar, borrar Room ni cancelar/destruir las revisiones pendientes. Incluir cualquier otro cliente autorizado que escriba ese mismo proyecto/raíz. Si no se puede detener la escritura, no afirmar que el respaldo de datos es consistente.
4. Con autorización específica, conservar una instantánea recuperable de archivos y metadatos del ámbito y del registro técnico: manifests, bindings V4/históricos, reservas de fotos, staging, journals, revisiones, agregados Excel, video journals y reservas/sesiones. Registrar mapa de IDs originales, relaciones y hashes en custodia privada. Una copia Drive tiene IDs nuevos: no sirve para reemplazar los originales sin un plan de restauración con IDs/estado coherentes.
5. Verificar que se puede leer el respaldo. Si hay un journal PENDING, primero entender su recuperación con el código vigente y dejar constancia del estado; no limpiar ni restaurar manualmente sus archivos como parte de una optimización.

## 2. Prueba controlada fuera de producción

El propietario autoriza un proyecto y raíz aislados, con servicios/permisos equivalentes, sin datos reales. Configurar ese entorno de prueba no modifica los valores productivos. No conectar una segunda copia de Apps Script al mismo ROOT productivo: su ScriptLock no coordinaría con el del proyecto original.

Aplicar el merge revisado sobre la base publicada, no estos archivos completos a ciegas. Mantener una sola definición de cada global/función y los módulos 10H/10K que realmente correspondan. Verificar sintaxis y manifiesto, sin agregar permisos para la telemetría.

Matriz mínima:

- SIC de dos usuarios/tablets con fotos V2; A, B, C, D con D intermedio: Excel A/B/D/C, carpetas sin renumerar, dos UUID distintos no se pisan.
- SCAP con padre confirmado, fotos y video nuevo e histórico. Comparar mediaId/fileId/nombre/categoría antes y después de reintentar; nunca renombrar una reserva antigua. Confirmar checksum/tamaño y ACK final.
- Pavimentos asfálticos, rígidos y no pavimentados con PDF/XLSX/JSON/fotos; revisar aportes SIC disponibles sin exigir inventar campos contractuales incompletos.
- Observaciones de cliente nuevo y anterior: literal `=1+1`, texto vacío, edición y conservación de notas; solo columna final sin estilo/filtro adicional, plantilla intacta, mismo fileId de consolidado.
- BUSY de staging/reserva/commit/video; corte antes de enviar y pérdida de respuesta después de escribir. Reintentar misma identidad/revisión/archivo; ni duplicar ni confirmar incompletos.
- Error a mitad de escritura y recuperación PENDING: archivo y estado coherentes, ACK de base retenido. No borrar los journals para que pase la prueba.
- Dos ejecuciones Google realmente simultáneas y carga escalonada con tamaños representativos; ver MEJORAS_RECONSTRUCCION_EXCEL para mediciones. Las intercalaciones Node no sustituyen esta comprobación.

Conciliar por UUID/revisión y archivo, además de filas y hashes. Un 200 o una capacidad habilitada no acredita una entrega. Registrar defectos sin tocar fichas de ingenieros.

## 3. Publicación de una versión nueva, solo tras aprobación

1. Resolver las puertas anteriores y congelar los archivos integrados con sus hashes. Conservar fuentes de base, diff aprobado y resultados. Si faltan fuentes, detener aquí.
2. Propietario autoriza edición/publicación en GBO_Inventario. Aplicar solo el merge aprobado; **no reemplazar Code.gs completo**. Ningún cambio a token, root, URL, propiedades, permisos o Servicio de Usuarios forma parte de esta optimización.
3. Guardar una versión nueva y actualizar **la implementación existente** para apuntar a ella. Mantener su URL y modalidades actuales. Anotar la versión anterior y la nueva en el registro privado. No dejar una segunda implementación escribiendo simultáneamente la misma raíz.
4. Consultar capacidades mediante GET. Comparar los campos ya aprobados sin exigir banderas nuevas. Observar ejecuciones/telemetría y confirmar que no contienen datos privados.
5. Solo con autorización adicional del ámbito/fichas ficticias, hacer un ensayo mínimo y conciliar ACK + archivos + Excel. No crear pruebas en producción por el hecho de tener permiso para publicar. Si no se autorizan escrituras de prueba, la comprobación productiva permanece solo de lectura.
6. Reanudar sincronización por turnos, vigilando pendientes/ACK y errores de las tablets. No redistribuir APK ni editar usuarios como parte del cambio de servidor.

## 4. Reversión del despliegue

Si aparecen ACK inconsistentes, fallos de plantilla, pérdida de notas, duplicados o regresión de espera medida, suspender nuevas sesiones de sincronización de forma coordinada y conservar todas las colas locales. Esperar/correlacionar ejecuciones en curso; cambiar la versión no interrumpe necesariamente una ya iniciada.

Seleccionar la versión anterior **compatible con el estado ya escrito** en la misma implementación. No alterar su URL/raíz/propiedades/permisos, no borrar `._GBO_SYNC_V3` y no restaurar manifests, bindings o journals antiguos sobre escrituras nuevas. El candidato de instrumentación no introduce un nuevo esquema persistente propio, pero la variante 10K o los diarios Video V2 presentes en la base sí condicionan qué versión antigua puede volver a leerlos.

Para 10H, retirar ciegamente el adaptador que reconoce destinos V2 puede romper recuperación de videos; para 10K, volver a una versión sin conciliación de notas/plantillas puede romper libros mixtos. Si no se ha comprobado esa compatibilidad, usar una corrección hacia adelante revisada o mantener las tablets offline hasta resolverlo, sin improvisar una restauración destructiva.

Después de revertir, consultar capacidades y conciliar las mismas identidades/revisiones y fileId. Reintentar las revisiones conservadas; no reconstruir identidades ni recrear fichas. La restauración de datos desde respaldo sería un procedimiento independiente, expresamente autorizado y basado en el mapa/estado consistente, no parte automática de volver a una versión de código.
