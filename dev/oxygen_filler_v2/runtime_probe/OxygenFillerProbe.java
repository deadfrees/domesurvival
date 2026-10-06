package com.wasted.domesurvival.fillerprobe;

import com.mojang.blaze3d.platform.NativeImage;
import com.wasted.domesurvival.forge.block.ModBlocks;
import com.wasted.domesurvival.forge.item.ModItems;
import com.wasted.domesurvival.forge.item.OxygenTankItem;
import com.wasted.domesurvival.forge.oxygen.room.*;
import com.wasted.domesurvival.forge.itempipe.ItemPipeBlock;
import com.wasted.domesurvival.forge.itempipe.ItemPipeRegistry;
import com.wasted.domesurvival.forge.machine.oxygen.*;
import com.wasted.domesurvival.forge.machine.module.*;
import com.wasted.domesurvival.forge.machine.side.*;
import com.wasted.domesurvival.forge.client.screen.OxygenFillerScreen;
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
public final class OxygenFillerProbe {
    static final boolean ENABLED=Boolean.getBoolean("dome.oxygenFillerReview");
    static final Path OUT=Path.of("../../dev/oxygen_filler_v2/runtime").toAbsolutePath().normalize();
    static final BlockPos DISPLAY=new BlockPos(0,100,0),TEST=new BlockPos(10,100,10);
    static boolean started,fixture,planned;static volatile boolean ready;static int ticks,step,failures;
    static double hoverX,hoverY;
    static void hover(int x,int y){var w=Minecraft.getInstance().getWindow();hoverX=((w.getGuiScaledWidth()-220)/2+x)*w.getGuiScale();hoverY=((w.getGuiScaledHeight()-266)/2+y)*w.getGuiScale();}
    record Step(int delay,Runnable action){}static final List<Step> steps=new ArrayList<>();
    static synchronized void log(String s){System.out.println("[FILLER_REVIEW] "+s);try{Files.createDirectories(OUT);Files.writeString(OUT.resolve("checks.txt"),s+"\n",StandardOpenOption.CREATE,StandardOpenOption.APPEND);}catch(Exception ex){throw new RuntimeException(ex);}}
    static void check(boolean ok,String s){if(!ok)failures++;log((ok?"PASS ":"FAIL ")+s);}
    static OxygenFillerBlockEntity place(ServerLevel l,BlockPos p){l.setBlockAndUpdate(p,Blocks.AIR.defaultBlockState());l.setBlockAndUpdate(p,ModBlocks.OXYGEN_FILLER.get().defaultBlockState());return (OxygenFillerBlockEntity)l.getBlockEntity(p);}
    static void tick(ServerLevel l,OxygenFillerBlockEntity f,int n){for(int i=0;i<n;i++)OxygenFillerBlockEntity.serverTick(l,f.getBlockPos(),f.getBlockState(),f);}
    static void mode(OxygenFillerBlockEntity f,RelativeSide s,SideMode m){for(int i=0;i<3&&f.sideMode(s.resolve(f.getMachineFacing()))!=m;i++)f.cycleSideMode(s);}
    static List<ItemEntity> drops(ServerLevel l){return l.getEntitiesOfClass(ItemEntity.class,new AABB(TEST).inflate(2));}
    static void clear(ServerLevel l){drops(l).forEach(ItemEntity::discard);}
    static int count(ServerLevel l,Item i){return drops(l).stream().filter(e->e.getItem().is(i)).mapToInt(e->e.getItem().getCount()).sum();}
    static void energy(OxygenFillerBlockEntity f,int amount){var n=f.saveWithoutMetadata();n.putInt("Energy",amount);f.load(n);}
    static void water(OxygenFillerBlockEntity f,int ignored,int oxygen){var n=f.saveWithoutMetadata();n.putInt("Oxygen",oxygen);f.load(n);}
    static ItemStack tank(Item item,int amount){var stack=new ItemStack(item);((OxygenTankItem)item).setOxygen(stack,amount);return stack;}
    static void process(ServerLevel l,ServerPlayer player){
        check(!ModBlocks.OXYGEN_FILLER.get().defaultBlockState().canOcclude(),"Recessed shell preserves neighbor faces");
        for(Item item:List.of(ModItems.SMALL_OXYGEN_TANK.get(),ModItems.MEDIUM_OXYGEN_TANK.get(),ModItems.LARGE_OXYGEN_TANK.get()))for(boolean efficient:List.of(false,true)){
            var f=place(l,TEST);if(efficient)f.getModules().setStackInSlot(0,new ItemStack(MachineModuleItems.EFFICIENCY.get()));
            int capacity=((OxygenTankItem)item).capacity();water(f,0,6000);energy(f,20000);
            var stack=tank(item,0);stack.setHoverName(net.minecraft.network.chat.Component.literal("Preserved tank"));f.getInventory().setStackInSlot(0,stack);
            tick(l,f,capacity-1);check(f.getTankOxygen()==capacity-1&&f.getInventory().getStackInSlot(1).isEmpty(),"No early output "+item+"/"+efficient);
            tick(l,f,1);var output=f.getInventory().getStackInSlot(1);
            check(f.getInventory().getStackInSlot(0).isEmpty()&&output.is(item)&&((OxygenTankItem)item).getOxygen(output)==capacity&&output.hasCustomHoverName(),"Full tank and NBT moved to output "+item+"/"+efficient);
            check(f.oxygenAmount()==6000-capacity&&f.getDataAccess().get(0)==20000-capacity*(efficient?4:5),"Exact oxygen and FE cost "+item+"/"+efficient);
            var old=f.saveWithoutMetadata();tick(l,f,20);check(f.oxygenAmount()==old.getInt("Oxygen")&&f.getDataAccess().get(0)==old.getInt("Energy"),"Idle consumes nothing");
        }
        var f=place(l,TEST);water(f,0,6000);energy(f,20000);f.getInventory().setStackInSlot(0,tank(ModItems.SMALL_OXYGEN_TANK.get(),119));f.getInventory().setStackInSlot(1,tank(ModItems.SMALL_OXYGEN_TANK.get(),120));
        tick(l,f,5);check(f.getTankOxygen()==120&&f.oxygenAmount()==5999&&f.getDataAccess().get(0)==19995,"Blocked output retains completed tank without repeated consumption");
        f.getInventory().extractItem(1,1,false);tick(l,f,1);check(f.getInventory().getStackInSlot(0).isEmpty()&&f.getInventory().getStackInSlot(1).is(ModItems.SMALL_OXYGEN_TANK.get()),"Output resumes when cleared");
        f.getInventory().setStackInSlot(0,tank(ModItems.LARGE_OXYGEN_TANK.get(),0));tick(l,f,10);
        var menu=new OxygenFillerMenu(3,player.getInventory(),f);menu.setTab(200);player.getInventory().setItem(9,new ItemStack(MachineModuleItems.EFFICIENCY.get()));
        check(!menu.quickMoveStack(player,0).isEmpty(),"Efficiency installs while filling");int before=menu.energyStored();tick(l,f,1);check(before-menu.energyStored()==4&&f.getTankOxygen()==11,"Efficiency applies at next per-unit fill step");
        check(!f.getModules().isItemValid(1,new ItemStack(MachineModuleItems.EFFICIENCY.get()))&&!f.getModules().isItemValid(1,new ItemStack(MachineModuleItems.OVERDRIVE.get())),"Duplicate and unsupported module rejected");
        f.getModules().setStackInSlot(1,new ItemStack(MachineModuleItems.BUFFER.get()));energy(f,35000);
        check(menu.energyCapacity()==35000&&!menu.getSlot(39).mayPickup(player)&&menu.quickMoveStack(player,39).isEmpty(),"High charge prevents buffer removal");
        energy(f,20000);check(menu.getSlot(39).mayPickup(player)&&!menu.quickMoveStack(player,39).isEmpty()&&menu.energyCapacity()==20000,"Safe buffer removal");
        energy(f,0);before=f.getTankOxygen();tick(l,f,10);check(before==f.getTankOxygen()&&!f.getBlockState().getValue(OxygenFillerBlock.LIT),"No power pauses pump and filling");
        energy(f,1000);water(f,0,0);tick(l,f,10);check(before==f.getTankOxygen(),"No oxygen pauses filling");water(f,0,100);tick(l,f,1);check(f.getTankOxygen()==before+1,"Restored oxygen resumes filling");
        var legacy=f.saveWithoutMetadata();legacy.remove("Modules");legacy.remove("PortFacing");var inventory=legacy.getCompound("Inventory");inventory.putInt("Size",1);inventory.getList("Items",10).removeIf(t->((net.minecraft.nbt.CompoundTag)t).getInt("Slot")!=0);f.load(legacy);
        check(f.getInventory().getSlots()==2&&!f.getInventory().getStackInSlot(0).isEmpty()&&f.getInventory().getStackInSlot(1).isEmpty(),"Legacy one-slot inventory migrates without losing tank");
        menu.setTab(202);check(!menu.getSlot(36).isActive()&&!menu.getSlot(38).isActive(),"Side panel hides machine and module slots");
        player.teleportTo(l,10.5,101,8.5,0,0);check(!menu.clickMenuButton(player,100+RelativeSide.FRONT.ordinal()),"Front setting denied");
    }
    static void ports(ServerLevel l){
        for(Direction facing:List.of(Direction.NORTH,Direction.EAST,Direction.SOUTH,Direction.WEST)){
            var f=place(l,TEST);l.setBlockAndUpdate(TEST,f.getBlockState().setValue(OxygenFillerBlock.FACING,facing));tick(l,f,1);
            for(RelativeSide side:RelativeSide.values())for(SideMode m:List.of(SideMode.DISABLED,SideMode.INPUT,SideMode.OUTPUT)){
                mode(f,side,m);var d=side.resolve(facing);water(f,0,3000);energy(f,1000);
                f.getInventory().setStackInSlot(0,ItemStack.EMPTY);f.getInventory().setStackInSlot(1,tank(ModItems.SMALL_OXYGEN_TANK.get(),120));
                var items=f.getCapability(ForgeCapabilities.ITEM_HANDLER,d).orElse(null);var fe=f.getCapability(ForgeCapabilities.ENERGY,d).orElse(null);var gas=f.getCapability(ModCapabilities.OXYGEN,d).orElse(null);
                boolean input=side!=RelativeSide.FRONT&&m==SideMode.INPUT,output=side!=RelativeSide.FRONT&&m==SideMode.OUTPUT;
                check((items!=null)==(input||output)&&(fe!=null)==input&&(gas!=null)==(input||output),"Capabilities "+facing+"/"+side+"/"+m);
                check(l.getBlockState(TEST).getValue(OxygenFillerBlock.portProperty(d))==PortVisual.fromMode(f.sideMode(d)),"Visual agrees "+facing+"/"+side+"/"+m);
                if(input){
                    var empty=tank(ModItems.SMALL_OXYGEN_TANK.get(),0);
                    check(items.insertItem(0,empty,true).isEmpty()&&f.getInventory().getStackInSlot(0).isEmpty(),"Tank input simulation");
                    check(items.insertItem(0,empty,false).isEmpty()&&items.extractItem(0,1,false).isEmpty()&&items.extractItem(1,1,false).isEmpty(),"Blue accepts tanks, never extracts");
                    check(!items.isItemValid(0,tank(ModItems.SMALL_OXYGEN_TANK.get(),120))&&!items.isItemValid(0,new ItemStack(Items.COAL)),"Only non-full tanks accepted");
                    check(gas.receiveOxygen(1000,true)==120&&f.oxygenAmount()==3000&&gas.extractOxygen(100,false)==0&&fe.receiveEnergy(1000,true)==64&&fe.extractEnergy(100,false)==0,"Input gas and FE limits");
                    var copy=items.getStackInSlot(0);copy.setCount(0);check(!f.getInventory().getStackInSlot(0).isEmpty(),"External item view cannot mutate inventory");
                    mode(f,side,SideMode.DISABLED);check(fe.receiveEnergy(64,false)==0&&gas.receiveOxygen(100,false)==0&&!items.isItemValid(0,empty),"Cached blue respects OFF");
                }else if(output){
                    check(items.extractItem(0,1,false).isEmpty()&&!items.insertItem(0,tank(ModItems.SMALL_OXYGEN_TANK.get(),0),false).isEmpty(),"Orange denies input and unfinished tank extraction");
                    check(items.extractItem(1,1,true).is(ModItems.SMALL_OXYGEN_TANK.get())&&!f.getInventory().getStackInSlot(1).isEmpty(),"Output simulation preserves tank");
                    check(items.extractItem(1,1,false).is(ModItems.SMALL_OXYGEN_TANK.get())&&f.getInventory().getStackInSlot(1).isEmpty()&&gas.receiveOxygen(100,false)==0,"Orange yields finished tank, no gas input");
                    mode(f,side,SideMode.DISABLED);check(items.extractItem(1,1,false).isEmpty()&&gas.extractOxygen(100,false)==0,"Cached orange respects OFF");
                }
            }
            check(!f.getCapability(ForgeCapabilities.ENERGY,null).isPresent()&&!f.getCapability(ForgeCapabilities.ITEM_HANDLER,null).isPresent()&&!f.getCapability(ModCapabilities.OXYGEN,null).isPresent(),"Unsided bypass denied "+facing);
        }
        var f=place(l,TEST);var pipePos=TEST.south();l.setBlockAndUpdate(pipePos,ModBlocks.OXYGEN_PIPE.get().defaultBlockState());
        var sourcePos=TEST.south(2);l.setBlockAndUpdate(sourcePos,ModBlocks.OXYGEN_ELECTROLYZER.get().defaultBlockState().setValue(OxygenElectrolyzerBlock.FACING,Direction.SOUTH));
        var source=(OxygenElectrolyzerBlockEntity)l.getBlockEntity(sourcePos);var n=source.saveWithoutMetadata();n.putInt("Oxygen",1000);source.load(n);
        while(source.sideMode(Direction.NORTH)!=SideMode.OUTPUT)source.cycleSideMode(RelativeSide.BACK);
        l.setBlockAndUpdate(pipePos,OxygenPipeBlock.refreshConnections(l,pipePos,l.getBlockState(pipePos)));
        for(SideMode m:List.of(SideMode.DISABLED,SideMode.INPUT,SideMode.OUTPUT)){
            mode(f,RelativeSide.BACK,m);water(f,0,0);tick(l,f,1);check(m==SideMode.INPUT?f.oxygenAmount()>0:f.oxygenAmount()==0,"Real oxygen pipe respects filler inlet "+m);
            var itemPipe=ItemPipeRegistry.STEEL_PIPE.get().defaultBlockState();check(ItemPipeBlock.refreshConnections(l,pipePos,itemPipe).getValue(ItemPipeBlock.NORTH)==(m!=SideMode.DISABLED),"Item pipe connects to enabled socket "+m);
        }
        mode(f,RelativeSide.BACK,SideMode.OUTPUT);mode(f,RelativeSide.TOP,SideMode.OUTPUT);water(f,0,500);
        int first=f.getCapability(ModCapabilities.OXYGEN,Direction.SOUTH).orElseThrow(()->new IllegalStateException()).extractOxygen(100,false);
        int second=f.getCapability(ModCapabilities.OXYGEN,Direction.UP).orElseThrow(()->new IllegalStateException()).extractOxygen(100,false);
        check(first+second==120&&f.oxygenAmount()==380,"Gas relay shares one 120-per-tick budget across outputs");
        l.removeBlock(pipePos,false);l.removeBlock(sourcePos,false);
    }
    static void room(ServerLevel l,BlockPos pos,boolean build){
        for(int x=-2;x<=2;x++)for(int z=-2;z<=2;z++)for(int y=0;y<=4;y++){
            if(x==0&&y==0&&z==0)continue;
            l.setBlockAndUpdate(pos.offset(x,y,z),(build&&(Math.abs(x)==2||Math.abs(z)==2||y==0||y==4)?(y==4?Blocks.SMOOTH_STONE:Blocks.GLASS):Blocks.AIR).defaultBlockState());
        }
        SealedRoomManager.invalidateAround(l,pos.above());
    }
    static void ventilation(ServerLevel l){
        var f=place(l,TEST);room(l,TEST,true);water(f,0,6000);energy(f,20000);f.cycleOperatingMode();tick(l,f,1);
        log("Room diagnostic mode="+f.getOperatingMode()+" status="+f.getDataAccess().get(6)+" state="+f.getDataAccess().get(8)+" volume="+f.getDataAccess().get(9)+" gas="+f.getDataAccess().get(10)+" required="+f.getDataAccess().get(11)+" airtightMachine="+SealedRoomManager.isAirtightBoundary(l,TEST,f.getBlockState())+" airAbove="+l.getBlockState(TEST.above()));
        check(f.getDataAccess().get(8)==SealedRoomManager.RoomState.SEALED.ordinal()&&f.getDataAccess().get(9)==27,"Vent discovers sealed 27-block room");
        check(!f.getCapability(ModCapabilities.OXYGEN,Direction.UP).isPresent()&&!f.getCapability(ForgeCapabilities.ITEM_HANDLER,Direction.UP).isPresent(),"Top reserved for ventilation outlet");
        tick(l,f,100);check(f.getDataAccess().get(10)==270&&f.getDataAccess().get(11)==270&&f.oxygenAmount()==5730&&f.getDataAccess().get(0)==18650,"Exact room oxygen capacity and energy cost");
        var n=f.saveWithoutMetadata();tick(l,f,30);check(f.oxygenAmount()==n.getInt("Oxygen")&&!f.getBlockState().getValue(OxygenFillerBlock.LIT),"Full room stops gas and visual activity");
        var gap=TEST.offset(2,2,0);l.removeBlock(gap,false);SealedRoomManager.invalidateAround(l,gap);tick(l,f,5);check(f.oxygenAmount()==n.getInt("Oxygen")&&!f.getBlockState().getValue(OxygenFillerBlock.LIT),"Leak stops ventilation instead of compensating");
        room(l,TEST,false);f.cycleOperatingMode();check(f.getCapability(ModCapabilities.OXYGEN,Direction.UP).isPresent(),"Tank mode restores top connector");
        // Transparent roofs seal by geometry even while sky light remains bright.
        BlockPos glassPos=TEST.above(8);var glass=place(l,glassPos);room(l,glassPos,true);
        for(int x=-2;x<=2;x++)for(int z=-2;z<=2;z++)l.setBlockAndUpdate(glassPos.offset(x,4,z),Blocks.GLASS.defaultBlockState());
        water(glass,0,6000);energy(glass,20000);glass.cycleOperatingMode();tick(l,glass,1);
        check(glass.getDataAccess().get(8)==SealedRoomManager.RoomState.SEALED.ordinal()&&glass.getDataAccess().get(9)==27,"Glass roof seals immediately without waiting for lighting");
        tick(l,glass,100);check(glass.getDataAccess().get(10)==270&&glass.oxygenAmount()==5730&&glass.getDataAccess().get(0)==18650,"Glass-roof room fills with exact oxygen and FE cost");
        BlockPos roofGap=glassPos.above(4);l.removeBlock(roofGap,false);SealedRoomManager.invalidateAround(l,roofGap);tick(l,glass,1);
        check(glass.getDataAccess().get(8)!=SealedRoomManager.RoomState.SEALED.ordinal(),"Real roof opening is not treated as sealed");
        l.setBlockAndUpdate(roofGap,Blocks.GLASS.defaultBlockState());SealedRoomManager.invalidateAround(l,glassPos.above());tick(l,glass,1);
        check(glass.getDataAccess().get(8)==SealedRoomManager.RoomState.SEALED.ordinal(),"Resealed glass roof restores room immediately");
        room(l,glassPos,false);l.removeBlock(glassPos,false);
    }
    static final BlockPos CACHE_ROOM=TEST.above(16);
    static void prepareNegativeCacheRoom(ServerLevel l){
        var f=place(l,CACHE_ROOM);room(l,CACHE_ROOM,true);
        l.removeBlock(CACHE_ROOM.above(4),false);water(f,0,6000);energy(f,20000);f.cycleOperatingMode();tick(l,f,1);
        check(f.getDataAccess().get(8)==SealedRoomManager.RoomState.OPEN.ordinal(),"Unroofed room is open by actual geometry");
        // The new roof lies outside the early-OPEN dependency set; simulate a
        // structure change without an event and require bounded cache recovery.
        l.setBlock(CACHE_ROOM.above(4),Blocks.GLASS.defaultBlockState(),2);
        check(SealedRoomManager.getOrDiscover(l,CACHE_ROOM.above()).state()==SealedRoomManager.RoomState.OPEN,"Negative room result remains cached before retry deadline");
    }
    static void verifyNegativeCacheRoom(ServerLevel l){
        var f=(OxygenFillerBlockEntity)l.getBlockEntity(CACHE_ROOM);
        check(f!=null&&f.getDataAccess().get(8)==SealedRoomManager.RoomState.SEALED.ordinal()&&f.getDataAccess().get(10)>0,"Closed distant roof recovers and fills without manual reset");
        room(l,CACHE_ROOM,false);l.removeBlock(CACHE_ROOM,false);
    }
    static void dismantle(ServerLevel l,ServerPlayer p){
        p.setGameMode(GameType.SURVIVAL);p.setShiftKeyDown(false);
        for(Item tool:new Item[]{Items.IRON_PICKAXE,Items.WOODEN_PICKAXE}){
            var f=place(l,TEST);clear(l);f.getModules().setStackInSlot(0,new ItemStack(MachineModuleItems.BUFFER.get()));
            p.setItemInHand(InteractionHand.MAIN_HAND,new ItemStack(tool));p.gameMode.destroyBlock(TEST);
            check(count(l,ModBlocks.OXYGEN_FILLER.get().asItem())==(tool==Items.IRON_PICKAXE?1:0),"Survival harvest tier "+tool);
            check(count(l,MachineModuleItems.BUFFER.get())==1,"Module drops exactly once "+tool);
        }
        var f=place(l,TEST);clear(l);f.getModules().setStackInSlot(0,new ItemStack(MachineModuleItems.BUFFER.get()));water(f,0,1000);energy(f,35000);f.getInventory().setStackInSlot(0,tank(ModItems.LARGE_OXYGEN_TANK.get(),100));tick(l,f,40);
        var wrench=ForgeRegistries.ITEMS.getValue(new ResourceLocation("domesurvival","machine_wrench"));p.getInventory().clearContent();p.setItemInHand(InteractionHand.MAIN_HAND,new ItemStack(wrench));
        var hit=new BlockHitResult(Vec3.atCenterOf(TEST),Direction.NORTH,TEST,false);mode(f,RelativeSide.TOP,SideMode.INPUT);mode(f,RelativeSide.LEFT,SideMode.OUTPUT);
        p.gameMode.useItemOn(p,l,p.getMainHandItem(),InteractionHand.MAIN_HAND,hit);check(f.getMachineFacing()==Direction.EAST&&f.sideMode(RelativeSide.LEFT.resolve(f.getMachineFacing()))==SideMode.OUTPUT,"Wrench rotates configured sides");
        var expected=f.saveWithoutMetadata();p.setShiftKeyDown(true);p.gameMode.useItemOn(p,l,p.getMainHandItem(),InteractionHand.MAIN_HAND,hit);
        check(l.isEmptyBlock(TEST)&&drops(l).size()==1&&p.getInventory().countItem(ModBlocks.OXYGEN_FILLER.get().asItem())==0,"Wrench creates one world drop");
        var entity=drops(l).get(0);var stack=entity.getItem().copy();var saved=stack.getTag().getCompound("BlockEntityTag");
        for(String key:List.of("Modules","Energy","Oxygen","Inventory","OperatingMode","UnifiedSideConfig"))check(saved.get(key).equals(expected.get(key)),"Portable data retained "+key);
        entity.playerTouch(p);check(entity.isAlive(),"Normal pickup delay");clear(l);p.setShiftKeyDown(false);p.setYRot(180);p.setItemInHand(InteractionHand.MAIN_HAND,stack);
        p.gameMode.useItemOn(p,l,p.getMainHandItem(),InteractionHand.MAIN_HAND,new BlockHitResult(Vec3.atCenterOf(TEST.below()).add(0,.5,0),Direction.UP,TEST.below(),false));
        f=(OxygenFillerBlockEntity)l.getBlockEntity(TEST);check(f!=null&&f.getTankOxygen()==140&&f.oxygenAmount()==960&&f.getDataAccess().get(0)==expected.getInt("Energy"),"Actual placement restores tank, oxygen and energy");
        check(f.sideMode(Direction.UP)==SideMode.INPUT&&f.sideMode(RelativeSide.LEFT.resolve(f.getMachineFacing()))==SideMode.OUTPUT&&!f.getCapability(ForgeCapabilities.FLUID_HANDLER,f.getMachineFacing()).isPresent(),"Placed ports follow orientation");
        p.setGameMode(GameType.ADVENTURE);p.setShiftKeyDown(true);p.setItemInHand(InteractionHand.MAIN_HAND,new ItemStack(wrench));p.gameMode.useItemOn(p,l,p.getMainHandItem(),InteractionHand.MAIN_HAND,hit);check(l.getBlockEntity(TEST)==f,"Adventure dismantle denied");p.setShiftKeyDown(false);p.setGameMode(GameType.CREATIVE);p.getInventory().clearContent();
    }
    @SubscribeEvent public static void server(TickEvent.ServerTickEvent e){
        if(!ENABLED||e.phase!=TickEvent.Phase.END||fixture)return;var mc=Minecraft.getInstance();var server=mc.getSingleplayerServer();if(server==null||server.getPlayerList().getPlayers().isEmpty())return;
        if(!mc.gameDirectory.toPath().toAbsolutePath().normalize().endsWith("oxygen-filler-review"))throw new IllegalStateException("Isolated directory required");
        fixture=true;try{
            var l=server.overworld();var p=server.getPlayerList().getPlayers().get(0);l.setDayTime(6000);l.getGameRules().getRule(GameRules.RULE_DAYLIGHT).set(false,server);l.getGameRules().getRule(GameRules.RULE_DOMOBSPAWNING).set(false,server);
            for(int x=-4;x<16;x++)for(int z=-4;z<16;z++)l.setBlockAndUpdate(new BlockPos(x,99,z),Blocks.SMOOTH_STONE.defaultBlockState());
            process(l,p);ports(l);ventilation(l);dismantle(l,p);prepareNegativeCacheRoom(l);
            var f=place(l,DISPLAY);f.getModules().setStackInSlot(0,new ItemStack(MachineModuleItems.BUFFER.get()));water(f,0,6000);energy(f,35000);f.getInventory().setStackInSlot(0,tank(ModItems.LARGE_OXYGEN_TANK.get(),0));mode(f,RelativeSide.TOP,SideMode.DISABLED);
            l.setBlockAndUpdate(DISPLAY.west(),ItemPipeRegistry.STEEL_PIPE.get().defaultBlockState());l.setBlockAndUpdate(DISPLAY.east(),ModBlocks.OXYGEN_PIPE.get().defaultBlockState());
            p.teleportTo(l,1.6,100.3,-2.7,19,23);p.getAbilities().flying=true;p.onUpdateAbilities();p.getInventory().setItem(9,new ItemStack(MachineModuleItems.EFFICIENCY.get()));ready=true;
        }catch(Throwable ex){check(false,"Server exception "+ex);ex.printStackTrace();ready=true;}
    }
    static void add(int delay,Runnable action){steps.add(new Step(delay,action));}
    static void shot(String name){try(NativeImage img=Screenshot.takeScreenshot(Minecraft.getInstance().getMainRenderTarget())){Files.createDirectories(OUT);img.writeToFile(OUT.resolve(name+".png"));log("Screenshot "+name);}catch(Exception ex){check(false,"Screenshot "+ex);}}
    static OxygenFillerScreen screen(){return (OxygenFillerScreen)Minecraft.getInstance().screen;}
    static void tab(int id){var mc=Minecraft.getInstance();screen().getMenu().setTab(id);mc.gameMode.handleInventoryButtonClick(screen().getMenu().containerId,id);}
    static void otherGui(Block block){var mc=Minecraft.getInstance();mc.player.closeContainer();mc.setScreen(null);
        mc.getSingleplayerServer().execute(()->{var p=mc.getSingleplayerServer().getPlayerList().getPlayers().get(0);var l=p.serverLevel();var pos=DISPLAY.south(2);
            l.setBlockAndUpdate(pos,block.defaultBlockState());NetworkHooks.openScreen(p,(net.minecraft.world.MenuProvider)l.getBlockEntity(pos),pos);});
    }
    static void plan(){var mc=Minecraft.getInstance();mc.setScreen(null);mc.options.hideGui=true;mc.getTutorial().setStep(net.minecraft.client.tutorial.TutorialSteps.NONE);
        var animated=new OxygenFillerBlockEntity(BlockPos.ZERO,ModBlocks.OXYGEN_FILLER.get().defaultBlockState().setValue(OxygenFillerBlock.LIT,true));OxygenFillerBlockEntity.clientTick(mc.level,BlockPos.ZERO,animated.getBlockState(),animated);float angle=animated.animationPhase(1);check(angle>0,"Compressor moves when active");animated.setBlockState(animated.getBlockState().setValue(OxygenFillerBlock.LIT,false));OxygenFillerBlockEntity.clientTick(mc.level,BlockPos.ZERO,animated.getBlockState(),animated);check(animated.animationPhase(1)==angle,"Compressor freezes while paused");
        check(ItemPipeBlock.refreshConnections(mc.level,DISPLAY.west(),mc.level.getBlockState(DISPLAY.west())).getValue(ItemPipeBlock.EAST),"Client blue item connection");
        check(OxygenPipeBlock.refreshConnections(mc.level,DISPLAY.east(),mc.level.getBlockState(DISPLAY.east())).getValue(OxygenPipeBlock.WEST),"Client orange oxygen connection");
        add(20,()->shot("01_existing_model"));
        add(10,()->{mc.options.hideGui=false;mc.getSingleplayerServer().execute(()->{var p=mc.getSingleplayerServer().getPlayerList().getPlayers().get(0);NetworkHooks.openScreen(p,(OxygenFillerBlockEntity)p.serverLevel().getBlockEntity(DISPLAY),DISPLAY);});});
        add(25,()->{check(mc.screen instanceof OxygenFillerScreen,"Networked filler GUI opens");check(screen().getMenu().energyCapacity()==35000&&screen().getMenu().energyStored()>32000,"Expanded FE buffer synchronized");shot("02_main_gui");mc.options.guiScale().set(3);mc.resizeDisplay();});
        add(20,()->{shot("03_main_scale3");mc.options.guiScale().set(2);mc.resizeDisplay();hover(20,70);});
        add(10,()->{shot("03_energy_tooltip");hoverX=hoverY=0;tab(202);});
        add(20,()->{shot("04_sides");mc.gameMode.handleInventoryButtonClick(screen().getMenu().containerId,100+RelativeSide.TOP.ordinal());});
        add(20,()->{check(screen().getMenu().getSideMode(RelativeSide.TOP)==SideMode.INPUT,"Networked side button updates menu");check(((OxygenFillerBlockEntity)mc.level.getBlockEntity(DISPLAY)).sideMode(Direction.UP)==SideMode.INPUT,"Client receives logical side update");tab(200);});
        add(20,()->{shot("05_modules_empty");mc.gameMode.handleInventoryMouseClick(screen().getMenu().containerId,0,0,ClickType.QUICK_MOVE,mc.player);});
        add(20,()->{check(screen().getMenu().getSlot(39).hasItem(),"Networked Shift-click installs efficiency while working");shot("06_modules_installed");mc.options.guiScale().set(3);mc.resizeDisplay();});
        add(20,()->{shot("07_module_scale3");mc.options.guiScale().set(2);mc.resizeDisplay();hover(28,75);});
        add(10,()->{shot("07_module_help");hoverX=hoverY=0;mc.player.closeContainer();
            var runtime=JeiCapture.runtime;check(runtime!=null,"JEI runtime available");var manager=runtime.getRecipeManager();
            check(manager.createRecipeCategoryLookup().get().filter(c->c.getRecipeType().getUid().equals(DomeSurvivalJeiPlugin.OXYGEN_FILLER.getUid())).count()==1,"Exactly one filler JEI category");
            check(manager.createRecipeLookup(DomeSurvivalJeiPlugin.OXYGEN_FILLER).get().count()==3,"Exactly three tank sizes, no duplicates");
            runtime.getRecipesGui().showTypes(List.of(DomeSurvivalJeiPlugin.OXYGEN_FILLER));});
        add(1,()->mc.getSingleplayerServer().execute(()->verifyNegativeCacheRoom(mc.getSingleplayerServer().overworld())));
        add(30,()->shot("08_jei"));add(100,()->shot("09_jei_progress"));
        add(10,()->{mc.setScreen(null);mc.getSingleplayerServer().execute(()->{var p=mc.getSingleplayerServer().getPlayerList().getPlayers().get(0);var l=p.serverLevel();room(l,DISPLAY,true);var f=(OxygenFillerBlockEntity)l.getBlockEntity(DISPLAY);water(f,0,6000);energy(f,35000);NetworkHooks.openScreen(p,f,DISPLAY);});});
        add(15,()->{screen().mouseClicked((mc.getWindow().getGuiScaledWidth()-220)/2+191,(mc.getWindow().getGuiScaledHeight()-266)/2+43,0);});
        add(15,()->{check(screen().getMenu().getOperatingMode()==OxygenFillerMode.VENTILATION,"Air pictogram switches mode over network");check(!screen().getMenu().getSlot(36).isActive(),"Vent hides item slots");shot("10_ventilation_gui");mc.player.closeContainer();mc.setScreen(null);mc.options.hideGui=true;mc.getSingleplayerServer().execute(()->{var p=mc.getSingleplayerServer().getPlayerList().getPlayers().get(0);p.teleportTo(p.serverLevel(),1.4,101.1,-.8,35,47);});});
        add(10,()->shot("11_ventilation_vapour"));
        add(10,()->shot("12_vapour_motion"));
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
        if(!started&&mc.screen instanceof TitleScreen&&mc.getOverlay()==null){started=true;mc.options.pauseOnLostFocus=false;mc.options.fov().set(50);mc.options.guiScale().set(2);mc.options.renderDistance().set(6);mc.options.enableVsync().set(false);mc.options.framerateLimit().set(120);mc.getLanguageManager().setSelected("ru_ru");mc.options.languageCode="ru_ru";mc.reloadResourcePacks();mc.createWorldOpenFlows().createFreshLevel("filler_review_"+System.currentTimeMillis(),new LevelSettings("Electrolyzer review",GameType.CREATIVE,false,Difficulty.PEACEFUL,true,new GameRules(),WorldDataConfiguration.DEFAULT),new WorldOptions(63L,false,false),a->a.registryOrThrow(Registries.WORLD_PRESET).getHolderOrThrow(WorldPresets.FLAT).value().createWorldDimensions());return;}
        if(!ready||mc.player==null||mc.level==null||mc.getOverlay()!=null)return;ticks++;
        if(!planned&&ticks>100){planned=true;ticks=0;plan();}else if(planned&&step<steps.size()&&ticks>=steps.get(step).delay()){ticks=0;try{steps.get(step++).action().run();}catch(Throwable ex){check(false,"Client exception "+ex);ex.printStackTrace();log("RESULT FAIL");mc.stop();}}
    }
}
