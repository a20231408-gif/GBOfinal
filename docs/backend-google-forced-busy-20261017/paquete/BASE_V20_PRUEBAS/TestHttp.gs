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
    cap._gboTest={testOnly:true,authorizedRoot:GBO_TEST_AUTHORIZED_ROOT,scriptId:ScriptApp.getScriptId(),run:r,harnessRevision:GBO_TEST_HARNESS_REVISION,guardPolicy:GBO_TEST_GUARD_POLICY,forcedBusyTestVersion:2,forcedBusyHoldMs:90000,inflight:Number(gboTestRawProps_().getProperty('GBO_TEST_INFLIGHT')||0)};
    return jsonResponse(cap);
  }catch(e){return jsonResponse({ok:false,code:/^TEST_/.test(e.code||'')?e.code:'TEST_NOT_READY',retryable:false});}
}
function doPost(e){
  var entered=false,p=null,ack=null,r=null,readLock=null;
  GboTestMeasure.start('invalid');
  try{
    p=JSON.parse(e.postData.contents);
    // Token is exclusive to this test project. No production property is read.
    if(!p||!API_TOKEN||p.token!==API_TOKEN)gboTestFail_('TEST_UNAUTHORIZED');
    if(p.action==='testInspectRecords'){var read=gboTestEnterReadOnly_(p._test);r=read.run;readLock=read.lock;}
    else {r=gboTestLease_(1,p._test);entered=true;}
    var allowed=['commitRecordV3','stageRecordV3','reservePhotoNamesV2','beginVideoV1','finishVideoV1','testSnapshot','testReadFile','testHoldLock','testLockStatus','testInspectRecords'];
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
    if(p.action==='testInspectRecords')ack=gboTestInspectRecords_(p.records,p.expectedOrder);
    else if(p.action==='testSnapshot')ack=gboTestSnapshot_();
    else if(p.action==='testReadFile'){
      var f=GboTestDriveApp.getFileById(p.fileId), b=f.getBlob();
      if(b.getBytes().length>8*1024*1024)gboTestFail_('TEST_DOWNLOAD_TOO_LARGE');
      ack={ok:true,fileId:p.fileId,base64:Utilities.base64Encode(b.getBytes())};
    }else if(p.action==='testHoldLock'){
      if(!/^[a-f0-9-]{36}$/.test(p.probeId||''))gboTestFail_('TEST_PROBE_INVALID');
      var lock=GboTestLocks.getScriptLock();if(!lock.tryLock(1000))gboTestFail_('TEST_HOLD_BUSY');
      try{
        gboTestRawProps_().setProperty('GBO_TEST_LOCK_HELD',JSON.stringify({runId:r.runId,probeId:p.probeId,holdMs:90000,until:Date.now()+90000}));
        var heldAt=Date.now();Utilities.sleep(90000);ack={ok:true,heldMs:Date.now()-heldAt};
      }finally{try{gboTestRawProps_().deleteProperty('GBO_TEST_LOCK_HELD');}finally{lock.releaseLock();}}
    }else if(p.action==='testLockStatus'){
      var status=JSON.parse(gboTestRawProps_().getProperty('GBO_TEST_LOCK_HELD')||'null');
      var matches=!!status&&status.runId===r.runId&&status.probeId===p.probeId;
      ack={ok:true,held:matches&&status.until>Date.now(),probeMatches:matches,remainingMs:matches?Math.max(0,status.until-Date.now()):0,holdMs:matches?status.holdMs:0};
    }else ack=JSON.parse(gboV20DoPost_({postData:{contents:JSON.stringify(p)}}).getContent());
    GboTestMeasure.state.businessMs=Date.now()-t;
  }catch(error){
    // Do not echo raw exceptions, request data, credentials or session URLs.
    ack={ok:false,code:/^[A-Z][A-Z0-9_]{1,90}$/.test(error.code||'')?error.code:'TEST_REQUEST_FAILED',retryable:false};
  }finally{
    if(entered){try{gboTestLease_(-1,p._test);}catch(error){ack={ok:false,code:'TEST_LEASE_RELEASE_FAILED',retryable:false};}}
    if(readLock){gboTestGuard_.readOnly=false;gboTestInvalidateReads_();readLock.releaseLock();}
  }
  var m=GboTestMeasure.state;m.totalServerMs=Date.now()-m.started;delete m.started;
  // This additional test field is removed before comparing the original ACK.
  ack._gboTest={forcedBusyTestVersion:2,metrics:m,runId:r&&r.runId,variant:GBO_TEST_VARIANT,harnessRevision:GBO_TEST_HARNESS_REVISION,guardPolicy:GBO_TEST_GUARD_POLICY,requestId:p&&p._test&&p._test.requestId||null};
  try{console.info(JSON.stringify({event:'GBO_TEST_METRICS',metrics:m,runFingerprint:r?gboTestFingerprint_(r.runId):null,requestFingerprint:p&&p._test&&p._test.requestId?gboTestFingerprint_(p._test.requestId):null,harnessRevision:GBO_TEST_HARNESS_REVISION,guardPolicy:GBO_TEST_GUARD_POLICY}));}catch(ignored){}
  return jsonResponse(ack);
}
function gboTestFingerprint_(text){return Utilities.computeDigest(Utilities.DigestAlgorithm.SHA_256,String(text)).map(function(x){return(x&255).toString(16).padStart(2,'0');}).join('').slice(0,12);}
function gboTestEnterReadOnly_(envelope){
  var l=LockService.getUserLock();if(!l.tryLock(10000))gboTestFail_('TEST_GATE_BUSY');
  try{
    gboTestContext_=null;var r=gboTestContextCheck_();
    if(!envelope||envelope.runId!==r.runId||envelope.epoch!==r.epoch||envelope.variant!==r.variant)gboTestFail_('TEST_STALE_RUN');
    if(Number(gboTestRawProps_().getProperty('GBO_TEST_INFLIGHT')||0)!==0)gboTestFail_('TEST_DIAGNOSTIC_INFLIGHT');
    gboTestGuard_.readOnly=true;gboTestInvalidateReads_();return {lock:l,run:r};
  }catch(e){l.releaseLock();throw e;}
}
/** Reads committed receipts and journals directly. Never calls v3Store, recovery,
 * reservation, stage or commit; never increments/resets inflight. */
function gboTestInspectRecords_(expected,order){
  if(!Array.isArray(expected)||!expected.length||expected.length>128)gboTestFail_('TEST_INSPECTION_INVALID');
  var r=gboTestContextCheck_(),root=GboTestDriveApp.getFolderById(GBO_TEST_AUTHORIZED_ROOT),dirs=root.getFoldersByName('._GBO_SYNC_V3');
  if(!dirs.hasNext())gboTestFail_('TEST_REGISTRY_MISSING');var registry=dirs.next();if(dirs.hasNext())gboTestFail_('TEST_REGISTRY_AMBIGUOUS');
  var results=[],receipts=[],recordNames=[],journals=[],it=registry.getFiles();
  while(it.hasNext()){var n=it.next().getName();if(/^record_[a-f0-9]{64}\.json$/.test(n))recordNames.push(n);if(/^journal_[a-f0-9]{64}\.json$/.test(n))journals.push(n);}
  function digest(s){return Utilities.computeDigest(Utilities.DigestAlgorithm.SHA_256,s).map(function(x){return(x&255).toString(16).padStart(2,'0');}).join('');}
  function read(name){var a=registry.getFilesByName(name);if(!a.hasNext())return null;var f=a.next();if(a.hasNext())gboTestFail_('TEST_INSPECTION_AMBIGUOUS');return JSON.parse(f.getBlob().getDataAsString());}
  expected.forEach(function(e,index){
    if(!/^(SIC|SCAP|PAVEMENT)\/TEST_[A-Za-z0-9_-]+$/.test(e.key||''))gboTestFail_('TEST_FICTITIOUS_UUID_REQUIRED');
    var id=digest(e.key),m=read('record_'+id+'.json'),j=read('journal_'+id+'.json');
    var checks={exists:!!m,journalComplete:!!j&&j.status==='COMPLETE',identityMatches:!!m&&m.key===e.key&&m.receipt&&m.receipt.key===e.key,
      revisionMatches:!!m&&m.revision===e.revision&&m.receipt&&m.receipt.revision===e.revision,
      contentHashMatches:!!m&&m.contentHash===e.contentHash&&m.receipt&&m.receipt.contentHash===e.contentHash,
      folderMatches:!!m&&m.receipt&&m.receipt.folderId===e.folderId,storedAckContract:!!m&&m.receipt&&m.receipt.ok===true&&m.receipt.syncProtocol===3&&m.receipt.pathVersion===4&&m.receipt.technicalDeliveryStatus==='DELIVERED'&&typeof m.receipt.alreadyExists==='boolean',filesMatch:false,receiptFilesMatch:false,liveHashesMatch:false,rowsMatch:false,stagingOwnershipMatch:false};
    if(m&&m.receipt){
      gboTestAssertId_(m.receipt.folderId);
      function sameFiles(actual){return Array.isArray(actual)&&actual.length===e.files.length&&new Set(actual.map(function(f){return f.key;})).size===actual.length&&e.files.every(function(f){var a=actual.find(function(x){return x.key===f.key;});return a&&a.fileId===f.fileId&&a.sha256===f.sha256&&typeof f.folderId==='string'&&a.folderId===f.folderId&&a.logicalPath===f.logicalPath;});}
      var files=m.files||[];checks.filesMatch=sameFiles(files);checks.receiptFilesMatch=sameFiles(m.receipt.files);
      checks.liveHashesMatch=files.every(function(f){gboTestAssertId_(f.fileId);var v=Drive.Files.get(f.fileId,{fields:'id,sha256Checksum',supportsAllDrives:true});return v.sha256Checksum===f.sha256;});
      checks.stagingOwnershipMatch=(e.stages||[]).every(function(f){
        var staged=GboTestDriveApp.getFileById(f.stageFileId),owner;
        try{owner=JSON.parse(staged.getDescription());}catch(error){return false;}
        var meta=Drive.Files.get(f.stageFileId,{fields:'id,name,parents,sha256Checksum',supportsAllDrives:true});
        if(owner.key!==e.key||owner.revision!==e.revision||owner.fileKey!==f.key||owner.sha256!==f.sha256||meta.sha256Checksum!==f.sha256||meta.name!==digest(f.key)+'_'+f.sha256+'.bin'||(meta.parents||[]).length!==1)return false;
        var stage=Drive.Files.get(meta.parents[0],{fields:'id,name,parents',supportsAllDrives:true});if(stage.name!==digest(e.key)+'_'+e.revision||(stage.parents||[]).length!==1)return false;
        var area=Drive.Files.get(stage.parents[0],{fields:'id,name,parents',supportsAllDrives:true});return area.name==='STAGING'&&(area.parents||[]).length===1&&area.parents[0]===registry.getId();
      });
      var rows=m.sicRows||[];checks.rowsMatch=rows.length===e.rows.length&&e.rows.every(function(x){return rows.some(function(y){return y.recordId===x.recordId&&y.format===x.format&&y.sha256===x.sha256;});});
      receipts=receipts.concat(m.receipt.sicFiles||[]);
    }
    results.push({ordinal:index+1,family:e.key.split('/')[0],recordFingerprint:gboTestFingerprint_(e.key),checks:checks,storedAckAlreadyExists:m&&m.receipt&&typeof m.receipt.alreadyExists==='boolean'?m.receipt.alreadyExists:null});
  });
  var winner=receipts.filter(function(x){return x.format==='SIC-18';}).sort(function(a,b){return b.recordIds.length-a.recordIds.length;})[0];
  var orderMatches=Array.isArray(order)&&!!winner&&order.every(function(x,i){return winner.recordIds[i]===x;});
  var safe=recordNames.length===expected.length&&journals.length===expected.length&&orderMatches&&results.every(function(x){return Object.keys(x.checks).every(function(k){return x.checks[k]===true;});});
  return {ok:true,readOnly:true,inflight:0,harnessRevision:GBO_TEST_HARNESS_REVISION,guardPolicy:GBO_TEST_GUARD_POLICY,recordCount:recordNames.length,journalCount:journals.length,orderMatches:orderMatches,safeToResume:safe,results:results};
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
