# Regla definitiva de grieta D y anclaje de parches - 19/10/2026

**Implementado. GO técnico para compilar una candidata de prueba. Pendiente validar el PDF nativo y la interacción física antes de distribuir una Release.** No se generó, instaló ni distribuyó APK; no hubo commit/push.

## Regla aplicada

Esta entrega **reemplaza la instrucción anterior de orientar D por el vector P1-P2**. Los informes y ejemplos anteriores se conservan como respaldo histórico, pero esa regla ya no describe el comportamiento actual.

El cálculo se realiza en los metros originales del esquema, antes de cualquier transformación:

```kotlin
val b = abs(x2 - x1)
val h = abs(y2 - y1)
val horizontal = b >= h
```

### Daño 24

- Se dibujan tres grietas aproximadamente horizontales si b>=h, verticales si h>b. La longitud corresponde a la dimensión predominante del rectángulo, sin seguir su diagonal ni elegir una junta para cambiar el eje.
- Los puntos se normalizan mediante mínimos/máximos; invertirlos conserva exactamente la geometría para el mismo UUID. El empate es horizontal.
- Se mantienen irregularidades transversales pequeñas, sin inclinación global. Los extremos de cada trazo siguen el mismo eje y cubren toda la extensión disponible dentro de la losa.
- Una captura exactamente horizontal/vertical conserva la pequeña banda visual referencial existente para separar los tres trazos. Esa banda no es una cantidad ni una medida persistida. Dos puntos coincidentes no producen una figura inventada ni valores no finitos.
- El eje se decide con b/h originales incluso si el dibujo se recorta al cruzar losas. Los históricos sin puntos conservan su símbolo referencial; no se les inventa un rectángulo medido.

### Daños 29/30

Para **nuevos adosados**:

| Dimensiones originales | Bordes candidatos | Desempate |
|---|---|---|
| b>h | Inferior y superior | Inferior si equidistan |
| h>b | Izquierdo y derecho | Izquierdo si equidistan |
| b=h | Los cuatro, por distancia mínima | Izquierdo, derecho, inferior, superior |

Se traslada el contorno al borde elegido, sin girarlo ni deformarlo, conservando tamaño, trama, coordenadas originales, severidad, UUID, correlativo y cantidad PCI. Un borde perpendicular más próximo no gana la selección. El modo Interior sigue conservando la ubicación interior.

El adosado nuevo requiere que la figura localizada quepa en una losa. Si no cabe, o la selección de losas es ambigua para adosar, aparece una explicación en la vista previa; no se reduce silenciosamente. La validación se repite dentro del guardado transaccional existente: el fallo revierte también el daño y la revisión. Para representar una reparación realmente extendida en varias losas puede conservarse el modo Interior y las medidas originales.

## Históricos y persistencia

Cambiar el algoritmo indiscriminadamente habría desplazado los registros ANCHORED ya guardados. Para evitarlo se añadió **solo la clave opcional `anchorRule=PARALLEL_AXIS_1`**, dentro de la anotación visual `_gboPatchVisuals` existente, manteniendo su versión 1. No se modificaron entidades, columnas, migraciones Room ni el contrato externo de sincronización.

- Un registro sin anotación visual conserva Interior, como antes.
- Un ANCHORED histórico sin esa clave conserva su algoritmo y ubicación anteriores, incluidos los empates de esquina. No se reubica por abrir la ficha ni por confirmar manteniendo el modo.
- Un adosado nuevo lleva la clave y reproduce la regla paralela al cerrar y reabrir.
- Interior siempre se respeta. Si el operador guarda Interior y posteriormente elige Adosado, esa nueva elección utiliza la regla actual.
- Las otras anotaciones y aportes del JSON se conservan. No se ejecutó una migración o reescritura de registros existentes.

Por esta protección, **un adosado histórico puede seguir en un borde que la regla nueva ya no escogería**. Es conservación deliberada de su ubicación, no una selección nueva incorrecta.

## Consistencia Canvas/PDF

Ambos consumen `PavementRigidVisualGeometry`. El PDF solo aplica su transformación de presentación. **Una D horizontal en X/Y originales se ve vertical tras el giro de 90 grados del PDF, junto con sus ejes; una vertical original se ve horizontal.** No se vuelve a calcular b/h usando las coordenadas de la página.

Se verificó la transformación comparando píxeles del dibujante Android con los del renderizador PDF rotado, con tolerancia de antialias, además de las comprobaciones métricas. El diseño general aprobado del PDF permanece intacto: `PavementExports.kt`, `PavementRigidPdfLayout.kt` y `PavementRigidPdfScheme.kt` conservan exactamente sus hashes del respaldo de entrada.

## Archivos modificados

Base: `D:\Downloads\GBO_PRUEBAS_8B`.

| Archivo de aplicación | Cambio |
|---|---|
| `app/src/main/java/com/tuempresa/inventariovial/pavement/PavementRigidVisualGeometry.kt` | Sustitución del vector por eje predominante y filtrado de bordes paralelos; conservación del anclaje histórico. |
| `app/src/main/java/com/tuempresa/inventariovial/pavement/PavementPatchVisuals.kt` | Marca visual opcional, conservación de históricos y comprobación de tamaño/selección. |
| `app/src/main/java/com/tuempresa/inventariovial/pavement/ui/PavementDialogs.kt` | Mostrar el motivo de una proyección inválida sin cerrar ni bloquear la elección Interior. |

Se actualizaron los tests `PavementFinalGeometryTest.kt` y los rótulos de `PavementFinalExamplesTest.kt` para retirar la expectativa diagonal y el empate de esquina para anclajes nuevos. Nuevos tests: `PavementAxisAnchoringTest.kt`, `PavementAxisAnchoringUiTest.kt`, `PavementAxisExamplesTest.kt`. Herramientas locales: `tools/verificar_orientacion_definitiva_20261019.py` y `tools/revisar_orientacion_definitiva_20261019.py`.

No se modificaron sincronización, copia local, backend, GBO LAB, cálculo PCI, Excel ni servicios/configuración privada.

## Pruebas y evidencias

**31 pruebas únicas aprobadas, cero fallidas.**

| Grupo | Pruebas |
|---|---:|
| Regla nueva, excepciones, históricos, Room, rollback y comparación Canvas/PDF | 9 |
| Interfaz: aviso de tamaño y elección Interior sin crash ni reducción | 1 |
| Ejemplos visuales con el dibujante y PDF actuales | 1 |
| Geometría previa actualizada a la nueva especificación | 4 |
| Integridad PCI/Excel y regresiones de esquina/juntas | 5 |
| Regresiones visuales/segmentos anteriores | 11 |

Casos cubiertos: D de 4x1, 1x4, 3x2, 2x3, cuadrada e invertida; ejes puros, longitud cero y recorte entre losas; parches superior/inferior/izquierdo/derecho para ambos códigos; borde perpendicular más cercano; empate cuadrado; Interior explícito; anclaje histórico; persistencia después de cerrar/reabrir Room; rechazo de tamaño sin guardado parcial. Se comprobaron también cobertura, escala y distribución de etiquetas del layout PDF aprobado.

La compilación fue Kotlin/JVM aislada contra las dependencias locales disponibles, sin empaquetar APK. Log principal: `docs/orientacion-definitiva-20261019/test-after.log` (`OK (31 tests)`). Tras ajustar únicamente los tamaños de los parches pequeños ficticios de la lámina, se repitió **solo el caso visual**, con `OK (1 test)`; no se cuenta como una prueba única adicional. No se volvieron a ejecutar las auditorías generales ni los ensayos de Google.

Ejemplos en `output/pdf/orientacion-definitiva-20261019`:

- `regla-definitiva-Canvas-real.png`: trece casos comparables en metros originales, incluidos los cuatro lados, las dos excepciones de borde perpendicular e Interior.
- `regla-definitiva-PDF-horizontal-SIMULADO.pdf`: seis páginas, todas de 842 x 595, con esos casos en el renderizador horizontal conservado. Se revisaron visualmente las seis páginas y la lámina completa.
- `SHA256SUMS.txt` y `PAGINAS_REVISADAS.json`: identificación de los artefactos.

Son datos ficticios y dibujo real mediante Canvas/Robolectric; **no son capturas de tablet**. El adaptador de pruebas empaqueta las páginas en un PDF rasterizado y no sustituye la comprobación del escritor nativo de Android. Los avisos de curvas PCI pendientes del fixture se conservaron, sin modificar el cálculo.

## Conservación y dictamen

Respaldo previo: `tmp/orientacion-final-20261019/FUENTES_ANTES.zip` (753 fuentes/configuración pública), inventario en `baseline.json` y diff selectivo en `docs/orientacion-definitiva-20261019/CAMBIOS_ESTA_ENTREGA.diff`. Los respaldos, informes y APK anteriores permanecen conservados.

Las cuatro propiedades privadas y las diez APK existentes mantienen sus hashes. La versión sigue en 1.4.10 (24); no se cambió firma, applicationId ni esquema Room. HEAD permanece sin cambios; no hubo commit/push, instalación, acceso a tablet, Google ni datos reales.

**GO técnico para una nueva compilación candidata de pruebas**, tras revisar los ejemplos. **NO-GO para distribución general sin comprobación física** de la captura, reapertura y PDF nativo. No se generó automáticamente otra candidata en este encargo.
