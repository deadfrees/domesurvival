"""Render the exact exported Minecraft meshes under identical studio lighting."""
from pathlib import Path
script=Path(__file__).with_name('create_coal_generator_v2.py')
exec(compile(script.read_text().split("base=body();ports=")[0],str(script),'exec'))
out=ROOT/'dev/machine_graphite_review';out.mkdir(parents=True,exist_ok=True)
g.OUT=out;g.PREVIEW=out
g.reset_scene();g.camera_setup(1800,800,73)
for x,name in [(-23,'coal_generator_inventory'),(0,'forming_press_inventory'),(23,'water_purifier_inventory')]:
    m=read(MODEL/(name+'.json'))
    if not name.startswith('coal_'):
        for side in ('east','west','south','up'):
            p=read(MODEL/f'coal_generator_input_port_{side}.json')
            m['textures'].update(p['textures']);m['elements']+=p['elements']
    assembly(m,g.screen_point(x,0),g.ROTATIONS['south'])
g.save_render('machine_materials',True)
g.reset_scene();g.camera_setup(1000,1000,26)
m=read(MODEL/'water_purifier_inventory.json')
for side in ('east','west','south','up'):
    p=read(MODEL/f'coal_generator_input_port_{side}.json')
    m['textures'].update(p['textures']);m['elements']+=p['elements']
assembly(m)
g.save_render('purifier_service_sides',True)
