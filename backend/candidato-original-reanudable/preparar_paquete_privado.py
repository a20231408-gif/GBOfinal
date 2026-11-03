"""Prepare files for the SAME original project. Local only; never deploys or rewrites credentials."""
import argparse,hashlib,json,pathlib,re,shutil

parser=argparse.ArgumentParser()
parser.add_argument('--original-dir',required=True,type=pathlib.Path)
parser.add_argument('--output-dir',required=True,type=pathlib.Path)
args=parser.parse_args();source=args.original_dir.resolve();out=args.output_dir.resolve()
if out.exists():raise SystemExit('El destino existe; no se sobrescribira.')
sources=[p for p in source.glob('*.gs') if p.stem in ('Codigo','Código','Code')]
if len(sources)!=1:raise SystemExit('Se requiere una unica fuente original de router.')
code=sources[0].read_text(encoding='utf-8-sig')
match=re.search(r'^const ROOT_FOLDER_ID\s*=\s*[\"\']([^\"\']+)[\"\']\s*;',code,re.M)
if not match:raise SystemExit('No hay declaracion de raiz verificable en la fuente original.')
# Retain source provenance: fresh API captures replace the older operational-GET companion file.
legacy=source/'CAPACIDADES_ORIGINAL_PRIVADAS.json'
capture=source.parent/'CAPTURA_PRIVADA.json'
if legacy.exists():
    if json.loads(legacy.read_text(encoding='utf-8-sig')).get('rootFolderId')!=match[1]:
        raise SystemExit('La raiz no coincide con la evidencia operativa original.')
elif capture.exists():
    proof=json.loads(capture.read_text(encoding='utf-8-sig'))
    expected={p['name']:p['sha256'] for p in proof.get('files',[]) if p.get('scope')=='v21'}
    actual={p.name:hashlib.sha256(p.read_bytes()).hexdigest() for p in source.iterdir() if p.is_file()}
    if proof.get('scriptIdMatches') is not True or proof.get('title')!='GBO_Inventario' or not expected or actual!=expected:
        raise SystemExit('Captura V21 incompleta, alterada o de otro proyecto. No se produjo paquete.')
else:raise SystemExit('Falta evidencia de procedencia de la fuente original.')
start=code.index('function doGet()');end=code.index('function doPost(',start)
before=code[start:end]
if 'commitContinuationVersion:' in before:raise SystemExit('El original ya tiene continuidad: revisar diferencias, no aplicar dos veces.')
fragment="""ok: true,
    rootGuardVersion: 1,
    rootGuardReady: rootsReady,
    rootRoutingVersion: admission ? 2 : 0,
    rootEpoch: admission ? admission.epoch : '',
    rootActivatedAt: admission ? admission.activatedAt : 0,
    commitContinuationVersion: typeof v3Enabled==='function' && v3Enabled() && rootsReady && PropertiesService.getScriptProperties().getProperty('GBO_RESUMABLE_ROOT_ID')===ROOT_FOLDER_ID ? 1 : 0,
    publicNamingVersion: typeof v3Enabled==='function' && v3Enabled() && rootsReady && typeof PublicDeliveryNames!=='undefined' && PropertiesService.getScriptProperties().getProperty('GBO_RESUMABLE_ROOT_ID')===ROOT_FOLDER_ID && PropertiesService.getScriptProperties().getProperty('GBO_PUBLIC_NAMES_ENABLED')==='true' ? 1 : 0,"""
if before.count('ok: true,')!=1:raise SystemExit('doGet distinto del contrato revisado; no se produjo paquete.')
# A capability GET verifies roots once, not once for each advertised field.
header='function doGet() {'
if before.count(header)!=1:raise SystemExit('Cabecera GET distinta: revisar fuente.')
before=before.replace(header,header+'''
  let rootsReady=false,admission=null;
  try {
    const dual=gboRootRoutingEnabled();
    rootsReady=dual?gboRootRoutingReady():gboRootReady();
    if(dual&&rootsReady)admission=gboRootRoutingConfig().admission;
  }catch(e){rootsReady=false;admission=null;}
''',1)
code=code[:start]+before.replace('ok: true,',fragment,1)+code[end:]
# Guard every original root entrypoint, including legacy SIC/SCAP/exports and video.
# Preserve the literal root now; an authorized cut is a separate manual operation.
root_lookup=re.compile(r'DriveApp\.getFolderById\(\s*ROOT_FOLDER_ID\s*\)')
if not root_lookup.search(code):raise SystemExit('No se encontraron entradas de raiz; revisar el original.')
code=root_lookup.sub('gboAuthorizedRoot()',code)
router='    if (data.action === "beginVideoV1" || data.action === "finishVideoV1")'
if code.count(router)!=1:raise SystemExit('Router distinto del revisado; no se produjo paquete.')
code=code.replace(router,"    // Authenticated requests fail closed before any legacy/cached write path.\n    try {gboUploadRootsGate();gboAssertRootRoutingAction(data);}catch(rootError){return jsonResponse({ok:false,code:rootError.code||'ROOT_VERIFICATION_FAILED',retryable:rootError.retryable===true});}\n"+router,1)
out.mkdir(parents=True)
unchanged=['appsscript.json','ScapPhotoPanelV2.gs','SicObservationsV1Core.gs','VideoNamesV2Core.gs','VideoV1Core.gs']
for name in unchanged:shutil.copy2(source/name,out/name)
video=(source/'VideoV1.gs').read_text(encoding='utf-8-sig')
def exact(text,old,new):
    if text.count(old)!=1:raise SystemExit('Fuente V21 distinta: no aplicar transformacion de video.')
    return text.replace(old,new,1)
video=exact(video,'const s=videoV1Store();','const s=videoV1Store(p);')
video=exact(video,'function videoV1Store() {','function videoV1Store(p) {\n  const route=gboVideoStoreContext(p);')
video=exact(video,'const root=DriveApp.getFolderById(ROOT_FOLDER_ID),technical=', 'const root=route.root,technical=')
video=exact(video,'parent:key=>v3Store().get(key)', 'parent:key=>key===p.parentKey?route.parent:null')
video=exact(video,"retryable:true});}\n  finally", "retryable:e.retryable===undefined?true:e.retryable===true});}\n  finally")
# The original binary resumable implementation, SHA validation and immutable media IDs remain intact.
if 'v3Store(' in video:raise SystemExit('El adaptador video conserva inicializacion con efectos secundarios.')
candidate=pathlib.Path(__file__).resolve().parent
if video!=(candidate/'VideoV1.gs').read_text(encoding='utf-8'):raise SystemExit('VideoV1 real difiere del adaptador versionado revisado.')
(out/'VideoV1.gs').write_text(video,encoding='utf-8')
for name in ['SyncV3.gs','SyncV3Core.gs','PhotoNamesV2Core.gs','PublicDeliveryNames.gs','AuthorizedRootGuard.gs','DeliveryRootRouting.gs','NewRootAdmission.gs']:shutil.copy2(candidate/name,out/name)
(out/'Codigo.gs').write_text(code,encoding='utf-8')
manifest={'localOnly':True,'newProject':False,'deployAttempted':False,'rootLiteralPreservedFromV21':True,'routingContractVersion':2,'privateConfigurationNotActivated':True,'originalSource':str(source),'sourceHashes':{p.name:hashlib.sha256(p.read_bytes()).hexdigest() for p in out.iterdir() if p.is_file()}}
(out/'MANIFIESTO_PRIVADO.json').write_text(json.dumps(manifest,indent=2),encoding='utf-8')
print('Paquete local preparado. Raiz original conservada; sin despliegue. '+str(out))
