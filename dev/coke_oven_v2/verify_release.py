"""Check isolated runtime evidence, unchanged oven model and final packaged resources."""
from pathlib import Path
import hashlib, json, struct, zipfile

root = Path(__file__).resolve().parents[2]
out = Path(__file__).resolve().parent
resources = root / 'src/main/resources'
assets = resources / 'assets/domesurvival'
baseline = json.loads((out / 'model_baseline.json').read_text())
for name, digest in baseline.items():
    assert hashlib.sha256((root / name).read_bytes()).hexdigest() == digest, name

checks = (out / 'runtime/checks.txt').read_text()
assert 'RESULT PASS failures=0' in checks and '\nFAIL ' not in checks
for name in ['client.log', 'build.log']:
    raw = (out / name).read_bytes()
    log = raw.decode('utf-16' if raw[:2] in (b'\xff\xfe', b'\xfe\xff') else 'utf-8')
    assert 'BUILD SUCCESSFUL' in log, name

files = [root / name for name in baseline]
files += list((assets / 'textures/gui/coke_oven_v2').iterdir())
files += [assets / 'lang/ru_ru.json', assets / 'lang/en_us.json',
          resources / 'data/domesurvival/loot_tables/blocks/coke_oven.json',
          resources / 'data/minecraft/tags/blocks/mineable/pickaxe.json',
          resources / 'data/minecraft/tags/blocks/needs_stone_tool.json']
for name, size in [('panel',(880,1064)), ('modules',(816,444)),
                   ('configuration',(816,444)), ('jei',(720,512))]:
    assert struct.unpack('>II',(assets / f'textures/gui/coke_oven_v2/{name}.png').read_bytes()[16:24]) == size

jar = root / 'build/libs/domesurvival-0.2.0.jar'
with zipfile.ZipFile(jar) as archive:
    names = archive.namelist()
    assert not any('probe/' in name.lower() for name in names)
    for path in files:
        assert archive.read(path.relative_to(resources).as_posix()) == path.read_bytes(), path
    for cls in ['machine/shaft/CokeOvenBlockEntity', 'machine/shaft/CokeOvenMenu',
                'client/screen/CokeOvenScreen', 'client/jei/CokeOvenJeiCategory',
                'item/EngineerWrenchItem', 'integration/customnpcs/JosephScriptCommand']:
        assert f'com/wasted/domesurvival/forge/{cls}.class' in names, cls
    for cls, code in [('client/screen/CokeOvenScreen',b'CO-01'),
                      ('client/screen/CoalGeneratorScreen',b'CG-01'),
                      ('machine/forming/FormingPressScreen',b'MP-01'),
                      ('machine/copper/CopperFurnaceScreen',b'CF-01')]:
        assert code not in archive.read(f'com/wasted/domesurvival/forge/{cls}.class'), cls

assert len(list((root / 'run/mods').glob('*.jar'))) == 85
assert not list((root / 'run/coke-oven-review/production_mod_hold').glob('*.jar'))
report = {'result':'PASS', 'runtime_assertions':sum(s.startswith('PASS ') for s in checks.splitlines()),
          'unchanged_model_assets':len(baseline), 'packaged_resources_checked':len(files),
          'jar':str(jar), 'sha256':hashlib.sha256(jar.read_bytes()).hexdigest(),
          'bytes':jar.stat().st_size,
          'screenshots':[p.name for p in sorted((out / 'runtime').glob('*.png'))]}
(out / 'release_validation.json').write_text(json.dumps(report,indent=2)+'\n')
print(json.dumps(report,indent=2))
