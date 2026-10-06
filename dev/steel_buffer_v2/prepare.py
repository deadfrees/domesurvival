from pathlib import Path
import json,hashlib,shutil
root=Path(__file__).resolve().parents[2];out=Path(__file__).parent
files=[root/f'src/main/java/com/wasted/domesurvival/forge/machine/energy/EnergyBuffer{n}.java' for n in ('Block','BlockEntity','Menu')]
files+=[root/'src/main/java/com/wasted/domesurvival/forge/client/screen/EnergyBufferScreen.java']
files+=list((root/'src/main/resources/assets/domesurvival/models/block').glob('energy_buffer*.json'))
files+=[root/'src/main/resources/assets/domesurvival/blockstates/energy_buffer.json',root/'src/main/resources/assets/domesurvival/models/item/energy_buffer.json']
files+=list((root/'src/main/resources/data/domesurvival/recipes').glob('*energy_buffer*.json'))
if not (out/'baseline.json').exists():
    data={}
    for p in files:
        rel=p.relative_to(root);dest=out/'baseline'/rel;dest.parent.mkdir(parents=True,exist_ok=True);shutil.copy2(p,dest);data[rel.as_posix()]=hashlib.sha256(p.read_bytes()).hexdigest()
    (out/'baseline.json').write_text(json.dumps(data,indent=2))
for name in ('run_review.ps1','build_release.ps1','review.init.gradle'):
    s=(root/'dev/bio_capsules'/name).read_text().replace('bio_capsules','steel_buffer_v2').replace('bio-capsules-review','steel-buffer-review').replace('bioCapsules','steelBuffer').replace('BioCapsules','SteelBuffer')
    if not (out/name).exists():(out/name).write_text(s)
config=root/'run/steel-buffer-review/config/fancymenu'
if not config.exists():config.parent.mkdir(parents=True,exist_ok=True);shutil.copytree(root/'run/bioincubator-review/config/fancymenu',config)
