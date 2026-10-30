// ONLY the private GBO test copy. Never run against an operational root.
function gboAuditPendingOperations() {
  const started=Date.now(), p=GBO_FICTITIOUS_SCAP_PAYLOAD;
  const root=gboPrivateTestRoot();
  const dirs=root.getFoldersByName('._GBO_SYNC_V3');
  if(!dirs.hasNext())throw Error('MISSING_TEST_REGISTRY');
  const files=dirs.next().getFiles();const report=[];
  while(files.hasNext()) {
    const file=files.next();if(!file.getName().startsWith('journal_'))continue;
    const raw=file.getBlob().getDataAsString();const journal=JSON.parse(raw);
    console.log('PRIVATE_JOURNAL_SNAPSHOT '+raw);
    journal.operations.forEach((op,index)=>{
      const t=Date.now(), actual=DriveApp.getFileById(op.id);
      const prefix='gbo-v3-file:'+p.key+':';const owner=actual.getDescription();
      const key=owner.startsWith(prefix)?owner.slice(prefix.length):null;
      const expected=p.files.find(f=>f.key===key);
      const parents=actual.getParents(),parent=parents.hasNext()?parents.next():null;
      const bytes=actual.getBlob().getBytes(),hash=gboTestHash(bytes);
      report.push({index,key,name:actual.getName(),id:actual.getId(),parentId:parent?.getId(),
        inArchive:parent?.getId()===journal.archiveId,created:op.created,
        ownerGbo:actual.getOwner().getEmail()==='reconocimientoinventariovial@gmail.com',
        hashMatches:!!expected&&hash===expected.sha256,sha256:hash,ms:Date.now()-t});
    });
  }
  console.log('PRIVATE_OPERATION_AUDIT '+JSON.stringify(report));
  console.log(JSON.stringify({phase:'AUDIT_COMPLETE',operations:report.length,
    validHashes:report.filter(x=>x.hashMatches).length,owners:report.filter(x=>x.ownerGbo).length,
    archiveOperations:report.filter(x=>x.inArchive).length,ms:Date.now()-started,testOnly:true}));
}
function gboPrivateTestRoot() {
  if(Session.getEffectiveUser().getEmail()!=='reconocimientoinventariovial@gmail.com')throw Error('WRONG_IDENTITY');
  const root=DriveApp.getFolderById(PropertiesService.getScriptProperties().getProperty('GBO_ISOLATED_ROOT_ID'));
  if(root.getId()!==ROOT_FOLDER_ID||root.getSharingAccess()!==DriveApp.Access.PRIVATE||root.getOwner().getEmail()!=='reconocimientoinventariovial@gmail.com')throw Error('WRONG_PRIVATE_ROOT');
  return root;
}
function gboTestHash(bytes){return Utilities.computeDigest(Utilities.DigestAlgorithm.SHA_256,bytes).map(b=>(b&255).toString(16).padStart(2,'0')).join('');}
