"""Refine six EXISTING pipe frames with face-sized, bevel-shaded material islands.
Blender Python authors the atlases, UVs, editable meshes, Blockbench files and previews.
Geometry, face sets, blockstates, item transforms and production Java remain unchanged.
"""
from pathlib import Path
import base64,copy,hashlib,importlib.util,json,math,shutil,uuid
import bpy
from mathutils import Vector,Quaternion
ROOT=Path(__file__).resolve().parents[3]
DEV=ROOT/'dev/pipe_materials_v2';BASE=DEV/'baseline'
OUT=ROOT/'source_assets/blender/pipe_materials_v2';PRE=OUT/'previews'
A=ROOT/'src/main/resources/assets/domesurvival'
IDS=['basic_energy_pipe','reinforced_energy_pipe','high_voltage_energy_pipe','basic_fluid_pipe','reinforced_fluid_pipe','high_pressure_fluid_pipe']
DENSITY=16;PAD=4
spec=importlib.util.spec_from_file_location('pipe_studio',Path(__file__).with_name('fluid_pipe_helpers.py'))
g=importlib.util.module_from_spec(spec);spec.loader.exec_module(g)
g.OUT=OUT;g.PREVIEW=PRE;g.EVIDENCE=DEV
for p in (OUT,PRE,OUT/'textures'):p.mkdir(parents=True,exist_ok=True)
def read(p):return json.loads(p.read_text(encoding='utf-8-sig'))
def dump(p,v):p.parent.mkdir(parents=True,exist_ok=True);p.write_text(json.dumps(v,indent=2)+'\n',encoding='utf-8')
def smooth(a,b,x):t=max(0,min(1,(x-a)/(b-a)));return t*t*(3-2*t)
def mix(a,b,t):return tuple(x+(y-x)*t for x,y in zip(a,b))
def rgb(s):return tuple(int(s[i:i+2],16)/255 for i in (0,2,4))
def lin(v):return v/12.92 if v<=.04045 else ((v+.055)/1.055)**2.4
def srgb(v):return v*12.92 if v<=.0031308 else 1.055*v**(1/2.4)-.055

# The generated source is only quiet surface grain. All geometry-aligned detail is authored below.
source=bpy.data.images.load(str(OUT/'satin_steel_source.png'));source.colorspace_settings.name='sRGB'
source_w,source_h=source.size;source_pixels=list(source.pixels[:])
def grain(x,y):
    i=(((y*7)%source_h)*source_w+(x*7)%source_w)*4
    return (srgb(sum(source_pixels[i:i+3])/3)-.57)*.055

def polygon(x,y,points):
    inside=False
    for a,b in zip(points,points[1:]+points[:1]):
        if (a[1]>y)!=(b[1]>y) and x<(b[0]-a[0])*(y-a[1])/(b[1]-a[1])+a[0]:inside=not inside
    return inside

def face_pixel(x,y,w,h,role,kind,tier):
    # Continuous shading, kept in sRGB until Blender image assignment.
    u=(x+.5)/w;v=(y+.5)/h;short=min(w,h);edge=min(x+.5,y+.5,w-x-.5,h-y-.5)
    steel=rgb(['71817F','788B90','556B73'][tier-1]);body=rgb(['52615F','4A5E65','344A55'][tier-1])
    accent=rgb('C2A264' if kind=='energy' else '528FAC')
    base={'body':body,'core':steel,'panel':rgb('28353A') if kind=='energy' else accent,'rail':steel,'end':steel,'edge':rgb('283235')}[role]
    across=u if w<h else v
    light=.82+.24*math.sin(math.pi*across)+.06*(1-v)
    if role in ('core','panel','end'):light=.9+.16*(1-v)+.035*math.cos(2*math.pi*u)
    # Narrow light chamfer and dark outer seam. No large random contrasting squares.
    bevel=smooth(.4,2.8,edge);light*=.59+.41*bevel
    if 1<edge<3.5:light+=.2*(1-v)*smooth(1,2,edge)
    c=tuple(max(0,min(1,a*light+grain(x,y))) for a in base)
    if role=='edge':
        rib=(x if w<h else y)%7
        c=tuple(a*(.84 if rib<1.6 else 1.0) for a in c)
    if role=='body':
        # Subtle straight seam along the length and two machined end sleeves.
        transverse=x if w<h else y;length=y if w<h else x;span=min(w,h);long=max(w,h)
        seam=min(abs(transverse-3.5),abs(transverse-(span-4.5)))
        if seam<.6:c=mix(c,rgb('253137'),.36)
        if min(length,long-1-length)<2.5:c=mix(c,steel,.38)
    if role=='rail' and min(w,h)>8:
        t=x if w<h else y
        if 3<t<5:c=mix(c,accent,.65 if tier==3 else .2)
    badge=(role=='panel' and kind=='energy') or (role=='core' and kind=='fluid')
    if badge and .65<w/h<1.5 and short>=24:
        # Recessed service plate, with four bevelled screws and a restrained service glyph.
        inset=min(u-.11,v-.11,.89-u,.89-v)*short
        if inset>=0:
            t=smooth(0,2,inset)
            c=mix(rgb('99A9AA'),rgb('28373F'),t)
            c=tuple(a*(1.03-.12*v)+grain(x,y)*.35 for a in c)
        if .18<u<.22 and .24<v<.70:c=mix(c,accent,.85)
        gx=(u-.34)/.34;gy=(v-.26)/.40
        mark=False
        if 0<=gx<=1 and 0<=gy<=1:
            if kind=='energy':mark=polygon(gx,gy,[(.58,0),(.12,.57),(.46,.57),(.31,1),(.91,.38),(.56,.38),(.77,0)])
            else:
                mark=((gx-.5)**2+(gy-.68)**2<.28**2) or (gy<.68 and gy>.04 and abs(gx-.5)<(.28*(gy-.04)/.64))
        if mark:c=mix(accent,rgb('D2DFDB'),.16*(1-gy))
        for n in range(tier):
            cx=.5+(n-(tier-1)/2)*.10
            if abs(u-cx)<.019 and .75<v<.80:c=rgb('A7BFC2') if kind=='fluid' else accent
        for cx,cy in ((.07,.07),(.93,.07),(.07,.93),(.93,.93)):
            dx=(u-cx)*short;dy=(v-cy)*short;r=math.hypot(dx,dy)
            if r<short*.039:
                c=tuple(a*(.67+.45*(1-dy/(short*.08))) for a in rgb('A8B8B8'))
                if abs(dy)<.65 and abs(dx)<short*.022:c=rgb('34434B')
    elif role=='end' and short>=22 and .7<w/h<1.4:
        inset=min(u-.17,v-.17,.83-u,.83-v)*short
        if inset>0:c=mix(rgb('222E33'),body,smooth(1.5,5,inset))
        if .42<u<.58 and .42<v<.58:c=mix(c,accent,.3)
        for cx,cy in ((.09,.09),(.91,.09),(.09,.91),(.91,.91)):
            dx=(u-cx)*short;dy=(v-cy)*short
            if dx*dx+dy*dy<2.8**2:c=rgb('B0BFBE') if dy<-.4 else rgb('435359')
    elif role=='panel' and kind=='fluid':
        # Flange band: restrained enamel with a narrow machined rim.
        transverse=x if w<h else y;span=min(w,h)
        if min(transverse,span-1-transverse)<1.6:c=mix(c,steel,.7)
    return tuple(max(0,min(1,a)) for a in c)

def face_key(e,d):
    w,h=g.face_size(e,d)
    return (e['faces'][d]['texture'][1:],round(w*DENSITY),round(h*DENSITY))

def make_atlas(id,models,kind,tier):
    keys=sorted({face_key(e,d) for m in models.values() for e in m['elements'] for d in e['faces']},key=lambda k:(-k[2],-k[1],k[0]))
    size=256
    while True:
        x=y=row=0;layout={}
        for role,w,h in keys:
            if x+w+2*PAD>size:x=0;y+=row;row=0
            layout[(role,w,h)]=(x+PAD,y+PAD,w,h);x+=w+2*PAD;row=max(row,h+2*PAD)
        if y+row<=size:break
        size*=2
    # Generated byte images store these channel values directly on Image.save().
    # Supply sRGB values here; applying a second linear conversion crushes game midtones.
    pixels=[.2,.25,.27,1.0]*(size*size)
    for (role,w,h),(ox,oy,_,_) in layout.items():
        for y in range(-PAD,h+PAD):
            for x in range(-PAD,w+PAD):
                c=face_pixel(max(0,min(w-1,x)),max(0,min(h-1,y)),w,h,role,kind,tier)
                index=((size-1-(oy+y))*size+ox+x)*4;pixels[index:index+4]=[*c,1.0]
    image=bpy.data.images.new(id+'_atlas',width=size,height=size,alpha=True)
    image.colorspace_settings.name='sRGB';image.pixels.foreach_set(pixels)
    file=OUT/'textures'/f'{id}.png';image.filepath_raw=str(file);image.file_format='PNG';image.save()
    dest=A/f'textures/block/pipe_materials_v2/{id}.png';dest.parent.mkdir(parents=True,exist_ok=True);shutil.copyfile(file,dest)
    for model in models.values():
        for e in model['elements']:
            for d,f in e['faces'].items():
                ox,oy,w,h=layout[face_key(e,d)]
                f['uv']=[v*16/size for v in (ox,oy,ox+w,oy+h)];f.pop('rotation',None)
        for role in list(model['textures']):
            if role!='particle':model['textures'][role]=f'domesurvival:block/pipe_materials_v2/{id}'
    return size,layout

def specs(models):
    result={'cap':[]}
    for part in ('core','arm'):
        result[part]=[]
        for e in models[part]['elements']:
            lo,hi=e['from'],e['to'];result[part].append({'name':e['name'],'minimum':[lo[0]-8,8-hi[2],lo[1]-8],'maximum':[hi[0]-8,8-lo[2],hi[1]-8],'faces':[g.MC_FACES[d] for d in e['faces']]})
    return result

def texture(objects,models,id,offset=(0,0,0),old=False):
    byname={e['name']:e for p in ('core','arm') for e in models[p]['elements']};materials={}
    for role,ref in models['core']['textures'].items():
        if role=='particle':continue
        m=g.material(id+'_'+role,(.5,.5,.5));bsdf=m.node_tree.nodes.get('Principled BSDF');bsdf.inputs['Roughness'].default_value=.76
        n=m.node_tree.nodes.new('ShaderNodeTexImage');n.image=bpy.data.images.load(str(A/('textures/'+ref.split(':')[1]+'.png')),check_existing=False);n.image.pack();n.interpolation='Closest';m.node_tree.links.new(n.outputs['Color'],bsdf.inputs['Base Color']);materials[role]=m
    normals={(1,0,0):'east',(-1,0,0):'west',(0,1,0):'up',(0,-1,0):'down',(0,0,1):'south',(0,0,-1):'north'}
    for obj in objects:
        e=byname[obj['source_part']];q=g.ROTATIONS[obj['connection_property']] if obj.get('connection_property') else Quaternion()
        mesh=obj.data;mesh.materials.clear()
        for m in materials.values():mesh.materials.append(m)
        uv=mesh.uv_layers.new(name='Minecraft_material_UV')
        for poly in mesh.polygons:
            n=q.inverted()@poly.normal;d=normals[(round(n.x),round(n.z),round(-n.y))];f=e['faces'][d];poly.material_index=list(materials).index(f['texture'][1:]);coords=[]
            for loop in poly.loop_indices:
                v=q.inverted()@(mesh.vertices[mesh.loops[loop].vertex_index].co-Vector(offset));coords.append(g.project((v.x+8,v.z+8,8-v.y),d))
            xmin,xmax=min(c[0] for c in coords),max(c[0] for c in coords);ymin,ymax=min(c[1] for c in coords),max(c[1] for c in coords)
            for loop,(x,y) in zip(poly.loop_indices,coords):
                s,t=(x-xmin)/(xmax-xmin),(y-ymin)/(ymax-ymin)
                for _ in range(f.get('rotation',0)//90):s,t=t,1-s
                a,b,c,d=f['uv'];uv.data[loop].uv=((a+(c-a)*s)/16,1-(b+(d-b)*t)/16)

def blockbench(id,model,size):
    uid=lambda s:str(uuid.uuid5(uuid.NAMESPACE_URL,id+'/'+s))
    elements=[]
    for i,e in enumerate(model['elements']):
        faces={d:{**copy.deepcopy(f),'texture':0} for d,f in e['faces'].items()}
        for d in ('north','south','east','west','up','down'):
            if d not in faces:faces[d]={'uv':[0,0,0,0],'texture':None}
        for f in faces.values():f['uv']=[v*size/16 for v in f['uv']]
        elements.append({'name':e.get('name',str(i)),'uuid':uid(str(i)),'type':'cube','from':e['from'],'to':e['to'],'origin':[8,8,8],'rotation':[0,0,0],'faces':faces,'box_uv':False,'visibility':True,'export':True})
    image=OUT/'textures'/f'{id}.png';item=read(BASE/f'models/item/{id}.json')
    dump(ROOT/f'source_assets/blockbench/pipe_materials_v2/{id}.bbmodel',{'meta':{'format_version':'4.10','model_format':'java_block','box_uv':False},'name':id+'_material_v2','resolution':{'width':size,'height':size},'elements':elements,'outliner':[e['uuid'] for e in elements],'textures':[{'name':id+'.png','id':'0','uuid':uid('atlas'),'width':size,'height':size,'uv_width':size,'uv_height':size,'source':'data:image/png;base64,'+base64.b64encode(image.read_bytes()).decode(),'mode':'bitmap'}],'display':item['display']})

all_models={};manifest={}
for index,id in enumerate(IDS):
    kind='energy' if index<3 else 'fluid';tier=index%3+1
    models={part:read(BASE/f'models/block/{id}_{part}.json') for part in ('core','arm','inventory')}
    size,layout=make_atlas(id,models,kind,tier);all_models[index]=models
    for part,model in models.items():dump(A/f'models/block/{id}_{part}.json',model)
    g.SPECS[index]=specs(models)
    g.reset_scene();g.camera_setup(1400,1000,29)
    objects=g.assembly(index,{'north','south'},prefix=id);texture(objects,models,id)
    g.label(id.replace('_',' ').upper(),-13,10,1)
    bpy.context.scene['stage']='Material V2 / original production frame unchanged';bpy.context.scene['minecraft_units_per_block']=16
    bpy.ops.wm.save_as_mainfile(filepath=str(OUT/f'{id}.blend'))
    blockbench(id,models['inventory'],size)
    manifest[id]={'atlas_size':size,'texels_per_model_unit':DENSITY,'islands':[{'role':k[0],'x':v[0],'y':v[1],'width':v[2],'height':v[3]} for k,v in layout.items()],'core_quads':sum(len(e['faces']) for e in models['core']['elements']),'arm_quads':sum(len(e['faces']) for e in models['arm']['elements'])}
g.reset_scene();g.camera_setup(2100,1500,72)
g.label('DOMESURVIVAL / REFINED PIPE MATERIALS',-33,23,1.4)
for index,id in enumerate(IDS):
    x=(index%3-1)*23;y=11 if index<3 else -10
    offset=g.screen_point(x,y);objects=g.assembly(index,{'north','south'},offset,prefix=id);texture(objects,all_models[index],id,offset)
    g.label(('ENERGY' if index<3 else 'FLUID')+f' / TIER {index%3+1}',x-7,y-7.3,.85)
g.label('SATIN STEEL / MACHINED SEAMS / ENAMEL SERVICE MARKS',-33,-23,.72,secondary=True)
g.save_render('01_refined_family',True)
g.reset_scene();g.camera_setup(1800,1200,60)
g.label('SAME FRAME / MATERIAL COMPARISON',-27,17,1.2)
for index,y in ((1,7),(4,-10)):
    id=IDS[index];old={p:read(BASE/f'models/block/{id}_{p}.json') for p in ('core','arm')}
    for x,models,label in ((-15,old,'BEFORE'),(15,all_models[index],'REFINED')):
        offset=g.screen_point(x,y);objects=g.assembly(index,{'north','east'},offset,prefix=label+id);texture(objects,models,id,offset,old=label=='BEFORE');g.label(label,x-5,y-6,.85)
g.save_render('02_before_after',True)
pack=ROOT/'run/pipe-materials-v2/resourcepacks/pipe_materials_v2_before'
dump(pack/'pack.mcmeta',{'pack':{'pack_format':15,'description':'DOMESURVIVAL pipes before material refinement'}})
for file in BASE.rglob('*'):
    if file.is_file():target=pack/'assets/domesurvival'/file.relative_to(BASE);target.parent.mkdir(parents=True,exist_ok=True);shutil.copyfile(file,target)
dump(DEV/'material_manifest.json',manifest)
dump(DEV/'blender_result.json',{'status':'PASS','blender':bpy.app.version_string,'masters':6,'geometry_source':'unchanged baseline JSON frames','atlases':6})
print('MATERIAL_V2_GENERATION_PASS')
