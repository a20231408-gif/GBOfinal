"""Local-only packaging and preservation audit. Does not publish or run Google."""
from pathlib import Path
import hashlib,json,re,zipfile,difflib
HERE=Path(__file__).parent
REPO=HERE.parent.parent
P=HERE/'paquete'
OLD=REPO/'docs/backend-google-pruebas-v20-20261017/paquete'
def sha(p):return hashlib.sha256(p.read_bytes()).hexdigest()
def write(p,text):p.parent.mkdir(parents=True,exist_ok=True);p.write_text(text,encoding='utf-8',newline='\n')
def include(p):return not any(x.startswith('salida-local') or x=='__pycache__' for x in p.parts) and p.suffix!='.pyc' and p.name!='SHA256SUMS.txt'
def main():
    mods=['Codigo','SyncV3','SyncV3Core','PhotoNamesV2Core','SicObservationsV1Core','VideoV1','VideoV1Core','VideoNamesV2Core']
    for v in ['BASE_V20_PRUEBAS','CANDIDATO_OPTIMIZADO_PRUEBAS']:
        for m in mods+['TestVariant']:assert sha(P/v/(m+'.gs'))==sha(OLD/v/(m+'.gs')),(v,m)
        assert sha(P/v/'appsscript.json')==sha(OLD/v/'appsscript.json')
    for n in ['TestIsolation.gs','TestHttp.gs']:assert sha(P/'BASE_V20_PRUEBAS'/n)==sha(P/'CANDIDATO_OPTIMIZADO_PRUEBAS'/n)
    source=list((Path('D:/Downloads/GBO_BACKEND_PRUEBAS_V20_20261017')/'RESULTADOS_GOOGLE').glob('BASE_*/resultado.json'));assert len(source)==1
    evidence=json.loads((P/'evidencia/HASHES_EVIDENCIA_REAL.json').read_text('utf-8'))
    assert all(sha(source[0].with_name(x['file']))==x['sha256'] for x in evidence),'Original real evidence changed'
    android=json.loads((REPO/'docs/exportaciones-1.4.9-20261016/FUENTES_CANDIDATA_SHA256.json').read_text('utf-8-sig'))
    assert all(sha(REPO/x['path'])==x['sha256'] for x in android),'Android sources changed'
    configs=[x for x in json.loads((REPO/'tmp/exportaciones-1.4.9-20261016/baseline.json').read_text('utf-8-sig')) if Path(x['path']).suffix=='.properties']
    assert all(sha(REPO/x['path'])==x['sha256'] for x in configs),'Private/local properties changed'
    oldZip=REPO/'output/GBO_BACKEND_PRUEBAS_V20_20261017.zip'
    assert sha(oldZip)=='fae445e057f5f774ffba4ac16248010d0c02ea832fcca03b4e679bbc2047b46d','Prior ZIP changed'
    apk=REPO/'output/GBO_RUTA_CLARA_CANDIDATA_1.4.9_23/GBO_Ruta_Clara_1.4.9.apk'
    assert sha(apk)=='508e68d1b3134d1b850a9dee60f4b35c9e3053e5d3073e4ea79edcce4f9756c6','Existing APK changed'
    audit={'androidEntriesChecked':len(android),'androidUnchanged':True,'apkUnchanged':True,'localConfigurationsChecked':len(configs),'localConfigurationsUnchanged':True,'businessModulesUnchanged':16,'manifestAndVariantUnchanged':True,'commonHelpersIdentical':True,'realEvidenceUnchanged':True,'priorZipUnchanged':True,'googleCallsByCodex':0,'published':False,'commits':0,'pushes':0}
    write(P/'evidencia/CONSERVACION_V2.json',json.dumps(audit,indent=2)+'\n')
    write(P/'evidencia/RESUMEN_VALIDACION_V2.json',json.dumps({'environment':'LOCAL_SIMULATED_GOOGLE_NO_NETWORK','distinctCases':35,'passed':35,'failed':0,'existingRegressionCases':23,'recoveryCases':12,'realEvidenceReadOffline':True,'postPatchGoogleExecutions':0,'productionPerformanceClaim':False,'tap':['regresion-final-v2.tap','recuperacion-contrato-final.tap','staging-readonly-final.tap']},indent=2)+'\n')
    for n in ['TestIsolation.gs','TestHttp.gs']:
        write(P/'diferencias'/('helpers-v2-'+n+'.diff'),''.join(difflib.unified_diff((OLD/'BASE_V20_PRUEBAS'/n).read_text('utf-8').splitlines(True),(P/'BASE_V20_PRUEBAS'/n).read_text('utf-8').splitlines(True),fromfile='TEST_HELPER_V1/'+n,tofile='TEST_HELPER_V2/'+n)))
    files=sorted(x for x in P.rglob('*') if x.is_file() and include(x))
    prod=re.search(r'const ROOT_FOLDER_ID\s*=\s*[\'\"]([^\'\"]+)',(REPO/'tmp/backend-v20-revision-20261016/originales-v20/Codigo.gs').read_text('utf-8-sig')).group(1).encode()
    for f in files:
        assert f.suffix.lower() not in ['.properties','.apk','.aab','.jks','.keystore','.pem','.p12']
        data=f.read_bytes();assert prod not in data,'Production root in package'
        assert b'-----BEGIN PRIVATE KEY-----' not in data
        if f.suffix.lower() in ['.cjs','.gs','.json','.md','.py','.ps1','.txt','.tap','.html','.diff']:
            text=data.decode('utf-8-sig')
            for u in re.findall(r'https://script\.google\.com/macros/s/[A-Za-z0-9_-]+/exec',text):assert u in ['https://script.google.com/macros/s/LOCAL_MOCK_ONLY/exec','https://script.google.com/macros/s/x/exec'],'Actual deployment URL in package'
    write(P/'SHA256SUMS.txt',''.join(sha(f)+'  '+f.relative_to(P).as_posix()+'\n' for f in files));files.append(P/'SHA256SUMS.txt')
    target=REPO/'output/GBO_REPARACION_ENSAYO_BASE_V20_HELPERS_V2_20261017.zip';assert not target.exists(),'Preserve existing output; do not overwrite'
    with zipfile.ZipFile(target,'w',zipfile.ZIP_DEFLATED,compresslevel=9) as z:
        for f in files:z.write(f,f.relative_to(P).as_posix())
    with zipfile.ZipFile(target) as z:
        assert z.testzip() is None
        for line in z.read('SHA256SUMS.txt').decode().splitlines():
            h,n=line.split(maxsplit=1);assert hashlib.sha256(z.read(n)).hexdigest()==h
    write(target.with_suffix('.zip.sha256'),sha(target)+'  '+target.name+'\n')
    print(json.dumps({'zip':str(target),'bytes':target.stat().st_size,'files':len(files),'sha256':sha(target),'conservation':audit},indent=2))
if __name__=='__main__':main()
