# Package the newest UI source and the verified validation debug.
from pathlib import Path
import hashlib,json,zipfile,os,shutil,re,runpy
R=Path(__file__).resolve().parents[1];E=R/'docs/ui-unification-evidence';O=R/'output/ui-unification'
def sha(p):return hashlib.sha256(p.read_bytes()).hexdigest()
def main():
    checks=runpy.run_path(str(R/'tools/verify_ui_unification.py'))
    preserved=checks['preservation']();tests=checks['tests']()
    assert 'BUILD SUCCESSFUL' in (E/'assemble-debug.log').read_text(encoding='utf-8')
    identity=json.loads((E/'debug-identity.json').read_text(encoding='utf-8-sig'))[0]
    assert identity['package']=='com.tuempresa.inventariovial' and identity['versionCode']==12
    assert identity['verified'] and identity['debuggable'] and not identity['isSplit'] and not identity['isTest']
    assert identity['zipAligned'] and identity['pageAligned16KiB']
    previous=json.loads((R/'final-evidence/validation-debug-identity.json').read_text(encoding='utf-8-sig'))[0]
    assert identity['certificateSha256']==previous['certificateSha256'], 'Debug certificate changed'
    apk=O/'GBO_InventarioVial_UI_UNIFICADA_DEBUG_v12.apk'
    assert sha(apk)==identity['apkSha256']
    with zipfile.ZipFile(apk) as z:
        assert z.testzip() is None
        assets=json.loads((R/'app/src/main/assets/signs/sign_assets_manifest.json').read_text(encoding='utf-8'))
        assert len(assets)==207
        assert all(hashlib.sha256(z.read('assets/signs/'+item['asset'])).hexdigest()==item['sha256'] for item in assets)
    config=(R/'app/build/generated/source/buildConfig/debug/com/tuempresa/inventariovial/BuildConfig.java').read_text(encoding='utf-8')
    assert all(re.search(name+r'\s*=\s*""',config) for name in ['DRIVE_API_TOKEN','DRIVE_WEB_APP_URL','SERVER_BASE_URL'])
    changed='\n'.join('- '+name for name in preserved['changedFiles']+preserved['newSourceFiles'])
    report=f"""# SIC UI UNIFICATION REPORT

## Resultado

Base: workspace más reciente, posterior a los nuevos SIC y a la corrección de producción. SOURCE + DEBUG validado. No se generó release ni se cambió la identidad de firma.

El dashboard ahora se organiza por familias y objetos: Fajas; Señalización y seguridad, Puntos críticos y Accidentes; Alcantarillas, Cunetas/drenaje, Badenes, Muros y Túneles; Derecho de vía; Puentes/SCAP. Pavimentos conserva su acceso. Los códigos SIC son secundarios.

## Antes / después y componentes

Antes había entradas independientes SIC-08, SIC-09, SIC-13, SIC-34 y SIC-18A. Ahora Fajas agrupa 08/09 y hay una sola entrada Alcantarillas con 18A como complemento. Todas las tarjetas usan InventoryElementButton, extraído del AssetButton maduro.

Se reutilizan SectionTitle, ChoiceSelector (chips/dropdown), ConditionSelector, LocationSummaryCard, PhotoCaptureCard, PhotoPreview, CulvertPhotos, RecordReviewSection y ValidationSummaryCard. InventoryFormSection e InventoryFieldRows son contenedores comunes; no se creó un segundo catálogo de controles por SIC.

Estructura: Identificación → Ubicación inicio/fin o puntual → Datos del elemento → Condición/Nivel → Fotografías → Observaciones → Revisión/acciones. Columnas adaptativas a 640 dp y ancho máximo de formulario 1000 dp. GNSS y ayudas de puntos críticos, accidentes y condiciones de Alcantarilla empiezan colapsados. Los rangos contractuales inicio/fin de SIC-13 se conservan.

La terminología es Guardar borrador / Revisar y finalizar / Finalizar / Exportar / Volver. Los borradores guardan texto crudo, incluso números incompletos. Finalizar valida obligatorios y exportar usa datos contractuales suficientes.

## Alcantarilla SIC-18 / SIC-18A

Sic18Fields sigue siendo la ficha base madura: ubicación, clase, tipo, ojos/vanos, sección, dimensiones y condiciones. Si estructural o funcional = 3, Completar detalle de condición mala despliega función, ubicación/tipo de falla estructural y estado/causa funcional. No se repiten inputs de ruta/calzada/PR/fecha/clase/tipo/ojos: se heredan.

Un UUID, una fila de historial, un conjunto de fotos categorizadas y una carpeta de entrega. El complemento puede capturarse antes de finalizar la base; su borrador conserva la FK al mismo UUID. Se corrigió la continuación de borradores SIC-18. Salir con cambios del detalle ofrece Guardar borrador y volver / Seguir editando.

Desde historial se abre un flujo único con base, detalle, fotografías, revisión y Exportar formatos. Los contratos siguen separados: SIC-18.xlsx y SIC-18A.xlsx cuando el detalle es requerido y completo. Sin mala condición: SIC-18A No requerido. Con un detalle existente y cambio 3 → 1/2: Conservar como antecedente / Cancelar cambio; no se elimina información y la exportación corriente no trata el antecedente como requisito.

Al editar una Alcantarilla activa, CulvertEditDraft guarda el estado crudo por operador en almacenamiento local, sin modificar los datos contractuales hasta finalizar. Conserva campos, detalle, rutas/categorías de fotos y metadatos de captura; los cambios aún en borrador no se exportan. Finalizar usa una transacción Room con EngineeringEdits y SicFieldRepository.

## Faja SIC-08 / SIC-09

Fajas abre una lista con tipo/sentido, ruta/calzada y PR inicio/fin. + Nueva faja abre una sola ficha: Identificación/Ubicación compartidas, Características SIC-08 y Anchos SIC-09.

Se pueden guardar/finalizar características sin anchos: Anchos pendiente → Completar anchos dentro de la misma ficha. Con datos completos, un solo botón exporta ambos Excel contractuales. saveFaja guarda base y anchos transaccionalmente.

SIC-09 conserva UUID y sourceRecordId a SIC-08. Los datos comunes se heredan del padre al recuperar el complemento. Las mediciones históricas múltiples se conservan mediante selector. No se infieren asociaciones por proximidad o nombre.

## Historial, archivos y Drive

El historial agrupa los SIC-09 vinculados bajo la Faja SIC-08, con estados de ambos formatos. SIC-18A conserva la identidad de SIC-18. Los borradores hermanos no duplican al padre cuando ambos son borradores.

Archivos generados reconoce ambos órdenes históricos de nombres de archivo y agrupa por UUID principal con contexto ruta/PR, conservando Abrir/Compartir y las comprobaciones FileProvider.

RouteDelivery prepara una carpeta por Faja vinculada con ambos contratos. El payload SIC-09 mantiene su recordId para el ACK original; el complemento de Alcantarilla usa la carpeta SIC-18. Drive V2, validación hash/ACK, token, servidor y Apps Script permanecen intactos. Solo se verificaron rutas y manifest locales: no hubo subidas reales ni reorganización silenciosa de carpetas antiguas.

## Room, seguridad y versión

Room 15; sin nueva migración ni destructive migration. Schemas, InventoryDatabase y todas las migraciones permanecen iguales. De {preserved['receivedFiles']} archivos recibidos registrados, {preserved['unchangedFiles']} mantienen hashes idénticos; las excepciones autorizadas se listan debajo.

Sin cambios en autenticación, device/huella/token, User Service, PCI/URCI, SCAP/Puentes, catálogo de 207 señales, selección de cámaras, KMZ y fotos limpias.

applicationId: com.tuempresa.inventariovial.
VersionCode: 11 → 12 para la nueva entrega debug según la política.
versionName: 1.4.0-sic-production conservado.
El único cambio semántico de app/build.gradle.kts es versionCode; signingConfig, certificado esperado, fail closed y verifyReleaseIdentity se conservan.

Certificado público esperado de release: {preserved['expectedCertificateSha256']}.
La keystore histórica sigue ausente. Esta iteración no ejecutó assembleRelease ni declaró resuelto ese bloqueo.

## Tests y build

Baseline registrado por identidad desde los XML de la versión recibida, coincidentes con la evidencia de producción: {tests['baselineTotal']} casos, 332 PASSED, 15 FAILED.
MissingBaselineCases: {len(tests['missingBaselineCases'])}.

Suite final: {tests['total']} total; {tests['passed']} PASSED; {tests['failed']} FAILED; {tests['skipped']} SKIPPED.
Fallos baseline presentes: {len(tests['baselineFailures'])}.
newFailures: {len(tests['newFailures'])}.
Nuevos casos: {len(tests['addedCases'])}, todos PASSED.
Los 15 fallos son las mismas identidades históricas; testDebugUnitTest devuelve BUILD FAILED por ellos, sin nuevas regresiones.

Las pruebas añadidas cubren dashboard, detalle por mala condición, ayudas colapsables, Faja guardar/reabrir/completar anchos, datos crudos, exportación uno/dos contratos, antecedentes sin borrado, FK del detalle antes de finalizar base, anchos históricos múltiples/sin vínculo, carpeta compartida y ACK original, agrupación de archivos y conservación de GPS/fotos limpias. Engineering2ScreenTest solo actualiza la etiqueta esperada del botón; conserva su escenario e identidad.

gradlew assembleDebug --no-configuration-cache --rerun-tasks -PgboValidationBuild=true: BUILD SUCCESSFUL.
La validación omite endpoints/credenciales y no modifica los archivos locales originales.
El APK es debug de pruebas; no es una actualización de campo.

APK: {apk.name}
- package: {identity['package']}
- versionCode: {identity['versionCode']}
- versionName: {identity['versionName']}
- minSdk: {identity['minSdk']}
- tamaño: {identity['bytes']} bytes
- SHA-256 archivo: {identity['apkSha256']}
- certificado debug SHA-256: {identity['certificateSha256'][0]}
- apksigner: Verified; APK completo, sin split/test; zipalign y alineación 16 KiB verificados.
- Integridad ZIP y 207 assets iguales al manifest: PASS.

## Archivos de aplicación modificados o añadidos

{changed}

Documentación y herramientas: SIC_UI_UNIFICATION_AUDIT.md, SIC_UI_UNIFICATION_BEFORE_AFTER.md, este reporte, ui-unification-evidence, verify_ui_unification.py y package_ui_unification.py. ANDROID_RELEASE_POLICY.md solo actualiza la versión preparada; conserva sus reglas. Informes y empaquetadores de iteraciones anteriores son evidencia histórica; esta entrega usa el verificador y empaquetador UI.

## Limitaciones

- Validación visual: Compose automatizado y documento conceptual; no screenshots fabricados. No hay tablet/AVD de prueba. Cámara/GNSS reales, revisión física horizontal/vertical e instalación: NOT TESTED.
- SIC-09 históricos sin vínculo explícito siguen accesibles/exportables como anchos históricos sin vínculo; no se adivina el padre. Cualquier asociación futura debe ser explícita y controlada.
- Las carpetas Drive históricas ya creadas y entregas iniciadas permanecen intactas. La nueva organización aplica a nuevas entregas preparadas con esta versión.
- No se genera release ni se prueba actualización de campo. El bloqueo de firma histórico permanece.
- Sin commit ni push.
"""
    (R/'docs/SIC_UI_UNIFICATION_REPORT.md').write_text(report,encoding='utf-8')
    O.mkdir(parents=True,exist_ok=True)
    for name in ['SIC_UI_UNIFICATION_REPORT.md','SIC_UI_UNIFICATION_AUDIT.md','SIC_UI_UNIFICATION_BEFORE_AFTER.md']:
        shutil.copy2(R/'docs'/name,O/name)
    excluded={'.git','.gradle','.kotlin','.idea','build','tmp','output','node_modules','__pycache__','.codex','.agents','.aws'}
    private={'local.properties','keystore.properties','drive.local.properties','server.local.properties','credentials.json','service-account.json'}
    selected=[]
    for directory,dirs,names in os.walk(R,topdown=True,followlinks=False):
        dirs[:]=sorted(d for d in dirs if d not in excluded)
        for name in sorted(names):
            p=Path(directory)/name;rel=p.relative_to(R)
            if name in private or p.suffix.lower() in {'.jks','.keystore','.p12','.pfx','.pem','.key','.apk','.zip','.pyc'}:continue
            if p.suffix.lower()=='.log' and not any(part in {'ui-unification-evidence','sic-new-evidence','final-evidence'} for part in rel.parts):continue
            if name=='production-package.log' or name.startswith(('hs_err_pid','replay_pid')) or name.startswith('.env') and name!='.env.example':continue
            assert p.resolve().is_relative_to(R)
            selected.append((p,rel))
    secrets=[]
    for name in ['drive.local.properties','server.local.properties','keystore.properties']:
        p=R/name
        if p.is_file():
            for line in p.read_text(encoding='utf-8').splitlines():
                if '=' in line and not line.lstrip().startswith('#'):
                    key,value=line.split('=',1)
                    if re.search('TOKEN|PASSWORD|SECRET|PIN',key,re.I) and len(value.strip())>=8:secrets.append(value.strip().encode())
    source=O/'GBO_InventarioVial_UI_UNIFICADA_SOURCE.zip'
    with zipfile.ZipFile(source,'w',compression=zipfile.ZIP_DEFLATED,compresslevel=6) as z:
        for p,rel in selected:
            data=p.read_bytes();assert not any(secret in data for secret in secrets), 'Private local value in source file'
            z.writestr('GBO_InventarioVial/'+rel.as_posix(),data)
    with zipfile.ZipFile(source) as z:
        assert z.testzip() is None
        assert not any(Path(name).name in private or Path(name).suffix.lower() in {'.jks','.keystore','.p12','.pfx','.pem','.key','.apk'} for name in z.namelist())
        for p,rel in selected:assert hashlib.sha256(z.read('GBO_InventarioVial/'+rel.as_posix())).hexdigest()==sha(p)
    result=dict(status='UI_UNIFICATION_SOURCE_AND_VALIDATION_DEBUG',roomVersion=15,migrationAdded=False,versionCode=12,signingPolicyPreserved=True,
        sourceFiles=len(selected),sourceZipIntegrityVerified=True,localSecretsExcluded=True,
        tests={k:tests[k] for k in ['total','passed','failed','skipped','baselineTotal','newFailures','missingBaselineCases','addedCases']},
        artifacts={p.name:dict(sha256=sha(p),bytes=p.stat().st_size) for p in O.iterdir() if p.is_file() and p.name!='hashes.json'})
    (O/'hashes.json').write_text(json.dumps(result,indent=2),encoding='utf-8')
    print(json.dumps(result,indent=2))
if __name__=='__main__':main()
