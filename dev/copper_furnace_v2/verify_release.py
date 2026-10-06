"""Verify the copper furnace runtime evidence, connector geometry and shipped assets."""
from pathlib import Path
import json,hashlib,zipfile,copy,struct
root=Path(__file__).resolve().parents[2]
out=Path(__file__).resolve().parent
assets=root/'src/main/resources/assets/domesurvival'
def read_log(path):
    raw=path.read_bytes()
    return raw.decode('utf-16' if raw[:2] in (b'\xff\xfe',b'\xfe\xff') else 'utf-8')
checks=(out/'runtime/checks.txt').read_text()
assert 'RESULT PASS failures=0' in checks and '\nFAIL ' not in checks
for log in ['client.log','build.log']:assert 'BUILD SUCCESSFUL' in read_log(out/log),log
def model(name):return json.loads((assets/'models/block'/f'{name}.json').read_text())
dirs=['north','east','south','west']
def rotate(elements,turns):
    result=copy.deepcopy(elements)
    for e in result:
        for _ in range(turns):
            lo,hi=e['from'],e['to'];e['from']=[16-hi[2],lo[1],lo[0]];e['to']=[16-lo[2],hi[1],hi[0]]
            e['faces']={dirs[(dirs.index(d)+1)%4] if d in dirs else d:f for d,f in e['faces'].items()}
    return result
def face(e,d):
    axis={'east':0,'west':0,'up':1,'down':1,'north':2,'south':2}[d]
    plane=e['to' if d in ('east','up','south') else 'from'][axis]
    return plane,[(e['from'][i],e['to'][i]) for i in range(3) if i!=axis]
def overlaps(a,b,d):
    pa,aa=face(a,d);pb,bb=face(b,d)
    return abs(pa-pb)<1e-5 and all(min(x[1],y[1])-max(x[0],y[0])>1e-5 for x,y in zip(aa,bb))
body=model('copper_furnace');configs=0
for turn in range(4):
    rotated=rotate(body['elements'],turn)
    for direction in dirs+['up','down']:
        if direction==dirs[turn]:continue
        for mode in ['input','output']:
            for e in model(f'copper_furnace_{mode}_port_{direction}')['elements']:
                for d in e['faces']:
                    for b in rotated:assert d not in b['faces'] or not overlaps(e,b,d),(turn,direction,mode,e['name'],b['name'],d)
            configs+=1
for name in ['copper_furnace','copper_furnace_on','copper_furnace_inventory']:
    for e in model(name)['elements']:assert all(0<=lo<hi<=16 for lo,hi in zip(e['from'],e['to'])),e['name']
files=list((assets/'models/block').glob('copper_furnace*.json'))
files+=[assets/'models/item/copper_furnace.json',assets/'blockstates/copper_furnace.json']
files+=list((assets/'textures/block/copper_furnace_v2').iterdir())
files+=list((assets/'textures/gui/copper_furnace_v2').iterdir())
files+=[assets/'lang/ru_ru.json',assets/'lang/en_us.json']
products=sorted({json.loads(p.read_text())['result']['item'].split(':')[1]
                 for p in (root/'src/main/resources/data/domesurvival/recipes/forming').glob('*.json')})
for name in products:
    texture=assets/'textures/item/press_products'/f'{name}.png'
    assert struct.unpack('>II',texture.read_bytes()[16:24])==(32,32),name
    files.extend([texture,texture.with_suffix('.png.mcmeta'),assets/'models/item'/f'{name}.json'])
jar=root/'build/libs/domesurvival-0.2.0.jar'
with zipfile.ZipFile(jar) as archive:
    names=archive.namelist();assert not any('probe/' in name.lower() for name in names)
    for path in files:assert archive.read(path.relative_to(root/'src/main/resources').as_posix())==path.read_bytes(),path
    for cls in ['machine/copper/CopperFurnaceMenu','machine/copper/CopperFurnaceScreen','client/jei/RefinedFuelMachinesJeiPlugin',
                'client/jei/RefinedMachineJeiArt','integration/customnpcs/JosephScriptCommand','machine/alloy/AlloyEnricherBlock','metro/MetroRestorationConsoleBlock']:
        assert 'com/wasted/domesurvival/forge/'+cls+'.class' in names,cls
assert len(list((root/'run/mods').glob('*.jar')))==85
assert not list((root/'run/copper-furnace-review/production_mod_hold').glob('*.jar'))
report={'result':'PASS','runtime_assertions':sum(s.startswith('PASS ') for s in checks.splitlines()),
        'connector_configurations':configs,'restored_32px_products':len(products),'resources_checked':len(files),'jar':str(jar),
        'sha256':hashlib.sha256(jar.read_bytes()).hexdigest(),'bytes':jar.stat().st_size,
        'screenshots':[p.name for p in sorted((out/'runtime').glob('*.png'))]}
(out/'release_validation.json').write_text(json.dumps(report,indent=2)+'\n');print(json.dumps(report,indent=2))
