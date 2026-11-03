const {test}=require('node:test'),assert=require('node:assert/strict'),vm=require('node:vm'),fs=require('node:fs'),path=require('node:path'),crypto=require('node:crypto');
const source=fs.readFileSync(path.join(__dirname,'../PreflightReadonlyPaged.gs'),'utf8');
const {reconcilePartitions}=require('../readonly-reconcile.cjs');
function fixture({change=false,bad=false,wrong=false}={}){
 const files=[],values={},logs=[];let lists=0,reads=0;
 for(let i=0;i<135;i++){
  const suffix=(i%16).toString(16)+i.toString(16).padStart(63,'0'),key='SIC/FICTICIO_'+i;
  const record={key,revision:2,contentHash:'HASH_FICTICIO',receipt:{ok:true,key,revision:2,contentHash:'HASH_FICTICIO',files:[],sicFiles:[]}};
  for(const kind of ['journal','record','binding_v4']){
   const id=kind+i;files.push({id,name:kind+'_'+suffix+'.json',modifiedTime:'SAME'});
   values[id]=JSON.stringify(kind==='record'?record:kind==='journal'?{status:i===134?'PENDING':i===133?'ROLLED_BACK':'COMPLETE',revision:2,operations:[]}: {identity:key,folderId:'FICTICIO_FOLDER',pathVersion:4});
  }
 }
 if(bad)values.journal0='NOT_JSON';
 const c=vm.createContext({ROOT_FOLDER_ID:'FICTICIO_ROOT',console:{log:s=>logs.push(s)},Utilities:{DigestAlgorithm:{SHA_256:'sha256'},computeDigest:(_,s)=>[...crypto.createHash('sha256').update(s).digest()]},
  Drive:{About:{get:()=>({user:{emailAddress:wrong?'OTHER_ACCOUNT':'reconocimientoinventariovial@gmail.com'}})},Files:{
   get:id=>({id,name:'FICTICIO',owners:[{emailAddress:'FICTICIO_OWNER'}],capabilities:{canEdit:true,canAddChildren:true}}),
   list:a=>{lists++;const start=+(a.pageToken||0);return {files:files.slice(start,start+100).map(f=>({...f,modifiedTime:change&&lists>5?'CHANGED':f.modifiedTime})),nextPageToken:start+100<files.length?String(start+100):undefined};}}},
  DriveApp:{getFolderById:()=>({getFoldersByName:()=>{let used=false;return {hasNext:()=>!used,next:()=>{used=true;return {getId:()=> 'FICTICIO_REGISTRY'};}};}}),
   getFileById:id=>{reads++;return {getBlob:()=>({getDataAsString:()=>values[id]})};}}});
 vm.runInContext(source,c);return {c,logs,get reads(){return reads;}};
}
function partition(f,bucket){const start=f.logs.length,end=f.c.gboPilotInventoryReadonlyBatch_(bucket);return {end,rows:f.logs.slice(start).filter(s=>s.startsWith('GBO_READONLY_ROW ')).map(s=>JSON.parse(s.slice(17)))};}
test('all sixteen bounded partitions cover 405 JSONs once with complete stable metadata pagination',()=>{
 const f=fixture(),parts=[...Array(16)].map((_,i)=>partition(f,i.toString(16)));
 assert.equal(f.reads,405);assert.ok(parts.every(p=>p.end.targets<=27&&p.end.mutations===0&&p.end.complete));
 const out=reconcilePartitions(parts);assert.equal(out.summary.journals,135);assert.deepEqual(out.summary.states,{COMPLETE:133,ROLLED_BACK:1,PENDING:1});assert.equal(out.summary.reviewJournals,2);assert.equal(out.summary.remoteFilesValidated,false);
});
test('wrong identity fails before any registry content read and no write services are available',()=>{const f=fixture({wrong:true});assert.throws(()=>partition(f,'0'),/PREFLIGHT_WRONG_EXECUTOR/);assert.equal(f.reads,0);assert.doesNotMatch(source,/setProperty|createFolder|createFile|setTrashed|v3Store\(/);});
test('unknown partition and incomplete extraction cannot be accepted as a complete inventory',()=>{const f=fixture();assert.throws(()=>partition(f,'q'),/INVALID_READONLY_BUCKET/);assert.throws(()=>reconcilePartitions([partition(f,'0')]),/INCOMPLETE_PARTITIONS/);});
test('unreadable journals are preserved and counted without changing a readable record',()=>{const f=fixture({bad:true}),out=reconcilePartitions([...Array(16)].map((_,i)=>partition(f,i.toString(16))));assert.equal(out.summary.states.UNREADABLE,1);assert.equal(out.summary.records,135);assert.ok(out.journals.find(r=>r.id==='journal0').requiresReview);});
test('concurrent metadata changes prevent stable acceptance',()=>{const f=fixture({change:true}),p=partition(f,'0');assert.equal(p.end.stableDuringRead,false);const parts=[p,...[...Array(15)].map((_,i)=>partition(f,(i+1).toString(16)))];assert.throws(()=>reconcilePartitions(parts),/CROSS_PARTITION_CHANGE|UNVERIFIED_PARTITION/);});
test('missing log rows, duplicate rows and stale ACK are rejected or reconciled explicitly',()=>{
 const f=fixture(),parts=[...Array(16)].map((_,i)=>partition(f,i.toString(16))),first=parts[0].rows.find(r=>r.kind==='record');first.ack.revision=1;
 const out=reconcilePartitions(parts);assert.ok(out.journals.find(r=>r.recordId===first.id).issues.includes('ACK_MISMATCH_OR_MISSING'));
 parts[0].rows.pop();assert.throws(()=>reconcilePartitions(parts),/MISSING_OR_DUPLICATE_ROWS/);
});
test('destination reader follows the exact chain and paginates all permissions without a sentinel write',()=>{
 const f=fixture();let calls=0;
 f.c.Drive.Files.get=id=>id==='root'?{id:'MY_DRIVE'}:{id,name:id,mimeType:'application/vnd.google-apps.folder',trashed:false,parents:[id==='DEST'?'PARENT':'MY_DRIVE'],owners:[{emailAddress:'reconocimientoinventariovial@gmail.com'}],capabilities:{canEdit:true,canAddChildren:true}};
 f.c.Drive.Permissions={list:(id,a)=>{calls++;return a.pageToken?{permissions:[{id:'OWNER',type:'user',role:'owner'}]}:{permissions:[],nextPageToken:'NEXT'};}};
 const result=f.c.gboPilotDestinationReadonlyWithConfig_({destinationId:'DEST',ancestorIds:['PARENT']});
 assert.equal(calls,4);assert.equal(result.chainMatches,true);assert.equal(result.institutionalOwners,true);assert.equal(result.canWrite,true);assert.equal(result.publicWriters.length,0);assert.equal(result.mutations,0);
 f.c.Drive.Permissions.list=()=>({permissions:[{id:'PUBLIC_EDITOR',type:'anyone',role:'writer'}]});
 assert.equal(f.c.gboPilotDestinationReadonlyWithConfig_({destinationId:'DEST',ancestorIds:['PARENT']}).publicWriters.length,2);
});
test('destination ownership, ancestry and account mismatches never receive an implicit PASS',()=>{
 const f=fixture();f.c.Drive.Permissions={list:()=>({permissions:[]})};
 f.c.Drive.Files.get=id=>({id,name:id,mimeType:'application/vnd.google-apps.folder',trashed:false,parents:['WRONG'],owners:[{emailAddress:'OTHER_OWNER'}],capabilities:{canEdit:true,canAddChildren:false}});
 const result=f.c.gboPilotDestinationReadonlyWithConfig_({destinationId:'DEST',ancestorIds:['PARENT']});
 assert.equal(result.chainMatches,false);assert.equal(result.institutionalOwners,false);assert.equal(result.canWrite,false);
 f.c.Drive.About.get=()=>({user:{emailAddress:'OTHER_ACCOUNT'}});assert.throws(()=>f.c.gboPilotDestinationReadonlyWithConfig_({destinationId:'DEST',ancestorIds:['PARENT']}),/PREFLIGHT_WRONG_EXECUTOR/);
});
