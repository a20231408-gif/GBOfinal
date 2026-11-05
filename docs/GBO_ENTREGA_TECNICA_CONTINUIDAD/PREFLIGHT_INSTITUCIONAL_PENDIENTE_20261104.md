# Preflight institucional pendiente — 04/11/2026

V21 permanece productivo. Este procedimiento prepara lecturas futuras; no autoriza instalar lectores, ejecutar funciones, cambiar propiedades ni publicar V22.

## A. Respaldo completo de ScriptProperties

El visor que mostró 50 entradas no acredita el conjunto total. Bajo autorización específica y cuenta institucional, una función temporal **solo de lectura** en el proyecto original debe tomar una única instantánea mediante `PropertiesService.getScriptProperties().getProperties()`. De ese mismo objeto calculará el número de claves y SHA-256 de `JSON.stringify(Object.keys(properties).sort().map(k => [k, properties[k]]))` en UTF-8, y formará un sobre privado con `schema:1`, `scriptId`, `capturedAt` UTC, `count`, `sha256` y `properties`. No debe registrar el sobre ni sus valores en `console.log`, Logger, ejecución visible, GitHub o respuestas públicas.

**Canal privado de retorno pendiente de elegir y autorizar:** transporte directo cifrado bajo custodia del propietario, con límite de tamaño medido y confirmación de bytes recibidos. No crear por conveniencia un archivo en Drive, endpoint, deployment ni ScriptProperty auxiliar. Si el canal no devuelve el sobre completo, detenerse. Guardar dos capturas independientes fuera de Git con ACL restringida, SHA de cada archivo y procedencia (scriptId, cuenta, hora, función/versión); ejecutar `node backend/candidato-original-reanudable/properties-backup-verify.cjs A.json B.json`. El verificador rechaza truncamiento declarado, hash alterado y capturas distintas, pero no demuestra por sí solo procedencia, ausencia de claves omitidas antes de calcular el conteo ni pausa de clientes. La revisión del código del lector y del canal es requisito antes de instalarlo o ejecutarlo.

## B. Inventario fresco

Identificar el proyecto, deployment V21, versión del editor, ejecutor, desencadenadores y ejecuciones mediante lectura institucional autorizada. Registrar todos los doPost posteriores al inventario anterior con hora y estado; una llamada completada no demuestra por sí sola commit ni ACK. No iniciar nuevo inventario mientras siga una ejecución activa que pueda escribir.

Ejecutar, solo tras autorización, los lectores ya presentes `gboPilotInventoryReadonly00` a `15` en dos ciclos completos separados. Conservar los mensajes BEGIN/ROW/REFS/END sin truncar en almacenamiento privado. Exigir en cada ciclo los 16 buckets exactos `0–f`, mismo `rootId`, ejecutor, firma de metadatos, `complete=true`, `stableDuringRead=true`, filas completas y cero ilegibles o conflictos no conciliados. Usar `readonly-reconcile.cjs`; cotejar ambos ciclos por ID/nombre, status journal, identidad y revisión record, `contentHash`, ACK completo, binding y folderId. Generar conjuntos de altas, bajas y cambios antes de compararlos con la referencia histórica 207/207/220 y 13 bindings sin record. Esa referencia no sustituye la lectura nueva. Toda diferencia requiere conciliación de sus IDs y efectos; no adoptar ni mover históricos ambiguos.

Para distinguir un doPost de escritura confirmada: correlacionar hora de ejecución y key/UUID/revisión con journal durable, record, binding, IDs de archivos, contenido/hash y ACK de esa misma revisión. `COMPLETE` con ACK coherente y archivos verificados acredita el efecto específico; PENDING, BUSY, error o ausencia de record no lo acredita. Una respuesta perdida puede dejar un efecto durable: consultar estado por lectores, sin reenviar ni ejecutar escritores para diagnosticar. La estabilidad de dos lecturas no prueba que no haya clientes capaces de escribir después.

## C. Registro de pausa de clientes

El responsable conserva una tabla privada con: identificador de cada instalación/usuario que usa la URL original; custodio; última sincronización conocida; mecanismo concreto de pausa; hora UTC de inicio; prueba de estado offline o de cola sin reintento; verificación independiente; hora de fin prevista; firma del responsable. Añadir declaración de cobertura de clientes desconocidos o no inventariados y política para altas durante la ventana. Un cliente sin identificar, un estado no comprobable o nuevos doPost durante la ventana impiden declarar pausa acreditada. OPPO con dos QUEUED y Xiaomi permanecen excluidos y sin alteración.

## D. Decisión

El hito B/DRAIN_ONLY solo puede plantearse cuando el responsable acredite pausa, respaldo completo verificable, inventario fresco estable, ausencia o conciliación de operaciones en curso, ejecutor/permisos y autorización institucional exacta. OPEN requiere otra autorización. No usar cuentas alternativas, crear backend nuevo ni modificar V21/ROOT para sortear discrepancias.
