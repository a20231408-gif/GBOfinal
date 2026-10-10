# Drive V2: cambio de servidor pendiente de publicación

Fecha: 08/10/2026. Fuente integrada: `google-apps-script/Code.gs`, versión anunciada `v7.0 · Integración Drive V2`. Patch reproducible contra B: `docs/DRIVE_V2_SERVER.patch`.

**El código del servidor está implementado y probado localmente. No se publicó, no se consultó el deployment de producción y no se modificaron sus Script Properties.** Las 37 pruebas Node utilizan dobles de Drive y Apps Script; no demuestran creación de carpetas en Google Drive real. `backend/user-service/` pertenece a otro servicio y permanece idéntico a A.

## Estructura implementada

```text
ROOT_FOLDER_ID/
  PE-22A/
    PUENTES/PUENTE_001/
      SIC-17.xlsx
      GENERAL/...
      REVISIONES/...
      SCAP.xlsx y medios publicados por el contrato SCAP
    ALCANTARILLAS/ALCANTARILLA_001/
      metadata.json
      SIC-18.xlsx
      PANORAMICA/...
      ENTRADA/...
      SALIDA/...
    SENALIZACION_VERTICAL/SENAL_001/
      metadata.json
      SIC-22.xlsx
      GENERAL/...
    PAVIMENTOS/ASFALTICO/MUESTRA_001/
      HC_001.pdf
      M_001.json
      PAV_ASF.xlsx
      SIC14.xlsx
      GENERAL/...
    PAVIMENTOS/RIGIDO/MUESTRA_001/...
    PAVIMENTOS/NO_PAVIMENTADO/MUESTRA_001/...
```

Familias adicionales: `CUNETAS`, `BADENES`, `SENALIZACION_HORIZONTAL`, `OTROS`. Las categorías justificadas provienen del registro; cuando no hay categoría se usa `GENERAL`. SCAP conserva su publicación y archivo de revisiones; Pavimentos conserva versiones previas cuando cambia un archivo. No se migra ni renombra automáticamente el contenido remoto anterior.

`DriveAssetPathRouter` construye componentes. `DrivePaths` guarda la asignación UUID → secuencia en preferencias antes de transmitir. Puentes SIC y SCAP usan el mismo `roadRecordId`. No se extrae identidad desde el nombre de una fotografía. El servidor congela UUID → ruta y carpeta → propietario bajo el bloqueo de subida. Una colisión de secuencias entre tablets se rechaza; debe resolverse antes de operar varias tablets sobre el mismo espacio de nombres. No hay un asignador central de secuencias en esta entrega.

## Inventario/SIC: HTTP POST JSON

Se conserva `action=uploadDeliveryFile`, `protocol=1`; los campos V2 son aditivos. Los marcadores entre `<...>` se sustituyen por datos reales. El token es el ya configurado; este documento no genera ni entrega credenciales.

```json
{
  "token": "<token existente>",
  "action": "uploadDeliveryFile",
  "protocol": 1,
  "deliveryId": "<UUID de lote>",
  "fingerprint": "<SHA-256 de snapshot revisado>",
  "kind": "photo",
  "recordId": "<UUID Room de alcantarilla>",
  "routeCode": "PE-22A",
  "sibCode": "SIB-02",
  "sicCode": "SIC-18",
  "assetFamily": "ALCANTARILLA",
  "elementFolder": "ALCANTARILLA 1",
  "photoId": "<UUID Room de fotografía>",
  "photoIndex": 1,
  "deliveryPhotoSequence": 1,
  "fileName": "001_F01.jpg",
  "mimeType": "image/jpeg",
  "pathVersion": 2,
  "logicalPath": "PE-22A/ALCANTARILLAS/ALCANTARILLA_001/ENTRADA",
  "photoPolicy": "ORIGINAL_CLEAN",
  "sha256": "<SHA-256 de los bytes transmitidos>",
  "base64": "<bytes originales codificados en base64>"
}
```

El payload añade los campos de captura disponibles producidos por `DeliveryMetadata.photoPayload`: posición, precisión y fecha de la fotografía. El JSON `metadata.json` contiene los datos del elemento y el arreglo de fotografías; no contiene tokens ni rutas privadas Android.

Para `kind=metadata`: mismos datos de lote/elemento, `fileName=metadata.json`, `mimeType=application/json`, `logicalPath=PE-22A/ALCANTARILLAS/ALCANTARILLA_001`, hash y base64 del JSON. No exige los campos exclusivos de fotografía.

Para `kind=sic` V2: mismos datos de lote/elemento, `fileName=SIC-18.xlsx`, MIME XLSX, ruta del elemento, hash y base64. Se escribe un SIC por elemento con el escritor B. Los exports agrupados de ruta anteriores siguen disponibles en Android; el servidor acepta además el payload SIC legacy sin `pathVersion` ni `elementFolder`.

ACK mínimo y extensión V2:

```json
{
  "ok": true,
  "protocol": 1,
  "deliveryId": "<mismo UUID>",
  "fileId": "<ID real Drive>",
  "folderId": "<ID real de carpeta destino>",
  "fileName": "001_F01.jpg",
  "sha256": "<mismo SHA-256>",
  "alreadyExists": false,
  "pathVersion": 2,
  "logicalPath": "PE-22A/ALCANTARILLAS/ALCANTARILLA_001/ENTRADA"
}
```

Android exige éxito, protocolo, lote, nombre, hash e IDs antes de marcar una fotografía o entrega sincronizada. Verifica la vigencia del snapshot antes de cada archivo y al terminar. Una anulación o edición interrumpe el lote. La API legacy foto-a-foto permanece desactivada con `STRICT_ELEMENT_FOLDERING=true`; el trabajador legacy tampoco acepta éxito sin hash, nombre e ID válidos. Los reintentos conservan los bytes preparados y la identidad del lote.

## Pavimentos: HTTP POST JSON

```json
{
  "token": "<token existente>",
  "action": "uploadPavementFile",
  "protocol": 1,
  "pathVersion": 2,
  "logicalPath": "PE-22A/PAVIMENTOS/ASFALTICO/MUESTRA_001/GENERAL",
  "photoPolicy": "ORIGINAL_CLEAN",
  "routeCode": "PE-22A",
  "tramo": "TRAMO_1",
  "surface": "ASPHALT",
  "sampleNo": 1,
  "sampleId": "<UUID Room de muestra>",
  "revision": "<8 dígitos hex de hash de entrega preparada>",
  "category": "PHOTO",
  "photoId": "<UUID de foto>",
  "damageId": "<UUID de daño>",
  "fileName": "001_D013.jpg",
  "mimeType": "image/jpeg",
  "sha256": "<SHA-256 de archivo>",
  "base64": "<bytes base64>"
}
```

Superficies: `ASPHALT`, `RIGID`, `UNSURFACED`. Categorías: `FIELD`, `SAMPLE_DATA`, `GROUP_DATA`, `PHOTO`, `SIC_SIC14`, `SIC_SIC14A`, `SIC_SIC15`, `SIC_SIC16`. Solo `PHOTO` lleva el sufijo `GENERAL`; documentos van en la carpeta de muestra. Nombres máximos: 20 caracteres. MIME: PDF, JSON, XLSX o JPEG según categoría.

```json
{
  "ok": true,
  "protocol": 1,
  "pavementProtocol": 1,
  "sampleId": "<mismo UUID>",
  "revision": "<misma revisión>",
  "pathVersion": 2,
  "logicalPath": "PE-22A/PAVIMENTOS/ASFALTICO/MUESTRA_001/GENERAL",
  "fileId": "<ID Drive>",
  "folderId": "<ID carpeta>",
  "fileName": "001_D013.jpg",
  "sha256": "<mismo hash>",
  "alreadyExists": false
}
```

La muestra solo queda `SYNCED` después de ACK válidos para todos sus archivos y comprobación de vigencia. Las copias y el manifiesto preparado son durables; reintentar no recalcula sus bytes. La revisión contempla los SIC generados. Un ACK V2 debe identificar muestra, revisión y ruta. Un servidor antiguo con protocolo 1 puede confirmar archivos compatibles en `EVALUACION_SUPERFICIAL`; la UI muestra esa ruta cuando no recibe confirmación V2. Los SIC nuevos pueden ser rechazados por un servidor antiguo: se conserva estado pendiente/error, nunca se declara sincronización completa.

## SCAP: ZIP y manifiesto

Se mantiene `action=uploadScap`, `protocol=2`, `recordKind=SCAP`, `scapInspectionId`, `revision`, `sha256`, `bridgeCode`, `bridgeName`, `base64`, más el `delivery` revisado del contrato B. El manifiesto ZIP añade:

```json
{
  "protocol": 2,
  "recordKind": "SCAP",
  "scapBundleVersion": 6,
  "pathVersion": 2,
  "logicalPath": "PE-3N/PUENTES/PUENTE_001",
  "roadRecordId": "<UUID Room del puente>",
  "photoPolicy": "ORIGINAL_CLEAN",
  "delivery": "<objeto de layout humano revisado B>",
  "files": "<arreglo real de paths, hashes y metadatos B>"
}
```

`delivery` y `files` son objetos/arreglos, no los textos ilustrativos mostrados aquí. El formato exacto se produce en `ScapDelivery.bundle` y se mantiene documentado en `DRIVE_SCAP_CONTRACT.md`.

ACK: `ok=true`, `protocol=2`, `recordKind=SCAP`, inspección y revisión originales, hash del ZIP, `fileId`, `folderId`, IDs del root configurado/ruta/familia/puente, `fileCount`, `files` con hashes, `path` y `logicalPath` iguales a la ruta revisada V2, `pathVersion=2`. `validAck` también admite el contrato humano anterior cuando su `path` coincide con el layout revisado legacy. Se valida la inspección COMPLETE, la revisión vigente y el registro padre no anulado; el padre técnico DRAFT de SCAP B se admite únicamente con inspección finalizada. `COMPLETE` y un ZIP local no significan sincronizado.

## Publicación y verificación pendiente

1. Revisar el diff entregado contra el código efectivo de producción. Mantener el token, root y propiedades existentes; no usar los placeholders del repositorio como configuración de producción.
2. Publicar una nueva versión del mismo Apps Script con el procedimiento habitual. Esta entrega no ejecutó ese paso.
3. Verificar por GET `deliveryProtocol=1`, `pavementProtocol=1`, `scapProtocol=2`, `scapBundleVersion=6`, `pathVersion=2` y política limpia.
4. En una carpeta de validación autorizada, enviar una alcantarilla con N fotos por categoría, un puente SIC→SCAP y SCAP→SIC, y una muestra por superficie. Confirmar los IDs y hashes remotos, no solo `ok`.
5. Repetir tras pérdida de respuesta; verificar ausencia de duplicados. Probar conflicto de propietario, token inválido y edición/anulación entre archivos. Ver el checklist tablet.

Hasta completar estos pasos, la estructura remota V2 queda **pendiente de validación y publicación**, aunque el código local y sus contratos estén integrados.
