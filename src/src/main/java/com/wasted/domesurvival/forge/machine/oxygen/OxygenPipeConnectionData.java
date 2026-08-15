package com.wasted.domesurvival.forge.machine.oxygen;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.saveddata.SavedData;

import java.util.HashMap;
import java.util.Map;

public final class OxygenPipeConnectionData extends SavedData {
    private static final String DATA_NAME = "domesurvival_oxygen_pipe_disconnects";
    private static final String LIST = "Entries";
    private final Map<Long, Byte> masks = new HashMap<>();

    public static OxygenPipeConnectionData get(ServerLevel level) {
        return level.getDataStorage().computeIfAbsent(
                OxygenPipeConnectionData::load,
                OxygenPipeConnectionData::new,
                DATA_NAME
        );
    }

    public static OxygenPipeConnectionData load(CompoundTag tag) {
        OxygenPipeConnectionData data = new OxygenPipeConnectionData();
        ListTag list = tag.getList(LIST, Tag.TAG_COMPOUND);
        for (int i = 0; i < list.size(); i++) {
            CompoundTag entry = list.getCompound(i);
            byte mask = entry.getByte("Mask");
            if (mask != 0) data.masks.put(entry.getLong("Pos"), mask);
        }
        return data;
    }

    public static boolean isDisconnected(LevelAccessor level, BlockPos pos, Direction side) {
        return level instanceof ServerLevel serverLevel
                && get(serverLevel).isDisconnected(pos, side);
    }

    public boolean isDisconnected(BlockPos pos, Direction side) {
        int bit = 1 << side.ordinal();
        return (Byte.toUnsignedInt(masks.getOrDefault(pos.asLong(), (byte) 0)) & bit) != 0;
    }

    /** @return true when the side is disconnected after toggling. */
    public boolean toggle(BlockPos pos, Direction side) {
        long key = pos.asLong();
        int bit = 1 << side.ordinal();
        int mask = Byte.toUnsignedInt(masks.getOrDefault(key, (byte) 0));
        mask ^= bit;
        if (mask == 0) masks.remove(key);
        else masks.put(key, (byte) mask);
        setDirty();
        return (mask & bit) != 0;
    }

    public void clear(BlockPos pos) {
        if (masks.remove(pos.asLong()) != null) setDirty();
    }

    @Override
    public CompoundTag save(CompoundTag tag) {
        ListTag list = new ListTag();
        for (Map.Entry<Long, Byte> entry : masks.entrySet()) {
            if (entry.getValue() == 0) continue;
            CompoundTag item = new CompoundTag();
            item.putLong("Pos", entry.getKey());
            item.putByte("Mask", entry.getValue());
            list.add(item);
        }
        tag.put(LIST, list);
        return tag;
    }
}
