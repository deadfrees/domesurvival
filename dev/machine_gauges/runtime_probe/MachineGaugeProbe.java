package com.wasted.domesurvival.gaugeprobe;

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
public final class MachineGaugeProbe {
    static final boolean ENABLED=Boolean.getBoolean("dome.gaugeReview");
    static final Path OUT=Path.of("../../dev/machine_gauges/runtime").toAbsolutePath().normalize();
    static final String BASE="com.wasted.domesurvival.forge.";
    record Gauge(String name,String menu,String screen,int valueIndex,int top,int height,int width){}
    static final List<Gauge> GAUGES=List.of(
            new Gauge("copper","machine.copper.CopperFurnaceMenu","machine.copper.CopperFurnaceScreen",0,55,66,12),
            new Gauge("coke","machine.shaft.CokeOvenMenu","client.screen.CokeOvenScreen",2,55,66,12),
            new Gauge("shaft","machine.shaft.ShaftFurnaceMenu","client.screen.ShaftFurnaceScreen",2,61,71,12),
            new Gauge("purifier","machine.water.WaterPurifierMenu","client.screen.WaterPurifierScreen",0,46,80,10),
            new Gauge("electrolyzer","machine.oxygen.OxygenElectrolyzerMenu","client.screen.OxygenElectrolyzerScreen",0,46,80,10),
            new Gauge("filler","machine.oxygen.OxygenFillerMenu","client.screen.OxygenFillerScreen",0,46,80,10),
            new Gauge("filter","machine.filter.FilterRegenerationMenu","machine.filter.FilterRegenerationScreen",0,46,80,10));
    static boolean started,planned;static int ticks,frames,step,failures;
    static ContainerData data;static NativeImage empty,full;
    record Step(int delay,Runnable action){}static final List<Step> steps=new ArrayList<>();
    static void log(String text){System.out.println("[GAUGES] "+text);try{Files.createDirectories(OUT);Files.writeString(OUT.resolve("checks.txt"),text+"\n",StandardOpenOption.CREATE,StandardOpenOption.APPEND);}catch(Exception e){throw new RuntimeException(e);}}
    static void check(boolean ok,String text){if(!ok)failures++;log((ok?"PASS ":"FAIL ")+text);}
    static void add(int delay,Runnable action){steps.add(new Step(delay,action));}
    static NativeImage snapshot(String name){NativeImage img=Screenshot.takeScreenshot(Minecraft.getInstance().getMainRenderTarget());try{img.writeToFile(OUT.resolve(name+".png"));}catch(Exception e){throw new RuntimeException(e);}return img;}
    static void open(String menuName,String screenName){try{
        var mc=Minecraft.getInstance();var menuClass=Class.forName(BASE+menuName);var inv=mc.player.getInventory();
        var buffer=new FriendlyByteBuf(io.netty.buffer.Unpooled.buffer());buffer.writeBlockPos(BlockPos.ZERO);
        AbstractContainerMenu menu;
        try{menu=(AbstractContainerMenu)menuClass.getConstructor(int.class,Inventory.class,FriendlyByteBuf.class).newInstance(77,inv,buffer);}finally{buffer.release();}
        var field=menuClass.getDeclaredField("data");field.setAccessible(true);data=(ContainerData)field.get(menu);
        mc.setScreen((AbstractContainerScreen<?>)Class.forName(BASE+screenName).getConstructor(menuClass,Inventory.class,Component.class).newInstance(menu,inv,Component.literal("Gauge review")));
    }catch(Exception e){throw new RuntimeException(e);}}
    static void compare(Gauge gauge,int amount,int capacity,String label){
        var mc=Minecraft.getInstance();int scale=(int)mc.getWindow().getGuiScale();
        int x=((mc.getWindow().getGuiScaledWidth()-220)/2+17)*scale;
        int y=((mc.getWindow().getGuiScaledHeight()-266)/2+gauge.top())*scale;
        int filled=capacity<=0||amount<=0?0:(int)Math.min(gauge.height(),(long)gauge.height()*amount/capacity);
        boolean matching=true;int coloured=0;
        try(var actual=snapshot(gauge.name()+"_"+label)){
            for(int dy=1;dy<gauge.height()*scale-1;dy++)for(int dx=1;dx<gauge.width()*scale-1;dx++){
                int expected=(dy>=(gauge.height()-filled)*scale?full:empty).getPixelRGBA(x+dx,y+dy);
                matching&=actual.getPixelRGBA(x+dx,y+dy)==expected;
                if(full.getPixelRGBA(x+dx,y+dy)!=empty.getPixelRGBA(x+dx,y+dy))coloured++;
            }
        }
        check(matching&&coloured>50,gauge.name()+" "+label+": fixed divisions and correctly clipped level");
    }
    static void plan(){
        for(var gauge:GAUGES){
            add(1,()->{open(gauge.menu(),gauge.screen());data.set(gauge.valueIndex(),0);data.set(gauge.valueIndex()+1,4000);});
            add(4,()->{empty=snapshot(gauge.name()+"_empty");data.set(gauge.valueIndex(),4000);});
            add(4,()->{full=snapshot(gauge.name()+"_full");data.set(gauge.valueIndex(),1000);});
            add(4,()->{compare(gauge,1000,4000,"quarter");data.set(gauge.valueIndex()+1,7000);});
            add(4,()->{compare(gauge,1000,7000,"expanded_capacity");data.set(gauge.valueIndex(),7000);});
            add(4,()->{compare(gauge,7000,7000,"expanded_full");data.set(gauge.valueIndex()+1,0);});
            add(4,()->{compare(gauge,7000,0,"zero_capacity");empty.close();full.close();});
        }
        add(1,()->{open("machine.sieve.SandSieveMenu","machine.sieve.SandSieveScreen");data.set(2,1000);data.set(3,2000);});
        add(4,()->{try(var img=snapshot("sieve_water")){
            var mc=Minecraft.getInstance();int scale=(int)mc.getWindow().getGuiScale();
            int x=((mc.getWindow().getGuiScaledWidth()-300)/2+25)*scale;
            int y=((mc.getWindow().getGuiScaledHeight()-227)/2+85)*scale;
            Set<Integer> colours=new HashSet<>();for(int dy=0;dy<12*scale;dy++)for(int dx=0;dx<12*scale;dx++)colours.add(img.getPixelRGBA(x+dx,y+dy));
            check(colours.size()>8,"Sieve uses detailed water sprite instead of flat fill");
        }});
        add(1,()->{log("RESULT "+(failures==0?"PASS":"FAIL")+" failures="+failures);Minecraft.getInstance().stop();});
    }
    @SubscribeEvent public static void client(TickEvent.ClientTickEvent e){
        if(!ENABLED||e.phase!=TickEvent.Phase.END)return;var mc=Minecraft.getInstance();
        if(!mc.gameDirectory.toPath().toAbsolutePath().normalize().endsWith("machine-gauge-review"))throw new IllegalStateException("Isolated review required");
        if(!started&&mc.screen!=null&&mc.screen.getClass().getSimpleName().equals("AccessibilityOnboardingScreen")&&mc.getOverlay()==null){mc.setScreen(new TitleScreen());return;}
        if(!started&&mc.screen instanceof TitleScreen&&mc.getOverlay()==null){
            started=true;mc.options.pauseOnLostFocus=false;mc.options.guiScale().set(2);mc.options.renderDistance().set(6);mc.options.enableVsync().set(false);mc.options.framerateLimit().set(120);mc.getTutorial().setStep(net.minecraft.client.tutorial.TutorialSteps.NONE);
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
