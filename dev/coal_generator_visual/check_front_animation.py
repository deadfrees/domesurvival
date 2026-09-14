"""Read-only pixel verification of two stationary-camera client captures."""
from pathlib import Path
import bpy,json
DEV=Path(__file__).resolve().parent
a=bpy.data.images.load(str(DEV/'runtime/02_working_close.png'))
b=bpy.data.images.load(str(DEV/'runtime/02b_animated_front.png'))
assert tuple(a.size)==tuple(b.size)
pa=list(a.pixels[:]);pb=list(b.pixels[:]);w,h=a.size;changed=[]
for i in range(0,len(pa),4):
    ca=pa[i:i+3];cb=pb[i:i+3]
    hot=lambda c:c[0]>.20 and c[0]>c[1]*1.22 and c[0]>c[2]*1.5
    if (hot(ca) or hot(cb)) and max(abs(x-y) for x,y in zip(ca,cb))>.025:
        changed.append(((i//4)%w,h-1-(i//4)//w))
result={'pass':len(changed)>100,'changed_hot_pixels':len(changed),'screen_bounds':[min(x for x,y in changed),min(y for x,y in changed),max(x for x,y in changed),max(y for x,y in changed)] if changed else None,
        'captures':['02_working_close.png','02b_animated_front.png'],'elapsed_game_ticks':9}
(DEV/'animation_validation.json').write_text(json.dumps(result,indent=2)+'\n')
print(json.dumps(result,indent=2));assert result['pass']
