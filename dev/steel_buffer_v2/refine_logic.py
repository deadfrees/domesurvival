"""One-time, scoped migration of the steel buffer; other tiers remain unchanged."""
from pathlib import Path
root=Path(__file__).resolve().parents[2];java=root/'src/main/java/com/wasted/domesurvival/forge'
p=java/'machine/energy/EnergyBufferBlockEntity.java';s=p.read_text()
a=s.index('    private final IEnergyStorage energyInputView');b=s.index('    private final ContainerData dataAccess',a)
s=s[:a]+'''    private final java.util.EnumMap<Direction, LazyOptional<IEnergyStorage>> ports = new java.util.EnumMap<>(Direction.class);
    private Direction lastFacing;

    private int remainingInput() { rollTransferStats(); return Math.max(0, MAX_RECEIVE_PER_TICK - receivedThisTick); }
    private int remainingOutput() { rollTransferStats(); return Math.max(0, MAX_OUTPUT_PER_TICK - sentThisTick); }
    public SideMode sideMode(@Nullable Direction side) {
        return side == null || side == getMachineFacing() ? SideMode.DISABLED : sideConfig.getMode(side);
    }
    private final class SideEnergy implements IEnergyStorage {
        private final Direction side;
        private SideEnergy(Direction side) { this.side = side; }
        public boolean canReceive() { return !isRemoved() && sideMode(side) == SideMode.INPUT; }
        public boolean canExtract() { return !isRemoved() && sideMode(side) == SideMode.OUTPUT; }
        public int getEnergyStored() { return energyStorage.getEnergyStored(); }
        public int getMaxEnergyStored() { return energyStorage.getMaxEnergyStored(); }
        public int receiveEnergy(int amount, boolean simulate) {
            if (!canReceive() || amount <= 0) return 0;
            int accepted = energyStorage.receiveEnergy(Math.min(amount, remainingInput()), simulate);
            if (!simulate && accepted > 0) { recordInput(accepted); onEnergyChanged(); }
            return accepted;
        }
        public int extractEnergy(int amount, boolean simulate) {
            if (!canExtract() || amount <= 0) return 0;
            int extracted = energyStorage.extractEnergy(Math.min(amount, remainingOutput()), simulate);
            if (!simulate && extracted > 0) { recordOutput(extracted); onEnergyChanged(); }
            return extracted;
        }
    }

'''+s[b:]
s=s.replace('        applyDefaultSideConfiguration();\n    }','        applyDefaultSideConfiguration();\n        lastFacing = getMachineFacing();\n    }',1)
s=s.replace('        buffer.rollTransferStats();\n        int charged = buffer.chargeInsertedItem(MAX_OUTPUT_PER_TICK);\n        int pushed = buffer.pushEnergyToNeighbors(level, pos, Math.max(0, MAX_OUTPUT_PER_TICK - charged));','''        if (buffer.lastFacing != buffer.getMachineFacing()) buffer.rotateSideConfiguration(buffer.lastFacing);
        buffer.rollTransferStats();
        buffer.energyStorage.setCapacityInternal(Math.max(buffer.getEnergyStored(), EnergyBufferCapacity.apply(ENERGY_CAPACITY, buffer.capacityEnchantLevel)));
        int charged = buffer.chargeInsertedItem(buffer.remainingOutput());
        int pushed = buffer.pushEnergyToNeighbors(level, pos, buffer.remainingOutput());''')
s=s.replace('int acceptedSimulation = target.receiveEnergy(available, true);','int acceptedSimulation = Math.max(0, Math.min(available, target.receiveEnergy(available, true)));')
s=s.replace('int acceptedActual = target.receiveEnergy(extracted, false);','int acceptedActual = Math.max(0, Math.min(extracted, target.receiveEnergy(extracted, false)));')
s=s.replace('lastReceivedPerTick = receivedThisTick;\n            lastSentPerTick = sentThisTick;','lastReceivedPerTick = gameTime == transferStatsTick + 1 ? receivedThisTick : 0;\n            lastSentPerTick = gameTime == transferStatsTick + 1 ? sentThisTick : 0;')
s=s.replace('(int) Math.round((energyStorage.getEnergyStored() * 4.0D) / capacity)', '(int) ((long) energyStorage.getEnergyStored() * 4 / capacity)')
s=s.replace('energyStorage.setCapacityInternal(targetCapacity);','energyStorage.setCapacityInternal(Math.max(targetCapacity, getEnergyStored()));\n        syncEnergyLevel();\n        notifyComparator();')
s=s.replace('        sideConfig.save(tag);','        sideConfig.save(tag);\n        tag.putString("PortFacing", getMachineFacing().getName());')
s=s.replace('EnergyBufferCapacity.apply(ENERGY_CAPACITY, capacityEnchantLevel)\n        );','Math.max(tag.getInt(NBT_ENERGY), EnergyBufferCapacity.apply(ENERGY_CAPACITY, capacityEnchantLevel))\n        );')
s=s.replace('        sideConfig.setMode(getMachineFacing(), SideMode.DISABLED);\n    }','''        Direction previous = Direction.byName(tag.getString("PortFacing"));
        remapSides(previous);
        refreshCapabilities();
    }

    private void remapSides(@Nullable Direction previous) {
        Direction facing = getMachineFacing();
        if (previous != null && previous.getAxis().isHorizontal() && previous != facing) {
            var modes = new java.util.EnumMap<RelativeSide, SideMode>(RelativeSide.class);
            for (var side : RelativeSide.values()) modes.put(side, sideConfig.getMode(side.resolve(previous)));
            for (var side : RelativeSide.values()) sideConfig.setMode(side.resolve(facing), modes.get(side));
        }
        sideConfig.setMode(facing, SideMode.DISABLED);
        lastFacing = facing;
    }

    public void rotateSideConfiguration(@Nullable Direction previous) {
        remapSides(previous);
        refreshCapabilities();
        syncAllPortStates();
        syncClientState();
        notifyNeighborConnections();
        setChanged();
    }''')
a=s.index('    @Override\n    public <T> @NotNull LazyOptional<T> getCapability');b=s.index('    private void notifyComparator()',a)
s=s[:a]+'''    @Override
    public <T> @NotNull LazyOptional<T> getCapability(@NotNull Capability<T> cap, @Nullable Direction side) {
        if (cap == ForgeCapabilities.ENERGY) {
            if (isRemoved() || sideMode(side) == SideMode.DISABLED) return LazyOptional.empty();
            return ports.computeIfAbsent(side, d -> LazyOptional.of(() -> new SideEnergy(d))).cast();
        }
        return super.getCapability(cap, side);
    }

    private void refreshCapabilities() { ports.values().forEach(LazyOptional::invalidate); ports.clear(); }
    @Override public void invalidateCaps() { super.invalidateCaps(); refreshCapabilities(); }
    @Override public void reviveCaps() { super.reviveCaps(); refreshCapabilities(); }

'''+s[b:];p.write_text(s)

p=java/'machine/energy/EnergyBufferMenu.java';s=p.read_text()
s=s.replace('    private final Level level;', '    private boolean sidePanelOpen;\n    private final Level level;')
s=s.replace('        addDataSlots(data);\n        addSlot(new SlotItemHandler(chargeInventory, 0, 79, 71));','''        // Vanilla DataSlot packets carry signed shorts: split every full value explicitly.
        for (int i=0;i<data.getCount();i++) {
            final int index=i;
            for (int part=0;part<2;part++) {
                final int shift=part*16;
                addDataSlot(new net.minecraft.world.inventory.DataSlot() {
                    public int get() { return (data.get(index) >>> shift) & 0xFFFF; }
                    public void set(int value) { data.set(index, (data.get(index) & ~(0xFFFF << shift)) | ((value & 0xFFFF) << shift)); }
                });
            }
        }
        addSlot(new SlotItemHandler(chargeInventory, 0, 178, 47) {
            @Override public boolean isActive() { return !sidePanelOpen; }
            @Override public boolean mayPlace(ItemStack stack) { return isActive() && EnergyItemCharging.isChargeable(stack) && super.mayPlace(stack); }
            @Override public boolean mayPickup(Player player) { return isActive() && super.mayPickup(player); }
        });''')
s=s.replace('8 + col * 18','14 + col * 22').replace('116 + row * 18','161 + row * 22').replace('8 + col * 18, 174','14 + col * 22, 229').replace('14 + col * 22, 174','14 + col * 22, 229')
s=s.replace('        int sideIndex = id - SIDE_BUTTON_BASE;','''        if (!stillValid(player)) return false;
        if (id == 201 || id == 202) { sidePanelOpen = id == 202; return true; }
        if (!sidePanelOpen) return false;
        int sideIndex = id - SIDE_BUTTON_BASE;''')
s=s.replace('    public static int sideButtonId', '    public boolean isSidePanelOpen() { return sidePanelOpen; }\n    public void setSidePanelOpen(boolean value) { sidePanelOpen=value; }\n\n    public static int sideButtonId')
s=s.replace('        if (!slot.hasItem()) return ItemStack.EMPTY;', '        if (!slot.isActive() || !slot.hasItem() || !slot.mayPickup(player)) return ItemStack.EMPTY;')
s=s.replace('} else if (EnergyItemCharging.isChargeable(stack)) {','} else if (!sidePanelOpen && EnergyItemCharging.isChargeable(stack)) {')
p.write_text(s)
p=java/'block/ModBlocks.java';s=p.read_text();s=s.replace('new EnergyBufferBlock(BlockBehaviour.Properties.copy(Blocks.IRON_BLOCK)\n                    .strength(4.0F, 8.0F))','new EnergyBufferBlock(BlockBehaviour.Properties.copy(Blocks.IRON_BLOCK)\n                    .strength(4.0F, 8.0F).noOcclusion())');p.write_text(s)
p=java/'item/EngineerWrenchItem.java';s=p.read_text();needle='            if(level.getBlockEntity(pos) instanceof com.wasted.domesurvival.forge.machine.organic.OrganicProcessorBlockEntity processor)';s=s.replace(needle,'''            if(level.getBlockEntity(pos) instanceof com.wasted.domesurvival.forge.machine.energy.EnergyBufferBlockEntity buffer)
                buffer.rotateSideConfiguration(state.getValue(BlockStateProperties.HORIZONTAL_FACING));
'''+needle);p.write_text(s)
for name in ('needs_stone_tool','mineable/pickaxe'):
    p=root/f'src/main/resources/data/minecraft/tags/blocks/{name}.json'
    import json
    data=json.loads(p.read_text());values=data['values']
    if 'domesurvival:energy_buffer' not in values:values.append('domesurvival:energy_buffer');p.write_text(json.dumps(data,indent=2)+'\n')
