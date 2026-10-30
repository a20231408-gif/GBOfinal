// Real local wall-clock measurements using fake Google services and Python ZIP.
// No latency numbers in this file represent Google, Drive, tablets or Starlink.
const fs=require('node:fs'),path=require('node:path'),assert=require('node:assert/strict'),os=require('node:os');
const {environment,hash}=require('./HarnessV20.cjs'),{payload,sample}=require('./CasosV20.cjs');
const variants=['v20','stage','borrador','instrumentado'],evidence=path.join(__dirname,'../evidencia');
const median=a=>[...a].sort((a,b)=>a-b)[Math.floor(a.length/2)];
function measure(variant,make){
 const e=environment(variant),inputs=make();e.resetMetrics();const start=performance.now(),acks=[];
 for(const p of inputs){const a=e.send(p);assert.equal(a.ok,true,JSON.stringify(a));acks.push(JSON.parse(JSON.stringify(a)));}
 return {variant,wallMs:performance.now()-start,metrics:JSON.parse(JSON.stringify(e.metrics)),acks};
}
const runs=[];
// Change order each repetition to expose order/warm-up noise instead of fixed-order bias.
for(let repeat=0;repeat<3;repeat++)for(const variant of variants.slice(repeat).concat(variants.slice(0,repeat))){
 const r=measure(variant,()=>Array.from({length:5},(_,i)=>payload('TEST_BENCH_'+i,i+1)));
 const un=r.metrics.calls['Utilities.unzip'],zip=r.metrics.calls['Utilities.zip'];
 assert.equal(un,['v20','stage'].includes(variant)?20:15);assert.equal(zip,5);
 runs.push({repeat,...r});
 fs.writeFileSync(path.join(evidence,'mediciones-parciales.json'),JSON.stringify({environment:'LOCAL ONLY; INTERMEDIATE',runs},null,2));
}
for(const repeat of [0,1,2]){const r=runs.filter(r=>r.repeat===repeat);for(const p of r.slice(1))assert.deepEqual(p.acks,r[0].acks);}
const rows=variants.map(variant=>{
 const r=runs.filter(r=>r.variant===variant),m=r[0].metrics;
 return {variant,commits:5,unzip:m.calls['Utilities.unzip'],zip:m.calls['Utilities.zip'],base64Decode:m.calls['Utilities.base64Decode'],sha256:m.calls['Utilities.computeDigest'],driveOperations:Object.entries(m.calls).filter(([k])=>/^(Drive|Folder|File)/.test(k)).reduce((a,[,v])=>a+v,0),wallMsMedian:median(r.map(r=>r.wallMs)),wallMsMin:Math.min(...r.map(r=>r.wallMs)),wallMsMax:Math.max(...r.map(r=>r.wallMs)),heldMsMedian:median(r.map(r=>r.metrics.lockHeldMs)),unzipMsMedian:median(r.map(r=>r.metrics.milliseconds['Utilities.unzip']))};
});
const multi=[];for(const variant of ['v20','borrador','instrumentado']){
 const r=measure(variant,()=>[sample('TEST_MULTI_A','ASFALTICO',5),sample('TEST_MULTI_B','ASFALTICO',5)]);assert.equal(r.metrics.calls['Utilities.unzip'],variant==='v20'?25:15);multi.push(r);
}
for(const r of multi.slice(1))assert.deepEqual(r.acks,multi[0].acks);
const staging=[];
const bytes=Buffer.alloc(256*1024,0x42),file={key:'metadata',path:'DATOS_APP/M_001.json',mimeType:'application/json',base64:bytes.toString('base64'),sha256:hash(bytes)};
for(let repeat=0;repeat<3;repeat++)for(const variant of variants){
 const e=environment(variant);e.resetMetrics();const start=performance.now();let last;
 for(let n=0;n<20;n++){last=e.context.stageRecordV3({syncProtocol:3,key:'PAVEMENT/TEST_STAGE_BENCH',revision:1,file});assert.equal(last.ok,true);}
 staging.push({repeat,variant,requests:20,bytesPerRequest:bytes.length,wallMs:performance.now()-start,metrics:e.metrics,ack:last});
}
for(const variant of variants){const r=staging.find(r=>r.variant===variant);assert.equal(r.metrics.inside['Utilities.base64Decode']||0,variant==='v20'?20:0);assert.equal(r.metrics.outside['Utilities.base64Decode']||0,variant==='v20'?0:20);}
const report={environment:'LOCAL WINDOWS NODE + PYTHON ZIP; FAKE DRIVE/LOCK; NOT GOOGLE MEASUREMENTS',realGoogleExecutions:0,node:process.version,platform:os.platform(),method:{repetitions:3,recordsPerRun:5,rowsPerCommit:1,zipTimestamps:'fixed for exact byte/SHA-256 parity; Google ZIP timestamp behavior not measured',busy:'Immediate fake rejection; no simulated 30-second sleep',cpuTiming:'includes Python process startup + local marshalling; not Apps Script cost'},rows,runs,multiRowSamples:multi,staging,stagingSummary:variants.map(v=>{const r=staging.filter(r=>r.variant===v);return {variant:v,requests:20,bytesPerRequest:bytes.length,wallMsMedian:median(r.map(x=>x.wallMs)),heldMsMedian:median(r.map(x=>x.metrics.lockHeldMs)),decodeInside:r[0].metrics.inside['Utilities.base64Decode']||0,decodeOutside:r[0].metrics.outside['Utilities.base64Decode']||0,shaInside:r[0].metrics.inside['Utilities.computeDigest']||0,shaOutside:r[0].metrics.outside['Utilities.computeDigest']||0};})};
fs.writeFileSync(path.join(evidence,'mediciones-locales.json'),JSON.stringify(report,null,2));
console.log(JSON.stringify({environment:report.environment,rows,staging:report.stagingSummary,multiRowSamples:multi.map(r=>({variant:r.variant,unzip:r.metrics.calls['Utilities.unzip'],zip:r.metrics.calls['Utilities.zip'],wallMs:r.wallMs}))},null,2));
