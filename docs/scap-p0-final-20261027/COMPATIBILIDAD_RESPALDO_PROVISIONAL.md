# Respaldo SCAP: límites del contrato, sin despliegue

## Evidencia disponible

Se revisaron las ocho fuentes V20 copiadas del editor y conservadas en `docs/backend-google-forced-busy-20261017/paquete/FUENTES_V20_REFERENCIA_NO_IMPORTAR`. No se consultó ni modificó Google en esta tarea. La última consulta de capacidades archivada es del 23/10; no acredita el estado del despliegue el 27/10.

`SyncV3Core.gs.txt`, función `validate`, comprueba identidad, revisión/base, ruta/familia, ubicación técnica, cantidad de archivos, hashes, JPEG, categorías y filas SIC. Trata el manifiesto SCAP como un archivo protegido por hash; no certifica que una inspección esté contractualmente completa. Las filas SIC que recibe sí pasan las validaciones del protocolo y del workbook.

El cliente actual ya permite cierre **con advertencias aceptadas**, si no hay valores inválidos ni problemas de integridad. El manifiesto conserva `completionStatus=INCOMPLETE`, `uploadedWithWarnings` y la captura completa; omite las filas SIC que no cumplen su validación individual. Un ACK válido acredita esa entrega de archivos y revisión, **no la finalización contractual**. La candidata identifica expresamente este caso como respaldo con datos pendientes.

Esto NO demuestra que el servidor publicado admita un nuevo flujo de borradores arbitrarios, valores inválidos o un repositorio provisional independiente del consolidado. No se habilitó ese flujo. Tampoco se inventó una capacidad que V20 no anuncia.

Las 20 categorías nuevas siguen requiriendo `scapPhotoPanelVersion>=2` y nombres V2, además de las capacidades existentes. Si falta compatibilidad, el transporte rechaza el envío antes del POST incompatible. Evidencias y cola permanecen en tablet. Los videos conservan su cola, categoría y ACK independientes.

## Propuesta mínima para un respaldo provisional general (NO implementada)

1. Comparar primero las fuentes del despliegue vigente con V20. Añadir una capacidad explícita, por ejemplo `scapProvisionalBackupVersion=1`, solamente después de probarla.
2. Definir un tipo de respaldo identificable y separado de los entregables contractuales. Conservar captura cruda, advertencias y medios; identificar visiblemente los documentos provisionales.
3. Validar siempre pertenencia de UUID, revisión/base, rutas autorizadas, categorías, hashes, formatos de medios y límites. Aceptar valores incompletos como captura cruda, sin convertirlos en ceros ni en datos contractuales.
4. No incorporar filas inválidas al consolidado. Una actualización provisional no debe borrar ni sustituir una fila contractual anteriormente válida. Documentar la relación entre respaldo provisional y futura entrega contractual.
5. Mantener ScriptLock transaccional, journal y publicación recuperable. El ACK debe distinguir el respaldo provisional, acreditar todos sus archivos y ser idempotente para el mismo UUID/revisión/hash. No reutilizar un ACK contractual para aparentar otra operación.
6. Probar dos clientes, BUSY, pérdida de respuesta, edición, reanudación y recuperación en raíz de pruebas antes de anunciar la capacidad. Publicación y reversión requieren autorización separada.

Hasta disponer de esa evidencia: guardar borrador, exportar ZIP local verificable y corregir/confirmar la ficha conforme al contrato existente. No se promete respaldo Drive provisional general ni GO de campo por ese flujo.
