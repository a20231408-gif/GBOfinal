/** Repository-only proposal for the Users service. No production adapter or route is enabled.
 * Store methods MUST read live authoritative users/tablets and verify PIN/backend credentials.
 * Nothing supplied by Android (including role, scopes or TABLET_ADMIN token) grants permission.
 */
var InventoryAuthorization10C=(()=>{
  const scopes={ANNUL:'INVENTORY_ANNUL',RESTORE:'INVENTORY_RESTORE'};
  function deny(code){const e=Error(code);e.code=code;throw e;}
  const component=v=>typeof v==='string' && /^[A-Za-z0-9_-]{1,80}$/.test(v);
  function target(p) {
    if(!p || !component(p.recordId) || !component(p.routeCode) || !['SIC','SCAP','PAVEMENT'].includes(p.kind))deny('INVALID_INVENTORY_TARGET');
  }
  function permission(user,route,scope) {
    return user?.active===true && ['ADMIN','OPERATOR'].includes(user.role) &&
      Array.isArray(user.inventoryScopes) && user.inventoryScopes.includes(scope) &&
      Array.isArray(user.inventoryRoutes) && user.inventoryRoutes.includes(route);
  }
  function issue(p,s) {
    target(p);
    const tablet=s.verifyTablet(p.tabletToken,p.fingerprint); // verifies token + live revocation
    if(!tablet?.active || tablet.fingerprint!==p.fingerprint)deny('DEVICE_REVOKED');
    const user=s.verifyPin(p.userId,p.pin); // existing service hash/rate-limit, never a client role
    const allowed=Object.values(scopes).filter(scope=>permission(user,p.routeCode,scope));
    if(!user || user.id!==p.userId || !allowed.length)deny('INVENTORY_PERMISSION_REQUIRED');
    const token=s.secureToken();
    if(typeof token!=='string' || !/^[A-Za-z0-9_-]{43}$/.test(token))deny('AUTHORITY_CONFIGURATION');
    const now=s.now();if(!Number.isSafeInteger(now) || now<0)deny('AUTHORITY_CONFIGURATION');
    const grant={userId:user.id,fingerprint:tablet.fingerprint,kind:p.kind,recordId:p.recordId,routeCode:p.routeCode,
      scopes:allowed,issuedAt:now,expiresAt:now+600000,authorizationVersion:1};
    s.putGrant(s.hash(token),grant); // no plaintext token/PIN in properties or audit logs
    s.audit({action:'INVENTORY_GRANT_ISSUED',userId:user.id,fingerprint:tablet.fingerprint,kind:p.kind,recordId:p.recordId,routeCode:p.routeCode,timestamp:now});
    return {ok:true,inventoryAuthorizationVersion:1,inventoryToken:token,expiresInSeconds:600,scopes:allowed,
      userId:user.id,fingerprint:tablet.fingerprint,kind:p.kind,recordId:p.recordId,routeCode:p.routeCode};
  }
  function authorize(p,s) {
    // This operation is server-to-server only. A shared Drive API token is never accepted here.
    if(s.verifyBackend(p?.backendCredential)!==true)deny('TRUSTED_BACKEND_REQUIRED');
    target(p);
    if(!Object.prototype.hasOwnProperty.call(scopes,p.operation) || !component(p.opId) ||
       !Number.isSafeInteger(p.baseRevision) || p.baseRevision<0 || typeof p.reason!=='string' || p.reason.trim().length<3 || p.reason.length>500 ||
       typeof p.inventoryToken!=='string' || !/^[A-Za-z0-9_-]{43}$/.test(p.inventoryToken))deny('INVALID_INVENTORY_REQUEST');
    const grant=s.getGrant(s.hash(p.inventoryToken));const now=s.now();
    if(!Number.isSafeInteger(now) || !grant || grant.authorizationVersion!==1 || !Number.isSafeInteger(grant.expiresAt) ||
       grant.expiresAt<=now || grant.issuedAt>now)deny('INVENTORY_GRANT_EXPIRED');
    if(grant.userId!==p.userId || grant.fingerprint!==p.fingerprint || grant.kind!==p.kind || grant.recordId!==p.recordId ||
       grant.routeCode!==p.routeCode || !Array.isArray(grant.scopes) || !grant.scopes.includes(scopes[p.operation]))deny('INVENTORY_GRANT_SCOPE');
    const tablet=s.tabletByFingerprint(grant.fingerprint);
    if(!tablet?.active || tablet.fingerprint!==grant.fingerprint)deny('DEVICE_REVOKED');
    const user=s.user(grant.userId);
    if(user?.id!==grant.userId || !permission(user,p.routeCode,scopes[p.operation]))deny('INVENTORY_PERMISSION_REQUIRED');
    // This is authorization only, never an inventory delivery/lifecycle ACK.
    return {ok:true,inventoryAuthorizationVersion:1,userId:grant.userId,fingerprint:grant.fingerprint,
      operation:p.operation,opId:p.opId,kind:p.kind,recordId:p.recordId,routeCode:p.routeCode,
      baseRevision:p.baseRevision,checkedAt:now,expiresAt:grant.expiresAt};
  }
  return {issue,authorize};
})();
if(typeof module!=='undefined')module.exports=InventoryAuthorization10C;
