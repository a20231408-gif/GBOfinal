"""Record build-only test/source evidence without inspecting private configuration."""
import hashlib
import json
from pathlib import Path
import re
import sys
import xml.etree.ElementTree as ET

ROOT = Path(__file__).resolve().parents[1]
EVIDENCE = ROOT / "docs/gradle-configuration-cache-evidence"


def tests():
    cases = {}
    for file in sorted((ROOT / "app/build/test-results/testDebugUnitTest").glob("TEST-*.xml")):
        for case in ET.parse(file).getroot().iter("testcase"):
            identity = case.attrib["classname"] + "#" + case.attrib["name"]
            assert identity not in cases, identity
            cases[identity] = (
                "FAILED" if case.find("failure") is not None or case.find("error") is not None
                else "SKIPPED" if case.find("skipped") is not None else "PASSED"
            )
    assert cases, "No test results found"
    return {"total": len(cases), "passed": list(cases.values()).count("PASSED"),
            "failed": list(cases.values()).count("FAILED"), "cases": cases}


mode = sys.argv[1]
if mode in ("baseline", "final"):
    result = tests()
    if mode == "final":
        baseline = json.loads((EVIDENCE / "baseline-tests.json").read_text(encoding="utf-8"))
        result["new_failures"] = [key for key, state in result["cases"].items()
                                  if state == "FAILED" and baseline["cases"].get(key) != "FAILED"]
        result["missing_cases"] = sorted(set(baseline["cases"]) - set(result["cases"]))
        result["changed_results"] = {key: [state, result["cases"].get(key)]
                                     for key, state in baseline["cases"].items()
                                     if state != result["cases"].get(key)}
    destination = EVIDENCE / (mode + "-tests.json")
elif mode == "scope":
    before = json.loads((EVIDENCE / "source-before.json").read_text(encoding="utf-8"))
    changed = [name for name, digest in before.items()
               if not (ROOT / name).is_file()
               or hashlib.sha256((ROOT / name).read_bytes()).hexdigest() != digest]
    protected_roots = ("app/src", "app/schemas", "backend", "google-apps-script", "gradle")
    added = sorted(str(file.relative_to(ROOT)).replace("\\", "/")
                   for folder in protected_roots if (ROOT / folder).exists()
                   for file in (ROOT / folder).rglob("*") if file.is_file()
                   and str(file.relative_to(ROOT)).replace("\\", "/") not in before)
    old_script = (EVIDENCE / "app-build-before.gradle.kts.txt").read_text(encoding="utf-8")
    new_script = (ROOT / "app/build.gradle.kts").read_text(encoding="utf-8")
    def without_guard(script):
        script = re.sub(r"^import (?:java\.security\.(?:KeyStore|MessageDigest|PrivateKey)|com\.gbo\.build\.ValidateReleaseConfigurationTask)\n", "", script, flags=re.M)
        return re.sub(r"val releaseConfigurationCheck = .*?\ntasks\.matching", "tasks.matching", script, count=1, flags=re.S)
    unchanged_outside_guard = without_guard(old_script) == without_guard(new_script)
    result = {"tracked_files": len(before), "changed_files": changed, "added_protected_files": added,
              "app_script_unchanged_outside_guard_and_imports": unchanged_outside_guard}
    assert changed == ["app/build.gradle.kts"] or set(changed) == {"app/build.gradle.kts"}, changed
    assert not added, added
    assert unchanged_outside_guard, "Unexpected change outside the release guard"
    destination = EVIDENCE / "scope-verification.json"
else:
    raise SystemExit("Expected baseline, final or scope")

destination.write_text(json.dumps(result, ensure_ascii=False, indent=2) + "\n", encoding="utf-8")
print(json.dumps({key: value for key, value in result.items() if key != "cases"}, ensure_ascii=False))
if mode == "final":
    assert not result["new_failures"], "New failing tests; see final-tests.json"
    assert not result["missing_cases"], "Missing baseline tests; see final-tests.json"
    assert not result["changed_results"], "Test results changed; see final-tests.json"
