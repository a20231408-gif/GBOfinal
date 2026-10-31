// Synthetic Drive metadata only. Does not change the shared historical mock.
const {ROOT}=require('../../../docs/scap-integracion-actual-20261027/pruebas/MockActual.cjs');
function rootGuardMock(e){
 const parent='MEMORY_PARENT_ONLY',myDrive='MEMORY_MY_DRIVE_ONLY';
 Object.assign(e.props,{GBO_AUTHORIZED_ROOT_ID:ROOT,GBO_AUTHORIZED_ROOT_PARENT_ID:parent,
  GBO_AUTHORIZED_ROOT_NAME:e.root.name,GBO_AUTHORIZED_ROOT_PARENT_NAME:'MEMORY FIELD PARENT',GBO_AUTHORIZED_ROOT_ANCESTOR_IDS:JSON.stringify([parent])});
 const state={root:{parents:[parent],capabilities:{canEdit:true,canAddChildren:true}},
  parent:{id:parent,name:'MEMORY FIELD PARENT',mimeType:'application/vnd.google-apps.folder',trashed:false,parents:[myDrive]},
  myDrive:{id:myDrive},additional:{},error:false};
 const get=e.context.Drive.Files.get;
 e.context.Drive.Files.get=(id,...args)=>{
  if(state.error)throw Error('SIMULATED_READ_FAILURE');
  if(id==='root')return state.myDrive;
  if(id===parent)return state.parent;
  if(state.additional[id])return state.additional[id];
  const m=get(id,...args);return id===ROOT?{...m,...state.root}:m;
 };
 return state;
}
module.exports={rootGuardMock};
