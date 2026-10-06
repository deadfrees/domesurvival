package com.wasted.domesurvival.organicprobe;

import com.mojang.blaze3d.platform.NativeImage;
import com.wasted.domesurvival.forge.block.ModBlocks;
import com.wasted.domesurvival.forge.item.ModItems;
import com.wasted.domesurvival.forge.item.OxygenTankItem;
import com.wasted.domesurvival.forge.oxygen.room.*;
import com.wasted.domesurvival.forge.itempipe.ItemPipeBlock;
import com.wasted.domesurvival.forge.itempipe.ItemPipeRegistry;
import com.wasted.domesurvival.forge.machine.organic.*;
import com.wasted.domesurvival.forge.machine.module.*;
import com.wasted.domesurvival.forge.machine.side.*;
import com.wasted.domesurvival.forge.machine.organic.OrganicProcessorScreen;
import com.wasted.domesurvival.forge.client.jei.ProcessingMachinesJeiPlugin;
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
public final class OrganicProcessorProbe {
    static final boolean ENABLED=Boolean.getBoolean("dome.organicProcessorReview");
    static final Path OUT=Path.of("../../dev/organic_processor_v2/runtime").toAbsolutePath().normalize();
    static final BlockPos DISPLAY=new BlockPos(0,100,0),TEST=new BlockPos(10,100,10);
    static boolean started,fixture,planned;static volatile boolean ready;static int ticks,step,failures,frames;
    static double hoverX,hoverY;
    static void hover(int x,int y){var w=Minecraft.getInstance().getWindow();hoverX=((w.getGuiScaledWidth()-220)/2+x)*w.getGuiScale();hoverY=((w.getGuiScaledHeight()-266)/2+y)*w.getGuiScale();}
    record Step(int delay,Runnable action){}static final List<Step> steps=new ArrayList<>();
    static synchronized void log(String s){System.out.println("[ORGANIC_REVIEW] "+s);try{Files.createDirectories(OUT);Files.writeString(OUT.resolve("checks.txt"),s+"\n",StandardOpenOption.CREATE,StandardOpenOption.APPEND);}catch(Exception ex){throw new RuntimeException(ex);}}
    static void check(boolean ok,String s){if(!ok)failures++;log((ok?"PASS ":"FAIL ")+s);}
    static OrganicProcessorBlockEntity place(ServerLevel l,BlockPos p){l.setBlockAndUpdate(p,Blocks.AIR.defaultBlockState());l.setBlockAndUpdate(p,OrganicProcessorRegistry.ORGANIC_PROCESSOR.get().defaultBlockState());return (OrganicProcessorBlockEntity)l.getBlockEntity(p);}
    static void tick(ServerLevel l,OrganicProcessorBlockEntity f,int n){for(int i=0;i<n;i++)OrganicProcessorBlockEntity.serverTick(l,f.getBlockPos(),f.getBlockState(),f);}
    static void mode(OrganicProcessorBlockEntity f,RelativeSide s,SideMode m){for(int i=0;i<3&&f.sideMode(s.resolve(f.getMachineFacing()))!=m;i++)f.cycleSideMode(s);}
    static List<ItemEntity> drops(ServerLevel l){return l.getEntitiesOfClass(ItemEntity.class,new AABB(TEST).inflate(2));}
    static void clear(ServerLevel l){drops(l).forEach(ItemEntity::discard);}
    static int count(ServerLevel l,Item i){return drops(l).stream().filter(e->e.getItem().is(i)).mapToInt(e->e.getItem().getCount()).sum();}
    static void energy(OrganicProcessorBlockEntity f,int amount){var n=f.saveWithoutMetadata();n.putInt("Energy",amount);f.load(n);}

    static ItemStack filter(){return new ItemStack(Items.WHEAT,1);}
    static ItemStack media(int n){return new ItemStack(Items.BONE_MEAL,n);}
    static void prepare(OrganicProcessorBlockEntity f){f.getInventory().setStackInSlot(0,new ItemStack(Items.WHEAT,32));f.getInventory().setStackInSlot(1,media(16));energy(f,50000);f.getCapability(ForgeCapabilities.FLUID_HANDLER,Direction.UP).orElseThrow(()->new IllegalStateException("water port")).fill(new FluidStack(ModFluids.PURIFIED_WATER.get(),4000),IFluidHandler.FluidAction.EXECUTE);}
    static void process(ServerLevel l,ServerPlayer p){
        check(!OrganicProcessorRegistry.ORGANIC_PROCESSOR.get().defaultBlockState().canOcclude(),"Recessed shell does not hide adjacent faces");
        var recipes=l.getRecipeManager().getAllRecipesFor(com.wasted.domesurvival.forge.recipe.ModRecipes.ORGANIC_PROCESSOR_TYPE.get());check(recipes.size()==3,"Three unchanged biosynthesis recipes");
        for(var r:recipes)for(int variant=0;variant<3;variant++){
            var f=place(l,TEST);prepare(f);
            var first=r.getPrimary().getItems()[0].copy();first.setCount(r.getPrimaryCount());var second=r.getAdditive().getItems()[0].copy();second.setCount(r.getAdditiveCount());f.getInventory().setStackInSlot(0,first);f.getInventory().setStackInSlot(1,second);
            if(variant>0)f.getModules().setStackInSlot(0,new ItemStack((variant==1?MachineModuleItems.EFFICIENCY:MachineModuleItems.OVERDRIVE).get()));
            int duration=f.requiredTicks(),cost=f.getDataAccess().get(7);tick(l,f,duration-1);check(f.getInventory().getStackInSlot(2).isEmpty(),"No early output "+r.getId()+"/"+variant);tick(l,f,1);
            check(ItemStack.matches(f.getInventory().getStackInSlot(2),r.getResult()),"Correct product "+r.getId()+"/"+variant);
            check(f.getDataAccess().get(0)==50000-cost&&f.getDataAccess().get(2)==4000-r.getWaterMb()&&f.getInventory().getStackInSlot(0).isEmpty()&&f.getInventory().getStackInSlot(1).isEmpty(),"Exact resources "+r.getId()+"/"+variant);
        }
        var f=place(l,TEST);prepare(f);tick(l,f,40);f.getModules().setStackInSlot(0,new ItemStack(MachineModuleItems.EFFICIENCY.get()));
        check(f.progressTicks()==40&&f.requiredTicks()==140&&f.getDataAccess().get(7)==3600,"Module change preserves paid cycle");var saved=f.saveWithoutMetadata();f.load(saved);tick(l,f,100);
        check(f.getDataAccess().get(0)==46400&&f.getDataAccess().get(2)==3750&&f.requiredTicks()==156&&f.getDataAccess().get(7)==2880,"Saved cycle finishes at old cost; next uses efficiency");tick(l,f,156);check(f.getDataAccess().get(0)==43520&&f.getInventory().getStackInSlot(2).getCount()==2,"Next cycle uses module cost exactly");
        check(!f.getModules().isItemValid(1,new ItemStack(MachineModuleItems.EFFICIENCY.get()))&&!f.getModules().isItemValid(1,new ItemStack(MachineModuleItems.OVERDRIVE.get())),"Duplicate and conflicting modules denied");
        f=place(l,TEST);prepare(f);tick(l,f,30);energy(f,0);tick(l,f,20);check(f.progressTicks()==30&&!f.getBlockState().getValue(OrganicProcessorBlock.ACTIVE),"No energy pauses rotor and paid cycle");
        energy(f,50000);saved=f.saveWithoutMetadata();saved.put("PurifiedWater",new FluidStack(ModFluids.PURIFIED_WATER.get(),100).writeToNBT(new net.minecraft.nbt.CompoundTag()));f.load(saved);tick(l,f,20);check(f.progressTicks()==30&&f.getDataAccess().get(0)==50000,"Insufficient water pauses without consumption");
        f.getCapability(ForgeCapabilities.FLUID_HANDLER,Direction.UP).orElseThrow(()->new IllegalStateException()).fill(new FluidStack(ModFluids.PURIFIED_WATER.get(),4000),IFluidHandler.FluidAction.EXECUTE);
        f.getInventory().setStackInSlot(2,new ItemStack(Items.STONE,64));tick(l,f,20);check(f.progressTicks()==30&&f.getDataAccess().get(0)==50000,"Blocked output pauses without consumption");f.getInventory().setStackInSlot(2,ItemStack.EMPTY);tick(l,f,1);check(f.progressTicks()==31,"Unblocked cycle resumes");
        saved=f.saveWithoutMetadata();saved.remove("CycleTicks");saved.remove("CycleEnergy");saved.remove("PortFacing");f.load(saved);tick(l,f,109);check(f.progressTicks()==0&&f.getInventory().getStackInSlot(2).getCount()==1,"Legacy NBT cycle completes once");
        f=place(l,TEST);prepare(f);f.getModules().setStackInSlot(0,new ItemStack(MachineModuleItems.BUFFER.get()));energy(f,87500);var menu=new OrganicProcessorMenu(1,p.getInventory(),f);menu.setTab(200);
        check(menu.energyCapacity()==87500&&!menu.getSlot(3).mayPickup(p)&&menu.quickMoveStack(p,3).isEmpty(),"Full buffer removal blocked");energy(f,50000);check(menu.getSlot(3).mayPickup(p),"Safe buffer removal permitted");menu.setTab(202);for(int i=0;i<5;i++)check(!menu.getSlot(i).isActive(),"Hidden machine slot "+i);
        f=place(l,TEST);prepare(f);f.getInventory().setStackInSlot(0,ItemStack.EMPTY);tick(l,f,100);check(f.getDataAccess().get(0)==50000&&f.getDataAccess().get(2)==4000,"Missing ingredient consumes nothing");
    }
    static void ports(ServerLevel l){
        for(Direction facing:List.of(Direction.NORTH,Direction.EAST,Direction.SOUTH,Direction.WEST)){
            var f=place(l,TEST);l.setBlockAndUpdate(TEST,f.getBlockState().setValue(OrganicProcessorBlock.FACING,facing));tick(l,f,1);
            for(RelativeSide side:RelativeSide.values())for(SideMode m:List.of(SideMode.DISABLED,SideMode.INPUT,SideMode.OUTPUT)){
                mode(f,side,m);var d=side.resolve(facing);energy(f,1000);f.getInventory().setStackInSlot(0,ItemStack.EMPTY);f.getInventory().setStackInSlot(1,ItemStack.EMPTY);f.getInventory().setStackInSlot(2,filter());
                var items=f.getCapability(ForgeCapabilities.ITEM_HANDLER,d).orElse(null);var fe=f.getCapability(ForgeCapabilities.ENERGY,d).orElse(null);var fluid=f.getCapability(ForgeCapabilities.FLUID_HANDLER,d).orElse(null);
                boolean input=side!=RelativeSide.FRONT&&m==SideMode.INPUT,output=side!=RelativeSide.FRONT&&m==SideMode.OUTPUT;
                check((items!=null)==(input||output)&&(fe!=null)==input&&(fluid!=null)==input,"Capabilities "+facing+"/"+side+"/"+m);
                check(l.getBlockState(TEST).getValue(OrganicProcessorBlock.portProperty(d))==PortVisual.fromMode(f.sideMode(d)),"Visual agrees "+facing+"/"+side+"/"+m);
                if(input){
                    var water=new FluidStack(ModFluids.PURIFIED_WATER.get(),250);
                    int before=f.getDataAccess().get(2);check(fluid.fill(water,IFluidHandler.FluidAction.SIMULATE)==Math.min(250,4000-before)&&f.getDataAccess().get(2)==before,"Water simulation");
                    check(fluid.fill(new FluidStack(Fluids.WATER,250),IFluidHandler.FluidAction.EXECUTE)==0&&fluid.drain(100,IFluidHandler.FluidAction.EXECUTE).isEmpty(),"Purified-only input, no draining");fluid.fill(water,IFluidHandler.FluidAction.EXECUTE);
                    check(items.insertItem(0,filter(),true).isEmpty()&&f.getInventory().getStackInSlot(0).isEmpty(),"Filter simulation");
                    check(items.insertItem(0,filter(),false).isEmpty()&&items.insertItem(1,media(1),false).isEmpty()&&items.extractItem(0,1,false).isEmpty()&&items.extractItem(2,1,false).isEmpty(),"Blue takes filter and media, never extracts");
                    check(!items.isItemValid(0,new ItemStack(Items.COAL))&&!items.isItemValid(1,filter())&&!items.isItemValid(2,filter()),"Input whitelist");
                    check(fe.receiveEnergy(1000,true)==256&&f.getDataAccess().get(0)==1000&&fe.extractEnergy(100,false)==0,"FE simulation and limit");
                    items.getStackInSlot(0).setCount(0);check(!f.getInventory().getStackInSlot(0).isEmpty(),"Item view cannot mutate inventory");
                    mode(f,side,SideMode.DISABLED);check(fluid.fill(water,IFluidHandler.FluidAction.EXECUTE)==0,"Cached fluid respects OFF");check(fe.receiveEnergy(256,false)==0&&!items.isItemValid(0,filter()),"Cached input respects OFF");
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
        for(Item tool:new Item[]{Items.WOODEN_PICKAXE,Items.GOLDEN_PICKAXE,Items.STONE_PICKAXE,Items.IRON_PICKAXE,Items.DIAMOND_PICKAXE,Items.NETHERITE_PICKAXE}){
            var f=place(l,TEST);clear(l);f.getModules().setStackInSlot(0,new ItemStack(MachineModuleItems.BUFFER.get()));f.getInventory().setStackInSlot(1,media(3));
            p.setItemInHand(InteractionHand.MAIN_HAND,new ItemStack(tool));p.gameMode.destroyBlock(TEST);
            check(count(l,OrganicProcessorRegistry.ORGANIC_PROCESSOR.get().asItem())==(tool!=Items.WOODEN_PICKAXE&&tool!=Items.GOLDEN_PICKAXE?1:0),"Survival harvest tier "+tool);
            check(count(l,MachineModuleItems.BUFFER.get())==1&&count(l,media(1).getItem())==3,"Contents and modules drop once");
        }
        var f=place(l,TEST);clear(l);prepare(f);f.getModules().setStackInSlot(0,new ItemStack(MachineModuleItems.BUFFER.get()));energy(f,87500);tick(l,f,40);
        var wrench=ForgeRegistries.ITEMS.getValue(new ResourceLocation("domesurvival","machine_wrench"));p.getInventory().clearContent();p.setItemInHand(InteractionHand.MAIN_HAND,new ItemStack(wrench));
        var hit=new BlockHitResult(Vec3.atCenterOf(TEST),Direction.NORTH,TEST,false);mode(f,RelativeSide.TOP,SideMode.INPUT);mode(f,RelativeSide.LEFT,SideMode.OUTPUT);
        p.gameMode.useItemOn(p,l,p.getMainHandItem(),InteractionHand.MAIN_HAND,hit);check(f.getMachineFacing()==Direction.EAST&&f.sideMode(RelativeSide.LEFT.resolve(f.getMachineFacing()))==SideMode.OUTPUT,"Wrench rotates configured sides");
        var expected=f.saveWithoutMetadata();p.setShiftKeyDown(true);p.gameMode.useItemOn(p,l,p.getMainHandItem(),InteractionHand.MAIN_HAND,hit);
        check(l.isEmptyBlock(TEST)&&drops(l).size()==1&&p.getInventory().countItem(OrganicProcessorRegistry.ORGANIC_PROCESSOR.get().asItem())==0,"Wrench drops one machine in world");
        var entity=drops(l).get(0);var stack=entity.getItem().copy();var saved=stack.getTag().getCompound("BlockEntityTag");
        for(String key:List.of("Modules","Energy","Progress","CycleTicks","CycleEnergy","PurifiedWater","Inventory","UnifiedSideConfig"))check(saved.get(key).equals(expected.get(key)),"Portable data retained "+key);
        entity.playerTouch(p);check(entity.isAlive(),"Normal pickup delay");clear(l);p.setShiftKeyDown(false);p.setYRot(180);p.setItemInHand(InteractionHand.MAIN_HAND,stack);
        p.gameMode.useItemOn(p,l,p.getMainHandItem(),InteractionHand.MAIN_HAND,new BlockHitResult(Vec3.atCenterOf(TEST.below()).add(0,.5,0),Direction.UP,TEST.below(),false));
        f=(OrganicProcessorBlockEntity)l.getBlockEntity(TEST);check(f!=null&&f.progressTicks()==40&&f.getDataAccess().get(0)==expected.getInt("Energy"),"Placement restores paid progress and FE");
        check(f.sideMode(RelativeSide.LEFT.resolve(f.getMachineFacing()))==SideMode.OUTPUT,"Placed sides follow new facing");
        p.setGameMode(GameType.ADVENTURE);p.setShiftKeyDown(true);p.setItemInHand(InteractionHand.MAIN_HAND,new ItemStack(wrench));p.gameMode.useItemOn(p,l,p.getMainHandItem(),InteractionHand.MAIN_HAND,hit);check(l.getBlockEntity(TEST)==f,"Adventure dismantle denied");p.setShiftKeyDown(false);p.setGameMode(GameType.CREATIVE);p.getInventory().clearContent();
    }
    @SubscribeEvent public static void server(TickEvent.ServerTickEvent e){
        if(!ENABLED||e.phase!=TickEvent.Phase.END||fixture)return;var mc=Minecraft.getInstance();var server=mc.getSingleplayerServer();if(server==null||server.getPlayerList().getPlayers().isEmpty())return;
        if(!mc.gameDirectory.toPath().toAbsolutePath().normalize().endsWith("organic-processor-review"))throw new IllegalStateException("Isolated directory required");
        fixture=true;try{
            var l=server.overworld();var p=server.getPlayerList().getPlayers().get(0);l.setDayTime(6000);l.getGameRules().getRule(GameRules.RULE_DAYLIGHT).set(false,server);l.getGameRules().getRule(GameRules.RULE_DOMOBSPAWNING).set(false,server);
            for(int x=-4;x<16;x++)for(int z=-4;z<16;z++)l.setBlockAndUpdate(new BlockPos(x,99,z),Blocks.SMOOTH_STONE.defaultBlockState());
            process(l,p);ports(l);dismantle(l,p);
            var f=place(l,DISPLAY);prepare(f);f.getModules().setStackInSlot(0,new ItemStack(MachineModuleItems.BUFFER.get()));energy(f,87500);f.getInventory().setStackInSlot(1,ItemStack.EMPTY);mode(f,RelativeSide.TOP,SideMode.DISABLED);
            l.setBlockAndUpdate(DISPLAY.west(),ItemPipeRegistry.STEEL_PIPE.get().defaultBlockState());
            p.teleportTo(l,1.6,100.3,-2.7,19,23);p.getAbilities().flying=true;p.onUpdateAbilities();p.getInventory().setItem(9,new ItemStack(MachineModuleItems.EFFICIENCY.get()));ready=true;
        }catch(Throwable ex){check(false,"Server exception "+ex);ex.printStackTrace();ready=true;}
    }
    static void add(int delay,Runnable action){steps.add(new Step(delay,action));}
    static void shot(String name){try(NativeImage img=Screenshot.takeScreenshot(Minecraft.getInstance().getMainRenderTarget())){Files.createDirectories(OUT);img.writeToFile(OUT.resolve(name+".png"));log("Screenshot "+name);}catch(Exception ex){check(false,"Screenshot "+ex);}}
    static OrganicProcessorScreen screen(){return (OrganicProcessorScreen)Minecraft.getInstance().screen;}
    static void tab(int id){var mc=Minecraft.getInstance();screen().getMenu().setTab(id);mc.gameMode.handleInventoryButtonClick(screen().getMenu().containerId,id);}
    static void otherGui(Block block){var mc=Minecraft.getInstance();mc.player.closeContainer();mc.setScreen(null);
        mc.getSingleplayerServer().execute(()->{var p=mc.getSingleplayerServer().getPlayerList().getPlayers().get(0);var l=p.serverLevel();var pos=DISPLAY.south(2);
            l.setBlockAndUpdate(pos,block.defaultBlockState());NetworkHooks.openScreen(p,(net.minecraft.world.MenuProvider)l.getBlockEntity(pos),pos);});
    }
    static void plan(){var mc=Minecraft.getInstance();
        
        check(mc.font.width(net.minecraft.network.chat.Component.translatable("gui.domesurvival.organic_processor_v2.input_short"))<=90,"Full input caption fits");mc.setScreen(null);mc.options.hideGui=true;mc.getTutorial().setStep(net.minecraft.client.tutorial.TutorialSteps.NONE);
        var animated=new OrganicProcessorBlockEntity(BlockPos.ZERO,OrganicProcessorRegistry.ORGANIC_PROCESSOR.get().defaultBlockState().setValue(OrganicProcessorBlock.ACTIVE,true));OrganicProcessorBlockEntity.clientTick(mc.level,BlockPos.ZERO,animated.getBlockState(),animated);float angle=animated.animationPhase(1);check(angle>0,"Cleaning carriage moves when active");animated.setBlockState(animated.getBlockState().setValue(OrganicProcessorBlock.ACTIVE,false));OrganicProcessorBlockEntity.clientTick(mc.level,BlockPos.ZERO,animated.getBlockState(),animated);check(animated.animationPhase(1)==angle,"Cleaning carriage freezes while paused");
        check(ItemPipeBlock.refreshConnections(mc.level,DISPLAY.west(),mc.level.getBlockState(DISPLAY.west())).getValue(ItemPipeBlock.EAST),"Client blue item connection");

        add(60,()->shot("01_existing_model"));
        add(45,()->{mc.options.hideGui=false;mc.getSingleplayerServer().execute(()->{var p=mc.getSingleplayerServer().getPlayerList().getPlayers().get(0);NetworkHooks.openScreen(p,(OrganicProcessorBlockEntity)p.serverLevel().getBlockEntity(DISPLAY),DISPLAY);});});
        add(90,()->{check(mc.screen instanceof OrganicProcessorScreen,"Networked regenerator GUI opens");check(screen().getMenu().energyCapacity()==87500&&screen().getMenu().energyStored()>85000,"Expanded FE buffer synchronized");shot("02_main_gui");mc.options.guiScale().set(3);mc.resizeDisplay();});
        add(1,()->mc.getSingleplayerServer().execute(()->{var f=(OrganicProcessorBlockEntity)mc.getSingleplayerServer().overworld().getBlockEntity(DISPLAY);f.getInventory().setStackInSlot(1,media(8));}));
        add(90,()->{check(screen().getMenu().status()==1,"Networked regeneration status is active");check(mc.level.getBlockState(DISPLAY).getValue(OrganicProcessorBlock.ACTIVE),"Client working state synchronized");shot("02_working_gui");});
        add(40,()->shot("02_carriage_motion"));
        add(60,()->{shot("03_main_scale3");mc.options.guiScale().set(2);mc.resizeDisplay();hover(20,70);});
        add(45,()->{shot("03_energy_tooltip");hoverX=hoverY=0;tab(202);});
        add(60,()->{shot("04_sides");mc.gameMode.handleInventoryButtonClick(screen().getMenu().containerId,100+RelativeSide.TOP.ordinal());});
        add(60,()->{check(screen().getMenu().getSideMode(RelativeSide.TOP)==SideMode.INPUT,"Networked side button updates menu");check(((OrganicProcessorBlockEntity)mc.level.getBlockEntity(DISPLAY)).sideMode(Direction.UP)==SideMode.INPUT,"Client receives logical side update");tab(200);});
        add(60,()->{shot("05_modules_empty");mc.gameMode.handleInventoryMouseClick(screen().getMenu().containerId,5,0,ClickType.QUICK_MOVE,mc.player);});
        add(60,()->{check(screen().getMenu().getSlot(4).hasItem(),"Networked Shift-click installs efficiency while working");shot("06_modules_installed");mc.options.guiScale().set(3);mc.resizeDisplay();});
        add(60,()->{shot("07_module_scale3");mc.options.guiScale().set(2);mc.resizeDisplay();hover(28,75);});
        add(45,()->{shot("07_module_help");hoverX=hoverY=0;mc.player.closeContainer();
            var runtime=JeiCapture.runtime;check(runtime!=null,"JEI runtime available");var manager=runtime.getRecipeManager();
            check(manager.createRecipeCategoryLookup().get().filter(c->c.getRecipeType().getUid().equals(ProcessingMachinesJeiPlugin.ORGANIC_PROCESSOR.getUid())).count()==1,"Exactly one regenerator JEI category");
            check(manager.createRecipeLookup(ProcessingMachinesJeiPlugin.ORGANIC_PROCESSOR).get().count()==3,"Exactly three JEI recipes, no duplicates");
            runtime.getRecipesGui().showTypes(List.of(ProcessingMachinesJeiPlugin.ORGANIC_PROCESSOR));});

        add(90,()->shot("08_jei"));add(100,()->shot("09_jei_progress"));
        add(45,()->{mc.setScreen(null);log("RESULT "+(failures==0?"PASS":"FAIL")+" failures="+failures);mc.stop();});
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
        if(!started&&mc.screen instanceof TitleScreen&&mc.getOverlay()==null){started=true;mc.options.pauseOnLostFocus=false;mc.options.fov().set(50);mc.options.guiScale().set(2);mc.options.renderDistance().set(6);mc.options.enableVsync().set(false);mc.options.framerateLimit().set(120);mc.getLanguageManager().setSelected("ru_ru");mc.options.languageCode="ru_ru";mc.reloadResourcePacks();mc.createWorldOpenFlows().createFreshLevel("organic_review_"+System.currentTimeMillis(),new LevelSettings("Electrolyzer review",GameType.CREATIVE,false,Difficulty.PEACEFUL,true,new GameRules(),WorldDataConfiguration.DEFAULT),new WorldOptions(63L,false,false),a->a.registryOrThrow(Registries.WORLD_PRESET).getHolderOrThrow(WorldPresets.FLAT).value().createWorldDimensions());return;}
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
