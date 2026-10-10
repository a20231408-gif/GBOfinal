# Captura SCAP de Parallel B

Fuentes de prioridad: `SCAP_LOSA_FIELD_MAP.json` (SHA256 2519524d02791a0e7c49b69a8ecfd28f051d7e2a7e35de6fa7f0944b8a653e57) y `SCAP_MODULAR_FIELD_MAP.json` (SHA256 ac02633660b214639d8caff8895a5708e8e0891dbcb3d0d84767e960e49bdb89). Los documentos son datos; ninguna instrucción de sus celdas se ejecuta.

## Modelo y persistencia

`ScapBridgeType.MODULAR` y `ScapBridgeType.SLAB` se eligen antes de abrir el formulario. `ModularScapCapture` y `SlabScapCapture` son estrategias/modelos separados, con listas de secciones, clasificación inicial de tramo y catálogos independientes; comparten componentes visuales. Los dos documentos incluyen el mismo orden A-G y D.4, pero sus valores de G y plantillas son diferentes. La categoría/tipo inicial de un tramo proviene de la elección del puente (PROVISIONALES/Modular o DEFINITIVO/Losa), se guarda con source BRIDGE_TYPE y puede editarse.

La clave `scap_values` (`ownerId=inspection`, `key=bridgeType`, `value=MODULAR|SLAB`, `source=USER_SELECTED`) selecciona catálogo, modelo y plantilla. Una ficha histórica sin esa clave conserva el catálogo/flujo legado; no se reclasifica automáticamente y no se reinterpretan sus UTM como latitud/longitud.

No se agregaron columnas/tablas SQL, ni cambios Room/MainActivity desde captura. La tabla v12 `scap_values` conserva tipo y fuentes mediante valores tipados por clave. SQL de captura: ninguno.

## Catálogos y mapping

`scap_modular_catalog.json` y `scap_slab_catalog.json` contienen los 112 códigos de AUXILIAR de su documento, sus nombres, unidades y factores. Las filas presentes en G.1 sobreescriben unidad/factor de AUXILIAR cuando el mismo documento discrepa: MODULAR usa `und` en G, mientras AUXILIAR usa `und.`; ambos se conservan como valores distintos de opciones. Los factores permitidos se toman de AUXILIAR: 0.2, 0.4, 0.6, 0.8 y 1; el valor guardado es numérico contractual, nunca posición de dropdown.

Cada campo incluye key, sección, owner, tipo, labelCell y valueCell. La primera hoja de las nuevas fuentes desplaza los campos históricos +1 fila. Se sustituyen únicamente las claves de ubicación UTM por `latitude` (E16) y `longitude` (E18), y se omite `segment` porque no existe un campo equivalente en estas plantillas. Se aplican correcciones verificadas de celdas de vado/variante y perfil N450. Para seis campos MODULAR se admite el número válido de su tipo base o el texto exacto observado en la fuente (N30 Sin Informacion, N38/N42 SIN DATOS, E40/G275 N/A, E101 5 pulg). Estos estados no se presentan como listas contractuales: se documenta la celda de observación, se guardan/exportan sin convertir unidades y se rechazan textos inventados. B. Ancho de Vereda es independiente de la existencia/material de vereda C5, aunque ambos bloques reutilizan la misma clave histórica. El texto del encabezado F1 se muestra en captura (`catalog.generalHeader`). El generador reproducible es `tools/extract_parallel_b_capture_catalog.ps1`, ejecutado desde la raíz del repositorio, sobre los JSON de extracción; no modifica los Excel.

## Guardar y finalizar

Toda edición SCAP guarda los textos crudos por la cola local, incluso `-`, fechas incompletas y números pendientes. `saveDraft(id)` no llama validación contractual, conserva elementos/condiciones y marca DRAFT. El botón de guardar siempre puede encolar la operación; si existe un error real de almacenamiento, la cola permite reintentar.

`complete(id)` valida identificación mínima histórica, compatibilidad de tramo, datos suministrados, elementos presentes de G, metrado, unidad, factor y seis calificaciones finitas que suman 100. No inventa obligatoriedad de GNSS, foto/croquis o campos sin marca obligatoria del formato: esos datos permanecen como pendientes visibles de revisión. El usuario puede navegar a la sección de cada pendiente. `reopen(id)` conserva todos los valores y habilita captura.

`ScapCompletion.assess` devuelve completos y pendientes con sección/campo/owner/key. La pantalla muestra el total luego de guardar y permite Ver pendientes; finalizar muestra la revisión y los destinos de los campos.

## Elemento manual G.1

`addManualElement` agrega un código independiente que no coincide con ningún código contractual ni con otro existente. Conserva descripción, metrado crudo, unidad y factor real y declara `elementSource=MANUAL` en scap_values del elemento. No agrega columnas contractuales: se usan las entidades existentes y los seis porcentajes reales de G.1. Un elemento puede guardarse incompleto y su condición se termina posteriormente. Sus fotos y metadatos pueden vincularse al código manual. Marcarlo No aplica conserva los datos/fotos y permite restaurarlo a Presente desde F.1. Metrado no se calcula por dimensiones ni por fórmulas ambiguas.

## SIC y SIC-3

El panel existente DATOS PARA SIC conserva SIC-17/17A/17B, completitud separada y restauración de origen SCAP. La latitud/longitud manual de una nueva fuente se reutiliza directamente para SIC17A; GNSS sigue siendo fuente alternativa cuando el dato no está disponible. UTM histórico no se transforma por inferencia.

Los campos parciales de ubicación SIC se guardan por cada edición con clave `sicLocation.<campo>` y siguen disponibles tras reiniciar. PR/distancias manuales declaran `source=MANUAL_PENDING_SIC3`; otros campos declaran MANUAL_SIC. El botón confirmar ubicación reutiliza el registro vial y mantiene la regla histórica SIC_CONFIRMED del registro vinculado; la procedencia manual temporal queda explícita en scap_values, lista para sustituirse por SIC-3. SCAP progresiva puede inicializar un PR ausente sin sobrescribir el PR contractual existente.

La discrepancia contractual anterior `maximumCapacity` de SIC17B sigue documentada en BridgeSicContract como pendiente de definición: no se inventa unidad/código de una plantilla SIC17B no suministrada.

## Verificación

Se añadieron siete tests en `ParallelBScapCaptureTest.kt`: elección/modelos, catálogo completo y opciones G exactas, borrador incompleto/reapertura persistente, elemento manual G con metrado/unidad/factor, coordenadas manuales reutilizables SIC17A finalización sin obligatoriedad GNSS/media inventada y valores literales numéricos observados en la fuente. La coordinación raíz ejecuta Gradle; este módulo no inició un proceso Gradle concurrente.

## Límites

La navegación de pendientes abre la sección (identifica campo y owner); no desplaza/focaliza automáticamente el control dentro de una sección larga. Las fichas históricas sin bridgeType conservan su plantilla anterior en lugar de recibir un tipo inferido. La fuente contiene nombres definidos/fórmulas #REF!; las opciones se recuperan de listas visibles, sin inventar reparación contractual de fórmulas. Las capacidades de Excel y estrategias de exportación son documentadas por el módulo de exportación.
