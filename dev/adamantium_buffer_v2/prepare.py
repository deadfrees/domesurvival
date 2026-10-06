from pathlib import Path
import json, hashlib, shutil, difflib
root=Path(__file__).resolve().parents[2];out=Path(__file__).parent
java=root/'src/main/java/com/wasted/domesurvival/forge'
files=[java/f'machine/energy/AdamantiumEnergyBuffer{n}.java' for n in ('Block','BlockEntity','Menu')]
files += [java/'client/screen/AdamantiumEnergyBufferScreen.java',java/'client/EnergyStorageTransferRateOverlay.java',java/'block/ModBlocks.java',java/'item/EngineerWrenchItem.java']
files += list((root/'src/main/resources/assets/domesurvival/models/block').glob('energy_buffer*.json'))
files += [root/'src/main/resources/assets/domesurvival'/p for p in ('blockstates/energy_buffer_adamantium.json','models/item/energy_buffer_adamantium.json')]
files += list((root/'src/main/resources/data/domesurvival/recipes').glob('*energy_buffer*.json'))
files += list((root/'src/main/resources/data/minecraft/tags/blocks').glob('needs_*_tool.json'))
if not (out/'baseline.json').exists():
    data={}
    for p in files:
        rel=p.relative_to(root);dest=out/'baseline'/rel;dest.parent.mkdir(parents=True,exist_ok=True);shutil.copy2(p,dest);data[rel.as_posix()]=hashlib.sha256(p.read_bytes()).hexdigest()
    (out/'baseline.json').write_text(json.dumps(data,indent=2))
for name in ('run_review.ps1','build_release.ps1','review.init.gradle'):
    s=(root/'dev/steel_buffer_v2'/name).read_text().replace('steel_buffer_v2','adamantium_buffer_v2').replace('steel-buffer-review','adamantium-buffer-review').replace('steelBuffer','adamantiumBuffer').replace('SteelBuffer','AdamantiumBuffer')
    if not (out/name).exists():(out/name).write_text(s)
config=root/'run/adamantium-buffer-review/config/fancymenu'
if not config.exists():config.parent.mkdir(parents=True,exist_ok=True);shutil.copytree(root/'run/steel-buffer-review/config/fancymenu',config)
for suffix in ('BlockEntity','Menu'):
    rel=Path('src/main/java/com/wasted/domesurvival/forge/machine/energy')
    steel=(root/'dev/steel_buffer_v2/baseline'/rel/f'EnergyBuffer{suffix}.java').read_text()
    adamantium=(root/rel/f'AdamantiumEnergyBuffer{suffix}.java').read_text().replace('AdamantiumEnergyBuffer','EnergyBuffer').replace('ENERGY_BUFFER_ADAMANTIUM','ENERGY_BUFFER').replace('energy_buffer_adamantium','energy_buffer').replace('4_000_000','250_000').replace('4_096','256')
    diff=''.join(difflib.unified_diff(steel.splitlines(True),adamantium.splitlines(True)))
    print(suffix, diff or 'Same original implementation; tier constants only')
