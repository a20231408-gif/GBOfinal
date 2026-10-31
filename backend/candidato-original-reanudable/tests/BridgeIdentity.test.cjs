const {test}=require('node:test'),assert=require('node:assert/strict'),fs=require('node:fs'),path=require('node:path'),vm=require('node:vm'),crypto=require('node:crypto');
const sha=b=>crypto.createHash('sha256').update(b).digest('hex');
for(const variant of ['referencia-v21','candidato-original-reanudable']){
 function setup(records=[]){
  const c=vm.createContext({});vm.runInContext(fs.readFileSync(path.join(__dirname,'../../',variant,'SyncV3Core.gs'),'utf8'),c);
  const p={syncProtocol:3,pathVersion:4,kind:'SCAP',key:'SCAP/FICTITIOUS_INSPECTION',recordId:'FICTITIOUS_INSPECTION',identityId:'FICTITIOUS_ROAD_UUID',revision:1,baseRevision:0,routeCode:'PE-12A',family:'PUENTE',familyPath:'PUENTES',orderAxis:12000,roadbedCode:'CD',direction:'ASC',startPrNumber:12,startDistanceM:0,bridgeCode:'FICTITIOUS_BRIDGE_CODE',sicRows:[],files:[{key:'metadata',path:'SCAP/FICTITIOUS_INSPECTION/metadata.json',mimeType:'application/json',base64:Buffer.from('{}').toString('base64'),sha256:sha('{}')}]};
  let begins=0;const store={get:key=>records.find(r=>r.key===key),all:()=>records,hash:sha,decode:b=>Buffer.from(b,'base64'),begin:()=>{begins++;throw Object.assign(Error('BEGIN_REACHED_WITHOUT_WRITE'),{code:'BEGIN_REACHED_WITHOUT_WRITE'});}};
  return {c,p,store,get begins(){return begins;},send(){p.contentHash=sha(c.SyncV3.canonical(p));return c.SyncV3.commit(p,store);}};
 }
 test(variant+': equal bridge code with another UUID conflicts independently of name, route and progressive',()=>{
  for(const route of ['PE-12A','PE-22A']){
   const s=setup([{key:'SCAP/OTHER',recordId:'OTHER',identityId:'OTHER_UUID',bridgeCode:'FICTITIOUS_BRIDGE_CODE',routeCode:route}]);
   assert.throws(()=>s.send(),e=>e.code==='BRIDGE_IDENTITY_CONFLICT'&&e.retryable===false);assert.equal(s.begins,0);
  }
 });
 test(variant+': companion SIC with the same road UUID is not another bridge',()=>{
  const s=setup([{key:'SIC/FICTITIOUS_ROAD_UUID',recordId:'FICTITIOUS_ROAD_UUID',identityId:'FICTITIOUS_ROAD_UUID',bridgeCode:'FICTITIOUS_BRIDGE_CODE',sicRows:[]}]);
  assert.throws(()=>s.send(),e=>e.code==='BEGIN_REACHED_WITHOUT_WRITE');assert.equal(s.begins,1);
 });
 test(variant+': missing historical identity is not silently adopted from a matching name or code',()=>{
  const s=setup([{key:'SIC/HISTORICAL',recordId:'FICTITIOUS_ROAD_UUID',bridgeCode:'FICTITIOUS_BRIDGE_CODE'}]);
  assert.throws(()=>s.send(),e=>e.code==='BRIDGE_IDENTITY_CONFLICT');assert.equal(s.begins,0);
 });
 test(variant+': legacy path version requires reviewed migration before accepting a modern request',()=>{
  const s=setup();s.store.get=()=>({pathVersion:3,revision:1,recordId:s.p.recordId,identityId:s.p.identityId});
  assert.throws(()=>s.send(),e=>e.code==='HISTORICAL_MIGRATION_REQUIRED');assert.equal(s.begins,0);
 });
 test(variant+': exact acknowledged replay returns the previous receipt, never another transaction',()=>{
  const s=setup();const prior={key:s.p.key,recordId:s.p.recordId,identityId:s.p.identityId,pathVersion:4,revision:1,contentHash:sha(s.c.SyncV3.canonical(s.p)),receipt:{ok:true,fictitiousReceipt:true},files:[]};s.store.get=()=>prior;
  const ack=s.send();assert.equal(ack.fictitiousReceipt,true);assert.equal(ack.alreadyExists,true);assert.equal(s.begins,0);
 });
}
