// ============================================================
// CONFIGURACIÓN
//
// Fotos: CARPETA RAÍZ / RUTA / SIB / FOTO
// Excel: CARPETA RAÍZ / EXPORTACIONES_SIC / ARCHIVO
// ============================================================

const ROOT_FOLDER_ID = "1u7II1gvhSIHBwVxFjnx6VsAqa3O_jTIi";

const API_TOKEN = "PON_AQUI_TU_TOKEN_ACTUAL";

// La entrega contractual no admite fotos sueltas directamente bajo RUTA/SIB.
const STRICT_ELEMENT_FOLDERING = true;


// ============================================================
// COMPROBAR QUE LA API FUNCIONA
// ============================================================

function doGet() {
  return jsonResponse({
    ok: true,
    message: "Inventario Vial API funcionando",
    version: "v7.0 · Integración Drive V2", scapProtocol: 2, scapBundleVersion: 6, pathVersion: 2, photoPolicy: "ORIGINAL_CLEAN", deliveryProtocol: 1, pavementProtocol: 1, strictElementFoldering: STRICT_ELEMENT_FOLDERING
  });
}


// ============================================================
// RECIBIR FOTOGRAFÍA O EXPORTACIÓN SIC
// ============================================================

function doPost(e) {
  let lock = null;
  let acquired = false;

  try {
    // 1. Validar solicitud.
    if (!e || !e.postData || !e.postData.contents) {
      return jsonResponse({
        ok: false,
        error: "Solicitud vacía"
      });
    }

    const data = JSON.parse(e.postData.contents);

    if (
      !data ||
      typeof data !== "object" ||
      Array.isArray(data)
    ) {
      return jsonResponse({
        ok: false,
        error: "Solicitud inválida"
      });
    }

    // 2. Validar token para cualquier tipo de subida.
    if (
      !API_TOKEN ||
      API_TOKEN === "PON_AQUI_TU_TOKEN_ACTUAL" ||
      data.token !== API_TOKEN
    ) {
      return jsonResponse({
        ok: false,
        error: "No autorizado"
      });
    }

    // SCAP tiene un contrato propio; nunca se deriva a las carpetas SIC/SIB.
    if (data.recordKind === "SCAP" || data.action === "uploadScap") {
      return uploadScap(data);
    }

    // 3. Derivar Excel y ZIP ANTES de exigir campos de fotos.
    if (data.action === "uploadSicExport") {
      return uploadSicExport(data);
    }

    if (data.action === "uploadDeliveryFile") return uploadDeliveryFile(data);

    if (data.action === "uploadPavementFile") return uploadPavementFile(data);

    // El flujo final obliga a usar PREPARAR ENTREGA para que cada foto termine
    // en RUTA/SIB/ELEMENTO N. El endpoint legacy se conserva solo desactivable
    // para compatibilidad de pruebas/migraciones.
    if (STRICT_ELEMENT_FOLDERING) return jsonResponse({
      ok:false, retryable:false,
      error:"Subida foto-a-foto deshabilitada. Usa PREPARAR ENTREGA para guardar RUTA/SIB/ELEMENTO N/FOTO."
    });

    // Flujo legacy, solo si STRICT_ELEMENT_FOLDERING=false.

    // 4. Validar campos obligatorios de las fotografías.
    const requiredFields = [
      "routeCode",
      "sibCode",
      "fileName",
      "base64"
    ];

    for (const field of requiredFields) {
      if (
        data[field] === undefined ||
        data[field] === null ||
        String(data[field]).trim() === ""
      ) {
        return jsonResponse({
          ok: false,
          error: "Falta " + field
        });
      }
    }

    if (
      typeof data.fileName !== "string" ||
      typeof data.base64 !== "string"
    ) {
      return jsonResponse({
        ok: false,
        error: "fileName y base64 deben ser texto"
      });
    }

    // 5. Normalizar ruta y códigos.
    const routeCode = normalizeFolderName(data.routeCode);
    const sibCode = normalizeSibCode(data.sibCode);

    let sicCode = null;

    if (
      data.sicCode !== undefined &&
      data.sicCode !== null &&
      String(data.sicCode).trim() !== ""
    ) {
      sicCode = normalizeSicCode(data.sicCode);
    }

    if (!/^SIB-\d{2}$/.test(sibCode)) {
      return jsonResponse({
        ok: false,
        error: "sibCode inválido: " + sibCode
      });
    }

    if (sicCode !== null && !isValidSicCode(sicCode)) {
      return jsonResponse({
        ok: false,
        error: "sicCode inválido: " + sicCode
      });
    }

    // 6. Preparar la imagen antes de tomar el bloqueo.
    const bytes = Utilities.base64Decode(data.base64);

    if (!bytes.length) {
      return jsonResponse({
        ok: false,
        error: "Imagen vacía"
      });
    }

    const blob = Utilities.newBlob(
      bytes,
      data.mimeType || "image/jpeg",
      data.fileName
    );

    // 7. Bloqueo compartido.
    // Evita crear carpetas y archivos duplicados por reintentos.
    lock = LockService.getScriptLock();
    acquired = lock.tryLock(30000);

    if (!acquired) {
      return jsonResponse({
        ok: false,
        retryable: true,
        error: "Servidor ocupado. Reintentar subida."
      });
    }

    // 8. Buscar o crear carpetas dentro del bloqueo.
    const rootFolder = DriveApp.getFolderById(ROOT_FOLDER_ID);

    const routeFolder = getOrCreateFolder(
      rootFolder,
      routeCode
    );

    const sibFolder = getOrCreateFolder(
      routeFolder,
      sibCode
    );

    // 9. Comprobar si esta foto ya se había subido.
    const existingFiles = sibFolder.getFilesByName(
      data.fileName
    );

    const alreadyExists = existingFiles.hasNext();

    const file = alreadyExists
      ? existingFiles.next()
      : sibFolder.createFile(blob);

    // 10. Responder a Android.
    return jsonResponse({
      ok: true,
      alreadyExists: alreadyExists,
      fileId: file.getId(),
      fileName: file.getName(),
      routeCode: routeCode,
      sibCode: sibCode,
      sicCode: sicCode,
      routeFolderId: routeFolder.getId(),
      sibFolderId: sibFolder.getId()
    });

  } catch (error) {
    return jsonResponse({
      ok: false,
      error: String(error)
    });

  } finally {
    if (acquired) {
      lock.releaseLock();
    }
  }
}


// ============================================================
// RECIBIR EXPORTACIÓN SIC: EXCEL O ZIP
//
// Se llama desde doPost DESPUÉS de validar el token.
// No requiere routeCode ni sibCode.
// ============================================================

function uploadSicExport(data) {
  let lock = null;
  let acquired = false;

  try {
    // 1. Validar nombre.
    const fileName =
      typeof data.fileName === "string"
        ? data.fileName.trim()
        : "";

    if (
      !fileName ||
      fileName.length > 180 ||
      /[\\/\x00-\x1F]/.test(fileName)
    ) {
      throw new Error("Nombre de archivo inválido");
    }

    if (!/^SIC[-_].+\.(xlsx|zip)$/i.test(fileName)) {
      throw new Error(
        "Se requiere un archivo que empiece por SIC- o SIC_ " +
        "y termine en .xlsx o .zip"
      );
    }

    // 2. Validar contenido.
    if (
      typeof data.base64 !== "string" ||
      !data.base64.trim()
    ) {
      throw new Error("Falta el contenido del archivo");
    }

    // Límite inicial elegido para esta solución.
    // No representa un límite oficial de Google.
    const maxBytes = 10 * 1024 * 1024;

    if (data.base64.length > 4 * Math.ceil(maxBytes / 3)) {
      throw new Error("El archivo supera el límite de 10 MB");
    }

    const bytes = Utilities.base64Decode(data.base64);

    if (!bytes.length || bytes.length > maxBytes) {
      throw new Error("Archivo vacío o mayor de 10 MB");
    }

    // 3. Preparar archivo sin convertirlo a Google Sheets.
    const mimeType = /\.zip$/i.test(fileName)
      ? "application/zip"
      : "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet";

    const blob = Utilities.newBlob(
      bytes,
      mimeType,
      fileName
    );

    // 4. Calcular huella del contenido.
    const digest = Utilities.computeDigest(
      Utilities.DigestAlgorithm.SHA_256,
      bytes
    );

    const hash = Utilities.base64EncodeWebSafe(digest);
    const marker = "sic-export-sha256:" + hash;

    // 5. Tomar el mismo bloqueo que utilizan las fotos.
    lock = LockService.getScriptLock();
    acquired = lock.tryLock(30000);

    if (!acquired) {
      return jsonResponse({
        ok: false,
        retryable: true,
        error: "Servidor ocupado. Reintentar subida."
      });
    }

    // 6. Buscar o crear carpeta de exportaciones.
    const root = DriveApp.getFolderById(ROOT_FOLDER_ID);

    const folder = getOrCreateFolder(
      root,
      "EXPORTACIONES_SIC"
    );

    // 7. Detectar reintentos y conflictos de nombre.
    const existing = folder.getFilesByName(fileName);

    let file;
    let alreadyExists = false;

    if (existing.hasNext()) {
      file = existing.next();

      if (existing.hasNext()) {
        throw new Error(
          "Existen varios archivos con ese nombre"
        );
      }

      // Comparar el contenido real permite recuperar incluso
      // una subida cuya respuesta se perdió.
      const previousHash = Utilities.base64EncodeWebSafe(
        Utilities.computeDigest(
          Utilities.DigestAlgorithm.SHA_256,
          file.getBlob().getBytes()
        )
      );

      if (previousHash !== hash) {
        throw new Error(
          "Ya existe otro contenido con ese nombre. " +
          "Genera una nueva versión con otro nombre."
        );
      }

      alreadyExists = true;

    } else {
      file = folder.createFile(blob);
      file.setDescription(marker);
    }

    // 8. Responder con el enlace del archivo.
    return jsonResponse({
      ok: true,
      alreadyExists: alreadyExists,
      fileId: file.getId(),
      fileName: file.getName(),
      fileUrl: file.getUrl(),
      folderId: folder.getId()
    });

  } catch (error) {
    return jsonResponse({
      ok: false,
      error: String(error)
    });

  } finally {
    if (acquired) {
      lock.releaseLock();
    }
  }
}


// ============================================================
// BUSCAR O CREAR CARPETA
// Solo llamar mientras se tiene el bloqueo.
// ============================================================

function getOrCreateFolder(parentFolder, folderName) {
  const folders = parentFolder.getFoldersByName(folderName);

  let selected = null;

  while (folders.hasNext()) {
    const candidate = folders.next();

    // Si existen carpetas duplicadas, elegir siempre
    // la de menor ID. No elimina ni mueve las demás.
    if (
      !selected ||
      candidate.getId() < selected.getId()
    ) {
      selected = candidate;
    }
  }

  return selected || parentFolder.createFolder(folderName);
}


// ============================================================
// NORMALIZAR NOMBRE DE RUTA
// ============================================================

function normalizeFolderName(value) {
  return String(value)
    .trim()
    .toUpperCase()
    .replace(/\//g, "-")
    .replace(/\\/g, "-");
}


// ============================================================
// NORMALIZAR CÓDIGOS
// Acepta: SIB-02, SIB02, 02, 2; SIC-23, SIC23, 23.
// Rechaza códigos malformados como SIC-23texto.
// ============================================================

function normalizeCode(value, prefix) {
  const text = String(value).trim().toUpperCase();

  const pattern = new RegExp(
    "^(?:" + prefix + "\\s*-?\\s*)?(\\d{1,2})$"
  );

  const match = text.match(pattern);

  if (!match) {
    return text;
  }

  return prefix + "-" + match[1].padStart(2, "0");
}


// SCAP v1: ZIP autocontenido (Excel + fotos marcadas + croquis + manifiesto).
// Conserva versiones. ACK solo después de guardar todos los archivos.
function uploadScap(data) {
  let lock = null, acquired = false;
  let stage = "validate-contract";
  const reject = (error, retryable = false) => jsonResponse({ok: false, retryable, stage, error});
  try {
    const uuid = /^[a-f0-9]{8}-[a-f0-9]{4}-[a-f0-9]{4}-[a-f0-9]{4}-[a-f0-9]{12}$/i;
    if (data.action !== "uploadScap" || data.recordKind !== "SCAP" || ![1, 2].includes(data.protocol) ||
        !uuid.test(data.scapInspectionId) || !Number.isSafeInteger(data.revision) || data.revision < 1 ||
        typeof data.bridgeCode !== "string" || typeof data.bridgeName !== "string" ||
        typeof data.sha256 !== "string" || !/^[a-f0-9]{64}$/.test(data.sha256) ||
        typeof data.base64 !== "string" || !data.base64.length) return reject("Contrato SCAP inválido");

    stage = "decode-base64";
    let bytes;
    try { bytes = Utilities.base64Decode(data.base64); }
    catch (_) { return reject("Base64 SCAP inválido"); }

    stage = "verify-zip-hash";
    if (scapHash(bytes) !== data.sha256) return reject("Huella SCAP incorrecta");
    const name = data.protocol === 2 ? "SCAP.zip" : "SCAP_" + data.scapInspectionId + "_" + data.revision + ".zip";
    const zip = Utilities.newBlob(bytes, "application/zip", name);

    stage = "unzip";
    let entries;
    try { entries = Utilities.unzip(zip); }
    catch (_) { return reject("ZIP SCAP inválido"); }

    stage = "validate-entries";
    const names = entries.map(b => b.getName());
    const allowedV1 = /^(SCAP\.xlsx|manifest\.json|fotos\/[a-f0-9-]+\.jpg|croquis\/[a-f0-9-]+\.png)$/i;
    const allowedV2 = /^(SCAP\.xlsx|manifest\.json|fotos\/[0-9]+_F[0-9]+\.jpg|croquis\/CROQUIS_[0-9]+\.png)$/i;
    const allowed = data.protocol === 2 ? allowedV2 : allowedV1;
    if (new Set(names).size !== names.length || !names.includes("SCAP.xlsx") || !names.includes("manifest.json") ||
        names.some(n => !allowed.test(n))) return reject("Contenido SCAP inválido");

    stage = "validate-manifest";
    const byPath = new Map(entries.map(b => [b.getName(), b]));
    let manifest;
    try { manifest = JSON.parse(byPath.get("manifest.json").getDataAsString("UTF-8")); }
    catch (_) { return reject("Manifiesto SCAP inválido"); }
    if (!manifest || manifest.recordKind !== "SCAP" || manifest.protocol !== data.protocol ||
        manifest.scapInspectionId !== data.scapInspectionId || manifest.revision !== data.revision ||
        manifest.bridgeCode !== data.bridgeCode || !Array.isArray(manifest.files) ||
        manifest.files.some(f => !f || typeof f.path !== "string" || typeof f.sha256 !== "string" || !/^[a-f0-9]{64}$/.test(f.sha256)) ||
        manifest.files.length !== entries.length - 1 || new Set(manifest.files.map(f => f.path)).size !== manifest.files.length ||
        manifest.files.some(f => f.path === "manifest.json" || !names.includes(f.path) ||
          scapHash(byPath.get(f.path).getBytes()) !== f.sha256)) return reject("Archivos SCAP no coinciden con el manifiesto");

    const hashes = new Map(manifest.files.map(f => [f.path, f.sha256]));
    hashes.set("manifest.json", scapHash(byPath.get("manifest.json").getBytes()));

    stage = "acquire-lock";
    lock = LockService.getScriptLock(); acquired = lock.tryLock(30000);
    if (!acquired) return reject("Servidor ocupado. Reintentar subida.", true);

    if (data.protocol === 2) {
      stage = "store-human-scap";
      return storeHumanScap(data, manifest, entries, zip, hashes);
    }

    stage = "resolve-folders";
    const root = getOrCreateFolder(DriveApp.getFolderById(ROOT_FOLDER_ID), "SCAP");
    const properties = PropertiesService.getScriptProperties();
    const inspectionKey = "scap.inspection." + data.scapInspectionId;
    const code = data.bridgeCode.trim();
    const codeKey = code ? "scap.bridge." + scapHash(code) : null;
    const previous = properties.getProperty(inspectionKey);
    const byCode = codeKey ? properties.getProperty(codeKey) : null;

    // ScriptProperties puede conservar IDs de pruebas antiguas o de carpetas movidas.
    // Solo se reutiliza una asociación si la carpeta sigue dentro del SCAP actual.
    const previousFolder = scapChildFolderById(root, previous);
    const codeFolder = scapChildFolderById(root, byCode);
    if (previousFolder && codeFolder && previousFolder.getId() !== codeFolder.getId())
      return reject("Código de puente asociado a otra carpeta. Revisar asociación SCAP.");

    const bridge = previousFolder || codeFolder ||
      getOrCreateFolder(root, "PUENTE_" + scapHash(code ? "code:" + code : "inspection:" + data.scapInspectionId));
    properties.setProperty(inspectionKey, bridge.getId());
    if (codeKey) properties.setProperty(codeKey, bridge.getId());
    const inspection = getOrCreateFolder(bridge, data.scapInspectionId);
    const version = getOrCreateFolder(inspection, String(data.revision) + "_" + data.sha256);

    stage = "save-files";
    const savedFiles = [];
    for (const blob of entries) {
      const path = blob.getName().split("/");
      const folder = path.length === 2 ? getOrCreateFolder(version, path[0]) : version;
      const expected = hashes.get(blob.getName());
      const saved = scapSaveExact(folder, blob.copyBlob().setName(path[path.length - 1]), expected);
      savedFiles.push({path: blob.getName(), fileId: saved.file.getId(), folderId: folder.getId()});
    }
    stage = "save-zip";
    const result = scapSaveExact(version, zip, data.sha256);
    stage = "ack";
    return jsonResponse({ok: true, protocol: 1, recordKind: "SCAP", scapInspectionId: data.scapInspectionId,
      revision: data.revision, sha256: data.sha256, fileId: result.file.getId(), fileName: name,
      fileUrl: result.file.getUrl(), folderId: version.getId(), bridgeFolderId: bridge.getId(),
      scapRootFolderId: root.getId(), configuredRootFolderId: ROOT_FOLDER_ID,
      alreadyExists: result.alreadyExists, fileCount: entries.length, files: savedFiles});
  } catch (error) {
    const conflict = error && error.scapConflict === true;
    console.error("SCAP failure", stage, conflict ? "integrity-conflict" : "operation-failed");
    return reject(conflict ? "Conflicto de integridad SCAP. Revisar archivos de la revisión." :
      "No se pudo completar SCAP en Drive. Reintentar.", !conflict);
  } finally { if (acquired) lock.releaseLock(); }
}

function scapHash(value) {
  return Utilities.computeDigest(Utilities.DigestAlgorithm.SHA_256, value)
    .map(b => (b & 255).toString(16).padStart(2, "0")).join("");
}

// Devuelve la carpeta solo si continúa siendo hija directa del padre esperado.
// Evita que IDs antiguos guardados en ScriptProperties envíen archivos a otra ruta.
function scapChildFolderById(parentFolder, folderId) {
  if (!folderId) return null;
  try {
    const folder = DriveApp.getFolderById(folderId);
    const parents = folder.getParents();
    while (parents.hasNext()) {
      if (parents.next().getId() === parentFolder.getId()) return folder;
    }
  } catch (_) {
    // Carpeta eliminada, movida, sin acceso o ID antiguo.
  }
  return null;
}

// SCAP v2 changes presentation only. Immutable revisions and existing v1 folders remain intact.
function storeHumanScap(data, manifest, entries, zip, hashes) {
  const fail = message => jsonResponse({ok: false, retryable: false, stage: "store-human-scap", error: message});
  const d = data.delivery;
  const expectedIdentity = data.bridgeCode.trim() ? "code:" + data.bridgeCode.trim() : "inspection:" + data.scapInspectionId;
  if (!d || !/^PE-(3N|22A|28C|12A|3NG|28H)$/.test(d.routeCode) || d.sibCode !== "SIB-08" ||
      !/^PUENTE [1-9][0-9]*$/.test(d.elementFolder) || d.shortCode !== "P-" + d.elementFolder.substring(7) ||
      d.bridgeIdentity !== expectedIdentity || JSON.stringify(d) !== JSON.stringify(manifest.delivery) ||
      manifest.routeCode !== d.routeCode || !d.photoPaths || !d.sketchPaths) return fail("Ubicación de entrega SCAP inválida.");

  const planned = Object.values(d.photoPaths).concat(Object.values(d.sketchPaths));
  const media = entries.map(b => b.getName()).filter(n => n !== "SCAP.xlsx" && n !== "manifest.json");
  if (new Set(planned).size !== planned.length || planned.length !== media.length ||
      planned.some(n => typeof n !== "string" || !media.includes(n) || n.split("/").pop().length > 20) ||
      Object.keys(d.photoPaths).some(id => !manifest.files.some(f => f.path === d.photoPaths[id] && f.photoId === id)))
    return fail("Los nombres de fotos no corresponden al manifiesto SCAP.");

  const properties = PropertiesService.getScriptProperties();
  const inspectionKey = "scap.human.inspection." + (manifest.pathVersion===2 ? "v2." : "") + data.scapInspectionId;
  const revisionKey = "scap.human.revision." + data.scapInspectionId + "." + data.revision;
  const revisionHash = properties.getProperty(revisionKey);
  if (revisionHash && revisionHash !== data.sha256)
    return fail("La revisión SCAP ya está asociada a otro contenido. Guarda una nueva revisión.");
  const codeKey = data.bridgeCode.trim() ? "scap.human.code." + (manifest.pathVersion===2 ? "v2." : "") + scapHash(data.bridgeCode.trim()) : null;
  const previous = properties.getProperty(inspectionKey);
  const byCode = codeKey && properties.getProperty(codeKey);
  const context = manifest.pathVersion===2 ? manifest.logicalPath : d.routeCode + "/" + d.sibCode + "/" + d.elementFolder;

  // La entrega visible debe resolverse siempre desde el ROOT_FOLDER_ID configurado.
  // Si una asociación guardada apunta a una carpeta antigua o movida, se ignora y repara.
  const configuredRoot = DriveApp.getFolderById(ROOT_FOLDER_ID);
  const route = getOrCreateFolder(configuredRoot, d.routeCode);
  const sib = getOrCreateFolder(route, manifest.pathVersion===2 ? "PUENTES" : d.sibCode);

  function validBinding(raw) {
    if (!raw) return null;
    try {
      const saved = JSON.parse(raw);
      if (!saved || typeof saved.path !== "string" || typeof saved.folderId !== "string") return null;
      const folder = scapChildFolderById(sib, saved.folderId);
      return folder ? {saved, folder} : null;
    } catch (_) {
      return null;
    }
  }

  const previousBinding = validBinding(previous);
  const codeBinding = validBinding(byCode);
  if (previousBinding && codeBinding && previousBinding.folder.getId() !== codeBinding.folder.getId())
    return fail("Código e inspección corresponden a puentes distintos.");

  for (const binding of [previousBinding, codeBinding]) {
    if (binding && binding.saved.path !== context)
      return fail("El orden o la ruta del puente cambió. Revisa la entrega antes de renombrar carpetas.");
  }

  let bridge = previousBinding ? previousBinding.folder : (codeBinding ? codeBinding.folder : null);
  if (!bridge) {
    bridge = manifest.pathVersion===2 ? driveV2Folder(manifest.logicalPath,manifest.roadRecordId,d.routeCode) : getOrCreateFolder(sib, d.elementFolder);
    const ownerKey = "scap.human.owner." + bridge.getId();
    const owner = properties.getProperty(ownerKey);
    if ((owner && owner !== expectedIdentity) || (manifest.pathVersion!==2 && properties.getProperty("delivery_folder_owner_" + bridge.getId())) ||
        (manifest.pathVersion!==2 && !owner && bridge.getFiles().hasNext())) return fail("La carpeta visible pertenece a otro elemento o entrega.");
    properties.setProperty(ownerKey, expectedIdentity);
  }

  const binding = JSON.stringify({path: context, folderId: bridge.getId()});
  properties.setProperty(inspectionKey, binding);
  if (codeKey) properties.setProperty(codeKey, binding);
  properties.setProperty(revisionKey, data.sha256);

  const currentKey = "scap.human.current." + bridge.getId();
  const pendingKey = "scap.human.publish." + bridge.getId();
  const pending = properties.getProperty(pendingKey);
  if (pending) publishHumanScap(bridge, JSON.parse(pending), properties, currentKey, pendingKey);

  const revisions = getOrCreateFolder(bridge, "REVISIONES");
  const versionName = data.revision + "_" + data.scapInspectionId + "_" + data.sha256;
  const version = getOrCreateFolder(revisions, versionName);
  const savedFiles = [];
  for (const blob of entries) {
    const path = blob.getName().split("/");
    const folder = path.length === 2 ? getOrCreateFolder(version, path[0]) : version;
    const expected = hashes.get(blob.getName());
    const file = scapSaveExact(folder, blob.copyBlob().setName(path[path.length - 1]), expected).file;
    savedFiles.push({path: blob.getName(), fileId: file.getId(), folderId: folder.getId(), sha256: expected});
  }
  const packed = scapSaveExact(version, zip, data.sha256);
  const current = JSON.parse(properties.getProperty(currentKey) || "null");
  const order = data.revision.toString().padStart(16, "0") + ":" + data.scapInspectionId;
  if (!current || (order >= current.order && current.versionFolderId !== version.getId())) {
    const publication = {order, versionFolderId: version.getId(), previous: current,
      files: savedFiles.concat([{path: "SCAP.zip", fileId: packed.file.getId(), folderId: version.getId(), sha256: data.sha256}])};
    properties.setProperty(pendingKey, JSON.stringify(publication));
    publishHumanScap(bridge, publication, properties, currentKey, pendingKey);
  }
  return jsonResponse({ok: true, protocol: 2, recordKind: "SCAP", scapInspectionId: data.scapInspectionId,
    revision: data.revision, sha256: data.sha256, fileId: packed.file.getId(), folderId: version.getId(),
    configuredRootFolderId: ROOT_FOLDER_ID, routeFolderId: route.getId(), sibFolderId: sib.getId(),
    bridgeFolderId: bridge.getId(), path: context, pathVersion:manifest.pathVersion||1, logicalPath:manifest.logicalPath||null, alreadyExists: packed.alreadyExists, fileCount: entries.length, files: savedFiles});
}

function publishHumanScap(bridge, publication, properties, currentKey, pendingKey) {
  if (publication.previous) {
    const previous = publication.previous;
    const archive = getOrCreateFolder(DriveApp.getFolderById(previous.versionFolderId), "VISTA_PUBLICADA");
    for (const entry of previous.files) {
      const parts = entry.path.split("/");
      const folder = parts.length === 2 ? getOrCreateFolder(archive, parts[0]) : archive;
      DriveApp.getFileById(entry.fileId).moveTo(folder);
    }
  }
  const currentFiles = [];
  for (const entry of publication.files) {
    const parts = entry.path.split("/");
    const folder = parts.length === 2 ? getOrCreateFolder(bridge, parts[0]) : bridge;
    const source = DriveApp.getFileById(entry.fileId);
    const hash = entry.sha256 || scapHash(source.getBlob().getBytes());
    const file = scapSaveExact(folder, source.getBlob().copyBlob().setName(parts[parts.length - 1]), hash).file;
    currentFiles.push({path: entry.path, fileId: file.getId(), folderId: folder.getId(), sha256: hash});
  }
  properties.setProperty(currentKey, JSON.stringify({order: publication.order, versionFolderId: publication.versionFolderId, files: currentFiles}));
  properties.deleteProperty(pendingKey);
}

function scapSaveExact(folder, blob, expectedHash) {
  const hash = expectedHash || scapHash(blob.getBytes());
  const marker = "scap-sha256:" + hash;
  function conflict() { const error = Error("Conflicto SCAP"); error.scapConflict = true; throw error; }
  if (!/^[a-f0-9]{64}$/.test(hash)) conflict();
  const existing = folder.getFilesByName(blob.getName());
  if (existing.hasNext()) {
    const file = existing.next();
    if (existing.hasNext()) conflict();
    const currentMarker = typeof file.getDescription === "function" ? file.getDescription() : "";
    if (currentMarker !== marker) {
      if (scapHash(file.getBlob().getBytes()) !== hash) conflict();
      if (typeof file.setDescription === "function") file.setDescription(marker);
    }
    return {file: file, alreadyExists: true};
  }
  const file = folder.createFile(blob);
  if (typeof file.setDescription === "function") file.setDescription(marker);
  return {file, alreadyExists: false};
}

function normalizeSibCode(value) {
  return normalizeCode(value, "SIB");
}


function normalizeSicCode(value) {
  return normalizeCode(value, "SIC");
}


// ============================================================
// VALIDAR SIC
// ============================================================

function isValidSicCode(value) {
  return [
    "SIC-17",
    "SIC-18",
    "SIC-19",
    "SIC-20",
    "SIC-21",
    "SIC-22",
    "SIC-23", "SIC-08", "SIC-09", "SIC-13", "SIC-34", "SIC-18A"
  ].includes(value);
}


// ============================================================
// RESPUESTA JSON
// ============================================================

function jsonResponse(data) {
  return ContentService
    .createTextOutput(JSON.stringify(data))
    .setMimeType(ContentService.MimeType.JSON);
}


// Reviewed route deliveries use the existing root, token and lock. Legacy SIC and SCAP stay independent.
function deliveryComponent(value) {
  return typeof value === "string" && value.length > 0 && value.length <= 120 && value === value.trim() &&
    !/[\\/:\x00-\x1f]/.test(value) && !value.includes("..");
}

// ============================================================
// EVALUACIÓN SUPERFICIAL DE PAVIMENTOS
// RUTA / EVALUACION_SUPERFICIAL / TRAMO / SUPERFICIE /
//   07_HOJAS_CAMPO / MUESTRA N
//   08_PANEL_FOTO / MUESTRA N
//   DATA_APP / MUESTRA N
// ============================================================
function pavementComponent(value, maxLen) {
  return typeof value === "string" && value.trim().length > 0 && value.trim().length <= (maxLen || 80) &&
    !/[\\/\x00-\x1F]/.test(value) && value !== "." && value !== "..";
}
function pavementSurfaceFolder(surface) {
  if (surface === "ASPHALT") return "ASFALTICO";
  if (surface === "RIGID") return "RIGIDO";
  if (surface === "UNSURFACED") return "NO_PAVIMENTADO";
  return null;
}
function pavementArchiveExisting(target, fileName) {
  const it = target.getFilesByName(fileName);
  if (!it.hasNext()) return;
  const revisions = getOrCreateFolder(target, "REVISIONES");
  while (it.hasNext()) {
    const old = it.next();
    const dot = fileName.lastIndexOf(".");
    const stem = dot > 0 ? fileName.substring(0, dot) : fileName;
    const ext = dot > 0 ? fileName.substring(dot) : "";
    old.setName((stem + "_R" + new Date().getTime() + ext).substring(0, 180));
    old.moveTo(revisions);
  }
}
function driveV2Parts(path, routeCode) {
  const parts = typeof path === "string" ? path.split("/") : [];
  const route = String(routeCode).trim().normalize("NFD").replace(/[\u0300-\u036f]/g, "").toUpperCase().replace(/\s+/g,"_").replace(/[^A-Z0-9_-]+/g,"_").replace(/^_+|_+$/g,"");
  if(parts.length<3 || parts.length>5 || parts[0]!==route || parts.some(p=>!p || p.length>80 || !/^[A-Z0-9_-]+$/.test(p))) throw new Error("Ruta lógica inválida");
  if(!["PUENTES","ALCANTARILLAS","CUNETAS","BADENES","OTROS","SENALIZACION_VERTICAL","SENALIZACION_HORIZONTAL","PAVIMENTOS","CALZADA_FAJAS","PUNTOS_CRITICOS","ACCIDENTES"].includes(parts[1])) throw new Error("Familia inválida");
  const pavement=parts[1]==="PAVIMENTOS";
  const elementIndex=pavement?3:2;
  if(pavement && !["ASFALTICO","RIGIDO","NO_PAVIMENTADO"].includes(parts[2])) throw new Error("Superficie inválida");
  if(!/^[A-Z_]+_[0-9]{3,}$/.test(parts[elementIndex]||"")) throw new Error("Identidad de elemento inválida");
  return {parts,elementIndex};
}
// Called under the existing upload lock. Freeze the element path by persistent Room ID.
function driveV2Folder(path, identity, routeCode) {
  if(typeof identity!=="string" || !identity.length || identity.length>100) throw new Error("ID persistente ausente");
  const {parts,elementIndex}=driveV2Parts(path,routeCode);
  const props=PropertiesService.getScriptProperties();
  const base=parts.slice(0,elementIndex+1).join("/");
  const key="drive.v2.identity."+scapHash(ROOT_FOLDER_ID+":"+parts[0]+":"+parts[1]+":"+identity);
  const saved=props.getProperty(key);
  if(saved && saved!==base) throw new Error("La identidad ya tiene otra ruta; no se renombra");
  let folder=DriveApp.getFolderById(ROOT_FOLDER_ID);
  for(let i=0;i<parts.length;i++) {
    folder=getOrCreateFolder(folder,parts[i]);
    if(i===elementIndex) {
      const ownerKey="drive.v2.owner."+folder.getId();
      const owner=props.getProperty(ownerKey);
      if(owner && owner!==identity) throw new Error("Carpeta de otro elemento");
      props.setProperty(ownerKey,identity);props.setProperty(key,base);
    }
  }
  return folder;
}
function uploadPavementFile(data) {
  const reject = (message, retryable) => jsonResponse({ok:false, retryable:!!retryable, error:message});
  if (data.protocol !== 1 || !pavementComponent(data.routeCode, 20) || !pavementComponent(data.tramo, 80) ||
      !pavementSurfaceFolder(data.surface) || !Number.isInteger(data.sampleNo) || data.sampleNo < 1 ||
      !deliveryId(data.sampleId) || !/^[a-f0-9]{8}$/.test(data.revision || "") ||
      !["FIELD","SAMPLE_DATA","GROUP_DATA","PHOTO","SIC_SIC14","SIC_SIC14A","SIC_SIC15","SIC_SIC16"].includes(data.category) ||
      !pavementComponent(data.fileName, 20) || !/^[a-f0-9]{64}$/.test(data.sha256 || "") ||
      typeof data.base64 !== "string" || typeof data.mimeType !== "string")
    return reject("Entrega de pavimentos inválida o nombre no permitido.", false);

  if (data.category === "PHOTO" && (data.mimeType !== "image/jpeg" || !/\.jpg$/i.test(data.fileName)))
    return reject("La fotografía de pavimentos debe ser JPG.", false);
  if (data.category === "FIELD" && (data.mimeType !== "application/pdf" || !/\.pdf$/i.test(data.fileName)))
    return reject("La hoja de campo debe ser PDF.", false);
  if (data.category === "SAMPLE_DATA" && (data.mimeType !== "application/json" || !/\.json$/i.test(data.fileName)))
    return reject("La data nativa debe ser JSON.", false);
  if ((data.category === "GROUP_DATA" || data.category.startsWith("SIC_")) && (!/\.xlsx$/i.test(data.fileName)))
    return reject("La data consolidada debe ser XLSX.", false);

  let bytes, blob;
  try {
    bytes = Utilities.base64Decode(data.base64);
    if (!bytes.length || scapHash(bytes) !== data.sha256) return reject("Hash/contenido de pavimentos inválido.", false);
    blob = Utilities.newBlob(bytes, data.mimeType, data.fileName);
  } catch (_) { return reject("No se pudo decodificar el archivo de pavimentos.", false); }

  if(data.pathVersion!==undefined) {
    try { if(data.pathVersion!==2) throw new Error();driveV2Parts(data.logicalPath,data.routeCode); }
    catch(_) { return reject("Ruta lógica inválida",false); }
  }
  const lock = LockService.getScriptLock(); let acquired=false;
  try {
    acquired = lock.tryLock(30000);
    if (!acquired) return reject("Servidor ocupado. Reintentar pavimentos.", true);
    const root = DriveApp.getFolderById(ROOT_FOLDER_ID);
    const route = getOrCreateFolder(root, normalizeFolderName(data.routeCode));
    let target;
    if(data.pathVersion===2) target=driveV2Folder(data.logicalPath,data.sampleId,data.routeCode);
    else {
    const activity = getOrCreateFolder(route, "EVALUACION_SUPERFICIAL");
    const tramo = getOrCreateFolder(activity, normalizeFolderName(data.tramo));
    const surface = getOrCreateFolder(tramo, pavementSurfaceFolder(data.surface));

    if (data.category === "FIELD") target = getOrCreateFolder(getOrCreateFolder(surface,"07_HOJAS_CAMPO"), "MUESTRA " + data.sampleNo);
    else if (data.category === "PHOTO") target = getOrCreateFolder(getOrCreateFolder(surface,"08_PANEL_FOTOGRAFICO"), "MUESTRA " + data.sampleNo);
    else if (data.category === "SAMPLE_DATA") target = getOrCreateFolder(getOrCreateFolder(surface,"DATA_APP"), "MUESTRA " + data.sampleNo);
    else target = getOrCreateFolder(surface,"DATA_APP");
    }

    const props = PropertiesService.getScriptProperties();
    const logical = [ROOT_FOLDER_ID,data.routeCode,data.tramo,data.surface,data.category,data.sampleId,data.fileName].join(":");
    const key = "pavement_file_" + scapHash(logical);
    const oldReceipt = props.getProperty(key);
    if (oldReceipt) {
      const old = JSON.parse(oldReceipt);
      if (old.sha256 === data.sha256 && old.folderId === target.getId() && old.fileName === data.fileName) {
        try {
          const existing = DriveApp.getFileById(old.fileId);
          return jsonResponse({ok:true, protocol:1, pavementProtocol:1,sampleId:data.sampleId,revision:data.revision,pathVersion:data.pathVersion||1,logicalPath:data.logicalPath||null, alreadyExists:true, fileId:existing.getId(), folderId:target.getId(), fileName:data.fileName, sha256:data.sha256});
        } catch (_) { /* Archivo eliminado: se recrea. */ }
      }
      pavementArchiveExisting(target, data.fileName);
    } else {
      // También protege frente a archivos creados manualmente o por una versión previa.
      const byName = target.getFilesByName(data.fileName);
      if (byName.hasNext()) {
        const f = byName.next();
        if (scapHash(f.getBlob().getBytes()) === data.sha256 && !byName.hasNext()) {
          const receipt={ok:true,protocol:1,pavementProtocol:1,sampleId:data.sampleId,revision:data.revision,pathVersion:data.pathVersion||1,logicalPath:data.logicalPath||null,alreadyExists:true,fileId:f.getId(),folderId:target.getId(),fileName:data.fileName,sha256:data.sha256};
          props.setProperty(key,JSON.stringify(receipt)); return jsonResponse(receipt);
        }
        pavementArchiveExisting(target, data.fileName);
      }
    }
    const file = target.createFile(blob);
    if (typeof file.setDescription === "function") file.setDescription("pavement-sha256:" + data.sha256 + "; sample:" + data.sampleId + "; revision:" + data.revision);
    const receipt={ok:true,protocol:1,pavementProtocol:1,sampleId:data.sampleId,revision:data.revision,pathVersion:data.pathVersion||1,logicalPath:data.logicalPath||null,alreadyExists:false,fileId:file.getId(),folderId:target.getId(),fileName:data.fileName,sha256:data.sha256};
    props.setProperty(key,JSON.stringify(receipt));
    return jsonResponse(receipt);
  } catch (error) {
    console.error("Pavement Drive failure", String(error));
    return reject("No se completó la entrega de pavimentos en Drive. Reintentar.", true);
  } finally { if (acquired) lock.releaseLock(); }
}

function deliveryId(value) { return typeof value === "string" && /^[A-Za-z0-9_-]{1,80}$/.test(value); }
function deliveryMetadataValid(meta, data) {
  if (!meta || meta.recordId !== data.recordId || meta.routeCode !== data.routeCode || meta.sibCode !== data.sibCode || !Array.isArray(meta.photos)) return false;
  function clean(value) {
    if (Array.isArray(value)) return value.every(clean);
    if (value && typeof value === "object") return Object.keys(value).every(key =>
      !/(token|password|secret|credential|localpath|originalpath|stampedpath|privatepath)/i.test(key) && clean(value[key]));
    return typeof value !== "string" || !/^(?:\/data\/|file:\/\/|content:\/\/)/i.test(value);
  }
  return clean(meta) && meta.photos.every(p => deliveryId(p.photoId) && Number.isInteger(p.photoIndex) && p.photoIndex > 0 &&
    Number.isInteger(p.deliveryPhotoSequence) && p.deliveryPhotoSequence > 0 && deliveryComponent(p.fileName) &&
    p.fileName.length <= 20 && /^[a-f0-9]{64}$/.test(p.sha256));
}
function uploadDeliveryFile(data) {
  const invalid = () => jsonResponse({ok: false, retryable: false, error: "Entrega inválida o nombre no permitido."});
  if (data.protocol !== 1 || !deliveryId(data.deliveryId) || !/^[a-f0-9]{64}$/.test(data.fingerprint || "") ||
      !deliveryComponent(data.routeCode) || !/^[A-Z0-9][A-Z0-9._ -]*$/.test(data.routeCode) ||
      !/^SIB-\d{2}$/.test(data.sibCode || "") || !isValidSicCode(data.sicCode) ||
      !deliveryComponent(data.fileName) || data.fileName.length > 20 ||
      !/^[a-f0-9]{64}$/.test(data.sha256 || "") || typeof data.base64 !== "string") return invalid();
  if(data.pathVersion!==undefined) {
    try { if(data.pathVersion!==2) throw new Error();driveV2Parts(data.logicalPath,data.routeCode); }
    catch(_) { return invalid(); }
  }
  const kind = data.kind;
  if (!["photo", "metadata", "sic"].includes(kind)) return invalid();
  if ((kind !== "sic" || data.pathVersion===2) && (!deliveryId(data.recordId) || !deliveryComponent(data.elementFolder) ||
      !/^[A-ZÁÉÍÓÚÜÑ ]+ [1-9][0-9]*$/.test(data.elementFolder))) return invalid();
  if (kind === "photo" && (!deliveryId(data.photoId) || !Number.isInteger(data.photoIndex) || data.photoIndex < 1 ||
      !Number.isInteger(data.deliveryPhotoSequence) || data.deliveryPhotoSequence < 1 || data.mimeType !== "image/jpeg" || !/\.jpg$/.test(data.fileName))) return invalid();
  if (kind === "metadata" && (data.fileName !== "metadata.json" || data.mimeType !== "application/json")) return invalid();
  if (kind === "sic" && (data.fileName !== data.sicCode + ".xlsx" ||
      data.mimeType !== "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet" || (data.pathVersion!==2 && data.elementFolder != null))) return invalid();
  let bytes, blob;
  try {
    bytes = Utilities.base64Decode(data.base64);
    if (!bytes.length || scapHash(bytes) !== data.sha256) return invalid();
    blob = Utilities.newBlob(bytes, data.mimeType, data.fileName);
    if (kind === "metadata" && !deliveryMetadataValid(JSON.parse(blob.getDataAsString()), data)) return invalid();
  } catch (_) { return invalid(); }
  const lock = LockService.getScriptLock();
  let acquired = false;
  try {
    acquired = lock.tryLock(30000);
    if (!acquired) return jsonResponse({ok: false, retryable: true, error: "Servidor ocupado. Reintentar entrega."});
    const properties = PropertiesService.getScriptProperties();
    // Congelar la huella por lote, no por ruta. La ruta puede recibir entregas posteriores
    // a medida que avanza el levantamiento sin quedar bloqueada por el primer fingerprint.
    const batchKey = "delivery_batch_" + scapHash(ROOT_FOLDER_ID + ":" + data.deliveryId);
    const frozen = properties.getProperty(batchKey);
    if (frozen && frozen !== data.fingerprint) return jsonResponse({ok: false, retryable: false, error: "El mismo lote llegó con otra huella; vuelve a preparar la entrega."});
    properties.setProperty(batchKey, data.fingerprint);
    const root = DriveApp.getFolderById(ROOT_FOLDER_ID);
    const route = getOrCreateFolder(root, data.routeCode);
    const sib = data.pathVersion===2 ? null : getOrCreateFolder(route, data.sibCode);
    let target = sib;
    if(data.pathVersion===2) target=driveV2Folder(data.logicalPath,data.recordId,data.routeCode);
    else if (kind !== "sic") {
      const folderKey = "delivery_record_" + scapHash(ROOT_FOLDER_ID + ":" + data.routeCode + ":" + data.recordId);
      const savedFolder = properties.getProperty(folderKey);
      const identity = data.sibCode + "/" + data.elementFolder;
      if (savedFolder) {
        const saved = JSON.parse(savedFolder);
        if (saved.identity !== identity) return jsonResponse({ok: false, retryable: false, error: "Conflicto de orden definitivo; no se renombra el elemento."});
        target = DriveApp.getFolderById(saved.id);
      } else {
        target = getOrCreateFolder(sib, data.elementFolder);
        const ownerKey = "delivery_folder_owner_" + target.getId();
        const owner = properties.getProperty(ownerKey);
        if (owner && owner !== data.recordId) return jsonResponse({ok: false, retryable: false, error: "La carpeta pertenece a otro elemento."});
        properties.setProperty(ownerKey, data.recordId);
        properties.setProperty(folderKey, JSON.stringify({id: target.getId(), identity}));
      }
    }
    const logicalId = kind === "photo" ? "photo:" + data.photoId : kind === "metadata" ? "metadata:" + data.recordId : "sic:" + (data.pathVersion===2?data.recordId:data.sibCode) + ":" + data.sicCode;
    const nameKey = "delivery_name_" + scapHash(target.getId() + ":" + data.fileName);
    const owner = properties.getProperty(nameKey);
    if (owner && owner !== logicalId) return jsonResponse({ok: false, retryable: false, error: "El nombre pertenece a otro archivo lógico."});
    properties.setProperty(nameKey, logicalId);
    const fileKey = "delivery_file_" + scapHash(ROOT_FOLDER_ID + ":" + data.routeCode + ":" + logicalId);
    const old = properties.getProperty(fileKey);
    if (old) {
      const saved = JSON.parse(old);
      if (saved.sha256 !== data.sha256 || saved.folderId !== target.getId() || saved.fileName !== data.fileName)
        return jsonResponse({ok: false, retryable: false, error: "Conflicto de identidad o hash; no se sobrescribe."});
    }
    const saved = scapSaveExact(target, blob);
    const receipt = {ok: true, protocol: 1, deliveryId: data.deliveryId, fileId: saved.file.getId(),
      folderId: target.getId(), pathVersion:data.pathVersion||1, logicalPath:data.logicalPath||null, fileName: data.fileName, sha256: data.sha256, alreadyExists: saved.alreadyExists};
    properties.setProperty(fileKey, JSON.stringify(receipt));
    return jsonResponse(receipt);
  } catch (_) {
    return jsonResponse({ok: false, retryable: true, error: "No se completó la entrega en Drive. Reintentar."});
  } finally { if (acquired) lock.releaseLock(); }
}
