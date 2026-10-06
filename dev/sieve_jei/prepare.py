from pathlib import Path
import shutil
root=Path(__file__).resolve().parents[2];out=Path(__file__).parent
(out/'runtime_probe').mkdir(exist_ok=True)
for name in ('run_review.ps1','review.init.gradle','build_release.ps1'):
 s=(root/'dev/organic_processor_v2'/name).read_text().replace('organic_processor_v2','sieve_jei').replace('organic-processor-review','sieve-jei-review').replace('organicProcessor','sieveJei').replace('OrganicProcessor','SieveJei')
 (out/name).write_text(s)
s=(root/'dev/machine_gauges/runtime_probe/MachineGaugeProbe.java').read_text()
a=s.index('    static final String BASE');b=s.index('    static boolean started',a);s=s[:a]+s[b:]
a=s.index('    static void open(');b=s.index('    @SubscribeEvent public static void client(',a)
s=s[:a]+'''    static void plan(){
        var mc=Minecraft.getInstance();var runtime=JeiCapture.runtime;var manager=runtime.getRecipeManager();
        var type=com.wasted.domesurvival.forge.client.jei.DomeSurvivalJeiPlugin.SAND_SIEVE;
        var category=manager.getRecipeCategory(type);var recipes=manager.createRecipeLookup(type).get().toList();
        check(recipes.size()==6,"All six sieve recipes registered");
        // Inspect the actual layout positions, including the full 24px frame footprint.
        try {
            var layout=Class.forName("com.wasted.domesurvival.forge.client.jei.DomeMachineRecipeCategory$SlotLayout");
            var factory=layout.getDeclaredMethod("forKind",com.wasted.domesurvival.forge.client.jei.DomeMachineRecipe.Layout.class);factory.setAccessible(true);
            Object slots=factory.invoke(null,com.wasted.domesurvival.forge.client.jei.DomeMachineRecipe.Layout.SAND_SIEVE);
            for(String group:List.of("itemInputs","fluidInputs","itemOutputs")){
                var method=layout.getDeclaredMethod(group);method.setAccessible(true);
                for(Object p:(List<?>)method.invoke(slots)){
                    var x=p.getClass().getDeclaredMethod("x");var y=p.getClass().getDeclaredMethod("y");x.setAccessible(true);y.setAccessible(true);
                    int px=(int)x.invoke(p),py=(int)y.invoke(p);check(px-4>=7&&px+20<=173&&py-4>=8&&py+20<=59,"Full slot frame fits inner panel: "+group+" "+px);
                }
            }
        }catch(Exception e){throw new RuntimeException(e);}
        for(int scale:List.of(2,3))for(var recipe:recipes){
            add(1,()->{mc.options.guiScale().set(scale);mc.resizeDisplay();runtime.getRecipesGui().showRecipes(category,List.of(recipe),List.of());});
            add(35,()->{try(var img=snapshot(recipe.id().getPath().replace('/','_')+"_scale"+scale)){check(img.getWidth()>0,"Rendered "+recipe.id()+" at scale "+scale);}});
        }
        add(1,()->{log("RESULT "+(failures==0?"PASS":"FAIL")+" failures="+failures);mc.stop();});
    }
'''+s[b:]
s=s.replace('gaugeprobe','sievejeiprobe').replace('MachineGaugeProbe','SieveJeiProbe').replace('dome.gaugeReview','dome.sieveJeiReview').replace('machine_gauges','sieve_jei').replace('machine-gauge-review','sieve-jei-review')
s=s.replace('mc.options.pauseOnLostFocus=false;','mc.getLanguageManager().setSelected("ru_ru");mc.options.languageCode="ru_ru";mc.reloadResourcePacks();mc.options.pauseOnLostFocus=false;')
(out/'runtime_probe/SieveJeiProbe.java').write_text(s,encoding='utf-8')
(out/'runtime_probe/JeiCapture.java').write_text((root/'dev/organic_processor_v2/runtime_probe/JeiCapture.java').read_text().replace('organicprobe','sievejeiprobe').replace('organic_review','sieve_jei_review'))
run=root/'run/sieve-jei-review';(run/'config/fancymenu').mkdir(parents=True,exist_ok=True)
shutil.copy2(root/'run/machine-gauge-review/config/fancymenu/options.txt',run/'config/fancymenu/options.txt')
