package com.wasted.domesurvival.forge.machine.oxygen;

import com.wasted.domesurvival.forge.block.ModBlocks;
import com.wasted.domesurvival.forge.machine.module.*;
import com.wasted.domesurvival.forge.machine.side.*;
import com.wasted.domesurvival.forge.registry.ModMenuTypes;
import net.minecraft.core.*;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.*;
import net.minecraft.world.entity.player.*;
import net.minecraft.world.inventory.*;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.AbstractFurnaceBlock;
import net.minecraft.world.level.block.entity.AbstractFurnaceBlockEntity;
import net.minecraftforge.items.*;

public final class OxygenFillerMenu extends AbstractContainerMenu {
    public static final int MAIN_TAB=201, MODULE_TAB=200, SIDE_TAB=202;
    private int tab=MAIN_TAB;
    private final Level level;
    private final BlockPos pos;
    private final OxygenFillerBlockEntity furnace;
    private final ContainerData data;
    public OxygenFillerMenu(int id,Inventory inv,FriendlyByteBuf extra){this(id,inv,null,new ItemStackHandler(2),new SimpleContainerData(OxygenFillerBlockEntity.DATA_COUNT),extra.readBlockPos());}
    public OxygenFillerMenu(int id,Inventory inv,OxygenFillerBlockEntity furnace){this(id,inv,furnace,furnace.getInventory(),furnace.getDataAccess(),furnace.getBlockPos());}
    private OxygenFillerMenu(int id,Inventory inv,OxygenFillerBlockEntity furnace,IItemHandler container,ContainerData data,BlockPos pos){
        super(ModMenuTypes.OXYGEN_FILLER.get(),id);this.furnace=furnace;this.data=data;this.pos=pos;level=inv.player.level();
        addDataSlots(new ContainerData(){
            public int get(int i){return (data.get(i/2)>>>((i%2)*16))&65535;}
            public void set(int i,int v){int shift=i%2*16;data.set(i/2,(data.get(i/2)&~(65535<<shift))|((v&65535)<<shift));}
            public int getCount(){return data.getCount()*2;}
        });
        for(int row=0;row<3;row++)for(int col=0;col<9;col++)addSlot(new Slot(inv,col+row*9+9,14+col*22,161+row*22));
        for(int col=0;col<9;col++)addSlot(new Slot(inv,col,14+col*22,229));
        addSlot(new SlotItemHandler(container,0,52,103){
            public boolean isActive(){return isMainPanelOpen()&&getOperatingMode()==OxygenFillerMode.TANK_FILLING;}
            public boolean mayPickup(Player player){return isActive();}
            public boolean mayPlace(ItemStack stack){return isActive()&&OxygenFillerBlockEntity.acceptsTank(stack);}
            public int getMaxStackSize(){return 1;}
        });
        addSlot(new SlotItemHandler(container,1,180,103){
            public boolean isActive(){return isMainPanelOpen()&&getOperatingMode()==OxygenFillerMode.TANK_FILLING;}
            public boolean mayPickup(Player player){return isActive();}
            public boolean mayPlace(ItemStack stack){return false;}
        });
        for(int i=0;i<2;i++)addSlot(new SlotItemHandler(furnace==null?new ItemStackHandler(2):furnace.getModules(),i,22,67+i*30){
            public boolean isActive(){return isModulePanelOpen();}
            public boolean mayPickup(Player p){return isActive()&&(!(getItem().getItem() instanceof MachineModuleItem m)||m.module().type()!=MachineModuleType.BUFFER||energyStored()<=OxygenFillerBlockEntity.ENERGY_CAPACITY);}
            public int getMaxStackSize(){return 1;}
            public boolean mayPlace(ItemStack s){return isActive()&&s.getItem() instanceof MachineModuleItem&&super.mayPlace(s);}
        });
    }
    public boolean isMainPanelOpen(){return tab==MAIN_TAB;}
    public boolean isModulePanelOpen(){return tab==MODULE_TAB;}
    public boolean isSidePanelOpen(){return tab==SIDE_TAB;}
    public void setTab(int tab){this.tab=tab;}
    public BlockPos getBlockPos(){return pos;}
    public int energyStored(){return data.get(0);}public int energyCapacity(){return data.get(1);}
    public int oxygen(){return data.get(2);}public int oxygenCapacity(){return data.get(3);}
    public int tankOxygen(){return data.get(4);}public int tankCapacity(){return data.get(5);}
    public int status(){return data.get(6);}public int fillEnergyCost(){return data.get(18);}
    public OxygenFillerMode getOperatingMode(){return OxygenFillerMode.byOrdinal(data.get(7));}
    public int roomVolume(){return data.get(9);}public int roomOxygen(){return data.get(10);}public int roomCapacity(){return data.get(11);}
    public SideMode getSideMode(RelativeSide side){
        if(side==RelativeSide.FRONT)return SideMode.DISABLED;
        var state=level.getBlockState(pos);
        Direction facing=state.hasProperty(AbstractFurnaceBlock.FACING)?state.getValue(AbstractFurnaceBlock.FACING):Direction.NORTH;
        int value=data.get(12+side.resolve(facing).ordinal());
        return value>=0&&value<SideMode.values().length?SideMode.values()[value]:SideMode.DISABLED;
    }
    @Override public boolean stillValid(Player player){return stillValid(ContainerLevelAccess.create(level,pos),player,ModBlocks.OXYGEN_FILLER.get());}
    @Override public boolean clickMenuButton(Player player,int id){
        if(!stillValid(player))return false;
        if(id==50 && isMainPanelOpen()){if(furnace!=null)furnace.cycleOperatingMode();return true;}
        if(id==MAIN_TAB||id==MODULE_TAB||id==SIDE_TAB){setTab(id);return true;}
        int side=id-100;
        if(!isSidePanelOpen()||side<0||side>=RelativeSide.values().length||RelativeSide.values()[side]==RelativeSide.FRONT)return false;
        if(furnace!=null)furnace.cycleSideMode(RelativeSide.values()[side]);return true;
    }
    @Override public ItemStack quickMoveStack(Player player,int index){
        if(index<0||index>=slots.size())return ItemStack.EMPTY;
        Slot slot=slots.get(index);if(!slot.isActive()||!slot.hasItem()||!slot.mayPickup(player))return ItemStack.EMPTY;
        ItemStack stack=slot.getItem(),copy=stack.copy();
        if(index>=36){if(!moveItemStackTo(stack,0,36,true))return ItemStack.EMPTY;}
        else if(stack.getItem() instanceof MachineModuleItem){if(!isModulePanelOpen()||!moveItemStackTo(stack,38,40,false))return ItemStack.EMPTY;}
        else if(OxygenFillerBlockEntity.acceptsTank(stack)&&isMainPanelOpen()&&getOperatingMode()==OxygenFillerMode.TANK_FILLING){if(!moveItemStackTo(stack,36,37,false))return ItemStack.EMPTY;}
        else if(index<27){if(!moveItemStackTo(stack,27,36,false))return ItemStack.EMPTY;}
        else if(!moveItemStackTo(stack,0,27,false))return ItemStack.EMPTY;
        if(stack.getCount()==copy.getCount())return ItemStack.EMPTY;
        if(stack.isEmpty())slot.set(ItemStack.EMPTY);else slot.setChanged();
        slot.onTake(player,stack);return copy;
    }
}
