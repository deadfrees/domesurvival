"""Bake the supplied neosteel reference into a clean 32px item sprite.
The checkerboard and watermark are excluded; the visible crystal chunk is retained.
"""
from pathlib import Path
import bpy,json,shutil,math
ROOT=Path(__file__).resolve().parents[3]
ASSETS=ROOT/'src/main/resources/assets/domesurvival'
SOURCE=ROOT/'source_assets/blender/material_family'
DEV=ROOT/'dev/materials_visual'
reference=SOURCE/'references/neosteel_reference.png'
target=ASSETS/'textures/item/materials_v2/neosteel_ingot.png'
bpy.ops.wm.read_factory_settings(use_empty=True)
bpy.context.preferences.filepaths.save_version=0
image=bpy.data.images.load(str(reference),check_existing=False)
image.pack();w,h=image.size;src=list(image.pixels[:])
outline=[(261,54),(303,54),(303,76),(325,76),(325,97),(346,97),(346,118),(366,118),(366,139),(388,139),(388,223),(365,223),(365,244),(345,244),(345,266),(324,266),(324,286),(262,286),(262,307),(198,307),(198,288),(157,288),(157,266),(114,266),(114,245),(73,245),(73,226),(52,226),(52,140),(73,140),(73,118),(114,118),(114,98),(135,98),(135,77),(198,77),(198,54)]
def inside(px,py):
    result=False;j=len(outline)-1
    for i in range(len(outline)):
        xi,yi=outline[i];xj,yj=outline[j]
        if ((yi>py)!=(yj>py)) and px<(xj-xi)*(py-yi)/(yj-yi)+xi:result=not result
        j=i
    return result
def sample(x,y):
    # Remove the reference watermark area by sampling the clean crystal above
    # it. The original capture includes both pale lettering and a brown label;
    # the wider replacement zone keeps either from becoming part of the item.
    if x>180 and y>260:y=max(55,y-40)
    x=max(0,min(w-1,int(x)));y=max(0,min(h-1,int(y)))
    c=src[4*((h-1-y)*w+x):4*((h-1-y)*w+x)+4]
    # Any remaining achromatic watermark pixel is replaced by a nearby purple
    # crystal pixel. Keep only genuinely dark achromatic edge pixels intact.
    r,g,b,a=c
    if a>.05 and max(r,g,b)-min(r,g,b)<.055 and min(r,g,b)>.16:
        for delta in (12,24,36):
            yy=max(55,y-delta)
            alt=src[4*((h-1-yy)*w+x):4*((h-1-yy)*w+x)+4]
            if alt[3]>.05 and max(alt[:3])-min(alt[:3])>.055:
                return alt
    return c
pixels=[]
for y in range(32):
    for x in range(32):
        sx=52+((x-.5)/31.0)*336;sy=54+((y-2.5)/27.0)*253
        if not inside(sx,sy):
            pixels.extend((0,0,0,0));continue
        c=sample(sx,sy);r,g,b,a=c
        # The checkerboard is achromatic and bright; the crystal is chromatic.
        if a<.05 or (min(r,g,b)>.62 and max(r,g,b)-min(r,g,b)<.08):
            pixels.extend((0,0,0,0));continue
        # Lift the dark reference capture so the dropped item remains readable
        # under normal world lighting; retain the purple hue instead of black.
        peak=max(r,g,b)
        if peak<.10:
            r,g,b=.055,.018,.125
        else:
            r,g,b=(min(1,r*1.32+.018),min(1,g*1.32+.012),min(1,b*1.32+.030))
        # Snap the supplied large pixels to a crisp Minecraft palette.
        pixels.extend(tuple(round(max(0,min(1,v))*24)/24 for v in (r,g,b))+(1,))
 # Blender stores image rows bottom-first; reverse the baked rows so the in-game
 # item keeps the same top/bottom orientation as the supplied reference.
pixels=[value for row in range(31,-1,-1) for value in pixels[row*32*4:(row+1)*32*4]]
target.parent.mkdir(parents=True,exist_ok=True)
backup=DEV/'baseline/src/main/resources/assets/domesurvival/textures/item/materials_v2/neosteel_ingot.png'
if not backup.exists():
    backup.parent.mkdir(parents=True,exist_ok=True);shutil.copyfile(target,backup)
out=bpy.data.images.new('neosteel_reference_sprite',width=32,height=32,alpha=True)
out.colorspace_settings.name='sRGB';out.pixels.foreach_set(pixels);out.filepath_raw=str(target);out.file_format='PNG';out.save();out.pack()
target.with_suffix('.png.mcmeta').write_text('{"texture":{"blur":false,"clamp":false}}\n',encoding='utf-8')
data=json.loads((DEV/'manifest.json').read_text())
for entry in data['items']:
    if entry['id']=='neosteel_ingot':
        entry['source']='source_assets/blender/material_family/references/neosteel_reference.png'
        entry['shape_source']='reference_crystal_chunk'
        entry['detail']='reference silhouette with checkerboard and watermark removed'
(DEV/'manifest.json').write_text(json.dumps(data,indent=2)+'\n',encoding='utf-8')
bpy.ops.wm.save_as_mainfile(filepath=str(SOURCE/'material_family.blend'))
print('NEOSTEEL_REFERENCE_COMPLETE',flush=True)
