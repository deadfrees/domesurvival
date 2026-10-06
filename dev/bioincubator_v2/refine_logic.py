"""Apply the biosynthesis family integration to the saved incubator baseline."""
from pathlib import Path
root=Path(__file__).resolve().parents[2];java=root/'src/main/java/com/wasted/domesurvival/forge'
p=java/'machine/bio/BioincubatorBlockEntity.java';s=p.read_text(encoding='utf-8')
s=s.replace('import com.wasted.domesurvival.forge.machine.side.SideMode;', 'import com.wasted.domesurvival.forge.machine.side.SideMode;\nimport com.wasted.domesurvival.forge.machine.module.*;\nimport net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;')
s=s.replace('implements MenuProvider', 'implements MenuProvider, IModularMachine')
# Some existing declarations use the qualified MenuProvider type.
s=s.replace('implements net.minecraft.world.MenuProvider {','implements net.minecraft.world.MenuProvider, IModularMachine {')
s=s.replace('public static final int DATA_COUNT = DATA_SIDES_START + 6;','public static final int DATA_CYCLE_ENERGY = DATA_SIDES_START + 6;\n    public static final int DATA_COUNT = DATA_CYCLE_ENERGY + 1;')
s=s.replace('            progress = 0;\n            setChanged();','            setChanged();',1)
a=s.index('    private final IEnergyStorage energyInputView');b=s.index('    private int progress;',a)
s=s[:a]+'''    private final java.util.EnumMap<Direction,Port> ports = new java.util.EnumMap<>(Direction.class);
    private final MachineModuleInventory modules = new MachineModuleInventory(this,MachineModuleResolver.STANDARD,this::modulesChanged);
    private int cycleTicks, cycleEnergy, cycleMode;
    private ItemStack cycleCapsule = ItemStack.EMPTY;
    private Direction lastFacing;
    private float animationTick, previousAnimationTick;
    private boolean syncPending;
    public int moduleSlotCount(){return 2;}
    public Set<MachineModuleType> allowedModuleTypes(){return Set.of(MachineModuleType.BUFFER,MachineModuleType.EFFICIENCY,MachineModuleType.OVERDRIVE);}
    public MachineModuleInventory getModules(){return modules;}
    private void modulesChanged(){energyStorage.setCapacityInternal(Math.max(energyStorage.getEnergyStored(),modules.modifiers().applyBufferCapacity(ENERGY_CAPACITY)));setChanged();}
    private void resetCycle(){progress=0;cycleTicks=cycleEnergy=0;cycleCapsule=ItemStack.EMPTY;}
    public static void clientTick(Level level,BlockPos pos,BlockState state,BioincubatorBlockEntity machine){
        machine.previousAnimationTick=machine.animationTick;
        if(state.getValue(BioincubatorBlock.LIT))machine.animationTick=(machine.animationTick+(machine.mode==MODE_REPAIR?2:1))%80;
    }
    public float animationPhase(float partial){float next=animationTick<previousAnimationTick?animationTick+80:animationTick;return (previousAnimationTick+(next-previousAnimationTick)*partial)/80F;}
    @Override public void setChanged(){super.setChanged();syncPending=true;}
    @Override public CompoundTag getUpdateTag(){return saveWithoutMetadata();}
    @Override public void handleUpdateTag(CompoundTag tag){load(tag);}
    @Override public ClientboundBlockEntityDataPacket getUpdatePacket(){return ClientboundBlockEntityDataPacket.create(this);}
    public void rotateSideConfiguration(@Nullable Direction previous){
        Direction facing=getMachineFacing();
        if(previous!=null&&previous!=facing){
            var modes=new java.util.EnumMap<RelativeSide,SideMode>(RelativeSide.class);
            for(var side:RelativeSide.values())modes.put(side,sideConfig.getMode(side.resolve(previous)));
            for(var side:RelativeSide.values())sideConfig.setMode(side.resolve(facing),modes.get(side));
        }
        sideConfig.setMode(facing,SideMode.DISABLED);lastFacing=facing;refreshCapabilities();portsNeedSync=true;syncAllPortStates();setChanged();
    }
'''+s[b:]
s=s.replace('            if (index == DATA_MODE) return mode;','            if (index == DATA_MODE) return mode;\n            if (index == DATA_CYCLE_ENERGY) return processingEnergy(recipe);')
s=s.replace('        progress = 0;\n        status = calculateStatus(currentRecipe());','        resetCycle();\n        status = calculateStatus(currentRecipe());')
a=s.index('        Recipe recipe = incubator.currentRecipe();',s.index('public static void serverTick'));b=s.index('        incubator.status =',a)
s=s[:a]+'''        if(incubator.lastFacing!=incubator.getMachineFacing())incubator.rotateSideConfiguration(incubator.lastFacing);
        if(incubator.progress>0&&!incubator.cycleCapsule.isEmpty()&&
                (incubator.cycleMode!=incubator.mode||!ItemStack.isSameItemSameTags(incubator.cycleCapsule,incubator.inventory.getStackInSlot(SLOT_CAPSULE))))incubator.resetCycle();
        Recipe recipe = incubator.currentRecipe();
        int newStatus = incubator.calculateStatus(recipe);
        boolean changed = false;
        if(newStatus==STATUS_RUNNING){
            if(incubator.cycleTicks==0){
                incubator.cycleTicks=incubator.processTicks(recipe);incubator.cycleEnergy=incubator.processingEnergy(recipe);
                incubator.cycleCapsule=incubator.inventory.getStackInSlot(SLOT_CAPSULE).copyWithCount(1);incubator.cycleMode=incubator.mode;
            }
            incubator.energyStorage.removeEnergyInternal(incubator.energyPerTick(recipe));incubator.progress++;changed=true;
            if(incubator.progress>=incubator.cycleTicks){
                boolean finished=incubator.mode==MODE_REPAIR?incubator.finishRepair():recipe!=null&&incubator.finishCycle(recipe);
                if(finished)incubator.resetCycle();else incubator.progress=incubator.cycleTicks-1;
            }
        }else if(newStatus==STATUS_NO_CAPSULE||newStatus==STATUS_INVALID_CAPSULE||newStatus==STATUS_DAMAGED_CAPSULE||newStatus==STATUS_REQUIRES_DAMAGED){
            if(incubator.progress!=0){incubator.resetCycle();changed=true;}
        }
'''+s[b:]
s=s.replace('        if (changed) {\n            incubator.setChanged();\n        }','''        if (changed) incubator.setChanged();
        if(incubator.syncPending&&level.getGameTime()%10==0){incubator.syncPending=false;level.sendBlockUpdated(pos,level.getBlockState(pos),level.getBlockState(pos),2);}''')
s=s.replace('energyStorage.getEnergyStored() < REPAIR_ENERGY_PER_TICK','energyStorage.getEnergyStored() < energyPerTick(recipe)').replace('energyStorage.getEnergyStored() < recipe.energyPerTick()','energyStorage.getEnergyStored() < energyPerTick(recipe)')
a=s.index('    private int energyPerTick(');b=s.index('    private int speciesId()',a)
s=s[:a]+'''    private int baseTicks(@Nullable Recipe recipe){return mode==MODE_REPAIR?REPAIR_PROCESS_TICKS:recipe==null?0:recipe.processTicks();}
    public int processingEnergy(@Nullable Recipe recipe){
        if(cycleTicks>0)return cycleEnergy;
        int base=baseTicks(recipe)*(mode==MODE_REPAIR?REPAIR_ENERGY_PER_TICK:recipe==null?0:recipe.energyPerTick());
        return modules.modifiers().applyEnergyCost(base);
    }
    private int energyPerTick(@Nullable Recipe recipe){
        int ticks=Math.max(1,processTicks(recipe)),cost=processingEnergy(recipe),before=Math.min(progress,ticks),after=Math.min(progress+1,ticks);
        return (int)((long)cost*after/ticks-(long)cost*before/ticks);
    }
    public int processTicks(@Nullable Recipe recipe){return cycleTicks>0?cycleTicks:modules.modifiers().applyProcessingTicks(baseTicks(recipe));}
'''+s[b:]
s=s.replace('    private Direction getMachineFacing()', '    public Direction getMachineFacing()')
s=s.replace('        energyStorage.setEnergyStoredInternal(tag.getInt(NBT_ENERGY));','''        modules.deserializeNBT(tag.getCompound("Modules"));
        energyStorage.setCapacityInternal(Math.max(tag.getInt(NBT_ENERGY),modules.modifiers().applyBufferCapacity(ENERGY_CAPACITY)));
        energyStorage.setEnergyStoredInternal(tag.getInt(NBT_ENERGY));''')
s=s.replace('        progress = tag.getInt(NBT_PROGRESS);','''        progress = Math.max(0,tag.getInt(NBT_PROGRESS));
        cycleTicks=Math.max(0,tag.getInt("CycleTicks"));cycleEnergy=Math.max(0,tag.getInt("CycleEnergy"));
        cycleCapsule=ItemStack.of(tag.getCompound("CycleCapsule"));cycleMode=tag.getInt("CycleMode");''')
s=s.replace('''        sideConfig.setMode(getMachineFacing(), SideMode.DISABLED);
        if (!hasConfiguredInput() || !hasConfiguredOutput()) {
            applyDefaultSideConfiguration();
        }''','        rotateSideConfiguration(Direction.byName(tag.getString("PortFacing")));')
s=s.replace('        tag.putInt(NBT_PROGRESS, progress);','''        tag.putInt(NBT_PROGRESS, progress);
        tag.put("Modules",modules.serializeNBT());tag.putInt("CycleTicks",cycleTicks);tag.putInt("CycleEnergy",cycleEnergy);
        tag.put("CycleCapsule",cycleCapsule.save(new CompoundTag()));tag.putInt("CycleMode",cycleMode);
        tag.putString("PortFacing",getMachineFacing().getName());''')
a=s.index('    @Override\n    public <T> @NotNull LazyOptional<T> getCapability');b=s.index('    public record Recipe(',a)
port=(java/'machine/organic/OrganicProcessorBlockEntity.java').read_text();port=port[port.index('    public SideMode sideMode('):port.index('    @Override\n    public Component getDisplayName()')]
port=port.replace('energy.','energyStorage.').replace('water.','purifiedWaterTank.').replace('getMachineFacing()','getMachineFacing()').replace('return 3;','return 5;').replace('slot<3','slot<5').replace('slot<2','slot<4').replace('slot==2','slot==4')
s=s[:a]+port+s[b:]
p.write_text(s,encoding='utf-8')
