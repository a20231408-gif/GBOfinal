# Ordenamiento técnico dinámico en Drive - propuesta, sin activar

Estado: solo diseño. No se cambió Apps Script, ROOT_FOLDER_ID, bindings, numeración ni rutas publicadas. Se conserva la entrega automática al finalizar fichas y cerrar muestras. No se requiere esta propuesta para usar las correcciones de croquis.

## Recomendación: A, carpetas estables e índice técnico derivado

Conservar UUID, `folderId`, binding identidad/número, nombres fotográficos V2 y sus reservas. Generar un índice por carretera/formato o familia, con vistas por calzada y sentido. Sus filas contendrían identidad, PR/distancia, nombre y vínculo a la carpeta estable, revisión confirmada y estado de vigencia. El índice es una vista de manifestaciones confirmadas, no una segunda base de identidades ni un requisito previo para guardar offline.

Ejemplo: A=0010+100, B=0010+200, C=0010+400, luego D=0010+300, misma calzada y sentido creciente. Orden de filas/índice: **A, B, D, C**. Si se reservaron en ese orden, carpetas A=1, B=2, C=3, D=4: no renombrarlas. Si varias tablets reservan simultáneamente, los números pueden reflejar el orden de reservas; nunca deben deducirse de la progresiva. El Excel actual ya usa orden contractual; el nuevo índice solo permitiría navegar las carpetas con esa misma secuencia técnica.

### Actualización automática multitablet propuesta

1. El commit actual valida todos los archivos y produce su ACK durable. No incluir fichas en borrador, revisiones rechazadas ni un archivo solamente staged.
2. Bajo la misma autoridad transaccional del servidor, registrar que el ámbito necesita actualizar su índice, con generación monótona. Un reenvío de igual UUID/revisión no crea otro elemento ni otra generación innecesaria.
3. Construir una vista de los manifests confirmados de ese ámbito. Ordenar por la misma función contractual del consolidado: calzada normalizada, sentido normalizado, PR numérico y distancia con signo según sentido, UUID de desempate. La carretera delimita el ámbito; no mezclar PR de carreteras diferentes.
4. Si se prepara el índice fuera del lock global, publicar solo tras comprobar bajo lock que sigue vigente la generación leída; una generación vieja nunca sobrescribe otra más nueva. Reintentar de forma acotada o dejar una actualización pendiente durable. No quitar protecciones de concurrencia.
5. Mantener el `fileId` del índice, su generación y fecha de actualización. Su fallo no invalida un ACK ya confirmado ni provoca reenvío destructivo de fotografías. Mostrar que la vista está pendiente/desactualizada.
6. Una edición autorizada de PR cambia la posición del índice/consolidado tras su nuevo ACK; conserva identidad, carpeta, fotos y recibos históricos. Una futura anulación deberá seguir su propio contrato aprobado; no se implementa aquí.

El propietario debe aprobar formato, ubicación y permisos del índice antes de escribirlo. Para empezar, bastaría un archivo de índice estable; no crear una jerarquía espejo de carpetas. Probar dos usuarios/tablets, llegada tardía D, cambios de PR, empate, sentidos distintos, BUSY, respuesta perdida y fallo de publicación del índice.

## Comparación de alternativas

| Alternativa | Compatibilidad y riesgos | Decisión |
|---|---|---|
| A. Índice derivado, carpetas intactas | Preserva bindings, IDs, nombres, ACK y vínculos. Necesita actualización idempotente y control de generación para no mostrar una vista antigua. | Recomendada. |
| B. Prefijos técnicos en nombres | `folderId` puede conservarse, pero los nombres/rutas participan en validadores de ACK, reservas V2 y referencias humanas. Un prefijo de PR mal formado tampoco se ordena numéricamente; distintas vistas de Drive pueden ordenar por fecha. Requiere contrato de presentación versionado y compatibilidad de clientes históricos. | Solo futura opción de visualización, no cambio de esta candidata. |
| C. Renumeración real | D entre B y C obliga a cambiar al menos C y quizá muchas carpetas; clientes offline conservan recibos/rutas antiguos. Riesgos de colisiones, revisiones simultáneas, reservas activas y nombres de fotos. Requiere migración explícita, journal, recuperación y transición de clientes. El mismo `folderId` no resuelve las dependencias por nombre. | No recomendada; no ejecutar. |

## GNSS, KML/KMZ y progresiva contractual

- **Orden visual local:** `LocalGeometryControls` guarda KML/KMZ en `visual_geometry_9`, con preferencias `visual_order_9`. Suma longitudes de segmentos sin sumar huecos; permite invertir sentido y posición manual local. `AssetOrderScreen` ordena la vista por ese resultado. No modifica Room/revisiones, la referencia contractual ni el backend.
- **Referencia técnica:** `RoadReferenceRepository.loadFromAssets()` carga `road_reference.json`, y puede aplicar una geometría activada por la herramienta separada `CoreFieldTools`/`KmzGeometryStore` (`road_geometry_versions`). Esta activación requiere ruta explícita y rechaza reemplazar sin calibración una ruta con PR oficiales. No confundirla con el botón de orden local.
- **GNSS_MAP_MATCH:** `AutomaticDelivery.technicalOrder()` usa `RoadAxisPositioner` con la ruta y calzada elegidas y los límites de precisión/distancia/ambigüedad. Puede conservar un eje ya persistido como respaldo; la etiqueta no equivale por sí sola a una nueva medición GNSS fiable. El eje es una medida geométrica, no un PR contractual.
- **CONTRACTUAL_PR:** cuando no hay eje utilizable, usa PR/distancia válidos mediante `ChainageCalculator`. No inventa coordenadas; una ficha sin ubicación contractual suficiente queda pendiente de validación/ordenamiento. No se altera PR para forzar una posición visual.
- **Consolidado:** `SyncV3Core.compareTechnical` usa calzada, sentido y PR*1000+distancia, con signo para sentido decreciente. La entrega conserva ambos orígenes, pero V4 no usa el eje geométrico para reemplazar esta ordenación contractual.
- **Sin Internet:** mapas incluidos/importados, guardado y cálculo local siguen disponibles. Cola y snapshots esperan conexión; solo el ACK confirma la entrega. No hace falta red para conocer la progresiva anotada.
- **GNSS impreciso:** el orden visual advierte o deja sin proyección; no puede validar una ubicación solo porque exista una línea KML. Mantener PR/distancia medidos y revisar cualquier eje persistido antes de darle valor contractual.
- **Calzadas y sentidos:** son grupos técnicos distintos. El matcher busca primero la calzada seleccionada, con respaldo EJE donde corresponde; jamás unir PR comparables de sentidos o carreteras diferentes sin agruparlos. El índice debe mostrar explícitamente esos filtros.
- **KMZ de otra carretera:** el parser comprueba estructura, coordenadas y tamaño, pero la ruta se asigna desde la selección del operador: no demuestra correspondencia geográfica real. Puede aceptarse un archivo válido mal etiquetado. En orden local produce una vista incorrecta o no encuentra matching; no reordena el backend. Antes de activar una referencia técnica se debe comprobar extensión geográfica, empalmes, calzada, sentido y anclajes PR con el responsable. No afirmar que la importación valida automáticamente la carretera.

## Ampliación de las seis carreteras

`ProjectRouteCatalog.parse` exige actualmente exactamente seis rutas y correspondencias ítem/tramo únicas. Importar un KMZ no amplía ese catálogo. Proponer una revisión versionada del catálogo aprobado, sin aceptar texto libre como nueva carretera: códigos únicos autorizados, tramos/ítems, ejes y PR calibrados, metadatos de origen/versión y validación de geometría por calzada. Revisar consumidores Android, plantillas, enrutadores y validación de códigos del servidor antes de publicarlo coordinadamente. Mantener lectura de rutas históricas y prohibir reubicar identidades por cambio de catálogo. Esta ampliación no se implementa ahora.

## Backend necesario y activación futura

Para A harían falta generación/registro durable del índice por ámbito, constructor basado en manifests confirmados, actualización idempotente con control de generación y consulta de estado/versión. Revisar primero la versión realmente desplegada, no solo sus capacidades. Comparar con 10H/10K/hotfix pendientes para no sobrescribir arreglos productivos.

La reversión desactiva la actualización/consulta del índice y conserva el último archivo como vista fechada; no toca fichas, fotos, reservas, Excel existentes ni `._GBO_SYNC_V3`. No preparar ni publicar un parche Apps Script en esta etapa: falta aprobación de este contrato y evidencia contra dos clientes aislados.
