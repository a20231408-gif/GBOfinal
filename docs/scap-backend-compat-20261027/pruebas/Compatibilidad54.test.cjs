const {test}=require('node:test'),assert=require('node:assert/strict');
const fs=require('node:fs'),path=require('node:path'),vm=require('node:vm');
const {env,fixture,plain,enableCandidate,hash,root}=require('./Harness.cjs');
const fixtures=path.join(__dirname,'fixtures');
const wire=()=>JSON.parse(fs.readFileSync(path.join(fixtures,'ANDROID_SCAP_54.json'),'utf8'));
const clone=x=>JSON.parse(JSON.stringify(x));
function reserve(e,p){return plain(e.context.reservePhotoNamesV2({...p,photos:p.files.filter(f=>f.key.startsWith('photo/')).map(f=>({id:f.key.slice(6),category:f.photoCategory}))}));}
function staged(e,p){
  const q=clone(p);
  for(const f of q.files){
    const ack=plain(e.context.stageRecordV3({action:'stageRecordV3',syncProtocol:3,key:q.key,revision:q.revision,file:clone(f)}));
    assert.equal(ack.ok,true,JSON.stringify(ack));assert.equal(ack.key,q.key);assert.equal(ack.revision,q.revision);
    assert.equal(ack.fileKey,f.key);assert.equal(ack.sha256,f.sha256);
    f.stageFileId=ack.stageFileId;delete f.base64;
  }
  return q;
}
function parts(e,base64){return e.context.Utilities.unzip(e.context.Utilities.newBlob([...Buffer.from(base64,'base64')])).map(b=>({name:b.getName(),text:()=>b.getDataAsString()}));}
function assertAck(e,p,a){
  assert.equal(a.ok,true,JSON.stringify(a));assert.equal(a.key,p.key);assert.equal(a.revision,p.revision);assert.equal(a.contentHash,p.contentHash);
  assert.equal(a.files.length,p.files.length);assert.equal(new Set(a.files.map(f=>f.fileId)).size,p.files.length);
  for(const f of p.files){const ack=a.files.find(x=>x.key===f.key);assert.ok(ack);assert.equal(ack.sha256,f.sha256);assert.equal(ack.requestedPath,f.path);assert.equal(ack.logicalPath,a.logicalPath+'/'+f.path);assert.equal(hash(Buffer.from(e.objects.get(ack.fileId).getBlob().getBytes())),f.sha256);}
}

test('original V20 rejects official categories; declaring only the flag cannot implement the missing catalog',()=>{
  const e=env('base'),p=wire();
  assert.equal(reserve(e,p).code,'INVALID_NAME_RESERVATION');
  assert.throws(()=>e.context.SyncV3.photoPath(p,p.files.find(f=>f.key.startsWith('photo/')),hash,'TEST.jpg'),/INVALID_PHOTO_CATEGORY/);
  assert.equal(e.context.PhotoNamesV2.SCAP_PANEL_VERSION,undefined);
});

test('actual Android payload stages and commits 54 official photos, 3 sketches, SCAP workbook and manifest with exact hashes and ACK',()=>{
  const e=env(),p=wire();
  assert.equal(p.files.length,59);assert.equal(p.files.filter(f=>f.key.startsWith('photo/')).length,54);
  assert.equal(p.files.filter(f=>f.key.startsWith('sketch/')).length,3);
  assert.equal(new Set(p.files.filter(f=>f.key.startsWith('photo/')).map(f=>f.photoCategory)).size,20);
  assert.equal(hash(e.context.SyncV3.canonical(p)),p.contentHash,'Android and V20 canonical hash');
  const names=reserve(e,p);assert.equal(names.ok,true,JSON.stringify(names));
  for(const f of p.files.filter(f=>f.key.startsWith('photo/')))assert.equal(names.photos.find(n=>n.id===f.key.slice(6)).name,f.path.split('/').pop());
  const q=staged(e,p),a=plain(e.context.commitRecordV3(clone(q)));assertAck(e,p,a);
  assert.equal(a.folderId,names.folderId);assert.equal(a.sicFiles.length,3);
  assert.deepEqual(a.sicFiles.map(x=>x.format).sort(),['SIC-17','SIC-17A','SIC-17B']);
  for(const f of a.sicFiles){assert.equal(f.recordIds.length,1);assert.equal(f.recordIds[0],p.identityId);assert.equal(f.logicalPath,p.routeCode+'/EXCEL_SIC/'+f.format+'.xlsx');assert.equal(hash(Buffer.from(e.objects.get(f.fileId).getBlob().getBytes())),f.sha256);}
  const count=e.objects.size,repeated=plain(e.context.commitRecordV3(clone(q)));
  assert.equal(repeated.alreadyExists,true);assert.equal(repeated.folderId,a.folderId);assert.equal(repeated.contentHash,a.contentHash);
  assert.deepEqual(repeated.files,a.files);assert.deepEqual(repeated.sicFiles,a.sicFiles);assert.equal(e.objects.size,count);
  const target=path.join(fixtures,process.env.GBO_ACK_OUTPUT || 'ACK_CANDIDATO_54.json');assert.equal(fs.existsSync(target),false,'Conservar ACK de cada corrida');fs.writeFileSync(target,JSON.stringify(a,null,2));
});

test('F2 remains 32 photos while the remote manifest and originals retain all 54; continuation HTML is local, not secretly added to ACK',()=>{
  const e=env(),p=wire(),book=p.files.find(f=>f.key==='SCAP.xlsx'),manifestFile=p.files.find(f=>f.key==='manifest.json');
  const sheet=parts(e,book.base64).find(x=>x.name==='xl/worksheets/sheet4.xml').text();
  assert.equal((sheet.match(/TEST_FOTO_/g)||[]).length,32);
  const m=JSON.parse(Buffer.from(manifestFile.base64,'base64').toString());
  assert.equal(m.fieldCapture.photos.length,54);assert.equal(m.fieldCapture.sketches.length,3);assert.equal(m.revision,p.revision);
  for(const f of m.files){const actual=p.files.find(x=>x.path===f.path);assert.ok(actual,f.path);assert.equal(actual.sha256,f.sha256);}
  assert.equal(p.files.some(f=>f.path.endsWith('CONTINUACION_FOTOGRAFICA.html')),false);
  const local=fs.readFileSync(path.join(root,'docs/scap-p0-final-20261027/evidencias/p0-revalidado/SCAP_54_FOTOS_3_CROQUIS_FICTICIO.zip'));
  const items=parts(e,local.toString('base64'));
  assert.ok(items.some(x=>x.name.endsWith('CONTINUACION_FOTOGRAFICA.html')));
  assert.equal(items.filter(x=>x.name.split('/').includes('FOTOGRAFIAS')).length,54);
});

test('BUSY in staging or commit never consumes a successful receipt; an uncertain reply repeats identical identity and files',()=>{
  const e=env(),{payload:p}=fixture(e,[1,20]);reserve(e,p);const count=e.objects.size;
  e.state.busy=true;
  assert.equal(e.context.stageRecordV3({syncProtocol:3,key:p.key,revision:p.revision,file:clone(p.files[0])}).code,'BUSY');
  assert.equal(e.send(p).code,'BUSY');assert.equal(e.objects.size,count);
  e.state.busy=false;const a=plain(e.send(p)),b=plain(e.send(p));assert.equal(a.ok,true);assert.equal(b.alreadyExists,true);
  assert.equal(a.folderId,b.folderId);assert.deepEqual(a.files,b.files);
});

test('interrupted update rolls back; retry retains all 59 file IDs, stable folder, three SIC rows and original media hashes',()=>{
  const e=env(),p=wire();assert.equal(reserve(e,p).ok,true);const a=plain(e.send(p));assertAck(e,p,a);
  const edit=clone(p);edit.baseRevision=p.revision;edit.revision++;
  const file=edit.files.find(f=>f.key==='manifest.json'),manifest=JSON.parse(Buffer.from(file.base64,'base64').toString());
  manifest.revision=edit.revision;manifest.testOnlyNote='EDICION_FICTICIA';const bytes=Buffer.from(JSON.stringify(manifest));file.base64=bytes.toString('base64');file.sha256=hash(bytes);
  edit.contentHash=hash(e.context.SyncV3.canonical(edit));
  e.state.failAfter=e.updates+3;assert.equal(e.send(edit).ok,false);
  for(const f of a.files)assert.equal(hash(Buffer.from(e.objects.get(f.fileId).getBlob().getBytes())),f.sha256);
  const b=plain(e.send(edit));assertAck(e,edit,b);assert.equal(b.folderId,a.folderId);
  for(const f of b.files)assert.equal(f.fileId,a.files.find(x=>x.key===f.key).fileId);
  assert.ok(b.sicFiles.every(f=>f.recordIds.length===1));assert.equal(e.send(edit).alreadyExists,true);
});

test('existing V20 photo reservations and ACK file IDs remain immutable when new official evidence is added',()=>{
  const e=env('base'),p=wire();p.sicRows=[];p.files=p.files.filter(f=>f.key.startsWith('photo/')).slice(0,1);p.files[0].photoCategory='UPSTREAM';
  let names=reserve(e,p);assert.equal(names.ok,true);p.files[0].path='SCAP/'+p.recordId+'/AGUAS_ARRIBA/'+names.photos[0].name;
  const a=plain(e.send(p));assert.equal(a.ok,true,JSON.stringify(a));
  enableCandidate(e);const q=clone(p);q.baseRevision=p.revision;q.revision++;
  assert.equal(reserve(e,q).photos[0].name,names.photos[0].name);
  const added={...q.files[0],key:'photo/TEST_NUEVA_20',photoCategory:'SCAP20_20'};q.files.push(added);
  names=reserve(e,q);assert.equal(names.ok,true);const category=e.context.ScapPhotoPanelV2.get(added.photoCategory);
  added.path='SCAP/'+q.recordId+'/'+category.folder+'/'+names.photos.find(n=>n.id==='TEST_NUEVA_20').name;
  const b=plain(e.send(q));assert.equal(b.ok,true,JSON.stringify(b));assert.equal(b.folderId,a.folderId);assert.equal(b.files.find(x=>x.key===p.files[0].key).fileId,a.files[0].fileId);
  const wrong=clone(q);wrong.baseRevision=q.revision;wrong.revision++;wrong.files[0].photoCategory='SCAP20_19';assert.equal(reserve(e,wrong).code,'PHOTO_CATEGORY_RECONCILIATION_REQUIRED');
});

test('official reservations are rejected for SIC, Pavements or another family before allocating its folder',()=>{
  const e=env(),{payload:p}=fixture(e,[1]);const count=e.objects.size;
  for(const [extra,expected] of [[{kind:'SIC',family:'PUENTE',identityId:p.recordId},'INVALID_NAME_RESERVATION'],[{kind:'SCAP',family:'ALCANTARILLA',familyPath:'ALCANTARILLAS'},'INVALID_FAMILY_LOCATION'],[{kind:'PAVEMENT',family:'MUESTRA',surface:'RIGIDO',tramo:'TEST_TRAMO'},'INVALID_NAME_RESERVATION']]){
    const q={...p,...extra,key:extra.kind+'/'+p.recordId};assert.equal(reserve(e,q).code,expected);assert.equal(e.objects.size,count);
  }
  let allocated=false;
  assert.throws(()=>e.context.PhotoNamesV2.reserve({...p,family:'ALCANTARILLA',photos:[{id:'TEST_BAD_FAMILY',category:'SCAP20_01'}]},
    {element:()=>{allocated=true;throw Error('MUST_NOT_ALLOCATE');}}),/INVALID_NAME_RESERVATION/);
  assert.equal(allocated,false);
});

test('foreign staging identities are rejected and cannot replace the current inspection evidence',()=>{
  const e=env(),{payload:a}=fixture(e,[1],'TEST_ORIGEN'),{payload:b}=fixture(e,[1],'TEST_DESTINO');
  const stagedA=staged(e,a);const forged=clone(b);forged.files[0].stageFileId=stagedA.files[0].stageFileId;delete forged.files[0].base64;
  assert.equal(e.send(forged).code,'STAGE_OWNERSHIP_CONFLICT');assert.equal(e.send(a).ok,true);
});

test('capability is fail-closed for original cores, missing modules, disabled naming or missing advanced Drive',()=>{
  const fragment=fs.readFileSync(path.join(__dirname,'../PARCHE_NO_DESPLEGADO/CAPACIDAD_Codigo_FRAGMENTO_NO_REEMPLAZAR.txt'),'utf8');
  const evaluate=e=>vm.runInContext('({'+fragment+'})',e.context).scapPhotoPanelVersion;
  assert.equal(evaluate(env('base')),0);assert.equal(evaluate(env()),2);
  for(const name of ['ScapPhotoPanelV2','PhotoNamesV2','VideoNamesV2','SyncV3','Drive']){const e=env();e.context[name]=undefined;assert.equal(evaluate(e),0);}
  const e=env();e.context.PropertiesService={getScriptProperties:()=>({getProperty:key=>key==='GBO_PHOTO_NAMES_V2_ENABLED'?'false':'true'})};assert.equal(evaluate(e),0);
});
