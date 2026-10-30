'use strict';
const fs=require('node:fs'),path=require('node:path'),crypto=require('node:crypto');
const clone=x=>JSON.parse(JSON.stringify(x)),hash=x=>crypto.createHash('sha256').update(x).digest('hex'),fp=x=>hash(String(x)).slice(0,12);
const stable=x=>Array.isArray(x)?x.map(stable):x&&typeof x==='object'?Object.fromEntries(Object.keys(x).sort().map(k=>[k,stable(x[k])])):x;
class SafeError extends Error{constructor(condition,checks={}){super(condition);this.code=condition;this.safeChecks=checks;}}
function check(ok,code,details={}){if(!ok)throw new SafeError(code,details);}
function json(p){return JSON.parse(fs.readFileSync(p,'utf8').replace(/^\uFEFF/,''));}
function ackSummary(p,a,original){return {okBoolean:a?.ok===true,alreadyExistsPresent:Object.hasOwn(a||{},'alreadyExists'),alreadyExistsType:typeof a?.alreadyExists,alreadyExists:typeof a?.alreadyExists==='boolean'?a.alreadyExists:null,
  identityMatches:typeof a?.key==='string'&&a.key===p.key,revisionMatches:Number.isSafeInteger(a?.revision)&&a.revision===p.revision,contentHashMatches:typeof a?.contentHash==='string'&&a.contentHash===p.contentHash,
  folderPresent:typeof a?.folderId==='string'&&!!a.folderId,folderMatchesOriginal:original? a?.folderId===original.folderId:null,metricsPresent:!!a?._gboTest?.metrics};}
function validateAck(p,a){
  const s=ackSummary(p,a);check(s.okBoolean,'ACK_OK_INVALID',s);check(a.syncProtocol===3&&a.pathVersion===4,'ACK_PROTOCOL_MISMATCH',s);
  check(s.identityMatches,'ACK_IDENTITY_MISMATCH',s);check(s.revisionMatches,'ACK_REVISION_MISMATCH',s);check(s.contentHashMatches,'ACK_CONTENT_HASH_MISMATCH',s);
  check(a.technicalDeliveryStatus==='DELIVERED','ACK_DELIVERY_STATUS_MISMATCH',s);check(s.folderPresent&&typeof a.logicalPath==='string','ACK_FOLDER_MISSING',s);
  check(typeof a.alreadyExists==='boolean','ACK_ALREADY_EXISTS_TYPE',s);
  check(Array.isArray(a.files)&&a.files.length===p.files.length,'ACK_FILES_COUNT_MISMATCH',s);
  check(new Set(a.files.map(f=>f.key)).size===a.files.length,'ACK_DUPLICATE_FILE_KEY',s);
  for(const f of p.files){const x=a.files.find(x=>x.key===f.key);check(x&&x.sha256===f.sha256&&typeof x.fileId==='string'&&!!x.fileId&&typeof x.folderId==='string'&&!!x.folderId&&typeof x.logicalPath==='string','ACK_FILE_MISMATCH',s);}
  check(Array.isArray(a.sicFiles),'ACK_SIC_FILES_INVALID',s);return a;
}
function validateReplay(p,a,original){validateAck(p,a);const s=ackSummary(p,a,original);check(a.alreadyExists===true,'ACK_ALREADY_EXISTS_NOT_TRUE',s);check(s.folderMatchesOriginal,'ACK_FOLDER_MISMATCH',s);check(hash(JSON.stringify(stable(a.files)))===hash(JSON.stringify(stable(original.files))),'ACK_FILE_IDENTITY_CHANGED',s);return a;}
function safeFailure(error,stage){const at=/(ejecutar|recuperacion)\.cjs:(\d+):(\d+)/.exec(error?.stack||'');return {stage:stage||'UNKNOWN',condition:error instanceof SafeError?error.code:'UNCLASSIFIED_LOCAL_FAILURE',checks:error instanceof SafeError?error.safeChecks:{},assertion:error?.code==='ERR_ASSERTION',sourceLocation:at?{module:at[1]+'.cjs',line:Number(at[2]),column:Number(at[3])}:null,rawMessageOmitted:true};}
function sameRun(a,b){return ['runId','epoch','variant','folderId','folderVariant'].every(k=>(a?.[k]??null)===(b?.[k]??null));}
function validateTelemetry(a,run,requestId){
  check(!!a?._gboTest?.metrics,'HARNESS_METRICS_MISSING',{});
  const t=a._gboTest;check(t.harnessRevision===2&&t.guardPolicy==='FRESH_WRITES_LOCKED_READ_CACHE_V2','HARNESS_REVISION_MISMATCH');
  check(t.runId===run.runId&&t.variant===run.variant&&t.requestId===requestId,'HARNESS_RESPONSE_BINDING_MISMATCH');
}
function loadEvidence(sourceDir,tools,videoPath,requireVideo=false,allowSimulated=false){
  check(!!sourceDir,'SOURCE_EVIDENCE_REQUIRED');sourceDir=path.resolve(sourceDir);
  const report=json(path.join(sourceDir,'resultado.json')),cache=json(path.join(sourceDir,'avance.json'));
  const events=fs.readFileSync(path.join(sourceDir,'eventos.jsonl'),'utf8').replace(/^\uFEFF/,'').split(/\r?\n/).filter(Boolean).map(x=>JSON.parse(x));
  check(report.environment==='REAL_GOOGLE_REQUESTS_FROM_OWNER_RUNNER'||allowSimulated&&report.environment==='LOCAL_SIMULATED_GOOGLE_NO_NETWORK','EVIDENCE_ENVIRONMENT_INVALID');
  const clean=e=>{e=clone(e);delete e.responseDiscardedByClient;return e;};
  check(hash(JSON.stringify((report.events||[]).map(clean)))===hash(JSON.stringify(events.map(clean))),'EVIDENCE_JOURNAL_DIVERGENCE');
  const fixture=json(path.join(__dirname,'fixtures/records.json')),profile=report.photoFixture?.profile,extra=report.extraPerFamily;
  check(['CAMPO','COMPAT'].includes(profile)&&Number.isSafeInteger(extra),'EVIDENCE_PROFILE_INVALID');
  if(process.env.GBO_TEST_PHOTO_PROFILE)check(process.env.GBO_TEST_PHOTO_PROFILE===profile,'PROFILE_CHANGED');
  if(process.env.GBO_TEST_EXTRA_PER_FAMILY)check(Number(process.env.GBO_TEST_EXTRA_PER_FAMILY)===extra,'LOAD_CHANGED');
  const load=tools.extraRecords(fixture,extra),photo=profile==='CAMPO'?fs.readFileSync(path.join(__dirname,'fixtures/FOTO_SINTETICA_2048.jpg')):null;
  if(photo){check(hash(photo)===report.photoFixture.sha256&&photo.length===report.photoFixture.sizeBytes,'PHOTO_FIXTURE_CHANGED');for(const p of [...fixture.records,fixture.legacyEdit,fixture.blankEdit,...load])for(const f of p.files.filter(f=>f.key.startsWith('photo/'))){f.base64=photo.toString('base64');f.sha256=hash(photo);}}
  check(/^[a-f0-9]{64}$/.test(report.videoFixture?.sha256||''),'VIDEO_FIXTURE_INVALID');
  let videoVerified=false;if(videoPath&&fs.existsSync(videoPath)){const v=fs.readFileSync(videoPath);check(hash(v)===report.videoFixture.sha256&&v.length===report.videoFixture.sizeBytes,'VIDEO_FIXTURE_CHANGED');videoVerified=true;}
  check(!requireVideo||videoVerified,'SAME_VIDEO_REQUIRED_FOR_RESUME');
  check(cache._fixtureSignature===hash(JSON.stringify({corpus:fixture,load,video:report.videoFixture.sha256,epoch:report.run?.epoch})),'FIXTURE_SIGNATURE_MISMATCH');
  const planned=new Map([...fixture.records,...load].map(p=>[p.recordId,p]));planned.set('legacyEdit',fixture.legacyEdit);planned.set('blankEdit',fixture.blankEdit);
  const latest=new Map();let confirmed=0;
  for(const [label,v] of Object.entries(cache)){
    if(!v?.ack||!v.payload)continue;const p=v.payload,a=v.ack;validateAck(p,a);check(hash(tools.canonical(p))===p.contentHash,'CACHE_CANONICAL_MISMATCH');
    check(planned.has(label),'CACHE_LABEL_UNSUPPORTED');const expected=clone(planned.get(label));expected.photoNamingVersion=2;
    for(const f of expected.files){const sent=p.files.find(x=>x.key===f.key);check(sent&&sent.sha256===f.sha256,'CACHE_FILE_FIXTURE_CHANGED');if(f.key.startsWith('photo/')){check(sent.path.slice(0,sent.path.lastIndexOf('/')+1)===f.path.slice(0,f.path.lastIndexOf('/')+1),'CACHE_PHOTO_PATH_CHANGED');f.path=sent.path;}else check(sent.path===f.path,'CACHE_DOCUMENT_PATH_CHANGED');}
    check(hash(tools.canonical(expected))===p.contentHash,'CACHE_FIXTURE_CANONICAL_MISMATCH');
    for(const row of p.sicRows)check(hash(Buffer.from(row.base64,'base64'))===row.sha256,'CACHE_XLSX_BYTES_CHANGED');
    for(const f of p.files){if(f.stageFileId)check(typeof f.stageFileId==='string','CACHE_STAGE_REFERENCE_INVALID');else check(typeof f.base64==='string'&&hash(Buffer.from(f.base64,'base64'))===f.sha256,'CACHE_INLINE_BYTES_CHANGED');}
    const old=latest.get(p.key);if(old){check(old.ack.folderId===a.folderId,'CACHE_FOLDER_DIVERGENCE');if(old.payload.revision===p.revision)check(old.payload.contentHash===p.contentHash,'CACHE_REVISION_AMBIGUOUS');}
    if(!old||old.payload.revision<p.revision)latest.set(p.key,v);confirmed++;
  }
  check(latest.size===report.expectedParents&&latest.size===8+3*extra,'CACHE_PARENTS_INCOMPLETE');
  const records=[...latest.values()].map(v=>({key:v.payload.key,revision:v.payload.revision,contentHash:v.payload.contentHash,folderId:v.ack.folderId,files:v.ack.files.map(f=>({key:f.key,fileId:f.fileId,folderId:f.folderId,logicalPath:f.logicalPath,sha256:f.sha256})),stages:v.payload.files.filter(f=>f.stageFileId).map(f=>({key:f.key,stageFileId:f.stageFileId,sha256:f.sha256})),rows:v.payload.sicRows.map(x=>({recordId:x.recordId,format:x.format,sha256:x.sha256}))}));
  return {sourceDir,report,cache,events,records,fixture,load,videoVerified,inputHashes:Object.fromEntries(['resultado.json','eventos.jsonl','avance.json'].map(n=>[n,hash(fs.readFileSync(path.join(sourceDir,n)))]))};
}
async function diagnose(tools,options={}){
  const dir=path.resolve(process.env.GBO_TEST_OUTPUT||path.join(__dirname,'../DIAGNOSTICOS'), 'diagnostico-'+Date.now()+'-'+crypto.randomUUID().slice(0,8));fs.mkdirSync(dir,{recursive:true});
  const result={mode:'READ_ONLY_DIAGNOSTIC',realGoogleExecution:!options.simulated,safeToResume:false};
  let stage='OFFLINE_EVIDENCE',source;
  try{
    source=loadEvidence(process.env.GBO_TEST_SOURCE_DIR,tools,process.env.GBO_TEST_VIDEO,options.requireVideo,options.simulated);
    Object.assign(result,{inputHashes:source.inputHashes,runFingerprint:fp(source.report.run.runId),epochFingerprint:fp(source.report.run.epoch),sameFixtures:true,videoBytesVerified:source.videoVerified});stage='CAPABILITY';
    const url=tools.endpoint(process.env.GBO_TEST_EXEC_URL||''),token=process.env.GBO_TEST_TOKEN;check(/^[a-fA-F0-9]{64,128}$/.test(token||''),'TEST_CREDENTIAL_REQUIRED');
    console.log('[DIAGNOSTICO] evidencia local validada; padres='+source.records.length+'; sin cargas ni cambios de contador.');
    const cap=await(await fetch(url,{signal:AbortSignal.timeout(120000)})).json();const run=tools.checkCapability(cap,process.env.GBO_TEST_SCRIPT_ID);
    check(cap._gboTest.harnessRevision===2&&cap._gboTest.guardPolicy==='FRESH_WRITES_LOCKED_READ_CACHE_V2','HELPERS_V2_NOT_DEPLOYED');
    check(sameRun(run,source.report.run),'RUN_OR_EPOCH_CHANGED');check(cap._gboTest.inflight===0,'DIAGNOSTIC_INFLIGHT_NOT_ZERO');
    stage='INSPECT_COMMITTED_RECEIPTS';const requestId=crypto.randomUUID(),start=performance.now();
    const response=await fetch(url,{method:'POST',headers:{'Content-Type':'application/json'},body:JSON.stringify({action:'testInspectRecords',records:source.records,expectedOrder:['TEST_SIC_A','TEST_SIC_B','TEST_SIC_D','TEST_SIC_C'],token,_test:{runId:run.runId,epoch:run.epoch,variant:run.variant,requestId}}),signal:AbortSignal.timeout(180000)});
    check(response.status===200,'DIAGNOSTIC_HTTP_STATUS');const a=await response.json();validateTelemetry(a,run,requestId);check(a.ok===true,'DIAGNOSTIC_SERVER_REJECTED');
    result.durationMs=performance.now()-start;result.results=a.results;result.orderMatches=a.orderMatches;result.recordCount=a.recordCount;result.journalCount=a.journalCount;result.harnessRevision=2;result.guardPolicy=cap._gboTest.guardPolicy;result.metrics=a._gboTest.metrics;
    check(a.readOnly===true&&a.inflight===0,'DIAGNOSTIC_NOT_READ_ONLY');check(a.safeToResume===true,'LIVE_MANIFEST_RECONCILIATION_FAILED');
    stage='RECHECK_FENCE';const after=await(await fetch(url,{signal:AbortSignal.timeout(120000)})).json();tools.checkCapability(after,process.env.GBO_TEST_SCRIPT_ID);check(sameRun(after._gboTest.run,run)&&after._gboTest.inflight===0,'FENCE_CHANGED_AFTER_DIAGNOSTIC');
    result.safeToResume=true;console.log('[DIAGNOSTICO] '+source.records.length+'/'+source.records.length+' padres conciliados; journals COMPLETE; inflight=0; sin escrituras Drive.');
    return {source,certificate:result,diagnosticDir:dir};
  }catch(error){result.failure=safeFailure(error,stage);console.error('[DIAGNOSTICO] detenido; etapa='+stage+'; condicion='+result.failure.condition);throw error;}
  finally{fs.writeFileSync(path.join(dir,'diagnostico.json'),JSON.stringify(result,null,2));}
}
module.exports={SafeError,check,hash,fp,ackSummary,validateAck,validateReplay,safeFailure,sameRun,validateTelemetry,loadEvidence,diagnose};
