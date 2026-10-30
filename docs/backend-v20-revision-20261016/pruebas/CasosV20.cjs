// Synthetic records only. Uses existing Android-generated XLSX/JPEG fixtures.
const fs=require('node:fs'),path=require('node:path');
const {ROOT,hash,zipParts}=require('./HarnessV20.cjs');
const clone=x=>JSON.parse(JSON.stringify(x));
function payload(id='TEST_V20_A',pr=10){
 const p=JSON.parse(fs.readFileSync(path.join(ROOT,'docs/drive-familias-2a/examples/culvert-a/payload.json'),'utf8'));
 Object.assign(p,{recordId:id,key:'SIC/'+id,revision:1,baseRevision:0,startPrNumber:pr,startDistanceM:0,orderAxis:pr*1000});
 p.sicRows.forEach(r=>Object.assign(r,{recordId:id,startPrNumber:pr,startDistanceM:0,orderAxis:pr*1000}));
 for(const f of p.files)if(f.key.startsWith('photo/')){f.key='photo/'+id;f.photoCategory='CULVERT_INLET';f.path='ENTRADA/F_'+hash(id).slice(0,16)+'.jpg';}
 const bytes=Buffer.from(JSON.stringify({synthetic:true,recordId:id}));
 const f=p.files.find(f=>f.key==='metadata');if(f)Object.assign(f,{base64:bytes.toString('base64'),sha256:hash(bytes)});
 return p;
}
function rewrite(row,fn){
 const parts=zipParts('unzip',row.base64);fn(parts);
 const bytes=Buffer.from(zipParts('zip',parts),'base64');Object.assign(row,{base64:bytes.toString('base64'),sha256:hash(bytes)});return row;
}
const escape=s=>s.replace(/&/g,'&amp;').replace(/</g,'&lt;');
function annotated(p,text){
 for(const r of p.sicRows)rewrite(r,parts=>{
  const f=parts.find(f=>f.name==='xl/worksheets/sheet1.xml');let s=Buffer.from(f.base64,'base64').toString();
  const cell=(at,t)=>`<c r="${at}" s="3" t="inlineStr"><is><t xml:space="preserve">${escape(t)}</t></is></c>`;
  s=s.replace(/(<row\b[^>]*r="13"[^>]*>)([\s\S]*?)(<\/row>)/,(_,a,b,c)=>a+b+cell('N13','OBSERVACIONES')+c)
   .replace(/(<row\b[^>]*r="14"[^>]*>)([\s\S]*?)(<\/row>)/,(_,a,b,c)=>a+b+cell('N14',text)+c)
   .replace(/(<dimension ref="[^"]*:)[A-Z]+14"/,'$1N14"');
  f.base64=Buffer.from(s).toString('base64');
 });return p;
}
function sample(id='TEST_V20_PAV',surface='ASFALTICO',rows=1){
 const p=payload(id);Object.assign(p,{kind:'PAVEMENT',key:'PAVEMENT/'+id,family:'MUESTRA',tramo:'TRAMO_TEST',surface});
 for(const k of ['familyPath','technicalDeliveryVersion','orderSource','georeferencing','technicalDeliveryWithoutGps','photoCategoryVersion','photoHasEnds','photoClassCode','photoTypeCode'])delete p[k];
 for(const f of p.files)if(f.key.startsWith('photo/')){f.path='PANEL_FOTOGRAFICO/'+f.path.split('/').pop();f.photoCategory='GENERAL';}
 const bytes=fs.readFileSync(path.join(ROOT,'docs/drive-familias-2a/examples/pavement-a/SIC-14.xlsx'));
 // Format/surface protocol coverage; the SIC-14 fixture is not a claim of SIC-15 contractual layout.
 const format={ASFALTICO:'SIC-14',RIGIDO:'SIC-15',NO_PAVIMENTADO:'SIC-16'}[surface];
 p.sicRows=Array.from({length:rows},(_,i)=>({recordId:id+'_D'+i,format,orderAxis:10000+i,startPrNumber:10,startDistanceM:i,direction:'INCREASING',roadbedCode:'CD',sha256:hash(bytes),base64:bytes.toString('base64')}));return p;
}
function bridge(id='TEST_V20_SCAP'){
 const p=payload(id);Object.assign(p,{kind:'SCAP',key:'SCAP/'+id,family:'PUENTE',familyPath:'PUENTES',identityId:id+'_BRIDGE',sicRows:[]});
 for(const f of p.files){if(f.key.startsWith('photo/')){f.path='SCAP/'+id+'/GENERAL/'+f.path.split('/').pop();f.photoCategory='GENERAL';}else f.path='SCAP/'+id+'/manifest.json';}
 return p;
}
function reserve(e,p){
 const a=e.context.reservePhotoNamesV2({...clone(p),photoNamingVersion:2,photos:p.files.filter(f=>f.key.startsWith('photo/')).map(f=>({id:f.key.slice(6),category:f.photoCategory}))});
 if(!a.ok)throw Error(JSON.stringify(a));p.photoNamingVersion=2;
 for(const f of p.files.filter(f=>f.key.startsWith('photo/')))f.path=f.path.slice(0,f.path.lastIndexOf('/')+1)+a.photos.find(x=>x.id===f.key.slice(6)).name;
 return a;
}
function parts(e,a){return zipParts('unzip',Buffer.from(e.objects.get(a.sicFiles[0].fileId).getBlob().getBytes()).toString('base64'));}
function sheet(e,a){return Buffer.from(parts(e,a).find(x=>x.name==='xl/worksheets/sheet1.xml').base64,'base64').toString();}
function manifests(e){return [...e.objects.values()].filter(f=>f.parent?.name==='._GBO_SYNC_V3'&&/^record_[a-f0-9]{64}\.json$/.test(f.name)).map(f=>JSON.parse(f.getBlob().getDataAsString()));}
module.exports={payload,sample,bridge,rewrite,annotated,reserve,clone,parts,sheet,manifests};
