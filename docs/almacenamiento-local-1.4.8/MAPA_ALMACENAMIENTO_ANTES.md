# Mapa comprobado antes de implementar 1.4.8

Fuentes actuales de la candidata 1.4.7, no la rama histórica. `filesDir` y `cacheDir` son privados: normalmente están bajo `/data/user/0/com.tuempresa.inventariovial/`, inaccesibles al administrador de archivos. No equivalen a Download, DCIM ni Android/media.

| Contenido | Ubicación actual y categoría | Cuándo es accesible fuera de GBO |
|---|---|---|
| SIC, detalles, fotos/metadatos, sesiones | Room `inventario_vial.db`, persistente privado A | No es un documento compartido. |
| Algunos borradores de formularios | `filesDir/inventory_drafts/*.json`, A, además de Room | Solo dentro de GBO. |
| SCAP y borradores, defectos, referencias de croquis | Tablas SCAP en Room; URI/rutas en entidades, A | Croquis que referencian URI SAF dependen del permiso/origen; no son una carpeta pública de entregas. |
| Pavimentos, daños, marcas 22/26 | Room; referencias 26 en `sicDataJson`, A | Se incluyen en exportaciones, no en documentos públicos al guardar cada campo. |
| Fotos capturadas/importadas SIC, SCAP, Pavimentos | `filesDir/images`, A. Original y metadatos Room; algunas variantes en `stamped` | No aparecen por ello en Galería o Descargas. |
| Videos SCAP y sesiones de subida | `filesDir/videos`, A; Room separado `field_video_9.db` | Original privado; una sesión de Drive no es una copia local pública. |
| Excel SIC manual y por elemento/ruta | `cacheDir/sic_exports`, B | C solo si el operador completa CreateDocument y copia al URI elegido. La caché puede desaparecer. |
| Excel SCAP manual | `filesDir/scap_exports`, A | C después de CreateDocument. El mensaje previo no demostraba una copia pública automática. |
| ZIP SCAP | `filesDir/scap-drive/<UUID>/<revision>_bundle…zip`, A; preparación en caché B | Privado, aunque esté completo. |
| PDF/Excel/JSON Pavimentos | `filesDir/pavement/exports` y manifiesto A; además preparaciones automáticas A | C si se usa CreateDocument; registrar `destinationUri` no vuelve público el origen privado. |
| Preparaciones nuevas Drive, staging/revisiones | `filesDir/automatic_drive/<hash>/<revision>`, A; outbox Room, recibos privados | Se mantienen para reintento/ACK. No constituyen una exportación a Descargas. |
| Lotes ZIP/Excel de rutas, otros archivos generados | Carpetas privadas de entregas y/o caché, A/B; índice `GeneratedFileIndex` | Abrir/Compartir mediante FileProvider concede acceso temporal, no publica una carpeta. |
| Documento elegido con selector Android | URI SAF, C | Depende del proveedor elegido: puede ser Descargas local o un proveedor remoto. No asumir ubicación local ni permiso eterno. |
| Copia automática en almacenamiento compartido | **No hay flujo general MediaStore para las tres familias**, D ausente | Este es el defecto de experiencia solicitado, no pérdida demostrada de Room/fotos. |

Fuentes inspeccionadas: LocalSicExport, SicExportScreen, ElementExports, ScapExportPanel, ScapDelivery, PavementGeneratedFiles, PavementOutputPanel, PhotoUtils, importadores fotográficos, VideoStorage/VideoSync, AutomaticDelivery/AutomaticSync, InventoryDatabase y los DAO. Problemas: mensajes mezclan persistencia interna y archivo público; generación y sincronización no equivalen a exportación visible; falta cola local durable, destino explícito, referencias reabribles y recuperación de copia interrumpida. No se limpió ni movió ningún original.
