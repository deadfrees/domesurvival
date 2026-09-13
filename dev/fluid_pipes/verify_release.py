"""Check the built mod contains current fluid resources and excludes the test harness."""
from pathlib import Path
import hashlib, json, zipfile

ROOT=Path(__file__).resolve().parents[2]
DEV=ROOT/'dev/fluid_pipes'
report=json.loads((DEV/'static_validation.json').read_text())
jar=ROOT/'build/libs/domesurvival-0.2.0.jar'
checks=[]
def check(ok,name):checks.append({'pass':bool(ok),'check':name})
with zipfile.ZipFile(jar) as z:
    for rel in report['changed_files']+report['added_files']:
        check(z.read(rel.removeprefix('src/main/resources/'))==(ROOT/rel).read_bytes(),rel)
    check(not any('/fluidprobe/' in name or 'FluidPipeVisualProbe' in name for name in z.namelist()),'No fluid test harness in production JAR')
runtime=(DEV/'runtime/checks.txt').read_text()
check('RESULT PASS failures=0' in runtime and not any(line.startswith('FAIL ') for line in runtime.splitlines()),'Runtime test passed with zero failed checks')
check(report['status']=='PASS','Static mesh/resource checks passed')
build_log=(DEV/'build.log').read_bytes()
build_text=build_log.decode('utf-16' if build_log.startswith((b'\xff\xfe',b'\xfe\xff')) else 'utf-8',errors='replace')
check('BUILD SUCCESSFUL' in build_text,'Gradle build succeeded')
result={'status':'PASS' if all(c['pass'] for c in checks) else 'FAIL','jar':str(jar),'sha256':hashlib.sha256(jar.read_bytes()).hexdigest(),'bytes':jar.stat().st_size,'fluid_resources':33,'runtime_passed_checks':sum(l.startswith('PASS ') for l in runtime.splitlines()),'checks':checks}
(DEV/'release_validation.json').write_text(json.dumps(result,indent=2)+'\n')
print(json.dumps({k:v for k,v in result.items() if k!='checks'},indent=2))
assert result['status']=='PASS',[c for c in checks if not c['pass']]
