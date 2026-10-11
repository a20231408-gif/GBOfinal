const fs = require('node:fs');
const vm = require('node:vm');
const assert = require('node:assert/strict');
const {test} = require('node:test');
const crypto = require('node:crypto');

function environment(strict = false) {
  let held = false, releases = 0, reads = 0, sequence = 0;
  const iterator = values => {let i = 0; return {hasNext: () => i < values.length, next: () => values[i++]};};
  const registry = new Map(), properties = new Map();
  function blob(bytes, mime, name) {
    return {bytes, mime, name, getBytes() {return bytes;}, getName() {return this.name;},
      getDataAsString() {return Buffer.from(bytes).toString('utf8');},
      setName(n) {this.name = n; return this;}, copyBlob() {return blob(bytes, mime, this.name);}};
  }
  function folder(name, id = String(++sequence).padStart(4, '0'), parent = null) {
    const f = {name, id, parent, folders: [], files: [], getId() {return id;}, getFiles() {return iterator(this.files);},
      getParents() {assert.ok(held); return iterator(this.parent ? [this.parent] : []);},
      getFoldersByName(name) {assert.ok(held); reads++; return iterator(this.folders.filter(f => f.name === name));},
      createFolder(name) {assert.ok(held); const child = folder(name, String(++sequence).padStart(4, '0'), this); this.folders.push(child); return child;},
      getFilesByName(name) {assert.ok(held); return iterator(this.files.filter(f => f.getName() === name));},
      createFile(blob) {assert.ok(held); if (this.fail) throw Error('Drive failed');
        const fileId = 'file-' + (++sequence);
        let description = '';
        const file = {getId: () => fileId, getName: () => blob.name, getBlob: () => blob,
          getUrl: () => 'https://drive.google.com/file/d/' + fileId, getDescription: () => description,
          setName(value) {blob.name = value; return file;},
          setDescription(value) {description = value; return file;}, moveTo(target) { assert.ok(held); for (const parent of registry.values()) if (parent.files) parent.files = parent.files.filter(f => f !== file); target.files.push(file); return file; }}; this.files.push(file); registry.set(fileId, file); return file;}
    };
    registry.set(id, f); return f;
  }
  const root = folder('root');
  const state = {busy: false, competing: null};
  const context = vm.createContext({
    DriveApp: {getFileById(id) {assert.ok(held); return registry.get(id);}, getFolderById(id) {assert.ok(held); reads++; return registry.get(id) || root;}},
    PropertiesService: {getScriptProperties: () => ({getProperty: key => properties.get(key) || null, setProperty: (key, value) => properties.set(key, value), deleteProperty: key => properties.delete(key)})},
    LockService: {getScriptLock: () => ({tryLock() {
      if (state.busy || held) return false;
      held = true;
      if (state.competing) {const fn = state.competing; state.competing = null; fn();}
      return true;
    }, releaseLock() {assert.ok(held); held = false; releases++;}})},
    Utilities: {base64Decode: text => [...Buffer.from(text, 'base64')], newBlob: blob,
      DigestAlgorithm: {SHA_256: 'sha256'}, computeDigest: (_, bytes) => [...crypto.createHash('sha256').update(typeof bytes === 'string' ? bytes : Buffer.from(bytes)).digest()],
      base64EncodeWebSafe: bytes => Buffer.from(bytes).toString('base64url'),
      unzip: b => JSON.parse(Buffer.from(b.bytes).toString()).map(f => blob([...Buffer.from(f.text)], 'application/octet-stream', f.name))},
    ContentService: {MimeType: {JSON: 'json'}, createTextOutput: text => ({setMimeType: () => JSON.parse(text)})}
  });
  let source=fs.readFileSync(__dirname + '/Code.gs', 'utf8').replace(/const API_TOKEN = "[^"]*";/, 'const API_TOKEN = "test-only";');
  if (!strict) source=source.replace('const STRICT_ELEMENT_FOLDERING = true;', 'const STRICT_ELEMENT_FOLDERING = false;');
  vm.runInContext(source, context);
  const payload = {token: 'test-only', routeCode: 'li-100', sibCode: '2', sicCode: 'SIC23', fileName: 'photo-1.jpg', base64: 'AQID'};
  const send = (changes = {}) => context.doPost({postData: {contents: JSON.stringify({...payload, ...changes})}});
  return {context, root, state, folder, send, registry, properties, get reads() {return reads;}, get held() {return held;}, get releases() {return releases;}};
}

test('several photos share route and SIB and retry does not create another file', () => {
  const e = environment(); const first = e.send(); const second = e.send({fileName: 'photo-2.jpg'}); const retry = e.send();
  assert.equal(first.ok, true); assert.equal(first.routeFolderId, second.routeFolderId); assert.equal(first.sibFolderId, second.sibFolderId);
  assert.equal(e.root.folders.length, 1); assert.equal(e.root.folders[0].folders.length, 1);
  assert.equal(e.root.folders[0].folders[0].files.length, 2); assert.equal(retry.alreadyExists, true); assert.equal(e.releases, 3);
});
test('health only advertises no-GPS technical delivery when V3 engine, Drive and enable flag exist',()=>{
  const e=environment();assert.equal(e.context.doGet().technicalDeliveryWithoutGps,false);
  vm.runInContext('var SyncV3={}; var Drive={}; function v3Enabled(){return PropertiesService.getScriptProperties().getProperty("GBO_SYNC_V3_ENABLED")==="true";}',e.context);
  assert.equal(e.context.doGet().technicalDeliveryWithoutGps,false);
  e.properties.set('GBO_SYNC_V3_ENABLED','true');const health=e.context.doGet();
  assert.equal(health.technicalDeliveryWithoutGps,true);assert.equal(health.syncProtocol,3);assert.equal(health.pathVersion,3);
  assert.equal(typeof health.rootFolderId,'string');assert.ok(health.rootFolderId);
});
test('competing request cannot reach Drive and can retry after the first finishes', () => {
  const e = environment(); let competitor;
  e.state.competing = () => {competitor = e.send({fileName: 'photo-2.jpg'}); assert.equal(e.reads, 0);};
  assert.equal(e.send().ok, true); assert.equal(competitor.ok, false); assert.equal(competitor.retryable, true);
  assert.equal(e.send({fileName: 'photo-2.jpg'}).ok, true); assert.equal(e.root.folders.length, 1);
});
test('busy lock does not release another execution lock or touch Drive', () => {
  const e = environment(); e.state.busy = true;
  assert.equal(e.send().retryable, true); assert.equal(e.reads, 0); assert.equal(e.releases, 0);
});
test('Drive failure releases lock and a later request can proceed', () => {
  const e = environment(); e.send(); const target = e.root.folders[0].folders[0]; target.fail = true;
  assert.equal(e.send({fileName: 'photo-2.jpg'}).ok, false); assert.equal(e.held, false);
  target.fail = false; assert.equal(e.send({fileName: 'photo-2.jpg'}).ok, true);
});
test('duplicate existing folders are selected consistently without deleting either', () => {
  const e = environment(); const a = e.folder('LI-100', 'a'); const z = e.folder('LI-100', 'z');
  e.root.folders.push(z, a); assert.equal(e.send().routeFolderId, 'a');
  e.root.folders.reverse(); assert.equal(e.send({fileName: 'photo-2.jpg'}).routeFolderId, 'a');
  assert.equal(e.root.folders.length, 2); assert.equal(z.files.length, 0);
});
test('unauthorized and invalid codes do not access Drive', () => {
  const e = environment();
  for (const changes of [{token: 'wrong'}, {routeCode: ' '}, {sibCode: '2garbage'}, {sicCode: 'SIC-23text'}, {sicCode: '24'}, {base64: ''}]) {
    assert.equal(e.send(changes).ok, false);
  }
  assert.equal(e.reads, 0); assert.equal(e.releases, 0);
});
test('SIC remains optional and canonical codes remain compatible with Android', () => {
  const e = environment(); const result = e.send({sicCode: null});
  assert.equal(result.ok, true); assert.equal(result.routeCode, 'LI-100'); assert.equal(result.sibCode, 'SIB-02'); assert.equal(result.sicCode, null);
});
test('malformed JSON is handled without creating folders', () => {
  const e = environment(); assert.equal(e.context.doPost({postData: {contents: '{'}}).ok, false);
  assert.equal(e.context.doPost(null).ok, false); assert.equal(e.reads, 0);
});


test('production strict mode rejects legacy loose photos before touching Drive', () => {
  const e=environment(true); const result=e.send();
  assert.equal(result.ok,false); assert.match(result.error,/PREPARAR ENTREGA/); assert.equal(e.reads,0);
});


test('pavement protocol stores field sheet, native data, group Excel and photo in the evaluation tree', () => {
  const e = environment();
  const sendPav = (category, fileName, mimeType, text='payload', changes={}) => {
    const bytes=Buffer.from(text); const payload={token:'test-only',action:'uploadPavementFile',protocol:1,
      routeCode:'PE-22A',tramo:'TRAMO 2',surface:'ASPHALT',sampleNo:1,
      sampleId:'sample-0001',revision:'00000001',category,fileName,mimeType,
      sha256:hash(bytes),base64:bytes.toString('base64'),...changes};
    return e.context.doPost({postData:{contents:JSON.stringify(payload)}});
  };
  const a=sendPav('FIELD','HC_001.pdf','application/pdf','pdf');
  const b=sendPav('SAMPLE_DATA','M_001.json','application/json','json');
  const c=sendPav('GROUP_DATA','PAV_ASF.xlsx','application/vnd.openxmlformats-officedocument.spreadsheetml.sheet','xlsx');
  const d=sendPav('PHOTO','001_D013.jpg','image/jpeg','jpeg');
  assert.deepEqual([a.ok,b.ok,c.ok,d.ok],[true,true,true,true]);
  const route=e.root.folders.find(f=>f.name==='PE-22A');
  const activity=route.folders.find(f=>f.name==='EVALUACION_SUPERFICIAL');
  const tramo=activity.folders.find(f=>f.name==='TRAMO 2');
  const surface=tramo.folders.find(f=>f.name==='ASFALTICO');
  assert.equal(surface.folders.find(f=>f.name==='07_HOJAS_CAMPO').folders[0].files[0].getName(),'HC_001.pdf');
  assert.equal(surface.folders.find(f=>f.name==='08_PANEL_FOTOGRAFICO').folders[0].files[0].getName(),'001_D013.jpg');
  assert.equal(surface.folders.find(f=>f.name==='DATA_APP').files[0].getName(),'PAV_ASF.xlsx');
  assert.equal(surface.folders.find(f=>f.name==='DATA_APP').folders[0].files[0].getName(),'M_001.json');
  assert.equal(sendPav('PHOTO','001_D013.jpg','image/jpeg','jpeg').alreadyExists,true);
});

test('pavement protocol archives a changed revision instead of silently overwriting it', () => {
  const e=environment();
  const send=(revision,text)=>{const bytes=Buffer.from(text);return e.context.doPost({postData:{contents:JSON.stringify({
    token:'test-only',action:'uploadPavementFile',protocol:1,routeCode:'PE-3N',tramo:'TRAMO 1',surface:'RIGID',
    sampleNo:2,sampleId:'sample-0002',revision,category:'FIELD',fileName:'HC_002.pdf',mimeType:'application/pdf',
    sha256:hash(bytes),base64:bytes.toString('base64')})}})};
  assert.equal(send('00000001','one').ok,true);
  assert.equal(send('00000002','two').ok,true);
  const surface=e.root.folders[0].folders[0].folders[0].folders[0];
  const field=surface.folders.find(f=>f.name==='07_HOJAS_CAMPO').folders[0];
  assert.equal(field.files.length,1);
  assert.equal(field.files[0].getName(),'HC_002.pdf');
  assert.equal(field.folders.find(f=>f.name==='REVISIONES').files.length,1);
});

const hash = bytes => crypto.createHash('sha256').update(bytes).digest('hex');
function scap(changes = {}, extra = []) {
  const p = {action: 'uploadScap', recordKind: 'SCAP', protocol: 1,
    scapInspectionId: '00000000-0000-0000-0000-000000000001', revision: 100,
    bridgeCode: 'PE-3N/P-001', bridgeName: 'Puente de prueba', ...changes};
  const entries = [{name: 'SCAP.xlsx', text: 'synthetic workbook'}, ...extra];
  const manifest = {...p, files: entries.map(f => ({path: f.name, sha256: hash(f.text)}))};
  entries.push({name: 'manifest.json', text: JSON.stringify(manifest)});
  const bytes = Buffer.from(JSON.stringify(entries)); // unzip is mocked; real ZIP tested on Android.
  return {...p, base64: bytes.toString('base64'), sha256: hash(bytes)};
}

function humanScap(number = 1, changes = {}, image = 'photo revision 1') {
  const id = changes.scapInspectionId || '00000000-0000-0000-0000-000000000001';
  const bridgeCode = changes.bridgeCode === undefined ? 'PE-3N/CONTRACT-' + id.slice(-3) : changes.bridgeCode;
  const photoId = 'photo-' + id;
  const delivery = {routeCode: 'PE-3N', sibCode: 'SIB-08', elementFolder: 'PUENTE ' + number,
    shortCode: 'P-' + number, bridgeIdentity: bridgeCode.trim() ? 'code:' + bridgeCode.trim() : 'inspection:' + id,
    photoPaths: {[photoId]: 'fotos/001_F01.jpg'}, sketchPaths: {sketch: 'croquis/CROQUIS_1.png'}};
  const p = {action: 'uploadScap', recordKind: 'SCAP', protocol: 2, scapInspectionId: id, revision: 100,
    bridgeCode, bridgeName: 'Puente sintético', routeCode: 'PE-3N', delivery, ...changes};
  const entries = [{name: 'SCAP.xlsx', text: 'workbook revision ' + p.revision},
    {name: 'fotos/001_F01.jpg', text: image}, {name: 'croquis/CROQUIS_1.png', text: 'sketch'}];
  const manifest = {...p, files: entries.map(f => ({path: f.name, sha256: hash(f.text),
    ...(f.name.startsWith('fotos/') ? {photoId} : {})}))};
  entries.push({name: 'manifest.json', text: JSON.stringify(manifest)});
  const bytes = Buffer.from(JSON.stringify(entries));
  return {...p, base64: bytes.toString('base64'), sha256: hash(bytes)};
}

test('Drive V2 SCAP publishes under the same persistent bridge folder as SIC', () => {
  const e=environment();
  const path='PE-3N/PUENTES/PUENTE_001';
  const p=humanScap(1,{pathVersion:2,logicalPath:path,roadRecordId:'bridge-record-1',photoPolicy:'ORIGINAL_CLEAN'});
  const ack=e.send(p);assert.equal(ack.ok,true);assert.equal(ack.path,path);assert.equal(ack.logicalPath,path);
  assert.equal(e.registry.get(ack.bridgeFolderId).name,'PUENTE_001');
  assert.equal(e.send(p).fileId,ack.fileId);
  const sic=e.send(delivery({routeCode:'PE-3N',sibCode:'SIB-08',sicCode:'SIC-17',assetFamily:'PUENTE',elementFolder:'PUENTE 1',recordId:'bridge-record-1',
    pathVersion:2,logicalPath:path+'/GENERAL'}));
  assert.equal(sic.ok,true);assert.equal(e.registry.get(sic.folderId).parent.getId(),ack.bridgeFolderId);
});

test('SCAP human protocol stores three bridges captured at 10/30/20 km as PUENTE 1/3/2 and retries without duplicates', () => {
  const e = environment();
  // Android derives this position order; the server preserves the reviewed mapping.
  const capture = [{km: 10, number: 1}, {km: 30, number: 3}, {km: 20, number: 2}];
  const payloads = capture.map((row, i) => humanScap(row.number, {
    scapInspectionId: '00000000-0000-0000-0000-00000000000' + (i + 1)}));
  const first = payloads.map(p => e.send(p));
  assert.deepEqual(first.map(a => a.ok), [true, true, true]);
  assert.deepEqual(first.map(a => a.path), ['PE-3N/SIB-08/PUENTE 1', 'PE-3N/SIB-08/PUENTE 3', 'PE-3N/SIB-08/PUENTE 2']);
  const count = e.registry.size;
  payloads.forEach((p, i) => {
    const retry = e.send(p); assert.equal(retry.ok, true); assert.equal(retry.alreadyExists, true);
    assert.equal(retry.fileId, first[i].fileId); assert.equal(retry.sha256, p.sha256);
    assert.equal(retry.scapInspectionId, p.scapInspectionId); assert.equal(retry.revision, p.revision);
    const bridge = e.registry.get(retry.bridgeFolderId);
    assert.deepEqual(bridge.files.map(f => f.getName()).sort(), ['SCAP.xlsx', 'SCAP.zip', 'manifest.json']);
    assert.ok(bridge.folders.some(f => f.name === 'fotos'));
  });
  assert.equal(e.registry.size, count);
  assert.deepEqual(e.root.folders.map(f => f.name), ['PE-3N']);
});

test('SCAP human protocol ignores stale bindings outside the reviewed route and repairs them', () => {
  const e = environment();
  const payload = humanScap();
  const outsider = e.folder('PUENTE 1', 'old-human-bridge');
  const stale = JSON.stringify({path: 'PE-3N/SIB-08/PUENTE 1', folderId: outsider.getId()});
  e.properties.set('scap.human.inspection.' + payload.scapInspectionId, stale);
  e.properties.set('scap.human.code.' + hash(payload.bridgeCode.trim()), stale);
  const result = e.send(payload);
  assert.equal(result.ok, true);
  assert.notEqual(result.bridgeFolderId, outsider.getId());
  assert.equal(result.path, 'PE-3N/SIB-08/PUENTE 1');
  const route = e.registry.get(result.routeFolderId), sib = e.registry.get(result.sibFolderId);
  assert.equal(route.parent.getId(), e.root.getId());
  assert.equal(sib.parent.getId(), route.getId());
  assert.equal(e.registry.get(result.bridgeFolderId).parent.getId(), sib.getId());
});

test('SCAP human protocol preserves the previous revision and its hashes when publishing and retrying a newer one', () => {
  const e = environment(), oldPayload = humanScap(), nextPayload = humanScap(1, {revision: 101}, 'photo revision 2');
  const old = e.send(oldPayload);
  const oldBytes = new Map(old.files.map(f => [f.fileId, e.registry.get(f.fileId).getBlob().getDataAsString()]));
  const next = e.send(nextPayload);
  assert.equal(next.ok, true); assert.notEqual(old.folderId, next.folderId); assert.equal(old.bridgeFolderId, next.bridgeFolderId);
  for (const [id, bytes] of oldBytes) assert.equal(e.registry.get(id).getBlob().getDataAsString(), bytes);
  const bridge = e.registry.get(next.bridgeFolderId);
  assert.equal(bridge.files.find(f => f.getName() === 'SCAP.xlsx').getBlob().getDataAsString(), 'workbook revision 101');
  const count = e.registry.size;
  assert.equal(e.send(nextPayload).fileId, next.fileId);
  assert.equal(e.send(oldPayload).fileId, old.fileId); // A late retry cannot roll the current view back.
  assert.equal(e.registry.size, count);
  assert.equal(bridge.files.find(f => f.getName() === 'SCAP.xlsx').getBlob().getDataAsString(), 'workbook revision 101');
});

test('SCAP human protocol recovers interrupted publication without duplicate files', () => {
  const e = environment(), p = humanScap();
  const first = e.send(p), bridge = e.registry.get(first.bridgeFolderId);
  const originalCreate = bridge.createFile;
  let writes = 0;
  bridge.createFile = function(blob) {if (++writes === 2) throw Error('interrupted publication'); return originalCreate.call(this, blob);};
  const next = humanScap(1, {revision: 101}, 'new pixels');
  assert.equal(e.send(next).retryable, true); assert.equal(e.held, false);
  bridge.createFile = originalCreate;
  const retry = e.send(next); assert.equal(retry.ok, true);
  assert.deepEqual(bridge.files.map(f => f.getName()).sort(), ['SCAP.xlsx', 'SCAP.zip', 'manifest.json']);
  const count = e.registry.size;
  assert.equal(e.send(next).fileId, retry.fileId); assert.equal(e.registry.size, count);
  assert.equal(e.properties.has('scap.human.publish.' + bridge.id), false);
});

test('SCAP human protocol rejects changed route/order and occupied bridge folder without mixing identities', () => {
  const e = environment(); assert.equal(e.send(humanScap()).ok, true);
  const count = e.registry.size;
  assert.equal(e.send(humanScap(2, {revision: 101})).ok, false);
  assert.equal(e.send(humanScap(1, {scapInspectionId: '00000000-0000-0000-0000-000000000009'})).ok, false);
  assert.equal(e.registry.size, count);
});

test('SCAP human protocol groups inspections by full bridgeCode while preserving both UUID revisions and v1 files', () => {
  const e = environment(), legacy = e.send(scap());
  const first = e.send(humanScap());
  const second = e.send(humanScap(1, {scapInspectionId: '00000000-0000-0000-0000-000000000009', bridgeCode: 'PE-3N/CONTRACT-001', revision: 101}));
  assert.equal(first.ok, true); assert.equal(second.ok, true);
  assert.equal(first.bridgeFolderId, second.bridgeFolderId); assert.notEqual(first.folderId, second.folderId);
  assert.equal(e.send(scap()).fileId, legacy.fileId);
  assert.equal(e.registry.get(legacy.fileId).getBlob().getBytes().length > 0, true);
});

test('SCAP human protocol rejects a changed hash for the same inspection revision', () => {
  const e = environment(); const p = humanScap(); assert.equal(e.send(p).ok, true);
  const count = e.registry.size;
  assert.equal(e.send(humanScap(1, {}, 'changed bytes with same revision')).ok, false);
  assert.equal(e.registry.size, count);
  assert.equal(e.send(p).ok, true);
});
test('supplied v3 SIC Excel and ZIP exports retain content validation and idempotency', () => {
  const e = environment(); const p = {action: 'uploadSicExport', fileName: 'SIC_2026.xlsx'};
  const first = e.send(p); assert.equal(first.ok, true); assert.ok(first.fileUrl);
  assert.equal(e.send(p).alreadyExists, true);
  assert.equal(e.send({...p, base64: 'BAUG'}).ok, false);
  assert.equal(e.send({...p, fileName: 'SIC_2026.zip'}).ok, true);
  assert.equal(e.root.folders[0].name, 'EXPORTACIONES_SIC');
});
test('SCAP stores its workbook, separate photos and ZIP; retry confirms the same file', () => {
  const e = environment(), p = scap({}, [{name: 'fotos/00000000-0000-0000-0000-000000000002.jpg', text: 'image'}]);
  const first = e.send(p), retry = e.send(p);
  assert.equal(first.ok, true); assert.equal(first.sha256, p.sha256); assert.equal(first.revision, 100);
  assert.equal(first.fileId, retry.fileId); assert.equal(retry.alreadyExists, true); assert.equal(first.fileCount, 3);
  assert.equal(e.root.folders[0].name, 'SCAP'); assert.equal(e.registry.get(first.folderId).files.length, 3);
  assert.equal(e.registry.get(first.folderId).folders[0].files.length, 1);
  assert.equal(e.send().ok, true); assert.equal(e.root.folders[1].name, 'LI-100');
});
test('bridge identity uses the full code, groups inspections and never groups only by name', () => {
  const e = environment(); const first = e.send(scap());
  const second = e.send(scap({scapInspectionId: '00000000-0000-0000-0000-000000000002'}));
  assert.equal(first.bridgeFolderId, second.bridgeFolderId); assert.notEqual(first.folderId, second.folderId);
  const third = e.send(scap({scapInspectionId: '00000000-0000-0000-0000-000000000003', bridgeCode: 'PE-3N-P-001'}));
  assert.notEqual(first.bridgeFolderId, third.bridgeFolderId);
});
test('legacy SCAP ignores a stale bridge mapping outside the current SCAP root', () => {
  const e = environment();
  const outsider = e.folder('old-bridge', 'old-bridge');
  e.properties.set('scap.bridge.' + hash('PE-3N/P-001'), outsider.getId());
  const result = e.send(scap());
  assert.equal(result.ok, true);
  assert.notEqual(result.bridgeFolderId, outsider.getId());
  assert.equal(e.root.folders[0].name, 'SCAP');
  assert.equal(e.registry.get(result.bridgeFolderId).parent.getId(), e.root.folders[0].getId());
  assert.equal(result.configuredRootFolderId, '1u7II1gvhSIHBwVxFjnx6VsAqa3O_jTIi');
  assert.equal(result.scapRootFolderId, e.root.folders[0].getId());
});
test('missing code uses inspection UUID; later code retains the folder and can group future inspections', () => {
  const e = environment(); const first = e.send(scap({bridgeCode: ''}));
  const second = e.send(scap({revision: 101}));
  assert.equal(first.bridgeFolderId, second.bridgeFolderId);
  const third = e.send(scap({scapInspectionId: '00000000-0000-0000-0000-000000000002'}));
  assert.equal(second.bridgeFolderId, third.bridgeFolderId);
});
test('conflicting existing identity requires review instead of merging or moving files', () => {
  const e = environment(); e.send(scap());
  const id = '00000000-0000-0000-0000-000000000002'; e.send(scap({scapInspectionId: id, bridgeCode: ''}));
  const conflict = e.send(scap({scapInspectionId: id, revision: 101}));
  assert.equal(conflict.ok, false); assert.equal(conflict.retryable, false); assert.equal(e.held, false);
});
test('SCAP rejects unauthorized requests, hash mismatches, unsafe entries and old protocol before touching Drive', () => {
  const e = environment(); const valid = scap();
  for (const p of [{...valid, token: 'bad'}, {...valid, protocol: 0}, {...valid, sha256: '0'.repeat(64)},
    scap({}, [{name: '../foreign.png', text: 'x'}]), {...valid, action: 'photo'}]) assert.equal(e.send(p).ok, false);
  assert.equal(e.reads, 0);
});
test('SCAP lock contention and partial Drive failures retry without duplicate completed files', () => {
  const e = environment(), p = scap(); e.state.busy = true;
  assert.equal(e.send(p).retryable, true); assert.equal(e.reads, 0); e.state.busy = false;
  const first = e.send(p); const target = e.registry.get(first.folderId);
  target.files.pop(); target.fail = true;
  assert.equal(e.send(p).ok, false); assert.equal(e.held, false);
  target.fail = false; assert.equal(e.send(p).ok, true); assert.equal(target.files.length, 3);
});


function delivery(changes = {}, bytes = Buffer.from('synthetic jpg bytes')) {
  return {action: 'uploadDeliveryFile', protocol: 1, deliveryId: 'delivery-1', fingerprint: 'f'.repeat(64),
    kind: 'photo', routeCode: 'PE-22A', sibCode: 'SIB-02', sicCode: 'SIC-18', recordId: 'record-A',
    photoId: 'photo-A1', photoIndex: 1, deliveryPhotoSequence: 1, elementFolder: 'ALCANTARILLA 1',
    shortCode: 'ALC-1', assetFamily: 'ALCANTARILLA', fileName: '001_F01.jpg', mimeType: 'image/jpeg',
    photoLatitude: -12.1, photoLongitude: -77.1, photoGpsAccuracyM: .8,
    confirmedPrCode: '0015', confirmedDistanceM: 200, suggestedPrCode: '0010', suggestedDistanceM: 2650,
    axisMeasureM: 12650, base64: bytes.toString('base64'), sha256: hash(bytes), ...changes};
}

test('Drive V2 keeps multiple clean culvert photos in explicit categories and freezes persistent element identity', () => {
  const e=environment();
  const base='PE-22A/ALCANTARILLAS/ALCANTARILLA_001';
  const a=e.send(delivery({pathVersion:2,logicalPath:base+'/PANORAMICA',photoPolicy:'ORIGINAL_CLEAN'}));
  assert.equal(a.ok,true);assert.equal(a.logicalPath,base+'/PANORAMICA');assert.equal(a.pathVersion,2);
  const b=e.send(delivery({pathVersion:2,logicalPath:base+'/ENTRADA',photoId:'photo-A2',photoIndex:2,deliveryPhotoSequence:2,fileName:'002_F02.jpg'}));
  assert.equal(b.ok,true);assert.notEqual(a.folderId,b.folderId);
  assert.equal(e.send(delivery({pathVersion:2,logicalPath:base+'/PANORAMICA'})).fileId,a.fileId);
  const changed=e.send(delivery({pathVersion:2,logicalPath:'PE-22A/ALCANTARILLAS/ALCANTARILLA_002/PANORAMICA'}));
  assert.equal(changed.ok,false);
  const owner=e.send(delivery({pathVersion:2,logicalPath:base+'/GENERAL',recordId:'record-B',photoId:'photo-B1'}));
  assert.equal(owner.ok,false);
  assert.equal(e.registry.get(a.folderId).files[0].getBlob().getDataAsString(),'synthetic jpg bytes');
});

test('Drive V2 pavement has a stable sample folder, ACK identity, and explicit SIC export category', () => {
  const e=environment();const bytes=Buffer.from('native synthetic workbook');
  const p={action:'uploadPavementFile',protocol:1,pathVersion:2,logicalPath:'PE-22A/PAVIMENTOS/RIGIDO/MUESTRA_001',
    routeCode:'PE-22A',tramo:'TRAMO 1',surface:'RIGID',sampleNo:8,sampleId:'sample-id-1',revision:'00000001',
    category:'SIC_SIC15',fileName:'SIC15.xlsx',mimeType:'application/vnd.openxmlformats-officedocument.spreadsheetml.sheet',sha256:hash(bytes),base64:bytes.toString('base64')};
  const ack=e.send(p);assert.equal(ack.ok,true);assert.equal(ack.sampleId,p.sampleId);assert.equal(ack.logicalPath,p.logicalPath);
  assert.equal(e.send(p).fileId,ack.fileId);
  assert.equal(e.registry.get(ack.folderId).name,'MUESTRA_001');
});
function deliveryMetadata(changes = {}) {
  const metadata = {recordId: 'record-A', routeCode: 'PE-22A', sibCode: 'SIB-02',
    location: {confirmedPr: '0015', confirmedDistance: 200, locationSource: 'MANUAL_OVERRIDE'},
    suggestion: {suggestedPr: '0010', suggestedDistance: 2650},
    photos: [{photoId: 'photo-A1', photoIndex: 1, deliveryPhotoSequence: 1, fileName: '001_F01.jpg', sha256: hash('synthetic jpg bytes')}], ...changes};
  const bytes = Buffer.from(JSON.stringify(metadata));
  return delivery({kind: 'metadata', fileName: 'metadata.json', mimeType: 'application/json'}, bytes);
}
test('delivery protocol stores multiple photos, two elements, metadata and SIC in final route/SIB structure', () => {
  const e = environment();
  assert.equal(e.context.doGet().deliveryProtocol, 1);
  const a = e.send(delivery()); assert.equal(a.ok, true);
  const a2 = e.send(delivery({photoId: 'photo-A2', photoIndex: 2, deliveryPhotoSequence: 2, fileName: '002_F02.jpg'}));
  const b = e.send(delivery({recordId: 'record-B', photoId: 'photo-B1', elementFolder: 'ALCANTARILLA 2', deliveryPhotoSequence: 3, fileName: '003_F01.jpg'}));
  assert.equal(a2.folderId, a.folderId); assert.notEqual(b.folderId, a.folderId);
  const m = e.send(deliveryMetadata()); assert.equal(m.ok, true); assert.equal(m.folderId, a.folderId);
  const x = e.send(delivery({kind: 'sic', elementFolder: null, fileName: 'SIC-18.xlsx', mimeType: 'application/vnd.openxmlformats-officedocument.spreadsheetml.sheet'}));
  assert.equal(x.ok, true);
  const route = e.root.folders[0], sib = route.folders[0];
  assert.equal(route.name, 'PE-22A'); assert.equal(sib.name, 'SIB-02');
  assert.deepEqual(sib.folders.map(f => f.name), ['ALCANTARILLA 1', 'ALCANTARILLA 2']);
  assert.deepEqual(sib.files.map(f => f.getName()), ['SIC-18.xlsx']);
  assert.deepEqual(sib.folders[0].files.map(f => f.getName()), ['001_F01.jpg', '002_F02.jpg', 'metadata.json']);
});
test('delivery retry is idempotent by photo identity and hash, including partial failure', () => {
  const e = environment(), first = e.send(delivery());
  const retry = e.send(delivery());
  assert.equal(retry.fileId, first.fileId); assert.equal(retry.sha256, first.sha256); assert.equal(retry.alreadyExists, true);
  const target = e.registry.get(first.folderId); target.fail = true;
  const second = delivery({photoId: 'photo-A2', fileName: '002_F02.jpg', photoIndex: 2, deliveryPhotoSequence: 2});
  assert.equal(e.send(second).retryable, true); assert.equal(e.held, false);
  target.fail = false; assert.equal(e.send(second).ok, true); assert.equal(e.send(second).alreadyExists, true);
  assert.equal(target.files.length, 2);
});
test('delivery rejects token, traversal, arbitrary paths, unsupported protocol and bad hashes before Drive access', () => {
  const e = environment();
  for (const invalid of [{token: 'wrong'}, {routeCode: '../PE-22A'}, {sibCode: '../../SIB-02'},
    {elementFolder: '../ALCANTARILLA 1'}, {elementFolder: 'A/B 1'}, {fileName: '../001_F01.jpg'},
    {recordId: '../record'}, {photoId: '../photo'}, {protocol: 0}, {sha256: '0'.repeat(64)},
    {fileName: '123456789012345678901.jpg'}]) assert.equal(e.send(delivery(invalid)).ok, false);
  assert.equal(e.reads, 0); assert.equal(e.releases, 0);
});
test('delivery metadata cannot include secrets or private Android paths', () => {
  const e = environment();
  for (const extra of [{token: 'private'}, {photo: {originalPath: '/data/private/photo.jpg'}},
    {recordId: 'another-record'}, {extra: 'content://private-photo'}]) assert.equal(e.send(deliveryMetadata(extra)).ok, false);
  assert.equal(e.reads, 0);
  const valid = deliveryMetadata(); assert.equal(e.send(valid).ok, true);
  assert.equal(e.send(valid).alreadyExists, true);
});
test('delivery refuses changed content, changed order or reused names without renaming or overwriting', () => {
  const e = environment(), first = e.send(delivery());
  for (const request of [delivery({elementFolder: 'ALCANTARILLA 2'}), delivery({fingerprint: 'b'.repeat(64)}),
    delivery({photoId: 'another-photo'}), delivery({}, Buffer.from('changed image'))]) {
    const result = e.send(request); assert.equal(result.ok, false); assert.equal(result.retryable, false);
  }
  assert.equal(e.registry.get(first.folderId).files.length, 1);
  assert.equal(e.root.folders[0].folders[0].folders.length, 1);
});
test('delivery lock contention retries and legacy SIC/SCAP endpoints still work alongside a delivery', () => {
  const e = environment(); e.state.busy = true;
  assert.equal(e.send(delivery()).retryable, true); assert.equal(e.reads, 0); e.state.busy = false;
  assert.equal(e.send(delivery()).ok, true);
  assert.equal(e.send().ok, true);
  assert.equal(e.send({action: 'uploadSicExport', fileName: 'SIC_legacy.xlsx'}).ok, true);
  assert.equal(e.send(scap()).ok, true);
});


test('Drive V2 stores SIC workbooks under each persistent element while legacy route exports remain compatible', () => {
  const e=environment();
  const a=e.send(delivery({kind:'sic',pathVersion:2,logicalPath:'PE-22A/ALCANTARILLAS/ALCANTARILLA_001',fileName:'SIC-18.xlsx',mimeType:'application/vnd.openxmlformats-officedocument.spreadsheetml.sheet'}));
  const b=e.send(delivery({kind:'sic',pathVersion:2,logicalPath:'PE-22A/ALCANTARILLAS/ALCANTARILLA_002',recordId:'record-B',elementFolder:'ALCANTARILLA 2',fileName:'SIC-18.xlsx',mimeType:'application/vnd.openxmlformats-officedocument.spreadsheetml.sheet'}));
  assert.equal(a.ok,true);assert.equal(b.ok,true);assert.notEqual(a.folderId,b.folderId);
  assert.equal(e.registry.get(a.folderId).name,'ALCANTARILLA_001');assert.equal(a.pathVersion,2);
  assert.equal(e.send(delivery({kind:'sic',pathVersion:2,logicalPath:'PE-22A/ALCANTARILLAS/ALCANTARILLA_001',fileName:'SIC-18.xlsx',mimeType:'application/vnd.openxmlformats-officedocument.spreadsheetml.sheet'})).fileId,a.fileId);
});


test('Drive V2 lets SCAP join a bridge folder that already contains its SIC workbook', () => {
  const e=environment();const path='PE-3N/PUENTES/PUENTE_001';
  const sic=e.send(delivery({kind:'sic',routeCode:'PE-3N',sibCode:'SIB-08',sicCode:'SIC-17',assetFamily:'PUENTE',elementFolder:'PUENTE 1',recordId:'bridge-record-1',pathVersion:2,logicalPath:path,fileName:'SIC-17.xlsx',mimeType:'application/vnd.openxmlformats-officedocument.spreadsheetml.sheet'}));
  assert.equal(sic.ok,true);
  const ack=e.send(humanScap(1,{pathVersion:2,logicalPath:path,roadRecordId:'bridge-record-1',photoPolicy:'ORIGINAL_CLEAN'}));
  assert.equal(ack.ok,true);assert.equal(ack.bridgeFolderId,sic.folderId);
  assert.ok(e.registry.get(sic.folderId).files.some(f=>f.getName()==='SIC-17.xlsx'));
});

test('Drive V2 accepts the new SIC families without changing ACK or idempotency', () => {
  const e=environment();
  for(const [code,family,prefix] of [['SIC-08','CALZADA_FAJAS','FAJA'],['SIC-09','CALZADA_FAJAS','FAJA'],['SIC-13','PUNTOS_CRITICOS','PUNTO_CRITICO'],['SIC-34','ACCIDENTES','ACCIDENTE']]) {
    const p=delivery({kind:'sic',sicCode:code,recordId:code,pathVersion:2,logicalPath:`PE-22A/${family}/${prefix}_${code==='SIC-09'?'002':'001'}`,fileName:`${code}.xlsx`,mimeType:'application/vnd.openxmlformats-officedocument.spreadsheetml.sheet'});
    const ack=e.send(p);assert.equal(ack.ok,true,ack.error);assert.equal(ack.protocol,1);assert.equal(ack.sha256,p.sha256);
    assert.equal(e.send(p).fileId,ack.fileId);
  }
});

test('SIC-18A shares its parent element UUID and folder with SIC-18', () => {
  const e=environment();const common={kind:'sic',recordId:'parent',pathVersion:2,logicalPath:'PE-22A/ALCANTARILLAS/ALCANTARILLA_001',mimeType:'application/vnd.openxmlformats-officedocument.spreadsheetml.sheet'};
  const parent=e.send(delivery({...common,sicCode:'SIC-18',fileName:'SIC-18.xlsx'}));
  const extra=e.send(delivery({...common,sicCode:'SIC-18A',fileName:'SIC-18A.xlsx'}));
  assert.equal(parent.ok,true);assert.equal(extra.ok,true);assert.equal(parent.folderId,extra.folderId);
  assert.deepEqual(e.registry.get(parent.folderId).files.map(f=>f.getName()),['SIC-18.xlsx','SIC-18A.xlsx']);
});
