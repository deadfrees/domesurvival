"""Bake the supplied clean gear reference into all press gear variants.

The silhouette and black outline stay identical for every metal. Only the
interior palette is remapped from the reference luminance.
"""
from pathlib import Path
import bpy,shutil

ROOT=Path(__file__).resolve().parents[3]
ASSETS=ROOT/'src/main/resources/assets/domesurvival'
REF=ROOT/'source_assets/blender/press_products/references/gear_gold_reference.png'
OUT=ASSETS/'textures/item/press_products'
DEV=ROOT/'dev/forming_press_v2'
IDS=['goteium_gear','lead_gear','nickel_gear','steel_gear','tin_gear','voltarium_gear']
PALETTE={'goteium':'49B9B5','lead':'77788F','nickel':'C0B597','steel':'77848C','tin':'B7C7CC','voltarium':'9582CE'}
CROP=(100,35,635,553)

bpy.ops.wm.read_factory_settings(use_empty=True)
bpy.context.preferences.filepaths.save_version=0
image=bpy.data.images.load(str(REF),check_existing=False);image.pack()
w,h=image.size;src=list(image.pixels[:])

def clamp(v):return max(0,min(1,v))
def color(hexv):return tuple(int(hexv[i:i+2],16)/255 for i in (0,2,4))
def px(x,y):
    x=max(0,min(w-1,int(x)));y=max(0,min(h-1,int(y)))
    i=4*((h-1-y)*w+x);return src[i:i+4]
def background(c):
    r,g,b,a=c;spread=max(r,g,b)-min(r,g,b)
    return a<.05 or (spread<.07 and min(r,g,b)>.43)
def save32(path,pixels):
    path.parent.mkdir(parents=True,exist_ok=True)
    backup=DEV/'baseline_press_products'/path.relative_to(ROOT)
    if path.exists() and not backup.exists():
        backup.parent.mkdir(parents=True,exist_ok=True);shutil.copyfile(path,backup)
    ordered=[v for row in range(31,-1,-1) for v in pixels[row*32*4:(row+1)*32*4]]
    out=bpy.data.images.new(path.stem+'_gear_ref',width=32,height=32,alpha=True)
    out.colorspace_settings.name='sRGB';out.pixels.foreach_set(ordered)
    out.filepath_raw=str(path);out.file_format='PNG';out.save();out.pack()
    path.with_suffix('.png.mcmeta').write_text('{"texture":{"blur":false,"clamp":false}}\n',encoding='utf-8')

x0,y0,x1,y1=CROP
for item in IDS:
    metal=item.split('_')[0];base=color(PALETTE[metal]);pixels=[]
    for y in range(32):
        for x in range(32):
            sx=x0+(x+.5)/32*(x1-x0);sy=y0+(y+.5)/32*(y1-y0)
            c=px(sx,sy);r,g,b,a=c
            if background(c):
                pixels.extend((0,0,0,0));continue
            lum=.2126*r+.7152*g+.0722*b
            # Recolour every visible reference pixel, including the formerly
            # black edge, so each gear is one coherent metal colour.
            factor=.40+1.18*lum
            # A small directional lift matches the reference's upper-left light.
            factor+=.07 if (x+y)<30 else -.025 if (x+y)>35 else 0
            pixels.extend(tuple(round(clamp(v*factor)*24)/24 for v in base)+(1,))
    save32(OUT/(item+'.png'),pixels);print('GEAR_REFERENCE',item,flush=True)
print('GEAR_REFERENCE_COMPLETE',len(IDS),flush=True)
