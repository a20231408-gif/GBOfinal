'use strict';
/** Offline only. Input and detailed output belong outside Git; stdout is aggregate counts. */
function reconcilePartitions(parts){
 if(parts.length!==16||new Set(parts.map(p=>p.end?.bucket)).size!==16)throw Error('INCOMPLETE_PARTITIONS');
 const signatures=new Set(parts.map(p=>p.end.signature));if(signatures.size!==1)throw Error('CROSS_PARTITION_CHANGE');
 const rows=[];
 for(const p of parts){
  if(!p.end.readOnly||p.end.mutations!==0||!p.end.complete||!p.end.stableDuringRead||p.end.signature!==p.end.verificationSignature)throw Error('UNVERIFIED_PARTITION');
  if(p.rows.length!==p.end.targets||new Set(p.rows.map(r=>r.id)).size!==p.rows.length)throw Error('MISSING_OR_DUPLICATE_ROWS');
  rows.push(...p.rows);
 }
 if(rows.length!==parts[0].end.relevantEntries||new Set(rows.map(r=>r.id)).size!==rows.length)throw Error('INCOMPLETE_REGISTRY');
 const grouped=new Map();for(const r of rows){const a=grouped.get(r.name)||[];a.push(r);grouped.set(r.name,a);}
 const states={},journals=[],records=rows.filter(r=>r.kind==='record'),bindings=rows.filter(r=>r.kind.startsWith('binding'));
 for(const r of rows.filter(r=>r.kind==='journal')){
  const state=r.unreadable?'UNREADABLE':r.status||'UNKNOWN';states[state]=(states[state]||0)+1;
  const candidates=grouped.get(r.name.replace(/^journal_/,'record_'))||[],record=candidates.length===1?candidates[0]:null;
  const issues=[];if(r.unreadable)issues.push('JOURNAL_UNREADABLE');
  if(!['COMPLETE','PENDING','ROLLED_BACK'].includes(state))issues.push('UNKNOWN_OR_UNREADABLE_STATE');
  if(!r.unreadable&&r.operations===null)issues.push('INVALID_OPERATIONS');
  if(candidates.length>1)issues.push('DUPLICATE_RECORD_NAME');
  if(state==='COMPLETE'){
   if(!record||record.unreadable)issues.push('RECORD_MISSING_OR_UNREADABLE');
   else {
    const ack=record.ack;
    if(!ack||ack.ok!==true||ack.key!==record.key||ack.revision!==record.revision||ack.contentHash!==record.contentHash)issues.push('ACK_MISMATCH_OR_MISSING');
    if(r.revision!==null&&r.revision!==record.revision)issues.push('JOURNAL_REVISION_MISMATCH');
   }
  }
  journals.push({...r,key:r.key||record?.key||null,recordId:record?.id||null,recordRevision:record?.revision??null,issues,
   requiresReview:state!=='COMPLETE'||issues.length>0,requiresOldRoot:true});
 }
 return {summary:{readOnly:true,mutations:0,stableDuringRead:true,signature:[...signatures][0],registryEntries:parts[0].end.registryEntries,
  inventoriedEntries:rows.length,journals:journals.length,states,records:records.length,bindings:bindings.length,
  unreadableRecords:records.filter(r=>r.unreadable).length,unreadableBindings:bindings.filter(r=>r.unreadable).length,
  reviewJournals:journals.filter(r=>r.requiresReview).length,duplicateNames:[...grouped.values()].filter(a=>a.length>1).length,
  remoteFilesValidated:false,photoReservationsContentRead:false},journals,records,bindings};
}
module.exports={reconcilePartitions};
if(require.main===module){
 const fs=require('node:fs'),path=require('node:path');const [directory,output]=process.argv.slice(2);
 if(!directory||!output)throw Error('Use private input directory and a new private output path');
 const parts=fs.readdirSync(directory).filter(n=>/^partition-[0-9a-f]\.json$/.test(n)).sort().map(n=>JSON.parse(fs.readFileSync(path.join(directory,n),'utf8')));
 const result=reconcilePartitions(parts);fs.writeFileSync(output,JSON.stringify(result,null,2),{flag:'wx'});console.log(JSON.stringify(result.summary));
}
