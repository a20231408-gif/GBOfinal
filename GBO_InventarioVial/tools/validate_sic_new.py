"""Offline final evidence for the new SIC iteration; original baseline identities remain authoritative."""
from pathlib import Path
import hashlib,json,re,shutil,xml.etree.ElementTree as ET,zipfile

ROOT=Path(__file__).resolve().parents[1]
E=ROOT/'docs/sic-new-evidence'
def sha(p):return hashlib.sha256(p.read_bytes()).hexdigest()
def write(name,data):(E/name).write_text(json.dumps(data,indent=2,ensure_ascii=False)+'\n',encoding='utf-8')
def tests():
    base=json.loads((E/'baseline.json').read_text(encoding='utf-8'))
    failed=[];passed=[];skipped=[];total=0
    files=list((ROOT/'app/build/test-results/testDebugUnitTest').glob('TEST-*.xml'))
    assert files
    dst=E/'final-tests';dst.mkdir(exist_ok=True)
    for p in files:
        shutil.copy2(p,dst/p.name)
        for c in ET.parse(p).getroot().findall('testcase'):
            total+=1;name=c.attrib['classname']+'#'+c.attrib['name']
            if c.find('failure') is not None or c.find('error') is not None:failed.append(name)
            elif c.find('skipped') is not None:skipped.append(name)
            else:passed.append(name)
    expected=set(base['failures']);actual=set(failed)
    result=dict(total=total,passed=len(passed),failed=len(failed),skipped=len(skipped),baselineFailures=sorted(actual&expected),
        newFailures=sorted(actual-expected),resolvedBaselineFailures=sorted(expected-actual),failures=sorted(failed),
        newTestsPassed=[n for n in passed if any(k in n for k in ['SicNewFormatsTest','SicMigration14To15Test','SignalAssetCatalogTest','TrafficSignVisualSelectorTest'])])
    write('final-test-results.json',result)
    assert not result['newFailures'],result['newFailures']
    assert not skipped and len(result['newTestsPassed'])==24,result
    print(json.dumps({k:result[k] for k in ['total','passed','failed','skipped','newFailures']},indent=2))
def sources():
    before=json.loads((E/'source-before.json').read_text(encoding='utf-8'))
    changed=[n for n,h in before.items() if not (ROOT/n).exists() or sha(ROOT/n)!=h]
    protected=['backend/user-service/','app/src/main/java/com/tuempresa/inventariovial/auth/',
        'app/src/main/java/com/tuempresa/inventariovial/pavement/','app/src/main/java/com/tuempresa/inventariovial/scap/']
    unchanged=[n for n in before if any(n.startswith(prefix) for prefix in protected)]
    assert not set(changed)&set(unchanged)
    original=zipfile.ZipFile(ROOT.parent.parent/'GBO_InventarioVial_INTEGRADO_FINAL_SOURCE.zip')
    dbpath='app/src/main/java/com/tuempresa/inventariovial/data/database/InventoryDatabase.kt'
    a=original.read('GBO_InventarioVial/'+dbpath).decode('utf-8');b=(ROOT/dbpath).read_text(encoding='utf-8')
    def block(s,key):
        start=s.index('val '+key);end=s.index('\n        val ',start+4) if '\n        val ' in s[start+4:] else s.index('\n        @Volatile',start)
        return s[start:end].strip()
    # Braces isolate each migration despite new sibling declarations.
    def migration(s,key):
        start=s.index('val '+key);first=s.index('{',start);depth=0
        for i in range(first,len(s)):
            if s[i]=='{':depth+=1
            elif s[i]=='}':
                depth-=1
                if depth==0:return s[start:i+1].replace('\r\n','\n')
    assert all(migration(a,key)==migration(b,key) for key in ['MIGRATION_12_13','MIGRATION_13_14'])
    schemas=['app/schemas/com.tuempresa.inventariovial.data.database.InventoryDatabase/'+str(v)+'.json' for v in range(4,15)]
    assert all(original.read('GBO_InventarioVial/'+p)==(ROOT/p).read_bytes() for p in schemas)
    after={p.relative_to(ROOT).as_posix():sha(p) for folder in ['app/src','backend','google-apps-script','tools'] for p in (ROOT/folder).rglob('*') if p.is_file() and '__pycache__' not in p.parts}
    added=sorted(set(after)-set(before))
    write('changed-files.json',dict(modified=sorted(changed),added=added,removed=sorted(set(before)-set(after)),protectedFilesUnchanged=len(unchanged),migration12to13Unchanged=True,migration13to14Unchanged=True,existingSchemas4to14Unchanged=True))
    print('Changed existing files:',len(changed),'Added:',len(added),'Protected unchanged:',len(unchanged))
if __name__=='__main__':
    tests();sources()
