"""Check socket planes at all rotations and the shared generated item family."""
from pathlib import Path
from itertools import combinations
import copy,json
root=Path(__file__).resolve().parents[2]
assets=root/'src/main/resources/assets/domesurvival'
def model(name):return json.loads((assets/'models/block'/f'{name}.json').read_text())
body=model('oxygen_filler')
dirs=['north','east','south','west']
def rotate(elements,turns):
    result=copy.deepcopy(elements)
    for e in result:
        for _ in range(turns):
            lo,hi=e['from'],e['to']
            e['from']=[16-hi[2],lo[1],lo[0]];e['to']=[16-lo[2],hi[1],hi[0]]
            e['faces']={dirs[(dirs.index(d)+1)%4] if d in dirs else d:f for d,f in e['faces'].items()}
    return result
def face(e,d):
    axis={'east':0,'west':0,'up':1,'down':1,'north':2,'south':2}[d]
    plane=e['to' if d in ('east','up','south') else 'from'][axis]
    uv=[i for i in range(3) if i!=axis]
    return plane,[(e['from'][i],e['to'][i]) for i in uv]
def overlaps(a,b,d):
    pa,aa=face(a,d);pb,bb=face(b,d)
    return abs(pa-pb)<1e-5 and all(min(x[1],y[1])-max(x[0],y[0])>1e-5 for x,y in zip(aa,bb))
checks=0
for turn in range(4):
    rotated=rotate(body['elements'],turn)
    for direction in dirs+['up','down']:
        if direction==dirs[turn]:continue
        for mode in ['input','output']:
            port=model(f'coal_generator_{mode}_port_{direction}')
            for e in port['elements']:
                for d in e['faces']:
                    for b in rotated:
                        assert d not in b['faces'] or not overlaps(e,b,d),(turn,direction,mode,e['name'],b['name'],d)
            checks+=1
generator=model('coal_generator')
reference=[f['uv'] for e in generator['elements'] for f in e['faces'].values() if f['texture']=='#panel']
for e in body['elements'][:5]:
    assert all(f['texture']=='#panel' and f['uv'] in reference for f in e['faces'].values())
assert body['textures']['panel']==generator['textures']['panel']
modules=['buffer','efficiency','overdrive','automation','emergency_protection','communication']
for name in modules:
    item=json.loads((assets/'models/item'/f'{name}_module.json').read_text())
    assert item['parent']=='minecraft:item/generated' and 'elements' not in item
    assert (assets/'textures'/ (item['textures']['layer0'].split(':')[1]+'.png')).is_file()
report={'result':'PASS','socket_configurations_without_coplanar_overlap':checks,'flat_module_items':len(modules),'electrolyzer_enclosure_uses_generator_panel_uv':True}
Path(__file__).with_name('visual_validation.json').write_text(json.dumps(report,indent=2)+'\n')
print(json.dumps(report,indent=2))
