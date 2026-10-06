"""Four stationary steel battery cassettes, with real charge-level windows."""
from pathlib import Path
source=Path(__file__).with_name('create_water_purifier_v2.py')
s=source.read_text().replace('water_purifier','steel_buffer').replace('Purifier / ','Steel buffer / ').replace('WATER_PURIFIER','STEEL_BUFFER')
a=s.index('for x in (0,15.2):');b=s.index('for x in (1,12.8):',a)
s=s[:a]+'''# The original narrow corner posts must not share exterior faces with Nexus armor.
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
s=s[:a]+'''box('Battery shelf',[3,2.5,1.5],[13,2.9,8],'steel')
for x in (3.0,12.65):box('Cassette guide',[x,3,1.7],[x+.35,12.2,7.9],'trim')
for i,y in enumerate((3.0,5.3,7.6,9.9)):
    box('Accumulator casing',[3.5,y,2.0],[12.5,y+1.8,7.8],'body')
    box('Lid gasket',[3.5,y,1.9],[12.5,y+1.8,2.0],'black')
    box('Steel cassette lid',[3.7,y+.2,1.7],[12.3,y+1.6,2.0],'steel')
    for x in (4.0,11.4):box('Bus terminal',[x,y+.55,1.55],[x+.6,y+1.15,1.7],'brass')
    box('Charge window '+str(i),[6,y+.6,1.64],[10,y+1.2,1.69],'input')
'''+s[b:]
a=s.index('elements=[]',s.index('def model(es):'));b=s.index('# Same studio',a)
s=s[:a]+'''import copy
dest=A/'models/block/steel_buffer_v2';dest.mkdir(parents=True,exist_ok=True)
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
item={'parent':'domesurvival:block/steel_buffer_v2/level_0','overrides':[
    {'predicate':{'domesurvival:buffer_charge':i/4},'model':f'domesurvival:block/steel_buffer_v2/level_{i}'} for i in range(1,5)]}
(A/'models/item/energy_buffer.json').write_text(json.dumps(item,indent=2)+'\\n')
parts=[]
for facing,angle in [('north',0),('east',90),('south',180),('west',270)]:
    for level in range(5):parts.append({'when':{'facing':facing,'energy_level':str(level)},'apply':{'model':f'domesurvival:block/steel_buffer_v2/level_{level}','y':angle}})
for direction in ('up','down','north','south','east','west'):
    for mode in ('input','output'):parts.append({'when':{'port_'+direction:mode},'apply':{'model':f'domesurvival:block/coal_generator_{mode}_port_{direction}'}})
(A/'blockstates/energy_buffer.json').write_text(json.dumps({'multipart':parts},indent=2)+'\\n')
moving=[]
'''+s[b:]
exec(compile(s,str(source),'exec'))
