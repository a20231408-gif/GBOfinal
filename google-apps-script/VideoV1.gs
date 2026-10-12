/** Install alongside VideoV1Core.js as .gs. Disabled until explicitly enabled and acceptance-tested. */
function videoV1Enabled() {return v3Enabled() && PropertiesService.getScriptProperties().getProperty('GBO_VIDEO_V1_ENABLED')==='true';}
function videoV1Action(p) {
  const lock=LockService.getScriptLock();let held=false;
  try {
    if(!videoV1Enabled() || typeof VideoV1==='undefined' || typeof Drive==='undefined')return jsonResponse({ok:false,code:'VIDEO_DISABLED',retryable:false});
    held=lock.tryLock(30000);if(!held)return jsonResponse({ok:false,code:'BUSY',retryable:true});
    const s=videoV1Store();
    return jsonResponse(p.action==='beginVideoV1'?VideoV1.begin(p,s):VideoV1.finish(p,s));
  }catch(e){return jsonResponse({ok:false,code:e.code || 'VIDEO_STORAGE_OR_NETWORK_ERROR',retryable:true});}
  finally {if(held)lock.releaseLock();}
}
function videoV1Store() {
  const root=DriveApp.getFolderById(ROOT_FOLDER_ID),technical=getOrCreateFolder(root,'._GBO_SYNC_V3');
  const registry=getOrCreateFolder(technical,'VIDEO_V1');
  const hash=bytes=>Utilities.computeDigest(Utilities.DigestAlgorithm.SHA_256,bytes).map(x=>(x&255).toString(16).padStart(2,'0')).join('');
  const filename=id=>'video_'+hash(id)+'.json';
  function find(id) {const files=registry.getFilesByName(filename(id));if(!files.hasNext())return null;const f=files.next();if(files.hasNext())VideoV1.reject('DUPLICATE_VIDEO_BINDING');return f;}
  function destination(j) {return getOrCreateFolder(getOrCreateFolder(DriveApp.getFolderById(j.folderId),'VIDEOS'),j.category);}
  const owner=j=>'gbo-video-v1:'+j.parentKey+':'+j.mediaId;
  function metadata(j) {try {return Drive.Files.get(j.fileId,{fields:'id,size,mimeType,parents,description,sha256Checksum',supportsAllDrives:true});}catch(e) {if(/404|not found|File not found/i.test(String(e)))return null;throw e;}}
  return {
    parent:key=>v3Store().get(key),now:()=>Date.now(),
    allocate:()=>Drive.Files.generateIds({count:1,space:'drive'}).ids[0],
    get:id=>{const f=find(id);return f?JSON.parse(f.getBlob().getDataAsString()):null;},
    put:j=>{const f=find(j.mediaId);if(f)f.setContent(JSON.stringify(j));else registry.createFile(Utilities.newBlob(JSON.stringify(j),'application/json',filename(j.mediaId)));},
    ensure:j=>{
      const dest=destination(j),existing=metadata(j);
      if(existing) {if(existing.description!==owner(j) || !existing.parents.includes(dest.getId()))VideoV1.reject('VIDEO_OWNERSHIP_CONFLICT');return;}
      const suffix={'video/mp4':'mp4','video/webm':'webm','video/3gpp':'3gp'}[j.mimeType];
      Drive.Files.create({id:j.fileId,name:'V_'+hash(j.mediaId).slice(0,16)+'.'+suffix,mimeType:j.mimeType,parents:[dest.getId()],description:owner(j),appProperties:{gboMediaId:j.mediaId,gboSha256:j.sha256,gboCategory:j.category,gboCategoryLabel:j.categoryLabel}},null,{fields:'id',supportsAllDrives:true});
    },
    open:j=>{
      const response=UrlFetchApp.fetch('https://www.googleapis.com/upload/drive/v3/files/'+encodeURIComponent(j.fileId)+'?uploadType=resumable&supportsAllDrives=true',{
        method:'patch',contentType:'application/json',payload:'{}',headers:{Authorization:'Bearer '+ScriptApp.getOAuthToken(),'X-Upload-Content-Type':j.mimeType,'X-Upload-Content-Length':String(j.sizeBytes)},muteHttpExceptions:true,followRedirects:false});
      if(response.getResponseCode()!==200)VideoV1.reject('VIDEO_SESSION_START_FAILED');
      const headers=response.getAllHeaders(),url=headers.Location || headers.location;
      if(typeof url!=='string' || !/^https:\/\/www\.googleapis\.com\/upload\/drive\/v3\/files/.test(url))VideoV1.reject('INVALID_VIDEO_SESSION');
      return url;
    },
    verify:j=>{
      const m=metadata(j),dest=destination(j);
      if(!m || m.description!==owner(j) || !m.parents.includes(dest.getId()) || Number(m.size)!==j.sizeBytes)VideoV1.reject('VIDEO_INCOMPLETE');
      // Drive computes the checksum of binary uploads; verify without loading a video blob in Apps Script.
      if(!/^[a-f0-9]{64}$/.test(m.sha256Checksum || ''))VideoV1.reject('VIDEO_CHECKSUM_NOT_AVAILABLE');
      return {sizeBytes:Number(m.size),sha256:m.sha256Checksum,mimeType:m.mimeType};
    }
  };
}
