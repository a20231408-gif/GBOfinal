const {test}=require('node:test'),assert=require('node:assert/strict'),fs=require('node:fs'),path=require('node:path');
const {environment,hash}=require('./DriveV4Harness.cjs'),core=require('./SyncV3Core.js');
const examples=path.resolve(__dirname,'../docs/drive-familias-2a/examples');
function fixture(id) {return JSON.parse(fs.readFileSync(path.join(examples,id,'payload.json')));}
function next(p,patch={}) {return {...structuredClone(p),revision:p.revision+1,baseRevision:p.revision,...patch};}
test('two tablets allocate culvert 1 and 2; earlier PR cannot renumber the first',()=>{
 const e=environment(),b=e.send(fixture('culvert-a')),a=e.send(fixture('culvert-b'));
 assert.equal(b.ok,true,JSON.stringify(b));assert.equal(a.ok,true,JSON.stringify(a));
 assert.equal(b.logicalPath,'PE-22A/ALCANTARILLAS/ALCANTARILLA 1');assert.equal(a.logicalPath,'PE-22A/ALCANTARILLAS/ALCANTARILLA 2');
 assert.equal(a.files.find(f=>f.key.startsWith('photo/')).logicalPath.includes('/ENTRADA/'),true);
 assert.equal(a.sicFiles[0].logicalPath,'PE-22A/EXCEL_SIC/SIC-18.xlsx');assert.equal(a.sicFiles[0].fileId,b.sicFiles[0].fileId);
 assert.deepEqual([...a.sicFiles[0].recordIds],['culvert-b','culvert-a']);
 const retry=e.send(fixture('culvert-a'));assert.equal(retry.alreadyExists,true);assert.equal(retry.folderId,b.folderId);
 fs.writeFileSync(path.join(examples,'local-drive-v4-receipts.json'),JSON.stringify({source:'LOCAL DRIVE DOUBLES, NOT REMOTE',b,a,retry},null,2));
});
test('progressive edit keeps UUID folderId number and fileIds',()=>{
 const e=environment(),p=fixture('culvert-a'),a=e.send(p);assert.equal(a.ok,true);
 e.send(fixture('culvert-b'));const changed=next(p,{startPrNumber:1,startDistanceM:0,orderAxis:1000});
 changed.sicRows=changed.sicRows.map(r=>({...r,startPrNumber:1,startDistanceM:0,orderAxis:1000}));
 const b=e.send(changed);assert.equal(b.ok,true,JSON.stringify(b));assert.equal(a.folderId,b.folderId);assert.equal(a.logicalPath,b.logicalPath);assert.equal(a.files[0].fileId,b.files[0].fileId);
 assert.deepEqual([...b.sicFiles[0].recordIds],['culvert-a','culvert-b']);
});
test('SIC20 families and SIC21 grouped classes are separate with no subtype folders',()=>{
 const e=environment();for(const id of ['ford-a','wall-a','tunnel-a','mark-a','safety-a','stud-a']) {
  const p=fixture(id),a=e.send(p);assert.equal(a.ok,true,JSON.stringify(a));assert.equal(a.logicalPath,p.routeCode+'/'+p.familyPath+'/'+p.family+' 1');assert.equal(a.logicalPath.includes('SIB'),false);
 }
});
test('independent SIC08 and SIC09 deliver actual Android JPEG and Excel without GPS',()=>{
 const e=environment();for(const id of ['SIC-08','SIC-09']) {
  const p=fixture(id);assert.equal(hash(core.canonical(p)),p.contentHash,'Android/backend canonical hash');
  const a=e.send(p);assert.equal(a.ok,true,JSON.stringify(a));assert.equal(a.externalGeoreferencingStatus,'PENDING');assert.equal(a.georeferencingStatus,'PENDING_EXTERNAL');
  assert.equal(a.logicalPath.includes('SIB'),false);assert.equal(a.sicFiles[0].logicalPath,p.routeCode+'/EXCEL_SIC/'+id+'.xlsx');
 }
});
test('all generated Android payloads agree with server canonical hash',()=>{
 for(const id of ['culvert-a','culvert-b','ford-a','wall-a','tunnel-a','mark-a','safety-a','stud-a']) {const p=fixture(id);assert.equal(hash(core.canonical(p)),p.contentHash,id);}
});
test('busy script lock cannot allocate or create folders',()=>{
 const e=environment();e.state.busy=true;assert.equal(e.send(fixture('culvert-a')).code,'BUSY');assert.equal(e.objects.size,1);
});
test('same revision conflict cannot overwrite a confirmed delivery',()=>{
 const e=environment(),p=fixture('culvert-a');const a=e.send(p);const q=structuredClone(p);q.startPrNumber=1;q.sicRows.forEach(r=>r.startPrNumber=1);
 assert.equal(e.send(q).code,'REVISION_CONFLICT');assert.equal(e.send(p).folderId,a.folderId);
});
test('failure during first write retries durable reservation without another element',()=>{
 const e=environment(),p=fixture('culvert-a');e.state.failAfter=1;assert.equal(e.send(p).ok,false);
 const b=e.send(fixture('culvert-b'));assert.equal(b.ok,true,JSON.stringify(b));assert.equal(b.logicalPath.endsWith('ALCANTARILLA 2'),true);
 const a=e.send(p);assert.equal(a.ok,true,JSON.stringify(a));assert.equal(a.logicalPath.endsWith('ALCANTARILLA 1'),true);
 const culverts=[...e.objects.values()].filter(x=>x.folders && /^ALCANTARILLA [12]$/.test(x.name));assert.equal(culverts.length,2);
});
test('new requests reject SIB/defaults and mismatched family before creating technical files',()=>{
 const e=environment(),p=fixture('culvert-a');assert.equal(e.send({...p,sibCode:'SIB-02'}).code,'INVALID_FAMILY_LOCATION');
 assert.equal(e.send({...p,familyPath:'MUROS'}).code,'INVALID_FAMILY_LOCATION');
 assert.equal(e.send({...p,pathVersion:3,sibCode:'SIB-02'}).code,'LEGACY_READ_ONLY');
});
test('historical SIB receipt reads unchanged, migration never automatic',()=>{
 const e=environment(),p=fixture('culvert-a');const lock=e.context.LockService.getScriptLock();assert.ok(lock.tryLock(1));
 const route=e.context.getOrCreateFolder(e.root,'PE-22A'),sib=e.context.getOrCreateFolder(route,'SIB-02');const marker=sib.createFile(e.context.Utilities.newBlob('historical','text/plain','keep.txt'));
 const legacy={...p,pathVersion:3,sibCode:'SIB-02'};delete legacy.familyPath;legacy.contentHash=hash(core.canonical(legacy));
 const old={key:p.key,revision:p.revision,pathVersion:3,contentHash:legacy.contentHash,receipt:{ok:true,syncProtocol:3,pathVersion:3,folderId:sib.getId(),logicalPath:'PE-22A/SIB-02/ALCANTARILLA 1'}};
 const registry=e.context.getOrCreateFolder(e.root,'._GBO_SYNC_V3');registry.createFile(e.context.Utilities.newBlob(JSON.stringify(old),'application/json','record_'+hash(p.key)+'.json'));lock.releaseLock();
 assert.equal(e.send(legacy).alreadyExists,true);assert.equal(e.send(p).code,'HISTORICAL_MIGRATION_REQUIRED');assert.equal(marker.getBlob().getDataAsString(),'historical');assert.equal(sib.parent,route);
 const other=e.send(fixture('culvert-b'));assert.equal(other.ok,true);assert.equal(sib.files.length,1);assert.equal(sib.folders.length,0);
});
test('technical order is numeric, direction-sensitive and ties use UUID',()=>{
 const row=(id,pr,direction='INCREASING',roadbedCode='CD')=>({recordId:id,startPrNumber:pr,startDistanceM:0,direction,roadbedCode});
 assert.deepEqual([row('b',10),row('a',2),row('d',2,'DECREASING'),row('c',10,'DECREASING')].sort(core.compareTechnical).map(x=>x.recordId),['c','d','a','b']);
});
test('future coded photo categories and multiple photographs require no backend redesign',()=>{
 const e=environment(),p=fixture('culvert-a');const photo=p.files.find(f=>f.key.startsWith('photo/'));p.files.push({...photo,key:'photo/second',path:'INICIO/F_second.jpg'});
 const a=e.send(p);assert.equal(a.ok,true,JSON.stringify(a));assert.equal(a.files.filter(f=>f.key.startsWith('photo/')).length,2);
 const r=e.send(p);assert.equal(r.files.length,a.files.length);assert.deepEqual(JSON.parse(JSON.stringify(r.files)),JSON.parse(JSON.stringify(a.files)));
});

test('SCAP inspections share a stable bridge identity but keep individual manifests and categories',()=>{
 const e=environment();function scap(id) {
  const p=fixture('culvert-a');p.kind='SCAP';p.recordId=id;p.key='SCAP/'+id;p.identityId='bridge-identity';p.family='PUENTE';p.familyPath='PUENTES';p.sicRows=[];
  const meta=p.files.find(f=>f.key==='metadata'),photo=p.files.find(f=>f.key.startsWith('photo/'));
  meta.path='SCAP/'+id+'/manifest.json';photo.key='photo/'+id;photo.path='AGUAS_ARRIBA/F_'+id+'.jpg';return p;
 }
 const a=e.send(scap('inspection-a')),b=e.send(scap('inspection-b'));assert.equal(a.ok,true,JSON.stringify(a));assert.equal(b.ok,true,JSON.stringify(b));
 assert.equal(a.folderId,b.folderId);assert.equal(a.logicalPath,'PE-22A/PUENTES/PUENTE 1');assert.notEqual(a.files[0].fileId,b.files[0].fileId);
 assert.equal(b.files.find(f=>f.key==='metadata').logicalPath.endsWith('/SCAP/inspection-b/manifest.json'),true);
});
test('pavement SIC14 native two-row header consolidates per route without changing template',()=>{
 const e=environment(),bytes=fs.readFileSync(path.join(examples,'pavement-a','SIC-14.xlsx'));
 function sample(id,pr) {
  const p=fixture('culvert-a');p.kind='PAVEMENT';p.recordId=id;p.key='PAVEMENT/'+id;p.tramo='TRAMO';p.surface='ASFALTICO';
  for(const k of ['family','familyPath','technicalDeliveryVersion','orderSource','georeferencing','technicalDeliveryWithoutGps','orderAxis','roadbedCode','direction','startPrNumber','startDistanceM'])delete p[k];
  p.family='MUESTRA';const native=Buffer.from(JSON.stringify({sampleId:id,latitude:null,longitude:null}));p.files=[{key:'sample_json',path:'DATOS_APP/M_001.json',mimeType:'application/json',sha256:hash(native),base64:native.toString('base64')}];
  p.sicRows=[{recordId:id+'_damage',format:'SIC-14',orderAxis:pr*1000,orderSource:'CONTRACTUAL_PR',roadbedCode:'CD',direction:'INCREASING',startPrNumber:pr,startDistanceM:0,sha256:hash(bytes),base64:bytes.toString('base64')}];return p;
 }
 const a=e.send(sample('sample-a',10)),b=e.send(sample('sample-b',2));assert.equal(a.ok,true,JSON.stringify(a));assert.equal(b.ok,true,JSON.stringify(b));
 assert.equal(a.logicalPath,'PE-22A/EVALUACION_SUPERFICIAL/TRAMO/ASFALTICO/MUESTRA 1');assert.equal(b.logicalPath,'PE-22A/EVALUACION_SUPERFICIAL/TRAMO/ASFALTICO/MUESTRA 2');assert.equal(a.sicFiles[0].fileId,b.sicFiles[0].fileId);assert.equal(b.sicFiles[0].logicalPath,'PE-22A/EXCEL_SIC/SIC-14.xlsx');assert.deepEqual([...b.sicFiles[0].recordIds],['sample-b_damage','sample-a_damage']);
 const book=e.context.v3Store;const data=Buffer.from(e.objects.get(b.sicFiles[0].fileId).getBlob().getBytes());
 fs.writeFileSync(path.join(examples,'SIC-14-two-samples-local.xlsx'),data);
});

test('another UUID cannot insert or replace rows in the consolidated inventory',()=>{
 const e=environment(),p=fixture('culvert-a');p.sicRows[0].recordId='culvert-b';assert.equal(e.send(p).code,'ROW_OWNER_MISMATCH');
});
