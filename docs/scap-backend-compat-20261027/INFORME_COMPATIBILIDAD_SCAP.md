# SCAP: compatibilidad del servidor — 27/10/2026

**Causa confirmada del bloqueo de capacidad:** el backend configurado en la candidata 1.4.15 (29) respondió **HTTP 200**, con fotos V2 y videos SCAP habilitados, pero **no incluye `scapPhotoPanelVersion`**. Android exige un valor al menos 2 para las categorías nuevas. La consulta fue GET sin token ni escritura; se verificó el destino contra la configuración de la APK por hash, sin revelar su URL. [Evidencia segura](evidencias/CAPACIDADES_SOLO_LECTURA.json).

| Capacidad exigida | Respuesta real |
|---|---|
| `ok=true`, `syncProtocol=3`, `pathVersion=4` | Cumple |
| `stableFamilyNumbering=true`, `photoCategoryVersion=1` | Cumple |
| `readablePhotoNamingVersion=2` | Cumple |
| **`scapPhotoPanelVersion>=2`** | **Ausente: bloquea las categorías nuevas** |
| `technicalDeliveryWithoutGps=true`, cuando corresponde | Cumple |
| Videos: protocolo 1, `SCAP_ONLY`, nombres V2 | Cumple; también necesitan el panel 2 para categorías nuevas |

El `scapBundleVersion=6` publicado es un anuncio del flujo heredado; no es la condición que falla en este envío V3. No debe cambiarse para eludir el bloqueo.

## Parche mínimo propuesto

Las fuentes V20 aportadas no admiten `SCAP20_01`…`SCAP20_20`: rechazarían reservas o categorías aunque se anunciase la capacidad. **No basta añadir el número 2.** Se reutilizó el parche existente, sin duplicarlo ni mezclar optimizaciones:

- Añadir `ScapPhotoPanelV2.gs`: catálogo oficial, carpetas numeradas y nombres seguros.
- Integrar `PhotoNamesV2Core.gs`, `SyncV3Core.gs` y `VideoNamesV2Core.gs`: ampliación exclusiva SCAP, nombres coherentes y reservas históricas inmutables.
- Añadir solo el fragmento adjunto al objeto de capacidades del `doGet` actual. Anuncia 2 si los módulos y habilitaciones existentes son compatibles. **No es una ScriptProperty nueva.**

Se reforzó el rechazo temprano de categorías oficiales fuera de SCAP/PUENTE y el anuncio exige Drive avanzado presente. No se entrega ni reemplaza `Codigo.gs` completo. Se mantienen ScriptLock, transacciones, consolidación/Observaciones, hashes canónicos, UUID, revisión, números de carpeta, ACK y protocolos. [Archivos y procedimiento](DESPLIEGUE_CONTROLADO_Y_REVERSION.md).

## Pruebas y fotografías adicionales

**18 pruebas de backend y 5 comprobaciones JVM Android: PASS**, sin fallos finales. El payload generado por el Android actual se ensayó con el adaptador V20 real sobre Drive/Lock simulados: **54 JPEG, 3 croquis, SCAP.xlsx y manifest.json = 59 archivos**; además, tres filas SIC-17/17A/17B y sus consolidados. Se comprobaron SHA-256, metadatos, categorías, reservas, identidad, revisión, ACK, repetición sin duplicados, BUSY, edición con rollback y conservación de nombres/IDs históricos. Android acepta el ACK candidato y rechaza archivos ausentes, hashes o revisión incorrectos.

**F.2 conserva 32 posiciones.** Las 54 fotos originales y sus metadatos llegan en el payload y se preservaron en la simulación. El HTML de continuación fotográfica existe en el ZIP **local**; Android no lo envía hoy a Drive. Este parche no inventa ese archivo remoto ni lo añade falsamente al ACK. Para desbloquear la entrega de todas las fotos no hace falta cambiar esa estructura.

## Conexión y necesidad de APK

Con JSON válido sin la capacidad, Android produce `SyncPending` y no envía las fotos incompatibles. Un timeout/IO produce `SyncCommunicationFailure` y reintento; HTTP 503 utiliza esa misma vía de comunicación, por lo que no distingue por sí solo red frente a indisponibilidad HTTP. Un HTML con HTTP 200 produce error JSON, no una falsa compatibilidad: su mensaje actual puede ser genérico de preparación/comprobación. No se cambió Android.

**Con 1.4.15 (29), esta compatibilidad puede resolverse solo en Apps Script**, manteniendo la URL; no requiere otra compilación. No se inspeccionó la versión instalada en la tablet del ingeniero. APK anteriores al P0 pueden conservar otros defectos de más de 32 fotos; el ensayo corresponde a las fuentes actuales de 1.4.15.

## Límites y decisión

- La paridad se contrastó con las fuentes V20 aportadas, no descargando las fuentes privadas del despliegue. Comparar los tres cores actuales antes de sustituirlos.
- Se mantienen límites V20: 80 archivos por revisión, 24 MiB por staging y 160 MiB por revisión incluyendo filas SIC. El caso de 54 fotos cabe por cantidad; el tamaño real de la ficha debe cumplirlos. No se promete una mejora de latencia o cuotas.
- Falta prueba real de escritura en Google aislado y autorización de publicación. Los tiempos Node son simulados; el único acceso Google ejecutado fue el GET de capacidades.

**GO para revisión y ensayo aislado del parche. Publicación productiva pendiente de autorización, paridad y prueba Google.** Después se actualizaría el despliegue existente y Android reintentaría la misma ficha, sin recrearla ni borrar evidencias. [Pasos y reversión](DESPLIEGUE_CONTROLADO_Y_REVERSION.md).
