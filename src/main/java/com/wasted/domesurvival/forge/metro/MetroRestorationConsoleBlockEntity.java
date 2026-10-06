package com.wasted.domesurvival.forge.metro;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

import java.util.UUID;

public final class MetroRestorationConsoleBlockEntity extends BlockEntity implements MenuProvider {
    public static final int DATA_STATE = 0;
    public static final int DATA_DISCOVERED = 1;
    public static final int DATA_RESTORED = 2;
    public static final int DATA_DEPOSIT_START = 3;
    public static final int DATA_COUNT = DATA_DEPOSIT_START + MetroRestorationRequirements.all().size();

    private static final String NBT_STATION_ID = "StationId";

    @Nullable private UUID stationId;
    private StationState state = StationState.ABANDONED;
    private boolean discovered;
    private boolean restored;
    private final int[] deposited = new int[MetroRestorationRequirements.all().size()];

    private final ContainerData dataAccess = new ContainerData() {
        @Override
        public int get(int index) {
            if (index == DATA_STATE) return state.ordinal();
            if (index == DATA_DISCOVERED) return discovered ? 1 : 0;
            if (index == DATA_RESTORED) return restored ? 1 : 0;
            int requirementIndex = index - DATA_DEPOSIT_START;
            if (requirementIndex >= 0 && requirementIndex < deposited.length) return deposited[requirementIndex];
            return 0;
        }

        @Override public void set(int index, int value) { }
        @Override public int getCount() { return DATA_COUNT; }
    };

    public MetroRestorationConsoleBlockEntity(BlockPos pos, BlockState state) {
        super(MetroRegistry.METRO_RESTORATION_CONSOLE_BLOCK_ENTITY.get(), pos, state);
    }

    @Override
    public void onLoad() {
        super.onLoad();
        if (level instanceof ServerLevel serverLevel) {
            ensureRegistered(serverLevel);
            MetroRestorationService.recoverInterrupted(serverLevel, this);
            MetroRestorationService.ensureRestoredVisuals(serverLevel, this);
        }
    }

    public void ensureRegistered(ServerLevel serverLevel) {
        UUID resolved = MetroStationManager.ensureRegistered(serverLevel, stationId, worldPosition);
        if (!resolved.equals(stationId)) {
            stationId = resolved;
            setChanged();
        }
        refreshFromSavedData(serverLevel);
    }

    public void discover(ServerLevel serverLevel) {
        ensureRegistered(serverLevel);
        if (stationId == null) return;
        MetroStationManager.discover(serverLevel, stationId);
        refreshFromSavedData(serverLevel);
        MetroRestorationService.ensureRestoredVisuals(serverLevel, this);
    }

    public void refreshFromSavedData(ServerLevel serverLevel) {
        if (stationId == null) return;
        MetroStationRecord record = MetroStationSavedData.get(serverLevel.getServer()).get(stationId);
        if (record == null) return;
        state = record.state();
        discovered = record.discovered();
        restored = record.restored();
        for (int i = 0; i < deposited.length; i++) {
            MetroRestorationRequirements.Requirement requirement = MetroRestorationRequirements.all().get(i);
            deposited[i] = record.restored()
                    ? requirement.required()
                    : Math.min(requirement.required(), record.restorationDeposit(requirement.id()));
        }
        setChanged();
    }

    @Nullable
    public UUID getStationId() {
        return stationId;
    }

    public ContainerData getDataAccess() {
        return dataAccess;
    }

    @Override
    protected void saveAdditional(CompoundTag tag) {
        super.saveAdditional(tag);
        if (stationId != null) tag.putUUID(NBT_STATION_ID, stationId);
    }

    @Override
    public void load(CompoundTag tag) {
        super.load(tag);
        stationId = tag.hasUUID(NBT_STATION_ID) ? tag.getUUID(NBT_STATION_ID) : null;
    }

    @Override
    public Component getDisplayName() {
        if (stationId != null && level instanceof ServerLevel serverLevel) {
            MetroStationRecord record = MetroStationSavedData.get(serverLevel.getServer()).get(stationId);
            if (record != null) return Component.literal(record.stationName());
        }
        return Component.literal("\u041a\u043e\u043d\u0441\u043e\u043b\u044c \u0432\u043e\u0441\u0441\u0442\u0430\u043d\u043e\u0432\u043b\u0435\u043d\u0438\u044f \u043c\u0435\u0442\u0440\u043e");
    }

    @Nullable
    @Override
    public AbstractContainerMenu createMenu(int containerId, Inventory inventory, Player player) {
        return new MetroRestorationConsoleMenu(containerId, inventory, this);
    }
}
