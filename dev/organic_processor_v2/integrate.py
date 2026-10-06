from pathlib import Path
import json, re
root=Path(__file__).resolve().parents[2]
java=root/'src/main/java/com/wasted/domesurvival/forge'
def put(p,s): p.write_text(s,encoding='utf-8')
for kind in ('Renderer','Preview'):
    s=(java/f'client/render/FilterRegeneration{kind}.java').read_text(encoding='utf-8')
    s=s.replace('machine.filter','machine.organic').replace('FilterRegeneration','OrganicProcessor').replace('FILTER_REGENERATION_STATION','ORGANIC_PROCESSOR').replace('FILTER_REGENERATION_BLOCK_ENTITY','ORGANIC_PROCESSOR_BLOCK_ENTITY').replace('filter_regeneration_station_','organic_processor_').replace('carriage','rotor').replace('cleaning carriage','mixing rotor').replace('Missing filler assembly','Missing organic processor assembly')
    s=s.replace('pose.pushPose();pose.translate(0,(1-Math.cos(rotation*Math.PI*2))*.125,0);','pose.pushPose();pose.translate(.5,0,5/16D);pose.mulPose(Axis.YP.rotationDegrees(rotation*360));pose.translate(-.5,0,-5/16D);')
    put(java/f'client/render/OrganicProcessor{kind}.java',s)
s=(java/'machine/filter/FilterRegenerationScreen.java').read_text(encoding='utf-8').replace('machine.filter','machine.organic').replace('FilterRegeneration','OrganicProcessor').replace('filter_regeneration_v2','organic_processor_v2')
s=s.replace('g,leftPos+123,topPos+76,27','g,leftPos+134,topPos+73,27')
s=s.replace('int width=(int)Math.min(85,(long)menu.progress()*85/Math.max(1,menu.progressMax()));','''com.wasted.domesurvival.forge.client.gui.MachineGaugeRenderer.fluid(g,leftPos+41,topPos+46,14,46,menu.waterStored(),menu.waterCapacity(),com.wasted.domesurvival.forge.fluid.ModFluids.PURIFIED_WATER.get());
            int width=(int)Math.min(130,(long)menu.progress()*130/Math.max(1,menu.progressMax()));''')
s=s.replace('leftPos+82,topPos+111,leftPos+82+width,topPos+119','leftPos+69,topPos+115,leftPos+69+width,topPos+121').replace('widget(g,82,111,85,8','widget(g,69,115,130,6')
s=s.replace('text(g,t("media"),46,49,54,MUTED);','')
s=s.replace('new Rect(79,108,91,14)','new Rect(66,112,136,12)').replace('menu.cycleEnergy()','menu.recipeEnergy()').replace(',menu.regenerationCycles(),menu.maxRegenerationCycles()',',menu.waterRequired()')
s=s.replace('else if(inside(x,y,new Rect(96,45,71,57)))','else if(inside(x,y,new Rect(38,43,20,52)))help(g,t("water",menu.waterStored(),menu.waterCapacity()),x,y);\n        else if(inside(x,y,new Rect(100,45,64,57)))')
put(java/'machine/organic/OrganicProcessorScreen.java',s)
s=(java/'compat/jei/DomeSurvivalJeiPlugin.java').read_text(encoding='utf-8')
s=re.sub(r'import .*OrganicProcessor.*;\n','',s)
s=re.sub(r'    public static final RecipeType<OrganicProcessorRecipe> ORGANIC_PROCESSING =\s*RecipeType.create[^;]+;\s*','',s)
s=s.replace('new IndustrialCrusherJeiCategory(guiHelper),\n                new OrganicProcessorJeiCategory(guiHelper)','new IndustrialCrusherJeiCategory(guiHelper)')
s=re.sub(r'        registration.addRecipe(?:s|Catalysts)\(\s*ORGANIC_PROCESSING,.*?\n        \);\n','',s,flags=re.S)
put(java/'compat/jei/DomeSurvivalJeiPlugin.java',s)
(java/'compat/jei/OrganicProcessorJeiCategory.java').unlink()
# Reuse the compact recipe ingredient registration and preserve exact counts.
s=(java/'client/jei/OrganicProcessorRecipeCategory.java').read_text(encoding='utf-8')
s=s.replace('HEIGHT = 118','HEIGHT = 128')
for k,v in {'PRIMARY_X':17,'PRIMARY_Y':33,'ADDITIVE_X':17,'ADDITIVE_Y':66,'WATER_X':47,'WATER_Y':35,'OUTPUT_Y':66,'PROGRESS_X':75,'PROGRESS_Y':90,'PROGRESS_W':89,'PROGRESS_H':6}.items():
    s=re.sub(rf'{k} = \d+',f'{k} = {v}',s)
s=s.replace('.setStandardSlotBackground()','').replace('.setOutputSlotBackground()','').replace('true, 16, 16','true, 12, 42')
a=s.index('        DomeJeiStyle.drawIndustrialPanel');b=s.index('    private static List<ItemStack>',a)
s=s[:a]+'''        var panel = new net.minecraft.resources.ResourceLocation("domesurvival","textures/gui/organic_processor_v2/jei.png");
        graphics.blit(panel,0,0,180,128,0,0,720,512,720,512);
        DomeJeiStyle.drawCenteredClamped(graphics,getTitle(),90,10,160,DomeJeiStyle.TEXT);
        com.wasted.domesurvival.forge.client.render.OrganicProcessorPreview.draw(graphics,104,57,25,true,0,0);
        int filled=(int)(PROGRESS_W*DomeJeiStyle.animationFraction(recipe.getProcessingTime()));
        if(filled>0){
            graphics.enableScissor(PROGRESS_X,PROGRESS_Y,PROGRESS_X+filled,PROGRESS_Y+PROGRESS_H);
            graphics.blit(new net.minecraft.resources.ResourceLocation("domesurvival","textures/gui/coal_generator_v2/widgets.png"),PROGRESS_X,PROGRESS_Y,PROGRESS_W,PROGRESS_H,0,192,256,32,512,256);
            graphics.disableScissor();
        }
        DomeJeiStyle.drawCenteredClamped(graphics,Component.literal(recipe.getEnergy()+" FE · "+seconds(recipe.getProcessingTime())),90,111,158,DomeJeiStyle.TEXT_DIM);
    }

'''+s[b:]
put(java/'client/jei/OrganicProcessorRecipeCategory.java',s)
ru={'module_title':'Два слота модулей','module_next':'Со следующего цикла','module_conflict':'Разгон / экономия','input':'Вход','input_short':'Сырьё · вода · FE','output':'Выход','output_short':'Готовый продукт','module_help':'Совместимы модули буфера, экономии и разгона. Разгон и экономия несовместимы. Изменения действуют со следующего цикла.','buffer_help':'Буфер: +75% к запасу энергии. Снятие доступно, когда запас не превышает 50 000 FE.','efficiency_help':'Экономия: расход энергии −20%, длительность цикла +10%.','overdrive_help':'Разгон: длительность цикла −20%, расход энергии +25%.','unsupported_module':'Этот модуль не поддерживается.','front':'Рабочая камера — порт отключён.','input_help':'Приём сырья, очищенной воды и энергии.','output_help':'Выдача готового продукта.','off':'Порт отключён.','energy':'Энергия: %s / %s FE','water':'Очищенная вода: %s / %s mB','cycle':'Цикл: %s FE · %s с · %s mB','status.0':'Готов к работе','status.1':'Биосинтез','status.2':'Недостаточно энергии','status.3':'Нет подходящего рецепта','status.4':'Недостаточно сырья','status.5':'Недостаточно очищенной воды','status.6':'Выход заполнен'}
en={'module_title':'Two module slots','module_next':'Applies next cycle','module_conflict':'Overdrive / efficiency','input':'Input','input_short':'Items · water · FE','output':'Output','output_short':'Finished product','module_help':'Accepts buffer, efficiency and overdrive modules. Efficiency and overdrive conflict. Changes apply next cycle.','buffer_help':'Buffer: +75% energy capacity. Remove only when stored energy is at most 50,000 FE.','efficiency_help':'Efficiency: −20% energy cost, +10% cycle time.','overdrive_help':'Overdrive: −20% cycle time, +25% energy cost.','unsupported_module':'Unsupported module.','front':'Working chamber — port disabled.','input_help':'Accepts ingredients, purified water and energy.','output_help':'Extracts finished products.','off':'Port disabled.','energy':'Energy: %s / %s FE','water':'Purified water: %s / %s mB','cycle':'Cycle: %s FE · %s s · %s mB','status.0':'Ready','status.1':'Biosynthesis','status.2':'Insufficient energy','status.3':'No matching recipe','status.4':'Insufficient ingredients','status.5':'Insufficient purified water','status.6':'Output full'}
for locale,values in [('ru_ru',ru),('en_us',en)]:
    p=root/f'src/main/resources/assets/domesurvival/lang/{locale}.json'
    data=json.loads(p.read_text(encoding='utf-8'));data.update({'gui.domesurvival.organic_processor_v2.'+k:v for k,v in values.items()})
    put(p,json.dumps(data,ensure_ascii=False,indent=2)+'\n')
for name in ('build_release.ps1','run_review.ps1','review.init.gradle'):
    s=(root/f'dev/filter_regeneration_v2/{name}').read_text().replace('filter_regeneration_v2','organic_processor_v2').replace('filter-regeneration-review','organic-processor-review').replace('filterRegeneration','organicProcessor').replace('FilterRegeneration','OrganicProcessor')
    put(root/f'dev/organic_processor_v2/{name}',s)
