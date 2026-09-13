from pathlib import Path
import hashlib,json,zipfile
ROOT=Path(__file__).resolve().parents[2];DEV=ROOT/'dev/pipe_materials_v2'
static=json.loads((DEV/'static_validation.json').read_text());jar=ROOT/'build/libs/domesurvival-0.2.0.jar'
checks=[]
def check(ok,msg):checks.append({'pass':bool(ok),'check':msg})
with zipfile.ZipFile(jar) as z:
    for rel in static['changed_files']+static['added_files']:
        check(z.read(rel.removeprefix('src/main/resources/'))==(ROOT/rel).read_bytes(),rel+' matches built JAR')
    check(not any('/materialprobe/' in n or 'PipeMaterialProbe' in n for n in z.namelist()),'Test-only material probe excluded')
    check(not any('/fluidprobe/' in n for n in z.namelist()),'Prior fluid probe excluded')
log=(DEV/'build.log').read_bytes();text=log.decode('utf-16' if log.startswith((b'\xff\xfe',b'\xfe\xff')) else 'utf-8',errors='replace')
check('BUILD SUCCESSFUL' in text,'Production build succeeded')
runtime=(DEV/'runtime/checks.txt').read_text()
check('RESULT PASS failures=0' in runtime and not any(l.startswith('FAIL ') for l in runtime.splitlines()),'Final runtime review passed without failed checks')
check(static['status']=='PASS','Frame, atlas and saved master validation passed')
result={'status':'PASS' if all(c['pass'] for c in checks) else 'FAIL','jar':str(jar),'sha256':hashlib.sha256(jar.read_bytes()).hexdigest(),'bytes':jar.stat().st_size,'resources':24,'static_checks':static['checks_count'],'runtime_checks':sum(l.startswith('PASS ') for l in runtime.splitlines()),'checks':checks}
(DEV/'release_validation.json').write_text(json.dumps(result,indent=2)+'\n')
print(json.dumps({k:v for k,v in result.items() if k!='checks'},indent=2));assert result['status']=='PASS'
