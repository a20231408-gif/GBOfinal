// Offline reconciliation of PRIVATE output from gboAdminRegistryReadonly. No I/O or mutation.
function reconcile(report,authorizedTestKeys=[]) {
 const states={},keys=new Map(),issues=[];let unreadable=0;
 for(const registry of report.registries||[]) {
  if(!registry.stable)issues.push('UNSTABLE_REGISTRY');
  for(const row of registry.rows||[]) {
   if(row.unreadable){unreadable++;continue;}
   const v=row.value;
   if(row.name.startsWith('journal_')){const state=v.status||'UNKNOWN';states[state]=(states[state]||0)+1;if(state!=='COMPLETE')issues.push('JOURNAL_REQUIRES_RECONCILIATION');}
   if(row.name.startsWith('record_')) {
    if(!v.key){issues.push('RECORD_WITHOUT_KEY');continue;}
    const list=keys.get(v.key)||[];list.push({root:registry.scope.rootId,revision:v.revision,receiptRevision:v.receipt?.revision});keys.set(v.key,list);
    if(!v.receipt || v.receipt.revision!==v.revision || !v.contentHash || v.receipt.contentHash!==v.contentHash)issues.push('ACK_METADATA_MISMATCH');
   }
  }
 }
 if(unreadable)issues.push('UNREADABLE_METADATA');
 for(const list of keys.values())if(list.length>1)issues.push('MULTIPLE_RECORD_AUTHORITIES');
 const requested=[...new Set(authorizedTestKeys)];
 return {states,unreadable,recordKeys:keys.size,issues:[...new Set(issues)],
  explicitlySelectedTestKeys:requested.filter(key=>keys.has(key)),missingSelectedKeys:requested.filter(key=>!keys.has(key)),
  testClassificationIsNotDeletionAuthorization:true,ackChecksAreMetadataOnly:true,usableForDeletion:false};
}
module.exports={reconcile};
