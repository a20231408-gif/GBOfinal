# Commit SCAP reanudable v1 — SOLO COPIA AISLADA

No importar en producción. `SyncV3.gs` exige la propiedad GBO_ISOLATED_ROOT_ID y la identidad GBO; el original y `backend/referencia-v21/` permanecen intactos.

Mantiene syncProtocol 3/pathVersion 4/canonical hash y el ACK final. Una ejecución puede devolver `{ok:false,code:"COMMIT_IN_PROGRESS",retryable:true}`. Es continuación versión 1: el cliente debe conservar exactamente paquete/UUID/revisión/hash y repetir commit; nunca tratar esa respuesta como ACK. La compatibilidad de reintentos se prueba por separado. No cambia timeouts Android.

El journal permanece PENDING entre ejecuciones, con intención durable antes de crear por ID preasignado. Se reconcilian ID, propietario lógico, padre, nombre y SHA-256 antes de repetir. Se conserva el registro base mientras el journal está pendiente, incluso si se perdió una respuesta al escribir el registro nuevo. Otras transacciones quedan BUSY para proteger los consolidados. Solo finish publica COMPLETE y permite devolver ok:true.

Los hashes binarios/JPEG del staging se verifican y certifican en metadatos privados, ligados al SHA-256 real de Drive, propietario lógico, revisión, tamaño y MIME. Se vuelven a comprobar identidad, ubicación y checksum al reintentar. Un certificado no evita detectar cambios posteriores del blob. Los bytes se descargan de forma diferida para escribir, conservando los validadores originales de formato y canonical hash. Los XLSX consolidados se materializan una vez en el journal, para que los timestamps ZIP no cambien sus hashes entre ejecuciones.

La adopción del journal antiguo está limitada al fixture ficticio previamente auditado (key/revision/contentHash exactos). Antes de actualizarlo se conserva un snapshot `evidence_legacy_*.json`. No es una herramienta de migración de journals productivos. Un conflicto queda bloqueado conservando evidencia, sin rollback destructivo automático.

Instrumentación COMMIT_V1: búsquedas, metadatos, validación de bytes, certificados, carpetas, creación/actualización, journal y total. El presupuesto de 85 s se comprueba antes de nuevas operaciones; no sustituye la verificación del tiempo real HTTP ni garantiza un límite para una única llamada lenta de Drive.
