from pathlib import Path
import json,hashlib,zipfile,struct
root=Path(__file__).resolve().parents[2];out=Path(__file__).parent
resources=root/'src/main/resources';assets=resources/'assets/domesurvival'
checks=(out/'runtime/checks.txt').read_text()
assert 'RESULT PASS failures=0' in checks and '\nFAIL ' not in checks
for name in ('build.log','client.log'):
    b=(out/name).read_bytes();s=b.decode('utf-16' if b.startswith((b'\xff\xfe',b'\xfe\xff')) else 'utf-8')
    assert 'BUILD SUCCESSFUL' in s,name
models=[]
for level in range(5):
    p=assets/f'models/block/steel_buffer_v2/level_{level}.json';m=json.loads(p.read_text());models.append(p)
    windows=[e for e in m['elements'] if e['name'].startswith('Charge window ')]
    assert len(windows)==4
    assert sum('forge_data' in e for e in windows)==level
    for e in m['elements']:
        assert all(0<=lo<hi<=16 for lo,hi in zip(e['from'],e['to'])),e['name']
        for f in e['faces'].values():assert all(0<=v<=16 for v in f['uv'])
    for value in m['textures'].values():
        namespace,path=value.split(':');assert (resources/f'assets/{namespace}/textures/{path}.png').exists()
for name,size in [('panel',(880,1064)),('configuration',(816,444))]:
    assert struct.unpack('>II',(assets/f'textures/gui/steel_buffer_v2/{name}.png').read_bytes()[16:24])==size
state=json.loads((assets/'blockstates/energy_buffer.json').read_text())
assert len(state['multipart'])==32
for part in state['multipart']:
    namespace,path=part['apply']['model'].split(':');assert (resources/f'assets/{namespace}/models/{path}.json').exists()
baseline=json.loads((out/'baseline.json').read_text())
unchanged=[p for p in baseline if '/recipes/' in p or '/models/block/' in p]
for p in unchanged:assert hashlib.sha256((root/p).read_bytes()).hexdigest()==baseline[p],p
for locale in ('ru_ru','en_us'):
    data=json.loads((assets/f'lang/{locale}.json').read_text(encoding='utf-8'))
    assert not any('?' in v for k,v in data.items() if k.startswith('gui.domesurvival.steel_buffer_v2.'))
files=models+[assets/'models/item/energy_buffer.json',assets/'blockstates/energy_buffer.json']
files+=list((assets/'textures/gui/steel_buffer_v2').glob('*.png'))+[assets/'lang/ru_ru.json',assets/'lang/en_us.json']
jar=root/'build/libs/domesurvival-0.2.0.jar'
with zipfile.ZipFile(jar) as z:
    for p in files:assert z.read(p.relative_to(resources).as_posix())==p.read_bytes(),p
    assert not any('steelbufferprobe' in n for n in z.namelist())
    for name in ('machine/energy/EnergyBufferBlockEntity','machine/energy/EnergyBufferMenu','client/screen/EnergyBufferScreen','client/render/SteelBufferPreview','integration/customnpcs/JosephScriptCommand'):
        assert f'com/wasted/domesurvival/forge/{name}.class' in z.namelist()
mods=json.loads((out/'runtime_mods.json').read_text(encoding='utf-8-sig'))
assert set(p.name for p in (root/'run/mods').glob('*.jar'))==set(m['name'] for m in mods)
for m in mods:assert hashlib.sha256((root/'run/mods'/m['name']).read_bytes()).hexdigest().upper()==m['sha256']
assert not list((root/'run/steel-buffer-review/production_mod_hold').glob('*.jar'))
report=dict(result='PASS',runtime_assertions=sum(s.startswith('PASS ') for s in checks.splitlines()),charge_states=5,
            packaged_resources=len(files),preserved_resources=len(unchanged),sha256=hashlib.sha256(jar.read_bytes()).hexdigest())
(out/'release_validation.json').write_text(json.dumps(report,indent=2)+'\n');print(json.dumps(report,indent=2))
