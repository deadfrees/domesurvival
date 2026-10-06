package com.wasted.domesurvival.steelbufferprobe;

import com.mojang.blaze3d.platform.NativeImage;
import com.wasted.domesurvival.forge.block.ModBlocks;
import com.wasted.domesurvival.forge.machine.energy.*;
import com.wasted.domesurvival.forge.machine.side.*;
import com.wasted.domesurvival.forge.client.screen.EnergyBufferScreen;
import net.minecraft.client.*;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraft.core.*;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.*;
import net.minecraft.world.*;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.*;
import net.minecraft.world.level.*;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.levelgen.WorldOptions;
import net.minecraft.world.level.levelgen.presets.WorldPresets;
import net.minecraft.world.phys.*;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.energy.IEnergyStorage;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.network.NetworkHooks;
import net.minecraftforge.registries.ForgeRegistries;
import java.nio.file.*;
import java.util.*;

@Mod.EventBusSubscriber(modid="domesurvival",value=Dist.CLIENT)
public final class SteelBufferProbe {
    static final boolean ENABLED=Boolean.getBoolean("dome.steelBufferReview");
    static final Path OUT=Path.of("../../dev/steel_buffer_v2/runtime").toAbsolutePath().normalize();
    static final BlockPos DISPLAY=new BlockPos(0,100,0), TEST=new BlockPos(12,100,12);
    static boolean started,fixture,planned;static volatile boolean ready;static int ticks,step,frames,failures;
    record Step(int delay,Runnable action){}static final List<Step> steps=new ArrayList<>();
    static synchronized void log(String s){System.out.println("[STEEL_REVIEW] "+s);try{Files.createDirectories(OUT);Files.writeString(OUT.resolve("checks.txt"),s+"\n",StandardOpenOption.CREATE,StandardOpenOption.APPEND);}catch(Exception e){throw new RuntimeException(e);}}
    static void check(boolean ok,String text){if(!ok)failures++;log((ok?"PASS ":"FAIL ")+text);}
    static EnergyBufferBlockEntity place(ServerLevel l,BlockPos pos){l.setBlockAndUpdate(pos,Blocks.AIR.defaultBlockState());l.setBlockAndUpdate(pos,ModBlocks.ENERGY_BUFFER.get().defaultBlockState());return (EnergyBufferBlockEntity)l.getBlockEntity(pos);}
    static void mode(EnergyBufferBlockEntity f,RelativeSide side,SideMode mode){for(int i=0;i<3&&f.sideMode(side.resolve(f.getMachineFacing()))!=mode;i++)f.cycleSideMode(side);}
    static void off(EnergyBufferBlockEntity f){for(var side:RelativeSide.values())mode(f,side,SideMode.DISABLED);}
    static IEnergyStorage cap(EnergyBufferBlockEntity f,Direction side){return f.getCapability(ForgeCapabilities.ENERGY,side).orElseThrow(()->new IllegalStateException("Missing FE port "+side));}
    static void energy(EnergyBufferBlockEntity f,int amount){var n=f.saveWithoutMetadata();n.putInt("Energy",amount);f.load(n);}
    static void next(ServerLevel l){((net.minecraft.world.level.storage.ServerLevelData)l.getLevelData()).setGameTime(l.getGameTime()+1);}
    static void tick(ServerLevel l,EnergyBufferBlockEntity f){EnergyBufferBlockEntity.serverTick(l,f.getBlockPos(),f.getBlockState(),f);}
    static ItemStack cell(){return new ItemStack(ForgeRegistries.ITEMS.getValue(new ResourceLocation("domesurvival","neosteel_energy_cell")));}
    static List<ItemEntity> drops(ServerLevel l){return l.getEntitiesOfClass(ItemEntity.class,new AABB(TEST).inflate(2));}
    static void clear(ServerLevel l){drops(l).forEach(ItemEntity::discard);}
    static long count(ServerLevel l,Item item){return drops(l).stream().filter(e->e.getItem().is(item)).mapToLong(e->e.getItem().getCount()).sum();}
    static void storage(ServerLevel l){
        check(!ModBlocks.ENERGY_BUFFER.get().defaultBlockState().canOcclude(),"Recessed case preserves neighboring faces");
        for(int enchant=0;enchant<=4;enchant++){
            var f=place(l,TEST);off(f);f.setCapacityEnchantLevel(enchant);int capacity=250000*(2+enchant)/2;
            check(f.getEnergyCapacity()==capacity,"Capacity enchant "+enchant);
            for(int i=0;i<=4;i++){energy(f,capacity*i/4);tick(l,f);check(l.getBlockState(TEST).getValue(EnergyBufferBlock.ENERGY_LEVEL)==i,"World indicator "+enchant+"/"+i);check(ModBlocks.ENERGY_BUFFER.get().getAnalogOutputSignal(f.getBlockState(),l,TEST)==(i==0?0:1+(capacity*i/4)*14/capacity),"Comparator "+enchant+"/"+i);}
            var saved=f.saveWithoutMetadata();var restored=new EnergyBufferBlockEntity(TEST,f.getBlockState());restored.load(saved);
            check(restored.getEnergyStored()==capacity&&restored.getEnergyCapacity()==capacity&&restored.getCapacityEnchantLevel()==enchant,"Full enchanted NBT "+enchant);
            for(var side:RelativeSide.values())check(restored.sideMode(side.resolve(restored.getMachineFacing()))==SideMode.DISABLED,"All OFF persists "+enchant+"/"+side);
        }
        var f=place(l,TEST);off(f);f.setCapacityEnchantLevel(4);energy(f,700000);f.setCapacityEnchantLevel(0);check(f.getEnergyStored()==700000,"Forced capacity reduction does not delete FE");
        var old=new net.minecraft.nbt.CompoundTag();old.putInt("Energy",120000);old.putInt("ModeUp",1);old.putInt("ModeEast",2);f.load(old);
        check(f.getEnergyStored()==120000&&f.sideMode(Direction.UP)==SideMode.INPUT&&f.sideMode(Direction.EAST)==SideMode.OUTPUT,"Legacy side and energy migration");
    }
    static void ports(ServerLevel l){
        for(var facing:Direction.Plane.HORIZONTAL){
            var f=place(l,TEST);l.setBlockAndUpdate(TEST,f.getBlockState().setValue(EnergyBufferBlock.FACING,facing));f.rotateSideConfiguration(Direction.NORTH);off(f);
            for(var side:RelativeSide.values())for(var mode:List.of(SideMode.DISABLED,SideMode.INPUT,SideMode.OUTPUT)){
                mode(f,side,mode);next(l);energy(f,10000);Direction d=side.resolve(facing);boolean enabled=side!=RelativeSide.FRONT&&mode!=SideMode.DISABLED;
                var optional=f.getCapability(ForgeCapabilities.ENERGY,d);String label=facing+"/"+side+"/"+mode;
                check(optional.isPresent()==enabled,"Port availability "+label);
                check(l.getBlockState(TEST).getValue(EnergyBufferBlock.portProperty(d))==(enabled?PortVisual.fromMode(mode):PortVisual.OFF),"Port visual "+label);
                if(enabled){
                    var c=optional.orElseThrow(()->new IllegalStateException());int before=f.getEnergyStored();
                    check(c.receiveEnergy(9999,true)==(mode==SideMode.INPUT?256:0)&&c.extractEnergy(9999,true)==(mode==SideMode.OUTPUT?256:0)&&f.getEnergyStored()==before,"Simulation and direction "+label);
                    check(c.receiveEnergy(-1,false)==0&&c.extractEnergy(-1,false)==0,"Negative transfer rejected "+label);
                    int received=c.receiveEnergy(100,false),sent=c.extractEnergy(100,false);
                    check(received==(mode==SideMode.INPUT?100:0)&&sent==(mode==SideMode.OUTPUT?100:0),"Real transfer direction "+label);
                    mode(f,side,SideMode.DISABLED);check(c.receiveEnergy(10,false)==0&&c.extractEnergy(10,false)==0,"Retained handler follows OFF "+label);
                }
                mode(f,side,SideMode.DISABLED);
            }
            check(!f.getCapability(ForgeCapabilities.ENERGY,null).isPresent(),"Unsided bypass denied "+facing);
        }
        var f=place(l,TEST);off(f);mode(f,RelativeSide.TOP,SideMode.INPUT);mode(f,RelativeSide.BACK,SideMode.INPUT);next(l);energy(f,10000);
        check(cap(f,Direction.UP).receiveEnergy(200,false)==200&&cap(f,Direction.SOUTH).receiveEnergy(200,false)==56&&cap(f,Direction.UP).receiveEnergy(200,false)==0,"Input allowance shared across calls and faces");
        next(l);check(cap(f,Direction.UP).receiveEnergy(1000,true)==256&&cap(f,Direction.UP).receiveEnergy(1000,true)==256&&cap(f,Direction.UP).receiveEnergy(1000,false)==256,"Simulation does not consume next-tick allowance");
        mode(f,RelativeSide.TOP,SideMode.OUTPUT);mode(f,RelativeSide.BACK,SideMode.OUTPUT);next(l);
        check(cap(f,Direction.UP).extractEnergy(200,false)==200&&cap(f,Direction.SOUTH).extractEnergy(200,false)==56&&cap(f,Direction.UP).extractEnergy(1,false)==0,"Output allowance shared across calls and faces");
        var retained=cap(f,Direction.UP);l.setBlockAndUpdate(TEST,Blocks.AIR.defaultBlockState());check(retained.extractEnergy(100,false)==0,"Removed block closes retained handler");
    }
    static void transfer(ServerLevel l){
        var f=place(l,TEST);off(f);energy(f,10000);mode(f,RelativeSide.RIGHT,SideMode.OUTPUT);
        var target=place(l,TEST.east());off(target);mode(target,RelativeSide.LEFT,SideMode.INPUT);energy(target,0);next(l);tick(l,f);
        check(f.getEnergyStored()==9744&&target.getEnergyStored()==256,"Automatic neighbor transfer conserves FE");
        next(l);var item=cell();f.getChargeInventory().setStackInSlot(0,item);var itemEnergy=item.getCapability(ForgeCapabilities.ENERGY).orElseThrow(()->new IllegalStateException());
        mode(f,RelativeSide.TOP,SideMode.OUTPUT);int before=f.getEnergyStored();check(cap(f,Direction.UP).extractEnergy(100,false)==100,"External output reserves shared allowance");tick(l,f);
        check(before-f.getEnergyStored()==256&&itemEnergy.getEnergyStored()==156&&target.getEnergyStored()==256,"Item charge and push share remaining output");
        next(l);tick(l,f);check(itemEnergy.getEnergyStored()==412&&target.getEnergyStored()==256,"Item charging priority preserves 256 FE total");
        l.setBlockAndUpdate(TEST.east(),Blocks.AIR.defaultBlockState());f.getChargeInventory().setStackInSlot(0,ItemStack.EMPTY);
    }
    static void dismantle(ServerLevel l,ServerPlayer p){
        for(Item tool:new Item[]{Items.WOODEN_PICKAXE,Items.GOLDEN_PICKAXE,Items.STONE_PICKAXE,Items.IRON_PICKAXE,Items.DIAMOND_PICKAXE,Items.NETHERITE_PICKAXE}){
            var f=place(l,TEST);clear(l);f.setCapacityEnchantLevel(4);f.getChargeInventory().setStackInSlot(0,cell());
            p.setGameMode(GameType.SURVIVAL);p.setShiftKeyDown(false);p.setItemInHand(InteractionHand.MAIN_HAND,new ItemStack(tool));p.gameMode.destroyBlock(TEST);
            long expected=tool!=Items.WOODEN_PICKAXE&&tool!=Items.GOLDEN_PICKAXE?1:0;
            check(count(l,ModBlocks.ENERGY_BUFFER.get().asItem())==expected&&count(l,cell().getItem())==1,"Survival block and cell drops "+tool);
            if(expected==1)check(drops(l).stream().filter(e->e.getItem().is(ModBlocks.ENERGY_BUFFER.get().asItem())).allMatch(e->EnergyBufferCapacity.getLevel(e.getItem())==4),"Normal mining preserves enchant "+tool);
        }
        var f=place(l,TEST);clear(l);off(f);f.setCapacityEnchantLevel(4);energy(f,700000);f.getChargeInventory().setStackInSlot(0,cell());mode(f,RelativeSide.LEFT,SideMode.INPUT);mode(f,RelativeSide.TOP,SideMode.OUTPUT);
        p.getInventory().clearContent();p.setItemInHand(InteractionHand.MAIN_HAND,new ItemStack(ForgeRegistries.ITEMS.getValue(new ResourceLocation("domesurvival","machine_wrench"))));
        var hit=new BlockHitResult(Vec3.atCenterOf(TEST),Direction.NORTH,TEST,false);p.gameMode.useItemOn(p,l,p.getMainHandItem(),InteractionHand.MAIN_HAND,hit);
        check(f.getMachineFacing()==Direction.EAST&&f.sideMode(Direction.NORTH)==SideMode.INPUT,"Wrench rotates relative configuration");
        p.setShiftKeyDown(true);p.gameMode.useItemOn(p,l,p.getMainHandItem(),InteractionHand.MAIN_HAND,hit);
        check(l.isEmptyBlock(TEST)&&drops(l).size()==2&&count(l,cell().getItem())==1&&count(l,ModBlocks.ENERGY_BUFFER.get().asItem())==1,"Wrench drops buffer and excluded cell exactly once");
        ItemStack portable=drops(l).stream().map(ItemEntity::getItem).filter(s->s.is(ModBlocks.ENERGY_BUFFER.get().asItem())).findFirst().orElseThrow().copy();
        check(portable.getTagElement("BlockEntityTag").getInt("Energy")==700000&&!portable.getTagElement("BlockEntityTag").contains("ChargeSlot")&&EnergyBufferCapacity.getLevel(portable)==4,"Portable charge and enchant preserved without duplicate slot");
        clear(l);p.setShiftKeyDown(false);p.setYRot(0);p.setItemInHand(InteractionHand.MAIN_HAND,portable);
        p.gameMode.useItemOn(p,l,p.getMainHandItem(),InteractionHand.MAIN_HAND,new BlockHitResult(Vec3.atCenterOf(TEST.below()).add(0,.5,0),Direction.UP,TEST.below(),false));f=(EnergyBufferBlockEntity)l.getBlockEntity(TEST);
        check(f!=null&&f.getEnergyStored()==700000&&f.getEnergyCapacity()==750000&&f.getChargeInventory().getStackInSlot(0).isEmpty(),"Placement restores charged enchanted buffer");
        check(f.sideMode(RelativeSide.LEFT.resolve(f.getMachineFacing()))==SideMode.INPUT&&f.sideMode(Direction.UP)==SideMode.OUTPUT,"Placement remaps preserved sides");
        p.getInventory().clearContent();p.setGameMode(GameType.CREATIVE);
    }
    @SubscribeEvent public static void server(TickEvent.ServerTickEvent e){
        if(!ENABLED||e.phase!=TickEvent.Phase.END||fixture)return;var mc=Minecraft.getInstance();var server=mc.getSingleplayerServer();if(server==null||server.getPlayerList().getPlayers().isEmpty())return;
        if(!mc.gameDirectory.toPath().toAbsolutePath().normalize().endsWith("steel-buffer-review"))throw new IllegalStateException("Isolated directory required");fixture=true;
        try{
            var l=server.overworld();var p=server.getPlayerList().getPlayers().get(0);l.setDayTime(6000);l.getGameRules().getRule(GameRules.RULE_DAYLIGHT).set(false,server);l.getGameRules().getRule(GameRules.RULE_DOMOBSPAWNING).set(false,server);
            for(int x=-4;x<17;x++)for(int z=-4;z<17;z++)l.setBlockAndUpdate(new BlockPos(x,99,z),Blocks.SMOOTH_STONE.defaultBlockState());
            storage(l);ports(l);transfer(l);dismantle(l,p);
            var f=place(l,DISPLAY);off(f);f.setCapacityEnchantLevel(4);energy(f,700000);
            p.teleportTo(l,1.6,100.3,-2.7,19,23);p.getAbilities().flying=true;p.onUpdateAbilities();ready=true;
        }catch(Throwable ex){check(false,"Server exception "+ex);ex.printStackTrace();ready=true;}
    }
    static void add(int delay,Runnable action){steps.add(new Step(delay,action));}
    static void shot(String name){try(NativeImage img=Screenshot.takeScreenshot(Minecraft.getInstance().getMainRenderTarget())){img.writeToFile(OUT.resolve(name+".png"));log("Screenshot "+name);}catch(Exception e){check(false,e.toString());}}
    static EnergyBufferScreen screen(){return (EnergyBufferScreen)Minecraft.getInstance().screen;}
    static void plan(){var mc=Minecraft.getInstance();mc.setScreen(null);mc.options.hideGui=true;mc.getTutorial().setStep(net.minecraft.client.tutorial.TutorialSteps.NONE);
        add(60,()->shot("01_model"));add(1,()->{mc.options.hideGui=false;mc.getSingleplayerServer().execute(()->{var p=mc.getSingleplayerServer().getPlayerList().getPlayers().get(0);NetworkHooks.openScreen(p,(EnergyBufferBlockEntity)p.serverLevel().getBlockEntity(DISPLAY),DISPLAY);});});
        add(90,()->{check(mc.screen instanceof EnergyBufferScreen,"Networked GUI opens");check(screen().getMenu().getEnergyStored()==700000&&screen().getMenu().getEnergyCapacity()==750000,"32-bit energy and capacity synchronized");shot("02_main");mc.options.guiScale().set(3);mc.resizeDisplay();});
        add(60,()->{shot("03_scale3");mc.options.guiScale().set(2);mc.resizeDisplay();screen().getMenu().setSidePanelOpen(true);mc.gameMode.handleInventoryButtonClick(screen().getMenu().containerId,202);});
        add(60,()->{shot("04_sides");check(!screen().getMenu().getSlot(0).isActive(),"Charge slot hidden on side page");mc.gameMode.handleInventoryButtonClick(screen().getMenu().containerId,100+RelativeSide.TOP.ordinal());});
        add(60,()->{check(screen().getMenu().getSideMode(RelativeSide.TOP)==SideMode.INPUT,"Side change synchronized");screen().getMenu().setSidePanelOpen(false);mc.gameMode.handleInventoryButtonClick(screen().getMenu().containerId,201);mc.getSingleplayerServer().execute(()->{var f=(EnergyBufferBlockEntity)mc.getSingleplayerServer().overworld().getBlockEntity(DISPLAY);f.getChargeInventory().setStackInSlot(0,cell());});});
        add(80,()->{check(screen().getMenu().getOutputPerTick()==256,"Actual charging throughput displayed");shot("05_charging");var models=Collections.newSetFromMap(new IdentityHashMap<net.minecraft.client.resources.model.BakedModel,Boolean>());
            for(int i=0;i<=4;i++){var item=new ItemStack(ModBlocks.ENERGY_BUFFER.get());var tag=item.getOrCreateTagElement("BlockEntityTag");tag.putInt("Energy",i*62500);var model=mc.getItemRenderer().getModel(item,mc.level,mc.player,0);check(model!=mc.getModelManager().getMissingModel()&&models.add(model),"Charged item model level "+i);}
        });
        add(10,()->{log("RESULT "+(failures==0?"PASS":"FAIL")+" failures="+failures);mc.stop();});
    }
    @SubscribeEvent public static void client(TickEvent.ClientTickEvent e){
        if(!ENABLED||e.phase!=TickEvent.Phase.END)return;var mc=Minecraft.getInstance();
        if(!started&&mc.getOverlay()==null&&mc.screen!=null&&mc.screen.getClass().getSimpleName().equals("AccessibilityOnboardingScreen")){mc.setScreen(new TitleScreen());return;}
        if(!started&&mc.screen instanceof TitleScreen&&mc.getOverlay()==null){started=true;mc.options.pauseOnLostFocus=false;mc.options.fov().set(50);mc.options.guiScale().set(2);mc.options.renderDistance().set(6);mc.options.enableVsync().set(false);mc.options.framerateLimit().set(120);mc.getLanguageManager().setSelected("ru_ru");mc.options.languageCode="ru_ru";mc.reloadResourcePacks();mc.createWorldOpenFlows().createFreshLevel("steel_review_"+System.currentTimeMillis(),new LevelSettings("Steel buffer review",GameType.CREATIVE,false,Difficulty.PEACEFUL,true,new GameRules(),WorldDataConfiguration.DEFAULT),new WorldOptions(63L,false,false),a->a.registryOrThrow(Registries.WORLD_PRESET).getHolderOrThrow(WorldPresets.FLAT).value().createWorldDimensions());return;}
        if(!ready||mc.player==null||mc.level==null||mc.getOverlay()!=null)return;if(!planned&&++ticks>100){planned=true;plan();}
    }
    @SubscribeEvent public static void render(TickEvent.RenderTickEvent e){
        if(!ENABLED||!planned||e.phase!=TickEvent.Phase.END||step>=steps.size())return;
        if(++frames>=steps.get(step).delay()){frames=0;try{steps.get(step++).action().run();}catch(Throwable ex){check(false,"Client exception "+ex);ex.printStackTrace();log("RESULT FAIL");Minecraft.getInstance().stop();}}
    }
}
