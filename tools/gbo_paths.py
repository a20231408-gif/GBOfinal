"""Local development paths. No credentials or service configuration are read here."""
from pathlib import Path
import os,shutil
ROOT=Path(__file__).resolve().parents[1]
def java_executable():
 home=os.environ.get("JAVA_HOME")
 if home:
  p=Path(home)/"bin"/("java.exe"if os.name=="nt"else"java")
  if p.is_file():return p
 found=shutil.which("java")
 if found:return Path(found)
 raise RuntimeError("Configura JAVA_HOME con el JBR de Android Studio o añade java al PATH.")
def android_sdk():
 for k in ["ANDROID_SDK_ROOT","ANDROID_HOME"]:
  if os.environ.get(k):return Path(os.environ[k])
 p=ROOT/"local.properties"
 if p.exists():
  for line in p.read_text("utf-8-sig").splitlines():
   if line.startswith("sdk.dir="):return Path(line.split("=",1)[1].replace("\\:",":").replace("\\\\","\\"))
 raise RuntimeError("Configura sdk.dir en local.properties o ANDROID_SDK_ROOT.")
