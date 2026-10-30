# Parche aislado de orden SIC — NO DESPLEGADO

Archivo completo: SyncV3Core.gs. Base: las fuentes originales V20 previamente revisadas, conservadas en FUENTES_V20_REFERENCIA_NO_IMPORTAR. El único cambio funcional es compareTechnical: PR numérico ascendente, metros y recordId como desempate. CAMBIO_ORDENAMIENTO.diff permite comprobarlo.

No sustituir Codigo.gs, SyncV3.gs, los módulos de videos, fotos u Observaciones. No mezclar este archivo con los candidatos de optimización ni con instrumentación sin un diff independiente. No cambia SHA/canonical, validaciones, reserva de números, UUID, revisiones, BUSY, journals, transacciones, ScriptLock ni ACK. Las familias/rutas y libros separados se conservan. El cambio actúa en la consolidación V4: el camino legado sin metadatos PR no se migra ni reinterpreta automáticamente.

Tres pruebas aisladas aprobadas: XLSX ascendente con calzadas/sentidos distintos; A/B/C/D -> A/B/D/C sin renumeración e idempotencia/BUSY; Observaciones históricas y edición posterior. El harness existente usa objetos Drive/LockService ficticios, sin HTTP. No son mediciones Google ni validación exhaustiva de todos los datos históricos de producción.

Reproducción local desde la carpeta del proyecto: `node --test docs/campo-20261023/backend-parche/OrdenProgresiva.test.cjs`. Reutiliza el harness/fixtures V20 locales previos, que deben conservarse. El XLSX de ejemplo contiene exclusivamente datos ficticios.

Antes de cualquier despliegue: confirmar que las ocho fuentes del servidor actual siguen correspondiendo a V20; guardar respaldo de fuentes y versión de implementación; comparar este diff contra el SyncV3Core real. No copiar valores privados de propiedades al informe. Validar el caso de progresivas mezcladas, una edición y un histórico con Observaciones en un proyecto de pruebas autorizado. Si faltan metadatos PR en históricos, detener su publicación y definir una conciliación explícita, sin renumerar ni fabricar PR.

Solo después de autorización separada: crear una nueva versión de la implementación existente, conservando URL, identidad, permisos, raíz y propiedades; no crear otro servidor de usuarios. Conciliar una entrega ficticia autorizada y conservar sus recibos. Reversión: seleccionar la versión anterior de esa misma implementación; no borrar ni restaurar journals antiguos sobre escrituras nuevas. El orden existente no cambia por copiar el archivo al disco local; los consolidados remotos actuales continúan con el comportamiento productivo hasta una publicación autorizada.
