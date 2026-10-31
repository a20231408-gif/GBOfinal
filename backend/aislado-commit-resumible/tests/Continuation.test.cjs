const {test}=require('node:test'),assert=require('node:assert/strict'),fs=require('node:fs'),path=require('node:path'),vm=require('node:vm');
const {env,ROOT,sha}=require('../../../docs/scap-integracion-actual-20261027/pruebas/MockActual.cjs');
const operational=process.env.GBO_RESUMABLE_SOURCE==='original';
const source=path.resolve(__dirname,operational?'../../candidato-original-reanudable':'..');
const clone=x=>JSON.parse(JSON.stringify(x));
const plain=x=>JSON.parse(x.getContent());
function setup(){
 const e=env('INTEGRADO');e.props.GBO_ISOLATED_ROOT_ID=ROOT;e.props.GBO_RESUMABLE_ROOT_ID=ROOT;e.props.GBO_PUBLIC_NAMES_ENABLED='true';
 e.context.Session={getEffectiveUser:()=>({getEmail:()=>'reconocimientoinventariovial@gmail.com'})};
 e.context.DriveApp.Access={PRIVATE:'PRIVATE'};e.root.name='GBO_PRUEBAS_PRIVADAS_20261030_MOCK';e.root.getSharingAccess=()=> 'PRIVATE';e.root.getOwner=()=>({getEmail:()=>'reconocimientoinventariovial@gmail.com'});
 if(operational)require('../../candidato-original-reanudable/tests/RootGuardMock.cjs').rootGuardMock(e);
 let tick=0,createCost=0,metadataCost=0,afterCreate=null,afterUpdate=null,allocation=0;
 const originalGet=e.context.Drive.Files.get;e.context.Drive.Files.get=(...args)=>{tick+=metadataCost;return originalGet(...args);};
 e.context.Drive.Files.generateIds=({count=1}={})=>({ids:Array.from({length:count},()=> 'preallocated_'+(++allocation))});
 const originalCreate=e.context.Drive.Files.create;
 e.context.Drive.Files.create=(m,b)=>{const out=originalCreate(m,b);tick+=createCost;if(afterCreate)afterCreate(m,out);return out;};
 e.context.Drive.Files.update=(m,id,b,options={})=>{
  const f=e.objects.get(id);if(!f)throw Error('404 File not found');
  if(b)f.b=b;if(m.name!==undefined)f.setName(m.name);if(m.description!==undefined)f.setDescription(m.description);
  if(options.addParents&&f.parent.id!==options.addParents)f.moveTo(e.objects.get(options.addParents));
  if(afterUpdate)afterUpdate(m,id,b);return {id};
 };
 e.context.Date=class extends Date{static now(){return tick;}};
 const p=JSON.parse(fs.readFileSync(path.resolve(__dirname,'../../../docs/scap-integracion-actual-20261027/pruebas/fixtures/ANDROID_SCAP_54.json'),'utf8'));
 const reservation=plain(e.context.reservePhotoNamesV2({...p,photos:p.files.filter(x=>x.key.startsWith('photo/')).map(x=>({id:x.key.slice(6),category:x.photoCategory}))}));assert.equal(reservation.ok,true);
 const stages={};for(const f of p.files){const ack=plain(e.context.stageRecordV3({...p,file:f}));assert.equal(ack.ok,true);stages[f.key]=ack.stageFileId;}
 const q=clone(p);for(const f of q.files){f.stageFileId=stages[f.key];delete f.base64;}
 function load(){for(const name of (operational?['AuthorizedRootGuard','PublicDeliveryNames','PhotoNamesV2Core','SyncV3Core','SyncV3']:['SyncV3Core','SyncV3']))vm.runInContext(fs.readFileSync(path.join(source,name+'.gs'),'utf8'),e.context,{filename:name+'.gs'});}
 function send(x=q){return plain(e.context.commitRecordV3(clone(x)));}
 function journal(){const f=[...e.objects.values()].find(x=>x.name==='journal_'+sha(q.key)+'.json');return f&&JSON.parse(f.getBlob().getDataAsString());}
 function complete(x=q){let a;for(let n=0;n<50;n++){a=send(x);if(a.ok)return {ack:a,calls:n+1};assert.equal(a.code,'COMMIT_IN_PROGRESS',JSON.stringify(a));assert.equal(a.retryable,true);assert.equal(journal()?.status==='COMPLETE',false);}throw Error('Continuation stalled');}
 return {e,p,q,load,send,journal,complete,get elapsed(){return tick;},set metadataCost(v){metadataCost=v;},set createCost(v){createCost=v;},set afterCreate(v){afterCreate=v;},set afterUpdate(v){afterUpdate=v;}};
}
function assertAck(s,a,p=s.q){
 assert.equal(a.ok,true);assert.equal(a.key,p.key);assert.equal(a.revision,p.revision);assert.equal(a.contentHash,p.contentHash);
 assert.equal(a.files.length,59);assert.equal(a.sicFiles.length,3);assert.equal(new Set(a.files.map(x=>x.fileId)).size,59);
 for(const f of a.files){const expected=p.files.find(x=>x.key===f.key),actual=s.e.objects.get(f.fileId);assert.equal(sha(Buffer.from(actual.getBlob().getBytes())),expected.sha256);assert.equal(f.sha256,expected.sha256);assert.equal(actual.name,expected.path.split('/').pop());assert.equal(actual.parent.id,f.folderId);}
 assert.equal(s.journal().status,'COMPLETE');
 const publicFiles=[...s.e.objects.values()].filter(x=>x.description?.startsWith('gbo-v3-file:'+p.key+':')&&x.parent.id!==s.journal().archiveId);
 assert.equal(publicFiles.length,59);
}
test(operational?'unreviewed operational legacy journal is preserved and rejected without mutation':'adopts exactly fourteen legacy operations including the archived fourteenth; completes 59 + 3 SIC with unchanged IDs and stable lost ACK',()=>{
 const s=setup(),{e,q}=s;
 const tx=e.context.v3Store().begin(q,{},[]);for(const f of q.files.slice(0,14))tx.file({...f,bytes:e.context.v3Store().fileBytes(q,f)});
 let j=s.journal();const fourteen=j.operations.map(x=>x.id);e.objects.get(fourteen[13]).moveTo(e.objects.get(j.archiveId));
 const snapshot=JSON.stringify(s.journal());e.context.GBO_FICTITIOUS_SCAP_PAYLOAD=clone(q);s.load();s.createCost=20000;
 if(operational){const writes=e.writes.length;assert.equal(s.send().code,'LEGACY_RECOVERY_REQUIRES_REVIEW');assert.equal(JSON.stringify(s.journal()),snapshot);assert.equal(e.writes.length,writes);return;}
 const result=s.complete();assert.ok(result.calls>1);assertAck(s,result.ack);
 for(let n=0;n<14;n++)assert.equal(result.ack.files[n].fileId,fourteen[n]);
 const backup=[...e.objects.values()].find(x=>x.name.startsWith('evidence_legacy_'));assert.equal(backup.getBlob().getDataAsString(),snapshot);
 const count=e.objects.size,again=s.send();assert.equal(again.alreadyExists,true);assert.deepEqual(again.files,result.ack.files);assert.deepEqual(again.sicFiles,result.ack.sicFiles);assert.equal(e.objects.size,count);
});
test('fresh 59-file commit resumes before mutation, fences competing owners, and never exposes a pending receipt',()=>{
 const s=setup();s.load();s.createCost=20000;
 let a=s.send();assert.equal(a.ok,false);assert.equal(a.code,'COMMIT_IN_PROGRESS');assert.equal(s.journal().status,'PENDING');
 const writes=s.e.writes.length;const other={...s.q,key:'SCAP/ANOTHER',recordId:'ANOTHER'};assert.equal(s.send(other).code,'BUSY');assert.equal(s.e.writes.length,writes);
 assert.equal(s.send({...s.q,contentHash:'f'.repeat(64)}).code,'REVISION_CONFLICT');
 assertAck(s,s.complete().ack);
});
test('lost create response reconciles the preallocated ID instead of allocating a duplicate',()=>{
 const s=setup();s.load();let lostId;
 s.afterCreate=(m,a)=>{if(!lostId&&m.description?.startsWith('gbo-v3-file:')){lostId=a.id;throw Error('LOST_RESPONSE_AFTER_CREATE');}};
 let a=s.send();assert.equal(a.ok,false);assert.equal(a.retryable,true);assert.equal(s.journal().status,'PENDING');
 s.afterCreate=null;const ack=s.complete().ack;assertAck(s,ack);assert.ok(ack.files.some(f=>f.fileId===lostId));
});
test(operational?'operational candidate ignores isolated fault-injection property':'editor-armed interruption fires after the actual write and is reconciled once without duplicate files',()=>{
 const s=setup();s.load();s.e.props.GBO_TEST_FAULT_V1=JSON.stringify({armed:true,phase:'afterCreate',ownerPattern:'gbo-v3-file:'});
 if(operational){assertAck(s,s.complete().ack);assert.equal(JSON.parse(s.e.props.GBO_TEST_FAULT_V1).armed,true);return;}
 assert.equal(s.send().code,'TEST_INTERRUPTED');assert.equal(JSON.parse(s.e.props.GBO_TEST_FAULT_V1).armed,false);assert.equal(s.journal().status,'PENDING');assertAck(s,s.complete().ack);
});
test('lost record-write response cannot bypass pending binding/journal completion with a premature ACK',()=>{
 const s=setup();s.load();let lost=false;
 s.afterCreate=(m)=>{if(!lost&&m.name.startsWith('record_')){lost=true;throw Error('LOST_RECORD_RESPONSE');}};
 assert.equal(s.send().ok,false);assert.equal(s.journal().status,'PENDING');assert.equal(s.e.context.v3Store(s.q).get(s.q.key),null);
 s.afterCreate=null;assertAck(s,s.complete().ack);
});
test('changed staging after validation proof and foreign root fail closed without an ACK',()=>{
 const s=setup();s.load();s.createCost=20000;assert.equal(s.send().ok,false);
 const file=s.e.objects.get(s.q.files[30].stageFileId);file.b=s.e.blob('CHANGED','image/jpeg',file.name);
 assert.equal(s.send().ok,false);assert.equal(s.send().code,'HASH_MISMATCH');assert.equal(s.journal().status,'PENDING');
 s.e.props[operational?'GBO_RESUMABLE_ROOT_ID':'GBO_ISOLATED_ROOT_ID']='OUTSIDE_ROOT';const writes=s.e.writes.length;assert.equal(s.send().code,operational?'RESUMABLE_ROOT_NOT_APPROVED':'ISOLATED_BACKEND_ONLY');assert.equal(s.e.writes.length,writes);
});
test('script contention returns BUSY and preserves all staged bytes',()=>{
 const s=setup();s.load();s.e.hold(true);const writes=s.e.writes.length;assert.equal(s.send().code,'BUSY');assert.equal(s.e.writes.length,writes);s.e.hold(false);assertAck(s,s.complete().ack);
});
test('preallocated media intent still rejects a foreign existing name instead of creating a duplicate',()=>{
 const s=setup();s.load();s.createCost=20000;assert.equal(s.send().ok,false);
 const op=s.journal().operations.find(x=>!s.e.objects.has(x.id)),f=s.q.files.find(x=>'gbo-v3-file:'+s.q.key+':'+x.key===op.owner);
 const binding=[...s.e.objects.values()].find(x=>x.name==='binding_v4_'+sha(s.q.identityId)+'.json');let parent=s.e.objects.get(JSON.parse(binding.getBlob().getDataAsString()).folderId);
 const pieces=f.path.split('/');const name=pieces.pop();for(const part of pieces){const it=parent.getFoldersByName(part);parent=it.hasNext()?it.next():parent.createFolder(part);}
 const foreign=parent.createFile(s.e.blob('FOREIGN','text/plain',name));const a=s.send();assert.equal(a.ok,false);assert.equal(a.code,'HISTORICAL_BINDING_REQUIRED');assert.equal(foreign.getBlob().getDataAsString(),'FOREIGN');assert.equal([...s.e.objects.values()].filter(x=>x.parent?.id===parent.id&&x.name===name).length,1);
});
test('protocol boundary: eighty fictitious files complete; eighty-one is rejected without a transaction',()=>{
 const s=setup();s.load();const q=clone(s.q),original=s.p.files.find(f=>f.key.startsWith('photo/'));
 const extra=Array.from({length:21},(_,n)=>({...original,key:'photo/BOUNDARY_'+n}));
 const names=plain(s.e.context.reservePhotoNamesV2({...q,photos:s.p.files.filter(x=>x.key.startsWith('photo/')).concat(extra).map(x=>({id:x.key.slice(6),category:x.photoCategory}))}));assert.equal(names.ok,true);
 for(const f of extra){const n=names.photos.find(x=>x.id===f.key.slice(6));f.path=original.path.slice(0,original.path.lastIndexOf('/')+1)+n.name;
  const staged=plain(s.e.context.stageRecordV3({...q,file:f}));assert.equal(staged.ok,true);f.stageFileId=staged.stageFileId;delete f.base64;q.files.push(f);}
 q.contentHash=sha(s.e.context.SyncV3.canonical(q));const invalid=clone(q);invalid.files.push({...invalid.files[0],key:'OVER_LIMIT'});invalid.contentHash=sha(s.e.context.SyncV3.canonical(invalid));
 const writes=s.e.writes.length;assert.equal(s.send(invalid).code,'INVALID_REQUEST');assert.equal(s.e.writes.length,writes);
 s.createCost=20000;const a=s.complete(q).ack;assert.equal(a.files.length,80);assert.equal(a.sicFiles.length,3);assert.equal(new Set(a.files.map(x=>x.fileId)).size,80);assert.equal(s.journal().status,'COMPLETE');
});
test('revision update keeps file IDs and original backup when an update response is lost',()=>{
 const s=setup();s.load();const first=s.complete().ack;const q=clone(s.q);q.baseRevision=q.revision;q.revision++;
 for(const f of q.files){const original=s.p.files.find(x=>x.key===f.key);let bytes=Buffer.from(original.base64,'base64');if(f===q.files[0])bytes=Buffer.concat([bytes.subarray(0,-2),Buffer.from([42,255,217])]);f.sha256=sha(bytes);const staged=plain(s.e.context.stageRecordV3({...q,file:{...f,base64:bytes.toString('base64')}}));assert.equal(staged.ok,true);f.stageFileId=staged.stageFileId;}
 q.contentHash=sha(s.e.context.SyncV3.canonical(q));let fail=true;
 s.afterUpdate=(m,id,b)=>{if(fail&&b&&id===first.files[0].fileId){fail=false;throw Error('LOST_UPDATE_RESPONSE');}};
 assert.equal(s.send(q).ok,false);assert.equal(s.journal().status,'PENDING');assert.equal(s.e.context.v3Store(q).get(q.key).revision,q.baseRevision);
 s.afterUpdate=null;const final=s.complete(q).ack;assertAck(s,final,q);assert.deepEqual(final.files.map(x=>x.fileId),first.files.map(x=>x.fileId));
 const op=s.journal().operations.find(x=>x.id===first.files[0].fileId);assert.ok(op.backupId);assert.equal(sha(Buffer.from(s.e.objects.get(op.backupId).getBlob().getBytes())),first.files[0].sha256);
});

if(operational){
 test('slow metadata yields inside validation before six minutes without a false ACK or losing original staging',()=>{
  const s=setup();s.load();s.metadataCost=5000;
  assert.equal(s.send().code,'COMMIT_IN_PROGRESS');assert.ok(s.elapsed<360000);assert.equal(s.e.objects.has(s.q.files[58].stageFileId),true);
  s.metadataCost=0;assertAck(s,s.complete().ack);
 });
 test('descriptive reservations remain blocked when their explicit activation property is absent',()=>{
  const s=setup();s.load();delete s.e.props.GBO_PUBLIC_NAMES_ENABLED;const writes=s.e.writes.length;
  assert.equal(s.send({...s.q,publicNamingVersion:1}).code,'PUBLIC_NAMES_DISABLED');assert.equal(s.e.writes.length,writes);
 });
 test('descriptive V2 reservation and canonical ACK remain stable across edits, replay and an older numeric binding',()=>{
  const s=setup();s.load();const {e}=s;
  const source=s.p.files.find(f=>f.key.startsWith('photo/'));
  const p={...clone(s.p),kind:'SIC',key:'SIC/FICTITIOUS_PUBLIC_NAMES',recordId:'FICTITIOUS_PUBLIC_NAMES',identityId:'FICTITIOUS_PUBLIC_NAMES',family:'ALCANTARILLA',familyPath:'ALCANTARILLAS',baseRevision:0,publicNamingVersion:1,publicDisplayLabel:'ALCANTARILLA - PE-28H - PR 40+200',sicRows:[],files:[{...source,key:'photo/FICTITIOUS_PHOTO',photoCategory:'CULVERT_PANORAMIC'}]};
  delete p.contentHash;delete p.action;
  const request={...p,photos:[{id:'FICTITIOUS_PHOTO',category:'CULVERT_PANORAMIC'}]};
  const reserve=plain(e.context.reservePhotoNamesV2(request));assert.equal(reserve.ok,true,JSON.stringify(reserve));
  assert.equal(reserve.photos[0].name,'ALCANTARILLA - PE-28H - PR 40+200 - PANORAMICA - Foto 1.jpg');
  assert.ok(reserve.publicElementFolder.startsWith('ALCANTARILLA 1 - '));
  p.publicElementFolder=reserve.publicElementFolder;p.publicDisplayLabel=reserve.publicDisplayLabel;
  const f=p.files[0];f.path='PANORAMICA/'+reserve.photos[0].name;
  const staged=plain(e.context.stageRecordV3({...p,file:f}));assert.equal(staged.ok,true);f.stageFileId=staged.stageFileId;delete f.base64;
  p.contentHash=sha(e.context.SyncV3.canonical(p));const ack=s.complete(p).ack;assert.equal(ack.logicalPath,reserve.logicalPath);assert.equal(ack.files[0].requestedPath,f.path);
  assert.equal(e.objects.get(ack.folderId).name,reserve.publicElementFolder);
  assert.equal(s.send(p).alreadyExists,true);
  const changed=plain(e.context.reservePhotoNamesV2({...request,baseRevision:p.revision,publicDisplayLabel:'ETIQUETA EDITADA'}));
  assert.equal(changed.ok,true);assert.equal(changed.logicalPath,reserve.logicalPath);assert.equal(changed.photos[0].name,reserve.photos[0].name);
  const oldRequest={...clone(s.p),publicNamingVersion:1,publicDisplayLabel:'PUENTE - NOMBRE REAL',photos:s.p.files.filter(f=>f.key.startsWith('photo/')).map(f=>({id:f.key.slice(6),category:f.photoCategory}))};delete oldRequest.contentHash;
  const old=plain(e.context.reservePhotoNamesV2(oldRequest));
  assert.equal(old.ok,true,JSON.stringify(old));assert.equal(old.logicalPath,s.p.routeCode+'/PUENTES/PUENTE 1');assert.equal(old.photos[0].name,s.p.files.find(f=>f.key.startsWith('photo/')).path.split('/').pop());
  const fixture=path.join(sourceDir(),'tests','PublicNamesContract.json');fs.mkdirSync(path.dirname(fixture),{recursive:true});fs.writeFileSync(fixture,JSON.stringify({prepared:p,ack,reservation:reserve},null,2));
 });
 test('Unicode prefix truncation, reserved names and destructive characters are sanitized without splitting a code point',()=>{
  const s=setup();s.load();const n=s.e.context.PublicDeliveryNames;
  assert.equal(n.component('abc😀z',4),'abc');assert.equal(n.component('CON'),'Ficha - CON');assert.equal(n.component('A/../B\u0000'), 'A B');assert.equal(n.component('Cafe\u0301'),'Café');
  const long=n.photoName('Ñ'.repeat(100),'CULVERT_PANORAMIC',999);assert.ok(Buffer.byteLength(long)<=80);assert.ok(long.endsWith('Foto 999.jpg'));
 });
}
function sourceDir(){return source;}
