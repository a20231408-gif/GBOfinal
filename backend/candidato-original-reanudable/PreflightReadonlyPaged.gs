/** Optional editor-only auditor. Never called by doGet/doPost; no property or Drive writes.
 * Fixed hash partitions avoid the six-minute ceiling without a persisted cursor.
 * Every partition takes two complete metadata snapshots; combine only matching signatures.
 */
function gboPilotInventoryReadonlyBatch_(bucket) {
  if(!/^[0-9a-f]$/.test(bucket))throw Error('INVALID_READONLY_BUCKET');
  const started=Date.now(),deadline=started+240000;
  const executor=Drive.About.get({fields:'user(emailAddress)'}).user?.emailAddress;
  if(executor!=='reconocimientoinventariovial@gmail.com')throw Error('PREFLIGHT_WRONG_EXECUTOR');
  const root=Drive.Files.get(ROOT_FOLDER_ID,{fields:'id,name,trashed,owners(emailAddress),capabilities(canEdit,canAddChildren)',supportsAllDrives:true});
  const folders=DriveApp.getFolderById(ROOT_FOLDER_ID).getFoldersByName('._GBO_SYNC_V3');
  if(!folders.hasNext())throw Error('REGISTRY_NOT_FOUND_NO_FOLDER_CREATED');
  const registry=folders.next();if(folders.hasNext())throw Error('DUPLICATE_REGISTRY');
  function listAll(){
    const files=[],seen=new Set();let token,pages=0;
    do{
      if(Date.now()>deadline)throw Error('READONLY_METADATA_BUDGET_EXCEEDED');
      const page=Drive.Files.list({q:"'"+registry.getId()+"' in parents and trashed=false",pageSize:100,pageToken:token,
        fields:'nextPageToken,files(id,name,mimeType,modifiedTime)',orderBy:'name',supportsAllDrives:true,includeItemsFromAllDrives:true});
      pages++;for(const f of page.files||[]){if(seen.has(f.id))throw Error('UNSTABLE_PAGINATION_DUPLICATE');seen.add(f.id);files.push(f);}
      if(token&&page.nextPageToken===token)throw Error('UNSTABLE_PAGINATION_REPEATED_TOKEN');token=page.nextPageToken;
    }while(token);return {files,pages};
  }
  function signature(files){return Utilities.computeDigest(Utilities.DigestAlgorithm.SHA_256,
    files.map(f=>f.id+'\t'+f.name+'\t'+f.modifiedTime).sort().join('\n')).map(b=>(b&255).toString(16).padStart(2,'0')).join('');}
  const first=listAll(),before=signature(first.files),pattern=/^(journal|record|binding(?:_v4)?)_([a-f0-9]{64})\.json$/;
  const names=new Set(),duplicates=[];
  for(const f of first.files){if(names.has(f.name))duplicates.push(f.name);names.add(f.name);}
  const targets=first.files.filter(f=>pattern.test(f.name)&&f.name.match(pattern)[2][0]===bucket).sort((a,b)=>a.name.localeCompare(b.name)||a.id.localeCompare(b.id));
  const base={schema:1,readOnly:true,bucket,rootId:root.id,executor,signature:before,registryEntries:first.files.length,
    relevantEntries:first.files.filter(f=>pattern.test(f.name)).length,photoReservations:first.files.filter(f=>f.name.startsWith('photo_names_v2_')).length,
    targets:targets.length,pages:first.pages,duplicates,mutations:0};
  console.log('GBO_READONLY_BEGIN '+JSON.stringify(base));let processed=0;
  for(const f of targets){
    if(Date.now()>deadline)break;
    const kind=f.name.match(pattern)[1];let row={id:f.id,name:f.name,kind,modifiedTime:f.modifiedTime};
    try{
      const value=JSON.parse(DriveApp.getFileById(f.id).getBlob().getDataAsString());
      if(!value||typeof value!=='object'||Array.isArray(value))throw Error('INVALID_JSON_OBJECT');
      if(kind==='journal')Object.assign(row,{status:typeof value.status==='string'?value.status:'UNKNOWN',key:value.key||null,
        revision:value.revision??null,continuationVersion:value.continuationVersion??null,
        operations:Array.isArray(value.operations)?value.operations.length:null,
        operationIds:Array.isArray(value.operations)?value.operations.map(o=>o.id).filter(Boolean):[],requiresOldRoot:true});
      else if(kind==='record'){
        const ack=value.receipt;
        Object.assign(row,{key:value.key||null,identityId:value.identityId||value.recordId||null,revision:value.revision??null,
          contentHash:value.contentHash||null,pathVersion:value.pathVersion??null,routeCode:value.routeCode||null,
          familyPath:value.familyPath||null,sibCode:value.sibCode||null,number:value.number??null,requiresOldRoot:true,
          ack:ack?{ok:ack.ok,key:ack.key,revision:ack.revision,contentHash:ack.contentHash,folderId:ack.folderId,
            logicalPath:ack.logicalPath,pathVersion:ack.pathVersion,files:Array.isArray(ack.files)?ack.files.length:null,
            sicFiles:Array.isArray(ack.sicFiles)?ack.sicFiles.length:null}:null});
        const refs=[...(Array.isArray(ack?.files)?ack.files:[]),...(Array.isArray(ack?.sicFiles)?ack.sicFiles:[])].map(r=>{
          const clean={};for(const key of ['id','fileId','key','name','logicalPath','relativePath','sha256','size','format'])if(r[key]!==undefined)clean[key]=r[key];return clean;});
        for(let offset=0;offset<refs.length;offset+=5)console.log('GBO_READONLY_REFS '+JSON.stringify({bucket,recordFileId:f.id,offset,refs:refs.slice(offset,offset+5)}));
      }else Object.assign(row,{identity:value.identity||null,folderId:value.folderId||null,pathVersion:value.pathVersion??null,
        scope:value.scope||null,logicalPath:value.logicalPath||null,requiresOldRoot:true});
    }catch(e){row.unreadable=true;row.requiresReview=true;}
    console.log('GBO_READONLY_ROW '+JSON.stringify(row));processed++;
  }
  const second=listAll(),after=signature(second.files);
  const report={...base,processed,complete:processed===targets.length,stableDuringRead:before===after,verificationSignature:after,
    verificationPages:second.pages,elapsedMs:Date.now()-started,rootOwner:root.owners?.map(o=>o.emailAddress)||[],
    canEdit:root.capabilities?.canEdit===true,canAddChildren:root.capabilities?.canAddChildren===true};
  console.log('GBO_READONLY_END '+JSON.stringify(report));return report;
}
function gboPilotInventoryReadonly00(){return gboPilotInventoryReadonlyBatch_('0');}
function gboPilotInventoryReadonly01(){return gboPilotInventoryReadonlyBatch_('1');}
function gboPilotInventoryReadonly02(){return gboPilotInventoryReadonlyBatch_('2');}
function gboPilotInventoryReadonly03(){return gboPilotInventoryReadonlyBatch_('3');}
function gboPilotInventoryReadonly04(){return gboPilotInventoryReadonlyBatch_('4');}
function gboPilotInventoryReadonly05(){return gboPilotInventoryReadonlyBatch_('5');}
function gboPilotInventoryReadonly06(){return gboPilotInventoryReadonlyBatch_('6');}
function gboPilotInventoryReadonly07(){return gboPilotInventoryReadonlyBatch_('7');}
function gboPilotInventoryReadonly08(){return gboPilotInventoryReadonlyBatch_('8');}
function gboPilotInventoryReadonly09(){return gboPilotInventoryReadonlyBatch_('9');}
function gboPilotInventoryReadonly10(){return gboPilotInventoryReadonlyBatch_('a');}
function gboPilotInventoryReadonly11(){return gboPilotInventoryReadonlyBatch_('b');}
function gboPilotInventoryReadonly12(){return gboPilotInventoryReadonlyBatch_('c');}
function gboPilotInventoryReadonly13(){return gboPilotInventoryReadonlyBatch_('d');}
function gboPilotInventoryReadonly14(){return gboPilotInventoryReadonlyBatch_('e');}
function gboPilotInventoryReadonly15(){return gboPilotInventoryReadonlyBatch_('f');}

/** A configured wrapper belongs only in a private, explicitly approved editor copy. */
function gboPilotDestinationReadonlyWithConfig_(config){
  const executor=Drive.About.get({fields:'user(emailAddress)'}).user?.emailAddress;
  if(executor!=='reconocimientoinventariovial@gmail.com')throw Error('PREFLIGHT_WRONG_EXECUTOR');
  if(!config||!config.destinationId||!Array.isArray(config.ancestorIds)||!config.ancestorIds.length)throw Error('DESTINATION_NOT_CONFIGURED');
  const ids=[config.destinationId,...config.ancestorIds];if(new Set(ids).size!==ids.length)throw Error('DUPLICATE_DESTINATION_CHAIN');
  const chain=ids.map(id=>{
    const f=Drive.Files.get(id,{fields:'id,name,mimeType,trashed,parents,owners(emailAddress),capabilities(canEdit,canAddChildren)',supportsAllDrives:true});
    let token;const permissions=[];
    do{
      const page=Drive.Permissions.list(id,{pageSize:100,pageToken:token,fields:'nextPageToken,permissions(id,type,role,emailAddress,domain,permissionDetails(inherited,inheritedFrom))',supportsAllDrives:true});
      permissions.push(...(page.permissions||[]));if(token&&page.nextPageToken===token)throw Error('REPEATED_PERMISSION_PAGE');token=page.nextPageToken;
    }while(token);return {...f,permissions};
  });
  const myDrive=Drive.Files.get('root',{fields:'id'});
  const chainMatches=chain.every((f,i)=>f.mimeType==='application/vnd.google-apps.folder'&&f.trashed===false&&f.parents?.length===1&&f.parents[0]===(chain[i+1]?.id||myDrive.id));
  const institutionalOwners=chain.every(f=>f.owners?.length===1&&f.owners[0].emailAddress===executor);
  const publicWriters=chain.flatMap(f=>f.permissions.filter(p=>['anyone','domain'].includes(p.type)&&['writer','organizer','fileOrganizer','owner'].includes(p.role)).map(p=>({folderId:f.id,permissionId:p.id,type:p.type,role:p.role})));
  const canWrite=chain.every(f=>f.capabilities?.canEdit===true&&f.capabilities?.canAddChildren===true);
  const out={readOnly:true,executor,chainMatches,institutionalOwners,canWrite,publicWriters,chain,mutations:0};
  console.log('GBO_READONLY_DESTINATION '+JSON.stringify(out));return out;
}
