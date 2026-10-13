const fs=require('node:fs'), vm=require('node:vm'), assert=require('node:assert/strict'), crypto=require('node:crypto');
const {test}=require('node:test');const {spawnSync}=require('node:child_process');const path=require('node:path');
const core=require('./SyncV3Core.js');
const PY=process.env.GBO_AUDIT_PYTHON || 'C:/Users/gerbc/.cache/codex-runtimes/codex-primary-runtime/dependencies/python/python.exe';
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
    fs.readFileSync(__dirname+'/PhotoNamesV2Core.js','utf8')+'\n'+fs.readFileSync(__dirname+'/SyncV3Core.js','utf8')+'\n'+fs.readFileSync(__dirname+'/SyncV3.gs','utf8'),context);
  return {root,objects,state,context,send:p=>context.commitRecordV3(JSON.parse(JSON.stringify({...p,contentHash:hash(core.canonical(p))}))),get updates(){return updateCount;}};
}
const hash=b=>crypto.createHash('sha256').update(b).digest('hex');
module.exports={environment,hash};
