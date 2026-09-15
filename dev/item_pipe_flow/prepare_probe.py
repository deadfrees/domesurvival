from pathlib import Path
root=Path(__file__).resolve().parents[2]
old=root/'dev/item_pipe_visual'
out=root/'dev/item_pipe_flow'
for name in ['item_pipe_test.init.gradle','run_review.ps1']:
    text=(old/name).read_text(encoding='utf-8-sig').replace('dev/item_pipe_visual','dev/item_pipe_flow').replace('item-pipe-review','item-pipe-flow-review')
    (out/name).write_text(text,encoding='utf-8')
text=(old/'runtime_probe/ItemPipeProbe.java').read_text(encoding='utf-8-sig')
header=text[:text.index('    @SubscribeEvent public static void server')]
header=header.replace('dev/item_pipe_visual','dev/item_pipe_flow')
header=header.replace('import com.mojang', 'import net.minecraft.nbt.CompoundTag;\nimport net.minecraft.nbt.Tag;\nimport com.wasted.domesurvival.forge.block.ModBlocks;\nimport com.wasted.domesurvival.forge.machine.coal.*;\nimport com.wasted.domesurvival.forge.machine.side.*;\nimport com.mojang',1)
helpers=text[text.index('    static void add('):text.index('    static void network()')]
client=text[text.index('    @SubscribeEvent public static void client'):]
client=client.replace('item_pipe_review_', 'item_pipe_flow_review_').replace('ticks>260','ticks>25')
body=(out/'probe_body.txt').read_text(encoding='utf-8')
(out/'runtime_probe').mkdir(exist_ok=True)
(out/'runtime_probe/ItemPipeProbe.java').write_text(header+body+helpers+client,encoding='utf-8')
for lang,value in [('en_us','Travel speed: %s blocks/s'),('ru_ru','Скорость движения: %s блок/с')]:
    file=root/f'src/main/resources/assets/domesurvival/lang/{lang}.json'
    text=file.read_text(encoding='utf-8-sig')
    if '"item.domesurvival.item_pipe.travel_speed"' not in text:
        text=text.replace('{','{\n  "item.domesurvival.item_pipe.travel_speed": "'+value+'",',1)
        file.write_text(text,encoding='utf-8')
