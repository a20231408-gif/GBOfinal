/** Server-only integration hook. Call BEFORE v3Store/recovery and again inside shared ScriptLock.
 * Resolver must read authoritative metadata without mutation; never accept client-supplied resolved keys.
 * No deployment or router wiring is performed by this module.
 */
var AdminWriteFence=(()=>{
 function create({store,resolveIdentities}){
  if(!store?.fenced||typeof resolveIdentities!=='function')throw Error('WRITE_FENCE_NOT_CONFIGURED');
  return {assertAllowed(action,request){
   const identities=resolveIdentities(action,request);
   if(!Array.isArray(identities)||!identities.length||identities.some(k=>typeof k!=='string'||!/^(SIC|SCAP|PAVEMENT)\/[A-Za-z0-9_-]{1,80}$/.test(k)))throw Error('AUTHORITATIVE_IDENTITY_REQUIRED');
   for(const key of new Set(identities))if(store.fenced(key))throw Error('ADMINISTRATIVE_FENCE');
   return true;
  }};
 }
 return {create};
})();
if(typeof module!=='undefined')module.exports=AdminWriteFence;
