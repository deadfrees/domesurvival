package com.wasted.domesurvival.shaftprobe;

import com.mojang.blaze3d.platform.NativeImage;
import com.wasted.domesurvival.forge.block.ModBlocks;
import com.wasted.domesurvival.forge.item.ModItems;
import com.wasted.domesurvival.forge.itempipe.ItemPipeBlock;
import com.wasted.domesurvival.forge.itempipe.ItemPipeRegistry;
import com.wasted.domesurvival.forge.machine.shaft.*;
import com.wasted.domesurvival.forge.machine.module.*;
import com.wasted.domesurvival.forge.machine.side.*;
import com.wasted.domesurvival.forge.client.screen.ShaftFurnaceScreen;
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
public final class ShaftFurnaceProbe {
    static final boolean ENABLED=Boolean.getBoolean("dome.shaftFurnaceReview");
    static final Path OUT=Path.of("../../dev/shaft_furnace_v2/runtime").toAbsolutePath().normalize();
    static final BlockPos DISPLAY=new BlockPos(0,100,0),TEST=new BlockPos(10,100,10);
    static boolean started,fixture,planned;static volatile boolean ready;static int ticks,step,failures;
    record Step(int delay,Runnable action){}static final List<Step> steps=new ArrayList<>();
    static synchronized void log(String s){System.out.println("[SHAFT_REVIEW] "+s);try{Files.createDirectories(OUT);Files.writeString(OUT.resolve("checks.txt"),s+"\n",StandardOpenOption.CREATE,StandardOpenOption.APPEND);}catch(Exception ex){throw new RuntimeException(ex);}}
    static void check(boolean ok,String s){if(!ok)failures++;log((ok?"PASS ":"FAIL ")+s);}
    static ShaftFurnaceBlockEntity place(ServerLevel l,BlockPos p){l.setBlockAndUpdate(p,Blocks.AIR.defaultBlockState());l.setBlockAndUpdate(p,ModBlocks.SHAFT_FURNACE.get().defaultBlockState());return (ShaftFurnaceBlockEntity)l.getBlockEntity(p);}
    static void tick(ServerLevel l,ShaftFurnaceBlockEntity f,int n){for(int i=0;i<n;i++)ShaftFurnaceBlockEntity.serverTick(l,f.getBlockPos(),f.getBlockState(),f);}
    static void mode(ShaftFurnaceBlockEntity f,RelativeSide s,SideMode m){for(int i=0;i<3&&f.sideMode(s.resolve(f.facing()))!=m;i++)f.cycleSideMode(s);}
    static List<ItemEntity> drops(ServerLevel l){return l.getEntitiesOfClass(ItemEntity.class,new AABB(TEST).inflate(2));}
    static void clear(ServerLevel l){drops(l).forEach(ItemEntity::discard);}
    static int count(ServerLevel l,Item i){return drops(l).stream().filter(e->e.getItem().is(i)).mapToInt(e->e.getItem().getCount()).sum();}
    static void process(ServerLevel l,ServerPlayer p){
        var f=place(l,TEST);var inv=f.getInventory();var coke=ModItems.COAL_COKE.get();
        inv.setStackInSlot(0,new ItemStack(Items.IRON_INGOT,16));inv.setStackInSlot(1,new ItemStack(coke,3));
        check(f.fuelDuration(new ItemStack(coke))==3200,"Base coke burn is unchanged");
        check(f.fuelDuration(new ItemStack(Items.COAL))==0&&!ShaftFurnaceBlockEntity.isValidCoke(new ItemStack(Items.CHARCOAL)),"Only coke fuels the furnace");
        tick(l,f,2999);check(inv.getStackInSlot(2).isEmpty()&&inv.getStackInSlot(3).isEmpty()&&f.getDataAccess().get(0)==2999,"No outputs before 150 second cycle");
        tick(l,f,1);check(inv.getStackInSlot(2).is(ModItems.STEEL_INGOT.get())&&inv.getStackInSlot(3).is(ModItems.SLAG.get())&&inv.getStackInSlot(0).getCount()==15,"One iron produces exactly one steel and one slag");
        var menu=new ShaftFurnaceMenu(7,p.getInventory(),f);menu.setTab(200);p.getInventory().setItem(9,new ItemStack(MachineModuleItems.EFFICIENCY.get()));
        int burn=menu.burnRemaining();check(!menu.quickMoveStack(p,4).isEmpty()&&f.hasEfficiency(),"Install efficiency with Shift-click while burning");
        check(menu.burnRemaining()==burn&&menu.burnTotal()==3200,"Installing preserves current fuel snapshot");
        tick(l,f,burn);check(menu.burnTotal()==3680,"Next coke burns 15 percent longer");
        check(menu.progressMax()==3000,"Module does not accelerate smelting");
        check(!menu.quickMoveStack(p,40).isEmpty()&&!f.hasEfficiency()&&menu.burnTotal()==3680,"Hot removal preserves already burning fuel");
        check(!f.getModules().insertItem(0,new ItemStack(MachineModuleItems.OVERDRIVE.get()),false).isEmpty()&&f.getModules().getStackInSlot(0).isEmpty(),"Empty module socket rejects overdrive");
        check(!f.getModules().insertItem(0,new ItemStack(MachineModuleItems.BUFFER.get()),false).isEmpty(),"Energy buffer module rejected");
        for(int output:new int[]{2,3}){
            inv.setStackInSlot(output,new ItemStack(output==2?ModItems.STEEL_INGOT.get():ModItems.SLAG.get(),64));
            int progress=menu.progress(),raw=inv.getStackInSlot(0).getCount();tick(l,f,10);
            check(menu.progress()==progress&&inv.getStackInSlot(0).getCount()==raw,"Full output pauses safely slot "+output);
            inv.setStackInSlot(output,ItemStack.EMPTY);tick(l,f,1);check(menu.progress()==progress+1,"Clearing output resumes slot "+output);
        }
        inv.setStackInSlot(3,new ItemStack(Items.COBBLESTONE));int progress=menu.progress();tick(l,f,10);check(menu.progress()==progress,"Foreign item in slag output blocks processing");inv.setStackInSlot(3,ItemStack.EMPTY);
        var saved=f.saveWithoutMetadata();saved.putInt("BurnTime",0);saved.putInt("BurnTimeMax",0);saved.putInt("Progress",13);f.load(saved);
        inv.setStackInSlot(1,ItemStack.EMPTY);tick(l,f,10);check(menu.progress()==13,"No coke pauses unfinished cycle");
        inv.setStackInSlot(1,new ItemStack(coke));tick(l,f,1);check(menu.progress()==14,"Adding coke resumes unfinished cycle");
        inv.setStackInSlot(0,ItemStack.EMPTY);tick(l,f,1);check(menu.progress()==0,"Removing iron resets progress");
        inv.setStackInSlot(0,new ItemStack(Items.IRON_INGOT));
        f.getModules().setStackInSlot(0,new ItemStack(MachineModuleItems.EFFICIENCY.get()));saved=f.saveWithoutMetadata();saved.putInt("BurnTime",70000);saved.putInt("BurnTimeMax",80000);f.load(saved);
        check(menu.burnRemaining()==70000&&menu.burnTotal()==80000,"Full-width fuel survives NBT");
        var restored=new ShaftFurnaceBlockEntity(TEST,f.getBlockState());restored.load(f.saveWithoutMetadata());check(restored.hasEfficiency()&&restored.getDataAccess().get(2)==70000,"Module and fuel survive save/reload");
        saved.remove("Modules");saved.remove("UnifiedSideConfig");saved.remove("PortFacing");restored.load(saved);check(!restored.hasEfficiency()&&restored.sideMode(restored.facing().getClockWise())==SideMode.INPUT,"Legacy save retains original feed side");
        menu.setTab(202);check(!menu.getSlot(0).isActive()&&!menu.getSlot(2).mayPickup(p)&&!menu.getSlot(3).mayPickup(p)&&!menu.getSlot(40).isActive(),"Hidden machine and module slots are protected");
        p.teleportTo(l,10.5,101,8.5,0,0);check(!menu.clickMenuButton(p,100+RelativeSide.FRONT.ordinal()),"Menu refuses front port");
        menu.setTab(201);p.getInventory().setItem(9,new ItemStack(Items.IRON_INGOT,2));p.getInventory().setItem(10,new ItemStack(coke,2));
        check(!menu.quickMoveStack(p,4).isEmpty()&&!menu.quickMoveStack(p,5).isEmpty(),"Shift-click routes iron and coke to their own slots");
    }
    static void ports(ServerLevel l){
        for(Direction facing:List.of(Direction.NORTH,Direction.EAST,Direction.SOUTH,Direction.WEST)){
            var f=place(l,TEST);l.setBlockAndUpdate(TEST,f.getBlockState().setValue(ShaftFurnaceBlock.FACING,facing));tick(l,f,1);
            for(RelativeSide s:RelativeSide.values())for(SideMode m:List.of(SideMode.DISABLED,SideMode.INPUT,SideMode.OUTPUT)){
                mode(f,s,m);Direction d=s.resolve(facing);var cap=f.getCapability(ForgeCapabilities.ITEM_HANDLER,d).orElse(null);
                check((cap!=null)==(s!=RelativeSide.FRONT&&m!=SideMode.DISABLED),"Port presence "+facing+"/"+s+"/"+m);
                check(!f.getCapability(ForgeCapabilities.ENERGY,d).isPresent(),"No FE "+facing+"/"+s+"/"+m);
                if(cap==null)continue;
                var inv=f.getInventory();inv.setStackInSlot(0,new ItemStack(Items.IRON_INGOT,2));inv.setStackInSlot(1,new ItemStack(ModItems.COAL_COKE.get(),2));inv.setStackInSlot(2,new ItemStack(ModItems.STEEL_INGOT.get(),2));
                if(m==SideMode.INPUT){
                    check(cap.insertItem(0,new ItemStack(Items.IRON_INGOT),true).isEmpty()&&cap.insertItem(1,new ItemStack(ModItems.COAL_COKE.get()),true).isEmpty(),"Blue accepts feed and fuel "+s);
                    check(!cap.insertItem(0,new ItemStack(Items.CHARCOAL),true).isEmpty()&&!cap.insertItem(2,new ItemStack(ModItems.STEEL_INGOT.get()),true).isEmpty()&&cap.extractItem(0,1,true).isEmpty(),"Blue blocks invalid input and extraction "+s);
                }else{
                    check(cap.extractItem(2,1,true).is(ModItems.STEEL_INGOT.get())&&cap.extractItem(0,1,true).isEmpty()&&cap.extractItem(1,1,true).isEmpty(),"Orange protects iron and coke "+s);
                    inv.setStackInSlot(3,new ItemStack(ModItems.SLAG.get()));check(cap.extractItem(3,1,true).is(ModItems.SLAG.get()),"Orange extracts slag "+s);
                    mode(f,s,SideMode.DISABLED);check(cap.extractItem(2,1,false).isEmpty(),"Cached handler obeys OFF "+s);
                }
            }
            check(!f.getCapability(ForgeCapabilities.ITEM_HANDLER,null).isPresent(),"Unsided access denied "+facing);
        }
        var f=place(l,TEST);mode(f,RelativeSide.BACK,SideMode.OUTPUT);l.setBlockAndUpdate(TEST.south(),Blocks.CHEST.defaultBlockState());
        f.getInventory().setStackInSlot(2,new ItemStack(ModItems.STEEL_INGOT.get(),3));f.getInventory().setStackInSlot(3,new ItemStack(ModItems.SLAG.get(),2));
        long time=l.getGameTime();l.getServer().getWorldData().overworldData().setGameTime(time-time%5);tick(l,f,1);
        var chest=(net.minecraft.world.level.block.entity.ChestBlockEntity)l.getBlockEntity(TEST.south());check(chest.countItem(ModItems.STEEL_INGOT.get())==3&&chest.countItem(ModItems.SLAG.get())==2,"Enabled output exports to neighboring inventory");
        mode(f,RelativeSide.BACK,SideMode.DISABLED);f.getInventory().setStackInSlot(2,new ItemStack(ModItems.STEEL_INGOT.get(),2));tick(l,f,1);
        check(chest.countItem(ModItems.STEEL_INGOT.get())==3&&chest.countItem(ModItems.SLAG.get())==2,"Disabled output stops automatic export");l.getServer().getWorldData().overworldData().setGameTime(time);l.removeBlock(TEST.south(),false);
    }
    static void dismantle(ServerLevel l,ServerPlayer p){
        p.setGameMode(GameType.SURVIVAL);p.setShiftKeyDown(false);
        for(Item tool:new Item[]{Items.STONE_PICKAXE,Items.WOODEN_PICKAXE}){
            var f=place(l,TEST);clear(l);f.getInventory().setStackInSlot(0,new ItemStack(Items.IRON_INGOT,3));f.getInventory().setStackInSlot(1,new ItemStack(ModItems.COAL_COKE.get(),2));f.getModules().setStackInSlot(0,new ItemStack(MachineModuleItems.EFFICIENCY.get()));
            f.getInventory().setStackInSlot(2,new ItemStack(ModItems.STEEL_INGOT.get(),4));f.getInventory().setStackInSlot(3,new ItemStack(ModItems.SLAG.get(),5));
            p.setItemInHand(InteractionHand.MAIN_HAND,new ItemStack(tool));p.gameMode.destroyBlock(TEST);
            check(count(l,ModBlocks.SHAFT_FURNACE.get().asItem())==(tool==Items.STONE_PICKAXE?1:0),"Survival harvest tier "+tool);
            check(count(l,Items.IRON_INGOT)==3&&count(l,ModItems.COAL_COKE.get())==2&&count(l,MachineModuleItems.EFFICIENCY.get())==1,"Contents and module drop exactly once "+tool);
            check(count(l,ModItems.STEEL_INGOT.get())==4&&count(l,ModItems.SLAG.get())==5,"Both output slots drop exactly once "+tool);
        }
        var f=place(l,TEST);clear(l);f.getInventory().setStackInSlot(0,new ItemStack(Items.IRON_INGOT,7));f.getInventory().setStackInSlot(1,new ItemStack(ModItems.COAL_COKE.get(),3));f.getModules().setStackInSlot(0,new ItemStack(MachineModuleItems.EFFICIENCY.get()));tick(l,f,80);
        f.getInventory().setStackInSlot(2,new ItemStack(ModItems.STEEL_INGOT.get(),4));f.getInventory().setStackInSlot(3,new ItemStack(ModItems.SLAG.get(),5));
        var wrench=ForgeRegistries.ITEMS.getValue(new ResourceLocation("domesurvival","machine_wrench"));p.getInventory().clearContent();p.setItemInHand(InteractionHand.MAIN_HAND,new ItemStack(wrench));
        var hit=new BlockHitResult(Vec3.atCenterOf(TEST),Direction.NORTH,TEST,false);mode(f,RelativeSide.TOP,SideMode.INPUT);mode(f,RelativeSide.LEFT,SideMode.OUTPUT);
        p.gameMode.useItemOn(p,l,p.getMainHandItem(),InteractionHand.MAIN_HAND,hit);
        check(f.facing()==Direction.EAST&&f.sideMode(RelativeSide.LEFT.resolve(f.facing()))==SideMode.OUTPUT,"Wrench rotates configured sides");
        var expected=f.saveWithoutMetadata();p.setShiftKeyDown(true);p.gameMode.useItemOn(p,l,p.getMainHandItem(),InteractionHand.MAIN_HAND,hit);
        check(l.isEmptyBlock(TEST)&&drops(l).size()==1&&p.getInventory().countItem(ModBlocks.SHAFT_FURNACE.get().asItem())==0,"Wrench creates one world drop without inserting into inventory");
        var entity=drops(l).get(0);var stack=entity.getItem().copy();var saved=stack.getTag().getCompound("BlockEntityTag");
        check(saved.getInt("BurnTime")==expected.getInt("BurnTime")&&saved.getInt("Progress")==80&&saved.getCompound("Modules").equals(expected.getCompound("Modules")),"Portable oven retains fuel progress and upgrade");
        check(saved.getCompound("Inventory").equals(expected.getCompound("Inventory")),"Portable item retains all four inventory slots");
        entity.playerTouch(p);check(entity.isAlive(),"World drop uses normal pickup delay");clear(l);
        p.setShiftKeyDown(false);p.setYRot(180);p.setItemInHand(InteractionHand.MAIN_HAND,stack);
        p.gameMode.useItemOn(p,l,p.getMainHandItem(),InteractionHand.MAIN_HAND,new BlockHitResult(Vec3.atCenterOf(TEST.below()).add(0,.5,0),Direction.UP,TEST.below(),false));
        f=(ShaftFurnaceBlockEntity)l.getBlockEntity(TEST);check(f!=null&&f.hasEfficiency()&&f.getInventory().getStackInSlot(0).getCount()==7&&f.getDataAccess().get(0)==80,"Actual BlockItem placement restores saved machine");
        check(f.getInventory().getStackInSlot(2).getCount()==4&&f.getInventory().getStackInSlot(3).getCount()==5,"Placement restores steel and slag outputs");
        check(f.sideMode(Direction.UP)==SideMode.INPUT&&f.sideMode(RelativeSide.LEFT.resolve(f.facing()))==SideMode.OUTPUT&&!f.getCapability(ForgeCapabilities.ITEM_HANDLER,f.facing()).isPresent(),"Port configuration follows newly placed orientation");
        p.setGameMode(GameType.ADVENTURE);p.setShiftKeyDown(true);p.setItemInHand(InteractionHand.MAIN_HAND,new ItemStack(wrench));p.gameMode.useItemOn(p,l,p.getMainHandItem(),InteractionHand.MAIN_HAND,hit);check(l.getBlockEntity(TEST)==f,"Adventure mode blocks dismantle");
        p.setShiftKeyDown(false);p.setGameMode(GameType.CREATIVE);p.getInventory().clearContent();
    }
    @SubscribeEvent public static void server(TickEvent.ServerTickEvent e){
        if(!ENABLED||e.phase!=TickEvent.Phase.END||fixture)return;var mc=Minecraft.getInstance();var server=mc.getSingleplayerServer();if(server==null||server.getPlayerList().getPlayers().isEmpty())return;
        if(!mc.gameDirectory.toPath().toAbsolutePath().normalize().endsWith("shaft-furnace-review"))throw new IllegalStateException("Isolated directory required");
        fixture=true;try{
            var l=server.overworld();var p=server.getPlayerList().getPlayers().get(0);l.setDayTime(6000);l.getGameRules().getRule(GameRules.RULE_DAYLIGHT).set(false,server);l.getGameRules().getRule(GameRules.RULE_DOMOBSPAWNING).set(false,server);
            for(int x=-4;x<16;x++)for(int z=-4;z<16;z++)l.setBlockAndUpdate(new BlockPos(x,99,z),Blocks.SMOOTH_STONE.defaultBlockState());
            process(l,p);ports(l);dismantle(l,p);
            var f=place(l,DISPLAY);f.getInventory().setStackInSlot(0,new ItemStack(Items.IRON_INGOT,64));f.getInventory().setStackInSlot(1,new ItemStack(ModItems.COAL_COKE.get(),64));
            l.setBlockAndUpdate(DISPLAY.east(),ItemPipeRegistry.STEEL_PIPE.get().defaultBlockState());
            l.setBlockAndUpdate(DISPLAY.west(),ItemPipeRegistry.STEEL_PIPE.get().defaultBlockState());
            var saved=f.saveWithoutMetadata();saved.putInt("BurnTime",70000);saved.putInt("BurnTimeMax",80000);saved.putInt("Progress",500);f.load(saved);
            p.teleportTo(l,1.5,102,-4,0,25);p.getAbilities().flying=true;p.onUpdateAbilities();p.getInventory().setItem(9,new ItemStack(MachineModuleItems.EFFICIENCY.get()));ready=true;
        }catch(Throwable ex){check(false,"Server exception "+ex);ex.printStackTrace();ready=true;}
    }
    static void add(int delay,Runnable action){steps.add(new Step(delay,action));}
    static void shot(String name){try(NativeImage img=Screenshot.takeScreenshot(Minecraft.getInstance().getMainRenderTarget())){Files.createDirectories(OUT);img.writeToFile(OUT.resolve(name+".png"));log("Screenshot "+name);}catch(Exception ex){check(false,"Screenshot "+ex);}}
    static ShaftFurnaceScreen screen(){return (ShaftFurnaceScreen)Minecraft.getInstance().screen;}
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
        add(10,()->{mc.options.hideGui=false;mc.getSingleplayerServer().execute(()->{var p=mc.getSingleplayerServer().getPlayerList().getPlayers().get(0);NetworkHooks.openScreen(p,(ShaftFurnaceBlockEntity)p.serverLevel().getBlockEntity(DISPLAY),DISPLAY);});});
        add(25,()->{check(mc.screen instanceof ShaftFurnaceScreen,"Networked shaft GUI opens");check(screen().getMenu().burnTotal()==80000&&screen().getMenu().burnRemaining()>65535,"32-bit fuel value synchronized");shot("02_main_gui");mc.options.guiScale().set(3);mc.resizeDisplay();});
        add(20,()->{shot("03_main_scale3");mc.options.guiScale().set(2);mc.resizeDisplay();tab(202);});
        add(20,()->{shot("04_sides");mc.gameMode.handleInventoryButtonClick(screen().getMenu().containerId,100+RelativeSide.TOP.ordinal());});
        add(20,()->{check(screen().getMenu().getSideMode(RelativeSide.TOP)==SideMode.INPUT,"Networked side button updates menu");check(((ShaftFurnaceBlockEntity)mc.level.getBlockEntity(DISPLAY)).sideMode(Direction.UP)==SideMode.INPUT,"Client receives logical side update");tab(200);});
        add(20,()->{shot("05_modules_empty");mc.gameMode.handleInventoryMouseClick(screen().getMenu().containerId,4,0,ClickType.QUICK_MOVE,mc.player);});
        add(20,()->{check(screen().getMenu().efficiency()&&screen().getMenu().getSlot(40).hasItem(),"Networked Shift-click installs efficiency while working");shot("06_modules_installed");mc.options.guiScale().set(3);mc.resizeDisplay();});
        add(20,()->{shot("07_module_scale3");mc.options.guiScale().set(2);mc.resizeDisplay();mc.player.closeContainer();
            var runtime=JeiCapture.runtime;check(runtime!=null,"JEI runtime available");var manager=runtime.getRecipeManager();
            check(manager.createRecipeCategoryLookup().get().filter(c->c.getRecipeType().getUid().equals(DomeSurvivalJeiPlugin.SHAFT_FURNACE.getUid())).count()==1,"Exactly one shaft JEI category");
            check(manager.createRecipeLookup(DomeSurvivalJeiPlugin.SHAFT_FURNACE).get().count()==1,"Exactly one shaft recipe");
            runtime.getRecipesGui().showTypes(List.of(DomeSurvivalJeiPlugin.SHAFT_FURNACE));});
        add(30,()->shot("08_jei"));add(100,()->shot("09_jei_progress"));
        add(10,()->otherGui(ModBlocks.COKE_OVEN.get()));
        add(25,()->{shot("10_coke_main");mc.options.guiScale().set(3);mc.resizeDisplay();});
        add(20,()->shot("11_coke_main_scale3"));
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
        if(!started&&mc.screen instanceof TitleScreen&&mc.getOverlay()==null){started=true;mc.options.pauseOnLostFocus=false;mc.options.fov().set(50);mc.options.guiScale().set(2);mc.options.renderDistance().set(6);mc.options.enableVsync().set(false);mc.options.framerateLimit().set(120);mc.getLanguageManager().setSelected("ru_ru");mc.options.languageCode="ru_ru";mc.reloadResourcePacks();mc.createWorldOpenFlows().createFreshLevel("shaft_review_"+System.currentTimeMillis(),new LevelSettings("Shaft review",GameType.CREATIVE,false,Difficulty.PEACEFUL,true,new GameRules(),WorldDataConfiguration.DEFAULT),new WorldOptions(63L,false,false),a->a.registryOrThrow(Registries.WORLD_PRESET).getHolderOrThrow(WorldPresets.FLAT).value().createWorldDimensions());return;}
        if(!ready||mc.player==null||mc.level==null||mc.getOverlay()!=null)return;ticks++;
        if(!planned&&ticks>100){planned=true;ticks=0;plan();}else if(planned&&step<steps.size()&&ticks>=steps.get(step).delay()){ticks=0;try{steps.get(step++).action().run();}catch(Throwable ex){check(false,"Client exception "+ex);ex.printStackTrace();log("RESULT FAIL");mc.stop();}}
    }
}
