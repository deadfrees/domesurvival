"""Bake a pixel-crystal neosteel skin over the shared voltarium ingot base."""
from pathlib import Path
import bpy,json,shutil,math,random
ROOT=Path(__file__).resolve().parents[3]
ASSETS=ROOT/'src/main/resources/assets/domesurvival'
SOURCE=ROOT/'source_assets/blender/material_family'
DEV=ROOT/'dev/materials_visual'
shape=ASSETS/'textures/item/voltarium_ingot.png'
target=ASSETS/'textures/item/materials_v2/neosteel_ingot.png'
bpy.ops.wm.read_factory_settings(use_empty=True)
bpy.context.preferences.filepaths.save_version=0
image=bpy.data.images.load(str(shape),check_existing=False)
image.pack();sw,sh=image.size;src=list(image.pixels[:])
mask=[]
for y in range(32):
    row=[]
    for x in range(32):
        i=4*((sh-1-(31-y)*sh//32)*sw+x*sw//32)
        row.append(src[i+3]>.01)
    mask.append(row)
def clamp(v):return max(0,min(1,v))
def rgb(hexv):return tuple(int(hexv[i:i+2],16)/255 for i in (0,2,4))
deep=rgb('24113D');purple=rgb('6338A8');violet=rgb('9C63F2');blue=rgb('373CFF');pink=rgb('D47BFF')
rng=random.Random(4317);pixels=[]
for y in range(32):
    for x in range(32):
        if not mask[y][x]:
            pixels.extend((0,0,0,0));continue
        edge=any(not mask[yy][xx] for xx,yy in ((x-1,y),(x+1,y),(x,y-1),(x,y+1)) if 0<=xx<32 and 0<=yy<32)
        base=list(deep if edge else purple)
        band=.18*math.sin((x+y)*.37)+.10*math.sin((x*2-y)*.19)
        base=[clamp(c+band) for c in base]
        d1=abs(y-(.58*x+3.5));d2=abs(y-(-.72*x+27.0));d3=abs(y-(.36*x+22.0))
        if d1<1.0:base=list(pink)
        elif d1<2.6:base=list(violet)
        elif d1<3.8:base=[clamp(c*1.35) for c in blue]
        elif d2<.9:base=list(blue)
        elif d2<2.0:base=list(violet)
        elif d3<.8:base=list(violet)
        if not edge and (x//3+y//4)%5==0:base=[clamp(c*1.16) for c in base]
        if not edge and rng.random()<.06:base=[clamp(c*.78) for c in base]
        if edge and y<16:base=[clamp(c*1.18) for c in base]
        pixels.extend((*base,1))
target.parent.mkdir(parents=True,exist_ok=True)
backup=DEV/'baseline/src/main/resources/assets/domesurvival/textures/item/materials_v2/neosteel_ingot.png'
if not backup.exists():
    backup.parent.mkdir(parents=True,exist_ok=True);shutil.copyfile(target,backup)
out=bpy.data.images.new('detailed_neosteel_ingot',width=32,height=32,alpha=True)
out.colorspace_settings.name='sRGB';out.pixels.foreach_set(pixels);out.filepath_raw=str(target);out.file_format='PNG';out.save();out.pack()
target.with_suffix('.png.mcmeta').write_text('{"texture":{"blur":false,"clamp":false}}\n',encoding='utf-8')
data=json.loads((DEV/'manifest.json').read_text())
for entry in data['items']:
    if entry['id']=='neosteel_ingot':
        entry['source']='src/main/resources/assets/domesurvival/textures/item/voltarium_ingot.png';entry['shape_source']='voltarium_ingot';entry['detail']='pixel crystal veins over shared ingot base'
(DEV/'manifest.json').write_text(json.dumps(data,indent=2)+'\n',encoding='utf-8')
bpy.ops.wm.save_as_mainfile(filepath=str(SOURCE/'material_family.blend'))
print('DETAILED_NEOSTEEL_INGOT_COMPLETE',flush=True)

