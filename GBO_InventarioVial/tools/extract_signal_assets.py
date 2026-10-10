"""Offline MAN_6 extraction. PDF text anchors, embedded raster first, vector crop fallback.

pip install PyMuPDF Pillow
python tools/extract_signal_assets.py --pdf "D:/Downloads/MAN_6 DCT-2016.pdf"
No OCR, no synthesized graphics. Ambiguous mappings remain unverified.
"""
from pathlib import Path
import argparse, base64, hashlib, html, io, json, re
import pymupdf as fitz
from PIL import Image, ImageChops

ROOT = Path(__file__).resolve().parents[1]
CATALOG = ROOT / "app/src/main/java/com/tuempresa/inventariovial/TrafficSignCatalog.kt"
CODE = re.compile(r"\b[RPIT]-\d+(?:-\d+)?[A-Z]?\b")

def headings(page):
    result = []
    for block in page.get_text("dict")["blocks"]:
        if block["type"] != 0:
            continue
        bold = [span for line in block["lines"] for span in line["spans"]
                if span["flags"] & 16]
        text = " ".join(s["text"] for s in bold)
        if not text.strip().startswith("SEÑAL"):
            continue
        text = re.sub(r"([RPIT])-\s+(\d)", r"\1-\2", text)
        codes = list(dict.fromkeys(CODE.findall(text.upper())))
        if codes:
            bottom = max(s["bbox"][3] for s in bold)
            result.append((min(s["bbox"][1] for s in bold), bottom, codes, text))
    return sorted(result)

def thumbnail(image):
    image = image.convert("RGB")
    # Trim blank margins only, preserve actual sign borders and legends.
    delta = ImageChops.difference(image, Image.new("RGB", image.size, "white"))
    box = delta.convert("L").point(lambda v: 255 if v > 24 else 0).getbbox()
    if box:
        image = image.crop(box)
    image.thumbnail((320, 320), Image.Resampling.LANCZOS)
    return image

def extract(pdf, out, audit):
    catalog = re.findall(r'Sign\("([^"]+)",\s*"([^"]+)"', CATALOG.read_text(encoding="utf-8"))
    rows = {code: dict(code=code, name=name, asset=None, source="MAN_6", sourcePrintedPage=None,
                        crop=None, verified=False, reason="Figura no identificada inequívocamente") for code, name in catalog}
    doc = fitz.open(pdf)
    for index in range(80, 129):
        page = doc[index]
        anchors = headings(page)
        images = [im for im in page.get_image_info(xrefs=True)
                  if im["bbox"][1] > 50 and im["bbox"][0] < 270]
        for n, (top, bottom, codes, title) in enumerate(anchors):
            limit = anchors[n+1][0] if n+1 < len(anchors) else 630
            candidates = [im for im in images if im["bbox"][1] >= bottom-3 and im["bbox"][3] < limit
                          and im["bbox"][1] <= bottom+40 and im["bbox"][2]-im["bbox"][0] < 240]
            candidates.sort(key=lambda im: im["bbox"][0])
            if codes == ["I-2A"] and len(candidates) == 3:
                candidates = candidates[:1]  # Explicit national representative, printed p.118.
            for k, code in enumerate(codes):
                if code not in rows or rows[code]["verified"]:
                    continue
                image = None
                if len(candidates) == len(codes):
                    candidate = candidates[k]
                    rect = fitz.Rect(candidate["bbox"])
                    # Embedded images may have corrective vector overlays (e.g. P-25B).
                    # Inspect/extract the original, then preserve the page's composed graphic.
                    if candidate["xref"]:
                        doc.extract_image(candidate["xref"])
                    pix = page.get_pixmap(matrix=fitz.Matrix(3,3), clip=rect, alpha=False)
                    image = Image.frombytes("RGB", (pix.width,pix.height), pix.samples)
                    method = "embedded-bbox-composited-crop"
                elif not candidates and len(codes)==1:
                    # Vector signs in the left column. Isolate their own drawing geometry.
                    left = page.rect.width/2-150 if index % 2 else 60
                    drawings = [fitz.Rect(d["rect"]) for d in page.get_drawings()
                                if d["rect"].y0 > bottom and d["rect"].y1 < limit-2
                                and d["rect"].x0 >= left and d["rect"].x1 <= left+105
                                and d["rect"].width > 12 and d["rect"].height > 12]
                    if drawings:
                        rect = drawings[0]
                        for d in drawings[1:]: rect |= d
                        if rect.width > 25 and rect.height > 25:
                            rect += (-1,-1,1,1)
                            pix=page.get_pixmap(matrix=fitz.Matrix(3,3),clip=rect,alpha=False)
                            image=Image.frombytes("RGB",(pix.width,pix.height),pix.samples)
                            method="vector-page-crop"
                if image is None:
                    rows[code].update(sourcePrintedPage=index, reason="Asociación/crop ambiguo; requiere revisión")
                    continue
                image=thumbnail(image)
                asset=code.lower().replace("-","_")+".webp"
                out.mkdir(parents=True,exist_ok=True)
                image.save(out/asset,"WEBP",lossless=True)
                rows[code].update(asset=asset,sourcePrintedPage=index,sourcePdfPage=index+1,
                    crop=dict(x=round(rect.x0,3),y=round(rect.y0,3),width=round(rect.width,3),height=round(rect.height,3),units="PDF points"),
                    verified=True,method=method,reason="Texto del encabezado y figura única en su sección",
                    sha256=hashlib.sha256((out/asset).read_bytes()).hexdigest(),width=image.width,height=image.height)
                if code=="I-2A": rows[code]["variant"]="Nacional; MAN_6 también muestra Departamental y Vecinal"
    overrides=json.loads((ROOT/"tools/signal_crop_overrides.json").read_text(encoding="utf-8"))
    for code, override in overrides.items():
        assert code in rows
        index=override["printedPage"]
        # Explicitly reviewed caption/crop for combined raster + vector graphics.
        assert code in re.sub(r"\s+", "", doc[index].get_text())
        rect=fitz.Rect(override["rect"])
        pix=doc[index].get_pixmap(matrix=fitz.Matrix(5,5),clip=rect,alpha=False)
        image=thumbnail(Image.frombytes("RGB",(pix.width,pix.height),pix.samples))
        asset=code.lower().replace("-","_")+".webp"
        image.save(out/asset,"WEBP",lossless=True)
        rows[code].update(asset=asset,sourcePrintedPage=index,sourcePdfPage=index+1,
            crop=dict(x=rect.x0,y=rect.y0,width=rect.width,height=rect.height,units="PDF points"),verified=True,
            method="reviewed-composited-page-crop",reason=override["evidence"],
            sha256=hashlib.sha256((out/asset).read_bytes()).hexdigest(),width=image.width,height=image.height)
    manifest=list(rows.values())
    (out/"sign_assets_manifest.json").write_text(json.dumps(manifest,ensure_ascii=False,indent=2),encoding="utf-8")
    # Standalone audit: thumbnails embedded so it also works outside the source tree.
    cards=[]
    for row in manifest:
        encoded=base64.b64encode((out/row["asset"]).read_bytes()).decode("ascii") if row["verified"] else ""
        graphic=f'<img loading="lazy" src="data:image/webp;base64,{encoded}" alt="{html.escape(row["code"]+", "+row["name"])}">' if row["verified"] else '<p class="missing">Imagen no disponible</p>'
        cards.append(f'<article>{graphic}<b>{row["code"]}</b><p>{html.escape(row["name"])}</p><small>MAN_6 p.{row["sourcePrintedPage"] or "—"} · verified={str(row["verified"]).lower()}</small></article>')
    audit.write_text('<!doctype html><html lang="es"><meta charset="utf-8"><title>Auditoría de señales MAN_6</title><style>body{font:15px system-ui;margin:24px;background:#eef2f5}main{display:grid;grid-template-columns:repeat(auto-fill,minmax(190px,1fr));gap:14px}article{background:white;padding:14px;border-radius:8px}img{width:100%;height:150px;object-fit:contain}b{display:block}.missing{height:130px;background:#eee;padding:10px}small{color:#555}</style><h1>Señales extraídas de MAN_6</h1><p>Comparar código, orientación, nombre y página fuente. Solo gráficos del manual. I-2A: variante nacional.</p><main>'+''.join(cards)+'</main></html>',encoding="utf-8")
    print(json.dumps(dict(total=len(manifest),verified=sum(r["verified"] for r in manifest),missing=[r["code"] for r in manifest if not r["verified"]]),indent=2))

if __name__ == "__main__":
    parser=argparse.ArgumentParser()
    parser.add_argument("--pdf",required=True,type=Path)
    args=parser.parse_args()
    extract(args.pdf,ROOT/"app/src/main/assets/signs",ROOT/"docs/SIGNAL_ASSET_AUDIT.html")
