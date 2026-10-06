package com.wasted.domesurvival.filterprobe;

import com.mojang.blaze3d.platform.NativeImage;
import com.wasted.domesurvival.forge.block.ModBlocks;
import com.wasted.domesurvival.forge.item.ModItems;
import com.wasted.domesurvival.forge.item.OxygenTankItem;
import com.wasted.domesurvival.forge.oxygen.room.*;
import com.wasted.domesurvival.forge.itempipe.ItemPipeBlock;
import com.wasted.domesurvival.forge.itempipe.ItemPipeRegistry;
import com.wasted.domesurvival.forge.machine.filter.*;
import com.wasted.domesurvival.forge.machine.module.*;
import com.wasted.domesurvival.forge.machine.side.*;
import com.wasted.domesurvival.forge.machine.filter.FilterRegenerationScreen;
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
import com.wasted.domesurvival.forge.capability.ModCapabilities;
import com.wasted.domesurvival.forge.transport.fluid.*;
import net.minecraftforge.fluids.*;
import net.minecraftforge.fluids.capability.*;
import net.minecraft.world.level.material.Fluids;
import com.wasted.domesurvival.forge.fluid.ModFluids;
import com.wasted.domesurvival.forge.item.WaterFilterItem;
import java.util.*;

@Mod.EventBusSubscriber(modid="domesurvival",value=Dist.CLIENT)
public final class FilterRegenerationProbe {
    static final boolean ENABLED=Boolean.getBoolean("dome.filterRegenerationReview");
    static final Path OUT=Path.of("../../dev/filter_regeneration_v2/runtime").toAbsolutePath().normalize();
    static final BlockPos DISPLAY=new BlockPos(0,100,0),TEST=new BlockPos(10,100,10);
    static boolean started,fixture,planned;static volatile boolean ready;static int ticks,step,failures;
    static double hoverX,hoverY;
    static void hover(int x,int y){var w=Minecraft.getInstance().getWindow();hoverX=((w.getGuiScaledWidth()-220)/2+x)*w.getGuiScale();hoverY=((w.getGuiScaledHeight()-266)/2+y)*w.getGuiScale();}
    record Step(int delay,Runnable action){}static final List<Step> steps=new ArrayList<>();
    static synchronized void log(String s){System.out.println("[FILTER_REVIEW] "+s);try{Files.createDirectories(OUT);Files.writeString(OUT.resolve("checks.txt"),s+"\n",StandardOpenOption.CREATE,StandardOpenOption.APPEND);}catch(Exception ex){throw new RuntimeException(ex);}}
    static void check(boolean ok,String s){if(!ok)failures++;log((ok?"PASS ":"FAIL ")+s);}
    static FilterRegenerationBlockEntity place(ServerLevel l,BlockPos p){l.setBlockAndUpdate(p,Blocks.AIR.defaultBlockState());l.setBlockAndUpdate(p,FilterRegenerationRegistry.FILTER_REGENERATION_STATION.get().defaultBlockState());return (FilterRegenerationBlockEntity)l.getBlockEntity(p);}
    static void tick(ServerLevel l,FilterRegenerationBlockEntity f,int n){for(int i=0;i<n;i++)FilterRegenerationBlockEntity.serverTick(l,f.getBlockPos(),f.getBlockState(),f);}
    static void mode(FilterRegenerationBlockEntity f,RelativeSide s,SideMode m){for(int i=0;i<3&&f.sideMode(s.resolve(f.getMachineFacing()))!=m;i++)f.cycleSideMode(s);}
    static List<ItemEntity> drops(ServerLevel l){return l.getEntitiesOfClass(ItemEntity.class,new AABB(TEST).inflate(2));}
    static void clear(ServerLevel l){drops(l).forEach(ItemEntity::discard);}
    static int count(ServerLevel l,Item i){return drops(l).stream().filter(e->e.getItem().is(i)).mapToInt(e->e.getItem().getCount()).sum();}
    static void energy(FilterRegenerationBlockEntity f,int amount){var n=f.saveWithoutMetadata();n.putInt("Energy",amount);f.load(n);}

    static ItemStack filter(){var s=new ItemStack(ModItems.WATER_FILTER_CARTRIDGE.get());s.setDamageValue(s.getMaxDamage()/2);s.setHoverName(net.minecraft.network.chat.Component.literal("Kept filter"));return s;}
    static ItemStack media(int n){return new ItemStack(ForgeRegistries.ITEMS.getValue(new ResourceLocation("domesurvival","filter_regeneration_media")),n);}
    static void prepare(FilterRegenerationBlockEntity f){f.getInventory().setStackInSlot(0,filter());f.getInventory().setStackInSlot(1,media(8));energy(f,20000);}
    static void process(ServerLevel l,ServerPlayer p){
        check(!FilterRegenerationRegistry.FILTER_REGENERATION_STATION.get().defaultBlockState().canOcclude(),"Recessed shell keeps neighboring faces");
        for(var item:ForgeRegistries.ITEMS.getValues()){
            var input=new ItemStack(item);if(!FilterRegenerationBlockEntity.isEligibleFilter(input))continue;
            input.setDamageValue(Math.max(1,input.getMaxDamage()/4));input.setHoverName(net.minecraft.network.chat.Component.literal("Kept name"));
            for(int variant=0;variant<3;variant++){
                var f=place(l,TEST);prepare(f);f.getInventory().setStackInSlot(0,input.copy());
                if(variant>0)f.getModules().setStackInSlot(0,new ItemStack((variant==1?MachineModuleItems.EFFICIENCY:MachineModuleItems.OVERDRIVE).get()));
                int duration=f.processingTicks(),cost=f.processingEnergy();tick(l,f,duration-1);
                check(f.getInventory().getStackInSlot(2).isEmpty(),"No early repair "+item+"/"+variant);tick(l,f,1);
                var out=f.getInventory().getStackInSlot(2);
                check(out.is(item)&&out.getDamageValue()==0&&out.hasCustomHoverName()&&FilterRegenerationBlockEntity.getRegenerationCycles(out)==1,"Repair preserves filter NBT "+item+"/"+variant);
                check(f.getDataAccess().get(0)==20000-cost&&f.getInventory().getStackInSlot(1).getCount()==7,"Exact cycle FE and media "+item+"/"+variant);
            }
        }
        var f=place(l,TEST);prepare(f);tick(l,f,50);var menu=new FilterRegenerationMenu(1,p.getInventory(),f);
        check(!menu.getSlot(36).mayPickup(p)&&menu.quickMoveStack(p,36).isEmpty(),"Paid cycle locks filter removal");
        f.getModules().setStackInSlot(0,new ItemStack(MachineModuleItems.EFFICIENCY.get()));
        check(f.processingTicks()==200&&f.processingEnergy()==8000,"Hot module keeps current cycle snapshot");
        var saved=f.saveWithoutMetadata();f.load(saved);tick(l,f,150);
        check(f.getDataAccess().get(0)==12000&&f.getInventory().getStackInSlot(1).getCount()==7&&f.processingEnergy()==6400,"Saved cycle finishes at old cost; next uses module");
        tick(l,f,f.processingTicks());check(f.getInventory().getStackInSlot(2).getDamageValue()==0&&f.getDataAccess().get(0)==5600,"Automatic second cycle at new cost");
        check(!f.getModules().isItemValid(1,new ItemStack(MachineModuleItems.EFFICIENCY.get()))&&!f.getModules().isItemValid(1,new ItemStack(MachineModuleItems.OVERDRIVE.get())),"Duplicate and conflicting modules rejected");
        f=place(l,TEST);prepare(f);tick(l,f,30);energy(f,0);tick(l,f,20);check(f.getDataAccess().get(2)==30&&!f.getBlockState().getValue(FilterRegenerationBlock.ACTIVE),"No energy pauses without losing paid work");
        energy(f,20000);f.getInventory().setStackInSlot(1,ItemStack.EMPTY);tick(l,f,20);check(f.getDataAccess().get(2)==30&&f.getDataAccess().get(0)==20000,"Missing media pauses without spending");
        f.getInventory().setStackInSlot(1,media(8));tick(l,f,1);check(f.getDataAccess().get(2)==31,"Restoring media resumes");
        saved=f.saveWithoutMetadata();saved.remove("Modules");saved.remove("CycleTicks");saved.remove("CycleEnergy");saved.remove("CycleFilter");saved.remove("PortFacing");saved.getCompound("Inventory").putInt("Size",2);f.load(saved);
        check(f.getInventory().getSlots()==3&&f.getDataAccess().get(2)==31&&f.processingTicks()==200&&f.processingEnergy()==8000,"Legacy two-slot paid cycle migrates");
        tick(l,f,169);check(f.getInventory().getStackInSlot(1).getCount()==7&&FilterRegenerationBlockEntity.getRegenerationCycles(f.getInventory().getStackInSlot(0))==1,"Legacy cycle completes once");
        f=place(l,TEST);prepare(f);var limited=filter();limited.getOrCreateTag().putInt("DomeRegenCycles",7);f.getInventory().setStackInSlot(0,limited);tick(l,f,200);
        check(FilterRegenerationBlockEntity.getRegenerationCycles(f.getInventory().getStackInSlot(2))==8&&f.getInventory().getStackInSlot(2).getDamageValue()>0,"Eighth repair outputs still-damaged filter");
        var exhausted=f.getInventory().extractItem(2,1,false);f.getInventory().setStackInSlot(0,exhausted);tick(l,f,300);check(f.getDataAccess().get(0)==12000&&f.getInventory().getStackInSlot(1).getCount()==7,"Exhausted filter consumes no resources");
        f=place(l,TEST);prepare(f);var shallow=filter();shallow.setDamageValue(1);f.getInventory().setStackInSlot(0,shallow);f.getInventory().setStackInSlot(2,filter());tick(l,f,220);
        check(f.status()==FilterRegenerationBlockEntity.STATUS_OUTPUT_FULL&&f.getDataAccess().get(0)==12000&&f.getInventory().getStackInSlot(0).getDamageValue()==0,"Blocked output retains repair without repeat charge");
        f.getInventory().extractItem(2,1,false);tick(l,f,1);check(f.getInventory().getStackInSlot(0).isEmpty()&&f.getInventory().getStackInSlot(2).getDamageValue()==0,"Cleared output receives repaired filter");
        f.getModules().setStackInSlot(0,new ItemStack(MachineModuleItems.BUFFER.get()));energy(f,35000);menu=new FilterRegenerationMenu(2,p.getInventory(),f);menu.setTab(200);
        check(menu.energyCapacity()==35000&&!menu.getSlot(39).mayPickup(p)&&menu.quickMoveStack(p,39).isEmpty(),"High-charge buffer removal blocked");energy(f,20000);check(menu.getSlot(39).mayPickup(p),"Safe buffer removal allowed");
        menu.setTab(202);check(!menu.getSlot(36).isActive()&&!menu.getSlot(37).isActive()&&!menu.getSlot(38).isActive()&&!menu.getSlot(39).isActive(),"Configuration hides all process and module slots");
    }
    static void ports(ServerLevel l){
        for(Direction facing:List.of(Direction.NORTH,Direction.EAST,Direction.SOUTH,Direction.WEST)){
            var f=place(l,TEST);l.setBlockAndUpdate(TEST,f.getBlockState().setValue(FilterRegenerationBlock.FACING,facing));tick(l,f,1);
            for(RelativeSide side:RelativeSide.values())for(SideMode m:List.of(SideMode.DISABLED,SideMode.INPUT,SideMode.OUTPUT)){
                mode(f,side,m);var d=side.resolve(facing);energy(f,1000);f.getInventory().setStackInSlot(0,ItemStack.EMPTY);f.getInventory().setStackInSlot(1,ItemStack.EMPTY);f.getInventory().setStackInSlot(2,filter());
                var items=f.getCapability(ForgeCapabilities.ITEM_HANDLER,d).orElse(null);var fe=f.getCapability(ForgeCapabilities.ENERGY,d).orElse(null);
                boolean input=side!=RelativeSide.FRONT&&m==SideMode.INPUT,output=side!=RelativeSide.FRONT&&m==SideMode.OUTPUT;
                check((items!=null)==(input||output)&&(fe!=null)==input,"Capabilities "+facing+"/"+side+"/"+m);
                check(l.getBlockState(TEST).getValue(FilterRegenerationBlock.portProperty(d))==PortVisual.fromMode(f.sideMode(d)),"Visual agrees "+facing+"/"+side+"/"+m);
                if(input){
                    check(items.insertItem(0,filter(),true).isEmpty()&&f.getInventory().getStackInSlot(0).isEmpty(),"Filter simulation");
                    check(items.insertItem(0,filter(),false).isEmpty()&&items.insertItem(1,media(1),false).isEmpty()&&items.extractItem(0,1,false).isEmpty()&&items.extractItem(2,1,false).isEmpty(),"Blue takes filter and media, never extracts");
                    check(!items.isItemValid(0,new ItemStack(Items.COAL))&&!items.isItemValid(1,filter())&&!items.isItemValid(2,filter()),"Input whitelist");
                    check(fe.receiveEnergy(1000,true)==64&&f.getDataAccess().get(0)==1000&&fe.extractEnergy(100,false)==0,"FE simulation and limit");
                    items.getStackInSlot(0).setCount(0);check(!f.getInventory().getStackInSlot(0).isEmpty(),"Item view cannot mutate inventory");
                    mode(f,side,SideMode.DISABLED);check(fe.receiveEnergy(64,false)==0&&!items.isItemValid(0,filter()),"Cached input respects OFF");
                }else if(output){
                    check(items.extractItem(0,1,false).isEmpty()&&!items.insertItem(0,filter(),false).isEmpty(),"Orange denies insertion and unfinished filter");
                    check(!items.extractItem(2,1,true).isEmpty()&&!f.getInventory().getStackInSlot(2).isEmpty(),"Output simulation");
                    check(!items.extractItem(2,1,false).isEmpty()&&f.getInventory().getStackInSlot(2).isEmpty(),"Orange extracts only finished filter");
                    f.getInventory().setStackInSlot(2,filter());mode(f,side,SideMode.DISABLED);check(items.extractItem(2,1,false).isEmpty(),"Cached output respects OFF");
                }
            }
            check(!f.getCapability(ForgeCapabilities.ENERGY,null).isPresent()&&!f.getCapability(ForgeCapabilities.ITEM_HANDLER,null).isPresent(),"Unsided automation denied");
            var n=f.saveWithoutMetadata();f.load(n);for(var side:RelativeSide.values())check(f.sideMode(side.resolve(facing))==SideMode.DISABLED,"Side settings survive save "+side);
        }
    }
    static void dismantle(ServerLevel l,ServerPlayer p){
        p.setGameMode(GameType.SURVIVAL);p.setShiftKeyDown(false);
        for(Item tool:new Item[]{Items.IRON_PICKAXE,Items.WOODEN_PICKAXE}){
            var f=place(l,TEST);clear(l);f.getModules().setStackInSlot(0,new ItemStack(MachineModuleItems.BUFFER.get()));f.getInventory().setStackInSlot(1,media(3));
            p.setItemInHand(InteractionHand.MAIN_HAND,new ItemStack(tool));p.gameMode.destroyBlock(TEST);
            check(count(l,FilterRegenerationRegistry.FILTER_REGENERATION_STATION.get().asItem())==(tool==Items.IRON_PICKAXE?1:0),"Survival harvest tier "+tool);
            check(count(l,MachineModuleItems.BUFFER.get())==1&&count(l,media(1).getItem())==3,"Contents and modules drop once");
        }
        var f=place(l,TEST);clear(l);prepare(f);f.getModules().setStackInSlot(0,new ItemStack(MachineModuleItems.BUFFER.get()));energy(f,35000);tick(l,f,40);
        var wrench=ForgeRegistries.ITEMS.getValue(new ResourceLocation("domesurvival","machine_wrench"));p.getInventory().clearContent();p.setItemInHand(InteractionHand.MAIN_HAND,new ItemStack(wrench));
        var hit=new BlockHitResult(Vec3.atCenterOf(TEST),Direction.NORTH,TEST,false);mode(f,RelativeSide.TOP,SideMode.INPUT);mode(f,RelativeSide.LEFT,SideMode.OUTPUT);
        p.gameMode.useItemOn(p,l,p.getMainHandItem(),InteractionHand.MAIN_HAND,hit);check(f.getMachineFacing()==Direction.EAST&&f.sideMode(RelativeSide.LEFT.resolve(f.getMachineFacing()))==SideMode.OUTPUT,"Wrench rotates configured sides");
        var expected=f.saveWithoutMetadata();p.setShiftKeyDown(true);p.gameMode.useItemOn(p,l,p.getMainHandItem(),InteractionHand.MAIN_HAND,hit);
        check(l.isEmptyBlock(TEST)&&drops(l).size()==1&&p.getInventory().countItem(FilterRegenerationRegistry.FILTER_REGENERATION_STATION.get().asItem())==0,"Wrench drops one machine in world");
        var entity=drops(l).get(0);var stack=entity.getItem().copy();var saved=stack.getTag().getCompound("BlockEntityTag");
        for(String key:List.of("Modules","Energy","Progress","CycleTicks","CycleEnergy","CycleFilter","Inventory","UnifiedSideConfig"))check(saved.get(key).equals(expected.get(key)),"Portable data retained "+key);
        entity.playerTouch(p);check(entity.isAlive(),"Normal pickup delay");clear(l);p.setShiftKeyDown(false);p.setYRot(180);p.setItemInHand(InteractionHand.MAIN_HAND,stack);
        p.gameMode.useItemOn(p,l,p.getMainHandItem(),InteractionHand.MAIN_HAND,new BlockHitResult(Vec3.atCenterOf(TEST.below()).add(0,.5,0),Direction.UP,TEST.below(),false));
        f=(FilterRegenerationBlockEntity)l.getBlockEntity(TEST);check(f!=null&&f.getDataAccess().get(2)==40&&f.getDataAccess().get(0)==expected.getInt("Energy"),"Placement restores paid progress and FE");
        check(f.sideMode(RelativeSide.LEFT.resolve(f.getMachineFacing()))==SideMode.OUTPUT,"Placed sides follow new facing");
        p.setGameMode(GameType.ADVENTURE);p.setShiftKeyDown(true);p.setItemInHand(InteractionHand.MAIN_HAND,new ItemStack(wrench));p.gameMode.useItemOn(p,l,p.getMainHandItem(),InteractionHand.MAIN_HAND,hit);check(l.getBlockEntity(TEST)==f,"Adventure dismantle denied");p.setShiftKeyDown(false);p.setGameMode(GameType.CREATIVE);p.getInventory().clearContent();
    }
    @SubscribeEvent public static void server(TickEvent.ServerTickEvent e){
        if(!ENABLED||e.phase!=TickEvent.Phase.END||fixture)return;var mc=Minecraft.getInstance();var server=mc.getSingleplayerServer();if(server==null||server.getPlayerList().getPlayers().isEmpty())return;
        if(!mc.gameDirectory.toPath().toAbsolutePath().normalize().endsWith("filter-regeneration-review"))throw new IllegalStateException("Isolated directory required");
        fixture=true;try{
            var l=server.overworld();var p=server.getPlayerList().getPlayers().get(0);l.setDayTime(6000);l.getGameRules().getRule(GameRules.RULE_DAYLIGHT).set(false,server);l.getGameRules().getRule(GameRules.RULE_DOMOBSPAWNING).set(false,server);
            for(int x=-4;x<16;x++)for(int z=-4;z<16;z++)l.setBlockAndUpdate(new BlockPos(x,99,z),Blocks.SMOOTH_STONE.defaultBlockState());
            process(l,p);ports(l);dismantle(l,p);
            var f=place(l,DISPLAY);prepare(f);f.getModules().setStackInSlot(0,new ItemStack(MachineModuleItems.BUFFER.get()));energy(f,35000);f.getInventory().setStackInSlot(1,ItemStack.EMPTY);mode(f,RelativeSide.TOP,SideMode.DISABLED);
            l.setBlockAndUpdate(DISPLAY.west(),ItemPipeRegistry.STEEL_PIPE.get().defaultBlockState());
            p.teleportTo(l,1.6,100.3,-2.7,19,23);p.getAbilities().flying=true;p.onUpdateAbilities();p.getInventory().setItem(9,new ItemStack(MachineModuleItems.EFFICIENCY.get()));ready=true;
        }catch(Throwable ex){check(false,"Server exception "+ex);ex.printStackTrace();ready=true;}
    }
    static void add(int delay,Runnable action){steps.add(new Step(delay,action));}
    static void shot(String name){try(NativeImage img=Screenshot.takeScreenshot(Minecraft.getInstance().getMainRenderTarget())){Files.createDirectories(OUT);img.writeToFile(OUT.resolve(name+".png"));log("Screenshot "+name);}catch(Exception ex){check(false,"Screenshot "+ex);}}
    static FilterRegenerationScreen screen(){return (FilterRegenerationScreen)Minecraft.getInstance().screen;}
    static void tab(int id){var mc=Minecraft.getInstance();screen().getMenu().setTab(id);mc.gameMode.handleInventoryButtonClick(screen().getMenu().containerId,id);}
    static void otherGui(Block block){var mc=Minecraft.getInstance();mc.player.closeContainer();mc.setScreen(null);
        mc.getSingleplayerServer().execute(()->{var p=mc.getSingleplayerServer().getPlayerList().getPlayers().get(0);var l=p.serverLevel();var pos=DISPLAY.south(2);
            l.setBlockAndUpdate(pos,block.defaultBlockState());NetworkHooks.openScreen(p,(net.minecraft.world.MenuProvider)l.getBlockEntity(pos),pos);});
    }
    static void plan(){var mc=Minecraft.getInstance();
        check(mc.font.width(net.minecraft.network.chat.Component.translatable("gui.domesurvival.filter_regeneration_v2.media"))<=54,"Full media caption fits");
        check(mc.font.width(net.minecraft.network.chat.Component.translatable("gui.domesurvival.filter_regeneration_v2.input_short"))<=90,"Full input caption fits");mc.setScreen(null);mc.options.hideGui=true;mc.getTutorial().setStep(net.minecraft.client.tutorial.TutorialSteps.NONE);
        var animated=new FilterRegenerationBlockEntity(BlockPos.ZERO,FilterRegenerationRegistry.FILTER_REGENERATION_STATION.get().defaultBlockState().setValue(FilterRegenerationBlock.ACTIVE,true));FilterRegenerationBlockEntity.clientTick(mc.level,BlockPos.ZERO,animated.getBlockState(),animated);float angle=animated.animationPhase(1);check(angle>0,"Cleaning carriage moves when active");animated.setBlockState(animated.getBlockState().setValue(FilterRegenerationBlock.ACTIVE,false));FilterRegenerationBlockEntity.clientTick(mc.level,BlockPos.ZERO,animated.getBlockState(),animated);check(animated.animationPhase(1)==angle,"Cleaning carriage freezes while paused");
        check(ItemPipeBlock.refreshConnections(mc.level,DISPLAY.west(),mc.level.getBlockState(DISPLAY.west())).getValue(ItemPipeBlock.EAST),"Client blue item connection");

        add(20,()->shot("01_existing_model"));
        add(10,()->{mc.options.hideGui=false;mc.getSingleplayerServer().execute(()->{var p=mc.getSingleplayerServer().getPlayerList().getPlayers().get(0);NetworkHooks.openScreen(p,(FilterRegenerationBlockEntity)p.serverLevel().getBlockEntity(DISPLAY),DISPLAY);});});
        add(25,()->{check(mc.screen instanceof FilterRegenerationScreen,"Networked regenerator GUI opens");check(screen().getMenu().energyCapacity()==35000&&screen().getMenu().energyStored()>32000,"Expanded FE buffer synchronized");shot("02_main_gui");mc.options.guiScale().set(3);mc.resizeDisplay();});
        add(1,()->mc.getSingleplayerServer().execute(()->{var f=(FilterRegenerationBlockEntity)mc.getSingleplayerServer().overworld().getBlockEntity(DISPLAY);f.getInventory().setStackInSlot(1,media(8));}));
        add(25,()->{check(screen().getMenu().status()==1,"Networked regeneration status is active");check(mc.level.getBlockState(DISPLAY).getValue(FilterRegenerationBlock.ACTIVE),"Client working state synchronized");shot("02_working_gui");});
        add(40,()->shot("02_carriage_motion"));
        add(20,()->{shot("03_main_scale3");mc.options.guiScale().set(2);mc.resizeDisplay();hover(20,70);});
        add(10,()->{shot("03_energy_tooltip");hoverX=hoverY=0;tab(202);});
        add(20,()->{shot("04_sides");mc.gameMode.handleInventoryButtonClick(screen().getMenu().containerId,100+RelativeSide.TOP.ordinal());});
        add(20,()->{check(screen().getMenu().getSideMode(RelativeSide.TOP)==SideMode.INPUT,"Networked side button updates menu");check(((FilterRegenerationBlockEntity)mc.level.getBlockEntity(DISPLAY)).sideMode(Direction.UP)==SideMode.INPUT,"Client receives logical side update");tab(200);});
        add(20,()->{shot("05_modules_empty");mc.gameMode.handleInventoryMouseClick(screen().getMenu().containerId,0,0,ClickType.QUICK_MOVE,mc.player);});
        add(20,()->{check(screen().getMenu().getSlot(40).hasItem(),"Networked Shift-click installs efficiency while working");shot("06_modules_installed");mc.options.guiScale().set(3);mc.resizeDisplay();});
        add(20,()->{shot("07_module_scale3");mc.options.guiScale().set(2);mc.resizeDisplay();hover(28,75);});
        add(10,()->{shot("07_module_help");hoverX=hoverY=0;mc.player.closeContainer();
            var runtime=JeiCapture.runtime;check(runtime!=null,"JEI runtime available");var manager=runtime.getRecipeManager();
            check(manager.createRecipeCategoryLookup().get().filter(c->c.getRecipeType().getUid().equals(DomeSurvivalJeiPlugin.FILTER_REGENERATION.getUid())).count()==1,"Exactly one regenerator JEI category");
            check(manager.createRecipeLookup(DomeSurvivalJeiPlugin.FILTER_REGENERATION).get().count()==ForgeRegistries.ITEMS.getValues().stream().filter(i->FilterRegenerationBlockEntity.isEligibleFilter(new ItemStack(i))).count(),"One recipe per eligible filter, no duplicates");
            runtime.getRecipesGui().showTypes(List.of(DomeSurvivalJeiPlugin.FILTER_REGENERATION));});

        add(30,()->shot("08_jei"));add(100,()->shot("09_jei_progress"));
        add(10,()->{mc.setScreen(null);log("RESULT "+(failures==0?"PASS":"FAIL")+" failures="+failures);mc.stop();});
    }
    @SubscribeEvent public static void mouse(TickEvent.RenderTickEvent e){
        if(!ENABLED||e.phase!=TickEvent.Phase.START||!planned)return;
        try{var mc=Minecraft.getInstance();
            net.minecraftforge.fml.util.ObfuscationReflectionHelper.findField(MouseHandler.class,"f_91507_").setDouble(mc.mouseHandler,hoverX);
            net.minecraftforge.fml.util.ObfuscationReflectionHelper.findField(MouseHandler.class,"f_91508_").setDouble(mc.mouseHandler,hoverY);
        }catch(ReflectiveOperationException ex){throw new RuntimeException(ex);}
    }
    @SubscribeEvent public static void client(TickEvent.ClientTickEvent e){
        if(!ENABLED||e.phase!=TickEvent.Phase.END)return;var mc=Minecraft.getInstance();
        if(!started&&mc.screen!=null&&mc.screen.getClass().getSimpleName().equals("AccessibilityOnboardingScreen")&&mc.getOverlay()==null){mc.setScreen(new TitleScreen());return;}
        if(!started&&mc.screen instanceof TitleScreen&&mc.getOverlay()==null){started=true;mc.options.pauseOnLostFocus=false;mc.options.fov().set(50);mc.options.guiScale().set(2);mc.options.renderDistance().set(6);mc.options.enableVsync().set(false);mc.options.framerateLimit().set(120);mc.getLanguageManager().setSelected("ru_ru");mc.options.languageCode="ru_ru";mc.reloadResourcePacks();mc.createWorldOpenFlows().createFreshLevel("filter_review_"+System.currentTimeMillis(),new LevelSettings("Electrolyzer review",GameType.CREATIVE,false,Difficulty.PEACEFUL,true,new GameRules(),WorldDataConfiguration.DEFAULT),new WorldOptions(63L,false,false),a->a.registryOrThrow(Registries.WORLD_PRESET).getHolderOrThrow(WorldPresets.FLAT).value().createWorldDimensions());return;}
        if(!ready||mc.player==null||mc.level==null||mc.getOverlay()!=null)return;ticks++;
        if(!planned&&ticks>100){planned=true;ticks=0;plan();}else if(planned&&step<steps.size()&&ticks>=steps.get(step).delay()){ticks=0;try{steps.get(step++).action().run();}catch(Throwable ex){check(false,"Client exception "+ex);ex.printStackTrace();log("RESULT FAIL");mc.stop();}}
    }
}
