from pathlib import Path
import json, zipfile, hashlib, re, sys
root=Path(__file__).resolve().parents[2];out=Path(__file__).parent
resources=root/'src/main/resources';assets=resources/'assets/domesurvival'
manifest=json.loads((out/'manifest.json').read_text())
species=manifest['species']
data=json.loads((resources/'data/domesurvival/bio_module_loot/default.json').read_text())
assert set('minecraft:'+s for s in species)==set(s['entity'] for s in data['species'])
java=(root/'src/main/java/com/wasted/domesurvival/forge/client/BioModuleModels.java').read_text()
assert re.findall(r'"([a-z_]+)"',java.split('List.of(')[1].split(');')[0])==species
models=list((assets/'models/item/bio_capsules').glob('*.json'))
assert len(models)==53
overrides=json.loads((assets/'models/item/bio_module.json').read_text())['overrides']
assert len(overrides)==52
assert [o['predicate']['domesurvival:bio_variant'] for o in overrides]==[i/64 for i in range(1,53)]
atlas=json.loads((resources/'assets/minecraft/atlases/blocks.json').read_text())
portrait_sprites={s['resource'] for s in atlas['sources'] if s['type']=='minecraft:single'}
for p in models:
    m=json.loads(p.read_text())
    for e in m['elements']:
        assert all(0<=a<=b<=16 for a,b in zip(e['from'],e['to']))
        assert any(a<b for a,b in zip(e['from'],e['to']))
        for face in e['faces'].values():
            assert face['texture'][1:] in m['textures']
            assert all(0<=u<=16 for u in face['uv'])
    for t in m['textures'].values():
        ns,name=t.split(':')
        if ns=='domesurvival':assert (assets/f'textures/{name}.png').exists(),t
        if name.startswith('gui/bio/'):assert t in portrait_sprites,t
    assert m['gui_light']=='front'
if '--resources-only' in sys.argv:
    print('PASS: 53 models, 52 predicates, all 26 configured species, texture paths and UV bounds')
    sys.exit(0)
checks=(out/'runtime/checks.txt').read_text()
assert 'RESULT PASS failures=0' in checks and '\nFAIL ' not in checks
for name in ('client.log','build.log'):
    b=(out/name).read_bytes();s=b.decode('utf-16' if b.startswith((b'\xff\xfe',b'\xfe\xff')) else 'utf-8')
    assert 'BUILD SUCCESSFUL' in s,name
files=models+[assets/'models/item/bio_module.json']+[assets/f'models/item/{n}.json' for n in manifest['legacy']]
files.append(resources/'assets/minecraft/atlases/blocks.json')
jar=root/'build/libs/domesurvival-0.2.0.jar'
with zipfile.ZipFile(jar) as z:
    assert not any('capsuleprobe' in n for n in z.namelist())
    assert 'com/wasted/domesurvival/forge/client/BioModuleModels.class' in z.namelist()
    for p in files:assert z.read(p.relative_to(resources).as_posix())==p.read_bytes(),p
mods=json.loads((out/'runtime_mods.json').read_text(encoding='utf-8-sig'))
assert set(p.name for p in (root/'run/mods').glob('*.jar'))==set(m['name'] for m in mods)
for m in mods:assert hashlib.sha256((root/'run/mods'/m['name']).read_bytes()).hexdigest().upper()==m['sha256']
assert not list((root/'run/bio-capsules-review/production_mod_hold').glob('*.jar'))
report=dict(result='PASS',species=len(species),variants=52,legacy_aliases=4,
            runtime_assertions=sum(s.startswith('PASS ') for s in checks.splitlines()),
            packaged_models=len(files),sha256=hashlib.sha256(jar.read_bytes()).hexdigest())
(out/'release_validation.json').write_text(json.dumps(report,indent=2)+'\n')
print(json.dumps(report,indent=2))
