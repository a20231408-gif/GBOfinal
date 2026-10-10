"""Package the same base checkout with Parallel B changes; exclude machine secrets/caches."""
from pathlib import Path
import difflib, hashlib, json, zipfile, os
import xml.etree.ElementTree as ET

ROOT=Path(__file__).resolve().parents[1]
BASE=ROOT/'output/parallel-b/baseline/source'

def test_results(path):
    tests=0;failures=[]
    for file in sorted(path.glob('*.xml')):
        tree=ET.parse(file).getroot();tests+=int(tree.get('tests','0'))
        for case in tree.findall('testcase'):
            if case.find('failure') is not None:
                failures.append({'class':case.get('classname'),'name':case.get('name')})
    return {'tests':tests,'failureCount':len(failures),'failures':failures}

def evidence():
    before=json.loads((ROOT/'docs/PARALLEL_B_BASELINE.json').read_text(encoding='utf-8'))
    after=test_results(ROOT/'app/build/test-results/testDebugUnitTest')
    key=lambda x:(x['class'],x['name'])
    after['newFailures']=[x for x in after['failures'] if key(x) not in {key(b) for b in before['failures']}]
    after['baselineTests']=before['tests'];after['baselineFailureCount']=len(before['failures'])
    after['assembleDebug']='SUCCESS' if 'BUILD SUCCESSFUL' in (ROOT/'output/parallel-b/assemble.log').read_text(encoding='utf-8',errors='replace') else 'FAILED'
    (ROOT/'docs/PARALLEL_B_VALIDATION.json').write_text(json.dumps(after,ensure_ascii=False,indent=2),encoding='utf-8')
    changed=[];diff=[]
    candidates=list((ROOT/'app/src').rglob('*'))+list((ROOT/'app/schemas').rglob('*'))+[ROOT/'app/build.gradle.kts']
    for file in candidates:
        if not file.is_file():continue
        relative=file.relative_to(ROOT);old=BASE/relative
        if old.exists() and old.read_bytes()==file.read_bytes():continue
        changed.append({'path':relative.as_posix(),'change':'MODIFIED' if old.exists() else 'ADDED','sha256':hashlib.sha256(file.read_bytes()).hexdigest()})
        if file.suffix in {'.kt','.kts','.json','.xml','.md','.txt'}:
            original=old.read_text(encoding='utf-8-sig').splitlines(keepends=True) if old.exists() else []
            current=file.read_text(encoding='utf-8-sig').splitlines(keepends=True)
            diff.extend(difflib.unified_diff(original,current,fromfile='a/'+relative.as_posix(),tofile='b/'+relative.as_posix()))
    (ROOT/'docs/PARALLEL_B_FILES.json').write_text(json.dumps(changed,ensure_ascii=False,indent=2),encoding='utf-8')
    (ROOT/'docs/PARALLEL_B_CHANGES.diff').write_text(''.join(diff),encoding='utf-8')
    print(json.dumps({'tests':after['tests'],'failures':after['failureCount'],'newFailures':after['newFailures'],'assemble':after['assembleDebug'],'changedAppFiles':len(changed)},ensure_ascii=False))
    return after

def package():
    result=evidence()
    assert not result['newFailures'], 'No package with new regression failures'
    assert result['assembleDebug']=='SUCCESS', 'No package with failed assembly'
    target=ROOT/'GBO_PARALLEL_B_INVENTARIO_PUENTES.zip'
    excluded={'.gradle','.idea','.kotlin','.git','build','output','tmp','node_modules','__pycache__','.agents','.codex'}
    machine={'local.properties','drive.local.properties','server.local.properties'}
    paths=[]
    for directory,dirs,files in os.walk(ROOT,topdown=True,followlinks=False):
        dirs[:]=[name for name in dirs if name not in excluded]
        for name in files:
            p=Path(directory)/name
            if name not in machine and p.suffix not in {'.zip','.log','.hprof'}:paths.append(p)
    with zipfile.ZipFile(target,'w',compression=zipfile.ZIP_DEFLATED,compresslevel=6) as z:
        for p in sorted(paths):z.write(p,'GBO_InventarioVial/'+p.relative_to(ROOT).as_posix())
        for name in ['compile.log','tests.log','assemble.log']:
            path=ROOT/'output/parallel-b'/name
            if path.exists():z.write(path,'GBO_InventarioVial/verification/'+name)
        for name in ['tests.log','assemble.log']:
            path=ROOT/'output/parallel-b/baseline'/name
            if path.exists():z.write(path,'GBO_InventarioVial/verification/baseline/'+name)
    with zipfile.ZipFile(target) as z:
        assert z.testzip() is None
        assert not any(Path(n).name in machine for n in z.namelist())
        print(f'{target.name}: {len(z.namelist())} files; {target.stat().st_size} bytes; CRC verified')

if __name__=='__main__':package()
