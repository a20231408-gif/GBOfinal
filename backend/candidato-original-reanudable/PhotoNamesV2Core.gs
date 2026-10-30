/* Readable names are labels. Identity and numbering are reserved under ScriptLock by UUID. */
var PhotoNamesV2=(()=>{
  const familyCodes={PUENTE:'PTE',ALCANTARILLA:'ALC',CUNETA:'CUN',CANAL:'CAN','BAJADA DE AGUA':'BAJ','ZANJA DE DRENAJE':'ZDR','ZANJA DE CORONACION':'ZCO','CUNETA DE BANQUETA':'CBA',BADEN:'BAD',MURO:'MUR',TUNEL:'TUN',MARCA:'MAR',TACHA:'TAC',DISPOSITIVO:'SEG',SENAL:'SEN','DERECHO DE VIA':'DVI','ZONA URBANA':'ZUR','PUNTO ESPECIFICO':'PES',CANTERA:'CTE',FAJA:'FAJ','ANCHO FAJA':'AFJ','PUNTO CRITICO':'PCR',ACCIDENTE:'ACC'};
  const categoryCodes={GENERAL:'GEN',INICIO:'INI',FIN:'FIN',OBSERVACION:'OBS',PANORAMICA:'PAN',ELEMENTO:'ELM',CULVERT_PANORAMIC:'PAN',CULVERT_INLET:'ENT',CULVERT_OUTLET:'SAL',ENTRADA:'ENT',SALIDA:'SAL',RIGHT_SIDE:'DER',LEFT_SIDE:'IZQ',UPSTREAM:'ARR',DOWNSTREAM:'ABA',ROAD_DECK:'TAB',INFORMATIVE_SIGN:'INF',RAILINGS_SIDEWALKS_PIPES:'BVT',DEFECT:'DEF',BRIDGE_ELEMENT:'OEP',TRANSVERSE:'TRA',ELEMENT:'ELM',ACCESS:'ACC',CHANNEL:'CAU',OTHER:'OTR'};
  function fail(code){const e=Error(code);e.code=code;e.retryable=false;throw e;}
  function name(family,element,number,category,surface) {
    const panel=typeof ScapPhotoPanelV2!=='undefined' ? ScapPhotoPanelV2.get(category) : null;
    if(panel) {if(family!=='PUENTE')fail('INVALID_SCAP_PANEL_FAMILY');return ScapPhotoPanelV2.fileName(category,element,number,'jpg');}
    const code=family==='MUESTRA'?({ASFALTICO:'PAF',RIGIDO:'PRG',NO_PAVIMENTADO:'PNP'}[surface]):familyCodes[family];
    const cat=categoryCodes[category];
    if(!code || !cat || !Number.isSafeInteger(element) || element<1 || !Number.isSafeInteger(number) || number<1 || number>999)fail('READABLE_NAME_LIMIT_OR_CATALOG');
    const result=String(number).padStart(3,'0')+'_'+code+element+'_'+cat+'.jpg';
    if(result.length>20)fail('READABLE_NAME_LIMIT_OR_CATALOG');
    return result;
  }
  function reserve(p,s) {
    if(!p || p.pathVersion!==4 || p.photoNamingVersion!==2 || !['SIC','SCAP','PAVEMENT'].includes(p.kind) || p.key!==p.kind+'/'+p.recordId ||
       !/^[A-Za-z0-9_-]{1,80}$/.test(p.recordId) || !Number.isSafeInteger(p.revision) || p.revision<1 || !Array.isArray(p.photos) || p.photos.length>80 ||
       !Number.isSafeInteger(p.baseRevision) || p.baseRevision<0 || typeof p.routeCode!=='string' || !/^[A-Za-z0-9_-]{1,80}$/.test(p.routeCode))fail('INVALID_NAME_RESERVATION');
    if(!familyCodes[p.family] && p.family!=='MUESTRA')fail('INVALID_NAME_RESERVATION');
    const seen=new Set();
    for(const f of p.photos) {
      if(!/^[A-Za-z0-9_-]{1,80}$/.test(f.id) || seen.has(f.id) || ['__proto__','constructor','prototype'].includes(f.id) || !(Object.prototype.hasOwnProperty.call(categoryCodes,f.category) || (p.kind==='SCAP' && p.family==='PUENTE' && typeof ScapPhotoPanelV2!=='undefined' && ScapPhotoPanelV2.get(f.category))))fail('INVALID_NAME_RESERVATION');
      seen.add(f.id);
    }
    if(p.publicNamingVersion!=null&&(p.publicNamingVersion!==1||typeof p.publicDisplayLabel!=='string'||PublicDeliveryNames.component(p.publicDisplayLabel)!==p.publicDisplayLabel))fail('INVALID_PUBLIC_LABEL');
    const old=s.record(p.key);
    if((old?.revision || 0)!==p.baseRevision)fail('BASE_REVISION_CONFLICT');
    const location=s.element(p);
    const current=s.names(p.identityId || p.recordId) || {next:0,photos:{}};
    // Work on a copy; a catalog/length conflict cannot partially reserve the batch.
    const updated=JSON.parse(JSON.stringify(current));
    if(p.publicNamingVersion===1&&!updated.publicDisplayLabel)updated.publicDisplayLabel=p.publicDisplayLabel;
    const result=p.photos.map(f=>{
      const previous=Object.prototype.hasOwnProperty.call(updated.photos,f.id)?updated.photos[f.id]:null;
      if(previous) {if(previous.category!==f.category)fail('PHOTO_CATEGORY_RECONCILIATION_REQUIRED');return {id:f.id,...previous};}
      const historical=(old?.files || []).find(x=>x.key==='photo/'+f.id);
      const saved=historical?{category:f.category,name:historical.requestedPath.split('/').pop(),historical:true}:
          (p.publicNamingVersion===1?{category:f.category,name:PublicDeliveryNames.photoName(updated.publicDisplayLabel,f.category,++updated.next),number:updated.next,publicNamingVersion:1,historical:false}:{category:f.category,name:name(p.family,location.sequence,++updated.next,f.category,p.surface),historical:false});
      updated.photos[f.id]=saved;return {id:f.id,...saved};
    });
    s.saveNames(p.identityId || p.recordId,updated);
    const publicInfo=p.publicNamingVersion===1?{publicNamingVersion:1,publicDisplayLabel:updated.publicDisplayLabel,publicElementFolder:location.logicalPath.split('/').pop()}:{};
    return {...publicInfo,ok:true,photoNamingVersion:2,key:p.key,revision:p.revision,identityId:p.identityId || p.recordId,folderId:location.folder.getId(),logicalPath:location.logicalPath,photos:result};
  }
  return {SCAP_PANEL_VERSION:2,familyCodes,categoryCodes,name,reserve};
})();
if(typeof module!=='undefined')module.exports=PhotoNamesV2;
