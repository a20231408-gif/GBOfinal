/** Local proposal only. No credentials or IDs. Roots keep their own ACKs, bindings and journals. */
var DeliveryRootPolicy=(()=>{
  function reject(code,retryable=false){const e=Error(code);e.code=code;e.retryable=retryable;throw e;}
  function select(p,states,mode,purpose='commit') {
    if(!p||!/^(SIC|SCAP|PAVEMENT)\/[A-Za-z0-9_-]{1,80}$/.test(p.key)||!Number.isSafeInteger(p.revision)||p.revision<1)
      reject('ROOT_ROUTE_INVALID_REQUEST');
    const kind=p.key.split('/')[0],recordId=p.key.split('/')[1];
    if(p.kind!=null&&p.kind!==kind||p.recordId!=null&&p.recordId!==recordId)reject('ROOT_ROUTE_INVALID_REQUEST');
    const identity=p.identityId||(kind==='SCAP'?null:recordId);
    if(identity!=null&&!/^[A-Za-z0-9_-]{1,80}$/.test(identity))reject('ROOT_ROUTE_INVALID_REQUEST');
    if(kind!=='SCAP'&&identity!==recordId)reject('ROOT_IDENTITY_CONFLICT');
    const owners=new Set();
    for(let n=0;n<states.length;n++) {
      const s=states[n];
      for(const r of s.records) {
        if(r.key===p.key) {
          owners.add(n);
          if(identity&&r.identityId&&identity!==r.identityId)reject('ROOT_IDENTITY_CONFLICT');
          if(identity&&!r.identityId&&!(r.revision===p.revision&&r.contentHash===p.contentHash))
            reject('LEGACY_ROOT_IDENTITY_REQUIRES_REVIEW');
        }
        if(identity&&r.identityId===identity)owners.add(n);
        if(p.bridgeCode&&r.bridgeCode===p.bridgeCode&&r.identityId!==identity)reject('BRIDGE_IDENTITY_CONFLICT');
      }
      for(const pin of s.pins) if(pin.key===p.key||identity&&pin.identityId===identity) {
        if(pin.rootId!==s.rootId||pin.version!==1||pin.key===p.key&&identity&&pin.identityId!==identity)
          reject('ROOT_IDENTITY_CONFLICT');
        owners.add(n);
      }
      if(identity&&(s.bindings.some(b=>b.identity===identity)||s.photoIdentities.includes(identity)))owners.add(n);
      if(s.stagedKey||s.journals.some(j=>j.key===p.key))owners.add(n);
    }
    if(owners.size>1)reject('ROOT_IDENTITY_SPLIT_REQUIRES_REVIEW');
    const n=owners.size?[...owners][0]:1,s=states[n];
    // Old clients send only key/revision while staging. A reservation pin or actual history is mandatory.
    if(purpose==='stage'&&!owners.size&&!identity)reject('ROOT_ROUTE_RESERVATION_REQUIRED');
    if(!identity&&!s.pins.some(x=>x.key===p.key)&&!s.records.some(x=>x.key===p.key)&&!s.journals.some(x=>x.key===p.key))
      reject('ROOT_ROUTE_RESERVATION_REQUIRED');
    const pending=s.journals.find(j=>j.key===p.key&&j.status==='PENDING');
    if(pending&&!pending.continuationVersion)reject('LEGACY_RECOVERY_REQUIRES_REVIEW');
    if(mode==='DRAIN_ONLY') {
      const r=s.records.find(x=>x.key===p.key);
      const replay=purpose==='commit'&&r&&r.revision===p.revision&&r.contentHash===p.contentHash&&!pending;
      const resume=pending&&pending.continuationVersion===1&&pending.revision===p.revision&&
        (!p.contentHash||pending.contentHash===p.contentHash)&&purpose!=='reserve';
      const videoParent=purpose==='video'&&r&&r.receipt&&r.receipt.ok===true&&!pending;
      if(!replay&&!resume&&!videoParent)reject('ROOT_ROUTING_DRAIN_ONLY');
    }
    return {index:n,identity:identity||s.pins.find(x=>x.key===p.key)?.identityId||s.records.find(x=>x.key===p.key)?.identityId||null};
  }
  return {select,reject};
})();

function gboRootRoutingEnabled(){
  const p=PropertiesService.getScriptProperties(),enabled=p.getProperty('GBO_ROOT_ROUTING_ENABLED')==='true';
  if(!enabled&&p.getProperty('GBO_ROOT_ROUTING_ACTIVATED'))DeliveryRootPolicy.reject('ROOT_ROUTING_DOWNGRADE_BLOCKED');
  return enabled;
}
function gboRootRoutingConfig() {
  const props=PropertiesService.getScriptProperties();let config;
  try{config=JSON.parse(props.getProperty('GBO_ROOT_ROUTING_CONFIG'));}catch(e){DeliveryRootPolicy.reject('ROOT_ROUTING_NOT_APPROVED');}
  if(!config||config.version!==1||config.oldRootId!==ROOT_FOLDER_ID||!config.newRoot||
     config.newRoot.id===ROOT_FOLDER_ID||!['OPEN','DRAIN_ONLY'].includes(config.mode)||
     config.inventoryReviewed!==true||!/^[a-f0-9]{64}$/.test(config.inventorySha256||''))
    DeliveryRootPolicy.reject('ROOT_ROUTING_NOT_APPROVED');
  gboValidateRootAdmissionConfig(config);
  let oldAncestors;try{oldAncestors=JSON.parse(props.getProperty('GBO_AUTHORIZED_ROOT_ANCESTOR_IDS'));}catch(e){DeliveryRootPolicy.reject('ROOT_ROUTING_NOT_APPROVED');}
  if(!Array.isArray(oldAncestors)||!Array.isArray(config.newRoot.ancestorIds)||oldAncestors.includes(config.newRoot.id)||config.newRoot.ancestorIds.includes(ROOT_FOLDER_ID))
    DeliveryRootPolicy.reject('ROOT_ROUTING_NOT_APPROVED');
  const activated=props.getProperty('GBO_ROOT_ROUTING_ACTIVATED');
  if(activated) {
    let old;try{old=JSON.parse(activated);}catch(e){DeliveryRootPolicy.reject('ROOT_ROUTING_NOT_APPROVED');}
    if(old.oldRootId!==config.oldRootId||old.newRootId!==config.newRoot.id||old.epoch!==config.admission.epoch||old.activatedAt!==config.admission.activatedAt||old.historicalReadOnly!==!!config.oldRootReadOnly)DeliveryRootPolicy.reject('ROOT_ROUTING_DOWNGRADE_BLOCKED');
  }
  return config;
}
function gboRootRoutingReady(){try{const c=gboRootRoutingConfig();gboAssertInstitutionalExecutor(c);gboHistoricalReadRoot(c);gboInstitutionalDestination(c);return gboRootRoutingEnabled();}catch(e){return false;}}
function gboDeliveryBackendReady(){try{return gboRootRoutingEnabled()?gboRootRoutingReady():gboRootReady();}catch(e){return false;}}
function gboAssertRootRoutingAction(p) {
  if(gboRootRoutingEnabled()&&!['reservePhotoNamesV2','stageRecordV3','commitRecordV3','beginVideoV1','finishVideoV1'].includes(p.action))
    DeliveryRootPolicy.reject('ROOT_ROUTE_UNSUPPORTED_ACTION');
}

/** Called under the existing ScriptLock. Read both complete registries BEFORE any write. */
function gboDeliveryRoute(p,purpose) {
  if(!gboRootRoutingEnabled())return {root:gboAuthorizedRoot()};
  const config=gboRootRoutingConfig();gboAssertInstitutionalExecutor(config);
  const oldRoot=gboHistoricalReadRoot(config);
  const newRoot=gboInstitutionalDestination(config);
  const deadline=Date.now()+85000;
  const checkpoint=()=>{if(Date.now()>deadline)DeliveryRootPolicy.reject('ROOT_ROUTING_READ_IN_PROGRESS',true);};
  const hash=x=>Utilities.computeDigest(Utilities.DigestAlgorithm.SHA_256,x).map(b=>(b&255).toString(16).padStart(2,'0')).join('');
  function list(it){const result=[];while(it.hasNext()){checkpoint();result.push(it.next());}return result;}
  function only(it){const xs=list(it);if(xs.length>1)DeliveryRootPolicy.reject('ROOT_REGISTRY_REQUIRES_REVIEW');return xs[0]||null;}
  function snapshot(root) {
    const state={rootId:root.getId(),records:[],bindings:[],pins:[],photoIdentities:[],journals:[],stagedKey:false};
    const registry=only(root.getFoldersByName('._GBO_SYNC_V3'));if(!registry)return state;
    const seenNames=new Set();
    for(const f of list(registry.getFiles())) {
      const name=f.getName();if(!/^(record_|binding_|journal_|root_route_v1_)/.test(name))continue;
      if(seenNames.has(name))DeliveryRootPolicy.reject('ROOT_REGISTRY_REQUIRES_REVIEW');seenNames.add(name);
      let value;try{checkpoint();value=JSON.parse(f.getBlob().getDataAsString());}catch(e){if(e.code)throw e;DeliveryRootPolicy.reject('ROOT_REGISTRY_REQUIRES_REVIEW');}
      if(!value||typeof value!=='object')DeliveryRootPolicy.reject('ROOT_REGISTRY_REQUIRES_REVIEW');
      if(name.startsWith('record_')) {
        if(typeof value.key!=='string'||name!=='record_'+hash(value.key)+'.json')DeliveryRootPolicy.reject('ROOT_REGISTRY_REQUIRES_REVIEW');
        state.records.push(value);
      }else if(name.startsWith('binding_')) {
        if(typeof value.identity!=='string')DeliveryRootPolicy.reject('ROOT_REGISTRY_REQUIRES_REVIEW');state.bindings.push(value);
      }else if(name.startsWith('root_route_v1_')) {
        if(typeof value.key!=='string'||name!=='root_route_v1_'+hash(value.key)+'.json')DeliveryRootPolicy.reject('ROOT_REGISTRY_REQUIRES_REVIEW');state.pins.push(value);
      }else {
        if(!['PENDING','COMPLETE','ROLLED_BACK'].includes(value.status))DeliveryRootPolicy.reject('ROOT_REGISTRY_REQUIRES_REVIEW');
        if(value.key)state.journals.push(value);
        else if(p&&name==='journal_'+hash(p.key)+'.json')state.journals.push({...value,key:p.key});
      }
    }
    if(p) {
      const identity=p.identityId||(p.key?.startsWith('SCAP/')?null:p.key?.split('/')[1]);
      if(identity&&only(registry.getFilesByName('photo_names_v2_'+hash(identity)+'.json')))state.photoIdentities.push(identity);
      const staging=only(registry.getFoldersByName('STAGING'));
      if(staging)state.stagedKey=list(staging.getFolders()).some(f=>f.getName().startsWith(hash(p.key)+'_'));
    }
    return state;
  }
  const roots=[oldRoot,newRoot],states=roots.map(snapshot),selected=DeliveryRootPolicy.select(p,states,config.mode,purpose);
  const known=states.some(s=>s.records.some(r=>r.key===p.key||selected.identity&&r.identityId===selected.identity)||
    s.pins.some(r=>r.key===p.key||selected.identity&&r.identityId===selected.identity)||
    selected.identity&&(s.bindings.some(b=>b.identity===selected.identity)||s.photoIdentities.includes(selected.identity))||s.stagedKey||s.journals.some(j=>j.key===p.key));
  if(selected.index===0&&config.oldRootReadOnly)DeliveryRootPolicy.reject('HISTORICAL_ROOT_READ_ONLY');
  if(selected.index===1&&!known)gboAssertNewRootAdmission(p,config);
  return {root:roots[selected.index],identity:selected.identity,dual:true};
}

/** Durable key-to-root authority for older staging requests that omit the parent UUID. */
function gboPinDeliveryRoot(route,p) {
  if(!route.dual||!route.identity)return;
  gboEnsureRootActivation(route);
  const root=route.root,registry=getOrCreateFolder(root,'._GBO_SYNC_V3');
  const hash=Utilities.computeDigest(Utilities.DigestAlgorithm.SHA_256,p.key).map(x=>(x&255).toString(16).padStart(2,'0')).join('');
  const name='root_route_v1_'+hash+'.json',files=registry.getFilesByName(name);
  const pin={version:1,key:p.key,identityId:route.identity,rootId:root.getId()};
  if(files.hasNext()) {
    const f=files.next();if(files.hasNext())DeliveryRootPolicy.reject('ROOT_REGISTRY_REQUIRES_REVIEW');
    const old=JSON.parse(f.getBlob().getDataAsString());
    if(old.key!==pin.key||old.identityId!==pin.identityId||old.rootId!==pin.rootId||old.version!==1)DeliveryRootPolicy.reject('ROOT_IDENTITY_CONFLICT');
    return;
  }
  registry.createFile(Utilities.newBlob(JSON.stringify(pin),'application/json',name));
}

/** Called under ScriptLock BEFORE the first folder or binding can be created. */
function gboEnsureRootActivation(route) {
  if(!route.dual)return;
  const props=PropertiesService.getScriptProperties(),config=gboRootRoutingConfig();
  if(!props.getProperty('GBO_ROOT_ROUTING_ACTIVATED'))props.setProperty('GBO_ROOT_ROUTING_ACTIVATED',JSON.stringify({oldRootId:config.oldRootId,newRootId:config.newRoot.id,epoch:config.admission.epoch,activatedAt:config.admission.activatedAt,historicalReadOnly:!!config.oldRootReadOnly}));
}

if(typeof module!=='undefined')module.exports=DeliveryRootPolicy;

/** Video can only follow an already acknowledged SCAP parent. It cannot admit a new identity. */
function gboVideoDeliveryRoute(p) {
  const match=/^SCAP\/([A-Za-z0-9_-]{1,80})$/.exec(p?.parentKey||'');
  if(!match||!Number.isSafeInteger(p.parentRevision)||p.parentRevision<1)DeliveryRootPolicy.reject('INVALID_VIDEO');
  const route=gboDeliveryRoute({key:p.parentKey,revision:p.parentRevision},'video');
  const registries=route.root.getFoldersByName('._GBO_SYNC_V3');
  if(!registries.hasNext())DeliveryRootPolicy.reject('VIDEO_PARENT_NOT_SYNCED');
  const registry=registries.next();if(registries.hasNext())DeliveryRootPolicy.reject('ROOT_REGISTRY_REQUIRES_REVIEW');
  const hash=Utilities.computeDigest(Utilities.DigestAlgorithm.SHA_256,p.parentKey).map(x=>(x&255).toString(16).padStart(2,'0')).join('');
  const journals=registry.getFilesByName('journal_'+hash+'.json');
  if(journals.hasNext()){
    const journal=JSON.parse(journals.next().getBlob().getDataAsString());
    if(journals.hasNext())DeliveryRootPolicy.reject('ROOT_REGISTRY_REQUIRES_REVIEW');
    if(journal.status==='PENDING')DeliveryRootPolicy.reject('BUSY',true);
    if(journal.status!=='COMPLETE')DeliveryRootPolicy.reject('VIDEO_PARENT_NOT_SYNCED');
  }
  const files=registry.getFilesByName('record_'+hash+'.json');
  if(!files.hasNext())DeliveryRootPolicy.reject('VIDEO_PARENT_NOT_SYNCED');
  const parent=JSON.parse(files.next().getBlob().getDataAsString());
  if(files.hasNext()||parent.key!==p.parentKey||parent.kind!=='SCAP'||parent.pathVersion!==4||parent.revision<p.parentRevision||!parent.receipt?.ok||parent.receipt.key!==parent.key||parent.receipt.revision!==parent.revision||parent.receipt.contentHash!==parent.contentHash)
    DeliveryRootPolicy.reject('VIDEO_PARENT_NOT_SYNCED');
  return {root:route.root,parent};
}

function gboAssertFolderWithinRoot(id,rootId) {
  const seen=new Set();
  for(let n=0;n<32;n++){
    if(id===rootId)return;
    if(!id||seen.has(id))DeliveryRootPolicy.reject('VIDEO_PARENT_ROOT_MISMATCH');
    seen.add(id);let f;
    try{f=Drive.Files.get(id,{fields:'id,mimeType,trashed,parents',supportsAllDrives:true});}catch(e){DeliveryRootPolicy.reject('VIDEO_PARENT_ROOT_UNVERIFIED',true);}
    if(f.id!==id||f.trashed||f.mimeType!=='application/vnd.google-apps.folder'||f.parents?.length!==1)
      DeliveryRootPolicy.reject('VIDEO_PARENT_ROOT_MISMATCH');
    id=f.parents[0];
  }
  DeliveryRootPolicy.reject('VIDEO_PARENT_ROOT_MISMATCH');
}

/** Read-only gate before the original video adapter can create a folder, journal or upload session. */
function gboVideoStoreContext(p) {
  VideoV1.validate(p);
  const route=gboVideoDeliveryRoute(p);
  gboAssertFolderWithinRoot(route.parent.receipt.folderId,route.root.getId());
  if(gboRootRoutingEnabled()&&gboRootRoutingConfig().mode==='DRAIN_ONLY') {
    const registry=route.root.getFoldersByName('._GBO_SYNC_V3').next();
    const videos=registry.getFoldersByName('VIDEO_V1');
    if(!videos.hasNext())DeliveryRootPolicy.reject('ROOT_ROUTING_DRAIN_ONLY');
    const folder=videos.next();if(videos.hasNext())DeliveryRootPolicy.reject('ROOT_REGISTRY_REQUIRES_REVIEW');
    const hash=Utilities.computeDigest(Utilities.DigestAlgorithm.SHA_256,p.mediaId).map(x=>(x&255).toString(16).padStart(2,'0')).join('');
    const files=folder.getFilesByName('video_'+hash+'.json');
    if(!files.hasNext())DeliveryRootPolicy.reject('ROOT_ROUTING_DRAIN_ONLY');
    const old=JSON.parse(files.next().getBlob().getDataAsString());
    if(files.hasNext()||old.parentKey!==p.parentKey||old.folderId!==route.parent.receipt.folderId)
      DeliveryRootPolicy.reject('VIDEO_IDENTITY_CONFLICT');
  }
  return route;
}
