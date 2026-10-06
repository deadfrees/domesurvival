"""A simple timber workbench panel, using the sieve's vanilla materials and original slots."""
from pathlib import Path
import zipfile
source=Path(__file__).with_name('create_coal_generator_gui.py')
helpers=source.read_text(encoding='utf-8').split('main=setup(')[0]
helpers=helpers.replace('gui/coal_generator_v2','gui/sand_sieve_v2').replace('blender/coal_generator_gui','blender/sand_sieve_gui')
exec(compile(helpers,str(source),'exec'))
jar=Path.home()/'.gradle/caches/forge_gradle/minecraft_repo/versions/1.20.1/client.jar'
(SOURCE/'textures').mkdir(exist_ok=True)
with zipfile.ZipFile(jar) as z:
    for name in ('spruce_planks','stone_bricks'):
        (SOURCE/'textures'/f'{name}.png').write_bytes(z.read(f'assets/minecraft/textures/block/{name}.png'))

M['case']=material('Warm charcoal pocket','29261F',.05)
M['well']=material('Dark wooden recess','25251F',.03)
M['rim']=material('Dull iron socket edges','686C66',.25)
M['black']=material('Frame joints','24221C',.0)
M['copper']=material('Aged copper trim','AA7050',.30)
M['label']=material('Plain pale title plaque','BCAB86',.0)
M['woodedge']=material('Spruce edge','57422A',.0)

def textured(name,w,h):
    mat=bpy.data.materials.new(name+' vanilla material');mat.use_nodes=True
    bsdf=mat.node_tree.nodes.get('Principled BSDF');bsdf.inputs['Roughness'].default_value=.95
    image=bpy.data.images.load(str(SOURCE/'textures'/f'{name}.png'));image.pack()
    coord=mat.node_tree.nodes.new('ShaderNodeTexCoord')
    scale=mat.node_tree.nodes.new('ShaderNodeVectorMath');scale.operation='MULTIPLY';scale.inputs[1].default_value=(w/48,h/48,1)
    tex=mat.node_tree.nodes.new('ShaderNodeTexImage');tex.image=image;tex.interpolation='Closest';tex.extension='REPEAT'
    mat.node_tree.links.new(coord.outputs['Generated'],scale.inputs[0]);mat.node_tree.links.new(scale.outputs['Vector'],tex.inputs['Vector']);mat.node_tree.links.new(tex.outputs['Color'],bsdf.inputs['Base Color'])
    return mat

main=setup('Timber sieve workbench',300,227)
M['wood']=textured('spruce_planks',300,227);M['stone']=textured('stone_bricks',300,16)
box('Dark frame joint',0,0,300,227,1.5,'black',1.2,3)
box('Timber outer frame',1,1,298,225,3,'woodedge',.8,2)
box('Spruce board facing',4,4,292,219,3.1,'wood',.3,1)
box('Stone footing',4,214,292,9,3.15,'stone',.25,.7)
box('Title rim',45,6,210,18,3.4,'copper',.35,.5)
box('Plain title plaque',46,7,208,16,3.7,'label',.3,.4)
box('Work tray border',9,30,282,89,3.2,'woodedge',.4,.5)
box('Work tray inset',11,32,278,85,3.3,'case',.3,.4)
well(20,52,22,54,False)
for y in range(57,104,9):box('Water volume mark',43,y,2,.4,3.9,'rim',.05,.1)
for x in (62,100,202,226,250):well(x,58,22,22,False)
well(58,91,210,8,False)
box('Inventory label rim',48,121,79,12,3.6,'copper',.3,.3)
box('Inventory label field',49,122,77,10,3.9,'case',.25,.3)
for row in range(3):
    for col in range(9):well(48+22*col,134+22*row,22,22,False)
for col in range(9):well(48+22*col,200,22,22,False)
for x in (7,293):
    for y in (8,113,219):screw(x,y)
render(main,'panel')
bpy.ops.wm.save_as_mainfile(filepath=str(SOURCE/'sand_sieve_gui.blend'))
print('SAND_SIEVE_GUI_COMPLETE',flush=True)
