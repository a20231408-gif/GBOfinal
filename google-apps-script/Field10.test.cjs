const {test}=require('node:test'),assert=require('node:assert/strict'),fs=require('node:fs');
const names=require('./PhotoNamesV2Core.js'),core=require('./SyncV3Core.js'),video=require('./VideoV1Core.js');
const {environment,hash}=require('./DriveV4Harness.cjs');
const fixture=()=>JSON.parse(fs.readFileSync('docs/fotografias-5/examples/alcantarilla5.json'));
function reserve(e,p,photos=p.files.filter(f=>f.key.startsWith('photo/')).map(f=>({id:f.key.substring(6),category:f.photoCategory}))) {
  return e.context.reservePhotoNamesV2({...p,photos,photoNamingVersion:2});
}
test('readable catalog covers every V4 family and contractual photo category within 20 characters',()=>{
  assert.deepEqual(Object.keys(names.familyCodes).sort(),Object.keys(core.families).sort());
  for(const family of Object.keys(names.familyCodes))for(const category of Object.keys(names.categoryCodes))assert.ok(names.name(family,1,999,category).length<=20);
  assert.equal(names.name('ALCANTARILLA',1,1,'CULVERT_PANORAMIC'),'001_ALC1_PAN.jpg');
  assert.equal(names.name('ALCANTARILLA',2,1,'CULVERT_INLET'),'001_ALC2_ENT.jpg');
  for(const [surface,code] of Object.entries({ASFALTICO:'PAF',RIGIDO:'PRG',NO_PAVIMENTADO:'PNP'}))assert.equal(names.name('MUESTRA',1,1,'GENERAL',surface),`001_${code}1_GEN.jpg`);
  assert.throws(()=>names.name('ALCANTARILLA',100000,1,'GENERAL'),/READABLE_NAME_LIMIT/);
  assert.throws(()=>names.name('ALCANTARILLA',1,1000,'GENERAL'),/READABLE_NAME_LIMIT/);
});
test('two tablets reserve by photo UUID, idempotently, without reusing retired counters',()=>{
  const e=environment(),p=fixture(),a=reserve(e,p),again=reserve(e,p);assert.equal(a.ok,true,JSON.stringify(a));assert.deepEqual(a,again);
  const b=reserve(e,p,[{id:'second-tablet-photo10',category:'CULVERT_INLET'}]);assert.equal(b.ok,true);assert.equal(b.folderId,a.folderId);
  assert.ok(!a.photos.some(f=>f.name===b.photos[0].name));assert.ok(b.photos[0].name.length<=20);
  const c=reserve(e,p,[{id:'replacement10',category:'CULVERT_OUTLET'}]);assert.notEqual(c.photos[0].name,b.photos[0].name);
  const wrong=reserve(e,p,[{id:a.photos[0].id,category:'GENERAL'}]);assert.equal(wrong.code,'PHOTO_CATEGORY_RECONCILIATION_REQUIRED');
});
test('reserved names deliver real JPEG and preserve ACK, folder and file IDs on retries',()=>{
  const e=environment(),p=fixture(),r=reserve(e,p);assert.equal(r.ok,true);p.photoNamingVersion=2;
  for(const f of p.files.filter(f=>f.key.startsWith('photo/')))f.path=core.photoPath(p,f,hash,r.photos.find(x=>x.id===f.key.substring(6)).name);
  const a=e.send(p),b=e.send(p);assert.equal(a.ok,true,JSON.stringify(a));assert.equal(b.alreadyExists,true);assert.equal(JSON.stringify(a.files),JSON.stringify(b.files));
  assert.ok(a.files.filter(f=>f.key.startsWith('photo/')).every(f=>f.logicalPath.split('/').pop().length<=20));
  assert.equal(a.sicFiles[0].logicalPath,p.routeCode+'/EXCEL_SIC/SIC-18.xlsx');
});
test('unreserved or forged photo labels cannot silently rename a photo',()=>{
  const e=environment(),p=fixture();p.photoNamingVersion=2;assert.equal(e.send(p).code,'PHOTO_NAME_RESERVATION_REQUIRED');
  const r=reserve(e,p);assert.equal(r.ok,true);p.files.find(f=>f.key.startsWith('photo/')).path='PANORAMICA/001_ALC999_PAN.jpg';
  assert.equal(e.send(p).code,'PHOTO_PATH_CATEGORY_MISMATCH');
});
test('historical photos retain exact names while a new UUID gets a readable name',()=>{
  const e=environment(),p=fixture(),a=e.send(p);assert.equal(a.ok,true);
  const q={...structuredClone(p),revision:p.revision+1,baseRevision:p.revision};
  const added=structuredClone(q.files.find(f=>f.key.startsWith('photo/')));added.key='photo/new-future10';added.photoCategory='CULVERT_OUTLET';q.files.push(added);
  const r=reserve(e,q);assert.equal(r.ok,true,JSON.stringify(r));
  for(const f of r.photos)if(f.id!== 'new-future10'){assert.equal(f.historical,true);assert.equal(f.name,p.files.find(x=>x.key==='photo/'+f.id).path.split('/').pop());}
  q.photoNamingVersion=2;for(const f of q.files.filter(f=>f.key.startsWith('photo/')))f.path=core.photoPath(q,f,hash,r.photos.find(x=>x.id===f.key.substring(6)).name);
  const b=e.send(q);assert.equal(b.ok,true,JSON.stringify(b));for(const old of a.files)assert.equal(b.files.find(f=>f.key===old.key).fileId,old.fileId);
});
test('new technical backups live in internal registry; visible folders and old ACKs are stable',()=>{
  const e=environment(),p=fixture(),a=e.send(p);assert.equal(a.ok,true);
  const q={...structuredClone(p),revision:p.revision+1,baseRevision:p.revision};const b=e.send(q);assert.equal(b.ok,true);assert.equal(b.folderId,a.folderId);
  assert.equal(e.objects.get(a.folderId).folders.some(f=>f.name==='REVISIONES'),false);
  const registry=e.root.folders.find(f=>f.name==='._GBO_SYNC_V3'),revisions=registry.folders.find(f=>f.name==='REVISIONS');assert.ok(revisions.folders.length);
  assert.ok(revisions.folders.some(owner=>owner.folders.some(rev=>rev.files.some(f=>f.name==='SIC-18.xlsx'))));
});
test('video rejects conventional SIC and pavements before allocating or touching a file',()=>{
  for(const kind of ['SIC','PAVEMENT'])assert.throws(()=>video.validate({videoProtocol:1,parentKey:kind+'/parent',mediaId:'video10',category:'GENERAL',categoryLabel:'General',mimeType:'video/mp4',sizeBytes:20,sha256:'a'.repeat(64),parentRevision:1}),/INVALID_VIDEO/);
});
