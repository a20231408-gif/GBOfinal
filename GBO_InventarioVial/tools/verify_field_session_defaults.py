"""Compare regression results and protected source hashes for the session fix."""
import hashlib
import json
from pathlib import Path
import sys
import xml.etree.ElementTree as ET

ROOT = Path(__file__).resolve().parents[1]
OUT = ROOT / "docs/field-session-defaults-evidence"
mode = sys.argv[1]
if mode == "tests":
    cases = {}
    for file in sorted((ROOT / "app/build/test-results/testDebugUnitTest").glob("TEST-*.xml")):
        for case in ET.parse(file).getroot().iter("testcase"):
            key = case.attrib["classname"] + "#" + case.attrib["name"]
            assert key not in cases, key
            cases[key] = "FAILED" if case.find("failure") is not None or case.find("error") is not None else "SKIPPED" if case.find("skipped") is not None else "PASSED"
    baseline = json.loads((OUT / "baseline-tests.json").read_text(encoding="utf-8"))["cases"]
    result = {"total": len(cases), "passed": list(cases.values()).count("PASSED"),
              "failed": list(cases.values()).count("FAILED"), "cases": cases,
              "new_failures": [key for key, state in cases.items() if state == "FAILED" and baseline.get(key) != "FAILED"],
              "missing_cases": sorted(set(baseline) - set(cases)),
              "changed_baseline_results": {key: [state, cases.get(key)] for key, state in baseline.items() if state != cases.get(key)},
              "regression_cases": {key: state for key, state in cases.items() if ".FieldSessionDefaultsTest#" in key or ".FieldSessionFormsTest#" in key}}
    (OUT / "final-tests.json").write_text(json.dumps(result, ensure_ascii=False, indent=2) + "\n", encoding="utf-8")
    print(json.dumps({key: value for key, value in result.items() if key != "cases"}, ensure_ascii=False))
    assert not result["new_failures"] and not result["missing_cases"], "New failures or missing baseline cases"
    assert len(result["regression_cases"]) == 9 and set(result["regression_cases"].values()) == {"PASSED"}, "Regression suite incomplete"
elif mode == "scope":
    before = json.loads((OUT / "source-before.json").read_text(encoding="utf-8"))
    changed = sorted(name for name, digest in before.items() if not (ROOT / name).is_file()
                     or hashlib.sha256((ROOT / name).read_bytes()).hexdigest() != digest)
    allowed = {"app/src/main/java/com/tuempresa/inventariovial/" + name for name in (
        "MainActivity.kt", "sic/SicFieldWorkspace.kt", "pavement/PavementHeader.kt", "pavement/PavementRepository.kt",
        "pavement/ui/PavementDialogs.kt", "scap/data/ScapRepository.kt", "scap/ui/ScapWorkspace.kt")}
    allowed.update("app/src/test/java/com/tuempresa/inventariovial/" + name for name in (
        "SicUiParityTest.kt", "TabletUxUiTest.kt"))
    assert set(changed) <= allowed, changed
    added = sorted(file.relative_to(ROOT).as_posix() for file in (ROOT / "app/src").rglob("*")
                   if file.is_file() and file.relative_to(ROOT).as_posix() not in before)
    expected_added = {"app/src/main/java/com/tuempresa/inventariovial/field/FieldSessionDefaults.kt",
                      "app/src/test/java/com/tuempresa/inventariovial/FieldSessionDefaultsTest.kt",
                      "app/src/test/java/com/tuempresa/inventariovial/FieldSessionFormsTest.kt"}
    assert set(added) == expected_added, added
    result = {"tracked_files": len(before), "changed_files": changed, "added_source_files": added,
              "room_schema_auth_tablet_admin_backend_apps_script_gradle_unchanged": True}
    (OUT / "scope-verification.json").write_text(json.dumps(result, indent=2) + "\n", encoding="utf-8")
    print(json.dumps(result, ensure_ascii=False))
else:
    raise SystemExit("Expected tests or scope")
