"""Fit the original neosteel crystal texture onto the regular ingot mask."""
from pathlib import Path
import bpy,json,shutil

ROOT=Path(__file__).resolve().parents[3]
ASSETS=ROOT/'src/main/resources/assets/domesurvival'
SOURCE=ROOT/'source_assets/blender/material_family'
DEV=ROOT/'dev/materials_visual'
SHAPE=ASSETS/'textures/item/materials_v2/steel_ingot.png'
REF=SOURCE/'references/neosteel_reference.png'
TARGET=ASSETS/'textures/item/materials_v2/neosteel_ingot.png'

bpy.ops.wm.read_factory_settings(use_empty=True)
bpy.context.preferences.filepaths.save_version=0
shape_img=bpy.data.images.load(str(SHAPE),check_existing=False);shape_img.pack()
sw,sh=shape_img.size;shape=list(shape_img.pixels[:])
ref_img=bpy.data.images.load(str(REF),check_existing=False);ref_img.pack()
rw,rh=ref_img.size;ref=list(ref_img.pixels[:])

def clamp(v):return max(0,min(1,v))
def pixel(buf,w,h,x,y):
    x=max(0,min(w-1,int(x)));y=max(0,min(h-1,int(y)))
    i=4*((h-1-y)*w+x);return buf[i:i+4]
def chromatic(c):
    return c[3]>.05 and max(c[:3])-min(c[:3])>.055
def crystal_colour(c):
    if not chromatic(c) or max(c[:3])<=.025:return False
    r,g,b,a=c
    # The reference watermark has brown/gold pixels; the real crystal keeps
    # its blue component at least close to the red/green channels.
    return not (b<g*.78 and r>g*.95)
def crystal_pixel(x,y):
    # Exclude the old watermark while retaining the crystal's purple/blue
    # texture. The checkerboard becomes a deep purple fill inside the ingot.
    if x>160 and y>240:y=max(55,y-45)
    c=pixel(ref,rw,rh,x,y)
    if crystal_colour(c):return c
    for delta in (12,-12,24,-24,36,-36,48,-48,60,-60):
        yy=max(55,min(rh-1,y+delta));alt=pixel(ref,rw,rh,x,yy)
        if crystal_colour(alt):return alt
    return (.11,.035,.22,1)

mask=[]
for y in range(sh):
    for x in range(sw):
        if pixel(shape,sw,sh,x,y)[3]>.05:mask.append((x,y))
minx=min(x for x,y in mask);maxx=max(x for x,y in mask)
miny=min(y for x,y in mask);maxy=max(y for x,y in mask)
# The supplied crystal image is cropped to its clean visible artwork.
rx0,ry0,rx1,ry1=52,54,388,307
pixels=[]
for y in range(32):
    for x in range(32):
        sc=pixel(shape,sw,sh,x,y)
        if sc[3]<=.05:
            pixels.extend((0,0,0,0));continue
        u=(x-minx+.5)/(maxx-minx+1);v=(y-miny+.5)/(maxy-miny+1)
        c=crystal_pixel(rx0+u*(rx1-rx0),ry0+v*(ry1-ry0))
        # Keep the original crystal palette, only lifting the very darkest
        # capture pixels enough to remain readable as an inventory ingot.
        rgb=tuple(clamp(v*1.18+.012) for v in c[:3])
        pixels.extend(tuple(round(v*24)/24 for v in rgb)+(1,))

TARGET.parent.mkdir(parents=True,exist_ok=True)
backup=DEV/'baseline/src/main/resources/assets/domesurvival/textures/item/materials_v2/neosteel_ingot.png'
if not backup.exists():
    backup.parent.mkdir(parents=True,exist_ok=True);shutil.copyfile(TARGET,backup)
ordered=[v for row in range(31,-1,-1) for v in pixels[row*32*4:(row+1)*32*4]]
out=bpy.data.images.new('neosteel_ingot_crystal_texture',width=32,height=32,alpha=True)
out.colorspace_settings.name='sRGB';out.pixels.foreach_set(ordered)
out.filepath_raw=str(TARGET);out.file_format='PNG';out.save();out.pack()
TARGET.with_suffix('.png.mcmeta').write_text('{"texture":{"blur":false,"clamp":false}}\n',encoding='utf-8')
data=json.loads((DEV/'manifest.json').read_text())
for entry in data['items']:
    if entry['id']=='neosteel_ingot':
        entry['source']='source_assets/blender/material_family/references/neosteel_reference.png'
        entry['shape_source']='steel_ingot_base'
        entry['detail']='original crystal texture fitted to the regular ingot silhouette'
(DEV/'manifest.json').write_text(json.dumps(data,indent=2)+'\n',encoding='utf-8')
bpy.ops.wm.save_as_mainfile(filepath=str(SOURCE/'material_family.blend'))
print('NEOSTEEL_CRYSTAL_TEXTURE_INGOT_COMPLETE',flush=True)
