"""Render six matching flat inventory items; no cuboid/block model is exported."""
from pathlib import Path
import json
source=Path(__file__).with_name('create_coal_generator_gui.py')
helpers=source.read_text().split('main=setup(')[0]
helpers=helpers.replace('gui/coal_generator_v2','item/modules_v2').replace('blender/coal_generator_gui','blender/module_items')
exec(compile(helpers,str(source),'exec'))
palette={'efficiency':'77BA88','overdrive':'E5A35E','buffer':'73BBD1','automation':'B896D6','emergency_protection':'D37B76','communication':'7F9FD6'}
def line(name,a,b,width,mat='accent'):
    x,y=a;xx,yy=b
    obj=box(name,(x+xx)/2-width/2,(y+yy)/2-math.hypot(xx-x,yy-y)/2,width,math.hypot(xx-x,yy-y),4.7,mat,.25,.25)
    obj.rotation_euler.z=math.atan2(xx-x,yy-y)
for kind,color in palette.items():
    scene=setup(kind,32,32);scene.render.resolution_x=128;scene.render.resolution_y=128
    scene.cycles.samples=32;scene.view_settings.exposure=-.6
    M['accent']=material(kind+' enamel',color,.25)
    box('Graphite cartridge',4,2,24,25,2,'case',1.1,2)
    box('Steel bevel',4.8,2.8,22.4,23.4,2.5,'rim',.8,1)
    box('Inset ceramic board',6.1,4.1,19.8,20.8,3,'black',.65,1)
    for i in range(6):box('Brass contact',6.4+i*3.3,26,2.1,4,2,'brass',.15,.8)
    for x in (6.4,24):
        for y in (4.4,22.7):box('Captive fastener',x,y,1.4,1.4,3.5,'steel',.35,.5)
    box('Identity enamel',10,5.8,12,1.5,3.7,'accent',.3,.4)
    for y in (10,14,18):
        box('Circuit trace',7.3,y,2.4,.5,3.3,'brass',.1,.2)
        box('Circuit trace',22.3,y,2.4,.5,3.3,'brass',.1,.2)
    box('Symbol socket',9.5,8.5,13,13,3.7,'panel',.6,.5)
    if kind=='buffer':
        for x in (11,16.7):
            box('Energy cell',x,11,4.3,8,4.4,'steel',.7,.6)
            box('Cell inset',x+.6,12,3.1,5.8,4.6,'accent',.35,.2)
    elif kind=='overdrive':
        for y in (12,17):line('Speed chevron',(11,y),(16,y-3),1.6);line('Speed chevron',(16,y-3),(21,y),1.6)
    elif kind=='efficiency':
        line('Efficiency check',(11,15),(14.4,18),2);line('Efficiency check',(14.4,18),(21,11),2)
    elif kind=='automation':
        for x,y in [(11,11),(18,11),(14.5,17)]:box('Process node',x,y,3,3,4.6,'accent',.35,.3)
        line('Routing',(12.5,12.5),(19.5,12.5),.9,'brass');line('Routing',(19.5,12.5),(16,18),.9,'brass')
    elif kind=='emergency_protection':
        for a,b in [((11,10),(21,10)),((11,10),(11,16)),((21,10),(21,16)),((11,16),(16,20)),((21,16),(16,20))]:line('Shield',a,b,1.5)
        line('Safety stem',(16,12),(16,16),1.2,'steel')
    else:
        for i in range(3):box('Signal bar',11+i*3.8,17-i*3,2.3,3+i*3,4.6,'accent',.35,.3)
    render(scene,kind+'_module')
    model={'parent':'minecraft:item/generated','textures':{'layer0':'domesurvival:item/modules_v2/'+kind+'_module'}}
    (ROOT/'src/main/resources/assets/domesurvival/models/item'/f'{kind}_module.json').write_text(json.dumps(model,indent=2)+'\n')
bpy.ops.wm.save_as_mainfile(filepath=str(SOURCE/'module_items.blend'))
print('MODULE_ITEMS_COMPLETE')
