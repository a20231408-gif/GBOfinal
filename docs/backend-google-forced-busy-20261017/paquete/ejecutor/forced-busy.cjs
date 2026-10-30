'use strict';
// Test-only contention. Never changes the business timeout, ACK or lock.
const {randomUUID}=require('node:crypto');
const {SafeError}=require('./recuperacion.cjs');
const VERSION=2,HOLD_MS=90000,MIN_REMAINING_MS=60000,MIN_WAIT_MS=29000;
async function forceBusy({call,stage,now=()=>performance.now(),wait=ms=>new Promise(r=>setTimeout(r,ms)),progress=()=>{}}){
  const probeId=randomUUID(),started=now();
  const proof={version:VERSION,outcome:'INCONCLUSIVE',excludedFromPerformance:true,holdMs:HOLD_MS,minRemainingMs:MIN_REMAINING_MS,competitorSent:false};
  const opts={scenario:'FORCED_BUSY',singleAttempt:true};
  let holdResult,holdFailure,problem;
  // Observe rejection immediately; always drain holder before returning/throwing.
  const holder=call({action:'testHoldLock',probeId},opts).then(a=>{holdResult=a;},e=>{holdFailure=e;});
  const inconclusive=reason=>{throw new SafeError('FORCED_BUSY_INCONCLUSIVE',{reason,...proof});};
  try{
    progress('FORCED_BUSY_WAIT_HOLDER');let ready=false,statusRemaining=0,statusSent=0;
    for(let i=0;i<15;i++){
      await wait(500);if(holdFailure)throw holdFailure;
      if(holdResult)inconclusive('HOLDER_FINISHED_BEFORE_COMPETITOR');
      const t=now(),s=await call({action:'testLockStatus',probeId},opts),received=now();
      if(s.ok!==true||s._gboTest?.forcedBusyTestVersion!==VERSION)inconclusive('STATUS_OR_HELPER_INVALID');
      if(!s.held)continue;
      // Subtract the entire RTT: conservative lower bound, no client/server clock sync.
      const remaining=s.remainingMs-(received-t)-(now()-received);
      proof.remainingLowerBoundMs=Math.floor(remaining);
      if(s.probeMatches!==true||s.holdMs!==HOLD_MS||!Number.isFinite(remaining)||remaining<MIN_REMAINING_MS)inconclusive('INSUFFICIENT_VERIFIED_MARGIN');
      statusRemaining=s.remainingMs;statusSent=t;ready=true;break;
    }
    if(!ready)inconclusive('HOLDER_NOT_OBSERVED');
    progress('FORCED_BUSY_COMPETITOR');
    proof.remainingLowerBoundMs=Math.floor(statusRemaining-(now()-statusSent));
    if(proof.remainingLowerBoundMs<MIN_REMAINING_MS)inconclusive('MARGIN_EXPIRED_BEFORE_DISPATCH');
    proof.competitorSent=true;
    const a=await call(stage,opts),locks=a._gboTest?.metrics?.locks;
    proof.responseBusy=a.ok===false&&a.code==='BUSY'&&a.retryable===true;
    proof.lockEvidenceMatches=Array.isArray(locks)&&locks.length===1&&locks[0].acquired===false&&Number.isFinite(locks[0].waitMs)&&locks[0].waitMs>=MIN_WAIT_MS;
    proof.waitMs=Array.isArray(locks)&&Number.isFinite(locks[0]?.waitMs)?locks[0].waitMs:null;
    proof.competitorAcquired=Array.isArray(locks)?locks.some(l=>l.acquired===true):null;
    if(!proof.responseBusy||!proof.lockEvidenceMatches||a._gboTest?.forcedBusyTestVersion!==VERSION)inconclusive('TARGET_RESPONSE_OR_LOCK_NOT_BUSY');
  }catch(e){problem=e;}
  finally{progress('FORCED_BUSY_DRAIN_HOLDER');await holder;proof.totalArtificialClientMs=now()-started;}
  if(holdFailure)throw holdFailure; // Transport uncertainty keeps its existing safety stop.
  const hlocks=holdResult?._gboTest?.metrics?.locks;
  proof.holderConfirmed=holdResult?.ok===true&&holdResult?.heldMs>=HOLD_MS&&holdResult?._gboTest?.forcedBusyTestVersion===VERSION&&Array.isArray(hlocks)&&hlocks.length===1&&hlocks[0].acquired===true&&hlocks[0].heldMs>=HOLD_MS;
  if(problem){if(problem.code==='FORCED_BUSY_INCONCLUSIVE')problem.safeChecks={...problem.safeChecks,...proof};throw problem;}
  if(!proof.holderConfirmed)inconclusive('HOLDER_COMPLETION_NOT_VERIFIED');
  proof.outcome='VERIFIED_BUSY';return proof;
}
module.exports={forceBusy,VERSION,HOLD_MS,MIN_REMAINING_MS,MIN_WAIT_MS};
