/* Proposal only. No Drive calls, credentials, roots or deployment side effects. */
var PavementSicChronologyV1=(()=>{
 const formats=['SIC-14','SIC-14A','SIC-15','SIC-16'];
 const validDate=s=>typeof s==='string' && /^\d{4}-\d{2}-\d{2}$/.test(s) && Number.isFinite(Date.parse(s+'T00:00:00.000Z')) && new Date(s+'T00:00:00.000Z').toISOString().slice(0,10)===s;
 const valid=r=>validDate(r.registeredDate) && Number.isSafeInteger(r.registeredAt) && r.registeredAt>0;
 const text=(a,b)=>a===b?0:a<b?-1:1;
 const compare=(a,b)=>text(a.registeredDate,b.registeredDate)||a.registeredAt-b.registeredAt||text(a.recordId,b.recordId);
 const ready=()=>typeof SyncV3!=='undefined' && SyncV3.PAVEMENT_CHRONOLOGY_VERSION===1;
 return {VERSION:1,formats,validDate,valid,compare,ready};
})();
