"""Original press panel, sharing the approved generator's Blender material helpers."""
from pathlib import Path
source=Path(__file__).with_name('create_coal_generator_gui.py')
helpers=source.read_text().split("main=setup(")[0]
helpers=helpers.replace("gui/coal_generator_v2", "gui/forming_press_v2").replace("blender/coal_generator_gui", "blender/forming_press_gui")
exec(compile(helpers,str(source),'exec'))
main=setup('Metalworking control panel',220,266);base(220,266)
box('Nameplate gasket',8,6,156,16,3.5,'black',.7)
box('Brass nameplate',9,7,154,14,3.8,'brass',.5)
box('Instruments surround',8,24,204,112,3.3,'black',.7)
box('Instrument face',9,25,202,110,3.5,'case',.7)
well(14,37,18,53);well(42,38,166,15)
well(42,68,24,24,False);well(178,68,24,24,False)
well(79,73,91,14,False)
for i in range(5):well(24+36*i,108,30,26,False)
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
modules=setup('Module service insert',204,111)
box('Module inset',0,0,204,111,3.5,'case',.65)
box('Module socket panel',5,23,38,65,3.55,'black',.6)
well(10,28,24,24,False);well(10,58,24,24,False)
box('Module specifications',46,22,151,72,3.8,'well',.6)
render(modules,'modules')
# Reuse the actual approved control artwork; do not render near-identical variants.
import shutil
for name in ['widgets.png','widgets.png.mcmeta']:
    shutil.copyfile(ROOT/'src/main/resources/assets/domesurvival/textures/gui/coal_generator_v2'/name,OUT/name)
bpy.context.window.scene=main
bpy.ops.wm.save_as_mainfile(filepath=str(SOURCE/'forming_press_gui.blend'))
print('FORMING_PRESS_GUI_COMPLETE')
