// Optional private package path is provided explicitly; the test never sends HTTP or prints source/configuration.
const {test}=require('node:test'),assert=require('node:assert/strict'),fs=require('node:fs'),path=require('node:path'),vm=require('node:vm');
const {env,ROOT}=require('../../../docs/scap-integracion-actual-20261027/pruebas/MockActual.cjs');
const original=process.env.GBO_PREPARED_ORIGINAL_DIR;
function prepared(){
 assert.ok(original,'Provide GBO_PREPARED_ORIGINAL_DIR for a locally prepared private package.');
 const e=env('INTEGRADO');e.props.GBO_RESUMABLE_ROOT_ID=ROOT;
 const guard=require('./RootGuardMock.cjs').rootGuardMock(e);
 const context=vm.createContext({...e.context});
 for(const name of ['Codigo','AuthorizedRootGuard','DeliveryRootRouting','NewRootAdmission','ScapPhotoPanelV2','SicObservationsV1Core','VideoNamesV2Core','VideoV1Core','VideoV1','PublicDeliveryNames','SyncV3Core','PhotoNamesV2Core','SyncV3']){
  let source=fs.readFileSync(path.join(original,name+'.gs'),'utf8');
  if(name==='Codigo'){
   const pattern=/^const ROOT_FOLDER_ID\s*=.*?;/m;assert.ok(pattern.test(source),'Root declaration absent; no private value printed.');
   source=source.replace(pattern,"const ROOT_FOLDER_ID = '"+ROOT+"';");
  }
  vm.runInContext(source,context,{filename:name+'.gs'});
 }
 return {e,guard,context,json:out=>JSON.parse(out.getContent())};
}
test('complete original package keeps protocol 3/V4/V2/panel 2 and announces resumable commits with explicit matching root',()=>{
 const {e,context,json}=prepared();const caps=json(context.doGet());assert.equal(caps.syncProtocol,3);assert.equal(caps.pathVersion,4);assert.equal(caps.readablePhotoNamingVersion,2);assert.equal(caps.scapPhotoPanelVersion,2);assert.equal(caps.commitContinuationVersion,1);assert.equal(caps.rootFolderId,ROOT);
 assert.equal(caps.publicNamingVersion,0);e.props.GBO_PUBLIC_NAMES_ENABLED='true';assert.equal(json(context.doGet()).publicNamingVersion,1);
});
test('authenticated legacy, cached and V3 routes all stop before writes when root identity is unapproved',()=>{
 const {e,context,json}=prepared();delete e.props.GBO_AUTHORIZED_ROOT_ID;
 assert.equal(json(context.doGet()).rootGuardReady,false);assert.equal(json(context.doGet()).commitContinuationVersion,0);
 for(const action of ['beginVideoV1','finishVideoV1','reservePhotoNamesV2','stageRecordV3','commitRecordV3','uploadScap','uploadSicExport','uploadDeliveryFile','uploadPavementFile']){
  const before=e.writes.length;const result=json(context.doPost({postData:{contents:JSON.stringify({token:e.props.GBO_DRIVE_API_TOKEN,action})}}));
  assert.equal(result.code,'ROOT_NOT_AUTHORIZED',action);assert.equal(e.writes.length,before,action);
 }
});
test('prepared package guards all root lookups, with unchanged manifest and no test helpers',()=>{
 const files=fs.readdirSync(original);assert.equal(files.includes('Ensayo.gs'),false);assert.equal(files.includes('TestIsolation.gs'),false);
 for(const name of files.filter(n=>n.endsWith('.gs')&&n!=='AuthorizedRootGuard.gs'))assert.doesNotMatch(fs.readFileSync(path.join(original,name),'utf8'),/DriveApp\.getFolderById\(\s*ROOT_FOLDER_ID\s*\)/,name);
 const manifest=JSON.parse(fs.readFileSync(path.join(original,'MANIFIESTO_PRIVADO.json'),'utf8'));
 assert.equal(fs.readFileSync(path.join(original,'appsscript.json'),'utf8'),fs.readFileSync(path.join(manifest.originalSource,'appsscript.json'),'utf8'));
});
test('original router still rejects invalid application tokens without a Drive mutation',()=>{
 const {e,context,json}=prepared();const before=e.writes.length;
 assert.equal(json(context.doPost({postData:{contents:JSON.stringify({token:'FICTITIOUS_INVALID',action:'commitRecordV3'})}})).ok,false);assert.equal(e.writes.length,before);
});
test('missing approval root cannot advertise continuation or mutate the operational store',()=>{
 const {e,context,json}=prepared();delete e.props.GBO_RESUMABLE_ROOT_ID;
 assert.equal(json(context.doGet()).commitContinuationVersion,0);const before=e.writes.length;
 assert.equal(json(context.commitRecordV3({})).code,'RESUMABLE_ROOT_NOT_APPROVED');assert.equal(e.writes.length,before);
});

test('prepared authenticated router refuses unreviewed dual-root and video/legacy paths before writes',()=>{
 const {e,context,json}=prepared();e.props.GBO_ROOT_ROUTING_ENABLED='true';
 assert.equal(json(context.doGet()).commitContinuationVersion,0);assert.equal(json(context.doGet()).rootRoutingVersion,0);
 const before=e.writes.length;
 for(const action of ['beginVideoV1','finishVideoV1','uploadScap','uploadSicExport','uploadDeliveryFile','uploadPavementFile']){
  const reply=json(context.doPost({postData:{contents:JSON.stringify({token:e.props.GBO_DRIVE_API_TOKEN,action})}}));
  assert.equal(reply.code,'ROOT_ROUTING_NOT_APPROVED',action);
 }
 assert.equal(e.writes.length,before);
});


test('prepared GET negotiates routing epoch without exposing executor and verifies the institutional identity once',()=>{
 const {e,guard,context,json}=prepared(),next=new e.root.constructor('FICTITIOUS_DESTINATION',null,'MEMORY_NEW_ROOT');
 guard.additional[next.id]={id:next.id,name:next.name,mimeType:'application/vnd.google-apps.folder',trashed:false,parents:[guard.parent.id],capabilities:{canEdit:true,canAddChildren:true}};
 for(const f of [guard.parent,guard.additional[next.id]])Object.assign(f,{owners:[{emailAddress:'institutional@example.invalid'}],permissions:[{type:'user',role:'owner'}]});
 const get=context.Drive.Files.get;context.Drive.Files.get=(id,...a)=>id===ROOT?{...get(id,...a),owners:[{emailAddress:'historical@example.invalid'}],parents:[]}:get(id,...a);
 let aboutReads=0;context.Drive.About={get:()=>{aboutReads++;return {user:{emailAddress:'institutional@example.invalid'}};}};
 e.props.GBO_ROOT_ROUTING_ENABLED='true';e.props.GBO_ROOT_ROUTING_CONFIG=JSON.stringify({version:1,oldRootId:ROOT,oldRootReadOnly:{name:e.root.name,ownerEmail:'historical@example.invalid'},
  newRoot:{id:next.id,parentId:guard.parent.id,name:next.name,parentName:guard.parent.name,ancestorIds:[guard.parent.id]},
  inventoryReviewed:true,inventorySha256:'a'.repeat(64),mode:'OPEN',admission:{version:1,epoch:'e'.repeat(32),activatedAt:100,executorEmail:'institutional@example.invalid'}});
 const before=e.writes.length,caps=json(context.doGet());assert.equal(caps.rootGuardReady,true);assert.equal(caps.rootRoutingVersion,2);assert.equal(caps.rootEpoch,'e'.repeat(32));assert.equal(caps.rootActivatedAt,100);
 assert.equal(caps.commitContinuationVersion,1);assert.equal(aboutReads,1);assert.equal(e.writes.length,before);assert.ok(!JSON.stringify(caps).includes('institutional@example.invalid'));
});
