const {test}=require('node:test'),assert=require('node:assert/strict'),fs=require('node:fs'),path=require('node:path');
const {environment,hash}=require('./DriveV4Harness.cjs'),core=require('./SyncV3Core.js');
const folder=path.resolve(__dirname,'../docs/fotografias-5/examples');
const fixture=id=>JSON.parse(fs.readFileSync(path.join(folder,id+'.json'),'utf8'));
const clone=p=>structuredClone(p),next=p=>({...clone(p),revision:p.revision+1,baseRevision:p.revision});
const photos=p=>p.files.filter(f=>f.key.startsWith('photo/'));
const photoName=f=>'F_'+hash(f.key.substring(6)).substring(0,16)+'.jpg';
const ack=(e,p)=>{const a=e.send(p);assert.equal(a.ok,true,JSON.stringify(a));return a;};
const read=(e,id)=>Buffer.from(e.objects.get(id).getBlob().getBytes());
function noTechnicalWrites(e) {assert.equal(e.updates,0);assert.equal([...e.objects.values()].filter(x=>x.b).length,0);assert.equal(e.root.folders.some(f=>f.name!== '._GBO_SYNC_V3'),false);}

for(const id of ['baden5','muro5','tunel5','cuneta5','guardavia5','tacha5','senal5','alcantarilla5'])test(id+' actual Android categories JPEG Excel and idempotent V3 receipt',()=>{
 const e=environment(),p=fixture(id);assert.equal(hash(core.canonical(p)),p.contentHash,'Android / Apps Script signed category contract');
 const a=ack(e,p);assert.equal(a.pathVersion,4);assert.equal(a.logicalPath.includes('SIB'),false);assert.equal(a.georeferencingStatus,'PENDING_EXTERNAL');
 for(const f of photos(p))assert.ok(a.files.some(x=>x.key===f.key && x.requestedPath===f.path && read(e,x.fileId).equals(Buffer.from(f.base64,'base64'))));
 assert.equal(a.sicFiles.length,p.sicRows.length);const count=e.objects.size,updates=e.updates;
 const retry=ack(e,p);assert.equal(retry.alreadyExists,true);assert.equal(retry.folderId,a.folderId);assert.equal(e.objects.size,count);assert.equal(e.updates,updates);
 assert.deepEqual([...retry.files.map(f=>f.fileId)],[...a.files.map(f=>f.fileId)]);
});

test('historical GENERAL remains visible without migration or subtype folder',()=>{
 const e=environment(),p=fixture('senal5'),a=ack(e,p),general=photos(p).find(f=>f.photoCategory==='GENERAL');
 assert.ok(a.files.some(f=>f.key===general.key && f.logicalPath.includes('/GENERAL/')));
 assert.equal([...e.objects.values()].some(f=>f.folders && ['P-1','ACERO','BUENA','PREVENTIVA'].includes(f.name)),false);
});
test('backend rejects a photo path that contradicts its internal category before writing',()=>{
 const e=environment(),p=fixture('baden5');photos(p)[0].path='FIN/'+photoName(photos(p)[0]);
 assert.equal(e.send(p).code,'PHOTO_PATH_CATEGORY_MISMATCH');noTechnicalWrites(e);
 const q=fixture('senal5');photos(q)[0].path='PREVENTIVA/'+photoName(photos(q)[0]);
 assert.equal(e.send(q).code,'PHOTO_PATH_CATEGORY_MISMATCH');noTechnicalWrites(e);
});
test('SIC08 and SIC09 cannot inherit linear categories even if they have two PRs',()=>{
 for(const id of ['SIC-08','SIC-09']) {
  const e=environment(),p=JSON.parse(fs.readFileSync(path.resolve(__dirname,'../docs/drive-familias-2a/examples',id,'payload.json'),'utf8'));
  Object.assign(p,{photoCategoryVersion:1,photoHasEnds:true,photoClassCode:'',photoTypeCode:''});
  for(const f of photos(p)){f.photoCategory='GENERAL';f.path='GENERAL/'+photoName(f);}
  ack(e,p);const q=next(p),f=photos(q)[0];f.photoCategory='INICIO';f.path='INICIO/'+photoName(f);
  assert.equal(e.send(q).code,'INVALID_PHOTO_CATEGORY');
 }
});
test('adding an observation retains previous fileIds and family numbering',()=>{
 const e=environment(),p=fixture('baden5'),a=ack(e,p),q=next(p),f=clone(photos(p)[0]);
 f.key='photo/add-observation5';f.photoCategory='OBSERVACION';f.path='OBSERVACION/'+photoName(f);q.files.push(f);
 const b=ack(e,q);assert.equal(b.folderId,a.folderId);assert.equal(b.logicalPath,a.logicalPath);
 for(const old of a.files)assert.equal(b.files.find(f=>f.key===old.key).fileId,old.fileId);
 assert.equal(b.files.length,a.files.length+1);assert.equal(ack(e,q).alreadyExists,true);
});
test('explicit recategorization moves the same remote identity and archives prior placement',()=>{
 const e=environment(),p=fixture('baden5'),a=ack(e,p),q=next(p),f=photos(q)[0],old=a.files.find(x=>x.key===f.key);
 f.photoCategory='FIN';f.path='FIN/'+photoName(f);const b=ack(e,q),changed=b.files.find(x=>x.key===f.key);
 assert.equal(changed.fileId,old.fileId);assert.ok(changed.logicalPath.includes('/FIN/'));assert.equal(ack(e,q).alreadyExists,true);
 assert.ok([...e.objects.values()].some(x=>x.b && x.parent.name.startsWith(q.revision+'_') && read(e,x.id).equals(read(e,old.fileId))));
});
test('replacement updates fileId safely and preserves original archive',()=>{
 const e=environment(),p=fixture('baden5'),a=ack(e,p),q=next(p),f=photos(q)[0],old=a.files.find(x=>x.key===f.key),bytes=read(e,old.fileId);
 const changed=Buffer.from(bytes);changed[changed.length-3]^=1;f.base64=changed.toString('base64');f.sha256=hash(changed);
 const b=ack(e,q);assert.equal(b.files.find(x=>x.key===f.key).fileId,old.fileId);assert.deepEqual(read(e,old.fileId),changed);
 assert.ok([...e.objects.values()].some(x=>x.b && x.parent.name.startsWith(q.revision+'_') && read(e,x.id).equals(bytes)));
});
test('confirmed retirement archives only owned photo and retains another record and all Excel rows',()=>{
 const e=environment(),p=fixture('baden5'),a=ack(e,p),other=clone(p);
 other.recordId='other-baden5';other.key='SIC/'+other.recordId;other.identityId=other.recordId;other.sicRows.forEach(r=>r.recordId=other.recordId);
 const b=ack(e,other),otherFile=b.files.find(f=>f.key.startsWith('photo/')),otherBytes=read(e,otherFile.fileId);
 const q=next(p),retired=photos(q)[0],old=a.files.find(f=>f.key===retired.key);q.files=q.files.filter(f=>f.key!==retired.key);
 const c=ack(e,q);assert.equal(c.folderId,a.folderId);assert.ok(e.objects.get(old.fileId).parent.name.startsWith(q.revision+'_'));
 assert.deepEqual(read(e,otherFile.fileId),otherBytes);assert.deepEqual([...c.sicFiles[0].recordIds].sort(),[p.recordId,other.recordId].sort());
});
test('two photo editors conflict without overwriting the winner; winner retry is harmless',()=>{
 const e=environment(),p=fixture('baden5');ack(e,p);const winner=next(p),w=photos(winner)[0];w.photoCategory='FIN';w.path='FIN/'+photoName(w);
 const a=ack(e,winner),loser=next(p);loser.revision+=5;const before=e.updates;
 const conflict=e.send(loser);assert.equal(conflict.code,'BASE_REVISION_CONFLICT');assert.equal(conflict.remoteRevision,winner.revision);assert.equal(e.updates,before);
 assert.equal(ack(e,winner).folderId,a.folderId);assert.equal(e.updates,before);
});
test('SCAP inspections share bridge identity but keep separate photo and packet paths',()=>{
 const e=environment();let bridgeFolder;
 for(const id of ['inspection5-a','inspection5-b']) {
  const p=fixture('alcantarilla5');Object.assign(p,{kind:'SCAP',recordId:id,key:'SCAP/'+id,identityId:'bridge5',family:'PUENTE',familyPath:'PUENTES',photoHasEnds:false,photoClassCode:'',photoTypeCode:'',sicRows:[]});
  p.files=photos(p).slice(0,2).map((f,n)=>({...f,key:'photo/'+id+'-'+n,photoCategory:n?'DOWNSTREAM':'UPSTREAM'}));
  p.files.forEach(f=>f.path='SCAP/'+id+'/'+(f.photoCategory==='UPSTREAM'?'AGUAS_ARRIBA':'AGUAS_ABAJO')+'/'+photoName(f));
  const bytes=Buffer.from(JSON.stringify({inspectionId:id,bridgeId:'bridge5'}));p.files.push({key:'manifest',path:'SCAP/'+id+'/manifest.json',mimeType:'application/json',base64:bytes.toString('base64'),sha256:hash(bytes)});
  const a=ack(e,p);if(bridgeFolder)assert.equal(a.folderId,bridgeFolder);bridgeFolder=a.folderId;
  assert.ok(a.files.every(f=>f.logicalPath.includes('/SCAP/'+id+'/')));assert.equal(ack(e,p).alreadyExists,true);
 }
 assert.equal([...e.objects.values()].filter(x=>x.folders && x.name==='PUENTE 1').length,1);
});
test('a malformed or tampered category signature cannot commit any files',()=>{
 const e=environment(),p=fixture('baden5');p.photoCategoryVersion=99;assert.equal(e.send(p).code,'UNSUPPORTED_PHOTO_CATEGORY_VERSION');
 const q=fixture('baden5'),f=photos(q)[0];f.photoCategory='FIN';f.path='FIN/'+photoName(f);
 assert.equal(e.context.commitRecordV3(q).code,'CONTENT_HASH_MISMATCH');noTechnicalWrites(e);
});
test('ford keeps physical INICIO and FIN even when its contractual location has one PR',()=>{
 const e=environment(),p=fixture('baden5');p.photoHasEnds=false;
 const a=ack(e,p);assert.ok(a.files.some(f=>f.requestedPath.startsWith('INICIO/')));assert.ok(a.files.some(f=>f.requestedPath.startsWith('FIN/')));
});
