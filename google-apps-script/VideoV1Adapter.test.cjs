const {test}=require('node:test'),assert=require('node:assert/strict'),vm=require('node:vm'),fs=require('node:fs'),crypto=require('node:crypto');
const core=require('./VideoV1Core.js');
function environment() {
  const objects=new Map(),files=new Map();let seq=0,held=false,enabled=true,sessionCount=0;const parent={kind:'SCAP',family:'PUENTE',pathVersion:4,revision:20,receipt:{folderId:'parent'}};
  const iterator=a=>{let n=0;return {hasNext:()=>n<a.length,next:()=>a[n++]};};
  const blob=(value,mime,name)=>({getDataAsString:()=>String(value),getName:()=>name,getContentType:()=>mime});
  class Folder {
    constructor(id){this.id=id;this.children=new Map();this.files=[];objects.set(id,this);}
    getId(){return this.id;}getFilesByName(name){return iterator(this.files.filter(x=>x.name===name));}
    createFile(b){assert.ok(held);const f={name:b.getName(),text:b.getDataAsString(),getBlob(){return blob(this.text,'application/json',this.name);},setContent(t){this.text=t;}};this.files.push(f);return f;}
  }
  new Folder('root');new Folder('parent');
  const context=vm.createContext({VideoV1:core,ROOT_FOLDER_ID:'root',v3Enabled:()=>true,
    PropertiesService:{getScriptProperties:()=>({getProperty:()=>String(enabled)})},
    LockService:{getScriptLock:()=>({tryLock(){if(held)return false;held=true;return true;},releaseLock(){held=false;}})},
    DriveApp:{getFolderById:id=>objects.get(id)},
    getOrCreateFolder:(p,name)=>{assert.ok(held);if(!p.children.has(name))p.children.set(name,new Folder('folder-'+(++seq)));return p.children.get(name);},
    Drive:{Files:{generateIds:()=>({ids:['binary-'+(++seq)]}),get:(id)=>{if(!files.has(id))throw Error('404 File not found');return files.get(id);},create:(meta)=>{assert.ok(held);files.set(meta.id,{...meta,size:0});return {id:meta.id};}}},
    v3Store:()=>({get:()=>parent}),ScriptApp:{getOAuthToken:()=>'mock-token'},
    UrlFetchApp:{fetch:(url,options)=>{assert.match(url,/^https:\/\/www.googleapis.com\/upload\/drive\/v3\/files\/binary-/);assert.equal(options.method,'patch');assert.equal(options.headers.Authorization,'Bearer mock-token');sessionCount++;return {getResponseCode:()=>200,getAllHeaders:()=>({Location:'https://www.googleapis.com/upload/drive/v3/files/id?upload_id=mock'})};}},
    Utilities:{DigestAlgorithm:{SHA_256:'sha256'},computeDigest:(_,b)=>[...crypto.createHash('sha256').update(b).digest()],newBlob:blob},
    jsonResponse:x=>x,Date
  });
  vm.runInContext(fs.readFileSync('google-apps-script/VideoV1.gs','utf8'),context);
  return {context,files,parent,setEnabled:b=>enabled=b,get starts(){return sessionCount;}};
}
const p=()=>({action:'beginVideoV1',videoProtocol:1,parentKey:'SCAP/parent',mediaId:'media9',category:'GENERAL',categoryLabel:'General',mimeType:'video/mp4',sizeBytes:300,sha256:crypto.createHash('sha256').update('content').digest('hex'),parentRevision:20});
test('Apps Script disabled flag creates no remote objects',()=>{const e=environment();e.setEnabled(false);assert.equal(e.context.videoV1Action(p()).code,'VIDEO_DISABLED');assert.equal(e.files.size,0);});
test('Apps Script adapter retains generated ID and resumes binary session',()=>{const e=environment(),a=e.context.videoV1Action(p()),b=e.context.videoV1Action(p());assert.equal(a.ok,true);assert.equal(a.fileId,b.fileId);assert.equal(e.files.size,1);assert.equal(e.starts,1);const f=e.files.get(a.fileId);f.size=300;f.sha256Checksum=p().sha256;const receipt=e.context.videoV1Action({...p(),action:'finishVideoV1',fileId:a.fileId});assert.equal(receipt.complete,true);assert.equal(e.files.size,1);});
test('Apps Script verifies Drive binary SHA256 before confirming',()=>{const e=environment(),a=e.context.videoV1Action(p()),f=e.files.get(a.fileId);f.size=300;assert.equal(e.context.videoV1Action({...p(),action:'finishVideoV1',fileId:a.fileId}).code,'VIDEO_CHECKSUM_NOT_AVAILABLE');f.sha256Checksum='a'.repeat(64);assert.equal(e.context.videoV1Action({...p(),action:'finishVideoV1',fileId:a.fileId}).code,'VIDEO_HASH_OR_SIZE_MISMATCH');});
