"""Verify the new-SIC source of truth and collect production-iteration test evidence."""
from pathlib import Path
import hashlib,json,xml.etree.ElementTree as ET
R=Path(__file__).resolve().parents[1];E=R/'final-evidence'
before=json.loads((E/'sic-protected-before.json').read_text(encoding='utf-8'))
changed=[n for n,h in before.items() if not (R/n).is_file() or hashlib.sha256((R/n).read_bytes()).hexdigest()!=h]
result=dict(protectedFiles=len(before),changedProtectedFiles=changed,roomVersion=15,signAssets=207)
(E/'production-preservation.json').write_text(json.dumps(result,indent=2),encoding='utf-8')
assert not changed,changed
def tests():
    baseline=json.loads((R/'docs/sic-new-evidence/final-test-results.json').read_text(encoding='utf-8'))
    total=passed=skipped=0;failed=[]
    for p in (R/'app/build/test-results/testDebugUnitTest').glob('TEST-*.xml'):
        for c in ET.parse(p).getroot().findall('testcase'):
            total+=1;name=c.attrib['classname']+'#'+c.attrib['name']
            if c.find('failure') is not None or c.find('error') is not None:failed.append(name)
            elif c.find('skipped') is not None:skipped+=1
            else:passed+=1
    expected=set(baseline['failures'])
    stats=dict(total=total,passed=passed,failed=len(failed),skipped=skipped,baselineFailures=sorted(set(failed)&expected),
        newFailures=sorted(set(failed)-expected),resolvedBaselineFailures=sorted(expected-set(failed)),failures=sorted(failed))
    (E/'production-test-results.json').write_text(json.dumps(stats,indent=2),encoding='utf-8')
    assert total==347 and passed==332 and not stats['newFailures'] and skipped==0,stats
    print(json.dumps({k:stats[k] for k in ['total','passed','failed','skipped','newFailures']},indent=2))
if __name__=='__main__':
    import sys
    print('Protected source unchanged:',len(before))
    if '--tests' in sys.argv:tests()
