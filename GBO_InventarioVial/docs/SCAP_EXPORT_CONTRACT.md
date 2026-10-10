# Exportación SCAP por tipo

Las nuevas fichas seleccionan `ScapBridgeType.SLAB` o `MODULAR`, persistido como `scap_values.ownerId=inspection,key=bridgeType`. Cada tipo usa su archivo original copiado byte a byte y su estrategia `ScapCanonicalLayout`. Las fichas históricas sin tipo conservan su exportador anterior; no se interpreta automáticamente su tipo.

Fuentes:

- LOSA: `FICHA SCAP - GBO modelo - LOSA.xlsx`, SHA-256 `2519524d02791a0e7c49b69a8ecfd28f051d7e2a7e35de6fa7f0944b8a653e57`.
- MODULAR: `FICHA SCAP - GBO modelo - MODULAR.xlsx`, SHA-256 `ac02633660b214639d8caff8895a5708e8e0891dbcb3d0d84767e960e49bdb89`.

Los inventarios completos de celdas, secciones, encabezados, validaciones, fórmulas, imágenes, merges y geometría están en `SCAP_LOSA_FIELD_MAP.json` / `SCAP_MODULAR_FIELD_MAP.json`. Los mapas operativos de 140 campos están en `tools/scap_slab_field_map.json` / `tools/scap_modular_field_map.json`; `tools/generate_scap_export_maps.py` los genera desde los catálogos por tipo sin guardar las fuentes XLSX.

## Hojas y coordenadas

Ambos formatos tienen nueve hojas originales, preservadas en su orden y nombre:

| Índice | Hoja | Escritura |
| --- | --- | --- |
| 1 | A-D.- FICHA | Captura principal, J10 nombre, J12 código, E16 latitud, E18 longitud, P16 ruta, P18 progresiva; F1 encabezado general fijo |
| 2 | E.- CROQUIS | Tres anchors originales: elevación, planta, sección transversal |
| 3 | F.1- CONDICION GLOBAL | Código B, descripción C, observación F en filas contractuales por grupo |
| 4 | F.2.-PANEL FOTOGRAFICO | Lista B/C/D filas 11–42 y 32 anchors de foto originales |
| 5 | G.- CONDICION ESTADISTICA | G.1 filas 14–44: A código, B descripción, D metrado manual, E unidad, F factor real, G:L calificaciones 0–5 |
| 6 | G.2-CONCLUSIONES | Bloques distintos por tipo, fórmulas derivadas intactas |
| 7 | AUXILIAR | Catálogo contractual original intacto |
| 8 | Hoja1 | Auxiliar original intacto |
| 9 | Hoja3 | Auxiliar original intacto |

G.2 LOSA utiliza B19/E19, B29/E29 y B43/E43. MODULAR utiliza B28/E28, B49/E49 y B60/E60. Las conclusiones generadas mediante fórmulas de la fuente no se sustituyen por texto; las observaciones específicas del ejemplo fuente se limpian en los espacios de captura.

D.4 de nuevas fichas conserva el bloque visible y escribe hasta quince puntos C/G/J/N462:476 y contador E448. Se limpian las mediciones auxiliares de ejemplo sin alterar sus fórmulas. No se crea infraestructura CORE.

## Límites y revisión

La revisión bloquea exportaciones que omitirían información. El borrador permanece disponible y todos sus valores se conservan.

| Repetible | LOSA | MODULAR |
| --- | --- | --- |
| F.1 superestructura | 3, filas 22–24 | 1, fila 22 |
| F.1 subestructura | 3, filas 26–28 | 2, filas 24–25 |
| F.1 detalles | 4, filas 31–34 | 2, filas 27–28 |
| F.1 cauce | 3, filas 36–38 | 3, filas 30–32 |
| F.1 accesos | 4, filas 41–44 | 4, filas 34–37 |
| G.1 | 31 elementos | 31 elementos |
| Tramos con caracterización | 2 | 2 |
| Pilares independientes | 3 | 3 |
| Apoyos | 2 | 2 |
| Junta | 1 | 1 |
| Fotos | 32 | 32 |
| Croquis | uno por tipo | uno por tipo |
| D.4 perfil | 15 puntos | 15 puntos |

C.1 contiene tres espacios de longitud, pero C.1/C.2 solamente dos juegos de caracterización. C.3 contiene un juego de tablero. Un segundo tablero con datos distintos bloquea exportación y necesita una continuación aprobada del formato. No se expande arbitrariamente el libro original ni se descartan repetibles silenciosamente.

## Conservación de plantilla y fotografías

Se modifican exclusivamente valores de captura y anchors fotográficos/croquis. Permanecen hojas, estilos, nombres definidos, merges, anchos, alturas, encabezados, logos, impresión y fórmulas derivadas. Las fórmulas de ejemplo del **metrado de entrada G.1** se sustituyen por el metrado manual, y las fórmulas de búsqueda de unidad en espacios de entrada se sustituyen por la unidad contractual real: no son reglas de metrado adoptadas por la aplicación. El libro fuerza recálculo al abrir.

Las fuentes contienen nombres definidos y fórmulas `#REF!` preexistentes. Se conservan y se muestran como advertencia en revisión; esta implementación no inventa sus vínculos. También se conserva P14 vinculado al distrito y J63 vinculado a la longitud total según la fuente; requieren revisión técnica del responsable del formato si se pretende utilizar otra regla.

Las imágenes JPEG/PNG conservan proporción mediante FIT_CENTER dentro de su anchor original. Se elimina crop de la imagen de ejemplo; se calculan el tamaño y los desplazamientos centrados en EMU. Se consideran las ocho orientaciones EXIF, incluidas rotaciones y reflejos. Exportación convierte las fotos a JPEG limitado y los croquis a PNG sin modificar los archivos de captura. Los logos originales permanecen referenciados.

Los campos numéricos con un literal efectivamente observado en la fuente usan `NUMERIC_OR_SOURCE_STATUS`: exportan los números como valores numéricos y los literales exactos como texto. MODULAR contiene `Sin Informacion` en N30, `SIN DATOS` en N38/N42, `N/A` en E40/G275 y `5 pulg` en E101. Es evidencia del valor de la fuente, no una lista contractual inventada. La captura valida únicamente el tipo numérico original o las opciones efectivamente observadas; el exportador no convierte esas expresiones en cero ni las elimina.

La versión local del paquete SCAP pasa de 4 a 5 para que una exportación por tipo no reutilice un ZIP previo con la plantilla histórica. No cambia el router Drive, protocolo, workers ni rutas de CORE.

## Verificación

`ScapCanonicalExportTest` comprueba los hashes de ambas fuentes, selección de plantilla, nueve hojas originales, estilos, merges, anchos/alturas, fórmulas derivadas, logos, coordenadas manuales, código manual alfanumérico, metrado, unidad, factor real, límites y rechazo de tipo incompatible. Comprueba fotos portrait/landscape centradas y orientación EXIF. Las exportaciones sintéticas se guardan en `tmp/parallel-b/SCAP_LOSA.xlsx` y `SCAP_MODULAR.xlsx` para inspección.

Una quinta prueba verifica literales numéricos de la fuente y números tipados. `tmp/parallel-b/SCAP_PHOTOS_FIT.xlsx` conserva dos imágenes sintéticas, landscape y portrait con EXIF, para inspección visual del FIT_CENTER.
