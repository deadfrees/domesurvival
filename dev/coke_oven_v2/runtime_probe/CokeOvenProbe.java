package com.wasted.domesurvival.cokeprobe;

import com.mojang.blaze3d.platform.NativeImage;
import com.wasted.domesurvival.forge.block.ModBlocks;
import com.wasted.domesurvival.forge.item.ModItems;
import com.wasted.domesurvival.forge.itempipe.ItemPipeBlock;
import com.wasted.domesurvival.forge.itempipe.ItemPipeRegistry;
import com.wasted.domesurvival.forge.machine.shaft.*;
import com.wasted.domesurvival.forge.machine.module.*;
import com.wasted.domesurvival.forge.machine.side.*;
import com.wasted.domesurvival.forge.client.screen.CokeOvenScreen;
import com.wasted.domesurvival.forge.client.jei.DomeSurvivalJeiPlugin;
import net.minecraft.client.*;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraft.core.*;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.*;
import net.minecraft.world.*;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.item.*;
import net.minecraft.world.level.*;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.levelgen.WorldOptions;
import net.minecraft.world.level.levelgen.presets.WorldPresets;
import net.minecraft.world.phys.*;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.network.NetworkHooks;
import net.minecraftforge.registries.ForgeRegistries;
import java.nio.file.*;
import java.util.*;

@Mod.EventBusSubscriber(modid="domesurvival",value=Dist.CLIENT)
public final class CokeOvenProbe {
    static final boolean ENABLED=Boolean.getBoolean("dome.cokeOvenReview");
    static final Path OUT=Path.of("../../dev/coke_oven_v2/runtime").toAbsolutePath().normalize();
    static final BlockPos DISPLAY=new BlockPos(0,100,0),TEST=new BlockPos(10,100,10);
    static boolean started,fixture,planned;static volatile boolean ready;static int ticks,step,failures;
    record Step(int delay,Runnable action){}static final List<Step> steps=new ArrayList<>();
    static synchronized void log(String s){System.out.println("[COKE_REVIEW] "+s);try{Files.createDirectories(OUT);Files.writeString(OUT.resolve("checks.txt"),s+"\n",StandardOpenOption.CREATE,StandardOpenOption.APPEND);}catch(Exception ex){throw new RuntimeException(ex);}}
    static void check(boolean ok,String s){if(!ok)failures++;log((ok?"PASS ":"FAIL ")+s);}
    static CokeOvenBlockEntity place(ServerLevel l,BlockPos p){l.setBlockAndUpdate(p,Blocks.AIR.defaultBlockState());l.setBlockAndUpdate(p,ModBlocks.COKE_OVEN.get().defaultBlockState());return (CokeOvenBlockEntity)l.getBlockEntity(p);}
    static void tick(ServerLevel l,CokeOvenBlockEntity f,int n){for(int i=0;i<n;i++)CokeOvenBlockEntity.serverTick(l,f.getBlockPos(),f.getBlockState(),f);}
    static void mode(CokeOvenBlockEntity f,RelativeSide s,SideMode m){for(int i=0;i<3&&f.sideMode(s.resolve(f.facing()))!=m;i++)f.cycleSideMode(s);}
    static List<ItemEntity> drops(ServerLevel l){return l.getEntitiesOfClass(ItemEntity.class,new AABB(TEST).inflate(2));}
    static void clear(ServerLevel l){drops(l).forEach(ItemEntity::discard);}
    static int count(ServerLevel l,Item i){return drops(l).stream().filter(e->e.getItem().is(i)).mapToInt(e->e.getItem().getCount()).sum();}
    static void process(ServerLevel l,ServerPlayer p){
        var f=place(l,TEST);var inv=f.getInventory();inv.setStackInSlot(0,new ItemStack(Items.COAL,16));inv.setStackInSlot(1,new ItemStack(Items.COAL,3));
        check(f.fuelDuration(new ItemStack(Items.COAL))==1600,"Base coal burn duration unchanged");
        tick(l,f,1599);check(inv.getStackInSlot(2).isEmpty()&&f.getDataAccess().get(0)==1599,"No output before 80 second cycle");
        tick(l,f,1);check(inv.getStackInSlot(2).is(ModItems.COAL_COKE.get())&&inv.getStackInSlot(0).getCount()==15,"One coal makes exactly one coke");
        var menu=new CokeOvenMenu(7,p.getInventory(),f);menu.setTab(200);p.getInventory().setItem(9,new ItemStack(MachineModuleItems.EFFICIENCY.get()));
        int burn=f.getDataAccess().get(2);check(!menu.quickMoveStack(p,3).isEmpty()&&f.hasEfficiency(),"Install efficiency with Shift-click while burning");
        check(f.getDataAccess().get(2)==burn&&f.getDataAccess().get(3)==1600,"Installing does not refill active fuel");
        tick(l,f,1);check(f.getDataAccess().get(3)==1840,"Next coal gets 15 percent longer burn");
        check(menu.progressMax()==1600,"Module never accelerates coke cycle");
        check(!f.getModules().insertItem(0,new ItemStack(MachineModuleItems.OVERDRIVE.get()),false).isEmpty(),"Overdrive rejected");
        check(!menu.quickMoveStack(p,39).isEmpty()&&!f.hasEfficiency(),"Remove efficiency during active processing");
        check(!f.getModules().insertItem(0,new ItemStack(MachineModuleItems.OVERDRIVE.get()),false).isEmpty()
                &&f.getModules().getStackInSlot(0).isEmpty(),"Empty module socket rejects overdrive");
        check(f.getDataAccess().get(3)==1840,"Removal retains burning fuel snapshot");
        inv.setStackInSlot(2,new ItemStack(ModItems.COAL_COKE.get(),64));int progress=menu.progress();tick(l,f,10);
        check(menu.progress()==progress,"Full output pauses instead of losing progress");
        inv.setStackInSlot(2,ItemStack.EMPTY);tick(l,f,1);check(menu.progress()==progress+1,"Clearing output resumes cycle");
        var saved=f.saveWithoutMetadata();saved.putInt("BurnTime",0);saved.putInt("BurnTimeMax",0);saved.putInt("Progress",13);f.load(saved);
        inv.setStackInSlot(1,ItemStack.EMPTY);tick(l,f,10);check(menu.progress()==13,"Missing fuel preserves unfinished cycle");
        saved=f.saveWithoutMetadata();saved.putInt("Progress",0);f.load(saved);
        inv.setStackInSlot(1,new ItemStack(Items.LAVA_BUCKET));tick(l,f,1);check(inv.getStackInSlot(1).is(Items.BUCKET)&&menu.burnTotal()==20000,"Fuel container retained");
        f.getModules().setStackInSlot(0,new ItemStack(MachineModuleItems.EFFICIENCY.get()));saved=f.saveWithoutMetadata();saved.putInt("BurnTime",70000);saved.putInt("BurnTimeMax",80000);f.load(saved);
        check(menu.burnRemaining()==70000&&menu.burnTotal()==80000,"Full-width fuel values survive NBT");
        var restored=new CokeOvenBlockEntity(TEST,f.getBlockState());restored.load(f.saveWithoutMetadata());check(restored.hasEfficiency()&&restored.getDataAccess().get(2)==70000,"Module and fuel survive save/reload");
        saved.remove("Modules");saved.remove("UnifiedSideConfig");saved.remove("PortFacing");restored.load(saved);
        check(!restored.hasEfficiency()&&restored.sideMode(restored.facing().getClockWise())==SideMode.INPUT,"Old saves restore original feed side");
        menu.setTab(202);check(!menu.getSlot(0).isActive()&&!menu.getSlot(2).mayPickup(p)&&!menu.getSlot(39).isActive(),"Hidden slots cannot be operated under side panel");
        check(!menu.clickMenuButton(p,100+RelativeSide.FRONT.ordinal()),"Menu refuses front port configuration");
    }
    static void ports(ServerLevel l){
        for(Direction facing:List.of(Direction.NORTH,Direction.EAST,Direction.SOUTH,Direction.WEST)){
            var f=place(l,TEST);l.setBlockAndUpdate(TEST,f.getBlockState().setValue(CokeOvenBlock.FACING,facing));tick(l,f,1);
            for(RelativeSide s:RelativeSide.values())for(SideMode m:List.of(SideMode.DISABLED,SideMode.INPUT,SideMode.OUTPUT)){
                mode(f,s,m);Direction d=s.resolve(facing);var cap=f.getCapability(ForgeCapabilities.ITEM_HANDLER,d).orElse(null);
                check((cap!=null)==(s!=RelativeSide.FRONT&&m!=SideMode.DISABLED),"Port presence "+facing+"/"+s+"/"+m);
                check(!f.getCapability(ForgeCapabilities.ENERGY,d).isPresent(),"No FE "+facing+"/"+s+"/"+m);
                if(cap==null)continue;
                var inv=f.getInventory();inv.setStackInSlot(0,new ItemStack(Items.COAL,2));inv.setStackInSlot(1,new ItemStack(Items.COAL,2));inv.setStackInSlot(2,new ItemStack(ModItems.COAL_COKE.get(),2));
                if(m==SideMode.INPUT){
                    check(cap.insertItem(0,new ItemStack(Items.COAL),true).isEmpty()&&cap.insertItem(1,new ItemStack(Items.COAL),true).isEmpty(),"Blue accepts feed and fuel "+s);
                    check(!cap.insertItem(0,new ItemStack(Items.CHARCOAL),true).isEmpty()&&!cap.insertItem(2,new ItemStack(ModItems.COAL_COKE.get()),true).isEmpty()&&cap.extractItem(0,1,true).isEmpty(),"Blue blocks invalid input and extraction "+s);
                }else{
                    check(cap.extractItem(2,1,true).is(ModItems.COAL_COKE.get())&&cap.extractItem(0,1,true).isEmpty()&&cap.extractItem(1,1,true).isEmpty(),"Orange protects coal and fuel "+s);
                    inv.setStackInSlot(1,new ItemStack(Items.BUCKET));check(cap.extractItem(1,1,true).is(Items.BUCKET),"Orange extracts empty bucket "+s);
                    mode(f,s,SideMode.DISABLED);check(cap.extractItem(2,1,false).isEmpty(),"Cached handler obeys OFF "+s);
                }
            }
            check(!f.getCapability(ForgeCapabilities.ITEM_HANDLER,null).isPresent(),"Unsided access denied "+facing);
        }
        var f=place(l,TEST);mode(f,RelativeSide.BACK,SideMode.OUTPUT);l.setBlockAndUpdate(TEST.south(),Blocks.CHEST.defaultBlockState());
        f.getInventory().setStackInSlot(2,new ItemStack(ModItems.COAL_COKE.get(),3));
        long time=l.getGameTime();l.getServer().getWorldData().overworldData().setGameTime(time-time%5);tick(l,f,1);
        var chest=(net.minecraft.world.level.block.entity.ChestBlockEntity)l.getBlockEntity(TEST.south());check(chest.countItem(ModItems.COAL_COKE.get())==3,"Enabled output exports to neighboring inventory");
        mode(f,RelativeSide.BACK,SideMode.DISABLED);f.getInventory().setStackInSlot(2,new ItemStack(ModItems.COAL_COKE.get(),2));tick(l,f,1);
        check(chest.countItem(ModItems.COAL_COKE.get())==3,"Disabled output stops automatic export");l.getServer().getWorldData().overworldData().setGameTime(time);l.removeBlock(TEST.south(),false);
    }
    static void dismantle(ServerLevel l,ServerPlayer p){
        p.setGameMode(GameType.SURVIVAL);p.setShiftKeyDown(false);
        for(Item tool:new Item[]{Items.STONE_PICKAXE,Items.WOODEN_PICKAXE}){
            var f=place(l,TEST);clear(l);f.getInventory().setStackInSlot(0,new ItemStack(Items.COAL,3));f.getInventory().setStackInSlot(1,new ItemStack(Items.CHARCOAL,2));f.getModules().setStackInSlot(0,new ItemStack(MachineModuleItems.EFFICIENCY.get()));
            p.setItemInHand(InteractionHand.MAIN_HAND,new ItemStack(tool));p.gameMode.destroyBlock(TEST);
            check(count(l,ModBlocks.COKE_OVEN.get().asItem())==(tool==Items.STONE_PICKAXE?1:0),"Survival harvest tier "+tool);
            check(count(l,Items.COAL)==3&&count(l,Items.CHARCOAL)==2&&count(l,MachineModuleItems.EFFICIENCY.get())==1,"Contents and module drop exactly once "+tool);
        }
        var f=place(l,TEST);clear(l);f.getInventory().setStackInSlot(0,new ItemStack(Items.COAL,7));f.getInventory().setStackInSlot(1,new ItemStack(Items.CHARCOAL,3));f.getModules().setStackInSlot(0,new ItemStack(MachineModuleItems.EFFICIENCY.get()));tick(l,f,80);
        var wrench=ForgeRegistries.ITEMS.getValue(new ResourceLocation("domesurvival","machine_wrench"));p.getInventory().clearContent();p.setItemInHand(InteractionHand.MAIN_HAND,new ItemStack(wrench));
        var hit=new BlockHitResult(Vec3.atCenterOf(TEST),Direction.NORTH,TEST,false);mode(f,RelativeSide.TOP,SideMode.INPUT);mode(f,RelativeSide.LEFT,SideMode.OUTPUT);
        p.gameMode.useItemOn(p,l,p.getMainHandItem(),InteractionHand.MAIN_HAND,hit);
        check(f.facing()==Direction.EAST&&f.sideMode(RelativeSide.LEFT.resolve(f.facing()))==SideMode.OUTPUT,"Wrench rotates configured sides");
        var expected=f.saveWithoutMetadata();p.setShiftKeyDown(true);p.gameMode.useItemOn(p,l,p.getMainHandItem(),InteractionHand.MAIN_HAND,hit);
        check(l.isEmptyBlock(TEST)&&drops(l).size()==1&&p.getInventory().countItem(ModBlocks.COKE_OVEN.get().asItem())==0,"Wrench creates one world drop without inserting into inventory");
        var entity=drops(l).get(0);var stack=entity.getItem().copy();var saved=stack.getTag().getCompound("BlockEntityTag");
        check(saved.getInt("BurnTime")==expected.getInt("BurnTime")&&saved.getInt("Progress")==80&&saved.getCompound("Modules").equals(expected.getCompound("Modules")),"Portable oven retains fuel progress and upgrade");
        entity.playerTouch(p);check(entity.isAlive(),"World drop uses normal pickup delay");clear(l);
        p.setShiftKeyDown(false);p.setYRot(180);p.setItemInHand(InteractionHand.MAIN_HAND,stack);
        p.gameMode.useItemOn(p,l,p.getMainHandItem(),InteractionHand.MAIN_HAND,new BlockHitResult(Vec3.atCenterOf(TEST.below()).add(0,.5,0),Direction.UP,TEST.below(),false));
        f=(CokeOvenBlockEntity)l.getBlockEntity(TEST);check(f!=null&&f.hasEfficiency()&&f.getInventory().getStackInSlot(0).getCount()==7&&f.getDataAccess().get(0)==80,"Actual BlockItem placement restores saved machine");
        check(f.sideMode(Direction.UP)==SideMode.INPUT&&f.sideMode(RelativeSide.LEFT.resolve(f.facing()))==SideMode.OUTPUT&&!f.getCapability(ForgeCapabilities.ITEM_HANDLER,f.facing()).isPresent(),"Port configuration follows newly placed orientation");
        p.setGameMode(GameType.ADVENTURE);p.setShiftKeyDown(true);p.setItemInHand(InteractionHand.MAIN_HAND,new ItemStack(wrench));p.gameMode.useItemOn(p,l,p.getMainHandItem(),InteractionHand.MAIN_HAND,hit);check(l.getBlockEntity(TEST)==f,"Adventure mode blocks dismantle");
        p.setShiftKeyDown(false);p.setGameMode(GameType.CREATIVE);p.getInventory().clearContent();
    }
    @SubscribeEvent public static void server(TickEvent.ServerTickEvent e){
        if(!ENABLED||e.phase!=TickEvent.Phase.END||fixture)return;var mc=Minecraft.getInstance();var server=mc.getSingleplayerServer();if(server==null||server.getPlayerList().getPlayers().isEmpty())return;
        if(!mc.gameDirectory.toPath().toAbsolutePath().normalize().endsWith("coke-oven-review"))throw new IllegalStateException("Isolated directory required");
        fixture=true;try{
            var l=server.overworld();var p=server.getPlayerList().getPlayers().get(0);l.setDayTime(6000);l.getGameRules().getRule(GameRules.RULE_DAYLIGHT).set(false,server);l.getGameRules().getRule(GameRules.RULE_DOMOBSPAWNING).set(false,server);
            for(int x=-4;x<16;x++)for(int z=-4;z<16;z++)l.setBlockAndUpdate(new BlockPos(x,99,z),Blocks.SMOOTH_STONE.defaultBlockState());
            process(l,p);ports(l);dismantle(l,p);
            var f=place(l,DISPLAY);f.getInventory().setStackInSlot(0,new ItemStack(Items.COAL,64));f.getInventory().setStackInSlot(1,new ItemStack(Items.COAL,64));
            l.setBlockAndUpdate(DISPLAY.east(),ItemPipeRegistry.STEEL_PIPE.get().defaultBlockState());
            l.setBlockAndUpdate(DISPLAY.west(),ItemPipeRegistry.STEEL_PIPE.get().defaultBlockState());
            var saved=f.saveWithoutMetadata();saved.putInt("BurnTime",70000);saved.putInt("BurnTimeMax",80000);saved.putInt("Progress",500);f.load(saved);
            p.teleportTo(l,1.5,102,-4,0,25);p.getAbilities().flying=true;p.onUpdateAbilities();p.getInventory().setItem(9,new ItemStack(MachineModuleItems.EFFICIENCY.get()));ready=true;
        }catch(Throwable ex){check(false,"Server exception "+ex);ex.printStackTrace();ready=true;}
    }
    static void add(int delay,Runnable action){steps.add(new Step(delay,action));}
    static void shot(String name){try(NativeImage img=Screenshot.takeScreenshot(Minecraft.getInstance().getMainRenderTarget())){Files.createDirectories(OUT);img.writeToFile(OUT.resolve(name+".png"));log("Screenshot "+name);}catch(Exception ex){check(false,"Screenshot "+ex);}}
    static CokeOvenScreen screen(){return (CokeOvenScreen)Minecraft.getInstance().screen;}
    static void tab(int id){var mc=Minecraft.getInstance();screen().getMenu().setTab(id);mc.gameMode.handleInventoryButtonClick(screen().getMenu().containerId,id);}
    static void otherGui(Block block){var mc=Minecraft.getInstance();mc.player.closeContainer();mc.setScreen(null);
        mc.getSingleplayerServer().execute(()->{var p=mc.getSingleplayerServer().getPlayerList().getPlayers().get(0);var l=p.serverLevel();var pos=DISPLAY.south(2);
            l.setBlockAndUpdate(pos,block.defaultBlockState());NetworkHooks.openScreen(p,(net.minecraft.world.MenuProvider)l.getBlockEntity(pos),pos);});
    }
    static void plan(){var mc=Minecraft.getInstance();mc.setScreen(null);mc.options.hideGui=true;mc.getTutorial().setStep(net.minecraft.client.tutorial.TutorialSteps.NONE);
        check(ItemPipeBlock.hasObjectConnector(mc.level,DISPLAY.east(),Direction.WEST),"Client pipe recognizes input");
        check(ItemPipeBlock.hasObjectConnector(mc.level,DISPLAY.west(),Direction.EAST),"Client pipe recognizes output");
        check(!ItemPipeBlock.hasObjectConnector(mc.level,DISPLAY.north(),Direction.SOUTH),"Client pipe rejects front");
        add(20,()->shot("01_existing_model"));
        add(10,()->{mc.options.hideGui=false;mc.getSingleplayerServer().execute(()->{var p=mc.getSingleplayerServer().getPlayerList().getPlayers().get(0);NetworkHooks.openScreen(p,(CokeOvenBlockEntity)p.serverLevel().getBlockEntity(DISPLAY),DISPLAY);});});
        add(25,()->{check(mc.screen instanceof CokeOvenScreen,"Networked coke GUI opens");check(screen().getMenu().burnTotal()==80000&&screen().getMenu().burnRemaining()>65535,"32-bit fuel value synchronized");shot("02_main_gui");mc.options.guiScale().set(3);mc.resizeDisplay();});
        add(20,()->{shot("03_main_scale3");mc.options.guiScale().set(2);mc.resizeDisplay();tab(202);});
        add(20,()->{shot("04_sides");mc.gameMode.handleInventoryButtonClick(screen().getMenu().containerId,100+RelativeSide.TOP.ordinal());});
        add(20,()->{check(screen().getMenu().getSideMode(RelativeSide.TOP)==SideMode.INPUT,"Networked side button updates menu");check(((CokeOvenBlockEntity)mc.level.getBlockEntity(DISPLAY)).sideMode(Direction.UP)==SideMode.INPUT,"Client receives logical side update");tab(200);});
        add(20,()->{shot("05_modules_empty");mc.gameMode.handleInventoryMouseClick(screen().getMenu().containerId,3,0,ClickType.QUICK_MOVE,mc.player);});
        add(20,()->{check(screen().getMenu().efficiency()&&screen().getMenu().getSlot(39).hasItem(),"Networked Shift-click installs efficiency while working");shot("06_modules_installed");mc.options.guiScale().set(3);mc.resizeDisplay();});
        add(20,()->{shot("07_module_scale3");mc.options.guiScale().set(2);mc.resizeDisplay();mc.player.closeContainer();
            var runtime=JeiCapture.runtime;check(runtime!=null,"JEI runtime available");var manager=runtime.getRecipeManager();
            check(manager.createRecipeCategoryLookup().get().filter(c->c.getRecipeType().getUid().equals(DomeSurvivalJeiPlugin.COKE_OVEN.getUid())).count()==1,"Exactly one coke JEI category");
            check(manager.createRecipeLookup(DomeSurvivalJeiPlugin.COKE_OVEN).get().count()==1,"Exactly one coke recipe");
            runtime.getRecipesGui().showTypes(List.of(DomeSurvivalJeiPlugin.COKE_OVEN));});
        add(30,()->shot("08_jei"));add(100,()->shot("09_jei_progress"));
        add(10,()->otherGui(ModBlocks.COAL_GENERATOR.get()));add(20,()->{check(mc.screen!=null&&mc.screen.getClass().getSimpleName().equals("CoalGeneratorScreen"),"Generator GUI without footer opens");shot("10_generator_without_code");});
        add(10,()->otherGui(ForgeRegistries.BLOCKS.getValue(new ResourceLocation("domesurvival","forming_press"))));add(20,()->{check(mc.screen!=null&&mc.screen.getClass().getSimpleName().equals("FormingPressScreen"),"Press GUI without footer opens");shot("11_press_without_code");});
        add(10,()->otherGui(ModBlocks.COPPER_FURNACE.get()));add(20,()->{check(mc.screen!=null&&mc.screen.getClass().getSimpleName().equals("CopperFurnaceScreen"),"Copper GUI without footer opens");shot("12_copper_without_code");});
        add(10,()->{mc.setScreen(null);log("RESULT "+(failures==0?"PASS":"FAIL")+" failures="+failures);mc.stop();});
    }
    @SubscribeEvent public static void mouse(TickEvent.RenderTickEvent e){
        if(!ENABLED||e.phase!=TickEvent.Phase.START||!planned)return;
        try{var mc=Minecraft.getInstance();
            net.minecraftforge.fml.util.ObfuscationReflectionHelper.findField(MouseHandler.class,"f_91507_").setDouble(mc.mouseHandler,0);
            net.minecraftforge.fml.util.ObfuscationReflectionHelper.findField(MouseHandler.class,"f_91508_").setDouble(mc.mouseHandler,0);
        }catch(ReflectiveOperationException ex){throw new RuntimeException(ex);}
    }
    @SubscribeEvent public static void client(TickEvent.ClientTickEvent e){
        if(!ENABLED||e.phase!=TickEvent.Phase.END)return;var mc=Minecraft.getInstance();
        if(!started&&mc.screen!=null&&mc.screen.getClass().getSimpleName().equals("AccessibilityOnboardingScreen")&&mc.getOverlay()==null){mc.setScreen(new TitleScreen());return;}
        if(!started&&mc.screen instanceof TitleScreen&&mc.getOverlay()==null){started=true;mc.options.pauseOnLostFocus=false;mc.options.fov().set(50);mc.options.guiScale().set(2);mc.options.renderDistance().set(6);mc.options.enableVsync().set(false);mc.options.framerateLimit().set(120);mc.getLanguageManager().setSelected("ru_ru");mc.options.languageCode="ru_ru";mc.reloadResourcePacks();mc.createWorldOpenFlows().createFreshLevel("coke_review_"+System.currentTimeMillis(),new LevelSettings("Coke review",GameType.CREATIVE,false,Difficulty.PEACEFUL,true,new GameRules(),WorldDataConfiguration.DEFAULT),new WorldOptions(63L,false,false),a->a.registryOrThrow(Registries.WORLD_PRESET).getHolderOrThrow(WorldPresets.FLAT).value().createWorldDimensions());return;}
        if(!ready||mc.player==null||mc.level==null||mc.getOverlay()!=null)return;ticks++;
        if(!planned&&ticks>100){planned=true;ticks=0;plan();}else if(planned&&step<steps.size()&&ticks>=steps.get(step).delay()){ticks=0;try{steps.get(step++).action().run();}catch(Throwable ex){check(false,"Client exception "+ex);ex.printStackTrace();log("RESULT FAIL");mc.stop();}}
    }
}
