/** Isolated integration core. No routes; authority/inventory/backup adapters are mandatory. */
var AdminLifecycle = (()=>{
  const fail=code=>{throw Error(code);};
  function create(deps){
    for(const name of ['store','authority','inventory','backup','files','hash'])if(!deps[name])fail('MISSING_'+name.toUpperCase());
    const {store,authority,inventory,backup,files,hash}=deps;
    function binding(plan){return hash(JSON.stringify({key:plan.key,identityId:plan.identityId,revision:plan.revision,rootId:plan.rootId,files:plan.files.map(f=>({id:f.id,sha256:f.sha256})).sort((a,b)=>a.id.localeCompare(b.id))}));}
    function check(plan,grant,purpose){
      const actor=authority.authorize(grant,{purpose,key:plan.key,operationId:plan.operationId,digest:binding(plan),revision:plan.revision});
      if(!actor?.id||!actor.driveOwnerId)fail('INDIVIDUAL_AUTHORITY_REQUIRED');
      const current=inventory.inspect(plan.key);
      if(!current.complete || current.pending || current.shared || current.identityConflict || current.revision!==plan.revision || current.identityId!==plan.identityId || current.rootId!==plan.rootId || current.ownerId!==actor.id)fail('INVENTORY_PROTECTED');
      if(hash(JSON.stringify(current.files.map(f=>({id:f.id,sha256:f.sha256})).sort((a,b)=>a.id.localeCompare(b.id))))!==hash(JSON.stringify(plan.files.map(f=>({id:f.id,sha256:f.sha256})).sort((a,b)=>a.id.localeCompare(b.id)))))fail('FILE_SET_CHANGED');
      if(!backup.verify(plan))fail('BACKUP_REQUIRED');
      return actor;
    }
    function exclusive(plan,file,actor){
      const info=files.inspect(file.id);
      if(info.id!==file.id||info.ownerId!==actor.driveOwnerId||!info.canEdit||info.shared||info.sha256!==file.sha256||info.rootId!==plan.rootId)fail('FILE_NOT_EXCLUSIVE');
      return info;
    }
    function execute(plan,grant,restore=false){return store.lock(()=>{
      if(!plan || !/^[A-Za-z0-9_-]{1,80}$/.test(plan.operationId)||!/^(SIC|SCAP|PAVEMENT)\/[A-Za-z0-9_-]{1,80}$/.test(plan.key)||!Number.isSafeInteger(plan.revision)||plan.revision<1||!plan.identityId||!plan.rootId||!Array.isArray(plan.files)||!plan.files.length||new Set(plan.files.map(f=>f.id)).size!==plan.files.length)fail('INVALID_PLAN');
      const digest=binding(plan),actor=check(plan,grant,restore?'RESTORE':'TRASH');
      let j=store.read(plan.operationId);
      if(j&&(j.digest!==digest||j.key!==plan.key))fail('OPERATION_ID_REUSED');
      if(!restore&&j&&['RESTAURADO','RESTAURANDO','PARCIAL_RESTAURACION'].includes(j.state))fail('STALE_TRASH_AFTER_RESTORE');
      if(restore && (!j||!['COMPLETADO','RESTAURANDO','PARCIAL_RESTAURACION','RESTAURADO'].includes(j.state)))fail('RESTORE_NOT_READY');
      if(!j){
        if(store.fenced(plan.key))fail('ANOTHER_OPERATION_FENCE');
        for(const file of plan.files)exclusive(plan,file,actor); // Protect the entire set before any effect.
        j={operationId:plan.operationId,key:plan.key,identityId:plan.identityId,revision:plan.revision,digest,state:'AUTORIZADO',fence:true,done:[],restored:[],actorId:actor.id};
        store.save(j); // Single durable document contains both fence and intent, before any effect.
      }
      if(j.state===(restore?'RESTAURADO':'COMPLETADO'))return j;
      try{
        j.state=restore?'RESTAURANDO':'ELIMINANDO_REMOTO';store.save(j);
        for(const f of plan.files){
          check(plan,grant,restore?'RESTORE':'TRASH'); // live revocation, revision, references and backup
          const info=exclusive(plan,f,actor);
          j.pendingFileId=f.id;store.save(j);
          if(info.trashed!==!restore)files.setTrashed(f.id,!restore);
          if(files.inspect(f.id).trashed!==!restore)fail('REMOTE_NOT_VERIFIED');
          const list=restore?j.restored:j.done;if(!list.includes(f.id))list.push(f.id);
          delete j.pendingFileId;store.save(j);
        }
        j.state=restore?'RESTAURADO':'COMPLETADO';store.save(j);return j;
      }catch(e){j.state=restore?'PARCIAL_RESTAURACION':'PARCIAL';j.errorCode='RECONCILIATION_REQUIRED';store.save(j);throw e;}
    });}
    function assertUploadAllowed(key){if(!key||store.fenced(key))fail('ADMINISTRATIVE_FENCE');}
    return {execute,assertUploadAllowed}; // Restore retains fence; no unsafe resurrection or purge.
  }
  return {create};
})();
if(typeof module!=='undefined')module.exports=AdminLifecycle;
