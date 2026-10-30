const {test}=require('node:test'),assert=require('node:assert/strict'),fs=require('node:fs'),path=require('node:path'),crypto=require('node:crypto');
const {env,sha,ROOT}=require('./MockGoogle.cjs'),r=require('../ejecutor/recuperacion.cjs'),runner=require('../ejecutor/ejecutar.cjs'),fixture=require('../ejecutor/fixtures/records.json');
const clone=x=>JSON.parse(JSON.stringify(x)),ok=a=>{assert.equal(a.ok,true,a.code);return a;};
const out=path.join(__dirname,'salida-local-v2');fs.mkdirSync(out,{recursive:true});
function inspection(p,a){return {key:p.key,revision:p.revision,contentHash:p.contentHash,folderId:a.folderId,files:a.files.map(f=>({key:f.key,fileId:f.fileId,folderId:f.folderId,logicalPath:f.logicalPath,sha256:f.sha256})),stages:p.files.filter(f=>f.stageFileId).map(f=>({key:f.key,stageFileId:f.stageFileId,sha256:f.sha256})),rows:p.sicRows.map(x=>({recordId:x.recordId,format:x.format,sha256:x.sha256}))};}
for(const variant of ['BASE','CANDIDATO']){
 test(variant+': cached reads cannot authorize writes after an external move or poisoned cache',()=>{
  const e=env(variant);e.start();const f=e.context.GboTestDriveApp.getFolderById(ROOT).createFile(e.blob('SAFE','text/plain','safe'));
  const l=e.context.GboTestLocks.getScriptLock();assert.equal(l.tryLock(30000),true);f.getName();f.getName();e.objects.get(f.getId()).parent=e.outside;
  const n=e.writes.length;assert.throws(()=>f.setContent('BAD'));assert.equal(e.writes.length,n);
  e.context.gboTestGuard_.reads.FOREIGN_FILE=true;assert.throws(()=>e.context.GboTestDrive.Files.update({name:'BAD'},'FOREIGN_FILE',e.blob('BAD'),{}));assert.equal(e.writes.length,n);l.releaseLock();assert.equal(Object.keys(e.context.gboTestGuard_.reads).length,0);
 });
 test(variant+': run folder moved outside authorized root is rejected before a mutation',()=>{
  const e=env(variant);e.start();const f=e.context.GboTestDriveApp.getFolderById(ROOT).createFile(e.blob('SAFE','text/plain','safe'));e.objects.get(e.run.folderId).parent=e.outside;const n=e.writes.length;assert.throws(()=>f.setContent('BAD'),/TEST_RUN_MOVED/);assert.equal(e.writes.length,n);
 });
 test(variant+': inspection reads COMPLETE journal and stored ACK=false without any Drive or property write',()=>{
  const e=env(variant);e.start();const p=e.prepare(fixture.records[0]);
  for(const f of p.files){const staged=ok(e.post({action:'stageRecordV3',syncProtocol:3,key:p.key,revision:p.revision,file:f}));f.stageFileId=staged.stageFileId;delete f.base64;}
  const a=ok(e.post(p)),n=e.writes.length,props=JSON.stringify(e.props);
  const d=ok(e.post({action:'testInspectRecords',records:[inspection(p,a)],expectedOrder:[p.recordId]}));assert.equal(d.safeToResume,true);assert.equal(d.results[0].storedAckAlreadyExists,false);assert.equal(e.writes.length,n);assert.equal(JSON.stringify(e.props),props);
  const stage=e.objects.get(p.files[0].stageFileId),description=stage.description;stage.description=JSON.stringify({...JSON.parse(description),revision:99});
  const wrong=ok(e.post({action:'testInspectRecords',records:[inspection(p,a)],expectedOrder:[p.recordId]}));assert.equal(wrong.safeToResume,false);assert.equal(wrong.results[0].checks.stagingOwnershipMatch,false);assert.equal(e.writes.length,n);stage.description=description;
  e.props.GBO_TEST_INFLIGHT='1';assert.equal(e.post({action:'testInspectRecords',records:[inspection(p,a)],expectedOrder:[p.recordId]}).code,'TEST_DIAGNOSTIC_INFLIGHT');assert.equal(e.props.GBO_TEST_INFLIGHT,'1');assert.equal(e.writes.length,n);
 });
 test(variant+': inspection never recovers a PENDING journal, and blocks hash/identity mismatches',()=>{
  const e=env(variant);e.start();const p=e.prepare(fixture.records[0]),a=ok(e.post(p));const journal=[...e.objects.values()].find(f=>f.name?.startsWith('journal_'));const body=JSON.parse(journal.getBlob().getDataAsString());body.status='PENDING';journal.setContent(JSON.stringify(body));const n=e.writes.length;
  const d=ok(e.post({action:'testInspectRecords',records:[inspection(p,a)],expectedOrder:[p.recordId]}));assert.equal(d.safeToResume,false);assert.equal(d.results[0].checks.journalComplete,false);assert.equal(e.writes.length,n);assert.equal(JSON.parse(journal.getBlob().getDataAsString()).status,'PENDING');
 });
}
test('ACK contract flags false alreadyExists, folder, identity, revision, hash and absent metrics with safe conditions',()=>{
 const e=env();e.start();const p=e.prepare(fixture.records[0]),a=ok(e.post(p)),b=ok(e.post(p));
 for(const [field,value,code] of [['alreadyExists',false,'ACK_ALREADY_EXISTS_NOT_TRUE'],['folderId','SENSITIVE_OTHER_ID','ACK_FOLDER_MISMATCH'],['key','SIC/PRIVATE_UUID','ACK_IDENTITY_MISMATCH'],['revision',99,'ACK_REVISION_MISMATCH'],['contentHash','SENSITIVE_HASH','ACK_CONTENT_HASH_MISMATCH']]){const q=clone(b);q[field]=value;assert.throws(()=>r.validateReplay(p,q,a),error=>{const s=r.safeFailure(error,'LOCAL_COPY_ACK_VALIDATION');assert.equal(s.condition,code);assert.ok(!JSON.stringify(s).includes('SENSITIVE'));assert.ok(!JSON.stringify(s).includes('PRIVATE_UUID'));return true;});}
 assert.throws(()=>r.validateTelemetry({ok:true},e.run,'TRACE'),error=>error.code==='HARNESS_METRICS_MISSING');
 const failure=r.safeFailure(Error('TOKEN_SECRET URL_SECRET'), 'STAGE');assert.ok(!JSON.stringify(failure).includes('TOKEN_SECRET'));
});
test('fewer guard reads preserve identical functional writes and ACK: local counts, not Google performance',()=>{
 const rows=[];
 for(const v1 of [true,false]){const e=env('BASE',{helpersV1:v1});e.start();let queries=0,unzip=0;const acks=[];
  for(const original of [fixture.records[0],fixture.records[4],fixture.records[6]]){const p=e.prepare(original),a=ok(e.post(p));queries+=a._gboTest.metrics.operations['guard.metadata.get']||0;unzip+=a._gboTest.metrics.operations['cpu.unzip']||0;delete a._gboTest;acks.push(a);}
  rows.push({v1,queries,unzip,writes:clone(e.writes),acks});
 }
 assert.ok(rows[1].queries<rows[0].queries);assert.equal(rows[1].unzip,rows[0].unzip);assert.deepEqual(rows[1].writes,rows[0].writes);assert.deepEqual(rows[1].acks,rows[0].acks);
 fs.writeFileSync(path.join(__dirname,'../evidencia/COMPARACION_GUARD_LOCAL.json'),JSON.stringify({environment:'LOCAL_SIMULATED_GOOGLE_NO_NETWORK',oldQueries:rows[0].queries,newQueries:rows[1].queries,functionalWritesEqual:true,ackEqual:true,unzipEqual:true,realGoogleMeasurementsAfterPatch:0},null,2));
});
test('both client tasks settle before report failure; original failure is retained',async()=>{
 let finished=false;const first=Promise.reject(new r.SafeError('PRIMARY_FAILURE'));const other=new Promise(resolve=>setTimeout(()=>{finished=true;resolve();},30));
 await assert.rejects(()=>runner.settleClients([first,other]),error=>{assert.equal(finished,true);return error.code==='PRIMARY_FAILURE';});
});
test('read-only diagnostic and resume reproduce stop after cached parents, explain failures, preserve original evidence',async()=>{
 const e=env();e.start();const cache={BUSY:true,_primed:true},video=Buffer.from('MOCK_ONLY_FICTITIOUS_VIDEO'),videoPath=path.join(out,'test.webm');fs.writeFileSync(videoPath,video);
 for(const original of fixture.records){const p=e.prepare(original),a=ok(e.post(p));delete a._gboTest;cache[p.recordId]={payload:p,ack:a,client:'SIMULATED',totalPrepareStageAckMs:0};}
 cache._fixtureSignature=sha(JSON.stringify({corpus:fixture,load:[],video:sha(video),epoch:e.run.epoch}));
 const src=path.join(out,'source-'+crypto.randomUUID());fs.mkdirSync(src);const previous={environment:'LOCAL_SIMULATED_GOOGLE_NO_NETWORK',run:e.run,status:'INCOMPLETE',extraPerFamily:0,expectedParents:8,events:[],photoFixture:{profile:'COMPAT',sha256:null,sizeBytes:null},videoFixture:{sha256:sha(video),sizeBytes:video.length,mimeType:'video/webm'}};
 fs.writeFileSync(path.join(src,'avance.json'),JSON.stringify(cache));fs.writeFileSync(path.join(src,'resultado.json'),JSON.stringify(previous));fs.writeFileSync(path.join(src,'eventos.jsonl'),'');
 const before=Object.fromEntries(fs.readdirSync(src).map(n=>[n,sha(fs.readFileSync(path.join(src,n)))]));const savedFetch=global.fetch,savedEnv={...process.env};let fault='',network=[];
 try{
  global.fetch=async(url,options={})=>{const u=new URL(url);if(u.hostname==='www.googleapis.com'){const f=e.objects.get(u.pathname.split('/').pop());if(options.body.length){f.b=e.blob(options.body,'video/webm',f.name);return {status:200,headers:{get:()=>null}};}return {status:308,headers:{get:()=>null}};}
   if(!options.method)return {json:async()=>JSON.parse(e.context.doGet().getContent())};const p=JSON.parse(options.body);network.push({action:p.action,key:p.key,revision:p.revision});let a=e.post(p,p._test);
   if(p._client==='1.4.9_LOCAL_COPY_REPLAY'){if(fault==='flag')a.alreadyExists=false;if(fault==='metrics')delete a._gboTest;if(fault==='timeout'){const error=Error('URL_PRIVATE_TOKEN_PRIVATE');error.name='TimeoutError';throw error;}}
   return {status:200,json:async()=>a};};
  Object.assign(process.env,{GBO_TEST_MODE:'DIAGNOSTIC',GBO_TEST_SOURCE_DIR:src,GBO_TEST_EXEC_URL:'https://script.google.com/macros/s/LOCAL_MOCK_ONLY/exec',GBO_TEST_SCRIPT_ID:'TEST_SCRIPT_ONLY',GBO_TEST_TOKEN:'a'.repeat(64),GBO_TEST_OUTPUT:path.join(out,'new'),GBO_TEST_VIDEO:videoPath,GBO_TEST_EXTRA_PER_FAMILY:'0',GBO_TEST_PHOTO_PROFILE:'COMPAT'});
  const n=e.writes.length,props=JSON.stringify(e.props);const d=await runner.main({simulated:true});assert.equal(d.certificate.safeToResume,true);assert.equal(e.writes.length,n);assert.equal(JSON.stringify(e.props),props);assert.ok(network.every(x=>x.action==='testInspectRecords'));
  const pinned=JSON.parse(e.props.GBO_TEST_RUN);e.props.GBO_TEST_RUN=JSON.stringify({...pinned,epoch:'CHANGED'});network=[];await assert.rejects(()=>runner.main({simulated:true}),error=>error.code==='RUN_OR_EPOCH_CHANGED');assert.equal(network.length,0);e.props.GBO_TEST_RUN=JSON.stringify(pinned);
  for(const [inject,condition] of [['flag','ACK_ALREADY_EXISTS_NOT_TRUE'],['metrics','HARNESS_METRICS_MISSING'],['timeout','TRANSPORT_UNCERTAIN_REQUIRES_DIAGNOSTIC']]){fault=inject;process.env.GBO_TEST_MODE='RESUME';network=[];await assert.rejects(()=>runner.main({simulated:true}),error=>error.code===condition);assert.ok(!network.some(x=>x.action==='stageRecordV3'));assert.equal(network.filter(x=>x.action==='commitRecordV3').length,1);assert.equal(e.writes.length,n);}
  fault='';network=[];process.env.GBO_TEST_MODE='RESUME';await runner.main({simulated:true});assert.ok(network.some(x=>x.action==='finishVideoV1'));assert.ok(network.filter(x=>x.action==='stageRecordV3').every(x=>x.key==='SIC/TEST_SIC_A'&&[2,3].includes(x.revision)),'Cached original parents must not be staged again');assert.ok(!network.some(x=>x.action==='testHoldLock'));
  assert.deepEqual(Object.fromEntries(fs.readdirSync(src).map(n=>[n,sha(fs.readFileSync(path.join(src,n)))])),before);
  const evidence=r.loadEvidence(src,runner,videoPath,true,true);assert.equal(evidence.records.length,8);
  const corrupt=clone(cache);corrupt._fixtureSignature='0'.repeat(64);const altered=path.join(out,'altered-'+crypto.randomUUID());fs.mkdirSync(altered);for(const f of ['resultado.json','eventos.jsonl'])fs.copyFileSync(path.join(src,f),path.join(altered,f));fs.writeFileSync(path.join(altered,'avance.json'),JSON.stringify(corrupt));assert.throws(()=>r.loadEvidence(altered,runner,videoPath,true,true),error=>error.code==='FIXTURE_SIGNATURE_MISMATCH');
 }finally{global.fetch=savedFetch;for(const k of Object.keys(process.env))if(!(k in savedEnv))delete process.env[k];Object.assign(process.env,savedEnv);}
});
