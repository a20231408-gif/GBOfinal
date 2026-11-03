// Synthetic in-memory roots only. No HTTP, credentials, Drive accounts or field records.
const {test}=require('node:test'),assert=require('node:assert/strict'),fs=require('node:fs'),path=require('node:path'),vm=require('node:vm');
const {env,ROOT,sha,zip}=require('../../../docs/scap-integracion-actual-20261027/pruebas/MockActual.cjs');
const policy=require('../DeliveryRootRouting.gs');
const clone=x=>JSON.parse(JSON.stringify(x));
const empty=id=>({rootId:id,records:[],bindings:[],pins:[],photoIdentities:[],journals:[],stagedKey:false});
const request=()=>({syncProtocol:3,pathVersion:4,kind:'SCAP',key:'SCAP/FICTITIOUS_INSPECTION',recordId:'FICTITIOUS_INSPECTION',identityId:'FICTITIOUS_PARENT',
 revision:1,baseRevision:0,rootRoutingVersion:2,rootEpoch:'e'.repeat(32),captureCreatedAt:Date.now(),routeCode:'PE-12A',family:'PUENTE',familyPath:'PUENTES',bridgeCode:'FICTITIOUS_BRIDGE',orderAxis:12000,roadbedCode:'CD',direction:'ASC',startPrNumber:12,startDistanceM:0,
 files:[{key:'metadata',path:'SCAP/FICTITIOUS_INSPECTION/metadata.json',mimeType:'application/json',base64:Buffer.from('{}').toString('base64'),sha256:sha('{}')}],sicRows:[]});
function states(){return [empty('MEMORY_OLD'),empty('MEMORY_NEW')];}
test('policy: new identity selects new root, shared historical SIC/SCAP parent selects old root',()=>{
 const p=request(),s=states();assert.equal(policy.select(p,s,'OPEN').index,1);
 s[0].bindings.push({identity:p.identityId});assert.equal(policy.select(p,s,'OPEN').index,0);
});
test('policy: duplicate key/parent in both roots blocks even if ACKs look equal',()=>{
 const p=request(),s=states();for(const r of s)r.records.push({...p,receipt:{ok:true}});
 assert.throws(()=>policy.select(p,s,'OPEN'),e=>e.code==='ROOT_IDENTITY_SPLIT_REQUIRES_REVIEW');
});
test('policy: global bridge-code collision is never bypassed by changing root, route or labels',()=>{
 const p=request(),s=states();s[0].records.push({key:'SIC/OTHER',identityId:'OTHER',bridgeCode:p.bridgeCode});
 assert.throws(()=>policy.select(p,s,'OPEN'),e=>e.code==='BRIDGE_IDENTITY_CONFLICT');
});
test('policy: legacy missing UUID is not adopted; only exact acknowledged replay can reach validation',()=>{
 const p=request(),s=states();s[0].records.push({key:p.key,revision:1,contentHash:'f'.repeat(64)});
 assert.throws(()=>policy.select(p,s,'OPEN'),e=>e.code==='LEGACY_ROOT_IDENTITY_REQUIRES_REVIEW');
 p.contentHash='f'.repeat(64);assert.equal(policy.select(p,s,'OPEN').index,0);
});
test('policy: old stage envelope requires historical authority; never guesses a new SCAP parent',()=>{
 const p={key:request().key,revision:1},s=states();
 assert.throws(()=>policy.select(p,s,'OPEN','stage'),e=>e.code==='ROOT_ROUTE_RESERVATION_REQUIRED');
 s[0].pins.push({version:1,key:p.key,identityId:'FICTITIOUS_PARENT',rootId:s[0].rootId});
 assert.equal(policy.select(p,s,'OPEN','stage').index,0);
});
test('policy: unrelated legacy pending does not reroute a new identity, affected legacy pending needs review',()=>{
 const p=request(),s=states();s[0].journals.push({key:'SCAP/OTHER',status:'PENDING'});
 assert.equal(policy.select(p,s,'OPEN').index,1);s[0].journals[0].key=p.key;
 assert.throws(()=>policy.select(p,s,'OPEN'),e=>e.code==='LEGACY_RECOVERY_REQUIRES_REVIEW');
});

function setup(){
 const e=env('INTEGRADO'),guard=require('./RootGuardMock.cjs').rootGuardMock(e);
 e.props.GBO_RESUMABLE_ROOT_ID=ROOT;
 const next=new e.root.constructor('FICTITIOUS_DESTINATION',null,'MEMORY_NEW_ROOT');
 guard.additional[next.id]={id:next.id,name:next.name,mimeType:'application/vnd.google-apps.folder',trashed:false,
   parents:[guard.parent.id],capabilities:{canEdit:true,canAddChildren:true}};
 for(const f of [guard.parent,guard.additional[next.id]])Object.assign(f,{owners:[{emailAddress:'institutional@example.invalid'}],permissions:[{type:'user',role:'owner'}]});
 const config={version:1,oldRootId:ROOT,newRoot:{id:next.id,parentId:guard.parent.id,name:next.name,parentName:guard.parent.name,ancestorIds:[guard.parent.id]},
   inventoryReviewed:true,inventorySha256:'a'.repeat(64),mode:'OPEN',admission:{version:1,epoch:'e'.repeat(32),activatedAt:1,executorEmail:'institutional@example.invalid'}};
 e.context.Drive.About={get:()=>({user:{emailAddress:'institutional@example.invalid'}})};
 let seq=0;e.context.Drive.Files.generateIds=({count=1}={})=>({ids:Array.from({length:count},()=> 'FICTITIOUS_ALLOCATED_'+(++seq))});
 e.context.Drive.Files.update=(m,id,b,options={})=>{const f=e.objects.get(id);assert.ok(f);if(b)f.b=b;if(m.name!==undefined)f.setName(m.name);if(m.description!==undefined)f.setDescription(m.description);
   if(options.addParents&&f.parent.id!==options.addParents)f.moveTo(e.objects.get(options.addParents));return {id};};
 for(const n of ['AuthorizedRootGuard','DeliveryRootRouting','NewRootAdmission','PublicDeliveryNames','PhotoNamesV2Core','SyncV3Core','SyncV3','VideoNamesV2Core','VideoV1Core'])
   vm.runInContext(fs.readFileSync(path.join(__dirname, ['VideoNamesV2Core','VideoV1Core'].includes(n)?'../../../google-apps-script/'+n+'.js':'../'+n+'.gs'),'utf8'),e.context,{filename:n+'.gs'});
 const plain=x=>JSON.parse(x.getContent());
 function configure(){e.props.GBO_ROOT_ROUTING_CONFIG=JSON.stringify(config);e.props.GBO_ROOT_ROUTING_ENABLED='true';}
 function reserve(p){return plain(e.context.reservePhotoNamesV2({...clone(p),photoNamingVersion:2,photos:[]}));}
 function stage(p,oldClient=false){const q=oldClient?{key:p.key,revision:p.revision,syncProtocol:3}:clone(p);
   const a=plain(e.context.stageRecordV3({...q,file:clone(p.files[0])}));assert.equal(a.ok,true,JSON.stringify(a));return a;}
 function prepare(p,oldClient=false){const q=clone(p),a=stage(q,oldClient);delete q.files[0].base64;q.files[0].stageFileId=a.stageFileId;
   q.contentHash=sha(e.context.SyncV3.canonical(q));return q;}
 const commit=p=>plain(e.context.commitRecordV3(clone(p)));
 function rootOf(id){let f=e.objects.get(id);assert.ok(f);while(f.parent)f=f.parent;return f.id;}
 function journal(root,p){const it=root.getFoldersByName('._GBO_SYNC_V3');if(!it.hasNext())return null;const files=it.next().getFilesByName('journal_'+sha(p.key)+'.json');return files.hasNext()?JSON.parse(files.next().getBlob().getDataAsString()):null;}
 return {e,next,config,configure,reserve,stage,prepare,commit,rootOf,journal,guard};
}
function strict(s,a,p,root){assert.equal(a.ok,true,JSON.stringify(a));assert.equal(a.key,p.key);assert.equal(a.revision,p.revision);assert.equal(a.contentHash,p.contentHash);
 assert.equal(s.rootOf(a.folderId),root);assert.equal(a.files.length,1);const f=a.files[0];assert.equal(s.rootOf(f.fileId),root);
 assert.equal(f.sha256,p.files[0].sha256);assert.equal(sha(Buffer.from(s.e.objects.get(f.fileId).getBlob().getBytes())),f.sha256);
 assert.equal(s.journal(root===ROOT?s.e.root:s.next,p).status,'COMPLETE');}
test('adapter: new UUID uses new root, reservation pins old-client stages, exact lost ACK replay duplicates nothing',()=>{
 const s=setup();s.configure();const p=request();assert.equal(s.reserve(p).ok,true);
 const q=s.prepare(p,true),ack=s.commit(q);strict(s,ack,q,s.next.id);
 const before=s.e.objects.size,replay=s.commit(q);assert.equal(replay.alreadyExists,true);assert.deepEqual(replay.files,ack.files);assert.equal(s.e.objects.size,before);
 assert.equal(s.e.root.getFoldersByName('._GBO_SYNC_V3').hasNext(),false);
});
test('adapter: historical ACK, its edition and shared bridge inspection stay in old root with original IDs',()=>{
 const s=setup(),p=request(),q=s.prepare(p),first=s.commit(q);strict(s,first,q,ROOT);
 s.configure();assert.deepEqual(s.commit(q).files,first.files);
 const edit={...p,revision:2,baseRevision:1},edited=s.prepare(edit,true),ack=s.commit(edited);strict(s,ack,edited,ROOT);assert.deepEqual(ack.files.map(x=>x.fileId),first.files.map(x=>x.fileId));
 const companion={...request(),key:'SCAP/FICTITIOUS_SECOND_INSPECTION',recordId:'FICTITIOUS_SECOND_INSPECTION'};
 assert.equal(s.reserve(companion).ok,true);assert.equal(s.rootOf(s.reserve(companion).folderId),ROOT);
 assert.equal(s.next.getFoldersByName('._GBO_SYNC_V3').hasNext(),false);
});
test('adapter: legacy pending is preserved without writes; it never moves to a blank destination',()=>{
 const s=setup(),p=request();s.prepare(p);const registry=s.e.root.getFoldersByName('._GBO_SYNC_V3').next();
 const frozen=JSON.stringify({name:'journal_'+sha(p.key)+'.json',key:p.key,status:'PENDING',revision:1,operations:[{id:'FICTITIOUS_KEEP'}]});
 const file=registry.createFile(s.e.blob(frozen,'application/json','journal_'+sha(p.key)+'.json'));s.configure();
 const writes=s.e.writes.length;assert.equal(s.commit(p).code,'LEGACY_RECOVERY_REQUIRES_REVIEW');assert.equal(s.e.writes.length,writes);assert.equal(file.getBlob().getDataAsString(),frozen);
});
test('adapter: interruption after actual creation resumes same pending journal and ID, including in DRAIN_ONLY',()=>{
 const s=setup();s.configure();const p=request();assert.equal(s.reserve(p).ok,true);const q=s.prepare(p);
 const create=s.e.context.Drive.Files.create;let id;
 s.e.context.Drive.Files.create=(m,b)=>{const a=create(m,b);if(!id&&m.description?.startsWith('gbo-v3-file:')){id=a.id;throw Error('FICTITIOUS_LOST_CREATE_RESPONSE');}return a;};
 assert.equal(s.commit(q).ok,false);assert.equal(s.journal(s.next,q).status,'PENDING');
 s.config.mode='DRAIN_ONLY';s.configure();const ack=s.commit(q);strict(s,ack,q,s.next.id);assert.equal(ack.files[0].fileId,id);
 assert.equal(s.commit({...q,baseRevision:1,revision:2}).code,'ROOT_ROUTING_DRAIN_ONLY');
 assert.deepEqual(s.commit(q).files,ack.files);assert.equal(s.reserve({...p,key:'SCAP/FICTITIOUS_OTHER',recordId:'FICTITIOUS_OTHER'}).code,'ROOT_ROUTING_DRAIN_ONLY');
});
test('adapter: disabling or substituting roots after activation fails closed; cannot simulate rollback by changing ROOT',()=>{
 const s=setup();s.configure();assert.equal(s.reserve(request()).ok,true);const writes=s.e.writes.length;
 s.e.props.GBO_ROOT_ROUTING_ENABLED='false';assert.equal(s.commit(request()).code,'ROOT_ROUTING_DOWNGRADE_BLOCKED');
 s.e.props.GBO_ROOT_ROUTING_ENABLED='true';s.config.oldRootId='FICTITIOUS_SUBSTITUTED';s.configure();
 assert.equal(s.commit(request()).code,'ROOT_ROUTING_NOT_APPROVED');assert.equal(s.e.writes.length,writes);
});
test('adapter: incorrect destination permission, unreadable registry and unapproved inventory all reject before writes',()=>{
 for(const mutate of [s=>s.guard.additional[s.next.id].capabilities.canEdit=false,s=>s.config.inventoryReviewed=false,
   s=>s.e.root.createFolder('._GBO_SYNC_V3').createFile(s.e.blob('{bad','application/json','record_broken.json'))]){
  const s=setup();mutate(s);s.configure();const before=s.e.writes.length;assert.equal(s.commit(request()).ok,false);assert.equal(s.e.writes.length,before);
 }
});
test('adapter: stage from another root or altered bytes cannot yield a valid ACK',()=>{
 const s=setup(),p=request(),old=s.prepare(p);s.configure();const newP={...request(),key:'SCAP/FICTITIOUS_NEW',recordId:'FICTITIOUS_NEW',identityId:'FICTITIOUS_OTHER_PARENT',bridgeCode:'FICTITIOUS_NEW_CODE'};
 const q={...newP,files:[{...newP.files[0],stageFileId:old.files[0].stageFileId}]};delete q.files[0].base64;q.contentHash=sha(s.e.context.SyncV3.canonical(q));
 const before=s.e.writes.length;assert.equal(s.commit(q).code,'STAGE_OWNERSHIP_CONFLICT');
 assert.ok(s.e.writes.length<=before+1); // An empty technical registry may be allocated; no public transaction/ACK.
 const prepared=s.prepare(newP);s.e.objects.get(prepared.files[0].stageFileId).b=s.e.blob('CHANGED','application/json','metadata.json');
 assert.equal(s.commit(prepared).code,'HASH_MISMATCH');assert.equal(s.journal(s.next,prepared),null);
});
test('adapter: authenticated legacy actions are explicitly blocked in dual-root mode',()=>{
 const s=setup();s.configure();const before=s.e.writes.length;
 for(const action of ['uploadScap','uploadDeliveryFile'])
  assert.throws(()=>s.e.context.gboAssertRootRoutingAction({action}),e=>e.code==='ROOT_ROUTE_UNSUPPORTED_ACTION');
 assert.equal(s.e.writes.length,before);
});
test('adapter: complete iteration finds a historical identity beyond 100 entries without writes',()=>{
 const s=setup(),p=request(),registry=s.e.root.createFolder('._GBO_SYNC_V3');
 for(let n=0;n<135;n++) {
  const key=n===134?p.key:'SIC/FICTITIOUS_'+n;
  registry.createFile(s.e.blob(JSON.stringify({key,identityId:n===134?p.identityId:'FICTITIOUS_'+n,revision:1}),
   'application/json','record_'+sha(key)+'.json'));
 }
 s.configure();const before=s.e.writes.length;assert.equal(s.e.context.gboDeliveryRoute(p,'commit').root.getId(),ROOT);assert.equal(s.e.writes.length,before);
});
test('adapter: lost record response cannot expose premature ACK across root routing',()=>{
 const s=setup();s.configure();const p=request();s.reserve(p);const q=s.prepare(p),create=s.e.context.Drive.Files.create;let lost=false;
 s.e.context.Drive.Files.create=(m,b)=>{const a=create(m,b);if(!lost&&m.name.startsWith('record_')){lost=true;throw Error('FICTITIOUS_LOST_RECORD_RESPONSE');}return a;};
 assert.equal(s.commit(q).ok,false);assert.equal(s.journal(s.next,q).status,'PENDING');
 assert.equal(s.e.context.v3Store(q).get(q.key),null);const ack=s.commit(q);strict(s,ack,q,s.next.id);
});
test('adapter: descriptive V2 photo reservation and binding remain immutable after edit in the new root',()=>{
 const s=setup();s.configure();s.e.props.GBO_PUBLIC_NAMES_ENABLED='true';
 const p={...request(),photoNamingVersion:2,publicNamingVersion:1,publicDisplayLabel:'PUENTE - PE-12A - PR 12+000 - FICTICIO'};
 const photo={id:'FICTITIOUS_PHOTO',category:'GENERAL'};
 const reserve=JSON.parse(s.e.context.reservePhotoNamesV2({...p,photos:[photo]}).getContent());assert.equal(reserve.ok,true);
 p.publicElementFolder=reserve.publicElementFolder;p.publicDisplayLabel=reserve.publicDisplayLabel;
 const bytes=Buffer.from([255,216,1,255,217]);p.files.push({key:'photo/'+photo.id,path:'SCAP/'+p.recordId+'/GENERAL/'+reserve.photos[0].name,
  photoCategory:'GENERAL',mimeType:'image/jpeg',base64:bytes.toString('base64'),sha256:sha(bytes)});
 const q=clone(p);for(const f of q.files){const staged=JSON.parse(s.e.context.stageRecordV3({...p,file:f}).getContent());assert.equal(staged.ok,true);f.stageFileId=staged.stageFileId;delete f.base64;}
 q.contentHash=sha(s.e.context.SyncV3.canonical(q));const ack=s.commit(q);assert.equal(ack.ok,true,JSON.stringify(ack));assert.equal(ack.files.length,2);
 assert.equal(s.rootOf(ack.folderId),s.next.id);assert.equal(ack.files.find(x=>x.key===p.files[1].key).requestedPath,p.files[1].path);
 const repeat=JSON.parse(s.e.context.reservePhotoNamesV2({...p,revision:2,baseRevision:1,publicDisplayLabel:'ETIQUETA CAMBIADA',photos:[photo]}).getContent());
 assert.equal(repeat.ok,true);assert.equal(repeat.photos[0].name,reserve.photos[0].name);assert.equal(repeat.folderId,reserve.folderId);assert.equal(repeat.publicDisplayLabel,reserve.publicDisplayLabel);
});


test('admission: old offline, missing metadata, future clock, foreign epoch and base revision reject before writes',()=>{
 for(const change of [p=>delete p.captureCreatedAt,p=>p.captureCreatedAt=1,p=>p.captureCreatedAt=Date.now()+600000,
  p=>delete p.rootRoutingVersion,p=>p.rootEpoch='d'.repeat(32),p=>p.baseRevision=1]) {
  const s=setup();s.config.admission.activatedAt=Date.now()-1000;s.configure();const p=request();change(p);
  const before=s.e.writes.length;assert.equal(s.reserve(p).code,'NEW_ROOT_CAPTURE_NOT_ADMITTED');assert.equal(s.e.writes.length,before);
 }
});

test('admission: executor, institutional ownership and public writer are verified before any mutation',()=>{
 for(const [change,code] of [
  [s=>s.e.context.Drive.About.get=()=>({user:{emailAddress:'personal@example.invalid'}}),'ROOT_EXECUTOR_MISMATCH'],
  [s=>s.e.context.Drive.About.get=()=>{throw Error('DENIED');},'ROOT_EXECUTOR_NOT_VERIFIED'],
  [s=>s.guard.additional[s.next.id].owners=[{emailAddress:'personal@example.invalid'}],'ROOT_INSTITUTIONAL_OWNERSHIP_REQUIRED'],
  [s=>s.guard.parent.permissions.push({type:'anyone',role:'writer'}),'ROOT_PUBLIC_WRITE_ACCESS']]) {
  const s=setup();s.configure();change(s);const before=s.e.writes.length;
  assert.equal(s.reserve(request()).code,code);assert.equal(s.e.context.gboRootRoutingReady(),false);assert.equal(s.e.writes.length,before);
 }
});

test('admission: committed routing epoch cannot be substituted during replay',()=>{
 const s=setup();s.configure();const p=request();assert.equal(s.reserve(p).ok,true);const before=s.e.writes.length;
 s.config.admission.epoch='d'.repeat(32);s.configure();assert.equal(s.reserve(p).code,'ROOT_ROUTING_DOWNGRADE_BLOCKED');assert.equal(s.e.writes.length,before);
});

function videoFixture(dual) {
 const s=setup();if(dual)s.configure();const p=request();s.reserve(p);const q=s.prepare(p),ack=s.commit(q);assert.equal(ack.ok,true,JSON.stringify(ack));
 vm.runInContext(fs.readFileSync(path.join(__dirname,'../VideoV1.gs'),'utf8'),s.e.context,{filename:'PreparedVideoV1.gs'});
 const video={action:'beginVideoV1',videoProtocol:1,parentKey:p.key,parentRevision:1,mediaId:'FICTITIOUS_VIDEO',category:'GENERAL',categoryLabel:'General',mimeType:'video/mp4',sizeBytes:5,sha256:sha('video')};
 const call=v=>JSON.parse(s.e.context.videoV1Action(clone(v)).getContent());
 return {...s,p,q,ack,video,call};
}

test('video: exact V21 adapter follows the acknowledged parent in either root; replay uses one binary ID',()=>{
 for(const dual of [false,true]){
  const s=videoFixture(dual),before=s.e.objects.size,first=s.call(s.video);assert.equal(first.ok,true,JSON.stringify(first));assert.equal(first.complete,false);
  assert.equal(s.rootOf(first.fileId),dual?s.next.id:ROOT);
  const created=s.e.objects.size;assert.ok(created>before);assert.equal(s.call(s.video).fileId,first.fileId);assert.equal(s.e.objects.size,created);
  const f=s.e.objects.get(first.fileId);f.b=s.e.blob('video','video/mp4',f.name);
  const done=s.call({...s.video,action:'finishVideoV1',fileId:first.fileId});assert.equal(done.ok,true);assert.equal(done.complete,true);assert.equal(done.sha256,s.video.sha256);
  assert.equal(s.call(s.video).fileId,first.fileId);assert.equal(s.call(s.video).complete,true);
 }
});

test('video: pending parent cannot trigger recovery, allocate IDs or create a video journal',()=>{
 const s=videoFixture(true),registry=s.next.getFoldersByName('._GBO_SYNC_V3').next(),file=registry.getFilesByName('journal_'+sha(s.p.key)+'.json').next();
 const pending=JSON.parse(file.getBlob().getDataAsString());pending.status='PENDING';file.setContent(JSON.stringify(pending));const before=s.e.writes.length;
 const a=s.call(s.video);assert.equal(a.code,'BUSY');assert.equal(a.retryable,true);assert.equal(s.e.writes.length,before);assert.equal(s.journal(s.next,s.p).status,'PENDING');
});

test('video: foreign receipt folder rejects before touching either root',()=>{
 const s=videoFixture(true),registry=s.next.getFoldersByName('._GBO_SYNC_V3').next(),file=registry.getFilesByName('record_'+sha(s.p.key)+'.json').next();
 const record=JSON.parse(file.getBlob().getDataAsString());record.receipt.folderId=s.e.outside.id;file.setContent(JSON.stringify(record));const before=s.e.writes.length;
 assert.equal(s.call(s.video).code,'VIDEO_PARENT_ROOT_MISMATCH');assert.equal(s.e.writes.length,before);
});

test('video: DRAIN_ONLY blocks new media but resumes partial sessions and acknowledges completed media',()=>{
 const s=videoFixture(true),first=s.call(s.video);assert.equal(first.ok,true);s.config.mode='DRAIN_ONLY';s.configure();
 const before=s.e.writes.length;assert.equal(s.call({...s.video,mediaId:'FICTITIOUS_UNSEEN_VIDEO'}).code,'ROOT_ROUTING_DRAIN_ONLY');assert.equal(s.e.writes.length,before);
 assert.equal(s.call(s.video).fileId,first.fileId);
 const f=s.e.objects.get(first.fileId);f.b=s.e.blob('video','video/mp4',f.name);
 assert.equal(s.call({...s.video,action:'finishVideoV1',fileId:first.fileId}).complete,true);assert.equal(s.call(s.video).complete,true);
});


test('shared historical root: inaccessible ancestors never guessed, new writes stay institutional and all old writes stay blocked',()=>{
 const s=setup(),old=request();s.reserve(old);const q=s.prepare(old);assert.equal(s.commit(q).ok,true);
 const get=s.e.context.Drive.Files.get;
 s.e.context.Drive.Files.get=(id,...args)=>id===ROOT?{...get(id,...args),owners:[{emailAddress:'historical@example.invalid'}],parents:[]}:get(id,...args);
 s.config.oldRootReadOnly={name:s.e.root.name,ownerEmail:'historical@example.invalid'};s.configure();
 assert.equal(s.e.context.gboRootRoutingReady(),true);
 const before=s.e.writes.length;assert.equal(s.commit(q).code,'HISTORICAL_ROOT_READ_ONLY');assert.equal(s.e.writes.length,before);
 assert.equal(s.reserve(old).code,'HISTORICAL_ROOT_READ_ONLY');assert.equal(s.e.writes.length,before);
 const fresh={...request(),key:'SCAP/FICTITIOUS_NEW_SHARED',recordId:'FICTITIOUS_NEW_SHARED',identityId:'FICTITIOUS_NEW_PARENT',bridgeCode:'FICTITIOUS_NEW_CODE'};
 assert.equal(s.reserve(fresh).ok,true);strict(s,s.commit(s.prepare(fresh)),s.prepare(fresh),s.next.id);
 assert.equal(s.e.context.gboUploadRootsReady(),true);
});

test('shared historical root: wrong recorded owner blocks before reading or writing a new registry',()=>{
 const s=setup();s.config.oldRootReadOnly={name:s.e.root.name,ownerEmail:'wrong@example.invalid'};s.configure();const before=s.e.writes.length;
 assert.equal(s.reserve(request()).code,'HISTORICAL_ROOT_IDENTITY_MISMATCH');assert.equal(s.e.writes.length,before);
});


function syntheticWorkbook(label,row) {
 const parts=[{name:'[Content_Types].xml',text:'<Types xmlns="http://schemas.openxmlformats.org/package/2006/content-types"><Default Extension="xml" ContentType="application/xml"/></Types>'},
  {name:'xl/styles.xml',text:'<styleSheet xmlns="http://schemas.openxmlformats.org/spreadsheetml/2006/main"><cellXfs count="1"><xf numFmtId="0"/></cellXfs></styleSheet>'},
  {name:'xl/worksheets/sheet1.xml',text:`<worksheet xmlns="http://schemas.openxmlformats.org/spreadsheetml/2006/main"><dimension ref="A1:C14"/><sheetData><row r="1"><c r="A1" t="inlineStr"><is><t>${label}</t></is></c></row><row r="14"><c r="A14" t="inlineStr"><is><t>${row}</t></is></c><c r="B14" s="0"><v>10</v></c></row></sheetData></worksheet>`}];
 return zip('zip',parts.map(p=>({name:p.name,base64:Buffer.from(p.text).toString('base64')})));
}
function workbookPart(bytes,name){return Buffer.from(zip('unzip',Buffer.from(bytes).toString('base64')).find(p=>p.name===name).base64,'base64').toString();}
function stageAll(s,p){const q=clone(p);for(const f of q.files){const a=JSON.parse(s.e.context.stageRecordV3({...p,file:f}).getContent());assert.equal(a.ok,true,JSON.stringify(a));f.stageFileId=a.stageFileId;delete f.base64;}
 q.contentHash=sha(s.e.context.SyncV3.canonical(q));return q;}
function culvert(label){const p=JSON.parse(fs.readFileSync(path.join(__dirname,'../../../docs/drive-familias-2a/examples',label,'payload.json'),'utf8'));
 Object.assign(p,{rootRoutingVersion:2,rootEpoch:'e'.repeat(32),captureCreatedAt:Date.now()});
 const row=p.sicRows[0],base64=syntheticWorkbook('SIC-18A',label);
 p.sicRows.push({...row,format:'SIC-18A',base64,sha256:sha(Buffer.from(base64,'base64'))});return p;}

test('two tablets: SIC18 and SIC18A keep both rows, styles and stable IDs after updates and exact ACK replay in the new root',()=>{
 const s=setup();s.configure();const a=culvert('culvert-a'),b=culvert('culvert-b');
 const qa=stageAll(s,a),first=s.commit(qa);assert.equal(first.ok,true,JSON.stringify(first));
 const qb=stageAll(s,b),second=s.commit(qb);assert.equal(second.ok,true,JSON.stringify(second));assert.notEqual(first.folderId,second.folderId);
 for(const f of second.sicFiles){assert.equal(s.rootOf(f.fileId),s.next.id);assert.equal(f.logicalPath,`PE-22A/EXCEL_SIC/${f.format}.xlsx`);assert.deepEqual([...f.recordIds].sort(),['culvert-a','culvert-b']);}
 const edited={...a,revision:a.revision+1,baseRevision:a.revision};edited.sicRows=edited.sicRows.map(r=>{if(r.format!=='SIC-18A')return r;const base64=syntheticWorkbook('SIC-18A','UPDATED_A');return {...r,base64,sha256:sha(Buffer.from(base64,'base64'))};});
 const qe=stageAll(s,edited),ack=s.commit(qe);assert.equal(ack.ok,true,JSON.stringify(ack));assert.equal(ack.folderId,first.folderId);
 for(const f of ack.sicFiles){const previous=second.sicFiles.find(x=>x.format===f.format);assert.equal(f.fileId,previous.fileId);assert.deepEqual([...f.recordIds].sort(),['culvert-a','culvert-b']);
  const bytes=s.e.objects.get(f.fileId).getBlob().getBytes(),sheet=workbookPart(bytes,'xl/worksheets/sheet1.xml');
  assert.equal((sheet.match(/<row r="1[45]"/g)||[]).length,2);
  const original=edited.sicRows.find(r=>r.format===f.format);
  assert.equal(workbookPart(bytes,'xl/styles.xml'),workbookPart(Buffer.from(original.base64,'base64'),'xl/styles.xml'));
  if(f.format==='SIC-18A'){assert.ok(sheet.includes('UPDATED_A'));assert.ok(sheet.includes('culvert-b'));}
 }
 const before=s.e.objects.size;assert.deepEqual(s.commit(qe).sicFiles,ack.sicFiles);assert.equal(s.e.objects.size,before);
 assert.equal(s.e.root.getFoldersByName('._GBO_SYNC_V3').hasNext(),false);
});

test('Pavements: new native delivery routes to the institution with strict hash, ACK and replay',()=>{
 const s=setup();s.configure();const p={...request(),kind:'PAVEMENT',key:'PAVEMENT/FICTITIOUS_PAVEMENT',recordId:'FICTITIOUS_PAVEMENT',identityId:'FICTITIOUS_PAVEMENT',family:'MUESTRA',tramo:'TRAMO FICTICIO',surface:'RIGIDO'};
 delete p.bridgeCode;delete p.familyPath;
 const q=stageAll(s,p),ack=s.commit(q);assert.equal(ack.ok,true,JSON.stringify(ack));assert.equal(s.rootOf(ack.folderId),s.next.id);assert.equal(ack.contentHash,q.contentHash);
 const before=s.e.objects.size;assert.equal(s.commit(q).alreadyExists,true);assert.equal(s.e.objects.size,before);
});


test('activation authority persists before the first Drive write; interrupted registry creation cannot allow downgrade',()=>{
 const s=setup();s.configure();s.next.createFolder=()=>{assert.ok(s.e.props.GBO_ROOT_ROUTING_ACTIVATED);throw Error('FICTITIOUS_FIRST_WRITE_LOST');};
 assert.equal(s.reserve(request()).ok,false);assert.ok(s.e.props.GBO_ROOT_ROUTING_ACTIVATED);
 s.e.props.GBO_ROOT_ROUTING_ENABLED='false';assert.equal(s.reserve(request()).code,'ROOT_ROUTING_DOWNGRADE_BLOCKED');
});

test('duplicate metadata names stop unrelated new deliveries before any write',()=>{
 const s=setup();const registry=s.e.root.createFolder('._GBO_SYNC_V3'),value={key:'SIC/FICTITIOUS_DUPLICATED',identityId:'FICTITIOUS_DUPLICATED',revision:1};
 for(let n=0;n<2;n++)registry.createFile(s.e.blob(JSON.stringify(value),'application/json','record_'+sha(value.key)+'.json'));
 s.configure();const before=s.e.writes.length;assert.equal(s.reserve(request()).code,'ROOT_REGISTRY_REQUIRES_REVIEW');assert.equal(s.e.writes.length,before);
});

test('dual-root writer contention returns BUSY without changing any root, journal, stage or activation',()=>{
 const s=setup();s.configure();s.e.hold(true);const before=s.e.writes.length;const a=s.reserve(request());assert.equal(a.code,'BUSY');assert.equal(s.e.writes.length,before);assert.equal(s.e.props.GBO_ROOT_ROUTING_ACTIVATED,undefined);
});
