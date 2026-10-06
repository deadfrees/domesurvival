package com.wasted.domesurvival.forge.machine.copper;

import com.wasted.domesurvival.forge.block.CopperFurnaceBlock;
import com.wasted.domesurvival.forge.machine.module.*;
import com.wasted.domesurvival.forge.machine.side.*;
import com.wasted.domesurvival.forge.registry.ModBlockEntities;
import net.minecraft.core.*;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.*;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.AbstractFurnaceBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.common.capabilities.*;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.items.IItemHandler;
import net.minecraftforge.items.wrapper.SidedInvWrapper;
import org.jetbrains.annotations.Nullable;
import java.util.*;

/** Fuel furnace: vanilla recipes/XP/remainders, 80% cook time, 125% fuel duration. */
public final class CopperFurnaceBlockEntity extends AbstractFurnaceBlockEntity implements IModularMachine {
    public static final int DATA_COUNT=12;
    private final UnifiedSideConfig sides=new UnifiedSideConfig();
    private final MachineModuleInventory modules=new MachineModuleInventory(this,MachineModuleResolver.STANDARD,this::setChanged);
    private final EnumMap<Direction,LazyOptional<IItemHandler>> ports=new EnumMap<>(Direction.class);
    private Direction lastFacing;
    private boolean dismantling;
    private final ContainerData syncData=new ContainerData() {
        public int get(int i) {
            if(i<4)return dataAccess.get(i);
            if(i<10)return sideMode(Direction.values()[i-4]).ordinal();
            if(i==10)return hasEfficiency()?1:0;
            return status();
        }
        public void set(int i,int value){if(i<4)dataAccess.set(i,value);}
        public int getCount(){return DATA_COUNT;}
    };

    public CopperFurnaceBlockEntity(BlockPos pos,BlockState state){
        super(ModBlockEntities.COPPER_FURNACE.get(),pos,state,RecipeType.SMELTING);
        defaults();lastFacing=facing();
    }
    @Override public int moduleSlotCount(){return 1;}
    @Override public Set<MachineModuleType> allowedModuleTypes(){return Set.of(MachineModuleType.EFFICIENCY);}
    public MachineModuleInventory getModules(){return modules;}
    public ContainerData getDataAccess(){return syncData;}
    public boolean hasEfficiency(){return modules!=null&&modules.isConfigurationValid()&&
            modules.getStackInSlot(0).getItem() instanceof MachineModuleItem m&&m.module().type()==MachineModuleType.EFFICIENCY;}
    public boolean isDismantling(){return dismantling;}
    public void setDismantling(boolean value){dismantling=value;}
    @Override protected Component getDefaultName(){return Component.translatable("container.domesurvival.copper_furnace");}
    @Override protected AbstractContainerMenu createMenu(int id,Inventory inv){return new CopperFurnaceMenu(id,inv,this);}

    @Override protected int getBurnDuration(ItemStack fuel){
        long base=super.getBurnDuration(fuel);
        if(base<=0)return 0;
        // Snapshot once at ignition: hot-swapping never refills burning fuel.
        return (int)Math.min(Integer.MAX_VALUE,base*5/4*(hasEfficiency()?115:100)/100);
    }
    public int fuelDuration(ItemStack stack){return getBurnDuration(stack);}
    public static boolean acceptsInput(Level world,ItemStack stack){
        return world!=null&&!stack.isEmpty()&&world.getRecipeManager()
                .getRecipeFor(RecipeType.SMELTING,new SimpleContainer(stack),world).isPresent();
    }
    @Override public boolean canPlaceItem(int slot,ItemStack stack){
        return slot==0?acceptsInput(level,stack):slot==1&&getBurnDuration(stack)>0;
    }
    public Direction facing(){return getBlockState().getValue(CopperFurnaceBlock.FACING);}
    public SideMode sideMode(Direction side){
        if(side==null||side==facing())return SideMode.DISABLED;
        if(level!=null&&level.isClientSide)return switch(getBlockState().getValue(CopperFurnaceBlock.portProperty(side))){
            case INPUT->SideMode.INPUT;case OUTPUT->SideMode.OUTPUT;default->SideMode.DISABLED;
        };
        return sides.getMode(side);
    }
    private void defaults(){
        sides.reset();sides.setMode(Direction.UP,SideMode.INPUT);
        sides.setMode(facing().getOpposite(),SideMode.INPUT);sides.setMode(Direction.DOWN,SideMode.OUTPUT);
    }
    public void cycleSideMode(RelativeSide side){
        if(side==RelativeSide.FRONT)return;
        sides.cycleMode(side.resolve(facing()));refreshPorts();syncPorts();setChanged();
    }
    public void rotateSideConfiguration(Direction previous){
        EnumMap<RelativeSide,SideMode> copy=new EnumMap<>(RelativeSide.class);
        for(var side:RelativeSide.values())copy.put(side,sides.getMode(side.resolve(previous)));
        for(var side:RelativeSide.values())sides.setMode(side.resolve(facing()),copy.get(side));
        lastFacing=facing();refreshPorts();syncPorts();setChanged();
    }
    private void syncPorts(){
        if(level==null||level.isClientSide)return;
        sides.setMode(facing(),SideMode.DISABLED);
        BlockState state=getBlockState(),next=state;
        for(Direction side:Direction.values())next=next.setValue(CopperFurnaceBlock.portProperty(side),PortVisual.fromMode(sideMode(side)));
        if(!next.equals(state))level.setBlock(worldPosition,next,3);
    }
    @Override public void onLoad(){super.onLoad();lastFacing=facing();syncPorts();}
    @Override public int[] getSlotsForFace(Direction side){return switch(sideMode(side)){
        case INPUT->new int[]{0,1};case OUTPUT->new int[]{2,1};default->new int[0];};}
    @Override public boolean canPlaceItemThroughFace(int slot,ItemStack stack,@Nullable Direction side){
        return sideMode(side)==SideMode.INPUT&&canPlaceItem(slot,stack);
    }
    @Override public boolean canTakeItemThroughFace(int slot,ItemStack stack,Direction side){
        return sideMode(side)==SideMode.OUTPUT&&(slot==2||slot==1&&!stack.isEmpty()&&getBurnDuration(stack)<=0);
    }
    @Override public <T> LazyOptional<T> getCapability(Capability<T> cap,@Nullable Direction side){
        if(cap==ForgeCapabilities.ITEM_HANDLER){
            if(isRemoved()||sideMode(side)==SideMode.DISABLED)return LazyOptional.empty();
            return ports.computeIfAbsent(side,d->LazyOptional.of(()->new SidedInvWrapper(this,d))).cast();
        }
        if(cap==ForgeCapabilities.ENERGY)return LazyOptional.empty();
        return super.getCapability(cap,side);
    }
    private void refreshPorts(){ports.values().forEach(LazyOptional::invalidate);ports.clear();}
    @Override public void invalidateCaps(){super.invalidateCaps();refreshPorts();}
    @Override public void reviveCaps(){super.reviveCaps();refreshPorts();}

    private int status(){
        if(level==null)return 0;
        var recipe=level.getRecipeManager().getRecipeFor(RecipeType.SMELTING,this,level);
        if(recipe.isEmpty())return getItem(0).isEmpty()?0:3;
        ItemStack result=recipe.get().getResultItem(level.registryAccess()),out=getItem(2);
        if(!out.isEmpty()&&(!ItemStack.isSameItemSameTags(out,result)||out.getCount()+result.getCount()>out.getMaxStackSize()))return 4;
        return dataAccess.get(0)>0?1:2;
    }
    private void scaleCookTime(){
        if(level==null)return;
        level.getRecipeManager().getRecipeFor(RecipeType.SMELTING,this,level).ifPresent(recipe->{
            int ticks=(int)Math.max(1,Math.min(Integer.MAX_VALUE,((long)recipe.getCookingTime()*4+4)/5));
            dataAccess.set(3,ticks);
        });
    }
    public static void serverTick(Level level,BlockPos pos,BlockState state,CopperFurnaceBlockEntity furnace){
        if(furnace.lastFacing!=furnace.facing())furnace.rotateSideConfiguration(furnace.lastFacing);
        furnace.scaleCookTime();
        AbstractFurnaceBlockEntity.serverTick(level,pos,furnace.getBlockState(),furnace);
        furnace.scaleCookTime();
    }
    @Override protected void saveAdditional(CompoundTag tag){
        super.saveAdditional(tag);tag.put("Modules",modules.serializeNBT());sides.save(tag);
        // Preserve exact values instead of vanilla's short burn-time storage.
        tag.putInt("DomeBurnTime",dataAccess.get(0));tag.putInt("DomeBurnDuration",dataAccess.get(1));
        tag.putInt("DomeCookProgress",dataAccess.get(2));tag.putInt("DomeCookTotal",dataAccess.get(3));
    }
    @Override public void load(CompoundTag tag){
        super.load(tag);
        if(tag.contains("Modules"))modules.deserializeNBT(tag.getCompound("Modules"));
        else modules.setStackInSlot(0,ItemStack.EMPTY);
        if(!sides.load(tag))defaults();sides.setMode(facing(),SideMode.DISABLED);lastFacing=facing();
        if(tag.contains("DomeBurnTime"))dataAccess.set(0,Math.max(0,tag.getInt("DomeBurnTime")));
        if(tag.contains("DomeBurnDuration"))dataAccess.set(1,Math.max(0,tag.getInt("DomeBurnDuration")));
        if(tag.contains("DomeCookProgress"))dataAccess.set(2,Math.max(0,tag.getInt("DomeCookProgress")));
        if(tag.contains("DomeCookTotal"))dataAccess.set(3,Math.max(1,tag.getInt("DomeCookTotal")));
        refreshPorts();
        if(level!=null&&!level.isClientSide&&level.getBlockEntity(worldPosition)==this)syncPorts();
    }
}
