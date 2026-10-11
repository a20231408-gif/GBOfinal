/* Versioned engine shared unchanged by Apps Script and Node regression tests. No secrets. */
var SyncV3 = (() => {
  const reject = (code, retryable=false) => { const e=Error(code);e.code=code;e.retryable=retryable;throw e; };
  const component = s => typeof s==='string' && s.length>0 && s.length<=80 && !/[\\/\x00-\x1f]/.test(s) && s!=='.' && s!=='..';
  function validate(p, store) {
    if(!p || p.syncProtocol!==3 || p.pathVersion!==3 || !['SIC','SCAP','PAVEMENT'].includes(p.kind) ||
      !/^(SIC|SCAP|PAVEMENT)\/[A-Za-z0-9_-]{1,80}$/.test(p.key) || p.key!==p.kind+'/'+p.recordId ||
      !Number.isSafeInteger(p.revision) || p.revision<1 || !Number.isSafeInteger(p.baseRevision) || p.baseRevision<0 ||
      !/^[a-f0-9]{64}$/.test(p.contentHash) || !component(p.routeCode) || !Array.isArray(p.files) || !p.files.length ||
      p.files.length>80 || !Array.isArray(p.sicRows) || p.sicRows.length>12)reject('INVALID_REQUEST');
    if(p.kind==='PAVEMENT') {if(!component(p.tramo) || !['ASFALTICO','RIGIDO','NO_PAVIMENTADO'].includes(p.surface))reject('INVALID_LOCATION');}
    else if(!/^SIB-\d{2}$/.test(p.sibCode) || !component(p.family) || !Number.isFinite(p.orderAxis) || p.orderAxis<0)reject('ORDERING_REQUIRED');
    if(p.orderSource!=null && !['GNSS_MAP_MATCH','CONTRACTUAL_PR'].includes(p.orderSource))reject('INVALID_ORDER_SOURCE');
    if(p.technicalDeliveryWithoutGps===true && (!p.georeferencing ||
      !['PENDING_EXTERNAL','COORDINATES_AVAILABLE'].includes(p.georeferencing.status) ||
      p.georeferencing.externalSubmetricStatus!=='PENDING'))reject('INVALID_GEOREFERENCING_STATE');
    if(p.technicalDeliveryVersion===1 && (!p.georeferencing || !['PENDING_EXTERNAL','COORDINATES_AVAILABLE'].includes(p.georeferencing.status) ||
       p.georeferencing.externalSubmetricStatus!=='PENDING' || !['GNSS_MAP_MATCH','CONTRACTUAL_PR'].includes(p.orderSource)))reject('INVALID_GEOREFERENCING_STATE');
    const seen=new Set();let size=0;
    for(const f of p.files) {
      if(!component(f.key.replace(/\//g,'_')) || seen.has(f.key) || typeof f.path!=='string' ||
         f.path.split('/').some(s=>!component(s) || s.length>20) || !/^[a-f0-9]{64}$/.test(f.sha256))reject('INVALID_FILE');
      seen.add(f.key);
      f.bytes=store.fileBytes?store.fileBytes(p,f):store.decode(f.base64);size+=f.bytes.length;
      if(!f.bytes.length || store.hash(f.bytes)!==f.sha256)reject('HASH_MISMATCH');
      if(f.mimeType==='image/jpeg' && !store.isJpeg(f.bytes))reject('NOT_JPEG');
    }
    for(const row of p.sicRows) {
      if(!/^SIC-(08|09|13|17|17A|17B|18|18A|19|20|21|22|23|34)$/.test(row.format) || !component(row.recordId) ||
        !Number.isFinite(row.orderAxis) || row.orderAxis<0 || !/^[a-f0-9]{64}$/.test(row.sha256) ||
        p.technicalDeliveryVersion===1 && (row.orderSource!==p.orderSource || row.orderAxis!==p.orderAxis))reject('INVALID_ROW');
      const bytes=store.decode(row.base64);size+=bytes.length;
      if(store.hash(bytes)!==row.sha256)reject('HASH_MISMATCH');
      row.workbook=store.unpackWorkbook(bytes);
      if(row.workbook.rows.length!==1)reject('SINGLE_UUID_ROW_REQUIRED');
    }
    if(size>160*1024*1024)reject('REVISION_SIZE_LIMIT');
    if(store.hash(canonical(p))!==p.contentHash)reject('CONTENT_HASH_MISMATCH');
  }
  function canonical(p) {
    const lines=(rows,keys)=>rows.map(row=>keys.map(k=>row[k]==null?'':String(row[k])).join('\t')).sort().join('\n');
    const technical=p.technicalDeliveryVersion===1?'\n'+['technicalDeliveryVersion','orderAxis','orderSource','technicalDeliveryWithoutGps'].map(k=>String(p[k])).join('\t')+
      '\t'+p.georeferencing.status+'\t'+p.georeferencing.externalSubmetricStatus+'\n'+lines(p.sicRows,['recordId','format','orderAxis','orderSource']):'';
    return ['key','revision','routeCode','sibCode','family','tramo','surface'].map(k=>p[k]==null?'':String(p[k])).join('\t')+'\n'+
      lines(p.files,['key','path','mimeType','sha256'])+'\n'+lines(p.sicRows,['recordId','format','sha256'])+technical;
  }
  /** Header/media/styles come from the actual Android workbook. Only data rows and print extent change. */
  function mergeSheet(template, rows) {
    if(!rows.length)reject('EMPTY_AGGREGATE');
    const sheetData=/<sheetData>([\s\S]*?)<\/sheetData>/.exec(template);
    if(!sheetData)reject('INVALID_WORKBOOK');
    const header=(sheetData[1].match(/<row\b[^>]*>[\s\S]*?<\/row>/g)||[]).filter(r=>Number(/\br="(\d+)"/.exec(r)[1])<14).join('');
    const body=rows.map((xml,i)=>{
      if(/<(?:f|v)[^>]*>/.test(xml) && /<f\b/.test(xml))reject('UNSUPPORTED_ROW_FORMULA');
      return xml.replace(/\br="(\d+)"/,`r="${i+14}"`).replace(/\br="([A-Z]+)\d+"/g,(_,col)=>`r="${col}${i+14}"`);
    }).join('');
    return template.replace(sheetData[0],'<sheetData>'+header+body+'</sheetData>')
      .replace(/(<dimension ref="[^"]*:[A-Z]+)\d+("\s*\/?>)/,(_,a,b)=>a+(13+rows.length)+b);
  }
  function commit(p,store) {
    validate(p,store);
    const old=store.get(p.key);
    if(old && old.revision===p.revision) {
      if(old.contentHash!==p.contentHash)reject('REVISION_CONFLICT');
      return {...old.receipt,alreadyExists:true};
    }
    if(old && p.revision<old.revision)reject('STALE_REVISION');
    if((old?.revision || 0)!==p.baseRevision)reject('BASE_REVISION_CONFLICT');
    const all=store.all();
    if(all.length>1000)reject('REGISTRY_CAPACITY');
    if(p.bridgeCode && all.some(x=>x.bridgeCode===p.bridgeCode && x.identityId!==(p.identityId || p.recordId)))reject('BRIDGE_IDENTITY_CONFLICT');
    const next={key:p.key,kind:p.kind,recordId:p.recordId,identityId:p.identityId || p.recordId,bridgeCode:p.bridgeCode || '',
      revision:p.revision,contentHash:p.contentHash,routeCode:p.routeCode,sibCode:p.sibCode,tramo:p.tramo,surface:p.surface,
      family:p.family,orderAxis:p.orderAxis,orderSource:p.orderSource || 'GNSS_MAP_MATCH',
      georeferencing:p.georeferencing || null,sicRows:p.sicRows.map(({workbook,...row})=>row)};
    const missing=(old?.sicRows || []).filter(r=>!next.sicRows.some(n=>n.format===r.format));
    if(missing.length && (old.routeCode!==p.routeCode || old.sibCode!==p.sibCode))reject('LOCATION_RECONCILIATION_REQUIRED');
    next.sicRows=next.sicRows.concat(missing);
    const future=all.filter(x=>x.key!==p.key).concat(next);
    const targets=new Set([...p.sicRows,...(old?.sicRows || [])].map(r=>r.format));
    const prepared=[];
    for(const format of targets) {
      // Each owner is upserted by stable UUID. A tablet sends its own row, never a whole replacement subset.
      for(const scope of new Set([p.routeCode+'/'+p.sibCode,...(old?[old.routeCode+'/'+old.sibCode]:[])])) {
        const sources=future.filter(x=>x.routeCode+'/'+x.sibCode===scope).flatMap(x=>(x.sicRows || []).filter(r=>r.format===format));
        const ids=new Set();sources.forEach(r=>{if(ids.has(r.recordId))reject('ROW_IDENTITY_CONFLICT');ids.add(r.recordId);});
        // Contractual PR and geometric axes have different origins; retain both sources.
        const sorted=sources.sort((a,b)=>Number(a.orderSource==='CONTRACTUAL_PR')-Number(b.orderSource==='CONTRACTUAL_PR') || a.orderAxis-b.orderAxis || a.recordId.localeCompare(b.recordId));
        if(sorted.length) {
          const books=sorted.map(r=>store.unpackWorkbook(store.decode(r.base64)));
          if(books.some(b=>b.signature!==books[0].signature))reject('WORKBOOK_TEMPLATE_CONFLICT');
          const bytes=store.packWorkbook(books[0],mergeSheet(books[0].sheet,books.map(b=>b.rows[0])),sorted.length);
          prepared.push({scope,format,bytes,recordIds:sorted.map(r=>r.recordId),sha256:store.hash(bytes)});
        } else prepared.push({scope,format,retire:true});
      }
    }
    const transaction=store.begin(p,next,all);
    try {
      const files=p.files.map(f=>transaction.file(f));
      const sicFiles=prepared.map(f=>transaction.aggregate(f)).filter(Boolean);
      if(p.removeMissingOwnedFiles===true)transaction.retireMissing(p.files.map(f=>f.key));
      const receipt={ok:true,syncProtocol:3,pathVersion:3,key:p.key,revision:p.revision,contentHash:p.contentHash,
        folderId:transaction.folderId,logicalPath:transaction.logicalPath,files,sicFiles,retainedSicFormats:missing.map(r=>r.format),alreadyExists:false};
      receipt.technicalDeliveryStatus='DELIVERED';
      receipt.georeferencingStatus=p.georeferencing?.status || 'UNASSESSED';
      receipt.externalGeoreferencingStatus=p.georeferencing?.externalSubmetricStatus || 'UNASSESSED';
      receipt.orderSource=next.orderSource;
      next.receipt=receipt;next.files=files;transaction.save(next);
      transaction.finish();return receipt;
    }catch(e){transaction.rollback();throw e;}
  }
  return {commit,validate,mergeSheet,reject,component,canonical};
})();
if(typeof module!=='undefined')module.exports=SyncV3;
