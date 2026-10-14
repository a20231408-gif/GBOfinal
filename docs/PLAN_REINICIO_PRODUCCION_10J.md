# Plan de reinicio controlado de datos de prueba

Fecha: 14 de octubre de 2026. Estado: **plan para decisión del propietario; no ejecutado**. Base de código 10I, 8f68672. No se hizo inventario de contenido real de Drive o de bases de tablets; cantidades y clasificación de UUID quedan por completar. Este documento no autoriza borrar, mover ni cambiar configuración.

## Decisión propuesta

Preferir una **raíz de datos de producción nueva y vacía**, conservando la raíz de pruebas completa como archivo recuperable. Preservar el proyecto Apps Script, URL de implementación, servicio de Usuarios, credenciales y capacidades existentes; cualquier cambio futuro de destino o publicación lo hace el propietario tras aprobarlo. No copiar a la raíz nueva los consolidados, bindings ni ._GBO_SYNC_V3 de pruebas.

Una raíz nueva por sí sola **no impide resubidas antiguas**: las tablets envían al mismo /exec y el servidor resuelve ROOT_FOLDER_ID. Una tablet offline con outbox antiguo podría poblar la nueva raíz al reconectar. El protocolo actual no incluye una identidad de conjunto de datos o época de reinicio validada por el servidor. Tampoco basta cancelar WorkManager: wake y la cola Room pueden programarlo de nuevo.

No declarar seguro el inicio de campo hasta controlar todas las tablets y los posibles emisores antiguos. Si no puede asegurarse su aislamiento, será necesaria una barrera de servidor y cliente para un identificador de conjunto de datos, con rechazo de peticiones antiguas/ausentes **en todos** los endpoints de escritura, incluidos legacy, fotos, reservas y videos. Es una propuesta técnica futura mínima, no una función implementada aquí. El token compartido o ACCESS_CONTROL_ENABLED=false no proporcionan esa barrera por conjunto de datos; no se cambian credenciales ni banderas ahora.

## Alternativas

| Alternativa | Ventaja | Riesgo y condición |
| --- | --- | --- |
| Raíz nueva; raíz antigua preservada | Separación visible, recuperación sencilla, sin desmontar metadatos en uso | Requiere cambio manual de destino aprobado y aislamiento/barrera frente a todos los clientes antiguos; mantener configuración, no copiar estado de pruebas |
| Reinicializar la raíz existente | Mantiene el ID y enlaces de la raíz | Más peligroso: hay que archivar coordinadamente archivos, registry, recibos legacy y sesiones reanudables. Borrar solo Excel o carpetas deja referencias vivas; conservar registry con carpetas vacías tampoco es un reinicio |
| Renombrar carpetas o borrar solo Excel | Ninguna garantía suficiente | No recomendado: los fileId/bindings y los journals pueden reescribir/recrear archivos; los números no empiezan necesariamente de cero |

Si conservar el ROOT_FOLDER_ID fuera una condición absoluta, usar la segunda opción solamente tras ensayar en una copia completa y con el mismo aislamiento. No ejecutar ninguna de las dos opciones en esta tarea.

## Inventario previo obligatorio

El propietario designa responsable y ventana sin capturas/envíos. Preparar un manifiesto privado con:

- Cada tablet: identificador ya existente/huella, custodio, aplicación Debug/Release, versión y certificado, última conexión, estado físico/offline, acceso a un respaldo compatible y autorización de mantenimiento. No copiar tokens ni PIN al informe.
- Por tablet y módulo: UUID, estado, revisiones local/confirmada, número visible, claves de outbox, preparaciones conservadas, fileId remotos, fotos activas/históricas, videos y offset/sesión de carga. Marcar explícitamente cada elemento como PRUEBA o CONSERVAR; el prefijo TEST no sustituye revisar los UUID.
- Drive: raíz real y estructura por ruta, EXCEL_SIC, SIC/SCAP/Pavimentos, fotos, videos, versiones/revisiones, staging, datos técnicos y cualquier archivo manual fuera de esas carpetas. Registrar IDs y padres; no publicar vínculos privados en Git.
- Apps Script: copia privada de fuentes publicadas, versión/implementación/ejecutor, propiedades y configuración. Enumerar por nombre/alcance los recibos y referencias de datos sin volcar secretos al repositorio. Servicio de Usuarios y permisos de tablets son inventario separado que se conserva.
- Otras vías: sincronización enterprise-inventory-sync, importaciones manuales, procesos de escritorio, APK antiguas disponibles para instalar, copias en Descargas/SAF y sesiones reanudables que siguen siendo válidas.

Si existe un servicio enterprise activo detrás de la configuración local, sus registros y recibos son un almacén adicional: una raíz Drive limpia no los reinicia. En esta revisión no se consultó ese servicio ni se dispone de un inventario de sus datos; el propietario debe clasificarlo y coordinar su aislamiento/archivo si se utiliza. No se atribuye su estado a un resultado del diagnóstico Drive.

**No se conoce el número de tablets ni UUID de pruebas en esta revisión.** Ninguna tablet no localizada se considera automáticamente limpia o bloqueada.

## Qué se limpiaría localmente y qué se conserva

Inventario basado en InventoryDatabase v19, VideoDatabase v1 y rutas reales del código. Es una selección futura por UUID/ámbito PRUEBA; no se propone clearAllTables ni borrar la base completa.

| Almacén | Tratamiento propuesto tras respaldo y aprobación |
| --- | --- |
| inventario_vial.db: inventory_records; sic17_details a sic23_details; sic17a/b, sic18a, sic_extension_details | Retirar únicamente registros PRUEBA y detalles dependientes, respetando relaciones y transacción Room |
| photos, photo_history, track_points | Retirar filas de pruebas y sus vínculos tras verificar archivos y referencias cruzadas; preservar originales en respaldo |
| scap_inspections, scap_values, scap_spans, scap_substructures, scap_supports, scap_elements, scap_conditions, scap_defects, scap_sketches, scap_profile_points, scap_joints | Retirar el subgrafo de inspecciones ficticias, incluidos vínculos con inventory_records; no dejar una cabecera SCAP huérfana |
| pavement_samples, pavement_damages, pavement_damage_photos, pavement_sections | Retirar muestras de pruebas, daños, historial fotográfico y configuración de sección exclusiva de esas pruebas; no borrar una sección compartida con datos que deban conservarse |
| drive_sync_outbox, delivery_batches | Retirar intenciones, revisiones, ACK, preparedJson, receiptJson y lotes exclusivamente de pruebas; no limitarse a poner SYNCED o volver a baseRevision=0 |
| field_video_9.db, tabla videos | Retirar filas de videos de prueba, parentKey, ACK, offsets y referencias de sesión. Archivar originales; no reutilizar una URL reanudable antigua |
| user_cache, user_sync_state, local_login_state, auth_audit | **Conservar** usuarios/verificadores PIN, sincronización y estado de bloqueo; mantener auditoría histórica y añadir acta de mantenimiento, sin reescribir roles. Una nueva sesión podrá requerir el PIN normal |
| field_prs, field_sessions | Conservar PR/rutas y contexto válido; revisar punteros de sesión a fichas retiradas y limpiar solo selección/registro en curso de pruebas, no los catálogos ni al operador |
| files/images, stamped, videos y videos/capture | Inventariar todos los archivos por referencia; archivar/quitar solo los de prueba aprobados. No borrar images entero si hay contenido conservado o referencias externas |
| files/automatic_drive, route_deliveries, scap-drive, scap_exports, pavement/drive_staging, pavement/exports + manifest.json | Archivar las preparaciones/exportaciones de los UUID de prueba y sus stages-*.json; retirar sus índices locales junto con la cola. No restaurarlos después en la producción limpia |
| files/inventory_drafts, culvert_edit_drafts | Retirar borradores/autoguardados de pruebas aprobados para evitar que reaparezcan como nuevas fichas |
| cache/sic_exports, kml y copias temporales de entrega | Limpiar únicamente artefactos de prueba; no mezclar este paso con el borrado global de datos de Android |
| Descargas y destinos SAF escogidos por usuario | Inventariar y archivar manualmente; quedan fuera del sandbox y no desaparecen al retirar filas. Marcar PRUEBAS para evitar importación accidental |

La tabla de VideoDatabase se llama **videos** en el código; confirmar el esquema de la APK instalada antes de cualquier procedimiento ejecutable. Las relaciones de Room y el orden de retirada se deben obtener del esquema real y ensayar; este plan no contiene SQL destructivo.

### Preferencias y claves

**Conservar íntegramente:** user_service (provisión cifrada del /exec y token de tablet), device_access (huella y autorización), Android Keystore y sus alias, tablet_admin_permission_hints, field_settings, field_camera, rutas/KMZ y plantillas empaquetadas, road_geometry_versions/active_road_geometry, visual_geometry_9 y claves route/*/reverse/* de visual_order_9. Mantener firma, applicationId, usuario Android y autorización del servicio de Usuarios. No cambiar claves de cifrado.

**Archivar y retirar selectivamente las referencias de datos de pruebas:** drive_receipts_v2, drive_bindings_v4, drive_element_identity_v2, photo_names_v2, photo_names_9, video_readable_names_v2, element_numbers_9 (id/UUID y next/ámbito de pruebas), drive_progress_10d, drive_core_sync_times; manual/UUID de visual_order_9, selecciones/autoguardados asociados en sic_capture_context y survey_context. Estas últimas preferencias también contienen contexto útil: conservar los valores de ruta/configuración y quitar solo punteros a fichas de prueba. sic_export contiene encabezados de exportación: conservar encabezados/plantilla, revisar por separado sus marcadores de trabajo.

No reusar IDs ni recibos de prueba para datos reales. Los consecutivos locales y los del servidor son diferentes: reiniciar uno no reinicia el otro. El número central V4 se deriva de bindings por ámbito/familia; fotos y videos tienen reservas propias que deben pertenecer a la misma nueva campaña.

## Qué se archivaría en Drive/backend

Conservar la raíz de pruebas como unidad coherente, sin vaciar sus carpetas a mano. Incluye:

- Carpetas de elementos SIC y Puentes SCAP, EVALUACION_SUPERFICIAL, HOJAS_CAMPO, DATOS_APP, paneles, VIDEOS, los nombres/carpetas legacy y las versiones históricas.
- EXCEL_SIC por ruta con todas sus filas y archivos manuales de superficie. Un Excel consolidado no debe sobrevivir con filas de pruebas en la nueva producción.
- **._GBO_SYNC_V3 completo:** record_*.json con revisiones/receipts/filas/base64, binding_*/binding_v4_*, aggregate_*, photo_names_v2_*, STAGING, REVISIONS y journals. VIDEO_V1 incluye video_*.json, reservas names_v2_* y sesiones/fileId asociados.
- Journals PENDING y sus backups deben permanecer juntos. v3Store recupera transacciones pendientes antes de leer los recibos; perder una mitad puede impedir recuperar o reintroducir contenido. El responsable debe resolverlos con el código existente y en una copia de ensayo, no borrarlos para que desaparezca el aviso.
- ScriptProperties legacy de Code.gs (recibos pavement_file_*, bindings SCAP, delivery_folder/file/name/batch y propietarios) pueden referenciar IDs de carpetas/archivos aun fuera de la raíz nueva. Inventariar claves reales y dependencias; **no reiniciar todas las ScriptProperties** ni copiar ciegamente los recibos a producción.
- Conservar propiedades de capacidades V3/V4, fotos, videos, credenciales y configuración, y todo el servicio de Usuarios. La separación exacta entre configuración y datos exige revisar la implementación publicada; la copia local no demuestra qué claves existen en producción.

Las sesiones de video ya emitidas pueden recibir bytes directamente hacia Drive sin pasar por una nueva llamada de inicio. Aislar y detener los clientes evita nuevos chunks; una barrera futura debe contemplar también sesiones antiguas, el cierre ACK y sus archivos, no solo commitRecordV3. Si no es posible invalidarlas con el mecanismo disponible, mantener sus destinos en la raíz archivada y esperar su vencimiento/control del emisor antes de declarar cerrado el reinicio.

## Secuencia asistida por dispositivo

1. El propietario confirma los UUID de prueba y los elementos CONSERVAR; fija responsables, hora de corte y el criterio de numeración de la campaña nueva. No empezar registros reales durante la ventana.
2. Aislar de red **todas** las tablets emisoras y otros clientes. Detener capturas/recorridos y la aplicación con procedimiento de mantenimiento autorizado; no hacer force-stop ahora. Mantener las tablets no localizadas en cuarentena y fuera de producción.
3. Con cada aplicación quiescente, realizar respaldo consistente de ambas bases, sus WAL/SHM si corresponden, archivos, índices y preferencias. Un archivo .db copiado con escrituras activas no basta. Reabrir/verificar en una copia aislada, comparar conteos/UUID y SHA-256 del manifiesto. Guardar cifrado, fuera de Git, con custodio y segunda copia.
4. Preparar en una versión de mantenimiento firmada de forma compatible una operación **local y selectiva**, con vista previa de UUID/archivos, transacción Room y acta. No existe actualmente una herramienta de reinicio selectivo lista y validada; este plan no la desarrolla. Para Debug podría estudiarse run-as, pero no se presupone disponible en Release ni se utiliza ahora. No extraer claves ni usar root para sortearlo.
5. Cancelar y esperar la detención de trabajos de datos: automatic-drive-drain, automatic-drive-recovery, pavement-drive-*, route-delivery-*, drive-upload-*, field-videos-9 y enterprise-inventory-sync. Conservar gbo-user-sync-now/periodic. No borrar manualmente androidx.work.workdb; WorkManager debe gestionar su estado. Impedir que la app reencole mientras dura el mantenimiento; el código actual no tiene una barrera global de mantenimiento que se dé por implementada.
6. Ejecutar la retirada selectiva futura de filas, archivos e índices aprobados. Conservar auth, Keystore, rutas y configuración. Retirar archivos después de confirmar las referencias DB; manejar una interrupción con manifiesto y recuperación, sin dejar un commit parcial oculto.
7. Comprobar en modo offline cero fichas/medios de prueba visibles, cero outbox/preparaciones y ningún trabajo de datos pendiente/activo; comprobar usuarios, huella, rutas, KMZ y plantillas conservados. Reiniciar la app todavía aislada y revisar que nada reaparece.
8. Solo después del acta de todas las tablets o de una barrera efectiva, el propietario aprueba/prepara la nueva raíz y el cambio de destino/publicación manual necesario. Verificar capacidades y ausencia de bindings/filas de pruebas. Mantener la raíz anterior y sus metadatos archivados sin conexiones de clientes nuevos.
9. Reconectar una tablet limpiada bajo supervisión, sin crear fichas: verificar que no se reenvía ningún UUID viejo. Repetir tras reinicio y ejecución del mecanismo periódico. Después, con autorización específica, una entrega de aceptación ficticia en un ámbito separado de validación comprueba SIC/SCAP/Pavimentos, ACK, nombres y numeración. No contaminar el levantamiento real con esa aceptación.
10. Liberar el dispositivo para campo y registrar el acta: identidad antes/después, conteos, manifest hashes, versión y certificado, root de campaña, responsable y resultado. Liberar las siguientes de una en una. Ninguna tablet vuelve a producción solo por tener conexión.

## Verificación y recuperación

| Control de aceptación | Evidencia mínima |
| --- | --- |
| Preservación | Misma huella/usuario Android, firma/applicationId, provisión y autorización; conteos de usuarios y rutas/KMZ/plantillas; PIN verificado solo por su titular |
| Limpieza selectiva | Lista de UUID retirados, conteos por tabla y archivos, cero referencias huérfanas; cualquier CONSERVAR permanece |
| Cola detenida | Outbox/lotes/preparaciones/URLs de video antiguas ausentes; trabajos de datos cancelados y sin reaparición tras abrir/reiniciar |
| Backend nuevo | Cero filas, bindings, reservas y recibos de pruebas; no reaparecen tras reconectar y recuperar periódicos; capacidades conservadas |
| Continuidad | Primera entrega autorizada confirma todos sus archivos/formatos esperados, no solo stage; numeración de la campaña y aislamiento de UUID antiguos comprobados |
| Recuperabilidad | Respaldo comprobado en copia y manifiesto custodiado, instrucciones de restauración para la misma instalación y claves |

Si falla una verificación: aislar otra vez, detener nuevas entregas y conservar ambos estados. No mezclar snapshot antiguo de una tablet con backend nuevo. Restaurar cliente y raíz/metadata correspondientes como **un mismo punto de recuperación**, después de respaldar cualquier dato nuevo de campo. Mantener separado ese dato nuevo y conciliarlo; no sobrescribirlo con la copia de pruebas.

Restaurar provisión cifrada en otra tablet no garantiza funcionamiento: depende de su Android Keystore y autorización. No se promete clonación de identidad ni recuperación mediante reinstalación. Para Release sin backup interno compatible, la falta de respaldo restaurable es un bloqueo, no una razón para ejecutar pm clear, adb backup obsoleto o desinstalar.

## Intervención requerida del propietario

Autorizar inventario privado de Drive/tablets, clasificación por UUID y acceso a cada dispositivo; elegir raíz/campaña y números iniciales; aprobar respaldo y ensayo de restauración; aprobar y validar la herramienta local de mantenimiento futura y, si hay emisores no controlados, la barrera de conjunto de datos; ejecutar cualquier cambio de destino/publicación o archivo de backend; validar y firmar las actas por tablet. Mantener los PIN y secretos fuera de informes y Git.

**Acciones ejecutadas en 10J sobre los datos: ninguna.** No se cambia ROOT_FOLDER_ID, ScriptProperties, permisos, credenciales ni carpetas. No se desarrolla borrado remoto masivo ni anulación individual.

## Fuentes técnicas

InventoryDatabase.kt y app/schemas/.../19.json; media/VideoStorage.kt y VideoSync.kt; core/AutomaticSync.kt, AutomaticDelivery.kt, DriveAdapters.kt, LocalDraftDiscard.kt y GeneratedFileIndex.kt; DriveSync.kt; server/ServerSyncWorker.kt; field/ModuleElements.kt; road/LocalVisualOrder.kt y KmzRouteImport.kt; auth/UserServiceClient.kt; google-apps-script/SyncV3.gs, SyncV3Core.js, VideoV1.gs, PhotoNamesV2Core.js y Code.gs. Son fuentes locales; no sustituyen el inventario del servidor publicado ni de la flota.
