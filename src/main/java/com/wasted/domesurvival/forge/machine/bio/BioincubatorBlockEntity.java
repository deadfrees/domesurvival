package com.wasted.domesurvival.forge.machine.bio;

import com.mojang.logging.LogUtils;
import com.wasted.domesurvival.forge.bio.BioLootData;
import com.wasted.domesurvival.forge.bio.BioModuleData;
import com.wasted.domesurvival.forge.fluid.ModFluids;
import com.wasted.domesurvival.forge.item.ModItems;
import com.wasted.domesurvival.forge.machine.energy.MachineEnergyStorage;
import com.wasted.domesurvival.forge.machine.side.PortVisual;
import com.wasted.domesurvival.forge.machine.side.RelativeSide;
import com.wasted.domesurvival.forge.machine.side.SideMode;
import com.wasted.domesurvival.forge.machine.module.*;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import com.wasted.domesurvival.forge.machine.side.UnifiedSideConfig;
import com.wasted.domesurvival.forge.registry.ModBlockEntities;
import com.wasted.domesurvival.forge.quest.QuestProgressService;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.AgeableMob;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.energy.IEnergyStorage;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.capability.IFluidHandler;
import net.minecraftforge.fluids.capability.templates.FluidTank;
import net.minecraftforge.items.IItemHandler;
import net.minecraftforge.items.ItemStackHandler;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;

import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * BIOINCUBATOR_VISUAL_V191
 * Connector-aware incubator refined after in-game review.
 */
public final class BioincubatorBlockEntity extends BlockEntity implements MenuProvider, IModularMachine {
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final Set<String> REPORTED_INVALID_SAMPLES = ConcurrentHashMap.newKeySet();
    public static final int ENERGY_CAPACITY = 60_000;
    public static final int MAX_ENERGY_INPUT_PER_TICK = 128;
    public static final int WATER_CAPACITY = 6_000;

    public static final int SLOT_CAPSULE = 0;
    public static final int SLOT_FEED = 1;
    public static final int SLOT_BIOGEL = 2;
    public static final int SLOT_NUTRIENT = 3;
    public static final int SLOT_OUTPUT = 4;
    public static final int SLOT_COUNT = 5;

    public static final int MODE_INCUBATION = 0;
    public static final int MODE_REPAIR = 1;
    public static final int REPAIR_WATER_MB = 1_000;
    public static final int REPAIR_ENERGY_PER_TICK = 80;
    public static final int REPAIR_PROCESS_TICKS = 1_800;

    public static final int DATA_ENERGY = 0;
    public static final int DATA_ENERGY_CAPACITY = 1;
    public static final int DATA_WATER = 2;
    public static final int DATA_WATER_CAPACITY = 3;
    public static final int DATA_PROGRESS = 4;
    public static final int DATA_PROGRESS_MAX = 5;
    public static final int DATA_STATUS = 6;
    public static final int DATA_SPECIES = 7;
    public static final int DATA_MODE = 8;
    public static final int DATA_SIDES_START = 9;
    public static final int DATA_CYCLE_ENERGY = DATA_SIDES_START + 6;
    public static final int DATA_COUNT = DATA_CYCLE_ENERGY + 1;

    public static final int STATUS_IDLE = 0;
    public static final int STATUS_RUNNING = 1;
    public static final int STATUS_NO_CAPSULE = 2;
    public static final int STATUS_INVALID_CAPSULE = 3;
    public static final int STATUS_NO_FEED = 4;
    public static final int STATUS_NO_WATER = 5;
    public static final int STATUS_NO_ENERGY = 6;
    public static final int STATUS_OUTPUT_BLOCKED = 7;
    public static final int STATUS_DATABASE_LOCKED = 8;
    public static final int STATUS_DAMAGED_CAPSULE = 9;
    public static final int STATUS_REQUIRES_DAMAGED = 10;
    public static final int STATUS_NO_REPAIR_MATERIALS = 11;
    public static final int STATUS_REPAIR_OUTPUT_FULL = 12;

    private static final String NBT_INVENTORY = "Inventory";
    private static final String NBT_ENERGY = "Energy";
    private static final String NBT_WATER = "PurifiedWater";
    private static final String NBT_PROGRESS = "Progress";
    private static final String NBT_MODE = "Mode";

    private final UnifiedSideConfig sideConfig = new UnifiedSideConfig();

    private final ItemStackHandler inventory = new ItemStackHandler(SLOT_COUNT) {
        @Override
        public boolean isItemValid(int slot, @NotNull ItemStack stack) {
            if (slot == SLOT_CAPSULE) {
                BioModuleData.Sample sample = BioModuleData.sample(stack);
                return BioModuleData.isIdentificationUnlocked(level)
                        && sample != null
                        && BioLootData.isAllowed(sample.entityId())
                        && (mode == MODE_REPAIR ? sample.damaged() : !sample.damaged());
            }
            if (slot == SLOT_FEED) {
                if (mode == MODE_REPAIR) {
                    return stack.is(ModItems.BIO_REPAIR_KIT.get());
                }
                return BioLootData.allSpecies().stream().anyMatch(species ->
                        stack.is(net.minecraftforge.registries.ForgeRegistries.ITEMS.getValue(species.feedItem())));
            }
            if (slot == SLOT_BIOGEL) return mode == MODE_REPAIR && stack.is(ModItems.BIOGEL.get());
            if (slot == SLOT_NUTRIENT) return mode == MODE_REPAIR && stack.is(ModItems.NUTRIENT_MIX.get());
            return false;
        }

        @Override
        protected void onContentsChanged(int slot) {
            setChanged();
        }
    };

    private final MachineEnergyStorage energyStorage =
            new MachineEnergyStorage(ENERGY_CAPACITY, MAX_ENERGY_INPUT_PER_TICK, 0);

    private final FluidTank purifiedWaterTank = new FluidTank(
            WATER_CAPACITY,
            stack -> stack.getFluid().isSame(ModFluids.PURIFIED_WATER.get())
    ) {
        @Override
        protected void onContentsChanged() {
            setChanged();
        }
    };

    private final java.util.EnumMap<Direction,Port> ports = new java.util.EnumMap<>(Direction.class);
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
        if(state.getValue(BioincubatorBlock.LIT))machine.animationTick=(machine.animationTick+1)%240;
    }
    public float animationPhase(float partial){float next=animationTick<previousAnimationTick?animationTick+240:animationTick;return (previousAnimationTick+(next-previousAnimationTick)*partial)/240F;}
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
    private int progress;
    private int mode = MODE_INCUBATION;
    private int status = STATUS_IDLE;
    private boolean portsNeedSync = true;

    private final ContainerData dataAccess = new ContainerData() {
        @Override
        public int get(int index) {
            Recipe recipe = currentRecipe();

            if (index == DATA_ENERGY) return energyStorage.getEnergyStored();
            if (index == DATA_ENERGY_CAPACITY) return energyStorage.getMaxEnergyStored();
            if (index == DATA_WATER) return purifiedWaterTank.getFluidAmount();
            if (index == DATA_WATER_CAPACITY) return purifiedWaterTank.getCapacity();
            if (index == DATA_PROGRESS) return progress;
            if (index == DATA_PROGRESS_MAX) return processTicks(recipe);
            if (index == DATA_STATUS) return status;
            if (index == DATA_SPECIES) return speciesId();
            if (index == DATA_MODE) return mode;
            if (index == DATA_CYCLE_ENERGY) return processingEnergy(recipe);

            if (index >= DATA_SIDES_START && index < DATA_SIDES_START + 6) {
                Direction direction = Direction.values()[index - DATA_SIDES_START];
                return sideConfig.getMode(direction).ordinal();
            }

            return 0;
        }

        @Override public void set(int index, int value) {}
        @Override public int getCount() { return DATA_COUNT; }
    };

    public BioincubatorBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.BIOINCUBATOR.get(), pos, state);
        applyDefaultSideConfiguration();
    }

    private void applyDefaultSideConfiguration() {
        sideConfig.reset();
        Direction facing = getMachineFacing();

        for (RelativeSide relative : RelativeSide.values()) {
            Direction worldSide = relative.resolve(facing);
            SideMode mode = switch (relative) {
                case FRONT -> SideMode.DISABLED;
                case TOP, LEFT, BACK -> SideMode.INPUT;
                case RIGHT, BOTTOM -> SideMode.OUTPUT;
            };
            sideConfig.setMode(worldSide, mode);
        }

        portsNeedSync = true;
    }

    public static boolean isConfigurableSide(RelativeSide side) {
        return side != RelativeSide.FRONT;
    }

    public SideMode cycleSideMode(RelativeSide relativeSide) {
        if (!isConfigurableSide(relativeSide)) {
            return SideMode.DISABLED;
        }

        Direction worldSide = relativeSide.resolve(getMachineFacing());
        SideMode next = sideConfig.cycleMode(worldSide);

        if (sideConfig.getMode(worldSide) == next) {
            refreshCapabilities();
            syncPortState(worldSide);
            notifyPortVisualUpdate();
            portsNeedSync = false;
            setChanged();
        }

        return next;
    }

    private void notifyPortVisualUpdate() {
        if (level == null || level.isClientSide) {
            return;
        }
        BlockState visualState = level.getBlockState(worldPosition);
        level.sendBlockUpdated(worldPosition, visualState, visualState, 3);
    }
    public int toggleMode() {
        mode = mode == MODE_INCUBATION ? MODE_REPAIR : MODE_INCUBATION;
        resetCycle();
        status = calculateStatus(currentRecipe());
        setChanged();
        return mode;
    }

    public static void serverTick(Level level, BlockPos pos, BlockState state, BioincubatorBlockEntity incubator) {
        if (incubator.portsNeedSync) {
            incubator.syncAllPortStates();
            incubator.notifyPortVisualUpdate();
            incubator.portsNeedSync = false;
        }

        if(incubator.lastFacing!=incubator.getMachineFacing())incubator.rotateSideConfiguration(incubator.lastFacing);
        if(incubator.progress>0&&!incubator.cycleCapsule.isEmpty()&&
                (incubator.cycleMode!=incubator.mode||!ItemStack.isSameItemSameTags(incubator.cycleCapsule,incubator.inventory.getStackInSlot(SLOT_CAPSULE))))incubator.resetCycle();
        Recipe recipe = incubator.currentRecipe();
        int newStatus = incubator.calculateStatus(recipe);
        boolean changed = false;
        if(newStatus==STATUS_RUNNING){
            if(incubator.cycleTicks==0){
                int ticks=incubator.processTicks(recipe),cost=incubator.processingEnergy(recipe);
                incubator.cycleTicks=ticks;incubator.cycleEnergy=cost;
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
        incubator.status = incubator.calculateStatus(incubator.currentRecipe());
        boolean lit = incubator.status == STATUS_RUNNING;

        BlockState currentState = level.getBlockState(pos);
        if (currentState.hasProperty(BioincubatorBlock.LIT)
                && currentState.getValue(BioincubatorBlock.LIT) != lit) {
            level.setBlock(pos, currentState.setValue(BioincubatorBlock.LIT, lit), 3);
            changed = true;
        }

        if (changed) incubator.setChanged();
        if(incubator.syncPending&&level.getGameTime()%10==0){incubator.syncPending=false;level.sendBlockUpdated(pos,level.getBlockState(pos),level.getBlockState(pos),2);}
    }

    private int calculateStatus(@Nullable Recipe recipe) {
        ItemStack capsule = inventory.getStackInSlot(SLOT_CAPSULE);
        if (capsule.isEmpty()) return STATUS_NO_CAPSULE;
        BioModuleData.Sample sample = BioModuleData.sample(capsule);
        if (sample == null) return STATUS_INVALID_CAPSULE;
        if (!BioLootData.isAllowed(sample.entityId())) return STATUS_INVALID_CAPSULE;

        if (mode == MODE_REPAIR) {
            if (!sample.damaged()) return STATUS_REQUIRES_DAMAGED;
            if (!inventory.getStackInSlot(SLOT_FEED).is(ModItems.BIO_REPAIR_KIT.get())
                    || !inventory.getStackInSlot(SLOT_BIOGEL).is(ModItems.BIOGEL.get())
                    || !inventory.getStackInSlot(SLOT_NUTRIENT).is(ModItems.NUTRIENT_MIX.get())) {
                return STATUS_NO_REPAIR_MATERIALS;
            }
            if (!inventory.getStackInSlot(SLOT_OUTPUT).isEmpty()) return STATUS_REPAIR_OUTPUT_FULL;
            if (purifiedWaterTank.getFluidAmount() < REPAIR_WATER_MB) return STATUS_NO_WATER;
            if (energyStorage.getEnergyStored() < energyPerTick(recipe)) return STATUS_NO_ENERGY;
            return STATUS_RUNNING;
        }

        if (sample.damaged()) return STATUS_DAMAGED_CAPSULE;
        if (recipe == null) return STATUS_INVALID_CAPSULE;

        ItemStack feed = inventory.getStackInSlot(SLOT_FEED);
        if (!recipe.feedMatches(feed) || feed.getCount() < recipe.feedCount()) {
            return STATUS_NO_FEED;
        }

        if (purifiedWaterTank.getFluidAmount() < recipe.waterMb()) {
            return STATUS_NO_WATER;
        }

        if (!canSpawnOutput()) {
            return STATUS_OUTPUT_BLOCKED;
        }

        if (energyStorage.getEnergyStored() < energyPerTick(recipe)) {
            return STATUS_NO_ENERGY;
        }

        return STATUS_RUNNING;
    }

    private boolean finishRepair() {
        if (!(level instanceof ServerLevel)) return false;
        BioModuleData.Sample sample = BioModuleData.sample(inventory.getStackInSlot(SLOT_CAPSULE));
        if (sample == null || !sample.damaged() || !BioLootData.isAllowed(sample.entityId())
                || !inventory.getStackInSlot(SLOT_OUTPUT).isEmpty()) {
            return false;
        }

        inventory.extractItem(SLOT_CAPSULE, 1, false);
        inventory.extractItem(SLOT_FEED, 1, false);
        inventory.extractItem(SLOT_BIOGEL, 1, false);
        inventory.extractItem(SLOT_NUTRIENT, 1, false);
        purifiedWaterTank.drain(REPAIR_WATER_MB, IFluidHandler.FluidAction.EXECUTE);
        inventory.setStackInSlot(SLOT_OUTPUT,
                com.wasted.domesurvival.forge.item.BioModuleItem.create(sample.entityId(), false));
        setChanged();
        return true;
    }

    private boolean finishCycle(Recipe recipe) {
        if (!(level instanceof ServerLevel serverLevel) || !canSpawnOutput()) {
            return false;
        }

        Entity entity = recipe.entityType().create(serverLevel);
        if (!(entity instanceof AgeableMob animal)) {
            return false;
        }

        Direction facing = getBlockState().getValue(BioincubatorBlock.FACING);
        BlockPos out = worldPosition.relative(facing);

        animal.moveTo(out.getX() + 0.5D, out.getY(), out.getZ() + 0.5D, facing.toYRot(), 0.0F);
        animal.setBaby(true);

        if (!serverLevel.addFreshEntity(animal)) {
            return false;
        }

        inventory.extractItem(SLOT_CAPSULE, 1, false);
        inventory.extractItem(SLOT_FEED, recipe.feedCount(), false);
        purifiedWaterTank.drain(recipe.waterMb(), IFluidHandler.FluidAction.EXECUTE);

        QuestProgressService.set(serverLevel, "FAUNA_RESTORATION_STARTED", "bioincubator:first_birth");

        // This is a global story milestone. Do not limit it by distance or
        // dimension: an automated/chunk-loaded incubator may complete while
        // the acting team member is elsewhere.
        for (ServerPlayer player : serverLevel.getServer().getPlayerList().getPlayers()) {
            grantFirstBirthAdvancement(player);
        }

        setChanged();
        return true;
    }

    private static void grantFirstBirthAdvancement(ServerPlayer player) {
        player.server.getCommands().performPrefixedCommand(
                player.server.createCommandSourceStack().withSuppressedOutput(),
                "advancement grant "
                        + player.getScoreboardName()
                        + " only domesurvival:quest_actions/bioincubator_first_birth"
        );
    }

    private boolean canSpawnOutput() {
        if (level == null) {
            return false;
        }

        Direction facing = getBlockState().getValue(BioincubatorBlock.FACING);
        BlockPos out = worldPosition.relative(facing);
        BlockPos above = out.above();

        return level.getBlockState(out).getCollisionShape(level, out).isEmpty()
                && level.getBlockState(above).getCollisionShape(level, above).isEmpty();
    }

    @Nullable
    private Recipe currentRecipe() {
        if (mode != MODE_INCUBATION) return null;
        return recipeForCapsule(inventory.getStackInSlot(SLOT_CAPSULE));
    }

    private int baseTicks(@Nullable Recipe recipe){return mode==MODE_REPAIR?REPAIR_PROCESS_TICKS:recipe==null?0:recipe.processTicks();}
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
    private int speciesId() {
        BioModuleData.Sample sample = BioModuleData.sample(inventory.getStackInSlot(SLOT_CAPSULE));
        if (sample == null || !BioLootData.isAllowed(sample.entityId())) return 0;
        EntityType<?> type = net.minecraftforge.registries.ForgeRegistries.ENTITY_TYPES.getValue(sample.entityId());
        return type == null ? 0 : net.minecraft.core.registries.BuiltInRegistries.ENTITY_TYPE.getId(type) + 1;
    }

    @Nullable
    private Recipe recipeForCapsule(ItemStack capsule) {
        if (capsule.isEmpty()) {
            return null;
        }
        BioModuleData.Sample sample = BioModuleData.sample(capsule);
        if (sample == null || sample.damaged()) {
            return null;
        }

        BioLootData.Species species = BioLootData.species(sample.entityId());
        EntityType<?> entityType = net.minecraftforge.registries.ForgeRegistries.ENTITY_TYPES.getValue(sample.entityId());
        Item feedItem = species == null ? null
                : net.minecraftforge.registries.ForgeRegistries.ITEMS.getValue(species.feedItem());
        if (species == null || entityType == null || feedItem == null) {
            if (REPORTED_INVALID_SAMPLES.add(sample.entityId().toString())) {
                LOGGER.warn("Bioincubator rejected unsupported biological sample {}", sample.entityId());
            }
            return null;
        }

        int registryId = net.minecraft.core.registries.BuiltInRegistries.ENTITY_TYPE.getId(entityType);
        return new Recipe(registryId + 1, entityType, feedItem,
                species.feedCount(), species.waterMb(), species.energyPerTick(), species.processTicks());
    }

    public Direction getMachineFacing() {
        BlockState state = getBlockState();
        return state.hasProperty(BioincubatorBlock.FACING)
                ? state.getValue(BioincubatorBlock.FACING)
                : Direction.NORTH;
    }

    private boolean isFrontWorldSide(Direction direction) {
        return direction == getMachineFacing();
    }

    private void syncAllPortStates() {
        if (level == null || level.isClientSide) return;

        BlockState state = level.getBlockState(worldPosition);
        if (!(state.getBlock() instanceof BioincubatorBlock)) return;

        BlockState updated = state;
        for (Direction direction : Direction.values()) {
            updated = updated.setValue(
                    BioincubatorBlock.portProperty(direction),
                    PortVisual.fromMode(sideConfig.getMode(direction))
            );
        }

        if (!updated.equals(state)) {
            level.setBlock(worldPosition, updated, 3);
        } else {
            level.sendBlockUpdated(
                    worldPosition,
                    state,
                    state,
                    net.minecraft.world.level.block.Block.UPDATE_CLIENTS
            );
        }
    }

    private void syncPortState(Direction direction) {
        if (level == null || level.isClientSide) return;

        BlockState state = level.getBlockState(worldPosition);
        if (!(state.getBlock() instanceof BioincubatorBlock)) return;

        PortVisual visual = PortVisual.fromMode(sideConfig.getMode(direction));
        var property = BioincubatorBlock.portProperty(direction);
        BlockState updated = state.setValue(property, visual);

        if (!updated.equals(state)) {
            level.setBlock(worldPosition, updated, 3);
        } else {
            level.sendBlockUpdated(
                    worldPosition,
                    state,
                    state,
                    net.minecraft.world.level.block.Block.UPDATE_CLIENTS
            );
        }
    }

    @Override
    public Component getDisplayName() {
        return Component.translatable("container.domesurvival.bioincubator");
    }

    @Nullable
    @Override
    public AbstractContainerMenu createMenu(int containerId, Inventory inventory, Player player) {
        return new BioincubatorMenu(containerId, inventory, this);
    }

    public ItemStackHandler getInventory() {
        return inventory;
    }

    public ContainerData getDataAccess() {
        return dataAccess;
    }

    @Override
    public void load(CompoundTag tag) {
        super.load(tag);

        if (tag.contains(NBT_INVENTORY)) {
            CompoundTag inventoryTag = tag.getCompound(NBT_INVENTORY).copy();
            inventoryTag.putInt("Size", SLOT_COUNT);
            inventory.deserializeNBT(inventoryTag);
        }

        modules.deserializeNBT(tag.getCompound("Modules"));
        energyStorage.setCapacityInternal(Math.max(tag.getInt(NBT_ENERGY),modules.modifiers().applyBufferCapacity(ENERGY_CAPACITY)));
        energyStorage.setEnergyStoredInternal(tag.getInt(NBT_ENERGY));

        if (tag.contains(NBT_WATER)) {
            purifiedWaterTank.readFromNBT(tag.getCompound(NBT_WATER));
        }

        progress = Math.max(0,tag.getInt(NBT_PROGRESS));
        cycleTicks=Math.max(0,tag.getInt("CycleTicks"));cycleEnergy=Math.max(0,tag.getInt("CycleEnergy"));
        cycleCapsule=ItemStack.of(tag.getCompound("CycleCapsule"));cycleMode=tag.getInt("CycleMode");
        mode = tag.getInt(NBT_MODE) == MODE_REPAIR ? MODE_REPAIR : MODE_INCUBATION;

        if (!sideConfig.load(tag)) {
            applyDefaultSideConfiguration();
        }

        rotateSideConfiguration(Direction.byName(tag.getString("PortFacing")));
        portsNeedSync = true;
        status = calculateStatus(currentRecipe());
        refreshCapabilities();
    }

    @Override
    protected void saveAdditional(CompoundTag tag) {
        super.saveAdditional(tag);

        tag.put(NBT_INVENTORY, inventory.serializeNBT());
        tag.putInt(NBT_ENERGY, energyStorage.getEnergyStored());

        CompoundTag water = new CompoundTag();
        purifiedWaterTank.writeToNBT(water);
        tag.put(NBT_WATER, water);

        tag.putInt(NBT_PROGRESS, progress);
        tag.put("Modules",modules.serializeNBT());tag.putInt("CycleTicks",cycleTicks);tag.putInt("CycleEnergy",cycleEnergy);
        tag.put("CycleCapsule",cycleCapsule.save(new CompoundTag()));tag.putInt("CycleMode",cycleMode);
        tag.putString("PortFacing",getMachineFacing().getName());
        tag.putInt(NBT_MODE, mode);
        sideConfig.save(tag);
    }

    public SideMode sideMode(@Nullable Direction side) {
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
            public int receiveEnergy(int n,boolean sim){int accepted=input()?energyStorage.receiveEnergy(n,sim):0;if(accepted>0&&!sim)setChanged();return accepted;}
            public int extractEnergy(int n,boolean sim){return 0;}
            public boolean canReceive(){return input();} public boolean canExtract(){return false;}
            public int getEnergyStored(){return energyStorage.getEnergyStored();} public int getMaxEnergyStored(){return energyStorage.getMaxEnergyStored();}
        });
        final LazyOptional<IFluidHandler> fluid=LazyOptional.of(()->new IFluidHandler(){
            public int getTanks(){return 1;}
            public FluidStack getFluidInTank(int tank){return tank==0&&input()?purifiedWaterTank.getFluid().copy():FluidStack.EMPTY;}
            public int getTankCapacity(int tank){return tank==0?purifiedWaterTank.getCapacity():0;}
            public boolean isFluidValid(int tank,FluidStack stack){return tank==0&&input()&&purifiedWaterTank.isFluidValid(tank,stack);}
            public int fill(FluidStack stack,FluidAction action){return input()?purifiedWaterTank.fill(stack,action):0;}
            public FluidStack drain(FluidStack stack,FluidAction action){return FluidStack.EMPTY;}
            public FluidStack drain(int amount,FluidAction action){return FluidStack.EMPTY;}
        });
        public int getSlots(){return 5;}
        public ItemStack getStackInSlot(int slot){return slot>=0&&slot<5&&(input()&&slot<4||output()&&slot==4)?inventory.getStackInSlot(slot).copy():ItemStack.EMPTY;}
        public boolean isItemValid(int slot,ItemStack stack){return slot>=0&&slot<4&&input()&&inventory.isItemValid(slot,stack);}
        public ItemStack insertItem(int slot,ItemStack stack,boolean simulate){return isItemValid(slot,stack)?inventory.insertItem(slot,stack,simulate):stack;}
        public ItemStack extractItem(int slot,int amount,boolean simulate){return slot==4&&output()?inventory.extractItem(slot,amount,simulate):ItemStack.EMPTY;}
        public int getSlotLimit(int slot){return slot>=0&&slot<5?inventory.getSlotLimit(slot):0;}
    }

    public record Recipe(
            int speciesId,
            EntityType<?> entityType,
            Item feedItem,
            int feedCount,
            int waterMb,
            int energyPerTick,
            int processTicks
    ) {
        public boolean feedMatches(ItemStack stack) {
            return !stack.isEmpty() && stack.is(feedItem);
        }
    }
}
