"""Validate the built artifact against the isolated, networked client review."""
from pathlib import Path
import hashlib
import json
import zipfile

root = Path(__file__).resolve().parents[2]
out = Path(__file__).resolve().parent
jar = root / 'build/libs/domesurvival-0.2.0.jar'
checks = (out / 'runtime/checks.txt').read_text(encoding='utf-8')
assert 'RESULT PASS failures=0' in checks and '\nFAIL ' not in checks
required = [
    'Replaced machine restores contents, module and operation',
    'Shift wrench returns exactly one machine, no loose duplicate contents',
    'Protection break event prevents portable extraction',
    'Networked expanded buffer retains values above signed short range',
    'Client blue input collar visible',
    'Client orange output collar visible',
]
for name in required:
    assert 'PASS ' + name in checks, name

def read_log(path):
    raw = path.read_bytes()
    return raw.decode('utf-16' if raw.startswith((b'\xff\xfe', b'\xfe\xff')) else 'utf-8')

for name in ['build.log', 'client.log']:
    assert 'BUILD SUCCESSFUL' in read_log(out / name), name
resources = root / 'src/main/resources'
assets = resources / 'assets/domesurvival'
files = list((assets / 'textures/gui/forming_press_v2').iterdir())
files += list((assets / 'models/block').glob('forming_press*.json'))
files += [assets / 'models/item/forming_press.json', assets / 'lang/ru_ru.json', assets / 'lang/en_us.json']
with zipfile.ZipFile(jar) as archive:
    assert not any('probe/' in name.lower() for name in archive.namelist())
    for file in files:
        assert archive.read(file.relative_to(resources).as_posix()) == file.read_bytes(), file
    assert 'com/wasted/domesurvival/forge/client/render/FormingPressRenderer.class' in archive.namelist()
    wrench = archive.read('com/wasted/domesurvival/forge/item/EngineerWrenchItem.class')
    assert b'cofh/thermal' not in wrench and b'cofh/core/item' not in wrench
    assert b'net/minecraft/world/item/Item' in wrench
restored = len(list((root / 'run/mods').glob('*.jar')))
assert restored == 85, restored
assert not list((root / 'run/forming-press-review/production_mod_hold').glob('*.jar'))
report = {
    'result': 'PASS',
    'runtime_assertions': sum(line.startswith('PASS ') for line in checks.splitlines()),
    'jar': str(jar), 'bytes': jar.stat().st_size,
    'sha256': hashlib.sha256(jar.read_bytes()).hexdigest(),
    'resources_checked': len(files), 'restored_mod_jars': restored,
    'probe_classes_in_release': False,
    'native_wrench_thermal_superclass': False,
    'screenshots': [file.name for file in sorted((out / 'runtime').glob('*.png'))],
}
(out / 'release_validation.json').write_text(json.dumps(report, indent=2) + '\n', encoding='utf-8')
print(json.dumps(report, indent=2))
