"""Copper-accented control panel and matching recipe cards, authored with Blender."""
from pathlib import Path
source=Path(__file__).with_name('create_coal_generator_gui.py')
helpers=source.read_text().split('main=setup(')[0]
helpers=helpers.replace('gui/coal_generator_v2','gui/copper_furnace_v2').replace('blender/coal_generator_gui','blender/copper_furnace_gui')
exec(compile(helpers,str(source),'exec'))
M['copper']=material('Brushed copper','A97553',.5)
main=setup('Copper furnace control panel',220,266);base(220,266)
box('Nameplate gasket',8,6,156,16,3.5,'black',.7)
box('Copper nameplate',9,7,154,14,3.8,'copper',.5)
box('Instruments surround',8,24,204,112,3.3,'black',.7)
box('Instrument face',9,25,202,110,3.5,'case',.7)
well(14,51,18,73,False);well(42,51,24,24,False);well(42,101,24,24,False);well(178,75,24,24,False)
well(79,71,91,14,False)
for row in range(3):
    for col in range(9):well(11+22*col,158+22*row,22,22)
for col in range(9):well(11+22*col,226,22,22)
for x in (7,213):
    for y in (28,143,257):screw(x,y)
render(main,'panel')
config=setup('Side configuration',204,111)
box('Configuration inset',0,0,204,111,3.5,'case',.65)
box('Cube net recess',5,18,88,79,3.8,'black',.7)
box('Legend recess',97,18,101,76,3.8,'well',.7)
for x,y in [(38,22),(14,46),(38,46),(62,46),(38,70),(62,70)]:well(x,y,20,20)
render(config,'configuration')
modules=setup('Fuel economy module',204,111)
box('Module inset',0,0,204,111,3.5,'case',.65)
box('Module socket panel',5,33,38,35,3.55,'black',.6);well(10,38,24,24,False)
box('Specifications',46,22,151,72,3.8,'well',.6)
render(modules,'modules')
# Three original recipe cards use the exact material and slot system of the GUIs.
for kind in ('forming','copper','coal'):
    card=setup('JEI / '+kind,180,128);base(180,128)
    box('Recipe title recess',7,5,166,17,3.6,'black',.5)
    box('Recipe badge',8,6,164,15,3.8,'copper' if kind=='copper' else 'brass',.45)
    if kind=='forming':
        well(13,44,24,24,False);well(143,44,24,24,False);well(46,83,88,12,False)
    elif kind=='copper':
        well(13,32,24,24,False);well(13,66,24,24,False);well(143,49,24,24,False);well(46,83,88,12,False)
    else:
        well(13,44,24,24,False);well(143,35,18,47,False);well(46,83,88,12,False)
    render(card,'jei_'+kind)
bpy.context.window.scene=main;bpy.ops.wm.save_as_mainfile(filepath=str(SOURCE/'copper_furnace_gui.blend'))
print('COPPER_FURNACE_GUI_COMPLETE',flush=True)
