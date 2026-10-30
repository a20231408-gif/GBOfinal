// Offline only: the existing Drive/LockService harness never opens HTTP connections.
const {test}=require('node:test'),assert=require('node:assert/strict'),fs=require('node:fs'),vm=require('node:vm'),path=require('node:path');
const {environment,zipParts}=require('../../backend-v20-revision-20261016/pruebas/HarnessV20.cjs');
const {payload,rewrite,annotated,parts}=require('../../backend-v20-revision-20261016/pruebas/CasosV20.cjs');
const source=fs.readFileSync(path.join(__dirname,'SyncV3Core.gs'),'utf8');
function setup(){const e=environment('v20');vm.runInContext(source,e.context);return e;}
function fictitious(id,pr,metres=0,roadbed='CD',direction='INCREASING') {
  const p=payload(id,pr);Object.assign(p,{startDistanceM:metres,roadbedCode:roadbed,direction});
  for(const r of p.sicRows){Object.assign(r,{startDistanceM:metres,roadbedCode:roadbed,direction});rewrite(r,entries=>{
    const sheet=entries.find(f=>f.name==='xl/worksheets/sheet1.xml');let xml=Buffer.from(sheet.base64,'base64').toString();
    for(const [cell,value] of [['B14',roadbed],['C14',String(pr).padStart(4,'0')],['D14',String(metres)]]){
      const rx=new RegExp('<c\\b[^>]*\\br="'+cell+'"[^>]*>[\\s\\S]*?<\\/c>');assert.ok(rx.test(xml),cell);
      xml=xml.replace(rx,match=>{const style=match.match(/\bs="([^"]*)"/);return '<c r="'+cell+'"'+(style?' s="'+style[1]+'"':'')+' t="inlineStr"><is><t>'+value+'</t></is></c>';});
    }
    sheet.base64=Buffer.from(xml).toString('base64');
  });}return p;
}
test('real XLSX ascends by numeric PR/metres across roadbeds/directions; exact ties use UUID',()=>{
  const e=setup();let ack;
  for(const p of [fictitious('TEST_C',12,58),fictitious('TEST_A',0,301,'CI','DECREASING'),fictitious('TEST_E',2,229),fictitious('TEST_B',0,820),fictitious('TEST_D',11,0,'CI','DECREASING')]){
    ack=e.send(p);assert.equal(ack.ok,true,JSON.stringify(ack));
  }
  assert.deepEqual([...ack.sicFiles[0].recordIds],['TEST_A','TEST_B','TEST_E','TEST_D','TEST_C']);
  const entries=parts(e,ack);const xml=Buffer.from(entries.find(x=>x.name==='xl/worksheets/sheet1.xml').base64,'base64').toString();
  const prs=[...xml.matchAll(/<c\b[^>]*r="C(?:14|15|16|17|18)"[^>]*>([\s\S]*?)<\/c>/g)].map(x=>x[1].replace(/<[^>]*>/g,''));
  assert.deepEqual(prs,['0000','0000','0002','0011','0012']);
  const book=e.objects.get(ack.sicFiles[0].fileId);fs.writeFileSync(path.join(__dirname,'SIC-18-CONSOLIDADO-FICTICIO.xlsx'),Buffer.from(book.getBlob().getBytes()));
});
test('A B C D produces A B D C, keeps folder numbers, exact replay ACK and BUSY',()=>{
  const e=setup(),inputs=[fictitious('TEST_A',1),fictitious('TEST_B',2),fictitious('TEST_C',4),fictitious('TEST_D',3)];
  const a=inputs.map(p=>{const r=e.send(p);assert.equal(r.ok,true,JSON.stringify(r));return r;});
  assert.deepEqual([...a[3].sicFiles[0].recordIds],['TEST_A','TEST_B','TEST_D','TEST_C']);
  assert.ok(a[2].logicalPath.endsWith('ALCANTARILLA 3'));assert.ok(a[3].logicalPath.endsWith('ALCANTARILLA 4'));
  const replay=e.send(inputs[2]);assert.equal(replay.alreadyExists,true);assert.equal(replay.folderId,a[2].folderId);
  e.state.busy=true;const count=e.objects.size;assert.equal(e.send(fictitious('TEST_BUSY',5)).code,'BUSY');assert.equal(e.objects.size,count);
});
test('historical observations and a later edit survive rebuilding under the same transaction',()=>{
  const e=setup();let p=annotated(fictitious('TEST_NOTE',2),' =1+1 & TEXTO FICTICIO ');let a=e.send(p);assert.equal(a.ok,true,JSON.stringify(a));
  let q=fictitious('TEST_OTHER',1);a=e.send(q);assert.equal(a.ok,true,JSON.stringify(a));
  const entries=parts(e,a);const xml=Buffer.from(entries.find(x=>x.name==='xl/worksheets/sheet1.xml').base64,'base64').toString();assert.ok(xml.includes('TEXTO FICTICIO'));
  const edited={...p,revision:2,baseRevision:1};a=e.send(edited);assert.equal(a.ok,true,JSON.stringify(a));assert.equal(a.revision,2);
  const replay=e.send(edited);assert.equal(replay.alreadyExists,true);assert.equal(replay.folderId,a.folderId);
});
