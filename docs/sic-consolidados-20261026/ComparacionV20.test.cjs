const {test}=require('node:test'),assert=require('node:assert/strict');
const fs=require('node:fs'),path=require('node:path'),vm=require('node:vm');
const root=path.resolve(__dirname,'../..');
const {environment}=require(path.join(root,'google-apps-script/DriveV4Harness.cjs'));
const original=path.join(root,'docs/backend-google-forced-busy-20261017/paquete/FUENTES_V20_REFERENCIA_NO_IMPORTAR');
function env(){
  const e=environment(path.join(original,'SyncV3.gs.txt'));
  for(const file of ['SyncV3Core','SicObservationsV1Core'])vm.runInContext(fs.readFileSync(path.join(original,file+'.gs.txt'),'utf8'),e.context);
  return e;
}
function parts(e,bytes){return new Map(e.context.Utilities.unzip(e.context.Utilities.newBlob([...bytes],'application/zip','ficticio.xlsx')).map(b=>[b.getName(),Buffer.from(b.getBytes())]));}
test('actual V20 merge/pack matches local route XLSX parts for SIC-17, SIC-17A and SIC-17B',()=>{
  for(const format of ['SIC-17','SIC-17A','SIC-17B']) {
    const e=env(),lock=e.context.LockService.getScriptLock();assert.equal(lock.tryLock(30000),true);
    try {
      const local=fs.readFileSync(path.join(__dirname,'evidencias',format+'_TRES_PUENTES_FICTICIOS.xlsx'));
      const source=parts(e,local),sheet=source.get('xl/worksheets/sheet1.xml').toString();
      const all=[...sheet.matchAll(/<row\b[^>]*>[\s\S]*?<\/row>/g)].map(m=>m[0]);
      const header=all.filter(r=>Number(/\br="(\d+)"/.exec(r)[1])<14).join('');
      const rows=all.filter(r=>Number(/\br="(\d+)"/.exec(r)[1])>=14);assert.equal(rows.length,3);
      const store=e.context.v3Store();
      const books=rows.map(row=>{
        const data=row.replace(/\br="\d+"/,'r="14"').replace(/\br="([A-Z]+)\d+"/g,(_,c)=>`r="${c}14"`);
        const xml=sheet.replace(/<sheetData>[\s\S]*?<\/sheetData>/,'<sheetData>'+header+data+'</sheetData>');
        const zip=e.context.Utilities.zip([...source].map(([name,bytes])=>e.context.Utilities.newBlob(name==='xl/worksheets/sheet1.xml'?xml:[...bytes],'application/xml',name)));
        const book=store.unpackWorkbook(zip.getBytes(),14);assert.equal(book.rows.length,1);return book;
      });
      assert.ok(books.every(b=>b.signature===books[0].signature));
      const remote=parts(e,Buffer.from(store.packWorkbook(books[0],e.context.SyncV3.mergeSheet(books[0].sheet,books.map(b=>b.rows[0]),14),3)));
      assert.deepEqual([...remote.keys()].sort(),[...source.keys()].sort());
      for(const [name,bytes] of source)assert.deepEqual(remote.get(name),bytes,format+' '+name);
    }finally{lock.releaseLock();}
  }
});
test('V20 rejects unsupported data-row formulas rather than silently shifting references',()=>{
  const e=env();assert.throws(()=>e.context.SyncV3.mergeSheet('<worksheet><sheetData><row r="13"></row></sheetData></worksheet>',
    ['<row r="14"><c r="A14"><f>A13+1</f><v>1</v></c></row>']),x=>x.code==='UNSUPPORTED_ROW_FORMULA');
});
test('V20 contractual route aggregate scope and stable folder numbers remain independent of row order',()=>{
  const e=env();assert.equal(e.context.SyncV3.aggregateScope({pathVersion:4,routeCode:'PE-28H'}),'PE-28H/EXCEL_SIC');
  const bindings=[{pathVersion:4,identity:'FICTICIO_B',scope:'PE-28H/PUENTES',family:'PUENTE',sequence:7}];
  assert.equal(e.context.SyncV3.allocateNumber('PE-28H/PUENTES','PUENTE','FICTICIO_B',bindings),7);
  const rows=[{recordId:'PR_LOW',roadbedCode:'CI',direction:'INCREASING',startPrNumber:1,startDistanceM:0},{recordId:'PR_HIGH',roadbedCode:'CD',direction:'INCREASING',startPrNumber:9,startDistanceM:0}];
  rows.sort(e.context.SyncV3.compareTechnical);assert.equal(rows[0].recordId,'PR_HIGH');
});
