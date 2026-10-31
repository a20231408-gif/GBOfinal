/** Manual preflight in the ORIGINAL project. No Drive/property writes; never called by HTTP. */
function gboPilotInventoryReadonly() {
  // Drive's existing scope supplies the runtime identity; no extra email OAuth scope.
  const effective=Drive.About.get({fields:'user(emailAddress)'}).user?.emailAddress;
  if(effective!=='reconocimientoinventariovial@gmail.com')throw Error('PREFLIGHT_WRONG_EXECUTOR');
  const root=Drive.Files.get(ROOT_FOLDER_ID,{fields:'id,name,capabilities(canEdit,canAddChildren)',supportsAllDrives:true});
  const folders=DriveApp.getFolderById(ROOT_FOLDER_ID).getFoldersByName('._GBO_SYNC_V3');
  if(!folders.hasNext())throw Error('REGISTRY_NOT_FOUND_NO_FOLDER_CREATED');
  const registry=folders.next();if(folders.hasNext())throw Error('DUPLICATE_REGISTRY');
  function listAll(){
    const files=[];let token, pages=0;const seen=new Set();
    do {
      const page=Drive.Files.list({q:"'"+registry.getId()+"' in parents and trashed=false",pageSize:100,pageToken:token,fields:'nextPageToken,files(id,name,mimeType,modifiedTime)',orderBy:'name',supportsAllDrives:true,includeItemsFromAllDrives:true});
      pages++;for(const f of page.files||[]){if(seen.has(f.id))throw Error('UNSTABLE_PAGINATION_DUPLICATE');seen.add(f.id);files.push(f);}
      if(page.nextPageToken===token&&token)throw Error('UNSTABLE_PAGINATION_REPEATED_TOKEN');
      token=page.nextPageToken;
    }while(token);
    return {files,pages};
  }
  const started=new Date().toISOString(),first=listAll();
  const records=new Map(first.files.filter(f=>/^record_[a-f0-9]{64}\.json$/.test(f.name)).map(f=>[f.name,f]));
  const recordRows=[],bindingRows=[],recordValues=new Map(),duplicateNames=[];
  const names=new Set();for(const f of first.files){if(names.has(f.name))duplicateNames.push(f.name);names.add(f.name);}
  for(const f of records.values()){
    try {
      const r=JSON.parse(DriveApp.getFileById(f.id).getBlob().getDataAsString());
      if(!r||typeof r!=='object'||Array.isArray(r))throw Error('INVALID_RECORD');
      recordValues.set(f.name,r);
      recordRows.push({id:f.id,name:f.name,key:r.key||null,identityId:r.identityId||r.recordId||null,revision:r.revision||null,pathVersion:r.pathVersion||null,hasReceipt:!!r.receipt,receiptRevision:r.receipt?.revision||null,receiptHashMatches:!!r.receipt&&r.receipt.contentHash===r.contentHash,requiresOldRoot:true});
    }catch(e){recordRows.push({id:f.id,name:f.name,unreadable:true,requiresReview:true,requiresOldRoot:true});}
  }
  for(const f of first.files.filter(f=>/^binding_(?:v4_)?[a-f0-9]{64}\.json$/.test(f.name))){
    try {
      const b=JSON.parse(DriveApp.getFileById(f.id).getBlob().getDataAsString());
      if(!b||typeof b!=='object'||Array.isArray(b))throw Error('INVALID_BINDING');
      bindingRows.push({id:f.id,name:f.name,identity:b.identity||null,folderId:b.folderId||null,pathVersion:b.pathVersion||null,scope:b.scope||null,logicalPath:b.logicalPath||null,requiresOldRoot:true});
    }catch(e){bindingRows.push({id:f.id,name:f.name,unreadable:true,requiresReview:true,requiresOldRoot:true});}
  }
  const rows=[],states={};
  for(const f of first.files.filter(f=>/^journal_[a-f0-9]{64}\.json$/.test(f.name))){
    try {
      const j=JSON.parse(DriveApp.getFileById(f.id).getBlob().getDataAsString());
      if(!j||typeof j!=='object'||Array.isArray(j))throw Error('INVALID_JOURNAL');
      const status=typeof j.status==='string'?j.status:'UNKNOWN';
      const row={id:f.id,name:f.name,status,continuationVersion:j.continuationVersion||null,operations:Array.isArray(j.operations)?j.operations.length:null,key:j.key||null,revision:j.revision||null};
      const recordName=f.name.replace(/^journal_/,'record_'),record=recordValues.get(recordName);
      row.recordId=records.get(recordName)?.id||null;
      if(record){row.key=row.key||record.key||null;row.recordRevision=record.revision||null;row.identityId=record.identityId||record.recordId||null;row.recordHasReceipt=!!record.receipt;}
      row.issues=[];
      if(!Array.isArray(j.operations))row.issues.push('INVALID_OPERATIONS');
      if(!['COMPLETE','PENDING','ROLLED_BACK'].includes(status))row.issues.push('UNKNOWN_STATE');
      if(status==='COMPLETE'&&!record)row.issues.push('RECORD_MISSING_OR_UNREADABLE');
      if(status==='COMPLETE'&&record&&(!record.receipt||record.receipt.revision!==record.revision||record.receipt.contentHash!==record.contentHash))row.issues.push('RECEIPT_MISMATCH_OR_MISSING');
      if(status==='COMPLETE'&&record&&j.revision!=null&&j.revision!==record.revision)row.issues.push('JOURNAL_REVISION_MISMATCH');
      if(status!=='COMPLETE'){
        if(!row.key){
          for(const op of (j.operations||[]).slice(0,3)){
            if(!op.id)continue;
            let description='';try {description=Drive.Files.get(op.id,{fields:'description',supportsAllDrives:true}).description||'';}catch(e){row.issues.push('OPERATION_METADATA_UNREADABLE');continue;}
            const match=/^gbo-v3-file:((?:SIC|SCAP|PAVEMENT)\/[A-Za-z0-9_-]+):/.exec(description);
            if(match){row.key=match[1];break;}
          }
        }
        row.requiresReview=true;
      }
      if(row.issues.length)row.requiresReview=true;
      row.requiresOldRoot=status!=='COMPLETE'||!!record;
      states[status]=(states[status]||0)+1;
      rows.push(row);
    }catch(e){states.UNREADABLE=(states.UNREADABLE||0)+1;rows.push({id:f.id,name:f.name,status:'UNREADABLE',requiresReview:true});}
  }
  const second=listAll();
  const journalSignature=a=>a.files.map(f=>f.id+'\t'+f.name+'\t'+f.modifiedTime).sort().join('\n');
  const report={readOnly:true,started,finished:new Date().toISOString(),executor:effective,rootName:root.name,canEdit:root.capabilities?.canEdit===true,canAddChildren:root.capabilities?.canAddChildren===true,pages:first.pages,verificationPages:second.pages,registryEntries:first.files.length,journals:rows.length,states,records:recordRows.length,bindings:bindingRows.length,photoReservations:first.files.filter(f=>f.name.startsWith('photo_names_v2_')).length,duplicateNames,reviewJournals:rows.filter(r=>r.requiresReview).length,unreadableRecords:recordRows.filter(r=>r.unreadable).length,unreadableBindings:bindingRows.filter(r=>r.unreadable).length,stableDuringRead:journalSignature(first)===journalSignature(second),mutations:0};
  console.log('GBO_PILOT_SUMMARY '+JSON.stringify(report));
  for(let i=0;i<rows.length;i+=10)console.log('GBO_PILOT_ROWS '+JSON.stringify({offset:i,rows:rows.slice(i,i+10)}));
  for(let i=0;i<recordRows.length;i+=10)console.log('GBO_PILOT_RECORDS '+JSON.stringify({offset:i,rows:recordRows.slice(i,i+10)}));
  for(let i=0;i<bindingRows.length;i+=10)console.log('GBO_PILOT_BINDINGS '+JSON.stringify({offset:i,rows:bindingRows.slice(i,i+10)}));
  console.log('GBO_PILOT_END '+JSON.stringify({journals:rows.length,rowsEmitted:rows.length,mutations:0}));
  return report;
}

// Render these placeholders only into a PRIVATE editor copy; never commit configured IDs.
var GBO_PREFLIGHT_DESTINATION_ID='PRIVATE_DESTINATION_ID_NOT_SET';
var GBO_PREFLIGHT_DESTINATION_ANCESTORS=[];
function gboPilotDestinationReadonly(){
  const executor=Drive.About.get({fields:'user(emailAddress)'}).user?.emailAddress;
  if(executor!=='reconocimientoinventariovial@gmail.com')throw Error('PREFLIGHT_WRONG_EXECUTOR');
  if(GBO_PREFLIGHT_DESTINATION_ID==='PRIVATE_DESTINATION_ID_NOT_SET'||!GBO_PREFLIGHT_DESTINATION_ANCESTORS.length)throw Error('DESTINATION_NOT_CONFIGURED');
  const options={fields:'id,name,mimeType,trashed,parents,owners(emailAddress),capabilities(canEdit,canAddChildren),permissions(id,type,role,emailAddress,permissionDetails(inherited,inheritedFrom))',supportsAllDrives:true};
  const chain=[GBO_PREFLIGHT_DESTINATION_ID,...GBO_PREFLIGHT_DESTINATION_ANCESTORS].map(id=>Drive.Files.get(id,options));
  const myDrive=Drive.Files.get('root',{fields:'id'});
  const chainMatches=chain.every((f,i)=>f.mimeType==='application/vnd.google-apps.folder'&&f.trashed===false&&f.parents?.length===1&&f.parents[0]===(chain[i+1]?.id||myDrive.id));
  const report={readOnly:true,executor,chainMatches,chain,mutations:0};
  console.log('GBO_DESTINATION_SUMMARY '+JSON.stringify(report));return report;
}
