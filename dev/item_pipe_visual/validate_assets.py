"""Blender validation of geometry envelopes, render masks, UVs and strict production scope."""
from pathlib import Path
import bpy, json, hashlib
ROOT=Path(__file__).resolve().parents[2];DEV=ROOT/'dev/item_pipe_visual';BASE=DEV/'baseline'
A=ROOT/'src/main/resources/assets/domesurvival';checks=[]
IDS=[x+'_item_pipe' for x in ('copper','steel','desh','filtering')]
def read(p):return json.loads(p.read_text(encoding='utf-8-sig'))
def check(ok,name):
    checks.append({'pass':bool(ok),'check':name})
    if not ok:print('FAIL',name)
for id in IDS:
    for part in ('core','arm','inventory'):
        old=read(BASE/f'models/block/{id}_{part}.json');m=read(A/f'models/block/{id}_{part}.json')
        check(m.get('loader')=='forge:composite',f'{id}/{part}: separate render passes')
        oldelements={e['name']:e for e in old['elements']};union={}
        for layer,child in m['children'].items():
            check(child['render_type']==('minecraft:cutout' if layer=='frame' else 'minecraft:translucent'),f'{id}/{part}/{layer}: correct render type')
            for e in child['elements']:
                original=oldelements[e['name']]
                if id=='filtering_item_pipe' and 'panel' in e['name']:
                    reference=read(BASE/'models/block/steel_item_pipe_core.json')
                    original=next(x for x in reference['elements'] if x['name']==e['name'])
                check(e['from']==original['from'] and e['to']==original['to'],f'{id}/{part}/{layer}/{e["name"]}: approved frame envelope')
                union.setdefault(e['name'],set()).update(e['faces'])
                for d,f in e['faces'].items():
                    uv=f['uv'];check(len(uv)==4 and 0<=uv[0]<uv[2]<=16 and 0<=uv[1]<uv[3]<=16,f'{id}/{part}/{layer}/{e["name"]}/{d}: UV in atlas')
                    ref=child['textures'][f['texture'][1:]];check((A/('textures/'+ref.split(':')[1]+'.png')).exists(),f'{id}/{part}/{layer}: texture exists')
        check(all(union.get(n)==set(e['faces']) for n,e in oldelements.items()),f'{id}/{part}: every original face retained across layers')
        check(m.get('display')==old.get('display'),f'{id}/{part}: original display transforms')
    old=read(BASE/f'blockstates/{id}.json');state=read(A/f'blockstates/{id}.json')
    for entry in old['multipart']:
        if 'uvlock' in entry['apply']:entry['apply']['uvlock']=False
    check(old==state,f'{id}: all six connection rules preserved; UV follows rotated face')
    for suffix in ('','_frame','_glass'):
        p=A/f'textures/block/item_pipe_refined/{id}{suffix}.png';image=bpy.data.images.load(str(p));pixels=list(image.pixels);alpha=pixels[3::4]
        check(image.size[0]==image.size[1] and image.size[0] in (256,512,1024),f'{id}{suffix}: power-of-two atlas')
        if suffix=='_frame':check(set(round(a,4) for a in alpha)<={0.,1.} and 1. in alpha,f'{id}: metal mask opaque or empty')
        if suffix=='_glass':check(any(.12<a<.29 for a in alpha) and max(alpha)<.30,f'{id}: visible glass alpha 13–28%, no opaque texels')
    for ext in ('.blend',):check((ROOT/f'source_assets/blender/item_pipes/{id}{ext}').exists(),f'{id}: editable Blender master')
    for mode in ('frame','input','output','disabled'):
        image=bpy.data.images.load(str(A/f'textures/block/item_pipe_refined/{id}_connector_{mode}.png'))
        old=bpy.data.images.load(str(BASE/f'textures/block/item_pipe/connector_{mode}.png'))
        pixels=list(image.pixels[:]);op=list(old.pixels[:]);w,hh=image.size;ow,oh=old.size
        check((w,hh)==(128,128),f'{id}/{mode}: high resolution connector material')
        check(all(abs(pixels[(y*w+x)*4+3]-op[((y*oh//hh)*ow+x*ow//w)*4+3])<.001 for y in range(hh) for x in range(w)),f'{id}/{mode}: original connector silhouette retained')
    bb=read(ROOT/f'source_assets/blockbench/item_pipes/{id}.bbmodel')
    check(len(bb['elements'])==17 and bb['textures'][0]['source'].startswith('data:image/png;base64,'),f'{id}: Blockbench inventory with embedded texture')
baseline=read(DEV/'baseline_hashes.json');changed=[];new=[]
for p in (ROOT/'src/main').rglob('*'):
    if not p.is_file():continue
    rel=str(p.relative_to(ROOT));digest=hashlib.sha256(p.read_bytes()).hexdigest()
    if rel not in baseline:new.append(rel)
    elif baseline[rel]!=digest:changed.append(rel)
allowed_java={'ItemPipeNetworkManager.java','ItemPipeBlockEntityRenderer.java','ItemPipeVisualNetwork.java','ItemPipeTravelVisuals.java'}
for rel in changed+new:
    p=Path(rel)
    allowed=(p.name in allowed_java and 'itempipe' in p.parts) or ('assets' in p.parts and (any(p.name.startswith(id) for id in IDS)))
    check(allowed,'Production scope: '+rel)
for rel in baseline:check((ROOT/rel).exists(),'No deleted production file: '+rel) if not (ROOT/rel).exists() else None
result={'pass':all(c['pass'] for c in checks),'checks':len(checks),'failures':[c for c in checks if not c['pass']],'changed':changed,'new':new}
(DEV/'asset_validation.json').write_text(json.dumps(result,indent=2),encoding='utf-8')
print(json.dumps({k:v for k,v in result.items() if k not in ('changed','new')},indent=2))
if not result['pass']:raise RuntimeError('Item pipe asset validation failed')
