// Entirely fictitious Drive/LockService doubles. No HTTP or production access.
const {test}=require('node:test'),assert=require('node:assert/strict'),fs=require('node:fs'),path=require('node:path');
const {environment,hash}=require('./DriveV4Harness.cjs');
const fixture=()=>JSON.parse(fs.readFileSync(path.resolve(__dirname,'../docs/fotografias-5/examples/senal5.json'),'utf8'));
const candidate=path.resolve(__dirname,'../docs/hotfix-1.4.6/backend-propuesto/SyncV3.gs');
function signal(id,pr=1) {
  const p=fixture();p.recordId=id;p.key='SIC/'+id;p.revision=1;p.baseRevision=0;
  p.startPrNumber=pr;p.startDistanceM=0;p.orderAxis=pr*1000;
  p.sicRows.forEach(r=>Object.assign(r,{recordId:id,startPrNumber:pr,startDistanceM:0,orderAxis:pr*1000}));
  for(const [n,f] of p.files.entries())if(f.key.startsWith('photo/')) {
    f.key='photo/'+id+'-'+n;f.path=f.path.substring(0,f.path.lastIndexOf('/')+1)+'F_'+hash(id+'-'+n).substring(0,16)+'.jpg';
  }
  return p;
}
function reserve(e,p) {
  const a=e.context.reservePhotoNamesV2({...p,photoNamingVersion:2,photos:p.files.filter(f=>f.key.startsWith('photo/')).map(f=>({id:f.key.substring(6),category:f.photoCategory}))});
  assert.equal(a.ok,true,JSON.stringify(a));p.photoNamingVersion=2;
  for(const f of p.files.filter(f=>f.key.startsWith('photo/')))f.path=f.path.substring(0,f.path.lastIndexOf('/')+1)+a.photos.find(x=>x.id===f.key.substring(6)).name;
  return a;
}
for(const [label,adapter] of [['local',undefined],['propuesto',candidate]]) {
  test(label+': dos tablets y dos operadores, BUSY global, V2 y dos filas ordenadas sin duplicados',()=>{
    const e=environment(adapter),p=signal('TEST_AUTO_146_TABLET_A',10),q=signal('TEST_AUTO_146_TABLET_B',2);
    // GBO operator identity is data, never the LockService scope.
    for(const [payload,operator] of [[p,'ingeniero-a'],[q,'ingeniero-b']]) {
      const bytes=Buffer.from(JSON.stringify({operator,recordId:payload.recordId,synthetic:true}));
      const meta=payload.files.find(f=>f.key==='metadata');Object.assign(meta,{base64:bytes.toString('base64'),sha256:hash(bytes)});
    }
    e.state.busy=true;const objects=e.objects.size;
    const denied=e.send(p);assert.equal(denied.code,'BUSY');assert.equal(denied.retryable,true);assert.equal(e.objects.size,objects);
    e.state.busy=false;reserve(e,p);const a=e.send(p);assert.equal(a.ok,true,JSON.stringify(a));
    e.state.busy=true;assert.equal(e.context.reservePhotoNamesV2({...q,photos:[]}).code,'BUSY');
    e.state.busy=false;reserve(e,q);const b=e.send(q);assert.equal(b.ok,true,JSON.stringify(b));
    assert.notEqual(a.folderId,b.folderId);assert.equal(a.sicFiles[0].fileId,b.sicFiles[0].fileId);
    assert.deepEqual([...b.sicFiles[0].recordIds],[q.recordId,p.recordId]);
    const before=e.objects.size,updates=e.updates,retry=e.send(p);
    assert.equal(retry.alreadyExists,true);assert.equal(retry.folderId,a.folderId);assert.equal(e.objects.size,before);assert.equal(e.updates,updates);
    assert.ok(a.files.filter(f=>f.key.startsWith('photo/')).every(f=>f.logicalPath.match(/\d{3}_SEN\d+_[A-Z]{3}\.jpg$/)));
  });
  test(label+': staging BUSY recupera los mismos bytes e ID; hash inválido no escribe',()=>{
    const e=environment(adapter),p=signal('TEST_AUTO_146_STAGE'),file=p.files[0];
    const stage={syncProtocol:3,key:p.key,revision:p.revision,file};
    e.state.busy=true;assert.equal(e.context.stageRecordV3(stage).code,'BUSY');assert.equal(e.objects.size,1);
    e.state.busy=false;const a=e.context.stageRecordV3(stage),b=e.context.stageRecordV3(stage);
    assert.equal(a.ok,true,JSON.stringify(a));assert.equal(b.stageFileId,a.stageFileId);
    const count=e.objects.size;assert.equal(e.context.stageRecordV3({...stage,file:{...file,sha256:'f'.repeat(64)}}).code,'HASH_OR_FILE_SIZE');assert.equal(e.objects.size,count);
  });
  test(label+': SCAP y Pavimentos conservan identidades y ACK frente a BUSY',()=>{
    for(const kind of ['SCAP','PAVEMENT']) {
      const e=environment(adapter);let p=signal('TEST_AUTO_146_'+kind);
      if(kind==='SCAP') {
        p.kind='SCAP';p.key='SCAP/'+p.recordId;p.identityId='TEST_AUTO_146_BRIDGE';p.family='PUENTE';p.familyPath='PUENTES';p.sicRows=[];
        delete p.photoCategoryVersion;
        p.files=p.files.filter(f=>f.key==='metadata');p.files[0].path='SCAP/'+p.recordId+'/manifest.json';
      }else {
        const bytes=fs.readFileSync(path.resolve(__dirname,'../docs/drive-familias-2a/examples/pavement-a/SIC-14.xlsx'));
        p.kind='PAVEMENT';p.key='PAVEMENT/'+p.recordId;p.tramo='TRAMO_TEST';p.surface='ASFALTICO';
        for(const k of ['familyPath','technicalDeliveryVersion','orderSource','georeferencing','technicalDeliveryWithoutGps','orderAxis','roadbedCode','direction','startPrNumber','startDistanceM','photoCategoryVersion','photoHasEnds','photoClassCode','photoTypeCode'])delete p[k];
        p.family='MUESTRA';p.files=p.files.filter(f=>f.key==='metadata');p.files[0].path='DATOS_APP/M_001.json';
        p.sicRows=[{recordId:p.recordId+'_damage',format:'SIC-14',orderAxis:1000,orderSource:'CONTRACTUAL_PR',roadbedCode:'CD',direction:'INCREASING',startPrNumber:1,startDistanceM:0,sha256:hash(bytes),base64:bytes.toString('base64')}];
      }
      e.state.busy=true;assert.equal(e.send(p).code,'BUSY');e.state.busy=false;
      const a=e.send(p);assert.equal(a.ok,true,JSON.stringify(a));const b=e.send(p);assert.equal(b.alreadyExists,true);assert.equal(a.folderId,b.folderId);
    }
  });
}
test('propuesta mide locks sin datos sensibles y mantiene el motor transaccional idéntico',()=>{
  const e=environment(candidate),events=[];e.context.console={info:s=>events.push(JSON.parse(s))};
  const p=signal('TEST_AUTO_146_METRICS');e.state.busy=true;e.send(p);e.state.busy=false;reserve(e,p);e.send(p);
  assert.ok(events.length>=3);assert.ok(events.every(x=>Object.keys(x).sort().join()==='acquired,event,heldMs,operation,phases,prepareMs,waitMs'));
  assert.equal(fs.readFileSync(path.resolve(__dirname,'../docs/hotfix-1.4.6/backend-propuesto/SyncV3Core.gs'),'utf8'),fs.readFileSync(__dirname+'/SyncV3Core.js','utf8'));
});
module.exports={signal,reserve};
