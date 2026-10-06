from pathlib import Path
import hashlib
import json
import zipfile

root = Path(__file__).resolve().parents[2]
out = Path(__file__).resolve().parent
checks = (out / 'runtime/checks.txt').read_text(encoding='utf-8')
assert 'RESULT PASS failures=0' in checks and '\nFAIL ' not in checks
for name in ('client.log', 'build.log'):
    raw = (out / name).read_bytes()
    log = raw.decode('utf-16' if raw.startswith((b'\xff\xfe', b'\xfe\xff')) else 'utf-8-sig')
    assert 'BUILD SUCCESSFUL' in log, name
jar = root / 'build/libs/domesurvival-0.2.0.jar'
with zipfile.ZipFile(jar) as archive:
    assert 'com/wasted/domesurvival/forge/client/gui/MachineGaugeRenderer.class' in archive.namelist()
    assert 'com/wasted/domesurvival/forge/integration/customnpcs/JosephScriptCommand.class' in archive.namelist()
    assert not any('probe/' in entry.lower() for entry in archive.namelist())
assert not list((root / 'run/machine-gauge-review/production_mod_hold').glob('*.jar'))
assert len(list((root / 'run/mods').glob('*.jar'))) == 85
result = dict(result='PASS', render_checks=sum(line.startswith('PASS ') for line in checks.splitlines()),
              jar=str(jar), sha256=hashlib.sha256(jar.read_bytes()).hexdigest())
(out / 'release_validation.json').write_text(json.dumps(result, indent=2) + '\n')
print(json.dumps(result, indent=2))
