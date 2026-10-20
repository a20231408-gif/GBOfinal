"""Render every local comparison page for visual inspection; never contacts a service."""
from pathlib import Path
import sys,json,hashlib
ROOT=Path(__file__).resolve().parents[1]
sys.path.insert(0,str(ROOT/'tmp/pavimentos-20261019/python'))
import pymupdf as fitz
from PIL import Image,ImageDraw
out=ROOT/'output/pdf/legibilidad-rigidos-20261019'
render=out/'paginas';render.mkdir(exist_ok=True)
summary=[]
for f in sorted(out.glob('*.pdf')):
    document=fitz.open(f);thumbs=[]
    for i,page in enumerate(document):
        name=render/f'{f.stem}-{i+1:02}.png'
        page.get_pixmap(matrix=fitz.Matrix(1.5,1.5)).save(str(name))
        im=Image.open(name).convert('RGB');im.thumbnail((600,460))
        tile=Image.new('RGB',(620,490),'#dddddd');tile.paste(im,((620-im.width)//2,20))
        ImageDraw.Draw(tile).text((10,470),f'{f.stem} / {i+1}',fill='black');thumbs.append(tile)
    for start in range(0,len(thumbs),6):
        batch=thumbs[start:start+6];canvas=Image.new('RGB',(1240,490*((len(batch)+1)//2)),'white')
        for j,im in enumerate(batch):canvas.paste(im,((j%2)*620,(j//2)*490))
        canvas.save(render/f'{f.stem}-contacto-{start//6+1}.png')
    sizes=[[round(page.rect.width),round(page.rect.height)] for page in document]
    assert all(size==[842,595] for size in sizes)
    summary.append({'file':f.name,'sha256':hashlib.sha256(f.read_bytes()).hexdigest(),'pages':len(document),'sizes':sizes})
(out/'PAGINAS_REVISADAS.json').write_text(json.dumps(summary,indent=2),'utf-8')
expected=set()
for row in summary:
    stem=Path(row['file']).stem
    expected.update(f'{stem}-{i+1:02}.png' for i in range(row['pages']))
    expected.update(f'{stem}-contacto-{i+1}.png' for i in range((row['pages']+5)//6))
archive=ROOT/'tmp/pdf-legibilidad-20261019/paginas-iteraciones-anteriores'
for previous in render.glob('*.png'):
    if previous.name not in expected:
        archive.mkdir(exist_ok=True)
        target=archive/previous.name
        assert previous.resolve().is_relative_to(ROOT.resolve()) and target.resolve().is_relative_to(ROOT.resolve())
        if not target.exists():previous.rename(target) # Preserve evidence instead of mixing stale page images.
print(json.dumps(summary,indent=2))
