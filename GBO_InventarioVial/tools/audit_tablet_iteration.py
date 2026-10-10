"""Fresh source/test evidence and read-only extraction of the supplied SCAP contracts."""
from pathlib import Path
import hashlib, json, sys, zipfile, xml.etree.ElementTree as ET
import openpyxl
R=Path(__file__).resolve().parents[1]
E=R/'docs/tablet-admin-stability-scap-evidence'
def sha(p): return hashlib.sha256(p.read_bytes()).hexdigest()
def cases():
    out={}
    for p in (R/'app/build/test-results/testDebugUnitTest').glob('TEST-*.xml'):
        for c in ET.parse(p).getroot().findall('testcase'):
            out[c.attrib['classname']+'#'+c.attrib['name']]='FAILED' if c.find('failure') is not None or c.find('error') is not None else 'SKIPPED' if c.find('skipped') is not None else 'PASSED'
    return out
def snapshot():
    E.mkdir(parents=True,exist_ok=True)
    assert not (E/'source-before.json').exists()
    files=[p for d in ['app/src','app/schemas','backend','google-apps-script'] for p in (R/d).rglob('*') if p.is_file()]+[R/'app/build.gradle.kts']
    hashes={p.relative_to(R).as_posix():sha(p) for p in files}
    (E/'source-before.json').write_text(json.dumps(hashes,indent=2),encoding='utf-8')
    with zipfile.ZipFile(E/'received-v13-source.zip','w',zipfile.ZIP_DEFLATED) as z:
        for p in files: z.write(p,p.relative_to(R).as_posix())
    print('Captured actual v13 source:',len(files),'files; no Git repository.')
def tests(baseline=False):
    c=cases(); assert c
    result=dict(total=len(c),passed=sum(v=='PASSED' for v in c.values()),failed=sum(v=='FAILED' for v in c.values()),cases=c)
    if not baseline:
        before=json.loads((E/'baseline.json').read_text(encoding='utf-8'))['cases']
        result.update(newFailures=[n for n,v in c.items() if v=='FAILED' and before.get(n)!='FAILED'],missingBaselineCases=sorted(set(before)-set(c)),addedCases=sorted(set(c)-set(before)))
    (E/('baseline.json' if baseline else 'final-tests.json')).write_text(json.dumps(result,indent=2),encoding='utf-8')
    print(json.dumps({k:v for k,v in result.items() if k!='cases'},indent=2))
def templates():
    result={}
    for kind in ['LOSA','MODULAR']:
        p=Path('D:/Downloads')/f'FICHA SCAP - GBO modelo - {kind}.xlsx'
        w=openpyxl.load_workbook(p,data_only=False)
        data=dict(source=str(p),sha256=sha(p),sheets=[])
        for s in w:
            if s.title.startswith(('F.','G.')):
                cells=[dict(cell=c.coordinate,value=c.value) for row in s for c in row if c.value is not None]
                data['sheets'].append(dict(name=s.title,cells=cells,merges=[str(m) for m in s.merged_cells.ranges]))
                print(kind,s.title)
                print('populated cells',len(cells))
        result[kind]=data
    (E/'templates.json').write_text(json.dumps(result,ensure_ascii=False,indent=2,default=str),encoding='utf-8')
if __name__=='__main__':
    if '--snapshot' in sys.argv:snapshot()
    if '--templates' in sys.argv:templates()
    if '--baseline' in sys.argv:tests(True)
    if '--final' in sys.argv:tests()
