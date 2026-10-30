// Fake Drive only; never connects to Google. Exercises the packaged files, not a deployed script.
const {test}=require('node:test'),assert=require('node:assert/strict'),fs=require('node:fs'),path=require('node:path'),vm=require('node:vm');
const root=path.resolve(__dirname,'../..'),pkg=path.resolve(__dirname,'..');
const {environment,hash}=require(root+'/google-apps-script/DriveV4Harness.cjs');
const adapter=pkg+'/candidato-no-publicar/SyncV3.gs',engine=pkg+'/referencia-no-reemplazar/SyncV3Core.gs';
const read=p=>fs.readFileSync(p,'utf8');
function env(withObservations=true) {
 const e=environment(adapter);vm.runInContext(read(engine),e.context);
 if(withObservations)vm.runInContext(read(root+'/google-apps-script/SicObservationsV1Core.js'),e.context);
 return e;
}
function payload(id,pr=10,note) {
 const p=JSON.parse(read(root+'/docs/drive-familias-2a/examples/culvert-a/payload.json'));
 p.recordId=id;p.key='SIC/'+id;p.revision=1;p.baseRevision=0;p.startPrNumber=pr;p.startDistanceM=0;p.orderAxis=pr*1000;
 for(const row of p.sicRows)Object.assign(row,{recordId:id,startPrNumber:pr,startDistanceM:0,orderAxis:pr*1000});
 return p;
}
function annotated(e,p,text) {
 const U=e.context.Utilities;
 for(const row of p.sicRows) {
   const parts=U.unzip(U.newBlob([...Buffer.from(row.base64,'base64')]));
   const out=parts.map(b=>{
     if(b.getName()!=='xl/worksheets/sheet1.xml')return b;
     let sheet=b.getDataAsString();
     const cell=(at,value)=>`<c r="${at}" s="3" t="inlineStr"><is><t xml:space="preserve">${value.replace(/&/g,'&amp;').replace(/</g,'&lt;')}</t></is></c>`;
     sheet=sheet.replace(/(<row\b[^>]*r="13"[^>]*>)([\s\S]*?)(<\/row>)/,(_,a,b,z)=>a+b+cell('N13','OBSERVACIONES')+z)
       .replace(/(<row\b[^>]*r="14"[^>]*>)([\s\S]*?)(<\/row>)/,(_,a,b,z)=>a+b+cell('N14',text)+z)
       .replace(/(<dimension ref="[^"]*:)[A-Z]+14"/, '$1N14"');
     return U.newBlob(sheet,'application/xml',b.getName());
   });
   const bytes=Buffer.from(U.zip(out).getBytes());row.base64=bytes.toString('base64');row.sha256=hash(bytes);
 }
 return p;
}
function xml(e,a){return e.context.Utilities.unzip(e.objects.get(a.sicFiles[0].fileId).getBlob()).find(b=>b.getName()==='xl/worksheets/sheet1.xml').getDataAsString();}
test('El motor empaquetado es idéntico al local/10K y ambos .gs compilan como JavaScript V8',()=>{
 assert.equal(read(engine),read(root+'/google-apps-script/SyncV3Core.js'));
 assert.equal(read(engine),read(root+'/docs/parche-apps-script-10k/SyncV3Core.gs'));
 new vm.Script(read(engine));new vm.Script(read(adapter));
 assert.match(read(adapter),/lock\.tryLock\(30000\)/);
});
test('Adaptador propuesto + Observaciones 10K: mezcla de clientes, texto literal, edición y ACK perdido',()=>{
 const e=env(),a=annotated(e,payload('REVIEW_A',10),' =1+1 & PINTADO OK 57 E2 '),b=payload('REVIEW_B',2);
 const first=e.send(a);assert.equal(first.ok,true,JSON.stringify(first));
 const second=e.send(b);assert.equal(second.ok,true,JSON.stringify(second));
 let sheet=xml(e,second);assert.ok(sheet.includes(' =1+1 &amp; PINTADO OK 57 E2 '));assert.ok(!/<c r="N1[345]" s=/.test(sheet));assert.ok(!sheet.includes('<f>'));
 assert.deepEqual([...second.sicFiles[0].recordIds],['REVIEW_B','REVIEW_A']);
 const edit=payload('REVIEW_A',10);edit.revision=2;edit.baseRevision=1;
 const changed=e.send(edit);assert.equal(changed.ok,true,JSON.stringify(changed));assert.ok(xml(e,changed).includes('PINTADO OK 57 E2'));
 const writes=e.updates;assert.equal(e.send(edit).alreadyExists,true);assert.equal(e.updates,writes);
 assert.equal(changed.folderId,first.folderId);assert.equal(changed.sicFiles[0].fileId,first.sicFiles[0].fileId);
 const clear=annotated(e,payload('REVIEW_A',10),'');clear.revision=3;clear.baseRevision=2;
 const ack=e.send(clear);assert.equal(ack.ok,true,JSON.stringify(ack));assert.ok(!xml(e,ack).includes('PINTADO OK'));
});
test('Una capacidad Observaciones=1 no acredita el método plain requerido: módulo anterior bloquea el candidato',()=>{
 const e=env();vm.runInContext('SicObservationsV1.plain=undefined',e.context);
 assert.equal(e.send(payload('REVIEW_OLD_OBS')).ok,false);
 assert.equal([...e.objects.values()].filter(x=>x.name==='SIC-18.xlsx').length,0);
});
test('Video 10H puede consultar el padre confirmado mediante v3Store() sin parámetro',()=>{
 const e=env(),p=payload('REVIEW_SCAP');Object.assign(p,{kind:'SCAP',key:'SCAP/REVIEW_SCAP',identityId:'REVIEW_BRIDGE',family:'PUENTE',familyPath:'PUENTES',sicRows:[]});
 p.files=p.files.filter(f=>f.key==='metadata');p.files[0].path='SCAP/REVIEW_SCAP/manifest.json';
 const ack=e.send(p);assert.equal(ack.ok,true,JSON.stringify(ack));
 for(const file of ['VideoNamesV2Core.js','VideoV1Core.js','VideoV1.gs'])vm.runInContext(read(root+'/google-apps-script/'+file),e.context);
 const lock=e.context.LockService.getScriptLock();assert.equal(lock.tryLock(1),true);
 try {const parent=e.context.videoV1Store().parent(p.key);assert.equal(parent.receipt.folderId,ack.folderId);assert.equal(parent.revision,p.revision);}
 finally {lock.releaseLock();}
});
test('Staging, reserva V2 y vídeo comparten ScriptLock y BUSY no crea objetos',()=>{
 const e=env(),p=payload('REVIEW_SHARED_LOCK');for(const file of ['VideoNamesV2Core.js','VideoV1Core.js','VideoV1.gs'])vm.runInContext(read(root+'/google-apps-script/'+file),e.context);
 const lock=e.context.LockService.getScriptLock();lock.tryLock(1);const n=e.objects.size;
 try {
  assert.equal(e.context.stageRecordV3({syncProtocol:3,key:p.key,revision:1,file:p.files[0]}).code,'BUSY');
  assert.equal(e.context.reservePhotoNamesV2({...p,photos:[]}).code,'BUSY');
  assert.equal(e.context.videoV1Action({action:'beginVideoV1'}).code,'BUSY');
  assert.equal(e.objects.size,n);
 }finally {lock.releaseLock();}
});
test('Un fallo del logger no cambia ACK ni deja el lock adquirido',()=>{
 const e=env();e.context.console={info(){throw Error('synthetic logger failure');}};
 const p=payload('REVIEW_LOGGER'),a=e.send(p);assert.equal(a.ok,true,JSON.stringify(a));assert.equal(e.send(p).alreadyExists,true);
 const lock=e.context.LockService.getScriptLock();assert.equal(lock.tryLock(1),true);lock.releaseLock();
});
test('El costo XLSX no se reduce en este parche: cinco commits, veinte unzip y cinco zip en ambos adaptadores',()=>{
 const counts=[];
 for(const a of [undefined,adapter]) {
  const e=environment(a);let unzip=0,zip=0;const U=e.context.Utilities,un=U.unzip,z=U.zip;
  U.unzip=(...args)=>{unzip++;return un(...args);};U.zip=(...args)=>{zip++;return z(...args);};
  for(let i=1;i<=5;i++){const ack=e.send(payload('REVIEW_COST_'+i,i));assert.equal(ack.ok,true,JSON.stringify(ack));}
  assert.equal(unzip,20);assert.equal(zip,5);counts.push({adapter:a?'packaged':'local',unzip,zip});
 }
 fs.writeFileSync(pkg+'/evidencia/costo-xlsx-medido.json',JSON.stringify({environment:'LOCAL DOUBLES; CALL COUNTS, NOT GOOGLE LATENCY',rowsPerCommit:1,commits:5,counts,explanation:'validate incoming row n + consolidate all rows n(n+1)/2 = 20 unzip; no XLSX acceleration implemented'},null,2));
});
