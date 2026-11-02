const {test}=require('node:test'),assert=require('node:assert/strict'),vm=require('node:vm'),fs=require('node:fs');
const source=fs.readFileSync(__dirname+'/ReadonlyRegistry.gs','utf8');
function setup(patch={}){
 let reads=0;const calls=[];
 const globals={Drive:{About:{get:()=>({user:{emailAddress:'fictitious@example.invalid'}})},Files:{get:(id,options)=>{calls.push(['get',id]);return {id,name:'._GBO_SYNC_V3',parents:['ROOT_FIXTURE'],trashed:false};},list:p=>{calls.push(['list',p.pageToken]);return p.pageToken?{files:[{id:'b',name:'journal_b.json',modifiedTime:'1'}]}:{files:[{id:'a',name:'record_a.json',modifiedTime:'1'}],nextPageToken:'page2'};}}},DriveApp:{getFileById:id=>({getBlob:()=>({getDataAsString:()=>{reads++;return JSON.stringify(id==='a'?{key:'SIC/FICTITIOUS',receipt:{revision:1}}:{status:'COMPLETE'});}})})}};
 patch(globals,calls);vm.createContext(globals);vm.runInContext(source,globals);
 return {run:()=>globals.gboAdminRegistryReadonly({executor:'fictitious@example.invalid',registries:[{rootId:'ROOT_FIXTURE',registryId:'REGISTRY_FIXTURE'}]}),globals,calls,reads:()=>reads};
}
test('actual API adapter paginates twice, reads JSON, has no mutating service available',()=>{const f=setup(()=>{}),r=f.run();assert.equal(r.registries[0].entries,2);assert(r.registries[0].stable);assert.equal(f.calls.filter(x=>x[0]==='list').length,4);assert.equal(f.reads(),2);assert.equal(r.usableForDeletion,false);});
test('wrong executor denied before registry reads',()=>{const f=setup(g=>{g.Drive.About.get=()=>({user:{emailAddress:'other'}});});assert.throws(f.run,/WRONG_EXECUTOR/);assert.equal(f.calls.length,0);});
test('wrong root is rejected',()=>{const f=setup(g=>{g.Drive.Files.get=()=>({name:'._GBO_SYNC_V3',parents:['OTHER']});});assert.throws(f.run,/ROOT_MISMATCH/);});
test('repeated pagination token denied',()=>{const f=setup(g=>{g.Drive.Files.list=()=>({files:[],nextPageToken:'loop'});});assert.throws(f.run,/REPEATED_PAGE_TOKEN/);});
test('unreadable registry JSON is reported without inferred ownership',()=>{const f=setup(g=>{g.DriveApp.getFileById=()=>({getBlob:()=>({getDataAsString:()=>'{broken'})});});assert.equal(f.run().registries[0].unreadable,2);});
test('second pass detects unstable registry',()=>{let n=0;const f=setup(g=>{g.Drive.Files.list=()=>({files:[{id:'a',name:'record_a.json',modifiedTime:String(++n)}]});});assert.equal(f.run().registries[0].stable,false);});
