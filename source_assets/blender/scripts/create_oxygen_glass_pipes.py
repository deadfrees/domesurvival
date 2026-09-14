"""Thin Galacticraft-inspired glass oxygen pipes. Author all game assets with Blender Python.
No ticking entities or changes to oxygen transfer. All dimensions are Minecraft model units.
"""
from pathlib import Path
import sys,copy,json,math,shutil,base64,uuid
import bpy
from mathutils import Quaternion,Vector
sys.path.insert(0,str(Path(__file__).parent))
import fluid_pipe_helpers as g
ROOT=Path(__file__).resolve().parents[3];A=ROOT/'src/main/resources/assets/domesurvival'
DEV=ROOT/'dev/oxygen_pipe_visual';BASE=DEV/'baseline';OUT=ROOT/'source_assets/blender/oxygen_pipes'
g.OUT=OUT;g.PREVIEW=OUT/'previews';g.EVIDENCE=DEV
for p in (OUT,OUT/'textures',g.PREVIEW):p.mkdir(parents=True,exist_ok=True)
IDS=['oxygen_pipe','reinforced_oxygen_pipe','high_flow_oxygen_pipe'];FACES=['north','south','east','west','up','down']
ROT={'north':{},'east':{'y':90},'south':{'y':180},'west':{'y':270},'up':{'x':270},'down':{'x':90}}
def dump(p,v):p.parent.mkdir(parents=True,exist_ok=True);p.write_text(json.dumps(v,indent=2)+'\n',encoding='utf-8')
def read(p):return json.loads(p.read_text(encoding='utf-8-sig'))
def rgb(s):return tuple(int(s[i:i+2],16)/255 for i in (0,2,4))
def mix(a,b,t):return tuple(x+(y-x)*t for x,y in zip(a,b))
def cube(name,lo,hi,role,faces=FACES):return {'name':name,'from':lo,'to':hi,'faces':{d:{'texture':'#'+role} for d in faces}}
def models(tier):
    # Unconnected directions close only their own face. Connected directions remain open.
    core=[cube('junction_face',[7,7,7],[9,9,9],'glass',['north'])]
    arm=[cube('glass_tube',[7,7,0],[9,9,7],'glass',['east','west','up','down'])]
    for ring in range(tier+1):
        a=ring*.48;b=a+.30
        arm += [cube(f'collar{ring}_left',[6.75,6.75,a],[7,9.25,b],'metal'),cube(f'collar{ring}_right',[9,6.75,a],[9.25,9.25,b],'metal'),
                cube(f'collar{ring}_bottom',[7,6.75,a],[9,7,b],'metal'),cube(f'collar{ring}_top',[7,9,a],[9,9.25,b],'metal')]
    inv=[cube('inventory_junction',[7,7,7],[9,9,9],'glass',['east','west','up','down'])]+copy.deepcopy(arm)
    for e in arm:
        e=copy.deepcopy(e);e['name']='south_'+e['name'];e['from'][2],e['to'][2]=16-e['to'][2],16-e['from'][2]
        inv.append(e)
    return {p:{'ambientocclusion':False,'textures':{},'elements':e} for p,e in [('core',core),('arm',arm),('inventory',inv)]}
def key(e,d):
    w,h=g.face_size(e,d);return e['faces'][d]['texture'][1:],max(1,round(w*16)),max(1,round(h*16))
def pixel(x,y,w,h,role,tier):
    u=(x+.5)/w;v=(y+.5)/h;edge=min(x+.5,y+.5,w-x-.5,h-y-.5)
    if role=='glass':
        transverse=u if w<h else v
        tint=rgb(['B9DED4','9ACBDC','C7B5DC'][tier])
        c=mix(tint,rgb('EAF7F3'),.45*(1-transverse));alpha=.12+.05*(1-transverse)
        # Polished glass edges and one narrow, low-contrast reflection.
        if edge<1.0:alpha=.68;c=rgb(['DDEFEA','9CDBEF','D5BCEB'][tier])
        elif edge<2.0:alpha=.30
        if abs(transverse-.23)<.021:alpha=.28;c=rgb('ECF9F6')
        return (*c,alpha)
    base=rgb(['A5C6BA','488DAC','8E75B0'][tier]);accent=rgb(['D8EAE1','84D3EF','D8BCF1'][tier])
    across=u if w<h else v;light=.80+.21*math.sin(math.pi*across)+.09*(1-v)
    if edge<1.1:light*=.7
    c=tuple(a*light for a in base)
    short=min(w,h);length=max(w,h);t=y if w<h else x;s=x if w<h else y
    if short>=5:
        if 1<s<3:c=mix(c,accent,.85)
        for n in range(tier+1):
            cx=length*.5+(n-tier/2)*4
            if abs(t-cx)<1 and short*.35<s<short*.8:c=rgb('D8ECE5')
    return (*c,1.)
def atlases(id,ms,tier):
    keys=sorted({key(e,d) for m in ms.values() for e in m['elements'] for d in e['faces']},key=lambda k:(-k[2],-k[1]))
    size=256;pad=4;x=y=row=0;layout={}
    for k in keys:
        role,w,h=k
        if x+w+pad*2>size:x=0;y+=row;row=0
        layout[k]=(x+pad,y+pad,w,h);x+=w+pad*2;row=max(row,h+pad*2)
    assert y+row<=size
    buffers={n:[0.]*(size*size*4) for n in ('full','metal','glass')}
    for (role,w,h),(ox,oy,_,_) in layout.items():
        for yy in range(-pad,h+pad):
            for xx in range(-pad,w+pad):
                rgba=pixel(max(0,min(w-1,xx)),max(0,min(h-1,yy)),w,h,role,tier)
                i=((size-1-oy-yy)*size+ox+xx)*4
                buffers['full'][i:i+4]=rgba;buffers[role][i:i+4]=rgba
    for layer,pixels in buffers.items():
        name=id+('' if layer=='full' else '_'+layer)+'.png'
        image=bpy.data.images.new(name,width=size,height=size,alpha=True);image.colorspace_settings.name='sRGB';image.pixels.foreach_set(pixels)
        image.filepath_raw=str(OUT/'textures'/name);image.file_format='PNG';image.save()
        dest=A/'textures/block/oxygen_glass'/name;dest.parent.mkdir(parents=True,exist_ok=True);shutil.copyfile(OUT/'textures'/name,dest)
    particle=bpy.data.images.new(id+'_particle',width=16,height=16,alpha=True)
    particle.pixels.foreach_set([c for y in range(16) for x in range(16) for c in (*mix(rgb('A0C4CB'),rgb('DEEFEB'),.25+.35*(15-y)/15),1.)])
    particle.filepath_raw=str(OUT/'textures'/f'{id}_particle.png');particle.file_format='PNG';particle.save()
    shutil.copyfile(OUT/'textures'/f'{id}_particle.png',A/'textures/block/oxygen_glass'/f'{id}_particle.png')
    for part,m in ms.items():
        for e in m['elements']:
            for d,f in e['faces'].items():
                ox,oy,w,h=layout[key(e,d)];f['uv']=[n*16/size for n in (ox,oy,ox+w,oy+h)]
        m['textures']={r:f'domesurvival:block/oxygen_glass/{id}' for r in ('glass','metal')};m['textures']['particle']=f'domesurvival:block/oxygen_glass/{id}_particle'
        result={'loader':'forge:composite','ambientocclusion':False,'textures':m['textures'],'children':{},'item_render_order':[]}
        for role,render in [('metal','minecraft:cutout'),('glass','minecraft:translucent')]:
            elements=[e for e in m['elements'] if any(f['texture']=='#'+role for f in e['faces'].values())]
            if elements:
                result['children'][role]={'render_type':render,'ambientocclusion':False,'textures':{role:f'domesurvival:block/oxygen_glass/{id}_{role}'},'elements':elements}
                result['item_render_order'].append(role)
        name='connection' if part=='arm' else part
        dump(A/f'models/block/{id}_{name}.json',result)
    state={'multipart':[]}
    for direction,rot in ROT.items():
        for connected,part in [('false','core'),('true','connection')]:
            state['multipart'].append({'when':{direction:connected},'apply':{'model':f'domesurvival:block/{id}_{part}',**rot,'uvlock':False}})
    dump(A/f'blockstates/{id}.json',state)
    return size,layout
def assembly(ms,id,connections,offset=(0,0,0)):
    mats={}
    for role in ('glass','metal'):
        mat=g.material(id+'_'+role,(.7,.8,.8));bsdf=mat.node_tree.nodes.get('Principled BSDF');bsdf.inputs['Roughness'].default_value=.30 if role=='glass' else .72
        tex=mat.node_tree.nodes.new('ShaderNodeTexImage');tex.image=bpy.data.images.load(str(OUT/'textures'/f'{id}_{role}.png'));tex.image.pack();tex.interpolation='Closest'
        mat.node_tree.links.new(tex.outputs['Color'],bsdf.inputs['Base Color']);mat.node_tree.links.new(tex.outputs['Alpha'],bsdf.inputs['Alpha']);mats[role]=mat
    normals={(1,0,0):'east',(-1,0,0):'west',(0,1,0):'up',(0,-1,0):'down',(0,0,1):'south',(0,0,-1):'north'}
    objects=[]
    for direction in FACES:
        q=g.ROTATIONS[direction]
        for e in ms['arm' if direction in connections else 'core']['elements']:
            lo,hi=e['from'],e['to'];role=next(iter(e['faces'].values()))['texture'][1:]
            spec={'name':e['name'],'minimum':[lo[0]-8,8-hi[2],lo[1]-8],'maximum':[hi[0]-8,8-lo[2],hi[1]-8],'faces':[g.MC_FACES[d] for d in e['faces']]}
            obj=g.mesh_box(spec,id+'_'+direction+'_'+e['name'],mats[role],q,offset);objects.append(obj);uv=obj.data.uv_layers.new(name='Minecraft_UV')
            for poly in obj.data.polygons:
                n=q.inverted()@poly.normal;d=normals[(round(n.x),round(n.z),round(-n.y))];f=e['faces'][d];coords=[]
                for loop in poly.loop_indices:
                    v=q.inverted()@(obj.data.vertices[obj.data.loops[loop].vertex_index].co-Vector(offset));coords.append(g.project((v.x+8,v.z+8,8-v.y),d))
                x0,x1=min(c[0] for c in coords),max(c[0] for c in coords);y0,y1=min(c[1] for c in coords),max(c[1] for c in coords)
                for loop,(x,y) in zip(poly.loop_indices,coords):
                    s,t=(x-x0)/(x1-x0),(y-y0)/(y1-y0);a,b,c,d=f['uv'];uv.data[loop].uv=((a+(c-a)*s)/16,1-(b+(d-b)*t)/16)
    return objects
def blockbench(id,m,size):
    uid=lambda s:str(uuid.uuid5(uuid.NAMESPACE_URL,id+'/'+s));elements=[]
    for i,e in enumerate(m['elements']):
        faces={d:{'uv':[n*size/16 for n in f['uv']],'texture':0} for d,f in e['faces'].items()}
        for d in FACES:faces.setdefault(d,{'uv':[0,0,0,0],'texture':None})
        elements.append({**e,'uuid':uid(str(i)),'type':'cube','faces':faces,'box_uv':False,'origin':[8,8,8],'rotation':[0,0,0],'visibility':True,'export':True})
    dump(ROOT/f'source_assets/blockbench/oxygen_pipes/{id}.bbmodel',{'meta':{'format_version':'4.10','model_format':'java_block','box_uv':False},'name':id,'resolution':{'width':size,'height':size},'elements':elements,'outliner':[e['uuid'] for e in elements],
        'textures':[{'id':'0','uuid':uid('atlas'),'name':id+'.png','width':size,'height':size,'uv_width':size,'uv_height':size,'source':'data:image/png;base64,'+base64.b64encode((OUT/'textures'/f'{id}.png').read_bytes()).decode(),'mode':'bitmap'}],
        'display':read(BASE/f'models/item/{id}.json')['display']})
# Seamless density pattern for the real-transfer gas effect, drawn only inside active pipes.
gas=bpy.data.images.new('oxygen_gas_flow',width=128,height=128,alpha=True);pixels=[]
for y in range(128):
    for x in range(128):
        u=(x+.5)/128;v=(y+.5)/128
        across=max(0,1-((u-.5)/.5)**2)**1.8
        density=.55+.25*math.cos(2*math.pi*v)+.12*math.cos(4*math.pi*v+.8*u)
        pixels.extend((1.,1.,1.,across*density))
gas.pixels.foreach_set(pixels);gas.filepath_raw=str(OUT/'textures/oxygen_gas_flow.png');gas.file_format='PNG';gas.save()
dest=A/'textures/block/oxygen_glass/oxygen_gas_flow.png';dest.parent.mkdir(parents=True,exist_ok=True);shutil.copyfile(OUT/'textures/oxygen_gas_flow.png',dest)
all_models={};manifest={}
for tier,id in enumerate(IDS):
    ms=models(tier);size,layout=atlases(id,ms,tier);all_models[id]=ms
    g.reset_scene();g.camera_setup(1300,900,25);assembly(ms,id,{'north','south'})
    g.label(id.replace('_',' ').upper(),-11,9,.8)
    bpy.context.scene['description']='2 MC-unit glass bore; 2.5-unit satin compression rings; original oxygen transport retained.'
    bpy.ops.wm.save_as_mainfile(filepath=str(OUT/f'{id}.blend'));blockbench(id,ms['inventory'],size)
    manifest[id]={'bore_width':2,'collar_width':2.5,'rings':tier+1,'atlas_size':size,'transfer_rate':[30,60,120][tier],'core_face_quads':1,'connection_quads':4+24*(tier+1)}
g.reset_scene();g.camera_setup(1800,1200,67)
g.label('DOMESURVIVAL / THIN GLASS OXYGEN PIPES',-30,20,1.05)
for i,id in enumerate(IDS):
    x=(i-1)*21
    for y,connections in [(8,{'north','south'}),(-8,{'north','east'})]:assembly(all_models[id],id,connections,g.screen_point(x,y))
    g.label(['BASIC / 30 O2','REINFORCED / 60 O2','HIGH FLOW / 120 O2'][i],x-7,-16,.7)
g.label('2 PX GLASS / SMALL SATIN RINGS / OPEN CONNECTED ENDS',-30,-22,.65,secondary=True)
g.save_render('01_oxygen_glass_family',True);dump(DEV/'manifest.json',manifest)
print('OXYGEN GLASS FAMILY: three thin tiers authored')
