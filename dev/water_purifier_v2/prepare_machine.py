"""One-time adaptation of the existing purifier. Do not rerun after manual edits."""
from pathlib import Path
import json, hashlib
R=Path(__file__).resolve().parents[2]
J=R/'src/main/java/com/wasted/domesurvival/forge'
out=Path(__file__).parent
path=J/'machine/water/WaterPurifierBlockEntity.java'
s=path.read_text()
(out/'original_block_entity.java.txt').write_text(s)
s=s.replace('import com.wasted.domesurvival.forge.fluid.ModFluids;', 'import com.wasted.domesurvival.forge.fluid.ModFluids;\nimport com.wasted.domesurvival.forge.machine.module.*;\nimport java.util.*;')
s=s.replace('implements net.minecraft.world.MenuProvider {','implements net.minecraft.world.MenuProvider, IModularMachine {')
s=s.replace('DATA_COUNT = DATA_SIDES_START + 6','DATA_COUNT = DATA_SIDES_START + 8')
s=s.replace('WaterPurifierBlockEntity.this.progress = 0;', 'WaterPurifierBlockEntity.this.progress = 0;\n                cycleTicks = cycleEnergy = 0;')
start=s.index('    private final IEnergyStorage energyInputView')
end=s.index('    private int progress;',start)
s=s[:start]+'''    private final EnumMap<Direction, Port> ports = new EnumMap<>(Direction.class);
    private Direction lastFacing;
    private final MachineModuleInventory modules = new MachineModuleInventory(this, MachineModuleResolver.STANDARD, this::modulesChanged);
    private int cycleTicks, cycleEnergy;
    @Override public int moduleSlotCount() { return 2; }
    @Override public Set<MachineModuleType> allowedModuleTypes() { return Set.of(MachineModuleType.BUFFER, MachineModuleType.EFFICIENCY, MachineModuleType.OVERDRIVE); }
    public MachineModuleInventory getModules() { return modules; }
    private void modulesChanged() { energyStorage.setCapacityInternal(modules.modifiers().applyBufferCapacity(ENERGY_CAPACITY)); setChanged(); }
    private float animationTick, previousAnimationTick;
    public void clientAnimationTick() { previousAnimationTick = animationTick; if (getBlockState().getValue(WaterPurifierBlock.LIT)) animationTick = (animationTick + 1) % 80; }
    public float pumpAngle(float partial) { float next = animationTick < previousAnimationTick ? animationTick + 80 : animationTick; return (previousAnimationTick + (next - previousAnimationTick) * partial) * 4.5F; }
    public int rawAmount() { return rawWaterTank.getFluidAmount(); }
    public int purifiedAmount() { return purifiedWaterTank.getFluidAmount(); }
    private int nextEnergyCost() { int ticks = currentProcessTicks(), total = currentCycleEnergy(); return (int)((long)total * (progress + 1) / ticks - (long)total * progress / ticks); }

'''+s[end:]
s=s.replace('if (index == DATA_STATUS) return status;', 'if (index == DATA_STATUS) return status;\n            if (index == 15) return currentCycleEnergy();\n            if (index == 16) return hasUsableFilter() ? inventory.getStackInSlot(SLOT_FILTER).getMaxDamage() - inventory.getStackInSlot(SLOT_FILTER).getDamageValue() : 0;')
s=s.replace('return sideConfig.getMode(Direction.values()[index - DATA_SIDES_START]).ordinal();','return sideMode(Direction.values()[index - DATA_SIDES_START]).ordinal();')
s=s.replace('        applyDefaultSideConfiguration();\n    }','        applyDefaultSideConfiguration(); lastFacing = getMachineFacing();\n    }',1)
a=s.index('        for (RelativeSide relative : RelativeSide.values())')
b=s.index('\n    public static boolean isConfigurableSide',a)
s=s[:a]+'''        sideConfig.setMode(Direction.UP, SideMode.INPUT);
        sideConfig.setMode(facing.getCounterClockWise(), SideMode.INPUT);
        sideConfig.setMode(Direction.DOWN, SideMode.OUTPUT);
        sideConfig.setMode(facing.getClockWise(), SideMode.OUTPUT);
        sideConfig.setMode(facing.getOpposite(), SideMode.OUTPUT);
    }
'''+s[b:]
s=s.replace('        purifier.syncAllPortStates();','        if (purifier.lastFacing != purifier.getMachineFacing()) purifier.rotateSideConfiguration(purifier.lastFacing);\n        purifier.syncAllPortStates();',1)
s=s.replace('            int energyPerTick = purifier.currentEnergyPerTick();\n            int processTicks = purifier.currentProcessTicks();','''            if (purifier.cycleTicks == 0) {
                purifier.cycleTicks = purifier.currentProcessTicks();
                purifier.cycleEnergy = purifier.modules.modifiers().applyEnergyCost(purifier.baseCycleEnergy());
            }
            int energyPerTick = purifier.nextEnergyCost();
            int processTicks = purifier.currentProcessTicks();''')
s=s.replace('                    purifier.progress = 0;','                    purifier.progress = 0; purifier.cycleTicks = purifier.cycleEnergy = 0;',1)
s=s.replace('        } else if (purifier.progress != 0 && newStatus != STATUS_NO_ENERGY) {\n            purifier.progress = 0;\n            changed = true;\n        }','        }')
s=s.replace('        boolean shouldBeLit = purifier.status == STATUS_RUNNING;','        boolean shouldBeLit = newStatus == STATUS_RUNNING;')
s=s.replace('        if (state.getValue(WaterPurifierBlock.LIT) != shouldBeLit) {\n            level.setBlock(pos, state.setValue(WaterPurifierBlock.LIT, shouldBeLit), 3);','        BlockState current = purifier.getBlockState();\n        if (current.getValue(WaterPurifierBlock.LIT) != shouldBeLit) {\n            level.setBlock(pos, current.setValue(WaterPurifierBlock.LIT, shouldBeLit), 3);')
s=s.replace('        if (changed) purifier.setChanged();','        if (changed) { purifier.setChanged(); if (level.getGameTime() % 10 == 0) level.sendBlockUpdated(pos, purifier.getBlockState(), purifier.getBlockState(), 2); }',1)
s=s.replace('            if (isFrontWorldSide(direction)) continue;','            if (sideMode(direction) != SideMode.OUTPUT) continue;')
s=s.replace('energyStorage.getEnergyStored() < currentEnergyPerTick()', 'energyStorage.getEnergyStored() < nextEnergyCost()')
s=s.replace('        return filter != null ? filter.processTicks() : PROCESS_TICKS;', '        return cycleTicks > 0 ? cycleTicks : modules.modifiers().applyProcessingTicks(filter != null ? filter.processTicks() : PROCESS_TICKS);')
start=s.index('    private int currentEnergyPerTick()')
end=s.index('    private void finishCycle()',start)
s=s[:start]+'''    private int baseCycleEnergy() { WaterFilterItem filter = currentFilterItem(); return filter == null ? PROCESS_TICKS * ENERGY_PER_TICK : filter.processTicks() * filter.energyPerTick(); }
    private int currentCycleEnergy() { return cycleTicks > 0 ? cycleEnergy : modules.modifiers().applyEnergyCost(baseCycleEnergy()); }

'''+s[end:]
s=s.replace('refreshCapabilities(); syncPortState(worldSide); setChanged(); return mode;', 'routingChanged(); return mode;')
start=s.index('    private boolean isFrontWorldSide')
s=s[:start]+'''    public SideMode sideMode(@Nullable Direction side) { return side == null || side == getMachineFacing() ? SideMode.DISABLED : sideConfig.getMode(side); }
    public void rotateSideConfiguration(Direction previous) {
        EnumMap<RelativeSide, SideMode> old = new EnumMap<>(RelativeSide.class);
        for (RelativeSide side : RelativeSide.values()) old.put(side, sideConfig.getMode(side.resolve(previous)));
        for (RelativeSide side : RelativeSide.values()) sideConfig.setMode(side.resolve(getMachineFacing()), old.get(side));
        sideConfig.setMode(getMachineFacing(), SideMode.DISABLED); lastFacing = getMachineFacing(); routingChanged();
    }
    private void routingChanged() { refreshCapabilities(); syncAllPortStates(); setChanged(); if (level != null) { level.sendBlockUpdated(worldPosition,getBlockState(),getBlockState(),3); level.updateNeighborsAt(worldPosition,getBlockState().getBlock()); } }
    private boolean isFrontWorldSide(Direction side) { return side == getMachineFacing(); }

'''+s[s.index('    private void syncPortState',start):]
s=s.replace('sideConfig.save(tag);','sideConfig.save(tag);\n        tag.put("Modules", modules.serializeNBT()); tag.putInt("CycleTicks", cycleTicks); tag.putInt("CycleEnergy", cycleEnergy); tag.putString("PortFacing", getMachineFacing().getName());')
s=s.replace('        energyStorage.setEnergyStoredInternal(tag.getInt(NBT_ENERGY));','        if (tag.contains("Modules")) modules.deserializeNBT(tag.getCompound("Modules")); else for (int i=0;i<2;i++) modules.setStackInSlot(i,ItemStack.EMPTY);\n        modulesChanged(); energyStorage.setEnergyStoredInternal(tag.getInt(NBT_ENERGY));')
s=s.replace('        progress = Math.max(0, Math.min(currentProcessTicks() - 1, tag.getInt(NBT_PROGRESS)));', '''        cycleTicks = Math.max(0, tag.getInt("CycleTicks")); cycleEnergy = Math.max(0, tag.getInt("CycleEnergy"));
        // A legacy unfinished cycle retains its original cartridge timing/cost.
        if (cycleTicks == 0 && tag.getInt(NBT_PROGRESS) > 0 && currentFilterItem() != null) { cycleTicks = currentFilterItem().processTicks(); cycleEnergy = baseCycleEnergy(); }
        progress = Math.max(0, Math.min(currentProcessTicks() - 1, tag.getInt(NBT_PROGRESS)));''')
s=s.replace('        sideConfig.setMode(getMachineFacing(), SideMode.DISABLED);\n        status', '''        Direction savedFacing = Direction.byName(tag.getString("PortFacing"));
        if (savedFacing != null && savedFacing.getAxis().isHorizontal() && savedFacing != getMachineFacing()) {
            EnumMap<RelativeSide,SideMode> old=new EnumMap<>(RelativeSide.class);
            for (RelativeSide side:RelativeSide.values()) old.put(side,sideConfig.getMode(side.resolve(savedFacing)));
            for (RelativeSide side:RelativeSide.values()) sideConfig.setMode(side.resolve(getMachineFacing()),old.get(side));
        }
        for (Direction side:Direction.values()) if (sideConfig.getMode(side)==SideMode.BOTH) sideConfig.setMode(side,side==Direction.UP||side==getMachineFacing().getCounterClockWise()?SideMode.INPUT:SideMode.OUTPUT);
        sideConfig.setMode(getMachineFacing(), SideMode.DISABLED); lastFacing=getMachineFacing(); refreshCapabilities();
        status''')
start=s.index('    @Override public <T> @NotNull LazyOptional<T> getCapability')
end=s.index('    @Override public Component getDisplayName()',start)
s=s[:start]+'''    @Override public CompoundTag getUpdateTag() { return saveWithoutMetadata(); }
    @Override public net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket getUpdatePacket() { return net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket.create(this); }
    @Override public <T> @NotNull LazyOptional<T> getCapability(@NotNull Capability<T> cap, @Nullable Direction side) {
        if (cap==ForgeCapabilities.ENERGY || cap==ForgeCapabilities.FLUID_HANDLER || cap==ForgeCapabilities.ITEM_HANDLER) {
            if (isRemoved() || sideMode(side)==SideMode.DISABLED) return LazyOptional.empty();
            Port port=ports.computeIfAbsent(side,Port::new);
            if (cap==ForgeCapabilities.ENERGY) return sideMode(side)==SideMode.INPUT?port.energy.cast():LazyOptional.empty();
            return cap==ForgeCapabilities.FLUID_HANDLER?port.fluid.cast():port.items.cast();
        }
        return super.getCapability(cap,side);
    }
    private void refreshCapabilities() { ports.values().forEach(p->{p.energy.invalidate();p.fluid.invalidate();p.items.invalidate();});ports.clear(); }
    @Override public void invalidateCaps() { super.invalidateCaps();refreshCapabilities(); }
    @Override public void reviveCaps() { super.reviveCaps();refreshCapabilities(); }
    private final class Port implements IEnergyStorage, IFluidHandler, IItemHandler {
        final Direction side; Port(Direction side) { this.side=side; }
        final LazyOptional<IEnergyStorage> energy=LazyOptional.of(()->this);
        final LazyOptional<IFluidHandler> fluid=LazyOptional.of(()->this);
        final LazyOptional<IItemHandler> items=LazyOptional.of(()->this);
        boolean input() { return !isRemoved()&&sideMode(side)==SideMode.INPUT; }
        boolean output() { return !isRemoved()&&sideMode(side)==SideMode.OUTPUT; }
        public int receiveEnergy(int amount,boolean simulate) { int accepted=input()?energyStorage.receiveEnergy(amount,simulate):0; if(accepted>0&&!simulate)setChanged();return accepted; }
        public int extractEnergy(int amount,boolean simulate) { return 0; }
        public int getEnergyStored() { return energyStorage.getEnergyStored(); }
        public int getMaxEnergyStored() { return energyStorage.getMaxEnergyStored(); }
        public boolean canReceive() { return input(); } public boolean canExtract() { return false; }
        public int getTanks() { return 1; }
        public FluidStack getFluidInTank(int tank) { return (input()?rawWaterTank.getFluid():output()?purifiedWaterTank.getFluid():FluidStack.EMPTY).copy(); }
        public int getTankCapacity(int tank) { return input()?RAW_TANK_CAPACITY:PURIFIED_TANK_CAPACITY; }
        public boolean isFluidValid(int tank,FluidStack stack) { return input()&&rawWaterTank.isFluidValid(0,stack); }
        public int fill(FluidStack stack,FluidAction action) { return input()?rawWaterTank.fill(stack,action):0; }
        public FluidStack drain(FluidStack stack,FluidAction action) { return output()?purifiedWaterTank.drain(stack,action):FluidStack.EMPTY; }
        public FluidStack drain(int amount,FluidAction action) { return output()?purifiedWaterTank.drain(amount,action):FluidStack.EMPTY; }
        public int getSlots() { return 2; }
        public ItemStack getStackInSlot(int slot) { return inventory.getStackInSlot(slot).copy(); }
        public int getSlotLimit(int slot) { return inventory.getSlotLimit(slot); }
        public boolean isItemValid(int slot,ItemStack stack) { return input()&&inventory.isItemValid(slot,stack); }
        public ItemStack insertItem(int slot,ItemStack stack,boolean simulate) { return isItemValid(slot,stack)?inventory.insertItem(slot,stack,simulate):stack; }
        public ItemStack extractItem(int slot,int amount,boolean simulate) {
            ItemStack stack=inventory.getStackInSlot(slot);
            boolean used=slot==SLOT_WATER_BUCKET&&stack.is(Items.BUCKET)||slot==SLOT_FILTER&&stack.getItem() instanceof WaterFilterItem&&stack.getDamageValue()>=stack.getMaxDamage();
            return output()&&used?inventory.extractItem(slot,amount,simulate):ItemStack.EMPTY;
        }
    }
'''+s[end:]
path.write_text(s)
print('WATER_PURIFIER_MECHANICS_PREPARED')
