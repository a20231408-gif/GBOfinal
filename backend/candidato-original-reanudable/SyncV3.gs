/** CANDIDATE FOR ORIGINAL PROJECT. Not deployed. Retains ROOT_FOLDER_ID, protocol 3 and strict final ACK. */
var v3Profile={};
function v3Timed(phase,fn){const t=Date.now();try{return fn();}finally{const x=v3Profile[phase]||(v3Profile[phase]={calls:0,ms:0});x.calls++;x.ms+=Date.now()-t;}}
function v3Yield(){SyncV3.reject('COMMIT_IN_PROGRESS',true);}
function v3TestFault() {} // Fault injection is deliberately absent from the operational candidate.
function v3Enabled() {return PropertiesService.getScriptProperties().getProperty('GBO_SYNC_V3_ENABLED')==='true';}
function commitRecordV3(data) {
  const lock=LockService.getScriptLock();let held=false;
  try {
    if(!v3Enabled())return jsonResponse({ok:false,code:'V3_DISABLED',retryable:false});
    held=lock.tryLock(1000);if(!held)return jsonResponse({ok:false,code:'BUSY',retryable:true});
    if(typeof Drive==='undefined')return jsonResponse({ok:false,code:'ADVANCED_DRIVE_REQUIRED',retryable:false});
    return jsonResponse(v3Timed('commit',()=>SyncV3.commit(data,v3Store(data))));
  }catch(e){return jsonResponse({ok:false,code:e.code || 'COMMUNICATION_OR_STORAGE_ERROR',retryable:e.retryable!==false,commitContinuationVersion:1,...(Number.isSafeInteger(e.remoteRevision)?{remoteRevision:e.remoteRevision}:{})});}
  finally{if(held)lock.releaseLock();console.info(JSON.stringify({metric:'COMMIT_V1',phases:v3Profile}));}
}

function stageRecordV3(p) {
  const lock=LockService.getScriptLock();let held=false;
  try {
    if(!v3Enabled())return jsonResponse({ok:false,code:'V3_DISABLED',retryable:false});
    if(p.syncProtocol!==3 || !/^(SIC|SCAP|PAVEMENT)\/[A-Za-z0-9_-]{1,80}$/.test(p.key) || !Number.isSafeInteger(p.revision) || p.revision<1)SyncV3.reject('INVALID_STAGE');
    held=lock.tryLock(30000);if(!held)return jsonResponse({ok:false,code:'BUSY',retryable:true});
    const s=v3Store(p),f=p.file;
    if(!f || !SyncV3.component(f.key.replace(/\//g,'_')) || !/^[a-f0-9]{64}$/.test(f.sha256))SyncV3.reject('INVALID_STAGE');
    const bytes=s.decode(f.base64);if(!bytes.length || bytes.length>24*1024*1024 || s.hash(bytes)!==f.sha256)SyncV3.reject('HASH_OR_FILE_SIZE');
    if(f.mimeType==='image/jpeg'&&!s.isJpeg(bytes))SyncV3.reject('NOT_JPEG');
    const folder=getOrCreateFolder(getOrCreateFolder(getOrCreateFolder(DriveApp.getFolderById(ROOT_FOLDER_ID),'._GBO_SYNC_V3'),'STAGING'),s.hash(p.key)+'_'+p.revision);
    const name=s.hash(f.key)+'_'+f.sha256+'.bin';const existing=folder.getFilesByName(name);
    let file;
    if(existing.hasNext()){file=existing.next();if(existing.hasNext())SyncV3.reject('DUPLICATE_STAGE');}
    else {file=folder.createFile(Utilities.newBlob(bytes,f.mimeType,name));file.setDescription(JSON.stringify({key:p.key,revision:p.revision,fileKey:f.key,sha256:f.sha256,gboVerifiedV1:{sha256:f.sha256,size:bytes.length,mimeType:f.mimeType,jpeg:true}}));}
    if(s.hash(file.getBlob().getBytes())!==f.sha256)SyncV3.reject('STAGE_HASH_MISMATCH');
    return jsonResponse({ok:true,syncProtocol:3,key:p.key,revision:p.revision,fileKey:f.key,sha256:f.sha256,stageFileId:file.getId()});
  }catch(e){return jsonResponse({ok:false,code:e.code || 'STAGE_COMMUNICATION_ERROR',retryable:e.retryable!==false});}
  finally {if(held)lock.releaseLock();}
}

function v3Store(request) {
  // Explicitly bind activation to the current operational root. Never silently relocate journals/ACKs.
  if(PropertiesService.getScriptProperties().getProperty('GBO_RESUMABLE_ROOT_ID')!==ROOT_FOLDER_ID)
    SyncV3.reject('RESUMABLE_ROOT_NOT_APPROVED');
  if(request?.publicNamingVersion===1&&PropertiesService.getScriptProperties().getProperty('GBO_PUBLIC_NAMES_ENABLED')!=='true')SyncV3.reject('PUBLIC_NAMES_DISABLED');
  const deadline=Date.now()+85000;
  const checkpoint=()=>{if(Date.now()>deadline)v3Yield();};
  const root=DriveApp.getFolderById(ROOT_FOLDER_ID);
  const registry=getOrCreateFolder(root,'._GBO_SYNC_V3');
  const hash=bytes=>Utilities.computeDigest(Utilities.DigestAlgorithm.SHA_256,bytes).map(x=>(x&255).toString(16).padStart(2,'0')).join('');
  const decode=s=>Utilities.base64Decode(s);
  const list=iterator=>{const a=[];while(iterator.hasNext()){checkpoint();a.push(iterator.next());}return a;};
  const names=new Map(),jsons=new Map(),metas=new Map(),folders=new Map();
  const named=(folder,name)=>{const k=folder.getId()+'\0'+name;if(names.has(k))return names.get(k);checkpoint();const a=v3Timed('lookupFile',()=>list(folder.getFilesByName(name)));if(a.length>1)SyncV3.reject('DUPLICATE_REMOTE_NAME');const f=a[0]||null;names.set(k,f);return f;};
  const read=name=>{if(jsons.has(name))return jsons.get(name);const f=named(registry,name);const j=f?v3Timed('readJson',()=>JSON.parse(f.getBlob().getDataAsString())):null;jsons.set(name,j);return j;};
  const cachedFolder=(p,n)=>{const k=p.getId()+'\0'+n;if(!folders.has(k))folders.set(k,v3Timed('folder',()=>getOrCreateFolder(p,n)));return folders.get(k);};
  const meta=id=>{if(!metas.has(id)){checkpoint();metas.set(id,v3Timed('metadata',()=>Drive.Files.get(id,{fields:'id,name,description,mimeType,parents,size,sha256Checksum',supportsAllDrives:true})));}return metas.get(id);};
  const optionalMeta=id=>{try{return meta(id);}catch(e){if(/not found|404/i.test(String(e)))return null;throw e;}};
  const digest=(id,m)=>m.sha256Checksum||v3Timed('downloadVerify',()=>hash(DriveApp.getFileById(id).getBlob().getBytes()));
  const filename=key=>'record_'+hash(key)+'.json';
  const jsonFile=(name,obj)=>v3Timed('journalWrite',()=>{const f=named(registry,name);if(f){f.setContent(JSON.stringify(obj));jsons.set(name,obj);metas.delete(f.getId());return f;}const created=registry.createFile(Utilities.newBlob(JSON.stringify(obj),'application/json',name));names.set(registry.getId()+'\0'+name,created);jsons.set(name,obj);return created;});
  const parent=file=>DriveApp.getFolderById(meta(file.getId()).parents[0]);
  function restore(journal) {
    for(const op of [...journal.operations].reverse()) {
      if(op.folder) {
        const folder=DriveApp.getFolderById(op.id);folder.setName(op.name);folder.moveTo(DriveApp.getFolderById(op.parentId));continue;
      }
      const file=DriveApp.getFileById(op.id);
      if(op.created)file.moveTo(DriveApp.getFolderById(journal.archiveId));
      else {
        const backup=DriveApp.getFileById(op.backupId).getBlob();
        Drive.Files.update({name:op.name,mimeType:backup.getContentType()},op.id,backup,{fields:'id',supportsAllDrives:true});
        file.moveTo(DriveApp.getFolderById(op.parentId));
      }
    }
    journal.status='ROLLED_BACK';jsonFile(journal.name,journal);
  }
  // A pending transaction fences every other owner until its strict final publication.
  // Unlike the legacy adapter, replay reconciles completed operations instead of undoing them.
  const journals=list(registry.getFiles()).filter(f=>f.getName().startsWith('journal_'));
  let pending=null;
  for(const f of journals) {const j=read(f.getName());if(j.status!=='PENDING')continue;
    if(!request||typeof request.key!=='string'||j.name!=='journal_'+hash(request.key)+'.json'||pending)SyncV3.reject('BUSY',true);
    if(!j.continuationVersion)SyncV3.reject('LEGACY_RECOVERY_REQUIRES_REVIEW');
    if(j.continuationVersion===1&&(j.key!==request.key||j.revision!==request.revision||request.contentHash&&j.contentHash!==request.contentHash))SyncV3.reject('REVISION_CONFLICT');
    pending=j;
  }
  const recordRead=key=>pending&&pending.key===key&&Object.prototype.hasOwnProperty.call(pending,'baseRecord')?pending.baseRecord:read(filename(key));
  function unpackWorkbook(bytes,dataStartRow=14) {
    const parts=Utilities.unzip(Utilities.newBlob(bytes,'application/zip','source.xlsx'));
    const map={};parts.forEach(b=>{if(map[b.getName()])SyncV3.reject('INVALID_WORKBOOK');map[b.getName()]=b;});
    if(!map['xl/worksheets/sheet1.xml'] || parts.filter(b=>/^xl\/worksheets\/sheet\d+\.xml$/.test(b.getName())).length!==1 || map['xl/sharedStrings.xml'])SyncV3.reject('UNSUPPORTED_WORKBOOK');
    const sheet=map['xl/worksheets/sheet1.xml'].getDataAsString();
    const content=/<sheetData>([\s\S]*?)<\/sheetData>/.exec(sheet);
    if(!content)SyncV3.reject('INVALID_WORKBOOK');
    const all=(content[1].match(/<row\b[^>]*>[\s\S]*?<\/row>/g)||[]);
    const rows=all.filter(r=>Number(/\br="(\d+)"/.exec(r)[1])>=dataStartRow);
    // Format columns/styles/header geometry must agree; route title text can vary across separate scopes.
    const observations=typeof SicObservationsV1!=='undefined'?SicObservationsV1.inspect(sheet,dataStartRow):null;
    const signature=hash(map['xl/styles.xml'].getBytes())+':'+hash(observations?observations.columns:(/<cols>([\s\S]*?)<\/cols>/.exec(sheet)||[])[1] || '')+
      ':'+hash(observations?observations.header:all.find(r=>/\br="13"/.test(r)) || '');
    return {parts,sheet,rows,signature,dataStartRow,observationColumn:observations?.observationColumn || null};
  }
  function packWorkbook(book,sheet,rowCount) {
    if(typeof SicObservationsV1!=='undefined')sheet=SicObservationsV1.plain(sheet,book.observationColumn);
    const parts=book.parts.map(b=>{
      if(b.getName()==='xl/worksheets/sheet1.xml')return Utilities.newBlob(sheet,'application/xml',b.getName());
      if(b.getName()==='xl/workbook.xml') {
        let xml=b.getDataAsString().replace(/(!\$A\$\d+:\$[A-Z]+\$)\d+/g,(_,start)=>start+((book.dataStartRow || 14)-1+rowCount));
        if(typeof SicObservationsV1!=='undefined')xml=SicObservationsV1.printExtent(xml,book.observationColumn);
        return Utilities.newBlob(xml,'application/xml',b.getName());
      }
      return b.copyBlob();
    });
    return Utilities.zip(parts,'consolidated.xlsx').getBytes();
  }
  function elementLocation(p) {
      const identity=p.identityId || p.recordId;
      const bindingName=(p.pathVersion===4?'binding_v4_':'binding_')+hash(identity)+'.json';let previousBinding=read(bindingName);
      if(p.pathVersion===4 && read('binding_'+hash(identity)+'.json'))SyncV3.reject('HISTORICAL_MIGRATION_REQUIRED');
      const scope=p.kind==='PAVEMENT'?p.routeCode+'/EVALUACION_SUPERFICIAL/'+p.tramo+'/'+p.surface:p.routeCode+'/'+(p.pathVersion===4?p.familyPath:p.sibCode);
      const bindings=list(registry.getFiles()).filter(f=>f.getName().startsWith('binding_')).map(f=>JSON.parse(f.getBlob().getDataAsString()));
      const peers=bindings.filter(b=>b.scope===scope && b.family===p.family && b.identity!==identity);
      let sequence=previousBinding?.sequence;
      const changed=previousBinding && (previousBinding.scope!==scope || previousBinding.family!==p.family);
      if(p.pathVersion===4) {
        sequence=SyncV3.allocateNumber(scope,p.family,identity,bindings.filter(b=>b.pathVersion===4));
      } else if(!previousBinding || changed) sequence=Math.max(0,...peers.map(b=>b.sequence))+1;
      if(p.pathVersion===4 && changed)SyncV3.reject('IDENTITY_RELOCATION_REQUIRES_MIGRATION');
      // Reserve UUID/number before creating a folder. Interrupted allocation can be resumed by its owner.
      if(p.pathVersion===4 && !previousBinding) {
        previousBinding={pathVersion:4,identity,scope,family:p.family,sequence,folderId:null,logicalPath:scope+'/'+(p.publicNamingVersion===1?PublicDeliveryNames.component(p.family+' '+sequence+' - '+p.publicDisplayLabel,80):p.family+' '+sequence)};
        jsonFile(bindingName,previousBinding).setDescription('gbo-v3-metadata:'+bindingName);
      }
      if(p.publicNamingVersion===1&&p.contentHash&&p.publicElementFolder!==previousBinding?.logicalPath?.split('/').pop())SyncV3.reject('PUBLIC_BINDING_CONFLICT');
      let target=root;for(const part of scope.split('/'))target=cachedFolder(target,part);
      let logicalPath=scope;let folder=target;
      if(p.pathVersion===4 || p.kind!=='PAVEMENT') {
        const name=previousBinding?.logicalPath?.split('/').pop()||p.family+' '+sequence;logicalPath+='/'+name;
        if(previousBinding?.folderId)folder=DriveApp.getFolderById(previousBinding.folderId);
        else {
          const matches=list(target.getFoldersByName(name));
          if(matches.length>1)SyncV3.reject('DUPLICATE_REMOTE_FOLDER');
          if(matches.length && matches[0].getDescription()!=='gbo-v3-owner:'+identity)SyncV3.reject('HISTORICAL_BINDING_REQUIRED');
          folder=matches[0] || target.createFolder(name);folder.setDescription('gbo-v3-owner:'+identity);
        }
        if(folder.getDescription()!=='gbo-v3-owner:'+identity)SyncV3.reject('FOLDER_OWNERSHIP_CONFLICT');
      }
      // Durable reservation while ScriptLock is held: failed delivery retries retain the UUID number.
      if(p.pathVersion===4 && !previousBinding.folderId) {
        previousBinding={...previousBinding,folderId:folder.getId(),logicalPath};
        jsonFile(bindingName,previousBinding).setDescription('gbo-v3-metadata:'+bindingName);
      }
      return {folder,logicalPath,sequence,identity,scope,bindingName,previousBinding,changed,target};
  }
  function stageInfo(p,f) {
    if(!f.stageFileId)SyncV3.reject('STAGING_REQUIRED');
    const info=meta(f.stageFileId),d=JSON.parse(info.description);
    if(d.key!==p.key||d.revision!==p.revision||d.fileKey!==f.key||d.sha256!==f.sha256)SyncV3.reject('STAGE_OWNERSHIP_CONFLICT');
    const stage=meta(info.parents[0]),staging=meta(stage.parents[0]);
    if(info.parents.length!==1||stage.name!==hash(p.key)+'_'+p.revision||staging.name!=='STAGING'||staging.parents[0]!==registry.getId())SyncV3.reject('STAGE_LOCATION_CONFLICT');
    return {info,d};
  }
  return {
    hash,decode,unpackWorkbook:(...a)=>v3Timed('sicUnpack',()=>unpackWorkbook(...a)),packWorkbook:(...a)=>v3Timed('sicConsolidate',()=>packWorkbook(...a)),
    deferBytes:true,
    validateStage(p,f) {
      const {info,d}=stageInfo(p,f),proof=d.gboVerifiedV1;
      if(info.mimeType!==f.mimeType||Number(info.size)<1)SyncV3.reject('INVALID_STAGE');
      if(info.sha256Checksum&&info.sha256Checksum!==f.sha256)SyncV3.reject('HASH_MISMATCH');
      if(proof&&proof.sha256===f.sha256&&proof.size===Number(info.size)&&proof.mimeType===f.mimeType&&proof.jpeg===true&&info.sha256Checksum===f.sha256)return {size:proof.size};
      checkpoint();const bytes=v3Timed('validateBytes',()=>DriveApp.getFileById(f.stageFileId).getBlob().getBytes());
      if(hash(bytes)!==f.sha256)SyncV3.reject('HASH_MISMATCH');
      if(f.mimeType==='image/jpeg'&&!((bytes[0]&255)===255&&(bytes[1]&255)===216&&(bytes[bytes.length-2]&255)===255&&(bytes[bytes.length-1]&255)===217))SyncV3.reject('NOT_JPEG');
      d.gboVerifiedV1={sha256:f.sha256,size:bytes.length,mimeType:f.mimeType,jpeg:true};
      v3Timed('validationProof',()=>Drive.Files.update({description:JSON.stringify(d)},f.stageFileId,null,{fields:'id',supportsAllDrives:true}));metas.delete(f.stageFileId);return {size:bytes.length};
    },
    readablePhotoName(p,f) {const names=read('photo_names_v2_'+hash(p.identityId || p.recordId)+'.json');const saved=names?.photos?.[f.key.substring(6)];if(!saved || saved.category!==f.photoCategory)SyncV3.reject('PHOTO_NAME_RESERVATION_REQUIRED');return saved.name;},
    reserveReadablePhotos(p) {
      const family=SyncV3.families[p.family];
      if(p.kind!=='PAVEMENT' && (!family || p.familyPath!==family[0]) || p.kind==='SCAP' && p.family!=='PUENTE' ||
         p.kind==='SIC' && p.identityId!=null && p.identityId!==p.recordId || p.kind==='SCAP' && !SyncV3.component(p.identityId) ||
         p.kind==='PAVEMENT' && (p.family!=='MUESTRA' || !SyncV3.component(p.tramo) || !['ASFALTICO','RIGIDO','NO_PAVIMENTADO'].includes(p.surface)))SyncV3.reject('INVALID_FAMILY_LOCATION');
      return PhotoNamesV2.reserve(p,{record:key=>read(filename(key)),element:elementLocation,
        names:id=>read('photo_names_v2_'+hash(id)+'.json'),saveNames:(id,value)=>jsonFile('photo_names_v2_'+hash(id)+'.json',value)});
    },
    fileBytes(p,f) {
      if(!f.stageFileId)return decode(f.base64);
      const file=DriveApp.getFileById(f.stageFileId),info=meta(f.stageFileId),description=JSON.parse(info.description);
      if(description.key!==p.key || description.revision!==p.revision || description.fileKey!==f.key || description.sha256!==f.sha256)SyncV3.reject('STAGE_OWNERSHIP_CONFLICT');
      const expected=hash(p.key)+'_'+p.revision;
      const stage=meta(info.parents[0]),staging=meta(stage.parents[0]);
      if(stage.name!==expected || staging.name!=='STAGING' || staging.parents[0]!==registry.getId())SyncV3.reject('STAGE_LOCATION_CONFLICT');
      return v3Timed('stageDownload',()=>file.getBlob().getBytes());
    },
    isJpeg:bytes=>(bytes[0]&255)===255 && (bytes[1]&255)===216 && (bytes[bytes.length-2]&255)===255 && (bytes[bytes.length-1]&255)===217,
    get:recordRead,
    all:()=>list(registry.getFiles()).filter(f=>f.getName().startsWith('record_')).map(f=>read(f.getName())).filter(x=>!pending||x.key!==pending.key).concat(pending?.baseRecord?[pending.baseRecord]:[]),
    begin(p,next,all) {
      const {folder,logicalPath,sequence,identity,scope,bindingName,previousBinding,changed,target}=elementLocation(p);
      const archive=cachedFolder(cachedFolder(cachedFolder(registry,'REVISIONS'),hash(p.key)),p.revision+'_'+p.contentHash.substring(0,8));
      const old=recordRead(p.key);const oldFiles=old?.files || [];
      const journal=pending||{name:'journal_'+hash(p.key)+'.json',status:'PENDING',archiveId:archive.getId(),operations:[]};
      if(!pending){const prior=read(journal.name);if(prior)jsonFile('evidence_complete_'+hash(p.key)+'_'+prior.revision+'_'+Date.now()+'.json',JSON.parse(JSON.stringify(prior)));}
      if(journal.archiveId!==archive.getId())SyncV3.reject('PENDING_REVISION_MISMATCH');
      if(!journal.continuationVersion){
        if(pending)jsonFile('evidence_legacy_'+hash(p.key)+'_'+Date.now()+'.json',JSON.parse(JSON.stringify(journal)));
        Object.assign(journal,{continuationVersion:1,key:p.key,revision:p.revision,contentHash:p.contentHash,baseRecord:old,startedAt:Date.now()});
      }
      pending=journal;
      const persist=()=>jsonFile(journal.name,journal);persist();
      const newId=()=>v3Timed('allocateId',()=>Drive.Files.generateIds({count:1,space:'drive',type:'files'}).ids[0]);
      const create=(id,bytes,mime,name,parentId,owner)=>{
        v3Timed('createFile',()=>Drive.Files.create({id,name,mimeType:mime,parents:[parentId],description:owner},Utilities.newBlob(bytes,mime,name),{fields:'id',supportsAllDrives:true}));metas.delete(id);names.set(parentId+'\0'+name,DriveApp.getFileById(id));v3TestFault('afterCreate',owner);
      };
      if(!old&&!journal.mediaPlanned){
        // Allocate the complete immutable media intent before any write. Per-file completion
        // is reconciled from Drive; flushing it with the next checkpoint avoids two journal
        // round trips for every photograph without losing the preallocated identity.
        for(const op of journal.operations.filter(x=>x.created&&!x.owner)){
          const m=meta(op.id),f=p.files.find(f=>'gbo-v3-file:'+p.key+':'+f.key===m.description);
          if(!f||digest(op.id,m)!==f.sha256||m.name!==f.path.split('/').pop())SyncV3.reject('LEGACY_OPERATION_MISMATCH');
          Object.assign(op,{owner:m.description,expectedSha:f.sha256,name:m.name,mime:f.mimeType,requestedPath:f.path});
        }
        const missing=p.files.filter(f=>!journal.operations.some(op=>op.owner==='gbo-v3-file:'+p.key+':'+f.key));
        const ids=missing.length?v3Timed('allocateMediaIds',()=>Drive.Files.generateIds({count:missing.length,space:'drive',type:'files'}).ids):[];
        if(ids.length!==missing.length||new Set(ids).size!==ids.length)SyncV3.reject('ID_ALLOCATION_FAILED',true);
        missing.forEach((f,n)=>journal.operations.push({id:ids[n],created:true,owner:'gbo-v3-file:'+p.key+':'+f.key,expectedSha:f.sha256,name:f.path.split('/').pop(),mime:f.mimeType,requestedPath:f.path}));
        journal.mediaPlanned=true;persist();
      }
      function protect(file,created=false) {
        const existing=journal.operations.find(x=>x.id===file.getId());if(existing)return existing;
        const op={id:file.getId(),created};
        if(!created){const m=meta(file.getId());op.backupId=newId();op.parentId=m.parents[0];op.name=m.name;op.originalSha=digest(m.id,m);op.mime=m.mimeType;}
        journal.operations.push(op);persist();return op;
      }
      function write(bytes,mime,name,parentFolder,oldId,owner,expectedHint) {
        const expectedSha=expectedHint||hash(bytes),parentId=parentFolder.getId();
        const source=()=>typeof bytes==='function'?bytes():bytes;
        let op=journal.operations.find(x=>x.owner===owner);
        if(!op && !oldId) {
          // Adopt legacy writes only by immutable description; never by visible name alone.
          const candidates=journal.operations.filter(x=>x.created&&!x.owner).filter(x=>optionalMeta(x.id)?.description===owner);
          if(candidates.length>1)SyncV3.reject('DUPLICATE_REMOTE_NAME');op=candidates[0];
        }
        if(!op && oldId)op=protect(DriveApp.getFileById(oldId));
        if(!op){checkpoint();if(named(parentFolder,name))SyncV3.reject('HISTORICAL_BINDING_REQUIRED');op={id:newId(),created:true};journal.operations.push(op);}
        if(op.owner&&op.owner!==owner)SyncV3.reject('FILE_OWNERSHIP_CONFLICT');
        if(op.expectedSha&&op.expectedSha!==expectedSha)SyncV3.reject('PENDING_CONTENT_CHANGED');
        if(!op.owner){Object.assign(op,{owner,expectedSha,name,targetParentId:parentId,mime});persist();}
        let current=optionalMeta(op.id);
        if(current && current.description!==owner)SyncV3.reject('FILE_OWNERSHIP_CONFLICT');
        if(current && digest(op.id,current)===expectedSha && current.name===name && current.mimeType===mime && current.parents.length===1 && current.parents[0]===parentId){op.completed=true;return DriveApp.getFileById(op.id);}
        checkpoint();
        if(op.created){
          const collision=named(parentFolder,name);if(collision&&collision.getId()!==op.id)SyncV3.reject('HISTORICAL_BINDING_REQUIRED');
          if(current){
            if(digest(op.id,current)!==expectedSha)SyncV3.reject('WRITE_HASH_MISMATCH');
            if(![archive.getId(),parentId].includes(current.parents[0]))SyncV3.reject('FILE_OWNERSHIP_CONFLICT');
            v3Timed('moveFile',()=>Drive.Files.update({name},op.id,null,{addParents:parentId,removeParents:current.parents.filter(x=>x!==parentId).join(','),fields:'id',supportsAllDrives:true}));
          }else create(op.id,source(),mime,name,parentId,owner);
        }else{
          if(!current)SyncV3.reject('OWNED_FILE_MISSING');
          let backup=optionalMeta(op.backupId);
          if(!backup){if(digest(op.id,current)!==op.originalSha)SyncV3.reject('ORIGINAL_BACKUP_MISSING');create(op.backupId,DriveApp.getFileById(op.id).getBlob().getBytes(),op.mime,op.name,archive.getId(),'gbo-v3-backup:'+op.id);backup=meta(op.backupId);}
          if(digest(op.backupId,backup)!==op.originalSha)SyncV3.reject('BACKUP_HASH_MISMATCH');
          v3Timed('updateFile',()=>Drive.Files.update({name,mimeType:mime},op.id,Utilities.newBlob(source(),mime,name),{addParents:parentId,removeParents:current.parents.filter(x=>x!==parentId).join(','),fields:'id',supportsAllDrives:true}));
          v3TestFault('afterUpdate',owner);
        }
        metas.delete(op.id);const actual=meta(op.id);
        if(actual.description!==owner||actual.name!==name||actual.mimeType!==mime||actual.parents.length!==1||actual.parents[0]!==parentId||digest(op.id,actual)!==expectedSha)SyncV3.reject('WRITE_HASH_MISMATCH',true);
        op.completed=true;return DriveApp.getFileById(op.id);
      }
      function metadata(name,obj) {
        const oldFile=named(registry,name);
        write(Utilities.newBlob(JSON.stringify(obj)).getBytes(),'application/json',name,registry,oldFile?.getId(),'gbo-v3-metadata:'+name);
      }
      const transaction={folderId:folder.getId(),logicalPath,
        file(f) {
          let requestedPath=f.path;let path=f.path;
          if(p.kind==='PAVEMENT') {
            path=path.replace(/(HC_|M_)\d+(\.)/,(_,prefix,suffix)=>prefix+String(sequence).padStart(3,'0')+suffix);
            if(f.key.startsWith('photo/') && (p.photoNamingVersion!==2 || read('photo_names_v2_'+hash(p.identityId || p.recordId)+'.json')?.photos?.[f.key.substring(6)]?.historical))path=path.replace(/[^/]+$/,name=>'M'+String(sequence).padStart(3,'0')+'_'+name);
          }
          const pieces=path.split('/');const name=pieces.pop();let dest=folder;pieces.forEach(part=>dest=cachedFolder(dest,part));
          const previous=oldFiles.find(x=>x.key===f.key);
          let file;
          if(previous && previous.sha256===f.sha256 && previous.logicalPath===logicalPath+'/'+path) {
            file=DriveApp.getFileById(previous.fileId);
            if(file.getDescription()!=='gbo-v3-file:'+p.key+':'+f.key || parent(file).getId()!==dest.getId())SyncV3.reject('FILE_OWNERSHIP_CONFLICT');
            if(hash(file.getBlob().getBytes())!==f.sha256)SyncV3.reject('WRITE_HASH_MISMATCH',true);
          }else {
            const source=()=>{checkpoint();const bytes=f.bytes||v3Timed('writeSourceBytes',()=>DriveApp.getFileById(f.stageFileId).getBlob().getBytes());if(hash(bytes)!==f.sha256)SyncV3.reject('HASH_MISMATCH');return bytes;};
            file=write(source,f.mimeType,name,dest,previous?.fileId,'gbo-v3-file:'+p.key+':'+f.key,f.sha256);
          }
          return {key:f.key,sha256:f.sha256,fileId:file.getId(),folderId:dest.getId(),requestedPath,logicalPath:logicalPath+'/'+path};
        },
        aggregate(f) {
          if(!f.retire){
            const plans=journal.aggregatePlans||(journal.aggregatePlans={}),key=f.scope+'/'+f.format;
            if(!plans[key]){plans[key]={base64:Utilities.base64Encode(f.bytes),sha256:f.sha256,recordIds:f.recordIds};persist();}
            const plan=plans[key];
            if(JSON.stringify(plan.recordIds)!==JSON.stringify(f.recordIds))SyncV3.reject('PENDING_AGGREGATE_CHANGED');
            f={...f,bytes:decode(plan.base64),sha256:plan.sha256};
            if(hash(f.bytes)!==f.sha256)SyncV3.reject('AGGREGATE_PLAN_HASH_MISMATCH');
          }
          const name='aggregate_'+hash(f.scope+'/'+f.format)+'.json';const binding=read(name);
          if(f.retire) {
            if(binding){const file=DriveApp.getFileById(binding.fileId);protect(file);file.moveTo(archive);metadata(name,{...binding,retired:true});}
            return null;
          }
          let dest=root;f.scope.split('/').forEach(part=>dest=cachedFolder(dest,part));
          const file=write(f.bytes,'application/vnd.openxmlformats-officedocument.spreadsheetml.sheet',f.format+'.xlsx',dest,binding?.fileId,'gbo-v3-aggregate:'+f.scope+'/'+f.format);
          metadata(name,{fileId:file.getId()});
          return {format:f.format,fileId:file.getId(),folderId:dest.getId(),logicalPath:f.scope+'/'+f.format+'.xlsx',sha256:f.sha256,recordIds:f.recordIds};
        },
        retireMissing(keys) {
          oldFiles.filter(f=>!keys.includes(f.key)).forEach(f=>{
            const file=DriveApp.getFileById(f.fileId);
            if(file.getDescription()!=='gbo-v3-file:'+p.key+':'+f.key)SyncV3.reject('FILE_OWNERSHIP_CONFLICT');
            protect(file);file.moveTo(archive);
          });
        },
        save(value) {
          metadata(filename(p.key),value);
          metadata(bindingName,{pathVersion:p.pathVersion,identity,scope,family:p.family,sequence,orderAxis:p.orderAxis,orderSource:p.orderSource || 'GNSS_MAP_MATCH',folderId:folder.getId(),logicalPath});
          if(p.pathVersion===3 && p.kind!=='PAVEMENT' && previousBinding && changed){
            // Preserve shared bridge identities; never move another record's SCAP/SIC silently.
            if(all.some(x=>x.key!==p.key && x.identityId===identity && x.routeCode+'/'+x.sibCode!==scope))SyncV3.reject('SHARED_IDENTITY_RELOCATION_REQUIRED');
            // Folder relocation has its own recovery entry.
            const op={id:folder.getId(),folder:true,parentId:parent(folder).getId(),name:folder.getName()};
            journal.operations.push(op);persist();folder.setName(p.family+' '+sequence);folder.moveTo(target);
          }
        },
        finish(){journal.status='COMPLETE';journal.finishedAt=Date.now();persist();pending=null;},
        suspend(e){journal.lastError=e.code||'UNCONFIRMED_DRIVE_OPERATION';persist();},
        rollback(){restore(journal);}
      };
      return transaction;
    }
  };
}

function reservePhotoNamesV2(p) {
  const lock=LockService.getScriptLock();let held=false;
  try {
    if(!v3Enabled() || PropertiesService.getScriptProperties().getProperty('GBO_PHOTO_NAMES_V2_ENABLED')!=='true' || typeof PhotoNamesV2==='undefined')SyncV3.reject('READABLE_NAMES_DISABLED');
    held=lock.tryLock(30000);if(!held)SyncV3.reject('BUSY',true);
    return jsonResponse(v3Store(p).reserveReadablePhotos(p));
  }catch(e){return jsonResponse({ok:false,code:e.code || 'NAME_RESERVATION_ERROR',retryable:e.retryable===true});}
  finally {if(held)lock.releaseLock();}
}
