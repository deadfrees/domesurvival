from pathlib import Path
r=Path(__file__).resolve().parents[2]
p=r/'src/main/java/com/wasted/domesurvival/forge/machine/organic/OrganicProcessorBlockEntity.java'
s=p.read_text()
a=s.index('    private final IItemHandler fullItems');b=s.index('    private int progress;',a)
s=s[:a]+'''    private final java.util.EnumMap<Direction, Port> ports = new java.util.EnumMap<>(Direction.class);

'''+s[b:]
a=s.index('    @Override\n    public <T> @NotNull LazyOptional<T> getCapability');b=s.index('    @Override\n    public Component getDisplayName()',a)
s=s[:a]+'''    public SideMode sideMode(@Nullable Direction side) {
        return side == null || side == getMachineFacing() ? SideMode.DISABLED : sideConfig.getMode(side);
    }
    @Override public <T> @NotNull LazyOptional<T> getCapability(@NotNull Capability<T> cap,@Nullable Direction side) {
        if (cap == ForgeCapabilities.ITEM_HANDLER || cap == ForgeCapabilities.ENERGY || cap == ForgeCapabilities.FLUID_HANDLER) {
            if (isRemoved() || sideMode(side) == SideMode.DISABLED) return LazyOptional.empty();
            Port port = ports.computeIfAbsent(side, Port::new);
            if (cap == ForgeCapabilities.ITEM_HANDLER) return port.items.cast();
            if (sideMode(side) != SideMode.INPUT) return LazyOptional.empty();
            return cap == ForgeCapabilities.ENERGY ? port.power.cast() : port.fluid.cast();
        }
        return super.getCapability(cap,side);
    }
    private void refreshCapabilities() {
        ports.values().forEach(p -> {p.items.invalidate();p.power.invalidate();p.fluid.invalidate();});
        ports.clear();
    }
    @Override public void invalidateCaps(){super.invalidateCaps();refreshCapabilities();}
    @Override public void reviveCaps(){super.reviveCaps();refreshCapabilities();}

    private final class Port implements IItemHandler {
        final Direction side;
        Port(Direction side){this.side=side;}
        boolean input(){return !isRemoved() && sideMode(side)==SideMode.INPUT;}
        boolean output(){return !isRemoved() && sideMode(side)==SideMode.OUTPUT;}
        final LazyOptional<IItemHandler> items=LazyOptional.of(()->this);
        final LazyOptional<IEnergyStorage> power=LazyOptional.of(()->new IEnergyStorage(){
            public int receiveEnergy(int n,boolean sim){int accepted=input()?energy.receiveEnergy(n,sim):0;if(accepted>0&&!sim)setChanged();return accepted;}
            public int extractEnergy(int n,boolean sim){return 0;}
            public boolean canReceive(){return input();} public boolean canExtract(){return false;}
            public int getEnergyStored(){return energy.getEnergyStored();} public int getMaxEnergyStored(){return energy.getMaxEnergyStored();}
        });
        final LazyOptional<IFluidHandler> fluid=LazyOptional.of(()->new IFluidHandler(){
            public int getTanks(){return 1;}
            public FluidStack getFluidInTank(int tank){return tank==0&&input()?water.getFluid().copy():FluidStack.EMPTY;}
            public int getTankCapacity(int tank){return tank==0?water.getCapacity():0;}
            public boolean isFluidValid(int tank,FluidStack stack){return tank==0&&input()&&water.isFluidValid(tank,stack);}
            public int fill(FluidStack stack,FluidAction action){return input()?water.fill(stack,action):0;}
            public FluidStack drain(FluidStack stack,FluidAction action){return FluidStack.EMPTY;}
            public FluidStack drain(int amount,FluidAction action){return FluidStack.EMPTY;}
        });
        public int getSlots(){return 3;}
        public ItemStack getStackInSlot(int slot){return slot>=0&&slot<3&&(input()&&slot<2||output()&&slot==2)?inventory.getStackInSlot(slot).copy():ItemStack.EMPTY;}
        public boolean isItemValid(int slot,ItemStack stack){return slot>=0&&slot<2&&input()&&inventory.isItemValid(slot,stack);}
        public ItemStack insertItem(int slot,ItemStack stack,boolean simulate){return isItemValid(slot,stack)?inventory.insertItem(slot,stack,simulate):stack;}
        public ItemStack extractItem(int slot,int amount,boolean simulate){return slot==2&&output()?inventory.extractItem(slot,amount,simulate):ItemStack.EMPTY;}
        public int getSlotLimit(int slot){return slot>=0&&slot<3?inventory.getSlotLimit(slot):0;}
    }

'''+s[b:]
s=s.replace('                progress = 0;\n                activeRecipe = null;\n','')
s=s.replace('        sideConfig.setMode(getMachineFacing(), SideMode.DISABLED);\n        rotateSideConfiguration','        rotateSideConfiguration')
p.write_text(s)
