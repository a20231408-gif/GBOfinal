/* Versioned engine shared unchanged by Apps Script and Node regression tests. No secrets. */
var SyncV3 = (() => {
  const reject = (code, retryable=false, remoteRevision=null) => { const e=Error(code);e.code=code;e.retryable=retryable;e.remoteRevision=remoteRevision;throw e; };
  const component = s => typeof s==='string' && s.length>0 && s.length<=80 && !/[\\/\x00-\x1f]/.test(s) && s!=='.' && s!=='..';
  const families={
    PUENTE:['PUENTES',['SIC-17','SIC-17A','SIC-17B']], ALCANTARILLA:['ALCANTARILLAS',['SIC-18','SIC-18A']],
    CUNETA:['CUNETAS',['SIC-19']], CANAL:['CANALES',['SIC-19']], 'BAJADA DE AGUA':['BAJADAS_AGUA',['SIC-19']],
    'ZANJA DE DRENAJE':['ZANJAS_DRENAJE',['SIC-19']], 'ZANJA DE CORONACION':['ZANJAS_CORONACION',['SIC-19']],
    'CUNETA DE BANQUETA':['CUNETAS_BANQUETA',['SIC-19']], BADEN:['BADENES',['SIC-20']], MURO:['MUROS',['SIC-20']], TUNEL:['TUNELES',['SIC-20']],
    MARCA:['SENALIZACION_Y_SEGURIDAD_VIAL/MARCAS',['SIC-21']], TACHA:['SENALIZACION_Y_SEGURIDAD_VIAL/TACHAS',['SIC-21']],
    DISPOSITIVO:['SENALIZACION_Y_SEGURIDAD_VIAL/SEGURIDAD_VIAL',['SIC-21']], SENAL:['SENALIZACION_VERTICAL',['SIC-22']],
    'DERECHO DE VIA':['DERECHO_DE_VIA',['SIC-23']], 'ZONA URBANA':['ZONAS_URBANAS',['SIC-23']], 'PUNTO ESPECIFICO':['PUNTOS_ESPECIFICOS',['SIC-23']], CANTERA:['CANTERAS',['SIC-23']],
    FAJA:['FAJAS_CALZADA',['SIC-08']], 'ANCHO FAJA':['ANCHOS_FAJA',['SIC-09']],
    'PUNTO CRITICO':['PUNTOS_CRITICOS',['SIC-13']], ACCIDENTE:['ACCIDENTES',['SIC-34']]
  };
  const aggregateScope=p=>p.pathVersion===4?p.routeCode+'/EXCEL_SIC':p.routeCode+'/'+p.sibCode;
  const decreasing=d=>['D','DECRECIENTE','DESCENDING','REVERSE','DECREASING'].includes(d);
  const measure=r=>(r.startPrNumber*1000+r.startDistanceM)*(decreasing(r.direction)?-1:1);
  const compareText=(a,b)=>a===b?0:a<b?-1:1;
  const normalized=s=>String(s).trim().toUpperCase();
  function compareTechnical(a,b) {
    return compareText(normalized(a.roadbedCode),normalized(b.roadbedCode)) || compareText(normalized(a.direction),normalized(b.direction)) ||
      measure(a)-measure(b) || compareText(a.recordId,b.recordId);
  }
  function allocateNumber(scope,family,identity,bindings) {
    const previous=bindings.find(b=>b.identity===identity);
    if(previous) {
      if(previous.scope!==scope || previous.family!==family)reject('IDENTITY_RELOCATION_REQUIRES_MIGRATION');
      return previous.sequence;
    }
    return Math.max(0,...bindings.filter(b=>b.scope===scope && b.family===family).map(b=>b.sequence))+1;
  }
  // Validated internal codes are the sole authority for new photographic paths.
  const photoFolders={GENERAL:'GENERAL',INICIO:'INICIO',FIN:'FIN',OBSERVACION:'OBSERVACION',PANORAMICA:'PANORAMICA',ELEMENTO:'ELEMENTO',
    CULVERT_INLET:'ENTRADA',CULVERT_OUTLET:'SALIDA',CULVERT_PANORAMIC:'PANORAMICA',ENTRADA:'ENTRADA',SALIDA:'SALIDA',
    RIGHT_SIDE:'VISTA_LADO_DERECHO',LEFT_SIDE:'VISTA_LADO_IZQUIERDO',UPSTREAM:'AGUAS_ARRIBA',DOWNSTREAM:'AGUAS_ABAJO',ROAD_DECK:'CARRETERA_TABLERO',
    INFORMATIVE_SIGN:'SENAL_INFORMATIVA',RAILINGS_SIDEWALKS_PIPES:'BARANDAS_VEREDAS_TUBERIAS',DEFECT:'ANOMALIAS_FALLAS',BRIDGE_ELEMENT:'OTROS_ELEMENTOS_PUENTE',
    TRANSVERSE:'VISTA_TRANSVERSAL',ELEMENT:'ELEMENTO',ACCESS:'ACCESO',CHANNEL:'CAUCE',OTHER:'OTRA'};
  const scapCategories=['GENERAL','RIGHT_SIDE','LEFT_SIDE','UPSTREAM','DOWNSTREAM','ROAD_DECK','INFORMATIVE_SIGN','RAILINGS_SIDEWALKS_PIPES','DEFECT','BRIDGE_ELEMENT','TRANSVERSE','ELEMENT','ACCESS','CHANNEL','OTHER'];
  function photoPath(p,f,hash) {
    let allowed=['GENERAL'];
    if(p.kind==='SCAP' || p.family==='PUENTE')allowed=scapCategories;
    else if(p.family==='ALCANTARILLA')allowed=allowed.concat(['CULVERT_INLET','CULVERT_OUTLET','CULVERT_PANORAMIC','ENTRADA','SALIDA','PANORAMICA']);
    else if(p.family==='SENAL')allowed=allowed.concat(['PANORAMICA','ELEMENTO']);
    else if(p.family==='BADEN' || p.photoHasEnds===true && (['MURO','TUNEL','CUNETA','CANAL','BAJADA DE AGUA','ZANJA DE DRENAJE','ZANJA DE CORONACION','CUNETA DE BANQUETA','TACHA','MARCA','DERECHO DE VIA','ZONA URBANA'].includes(p.family) || p.family==='DISPOSITIVO' && p.photoClassCode==='19' && p.photoTypeCode==='1'))allowed=allowed.concat(['INICIO','FIN','OBSERVACION']);
    if(!allowed.includes(f.photoCategory) || !Object.prototype.hasOwnProperty.call(photoFolders,f.photoCategory))reject('INVALID_PHOTO_CATEGORY');
    const id=f.key.substring(6);
    if(!component(id))reject('INVALID_PHOTO_ID');
    return (p.kind==='SCAP'?'SCAP/'+p.recordId+'/':'')+photoFolders[f.photoCategory]+'/F_'+hash(id).substring(0,16)+'.jpg';
  }
  function validate(p, store) {
    if(!p || p.syncProtocol!==3 || ![3,4].includes(p.pathVersion) || !['SIC','SCAP','PAVEMENT'].includes(p.kind) ||
      !/^(SIC|SCAP|PAVEMENT)\/[A-Za-z0-9_-]{1,80}$/.test(p.key) || p.key!==p.kind+'/'+p.recordId ||
      !Number.isSafeInteger(p.revision) || p.revision<1 || !Number.isSafeInteger(p.baseRevision) || p.baseRevision<0 ||
      !/^[a-f0-9]{64}$/.test(p.contentHash) || !component(p.routeCode) || !Array.isArray(p.files) || !p.files.length ||
      p.files.length>80 || !Array.isArray(p.sicRows) || p.sicRows.length>200)reject('INVALID_REQUEST');
    if(p.kind==='PAVEMENT') {if(p.pathVersion===4 && p.family!=='MUESTRA')reject('INVALID_FAMILY_LOCATION');if(!component(p.tramo) || !['ASFALTICO','RIGIDO','NO_PAVIMENTADO'].includes(p.surface))reject('INVALID_LOCATION');}
    else if(p.pathVersion===4) {
      const family=families[p.family];
      if(!family || p.familyPath!==family[0] || p.sibCode!=null || !Number.isFinite(p.orderAxis) || p.orderAxis<0 ||
        typeof p.roadbedCode!=='string' || typeof p.direction!=='string' || !Number.isSafeInteger(p.startPrNumber) || p.startPrNumber<0 ||
        !Number.isFinite(p.startDistanceM) || p.startDistanceM<0 || p.identityId!=null && !component(p.identityId))reject('INVALID_FAMILY_LOCATION');
      if(p.kind==='SCAP' && p.family!=='PUENTE')reject('INVALID_SCAP_FAMILY');
      if(p.kind==='SIC' && p.identityId!=null && p.identityId!==p.recordId)reject('ROW_OWNER_MISMATCH');
      if(p.sicRows.some(r=>!family[1].includes(r.format)))reject('SIC_FAMILY_MISMATCH');
    } else if(!/^SIB-\d{2}$/.test(p.sibCode) || !component(p.family) || !Number.isFinite(p.orderAxis) || p.orderAxis<0)reject('ORDERING_REQUIRED');
    const existing=store.get(p.key);
    if(p.pathVersion===3 && (!existing || existing.revision!==p.revision))reject('LEGACY_READ_ONLY');
    if(p.pathVersion===4 && existing && (existing.pathVersion || 3)!==4)reject('HISTORICAL_MIGRATION_REQUIRED');
    if(p.orderSource!=null && !['GNSS_MAP_MATCH','CONTRACTUAL_PR'].includes(p.orderSource))reject('INVALID_ORDER_SOURCE');
    if(p.technicalDeliveryWithoutGps===true && (!p.georeferencing ||
      !['PENDING_EXTERNAL','COORDINATES_AVAILABLE'].includes(p.georeferencing.status) ||
      p.georeferencing.externalSubmetricStatus!=='PENDING'))reject('INVALID_GEOREFERENCING_STATE');
    if(p.technicalDeliveryVersion===1 && (!p.georeferencing || !['PENDING_EXTERNAL','COORDINATES_AVAILABLE'].includes(p.georeferencing.status) ||
       p.georeferencing.externalSubmetricStatus!=='PENDING' || !['GNSS_MAP_MATCH','CONTRACTUAL_PR'].includes(p.orderSource)))reject('INVALID_GEOREFERENCING_STATE');
    if(p.photoCategoryVersion!=null && p.photoCategoryVersion!==1)reject('UNSUPPORTED_PHOTO_CATEGORY_VERSION');
    if(p.photoCategoryVersion===1 && (p.pathVersion!==4 || !['SIC','SCAP'].includes(p.kind) || typeof p.photoHasEnds!=='boolean' || typeof p.photoClassCode!=='string' || typeof p.photoTypeCode!=='string'))reject('INVALID_PHOTO_CONTRACT');
    const seen=new Set();let size=0;
    for(const f of p.files) {
      if(!component(f.key.replace(/\//g,'_')) || seen.has(f.key) || typeof f.path!=='string' ||
         f.path.split('/').some(s=>!component(s) || s.length>80 || p.pathVersion===4 && /^SIB(?:[-_ ]|$)/i.test(s)) || !/^[a-f0-9]{64}$/.test(f.sha256))reject('INVALID_FILE');
      if(p.photoCategoryVersion===1 && f.key.startsWith('photo/')) {
        if(f.mimeType!=='image/jpeg' || f.path!==photoPath(p,f,store.hash))reject('PHOTO_PATH_CATEGORY_MISMATCH');
      }
      seen.add(f.key);
      f.bytes=store.fileBytes?store.fileBytes(p,f):store.decode(f.base64);size+=f.bytes.length;
      if(!f.bytes.length || store.hash(f.bytes)!==f.sha256)reject('HASH_MISMATCH');
      if(f.mimeType==='image/jpeg' && !store.isJpeg(f.bytes))reject('NOT_JPEG');
    }
    for(const row of p.sicRows) {
      if(p.pathVersion===4 && (p.kind==='PAVEMENT'?!row.recordId.startsWith(p.recordId+'_'):row.recordId!==(p.identityId || p.recordId)))reject('ROW_OWNER_MISMATCH');
      if(p.pathVersion===4 && (!Number.isSafeInteger(row.startPrNumber) || row.startPrNumber<0 || !Number.isFinite(row.startDistanceM) || row.startDistanceM<0 || typeof row.direction!=='string' || typeof row.roadbedCode!=='string'))reject('INVALID_TECHNICAL_ROW');
      if(p.kind==='PAVEMENT' && !({ASFALTICO:['SIC-14','SIC-14A'],RIGIDO:['SIC-15'],NO_PAVIMENTADO:['SIC-16']}[p.surface] || []).includes(row.format))reject('SIC_SURFACE_MISMATCH');
      if(p.pathVersion===4 && p.kind!=='PAVEMENT' && ['roadbedCode','direction','startPrNumber','startDistanceM'].some(k=>row[k]!==p[k]))reject('ROW_ORDER_MISMATCH');
      if(!/^SIC-(08|09|13|14|14A|15|16|17|17A|17B|18|18A|19|20|21|22|23|34)$/.test(row.format) || !component(row.recordId) ||
        !Number.isFinite(row.orderAxis) || row.orderAxis<0 || !/^[a-f0-9]{64}$/.test(row.sha256) ||
        p.technicalDeliveryVersion===1 && (row.orderSource!==p.orderSource || row.orderAxis!==p.orderAxis))reject('INVALID_ROW');
      const bytes=store.decode(row.base64);size+=bytes.length;
      if(store.hash(bytes)!==row.sha256)reject('HASH_MISMATCH');
      row.workbook=store.unpackWorkbook(bytes, /^SIC-(14|14A|15|16)$/.test(row.format)?3:14);
      if(row.workbook.rows.length!==1)reject('SINGLE_UUID_ROW_REQUIRED');
    }
    if(size>160*1024*1024)reject('REVISION_SIZE_LIMIT');
    if(store.hash(canonical(p))!==p.contentHash)reject('CONTENT_HASH_MISMATCH');
  }
  function canonical(p) {
    const lines=(rows,keys)=>rows.map(row=>keys.map(k=>row[k]==null?'':String(row[k])).join('\t')).sort().join('\n');
    const technical=p.technicalDeliveryVersion===1?'\n'+['technicalDeliveryVersion','orderAxis','orderSource','technicalDeliveryWithoutGps'].map(k=>String(p[k])).join('\t')+
      '\t'+p.georeferencing.status+'\t'+p.georeferencing.externalSubmetricStatus+'\n'+lines(p.sicRows,['recordId','format','orderAxis','orderSource']):'';
    return (p.pathVersion===4?['key','revision','routeCode','familyPath','family','tramo','surface','identityId','roadbedCode','direction','startPrNumber','startDistanceM']:['key','revision','routeCode','sibCode','family','tramo','surface']).map(k=>p[k]==null?'':String(p[k])).join('\t')+'\n'+
      lines(p.files,['key','path','mimeType','sha256'])+'\n'+lines(p.sicRows,['recordId','format','sha256'])+technical+(p.pathVersion===4?'\npathVersion=4\n'+lines(p.sicRows,['recordId','format','roadbedCode','direction','startPrNumber','startDistanceM']):'')+(p.photoCategoryVersion===1?'\nphotoCategoryVersion=1\n'+['photoHasEnds','photoClassCode','photoTypeCode'].map(k=>String(p[k])).join('\t')+'\n'+lines(p.files,['key','photoCategory']):'');
  }
  /** Header/media/styles come from the actual Android workbook. Only data rows and print extent change. */
  function mergeSheet(template, rows, start=14) {
    if(!rows.length)reject('EMPTY_AGGREGATE');
    const sheetData=/<sheetData>([\s\S]*?)<\/sheetData>/.exec(template);
    if(!sheetData)reject('INVALID_WORKBOOK');
    const header=(sheetData[1].match(/<row\b[^>]*>[\s\S]*?<\/row>/g)||[]).filter(r=>Number(/\br="(\d+)"/.exec(r)[1])<start).join('');
    const body=rows.map((xml,i)=>{
      if(/<(?:f|v)[^>]*>/.test(xml) && /<f\b/.test(xml))reject('UNSUPPORTED_ROW_FORMULA');
      return xml.replace(/\br="(\d+)"/,`r="${i+start}"`).replace(/\br="([A-Z]+)\d+"/g,(_,col)=>`r="${col}${i+start}"`);
    }).join('');
    return template.replace(sheetData[0],'<sheetData>'+header+body+'</sheetData>')
      .replace(/(<dimension ref="[^"]*:[A-Z]+)\d+("\s*\/?>)/,(_,a,b)=>a+(start-1+rows.length)+b);
  }
  function commit(p,store) {
    validate(p,store);
    const old=store.get(p.key);
    if(old && old.revision===p.revision) {
      if(old.contentHash!==p.contentHash)reject('REVISION_CONFLICT',false,old?.revision || 0);
      return {...old.receipt,alreadyExists:true};
    }
    if(old && p.revision<old.revision)reject('STALE_REVISION',false,old?.revision || 0);
    if((old?.revision || 0)!==p.baseRevision)reject('BASE_REVISION_CONFLICT',false,old?.revision || 0);
    const all=store.all();
    if(all.length>1000)reject('REGISTRY_CAPACITY');
    if(p.bridgeCode && all.some(x=>x.bridgeCode===p.bridgeCode && x.identityId!==(p.identityId || p.recordId)))reject('BRIDGE_IDENTITY_CONFLICT');
    const next={key:p.key,kind:p.kind,recordId:p.recordId,identityId:p.identityId || p.recordId,bridgeCode:p.bridgeCode || '',
      revision:p.revision,contentHash:p.contentHash,pathVersion:p.pathVersion,routeCode:p.routeCode,sibCode:p.sibCode,familyPath:p.familyPath,tramo:p.tramo,surface:p.surface,
      family:p.family,orderAxis:p.orderAxis,orderSource:p.orderSource || 'GNSS_MAP_MATCH',
      georeferencing:p.georeferencing || null,sicRows:p.sicRows.map(({workbook,...row})=>row)};
    const missing=(old?.sicRows || []).filter(r=>!next.sicRows.some(n=>n.format===r.format));
    if(missing.length && (aggregateScope(old)!==aggregateScope(p)))reject('LOCATION_RECONCILIATION_REQUIRED');
    next.sicRows=next.sicRows.concat(missing);
    const future=all.filter(x=>x.key!==p.key).concat(next);
    const targets=new Set([...p.sicRows,...(old?.sicRows || [])].map(r=>r.format));
    const prepared=[];
    for(const format of targets) {
      // Each owner is upserted by stable UUID. A tablet sends its own row, never a whole replacement subset.
      for(const scope of new Set([aggregateScope(p),...(old?[aggregateScope(old)]:[])])) {
        let sources=future.filter(x=>aggregateScope(x)===scope).flatMap(x=>(x.sicRows || []).filter(r=>r.format===format)
          .map(r=>({...r,ownerRevision:x.revision,ownerKey:x.key})));
        if(p.pathVersion===4) {
          const latest=new Map();
          sources.forEach(r=>{const old=latest.get(r.recordId);if(!old || r.ownerRevision>old.ownerRevision || r.ownerRevision===old.ownerRevision && r.ownerKey>old.ownerKey)latest.set(r.recordId,r);});
          sources=[...latest.values()];
        }
        const ids=new Set();sources.forEach(r=>{if(ids.has(r.recordId))reject('ROW_IDENTITY_CONFLICT');ids.add(r.recordId);});
        // Contractual PR and geometric axes have different origins; retain both sources.
        const sorted=sources.sort(p.pathVersion===4?compareTechnical:(a,b)=>a.orderAxis-b.orderAxis || a.recordId.localeCompare(b.recordId));
        if(sorted.length) {
          const books=sorted.map(r=>store.unpackWorkbook(store.decode(r.base64), /^SIC-(14|14A|15|16)$/.test(r.format)?3:14));
          if(books.some(b=>b.signature!==books[0].signature))reject('WORKBOOK_TEMPLATE_CONFLICT');
          const bytes=store.packWorkbook(books[0],mergeSheet(books[0].sheet,books.map(b=>b.rows[0]),books[0].dataStartRow || 14),sorted.length);
          prepared.push({scope,format,bytes,recordIds:sorted.map(r=>r.recordId),sha256:store.hash(bytes)});
        } else prepared.push({scope,format,retire:true});
      }
    }
    const transaction=store.begin(p,next,all);
    try {
      const files=p.files.map(f=>transaction.file(f));
      const sicFiles=prepared.map(f=>transaction.aggregate(f)).filter(Boolean);
      if(p.removeMissingOwnedFiles===true)transaction.retireMissing(p.files.map(f=>f.key));
      const receipt={ok:true,syncProtocol:3,pathVersion:p.pathVersion,key:p.key,revision:p.revision,contentHash:p.contentHash,
        folderId:transaction.folderId,logicalPath:transaction.logicalPath,files,sicFiles,retainedSicFormats:missing.map(r=>r.format),alreadyExists:false};
      receipt.technicalDeliveryStatus='DELIVERED';
      receipt.georeferencingStatus=p.georeferencing?.status || 'UNASSESSED';
      receipt.externalGeoreferencingStatus=p.georeferencing?.externalSubmetricStatus || 'UNASSESSED';
      receipt.orderSource=next.orderSource;
      next.receipt=receipt;next.files=files;transaction.save(next);
      transaction.finish();return receipt;
    }catch(e){transaction.rollback();throw e;}
  }
  return {commit,validate,mergeSheet,reject,component,canonical,families,aggregateScope,compareTechnical,allocateNumber};
})();
if(typeof module!=='undefined')module.exports=SyncV3;
