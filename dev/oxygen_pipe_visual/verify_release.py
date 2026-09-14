from pathlib import Path
import hashlib,json,zipfile
ROOT=Path(__file__).resolve().parents[2];DEV=ROOT/'dev/oxygen_pipe_visual'
static=json.loads((DEV/'asset_validation.json').read_text());jar=ROOT/'build/libs/domesurvival-0.2.0.jar';checks=[]
def check(ok,name):checks.append({'pass':bool(ok),'check':name})
with zipfile.ZipFile(jar) as z:
    for rel in static['changed']+static['new']:
        rel=rel.replace('\\','/')
        if '/resources/' in rel:check(z.read(rel.split('/resources/')[1])==(ROOT/rel).read_bytes(),rel+' matches JAR')
    names=z.namelist()
    check(not any('/oxygenpipeprobe/' in n or '/itempipeprobe/' in n or '/materialprobe/' in n or '/fluidprobe/' in n for n in names),'No development probes in release')
    for name in ('OxygenFlowNetwork','OxygenGasFlowRenderer'):
        check(any(n.endswith('/'+name+'.class') for n in names),name+' present in release')
log=(DEV/'build.log').read_bytes();log=log.decode('utf-16' if log.startswith((b'\xff\xfe',b'\xfe\xff')) else 'utf-8',errors='replace')
check('BUILD SUCCESSFUL' in log,'Production build successful')
runtime=(DEV/'runtime/checks.txt').read_text()
check('RESULT PASS failures=0' in runtime and not any(l.startswith('FAIL ') for l in runtime.splitlines()),'Final runtime review passed')
check(static['pass'],'Geometry, alpha masks, references and scope validated')
check(len(list((ROOT/'run/mods').glob('*.jar')))==85 and not list((ROOT/'run/oxygen-pipe-review/production_mod_hold').glob('*.jar')),'85 production mod JARs restored')
result={'pass':all(c['pass'] for c in checks),'jar':str(jar),'sha256':hashlib.sha256(jar.read_bytes()).hexdigest(),'bytes':jar.stat().st_size,
        'static_checks':static['checks'],'runtime_checks':sum(l.startswith('PASS ') for l in runtime.splitlines()),'checks':checks}
(DEV/'release_validation.json').write_text(json.dumps(result,indent=2)+'\n')
print(json.dumps({k:v for k,v in result.items() if k!='checks'},indent=2));assert result['pass'],[c for c in checks if not c['pass']]
