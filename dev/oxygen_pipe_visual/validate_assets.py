from pathlib import Path
import bpy,json,hashlib,subprocess
ROOT=Path(__file__).resolve().parents[2];DEV=ROOT/'dev/oxygen_pipe_visual';A=ROOT/'src/main/resources/assets/domesurvival'
IDS=['oxygen_pipe','reinforced_oxygen_pipe','high_flow_oxygen_pipe'];checks=[]
def read(p):return json.loads(p.read_text(encoding='utf-8-sig'))
def check(ok,s):checks.append({'pass':bool(ok),'check':s})
for i,id in enumerate(IDS):
    state=read(A/f'blockstates/{id}.json');check(len(state['multipart'])==12,id+' six independent open/closed directions')
    for direction in ('north','south','east','west','up','down'):
        entries=[e for e in state['multipart'] if direction in e['when']]
        check(len(entries)==2 and {e['when'][direction] for e in entries}=={'true','false'},id+' complementary '+direction)
    for part in ('core','connection','inventory'):
        m=read(A/f'models/block/{id}_{part}.json');check(m['loader']=='forge:composite',id+'/'+part+' composite layers')
        quads=0
        for layer,child in m['children'].items():
            check(child['render_type']==('minecraft:cutout' if layer=='metal' else 'minecraft:translucent'),id+'/'+part+'/'+layer+' correct pass')
            for e in child['elements']:
                check(all(0<=lo<hi<=16 for lo,hi in zip(e['from'],e['to'])),id+'/'+e['name']+' valid bounds')
                if layer=='metal':check(e['from'][0]>=6.75 and e['to'][0]<=9.25 and e['from'][1]>=6.75 and e['to'][1]<=9.25,id+'/'+e['name']+' thin collar')
                for d,f in e['faces'].items():
                    quads+=1;u=f['uv'];check(0<=u[0]<u[2]<=16 and 0<=u[1]<u[3]<=16,id+'/'+part+'/'+d+' valid UV')
                    ref=child['textures'][f['texture'][1:]];check((A/('textures/'+ref.split(':')[1]+'.png')).exists(),id+' texture reference')
        check(quads==({'core':1,'connection':4+24*(i+1),'inventory':12+48*(i+1)}[part]),id+'/'+part+' expected lightweight quad count')
    for suffix in ('metal','glass'):
        image=bpy.data.images.load(str(A/f'textures/block/oxygen_glass/{id}_{suffix}.png'));p=list(image.pixels[:]);alpha=p[3::4]
        check(tuple(image.size)==(256,256),id+'/'+suffix+' 256px atlas')
        if suffix=='metal':check(set(alpha)<={0.,1.},id+' opaque rings')
        else:check(any(.12<a<.18 for a in alpha) and max(alpha)<.7,id+' translucent glass')
    bb=read(ROOT/f'source_assets/blockbench/oxygen_pipes/{id}.bbmodel')
    check(len(bb['elements'])==3+8*(i+1),id+' editable inventory mesh with tier rings')
    check((ROOT/f'source_assets/blender/oxygen_pipes/{id}.blend').exists(),id+' Blender master')
baseline=read(DEV/'baseline_hashes.json');changed=[];new=[]
java={'OxygenPipeTier.java','OxygenPipeTransferService.java','OxygenFlowNetwork.java','OxygenGasFlowRenderer.java'}
for p in (ROOT/'src/main').rglob('*'):
    if not p.is_file():continue
    rel=str(p.relative_to(ROOT));sha=hashlib.sha256(p.read_bytes()).hexdigest()
    if rel not in baseline:new.append(rel)
    elif baseline[rel]!=sha:changed.append(rel)
external_committed=[]
for rel in changed+new:
    p=Path(rel)
    scoped=(p.name in java and 'oxygen' in p.parts) or ('assets' in p.parts and ('oxygen_glass' in p.parts or any(p.name.startswith(id) for id in IDS)))
    if not scoped:
        head=subprocess.run(['git','show','HEAD:'+p.as_posix()],cwd=ROOT,capture_output=True)
        committed=head.returncode==0 and head.stdout.replace(b'\r\n',b'\n')==(ROOT/p).read_bytes().replace(b'\r\n',b'\n')
        check(committed,'Unrelated concurrent change already committed: '+rel)
        if committed:external_committed.append(rel)
    else:check(True,'Oxygen-only scope: '+rel)
changed=[p for p in changed if p not in external_committed]
new=[p for p in new if p not in external_committed]
for rel in baseline:
    if not (ROOT/rel).exists():check(False,'Unexpected deletion: '+rel)
source=(ROOT/'src/main/java/com/wasted/domesurvival/forge/machine/oxygen/OxygenPipeTier.java').read_text()
check(all(t in source for t in ('BASIC(30, 6.75D, 9.25D)','REINFORCED(60, 6.75D, 9.25D)','HIGH_FLOW(120, 6.75D, 9.25D)')),'Fourfold slower 30/60/120 throughput and thin collisions')
result={'pass':all(c['pass'] for c in checks),'checks':len(checks),'failures':[c for c in checks if not c['pass']],'changed':changed,'new':new,'external_committed':external_committed}
(DEV/'asset_validation.json').write_text(json.dumps(result,indent=2)+'\n');print(json.dumps({k:v for k,v in result.items() if k not in ('changed','new')},indent=2));assert result['pass']
