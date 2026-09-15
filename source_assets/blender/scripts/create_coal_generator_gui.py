"""Render an original graphite control panel in Blender. Run with blender -b -P.

Coordinates are Minecraft GUI pixels; the baked artwork is four times that size.
Item wells retain the existing menu coordinates. No external mod artwork is used.
"""
import bpy, math, sys
from pathlib import Path
from mathutils import Vector

ROOT = Path(__file__).resolve().parents[3]
OUT = ROOT / 'src/main/resources/assets/domesurvival/textures/gui/coal_generator_v2'
SOURCE = ROOT / 'source_assets/blender/coal_generator_gui'
OUT.mkdir(parents=True, exist_ok=True)
SOURCE.mkdir(parents=True, exist_ok=True)
bpy.ops.wm.read_factory_settings(use_empty=True)
bpy.context.preferences.filepaths.save_version = 0

def linear(v):
    return v/12.92 if v <= .04045 else ((v+.055)/1.055)**2.4

def material(name, color, metal=.5):
    mat = bpy.data.materials.new(name)
    mat.use_nodes = True
    node = mat.node_tree.nodes.get('Principled BSDF')
    rgb = [linear(int(color[i:i+2],16)/255) for i in (0,2,4)]
    node.inputs['Base Color'].default_value = (*rgb,1)
    node.inputs['Metallic'].default_value = metal
    node.inputs['Roughness'].default_value = .48
    # Fine pressed-metal grain, much quieter than pixel noise.
    noise=mat.node_tree.nodes.new('ShaderNodeTexNoise')
    noise.inputs['Scale'].default_value=185
    bump=mat.node_tree.nodes.new('ShaderNodeBump')
    bump.inputs['Strength'].default_value=.12
    bump.inputs['Distance'].default_value=.025
    mat.node_tree.links.new(noise.outputs['Fac'],bump.inputs['Height'])
    mat.node_tree.links.new(bump.outputs['Normal'],node.inputs['Normal'])
    return mat

M={k:material(k,v,m) for k,v,m in [
    ('case','1C2022',.5),('panel','272D30',.5),('rim','4D595E',.7),
    ('steel','899797',.8),('black','111518',.15),('well','1B2326',.15),
    ('brass','A98C58',.65),('input','528FAC',.4),('output','C2A264',.4),
    ('amber','E6AF5C',.3),('orange','D88643',.3),('muted','455158',.5)]}

def box(name,x,y,w,h,top,mat,bevel=.4,depth=1):
    bpy.ops.mesh.primitive_cube_add(size=1,location=(x+w/2,H-y-h/2,top-depth/2))
    obj=bpy.context.object;obj.name=name;obj.dimensions=(w,h,depth)
    bpy.ops.object.transform_apply(location=False,rotation=False,scale=True)
    obj.data.materials.append(M[mat])
    if bevel:
        mod=obj.modifiers.new('Machined edge','BEVEL');mod.width=bevel;mod.segments=3
        obj.modifiers.new('Weighted normals','WEIGHTED_NORMAL')
    return obj

def screw(x,y):
    bpy.ops.mesh.primitive_cylinder_add(vertices=24,radius=1.15,depth=.6,location=(x,H-y,4.1))
    obj=bpy.context.object;obj.name='Recessed fastener';obj.data.materials.append(M['steel'])
    bevel=obj.modifiers.new('Head chamfer','BEVEL');bevel.width=.16;bevel.segments=3
    box('Screw slot',x-.7,y-.15,1.4,.3,4.42,'black',.08,.04)

def well(x,y,w,h,lip=True):
    box('Socket gasket',x,y,w,h,3.25,'black',.55)
    box('Socket bevel',x+.7,y+.7,w-1.4,h-1.4,3.6,'rim',.45)
    box('Recessed field',x+2,y+2,w-4,h-4,3.64,'well',.3,.4)
    if lip: box('Lower lip',x+2,y+h-1.4,w-4,.65,3.85,'muted',.18,.4)

def base(w,h):
    box('Rubber perimeter',.2,.2,w-.4,h-.4,1.5,'black',1.8,3)
    box('Graphite shell',1.2,1.2,w-2.4,h-2.4,3,'case',1.4,3)
    box('Inset steel face',4,4,w-8,h-8,3.1,'panel',.8,1)

def setup(name,w,h):
    global H
    H=h
    scene=bpy.data.scenes.new(name);bpy.context.window.scene=scene
    scene.render.engine='CYCLES';scene.cycles.samples=40;scene.cycles.use_denoising=True
    scene.render.resolution_x=w*4;scene.render.resolution_y=h*4;scene.render.resolution_percentage=100
    scene.render.image_settings.file_format='PNG';scene.render.image_settings.color_mode='RGBA'
    scene.render.film_transparent=True
    scene.view_settings.view_transform='Standard';scene.view_settings.look='None'
    scene.view_settings.exposure=-.6
    world=bpy.data.worlds.new(name+' soft environment');scene.world=world;world.use_nodes=True
    world.node_tree.nodes.get('Background').inputs['Color'].default_value=(.7,.78,.83,1)
    world.node_tree.nodes.get('Background').inputs['Strength'].default_value=.65
    bpy.ops.object.camera_add(location=(w/2,h/2,400));cam=bpy.context.object
    cam.data.type='ORTHO';cam.data.ortho_scale=h;scene.camera=cam
    # Blender's orthographic scale is the width when the output is landscape.
    if w>h:cam.data.ortho_scale=w
    for name,pos,power,size in [('Key',(-60,h+100,210),1250000,200),('Fill',(w+140,-80,150),400000,160)]:
        bpy.ops.object.light_add(type='AREA',location=pos);lamp=bpy.context.object
        lamp.name=name;lamp.data.energy=power;lamp.data.shape='DISK';lamp.data.size=size
        lamp.rotation_euler=(Vector((w/2,h/2,0))-lamp.location).to_track_quat('-Z','Y').to_euler()
    return scene

def render(scene,name):
    if '--only' in sys.argv and name != sys.argv[sys.argv.index('--only')+1]: return
    scene.render.filepath=str(OUT/(name+'.png'));bpy.ops.render.render(write_still=True)
    (OUT/(name+'.png.mcmeta')).write_text('{"texture":{"blur":true,"clamp":true}}\n')

main=setup('Main instrument panel',220,266);base(220,266)
box('Nameplate gasket',8,6,156,16,3.5,'black',.7)
box('Brass nameplate',9,7,154,14,3.8,'brass',.5)
box('Instruments surround',8,24,204,112,3.3,'black',.7)
box('Instrument face',9,25,202,110,3.5,'case',.7)
well(14,37,18,53);well(42,38,166,15)
well(14,108,153,14);well(178,102,24,24)
# Ticks are baked beside, never over, the readouts.
for i in range(11):box('Energy scale',33.2,40+i*4.4,3 if i%5==0 else 1.4,.35,3.9,'rim',.1,.1)
for i in range(16):box('Burn scale',17+i*9.5,123, .35,1.3 if i%5==0 else .65,3.9,'rim',.1,.1)
box('Inventory separation',10,152,200,.65,3.6,'rim',.2,.3)
for row in range(3):
    for col in range(9):well(11+22*col,158+22*row,22,22)
for col in range(9):well(11+22*col,226,22,22)
for x in (7,213):
    for y in (28,143,257):screw(x,y)
for x in (13,184):
    for i in range(5):box('Lower cooling vent',x+i*4,254,2,5,3.4,'black',.5,.4)
render(main,'panel')

config=setup('Side configuration insert',204,100)
box('Configuration inset',0,0,204,100,3.5,'case',.65)
box('Cube net recess',5,18,88,79,3.8,'black',.7)
box('Legend recess',97,18,101,52,3.8,'well',.7)
box('Legend separator',101,44,92,.4,4,'rim',.1,.1)
for x,y in [(38,22),(14,46),(38,46),(62,46),(38,70),(62,70)]:well(x,y,20,20)
# Fuel slot at exactly the same position as in the main panel (relative x=8,y=25).
well(170,77,24,23);box('Blue fuel inlet',170,75,24,1,3.9,'input',.2,.2)
render(config,'configuration')

modules=setup('Module service insert',204,100)
box('Module inset',0,0,204,100,3.5,'case',.65)
box('Module socket panel',5,23,38,35,3.55,'black',.6)
well(10,28,24,24,lip=False)
box('Module specifications',46,22,151,37,3.8,'well',.6)
well(170,77,24,23)
render(modules,'modules')

widgets=setup('Reusable instrument widgets',128,64)
# Atlas positions in logical pixels: gear 0,0; off/input/output side tiles at y=24;
# full energy gauge x=64,y=0,w=12,h=47; fuel strip x=0,y=48,w=64,h=8.
well(0,0,20,20)
bpy.ops.mesh.primitive_torus_add(major_radius=4.1,minor_radius=1.2,major_segments=48,minor_segments=12,location=(10,H-10,4.8))
bpy.context.object.data.materials.append(M['steel'])
for i in range(8):
    a=i*math.pi/4;obj=box('Gear tooth',9+5*math.sin(a),9+5*math.cos(a),2,2,5,'steel',.3)
    obj.rotation_euler.z=-a
well(88,0,20,20)
box('Upgrade chip',93,5,10,10,4.5,'brass',.7)
box('Upgrade chip inset',95,7,6,6,4.8,'case',.45)
for i in range(4):
    for x in (91,103):box('Chip pin',x,6+i*2,2,1,4.2,'steel',.15,.3)
for j,mat in enumerate(['muted','input','output']):
    well(j*20,24,20,20);box('Port indicator',j*20+3,40,14,1.4,4.2,mat,.3,.4)
for x in range(12):
    box('Energy glass filament',64+x,0,1,47,4.2,'amber' if x<7 else 'brass',.14,.3)
for i in range(1,10):box('Energy divisions',64,i*4.7,12,.45,4.6,'black',.1,.2)
box('Fuel copper channel',0,48,64,8,4.1,'orange',1,.8)
box('Fuel reflected light',1,49,62,1.1,4.4,'amber',.4,.2)
render(widgets,'widgets')
bpy.context.window.scene=main
bpy.ops.wm.save_as_mainfile(filepath=str(SOURCE/'coal_generator_gui.blend'))
print('GUI_RENDER_COMPLETE',OUT)
