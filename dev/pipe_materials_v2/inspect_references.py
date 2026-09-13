from pathlib import Path
import hashlib, json, struct, zipfile
import bpy
from mathutils import Vector
ROOT=Path(__file__).resolve().parents[2]
OUT=ROOT/'dev/pipe_materials_v2'
REF=OUT/'references'
REF.mkdir(parents=True,exist_ok=True)
entries=[
('Mekanism-','assets/mekanism/textures/block/models/multipart/basic_universal_cable.png','Mekanism / universal cable'),
('Mekanism-','assets/mekanism/textures/block/models/multipart/basic_mechanical_pipe.png','Mekanism / mechanical pipe'),
('EnderIO-','assets/enderio/textures/block/conduit/energy_conduit.png','Ender IO / energy conduit'),
('EnderIO-','assets/enderio/textures/block/conduit/pressurized_fluid_conduit.png','Ender IO / pressure conduit'),
('thermal_dynamics-','assets/thermal/textures/block/ducts/energy_duct.png','Thermal / energy duct'),
('thermal_dynamics-','assets/thermal/textures/block/ducts/fluid_duct.png','Thermal / fluid duct'),
('thermal_dynamics-','assets/thermal/textures/block/ducts/fluid_duct_connector.png','Thermal / fluid connector'),
('ImmersiveEngineering-','assets/immersiveengineering/textures/block/metal_device/fluid_pipe.png','Immersive Engineering / pipe'),
]
bpy.ops.wm.read_factory_settings(use_empty=True)
scene=bpy.context.scene;scene.render.engine='CYCLES';scene.cycles.samples=1
scene.render.resolution_x=1600;scene.render.resolution_y=1000;scene.render.resolution_percentage=100
scene.view_settings.view_transform='Standard';scene.world=bpy.data.worlds.new('background');scene.world.color=(.06,.06,.06)
def emission(name,color):
 m=bpy.data.materials.new(name);m.use_nodes=True;n=m.node_tree.nodes;n.clear();o=n.new('ShaderNodeOutputMaterial');e=n.new('ShaderNodeEmission');e.inputs['Color'].default_value=(*color,1);m.node_tree.links.new(e.outputs[0],o.inputs[0]);return m,e
mat,_=emission('label',(.78,.83,.85))
manifest=[]
for i,(prefix,entry,label) in enumerate(entries):
 jar=next((ROOT/'run/mods').glob(prefix+'*.jar'))
 with zipfile.ZipFile(jar) as z:raw=z.read(entry)
 file=REF/(str(i)+'_'+Path(entry).name);file.write_bytes(raw)
 w,h=struct.unpack('>II',raw[16:24]);manifest.append({'jar':jar.name,'entry':entry,'png_dimensions':[w,h],'sha256':hashlib.sha256(raw).hexdigest()})
 x=(i%4-1.5)*4;y=(.5-i//4)*4.8
 bpy.ops.mesh.primitive_plane_add(size=3.5,location=(x,y,0));obj=bpy.context.object
 m,e=emission(label,(1,1,1));tex=m.node_tree.nodes.new('ShaderNodeTexImage');tex.image=bpy.data.images.load(str(file));tex.interpolation='Closest';m.node_tree.links.new(tex.outputs['Color'],e.inputs['Color']);obj.data.materials.append(m)
 bpy.ops.object.text_add(location=(x-1.75,y-2.1,.1));t=bpy.context.object;t.data.body=label;t.data.size=.18;t.data.materials.append(mat)
cam=bpy.data.cameras.new('front');ob=bpy.data.objects.new('front',cam);scene.collection.objects.link(ob);ob.location=(0,0,20);cam.type='ORTHO';cam.ortho_scale=16.8;scene.camera=ob
scene.render.filepath=str(REF/'reference_texture_study.png');bpy.ops.render.render(write_still=True)
(OUT/'reference_manifest.json').write_text(json.dumps(manifest,indent=2)+'\n')
print('REFERENCE_STUDY_READY')
