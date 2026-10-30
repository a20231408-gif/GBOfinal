"""Portable Gradle benchmark for Windows. No install, upload, clean or source deletion.
Source edits for the incremental phase are comments in an explicitly isolated copy only.
"""
from pathlib import Path
import argparse,ctypes,ctypes.wintypes as W,hashlib,json,os,re,subprocess,threading,time,sys,shutil

class Entry(ctypes.Structure):
 _fields_=[('size',W.DWORD),('usage',W.DWORD),('pid',W.DWORD),('heap',ctypes.c_size_t),('module',W.DWORD),('threads',W.DWORD),('parent',W.DWORD),('priority',W.LONG),('flags',W.DWORD),('exe',W.WCHAR*260)]
class Memory(ctypes.Structure):
 _fields_=[('cb',W.DWORD),('faults',W.DWORD)]+[(n,ctypes.c_size_t)for n in ['peak','rss','peakPaged','paged','peakNonPaged','nonPaged','minWs','maxWs','private']]
k=ctypes.WinDLL('kernel32',use_last_error=True)
k.CreateToolhelp32Snapshot.argtypes=[W.DWORD,W.DWORD];k.CreateToolhelp32Snapshot.restype=W.HANDLE
k.Process32FirstW.argtypes=[W.HANDLE,ctypes.POINTER(Entry)];k.Process32NextW.argtypes=[W.HANDLE,ctypes.POINTER(Entry)]
k.OpenProcess.argtypes=[W.DWORD,W.BOOL,W.DWORD];k.OpenProcess.restype=W.HANDLE
k.CloseHandle.argtypes=[W.HANDLE];k.K32GetProcessMemoryInfo.argtypes=[W.HANDLE,ctypes.POINTER(Memory),W.DWORD]
def processes():
 h=k.CreateToolhelp32Snapshot(2,0);e=Entry();e.size=ctypes.sizeof(e);out=[]
 try:
  ok=k.Process32FirstW(h,ctypes.byref(e))
  while ok:out.append((int(e.pid),int(e.parent)));ok=k.Process32NextW(h,ctypes.byref(e))
 finally:k.CloseHandle(h)
 return out

def measure(pid,stop,result):
 seen={pid};peak=0;privatePeak=0
 while not stop.is_set():
  entries=processes();changed=True
  while changed:
   changed=False
   for child,parent in entries:
    if parent in seen and child not in seen:seen.add(child);changed=True
  rss=private=0
  for child in seen:
   h=k.OpenProcess(0x410,False,child)
   if not h:continue
   try:
    m=Memory();m.cb=ctypes.sizeof(m)
    if k.K32GetProcessMemoryInfo(h,ctypes.byref(m),m.cb):rss+=m.rss;private+=m.private
   finally:k.CloseHandle(h)
  peak=max(peak,rss);privatePeak=max(privatePeak,private);stop.wait(.5)
 result.update(peakObservedProcessTreeRssMiB=round(peak/2**20,1),peakObservedPrivateCommitMiB=round(privatePeak/2**20,1),sampleIntervalSeconds=.5,processesObserved=len(seen),memoryMetric='Sampled sum for this process tree; shared pages may be counted twice; excludes Android Studio and unrelated apps.')

def main():
 a=argparse.ArgumentParser();a.add_argument('--project',required=True);a.add_argument('--report',required=True);a.add_argument('--phase',choices=['cold','incremental','tests','release-check','instrumentation-compile'],required=True);a.add_argument('--allow-downloads',action='store_true');a.add_argument('--temp-root');args=a.parse_args()
 root=Path(args.project).resolve();report=Path(args.report).resolve();report.mkdir(parents=True,exist_ok=True)
 assert (root/'settings.gradle.kts').is_file() and (root/'gradlew.bat').is_file()
 runtime=Path(args.temp_root).resolve()if args.temp_root else root.parent/'t';runtime.mkdir(parents=True,exist_ok=True)
 env=os.environ.copy();assert env.get('JAVA_HOME'),'Set JAVA_HOME to the selected JDK/JBR'
 env['PATH']=str(Path(env['JAVA_HOME'])/'bin')+';'+env.get('PATH','');env['TEMP']=str(runtime);env['TMP']=str(runtime)
 secrets=[]
 for name in ['server.local.properties','drive.local.properties','keystore.properties']:
  p=root/name
  if not p.exists():continue
  for line in p.read_text('utf-8-sig').splitlines():
   if '='not in line or line.lstrip().startswith('#'):continue
   key,value=line.split('=',1);value=value.strip()
   if len(value)>3 and any(x in key.upper()for x in ['TOKEN','PASSWORD','SECRET','ALIAS','URL']):secrets.extend([value,value.replace('\\:',':').replace('\\\\','\\')])
 for key,value in env.items():
  if key.startswith('GBO_RELEASE_')and value:secrets.append(value)
 def redact(s):
  for value in sorted(set(secrets),key=len,reverse=True):s=s.replace(value,'[PRIVATE]')
  return re.sub(r'https?://[^\s<>\"\x27]+','[URL OMITTED]',s)
 options=['--no-daemon','--no-configuration-cache','--no-build-cache','--console=plain','--max-workers=1','-Dorg.gradle.jvmargs=-Xmx2048m -XX:ActiveProcessorCount=2 -XX:HeapBaseMinAddress=8g -Dfile.encoding=UTF-8','-Pkotlin.compiler.execution.strategy=in-process']
 if not args.allow_downloads:options.append('--offline')
 changed=None;original=None;before=None
 if args.phase=='cold':
  assert not(root/'app/build').exists(),'Cold phase requires a NEW isolated source export; never clean the Huawei project.'
  task=[':app:assembleDebug']
 elif args.phase=='incremental':
  assert 'GBO_MIG_'in str(root.parent).upper(),'Incremental marker is permitted only in an isolated GBO_MIG_ workspace.'
  changed=root/'app/src/main/java/com/tuempresa/inventariovial/pavement/PavementJointOccurrences.kt';original=changed.read_bytes();before=hashlib.sha256(original).hexdigest();changed.write_bytes(original+b'\n// Migration benchmark: nonfunctional comment in isolated copy.\n');task=[':app:assembleDebug']
 elif args.phase=='tests':
  task=['-I',str(root/'tools/migracion/pruebas.init.gradle'),'-Dgbo.migration.temp='+str(runtime),':app:testDebugUnitTest']
  cases=json.loads((root/'tools/migracion/tests_criticos.json').read_text('utf-8'))
  for name in cases:task+=['--tests','com.tuempresa.inventariovial.'+name]
  repo=Path.home()/'.m2/repository/org/robolectric/android-all-instrumented'
  versions=['9-robolectric-4913185-2-i7','14-robolectric-10818077-i7']
  jars=[repo/v/('android-all-instrumented-'+v+'.jar')for v in versions]
  if all(p.is_file()for p in jars):
   flat=root/'tmp/robolectric-runtime';flat.mkdir(parents=True,exist_ok=True)
   for jar in jars:
    target=flat/jar.name
    if not target.exists():shutil.copy2(jar,target)
    assert hashlib.sha256(target.read_bytes()).digest()==hashlib.sha256(jar.read_bytes()).digest(),'Robolectric cache copy mismatch'
   options+=['-Dgbo.migration.roboCache='+str(flat)]
  elif not args.allow_downloads:raise RuntimeError('Robolectric SDK28/34 cache missing. Warm selected tests online first, outside the measured offline run.')
 elif args.phase=='instrumentation-compile':task=[':app:compileDebugAndroidTestKotlin']
 else:task=[':app:validateReleaseConfiguration']
 assert not any('gboValidationBuild' in x for x in options+task)
 started=time.monotonic();metrics={};stop=threading.Event();code=-1
 try:
  p=subprocess.Popen(['cmd.exe','/d','/c',str(root/'gradlew.bat'),*options,*task],cwd=root,env=env,stdout=subprocess.PIPE,stderr=subprocess.STDOUT,text=True,encoding='utf-8',errors='replace')
  t=threading.Thread(target=measure,args=(p.pid,stop,metrics),daemon=True);t.start()
  with(report/(args.phase+'.log')).open('w',encoding='utf-8')as log:
   for line in p.stdout:
    safe=redact(line);log.write(safe);log.flush()
    if line.startswith('> Task')or any(x in line for x in ['BUILD SUCCESSFUL','BUILD FAILED','FAILURE:','FAILED','GRADLE_RESULT']):print(safe,end='',flush=True)
  code=p.wait()
 finally:
  stop.set()
  if 't'in locals():t.join(3)
  if changed is not None:changed.write_bytes(original);assert hashlib.sha256(changed.read_bytes()).hexdigest()==before
 result={'phase':args.phase,'exitCode':code,'seconds':round(time.monotonic()-started,2),'gradleOffline':not args.allow_downloads,'downloadsAllowed':args.allow_downloads,'dependencyCache':'existing global cache; project build outputs empty only for cold','androidStudioIndexingIncluded':False,'profile':'Windows 8GB conservative, 1 worker / 2 visible processors / 2048MiB Gradle heap; repeat identical profile on ASUS','commandOptions':options,'tasks':task,'sourceCommentRestored':changed is not None,'validationBuild':False,**metrics}
 (report/(args.phase+'.json')).write_text(json.dumps(result,indent=2),'utf-8');print('BENCHMARK_RESULT',args.phase,code,result['seconds'],flush=True)
 return code
if __name__=='__main__':raise SystemExit(main())
