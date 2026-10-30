// Editor-only helpers. No tokens, endpoints, roots or file IDs are logged.
function gboResumeAudited59(){
  const root=gboPrivateTestRoot(),props=PropertiesService.getScriptProperties(),p=JSON.parse(JSON.stringify(GBO_FICTITIOUS_SCAP_PAYLOAD));
  const ids=JSON.parse(props.getProperty('GBO_TEST_STAGE_IDS')||'{}');
  for(const f of p.files){if(!ids[f.key])throw Error('MISSING_VERIFIED_STAGE');f.stageFileId=ids[f.key];delete f.base64;}
  p.action='commitRecordV3';p.token=props.getProperty('GBO_DRIVE_API_TOKEN');
  const start=Date.now(),ack=JSON.parse(doPost({postData:{contents:JSON.stringify(p)}}).getContent());
  const stats=JSON.parse(props.getProperty('GBO_RESUME_59_STATS')||'[]');stats.push({execution:stats.length+1,ms:Date.now()-start,ok:ack.ok,code:ack.code||null});props.setProperty('GBO_RESUME_59_STATS',JSON.stringify(stats));
  if(ack.ok){
    if(ack.key!==p.key||ack.revision!==p.revision||ack.contentHash!==p.contentHash||ack.files.length!==59||ack.sicFiles.length!==3)throw Error('INCOMPLETE_ACK');
    const name='TEST_ACK_CANDIDATO.json',old=root.getFilesByName(name),raw=JSON.stringify(ack);
    if(old.hasNext())old.next().setContent(raw);else root.createFile(Utilities.newBlob(raw,'application/json',name));
  }
  const registry=root.getFoldersByName('._GBO_SYNC_V3').next(),j=JSON.parse(registry.getFilesByName('journal_'+gboTestHash(p.key)+'.json').next().getBlob().getDataAsString());
  console.log(JSON.stringify({phase:ack.ok?'ACK_CANDIDATE':'COMMIT_PENDING',execution:stats.length,ms:stats[stats.length-1].ms,code:ack.code||null,journal:j.status,operations:j.operations.length,completed:j.operations.filter(x=>x.completed).length,files:ack.files?.length||0,sicFiles:ack.sicFiles?.length||0,alreadyExists:ack.alreadyExists||false,testOnly:true}));
}
function gboVerify59(){
  const root=gboPrivateTestRoot(),props=PropertiesService.getScriptProperties(),f=root.getFilesByName('TEST_ACK_CANDIDATO.json');if(!f.hasNext())throw Error('NO_ACK_CANDIDATE');
  const raw=f.next().getBlob().getDataAsString(),ack=JSON.parse(raw),p=GBO_FICTITIOUS_SCAP_PAYLOAD;
  if(!ack.ok||ack.key!==p.key||ack.revision!==p.revision||ack.contentHash!==p.contentHash||ack.files.length!==59||ack.sicFiles.length!==3)throw Error('INCOMPLETE_ACK');
  const hash=gboTestHash(raw),old=JSON.parse(props.getProperty('GBO_VERIFY_59')||'{}');
  const proof=old.hash===hash?old:{hash,cursor:0,ms:0};const start=Date.now(),all=ack.files.concat(ack.sicFiles),folders={};
  function path(id){if(id===root.getId())return '';if(folders[id]!==undefined)return folders[id];const m=Drive.Files.get(id,{fields:'id,name,parents,mimeType',supportsAllDrives:true});if(m.parents?.length!==1||m.mimeType!=='application/vnd.google-apps.folder')throw Error('INVALID_PARENT');return folders[id]=(path(m.parents[0])+'/'+m.name).replace(/^\//,'');}
  const end=Math.min(all.length,proof.cursor+14);
  for(let n=proof.cursor;n<end;n++){
    const item=all[n],file=DriveApp.getFileById(item.fileId),parents=file.getParents();
    if(!parents.hasNext())throw Error('NO_PARENT');const parent=parents.next();if(parents.hasNext()||parent.getId()!==item.folderId)throw Error('WRONG_PARENT');
    if(file.getOwner().getEmail()!=='reconocimientoinventariovial@gmail.com'||gboTestHash(file.getBlob().getBytes())!==item.sha256||path(parent.getId())+'/'+file.getName()!==item.logicalPath)throw Error('OWNER_HASH_PATH_MISMATCH');
    if(n<59){const expected=p.files.find(x=>x.key===item.key);if(!expected||expected.sha256!==item.sha256||expected.path!==item.requestedPath)throw Error('FILE_ACK_MISMATCH');}
    else if(!item.recordIds.includes(p.identityId)||!p.sicRows.some(x=>x.format===item.format))throw Error('SIC_ACK_MISMATCH');
    proof.cursor=n+1;
  }
  proof.ms+=Date.now()-start;props.setProperty('GBO_VERIFY_59',JSON.stringify(proof));
  if(proof.cursor===all.length){const out=root.getFilesByName('TEST_ACK_VERIFICADO.json');if(out.hasNext())out.next().setContent(raw);else root.createFile(Utilities.newBlob(raw,'application/json','TEST_ACK_VERIFICADO.json'));}
  console.log(JSON.stringify({phase:proof.cursor===all.length?'VERIFIED_59_COMPLETE':'VERIFYING_59',verified:proof.cursor,total:all.length,verificationMs:proof.ms,testOnly:true}));
}
function gboHoldTestLock(){gboPrivateTestRoot();const lock=LockService.getScriptLock();if(!lock.tryLock(1000))throw Error('TEST_BUSY');try{console.log('TEST_LOCK_HELD');Utilities.sleep(35000);}finally{lock.releaseLock();console.log('TEST_LOCK_RELEASED');}}
function gboCreateFresh59Root(){
  const old=gboPrivateTestRoot(),props=PropertiesService.getScriptProperties();
  if(!old.getFilesByName('TEST_ACK_VERIFICADO.json').hasNext()||JSON.parse(props.getProperty('GBO_VERIFY_59')||'{}').cursor!==62)throw Error('RECOVERY_NOT_VERIFIED');
  const snapshot={root:old.getId(),stages:JSON.parse(props.getProperty('GBO_TEST_STAGE_IDS')),stats:JSON.parse(props.getProperty('GBO_RESUME_59_STATS')),verification:JSON.parse(props.getProperty('GBO_VERIFY_59'))};
  if(old.getFilesByName('TEST_RECOVERY_CONFIGURATION.json').hasNext())throw Error('FRESH_ROOT_ALREADY_CREATED');
  old.createFile(Utilities.newBlob(JSON.stringify(snapshot),'application/json','TEST_RECOVERY_CONFIGURATION.json'));
  const fresh=DriveApp.createFolder('GBO_PRUEBAS_PRIVADAS_20261030_'+Utilities.getUuid());
  if(fresh.getOwner().getEmail()!=='reconocimientoinventariovial@gmail.com'||fresh.getSharingAccess()!==DriveApp.Access.PRIVATE)throw Error('FRESH_ROOT_NOT_PRIVATE');
  props.setProperty('GBO_RECOVERY_PREVIOUS_ROOT',old.getId());props.setProperty('GBO_ISOLATED_ROOT_ID',fresh.getId());
  props.setProperty('GBO_TEST_STAGE_IDS','{}');props.setProperty('GBO_RESUME_59_STATS','[]');props.setProperty('GBO_VERIFY_59','{}');props.setProperty('GBO_TEST_FAULT_V1','{}');
  console.log(JSON.stringify({phase:'FRESH_PRIVATE_ROOT_READY',previousRootPreserved:true,testOnly:true}));
}
function gboVerifyNoDuplicates(){
  const root=gboPrivateTestRoot(),it=root.getFilesByName('TEST_ACK_VERIFICADO.json');if(!it.hasNext())throw Error('ACK_NOT_VERIFIED');
  const ack=JSON.parse(it.next().getBlob().getDataAsString()),seen=[],names=new Set();
  function walk(id,depth){if(depth>12)throw Error('UNEXPECTED_DEPTH');let page;
    do{const r=Drive.Files.list({q:"'"+id+"' in parents and trashed=false",fields:'nextPageToken,files(id,name,mimeType,description,parents)',pageSize:1000,pageToken:page,supportsAllDrives:true,includeItemsFromAllDrives:true});
      for(const f of r.files||[]){const k=id+'\0'+f.name;if(names.has(k))throw Error('DUPLICATE_PUBLIC_NAME');names.add(k);if(f.mimeType==='application/vnd.google-apps.folder')walk(f.id,depth+1);else seen.push(f);}page=r.nextPageToken;
    }while(page);
  }
  walk(ack.folderId,0);
  if(seen.length!==59||seen.some(f=>!ack.files.some(a=>a.fileId===f.id)))throw Error('EXTRA_OR_MISSING_PUBLIC_FILE');
  for(const a of ack.sicFiles){const r=Drive.Files.list({q:"'"+a.folderId+"' in parents and name='"+a.format+".xlsx' and trashed=false",fields:'files(id)',pageSize:1000,supportsAllDrives:true,includeItemsFromAllDrives:true});if(r.files?.length!==1||r.files[0].id!==a.fileId)throw Error('DUPLICATE_SIC');}
  console.log(JSON.stringify({phase:'NO_DUPLICATES_VERIFIED',publicFiles:59,sicFiles:3,testOnly:true}));
}
