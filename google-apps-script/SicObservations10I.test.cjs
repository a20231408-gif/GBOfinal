const {test}=require('node:test'),assert=require('node:assert/strict'),crypto=require('node:crypto');
const obs=require('./SicObservationsV1Core.js'),core=require('./SyncV3Core.js');
const hash=b=>crypto.createHash('sha256').update(b).digest('hex');
const cell=(c,row,t)=>`<c r="${c}${row}" t="inlineStr"><is><t xml:space="preserve">${t}</t></is></c>`;
function sheet(note,body='UUID_A') {
  const has=note!==undefined;
  return `<worksheet><dimension ref="A1:${has?'C':'B'}14"/><cols><col min="1" max="1" width="14"/><col min="2" max="2" width="14"/>${has?'<col min="3" max="3" width="45"/>':''}</cols><sheetData><row r="13">${cell('A',13,'IDENTIDAD')}${cell('B',13,'MEDIDA')}${has?cell('C',13,'OBSERVACIONES'):''}</row><row r="14">${cell('A',14,body)}${cell('B',14,'12.5')}${has?cell('C',14,note):''}</row></sheetData></worksheet>`;
}
function book(note,body='UUID_A') {
  const xml=sheet(note,body),i=obs.inspect(xml);
  return {sheet:xml,rows:xml.match(/<row r="14">[\s\S]*?<\/row>/g),signature:hash(i.header+i.columns),dataStartRow:14,observationColumn:i.observationColumn};
}
test('Trailing observations reconcile with the exact previous header and columns only',()=>{
  const old=book(),now=book('PINTADO OK 57 E2');assert.equal(old.signature,now.signature);assert.equal(now.observationColumn,'C');assert.equal(obs.select([old,now]).sheet,now.sheet);
  assert.throws(()=>obs.select([old,{...now,signature:'foreign'}]),/OBSERVATIONS_TEMPLATE_CONFLICT/);
});
test('A preexisting last header outside formatted range is reused without duplication',()=>{
  const outside=sheet('text').replace('<col min="3" max="3" width="45"/>','');
  const i=obs.inspect(outside);assert.equal(i.observationColumn,'C');assert.equal(hash(i.header+i.columns),book().signature);
  assert.equal((outside.match(/OBSERVACIONES/g)||[]).length,1);
});
test('Multiple UUID rows retain their own literal note including empty observations',()=>{
  const a=book('PINTADO OK 57 E2','UUID_A'),b=book('','UUID_B');
  const merged=core.mergeSheet(obs.select([a,b]).sheet,[a.rows[0],b.rows[0]],14);
  assert.match(merged,/r="C14"[^>]*>[\s\S]*?PINTADO OK 57 E2/);assert.match(merged,/r="A15"[^>]*>[\s\S]*?UUID_B/);
  assert.equal((merged.match(/PINTADO OK 57 E2/g)||[]).length,1);assert.ok(!merged.includes('<f'));
});
test('Legacy client updating the same UUID retains its confirmed note and new ordinary cells',()=>{
  const previous=book('PINTADO OK 57 E2','UUID_A'),legacy=book(undefined,'UUID_A_EDIT');
  const inherited=obs.inherit(legacy,previous);
  assert.match(inherited.rows[0],/UUID_A_EDIT/);assert.match(inherited.rows[0],/PINTADO OK 57 E2/);
  // An explicit new-client empty cell is an intentional edit and must stay empty.
  const cleared=book('','UUID_A_EDIT');assert.equal(obs.inherit(cleared,previous),cleared);assert.ok(!cleared.rows[0].includes('PINTADO OK'));
});
test('Unrelated columns, duplicate headers and displaced observations cannot be reconciled',()=>{
  for(const xml of [sheet('x').replace('OBSERVACIONES','OBSERVACIONES</t></is></c>'+cell('D',13,'OTRO')+'<c><is><t>'),
    sheet('x').replace(cell('B',13,'MEDIDA'),cell('B',13,'OBSERVACIONES')),
    sheet('x').replace('r="C13"','r="D13"')])assert.throws(()=>obs.inspect(xml),/OBSERVATIONS_TEMPLATE_CONFLICT/);
});
test('Other pavement header layouts are not changed by the SIC observation module',()=>{
  assert.equal(obs.inspect(sheet('x'),3).observationColumn,null);
});
test('An existing observation header outside print/table extent is integrated into the final range',()=>{
  const b=book('x');b.sheet=b.sheet.replace('<col min="3" max="3" width="45"/>','').replace('ref="A1:C14"','ref="A1:B14"');
  const ready=obs.select([b]);assert.match(ready.sheet,/ref="A1:C14"/);assert.match(ready.sheet,/<col min="3" max="3"/);
  assert.equal(obs.printExtent("SIC!$A$5:$B$14",'C'),"SIC!$A$5:$C$14");
  assert.equal(obs.printExtent("SIC!$A$5:$D$14",'C'),"SIC!$A$5:$D$14");
});
test('Real V3/V4 commit engine upserts UUIDs, survives repeated legacy edits and lost ACK without erasing notes',()=>{
  global.SicObservationsV1=obs;
  const records=new Map();let finalXml='',writes=0;
  const unpack=bytes=>{const xml=bytes.toString(),i=obs.inspect(xml);return {sheet:xml,rows:xml.match(/<row r="14">[\s\S]*?<\/row>/g),signature:hash(i.header+i.columns),dataStartRow:14,observationColumn:i.observationColumn};};
  const store={decode:s=>Buffer.from(s,'base64'),hash,get:k=>records.get(k),all:()=>[...records.values()],unpackWorkbook:unpack,
    packWorkbook:(_,xml)=>Buffer.from(xml),begin:()=>({folderId:'fixed-folder',logicalPath:'TEST10I/ALCANTARILLAS/ALCANTARILLA 1',
      file:f=>({key:f.key,fileId:'metadata-fixed',sha256:f.sha256}),
      aggregate:f=>{writes++;finalXml=f.bytes.toString();return {fileId:'aggregate-fixed',recordIds:f.recordIds};},
      save:r=>records.set(r.key,JSON.parse(JSON.stringify(r))),finish:()=>{},rollback:()=>{}})};
  function payload(id,rev,base,note,body=id) {
    const bytes=Buffer.from(sheet(note,body)),meta=Buffer.from('{}');
    const p={syncProtocol:3,pathVersion:4,kind:'SIC',key:'SIC/'+id,recordId:id,identityId:id,revision:rev,baseRevision:base,
      routeCode:'TEST10I',family:'ALCANTARILLA',familyPath:'ALCANTARILLAS',orderAxis:id==='A'?1:2,roadbedCode:'UC',direction:'INCREASING',startPrNumber:1,startDistanceM:id==='A'?1:2,
      files:[{key:'metadata',path:'metadata.json',mimeType:'application/json',base64:meta.toString('base64'),sha256:hash(meta)}],
      sicRows:[{recordId:id,format:'SIC-18',orderAxis:id==='A'?1:2,startPrNumber:1,startDistanceM:id==='A'?1:2,roadbedCode:'UC',direction:'INCREASING',sha256:hash(bytes),base64:bytes.toString('base64')}]};
    p.contentHash=hash(core.canonical(p));return p;
  }
  try {
    const a=core.commit(payload('A',1,0,'PINTADO OK 57 E2'),store);assert.equal(a.ok,true);
    core.commit(payload('B',1,0,undefined),store);assert.match(finalXml,/PINTADO OK 57 E2/);assert.equal(records.size,2);
    core.commit(payload('A',2,1,undefined,'A_EDIT'),store);assert.match(finalXml,/A_EDIT/);assert.match(finalXml,/PINTADO OK 57 E2/);
    assert.ok(records.get('SIC/A').sicRows[0].observationsTemplateBase64);
    core.commit(payload('B',2,1,undefined,'B_EDIT'),store);assert.match(finalXml,/PINTADO OK 57 E2/);
    const p=payload('A',3,2,undefined,'A_EDIT_2');core.commit(p,store);const count=writes;
    assert.equal(core.commit(p,store).alreadyExists,true);assert.equal(writes,count);assert.match(finalXml,/PINTADO OK 57 E2/);
    core.commit(payload('A',4,3,''),store);assert.ok(!finalXml.includes('PINTADO OK'));assert.equal(records.size,2);
    assert.equal(a.sicFiles[0].fileId,'aggregate-fixed');
  } finally {delete global.SicObservationsV1;}
});
