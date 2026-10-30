# GBO_Inventario - preparado para revisión, NO listo para producción

La carpeta usa el nombre solicitado; «listo para revisión» no significa publicable. **Faltan las fuentes de la versión exacta desplegada y mediciones reales autorizadas.** No hay publicador automático, cambio de configuración ni copia de Code.gs. No se ha modificado el proyecto GBO - Servicio de Usuarios.

## Inventario exacto de .gs entregados

| Archivo | Origen | Optimización real | Acción ahora |
|---|---|---|---|
| `candidato-no-publicar/SyncV3.gs` | Copia byte a byte de `docs/hotfix-1.4.6/backend-propuesto/SyncV3.gs` | Decodificación base64 y SHA-256 del archivo de staging antes del ScriptLock; telemetría numérica por operación/fase. Mantiene ScriptLock, tryLock(30000), lecturas/escrituras Drive y consolidación protegidos. | Revisar diff contra la versión publicada; no reemplazar automáticamente. |
| `referencia-no-reemplazar/SyncV3Core.gs` | Copia byte a byte del motor local `google-apps-script/SyncV3Core.js` | Ninguna: motor transaccional y reconstrucción XLSX sin cambios. | Referencia de contrato. Si el motor publicado es distinto, conservarlo hasta revisar cada diferencia. No sustituirlo solo por acompañar al adaptador. |

Los hashes están en `evidencia/archivos-gs.json`. La propuesta original permanece intacta. No se añadió otra optimización especulativa a estos archivos. Los demás módulos no están incluidos como instalables porque aún no se revisaron sus versiones publicadas.

## Hallazgo de compatibilidad que impide copiar a ciegas

El adaptador local base y el motor coinciden byte por byte con los archivos de `docs/parche-apps-script-10k`. El adaptador propuesto hereda llamadas a `SicObservationsV1.plain`, además de `inspect`/`printExtent`, y el motor usa `select`/`inherit`. **El README anterior que decía no incluir 10K no demuestra independencia de 10K: hay una dependencia efectiva de su base local.** Si producción tiene un módulo Observaciones anterior sin `plain`, copiar el adaptador puede romper la exportación. La prueba combinada demuestra ese rechazo de forma aislada. `sicObservationsVersion=1` no distingue esas variantes.

No se mezclaron ni publicaron ahora 10H o 10K. Antes de integrar, hay que identificar qué partes ya existen en producción. Los nombres V2 de videos anunciados tampoco demuestran que cada línea del parche 10H sea idéntica.

## Qué puede y qué no puede mejorar

- Disminuye el trabajo de validación pura dentro del lock de staging; no se ha medido cuánto tarda en Google ni que sea el origen de las esperas de campo.
- Instrumenta `stage`, `reserve` y `commit`: preparación, espera/adquisición, duración aproximada de ocupación y fases de lectura/recuperación/XLSX/transacción.
- **No reduce reconstrucciones de Excel, no elimina el lock global ni permite commits paralelos.** El ensayo determinista local de cinco entregas con una fila cada una hace 20 descompresiones y 5 compresiones tanto antes como después.
- La instrumentación no cubre el lock de VideoV1 ni desglosa cada operación Drive. Las fases anidadas se solapan: no sumarlas como si fueran tramos excluyentes. Los tiempos son por ejecución del script; no prueban la latencia completa observada por Android.

## Orden de revisión

1. Leer `ARCHIVOS_PUBLICADOS_REQUERIDOS.md` y obtener únicamente código redactado y metadatos no privados de GBO_Inventario.
2. Comparar las fuentes exactas con la referencia y revisar los puntos de `COMPARACION_Y_COMPATIBILIDAD.md`.
3. Elegir mejoras posteriores usando `MEJORAS_RECONSTRUCCION_EXCEL.md`; ninguna está activada por este paquete.
4. Solo después de paridad y pruebas aisladas reales, solicitar autorización para el procedimiento de `RESPALDO_PRUEBA_PUBLICACION_REVERSION.md`.

## Evidencia y alcance

`evidencia/capacidades-consulta-anterior.json` conserva la lectura sanitizada del 15/10/2026, 20:20 Lima: V3/V4, fotos legibles V2, videos V1 SCAP, nombres de video V2 y Observaciones SIC=1. **No se hizo otra consulta ni se leyó Drive productivo para preparar este paquete. No equivale a una instantánea actual del código desplegado.**

Las pruebas locales utilizan Drive, LockService, usuarios y archivos ficticios. Ver `evidencia/RESUMEN_PRUEBAS.json` al finalizar. El caso combinado prueba los .gs de esta carpeta; las regresiones 10H/10K prueban módulos locales de referencia, no producción. La carga de dos tablets simula intercalaciones/BUSY y pérdidas de respuesta; no es concurrencia real de ejecuciones Google ni demuestra su rendimiento.

No copiar archivos `.test.cjs`, diferencias, informes o módulos duplicados al editor Apps Script. No hay Code.gs, tokens, propiedades privadas, claves ni IDs de carpetas reales en este paquete.
