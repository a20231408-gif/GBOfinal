# Auditoría acotada y compatibilidad histórica

Solo rutas Drive, backend V3, modelos/catálogos, categorías, UUID/numeración, metadata/repositorios, Excel, orden/mapa/KMZ y pruebas vinculadas.

Activos corregidos: AutomaticDelivery (antes exigía sibCode; compartía identidad SIC-09; ACK/Excel ligados a SIB), DrivePathPlanner/DrivePaths, OrderedAsset/AssetPresentationCatalog, ScapHumanDelivery, SyncV3Core/SyncV3, vistas de orden/mapa y KmlExporter. El mapFolder fijo del KMZ dejaba afuera fajas, puntos críticos y accidentes. SIC-19 clases 12/13 y SIC-23 clases 22/23/24 no estaban correctamente diferenciadas. SIC-21 no tenía detalle relacionado en InventoryRecordWithPhotos. Pavimentos y padres SCAP se agregaron mediante proyecciones de solo lectura, sin persistir coordenadas ni alterar sus registros.

Referencias históricas retenidas: DriveFolderRouter.resolveSib para parámetros de jobs antiguos; ViewModel/scheduleDriveUpload/DriveUploadWorker transforma esos jobs a la cola por UUID, cuyo emisor es V4. DeliveryMetadata, DeliverySync y contratos V1/V2 de Code.gs conservan lectura/documentación de lotes anteriores. El backend V3 habilitado rechaza nuevas llamadas legacy antes de escribir. No son fallbacks del esquema 4. Los fixtures y documentos de Prompt 1 describen el esquema anterior; no se sobrescribieron.

SyncV3 conserva canonical/recibos de pathVersion 3 para reintentos idénticos de revisiones ya registradas. Nuevos registros pathVersion 3 se rechazan LEGACY_READ_ONLY. Un UUID histórico que intente cambiar a pathVersion 4 se rechaza HISTORICAL_MIGRATION_REQUIRED. Nunca se adopta una carpeta ajena ni se mueve contenido SIB para una entrega nueva. Room conserva todas sus columnas; únicamente se añadió una relación de lectura de sic21_details, sin migración de datos.

Migración controlada pendiente, sin ejecutarla:
1. Autorizar alcance/raíz y ventana sin entregas; respaldar archivos, registros/bindings/recibos y sus hashes.
2. Generar inventario de correspondencia UUID → revisión → folderId/fileIds históricos → familia propuesta → fila SIC. Informe de solo lectura; revisar categorías y duplicados.
3. Aprobar el manifiesto de migración y elegir mantener histórico enlazado o mover elementos identificados. Reservar números V4 atómicamente, registrar vínculo origen/destino y operación reversible.
4. Reconciliar consolidado por ruta/SIC con los UUID aprobados, sin insertar dos veces una misma fila. Preservar referencias históricas y todos los IDs que deban seguir accesibles.
5. Verificar hashes/IDs, fotografías, filas, ACL y reintentos antes de marcar la migración completa. Ejecutar rollback del manifiesto si falla; no borrar originales automáticamente.

No se ejecutó migración, no se tocó el SIC-08 de pruebas bajo SIB-02 y no se publicaron Apps Script. Cambios de ruta/familia de una identidad ya vinculada también requieren reconciliación autorizada; cambiar solamente PR actualiza datos/orden y conserva folderId/número. La edición de sincronizados del Prompt 2B permanece fuera de esta tarea.
