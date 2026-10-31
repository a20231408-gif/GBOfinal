const {test}=require('node:test'),assert=require('node:assert/strict'),fs=require('node:fs'),path=require('node:path'),vm=require('node:vm');
const {env,ROOT}=require('../../../docs/scap-integracion-actual-20261027/pruebas/MockActual.cjs');
const {rootGuardMock}=require('./RootGuardMock.cjs');
function setup(){const e=env('INTEGRADO'),state=rootGuardMock(e);vm.runInContext(fs.readFileSync(path.join(__dirname,'../AuthorizedRootGuard.gs'),'utf8'),e.context);return {e,state};}
function rejects(change,code){const {e,state}=setup();change(e,state);const before=e.writes.length;assert.throws(()=>e.context.gboAuthorizedRoot(),x=>x.code===code);assert.equal(e.context.gboRootReady(),false);assert.equal(e.writes.length,before);}
test('verified explicit IDs and parent chain allow creation only beneath authorized folder',()=>{
 const {e}=setup(),before=e.writes.length;assert.equal(e.context.gboRootReady(),true);assert.equal(e.writes.length,before);
 assert.equal(e.context.gboAuthorizedRoot().getId(),ROOT);e.context.getOrCreateFolder(e.context.gboAuthorizedRoot(),'CHILD_ONLY_IN_MEMORY');
 assert.deepEqual(e.writes.slice(before),[{op:'mkdir',parent:ROOT}]);
});
test('every missing identity property rejects before any write',()=>{for(const key of ['GBO_AUTHORIZED_ROOT_ID','GBO_AUTHORIZED_ROOT_PARENT_ID','GBO_AUTHORIZED_ROOT_NAME','GBO_AUTHORIZED_ROOT_PARENT_NAME','GBO_AUTHORIZED_ROOT_ANCESTOR_IDS'])rejects(e=>delete e.props[key],'ROOT_NOT_AUTHORIZED');});
test('explicit nested ancestor chain is accepted; malformed, cyclic or substituted chains are blocked',()=>{
 const {e,state}=setup(),container='MEMORY_CONTAINER_ONLY';e.props.GBO_AUTHORIZED_ROOT_ANCESTOR_IDS=JSON.stringify([state.parent.id,container]);
 state.additional[container]={id:container,name:'CONTAINER',mimeType:state.parent.mimeType,trashed:false,parents:[state.myDrive.id]};state.parent.parents=[container];
 const before=e.writes.length;assert.equal(e.context.gboAuthorizedRoot().getId(),ROOT);assert.equal(e.writes.length,before);
 for(const value of ['INVALID_JSON','null','[]',JSON.stringify(['root']),JSON.stringify([state.parent.id,state.parent.id])])rejects(e=>e.props.GBO_AUTHORIZED_ROOT_ANCESTOR_IDS=value,'ROOT_NOT_AUTHORIZED');
 state.additional[container].parents=[state.parent.id];assert.throws(()=>e.context.gboAuthorizedRoot(),x=>x.code==='ROOT_IDENTITY_MISMATCH');assert.equal(e.writes.length,before);
});
test('root aliases, mismatched configured ID and equal parent ID cannot fall back to My Drive',()=>{
 for(const id of ['root','','OTHER_MEMORY_ROOT'])rejects(e=>e.props.GBO_AUTHORIZED_ROOT_ID=id,'ROOT_NOT_AUTHORIZED');
 rejects(e=>e.props.GBO_AUTHORIZED_ROOT_PARENT_ID=ROOT,'ROOT_NOT_AUTHORIZED');
 rejects((e,s)=>s.myDrive.id=ROOT,'ROOT_IDENTITY_MISMATCH');
 rejects((e,s)=>s.myDrive.id=e.props.GBO_AUTHORIZED_ROOT_PARENT_ID,'ROOT_IDENTITY_MISMATCH');
});
test('same display name never authorizes a sibling, moved folder or wrong parent',()=>{
 rejects((e,s)=>s.root.id='SIBLING_MEMORY_ID','ROOT_IDENTITY_MISMATCH');
 rejects((e,s)=>s.root.parents=['OTHER_MEMORY_PARENT'],'ROOT_IDENTITY_MISMATCH');
 rejects((e,s)=>s.parent.parents=['OTHER_MY_DRIVE'],'ROOT_IDENTITY_MISMATCH');
 rejects((e,s)=>s.root.parents=[],'ROOT_IDENTITY_MISMATCH');
});
test('renamed, trashed and non-folder identities fail closed',()=>{
 for(const field of ['root','parent']){
  rejects((e,s)=>s[field].name='RENAMED','ROOT_IDENTITY_MISMATCH');
  rejects((e,s)=>s[field].trashed=true,'ROOT_IDENTITY_MISMATCH');
  rejects((e,s)=>s[field].mimeType='application/vnd.google-apps.shortcut','ROOT_IDENTITY_MISMATCH');
 }
});
test('read-only or unknown write capabilities reject without a sentinel file',()=>{
 for(const capabilities of [{canEdit:false,canAddChildren:true},{canEdit:true,canAddChildren:false},undefined])rejects((e,s)=>s.root.capabilities=capabilities,'ROOT_NOT_WRITABLE');
});
test('Drive read failure is retryable and never creates a fallback folder',()=>{
 const {e,state}=setup();state.error=true;const before=e.writes.length;
 assert.throws(()=>e.context.gboAuthorizedRoot(),x=>x.code==='ROOT_VERIFICATION_FAILED'&&x.retryable===true);assert.equal(e.writes.length,before);
});
