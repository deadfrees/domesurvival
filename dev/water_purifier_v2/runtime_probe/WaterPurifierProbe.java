package com.wasted.domesurvival.waterprobe;

import com.mojang.blaze3d.platform.NativeImage;
import com.wasted.domesurvival.forge.block.ModBlocks;
import com.wasted.domesurvival.forge.item.ModItems;
import com.wasted.domesurvival.forge.itempipe.ItemPipeBlock;
import com.wasted.domesurvival.forge.itempipe.ItemPipeRegistry;
import com.wasted.domesurvival.forge.machine.water.*;
import com.wasted.domesurvival.forge.machine.module.*;
import com.wasted.domesurvival.forge.machine.side.*;
import com.wasted.domesurvival.forge.client.screen.WaterPurifierScreen;
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
import net.minecraftforge.fluids.*;
import net.minecraftforge.fluids.capability.*;
import net.minecraft.world.level.material.Fluids;
import com.wasted.domesurvival.forge.fluid.ModFluids;
import com.wasted.domesurvival.forge.item.WaterFilterItem;
import java.util.*;

@Mod.EventBusSubscriber(modid="domesurvival",value=Dist.CLIENT)
public final class WaterPurifierProbe {
    static final boolean ENABLED=Boolean.getBoolean("dome.waterPurifierReview");
    static final Path OUT=Path.of("../../dev/water_purifier_v2/runtime").toAbsolutePath().normalize();
    static final BlockPos DISPLAY=new BlockPos(0,100,0),TEST=new BlockPos(10,100,10);
    static boolean started,fixture,planned;static volatile boolean ready;static int ticks,step,failures;
    static double hoverX,hoverY;
    static void hover(int x,int y){var w=Minecraft.getInstance().getWindow();hoverX=((w.getGuiScaledWidth()-220)/2+x)*w.getGuiScale();hoverY=((w.getGuiScaledHeight()-266)/2+y)*w.getGuiScale();}
    record Step(int delay,Runnable action){}static final List<Step> steps=new ArrayList<>();
    static synchronized void log(String s){System.out.println("[WATER_REVIEW] "+s);try{Files.createDirectories(OUT);Files.writeString(OUT.resolve("checks.txt"),s+"\n",StandardOpenOption.CREATE,StandardOpenOption.APPEND);}catch(Exception ex){throw new RuntimeException(ex);}}
    static void check(boolean ok,String s){if(!ok)failures++;log((ok?"PASS ":"FAIL ")+s);}
    static WaterPurifierBlockEntity place(ServerLevel l,BlockPos p){l.setBlockAndUpdate(p,Blocks.AIR.defaultBlockState());l.setBlockAndUpdate(p,ModBlocks.WATER_PURIFIER.get().defaultBlockState());return (WaterPurifierBlockEntity)l.getBlockEntity(p);}
    static void tick(ServerLevel l,WaterPurifierBlockEntity f,int n){for(int i=0;i<n;i++)WaterPurifierBlockEntity.serverTick(l,f.getBlockPos(),f.getBlockState(),f);}
    static void mode(WaterPurifierBlockEntity f,RelativeSide s,SideMode m){for(int i=0;i<3&&f.sideMode(s.resolve(f.getMachineFacing()))!=m;i++)f.cycleSideMode(s);}
    static List<ItemEntity> drops(ServerLevel l){return l.getEntitiesOfClass(ItemEntity.class,new AABB(TEST).inflate(2));}
    static void clear(ServerLevel l){drops(l).forEach(ItemEntity::discard);}
    static int count(ServerLevel l,Item i){return drops(l).stream().filter(e->e.getItem().is(i)).mapToInt(e->e.getItem().getCount()).sum();}
    static void energy(WaterPurifierBlockEntity f,int amount){var n=f.saveWithoutMetadata();n.putInt("Energy",amount);f.load(n);}
    static void water(WaterPurifierBlockEntity f,int raw,int pure){var n=f.saveWithoutMetadata();n.put("RawTank",new FluidStack(Fluids.WATER,raw).writeToNBT(new net.minecraft.nbt.CompoundTag()));n.put("PurifiedTank",new FluidStack(ModFluids.PURIFIED_WATER.get(),pure).writeToNBT(new net.minecraft.nbt.CompoundTag()));f.load(n);}
    static void process(ServerLevel l,ServerPlayer player){
        for(var filter:List.of(ModItems.WATER_FILTER_CARTRIDGE.get(),ModItems.IMPROVED_WATER_FILTER.get()))for(int variant=0;variant<3;variant++){
            var f=place(l,TEST);if(variant>0)f.getModules().setStackInSlot(0,new ItemStack(variant==1?MachineModuleItems.EFFICIENCY.get():MachineModuleItems.OVERDRIVE.get()));
            f.getInventory().setStackInSlot(1,new ItemStack(filter));water(f,1000,0);energy(f,20000);
            int time=f.getDataAccess().get(7),cost=f.getDataAccess().get(15);tick(l,f,time-1);check(f.purifiedAmount()==0,"No early fluid result "+filter+"/"+variant);
            tick(l,f,1);check(f.rawAmount()==750&&f.purifiedAmount()==200,"Exact 250 to 200 mB conversion "+filter+"/"+variant);
            check(20000-f.getDataAccess().get(0)==cost,"Exact energy cost "+filter+"/"+variant);
            check(f.getInventory().getStackInSlot(1).getDamageValue()==1,"One filter durability per cycle "+variant);
        }
        var f=place(l,TEST);f.getInventory().setStackInSlot(1,new ItemStack(ModItems.WATER_FILTER_CARTRIDGE.get()));water(f,1000,0);energy(f,20000);tick(l,f,40);
        var menu=new WaterPurifierMenu(3,player.getInventory(),f);menu.setTab(200);player.getInventory().setItem(9,new ItemStack(MachineModuleItems.EFFICIENCY.get()));
        check(!menu.quickMoveStack(player,2).isEmpty(),"Shift-click installs while working");check(menu.progressMax()==200&&menu.cycleEnergy()==3000,"Hot insertion preserves active cycle");
        var restored=new WaterPurifierBlockEntity(TEST,f.getBlockState());restored.load(f.saveWithoutMetadata());check(restored.getDataAccess().get(7)==200&&restored.getDataAccess().get(15)==3000,"Active cycle snapshot survives save");
        tick(l,f,160);check(f.purifiedAmount()==200&&menu.progressMax()==223&&menu.cycleEnergy()==2400,"Next cycle adopts efficiency");
        check(!f.getModules().insertItem(1,new ItemStack(MachineModuleItems.OVERDRIVE.get()),false).isEmpty(),"Conflicting speed modules rejected");
        check(!f.getModules().insertItem(1,new ItemStack(MachineModuleItems.EFFICIENCY.get()),false).isEmpty(),"Duplicate modules rejected");
        f.getModules().setStackInSlot(1,new ItemStack(MachineModuleItems.BUFFER.get()));energy(f,34000);check(menu.energyCapacity()==35000&&!menu.getSlot(39).mayPickup(player),"Charged buffer cannot be removed");
        energy(f,20000);check(menu.getSlot(39).mayPickup(player)&&!menu.quickMoveStack(player,39).isEmpty()&&menu.energyCapacity()==20000,"Safe buffer removal preserves energy");
        tick(l,f,20);int progress=menu.progress();water(f,750,3900);tick(l,f,20);check(menu.progress()==progress,"Full tank pauses unfinished cycle");water(f,750,0);tick(l,f,1);check(menu.progress()==progress+1,"Freed tank resumes cycle");
        energy(f,0);progress=menu.progress();tick(l,f,20);check(menu.progress()==progress,"No energy pauses cycle");energy(f,10000);tick(l,f,1);check(menu.progress()==progress+1,"Power restored resumes cycle");
        water(f,0,0);progress=menu.progress();tick(l,f,20);check(menu.progress()==progress,"No water pauses cycle");
        f.getInventory().setStackInSlot(0,new ItemStack(Items.WATER_BUCKET));tick(l,f,1);check(f.rawAmount()==1000&&f.getInventory().getStackInSlot(0).is(Items.BUCKET),"Water bucket becomes one empty bucket");
        var spent=new ItemStack(ModItems.WATER_FILTER_CARTRIDGE.get());spent.setDamageValue(spent.getMaxDamage()-1);spent.getOrCreateTag().putInt("RegenerationCycles",2);f.getInventory().setStackInSlot(1,spent);energy(f,20000);tick(l,f,menu.progressMax());
        check(f.getInventory().getStackInSlot(1).getDamageValue()==spent.getMaxDamage()&&f.getInventory().getStackInSlot(1).getTag().getInt("RegenerationCycles")==2,"Exhausted cartridge retains regeneration NBT");
        int result=f.purifiedAmount();tick(l,f,300);check(f.purifiedAmount()==result,"Exhausted filter stops processing");
        menu.setTab(202);check(!menu.getSlot(0).isActive()&&!menu.getSlot(1).mayPickup(player)&&!menu.getSlot(38).isActive(),"Hidden slots protected");
        player.teleportTo(l,10.5,101,8.5,0,0);check(!menu.clickMenuButton(player,100+RelativeSide.FRONT.ordinal()),"Front menu button rejected");
        var legacy=f.saveWithoutMetadata();legacy.remove("PortFacing");var sides=legacy.getCompound("UnifiedSideConfig");for(Direction d:Direction.values())sides.putString(d.getName(),d==f.getMachineFacing()?"disabled":"output");f.load(legacy);check(f.sideMode(Direction.UP)==SideMode.INPUT,"Legacy all-output defaults gain functioning input");
    }
    static void ports(ServerLevel l){
        for(Direction facing:List.of(Direction.NORTH,Direction.EAST,Direction.SOUTH,Direction.WEST)){
            var f=place(l,TEST);l.setBlockAndUpdate(TEST,f.getBlockState().setValue(WaterPurifierBlock.FACING,facing));tick(l,f,1);
            for(RelativeSide side:RelativeSide.values())for(SideMode m:List.of(SideMode.DISABLED,SideMode.INPUT,SideMode.OUTPUT)){
                mode(f,side,m);var d=side.resolve(facing);var items=f.getCapability(ForgeCapabilities.ITEM_HANDLER,d).orElse(null);var fluids=f.getCapability(ForgeCapabilities.FLUID_HANDLER,d).orElse(null);var fe=f.getCapability(ForgeCapabilities.ENERGY,d).orElse(null);
                boolean open=side!=RelativeSide.FRONT&&m!=SideMode.DISABLED;check((items!=null)==open&&(fluids!=null)==open&&(fe!=null)==(open&&m==SideMode.INPUT),"Capabilities "+facing+"/"+side+"/"+m);
                if(!open)continue;water(f,1000,1000);energy(f,1000);f.getInventory().setStackInSlot(0,ItemStack.EMPTY);f.getInventory().setStackInSlot(1,ItemStack.EMPTY);
                if(m==SideMode.INPUT){
                    check(fluids.fill(new FluidStack(Fluids.WATER,250),IFluidHandler.FluidAction.SIMULATE)==250&&fluids.drain(250,IFluidHandler.FluidAction.SIMULATE).isEmpty(),"Blue water fill only "+side);
                    check(fluids.fill(new FluidStack(Fluids.LAVA,250),IFluidHandler.FluidAction.EXECUTE)==0&&fe.receiveEnergy(1000,true)==64&&fe.extractEnergy(1000,false)==0,"Blue FE limit and fluid validation "+side);
                    check(items.insertItem(0,new ItemStack(Items.WATER_BUCKET),true).isEmpty()&&items.insertItem(1,new ItemStack(ModItems.WATER_FILTER_CARTRIDGE.get()),true).isEmpty()&&!items.insertItem(1,new ItemStack(Items.COAL),true).isEmpty(),"Blue item validation "+side);
                    mode(f,side,SideMode.DISABLED);check(fe.receiveEnergy(64,false)==0&&fluids.fill(new FluidStack(Fluids.WATER,100),IFluidHandler.FluidAction.EXECUTE)==0&&!items.insertItem(0,new ItemStack(Items.WATER_BUCKET),false).isEmpty(),"Cached input obeys OFF "+side);
                }else{
                    check(fluids.drain(200,IFluidHandler.FluidAction.SIMULATE).getFluid()==ModFluids.PURIFIED_WATER.get()&&fluids.fill(new FluidStack(Fluids.WATER,250),IFluidHandler.FluidAction.EXECUTE)==0,"Orange drains purified water only "+side);
                    f.getInventory().setStackInSlot(0,new ItemStack(Items.BUCKET));f.getInventory().setStackInSlot(1,new ItemStack(ModItems.WATER_FILTER_CARTRIDGE.get()));check(items.extractItem(0,1,true).is(Items.BUCKET)&&items.extractItem(1,1,true).isEmpty(),"Orange protects working filter "+side);
                    var spent=f.getInventory().getStackInSlot(1);spent.setDamageValue(spent.getMaxDamage());check(!items.extractItem(1,1,true).isEmpty(),"Orange releases spent filter "+side);
                    mode(f,side,SideMode.DISABLED);check(fluids.drain(100,IFluidHandler.FluidAction.EXECUTE).isEmpty()&&items.extractItem(0,1,false).isEmpty(),"Cached output obeys OFF "+side);
                }
            }
            check(!f.getCapability(ForgeCapabilities.ENERGY,null).isPresent()&&!f.getCapability(ForgeCapabilities.FLUID_HANDLER,null).isPresent()&&!f.getCapability(ForgeCapabilities.ITEM_HANDLER,null).isPresent(),"Unsided access denied "+facing);
        }
        var f=place(l,TEST);var neighbor=place(l,TEST.south());mode(neighbor,RelativeSide.FRONT,SideMode.DISABLED);l.setBlockAndUpdate(TEST.south(),neighbor.getBlockState().setValue(WaterPurifierBlock.FACING,Direction.SOUTH));tick(l,neighbor,1);mode(neighbor,RelativeSide.BACK,SideMode.INPUT);
        // A raw-water inlet must reject this machine's purified output.
        water(f,0,1000);tick(l,f,1);check(f.purifiedAmount()==1000&&neighbor.rawAmount()==0,"Automatic export cannot contaminate raw tank");l.removeBlock(TEST.south(),false);
        var tank=new net.minecraftforge.fluids.capability.templates.FluidTank(4000);
        var tankCap=net.minecraftforge.common.util.LazyOptional.<IFluidHandler>of(()->tank);
        l.setBlockAndUpdate(TEST.south(),Blocks.CHEST.defaultBlockState());
        l.setBlockEntity(new net.minecraft.world.level.block.entity.ChestBlockEntity(TEST.south(),Blocks.CHEST.defaultBlockState()){
            @Override public <T> net.minecraftforge.common.util.LazyOptional<T> getCapability(net.minecraftforge.common.capabilities.Capability<T> cap,Direction side){return cap==ForgeCapabilities.FLUID_HANDLER?tankCap.cast():super.getCapability(cap,side);}
        });
        mode(f,RelativeSide.BACK,SideMode.DISABLED);tick(l,f,1);check(tank.isEmpty(),"OFF prevents automatic fluid output");
        mode(f,RelativeSide.BACK,SideMode.INPUT);tick(l,f,1);check(tank.isEmpty(),"Blue prevents automatic fluid output");
        mode(f,RelativeSide.BACK,SideMode.OUTPUT);tick(l,f,1);check(tank.getFluidAmount()==100&&f.purifiedAmount()==900,"Orange automatically exports 100 mB");l.removeBlock(TEST.south(),false);
    }
    static void dismantle(ServerLevel l,ServerPlayer p){
        p.setGameMode(GameType.SURVIVAL);p.setShiftKeyDown(false);
        for(Item tool:new Item[]{Items.IRON_PICKAXE,Items.WOODEN_PICKAXE}){
            var f=place(l,TEST);clear(l);f.getInventory().setStackInSlot(0,new ItemStack(Items.WATER_BUCKET));f.getInventory().setStackInSlot(1,new ItemStack(ModItems.WATER_FILTER_CARTRIDGE.get()));f.getModules().setStackInSlot(0,new ItemStack(MachineModuleItems.BUFFER.get()));
            p.setItemInHand(InteractionHand.MAIN_HAND,new ItemStack(tool));p.gameMode.destroyBlock(TEST);
            check(count(l,ModBlocks.WATER_PURIFIER.get().asItem())==(tool==Items.IRON_PICKAXE?1:0),"Survival harvest tier "+tool);
            check(count(l,Items.WATER_BUCKET)==1&&count(l,ModItems.WATER_FILTER_CARTRIDGE.get())==1&&count(l,MachineModuleItems.BUFFER.get())==1,"Contents and module drop exactly once "+tool);
        }
        var f=place(l,TEST);clear(l);f.getInventory().setStackInSlot(1,new ItemStack(ModItems.WATER_FILTER_CARTRIDGE.get()));f.getModules().setStackInSlot(0,new ItemStack(MachineModuleItems.BUFFER.get()));water(f,3000,1000);energy(f,34000);tick(l,f,40);
        var wrench=ForgeRegistries.ITEMS.getValue(new ResourceLocation("domesurvival","machine_wrench"));p.getInventory().clearContent();p.setItemInHand(InteractionHand.MAIN_HAND,new ItemStack(wrench));
        var hit=new BlockHitResult(Vec3.atCenterOf(TEST),Direction.NORTH,TEST,false);mode(f,RelativeSide.TOP,SideMode.INPUT);mode(f,RelativeSide.LEFT,SideMode.OUTPUT);
        p.gameMode.useItemOn(p,l,p.getMainHandItem(),InteractionHand.MAIN_HAND,hit);check(f.getMachineFacing()==Direction.EAST&&f.sideMode(RelativeSide.LEFT.resolve(f.getMachineFacing()))==SideMode.OUTPUT,"Wrench rotates configured sides");
        var expected=f.saveWithoutMetadata();p.setShiftKeyDown(true);p.gameMode.useItemOn(p,l,p.getMainHandItem(),InteractionHand.MAIN_HAND,hit);
        check(l.isEmptyBlock(TEST)&&drops(l).size()==1&&p.getInventory().countItem(ModBlocks.WATER_PURIFIER.get().asItem())==0,"Wrench creates one world drop");
        var entity=drops(l).get(0);var stack=entity.getItem().copy();var saved=stack.getTag().getCompound("BlockEntityTag");
        for(String key:List.of("Inventory","Modules","Energy","RawTank","PurifiedTank","Progress","CycleTicks","CycleEnergy","UnifiedSideConfig"))check(saved.get(key).equals(expected.get(key)),"Portable data retained "+key);
        entity.playerTouch(p);check(entity.isAlive(),"Normal pickup delay");clear(l);p.setShiftKeyDown(false);p.setYRot(180);p.setItemInHand(InteractionHand.MAIN_HAND,stack);
        p.gameMode.useItemOn(p,l,p.getMainHandItem(),InteractionHand.MAIN_HAND,new BlockHitResult(Vec3.atCenterOf(TEST.below()).add(0,.5,0),Direction.UP,TEST.below(),false));
        f=(WaterPurifierBlockEntity)l.getBlockEntity(TEST);check(f!=null&&f.rawAmount()==3000&&f.purifiedAmount()==1000&&f.getDataAccess().get(0)==expected.getInt("Energy")&&f.getDataAccess().get(6)==40,"Actual placement restores fluid energy and cycle");
        check(f.sideMode(Direction.UP)==SideMode.INPUT&&f.sideMode(RelativeSide.LEFT.resolve(f.getMachineFacing()))==SideMode.OUTPUT&&!f.getCapability(ForgeCapabilities.FLUID_HANDLER,f.getMachineFacing()).isPresent(),"Placed ports follow orientation");
        p.setGameMode(GameType.ADVENTURE);p.setShiftKeyDown(true);p.setItemInHand(InteractionHand.MAIN_HAND,new ItemStack(wrench));p.gameMode.useItemOn(p,l,p.getMainHandItem(),InteractionHand.MAIN_HAND,hit);check(l.getBlockEntity(TEST)==f,"Adventure dismantle denied");p.setShiftKeyDown(false);p.setGameMode(GameType.CREATIVE);p.getInventory().clearContent();
    }
    @SubscribeEvent public static void server(TickEvent.ServerTickEvent e){
        if(!ENABLED||e.phase!=TickEvent.Phase.END||fixture)return;var mc=Minecraft.getInstance();var server=mc.getSingleplayerServer();if(server==null||server.getPlayerList().getPlayers().isEmpty())return;
        if(!mc.gameDirectory.toPath().toAbsolutePath().normalize().endsWith("water-purifier-review"))throw new IllegalStateException("Isolated directory required");
        fixture=true;try{
            var l=server.overworld();var p=server.getPlayerList().getPlayers().get(0);l.setDayTime(6000);l.getGameRules().getRule(GameRules.RULE_DAYLIGHT).set(false,server);l.getGameRules().getRule(GameRules.RULE_DOMOBSPAWNING).set(false,server);
            for(int x=-4;x<16;x++)for(int z=-4;z<16;z++)l.setBlockAndUpdate(new BlockPos(x,99,z),Blocks.SMOOTH_STONE.defaultBlockState());
            process(l,p);ports(l);dismantle(l,p);
            var f=place(l,DISPLAY);f.getInventory().setStackInSlot(1,new ItemStack(ModItems.WATER_FILTER_CARTRIDGE.get()));f.getModules().setStackInSlot(0,new ItemStack(MachineModuleItems.BUFFER.get()));water(f,3500,1000);energy(f,35000);mode(f,RelativeSide.TOP,SideMode.DISABLED);
            l.setBlockAndUpdate(DISPLAY.east(),ItemPipeRegistry.STEEL_PIPE.get().defaultBlockState());l.setBlockAndUpdate(DISPLAY.west(),ItemPipeRegistry.STEEL_PIPE.get().defaultBlockState());
            p.teleportTo(l,1.5,102,-4,0,25);p.getAbilities().flying=true;p.onUpdateAbilities();p.getInventory().setItem(9,new ItemStack(MachineModuleItems.EFFICIENCY.get()));ready=true;
        }catch(Throwable ex){check(false,"Server exception "+ex);ex.printStackTrace();ready=true;}
    }
    static void add(int delay,Runnable action){steps.add(new Step(delay,action));}
    static void shot(String name){try(NativeImage img=Screenshot.takeScreenshot(Minecraft.getInstance().getMainRenderTarget())){Files.createDirectories(OUT);img.writeToFile(OUT.resolve(name+".png"));log("Screenshot "+name);}catch(Exception ex){check(false,"Screenshot "+ex);}}
    static WaterPurifierScreen screen(){return (WaterPurifierScreen)Minecraft.getInstance().screen;}
    static void tab(int id){var mc=Minecraft.getInstance();screen().getMenu().setTab(id);mc.gameMode.handleInventoryButtonClick(screen().getMenu().containerId,id);}
    static void otherGui(Block block){var mc=Minecraft.getInstance();mc.player.closeContainer();mc.setScreen(null);
        mc.getSingleplayerServer().execute(()->{var p=mc.getSingleplayerServer().getPlayerList().getPlayers().get(0);var l=p.serverLevel();var pos=DISPLAY.south(2);
            l.setBlockAndUpdate(pos,block.defaultBlockState());NetworkHooks.openScreen(p,(net.minecraft.world.MenuProvider)l.getBlockEntity(pos),pos);});
    }
    static void plan(){var mc=Minecraft.getInstance();mc.setScreen(null);mc.options.hideGui=true;mc.getTutorial().setStep(net.minecraft.client.tutorial.TutorialSteps.NONE);
        var animated=new WaterPurifierBlockEntity(BlockPos.ZERO,ModBlocks.WATER_PURIFIER.get().defaultBlockState().setValue(WaterPurifierBlock.LIT,true));animated.clientAnimationTick();animated.clientAnimationTick();float angle=animated.pumpAngle(1);check(angle>0,"Pump moves when active");animated.setBlockState(animated.getBlockState().setValue(WaterPurifierBlock.LIT,false));animated.clientAnimationTick();check(animated.pumpAngle(1)==angle,"Pump freezes while paused");
        check(ItemPipeBlock.hasObjectConnector(mc.level,DISPLAY.east(),Direction.WEST),"Client pipe recognizes input");
        check(ItemPipeBlock.hasObjectConnector(mc.level,DISPLAY.west(),Direction.EAST),"Client pipe recognizes output");
        check(!ItemPipeBlock.hasObjectConnector(mc.level,DISPLAY.north(),Direction.SOUTH),"Client pipe rejects front");
        add(20,()->shot("01_existing_model"));
        add(10,()->{mc.options.hideGui=false;mc.getSingleplayerServer().execute(()->{var p=mc.getSingleplayerServer().getPlayerList().getPlayers().get(0);NetworkHooks.openScreen(p,(WaterPurifierBlockEntity)p.serverLevel().getBlockEntity(DISPLAY),DISPLAY);});});
        add(25,()->{check(mc.screen instanceof WaterPurifierScreen,"Networked purifier GUI opens");check(screen().getMenu().energyCapacity()==35000&&screen().getMenu().energyStored()>20000,"Expanded FE buffer synchronized");shot("02_main_gui");mc.options.guiScale().set(3);mc.resizeDisplay();});
        add(20,()->{shot("03_main_scale3");mc.options.guiScale().set(2);mc.resizeDisplay();hover(20,70);});
        add(10,()->{shot("03_energy_tooltip");hoverX=hoverY=0;tab(202);});
        add(20,()->{shot("04_sides");mc.gameMode.handleInventoryButtonClick(screen().getMenu().containerId,100+RelativeSide.TOP.ordinal());});
        add(20,()->{check(screen().getMenu().getSideMode(RelativeSide.TOP)==SideMode.INPUT,"Networked side button updates menu");check(((WaterPurifierBlockEntity)mc.level.getBlockEntity(DISPLAY)).sideMode(Direction.UP)==SideMode.INPUT,"Client receives logical side update");tab(200);});
        add(20,()->{shot("05_modules_empty");mc.gameMode.handleInventoryMouseClick(screen().getMenu().containerId,2,0,ClickType.QUICK_MOVE,mc.player);});
        add(20,()->{check(screen().getMenu().getSlot(39).hasItem(),"Networked Shift-click installs efficiency while working");shot("06_modules_installed");mc.options.guiScale().set(3);mc.resizeDisplay();});
        add(20,()->{shot("07_module_scale3");mc.options.guiScale().set(2);mc.resizeDisplay();hover(28,75);});
        add(10,()->{shot("07_module_help");hoverX=hoverY=0;mc.player.closeContainer();
            var runtime=JeiCapture.runtime;check(runtime!=null,"JEI runtime available");var manager=runtime.getRecipeManager();
            check(manager.createRecipeCategoryLookup().get().filter(c->c.getRecipeType().getUid().equals(DomeSurvivalJeiPlugin.WATER_PURIFIER.getUid())).count()==1,"Exactly one purifier JEI category");
            check(manager.createRecipeLookup(DomeSurvivalJeiPlugin.WATER_PURIFIER).get().count()==2,"Two distinct filter recipes");
            runtime.getRecipesGui().showTypes(List.of(DomeSurvivalJeiPlugin.WATER_PURIFIER));});
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
        if(!started&&mc.screen instanceof TitleScreen&&mc.getOverlay()==null){started=true;mc.options.pauseOnLostFocus=false;mc.options.fov().set(50);mc.options.guiScale().set(2);mc.options.renderDistance().set(6);mc.options.enableVsync().set(false);mc.options.framerateLimit().set(120);mc.getLanguageManager().setSelected("ru_ru");mc.options.languageCode="ru_ru";mc.reloadResourcePacks();mc.createWorldOpenFlows().createFreshLevel("shaft_review_"+System.currentTimeMillis(),new LevelSettings("Shaft review",GameType.CREATIVE,false,Difficulty.PEACEFUL,true,new GameRules(),WorldDataConfiguration.DEFAULT),new WorldOptions(63L,false,false),a->a.registryOrThrow(Registries.WORLD_PRESET).getHolderOrThrow(WorldPresets.FLAT).value().createWorldDimensions());return;}
        if(!ready||mc.player==null||mc.level==null||mc.getOverlay()!=null)return;ticks++;
        if(!planned&&ticks>100){planned=true;ticks=0;plan();}else if(planned&&step<steps.size()&&ticks>=steps.get(step).delay()){ticks=0;try{steps.get(step++).action().run();}catch(Throwable ex){check(false,"Client exception "+ex);ex.printStackTrace();log("RESULT FAIL");mc.stop();}}
    }
}
