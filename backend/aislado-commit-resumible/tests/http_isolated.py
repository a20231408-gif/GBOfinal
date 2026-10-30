"""Real HTTP tests, only for the explicitly approved private GBO test deployment.
Never accepts endpoints or tokens as command-line arguments. Never logs either.
"""
import argparse,hashlib,json,pathlib,re,time,urllib.request,urllib.parse

root=pathlib.Path('C:/GBO')
private=root/'tmp/entrega-20261030/backend-privado/desbloqueo'
public=root/'tmp/desbloqueo-20261030'
cli=argparse.ArgumentParser();cli.add_argument('mode',choices=['probe','stage','commit','busy','foreign']);cli.add_argument('--props',default='propiedades-http-privadas.json');a=cli.parse_args()
assert pathlib.Path(a.props).name==a.props
properties=json.loads((private/a.props).read_text(encoding='utf-8'))
deployment=(private/'http-despliegue-autorizado.txt').read_text(encoding='utf-8')
urls=set(re.findall(r'https://script\.google\.com/macros/s/[A-Za-z0-9_-]+/exec',deployment));assert len(urls)==1
endpoint=next(iter(urls));token=properties['GBO_DRIVE_API_TOKEN'];test_root=properties['GBO_ISOLATED_ROOT_ID']
old={}
for line in (root/'drive.local.properties').read_text(encoding='utf-8-sig').splitlines():
 if '=' in line and not line.lstrip().startswith('#'):
  k,v=line.split('=',1);old[k.strip()]=re.sub(r'\\([:=/\\])',r'\1',v.strip())
assert endpoint!=old['DRIVE_WEB_APP_URL'] and token!=old['DRIVE_API_TOKEN'] and len(token)>=32 and len(test_root)>10
p=json.loads((root/'docs/scap-integracion-actual-20261027/pruebas/fixtures/ANDROID_SCAP_54.json').read_text(encoding='utf-8'))
scope=hashlib.sha256((endpoint+'\0'+test_root).encode()).hexdigest()[:16]
run=str(time.time_ns());metrics=[]
def record(row):
 row={'case':'fresh' if 'fresh' in a.props else 'recovery',**row}
 metrics.append(row);print(json.dumps(row),flush=True)
 (public/('http-'+run+'.json')).write_text(json.dumps(metrics,indent=2),encoding='utf-8')
def call(phase,payload=None,valid_token=True):
 start=time.monotonic()
 req=urllib.request.Request(endpoint,headers={'Accept':'application/json'})
 if payload is not None:
  req=urllib.request.Request(endpoint,data=json.dumps({**payload,'token':token if valid_token else 'INVALID_TEST_TOKEN'}).encode(),headers={'Content-Type':'application/json','Accept':'application/json'},method='POST')
 try:
  with urllib.request.urlopen(req,timeout=180) as h:
   raw=h.read();status=h.status
  response=json.loads(raw)
 except Exception as exc:
  record({'phase':phase,'errorType':type(exc).__name__,'seconds':round(time.monotonic()-start,3)});raise RuntimeError('HTTP test did not obtain JSON; private endpoint not displayed') from None
 (private/('http-response-'+run+'-'+str(len(metrics))+'.json')).write_bytes(raw)
 record({'phase':phase,'status':status,'ok':response.get('ok'),'code':response.get('code'),'seconds':round(time.monotonic()-start,3)})
 return response
caps=call('CAPABILITIES')
assert caps.get('ok') and all(caps.get(k)==v for k,v in {'syncProtocol':3,'pathVersion':4,'readablePhotoNamingVersion':2,'scapPhotoPanelVersion':2,'commitContinuationVersion':1}.items())
assert 'rootFolderId' not in caps
if a.mode=='probe':
 denied=call('INVALID_AUTHORIZATION',{'action':'reservePhotoNamesV2'},False);assert denied.get('ok') is False and denied.get('error')=='No autorizado'
 names=call('RESERVATION',{**p,'action':'reservePhotoNamesV2','photos':[{'id':f['key'][6:],'category':f['photoCategory']} for f in p['files'] if f['key'].startswith('photo/')]})
 assert names.get('ok');assert names['key']==p['key'] and names['revision']==p['revision']
 for f in p['files']:
  if f['key'].startswith('photo/'):assert next(x for x in names['photos'] if x['id']==f['key'][6:])['name']==f['path'].split('/')[-1]
 record({'phase':'PROBE_VERIFIED','testOnly':True})
elif a.mode=='stage':
 journal=private/('http-stages-'+scope+'.json')
 saved=json.loads(journal.read_text(encoding='utf-8')) if journal.exists() else {'contentHash':p['contentHash'],'stages':{}}
 assert saved['contentHash']==p['contentHash']
 for n,f in enumerate(p['files']):
  if f['key'] in saved['stages']:continue
  ack=call('STAGE_'+str(n+1),{'action':'stageRecordV3','syncProtocol':3,'key':p['key'],'revision':p['revision'],'file':f})
  assert ack.get('ok') and ack['key']==p['key'] and ack['revision']==p['revision'] and ack['fileKey']==f['key'] and ack['sha256']==f['sha256'] and ack.get('stageFileId')
  saved['stages'][f['key']]=ack['stageFileId'];temporary=journal.with_suffix('.pending');temporary.write_text(json.dumps(saved),encoding='utf-8');temporary.replace(journal)
 record({'phase':'STAGING_VERIFIED','files':len(saved['stages']),'testOnly':True})
else:
 journal=private/('http-stages-'+scope+'.json')
 ids=json.loads(journal.read_text(encoding='utf-8'))['stages'] if journal.exists() else json.loads(properties['GBO_TEST_STAGE_IDS'])
 if a.mode=='foreign':ids=json.loads(json.loads((private/'propiedades-http-privadas.json').read_text(encoding='utf-8'))['GBO_TEST_STAGE_IDS'])
 q=json.loads(json.dumps(p));q['action']='commitRecordV3'
 for f in q['files']:f['stageFileId']=ids[f['key']];del f['base64']
 if a.mode=='busy':q.update(key='SCAP/FICTITIOUS_CONCURRENT',recordId='FICTITIOUS_CONCURRENT')
 ack=call('FOREIGN_STAGE' if a.mode=='foreign' else 'COMMIT' if a.mode=='commit' else 'CONCURRENT_COMMIT',q)
 if a.mode=='busy':assert ack.get('ok') is False and ack.get('code')=='BUSY';record({'phase':'BUSY_VERIFIED','testOnly':True})
 elif a.mode=='foreign':assert ack.get('ok') is False and ack.get('code')=='STAGE_LOCATION_CONFLICT';record({'phase':'FOREIGN_STAGE_REFUSED','testOnly':True})
 elif not ack.get('ok'):assert ack.get('retryable') and ack.get('code') in ['COMMIT_IN_PROGRESS','TEST_INTERRUPTED','BUSY'];record({'phase':'UNCONFIRMED_PRESERVED','code':ack['code'],'testOnly':True})
 else:
  assert ack['key']==p['key'] and ack['revision']==p['revision'] and ack['contentHash']==p['contentHash']
  assert len(ack['files'])==59 and len({x['fileId'] for x in ack['files']})==59 and len(ack['sicFiles'])==3
  for f in p['files']:
   item=next(x for x in ack['files'] if x['key']==f['key']);assert item['sha256']==f['sha256'] and item['requestedPath']==f['path'] and item.get('folderId') and item.get('fileId')
  assert {x['format'] for x in ack['sicFiles']}=={x['format'] for x in p['sicRows']}
  assert all(p['identityId'] in x['recordIds'] for x in ack['sicFiles'])
  (private/('http-ack-'+scope+'-'+run+'.json')).write_text(json.dumps(ack),encoding='utf-8')
  record({'phase':'ACK_ENVELOPE_VERIFIED','files':59,'sicFiles':3,'alreadyExists':ack.get('alreadyExists'),'driveBytesVerificationPending':True,'testOnly':True})
