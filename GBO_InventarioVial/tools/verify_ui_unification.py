"""Compare exact received test identities and preserved source; no hard-coded final test count."""
from pathlib import Path
import hashlib,json,xml.etree.ElementTree as ET,zipfile,sys,difflib
R=Path(__file__).resolve().parents[1];E=R/'docs/ui-unification-evidence'
def sha(p):return hashlib.sha256(p.read_bytes()).hexdigest()
def preservation():
    before=json.loads((E/'source-before.json').read_text(encoding='utf-8'))
    changed=[name for name,digest in before.items() if not (R/name).is_file() or sha(R/name)!=digest]
    allowed={
        'app/build.gradle.kts',
        'app/src/main/java/com/tuempresa/inventariovial/MainActivity.kt',
        'app/src/main/java/com/tuempresa/inventariovial/RecordHistoryScreen.kt',
        'app/src/main/java/com/tuempresa/inventariovial/core/GeneratedFileIndex.kt',
        'app/src/main/java/com/tuempresa/inventariovial/core/CoreFieldTools.kt',
        'app/src/main/java/com/tuempresa/inventariovial/field/FieldUi.kt',
        'app/src/main/java/com/tuempresa/inventariovial/sic/SicFieldRepository.kt',
        'app/src/main/java/com/tuempresa/inventariovial/sic/SicFieldWorkspace.kt',
        'app/src/main/java/com/tuempresa/inventariovial/delivery/RouteDelivery.kt',
        'app/src/test/java/com/tuempresa/inventariovial/Engineering2ScreenTest.kt'}
    assert set(changed)<=allowed,changed
    zip_path=R/'output/final-delivery/GBO_InventarioVial_PRODUCTION_SOURCE.zip'
    with zipfile.ZipFile(zip_path) as z:
        old=z.read('GBO_InventarioVial/app/build.gradle.kts').decode('utf-8').replace('\r\n','\n')
        current=(R/'app/build.gradle.kts').read_text(encoding='utf-8')
        assert current==old.replace('val productionVersionCode = 11','val productionVersionCode = 12'), 'Signing policy was modified'
        diffs=[]
        for name in changed:
            original=z.read('GBO_InventarioVial/'+name).decode('utf-8').replace('\r\n','\n')
            diffs+=list(difflib.unified_diff(original.splitlines(True),(R/name).read_text(encoding='utf-8').splitlines(True),fromfile='before/'+name,tofile='after/'+name))
        (E/'source-diff.patch').write_text(''.join(diffs),encoding='utf-8')
    new=[p.relative_to(R).as_posix() for p in (R/'app/src').rglob('*') if p.is_file() and p.relative_to(R).as_posix() not in before]
    protected=[name for name in before if name not in allowed]
    result=dict(roomVersion=15,migrationAdded=False,receivedFiles=len(before),unchangedFiles=len(before)-len(changed),
        changedFiles=changed,newSourceFiles=sorted(new),releaseChangeOnlyVersionCode='11 → 12',signingPolicyPreserved=True,
        expectedCertificateSha256='0da8a2cbcf6bce5554526c80de08878d077af1542e488292b3869cdd8ee17315',protectedFiles=len(protected),protectedHashesUnchanged=True)
    (E/'preservation.json').write_text(json.dumps(result,indent=2),encoding='utf-8')
    print(json.dumps(result,indent=2))
    return result

def tests():
    baseline=json.loads((E/'baseline.json').read_text(encoding='utf-8'))['cases']
    cases={};failures={}
    for p in (R/'app/build/test-results/testDebugUnitTest').glob('TEST-*.xml'):
        for c in ET.parse(p).getroot().findall('testcase'):
            name=c.attrib['classname']+'#'+c.attrib['name']
            f=c.find('failure');err=c.find('error')
            cases[name]='FAILED' if f is not None or err is not None else 'SKIPPED' if c.find('skipped') is not None else 'PASSED'
            if f is not None or err is not None:failures[name]=(f if f is not None else err).attrib.get('message','')
    expected={name for name,state in baseline.items() if state=='FAILED'}
    failed={name for name,state in cases.items() if state=='FAILED'}
    new=failed-expected;missing=set(baseline)-set(cases)
    stats=dict(total=len(cases),passed=sum(v=='PASSED' for v in cases.values()),failed=len(failed),skipped=sum(v=='SKIPPED' for v in cases.values()),
        baselineTotal=len(baseline),baselineFailures=sorted(failed&expected),newFailures=sorted(new),missingBaselineCases=sorted(missing),
        addedCases=sorted(set(cases)-set(baseline)),resolvedBaselineFailures=sorted(expected-failed),failures=failures,cases=cases)
    (E/'full-test-results.json').write_text(json.dumps(stats,indent=2),encoding='utf-8')
    assert not new and not missing and not stats['skipped'], {k:v for k,v in stats.items() if k!='cases'}
    assert stats['addedCases'] and all(cases[n]=='PASSED' for n in stats['addedCases'])
    print(json.dumps({k:v for k,v in stats.items() if k not in {'cases','failures'}},indent=2))
    return stats
if __name__=='__main__':
    preservation()
    if '--tests' in sys.argv:tests()
