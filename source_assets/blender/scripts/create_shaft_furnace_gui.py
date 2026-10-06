"""Shaft furnace control panel: deepslate bricks with separated graphite instruments."""
from pathlib import Path
source=Path(__file__).with_name('create_coal_generator_gui.py')
helpers=source.read_text().split('main=setup(')[0]
helpers=helpers.replace('gui/coal_generator_v2','gui/shaft_furnace_v2').replace('blender/coal_generator_gui','blender/shaft_furnace_gui')
exec(compile(helpers,str(source),'exec'))
brick=bpy.data.images.load(str(SOURCE/'deepslate_bricks.png'));brick.pack()
def masonry(w,h):
    mat=bpy.data.materials.new('Original oven bricks '+str(w));mat.use_nodes=True
    nodes=mat.node_tree.nodes;links=mat.node_tree.links;bsdf=nodes.get('Principled BSDF')
    bsdf.inputs['Roughness'].default_value=.92
    coord=nodes.new('ShaderNodeTexCoord');mapping=nodes.new('ShaderNodeVectorMath');mapping.operation='MULTIPLY';mapping.inputs[1].default_value=(w/64,h/64,1)
    tex=nodes.new('ShaderNodeTexImage');tex.image=brick;tex.interpolation='Closest';tex.extension='REPEAT'
    links.new(coord.outputs['Generated'],mapping.inputs[0]);links.new(mapping.outputs['Vector'],tex.inputs['Vector']);links.new(tex.outputs['Color'],bsdf.inputs['Base Color'])
    bump=nodes.new('ShaderNodeBump');bump.inputs['Strength'].default_value=.12;bump.inputs['Distance'].default_value=.06
    links.new(tex.outputs['Color'],bump.inputs['Height']);links.new(bump.outputs['Normal'],bsdf.inputs['Normal'])
    M['brick']=mat
    box('Deepslate brick masonry',4,4,w-8,h-8,3.15,'brick',.4,.5)
def backing(w,h):
    base(w,h);masonry(w,h)
def strip(name,x,y,w,h):box(name,x,y,w,h,3.35,'case',.5,.3)

main=setup('Shaft furnace brick control panel',220,266);backing(220,266)
box('Nameplate gasket',8,6,156,16,3.5,'black',.7)
box('Blackened metal nameplate',9,7,154,14,3.8,'case',.5)
strip('Process heading',9,30,202,12)
strip('Cycle readout',78,58,92,17)
well(14,58,18,77,False);well(42,58,24,24,False);well(42,111,24,24,False);well(178,58,24,24,False);well(178,111,24,24,False);well(79,81,91,14,False)
box('Inventory label frame',10,143,72,14,3.5,'rim',.5,.3)
box('Inventory label inset',11,144,70,12,3.8,'case',.4,.3)
for row in range(3):
    for col in range(9):well(12+22*col,159+22*row,20,20,False)
for col in range(9):well(12+22*col,227,20,20,False)
for x in (7,213):
    for y in (28,143,257):screw(x,y)
render(main,'panel')
config=setup('Shaft furnace logical sides',204,111);backing(204,111)
strip('Heading',4,3,196,12)
box('Cube net recess',5,19,88,78,3.4,'black',.7)
box('Side legend',97,19,101,75,3.5,'well',.7)
for x,y in [(38,22),(14,46),(38,46),(62,46),(38,70),(62,70)]:well(x,y,20,20,False)
render(config,'configuration')
modules=setup('Shaft furnace fuel efficiency',204,111);backing(204,111)
strip('Heading',4,3,196,15)
box('Module socket panel',5,33,38,35,3.4,'black',.6);well(10,38,24,24,False)
box('Module specifications',47,22,150,72,3.5,'well',.6)
render(modules,'modules')
card=setup('JEI coke oven',180,128);backing(180,128)
box('Recipe title',7,5,166,17,3.6,'case',.5)
well(13,32,24,24,False);well(13,66,24,24,False);well(143,32,24,24,False);well(143,66,24,24,False);well(46,83,88,12,False)
strip('Recipe details',7,98,166,25)
render(card,'jei')
bpy.context.window.scene=main;bpy.ops.wm.save_as_mainfile(filepath=str(SOURCE/'shaft_furnace_gui.blend'))
print('SHAFT_FURNACE_GUI_COMPLETE',flush=True)
