"""Self-contained fluid pipe mesh, pixel and studio helpers, adapted from the approved pipe pipeline."""
from pathlib import Path
import json, math, struct, zlib
import bpy
from mathutils import Vector, Quaternion
ROOT=Path(__file__).resolve().parents[3]
OUT=ROOT/'source_assets/blender/fluid_pipes'
PREVIEW=OUT/'previews'
EVIDENCE=ROOT/'dev/fluid_pipes'
PREFIX='basic'
SPECS={}


DIRECTIONS = {"north": (0, 1, 0), "south": (0, -1, 0),
              "east": (1, 0, 0), "west": (-1, 0, 0),
              "up": (0, 0, 1), "down": (0, 0, -1)}


ROTATIONS = {"north": Quaternion((0, 0, 1), 0),
             "south": Quaternion((0, 0, 1), math.pi),
             "east": Quaternion((0, 0, 1), -math.pi / 2),
             "west": Quaternion((0, 0, 1), math.pi / 2),
             "up": Quaternion((1, 0, 0), math.pi / 2),
             "down": Quaternion((1, 0, 0), -math.pi / 2)}


FACE_IDS = {"bottom": (3, 2, 1, 0), "top": (4, 5, 6, 7),
            "near": (0, 1, 5, 4), "far": (2, 3, 7, 6),
            "right": (1, 2, 6, 5), "left": (3, 0, 4, 7)}


MC_FACES = {"north": "far", "south": "near", "east": "right", "west": "left", "up": "top", "down": "bottom"}


def material(name, color):
    mat = bpy.data.materials.new(name)
    mat.diffuse_color = (*color, 1)
    mat.use_nodes = True
    bsdf = mat.node_tree.nodes.get("Principled BSDF")
    bsdf.inputs["Base Color"].default_value = (*color, 1)
    bsdf.inputs["Roughness"].default_value = 0.78
    return mat


def annotation_material(name, color):
    mat = material(name, color)
    nodes = mat.node_tree.nodes
    nodes.clear()
    output = nodes.new("ShaderNodeOutputMaterial")
    emission = nodes.new("ShaderNodeEmission")
    emission.inputs["Color"].default_value = (*color, 1)
    emission.inputs["Strength"].default_value = 1
    mat.node_tree.links.new(emission.outputs[0], output.inputs["Surface"])
    return mat


def mesh_box(spec, name, mat, quat=None, offset=(0, 0, 0), scale=1):
    x0, y0, z0 = spec["minimum"]
    x1, y1, z1 = spec["maximum"]
    coordinates = [(x0,y0,z0), (x1,y0,z0), (x1,y1,z0), (x0,y1,z0),
                   (x0,y0,z1), (x1,y0,z1), (x1,y1,z1), (x0,y1,z1)]
    q = quat or Quaternion()
    verts = [q @ (Vector(v) * scale) + Vector(offset) for v in coordinates]
    mesh = bpy.data.meshes.new(name)
    mesh.from_pydata(verts, [], [FACE_IDS[f] for f in spec["faces"]])
    mesh.update()
    obj = bpy.data.objects.new(name, mesh)
    bpy.context.collection.objects.link(obj)
    mesh.materials.append(mat)
    obj["source_part"] = spec["name"]
    obj["axis_aligned_cuboid_source"] = True
    # Coordinates are baked into vertices: object scale and rotation stay identity.
    return obj


def assembly(tier, enabled, offset=(0,0,0), quat=None, scale=1, prefix="pipe"):
    q = quat or Quaternion()
    result = []
    for spec in SPECS[tier]["core"]:
        result.append(mesh_box(spec, f"{prefix}/{spec['name']}", CLAY, q, offset, scale))
    for direction in DIRECTIONS:
        role = "arm" if direction in enabled else "cap"
        for spec in SPECS[tier][role]:
            obj = mesh_box(spec, f"{prefix}/{direction}/{spec['name']}", CLAY,
                           q @ ROTATIONS[direction], offset, scale)
            obj["connection_property"] = direction
            obj["when"] = role == "arm"
            result.append(obj)
    return result


def reset_scene():
    bpy.ops.wm.read_factory_settings(use_empty=True)
    bpy.context.scene.unit_settings.system = "METRIC"
    bpy.context.scene.unit_settings.scale_length = 1 / 16
    bpy.context.scene["stage"] = "FLUID PIPE FAMILY / custom flanged geometry"
    bpy.context.scene["minecraft_units_per_block"] = 16
    global CLAY, INK, MUTED, PROXY
    CLAY = material("uniform_grey_clay_all_tiers", (0.38, 0.40, 0.42))
    INK = annotation_material("annotation_ink", (0.018, 0.027, 0.035))
    MUTED = annotation_material("annotation_secondary", (0.10, 0.12, 0.14))
    PROXY = material("reference_port_only", (0.23, 0.25, 0.27))


def camera_setup(width, height, span):
    scene = bpy.context.scene
    scene.render.engine = "CYCLES"
    scene.cycles.samples = 8
    scene.cycles.use_denoising = True
    scene.render.threads_mode = "FIXED"
    scene.render.threads = 8
    scene.render.resolution_x, scene.render.resolution_y = width, height
    scene.render.resolution_percentage = 100
    scene.render.image_settings.file_format = "PNG"
    scene.world = bpy.data.worlds.new("neutral_studio")
    scene.world.use_nodes = True
    scene.world.node_tree.nodes["Background"].inputs[0].default_value = (0.77, 0.78, 0.78, 1)
    scene.world.node_tree.nodes["Background"].inputs[1].default_value = 0.8
    scene.view_settings.view_transform = "Standard"
    scene.view_settings.look = "None"
    bpy.context.preferences.filepaths.save_version = 0
    cam_data = bpy.data.cameras.new("orthographic_review")
    cam = bpy.data.objects.new("orthographic_review", cam_data)
    scene.collection.objects.link(cam)
    cam.location = (70, -100, 80)
    cam.rotation_euler = (-cam.location).to_track_quat('-Z','Y').to_euler()
    cam.data.type = "ORTHO"
    cam.data.ortho_scale = span
    cam.data.clip_end = 1000
    scene.camera = cam
    global CAMERA_Q, RIGHT, UP, TOWARD
    CAMERA_Q = cam.rotation_euler.to_quaternion()
    RIGHT, UP, TOWARD = [CAMERA_Q @ Vector(v) for v in ((1,0,0),(0,1,0),(0,0,1))]
    # Directional studio lights give every cell equal lighting and cannot appear
    # as luminous panels across the wide/portrait presentation layouts.
    for name, loc, power in (("key_light", (20,-30,80), 1.5),
                             ("fill_light", (-50,-10,20), 0.5),
                             ("rim_light", (20,50,40), 0.8)):
        data = bpy.data.lights.new(name, "SUN")
        data.energy = power
        data.angle = math.radians(12)
        obj = bpy.data.objects.new(name, data)
        scene.collection.objects.link(obj)
        obj.location = loc
        obj.rotation_euler = (-obj.location).to_track_quat('-Z','Y').to_euler()


def screen_point(x, y, depth=0):
    return RIGHT * x + UP * y + TOWARD * depth


def label(text, x, y, size=1, align="LEFT", secondary=False):
    data = bpy.data.curves.new("label_" + text, 'FONT')
    data.body, data.size, data.align_x = text, size, align
    data.extrude = 0
    obj = bpy.data.objects.new("label_" + text, data)
    bpy.context.collection.objects.link(obj)
    obj.location = screen_point(x, y, 16)
    obj.rotation_euler = CAMERA_Q.to_euler()
    data.materials.append(MUTED if secondary else INK)


def save_render(name, save_blend=False):
    scene = bpy.context.scene
    scene.render.filepath = str(PREVIEW / (name + ".png"))
    if save_blend:
        bpy.ops.wm.save_as_mainfile(filepath=str(OUT / (name + ".blend")))
    bpy.ops.render.render(write_still=True)
    if scene.render.resolution_y > 2000:
        scene.render.image_settings.file_format = "JPEG"
        scene.render.image_settings.quality = 92
        bpy.data.images["Render Result"].save_render(str(PREVIEW / (name + "_review.jpg")), scene=scene)
        scene.render.image_settings.file_format = "PNG"
    progress("rendered " + name)


def progress(message):
    (EVIDENCE / "greybox_progress.txt").write_text(message, encoding="utf-8")
    print(message, flush=True)


def dump(path, data):
    path.parent.mkdir(parents=True, exist_ok=True)
    path.write_text(json.dumps(data, indent=2) + '\n', encoding='utf-8')


def png(path, tile):
    # Lossless exact-palette PNG, authored as part of the Blender Python pipeline.
    def chunk(kind, data):
        return struct.pack('!I', len(data))+kind+data+struct.pack('!I', zlib.crc32(kind+data)&0xffffffff)
    raw = b''.join(b'\x00'+b''.join(bytes.fromhex(color)+b'\xff' for color in row) for row in tile)
    path.parent.mkdir(parents=True, exist_ok=True)
    path.write_bytes(b'\x89PNG\r\n\x1a\n'+chunk(b'IHDR',struct.pack('!2I5B',16,16,8,6,0,0,0))+chunk(b'IDAT',zlib.compress(raw))+chunk(b'IEND',b''))


def face_size(element, face):
    d = [b-a for a,b in zip(element['from'],element['to'])]
    return {'north':(d[0],d[1]),'south':(d[0],d[1]),'east':(d[2],d[1]),'west':(d[2],d[1]),'up':(d[0],d[2]),'down':(d[0],d[2])}[face]


def project(v, direction):
    x,y,z=v
    return {'north':(16-x,16-y),'south':(x,16-y),'east':(16-z,16-y),
            'west':(z,16-y),'up':(x,z),'down':(x,16-z)}[direction]


def texture_objects(objects, models, g, outer=Quaternion(), offset=Vector((0,0,0)), scale=1):
    byname={e['name']:e for model in models.values() for e in model['elements']}
    mats={}
    for role in ('body','core','panel','rail','end','edge'):
        mat=g.material(PREFIX+'_'+role,(.5,.5,.5))
        tex=mat.node_tree.nodes.new('ShaderNodeTexImage')
        tex.image=bpy.data.images.load(str(OUT/f'textures/{PREFIX}_{role}.png'),check_existing=False)
        tex.image.pack(); tex.interpolation='Closest'
        mat.node_tree.links.new(tex.outputs['Color'],mat.node_tree.nodes.get('Principled BSDF').inputs['Base Color'])
        mats[role]=mat
    normal_names={(1,0,0):'east',(-1,0,0):'west',(0,1,0):'up',(0,-1,0):'down',(0,0,1):'south',(0,0,-1):'north'}
    for obj in objects:
        element=byname.get(obj.get('source_part'))
        if not element: continue
        q=outer @ g.ROTATIONS[obj['connection_property']] if obj.get('connection_property') else outer
        mesh=obj.data; mesh.materials.clear()
        for m in mats.values(): mesh.materials.append(m)
        while mesh.uv_layers: mesh.uv_layers.remove(mesh.uv_layers[0])
        uv=mesh.uv_layers.new(name='Minecraft_UV')
        for poly in mesh.polygons:
            n=q.inverted() @ poly.normal
            face=normal_names[(round(n.x),round(n.z),round(-n.y))]
            config=element['faces'][face]
            poly.material_index=list(mats).index(config['texture'][1:])
            coords=[]
            for loop in poly.loop_indices:
                v=(q.inverted() @ (mesh.vertices[mesh.loops[loop].vertex_index].co-Vector(offset)))/scale
                coords.append(project((v.x+8,v.z+8,8-v.y),face))
            xmin,xmax=min(v[0] for v in coords),max(v[0] for v in coords)
            ymin,ymax=min(v[1] for v in coords),max(v[1] for v in coords)
            for loop,(x,y) in zip(poly.loop_indices,coords):
                s,t=(x-xmin)/(xmax-xmin),(y-ymin)/(ymax-ymin)
                for _ in range(config.get('rotation',0)//90): s,t=t,1-s
                a,b,c,d=config['uv']
                uv.data[loop].uv=((a+(c-a)*s)/16,1-(b+(d-b)*t)/16)
