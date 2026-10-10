"""Independent, read-only inspection of app-generated acceptance artifacts and supplied sources."""
from pathlib import Path
import hashlib,json,re,zipfile,xml.etree.ElementTree as ET
import openpyxl
from PIL import Image
from pypdf import PdfReader
import pypdfium2 as pdfium

ROOT=Path(__file__).resolve().parents[1]
OUT=ROOT/'docs/acceptance-audit-v15-2026-10-10'
EVIDENCE=OUT/'evidence'
def sha(p):return hashlib.sha256(p.read_bytes()).hexdigest()
def write(name,value):
    (EVIDENCE/name).write_text(json.dumps(value,ensure_ascii=False,indent=2,default=str)+'\n',encoding='utf-8')

def workbook(p):
    w=openpyxl.load_workbook(p,read_only=False,data_only=False)
    result={'sheets':w.sheetnames,'sheet_details':[]}
    for s in w:
        formulas=[{'cell':c.coordinate,'formula':c.value} for row in s for c in row if c.data_type=='f']
        refs=[x for x in formulas if '#REF!' in x['formula']]
        result['sheet_details'].append({'name':s.title,'dimensions':s.calculate_dimension(),
          'formula_count':len(formulas),'broken_formula_refs':refs,
          'hidden_rows':[k for k,v in s.row_dimensions.items() if v.hidden],
          'merges':[str(m) for m in s.merged_cells.ranges],
          'sample_nonblank':[(c.coordinate,c.value,c.data_type) for row in s for c in row if c.value is not None][:20]})
    with zipfile.ZipFile(p) as z:
        result['zip_crc_valid']=z.testzip() is None
        result['media_count']=len([n for n in z.namelist() if n.startswith('xl/media/')])
        for n in z.namelist():
            if n.endswith(('.xml','.rels')): ET.fromstring(z.read(n))
        result['all_xml_well_formed']=True
        root=ET.fromstring(z.read('xl/workbook.xml'))
        result['broken_defined_names']=[e.attrib.get('name') for e in root.iter() if e.tag.endswith('definedName') and '#REF!' in (e.text or '')]
    return result

def artifacts():
    results=[]
    for p in sorted((OUT/'scenarios').rglob('*')):
        if not p.is_file():continue
        entry={'file':p.relative_to(OUT).as_posix(),'size':p.stat().st_size,'sha256':sha(p)}
        try:
            if p.suffix=='.xlsx':entry['inspection']=workbook(p)
            elif p.suffix.lower() in ('.jpg','.png'):
                with Image.open(p) as im:
                    im.load();entry['inspection']={'decoded_format':im.format,'dimensions':im.size,'jpeg_extension_matches':p.suffix.lower()!='.jpg' or im.format=='JPEG','exif_tags':list(im.getexif().keys())}
            elif p.suffix=='.pdf':
                doc=pdfium.PdfDocument(str(p));text='\n'.join(page.get_textpage().get_text_range() for page in doc)
                entry['inspection']={'pages':len(doc),'text':text}
                if len(doc):doc[0].render(scale=1.5).to_pil().save(p.with_name(p.stem+'-page1.png'))
            elif p.suffix=='.json':entry['inspection']={'json_valid':True};json.loads(p.read_text(encoding='utf-8'))
            elif p.suffix=='.zip':
                with zipfile.ZipFile(p) as z:
                    entry['inspection']={'zip_crc_valid':z.testzip() is None,'members':z.namelist()}
                    if 'manifest.json' in z.namelist():
                        m=json.loads(z.read('manifest.json'));entry['inspection']['manifest']=m
                        entry['inspection']['manifest_hashes_match']=all(hashlib.sha256(z.read(f['path'])).hexdigest()==f['sha256'] for f in m.get('files',[]))
        except Exception as exc:entry['inspection_error']=f'{type(exc).__name__}: {exc}'
        results.append(entry)
    write('artifact-inspection.json',results)
    print('Artifacts',len(results),'inspection errors',sum('inspection_error' in e for e in results))

def sources():
    results=[]
    for p in sorted((ROOT/'docs/reference').glob('*.xlsx')):
        try:results.append({'file':p.relative_to(ROOT).as_posix(),'sha256':sha(p),'inspection':workbook(p)})
        except Exception as exc:results.append({'file':str(p),'error':str(exc)})
    write('reference-workbooks.json',results)
    references=[]
    for name in ('MAN_8 IV-2014_2015.pdf','MAN_6 DCT-2016.pdf'):
        p=Path('D:/Downloads')/name
        if not p.is_file():references.append({'file':str(p),'available':False});continue
        doc=pdfium.PdfDocument(str(p)); found=[]
        for index,page in enumerate(doc):
            text=page.get_textpage().get_text_range()
            codes=sorted(set(re.findall(r'SIC[-\s]*(?:08|09|13|14|15|16|17[AaBb]?|18[Aa]?|19|20|21|22|23|34)\b',text)))
            if codes:found.append({'pdf_page':index+1,'codes':codes,'text':text})
        references.append({'file':str(p),'sha256':sha(p),'pdf_pages':len(doc),'sic_pages':found})
        # Render two actual source pages containing SIC titles for visual verification.
        chosen=[f for f in found if len(f['codes'])<=3][:2]
        for f in chosen:doc[f['pdf_page']-1].render(scale=1.25).to_pil().save(EVIDENCE/f'{name[:5]}-page-{f["pdf_page"]}.png')
    write('official-manuals.json',references)
    print('Reference workbooks',len(results),'official manuals',[(r['file'],r.get('pdf_pages')) for r in references])

def contract_sources():
    sources=[]
    paths=['core/DrivePaths.kt','core/DriveAssetPathRouter.kt','core/CleanPhotoPolicy.kt','delivery/DeliverySync.kt','delivery/RouteDelivery.kt',
      'delivery/DeliveryPhotoNameBuilder.kt','field/PhotoCategoryPolicy.kt','pavement/PavementDrive.kt','pavement/PavementModels.kt',
      'pavement/PavementCalculation.kt','pavement/PavementSic.kt','sic/SicFieldFormats.kt','export/SicExportFormat.kt',
      'scap/export/ScapDelivery.kt','scap/export/ScapDriveSync.kt','scap/export/ScapExcelExporter.kt','scap/export/ScapCanonicalExporter.kt',
      'scap/domain/ScapBridgeType.kt','data/database/InventoryDatabase.kt']
    base=ROOT/'app/src/main/java/com/tuempresa/inventariovial'
    for rel in paths:
        p=base/rel
        sources.append({'file':p.relative_to(ROOT).as_posix(),'sha256':sha(p),
          'lines':[{'line':i,'text':line} for i,line in enumerate(p.read_text(encoding='utf-8').splitlines(),1)
          if re.search(r'SYNCED|logicalPath|pathVersion|SCAP_BUNDLE|SIC\d|\bSIC-|version =|addMigrations|MIGRATION_|GENERAL|PANORAMICA|ENTRADA|SALIDA|PAVIMENTOS|QUEUED|fingerprint|20|ORIGINAL_CLEAN',line) and 'token' not in line.lower()]})
    write('implementation-source-index.json',sources)

if __name__=='__main__':
    import sys
    {'artifacts':artifacts,'sources':sources,'contracts':contract_sources}[sys.argv[1]]()
