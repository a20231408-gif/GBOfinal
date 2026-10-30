'use strict';
const {test}=require('node:test'),assert=require('node:assert/strict');
const {forceBusy}=require('../ejecutor/forced-busy.cjs');
const {SafeError}=require('../ejecutor/recuperacion.cjs');
const {env}=require('./MockGoogle.cjs');
test('real local BASE handler: 35s holder, arrival at 5065ms, acquisition at 35000ms returns a valid stage ACK',()=>{
 const e=env();e.start();const NativeDate=Date;let clock=5065,requested;
 e.context.Date=class extends NativeDate{static now(){return clock;}};
 e.context.LockService.getScriptLock=()=>({tryLock(ms){requested=ms;clock=35000;return true;},releaseLock(){}});
 const p=require('../ejecutor/fixtures/records.json').records[0],f=p.files.find(x=>x.key==='metadata');
 const a=e.post({action:'stageRecordV3',syncProtocol:3,key:p.key,revision:1,file:f});
 assert.equal(requested,30000);assert.equal(a.ok,true);assert.equal(a.sha256,f.sha256);assert.ok(a.stageFileId);
 assert.equal(a._gboTest.metrics.locks[0].waitMs,29935);assert.equal(a._gboTest.metrics.locks[0].acquired,true);
 assert.equal(e.props.GBO_TEST_INFLIGHT,'0');
});
function scenario(o={}){
 let t=0,release,sent=0,drained=false,probe;
 const ack=(ok,locks,extra={})=>({ok,...extra,_gboTest:{forcedBusyTestVersion:2,metrics:{locks}}});
 const holder=ack(true,[{acquired:true,waitMs:10,heldMs:90000}],{heldMs:90000});
 const call=async(p,opts)=>{
  assert.equal(opts.scenario,'FORCED_BUSY');assert.equal(opts.singleAttempt,true);
  if(p.action==='testHoldLock'){probe=p.probeId;return new Promise((resolve,reject)=>{release=()=>{t=Math.max(t,90000);drained=true;o.holderTransport?reject(new SafeError('TRANSPORT_UNCERTAIN_REQUIRES_DIAGNOSTIC')):resolve(o.badHolder?ack(true,[],{heldMs:90000}):holder);};});}
  if(p.action==='testLockStatus'){
   assert.equal(p.probeId,probe);t+=o.rtt||1000;
   return ack(true,[],{held:o.notObserved?false:true,probeMatches:o.wrongProbe?false:true,holdMs:90000,remainingMs:o.remaining??85000});
  }
  assert.equal(p.action,'stageRecordV3');sent++;t+=o.lateAcquire?29935:30000;
  // Models the real boundary: holder was seen, but request reaches lock late.
  const a=o.lateAcquire?ack(true,[{acquired:true,waitMs:29935,heldMs:17145}]):ack(false,o.noMetrics?[]:[{acquired:false,waitMs:o.waitMs??30000,heldMs:0}],{code:o.otherCode||'BUSY',retryable:o.notRetryable?false:true});
  release();return a;
 };
 const task=forceBusy({call,stage:{action:'stageRecordV3'},now:()=>t,wait:async ms=>{t+=ms;},progress:stage=>{if(stage==='FORCED_BUSY_COMPETITOR'&&o.dispatchDelay)t+=o.dispatchDelay;if(stage==='FORCED_BUSY_DRAIN_HOLDER'&&!drained)release();}});
 return {task,get sent(){return sent;},get drained(){return drained;}};
}
test('verified BUSY is the targeted response plus failed ~30s lock and completed 90s holder',async()=>{const x=scenario();const p=await x.task;assert.equal(p.outcome,'VERIFIED_BUSY');assert.equal(p.excludedFromPerformance,true);assert.equal(p.waitMs,30000);assert.equal(p.totalArtificialClientMs,90000);assert.equal(x.drained,true);});
test('late lock acquisition at 29935ms is INCONCLUSIVE, never a business failure',async()=>{const x=scenario({lateAcquire:true});await assert.rejects(x.task,e=>e.code==='FORCED_BUSY_INCONCLUSIVE'&&e.safeChecks.competitorAcquired===true&&e.safeChecks.responseBusy===false);assert.equal(x.drained,true);});
for(const [label,o,reason] of [
 ['insufficient remaining hold',{remaining:59999},'INSUFFICIENT_VERIFIED_MARGIN'],
 ['network RTT consumes margin',{remaining:65000,rtt:6000},'INSUFFICIENT_VERIFIED_MARGIN'],
 ['margin expires before dispatch',{remaining:65000,dispatchDelay:5000},'MARGIN_EXPIRED_BEFORE_DISPATCH'],
 ['other holder probe',{wrongProbe:true},'INSUFFICIENT_VERIFIED_MARGIN'],
 ['holder not observed',{notObserved:true},'HOLDER_NOT_OBSERVED'],
 ['BUSY lacks lock evidence',{noMetrics:true},'TARGET_RESPONSE_OR_LOCK_NOT_BUSY'],
 ['unrelated test gate BUSY',{otherCode:'TEST_GATE_BUSY'},'TARGET_RESPONSE_OR_LOCK_NOT_BUSY'],
 ['implausibly early BUSY',{waitMs:50},'TARGET_RESPONSE_OR_LOCK_NOT_BUSY'],
 ['BUSY not retryable',{notRetryable:true},'TARGET_RESPONSE_OR_LOCK_NOT_BUSY'],
 ['holder completion lacks lock evidence',{badHolder:true},'HOLDER_COMPLETION_NOT_VERIFIED']
])test(label+' preserves evidence and drains holder',async()=>{const x=scenario(o);await assert.rejects(x.task,e=>e.code==='FORCED_BUSY_INCONCLUSIVE'&&e.safeChecks.reason===reason);assert.equal(x.drained,true);if(['INSUFFICIENT_VERIFIED_MARGIN','MARGIN_EXPIRED_BEFORE_DISPATCH','HOLDER_NOT_OBSERVED'].includes(reason))assert.equal(x.sent,0);});
test('transport uncertainty remains a diagnostic stop after holder drain',async()=>{const x=scenario({holderTransport:true});await assert.rejects(x.task,{code:'TRANSPORT_UNCERTAIN_REQUIRES_DIAGNOSTIC'});assert.equal(x.drained,true);});
test('exact conservative 60000ms remaining margin permits verified competitor',async()=>{const x=scenario({remaining:61000});assert.equal((await x.task).outcome,'VERIFIED_BUSY');});
test('unrelated earlier BUSY cannot turn a successful targeted stage into PASS',async()=>{const history=[{code:'BUSY'}],x=scenario({lateAcquire:true});assert.ok(history.some(e=>e.code==='BUSY'));await assert.rejects(x.task,{code:'FORCED_BUSY_INCONCLUSIVE'});});
for(const variant of ['BASE','CANDIDATO'])test(variant+' helper: correlated status, 90s cap, no Drive writes; hold cleanup on sleep failure',()=>{
 const e=env(variant);e.start();const before=e.writes.length,p='a1234567-1234-1234-1234-123456789abc';
 const cap=JSON.parse(e.context.doGet().getContent());assert.equal(cap._gboTest.forcedBusyTestVersion,2);assert.equal(cap._gboTest.forcedBusyHoldMs,90000);
 e.props.GBO_TEST_LOCK_HELD=JSON.stringify({runId:e.run.runId,probeId:p,until:Date.now()+90000,holdMs:90000});
 const same=e.post({action:'testLockStatus',probeId:p});assert.equal(same.held,true);assert.ok(same.remainingMs>85000);assert.equal(same.probeMatches,true);
 assert.equal(e.post({action:'testLockStatus',probeId:'different'}).held,false);
 assert.equal(e.post({action:'testHoldLock',probeId:'invalid'}).code,'TEST_PROBE_INVALID');
 let slept=0;e.context.Utilities.sleep=ms=>{slept=ms;throw Error('injected local sleep error');};
 const failed=e.post({action:'testHoldLock',probeId:p});assert.equal(slept,90000);assert.equal(failed.ok,false);assert.equal(e.props.GBO_TEST_LOCK_HELD,undefined);assert.equal(e.props.GBO_TEST_INFLIGHT,'0');
 assert.equal(e.writes.length,before);const lock=e.context.GboTestLocks.getScriptLock();assert.equal(lock.tryLock(0),true);lock.releaseLock();
});
