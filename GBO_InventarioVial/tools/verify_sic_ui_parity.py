"""Snapshot the received v12 source and exact test identities; audit the parity iteration."""
from pathlib import Path
import hashlib, json, sys, xml.etree.ElementTree as ET, zipfile, difflib
R=Path(__file__).resolve().parents[1]
E=R/'docs/sic-ui-parity-evidence'
def sha(p): return hashlib.sha256(p.read_bytes()).hexdigest()
def cases():
    result={}
    for p in (R/'app/build/test-results/testDebugUnitTest').glob('TEST-*.xml'):
        for c in ET.parse(p).getroot().findall('testcase'):
            result[c.attrib['classname']+'#'+c.attrib['name']]='FAILED' if c.find('failure') is not None or c.find('error') is not None else 'SKIPPED' if c.find('skipped') is not None else 'PASSED'
    return result
def snapshot():
    E.mkdir(parents=True,exist_ok=True)
    assert not (E/'source-before.json').exists(), 'Do not replace the received baseline'
    files=[p for folder in ['app/src','app/schemas','backend','google-apps-script'] for p in (R/folder).rglob('*') if p.is_file()]
    files += [R/'app/build.gradle.kts',R/'tools/apk_identity_audit.ps1',R/'.gitignore']
    before={p.relative_to(R).as_posix():sha(p) for p in files}
    previous=json.loads((R/'docs/ui-unification-evidence/full-test-results.json').read_text(encoding='utf-8'))['cases']
    baseline=cases() or previous
    assert baseline==previous and len(baseline)==362 and sum(v=='FAILED' for v in baseline.values())==15
    with zipfile.ZipFile(R/'output/ui-unification/GBO_InventarioVial_UI_UNIFICADA_SOURCE.zip') as z:
        for n,h in before.items():
            assert hashlib.sha256(z.read('GBO_InventarioVial/'+n)).hexdigest()==h, 'Received source differs from v12: '+n
    (E/'source-before.json').write_text(json.dumps(before,indent=2),encoding='utf-8')
    (E/'baseline.json').write_text(json.dumps(dict(sourceVersionCode=12,origin='Verified v12 full-test-results.json; source hashes compared with the delivered v12 ZIP; build directory currently absent',cases=baseline),indent=2),encoding='utf-8')
    with zipfile.ZipFile(E/'received-ui-source.zip','w',zipfile.ZIP_DEFLATED) as z:
        for p in files:z.write(p,p.relative_to(R).as_posix())
    print('Received v12: %d source files; exact baseline 362 tests, 347 passed, 15 historical failures.' % len(files))
def verify():
    before=json.loads((E/'source-before.json').read_text(encoding='utf-8'))
    changed=[n for n,h in before.items() if not (R/n).is_file() or sha(R/n)!=h]
    allowed={'app/build.gradle.kts','app/src/main/java/com/tuempresa/inventariovial/MainActivity.kt','app/src/main/java/com/tuempresa/inventariovial/sic/SicFieldWorkspace.kt','app/src/main/java/com/tuempresa/inventariovial/field/CulvertUnifiedScreen.kt','app/src/main/java/com/tuempresa/inventariovial/field/FieldUi.kt','app/src/main/java/com/tuempresa/inventariovial/field/CulvertPhotos.kt'}
    allowed.add('app/src/main/java/com/tuempresa/inventariovial/field/SurveyPreferences.kt')
    assert set(changed)<=allowed, changed
    reference=E/'release-config-reference.json'
    if (E/'received-ui-source.zip').exists():
        with zipfile.ZipFile(E/'received-ui-source.zip') as z:
            old=z.read('app/build.gradle.kts').decode('utf-8').replace('\r\n','\n')
            expected=old.replace('val productionVersionCode = 12','val productionVersionCode = 13')
            assert (R/'app/build.gradle.kts').read_text(encoding='utf-8')==expected
            if not reference.exists():reference.write_text(json.dumps(dict(receivedVersionCode=12,versionCode=13,expectedNormalizedSha256=hashlib.sha256(expected.encode()).hexdigest(),change='Only versionCode 12 → 13; release certificate, gates, signing and applicationId unchanged'),indent=2),encoding='utf-8')
            diff=[]
            for n in changed:
                diff+=list(difflib.unified_diff(z.read(n).decode('utf-8').replace('\r\n','\n').splitlines(True),(R/n).read_text(encoding='utf-8').splitlines(True),fromfile='v12/'+n,tofile='parity/'+n))
            (E/'source-diff.patch').write_text(''.join(diff),encoding='utf-8')
    else:
        assert hashlib.sha256((R/'app/build.gradle.kts').read_text(encoding='utf-8').encode()).hexdigest()==json.loads(reference.read_text(encoding='utf-8'))['expectedNormalizedSha256']
    result=dict(receivedFiles=len(before),unchangedFiles=len(before)-len(changed),changedFiles=changed,roomVersion=15,migrationAdded=False,signingUnchanged=True,versionCode=13,protectedHashesUnchanged=True)
    (E/'preservation.json').write_text(json.dumps(result,indent=2),encoding='utf-8')
    print(json.dumps(result,indent=2))
    return result
def tests():
    baseline=json.loads((E/'baseline.json').read_text(encoding='utf-8'))['cases'];after=cases()
    failed={n for n,s in after.items() if s=='FAILED'};historical={n for n,s in baseline.items() if s=='FAILED'}
    result=dict(total=len(after),passed=sum(s=='PASSED' for s in after.values()),failed=len(failed),skipped=sum(s=='SKIPPED' for s in after.values()),baselineTotal=len(baseline),newFailures=sorted(failed-historical),missingBaselineCases=sorted(set(baseline)-set(after)),addedCases=sorted(set(after)-set(baseline)),baselineFailures=sorted(failed&historical),cases=after)
    (E/'full-test-results.json').write_text(json.dumps(result,indent=2),encoding='utf-8')
    assert not result['newFailures'] and not result['missingBaselineCases'] and not result['skipped']
    assert result['addedCases'] and all(after[n]=='PASSED' for n in result['addedCases'])
    print(json.dumps({k:v for k,v in result.items() if k!='cases'},indent=2))
    return result
def visuals():
    from PIL import Image, ImageChops
    names=['A_Alcantarilla_SIC18','B_Faja_SIC08_09','C_PuntoCritico_SIC13','D_Accidente_SIC34']
    folder=E/'screenshots'
    images=[Image.open(folder/(n+'.png')).convert('RGB') for n in names]
    bounds=[json.loads((folder/(n+'.bounds.json')).read_text(encoding='utf-8')) for n in names]
    comparisons=[]
    for name,im,b in zip(names[1:],images[1:],bounds[1:]):
        assert im.size==images[0].size==(800,1200)
        for key in bounds[0]:
            a=[float(x) for x in bounds[0][key].split(',')];actual=[float(x) for x in b[key].split(',')]
            assert (a[0],a[1],a[3])==(actual[0],actual[1],actual[3]) if key=='title' else a==actual, (name,key,a,actual)
        for region in [(0,0,800,150),(0,220,800,670)]:
            assert ImageChops.difference(images[0].crop(region),im.crop(region)).getbbox() is None, (name,region)
        comparisons.append(dict(screen=name,boundsIdentical=True,titleOriginAndHeightIdentical=True,commonButtonAndSurveyHeaderPixelsIdentical=True))
    result=dict(reference=names[0],capture='Native Android Canvas drawing the actual production Compose view in Robolectric API 34, 800x1200 mdpi; no physical device',comparisons=comparisons,screenshots={n:dict(sha256=sha(folder/(n+'.png')),bytes=(folder/(n+'.png')).stat().st_size) for n in names},note='Title/subtitle and technical fields differ intentionally; linear elements reuse the existing TrackCapturePanel, absent for a point culvert.')
    (E/'visual-parity.json').write_text(json.dumps(result,indent=2),encoding='utf-8')
    print(json.dumps(result,indent=2))
    return result
if __name__=='__main__':
    if '--snapshot' in sys.argv:snapshot()
    else:
        verify()
        if '--tests' in sys.argv:tests()
        if '--visuals' in sys.argv:visuals()
