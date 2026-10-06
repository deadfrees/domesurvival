"""Bake a more readable steel ingot while preserving the voltarium silhouette."""
from pathlib import Path
import bpy,json,shutil
ROOT=Path(__file__).resolve().parents[3]
ASSETS=ROOT/'src/main/resources/assets/domesurvival';SOURCE=ROOT/'source_assets/blender/material_family';DEV=ROOT/'dev/materials_visual'
shape=ASSETS/'textures/item/voltarium_ingot.png';steel=ASSETS/'textures/item/metallurgy/steel_ingot.png';target=ASSETS/'textures/item/materials_v2/steel_ingot.png'
bpy.ops.wm.read_factory_settings(use_empty=True);bpy.context.preferences.filepaths.save_version=0
shape_img=bpy.data.images.load(str(shape),check_existing=False);shape_img.pack();sw,sh=shape_img.size;sp=list(shape_img.pixels[:])
steel_img=bpy.data.images.load(str(steel),check_existing=False);steel_img.pack();raw=list(steel_img.pixels[:]);palette=[]
for i in range(0,len(raw),4):
    if raw[i+3]>.01:
        c=raw[i:i+3];palette.append((.2126*c[0]+.7152*c[1]+.0722*c[2],c))
palette.sort(key=lambda item:item[0]);colors=[palette[int(i*(len(palette)-1)/15)][1] for i in range(16)]
mask=[]
for y in range(32):
    row=[]
    for x in range(32):
        i=4*((sh-1-(31-y)*sh//32)*sw+x*sw//32);row.append(sp[i+3]>.01)
    mask.append(row)
pixels=[]
for y in range(32):
    for x in range(32):
        i=4*((sh-1-(31-y)*sh//32)*sw+x*sw//32);opaque=mask[y][x]
        if not opaque:
            pixels.extend((0,0,0,0));continue
        l=max(0,min(1,.2126*sp[i]+.7152*sp[i+1]+.0722*sp[i+2]));c=list(colors[int(round(l*15))])
        edge=any(not mask[yy][xx] for xx,yy in ((x-1,y),(x+1,y),(x,y-1),(x,y+1)) if 0<=xx<32 and 0<=yy<32)
        upper=not mask[y-1][x] if y>0 else True;lower=not mask[y+1][x] if y<31 else True
        if edge and lower:factor=.62
        elif edge and upper:factor=1.20
        elif (x+y)%9==0:factor=1.08
        else:factor=.94+.045*((x+2*y)%4)
        pixels.extend((*[min(1,v*factor) for v in c],1))
target.parent.mkdir(parents=True,exist_ok=True);backup=DEV/'baseline/src/main/resources/assets/domesurvival/textures/item/materials_v2/steel_ingot.png'
if not backup.exists():
    backup.parent.mkdir(parents=True,exist_ok=True);shutil.copyfile(target,backup)
out=bpy.data.images.new('detailed_steel_ingot',width=32,height=32,alpha=True);out.colorspace_settings.name='sRGB';out.pixels.foreach_set(pixels);out.filepath_raw=str(target);out.file_format='PNG';out.save();out.pack()
target.with_suffix('.png.mcmeta').write_text('{"texture":{"blur":false,"clamp":false}}\n',encoding='utf-8')
data=json.loads((DEV/'manifest.json').read_text())
for entry in data['items']:
    if entry['id']=='steel_ingot':
        entry['source']='src/main/resources/assets/domesurvival/textures/item/voltarium_ingot.png';entry['shape_source']='voltarium_ingot';entry['detail']='pixel bevel and cast highlight'
(DEV/'manifest.json').write_text(json.dumps(data,indent=2)+'\n',encoding='utf-8')
bpy.ops.wm.save_as_mainfile(filepath=str(SOURCE/'material_family.blend'));print('DETAILED_STEEL_INGOT_COMPLETE',flush=True)

