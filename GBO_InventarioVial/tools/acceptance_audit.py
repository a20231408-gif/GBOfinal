"""Read-only production audit helpers; evidence is written under docs only."""
from pathlib import Path
import hashlib
import json
import re
import sys
import xml.etree.ElementTree as ET

ROOT = Path(__file__).resolve().parents[1]
OUT = ROOT / 'docs/acceptance-audit-v15-2026-10-10'

def save(name, value):
    path = OUT / name
    path.parent.mkdir(parents=True, exist_ok=True)
    path.write_text(json.dumps(value, ensure_ascii=False, indent=2) + '\n', encoding='utf-8')

def digest(path):
    return hashlib.sha256(path.read_bytes()).hexdigest()

def protected():
    files = set()
    for folder in ('app/src', 'app/schemas', 'buildSrc/src', 'backend', 'google-apps-script', 'gradle', 'docs/reference'):
        files.update(p for p in (ROOT / folder).rglob('*') if p.is_file())
    files.update(p for p in (ROOT / 'docs').glob('*.md'))
    for name in ('app/build.gradle.kts', 'build.gradle.kts', 'buildSrc/build.gradle.kts',
                 'settings.gradle.kts', 'gradle.properties', 'gradlew', 'gradlew.bat',
                 'drive.local.properties', 'server.local.properties', 'keystore.properties'):
        if (ROOT / name).is_file(): files.add(ROOT / name)
    files.update((ROOT / 'app/build/outputs/apk/release').glob('*.apk'))
    return {p.relative_to(ROOT).as_posix(): digest(p) for p in sorted(files)}

def snapshot():
    target = OUT / 'evidence/protected-before.json'
    assert not target.exists(), 'Do not overwrite the initial snapshot'
    data = protected()
    save('evidence/protected-before.json', data)
    print(f'Initial protected snapshot: {len(data)} files')

def verify():
    before = json.loads((OUT / 'evidence/protected-before.json').read_text(encoding='utf-8'))
    current = protected()
    changed = [name for name, sha in before.items() if current.get(name) != sha]
    added = sorted(set(current) - set(before))
    allowed_added = {'app/src/test/java/com/tuempresa/inventariovial/AcceptanceDeliveryAuditTest.kt'}
    save('evidence/protected-after.json', {'changed': changed, 'added': added,
        'production_unchanged': not changed and set(added) <= allowed_added})
    assert not changed and set(added) <= allowed_added, (changed, added)
    print('Protected production, schemas, configuration, references and Release APK unchanged')

def tests():
    cases = {}
    failures = []
    baseline = json.loads((ROOT / 'docs/field-session-defaults-evidence/final-tests.json').read_text(encoding='utf-8'))['cases']
    xml_folder = ROOT / (sys.argv[2] if len(sys.argv) > 2 else 'app/build/test-results/testDebugUnitTest')
    for path in sorted(xml_folder.glob('TEST-*.xml')):
        for case in ET.parse(path).getroot().iter('testcase'):
            key = case.attrib['classname'] + '#' + case.attrib['name']
            failure = case.find('failure')
            error = case.find('error')
            issue = failure if failure is not None else error
            state = 'FAILED' if issue is not None else 'SKIPPED' if case.find('skipped') is not None else 'PASSED'
            cases[key] = state
            if issue is not None:
                trace = issue.text or ''
                failures.append({'case': key, 'type': issue.attrib.get('type'),
                    'message': issue.attrib.get('message'), 'trace': trace,
                    'baseline_failed': baseline.get(key) == 'FAILED'})
    result = {'xml_source': xml_folder.relative_to(ROOT).as_posix(), 'total': len(cases), 'passed': list(cases.values()).count('PASSED'),
        'failed': len(failures), 'cases': cases, 'failures': failures,
        'new_failures': [f['case'] for f in failures if not f['baseline_failed']],
        'missing_baseline_cases': sorted(set(baseline) - set(cases))}
    save('evidence/junit-analysis.json', result)
    print(json.dumps({k: v for k, v in result.items() if k not in ('cases', 'failures', 'missing_baseline_cases')}, ensure_ascii=False))
    print('Missing baseline cases:', len(result['missing_baseline_cases']))
    for failure in failures:
        print(failure['case'].split('.')[-1], (failure['message'] or '')[:500].replace('\n', ' '))

def index():
    sources = []
    for path in sorted((ROOT / 'docs').glob('*.md')):
        text = path.read_text(encoding='utf-8', errors='replace')
        sources.append({'file': path.relative_to(ROOT).as_posix(),
            'headings': [{'line': i, 'title': line.strip('# ')} for i, line in enumerate(text.splitlines(), 1) if line.startswith('#')],
            'historical_notice': text.startswith('> DOCUMENTO HISTÓRICO')})
    save('evidence/document-index.json', sources)
    print(f'Indexed {len(sources)} project documents')

def health():
    import urllib.request
    import urllib.error
    import urllib.parse
    import zipfile
    # Only extract public endpoint URLs from the APK. No properties or tokens are read.
    apk = next((ROOT / 'app/build/outputs/apk/release').glob('*.apk'))
    with zipfile.ZipFile(apk) as archive:
        dex = b''.join(archive.read(n) for n in archive.namelist() if re.fullmatch(r'classes\d*\.dex', n))
    urls = set(re.findall(rb'https://script\.google\.com/macros/s/[A-Za-z0-9_-]+/exec', dex))
    result = {'read_only_get': True, 'token_read': False, 'token_sent': False,
        'endpoint_source': 'Release APK public HTTPS URL', 'apk_endpoint_count': len(urls)}
    if not urls:
        result['error'] = 'No unique public Apps Script URL in Release APK'
        save('evidence/endpoint-health.json', result)
        print(json.dumps(result, ensure_ascii=False))
        return
    endpoints = []
    for raw_url in sorted(urls):
        url = raw_url.decode('ascii')
        endpoints.append(probe_endpoint(url))
    result['endpoints'] = endpoints
    save('evidence/endpoint-health.json', result)
    print(json.dumps(result, ensure_ascii=False))

def probe_endpoint(url):
    import urllib.request
    import urllib.error
    import urllib.parse
    result = {'public_url_sha256': hashlib.sha256(url.encode()).hexdigest()}
    parsed = urllib.parse.urlsplit(url)
    assert parsed.scheme == 'https' and parsed.hostname == 'script.google.com' and re.fullmatch(r'/macros/s/[^/]+/exec', parsed.path)
    try:
        with urllib.request.urlopen(urllib.request.Request(url, method='GET'), timeout=30) as response:
            body = response.read(1024 * 1024)
            result['http_status'] = response.status
        try:
            data = json.loads(body)
            allowed = ('ok', 'version', 'deliveryProtocol', 'pavementProtocol', 'scapProtocol',
                       'scapBundleVersion', 'pathVersion', 'photoPolicy', 'strictElementFoldering')
            result['capabilities'] = {key: data[key] for key in allowed if key in data}
            result['json_response'] = True
        except (ValueError, TypeError):
            result['json_response'] = False
            result['error'] = 'Endpoint did not return JSON'
    except urllib.error.HTTPError as error:
        result['http_status'] = error.code
        result['error'] = 'HTTP_ERROR'
    except Exception as error:
        result['error'] = type(error).__name__
    return result

if __name__ == '__main__':
    {'snapshot': snapshot, 'verify': verify, 'tests': tests, 'index': index, 'health': health}[sys.argv[1]]()
