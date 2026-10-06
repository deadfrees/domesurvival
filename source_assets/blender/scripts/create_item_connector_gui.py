"""Bake the transport-connector panel with the approved machine GUI materials."""
from pathlib import Path
source=Path(__file__).with_name('create_coal_generator_gui.py')
helpers=source.read_text().split('main=setup(')[0]
helpers=helpers.replace('gui/coal_generator_v2','gui/item_connector_v2').replace('blender/coal_generator_gui','blender/item_connector_gui')
exec(compile(helpers,str(source),'exec'))
main=setup('Transport connector',236,176);base(236,176)
box('Title gasket',8,6,220,17,3.5,'black',.7)
box('Title plate',9,7,218,15,3.8,'brass',.5)
box('Connection status',10,29,216,51,3.5,'well',.7)
well(16,38,26,26,False)
for x in (12,85,158):well(x,90,66,28,False)
box('Mode description',10,127,216,37,3.5,'well',.7)
for x in (6,230):
    for y in (27,123,169):screw(x,y)
render(main,'panel')
bpy.ops.wm.save_as_mainfile(filepath=str(SOURCE/'item_connector_gui.blend'))
print('ITEM_CONNECTOR_GUI_COMPLETE',flush=True)
