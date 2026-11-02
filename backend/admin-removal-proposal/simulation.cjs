'use strict';
// Executable contract model ONLY. No HTTP, Apps Script, Drive or filesystem adapters.
// Production must supply independently verified identity, grants, inventory, locks and tombstones.
const states = Object.freeze(['PREVISUALIZADO','AUTORIZADO','PENDIENTE_DE_SERVIDOR','ELIMINANDO_REMOTO','VERIFICANDO_REMOTO','LIMPIANDO_LOCAL','COMPLETADO','PARCIAL','BLOQUEADO']);
function preview(plan) {
  if (!plan.operationId || !plan.records.length || new Set(plan.records.map(r=>r.key)).size!==plan.records.length) throw Error('INVALID_PLAN');
  return {state:'PREVISUALIZADO',plan:JSON.parse(JSON.stringify(plan)),done:[],audit:[],tombstones:[],purged:false};
}
function confirm(journal,text,second,serverGrant) {
  const j=JSON.parse(JSON.stringify(journal));
  if(j.state!=='PREVISUALIZADO')throw Error('INVALID_STATE');
  if(text!=='ELIMINAR' || j.plan.records.length>1&&!second)return j;
  // This is a MOCK grant assertion, never a production authentication mechanism.
  if(!serverGrant || serverGrant.operationId!==j.plan.operationId || !serverGrant.verified || !serverGrant.admin || serverGrant.digest!==j.plan.digest) {
    j.state='BLOQUEADO';return j;
  }
  j.grant=serverGrant;j.state='AUTORIZADO';j.audit.push('MOCK_AUTHORIZED');return j;
}
async function run(j,io) {
  if(io.simulationOnly!==true)throw Error('NO_PRODUCTION_ADAPTER');
  if(j.restoreRequested)throw Error('RESTORE_RECONCILIATION_NOT_IMPLEMENTED');
  if(!['AUTORIZADO','PENDIENTE_DE_SERVIDOR','PARCIAL'].includes(j.state))return j;
  return io.lock(async()=>{
    try {
      j.state='PENDIENTE_DE_SERVIDOR';await io.save(j);
      // Preflight the ENTIRE set, including completed files on a retry.
      for(const r of j.plan.records) {
        const current=await io.inventory(r.key);
        if(current.revision!==r.revision || current.owner!==j.grant.actor || !current.backupVerified || current.pendingOutbox || current.shared || current.linked || !current.bindingVerified || !current.rootAllowed || !current.completeInventory || current.manifestDigest!==r.manifestDigest)throw Error('RECORD_PROTECTED');
        for(const file of current.files)if(file.owner!==j.grant.actor || file.shared || !file.hashVerified)throw Error('FILE_PROTECTED');
      }
      // An atomic backend fence must precede any removal and reject ALL old uploads.
      // Mock stores persist this with their journal. Production implementation is absent.
      j.tombstones=j.plan.records.map(r=>({key:r.key,revision:r.revision,operationId:j.plan.operationId}));
      await io.fenceAndSave(j);
      j.state='ELIMINANDO_REMOTO';await io.save(j);
      for(const r of j.plan.records)for(const file of (await io.inventory(r.key)).files) {
        if(j.done.includes(file.id))continue;
        await io.trash(file.id,j.plan.operationId); // idempotent even if response was lost
        j.done.push(file.id);await io.save(j);
      }
      j.state='VERIFICANDO_REMOTO';await io.save(j);
      for(const r of j.plan.records)for(const file of (await io.inventory(r.key)).files)if(!await io.isTrashed(file.id))throw Error('REMOTE_NOT_CONFIRMED');
      j.state='LIMPIANDO_LOCAL';await io.save(j);
      await io.releaseExclusiveCopies(j); // MOCK only, originals/backup retained
      j.state='COMPLETADO';j.audit.push('MOCK_TRASH_VERIFIED');await io.save(j);
    }catch(e){j.state=j.tombstones.length?'PARCIAL':'BLOQUEADO';j.error=e.message;await io.save(j);}
    return j;
  });
}
function canUpload(j,key){return !j.tombstones.some(x=>x.key===key);}
function restoreSimulation(j){if(j.purged)throw Error('IRREVERSIBLE');return {...j,state:'PENDIENTE_DE_SERVIDOR',restoreRequested:true,audit:[...j.audit,'MOCK_RESTORE_REQUIRES_SERVER_RECONCILIATION']};}
function purgeSimulation(j,grant){if(j.state!=='COMPLETADO'||!grant?.explicitPermanentAuthorization)throw Error('PERMANENT_NOT_AUTHORIZED');return {...j,purged:true};}
module.exports={states,preview,confirm,run,canUpload,restoreSimulation,purgeSimulation};
