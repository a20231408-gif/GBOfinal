# Consolidados SIC locales por ruta — 26/10/2026

Los cambios están implementados en las fuentes, sin generar APK, instalar aplicaciones, acceder al Drive privado ni publicar Apps Script. Se conserva la candidata 1.4.13 (27) del 23/10: **esa APK anterior no incluye esta intervención**. La versión y firma no se cambiaron. Los cambios siguen locales, sin commit/push.

## Resultado para el ingeniero

**EXPORTAR SIC** permite elegir SIC-17, SIC-17A o SIC-17B. Guarda y verifica el consolidado del formato y ruta seleccionados directamente en almacenamiento público. Muestra progreso, resultado, ubicación y **Abrir Excel**. La apertura resuelve el documento principal vigente, porque una actualización puede haber convertido el URI anterior en respaldo. Ya no abre un selector de archivo para estos consolidados ni sugiere un nombre con fecha/UUID.

Ejemplo comprobado con Room y XLSX reales, datos ficticios:

```
Descargas/
└── GBO Inventario Vial/
    ├── PE-28H/
    │   └── EXCEL_SIC/
    │       ├── SIC-17.xlsx
    │       ├── SIC-17A.xlsx
    │       └── SIC-17B.xlsx
    └── PE-22A/
        └── EXCEL_SIC/
            └── SIC-17A.xlsx
```

Correviento (10+100), Santa Rosa (12+300) y Tercer Puente (11+200) producen **un SIC-17A.xlsx con tres filas: Correviento, Tercer Puente, Santa Rosa**. Al editar Correviento se actualiza su fila; las otras dos permanecen. Repetir la exportación sin cambios conserva el mismo URI y no vuelve a escribir el archivo. Hay un archivo principal por ruta/formato; los respaldos de sustitución llevan nombres que comienzan con punto y pueden ser visibles si el administrador muestra archivos ocultos.

Cada libro incluye exclusivamente datos disponibles en la tablet. No descarga registros de otros ingenieros ni sustituye el consolidado compartido de Drive. Un Excel local no acredita un ACK.

Las copias provisionales y paquetes por ficha ya existentes se conservan como respaldos separados. No se migraron ni renombraron masivamente sus carpetas; no sustituyen al principal de EXCEL_SIC ni se crean al pulsar la nueva exportación del consolidado.

**EXPORTAR SCAP** sigue siendo individual, con plantilla y límites vigentes. La sugerencia pasa a `SCAP - nombre del puente.xlsx`. Su preparación privada se separa por inspección/revisión para evitar colisiones entre puentes con igual nombre. Conserva el selector de guardado explícito y el flujo existente de copias/paquetes locales; no se confunde con EXCEL_SIC.

## Causas comprobadas y límites de la evidencia

| Observación | Código y evidencia |
|---|---|
| SIC-17/SIC-17B parecen no responder | El flujo detenía la exportación cuando `missingRequired` contenía campos del formato elegido. La corrección y el acceso a completar estaban dentro del panel de exportación, después del informe de pendientes, y parte del detalle quedaba bajo opciones técnicas. Es una causa reproducida con datos incompletos; **no demuestra qué campo faltaba en la tablet del ingeniero**, cuyo estado Room no se inspeccionó. Ahora se muestra `No se pudo exportar SIC-17B. Falta: [etiquetas reales]`, con acción directa al formato, y el panel se ubica antes del informe largo. Las pruebas Compose recorren los tres formatos válidos y ambos rechazos incompletos. |
| Nombre técnico | `ScapExportPanel.export()` construía `${formato}_${fecha}_${fragmentoUUID}.xlsx` y pasaba ese nombre a `CreateDocument`. Se retiró ese mecanismo del consolidado principal. |
| Ausencia de EXCEL_SIC público | El archivo se preparaba en `filesDir/scap_exports`; `ACTION_CREATE_DOCUMENT` guardaba donde eligiera el usuario. Las copias automáticas existentes eran por ficha/revisión o archivos confirmados propios, sin el consolidado global. Ahora se publica un documento local independiente en la jerarquía real de `aggregateScope` V4. |
| Una fila / archivo por puente | El método anterior sí podía reunir varias fichas, pero la interfaz solo incluía inspecciones `COMPLETE`, registros vinculados activos/borrador y formatos sin faltantes, y luego deduplicaba inspecciones por identidad. El resultado podía tener una sola fila si solo una sobrevivía a esos filtros. El archivo recibido no contiene identidad/historial suficiente para saber cuántas otras fichas existían o estaban pendientes. **Una fila no prueba por sí sola que siempre se exportara individualmente.** Cada ejecución creaba además un documento nuevo con nombre técnico, en vez de actualizar un archivo público estable. |

La proyección nueva no exige que todo SCAP esté cerrado para exportar un SIC independiente ya válido. Valida los campos del SIC seleccionado y no utiliza datos de otro formato para completar faltantes. No altera los requisitos contractuales existentes.

## Plantilla real recibida y definición V20

Se leyó una copia del XLSX SIC-17A proporcionado por el ingeniero. El original permanece intacto y `referencias/` está excluida mediante `.git/info/exclude`; ni el archivo real ni su imagen están incorporados a esta entrega versionable. Su procedencia del consolidado remoto sigue **sin confirmar**.

El archivo tiene hoja PE-3N, 16 columnas, una fila, dos logotipos y ninguna fórmula. Coinciden con el exportador actual: encabezados y estilos, fuentes, formatos numéricos, columnas/anchos, combinaciones, alturas del encabezado, validaciones y geometría de dibujos. Los estilos, imágenes y dibujos son byte a byte iguales. No declara `pageSetup`, márgenes ni validaciones; no se inventó orientación o validación ausente.

Difieren la hoja y los nombres de impresión: el consolidador local anterior renombraba la hoja a la ruta y añadía áreas/títulos de impresión. También varían relaciones y declaraciones XML relacionadas con ese empaquetado, la ruta y los valores de datos. Es consistente con el flujo anterior, sin demostrar de forma única qué pantalla lo generó. La comparación segura está en `evidencias/COMPARACION_ESTRUCTURAL_SIN_DATOS.json`. Se revisaron visualmente ambas imágenes; la real está solo en el directorio temporal privado y la ficticia está en esta entrega.

**Apps Script V20 no obtiene una plantilla SIC maestra mediante un ID de Drive.** `validate` descomprime el XLSX de una fila recibido desde Android; `commit` reúne las identidades y comprueba firmas de estilos, columnas y encabezado. `mergeSheet` y `packWorkbook` conservan las partes del libro recibido, sustituyendo filas, dimensión y extensión de impresión. En V4, el destino es `ruta/EXCEL_SIC/SIC-xx.xlsx`, con binding de archivo persistente. Una nueva revisión reemplaza la contribución del UUID; no es un append ciego. El número estable de carpeta del elemento se asigna aparte y no se renumera al ordenar filas.

Los formatos SIC de Android se generan con `SicExcelWriter`, sus columnas y los dos logotipos de recursos; no hay un XLSX SIC-17A maestro binario que garantice por sí solo equivalencia con un libro histórico productivo. Las plantillas binarias SCAP losa/modular corresponden a las fichas SCAP, no a estos consolidados SIC.

El nuevo ensamblador conserva una sola hoja con el nombre del XLSX de origen, como V20; no la vuelve a renombrar a la ruta ni añade hojas o columnas técnicas. Tres pruebas Node compararon las partes completas de SIC-17/17A/17B contra `merge/pack` de las fuentes V20 originales **en un entorno simulado**: equivalentes. No son operaciones ni ACK reales de Google.

**Pendiente:** recibir los consolidados SIC-17, SIC-17A y SIC-17B descargados directamente del despliegue vigente, con procedencia confirmada. No se afirma equivalencia exacta con esos archivos todavía. Tampoco se implementó un endpoint de descarga.

## Identidad, acumulación, orden y recuperación

- La identidad interna de fila es el UUID del registro vial; SCAP elige la última inspección local por `updatedAt`, con desempate estable por identidad. Pavimentos conserva el UUID de cada daño. La correspondencia, revisión y hash del XLSX de una fila se guardan en manifiestos privados externos al Excel.
- Se incorporan los registros locales válidos y se actualiza su entrada por identidad. Si una edición queda incompleta, se conserva la última fila válida y se informa expresamente que el consolidado no está completamente actualizado. Los nuevos registros incompletos no reciben filas inventadas. Una exportación explícita de la ficha incompleta muestra sus faltantes y no anuncia éxito.
- Orden: PR numérica, metros y UUID para desempatar; PR inválida al final. SIC-17A/B toman la clave del registro vial SCAP vinculado sin añadir PR visibles a esas plantillas. No se ordena por calzada o fecha y no se cambia el cálculo contractual de longitud.
- Un mutex y un bloqueo de archivo serializan lectura, preparación y publicación. Los snapshots se leen con Room. Los ZIP locales se normalizan para que una reexportación idéntica no cree otra versión física.
- Se prepara una nueva versión privada, se abre/valida estructuralmente y se publica una copia temporal. Se verifica tamaño/SHA-256; después se renombra el archivo principal anterior como respaldo y se promueve el nuevo. El archivo final vuelve a verificarse antes de mostrar éxito.
- `AtomicFile` protege los manifiestos privados. El journal registra preparación, staging, respaldo y promoción; se recupera antes de leer una revisión nueva. Se conservan XLSX y manifiestos de versiones previas. Un proveedor que cambia el URI al renombrar se concilia con ruta/nombre y bytes exactos, sin sobrescribir un destino desconocido.
- **SAF/MediaStore no garantizan una sustitución atómica conjunta.** Una interrupción después del respaldo puede dejar temporalmente ausente el nombre principal; quedan respaldo público y versión privada válidos, y el siguiente intento continúa el journal. No se promete atomicidad inexistente.
- Archivos públicos modificados fuera de GBO, nombres ocupados o permisos revocados detienen la sustitución; se preservan originales/versiones privadas y se informa el fallo. No se borra ningún archivo para resolverlo.

La actualización automática reutiliza los trabajos locales existentes cuando las copias automáticas están habilitadas. Al procesar una revisión de SIC, SCAP o Pavimentos se actualizan los formatos correspondientes, sin Internet. La prueba instala los triggers reales de Room, registra un puente, procesa su solicitud, incorpora el segundo y edita el primero: el principal pasa a dos filas sin duplicados ni solicitud de subida remota. No se crean consolidados de formatos sin datos válidos. No se cambiaron WorkManager, sus políticas o la cola remota. El guardado en Room siempre precede al documento público; si la copia falla, las fichas siguen en GBO y se puede reintentar.

## Compatibilidad que requiere atención

1. **Plantillas SIC-17 históricas mixtas:** la captura SIC independiente incluye su columna OBSERVACIONES, mientras que la proyección SCAP actual tiene una definición distinta. Si ambas aparecen en la misma ruta, se detecta incompatibilidad y se conserva el consolidado anterior; no se descartan filas ni se fuerza una plantilla inventada. La prueba negativa demuestra el bloqueo. Es necesario confirmar el SIC-17 contractual real y conciliar ambas definiciones antes de aprobar ese caso para campo. V20 también rechaza firmas de plantilla incompatibles; esta intervención no lo disimula ni cambia el contrato de sincronización.
2. **Orden remoto:** la base V20 revisada sigue priorizando calzada/sentido y su medida técnica; el orden local nuevo solicitado es PR/metros. Una prueba demuestra esta diferencia. No se publicó el parche previo de ordenamiento del 23/10 ni se mezcló con esta tarea. El archivo local no se presenta como réplica global exacta.
3. **Fotos/videos SCAP:** continúan las 20 categorías, los originales privados y las barreras de capacidad existentes. El backend que no anuncia `scapPhotoPanelVersion=2` mantiene esas entregas en espera. No se cambió su contrato ni se publicó el parche de categorías.
4. No se ejecutó instalación, prueba física de cámara/Archivos/Excel, operación Google ni verificación del consolidado productivo. Las pruebas de proveedores Android son controladas en Robolectric.

## Validación y decisión

**73 casos Android distintos aprobados (21 nuevos y 52 regresiones), 0 fallos finales, 0 omitidos; 3 pruebas Node V20 aprobadas.** Resultados por clase y etapa: `evidencias/RESULTADOS.json` y XML JUnit. Las pruebas cubren Room real, Compose, XLSX reales, tres puentes, edición/repetición, rutas separadas, PR ausente, dos tablets offline independientes, concurrencia, cierre/reapertura, escritura parcial, pérdida de respuesta, permisos, nombres/propietarios y recuperación SAF con cambios de URI. No se cuentan reejecuciones como casos adicionales.

Se conservaron los fallos intermedios. Se corrigió una comprobación que interpretaba direcciones de celda implícitas válidas de Pavimentos como duplicadas, sin modificar su exportador; se usa la consulta Bundle compatible con DocumentsProvider desde API 26. En los tests se corrigieron la columna de nombre frente a código, la lectura de texto DOM, el espacio libre ficticio de StatFs y la expectativa de la etiqueta de apertura. Las ejecuciones finales pasaron. Los avisos de JDK/Conscrypt y una inferencia de tipo de un fixture no fueron bloqueantes.

La conservación por SHA-256 acredita cinco propiedades privadas, quince APK anteriores, las fuentes del backend, Gradle/versión/firma y todos los esquemas Room intactos. Solo se modificaron seis fuentes existentes y se añadieron dos fuentes y seis archivos de pruebas. El original real sigue excluido y sin cambios. El parche comparativo está en `CAMBIOS_ANDROID_20261026.patch`; las fuentes actuales ya contienen esos cambios, no se debe reaplicar sobre ellas. Su comprobación sobre copias del respaldo usa `--ignore-space-change` por finales de línea Windows.

**GO condicionado para preparar una siguiente candidata de validación física de rutas con plantillas compatibles. NO-GO para distribución general o para declarar réplica exacta de Drive:** faltan la comparación con XLSX remotos confirmados, la conciliación del SIC-17 mixto y la prueba física. Por instrucción del propietario, no se genera una nueva APK ahora.

Prueba física posterior: exportar SIC-17A de un puente ficticio, localizar `EXCEL_SIC`, añadir otros dos, editar el primero y reexportar; verificar tres filas, orden, apertura del XLSX y respaldos. Cerrar/reabrir sin Internet, probar SIC-17 y SIC-17B válidos y uno incompleto. En Android 8/9 autorizar una sola vez la carpeta local con SAF. No sincronizar pruebas reales sin autorización separada.
