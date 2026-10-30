"""Audit and package local files only. No network, Git writes or Android build."""
import difflib, hashlib, json, re, zipfile
from pathlib import Path

HERE=Path(__file__).resolve().parent.parent
REPO=HERE.parent.parent
PACKAGE=HERE/'paquete'
OLD=REPO/'docs/backend-v20-revision-20261016'
ORIGINAL=REPO/'tmp/backend-v20-revision-20261016/originales-v20'
def sha(p):return hashlib.sha256(p.read_bytes()).hexdigest()
def write(p,text):p.parent.mkdir(parents=True,exist_ok=True);p.write_text(text,encoding='utf-8',newline='\n')
def include(p):
    return not any(x in p.parts for x in ['__pycache__','salida-local-no-google']) and p.suffix!='.pyc' and p.name!='SHA256SUMS.txt'

def main():
    # Prior delivered Android sources/resources/build config must remain byte-identical.
    snapshot=json.loads((REPO/'docs/exportaciones-1.4.9-20261016/FUENTES_CANDIDATA_SHA256.json').read_text('utf-8-sig'))
    different=[x['path'] for x in snapshot if not (REPO/x['path']).is_file() or sha(REPO/x['path'])!=x['sha256']]
    assert not different,'Android changed: '+str(different)
    originals=json.loads((OLD/'evidencia/inventario-originales.json').read_text('utf-8-sig'))['files']
    assert len(originals)==8
    assert all(sha(ORIGINAL/x['name'])==x['sha256'] for x in originals),'Original V20 source changed'
    candidate=[]
    for line in (OLD/'SHA256SUMS.txt').read_text('utf-8-sig').splitlines():
        expected,name=line.split(maxsplit=1)
        if name.startswith('candidato-no-publicar/'):
            assert sha(OLD/name)==expected,'Approved candidate changed';candidate.append(name)
    assert len(candidate)==2
    private=[x for x in json.loads((REPO/'tmp/exportaciones-1.4.9-20261016/baseline.json').read_text('utf-8-sig')) if Path(x['path']).suffix.lower() in ['.properties','.jks','.keystore']]
    assert all((REPO/x['path']).is_file() and sha(REPO/x['path'])==x['sha256'] for x in private),'Local private/config file changed'
    apk=REPO/'output/GBO_RUTA_CLARA_CANDIDATA_1.4.9_23/GBO_Ruta_Clara_1.4.9_23_CANDIDATA.apk'
    if not apk.is_file():
        matches=list(apk.parent.glob('*.apk'))
        assert len(matches)==1,'Existing APK location is ambiguous; do not touch APK files'
        apk=matches[0]
    assert sha(apk)=='508e68d1b3134d1b850a9dee60f4b35c9e3053e5d3073e4ea79edcce4f9756c6','Existing APK changed'
    for name in ['SyncV3.gs','SyncV3Core.gs']:
        a=(ORIGINAL/name).read_text('utf-8-sig').splitlines(True)
        b=(OLD/'candidato-no-publicar'/name).read_text('utf-8-sig').splitlines(True)
        write(PACKAGE/'diferencias'/(name+'.diff'),''.join(difflib.unified_diff(a,b,fromfile='V20/'+name,tofile='CANDIDATO_REVISADO/'+name)))
    check={'androidSourceEntriesChecked':len(snapshot),'androidSourcesUnchanged':True,'historicalV20ModulesChecked':8,'historicalV20Unchanged':True,
           'approvedCandidateModulesChecked':2,'approvedCandidateUnchanged':True,'localConfigurationEntriesChecked':len(private),'localConfigurationsUnchanged':True,
           'android149ApkUnchanged':True,'verifiedExistingApkPath':apk.relative_to(REPO).as_posix(),'googleExecutions':0,'googleWrites':0,'productionChanges':0,'commits':0,'pushes':0}
    write(PACKAGE/'evidencia/CONSERVACION.json',json.dumps(check,indent=2)+'\n')
    # Manifest provenance is explicitly not certified without the original JSON.
    for d in ['BASE_V20_PRUEBAS','CANDIDATO_OPTIMIZADO_PRUEBAS']:
        manifest=json.loads((PACKAGE/d/'appsscript.json').read_text('utf-8'))
        assert manifest['runtimeVersion']=='V8'
        assert manifest['dependencies']['enabledAdvancedServices']==[{'userSymbol':'Drive','version':'v3','serviceId':'drive'}]
    # Test/reference modules must never carry an executable production root or URL.
    prod=re.search(r'const ROOT_FOLDER_ID\s*=\s*[\'\"]([^\'\"]+)',(ORIGINAL/'Codigo.gs').read_text('utf-8-sig')).group(1)
    files=sorted(p for p in PACKAGE.rglob('*') if p.is_file() and include(p))
    for p in files:
        assert p.suffix.lower() not in ['.properties','.keystore','.jks','.apk','.aab','.p12','.pem','.key'],p.name
        b=p.read_bytes();assert prod.encode() not in b,'Production root found in '+str(p.relative_to(PACKAGE))
        assert b'-----BEGIN PRIVATE KEY-----' not in b and b'-----BEGIN RSA PRIVATE KEY-----' not in b
        if p.suffix.lower() in ['.gs','.txt','.md','.json','.cjs','.py','.ps1','.html','.diff','.tap']:
            text=b.decode('utf-8-sig')
            for u in re.findall(r'https://script\.google\.com/macros/s/[A-Za-z0-9_-]+/exec',text):
                assert u in ['https://script.google.com/macros/s/LOCAL_MOCK_ONLY/exec','https://script.google.com/macros/s/x/exec'],'Private deployment URL found in '+str(p.relative_to(PACKAGE))
    write(PACKAGE/'SHA256SUMS.txt',''.join(sha(p)+'  '+p.relative_to(PACKAGE).as_posix()+'\n' for p in files))
    files.append(PACKAGE/'SHA256SUMS.txt')
    dest=REPO/'output/GBO_BACKEND_PRUEBAS_V20_20261017.zip'
    assert not dest.exists(),'Existing ZIP preserved; choose a new filename before repackaging'
    dest.parent.mkdir(parents=True,exist_ok=True)
    with zipfile.ZipFile(dest,'w',zipfile.ZIP_DEFLATED,compresslevel=9) as z:
        for p in files:z.write(p,p.relative_to(PACKAGE).as_posix())
    with zipfile.ZipFile(dest) as z:
        assert z.testzip() is None
        sums=z.read('SHA256SUMS.txt').decode().splitlines()
        for line in sums:
            h,n=line.split(maxsplit=1);assert hashlib.sha256(z.read(n)).hexdigest()==h,n
        assert len(z.namelist())==len(files)
    write(dest.with_suffix('.zip.sha256'),sha(dest)+'  '+dest.name+'\n')
    print(json.dumps({'zip':str(dest),'files':len(files),'bytes':dest.stat().st_size,'sha256':sha(dest),'conservation':check},indent=2))

if __name__=='__main__':main()
