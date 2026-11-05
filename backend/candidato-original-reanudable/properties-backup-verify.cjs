'use strict';
// Offline validation only. Never print property names, values or source parse errors.
const crypto=require('node:crypto'),fs=require('node:fs');
function canonical(properties){return JSON.stringify(Object.keys(properties).sort().map(k=>[k,properties[k]]));}
function verify(snapshot){
 if(!snapshot||snapshot.schema!==1||typeof snapshot.scriptId!=='string'||!snapshot.scriptId||!snapshot.capturedAt||!Number.isFinite(Date.parse(snapshot.capturedAt)))throw Error('INVALID_ENVELOPE');
 const p=snapshot.properties;
 if(!p||typeof p!=='object'||Array.isArray(p)||Object.values(p).some(v=>typeof v!=='string'))throw Error('INVALID_PROPERTIES');
 if(!Number.isInteger(snapshot.count)||Object.keys(p).length!==snapshot.count)throw Error('INCOMPLETE_PROPERTIES');
 const hash=crypto.createHash('sha256').update(canonical(p),'utf8').digest('hex');
 if(hash!==snapshot.sha256)throw Error('PROPERTY_HASH_MISMATCH');
 return {hash,count:snapshot.count};
}
function compare(a,b){
 const x=verify(a),y=verify(b);
 if(a.scriptId!==b.scriptId)throw Error('DIFFERENT_SCRIPT');
 if(Date.parse(b.capturedAt)<=Date.parse(a.capturedAt))throw Error('NOT_A_LATER_CAPTURE');
 if(x.hash!==y.hash)throw Error('PROPERTIES_CHANGED');
 return {verified:true,count:x.count,remoteProvenanceVerified:false,clientPauseVerified:false};
}
module.exports={canonical,verify,compare};
if(require.main===module){
 try{if(process.argv.length!==4)throw Error('USAGE_TWO_PRIVATE_JSON_FILES');
 const a=JSON.parse(fs.readFileSync(process.argv[2],'utf8')),b=JSON.parse(fs.readFileSync(process.argv[3],'utf8'));
 console.log(JSON.stringify(compare(a,b)));
 }catch(e){const allowed=/^(INVALID_ENVELOPE|INVALID_PROPERTIES|INCOMPLETE_PROPERTIES|PROPERTY_HASH_MISMATCH|DIFFERENT_SCRIPT|NOT_A_LATER_CAPTURE|PROPERTIES_CHANGED|USAGE_TWO_PRIVATE_JSON_FILES)$/;
 console.error(allowed.test(e.message)?e.message:'PRIVATE_INPUT_UNREADABLE');process.exitCode=1;}
}