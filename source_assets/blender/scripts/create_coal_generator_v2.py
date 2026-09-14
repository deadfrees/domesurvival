"""Coal generator art pass, authored/exported with Blender Python. No gameplay edits.

The existing one-block furnace, north-facing firebox, lit state and configurable
world-side ports are retained. Shared machine port resources are never overwritten.
"""
from pathlib import Path
import bpy,sys,json,math,copy,shutil,hashlib,base64,uuid
from mathutils import Vector,Quaternion
sys.path.insert(0,str(Path(__file__).parent))
import fluid_pipe_helpers as g
ROOT=Path(__file__).resolve().parents[3];A=ROOT/'src/main/resources/assets/domesurvival'
DEV=ROOT/'dev/coal_generator_visual';OUT=ROOT/'source_assets/blender/coal_generator'
TEX=A/'textures/block/coal_generator_v2';MODEL=A/'models/block'
FACES=['north','south','east','west','up','down']
ROT={'north':{},'east':{'y':90},'south':{'y':180},'west':{'y':270},'up':{'x':270},'down':{'x':90}}
g.OUT=OUT;g.PREVIEW=OUT/'previews';g.EVIDENCE=DEV
for p in (DEV,OUT/'textures',g.PREVIEW,TEX):p.mkdir(parents=True,exist_ok=True)
def dump(p,v):p.parent.mkdir(parents=True,exist_ok=True);p.write_text(json.dumps(v,indent=2)+'\n',encoding='utf-8')
def read(p):return json.loads(p.read_text(encoding='utf-8-sig'))
if not (DEV/'baseline_hashes.json').exists():
    dump(DEV/'baseline_hashes.json',{p.relative_to(ROOT).as_posix():hashlib.sha256(p.read_bytes()).hexdigest() for p in (ROOT/'src/main').rglob('*') if p.is_file()})
    for folder in ('blockstates','models/block','models/item','textures/block'):
        for p in (A/folder).glob('coal_generator*'):
            if p.is_file():
                dest=DEV/'baseline'/p.relative_to(A);dest.parent.mkdir(parents=True,exist_ok=True);shutil.copyfile(p,dest)
def rgb(s):return tuple(int(s[i:i+2],16)/255 for i in (0,2,4))
def mix(a,b,t):return tuple(x+(y-x)*max(0,min(1,t)) for x,y in zip(a,b))
def cube(name,lo,hi,role,faces=FACES):return {'name':name,'from':lo,'to':hi,'faces':{d:{'texture':'#'+role} for d in faces}}
def turn(e,d):
    e=copy.deepcopy(e);q=g.ROTATIONS[d]
    def rotate(v):
        x,y,z=v
        # Exact quarter turns avoid quaternion float drift at 0/16 block boundaries.
        return {'north':(x,y,z),'east':(16-z,y,x),'south':(16-x,y,16-z),
                'west':(z,y,16-x),'up':(x,16-z,y),'down':(x,z,16-y)}[d]
    corners=[rotate((x,y,z)) for x in (e['from'][0],e['to'][0]) for y in (e['from'][1],e['to'][1]) for z in (e['from'][2],e['to'][2])]
    e['from']=[min(v[i] for v in corners) for i in range(3)];e['to']=[max(v[i] for v in corners) for i in range(3)]
    face_map={}
    for f in e['faces']:
        n=q@Vector(g.DIRECTIONS[f]);target=min(FACES,key=lambda t:(n-Vector(g.DIRECTIONS[t])).length)
        face_map[target]=e['faces'][f]
    e['faces']=face_map;e['name']=d+'_'+e['name'];return e
def body():
    es=[cube('cast_body',[.6,.75,4.85],[15.4,15.35,15.4],'body')]
    for name,lo,hi in [('left',[.6,.75,.65],[3.4,15.35,4.85]),('right',[11.95,.75,.65],[15.4,15.35,4.85]),
                       ('bottom',[3.4,.75,.65],[11.95,4.25,4.85]),('top',[3.4,10.85,.65],[11.95,15.35,4.85])]:
        es.append(cube('hollow_body_'+name,lo,hi,'body'))
    for x in (0,15.15):
        for z in (0,15.15):es.append(cube('corner_post',[x,.4,z],[x+.85,15.7,z+.85],'frame'))
    for y in (.35,15.15):
        for z in (0,15.15):es.append(cube('front_rear_crossmember',[.85,y,z],[15.15,y+.55,z+.85],'frame'))
        for x in (0,15.15):es.append(cube('side_crossmember',[x,y,.85],[x+.85,y+.55,15.15],'frame'))
    for x in (1,12.8):es.append(cube('mounting_skid',[x,0,.35],[x+2.2,.65,15.65],'black'))
    # Recessed front fire door and instrument header, retaining the old face layout.
    es += [cube('front_service_plate',[1.3,1.45,.39],[14.7,14.95,.64],'panel', ['north','east','west','up','down']),
           cube('door_shadow',[2.3,3.4,.28],[13.6,12,.38],'black',['north']),
           cube('door_left_jamb',[2.65,3.7,0],[3.4,11.55,.28],'trim'),
           cube('door_right_jamb',[11.95,3.7,0],[12.7,11.55,.28],'trim'),
           cube('door_sill',[3.4,3.7,0],[11.95,4.25,.28],'trim'),
           cube('door_lintel',[3.4,10.85,0],[11.95,11.55,.28],'trim'),
           cube('dial_bezel',[2.8,12.0,.10],[5.8,14.75,.38],'trim'),
           cube('temperature_dial',[3,12.2,.075],[5.6,14.55,.09],'dial',['north']),
           cube('nameplate',[6.55,12.65,.12],[11.9,14.55,.37],'brass'),
           cube('serial_stamp',[6.7,12.8,.09],[11.75,14.4,.11],'badge',['north']),
           cube('run_indicator',[12.65,12.7,.08],[13.45,14.45,.36],'status',['north']),
           cube('ash_drawer',[3.5,1.95,.12],[12.5,3.0,.35],'trim'),
           cube('ash_drawer_pull',[6.65,2.15,0],[9.35,2.5,.14],'black')]
    for x in (4.4,6.15,7.9,9.65,11.25):es.append(cube('cast_fire_grate',[x,4.25,.05],[x+.24,10.85,.22],'frame'))
    for y in (5.0,9.6):es.append(cube('hinge',[1.85,y,0],[2.6,y+1.35,.38],'steel'))
    for y in (6.3,9.05):es.append(cube('handle_mount',[12.75,y,0],[13.85,y+.4,.35],'steel'))
    es.append(cube('insulated_handle',[13.4,6.7,0],[13.9,9.05,.3],'rubber'))
    # Side panels carry a flush closed service socket; active ports cover this cap.
    panel=[cube('service_skin',[1.2,1.5,.30],[14.8,14.95,.59],'panel',['north','east','west','up','down']),
           cube('socket_recess',[4.75,4.75,.20],[11.25,11.25,.28],'black',['north']),
           cube('sealed_socket',[5.3,5.3,.16],[10.7,10.7,.19],'cap',['north']),
           cube('lower_service_stripe',[2.1,2.4,.22],[13.9,2.8,.29],'brass',['north']),
           cube('vent_recess',[3.7,12.05,.21],[12.3,14.3,.28],'black',['north'])]
    for y in (12.35,12.95,13.55):panel.append(cube('cooling_louvre',[4,y,.03],[12,y+.22,.22],'trim',['north','up','down','east','west']))
    for d in ('east','south','west'):es += [turn(e,d) for e in panel]
    roof=[cube('roof_skin',[1.2,1.2,.30],[14.8,14.8,.59],'panel',['north']),
          cube('roof_socket_shadow',[4.7,4.7,.18],[11.3,11.3,.29],'black',['north']),
          cube('roof_sealed_socket',[5.3,5.3,.14],[10.7,10.7,.17],'cap',['north'])]
    for x in (2.1,12.65):
        roof.append(cube('vent_bank_shadow',[x-.2,3,.20],[x+1.45,13,.28],'black',['north']))
        for y in (3.5,4.7,5.9,7.1,8.3,9.5,10.7,11.9):roof.append(cube('roof_louvre',[x,y,.02],[x+1.25,y+.38,.19],'trim',['north','east','west','up','down']))
    es += [turn(e,'up') for e in roof]
    es += [turn(e,'down') for e in roof[:3]]
    for d in FACES:
        for x in (1.5,14):
            for y in (1.0,14.9):es.append(turn(cube('captive_screw',[x,y,.02],[x+.42,y+.42,.25],'bolt',['north','east','west','up','down']),d))
    # Cut a real opening through both front plates, exposing a 4.5-unit deep cavity.
    opened=[]
    for e in es:
        if e['name'] not in ('front_service_plate','door_shadow'):opened.append(e);continue
        x0,y0,z0=e['from'];x1,y1,z1=e['to'];role=next(iter(e['faces'].values()))['texture'][1:]
        for name,lo,hi in [('left',[x0,y0,z0],[3.4,y1,z1]),('right',[11.95,y0,z0],[x1,y1,z1]),
                           ('lower',[3.4,y0,z0],[11.95,4.25,z1]),('upper',[3.4,10.85,z0],[11.95,y1,z1])]:
            opened.append(cube(e['name']+'_'+name,lo,hi,role))
    es=opened
    for name,lo,hi in [('rear',[3.4,4.25,4.6],[11.95,10.85,4.8]),('left',[3.4,4.25,.3],[3.55,10.85,4.6]),
                      ('right',[11.8,4.25,.3],[11.95,10.85,4.6]),('floor',[3.55,4.25,.3],[11.8,4.45,4.6]),
                      ('ceiling',[3.55,10.65,.3],[11.8,10.85,4.6])]:es.append(cube('refractory_'+name,lo,hi,'refractory'))
    # Separate bevelled charcoal chunks, in two staggered depth rows.
    for row,z in enumerate((.65,2.55)):
        for col in range(5):
            x=3.8+col*1.48+row*.13;h=.70+.25*(.5+.5*math.sin(col*2.7+row));w=1.24
            es.append(cube(f'coal_{row}_{col}_base',[x,4.47,z],[x+w,4.47+h,z+1.38],'coal'))
            es.append(cube(f'coal_{row}_{col}_crown',[x+.13,4.47+h,z+.13],[x+w-.13,4.68+h,z+1.25],'coal'))
    for i,e in enumerate(es):e['name']=f'{i:03}_'+e['name']
    return es
def flames():
    es=[]
    for i,(x,z,h) in enumerate(((4.9,1.7,3.7),(7.5,1.6,4.5),(10.3,1.8,3.8),(6,3.5,4.7),(9.3,3.4,4.1))):
        for angle in (-45,45):
            e=cube(f'flame_{i}_{angle}',[x-.77,5.15,z-.01],[x+.77,5.15+h,z+.01],'flame_back' if i>2 else 'flame',['north','south'])
            e['rotation']={'origin':[x,5.15,z],'axis':'y','angle':angle,'rescale':False}
            for f in e['faces'].values():f['uv']=[0,0,16,16]
            es.append(e)
    return es
def port(mode):
    es=[cube('socket_insert',[5.3,5.3,.10],[10.7,10.7,.13],'socket_'+mode,['north'])]
    for n,lo,hi in [('left',[4.85,4.85,.015],[5.3,11.15,.15]),('right',[10.7,4.85,.015],[11.15,11.15,.15]),('lower',[5.3,4.85,.015],[10.7,5.3,.15]),('upper',[5.3,10.7,.015],[10.7,11.15,.15])]:es.append(cube('collar_'+n,lo,hi,'steel'))
    for x in (5.4,9.4):es.append(cube('mode_band',[x,10.85,0],[x+1.2,11.08,.01],mode,['north']))
    return es
PALETTE={'body':'1C2022','panel':'272D30','frame':'151A1D','trim':'4D595E','steel':'65767D','black':'111518','rubber':'1C2022','brass':'A98C58','bolt':'899797','cap':'2B3438','input':'528FAC','output':'C2A264','refractory':'181716'}
SPECIAL={'coal','flame','flame_back','dial','badge','status','socket_input','socket_output'}
# Reuse the same approved grain source, density and steel/accent palette as pipe V2.
grain_image=bpy.data.images.load(str(ROOT/'source_assets/blender/pipe_materials_v2/satin_steel_source.png'))
grain_w,grain_h=grain_image.size;grain_pixels=list(grain_image.pixels[:])
def grain(x,y):
    i=(((y*7)%grain_h)*grain_w+(x*7)%grain_w)*4;v=sum(grain_pixels[i:i+3])/3
    srgb=v*12.92 if v<=.0031308 else 1.055*v**(1/2.4)-.055
    return (srgb-.57)*.055
def key(e,d):
    f=e['faces'][d];w,h=g.face_size(e,d);return f['texture'][1:],max(2,round(w*16)),max(2,round(h*16))
def material_pixel(x,y,w,h,role):
    u=(x+.5)/w;v=(y+.5)/h;edge=min(x+.5,y+.5,w-x-.5,h-y-.5)
    base=rgb(PALETTE[role]);light=.87+.13*(1-v)+.065*math.sin(math.pi*u)
    light*=.63+.37*min(1,edge/2)
    if 1<edge<2.6:light+=.16*(1-v)
    c=tuple(max(0,min(1,a*light+grain(x,y))) for a in base)
    if role=='rubber' and y%5<1:c=tuple(a*.8 for a in c)
    if role=='cap':
        if abs(u-.5)<.025 and .30<v<.70:c=rgb('24343A')
        if .32<u<.68 and abs(v-.5)<.025:c=rgb('24343A')
    if role=='bolt' and abs(u+v-1)<.09 and .2<u<.8:c=rgb('3C4B50')
    return (*c,1)
def save_image(name,w,h,pixels):
    im=bpy.data.images.new(name,width=w,height=h,alpha=True);im.colorspace_settings.name='sRGB';im.pixels.foreach_set(pixels)
    im.filepath_raw=str(OUT/'textures'/f'{name}.png');im.file_format='PNG';im.save();shutil.copyfile(OUT/'textures'/f'{name}.png',TEX/f'{name}.png')
def atlas(all_elements):
    keys=sorted({key(e,d) for e in all_elements for d in e['faces'] if e['faces'][d]['texture'][1:] not in SPECIAL},key=lambda k:(-k[2],-k[1],k[0]))
    size=512;pad=3
    while True:
        x=y=row=0;layout={}
        for k in keys:
            r,w,h=k
            if x+w+pad*2>size:x=0;y+=row;row=0
            layout[k]=(x+pad,y+pad,w,h);x+=w+pad*2;row=max(row,h+pad*2)
        if y+row<=size:break
        size*=2
    pixels=[0.]*(size*size*4)
    for (role,w,h),(ox,oy,_,_) in layout.items():
        for y in range(-pad,h+pad):
            for x in range(-pad,w+pad):
                rgba=material_pixel(max(0,min(w-1,x)),max(0,min(h-1,y)),w,h,role)
                i=((size-1-oy-y)*size+ox+x)*4;pixels[i:i+4]=rgba
    save_image('satin_atlas',size,size,pixels)
    for e in all_elements:
        for d,f in e['faces'].items():
            role=f['texture'][1:]
            if role in SPECIAL:f['uv']=[0,0,16,16]
            else:
                x,y,w,h=layout[key(e,d)];f['uv']=[n*16/size for n in (x,y,x+w,y+h)]
    return size
FONT={'C':['111','100','100','100','111'],'G':['111','100','101','101','111'],'0':['111','101','101','101','111'],'1':['010','110','010','010','111'],'-':['000','000','111','000','000']}
def special_pixel(role,u,v,frame=0,lit=False):
    if role in ('flame','flame_back'):
        if not lit:return (0,0,0,0)
        phase=2*math.pi*frame/8+(1.7 if role=='flame_back' else 0)
        tip=.12+.05*math.sin(phase);t=max(0,min(1,(v-tip)/(1-tip)))
        bend=.12*math.sin(v*7-phase)*(.95-t)
        width=.012+.23*t
        shape=max(0,1-abs(u-.5-bend)/width)
        alpha=min(1,shape*4)*min(1,t*18) if v>tip else 0
        c=mix(rgb('B74716'),rgb('FFE0A0'),shape*.60+t*.35)
        return (*c,alpha)
    if role=='coal':
        edge=min(u,v,1-u,1-v);light=.72+.28*(1-v)+.05*math.sin(u*31+v*27)
        c=tuple(a*light for a in rgb('292B2A'))
        if lit:
            seam=max(0,1-edge/.14);pulse=.65+.25*math.sin(frame*math.pi/2+u*3+v*2)
            c=mix(c,mix(rgb('AC3814'),rgb('F2A444'),seam),seam*pulse)
        return (*c,1)
    if role=='fire':
        # Irregular coal lumps with hot seams, plus short moving flame tongues.
        c=mix(rgb('10181B'),rgb('322820'),v*.5)
        if lit:
            for n,center in enumerate((.15,.37,.61,.84)):
                tip=.23+.12*math.sin(frame*.95+n*1.7)
                if tip<v<.88:
                    t=(v-tip)/(.88-tip);bend=.035*math.sin(v*12+frame*.9+n)
                    width=.011+.055*t;heat=math.exp(-((u-center-bend)/width)**2)*min(1,t*4)
                    c=mix(c,mix(rgb('A83D18'),rgb('F1AE47'),t),heat*.92)
        if v>.57:
            closest=100;rock=0;local=(0,0)
            for row in range(3):
                for col in range(7):
                    cx=(col+.5+(row%2)*.42)/7;cy=.67+row*.145+.018*math.sin(col*4+row)
                    dx=(u-cx)/(.092+.012*math.sin(col*3+row));dy=(v-cy)/.092;dist=math.hypot(dx,dy)
                    if dist<closest:closest=dist;rock=row*7+col;local=(dx,dy)
            surface=.28+.12*(1-local[1])+.04*math.sin(u*85+v*41)
            coal=mix(rgb('172022'),rgb('5B5143'),surface)
            if lit:
                seam=max(0,min(1,(closest-.62)*3));pulse=.7+.25*math.sin(frame*.95+rock*1.3)
                c=mix(coal,mix(rgb('B9451B'),rgb('E6A44D'),max(0,closest-.72)*2),seam*pulse)
            else:c=coal
        return (*c,1)
    if role=='status':return (*rgb('C9964F' if lit else '3E514E'),1)
    if role=='badge':
        c=rgb('B29C6D');ix=int(u*25)-2;iy=int(v*9)-2
        if 0<=ix<20 and 0<=iy<5:
            char='CG-01'[ix//4]
            if ix%4<3 and FONT[char][iy][ix%4]=='1':c=rgb('303E40')
        return (*c,1)
    if role=='dial':
        dx=u-.5;dy=v-.5;r=math.hypot(dx,dy);angle=math.atan2(dy,dx)
        c=rgb('233137') if r>.46 else rgb('ADAEA0') if r>.40 else rgb('D0CCB5')
        if .31<r<.39 and int((angle+math.pi)*20)%5==0:c=rgb('354449')
        # Decorative temperature instrument, not a numerical gameplay readout.
        end=(.22,-.20) if lit else (-.24,.15);t=max(0,min(1,(dx*end[0]+dy*end[1])/(end[0]**2+end[1]**2)))
        if math.hypot(dx-t*end[0],dy-t*end[1])<.018 or r<.045:c=rgb('934C32')
        return (*c,1)
    accent=rgb(PALETTE['input' if role=='socket_input' else 'output']);edge=min(u,v,1-u,1-v)
    c=mix(rgb('101A20'),rgb('35474E'),.6*(1-v))
    if .06<edge<.11:c=mix(accent,rgb('C4D3D2'),.20*(1-v))
    if .22<u<.78 and .22<v<.78:
        if int(u*22)%3==0 or int(v*22)%3==0:c=rgb('536268')
    # Four copper contacts and a directional mode chevron remain visible uncabled.
    for x in (.18,.82):
        for y in (.18,.82):
            if abs(u-x)<.035 and abs(v-y)<.035:c=rgb('BEA775')
    arrow_v=v if role=='socket_input' else 1-v
    if abs(arrow_v-(.84-abs(u-.5)*.6))<.018 and .33<u<.67:c=accent
    return (*c,1)
def specials():
    for role in sorted(SPECIAL):
        states=(False,True) if role in ('coal','flame','flame_back','dial','status') else (False,)
        for lit in states:
            frames=(8 if role.startswith('flame') else 4) if lit and role in ('coal','flame','flame_back') else 1;size=64;pixels=[]
            for y in range(size*frames):
                top=size*frames-1-y;frame=top//size
                for x in range(size):pixels.extend(special_pixel(role,(x+.5)/size,(top%size+.5)/size,frame,lit))
            name=role+('_on' if lit else '');save_image(name,size,size*frames,pixels)
            if frames>1:
                meta={'animation':{'frametime':3 if role.startswith('flame') else 6,'interpolate':True,'frames':list(range(frames))}}
                dump(TEX/f'{name}.png.mcmeta',meta);dump(OUT/'textures'/f'{name}.png.mcmeta',meta)
def model(elements,lit=False):
    textures={r:'domesurvival:block/coal_generator_v2/satin_atlas' for r in PALETTE}
    textures.update({r:'domesurvival:block/coal_generator_v2/'+r+('_on' if lit and r in ('coal','flame','flame_back','dial','status') else '') for r in SPECIAL})
    textures['particle']='domesurvival:block/coal_generator_v2/particle'
    return {'parent':'minecraft:block/block','render_type':'minecraft:cutout','ambientocclusion':True,'textures':textures,'elements':elements+(flames() if lit else [])}
def assembly(m,offset=(0,0,0),q=Quaternion()):
    mats={};objects=[]
    for role,ref in m['textures'].items():
        if role=='particle':continue
        mat=g.material('coal_'+role,(.4,.5,.5));tex=mat.node_tree.nodes.new('ShaderNodeTexImage')
        tex.image=bpy.data.images.load(str(A/('textures/'+ref.split(':')[1]+'.png')),check_existing=True);tex.image.pack();tex.interpolation='Closest'
        bsdf=mat.node_tree.nodes.get('Principled BSDF');mat.node_tree.links.new(tex.outputs['Color'],bsdf.inputs['Base Color'])
        mat.node_tree.links.new(tex.outputs['Alpha'],bsdf.inputs['Alpha'])
        if role.startswith('flame'):
            mat.node_tree.links.new(tex.outputs['Color'],bsdf.inputs['Emission Color']);bsdf.inputs['Emission Strength'].default_value=.6
        mats[role]=mat
    normals={(1,0,0):'east',(-1,0,0):'west',(0,1,0):'up',(0,-1,0):'down',(0,0,1):'south',(0,0,-1):'north'}
    for e in m['elements']:
        lo,hi=e['from'],e['to'];role=next(iter(e['faces'].values()))['texture'][1:]
        spec={'name':e['name'],'minimum':[lo[0]-8,8-hi[2],lo[1]-8],'maximum':[hi[0]-8,8-lo[2],hi[1]-8],'faces':[g.MC_FACES[d] for d in e['faces']]}
        obj=g.mesh_box(spec,e['name'],mats[role],q,offset);uv=obj.data.uv_layers.new(name='Minecraft_UV');objects.append(obj)
        for poly in obj.data.polygons:
            n=q.inverted()@poly.normal;direction=normals[(round(n.x),round(n.z),round(-n.y))];f=e['faces'][direction];coords=[]
            for loop in poly.loop_indices:
                v=q.inverted()@(obj.data.vertices[obj.data.loops[loop].vertex_index].co-Vector(offset));coords.append(g.project((v.x+8,v.z+8,8-v.y),direction))
            x0,x1=min(c[0] for c in coords),max(c[0] for c in coords);y0,y1=min(c[1] for c in coords),max(c[1] for c in coords)
            for loop,(x,y) in zip(poly.loop_indices,coords):
                s,t=(x-x0)/(x1-x0),(y-y0)/(y1-y0);a,b,c,d=f['uv'];tu,tv=(a+(c-a)*s)/16,1-(b+(d-b)*t)/16
                if role in ('coal','flame','flame_back') and m['textures'][role].endswith('_on'):
                    frames=8 if role.startswith('flame') else 4;tv=1-1/frames+tv/frames
                uv.data[loop].uv=(tu,tv)
        if 'rotation' in e:
            rot=e['rotation'];x,y,z=rot['origin'];origin=Vector((x-8,8-z,y-8));rq=Quaternion((0,0,1),math.radians(rot['angle']))
            for vertex in obj.data.vertices:
                local=q.inverted()@(vertex.co-Vector(offset));vertex.co=q@(origin+rq@(local-origin))+Vector(offset)
            obj.data.update()
    return objects
def blockbench(m,size):
    refs=list(dict.fromkeys(m['textures'].values()));refs.remove(m['textures']['particle']);textures=[];elements=[]
    uid=lambda s:str(uuid.uuid5(uuid.NAMESPACE_URL,'coal_generator_v2/'+s))
    for i,ref in enumerate(refs):
        file=TEX/(ref.split('/')[-1]+'.png');im=bpy.data.images.load(str(file),check_existing=True);w,h=im.size
        textures.append({'id':str(i),'uuid':uid(ref),'name':file.name,'width':w,'height':h,'uv_width':w,'uv_height':h,'source':'data:image/png;base64,'+base64.b64encode(file.read_bytes()).decode(),'mode':'bitmap'})
    for i,e in enumerate(m['elements']):
        faces={}
        for d,f in e['faces'].items():
            idx=refs.index(m['textures'][f['texture'][1:]]);w,h=textures[idx]['width'],textures[idx]['height']
            faces[d]={'texture':idx,'uv':[n*(w if j%2==0 else h)/16 for j,n in enumerate(f['uv'])]}
        for d in FACES:faces.setdefault(d,{'texture':None,'uv':[0,0,0,0]})
        elements.append({**e,'name':str(i)+'_'+e['name'],'uuid':uid(str(i)),'type':'cube','faces':faces,'box_uv':False,'origin':[8,8,8],'rotation':[0,0,0]})
    dump(ROOT/'source_assets/blockbench/coal_generator/coal_generator.bbmodel',{'meta':{'format_version':'4.10','model_format':'java_block','box_uv':False},'name':'coal_generator_v2','resolution':{'width':size,'height':size},'elements':elements,'outliner':[e['uuid'] for e in elements],'textures':textures})
base=body();ports={mode:port(mode) for mode in ('input','output')};size=atlas(base+ports['input']+ports['output']);specials()
save_image('particle',16,16,[c for y in range(16) for x in range(16) for c in material_pixel(x,y,16,16,'panel')])
dump(MODEL/'coal_generator.json',model(base));dump(MODEL/'coal_generator_lit.json',model(base,True))
state=read(DEV/'baseline/blockstates/coal_generator.json')
for part in state['multipart']:
    ref=part['apply']['model']
    if ref.startswith('domesurvival:block/machine_'):part['apply']['model']=ref.replace('block/machine_','block/coal_generator_')
for mode,es in ports.items():
    for direction in FACES:dump(MODEL/f'coal_generator_{mode}_port_{direction}.json',model([turn(e,direction) for e in es]))
dump(A/'blockstates/coal_generator.json',state)
inventory=copy.deepcopy(base)
for d in ('east','west','south','down','up'):inventory += [turn(e,d) for e in ports['input' if d=='up' else 'output']]
dump(MODEL/'coal_generator_inventory.json',model(inventory));dump(A/'models/item/coal_generator.json',{'parent':'domesurvival:block/coal_generator_inventory'})
dump(DEV/'manifest.json',{'body_elements':len(base),'body_quads':sum(len(e['faces']) for e in base),'flame_quads':sum(len(e['faces']) for e in flames()),'firebox_depth':4.5,'coal_chunks':10,'port_elements':len(ports['input']),'port_quads':sum(len(e['faces']) for e in ports['input']),'atlas':size,'pixels_per_model_unit':16,'bounds':[0,16],'states':4*2*3**6,'production_java_changes':1})
(OUT/'.gitignore').write_text('*.blend1\n*.blend2\n')
g.reset_scene();g.camera_setup(1500,1150,39);assembly(model(inventory),q=g.ROTATIONS['south'])
g.label('DOMESURVIVAL / COAL GENERATOR',-17.5,13.4,.8)
g.label('SATIN STEEL / RECESSED FIREBOX / CONFIGURABLE SOCKETS',-17.5,-13.5,.48,secondary=True)
g.save_render('01_coal_generator',True);blockbench(model(inventory),size)
g.reset_scene();g.camera_setup(1800,1050,65)
for x,lit in [(-17,False),(17,True)]:assembly(model(inventory,lit),g.screen_point(x,0),g.ROTATIONS['south'])
g.label('COAL GENERATOR / IDLE AND WORKING',-30,15.3,1)
g.label('IDLE',-20,-14,.85);g.label('WORKING / ANIMATED EMBERS',7,-14,.85)
g.save_render('02_idle_and_working',True)
g.reset_scene();g.camera_setup(1800,1100,65)
old=read(DEV/'baseline/models/block/coal_generator.json');e=cube('original_cube',[0,0,0],[16,16,16],'side')
for d,f in e['faces'].items():f.update(texture='#'+('front' if d=='north' else 'top' if d=='up' else 'side'),uv=[0,0,16,16])
old['elements']=[e];assembly(old,g.screen_point(-17,0),g.ROTATIONS['south']);assembly(model(inventory),g.screen_point(17,0),g.ROTATIONS['south'])
g.label('COAL GENERATOR / EXISTING BASE TO REFINED MODEL',-30,16.3,.9)
g.label('ORIGINAL',-22,-13,.85);g.label('REFINED / PIPE V2 MATERIALS',6,-13,.85)
g.save_render('03_before_and_after',True)
print('Coal generator V2 assets and editable sources exported.',flush=True)
