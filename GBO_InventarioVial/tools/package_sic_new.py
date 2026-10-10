"""Package only the validated new SIC source and debug APK; exclude local secrets and build caches."""
from pathlib import Path
import hashlib,json,os,re,runpy,shutil,zipfile

ROOT=Path(__file__).resolve().parents[1]
E=ROOT/'docs/sic-new-evidence'
OUT=ROOT/'output/sic-new'
def sha(path):return hashlib.sha256(path.read_bytes()).hexdigest()
def source_files():
    excluded={'.git','.gradle','.kotlin','.idea','build','tmp','output','node_modules','__pycache__','.codex','.agents','.aws'}
    names={'local.properties','drive.local.properties','server.local.properties','credentials.json','service-account.json'}
    for directory,dirs,files in os.walk(ROOT,topdown=True,followlinks=False):
        dirs[:]=sorted(d for d in dirs if d not in excluded)
        for name in sorted(files):
            p=Path(directory)/name
            rel=p.relative_to(ROOT)
            if p.name in names:continue
            if p.suffix.lower() in {'.apk','.zip','.keystore','.jks','.p12','.pem','.key','.pyc'}:continue
            if p.suffix.lower()=='.log' and p.parent!=E:continue
            if p.name.startswith(('hs_err_pid','replay_pid')) or (p.name.startswith('.env') and p.name!='.env.example'):continue
            assert p.resolve().is_relative_to(ROOT)
            yield p,rel
def main():
    OUT.mkdir(parents=True,exist_ok=True)
    results=json.loads((E/'final-test-results.json').read_text(encoding='utf-8'))
    assert not results['newFailures'] and results['total']==347 and len(results['newTestsPassed'])==24
    assert 'BUILD SUCCESSFUL' in (ROOT/'sic_assemble_debug.log').read_text(encoding='utf-8')
    assert '21 service tests passed.' in (ROOT/'sic_user_service_tests.log').read_text(encoding='utf-8')
    assert re.search(r'pass 39', (ROOT/'sic_apps_script_tests.log').read_text(encoding='utf-8'))
    # Re-read hashes after the final build. No changes to production after the final suite.
    runpy.run_path(str(ROOT/'tools/validate_sic_new.py'))['sources']()
    for src,dst in [('baseline_test_run.log','baseline-gradle.log'),('sic_final_tests.log','final-gradle-tests.log'),
        ('sic_assemble_debug.log','assemble-debug.log'),('sic_apps_script_tests.log','apps-script-tests.log'),('sic_user_service_tests.log','user-service-tests.log')]:
        shutil.copy2(ROOT/src,E/dst)
    apk=OUT/'GBO_InventarioVial_SIC_08_09_13_18A_34_DEBUG.apk'
    shutil.copy2(ROOT/'app/build/outputs/apk/debug/app-debug.apk',apk)
    classes=[];dex_classes=runpy.run_path(str(ROOT/'tools/final_integration_validation.py'))['dex_classes']
    manifest=json.loads((ROOT/'app/src/main/assets/signs/sign_assets_manifest.json').read_text(encoding='utf-8'))
    prefix='Lcom/tuempresa/inventariovial/'
    with zipfile.ZipFile(apk) as z:
        assert z.testzip() is None
        for name in z.namelist():
            if re.fullmatch(r'classes\d*\.dex',name):classes.extend(dex_classes(z.read(name)))
        for row in manifest:
            assert hashlib.sha256(z.read('assets/signs/'+row['asset'])).hexdigest()==row['sha256']
        assert len(manifest)==207 and all(r['verified'] for r in manifest)
        assert not any(n.lower().endswith('.pdf') for n in z.namelist())
        for name,source in [('assets/scap_modular_template.xlsx','FICHA SCAP - GBO modelo - MODULAR.xlsx'),('assets/scap_slab_template.xlsx','FICHA SCAP - GBO modelo - LOSA.xlsx')]:
            assert hashlib.sha256(z.read(name)).hexdigest()==sha(ROOT/'docs/reference'/source)
    required=[prefix+name+';' for name in ['sic/SicFieldRepository','sic/SicFieldFormat','sic/SicDeliveryStatus','field/SignalAssetCatalog','core/InventoryDraftStore','core/DrivePaths','core/GeneratedFileIndex','data/database/InventoryDatabase_Impl','auth/AuthSessionManager','pavement/PavementMigration']]
    assert all(name in classes for name in required)
    config=(ROOT/'app/build/generated/source/buildConfig/debug/com/tuempresa/inventariovial/BuildConfig.java').read_text(encoding='utf-8')
    assert all(re.search(name+r'\s*=\s*""',config) for name in ['DRIVE_API_TOKEN','DRIVE_WEB_APP_URL','SERVER_BASE_URL'])
    apk_check=dict(sha256=sha(apk),bytes=apk.stat().st_size,verifiedAssets=207,requiredClasses=required,canonicalScapTemplatesUnchanged=True,noRuntimePdf=True,localCredentialsOmitted=True)
    (E/'apk-content.json').write_text(json.dumps(apk_check,indent=2)+'\n',encoding='utf-8')
    changes=json.loads((E/'changed-files.json').read_text(encoding='utf-8'))
    p=ROOT/'docs/SIC_NEW_FORMATS_REPORT.md';report=p.read_text(encoding='utf-8')
    status=f'''| Validación | Total | Passed | Failed | Baseline failures | New failures |
|---|---:|---:|---:|---:|---:|
| Baseline antes de producción | 323 | 308 | 15 | 15 | 0 |
| Suite final | {results['total']} | {results['passed']} | {results['failed']} | {len(results['baselineFailures'])} | 0 |
| Pruebas nuevas | 24 | 24 | 0 | 0 | 0 |
| User Service | 21 | 21 | 0 | 0 | 0 |
| Apps Script | 39 | 39 | 0 | 0 | 0 |

`gradlew testDebugUnitTest --no-configuration-cache`: ejecutado íntegramente; Gradle termina BUILD FAILED por los 15 fallos históricos autorizados, con las mismas identidades y cero fallos nuevos. No se deshabilitaron pruebas históricas. Se actualizaron únicamente cadenas de migración y expectativas de la versión final en las pruebas que antes pasaban, y se conserva el test de los siete contratos XLSX originales separado de los nuevos formatos.

`gradlew assembleDebug --no-configuration-cache --rerun-tasks`: **BUILD SUCCESSFUL**. APK debug verificado: clases de los nuevos formatos y módulos originales, 207 assets con hashes del manifest, plantillas canónicas SCAP LOSA/MODULAR, sin PDF runtime y sin credenciales privadas.

APK SHA-256: `{apk_check['sha256']}`.
'''
    report=report.replace('<!-- FINAL_VALIDATION -->',status)
    files='Archivos existentes modificados ('+str(len(changes['modified']))+'):\n\n'+''.join('- `'+name+'`\n' for name in changes['modified'])
    new=[n for n in changes['added'] if not n.startswith('app/src/main/assets/signs/')]
    files+='\nFuentes/herramientas nuevas:\n\n'+''.join('- `'+name+'`\n' for name in new)
    files+='\nAdemás: schema 15.json; 207 WebP y manifest; documentos solicitados, logs y XML de evidencia. El inventario completo con hashes queda en `sic-new-evidence/packaged-source-files.json`.\n'
    report=report.replace('<!-- CHANGED_FILES -->',files);p.write_text(report,encoding='utf-8')
    for name in ['SIC_NEW_FORMATS_REPORT.md','SIC_NEW_FORMATS_TABLET_CHECKLIST.md','SIGNAL_ASSET_AUDIT.html']:
        shutil.copy2(ROOT/'docs'/name,OUT/name)
    examples=ROOT/'app/build/outputs/sic-new/contractual-examples'
    shutil.copytree(examples,OUT/'EJEMPLOS_XLSX_SINTETICOS',dirs_exist_ok=True)
    # Keep the requested audit self-contained and ensure every image remains embedded.
    audit=(OUT/'SIGNAL_ASSET_AUDIT.html').read_text(encoding='utf-8')
    assert audit.count('data:image/webp;base64,')==207
    items=list(source_files())
    inventory={rel.as_posix():dict(sha256=sha(p),bytes=p.stat().st_size) for p,rel in items if rel.as_posix()!='docs/sic-new-evidence/packaged-source-files.json'}
    (E/'packaged-source-files.json').write_text(json.dumps(inventory,indent=2,ensure_ascii=False)+'\n',encoding='utf-8')
    items=list(source_files())
    archive=OUT/'GBO_InventarioVial_SIC_08_09_13_18A_34_SOURCE.zip'
    with zipfile.ZipFile(archive,'w',compression=zipfile.ZIP_DEFLATED,compresslevel=6) as z:
        for p,rel in items:z.write(p,'GBO_InventarioVial/'+rel.as_posix())
    with zipfile.ZipFile(archive) as z:
        assert z.testzip() is None
        for p,rel in items:assert hashlib.sha256(z.read('GBO_InventarioVial/'+rel.as_posix())).hexdigest()==sha(p)
        assert 'GBO_InventarioVial/app/schemas/com.tuempresa.inventariovial.data.database.InventoryDatabase/15.json' in z.namelist()
        assert not any(n.endswith(('local.properties','.keystore','.jks','.apk')) for n in z.namelist())
    delivery=dict(sourceFileCount=len(items),zipIntegrityVerified=True,workspaceHashesVerified=True,artifacts={p.name:dict(sha256=sha(p),bytes=p.stat().st_size) for p in OUT.iterdir() if p.is_file() and p.name!='DELIVERY_SHA256.json'})
    (OUT/'DELIVERY_SHA256.json').write_text(json.dumps(delivery,indent=2,ensure_ascii=False)+'\n',encoding='utf-8')
    print(json.dumps(delivery,indent=2,ensure_ascii=False))
if __name__=='__main__':main()
