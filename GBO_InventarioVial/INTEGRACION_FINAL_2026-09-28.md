# Integración final de campo — GBO Inventario Vial

Base: `GBO_InventarioVial_PROMPT_3.zip`.

La versión antigua de la rama del compañero **no se copió completa**. Se portó únicamente el arreglo de robustez de SCAP→Drive (JPG, tamaño acotado, tiempos de espera largos, ACK/hash/idempotencia) sobre la arquitectura moderna de Rondas 1–3, que conserva rutas oficiales, orden geométrico, Item/Tramo, metadata, KML y entrega humana `RUTA/SIB-08/PUENTE N`.

## Correcciones SCAP incorporadas

- C.4: orden visual contractual `Estribo izquierdo → Estribo derecho → Pilares → Macizo/cámara izquierda → Macizo/cámara derecha`.
- `Dept Político / Dept Vial` pasa a `Dpto. Político / Dpto. Vial` en UI y Excel exportado.
- D.4 **no se elimina**: la plantilla lo contiene oculto. En el Excel exportado se hacen visibles exclusivamente las filas 442–478 del bloque `D.4 : PERFIL LONGITUDINAL DEL TERRENO`.
- Navegación contractual:
  - F.1 Elementos conformantes.
  - F.2 Condición global (observaciones/fallas y evidencia).
  - Panel fotográfico independiente.
  - G.1 Condición estadística (metrado, factor, porcentajes 0–5 y resultado).
  - G.2 Observaciones y recomendaciones (normativas, preventivas, ejecutivas).
  - `REVISIÓN Y EXPORTACIÓN` queda fuera de A–G.
- G.2 se persiste con `ScapFieldValue` (sin nueva migración Room) y se exporta a la hoja `CONCLUSIONES`; se limpian los textos de ejemplo Agua Blanca.

## SCAP → Drive: integración de la corrección del compañero

- Fotografías SCAP del paquete remoto: JPEG, calidad 85, lado mayor 1600 px.
- Croquis: PNG, lado mayor 1200 px.
- Bundle SCAP: versión 4 para evitar reutilizar cachés PNG antiguos.
- Protocolo 2 conserva `RUTA/SIB-08/PUENTE N`, `P-N`, UUID, bridgeCode, revisiones, hashes e idempotencia.
- Reintentos usan bytes deterministas por revisión + layout revisado.
- Timeouts específicos de SCAP: read/write 360 s, call 420 s.
- Apps Script valida hash del ZIP y de cada archivo antes de escribir.
- Los archivos de Drive reciben marcador SHA-256 en descripción; un retry valida y reutiliza el mismo archivo en vez de duplicarlo.
- Publicación humana mantiene `REVISIONES` y la vista actual en `PUENTE N`.
- Fotos visibles en entrega humana usan `.jpg`; croquis siguen `.png`.

## Validación realizada en este entorno

- `google-apps-script/Code.test.cjs`: **27/27 PASS**.
- Se probaron en simulación: bloqueo, retry, integridad, 3 puentes ordenados 10/30/20 km, rutas `PUENTE 1/3/2`, revisiones, recuperación de publicación interrumpida, no duplicación y coexistencia con SIC/delivery.
- JSON de assets: válido.
- Revisión sintáctica de los Kotlin modificados: sin errores sintácticos detectados.

### No ejecutado aquí

No existe Android SDK utilizable en este contenedor, por lo que **debe compilarse el proyecto en Android Studio/Windows antes de distribuir el APK**. No se afirma que la subida a Google Drive real haya sido validada: las pruebas de Apps Script usan un Drive simulado.

## Orden obligatorio de despliegue

1. En el proyecto de Google Apps Script real, sustituir `Code.gs` por esta versión.
2. Conservar/configurar el `ROOT_FOLDER_ID` real y el `API_TOKEN` real; no subirlos a Git.
3. `Implementar → Gestionar implementaciones → Editar implementación actual → Nueva versión → Implementar` para conservar la misma URL `/exec`.
4. Verificar por GET que el Web App muestre `v6 · SCAP JPG + entrega por ruta`, `scapProtocol: 2`, `scapBundleVersion: 4`.
5. En el clon Android local, conservar/copiar `drive.local.properties` con el token y URL `/exec` reales.
6. Ejecutar:

```powershell
.\gradlew.bat testDebugUnitTest assembleDebug compileDebugAndroidTestKotlin --console=plain
```

7. Instalar el APK **encima de la versión previa**, sin desinstalar, para validar migraciones y persistencia.

## Prueba real mínima antes de campo

### A. Regresión básica
- Login/activación.
- Historial anterior.
- Fotos anteriores.
- Crear/editar un SIC.
- Crear/reabrir SCAP.

### B. Ficha SCAP
- C4: Estribos → Pilares → Macizos.
- A: `Dpto.` visible.
- D: D1/D2/D3/D4 en UI.
- Exportar SCAP y abrir en Excel: D4 debe estar visible.
- F.1, F.2, Panel fotográfico, G.1, G.2 y REVISIÓN separados.
- Escribir textos distintos en los 6 campos de G.2 y comprobarlos en `CONCLUSIONES`.

### C. Drive (crítico)
Crear **tres puentes de prueba**, no solo uno, y sincronizarlos. Confirmar:

```text
PE-XX/
└── SIB-08/
    ├── PUENTE 1/
    ├── PUENTE 2/
    └── PUENTE 3/
```

Dentro de cada puente deben existir la vista actual (`SCAP.xlsx`, manifiesto/ZIP, fotos/croquis según corresponda) y `REVISIONES`. Repetir la sincronización de uno de ellos y comprobar que no aparece `(1)` ni un duplicado.

### D. Reabrir Excel descargado desde Drive
- Debe abrir sin reparación.
- Fotografías visibles.
- D4 visible.
- G.1 correcto.
- G.2 con los textos capturados.

## Archivos modificados respecto a PROMPT_3

- `app/src/main/assets/scap_fields.json`
- `app/src/main/java/com/tuempresa/inventariovial/scap/data/ScapRepository.kt`
- `app/src/main/java/com/tuempresa/inventariovial/scap/domain/ScapPresentation.kt`
- `app/src/main/java/com/tuempresa/inventariovial/scap/ui/ScapForms.kt`
- `app/src/main/java/com/tuempresa/inventariovial/scap/ui/ScapElements.kt`
- `app/src/main/java/com/tuempresa/inventariovial/scap/ui/ScapWorkspace.kt`
- `app/src/main/java/com/tuempresa/inventariovial/scap/export/TemplateWorkbook.kt`
- `app/src/main/java/com/tuempresa/inventariovial/scap/export/ScapExcelExporter.kt`
- `app/src/main/java/com/tuempresa/inventariovial/scap/export/ScapDelivery.kt`
- `app/src/main/java/com/tuempresa/inventariovial/scap/export/ScapHumanDelivery.kt`
- `app/src/main/java/com/tuempresa/inventariovial/scap/export/ScapDriveSync.kt`
- `app/src/test/java/com/tuempresa/inventariovial/ScapDriveTest.kt`
- `app/src/test/java/com/tuempresa/inventariovial/ScapHumanDeliveryTest.kt`
- `app/src/test/java/com/tuempresa/inventariovial/ScapExportTest.kt`
- `google-apps-script/Code.gs`
- `google-apps-script/Code.test.cjs`
