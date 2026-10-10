"""Export mappings from independently extracted, type-specific capture contracts.

Never save/rebuild XLSX sources. Mapping output names match generated Android assets.
"""
import json
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
for kind in ('slab', 'modular'):
    catalog = json.loads((ROOT / f'app/src/main/assets/scap_{kind}_catalog.json').read_text(encoding='utf-8'))
    fields = catalog['fields']
    # Source labels place these values in G/N; inherited Agua Blanca keys had E/L.
    overrides = {'fordDistanceM': 'G340', 'fordPeriod': 'G342', 'fordMinDepthM': 'G344',
                 'fordSoil': 'G346', 'existingDetour': 'G348', 'detourNeeded': 'G350',
                 'parallelLengthM': 'L342', 'parallelSubstructure': 'L344', 'parallelType': 'L346',
                 'profileReference': 'N450', 'scourProtectionType': 'L478'}
    for f in fields:
        if f['key'] in overrides:
            f['valueCell'] = overrides[f['key']]
    (ROOT / f'tools/scap_{kind}_field_map.json').write_text(
        json.dumps(fields, ensure_ascii=False, indent=2), encoding='utf-8')
    print(kind, len(fields), 'mapped fields')
