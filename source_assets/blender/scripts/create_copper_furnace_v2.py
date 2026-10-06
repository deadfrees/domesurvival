"""Copper-clad fuel furnace. Blender source and exact Minecraft cuboids share geometry."""
from pathlib import Path
script=Path(__file__).with_name('create_coal_generator_v2.py')
helpers=script.read_text().split('base=body();ports=')[0]
# Reuse the approved material, UV and fire authoring functions in an isolated scope.
a=helpers.index("if not (DEV/'baseline_hashes.json').exists():")
b=helpers.index('def rgb(s):')
helpers=helpers[:a]+helpers[b:]
helpers=helpers.replace('coal_generator','copper_furnace')
exec(compile(helpers,str(script),'exec'))
PALETTE.update(body='A45E43',panel='C07855',trim='D5936D',frame='754631',brass='B98B55')

# Broad Minecraft-sized copper patches with restrained edge shading. The furnace
# is an early copper-clad vanilla furnace, not a small industrial generator.
metal_pixel=material_pixel
def material_pixel(x,y,w,h,role):
    if role not in ('panel','body','trim','frame'):return metal_pixel(x,y,w,h,role)
    u=(x+.5)/w;v=(y+.5)/h
    gx=int(x/16);gy=int(y/16)
    patch=math.sin(gx*4.17+gy*7.13)*.025+math.sin(gx*1.73-gy*2.31)*.018
    edge=min(x+.5,y+.5,w-x-.5,h-y-.5)
    light=.94+.07*(1-v)+patch
    if edge<2:light*=.78
    elif edge<4:light+=.06*(1-v)
    # Quiet folded sheet seam, following the vanilla copper block's large panels.
    if role=='panel' and w>150 and h>150 and abs(y-h*.5)<1.5:light*=.86
    return (*tuple(max(0,min(1,c*light+grain(x,y)*.25)) for c in rgb(PALETTE[role])),1)

def body():
    es=[]
    def add(name,lo,hi,role,faces=FACES):es.append(cube(name,lo,hi,role,faces))
    # A simple enclosed copper cube, with the two familiar furnace openings.
    add('Copper rear shell',[.2,.2,4.6],[15.8,15.8,15.8],'panel')
    add('Left copper wall',[.2,.2,.2],[3,15.8,4.6],'panel')
    add('Right copper wall',[13,.2,.2],[15.8,15.8,4.6],'panel')
    add('Copper hearth apron',[3,.2,.2],[13,2,4.6],'body')
    add('Copper chamber divider',[3,7.2,.2],[13,9.5,4.6],'panel')
    add('Copper upper lintel',[3,12,.2],[13,15.8,4.6],'panel')
    # Narrow folded copper edges keep the block silhouette flush and closed.
    for x in (0,15.8):
        for z in (0,15.8):add('Folded copper corner',[x,0,z],[x+.2,16,z+.2],'body')
    for y in (0,15.8):
        for z in (0,15.8):add('Folded copper rim',[.2,y,z],[15.8,y+.2,z+.2],'body')
        for x in (0,15.8):add('Folded copper side rim',[x,y,.2],[x+.2,y+.2,15.8],'body')
    # Deep lower hearth and shallow upper charging opening; no industrial door.
    for bottom,top,depth in ((2,7.2,4.4),(9.5,12,1.8)):
        for name,lo,hi in [('rear',[3,bottom,depth],[13,top,depth+.1]),
                           ('left',[3,bottom,.2],[3.2,top,depth]),
                           ('right',[12.8,bottom,.2],[13,top,depth]),
                           ('floor',[3.2,bottom,.2],[12.8,bottom+.2,depth]),
                           ('roof',[3.2,top-.2,.2],[12.8,top,depth])]:
            add('Stone hearth '+name,lo,hi,'refractory')
    for z in (.7,2.5):
        for col in range(4):
            x=3.5+col*2.3
            add('Coal bed',[x,2.22,z],[x+1.65,2.9+(col%2)*.18,z+1.4],'coal')
    for i,e in enumerate(es):e['name']=f'{i:03}_'+e['name']
    return es

def flames():
    es=[]
    for i,(x,z,h) in enumerate(((4.6,1.35,3.4),(8,1.3,3.9),(11.4,1.4,3.5),(6,3.2,3.7),(10,3.1,3.6))):
        for angle in (-45,45):
            e=cube(f'Flame {i} {angle}',[x-1.1,2.95,z-.01],[x+1.1,2.95+h,z+.01],'flame_back' if i>2 else 'flame',['north','south'])
            e['rotation']={'origin':[x,2.95,z],'axis':'y','angle':angle,'rescale':False}
            for f in e['faces'].values():f['uv']=[0,0,16,16]
            es.append(e)
    return es

original_special_pixel=special_pixel
def special_pixel(role,u,v,frame=0,lit=False):
    if lit and role in ('flame','flame_back'):
        u=(int(u*24)+.5)/24;v=(int(v*24)+.5)/24
        phase=frame*math.tau/8+(1.7 if role=='flame_back' else 0)
        tip=.07+.035*math.sin(phase)
        t=max(0,min(1,(v-tip)/(1-tip)))
        bend=.12*math.sin(v*6-phase)*(1-t)
        width=.025+.35*t
        inside=1-abs(u-.5-bend)/width
        if v<tip or inside<0:return (0,0,0,0)
        heat=inside*.6+t*.45
        color='FF6B18' if heat<.42 else 'FFAB2C' if heat<.72 else 'FFE080'
        return (*rgb(color),1)
    if lit and role=='coal':
        edge=min(u,v,1-u,1-v)
        fissure=max(0,1-edge/.19,max(0,1-abs(u*.75+v-.72)/.065)*.8)
        pulse=.70+.16*math.sin(frame*math.pi/2+u*3+v*2)
        return (*mix(rgb('37302A'),rgb('FF9D35'),fissure*pulse),1)
    return original_special_pixel(role,u,v,frame,lit)

original_model=model
def model(elements,lit=False):
    result=original_model(copy.deepcopy(elements),lit)
    if lit:
        for e in result['elements']:
            roles={f['texture'] for f in e['faces'].values()}
            if roles.intersection({'#coal','#flame','#flame_back'}):
                e['shade']=False
                e['forge_data']={'block_light':15 if '#coal' not in roles else 12,'ambient_occlusion':False}
    return result

base=body();ports={mode:port(mode) for mode in ('input','output')}
size=atlas(base+ports['input']+ports['output']);specials()
save_image('particle',16,16,[c for y in range(16) for x in range(16) for c in material_pixel(x,y,16,16,'panel')])
dump(MODEL/'copper_furnace.json',model(base));dump(MODEL/'copper_furnace_on.json',model(base,True))
state={'multipart':[]}
for lit in (False,True):
    for direction,rot in [('north',0),('east',90),('south',180),('west',270)]:
        state['multipart'].append({'when':{'facing':direction,'lit':str(lit).lower()},'apply':{'model':'domesurvival:block/copper_furnace'+('_on' if lit else ''),'y':rot}})
for mode,es in ports.items():
    for direction in FACES:
        dump(MODEL/f'copper_furnace_{mode}_port_{direction}.json',model([turn(e,direction) for e in es]))
        state['multipart'].append({'when':{'port_'+direction:mode},'apply':{'model':f'domesurvival:block/copper_furnace_{mode}_port_{direction}'}})
dump(A/'blockstates/copper_furnace.json',state)
inventory=copy.deepcopy(base)+[turn(e,'up') for e in ports['input']]
dump(MODEL/'copper_furnace_inventory.json',model(inventory));dump(A/'models/item/copper_furnace.json',{'parent':'domesurvival:block/copper_furnace_inventory'})
g.reset_scene();g.camera_setup(1200,950,29);assembly(model(inventory),q=g.ROTATIONS['south'])
g.save_render('copper_furnace',True)
print('COPPER_FURNACE_MODEL_COMPLETE',len(base),size,flush=True)
