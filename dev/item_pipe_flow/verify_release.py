from pathlib import Path
import hashlib,json,zipfile

root=Path(__file__).resolve().parents[2]
out=Path(__file__).resolve().parent
jar=root/'build/libs/domesurvival-0.2.0.jar'
checks=(out/'runtime/checks.txt').read_text(encoding='utf-8')
assert 'RESULT PASS failures=0' in checks and '\nFAIL ' not in checks
for required in ['No orphaned in-flight stacks at completion',
                 'Filtering preserves routing and inherits steel travel speed',
                 'Broken route refunds packet without duplication',
                 'Waiting packet delivered exactly once after receiver frees',
                 'Client collar west blue restored',
                 'Smooth sub-tick position follows physical distance']:
    assert 'PASS '+required in checks,required
def log(path):
    raw=path.read_bytes()
    return raw.decode('utf-16' if raw.startswith((b'\xff\xfe',b'\xfe\xff')) else 'utf-8')
assert 'BUILD SUCCESSFUL' in log(out/'build.log')
assert 'BUILD SUCCESSFUL' in log(out/'client.log')
resources=['data/domesurvival/item_pipe_balance/default.json',
           'assets/domesurvival/lang/en_us.json','assets/domesurvival/lang/ru_ru.json']
with zipfile.ZipFile(jar) as z:
    assert not any('itempipeprobe/' in n.lower() or 'coalgeneratorprobe' in n.lower() for n in z.namelist())
    assert 'com/wasted/domesurvival/forge/itempipe/ItemPipeTransitData.class' in z.namelist()
    for name in resources:
        assert z.read(name)==(root/'src/main/resources'/name).read_bytes(),name
    for file in (root/'src/main/resources/assets/domesurvival/textures/gui/coal_generator_v2').iterdir():
        assert z.read('assets/domesurvival/textures/gui/coal_generator_v2/'+file.name)==file.read_bytes()
    assert z.read('assets/domesurvival/models/item/buffer_module.json')==(root/'src/main/resources/assets/domesurvival/models/item/buffer_module.json').read_bytes()
restored=len(list((root/'run/mods').glob('*.jar')))
assert restored==85,restored
assert not list((root/'run/item-pipe-flow-review/production_mod_hold').glob('*.jar'))
report={'result':'PASS','checks':sum(s.startswith('PASS ') for s in checks.splitlines()),
        'jar':str(jar),'bytes':jar.stat().st_size,'sha256':hashlib.sha256(jar.read_bytes()).hexdigest(),
        'restored_mod_jars':restored,'test_classes_in_release':False,
        'arrival_checks':[s for s in checks.splitlines() if 'Physical arrival' in s]}
(out/'release_validation.json').write_text(json.dumps(report,indent=2)+'\n',encoding='utf-8')
print(json.dumps(report,indent=2))
