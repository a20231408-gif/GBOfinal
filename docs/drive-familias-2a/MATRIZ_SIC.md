# Matriz completa del inventario implementado

Fuente: SicExportFormat, SicFieldFormat, SicCatalogRepository, Sic23FormState, SupplementaryFormat, PavementSicFormat, PhotoCategoryPolicy y ScapPhotoCategories. No se clasificaron formatos que GBO no implementa.

Todas las rutas siguientes llevan RUTA al inicio. El sufijo N es asignado atómicamente por el servidor a un UUID estable; el orden técnico nunca lo cambia. Columnas/plantillas contractuales intactas. Excel consolidado bajo RUTA/EXCEL_SIC/. Mapa/KMZ: todos permanecen en el inventario ordenado; se dibuja geometría solamente si existe coordenada real válida. Sin GPS se incluye entrada sin geometría y advertencia. Pavimentos no tienen coordenadas en su modelo: no se convierten X/Y del daño en latitud/longitud.

| SIC | Familia Drive | Clase | Carpeta numerada | Atributos sin carpetas | Categorías reales | Excel consolidado | Mapa/KMZ |
|---|---|---|---|---|---|---|---|
| SIC-08 | FAJAS_CALZADA | Faja de calzada | FAJA N | tipo, sentido, faja | GENERAL | SIC-08 | Sí; con geometría real cuando exista |
| SIC-09 | ANCHOS_FAJA | Medición de ancho independiente | ANCHO FAJA N | ancho útil/total, faja | GENERAL | SIC-09 | Sí; con geometría real cuando exista |
| SIC-13 | PUNTOS_CRITICOS | Punto/sector crítico | PUNTO CRITICO N | clases 15,16,17,17A..17D; lado | GENERAL | SIC-13 | Sí; con geometría real cuando exista |
| SIC-17 | PUENTES | Puente/pontón, clases 01..04 | PUENTE N | definitivo, provisional, artesanal, histórico; tipo; servicio; condición | GENERAL (ficha SIC); categorías SCAP cuando hay inspección | SIC-17 | Sí; con geometría real cuando exista |
| SIC-17A/17B | PUENTES | Complementos del mismo puente UUID | PUENTE N | datos de complemento; no nuevo elemento | Fotos del padre; SCAP conserva sus categorías | SIC-17A y 17B, cuando completos | Sí; con geometría real cuando exista |
| SIC-18 | ALCANTARILLAS | Alcantarilla clases 06/07 | ALCANTARILLA N | definitiva/artesanal, sección, material, condición | CULVERT_INLET→ENTRADA; CULVERT_OUTLET→SALIDA; CULVERT_PANORAMIC→PANORAMICA | SIC-18 | Sí; con geometría real cuando exista |
| SIC-18A | ALCANTARILLAS | Complemento condición mala del UUID SIC-18 | ALCANTARILLA N | tipo de falla, causa, estado | Fotos del padre; mismas categorías | SIC-18A cuando completo | Sí; con geometría real cuando exista |
| SIC-19 | CUNETAS | Clase 08 | CUNETA N | material, sección, condición | GENERAL | SIC-19 | Sí; con geometría real cuando exista |
| SIC-19 | CANALES | Clase 09 | CANAL N | material, sección, condición | GENERAL | SIC-19 | Sí; con geometría real cuando exista |
| SIC-19 | BAJADAS_AGUA | Clase 10 | BAJADA DE AGUA N | material, sección, condición | GENERAL | SIC-19 | Sí; con geometría real cuando exista |
| SIC-19 | ZANJAS_DRENAJE | Clase 11 | ZANJA DE DRENAJE N | material, sección, condición | GENERAL | SIC-19 | Sí; con geometría real cuando exista |
| SIC-19 | ZANJAS_CORONACION | Clase 12 | ZANJA DE CORONACION N | material, sección, condición | GENERAL | SIC-19 | Sí; con geometría real cuando exista |
| SIC-19 | CUNETAS_BANQUETA | Clase 13 | CUNETA DE BANQUETA N | material, sección, condición | GENERAL | SIC-19 | Sí; con geometría real cuando exista |
| SIC-20 | BADENES | Clase 12 | BADEN N | tipo, material, condición | GENERAL | SIC-20 | Sí; con geometría real cuando exista |
| SIC-20 | TUNELES | Clase 13 | TUNEL N | tipo, material, condición | GENERAL | SIC-20 | Sí; con geometría real cuando exista |
| SIC-20 | MUROS | Clase 14 | MURO N | tipo, material, condición | GENERAL | SIC-20 | Sí; con geometría real cuando exista |
| SIC-21 | SENALIZACION_Y_SEGURIDAD_VIAL/MARCAS | Clase 18: marcas | MARCA N | central/lateral, material, condición | GENERAL | SIC-21 | Sí; con geometría real cuando exista |
| SIC-21 | SENALIZACION_Y_SEGURIDAD_VIAL/SEGURIDAD_VIAL | Clase 19: guardavías, delineadores, barreras, resaltos | DISPOSITIVO N | tipos 1..4, material, condición | GENERAL | SIC-21 | Sí; con geometría real cuando exista |
| SIC-21 | SENALIZACION_Y_SEGURIDAD_VIAL/TACHAS | Clase 20: tachas | TACHA N | central/lateral, material, condición | GENERAL | SIC-21 | Sí; con geometría real cuando exista |
| SIC-22 | SENALIZACION_VERTICAL | Señal vertical/poste kilométrico | SENAL N | reglamentaria, preventiva, informativa; señal/material/condición | GENERAL | SIC-22 | Sí; con geometría real cuando exista |
| SIC-23 | DERECHO_DE_VIA | Clase 21 | DERECHO DE VIA N | tipo/ancho/descripción | GENERAL | SIC-23 | Sí; con geometría real cuando exista |
| SIC-23 | ZONAS_URBANAS | Clase 22 | ZONA URBANA N | descripción | GENERAL | SIC-23 | Sí; con geometría real cuando exista |
| SIC-23 | PUNTOS_ESPECIFICOS | Clase 23 | PUNTO ESPECIFICO N | tipo/descripción | GENERAL | SIC-23 | Sí; con geometría real cuando exista |
| SIC-23 | CANTERAS | Clase 24 | CANTERA N | descripción | GENERAL | SIC-23 | Sí; con geometría real cuando exista |
| SIC-34 | ACCIDENTES | Accidente/sector | ACCIDENTE N | tipo y nivel; la equivalencia contractual pendiente sigue bloqueando validación | GENERAL | SIC-34 | Sí; con geometría real cuando exista |
| SIC-14/14A | EVALUACION_SUPERFICIAL/<tramo>/ASFALTICO | Muestra asfáltica; daños como filas UUID | MUESTRA N | tipo de daño, gravedad, densidad; PCI no crea carpeta | PANEL_FOTOGRAFICO existente | SIC-14 y 14A cuando completos | Sí; con geometría real cuando exista |
| SIC-15 | EVALUACION_SUPERFICIAL/<tramo>/RIGIDO | Muestra rígida; daños como filas UUID | MUESTRA N | tipo de daño, gravedad, densidad | PANEL_FOTOGRAFICO existente | SIC-15 cuando completo | Sí; con geometría real cuando exista |
| SIC-16 | EVALUACION_SUPERFICIAL/<tramo>/NO_PAVIMENTADO | Muestra no pavimentada; daños como filas UUID | MUESTRA N | tipo de daño, gravedad, densidad; URCI no crea carpeta | PANEL_FOTOGRAFICO existente | SIC-16 cuando completo | Sí; con geometría real cuando exista |

SCAP individual: RUTA/PUENTES/PUENTE N/SCAP/<UUID inspección>/ conserva el Excel SCAP, manifest y croquis de esa inspección. Nunca se fusiona ese workbook con SIC-17; solamente las filas SIC-17/A/B validadas participan en los consolidados contractuales. Inspecciones distintas del mismo puente comparten el número del puente y mantienen paquetes y fotografías propias. Un mismo UUID de puente aparece una vez en cada consolidado SIC, con la revisión validada más reciente.

Categorías SCAP de su formulario, por identificador estable:

| ID | Carpeta visible normalizada |
|---|---|
| RIGHT_SIDE | VISTA_LADO_DERECHO |
| LEFT_SIDE | VISTA_LADO_IZQUIERDO |
| UPSTREAM | AGUAS_ARRIBA |
| DOWNSTREAM | AGUAS_ABAJO |
| ROAD_DECK | CARRETERA_TABLERO |
| INFORMATIVE_SIGN | SENAL_INFORMATIVA |
| RAILINGS_SIDEWALKS_PIPES | BARANDAS_VEREDAS_TUBERIAS |
| DEFECT | ANOMALIAS_FALLAS |
| BRIDGE_ELEMENT | OTROS_ELEMENTOS_PUENTE |

Categorías históricas SCAP conservadas por código: GENERAL, TRANSVERSE→VISTA_TRANSVERSAL, ELEMENT→ELEMENTO, ACCESS→ACCESO, CHANNEL→CAUCE, OTHER→OTRA. No se crean categorías nuevas en formularios. El backend admite futuras claves estables (por ejemplo INICIO/FIN) cuando el formulario las incorpore; una categoría admite N archivos. Los directorios se materializan al recibir fotografías de su categoría; no se fabrican fotos ni categorías vacías.

SIC-08 y SIC-09: ninguna relación automática; se conserva sourceRecordId histórico como metadata, sin compartir identidad/ruta/carpeta. SIC-18A y SIC-17A/B sí son complementos explícitos del modelo real y conservan UUID del padre.

Casos sin detalle/clase reconocida quedan con advertencia y sin envío, en vez de inventar una familia o introducir SIB por defecto. Se preservan las restricciones de validación contractual preexistentes de SIC-14A y SIC-34.
