# Matriz focalizada — Prompt 7

| Requisito | Implementación reutilizada / conexión | Evidencia y alcance |
|---|---|---|
| A. Guardar y abrir detalle | MainActivity, SicFieldWorkspace, CulvertUnifiedScreen → RecordDetailScreen | UI: completar borrador SIC-08 abre detalle del mismo UUID y genera outbox; callbacks de formularios generales revisados |
| B. Ficha completa | RecordSheet, captureSummary, SicExportFormat, SupplementaryForms | UI abre consulta; proyección contractual comprobada; revisión de ramas 08/09, 17/A/B, 18/A, 19–23/34 |
| C/D. Galería correcta/categorías | RecordDetailPresentation.photos, InventoryPhotoReview | Prueba de registro ajeno, orden, contadores GENERAL e inspecciones distintas; UI abre imagen ampliada |
| E. XLSX real | LocalSicExport.prepareRoute, SicExcelWriter, GeneratedFileIndex | Paquete ZIP/XML XLSX válido; URI content y permiso de lectura; UI genera y expone Abrir; visor Android físico pendiente |
| F/G. Consolidado y columnas | Exportador existente por ruta/SIC | Dos filas, otra ruta y borrador excluidos; edición conserva fila ajena y 13 columnas SIC-18; SicExcelWriterTest valida formatos oficiales |
| H/I/J. Edición, actualización, UUID | Repositorios 2B y editores existentes | 10 regresiones SyncedRecordEdits2BTest; estado UPDATE_PENDING; rechazo obsoleto/conflicto; pruebas Node de dos operadores |
| K. Datos/fotos offline | Room, archivos originales y caché XLSX | Pruebas locales sin servicio remoto; copia recuperable desde otro LocalSicExport; confirmación remota nunca inferida |
| L. GENERAL histórico | Fallback solo para categoría nula/vacía | Prueba de agrupación y UI de fotografía GENERAL; historial conserva categoría original |
| M. SCAP individual | forRoadRecord → ScapWorkspace; ScapMedia/ScapExportPanel | Prueba nueva de dos inspecciones, abre la elegida y excluye nombre/foto ajenos; antigua prueba de pendientes falla en fixture (documentada) |
| N. Volver | Callbacks actuales, BackHandler y selección guardada | UI vuelve a Mis registros y ejecuta salida del detalle; cuatro entradas del dashboard conservadas; retorno físico al dashboard pendiente |
| O. Vertical/horizontal | Material 3, LazyColumn, botones >=48/56 dp | Pruebas Compose en 800×1280 y 1280×800; capturas nativas inspeccionadas; giro físico pendiente |
| P. Entrega automática | Mismos repositorios/outbox/worker | Validado crea QUEUED, borrador no crea entrega definitiva; exportar local conserva syncedRevision=0; ninguna cola nueva |
| Pavimentos | Proyección de orden → muestra original | Reutiliza editor, galerías y exportación propios; revisión de enlace, sin prueba física ni cambios PCI/URCI |
| Remoto/global | Backend existente sin modificaciones | 8 pruebas Node con dobles Drive: mismo fileId, filas ajenas, conflictos y rollback; NO prueba remota |

Los resultados completos y los intentos fallidos figuran en VALIDACION.json. No se ejecutó la suite completa.
