"""Package the verified integration; never include caches, local credentials, keys or inherited APKs."""
from pathlib import Path
import hashlib
import json
import shutil
import zipfile

ROOT=Path(__file__).resolve().parents[1]
DELIVERY=ROOT/'output/final-delivery'
DELIVERY.mkdir(parents=True,exist_ok=True)

def sha(path):return hashlib.sha256(Path(path).read_bytes()).hexdigest()

def source_files():
    excluded_dirs={'.git','.gradle','.kotlin','.idea','build','tmp','output','node_modules','__pycache__','.codex','.agents','.aws'}
    excluded_names={'local.properties','drive.local.properties','server.local.properties','credentials.json','service-account.json'}
    for p in sorted(ROOT.rglob('*')):
        if not p.is_file():continue
        relative=p.relative_to(ROOT)
        if any(part in excluded_dirs for part in relative.parts):continue
        if p.name in excluded_names or p.suffix.lower() in {'.apk','.zip','.keystore','.jks','.p12','.pem','.key','.pyc'}:continue
        if p.name.startswith(('hs_err_pid','replay_pid')):continue
        if p.name.startswith('.env') and p.name!='.env.example':continue
        if not p.resolve().is_relative_to(ROOT):raise ValueError('External source path')
        yield p,relative

def package():
    test=json.loads((ROOT/'docs/final-evidence/TEST_RESULTS.json').read_text())
    assert test['newFailures']==[] and test['integratedMigrationPassed']
    apk=DELIVERY/'GBO_InventarioVial_INTEGRADO_DEBUG.apk'
    content=json.loads((ROOT/'docs/final-evidence/APK_CONTENT_CHECK.json').read_text())
    assert sha(apk)==content['sha256']
    assert 'BUILD SUCCESSFUL' in (ROOT/'docs/final-evidence/final-assemble.log').read_text(encoding='utf-8')
    items=list(source_files())
    inventory={relative.as_posix():{'sha256':sha(p),'bytes':p.stat().st_size} for p,relative in items
               if relative.as_posix()!='docs/final-evidence/FINAL_SOURCE_FILES.json'}
    path=ROOT/'docs/final-evidence/FINAL_SOURCE_FILES.json'
    path.write_text(json.dumps(inventory,indent=2,ensure_ascii=False)+'\n',encoding='utf-8')
    items=list(source_files())
    source=DELIVERY/'GBO_InventarioVial_INTEGRADO_FINAL_SOURCE.zip'
    with zipfile.ZipFile(source,'w',compression=zipfile.ZIP_DEFLATED,compresslevel=6) as archive:
        for p,relative in items:archive.write(p,'GBO_InventarioVial/'+relative.as_posix())
    with zipfile.ZipFile(source) as archive:
        assert archive.testzip() is None
        names=set(archive.namelist())
        prefix='GBO_InventarioVial/'
        required=['app/schemas/com.tuempresa.inventariovial.data.database.InventoryDatabase/14.json',
                  'docs/FINAL_MERGE_PLAN.md','docs/FINAL_INTEGRATION_REPORT.md','docs/FINAL_TABLET_CHECKLIST.md',
                  'docs/DRIVE_V2_SERVER_CHANGE_REQUIRED.md','docs/DRIVE_V2_SERVER.patch','docs/SIC14A_PENDING_DECISION.md',
                  'app/src/main/java/com/tuempresa/inventariovial/core/DrivePaths.kt',
                  'app/src/main/java/com/tuempresa/inventariovial/pavement/PavementMigration.kt',
                  'app/src/main/java/com/tuempresa/inventariovial/auth/UserServiceClient.kt',
                  'backend/user-service/tests/service.test.cjs','gradle/wrapper/gradle-wrapper.jar']
        assert all(prefix+p in names for p in required)
        assert all(not any(part in {'tmp','output','build','.gradle','.idea'} for part in Path(n).parts) for n in names)
        for p,relative in items:
            assert hashlib.sha256(archive.read(prefix+relative.as_posix())).hexdigest()==sha(p)
    for name in ['FINAL_INTEGRATION_REPORT.md','FINAL_TABLET_CHECKLIST.md','DRIVE_V2_SERVER_CHANGE_REQUIRED.md','DRIVE_V2_SERVER.patch','SIC14A_PENDING_DECISION.md']:
        shutil.copy2(ROOT/'docs'/name,DELIVERY/name)
    result={'sourceFileCount':len(items),'sourceZipIntegrityVerified':True,'sourceMatchesWorkspaceHashes':True,
            'artifacts':{p.name:{'sha256':sha(p),'bytes':p.stat().st_size} for p in DELIVERY.iterdir() if p.is_file() and p.name!='DELIVERY_SHA256.json'}}
    (DELIVERY/'DELIVERY_SHA256.json').write_text(json.dumps(result,indent=2,ensure_ascii=False)+'\n',encoding='utf-8')
    print(json.dumps(result,ensure_ascii=False))

if __name__=='__main__':package()
