"""Validate the electrolyzer's tested production artifact, excluding probe classes."""
from pathlib import Path
import hashlib,json,zipfile
root=Path(__file__).resolve().parents[2];out=Path(__file__).resolve().parent
resources=root/'src/main/resources';assets=resources/'assets/domesurvival'
checks=(out/'runtime/checks.txt').read_text()
assert 'RESULT PASS failures=0' in checks and '\nFAIL ' not in checks
for name in ['client.log','build.log']:
    raw=(out/name).read_bytes();log=raw.decode('utf-16' if raw.startswith((b'\xff\xfe',b'\xfe\xff')) else 'utf-8')
    assert 'BUILD SUCCESSFUL' in log,name
visual=json.loads((out/'visual_validation.json').read_text());assert visual['result']=='PASS'
files=list((assets/'textures/gui/oxygen_electrolyzer_v2').iterdir())
files+=list((assets/'models/block').glob('oxygen_electrolyzer*.json'))
files+=list((assets/'models/block').glob('coal_generator_*_port_*.json'))
files+=[assets/'models/item/oxygen_electrolyzer.json',assets/'blockstates/oxygen_electrolyzer.json',assets/'textures/block/coal_generator_v2/satin_atlas.png',assets/'lang/ru_ru.json',assets/'lang/en_us.json',resources/'data/domesurvival/loot_tables/blocks/oxygen_electrolyzer.json']
jar=root/'build/libs/domesurvival-0.2.0.jar'
with zipfile.ZipFile(jar) as z:
    names=z.namelist();assert not any('probe/' in n.lower() for n in names)
    for p in files:assert z.read(p.relative_to(resources).as_posix())==p.read_bytes(),p
    for name in ['machine/oxygen/OxygenElectrolyzerBlockEntity','machine/oxygen/OxygenElectrolyzerMenu','client/render/OxygenElectrolyzerRenderer','client/render/OxygenElectrolyzerPreview','client/screen/OxygenElectrolyzerScreen','client/jei/OxygenElectrolyzerJeiCategory','integration/customnpcs/JosephScriptCommand']:
        assert f'com/wasted/domesurvival/forge/{name}.class' in names,name
assert len(list((root/'run/mods').glob('*.jar')))==85
assert not list((root/'run/oxygen-electrolyzer-review/production_mod_hold').glob('*.jar'))
report={'result':'PASS','runtime_assertions':sum(x.startswith('PASS ') for x in checks.splitlines()),'visual_checks':visual,'packaged_resources_checked':len(files),'jar':str(jar),'sha256':hashlib.sha256(jar.read_bytes()).hexdigest(),'bytes':jar.stat().st_size,'screenshots':[p.name for p in sorted((out/'runtime').glob('*.png'))]}
(out/'release_validation.json').write_text(json.dumps(report,indent=2)+'\n');print(json.dumps(report,indent=2))
