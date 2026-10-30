"""Build a credential-free, test-only GAS package. Never contacts Google."""
import hashlib, json, os, re, shutil, zipfile
from pathlib import Path

HERE = Path(__file__).resolve().parent.parent
REPO = HERE.parent.parent
OUT = HERE / 'paquete'
ORIGINAL = REPO / 'tmp/backend-v20-revision-20261016/originales-v20'
CANDIDATE = REPO / 'docs/backend-v20-revision-20261016/candidato-no-publicar'
TEST_ROOT = os.environ.get("GBO_TEST_ROOT_ID", "").strip()
MODULES = ['Codigo', 'SyncV3', 'SyncV3Core', 'PhotoNamesV2Core',
           'SicObservationsV1Core', 'VideoV1', 'VideoV1Core', 'VideoNamesV2Core']

def sha(b): return hashlib.sha256(b).hexdigest()
def write(p, s):
    p.parent.mkdir(parents=True, exist_ok=True)
    p.write_text(s, encoding='utf-8', newline='\n')

def adapted(s, name):
    # Only common test seams; original business bodies are otherwise retained.
    if name == 'Codigo':
        s, count = re.subn(r'const ROOT_FOLDER_ID\s*=\s*[^;]+;',
                           "const ROOT_FOLDER_ID = '" + TEST_ROOT + "';", s)
        assert count == 1
        s = s.replace('function doGet(', 'function gboV20DoGet_(')
        s = s.replace('function doPost(', 'function gboV20DoPost_(')
    for a, b in [('DriveApp', 'GboTestDriveApp'), ('Drive.Files', 'GboTestDrive.Files'),
                 ('UrlFetchApp', 'GboTestUrlFetch'), ('PropertiesService', 'GboTestProperties'),
                 ('LockService', 'GboTestLocks'), ('Utilities', 'GboTestUtilities')]:
        s = re.sub(r'\b' + re.escape(a) + r'\b', b, s)
    if name == 'SyncV3Core':
        assert s.count('    validate(p,store);') == 1
        s = s.replace('    validate(p,store);', "    GboTestMeasure.call('validation',()=>validate(p,store));")
        s = s.replace('    const prepared=[];', "    const _gboConsolidation=Date.now();\n    const prepared=[];")
        s = s.replace('    const transaction=store.begin(p,next,all);',
                      "    GboTestMeasure.add('consolidation',_gboConsolidation);\n    const transaction=store.begin(p,next,all);")
    if name == 'SyncV3':
        s = s.replace('function v3Store(', 'function gboV20Store_(')
    return s

def main():
    if not re.fullmatch(r"[A-Za-z0-9_-]{20,}", TEST_ROOT):
        raise SystemExit("Configure the authorized test root privately; no output written.")
    provenance=[]
    for variant, dirname in [('BASE', 'BASE_V20_PRUEBAS'), ('CANDIDATO', 'CANDIDATO_OPTIMIZADO_PRUEBAS')]:
        d=OUT / dirname
        for name in MODULES:
            src=(CANDIDATE if variant=='CANDIDATO' and name in ['SyncV3','SyncV3Core'] else ORIGINAL)/(name+'.gs')
            b=src.read_bytes(); s=b.decode('utf-8-sig')
            write(d/(name+'.gs'), adapted(s,name))
            provenance.append({'variant':variant,'module':name+'.gs','sourceSha256':sha(b),
                               'packagedSha256':sha((d/(name+'.gs')).read_bytes())})
        for n in ['TestIsolation.gs','TestHttp.gs']:
            write(d/n,(HERE/'herramientas'/n).read_text(encoding='utf-8'))
        write(d/'TestVariant.gs', "// TEST ONLY. Never copy to production.\nconst GBO_TEST_VARIANT = '"+variant+"';\n")
        write(d/'appsscript.json',json.dumps({'timeZone':'America/Lima','dependencies':{'enabledAdvancedServices':[
            {'userSymbol':'Drive','version':'v3','serviceId':'drive'}]},'exceptionLogging':'STACKDRIVER',
            'runtimeVersion':'V8'},indent=2)+'\n')
    write(OUT/'evidencia/PROCEDENCIA.json',json.dumps(provenance,indent=2)+'\n')
    # Exact originals remain locally available; Codigo contains a private production root.
    # Do not ship an executable production config, even in a provenance subdirectory.
    for name in MODULES[1:]:
        dst=OUT/'FUENTES_V20_REFERENCIA_NO_IMPORTAR'/(name+'.gs.txt')
        dst.parent.mkdir(parents=True,exist_ok=True);shutil.copyfile(ORIGINAL/(name+'.gs'),dst)
    reference=(ORIGINAL/'Codigo.gs').read_text(encoding='utf-8-sig')
    reference,n=re.subn(r'const ROOT_FOLDER_ID\s*=\s*[^;]+;',"const ROOT_FOLDER_ID = '[RAIZ_PRODUCTIVA_OMITIDA_NO_EJECUTAR]';",reference)
    assert n==1
    write(OUT/'FUENTES_V20_REFERENCIA_NO_IMPORTAR/Codigo.gs.txt',reference)
    for name in ['RESUMEN_PRUEBAS.json','mediciones-locales.json']:
        shutil.copyfile(REPO/'docs/backend-v20-revision-20261016/evidencia'/name,OUT/'evidencia'/('ANTERIOR_'+name))
    shutil.copyfile(REPO/'docs/backend-v20-revision-20261016/PROPUESTA_2_HISTORICOS_NO_IMPLEMENTADA.md',OUT/'PROPUESTA_2_HISTORICOS_NO_IMPLEMENTADA.md')
    print('Prepared 2 x 8 modules, common test isolation and provenance; no Google calls.')

if __name__=='__main__': main()
