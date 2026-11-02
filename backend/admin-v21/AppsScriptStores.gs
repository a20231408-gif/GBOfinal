/** Candidate only: not installed, no HTTP handlers, no folder creation, execution disabled by default.
 * Enable only in an authorized isolated test after binding to verified technical registry IDs.
 */
function gboAdminPersistentStore(config) {
  if(config?.allowMutations!==true || !config.registryId)throw Error('ADMIN_STORE_DISABLED');
  const registry=DriveApp.getFolderById(config.registryId);
  const digest=s=>Utilities.computeDigest(Utilities.DigestAlgorithm.SHA_256,s).map(x=>(x&255).toString(16).padStart(2,'0')).join('');
  const name=id=>'admin_operation_'+digest(id)+'.json';
  function find(id){const it=registry.getFilesByName(name(id));if(!it.hasNext())return null;const f=it.next();if(it.hasNext())throw Error('DUPLICATE_ADMIN_OPERATION');return f;}
  function readFile(f){const j=JSON.parse(f.getBlob().getDataAsString());if(!j.operationId||!j.key||typeof j.fence!=='boolean')throw Error('INVALID_ADMIN_JOURNAL');return j;}
  return {
    lock:fn=>{const lock=LockService.getScriptLock();if(!lock.tryLock(1000))throw Error('BUSY');try{return fn();}finally{lock.releaseLock();}},
    read:id=>{const f=find(id);return f?readFile(f):null;},
    save:j=>{const text=JSON.stringify(j);const f=find(j.operationId);if(f)f.setContent(text);else registry.createFile(Utilities.newBlob(text,'application/json',name(j.operationId)));if(JSON.stringify(readFile(find(j.operationId)))!==text)throw Error('JOURNAL_WRITE_UNCONFIRMED');},
    fenced:key=>{const it=registry.getFiles();while(it.hasNext()){const f=it.next();if(!f.getName().startsWith('admin_operation_'))continue;const j=readFile(f);if(j.key===key&&j.fence)return true;}return false;}
  };
}
function gboAdminTrashStore(config){
  if(config?.allowMutations!==true || typeof config.inspectExclusiveFile!=='function')throw Error('ADMIN_TRASH_DISABLED');
  // Inspector must independently verify ancestry, hashes, reverse references and effective ownership.
  return {inspect:config.inspectExclusiveFile,setTrashed:(id,value)=>Drive.Files.update({trashed:value},id,{fields:'id,trashed',supportsAllDrives:true})};
}
