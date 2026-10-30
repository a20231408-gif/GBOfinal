/** Test envelope and telemetry only. Business ACK fields are left unchanged. */
function gboTestLease_(delta,envelope){
  // The independent web app MUST execute as its owner, also for both clients.
  // UserLock is then shared by these executions, separate from protocol ScriptLock.
  var l=LockService.getUserLock();if(!l.tryLock(10000))gboTestFail_('TEST_GATE_BUSY');
  try{
    gboTestContext_=null;var r=gboTestContextCheck_();
    if(!envelope||envelope.runId!==r.runId||envelope.variant!==r.variant||envelope.epoch!==r.epoch)gboTestFail_('TEST_STALE_RUN');
    var p=gboTestRawProps_(), n=Number(p.getProperty('GBO_TEST_INFLIGHT')||0);
    if(!Number.isSafeInteger(n)||n<0||n+delta<0)gboTestFail_('TEST_LEASE_INVALID');
    p.setProperty('GBO_TEST_INFLIGHT',String(n+delta));return r;
  }finally{l.releaseLock();}
}
function doGet(){
  try{
    gboTestContext_=null;var r=gboTestContextCheck_();
    var cap=JSON.parse(gboV20DoGet_().getContent());
    cap._gboTest={testOnly:true,authorizedRoot:GBO_TEST_AUTHORIZED_ROOT,scriptId:ScriptApp.getScriptId(),run:r};
    return jsonResponse(cap);
  }catch(e){return jsonResponse({ok:false,code:/^TEST_/.test(e.code||'')?e.code:'TEST_NOT_READY',retryable:false});}
}
function doPost(e){
  var entered=false,p=null,ack=null,r=null;
  GboTestMeasure.start('invalid');
  try{
    p=JSON.parse(e.postData.contents);
    // Token is exclusive to this test project. No production property is read.
    if(!p||!API_TOKEN||p.token!==API_TOKEN)gboTestFail_('TEST_UNAUTHORIZED');
    r=gboTestLease_(1,p._test);entered=true;
    var allowed=['commitRecordV3','stageRecordV3','reservePhotoNamesV2','beginVideoV1','finishVideoV1','testSnapshot','testReadFile','testHoldLock','testLockStatus'];
    if(!allowed.includes(p.action))gboTestFail_('TEST_ACTION_DISABLED');
    GboTestMeasure.state.action=p.action;
    if(['commitRecordV3','reservePhotoNamesV2'].includes(p.action)&&p.routeCode!=='TEST_AUTO_BACKEND')gboTestFail_('TEST_FICTITIOUS_ROUTE_REQUIRED');
    if(['commitRecordV3','stageRecordV3'].includes(p.action)&&! /^(SIC|SCAP|PAVEMENT)\/TEST_[A-Za-z0-9_-]+$/.test(p.key||''))gboTestFail_('TEST_FICTITIOUS_UUID_REQUIRED');
    if(['beginVideoV1','finishVideoV1'].includes(p.action)&&!/^SCAP\/TEST_/.test(p.parentKey||''))gboTestFail_('TEST_FICTITIOUS_PARENT_REQUIRED');
    // Reject request-supplied Drive references before the business handler can create anything.
    (function preflight(value){if(!value||typeof value!=='object')return;Object.keys(value).forEach(function(k){
      if(['fileId','stageFileId','folderId','parentId','backupId','archiveId'].includes(k)&&value[k]!=null)gboTestAssertId_(value[k]);
      else if(k!=='base64')preflight(value[k]);
    });})(p);
    var t=Date.now();
    if(p.action==='testSnapshot')ack=gboTestSnapshot_();
    else if(p.action==='testReadFile'){
      var f=GboTestDriveApp.getFileById(p.fileId), b=f.getBlob();
      if(b.getBytes().length>8*1024*1024)gboTestFail_('TEST_DOWNLOAD_TOO_LARGE');
      ack={ok:true,fileId:p.fileId,base64:Utilities.base64Encode(b.getBytes())};
    }else if(p.action==='testHoldLock'){
      var lock=GboTestLocks.getScriptLock();if(!lock.tryLock(1000))gboTestFail_('TEST_HOLD_BUSY');
      try{
        gboTestRawProps_().setProperty('GBO_TEST_LOCK_HELD',JSON.stringify({runId:r.runId,until:Date.now()+35000}));
        Utilities.sleep(35000);ack={ok:true,heldMs:35000};
      }finally{gboTestRawProps_().deleteProperty('GBO_TEST_LOCK_HELD');lock.releaseLock();}
    }else if(p.action==='testLockStatus'){
      var status=JSON.parse(gboTestRawProps_().getProperty('GBO_TEST_LOCK_HELD')||'null');
      ack={ok:true,held:!!status&&status.runId===r.runId&&status.until>Date.now()};
    }else ack=JSON.parse(gboV20DoPost_({postData:{contents:JSON.stringify(p)}}).getContent());
    GboTestMeasure.state.businessMs=Date.now()-t;
  }catch(error){
    // Do not echo raw exceptions, request data, credentials or session URLs.
    ack={ok:false,code:/^[A-Z][A-Z0-9_]{1,90}$/.test(error.code||'')?error.code:'TEST_REQUEST_FAILED',retryable:false};
  }finally{
    if(entered){try{gboTestLease_(-1,p._test);}catch(error){ack={ok:false,code:'TEST_LEASE_RELEASE_FAILED',retryable:false};}}
  }
  var m=GboTestMeasure.state;m.totalServerMs=Date.now()-m.started;delete m.started;
  // This additional test field is removed before comparing the original ACK.
  ack._gboTest={metrics:m,runId:r&&r.runId,variant:GBO_TEST_VARIANT};
  try{console.info(JSON.stringify({event:'GBO_TEST_METRICS',metrics:m,runId:r&&r.runId,variant:GBO_TEST_VARIANT}));}catch(ignored){}
  return jsonResponse(ack);
}
function gboTestSnapshot_(){
  var r=gboTestContextCheck_(), files=[], manifests=[],journals=[];
  function sha(b){return Utilities.computeDigest(Utilities.DigestAlgorithm.SHA_256,b).map(function(x){return(x&255).toString(16).padStart(2,'0');}).join('');}
  function walk(folder,path){
    // Read-only diagnostic: verify each containing folder once, then each file's
    // direct parent. Do not download every photograph or archived staging binary.
    var folderId=folder.getId();gboTestAssertId_(folderId);
    var fs=folder.getFiles();while(fs.hasNext()){
      var f=fs.next(),id=f.getId();
      var meta=GboTestMeasure.operation('diagnostic.metadata.get',function(){return Drive.Files.get(id,{fields:'id,name,size,mimeType,parents,trashed,shortcutDetails,sha256Checksum',supportsAllDrives:true});});
      if(meta.trashed||meta.shortcutDetails||(meta.parents||[]).length!==1||meta.parents[0]!==folderId)gboTestFail_('TEST_SNAPSHOT_REFERENCE_INVALID');
      var name=meta.name,b=null,bytes=null;
      var isRecord=path==='._GBO_SYNC_V3/'&&/^record_[a-f0-9]{64}\.json$/.test(name);
      var isJournal=path==='._GBO_SYNC_V3/'&&/^journal_/.test(name);
      if(isRecord||isJournal||/\.xlsx$/.test(name)||!/^[a-f0-9]{64}$/.test(meta.sha256Checksum||'')){
        b=GboTestMeasure.operation('diagnostic.getBlob',function(){return f.getBlob();});bytes=b.getBytes();
      }
      var entry={path:path+name,id:id,sha256:bytes?sha(bytes):meta.sha256Checksum,sizeBytes:Number(meta.size),mimeType:meta.mimeType};
      if(/\.xlsx$/.test(name)){
        entry.parts=Utilities.unzip(Utilities.newBlob(bytes,'application/zip')).map(function(p){return {name:p.getName(),sha256:sha(p.getBytes())};}).sort(function(a,b){return a.name.localeCompare(b.name);});
      }
      if(isRecord){
        var v=JSON.parse(b.getDataAsString());
        manifests.push({key:v.key,revision:v.revision,contentHash:v.contentHash,kind:v.kind,files:v.files,sicRows:(v.sicRows||[]).map(function(x){return {recordId:x.recordId,format:x.format,sha256:x.sha256,observationsInherited:!!x.observationsTemplateBase64};}),receipt:v.receipt});
      }
      if(isJournal){var j=JSON.parse(b.getDataAsString());journals.push({name:name,status:j.status});}
      // Session URLs and journal contents are never exported.
      files.push(entry);
    }
    var it=folder.getFolders();
    while(it.hasNext()){var child=it.next(),name=child.getName();
      // These are intentional recovery copies, not active logical duplicates.
      if(path==='._GBO_SYNC_V3/'&&['STAGING','REVISIONS'].includes(name))continue;
      walk(child,path+name+'/');
    }
  }
  walk(DriveApp.getFolderById(r.folderId),'');
  return {ok:true,run:r,files:files,manifests:manifests,journals:journals};
}
