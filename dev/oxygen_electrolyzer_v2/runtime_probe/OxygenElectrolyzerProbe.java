package com.wasted.domesurvival.electrolyzerprobe;

import com.mojang.blaze3d.platform.NativeImage;
import com.wasted.domesurvival.forge.block.ModBlocks;
import com.wasted.domesurvival.forge.item.ModItems;
import com.wasted.domesurvival.forge.itempipe.ItemPipeBlock;
import com.wasted.domesurvival.forge.itempipe.ItemPipeRegistry;
import com.wasted.domesurvival.forge.machine.oxygen.*;
import com.wasted.domesurvival.forge.machine.module.*;
import com.wasted.domesurvival.forge.machine.side.*;
import com.wasted.domesurvival.forge.client.screen.OxygenElectrolyzerScreen;
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
public final class OxygenElectrolyzerProbe {
    static final boolean ENABLED=Boolean.getBoolean("dome.oxygenElectrolyzerReview");
    static final Path OUT=Path.of("../../dev/oxygen_electrolyzer_v2/runtime").toAbsolutePath().normalize();
    static final BlockPos DISPLAY=new BlockPos(0,100,0),TEST=new BlockPos(10,100,10);
    static boolean started,fixture,planned;static volatile boolean ready;static int ticks,step,failures;
    static double hoverX,hoverY;
    static void hover(int x,int y){var w=Minecraft.getInstance().getWindow();hoverX=((w.getGuiScaledWidth()-220)/2+x)*w.getGuiScale();hoverY=((w.getGuiScaledHeight()-266)/2+y)*w.getGuiScale();}
    record Step(int delay,Runnable action){}static final List<Step> steps=new ArrayList<>();
    static synchronized void log(String s){System.out.println("[ELECTROLYZER_REVIEW] "+s);try{Files.createDirectories(OUT);Files.writeString(OUT.resolve("checks.txt"),s+"\n",StandardOpenOption.CREATE,StandardOpenOption.APPEND);}catch(Exception ex){throw new RuntimeException(ex);}}
    static void check(boolean ok,String s){if(!ok)failures++;log((ok?"PASS ":"FAIL ")+s);}
    static OxygenElectrolyzerBlockEntity place(ServerLevel l,BlockPos p){l.setBlockAndUpdate(p,Blocks.AIR.defaultBlockState());l.setBlockAndUpdate(p,ModBlocks.OXYGEN_ELECTROLYZER.get().defaultBlockState());return (OxygenElectrolyzerBlockEntity)l.getBlockEntity(p);}
    static void tick(ServerLevel l,OxygenElectrolyzerBlockEntity f,int n){for(int i=0;i<n;i++)OxygenElectrolyzerBlockEntity.serverTick(l,f.getBlockPos(),f.getBlockState(),f);}
    static void mode(OxygenElectrolyzerBlockEntity f,RelativeSide s,SideMode m){for(int i=0;i<3&&f.sideMode(s.resolve(f.getMachineFacing()))!=m;i++)f.cycleSideMode(s);}
    static List<ItemEntity> drops(ServerLevel l){return l.getEntitiesOfClass(ItemEntity.class,new AABB(TEST).inflate(2));}
    static void clear(ServerLevel l){drops(l).forEach(ItemEntity::discard);}
    static int count(ServerLevel l,Item i){return drops(l).stream().filter(e->e.getItem().is(i)).mapToInt(e->e.getItem().getCount()).sum();}
    static void energy(OxygenElectrolyzerBlockEntity f,int amount){var n=f.saveWithoutMetadata();n.putInt("Energy",amount);f.load(n);}
    static void water(OxygenElectrolyzerBlockEntity f,int water,int oxygen){var n=f.saveWithoutMetadata();n.put("PurifiedWater",new FluidStack(ModFluids.PURIFIED_WATER.get(),water).writeToNBT(new net.minecraft.nbt.CompoundTag()));n.putInt("Oxygen",oxygen);f.load(n);}
    static void process(ServerLevel l,ServerPlayer player){
        check(!ModBlocks.OXYGEN_ELECTROLYZER.get().defaultBlockState().canOcclude(),"Recessed shell does not hide neighboring block faces");
        int[] times={200,223,149},costs={2400,1920,3840};
        for(int variant=0;variant<3;variant++){
            var f=place(l,TEST);if(variant>0)f.getModules().setStackInSlot(0,new ItemStack(variant==1?MachineModuleItems.EFFICIENCY.get():MachineModuleItems.OVERDRIVE.get()));
            water(f,1000,0);energy(f,30000);
            check(f.getDataAccess().get(7)==times[variant]&&f.getDataAccess().get(15)==costs[variant],"Expected module time/cost "+variant);
            tick(l,f,times[variant]-1);check(f.oxygenAmount()==0&&f.waterAmount()==1000,"No early result "+variant);
            tick(l,f,1);check(f.waterAmount()==800&&f.oxygenAmount()==96,"Exact water to oxygen "+variant);
            check(30000-f.getDataAccess().get(0)==costs[variant],"Exact charged energy "+variant);
        }
        var f=place(l,TEST);water(f,1000,0);energy(f,30000);tick(l,f,40);
        var menu=new OxygenElectrolyzerMenu(3,player.getInventory(),f);menu.setTab(200);player.getInventory().setItem(9,new ItemStack(MachineModuleItems.EFFICIENCY.get()));
        check(!menu.quickMoveStack(player,0).isEmpty(),"Hot module insertion");check(menu.progressMax()==200&&menu.cycleEnergy()==2400,"Active cycle unchanged by insertion");
        var saved=f.saveWithoutMetadata();var restored=new OxygenElectrolyzerBlockEntity(TEST,f.getBlockState());restored.load(saved);
        check(restored.getDataAccess().get(6)==40&&restored.getDataAccess().get(7)==200&&restored.getDataAccess().get(15)==2400,"Cycle snapshot survives save");
        tick(l,f,160);check(f.oxygenAmount()==96&&menu.progressMax()==223&&menu.cycleEnergy()==1920,"Next cycle adopts efficiency");
        check(!f.getModules().insertItem(1,new ItemStack(MachineModuleItems.OVERDRIVE.get()),false).isEmpty(),"Conflicting modules denied");
        check(!f.getModules().insertItem(1,new ItemStack(MachineModuleItems.EFFICIENCY.get()),false).isEmpty(),"Duplicate modules denied");
        tick(l,f,20);check(!menu.quickMoveStack(player,36).isEmpty()&&menu.progressMax()==223&&menu.cycleEnergy()==1920,"Hot removal preserves current cycle");
        tick(l,f,203);check(menu.progressMax()==200&&menu.cycleEnergy()==2400,"Following cycle returns to base");
        f.getModules().setStackInSlot(1,new ItemStack(MachineModuleItems.BUFFER.get()));energy(f,52000);
        check(menu.energyCapacity()==52500&&!menu.getSlot(37).mayPickup(player)&&menu.quickMoveStack(player,37).isEmpty(),"Charged buffer removal blocked");
        energy(f,30000);check(menu.getSlot(37).mayPickup(player)&&!menu.quickMoveStack(player,37).isEmpty()&&menu.energyCapacity()==30000,"Safe buffer removal");
        tick(l,f,20);int progress=menu.progress();water(f,800,3950);tick(l,f,20);check(menu.progress()==progress&&!f.getBlockState().getValue(OxygenElectrolyzerBlock.LIT),"Full oxygen buffer pauses");
        water(f,800,0);tick(l,f,1);check(menu.progress()==progress+1,"Freed oxygen buffer resumes");
        energy(f,0);progress=menu.progress();tick(l,f,20);check(menu.progress()==progress,"No power pauses");energy(f,10000);tick(l,f,1);check(menu.progress()==progress+1,"Restored power resumes");
        water(f,0,0);progress=menu.progress();tick(l,f,20);check(menu.progress()==progress,"No water pauses");water(f,800,0);tick(l,f,1);check(menu.progress()==progress+1,"Restored water resumes");
        menu.setTab(202);check(!menu.getSlot(36).isActive()&&!menu.getSlot(36).mayPickup(player),"Hidden module slots protected");
        player.teleportTo(l,10.5,101,8.5,0,0);check(!menu.clickMenuButton(player,100+RelativeSide.FRONT.ordinal()),"Front button denied");
        var legacy=f.saveWithoutMetadata();legacy.remove("PortFacing");legacy.remove("CycleTicks");legacy.remove("CycleEnergy");legacy.putInt("Progress",70);
        var sides=legacy.getCompound("UnifiedSideConfig");for(Direction d:Direction.values())sides.putString(d.getName(),d==f.getMachineFacing()?"disabled":"output");
        f.load(legacy);check(f.sideMode(Direction.UP)==SideMode.INPUT&&f.getDataAccess().get(6)==70&&f.getDataAccess().get(15)==2400,"Legacy defaults and paid cycle migrate");
        mode(f,RelativeSide.BACK,SideMode.DISABLED);var custom=f.saveWithoutMetadata();custom.remove("PortFacing");f.load(custom);check(f.sideMode(Direction.SOUTH)==SideMode.DISABLED,"Legacy custom OFF retained");
    }
    static void ports(ServerLevel l){
        for(Direction facing:List.of(Direction.NORTH,Direction.EAST,Direction.SOUTH,Direction.WEST)){
            var f=place(l,TEST);l.setBlockAndUpdate(TEST,f.getBlockState().setValue(OxygenElectrolyzerBlock.FACING,facing));tick(l,f,1);
            for(RelativeSide side:RelativeSide.values())for(SideMode m:List.of(SideMode.DISABLED,SideMode.INPUT,SideMode.OUTPUT)){
                mode(f,side,m);var d=side.resolve(facing);water(f,1000,1000);energy(f,1000);
                var fluids=f.getCapability(ForgeCapabilities.FLUID_HANDLER,d).orElse(null);var fe=f.getCapability(ForgeCapabilities.ENERGY,d).orElse(null);var gas=f.getCapability(ModCapabilities.OXYGEN,d).orElse(null);
                boolean input=side!=RelativeSide.FRONT&&m==SideMode.INPUT,output=side!=RelativeSide.FRONT&&m==SideMode.OUTPUT;
                check((fluids!=null)==input&&(fe!=null)==input&&(gas!=null)==output&&!f.getCapability(ForgeCapabilities.ITEM_HANDLER,d).isPresent(),"Capabilities "+facing+"/"+side+"/"+m);
                check(l.getBlockState(TEST).getValue(OxygenElectrolyzerBlock.portProperty(d))==PortVisual.fromMode(f.sideMode(d)),"Visual port agrees "+facing+"/"+side+"/"+m);
                if(input){
                    check(fluids.fill(new FluidStack(ModFluids.PURIFIED_WATER.get(),250),IFluidHandler.FluidAction.SIMULATE)==250&&f.waterAmount()==1000,"Input simulation unchanged "+side);
                    check(fluids.fill(new FluidStack(Fluids.WATER,250),IFluidHandler.FluidAction.EXECUTE)==0&&fluids.fill(new FluidStack(Fluids.LAVA,250),IFluidHandler.FluidAction.EXECUTE)==0,"Only purified water accepted "+side);
                    check(fluids.fill(new FluidStack(ModFluids.PURIFIED_WATER.get(),250),IFluidHandler.FluidAction.EXECUTE)==250&&f.waterAmount()==1250&&fluids.drain(100,IFluidHandler.FluidAction.EXECUTE).isEmpty(),"Water input only "+side);
                    check(fe.receiveEnergy(1000,true)==64&&fe.extractEnergy(1000,false)==0&&fe.canReceive()&&!fe.canExtract(),"FE direction and limit "+side);
                    var copy=fluids.getFluidInTank(0);copy.setAmount(1);check(f.waterAmount()==1250,"Tank view cannot mutate storage "+side);
                    mode(f,side,SideMode.DISABLED);check(!fe.canReceive()&&fe.receiveEnergy(64,false)==0&&fluids.fill(new FluidStack(ModFluids.PURIFIED_WATER.get(),100),IFluidHandler.FluidAction.EXECUTE)==0,"Cached input obeys OFF "+side);
                }else if(output){
                    check(gas.extractOxygen(1000,true)==120&&f.oxygenAmount()==1000&&gas.receiveOxygen(100,false)==0,"Oxygen simulation and direction "+side);
                    check(gas.extractOxygen(1000,false)==120&&f.oxygenAmount()==880,"Oxygen extraction amount "+side);
                    mode(f,side,SideMode.DISABLED);check(!gas.canExtract()&&gas.extractOxygen(100,false)==0,"Cached output obeys OFF "+side);
                }
            }
            check(!f.getCapability(ForgeCapabilities.ENERGY,null).isPresent()&&!f.getCapability(ForgeCapabilities.FLUID_HANDLER,null).isPresent()&&!f.getCapability(ModCapabilities.OXYGEN,null).isPresent(),"Unsided bypass denied "+facing);
        }
        var f=place(l,TEST);var pipePos=TEST.south();var gasPipe=ModBlocks.OXYGEN_PIPE.get().defaultBlockState();l.setBlockAndUpdate(pipePos,gasPipe);
        for(var m:List.of(SideMode.DISABLED,SideMode.INPUT,SideMode.OUTPUT)){
            mode(f,RelativeSide.BACK,m);check(OxygenPipeBlock.refreshConnections(l,pipePos,gasPipe).getValue(OxygenPipeBlock.NORTH)==(m==SideMode.OUTPUT),"Gas pipe connects only to orange "+m);
            var fluidPipe=FluidPipeRegistry.BASIC_FLUID_PIPE.get().defaultBlockState();
            check(FluidPipeBlock.refreshConnections(l,pipePos,fluidPipe).getValue(FluidPipeBlock.NORTH)==(m==SideMode.INPUT),"Fluid pipe connects only to blue "+m);
        }
        var sinkPos=TEST.south(2);
        l.setBlockAndUpdate(sinkPos,ModBlocks.OXYGEN_FILLER.get().defaultBlockState().setValue(OxygenFillerBlock.FACING,Direction.SOUTH));
        var sink=l.getBlockEntity(sinkPos).getCapability(ModCapabilities.OXYGEN,Direction.NORTH).orElseThrow(()->new IllegalStateException("Missing filler inlet"));
        l.setBlockAndUpdate(pipePos,OxygenPipeBlock.refreshConnections(l,pipePos,l.getBlockState(pipePos)));
        water(f,1000,1000);
        for(var m:List.of(SideMode.DISABLED,SideMode.INPUT,SideMode.OUTPUT)){
            mode(f,RelativeSide.BACK,m);
            int before=f.oxygenAmount(),received=OxygenPipeTransferService.pull(l,sinkPos,sink,100,d->d==Direction.NORTH);
            check(m==SideMode.OUTPUT?received>0&&f.oxygenAmount()==before-received:received==0&&f.oxygenAmount()==before,"Real oxygen pipe transfer obeys port "+m);
        }
        check(sink.getOxygenStored()>0,"Filler receives oxygen through real pipe");
        l.removeBlock(pipePos,false);l.removeBlock(sinkPos,false);
    }
    static void dismantle(ServerLevel l,ServerPlayer p){
        p.setGameMode(GameType.SURVIVAL);p.setShiftKeyDown(false);
        for(Item tool:new Item[]{Items.IRON_PICKAXE,Items.WOODEN_PICKAXE}){
            var f=place(l,TEST);clear(l);f.getModules().setStackInSlot(0,new ItemStack(MachineModuleItems.BUFFER.get()));
            p.setItemInHand(InteractionHand.MAIN_HAND,new ItemStack(tool));p.gameMode.destroyBlock(TEST);
            check(count(l,ModBlocks.OXYGEN_ELECTROLYZER.get().asItem())==(tool==Items.IRON_PICKAXE?1:0),"Survival harvest tier "+tool);
            check(count(l,MachineModuleItems.BUFFER.get())==1,"Module drops exactly once "+tool);
        }
        var f=place(l,TEST);clear(l);f.getModules().setStackInSlot(0,new ItemStack(MachineModuleItems.BUFFER.get()));water(f,3000,1000);energy(f,52000);tick(l,f,40);
        var wrench=ForgeRegistries.ITEMS.getValue(new ResourceLocation("domesurvival","machine_wrench"));p.getInventory().clearContent();p.setItemInHand(InteractionHand.MAIN_HAND,new ItemStack(wrench));
        var hit=new BlockHitResult(Vec3.atCenterOf(TEST),Direction.NORTH,TEST,false);mode(f,RelativeSide.TOP,SideMode.INPUT);mode(f,RelativeSide.LEFT,SideMode.OUTPUT);
        p.gameMode.useItemOn(p,l,p.getMainHandItem(),InteractionHand.MAIN_HAND,hit);check(f.getMachineFacing()==Direction.EAST&&f.sideMode(RelativeSide.LEFT.resolve(f.getMachineFacing()))==SideMode.OUTPUT,"Wrench rotates configured sides");
        var expected=f.saveWithoutMetadata();p.setShiftKeyDown(true);p.gameMode.useItemOn(p,l,p.getMainHandItem(),InteractionHand.MAIN_HAND,hit);
        check(l.isEmptyBlock(TEST)&&drops(l).size()==1&&p.getInventory().countItem(ModBlocks.OXYGEN_ELECTROLYZER.get().asItem())==0,"Wrench creates one world drop");
        var entity=drops(l).get(0);var stack=entity.getItem().copy();var saved=stack.getTag().getCompound("BlockEntityTag");
        for(String key:List.of("Modules","Energy","PurifiedWater","Oxygen","Progress","CycleTicks","CycleEnergy","UnifiedSideConfig"))check(saved.get(key).equals(expected.get(key)),"Portable data retained "+key);
        entity.playerTouch(p);check(entity.isAlive(),"Normal pickup delay");clear(l);p.setShiftKeyDown(false);p.setYRot(180);p.setItemInHand(InteractionHand.MAIN_HAND,stack);
        p.gameMode.useItemOn(p,l,p.getMainHandItem(),InteractionHand.MAIN_HAND,new BlockHitResult(Vec3.atCenterOf(TEST.below()).add(0,.5,0),Direction.UP,TEST.below(),false));
        f=(OxygenElectrolyzerBlockEntity)l.getBlockEntity(TEST);check(f!=null&&f.waterAmount()==3000&&f.oxygenAmount()==1000&&f.getDataAccess().get(0)==expected.getInt("Energy")&&f.getDataAccess().get(6)==40,"Actual placement restores fluid energy and cycle");
        check(f.sideMode(Direction.UP)==SideMode.INPUT&&f.sideMode(RelativeSide.LEFT.resolve(f.getMachineFacing()))==SideMode.OUTPUT&&!f.getCapability(ForgeCapabilities.FLUID_HANDLER,f.getMachineFacing()).isPresent(),"Placed ports follow orientation");
        p.setGameMode(GameType.ADVENTURE);p.setShiftKeyDown(true);p.setItemInHand(InteractionHand.MAIN_HAND,new ItemStack(wrench));p.gameMode.useItemOn(p,l,p.getMainHandItem(),InteractionHand.MAIN_HAND,hit);check(l.getBlockEntity(TEST)==f,"Adventure dismantle denied");p.setShiftKeyDown(false);p.setGameMode(GameType.CREATIVE);p.getInventory().clearContent();
    }
    @SubscribeEvent public static void server(TickEvent.ServerTickEvent e){
        if(!ENABLED||e.phase!=TickEvent.Phase.END||fixture)return;var mc=Minecraft.getInstance();var server=mc.getSingleplayerServer();if(server==null||server.getPlayerList().getPlayers().isEmpty())return;
        if(!mc.gameDirectory.toPath().toAbsolutePath().normalize().endsWith("oxygen-electrolyzer-review"))throw new IllegalStateException("Isolated directory required");
        fixture=true;try{
            var l=server.overworld();var p=server.getPlayerList().getPlayers().get(0);l.setDayTime(6000);l.getGameRules().getRule(GameRules.RULE_DAYLIGHT).set(false,server);l.getGameRules().getRule(GameRules.RULE_DOMOBSPAWNING).set(false,server);
            for(int x=-4;x<16;x++)for(int z=-4;z<16;z++)l.setBlockAndUpdate(new BlockPos(x,99,z),Blocks.SMOOTH_STONE.defaultBlockState());
            process(l,p);ports(l);dismantle(l,p);
            var f=place(l,DISPLAY);f.getModules().setStackInSlot(0,new ItemStack(MachineModuleItems.BUFFER.get()));water(f,3500,1000);energy(f,52500);mode(f,RelativeSide.TOP,SideMode.DISABLED);
            l.setBlockAndUpdate(DISPLAY.west(),FluidPipeRegistry.BASIC_FLUID_PIPE.get().defaultBlockState());l.setBlockAndUpdate(DISPLAY.east(),ModBlocks.OXYGEN_PIPE.get().defaultBlockState());
            p.teleportTo(l,1.6,100.3,-2.7,19,23);p.getAbilities().flying=true;p.onUpdateAbilities();p.getInventory().setItem(9,new ItemStack(MachineModuleItems.EFFICIENCY.get()));ready=true;
        }catch(Throwable ex){check(false,"Server exception "+ex);ex.printStackTrace();ready=true;}
    }
    static void add(int delay,Runnable action){steps.add(new Step(delay,action));}
    static void shot(String name){try(NativeImage img=Screenshot.takeScreenshot(Minecraft.getInstance().getMainRenderTarget())){Files.createDirectories(OUT);img.writeToFile(OUT.resolve(name+".png"));log("Screenshot "+name);}catch(Exception ex){check(false,"Screenshot "+ex);}}
    static OxygenElectrolyzerScreen screen(){return (OxygenElectrolyzerScreen)Minecraft.getInstance().screen;}
    static void tab(int id){var mc=Minecraft.getInstance();screen().getMenu().setTab(id);mc.gameMode.handleInventoryButtonClick(screen().getMenu().containerId,id);}
    static void otherGui(Block block){var mc=Minecraft.getInstance();mc.player.closeContainer();mc.setScreen(null);
        mc.getSingleplayerServer().execute(()->{var p=mc.getSingleplayerServer().getPlayerList().getPlayers().get(0);var l=p.serverLevel();var pos=DISPLAY.south(2);
            l.setBlockAndUpdate(pos,block.defaultBlockState());NetworkHooks.openScreen(p,(net.minecraft.world.MenuProvider)l.getBlockEntity(pos),pos);});
    }
    static void plan(){var mc=Minecraft.getInstance();mc.setScreen(null);mc.options.hideGui=true;mc.getTutorial().setStep(net.minecraft.client.tutorial.TutorialSteps.NONE);
        var animated=new OxygenElectrolyzerBlockEntity(BlockPos.ZERO,ModBlocks.OXYGEN_ELECTROLYZER.get().defaultBlockState().setValue(OxygenElectrolyzerBlock.LIT,true));animated.clientAnimationTick();animated.clientAnimationTick();float angle=animated.animationPhase(1);check(angle>0,"Bubbles move when active");animated.setBlockState(animated.getBlockState().setValue(OxygenElectrolyzerBlock.LIT,false));animated.clientAnimationTick();check(animated.animationPhase(1)==angle,"Bubbles freeze while paused");
        check(FluidPipeBlock.refreshConnections(mc.level,DISPLAY.west(),mc.level.getBlockState(DISPLAY.west())).getValue(FluidPipeBlock.EAST),"Client fluid connection");
        check(OxygenPipeBlock.refreshConnections(mc.level,DISPLAY.east(),mc.level.getBlockState(DISPLAY.east())).getValue(OxygenPipeBlock.WEST),"Client oxygen connection");
        add(20,()->shot("01_existing_model"));
        add(10,()->{mc.options.hideGui=false;mc.getSingleplayerServer().execute(()->{var p=mc.getSingleplayerServer().getPlayerList().getPlayers().get(0);NetworkHooks.openScreen(p,(OxygenElectrolyzerBlockEntity)p.serverLevel().getBlockEntity(DISPLAY),DISPLAY);});});
        add(25,()->{check(mc.screen instanceof OxygenElectrolyzerScreen,"Networked purifier GUI opens");check(screen().getMenu().energyCapacity()==52500&&screen().getMenu().energyStored()>30000,"Expanded FE buffer synchronized");shot("02_main_gui");mc.options.guiScale().set(3);mc.resizeDisplay();});
        add(20,()->{shot("03_main_scale3");mc.options.guiScale().set(2);mc.resizeDisplay();hover(20,70);});
        add(10,()->{shot("03_energy_tooltip");hoverX=hoverY=0;tab(202);});
        add(20,()->{shot("04_sides");mc.gameMode.handleInventoryButtonClick(screen().getMenu().containerId,100+RelativeSide.TOP.ordinal());});
        add(20,()->{check(screen().getMenu().getSideMode(RelativeSide.TOP)==SideMode.INPUT,"Networked side button updates menu");check(((OxygenElectrolyzerBlockEntity)mc.level.getBlockEntity(DISPLAY)).sideMode(Direction.UP)==SideMode.INPUT,"Client receives logical side update");tab(200);});
        add(20,()->{shot("05_modules_empty");mc.gameMode.handleInventoryMouseClick(screen().getMenu().containerId,0,0,ClickType.QUICK_MOVE,mc.player);});
        add(20,()->{check(screen().getMenu().getSlot(37).hasItem(),"Networked Shift-click installs efficiency while working");shot("06_modules_installed");mc.options.guiScale().set(3);mc.resizeDisplay();});
        add(20,()->{shot("07_module_scale3");mc.options.guiScale().set(2);mc.resizeDisplay();hover(28,75);});
        add(10,()->{shot("07_module_help");hoverX=hoverY=0;mc.player.closeContainer();
            var runtime=JeiCapture.runtime;check(runtime!=null,"JEI runtime available");var manager=runtime.getRecipeManager();
            check(manager.createRecipeCategoryLookup().get().filter(c->c.getRecipeType().getUid().equals(DomeSurvivalJeiPlugin.OXYGEN_ELECTROLYZER.getUid())).count()==1,"Exactly one electrolyzer JEI category");
            check(manager.createRecipeLookup(DomeSurvivalJeiPlugin.OXYGEN_ELECTROLYZER).get().count()==1,"One electrolysis recipe");
            runtime.getRecipesGui().showTypes(List.of(DomeSurvivalJeiPlugin.OXYGEN_ELECTROLYZER));});
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
        if(!started&&mc.screen instanceof TitleScreen&&mc.getOverlay()==null){started=true;mc.options.pauseOnLostFocus=false;mc.options.fov().set(50);mc.options.guiScale().set(2);mc.options.renderDistance().set(6);mc.options.enableVsync().set(false);mc.options.framerateLimit().set(120);mc.getLanguageManager().setSelected("ru_ru");mc.options.languageCode="ru_ru";mc.reloadResourcePacks();mc.createWorldOpenFlows().createFreshLevel("electrolyzer_review_"+System.currentTimeMillis(),new LevelSettings("Electrolyzer review",GameType.CREATIVE,false,Difficulty.PEACEFUL,true,new GameRules(),WorldDataConfiguration.DEFAULT),new WorldOptions(63L,false,false),a->a.registryOrThrow(Registries.WORLD_PRESET).getHolderOrThrow(WorldPresets.FLAT).value().createWorldDimensions());return;}
        if(!ready||mc.player==null||mc.level==null||mc.getOverlay()!=null)return;ticks++;
        if(!planned&&ticks>100){planned=true;ticks=0;plan();}else if(planned&&step<steps.size()&&ticks>=steps.get(step).delay()){ticks=0;try{steps.get(step++).action().run();}catch(Throwable ex){check(false,"Client exception "+ex);ex.printStackTrace();log("RESULT FAIL");mc.stop();}}
    }
}
