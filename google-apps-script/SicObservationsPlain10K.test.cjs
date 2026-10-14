const test=require('node:test'),assert=require('node:assert/strict');
const obs=require('./SicObservationsV1Core.js');
test('Only trailing observations lose fill borders font number format and filter membership',()=>{
  const a='<c r="A14" s="4"><v>12</v></c>',b='<c r="B14" s="3" t="inlineStr"><is><t>BASE</t></is></c>';
  const note='<c r="C14" s="3" t="inlineStr"><is><t xml:space="preserve"> =1+1 </t></is></c>';
  const sheet=`<worksheet><cols><col min="1" max="1" width="20"/><col min="3" max="3" width="45" style="3"/></cols><sheetData><row r="13"><c r="C13" s="2" t="inlineStr"><is><t>OBSERVACIONES</t></is></c></row><row r="14">${a}${b}${note}</row></sheetData><autoFilter ref="A13:C14"><filterColumn colId="0"><filters><filter val="12"/></filters></filterColumn><filterColumn colId="2"><filters/></filterColumn></autoFilter><mergeCells><mergeCell ref="A1:B4"/></mergeCells></worksheet>`;
  const out=obs.plain(sheet,'C');assert.ok(out.includes(a)&&out.includes(b));
  assert.match(out,/<c r="C13" t="inlineStr"/);assert.match(out,/<c r="C14" t="inlineStr"/);
  assert.ok(out.includes(' =1+1 '));assert.ok(!out.includes('<f>'));
  assert.match(out,/autoFilter ref="A13:B14"/);assert.ok(out.includes('colId="0"'));assert.ok(!out.includes('colId="2"'));
  assert.ok(!out.includes('min="3"'));assert.ok(out.includes('<mergeCell ref="A1:B4"/>'));
  assert.equal(obs.plain(out,'C'),out);
});
test('A separate observations filter is removed and an unrelated contractual filter remains byte identical',()=>{
  assert.equal(obs.plain('<worksheet><autoFilter ref="C13:C20"/></worksheet>','C'),'<worksheet></worksheet>');
  const s='<worksheet><autoFilter ref="A13:B20"/><conditionalFormatting sqref="A14"><cfRule type="expression" priority="1"><formula>A14=1</formula></cfRule></conditionalFormatting></worksheet>';
  assert.equal(obs.plain(s,'C'),s);assert.equal(obs.plain(s,null),s);
});
test('Final merged rows from earlier styled clients are normalized without changing literal text',()=>{
  const sheet='<worksheet><sheetData><row r="14"><c r="A14" s="4"><v>10</v></c><c r="AA14" s="7" t="inlineStr"><is><t>Texto &amp; anterior</t></is></c></row></sheetData><autoFilter ref="A13:AA14"/></worksheet>';
  const out=obs.plain(sheet,'AA');assert.ok(out.includes('<c r="A14" s="4">'));assert.ok(out.includes('<c r="AA14" t="inlineStr">'));
  assert.ok(out.includes('Texto &amp; anterior'));assert.ok(out.includes('ref="A13:Z14"'));
});
