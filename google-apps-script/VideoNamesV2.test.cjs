const {test}=require('node:test'),assert=require('node:assert/strict'),fs=require('node:fs');
const names=require('./VideoNamesV2Core.js'),core=require('./VideoV1Core.js');
const p=(mediaId='v1',category='UPSTREAM',categoryLabel='Aguas arriba',mimeType='video/mp4')=>({videoProtocol:1,parentKey:'SCAP/bridge',mediaId,category,categoryLabel,mimeType,sizeBytes:10,sha256:'a'.repeat(64),parentRevision:1});
test('All real canonical SCAP labels match the checked-in naming catalog',()=>{
  for(const variant of ['slab','modular'])assert.deepEqual(names.catalog.map(x=>x[0]),JSON.parse(fs.readFileSync(`app/src/main/assets/scap_${variant}_catalog.json`,'utf8')).photoNames);
  assert.equal(new Set(names.catalog.map(x=>x[1])).size,names.catalog.length);
  for(const [label,code] of names.catalog) {
    const a=names.reserve(p('v','T_HASH',label),1).naming;
    assert.equal(a.categoryCode,code);assert.ok(a.fileName.length<=20);assert.ok(!a.categoryFolder.startsWith('T_'));
  }
});
test('Numbers are stable per category, shared across tablets and legacy/hash category aliases',()=>{
  const a=names.reserve(p(),1),b=names.reserve(p('v2','T_HASH','AGUAS ARRIBA'),1,a.state);
  const c=names.reserve(p('v3','DOWNSTREAM','Aguas abajo'),1,b.state);
  assert.equal(b.naming.fileName,'002_PUE1_ARR.mp4');assert.equal(c.naming.fileName,'001_PUE1_ABA.mp4');
  assert.deepEqual(names.reserve(p(),1,c.state).naming,a.naming);
  assert.throws(()=>names.reserve({...p(),sha256:'b'.repeat(64)},1,c.state),/VIDEO_NAME_BINDING_CONFLICT/);
});
test('Actual format extension and limit are enforced without partially mutating state',()=>{
  for(const [mime,ext] of [['video/mp4','mp4'],['video/webm','webm'],['video/3gpp','3gp']])assert.equal(names.reserve(p('v','ROAD_DECK','Carretera / tablero',mime),1).naming.fileName,`001_PUE1_TAB.${ext}`);
  const a=names.reserve(p(),1),before=JSON.stringify(a.state);a.state.next.ARR=999;
  assert.throws(()=>names.reserve(p('v2'),1,a.state),/VIDEO_READABLE_NAME_LIMIT/);assert.equal(a.state.next.ARR,999);
  assert.throws(()=>names.reserve(p(),123456789),/VIDEO_READABLE_NAME_LIMIT/);assert.ok(before);
});
test('Edited SCAP labels remain human and colliding short codes cannot collide in filenames',()=>{
  const a=names.reserve(p('v1','T_ONE','Puente prueba'),1),b=names.reserve(p('v2','T_TWO','Pasarela prueba'),1,a.state);
  assert.equal(a.naming.categoryCode,'PP');assert.equal(b.naming.categoryCode,'PP');
  assert.equal(a.naming.fileName,'001_PUE1_PP.mp4');assert.equal(b.naming.fileName,'002_PUE1_PP.mp4');
  assert.equal(a.naming.categoryFolder,'PUENTE_PRUEBA');assert.equal(b.naming.categoryFolder,'PASARELA_PRUEBA');
});
test('Prototype-like media identity is rejected by the real protocol before reservation',()=>{
  for(const id of ['__proto__','constructor','prototype'])assert.throws(()=>core.validate(p(id)),/INVALID_VIDEO/);
});
test('Real SCAP labels containing separators remain metadata, never unvalidated paths',()=>{
  for(const [category,label] of [['ROAD_DECK','Carretera / tablero'],['RAILINGS_SIDEWALKS_PIPES','Barandas / veredas / tuberías']]) {
    core.validate(p('v',category,label));const r=names.reserve(p('v',category,label),1);
    assert.match(r.naming.categoryFolder,/^[A-Z0-9_]+$/);assert.ok(!r.naming.categoryFolder.includes('/'));
  }
  assert.throws(()=>core.validate(p('v','ROAD_DECK','bad\nlabel')),/INVALID_VIDEO/);
});
