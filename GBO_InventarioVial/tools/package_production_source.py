"""Deliver prepared production sources with explicit signing block; never copy a stale field APK."""
from pathlib import Path
import hashlib,json,os,re,runpy,shutil,zipfile
R=Path(__file__).resolve().parents[1];E=R/'final-evidence';O=R/'output/final-delivery'
def sha(p):return hashlib.sha256(p.read_bytes()).hexdigest()
def main():
    runpy.run_path(str(R/'tools/verify_production_preservation.py'))['tests']()
    signing=json.loads((E/'release-identity-summary.json').read_text(encoding='utf-8'))
    tests=json.loads((E/'production-test-results.json').read_text(encoding='utf-8'))
    debug=json.loads((E/'validation-debug-identity.json').read_text(encoding='utf-8-sig'))[0]
    assert debug['package']=='com.tuempresa.inventariovial' and debug['versionCode']==11 and debug['debuggable'] and debug['verified']
    assert 'RELEASE_BLOCKED_MISSING_SIGNING_KEY' in (E/'assemble-release.log').read_text(encoding='utf-8')
    assert 'BUILD SUCCESSFUL' in (E/'production-debug-build.log').read_text(encoding='utf-8')
    assert len(json.loads((E/'identity-gate-tests.json').read_text(encoding='utf-8')))==4
    assert not (O/'GBO_InventarioVial_PRODUCTION_RELEASE.apk').exists(), 'Never deliver an unverified/stale production APK'
    with zipfile.ZipFile(O/'GBO_InventarioVial_VALIDATION_DEBUG_v11.apk') as z:
        assert z.testzip() is None
        manifest=json.loads((R/'app/src/main/assets/signs/sign_assets_manifest.json').read_text(encoding='utf-8'))
        assert all(hashlib.sha256(z.read('assets/signs/'+x['asset'])).hexdigest()==x['sha256'] for x in manifest)
        assert len(manifest)==207
    config=(R/'app/build/generated/source/buildConfig/debug/com/tuempresa/inventariovial/BuildConfig.java').read_text(encoding='utf-8')
    assert all(re.search(name+r'\s*=\s*""',config) for name in ['DRIVE_API_TOKEN','DRIVE_WEB_APP_URL','SERVER_BASE_URL'])
    text=f'''# APK_RELEASE_REPORT

## Estado

**VERSIONADO CORREGIDO. FIRMA DE LA NUEVA RELEASE BLOQUEADA. ACTUALIZACIÓN REAL NOT TESTED.** No se declara resuelto el problema de instalación y no se entrega una supuesta APK de producción.

Base exclusiva: `GBO_InventarioVial_SIC_08_09_13_18A_34_SOURCE.zip`, correspondiente a SIC_NEW_FORMATS_REPORT.md. Room 15, MIGRATION_14_15 y todas las migraciones/SIC/módulos se conservan. Se verificaron hashes idénticos de los 576 archivos protegidos app/src, schemas, backend y Apps Script.

## Identidad preparada

| Campo | Valor |
|---|---|
| previousMaximumVersionCode | 10 |
| newVersionCode | 11 |
| versionName | 1.4.0-sic-production |
| applicationId | com.tuempresa.inventariovial |
| Room | 15 |
| minSdk declarado | 26 |
| targetSdk declarado | 37 |
| certificado público esperado SHA-256 | {signing['expectedReleaseCertSha256']} |
| nueva APK de producción | NO GENERADA |
| SHA-256/tamaño/certificado de nueva release | NO DISPONIBLES |

VERSIONCODE10 se comparó byte por byte con su ZIP integrado predecesor: únicamente app/build.gradle.kts difiere, con versionCode 8→10. No se copiaron sus fuentes sobre SIC. El máximo entre APK históricos encontrados es 8; el reporte de corrección menciona 9 y la fuente VERSIONCODE10 verifica 10. La nueva iteración usa 11, superior al máximo conocido previo.

## Auditoría de firma y bloqueo

68 APK examinados con herramientas Android SDK oficiales; 46 tienen el applicationId principal. FIELD_SIGNATURE_A es GBO corporativa, compartida por los dos APK adjuntos y doce APK principales. FIELD_SIGNATURE_B es Android Debug, presente en 34 APK principales, incluido el debug SIC anterior. Los grupos de dispositivos concretos permanecen UNKNOWN.

SIGNATURE_MISMATCH_CONFIRMED entre el APK SIC debug previo y los APK históricos GBO adjuntos. No se atribuye sin evidencia un certificado a un fabricante/tablet particular. No se combinan firmas, no se genera clave nueva ni se deshabilita seguridad del dispositivo. Se fija la identidad pública GBO, derivada del certificado de APK conocidos.

No se encontró la keystore privada GBO ni variables GBO_RELEASE_* configuradas; tampoco server.local.properties. Se solicitó al usuario la ubicación/configuración histórica, sin solicitar passwords por chat. drive.local.properties existente se mantiene privado. La nueva release debe utilizar configuración autorizada local, sin gboValidationBuild; aquí su configuración funcional completa no puede verificarse.

`assembleRelease --no-configuration-cache --rerun-tasks` se ejecutó sin gboValidationBuild: BUILD FAILED en validateReleaseConfiguration por RELEASE_BLOCKED_MISSING_SIGNING_KEY. Es el bloqueo previsto y no un APK unsigned o firmado con debug. RELEASE_BLOCKED_MISSING_SIGNING_KEY.md detalla lo faltante.

## Cambios de esta iteración

- app/build.gradle.kts: versionCode 11, versionName de trazabilidad, signingConfigs solo cuando existe configuración local explícita, validación de clave privada/certificado y configuración funcional, y verifyReleaseIdentity sobre APK completo. Se migraron dos llamadas deprecated assets.srcDir a assets.directories.add manteniendo exactamente las rutas existentes.
- .gitignore: exclusión de configuración local y archivos de firma.
- keystore.properties.example: solo nombres y valores vacíos.
- tools/apk_identity_audit.ps1: diagnóstico/gate con aapt, apksigner, zipalign, package/version/certificado, rechazo de APK split/test/debug y SHA-256. Compatible con Windows PowerShell, sin depender de Get-FileHash.
- tools/verify_production_preservation.py y tools/package_production_source.py: evidencias, preservación y empaquetado seguro.
- Documentación/evidencia de versión, firma, política, actualización pendiente y bloqueo.

No se editaron Kotlin de aplicación, Room/schema/migraciones, UI/SIC, cálculos, SCAP/Puentes/Pavimentos, KMZ, protocolo Drive V2, User Service, autenticación, fingerprint, token o fotos limpias.

## Pruebas

Suite completa: **{tests['total']} total, {tests['passed']} passed, {tests['failed']} failed, 15 baselineFailures, 0 newFailures, 0 skipped**. Coinciden las identidades históricas; las pruebas de migración nuevas y los cinco SIC siguen pasando. La salida Gradle BUILD FAILED de tests corresponde solo a esos 15 fallos autorizados.

Cuatro controles del script de identidad PASS: referencia histórica correcta aceptada; certificado distinto, versionCode distinto y applicationId distinto rechazados. La referencia positiva no es una nueva release y no prueba actualización real.

`assembleDebug --no-configuration-cache -PgboValidationBuild=true`: BUILD SUCCESSFUL. Debug de validación 11 verificado con 207 assets idénticos, package correcto, minSdk 26, APK íntegro y alineado. Este APK es **SOLO VALIDACIÓN**, tiene certificado Android Debug y endpoints/credenciales omitidos. No se distribuye como actualización de campo.

Debug de validación SHA-256: `{debug['apkSha256']}`; tamaño: {debug['bytes']} bytes. No confundir este hash con un APK production release que no existe.

## Actualización y fabricantes

9→nueva: NOT TESTED. 10→nueva: NOT TESTED. Histórico 8→nueva: NOT TESTED. No hay dispositivo conectado ni AVD configurado y falta firma de nueva producción. No se ejecutó adb install -r y no se afirmó Success ni conservación física de datos.

Samsung: se verificaron integridad, apksigner, package, SDK y zipalign de las referencias y del debug nuevo; no hay APK nueva de producción ni prueba en Samsung. No se aplica hack por fabricante.

Huawei/Lenovo: la firma distinta entre APK conocidos está confirmada; la correspondencia con sus instalaciones exactas sigue pendiente. Xiaomi/OPPO: el comportamiento comunicado por el usuario no sustituye una comparación de sus APK instalados. Consultar APK_SIGNATURE_AUDIT.md y APK_IN_PLACE_UPGRADE_TEST.md.

Pendiente: aportar/configurar la clave histórica y server.local.properties autorizados; compilar release y pasar verifyReleaseIdentity; probar al menos una actualización real sin desinstalar, conservando Room, registros, fotos, borradores, archivos, usuarios, configuración y relaciones. No se recomienda desinstalar. No hubo commit ni push.
'''
    (R/'docs/APK_RELEASE_REPORT.md').write_text(text,encoding='utf-8')
    docs=['APK_RELEASE_REPORT.md','APK_SIGNATURE_AUDIT.md','APK_IN_PLACE_UPGRADE_TEST.md','ANDROID_RELEASE_POLICY.md','RELEASE_SIGNING.md','RELEASE_BLOCKED_MISSING_SIGNING_KEY.md','VERSIONCODE10_DIFF_AUDIT.md']
    for name in docs:shutil.copy2(R/'docs'/name,O/name)
    shutil.copy2(R/'tools/apk_identity_audit.ps1',O/'apk_identity_audit.ps1')
    excluded={'.git','.gradle','.kotlin','.idea','build','tmp','output','node_modules','__pycache__','.codex','.agents','.aws'}
    private={'local.properties','keystore.properties','drive.local.properties','server.local.properties','credentials.json','service-account.json'}
    files=[]
    for directory,dirs,names in os.walk(R,topdown=True,followlinks=False):
        dirs[:]=sorted(d for d in dirs if d not in excluded)
        for name in sorted(names):
            p=Path(directory)/name;rel=p.relative_to(R)
            if name == 'production-package.log' or name in private or p.suffix.lower() in {'.jks','.keystore','.p12','.pfx','.pem','.key','.apk','.zip','.pyc'}:continue
            if p.suffix.lower()=='.log' and not (p.parent==E or 'sic-new-evidence' in rel.parts):continue
            if name.startswith(('hs_err_pid','replay_pid')) or (name.startswith('.env') and name!='.env.example'):continue
            assert p.resolve().is_relative_to(R)
            files.append((p,rel))
    source=O/'GBO_InventarioVial_PRODUCTION_SOURCE.zip'
    with zipfile.ZipFile(source,'w',compression=zipfile.ZIP_DEFLATED,compresslevel=6) as z:
        for p,rel in files:z.write(p,'GBO_InventarioVial/'+rel.as_posix())
    with zipfile.ZipFile(source) as z:
        assert z.testzip() is None
        assert not any(Path(n).name in private or Path(n).suffix.lower() in {'.jks','.keystore','.p12','.pfx','.pem','.key','.apk'} for n in z.namelist())
        for p,rel in files:assert hashlib.sha256(z.read('GBO_InventarioVial/'+rel.as_posix())).hexdigest()==sha(p)
    hashes=dict(status='RELEASE_BLOCKED_MISSING_SIGNING_KEY',productionApkGenerated=False,previousMaximumVersionCode=10,newVersionCode=11,
        expectedReleaseCertificateSha256=signing['expectedReleaseCertSha256'],protectedFilesUnchanged=576,sourceFiles=len(files),sourceZipIntegrityVerified=True,
        artifacts={p.name:dict(sha256=sha(p),bytes=p.stat().st_size) for p in O.iterdir() if p.is_file() and p.name!='hashes.json'})
    (O/'hashes.json').write_text(json.dumps(hashes,indent=2,ensure_ascii=False),encoding='utf-8')
    print(json.dumps(hashes,indent=2,ensure_ascii=False))
if __name__=='__main__':main()
