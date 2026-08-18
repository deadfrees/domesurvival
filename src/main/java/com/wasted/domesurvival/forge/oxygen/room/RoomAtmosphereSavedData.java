package com.wasted.domesurvival.forge.oxygen.room;

import it.unimi.dsi.fastutil.longs.Long2LongOpenHashMap;
import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.longs.LongIterator;
import it.unimi.dsi.fastutil.longs.LongOpenHashSet;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.saveddata.SavedData;
import org.jetbrains.annotations.Nullable;

/**
 * Persistent oxygen/pressure state for player-built rooms.
 *
 * Geometry remains transient in {@link SealedRoomManager}; only compact room identity,
 * oxygen amount and V62 leak timing/outlet metadata are saved.
 */
public final class RoomAtmosphereSavedData extends SavedData {
    private static final String DATA_NAME = "domesurvival_room_atmospheres";
    private static final String TAG_ROOMS = "Rooms";
    private static final String TAG_ROOM_ID = "RoomId";
    private static final String TAG_GEOMETRY = "Geometry";
    private static final String TAG_REQUIRED = "RequiredOxygen";
    private static final String TAG_OXYGEN = "Oxygen";
    private static final String TAG_OPERATIONAL = "Operational";
    private static final String TAG_PRESSURE_STATE = "PressureState";
    private static final String TAG_LEAK_START_TIME = "LeakStartTime";
    private static final String TAG_LEAK_START_OXYGEN = "LeakStartOxygen";
    private static final String TAG_LEAK_OUTLETS = "LeakOutlets";

    private final Long2ObjectOpenHashMap<AtmosphereRecord> rooms = new Long2ObjectOpenHashMap<>();
    private final Long2LongOpenHashMap roomByLeakOutlet = new Long2LongOpenHashMap();

    public RoomAtmosphereSavedData() {
        roomByLeakOutlet.defaultReturnValue(Long.MIN_VALUE);
    }

    public static RoomAtmosphereSavedData get(ServerLevel level) {
        return level.getDataStorage().computeIfAbsent(
                RoomAtmosphereSavedData::load,
                RoomAtmosphereSavedData::new,
                DATA_NAME
        );
    }

    public static RoomAtmosphereSavedData load(CompoundTag tag) {
        RoomAtmosphereSavedData data = new RoomAtmosphereSavedData();
        ListTag roomList = tag.getList(TAG_ROOMS, Tag.TAG_COMPOUND);
        for (int i = 0; i < roomList.size(); i++) {
            CompoundTag roomTag = roomList.getCompound(i);
            if (!roomTag.contains(TAG_ROOM_ID) || !roomTag.contains(TAG_GEOMETRY)) {
                continue;
            }

            long roomId = roomTag.getLong(TAG_ROOM_ID);
            long geometry = roomTag.getLong(TAG_GEOMETRY);
            int required = Math.max(0, roomTag.getInt(TAG_REQUIRED));
            int oxygen = Math.max(0, Math.min(required, roomTag.getInt(TAG_OXYGEN)));
            PressureState pressureState = roomTag.contains(TAG_PRESSURE_STATE)
                    ? PressureState.byOrdinal(roomTag.getInt(TAG_PRESSURE_STATE))
                    : PressureState.SEALED;
            boolean operational = roomTag.contains(TAG_OPERATIONAL)
                    ? roomTag.getBoolean(TAG_OPERATIONAL)
                    : pressureState == PressureState.SEALED && required > 0 && oxygen >= required;
            long leakStartTime = Math.max(0L, roomTag.getLong(TAG_LEAK_START_TIME));
            int leakStartOxygen = Math.max(0, Math.min(required, roomTag.getInt(TAG_LEAK_START_OXYGEN)));

            LongOpenHashSet leakOutlets = new LongOpenHashSet();
            for (long outlet : roomTag.getLongArray(TAG_LEAK_OUTLETS)) {
                leakOutlets.add(outlet);
            }

            if (oxygen <= 0 && pressureState == PressureState.LEAKING) {
                pressureState = PressureState.DEPRESSURIZED;
            }
            if (pressureState == PressureState.SEALED) {
                leakStartTime = 0L;
                leakStartOxygen = 0;
                leakOutlets.clear();
            }
            if (pressureState != PressureState.SEALED || oxygen <= 0) {
                operational = false;
            }

            AtmosphereRecord record = new AtmosphereRecord(
                    geometry,
                    required,
                    oxygen,
                    pressureState,
                    operational,
                    leakStartTime,
                    leakStartOxygen,
                    leakOutlets
            );
            data.rooms.put(roomId, record);
            data.indexLeakOutlets(roomId, record);
        }
        return data;
    }

    /** Returns/reconciles atmosphere for an exact sealed geometry. */
    public AtmosphereSnapshot getOrCreate(SealedRoomManager.RoomSnapshot room) {
        if (!room.sealed()) {
            return new AtmosphereSnapshot(0, 0, PressureState.DEPRESSURIZED);
        }

        int required = RoomAtmosphereRules.requiredOxygen(room.volume());
        AtmosphereRecord record = rooms.get(room.roomId());

        if (record == null || record.geometrySignature != room.geometrySignature()) {
            replaceRecord(room.roomId(), new AtmosphereRecord(
                    room.geometrySignature(), required, 0, PressureState.SEALED, false,
                    0L, 0, new LongOpenHashSet()
            ));
            setDirty();
            record = rooms.get(room.roomId());
        } else if (record.required != required) {
            record.required = required;
            record.oxygen = Math.min(record.oxygen, required);
            record.leakStartOxygen = Math.min(record.leakStartOxygen, required);
            setDirty();
        }

        return snapshot(record);
    }

    /**
     * Called when a ventilation outlet discovers a sealed room after a geometry change/restart.
     * If this is the same room that was leaking, the remaining pressure is preserved and the
     * leak stops immediately. Different stale outlet mappings are detached instead.
     */
    public AtmosphereSnapshot reconcileSealed(SealedRoomManager.RoomSnapshot room,
                                               long outletKey,
                                               long gameTime) {
        if (!room.sealed()) {
            return new AtmosphereSnapshot(0, 0, PressureState.DEPRESSURIZED);
        }

        long mappedRoomId = roomByLeakOutlet.get(outletKey);
        if (mappedRoomId != Long.MIN_VALUE) {
            AtmosphereRecord leaking = rooms.get(mappedRoomId);
            if (leaking != null) {
                boolean changed = updateLeak(leaking, gameTime);
                if (mappedRoomId == room.roomId()
                        && leaking.geometrySignature == room.geometrySignature()) {
                    clearLeakMappings(mappedRoomId, leaking);
                    leaking.pressureState = PressureState.SEALED;
                    leaking.leakStartTime = 0L;
                    leaking.leakStartOxygen = 0;
                    leaking.operational = leaking.required > 0 && leaking.oxygen >= leaking.required;
                    changed = true;
                } else {
                    leaking.leakOutlets.remove(outletKey);
                    roomByLeakOutlet.remove(outletKey);
                    changed = true;
                }
                if (changed) setDirty();
            } else {
                roomByLeakOutlet.remove(outletKey);
            }
        }

        AtmosphereRecord exact = rooms.get(room.roomId());
        if (exact != null
                && exact.geometrySignature == room.geometrySignature()
                && exact.pressureState != PressureState.SEALED) {
            boolean changed = updateLeak(exact, gameTime);
            clearLeakMappings(room.roomId(), exact);
            exact.pressureState = PressureState.SEALED;
            exact.leakStartTime = 0L;
            exact.leakStartOxygen = 0;
            exact.operational = exact.required > 0 && exact.oxygen >= exact.required;
            if (changed || exact.pressureState == PressureState.SEALED) setDirty();
        }

        return getOrCreate(room);
    }

    /** Adds oxygen only to an actually sealed room. Leaking rooms cannot be compensated. */
    public int addOxygen(SealedRoomManager.RoomSnapshot room, int amount) {
        if (!room.sealed() || amount <= 0) return 0;

        AtmosphereSnapshot current = getOrCreate(room);
        if (current.pressureState() != PressureState.SEALED || current.full()) return 0;

        AtmosphereRecord record = rooms.get(room.roomId());
        int accepted = Math.min(amount, record.required - record.oxygen);
        if (accepted <= 0) return 0;

        record.oxygen += accepted;
        if (record.required > 0 && record.oxygen >= record.required) {
            record.operational = true;
        }
        setDirty();
        return accepted;
    }

    /**
     * Life-support consumption for occupants of one canonical sealed room.
     * This is intentionally separate from filler ticks so several ventilation outlets
     * can never multiply the per-player breathing cost.
     */
    public int consumeOxygen(SealedRoomManager.RoomSnapshot room, int amount) {
        if (!room.sealed() || amount <= 0) return 0;

        AtmosphereRecord record = rooms.get(room.roomId());
        if (record == null
                || record.geometrySignature != room.geometrySignature()
                || record.pressureState != PressureState.SEALED
                || !record.operational
                || record.oxygen <= 0) {
            return 0;
        }

        int consumed = Math.min(amount, record.oxygen);
        if (consumed <= 0) return 0;

        record.oxygen -= consumed;
        if (record.oxygen <= 0) {
            record.oxygen = 0;
            record.operational = false;
        }
        setDirty();
        return consumed;
    }

    /** Starts or joins a V62 leak session for a previously sealed room. */
    public LeakAtmosphereSnapshot startLeak(SealedRoomManager.RoomSnapshot room,
                                            long outletKey,
                                            long gameTime) {
        AtmosphereRecord record = rooms.get(room.roomId());
        if (record == null || record.geometrySignature != room.geometrySignature()) {
            return new LeakAtmosphereSnapshot(
                    room.roomId(), room.geometrySignature(), 0,
                    RoomAtmosphereRules.requiredOxygen(room.volume()),
                    PressureState.DEPRESSURIZED
            );
        }

        boolean changed = updateLeak(record, gameTime);
        if (record.pressureState == PressureState.SEALED) {
            record.operational = false;
            if (record.oxygen > 0) {
                record.pressureState = PressureState.LEAKING;
                record.leakStartTime = Math.max(0L, gameTime);
                record.leakStartOxygen = record.oxygen;
            } else {
                record.pressureState = PressureState.DEPRESSURIZED;
                record.leakStartTime = 0L;
                record.leakStartOxygen = 0;
            }
            changed = true;
        }

        if (record.leakOutlets.add(outletKey)) {
            roomByLeakOutlet.put(outletKey, room.roomId());
            changed = true;
        }

        if (changed) setDirty();
        return leakSnapshot(room.roomId(), record);
    }

    /** Returns current leaking/depressurized pressure associated with this machine outlet. */
    @Nullable
    public LeakAtmosphereSnapshot getLeakByOutlet(long outletKey, long gameTime) {
        long roomId = roomByLeakOutlet.get(outletKey);
        if (roomId == Long.MIN_VALUE) return null;

        AtmosphereRecord record = rooms.get(roomId);
        if (record == null || record.pressureState == PressureState.SEALED) {
            roomByLeakOutlet.remove(outletKey);
            return null;
        }

        if (updateLeak(record, gameTime)) setDirty();
        return leakSnapshot(roomId, record);
    }

    /** Direct lookup used by the transient leaking-room geometry cache. */
    @Nullable
    public LeakAtmosphereSnapshot getLeak(long roomId, long geometrySignature, long gameTime) {
        AtmosphereRecord record = rooms.get(roomId);
        if (record == null
                || record.geometrySignature != geometrySignature
                || record.pressureState == PressureState.SEALED) {
            return null;
        }
        if (updateLeak(record, gameTime)) setDirty();
        return leakSnapshot(roomId, record);
    }

    /** Advances all persisted leak curves. Cost is O(number of known pressurized rooms). */
    public void tickLeaks(long gameTime) {
        boolean changed = false;
        for (AtmosphereRecord record : rooms.values()) {
            if (record.pressureState == PressureState.LEAKING) {
                changed |= updateLeak(record, gameTime);
            }
        }
        if (changed) setDirty();
    }

    /** Remove one destroyed ventilation outlet from persistent reverse leak lookup. */
    public void forgetOutlet(long outletKey) {
        long roomId = roomByLeakOutlet.remove(outletKey);
        if (roomId == Long.MIN_VALUE) return;
        AtmosphereRecord record = rooms.get(roomId);
        if (record != null && record.leakOutlets.remove(outletKey)) {
            setDirty();
        }
    }

    /** V61 behavior remains: a normal sealed room becomes breathable only at 100%. */
    public boolean isBreathable(SealedRoomManager.RoomSnapshot room) {
        if (!room.sealed()) return false;
        AtmosphereRecord record = rooms.get(room.roomId());
        return record != null
                && record.geometrySignature == room.geometrySignature()
                && record.pressureState == PressureState.SEALED
                && record.required > 0
                && record.operational
                && record.oxygen > 0;
    }

    /** Emergency breathability while V62 pressure is escaping from the old sealed volume. */
    public boolean isLeakingBreathable(long roomId, long geometrySignature, long gameTime) {
        AtmosphereRecord record = rooms.get(roomId);
        if (record == null
                || record.geometrySignature != geometrySignature
                || record.pressureState != PressureState.LEAKING) {
            return false;
        }
        if (updateLeak(record, gameTime)) setDirty();
        return RoomAtmosphereRules.pressurePermille(record.oxygen, record.required)
                >= RoomAtmosphereRules.LEAK_BREATHABLE_PRESSURE_PERMILLE;
    }

    @Override
    public CompoundTag save(CompoundTag tag) {
        ListTag roomList = new ListTag();
        rooms.long2ObjectEntrySet().forEach(entry -> {
            AtmosphereRecord record = entry.getValue();
            CompoundTag roomTag = new CompoundTag();
            roomTag.putLong(TAG_ROOM_ID, entry.getLongKey());
            roomTag.putLong(TAG_GEOMETRY, record.geometrySignature);
            roomTag.putInt(TAG_REQUIRED, record.required);
            roomTag.putInt(TAG_OXYGEN, record.oxygen);
            roomTag.putBoolean(TAG_OPERATIONAL, record.operational);
            roomTag.putInt(TAG_PRESSURE_STATE, record.pressureState.ordinal());
            roomTag.putLong(TAG_LEAK_START_TIME, record.leakStartTime);
            roomTag.putInt(TAG_LEAK_START_OXYGEN, record.leakStartOxygen);
            roomTag.putLongArray(TAG_LEAK_OUTLETS, record.leakOutlets.toLongArray());
            roomList.add(roomTag);
        });
        tag.put(TAG_ROOMS, roomList);
        return tag;
    }

    private boolean updateLeak(AtmosphereRecord record, long gameTime) {
        if (record.pressureState != PressureState.LEAKING) return false;

        if (record.oxygen <= 0 || record.leakStartOxygen <= 0) {
            record.oxygen = 0;
            record.pressureState = PressureState.DEPRESSURIZED;
            record.operational = false;
            record.leakStartTime = 0L;
            record.leakStartOxygen = 0;
            return true;
        }

        long elapsed = Math.max(0L, gameTime - record.leakStartTime);
        long duration = RoomAtmosphereRules.depressurizationTicksForOxygen(record.leakStartOxygen);
        if (duration <= 0L || elapsed >= duration) {
            record.oxygen = 0;
            record.pressureState = PressureState.DEPRESSURIZED;
            record.operational = false;
            record.leakStartTime = 0L;
            record.leakStartOxygen = 0;
            return true;
        }

        long escaped = RoomAtmosphereRules.escapedOxygen(elapsed);
        int target = (int) Math.max(0L, (long) record.leakStartOxygen - escaped);
        target = Math.min(target, record.oxygen);
        if (target == record.oxygen) return false;

        record.oxygen = target;
        return true;
    }

    private void replaceRecord(long roomId, AtmosphereRecord replacement) {
        AtmosphereRecord old = rooms.put(roomId, replacement);
        if (old != null) {
            clearLeakMappings(roomId, old);
        }
        indexLeakOutlets(roomId, replacement);
    }

    private void indexLeakOutlets(long roomId, AtmosphereRecord record) {
        if (record.pressureState == PressureState.SEALED) return;
        LongIterator iterator = record.leakOutlets.iterator();
        while (iterator.hasNext()) {
            roomByLeakOutlet.put(iterator.nextLong(), roomId);
        }
    }

    private void clearLeakMappings(long roomId, AtmosphereRecord record) {
        LongIterator iterator = record.leakOutlets.iterator();
        while (iterator.hasNext()) {
            long outlet = iterator.nextLong();
            if (roomByLeakOutlet.get(outlet) == roomId) {
                roomByLeakOutlet.remove(outlet);
            }
        }
        record.leakOutlets.clear();
    }

    private static AtmosphereSnapshot snapshot(AtmosphereRecord record) {
        return new AtmosphereSnapshot(record.oxygen, record.required, record.pressureState);
    }

    private static LeakAtmosphereSnapshot leakSnapshot(long roomId, AtmosphereRecord record) {
        return new LeakAtmosphereSnapshot(
                roomId,
                record.geometrySignature,
                record.oxygen,
                record.required,
                record.pressureState
        );
    }

    public enum PressureState {
        SEALED,
        LEAKING,
        DEPRESSURIZED;

        public static PressureState byOrdinal(int ordinal) {
            PressureState[] values = values();
            return ordinal >= 0 && ordinal < values.length ? values[ordinal] : SEALED;
        }
    }

    public record AtmosphereSnapshot(int oxygen, int required, PressureState pressureState) {
        public int missing() {
            return Math.max(0, required - oxygen);
        }

        public boolean full() {
            return required > 0 && oxygen >= required;
        }

        public int pressurePermille() {
            return RoomAtmosphereRules.pressurePermille(oxygen, required);
        }
    }

    public record LeakAtmosphereSnapshot(
            long roomId,
            long geometrySignature,
            int oxygen,
            int required,
            PressureState pressureState
    ) {
        public int pressurePermille() {
            return RoomAtmosphereRules.pressurePermille(oxygen, required);
        }

        public boolean leaking() {
            return pressureState == PressureState.LEAKING;
        }

        public boolean depressurized() {
            return pressureState == PressureState.DEPRESSURIZED;
        }
    }

    private static final class AtmosphereRecord {
        private final long geometrySignature;
        private int required;
        private int oxygen;
        private PressureState pressureState;
        private boolean operational;
        private long leakStartTime;
        private int leakStartOxygen;
        private final LongOpenHashSet leakOutlets;

        private AtmosphereRecord(long geometrySignature,
                                 int required,
                                 int oxygen,
                                 PressureState pressureState,
                                 boolean operational,
                                 long leakStartTime,
                                 int leakStartOxygen,
                                 LongOpenHashSet leakOutlets) {
            this.geometrySignature = geometrySignature;
            this.required = Math.max(0, required);
            this.oxygen = Math.max(0, Math.min(this.required, oxygen));
            this.pressureState = pressureState == null ? PressureState.SEALED : pressureState;
            this.operational = this.pressureState == PressureState.SEALED
                    && this.required > 0
                    && this.oxygen > 0
                    && operational;
            this.leakStartTime = Math.max(0L, leakStartTime);
            this.leakStartOxygen = Math.max(0, Math.min(this.required, leakStartOxygen));
            this.leakOutlets = leakOutlets == null ? new LongOpenHashSet() : leakOutlets;
        }
    }
}
