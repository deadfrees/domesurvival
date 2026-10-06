"""Integrate the verified buffer implementation without changing adamantium limits."""
from pathlib import Path
import re,json,hashlib
root=Path(__file__).resolve().parents[2];out=Path(__file__).parent
java=root/'src/main/java/com/wasted/domesurvival/forge';baseline=json.loads((out/'baseline.json').read_text())
def convert(s):
    return s.replace('Titan','Adamantium').replace('titan','adamantium').replace('TITAN','ADAMANTIUM')
for folder,stem in [('machine/energy','EnergyBufferBlockEntity'),('machine/energy','EnergyBufferMenu'),('client/screen','EnergyBufferScreen')]:
    p=java/f'{folder}/Adamantium{stem}.java';rel=p.relative_to(root).as_posix()
    s=convert((java/f'{folder}/Titan{stem}.java').read_text())
    if stem.endswith('BlockEntity'):s=s.replace('1_000_000','4_000_000').replace('1_024','4_096')
    assert hashlib.sha256(p.read_bytes()).hexdigest()==baseline[rel] or p.read_text()==s,rel
    p.write_text(s,encoding='utf-8')
p=java/'client/render/AdamantiumBufferPreview.java'
assert not p.exists() or p.read_text()==convert((java/'client/render/TitanBufferPreview.java').read_text())
p.write_text(convert((java/'client/render/TitanBufferPreview.java').read_text()),encoding='utf-8')
p=java/'client/EnergyStorageTransferRateOverlay.java';s=p.read_text()
needle='import com.wasted.domesurvival.forge.client.screen.TitanEnergyBufferScreen;'
s=s.replace(needle,needle+'\n'+needle.replace('Titan','Adamantium'))
needle='screen instanceof TitanEnergyBufferScreen';assert needle in s
s=s.replace(needle,needle+' || screen instanceof AdamantiumEnergyBufferScreen');p.write_text(s,encoding='utf-8')
p=java/'block/ModBlocks.java';s=p.read_text();needle='new AdamantiumEnergyBufferBlock(BlockBehaviour.Properties.copy(Blocks.IRON_BLOCK)\n                    .strength(5.0F, 12.0F))'
assert needle in s;s=s.replace(needle,needle[:-1]+'.noOcclusion())');p.write_text(s,encoding='utf-8')
p=java/'item/EngineerWrenchItem.java';s=p.read_text();needle='            if(level.getBlockEntity(pos) instanceof com.wasted.domesurvival.forge.machine.energy.TitanEnergyBufferBlockEntity buffer)\n                buffer.rotateSideConfiguration(state.getValue(BlockStateProperties.HORIZONTAL_FACING));'
assert needle in s;s=s.replace(needle,needle+'\n'+needle.replace('Titan','Adamantium'));p.write_text(s,encoding='utf-8')
p=root/'src/main/resources/data/minecraft/tags/blocks/needs_stone_tool.json';j=json.loads(p.read_text());v='domesurvival:energy_buffer_adamantium'
if v not in j['values']:j['values'].append(v)
p.write_text(json.dumps(j,indent=2)+'\n')
p=root/'source_assets/blender/scripts/create_adamantium_buffer_gui.py';assert not p.exists()
p.write_text(convert(p.with_name('create_titan_buffer_gui.py').read_text()))
s=convert((root/'dev/titan_buffer_v2/runtime_probe/TitanBufferProbe.java').read_text())
numbers={1000000:4000000,3000000:12000000,2800000:11200000,250000:1000000,1024:4096,8976:95904,924:3996,1948:8092,800:3200,224:896,10000:100000}
s=re.sub(r'\b(?:'+ '|'.join(map(str,numbers))+r')\b',lambda m:str(numbers[int(m[0])]),s)
# Finish checking the live rate before the 100kFE test cell can fill at 4096 FE/t.
s=s.replace('add(80,()->{check(screen().getMenu().getOutputPerTick()', 'add(30,()->{check(screen().getMenu().getOutputPerTick()')
p=out/'runtime_probe/AdamantiumBufferProbe.java';p.parent.mkdir(exist_ok=True);p.write_text(s,encoding='utf-8')
(out/'verify_release.py').write_text(convert((root/'dev/titan_buffer_v2/verify_release.py').read_text()),encoding='utf-8')
print('Adamantium tier integrated; original 4M FE / 4096 FE per tick preserved')
