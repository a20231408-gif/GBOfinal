/** PROPOSAL ONLY. Requires explicit approval to install, read properties and create a private backup file.
 * Not part of V22, doGet or doPost. No logs, property updates, permissions or deployments.
 */
function gboPrivatePropertiesBackupProposed() {
  const expected='reconocimientoinventariovial@gmail.com';
  const user=Drive.About.get({fields:'user(emailAddress)'}).user;
  if(!user || user.emailAddress!==expected)throw Error('WRONG_EXECUTOR');
  const root=Drive.Files.get('root',{fields:'id,owners(emailAddress),capabilities(canAddChildren)'});
  if(root.owners?.length!==1||root.owners[0].emailAddress!==expected||!root.capabilities?.canAddChildren)throw Error('PRIVATE_ROOT_NOT_VERIFIED');
  const properties=PropertiesService.getScriptProperties().getProperties();
  const canonical=JSON.stringify(Object.keys(properties).sort().map(k=>[k,properties[k]]));
  function hash(bytes){return Utilities.computeDigest(Utilities.DigestAlgorithm.SHA_256,bytes).map(b=>(b&255).toString(16).padStart(2,'0')).join('');}
  const capturedAt=new Date().toISOString();
  const envelope={schema:1,scriptId:ScriptApp.getScriptId(),capturedAt,count:Object.keys(properties).length,
    sha256:hash(Utilities.newBlob(canonical).getBytes()),properties};
  const body=JSON.stringify(envelope),blob=Utilities.newBlob(body,'application/json','GBO_PROPERTIES_PRIVATE_'+capturedAt.replace(/[:.]/g,'-')+'.json');
  const bytes=blob.getBytes();
  // Deliberate implementation bound, not a claim about a Google API maximum.
  if(bytes.length>8*1024*1024)throw Error('BACKUP_TOO_LARGE_NO_FILE_CREATED');
  // New file in verified My Drive root; do not supply sharing permissions or publish a link.
  const file=Drive.Files.create({name:blob.getName(),mimeType:'application/json',parents:[root.id]},blob,{fields:'id,size,owners(emailAddress),permissions(type,role,emailAddress)'});
  if(file.owners?.length!==1||file.owners[0].emailAddress!==expected||!file.permissions||
     file.permissions.some(p=>p.type!=='user'||p.emailAddress!==expected||p.role!=='owner'))throw Error('BACKUP_PERMISSIONS_REQUIRE_PRIVATE_REVIEW');
  const readback=DriveApp.getFileById(file.id).getBlob().getBytes();
  if(readback.length!==bytes.length||hash(readback)!==hash(bytes))throw Error('BACKUP_READBACK_MISMATCH');
  return {fileId:file.id,bytes:bytes.length,fileSha256:hash(bytes),count:envelope.count,capturedAt,scriptId:envelope.scriptId};
}