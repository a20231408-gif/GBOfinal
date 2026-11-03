/** Cutover admission, not administrative authority. Never classifies an offline old queue as new. */
function gboValidateRootAdmissionConfig(c) {
  const a=c&&c.admission;
  if(!a||a.version!==1||!Number.isSafeInteger(a.activatedAt)||a.activatedAt<1||
     !/^[a-f0-9]{32}$/.test(a.epoch||'')||typeof a.executorEmail!=='string'||!a.executorEmail.includes('@'))
    DeliveryRootPolicy.reject('ROOT_ADMISSION_NOT_CONFIGURED');
  return a;
}
function gboAssertInstitutionalExecutor(c) {
  const a=gboValidateRootAdmissionConfig(c);let actual;
  try{actual=Drive.About.get({fields:'user(emailAddress)'}).user?.emailAddress;}catch(e){DeliveryRootPolicy.reject('ROOT_EXECUTOR_NOT_VERIFIED',true);}
  if(actual!==a.executorEmail)DeliveryRootPolicy.reject('ROOT_EXECUTOR_MISMATCH');
}
function gboAssertNewRootAdmission(p,c,now=Date.now()) {
  const a=gboValidateRootAdmissionConfig(c);
  if(p.rootRoutingVersion!==2||p.rootEpoch!==a.epoch||!Number.isSafeInteger(p.captureCreatedAt)||
     p.captureCreatedAt<a.activatedAt||p.captureCreatedAt>now+300000||p.baseRevision!==0)
    DeliveryRootPolicy.reject('NEW_ROOT_CAPTURE_NOT_ADMITTED');
}

/** This cut is for an institution-owned My Drive destination, not an unconfigured shared drive. */
function gboInstitutionalDestination(c) {
  const root=gboVerifyRootIdentity(c.newRoot),owner=gboValidateRootAdmissionConfig(c).executorEmail;
  for(const id of [c.newRoot.id,...c.newRoot.ancestorIds]) {
    let m;
    try{m=Drive.Files.get(id,{fields:'id,driveId,owners(emailAddress),permissions(type,role)',supportsAllDrives:true});}
    catch(e){DeliveryRootPolicy.reject('ROOT_OWNERSHIP_NOT_VERIFIED',true);}
    if(m.driveId||m.owners?.length!==1||m.owners[0].emailAddress!==owner)
      DeliveryRootPolicy.reject('ROOT_INSTITUTIONAL_OWNERSHIP_REQUIRED');
    if(!Array.isArray(m.permissions))DeliveryRootPolicy.reject('ROOT_ACCESS_NOT_VERIFIED');
    if(m.permissions.some(p=>p.type==='anyone'&&['writer','organizer','fileOrganizer','owner'].includes(p.role)))
      DeliveryRootPolicy.reject('ROOT_PUBLIC_WRITE_ACCESS');
  }
  return root;
}

/** Shared historical roots need not be in the institutional user's own My Drive.
 * Exact ID/name/owner are reviewed privately; no missing ancestor is guessed and no write is allowed.
 */
function gboHistoricalReadRoot(c) {
  if(!c.oldRootReadOnly)return gboAuthorizedRoot();
  const s=c.oldRootReadOnly;
  if(typeof s.name!=='string'||!s.name||typeof s.ownerEmail!=='string'||!s.ownerEmail.includes('@'))
    DeliveryRootPolicy.reject('HISTORICAL_ROOT_NOT_REVIEWED');
  let m,myDrive;
  try{
    m=Drive.Files.get(c.oldRootId,{fields:'id,name,mimeType,trashed,owners(emailAddress)',supportsAllDrives:true});
    myDrive=Drive.Files.get('root',{fields:'id'});
  }catch(e){DeliveryRootPolicy.reject('HISTORICAL_ROOT_UNREADABLE',true);}
  if(c.oldRootId===myDrive.id||m.id!==c.oldRootId||m.trashed!==false||m.mimeType!=='application/vnd.google-apps.folder'||
    m.name!==s.name||m.owners?.length!==1||m.owners[0].emailAddress!==s.ownerEmail)
    DeliveryRootPolicy.reject('HISTORICAL_ROOT_IDENTITY_MISMATCH');
  return DriveApp.getFolderById(c.oldRootId);
}
function gboUploadRootsGate() {
  if(!gboRootRoutingEnabled())return gboAuthorizedRoot();
  const c=gboRootRoutingConfig();gboAssertInstitutionalExecutor(c);gboInstitutionalDestination(c);
  return gboHistoricalReadRoot(c);
}
function gboUploadRootsReady(){try{gboUploadRootsGate();return true;}catch(e){return false;}}
