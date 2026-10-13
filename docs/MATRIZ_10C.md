# Matriz 10C

| Caso | Evidencia | Resultado |
|---|---|---|
| Captura guardada por UUID, múltiples fotos, dos daños iguales y panel por muestra | PavementPhotos10CTest | Aprobado local; cámara representada por archivo sintético |
| PNG/JPEG originales y EXIF preservados, copia JPEG final | PavementPhotos10CTest | Aprobado local |
| Reemplazo/retirada con historial, no sobrescribir ni borrar daño con evidencia | PavementPhotos10CTest | Aprobado local |
| Reinicio offline de base Room, asociaciones activas/retiradas | PavementPhotos10CTest | Aprobado local; no reinicio físico de tablet |
| Migración 18→19 conserva fotos históricas/muestra cerrada | PavementPhotos10CTest | Aprobado local |
| Cola existente, SHA-256/JPEG, ACK perdido y reintento idempotente | PavementPhotos10CTest | Aprobado local, backend y contenedor PDF simulados |
| Acciones por UUID y contador, no videos, consulta de muestra cerrada | PavementPhotoUi10CTest | Aprobado Compose local |
| Navegación desde muestra y Fotografías general, actualización sin refresco manual | PavementPhotoUi10CTest | Aprobado Compose local |
| Huella de snapshot histórico tras esquema 19, incluidas fechas iguales | PavementPhotos10CTest | Aprobado local; última ejecución de transporte/huella: 2/2 en orden-historico.log |
| Regresión importación/retirada y migración histórica 12→actual | PavementModuleTest / PavementParallelCTest focalizados | Aprobado local |
| Descarte/recuperación local conserva UUID y originales y no encola | LocalDraftDiscard10CTest | Aprobado local |
| Cola/error/ACK incierto, binding, foto sincronizada y manifiesto anterior bloquean descarte | LocalDraftDiscard10CTest | Aprobado local |
| Descarte de Pavimentos solo por inspector creador | LocalDraftDiscard10CTest | Aprobado local |
| Llamada directa no anula remotamente sin autoridad | InventoryLifecycle10Test | Aprobado local |
| PIN y permiso específico, tablet/ficha/ruta/usuario, revocación y vencimiento | InventoryAuthorization10C.test.cjs | **7 casos aprobados (6 + 1)**, almacén de autoridad simulado |
| Anular segundo de tres, Excel de todos operadores, restauración, offline y conflictos | PENDIENTES_BACKEND_10C | **Pendiente, no implementado ni aprobado** |
| Numeración continua después de ACK, sin renombrar carpetas | Diseño backend | Pendiente de anulación compatible; algoritmo aprobado no se modifica |
| SIC/SCAP | No cambios en componentes fotográficos; bloqueo remoto conservado | Regresiones específicas de esquema/descartes; no se reejecuta toda la batería anterior |
| Cámara real/importación URI/HEIC, proceso Android finalizado y entrega Drive real | Guía tablet en ENTREGA_10C | Pendiente físico; ninguna escritura real 10C |
| APK Debug con servicios locales | apk-debug-10c.json / debug-10c.log | BUILD SUCCESSFUL, configuración real local y firma Debug verificadas; versión 1.4.2 / código 16 |

Resultado acumulado: **18 casos Android distintos aprobados** en ejecuciones focalizadas y **7 casos de autorización** aprobados entre dos ejecuciones (6 + 1). La compilación final `assembleDebug -PgboValidationBuild=false` terminó correctamente. Los endpoints/token locales están presentes en la APK; no se publican sus valores. Auditoría de entrega: sin valores privados expuestos ni APK/configuración local rastreada por Git. La prueba física y la anulación remota siguen pendientes.

Primer intento de pruebas: compilación falló por firma ampliada de PavementDamageTable que desplazó el callback de una llamada antigua. Se conserva el overload anterior y se registra el resultado posterior sin cambiar esa prueba aprobada. Logs y XML de cada ejecución se mantienen como evidencia.

La primera corrida funcional tuvo 17 casos, 11 aprobados y 6 fallidos. Se corrigieron únicamente esos seis: identidad Android ausente en fixtures, cadena histórica incompleta, índices opcionales del JSON de esquema, limpieza del test Compose que ocultaba su fallo, selección semántica para navegación del diálogo y ACK simulado de Excel. Finalmente se aisló la preparación: `PdfDocument.nativeCreateDocument` no provee un handle nativo en este JVM Windows; el error de documento cerrado ocurría antes de post. Se añadió shadow **solo de contenedor PDF** para las dos pruebas de cola/huella; el renderer usa Canvas/Bitmap reales, fotografías JPEG y SHA-256 reales, Excel del motor real y ACK simulado. El PDF sintético no es un PDF contractual probado ni una entrega real.

La huella y el orden de lectura de fotos activas conservan el formato de esquema 18, para evitar invalidar snapshots pendientes al agregar columnas de historial. Caso adicional explícito con dos fotos de fecha idéntica e IDs fuera de orden; no se modifica el ordenamiento KMZ ni la numeración estable. Los resultados finales se acumulan de ejecuciones focalizadas; no se afirma que toda la batería anterior se volvió a correr en una única ejecución.
