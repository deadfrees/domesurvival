package com.wasted.domesurvival.forge.machine.shaft;

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

public final class CokeOvenMenu extends AbstractContainerMenu {
    public static final int MAIN_TAB=201, MODULE_TAB=200, SIDE_TAB=202;
    private int tab=MAIN_TAB;
    private final Level level;
    private final BlockPos pos;
    private final CokeOvenBlockEntity furnace;
    private final ContainerData data;
    public CokeOvenMenu(int id,Inventory inv,FriendlyByteBuf extra){this(id,inv,null,new ItemStackHandler(3),new SimpleContainerData(CokeOvenBlockEntity.DATA_COUNT),extra.readBlockPos());}
    public CokeOvenMenu(int id,Inventory inv,CokeOvenBlockEntity furnace){this(id,inv,furnace,furnace.getInventory(),furnace.getDataAccess(),furnace.getBlockPos());}
    private CokeOvenMenu(int id,Inventory inv,CokeOvenBlockEntity furnace,IItemHandler container,ContainerData data,BlockPos pos){
        super(ModMenuTypes.COKE_OVEN.get(),id);this.furnace=furnace;this.data=data;this.pos=pos;level=inv.player.level();
        addDataSlots(new ContainerData(){
            public int get(int i){return (data.get(i/2)>>>((i%2)*16))&65535;}
            public void set(int i,int v){int shift=i%2*16;data.set(i/2,(data.get(i/2)&~(65535<<shift))|((v&65535)<<shift));}
            public int getCount(){return data.getCount()*2;}
        });
        addSlot(new SlotItemHandler(container,0,46,55){
            public boolean isActive(){return isMainPanelOpen();}public boolean mayPickup(Player p){return isActive();}
            public boolean mayPlace(ItemStack s){return isActive()&&CokeOvenBlockEntity.isValidCoal(s);}
        });
        addSlot(new SlotItemHandler(container,1,46,105){
            public boolean isActive(){return isMainPanelOpen();}public boolean mayPickup(Player p){return isActive();}
            public boolean mayPlace(ItemStack s){return isActive()&&CokeOvenBlockEntity.isValidFuel(s);}
        });
        addSlot(new SlotItemHandler(container,2,182,79){
            public boolean isActive(){return isMainPanelOpen();}public boolean mayPickup(Player p){return isActive();}
            public boolean mayPlace(ItemStack s){return false;}
        });
        for(int row=0;row<3;row++)for(int col=0;col<9;col++)addSlot(new Slot(inv,col+row*9+9,14+col*22,161+row*22));
        for(int col=0;col<9;col++)addSlot(new Slot(inv,col,14+col*22,229));
        addSlot(new SlotItemHandler(furnace==null?new ItemStackHandler(1):furnace.getModules(),0,22,67){
            public boolean isActive(){return isModulePanelOpen();}public boolean mayPickup(Player p){return isActive();}
            public int getMaxStackSize(){return 1;}
            public boolean mayPlace(ItemStack s){return isActive()&&s.getItem() instanceof MachineModuleItem m&&m.module().type()==MachineModuleType.EFFICIENCY&&super.mayPlace(s);}
        });
    }
    public boolean isMainPanelOpen(){return tab==MAIN_TAB;}
    public boolean isModulePanelOpen(){return tab==MODULE_TAB;}
    public boolean isSidePanelOpen(){return tab==SIDE_TAB;}
    public void setTab(int tab){this.tab=tab;}
    public BlockPos getBlockPos(){return pos;}
    public int burnRemaining(){return data.get(2);}public int burnTotal(){return data.get(3);}
    public int progress(){return data.get(0);}public int progressMax(){return data.get(1);}
    public boolean efficiency(){return data.get(10)!=0;}public int status(){return data.get(11);}
    public SideMode getSideMode(RelativeSide side){
        if(side==RelativeSide.FRONT)return SideMode.DISABLED;
        var state=level.getBlockState(pos);
        Direction facing=state.hasProperty(AbstractFurnaceBlock.FACING)?state.getValue(AbstractFurnaceBlock.FACING):Direction.NORTH;
        int value=data.get(4+side.resolve(facing).ordinal());
        return value>=0&&value<SideMode.values().length?SideMode.values()[value]:SideMode.DISABLED;
    }
    @Override public boolean stillValid(Player player){return stillValid(ContainerLevelAccess.create(level,pos),player,ModBlocks.COKE_OVEN.get());}
    @Override public boolean clickMenuButton(Player player,int id){
        if(!stillValid(player))return false;
        if(id==MAIN_TAB||id==MODULE_TAB||id==SIDE_TAB){setTab(id);return true;}
        int side=id-100;
        if(!isSidePanelOpen()||side<0||side>=RelativeSide.values().length||RelativeSide.values()[side]==RelativeSide.FRONT)return false;
        if(furnace!=null)furnace.cycleSideMode(RelativeSide.values()[side]);return true;
    }
    @Override public ItemStack quickMoveStack(Player player,int index){
        if(index<0||index>=slots.size())return ItemStack.EMPTY;
        Slot slot=slots.get(index);if(!slot.isActive()||!slot.hasItem()||!slot.mayPickup(player))return ItemStack.EMPTY;
        ItemStack stack=slot.getItem(),copy=stack.copy();
        if(index<3||index==39){if(!moveItemStackTo(stack,3,39,true))return ItemStack.EMPTY;}
        else if(stack.getItem() instanceof MachineModuleItem){if(!isModulePanelOpen()||!moveItemStackTo(stack,39,40,false))return ItemStack.EMPTY;}
        else if(isMainPanelOpen()&&CokeOvenBlockEntity.isValidCoal(stack)){if(!moveItemStackTo(stack,0,1,false))return ItemStack.EMPTY;}
        else if(isMainPanelOpen()&&CokeOvenBlockEntity.isValidFuel(stack)){if(!moveItemStackTo(stack,1,2,false))return ItemStack.EMPTY;}
        else if(index<30){if(!moveItemStackTo(stack,30,39,false))return ItemStack.EMPTY;}
        else if(!moveItemStackTo(stack,3,30,false))return ItemStack.EMPTY;
        if(stack.getCount()==copy.getCount())return ItemStack.EMPTY;
        if(index==2)slot.onQuickCraft(stack,copy);
        if(stack.isEmpty())slot.set(ItemStack.EMPTY);else slot.setChanged();
        slot.onTake(player,stack);return copy;
    }
}
