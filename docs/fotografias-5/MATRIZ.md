# Categorías fotográficas — Prompt 5

Base: Prompt 4 `fec797c73d6eca00a009a3e53752b40276ecad35`, que incluye los checkpoints 2A y 2B. La política utiliza el modelo y los códigos, nunca nombres de archivos, materiales, condiciones o subtipos de señal.

| SIC | Elemento / modelo | Anteriores | Definitivas para captura | Motivo |
|---|---|---|---|---|
| 08 | Fajas / calzada | GENERAL | GENERAL | Formato independiente, sin inventar categorías por sus PR |
| 09 | Anchos de faja | GENERAL | GENERAL | Registro independiente de ancho |
| 13 | Punto crítico | GENERAL | GENERAL | Evidencia del punto |
| 14/14A | Pavimento asfáltico | Panel propio de daños | Sin cambios | Conserva muestra, daño y panel de pavimentos |
| 15 | Pavimento rígido | Panel propio de daños | Sin cambios | Conserva PCI y registros de losas |
| 16 | No pavimentado | Panel propio de daños | Sin cambios | Conserva URCI y muestra |
| 17/17A/17B | Puente / SCAP | Categorías SCAP existentes | Las mismas claves SCAP | No añadir ni sustituir vistas; UUID de inspección separa paquetes e imágenes |
| 18/18A | Alcantarilla | CULVERT_PANORAMIC, CULVERT_INLET, CULVERT_OUTLET | Las mismas claves → PANORAMICA, ENTRADA, SALIDA | Categorías ya correctas; conserva UUID compartido |
| 19 clase 08 | Cuneta | GENERAL | INICIO, FIN, OBSERVACION | Modelo longitudinal con dos extremos |
| 19 clase 09 | Canal | GENERAL | INICIO, FIN, OBSERVACION | Modelo longitudinal |
| 19 clase 10 | Bajada de agua | GENERAL | INICIO, FIN, OBSERVACION | Modelo con inicio y descarga final |
| 19 clase 11 | Zanja de drenaje | GENERAL | INICIO, FIN, OBSERVACION | Modelo longitudinal |
| 19 clase 12 | Zanja de coronación | GENERAL | INICIO, FIN, OBSERVACION | Modelo longitudinal |
| 19 clase 13 | Cuneta de banqueta | GENERAL | INICIO, FIN, OBSERVACION | Modelo longitudinal |
| 20 clase 12 | Badén | GENERAL | INICIO, FIN, OBSERVACION | Extremos físicos de entrada/salida solicitados; su modelo contractual puede conservar un único PR. No se inventa un PR final ni GNSS |
| 20 clase 13 | Túnel | GENERAL | INICIO, FIN, OBSERVACION | El modelo contempla progresivas inicial y final |
| 20 clase 14 | Muro | GENERAL | INICIO, FIN, OBSERVACION | Modelo longitudinal |
| 21 clase 18 | Marcas horizontales | GENERAL | INICIO, FIN, OBSERVACION | El formulario actual exige tramo con extremos |
| 21 clase 20 | Tachas por tramo | GENERAL | INICIO, FIN, OBSERVACION | Solo para tramo; punto histórico sin extremo final conserva GENERAL |
| 21 clase 19/tipo 1 | Guardavía | GENERAL | INICIO, FIN, OBSERVACION | Dispositivo longitudinal |
| 21 clase 19/tipos 2–4 | Poste, barrera, resalto | GENERAL | GENERAL | No aplicar la regla longitudinal solo por pertenecer a Seguridad vial |
| 22 | Señal vertical / hitos | GENERAL | PANORAMICA, ELEMENTO | Contexto y señal; sin carpetas por tipo, material o condición |
| 23 clases 21/22 | Derecho de vía / zona urbana | GENERAL | INICIO, FIN, OBSERVACION | El modelo de captura contempla tramo inicial/final |
| 23 clases 23/24 | Punto específico / cantera | GENERAL | GENERAL | Modelos puntuales |
| 34 | Accidente | GENERAL | GENERAL | Evidencia del evento |

En registros anteriores sin los extremos necesarios, la galería conserva GENERAL y las categorías anteriores. Un badén puede utilizar referencias fotográficas físicas de entrada/salida aunque su ficha contractual tenga un solo PR. No se cambian validaciones de ubicación ni columnas SIC.

Todas las categorías admiten varias imágenes. El límite técnico por revisión V3 (80 archivos y 160 MiB, incluyendo complementos) permanece; no existe un límite de una foto por categoría. No se generan carpetas vacías.

GENERAL histórico aparece como «Históricas · GENERAL» cuando no es una categoría de captura nueva. Ninguna apertura o guardado lo reasigna. El selector permite reclasificación explícita; esa acción registra historial y nueva revisión.

SCAP conserva: RIGHT_SIDE, LEFT_SIDE, UPSTREAM, DOWNSTREAM, ROAD_DECK, INFORMATIVE_SIGN, RAILINGS_SIDEWALKS_PIPES, DEFECT, BRIDGE_ELEMENT, GENERAL, TRANSVERSE, ELEMENT, ACCESS, CHANNEL, OTHER. Las etiquetas y su traducción Drive son las preexistentes. Paquete e imágenes de inspección quedan bajo `PUENTE N/SCAP/<inspectionUUID>/`; cada imagen utiliza su categoría existente y UUID propio. El consolidado SIC-17/17A/17B y la identidad del puente permanecen comunes.
