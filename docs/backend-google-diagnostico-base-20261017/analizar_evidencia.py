"""Read only owner-supplied local evidence. Emit aggregates, never full IDs/URLs."""
import collections, hashlib, json
from pathlib import Path
ROOT=Path(__file__).parent
source=Path('D:/Downloads/GBO_BACKEND_PRUEBAS_V20_20261017/RESULTADOS_GOOGLE')
dirs=list(source.glob('BASE_*/resultado.json'))
assert len(dirs)==1,'Select one source explicitly; do not guess between runs'
p=dirs[0];r=json.loads(p.read_text('utf-8-sig'))
events=[json.loads(x) for x in p.with_name('eventos.jsonl').read_text('utf-8-sig').splitlines() if x]
cache=json.loads(p.with_name('avance.json').read_text('utf-8-sig'))
result={'source':'OWNER_REAL_GOOGLE_BASE_HELPERS_V1','eventCount':len(events),'outcomes':dict(collections.Counter('OK' if x.get('ok') else x.get('code','UNKNOWN') for x in events)),
        'cachedParentAcks':sum(isinstance(x,dict) and 'ack' in x for x in cache.values()),'status':r['status'],
        'successfulEventsWithoutMetrics':[x['sequence'] for x in events if x.get('ok') and not x.get('metrics')],
        'lastEvent':{k:v for k,v in events[-1].items() if k in ['sequence','action','scenario','ok','clientMs','code']},
        'exactStopConditionRecoverableFromOldLogs':False,'rawAck119Recorded':False,'failureConditionRecorded':bool(r.get('failure')),
        'jsonlVsResultOnlyDiscardFlagDifferences':sum(a!=b for a,b in zip(r['events'],events)),
        'jsonlOtherDifferences':sum({k:v for k,v in a.items() if k!='responseDiscardedByClient'}!={k:v for k,v in b.items() if k!='responseDiscardedByClient'} for a,b in zip(r['events'],events)),
        'longCommits':[],'transportDurationsMs':[round(x['clientMs']) for x in events if x.get('code')=='TRANSPORT_FAILED'],
        'realGoogleExecutionsByCodex':0,'postPatchGoogleMeasurements':0}
for x in events:
    m=x.get('metrics') or {}
    if x.get('action')=='commitRecordV3' and m.get('businessMs',0)>150000:
        guard=m['phases']['guard.metadata.get'];unzip=m['phases'].get('cpu.unzip');unpack=m['phases'].get('store.unpackWorkbook')
        result['longCommits'].append({'sequence':x['sequence'],'family':x['family'],'serverMs':m['totalServerMs'],'scriptLocks':m['locks'],'guard':guard,'guardPercentOfServer':round(100*guard['ms']/m['totalServerMs'],2),'cpuUnzip':unzip,'unpackWorkbook':unpack})
(ROOT/'paquete/evidencia/ANALISIS_CORRIDA_REAL_REDACTADO.json').write_text(json.dumps(result,indent=2),encoding='utf-8')
print(json.dumps({'events':result['eventCount'],'outcomes':result['outcomes'],'cachedParents':result['cachedParentAcks'],'exactStopCondition':'NOT_RECORDED','realGoogleCallsByCodex':0}))
