const {test}=require('node:test'),assert=require('node:assert/strict'),fs=require('node:fs'),path=require('node:path'),{spawnSync}=require('node:child_process');
const {env}=require('./MockGoogle.cjs'),runner=require('../ejecutor/ejecutar.cjs');
const out=path.join(__dirname,'salida-local-no-google');fs.mkdirSync(out,{recursive:true});
test('complete runner BASE and CANDIDATO with mock network, lost ACK, BUSY, two clients, video, XLSX; no Google',async()=>{
 const savedFetch=global.fetch,savedEnv={...process.env};const reports=[];
 try{
  for(const variant of ['BASE','CANDIDATO']){
   const e=env(variant);e.start();let releaseHold=null,busySeen=false;
   global.fetch=async(url,options={})=>{
    const u=new URL(url);
    if(u.hostname==='www.googleapis.com'){
     const id=u.pathname.split('/').pop(),f=e.objects.get(id);assert.ok(f);
     if(options.body.length){f.b=e.blob(options.body,'video/webm',f.name);return {status:200,headers:{get:()=>null}};}
     return {status:308,headers:{get:()=>null}};
    }
    assert.equal(u.hostname,'script.google.com');
    if(!options.method)return {json:async()=>JSON.parse(e.context.doGet().getContent())};
    const p=JSON.parse(options.body);assert.equal(p.token,e.props.GBO_TEST_TOKEN);
    if(p.action==='testHoldLock'){
      e.hold(true);e.props.GBO_TEST_LOCK_HELD=JSON.stringify({runId:e.run.runId,until:Date.now()+40000});
      return await new Promise(resolve=>{releaseHold=()=>{e.hold(false);delete e.props.GBO_TEST_LOCK_HELD;resolve({status:200,json:async()=>({ok:true})});};});
    }
    const ack=e.post(p,p._test);
    if(ack.code==='BUSY'&&!busySeen){busySeen=true;releaseHold();}
    return {status:200,json:async()=>ack};
   };
   process.env.GBO_TEST_EXEC_URL='https://script.google.com/macros/s/LOCAL_MOCK_ONLY/exec';process.env.GBO_TEST_SCRIPT_ID='TEST_SCRIPT_ONLY';process.env.GBO_TEST_TOKEN='a'.repeat(64);
   process.env.GBO_TEST_OUTPUT=out;const v=path.join(out,'MOCK_OPAQUE_NOT_PLAYABLE.webm');fs.writeFileSync(v,'MOCK_ONLY_NOT_A_PLAYABLE_VIDEO');process.env.GBO_TEST_VIDEO=v;
   process.env.GBO_TEST_PHOTO_PROFILE='COMPAT';
   process.env.GBO_TEST_EXTRA_PER_FAMILY='1';
   await runner.main({simulated:true});
   const dir=path.join(out,variant+'_'+e.run.runId),report=JSON.parse(fs.readFileSync(path.join(dir,'resultado.json')));
   assert.equal(report.environment,'LOCAL_SIMULATED_GOOGLE_NO_NETWORK');assert.equal(report.status,'PROTOCOL_CHECKS_PASSED_REVIEW_XLSX_AND_GOOGLE_LOGS');assert.equal(report.snapshot.manifests.length,11);assert.ok(report.events.some(x=>x.code==='BUSY'));assert.equal(report.videoAck.complete,true);
   assert.ok(!fs.readFileSync(path.join(dir,'resultado.json'),'utf8').includes('upload_id='));assert.ok(!fs.readFileSync(path.join(dir,'resultado.json'),'utf8').includes('a'.repeat(64)));
   const py=process.env.GBO_TEST_PYTHON||'python';
   const check=spawnSync(py,['-c',"import sys,importlib.util,json,pathlib;s=importlib.util.spec_from_file_location('cmp',sys.argv[1]);m=importlib.util.module_from_spec(s);s.loader.exec_module(m);print(json.dumps(m.check_xlsx(pathlib.Path(sys.argv[2]))))",path.join(__dirname,'../ejecutor/comparar.py'),dir],{encoding:'utf8'});
   assert.equal(check.status,0,check.stderr);report.xlsxValidation=JSON.parse(check.stdout);reports.push(report);
   if(variant==='BASE'){
    const oldIds=e.run;e.switchVariant('CANDIDATO');e.context.gboTestResumePreviousBase();await runner.main({simulated:true});
    const upgrade=JSON.parse(fs.readFileSync(path.join(out,'UPGRADE_'+oldIds.runId,'resultado.json')));assert.equal(upgrade.environment,'LOCAL_SIMULATED_GOOGLE_NO_NETWORK');assert.equal(upgrade.status,'GOOGLE_UPGRADE_PROTOCOL_PASSED');assert.equal(upgrade.upgradeAck.revision,4);assert.equal(upgrade.run.folderId,oldIds.folderId);assert.equal(upgrade.snapshot.manifests.length,11);
   }
  }
  const stable=r=>r.snapshot.manifests.map(m=>({key:m.key,revision:m.revision,contentHash:m.contentHash,rows:m.sicRows,files:m.files.map(f=>({key:f.key,sha256:f.sha256,logicalPath:f.logicalPath}))})).sort((a,b)=>a.key.localeCompare(b.key));
  assert.deepEqual(stable(reports[0]),stable(reports[1]));
  const xlsx=r=>r.snapshot.files.filter(f=>f.parts&&f.path.includes('/EXCEL_SIC/')).map(f=>({path:f.path,parts:f.parts})).sort((a,b)=>a.path.localeCompare(b.path));assert.deepEqual(xlsx(reports[0]),xlsx(reports[1]));
  const unzip=r=>r.events.reduce((n,e)=>n+(e.metrics?.operations?.['cpu.unzip']||0),0);assert.ok(unzip(reports[1])<unzip(reports[0]));
  fs.writeFileSync(path.join(__dirname,'../evidencia/RESUMEN_EJECUTOR_LOCAL.json'),JSON.stringify({environment:'LOCAL_SIMULATED_GOOGLE_NO_NETWORK',parentsPerVariant:11,revisionsPreserved:true,uuidPreserved:true,activeFileHashesEqual:true,excelPartHashesEqual:true,upgradeFromOriginalHistoryWithSameIds:true,localCopies149ModeledNewRemoteJobs:0,baseUnzip:unzip(reports[0]),candidateUnzip:unzip(reports[1]),realGoogleExecutions:0},null,2));
 }finally{global.fetch=savedFetch;for(const k of Object.keys(process.env))if(!(k in savedEnv))delete process.env[k];Object.assign(process.env,savedEnv);}
});
