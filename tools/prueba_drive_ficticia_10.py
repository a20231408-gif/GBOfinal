"""Owner-authorized isolated reproduction. No deployment, real records, deletion or secrets in output."""
import base64, hashlib, io, json, pathlib, subprocess, time, urllib.request, uuid
from PIL import Image

root = pathlib.Path(__file__).resolve().parents[1]
out = root / 'docs/campo-10/evidencia'
out.mkdir(parents=True, exist_ok=True)
props = {}
for line in (root / 'drive.local.properties').read_text(encoding='utf-8-sig').splitlines():
    if '=' in line and not line.lstrip().startswith('#'):
        key, value = line.split('=', 1)
        props[key.strip()] = value.strip()

def post(p):
    data = dict(p, token=props['DRIVE_API_TOKEN'])
    request = urllib.request.Request(props['DRIVE_WEB_APP_URL'], data=json.dumps(data).encode(), headers={'Content-Type': 'application/json'})
    try:
        with urllib.request.urlopen(request, timeout=45) as r:
            return json.load(r)
    except Exception as e:
        raise RuntimeError(type(e).__name__) from None

def digest(data):
    return hashlib.sha256(data).hexdigest()

identity_file = out / 'ficha-ficticia-identidad.json'
if identity_file.exists():
    identity = json.loads(identity_file.read_text())
else:
    identity = {'recordId': 'prompt10-ficticio-' + str(uuid.uuid4()), 'revision': int(time.time()*1000), 'routeCode': 'GBO_PRUEBA_10'}
    identity_file.write_text(json.dumps(identity, indent=2)+'\n')
photo_id = identity['recordId'] + '-photo'
image = Image.new('RGB', (96, 64), (40, 120, 180))
png = io.BytesIO(); image.save(png, 'PNG')
jpeg = io.BytesIO(); image.save(jpeg, 'JPEG', quality=95)
jpeg_bytes = jpeg.getvalue()
(out / 'foto-ficticia.jpg').write_bytes(jpeg_bytes)
metadata = json.dumps({'recordId': identity['recordId'], 'purpose': 'PRUEBA FICTICIA PROMPT 10; NO INVENTARIO CONTRACTUAL', 'latitude': None, 'longitude': None}).encode()

def prepared(bytes_):
    p = dict(identity, action='commitRecordV3', syncProtocol=3, pathVersion=4, kind='SIC', key='SIC/'+identity['recordId'],
             baseRevision=0, family='ALCANTARILLA', familyPath='ALCANTARILLAS', roadbedCode='CD', direction='INCREASING',
             startPrNumber=0, startDistanceM=0, orderAxis=0, orderSource='CONTRACTUAL_PR', technicalDeliveryWithoutGps=True,
             technicalDeliveryVersion=1, georeferencing={'status':'PENDING_EXTERNAL','externalSubmetricStatus':'PENDING'},
             photoCategoryVersion=1, photoHasEnds=False, photoClassCode='', photoTypeCode='', sicRows=[], removeMissingOwnedFiles=True)
    p['files'] = [dict(key='photo/'+photo_id, path='PANORAMICA/F_'+digest(photo_id.encode())[:16]+'.jpg', mimeType='image/jpeg',
                       photoCategory='PANORAMICA', sha256=digest(bytes_), base64=base64.b64encode(bytes_).decode()),
                  dict(key='metadata',path='metadata.json',mimeType='application/json',sha256=digest(metadata),base64=base64.b64encode(metadata).decode())]
    code = "const c=require('./google-apps-script/SyncV3Core.js'),crypto=require('node:crypto');let s='';process.stdin.on('data',b=>s+=b);process.stdin.on('end',()=>process.stdout.write(crypto.createHash('sha256').update(c.canonical(JSON.parse(s))).digest('hex')));"
    r = subprocess.run(['node','-e',code],cwd=root,input=json.dumps(p),text=True,capture_output=True,check=True)
    p['contentHash']=r.stdout
    # Exercise the same stage/base64/sha256 transport as AutomaticDriveHttp.
    for f in p['files']:
        ack = post(dict(action='stageRecordV3',syncProtocol=3,key=p['key'],revision=p['revision'],file=f))
        if not ack.get('ok') or ack.get('sha256')!=f['sha256'] or ack.get('fileKey')!=f['key']:
            raise RuntimeError('STAGE_NOT_CONFIRMED: '+str(ack.get('code','unknown')))
        f['stageFileId']=ack['stageFileId']; del f['base64']
    return p

# Persist receipts only, never the token or endpoint. A second invocation only replays this same fictitious identity.
invalid = post(prepared(png.getvalue()))
if invalid.get('code')!='NOT_JPEG':
    raise RuntimeError('EXPECTED_NOT_JPEG: '+str(invalid.get('code','unexpected response')))
good_payload = prepared(jpeg_bytes)
ack = post(good_payload)
if not ack.get('ok') or ack.get('key')!=good_payload['key'] or ack.get('revision')!=good_payload['revision'] or ack.get('contentHash')!=good_payload['contentHash']:
    raise RuntimeError('JPEG_NOT_CONFIRMED: '+str(ack.get('code','invalid ACK')))
confirmed = next((f for f in ack.get('files',[]) if f.get('key')=='photo/'+photo_id), None)
if not confirmed or confirmed.get('sha256')!=digest(jpeg_bytes) or not confirmed.get('fileId'):
    raise RuntimeError('PHOTO_ACK_INCOMPLETE')
retry = post(good_payload)
if not retry.get('ok') or not retry.get('alreadyExists') or retry.get('files')!=ack.get('files'):
    raise RuntimeError('IDEMPOTENCY_NOT_CONFIRMED')
result = {'source':'REAL APPS SCRIPT/DRIVE; isolated fictitious record authorized by owner', 'identity':identity,
          'rejectedCode':invalid['code'], 'ack':ack, 'retrySameIds':True, 'jpegSha256':digest(jpeg_bytes),
          'jpegMd5':hashlib.md5(jpeg_bytes).hexdigest(), 'photoFileId':confirmed['fileId'], 'independentDriveReadback':'PENDING',
          'realBlockedRecordsRecovered':False}
(out/'prueba-drive-real-ficticia.json').write_text(json.dumps(result,indent=2)+'\n',encoding='utf-8')
print(json.dumps({'rejectedCode':invalid['code'],'jpegConfirmed':True,'retrySameIds':True,'photoFileId':confirmed['fileId']}))
