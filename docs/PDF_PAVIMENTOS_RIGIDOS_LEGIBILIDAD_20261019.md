# Legibilidad del PDF de Pavimentos rígidos - 19/10/2026

**Resultado: GO técnico para compilar una candidata de prueba y verificarla físicamente. NO-GO para distribución/Release final sin comprobar el PDF nativo en un dispositivo.** No se generó otra APK, no se instaló nada y no hubo commit/push. El proyecto conserva la configuración 1.4.10 (24) y todos los avances anteriores.

## 1. Causas comprobadas

**Esquema pequeño.** El layout anterior limitaba el alto a 180 puntos y la escala a `min(32, 180/ancho)`. El cambio a A4 horizontal no ampliaba automáticamente el dibujo. Una muestra de 7 x 10 m ocupaba aproximadamente 257 x 180 puntos; en un tramo largo la última sección podía quedar igualmente angosta. La gráfica compartía el espacio disponible con tablas y texto.

**Cruces.** Los llamados se asignaban a filas inferiores por el orden de Y1, no por el borde más cercano ni por la geometría que realmente se veía después de proyectar un parche. Cada líder podía atravesar casi toda la muestra; cuando se llenaba una fila, las etiquetas volvían al extremo izquierdo de otra. Así, incluso un orden longitudinal razonable producía diagonales largas y cruces.

**Color y número.** `PavementDrawing` conserva etiquetas con fondo del color de severidad y número. Sin embargo, tanto el código del checkpoint `5de1242581dc460ea38d45006020d022f8d4da78` como el PDF anterior a este encargo invocaban ese dibujante con `labels=false`; el PDF creaba por separado números negros. Por tanto, existe el sistema coloreado en el dibujante, pero no hay evidencia para atribuir su ausencia del PDF exclusivamente al último cambio horizontal ni afirmar que todas las APK anteriores lo mostraban. El color de las geometrías sí se conservaba.

## 2. Cambios implementados

- El esquema rígido tiene vistas grandes independientes, con un objetivo de 750 puntos de ancho y hasta 350 de alto en A4 horizontal (842 x 595). Las pruebas exigen aprovechar al menos el 85% de ese ancho útil, sin estiramiento distinto entre ejes.
- Se equilibran las secciones longitudinales para evitar una última figura diminuta. En el ejemplo de 60 m, las vistas son de 15 m y el dibujo mide **750 x 350**, frente a unos **643 x 180** de una vista larga anterior: aproximadamente 2,27 veces su área de dibujo en la página.
- Una muestra corta y ancha se amplía mediante fajas transversales, conservando coordenadas absolutas y escala uniforme. El ejemplo de 7 x 10 m usa X=0-3,5 y X=3,5-7 m, ambas con Y=0-10 m, a **750 x 262,5**. No se deforma ni se omite la mitad restante: cada faja aparece en su propia vista. Las losas conservan sus números globales.
- Se reutiliza el dibujante actual. No se cambian la grieta D vectorial, el adosado/interior de parches, la rotura de esquina ni las marcas del sello de junta. El recorte de cada vista obtiene referencias de la geometría visible, incluida la proyección visual de los parches.
- Los distintivos se distribuyen entre el borde superior e inferior más cercano. Dentro del dibujo, los líderes son verticales y cortos hacia esa mitad; la redistribución horizontal ocurre fuera de la figura, con orden monotónico y cajas separadas. Las marcas de una columna cercana comparten una llamada de zona, identificada con contorno discontinuo. Es una agrupación de presentación, no una medición nueva.
- Si los distintivos no caben, se generan paneles de detalle con el mismo encuadre y escala, mostrando todos los ítems a lo largo de esos paneles. No se reduce la letra para apilarlos ni se descartan registros. El caso de 24 daños concentrados utiliza tres paneles de rótulos para su primera vista.
- Cada distintivo incluye **#ítem, código y severidad**, por ejemplo `#3 29/M`, con borde/franja del color existente de severidad y texto negro sobre blanco. La tabla repite la franja junto al mismo ítem y conserva las columnas de código/descripción y severidad: la lectura no depende únicamente del color.
- La tabla de cada panel sigue el orden de sus distintivos superiores e inferiores y repite la identificación de vista/panel al continuar en otra página. La tabla completa final conserva todos los daños, cantidades, unidades y descripciones. Un daño que aparece en más de una vista sigue teniendo el mismo correlativo; las repeticiones de presentación no son cantidades adicionales.
- Si la tabla no cabe debajo del esquema grande, continúa en la página siguiente. No se encoge la gráfica para hacerla caber. Los avisos de vistas sin daños se dibujan dentro del esquema y el bloque final de resultados se mantiene junto cuando cabe, evitando páginas adicionales con un aviso aislado.

La paleta sigue significando severidad: L/baja, M/media, H/alta y sin severidad cuando no corresponde. No se crean categorías técnicas por color. Las marcas referenciales rojas del 26 conservan el comportamiento previo del dibujante. Los daños globales sin localización no reciben una posición inventada; los históricos sin coordenadas conservan su explicación técnica por losa en el detalle.

## 3. Archivos de esta entrega

Base: `D:\Downloads\GBO_PRUEBAS_8B`.

| Archivo | Cambio |
|---|---|
| `app/src/main/java/com/tuempresa/inventariovial/pavement/PavementExports.kt` | Selección del nuevo layout rígido, tablas identificadas y paginación. El código del escritor Excel contenido en este archivo permanece idéntico. |
| `app/src/main/java/com/tuempresa/inventariovial/pavement/PavementRigidPdfLayout.kt` (nuevo) | Vistas, recorte de referencias, agrupación y distribución de distintivos/paneles. |
| `app/src/main/java/com/tuempresa/inventariovial/pavement/PavementRigidPdfScheme.kt` (nuevo) | Dibujo grande con ejes, losas, líderes y distintivos coloreados. |
| `app/src/test/java/com/tuempresa/inventariovial/PavementPdfLegibilityTest.kt` (nuevo) | Nueve pruebas de layout, datos, casos visuales y paginación. |
| `app/src/test/java/com/tuempresa/inventariovial/PavementPdfDeliveryIntegrationTest.kt` (nuevo) | Preparación real local de paquetes Drive y Descargas, sin transporte HTTP. |
| `tools/verificar_pdf_legibilidad_20261019.py` (nuevo) | Compilación/pruebas Kotlin aisladas, sin APK. |
| `tools/revisar_pdf_legibilidad_20261019.py` (nuevo) | Rasterización e inventario de las páginas para revisión visual. |

La rama de dibujo de asfalto/no pavimentado conserva su layout anterior; pasó su regresión focalizada. No se modificaron pantallas de captura, geometría guardada, repositorio, Room, trabajadores, sincronización, backend ni configuración de servicios.

## 4. Pruebas y ejemplos

**23 pruebas aprobadas, cero fallidas al cierre.**

| Grupo | Cantidad | Evidencia |
|---|---:|---|
| Legibilidad y casos nuevos | 9 | Ancho mínimo, escala uniforme, cobertura completa, cajas sin superposición, orden de líderes, paneles, IDs largos, históricos/globales, datos/fotos y paginación sin avisos sueltos. |
| Geometrías aprobadas | 4 | D horizontal/vertical/diagonal/invertida, bordes, parches y persistencia. |
| Integridad aprobada | 5 | PCI/Excel equivalentes y regresiones de esquinas/juntas. |
| PDF/Excel anteriores focalizados | 4 | Fronteras, reportes densos, coordenadas y asfalto. |
| Integración de preparación local | 1 | PDF/foto/Excel/JSON preparados para Drive y Descargas con SHA-256; mismo PDF en ambos destinos; caché/revisión estable, sin confirmar ni enviar a un servidor. |

La preparación se ejecutó realmente dentro de las pruebas Android locales, sobre Room y archivos ficticios. Se comprobó el contrato existente syncProtocol=3/pathVersion=4, UUID/revisión y hashes de todos los archivos preparados; la repetición reutiliza el paquete. La intención de sincronización quedó exactamente como estaba, con revisión confirmada cero. **No se hizo una subida ni se fabricó un ACK.** Los puntos de exportación manual, copia automática y preparación Drive usan `PavementPdf.write`; no se cambió ese contrato.

Resultados finales: `docs/pdf-legibilidad-20261019/RESULTADOS.json`, `test-after.log` (`OK (22 tests)`) y `test-integration-final.log` (`OK (1 test)`). La compilación fue Kotlin/JVM aislada contra dependencias locales disponibles; no equivale a una compilación integral Gradle/R8 ni a una prueba física.

Ejemplos en `output/pdf/legibilidad-rigidos-20261019`:

| Caso | Archivo | Páginas |
|---|---|---:|
| Pocos daños dispersos | `pocos-SIMULADO.pdf` | 3 |
| 24 daños concentrados | `concentrados-SIMULADO.pdf` | 11 |
| Marcas próximas y severidades distintas | `cercanos-SIMULADO.pdf` | 8 |
| Tramo de 60 m y esquina final | `largo-SIMULADO.pdf` | 7 |

Se revisaron visualmente las **29 páginas**, mediante láminas de contacto completas y ampliaciones de gráficos/tablas. Todos los tamaños son 842 x 595. `paginas/` contiene los PNG y `SHA256SUMS.txt` identifica los cuatro PDF. Las imágenes de iteraciones anteriores se conservaron separadas para no mezclarlas con las páginas vigentes.

Estos ejemplos ejecutan el dibujante y paginador reales de la aplicación sobre Canvas/Robolectric. El adaptador de pruebas `SyntheticPdf146` empaqueta esas páginas en un PDF rasterizado: **no es el PDF vectorial nativo de una tablet ni una captura física**. Los datos son ficticios. Los avisos de curvas PCI pendientes del fixture se conservan; esta corrección no valida ni modifica curvas o resultados de cálculo.

## 5. Conservación, límites y siguiente paso

- Respaldo previo: `tmp/pdf-legibilidad-20261019/FUENTES_ANTES.zip`, con 749 fuentes/configuración pública; propiedades privadas inventariadas solo por hash. Diff selectivo: `docs/pdf-legibilidad-20261019/CAMBIOS_ESTA_ENTREGA.diff`.
- Solo se modificó un archivo existente de aplicación y se añadieron los dos módulos de presentación descritos. Se comprobó que el bloque `PavementExcel` es idéntico al respaldo y que las geometrías/cálculos anteriores no cambiaron.
- Las cuatro propiedades privadas y las diez APK anteriores mantienen sus hashes. Versiones, firma, applicationId y esquema Room siguen intactos. No se accedió a tablet, Google, Apps Script, Servicio de Usuarios, GBO LAB ni registros reales. HEAD no cambió; no hubo commit/push.
- El número de páginas puede aumentar para mantener legibles las figuras y las tablas. Una vista/panel no representa un nuevo registro. El registro completo final permite conciliar las repeticiones visuales sin sumar cantidades otra vez.
- Los PDF ya publicados y los paquetes de sincronización congelados no se reescriben con este cambio. El layout se aplica al generar un PDF nuevo. Se conserva la caché idempotente de entregas; no se invalida automáticamente para sustituir un documento pendiente o confirmado.

**GO:** revisar estos ejemplos y compilar una candidata de prueba tras la conformidad visual. La prueba física debe abrir un PDF recién generado en la tablet, revisar tamaño al 100%, tablas/vistas, D, parches, esquina y junta 26; comprobar su copia en Descargas y, cuando esté autorizado, la entrega normal en Drive. **NO-GO para distribuir una Release final sin esa comprobación.** En este encargo no se generó ni distribuyó una nueva APK.
