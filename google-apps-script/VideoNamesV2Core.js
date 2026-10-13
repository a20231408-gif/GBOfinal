/* Human labels only: UUID/category/fileId remain authoritative. Install as .gs. */
var VideoNamesV2=(()=>{
  // Code switch for coordinated rollback; no additional ScriptProperty.
  const ENABLED=true;
  const catalog=[
    ['ACCESO IZQUIERDO','ACI'],['AFIRMADO ACCESO IZQUIERDO','AFI'],
    ['ACCESO DERECHO','ACD'],['AFIRMADO ACCESO DERECHO','AFD'],
    ['SUPERFICIE DE RODARURA','ROD'],['AGUAS ARRIBA','ARR'],['AGUAS ABAJO','ABA'],
    ['ALA IZQUIERDA AGUAS ARRIBA','AIAR'],['ALA DERECHA AGUAS ARRIBA','ADAR'],
    ['ALA IZQUIERDA AGUAS ABAJO','AIAB'],['ALA DERECHA AGUAS ABAJO','ADAB'],
    ['PANTALLA DE ESTRIBO IZQUIERDO','PEI'],['PANTALLA DE ESTRIBO DERECHO','PED'],
    ['FISURA PANTALLA DE ESTRIBO DERECHO','FPED'],
    ['VIGA LONGITUDINAL AGUAS ARRIBA','VLAR'],['VIGA LONGITUDINAL AGUAS ABAJO','VLAB'],
    ['VIGAS TRANSVERSALES','VTR'],['FISURA EN VIGA TRANSVERSAL','FVTR'],
    ['LOSA - CARA INFERIOR','LCI'],['FONDO DE LOSA - MOHO','FLM'],
    ['VEREDA AGUAS ARRIBA','VEAR'],['VEREDA AGUAS ARRIBA - FRACTURA','VEAF'],
    ['VEREDA AGUAS ABAJO','VEAB'],['BARANDA AGUAS ARRIBA','BAAR'],
    ['TRAMO DE BARANDA AGUAS ARRIBA','TBAR'],['BARANDAS AGUAS ABAJO','BAAB'],
    ['TRAMO DE BARANDA AGUAS ABAJO','TBAB'],['TUBERIA DE DRENAJE AGUAS ARRIBA-ABAJO','TDRE'],
    ['CAUCE AGUAS ARRIBA','CAAR'],['CAUCE AGUAS ABAJO','CAAB'],
    ['SEÑAL INFORMATIVA DEL ACCESO IZQUIERDO','SIAI']
  ];
  // Categories used by the existing non-canonical SCAP form.
  const legacy={RIGHT_SIDE:['VISTA LADO DERECHO','DER'],LEFT_SIDE:['VISTA LADO IZQUIERDO','IZQ'],
    UPSTREAM:['AGUAS ARRIBA','ARR'],DOWNSTREAM:['AGUAS ABAJO','ABA'],ROAD_DECK:['CARRETERA TABLERO','TAB'],
    INFORMATIVE_SIGN:['SEÑAL INFORMATIVA','INF'],RAILINGS_SIDEWALKS_PIPES:['BARANDAS VEREDAS TUBERIAS','BVT'],
    DEFECT:['ANOMALIAS FALLAS','DEF'],BRIDGE_ELEMENT:['OTROS ELEMENTOS PUENTE','OEP'],
    GENERAL:['VISTA GENERAL','GEN'],TRANSVERSE:['VISTA TRANSVERSAL','TRA'],ELEMENT:['ELEMENTO','ELM'],
    ACCESS:['ACCESO','ACC'],CHANNEL:['CAUCE','CAU'],OTHER:['OTRA','OTR']};
  const suffixes={'video/mp4':'mp4','video/webm':'webm','video/3gpp':'3gp'};
  const normalize=s=>s.normalize('NFD').replace(/[\u0300-\u036f]/g,'').toUpperCase().replace(/[^A-Z0-9]+/g,' ').trim();
  function reject(code){const e=Error(code);e.code=code;throw e;}
  function category(p) {
    const label=normalize(p.categoryLabel);
    // A T_<hash> is a persistent Android category identity, never a folder label.
    const entry=catalog.find(x=>normalize(x[0])===label) ||
      (Object.prototype.hasOwnProperty.call(legacy,p.category)?legacy[p.category]:null);
    if(entry)return {code:entry[1],folder:normalize(entry[0]).replace(/ /g,'_')};
    // The form also permits edited/custom names. Preserve them, with a compact
    // readable abbreviation. Colliding abbreviations share a counter below.
    if(!label)reject('VIDEO_CATEGORY_LABEL_REQUIRED');
    const words=label.split(' '),code=(words.length===1?words[0].slice(0,4):words.map(w=>w[0]).join('').slice(0,4));
    return {code,folder:label.replace(/ /g,'_').slice(0,80)};
  }
  function reserve(p,element,state) {
    if(!Number.isSafeInteger(element) || element<1)reject('VIDEO_ELEMENT_BINDING_REQUIRED');
    const updated=JSON.parse(JSON.stringify(state || {next:{},videos:{}}));
    const old=Object.prototype.hasOwnProperty.call(updated.videos,p.mediaId)?updated.videos[p.mediaId]:null;
    if(old) {
      if(['parentKey','category','mimeType','sizeBytes','sha256'].some(k=>old[k]!==p[k]))reject('VIDEO_NAME_BINDING_CONFLICT');
      return {state:updated,naming:old.naming};
    }
    const c=category(p),number=(updated.next[c.code] || 0)+1;
    const name=String(number).padStart(3,'0')+'_PUE'+element+'_'+c.code+'.'+suffixes[p.mimeType];
    if(number>999 || !suffixes[p.mimeType] || name.length>20)reject('VIDEO_READABLE_NAME_LIMIT');
    const naming={videoNamingVersion:2,fileName:name,categoryFolder:c.folder,categoryCode:c.code,videoNumber:number,elementNumber:element};
    updated.next[c.code]=number;
    updated.videos[p.mediaId]={parentKey:p.parentKey,category:p.category,mimeType:p.mimeType,sizeBytes:p.sizeBytes,sha256:p.sha256,naming};
    return {state:updated,naming};
  }
  return {ENABLED,catalog,legacy,category,reserve};
})();
if(typeof module!=='undefined')module.exports=VideoNamesV2;
