"""Build the creative Nexus from the family chassis with one red infinite-energy display."""
from pathlib import Path
import json
import copy
root=Path(__file__).resolve().parents[3]
assets=root/'src/main/resources/assets/domesurvival'
adam=json.loads((assets/'models/block/adamantium_buffer_v2/level_4.json').read_text())
atlas='domesurvival:block/coal_generator_v2/satin_atlas'
uv={}
for element in adam['elements']:
    for face in element.get('faces',{}).values():
        role=face['texture'].lstrip('#')
        uv.setdefault(role,face['uv'])
def faces(role):
    patch=[5.90625,9.234375,6.59375,9.859375] if role=='steel' else uv[role]
    return {side:{'texture':'#'+role,'uv':patch} for side in ('down','up','north','south','west','east')}
elements=[]
def box(name,lo,hi,role):
    elements.append({'name':name,'from':lo,'to':hi,'faces':faces(role)})
for x in (0,14.65):
    for z in (0,14.65):
        box('Nexus reinforced corner',[x,.3,z],[x+1.35,14.65,z+1.35],'frame')
for x in (0,13.6):
    for z in (0,13.6):
        box('Nexus armored shoulder',[x,14.65,z],[x+2.4,15.85,z+2.4],'steel')
        # Cap the shoulder at block height without burying its brass inlay.
        box('Nexus shoulder cap',[x,15.85,z],[x+2.4,16,z+.65],'steel')
        box('Nexus shoulder cap',[x,15.85,z+1.75],[x+2.4,16,z+2.4],'steel')
        box('Nexus shoulder cap',[x,15.85,z+.65],[x+.65,16,z+1.75],'steel')
        box('Nexus shoulder cap',[x+1.75,15.85,z+.65],[x+2.4,16,z+1.75],'steel')
for z in (0,15.0):
    box('Nexus sealed roof rim',[2.4,15.7,z],[13.6,16,z+1.0],'trim')
for x in (0,15.0):
    box('Nexus sealed roof rim',[x,15.7,2.4],[x+1.0,16,13.6],'trim')
for z in (1.0,14.0):
    box('Nexus roof bridge',[2.4,15.4,z],[13.6,15.85,z+1.0],'trim')
for x in (.65,14.25):
    for z in (.65,14.25):
        box('Nexus brass inlay',[x,15.85,z],[x+1.1,16.0,z+1.1],'brass')
frame={'parent':'minecraft:block/block','render_type':'minecraft:cutout',
    'textures':{role:atlas for role in ('frame','steel','trim','brass','particle')},
    'elements':elements}
(assets/'models/block/energy_buffer_creative_frame.json').write_text(json.dumps(frame,indent=2)+'\n')
armor_names={'Reinforced corner post','Armored shoulder','Roof power bridge','Shoulder inlay'}
core_elements=[copy.deepcopy(element) for element in adam['elements']
               if element.get('name') not in armor_names
               and element.get('name') != 'Mounting foot'
               and not element.get('name','').startswith('Charge window ')]
# A continuous plinth meets the four lower rails at y=.3 and seals the
# daylight gap beneath the inherited mounting feet on every side.
core_elements.append({
    'name':'Nexus sealed base plinth','from':[0,0,0],'to':[16,.3,16],
    'faces':faces('frame')
})
# The Adamantium battery architecture remains visible, but its four level lamps
# become one permanent, bright red creative-energy display.
core_elements.append({
    'name':'Infinite energy indicator','from':[6.0,6.0,1.82],'to':[10.0,10.0,2.0],
    'shade':False,'forge_data':{'block_light':12,'ambient_occlusion':False},
    'faces':{'north':{'texture':'#creative_indicator','uv':[0,0,16,16]}}
})
core={'parent':'minecraft:block/block','render_type':'minecraft:cutout',
      'textures':{**adam['textures'],'creative_indicator':'domesurvival:block/energy_buffer_creative/front'},
      'elements':core_elements}
(assets/'models/block/energy_buffer_creative.json').write_text(json.dumps(core,indent=2)+'\n')
inventory={'parent':'minecraft:block/block','render_type':'minecraft:cutout',
           'textures':{**core['textures'],**frame['textures'],'particle':core['textures']['particle']},
           'elements':core['elements']+frame['elements']}
(assets/'models/block/energy_buffer_creative_inventory.json').write_text(json.dumps(inventory,indent=2)+'\n')
(assets/'models/item/energy_buffer_creative.json').write_text(json.dumps({
    'parent':'domesurvival:block/energy_buffer_creative_inventory'},indent=2)+'\n')
state=json.loads((assets/'blockstates/energy_buffer_creative.json').read_text())
frame_name='domesurvival:block/energy_buffer_creative_frame'
state['multipart']=[part for part in state['multipart'] if part.get('apply',{}).get('model')!=frame_name]
state['multipart'].insert(1,{'apply':{'model':frame_name}})
for part in state['multipart']:
    for key,mode in part.get('when',{}).items():
        if key.startswith('port_') and mode in ('input','output'):
            side=key.removeprefix('port_')
            part['apply']={'model':f'domesurvival:block/coal_generator_{mode}_port_{side}'}
(assets/'blockstates/energy_buffer_creative.json').write_text(json.dumps(state,indent=2)+'\n')
print('CREATIVE_NEXUS_FRAME_COMPLETE',len(elements))
