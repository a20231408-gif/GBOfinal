# Pavimentos rígidos y copia local - corrección final, 19/10/2026

Resultado: **GO técnico para compilar una próxima candidata, condicionado a la revisión visual del propietario. NO-GO para distribuir o declarar apta para campo sin prueba física.** No se generó, instaló ni distribuyó una APK. La configuración sigue en 1.4.10 (24). No se decidió una nueva numeración.

## Causas y correcciones

### 24 - Grieta de durabilidad D

El dibujante anterior elegía vertical/horizontal mediante la distancia del rectángulo a los bordes; un borde próximo podía prevalecer sobre los puntos capturados. Después dibujaba tres líneas sobre un eje, sin usar el vector P1-P2. Además, el daño 24 se validaba como AREA: una vertical u horizontal exacta no podía confirmarse como zona parcial de ancho y largo positivos.

Ahora se usan dx, dy, longitud euclidiana, vector unitario y perpendicular. Tres trazos longitudinales con irregularidad pequeña se separan perpendicularmente y se recortan segmento a segmento. Se canoniza el sentido para que invertir P1/P2 conserve el resultado. No se sustituyen los puntos medidos por los puntos del dibujo. El 24 usa captura LINE dentro de SLABS: la cantidad contractual sigue siendo el número de losas, no la longitud ni el área visual.

El recorte conserva la intersección de la zona registrada con cada losa. Si los puntos son exactamente horizontales/verticales, su rectángulo no tiene espesor: se usa exclusivamente una banda referencial de dibujo del 3,5% de la menor dimensión de losa, recortada al límite de esta. No se guarda como medida ni alimenta PCI. Los históricos sin coordenadas mantienen sus símbolos referenciales.

La rotación de 90 grados del PDF se conserva junto con sus ejes: Y longitudinal va a la derecha y X transversal hacia abajo. Una vertical del croquis puede verse horizontal en esa página porque también giraron los ejes; no se vuelve a inferir una orientación distinta.

### 29/30 - Parche grande/pequeño

El dibujante anterior solo trazaba el rectángulo medido; no existían proyección a bordes ni preferencia persistida. Ahora el formulario nuevo ofrece ADOSADO por defecto e INTERIOR explícito, con vista previa real antes de aceptar. El adosado traslada el rectángulo hasta el borde más próximo; un empate entre bordes adyacentes permite una esquina. Conserva dimensiones y orientación del rectángulo registrado; no lo rota, escala ni convierte en otra medición. Se admiten los cuatro lados.

La preferencia se guarda por UUID de daño en `_gboPatchVisuals`, versión 1, dentro de `PavementSampleEntity.sicDataJson`, el mismo sobre JSON existente que conserva los aportes SIC y las marcas del 26. Incluye identidad y dimensiones de malla. No añade columnas Room, migraciones, campos contractuales Excel ni acciones del protocolo remoto. Daño y preferencia se confirman en una transacción y una revisión. Los registros históricos sin esta preferencia siguen en INTERIOR; no se desplazan automáticamente. Sin coordenadas se conserva el símbolo referencial, sin inventar un borde observado. Metadatos desconocidos o de otra malla no autorizan el desplazamiento.

Canvas y PDF comparten `PavementRigidVisualGeometry`. La selección de secciones PDF usa los límites de los trazos proyectados. Los puntos originales, UUID, correlativo, severidad y cantidad permanecen separados de esta transformación visual.

### Copia local - mensajes y concurrencia

Se confirmaron en el código las siguientes causas:

1. `LocalFilesList` presentaba, para cada trabajo, un título de éxito/fallo seguido del mensaje persistido con el mismo resultado. Era una repetición de presentación comprobable, no prueba de dos ejecuciones.
2. Mostraba hasta seis trabajos de revisiones distintas sin identificar su revisión. Un éxito anterior y un fallo nuevo podían aparecer juntos como si describieran la misma copia. `SUPERSEDED` ya se excluía; los éxitos y fallos históricos sí se conservaban.
3. `saveDamage` actualiza la revisión; el outbox automático durable programa documentos después de agrupar cambios durante cinco segundos. La generación lee la ficha y sus daños/fotos y, al terminar, compara la revisión. Otro guardado durante ese intervalo causa legítimamente «La ficha cambió durante la preparación». La prueba con barreras reproduce ese caso y conserva el fallo, los originales y la copia verificada anterior.
4. `requestCurrent` ya envolvía su solicitud en transacción y el drenaje ya tenía un Mutex. Sin embargo, `request` podía llamarse directamente y hacía consultar/insertar sin atomicidad propia. Se cerró esa posibilidad con una transacción en el punto común. No se afirma que esta carrera haya ocurrido en la tablet reportada.

Ahora hay un único mensaje de resultado por trabajo, con revisión actual/anterior, alcance y contexto de intento anterior. Una copia READY anterior se identifica como anterior cuando cambia la ficha. Los errores siguen visibles y reintentables; el resumen permite abrir los demás intentos. Los avisos de solicitud son transitorios, y Room es la fuente del resultado durable. Los archivos verificados y sus rutas anteriores se conservan.

Solicitudes simultáneas de la misma revisión/alcance reutilizan el trabajo existente; reintentar un trabajo en cola o ejecutándose no lo reinicia. Si un intento fallido no tiene manifiesto y su revisión quedó antigua, el botón solicita la revisión actual y conserva el fallo histórico. Un manifiesto ya congelado conserva sus bytes y su recuperación idempotente. Hay comprobación de revisión tanto al terminar la generación como antes de congelar su plan; no se eliminaron guardas de revisión, digest ni publicación.

**Límite de la evidencia:** la captura del propietario no permite contar ejecuciones reales ni identificar sus revisiones. Sí se demostraron la presentación repetida, la mezcla de revisiones y la carrera de guardado/preparación mediante código y pruebas locales. No se atribuye una duplicación de archivos en esa tablet sin evidencia.

### Drive - «Preparando archivos / 0 de 0»

El drenaje remoto registra PREPARING con cantidades cero antes de negociar capacidades y preparar el paquete. Todavía no conoce cuántos archivos habrá; no equivale a una entrega ni al fallo de la copia en Descargas. La interfaz ahora dice «Cantidad de archivos aún no calculada; preparación local, sin ACK de entrega» en esa situación.

La preparación local de Descargas usa sus propios trabajos, manifiestos y directorio; no reutiliza una preparación Drive ni produce ACK remoto. El drenaje Drive mantiene sus propios archivos/preparaciones y contrato. La etiqueta «Subiendo» exige un WorkManager RUNNING para la misma identidad y revisión; un progreso persistido sin ese trabajador no demuestra actividad. El tiempo visible es desde el primer intento, no desde el comienzo de esa fase. Un 0/0 aislado no permite declarar un atasco. Una demora persistente requiere contrastar el trabajador y las fases del diagnóstico existente; no se añadió un timeout arbitrario, éxito forzado ni reintento remoto para ocultarla.

## Archivos de aplicación modificados en este encargo

Rutas relativas a `D:\Downloads\GBO_PRUEBAS_8B\app\src\main\java\com\tuempresa\inventariovial`:

| Archivo | Cambio |
|---|---|
| `pavement/PavementRigidVisualGeometry.kt` | Vector D, recorte y traslación de parches |
| `pavement/PavementDamageFields.kt` | Captura LINE del 24; cantidad SLABS intacta |
| `pavement/PavementPatchVisuals.kt` (nuevo) | Preferencia visual versionada compatible con históricos |
| `pavement/PavementRepository.kt` | Confirmación atómica de daño y modo |
| `pavement/PavementSketch.kt` | Inclusión por geometría visible en secciones PDF |
| `pavement/ui/PavementDialogs.kt` | Selector y vista previa en el formulario de parche |
| `pavement/ui/PavementWorkspace.kt` | Conexión con el guardado atómico existente |
| `localfiles/LocalCopies.kt` | Solicitudes/reintentos agrupados; guarda adicional antes del manifiesto |
| `localfiles/LocalCopyPresentation.kt` (nuevo) | Un resultado por intento con contexto de revisión |
| `localfiles/LocalFilesUi.kt` | Presentación y observación de revisiones; avisos transitorios |
| `core/SyncProgress.kt` | Explicación del total desconocido durante PREPARING |

No se cambió el exportador PDF horizontal ni el cálculo/exportador Excel. Se conservaron sus modificaciones previas. El diff de **este** encargo está en `docs/pavimentos-final-20261019/CAMBIOS_ESTA_CORRECCION.diff`; no confundirlo con todo el árbol Git, que ya tenía numerosos avances pendientes.

## Verificaciones realizadas

**49 pruebas aprobadas, 0 fallidas al cierre.** Compilación Kotlin/JVM aislada de las fuentes modificadas y pruebas contra dependencias locales en caché, sin Gradle assemble/package, firma, instalación ni HTTP. Ejecución reproducible local: `tools/verificar_pavimentos_final_sin_apk_20261019.py after` y después `extras`. Requiere esa caché de dependencias y los artefactos base existentes; falla si faltan, no fabrica una APK.

| Grupo | Pruebas | Resultado |
|---|---:|---|
| Geometría final | 4 | H/V largas, diagonales, inversión de puntos, bordes, transformación PDF, cuatro adosados, esquina, interior, persistencia Room y JSON |
| Copia local final | 4 | 12 solicitudes concurrentes/dos drenajes, edición durante preparación, éxito seguido de fallo, reintento actual, reapertura, fallo visible, ninguna entrega remota nueva |
| Regresiones LocalFiles148 | 16 | Hash, originales, repetición sin nueva escritura, interrupción/pérdida de respuesta, permisos, espacio, archivos externos alterados, destinos y transacciones |
| Juntas 26: confirmación y vista previa | 2 | Atomicidad, cancelación/deshacer y validación existentes |
| PDF/Excel focalizados anteriores | 4 | Renderizador horizontal, límites/secciones, formatos existentes |
| Ejemplos nuevos | 1 | Canvas real y renderizado horizontal de las páginas |
| Interfaz final | 2 | Selector/vista previa/confirmación y mensajes únicos después de cerrar y reabrir pantalla |
| Integridad y regresiones de esquina/juntas | 5 | PCI y contenido XLSX equivalentes, UUID, coordenadas, otras anotaciones y esquinas históricas |
| Regresiones visuales previas | 11 | Catálogo, trazos, extensiones históricas, proporciones y secciones |

Los dos lotes finalizaron `OK (31 tests)` y `OK (18 tests)`. La primera ejecución de interfaz detectó un error del fixture: la base de prueba había creado su propio trabajo automático además de los dos trabajos históricos insertados por el test. Se desactivó únicamente la automatización de ese fixture antes de insertar datos ficticios y se repitió el lote. No se relajó una condición de la aplicación. Se conserva el log inicial; también se corrigió la salida de caracteres del ejecutor local para que un fallo no quede tapado por un error de codificación de consola.

El chequeo de espacios de los archivos modificados en este encargo pasó. El chequeo del árbol completo informó un espacio final preexistente en `scap/export/ScapCanonicalExporter.kt:54`; ese archivo no cambió respecto al respaldo de entrada y se conservó. No afecta estas pruebas ni se corrigió fuera de alcance.

Evidencias: `tmp/pavimentos-final-20261019/compile-after.log`, `test-after.log`, `compile-extras.log`, `test-extras.log` y `test-extras-inicial.log`. Nuevas pruebas: `PavementFinalGeometryTest`, `LocalCopyFinalTest`, `PavementFinalExamplesTest`, `PavementFinalUiTest`, `PavementFinalIntegrityTest`.

Los ejemplos usan datos ficticios y el dibujante real Android sobre Canvas/Robolectric; **no son capturas de tablet**. El PDF de ejemplo es rasterizado por el adaptador de pruebas `SyntheticPdf146`, no por el escritor PDF nativo de un dispositivo. Se revisaron visualmente sus cuatro páginas de 842 x 595 y la lámina de doce casos. En el ejemplo PCI aparecen curvas pendientes ya existentes; la prueba garantiza que el cambio visual no modifica el resultado, no valida curvas que no existen.

## Conservación y reversión

- Respaldo previo a esta corrección: `tmp/pavimentos-final-20261019/FUENTES_ANTES.zip`, 744 fuentes/configuración pública, sin copiar propiedades privadas ni APK. `baseline.json` conserva el inventario de hashes y el estado Git anterior.
- Comparación final: las cuatro propiedades privadas inventariadas y las diez APK existentes conservan sus SHA-256. `app/build.gradle.kts`, esquema/modelos/migraciones Room, identidad de aplicación y firma no se modificaron. HEAD se conserva; no hubo commit/push.
- No se accedió a tablet, datos reales, Drive, Apps Script, GBO LAB ni Servicio de Usuarios. Los trabajos, bases y archivos utilizados por las pruebas son ficticios. Reintentar una copia no programa sincronización Drive; eso se comprobó en Room.
- Para revertir **solo este encargo**, comparar/restaurar exclusivamente los nueve archivos existentes listados en `RESULTADOS.json` desde el ZIP y apartar los dos archivos nuevos de aplicación y cinco tests nuevos. No extraer todo el ZIP sobre el proyecto ni hacer reset/clean: el árbol previo contiene trabajo que debe conservarse. Esta reversión es documental y no se ejecutó.

## Próximo paso autorizado por revisión, no ejecutado

Revisar la lámina `output/pdf/pavimentos-final-20261019/ejemplos-Canvas-real.png` y el PDF simulado. Después de la conformidad visual se podrá compilar otra candidata y probar físicamente, con registros ficticios: D horizontal/vertical y diagonales; parche en cuatro lados e interior; cerrar/reabrir; verificar PDF horizontal nativo; comprobar copia actual frente a anterior y reintento exclusivamente local. **La compilación integral Gradle, APK firmada y prueba física de estos cambios están pendientes.**

No se cambió la versión ni se empaquetó/distribuyó el release final mientras el propietario revisa estos ejemplos.
