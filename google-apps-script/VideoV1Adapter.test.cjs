const {test}=require('node:test'),assert=require('node:assert/strict'),vm=require('node:vm'),fs=require('node:fs'),crypto=require('node:crypto');
const core=require('./VideoV1Core.js');
const names=require('./VideoNamesV2Core.js');
function environment() {
  const objects=new Map(),files=new Map();let seq=0,held=false,enabled=true,sessionCount=0;const parent={kind:'SCAP',identityId:'bridge-id',family:'PUENTE',pathVersion:4,revision:20,receipt:{folderId:'parent'}};
  const iterator=a=>{let n=0;return {hasNext:()=>n<a.length,next:()=>a[n++]};};
  const blob=(value,mime,name)=>({getDataAsString:()=>String(value),getName:()=>name,getContentType:()=>mime});
  class Folder {
    constructor(id){this.id=id;this.children=new Map();this.files=[];objects.set(id,this);}
    getId(){return this.id;}getFilesByName(name){return iterator(this.files.filter(x=>x.name===name));}
    createFile(b){assert.ok(held);const f={name:b.getName(),text:b.getDataAsString(),getBlob(){return blob(this.text,'application/json',this.name);},setContent(t){this.text=t;}};this.files.push(f);return f;}
  }
  new Folder('root');new Folder('parent');
  const context=vm.createContext({VideoV1:core,VideoNamesV2:names,ROOT_FOLDER_ID:'root',v3Enabled:()=>true,
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
  function binding(sequence=1) {
    context.LockService.getScriptLock().tryLock();
    try {
      const authority=context.getOrCreateFolder(objects.get('root'),'._GBO_SYNC_V3');
      authority.createFile(blob(JSON.stringify({identity:'bridge-id',pathVersion:4,family:'PUENTE',folderId:'parent',sequence}),'application/json','binding_v4_'+crypto.createHash('sha256').update('bridge-id').digest('hex')+'.json'));
    } finally {context.LockService.getScriptLock().releaseLock();}
  }
  return {context,files,parent,objects,binding,setEnabled:b=>enabled=b,get starts(){return sessionCount;}};
}
const p=()=>({action:'beginVideoV1',videoProtocol:1,parentKey:'SCAP/parent',mediaId:'media9',category:'GENERAL',categoryLabel:'General',mimeType:'video/mp4',sizeBytes:300,sha256:crypto.createHash('sha256').update('content').digest('hex'),parentRevision:20});
test('Apps Script disabled flag creates no remote objects',()=>{const e=environment();e.setEnabled(false);assert.equal(e.context.videoV1Action(p()).code,'VIDEO_DISABLED');assert.equal(e.files.size,0);});
test('Apps Script adapter retains generated ID and resumes binary session',()=>{const e=environment(),a=e.context.videoV1Action(p()),b=e.context.videoV1Action(p());assert.equal(a.ok,true);assert.equal(a.fileId,b.fileId);assert.equal(e.files.size,1);assert.equal(e.starts,1);const f=e.files.get(a.fileId);f.size=300;f.sha256Checksum=p().sha256;const receipt=e.context.videoV1Action({...p(),action:'finishVideoV1',fileId:a.fileId});assert.equal(receipt.complete,true);assert.equal(e.files.size,1);});
test('Apps Script verifies Drive binary SHA256 before confirming',()=>{const e=environment(),a=e.context.videoV1Action(p()),f=e.files.get(a.fileId);f.size=300;assert.equal(e.context.videoV1Action({...p(),action:'finishVideoV1',fileId:a.fileId}).code,'VIDEO_CHECKSUM_NOT_AVAILABLE');f.sha256Checksum='a'.repeat(64);assert.equal(e.context.videoV1Action({...p(),action:'finishVideoV1',fileId:a.fileId}).code,'VIDEO_HASH_OR_SIZE_MISMATCH');});

const readable=()=>({...p(),videoNamingVersion:2,category:'T_5B0328A17D4BD8BD',categoryLabel:'AGUAS ARRIBA'});
test('V2 adapter resolves V4 element and readable folder while preserving internal category',()=>{
  const e=environment();e.binding(7);const a=e.context.videoV1Action(readable()),f=e.files.get(a.fileId);
  assert.equal(a.ok,true);assert.equal(a.fileName,'001_PUE7_ARR.mp4');assert.equal(a.categoryFolder,'AGUAS_ARRIBA');
  assert.equal(f.name,a.fileName);assert.equal(f.appProperties.gboCategory,readable().category);
  const folder=e.objects.get(f.parents[0]);assert.equal(e.objects.get('parent').children.get('VIDEOS').children.get('AGUAS_ARRIBA').id,folder.id);
  assert.equal(e.context.videoV1Action({...readable(),mediaId:'other-tablet'}).fileName,'002_PUE7_ARR.mp4');
});
test('ScriptLock rejects interleaved tablet before name or binary allocation',()=>{
  const e=environment();e.binding();const original=e.context.Drive.Files.create;let second;
  e.context.Drive.Files.create=meta=>{second=e.context.videoV1Action({...readable(),mediaId:'concurrent-tablet'});return original(meta);};
  const first=e.context.videoV1Action(readable());assert.equal(second.code,'BUSY');assert.equal(e.files.size,1);
  e.context.Drive.Files.create=original;
  assert.equal(e.context.videoV1Action({...readable(),mediaId:'concurrent-tablet'}).fileName,'002_PUE1_ARR.mp4');
  assert.equal(e.context.videoV1Action(readable()).fileId,first.fileId);
});
test('Lost begin, interrupted creation and lost finish keep reserved name and fileId',()=>{
  const e=environment();e.binding();const original=e.context.Drive.Files.create;let fail=true;
  e.context.Drive.Files.create=meta=>{if(fail){fail=false;throw Error('synthetic interruption');}return original(meta);};
  assert.equal(e.context.videoV1Action(readable()).ok,false);
  const a=e.context.videoV1Action(readable()),b=e.context.videoV1Action(readable());
  assert.equal(a.fileName,'001_PUE1_ARR.mp4');assert.equal(a.fileId,b.fileId);assert.equal(e.files.size,1);
  const f=e.files.get(a.fileId);f.size=300;f.sha256Checksum=p().sha256;
  const ack=e.context.videoV1Action({...readable(),action:'finishVideoV1',fileId:a.fileId});
  const retry=e.context.videoV1Action(readable());assert.equal(ack.complete,true);assert.equal(retry.complete,true);
  assert.equal(retry.fileName,ack.fileName);assert.equal(retry.fileId,ack.fileId);assert.equal(e.files.size,1);
});
test('Legacy reservation is never renamed or moved when a V2 client retries',()=>{
  const e=environment(),old=e.context.videoV1Action(p()),f=e.files.get(old.fileId),name=f.name,dest=f.parents[0];
  const retry=e.context.videoV1Action({...p(),videoNamingVersion:2});
  assert.equal(retry.videoNamingVersion,1);assert.equal(retry.historicalName,true);
  assert.equal(retry.fileId,old.fileId);assert.equal(f.name,name);assert.equal(f.parents[0],dest);
});
test('Missing V4 binding cannot reserve a fictional element number',()=>{
  const e=environment();assert.equal(e.context.videoV1Action(readable()).code,'VIDEO_ELEMENT_BINDING_REQUIRED');assert.equal(e.files.size,0);
});
test('Interruption between durable name reservation and journal creation retains the same slot',()=>{
  const e=environment();e.binding();const original=e.context.Utilities.newBlob;let fail=true;
  e.context.Utilities.newBlob=(value,mime,name)=>{if(fail && name.startsWith('video_')){fail=false;throw Error('synthetic journal failure');}return original(value,mime,name);};
  assert.equal(e.context.videoV1Action(readable()).ok,false);assert.equal(e.files.size,0);
  assert.equal(e.context.videoV1Action(readable()).fileName,'001_PUE1_ARR.mp4');
  assert.equal(e.context.videoV1Action({...readable(),mediaId:'next'}).fileName,'002_PUE1_ARR.mp4');
});
test('A manually renamed remote file is detected without overwriting its name',()=>{
  const e=environment();e.binding();const a=e.context.videoV1Action(readable()),file=e.files.get(a.fileId);
  file.name='manually_changed.mp4';assert.equal(e.context.videoV1Action(readable()).code,'VIDEO_NAME_METADATA_CONFLICT');
  assert.equal(file.name,'manually_changed.mp4');assert.equal(e.files.size,1);
});
test('Rollback capability disables new names but preserves existing V2 destinations and ACK',()=>{
  const e=environment();e.binding();const a=e.context.videoV1Action(readable());
  e.context.VideoNamesV2={...names,ENABLED:false};assert.equal(e.context.videoNamesV2Enabled(),false);
  assert.equal(e.context.videoV1Action({...readable(),mediaId:'new-video'}).code,'VIDEO_READABLE_NAMES_DISABLED');
  const retry=e.context.videoV1Action({...readable(),videoNamingVersion:undefined});assert.equal(retry.fileId,a.fileId);assert.equal(retry.fileName,a.fileName);
});
test('Published capability explicitly distinguishes naming V2 from transport V1',()=>{
  const e=environment();e.context.ContentService={MimeType:{JSON:'json'},createTextOutput:text=>({setMimeType:()=>JSON.parse(text)})};
  vm.runInContext(fs.readFileSync('google-apps-script/Code.gs','utf8'),e.context);
  const a=e.context.doGet();assert.equal(a.videoProtocol,1);assert.equal(a.readableVideoNamingVersion,2);assert.equal(a.videoScope,'SCAP_ONLY');
  e.context.VideoNamesV2={...names,ENABLED:false};assert.equal(e.context.doGet().readableVideoNamingVersion,0);assert.equal(e.context.doGet().videoProtocol,1);
  e.setEnabled(false);assert.equal(e.context.doGet().videoProtocol,0);assert.equal(e.context.doGet().readableVideoNamingVersion,0);
});
