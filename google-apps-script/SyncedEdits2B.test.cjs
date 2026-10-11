const {test}=require('node:test'),assert=require('node:assert/strict'),fs=require('node:fs'),path=require('node:path');
const {environment,hash}=require('./DriveV4Harness.cjs'),core=require('./SyncV3Core.js');
const directory=path.resolve(__dirname,'../docs/edicion-sincronizados-2b/examples');
const fixture=name=>JSON.parse(fs.readFileSync(path.join(directory,name+'.json'),'utf8'));
const clone=p=>structuredClone(p);
function sheet(e,bytes) {return e.context.Utilities.unzip(e.context.Utilities.newBlob([...bytes])).find(x=>x.getName()==='xl/worksheets/sheet1.xml').getDataAsString();}
function dataRows(xml) {return (xml.match(/<row\b[^>]*>[\s\S]*?<\/row>/g)||[]).filter(x=>Number(/\br="(\d+)"/.exec(x)[1])>=14);}
function renamed(row,number) {return row.replace(/\br="(\d+)"/,`r="${number}"`).replace(/\br="([A-Z]+)\d+"/g,(_,col)=>`r="${col}${number}"`);}
function onlyFolderCount(e) {return [...e.objects.values()].filter(x=>x.folders && /^ALCANTARILLA \d+$/.test(x.name)).length;}
function read(e,id) {return Buffer.from(e.objects.get(id).getBlob().getBytes());}
function check(a) {assert.equal(a.ok,true,JSON.stringify(a));return a;}

test('actual Android condition and PR updates preserve element/files and all consolidated rows',()=>{
 const e=environment(),initial=fixture('initial-a'),condition=fixture('condition-a'),other=fixture('initial-b'),pr=fixture('progressive-a');
 for(const p of [initial,condition,other,pr])assert.equal(hash(core.canonical(p)),p.contentHash);
 const a=check(e.send(initial)),b=check(e.send(other)),photo=a.files.find(f=>f.key.startsWith('photo/'));
 const photoBytes=read(e,photo.fileId),otherPhoto=b.files.find(f=>f.key.startsWith('photo/')),otherBytes=read(e,otherPhoto.fileId);
 const c=check(e.send(condition));assert.equal(c.folderId,a.folderId);assert.equal(c.logicalPath,a.logicalPath);assert.equal(c.sicFiles[0].fileId,b.sicFiles[0].fileId);
 assert.deepEqual([...c.sicFiles[0].recordIds].sort(),[initial.recordId,other.recordId].sort());
 assert.notEqual(condition.sicRows[0].sha256,initial.sicRows[0].sha256);
 const updated=check(e.send(pr));assert.equal(updated.folderId,a.folderId);assert.equal(updated.logicalPath,a.logicalPath);assert.equal(updated.files.find(f=>f.key.startsWith('photo/')).fileId,photo.fileId);
 assert.deepEqual([...updated.sicFiles[0].recordIds],[initial.recordId,other.recordId]);assert.equal(onlyFolderCount(e),2);
 const rows=dataRows(sheet(e,read(e,updated.sicFiles[0].fileId)));
 const expected=[pr,other].map((p,i)=>renamed(dataRows(sheet(e,Buffer.from(p.sicRows[0].base64,'base64')))[0],14+i));
 assert.deepEqual(rows,expected,'Excel cell data must match both complete Android revisions');
 assert.deepEqual(read(e,photo.fileId),photoBytes);assert.deepEqual(read(e,otherPhoto.fileId),otherBytes);
 fs.writeFileSync(path.join(directory,'SIC-18-after-edit-local.xlsx'),read(e,updated.sicFiles[0].fileId));
 fs.writeFileSync(path.join(directory,'local-receipts.json'),JSON.stringify({source:'LOCAL DRIVE DOUBLES; NOT REMOTE',a,b,c,updated},null,2));
});

test('two operators on one UUID conflict without overwriting the winner or other engineer',()=>{
 const e=environment(),p=fixture('initial-a');check(e.send(p));const other=check(e.send(fixture('initial-b'))),otherPhoto=other.files.find(f=>f.key.startsWith('photo/')),bytes=read(e,otherPhoto.fileId);
 const winner=fixture('condition-a'),ack=check(e.send(winner)),loser=clone(winner);loser.revision++;
 const count=e.updates,conflict=e.send(loser);assert.equal(conflict.code,'BASE_REVISION_CONFLICT');assert.equal(conflict.remoteRevision,winner.revision);assert.equal(e.updates,count);
 const retry=e.send(winner);assert.equal(retry.alreadyExists,true);assert.equal(retry.folderId,ack.folderId);assert.equal(e.updates,count);assert.deepEqual(read(e,otherPhoto.fileId),bytes);
});

test('same-number different-content and stale revisions are rejected with remote revision',()=>{
 const e=environment(),p=fixture('initial-a');check(e.send(p));const changed=clone(p);changed.files[0].path='SALIDA/another.jpg';
 const conflict=e.send(changed);assert.equal(conflict.code,'REVISION_CONFLICT');assert.equal(conflict.remoteRevision,p.revision);
 check(e.send(fixture('condition-a')));const stale=e.send(p);assert.equal(stale.code,'STALE_REVISION');assert.equal(stale.remoteRevision,fixture('condition-a').revision);
});

test('identical retries do not create new folders files or archives',()=>{
 const e=environment(),p=fixture('initial-a'),a=check(e.send(p));const count=e.objects.size,updates=e.updates;
 for(let i=0;i<3;i++){const b=check(e.send(p));assert.equal(b.alreadyExists,true);assert.equal(b.folderId,a.folderId);assert.deepEqual([...b.files.map(f=>f.fileId)],[...a.files.map(f=>f.fileId)]);}
 assert.equal(e.objects.size,count);assert.equal(e.updates,updates);assert.equal(onlyFolderCount(e),1);
});

test('photo recategorization add replace and removal preserve fileIds originals and history',()=>{
 const e=environment(),p=fixture('initial-a'),a=check(e.send(p)),photo=a.files.find(f=>f.key.startsWith('photo/')),original=read(e,photo.fileId);
 const q=fixture('condition-a'),f=q.files.find(f=>f.key.startsWith('photo/'));f.path=f.path.replace('ENTRADA/','SALIDA/');q.files.push({...f,key:'photo/added',path:'ENTRADA/F_added.jpg'});
 const b=check(e.send(q));assert.equal(b.files.find(x=>x.key===photo.key).fileId,photo.fileId);assert.ok(b.files.find(x=>x.key===photo.key).logicalPath.includes('/SALIDA/'));
 const update=clone(q);update.baseRevision=q.revision;update.revision++;const replacement=update.files.find(x=>x.key===photo.key);
 const edited=Buffer.from(original);edited[edited.length-3]^=1;replacement.base64=edited.toString('base64');replacement.sha256=hash(edited);
 const c=check(e.send(update));assert.equal(c.files.find(x=>x.key===photo.key).fileId,photo.fileId);assert.deepEqual(read(e,photo.fileId),edited);
 assert.ok([...e.objects.values()].some(x=>x.b && x.parent.name.startsWith(update.revision+'_') && read(e,x.id).equals(original)),'old photograph archived before replacement');
 const remove=clone(update);remove.baseRevision=update.revision;remove.revision++;remove.files=remove.files.filter(x=>x.key!==photo.key);check(e.send(remove));
 assert.ok(e.objects.get(photo.fileId).parent.name.startsWith(remove.revision+'_'),'removed owned photo is archived, never deleted');assert.equal(onlyFolderCount(e),1);
});

test('failure during edit rolls back Excel metadata and photos then retries on same IDs',()=>{
 const e=environment(),p=fixture('initial-a'),a=check(e.send(p));check(e.send(fixture('initial-b')));
 const before=read(e,a.sicFiles[0].fileId),photo=a.files.find(f=>f.key.startsWith('photo/')),original=read(e,photo.fileId),q=fixture('condition-a');
 e.state.failAfter=e.updates+2;const failed=e.send(q);assert.equal(failed.ok,false);
 assert.deepEqual(read(e,a.sicFiles[0].fileId),before);assert.deepEqual(read(e,photo.fileId),original);
 const retry=check(e.send(q));assert.equal(retry.folderId,a.folderId);assert.equal(retry.sicFiles[0].fileId,a.sicFiles[0].fileId);assert.equal(retry.sicFiles[0].recordIds.length,2);assert.equal(onlyFolderCount(e),2);
});

test('busy lock and identity substitution cannot touch another row or folder',()=>{
 const e=environment(),a=check(e.send(fixture('initial-a')));const count=e.updates;e.state.busy=true;assert.equal(e.send(fixture('condition-a')).code,'BUSY');e.state.busy=false;
 const forged=fixture('initial-b');forged.identityId=fixture('initial-a').recordId;forged.sicRows[0].recordId=forged.identityId;
 assert.equal(e.send(forged).code,'ROW_OWNER_MISMATCH');assert.equal(e.updates,count);assert.equal(onlyFolderCount(e),1);assert.equal(e.send(fixture('initial-a')).folderId,a.folderId);
});

test('technical delivery remains independent of external georeferencing on edits',()=>{
 const e=environment();check(e.send(fixture('initial-a')));const a=check(e.send(fixture('condition-a')));
 assert.equal(a.technicalDeliveryStatus,'DELIVERED');assert.equal(a.georeferencingStatus,'PENDING_EXTERNAL');assert.equal(a.externalGeoreferencingStatus,'PENDING');assert.equal(a.logicalPath.includes('SIB'),false);
});
