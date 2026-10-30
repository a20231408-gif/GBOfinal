const {test}=require('node:test'),assert=require('node:assert/strict');
const fs=require('node:fs'),path=require('node:path'),vm=require('node:vm');
const {env,fixture,plain,hash,root,reference}=require('./Harness.cjs');
test('all twenty official categories agree with Android assets, short names, numbered folders and safe extensions',()=>{
  const e=env(),catalog=plain(e.context.ScapPhotoPanelV2.categories);
  assert.deepEqual(catalog,JSON.parse(fs.readFileSync(path.join(__dirname,'../CATALOGO_OFICIAL.json'),'utf8')));
  for(const type of ['slab','modular'])assert.deepEqual(catalog.map(c=>c.label),JSON.parse(fs.readFileSync(path.join(root,'app/src/main/assets/scap_'+type+'_catalog.json'),'utf8')).photoNames);
  assert.equal(catalog.length,20);assert.equal(new Set(catalog.map(c=>c.folder)).size,20);
  for(const c of catalog)for(const ext of ['jpg','mp4','webm','3gp']){const name=e.context.ScapPhotoPanelV2.fileName(c.key,7,1,ext);assert.ok(name.startsWith(String(c.order).padStart(2,'0')+'_PUE7_'));assert.ok(name.length<=80);assert.match(name,/^[A-Z0-9_]+\.(jpg|mp4|webm|3gp)$/);}
});
test('V20 adapter commits real JPEG fixtures with official folders; a lost reply repeats the same ACK/file IDs without duplication',()=>{
  const e=env(),{payload:p}=fixture(e),a=plain(e.send(p));assert.equal(a.ok,true,JSON.stringify(a));
  assert.equal(a.files.length,5);assert.ok(a.files.every(f=>f.logicalPath.startsWith(a.logicalPath+'/SCAP/'+p.recordId+'/')));
  for(const f of a.files){assert.equal(f.requestedPath,p.files.find(x=>x.key===f.key).path);assert.equal(f.sha256,hash(Buffer.from(e.objects.get(f.fileId).getBlob().getBytes())));}
  const size=e.objects.size,b=plain(e.send(p));assert.equal(b.alreadyExists,true);assert.equal(b.folderId,a.folderId);assert.equal(b.revision,a.revision);assert.equal(b.contentHash,a.contentHash);assert.deepEqual(b.files,a.files);assert.equal(e.objects.size,size);
});
test('two inspection clients keep separate UUID ownership and share the server stable bridge folder',()=>{
  const e=env(),one=fixture(e,[1,3],'FICTICIO_TABLET_A'),two=fixture(e,[8,15,20],'FICTICIO_TABLET_B');
  const a=plain(e.send(one.payload)),b=plain(e.send(two.payload));assert.equal(a.ok,true);assert.equal(b.ok,true);assert.equal(a.folderId,b.folderId);
  assert.equal(new Set([...a.files,...b.files].map(f=>f.fileId)).size,5);assert.ok(a.files.every(f=>f.logicalPath.includes('/FICTICIO_TABLET_A/')));assert.ok(b.files.every(f=>f.logicalPath.includes('/FICTICIO_TABLET_B/')));
});
test('BUSY writes nothing and the exact pending request commits after releasing contention',()=>{
  const e=env(),{payload:p}=fixture(e);const count=e.objects.size;e.state.busy=true;
  const busy=e.send(p);assert.equal(busy.code,'BUSY');assert.equal(busy.retryable,true);assert.equal(e.objects.size,count);
  e.state.busy=false;assert.equal(e.send(p).ok,true);assert.equal(e.send(p).alreadyExists,true);
});
test('category/path contradictions and cross-family new categories are rejected before a transaction',()=>{
  const e=env(),{payload:p}=fixture(e,[1]);const count=e.objects.size;
  p.files[0].path=p.files[0].path.replace('01_VISTA_PANORAMICA_LADO_DERECHO','02_VISTA_PANORAMICA_LADO_IZQUIERDO');
  assert.equal(e.send(p).code,'PHOTO_PATH_CATEGORY_MISMATCH');assert.equal(e.objects.size,count);
  const q=fixture(e,[1],'FICTICIO_OTHER').payload;q.kind='SIC';q.key='SIC/'+q.recordId;q.identityId=q.recordId;
  assert.throws(()=>e.context.SyncV3.photoPath(q,q.files[0],hash,q.files[0].path.split('/').pop()),/INVALID_PHOTO_CATEGORY/);
  // The adapter rejects even earlier: this forged identity has no name reservation.
  assert.equal(e.send(q).code,'PHOTO_NAME_RESERVATION_REQUIRED');
});
test('replacement recovery preserves revision, immutable reservations, UUID and stable file identity',()=>{
  const e=env(),{payload:p}=fixture(e,[1]);const a=plain(e.send(p));assert.equal(a.ok,true);
  const q=structuredClone(p);q.baseRevision=p.revision;q.revision=p.revision+1;
  const bytes=Buffer.from(q.files[0].base64,'base64');bytes[20]^=1;q.files[0].base64=bytes.toString('base64');q.files[0].sha256=hash(bytes);
  e.state.failAfter=e.updates+1;assert.equal(e.send(q).ok,false);
  const b=plain(e.send(q));assert.equal(b.ok,true,JSON.stringify(b));assert.equal(b.folderId,a.folderId);assert.equal(b.files[0].fileId,a.files[0].fileId);assert.equal(e.send(q).alreadyExists,true);
});
test('historic names and bindings remain immutable; ambiguous labels are not rewritten',()=>{
  const e=env();assert.equal(e.context.PhotoNamesV2.name('PUENTE',7,1,'UPSTREAM'),'001_PTE7_ARR.jpg');
  const p={parentKey:'SCAP/FICTICIO',mediaId:'OLD',category:'UPSTREAM',categoryLabel:'AGUAS ARRIBA',mimeType:'video/mp4',sizeBytes:3,sha256:'a'.repeat(64)};
  const old=e.context.VideoNamesV2.reserve(p,7);assert.equal(old.naming.fileName,'001_PUE7_ARR.mp4');assert.equal(old.naming.categoryFolder,'AGUAS_ARRIBA');
  assert.deepEqual(plain(e.context.VideoNamesV2.reserve(p,7,old.state).naming),plain(old.naming));
  assert.throws(()=>e.context.VideoNamesV2.reserve({...p,category:'SCAP20_09'},7,old.state),/VIDEO_NAME_BINDING_CONFLICT/);
});
test('all twenty video reservations use the same official folders and survive duplicate jobs without extra counters',()=>{
  const e=env();let state;
  for(const cat of e.context.ScapPhotoPanelV2.categories){
    const p={parentKey:'SCAP/FICTICIO',mediaId:'VIDEO_'+cat.order,category:cat.key,categoryLabel:cat.label,mimeType:'video/mp4',sizeBytes:3,sha256:'a'.repeat(64)};
    const a=e.context.VideoNamesV2.reserve(p,7,state);state=a.state;
    assert.equal(a.naming.fileName,e.context.ScapPhotoPanelV2.fileName(cat.key,7,1,'mp4'));assert.equal(a.naming.categoryFolder,cat.folder);
    const repeated=e.context.VideoNamesV2.reserve(p,7,state);assert.deepEqual(plain(repeated),plain(a));
  }
  assert.equal(Object.keys(state.videos).length,20);
});
test('transaction/ACK/canonical/Excel/ordering functions are byte-for-byte equivalent to original V20',()=>{
  const original=vm.createContext({});vm.runInContext(fs.readFileSync(path.join(reference,'SyncV3Core.gs.txt'),'utf8'),original);
  const e=env();for(const key of ['commit','canonical','mergeSheet','validate','compareTechnical','allocateNumber'])assert.equal(e.context.SyncV3[key].toString(),original.SyncV3[key].toString(),key);
});
