"""Verify and package the latest iteration. Never read/emit operational secret values."""
from pathlib import Path
import hashlib,json,os,re,shutil,zipfile,sys
from PIL import Image
R=Path(__file__).resolve().parents[1]
E=R/'docs/tablet-admin-stability-scap-evidence'
O=R/'output/tablet-admin-stability-scap'
DOCS=['TABLET_ADMIN_IMPLEMENTATION_REPORT.md','TABLET_ADMIN_SERVER_DEPLOY.md',
      'SESSION_LIFECYCLE_AUDIT.md','APP_EXIT_DIAGNOSTIC_REPORT.md','SCAP_BRIDGE_F_G_AUDIT.md',
      'SCAP_TEMPLATE_INCONSISTENCIES.md','SCAP_F1_F2_G1_CORRECTION_REPORT.md',
      'SCAP_BRIDGE_TABLET_CHECKLIST.md','FINAL_TABLET_CHECKLIST_THIS_ITERATION.md']
def sha(p):return hashlib.sha256(p.read_bytes()).hexdigest()
def main():
    baseline=json.loads((E/'baseline.json').read_text(encoding='utf-8'))
    tests=json.loads((E/'final-tests.json').read_text(encoding='utf-8'))
    assert not tests['newFailures'] and not tests['missingBaselineCases']
    assert len(tests['addedCases'])>=10
    assert all(tests['cases'][c]=='PASSED' for c in tests['addedCases'])
    assert set(baseline['cases'])<=set(tests['cases'])
    assert 'BUILD SUCCESSFUL' in (E/'assemble-debug.log').read_text(encoding='utf-8',errors='replace')
    node=(E/'tablet-admin-tests.log').read_text(encoding='utf-8',errors='replace')
    assert '41' in node and re.search(r'(?:fail|failed)\s+0',node,re.I)
    identity=json.loads((E/'debug-identity.json').read_text(encoding='utf-8-sig'))
    if isinstance(identity,list):identity=identity[0]
    assert identity['package']=='com.tuempresa.inventariovial' and identity['versionCode']==14
    assert identity['verified'] and identity['debuggable'] and not identity['isSplit'] and not identity['isTest']
    assert identity['zipAligned'] and identity['pageAligned16KiB']
    assert identity['certificateSha256']==['d2e54cb086aa731ef046be300977d0b16c0d8f66fdcb08d0d9cfd2038bf0d872']
    before=json.loads((E/'source-before.json').read_text(encoding='utf-8'))
    assert all((R/k).is_file() for k in before), 'Received source was removed'
    assert all(sha(R/k)==v for k,v in before.items() if k.startswith('app/schemas/'))
    with zipfile.ZipFile(E/'received-v13-source.zip') as z:
        old=z.read('app/build.gradle.kts').decode('utf-8').replace('\r\n','\n')
    current=(R/'app/build.gradle.kts').read_text(encoding='utf-8')
    assert old.replace('val productionVersionCode = 13','val productionVersionCode = 14')==current
    config=(R/'app/build/generated/source/buildConfig/debug/com/tuempresa/inventariovial/BuildConfig.java').read_text(encoding='utf-8')
    assert all(re.search(n+r'\s*=\s*""',config) for n in ['DRIVE_API_TOKEN','DRIVE_WEB_APP_URL','SERVER_BASE_URL'])
    O.mkdir(parents=True,exist_ok=True)
    apk=O/'GBO_InventarioVial_TABLET_ADMIN_STABILITY_SCAP_DEBUG_v14.apk'
    assert apk.is_file() and sha(apk)==identity['apkSha256']
    # Local operational configuration is excluded. Compare known local values without printing them.
    secrets=[]
    for name in ['drive.local.properties','server.local.properties','keystore.properties']:
        p=R/name
        if p.is_file():
            for line in p.read_text(encoding='utf-8').splitlines():
                if '=' in line and not line.lstrip().startswith('#'):
                    key,value=line.split('=',1)
                    if re.search('TOKEN|PASSWORD|SECRET|PIN|RANDOM_KEY',key,re.I) and len(value.strip())>=8:
                        secrets.extend([value.strip().encode(),value.strip().encode('utf-16le')])
    signs=json.loads((R/'app/src/main/assets/signs/sign_assets_manifest.json').read_text(encoding='utf-8'))
    assert len(signs)==207
    templates={}
    with zipfile.ZipFile(apk) as z:
        assert z.testzip() is None
        for s in signs:
            p=R/'app/src/main/assets/signs'/s['asset']
            assert sha(p)==s['sha256'] and hashlib.sha256(z.read('assets/signs/'+s['asset'])).hexdigest()==s['sha256']
        for kind,asset in [('LOSA','scap_slab_template.xlsx'),('MODULAR','scap_modular_template.xlsx')]:
            source=Path('D:/Downloads')/f'FICHA SCAP - GBO modelo - {kind}.xlsx'
            packaged=R/'docs/reference'/source.name
            assert sha(source)==sha(packaged)==hashlib.sha256(z.read('assets/'+asset)).hexdigest()
            templates[kind]=sha(source)
        assert not any(n.startswith(('backend/','tools/')) for n in z.namelist())
        for name in z.namelist():
            if not name.endswith('/'):
                data=z.read(name)
                assert not any(v in data for v in secrets), 'Operational credential detected in APK'
                if name.endswith('.dex'):
                    assert b'SERVER_RANDOM_KEY' not in data, 'Server owner capability packaged in APK'
    screenshots={}
    for section in ['A','C5','F1','F2','G1']:
        p=E/'screenshots'/f'pending_{section}.png'
        with Image.open(p) as image:
            assert image.width>=800 and image.height>=1100
            screenshots[p.name]=dict(sha256=sha(p),width=image.width,height=image.height)
    result=dict(status='VERIFIED_SOURCE_AND_DEBUG_VALIDATION',versionCode=14,roomVersion=15,
                migrationAdded=False,signingPolicyPreserved=True,apkIdentity=identity,
                templates=templates,signalAssets=207,knownLocalSecretsAbsent=True,serverMasterKeyAbsentFromDex=True,
                privateConfigurationExcluded=True,tests={k:v for k,v in tests.items() if k!='cases'},
                baseline={k:v for k,v in baseline.items() if k!='cases'},nodeScenarios=41,nodeFailures=0,
                nativeScreenshots=screenshots,physicalTabletValidation='PENDING_USER',serverDeployment='PENDING_OWNER')
    (E/'artifact-verification.json').write_text(json.dumps(result,indent=2),encoding='utf-8')
    if '--verify-only' in sys.argv:
        print('APK, templates, signing policy, 207 signals, tests and private configuration: verified.')
        return
    excluded={'.git','.gradle','.kotlin','.idea','build','tmp','output','node_modules','__pycache__','.codex','.agents','.aws'}
    private={'local.properties','keystore.properties','drive.local.properties','server.local.properties','credentials.json','service-account.json'}
    selected=[]
    for directory,dirs,names in os.walk(R,topdown=True,followlinks=False):
        dirs[:]=sorted(d for d in dirs if d not in excluded)
        for name in sorted(names):
            p=Path(directory)/name;rel=p.relative_to(R)
            if name in private or p.suffix.lower() in {'.jks','.keystore','.p12','.pfx','.pem','.key','.apk','.zip','.pyc'}:continue
            if name.startswith(('hs_err_pid','replay_pid')) or name.startswith('.env') and name!='.env.example':continue
            if name in {'complete_scap_ui.py','update_scap_requirement_tests.py'}:continue # One-off edits, not distribution tools.
            if p.suffix.lower()=='.log' and not any(x.endswith('-evidence') for x in rel.parts):continue
            if 'baseline-thread-dump' in name:continue
            assert p.resolve().is_relative_to(R)
            data=p.read_bytes();assert not any(v in data for v in secrets), 'Operational credential detected in source'
            selected.append((p,rel))
    source=O/'GBO_InventarioVial_TABLET_ADMIN_STABILITY_SCAP_SOURCE.zip'
    manifest={rel.as_posix():dict(sha256=sha(p),bytes=p.stat().st_size) for p,rel in selected}
    with zipfile.ZipFile(source,'w',zipfile.ZIP_DEFLATED,compresslevel=6) as z:
        for p,rel in selected:z.write(p,'GBO_InventarioVial/'+rel.as_posix())
        z.writestr('GBO_InventarioVial/SOURCE_FILE_HASHES.json',json.dumps(manifest,indent=2))
    with zipfile.ZipFile(source) as z:
        assert z.testzip() is None
        for p,rel in selected:assert hashlib.sha256(z.read('GBO_InventarioVial/'+rel.as_posix())).hexdigest()==sha(p)
        assert all('GBO_InventarioVial/docs/'+name in z.namelist() for name in DOCS)
        assert not any(Path(n).name in private for n in z.namelist())
    for name in DOCS:shutil.copy2(R/'docs'/name,O/name)
    shutil.copy2(E/'artifact-verification.json',O/'artifact-verification.json')
    result.update(sourceFiles=len(selected),sourceZipIntegrityVerified=True,sourceZip=dict(name=source.name,sha256=sha(source),bytes=source.stat().st_size))
    (O/'hashes.json').write_text(json.dumps(result,indent=2),encoding='utf-8')
    print(json.dumps({k:result[k] for k in ['status','sourceFiles','sourceZip','tests']},indent=2))
if __name__=='__main__':main()
