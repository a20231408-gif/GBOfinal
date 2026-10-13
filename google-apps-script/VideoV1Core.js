/* Binary resumable media protocol, independent of SIC/photo revisions. */
var VideoV1 = (() => {
  const MAX_BYTES=32*1024*1024;
  function reject(code){const e=Error(code);e.code=code;throw e;}
  function validate(p) {
    if(!p || p.videoProtocol!==1 || !/^SCAP\/[A-Za-z0-9_-]{1,80}$/.test(p.parentKey) ||
      !/^[A-Za-z0-9_-]{1,80}$/.test(p.mediaId) || ['__proto__','constructor','prototype'].includes(p.mediaId) || !/^[A-Z0-9_]{1,80}$/.test(p.category) ||
      typeof p.categoryLabel!=='string' || !p.categoryLabel.trim() || p.categoryLabel.length>120 || /[\x00-\x1f]/.test(p.categoryLabel) ||
      !['video/mp4','video/webm','video/3gpp'].includes(p.mimeType) || !Number.isSafeInteger(p.sizeBytes) || p.sizeBytes<1 || p.sizeBytes>MAX_BYTES ||
      !/^[a-f0-9]{64}$/.test(p.sha256) || !Number.isSafeInteger(p.parentRevision) || p.parentRevision<1)reject('INVALID_VIDEO');
  }
  function bound(p,s) {
    validate(p);const parent=s.parent(p.parentKey);
    if(!parent?.receipt?.folderId || parent.pathVersion!==4 || parent.revision<p.parentRevision)reject('VIDEO_PARENT_NOT_SYNCED');
    if(parent.kind!=='SCAP' || parent.family!=='PUENTE')reject('VIDEO_BRIDGES_ONLY');
    const old=s.get(p.mediaId);
    if(old && ['parentKey','category','mimeType','sizeBytes','sha256'].some(k=>old[k]!==p[k]))reject('VIDEO_IDENTITY_CONFLICT');
    if(old && old.folderId!==parent.receipt.folderId)reject('VIDEO_RELOCATION_REQUIRES_MIGRATION');
    return {parent,old};
  }
  function receipt(j) {return {ok:true,videoProtocol:1,parentKey:j.parentKey,mediaId:j.mediaId,sha256:j.sha256,sizeBytes:j.sizeBytes,fileId:j.fileId,complete:j.complete===true,
    videoNamingVersion:j.videoNamingVersion || 1,historicalName:j.videoNamingVersion!==2,
    ...(j.videoNamingVersion===2?{fileName:j.fileName,categoryFolder:j.categoryFolder,categoryCode:j.categoryCode,videoNumber:j.videoNumber,elementNumber:j.elementNumber}: {})};}
  function begin(p,s) {
    const {parent,old}=bound(p,s);
    if(p.videoNamingVersion!=null && ![1,2].includes(p.videoNamingVersion))reject('INVALID_VIDEO_NAMING_VERSION');
    const naming=!old && p.videoNamingVersion===2?s.reserveName(p,parent):{};
    const journal=old || {...p,...naming,videoNamingVersion:naming.videoNamingVersion || 1,folderId:parent.receipt.folderId,fileId:s.allocate(),complete:false};
    // Persist the ID before remote creation: a lost start response cannot create a second file.
    if(!old)s.put(journal);
    s.ensure(journal);
    if(journal.complete)return receipt(journal);
    if(p.resetSession===true || !journal.sessionUrl || s.now()-journal.sessionAt>6*24*60*60*1000) {
      journal.sessionUrl=s.open(journal);journal.sessionAt=s.now();s.put(journal);
    }
    return {...receipt(journal),sessionUrl:journal.sessionUrl};
  }
  function finish(p,s) {
    const {old}=bound(p,s);
    if(!old || p.fileId!==old.fileId)reject('VIDEO_FILE_BINDING_MISMATCH');
    const verified=s.verify(old);
    if(verified.sizeBytes!==old.sizeBytes || verified.sha256!==old.sha256 || verified.mimeType!==old.mimeType)reject('VIDEO_HASH_OR_SIZE_MISMATCH');
    old.complete=true;delete old.sessionUrl;delete old.sessionAt;s.put(old);return receipt(old);
  }
  return {MAX_BYTES,validate,begin,finish,reject};
})();
if(typeof module!=='undefined')module.exports=VideoV1;
