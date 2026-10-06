package com.wasted.domesurvival.sievejeiprobe;

import com.mojang.blaze3d.platform.NativeImage;
import net.minecraft.client.*;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.world.Difficulty;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.*;
import net.minecraft.world.level.*;
import net.minecraft.world.level.levelgen.WorldOptions;
import net.minecraft.world.level.levelgen.presets.WorldPresets;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import java.nio.file.*;
import java.util.*;

/** Render the actual machine screens with controlled synchronized values. */
@Mod.EventBusSubscriber(modid="domesurvival",value=Dist.CLIENT)
public final class SieveJeiProbe {
    static final boolean ENABLED=Boolean.getBoolean("dome.sieveJeiReview");
    static final Path OUT=Path.of("../../dev/sieve_jei/runtime").toAbsolutePath().normalize();
    static boolean started,planned;static int ticks,frames,step,failures;
    static ContainerData data;static NativeImage empty,full;
    record Step(int delay,Runnable action){}static final List<Step> steps=new ArrayList<>();
    static void log(String text){System.out.println("[GAUGES] "+text);try{Files.createDirectories(OUT);Files.writeString(OUT.resolve("checks.txt"),text+"\n",StandardOpenOption.CREATE,StandardOpenOption.APPEND);}catch(Exception e){throw new RuntimeException(e);}}
    static void check(boolean ok,String text){if(!ok)failures++;log((ok?"PASS ":"FAIL ")+text);}
    static void add(int delay,Runnable action){steps.add(new Step(delay,action));}
    static NativeImage snapshot(String name){NativeImage img=Screenshot.takeScreenshot(Minecraft.getInstance().getMainRenderTarget());try{img.writeToFile(OUT.resolve(name+".png"));}catch(Exception e){throw new RuntimeException(e);}return img;}
    static void plan(){
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
    @SubscribeEvent public static void client(TickEvent.ClientTickEvent e){
        if(!ENABLED||e.phase!=TickEvent.Phase.END)return;var mc=Minecraft.getInstance();
        if(!mc.gameDirectory.toPath().toAbsolutePath().normalize().endsWith("sieve-jei-review"))throw new IllegalStateException("Isolated review required");
        if(!started&&mc.screen!=null&&mc.screen.getClass().getSimpleName().equals("AccessibilityOnboardingScreen")&&mc.getOverlay()==null){mc.setScreen(new TitleScreen());return;}
        if(!started&&mc.screen instanceof TitleScreen&&mc.getOverlay()==null){
            started=true;mc.getLanguageManager().setSelected("ru_ru");mc.options.languageCode="ru_ru";mc.reloadResourcePacks();mc.options.pauseOnLostFocus=false;mc.options.guiScale().set(2);mc.options.renderDistance().set(6);mc.options.enableVsync().set(false);mc.options.framerateLimit().set(120);mc.getTutorial().setStep(net.minecraft.client.tutorial.TutorialSteps.NONE);
            mc.createWorldOpenFlows().createFreshLevel("gauges_"+System.currentTimeMillis(),new LevelSettings("Gauge review",GameType.CREATIVE,false,Difficulty.PEACEFUL,true,new GameRules(),WorldDataConfiguration.DEFAULT),new WorldOptions(63L,false,false),a->a.registryOrThrow(Registries.WORLD_PRESET).getHolderOrThrow(WorldPresets.FLAT).value().createWorldDimensions());return;
        }
        if(mc.player==null||mc.level==null||mc.getOverlay()!=null)return;ticks++;
        if(!planned&&ticks>100){planned=true;ticks=0;plan();}
    }
    @SubscribeEvent public static void render(TickEvent.RenderTickEvent e){
        if(!ENABLED||!planned||e.phase!=TickEvent.Phase.END||step>=steps.size())return;
        // Tick catch-up can run several updates without rendering: snapshots must follow frames.
        if(++frames>=steps.get(step).delay()){
            frames=0;try{steps.get(step++).action().run();}catch(Throwable ex){check(false,"Exception "+ex);ex.printStackTrace();log("RESULT FAIL");Minecraft.getInstance().stop();}
        }
    }
}
