"""Bake the supplied press-product references into every matching result.
Blender performs the crop, nearest pixel reduction and material recolouring.
"""
from pathlib import Path
import bpy,json,shutil,math
ROOT=Path(__file__).resolve().parents[3]
ASSETS=ROOT/'src/main/resources/assets/domesurvival'
SOURCE=ROOT/'source_assets/blender/press_products'
DEV=ROOT/'dev/forming_press_v2'
REF=SOURCE/'references'
OUT=ASSETS/'textures/item/press_products'
PALETTE={'copper':'B97549','tin':'B7C7CC','steel':'77848C','nickel':'C0B597','silver':'D9E3E8','goteium':'49B9B5','voltarium':'9582CE','lead':'77788F'}
GROUPS={
 'wire':(['copper_wire','silver_wire','steel_wire','voltarium_wire'],REF/'wire_spool.png',(79,102,381,404)),
 'plate':(['copper_plate','goteium_plate','nickel_plate','steel_plate','tin_plate','voltarium_plate'],REF/'plate.png',(84,67,598,581)),
 # The supplied rod reference has a dark editor border around the white canvas.
 # Keep the crop inside that canvas so the border cannot become a false part of
 # the rod's bounding box.
 'rod':(['copper_rod','silver_rod','steel_rod','voltarium_rod'],REF/'rod.png',(150,80,700,610)),
 'tube':(['copper_tube','nickel_tube','steel_tube','tin_tube'],REF/'tube.png',(45,60,467,446)),
 'gear':(['goteium_gear','lead_gear','nickel_gear','steel_gear','tin_gear','voltarium_gear'],REF/'gear.png',(130,54,931,855))
}
bpy.ops.wm.read_factory_settings(use_empty=True)
bpy.context.preferences.filepaths.save_version=0
BACKUP=DEV/'baseline_press_products'
def color(hexv):return tuple(int(hexv[i:i+2],16)/255 for i in (0,2,4))
def clamp(v):return max(0,min(1,v))
def bg(c,kind):
    r,g,b,a=c
    if a<.05:return True
    spread=max(r,g,b)-min(r,g,b)
    # White/checkerboard backgrounds and the white gear watermark.
    return spread<.055 and min(r,g,b)>.76
def recolor(c,metal,kind):
    r,g,b,a=c;l=.2126*r+.7152*g+.0722*b
    base=color(PALETTE[metal])
    if kind=='gear':
        factor=.25+1.20*l
    elif kind=='wire':
        factor=.30+1.12*l
    else:
        factor=.28+1.16*l
    return tuple(clamp(v*factor) for v in base)+(1,)
def rod_recolor(c,metal,x,y):
    """Apply regular stepped metal bands to the diagonal rod reference.

    The source is intentionally pixelated, but its resize leaves isolated
    light/dark patches in the middle of the shaft. The bands below keep the
    supplied silhouette while making the shaft read as one straight piece.
    """
    base=color(PALETTE[metal])
    lum=.2126*c[0]+.7152*c[1]+.0722*c[2]
    lane=(x+y)-31
    if lum<.24:
        factor=.48
    elif lane<=-2:
        # Keep the upper bevel from collapsing into one flat colour. The
        # repeating two-pixel cadence follows the reference's stepped shine
        # while remaining regular after the 32px reduction.
        factor=(1.08,1.18,1.12,1.24)[((x-y)//2)%4]
    elif lane==-1:
        factor=1.08
    elif lane==0:
        factor=1.32 if ((x-y)//2)%3==0 else 1.16
    elif lane==1:
        factor=.90
    else:
        factor=.62
    return tuple(clamp(v*factor) for v in base)+(1,)
def save32(path,pixels):
    path.parent.mkdir(parents=True,exist_ok=True)
    ordered=[v for row in range(31,-1,-1) for v in pixels[row*32*4:(row+1)*32*4]]
    backup=BACKUP/path.relative_to(ROOT)
    if path.exists() and not backup.exists():
        backup.parent.mkdir(parents=True,exist_ok=True);shutil.copyfile(path,backup)
    image=bpy.data.images.new(path.stem+'_ref',width=32,height=32,alpha=True)
    image.colorspace_settings.name='sRGB';image.pixels.foreach_set(ordered);image.filepath_raw=str(path);image.file_format='PNG';image.save();image.pack()
    path.with_suffix('.png.mcmeta').write_text('{"texture":{"blur":false,"clamp":false}}\n',encoding='utf-8')
def px(src,w,h,x,y):
    x=max(0,min(w-1,int(x)));y=max(0,min(h-1,int(y)))
    i=4*((h-1-y)*w+x);return src[i:i+4]
def foreground(c,kind):
    r,g,b,a=c;spread=max(r,g,b)-min(r,g,b)
    if a<.05:return False
    if kind=='tube':return spread>.055 or min(r,g,b)<.38
    return spread>.055 or min(r,g,b)<.45
def object_bbox(src,w,h,box,kind):
    x0,y0,x1,y1=box;xs=[];ys=[]
    for y in range(y0,y1):
        for x in range(x0,x1):
            if foreground(px(src,w,h,x,y),kind):xs.append(x);ys.append(y)
    return min(xs),min(ys),max(xs)+1,max(ys)+1
def wire_keep(sx,sy):
    # Keep only the spool body; discard the loose wire tail from the reference.
    return (136<=sx<324 and 120<=sy<177) or (174<=sx<286 and 175<=sy<330) or (154<=sx<305 and 328<=sy<382)
def wire_shape(x,y):
    """Clean 32px spool silhouette, removing the loose reference tail."""
    if 2<=y<=4:return 6<=x<=25
    if 5<=y<=7:return 8<=x<=23
    if 8<=y<=23:return 10<=x<=21
    if 24<=y<=27:return 8<=x<=23
    if 28<=y<=29:return 8<=x<=23
    return False
def fill_row_gaps(pixels,rows=32):
    """Fill one-pixel holes in a pixel-art row without smoothing its edges."""
    out=list(pixels)
    for y in range(rows):
        active=[x for x in range(32) if out[4*(y*32+x)+3]>.05]
        if len(active)<2:continue
        left,right=min(active),max(active)
        for x in range(left+1,right):
            i=4*(y*32+x)
            if out[i+3]>.05:continue
            # Copy the closest painted pixel in the same row. This closes
            # the single-pixel notch at the reference rod's lower end.
            src=min((q for q in active if q!=x),key=lambda q:abs(q-x))
            j=4*(y*32+src);out[i:i+4]=out[j:j+4]
    return out
def clean_wire_shape(pixels):
    out=list(pixels)
    # Preserve the generated colours but force the reference's continuous
    # spool silhouette. The previous mask let the loose lower strand survive.
    for y in range(32):
        for x in range(32):
            if not wire_shape(x,y):
                i=4*(y*32+x);out[i:i+4]=(0,0,0,0)
    # The reference contains a single pale strand pixel at the lower-left
    # corner of the bottom cap. Match it to the adjacent dark cap instead.
    for y in (26,27):
        for x in (8,9):
            i=4*(y*32+x);j=4*(y*32+10);out[i:i+4]=out[j:j+4]
    return out
def bake(kind,ids,reference,box):
    image=bpy.data.images.load(str(reference),check_existing=False);image.pack();w,h=image.size;src=list(image.pixels[:])
    x0,y0,x1,y1=box;bx0,by0,bx1,by1=object_bbox(src,w,h,box,kind) if kind not in ('plate','wire') else box
    if kind not in ('plate','wire'):
        pad={'rod':3,'tube':2,'gear':1}[kind];bw=bx1-bx0;bh=by1-by0;scale=min((32-2*pad)/bw,(32-2*pad)/bh)
        dw=max(1,round(bw*scale));dh=max(1,round(bh*scale));ox=(32-dw)//2;oy=(32-dh)//2
    for item in ids:
        metal=item.split('_')[0];pixels=[]
        for y in range(32):
            for x in range(32):
                if kind in ('plate','wire'):
                    sx=x0+(x+.5)/32*(x1-x0);sy=y0+(y+.5)/32*(y1-y0)
                    if kind=='wire' and not wire_keep(sx,sy):pixels.extend((0,0,0,0));continue
                else:
                    if not (ox<=x<ox+dw and oy<=y<oy+dh):pixels.extend((0,0,0,0));continue
                    sx=bx0+(x-ox+.5)/dw*(bx1-bx0);sy=by0+(y-oy+.5)/dh*(by1-by0)
                c=px(src,w,h,sx,sy)
                # The spool core is intentionally light grey in the supplied
                # reference. Do not mistake those pixels for the white canvas.
                in_wire_core=kind=='wire' and 168<=sx<=292 and 168<=sy<=336
                if (bg(c,kind) and not in_wire_core) or (kind not in ('plate','wire') and not foreground(c,kind)):
                    pixels.extend((0,0,0,0));continue
                if kind=='rod':
                    out=rod_recolor(c,metal,x,y)
                elif kind!='wire' or (170<=sx<=290 and 170<=sy<=335):
                    out=recolor(c,metal,kind)
                else:
                    out=tuple(round(clamp(v)*24)/24 for v in c[:3])+(1,)
                pixels.extend(tuple(round(clamp(v)*24)/24 for v in out[:3])+(out[3],))
        if kind=='rod':pixels=fill_row_gaps(pixels)
        if kind=='wire':
            pixels=clean_wire_shape(pixels)
            # Close the remaining internal notch in the lower cap after the
            # stray reference pixel has been replaced.
            pixels=fill_row_gaps(pixels)
        save32(OUT/(item+'.png'),pixels);print('PRESS_REFERENCE',item,flush=True)
for kind,(ids,reference,box) in GROUPS.items():
    # This pass is intentionally limited to the two corrected groups. Plates,
    # tubes and gears stay byte-for-byte unchanged after their approved bake.
    if kind in ('rod','wire'):bake(kind,ids,reference,box)
print('PRESS_REFERENCE_COMPLETE',sum(len(v[0]) for v in GROUPS.values()),flush=True)
