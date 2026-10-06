"""Bake press products to flat item sprites with Blender; no game mesh is exported.

Run: blender -b -t 6 -P source_assets/blender/scripts/create_press_products.py
Recipe results are the authoritative scope. --only <id> renders a single preview.
"""
import bpy
import json
import math
import sys
from pathlib import Path
from mathutils import Vector

ROOT = Path(__file__).resolve().parents[3]
ASSETS = ROOT / 'src/main/resources/assets/domesurvival'
OUT = ASSETS / 'textures/item/press_products'
SOURCE = ROOT / 'source_assets/blender/press_products'
OUT.mkdir(parents=True, exist_ok=True)
SOURCE.mkdir(parents=True, exist_ok=True)
bpy.ops.wm.read_factory_settings(use_empty=True)
bpy.context.preferences.filepaths.save_version = 0
PALETTE = {'copper':'B97549', 'tin':'B7C7CC', 'steel':'77848C',
           'nickel':'C0B597', 'silver':'D9E3E8', 'lead':'77788F',
           'goteium':'49B9B5', 'voltarium':'9582CE'}

def material(name, color, factor=1):
    mat = bpy.data.materials.new(name); mat.use_nodes = True
    node = mat.node_tree.nodes.get('Principled BSDF')
    rgb = [int(color[i:i+2],16)/255 for i in (0,2,4)]
    node.inputs['Base Color'].default_value = tuple(((v+.055)/1.055)**2.4*factor for v in rgb)+(1,)
    # Broad, matte facets: the result must read as Minecraft pixel art, not a photo.
    node.inputs['Metallic'].default_value = .30
    node.inputs['Roughness'].default_value = .65
    return mat

def finish(obj, mat, bevel=.025):
    obj.data.materials.append(mat)
    if bevel:
        mod=obj.modifiers.new('Cut edge','BEVEL'); mod.width=bevel; mod.segments=1
        obj.modifiers.new('Face normals','WEIGHTED_NORMAL')
    return obj

def box(name, pos, dims, mat, bevel=.025):
    bpy.ops.mesh.primitive_cube_add(size=1, location=pos)
    obj=bpy.context.object; obj.name=name; obj.dimensions=dims
    bpy.ops.object.transform_apply(location=False,rotation=False,scale=True)
    return finish(obj,mat,bevel)

def annulus(name, radius, inner, depth, mat, teeth=0, z=0):
    n=teeth*8 if teeth else 32
    vertices=[]
    for h,r in [(-depth/2,radius),(depth/2,radius),(-depth/2,inner),(depth/2,inner)]:
        for i in range(n):
            # Broad tooth crown, sloped flanks, and a rounded root.
            rr=r
            if teeth and r==radius: rr*=([.84,.84,.86,1,1,1,.86,.84][i%8])
            a=i*2*math.pi/n
            vertices.append((rr*math.cos(a),rr*math.sin(a),h))
    faces=[]
    for i in range(n):
        j=(i+1)%n
        faces.extend([(i,j,n+j,n+i),(2*n+j,2*n+i,3*n+i,3*n+j),
                      (n+i,n+j,3*n+j,3*n+i),(j,i,2*n+i,2*n+j)])
    mesh=bpy.data.meshes.new(name); mesh.from_pydata(vertices,[],faces); mesh.update()
    obj=bpy.data.objects.new(name,mesh); bpy.context.collection.objects.link(obj); obj.location.z=z
    return finish(obj,mat,.018)

def cylinder(name, a,b,radius,mat):
    a,b=Vector(a),Vector(b)
    bpy.ops.mesh.primitive_cylinder_add(vertices=12,radius=radius,depth=(b-a).length,location=(a+b)/2)
    obj=bpy.context.object; obj.name=name
    obj.rotation_euler=(b-a).to_track_quat('Z','Y').to_euler()
    return finish(obj,mat,.018)

def strand(name, points, radius,mat):
    curve=bpy.data.curves.new(name,'CURVE');curve.dimensions='3D'
    curve.bevel_depth=radius;curve.bevel_resolution=1
    spline=curve.splines.new('POLY');spline.points.add(len(points)-1)
    for p,co in zip(spline.points,points):p.co=(*co,1)
    obj=bpy.data.objects.new(name,curve);bpy.context.collection.objects.link(obj);obj.data.materials.append(mat)

def setup(name):
    scene=bpy.data.scenes.new(name);bpy.context.window.scene=scene
    scene.render.engine='CYCLES';scene.cycles.samples=64;scene.cycles.use_denoising=True
    scene.render.filter_size=.01
    # Supersample lighting, then bake onto a hard 32 px grid in pixel_finish.
    scene.render.resolution_x=128;scene.render.resolution_y=128;scene.render.resolution_percentage=100
    scene.render.image_settings.file_format='PNG';scene.render.image_settings.color_mode='RGBA'
    scene.render.film_transparent=True
    scene.view_settings.view_transform='Standard';scene.view_settings.look='None';scene.view_settings.exposure=-.65
    world=bpy.data.worlds.new(name+' studio');scene.world=world;world.use_nodes=True
    world.node_tree.nodes.get('Background').inputs['Color'].default_value=(.65,.72,.8,1)
    world.node_tree.nodes.get('Background').inputs['Strength'].default_value=.8
    bpy.ops.object.camera_add(location=(0,-3.5,9))
    cam=bpy.context.object;cam.rotation_euler=(-cam.location).to_track_quat('-Z','Y').to_euler()
    cam.data.type='ORTHO';cam.data.ortho_scale=2.65;scene.camera=cam
    for pos,power,size in [((-3,1,5),480,4),((3,-1,3),160,3),((0,4,2),210,2)]:
        bpy.ops.object.light_add(type='AREA',location=pos);obj=bpy.context.object
        obj.data.energy=power;obj.data.shape='DISK';obj.data.size=size
        obj.rotation_euler=(-obj.location).to_track_quat('-Z','Y').to_euler()
    return scene

def pixel_finish(path):
    """Use Blender's image API to snap alpha/palette and add a one-pixel silhouette.

    Keeps the native 32 px grid crisp in inventory/hand and avoids hundreds of
    alpha slivers being extruded by Minecraft's generated item mesh.
    """
    img=bpy.data.images.load(str(path),check_existing=False)
    img.scale(32,32)
    w,h=img.size;src=list(img.pixels[:]);dst=[0.0]*len(src)
    for y in range(h):
        for x in range(w):
            i=4*(y*w+x)
            if src[i+3]>=.5:
                dst[i:i+4]=[round(src[i+c]*24)/24 for c in range(3)]+[1]
            else:
                neighbors=[4*(yy*w+xx) for xx,yy in [(x-1,y),(x+1,y),(x,y-1),(x,y+1)]
                           if 0<=xx<w and 0<=yy<h and src[4*(yy*w+xx)+3]>=.5]
                if neighbors:
                    k=neighbors[0];dst[i:i+4]=[round(src[k+c]*.42*24)/24 for c in range(3)]+[1]
    img.pixels[:]=dst;img.filepath_raw=str(path);img.file_format='PNG';img.save()
    bpy.data.images.remove(img)

recipes=sorted((ROOT/'src/main/resources/data/domesurvival/recipes/forming').glob('*.json'))
items=sorted({json.loads(p.read_text())['result']['item'].split(':')[1] for p in recipes})
for name in items:
    if '--only' in sys.argv and name!=sys.argv[sys.argv.index('--only')+1]:continue
    alloy,kind=name.rsplit('_',1)
    scene=setup(name)
    metal=material(alloy,PALETTE[alloy]);edge=material(alloy+' bright cut',PALETTE[alloy],1.2)
    dark=material(alloy+' tooling recess',PALETTE[alloy],.38)
    if kind=='plate':
        body=box('Rolled plate',(0,0,0),(1.75,1.48,.14),metal,.075)
        body.rotation_euler.z=math.radians(-24)
        # Restrained roll marks, not random scratches or a decorative frame.
        for y in (-.48,-.43):
            obj=box('Rolling witness line',(0,y,.073),(1.40,.009,.004),dark,.001)
            obj.location=body.rotation_euler.to_matrix() @ obj.location
            obj.rotation_euler.z=body.rotation_euler.z
        for x in (.46,.53,.60):
            obj=box('Batch stamp',(x,.48,.075),(.028,.095,.004),dark,.002)
            obj.location=body.rotation_euler.to_matrix() @ obj.location
            obj.rotation_euler.z=body.rotation_euler.z
    elif kind=='gear':
        annulus('Twelve cut teeth',1,.25,.15,metal,12)
        annulus('Machined web recess',.70,.33,.018,dark,z=.081)
        annulus('Web surface',.65,.37,.019,metal,z=.094)
        annulus('Bored hub',.37,.25,.23,edge)
        for i in range(6):
            a=i*math.tau/6
            obj=box('Radial web',(.48*math.cos(a),.48*math.sin(a),.111),(.35,.085,.026),edge,.014)
            obj.rotation_euler.z=a
    elif kind=='rod':
        a=Vector((-.73,-.72,.02));b=Vector((.73,.72,.02))
        cylinder('Drawn solid rod',a,b,.135,metal)
        axis=(b-a).normalized()
        cylinder('Clean sawn end',a-axis*.005,a+axis*.016,.120,edge)
        # A second short score near the far end catches a narrow highlight.
        cylinder('End machining band',b-axis*.14,b-axis*.10,.138,edge)
    elif kind=='tube':
        obj=annulus('Hollow drawn tube',.29,.21,1.85,metal)
        axis=Vector((.72,.60,-.30)).normalized()
        obj.rotation_euler=axis.to_track_quat('Z','Y').to_euler()
        for sign in (-1,1):
            lip=annulus('Bright cut rim',.292,.208,.04,edge)
            lip.rotation_euler=obj.rotation_euler;lip.location=axis*(sign*.90)
    elif kind=='wire':
        points=[]
        # Open, neatly wound coil; gaps and the loose ends identify wire at 16 px.
        for i in range(481):
            t=i/480;angle=t*math.tau*2.5
            radius=.27+.62*t
            points.append((radius*math.cos(angle),radius*math.sin(angle),.02))
        strand('Open wound wire',points,.055,metal)
        strand('Inner free end',[points[0],(.12,-.05,.02),(.07,-.15,.02)],.055,edge)
        strand('Outer free end',[points[-1],(-1,.21,.02),(-1,.51,.02)],.055,metal)
    else:raise ValueError(kind)
    scene.render.filepath=str(OUT/(name+'.png'));bpy.ops.render.render(write_still=True)
    pixel_finish(OUT/(name+'.png'))
    (OUT/(name+'.png.mcmeta')).write_text('{"texture":{"blur":false,"clamp":true}}\n')
    model={'parent':'minecraft:item/generated','textures':{'layer0':'domesurvival:item/press_products/'+name}}
    (ASSETS/'models/item'/f'{name}.json').write_text(json.dumps(model,indent=2)+'\n')
bpy.ops.wm.save_as_mainfile(filepath=str(SOURCE/'press_products.blend'))
print('PRESS_PRODUCTS_COMPLETE',len(items),flush=True)
