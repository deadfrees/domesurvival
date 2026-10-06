"""Derive the electrolyzer plumbing from the reviewed purifier implementation."""
from pathlib import Path
R=Path(__file__).resolve().parents[2]
J=R/'src/main/java/com/wasted/domesurvival/forge'
def rename(s):
    for a,b in [('WaterPurifier','OxygenElectrolyzer'),('WATER_PURIFIER','OXYGEN_ELECTROLYZER'),('water_purifier','oxygen_electrolyzer'),('machine.water','machine.oxygen')]:s=s.replace(a,b)
    return s
s=rename((J/'machine/water/WaterPurifierBlockEntity.java').read_text())
s=s.replace('public static final int ENERGY_CAPACITY = 20_000','public static final int ENERGY_CAPACITY = 30_000').replace('ModItems.BASIC_FILTER_ENERGY_PER_TICK','12').replace('ModItems.BASIC_FILTER_PROCESS_TICKS','200')
s=s.replace('RAW_TANK_CAPACITY','WATER_TANK_CAPACITY').replace('PURIFIED_TANK_CAPACITY','OXYGEN_CAPACITY').replace('RAW_WATER_PER_CYCLE','WATER_PER_CYCLE').replace('PURIFIED_WATER_PER_CYCLE','OXYGEN_PER_CYCLE').replace('WATER_PER_CYCLE = 250','WATER_PER_CYCLE = 200').replace('OXYGEN_PER_CYCLE = 200','OXYGEN_PER_CYCLE = 96')
s=s.replace('private static final int MAX_FLUID_OUTPUT_PER_TICK = 100;', 'public static final int MAX_OXYGEN_OUTPUT_PER_TICK = 120;')
s=s.replace('DATA_CAPACITY','DATA_ENERGY_CAPACITY').replace('DATA_RAW_WATER','DATA_WATER').replace('DATA_RAW_CAPACITY','DATA_WATER_CAPACITY').replace('DATA_PURIFIED_WATER','DATA_OXYGEN').replace('DATA_PURIFIED_CAPACITY','DATA_OXYGEN_CAPACITY').replace('DATA_SIDES_START + 8','DATA_SIDES_START + 7')
a=s.index('    public static final int SLOT_WATER_BUCKET');b=s.index('    public static final int DATA_ENERGY',a);s=s[:a]+s[b:]
s=s.replace('    public static final int STATUS_NO_FILTER = 3;\n','').replace('STATUS_NO_ENERGY = 4','STATUS_NO_ENERGY = 3').replace('STATUS_OUTPUT_FULL = 5','STATUS_OUTPUT_FULL = 4')
s=s.replace('    private static final String NBT_INVENTORY = "Inventory";\n','').replace('NBT_RAW_TANK','NBT_WATER').replace('"RawTank"','"PurifiedWater"').replace('NBT_PURIFIED_TANK','NBT_OXYGEN').replace('"PurifiedTank"','"Oxygen"')
a=s.index('    private final ItemStackHandler inventory');b=s.index('    private final MachineEnergyStorage',a);s=s[:a]+s[b:]
s=s.replace('rawWaterTank','waterTank').replace('stack.getFluid().isSame(Fluids.WATER)','stack.getFluid().isSame(ModFluids.PURIFIED_WATER.get())')
a=s.index('    private final FluidTank purifiedWaterTank');b=s.index('    private final EnumMap',a)
s=s[:a]+'    private final OxygenStorage oxygenStorage = new OxygenStorage(OXYGEN_CAPACITY, 0, MAX_OXYGEN_OUTPUT_PER_TICK);\n\n'+s[b:]
s=s.replace('rawAmount()', 'waterAmount()').replace('purifiedAmount()', 'oxygenAmount()').replace('purifiedWaterTank.getFluidAmount()','oxygenStorage.getOxygenStored()').replace('purifiedWaterTank.getCapacity()','oxygenStorage.getMaxOxygenStored()')
s=s.replace('public float pumpAngle(float partial)', 'public float animationPhase(float partial)').replace(' * 4.5F;', ' / 80F;')
a=s.index('            if (index == 16)');b=s.index('\n',a);s=s[:a]+s[b:]
s=s.replace('boolean changed = purifier.consumeWaterBucketIfPossible();','boolean changed = false;')
s=s.replace('purifier.modules.modifiers().applyEnergyCost(purifier.baseCycleEnergy())','purifier.modules.modifiers().applyEnergyCost(PROCESS_TICKS * ENERGY_PER_TICK)')
s=s.replace('        changed |= purifier.pushPurifiedWaterToNeighbors();','')
a=s.index('    private boolean pushPurifiedWaterToNeighbors()');b=s.index('    private int calculateStatus()',a);s=s[:a]+s[b:]
s=s.replace('        if (!hasUsableFilter()) return STATUS_NO_FILTER;\n','')
a=s.index('    private boolean consumeWaterBucketIfPossible()');b=s.index('    public ContainerData getDataAccess()',a)
s=s[:a]+'''    private int currentProcessTicks() { return cycleTicks > 0 ? cycleTicks : modules.modifiers().applyProcessingTicks(PROCESS_TICKS); }
    private int currentCycleEnergy() { return cycleTicks > 0 ? cycleEnergy : modules.modifiers().applyEnergyCost(PROCESS_TICKS * ENERGY_PER_TICK); }
    private void finishCycle() {
        waterTank.drain(WATER_PER_CYCLE, IFluidHandler.FluidAction.EXECUTE);
        oxygenStorage.addInternal(OXYGEN_PER_CYCLE);
    }

'''+s[b:]
s=s.replace('        tag.put(NBT_INVENTORY, inventory.serializeNBT());\n','').replace('tag.put(NBT_OXYGEN, purifiedWaterTank.writeToNBT(new CompoundTag()));','tag.putInt(NBT_OXYGEN, oxygenAmount());')
s=s.replace('        inventory.deserializeNBT(tag.getCompound(NBT_INVENTORY));\n','').replace('purifiedWaterTank.readFromNBT(tag.getCompound(NBT_OXYGEN));','oxygenStorage.setStoredInternal(tag.getInt(NBT_OXYGEN));')
s=s.replace(' && currentFilterItem() != null','').replace('cycleTicks = currentFilterItem().processTicks(); cycleEnergy = baseCycleEnergy();','cycleTicks = PROCESS_TICKS; cycleEnergy = PROCESS_TICKS * ENERGY_PER_TICK;')
s=s.replace('original cartridge timing/cost','original timing/cost').replace('Older purifier defaults exposed every resource despite five OUTPUT modes.','Older BOTH defaults were saved as five OUTPUT modes despite accepting FE/water.')
a=s.index('    @Override public <T> @NotNull LazyOptional<T> getCapability');b=s.index('    @Override public Component getDisplayName()',a)
s=s[:a]+'''    @Override public <T> @NotNull LazyOptional<T> getCapability(@NotNull Capability<T> cap, @Nullable Direction side) {
        if (cap==ForgeCapabilities.ENERGY || cap==ForgeCapabilities.FLUID_HANDLER || cap==com.wasted.domesurvival.forge.capability.ModCapabilities.OXYGEN) {
            if (isRemoved() || sideMode(side)==SideMode.DISABLED) return LazyOptional.empty();
            Port port=ports.computeIfAbsent(side,Port::new);
            if (cap==com.wasted.domesurvival.forge.capability.ModCapabilities.OXYGEN) return sideMode(side)==SideMode.OUTPUT?port.oxygen.cast():LazyOptional.empty();
            if (sideMode(side)!=SideMode.INPUT) return LazyOptional.empty();
            return cap==ForgeCapabilities.ENERGY?port.energy.cast():port.fluid.cast();
        }
        return super.getCapability(cap,side);
    }
    private void refreshCapabilities() { ports.values().forEach(p->{p.energy.invalidate();p.fluid.invalidate();p.oxygen.invalidate();});ports.clear(); }
    @Override public void invalidateCaps() { super.invalidateCaps();refreshCapabilities(); }
    @Override public void reviveCaps() { super.reviveCaps();refreshCapabilities(); }
    private final class Port implements IFluidHandler {
        final Direction side; Port(Direction side) { this.side=side; }
        boolean input() { return !isRemoved()&&sideMode(side)==SideMode.INPUT; }
        boolean output() { return !isRemoved()&&sideMode(side)==SideMode.OUTPUT; }
        final LazyOptional<IFluidHandler> fluid=LazyOptional.of(()->this);
        final LazyOptional<IEnergyStorage> energy=LazyOptional.of(()->new IEnergyStorage() {
            public int receiveEnergy(int amount,boolean simulate) { int n=input()?energyStorage.receiveEnergy(amount,simulate):0;if(n>0&&!simulate)setChanged();return n; }
            public int extractEnergy(int amount,boolean simulate) { return 0; }
            public int getEnergyStored() { return energyStorage.getEnergyStored(); }
            public int getMaxEnergyStored() { return energyStorage.getMaxEnergyStored(); }
            public boolean canReceive() { return input(); }public boolean canExtract() { return false; }
        });
        final LazyOptional<com.wasted.domesurvival.forge.capability.IOxygenStorage> oxygen=LazyOptional.of(()->new com.wasted.domesurvival.forge.capability.IOxygenStorage() {
            public int receiveOxygen(int amount,boolean simulate) { return 0; }
            public int extractOxygen(int amount,boolean simulate) { int n=output()?oxygenStorage.extractOxygen(amount,simulate):0;if(n>0&&!simulate)setChanged();return n; }
            public int getOxygenStored() { return oxygenAmount(); }public int getMaxOxygenStored() { return OXYGEN_CAPACITY; }
            public boolean canReceive() { return false; }public boolean canExtract() { return output(); }
        });
        public int getTanks() { return 1; }
        public FluidStack getFluidInTank(int tank) { return input()&&tank==0?waterTank.getFluid().copy():FluidStack.EMPTY; }
        public int getTankCapacity(int tank) { return tank==0?WATER_TANK_CAPACITY:0; }
        public boolean isFluidValid(int tank,FluidStack stack) { return input()&&tank==0&&waterTank.isFluidValid(0,stack); }
        public int fill(FluidStack stack,FluidAction action) { return input()?waterTank.fill(stack,action):0; }
        public FluidStack drain(FluidStack stack,FluidAction action) { return FluidStack.EMPTY; }
        public FluidStack drain(int amount,FluidAction action) { return FluidStack.EMPTY; }
    }
'''+s[b:]
for line in ['import com.wasted.domesurvival.forge.item.ModItems;','import com.wasted.domesurvival.forge.item.WaterFilterItem;','import net.minecraft.world.item.Items;','import net.minecraft.world.level.material.Fluids;','import net.minecraftforge.items.IItemHandler;','import net.minecraftforge.items.ItemStackHandler;']:
    s=s.replace(line+'\n','')
s=s.replace('Fallback values used only while no cartridge is installed.','Unmodified cycle balance.').replace('purifier','machine')
s=s.replace('    private int ambientSoundTick;', '    private int ambientSoundTick;\n    private boolean syncPending;\n    @Override public void setChanged() { super.setChanged(); syncPending=true; }')
s=s.replace('        if (changed) { machine.setChanged(); if (level.getGameTime() % 10 == 0) level.sendBlockUpdated(pos, machine.getBlockState(), machine.getBlockState(), 2); }', '''        if (changed) machine.setChanged();
        if (machine.syncPending && level.getGameTime() % 10 == 0) {
            level.sendBlockUpdated(pos, machine.getBlockState(), machine.getBlockState(), 2);machine.syncPending=false;
        }''')
(J/'machine/oxygen/OxygenElectrolyzerBlockEntity.java').write_text(s)

# Modules are the only inventory of this fluid/gas machine.
s=rename((J/'machine/water/WaterPurifierMenu.java').read_text())
s=s.replace('furnace.getInventory()','new ItemStackHandler(0)')
a=s.index('        addSlot(new SlotItemHandler(container,0');b=s.index('        for(int row=',a);s=s[:a]+s[b:]
s=s.replace('rawWater()','water()').replace('rawCapacity()','waterCapacity()').replace('purifiedWater()','oxygen()').replace('purifiedCapacity()','oxygenCapacity()')
s=s.replace('public int filterRemaining(){return data.get(16);}','')
a=s.index('        if(index<2||index>=38)');b=s.index('        if(stack.getCount()==copy.getCount())',a)
s=s[:a]+'''        if(index>=36){if(!moveItemStackTo(stack,0,36,true))return ItemStack.EMPTY;}
        else if(stack.getItem() instanceof MachineModuleItem){if(!isModulePanelOpen()||!moveItemStackTo(stack,36,38,false))return ItemStack.EMPTY;}
        else if(index<27){if(!moveItemStackTo(stack,27,36,false))return ItemStack.EMPTY;}
        else if(!moveItemStackTo(stack,0,27,false))return ItemStack.EMPTY;
'''+s[b:]
(J/'machine/oxygen/OxygenElectrolyzerMenu.java').write_text(s)

s=rename((J/'machine/water/WaterPurifierBlock.java').read_text())
a=s.index('                for (int slot = 0; slot < purifier.getInventory()');b=s.index('\n            }',a);s=s[:a]+s[b:]
s=s.replace('both tanks, inventory and upgrades','water, oxygen, energy and upgrades')
s=s.replace('Industrial water purifier','Industrial electrolyzer')
(J/'machine/oxygen/OxygenElectrolyzerBlock.java').write_text(s)
print('Electrolyzer block, cycle, ports and menu prepared')
