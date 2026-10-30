# Comparación de los nueve archivos recibidos

Fuente de verdad: `D:\Downloads\archivos apps script`. Los originales no se editaron. Se verificó también la integridad del ZIP anterior y sus cuatro fuentes mediante SHA-256; detalle en `evidencias/PAQUETE_ANTERIOR_VERIFICADO.json`.

| Archivo recibido | Paridad con referencia V20 | Integración propuesta |
|---|---|---|
| Código.gs | Mismo código tras omitir solo raíz privada y normalizar CRLF/LF | Añadir UNA propiedad al objeto de capacidades de doGet |
| PhotoNameV2Core.gs | Idéntico byte a byte | Aceptar SCAP20_01…SCAP20_20 para SCAP/PUENTE y reservar nombres oficiales |
| SyncV3Core.gs | Idéntico byte a byte | Resolver categoría/carpeta oficial y exigir nombres V2; no cambiar commit/canonical/merge/ACK |
| VideoNamesV2Core.gs | Idéntico byte a byte | Misma categoría, carpeta y nomenclatura para videos; conservar bindings antiguos |
| SyncV3.gs | Idéntico byte a byte | Sin cambios; conservar ScriptLock, staging, hashes y adaptador transaccional |
| VideoV1.gs | Idéntico byte a byte | Sin cambios; protocolo existente |
| VideoV1Core.gs | Idéntico byte a byte | Sin cambios; ACK/reanudación existentes |
| SicObservationsV1Core.gs | Idéntico byte a byte | Sin cambios; Observaciones 10K |
| appsscript.json | V8, America/Lima, Drive avanzado v3 | Sin cambios en producción; no ampliar permisos |

Se añade `ScapPhotoPanelV2.gs`: catálogo único de veinte posiciones, nombres cortos y carpetas numeradas. No realiza escrituras ni migraciones.

**Atención al nombre real:** el archivo existente se llama `PhotoNameV2Core.gs` (singular). Su objeto global se llama `PhotoNamesV2`. Actualizar ese mismo archivo; NO crear además `PhotoNamesV2Core.gs`, porque duplicaría el módulo. Los otros nombres se mantienen como fueron recibidos.

El diff `CAMBIOS_MINIMOS_SOBRE_FUENTES_ACTUALES.patch` permite revisar únicamente los cambios. Los finales de línea explican algunos finales de hunk juntos; no representan funciones adicionales. El archivo de diferencias V20/actual está vacío tras ocultar raíz y normalizar líneas: no hay diferencias funcionales detectadas en el export entregado. Esta conclusión corresponde a las copias proporcionadas, no a una lectura nueva del editor productivo.

## Garantías conservadas

- `commit`, `canonical`, `validate`, `mergeSheet`, `compareTechnical` y `allocateNumber` son idénticas por función frente al core recibido. No se integraron las optimizaciones generales propuestas anteriormente.
- ScriptLock, validación SHA-256/JPEG, staging, transacciones, revisiones, BUSY, identidad y ACK permanecen con su contrato actual.
- Excel SIC, consolidación por ruta, Observaciones y ordenamiento técnico no cambian.
- No se renumeran carpetas ni se renombran fotografías históricas. Un binding antiguo sigue siendo inmutable; las categorías ambiguas no se reasignan silenciosamente.
- Videos siguen en SCAP_ONLY. La prueba local comprueba las veinte reservas/nombres y los bindings antiguos; la recepción/reproducción de video en Google no se ha ensayado en esta entrega.
- `scapPhotoPanelVersion=2` solo se anuncia cuando catálogo y tres cores declaran versión 2, Drive avanzado está presente, V3 está activo y los nombres fotográficos V2 están habilitados. Si falta una dependencia comprobada, devuelve 0. No se activa ninguna propiedad productiva.

Ejemplos verificables del fixture: `01_PUE1_PAN_DER_001.jpg`, `03_PUE1_PERFIL_007.jpg`, `20_PUE1_CAUCE_ABAJO_054.jpg`. Un video de categoría 18 utiliza el patrón `18_PUE1_TABLERO_INF_001.mp4`. El contador fotográfico reservado existente se conserva; el prefijo 01–20 identifica el orden oficial.

Los SHA-256 originales están en `evidencias/COMPARACION_9_ARCHIVOS.json`; los archivos finales se verifican con `SHA256SUMS.txt`. La copia íntegra privada de los originales se conservó bajo `tmp/scap-integracion-actual-20261027/RESPALDO_FUENTES_RECIBIDAS_PRIVADO.zip`, fuera del ZIP entregable. Incluye configuración privada: mantenerla local. No se obtuvo ni exportó una copia de ScriptProperties del servidor.
