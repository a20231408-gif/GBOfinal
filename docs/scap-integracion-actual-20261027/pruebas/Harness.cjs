const {test}=require('node:test'),assert=require('node:assert/strict');
const fs=require('node:fs'),path=require('node:path'),vm=require('node:vm');
const root=path.resolve(__dirname,'../../..');
const backend=path.join(__dirname,'integrado_NO_IMPORTAR');
const reference=path.join(__dirname,'referencia_actual_NO_IMPORTAR');
const hash=require('./MockActual.cjs').sha;
const plain=x=>x&&typeof x.getContent==='function'?JSON.parse(x.getContent()):JSON.parse(JSON.stringify(x));
function env(variant='candidate'){return require('./MockActual.cjs').env(variant==='base'?'ACTUAL':'INTEGRADO');}
function fixture(e,orders=[20,15,8,3,1],id='PANEL_FICTICIO'){
  const base=JSON.parse(fs.readFileSync(path.join(__dirname,'fixtures/alcantarilla5.json'),'utf8'));
  Object.assign(base,{kind:'SCAP',recordId:id,key:'SCAP/'+id,identityId:'PUENTE_FICTICIO',family:'PUENTE',familyPath:'PUENTES',photoHasEnds:false,photoClassCode:'',photoTypeCode:'',sicRows:[],photoNamingVersion:2});
  const source=base.files.find(f=>f.key.startsWith('photo/'));
  base.files=orders.map(n=>({...source,key:'photo/FICTICIO_'+id+'_'+n,photoCategory:'SCAP20_'+String(n).padStart(2,'0')}));
  const result=plain(e.context.reservePhotoNamesV2({...base,photos:base.files.map(f=>({id:f.key.slice(6),category:f.photoCategory}))}));assert.equal(result.ok,true,JSON.stringify(result));
  base.files.forEach(f=>{const cat=e.context.ScapPhotoPanelV2.get(f.photoCategory),reserved=result.photos.find(r=>r.id===f.key.slice(6));f.path='SCAP/'+id+'/'+cat.folder+'/'+reserved.name;});
  return {payload:base,reservation:plain(result)};
}

function enableCandidate(e){for(const f of ['ScapPhotoPanelV2','PhotoNameV2Core','VideoNamesV2Core','SyncV3Core'])vm.runInContext(fs.readFileSync(path.join(backend,f+'.gs.txt'),'utf8'),e.context);return e;}
module.exports={env,fixture,plain,enableCandidate,hash,root,reference};
