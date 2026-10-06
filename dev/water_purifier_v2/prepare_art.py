from pathlib import Path
import json
R=Path(__file__).resolve().parents[2];J=R/'src/main/java/com/wasted/domesurvival/forge'
p=J/'machine/water/WaterPurifierMenu.java';p.write_text(p.read_text().replace('SHAFT_FURNACE','WATER_PURIFIER'))
s=(J/'client/render/FormingPressRenderer.java').read_text().replace('machine.forming.*','machine.water.*').replace('FormingPress','WaterPurifier').replace('forming_press_tool','water_purifier_pump')
s=s.replace('FormingPressRegistry.FORMING_PRESS_BLOCK_ENTITY','com.wasted.domesurvival.forge.registry.ModBlockEntities.WATER_PURIFIER')
s=s.replace('WaterPurifierRegistry.FORMING_PRESS_BLOCK_ENTITY','com.wasted.domesurvival.forge.registry.ModBlockEntities.WATER_PURIFIER')
s=s.replace('renderTool(press.toolOffset(partialTick),press.getMachineFacing(),pose,buffers,light,overlay);','renderAssembly(press.pumpAngle(partialTick),press.getMachineFacing(),press.rawAmount(),press.purifiedAmount(),pose,buffers,light,overlay);')
s=s.replace('public void renderTool(float offset,net.minecraft.core.Direction facing,PoseStack pose,MultiBufferSource buffers,int light,int overlay)', 'public void renderAssembly(float rotation,net.minecraft.core.Direction facing,int raw,int purified,PoseStack pose,MultiBufferSource buffers,int light,int overlay)')
s=s.replace('pose.translate(.5,offset,.5);','pose.translate(.5,0,.5);')
s=s.replace('        for(Cube cube:cubes)box(consumer,pose.last(),cube,light,overlay);', '''        // Tank fill is actual stored water, not a permanently full decoration.
        liquid(consumer,pose.last(),3.45F,raw,light,overlay);liquid(consumer,pose.last(),10.45F,purified,light,overlay);
        pose.pushPose();pose.translate(.5,5.25/16,3.8/16);pose.mulPose(Axis.ZP.rotationDegrees(rotation));pose.translate(-.5,-5.25/16,-3.8/16);
        for(Cube cube:cubes)box(consumer,pose.last(),cube,light,overlay);pose.popPose();''')
insert='''    private float[] waterUv;
    private void liquid(VertexConsumer consumer,PoseStack.Pose pose,float x,int amount,int light,int overlay){
        if(amount<=0)return;
        if(waterUv==null){
            try(var reader=Minecraft.getInstance().getResourceManager().openAsReader(new ResourceLocation("domesurvival","models/block/coal_generator_input_port_north.json"))){
                for(var element:JsonParser.parseReader(reader).getAsJsonObject().getAsJsonArray("elements"))for(var entry:element.getAsJsonObject().getAsJsonObject("faces").entrySet()){
                    var face=entry.getValue().getAsJsonObject();if(face.get("texture").getAsString().equals("#input"))waterUv=array(face.getAsJsonArray("uv"));
                }
            }catch(java.io.IOException ex){throw new IllegalStateException(ex);}
        }
        if(waterUv!=null)box(consumer,pose,new Cube(new float[]{x/16,4.05F/16,2.85F/16},new float[]{(x+2.05F)/16,(4.05F+7.3F*Math.min(4000,amount)/4000)/16,5.4F/16},waterUv),light,overlay);
    }
'''
s=s.replace('    private static void box(',insert+'    private static void box(')
(J/'client/render/WaterPurifierRenderer.java').write_text(s)
gui='''"""Purifier GUI, using the approved generator graphite material pipeline."""
from pathlib import Path
source=Path(__file__).with_name('create_coal_generator_gui.py')
helpers=source.read_text().split('main=setup(')[0]
helpers=helpers.replace('gui/coal_generator_v2','gui/water_purifier_v2').replace('blender/coal_generator_gui','blender/water_purifier_gui')
exec(compile(helpers,str(source),'exec'))
def backing(w,h):
    base(w,h)
    box('Inset instruments',8,28,w-16,112,3.3,'black',.7)
    box('Control face',9,29,w-18,110,3.5,'case',.6)
main=setup('Water purifier controls',220,266);backing(220,266)
box('Title gasket',8,6,156,16,3.5,'black',.7)
box('Title plate',9,7,154,14,3.8,'case',.5)
well(14,43,16,86,False);well(42,43,28,56,False);well(178,43,28,56,False)
well(42,107,24,24,False);well(178,107,24,24,False);well(79,108,91,14,False)
for x in (70.5,173.5):
    for y in range(47,96,8):box('Volume tick',x,y,2,.3,3.8,'trim' if 'trim' in M else 'steel',.1,.1)
box('Inventory caption',10,143,72,14,3.5,'rim',.5,.3)
box('Inventory inset',11,144,70,12,3.8,'case',.4,.3)
for row in range(3):
    for col in range(9):well(12+22*col,159+22*row,20,20,False)
for col in range(9):well(12+22*col,227,20,20,False)
for x in (7,213):
    for y in (28,143,257):screw(x,y)
render(main,'panel')
config=setup('Purifier logical ports',204,111);base(204,111)
box('Heading',4,3,196,12,3.5,'case',.5)
box('Cube recess',5,19,88,78,3.5,'black',.7);box('Legend',97,19,101,75,3.5,'well',.7)
for x,y in [(38,22),(14,46),(38,46),(62,46),(38,70),(62,70)]:well(x,y,20,20,False)
render(config,'configuration')
modules=setup('Purifier modules',204,111);base(204,111)
box('Heading',4,3,196,12,3.5,'case',.5)
box('Socket panel',5,29,38,64,3.5,'black',.6)
well(10,34,24,24,False);well(10,64,24,24,False)
box('Module specification',47,20,150,76,3.5,'well',.6)
render(modules,'modules')
card=setup('Purifier JEI',180,128);base(180,128)
box('Recipe title',7,5,166,17,3.6,'case',.5)
well(13,32,24,47,False);well(143,32,24,47,False);well(46,80,24,24,False);well(76,85,58,12,False)
box('Details',7,106,166,17,3.6,'case',.4)
render(card,'jei')
bpy.context.window.scene=main;bpy.ops.wm.save_as_mainfile(filepath=str(SOURCE/'water_purifier_gui.blend'))
print('WATER_PURIFIER_GUI_COMPLETE',flush=True)
'''
(R/'source_assets/blender/scripts/create_water_purifier_gui.py').write_text(gui)
ru={
'conversion':'250 → 200 мВ за цикл','energy':'%s / %s FE','raw':'Вода: %s / %s мВ','purified':'Очищенная вода: %s / %s мВ',
'process':'Прогресс: %s / %s тиков. Стоимость цикла: %s FE.',
'module_title':'Буфер / эффективность','module_next':'или разгон · 2 гнезда','module_install':'Мышь или Shift+клик','module_conflict':'Эффект со след. цикла',
'module_help':'Установите модуль мышью или Shift+кликом на этой вкладке. Буфер: 20 000 → 35 000 FE; снимать при заряде ≤ 20 000 FE. Эффективность: скорость 90%, энергия 80%. Разгон: скорость 135%, энергия 160%. Эффективность и разгон несовместимы, дубликаты запрещены. Замена доступна во время работы; текущий цикл сохраняет параметры.',
'input':'Синий — вход','input_short':'Вода / FE / предм.','output':'Оранж. — выход','output_short':'Чистая вода / тара',
'front':'Лицевая сторона не используется для портов.','input_help':'Вход воды, энергии, вёдер воды и фильтров.','output_help':'Выход очищенной воды, пустых вёдер и полностью изношенных фильтров. Рабочий фильтр и энергия не выдаются.','off':'Сторона отключена. Нажмите для переключения.',
'bucket_help':'Ведро воды наполняет входной бак на 1000 мВ. Пустое ведро можно забрать вручную или через оранжевый порт.',
'filter_help':'Установите картридж. За цикл расходуется 1 единица ресурса. Изношенный фильтр сохраняется для регенерации.',
'jei_details':'%s FE · %s с · 1 ресурс фильтра'}
en={'conversion':'250 → 200 mB per cycle','energy':'%s / %s FE','raw':'Water: %s / %s mB','purified':'Purified water: %s / %s mB','process':'Progress: %s / %s ticks. Cycle cost: %s FE.',
'module_title':'Buffer / efficiency','module_next':'or overdrive · 2 slots','module_install':'Click or Shift-click','module_conflict':'Applies next cycle',
'module_help':'Install with the mouse or Shift-click in this tab. Buffer: 20,000 → 35,000 FE; remove at ≤ 20,000 FE. Efficiency: 90% speed, 80% energy. Overdrive: 135% speed, 160% energy. Efficiency and overdrive conflict; duplicates are not allowed. Hot swapping preserves the current cycle.',
'input':'Blue — input','input_short':'Water / FE / items','output':'Orange — output','output_short':'Clean water / items','front':'No ports on the front.','input_help':'Accepts water, energy, water buckets and cartridges.','output_help':'Extracts purified water, empty buckets and exhausted cartridges. Working filters and energy cannot be extracted.','off':'Disabled. Click to cycle modes.',
'bucket_help':'One water bucket adds 1000 mB. Retrieve the empty bucket manually or through an orange port.','filter_help':'Insert a cartridge. Each cycle uses one durability point. Exhausted cartridges are kept for regeneration.','jei_details':'%s FE · %s s · 1 filter use'}
for lang,strings in [('ru_ru',ru),('en_us',en)]:
    p=R/f'src/main/resources/assets/domesurvival/lang/{lang}.json';s=p.read_text(encoding='utf-8-sig');i=s.rfind('}')
    added=',\n'.join('  '+json.dumps('gui.domesurvival.purifier_v2.'+k)+': '+json.dumps(v,ensure_ascii=False) for k,v in strings.items())
    p.write_text(s[:i].rstrip()+',\n'+added+'\n}\n',encoding='utf-8')
print('WATER_PURIFIER_ART_PREPARED')
