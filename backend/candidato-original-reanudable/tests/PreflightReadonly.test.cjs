const {test}=require('node:test'),assert=require('node:assert/strict'),vm=require('node:vm'),fs=require('node:fs'),path=require('node:path');
const source=fs.readFileSync(path.join(__dirname,'../PreflightReadonly.gs'),'utf8');
function run({change=false,bad=false}={}){
 const rows=Array.from({length:135},(_,i)=>({id:'J'+i,name:'journal_'+i.toString(16).padStart(64,'0')+'.json',mimeType:'application/json',modifiedTime:'SAME'}));let calls=0;const logs=[];
 const context=vm.createContext({ROOT_FOLDER_ID:'MOCK_ROOT',Session:{getEffectiveUser:()=>({getEmail:()=>'reconocimientoinventariovial@gmail.com'})},console:{log:x=>logs.push(x)},
 Drive:{Files:{get:id=>({id,name:'MOCK_ROOT',capabilities:{canEdit:true,canAddChildren:true}}),list:args=>{calls++;assert.equal(args.pageSize,100);const start=args.pageToken?100:0;return {files:rows.slice(start,start+100).map(x=>({...x,modifiedTime:change&&calls>2?'CHANGED':x.modifiedTime})),nextPageToken:start===0?'OPAQUE_PROVIDER_TOKEN':undefined};}}},
 DriveApp:{getFolderById:()=>({getFoldersByName:()=>{let used=false;return {hasNext:()=>!used,next:()=>{used=true;return {getId:()=> 'MOCK_REGISTRY'};}};}}),getFileById:id=>({getBlob:()=>({getDataAsString:()=>bad&&id==='J0'?'INVALID_JSON':JSON.stringify({status:id==='J134'?'PENDING':'COMPLETE',key:'SIC/FICTITIOUS_'+id,revision:1,operations:[]})})})}});
 vm.runInContext(source,context);const report=context.gboPilotInventoryReadonly();const emitted=logs.filter(s=>s.startsWith('GBO_PILOT_ROWS ')).flatMap(s=>JSON.parse(s.slice('GBO_PILOT_ROWS '.length)).rows);
 return {report,emitted,calls};
}
test('paginates 135 journals twice using the opaque token and emits every row exactly once',()=>{const x=run();assert.equal(x.calls,4);assert.equal(x.report.journals,135);assert.equal(x.report.states.COMPLETE,134);assert.equal(x.report.states.PENDING,1);assert.equal(x.report.stableDuringRead,true);assert.equal(x.emitted.length,135);assert.equal(new Set(x.emitted.map(r=>r.id)).size,135);assert.equal(x.report.canEdit,true);assert.equal(x.report.mutations,0);});
test('unreadable journal is retained and counted as a blocking review item',()=>{const x=run({bad:true});assert.equal(x.report.states.UNREADABLE,1);assert.equal(x.emitted.find(x=>x.id==='J0').requiresReview,true);});
test('concurrent changes cannot be reported as a stable inventory',()=>{assert.equal(run({change:true}).report.stableDuringRead,false);});
