package com.wasted.domesurvival.forge.machine.filter;

import com.wasted.domesurvival.forge.item.WaterFilterItem;
import com.wasted.domesurvival.forge.machine.energy.MachineEnergyStorage;
import com.wasted.domesurvival.forge.machine.module.*;
import com.wasted.domesurvival.forge.machine.oxygen.complex.OxygenComplexFilters;
import com.wasted.domesurvival.forge.machine.side.*;
import com.wasted.domesurvival.forge.sound.MachineAmbientSoundService;
import net.minecraft.core.*;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.ItemTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.*;
import net.minecraft.world.inventory.*;
import net.minecraft.world.item.*;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.common.capabilities.*;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.energy.IEnergyStorage;
import net.minecraftforge.items.*;
import org.jetbrains.annotations.*;
import java.util.*;

/** Paid repair cycles keep their price and duration when modules change. */
public final class FilterRegenerationBlockEntity extends BlockEntity implements MenuProvider, IModularMachine {
    public static final int ENERGY_CAPACITY=20_000, MAX_INPUT_PER_TICK=64, ENERGY_PER_TICK=40;
    public static final int PROCESSING_TICKS=200, ENERGY_PER_REGENERATION=8000, MAX_REGENERATION_CYCLES=8;
    public static final int SLOT_FILTER=0,SLOT_MEDIA=1,SLOT_OUTPUT=2;
    public static final int STATUS_READY=0,STATUS_REGENERATING=1,STATUS_NO_ENERGY=2,STATUS_NO_FILTER=3,
        STATUS_NO_MEDIA=4,STATUS_FILTER_HEALTHY=5,STATUS_EXHAUSTED=6,STATUS_OUTPUT_FULL=7;
    public static final int DATA_ENERGY=0,DATA_CAPACITY=1,DATA_PROGRESS=2,DATA_MAX_PROGRESS=3,
        DATA_STATUS=4,DATA_REGEN_CYCLES=5,DATA_MAX_REGEN_CYCLES=6,DATA_CYCLE_ENERGY=7,DATA_SIDES_START=8,DATA_COUNT=14;
    private static final String FILTER_NBT_REGEN_CYCLES="DomeRegenCycles";
    private static final TagKey<Item> MEDIA=ItemTags.create(new ResourceLocation("domesurvival","filter_regeneration_media"));
    private final UnifiedSideConfig sideConfig=new UnifiedSideConfig();
    private final MachineEnergyStorage energy=new MachineEnergyStorage(ENERGY_CAPACITY,MAX_INPUT_PER_TICK,0);
    private int progress,cycleTicks,cycleEnergy,ambientSoundTick;
    private ItemStack cycleFilter=ItemStack.EMPTY;
    private boolean internalInventoryChange,syncPending;
    private Direction lastFacing;
    private float animationTick,previousAnimationTick;
    private final ItemStackHandler inventory=new ItemStackHandler(3){
        @Override public boolean isItemValid(int slot,ItemStack stack){
            return slot==SLOT_FILTER?isEligibleFilter(stack):slot==SLOT_MEDIA&&isRegenerationMedia(stack);
        }
        @Override public int getSlotLimit(int slot){return slot==SLOT_MEDIA?64:1;}
        @Override protected void onContentsChanged(int slot){
            if(slot==SLOT_FILTER&&!internalInventoryChange&&progress>0
                    &&!ItemStack.matches(cycleFilter,getStackInSlot(SLOT_FILTER)))resetCycle();
            setChanged();
        }
    };
    private final MachineModuleInventory modules=new MachineModuleInventory(this,MachineModuleResolver.STANDARD,this::modulesChanged);
    private final EnumMap<Direction,Port> ports=new EnumMap<>(Direction.class);
    private final ContainerData data=new ContainerData(){
        public int get(int i){
            if(i>=DATA_SIDES_START&&i<DATA_COUNT)return sideMode(Direction.values()[i-DATA_SIDES_START]).ordinal();
            return switch(i){
                case DATA_ENERGY->energy.getEnergyStored();case DATA_CAPACITY->energy.getMaxEnergyStored();
                case DATA_PROGRESS->progress;case DATA_MAX_PROGRESS->processingTicks();case DATA_STATUS->status();
                case DATA_REGEN_CYCLES->getRegenerationCycles(inventory.getStackInSlot(SLOT_FILTER));
                case DATA_MAX_REGEN_CYCLES->MAX_REGENERATION_CYCLES;case DATA_CYCLE_ENERGY->processingEnergy();default->0;
            };
        }
        public void set(int i,int v){}public int getCount(){return DATA_COUNT;}
    };
    public FilterRegenerationBlockEntity(BlockPos pos,BlockState state){
        super(FilterRegenerationRegistry.FILTER_REGENERATION_BLOCK_ENTITY.get(),pos,state);
        defaults();lastFacing=getMachineFacing();
    }
    @Override public int moduleSlotCount(){return 2;}
    @Override public Set<MachineModuleType> allowedModuleTypes(){return Set.of(MachineModuleType.BUFFER,MachineModuleType.EFFICIENCY,MachineModuleType.OVERDRIVE);}
    public MachineModuleInventory getModules(){return modules;}
    private void modulesChanged(){energy.setCapacityInternal(modules.modifiers().applyBufferCapacity(ENERGY_CAPACITY));setChanged();}
    @Override public void setChanged(){super.setChanged();syncPending=true;}
    public ItemStackHandler getInventory(){return inventory;}
    public ContainerData getDataAccess(){return data;}
    public int processingTicks(){return progress>0?cycleTicks:modules.modifiers().applyProcessingTicks(PROCESSING_TICKS);}
    public int processingEnergy(){return progress>0?cycleEnergy:modules.modifiers().applyEnergyCost(ENERGY_PER_REGENERATION);}
    public boolean canRemoveFilter(){return progress==0;}
    private int nextEnergyCost(){
        int ticks=processingTicks(),cost=processingEnergy();
        return (int)(((long)(progress+1)*cost)/ticks-((long)progress*cost)/ticks);
    }
    private void resetCycle(){progress=0;cycleTicks=0;cycleEnergy=0;cycleFilter=ItemStack.EMPTY;}
    private void finishCycle(){
        ItemStack repaired=inventory.getStackInSlot(SLOT_FILTER).copy();
        repaired.setDamageValue(Math.max(0,repaired.getDamageValue()-Math.max(1,repaired.getMaxDamage()/4)));
        repaired.getOrCreateTag().putInt(FILTER_NBT_REGEN_CYCLES,getRegenerationCycles(repaired)+1);
        internalInventoryChange=true;
        inventory.setStackInSlot(SLOT_FILTER,repaired);
        inventory.extractItem(SLOT_MEDIA,1,false);
        internalInventoryChange=false;resetCycle();
    }
    private boolean moveCompleted(){
        var filter=inventory.getStackInSlot(SLOT_FILTER);
        if(progress>0||!isEligibleFilter(filter)||canRegenerateFilter(filter)||!inventory.getStackInSlot(SLOT_OUTPUT).isEmpty())return false;
        internalInventoryChange=true;
        inventory.setStackInSlot(SLOT_OUTPUT,filter.copy());inventory.setStackInSlot(SLOT_FILTER,ItemStack.EMPTY);
        internalInventoryChange=false;return true;
    }
    public int status(){
        ItemStack filter=inventory.getStackInSlot(SLOT_FILTER);
        if(!isEligibleFilter(filter))return STATUS_NO_FILTER;
        if(!canRegenerateFilter(filter))return !inventory.getStackInSlot(SLOT_OUTPUT).isEmpty()?STATUS_OUTPUT_FULL:
            filter.getDamageValue()==0?STATUS_FILTER_HEALTHY:STATUS_EXHAUSTED;
        if(!isRegenerationMedia(inventory.getStackInSlot(SLOT_MEDIA)))return STATUS_NO_MEDIA;
        if(energy.getEnergyStored()<nextEnergyCost())return STATUS_NO_ENERGY;
        return progress>0?STATUS_REGENERATING:STATUS_READY;
    }
    public static void serverTick(Level level,BlockPos pos,BlockState state,FilterRegenerationBlockEntity f){
        if(f.lastFacing!=f.getMachineFacing())f.rotateSideConfiguration(f.lastFacing);
        f.syncPorts();
        boolean changed=f.moveCompleted(),active=false;
        if(f.progress>0&&!ItemStack.matches(f.cycleFilter,f.inventory.getStackInSlot(SLOT_FILTER))){f.resetCycle();changed=true;}
        int status=f.status();
        if(status==STATUS_READY||status==STATUS_REGENERATING){
            if(f.progress==0){
                f.cycleTicks=f.processingTicks();f.cycleEnergy=f.processingEnergy();
                f.cycleFilter=f.inventory.getStackInSlot(SLOT_FILTER).copy();
            }
            f.energy.removeEnergyInternal(f.nextEnergyCost());f.progress++;active=true;changed=true;
            if(f.progress>=f.cycleTicks)f.finishCycle();
            changed|=f.moveCompleted();
        }
        f.ambientSoundTick=MachineAmbientSoundService.tick(level,pos,active,f.ambientSoundTick,MachineAmbientSoundService.MachineType.FILTER_REGENERATOR);
        state=level.getBlockState(pos);
        if(state.getValue(FilterRegenerationBlock.ACTIVE)!=active){level.setBlock(pos,state.setValue(FilterRegenerationBlock.ACTIVE,active),3);changed=true;}
        if(changed)f.setChanged();
        if(f.syncPending&&level.getGameTime()%10==0){f.syncPending=false;level.sendBlockUpdated(pos,level.getBlockState(pos),level.getBlockState(pos),2);}
    }
    public static void clientTick(Level level,BlockPos pos,BlockState state,FilterRegenerationBlockEntity f){
        f.previousAnimationTick=f.animationTick;
        if(state.getValue(FilterRegenerationBlock.ACTIVE))f.animationTick=(f.animationTick+1)%80;
    }
    public float animationPhase(float partial){float next=animationTick<previousAnimationTick?animationTick+80:animationTick;return (previousAnimationTick+(next-previousAnimationTick)*partial)/80F;}
    public static boolean isEligibleFilter(ItemStack s){return !s.isEmpty()&&s.isDamageableItem()&&(s.getItem() instanceof WaterFilterItem||OxygenComplexFilters.isAirFilter(s));}
    public static boolean isRegenerationMedia(ItemStack s){return !s.isEmpty()&&s.is(MEDIA);}
    public static boolean canRegenerateFilter(ItemStack s){return isEligibleFilter(s)&&s.getDamageValue()>0&&getRegenerationCycles(s)<MAX_REGENERATION_CYCLES;}
    public static int getRegenerationCycles(ItemStack s){return !s.hasTag()?0:Math.max(0,Math.min(MAX_REGENERATION_CYCLES,s.getTag().getInt(FILTER_NBT_REGEN_CYCLES)));}
    public Direction getMachineFacing(){return getBlockState().getValue(FilterRegenerationBlock.FACING);}
    public SideMode sideMode(Direction d){return d==null||d==getMachineFacing()?SideMode.DISABLED:sideConfig.getMode(d);}
    private void defaults(){
        sideConfig.reset();
        for(RelativeSide r:RelativeSide.values())sideConfig.setMode(r.resolve(getMachineFacing()),r==RelativeSide.FRONT?SideMode.DISABLED:r==RelativeSide.RIGHT||r==RelativeSide.BOTTOM?SideMode.OUTPUT:SideMode.INPUT);
    }
    public SideMode cycleSideMode(RelativeSide r){
        if(r==RelativeSide.FRONT)return SideMode.DISABLED;
        var result=sideConfig.cycleMode(r.resolve(getMachineFacing()));refreshCaps();syncPorts();setChanged();return result;
    }
    public void rotateSideConfiguration(Direction old){
        Direction facing=getMachineFacing();
        if(old!=null&&old!=facing){
            EnumMap<RelativeSide,SideMode> modes=new EnumMap<>(RelativeSide.class);
            for(var r:RelativeSide.values())modes.put(r,sideConfig.getMode(r.resolve(old)));
            for(var r:RelativeSide.values())sideConfig.setMode(r.resolve(facing),modes.get(r));
        }
        sideConfig.setMode(facing,SideMode.DISABLED);lastFacing=facing;refreshCaps();syncPorts();setChanged();
    }
    private void syncPorts(){
        if(level==null||level.isClientSide)return;
        BlockState state=level.getBlockState(worldPosition);
        if(!(state.getBlock() instanceof FilterRegenerationBlock))return;
        BlockState updated=state;
        for(Direction d:Direction.values())updated=updated.setValue(FilterRegenerationBlock.portProperty(d),PortVisual.fromMode(sideMode(d)));
        if(!state.equals(updated))level.setBlock(worldPosition,updated,3);
    }
    @Override public void onLoad(){super.onLoad();syncPorts();}
    @Override protected void saveAdditional(CompoundTag tag){
        super.saveAdditional(tag);tag.put("Inventory",inventory.serializeNBT());tag.putInt("Energy",energy.getEnergyStored());
        tag.putInt("Progress",progress);tag.putInt("CycleTicks",cycleTicks);tag.putInt("CycleEnergy",cycleEnergy);
        tag.put("CycleFilter",cycleFilter.save(new CompoundTag()));tag.put("Modules",modules.serializeNBT());
        sideConfig.save(tag);tag.putString("PortFacing",getMachineFacing().getName());
    }
    @Override public void load(CompoundTag tag){
        super.load(tag);internalInventoryChange=true;
        CompoundTag inv=tag.getCompound("Inventory").copy();inv.putInt("Size",3);inventory.deserializeNBT(inv);
        internalInventoryChange=false;
        if(tag.contains("Modules"))modules.deserializeNBT(tag.getCompound("Modules"));
        else for(int i=0;i<2;i++)modules.setStackInSlot(i,ItemStack.EMPTY);
        modulesChanged();energy.setEnergyStoredInternal(tag.getInt("Energy"));
        // Old saves have paid base-rate progress but no cycle snapshot.
        cycleTicks=tag.contains("CycleTicks")?Math.max(1,tag.getInt("CycleTicks")):PROCESSING_TICKS;
        cycleEnergy=tag.contains("CycleEnergy")?Math.max(1,tag.getInt("CycleEnergy")):ENERGY_PER_REGENERATION;
        progress=Math.max(0,Math.min(cycleTicks-1,tag.getInt("Progress")));
        cycleFilter=tag.contains("CycleFilter")?ItemStack.of(tag.getCompound("CycleFilter")):inventory.getStackInSlot(SLOT_FILTER).copy();
        if(progress==0||!canRegenerateFilter(inventory.getStackInSlot(SLOT_FILTER))||!ItemStack.matches(cycleFilter,inventory.getStackInSlot(SLOT_FILTER)))resetCycle();
        if(!sideConfig.load(tag))defaults();
        rotateSideConfiguration(Direction.byName(tag.getString("PortFacing")));
    }
    @Override public CompoundTag getUpdateTag(){return saveWithoutMetadata();}
    @Override public void handleUpdateTag(CompoundTag tag){load(tag);}
    @Override public ClientboundBlockEntityDataPacket getUpdatePacket(){return ClientboundBlockEntityDataPacket.create(this);}
    @Override public <T> @NotNull LazyOptional<T> getCapability(@NotNull Capability<T> cap,@Nullable Direction side){
        if(cap==ForgeCapabilities.ITEM_HANDLER||cap==ForgeCapabilities.ENERGY){
            if(isRemoved()||sideMode(side)==SideMode.DISABLED)return LazyOptional.empty();
            Port p=ports.computeIfAbsent(side,Port::new);
            return cap==ForgeCapabilities.ITEM_HANDLER?p.items.cast():sideMode(side)==SideMode.INPUT?p.power.cast():LazyOptional.empty();
        }
        return super.getCapability(cap,side);
    }
    private void refreshCaps(){ports.values().forEach(p->{p.items.invalidate();p.power.invalidate();});ports.clear();}
    @Override public void invalidateCaps(){super.invalidateCaps();refreshCaps();}
    @Override public void reviveCaps(){super.reviveCaps();refreshCaps();}
    private final class Port implements IItemHandler {
        final Direction side;Port(Direction d){side=d;}
        boolean input(){return !isRemoved()&&sideMode(side)==SideMode.INPUT;}
        boolean output(){return !isRemoved()&&sideMode(side)==SideMode.OUTPUT;}
        final LazyOptional<IItemHandler> items=LazyOptional.of(()->this);
        final LazyOptional<IEnergyStorage> power=LazyOptional.of(()->new IEnergyStorage(){
            public int receiveEnergy(int n,boolean sim){int v=input()?energy.receiveEnergy(n,sim):0;if(v>0&&!sim)setChanged();return v;}
            public int extractEnergy(int n,boolean sim){return 0;}public boolean canReceive(){return input();}public boolean canExtract(){return false;}
            public int getEnergyStored(){return energy.getEnergyStored();}public int getMaxEnergyStored(){return energy.getMaxEnergyStored();}
        });
        public int getSlots(){return 3;}
        public ItemStack getStackInSlot(int slot){return (input()&&(slot==0||slot==1)||output()&&slot==2)?inventory.getStackInSlot(slot).copy():ItemStack.EMPTY;}
        public boolean isItemValid(int slot,ItemStack s){return input()&&(slot==0||slot==1)&&inventory.isItemValid(slot,s);}
        public ItemStack insertItem(int slot,ItemStack s,boolean sim){return isItemValid(slot,s)?inventory.insertItem(slot,s,sim):s;}
        public ItemStack extractItem(int slot,int amount,boolean sim){return slot==2&&output()?inventory.extractItem(slot,amount,sim):ItemStack.EMPTY;}
        public int getSlotLimit(int slot){return inventory.getSlotLimit(slot);}
    }
    @Override public Component getDisplayName(){return Component.translatable("block.domesurvival.filter_regeneration_station");}
    @Override public AbstractContainerMenu createMenu(int id,Inventory inv,Player player){return new FilterRegenerationMenu(id,inv,this);}
}
