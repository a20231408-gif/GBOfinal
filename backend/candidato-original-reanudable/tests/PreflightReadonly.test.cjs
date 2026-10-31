const {test}=require('node:test'),assert=require('node:assert/strict'),vm=require('node:vm'),fs=require('node:fs'),path=require('node:path');
const source=fs.readFileSync(path.join(__dirname,'../PreflightReadonly.gs'),'utf8');
function run({change=false,bad=false}={}){
 const rows=Array.from({length:135},(_,i)=>({id:'J'+i,name:'journal_'+i.toString(16).padStart(64,'0')+'.json',mimeType:'application/json',modifiedTime:'SAME'}));let calls=0;const logs=[];
 const context=vm.createContext({ROOT_FOLDER_ID:'MOCK_ROOT',Session:{getEffectiveUser:()=>({getEmail:()=>'reconocimientoinventariovial@gmail.com'})},console:{log:x=>logs.push(x)},
 Drive:{About:{get:()=>({user:{emailAddress:'reconocimientoinventariovial@gmail.com'}})},Files:{get:id=>({id,name:'MOCK_ROOT',capabilities:{canEdit:true,canAddChildren:true}}),list:args=>{calls++;assert.equal(args.pageSize,100);const start=args.pageToken?100:0;return {files:rows.slice(start,start+100).map(x=>({...x,modifiedTime:change&&calls>2?'CHANGED':x.modifiedTime})),nextPageToken:start===0?'OPAQUE_PROVIDER_TOKEN':undefined};}}},
 DriveApp:{getFolderById:()=>({getFoldersByName:()=>{let used=false;return {hasNext:()=>!used,next:()=>{used=true;return {getId:()=> 'MOCK_REGISTRY'};}};}}),getFileById:id=>({getBlob:()=>({getDataAsString:()=>bad&&id==='J0'?'INVALID_JSON':JSON.stringify({status:id==='J134'?'PENDING':'COMPLETE',key:'SIC/FICTITIOUS_'+id,revision:1,operations:[]})})})}});
 vm.runInContext(source,context);const report=context.gboPilotInventoryReadonly();const emitted=logs.filter(s=>s.startsWith('GBO_PILOT_ROWS ')).flatMap(s=>JSON.parse(s.slice('GBO_PILOT_ROWS '.length)).rows);
 return {report,emitted,calls};
}
test('paginates 135 journals twice using the opaque token and emits every row exactly once',()=>{const x=run();assert.equal(x.calls,4);assert.equal(x.report.journals,135);assert.equal(x.report.states.COMPLETE,134);assert.equal(x.report.states.PENDING,1);assert.equal(x.report.stableDuringRead,true);assert.equal(x.emitted.length,135);assert.equal(new Set(x.emitted.map(r=>r.id)).size,135);assert.equal(x.report.canEdit,true);assert.equal(x.report.mutations,0);});
test('unreadable journal is retained and counted as a blocking review item',()=>{const x=run({bad:true});assert.equal(x.report.states.UNREADABLE,1);assert.equal(x.emitted.find(x=>x.id==='J0').requiresReview,true);});
test('concurrent changes cannot be reported as a stable inventory',()=>{assert.equal(run({change:true}).report.stableDuringRead,false);});

function fixture({unreadableRecord=false,pending=false}={}){
 const suffix='1'.repeat(64),files=[{id:'J',name:'journal_'+suffix+'.json'},{id:'R',name:'record_'+suffix+'.json'},{id:'B',name:'binding_v4_'+suffix+'.json'}].map(f=>({...f,modifiedTime:'UNCHANGED'}));
 const values={J:JSON.stringify({status:pending?'PENDING':'COMPLETE',revision:2,operations:[{id:'INACCESSIBLE_OPERATION'}]}),R:unreadableRecord?'INVALID_JSON':JSON.stringify({key:pending?null:'SIC/MEMORY_ONLY',identityId:'UUID_MEMORY_ONLY',revision:2,contentHash:'NEW',receipt:{revision:1,contentHash:'OLD'}}),B:JSON.stringify({identity:'UUID_MEMORY_ONLY',folderId:'FOLDER_MEMORY_ONLY',pathVersion:4})},logs=[];
 const c=vm.createContext({ROOT_FOLDER_ID:'MOCK_ROOT',console:{log:x=>logs.push(x)},Drive:{About:{get:()=>({user:{emailAddress:'reconocimientoinventariovial@gmail.com'}})},Files:{get:id=>{if(id==='INACCESSIBLE_OPERATION')throw Error('404');return {id,name:'MEMORY',capabilities:{canEdit:true,canAddChildren:true}};},list:()=>({files})}},DriveApp:{getFolderById:()=>({getFoldersByName:()=>{let seen=false;return {hasNext:()=>!seen,next:()=>{seen=true;return {getId:()=> 'MEMORY_REGISTRY'};}};}}),getFileById:id=>({getBlob:()=>({getDataAsString:()=>values[id]})})}});
 vm.runInContext(source,c);return {report:c.gboPilotInventoryReadonly(),logs};
}
test('inventories record revisions and bindings and flags a stale receipt without changing journal state',()=>{
 const x=fixture();assert.equal(x.report.records,1);assert.equal(x.report.bindings,1);assert.equal(x.report.states.COMPLETE,1);assert.equal(x.report.reviewJournals,1);
 const row=JSON.parse(x.logs.find(l=>l.startsWith('GBO_PILOT_ROWS ')).slice(15)).rows[0];assert.ok(row.issues.includes('RECEIPT_MISMATCH_OR_MISSING'));assert.equal(row.recordRevision,2);assert.equal(row.requiresOldRoot,true);
});
test('unreadable record does not double-count its readable COMPLETE journal',()=>{
 const x=fixture({unreadableRecord:true});assert.equal(x.report.unreadableRecords,1);assert.equal(x.report.states.COMPLETE,1);assert.equal(x.report.states.UNREADABLE,undefined);assert.equal(x.report.reviewJournals,1);
});
test('inaccessible operation metadata preserves a PENDING row and never counts it again as unreadable',()=>{
 const x=fixture({pending:true});assert.equal(x.report.states.PENDING,1);assert.equal(x.report.states.UNREADABLE,undefined);
 const row=JSON.parse(x.logs.find(l=>l.startsWith('GBO_PILOT_ROWS ')).slice(15)).rows[0];assert.ok(row.issues.includes('OPERATION_METADATA_UNREADABLE'));assert.equal(row.requiresReview,true);
});
test('destination probe reads identity, exact ancestor chain and writer capability without creating a sentinel',()=>{
 const c=vm.createContext({console:{log:()=>{}},Drive:{About:{get:()=>({user:{emailAddress:'reconocimientoinventariovial@gmail.com'}})},Files:{get:id=>id==='root'?{id:'MY_DRIVE'}:{id,name:id,mimeType:'application/vnd.google-apps.folder',trashed:false,parents:[id==='DEST'?'PARENT':'MY_DRIVE'],capabilities:{canEdit:true,canAddChildren:true},permissions:[{id:'OWNER',type:'user',role:'owner'}]}}}});
 vm.runInContext(source,c);c.GBO_PREFLIGHT_DESTINATION_ID='DEST';c.GBO_PREFLIGHT_DESTINATION_ANCESTORS=['PARENT'];const out=c.gboPilotDestinationReadonly();assert.equal(out.chainMatches,true);assert.equal(out.chain[0].capabilities.canAddChildren,true);assert.equal(out.mutations,0);
 c.Drive.About.get=()=>({user:{emailAddress:'WRONG_EXECUTOR'}});assert.throws(()=>c.gboPilotDestinationReadonly(),/PREFLIGHT_WRONG_EXECUTOR/);
});
