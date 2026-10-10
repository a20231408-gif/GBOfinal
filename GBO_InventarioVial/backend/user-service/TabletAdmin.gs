/* Explicit capability. Independent of role. HTTP callers hold dispatch_'s script lock. */
function hasTabletAdmin_(id) { return props_().getProperty('TABLET_ADMIN_PERMISSION_'+id)==='true'; }
// Owner-only editor function: deliberately absent from the HTTP action allowlist.
function applyTabletAdminPermission() {
  const lock=LockService.getScriptLock();lock.waitLock(10000);
  try {
    const p=props_(),r=JSON.parse(p.getProperty('TABLET_ADMIN_PERMISSION_REQUEST')||'{}');
    keys_(r,['userId','enabled','reason']);
    if(!readState_().users.some(u=>u.id===r.userId)||typeof r.enabled!=='boolean'||
       typeof r.reason!=='string'||!r.reason.trim()||r.reason.length>200) fail_('INVALID_REQUEST');
    p.setProperty('TABLET_ADMIN_PERMISSION_'+r.userId,String(r.enabled));
    if(!r.enabled) Object.keys(p.getProperties()).filter(k=>/^TABLET_ADMIN_SESSION_[a-f0-9]{64}$/.test(k)).forEach(k=>{
      if(JSON.parse(p.getProperty(k)).userId===r.userId)p.deleteProperty(k);
    });
    appendAudit_(auditEvent_('OWNER',r.enabled?'TABLET_ADMIN_PERMISSION_GRANTED':'TABLET_ADMIN_PERMISSION_REVOKED',r.userId,''));
    p.deleteProperty('TABLET_ADMIN_PERMISSION_REQUEST');
  } finally {lock.releaseLock();}
}
function authenticateTabletAdmin_(r,state) {
  const p=props_(),u=state.users.find(u=>u.id===r.userId),now=Date.now(),key='TABLET_ADMIN_ATTEMPTS_'+r.deviceId;
  const t=JSON.parse(p.getProperty(key)||'{"failures":0,"blockedUntil":0}');
  if(t.blockedUntil>now)fail_('RATE_LIMITED');
  const failures=t.blockedUntil?1:t.failures+1;
  p.setProperty(key,JSON.stringify({failures:failures,blockedUntil:failures>=5?now+300000:0}));
  const dummy={salt:'AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA=',pinHash:'AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA=',hashIterations:210000};
  const valid=typeof r.pin==='string'&&/^[0-9]{6,12}$/.test(r.pin)&&verifyPin_(r.pin,u||dummy);
  if(!valid||!u||!u.active||!hasTabletAdmin_(u.id)) {
    appendAudit_(auditEvent_('','TABLET_ADMIN_AUTH_FAILURE','',r.deviceId));fail_('TABLET_ADMIN_REQUIRED');
  }
  p.deleteProperty(key);const token=randomToken_();
  p.setProperty('TABLET_ADMIN_SESSION_'+r.deviceId,JSON.stringify({scope:'TABLET_ADMIN_ONLY',tokenHash:digest_(token),
    userId:u.id,userRevision:u.updatedAt,deviceId:r.deviceId,expiresAt:now+900000}));
  appendAudit_(auditEvent_(u.id,'TABLET_ADMIN_AUTH_SUCCESS','',r.deviceId));
  return {success:true,tabletAdminToken:token,expiresInSeconds:900,scope:'TABLET_ADMIN_ONLY'};
}
function tabletAdmin_(r,state) {
  if(!/^[A-Za-z0-9_-]{43}$/.test(r.tabletAdminToken||''))fail_('SESSION_EXPIRED');
  const p=props_(),s=JSON.parse(p.getProperty('TABLET_ADMIN_SESSION_'+r.deviceId)||'{}');
  if(s.scope!=='TABLET_ADMIN_ONLY'||s.deviceId!==r.deviceId||!(s.expiresAt>Date.now())||
    !equal_(s.tokenHash,digest_(r.tabletAdminToken)))fail_('SESSION_EXPIRED');
  const u=state.users.find(u=>u.id===s.userId);
  if(!u||!u.active||u.updatedAt!==s.userRevision||!hasTabletAdmin_(u.id))fail_('TABLET_ADMIN_REQUIRED');
  const key='TABLET_ADMIN_RATE_'+r.deviceId,t=JSON.parse(p.getProperty(key)||'{"start":0,"count":0}');
  if(Date.now()-t.start>=60000){t.start=Date.now();t.count=0;}
  if(++t.count>60)fail_('RATE_LIMITED');p.setProperty(key,JSON.stringify(t));return u;
}
function publicTablet_(id,t) {
  return {deviceId:id,alias:typeof t.alias==='string'&&t.alias.trim()?t.alias:'Tablet sin nombre',
    active:t.active===true&&t.revokedExplicitly!==true,scheme:t.scheme||'legacy-random-v1',createdAt:t.createdAt||null,
    createdBy:t.createdBy||null,revokedAt:t.revokedAt||null,revokedBy:t.revokedBy||null};
}
function tabletAdminAction_(r,state) {
  const actor=tabletAdmin_(r,state),p=props_();
  if(r.action==='listTablets') {
    const all=p.getProperties();return {success:true,tablets:Object.keys(all).filter(k=>/^TABLET_[a-f0-9]{64}$/.test(k))
      .sort().map(k=>publicTablet_(k.slice(7),JSON.parse(all[k])))};
  }
  const id=typeof r.targetDeviceId==='string'?r.targetDeviceId.trim().toLowerCase():'';
  if(!/^[a-f0-9]{64}$/.test(id))fail_('INVALID_REQUEST');
  if(r.action==='getTabletAudit')return {success:true,events:readAudit_().filter(e=>String(e[2]).indexOf('TABLET_')===0&&e[4]===id)
    .map(e=>({timestamp:e[0],actor:e[1],action:e[2],alias:e[3],deviceId:e[4]}))};
  const key='TABLET_'+id,old=p.getProperty(key),t=old?JSON.parse(old):{};let token=null,action;
  if(r.action==='provisionTabletAdmin') {
    if(old)fail_('TABLET_EXISTS');
    if(typeof r.alias!=='string'||!r.alias.trim()||r.alias.trim().length>120)fail_('INVALID_REQUEST');
    token=randomToken_();Object.assign(t,{tokenHash:digest_(token),active:true,scheme:'android-id-v2',alias:r.alias.trim(),
      createdAt:Date.now(),createdBy:actor.id});action='TABLET_PROVISIONED';
  } else {
    if(!old||!/^[a-f0-9]{64}$/.test(t.tokenHash||''))fail_('INVALID_REQUEST');
    if(r.action==='revokeTabletAdmin') {
      if(id===r.deviceId)fail_('TABLET_SELF_REVOKE');
      Object.assign(t,{active:false,revokedExplicitly:true,revokedAt:Date.now(),revokedBy:actor.id});action='TABLET_REVOKED';
    } else if(r.action==='reactivateTabletAdmin') {Object.assign(t,{active:true,revokedExplicitly:false});action='TABLET_REACTIVATED';}
    else if(r.action==='rotateTabletToken') {token=randomToken_();t.tokenHash=digest_(token);t.rotatedAt=Date.now();t.rotatedBy=actor.id;action='TABLET_TOKEN_ROTATED';}
    else fail_('INVALID_REQUEST');
  }
  p.setProperty(key,JSON.stringify(t));p.deleteProperty('SESSION_'+id);p.deleteProperty('TABLET_ADMIN_SESSION_'+id);
  appendAudit_(auditEvent_(actor.id,action,t.alias||'Tablet sin nombre',id));
  const result={success:true,tablet:publicTablet_(id,t)};
  if(token)result.tabletToken=token; // Single response; only its hash is persisted.
  return result;
}
