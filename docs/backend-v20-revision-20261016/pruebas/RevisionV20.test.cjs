const {test}=require('node:test'),assert=require('node:assert/strict'),fs=require('node:fs'),path=require('node:path'),vm=require('node:vm');
const {environment,hash,ROOT,INPUT}=require('./HarnessV20.cjs');
const {payload,sample,bridge,rewrite,annotated,reserve,clone,parts,sheet,manifests}=require('./CasosV20.cjs');
const evidence=path.join(__dirname,'../evidencia');
const variants=['v20','stage','borrador','instrumentado'];
const plain=x=>JSON.parse(JSON.stringify(x));
const ok=a=>{assert.equal(a.ok,true,JSON.stringify(a));return a;};
const edit=p=>({...clone(p),revision:p.revision+1,baseRevision:p.revision});
function paired(name,fn){test(name,()=>{const results=variants.map(v=>plain(fn(environment(v))));for(const r of results.slice(1))assert.deepEqual(r,results[0]);});}

test('Ocho originales compilan; siete módulos equivalentes a referencia sin aplicar optimizaciones duplicadas',()=>{
 for(const f of fs.readdirSync(path.join(INPUT,'originales-v20')))new vm.Script(fs.readFileSync(path.join(INPUT,'originales-v20',f),'utf8'));
 const par=JSON.parse(fs.readFileSync(path.join(evidence,'paridad-referencia.json')));
 assert.equal(par.filter(x=>x.equalIgnoringBlankLinesAndTrailingSpace).length,7);
 const core=fs.readFileSync(path.join(__dirname,'../candidato-no-publicar/SyncV3Core.gs'),'utf8');
 assert.equal((core.match(/const verifiedBooks=/g)||[]).length,1);
 const a=fs.readFileSync(path.join(__dirname,'../candidato-no-publicar/SyncV3.gs'),'utf8');
 assert.equal((a.match(/function v3LockTiming/g)||[]).length,1);assert.equal((a.match(/const bytes=Utilities.base64Decode\(f.base64\)/g)||[]).length,1);
});
paired('A B C D: A B D C en XLSX; carpeta C conserva número 3 y D número 4',e=>{
 const replies=[['A',1],['B',2],['C',4],['D',3]].map(([id,pr])=>ok(e.send(payload('TEST_'+id,pr))));
 assert.deepEqual([...replies[3].sicFiles[0].recordIds],['TEST_A','TEST_B','TEST_D','TEST_C']);
 assert.ok(replies[2].logicalPath.endsWith('ALCANTARILLA 3'));assert.ok(replies[3].logicalPath.endsWith('ALCANTARILLA 4'));
 const retry=ok(e.send(payload('TEST_C',4)));assert.equal(retry.folderId,replies[2].folderId);assert.equal(retry.alreadyExists,true);
 return {replies,retry,parts:parts(e,replies[3])};
});
paired('Fotos V2, staging y ACK perdidos: mismos bytes SHA-256 y fileId',e=>{
 const p=payload();const reservation=reserve(e,p),stages=[];
 for(const f of p.files){const req={syncProtocol:3,key:p.key,revision:p.revision,file:clone(f)},a=ok(e.context.stageRecordV3(req));assert.deepEqual(plain(e.context.stageRecordV3(req)),plain(a));stages.push(a);f.stageFileId=a.stageFileId;delete f.base64;}
 const ack=ok(e.send(p)),updates=e.updates,replay=ok(e.send(p));assert.equal(replay.alreadyExists,true);assert.equal(e.updates,updates);
 for(const f of ack.files)assert.equal(hash(Buffer.from(e.objects.get(f.fileId).getBlob().getBytes())),f.sha256);
 return {reservation,stages,ack,replay};
});
paired('Históricos sin Observaciones + nueva nota literal + edición legacy + borrado explícito',e=>{
 const old=payload('TEST_OLD',1),modern=annotated(payload('TEST_MODERN',2),' =1+1 & PINTADO OK 57 E2 ');
 ok(e.send(old));const a=ok(e.send(modern));assert.ok(sheet(e,a).includes(' =1+1 &amp; PINTADO OK 57 E2 '));
 const legacy=edit(payload('TEST_MODERN',2)),b=ok(e.send(legacy));assert.ok(sheet(e,b).includes('PINTADO OK'));
 const record=manifests(e).find(m=>m.key===legacy.key);assert.ok(record.sicRows[0].observationsTemplateBase64);
 const other=ok(e.send(payload('TEST_OTHER',3)));assert.ok(sheet(e,other).includes('PINTADO OK'));
 const legacyAgain={...clone(legacy),revision:3,baseRevision:2};const c=ok(e.send(legacyAgain));assert.ok(sheet(e,c).includes('PINTADO OK'));
 const clear=annotated({...payload('TEST_MODERN',2),revision:4,baseRevision:3},''),d=ok(e.send(clear));assert.ok(!sheet(e,d).includes('PINTADO OK'));
 const s=sheet(e,d);assert.ok(!/<c r="N\d+" s=/.test(s));assert.ok(!s.includes('<f>'));assert.equal(d.sicFiles[0].fileId,a.sicFiles[0].fileId);
 return {a,b,c,d,s,manifest:manifests(e)};
});
paired('Histórico con Observaciones + cliente legacy nuevo + encabezados, XML y medios preservados',e=>{
 const a=ok(e.send(annotated(payload('TEST_WITH_NOTE',1),'TEXTO HISTÓRICO'))),original=parts(e,a),b=ok(e.send(payload('TEST_WITHOUT_NOTE',2)));
 assert.ok(sheet(e,b).includes('TEXTO HISTÓRICO'));const current=parts(e,b);
 for(const p of original)if(!['xl/worksheets/sheet1.xml','xl/workbook.xml'].includes(p.name))assert.equal(current.find(x=>x.name===p.name).base64,p.base64,p.name);
 return {a,b,parts:current};
});
for(const surface of ['ASFALTICO','RIGIDO','NO_PAVIMENTADO'])paired('Pavimentos '+surface+': varias filas por muestra, revisión, JSON opaco 1.4.7 y ACK',e=>{
 const p=sample('TEST_PAV_'+surface,surface,3);const f=p.files.find(f=>f.key==='metadata');
 const bytes=Buffer.from(JSON.stringify({synthetic:true,sicData:{_gboJointAnnotations:{version:1,marks:[{kind:'V',row:0,column:1},{kind:'N',row:1,column:1}]}}}));Object.assign(f,{base64:bytes.toString('base64'),sha256:hash(bytes)});
 reserve(e,p);const a=ok(e.send(p)),q=edit(p),b=ok(e.send(q));assert.equal(a.folderId,b.folderId);assert.equal(b.sicFiles[0].recordIds.length,3);
 const meta=b.files.find(f=>f.key==='metadata');assert.equal(Buffer.from(e.objects.get(meta.fileId).getBlob().getBytes()).toString(),bytes.toString());
 assert.equal(manifests(e).length,1);return {a,b,parts:parts(e,b)};
});
paired('SCAP sin filas SIC conserva fotos V2 y padre para videos 10H',e=>{
 const p=bridge();reserve(e,p);const a=ok(e.send(p));
 const parent=e.lock(()=>e.context.videoV1Store().parent(p.key));assert.equal(parent.receipt.folderId,a.folderId);assert.equal(parent.kind,'SCAP');
 e.state.busy=true;const busy=e.context.videoV1Action({action:'beginVideoV1'});assert.equal(busy.code,'BUSY');
 return {a,parent,busy};
});
paired('Hash de archivo, hash canónico, plantilla y fila inválidos se rechazan sin confirmar',e=>{
 const p=payload(),bad=clone(p);bad.files[0].sha256='a'.repeat(64);assert.equal(e.send(bad).code,'HASH_MISMATCH');
 const canonical=clone(p);canonical.contentHash='b'.repeat(64);assert.equal(e.context.commitRecordV3(canonical).code,'CONTENT_HASH_MISMATCH');
 const invalid=clone(p);invalid.sicRows[0].sha256='f'.repeat(64);assert.equal(e.send(invalid).code,'HASH_MISMATCH');
 const two=clone(p);rewrite(two.sicRows[0],parts=>{const f=parts.find(x=>x.name==='xl/worksheets/sheet1.xml');let s=Buffer.from(f.base64,'base64').toString();const row=s.match(/<row\b[^>]*r="14"[^>]*>[\s\S]*?<\/row>/)[0];s=s.replace('</sheetData>',row.replace(/14/g,'15')+'</sheetData>');f.base64=Buffer.from(s).toString('base64');});
 assert.equal(e.send(two).code,'SINGLE_UUID_ROW_REQUIRED');assert.equal(manifests(e).length,0);return {confirmed:0};
});
paired('Conflicto de plantilla conserva Excel y manifiesto anteriores',e=>{
 const a=ok(e.send(payload('TEST_TEMPLATE_A',1))),before=parts(e,a),p=payload('TEST_TEMPLATE_B',2);
 rewrite(p.sicRows[0],parts=>{const f=parts.find(x=>x.name==='xl/styles.xml');f.base64=Buffer.from(Buffer.from(f.base64,'base64').toString()+'<!-- incompatible -->').toString('base64');});
 const b=e.send(p);assert.equal(b.code,'WORKBOOK_TEMPLATE_CONFLICT');assert.deepEqual(parts(e,a),before);assert.equal(manifests(e).length,1);return {a,b};
});
paired('Retención de formato ausente usa workbook histórico, no el de otro formato',e=>{
 const p=payload('TEST_TWO_FORMATS');p.sicRows.push({...clone(p.sicRows[0]),format:'SIC-18A'});const a=ok(e.send(p));
 const q=edit(p);q.sicRows=q.sicRows.filter(r=>r.format==='SIC-18');const b=ok(e.send(q));
 assert.deepEqual([...b.retainedSicFormats],['SIC-18A']);assert.equal(manifests(e)[0].sicRows.length,2);return {a,b};
});
paired('Revisiones stale, base conflictiva y edición no renumeran ni duplican',e=>{
 const p=payload(),a=ok(e.send(p)),q=edit(p);q.startPrNumber=1;q.orderAxis=1000;q.sicRows.forEach(r=>Object.assign(r,{startPrNumber:1,orderAxis:1000}));const b=ok(e.send(q));
 assert.equal(b.folderId,a.folderId);assert.equal(b.files[0].fileId,a.files[0].fileId);
 assert.equal(e.send({...p,revision:3,baseRevision:0}).code,'BASE_REVISION_CONFLICT');assert.equal(e.send(p).code,'STALE_REVISION');
 const conflict=clone(q);const f=conflict.files.find(f=>f.key==='metadata'),bytes=Buffer.from('CONFLICTING CONTENT');Object.assign(f,{base64:bytes.toString('base64'),sha256:hash(bytes)});assert.equal(e.send(conflict).code,'REVISION_CONFLICT');return {a,b};
});
for(const phase of ['file','aggregate','save','finish'])paired('Recuperación PENDING tras interrupción en '+phase+' antes de ACK',e=>{
 const p=payload(),a=ok(e.send(p)),old=parts(e,a),q=edit(p),f=q.files.find(f=>f.key==='metadata');const bytes=Buffer.from('SYNTHETIC EDIT');Object.assign(f,{base64:bytes.toString('base64'),sha256:hash(bytes)});
 const original=e.context.v3Store;let crash=true;
 e.context.v3Store=function(...args){const s=original(...args),begin=s.begin;s.begin=function(...args){const tx=begin(...args),fn=tx[phase];tx[phase]=function(...args){const value=fn(...args);if(crash){crash=false;throw Error('SIMULATED_EXECUTION_STOP');}return value;};tx.rollback=()=>{throw Error('SIMULATED_NO_FINALLY');};return tx;};return s;};
 const failed=e.send(q);assert.equal(failed.ok,false);e.context.v3Store=original;
 // Re-entering the real store recovers PENDING before it exposes receipts.
 const restored=e.lock(s=>s.get(p.key));
 // finish() may already have committed the COMPLETE marker: then replay must return the new receipt.
 if(phase==='finish')assert.equal(restored.revision,2);else {assert.equal(restored.revision,1);assert.deepEqual(parts(e,a),old);}
 const b=ok(e.send(q));assert.equal(b.folderId,a.folderId);assert.equal(b.files[0].fileId,a.files[0].fileId);assert.equal(manifests(e).length,1);
 const retry=ok(e.send(q));assert.equal(retry.alreadyExists,true);return {a,failed,b,retry};
});
paired('BUSY con solicitud válida no escribe; intercalación de dos tablets respeta ScriptLock',e=>{
 const p=payload('TEST_BUSY_A'),q=payload('TEST_BUSY_B');e.state.busy=true;const n=e.objects.size;
 const a=e.send(p),b=e.context.stageRecordV3({syncProtocol:3,key:p.key,revision:1,file:p.files[0]});assert.equal(a.code,'BUSY');assert.equal(b.code,'BUSY');assert.equal(e.objects.size,n);
 e.state.busy=false;const update=e.context.Drive.Files.update;let interleaved;
 ok(e.send(p));e.context.Drive.Files.update=(...args)=>{interleaved=e.send(q);return update(...args);};ok(e.send(edit(p)));assert.equal(interleaved.code,'BUSY');e.context.Drive.Files.update=update;
 return {a,b,second:ok(e.send(q))};
});
test('Diferencias intencionales de staging inválido: respuesta bajo BUSY y key no textual',()=>{
 const results=[];
 for(const v of variants){const e=environment(v),p=payload(),req={syncProtocol:3,key:p.key,revision:1,file:{...p.files[0],sha256:'f'.repeat(64)}};e.state.busy=true;
  const busy=e.context.stageRecordV3(req);e.state.busy=false;const malformed=e.context.stageRecordV3({...req,file:{...p.files[0],key:123}});
  assert.equal(busy.code,v==='v20'?'BUSY':'HASH_OR_FILE_SIZE');assert.equal(malformed.code,v==='v20'?'STAGE_COMMUNICATION_ERROR':'INVALID_STAGE');
  results.push({variant:v,busy,malformed});
 }
 fs.writeFileSync(path.join(evidence,'diferencias-respuestas-invalidas.json'),JSON.stringify(results,null,2));
});
test('Instrumentación solo numérica, fallo del logger no modifica ACK',()=>{
 const e=environment('instrumentado'),p=payload();const a=ok(e.send(p));assert.ok(e.logs.some(x=>x.operation==='commit'&&x.phases.unpackWorkbook));
 for(const event of e.logs){assert.deepEqual(Object.keys(event).sort(),['acquired','event','heldMs','operation','phases','prepareMs','waitMs'].sort());assert.ok(!JSON.stringify(event).includes(p.recordId));}
 e.context.console.info=()=>{throw Error('LOGGER_FAILURE');};assert.equal(ok(e.send(p)).alreadyExists,true);assert.equal(a.contentHash,hash(e.context.SyncV3.canonical(p)));
});
paired('Video 10H sobre padre real simulado: reserva, pérdida de ACK, checksum y video histórico',e=>{
 const p=bridge('TEST_VIDEO_PARENT');reserve(e,p);ok(e.send(p));
 const binaries=new Map();let seq=0,sessions=0;
 e.context.Drive.Files.generateIds=()=>({ids:['binary-'+(++seq)]});
 e.context.Drive.Files.get=id=>{if(!binaries.has(id))throw Error('404 File not found');return binaries.get(id);};
 e.context.Drive.Files.create=meta=>{binaries.set(meta.id,{...meta,size:0});return {id:meta.id};};
 e.context.ScriptApp={getOAuthToken:()=>'SYNTHETIC_TOKEN'};
 e.context.UrlFetchApp={fetch:()=>{sessions++;return {getResponseCode:()=>200,getAllHeaders:()=>({Location:'https://www.googleapis.com/upload/drive/v3/files/test?upload_id=TEST_ONLY'})};}};
 const req={action:'beginVideoV1',videoProtocol:1,videoNamingVersion:2,parentKey:p.key,parentRevision:1,mediaId:'TEST_VIDEO_1',category:'UPSTREAM',categoryLabel:'Aguas arriba',mimeType:'video/mp4',sizeBytes:123,sha256:hash('TEST VIDEO BYTES')};
 const a=ok(e.context.videoV1Action(req)),b=ok(e.context.videoV1Action(req));assert.equal(a.fileId,b.fileId);assert.equal(a.fileName,'001_PUE1_ARR.mp4');assert.equal(sessions,1);
 const f=binaries.get(a.fileId);f.size=req.sizeBytes;f.sha256Checksum='0'.repeat(64);assert.equal(e.context.videoV1Action({...req,action:'finishVideoV1',fileId:a.fileId}).code,'VIDEO_HASH_OR_SIZE_MISMATCH');
 f.sha256Checksum=req.sha256;const ack=ok(e.context.videoV1Action({...req,action:'finishVideoV1',fileId:a.fileId}));assert.equal(ack.complete,true);const replay=ok(e.context.videoV1Action(req));assert.equal(replay.complete,true);assert.equal(binaries.size,1);
 const legacy={...req,mediaId:'TEST_LEGACY_VIDEO'};delete legacy.videoNamingVersion;const old=ok(e.context.videoV1Action(legacy));const historical=ok(e.context.videoV1Action({...legacy,videoNamingVersion:2}));assert.equal(historical.fileId,old.fileId);assert.equal(historical.videoNamingVersion,1);assert.equal(historical.historicalName,true);
 return {a,b,ack,replay,old,historical};
});
test('Codigo V20: autenticación por propiedad existente, capacidades y despacho conservados sin secretos reales',()=>{
 const calls=[],context=vm.createContext({PropertiesService:{getScriptProperties:()=>({getProperty:name=>{calls.push(name);return name==='GBO_DRIVE_API_TOKEN'?'TEST_TOKEN':'true';}})},Drive:{},ContentService:{MimeType:{JSON:'JSON'},createTextOutput:s=>({setMimeType:()=>JSON.parse(s)})}});
 for(const name of ['SyncV3Core','SyncV3','PhotoNamesV2Core','SicObservationsV1Core','VideoV1Core','VideoNamesV2Core','VideoV1'])vm.runInContext(fs.readFileSync(path.join(INPUT,'originales-v20',name+'.gs'),'utf8'),context);
 let code=fs.readFileSync(path.join(INPUT,'originales-v20/Codigo.gs'),'utf8');code=code.replace(/const ROOT_FOLDER_ID\s*=\s*[^;]+;/,"const ROOT_FOLDER_ID='TEST_ROOT';");vm.runInContext(code,context);
 const cap=context.doGet();for(const [k,v] of Object.entries({syncProtocol:3,pathVersion:4,readablePhotoNamingVersion:2,videoProtocol:1,videoScope:'SCAP_ONLY',readableVideoNamingVersion:2,sicObservationsVersion:1}))assert.equal(cap[k],v,k);
 assert.ok(calls.includes('GBO_DRIVE_API_TOKEN'));const send=p=>context.doPost({postData:{contents:JSON.stringify(p)}});
 assert.equal(send({token:'WRONG',action:'commitRecordV3'}).ok,false);
 for(const [action,fn] of [['commitRecordV3','commitRecordV3'],['stageRecordV3','stageRecordV3'],['reservePhotoNamesV2','reservePhotoNamesV2'],['beginVideoV1','videoV1Action'],['finishVideoV1','videoV1Action']]){context[fn]=p=>({ok:true,testAction:p.action});assert.equal(send({token:'TEST_TOKEN',action}).testAction,action);}
});
paired('workbook enviado por el cliente se reemplaza por el validado; SHA inválido no obtiene hit',e=>{
 const p=payload('TEST_UNTRUSTED_BOOK');p.sicRows[0].workbook={rows:['FAKE ROW'],signature:'FAKE',sheet:'FAKE'};
 const a=ok(e.send(p));assert.ok(!sheet(e,a).includes('FAKE ROW'));
 const q=edit(p);q.sicRows[0].sha256='f'.repeat(64);assert.equal(e.send(q).code,'HASH_MISMATCH');return {a,parts:parts(e,a)};
});
paired('Staging ajeno y bytes PNG con MIME JPEG no producen ACK definitivo',e=>{
 const p=payload('TEST_STAGE_OWNER'),q=payload('TEST_STAGE_OTHER'),f=p.files[0],a=ok(e.context.stageRecordV3({syncProtocol:3,key:p.key,revision:1,file:f}));
 q.files[0]={...clone(f),stageFileId:a.stageFileId};delete q.files[0].base64;assert.equal(e.send(q).code,'STAGE_OWNERSHIP_CONFLICT');
 const bad=clone(p),bytes=Buffer.from([137,80,78,71,13,10,26,10]);Object.assign(bad.files[0],{base64:bytes.toString('base64'),sha256:hash(bytes)});
 assert.equal(e.send(bad).code,'NOT_JPEG');assert.equal(manifests(e).length,0);return {a};
});
