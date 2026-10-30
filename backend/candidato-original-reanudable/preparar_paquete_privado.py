"""Prepare files for the SAME original project. Local only; never deploys or rewrites credentials."""
import argparse,hashlib,json,pathlib,re,shutil

parser=argparse.ArgumentParser()
parser.add_argument('--original-dir',required=True,type=pathlib.Path)
parser.add_argument('--output-dir',required=True,type=pathlib.Path)
args=parser.parse_args();source=args.original_dir.resolve();out=args.output_dir.resolve()
if out.exists():raise SystemExit('El destino existe; no se sobrescribira.')
code=(source/'Codigo.gs').read_text(encoding='utf-8-sig')
match=re.search(r'^const ROOT_FOLDER_ID\s*=\s*[\"\']([^\"\']+)[\"\']\s*;',code,re.M)
capabilities=json.loads((source/'CAPACIDADES_ORIGINAL_PRIVADAS.json').read_text(encoding='utf-8-sig'))
if not match or match[1]!=capabilities.get('rootFolderId'):raise SystemExit('La raiz del respaldo no coincide con la respuesta operativa: recapturar el original antes de continuar.')
start=code.index('function doGet()');end=code.index('function doPost(',start)
before=code[start:end]
if 'commitContinuationVersion:' in before:raise SystemExit('El original ya tiene continuidad: revisar diferencias, no aplicar dos veces.')
fragment="""ok: true,
    commitContinuationVersion: typeof v3Enabled==='function' && v3Enabled() && PropertiesService.getScriptProperties().getProperty('GBO_RESUMABLE_ROOT_ID')===ROOT_FOLDER_ID ? 1 : 0,
    publicNamingVersion: typeof v3Enabled==='function' && v3Enabled() && typeof PublicDeliveryNames!=='undefined' && PropertiesService.getScriptProperties().getProperty('GBO_RESUMABLE_ROOT_ID')===ROOT_FOLDER_ID && PropertiesService.getScriptProperties().getProperty('GBO_PUBLIC_NAMES_ENABLED')==='true' ? 1 : 0,"""
if before.count('ok: true,')!=1:raise SystemExit('doGet distinto del contrato revisado; no se produjo paquete.')
code=code[:start]+before.replace('ok: true,',fragment,1)+code[end:]
out.mkdir(parents=True)
unchanged=['appsscript.json','ScapPhotoPanelV2.gs','SicObservationsV1Core.gs','VideoNamesV2Core.gs','VideoV1.gs','VideoV1Core.gs']
for name in unchanged:shutil.copy2(source/name,out/name)
candidate=pathlib.Path(__file__).resolve().parent
for name in ['SyncV3.gs','SyncV3Core.gs','PhotoNamesV2Core.gs','PublicDeliveryNames.gs']:shutil.copy2(candidate/name,out/name)
(out/'Codigo.gs').write_text(code,encoding='utf-8')
manifest={'localOnly':True,'newProject':False,'deployAttempted':False,'rootPreservedAndCheckedAgainstOperationalGet':True,'originalSource':str(source),'sourceHashes':{p.name:hashlib.sha256(p.read_bytes()).hexdigest() for p in out.iterdir() if p.is_file()}}
(out/'MANIFIESTO_PRIVADO.json').write_text(json.dumps(manifest,indent=2),encoding='utf-8')
print('Paquete local preparado. Raiz original conservada y contrastada; sin despliegue. '+str(out))
