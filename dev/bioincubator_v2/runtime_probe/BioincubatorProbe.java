package com.wasted.domesurvival.incubatorprobe;

import com.mojang.blaze3d.platform.NativeImage;
import com.wasted.domesurvival.forge.block.ModBlocks;
import com.wasted.domesurvival.forge.item.ModItems;
import com.wasted.domesurvival.forge.item.OxygenTankItem;
import com.wasted.domesurvival.forge.oxygen.room.*;
import com.wasted.domesurvival.forge.itempipe.ItemPipeBlock;
import com.wasted.domesurvival.forge.itempipe.ItemPipeRegistry;
import com.wasted.domesurvival.forge.machine.bio.*;
import com.wasted.domesurvival.forge.machine.module.*;
import com.wasted.domesurvival.forge.machine.side.*;
import com.wasted.domesurvival.forge.machine.bio.BioincubatorScreen;
import com.wasted.domesurvival.forge.client.jei.DomeSurvivalJeiPlugin;
import com.wasted.domesurvival.forge.bio.*;
import com.wasted.domesurvival.forge.item.BioModuleItem;
import com.wasted.domesurvival.forge.quest.QuestProgressService;
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
public final class BioincubatorProbe {
    static final boolean ENABLED=Boolean.getBoolean("dome.bioincubatorReview");
    static final Path OUT=Path.of("../../dev/bioincubator_v2/runtime").toAbsolutePath().normalize();
    static final BlockPos DISPLAY=new BlockPos(0,100,0),TEST=new BlockPos(10,100,10);
    static boolean started,fixture,planned;static volatile boolean ready;static int ticks,step,failures,frames;
    static double hoverX,hoverY;
    static void hover(int x,int y){var w=Minecraft.getInstance().getWindow();hoverX=((w.getGuiScaledWidth()-220)/2+x)*w.getGuiScale();hoverY=((w.getGuiScaledHeight()-266)/2+y)*w.getGuiScale();}
    record Step(int delay,Runnable action){}static final List<Step> steps=new ArrayList<>();
    static synchronized void log(String s){System.out.println("[INCUBATOR_REVIEW] "+s);try{Files.createDirectories(OUT);Files.writeString(OUT.resolve("checks.txt"),s+"\n",StandardOpenOption.CREATE,StandardOpenOption.APPEND);}catch(Exception ex){throw new RuntimeException(ex);}}
    static void check(boolean ok,String s){if(!ok)failures++;log((ok?"PASS ":"FAIL ")+s);}
    static BioincubatorBlockEntity place(ServerLevel l,BlockPos p){l.setBlockAndUpdate(p,Blocks.AIR.defaultBlockState());l.setBlockAndUpdate(p,ModBlocks.BIOINCUBATOR.get().defaultBlockState());return (BioincubatorBlockEntity)l.getBlockEntity(p);}
    static void tick(ServerLevel l,BioincubatorBlockEntity f,int n){for(int i=0;i<n;i++)BioincubatorBlockEntity.serverTick(l,f.getBlockPos(),f.getBlockState(),f);}
    static void mode(BioincubatorBlockEntity f,RelativeSide s,SideMode m){for(int i=0;i<3&&f.sideMode(s.resolve(f.getMachineFacing()))!=m;i++)f.cycleSideMode(s);}
    static List<ItemEntity> drops(ServerLevel l){return l.getEntitiesOfClass(ItemEntity.class,new AABB(TEST).inflate(2));}
    static void clear(ServerLevel l){drops(l).forEach(ItemEntity::discard);}
    static int count(ServerLevel l,Item i){return drops(l).stream().filter(e->e.getItem().is(i)).mapToInt(e->e.getItem().getCount()).sum();}
    static void energy(BioincubatorBlockEntity f,int amount){var n=f.saveWithoutMetadata();n.putInt("Energy",amount);f.load(n);}

    static final ResourceLocation CHICKEN=new ResourceLocation("minecraft","chicken");
    static ItemStack filter(){return BioModuleItem.create(CHICKEN,false);}
    static ItemStack media(int n){return new ItemStack(ForgeRegistries.ITEMS.getValue(BioLootData.species(CHICKEN).feedItem()),n);}
    static void prepare(BioincubatorBlockEntity f){
        var species=BioLootData.species(CHICKEN);f.getInventory().setStackInSlot(0,filter());f.getInventory().setStackInSlot(1,new ItemStack(ForgeRegistries.ITEMS.getValue(species.feedItem()),species.feedCount()*3));energy(f,60000);
        mode(f,RelativeSide.TOP,SideMode.INPUT);f.getCapability(ForgeCapabilities.FLUID_HANDLER,Direction.UP).orElseThrow(()->new IllegalStateException()).fill(new FluidStack(ModFluids.PURIFIED_WATER.get(),6000),IFluidHandler.FluidAction.EXECUTE);
    }
    static void repair(BioincubatorBlockEntity f){prepare(f);if(f.getDataAccess().get(8)!=1)f.toggleMode();f.getInventory().setStackInSlot(0,BioModuleItem.create(CHICKEN,true));f.getInventory().setStackInSlot(1,new ItemStack(ModItems.BIO_REPAIR_KIT.get()));f.getInventory().setStackInSlot(2,new ItemStack(ModItems.BIOGEL.get()));f.getInventory().setStackInSlot(3,new ItemStack(ModItems.NUTRIENT_MIX.get()));}
    static List<net.minecraft.world.entity.AgeableMob> babies(ServerLevel l){return l.getEntitiesOfClass(net.minecraft.world.entity.AgeableMob.class,new AABB(TEST).inflate(4));}
    static long run(ServerLevel l,BioincubatorBlockEntity f,int ticks){
        long used=0;for(int i=0;i<ticks;i++){
            if(f.getDataAccess().get(0)<1000){var cap=f.getCapability(ForgeCapabilities.ENERGY,Direction.UP).orElseThrow(()->new IllegalStateException());while(cap.receiveEnergy(128,false)>0){}}
            int before=f.getDataAccess().get(0);tick(l,f,1);used+=before-f.getDataAccess().get(0);
        }return used;
    }
    static void process(ServerLevel l,ServerPlayer p){
        QuestProgressService.set(l,BioModuleData.IDENTIFICATION_FLAG,"incubator_probe");
        check(!ModBlocks.BIOINCUBATOR.get().defaultBlockState().canOcclude(),"Recessed chamber keeps adjacent faces");
        for(var species:BioLootData.allSpecies())for(int task=0;task<2;task++){
            var f=place(l,TEST);prepare(f);babies(l).forEach(net.minecraft.world.entity.Entity::discard);
            if(task==1)repair(f);
            f.getInventory().setStackInSlot(0,BioModuleItem.create(species.entityId(),task==1));
            if(task==0)f.getInventory().setStackInSlot(1,new ItemStack(ForgeRegistries.ITEMS.getValue(species.feedItem()),species.feedCount()));
            int ticks=f.getDataAccess().get(5),cost=f.getDataAccess().get(15),water=task==1?1000:species.waterMb();
            long used=run(l,f,ticks-1);check(f.getInventory().getStackInSlot(4).isEmpty()&&babies(l).isEmpty(),"No early result "+species.entityId()+"/"+task);used+=run(l,f,1);
            // Vanilla Frog.setBaby is a no-op: its juvenile form is a separate tadpole entity.
            // Preserve the machine's existing species behavior; all other supported species receive juvenile age.
            if(task==0)check(babies(l).size()==1&&(babies(l).get(0) instanceof net.minecraft.world.entity.animal.frog.Frog?babies(l).get(0).getAge()==0:babies(l).get(0).getAge()<0)&&ForgeRegistries.ENTITY_TYPES.getKey(babies(l).get(0).getType()).equals(species.entityId()),"Correct species and vanilla juvenile behavior "+species.entityId());
            else{var sample=BioModuleData.sample(f.getInventory().getStackInSlot(4));check(sample!=null&&!sample.damaged()&&sample.entityId().equals(species.entityId()),"Repair preserves species "+species.entityId());}
            check(used==cost&&f.getDataAccess().get(2)==6000-water&&f.getInventory().getStackInSlot(0).isEmpty()&&f.getInventory().getStackInSlot(1).isEmpty(),"Exact resources "+species.entityId()+"/"+task);
        }
        for(var module:List.of(MachineModuleItems.EFFICIENCY,MachineModuleItems.OVERDRIVE))for(int task=0;task<2;task++){
            var f=place(l,TEST);if(task==1)repair(f);else prepare(f);babies(l).forEach(net.minecraft.world.entity.Entity::discard);
            f.getModules().setStackInSlot(0,new ItemStack(module.get()));int ticks=f.getDataAccess().get(5),cost=f.getDataAccess().get(15);
            check(run(l,f,ticks)==cost&&f.getDataAccess().get(4)==0&&(task==1?!f.getInventory().getStackInSlot(4).isEmpty():babies(l).size()==1),"Module cost and duration "+module.getId()+"/"+task);
        }
        var f=place(l,TEST);repair(f);tick(l,f,40);f.getModules().setStackInSlot(0,new ItemStack(MachineModuleItems.EFFICIENCY.get()));
        check(f.getDataAccess().get(4)==40&&f.getDataAccess().get(5)==1800&&f.getDataAccess().get(15)==144000,"Hot module preserves paid cycle");var saved=f.saveWithoutMetadata();f.load(saved);
        check(run(l,f,1760)==140800&&!f.getInventory().getStackInSlot(4).isEmpty(),"Saved cycle completes at original cost");
        check(!f.getModules().isItemValid(1,new ItemStack(MachineModuleItems.OVERDRIVE.get()))&&!f.getModules().isItemValid(1,new ItemStack(MachineModuleItems.EFFICIENCY.get())),"Conflicting and duplicate modules rejected");
        f=place(l,TEST);prepare(f);tick(l,f,30);energy(f,0);tick(l,f,20);check(f.getDataAccess().get(4)==30&&!f.getBlockState().getValue(BioincubatorBlock.LIT),"No FE freezes active cycle");energy(f,60000);
        l.setBlockAndUpdate(TEST.north(),Blocks.STONE.defaultBlockState());tick(l,f,20);check(f.getDataAccess().get(4)==30&&f.getDataAccess().get(0)==60000,"Blocked birth exit pauses without consuming");l.setBlockAndUpdate(TEST.north(),Blocks.AIR.defaultBlockState());
        var feed=f.getInventory().getStackInSlot(1).copy();f.getInventory().setStackInSlot(1,ItemStack.EMPTY);tick(l,f,20);check(f.getDataAccess().get(4)==30&&f.getDataAccess().get(0)==60000,"Missing feed pauses without consuming");f.getInventory().setStackInSlot(1,feed);
        saved=f.saveWithoutMetadata();saved.put("PurifiedWater",new FluidStack(ModFluids.PURIFIED_WATER.get(),1).writeToNBT(new net.minecraft.nbt.CompoundTag()));f.load(saved);tick(l,f,20);check(f.getDataAccess().get(4)==30&&f.getDataAccess().get(0)==60000,"Insufficient water pauses without consuming");
        f.getCapability(ForgeCapabilities.FLUID_HANDLER,Direction.UP).orElseThrow(()->new IllegalStateException()).fill(new FluidStack(ModFluids.PURIFIED_WATER.get(),6000),IFluidHandler.FluidAction.EXECUTE);tick(l,f,1);check(f.getDataAccess().get(4)==31,"Restored resources resume cycle");
        saved=f.saveWithoutMetadata();for(String key:List.of("CycleTicks","CycleEnergy","CycleCapsule","CycleMode","Modules","PortFacing"))saved.remove(key);f.load(saved);tick(l,f,1);check(f.getDataAccess().get(4)==32&&f.getDataAccess().get(15)>0,"Legacy NBT restores running cycle");
        for(var side:RelativeSide.values())mode(f,side,SideMode.DISABLED);saved=f.saveWithoutMetadata();f.load(saved);for(var side:RelativeSide.values())check(f.sideMode(side.resolve(f.getMachineFacing()))==SideMode.DISABLED,"Saved OFF remains OFF "+side);
        f=place(l,TEST);prepare(f);f.getModules().setStackInSlot(0,new ItemStack(MachineModuleItems.BUFFER.get()));energy(f,105000);var menu=new BioincubatorMenu(1,p.getInventory(),f);menu.setTab(200);
        check(menu.getEnergyCapacity()==105000&&!menu.getSlot(7).mayPickup(p)&&menu.quickMoveStack(p,7).isEmpty(),"Full buffer cannot be removed");energy(f,60000);check(menu.getSlot(7).mayPickup(p),"Safe buffer removal permitted");menu.setTab(202);for(int i=0;i<9;i++)check(!menu.getSlot(i).isActive(),"Internal tabs hide machine slot "+i);
    }
    static void ports(ServerLevel l){
        for(Direction facing:List.of(Direction.NORTH,Direction.EAST,Direction.SOUTH,Direction.WEST)){
            var f=place(l,TEST);l.setBlockAndUpdate(TEST,f.getBlockState().setValue(BioincubatorBlock.FACING,facing));tick(l,f,1);
            for(RelativeSide side:RelativeSide.values())for(SideMode m:List.of(SideMode.DISABLED,SideMode.INPUT,SideMode.OUTPUT)){
                mode(f,side,m);var d=side.resolve(facing);energy(f,1000);f.getInventory().setStackInSlot(0,ItemStack.EMPTY);f.getInventory().setStackInSlot(1,ItemStack.EMPTY);f.getInventory().setStackInSlot(4,filter());
                var items=f.getCapability(ForgeCapabilities.ITEM_HANDLER,d).orElse(null);var fe=f.getCapability(ForgeCapabilities.ENERGY,d).orElse(null);var fluid=f.getCapability(ForgeCapabilities.FLUID_HANDLER,d).orElse(null);
                boolean input=side!=RelativeSide.FRONT&&m==SideMode.INPUT,output=side!=RelativeSide.FRONT&&m==SideMode.OUTPUT;
                check((items!=null)==(input||output)&&(fe!=null)==input&&(fluid!=null)==input,"Capabilities "+facing+"/"+side+"/"+m);
                check(l.getBlockState(TEST).getValue(BioincubatorBlock.portProperty(d))==PortVisual.fromMode(f.sideMode(d)),"Visual agrees "+facing+"/"+side+"/"+m);
                if(input){
                    var water=new FluidStack(ModFluids.PURIFIED_WATER.get(),250);
                    int before=f.getDataAccess().get(2);check(fluid.fill(water,IFluidHandler.FluidAction.SIMULATE)==Math.min(250,6000-before)&&f.getDataAccess().get(2)==before,"Water simulation");
                    check(fluid.fill(new FluidStack(Fluids.WATER,250),IFluidHandler.FluidAction.EXECUTE)==0&&fluid.drain(100,IFluidHandler.FluidAction.EXECUTE).isEmpty(),"Purified-only input, no draining");fluid.fill(water,IFluidHandler.FluidAction.EXECUTE);
                    check(items.insertItem(0,filter(),true).isEmpty()&&f.getInventory().getStackInSlot(0).isEmpty(),"Capsule simulation");
                    check(items.insertItem(0,filter(),false).isEmpty()&&items.insertItem(1,media(1),false).isEmpty()&&items.extractItem(0,1,false).isEmpty()&&items.extractItem(4,1,false).isEmpty(),"Blue takes capsule and feed, never extracts");
                    check(!items.isItemValid(0,new ItemStack(Items.COAL))&&!items.isItemValid(1,filter())&&!items.isItemValid(4,filter()),"Input whitelist");
                    check(fe.receiveEnergy(1000,true)==128&&f.getDataAccess().get(0)==1000&&fe.extractEnergy(100,false)==0,"FE simulation and limit");
                    items.getStackInSlot(0).setCount(0);check(!f.getInventory().getStackInSlot(0).isEmpty(),"Item view cannot mutate inventory");
                    mode(f,side,SideMode.DISABLED);check(fluid.fill(water,IFluidHandler.FluidAction.EXECUTE)==0,"Cached fluid respects OFF");check(fe.receiveEnergy(128,false)==0&&!items.isItemValid(0,filter()),"Cached input respects OFF");
                }else if(output){
                    check(items.extractItem(0,1,false).isEmpty()&&!items.insertItem(0,filter(),false).isEmpty(),"Orange denies insertion and unrepaired capsule");
                    check(!items.extractItem(4,1,true).isEmpty()&&!f.getInventory().getStackInSlot(4).isEmpty(),"Output simulation");
                    check(!items.extractItem(4,1,false).isEmpty()&&f.getInventory().getStackInSlot(4).isEmpty(),"Orange extracts only repaired capsule");
                    f.getInventory().setStackInSlot(4,filter());mode(f,side,SideMode.DISABLED);check(items.extractItem(4,1,false).isEmpty(),"Cached output respects OFF");
                }
            }
            check(!f.getCapability(ForgeCapabilities.ENERGY,null).isPresent()&&!f.getCapability(ForgeCapabilities.ITEM_HANDLER,null).isPresent(),"Unsided automation denied");
            var n=f.saveWithoutMetadata();f.load(n);for(var side:RelativeSide.values())check(f.sideMode(side.resolve(facing))==SideMode.DISABLED,"Side settings survive save "+side);
        }
    }
    static void dismantle(ServerLevel l,ServerPlayer p){
        p.setGameMode(GameType.SURVIVAL);p.setShiftKeyDown(false);
        for(Item tool:new Item[]{Items.WOODEN_PICKAXE,Items.GOLDEN_PICKAXE,Items.STONE_PICKAXE,Items.IRON_PICKAXE,Items.DIAMOND_PICKAXE,Items.NETHERITE_PICKAXE}){
            var f=place(l,TEST);clear(l);f.getModules().setStackInSlot(0,new ItemStack(MachineModuleItems.BUFFER.get()));f.getInventory().setStackInSlot(1,media(3));
            p.setItemInHand(InteractionHand.MAIN_HAND,new ItemStack(tool));p.gameMode.destroyBlock(TEST);
            check(count(l,ModBlocks.BIOINCUBATOR.get().asItem())==(tool!=Items.WOODEN_PICKAXE&&tool!=Items.GOLDEN_PICKAXE?1:0),"Survival harvest tier "+tool);
            check(count(l,MachineModuleItems.BUFFER.get())==1&&count(l,media(1).getItem())==3,"Contents and modules drop once");
        }
        var f=place(l,TEST);clear(l);prepare(f);f.getModules().setStackInSlot(0,new ItemStack(MachineModuleItems.BUFFER.get()));energy(f,105000);tick(l,f,40);
        var wrench=ForgeRegistries.ITEMS.getValue(new ResourceLocation("domesurvival","machine_wrench"));p.getInventory().clearContent();p.setItemInHand(InteractionHand.MAIN_HAND,new ItemStack(wrench));
        var hit=new BlockHitResult(Vec3.atCenterOf(TEST),Direction.NORTH,TEST,false);mode(f,RelativeSide.TOP,SideMode.INPUT);mode(f,RelativeSide.LEFT,SideMode.OUTPUT);
        p.gameMode.useItemOn(p,l,p.getMainHandItem(),InteractionHand.MAIN_HAND,hit);check(f.getMachineFacing()==Direction.EAST&&f.sideMode(RelativeSide.LEFT.resolve(f.getMachineFacing()))==SideMode.OUTPUT,"Wrench rotates configured sides");
        var expected=f.saveWithoutMetadata();p.setShiftKeyDown(true);p.gameMode.useItemOn(p,l,p.getMainHandItem(),InteractionHand.MAIN_HAND,hit);
        check(l.isEmptyBlock(TEST)&&drops(l).size()==1&&p.getInventory().countItem(ModBlocks.BIOINCUBATOR.get().asItem())==0,"Wrench drops one machine in world");
        var entity=drops(l).get(0);var stack=entity.getItem().copy();var saved=stack.getTag().getCompound("BlockEntityTag");
        for(String key:List.of("Modules","Energy","Progress","CycleTicks","CycleEnergy","PurifiedWater","CycleCapsule","CycleMode","Inventory","UnifiedSideConfig"))check(saved.get(key).equals(expected.get(key)),"Portable data retained "+key);
        entity.playerTouch(p);check(entity.isAlive(),"Normal pickup delay");clear(l);p.setShiftKeyDown(false);p.setYRot(180);p.setItemInHand(InteractionHand.MAIN_HAND,stack);
        p.gameMode.useItemOn(p,l,p.getMainHandItem(),InteractionHand.MAIN_HAND,new BlockHitResult(Vec3.atCenterOf(TEST.below()).add(0,.5,0),Direction.UP,TEST.below(),false));
        f=(BioincubatorBlockEntity)l.getBlockEntity(TEST);check(f!=null&&f.getDataAccess().get(4)==40&&f.getDataAccess().get(0)==expected.getInt("Energy"),"Placement restores paid progress and FE");
        check(f.sideMode(RelativeSide.LEFT.resolve(f.getMachineFacing()))==SideMode.OUTPUT,"Placed sides follow new facing");
        p.setGameMode(GameType.ADVENTURE);p.setShiftKeyDown(true);p.setItemInHand(InteractionHand.MAIN_HAND,new ItemStack(wrench));p.gameMode.useItemOn(p,l,p.getMainHandItem(),InteractionHand.MAIN_HAND,hit);check(l.getBlockEntity(TEST)==f,"Adventure dismantle denied");p.setShiftKeyDown(false);p.setGameMode(GameType.CREATIVE);p.getInventory().clearContent();
    }
    @SubscribeEvent public static void server(TickEvent.ServerTickEvent e){
        if(!ENABLED||e.phase!=TickEvent.Phase.END||fixture)return;var mc=Minecraft.getInstance();var server=mc.getSingleplayerServer();if(server==null||server.getPlayerList().getPlayers().isEmpty())return;
        if(!mc.gameDirectory.toPath().toAbsolutePath().normalize().endsWith("bioincubator-review"))throw new IllegalStateException("Isolated directory required");
        fixture=true;try{
            var l=server.overworld();var p=server.getPlayerList().getPlayers().get(0);l.setDayTime(6000);l.getGameRules().getRule(GameRules.RULE_DAYLIGHT).set(false,server);l.getGameRules().getRule(GameRules.RULE_DOMOBSPAWNING).set(false,server);
            for(int x=-4;x<16;x++)for(int z=-4;z<16;z++)l.setBlockAndUpdate(new BlockPos(x,99,z),Blocks.SMOOTH_STONE.defaultBlockState());
            process(l,p);ports(l);dismantle(l,p);
            var f=place(l,DISPLAY);prepare(f);f.getModules().setStackInSlot(0,new ItemStack(MachineModuleItems.BUFFER.get()));energy(f,105000);f.getInventory().setStackInSlot(1,ItemStack.EMPTY);mode(f,RelativeSide.TOP,SideMode.DISABLED);
            l.setBlockAndUpdate(DISPLAY.west(),ItemPipeRegistry.STEEL_PIPE.get().defaultBlockState());
            p.teleportTo(l,1.6,100.3,-2.7,19,23);p.getAbilities().flying=true;p.onUpdateAbilities();p.getInventory().setItem(9,new ItemStack(MachineModuleItems.EFFICIENCY.get()));ready=true;
        }catch(Throwable ex){check(false,"Server exception "+ex);ex.printStackTrace();ready=true;}
    }
    static void add(int delay,Runnable action){steps.add(new Step(delay,action));}
    static void shot(String name){try(NativeImage img=Screenshot.takeScreenshot(Minecraft.getInstance().getMainRenderTarget())){Files.createDirectories(OUT);img.writeToFile(OUT.resolve(name+".png"));log("Screenshot "+name);}catch(Exception ex){check(false,"Screenshot "+ex);}}
    static BioincubatorScreen screen(){return (BioincubatorScreen)Minecraft.getInstance().screen;}
    static void tab(int id){var mc=Minecraft.getInstance();screen().getMenu().setTab(id);mc.gameMode.handleInventoryButtonClick(screen().getMenu().containerId,id);}
    static void otherGui(Block block){var mc=Minecraft.getInstance();mc.player.closeContainer();mc.setScreen(null);
        mc.getSingleplayerServer().execute(()->{var p=mc.getSingleplayerServer().getPlayerList().getPlayers().get(0);var l=p.serverLevel();var pos=DISPLAY.south(2);
            l.setBlockAndUpdate(pos,block.defaultBlockState());NetworkHooks.openScreen(p,(net.minecraft.world.MenuProvider)l.getBlockEntity(pos),pos);});
    }
    static void plan(){var mc=Minecraft.getInstance();mc.setScreen(null);mc.options.hideGui=true;mc.getTutorial().setStep(net.minecraft.client.tutorial.TutorialSteps.NONE);
        var animated=new BioincubatorBlockEntity(BlockPos.ZERO,ModBlocks.BIOINCUBATOR.get().defaultBlockState().setValue(BioincubatorBlock.LIT,true));BioincubatorBlockEntity.clientTick(mc.level,BlockPos.ZERO,animated.getBlockState(),animated);float phase=animated.animationPhase(1);check(phase>0,"Scanner moves when active");animated.setBlockState(animated.getBlockState().setValue(BioincubatorBlock.LIT,false));BioincubatorBlockEntity.clientTick(mc.level,BlockPos.ZERO,animated.getBlockState(),animated);check(animated.animationPhase(1)==phase,"Scanner stops while paused");
        add(40,()->shot("01_model"));
        add(1,()->{mc.options.hideGui=false;mc.getSingleplayerServer().execute(()->{var p=mc.getSingleplayerServer().getPlayerList().getPlayers().get(0);NetworkHooks.openScreen(p,(BioincubatorBlockEntity)p.serverLevel().getBlockEntity(DISPLAY),DISPLAY);});});
        add(90,()->{check(mc.screen instanceof BioincubatorScreen,"Networked GUI opens");check(screen().getMenu().getEnergyCapacity()==105000&&screen().getMenu().getEnergy()>100000,"32-bit FE buffer synchronized");shot("02_incubation");mc.getSingleplayerServer().execute(()->{var f=(BioincubatorBlockEntity)mc.getSingleplayerServer().overworld().getBlockEntity(DISPLAY);f.getInventory().setStackInSlot(1,media(16));});});
        add(90,()->{check(screen().getMenu().getStatus()==1,"Incubation starts");shot("03_working");mc.options.guiScale().set(3);mc.resizeDisplay();});
        add(60,()->{shot("04_scale3");mc.options.guiScale().set(2);mc.resizeDisplay();tab(202);});
        add(60,()->{shot("05_sides");mc.gameMode.handleInventoryButtonClick(screen().getMenu().containerId,100+RelativeSide.TOP.ordinal());});
        add(60,()->{check(screen().getMenu().getSideMode(RelativeSide.TOP)==SideMode.INPUT,"Side button synchronizes");tab(200);});
        add(60,()->{mc.gameMode.handleInventoryMouseClick(screen().getMenu().containerId,9,0,ClickType.QUICK_MOVE,mc.player);});
        add(60,()->{check(screen().getMenu().getSlot(8).hasItem(),"Shift-click installs compatible module");shot("06_modules");tab(201);mc.getSingleplayerServer().execute(()->{var f=(BioincubatorBlockEntity)mc.getSingleplayerServer().overworld().getBlockEntity(DISPLAY);repair(f);});});
        add(90,()->{check(screen().getMenu().getMode()==1&&screen().getMenu().getStatus()==1,"Repair mode and slots synchronized");shot("07_repair");mc.options.guiScale().set(3);mc.resizeDisplay();});
        add(60,()->{shot("08_repair_scale3");mc.options.guiScale().set(2);mc.resizeDisplay();mc.player.closeContainer();var runtime=JeiCapture.runtime;check(runtime!=null,"JEI available");
            var manager=runtime.getRecipeManager();int species=BioLootData.allSpecies().size();check(manager.createRecipeLookup(DomeSurvivalJeiPlugin.BIO_REPAIR).get().count()==species,"One repair JEI recipe per species");check(manager.createRecipeLookup(DomeSurvivalJeiPlugin.BIO_INCUBATION).get().count()==species,"One incubation JEI recipe per species");runtime.getRecipesGui().showTypes(List.of(DomeSurvivalJeiPlugin.BIO_REPAIR));});
        add(90,()->shot("09_jei_repair"));add(100,()->{shot("10_jei_repair_motion");JeiCapture.runtime.getRecipesGui().showTypes(List.of(DomeSurvivalJeiPlugin.BIO_INCUBATION));});add(90,()->shot("11_jei_incubation"));
        add(10,()->{log("RESULT "+(failures==0?"PASS":"FAIL")+" failures="+failures);mc.stop();});
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
        if(!started&&mc.screen instanceof TitleScreen&&mc.getOverlay()==null){started=true;mc.options.pauseOnLostFocus=false;mc.options.fov().set(50);mc.options.guiScale().set(2);mc.options.renderDistance().set(6);mc.options.enableVsync().set(false);mc.options.framerateLimit().set(120);mc.getLanguageManager().setSelected("ru_ru");mc.options.languageCode="ru_ru";mc.reloadResourcePacks();mc.createWorldOpenFlows().createFreshLevel("incubator_review_"+System.currentTimeMillis(),new LevelSettings("Electrolyzer review",GameType.CREATIVE,false,Difficulty.PEACEFUL,true,new GameRules(),WorldDataConfiguration.DEFAULT),new WorldOptions(63L,false,false),a->a.registryOrThrow(Registries.WORLD_PRESET).getHolderOrThrow(WorldPresets.FLAT).value().createWorldDimensions());return;}
        if(!ready||mc.player==null||mc.level==null||mc.getOverlay()!=null)return;ticks++;
        if(!planned&&ticks>100){planned=true;ticks=0;plan();}
    }
    @SubscribeEvent public static void render(TickEvent.RenderTickEvent e){
        if(!ENABLED||!planned||e.phase!=TickEvent.Phase.END||step>=steps.size())return;
        if(++frames>=steps.get(step).delay()){
            frames=0;try{steps.get(step++).action().run();}catch(Throwable ex){check(false,"Client exception "+ex);ex.printStackTrace();log("RESULT FAIL");Minecraft.getInstance().stop();}
        }
    }
}
