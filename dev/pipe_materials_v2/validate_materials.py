"""Structural and saved-Blender validation for the six material-only replacements."""
from pathlib import Path
import base64,copy,hashlib,itertools,json,math,struct,zlib
import bpy
from mathutils import Quaternion,Vector
ROOT=Path(__file__).resolve().parents[2];DEV=ROOT/'dev/pipe_materials_v2';BASE=DEV/'baseline'
A=ROOT/'src/main/resources/assets/domesurvival';SRC=ROOT/'source_assets/blender/pipe_materials_v2'
IDS=['basic_energy_pipe','reinforced_energy_pipe','high_voltage_energy_pipe','basic_fluid_pipe','reinforced_fluid_pipe','high_pressure_fluid_pipe']
checks=[]
def check(ok,msg):checks.append({'pass':bool(ok),'check':msg});print(('PASS ' if ok else 'FAIL ')+msg) if not ok else None
def read(p):return json.loads(p.read_text(encoding='utf-8-sig'))
def sha(p):return hashlib.sha256(p.read_bytes()).hexdigest()
def png_rows(data):
    width,height=struct.unpack('>II',data[16:24]);assert data[24:26]==bytes([8,6])
    packed=b'';at=8
    while at<len(data):
        n=int.from_bytes(data[at:at+4],'big')
        if data[at+4:at+8]==b'IDAT':packed+=data[at+8:at+8+n]
        at+=12+n
    raw=zlib.decompress(packed);previous=bytearray(width*4);rows=[];at=0
    for y in range(height):
        mode=raw[at];at+=1;row=bytearray(raw[at:at+width*4]);at+=width*4
        for x in range(len(row)):
            a=row[x-4] if x>=4 else 0;b=previous[x];c=previous[x-4] if x>=4 else 0
            pred=a+b-c;dist=(abs(pred-a),abs(pred-b),abs(pred-c));paeth=(a,b,c)[dist.index(min(dist))]
            row[x]=(row[x]+(0,a,b,(a+b)//2,paeth)[mode])&255
        rows.append(row);previous=row
    return rows
def frame(m):
    result=copy.deepcopy(m);result.pop('textures',None)
    for e in result['elements']:
        for f in e['faces'].values():f.pop('uv',None);f.pop('rotation',None)
    return result
baseline=read(DEV/'baseline_hashes.json');current={p.relative_to(ROOT).as_posix():sha(p) for p in (ROOT/'src/main').rglob('*') if p.is_file()}
changed=sorted(k for k,v in baseline.items() if current.get(k)!=v);added=sorted(set(current)-set(baseline))
expected=[f'src/main/resources/assets/domesurvival/models/block/{id}_{part}.json' for id in IDS for part in ('core','arm','inventory')]
expected_new=[f'src/main/resources/assets/domesurvival/textures/block/pipe_materials_v2/{id}.png' for id in IDS]
check(set(changed)==set(expected),'Only 18 scoped block model JSON files changed')
check(set(added)==set(expected_new),'Only six material PNG atlases added; Java and all other assets untouched')
manifest=read(DEV/'material_manifest.json')
for id in IDS:
    models={p:read(A/f'models/block/{id}_{p}.json') for p in ('core','arm','inventory')}
    old={p:read(BASE/f'models/block/{id}_{p}.json') for p in models}
    for part,model in models.items():
        check(frame(model)==frame(old[part]),id+'/'+part+': cuboid coordinates, face sets and non-material properties identical')
        for e in model['elements']:
            for face,f in e['faces'].items():
                u0,v0,u1,v1=f['uv'];check(0<=u0<u1<=16 and 0<=v0<v1<=16,id+'/'+e['name']+'/'+face+': atlas UV bounds')
                ref=model['textures'][f['texture'][1:]];check((A/('textures/'+ref.split(':')[1]+'.png')).exists(),id+': texture resolves')
    for rel in (f'blockstates/{id}.json',f'models/item/{id}.json'):
        check((A/rel).read_bytes()==(BASE/rel).read_bytes(),rel+': selectors, rotations and display contexts byte-identical')
    # All connection masks retain the same combined visible geometry budget.
    for mask in range(64):
        n=mask.bit_count()
        before=sum(len(e['faces']) for e in old['core']['elements'])+n*sum(len(e['faces']) for e in old['arm']['elements'])
        after=sum(len(e['faces']) for e in models['core']['elements'])+n*sum(len(e['faces']) for e in models['arm']['elements'])
        check(before==after,id+f' state {mask}: unchanged polygon budget')
    png=A/f'textures/block/pipe_materials_v2/{id}.png';data=png.read_bytes();size=manifest[id]['atlas_size']
    check(struct.unpack('>II',data[16:24])==(size,size),id+': atlas dimensions')
    check(data==(SRC/'textures'/png.name).read_bytes(),id+': source atlas equals runtime')
    rows=png_rows(data)
    body_island=next(i for i in manifest[id]['islands'] if i['role']=='body')
    sample_x=body_island['x']+body_island['width']//2;sample_y=body_island['y']+body_island['height']//2
    midpoint=sum(rows[sample_y][sample_x*4:sample_x*4+3])/765
    check(.22<midpoint<.68,id+f': encoded steel midtone {midpoint:.3f}; no double gamma darkening')
    img=bpy.data.images.load(str(png),check_existing=False);px=list(img.pixels[:]);check(all(a>.999 for a in px[3::4]),id+': opaque atlas')
    colors=len({tuple(round(x*255) for x in px[i:i+3]) for i in range(0,len(px),4)});check(colors>128,id+f': continuous tonal palette ({colors} colors)')
    islands=manifest[id]['islands']
    for a,b in itertools.combinations(islands,2):
        check(not (min(a['x']+a['width'],b['x']+b['width'])>max(a['x'],b['x']) and min(a['y']+a['height'],b['y']+b['height'])>max(a['y'],b['y'])),id+': UV islands do not overlap')
    bb=read(ROOT/f'source_assets/blockbench/pipe_materials_v2/{id}.bbmodel')
    check(base64.b64decode(bb['textures'][0]['source'].split(',')[1])==data,id+': Blockbench embeds current atlas')
    check(len(bb['elements'])==len(models['inventory']['elements']),id+': Blockbench cuboid count')
    for e,j in zip(bb['elements'],models['inventory']['elements']):check(e['from']==j['from'] and e['to']==j['to'] and sum(f['texture'] is not None for f in e['faces'].values())==len(j['faces']),id+': Blockbench frame/face preservation')
    bpy.ops.wm.open_mainfile(filepath=str(SRC/f'{id}.blend'),load_ui=False,use_scripts=False)
    meshes=[o for o in bpy.data.objects if o.type=='MESH' and o.get('source_part')]
    byname={e['name']:e for part in ('core','arm') for e in models[part]['elements']}
    check(len(meshes)==len(models['core']['elements'])+2*len(models['arm']['elements']),id+': saved editable straight assembly count')
    for obj in meshes:
        e=byname[obj['source_part']];q=Quaternion((0,0,1),math.pi) if obj.get('connection_property')=='south' else Quaternion()
        pts=[]
        for v in obj.data.vertices:
            p=q.inverted()@v.co;pts.append((p.x+8,p.z+8,8-p.y))
        check(all(abs(min(p[a] for p in pts)-e['from'][a])<1e-5 and abs(max(p[a] for p in pts)-e['to'][a])<1e-5 for a in range(3)),id+'/'+obj.name+': saved mesh matches frame')
        check(len(obj.data.polygons)==len(e['faces']) and obj.data.uv_layers.active is not None,id+'/'+obj.name+': saved faces/UV present')
        check(tuple(obj.scale)==(1,1,1),id+'/'+obj.name+': applied scale')
    check(all(i.packed_file is not None for i in bpy.data.images if i.source=='FILE'),id+': all master texture images packed')
status='PASS' if all(c['pass'] for c in checks) else 'FAIL'
(DEV/'static_validation.json').write_text(json.dumps({'status':status,'checks_count':len(checks),'changed_files':changed,'added_files':added,'checks':checks},indent=2)+'\n')
print('MATERIAL_STATIC',status,len(checks));assert status=='PASS'
