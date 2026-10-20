"""Compile only isolated Kotlin/JVM test classes against cached 1.4.9 artifacts.
No Gradle tasks, APK packaging, signing, installation, network or backend calls.
Requires the local cached dependencies; fails rather than building an APK.
"""
from pathlib import Path
import subprocess, sys, os, json
sys.stdout.reconfigure(errors='replace')

ROOT=Path(__file__).resolve().parents[1]
WORK=ROOT/'tmp/pavimentos-final-20261019'
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
    if mode=='extras':cp.insert(0,str(WORK/'classes-after'))
    missing=[x for x in cp if not Path(x).exists()]
    assert not missing, 'Cached 1.4.9 artifacts missing; no APK build attempted'
    compiler=[jar('org.jetbrains.kotlin',a,v) for a,v in [('kotlin-compiler-embeddable','2.2.10'),('kotlin-stdlib','2.2.10'),('kotlin-script-runtime','2.2.10'),('kotlin-reflect','1.6.10'),('kotlin-daemon-embeddable','2.2.10')]]
    compiler += [jar('org.jetbrains.kotlinx','kotlinx-coroutines-core-jvm','1.8.0'),jar('org.jetbrains','annotations','13.0')]
    compose=jar('org.jetbrains.kotlin','kotlin-compose-compiler-plugin-embeddable','2.2.10')
    android=Path('D:/Android/Sdk/platforms/android-36/android.jar')
    if not android.exists():android=sorted(Path('D:/Android/Sdk/platforms').glob('android-*/android.jar'))[-1]
    classes=WORK/('classes-'+mode);classes.mkdir(exist_ok=True)
    sources=[]
    if mode in ['after','legacy','render']:
        base=ROOT/'app/src/main/java/com/tuempresa/inventariovial/pavement'
        sources=[base/n for n in ['PavementSketch.kt','PavementRigidVisualGeometry.kt','PavementExports.kt','PavementRepository.kt','ui/PavementScheme.kt','ui/PavementWorkspace.kt','ui/PavementDialogs.kt','PavementPatchVisuals.kt','PavementDamageFields.kt']]
        main=ROOT/'app/src/main/java/com/tuempresa/inventariovial'
        sources += [main/n for n in ['localfiles/LocalCopies.kt','localfiles/LocalFilesUi.kt','localfiles/LocalCopyPresentation.kt','core/SyncProgress.kt']]
    if mode=='before':
        base=WORK/'respaldo/app/src/main/java/com/tuempresa/inventariovial/pavement'
        sources=[base/n for n in ['PavementSketch.kt','PavementExports.kt','ui/PavementScheme.kt']]
    tests=ROOT/'app/src/test/java/com/tuempresa/inventariovial'
    sources += [tests/'PavementVisualExamplesTest.kt']
    sources += [tests/n for n in ['PavementFinalGeometryTest.kt','PavementFinalExamplesTest.kt','LocalCopyFinalTest.kt','LocalFiles148Test.kt','PavementJointFlow20261019Test.kt','PavementJointPreviewUiTest.kt','UiWorkerFixture.kt','PavementRigid147Test.kt','PavementParallelCTest.kt','PavementObservations10ITest.kt','PavementReportTest.kt','PavementLegacyPdfFocusedTest.kt']]
    if mode=='extras':sources=[ROOT/'app/src/main/java/com/tuempresa/inventariovial/localfiles/LocalFilesUi.kt',tests/'PavementFinalUiTest.kt',tests/'PavementFinalIntegrityTest.kt',tests/'PavementVisualCorrectionsTest.kt']
    appjar=next(x for x in cp if 'runtime_app_classes_jar' in x)
    args=['-no-stdlib','-no-reflect','-jvm-target','17','-classpath',';'.join([str(android),*cp]),'-Xfriend-paths='+','.join([str(WORK/'classes-after'),appjar,*[x for x in cp if 'built_in_kotlinc' in x and 'debugUnitTest' in x]]),'-Xplugin='+str(compose),'-d',classes,*sources]
    argfile=WORK/('compile-'+mode+'.args');argfile.write_text('\n'.join(quote(a) for a in args),'utf-8')
    run(['-Xmx3g','-cp',';'.join(map(str,compiler)),'org.jetbrains.kotlin.cli.jvm.K2JVMCompiler','@'+str(argfile)],'compile-'+mode)
    testargs=['-Xmx3g','-Djava.io.tmpdir=D:/gbo-pav-final','-Drobolectric.offline=true','-Drobolectric.dependency.dir='+str(Path.home()/'.m2/repository/org/robolectric/android-all-instrumented/14-robolectric-10818077-i7'),'-Dgbo.visual.phase='+('after' if mode=='render' else mode),'-Dgbo.visual.output='+str(ROOT/'output/pdf/pavimentos-final-20261019'),'-cp',';'.join([str(classes),*cp]),'org.junit.runner.JUnitCore','com.tuempresa.inventariovial.PavementVisualExamplesTest']
    testargs=testargs[:testargs.index('org.junit.runner.JUnitCore')+1]+['com.tuempresa.inventariovial.'+n for n in ['PavementFinalGeometryTest','LocalCopyFinalTest','LocalFiles148Test','PavementJointFlow20261019Test','PavementJointPreviewUiTest','PavementLegacyPdfFocusedTest','PavementFinalExamplesTest']]
    if mode=='extras':testargs=testargs[:testargs.index('org.junit.runner.JUnitCore')+1]+['com.tuempresa.inventariovial.'+n for n in ['PavementFinalUiTest','PavementFinalIntegrityTest','PavementVisualCorrectionsTest']]
    argfile=WORK/('test-'+mode+'.args');argfile.write_text('\n'.join(quote(a) for a in testargs),'utf-8')
    run(['@'+str(argfile)],'test-'+mode,cwd=ROOT/'app')
if __name__=='__main__':main()
