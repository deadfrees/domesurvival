"""Tier 1 pixel materials, UV, item assembly and Blender master. Geometry is preserved."""
from pathlib import Path
import copy, hashlib, importlib.util, json, shutil, struct, traceback, zlib
import bpy
from mathutils import Vector, Quaternion

ROOT = Path(__file__).resolve().parents[3]
ASSETS = ROOT / 'src/main/resources/assets/domesurvival'
OUT = ROOT / 'source_assets/blender/energy_pipes'
DEV = ROOT / 'dev/energy_pipes'
BACKUP = ROOT / 'source_assets/baseline/energy_pipes/tier1'
ID = 'basic_energy_pipe'
PREFIX = 'basic'
BASELINE_NAME = 'tier1_baseline_hashes.json'
COMPARISON_PACK = 'energy_pipe_before'
FILES = [f'models/block/{ID}_{p}.json' for p in ('core','arm','inventory')] + [f'models/item/{ID}.json', f'blockstates/{ID}.json']
PALETTE = {'body':'596361','shadow':'303a3b','black':'171e20','light':'8f9990','steel':'707c77','amber':'c5a252','darkamber':'836d38'}


def dump(path, data):
    path.parent.mkdir(parents=True, exist_ok=True)
    path.write_text(json.dumps(data, indent=2) + '\n', encoding='utf-8')


def pixels(role):
    base = {'body':'body','core':'body','panel':'black','rail':'steel','end':'shadow','edge':'shadow'}[role]
    tile = [[PALETTE[base] for _ in range(16)] for _ in range(16)]
    def rect(x,y,w,h,color):
        for yy in range(y,y+h):
            for xx in range(x,x+w): tile[yy][xx] = PALETTE[color]
    if role == 'body':
        rect(0,0,1,16,'shadow'); rect(5,0,1,16,'shadow')
        rect(2,0,1,16,'darkamber'); rect(2,1,1,10,'amber')
        rect(4,2,1,1,'steel'); rect(1,9,1,1,'steel')
    elif role == 'core':
        rect(0,0,7,1,'light'); rect(0,1,1,6,'steel')
        rect(6,1,1,6,'shadow'); rect(1,6,5,1,'shadow')
        for x,y in ((1,1),(5,1),(1,5),(5,5)): rect(x,y,1,1,'black')
    elif role == 'panel':
        rect(0,0,6,1,'steel'); rect(0,0,1,6,'steel')
        rect(5,0,1,6,'shadow'); rect(0,5,6,1,'shadow')
        rect(2,1,1,4,'amber')  # Single contact/tier-I mark, not an active-flow indicator.
    elif role == 'rail':
        rect(0,0,1,16,'light'); rect(4,0,1,16,'body')
        rect(2,3,1,1,'body'); rect(1,10,1,1,'body')
    elif role == 'end':
        rect(0,0,6,1,'steel'); rect(0,0,1,6,'steel')
        rect(5,0,1,6,'black'); rect(0,5,6,1,'black')
        rect(2,2,2,2,'darkamber'); rect(2,2,1,1,'amber')
    return tile


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


def style_model(part):
    model = json.loads((BACKUP / f'models/block/{ID}_{part}.json').read_text(encoding='utf-8'))
    model['textures'] = {r:f'domesurvival:block/energy_pipe/{PREFIX}_{r}' for r in ('body','core','panel','rail','end','edge')}
    model['textures']['particle'] = model['textures']['core']
    for e in model['elements']:
        for direction, f in e['faces'].items():
            w,h = face_size(e,direction)
            if part == 'core':
                role = 'core' if e['name']=='core_body' else ('panel' if min(w,h)>2 else 'edge')
            else:
                role = ('end' if direction in ('north','south') else 'body') if e['name']=='arm_body' else 'rail'
            rotation = 0
            if part=='arm' and direction in ('east','west'):
                w,h = h,w
                rotation = 90
            f.update(texture='#'+role, uv=[0,0,round(w*2,5),round(h*2,5)])
            f.pop('rotation',None)
            if rotation: f['rotation']=rotation
    return model


def generate_assets():
    baseline = DEV / BASELINE_NAME
    if not baseline.exists():
        dump(baseline, {str(p.relative_to(ROOT)).replace(chr(92),'/'): hashlib.sha256(p.read_bytes()).hexdigest()
                        for p in (ROOT/'src/main').rglob('*') if p.is_file()})
    for relative in FILES:
        dest = BACKUP / relative
        if not dest.exists():
            dest.parent.mkdir(parents=True, exist_ok=True)
            shutil.copy2(ASSETS/relative, dest)
    for role in ('body','core','panel','rail','end','edge'):
        source = OUT / f'textures/{PREFIX}_{role}.png'
        png(source, pixels(role))
        target = ASSETS / f'textures/block/energy_pipe/{PREFIX}_{role}.png'
        target.parent.mkdir(parents=True, exist_ok=True)
        shutil.copyfile(source,target)
    models = {p:style_model(p) for p in ('core','arm')}
    for part, model in models.items(): dump(ASSETS/f'models/block/{ID}_{part}.json',model)
    state = json.loads((BACKUP/f'blockstates/{ID}.json').read_text(encoding='utf-8'))
    for entry in state['multipart']:
        if entry.get('when'): entry['apply']['uvlock']=False  # longitudinal texture rotates with its arm
    dump(ASSETS/f'blockstates/{ID}.json',state)
    inventory = copy.deepcopy(models['core'])
    inventory['elements'] += copy.deepcopy(models['arm']['elements'])
    opposite = {'north':'south','south':'north','east':'west','west':'east','up':'up','down':'down'}
    for e in copy.deepcopy(models['arm']['elements']):
        lo,hi=e['from'],e['to']
        e['from']=[16-hi[0],lo[1],16-hi[2]]; e['to']=[16-lo[0],hi[1],16-lo[2]]
        e['name']='south_'+e['name']
        e['faces']={opposite[d]:f for d,f in e['faces'].items()}
        for direction in ('up','down'):
            if direction in e['faces']: e['faces'][direction]['rotation']=(e['faces'][direction].get('rotation',0)+180)%360
        inventory['elements'].append(e)
    dump(ASSETS/f'models/block/{ID}_inventory.json',inventory)
    display = {}
    for name,rotation,translation,scale in (
        ('gui',[30,225,0],[0,0,0],1.0),('ground',[0,0,0],[0,1,0],.55),('fixed',[0,45,0],[0,0,0],1),
        ('firstperson_righthand',[0,45,0],[0,1.5,0],.65),('firstperson_lefthand',[0,225,0],[0,1.5,0],.65),
        ('thirdperson_righthand',[75,45,0],[0,2.5,0],.65),('thirdperson_lefthand',[75,225,0],[0,2.5,0],.65)):
        display[name]={'rotation':rotation,'translation':translation,'scale':[scale]*3}
    dump(ASSETS/f'models/item/{ID}.json',{'parent':f'domesurvival:block/{ID}_inventory','display':display})
    # Before/after comparison pack, confined to the isolated test client.
    pack = ROOT/'run/energy-pipe-visual/resourcepacks'/COMPARISON_PACK
    dump(pack/'pack.mcmeta',{'pack':{'pack_format':15,'description':'Energy pipes: original material comparison'}})
    for relative in FILES:
        target=pack/'assets/domesurvival'/relative
        target.parent.mkdir(parents=True,exist_ok=True)
        shutil.copyfile(BACKUP/relative,target)
    return models


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


def main():
    models=generate_assets()
    spec=importlib.util.spec_from_file_location('pipe_greybox',Path(__file__).with_name('create_energy_pipe_greyboxes.py'))
    g=importlib.util.module_from_spec(spec);spec.loader.exec_module(g)
    # Preserve the editable source collection and the exact existing mesh geometry.
    bpy.ops.wm.open_mainfile(filepath=str(OUT/'energy_pipe_tier_1.blend'),load_ui=False,use_scripts=False)
    for obj in list(bpy.data.objects):
        if obj.type=='MESH' and obj.get('source_part'):
            texture_objects([obj],models,g)
    for obj in bpy.data.objects:
        if obj.type=='FONT' and 'CURRENT GAME SHAPE' in obj.data.body:
            obj.data.body='TIER 1 MATERIAL MASTER / ORIGINAL GEOMETRY'
    bpy.context.scene['stage']='TIER 1 TEXTURES — original game shape'
    bpy.context.preferences.filepaths.save_version=0
    bpy.ops.wm.save_as_mainfile(filepath=str(OUT/'energy_pipe_tier_1.blend'))
    g.reset_scene();g.camera_setup(1600,900,64)
    g.label('DOMESURVIVAL / BASIC ENERGY PIPE',-29,14,1.6)
    g.label('GRAPHITE   /   STEEL   /   AMBER',-29,11.5,.85,secondary=True)
    for x,y,enabled in ((-16,0,{'north','south'}),(10,3,{'north','east'}),(10,-7,set())):
        offset=g.screen_point(x,y)
        objects=g.assembly(1,enabled,offset,prefix=f'textured_{x}_{y}')
        texture_objects(objects,models,g,offset=offset)
    g.label('ORIGINAL GEOMETRY / 16 x 16 PIXEL MATERIALS',-29,-15,.75,secondary=True)
    g.save_render('05_energy_pipe_tier1_styled',True)
    dump(DEV/'tier1_style_result.json',{'status':'PASS','geometry_changed':False,'texture_size':[16,16],
         'textures':6,'texels_per_unit':2,'runtime_tiers_changed':[1]})


if __name__ == '__main__':
    try: main()
    except Exception:
        dump(DEV/'tier1_style_result.json',{'status':'FAIL','error':traceback.format_exc()})
        raise
