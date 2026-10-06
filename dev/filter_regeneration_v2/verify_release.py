"""Verify tested filter-regenerator resources in the production artifact."""
from pathlib import Path
import hashlib,json,zipfile
root=Path(__file__).resolve().parents[2];out=Path(__file__).resolve().parent
resources=root/'src/main/resources';assets=resources/'assets/domesurvival'
checks=(out/'runtime/checks.txt').read_text(encoding='utf-8')
assert 'RESULT PASS failures=0' in checks and '\nFAIL ' not in checks
for name in ['client.log','build.log']:
    raw=(out/name).read_bytes();log=raw.decode('utf-16' if raw.startswith((b'\xff\xfe',b'\xfe\xff')) else 'utf-8')
    assert 'BUILD SUCCESSFUL' in log,name
visual=json.loads((out/'visual_validation.json').read_text());assert visual['result']=='PASS'
files=[resources/'data/minecraft/tags/blocks/needs_stone_tool.json',resources/'data/minecraft/tags/blocks/mineable/pickaxe.json']
files+=list((assets/'textures/gui/filter_regeneration_v2').iterdir())
files+=list((assets/'models/block').glob('filter_regeneration_station*.json'))
files+=list((assets/'models/block').glob('coal_generator_*_port_*.json'))
files+=[assets/'models/item/filter_regeneration_station.json',assets/'blockstates/filter_regeneration_station.json',assets/'textures/block/coal_generator_v2/satin_atlas.png',assets/'lang/ru_ru.json',assets/'lang/en_us.json',resources/'data/domesurvival/loot_tables/blocks/filter_regeneration_station.json']
jar=root/'build/libs/domesurvival-0.2.0.jar'
with zipfile.ZipFile(jar) as z:
    names=z.namelist();assert not any('probe/' in n.lower() for n in names)
    for p in files:assert z.read(p.relative_to(resources).as_posix())==p.read_bytes(),p
    for name in ['machine/filter/FilterRegenerationBlockEntity','machine/filter/FilterRegenerationMenu','machine/filter/FilterRegenerationScreen','client/render/FilterRegenerationRenderer','client/render/FilterRegenerationPreview','client/jei/FilterRegenerationJeiCategory','integration/customnpcs/JosephScriptCommand']:
        assert f'com/wasted/domesurvival/forge/{name}.class' in names,name
assert len(list((root/'run/mods').glob('*.jar')))==85
assert not list((root/'run/filter-regeneration-review/production_mod_hold').glob('*.jar'))
report={'result':'PASS','runtime_assertions':sum(x.startswith('PASS ') for x in checks.splitlines()),'visual_checks':visual,'packaged_resources_checked':len(files),'jar':str(jar),'sha256':hashlib.sha256(jar.read_bytes()).hexdigest(),'bytes':jar.stat().st_size,'screenshots':[p.name for p in sorted((out/'runtime').glob('*.png'))]}
(out/'release_validation.json').write_text(json.dumps(report,indent=2)+'\n');print(json.dumps(report,indent=2))
