"""Build the visual guide from approved real captures only; never synthesize UI."""
from pathlib import Path
import hashlib,json,re
from docx import Document
from docx.shared import Inches,Pt,RGBColor
from docx.oxml import OxmlElement
from docx.oxml.ns import qn
from docx.enum.text import WD_ALIGN_PARAGRAPH
from PIL import Image

ROOT=Path(__file__).resolve().parents[1];OUT=ROOT/'docs/manual-10i'
CAP=OUT/'capturas-reales';SOURCE=OUT/'GUIA_VISUAL_GBO_10J.md'
manifest=json.loads((CAP/'manifiesto.json').read_text(encoding='utf-8-sig'))
approved={r['file']:r for r in manifest['capturedAndReviewed'] if r.get('privacyReviewed') is True}
doc=Document();sec=doc.sections[0]
sec.page_width=Inches(8.5);sec.page_height=Inches(11)
sec.top_margin=sec.bottom_margin=Inches(.7);sec.left_margin=sec.right_margin=Inches(.8)
sec.header_distance=sec.footer_distance=Inches(.3);sec.different_first_page_header_footer=True
for border in list(doc.styles.element.iter(qn('w:pBdr'))):border.getparent().remove(border)
for name,size in [('Normal',11),('Title',30),('Subtitle',15),('Heading 1',17),('Heading 2',13),('Caption',9)]:
    st=doc.styles[name];st.font.name='Arial';st.font.size=Pt(size);st.font.color.rgb=RGBColor(0,0,0)
    st.font.underline=False;st.paragraph_format.space_after=Pt(7);st.paragraph_format.line_spacing=1.06
    if name.startswith('Heading'):st.paragraph_format.keep_with_next=True;st.paragraph_format.space_before=Pt(12)
doc.core_properties.author='GBO';doc.core_properties.last_modified_by='GBO'
doc.core_properties.title='Guía visual de GBO Inventario Vial';doc.core_properties.subject='Procedimientos de campo 10J'
doc.core_properties.keywords='GBO SIC SCAP Pavimentos 10J'
def field(p,code):
    r=p.add_run();start=OxmlElement('w:fldChar');start.set(qn('w:fldCharType'),'begin');r._r.append(start)
    t=OxmlElement('w:instrText');t.set(qn('xml:space'),'preserve');t.text=' '+code+' ';r._r.append(t)
    end=OxmlElement('w:fldChar');end.set(qn('w:fldCharType'),'end');r._r.append(end)
def paragraph(text,style=None):
    p=doc.add_paragraph(style=style)
    for i,part in enumerate(re.split(r'\*\*(.*?)\*\*',text)):p.add_run(part).bold=i%2==1
    if re.match(r'^\d+\. ',text):p.paragraph_format.left_indent=Inches(.2);p.paragraph_format.first_line_indent=Inches(-.2)
    return p
doc.add_paragraph('GBO Inventario Vial',style='Title')
doc.add_paragraph('Guía visual de campo',style='Subtitle')
paragraph('SIC   Puentes SCAP   Pavimentos')
paragraph('Edición 10J\n14 de octubre de 2026\nPara ingenieros de campo y administradores autorizados')
paragraph('Registrar, revisar y confirmar la entrega de evidencias desde la tablet.')
paragraph('Posiciones de capturas preparadas. El propietario obtendrá y trabajará las imágenes reales por separado; esta edición conserva sus ubicaciones sin imágenes de sustitución.')
doc.add_page_break();doc.add_heading('Índice',0);field(doc.add_paragraph(),'TOC \\o "1-1" \\h \\z \\u');doc.add_page_break()
header=sec.header.paragraphs[0];header.text='GBO Inventario Vial   |   Guía de campo 10J';header.style='Caption'
footer=sec.footer.paragraphs[0];footer.alignment=WD_ALIGN_PARAGRAPH.RIGHT
footer.add_run('Página ');field(footer,'PAGE');footer.add_run(' de ');field(footer,'NUMPAGES')
lines=SOURCE.read_text(encoding='utf-8').splitlines();i=0;used=[];pending=[]
while i<len(lines):
    line=lines[i].strip();i+=1
    if not line or line.startswith('# ') or line.startswith('Guía de campo visual'):continue
    if line.startswith('## '):doc.add_heading(line[3:],1);continue
    if line.startswith('### '):doc.add_heading(line[4:],2);continue
    cap=re.fullmatch(r'\[CAPTURA:([^|]+)\|([^]]+)\]',line)
    if cap:
        name,caption=cap.groups();row=approved.get(name);file=(CAP/name).resolve()
        if row:
            assert file.parent==CAP.resolve() and file.is_file(),'Capture outside approved directory or missing'
            assert hashlib.sha256(file.read_bytes()).hexdigest()==row['sha256'],'Capture hash mismatch'
            with Image.open(file) as im:w,h=im.size
            p=doc.add_paragraph();p.alignment=WD_ALIGN_PARAGRAPH.CENTER;p.paragraph_format.keep_with_next=True
            p.add_run().add_picture(str(file),width=Inches(min(6.7,5.3*w/h)))
            props=p._p.xpath('.//wp:docPr')
            if props:props[0].set('descr',caption)
            paragraph('Captura real · '+caption,'Caption');used.append(name)
        else:
            paragraph('Captura real pendiente · '+name+'\n'+caption,'Caption');pending.append(name)
        continue
    if line.startswith('|'):
        rows=[line]
        while i<len(lines) and lines[i].strip().startswith('|'):rows.append(lines[i].strip());i+=1
        data=[[c.strip() for c in r.strip('|').split('|')] for r in rows if not re.fullmatch(r'[| :\-]+',r)]
        table=doc.add_table(rows=0,cols=2);table.autofit=False
        table.columns[0].width=Inches(2.1);table.columns[1].width=Inches(4.6)
        for ri,cells in enumerate(data):
            row=table.add_row()
            for ci,text in enumerate(cells):
                cell=row.cells[ci];cell.width=Inches(2.1 if ci==0 else 4.6);cell.text=text
                for p in cell.paragraphs:
                    p.paragraph_format.space_after=Pt(7);p.paragraph_format.keep_with_next=ri==0
                    for run in p.runs:run.font.size=Pt(10.5);run.bold=ri==0
                if ri==0:
                    fill=OxmlElement('w:shd');fill.set(qn('w:fill'),'EAEAEA');cell._tc.get_or_add_tcPr().append(fill)
            pr=row._tr.get_or_add_trPr();pr.append(OxmlElement('w:cantSplit'))
            if ri==0:pr.append(OxmlElement('w:tblHeader'))
        doc.add_paragraph();continue
    paragraph(line)
update=OxmlElement('w:updateFields');update.set(qn('w:val'),'true');doc.settings.element.append(update)
doc.save(OUT/'GUIA_VISUAL_GBO_10J.docx')
(OUT/'estado-guia-visual-10j.json').write_text(json.dumps({'realCapturesIncluded':used,'pendingCaptures':pending,'uiImagesSynthesized':False,'annotationsAddedWithoutRealImage':False,'previousManualOverwritten':False},ensure_ascii=False,indent=2),encoding='utf-8')
print(json.dumps({'realCaptures':len(used),'pendingCaptures':len(pending)}))
