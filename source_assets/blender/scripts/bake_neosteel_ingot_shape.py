"""Bake neosteel as a regular ingot using the shared steel silhouette."""
from pathlib import Path
import bpy,json,shutil

ROOT=Path(__file__).resolve().parents[3]
ASSETS=ROOT/'src/main/resources/assets/domesurvival'
SOURCE=ROOT/'source_assets/blender/material_family'
DEV=ROOT/'dev/materials_visual'
shape=ASSETS/'textures/item/materials_v2/steel_ingot.png'
target=ASSETS/'textures/item/materials_v2/neosteel_ingot.png'

bpy.ops.wm.read_factory_settings(use_empty=True)
bpy.context.preferences.filepaths.save_version=0
image=bpy.data.images.load(str(shape),check_existing=False);image.pack()
w,h=image.size;src=list(image.pixels[:])

def clamp(v):return max(0,min(1,v))
def px(x,y):
    x=max(0,min(w-1,int(x)));y=max(0,min(h-1,int(y)))
    i=4*((h-1-y)*w+x);return src[i:i+4]

# Purple/blue neosteel palette sampled from the supplied crystal reference.
deep=(.075,.030,.16);shadow=(.20,.095,.34);base=(.39,.22,.66)
violet=(.62,.39,.95);highlight=(.84,.48,1.0);blue=(.16,.16,.95)
pixels=[]
for y in range(32):
    for x in range(32):
        c=px(x,y);r,g,b,a=c
        if a<.05:
            pixels.extend((0,0,0,0));continue
        lum=.2126*r+.7152*g+.0722*b
        if lum<.22: col=deep
        elif lum<.38: col=shadow
        elif lum<.56: col=base
        elif lum<.76: col=violet
        else: col=highlight
        d=y-(.62*x+3.0)
        if abs(d)<1.0: col=blue
        elif abs(d)<2.0: col=tuple(clamp(v*1.12) for v in violet)
        pixels.extend(tuple(round(clamp(v)*24)/24 for v in col)+(1,))

target.parent.mkdir(parents=True,exist_ok=True)
backup=DEV/'baseline/src/main/resources/assets/domesurvival/textures/item/materials_v2/neosteel_ingot.png'
if not backup.exists():
    backup.parent.mkdir(parents=True,exist_ok=True);shutil.copyfile(target,backup)
ordered=[v for row in range(31,-1,-1) for v in pixels[row*32*4:(row+1)*32*4]]
out=bpy.data.images.new('neosteel_ingot_regular_shape',width=32,height=32,alpha=True)
out.colorspace_settings.name='sRGB';out.pixels.foreach_set(ordered)
out.filepath_raw=str(target);out.file_format='PNG';out.save();out.pack()
target.with_suffix('.png.mcmeta').write_text('{"texture":{"blur":false,"clamp":false}}\n',encoding='utf-8')
data=json.loads((DEV/'manifest.json').read_text())
for entry in data['items']:
    if entry['id']=='neosteel_ingot':
        entry['source']='src/main/resources/assets/domesurvival/textures/item/materials_v2/steel_ingot.png'
        entry['shape_source']='steel_ingot_base'
        entry['detail']='regular ingot silhouette with neosteel purple-blue palette'
(DEV/'manifest.json').write_text(json.dumps(data,indent=2)+'\n',encoding='utf-8')
bpy.ops.wm.save_as_mainfile(filepath=str(SOURCE/'material_family.blend'))
print('NEOSTEEL_INGOT_SHAPE_COMPLETE',flush=True)
