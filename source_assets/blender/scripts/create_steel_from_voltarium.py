"""Give steel the voltarium ingot silhouette while retaining steel colouring."""
from pathlib import Path
import bpy,json,shutil
ROOT=Path(__file__).resolve().parents[3]
ASSETS=ROOT/'src/main/resources/assets/domesurvival';SOURCE=ROOT/'source_assets/blender/material_family';DEV=ROOT/'dev/materials_visual'
src=ASSETS/'textures/item/voltarium_ingot.png';steel=ASSETS/'textures/item/metallurgy/steel_ingot.png';target=ASSETS/'textures/item/materials_v2/steel_ingot.png'
bpy.ops.wm.read_factory_settings(use_empty=True);bpy.context.preferences.filepaths.save_version=0
image=bpy.data.images.load(str(src),check_existing=False);image.pack();w,h=image.size
steel_image=bpy.data.images.load(str(steel),check_existing=False);steel_image.pack();steel_px=list(steel_image.pixels[:]);steel_luma=[]
for i in range(0,len(steel_px),4):
    c=steel_px[i:i+3]
    if steel_px[i+3]>.01:steel_luma.append((.2126*c[0]+.7152*c[1]+.0722*c[2],c))
steel_luma.sort(key=lambda x:x[0]);palette=[steel_luma[int(i*(len(steel_luma)-1)/15)][1] for i in range(16)]
source=list(image.pixels[:]);pixels=[]
for y in range(32):
    for x in range(32):
        i=4*((h-1-(31-y)*h//32)*w+x*w//32);c=source[i:i+4]
        if c[3] <= .01:pixels.extend((0,0,0,0));continue
        l=max(0,min(1,.2126*c[0]+.7152*c[1]+.0722*c[2]));pixels.extend((*palette[int(round(l*15))],1))
target.parent.mkdir(parents=True,exist_ok=True)
backup=DEV/'baseline/src/main/resources/assets/domesurvival/textures/item/materials_v2/steel_ingot.png'
if not backup.exists():backup.parent.mkdir(parents=True,exist_ok=True);shutil.copyfile(target,backup)
out=bpy.data.images.new('steel_from_voltarium',width=32,height=32,alpha=True);out.colorspace_settings.name='sRGB';out.pixels.foreach_set(pixels);out.filepath_raw=str(target);out.file_format='PNG';out.save();out.pack()
target.with_suffix('.png.mcmeta').write_text('{"texture":{"blur":false,"clamp":false}}\n',encoding='utf-8')
data=json.loads((DEV/'manifest.json').read_text())
for entry in data['items']:
    if entry['id']=='steel_ingot':entry['source']='src/main/resources/assets/domesurvival/textures/item/voltarium_ingot.png';entry['shape_source']='voltarium_ingot'
(DEV/'manifest.json').write_text(json.dumps(data,indent=2)+'\n',encoding='utf-8')
bpy.ops.wm.save_as_mainfile(filepath=str(SOURCE/'material_family.blend'));print('STEEL_VOLTARIUM_SHAPE_COMPLETE',flush=True)
