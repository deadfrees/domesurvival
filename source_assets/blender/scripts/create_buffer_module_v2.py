"""Author the generator's buffer module with Blender; export Minecraft cuboids.

Reuses the approved generator satin atlas. No changes to other module families.
"""
from pathlib import Path
import bpy, json, sys, math
from mathutils import Vector
sys.path.insert(0,str(Path(__file__).parent))
import fluid_pipe_helpers as g
ROOT=Path(__file__).resolve().parents[3]
A=ROOT/'src/main/resources/assets/domesurvival'
OUT=ROOT/'source_assets/blender/buffer_module'
OUT.mkdir(parents=True,exist_ok=True)
bpy.ops.wm.read_factory_settings(use_empty=True)
bpy.context.preferences.filepaths.save_version=0
source=json.loads((A/'models/block/coal_generator.json').read_text())
uvs={}
port=json.loads((A/'models/block/coal_generator_input_port_north.json').read_text())
for e in source['elements']+port['elements']:
    for f in e['faces'].values():
        role=f['texture'][1:];u=f['uv'];area=(u[2]-u[0])*(u[3]-u[1])
        if role not in uvs or area>uvs[role][0]:uvs[role]=(area,u)
roles=['body','frame','trim','steel','black','brass','bolt','input']
mats={}
for role in roles:
    mat=g.material('Buffer / '+role,(.3,.3,.3))
    tex=mat.node_tree.nodes.new('ShaderNodeTexImage')
    tex.image=bpy.data.images.load(str(A/'textures/block/coal_generator_v2/satin_atlas.png'),check_existing=True)
    tex.image.pack();tex.interpolation='Closest'
    mat.node_tree.links.new(tex.outputs['Color'],mat.node_tree.nodes.get('Principled BSDF').inputs['Base Color'])
    mats[role]=mat
elements=[]
def box(name,lo,hi,role):
    element={'name':name,'from':lo,'to':hi,'faces':{d:{'texture':'#'+role,'uv':uvs[role][1]} for d in g.MC_FACES}}
    spec={'name':name,'minimum':[lo[0]-8,8-hi[2],lo[1]-8],'maximum':[hi[0]-8,8-lo[2],hi[1]-8],'faces':list(g.MC_FACES.values())}
    obj=g.mesh_box(spec,name,mats[role]);obj['minecraft_material']=role
    layer=obj.data.uv_layers.new(name='Minecraft UV')
    normals={(1,0,0):'east',(-1,0,0):'west',(0,1,0):'up',(0,-1,0):'down',(0,0,1):'south',(0,0,-1):'north'}
    for poly in obj.data.polygons:
        n=poly.normal;face=normals[(round(n.x),round(n.z),round(-n.y))]
        coords=[]
        for loop in poly.loop_indices:
            v=obj.data.vertices[obj.data.loops[loop].vertex_index].co
            coords.append(g.project((v.x+8,v.z+8,8-v.y),face))
        x0,x1=min(c[0] for c in coords),max(c[0] for c in coords)
        y0,y1=min(c[1] for c in coords),max(c[1] for c in coords)
        a,b,c,d=uvs[role][1]
        for loop,(x,y) in zip(poly.loop_indices,coords):
            layer.data[loop].uv=((a+(c-a)*(x-x0)/(x1-x0))/16,1-(b+(d-b)*(y-y0)/(y1-y0))/16)
    # Export bounds from the authored mesh, keeping the preview and game geometry identical.
    verts=[v.co for v in obj.data.vertices]
    element['from']=[min(v.x for v in verts)+8,min(v.z for v in verts)+8,8-max(v.y for v in verts)]
    element['to']=[max(v.x for v in verts)+8,max(v.z for v in verts)+8,8-min(v.y for v in verts)]
    elements.append(element)

box('Graphite cartridge backplate',[2,2.4,7.8],[14,14,8.8],'frame')
box('Recessed circuit board',[2.6,3,7.45],[13.4,13.4,7.85],'black')
for x in (2,13.35):box('Protective side rail',[x,2.4,6.6],[x+.65,14,7.8],'trim')
for y in (2.4,13.35):box('End rail',[2.65,y,6.6],[13.35,y+.65,7.8],'trim')
for x in (4,8.6):
    for y in (4,8.4):
        box('Capacitor gasket',[x-.2,y-.2,6.6],[x+3.6,y+3.6,7.45],'black')
        box('Capacitor housing',[x,y,5.35],[x+3.4,y+3.4,7.4],'body')
        box('Inset capacitor lid',[x+.25,y+.25,5.12],[x+3.15,y+3.15,5.35],'steel')
        box('Insulated top',[x+.5,y+.5,5.04],[x+2.9,y+2.9,5.12],'frame')
        box('Capacitor polarity stripe',[x+.75,y+1.45,5],[x+2.65,y+1.8,5.04],'input')
for i in range(6):box('Brass edge contact',[3.2+i*1.65,1,7.6],[4.2+i*1.65,2.7,8.65],'brass')
for x in (2.15,13.45):
    for y in (2.6,13.45):box('Captive corner bolt',[x,y,6.4],[x+.4,y+.4,6.65],'bolt')
box('Capacity identification badge',[5.3,12.25,7.25],[10.7,12.95,7.45],'brass')
model={'parent':'minecraft:block/block','gui_light':'side','ambientocclusion':True,
       'textures':{r:source['textures'][r] for r in roles},'elements':elements,
       'display':{'gui':{'rotation':[25,205,0],'translation':[0,0,0],'scale':[1,1,1]},
                  'ground':{'rotation':[0,0,0],'translation':[0,2,0],'scale':[.65,.65,.65]},
                  'fixed':{'rotation':[0,180,0],'translation':[0,0,0],'scale':[1,1,1]},
                  'thirdperson_righthand':{'rotation':[0,0,0],'translation':[0,2,0],'scale':[.6,.6,.6]},
                  'firstperson_righthand':{'rotation':[0,155,0],'translation':[0,2,0],'scale':[.8,.8,.8]}}}
model['textures']['particle']=source['textures']['body']
(A/'models/item/buffer_module.json').write_text(json.dumps(model,indent=2)+'\n')
scene=bpy.context.scene;scene.render.engine='CYCLES';scene.cycles.samples=48;scene.cycles.use_denoising=True
scene.render.resolution_x=700;scene.render.resolution_y=700;scene.render.resolution_percentage=100
scene.world=bpy.data.worlds.new('Buffer studio');scene.world.color=(.15,.15,.15)
bpy.ops.object.camera_add(location=(17,35,22));camera=bpy.context.object
camera.rotation_euler=(Vector((0,0,0))-camera.location).to_track_quat('-Z','Y').to_euler()
camera.data.type='ORTHO';camera.data.ortho_scale=20;scene.camera=camera
for pos,power,size in [((8,18,30),1400,18),((-18,10,10),900,14)]:
    bpy.ops.object.light_add(type='AREA',location=pos);light=bpy.context.object
    light.data.energy=power;light.data.shape='DISK';light.data.size=size
    light.rotation_euler=(-light.location).to_track_quat('-Z','Y').to_euler()
scene.render.film_transparent=True;scene.render.image_settings.color_mode='RGBA'
scene.render.filepath=str(OUT/'buffer_module.png');bpy.ops.render.render(write_still=True)
bpy.ops.wm.save_as_mainfile(filepath=str(OUT/'buffer_module.blend'))
print('BUFFER_MODULE_COMPLETE',len(elements),'cuboids')
