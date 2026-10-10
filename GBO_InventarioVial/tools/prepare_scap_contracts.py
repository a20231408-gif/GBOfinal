"""Read-only workbook extraction; updates JSON capture metadata, never rewrites XLSX."""
from pathlib import Path
import json,hashlib
import openpyxl
R=Path(__file__).resolve().parents[1]
for kind,asset in [('LOSA','scap_slab_catalog.json'),('MODULAR','scap_modular_catalog.json')]:
    source=Path('D:/Downloads')/f'FICHA SCAP - GBO modelo - {kind}.xlsx'
    w=openpyxl.load_workbook(source,data_only=False)
    f=w['F.1- CONDICION GLOBAL'];g=w['G.- CONDICION ESTADISTICA'];photo=w['F.2.-PANEL FOTOGRAFICO']
    p=R/'app/src/main/assets'/asset;catalog=json.loads(p.read_text(encoding='utf-8-sig'))
    assert catalog['sourceSha256']==hashlib.sha256(source.read_bytes()).hexdigest()
    group=None;descriptors=[]
    groups=['SUPERESTRUCTURA','SUBESTRUCTURA','DETALLES','CAUCE','ACCESOS'];gi=-1
    for row in range(21,45):
        code=f[f'B{row}'].value;name=f[f'C{row}'].value
        if isinstance(code,str) and any(code.strip().startswith(x) for x in ['I.','II.','III.','IV.','V.']):gi+=1;group=groups[gi]
        if not isinstance(code,(int,float)) or not isinstance(name,str):continue
        matching=[r for r in range(14,45) if str(g[f'B{r}'].value or '').strip()==name.strip()]
        target=matching[0] if len(matching)==1 else None
        existing=next((e for e in catalog['elements'] if e['code']==str(int(code))),None)
        d=dict(stableId=f'{kind}:F1:{row}',templateCode=str(int(code)),exportCode=str(int(g[f'A{target}'].value)) if target else str(int(code)),
            defaultName=name.strip(),globalRow=row,statisticalRow=target,group=group,
            unit=g[f'E{target}'].value if target else (existing or {}).get('unit',''),
            importanceFactor=g[f'F{target}'].value if target else (existing or {}).get('importanceFactor'))
        descriptors.append(d)
        if existing:existing.update(name=d['defaultName'],group=group,unit=d['unit'],importanceFactor=d['importanceFactor'] or 0)
    catalog['canonicalElements']=descriptors
    catalog['defaultElements']=[d['templateCode'] for d in descriptors]
    catalog['photoNames']=list(dict.fromkeys(str(photo[f'D{r}'].value).split(':',1)[0].strip() for r in range(11,43) if photo[f'D{r}'].value))
    p.write_text(json.dumps(catalog,ensure_ascii=False,indent=2)+'\n',encoding='utf-8')
    print(kind,len(descriptors),'F1 elements',len(catalog['photoNames']),'unique photo names')
