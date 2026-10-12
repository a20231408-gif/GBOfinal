# Entrega local — Prompt 5

Rama `codex/fotografias-sic-5`, copia aislada `prompt5`, desde Prompt 4 `fec797c73d6eca00a009a3e53752b40276ecad35`. Este checkpoint contiene la infraestructura 2A/2B; se extiende esa misma cola, tabla de fotografías y backend V3. El commit local de entrega es HEAD de esta rama, consultable con `git log -1`; no hay push ni merge.

## Interfaz y persistencia

Sección FOTOGRAFÍAS con contador por categoría, varias capturas, importación múltiple de imágenes, miniaturas, vista ampliada, reemplazo y retirada con confirmación. El selector de categoría permite cambios explícitos. La galería se reutiliza en captura, fichas completadas y SIC-18/18A; SCAP conserva sus formularios técnicos y registros fotográficos propios, y amplía únicamente las acciones de su galería existente.

[Matriz completa](MATRIZ.md): INICIO/FIN/OBSERVACION para modelos correspondientes; PANORAMICA/ELEMENTO en SIC-22; claves CULVERT_* intactas; categorías SCAP exactas; GENERAL conservado para SIC-08/09, puntos y fotos anteriores. No se reclasifica al abrir ni guardar.

Cada foto conserva UUID, recordId, orden, archivo original, capturedAt cuando existe, categoría, syncStatus y fileId. Las fotos nuevas utilizan identidad determinista de registro/ruta local; reemplazar mantiene UUID, índice y fileId, cambia la copia activa y conserva el original anterior. Borradores conservan categorías y relación de reemplazo. Se excluyen fotos SCAP de las operaciones sobre la galería técnica SIC del mismo puente.

Room **17 → 18** agrega únicamente `photo_history`, con instantánea del vínculo anterior, acción y revisión. Ninguna tabla existente cambia; ninguna fotografía se migra o reclasifica. Historial append-only de reemplazos, retiradas y cambios de categoría; los archivos originales no se eliminan. Retirar de la ficha activa no equivale a borrar un archivo histórico. Las transacciones y comprobación de revisión abierta 2B evitan aplicar una edición obsoleta.

## Drive y relaciones

Conserva `syncProtocol=3`, `pathVersion=4`, UUID/numeración/folderId estables, staging, fileId por clave `photo/<UUID>`, ACK verificable, reintentos, conflictos y consolidación completa del servidor. No se cambian columnas Excel SIC ni longitudes del Prompt 4.

Nuevas entregas declaran `photoCategoryVersion=1`; el servidor valida categorías por familia/modelo y deriva `CATEGORIA/F_<hashUUID>.jpg`. Rechaza cualquier ruta incompatible antes de escribir. La categoría y el contexto están firmados por el hash canónico; no utiliza el nombre visible o el tipo técnico para crear carpetas. GENERAL histórico se entrega como GENERAL. No se generan SIB ni categorías vacías.

SCAP conserva PUENTE N e identidad común del puente; cada inspección tiene `SCAP/<inspectionUUID>/<CATEGORIA>/F_<hashUUID>.jpg` y su paquete individual. Reconciliar una entrega anterior mueve el mismo fileId hacia el destino de inspección bajo control de revisión; no renumera puente ni elimina evidencia. Sin validación/despliegue compatible, permanece pendiente. Metadata SIC incluye UUID de foto/registro, SIC, calzada, categoría, orden global y por categoría, revisión, clave, ruta y fileId anterior; el recibo confirmado aporta los fileIds actuales. Manifest SCAP conserva photoId/categoría/orden e inspectionId con rutas coherentes.

El cliente espera GET con protocolo 3, jerarquía 4, numeración estable, entrega sin GPS y capacidad fotográfica 1. La configuración privada sigue idéntica: el servicio de pruebas necesita el [paquete integrado y verificación real](DESPLIEGUE_PRUEBAS.md). No se publicó Apps Script ni se realizó escritura remota. Los archivos y recibos de las pruebas locales son simulados.

## Archivos principales

- `field/PhotoCategoryPolicy.kt`, `field/CulvertPhotos.kt`, `core/FieldCommonUi.kt`: política y galería compartida.
- `MainActivity.kt`, `field/EngineeringEditScreen.kt`, `field/CulvertUnifiedScreen.kt`, `sic/SicFieldWorkspace.kt`: captura y reapertura, categorías/borradores y metadatos.
- `repository/InventoryPhotoEdits.kt`, `repository/EngineeringEdits.kt`, `sic/SicFieldRepository.kt`, `scap/data/ScapRepository.kt`: UUID/fileId estables, originales e historial transaccional.
- `data/entity/PhotoHistoryEntity.kt`, `data/dao/InventoryDao.kt`, `data/database/InventoryDatabase.kt`, esquema 18: migración aditiva, sin cambio de fotos existentes.
- `core/AutomaticDelivery.kt`, `core/DriveInventorySchema.kt`, `core/AutomaticSync.kt`: metadata, rutas por categoría/inspección y puerta de capacidades.
- `camera/PhotoCaptureMetadata.kt`, `core/InventoryDraftStore.kt`, `field/CulvertEditDraft.kt`: relación de reemplazo persistente.
- `scap/ui/ScapMedia.kt`, `scap/ui/ScapPhotoRecords.kt`: importación múltiple, reemplazo y título fotográfico; sin campos técnicos nuevos.
- `google-apps-script/SyncV3Core.js`, `Code.gs`: contrato de categoría validada y GET; V3 existente reutilizado.
- Pruebas nuevas `PhotoCategories5Test`, `PhotoCategories5ScreenTest`, `PhotoCategories5.test.cjs`; fixtures sintéticas nuevas en `examples/`. Ajustes mecánicos de expectativas de versión/migraciones en fixtures históricas no implican ejecutar sus suites completas.

## Verificación y pendientes

**25 pruebas Android distintas y 19 backend locales aprobadas; assembleDebug exitoso.** Última selección Android 4/4 y backend 3/3, sin repetir las selecciones ya aprobadas. Resultados exactos, casos y SHA APK: [VALIDACION.json](VALIDACION.json). Se ejecutan exclusivamente pruebas focalizadas y `assembleDebug`. Los errores iniciales de compilación y el fallo de fixture SIC-22 se conservan localmente; no se declaran aprobados.

APK Debug: `app/build/outputs/apk/debug/app-debug.apk`, con configuración local real copiada idéntica y sin `gboValidationBuild=true`. applicationId `com.tuempresa.inventariovial`, versionCode **16** y firma histórica conservados. Certificado Debug igual al Prompt 4; no se instala. El APK/propiedades/keystores/caches/logs y resultados crudos se excluyen del commit.

Pendientes: cámara/importador real y reapertura tras reinicio Android en tablet secundaria; actualización autorizada SOLO del Apps Script aislado; verificación real de JPEG/Excel/metadata/IDs, dos inspecciones SCAP, archivos históricos, reintentos y conflictos. La migración aditiva se verifica localmente y todavía requiere prueba física con copia respaldada. Categorías históricas desconocidas se muestran y conservan localmente; el contrato nuevo puede requerir clasificación explícita antes de reenviarlas. No hay migración automática ni conciliación automática de conflictos.

Trabajo anterior de SCAP/proguard/herramientas/configuraciones y resultados permanece en las copias anteriores. Los recursos auxiliares copiados para compilar en prompt5 siguen sin commit, separados de esta entrega. No se descarta ningún cambio preexistente. Detenerse aquí; Prompt 6 no iniciado.
