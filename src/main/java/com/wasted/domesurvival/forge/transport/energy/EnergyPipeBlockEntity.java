package com.wasted.domesurvival.forge.transport.energy;

import com.wasted.domesurvival.forge.registry.ModBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

import java.util.EnumMap;
import java.util.Map;

public final class EnergyPipeBlockEntity extends BlockEntity {
    private static final String SIDE_MODES_TAG = "SideModes";

    private final Map<Direction, EnergyPipeSideMode> sideModes = new EnumMap<>(Direction.class);

    public EnergyPipeBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.ENERGY_PIPE.get(), pos, state);
        for (Direction direction : Direction.values()) {
            sideModes.put(direction, EnergyPipeSideMode.AUTO);
        }
    }

    public EnergyPipeSideMode getSideMode(Direction side) {
        return sideModes.getOrDefault(side, EnergyPipeSideMode.AUTO);
    }

    public EnergyPipeSideMode cycleSideMode(Direction side, boolean reverse) {
        EnergyPipeSideMode next = getSideMode(side).cycle(reverse);
        setSideMode(side, next);
        return next;
    }

    public void setSideMode(Direction side, EnergyPipeSideMode mode) {
        if (mode == getSideMode(side)) return;

        sideModes.put(side, mode);
        setChanged();

        Level level = getLevel();
        if (level == null || level.isClientSide) return;

        BlockState oldState = getBlockState();
        BlockState refreshed = EnergyPipeBlock.refreshConnections(level, worldPosition, oldState);

        if (!refreshed.equals(oldState)) {
            level.setBlock(
                    worldPosition,
                    refreshed,
                    Block.UPDATE_NEIGHBORS | Block.UPDATE_CLIENTS
            );
        } else {
            level.sendBlockUpdated(
                    worldPosition,
                    oldState,
                    oldState,
                    Block.UPDATE_CLIENTS
            );
        }
    }

    @Override
    protected void saveAdditional(CompoundTag tag) {
        super.saveAdditional(tag);

        CompoundTag modes = new CompoundTag();
        for (Direction direction : Direction.values()) {
            modes.putByte(direction.getName(), (byte) getSideMode(direction).ordinal());
        }
        tag.put(SIDE_MODES_TAG, modes);
    }

    @Override
    public void load(CompoundTag tag) {
        super.load(tag);

        for (Direction direction : Direction.values()) {
            sideModes.put(direction, EnergyPipeSideMode.AUTO);
        }

        if (!tag.contains(SIDE_MODES_TAG, Tag.TAG_COMPOUND)) return;

        CompoundTag modes = tag.getCompound(SIDE_MODES_TAG);
        EnergyPipeSideMode[] values = EnergyPipeSideMode.values();

        for (Direction direction : Direction.values()) {
            if (!modes.contains(direction.getName(), Tag.TAG_BYTE)) continue;

            int ordinal = modes.getByte(direction.getName());
            if (ordinal >= 0 && ordinal < values.length) {
                sideModes.put(direction, values[ordinal]);
            }
        }
    }

    public static void serverTick(Level level, BlockPos pos,
                                  BlockState state, EnergyPipeBlockEntity blockEntity) {
        if (!(level instanceof ServerLevel serverLevel)) return;
        EnergyPipeNetwork.tick(serverLevel, pos);
    }
}
