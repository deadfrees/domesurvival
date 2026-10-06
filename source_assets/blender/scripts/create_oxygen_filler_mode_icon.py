"""Two item-like oxygen mode pictograms; shared GUI frames are not regenerated."""
from pathlib import Path
source=Path(__file__).with_name('create_coal_generator_gui.py')
s=source.read_text().split('main=setup(')[0]
s=s.replace('gui/coal_generator_v2','gui/oxygen_filler_v2').replace('blender/coal_generator_gui','blender/oxygen_filler_icons')
exec(compile(s,str(source),'exec'))
M['silver']=material('Brushed oxygen equipment silver','879398',.65)
M['cyan']=material('Oxygen cyan enamel','31B9D9',.25)
M['glass']=material('Blue visor glass','176680',.35)
M['shine']=material('Visor highlight','70C6CF',.2)
# Angular plates and stepped silhouettes remain readable at the actual 16 x 13 GUI size.
def plate(name,points,z,mat,thickness=.35):
    verts=[(x,H-y,z) for x,y in points]
    mesh=bpy.data.meshes.new(name);mesh.from_pydata(verts,[],[list(range(len(verts)))]);mesh.update()
    obj=bpy.data.objects.new(name,mesh);bpy.context.collection.objects.link(obj);obj.data.materials.append(M[mat])
    solid=obj.modifiers.new('Plate thickness','SOLIDIFY');solid.thickness=thickness
    return obj

tank=setup('Single oxygen cylinder filling',20,16);tank.view_settings.exposure=-.6
# Cylinder occupies the full height, with hose and filling station on its right.
box('Cylinder shadow',4,4.4,6.8,10.8,3,'black',.6,1.2)
plate('Cylinder silver shoulder',[(4.1,5.4),(4.9,3.9),(6,3.2),(8.8,3.2),(10.1,4.1),(10.6,5.4)],4,'silver')
box('Faceted bottle',4.2,5.1,6.1,9.4,4,'steel',.6,1.4)
box('Bottle lit face',5,5.1,3.5,9.3,4.2,'silver',.25,.35)
box('Bottle shade facet',9,5.1,1.2,9.3,4.25,'rim',.15,.3)
for y in (5.3,12.5):box('Cyan cylinder band',4.2,y,6.1,.8,4.5,'cyan',.15,.25)
box('Neck',6.2,2,2.4,1.7,4,'steel',.2,.6)
box('Valve',5.7,1.2,3.5,.8,4.4,'silver',.15,.4)
box('Valve stem',7,0.7,.7,.9,4.3,'rim',.1,.4)
box('Cylinder bottom shoe',4,14,6.7,1.1,4.3,'rim',.2,.5)
# An oxygen emblem, not a word label, remains legible when reduced.
box('Oxygen emblem',6.1,7.4,2.4,3.5,4.6,'shine',.25,.15)
box('Oxygen emblem aperture',6.8,8.2,1,1.9,4.8,'steel',.15,.15)
box('Fill coupling body',15,2.2,2.7,4.3,4,'rim',.4,1)
box('Fill coupling face',14.5,2.8,1,3.1,4.5,'silver',.2,.35)
box('Hose upper elbow',16,5.8,1.2,5.6,3.4,'black',.2,.7)
box('Hose lower elbow',12.1,10.4,5.1,1.2,3.4,'rim',.2,.7)
for x,y in [(10,3),(11.6,3.3),(13,3.1)]:box('Incoming oxygen',x,y,.9,.9,4.5,'cyan',.06,.2)
render(tank,'mode_tank')
bpy.ops.wm.save_as_mainfile(filepath=str(SOURCE/'mode_tank.blend'))

mask=setup('Room breathing mask',20,16);mask.view_settings.exposure=-.6
plate('Mask graphite outer shell',[(2,4),(4,1.4),(11,1),(14.7,3),(15.3,8.5),(13.6,12.3),(10.5,14.7),(5,13.7),(2,10)],3,'black')
plate('Silver helmet rim',[(2,4.1),(4.2,1.5),(10.8,1.1),(14.6,3.2),(14.4,4.9),(11,3.4),(5,3.5),(3.5,5.3)],4,'silver')
plate('Blue visor',[(3.6,5),(5,3.9),(11.5,3.9),(13.5,5),(12.8,9),(10.3,10.1),(5,9),(3.7,7.4)],4.2,'glass')
plate('Cyan visor reflection',[(4.5,5),(6,4.4),(7.3,4.4),(6.3,8.5),(4.9,7.8)],4.4,'cyan')
plate('Visor glint',[(5.2,4.8),(5.8,4.5),(6.3,4.5),(5.7,7.1),(5.1,6.6)],4.5,'shine')
box('Left mask cheek',2.5,7.7,2.4,4.3,4.5,'steel',.4,.7)
box('Right mask cheek',12.2,8,2.4,4,4.5,'steel',.4,.7)
box('Respirator gasket',6,9,6.4,5.6,4.7,'black',.65,.8)
box('Respirator silver frame',6.7,9.5,5,4.7,5,'steel',.35,.5)
box('Breathing cartridge',7.4,10,3.6,3.7,5.3,'case',.25,.4)
for y in (10.4,11.5,12.6):box('Cyan filter slot',8,y,2.4,.5,5.6,'cyan',.05,.2)
box('Left hose',1.5,11,4.4,1.6,3.8,'rim',.4,.8)
box('Hose silver ring',3,10.8,.8,2,4.1,'silver',.1,.3)
# A compact air sparkle differentiates breathing from tank filling.
box('Air cross horizontal',16,5,3,.8,4,'shine',.05,.2)
box('Air cross vertical',17.1,3.9,.8,3,4,'shine',.05,.2)
box('Air particle',16,9,1.1,1.1,4,'cyan',.05,.2)
render(mask,'mode_vent')
bpy.ops.wm.save_as_mainfile(filepath=str(SOURCE/'mode_vent.blend'))

