const {test}=require('node:test'),assert=require('node:assert/strict');
const fs=require('node:fs'),path=require('node:path'),vm=require('node:vm');
const root=path.resolve(__dirname,'../../..');
const backend=path.join(__dirname,'../PARCHE_NO_DESPLEGADO');
const reference=path.join(root,'docs/backend-google-forced-busy-20261017/paquete/FUENTES_V20_REFERENCIA_NO_IMPORTAR');
const {environment,hash}=require(path.join(root,'google-apps-script/DriveV4Harness.cjs'));
const plain=x=>JSON.parse(JSON.stringify(x));
function env(variant='candidate'){
  const e=environment(path.join(reference,'SyncV3.gs.txt'));
  for(const f of ['PhotoNamesV2Core','VideoNamesV2Core','SyncV3Core','SicObservationsV1Core'])vm.runInContext(fs.readFileSync(path.join(reference,f+'.gs.txt'),'utf8'),e.context);
  if(variant==='candidate')enableCandidate(e);
  return e;
}
function fixture(e,orders=[20,15,8,3,1],id='PANEL_FICTICIO'){
  const base=JSON.parse(fs.readFileSync(path.join(root,'docs/fotografias-5/examples/alcantarilla5.json'),'utf8'));
  Object.assign(base,{kind:'SCAP',recordId:id,key:'SCAP/'+id,identityId:'PUENTE_FICTICIO',family:'PUENTE',familyPath:'PUENTES',photoHasEnds:false,photoClassCode:'',photoTypeCode:'',sicRows:[],photoNamingVersion:2});
  const source=base.files.find(f=>f.key.startsWith('photo/'));
  base.files=orders.map(n=>({...source,key:'photo/FICTICIO_'+id+'_'+n,photoCategory:'SCAP20_'+String(n).padStart(2,'0')}));
  const result=e.context.reservePhotoNamesV2({...base,photos:base.files.map(f=>({id:f.key.slice(6),category:f.photoCategory}))});assert.equal(result.ok,true,JSON.stringify(result));
  base.files.forEach(f=>{const cat=e.context.ScapPhotoPanelV2.get(f.photoCategory),reserved=result.photos.find(r=>r.id===f.key.slice(6));f.path='SCAP/'+id+'/'+cat.folder+'/'+reserved.name;});
  return {payload:base,reservation:plain(result)};
}

function enableCandidate(e){for(const f of ['ScapPhotoPanelV2','PhotoNamesV2Core','VideoNamesV2Core','SyncV3Core'])vm.runInContext(fs.readFileSync(path.join(backend,f+'.gs'),'utf8'),e.context);return e;}
module.exports={env,fixture,plain,enableCandidate,hash,root,reference};
