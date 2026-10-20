# Daño 24: abanico y cuatro esquinas — 19/10/2026

Implementada la representación descrita por el propietario: seis grietas inclinadas e irregulares, tres ramificaciones conectadas y mayor concentración cerca de la esquina. Sin relleno ni contorno de reparación. **38 pruebas focalizadas aprobadas**: 37 de geometría, interfaz e integridad, y una de generación visual. No se generó APK.

**Limitación de la entrega:** el adjunto recibido contiene únicamente `Texto pegado.txt`, sin la imagen mencionada. No se localizó el abanico exacto en los respaldos examinados. Por ello, esta es una reconstrucción según la especificación escrita, pendiente de cotejo visual con la referencia original; no se afirma haber recuperado su figura exacta.

## Recuperación comprobada

Se revisaron copias de fuentes de 1.4.6, 1.4.7, 1.4.8, 1.4.9 y los respaldos locales de las modificaciones del 19/10, incluidos sus ZIP. El inventario de siete variantes únicas relevantes, rutas y hashes está en `d24-abanico-20261019/RECUPERACION.json`.

El respaldo `tmp/pavimentos-20261019/respaldo/app/src/main/java/com/tuempresa/inventariovial/pavement/ui/PavementScheme.kt` dibujaba el 24 como `NETWORK`: malla irregular y rectángulo. Las versiones posteriores encontradas usaban bandas/líneas, no el abanico solicitado. La lámina incluye una reproducción de aquella malla, claramente identificada como código antiguo encontrado, **no como imagen del propietario**.

Se reutiliza sin cambios la primitiva histórica `PavementSketch.crack`: irregularidad determinista por identificador y extremos fijos. Su función coincide con la del respaldo anterior; SHA-256 normalizado `2177d120d99cf51cf02d1bd9f61fc296f42579fda1be4c6a85ead2f6e8c70be1`. Evidencia en `PRIMITIVA_HISTORICA.json`.

## Esquina, orientación y casos ambiguos

- Usa la losa explícitamente indicada y sus límites métricos. Los dos puntos deben pertenecer a esa losa; no se adjudica otra silenciosamente.
- Conserva `b=abs(x2-x1)` y `h=abs(y2-y1)`. Genera el abanico normalizado y escala X por b e Y por h. Refleja X/Y para las cuatro esquinas; no rota según el vector P1–P2.
- Los radios relativos `.12, .21, .34, .50, .72, 1.0` concentran las grietas cerca de la esquina. Las ramas empiezan y terminan en vértices de esas grietas. Todos los trazos quedan dentro de la losa.
- Solo infiere una esquina si el rectángulo medido está cerca de dos bordes adyacentes: como máximo 15% de cada dimensión de losa y sin empate entre lados opuestos (margen de 1%). Es una regla conservadora de presentación, no una regla de medición PCI.
- El diálogo D24 muestra siempre una vista previa corregible mediante los campos de coordenadas y losas existentes. Informa explícitamente que el dibujo se ajusta a una esquina cercana y conserva los puntos originales.
- Interior distante, empate, varias losas, puntos fuera de la losa, ausencia de puntos o dimensión nula: muestra advertencia y marcadores en cruz; **no inventa un abanico**. La pantalla marca «revisar esquina». Los históricos sin coordenadas conservan su registro y muestran un marcador referencial por losa, sin atribuirles una esquina observada.
- No se bloqueó ni cambió el callback de guardado. No se guarda una nueva elección de esquina ni se escriben coordenadas proyectadas. Para obtener el abanico de un caso ambiguo, el operador debe corregir sus puntos/losa y revisar la vista previa. El PDF comparte los marcadores; las cruces no son grietas.

## Código de aplicación cambiado en esta entrega

| Archivo bajo `app/src/main/java/com/tuempresa/inventariovial/pavement/` | Cambio |
|---|---|
| `PavementDurabilityGeometry.kt` | Nuevo proyector D24 y advertencias, sin persistencia. |
| `PavementRigidVisualGeometry.kt` | Despacha D24 al proyector común y retira sus líneas paralelas. Conserva el algoritmo de parches. |
| `ui/PavementDialogs.kt` | Vista previa D24 y corrección con campos existentes; guardado intacto. |
| `ui/PavementScheme.kt` | Indica «revisar esquina» para D24 no resuelto. |

No cambiaron `PavementExports`, `PavementRigidPdfLayout`, `PavementRigidPdfScheme`, modelos, Room, repositorio, PCI, Excel, sincronización, copias locales, configuración ni servidores. Los parches 29/30 conservan su anclaje paralelo y la compatibilidad histórica. Daños 22 y 26 conservan su implementación. Los hashes y el diff respecto al inicio de **esta** tarea están en `d24-abanico-20261019/`.

## Pruebas y alcance real

Compilación focalizada Kotlin 2.2.10 de los cuatro archivos de aplicación y pruebas, contra dependencias locales en caché: **PASS**. JUnit/Robolectric API 34: **37 PASS**, más **1 PASS** de ejemplos visuales. No equivale a un ensamblado Gradle completo ni a una prueba física.

Verificado: cuatro esquinas; b/h de 4/1, 1/4, 3/2, 2/3 y 3/3; puntos inversos; losas 3.5×5, 7×3 y 2×8; límite de losas; ambigüedad; ramas conectadas; densidad; extensión exacta; ausencia de contornos cerrados; invariancia de entidades, cantidad por losas, resultado PCI, contenido XLSX y JSON nativo antes/después de dibujar. Conservados los tests de parches, persistencia de su modo, daño 22, anotaciones 26 y maquetación PDF. Las expectativas antiguas de D24 paralelo se sustituyeron por las nuevas.

La prueba de interfaz edita puntos interiores, observa la advertencia y luego el abanico, y confirma que el callback normal recibe el mismo UUID y los valores escritos. Doce comparaciones de píxeles entre el dibujante real Android y el renderer PDF girado verifican cuatro esquinas × tres proporciones, con tolerancia de rasterizado de 2 píxeles y correspondencia superior al 85%. También se verifican recortes entre segmentos e identidad de ítem.

El primer intento JUnit no ejecutó casos porque faltaba la carpeta temporal corta; se creó y la ejecución completa pasó. El log inicial se conserva. La compilación presenta avisos de dependencias Java y una condición redundante del preview; no errores.

## Evidencia visual y límites

En `output/pdf/d24-abanico-20261019/`:

- `D24-cuatro-esquinas-Canvas.png`: 12 combinaciones de esquina/proporción, puntos inversos, caso ambiguo y código antiguo encontrado.
- `D24-losas-rectangulares-y-junta.png`: distintas dimensiones y proximidad a otra losa.
- `D24-PDF-horizontal-SIMULADO.pdf`: seis páginas revisadas visualmente, con el layout aprobado y 13 daños ficticios. Se usan los mismos dibujantes de aplicación, mediante el adaptador raster de pruebas; **no es evidencia de PdfDocument nativo en una tablet**. El registro conserva los puntos originales, aunque el símbolo esté proyectado al borde.

El fixture muestra el estado PCI preexistente «falta curva rígido 24:M», sin inventar un resultado numérico; la prueba de integridad comprueba que dibujar no cambia ese resultado. No se añadieron curvas ni se modificó el cálculo.

## Estado y recuperación

**GO técnico para una compilación posterior. Pendiente de aprobación visual definitiva** por ausencia de la imagen original y pendiente de prueba física del PDF nativo. No declarar recuperada exactamente la figura de referencia ni aprobada para campo.

No APK, instalación, commit, push ni acceso a servicios. Diez APK y cinco `.properties` de raíz conservan sus hashes. Respaldo reversible del estado inicial en `tmp/d24-abanico-20261019/FUENTES_ANTES.zip`; no contiene los archivos privados de raíz. Preservados los respaldos y avances anteriores. Para revertir solo esta entrega, usar el diff y restaurar únicamente sus archivos desde ese respaldo, conservando antes otra copia; no restaurar globalmente el árbol de Git.
