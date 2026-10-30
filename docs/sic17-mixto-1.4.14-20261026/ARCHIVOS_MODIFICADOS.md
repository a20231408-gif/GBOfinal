# Archivos de esta intervención

Todos los cambios previos del proyecto se conservaron. Esta lista describe exclusivamente el hotfix mixto y la numeración autorizada; el estado Git contiene además avances anteriores sin confirmar.

| Archivo | Cambio |
|---|---|
| `app/src/main/java/com/tuempresa/inventariovial/export/LocalSic17Workbook.kt` | Nuevo adaptador local de las dos plantillas conocidas al esquema primario SIC-17, por claves semánticas y con validación estricta de partes, encabezados y tipos. |
| `app/src/main/java/com/tuempresa/inventariovial/export/SicExcelWriter.kt` | Reutiliza el escritor primario para filas locales normalizadas. Mantiene columnas, estilos y salida de los formatos existentes. |
| `app/src/main/java/com/tuempresa/inventariovial/export/LocalSicConsolidates.kt` | Adapta SIC-17 SCAP/independiente y filas históricas retenidas antes de consolidar. Conserva UUID, manifiestos, archivos originales y publicación recuperable. |
| `app/src/main/java/com/tuempresa/inventariovial/export/SicRouteWorkbook.kt` | Amplía únicamente el extremo de fila del filtro local SIC-17, sin incluir OBSERVACIONES ni retirar comprobaciones de incompatibilidad. |
| `app/src/test/java/com/tuempresa/inventariovial/SicConsolidationDurabilityTest.kt` | Sustituye la expectativa de rechazo del caso mixto por la consolidación correcta, conservando la comprobación del respaldo. |
| `app/src/test/java/com/tuempresa/inventariovial/MixedSic17HotfixTest.kt` | Seis casos: mezcla/ediciones/rutas, adaptación histórica y equivalencia primaria, fila retenida, rechazos peligrosos, fallos de publicación y tratamiento de fórmulas. |
| `app/build.gradle.kts` | Solo numeración: 1.4.13 (27) → 1.4.14 (28). |

Diagnóstico, evidencias y resultado de entrega: esta carpeta. Ejecutores y respaldo reversible del estado anterior: `tmp/sic17-mixto-1.4.14-20261026/`, incluido `FUENTES_ANTES.zip` y la APK anterior del directorio de compilación. Las APK históricas en `output` se conservan.

No se modificaron el exportador remoto `ScapSicExporter`, los módulos Apps Script, contratos ACK, configuración privada, firma, Room, WorkManager ni el panel fotográfico.
