# Módulo Fallas en pavimentos — alcance TDR y Drive

## Integración

El módulo de pavimentos está integrado dentro de la misma APK de Inventario Vial y se abre desde el dashboard mediante **Fallas en pavimentos**. No es un APK independiente: comparte sesión de usuario, base Room, cámara, marca de agua GBO y configuración Drive con el inventario vial.

Referencia visual conservada en `docs/reference/GBO_Fallas_Pavimentos.html` y formato de trabajo en `docs/Formato_para_programa_1.xlsx`.

## Captura implementada

- Superficie asfáltica: PCI, daños 1–20.
- Pavimento rígido: PCI, daños 21–39 y selección de losas.
- No pavimentado: URCI, daños 81–87.
- Cabecera con ruta del proyecto, tramo asociado, muestra, carril, calzada, PR inicial/final, dimensiones y fecha.
- Esquema X/Y de la unidad de inspección; línea/área por dos puntos, hueco por centro y diámetro, selección de losas y daños globales.
- Severidad, medidas complementarias, observación, fotos por daño y movimiento/corrección sobre el esquema.
- Cierre de muestra, PDF de hoja de campo y Excel con la hoja `Fallas` de 13 columnas más Resumen/Detalle/Iteraciones.
- Marca de agua corporativa obligatoria aplicada sobre una copia de entrega; el original local no se modifica.

## Drive

Una muestra cerrada puede enviarse desde **Subir muestra a Drive**. El endpoint `uploadPavementFile` de `google-apps-script/Code.gs` crea:

```text
RUTA/
└── EVALUACION_SUPERFICIAL/
    └── TRAMO N/
        └── ASFALTICO | RIGIDO | NO_PAVIMENTADO/
            ├── 07_HOJAS_CAMPO/
            │   └── MUESTRA N/
            │       └── HC_NNN.pdf
            ├── 08_PANEL_FOTOGRAFICO/
            │   └── MUESTRA N/
            │       ├── 001_D013.jpg
            │       └── ...
            └── DATA_APP/
                ├── PAV_ASF|RIG|NPV.xlsx
                └── MUESTRA N/
                    └── M_NNN.json
```

Los nombres de los archivos generados para entrega se mantienen en 20 caracteres o menos. Cada archivo se verifica por SHA-256. Un reintento con el mismo contenido es idempotente. Si un archivo lógico cambia en una revisión posterior, la versión anterior se mueve a `REVISIONES` en vez de sobrescribirse silenciosamente.

## Relación con el TDR

La captura implementada sirve como base de datos nativa del aplicativo/hoja de campo para inspección visual. La entrega Drive del módulo incluye documentación de campo, fotos JPG y data nativa/consolidada de la app.

El TDR también pide como entregables de Evaluación Superficial, según corresponda: Data EVALPAV/URCI (importación, exportación y gráfica), archivo de coordenadas, análisis estadístico, sectores homogéneos, desarrollo PCI/URCI, VIZIR, hojas de campo, panel fotográfico y formatos SIC de pavimentos.

### No se debe declarar todavía como generado automáticamente

Esta versión **no inventa ni sustituye** los siguientes formatos contractuales, porque no se suministraron sus plantillas/interfaz definitiva:

- archivos nativos de importación/exportación propios de EVALPAV/EVALURCI;
- archivo contractual de coordenadas;
- análisis estadístico contractual;
- sectorización homogénea;
- VIZIR;
- SIC-14, SIC-14a, SIC-15, SIC-15a, SIC-16 y SIC-16a oficiales.

El Excel `PAV_*.xlsx` es un consolidado GBO de captura, no debe presentarse como archivo oficial de EVALPAV/EVALURCI ni como SIC oficial.

## Curvas PCI/URCI

Los catálogos de daño y reglas de captura están presentes. Las curvas de valor deducible del módulo permanecen marcadas `PENDIENTE_DIGITALIZAR` donde no existe una tabla/curva digital validada. En esos casos la aplicación conserva los daños y densidades y muestra el índice como no calculable; no fabrica valores deducibles.

Para cerrar el cálculo numérico completo se debe incorporar una digitalización verificada de las curvas contra la fuente técnica y validarla con casos de referencia antes de uso contractual.
