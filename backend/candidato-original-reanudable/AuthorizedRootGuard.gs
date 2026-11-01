/** Local candidate only. Read-only identity check BEFORE any root-relative write. */
function gboAuthorizedRoot() {
  function reject(code,retryable) {const e=Error(code);e.code=code;e.retryable=retryable===true;throw e;}
  const p=PropertiesService.getScriptProperties();
  const id=p.getProperty('GBO_AUTHORIZED_ROOT_ID');
  const parentId=p.getProperty('GBO_AUTHORIZED_ROOT_PARENT_ID');
  const name=p.getProperty('GBO_AUTHORIZED_ROOT_NAME');
  const parentName=p.getProperty('GBO_AUTHORIZED_ROOT_PARENT_NAME');
  let ancestorIds;
  try {ancestorIds=JSON.parse(p.getProperty('GBO_AUTHORIZED_ROOT_ANCESTOR_IDS'));}catch(e){reject('ROOT_NOT_AUTHORIZED');}
  // Names confirm identity; they are NEVER used to search for or create a destination.
  if(!id || !parentId || !name || !parentName || id==='root' || parentId==='root' || id===parentId || id!==ROOT_FOLDER_ID ||
    !Array.isArray(ancestorIds) || ancestorIds.length<1 || ancestorIds.length>16 || ancestorIds[0]!==parentId ||
    ancestorIds.some(x=>typeof x!=='string'||!x||x==='root'||x===id) || new Set(ancestorIds).size!==ancestorIds.length)
    reject('ROOT_NOT_AUTHORIZED');
  return gboVerifyRootIdentity({id,parentId,name,parentName,ancestorIds});
}

/** Used for a second explicitly approved root; never searches by name or falls back. */
function gboVerifyRootIdentity(spec) {
  function reject(code,retryable) {const e=Error(code);e.code=code;e.retryable=retryable===true;throw e;}
  const {id,parentId,name,parentName,ancestorIds}=spec;
  if(!id||!parentId||!name||!parentName||id==='root'||parentId==='root'||id===parentId||
    !Array.isArray(ancestorIds)||ancestorIds.length<1||ancestorIds.length>16||ancestorIds[0]!==parentId||
    ancestorIds.some(x=>typeof x!=='string'||!x||x==='root'||x===id)||new Set(ancestorIds).size!==ancestorIds.length)
    reject('ROOT_NOT_AUTHORIZED');
  if(typeof Drive==='undefined')reject('ADVANCED_DRIVE_REQUIRED');
  let root,ancestors,myDrive;
  try {
    const options={fields:'id,name,mimeType,trashed,parents,capabilities(canEdit,canAddChildren)',supportsAllDrives:true};
    root=Drive.Files.get(id,options);
    ancestors=ancestorIds.map(x=>Drive.Files.get(x,options));
    myDrive=Drive.Files.get('root',{fields:'id'});
  }catch(e){reject('ROOT_VERIFICATION_FAILED',true);}
  const folder='application/vnd.google-apps.folder';
  if(!myDrive.id || id===myDrive.id || ancestorIds.includes(myDrive.id) || root.id!==id ||
    root.mimeType!==folder || root.trashed!==false || root.name!==name || ancestors[0].name!==parentName ||
    root.parents?.length!==1 || root.parents[0]!==parentId ||
    ancestors.some((a,i)=>a.id!==ancestorIds[i] || a.mimeType!==folder || a.trashed!==false ||
      a.parents?.length!==1 || a.parents[0]!== (ancestorIds[i+1]||myDrive.id)))
    reject('ROOT_IDENTITY_MISMATCH');
  if(root.capabilities?.canEdit!==true || root.capabilities?.canAddChildren!==true)reject('ROOT_NOT_WRITABLE');
  return DriveApp.getFolderById(id);
}

function gboRootReady() {try {gboAuthorizedRoot();return true;}catch(e){return false;}}
