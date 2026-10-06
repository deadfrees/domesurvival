"""Titan storage: a monolithic battery, power clamps, fins and four charge windows."""
from pathlib import Path
source=Path(__file__).with_name('create_water_purifier_v2.py')
s=source.read_text().replace('water_purifier','titan_buffer').replace('Purifier / ','Titan buffer / ').replace('WATER_PURIFIER','TITAN_BUFFER')
a=s.index('for x in (0,15.2):');b=s.index('for x in (1,12.8):',a)
s=s[:a]+'''# Keep the original rails clear of the heavy Nexus corners.
for x in (0,14.65):
    for z in (0,14.65):box('Reinforced corner post',[x,.3,z],[x+1.35,14.65,z+1.35],'frame')
for y in (.3,15.2):
    edge=1.35 if y<1 else 2.4
    for z in (0,15.2):box('Cross rail',[edge,y,z],[16-edge,y+.5,z+.8],'frame')
    for x in (0,15.2):box('Side rail',[x,y,edge],[x+.8,y+.5,16-edge],'frame')
for x in (0,13.6):
    for z in (0,13.6):
        box('Armored shoulder',[x,14.65,z],[x+2.4,15.85,z+2.4],'steel')
        for face in body[-1]['faces'].values():face['uv']=[5.90625,9.234375,6.59375,9.859375]
for z in (1.0,14.0):box('Roof power bridge',[2.4,15.4,z],[13.6,15.85,z+1.0],'trim')
for x in (.65,14.25):
    for z in (.65,14.25):box('Shoulder inlay',[x,15.85,z],[x+1.1,16.0,z+1.1],'brass')
'''+s[b:]
a=s.index('# Two open sight vessels:');b=s.index('for x in (1.45,14.1):',a)
s=s[:a]+'''box('Titan monolithic accumulator',[3.25,3.0,2.5],[12.75,12.8,8.0],'body')
box('Front plate gasket',[3.5,3.4,2.25],[12.5,12.4,2.5],'black')
box('Titanium face plate',[3.75,3.65,2.0],[12.25,12.15,2.25],'steel')
for y in (2.8,12.1):box('Power clamp',[3.0,y,1.75],[13.0,y+.9,8.1],'frame')
for x in (4.25,5.5,6.75):box('Vertical heat rib',[x,4.2,1.65],[x+.5,11.6,2.0],'trim')
box('Charge display bezel',[8.25,4.0,1.85],[11.75,11.8,2.0],'black')
for i,y in enumerate((4.25,6.15,8.05,9.95)):
    box('Charge window '+str(i),[8.65,y,1.75],[11.35,y+1.55,1.85],'input')
for y in (3.0,12.3):box('Power clamp contact',[7.5,y,1.5],[8.5,y+.45,1.75],'brass')
'''+s[b:]
a=s.index('elements=[]',s.index('def model(es):'));b=s.index('# Same studio',a)
s=s[:a]+'''import copy
dest=A/'models/block/titan_buffer_v2';dest.mkdir(parents=True,exist_ok=True)
for level in range(5):
    es=copy.deepcopy(body)
    for e in es:
        if e['name'].startswith('Charge window '):
            if int(e['name'].split()[-1])>=level:
                e['faces']={d:{'texture':'#black','uv':uvs['black'][1]} for d in e['faces']}
            else:
                e['shade']=False
                e['forge_data']={'block_light':12,'ambient_occlusion':False}
    (dest/f'level_{level}.json').write_text(json.dumps(model(es),indent=2)+'\\n')
item={'parent':'domesurvival:block/titan_buffer_v2/level_0','overrides':[
    {'predicate':{'domesurvival:buffer_charge':i/4},'model':f'domesurvival:block/titan_buffer_v2/level_{i}'} for i in range(1,5)]}
(A/'models/item/energy_buffer_titan.json').write_text(json.dumps(item,indent=2)+'\\n')
parts=[]
for facing,angle in [('north',0),('east',90),('south',180),('west',270)]:
    for level in range(5):parts.append({'when':{'facing':facing,'energy_level':str(level)},'apply':{'model':f'domesurvival:block/titan_buffer_v2/level_{level}','y':angle}})
for direction in ('up','down','north','south','east','west'):
    for mode in ('input','output'):parts.append({'when':{'port_'+direction:mode},'apply':{'model':f'domesurvival:block/coal_generator_{mode}_port_{direction}'}})
(A/'blockstates/energy_buffer_titan.json').write_text(json.dumps({'multipart':parts},indent=2)+'\\n')
moving=[]
'''+s[b:]
exec(compile(s,str(source),'exec'))
