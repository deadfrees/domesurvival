"""Blender-authored flanged fluid pipes; static Minecraft multipart export only."""
from pathlib import Path
import copy, hashlib, importlib.util, json, shutil, traceback
import bpy

HERE=Path(__file__).resolve().parent
ROOT=HERE.parents[2]
OUT=ROOT/'source_assets/blender/fluid_pipes'
DEV=ROOT/'dev/fluid_pipes'
ASSETS=ROOT/'src/main/resources/assets/domesurvival'
BACKUP=ROOT/'source_assets/baseline/fluid_pipes'
IDS={1:'basic_fluid_pipe',2:'reinforced_fluid_pipe',3:'high_pressure_fluid_pipe'}
ROLES=('body','core','panel','rail','end','edge')

def load_module(name,file):
    spec=importlib.util.spec_from_file_location(name,HERE/file)
    result=importlib.util.module_from_spec(spec);spec.loader.exec_module(result);return result

g=load_module('fluid_helpers','fluid_pipe_helpers.py')
p=g
g.OUT=OUT;g.PREVIEW=OUT/'previews';g.EVIDENCE=DEV
p.OUT=OUT
for folder in (OUT,g.PREVIEW,DEV):folder.mkdir(parents=True,exist_ok=True)
dump=p.dump

def tile(tier,role):
    palettes={1:('596361','76847d','8f9990'),2:('54636b','8c9995','a5afa7'),3:('35444a','617674','93a5a0')}
    body,steel,light=palettes[tier]
    colors={'body':body,'core':body,'panel':'527fa8','rail':steel,'end':'303a3b','edge':'171e20'}
    grid=[[colors[role] for _ in range(16)] for _ in range(16)]
    def rect(x,y,w,h,color):
        for yy in range(y,y+h):
            for xx in range(x,x+w):grid[yy][xx]=color
    if role=='body':
        rect(0,0,1,16,light);rect(7,0,1,16,'303a3b')
        rect(3,3,1,1,steel);rect(5,7,1,1,'303a3b')
    elif role=='core':
        rect(0,0,10,1,light);rect(0,1,1,9,steel);rect(9,0,1,10,'303a3b');rect(0,9,10,1,'303a3b')
        # A static fluid-service droplet and tier tick marks, not a contents display.
        rect(4,1,1,2,'527fa8');rect(3,3,3,2,'527fa8');rect(2,5,5,2,'527fa8');rect(3,7,3,1,'527fa8')
        for x in {1:(4,),2:(3,5),3:(2,4,6)}[tier]:rect(x,8,1,1,'9dbbcc')
        for x,y in ((1,1),(8,1),(1,8)):rect(x,y,1,1,'171e20')
    elif role=='rail':
        rect(0,0,1,16,light);rect(8,0,1,16,'303a3b')
        for x in (1,7):rect(x,1,1,1,'303a3b')
    elif role=='end':
        rect(0,0,10,1,light);rect(0,0,1,10,light);rect(9,0,1,10,'171e20');rect(0,9,10,1,'171e20')
        rect(2,2,6,6,'171e20');rect(3,3,4,4,body)
    elif role=='panel':
        rect(0,0,1,16,'305775');rect(8,0,1,16,'305775')
        for x in (2,6):rect(x,1,1,1,light)
    return grid

def element(name,width,z0,z1,role,omit=()):
    lo=[8-width/2,8-width/2,z0];hi=[8+width/2,8+width/2,z1]
    faces={}
    for side in ('north','south','east','west','up','down'):
        if side in omit:continue
        w,h=p.face_size({'from':lo,'to':hi},side)
        end=side in ('north','south')
        texture='end' if end and role!='edge' else role
        face={'texture':'#'+texture,'uv':[0,0,round(w*2,5),round(h*2,5)]}
        if side in ('east','west'):face['uv']=[0,0,round(h*2,5),round(w*2,5)];face['rotation']=90
        faces[side]=face
    return {'name':name,'from':lo,'to':hi,'faces':faces}

def geometry(tier):
    size={1:4.5,2:4.75,3:5}[tier];start=8-size/2
    sections={
        1:[(4.5,0,.75,'rail'),(3.5,.75,5,'body'),(4.25,5,start,'edge')],
        2:[(5,0,1,'rail'),(4,1,2.5,'body'),(4.75,2.5,3.25,'panel'),(4,3.25,4.75,'body'),(4.5,4.75,start,'edge')],
        3:[(5,0,1.25,'panel'),(4.75,1.25,2,'rail'),(4.25,2,4.5,'body'),(5,4.5,start,'edge')],
    }[tier]
    core=element('junction_body',size,start,16-start,'core')
    for face in core['faces'].values():face.update(texture='#core',uv=[0,0,10,10]);face.pop('rotation',None)
    arms=[]
    for i,(width,a,b,role) in enumerate(sections):
        omit=[]
        if i>0 and sections[i-1][0]>=width:omit.append('north')
        if (sections[i+1][0] if i+1<len(sections) else size)>=width:omit.append('south')
        arms.append(element(f'arm_section_{i}',width,a,b,role,omit))
    textures={r:f'domesurvival:block/fluid_pipe/{IDS[tier].replace("_fluid_pipe","")}_{r}' for r in ROLES}
    textures['particle']=textures['body']
    return {part:{'ambientocclusion':True,'textures':textures,'elements':items} for part,items in (('core',[core]),('arm',arms))}

def specs(models):
    result={'cap':[]}
    for part,model in models.items():
        result[part]=[]
        for e in model['elements']:
            lo,hi=e['from'],e['to']
            result[part].append({'name':e['name'],'minimum':[lo[0]-8,8-hi[2],lo[1]-8],'maximum':[hi[0]-8,8-lo[2],hi[1]-8],'faces':[g.MC_FACES[f] for f in e['faces']]})
    return result

def export_from_mesh(obj,e):
    # Export bounds from the bpy-created mesh, preserving explicit face/UV assignments.
    coordinates=[(v.co.x+8,v.co.z+8,8-v.co.y) for v in obj.data.vertices]
    result=copy.deepcopy(e)
    result['from']=[round(min(v[a] for v in coordinates),6) for a in range(3)]
    result['to']=[round(max(v[a] for v in coordinates),6) for a in range(3)]
    assert result['from']==e['from'] and result['to']==e['to']
    return result

def main():
    baseline=DEV/'baseline_hashes.json'
    if not baseline.exists():dump(baseline,{str(f.relative_to(ROOT)).replace(chr(92),'/'):hashlib.sha256(f.read_bytes()).hexdigest() for f in (ROOT/'src/main').rglob('*') if f.is_file()})
    for tier,id in IDS.items():
        files=[f'models/block/{id}_{part}.json' for part in ('core','arm','inventory')]+[f'models/item/{id}.json',f'blockstates/{id}.json']
        for rel in files:
            target=BACKUP/rel
            if not target.exists():target.parent.mkdir(parents=True,exist_ok=True);shutil.copy2(ASSETS/rel,target)
    all_models={tier:geometry(tier) for tier in IDS}
    g.SPECS={tier:specs(models) for tier,models in all_models.items()}
    g.reset_scene();g.camera_setup(1800,1100,70)
    g.label('DOMESURVIVAL / FLUID PIPE SHAPE STUDY',-32,17,1.5)
    for tier,x in ((1,-22),(2,0),(3,22)):
        for y,sides in ((6,{'north','south'}),(-9,{'north','east'})):g.assembly(tier,sides,g.screen_point(x,y),prefix=f'grey_{tier}_{y}')
        g.label(f'TIER {tier}',x-5,-1.5,1.1)
    g.save_render('01_fluid_pipe_greybox',True)
    for tier,id in IDS.items():
        prefix=id.replace('_fluid_pipe','');p.PREFIX=prefix
        for role in ROLES:
            file=OUT/f'textures/{prefix}_{role}.png';p.png(file,tile(tier,role))
            target=ASSETS/f'textures/block/fluid_pipe/{prefix}_{role}.png';target.parent.mkdir(parents=True,exist_ok=True);shutil.copyfile(file,target)
        g.reset_scene();g.camera_setup(1200,850,32)
        source=bpy.data.collections.new('SOURCE_MODULES_core_arm');bpy.context.scene.collection.children.link(source)
        for part,model in all_models[tier].items():
            for i,(e,spec) in enumerate(zip(model['elements'],g.SPECS[tier][part])):
                obj=g.mesh_box(spec,f'{part}/{e["name"]}',g.CLAY)
                if part=='arm':obj['connection_property']='north'
                obj['module']=part
                model['elements'][i]=export_from_mesh(obj,e)
                p.texture_objects([obj],all_models[tier],g)
                for collection in list(obj.users_collection):collection.objects.unlink(obj)
                source.objects.link(obj)
            dump(ASSETS/f'models/block/{id}_{part}.json',model)
        source.hide_render=True;source.hide_viewport=True
        objects=g.assembly(tier,{'north','south'},prefix='editable_straight')
        p.texture_objects(objects,all_models[tier],g)
        g.label(f'FLUID PIPE / TIER {tier}',-14,10,1)
        bpy.context.scene['stage']='Fluid pipe material master / Blender mesh export'
        bpy.ops.wm.save_as_mainfile(filepath=str(OUT/f'fluid_pipe_tier_{tier}.blend'))
        inv=copy.deepcopy(all_models[tier]['core']);inv['elements']+=copy.deepcopy(all_models[tier]['arm']['elements'])
        opposite={'north':'south','south':'north','east':'west','west':'east','up':'up','down':'down'}
        for e in copy.deepcopy(all_models[tier]['arm']['elements']):
            lo,hi=e['from'],e['to'];e['from']=[16-hi[0],lo[1],16-hi[2]];e['to']=[16-lo[0],hi[1],16-lo[2]];e['name']='south_'+e['name'];e['faces']={opposite[d]:f for d,f in e['faces'].items()}
            for d in ('up','down'):
                if d in e['faces']:e['faces'][d]['rotation']=(e['faces'][d].get('rotation',0)+180)%360
            inv['elements'].append(e)
        dump(ASSETS/f'models/block/{id}_inventory.json',inv)
        display={}
        for name,rotation,translation,scale in (
            ('gui',[30,225,0],[0,0,0],1),('ground',[0,0,0],[0,1,0],.55),('fixed',[0,45,0],[0,0,0],1),
            ('firstperson_righthand',[0,45,0],[0,1.5,0],.65),('firstperson_lefthand',[0,225,0],[0,1.5,0],.65),
            ('thirdperson_righthand',[75,45,0],[0,2.5,0],.65),('thirdperson_lefthand',[75,225,0],[0,2.5,0],.65)):
            display[name]={'rotation':rotation,'translation':translation,'scale':[scale]*3}
        dump(ASSETS/f'models/item/{id}.json',{'parent':f'domesurvival:block/{id}_inventory','display':display})
        state=json.loads((BACKUP/f'blockstates/{id}.json').read_text())
        for entry in state['multipart']:
            if 'when' in entry:entry['apply']['uvlock']=False
        dump(ASSETS/f'blockstates/{id}.json',state)
    pack=ROOT/'run/fluid-pipe-visual/resourcepacks/fluid_pipe_before'
    dump(pack/'pack.mcmeta',{'pack':{'pack_format':15,'description':'Fluid pipes: original models and materials'}})
    for f in BACKUP.rglob('*.json'):
        dest=pack/'assets/domesurvival'/f.relative_to(BACKUP);dest.parent.mkdir(parents=True,exist_ok=True);shutil.copyfile(f,dest)
    g.reset_scene();g.camera_setup(1800,1100,70)
    g.label('DOMESURVIVAL / FLUID PIPE FAMILY',-32,17,1.6)
    for tier,x in ((1,-22),(2,0),(3,22)):
        p.PREFIX=IDS[tier].replace('_fluid_pipe','')
        for y,sides in ((6,{'north','south'}),(-9,{'north','east'})):
            offset=g.screen_point(x,y);objects=g.assembly(tier,sides,offset,prefix=f'styled_{tier}_{y}');p.texture_objects(objects,all_models[tier],g,offset=offset)
        g.label(f'TIER {tier}',x-5,-1.5,1.1)
    g.label('CAST STEEL / SEALED FLANGES / FLUID SERVICE BLUE',-32,-19,.85,secondary=True)
    g.save_render('02_fluid_pipe_styled_lineup',True)
    dump(DEV/'model_manifest.json',{tier:{'id':IDS[tier],'core':models['core'],'arm':models['arm']} for tier,models in all_models.items()})
    dump(DEV/'blender_result.json',{'status':'PASS','blender_version':bpy.app.version_string,'masters':3,'mesh_bounds_export_verified':True,'textures':18,'runtime':'Minecraft JSON multipart'})

if __name__=='__main__':
    try:main()
    except Exception:
        dump(DEV/'blender_result.json',{'status':'FAIL','error':traceback.format_exc()});raise
