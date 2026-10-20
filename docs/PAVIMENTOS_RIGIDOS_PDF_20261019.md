# Pavimentos rígidos y PDF — correcciones del 19/10/2026

Proyecto: `D:\Downloads\GBO_PRUEBAS_8B`. Referencia conservada: **1.4.9 (23)**. Cambios implementados sobre el árbol local existente, con respaldo previo de sus avances. No se compiló, firmó, instaló ni distribuyó APK; no se utilizó Gradle para estas verificaciones. No se trabajó en GBO LAB ni en ningún servidor Apps Script.

## Causas y correcciones implementadas

**No había un intercambio de códigos.** El catálogo conserva 22/24/26/29/30 y sus nombres. El 24 utilizaba la receta NETWORK, que dibujaba un rectángulo con una cuadrícula de grietas cruzadas; se parecía a una superficie reparada. Los códigos 29/30 ya usaban PATCH, pero el dibujo genérico y la ubicación de las etiquetas dificultaban diferenciarlos. Se sustituyó únicamente la representación de estos tres daños rígidos, sin intercambiar sus códigos ni modificar mediciones.

**Pantalla y PDF ya compartían `PavementDrawing`.** Se mantiene ese dibujante y se añade `PavementRigidVisualGeometry`, una geometría común expresada en metros. No se creó un segundo algoritmo de fallas para el PDF.

| Daño | Comportamiento actual |
|---|---|
| 22, esquina | Se conserva la proyección aprobada a dos bordes adyacentes, con las cuatro esquinas y rechazo de ambigüedades. Los toques originales permanecen intactos. La revisión y pruebas verificaron que este código ya resolvía las cuatro orientaciones. |
| 24, durabilidad D | Tres trazos irregulares próximos y aproximadamente paralelos. Cerca de un borde de la losa se orientan según ese borde; en una zona interior se usa el eje dominante de la zona capturada. Se mantienen dentro de su extensión: no se traslada el daño hacia una junta distante ni se dibuja un contorno de parche. |
| 29, parche grande | Rectángulo cerrado de extensión métrica real, con trama diagonal acotada. Las proporciones y posición proceden de los dos puntos. Si alcanza una junta, se conserva esa conexión; si es interior, permanece interior. No se inventa una extensión hasta un borde. |
| 30, parche pequeño | Mismo carácter de reparación, escala y posición reales. Se probó un parche interior de 0,36 m². El símbolo no cambia automáticamente su código, severidad, cantidad ni clasificación PCI. |
| 26, sello | Al seleccionar el código se inicia la selección de juntas. La vista previa no guarda registros. Se puede volver a tocar para corregir, deshacer o cancelar. «Confirmar juntas y severidad» pasa por el diálogo y las validaciones existentes; al aceptar, daño GLOBAL y marcas se guardan en una sola transacción local. |

Los históricos sin coordenadas siguen sin extensión medida: las recetas son **referenciales**, no reconstrucciones de la superficie observada. No se asignan medidas a esos símbolos. Los históricos con malla inconsistente conservan sus marcas y requieren la revisión/limpieza explícita anterior; no se permite confirmarlas silenciosamente sobre otra malla.

**Límite del formato actual:** dos puntos representan un rectángulo alineado con X/Y; permiten cambiar orientación predominante, proporciones y ubicación, pero no reconstruyen un polígono inclinado arbitrario. Esa captura adicional sería una propuesta posterior; no se introdujo un formato persistido nuevo. No se ha implementado una reclasificación automática de parches por área gráfica.

Referencia consultada localmente: `D:\Downloads\Manual_de_PCI_y_URCI_para_calles_y_carre.pdf`, 286 páginas. Se contrastaron las secciones 136–139, 143–146, 150–153 y 161–166 y las figuras correspondientes. El daño 26 se mantiene como evaluación general de la unidad, no por cada losa o junta marcada. No se modificó la lógica de cálculo PCI.

## PDF horizontal

- Todas las páginas se generan apaisadas, **842 × 595 puntos**. La geometría se gira visualmente un cuarto de vuelta: Y longitudinal crece hacia la derecha y X transversal hacia abajo; ambos ejes, el origen de sección y los metros están indicados.
- Área prevista de esquema: hasta **718 × 180 puntos**, con una sola escala por muestra. No se deforma la losa ni se estira artificialmente un tramo corto para llenar la hoja.
- Los cortes siguen el eje longitudinal y se alinean con filas completas de losas cuando caben. Una falla que cruza un límite conserva su UUID/correlativo; se muestra por continuación y se advierte que no debe volver a sumarse.
- Losas con numeración global continua, texto horizontal y fondo legible. Las referencias de daños se colocan fuera del esquema, con líneas guía cuando la densidad lo permite; en casos densos se usa la leyenda y las coordenadas de la tabla.
- Se muestran PR de sección cuando PR inicial/final y longitud registrada son coherentes, incluyendo dirección decreciente. Si no lo son, se conserva el PR general y no se inventa una proyección por sección.
- Se amplían texto y tablas al ancho de la página y se conserva el detalle, severidades, cantidades, observaciones, fotos y resumen existentes. Una sección vacía se identifica como tal.

## Archivos

Base de fuentes: `app/src/main/java/com/tuempresa/inventariovial/pavement/`.

| Archivo | Cambio |
|---|---|
| `PavementExports.kt` | Orientación, transformación, ejes, tablas y referencias del PDF. El texto del exportador Excel anterior a `object PavementPdf` permanece idéntico, normalizando únicamente saltos de línea. |
| `PavementSketch.kt` | Receta específica D; escala, segmentación horizontal, transformación y referencia PR del PDF. |
| `PavementRigidVisualGeometry.kt` | Nuevo: trazos métricos de D y superficies reparadas, sin persistencia ni cantidades. |
| `ui/PavementScheme.kt` | Usa la geometría compartida; separa rótulos de losas de los trazos cuando el PDF necesita girarlos. |
| `ui/PavementWorkspace.kt` | Selección directa, vista previa, deshacer, cancelar y confirmación normal del 26. |
| `PavementRepository.kt` | Añade `saveDamageWithJointMarks`, envolviendo las dos operaciones y validaciones existentes en una transacción. |

Pruebas nuevas, bajo `app/src/test/java/com/tuempresa/inventariovial/`: `PavementVisualCorrectionsTest.kt`, `PavementVisualExamplesTest.kt`, `PavementJointFlow20261019Test.kt`, `PavementJointPreviewUiTest.kt` y `PavementLegacyPdfFocusedTest.kt`.

Pruebas existentes ajustadas a las dimensiones/segmentación nuevas: `PavementParallelCTest.kt`, `PavementObservations10ITest.kt`, `PavementReportTest.kt`. No se eliminaron sus comprobaciones.

Herramientas nuevas: `tools/verificar_pavimentos_sin_apk_20261019.py` y `tools/revisar_pdf_pavimentos_20261019.py`.

## Verificación y límites

**Resultado final: 27 casos distintos aprobados, 0 fallos pendientes.** Se conservó el ensayo inicial fallido y no se sumaron como casos nuevos las regeneraciones repetidas de ejemplos. Se revisaron visualmente las 14 páginas posteriores y las 12 anteriores.

Los resultados finales, hashes y conservación de archivos se registran en `docs/pavimentos-20261019/RESULTADOS.json`. Los logs completos permanecen en `tmp/pavimentos-20261019/`.

Se ejecutan pruebas locales de geometría, catálogo, cuatro esquinas, bordes/intersecciones, contornos/tramas, escalas, PR, continuidad, guardado transaccional, cancelación, persistencia al cerrar/reabrir, UUID, severidad, cantidades, JSON y Excel. La prueba interactiva Robolectric comprueba que seleccionar 26 activa la confirmación directamente, un toque muestra la marca, deshacer la retira y cancelar deja Room sin un daño nuevo ni marcas guardadas.

El primer ensayo detectó un extremo de trama ligeramente fuera del contorno por redondeo decimal: se corrigió acotando ambos extremos al rectángulo y se conservó el log del fallo. Los ajustes del arnés de compilación aislada también están registrados. No se ocultaron fallos ni se cambiaron cantidades para hacerlos pasar.

**Las imágenes y PDFs son ejemplos sintéticos del dibujante real**, ejecutado en Canvas nativo de Robolectric. El adaptador de pruebas `SyntheticPdf146` empaqueta esas páginas como PDF raster porque este entorno no implementa el escritor PdfDocument nativo. Se genera además una comparación con el dibujante anterior, compilado desde el respaldo tomado antes de editar. Esto permite revisar geometría y disposición; **no demuestra todavía el escritor PDF vectorial de una tablet, el tacto físico ni la impresión real**.

La generación de estos ejemplos no requiere APK, instalación, cambios en firmas o configuración de servicios. Las pruebas usan Room y datos ficticios propios del entorno de pruebas. No se ejecutaron pruebas remotas de sincronización. El motor PCI, catálogo/modelos, esquemas, DAO, migraciones y el protocolo remoto se conservaron.

## Ejemplos y revisión

Carpeta: `D:\Downloads\GBO_PRUEBAS_8B\output\pdf\pavimentos-20261019`.

- `before-corto-SIMULADO.pdf` / `after-corto-SIMULADO.pdf`: 10 m.
- `before-largo-SIMULADO.pdf` / `after-largo-SIMULADO.pdf`: 60 m.
- `before-multiseccion-SIMULADO.pdf` / `after-multiseccion-SIMULADO.pdf`: 100 m.
- `before-croquis-Canvas.png` / `after-croquis-Canvas.png`: mismo caso sobre Canvas compartido.
- `paginas/`: cada página renderizada y hojas de contacto para revisión completa. `PAGINAS_REVISADAS.json`: tamaños, número de páginas y hashes de los PDFs finales.

La revisión final comprueba orientación, límites, continuidad, identificación de fallas, leyendas, tablas y ausencia de texto cortado. Las limitaciones físicas indicadas arriba siguen pendientes aunque las regresiones estén aprobadas.

## Reversión sin perder los avances anteriores

1. Conservar una copia del estado actual y consultar `RESULTADOS.json`; si alguno de estos archivos volvió a cambiar después de esta entrega, comparar antes de restaurar.
2. El respaldo previo está en **`tmp/pavimentos-20261019/respaldo`**, conservando rutas relativas. Restaurar únicamente los **cinco archivos de producción existentes** modificados y los **tres archivos de pruebas existentes** listados arriba. Se recupera exactamente el trabajo local que había antes de esta corrección, no un commit histórico.
3. Mover a un archivo de respaldo fuera de `app/src` el archivo nuevo `PavementRigidVisualGeometry.kt` y las cinco pruebas nuevas, para que no queden referencias incompatibles al revertir. Conservarlos; no hace falta borrar nada.
4. No usar `git reset`, `git clean`, restauración global ni sustitución completa del proyecto. No tocar Room, registros, fotos, videos, credenciales, APK anteriores ni la carpeta independiente estable.

## Etapa posterior: requiere autorización separada

No se generó una APK candidata ni se modificó versionCode/versionName. Antes de compilar, acordar la nueva candidata y verificar configuración local/firma histórica sin exponer valores privados. Mantener las APK anteriores como respaldo.

Tras autorización: compilar la variante acordada en Android Studio, sin publicar servidores ni instalar automáticamente. En una tablet de pruebas respaldada verificar primero applicationId/firma y compatibilidad de actualización; no desinstalar ni limpiar datos ante incompatibilidad.

Prueba física breve, exclusivamente con datos ficticios y entrega local:

1. Sin Internet, abrir un histórico 1.4.9 y una muestra ficticia rígida nueva. Verificar cuatro esquinas, D junto a juntas horizontales/verticales, un parche grande de borde y otro interior, y un parche pequeño interior.
2. Seleccionar 26 directamente, marcar dos juntas y un cruce, deshacer/cancelar; repetir, confirmar la severidad normal y guardar. Cerrar/reabrir: mismos UUID, correlativos, coordenadas, severidad, fotos y marcas.
3. Exportar PDF **nativo** y Excel; abrirlos en la tablet. Comparar las figuras con pantalla, los números con la tabla y la continuidad en 10/60/100 m. Probar zoom e impresión apaisada al 100 %.
4. No habilitar entrega remota ni enviar esos ejemplos a Drive sin autorización. Una prueba física aprobada es el paso que falta antes de considerar resuelta la incidencia para campo.
