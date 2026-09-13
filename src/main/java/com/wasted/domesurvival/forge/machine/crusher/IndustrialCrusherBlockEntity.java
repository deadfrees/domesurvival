package com.wasted.domesurvival.forge.machine.crusher;

import com.wasted.domesurvival.forge.machine.energy.MachineEnergyStorage;
import com.wasted.domesurvival.forge.machine.module.*;
import com.wasted.domesurvival.forge.recipe.IndustrialCrusherRecipe;
import com.wasted.domesurvival.forge.recipe.ModRecipes;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.energy.IEnergyStorage;
import net.minecraftforge.items.IItemHandler;
import net.minecraftforge.items.ItemStackHandler;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Optional;
import java.util.Set;

public final class IndustrialCrusherBlockEntity extends BlockEntity implements net.minecraft.world.MenuProvider, IModularMachine {
    public static final int BASE_CAPACITY = 40_000;
    public static final int MAX_RECEIVE = 256;
    public static final int DATA_ENERGY=0, DATA_CAPACITY=1, DATA_PROGRESS=2, DATA_MAX_PROGRESS=3, DATA_RECIPE_ENERGY=4, DATA_STATUS=5, DATA_COUNT=6;
    public static final int READY=0, CRUSHING=1, NO_ENERGY=2, NO_RECIPE=3, OUTPUT_FULL=4;

    private final ItemStackHandler inventory = new ItemStackHandler(3) {
        @Override public boolean isItemValid(int slot, @NotNull ItemStack stack) { return slot == 0 && isValidInput(stack); }
        @Override protected void onContentsChanged(int slot) { if (slot == 0) invalidateRecipe(); setChanged(); }
    };
    private final MachineEnergyStorage energy = new MachineEnergyStorage(BASE_CAPACITY, MAX_RECEIVE, 0);
    private final MachineModuleInventory modules = new MachineModuleInventory(this, MachineModuleResolver.STANDARD, this::modulesChanged);

    private final IItemHandler items = new IItemHandler() {
        public int getSlots(){return 3;}
        public @NotNull ItemStack getStackInSlot(int s){return inventory.getStackInSlot(s);}
        public @NotNull ItemStack insertItem(int s,@NotNull ItemStack stack,boolean sim){return s==0?inventory.insertItem(0,stack,sim):stack;}
        public @NotNull ItemStack extractItem(int s,int amount,boolean sim){return (s==1||s==2)?inventory.extractItem(s,amount,sim):ItemStack.EMPTY;}
        public int getSlotLimit(int s){return inventory.getSlotLimit(s);}
        public boolean isItemValid(int s,@NotNull ItemStack stack){return s==0&&inventory.isItemValid(0,stack);}
    };
    private final IEnergyStorage energyInput = new IEnergyStorage() {
        public int receiveEnergy(int amount,boolean sim){int r=energy.receiveEnergy(amount,sim);if(!sim&&r>0)setChanged();return r;}
        public int extractEnergy(int amount,boolean sim){return 0;}
        public int getEnergyStored(){return energy.getEnergyStored();}
        public int getMaxEnergyStored(){return energy.getMaxEnergyStored();}
        public boolean canExtract(){return false;}
        public boolean canReceive(){return true;}
    };
    private LazyOptional<IItemHandler> itemCap=LazyOptional.of(()->items);
    private LazyOptional<IEnergyStorage> energyCap=LazyOptional.of(()->energyInput);

    private int progress;
    private boolean active;
    @Nullable private ResourceLocation activeRecipe;
    private ItemStack cachedInput=ItemStack.EMPTY;
    private Optional<IndustrialCrusherRecipe> cached=Optional.empty();
    private boolean cacheValid;

    private final ContainerData data=new ContainerData(){
        public int get(int i){
            IndustrialCrusherRecipe r=currentRecipe().orElse(null);
            if(i==DATA_ENERGY)return energy.getEnergyStored();
            if(i==DATA_CAPACITY)return energy.getMaxEnergyStored();
            if(i==DATA_PROGRESS)return progress;
            if(i==DATA_MAX_PROGRESS)return r==null?0:modules.modifiers().applyProcessingTicks(r.getProcessingTime());
            if(i==DATA_RECIPE_ENERGY)return r==null?0:modules.modifiers().applyEnergyCost(r.getEnergy());
            if(i==DATA_STATUS)return status(r);
            return 0;
        }
        public void set(int i,int v){}
        public int getCount(){return DATA_COUNT;}
    };

    public IndustrialCrusherBlockEntity(BlockPos pos, BlockState state){super(IndustrialCrusherRegistry.INDUSTRIAL_CRUSHER_BLOCK_ENTITY.get(),pos,state);}
    public int moduleSlotCount(){return 2;}
    public Set<MachineModuleType> allowedModuleTypes(){return Set.of(MachineModuleType.EFFICIENCY,MachineModuleType.OVERDRIVE,MachineModuleType.BUFFER,MachineModuleType.AUTOMATION);}

    public static void serverTick(Level level,BlockPos pos,BlockState state,IndustrialCrusherBlockEntity be){
        be.active=false; boolean changed=be.tickProcess();
        if(state.getValue(IndustrialCrusherBlock.ACTIVE)!=be.active){level.setBlock(pos,state.setValue(IndustrialCrusherBlock.ACTIVE,be.active),3);changed=true;}
        if(changed)be.setChanged();
    }

    private boolean tickProcess(){
        IndustrialCrusherRecipe r=currentRecipe().orElse(null);
        if(r==null){boolean c=progress!=0||activeRecipe!=null;progress=0;activeRecipe=null;return c;}
        if(activeRecipe==null||!activeRecipe.equals(r.getId())){progress=0;activeRecipe=r.getId();}
        if(!canAccept(1,r.getResult())||!canAccept(2,r.getByproduct()))return false;
        int ticks=modules.modifiers().applyProcessingTicks(r.getProcessingTime());
        int total=modules.modifiers().applyEnergyCost(r.getEnergy());
        int need=energyStep(total,ticks);
        if(energy.getEnergyStored()<need)return false;
        energy.removeEnergyInternal(need);progress++;active=true;
        if(progress>=ticks){finish(r);progress=0;activeRecipe=null;}
        return true;
    }

    private int energyStep(int total,int ticks){int a=(int)((long)total*Math.min(progress,ticks)/ticks);int b=(int)((long)total*Math.min(progress+1,ticks)/ticks);return Math.max(0,b-a);}
    private void finish(IndustrialCrusherRecipe r){
        ItemStack in=inventory.getStackInSlot(0); if(in.getCount()<r.getInputCount()||!r.acceptsIngredient(in))return;
        in.shrink(r.getInputCount());inventory.setStackInSlot(0,in);merge(1,r.getResult());
        if(level!=null&&!r.getByproduct().isEmpty()&&level.random.nextInt(10_000)<r.getByproductChancePerTenThousand())merge(2,r.getByproduct());
    }
    private void merge(int slot,ItemStack add){if(add.isEmpty())return;ItemStack cur=inventory.getStackInSlot(slot);if(cur.isEmpty())inventory.setStackInSlot(slot,add.copy());else{cur.grow(add.getCount());inventory.setStackInSlot(slot,cur);}}
    private boolean canAccept(int slot,ItemStack add){if(add.isEmpty())return true;ItemStack cur=inventory.getStackInSlot(slot);if(cur.isEmpty())return add.getCount()<=add.getMaxStackSize();if(!ItemStack.isSameItemSameTags(cur,add))return false;return cur.getCount()+add.getCount()<=cur.getMaxStackSize();}

    private Optional<IndustrialCrusherRecipe> currentRecipe(){
        if(level==null)return Optional.empty();ItemStack in=inventory.getStackInSlot(0);if(in.isEmpty())return Optional.empty();
        if(cacheValid&&ItemStack.isSameItemSameTags(cachedInput,in))return cached;
        cached=level.getRecipeManager().getAllRecipesFor(ModRecipes.INDUSTRIAL_CRUSHER_TYPE.get()).stream().filter(r->r.acceptsIngredient(in)&&in.getCount()>=r.getInputCount()).findFirst();
        cachedInput=in.copyWithCount(1);cacheValid=true;return cached;
    }
    private boolean isValidInput(ItemStack stack){return level!=null&&level.getRecipeManager().getAllRecipesFor(ModRecipes.INDUSTRIAL_CRUSHER_TYPE.get()).stream().anyMatch(r->r.acceptsIngredient(stack));}
    private void invalidateRecipe(){cacheValid=false;cachedInput=ItemStack.EMPTY;cached=Optional.empty();}
    private int status(@Nullable IndustrialCrusherRecipe r){if(r==null)return inventory.getStackInSlot(0).isEmpty()?READY:NO_RECIPE;if(!canAccept(1,r.getResult())||!canAccept(2,r.getByproduct()))return OUTPUT_FULL;int t=modules.modifiers().applyProcessingTicks(r.getProcessingTime());int e=modules.modifiers().applyEnergyCost(r.getEnergy());return energy.getEnergyStored()<energyStep(e,t)?NO_ENERGY:(progress>0?CRUSHING:READY);}
    private void modulesChanged(){energy.setCapacityInternal(modules.modifiers().applyBufferCapacity(BASE_CAPACITY));progress=0;activeRecipe=null;setChanged();}

    public ItemStackHandler getInventory(){return inventory;} public MachineModuleInventory getModules(){return modules;} public ContainerData getDataAccess(){return data;}
    protected void saveAdditional(CompoundTag tag){super.saveAdditional(tag);tag.put("Inventory",inventory.serializeNBT());tag.put("Modules",modules.serializeNBT());tag.putInt("Energy",energy.getEnergyStored());tag.putInt("Progress",progress);if(activeRecipe!=null)tag.putString("ActiveRecipe",activeRecipe.toString());}
    public void load(CompoundTag tag){super.load(tag);inventory.deserializeNBT(tag.getCompound("Inventory"));modules.deserializeNBT(tag.getCompound("Modules"));energy.setCapacityInternal(modules.modifiers().applyBufferCapacity(BASE_CAPACITY));energy.setEnergyStoredInternal(tag.getInt("Energy"));progress=Math.max(0,tag.getInt("Progress"));activeRecipe=tag.contains("ActiveRecipe")?ResourceLocation.tryParse(tag.getString("ActiveRecipe")):null;invalidateRecipe();}

    public <T>@NotNull LazyOptional<T> getCapability(@NotNull Capability<T> cap,@Nullable Direction side){if(cap==ForgeCapabilities.ITEM_HANDLER)return itemCap.cast();if(cap==ForgeCapabilities.ENERGY)return energyCap.cast();return super.getCapability(cap,side);}
    public void invalidateCaps(){super.invalidateCaps();itemCap.invalidate();energyCap.invalidate();}
    public void reviveCaps(){super.reviveCaps();itemCap=LazyOptional.of(()->items);energyCap=LazyOptional.of(()->energyInput);}
    public Component getDisplayName(){return Component.translatable("block.domesurvival.industrial_crusher");}
    @Nullable public AbstractContainerMenu createMenu(int id,Inventory inv,Player player){return new IndustrialCrusherMenu(id,inv,this);}
}
