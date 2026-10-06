"""Validate the reviewed material/connector release and packaged resource bytes."""
from pathlib import Path
import json,struct,zipfile,hashlib
root=Path(__file__).resolve().parents[2]
dev=root/'dev/materials_visual'
assets=root/'src/main/resources/assets/domesurvival'
manifest=json.loads((dev/'manifest.json').read_text())
pixels=json.loads((dev/'pixel_validation.json').read_text(encoding='utf-8-sig'))
assert pixels['result']=='PASS' and pixels['exact_original_designs']==39 and pixels['gunpowder_silhouettes']==9
assert len(manifest['items'])==30 and len(manifest['ores'])==12 and len(manifest['storage_blocks'])==8
checks=(root/'dev/copper_furnace_v2/runtime/checks.txt').read_text()
assert 'RESULT PASS failures=0' in checks and '\nFAIL ' not in checks
for name in ['INPUT','OUTPUT','DISABLED']:
    assert 'PASS Connector button synchronized '+name in checks
files=[root/p for p in manifest['files']]
assert not any('solarite' in str(p) for p in files)
for p in files:
    if p.suffix=='.png':assert struct.unpack('>II',p.read_bytes()[16:24])==(32,32),p
    if p.suffix=='.json':
        original=json.loads((dev/'baseline'/p.relative_to(root)).read_text())
        model=json.loads(p.read_text());original.pop('textures');model.pop('textures')
        assert original==model,p
files+=list((assets/'textures/gui/item_connector_v2').iterdir())
files+=[assets/'lang/ru_ru.json',assets/'lang/en_us.json']
for p in (assets/'textures/item/press_products').glob('*.png'):
    assert struct.unpack('>II',p.read_bytes()[16:24])==(32,32),p
    files.extend([p,p.with_suffix('.png.mcmeta'),assets/'models/item'/(p.stem+'.json')])
jar=root/'build/libs/domesurvival-0.2.0.jar'
with zipfile.ZipFile(jar) as archive:
    names=archive.namelist()
    assert not any('probe/' in name.lower() for name in names)
    for p in files:assert archive.read(p.relative_to(root/'src/main/resources').as_posix())==p.read_bytes(),p
    for folder in ['item','block']:
        actual={p.name for p in (assets/'textures'/folder/'materials_v2').iterdir()}
        expected={p.name for p in files if p.parent==assets/'textures'/folder/'materials_v2'}
        assert actual==expected,'Stale material exports'
        prefix='assets/domesurvival/textures/'+folder+'/materials_v2/'
        assert {name[len(prefix):] for name in names if name.startswith(prefix) and not name.endswith('/')}==expected,'Stale material exports in JAR'
    assert 'com/wasted/domesurvival/forge/integration/customnpcs/JosephScriptCommand.class' in names
assert len(list((root/'run/mods').glob('*.jar')))==85
assert not list((root/'run/copper-furnace-review/production_mod_hold').glob('*.jar'))
report={'result':'PASS','pixels':pixels,'runtime_assertions':sum(s.startswith('PASS ') for s in checks.splitlines()),
        'resources_checked':len(files),'jar':str(jar),'sha256':hashlib.sha256(jar.read_bytes()).hexdigest()}
(dev/'release_validation.json').write_text(json.dumps(report,indent=2)+'\n')
print(json.dumps(report,indent=2))
