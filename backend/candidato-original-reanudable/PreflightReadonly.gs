/** Manual preflight in the ORIGINAL project. No Drive/property writes; never called by HTTP. */
function gboPilotInventoryReadonly() {
  const effective=Session.getEffectiveUser().getEmail();
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
      token=page.nextPageToken;
    }while(token);
    return {files,pages};
  }
  const started=new Date().toISOString(),first=listAll();
  const records=new Map(first.files.filter(f=>/^record_[a-f0-9]{64}\.json$/.test(f.name)).map(f=>[f.name,f]));
  const rows=[],states={};
  for(const f of first.files.filter(f=>/^journal_[a-f0-9]{64}\.json$/.test(f.name))){
    try {
      const j=JSON.parse(DriveApp.getFileById(f.id).getBlob().getDataAsString());
      const status=typeof j.status==='string'?j.status:'UNKNOWN';states[status]=(states[status]||0)+1;
      const row={id:f.id,name:f.name,status,continuationVersion:j.continuationVersion||null,operations:Array.isArray(j.operations)?j.operations.length:null,key:j.key||null,revision:j.revision||null};
      if(status!=='COMPLETE'){
        const file=records.get(f.name.replace(/^journal_/,'record_'));
        if(file){const record=JSON.parse(DriveApp.getFileById(file.id).getBlob().getDataAsString());row.key=row.key||record.key||null;row.recordRevision=record.revision||null;row.routeCode=record.routeCode||null;row.recordHasReceipt=!!record.receipt;}
        if(!row.key){
          for(const op of (j.operations||[]).slice(0,3)){
            if(!op.id)continue;
            const description=Drive.Files.get(op.id,{fields:'description',supportsAllDrives:true}).description||'';
            const match=/^gbo-v3-file:((?:SIC|SCAP|PAVEMENT)\/[A-Za-z0-9_-]+):/.exec(description);
            if(match){row.key=match[1];break;}
          }
        }
        row.requiresReview=true;
      }
      rows.push(row);
    }catch(e){states.UNREADABLE=(states.UNREADABLE||0)+1;rows.push({id:f.id,name:f.name,status:'UNREADABLE',requiresReview:true});}
  }
  const second=listAll();
  const journalSignature=a=>a.files.filter(f=>/^(journal_|record_)/.test(f.name)).map(f=>f.id+'\t'+f.name+'\t'+f.modifiedTime).sort().join('\n');
  const report={readOnly:true,started,finished:new Date().toISOString(),executor:effective,rootName:root.name,canEdit:root.capabilities?.canEdit===true,canAddChildren:root.capabilities?.canAddChildren===true,pages:first.pages,verificationPages:second.pages,registryEntries:first.files.length,journals:rows.length,states,stableDuringRead:journalSignature(first)===journalSignature(second),mutations:0};
  console.log('GBO_PILOT_SUMMARY '+JSON.stringify(report));
  for(let i=0;i<rows.length;i+=10)console.log('GBO_PILOT_ROWS '+JSON.stringify({offset:i,rows:rows.slice(i,i+10)}));
  console.log('GBO_PILOT_END '+JSON.stringify({journals:rows.length,rowsEmitted:rows.length,mutations:0}));
  return report;
}
