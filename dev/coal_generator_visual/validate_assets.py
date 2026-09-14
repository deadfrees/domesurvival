from pathlib import Path
import bpy,json,hashlib,subprocess
ROOT=Path(__file__).resolve().parents[2];DEV=ROOT/'dev/coal_generator_visual';A=ROOT/'src/main/resources/assets/domesurvival'
checks=[]
def read(p):return json.loads(p.read_text(encoding='utf-8-sig'))
def check(ok,name):checks.append({'pass':bool(ok),'check':name})
manifest=read(DEV/'manifest.json');states=read(A/'blockstates/coal_generator.json');old=read(DEV/'baseline/blockstates/coal_generator.json')
check([p['when'] for p in states['multipart']]==[p['when'] for p in old['multipart']],'Existing facing, lit and world-side port conditions retained')
for p in states['multipart']:
    check((A/('models/'+p['apply']['model'].split(':')[1]+'.json')).exists(),'Multipart model reference')
    check(not p['apply']['model'].startswith('domesurvival:block/machine_'),'Dedicated generator ports, shared machine overlays unchanged')
models=[p for p in (A/'models/block').glob('coal_generator*.json') if p.name in ('coal_generator.json','coal_generator_lit.json','coal_generator_inventory.json') or '_input_port_' in p.name or '_output_port_' in p.name]
for p in models:
    m=read(p);check(m['render_type']=='minecraft:cutout',p.name+' render pass')
    for e in m['elements']:
        check(all(0<=lo<hi<=16 for lo,hi in zip(e['from'],e['to'])),p.name+'/'+e['name']+' one-block bounds')
        for d,f in e['faces'].items():
            uv=f['uv'];check(0<=uv[0]<uv[2]<=16 and 0<=uv[1]<uv[3]<=16,p.name+' valid UV')
            ref=m['textures'][f['texture'][1:]];check((A/('textures/'+ref.split(':')[1]+'.png')).exists(),p.name+' texture reference')
    if '_port_' in p.stem:
        direction=p.stem.split('_')[-1];axis={'north':2,'south':2,'east':0,'west':0,'up':1,'down':1}[direction]
        lo=min(e['from'][axis] for e in m['elements']);hi=max(e['to'][axis] for e in m['elements'])
        check((0<=lo<hi<=.15) if direction in ('north','west','down') else (15.85<=lo<hi<=16),p.name+' flush socket on correct world face')
        check(sum(len(e['faces']) for e in m['elements'])==manifest['port_quads'],p.name+' connector quad count')
base=read(A/'models/block/coal_generator.json');lit=read(A/'models/block/coal_generator_lit.json')
check(base['elements']==[e for e in lit['elements'] if not e['name'].startswith('flame_')],'Lit transition retains all solid geometry')
flames=[e for e in lit['elements'] if e['name'].startswith('flame_')]
check(len(flames)==10 and {e['rotation']['angle'] for e in flames}=={-45,45},'Ten crossed flame planes in two depth rows')
coals=[e for e in base['elements'] if '_coal_' in e['name']]
check(len(coals)==20 and max(e['to'][2] for e in coals)-min(e['from'][2] for e in coals)>3,'Twenty solid parts form ten coals across cavity depth')
check(not any(f['texture']=='#fire' for e in base['elements'] for f in e['faces'].values()),'Old flat fire billboard removed')
for name in ('coal','coal_on','flame_on','flame_back_on','dial','dial_on','status','status_on','satin_atlas'):
    image=bpy.data.images.load(str(A/f'textures/block/coal_generator_v2/{name}.png'));w,h=image.size
    check((w,h)==((64,512) if name.startswith('flame') else (64,256) if name=='coal_on' else (manifest['atlas'],manifest['atlas']) if name=='satin_atlas' else (64,64)),name+' expected dimensions')
for name,count in [('coal_on',4),('flame_on',8),('flame_back_on',8)]:
    meta=read(A/f'textures/block/coal_generator_v2/{name}.png.mcmeta')
    check(meta['animation']['interpolate'] and all(0<=n<count for n in meta['animation']['frames']),name+' smooth valid animation')
check(manifest['pixels_per_model_unit']==16,'Same texel density as approved pipe V2 materials')
baseline=read(DEV/'baseline_hashes.json');changed=[];new=[];external=[]
for p in (ROOT/'src/main').rglob('*'):
    if not p.is_file():continue
    rel=p.relative_to(ROOT).as_posix();sha=hashlib.sha256(p.read_bytes()).hexdigest()
    if baseline.get(rel)==sha:continue
    scoped='/assets/domesurvival/' in rel and ('/coal_generator_v2/' in rel or p.name.startswith('coal_generator') and p.suffix=='.json')
    if rel=='src/main/java/com/wasted/domesurvival/forge/block/ModBlocks.java':
        head=subprocess.run(['git','show','HEAD:'+rel],cwd=ROOT,capture_output=True,check=True).stdout.decode().replace('\r\n','\n')
        current=p.read_text();marker='() -> new CoalGeneratorBlock(BlockBehaviour.Properties.copy(Blocks.IRON_BLOCK)\n';updated=marker+'                    .noOcclusion()\n'
        expected=head if updated in head else head.replace(marker,updated)
        check(current==expected,'Only coal-generator noOcclusion property changes in shared registry');scoped=current==expected
    if scoped:(changed if rel in baseline else new).append(rel)
    else:
        head=subprocess.run(['git','show','HEAD:'+rel],cwd=ROOT,capture_output=True)
        ok=head.returncode==0 and head.stdout.replace(b'\r\n',b'\n')==p.read_bytes().replace(b'\r\n',b'\n')
        check(ok,'Unrelated concurrent committed change: '+rel);external.append(rel)
for rel in baseline:check((ROOT/rel).exists(),'Original file retained: '+rel)
check(all('/java/' not in p or p.endswith('/block/ModBlocks.java') for p in changed+new),'Transport and generation logic unchanged')
check(len(models)==15,'Two body states, inventory and twelve dedicated port models')
result={'pass':all(c['pass'] for c in checks),'checks':len(checks),'failures':[c for c in checks if not c['pass']],'changed':changed,'new':new,'external_committed':external}
(DEV/'asset_validation.json').write_text(json.dumps(result,indent=2)+'\n');print(json.dumps({k:v for k,v in result.items() if k not in ('changed','new','external_committed')},indent=2));assert result['pass']
