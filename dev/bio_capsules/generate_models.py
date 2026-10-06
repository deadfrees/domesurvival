"""Compose thin Minecraft item models from existing metal and species textures.

No raster editing: portraits use their original images or exact atlas UV cells.
"""
from pathlib import Path
import json, shutil, hashlib

ROOT = Path(__file__).resolve().parents[2]
A = ROOT / 'src/main/resources/assets/domesurvival'
OUT = Path(__file__).parent
SPECIES = 'cow pig sheep chicken rabbit horse donkey llama goat camel wolf cat ocelot fox bee panda turtle axolotl frog mooshroom sniffer strider hoglin mule parrot polar_bear'.split()
ATLAS = 'rabbit horse donkey llama goat camel wolf cat ocelot fox bee panda turtle axolotl frog mooshroom sniffer strider hoglin'.split()
COLORS = 'brown pink white white brown brown gray white white yellow gray light_gray yellow orange yellow white green pink lime red green red brown brown red white'.split()
LEGACY = {'cow_cryocapsule':'cow', 'sheep_cryocapsule':'sheep', 'chicken_cryocapsule':'chicken', 'damaged_pig_cryocapsule':'pig_damaged'}
owned = [A / f'models/item/{n}.json' for n in ['bio_module', *LEGACY]]
if not (OUT / 'baseline.json').exists():
    baseline = {}
    for p in owned:
        dest = OUT / 'baseline' / p.relative_to(ROOT)
        dest.parent.mkdir(parents=True, exist_ok=True)
        shutil.copy2(p, dest)
        baseline[p.relative_to(ROOT).as_posix()] = hashlib.sha256(p.read_bytes()).hexdigest()
    (OUT / 'baseline.json').write_text(json.dumps(baseline, indent=2))

uvs = {}
for name in ('coal_generator', 'coal_generator_input_port_north'):
    for e in json.loads((A / f'models/block/{name}.json').read_text())['elements']:
        for f in e['faces'].values():
            role = f['texture'][1:]
            uv = f['uv']; area = (uv[2]-uv[0])*(uv[3]-uv[1])
            if role not in uvs or area > uvs[role][0]: uvs[role] = (area, uv)

DISPLAY = {
    'gui': {'rotation':[0,180,0], 'translation':[0,0,0], 'scale':[1,1,1]},
    'ground': {'rotation':[0,0,0], 'translation':[0,2,0], 'scale':[.5,.5,.5]},
    'fixed': {'rotation':[0,180,0], 'translation':[0,0,0], 'scale':[1,1,1]},
    'thirdperson_righthand': {'rotation':[0,0,0], 'translation':[0,3,1], 'scale':[.55,.55,.55]},
    'thirdperson_lefthand': {'rotation':[0,0,0], 'translation':[0,3,1], 'scale':[.55,.55,.55]},
    'firstperson_righthand': {'rotation':[0,-90,25], 'translation':[1.13,3.2,1.13], 'scale':[.68,.68,.68]},
    'firstperson_lefthand': {'rotation':[0,90,-25], 'translation':[1.13,3.2,1.13], 'scale':[.68,.68,.68]},
}

def model(species, color, damaged=False):
    textures = {r:'domesurvival:block/coal_generator_v2/satin_atlas' for r in ('body','frame','trim','steel','black','brass','input')}
    textures.update(accent=f'minecraft:block/{color}_concrete', dark='minecraft:block/black_concrete',
                    white='minecraft:block/white_concrete', red='minecraft:block/red_concrete',
                    blue='minecraft:block/blue_concrete', status='minecraft:block/'+('orange' if damaged else 'cyan')+'_concrete',
                    particle='domesurvival:block/coal_generator_v2/particle')
    if species in ATLAS:
        i = ATLAS.index(species)
        portrait = [i%5*3.2, i//5*4, (i%5+1)*3.2, (i//5+1)*4]
        textures['portrait'] = 'domesurvival:gui/bio/bio_faces_atlas'
    elif species:
        portrait = [0,0,16,16]
        textures['portrait'] = 'domesurvival:gui/bio/'+species
    es = []
    def box(name, lo, hi, role, uv=None, faces=None):
        es.append(dict(name=name, **{'from':lo, 'to':hi}, shade=False,
                       faces={f:dict(texture='#'+role, uv=uv or (uvs[role][1] if role in uvs else [1,1,15,15]))
                              for f in (faces or ['north','south','east','west','up','down'])}))
    box('Graphite cartridge', [3,1,7],[13,13,9], 'frame')
    box('Dark portrait well', [3.5,3.5,6.85],[12.5,12.5,7], 'black')
    box('Lower steel collar', [4,0,6.65],[12,2,9.2], 'steel')
    box('Upper steel collar', [4,12,6.65],[12,14,9.2], 'steel')
    box('Species band', [4,12,6.55],[12,12.65,6.65], 'accent')
    box('Left rail', [3,2,6.65],[4,12,7], 'trim')
    if damaged:
        box('Broken rail lower', [12,2,6.65],[13,7,7], 'trim')
        box('Broken rail upper', [12,9,6.65],[13,12,7], 'trim')
    else: box('Right rail', [12,2,6.65],[13,12,7], 'trim')
    for x in (5,9): box('Contact', [x,1,6.5],[x+2,1.5,6.65], 'brass')
    box('Condition indicator', [6,2.5,6.65],[10,3.25,6.8], 'status')
    if species:
        # Front and reverse both identify the sample; no portrait mirrored by an override.
        box('Species portrait', [4,4,6.6],[12,12,6.6], 'portrait', portrait, ['north'])
        box('Reverse species portrait', [4,4,9.01],[12,12,9.01], 'portrait', portrait, ['south'])
    else:
        for y in (5,7,9): box('Unidentified sample', [6,y,6.6],[10,y+1,6.7], 'input')
    def tab(x,y,w,h,role='accent'):
        box('Species contour', [x,y,6.65],[x+w,y+h,8.5],role)
    if species in ('cow','mooshroom','goat'):
        for x in (4,11): tab(x,14,1,1.5,'white' if species!='goat' else 'dark')
    elif species in ('rabbit','donkey','mule'):
        for x in (4,10): tab(x,14,2,2); tab(x,15.5,2,.5,'dark')
    elif species in ('wolf','cat','ocelot','panda','polar_bear','llama','pig'):
        for x in (3,11): tab(x,13,2,2,'dark' if species=='panda' else 'accent')
    elif species == 'horse': tab(7,14,2,2,'dark')
    elif species == 'chicken': tab(7,14,2,1.5,'red')
    elif species == 'bee':
        for x in (4,11): tab(x,14,1,2,'dark')
    elif species == 'axolotl':
        for x in (1.5,13):
            for y in (7,10): tab(x,y,1.5,1)
    elif species == 'frog':
        for x in (3,10): tab(x,13,3,2)
    elif species == 'hoglin':
        for x in (2,13): tab(x,3,1,3,'white')
    elif species == 'strider':
        for x in (1.5,13): tab(x,6,1.5,1,'white');tab(x,9,1.5,1,'white')
    elif species == 'parrot':
        tab(7,14,2,2,'red');tab(2,6,1,4,'blue');tab(13,6,1,4,'blue')
    elif species == 'fox':
        for x in (3,11): tab(x,14,2,2,'dark')
    elif species == 'sheep':
        tab(2,5,1,7,'white');tab(13,5,1,7,'white')
    elif species in ('turtle','sniffer','camel'): tab(4,14,8,1)
    if damaged:
        for x,y in ((10,1),(10.5,.5),(11,0)): box('Collar scratch',[x,y,6.45],[x+.5,y+.5,6.5],'black')
    return dict(parent='minecraft:block/block', gui_light='front', ambientocclusion=False,
                textures=textures, display=DISPLAY, elements=es)

dest = A / 'models/item/bio_capsules'; dest.mkdir(parents=True,exist_ok=True)
for i,(s,c) in enumerate(zip(SPECIES,COLORS)):
    for damaged in (False,True):
        name = s+('_damaged' if damaged else '')
        (dest / (name+'.json')).write_text(json.dumps(model(s,c,damaged),indent=2)+'\n')
(dest/'unknown.json').write_text(json.dumps(model(None,'gray'),indent=2)+'\n')
overrides = []
for damaged in (False,True):
    for i,s in enumerate(SPECIES):
        overrides.append({'predicate':{'domesurvival:bio_variant':(i+1+26*damaged)/64},
                          'model':'domesurvival:item/bio_capsules/'+s+('_damaged' if damaged else '')})
(A/'models/item/bio_module.json').write_text(json.dumps({'parent':'domesurvival:item/bio_capsules/unknown','overrides':overrides},indent=2)+'\n')
for old,new in LEGACY.items():
    (A/f'models/item/{old}.json').write_text(json.dumps({'parent':'domesurvival:item/bio_capsules/'+new},indent=2)+'\n')
(OUT/'manifest.json').write_text(json.dumps({'species':SPECIES,'variants':52,'legacy':LEGACY},indent=2)+'\n')
print('Exported 52 individual capsule models, neutral fallback and four legacy aliases.')
