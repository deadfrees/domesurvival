"""Adapt the approved screen/preview to the electrolyzer's liquid and gas gauges."""
from pathlib import Path
import json
R=Path(__file__).resolve().parents[2];J=R/'src/main/java/com/wasted/domesurvival/forge';A=R/'src/main/resources/assets/domesurvival'
def rename(s):
    for a,b in [('WaterPurifier','OxygenElectrolyzer'),('WATER_PURIFIER','OXYGEN_ELECTROLYZER'),('water_purifier','oxygen_electrolyzer'),('machine.water','machine.oxygen'),('purifier_v2','electrolyzer_v2')]:s=s.replace(a,b)
    return s
s=rename((J/'client/screen/WaterPurifierScreen.java').read_text())
s=s.replace('menu.rawWater()','menu.water()').replace('menu.rawCapacity()','menu.waterCapacity()').replace('menu.purifiedWater()','menu.oxygen()').replace('menu.purifiedCapacity()','menu.oxygenCapacity()')
s=s.replace('net.minecraft.world.level.material.Fluids.WATER','com.wasted.domesurvival.forge.fluid.ModFluids.PURIFIED_WATER.get()')
s=s.replace('fluid(g,181,46,22,50,menu.oxygen(),menu.oxygenCapacity(),com.wasted.domesurvival.forge.fluid.ModFluids.PURIFIED_WATER.get());','gas(g,181,46,22,50,menu.oxygen(),menu.oxygenCapacity());')
s=s.replace('t("raw",','t("water",').replace('t("purified",','t("oxygen",')
s=s.replace('t("conversion"),44,31,','t("conversion"),44,33,')
s='\n'.join(line for line in s.splitlines() if 'bucket_help' not in line and 'filter_help' not in line)+'\n'
needle='    private void fluid('
i=s.index(needle)
s=s[:i]+'''    public static void gas(GuiGraphics g,int x,int y,int w,int h,int amount,int capacity) {
        com.wasted.domesurvival.forge.client.gui.OxygenGasGauge.draw(g,x,y,w,h,amount,capacity);
    }
'''+s[i:]
s=s.replace('gas(g,181,46,22,50,','gas(g,leftPos+180,topPos+45,24,52,')
(J/'client/screen/OxygenElectrolyzerScreen.java').write_text(s)
s=rename((J/'client/render/WaterPurifierPreview.java').read_text()).replace('pump in','electrode bath in').replace('raw,int purified','water,int oxygen')
s=s.replace('active?(net.minecraft.Util.getMillis()%4000)*.09F:0,Direction.NORTH,raw,purified','active?(net.minecraft.Util.getMillis()%4000)/4000F:0,Direction.NORTH,water,oxygen')
(J/'client/render/OxygenElectrolyzerPreview.java').write_text(s)
s=rename((J/'client/render/WaterPurifierRenderer.java').read_text()).replace('oxygen_electrolyzer_pump','oxygen_electrolyzer_bubbles').replace('Missing forming press tool geometry','Missing electrolyzer bubble geometry')
s=s.replace('press.pumpAngle(partialTick)','press.animationPhase(partialTick)').replace('press.rawAmount()','press.waterAmount()').replace('press.purifiedAmount()','press.oxygenAmount()')
a=s.index('        // Tank fill');b=s.index('\n    private float[] waterUv;',a)
s=s[:a]+'''        // Gas bubbles have real depth and freeze with the block's working state.
        if(raw>0) for(int i=0;i<cubes.size();i++) {
            Cube cube=cubes.get(i);float phase=(rotation+i*.167F)%1;
            pose.pushPose();pose.translate(0,(3.9F+phase*6.7F)/16-cube.lo[1],0);
            box(consumer,pose.last(),cube,light,overlay);pose.popPose();
        }
        alpha=78;
        liquid(buffers.getBuffer(RenderType.entityTranslucent(ATLAS)),pose.last(),3.6F,raw,light,overlay);
        alpha=255;pose.popPose();
    }
    private int alpha=255;
'''+s[b:]
s=s.replace('4.05F/16,2.85F/16','3.5F/16,3.20F/16').replace('(x+2.05F)/16,(4.05F+7.3F','(x+8.8F)/16,(3.5F+7.8F').replace('5.4F/16','7.4F/16')
s=s.replace('private static void box','private void box').replace('private static void quad','private void quad').replace('.color(255,255,255,255)','.color(255,255,255,alpha)')
(J/'client/render/OxygenElectrolyzerRenderer.java').write_text(s)
# New keys only, preserving all unrelated localization edits.
for locale in ('ru_ru','en_us'):
    path=A/f'lang/{locale}.json';data=json.loads(path.read_text(encoding='utf-8'));ru=locale=='ru_ru';prefix='gui.domesurvival.electrolyzer_v2.'
    for k,v in list(data.items()):
        if k.startswith('gui.domesurvival.purifier_v2.'):
            data[k.replace('purifier_v2','electrolyzer_v2')]=v.replace('20 000','30 000').replace('35 000','52 500').replace('20,000','30,000').replace('35,000','52,500')
    values={
      'conversion':('200 мВ → 96 O₂','200 mB → 96 O₂'),
      'water':('Очищенная вода: %s / %s мВ','Purified water: %s / %s mB'),
      'oxygen':('Кислород: %s / %s','Oxygen: %s / %s'),
      'input_short':('Вода / FE','Water / FE'), 'output_short':('Кислород','Oxygen'),
      'input_help':('Вход очищенной воды и энергии.','Purified water and energy input.'),
      'output_help':('Выход кислорода.','Oxygen output.'),
      'unsupported_module':('Не подходит для электролизёра.','Not supported by the electrolyzer.'),
      'jei_oxygen':('Кислород: 96 за цикл','Oxygen: 96 per cycle'),
      'buffer_help':('Ёмкость: 30 000 → 52 500 FE. Снятие при заряде ≤ 30 000 FE.','Capacity: 30,000 → 52,500 FE. Remove at ≤ 30,000 FE.')}
    for k,v in values.items():data[prefix+k]=v[0 if ru else 1]
    for k in ('raw','purified','bucket_help','filter_help'):data.pop(prefix+k,None)
    path.write_text(json.dumps(data,ensure_ascii=False,indent=2)+'\n',encoding='utf-8')
print('Electrolyzer screen, renderer and localization prepared')
