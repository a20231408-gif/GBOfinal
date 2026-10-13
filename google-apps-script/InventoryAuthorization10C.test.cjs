const {test}=require('node:test');
const assert=require('node:assert/strict');
const crypto=require('node:crypto');
const core=require('./InventoryAuthorization10C.js');
function environment() {
  const grants=new Map(),audit=[];let now=1000000;
  const user={id:'engineer',role:'OPERATOR',active:true,inventoryScopes:['INVENTORY_ANNUL','INVENTORY_RESTORE'],inventoryRoutes:['R10C']};
  const tablet={active:true,fingerprint:'tablet-10c'};
  const s={now:()=>now,hash:x=>crypto.createHash('sha256').update(x).digest('hex'),secureToken:()=>crypto.randomBytes(32).toString('base64url'),
    verifyTablet:(token,fp)=>token==='tablet-secret' && fp===tablet.fingerprint?tablet:null,
    verifyPin:(id,pin)=>id===user.id && pin==='test-pin'?user:null,putGrant:(hash,value)=>grants.set(hash,value),getGrant:hash=>grants.get(hash),
    audit:e=>audit.push(e),verifyBackend:secret=>secret==='backend-only-secret',tabletByFingerprint:()=>tablet,user:()=>user};
  const issuance={tabletToken:'tablet-secret',fingerprint:tablet.fingerprint,userId:user.id,pin:'test-pin',kind:'SIC',recordId:'uuid-alc2',routeCode:'R10C'};
  function request() {const grant=core.issue(issuance,s);return {...issuance,pin:undefined,tabletToken:undefined,inventoryToken:grant.inventoryToken,backendCredential:'backend-only-secret',operation:'ANNUL',opId:'op-annul-1',baseRevision:5,reason:'Ficha ficticia'};}
  return {s,user,tablet,grants,audit,issuance,request,advance:()=>now+=600001};
}
test('individual PIN and explicit route permission issue only target-bound opaque hashed grants',()=>{
  const e=environment(),p=e.request(),verified=core.authorize(p,e.s);
  assert.equal(verified.ok,true);assert.equal(verified.recordId,'uuid-alc2');assert.equal(verified.baseRevision,5);
  assert.equal(e.grants.has(p.inventoryToken),false);assert.equal(e.grants.size,1);
  assert.equal(JSON.stringify(e.audit).includes(p.inventoryToken),false);assert.equal(JSON.stringify(e.audit).includes('test-pin'),false);
  assert.equal(verified.inventoryToken,undefined);assert.equal(verified.syncProtocol,undefined);
});
test('ADMIN or TABLET_ADMIN claims alone never grant inventory permission',()=>{
  const e=environment();e.user.role='ADMIN';e.user.inventoryScopes=[];
  assert.throws(()=>core.issue({...e.issuance,role:'ADMIN',scope:'TABLET_ADMIN_ONLY',inventoryScopes:['INVENTORY_ANNUL']},e.s),{code:'INVENTORY_PERMISSION_REQUIRED'});
  assert.equal(e.grants.size,0);
});
test('bad PIN, tablet token and wrong route cannot obtain a grant',()=>{
  const e=environment();
  assert.throws(()=>core.issue({...e.issuance,pin:'wrong'},e.s),{code:'INVENTORY_PERMISSION_REQUIRED'});
  assert.throws(()=>core.issue({...e.issuance,tabletToken:'drive-shared-token'},e.s),{code:'DEVICE_REVOKED'});
  assert.throws(()=>core.issue({...e.issuance,routeCode:'other-route'},e.s),{code:'INVENTORY_PERMISSION_REQUIRED'});
});
test('Drive token cannot introspect grants and tokens cannot switch tablet, UUID, user, route or kind',()=>{
  const e=environment(),p=e.request();
  assert.throws(()=>core.authorize({...p,backendCredential:'drive-shared-token'},e.s),{code:'TRUSTED_BACKEND_REQUIRED'});
  for(const change of [{fingerprint:'other'},{recordId:'uuid-alc3'},{userId:'other'},{routeCode:'other'},{kind:'SCAP'}])
    assert.throws(()=>core.authorize({...p,...change},e.s),{code:'INVENTORY_GRANT_SCOPE'});
});
test('revocation, permission removal and expiry are checked again for every request',()=>{
  const e=environment(),p=e.request();e.tablet.active=false;
  assert.throws(()=>core.authorize(p,e.s),{code:'DEVICE_REVOKED'});e.tablet.active=true;e.user.inventoryScopes=[];
  assert.throws(()=>core.authorize(p,e.s),{code:'INVENTORY_PERMISSION_REQUIRED'});e.user.inventoryScopes=['INVENTORY_ANNUL'];e.advance();
  assert.throws(()=>core.authorize(p,e.s),{code:'INVENTORY_GRANT_EXPIRED'});
});
test('restoration requires its own permission and reason/revision/operation identifiers remain mandatory',()=>{
  const e=environment();e.user.inventoryScopes=['INVENTORY_ANNUL'];const p=e.request();
  assert.throws(()=>core.authorize({...p,operation:'RESTORE'},e.s),{code:'INVENTORY_GRANT_SCOPE'});
  for(const change of [{baseRevision:-1},{reason:''},{opId:''},{operation:'DELETE_PHYSICALLY'}])
    assert.throws(()=>core.authorize({...p,...change},e.s),{code:'INVALID_INVENTORY_REQUEST'});
});
test('a mismatched authoritative user cannot satisfy another engineers grant',()=>{
  const e=environment(),p=e.request();e.user.id='other-engineer';
  assert.throws(()=>core.authorize(p,e.s),{code:'INVENTORY_PERMISSION_REQUIRED'});
});
