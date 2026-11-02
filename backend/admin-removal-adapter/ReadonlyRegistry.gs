/** ISOLATED CANDIDATE, NOT ROUTED OR DEPLOYED. Actual read-only Apps Script API adapter.
 * Caller supplies independently verified registry/root IDs. Never calls v3Store/getOrCreateFolder.
 * Executor identity is NOT requester/admin authorization. Output is PRIVATE evidence only.
 */
function gboAdminRegistryReadonly(config) {
  if(!config || !config.executor || !Array.isArray(config.registries) || !config.registries.length)throw Error('EXPLICIT_READ_SCOPE_REQUIRED');
  const executor=Drive.About.get({fields:'user(emailAddress)'}).user?.emailAddress;
  if(executor!==config.executor)throw Error('WRONG_EXECUTOR');
  const safe=id=>typeof id==='string'&&/^[A-Za-z0-9_-]+$/.test(id);
  const results=[];
  for(const scope of config.registries) {
    if(!safe(scope.rootId)||!safe(scope.registryId))throw Error('INVALID_SCOPE_ID');
    const registry=Drive.Files.get(scope.registryId,{fields:'id,name,parents,trashed',supportsAllDrives:true});
    if(registry.trashed || registry.name!=='._GBO_SYNC_V3' || registry.parents?.length!==1 || registry.parents[0]!==scope.rootId)throw Error('REGISTRY_ROOT_MISMATCH');
    function list() {
      const files=[],ids=new Set(),tokens=new Set();let token;
      do {
        const page=Drive.Files.list({q:"'"+scope.registryId+"' in parents and trashed=false",pageSize:100,pageToken:token,fields:'nextPageToken,files(id,name,mimeType,modifiedTime)',orderBy:'name',supportsAllDrives:true,includeItemsFromAllDrives:true});
        for(const file of page.files||[]){if(ids.has(file.id))throw Error('DUPLICATE_PAGE_ITEM');ids.add(file.id);files.push(file);}
        token=page.nextPageToken;
        if(token){if(tokens.has(token))throw Error('REPEATED_PAGE_TOKEN');tokens.add(token);}
      }while(token);
      return files;
    }
    const first=list(),names=new Set(),rows=[];
    for(const f of first) {
      if(names.has(f.name))throw Error('DUPLICATE_REGISTRY_NAME');names.add(f.name);
      if(!/^(record_|journal_|binding_|photo_names_v2_)/.test(f.name))continue;
      try {
        const value=JSON.parse(DriveApp.getFileById(f.id).getBlob().getDataAsString());
        if(!value || typeof value!=='object'||Array.isArray(value))throw Error('INVALID_JSON_OBJECT');
        rows.push({id:f.id,name:f.name,value});
      }catch(e){rows.push({id:f.id,name:f.name,unreadable:true});}
    }
    const second=list();
    const signature=files=>files.map(f=>[f.id,f.name,f.modifiedTime].join('\t')).sort().join('\n');
    const stable=signature(first)===signature(second);
    results.push({scope,entries:first.length,rows,stable,unreadable:rows.filter(r=>r.unreadable).length});
  }
  return {readOnly:true,mutations:0,executorIsNotAdminAuthorization:true,registries:results,
    usableForDeletion:false,reason:'JOURNAL_METADATA_ONLY_NOT_COMPLETE_FILE_PERMISSION_REFERENCE_INVENTORY'};
}
