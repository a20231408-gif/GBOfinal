"""Compile current geometry/tests against local cached artifacts and the approved PDF renderer.
No Gradle tasks, APK packaging, signing, installation, network or backend calls.
Requires the local cached dependencies; fails rather than building an APK.
"""
from pathlib import Path
import subprocess, sys, os, json
sys.stdout.reconfigure(errors='replace')

ROOT=Path(__file__).resolve().parents[1]
WORK=ROOT/'tmp/sic20-juntas-20261019'
CACHE=Path.home()/'.gradle/caches/modules-2/files-2.1'
JAVA=Path('D:/Android Studio/jbr/bin/java.exe')
def jar(group,artifact,version):
    return next((CACHE/group/artifact/version).rglob(artifact+'-'+version+'.jar'))
def quote(x):return '"'+str(x).replace('\\','/').replace('"','\\"')+'"'
def run(args,name,cwd=ROOT):
    p=subprocess.run([str(JAVA),*args],cwd=cwd,stdout=subprocess.PIPE,stderr=subprocess.STDOUT)
    (WORK/(name+'.log')).write_bytes(p.stdout)
    print(name, 'PASS' if p.returncode==0 else 'FAIL',flush=True)
    if p.returncode:print(p.stdout.decode('utf-8',errors='replace')[-12000:]);raise SystemExit(p.returncode)
def main():
    mode=sys.argv[1] if len(sys.argv)>1 else 'after'
    cp=(ROOT/'tmp/pavimentos-20261019/worker-args-original.txt').read_text().splitlines()[1].strip('"').replace('\\\\','\\').split(';')
    cp=[str(ROOT/'tmp/d24-abanico-20261019/classes-after'),str(ROOT/'tmp/orientacion-final-20261019/classes-after'),str(ROOT/'tmp/pdf-legibilidad-20261019/classes-after'),str(ROOT/'tmp/pavimentos-final-20261019/classes-extras'),str(ROOT/'tmp/pavimentos-final-20261019/classes-after'),*cp]
    if mode in ('visual','sic-extra','scap-regression'):cp.insert(0,str(WORK/'classes-after'))
    missing=[x for x in cp if not Path(x).exists()]
    assert not missing, 'Cached artifacts missing; no APK build attempted'
    compiler=[jar('org.jetbrains.kotlin',a,v) for a,v in [('kotlin-compiler-embeddable','2.2.10'),('kotlin-stdlib','2.2.10'),('kotlin-script-runtime','2.2.10'),('kotlin-reflect','1.6.10'),('kotlin-daemon-embeddable','2.2.10')]]
    compiler += [jar('org.jetbrains.kotlinx','kotlinx-coroutines-core-jvm','1.8.0'),jar('org.jetbrains','annotations','13.0')]
    compose=jar('org.jetbrains.kotlin','kotlin-compose-compiler-plugin-embeddable','2.2.10')
    android=Path('D:/Android/Sdk/platforms/android-36/android.jar')
    if not android.exists():android=sorted(Path('D:/Android/Sdk/platforms').glob('android-*/android.jar'))[-1]
    classes=WORK/('classes-'+mode);classes.mkdir(exist_ok=True)
    base=ROOT/'app/src/main/java/com/tuempresa/inventariovial/pavement'
    tests=ROOT/'app/src/test/java/com/tuempresa/inventariovial'
    main=ROOT/'app/src/main/java/com/tuempresa/inventariovial'
    sources=[main/n for n in ['MainActivity.kt','model/form/Sic20Identity.kt','model/form/SicIdentity.kt','field/DraftDestination.kt','core/InventoryDraftStore.kt','data/dao/InventoryDao.kt','viewmodel/InventoryViewModel.kt','repository/InventoryRepository.kt']]
    sources += [tests/'Sic20Recovery20261019Test.kt']
    if mode=='sic-extra':sources=[main/'field/DraftDestination.kt']+[tests/n for n in ['Sic20Recovery20261019Test.kt','Sic20FormRecoveryUiTest.kt','Sic20Regression20261019Test.kt','HotfixIntegrity146Test.kt','FieldLifecycleTest.kt','TechnicalRules4Test.kt']]
    if mode=='sic-final':sources += [tests/n for n in ['Sic20FormRecoveryUiTest.kt','Sic20Regression20261019Test.kt','HotfixIntegrity146Test.kt','FieldLifecycleTest.kt','TechnicalRules4Test.kt']]
    if mode=='scap-regression':sources=[tests/'Sic20ScapRegression20261019Test.kt']
    appjar=next(x for x in cp if 'runtime_app_classes_jar' in x)
    args=['-no-stdlib','-no-reflect','-jvm-target','17','-classpath',';'.join([str(android),*cp]),'-Xfriend-paths='+','.join([str(WORK/'classes-after'),str(ROOT/'tmp/pdf-legibilidad-20261019/classes-after'),str(ROOT/'tmp/pavimentos-final-20261019/classes-after'),appjar,*[x for x in cp if 'built_in_kotlinc' in x and 'debugUnitTest' in x]]),'-Xplugin='+str(compose),'-d',classes,*sources]
    argfile=WORK/('compile-'+mode+'.args');argfile.write_text('\n'.join(quote(a) for a in args),'utf-8')
    run(['-Xmx3g','-cp',';'.join(map(str,compiler)),'org.jetbrains.kotlin.cli.jvm.K2JVMCompiler','@'+str(argfile)],'compile-'+mode)
    Path('D:/gbo-sic20-joint').mkdir(exist_ok=True)
    testargs=['-Xmx3g','-Djava.io.tmpdir=D:/gbo-sic20-joint','-Drobolectric.offline=true','-Drobolectric.dependency.dir='+str(Path.home()/'.m2/repository/org/robolectric/android-all-instrumented/14-robolectric-10818077-i7'),'-Dgbo.visual.phase='+('after' if mode=='render' else mode),'-Dgbo.visual.output='+str(ROOT/'output/pdf/sic20-juntas-20261019'),'-cp',';'.join([str(classes),*cp]),'org.junit.runner.JUnitCore','com.tuempresa.inventariovial.PavementVisualExamplesTest']
    testargs=testargs[:testargs.index('org.junit.runner.JUnitCore')+1]+['com.tuempresa.inventariovial.Sic20Recovery20261019Test']
    if mode in ('sic-extra','sic-final'):testargs += ['com.tuempresa.inventariovial.Sic20FormRecoveryUiTest','com.tuempresa.inventariovial.Sic20Regression20261019Test']
    if mode=='scap-regression':testargs=testargs[:testargs.index('org.junit.runner.JUnitCore')+1]+['com.tuempresa.inventariovial.Sic20ScapRegression20261019Test']
    argfile=WORK/('test-'+mode+'.args');argfile.write_text('\n'.join(quote(a) for a in testargs),'utf-8')
    run(['@'+str(argfile)],'test-'+mode,cwd=ROOT/'app')
if __name__=='__main__':main()
