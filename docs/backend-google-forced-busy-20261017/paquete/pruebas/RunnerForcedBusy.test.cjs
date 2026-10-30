'use strict';
const {test}=require('node:test'),assert=require('node:assert/strict'),fs=require('node:fs'),path=require('node:path'),os=require('node:os');
const {env}=require('./MockGoogle.cjs'),runner=require('../ejecutor/ejecutar.cjs');
test('runner persists targeted late-success as harness INCONCLUSIVE, drains holder, preserves staging and refuses to overwrite evidence',async()=>{
 const e=env();e.start();let finish,posts=0;const savedFetch=global.fetch,savedEnv={...process.env};
 const out=fs.mkdtempSync(path.join(os.tmpdir(),'gbo-forced-busy-regression-'));
 try{
  process.env.GBO_TEST_EXEC_URL='https://script.google.com/macros/s/LOCAL_MOCK_ONLY/exec';process.env.GBO_TEST_SCRIPT_ID='TEST_SCRIPT_ONLY';process.env.GBO_TEST_TOKEN='a'.repeat(64);
  process.env.GBO_TEST_MODE='FULL';delete process.env.GBO_TEST_SOURCE_DIR;process.env.GBO_TEST_OUTPUT=out;process.env.GBO_TEST_EXTRA_PER_FAMILY='0';process.env.GBO_TEST_PHOTO_PROFILE='COMPAT';
  process.env.GBO_TEST_VIDEO=path.join(out,'MOCK_NOT_PLAYABLE.webm');fs.writeFileSync(process.env.GBO_TEST_VIDEO,'MOCK_ONLY');
  global.fetch=async(url,opts={})=>{
   assert.equal(new URL(url).hostname,'script.google.com');
   if(!opts.method)return {json:async()=>JSON.parse(e.context.doGet().getContent())};
   posts++;const p=JSON.parse(opts.body);
   if(p.action==='testHoldLock'){
    e.props.GBO_TEST_LOCK_HELD=JSON.stringify({runId:e.run.runId,probeId:p.probeId,until:Date.now()+90000,holdMs:90000});
    return new Promise(resolve=>{finish=()=>{delete e.props.GBO_TEST_LOCK_HELD;resolve({status:200,json:async()=>({ok:true,heldMs:90000,_gboTest:{forcedBusyTestVersion:2,harnessRevision:2,guardPolicy:'FRESH_WRITES_LOCKED_READ_CACHE_V2',requestId:p._test.requestId,runId:e.run.runId,variant:'BASE',metrics:{locks:[{acquired:true,waitMs:0,heldMs:90000}]}}})});};});
   }
   const a=e.post(p,p._test);
   if(p.action==='stageRecordV3'){a._gboTest.metrics.locks[0].waitMs=29935;finish();}
   return {status:200,json:async()=>a};
  };
  await assert.rejects(runner.main({simulated:true}),{code:'FORCED_BUSY_INCONCLUSIVE'});
  const d=path.join(out,'BASE_'+e.run.runId),rp=path.join(d,'resultado.json'),report=JSON.parse(fs.readFileSync(rp));
  assert.equal(report.status,'INCOMPLETE');assert.equal(report.failureDomain,'TEST_HARNESS');assert.equal(report.failure.condition,'FORCED_BUSY_INCONCLUSIVE');assert.equal(report.forcedBusy.competitorAcquired,true);
  assert.ok(report.events.every(x=>x.scenario==='FORCED_BUSY'));assert.ok(!report.events.some(x=>x.action==='commitRecordV3'));assert.equal(report.normalScenarioStarted,undefined);
  assert.equal(e.props.GBO_TEST_LOCK_HELD,undefined);assert.equal(e.props.GBO_TEST_INFLIGHT,'0');
  const cache=JSON.parse(fs.readFileSync(path.join(d,'avance.json')));assert.ok(!cache.BUSY);assert.ok(!Object.values(cache).some(x=>x?.ack));
  const writes=e.writes.length,original=fs.readFileSync(rp);assert.ok(writes>1,'Successful artificial stage is preserved, never deleted');
  await assert.rejects(runner.main({simulated:true}),{code:'EXISTING_RUN_REQUIRES_DIAGNOSTIC_OR_NEW_CLEAN_RUN'});assert.equal(e.writes.length,writes);assert.deepEqual(fs.readFileSync(rp),original);
  const count=posts;e.props.GBO_TEST_INFLIGHT='1';await assert.rejects(runner.main({simulated:true}),{code:'RUN_ALREADY_INFLIGHT'});assert.equal(posts,count);assert.equal(e.props.GBO_TEST_INFLIGHT,'1');
 }finally{global.fetch=savedFetch;for(const k of Object.keys(process.env))if(!(k in savedEnv))delete process.env[k];Object.assign(process.env,savedEnv);}
});
