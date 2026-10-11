const fs=require('node:fs'), vm=require('node:vm'), assert=require('node:assert/strict'), crypto=require('node:crypto');
const {test}=require('node:test');const {spawnSync}=require('node:child_process');const path=require('node:path');
const core=require('./SyncV3Core.js');
const PY=process.env.GBO_AUDIT_PYTHON || 'C:/Users/gerbc/.cache/codex-runtimes/codex-primary-runtime/dependencies/python/python.exe';
const output=process.env.GBO_SYNC_TEST_OUTPUT || path.resolve(__dirname,'../docs/automatic-sync-v16/evidence');fs.mkdirSync(output,{recursive:true});
const inputRoot=path.resolve(__dirname,'../docs/automatic-sync-v16/examples/inputs');
const fallback=path.resolve(__dirname,'../docs/acceptance-audit-v15-2026-10-10/scenarios/all-sic/SIC-17.xlsx');
function zipParts(mode,data) {
  const script=`import sys,json,base64,zipfile,io\nj=json.load(sys.stdin)\nif j['mode']=='unzip':\n z=zipfile.ZipFile(io.BytesIO(base64.b64decode(j['data'])))\n print(json.dumps([{'name':n,'base64':base64.b64encode(z.read(n)).decode()} for n in z.namelist()]))\nelse:\n b=io.BytesIO()\n with zipfile.ZipFile(b,'w',zipfile.ZIP_DEFLATED) as z:\n  for p in j['data']:z.writestr(p['name'],base64.b64decode(p['base64']))\n print(base64.b64encode(b.getvalue()).decode())`;
  const r=spawnSync(PY,['-c',script],{input:JSON.stringify({mode,data}),encoding:'utf8',maxBuffer:32*1024*1024});assert.equal(r.status,0,r.stderr);return mode==='unzip'?JSON.parse(r.stdout):r.stdout.trim();
}
function environment() {
  let held=false,seq=0,updateCount=0;const objects=new Map();const state={busy:false,failAfter:0};
  const iter=a=>{let i=0;return {hasNext:()=>i<a.length,next:()=>a[i++]};};
  function blob(bytes,mime='application/octet-stream',name='') {
    const b=Buffer.isBuffer(bytes)?bytes:typeof bytes==='string'?Buffer.from(bytes):Buffer.from(bytes);
    return {getBytes:()=>[...b],getContentType:()=>mime,getName:()=>name,getDataAsString:()=>b.toString(),copyBlob:()=>blob(b,mime,name)};
  }
  class Folder {
    constructor(name,parent=null){this.name=name;this.parent=parent;this.id='folder-'+(++seq);this.files=[];this.folders=[];this.description='';objects.set(this.id,this);if(parent)parent.folders.push(this);}
    getId(){return this.id;}getName(){return this.name;}getFiles(){return iter([...this.files]);}getParents(){return iter(this.parent?[this.parent]:[]);}
    getFilesByName(n){return iter(this.files.filter(x=>x.name===n));}getFoldersByName(n){return iter(this.folders.filter(x=>x.name===n));}
    createFolder(n){assert.ok(held);return new Folder(n,this);}createFile(b){assert.ok(held);return new File(b,this);}
    getDescription(){return this.description;}setDescription(d){this.description=d;return this;}setName(n){this.name=n;return this;}
    moveTo(p){if(this.parent)this.parent.folders=this.parent.folders.filter(x=>x!==this);this.parent=p;p.folders.push(this);return this;}
  }
  class File {
    constructor(b,parent){this.b=b;this.name=b.getName();this.parent=parent;this.id='file-'+(++seq);this.description='';objects.set(this.id,this);parent.files.push(this);}
    getId(){return this.id;}getName(){return this.name;}getBlob(){return blob(this.b.getBytes(),this.b.getContentType(),this.name);}
    getParents(){return iter([this.parent]);}getDescription(){return this.description;}setDescription(d){this.description=d;return this;}
    setContent(s){this.b=blob(s,this.b.getContentType(),this.name);return this;}
    makeCopy(n,parent){return new File(blob(this.b.getBytes(),this.b.getContentType(),n),parent);}
    moveTo(p){this.parent.files=this.parent.files.filter(x=>x!==this);this.parent=p;p.files.push(this);return this;}
  }
  const core=require("./SyncV3Core.js");
  const root=new Folder('root');const context=vm.createContext({
    PropertiesService:{getScriptProperties:()=>({getProperty:()=>"true"})},
    DriveApp:{getFolderById:id=>{assert.ok(held);return objects.get(id);},getFileById:id=>{assert.ok(held);return objects.get(id);}},
    Drive:{Files:{update(meta,id,b){assert.ok(held);updateCount++;if(state.failAfter && updateCount===state.failAfter){state.failAfter=0;throw Error('injected network failure');}const f=objects.get(id);assert.ok(f instanceof File);f.b=b;f.name=meta.name;return {id};}}},
    LockService:{getScriptLock:()=>({tryLock(){if(state.busy || held)return false;held=true;return true;},releaseLock(){assert.ok(held);held=false;}})},
    Utilities:{DigestAlgorithm:{SHA_256:'SHA_256'},computeDigest:(_,bytes)=>[...crypto.createHash('sha256').update(typeof bytes==='string'?bytes:Buffer.from(bytes)).digest()],
      base64Decode:s=>[...Buffer.from(s,'base64')],newBlob:blob,
      unzip:b=>zipParts('unzip',Buffer.from(b.getBytes()).toString('base64')).map(p=>blob(Buffer.from(p.base64,'base64'),'application/octet-stream',p.name)),
      zip:parts=>blob(Buffer.from(zipParts('zip',parts.map(b=>({name:b.getName(),base64:Buffer.from(b.getBytes()).toString('base64')}))),'base64'),'application/zip','pack.xlsx')},
    ContentService:{MimeType:{JSON:'json'},createTextOutput:s=>({setMimeType:()=>JSON.parse(s)})}
  });
  vm.runInContext(`const ROOT_FOLDER_ID=${JSON.stringify(root.id)};function jsonResponse(x){return x;}function getOrCreateFolder(p,n){const i=p.getFoldersByName(n);if(i.hasNext()){const f=i.next();if(i.hasNext())throw Error('duplicate folder');return f;}return p.createFolder(n);}`+
    fs.readFileSync(__dirname+'/SyncV3Core.js','utf8')+'\n'+fs.readFileSync(__dirname+'/SyncV3.gs','utf8'),context);
  return {root,objects,state,context,send:p=>context.commitRecordV3(JSON.parse(JSON.stringify({...p,contentHash:hash(core.canonical(p))}))),get updates(){return updateCount;}};
}
const hash=b=>crypto.createHash('sha256').update(b).digest('hex');
const rowFile=(format,letter)=>{const candidate=path.join(inputRoot,letter,format+'.xlsx');return fs.readFileSync(fs.existsSync(candidate)?candidate:fallback);};
function payload(letter='A',revision=1,base=0,axis=100) {
  const bytes=Buffer.from(JSON.stringify({bridge:letter,revision}));const workbook=rowFile('SIC-17',letter);
  return {syncProtocol:3,pathVersion:3,key:'SIC/bridge-'+letter,kind:'SIC',recordId:'bridge-'+letter,revision,baseRevision:base,
    contentHash:hash(bytes),routeCode:'PE-22A',sibCode:'SIB-08',family:'PUENTE',orderAxis:axis,bridgeCode:'GBO-'+letter,
    files:[{key:'metadata',path:'metadata.json',mimeType:'application/json',sha256:hash(bytes),base64:bytes.toString('base64')}],
    sicRows:[{recordId:'bridge-'+letter,format:'SIC-17',orderAxis:axis,sha256:hash(workbook),base64:workbook.toString('base64')}],removeMissingOwnedFiles:true};
}
test('A+B+C, B edited, D from second tablet: four UUID rows and same consolidated fileId',()=>{
  const e=environment();const a=e.send(payload('A',1,0,100));assert.equal(a.ok,true);
  const b=e.send(payload('B',1,0,200));assert.equal(b.ok,true);const c=e.send(payload('C',1,0,300));assert.equal(c.ok,true);
  const edit=payload('B',2,1,200);edit.sicRows[0].base64=rowFile('SIC-17','B_EDIT').toString('base64');edit.sicRows[0].sha256=hash(Buffer.from(edit.sicRows[0].base64,'base64'));
  const before=e.objects.get(b.files[0].fileId).getBlob().getBytes();const revised=e.send(edit);assert.equal(revised.ok,true);assert.equal(revised.files[0].fileId,b.files[0].fileId);
  assert.notDeepEqual(e.objects.get(b.files[0].fileId).getBlob().getBytes(),before);
  const d=e.send(payload('D',1,0,400));assert.equal(d.ok,true);assert.equal(d.sicFiles[0].fileId,a.sicFiles[0].fileId);
  assert.deepEqual([...d.sicFiles[0].recordIds],['bridge-A','bridge-B','bridge-C','bridge-D']);
  fs.writeFileSync(output+'/SIC-17-four-bridges.xlsx',Buffer.from(e.objects.get(d.sicFiles[0].fileId).getBlob().getBytes()));
  fs.writeFileSync(output+'/multi-tablet-receipts.json',JSON.stringify({source:'Apps Script engine + Drive doubles + actual OOXML',a,b,c,revised,d},null,2));
});
test('same revision retry preserves every ID, no duplicated visible files',()=>{const e=environment();const p=payload();const a=e.send(p),b=e.send(p);assert.equal(b.ok,true);assert.equal(b.alreadyExists,true);assert.deepEqual(JSON.parse(JSON.stringify(a.files)),JSON.parse(JSON.stringify(b.files)));});
test('same revision/different bytes rejects without changing previous resource',()=>{const e=environment();const a=e.send(payload());const p=payload();const bytes=Buffer.from('changed content');p.files[0].base64=bytes.toString('base64');p.files[0].sha256=hash(bytes);assert.equal(e.send(p).code,'REVISION_CONFLICT');assert.equal(e.objects.get(a.files[0].fileId).name,'metadata.json');});
test('stale and conflicting base revisions cannot overwrite newer values',()=>{const e=environment();e.send(payload('A',2,0));assert.equal(e.send(payload('A',1,2)).code,'STALE_REVISION');assert.equal(e.send(payload('A',3,0)).code,'BASE_REVISION_CONFLICT');});
test('a busy global lock never writes or releases someone else lock',()=>{const e=environment();e.state.busy=true;assert.equal(e.send(payload()).code,'BUSY');assert.equal(e.objects.size,1);});
test('corrupt file hash rejects before Drive mutations',()=>{const e=environment();const p=payload();p.files[0].sha256='a'.repeat(64);assert.equal(e.send(p).code,'HASH_MISMATCH');assert.equal(e.root.folders.length,1);assert.equal(e.root.folders[0].files.length,0);});
test('PNG mislabeled JPEG rejects before definitive files',()=>{const e=environment();const p=payload();const bytes=Buffer.from([137,80,78,71]);p.files=[{key:'photo/photo-A',path:'GENERAL/001_F01.jpg',mimeType:'image/jpeg',sha256:hash(bytes),base64:bytes.toString('base64')}];assert.equal(e.send(p).code,'NOT_JPEG');});
test('late earlier element waits for explicit ordering reconciliation',()=>{const e=environment();assert.equal(e.send(payload('B',1,0,200)).ok,true);assert.equal(e.send(payload('A',1,0,100)).code,'ORDERING_REQUIRED');});
test('edit cannot silently reverse previously confirmed geometric order',()=>{const e=environment();e.send(payload('A',1,0,100));e.send(payload('B',1,0,200));assert.equal(e.send(payload('A',2,1,250)).code,'ORDERING_REQUIRED');});
test('bridge codes belong to a stable identity, no second UUID collision',()=>{const e=environment();e.send(payload());const p=payload('B',1,0,200);p.bridgeCode='GBO-A';assert.equal(e.send(p).code,'BRIDGE_IDENTITY_CONFLICT');});
test('failed update restores old resource bytes and revision, retry succeeds with same ID',()=>{const e=environment();const first=e.send(payload());const old=Buffer.from(e.objects.get(first.files[0].fileId).getBlob().getBytes());e.state.failAfter=e.updates+2;const p=payload('A',2,1);assert.equal(e.send(p).ok,false);assert.deepEqual(Buffer.from(e.objects.get(first.files[0].fileId).getBlob().getBytes()),old);const next=e.send(p);assert.equal(next.ok,true);assert.equal(next.files[0].fileId,first.files[0].fileId);});
test('photo removal archives only own omitted IDs, unrelated files survive',()=>{const e=environment();const p=payload();p.files.push({...p.files[0],key:'photo/photo-A',path:'GENERAL/001.json'});const a=e.send(p);const folder=e.objects.get(a.folderId);const stranger=folder.createFile; // Add externally without going through delivery ownership.
  e.state.busy=false;const other={id:'foreign',name:'foreign.jpg'};folder.files.push(other);const b=e.send(payload('A',2,1));assert.equal(b.ok,true);assert.ok(folder.files.includes(other));assert.equal(e.objects.get(a.files[1].fileId).parent.name.startsWith('2_'),true);});
test('different routes keep separate consolidated Excel resources',()=>{const e=environment();const a=e.send(payload());const p=payload('C');p.routeCode='PE-3N';const c=e.send(p);assert.equal(c.ok,true);assert.notEqual(a.sicFiles[0].fileId,c.sicFiles[0].fileId);assert.equal(c.sicFiles[0].logicalPath,'PE-3N/SIB-08/SIC-17.xlsx');});
test('route edit relocates own folder and retires old route row without orphan',()=>{const e=environment();const a=e.send(payload());const p=payload('A',2,1);p.routeCode='PE-3N';const b=e.send(p);assert.equal(b.ok,true);assert.equal(b.folderId,a.folderId);assert.equal(e.objects.get(a.folderId).parent.parent.name,'PE-3N');});
test('foreign historical folder is never adopted or renamed silently',()=>{const e=environment();const lock=e.context.LockService.getScriptLock();assert.ok(lock.tryLock(1));
 const route=e.context.getOrCreateFolder(e.root,'PE-22A'),sib=e.context.getOrCreateFolder(route,'SIB-08');sib.createFolder('PUENTE 1');lock.releaseLock();
 assert.equal(e.send(payload()).code,'HISTORICAL_BINDING_REQUIRED');assert.equal(sib.folders.length,1);
});

function noGps(letter='A',axis=100) {
  const p=payload(letter,1,0,axis);p.orderSource='CONTRACTUAL_PR';p.technicalDeliveryWithoutGps=true;p.technicalDeliveryVersion=1;
  p.georeferencing={status:'PENDING_EXTERNAL',externalSubmetricStatus:'PENDING',captureSource:'UNAVAILABLE'};
  p.sicRows.forEach(r=>r.orderSource=p.orderSource);
  const metadata=Buffer.from(JSON.stringify({latitude:null,longitude:null,georeferencing:p.georeferencing}));
  p.files[0].base64=metadata.toString('base64');p.files[0].sha256=hash(metadata);return p;
}
test('technical PR deliveries accept late earlier chainage without renaming existing folders',()=>{
  const e=environment();const b=e.send(noGps('B',200));assert.equal(b.ok,true);
  const a=e.send(noGps('A',100));assert.equal(a.ok,true);assert.notEqual(a.folderId,b.folderId);
  const again=e.send(noGps('B',200));assert.equal(again.folderId,b.folderId);assert.equal(again.logicalPath,b.logicalPath);
  assert.equal(a.technicalDeliveryStatus,'DELIVERED');assert.equal(a.georeferencingStatus,'PENDING_EXTERNAL');
  assert.equal(a.externalGeoreferencingStatus,'PENDING');assert.equal(a.sicFiles[0].fileId,b.sicFiles[0].fileId);
  assert.deepEqual([...a.sicFiles[0].recordIds],['bridge-A','bridge-B']);
});
test('geometric and contractual PR measures remain separate ordering sources',()=>{
  const e=environment();assert.equal(e.send(noGps('A',10000)).ok,true);
  assert.equal(e.send(payload('B',1,0,200)).ok,true);
  const c=e.send(noGps('C',1));assert.equal(c.ok,true);
  assert.deepEqual([...c.sicFiles[0].recordIds],['bridge-B','bridge-C','bridge-A']);
});
test('null GPS metadata and real Android photos/SIC-08/SIC-09 workbooks reach simulated Drive with stable IDs',()=>{
  const e=environment();const receipts=[];
  for(const format of ['SIC-08','SIC-09']) {
    const p=JSON.parse(fs.readFileSync(path.resolve(__dirname,'../docs/sync-sin-gps-2026-10-11/examples',format,'payload.json')));
    assert.equal(p.contentHash,hash(core.canonical(p)),'Android and backend canonical hash must match');
    const metadata=JSON.parse(Buffer.from(p.files.find(f=>f.key==='metadata').base64,'base64'));
    assert.equal(metadata.latitude,null);assert.equal(metadata.longitude,null);
    const a=e.send(p);assert.equal(a.ok,true,JSON.stringify(a));assert.equal(a.logicalPath.startsWith('PE-22A/SIB-02/FAJA '),true);
    assert.equal(a.georeferencingStatus,'PENDING_EXTERNAL');assert.equal(a.externalGeoreferencingStatus,'PENDING');
    const jpg=a.files.find(f=>f.key.startsWith('photo/'));assert.ok(jpg.fileId);assert.ok(jpg.folderId);
    assert.equal(e.objects.get(jpg.fileId).getBlob().getBytes()[0],255);assert.ok(a.sicFiles[0].fileId);
    const again=e.send(p);assert.equal(again.alreadyExists,true);assert.equal(again.folderId,a.folderId);
    assert.deepEqual(JSON.parse(JSON.stringify(again.files)),JSON.parse(JSON.stringify(a.files)));
    assert.equal(again.sicFiles[0].fileId,a.sicFiles[0].fileId);receipts.push({format,first:a,retry:again});
  }
  fs.writeFileSync(path.join(output,'no-gps-simulated-drive-receipts.json'),JSON.stringify({source:'Drive doubles, NOT real remote uploads',receipts},null,2));
});
test('technical hash protects delivery and georeferencing states against alteration',()=>{
  const e=environment();const p=noGps();const commit=JSON.parse(JSON.stringify({...p,contentHash:hash(core.canonical(p))}));
  commit.georeferencing.status='COORDINATES_AVAILABLE';assert.equal(e.context.commitRecordV3(commit).code,'CONTENT_HASH_MISMATCH');
});
test('invalid external georeferencing state is rejected before Drive delivery',()=>{
  const e=environment();const p=noGps();p.georeferencing.externalSubmetricStatus='COMPLETE';
  assert.equal(e.send(p).code,'INVALID_GEOREFERENCING_STATE');
});
