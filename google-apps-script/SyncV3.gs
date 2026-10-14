/** Add alongside Code.gs + SyncV3Core.js (as .gs). Never deploy without owner authorization. */
function v3Enabled() {return PropertiesService.getScriptProperties().getProperty('GBO_SYNC_V3_ENABLED')==='true';}
function commitRecordV3(data) {
  const lock=LockService.getScriptLock();let held=false;
  try {
    if(!v3Enabled())return jsonResponse({ok:false,code:'V3_DISABLED',retryable:false});
    held=lock.tryLock(30000);if(!held)return jsonResponse({ok:false,code:'BUSY',retryable:true});
    if(typeof Drive==='undefined')return jsonResponse({ok:false,code:'ADVANCED_DRIVE_REQUIRED',retryable:false});
    return jsonResponse(SyncV3.commit(data,v3Store()));
  }catch(e){return jsonResponse({ok:false,code:e.code || 'COMMUNICATION_OR_STORAGE_ERROR',retryable:e.retryable!==false,...(Number.isSafeInteger(e.remoteRevision)?{remoteRevision:e.remoteRevision}:{})});}
  finally{if(held)lock.releaseLock();}
}

function stageRecordV3(p) {
  const lock=LockService.getScriptLock();let held=false;
  try {
    if(!v3Enabled())return jsonResponse({ok:false,code:'V3_DISABLED',retryable:false});
    if(p.syncProtocol!==3 || !/^(SIC|SCAP|PAVEMENT)\/[A-Za-z0-9_-]{1,80}$/.test(p.key) || !Number.isSafeInteger(p.revision) || p.revision<1)SyncV3.reject('INVALID_STAGE');
    held=lock.tryLock(30000);if(!held)return jsonResponse({ok:false,code:'BUSY',retryable:true});
    const s=v3Store(),f=p.file;
    if(!f || !SyncV3.component(f.key.replace(/\//g,'_')) || !/^[a-f0-9]{64}$/.test(f.sha256))SyncV3.reject('INVALID_STAGE');
    const bytes=s.decode(f.base64);if(!bytes.length || bytes.length>24*1024*1024 || s.hash(bytes)!==f.sha256)SyncV3.reject('HASH_OR_FILE_SIZE');
    const folder=getOrCreateFolder(getOrCreateFolder(getOrCreateFolder(DriveApp.getFolderById(ROOT_FOLDER_ID),'._GBO_SYNC_V3'),'STAGING'),s.hash(p.key)+'_'+p.revision);
    const name=s.hash(f.key)+'_'+f.sha256+'.bin';const existing=folder.getFilesByName(name);
    let file;
    if(existing.hasNext()){file=existing.next();if(existing.hasNext())SyncV3.reject('DUPLICATE_STAGE');}
    else {file=folder.createFile(Utilities.newBlob(bytes,f.mimeType,name));file.setDescription(JSON.stringify({key:p.key,revision:p.revision,fileKey:f.key,sha256:f.sha256}));}
    if(s.hash(file.getBlob().getBytes())!==f.sha256)SyncV3.reject('STAGE_HASH_MISMATCH');
    return jsonResponse({ok:true,syncProtocol:3,key:p.key,revision:p.revision,fileKey:f.key,sha256:f.sha256,stageFileId:file.getId()});
  }catch(e){return jsonResponse({ok:false,code:e.code || 'STAGE_COMMUNICATION_ERROR',retryable:e.retryable!==false});}
  finally {if(held)lock.releaseLock();}
}

function v3Store() {
  const root=DriveApp.getFolderById(ROOT_FOLDER_ID);
  const registry=getOrCreateFolder(root,'._GBO_SYNC_V3');
  const hash=bytes=>Utilities.computeDigest(Utilities.DigestAlgorithm.SHA_256,bytes).map(x=>(x&255).toString(16).padStart(2,'0')).join('');
  const decode=s=>Utilities.base64Decode(s);
  const list=iterator=>{const a=[];while(iterator.hasNext())a.push(iterator.next());return a;};
  const named=(folder,name)=>{const a=list(folder.getFilesByName(name));if(a.length>1)SyncV3.reject('DUPLICATE_REMOTE_NAME');return a[0] || null;};
  const read=name=>{const f=named(registry,name);return f?JSON.parse(f.getBlob().getDataAsString()):null;};
  const filename=key=>'record_'+hash(key)+'.json';
  const jsonFile=(name,obj)=>{const f=named(registry,name);if(f){f.setContent(JSON.stringify(obj));return f;}return registry.createFile(Utilities.newBlob(JSON.stringify(obj),'application/json',name));};
  const parent=file=>list(file.getParents())[0];
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
  // Recover every interrupted transaction before reading any receipt or consolidated row.
  const journals=list(registry.getFiles()).filter(f=>f.getName().startsWith('journal_'));
  for(const f of journals) {const j=JSON.parse(f.getBlob().getDataAsString());if(j.status==='PENDING')restore(j);}
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
        previousBinding={pathVersion:4,identity,scope,family:p.family,sequence,folderId:null,logicalPath:scope+'/'+p.family+' '+sequence};
        jsonFile(bindingName,previousBinding).setDescription('gbo-v3-metadata:'+bindingName);
      }
      let target=root;for(const part of scope.split('/'))target=getOrCreateFolder(target,part);
      let logicalPath=scope;let folder=target;
      if(p.pathVersion===4 || p.kind!=='PAVEMENT') {
        const name=p.family+' '+sequence;logicalPath+='/'+name;
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
  return {
    hash,decode,unpackWorkbook,packWorkbook,
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
      const file=DriveApp.getFileById(f.stageFileId),meta=JSON.parse(file.getDescription());
      if(meta.key!==p.key || meta.revision!==p.revision || meta.fileKey!==f.key || meta.sha256!==f.sha256)SyncV3.reject('STAGE_OWNERSHIP_CONFLICT');
      const expected=hash(p.key)+'_'+p.revision;
      const stage=parent(file);
      if(stage.getName()!==expected || parent(stage).getName()!=='STAGING' || parent(parent(stage)).getId()!==registry.getId())SyncV3.reject('STAGE_LOCATION_CONFLICT');
      return file.getBlob().getBytes();
    },
    isJpeg:bytes=>(bytes[0]&255)===255 && (bytes[1]&255)===216 && (bytes[bytes.length-2]&255)===255 && (bytes[bytes.length-1]&255)===217,
    get:key=>read(filename(key)),
    all:()=>list(registry.getFiles()).filter(f=>f.getName().startsWith('record_')).map(f=>JSON.parse(f.getBlob().getDataAsString())),
    begin(p,next,all) {
      const {folder,logicalPath,sequence,identity,scope,bindingName,previousBinding,changed,target}=elementLocation(p);
      const archive=getOrCreateFolder(getOrCreateFolder(getOrCreateFolder(registry,'REVISIONS'),hash(p.key)),p.revision+'_'+p.contentHash.substring(0,8));
      const journal={name:'journal_'+hash(p.key)+'.json',status:'PENDING',archiveId:archive.getId(),operations:[]};
      const persist=()=>jsonFile(journal.name,journal);persist();
      const old=read(filename(p.key));const oldFiles=old?.files || [];
      function protect(file,created=false) {
        if(journal.operations.some(x=>x.id===file.getId()))return;
        const op={id:file.getId(),created};
        if(!created){op.backupId=file.makeCopy(file.getName(),archive).getId();op.parentId=parent(file).getId();op.name=file.getName();}
        journal.operations.push(op);persist();
      }
      function write(bytes,mime,name,parentFolder,oldId,owner) {
        let file;
        if(oldId) {
          file=DriveApp.getFileById(oldId);
          if(file.getDescription()!==owner)SyncV3.reject('FILE_OWNERSHIP_CONFLICT');
          protect(file);
          Drive.Files.update({name,mimeType:mime},oldId,Utilities.newBlob(bytes,mime,name),{fields:'id',supportsAllDrives:true});
        }else {
          if(named(parentFolder,name))SyncV3.reject('HISTORICAL_BINDING_REQUIRED');
          file=archive.createFile(Utilities.newBlob(bytes,mime,name));file.setDescription(owner);protect(file,true);
        }
        file.moveTo(parentFolder);
        if(hash(file.getBlob().getBytes())!==hash(bytes))SyncV3.reject('WRITE_HASH_MISMATCH',true);
        return file;
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
          const pieces=path.split('/');const name=pieces.pop();let dest=folder;pieces.forEach(part=>dest=getOrCreateFolder(dest,part));
          const previous=oldFiles.find(x=>x.key===f.key);
          let file;
          if(previous && previous.sha256===f.sha256 && previous.logicalPath===logicalPath+'/'+path) {
            file=DriveApp.getFileById(previous.fileId);
            if(file.getDescription()!=='gbo-v3-file:'+p.key+':'+f.key || parent(file).getId()!==dest.getId())SyncV3.reject('FILE_OWNERSHIP_CONFLICT');
            if(hash(file.getBlob().getBytes())!==f.sha256)SyncV3.reject('WRITE_HASH_MISMATCH',true);
          }else file=write(f.bytes,f.mimeType,name,dest,previous?.fileId,'gbo-v3-file:'+p.key+':'+f.key);
          return {key:f.key,sha256:f.sha256,fileId:file.getId(),folderId:dest.getId(),requestedPath,logicalPath:logicalPath+'/'+path};
        },
        aggregate(f) {
          const name='aggregate_'+hash(f.scope+'/'+f.format)+'.json';const binding=read(name);
          if(f.retire) {
            if(binding){const file=DriveApp.getFileById(binding.fileId);protect(file);file.moveTo(archive);metadata(name,{...binding,retired:true});}
            return null;
          }
          let dest=root;f.scope.split('/').forEach(part=>dest=getOrCreateFolder(dest,part));
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
        finish(){journal.status='COMPLETE';persist();},
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
    return jsonResponse(v3Store().reserveReadablePhotos(p));
  }catch(e){return jsonResponse({ok:false,code:e.code || 'NAME_RESERVATION_ERROR',retryable:e.retryable===true});}
  finally {if(held)lock.releaseLock();}
}
