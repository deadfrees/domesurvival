"""Validate actual exported meshes/resources and emit editable Blockbench companions.
Run with Blender --background --python-exit-code 1 --python this_file.
"""
from pathlib import Path
import base64, copy, hashlib, itertools, json, struct, uuid, zlib
import bpy

ROOT=Path(__file__).resolve().parents[2]
DEV=ROOT/'dev/fluid_pipes'
A=ROOT/'src/main/resources/assets/domesurvival'
SRC=ROOT/'source_assets/blender/fluid_pipes'
BASE=ROOT/'source_assets/baseline/fluid_pipes'
IDS=['basic_fluid_pipe','reinforced_fluid_pipe','high_pressure_fluid_pipe']
DIRS=['north','east','south','west','up','down']
ROLES=['body','core','panel','rail','end','edge']
REPORT={'checks':[]}

def check(ok,message):
    REPORT['checks'].append({'pass':bool(ok),'check':message})
    if not ok: print('FAIL',message)

def read(p):return json.loads(p.read_text(encoding='utf-8-sig'))
def dump(p,data):p.parent.mkdir(parents=True,exist_ok=True);p.write_text(json.dumps(data,indent=2)+'\n',encoding='utf-8')
def sha(p):return hashlib.sha256(p.read_bytes()).hexdigest()
def rotate(v,d):
    x,y,z=v
    return {'north':(x,y,z),'east':(16-z,y,x),'south':(16-x,y,16-z),'west':(z,y,16-x),'up':(x,16-z,y),'down':(x,z,16-y)}[d]
def box(e,d='north'):
    points=[rotate(v,d) for v in itertools.product(*zip(e['from'],e['to']))]
    return tuple(min(v[a] for v in points) for a in range(3)),tuple(max(v[a] for v in points) for a in range(3))
def contained(p,b):return all(b[0][a]-1e-7<=p[a]<=b[1][a]+1e-7 for a in range(3))
def overlap(a,b):return all(min(a[1][i],b[1][i])-max(a[0][i],b[0][i])>1e-7 for i in range(3))
def covered(b,colliders):
    # Partition at every collider plane; testing every resulting cell is exact for cuboids.
    axes=[]
    for a in range(3):
        edges=sorted({b[0][a],b[1][a]}|{v[a] for c in colliders for v in c if b[0][a]<v[a]<b[1][a]})
        axes.append([(x+y)/2 for x,y in zip(edges,edges[1:])])
    return all(any(contained(p,c) for c in colliders) for p in itertools.product(*axes))

def png(path):
    data=path.read_bytes();check(data[:8]==b'\x89PNG\r\n\x1a\n',str(path.name)+' PNG signature')
    at=8;payload=b'';header=None
    while at<len(data):
        n=struct.unpack('>I',data[at:at+4])[0];kind=data[at+4:at+8];body=data[at+8:at+8+n]
        check(zlib.crc32(kind+body)&0xffffffff==struct.unpack('>I',data[at+8+n:at+12+n])[0],path.name+' '+kind.decode()+' CRC')
        if kind==b'IHDR':header=struct.unpack('>IIBBBBB',body)
        if kind==b'IDAT':payload+=body
        at+=12+n
    check(header==(16,16,8,6,0,0,0),path.name+' RGBA 16x16')
    raw=zlib.decompress(payload)
    check(len(raw)==16*65 and all(raw[y*65]==0 for y in range(16)),path.name+' unfiltered pixel layout')
    check(all(raw[y*65+1+x*4+3]==255 for x in range(16) for y in range(16)),path.name+' fully opaque')

def blockbench(tier,id,inv,item):
    prefix=id.replace('_fluid_pipe','');textures=[]
    for i,role in enumerate(ROLES):
        file=A/f'textures/block/fluid_pipe/{prefix}_{role}.png'
        textures.append({'name':file.name,'id':str(i),'uuid':str(uuid.uuid5(uuid.NAMESPACE_URL,id+'/'+role)),'relative_path':'','source':'data:image/png;base64,'+base64.b64encode(file.read_bytes()).decode(),'width':16,'height':16,'uv_width':16,'uv_height':16,'mode':'bitmap','particle':role=='body'})
    elements=[];groups={'core':[],'north':[],'south':[]}
    for e in inv['elements']:
        uid=str(uuid.uuid5(uuid.NAMESPACE_URL,id+'/'+e['name']));faces={}
        for d in DIRS:
            f=copy.deepcopy(e['faces'].get(d,{'texture':None,'uv':[0,0,0,0]}))
            if f['texture'] is not None:f['texture']=ROLES.index(f['texture'][1:])
            faces[d]=f
        elements.append({'name':e['name'],'uuid':uid,'type':'cube','from':e['from'],'to':e['to'],'origin':[8,8,8],'rotation':[0,0,0],'rescale':False,'box_uv':False,'faces':faces,'visibility':True,'export':True})
        group='core' if e['name']=='junction_body' else 'south' if e['name'].startswith('south_') else 'north';groups[group].append(uid)
    result={'meta':{'format_version':'4.10','model_format':'java_block','box_uv':False},'name':id,'model_identifier':id,'resolution':{'width':16,'height':16},'elements':elements,'textures':textures,'display':item['display'],'outliner':[{'name':g,'uuid':str(uuid.uuid5(uuid.NAMESPACE_URL,id+'/group/'+g)),'origin':[8,8,8],'children':children,'export':True,'isOpen':True} for g,children in groups.items()]}
    target=ROOT/f'source_assets/blockbench/fluid_pipes/{id}.bbmodel';dump(target,result)
    loaded=read(target)
    check(sum(f['texture'] is not None for e in loaded['elements'] for f in e['faces'].values())==sum(len(e['faces']) for e in inv['elements']),id+' Blockbench visible faces match inventory')
    check(all(len(base64.b64decode(t['source'].split(',')[1]))>0 for t in loaded['textures']),id+' Blockbench embedded textures')

baseline=read(DEV/'baseline_hashes.json')
external=read(DEV/'external_energy_snapshot.json')
REPORT['external_changes_note']=external['note']
baseline.update(external['files'])
current={p.relative_to(ROOT).as_posix():sha(p) for p in (ROOT/'src/main').rglob('*') if p.is_file()}
changed=sorted(p for p,h in baseline.items() if current.get(p)!=h)
added=sorted(set(current)-set(baseline))
allowed=[];new=[]
for id in IDS:
    allowed += [f'src/main/resources/assets/domesurvival/{p}' for p in [f'blockstates/{id}.json',f'models/item/{id}.json']+[f'models/block/{id}_{part}.json' for part in ('core','arm','inventory')]]
    new += [f'src/main/resources/assets/domesurvival/textures/block/fluid_pipe/{id.replace("_fluid_pipe","")}_{r}.png' for r in ROLES]
check(set(changed)==set(allowed),'Exactly 15 fluid JSON files changed; independently restored energy snapshot retained')
check(set(added)==set(new),'Exactly 18 fluid PNG files added beyond baseline and documented external energy restoration')
REPORT.update(changed_files=changed,added_files=added,tiers=[])
manifest=read(DEV/'model_manifest.json')
all_endpoints=[]
for tier,id in enumerate(IDS,1):
    core=read(A/f'models/block/{id}_core.json');arm=read(A/f'models/block/{id}_arm.json');inv=read(A/f'models/block/{id}_inventory.json');item=read(A/f'models/item/{id}.json')
    check(core==manifest[str(tier)]['core'] and arm==manifest[str(tier)]['arm'],id+' exported mesh manifest equals resources')
    state=read(A/f'blockstates/{id}.json');old=read(BASE/f'blockstates/{id}.json')
    for data in (state,old):
        for part in data['multipart']:part['apply'].pop('uvlock',None)
    check(state==old,id+' original selectors/rotations/model IDs preserved')
    actual=read(A/f'blockstates/{id}.json')
    check(all(p['apply'].get('uvlock') is False for p in actual['multipart'] if 'when' in p),id+' arm texture rotates with geometry')
    check(set(item['display'])=={'gui','ground','fixed','firstperson_righthand','firstperson_lefthand','thirdperson_righthand','thirdperson_lefthand'},id+' seven item contexts')
    for model in (core,arm,inv):
        for e in model['elements']:
            check(all(0<=e['from'][a]<e['to'][a]<=16 for a in range(3)),id+'/'+e['name']+' valid bounds')
            for d,f in e['faces'].items():
                check(all(0<=v<=16 for v in f['uv']) and f.get('rotation',0) in (0,90,180,270),id+'/'+e['name']+'/'+d+' valid UV')
                ref=model['textures'].get(f['texture'][1:],'');check(ref.startswith('domesurvival:') and (A/('textures/'+ref.split(':')[-1]+'.png')).is_file(),id+' texture resolves')
    for role in ROLES:
        path=A/f'textures/block/fluid_pipe/{id.replace("_fluid_pipe","")}_{role}.png';png(path);check(path.read_bytes()==(SRC/'textures'/path.name).read_bytes(),path.name+' source texture equals runtime')
    arm_q=sum(len(e['faces']) for e in arm['elements']);check(arm_q==[15,25,20][tier-1],id+' arm quad budget')
    collider_core=((5.5,)*3,(10.5,)*3)
    collider_arm={'from':[5.5,5.5,0],'to':[10.5,10.5,5.5]}
    for mask in range(64):
        dirs=[d for i,d in enumerate(DIRS) if mask&(1<<i)]
        boxes=[box(e) for e in core['elements']]+[box(e,d) for d in dirs for e in arm['elements']]
        colliders=[collider_core]+[box(collider_arm,d) for d in dirs]
        check(all(covered(b,colliders) for b in boxes),id+f' state {mask}: inside unchanged collider union')
        check(not any(overlap(a,b) for a,b in itertools.combinations(boxes,2)),id+f' state {mask}: no overlapping cuboid volumes')
    # The authored north module reaches the block plane and joins its core without a gap.
    sections=sorted(arm['elements'],key=lambda e:e['from'][2]);check(sections[0]['from'][2]==0 and sections[-1]['to'][2]==core['elements'][0]['from'][2] and all(a['to'][2]==b['from'][2] for a,b in zip(sections,sections[1:])),id+' contiguous axial sections')
    all_endpoints.append(box(sections[0]))
    bpy.ops.wm.open_mainfile(filepath=str(SRC/f'fluid_pipe_tier_{tier}.blend'))
    collection=bpy.data.collections.get('SOURCE_MODULES_core_arm');check(collection is not None,id+' editable Blender source collection')
    for part,model in [('core',core),('arm',arm)]:
        objects={o['source_part']:o for o in collection.objects if o.get('module')==part}
        check(len(objects)==len(model['elements']),id+'/'+part+' actual master object count')
        for e in model['elements']:
            obj=objects[e['name']];vertices=[(v.co.x+8,v.co.z+8,8-v.co.y) for v in obj.data.vertices]
            check(all(abs(min(v[a] for v in vertices)-e['from'][a])<1e-6 and abs(max(v[a] for v in vertices)-e['to'][a])<1e-6 for a in range(3)),id+'/'+e['name']+' saved Blender mesh bounds equal JSON')
            check(len(obj.data.polygons)==len(e['faces']) and all(len(p.vertices)==4 for p in obj.data.polygons),id+'/'+e['name']+' saved mesh quad faces equal JSON')
            check(tuple(obj.scale)==(1,1,1) and all(abs(v)<1e-6 for v in obj.rotation_euler),id+'/'+e['name']+' applied transforms')
    blockbench(tier,id,inv,item)
    REPORT['tiers'].append({'id':id,'arm_quads':arm_q,'straight_quads':6+2*arm_q,'six_way_quads':6+6*arm_q,'item_cuboids':len(inv['elements'])})
for i,a in enumerate(all_endpoints):
    for j,b in enumerate(all_endpoints):
        for d in DIRS:check(a[0][2]==b[0][2]==0 and all(a[0][k]<8<a[1][k] and b[0][k]<8<b[1][k] for k in (0,1)),f'Tier {i+1}/{j+1} {d}: centered shared face, no axial gap')
for p in BASE.rglob('*.json'):
    rel=p.relative_to(BASE);check(p.read_bytes()==(ROOT/'run/fluid-pipe-visual/resourcepacks/fluid_pipe_before/assets/domesurvival'/rel).read_bytes(),str(rel)+' before pack unchanged')
REPORT['status']='PASS' if all(c['pass'] for c in REPORT['checks']) else 'FAIL'
REPORT['check_count']=len(REPORT['checks'])
dump(DEV/'static_validation.json',REPORT)
print('FLUID_STATIC',REPORT['status'],REPORT['check_count'])
assert REPORT['status']=='PASS'
