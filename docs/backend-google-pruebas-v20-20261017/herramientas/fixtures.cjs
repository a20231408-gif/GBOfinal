// Local build only. Existing synthetic Android XLSX/JPEG fixtures; no real records.
const fs=require('node:fs'),path=require('node:path');
const here=path.resolve(__dirname,'..'),repo=path.resolve(here,'../..');
const c=require(path.join(repo,'docs/backend-v20-revision-20261016/pruebas/CasosV20.cjs'));
const {hash}=require(path.join(repo,'docs/backend-v20-revision-20261016/pruebas/HarnessV20.cjs'));
const out=path.join(here,'paquete/ejecutor/fixtures');fs.mkdirSync(out,{recursive:true});
function fix(p){p.routeCode='TEST_AUTO_BACKEND';delete p.contentHash;delete p.token;
  const meta=p.files.find(f=>f.key==='metadata');if(meta){const b=Buffer.from(JSON.stringify({synthetic:true,recordId:p.recordId,operator:p.recordId.endsWith('B')?'TEST_CLIENT_149':'TEST_CLIENT_147',...(p.kind==='PAVEMENT'?{sicData:{_gboJointAnnotations:{version:1,marks:[{kind:'V',row:0,column:1}]}}}:{})}));meta.base64=b.toString('base64');meta.sha256=hash(b);}
  for(const r of p.sicRows)c.rewrite(r,parts=>{for(const x of parts)if(x.name.endsWith('.xml'))x.base64=Buffer.from(Buffer.from(x.base64,'base64').toString().replace(/culvert-a/g,p.recordId).replace(/engineer-a/g,'USUARIO_FICTICIO')).toString('base64');});
  return p;
}
const list=[['A',1],['B',2],['C',4],['D',3]].map(([n,pr])=>fix(c.payload('TEST_SIC_'+n,pr)));
list[0]=c.annotated(list[0],'=1+1 PINTADO OK 57 E2');
list.push(fix(c.sample('TEST_PAV_A','ASFALTICO',3)),fix(c.sample('TEST_PAV_B','ASFALTICO',2)),fix(c.bridge('TEST_SCAP_A')),fix(c.bridge('TEST_SCAP_B')));
// Valid minimal PDF for byte/hash transport tests. Not a native Android rendering test.
const objects=['<< /Type /Catalog /Pages 2 0 R >>','<< /Type /Pages /Kids [3 0 R] /Count 1 >>','<< /Type /Page /Parent 2 0 R /MediaBox [0 0 300 150] /Resources << /Font << /F1 4 0 R >> >> /Contents 5 0 R >>','<< /Type /Font /Subtype /Type1 /BaseFont /Helvetica >>'];
const stream='BT /F1 12 Tf 20 80 Td (TEST AUTO BACKEND - DATOS FICTICIOS) Tj ET';objects.push(`<< /Length ${Buffer.byteLength(stream)} >>\nstream\n${stream}\nendstream`);
let pdf='%PDF-1.4\n',offsets=[0];for(let i=0;i<objects.length;i++){offsets.push(Buffer.byteLength(pdf));pdf+=`${i+1} 0 obj\n${objects[i]}\nendobj\n`;}
const xref=Buffer.byteLength(pdf);pdf+=`xref\n0 ${objects.length+1}\n0000000000 65535 f \n`+offsets.slice(1).map(x=>String(x).padStart(10,'0')+' 00000 n \n').join('')+`trailer\n<< /Size ${objects.length+1} /Root 1 0 R >>\nstartxref\n${xref}\n%%EOF\n`;
for(const p of list){if(p.kind==='PAVEMENT'||p.kind==='SCAP'){const b=Buffer.from(pdf);p.files.push({key:'report',path:p.kind==='SCAP'?`SCAP/${p.recordId}/informe.pdf`:'informe.pdf',mimeType:'application/pdf',sha256:hash(b),base64:b.toString('base64')});}}
// Observation edit, legacy edit (column absent), explicit blank: same UUID/revision chain.
const legacy=fix(c.payload('TEST_SIC_A',1));legacy.revision=2;legacy.baseRevision=1;
const blank=c.annotated(fix(c.payload('TEST_SIC_A',1)),'');blank.revision=3;blank.baseRevision=2;
fs.writeFileSync(path.join(out,'records.json'),JSON.stringify({records:list,legacyEdit:legacy,blankEdit:blank},null,2));
// Tiny transport-only opaque binary; actual playable media is deliberately a separate gate.
// Never call it a real encoded video: the runner requires owner-provided playable .mp4.
fs.writeFileSync(path.join(out,'TRANSPORT_ONLY.bin'),Buffer.from('TEST_AUTO_BACKEND_OPAQUE_VIDEO_BYTES_NOT_A_PLAYABLE_MP4\n'));
console.log('8 fictitious parents, 2 edits, existing synthetic XLSX/JPEG, test PDF. Playable MP4 still required for Google acceptance.');
