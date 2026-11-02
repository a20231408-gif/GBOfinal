/** Policy adapter for a real Users authority. No client roles or shared-upload tokens accepted.
 * Cryptographic proof verification and authoritative live records are mandatory integration ports.
 */
var IndividualAuthority=(()=>{
 function create({proof,users,revocations,clock,audience,issuer}){
  if(!proof?.verifyAndDecode||!users?.read||!revocations?.isRevoked||!clock||!audience||!issuer)throw Error('AUTHORITY_NOT_CONFIGURED');
  return {authorize(token,request){
   const g=proof.verifyAndDecode(token); // Never decode an unverified client payload.
   const now=clock();
   if(!g||g.issuer!==issuer||g.audience!==audience||!g.subject||!g.jti||!g.driveOwnerId||!Number.isSafeInteger(g.expiresAt)||!Number.isSafeInteger(g.issuedAt)||g.issuedAt>now||g.expiresAt<=now||g.operationId!==request.operationId||g.digest!==request.digest||g.key!==request.key||g.revision!==request.revision||g.purpose!==request.purpose)throw Error('GRANT_INVALID');
   if(revocations.isRevoked(g.jti)!==false)throw Error('GRANT_REVOKED_OR_UNKNOWN');
   const u=users.read(g.subject);
   if(!u||u.id!==g.subject||u.active!==true||!Array.isArray(u.administrativeScopes)||!u.administrativeScopes.includes(request.purpose)||!Array.isArray(u.driveOwnerIds)||!u.driveOwnerIds.includes(g.driveOwnerId))throw Error('LIVE_PERMISSION_REQUIRED');
   return {id:u.id,driveOwnerId:g.driveOwnerId};
  }};
 }
 return {create};
})();
if(typeof module!=='undefined')module.exports=IndividualAuthority;
