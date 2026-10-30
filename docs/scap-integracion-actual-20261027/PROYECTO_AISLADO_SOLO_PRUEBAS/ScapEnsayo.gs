/** EDITOR ONLY. No deployment, no external endpoint and no production configuration. */
function scapEnsayoConfig_(){return {fixture:'SCAP54_FIXTURE_INPUT.json',sha:'e8e1156c9562f37847638ea1c15162739363da8d3686e1fd7eb898bf0749ed46',batch:12};}
function scapRequire_(ok,code){if(!ok)gboTestFail_(code);}
function scapHash_(bytes){return Utilities.computeDigest(Utilities.DigestAlgorithm.SHA_256,bytes).map(function(b){return(b&255).toString(16).padStart(2,'0');}).join('');}
function scapRead_(name){var f=GboTestDriveApp.getFolderById(GBO_TEST_AUTHORIZED_ROOT).getFilesByName(name);scapRequire_(f.hasNext(),'TEST_STATE_MISSING');var file=f.next();scapRequire_(!f.hasNext(),'TEST_STATE_DUPLICATE');return JSON.parse(file.getBlob().getDataAsString());}
function scapWrite_(name,value){
  var l=LockService.getUserLock();scapRequire_(l.tryLock(1000),'TEST_EDITOR_BUSY');
  try{var root=GboTestDriveApp.getFolderById(GBO_TEST_AUTHORIZED_ROOT),it=root.getFilesByName(name),text=JSON.stringify(value);
    if(it.hasNext()){var file=it.next();scapRequire_(!it.hasNext(),'TEST_STATE_DUPLICATE');file.setContent(text);}
    else root.createFile(Utilities.newBlob(text,'application/json',name));
  }finally{l.releaseLock();}
}
function scapState_(){gboTestConfig_();gboTestContextCheck_();return scapRead_('SCAP_ESTADO.json');}
function scapPayload_(state){return scapRead_(state.mode===2?'SCAP_REVISION_2.json':'SCAP_REVISION_1.json');}
function scapNotice_(value){console.info(JSON.stringify(value));return value;}
function scapPost_(payload){var r=gboTestContextCheck_(),p=JSON.parse(JSON.stringify(payload));p.token=gboTestRawProps_().getProperty('GBO_TEST_TOKEN');p._test={runId:r.runId,epoch:r.epoch,variant:r.variant,requestId:Utilities.getUuid()};return JSON.parse(doPost({postData:{contents:JSON.stringify(p)}}).getContent());}
function scapOk_(a){scapRequire_(a&&a.ok===true,'TEST_BACKEND_'+(a&&/^[A-Z0-9_]+$/.test(a.code||'')?a.code:'NO_ACK'));return a;}
function scapCommitPayload_(s){var p=scapPayload_(s);scapRequire_(s.stage.length===p.files.length,'TEST_STAGING_INCOMPLETE');p.files.forEach(function(f,i){f.stageFileId=s.stage[i];delete f.base64;});return p;}
function scapCheckAck_(p,a,previous){
  scapOk_(a);scapRequire_(a.syncProtocol===3&&a.pathVersion===4&&a.key===p.key&&a.revision===p.revision&&a.contentHash===p.contentHash,'TEST_ACK_IDENTITY');
  scapRequire_(a.folderId&&a.logicalPath===p.routeCode+'/PUENTES/PUENTE 1','TEST_ACK_FOLDER');
  scapRequire_(a.technicalDeliveryStatus==='DELIVERED'&&a.georeferencingStatus===p.georeferencing.status&&a.externalGeoreferencingStatus==='PENDING','TEST_ACK_TECHNICAL');
  scapRequire_(Array.isArray(a.files)&&a.files.length===p.files.length&&new Set(a.files.map(function(f){return f.key;})).size===a.files.length&&new Set(a.files.map(function(f){return f.fileId;})).size===a.files.length,'TEST_ACK_FILES');
  p.files.forEach(function(f){var b=a.files.find(function(x){return x.key===f.key;});scapRequire_(b&&b.sha256===f.sha256&&b.requestedPath===f.path&&b.logicalPath===a.logicalPath+'/'+f.path&&b.fileId&&b.folderId,'TEST_ACK_FILE');});
  scapRequire_(a.sicFiles&&a.sicFiles.length===3,'TEST_ACK_SIC');
  p.sicRows.forEach(function(row){var b=a.sicFiles.find(function(x){return x.format===row.format;});scapRequire_(b&&b.fileId&&b.folderId&&/^[a-f0-9]{64}$/.test(b.sha256)&&b.logicalPath===p.routeCode+'/EXCEL_SIC/'+row.format+'.xlsx'&&b.recordIds.length===1&&b.recordIds[0]===p.identityId,'TEST_ACK_SIC_ROW');});
  if(previous){scapRequire_(a.folderId===previous.folderId&&a.logicalPath===previous.logicalPath,'TEST_FOLDER_CHANGED');
    a.files.forEach(function(f){var old=previous.files.find(function(x){return x.key===f.key;});scapRequire_(old&&old.fileId===f.fileId,'TEST_FILE_ID_CHANGED');});
    a.sicFiles.forEach(function(f){var old=previous.sicFiles.find(function(x){return x.format===f.format;});scapRequire_(old&&old.fileId===f.fileId,'TEST_SIC_ID_CHANGED');});
  }
}
function scapPath_(file){var names=[file.getName()],it=file.getParents();scapRequire_(it.hasNext(),'TEST_NO_PARENT');var current=it.next();scapRequire_(!it.hasNext(),'TEST_MULTIPLE_PARENTS');var run=gboTestContextCheck_();
  for(var n=0;n<32;n++){if(current.getId()===run.folderId)return names.join('/');names.unshift(current.getName());it=current.getParents();scapRequire_(it.hasNext(),'TEST_NO_PARENT');current=it.next();scapRequire_(!it.hasNext(),'TEST_MULTIPLE_PARENTS');}
  gboTestFail_('TEST_PATH_TOO_DEEP');
}
function scapVerifySic_(ack){ack.sicFiles.forEach(function(f){var file=GboTestDriveApp.getFileById(f.fileId),bytes=file.getBlob().getBytes();scapRequire_(scapHash_(bytes)===f.sha256&&scapPath_(file)===f.logicalPath,'TEST_SIC_BYTES_OR_PATH');
  var parent=file.getParents().next(),siblings=parent.getFilesByName(file.getName());scapRequire_(parent.getId()===f.folderId&&siblings.hasNext()&&siblings.next().getId()===f.fileId&&!siblings.hasNext(),'TEST_SIC_DUPLICATE_FILE_OR_PARENT');
  var sheet=Utilities.unzip(Utilities.newBlob(bytes)).find(function(b){return b.getName()==='xl/worksheets/sheet1.xml';});scapRequire_(sheet,'TEST_SIC_WORKBOOK');
  var rows=(sheet.getDataAsString().match(/<row\b[^>]*>[\s\S]*?<\/row>/g)||[]).filter(function(x){var m=/\br="(\d+)"/.exec(x);return m&&Number(m[1])>=14;});scapRequire_(rows.length===1,'TEST_SIC_DUPLICATE_ROWS');
});}

function SCAP_00_PREPARAR_PROYECTO(){
  scapRequire_(ROOT_FOLDER_ID===GBO_TEST_AUTHORIZED_ROOT&&GBO_TEST_VARIANT==='SCAP','TEST_ROOT_OR_VARIANT');
  var props=gboTestRawProps_(),all=props.getProperties();
  if(all.GBO_TEST_SCAP_SETUP==='1'){gboTestConfig_();gboTestContextCheck_();return scapNotice_({status:'YA_PREPARADO_SIN_REINICIAR'});}
  scapRequire_(Object.keys(all).length===0,'TEST_PROJECT_NOT_EMPTY');
  var root=DriveApp.getFolderById(GBO_TEST_AUTHORIZED_ROOT);scapRequire_(root.getName()==='GBO_PRUEBAS_BACKEND_NO_PRODUCCION','TEST_ROOT_NAME');
  // A new test-only app credential is generated privately. It is never logged or requested from the owner.
  props.setProperty('GBO_TEST_SCRIPT_ID',ScriptApp.getScriptId());props.setProperty('GBO_TEST_CONFIRM','ONLY_'+GBO_TEST_AUTHORIZED_ROOT);
  props.setProperty('GBO_TEST_TOKEN',Utilities.getUuid().replace(/-/g,'')+Utilities.getUuid().replace(/-/g,''));
  props.setProperty('GBO_TEST_SCAP_SETUP','1');gboTestStartNewRun();
  scapWrite_('SCAP_ESTADO.json',{version:1,mode:1,phase:'CONFIGURADO',stage:[],verify:0,verified1:false,verified2:false,faultInjected:false});
  return scapNotice_({status:'PROYECTO_AISLADO_PREPARADO',next:'SCAP_01_CAPACIDADES'});
}
function SCAP_01_CAPACIDADES(){
  var c=JSON.parse(doGet().getContent()),keys=['ok','syncProtocol','pathVersion','stableFamilyNumbering','photoCategoryVersion','readablePhotoNamingVersion','scapPhotoPanelVersion','videoProtocol','videoScope','readableVideoNamingVersion','technicalDeliveryWithoutGps'],safe={};
  scapRequire_(c.ok&&c.syncProtocol===3&&c.pathVersion===4&&c.stableFamilyNumbering===true&&c.photoCategoryVersion===1&&c.readablePhotoNamingVersion===2&&c.scapPhotoPanelVersion===2,'TEST_CAPABILITIES');
  scapRequire_(c.videoProtocol===1&&c.videoScope==='SCAP_ONLY'&&c.readableVideoNamingVersion===2,'TEST_VIDEO_CAPABILITIES');
  keys.forEach(function(k){safe[k]=c[k];});scapWrite_('CAPACIDADES_SEGURAS.json',safe);return scapNotice_({status:'CAPACIDADES_PASS',scapPhotoPanelVersion:c.scapPhotoPanelVersion});
}
function SCAP_02_CARGAR_Y_RESERVAR(){
  var s=scapState_();if(s.phase!=='CONFIGURADO')return scapNotice_({status:'PREPARACION_EXISTENTE_CONSERVADA',phase:s.phase});
  var config=scapEnsayoConfig_(),root=DriveApp.getFolderById(GBO_TEST_AUTHORIZED_ROOT),it=root.getFilesByName(config.fixture);
  scapRequire_(it.hasNext(),'TEST_SUBIR_FIXTURE_AL_ROOT_AUTORIZADO');var f=it.next();scapRequire_(!it.hasNext(),'TEST_FIXTURE_DUPLICATE');var bytes=f.getBlob().getBytes();scapRequire_(scapHash_(bytes)===config.sha,'TEST_FIXTURE_SHA');
  var p=JSON.parse(f.getBlob().getDataAsString());scapRequire_(p.kind==='SCAP'&&p.routeCode==='TEST_AUTO_BACKEND'&&/^SCAP\/TEST_/.test(p.key)&&p.files.length===59&&p.sicRows.length===3,'TEST_FIXTURE_CONTRACT');
  scapRequire_(p.files.filter(function(x){return x.key.indexOf('photo/')===0;}).length===54&&new Set(p.files.filter(function(x){return x.key.indexOf('photo/')===0;}).map(function(x){return x.photoCategory;})).size===20,'TEST_FIXTURE_PHOTOS');
  p.files.forEach(function(x){scapRequire_(scapHash_(Utilities.base64Decode(x.base64))===x.sha256,'TEST_FIXTURE_FILE_SHA');});
  scapRequire_(scapHash_(SyncV3.canonical(p))===p.contentHash,'TEST_FIXTURE_CANONICAL');
  var q=JSON.parse(JSON.stringify(p));q.action='reservePhotoNamesV2';q.photos=p.files.filter(function(x){return x.key.indexOf('photo/')===0;}).map(function(x){return{id:x.key.slice(6),category:x.photoCategory};});
  var a=scapOk_(scapPost_(q));p.files.filter(function(x){return x.key.indexOf('photo/')===0;}).forEach(function(x){var n=a.photos.find(function(y){return y.id===x.key.slice(6);});scapRequire_(n&&n.name===x.path.split('/').pop(),'TEST_RESERVATION_NAME');});
  scapWrite_('SCAP_REVISION_1.json',p);s.phase='PREPARADO';scapWrite_('SCAP_ESTADO.json',s);return scapNotice_({status:'PREPARADO',files:59,photos:54,sketches:3});
}
function SCAP_03_SUBIR_LOTE(){
  var s=scapState_();scapRequire_(s.phase==='PREPARADO'||s.phase==='STAGING','TEST_WRONG_PHASE');var p=scapPayload_(s),limit=Math.min(p.files.length,s.stage.length+scapEnsayoConfig_().batch),until=Date.now()+120000;
  while(s.stage.length<limit&&Date.now()<until){var index=s.stage.length,f=p.files[index],a=scapOk_(scapPost_({action:'stageRecordV3',syncProtocol:3,key:p.key,revision:p.revision,file:f}));
    scapRequire_(a.key===p.key&&a.revision===p.revision&&a.fileKey===f.key&&a.sha256===f.sha256&&a.stageFileId,'TEST_STAGE_ACK');s.stage.push(a.stageFileId);s.phase='STAGING';scapWrite_('SCAP_ESTADO.json',s);
  }
  if(s.stage.length===p.files.length){s.phase='STAGING_COMPLETO';scapWrite_('SCAP_ESTADO.json',s);}
  return scapNotice_({status:s.phase,revisionTest:s.mode,staged:s.stage.length,total:p.files.length});
}
function SCAP_04_COMMIT_DESCARTAR_RESPUESTA(){
  var s=scapState_();scapRequire_(s.mode===1&&s.phase==='STAGING_COMPLETO','TEST_WRONG_PHASE');var p=scapCommitPayload_(s),a=scapOk_(scapPost_(p));
  scapCheckAck_(p,a); // Deliberately do not persist the client receipt. The server retains its real receipt.
  s.phase='RESPUESTA_DESCARTADA';scapWrite_('SCAP_ESTADO.json',s);return scapNotice_({status:'RESPUESTA_DESCARTADA_INTENCIONALMENTE',networkLossSimulated:true,next:'SCAP_05_REINTENTAR_MISMA_REVISION'});
}
function SCAP_05_REINTENTAR_MISMA_REVISION(){
  var s=scapState_();scapRequire_(s.mode===1&&s.phase==='RESPUESTA_DESCARTADA','TEST_WRONG_PHASE');var p=scapCommitPayload_(s),a=scapOk_(scapPost_(p));
  scapCheckAck_(p,a);scapRequire_(a.alreadyExists===true,'TEST_IDEMPOTENT_RECEIPT_REQUIRED');
  var root=GboTestDriveApp.getFolderById(GBO_TEST_AUTHORIZED_ROOT),registry=root.getFoldersByName('._GBO_SYNC_V3');scapRequire_(registry.hasNext(),'TEST_REGISTRY');
  var records=registry.next().getFilesByName('record_'+scapHash_(p.key)+'.json');scapRequire_(records.hasNext(),'TEST_RECORD_MISSING');var stored=JSON.parse(records.next().getBlob().getDataAsString());scapRequire_(!records.hasNext(),'TEST_DUPLICATE_RECORD');
  scapCheckAck_(p,a,stored.receipt);scapWrite_('ACK_REVISION_1_PRIVADO.json',a);s.phase='VERIFICAR';s.verify=0;scapWrite_('SCAP_ESTADO.json',s);return scapNotice_({status:'ACK_REINTENTO_PASS',alreadyExists:true,files:59});
}
function SCAP_06_PREPARAR_REVISION_2(){
  var s=scapState_();scapRequire_(s.verified1&&s.mode===1,'TEST_VERIFICAR_REVISION_1_PRIMERO');var p=scapPayload_(s);p.baseRevision=p.revision;p.revision++;
  var f=p.files.find(function(x){return x.key==='manifest.json';}),m=JSON.parse(Utilities.newBlob(Utilities.base64Decode(f.base64)).getDataAsString());m.revision=p.revision;m.testOnlyRevisionRecovery=true;
  var bytes=Utilities.newBlob(JSON.stringify(m)).getBytes();f.base64=Utilities.base64Encode(bytes);f.sha256=scapHash_(bytes);p.contentHash=scapHash_(SyncV3.canonical(p));
  scapWrite_('SCAP_REVISION_2.json',p);s.mode=2;s.phase='PREPARADO';s.stage=[];s.verify=0;scapWrite_('SCAP_ESTADO.json',s);return scapNotice_({status:'REVISION_2_PREPARADA_SIN_CAMBIAR_IDENTIDAD',next:'SCAP_03_SUBIR_LOTE'});
}
function SCAP_07_FALLO_Y_RECUPERACION(){
  var s=scapState_();scapRequire_(s.mode===2&&(s.phase==='STAGING_COMPLETO'||s.phase==='RECUPERACION_PENDIENTE'),'TEST_WRONG_PHASE');var p=scapCommitPayload_(s),a;
  if(!s.faultInjected){var nativeUpdate=GboTestDrive.Files.update,count=0,armed=true;
    GboTestDrive.Files.update=function(){count++;if(armed&&count===3){armed=false;var error=Error('TEST_INJECTED_REVISION_FAILURE');error.code='TEST_INJECTED_REVISION_FAILURE';error.retryable=true;throw error;}return nativeUpdate.apply(GboTestDrive.Files,arguments);};
    try{a=scapPost_(p);}finally{GboTestDrive.Files.update=nativeUpdate;}
    scapRequire_(!a.ok&&a.code==='TEST_INJECTED_REVISION_FAILURE','TEST_FAULT_NOT_REPRODUCED');s.faultInjected=true;s.phase='RECUPERACION_PENDIENTE';scapWrite_('SCAP_ESTADO.json',s);
    return scapNotice_({status:'FALLO_CONTROLADO_Y_ROLLBACK',networkFailure:false,next:'Repetir SCAP_07_FALLO_Y_RECUPERACION'});
  }
  a=scapOk_(scapPost_(p));scapCheckAck_(p,a,scapRead_('ACK_REVISION_1_PRIVADO.json'));scapWrite_('ACK_REVISION_2_PRIVADO.json',a);
  var repeat=scapOk_(scapPost_(p));scapCheckAck_(p,repeat,a);scapRequire_(repeat.alreadyExists===true,'TEST_REVISION_2_REPLAY');
  s.phase='VERIFICAR';s.verify=0;scapWrite_('SCAP_ESTADO.json',s);return scapNotice_({status:'REVISION_RECUPERADA_MISMOS_IDS',files:59});
}
function SCAP_08_VERIFICAR_LOTE(){
  var s=scapState_();scapRequire_(s.phase==='VERIFICAR','TEST_WRONG_PHASE');var p=scapPayload_(s),a=scapRead_(s.mode===1?'ACK_REVISION_1_PRIVADO.json':'ACK_REVISION_2_PRIVADO.json'),limit=Math.min(p.files.length,s.verify+scapEnsayoConfig_().batch),until=Date.now()+120000;
  while(s.verify<limit&&Date.now()<until){var expected=p.files[s.verify],ack=a.files.find(function(x){return x.key===expected.key;}),file=GboTestDriveApp.getFileById(ack.fileId);
    scapRequire_(scapHash_(file.getBlob().getBytes())===expected.sha256&&scapPath_(file)===ack.logicalPath,'TEST_GOOGLE_BYTES_OR_PATH');
    var parent=file.getParents().next(),siblings=parent.getFilesByName(file.getName());scapRequire_(parent.getId()===ack.folderId&&siblings.hasNext()&&siblings.next().getId()===ack.fileId&&!siblings.hasNext(),'TEST_DUPLICATE_FILE_OR_PARENT');s.verify++;scapWrite_('SCAP_ESTADO.json',s);
  }
  if(s.verify===p.files.length){scapVerifySic_(a);s.phase='VERIFICADO';if(s.mode===1)s.verified1=true;else s.verified2=true;scapWrite_('SCAP_ESTADO.json',s);}
  return scapNotice_({status:s.phase,revisionTest:s.mode,verified:s.verify,total:59,sicVerified:s.phase==='VERIFICADO'?3:0});
}
function SCAP_09_RESULTADO_SEGURO(){
  var s=scapState_(),inflight=Number(gboTestRawProps_().getProperty('GBO_TEST_INFLIGHT')||0),safe={testOnly:true,status:s.verified1&&s.verified2&&s.faultInjected&&inflight===0?'PASS':'INCOMPLETE',revision1Verified:s.verified1,revision2Verified:s.verified2,controlledFaultInjected:s.faultInjected,photos:54,sketches:3,categories:20,expectedFiles:59,verifiedFilesCurrentRevision:s.verify,sicFormats:['SIC-17','SIC-17A','SIC-17B'],identityAndFileIdsStable:s.verified2===true,lossOfClientReceiptSimulated:true,realNetworkCut:false,inflight:inflight};
  scapWrite_('RESULTADO_SEGURO_PARA_COMPARTIR.json',safe);return scapNotice_(safe);
}
function SCAP_10_DIAGNOSTICO_SOLO_LECTURA(){
  var s=scapState_();return scapNotice_({status:'SOLO_LECTURA',phase:s.phase,revisionTest:s.mode,staged:s.stage.length,verified:s.verify,inflight:Number(gboTestRawProps_().getProperty('GBO_TEST_INFLIGHT')||0),noCountersReset:true});
}
