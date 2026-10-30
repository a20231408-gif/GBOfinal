'use strict';
const fs=require('node:fs'),path=require('node:path'),vm=require('node:vm'),assert=require('node:assert/strict'),{test}=require('node:test');
const root=path.resolve(__dirname,'../../..');
const H=require(path.join(root,'docs/scap-integracion-actual-20261027/pruebas/Harness.cjs'));
const dir=path.join(__dirname,'../backend-propuesto-NO-PUBLICAR');
const v21=fs.readFileSync(path.join(root,'docs/scap-integracion-actual-20261027/pruebas/integrado_NO_IMPORTAR/SyncV3Core.gs.txt'),'utf8');
function environment(candidate=true){const e=H.env();if(candidate){for(const name of ['PavementSicChronologyV1','SyncV3Core'])vm.runInContext(fs.readFileSync(path.join(dir,name+'.gs'),'utf8'),e.context);}else vm.runInContext(v21,e.context);return e;}
const clone=p=>JSON.parse(JSON.stringify(p));
const fixtureRoot=process.env.GBO_PAVEMENT_FIXTURES||path.join(__dirname,'fixtures');
const fixture=()=>JSON.parse(fs.readFileSync(path.join(fixtureRoot,'ANDROID_PAVEMENT_CHRONO.json'),'utf8'));
function send(e,p){return e.post({...p,contentHash:H.hash(e.context.SyncV3.canonical(p))});}
function row(p,id,date,time,pr=0){const r=clone(p.sicRows[0]);return {...r,recordId:p.recordId+'_'+id,registeredDate:date,registeredAt:time,startPrNumber:pr};}
function aggregates(e,format='SIC-14'){return [...e.objects.values()].filter(x=>x.name===format+'.xlsx'&&x.parent.name==='EXCEL_SIC');}
function manifest(e,key){return [...e.objects.values()].filter(x=>x.name?.endsWith('.json')&&x.b).map(x=>{try{return JSON.parse(x.b.getDataAsString())}catch{return null}}).find(x=>x&&x.key===key&&Array.isArray(x.sicRows));}
test('Android canonical SHA exactly matches proposed Core with nullable PR',()=>{const e=environment(),p=fixture();assert.equal(H.hash(e.context.SyncV3.canonical(p)),p.contentHash);assert.equal(p.sicRows[0].startPrNumber,null);assert.equal(p.sicRows[0].startDistanceM,null);});
test('actual V21 rejects new row contract but accepts document-only no-PR backup, full ACK and replay',()=>{
 const e=environment(false),p=fixture();delete p.pavementSicChronologicalVersion;
 assert.equal(send(e,p).code,'INVALID_TECHNICAL_ROW');
 const backup=JSON.parse(fs.readFileSync(path.join(fixtureRoot,'ANDROID_PAVEMENT_V21.json'),'utf8'));assert.equal(H.hash(e.context.SyncV3.canonical(backup)),backup.contentHash);
 p.sicRows=[];const a=e.post(backup);assert.equal(a.ok,true,JSON.stringify(a));assert.equal(a.files.length,backup.files.length);assert.equal(a.sicFiles.length,0);
 for(const f of backup.files)assert.equal(a.files.find(x=>x.key===f.key).sha256,f.sha256);
 const writes=e.writes.length,b=e.post(backup);assert.equal(b.alreadyExists,true);assert.equal(b.folderId,a.folderId);assert.equal(b.contentHash,a.contentHash);assert.equal(e.writes.length,writes);
});
test('chronological consolidation uses date, persisted time, UUID; edits upsert, retry ACK stable',()=>{
 const e=environment(),p=fixture();p.sicRows=[row(p,'C','2026-10-23',100,0),row(p,'D','2026-10-22',300,0),row(p,'B','2026-10-22',200,999),row(p,'A','2026-10-22',200,100)];
 const a=send(e,p);assert.equal(a.ok,true,JSON.stringify(a));assert.equal(a.sicFiles.length,1);
 assert.deepEqual(Array.from(a.sicFiles[0].recordIds),['A','B','D','C'].map(x=>p.recordId+'_'+x));
 const file=a.sicFiles[0].fileId,folder=a.folderId;assert.equal(aggregates(e).length,1);
 const replay=send(e,p);assert.equal(replay.alreadyExists,true);assert.equal(replay.folderId,folder);
 p.baseRevision=p.revision;p.revision++;p.sicRows[0].startPrNumber=9999;
 const b=send(e,p);assert.equal(b.ok,true,JSON.stringify(b));assert.equal(b.folderId,folder);assert.equal(b.sicFiles[0].fileId,file);assert.equal(b.sicFiles[0].recordIds.length,4);assert.equal(aggregates(e).length,1);
});
test('historical rows without registration metadata require reconciliation before transactional writes',()=>{
 const e=environment(),p=fixture();delete p.pavementSicChronologicalVersion;p.sicRows=p.sicRows.slice(0,1);
 for(const r of p.sicRows){delete r.registeredDate;delete r.registeredAt;r.startPrNumber=1;r.startDistanceM=1;r.orderSource='CONTRACTUAL_PR';}
 const a=send(e,p);assert.equal(a.ok,true,JSON.stringify(a));const count=e.writes.length;
 const next=fixture();next.baseRevision=0;next.recordId='OTHER_TEST';next.key='PAVEMENT/'+next.recordId;next.sicRows=[row(next,'NEW','2026-10-20',20)];
 const b=send(e,next);assert.equal(b.ok,false);assert.equal(b.code,'PAVEMENT_REGISTRATION_RECONCILIATION_REQUIRED');
 // Adapter may persist staging/index housekeeping; transaction must not create the new native parent or replace aggregate.
 assert.equal([...e.objects.values()].filter(x=>x.name==='SIC-14.xlsx'&&x.parent.name==='EXCEL_SIC').length,1);
 assert.equal(e.writes.slice(count).some(x=>['update','advancedCreate'].includes(x.op)),false);
});
test('invalid date/clock rejected; historical payload canonical unchanged; other SIC still PR comparator',()=>{
 const e=environment(),p=fixture();p.sicRows[0].registeredDate='2026-02-30';assert.equal(send(e,p).code,'INVALID_PAVEMENT_REGISTRATION_ROW');
 p.sicRows[0].registeredDate='2026-10-20';p.sicRows[0].registeredAt=0;assert.equal(send(e,p).code,'INVALID_PAVEMENT_REGISTRATION_ROW');
 const b=environment(false);delete p.pavementSicChronologicalVersion;assert.equal(e.context.SyncV3.canonical(p),b.context.SyncV3.canonical(p));
 const rows=[{recordId:'C',roadbedCode:'CD',direction:'',startPrNumber:11,startDistanceM:0},{recordId:'B',roadbedCode:'CD',direction:'',startPrNumber:10,startDistanceM:20},{recordId:'A',roadbedCode:'CD',direction:'',startPrNumber:10,startDistanceM:10}];
 assert.deepEqual(rows.sort(e.context.SyncV3.compareTechnical).map(r=>r.recordId),['A','B','C']);
});
test('BUSY preserves request and successful retry; hash corruption cannot become ACK',()=>{
 const e=environment(),p=fixture();e.hold(true);const busy=send(e,p);assert.equal(busy.code,'BUSY');e.hold(false);
 const a=send(e,p);assert.equal(a.ok,true,JSON.stringify(a));const bad=clone(p);bad.files[0].sha256='0'.repeat(64);const b=send(e,bad);assert.equal(b.ok,false);assert.equal(b.code,'HASH_MISMATCH');
 const replay=send(e,p);assert.equal(replay.alreadyExists,true);assert.equal(replay.contentHash,a.contentHash);
});
