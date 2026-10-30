"""UIAutomator/ADB helper for TEST_AUTO_10G; never writes app databases or installs APKs.

All snapshots stay under ignored app/build/campo-10g. Secret/login fields are not
printed or filled. Finalization/retry requires a separately recorded human approval.
"""
import argparse, json, os, pathlib, re, shlex, subprocess, sys, time
import xml.etree.ElementTree as ET

ROOT=pathlib.Path(__file__).resolve().parents[1]
OUT=ROOT/'app/build/campo-10g'
from gbo_paths import android_sdk
ADB=str(android_sdk()/'platform-tools/adb.exe')
SERIAL=os.environ.get('GBO_ADB_SERIAL','')
PACKAGE='com.tuempresa.inventariovial'
OUT.mkdir(parents=True,exist_ok=True)
ENV=dict(os.environ,ANDROID_USER_HOME=str(pathlib.Path.home()/'.android'))

def adb(*args,binary=False):
    if not SERIAL:raise RuntimeError("Configura GBO_ADB_SERIAL para la tablet autorizada; no se ejecutó ADB.")
    p=subprocess.run([ADB,'-s',SERIAL,*args],env=ENV,capture_output=True,timeout=35)
    if p.returncode: raise RuntimeError('ADB command failed; no credential output retained')
    return p.stdout if binary else p.stdout.decode('utf-8',errors='replace')

def dump():
    adb('shell','uiautomator','dump','/sdcard/Download/TEST_AUTO_10G_ui.xml')
    adb('pull','/sdcard/Download/TEST_AUTO_10G_ui.xml',str(OUT/'ui-current.xml'))
    return ET.parse(OUT/'ui-current.xml').getroot()

def bounds(node):
    x1,y1,x2,y2=map(int,re.findall(r'\d+',node.get('bounds','')))
    return ((x1+x2)//2,(y1+y2)//2)

def public_nodes(root):
    return [dict(text=n.get('text',''),description=n.get('content-desc',''),bounds=n.get('bounds',''),
        cls=n.get('class',''),clickable=n.get('clickable','')) for n in root.iter('node')
        if n.get('password')!='true' and (n.get('text') or n.get('content-desc') or 'EditText' in n.get('class',''))]

def node_for(root,label):
    nodes=[n for n in root.iter('node') if n.get('text')==label or n.get('content-desc')==label]
    if len(nodes)!=1: raise RuntimeError('Target must be uniquely visible: '+label)
    return nodes[0]

def tap(label):
    root=dump()
    if re.search(r'finaliz|cerr|env[ií]o|sincron|reintent',label,re.I):
        approval=json.loads((OUT/'production-approval.json').read_text()) if (OUT/'production-approval.json').exists() else {}
        if approval.get('approved') is not True or approval.get('fixture')!='TEST_AUTO_10G':
            raise RuntimeError('Production action blocked: explicit human approval for TEST_AUTO_10G required')
        if not any('TEST_AUTO_10G' in n.get('text','') for n in root.iter('node')):
            raise RuntimeError('Production action blocked: test identity not visible')
    target=node_for(root,label)
    if target.get('enabled')=='false': raise RuntimeError('Target disabled by application validation')
    x,y=bounds(target);adb('shell','input','tap',str(x),str(y));time.sleep(.35)
    print(json.dumps(public_nodes(dump())))

def fill(label,value):
    if not re.fullmatch(r'[A-Za-z0-9_ .,/:+-]*',value): raise RuntimeError('Only ASCII fixture text allowed')
    root=dump();target=node_for(root,label)
    parents={c:p for p in root.iter() for c in p}
    while 'EditText' not in target.get('class','') and target in parents:
        parent=parents[target]
        candidates=[n for n in parent.iter('node') if 'EditText' in n.get('class','')]
        if len(candidates)==1: target=candidates[0];break
        target=parent
    if 'EditText' not in target.get('class','') or target.get('password')=='true':
        raise RuntimeError('No unique non-secret text field for '+label)
    x,y=bounds(target);adb('shell','input','tap',str(x),str(y))
    adb('shell','input','keycombination','113','29')
    adb('shell','input','text',shlex.quote(value.replace(' ','%s')))
    time.sleep(.35)
    print(json.dumps(public_nodes(dump())))

def main():
    parser=argparse.ArgumentParser();parser.add_argument('action',choices=['inspect','tap','fill','swipe','back','screenshot'])
    parser.add_argument('arguments',nargs='*');args=parser.parse_args()
    if adb('get-state').strip()!='device': raise RuntimeError('Authorized real device unavailable')
    if args.action=='inspect': print(json.dumps(public_nodes(dump())))
    elif args.action=='tap': tap(args.arguments[0])
    elif args.action=='fill': fill(args.arguments[0],args.arguments[1])
    elif args.action=='swipe':
        if len(args.arguments)!=4 or not all(v.isdigit() for v in args.arguments): raise RuntimeError('Four numeric screen coordinates required')
        adb('shell','input','swipe',*args.arguments,'350');time.sleep(.3);print(json.dumps(public_nodes(dump())))
    elif args.action=='back': adb('shell','input','keyevent','4');print(json.dumps(public_nodes(dump())))
    else: (OUT/'screen-current.png').write_bytes(adb('exec-out','screencap','-p',binary=True));print(str(OUT/'screen-current.png'))

if __name__=='__main__':
    try: main()
    except Exception as error: print('STOP: '+str(error));sys.exit(1)
