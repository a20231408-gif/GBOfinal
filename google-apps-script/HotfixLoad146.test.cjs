const {test}=require('node:test'),assert=require('node:assert/strict'),fs=require('node:fs'),path=require('node:path');
const {performance}=require('node:perf_hooks');
const {environment,hash}=require('./DriveV4Harness.cjs'),{signal,reserve}=require('./Hotfix146.test.cjs'),core=require('./SyncV3Core.js');
const output=path.resolve(__dirname,'../tmp/hotfix-1.4.6-20261015');
function sample(n) {
  const p=signal('TEST_AUTO_146_PAVEMENT_'+n,20-n);p.kind='PAVEMENT';p.key=p.kind+'/'+p.recordId;p.tramo='TEST_AUTO_146';p.surface='ASFALTICO';p.family='MUESTRA';
  for(const key of ['familyPath','technicalDeliveryVersion','orderSource','georeferencing','technicalDeliveryWithoutGps','orderAxis','roadbedCode','direction','startPrNumber','startDistanceM','photoCategoryVersion','photoHasEnds','photoClassCode','photoTypeCode'])delete p[key];
  const bytes=fs.readFileSync(path.resolve(__dirname,'../docs/drive-familias-2a/examples/pavement-a/SIC-14.xlsx'));
  p.sicRows=[{recordId:p.recordId+'_damage',format:'SIC-14',orderAxis:(20-n)*1000,orderSource:'CONTRACTUAL_PR',roadbedCode:'CD',direction:'INCREASING',startPrNumber:20-n,startDistanceM:0,sha256:hash(bytes),base64:bytes.toString('base64')}];
  for(const f of p.files.filter(f=>f.key.startsWith('photo/'))){f.photoCategory='GENERAL';f.path='PANEL_FOTOGRAFICO/'+f.path.split('/').pop();}
  return p;
}
for(const [label,adapter] of [['local',undefined],['propuesto',path.resolve(__dirname,'../docs/hotfix-1.4.6/backend-propuesto/SyncV3.gs')]])test(label+': carga 17 SIC + 11 muestras, dos tablets, BUSY y respuestas perdidas sin duplicar',()=>{
  const e=environment(adapter),events=[];e.context.console={info:s=>events.push(JSON.parse(s))};
  const payloads=[...Array.from({length:17},(_,n)=>signal('TEST_AUTO_146_SIC_'+(n+1),20-n)),...Array.from({length:11},(_,n)=>sample(n+1))];
  let stageCalls=0,commitCalls=0,busyResponses=0,lostStage=0,lostCommit=0,wireBytes=0;
  const timings=[];const start=performance.now();
  for(const [index,p] of payloads.entries()) {
    const bytes=Buffer.from(JSON.stringify({synthetic:true,operator:index%2?'ENGINEER_B':'ENGINEER_A',device:index%2?'TABLET_B':'TABLET_A',recordId:p.recordId}));
    const metadata=p.files.find(f=>f.key==='metadata');Object.assign(metadata,{base64:bytes.toString('base64'),sha256:hash(bytes)});
    const recordStart=performance.now();
    if(index%5===0){e.state.busy=true;const objects=e.objects.size;assert.equal(e.send(p).code,'BUSY');assert.equal(e.objects.size,objects);e.state.busy=false;busyResponses++;}
    const reservation=reserve(e,p);assert.ok(reservation.photos.length>0);
    const request={action:'reservePhotoNamesV2',photoNamingVersion:2,photos:reservation.photos.map(f=>({id:f.id,category:f.category}))};
    for(const k of ['pathVersion','kind','key','recordId','revision','baseRevision','identityId','routeCode','familyPath','family','tramo','surface'])if(k in p)request[k]=p[k];
    wireBytes+=Buffer.byteLength(JSON.stringify(request));
    for(const [fileIndex,file] of p.files.entries()) {
      const stage={action:'stageRecordV3',syncProtocol:3,key:p.key,revision:p.revision,file};
      const before=e.context.stageRecordV3(stage);stageCalls++;wireBytes+=Buffer.byteLength(JSON.stringify(stage));assert.equal(before.ok,true,JSON.stringify(before));
      if(index%7===0 && fileIndex===0) {
        // Simulate loss after a real staging write, before the tablet sees its response.
        const replay=e.context.stageRecordV3(stage);stageCalls++;lostStage++;wireBytes+=Buffer.byteLength(JSON.stringify(stage));assert.equal(replay.stageFileId,before.stageFileId);
      }
      file.stageFileId=before.stageFileId;delete file.base64;
    }
    const a=e.send(p);commitCalls++;wireBytes+=Buffer.byteLength(JSON.stringify(p));assert.equal(a.ok,true,JSON.stringify(a));
    if(index%7===0){const updates=e.updates,replay=e.send(p);commitCalls++;lostCommit++;wireBytes+=Buffer.byteLength(JSON.stringify(p));assert.equal(replay.alreadyExists,true);assert.equal(replay.folderId,a.folderId);assert.equal(e.updates,updates);}
    assert.equal(a.revision,p.revision);assert.equal(a.contentHash,hash(core.canonical(p)));assert.equal(a.files.length,p.files.length);
    timings.push({kind:p.kind,elapsedMs:performance.now()-recordStart,files:p.files.length});
  }
  const manifests=[...e.objects.values()].filter(f=>f.b && /^record_[a-f0-9]{64}\.json$/.test(f.name)).map(f=>JSON.parse(f.getBlob().getDataAsString()));
  assert.equal(manifests.length,28);assert.equal(new Set(manifests.map(m=>m.key)).size,28);
  for(const format of ['SIC-22','SIC-14']) {
    const owners=manifests.flatMap(m=>m.sicRows.filter(r=>r.format===format));const latest=manifests.filter(m=>m.receipt.sicFiles.some(f=>f.format===format)).at(-1).receipt.sicFiles.find(f=>f.format===format);
    assert.deepEqual([...latest.recordIds],owners.sort(core.compareTechnical).map(r=>r.recordId));
    assert.equal(owners.length,format==='SIC-22'?17:11);
  }
  const definitive=manifests.flatMap(m=>m.files.map(f=>f.fileId));assert.equal(new Set(definitive).size,definitive.length);
  const phases={};for(const event of events)for(const [name,value] of Object.entries(event.phases)){const row=phases[name] || (phases[name]={count:0,ms:0});row.count+=value.count;row.ms+=value.ms;}
  const report={environment:'NODE + PYTHON ZIP + IN-MEMORY DRIVE/LOCK; NOT GOOGLE TIMINGS',adapter:label,elapsedMs:performance.now()-start,
    records:28,sic:17,pavements:11,confirmed:28,definitiveFiles:definitive.length,stageCalls,commitCalls,busyResponses,lostStageResponses:lostStage,lostCommitResponses:lostCommit,wireBytesWithoutAuthToken:wireBytes,recordsMeasured:timings,serverPhaseTotals:phases,
    bandwidthModelOnly:[2,8,1000].map(mbps=>({mbps,minimumTransferSeconds:wireBytes*8/(mbps*1000000),note:'Ideal serialization bound; excludes latency, locks, CPU, server and retries'}))};
  fs.writeFileSync(path.join(output,'load-backend-'+label+'.json'),JSON.stringify(report,null,2));
});
