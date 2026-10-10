"""Import the six supplied engineering KMZ axes without simplification."""
import hashlib
import json
import math
from pathlib import Path
import xml.etree.ElementTree as ET
import zipfile

ROOT = Path(__file__).resolve().parents[1]
SOURCES = [
    ('PE-3N', 1, 1, 'Tramo 1 (Ruta Nacional PE-3N).kmz'),
    ('PE-22A', 1, 2, 'Tramo 2 (Ruta Nacional PE-22A).kmz'),
    ('PE-28C', 1, 3, 'Tramo 3 (Ruta Nacional PE-28C).kmz'),
    ('PE-12A', 2, 1, 'Item2-Tramo1 (Ruta Nacional PE-12A).kmz'),
    ('PE-3NG', 2, 2, 'Item 2 - Tramo 2 (Ruta Nacional PE-3NG).kmz'),
    ('PE-28H', 2, 3, 'Item 2 - Tramo3 (Ruta Nacional PE-28H).kmz'),
]

def distance(a, b):
    lon1, lat1, lon2, lat2 = map(math.radians, (*a, *b))
    h = math.sin((lat2-lat1)/2)**2 + math.cos(lat1)*math.cos(lat2)*math.sin((lon2-lon1)/2)**2
    return 2 * 6371008.8 * math.asin(math.sqrt(min(1, h)))

def main(source_dir):
    routes, segments = [], []
    for code, item, tramo, name in SOURCES:
        source = source_dir / name
        with zipfile.ZipFile(source) as archive:
            names = [n for n in archive.namelist() if n.lower().endswith('.kml')]
            assert len(names) == 1
            text = archive.read(names[0]).decode('utf-8-sig')
        # Repair only an undeclared Atom namespace for parsing; source files are untouched.
        if 'ns1:' in text and 'xmlns:ns1=' not in text:
            text = text.replace('<kml ', '<kml xmlns:ns1="http://www.w3.org/2005/Atom" ', 1)
        root = ET.fromstring(text)
        lines = root.findall('.//{*}LineString/{*}coordinates')
        assert len(lines) == 1, name
        raw = [tuple(map(float, p.split(',')[:2])) for p in lines[0].text.split()]
        coords = [p for i, p in enumerate(raw) if i == 0 or p != raw[i-1]]
        reverse = code == 'PE-3N' and distance(coords[-1], (-77.99548975924931, -8.253887590825283)) < distance(coords[0], (-77.99548975924931, -8.253887590825283))
        if reverse:
            coords.reverse()
        chainage, points = 0.0, []
        for i, (lon, lat) in enumerate(coords):
            if i:
                chainage += distance(coords[i-1], coords[i])
            points.append(dict(sequence=i, latitude=lat, longitude=lon, chainageM=chainage))
        segments.append(dict(routeCode=code, roadbedCode='EJE', segmentId=code+'_INGENIERIA_2026', points=points))
        routes.append(dict(routeCode=code, itemCode=str(item), segmentCode=f'Tramo {tramo}',
            engineeringSource=dict(fileName=name, sha256=hashlib.sha256(source.read_bytes()).hexdigest(),
                sourceVertices=len(raw), vertices=len(coords), consecutiveDuplicatesRemoved=len(raw)-len(coords),
                reversedSourceForContractDirection=reverse, geometricLengthM=chainage)))
        print(code, len(coords), round(chainage, 3), f'Item {item} / Tramo {tramo}')
    assets = ROOT / 'app/src/main/assets'
    (assets/'road_reference.json').write_text(json.dumps(dict(segments=segments, prs=[]), ensure_ascii=False, separators=(',', ':')), encoding='utf-8')
    (assets/'project_routes.json').write_text(json.dumps(dict(routes=routes), ensure_ascii=False, indent=2), encoding='utf-8')

if __name__ == '__main__':
    import sys
    main(Path(sys.argv[1]))
