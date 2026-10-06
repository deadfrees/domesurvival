"""Premium adamantium store: cross-shaped core, reinforced corners and roof bridges."""
from pathlib import Path
source=Path(__file__).with_name('create_water_purifier_v2.py')
s=source.read_text().replace('water_purifier','adamantium_buffer').replace('Purifier / ','Adamantium buffer / ').replace('WATER_PURIFIER','ADAMANTIUM_BUFFER')
a=s.index('for x in (0,15.2):');b=s.index('for x in (1,12.8):',a)
s=s[:a]+'''# Posts stop below the shoulders; rails meet their edges without coplanar overlap.
for x in (0,14.65):
    for z in (0,14.65):box('Reinforced corner post',[x,.3,z],[x+1.35,14.65,z+1.35],'frame')
for y in (.3,15.2):
    edge=1.35 if y<1 else 2.4
    for z in (0,15.2):box('Cross rail',[edge,y,z],[16-edge,y+.5,z+.8],'frame')
    for x in (0,15.2):box('Side rail',[x,y,edge],[x+.8,y+.5,16-edge],'frame')
for x in (0,13.6):
    for z in (0,13.6):
        box('Armored shoulder',[x,14.65,z],[x+2.4,15.85,z+2.4],'steel')
        # A broad satin patch avoids shimmering from the original seven-pixel strip.
        for face in body[-1]['faces'].values():face['uv']=[5.90625,9.234375,6.59375,9.859375]
for z in (1.0,14.0):box('Roof power bridge',[2.4,15.4,z],[13.6,15.85,z+1.0],'trim')
for x in (.65,14.25):
    for z in (.65,14.25):box('Shoulder inlay',[x,15.85,z],[x+1.1,16.0,z+1.1],'brass')
'''+s[b:]
a=s.index('# Two open sight vessels:');b=s.index('for x in (1.45,14.1):',a)
s=s[:a]+'''box('Core backing',[3,2.9,7.6],[13,12.9,8.2],'frame')
box('Vertical accumulator core',[5.4,3.2,2.2],[10.6,12.6,7.6],'steel')
box('Left accumulator arm',[3.2,5.5,2.7],[5.4,10.3,7.6],'steel')
box('Right accumulator arm',[10.6,5.5,2.7],[12.8,10.3,7.6],'steel')
for x in (3.0,11.1):
    for y in (3.0,11.1):box('Core corner clamp',[x,y,1.7],[x+1.9,y+1.7,7.6],'frame')
box('Central charge bezel',[5.85,3.55,2.0],[10.15,12.25,2.2],'black')
for i,y in enumerate((3.85,6.0,8.15,10.3)):
    box('Charge window '+str(i),[6.2,y,1.85],[9.8,y+1.6,2.0],'input')
box('Left core contact',[3.35,7.35,2.45],[5.4,8.45,2.7],'brass')
box('Right core contact',[10.6,7.35,2.45],[12.65,8.45,2.7],'brass')
box('Core foot',[5.0,2.65,3.0],[11.0,3.2,7.6],'body')
'''+s[b:]
a=s.index('elements=[]',s.index('def model(es):'));b=s.index('# Same studio',a)
s=s[:a]+'''import copy
dest=A/'models/block/adamantium_buffer_v2';dest.mkdir(parents=True,exist_ok=True)
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
item={'parent':'domesurvival:block/adamantium_buffer_v2/level_0','overrides':[
    {'predicate':{'domesurvival:buffer_charge':i/4},'model':f'domesurvival:block/adamantium_buffer_v2/level_{i}'} for i in range(1,5)]}
(A/'models/item/energy_buffer_adamantium.json').write_text(json.dumps(item,indent=2)+'\\n')
parts=[]
for facing,angle in [('north',0),('east',90),('south',180),('west',270)]:
    for level in range(5):parts.append({'when':{'facing':facing,'energy_level':str(level)},'apply':{'model':f'domesurvival:block/adamantium_buffer_v2/level_{level}','y':angle}})
for direction in ('up','down','north','south','east','west'):
    for mode in ('input','output'):parts.append({'when':{'port_'+direction:mode},'apply':{'model':f'domesurvival:block/coal_generator_{mode}_port_{direction}'}})
(A/'blockstates/energy_buffer_adamantium.json').write_text(json.dumps({'multipart':parts},indent=2)+'\\n')
moving=[]
'''+s[b:]
exec(compile(s,str(source),'exec'))
