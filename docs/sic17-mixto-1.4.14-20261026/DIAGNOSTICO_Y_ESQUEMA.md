# SIC-17 mixto: diagnóstico y esquema local común

## Causa raíz

`ScapSicExporter.write()` construye una fila con `BridgeSicContract.keys17` y `SicExportFormat.SIC17.columns`: 20 columnas A:T. Emplea `writeMappedRow()`, con título en A11, ruta en A12 y encabezados en la fila 13.

El exportador independiente usa esas mismas 20 columnas contractuales y añade OBSERVACIONES en U, a partir de `InventoryRecordEntity.observations`. Utiliza el libro primario de `SicExcelWriter`: título completo en A10, bloques de ubicación en la fila 12, encabezados en 13, filtro, impresión y congelación propios del formato primario.

Por tanto, eliminar U no resuelve todas las diferencias. La firma que comprueba el consolidador local incluye estilos, definiciones de columnas y la fila de encabezados. Rechazaba correctamente las dos representaciones distintas. La prueba previa del resultado deseado falló con `Plantillas de SIC-17 incompatibles`; su XML se conserva como evidencia anterior al arreglo.

La comparación de XLSX ficticios reales confirma que `xl/styles.xml` y las definiciones de columnas A:T son iguales entre ambos orígenes. La diferencia directa de firma está en la fila 13: SCAP usa altura 70 y 20 encabezados; el independiente, altura 60 y el encabezado adicional U. Además difieren los bloques superiores, filtro, dimensión y configuración de impresión. Detalle medido: `evidencias/COMPARACION_PLANTILLAS.json`.

Las fuentes V20 no aportan un XLSX SIC-17 maestro independiente: `SyncV3Core.validate()` abre el libro recibido; `commit()` compara firmas y `mergeSheet()` combina sus filas. `SicObservationsV1Core.inspect()` reconoce OBSERVACIONES únicamente como complemento final de la fila 13 y excluye esa columna de la firma contractual; `plain()` elimina formato propio de esa columna y la excluye del filtro. Eso no reconcilia por sí solo los otros encabezados y rangos. No se modificó ninguna fuente V20.

## Decisión de plantilla

El consolidado **local** utiliza la plantilla primaria SIC-17 ya existente en Android. Conserva A:T, sus estilos, títulos, logotipos, rangos y reglas de impresión. OBSERVACIONES permanece en U como complemento aprobado anteriormente, sin relleno, bordes, ancho personalizado ni filtro propio; se escribe como texto literal, también cuando comienza con `=`. No se inventa una ubicación dentro de las 20 columnas contractuales ni se desplazan sus campos.

No se afirma que este libro sea idéntico a un consolidado remoto de procedencia verificada: todavía falta ese archivo SIC-17 real.

| Columna | Campo semántico | Contenido |
|---|---|---|
| A | routeCode | Carretera |
| B | roadbedCode | Calzada |
| C | startPrCode | Código PR inicial, texto de cuatro dígitos |
| D | startDistanceM | Metros desde PR inicial |
| E | endPrCode | Código PR final, texto de cuatro dígitos |
| F | endDistanceM | Metros desde PR final |
| G | classCode | Clase, código de dos dígitos |
| H | typeCode | Tipo |
| I | bridgeCode | Código del puente |
| J | inventoriedCode | Inventariado |
| K | spans | Ojos / vanos |
| L | dimension1LengthM | Longitud |
| M | dimension2LowerHeightM | Altura inferior |
| N | structuralConditionCode | Condición estructural |
| O | functionalConditionCode | Condición funcional |
| P | surveyDate | Fecha, valor numérico Excel con estilo de fecha |
| Q | serviceTypeCode | Tipo de servicio |
| R | singularityCode | Singularidad salvada |
| S | singularityName | Nombre de singularidad |
| T | dimension3UpperHeightM | Altura superior |
| U | observations | Complemento literal sin formato |

Los dos encabezados “Código PR” y “Distancia (m)” se identifican por claves semánticas distintas para inicio y final. El adaptador valida la lista completa de etiquetas, tipos de celda, referencias y plantilla reconocida antes de transformar; no acepta un archivo solo por tener 20 o 21 columnas. No convierte PR en longitud acumulada ni cambia cálculos.

## Adaptación y conservación

`LocalSic17Workbook` admite las dos plantillas exactas generadas por los exportadores actuales. Compara todas las partes del XLSX, salvo el contenido variable de su única fila de datos, con ejemplares de esos escritores. No acepta encabezados, estilos, rangos, imágenes, relaciones ni columnas desconocidos. Lee valores por el esquema explícito, valida tipos y rechaza fórmulas de datos. Genera una fila de la plantilla primaria mediante el mismo escritor existente.

Para SCAP, U toma las observaciones del registro vial vinculado. Las observaciones de defectos F.2, conclusiones G.2 y comentarios específicos de SIC-17B no se concatenan en U: son campos de significado distinto y siguen en Room y en sus exportaciones SCAP/SIC-17B. No se añade redacción generada ni se altera el texto del operador.

Las filas históricas retenidas en la caché local se verifican por su SHA-256 original y se adaptan en memoria cuando corresponden a una de esas dos plantillas reconocidas. Se conservan sus archivos originales, UUID, revisión y procedencia en los manifiestos existentes. Una variante anterior desconocida se rechaza explícitamente, sin adivinar el mapeo ni reemplazar el Excel válido.

Se mantienen mutex, bloqueo de archivo, manifiestos, journal, comprobaciones de contenido y publicación con respaldo. No se modifican esquemas Room, fichas, UUID, ACK, WorkManager ni fotografías. El reemplazo público conserva sus límites conocidos: una interrupción entre respaldo y promoción puede dejar temporalmente ausente el nombre principal; el respaldo, la copia privada y el journal permiten completar la operación en el siguiente intento.

La plantilla primaria de una fila traía filtro `A13:T14`. Al consolidarla, el ensamblador anterior no extendía ese rango. Para SIC-17 local se corrige su extremo de fila: tres registros producen `A13:T16`; U sigue fuera del filtro. Las demás partes y los formatos SIC-17A/B no reciben este ajuste específico. El caso se detectó en una ejecución intermedia y se conserva su evidencia.

## Alcance remoto

Esta transformación se aplica exclusivamente a `LocalSicConsolidates`. Los XLSX que integran snapshots remotos siguen generándose con los exportadores existentes; no se alteran preparaciones persistidas ni hashes/ACK ya existentes. Por ello, **este hotfix no acredita ni resuelve automáticamente la mezcla histórica en el consolidado productivo V20**. Cualquier conciliación remota requiere comparar los XLSX y el despliegue reales y ensayarla por separado, sin tocar producción en esta tarea.

SIC-17A y SIC-17B mantienen sus proyecciones. Las 20 categorías SCAP mantienen sus barreras de capacidad: un backend sin `scapPhotoPanelVersion >= 2` no recibe esas evidencias incompatibles ni se anuncia su entrega como confirmada.
