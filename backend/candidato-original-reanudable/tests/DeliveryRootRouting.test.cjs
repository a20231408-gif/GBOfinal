// Synthetic in-memory roots only. No HTTP, credentials, Drive accounts or field records.
const {test}=require('node:test'),assert=require('node:assert/strict'),fs=require('node:fs'),path=require('node:path'),vm=require('node:vm');
const {env,ROOT,sha}=require('../../../docs/scap-integracion-actual-20261027/pruebas/MockActual.cjs');
const policy=require('../DeliveryRootRouting.gs');
const clone=x=>JSON.parse(JSON.stringify(x));
const empty=id=>({rootId:id,records:[],bindings:[],pins:[],photoIdentities:[],journals:[],stagedKey:false});
const request=()=>({syncProtocol:3,pathVersion:4,kind:'SCAP',key:'SCAP/FICTITIOUS_INSPECTION',recordId:'FICTITIOUS_INSPECTION',identityId:'FICTITIOUS_PARENT',
 revision:1,baseRevision:0,routeCode:'PE-12A',family:'PUENTE',familyPath:'PUENTES',bridgeCode:'FICTITIOUS_BRIDGE',orderAxis:12000,roadbedCode:'CD',direction:'ASC',startPrNumber:12,startDistanceM:0,
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
 const config={version:1,oldRootId:ROOT,newRoot:{id:next.id,parentId:guard.parent.id,name:next.name,parentName:guard.parent.name,ancestorIds:[guard.parent.id]},
   inventoryReviewed:true,inventorySha256:'a'.repeat(64),mode:'OPEN'};
 let seq=0;e.context.Drive.Files.generateIds=({count=1}={})=>({ids:Array.from({length:count},()=> 'FICTITIOUS_ALLOCATED_'+(++seq))});
 e.context.Drive.Files.update=(m,id,b,options={})=>{const f=e.objects.get(id);assert.ok(f);if(b)f.b=b;if(m.name!==undefined)f.setName(m.name);if(m.description!==undefined)f.setDescription(m.description);
   if(options.addParents&&f.parent.id!==options.addParents)f.moveTo(e.objects.get(options.addParents));return {id};};
 for(const n of ['AuthorizedRootGuard','DeliveryRootRouting','PublicDeliveryNames','PhotoNamesV2Core','SyncV3Core','SyncV3'])
   vm.runInContext(fs.readFileSync(path.join(__dirname,'../',n+'.gs'),'utf8'),e.context,{filename:n+'.gs'});
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
test('adapter: authenticated legacy and video actions are explicitly blocked in dual-root mode',()=>{
 const s=setup();s.configure();const before=s.e.writes.length;
 for(const action of ['uploadScap','uploadDeliveryFile','beginVideoV1','finishVideoV1'])
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
