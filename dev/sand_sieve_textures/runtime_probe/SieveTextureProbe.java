package com.wasted.domesurvival.sievetextureprobe;

import com.mojang.blaze3d.platform.NativeImage;
import com.wasted.domesurvival.forge.block.ModBlocks;
import com.wasted.domesurvival.forge.item.ModItems;
import com.wasted.domesurvival.forge.machine.sieve.*;
import net.minecraft.client.*;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraft.core.*;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.*;
import net.minecraft.world.item.*;
import net.minecraft.world.level.*;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.levelgen.WorldOptions;
import net.minecraft.world.level.levelgen.presets.WorldPresets;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.network.NetworkHooks;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.capability.IFluidHandler;
import net.minecraft.world.level.material.Fluids;
import com.wasted.domesurvival.forge.itempipe.*;
import com.wasted.domesurvival.forge.transport.fluid.*;
import java.nio.file.*;
import java.util.*;

/** Isolated visual review; no production classes are changed by this harness. */
@Mod.EventBusSubscriber(modid="domesurvival",value=Dist.CLIENT)
public final class SieveTextureProbe {
    static final boolean ENABLED=Boolean.getBoolean("dome.sieveTextureReview");
    static final Path OUT=Path.of("../../dev/sand_sieve_textures/runtime").toAbsolutePath().normalize();
    static final BlockPos POS=new BlockPos(0,100,0);
    static boolean started,fixture,planned;static volatile boolean ready;static int ticks,step,failures;
    static int processSounds,clickSounds;
    @SubscribeEvent public static void sound(net.minecraftforge.client.event.sound.PlaySoundEvent e){
        if(!ENABLED||!planned)return;
        String id=e.getOriginalSound().getLocation().toString();
        if(id.equals("domesurvival:sand_sieve_process"))processSounds++;
        if(id.equals("minecraft:block.lever.click")||id.equals("minecraft:ui.button.click"))clickSounds++;
    }
    static List<net.minecraft.client.particle.Particle> sandDust(){
        List<net.minecraft.client.particle.Particle> found=new ArrayList<>();
        try{
            for(var field:net.minecraft.client.particle.ParticleEngine.class.getDeclaredFields()){
                if(!Map.class.isAssignableFrom(field.getType()))continue;
                field.setAccessible(true);Object value=field.get(Minecraft.getInstance().particleEngine);
                if(value instanceof Map<?,?> map)for(Object queue:map.values())if(queue instanceof Collection<?> particles)
                    for(Object particle:particles)if(particle instanceof net.minecraft.client.particle.Particle dust&&dust.getClass().getSimpleName().equals("SandGrainParticle"))found.add(dust);
            }
        }catch(ReflectiveOperationException e){throw new RuntimeException(e);}
        return found;
    }
    record Step(int delay,Runnable action){}static final List<Step> steps=new ArrayList<>();
    static synchronized void log(String s){System.out.println("[SIEVE_TEXTURE] "+s);try{Files.createDirectories(OUT);Files.writeString(OUT.resolve("checks.txt"),s+"\n",StandardOpenOption.CREATE,StandardOpenOption.APPEND);}catch(Exception ex){throw new RuntimeException(ex);}}
    static void check(boolean ok,String s){if(!ok)failures++;log((ok?"PASS ":"FAIL ")+s);}
    @SubscribeEvent public static void server(TickEvent.ServerTickEvent e){
        if(!ENABLED||e.phase!=TickEvent.Phase.END||fixture)return;
        var mc=Minecraft.getInstance();var server=mc.getSingleplayerServer();if(server==null||server.getPlayerList().getPlayers().isEmpty())return;
        if(!mc.gameDirectory.toPath().toAbsolutePath().normalize().endsWith("sand-sieve-texture-review"))throw new IllegalStateException("Isolated review required");
        fixture=true;try{
            var l=server.overworld();var p=server.getPlayerList().getPlayers().get(0);
            l.setDayTime(6000);l.getGameRules().getRule(GameRules.RULE_DAYLIGHT).set(false,server);l.getGameRules().getRule(GameRules.RULE_DOMOBSPAWNING).set(false,server);
            for(int x=-5;x<7;x++)for(int z=-5;z<7;z++)l.setBlockAndUpdate(new BlockPos(x,99,z),Blocks.SMOOTH_STONE.defaultBlockState());
            l.setBlockAndUpdate(POS,ModBlocks.SAND_SIEVE.get().defaultBlockState());
            var f=(SandSieveBlockEntity)l.getBlockEntity(POS);f.getInventory().setStackInSlot(1,new ItemStack(ModItems.COPPER_SIEVE_MESH.get()));
            check(!f.getCapability(ForgeCapabilities.ENERGY,Direction.UP).isPresent(),"No energy capability added");
            for(Direction d:Direction.values()){
                var items=f.getCapability(ForgeCapabilities.ITEM_HANDLER,d).orElseThrow(()->new IllegalStateException("Missing item port"));
                if(d==Direction.DOWN){
                    f.getInventory().setStackInSlot(2,new ItemStack(Items.CLAY_BALL,3));
                    check(items.getSlots()==3&&items.extractItem(0,1,false).is(Items.CLAY_BALL),"Bottom port extracts results");
                    f.getInventory().setStackInSlot(2,ItemStack.EMPTY);
                }else check(items.getSlots()==2&&items.insertItem(0,new ItemStack(Items.SAND),true).isEmpty(),"Fixed item input "+d);
                var water=f.getCapability(ForgeCapabilities.FLUID_HANDLER,d).orElseThrow(()->new IllegalStateException("Missing water port"));
                check(water.fill(new FluidStack(Fluids.WATER,250),IFluidHandler.FluidAction.SIMULATE)==250,"Water port "+d);
            }
            check(l.getBlockState(POS).getProperties().stream().noneMatch(v->v.getName().startsWith("port_")),"No visible port state added");
            l.setBlockAndUpdate(POS.west(),ItemPipeRegistry.STEEL_PIPE.get().defaultBlockState());
            l.setBlockAndUpdate(POS.east(),FluidPipeRegistry.BASIC_FLUID_PIPE.get().defaultBlockState());
            check(ItemPipeBlock.refreshConnections(l,POS.west(),l.getBlockState(POS.west())).getValue(ItemPipeBlock.EAST),"Item pipe connects to hidden input");
            check(FluidPipeBlock.refreshConnections(l,POS.east(),l.getBlockState(POS.east())).getValue(FluidPipeBlock.WEST),"Water pipe connects without visible socket");
            l.removeBlock(POS.west(),false);l.removeBlock(POS.east(),false);
            p.teleportTo(l,1.6,100.3,-2.7,19,23);p.getAbilities().flying=true;p.onUpdateAbilities();p.setItemInHand(InteractionHand.MAIN_HAND,new ItemStack(ModBlocks.SAND_SIEVE.get()));ready=true;
        }catch(Throwable ex){check(false,"Server exception "+ex);ready=true;}
    }
    static void add(int delay,Runnable r){steps.add(new Step(delay,r));}
    static void shot(String name){try(NativeImage img=Screenshot.takeScreenshot(Minecraft.getInstance().getMainRenderTarget())){Files.createDirectories(OUT);img.writeToFile(OUT.resolve(name+".png"));log("Screenshot "+name);}catch(Exception ex){check(false,"Screenshot "+ex);}}
    static void plan(){var mc=Minecraft.getInstance();mc.setScreen(null);mc.options.hideGui=true;
        for(String name:List.of("stone_bricks","spruce_planks","stripped_spruce_log","copper_block"))
            check(mc.getResourceManager().getResource(new ResourceLocation("minecraft","textures/block/"+name+".png")).isPresent(),"Texture resolves: "+name);
        check(mc.getResourceManager().getResource(new ResourceLocation("domesurvival","sounds/machines/sand_sieve_process.ogg")).isPresent(),"Uploaded sieve sound resolves");
        add(20,()->shot("01_idle_front"));
        add(1,()->mc.getSingleplayerServer().execute(()->{
            var l=mc.getSingleplayerServer().overworld();var f=(SandSieveBlockEntity)l.getBlockEntity(POS);
            var p=mc.getSingleplayerServer().getPlayerList().getPlayers().get(0);
            f.getInventory().setStackInSlot(0,new ItemStack(Items.SAND,8));
            var state=l.getBlockState(POS);
            state.getBlock().use(state,l,POS,p,InteractionHand.MAIN_HAND,new net.minecraft.world.phys.BlockHitResult(net.minecraft.world.phys.Vec3.atCenterOf(POS),Direction.UP,POS,false));
            check(l.getBlockState(POS).getValue(SandSieveBlock.ACTIVE),"Existing manual cycle starts");
        }));
        add(10,()->{
            check(mc.level.getBlockState(POS).getValue(SandSieveBlock.ACTIVE),"Existing animation becomes active");
            check(processSounds==1&&clickSounds==0,"Activation plays uploaded sound once without vanilla click");
            var dust=sandDust();check(!dust.isEmpty(),"Sand particles visible during processing");
            check(!dust.isEmpty()&&dust.stream().allMatch(p->p.getBoundingBox().minY<POS.getY()+0.735),"Sand particles stay below the fixed mesh");
            shot("02_active_start");
        });
        add(32,()->shot("03_active_motion"));
        add(50,()->{check(!mc.level.getBlockState(POS).getValue(SandSieveBlock.ACTIVE),"Existing animation stops after cycle");mc.getSingleplayerServer().execute(()->{
            var p=mc.getSingleplayerServer().getPlayerList().getPlayers().get(0);var f=(SandSieveBlockEntity)p.serverLevel().getBlockEntity(POS);
            check(f.getInventory().getStackInSlot(0).getCount()==7,"Existing dry cycle consumes one sand");p.teleportTo(p.serverLevel(),2.2,101.0,3.4,149,24);
        });});
        add(20,()->shot("04_rear"));
        add(1,()->{mc.options.hideGui=false;mc.getSingleplayerServer().execute(()->{var p=mc.getSingleplayerServer().getPlayerList().getPlayers().get(0);p.teleportTo(p.serverLevel(),1.6,100.3,-2.7,19,23);});});
        add(20,()->shot("05_held_item"));
        add(1,()->mc.getSingleplayerServer().execute(()->{var p=mc.getSingleplayerServer().getPlayerList().getPlayers().get(0);var f=(SandSieveBlockEntity)p.serverLevel().getBlockEntity(POS);f.getWaterTank().fill(new FluidStack(Fluids.WATER,1750),IFluidHandler.FluidAction.EXECUTE);for(int i=2;i<5;i++)f.getInventory().setStackInSlot(i,ItemStack.EMPTY);f.tryStartCycle();NetworkHooks.openScreen(p,f,POS);}));
        add(18,()->{check(mc.screen instanceof SandSieveScreen,"New sieve GUI opens with existing menu");var menu=((SandSieveScreen)mc.screen).getMenu();check(menu.water()==1750&&menu.progress()>0,"Water and process synchronized");shot("06_gui_scale2");mc.options.guiScale().set(3);mc.resizeDisplay();});
        add(15,()->shot("07_gui_scale3"));
        add(50,()->{var menu=((SandSieveScreen)mc.screen).getMenu();check(menu.water()==1500&&menu.progress()==0,"Wet cycle retains existing water cost");shot("08_gui_finished");});
        add(50,()->{check(sandDust().isEmpty(),"Sand particles clear after processing stops");log("RESULT "+(failures==0?"PASS":"FAIL")+" failures="+failures);mc.stop();});
    }
    @SubscribeEvent public static void client(TickEvent.ClientTickEvent e){
        if(!ENABLED||e.phase!=TickEvent.Phase.END)return;var mc=Minecraft.getInstance();
        if(!started&&mc.screen!=null&&mc.screen.getClass().getSimpleName().equals("AccessibilityOnboardingScreen")&&mc.getOverlay()==null){mc.setScreen(new TitleScreen());return;}
        if(!started&&mc.screen instanceof TitleScreen&&mc.getOverlay()==null){
            started=true;mc.options.pauseOnLostFocus=false;mc.options.fov().set(50);mc.options.guiScale().set(2);mc.options.renderDistance().set(6);mc.options.enableVsync().set(false);mc.options.framerateLimit().set(120);mc.getTutorial().setStep(net.minecraft.client.tutorial.TutorialSteps.NONE);mc.getLanguageManager().setSelected("ru_ru");mc.options.languageCode="ru_ru";mc.reloadResourcePacks();
            mc.createWorldOpenFlows().createFreshLevel("sieve_texture_"+System.currentTimeMillis(),new LevelSettings("Sieve texture review",GameType.CREATIVE,false,Difficulty.PEACEFUL,true,new GameRules(),WorldDataConfiguration.DEFAULT),new WorldOptions(63L,false,false),a->a.registryOrThrow(Registries.WORLD_PRESET).getHolderOrThrow(WorldPresets.FLAT).value().createWorldDimensions());return;
        }
        if(!ready||mc.player==null||mc.level==null||mc.getOverlay()!=null)return;ticks++;
        if(!planned&&ticks>100){planned=true;ticks=0;plan();}
        else if(planned&&step<steps.size()&&ticks>=steps.get(step).delay()){ticks=0;try{steps.get(step++).action().run();}catch(Throwable ex){check(false,"Client exception "+ex);log("RESULT FAIL");mc.stop();}}
    }
}
