"""Compare owner-produced REAL Google reports; never contacts Google."""
import argparse, json, statistics, zipfile, re
from pathlib import Path
import xml.etree.ElementTree as ET

def percentile(values,p):
    values=sorted(values)
    return values[max(0, int(len(values)*p+0.999999)-1)] if values else None

def summarize(report):
    groups={}
    for event in report['events']:
        # All artificial holder/status/competitor time is excluded from benchmarks.
        if event.get('scenario')=='FORCED_BUSY' or event['action'] in ['testHoldLock','testLockStatus']:continue
        # Retries and BUSY remain visible; success-only percentile also separated.
        key=event['action']+'/'+str(event.get('family') or 'OTHER')+'/'+str(event.get('scenario') or 'NORMAL'); g=groups.setdefault(key,{'attempts':0,'busy':0,'errors':0,'clientMs':[],'businessMs':[],'serverMs':[],'locks':[],'phases':{},'operations':{}})
        g['attempts']+=1;g['busy']+=event.get('code')=='BUSY';g['errors']+=event.get('ok') is False
        if event.get('clientMs') is not None:g['clientMs'].append(event['clientMs'])
        m=event.get('metrics',{}) or {}
        if m.get('businessMs') is not None:g['businessMs'].append(m['businessMs'])
        if m.get('totalServerMs') is not None:g['serverMs'].append(m['totalServerMs'])
        g['locks']+=m.get('locks',[])
        for name,p in m.get('phases',{}).items():
            t=g['phases'].setdefault(name,{'count':0,'ms':0});t['count']+=p['count'];t['ms']+=p['ms']
        for name,count in m.get('operations',{}).items():g['operations'][name]=g['operations'].get(name,0)+count
    for g in groups.values():
        for name in ['clientMs','businessMs','serverMs']:
            v=g.pop(name);g[name]={'n':len(v),'p50':statistics.median(v) if v else None,'p95':percentile(v,.95),'total':sum(v)}
        v=g.pop('locks');g['locks']={'n':len(v),'waitMs':sum(x['waitMs'] for x in v),'heldMs':sum(x['heldMs'] for x in v),'busy':sum(not x['acquired'] for x in v)}
    return groups

def stable_manifest(m):
    return {'key':m['key'],'revision':m['revision'],'contentHash':m['contentHash'],'kind':m['kind'],
            'files':sorted([{k:f.get(k) for k in ['key','sha256','logicalPath','requestedPath']} for f in m['files']],key=lambda x:x['key']),
            'rows':sorted(m['sicRows'],key=lambda x:(x['format'],x['recordId']))}

def check_xlsx(folder):
    result=[];ns={'s':'http://schemas.openxmlformats.org/spreadsheetml/2006/main'}
    for p in sorted(folder.glob('*.xlsx')):
        with zipfile.ZipFile(p) as z:
            assert z.testzip() is None
            sheet=ET.fromstring(z.read('xl/worksheets/sheet1.xml'))
            rows=sheet.findall('s:sheetData/s:row',ns)
            heads=[r for r in rows if r.attrib.get('r')=='13']
            is_sic=bool(heads);start=14 if is_sic else 3
            cells=sheet.findall('.//s:c',ns);obscells=[c for c in cells if re.fullmatch(r'N\d+',c.attrib.get('r','')) and int(c.attrib['r'][1:])>=14] if is_sic else []
            for c in obscells:
                assert 's' not in c.attrib, 'Observaciones data cell has style'
                assert c.find('s:f',ns) is None,'Observation executed as formula'
            text=''.join(sheet.itertext())
            if p.name=='observaciones-heredadas.xlsx':assert '=1+1 PINTADO OK 57 E2' in text,'Historical observation lost'
            elif is_sic:assert 'PINTADO OK' not in text,'Explicit blank did not clear observation'
            result.append({'file':p.name,'validZip':True,'rows':sum(int(r.attrib['r'])>=start for r in rows),'observationsPlain':True,'sheetSha256':__import__('hashlib').sha256(z.read('xl/worksheets/sheet1.xml')).hexdigest()})
    assert len(result)>=3,'Missing downloaded real consolidated XLSX evidence'
    return result

def main():
    a=argparse.ArgumentParser();a.add_argument('base');a.add_argument('candidate');a.add_argument('--out',default='COMPARACION_GOOGLE.json');args=a.parse_args()
    bpath=Path(args.base);cpath=Path(args.candidate);b=json.loads(bpath.read_text('utf-8'));c=json.loads(cpath.read_text('utf-8'))
    assert b['environment']==c['environment']=='REAL_GOOGLE_REQUESTS_FROM_OWNER_RUNNER'
    assert b['status']==c['status']=='PROTOCOL_CHECKS_PASSED_REVIEW_XLSX_AND_GOOGLE_LOGS'
    assert b['run']['variant']=='BASE' and c['run']['variant']=='CANDIDATO'
    assert not b.get('recoveryContinuation') and not c.get('recoveryContinuation'),'Recovery continuation is not a paired performance benchmark'
    assert b.get('harnessRevision')==c.get('harnessRevision')==2,'Both variants must use the same helper revision'
    assert b.get('forcedBusyTestVersion')==c.get('forcedBusyTestVersion')==2,'Use the identical corrected contention test'
    assert b.get('artificialContentionExcluded') is True and c.get('artificialContentionExcluded') is True
    assert b.get('forcedBusy',{}).get('outcome')==c.get('forcedBusy',{}).get('outcome')=='VERIFIED_BUSY'
    assert b.get('guardPolicy')==c.get('guardPolicy')=='FRESH_WRITES_LOCKED_READ_CACHE_V2'
    assert b['run']['runId']!=c['run']['runId'] and b['run']['folderId']!=c['run']['folderId']
    assert b['videoFixture']==c['videoFixture'],'Use the very same fictitious video bytes for both variants'
    assert b['photoFixture']==c['photoFixture'],'Photo profiles and bytes must match'
    assert b['extraPerFamily']==c['extraPerFamily']
    bm=sorted(map(stable_manifest,b['snapshot']['manifests']),key=lambda x:x['key']);cm=sorted(map(stable_manifest,c['snapshot']['manifests']),key=lambda x:x['key']);assert bm==cm,'UUID/revision/hash/path/row divergence'
    def xlsx(r):return {f['path']:f['parts'] for f in r['snapshot']['files'] if 'parts' in f and '/EXCEL_SIC/' in f['path']}
    assert xlsx(b)==xlsx(c),'Excel part hashes differ; inspect before approving (ZIP container hashes alone may differ)'
    bx=check_xlsx(bpath.parent);cx=check_xlsx(cpath.parent)
    assert sorted(x['rows'] for x in bx if x['file'].startswith('consolidado-'))==sorted(b['expectedRows'].values())
    assert sorted(x['rows'] for x in cx if x['file'].startswith('consolidado-'))==sorted(c['expectedRows'].values())
    assert [{k:v for k,v in x.items() if k!='file'} for x in bx]==[{k:v for k,v in x.items() if k!='file'} for x in cx]
    out={'environment':'REAL_GOOGLE_OWNER_EXECUTIONS','integrityComparison':'PASSED','productionApproval':False,
         'reason':'Requires Google execution log review, repeated runs, contractual templates and owner approval',
         'artificialContentionExcluded':True,'artificialBase':b['forcedBusy'],'artificialCandidate':c['forcedBusy'],
         'base':summarize(b),'candidate':summarize(c),'xlsxBase':bx,'xlsxCandidate':cx}
    Path(args.out).write_text(json.dumps(out,indent=2),encoding='utf-8');print('Integrity comparison passed. Production is NOT automatically approved.')

if __name__=='__main__':main()
