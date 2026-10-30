'use strict';
const {test}=require('node:test'),assert=require('node:assert/strict'),fs=require('node:fs'),path=require('node:path');
const {env,sha,ROOT}=require('./MockActual.cjs');
const lab=path.join(__dirname,'../PROYECTO_AISLADO_SOLO_PRUEBAS');
function fresh(options={}){const e=env('LAB',options);for(const k of Object.keys(e.props))delete e.props[k];return e;}
function run(e,name){e.switchVariant('LAB');return e.context[name]();}
function setup(){const e=fresh();assert.equal(run(e,'SCAP_00_PREPARAR_PROYECTO').status,'PROYECTO_AISLADO_PREPARADO');return e;}
function fixture(e){const bytes=fs.readFileSync(path.join(__dirname,'../SCAP54_FIXTURE_INPUT.json'));e.root.createFile(e.blob(bytes,'application/json','SCAP54_FIXTURE_INPUT.json'));}
function stage(e){let a;for(let n=0;n<6;n++){a=run(e,'SCAP_03_SUBIR_LOTE');if(a.status==='STAGING_COMPLETO')return a;}assert.fail('staging did not finish');}
function verify(e){let a;for(let n=0;n<6;n++){a=run(e,'SCAP_08_VERIFICAR_LOTE');if(a.status==='VERIFICADO')return a;}assert.fail('verification did not finish');}

test('editor test project refuses wrong root, existing properties, or wrong root name before any writes',()=>{
  const a=fresh({badRoot:true});assert.throws(()=>a.context.SCAP_00_PREPARAR_PROYECTO(),/TEST_ROOT_OR_VARIANT/);assert.equal(a.writes.length,0);assert.deepEqual(a.props,{});
  const b=fresh();b.props.PRODUCTION_MARKER='not a secret';assert.throws(()=>b.context.SCAP_00_PREPARAR_PROYECTO(),/TEST_PROJECT_NOT_EMPTY/);assert.equal(b.writes.length,0);
  const c=fresh();c.root.name='WRONG';assert.throws(()=>c.context.SCAP_00_PREPARAR_PROYECTO(),/TEST_ROOT_NAME/);assert.equal(c.writes.length,0);assert.deepEqual(c.props,{});
});

test('guards reject foreign properties, foreign IDs, stale envelope, shortcuts and read-only writes',()=>{
  const e=setup();e.switchVariant('LAB');const ctx=e.context,original=e.writes.length;e.props.PRODUCTION_MARKER='test';assert.throws(()=>ctx.gboTestConfig_(),/TEST_FOREIGN_PROPERTIES/);delete e.props.PRODUCTION_MARKER;
  assert.throws(()=>ctx.GboTestDriveApp.getFileById(e.foreign.id),/TEST_OUTSIDE_RUN/);assert.equal(e.writes.length,original);
  assert.equal(e.post({action:'stageRecordV3',key:'SCAP/TEST_X',revision:1,file:{}},{...e.run,epoch:'STALE'}).code,'TEST_STALE_RUN');assert.equal(e.writes.length,original);
  const runFolder=e.objects.get(e.run.folderId),f=runFolder.createFile(e.blob('x','text/plain','x.txt'));f.shortcutDetails={targetId:e.foreign.id};
  assert.throws(()=>ctx.GboTestDriveApp.getFileById(f.id),/TEST_UNSAFE_REFERENCE/);delete f.shortcutDetails;
  const guarded=ctx.GboTestDriveApp.getFileById(f.id),count=e.writes.length;ctx.gboTestGuard_.readOnly=true;assert.throws(()=>guarded.setContent('x'),/TEST_READ_ONLY/);assert.equal(e.writes.length,count);
});

test('write guard revalidates ancestry after a previously valid read and rejects a moved target',()=>{
  const e=setup(),c=e.context,f=e.objects.get(e.run.folderId).createFile(e.blob('x','text/plain','x.txt'));c.gboTestGuard_.held=true;
  const wrapped=c.GboTestDriveApp.getFileById(f.id);wrapped.getBlob();f.moveTo(e.outside);const count=e.writes.length;
  assert.throws(()=>wrapped.setContent('bad'),/TEST_OUTSIDE_RUN/);assert.equal(e.writes.length,count);assert.equal(f.getBlob().getDataAsString(),'x');
});

test('editor setup is idempotent and the real doGet body preserves existing capability fields',()=>{
  const e=setup(),runId=e.props.GBO_TEST_RUN,count=e.writes.length,token=e.props.GBO_TEST_TOKEN;assert.equal(run(e,'SCAP_00_PREPARAR_PROYECTO').status,'YA_PREPARADO_SIN_REINICIAR');
  assert.equal(e.props.GBO_TEST_RUN,runId);assert.equal(e.props.GBO_TEST_TOKEN,token);assert.equal(e.writes.length,count);assert.match(token,/^[a-f0-9]{64}$/);
  assert.equal(run(e,'SCAP_01_CAPACIDADES').status,'CAPACIDADES_PASS');
  const current=JSON.parse(env('ACTUAL').context.doGet().getContent()),integrated=JSON.parse(env('INTEGRADO').context.doGet().getContent());
  delete integrated.scapPhotoPanelVersion;assert.deepEqual(integrated,current);
});

test('full integrated editor workflow verifies 59 files, 3 SIC books, dropped receipt, revision rollback and stable IDs in local Google mocks',()=>{
  const e=setup();fixture(e);assert.equal(run(e,'SCAP_01_CAPACIDADES').status,'CAPACIDADES_PASS');assert.equal(run(e,'SCAP_02_CARGAR_Y_RESERVAR').status,'PREPARADO');
  assert.equal(stage(e).staged,59);assert.equal(run(e,'SCAP_04_COMMIT_DESCARTAR_RESPUESTA').status,'RESPUESTA_DESCARTADA_INTENCIONALMENTE');
  assert.equal(run(e,'SCAP_05_REINTENTAR_MISMA_REVISION').alreadyExists,true);
  assert.equal(verify(e).sicVerified,3);assert.equal(run(e,'SCAP_06_PREPARAR_REVISION_2').status,'REVISION_2_PREPARADA_SIN_CAMBIAR_IDENTIDAD');
  stage(e);assert.equal(run(e,'SCAP_07_FALLO_Y_RECUPERACION').status,'FALLO_CONTROLADO_Y_ROLLBACK');assert.equal(run(e,'SCAP_07_FALLO_Y_RECUPERACION').status,'REVISION_RECUPERADA_MISMOS_IDS');verify(e);
  const result=run(e,'SCAP_09_RESULTADO_SEGURO');assert.equal(result.status,'PASS');assert.equal(result.inflight,0);assert.equal(result.identityAndFileIdsStable,true);
  const safe=JSON.stringify(result);assert.ok(!safe.includes(e.props.GBO_TEST_TOKEN));assert.ok(!safe.includes(e.run.runId));assert.ok(!safe.includes(e.run.folderId));assert.ok(!safe.includes('https://'));
  assert.ok(e.writes.every(w=>{let f=e.objects.get(w.parent||w.id);for(let n=0;f&&n<32;n++,f=f.parent)if(f.id===ROOT)return true;return false;}),'all fake Drive writes stay inside authorized root');
  const target=process.env.GBO_DRIVER_RESULT;if(target)fs.writeFileSync(target,JSON.stringify({environment:'LOCAL_GOOGLE_MOCK_NOT_GOOGLE',...result},null,2),{flag:'wx'});
});

test('verification cursor advances only after hashes, actual path, parent and duplicate checks all pass',()=>{
  const code=fs.readFileSync(path.join(lab,'ScapEnsayo.gs'),'utf8');
  const body=code.slice(code.indexOf('function SCAP_08_VERIFICAR_LOTE'),code.indexOf('function SCAP_09_RESULTADO_SEGURO'));
  assert.ok(body.indexOf('s.verify++')>body.indexOf('TEST_DUPLICATE_FILE_OR_PARENT'));
  assert.ok(code.includes('TEST_SIC_DUPLICATE_FILE_OR_PARENT'));
});


test('safe result never marks PASS while any request is inflight',()=>{
  const e=setup();e.switchVariant('LAB');const c=e.context,s=c.scapState_();s.verified1=true;s.verified2=true;s.faultInjected=true;s.verify=59;s.phase='VERIFICADO';c.scapWrite_('SCAP_ESTADO.json',s);e.props.GBO_TEST_INFLIGHT='1';
  const result=run(e,'SCAP_09_RESULTADO_SEGURO');assert.equal(result.status,'INCOMPLETE');assert.equal(result.inflight,1);assert.equal(e.props.GBO_TEST_INFLIGHT,'1');
});
