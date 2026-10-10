# PARALLEL C · Pavimentos / PCI / URCI

Trabajo sobre la base recibida GBO_FIX. Sin commit ni push. La carpeta no contiene Git; se entrega un unified diff generado contra la copia inicial y un proyecto fuente compilable.

## Cambios funcionales

- Cabecera: unidad secuencial por ruta/tramo/calzada/superficie, número editable y duplicados advertidos/rechazados antes de guardar. Datos consecutivos reutilizan ruta, tramo, carril, calzada, ancho y geometría rígida del inspector. Carril Derecho/Izquierdo y calzada UC/UD/CD/A1/A2 usan dropdown; la calzada viene del catálogo existente.
- Progresivas: parser numérico central, visualización `00+000`, inicio editable encadenado al final previo y final automático según longitud. Se conserva fecha de creación del dispositivo al editar/reabrir; historial incluye días anteriores y mantiene Continuar/Ver y asociaciones de daños/fotos.
- Rígido: filas longitudinales y columnas transversales, ancho/largo por losa, total de losas y cálculo visible de longitud. Densidad por losas únicas, mayor severidad por daño/losa, exclusión por losa dividida M/H y dos grietas M → H según p.159. Sello de juntas sigue global. Losas largas requieren definir juntas imaginarias antes de calcular.
- Croquis: origen inferior izquierdo, ajuste completo inicial y botón Ajustar, regla Y contigua al borde, transform único para captura/regla/dibujo, zoom focal estable y marcas según densidad de píxeles. Selección de losa con borde rojo de 6 px, dibujado al final. Tabla compacta incorpora cantidad; severidad L/M/H con amarillo/naranja/rojo y nombres completos.
- Geometría: 46 deterioros cubiertos con recetas de línea, grieta irregular, red, parche, hueco, bandas, corrugación, depresión/hinchamiento, esquina, junta y losa. UUID determina la irregularidad métrica; extremos medidos no cambian. El HTML original se usó como referencia visual, el manual como fuente técnica.
- Huecos grandes: área medida independiente de diámetro; equivalencia área/0.47 m² asfalto y área/0.65 m² no pavimentado. No inferir área circular. Registros históricos sin área quedan pendientes. Severidad asfáltica M/H para diámetro >0.75 m según profundidad.
- PDF: segmentación automática a escala legible, recorte de figuras sin alterar coordenadas, numeración en margen/leyenda y tablas con encabezado repetido en nuevas páginas. Para 50 m de ancho habitual: 0–20, 20–40, 40–50 m. Daños que cruzan límites se recortan en los segmentos involucrados; su cantidad total no se suma varias veces. Fotos y observaciones tienen continuación independiente.
- Excel final: conserva Fallas/Resumen/Detalle y las trece columnas de campo; desaparece hoja Iteraciones. El procedimiento CDV y las iteraciones del snapshot permanecen. Se normaliza severidad visual y progresiva al exportar.
- SIC-14/14A/15/16: trece columnas verificadas con las imágenes suministradas, agrupaciones Inicio/Fin, hoja nativa con formato y estado de completitud. Reutiliza campos inequívocos; los faltantes y equivalencias ambiguas requieren captura explícita con origen `MANUAL_PENDING_EXTERNAL_SOURCE`. El complemento persiste en Room y admite actualización por el creador después del cierre sin modificar el cálculo histórico.
- Archivos: registro durable por muestra/superficie/categoría/MIME/hash/URI, copias locales de exportaciones SAF y de los archivos preparados por el uploader propio de Pavimentos. Fotos originales conservan sus entidades y rutas; copias marcadas tienen identidad de foto/daño en el manifiesto.

## Validación

Baseline ejecutado: **249 pruebas; 15 fallos existentes; 0 errores**. Baseline assembleDebug exitoso. Evidencia en `output/parallel-c/baseline-summary.json`, XML iniciales y logs de baseline.

Suite final: **280 pruebas; 15 fallos existentes; 0 fallos nuevos; 0 errores**. Hay 31 pruebas nuevas. La salida Gradle de tests es FAILED debido a esos mismos 15 casos previos, no se oculta el estado del comando. Se compara identidad de cada prueba fallida contra el baseline en `output/parallel-c/validation.json`.

Cobertura añadida: secuencia editable y por ámbito, reutilización, fecha persistente y reapertura Room, progresivas y límites inválidos, geometría rígida/longitud, densidad por losas, reglas de severidad, equivalencias de huecos, catálogos dropdown en Compose, selección roja en píxeles, focal de zoom y Ajustar con pinch, escala 5/2/1, grietas determinísticas/extremos, 46 recetas, segmentación/fronteras, paginación con 70 daños, ausencia de iteraciones visuales, permanencia del cálculo, completitud y XML nativo SIC, hashes/asociaciones del manifiesto y migración 12→13 con fotos/usuarios/snapshot intactos.

Se actualizaron únicamente fixtures/aserciones relacionadas con la nueva versión en `BridgeSicFlowTest` y con calzada válida en `TabletUxDataTest`; sus comprobaciones de datos permanecen. El test de migración de Pavimentos añade 12→13 y el test de reporte espera segmentación. No se deshabilitaron tests.

Se inspeccionaron visualmente PNG generados por el mismo renderer del PDF, incluyendo el reporte de 46 m, la tabla de continuación y el caso de 70 daños. Los números y tablas no se superponen; datos físicamente coincidentes conservan su posición. Los cuatro XLSX sintéticos se reabrieron con openpyxl: trece columnas, encabezados agrupados D1:E1/F1:G1, valores numéricos y textos preservados. Los ejemplos se identifican como sintéticos y no representan inspecciones reales.

La compilación ordinaria devolvió éxito, pero una comprobación del APK detectó salida heredada sin las clases nuevas. Por eso la entrega se reconstruye con `assembleDebug --no-configuration-cache --rerun-tasks` y se verifica presencia de las clases de esta rama en DEX. Ver `assemble-fresh.log`, `apk-content-check.json` y `apk-sha256.txt` en la carpeta de validación de la entrega.

No se ejecutaron pruebas instrumentadas en dispositivo físico. Los tests de Compose y Canvas usan Robolectric, incluidos gestos y gráficos nativos.

## Limitaciones técnicas explícitas

Las curvas VD/CDV del baseline no están digitalizadas/validadas. Se conserva su validación y la negativa a producir un índice inventado; el resultado queda pendiente cuando requiere una curva ausente. Un caso sin deterioros y con dimensiones válidas puede producir 100 sin curvas. El detalle técnico queda plegado por defecto en la captura.

SIC-14A está bloqueado para tipos 07/13: la anotación de densidad dice 07-Huecos y el catálogo de la misma imagen dice 13-Huecos. No se inventa una resolución contractual. No existe libro XLSX original de estos SIC en la base; las hojas usan la estructura conocida de las imágenes y no se afirma identidad con un libro contractual no suministrado.

Google Earth se evaluó en términos de complejidad, riesgo y beneficio. No se implementó SDK, integración Earth ni KMZ; los contratos geográficos faltantes y la propuesta de overlay estático están en `PARALLEL_C_INTEGRATION.md`.

## Archivos y entrega

Producción: módulo `pavement/` y registro de versión/migración en `data/database/InventoryDatabase.kt`. Esquema nuevo: `app/schemas/.../13.json`. El resto de producción se cotejó contra el snapshot inicial: **0 archivos protegidos modificados**.

Listado exacto con hashes: `output/parallel-c/validation.json`. Diff: `output/parallel-c/changes.diff`. Nuevos tests: `PavementParallelCTest.kt`, `PavementParallelCUiTest.kt`. Auditoría reproducible: `tools/validate_parallel_c.py`.

Documentación: este reporte, `PARALLEL_C_INTEGRATION.md`, `PAVEMENT_MANUAL_MAPPING.md`, `PAVEMENT_GENERATED_FILES.json`; referencias SIC suministradas en `docs/reference/pavement/`.

Paquete: `GBO_PARALLEL_C_PAVIMENTOS.zip`. Contiene proyecto fuente, referencias/documentación, diff y evidencia de validación; excluye cachés, archivos locales de credenciales/configuración, builds heredados y respaldos de trabajo. Incluye el APK reconstruido y comprobado para revisión.
