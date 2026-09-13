"""Author four existing item-pipe frames, split metal/glass atlases and editable masters via bpy.
Run with Blender --background --factory-startup --python-exit-code 1 --python this_file.py.
Baseline resources in dev/item_pipe_visual/baseline are immutable inputs.
"""
from pathlib import Path
import copy, importlib.util, math, sys
import bpy
sys.path.insert(0, str(Path(__file__).parent))
import item_pipe_material_helpers as h
from mathutils import Vector
ROOT=h.ROOT; A=h.A; OUT=h.OUT; DEV=h.DEV; BASE=h.BASE; g=h.g
IDS=['copper_item_pipe','steel_item_pipe','desh_item_pipe','filtering_item_pipe']
ACCENTS=['C99768','AFC5C4','8EA2D2','83BD97']
PAD=4; DENSITY=16

def role_for(e,d):
    w,hh=g.face_size(e,d)
    if e['name']=='core_body':return 'shell'
    if 'panel' in e['name']:
        ref=e['faces'][d]['texture']
        if ref.endswith('_route'):return 'route_'+ref[1:-6]
        return 'window' if ref=='#icon' else 'trim'
    if 'body' in e['name']:return 'end' if abs(w-hh)<.01 else 'glass'
    return 'rail_window' if min(w,hh)>1 else 'trim'

def key(e,d):
    w,hh=g.face_size(e,d)
    return e['faces'][d]['texture'][1:],round(w*DENSITY),round(hh*DENSITY)

def pixel(x,y,w,hh,role,index):
    tier=min(index+1,3);accent=h.rgb(ACCENTS[index]);u=(x+.5)/w;v=(y+.5)/hh
    edge=min(x+.5,y+.5,w-x-.5,hh-y-.5)
    c=h.face_pixel(x,y,w,hh,'rail','energy',tier)
    alpha=1.0
    transverse=x if w<hh else y; length=y if w<hh else x
    span=min(w,hh);long=max(w,hh)
    if role=='shell' and edge>2:alpha=0.0
    if role=='end' and edge>2:alpha=0.0
    if role=='glass':
        if edge>2:
            alpha=.13+.05*(1-(u if w<hh else v))
            c=h.mix(h.rgb('A4C4C7'),accent,.18)
    if role=='rail_window':
        # Retain the original rail envelope; mill a longitudinal sight opening.
        if 3<transverse<span-3 and 9<length<long-9:alpha=0.0
        if min(length,long-1-length)<8:
            c=h.mix(c,accent,.60)
        if 1.2<min(transverse,span-1-transverse)<2.3:c=h.mix(c,h.rgb('CDDCDB'),.5)
    if role=='window' or role.startswith('route_'):
        if edge>5:
            alpha=.16
            c=h.mix(h.rgb('ABC6CB'),accent,.16)
            if abs(u-(.22+.28*v))<.012:alpha=.28
        else:
            c=h.mix(c,accent,.42)
            # Tier bars / compact filter grille stay on the lower metal rim.
            n=3 if index==3 else tier
            for i in range(n):
                if abs(u-(.5+(i-(n-1)/2)*.12))<.025 and .92<v<.97:c=h.rgb('D6E5DC')
            if index==3 and .14<u<.25 and .91<v<.98:c=h.rgb('254E36')
            for cx,cy in ((.055,.055),(.945,.055),(.055,.945),(.945,.945)):
                dx=(u-cx)*w;dy=(v-cy)*hh
                if dx*dx+dy*dy<1.7**2:c=h.rgb('B8CBC7') if dy<0 else h.rgb('344447')
        if role.startswith('route_'):
            route=role[6:]
            colors={'north':'3D7DCE','south':'C64A42','west':'4C9A57','east':'D4B83E','up':'E1E4E6','down':'8A59B5'}
            if .15<u<.85 and y<4:c=h.mix(h.rgb(colors[route]),h.rgb('D6E5DC'),.12*(1-v));alpha=1
            glyphs={'north':['101','111','111','111','101'],'south':['111','100','111','001','111'],
                    'west':['101','101','111','111','101'],'east':['111','100','110','100','111'],
                    'up':['101','101','101','101','111'],'down':['110','101','101','101','110']}
            gx=x-w//2+1;gy=y-(hh-6)
            if 0<=gx<3 and 0<=gy<5:
                c=h.rgb('E5EEE7') if glyphs[route][gy][gx]=='1' else h.rgb('294139');alpha=1
    return (*c,alpha)

def atlas(id,models,index):
    for m in models.values():
        for e in m['elements']:
            roles={d:role_for(e,d) for d in e['faces']}
            for d,role in roles.items():e['faces'][d]['texture']='#'+role
    keys=sorted({key(e,d) for m in models.values() for e in m['elements'] for d in e['faces']},key=lambda k:(-k[2],-k[1],k[0]))
    size=256
    while True:
        x=y=row=0;layout={}
        for role,w,hh in keys:
            if x+w+2*PAD>size:x=0;y+=row;row=0
            layout[role,w,hh]=(x+PAD,y+PAD,w,hh);x+=w+2*PAD;row=max(row,hh+2*PAD)
        if y+row<=size:break
        size*=2
    layers={name:[0.0]*(size*size*4) for name in ('full','frame','glass')};present={}
    for k,(ox,oy,w,hh) in layout.items():
        present[k]=set()
        for y in range(-PAD,hh+PAD):
            for x in range(-PAD,w+PAD):
                rgba=pixel(max(0,min(w-1,x)),max(0,min(hh-1,y)),w,hh,k[0],index)
                i=((size-1-oy-y)*size+ox+x)*4
                layers['full'][i:i+4]=rgba
                name='frame' if rgba[3]==1 else 'glass'
                if rgba[3]>0:
                    layers[name][i:i+4]=rgba;present[k].add(name)
    for name,pixels in layers.items():
        image=bpy.data.images.new(id+'_'+name,width=size,height=size,alpha=True)
        image.colorspace_settings.name='sRGB';image.pixels.foreach_set(pixels)
        file=OUT/'textures'/f'{id}{"" if name=="full" else "_"+name}.png'
        image.filepath_raw=str(file);image.file_format='PNG';image.save()
        import shutil
        dest=A/'textures/block/item_pipe_refined'/file.name;dest.parent.mkdir(parents=True,exist_ok=True);shutil.copyfile(file,dest)
    for part,m in models.items():
        face_keys={}
        for ei,e in enumerate(m['elements']):
            for d,f in e['faces'].items():
                k=key(e,d);face_keys[ei,d]=k;ox,oy,w,hh=layout[k]
                f['uv']=[n*16/size for n in (ox,oy,ox+w,oy+hh)];f.pop('rotation',None)
        roles=sorted({e['faces'][d]['texture'][1:] for e in m['elements'] for d in e['faces']})
        m['textures']={r:f'domesurvival:block/item_pipe_refined/{id}' for r in roles}
        m['textures']['particle']=f'domesurvival:block/item_pipe/{id}_detail'
        wrapper={k:copy.deepcopy(v) for k,v in m.items() if k!='elements'}
        wrapper.update(loader='forge:composite',children={},item_render_order=['frame','glass'])
        for name,render in [('frame','minecraft:cutout'),('glass','minecraft:translucent')]:
            child={'render_type':render,'ambientocclusion':False if name=='glass' else True,
                   'textures':{r:f'domesurvival:block/item_pipe_refined/{id}_{name}' for r in roles},'elements':[]}
            for ei,e in enumerate(m['elements']):
                clone=copy.deepcopy(e)
                clone['faces']={d:f for d,f in clone['faces'].items() if name in present[face_keys[ei,d]]}
                if clone['faces']:child['elements'].append(clone)
            wrapper['children'][name]=child
        h.dump(A/f'models/block/{id}_{part}.json',wrapper)
    return size,layout

def connectors(id,index):
    """Keep the original connector silhouette and click geometry, refine its surface only."""
    import shutil
    size=128;accent=h.rgb(ACCENTS[index]);tier=min(index+1,3)
    for mode in ('frame','input','output','disabled'):
        old=bpy.data.images.load(str(BASE/f'textures/block/item_pipe/connector_{mode}.png'),check_existing=True)
        ow,oh=old.size;original=list(old.pixels[:]);pixels=[]
        for py in range(size):
            for x in range(size):
                y=size-1-py;u=(x+.5)/size;v=(y+.5)/size
                alpha=original[((py*oh//size)*ow+x*ow//size)*4+3]
                c=h.face_pixel(x,y,size,size,'rail','energy',tier)
                edge=min(x+.5,y+.5,size-x-.5,size-y-.5)
                if mode=='frame':
                    # Light satin steel, a thin tier stripe and countersunk corner screws.
                    if 7<edge<11:c=h.mix(c,accent,.72)
                    if 1<edge<3:c=h.mix(c,h.rgb('D7E0DD'),.38)
                    for cx,cy in ((.10,.10),(.90,.10),(.10,.90),(.90,.90)):
                        dx=(u-cx)*size;dy=(v-cy)*size;r=math.hypot(dx,dy)
                        if r<4.1:
                            c=h.rgb('AFC1BF') if dy<-.6 else h.rgb('52676D')
                            if abs(dy)<.7 and abs(dx)<2.6:c=h.rgb('33464D')
                else:
                    color=h.rgb({'input':'579ED0','output':'D39A55','disabled':'798A8D'}[mode])
                    inset=min(u-.19,v-.19,.81-u,.81-v)*size
                    if inset>=0:
                        c=h.mix(h.rgb('35494D'),color,h.smooth(0,5,inset))
                        c=tuple(a*(1.08-.20*v) for a in c)
                        # Direction glyphs keep input/output readable without bright flat squares.
                        gx=(u-.30)/.40;gy=(v-.29)/.42
                        if 0<=gx<=1 and 0<=gy<=1:
                            if mode=='disabled':mark=abs(gy-.5)<.07
                            else:
                                if mode=='output':gy=1-gy
                                mark=(.41<gx<.59 and gy<.6) or (.35<gy<.84 and abs(gx-.5)<(.84-gy)*.78)
                            if mark:c=h.rgb('DBE5DD')
                    elif 6<edge<10:c=h.mix(c,accent,.5)
                pixels.extend((*c,alpha))
        image=bpy.data.images.new(id+'_connector_'+mode,width=size,height=size,alpha=True)
        image.colorspace_settings.name='sRGB';image.pixels.foreach_set(pixels)
        file=OUT/'textures'/f'{id}_connector_{mode}.png';image.filepath_raw=str(file);image.file_format='PNG';image.save()
        shutil.copyfile(file,A/'textures/block/item_pipe_refined'/file.name)

all_models={};manifest={}
for index,id in enumerate(IDS):
    models={part:h.read(BASE/f'models/block/{id}_{part}.json') for part in ('core','arm','inventory')}
    if id=='filtering_item_pipe':
        # The old world filter used thin floating route labels, unlike its inventory mesh.
        # User requested the same closed window frame as the three transport tiers.
        frame={e['name']:e for e in h.read(BASE/'models/block/steel_item_pipe_core.json')['elements']}
        for part in ('core','inventory'):
            for e in models[part]['elements']:
                if 'panel' in e['name']:
                    e['from']=copy.deepcopy(frame[e['name']]['from']);e['to']=copy.deepcopy(frame[e['name']]['to'])
    size,layout=atlas(id,models,index);all_models[index]=models
    connectors(id,index)
    # A rotated island must follow its physical face, including vertical connections.
    state=h.read(BASE/f'blockstates/{id}.json')
    for entry in state['multipart']:
        if 'uvlock' in entry['apply']:entry['apply']['uvlock']=False
    h.dump(A/f'blockstates/{id}.json',state)
    g.SPECS[index]=h.specs(models)
    g.reset_scene();g.camera_setup(1400,1000,28)
    objects=g.assembly(index,{'north','south'},prefix=id);h.texture(objects,models,id)
    g.label(id.replace('_',' ').upper(),-12,10,.85)
    bpy.context.scene['description']='Original item pipe frame; opaque satin metal and separate translucent sight windows.'
    bpy.ops.wm.save_as_mainfile(filepath=str(OUT/f'{id}.blend'))
    h.blockbench(id,models['inventory'],size)
    manifest[id]={'atlas_size':size,'layers':['cutout metal','translucent glass'],'glass_alpha':[.13,.28],
                  'islands':[{'role':k[0],'rect':list(v)} for k,v in layout.items()]}

g.reset_scene();g.camera_setup(1800,1300,61)
g.label('DOMESURVIVAL / ITEM TRANSPORT',-27,20,1.2)
for index,id in enumerate(IDS):
    x=-15 if index%2==0 else 15;y=8 if index<2 else -10
    offset=g.screen_point(x,y);objects=g.assembly(index,{'north','south'},offset,prefix=id)
    h.texture(objects,all_models[index],id,offset)
    g.label(['COPPER / TIER 1','STEEL / TIER 2','DESH / TIER 3','FILTER / ROUTING'][index],x-7,y-6.5,.8)
g.label('SATIN METAL / TRANSLUCENT SIGHT WINDOWS',-27,-23,.7,secondary=True)
g.save_render('01_item_pipe_family',True)
h.dump(DEV/'manifest.json',manifest)
print('ITEM PIPE FAMILY: four models and split material layers authored successfully')
