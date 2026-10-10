# Auditoría directa F.1 / F.2 / G.1

Se leyeron directamente los XLSX indicados por el usuario, sin reconstruirlos ni guardar con openpyxl. Fuente de listas: filas reales F.1; sourceSha256 verificado contra assets. Proyecto semántico F.1 Condición global, F.2 Panel fotográfico, G.1 Condición estadística. Nombres físicos intactos.

## LOSA

Fuente SHA-256 `2519524d02791a0e7c49b69a8ecfd28f051d7e2a7e35de6fa7f0944b8a653e57`. 17 elementos reales F.1.

| ID plantilla | Código F.1 | Código G.1 | Nombre | F.1 fila | G.1 fila |
| --- | --- | --- | --- | --- | --- |
| LOSA:F1:22 | 104 | 104 | Losa de concreto armado (Refuerzo Transversal) | 22 | 14 |
| LOSA:F1:23 | 110 | 110 | Viga Principales concreto armado | 23 | 15 |
| LOSA:F1:24 | 111 | 111 | Vigas Secundarias de concreto armado | 24 | 16 |
| LOSA:F1:26 | 202 | 202 | Elevación Cuerpo del Estribo de Concreto Armado | 26 | 17 |
| LOSA:F1:27 | 205 | 205 | Elevación Alas del Estribo de Concreto Armado | 27 | 18 |
| LOSA:F1:28 | 216 | 216 | Zapata de Concreto armado para Estribos | 28 | sin fila de ejemplo |
| LOSA:F1:31 | 301 | 301 | Capa Asfalto | 31 | 19 |
| LOSA:F1:32 | 311 | 311 | Vereda Concreto | 32 | 20 |
| LOSA:F1:33 | 353 | 353 | Barandas de Acero | 33 | 21 |
| LOSA:F1:34 | 372 | 372 | Tuberías PVC | 34 | 22 |
| LOSA:F1:36 | 401 | 401 | Márgenes del río | 36 | 23 |
| LOSA:F1:37 | 402 | 402 | Lecho del río | 37 | 24 |
| LOSA:F1:38 | 407 | 407 | Gaviones | 38 | sin fila de ejemplo |
| LOSA:F1:41 | 501 | 501 | Señalización | 41 | 25 |
| LOSA:F1:42 | 510 | 510 | Guardavias | 42 | sin fila de ejemplo |
| LOSA:F1:43 | 511 | 511 | Pavimento/Afirmado | 43 | 26 |
| LOSA:F1:44 | 530 | 530 | Visibilidad | 44 | 27 |
## MODULAR

Fuente SHA-256 `ac02633660b214639d8caff8895a5708e8e0891dbcb3d0d84767e960e49bdb89`. 10 elementos reales F.1.

| ID plantilla | Código F.1 | Código G.1 | Nombre | F.1 fila | G.1 fila |
| --- | --- | --- | --- | --- | --- |
| MODULAR:F1:22 | 168 | 168 | Estructura Metálica Bailey | 22 | 14 |
| MODULAR:F1:24 | 201 | 201 | Elevación Cuerpo del Estribo de Concreto Simple | 24 | 15 |
| MODULAR:F1:27 | 324 | 324 | Apoyo articulado de acero | 27 | 16 |
| MODULAR:F1:28 | 341 | 341 | Planchas Deslizantes | 28 | 17 |
| MODULAR:F1:30 | 401 | 401 | Márgenes del río | 30 | 18 |
| MODULAR:F1:31 | 402 | 402 | Lecho del río | 31 | 19 |
| MODULAR:F1:34 | 501 | 501 | Señalización | 34 | 20 |
| MODULAR:F1:35 | 510 | 504 | Muro de Concreto Armado en accesos | 35 | 21 |
| MODULAR:F1:36 | 511 | 511 | Pavimento/Afirmado | 36 | 22 |
| MODULAR:F1:37 | 530 | 530 | Visibilidad | 37 | 23 |

F.1 es la colección persistida; G.1 deriva aplicabilidad de esa misma colección. UNSET está incluido; NOT_APPLICABLE queda oculto sin borrar valores. Códigos originales y códigos de exportación son separados para no perder correspondencia; nombre observado y descripción global son datos de campo. No se copian cantidades/porcentajes de ejemplo a ficha nueva. Unidad/factor base provienen del G de la fuente cuando hay fila inequívoca; para los tres elementos LOSA omitidos en G se conserva la referencia AUXILIAR del catálogo ya recibido, documentada como fuente auxiliar.

F.2: 32 captions de ejemplo en D11:D42 por fuente; texto antes de dos puntos, 31 nombres únicos (una tubería repetida). Defaults disponibles en selector; registros reales se crean explícitamente, sin inventar 32 fotografías obligatorias. Se mantiene el texto/typos de fuente. Metadata y foto vinculados a UUID estable. Caption exportado NOMBRE: DESCRIPCIÓN, o solo nombre si falta descripción.
