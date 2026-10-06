"""One-time integration of the verified steel storage behavior into the titan tier."""
from pathlib import Path
import re,json,hashlib
root=Path(__file__).resolve().parents[2];out=Path(__file__).parent
java=root/'src/main/java/com/wasted/domesurvival/forge'
baseline=json.loads((out/'baseline.json').read_text())
def convert(s):
    for name in ('EnergyBufferBlockEntity','EnergyBufferBlock','EnergyBufferMenu','EnergyBufferScreen'):
        s=re.sub(r'\b'+name+r'\b','Titan'+name,s)
    return s.replace('SteelBufferPreview','TitanBufferPreview').replace('ENERGY_BUFFER.get()','ENERGY_BUFFER_TITAN.get()').replace('"block.domesurvival.energy_buffer"','"block.domesurvival.energy_buffer_titan"')
for folder,stem in [('machine/energy','EnergyBufferBlockEntity'),('machine/energy','EnergyBufferMenu'),('client/screen','EnergyBufferScreen')]:
    target=java/f'{folder}/Titan{stem}.java';rel=target.relative_to(root).as_posix()
    assert hashlib.sha256(target.read_bytes()).hexdigest()==baseline[rel],f'Changed since snapshot: {rel}'
    s=convert((java/f'{folder}/{stem}.java').read_text())
    if stem.endswith('BlockEntity'):s=s.replace('250_000','1_000_000').replace('= 256;','= 1_024;')
    if stem.endswith('Screen'):
        s=s.replace('Steel storage instruments','Titan storage instruments').replace('textures/gui/steel_buffer_v2/','textures/gui/titan_buffer_v2/')
    target.write_text(s,encoding='utf-8')
p=java/'client/render/TitanBufferPreview.java'
assert not p.exists()
p.write_text(convert((java/'client/render/SteelBufferPreview.java').read_text()),encoding='utf-8')
p=java/'client/EnergyStorageTransferRateOverlay.java';s=p.read_text()
s=s.replace('import com.wasted.domesurvival.forge.client.screen.EnergyBufferScreen;','import com.wasted.domesurvival.forge.client.screen.EnergyBufferScreen;\nimport com.wasted.domesurvival.forge.client.screen.TitanEnergyBufferScreen;')
s=s.replace('The refined steel screen owns its instruments','The refined storage screens own their instruments').replace('if (screen instanceof EnergyBufferScreen) return;','if (screen instanceof EnergyBufferScreen || screen instanceof TitanEnergyBufferScreen) return;');p.write_text(s,encoding='utf-8')
p=java/'block/ModBlocks.java';s=p.read_text();needle='new TitanEnergyBufferBlock(BlockBehaviour.Properties.copy(Blocks.IRON_BLOCK)\n                    .strength(4.5F, 10.0F))'
assert needle in s;s=s.replace(needle,needle[:-1]+'.noOcclusion())');p.write_text(s,encoding='utf-8')
p=java/'item/EngineerWrenchItem.java';s=p.read_text();needle='            if(level.getBlockEntity(pos) instanceof com.wasted.domesurvival.forge.machine.energy.EnergyBufferBlockEntity buffer)\n                buffer.rotateSideConfiguration(state.getValue(BlockStateProperties.HORIZONTAL_FACING));'
assert needle in s;s=s.replace(needle,needle+'\n'+needle.replace('EnergyBufferBlockEntity','TitanEnergyBufferBlockEntity'));p.write_text(s,encoding='utf-8')
p=root/'src/main/resources/data/minecraft/tags/blocks/needs_stone_tool.json';j=json.loads(p.read_text());v='domesurvival:energy_buffer_titan'
if v not in j['values']:j['values'].append(v)
p.write_text(json.dumps(j,indent=2)+'\n')
# The new panel follows the same layout, reusing the established labels and widgets.
p=root/'source_assets/blender/scripts/create_titan_buffer_gui.py'
assert not p.exists();p.write_text((p.with_name('create_steel_buffer_gui.py')).read_text().replace('steel_buffer','titan_buffer').replace('Steel buffer','Titan buffer').replace('STEEL_BUFFER','TITAN_BUFFER'))
# Build isolated runtime checks from the reviewed steel fixture with real titan limits.
s=(root/'dev/steel_buffer_v2/runtime_probe/SteelBufferProbe.java').read_text();s=convert(s)
for a,b in [('steelbufferprobe','titanbufferprobe'),('SteelBufferProbe','TitanBufferProbe'),('steelBufferReview','titanBufferReview'),('SteelBufferReview','TitanBufferReview'),('steel_buffer_v2','titan_buffer_v2'),('steel-buffer-review','titan-buffer-review'),('STEEL_REVIEW','TITAN_REVIEW'),('steel_review_','titan_review_'),('Steel buffer review','Titan buffer review')]:s=s.replace(a,b)
numbers={250000:1000000,750000:3000000,700000:2800000,62500:250000,256:1024,9744:8976,156:924,412:1948,200:800,56:224}
s=re.sub(r'\b(?:'+ '|'.join(map(str,numbers))+r')\b',lambda m:str(numbers[int(m[0])]),s)
s=s.replace('receiveEnergy(1000,','receiveEnergy(9999,').replace('preserves 256 FE','preserves 1024 FE')
p=out/'runtime_probe/TitanBufferProbe.java';p.parent.mkdir(exist_ok=True);p.write_text(s,encoding='utf-8')
s=(root/'dev/steel_buffer_v2/verify_release.py').read_text();s=convert(s).replace('steel_buffer_v2','titan_buffer_v2').replace('steelbufferprobe','titanbufferprobe').replace('steel-buffer-review','titan-buffer-review').replace('energy_buffer.json','energy_buffer_titan.json')
# UI text is deliberately shared with the steel storage controls.
s=s.replace("k.startswith('gui.domesurvival.titan_buffer_v2.')","k.startswith('gui.domesurvival.steel_buffer_v2.')")
(out/'verify_release.py').write_text(s,encoding='utf-8')
print('Titan logic, GUI, preview, runtime fixture integrated')
