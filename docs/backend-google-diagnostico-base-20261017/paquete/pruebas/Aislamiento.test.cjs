const {test}=require('node:test'),assert=require('node:assert/strict'),fs=require('node:fs'),path=require('node:path'),vm=require('node:vm');
const {env,sha,zip,ROOT}=require('./MockGoogle.cjs'),runner=require('../ejecutor/ejecutar.cjs');
const fixture=require('../ejecutor/fixtures/records.json'),clone=x=>JSON.parse(JSON.stringify(x));
const ok=a=>{assert.equal(a.ok,true,JSON.stringify(a));return a;};
for(const variant of ['BASE','CANDIDATO']){
 test(variant+': eight complete modules compile, capabilities remain V20 and isolated',()=>{const e=env(variant);e.start();const cap=JSON.parse(e.context.doGet().getContent());runner.checkCapability(cap,'TEST_SCRIPT_ONLY');assert.equal(e.writes.length,1);});
 test(variant+': wrong root, script ID, foreign properties and missing token fail before first write',()=>{
  for(const change of [e=>e.props.GBO_TEST_SCRIPT_ID='WRONG',e=>e.props.GBO_DRIVE_API_TOKEN='FORBIDDEN',e=>delete e.props.GBO_TEST_TOKEN]){const e=env(variant);change(e);assert.throws(()=>e.start());assert.equal(e.writes.length,0);}
  const e=env(variant,{badRoot:true});assert.throws(()=>e.start(),/TEST_ROOT_MISMATCH/);assert.equal(e.writes.length,0);
 });
 test(variant+': foreign file, update, move, copy, orphan and shortcuts blocked without writes',()=>{
  const e=env(variant);e.start();const n=e.writes.length;
  for(const f of [()=>e.context.GboTestDriveApp.getFileById('FOREIGN_FILE').setContent('BAD'),()=>e.context.GboTestDrive.Files.update({name:'BAD'},'FOREIGN_FILE',e.blob('BAD'),{}),()=>e.context.GboTestDriveApp.getFolderById(ROOT).moveTo(e.context.GboTestDriveApp.getFolderById('OUTSIDE_ROOT')),()=>e.context.GboTestDriveApp.getFolderById(ROOT).createFile(e.blob('x')).makeCopy('BAD',null)]){
   const before=e.writes.length;assert.throws(f);assert.ok(e.writes.length<=before+1); // Last scenario creates an authorized source only.
  }
  assert.ok(e.writes.every(x=>x.parent!=='OUTSIDE_ROOT'&&x.id!=='FOREIGN_FILE'));
  const inside=e.context.GboTestDriveApp.getFolderById(ROOT).createFile(e.blob('SAFE','text/plain','safe'));const native=e.objects.get(inside.getId());native.shortcutDetails={targetId:'FOREIGN_FILE'};const before=e.writes.length;assert.throws(()=>inside.setContent('BAD'));assert.equal(e.writes.length,before);
  delete native.shortcutDetails;native.parent=null;assert.throws(()=>inside.setContent('BAD'));assert.equal(e.writes.length,before);assert.ok(n>=1);
 });
 test(variant+': old run fence, outside-run IDs and variant switching with in-flight request blocked',()=>{
  const e=env(variant);const first=e.start(),f=e.context.GboTestDriveApp.getFolderById(ROOT).createFile(e.blob('old','text/plain','old')).getId();
  e.props.GBO_TEST_INFLIGHT='1';assert.throws(()=>e.start(),/INFLIGHT/);e.props.GBO_TEST_INFLIGHT='0';e.start();const n=e.writes.length;
  assert.equal(e.post({action:'testSnapshot'},first).code,'TEST_STALE_RUN');assert.throws(()=>e.context.GboTestDriveApp.getFileById(f));assert.equal(e.writes.length,n);
 });
 test(variant+': request-supplied foreign staging or finish IDs fail before any Drive write',()=>{
  const e=env(variant);e.start();const p=clone(fixture.records[0]);p.files[0].stageFileId='FOREIGN_FILE';const n=e.writes.length;
  assert.equal(e.post(p).code,'TEST_OUTSIDE_RUN');assert.equal(e.writes.length,n);
  assert.equal(e.post({action:'finishVideoV1',parentKey:'SCAP/TEST_SCAP_A',fileId:'FOREIGN_FILE'}).code,'TEST_OUTSIDE_RUN');assert.equal(e.writes.length,n);
 });
 test(variant+': BUSY writes nothing, staged SHA and replay ACK durable; safe numeric diagnostics',()=>{
  const e=env(variant);e.start();const p=e.prepare(fixture.records[0]),n=e.writes.length;e.hold(true);
  const busy=e.post({...p,action:'commitRecordV3'});assert.equal(busy.code,'BUSY');assert.equal(e.writes.length,n);e.hold(false);
  for(const f of p.files){const s=ok(e.post({action:'stageRecordV3',syncProtocol:3,key:p.key,revision:p.revision,file:f}));assert.equal(s.sha256,f.sha256);f.stageFileId=s.stageFileId;delete f.base64;}
  const a=runner.ackFor(p,ok(e.post(p))),writes=e.writes.length,b=runner.ackFor(p,ok(e.post(p)));assert.equal(b.alreadyExists,true);assert.equal(a.folderId,b.folderId);assert.equal(e.writes.length,writes);
  assert.ok(a._gboTest.metrics.phases.validation);assert.ok(a._gboTest.metrics.phases.consolidation);assert.ok(a._gboTest.metrics.phases['store.unpackWorkbook']);assert.ok(a._gboTest.metrics.locks.length);
  assert.ok(!JSON.stringify(a._gboTest.metrics).includes(p.recordId));assert.equal(e.props.GBO_TEST_INFLIGHT,'0');
 });
 test(variant+': SIC, SCAP, pavement docs/photos V2, historical observations and explicit blank retain identities',()=>{
  const e=env(variant);e.start();for(const original of fixture.records){const p=e.prepare(original);const a=ok(e.post(p));runner.ackFor(p,a);}
  const snap=ok(e.post({action:'testSnapshot'}));assert.equal(snap.manifests.length,8);assert.deepEqual(snap.manifests.flatMap(m=>m.receipt.sicFiles).find(f=>f.format==='SIC-18'&&f.recordIds.length===4).recordIds,['TEST_SIC_A','TEST_SIC_B','TEST_SIC_D','TEST_SIC_C']);
  const legacy=e.prepare(fixture.legacyEdit),a=ok(e.post(legacy));const sheet=zip('unzip',Buffer.from(e.objects.get(a.sicFiles[0].fileId).getBlob().getBytes()).toString('base64')).find(x=>x.name==='xl/worksheets/sheet1.xml');assert.ok(Buffer.from(sheet.base64,'base64').toString().includes('PINTADO OK'));
  const b=ok(e.post(e.prepare(fixture.blankEdit)));assert.equal(b.folderId,a.folderId);assert.equal(b.sicFiles[0].fileId,a.sicFiles[0].fileId);assert.equal(ok(e.post({action:'testSnapshot'})).manifests.length,8);
 });
 test(variant+': video resumable URL confined, begin/finish losses preserve one binary',()=>{
  const e=env(variant);e.start();const p=e.prepare(fixture.records[6]);ok(e.post(p));const bytes=Buffer.from('FICTITIOUS_OPAQUE_VIDEO'),req={action:'beginVideoV1',videoProtocol:1,videoNamingVersion:2,parentKey:p.key,parentRevision:1,mediaId:'TEST_VIDEO',category:'UPSTREAM',categoryLabel:'Aguas arriba ficticio',mimeType:'video/mp4',sizeBytes:bytes.length,sha256:sha(bytes)};
  const a=ok(e.post(req)),b=ok(e.post(req));assert.equal(a.fileId,b.fileId);const f=e.objects.get(a.fileId);f.b=e.blob(bytes,'video/mp4',f.name);const ack=ok(e.post({...req,action:'finishVideoV1',fileId:a.fileId}));assert.equal(ack.complete,true);assert.equal(ok(e.post(req)).complete,true);
  assert.throws(()=>e.context.GboTestUrlFetch.fetch('https://example.invalid/',{method:'patch',followRedirects:false}));assert.throws(()=>e.context.GboTestDrive.Files.create({id:'FOREIGN_FILE',name:'bad',parents:[e.run.folderId]},null,{}));
 });
 test(variant+': failed transaction rolls back, retry recovers and local-copy replay has no new files',()=>{
  const e=env(variant);e.start();const p=e.prepare(fixture.records[4]),a=ok(e.post(p));const q=clone(p);q.revision=2;q.baseRevision=1;q.contentHash=sha(e.context.SyncV3.canonical(q));e.fail();assert.equal(e.post(q).ok,false);
  const b=ok(e.post(q));assert.equal(b.folderId,a.folderId);const n=e.writes.length,replay=ok(e.post(q));assert.equal(replay.alreadyExists,true);assert.equal(e.writes.length,n);assert.ok(ok(e.post({action:'testSnapshot'})).journals.every(j=>j.status!=='PENDING'));
 });
}
test('runner rejects normal/production capability and unrelated endpoint before any POST',()=>{
 for(const u of ['https://example.com/exec','https://script.google.com/macros/s/x/exec?token=BAD','http://script.google.com/macros/s/x/exec'])assert.throws(()=>runner.endpoint(u));
 assert.throws(()=>runner.checkCapability({ok:true,syncProtocol:3},'TEST_SCRIPT_ONLY'));
});
test('load corpus has unique UUIDs and owned SIC/PAVEMENT rows without modifying original fixtures',()=>{
 const original=JSON.stringify(fixture),items=runner.extraRecords(fixture,3);assert.equal(items.length,9);assert.equal(new Set(items.map(p=>p.key)).size,9);
 for(const p of items){assert.match(p.recordId,/^TEST_LOAD_/);assert.equal(p.key,p.kind+'/'+p.recordId);for(const r of p.sicRows)assert.ok(p.kind==='PAVEMENT'?r.recordId.startsWith(p.recordId+'_'):r.recordId===p.recordId);}
 assert.equal(JSON.stringify(fixture),original);assert.throws(()=>runner.extraRecords(fixture,41));
});
test('candidate can fence and resume prior BASE history inside root; outside history rejected',()=>{
 const e=env('CANDIDATO');e.start();const id='b'.repeat(32),f=e.root.createFolder('TEST_BASE_'+id);e.props.GBO_TEST_LAST_BASE_RUN=JSON.stringify({variant:'BASE',runId:id,epoch:'OLD_EPOCH',folderId:f.id});
 const n=e.writes.length,r=e.context.gboTestResumePreviousBase();assert.equal(r.folderVariant,'BASE');assert.equal(r.variant,'CANDIDATO');assert.equal(e.writes.length,n);assert.equal(ok(e.post({action:'testSnapshot'})).files.length,0);
 e.props.GBO_TEST_LAST_BASE_RUN=JSON.stringify({variant:'BASE',runId:id,epoch:'OLD',folderId:'OUTSIDE_ROOT'});assert.throws(()=>e.context.gboTestResumePreviousBase());assert.equal(e.writes.length,n);
});
test('only two business modules differ between variants; guards identical and no native write bypass',()=>{
 const base=path.resolve(__dirname,'..'),a=path.join(base,'BASE_V20_PRUEBAS'),b=path.join(base,'CANDIDATO_OPTIMIZADO_PRUEBAS');const changes=[];
 for(const f of fs.readdirSync(a)){const x=fs.readFileSync(path.join(a,f),'utf8'),y=fs.readFileSync(path.join(b,f),'utf8');if(x!==y)changes.push(f);if(f.endsWith('.gs'))new vm.Script(x);if(!f.startsWith('Test'))assert.ok(!/\b(?:DriveApp|Drive\.Files|UrlFetchApp|PropertiesService|LockService|Utilities)\s*\./.test(x),f);}
 assert.deepEqual(changes.sort(),['SyncV3.gs','SyncV3Core.gs','TestVariant.gs']);
});

