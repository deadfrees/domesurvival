"""Graphite purifier with an open service chamber, twin vessels and a real pump."""
from pathlib import Path
script=Path(__file__).with_name('create_buffer_module_v2.py')
prefix=script.read_text().split("box('Graphite cartridge backplate'")[0]
prefix=prefix.replace('blender/buffer_module','blender/water_purifier').replace("'Buffer / '","'Purifier / '")
prefix=prefix.replace("roles=['body'","roles=['panel','body'")
exec(compile(prefix,str(script),'exec'))
body=elements
box('Rear pressure equipment',[.65,.7,8.5],[15.35,15.3,15.4],'panel')
box('Left enclosure',[.65,.7,.6],[2.65,15.3,8.5],'panel')
box('Right enclosure',[13.35,.7,.6],[15.35,15.3,8.5],'panel')
box('Roof enclosure',[2.65,13.2,.6],[13.35,15.3,8.5],'panel')
box('Pump base enclosure',[2.65,.7,.6],[13.35,2.6,8.5],'panel')
box('Chamber backing',[2.65,2.6,8.3],[13.35,13.2,8.5],'black')
for x in (0,15.2):
    for z in (0,15.2):box('Corner frame',[x,.3,z],[x+.8,15.7,z+.8],'frame')
for y in (.3,15.2):
    for z in (0,15.2):box('Cross rail',[.8,y,z],[15.2,y+.5,z+.8],'frame')
    for x in (0,15.2):box('Side rail',[x,y,.8],[x+.8,y+.5,15.2],'frame')
for x in (1,12.8):box('Mounting foot',[x,0,.5],[x+2.2,.55,15.5],'black')
# Two open sight vessels: fluid columns are rendered from the actual tank levels.
for x in (3.2,10.2):
    for y in (3.2,11.65):
        box('Vessel collar',[x-.2,y,2.25],[x+2.8,y+.65,5.8],'trim')
        box('Collar gasket',[x,y+.65,2.4],[x+2.6,y+.82,5.65],'black')
    for xx in (x,x+2.35):box('Sight glass edge',[xx,4.02,2.4],[xx+.25,11.65,2.65],'steel')
    box('Vessel rear wall',[x,4.02,5.5],[x+2.6,11.65,5.7],'frame')
    for y in (4.8,6.3,7.8,9.3,10.8):box('Volume graduation',[x+.2,y,2.38],[x+.75,y+.1,2.42],'trim')
    box('Upper feed stem',[x+1,12.47,3.55],[x+1.65,13.4,4.2],'steel')
# A pleated replaceable membrane between the water vessels.
box('Membrane frame',[6.45,7.6,4.6],[9.55,12.3,7.4],'frame')
for x in (6.75,7.2,7.65,8.1,8.55,9.0):box('Filter pleat',[x,7.9,3.95],[x+.22,12,4.65],'steel')
box('Filter retaining band',[6.45,9.75,3.75],[9.55,10.2,4.7],'brass')
box('Pump rear casing',[6.1,3.35,5.2],[9.9,7.15,6.4],'frame')
for x in (5.8,9.85):box('Pump mount',[x,3.1,3.3],[x+.35,7.5,6.5],'trim')
box('Lower manifold',[4.25,2.8,4.65],[11.8,3.35,5.4],'steel')
box('Upper manifold',[4.25,12.55,4.6],[11.8,13.1,5.3],'steel')
for x in (1.45,14.1):
    for y in (1.5,14):box('Front screw',[x,y,.15],[x+.4,y+.4,.5],'bolt')
for element in source['elements']:
    if element['name'].endswith(('_service_skin','_roof_skin')):
        box('Matched '+element['name'],element['from'],element['to'],'panel')
        body[-1]['faces']=json.loads(json.dumps(element['faces']))
for x in (.25,15.5):
    box('Side service pad',[x,4.6,4.6],[x+.25,11.4,11.4],'frame')
    for y in (12.3,13,13.7):box('Cooling fin',[x,y,3.8],[x+.25,y+.25,12.2],'trim')
box('Rear service pad',[4.6,4.6,15.55],[11.4,11.4,15.8],'frame')
for y in (.2,15.55):box('Vertical service pad',[4.6,y,4.6],[11.4,y+.25,11.4],'frame')
# Thin service hardware stays outside the central 4.6..11.4 port footprint.
# All raised layers have distinct planes to prevent connector z-fighting.
def service_detail(name,u0,v0,u1,v1,depth,role):
    for side in ('left','right','rear'):
        if side=='left':lo,hi=[.30-depth,v0,u0],[.30,v1,u1]
        elif side=='right':lo,hi=[15.70,v0,u0],[15.70+depth,v1,u1]
        else:lo,hi=[u0,v0,15.70],[u1,v1,15.70+depth]
        box(side+' '+name,lo,hi,role)
service_detail('lower hatch gasket',2.0,1.7,14.0,4.1,.08,'black')
service_detail('lower service hatch',2.2,1.9,13.8,3.9,.14,'body')
service_detail('hatch handle',6.5,2.6,9.5,2.95,.25,'trim')
for u in (2.5,13.1):
    for v in (2.2,3.3):service_detail('hatch captive bolt',u,v,u+.3,v+.3,.24,'bolt')
for u in (2.1,12.9):
    service_detail('reinforcing rib',u,5.0,u+.65,11.0,.18,'frame')
    service_detail('rib highlight',u+.15,5.2,u+.30,10.8,.22,'trim')
for u in (1.6,14.0):
    for v in (12.0,14.3):service_detail('upper panel fastener',u,v,u+.35,v+.35,.22,'bolt')
# Rear heat exchanger above the socket, distinct from the side cooling slats.
box('Rear cooler gasket',[3.7,12.0,15.71],[12.3,14.5,15.80],'black')
for x in (4.0,5.1,6.2,7.3,8.4,9.5,10.6,11.7):
    box('Rear exchanger rib',[x,12.2,15.80],[x+.3,14.3,15.94],'trim')
textures={role:source['textures'][role] for role in roles};textures['particle']=source['textures']['body']
def model(es):return {'parent':'minecraft:block/block','render_type':'minecraft:cutout','textures':textures,'elements':es}
elements=[]
box('Pump hub',[7.55,4.8,3.05],[8.45,5.7,4.6],'brass')
box('Impeller horizontal',[6.55,5.03,3.55],[9.45,5.47,4.2],'steel')
box('Impeller vertical',[7.78,3.8,3.55],[8.22,6.7,4.2],'steel')
moving=elements
for name,es in [('water_purifier',body),('water_purifier_lit',body),('water_purifier_pump',moving),('water_purifier_inventory',body+moving)]:
    (A/f'models/block/{name}.json').write_text(json.dumps(model(es),indent=2)+'\n')
(A/'models/item/water_purifier.json').write_text('{"parent":"domesurvival:block/water_purifier_inventory"}\n')
state=json.loads((A/'blockstates/water_purifier.json').read_text())
for part in state['multipart']:
    part['apply']['model']=part['apply']['model'].replace('block/machine_input_port_','block/coal_generator_input_port_').replace('block/machine_output_port_','block/coal_generator_output_port_')
(A/'blockstates/water_purifier.json').write_text(json.dumps(state,indent=2)+'\n')
# Same studio and color pipeline as the reference press.
studio=Path(__file__).with_name('create_forming_press_v2.py').read_text().split("scene=bpy.context.scene;scene.render.engine='CYCLES'")[1]
studio="scene=bpy.context.scene;scene.render.engine='CYCLES'"+studio
studio=studio.replace('forming_press.png','water_purifier.png').replace('forming_press.blend','water_purifier.blend').replace('FORMING_PRESS_MODEL_COMPLETE','WATER_PURIFIER_MODEL_COMPLETE')
exec(compile(studio,__file__,'exec'))
