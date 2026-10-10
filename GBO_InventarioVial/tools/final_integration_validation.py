"""Reproduce the final baseline, protected-source and APK DEX checks. No remote access."""
from pathlib import Path
import argparse
import hashlib
import json
import re
import struct
import xml.etree.ElementTree as ET
import zipfile

ROOT = Path(__file__).resolve().parents[1]
EVIDENCE = ROOT / 'docs/final-evidence'
EVIDENCE.mkdir(parents=True, exist_ok=True)

def sha(path):
    return hashlib.sha256(Path(path).read_bytes()).hexdigest()

def write(name, data):
    (EVIDENCE / name).write_text(json.dumps(data, indent=2, ensure_ascii=False) + '\n', encoding='utf-8')

def tests():
    baseline = json.loads((ROOT / 'docs/PARALLEL_B_BASELINE.json').read_text(encoding='utf-8'))
    expected = {(r['class'], r['name']) for r in baseline['failures']}
    failures, passed_names, total, skipped = [], [], 0, 0
    files = list((ROOT / 'app/build/test-results/testDebugUnitTest').glob('TEST-*.xml'))
    assert files, 'Missing test results'
    for path in files:
        for case in ET.parse(path).getroot().findall('testcase'):
            total += 1
            identity = {'class': case.attrib['classname'], 'name': case.attrib['name']}
            if case.find('skipped') is not None:
                skipped += 1
            elif case.find('failure') is not None or case.find('error') is not None:
                failures.append(identity)
            else:
                passed_names.append(identity)
    actual = {(r['class'], r['name']) for r in failures}
    new = sorted(actual - expected)
    missing = sorted(expected - actual)
    result = dict(total=total, passed=total-len(failures)-skipped, failed=len(failures), skipped=skipped,
                  newFailures=new, baselineFailures=sorted(actual & expected), missingBaselineFailures=missing,
                  gradleResult='BUILD FAILED' if failures else 'BUILD SUCCESSFUL', failures=failures,
                  integratedMigrationPassed=any(r['class'].endswith('IntegratedMigration12To14Test') for r in passed_names),
                  draftPersistencePassed=any(r['class'].endswith('InventoryDraftStoreTest') for r in passed_names))
    write('TEST_RESULTS.json', result)
    assert not new, f'New failures: {new}'
    assert result['integratedMigrationPassed'] and result['draftPersistencePassed']
    print(json.dumps({k:result[k] for k in ['total','passed','failed','skipped','newFailures','gradleResult']}))

def source():
    staging = ROOT / 'tmp/final-merge'
    assert staging.is_dir(), 'Original branch staging needed for hash audit'
    def files(root):
        return {p.relative_to(root).as_posix(): sha(p) for p in root.rglob('*') if p.is_file()}
    protected = {}
    for directory in ['app/src/main/java/com/tuempresa/inventariovial/auth', 'backend/user-service']:
        a, final = files(staging / 'A' / directory), files(ROOT / directory)
        protected[directory] = {'exactA': a == final, 'files': len(a)}
        assert a == final, directory
    wrapper = {name:sha(ROOT/name) == sha(staging/'B'/name) for name in
               ['gradlew','gradlew.bat','gradle/wrapper/gradle-wrapper.jar','gradle/wrapper/gradle-wrapper.properties']}
    assert all(wrapper.values())
    references = files(ROOT / 'docs/reference')
    original_refs = files(staging / 'B/docs/reference')
    retained = all(references.get(k) == v for k,v in original_refs.items())
    assert retained, 'Canonical B source references changed'
    schema = json.loads((ROOT / 'app/schemas/com.tuempresa.inventariovial.data.database.InventoryDatabase/14.json').read_text())
    assert schema['database']['version'] == 14
    db = (ROOT / 'app/src/main/java/com/tuempresa/inventariovial/data/database/InventoryDatabase.kt').read_text(encoding='utf-8')
    assert 'fallbackToDestructiveMigration' not in db
    migrations = [f'MIGRATION_{i}_{i+1}' for i in range(1,14)]
    assert all(m in db for m in migrations)
    build_configs=list((ROOT/'app/build/generated').rglob('BuildConfig.java'))
    config=next(p for p in build_configs if '/buildConfig/debug/' in p.as_posix()).read_text()
    assert all(re.search(rf'{name}\s*=\s*""',config) for name in ['DRIVE_API_TOKEN','DRIVE_WEB_APP_URL','SERVER_BASE_URL'])
    write('SOURCE_AUDIT.json', dict(protected=protected, wrapperExactB=wrapper, canonicalReferencesExactB=retained,
                                   roomVersion=14, schemaIdentityHash=schema['database']['identityHash'],
                                   registeredMigrations=migrations, noDestructiveMigration=True, validationCredentialsOmitted=True))
    print('Source audit PASS: auth/backend A, wrapper/references B, Room 14, empty validation credentials.')

def dex_classes(data):
    u32=lambda offset:struct.unpack_from('<I', data, offset)[0]
    assert data[:4] == b'dex\n'
    strings=[]
    for i in range(u32(56)):
        p=u32(u32(60)+4*i)
        while data[p]&128:p+=1
        p+=1
        end=data.index(b'\0',p)
        strings.append(data[p:end].decode('utf-8',errors='replace'))
    types=[strings[u32(u32(68)+4*i)] for i in range(u32(64))]
    return [types[u32(u32(100)+32*i)] for i in range(u32(96))]

def apk(path):
    classes=[]
    with zipfile.ZipFile(path) as archive:
        assert archive.testzip() is None
        dex=[n for n in archive.namelist() if re.fullmatch(r'classes\d*\.dex',n)]
        for name in dex:classes.extend(dex_classes(archive.read(name)))
        templates={name:hashlib.sha256(archive.read(name)).hexdigest() for name in archive.namelist()
                   if name in ['assets/scap_modular_template.xlsx','assets/scap_slab_template.xlsx']}
    prefix='Lcom/tuempresa/inventariovial/'
    modules={m:len([c for c in classes if c.startswith(prefix+m+'/')]) for m in ['core','scap','pavement']}
    required=[prefix+n+';' for n in ['core/InventoryDraftStore','core/DrivePaths','core/FieldCameraActivity',
                                    'core/PavementSyncWorker','core/GeneratedFileIndex','pavement/PavementMigration',
                                    'pavement/PavementDriveUploader','data/database/InventoryDatabase_Impl']]
    assert all(modules.values()) and all(c in classes for c in required)
    assert len(templates)==2
    for name,source in [('assets/scap_modular_template.xlsx','FICHA SCAP - GBO modelo - MODULAR.xlsx'),
                        ('assets/scap_slab_template.xlsx','FICHA SCAP - GBO modelo - LOSA.xlsx')]:
        assert templates[name]==sha(ROOT/'docs/reference'/source), 'APK template differs from canonical source'
    write('APK_CONTENT_CHECK.json',dict(apk=str(Path(path).resolve()),sha256=sha(path),sizeBytes=Path(path).stat().st_size,
           dexFiles=dex,classDefinitions=len(classes),moduleClassDefinitions=modules,requiredClasses=required,canonicalTemplates=templates))
    print('APK DEX PASS: '+json.dumps(modules)+', SHA-256 '+sha(path))

if __name__=='__main__':
    parser=argparse.ArgumentParser();parser.add_argument('--tests',action='store_true');parser.add_argument('--source',action='store_true');parser.add_argument('--apk',type=Path)
    args=parser.parse_args()
    if args.tests:tests()
    if args.source:source()
    if args.apk:apk(args.apk)
