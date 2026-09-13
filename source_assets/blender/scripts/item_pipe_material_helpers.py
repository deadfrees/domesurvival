"""Item pipe studio utilities, derived from the approved material V2 pipeline. No production writes on import."""
from pathlib import Path
import base64,copy,hashlib,importlib.util,json,math,shutil,uuid
import bpy
from mathutils import Vector,Quaternion
ROOT=Path(__file__).resolve().parents[3]
DEV=ROOT/'dev/item_pipe_visual';BASE=DEV/'baseline'
OUT=ROOT/'source_assets/blender/item_pipes';PRE=OUT/'previews'
A=ROOT/'src/main/resources/assets/domesurvival'
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
source=bpy.data.images.load(str(ROOT/'source_assets/blender/pipe_materials_v2/satin_steel_source.png'));source.colorspace_settings.name='sRGB'
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

def specs(models):
    result={'cap':[]}
    for part in ('core','arm'):
        result[part]=[]
        for e in models[part]['elements']:
            lo,hi=e['from'],e['to'];result[part].append({'name':e['name'],'minimum':[lo[0]-8,8-hi[2],lo[1]-8],'maximum':[hi[0]-8,8-lo[2],hi[1]-8],'faces':[g.MC_FACES[d] for d in e['faces']]})
    return result

def texture(objects,models,id,offset=(0,0,0),old=False):
    byname={e['name']:e for p in ('core','arm') for e in models[p]['elements']};materials={}
    for role,ref in {k:v for model in models.values() for k,v in model['textures'].items()}.items():
        if role=='particle':continue
        m=g.material(id+'_'+role,(.5,.5,.5));bsdf=m.node_tree.nodes.get('Principled BSDF');bsdf.inputs['Roughness'].default_value=.76
        n=m.node_tree.nodes.new('ShaderNodeTexImage');n.image=bpy.data.images.load(str(A/('textures/'+ref.split(':')[1]+'.png')),check_existing=False);n.image.pack();n.interpolation='Closest';m.node_tree.links.new(n.outputs['Color'],bsdf.inputs['Base Color']);m.node_tree.links.new(n.outputs['Alpha'],bsdf.inputs['Alpha']);materials[role]=m
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
    dump(ROOT/f'source_assets/blockbench/item_pipes/{id}.bbmodel',{'meta':{'format_version':'4.10','model_format':'java_block','box_uv':False},'name':id+'_material_v2','resolution':{'width':size,'height':size},'elements':elements,'outliner':[e['uuid'] for e in elements],'textures':[{'name':id+'.png','id':'0','uuid':uid('atlas'),'width':size,'height':size,'uv_width':size,'uv_height':size,'source':'data:image/png;base64,'+base64.b64encode(image.read_bytes()).decode(),'mode':'bitmap'}],'display':item['display']})
