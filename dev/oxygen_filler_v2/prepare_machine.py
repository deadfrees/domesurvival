"""Retain the established room simulation while upgrading the filler's own routing."""
from pathlib import Path
R=Path(__file__).resolve().parents[2]; J=R/'src/main/java/com/wasted/domesurvival/forge'; D=Path(__file__).parent
p=J/'machine/oxygen/OxygenFillerBlockEntity.java'
baseline=D/'original_block_entity.java.txt'
if not baseline.exists(): baseline.write_text(p.read_text(encoding='utf-8'),encoding='utf-8')
s=baseline.read_text(encoding='utf-8')
s=s.replace('import com.wasted.domesurvival.forge.item.ModItems;', 'import com.wasted.domesurvival.forge.item.ModItems;\nimport com.wasted.domesurvival.forge.machine.module.*;\nimport java.util.*;')
s=s.replace('implements MenuProvider {','implements MenuProvider, IModularMachine {')
s=s.replace('public static final int SLOT_TANK = 0;', 'public static final int SLOT_TANK = 0, SLOT_OUTPUT = 1;')
s=s.replace('DATA_COUNT = DATA_SIDES_START + 6','DATA_COUNT = DATA_SIDES_START + 7')
s=s.replace('public static final int STATUS_IDLE = 0;', 'public static final int STATUS_OUTPUT_FULL = 14;\n    public static final int STATUS_IDLE = 0;')
s=s.replace('new ItemStackHandler(1)', 'new ItemStackHandler(2)')
s=s.replace('return slot == SLOT_TANK && stack.getItem() instanceof OxygenTankItem;', 'return slot == SLOT_TANK && acceptsTank(stack);')
s=s.replace('        @Override\n        protected void onContentsChanged(int slot)', '        @Override public int getSlotLimit(int slot) { return 1; }\n        @Override\n        protected void onContentsChanged(int slot)')
a=s.index('    private final IEnergyStorage energyInputView');b=s.index('    private long oxygenOutputBudgetGameTime',a)
s=s[:a]+'''    private final EnumMap<Direction, Port> ports = new EnumMap<>(Direction.class);
    private final MachineModuleInventory modules = new MachineModuleInventory(this, MachineModuleResolver.STANDARD, this::modulesChanged);
    private Direction lastFacing;
    private boolean syncPending;
    private float animationTick, previousAnimationTick;
    @Override public int moduleSlotCount() { return 2; }
    @Override public Set<MachineModuleType> allowedModuleTypes() { return Set.of(MachineModuleType.BUFFER, MachineModuleType.EFFICIENCY); }
    public MachineModuleInventory getModules() { return modules; }
    private void modulesChanged() { energyStorage.setCapacityInternal(modules.modifiers().applyBufferCapacity(ENERGY_CAPACITY)); setChanged(); }
    @Override public void setChanged() { super.setChanged(); syncPending = true; }
    public int fillEnergyCost() { return modules.modifiers().applyEnergyCost(ENERGY_PER_FILL_TICK); }
    public int oxygenAmount() { return oxygenStorage.getOxygenStored(); }
    public static boolean acceptsTank(ItemStack stack) { return stack.getItem() instanceof OxygenTankItem tank && tank.getOxygen(stack) < tank.capacity(); }
    public float animationPhase(float partial) { float next=animationTick<previousAnimationTick?animationTick+80:animationTick;return (previousAnimationTick+(next-previousAnimationTick)*partial)/80F; }
    private boolean moveCompletedTank() {
        ItemStack stack=inventory.getStackInSlot(SLOT_TANK);
        if (!(stack.getItem() instanceof OxygenTankItem tank) || tank.getOxygen(stack)<tank.capacity() || !inventory.getStackInSlot(SLOT_OUTPUT).isEmpty()) return false;
        inventory.setStackInSlot(SLOT_OUTPUT,stack.copy()); inventory.setStackInSlot(SLOT_TANK,ItemStack.EMPTY); return true;
    }

'''+s[b:]
s=s.replace('            if (index == DATA_ENERGY)', '            if (index == 18) return fillEnergyCost();\n            if (index == DATA_ENERGY)')
s=s.replace('        applyDefaultSideConfiguration();\n    }','        applyDefaultSideConfiguration(); lastFacing=getMachineFacing();\n    }',1)
s=s.replace('relative == RelativeSide.FRONT ? SideMode.DISABLED : SideMode.INPUT','relative == RelativeSide.FRONT ? SideMode.DISABLED : relative == RelativeSide.RIGHT || relative == RelativeSide.BOTTOM ? SideMode.OUTPUT : SideMode.INPUT')
s=s.replace('        machine.syncAllPortStates();\n        boolean changed = false;', '        if(machine.lastFacing!=machine.getMachineFacing())machine.rotateSideConfiguration(machine.lastFacing);\n        machine.syncAllPortStates();\n        boolean changed = machine.operatingMode==OxygenFillerMode.TANK_FILLING && machine.moveCompletedTank();')
s=s.replace('removeEnergyInternal(ENERGY_PER_FILL_TICK) == ENERGY_PER_FILL_TICK','removeEnergyInternal(machine.fillEnergyCost()) == machine.fillEnergyCost()')
s=s.replace('        machine.status = machine.calculateStatus();\n\n        // The room', '        if(machine.operatingMode==OxygenFillerMode.TANK_FILLING)changed |= machine.moveCompletedTank();\n        machine.status = machine.calculateStatus();\n\n        // The room')
s=s.replace('if (state.getValue(OxygenFillerBlock.LIT) != visualWorking)', 'state=level.getBlockState(pos);\n        if (state.getValue(OxygenFillerBlock.LIT) != visualWorking)')
s=s.replace('        if (changed) {\n            machine.setChanged();\n        }', '        if (changed) machine.setChanged();\n        if(machine.syncPending && level.getGameTime()%10==0){machine.syncPending=false;machine.syncOperatingModeToClient();}')
s=s.replace('if (tank.getOxygen(stack) >= tank.capacity()) return STATUS_TANK_FULL;', 'if (tank.getOxygen(stack) >= tank.capacity()) return inventory.getStackInSlot(SLOT_OUTPUT).isEmpty()?STATUS_TANK_FULL:STATUS_OUTPUT_FULL;')
s=s.replace('energyStorage.getEnergyStored() < ENERGY_PER_FILL_TICK','energyStorage.getEnergyStored() < fillEnergyCost()')
s=s.replace('    public static void clientTick(Level level, BlockPos pos, BlockState state, OxygenFillerBlockEntity machine) {','''    public static void clientTick(Level level, BlockPos pos, BlockState state, OxygenFillerBlockEntity machine) {
        machine.previousAnimationTick=machine.animationTick;
        if(state.getValue(OxygenFillerBlock.LIT))machine.animationTick=(machine.animationTick+1)%80;''')
s=s.replace('private int getTankOxygen()', 'public int getTankOxygen()').replace('private int getTankCapacity()', 'public int getTankCapacity()')
a=s.index('    private boolean allowsOxygenInputOn');b=s.index('    /**',a)
s=s[:a]+'''    public SideMode sideMode(Direction direction) {
        if(direction==null || isFrontWorldSide(direction) || operatingMode==OxygenFillerMode.VENTILATION && direction==Direction.UP) return SideMode.DISABLED;
        return sideConfig.getMode(direction);
    }
    private boolean allowsOxygenInputOn(Direction direction) { return sideMode(direction)==SideMode.INPUT; }
    private boolean allowsOxygenOutputOn(Direction direction) { return sideMode(direction)==SideMode.OUTPUT; }
    public void rotateSideConfiguration(Direction oldFacing) {
        Direction facing=getMachineFacing();
        if(oldFacing!=null && oldFacing!=facing){
            EnumMap<RelativeSide,SideMode> old=new EnumMap<>(RelativeSide.class);
            for(RelativeSide side:RelativeSide.values())old.put(side,sideConfig.getMode(side.resolve(oldFacing)));
            for(RelativeSide side:RelativeSide.values())sideConfig.setMode(side.resolve(facing),old.get(side));
        }
        sideConfig.setMode(facing,SideMode.DISABLED);lastFacing=facing;refreshCapabilities();syncAllPortStates();setChanged();
    }

'''+s[b:]
s=s.replace('        sideConfig.save(tag);','        sideConfig.save(tag);\n        tag.put("Modules",modules.serializeNBT());tag.putString("PortFacing",getMachineFacing().getName());')
s=s.replace('        energyStorage.setEnergyStoredInternal(tag.getInt(NBT_ENERGY));','        if(tag.contains("Modules"))modules.deserializeNBT(tag.getCompound("Modules"));\n        else for(int i=0;i<2;i++)modules.setStackInSlot(i,ItemStack.EMPTY);\n        modulesChanged();\n        energyStorage.setEnergyStoredInternal(tag.getInt(NBT_ENERGY));')
s=s.replace('            inventory.deserializeNBT(tag.getCompound(NBT_INVENTORY));', '            CompoundTag stored=tag.getCompound(NBT_INVENTORY).copy();stored.putInt("Size",2);\n            inventory.deserializeNBT(stored);')
s=s.replace('        Direction facing = getMachineFacing();\n        sideConfig.setMode(facing, SideMode.DISABLED);','        Direction savedFacing=Direction.byName(tag.getString("PortFacing"));\n        rotateSideConfiguration(savedFacing);\n        Direction facing = getMachineFacing();\n        sideConfig.setMode(facing, SideMode.DISABLED);')
a=s.index('    @Override\n    public CompoundTag getUpdateTag()');b=s.index('    @Nullable\n    @Override\n    public ClientboundBlockEntityDataPacket',a)
s=s[:a]+'''    @Override public CompoundTag getUpdateTag() { return saveWithoutMetadata(); }
    @Override public void handleUpdateTag(CompoundTag tag) { load(tag); }

'''+s[b:]
a=s.index('    @Override\n    public <T> @NotNull LazyOptional<T> getCapability');b=s.index('    @Override\n    public Component getDisplayName()',a)
s=s[:a]+'''    @Override public <T> @NotNull LazyOptional<T> getCapability(@NotNull Capability<T> cap,@Nullable Direction side) {
        if(cap==ForgeCapabilities.ENERGY || cap==ForgeCapabilities.ITEM_HANDLER || cap==ModCapabilities.OXYGEN){
            if(isRemoved() || sideMode(side)==SideMode.DISABLED)return LazyOptional.empty();
            Port port=ports.computeIfAbsent(side,Port::new);
            if(cap==ForgeCapabilities.ENERGY)return sideMode(side)==SideMode.INPUT?port.energy.cast():LazyOptional.empty();
            return cap==ModCapabilities.OXYGEN?port.oxygen.cast():port.items.cast();
        }
        return super.getCapability(cap,side);
    }
    private void refreshCapabilities(){ports.values().forEach(p->{p.energy.invalidate();p.oxygen.invalidate();p.items.invalidate();});ports.clear();}
    @Override public void invalidateCaps(){super.invalidateCaps();refreshCapabilities();}
    @Override public void reviveCaps(){super.reviveCaps();refreshCapabilities();}
    private final class Port implements IItemHandler {
        final Direction side; Port(Direction side){this.side=side;}
        boolean input(){return !isRemoved()&&sideMode(side)==SideMode.INPUT;}
        boolean output(){return !isRemoved()&&sideMode(side)==SideMode.OUTPUT;}
        final LazyOptional<IItemHandler> items=LazyOptional.of(()->this);
        final LazyOptional<IEnergyStorage> energy=LazyOptional.of(()->new IEnergyStorage(){
            public int receiveEnergy(int n,boolean simulate){int accepted=input()?energyStorage.receiveEnergy(n,simulate):0;if(accepted>0&&!simulate)setChanged();return accepted;}
            public int extractEnergy(int n,boolean simulate){return 0;}
            public int getEnergyStored(){return energyStorage.getEnergyStored();}public int getMaxEnergyStored(){return energyStorage.getMaxEnergyStored();}
            public boolean canReceive(){return input();}public boolean canExtract(){return false;}
        });
        final LazyOptional<IOxygenStorage> oxygen=LazyOptional.of(()->new IOxygenStorage(){
            public int receiveOxygen(int n,boolean simulate){int accepted=input()?oxygenStorage.receiveOxygen(n,simulate):0;if(accepted>0&&!simulate)setChanged();return accepted;}
            public int extractOxygen(int n,boolean simulate){return output()?extractOxygenForNetwork(n,simulate):0;}
            public int getOxygenStored(){return oxygenAmount();}public int getMaxOxygenStored(){return OXYGEN_CAPACITY;}
            public boolean canReceive(){return input();}public boolean canExtract(){return output();}
        });
        public int getSlots(){return 2;}
        public ItemStack getStackInSlot(int slot){return (slot==SLOT_TANK&&input()||slot==SLOT_OUTPUT&&output())?inventory.getStackInSlot(slot).copy():ItemStack.EMPTY;}
        public ItemStack insertItem(int slot,ItemStack stack,boolean simulate){return isItemValid(slot,stack)?inventory.insertItem(slot,stack,simulate):stack;}
        public ItemStack extractItem(int slot,int amount,boolean simulate){return slot==SLOT_OUTPUT&&output()&&inventory.getStackInSlot(slot).getItem() instanceof OxygenTankItem tank&&tank.getOxygen(inventory.getStackInSlot(slot))==tank.capacity()?inventory.extractItem(slot,amount,simulate):ItemStack.EMPTY;}
        public int getSlotLimit(int slot){return 1;}
        public boolean isItemValid(int slot,ItemStack stack){return slot==SLOT_TANK&&input()&&acceptsTank(stack);}
    }

'''+s[b:]
s=s.replace('VENTILATION_PARTICLE_INTERVAL = 12','VENTILATION_PARTICLE_INTERVAL = 4')
s=s.replace('(level.random.nextDouble() - 0.5D) * 0.10D;', '(level.random.nextDouble() - 0.5D) * 0.28D;',1)
s=s.replace('double z = pos.getZ() + 0.5D + (level.random.nextDouble() - 0.5D) * 0.10D;', 'double z = pos.getZ() + (level.random.nextBoolean() ? .165D : .835D);\n        if(machine.getMachineFacing().getAxis()==Direction.Axis.X){double offset=x-pos.getX();x=pos.getX()+z-pos.getZ();z=pos.getZ()+offset;}')
s=s.replace('0.0D, 0.012D, 0.0D','(level.random.nextDouble()-.5)*.008D, .015D, (level.random.nextDouble()-.5)*.008D')
p.write_text(s,encoding='utf-8')
print('Filler mechanics prepared; room atmosphere code retained')
