# Entrega automática de Pavimentos a Drive

Revisión acotada del código de la rama 10I, base 8f68672. No se consultó el contenido de Drive ni se envió una muestra. Describe el cliente y los adaptadores del repositorio; no acredita que una instalación de campo tenga esta APK o que el servidor publicado coincida íntegramente con estos archivos.

## Explicación para los ingenieros

Al cerrar una muestra, GBO guarda una solicitud de entrega automática. Con conexión y el servicio habilitado, prepara y envía el reporte PDF, un Excel de esa muestra, los datos JSON y las fotos activas de sus daños. No hace falta pulsar Reporte PDF ni Excel de superficie para esta entrega. Si todavía faltan datos contractuales del SIC, esos formatos no se consolidan aunque la muestra y sus otros archivos puedan confirmarse.

Los botones Reporte PDF, Excel de superficie y Excel de Datos para SIC sirven para guardar una copia en el destino que elige el operador. Guardar o abrir esa copia no significa que ese mismo archivo esté en Drive. En particular, **el Excel de superficie que reúne varias muestras es una exportación local y no se envía automáticamente como archivo de grupo**. Se puede compartir manualmente por el mecanismo ordinario, eligiendo conscientemente el destino.

Subir muestra a Drive solicita/reintenta la misma entrega automática; no cambia este alcance. Comprobar la revisión confirmada y los archivos de su recibo. Un archivo local puede seguir indicando LOCAL aunque el PDF/Excel equivalente de la muestra esté confirmado por otro mecanismo.

## Flujo vigente y protocolo antiguo

| Paso | Código real | Efecto |
| --- | --- | --- |
| Cerrar | PavementRepository.closeSample, líneas 82–90 | Guarda CLOSED y solicita AutomaticSync.request dentro de la transacción Room; no renderiza archivos dentro de closeSample |
| Despertar | AutomaticSync.request/wake/enqueue | Outbox persistente drive_sync_outbox; trabajo immediate automatic-drive-drain y recuperación cada 15 minutos automatic-drive-recovery, ambos con red conectada |
| Adaptador adicional | PavementSyncAdapter.enqueue → PavementSyncWorker.doWork | Trabajo pavement-drive-{UUID}; delega en AutomaticSync.manual. **No llama a PavementDriveUploader.uploadSample** |
| Preparar/enviar | AutomaticSyncDrain → AutomaticDelivery.prepare → AutomaticDriveHttp.post | Genera snapshot inmutable por revisión, stageRecordV3 de cada archivo y commitRecordV3 final, syncProtocol=3/pathVersion=4 |
| Confirmar | AutomaticDelivery.validAck y AutomaticSyncDrain | Valida recibo completo y fuente vigente; confirma base/revisión y estado SYNCED solamente si corresponden |

PavementDriveUploader.uploadSample implementa uploadPavementFile protocol=1/pathVersion=2 y genera HC, JSON, PAV_ASF/RIG/NPV.xlsx de grupo, fotos y últimos SIC_* exportados. La búsqueda de llamadas en app/src solo encuentra su declaración; AutomaticDelivery reutiliza su **nativeJson**, no su envío. No atribuir ese Excel de grupo ni sus recibos al worker vigente. No se propone reactivar ese cargador como atajo.

## Qué se genera y qué se entrega

| Archivo | Momento y alcance | Entrega actual |
| --- | --- | --- |
| HC_###.pdf | AutomaticDelivery.prepare al procesar la muestra cerrada, con cálculo, croquis y fotos; reutiliza prepared-path4.json si la fuente/revisión coincide | Automática, clave field |
| M_###.xlsx | En esa preparación, PavementExcel.write con **listOf(data)**: una muestra; hojas Fallas, Resumen y Detalle | Automática, clave sample_excel. El resumen de sección no lo convierte en un consolidado con las demás muestras |
| M_###.json | En esa preparación, nativeJson de la muestra, daños, cálculo y asociaciones fotográficas, sin rutas privadas | Automática, clave sample_json |
| Fotos JPEG | Copias de fotos activas por daño; originales retirados/reemplazados se conservan como historial local | Automática, clave photo/{photoId}; nombres según fotos V2/reserva y capacidades |
| SIC-14 / SIC-14A / SIC-15 / SIC-16 | AutomaticDelivery.prepare genera una fila XLSX por daño cuando PavementSic.status del formato completo es complete | Las filas viajan en sicRows del commit y el servidor actualiza ruta/EXCEL_SIC; no dependen de pulsar Excel. Un formato incompleto se omite; no inferir su entrega del estado global de muestra |
| Reporte PDF guardado por operador | PavementOutputPanel, selector CreateDocument, PavementGeneratedFiles.export categoría FIELD | Copia interna persistente + destino URI. El pipeline automático regenera su propio PDF; no sube esta copia exacta ni escribe recibo por su ruta local |
| Excel de superficie guardado por operador | PavementOutputPanel filtra **solo por superficie** y exporta GROUP_DATA; puede incluir distintas rutas/tramos y borradores presentes en esa tablet, sin filtro CLOSED | **Local/manual**; el pipeline vigente no enumera ni adjunta ese GROUP_DATA. No presentarlo como libro exclusivo de una ruta/tramo ni como consolidado de todas las tablets |
| Excel SIC guardado por operador | PavementSicPanel, exportación SIC_* habilitada cuando el formato está completo | Copia local. El pipeline vigente genera las filas desde Room; no adjunta ese archivo ni consume su manifiesto. Completar/guardar datos SIC en muestra cerrada sí solicita nueva revisión |
| Copias preparadas y journals | files/automatic_drive/{hash(key)}/{revision}, prepared-path4.json, file_{hash}, stages-*.json | Material de reintento local. Se envían bytes de los archivos declarados, no el directorio ni el journal |

Un cierre puede quedar pendiente por configuración, capacidades, validaciones o red. La recuperación no garantiza un plazo exacto: Android decide cuándo ejecutar WorkManager. Una edición en borrador exige volver a cerrar para entregar su nueva revisión.

## Carpetas de Drive actuales

Todas las rutas son relativas a la raíz privada configurada en Apps Script, cuyo ID no se imprime ni cambia aquí. El número N lo reserva centralmente el binding V4 y puede diferir del número local; SyncV3.gs también ajusta HC_/M_ al consecutivo del servidor.

| Contenido | Ruta relativa |
| --- | --- |
| Carpeta de muestra | {RUTA}/EVALUACION_SUPERFICIAL/{TRAMO}/{ASFALTICO o RIGIDO o NO_PAVIMENTADO}/MUESTRA N/ |
| PDF | Carpeta de muestra + HOJAS_CAMPO/HC_NNN.pdf |
| Excel por muestra y JSON | Carpeta de muestra + DATOS_APP/M_NNN.xlsx y M_NNN.json |
| Fotos | Carpeta de muestra + PANEL_FOTOGRAFICO/{nombre confirmado}.jpg |
| Excel contractuales consolidados | {RUTA}/EXCEL_SIC/SIC-14.xlsx, SIC-14A.xlsx, SIC-15.xlsx o SIC-16.xlsx, según superficie y datos completos |
| Estado técnico | Raíz/._GBO_SYNC_V3; registros, bindings, staging, journals y archivos de recuperación; no es una carpeta que deba limpiarse aisladamente |

Las carpetas antiguas 07_HOJAS_CAMPO, 08_PANEL_FOTOGRAFICO y DATA_APP de uploadPavementFile sin pathVersion son compatibilidad de protocol 1. No son el destino del flujo V4 descrito arriba. En protocol 1 con pathVersion=2, driveV2Folder usa el logicalPath recibido; no extrapolar su distribución por categoría.

## Qué ACK acredita cada estado

1. **Stage ACK**: ok, key, revision, fileKey, sha256 y stageFileId correctos. Solo acredita el archivo en staging; no permite declarar la entrega final sincronizada.
2. **Commit ACK de muestra**: ok=true, syncProtocol=3, pathVersion=4, key/revision/contentHash exactos, folderId y logicalPath de MUESTRA N. files contiene exactamente las claves esperadas, sin duplicados, con hash, fileId, folderId, requestedPath y logicalPath. Se conservan los fileId de bindings anteriores. La fuente local debe seguir vigente para marcar SYNCED.
3. **SIC consolidados**: por cada fila solicitada, sicFiles debe contener formato, fileId, logicalPath={RUTA}/EXCEL_SIC/{formato}.xlsx y recordIds que incluya sampleId_damageId. No hay confirmación de un formato que no se incluyó por incompleto. Para Pavimentos validAck no exige el hash/folderId adicional de la rama technicalDeliveryVersion=1; no afirmar una validación más estricta que la implementada.
4. **Archivo de exportación local**: GeneratedFileIndex solo muestra SYNCED si DriveReceiptStore tiene recibo de esa localPath y hash. Ese recibo lo escribe el cargador antiguo uploadSample; el drain V3/V4 guarda el recibo de conjunto en Room y no escribe DriveReceiptStore.file para estas copias. Por eso LOCAL no prueba que falte el equivalente automático, y SYNCED de muestra no acredita una copia GROUP_DATA.
5. **ACK antiguo uploadPavementFile**: ok, protocol=1, mismo sha256/fileName y fileId/folderId; con pathVersion=2 exige además logicalPath/sampleId/revision. No es el ACK que consume actualmente PavementSyncWorker.

## Brechas demostradas y alcance de una eventual corrección

**Archivo generado que no se entrega:** GROUP_DATA de Excel de superficie (varias muestras). Es verificable comparando el filtro de PavementOutputPanel con listOf(data) y las únicas claves field/sample_excel/sample_json/photo de AutomaticDelivery. No hay llamada al cargador antiguo que genera PAV_ASF/RIG/NPV.xlsx.

**Trazabilidad de copias:** PavementGeneratedFiles.export puede encolar una muestra cerrada, pero no modifica su updatedAt ni la revisión de outbox; AutomaticSync.manual no reenvía una revisión ya SYNCED. El hash de copias locales no forma parte de sourceHash. La exportación es durable, pero no incorpora automáticamente ese archivo a la entrega. Los SIC se generan a partir de sus datos, no por la existencia de la copia exportada.

Estas son diferencias reales entre el registro de exportaciones y el transporte vigente. No se cambia el cliente ni el backend en esta tarea. Si se requiere enviar un **consolidado de superficie entre tablets**, hace falta acordar su alcance/propietario y construirlo desde el estado central; subir un libro local de una tablet como consolidado global podría omitir otras muestras. Una mejora separada podría mostrar los documentos del commit y distinguir copia local de archivo remoto. No reescribir fotos V2 ni activar protocol 1 para resolverlo.

## Referencias verificables

- app/src/main/java/com/tuempresa/inventariovial/pavement/PavementRepository.kt: closeSample, saveSic y reopenSample.
- core/DriveAdapters.kt: PavementSyncWorker y DriveReceiptStore; core/AutomaticSync.kt: drain y confirmación.
- core/AutomaticDelivery.kt: rama PAVEMENT, source, prepare y validAck.
- pavement/PavementDrive.kt: uploadSample y nativeJson; PavementGeneratedFiles.kt: export y manifest.
- pavement/ui/PavementOutputPanel.kt y PavementSicPanel.kt: selectores locales.
- pavement/PavementExports.kt y PavementSic.kt: contenido de los exportadores.
- core/GeneratedFileIndex.kt: descubrimiento y estado por recibo local.
- google-apps-script/SyncV3.gs: elementLocation, transaction.file/aggregate; SyncV3Core.js: commit; Code.gs: uploadPavementFile.

Comprobación: inspección del flujo y búsqueda de referencias, sin pruebas de red, escrituras en Drive ni nueva batería general. Los números de línea son orientativos de la base 10I; los símbolos anteriores identifican los puntos exactos.
