"""Validate the tested bioincubator, DNA clearance and packaged resources."""
from pathlib import Path
import hashlib, json, zipfile, math, struct

root = Path(__file__).resolve().parents[2]
out = Path(__file__).resolve().parent
resources = root / 'src/main/resources'
assets = resources / 'assets/domesurvival'
checks = (out / 'runtime/checks.txt').read_text(encoding='utf-8')
assert 'RESULT PASS failures=0' in checks and '\nFAIL ' not in checks
sieve = (root / 'dev/sieve_jei/runtime/checks.txt').read_text(encoding='utf-8')
assert 'RESULT PASS failures=0' in sieve and '\nFAIL ' not in sieve
for name in ('client.log', 'build.log'):
    raw = (out / name).read_bytes()
    log = raw.decode('utf-16' if raw.startswith((b'\xff\xfe', b'\xfe\xff')) else 'utf-8')
    assert 'BUILD SUCCESSFUL' in log, name

models = [json.loads((assets / f'models/block/{name}.json').read_text())
          for name in ('bioincubator', 'bioincubator_dna', 'bioincubator_inventory')]
body, dna, inventory = models
assert len(inventory['elements']) == len(body['elements']) + len(dna['elements'])
for model in models:
    for cube in model['elements']:
        for lo, hi in zip(cube['from'], cube['to']):
            assert 0 <= lo < hi <= 16
        for face in cube['faces'].values():
            assert all(0 <= v <= 16 for v in face['uv'])
    for texture in model['textures'].values():
        if not texture.startswith('#'):
            namespace, path = texture.split(':')
            assert (resources / f'assets/{namespace}/textures/{path}.png').exists()
# The entire revolving helix stays behind the front window and between the chamber walls.
for cube in dna['elements']:
    assert 3.85 < cube['from'][1] < cube['to'][1] < 12.2
    for x in (cube['from'][0], cube['to'][0]):
        for z in (cube['from'][2], cube['to'][2]):
            for degrees in range(360):
                a = math.radians(degrees)
                rx = 8 + (x - 8) * math.cos(a) - (z - 4.8) * math.sin(a)
                rz = 4.8 + (x - 8) * math.sin(a) + (z - 4.8) * math.cos(a)
                assert 3.15 < rx < 12.85 and 1.75 < rz < 8.1, (degrees, rx, rz)
for name, dim in [('panel', (880,1064)), ('repair', (880,1064)),
                  ('configuration', (816,444)), ('modules', (816,444)),
                  ('jei', (720,512)), ('jei_repair', (720,512))]:
    assert struct.unpack('>II', (assets / f'textures/gui/bioincubator_v2/{name}.png').read_bytes()[16:24]) == dim
for locale in ('ru_ru', 'en_us'):
    data = json.loads((assets / f'lang/{locale}.json').read_text(encoding='utf-8'))
    assert not any('?' in v for k, v in data.items() if k.startswith('gui.domesurvival.bioincubator_v2.'))
state = json.loads((assets / 'blockstates/bioincubator.json').read_text(encoding='utf-8-sig'))
for part in state['multipart']:
    apply = part['apply']
    for entry in (apply if isinstance(apply, list) else [apply]):
        namespace, path = entry['model'].split(':')
        assert (resources / f'assets/{namespace}/models/{path}.json').exists(), path
        assert 'bioincubator_input_port' not in path and 'bioincubator_output_port' not in path

files = list((assets / 'textures/gui/bioincubator_v2').iterdir())
files += list((assets / 'models/block').glob('bioincubator*.json'))
files += list((assets / 'models/block').glob('coal_generator_*_port_*.json'))
files += [assets / 'models/item/bioincubator.json', assets / 'blockstates/bioincubator.json',
          assets / 'textures/block/coal_generator_v2/satin_atlas.png',
          assets / 'lang/ru_ru.json', assets / 'lang/en_us.json',
          resources / 'data/domesurvival/bio_module_loot/default.json',
          resources / 'data/minecraft/tags/blocks/needs_stone_tool.json']
jar = root / 'build/libs/domesurvival-0.2.0.jar'
with zipfile.ZipFile(jar) as z:
    names = z.namelist()
    assert not any('probe/' in n.lower() for n in names)
    assert 'assets/domesurvival/models/block/bioincubator_scanner.json' not in names
    for p in files:
        assert z.read(p.relative_to(resources).as_posix()) == p.read_bytes(), p
    for name in ('machine/bio/BioincubatorBlockEntity', 'machine/bio/BioincubatorMenu',
                 'machine/bio/BioincubatorScreen', 'client/render/BioincubatorRenderer',
                 'client/render/BioincubatorPreview', 'client/jei/BioincubatorJeiCategory',
                 'integration/customnpcs/JosephScriptCommand'):
        assert f'com/wasted/domesurvival/forge/{name}.class' in names, name
assert len(list((root / 'run/mods').glob('*.jar'))) == 85
assert not list((root / 'run/bioincubator-review/production_mod_hold').glob('*.jar'))
report = dict(result='PASS', runtime_assertions=sum(x.startswith('PASS ') for x in checks.splitlines()),
              sieve_assertions=sum(x.startswith('PASS ') for x in sieve.splitlines()),
              packaged_resources=len(files), body_cuboids=len(body['elements']),
              moving_cuboids=len(dna['elements']), dna_clearance_angles=360,
              jar=str(jar), sha256=hashlib.sha256(jar.read_bytes()).hexdigest(), bytes=jar.stat().st_size)
(out / 'release_validation.json').write_text(json.dumps(report, indent=2) + '\n')
print(json.dumps(report, indent=2))
