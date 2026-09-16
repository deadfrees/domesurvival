"""Blender-authored one-block metalworking machine, using approved satin materials."""
from pathlib import Path
script=Path(__file__).with_name('create_buffer_module_v2.py')
prefix=script.read_text().split("box('Graphite cartridge backplate'")[0]
prefix=prefix.replace("blender/buffer_module", "blender/forming_press").replace("'Buffer / '","'Press / '")
exec(compile(prefix,str(script),'exec'))
body=elements
box('Rear machinery enclosure',[.65,.7,8.5],[15.35,15.3,15.4],'body')
box('Left column housing',[.65,.7,.6],[3.05,15.3,8.5],'body')
box('Right column housing',[12.95,.7,.6],[15.35,15.3,8.5],'body')
box('Upper hydraulic enclosure',[3.05,12.35,.6],[12.95,15.3,8.5],'body')
box('Lower drive enclosure',[3.05,.7,.6],[12.95,2.6,8.5],'body')
for x in (0,15.2):
    for z in (0,15.2):box('Structural corner',[x,.3,z],[x+.8,15.7,z+.8],'frame')
for y in (.3,15.2):
    for z in (0,15.2):box('Cross rail',[.8,y,z],[15.2,y+.5,z+.8],'frame')
    for x in (0,15.2):box('Side rail',[x,y,.8],[x+.8,y+.5,15.2],'frame')
for x in (1,12.8):box('Mounting foot',[x,0,.5],[x+2.2,.55,15.5],'black')
box('Deep working chamber rear',[3.05,2.6,8.35],[12.95,12.35,8.49],'black')
for x in (3.65,11.75):
    box('Guide base',[x-.3,2.6,3.1],[x+.85,3.5,4.45],'frame')
    box('Machined guide',[x,3.5,3.35],[x+.55,12.3,3.95],'steel')
    box('Guide top collar',[x-.2,11.65,3.15],[x+.75,12.35,4.25],'trim')
box('Workbed rim',[3.3,2.6,.5],[12.7,3.35,7.8],'trim')
box('Workbed',[4.2,3.35,1.1],[11.8,3.55,7.25],'steel')
for z in (2,3.5,5,6.5):box('Bed T slot',[4.3,3.55,z],[11.7,3.58,z+.12],'black')
box('Lower die',[6.3,3.58,3],[9.7,4.25,6.4],'frame')
box('Hydraulic cylinder',[6.2,10.9,4.1],[9.8,12.4,7.6],'frame')
box('Identification plate',[5.5,13.25,.3],[10.5,14.2,.58],'brass')
for x in (1.5,13.8):
    for y in (1.5,14):box('Captive screw',[x,y,.1],[x+.45,y+.45,.45],'bolt')
# Surface pads under the world-side ports, at the real block boundary.
for side in ['east','west','south','up','down']:
    p=json.loads((A/f'models/block/coal_generator_input_port_{side}.json').read_text())
    # Actual active connector models remain separate blockstate overlays.
    (A/f'models/block/forming_press_input_port_{side}.json').write_text(json.dumps(p,indent=2)+'\n')
    out=json.loads((A/f'models/block/coal_generator_output_port_{side}.json').read_text())
    (A/f'models/block/forming_press_output_port_{side}.json').write_text(json.dumps(out,indent=2)+'\n')
for mode in ['input','output']:
    p=json.loads((A/f'models/block/coal_generator_{mode}_port_north.json').read_text())
    (A/f'models/block/forming_press_{mode}_port_north.json').write_text(json.dumps(p,indent=2)+'\n')
for x in (.25,15.5):
    box('Side service pad',[x,4.6,4.6],[x+.25,11.4,11.4],'frame')
    for y in (12.3,13,13.7):box('Cooling fin',[x,y,3.8],[x+.25,y+.25,12.2],'trim')
box('Rear service pad',[4.6,4.6,15.65],[11.4,11.4,15.9],'frame')
for y in (.1,15.65):box('Vertical service pad',[4.6,y,4.6],[11.4,y+.25,11.4],'frame')
textures={role:source['textures'][role] for role in roles};textures['particle']=source['textures']['body']
def model(es):return {'parent':'minecraft:block/block','render_type':'minecraft:cutout','textures':textures,'elements':es}
# The moving tool is authored in the same scene and exported separately for the renderer.
elements=[]
box('Telescopic piston rod',[7.6,9.45,5.6],[8.4,14,6.4],'steel')
box('Moving tool carrier',[4.25,8.35,2.7],[11.75,9.45,7],'trim')
box('Carrier face',[4.65,8.55,2.5],[11.35,9.2,2.69],'frame')
box('Tool holder',[6.1,7.75,3.3],[9.9,8.35,6.9],'frame')
box('Upper die',[6.65,7.15,3.65],[9.35,7.75,6.6],'steel')
for x in (5,10.55):box('Carrier bolt',[x,8.7,2.35],[x+.45,9.1,2.5],'bolt')
moving=elements
for name,es in [('forming_press',body),('forming_press_active',body),('forming_press_tool',moving),('forming_press_inventory',body+moving)]:
    (A/f'models/block/{name}.json').write_text(json.dumps(model(es),indent=2)+'\n')
(A/'models/item/forming_press.json').write_text('{"parent":"domesurvival:block/forming_press_inventory"}\n')
# Preview represents geometry exported to the game; the tool is at its idle height.
scene=bpy.context.scene;scene.render.engine='CYCLES';scene.cycles.samples=48;scene.cycles.use_denoising=True
scene.render.resolution_x=1000;scene.render.resolution_y=1000;scene.render.resolution_percentage=100
scene.world=bpy.data.worlds.new('Press studio');scene.world.color=(.15,.15,.15)
bpy.ops.object.camera_add(location=(23,36,23));camera=bpy.context.object
camera.rotation_euler=(Vector((0,0,0))-camera.location).to_track_quat('-Z','Y').to_euler();camera.data.type='ORTHO';camera.data.ortho_scale=25;scene.camera=camera
for pos,power,size in [((8,18,30),1600,18),((-18,10,10),1100,14)]:
    bpy.ops.object.light_add(type='AREA',location=pos);light=bpy.context.object;light.data.energy=power;light.data.shape='DISK';light.data.size=size;light.rotation_euler=(-light.location).to_track_quat('-Z','Y').to_euler()
scene.render.film_transparent=True;scene.render.image_settings.color_mode='RGBA';scene.render.filepath=str(OUT/'forming_press.png')
bpy.ops.render.render(write_still=True);bpy.ops.wm.save_as_mainfile(filepath=str(OUT/'forming_press.blend'))
print('FORMING_PRESS_MODEL_COMPLETE',len(body),len(moving))
