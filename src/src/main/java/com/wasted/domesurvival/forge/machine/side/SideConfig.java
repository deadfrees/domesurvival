package com.wasted.domesurvival.forge.machine.side;

import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;

import java.util.Collections;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.Map;
import java.util.Set;

/**
 * Per-block-entity configuration of the six Minecraft directions.
 *
 * <p>Each supported resource channel receives an independent mode for every side.
 * The class deliberately contains no item, energy or fluid capability code. Concrete
 * machines use {@link #allowsInput(ResourceChannel, Direction)} and
 * {@link #allowsOutput(ResourceChannel, Direction)} when exposing their capabilities.</p>
 */
public final class SideConfig {
    private static final String NBT_ROOT = "SideConfig";

    private final EnumSet<ResourceChannel> supportedChannels;
    private final EnumMap<ResourceChannel, EnumMap<Direction, SideMode>> modes =
            new EnumMap<>(ResourceChannel.class);

    public SideConfig(ResourceChannel firstChannel, ResourceChannel... additionalChannels) {
        supportedChannels = EnumSet.of(firstChannel, additionalChannels);

        for (ResourceChannel channel : supportedChannels) {
            EnumMap<Direction, SideMode> sideModes = new EnumMap<>(Direction.class);
            for (Direction direction : Direction.values()) {
                sideModes.put(direction, SideMode.DISABLED);
            }
            modes.put(channel, sideModes);
        }
    }

    public Set<ResourceChannel> getSupportedChannels() {
        return Collections.unmodifiableSet(supportedChannels);
    }

    public boolean supports(ResourceChannel channel) {
        return supportedChannels.contains(channel);
    }

    public SideMode getMode(ResourceChannel channel, Direction side) {
        Map<Direction, SideMode> sideModes = modes.get(channel);
        if (sideModes == null) {
            return SideMode.DISABLED;
        }
        return sideModes.getOrDefault(side, SideMode.DISABLED);
    }

    public boolean setMode(ResourceChannel channel, Direction side, SideMode mode) {
        EnumMap<Direction, SideMode> sideModes = modes.get(channel);
        if (sideModes == null || side == null || mode == null) {
            return false;
        }

        SideMode previous = sideModes.put(side, mode);
        return previous != mode;
    }

    public SideMode cycleMode(ResourceChannel channel, Direction side) {
        SideMode next = getMode(channel, side).next();
        setMode(channel, side, next);
        return next;
    }

    public SideMode cycleMode(ResourceChannel channel, Direction side, Set<SideMode> allowedModes) {
        SideMode next = getMode(channel, side).nextAllowed(allowedModes);
        setMode(channel, side, next);
        return next;
    }

    public void setAll(ResourceChannel channel, SideMode mode) {
        EnumMap<Direction, SideMode> sideModes = modes.get(channel);
        if (sideModes == null || mode == null) {
            return;
        }

        for (Direction direction : Direction.values()) {
            sideModes.put(direction, mode);
        }
    }

    public boolean allowsInput(ResourceChannel channel, Direction side) {
        return getMode(channel, side).allowsInput();
    }

    public boolean allowsOutput(ResourceChannel channel, Direction side) {
        return getMode(channel, side).allowsOutput();
    }

    /**
     * Writes this configuration under a dedicated NBT sub-tag so concrete machines
     * can safely store inventories, energy and processing progress alongside it.
     */
    public void save(CompoundTag parentTag) {
        CompoundTag root = new CompoundTag();

        for (ResourceChannel channel : supportedChannels) {
            CompoundTag channelTag = new CompoundTag();
            for (Direction direction : Direction.values()) {
                channelTag.putString(direction.getName(), getMode(channel, direction).getSerializedName());
            }
            root.put(channel.getSerializedName(), channelTag);
        }

        parentTag.put(NBT_ROOT, root);
    }

    /**
     * Loads only channels supported by the current machine. Missing/unknown values
     * safely fall back to DISABLED, which prevents old or malformed saves from
     * accidentally exposing a capability on an unintended side.
     */
    public void load(CompoundTag parentTag) {
        reset();

        if (!parentTag.contains(NBT_ROOT)) {
            return;
        }

        CompoundTag root = parentTag.getCompound(NBT_ROOT);
        for (ResourceChannel channel : supportedChannels) {
            if (!root.contains(channel.getSerializedName())) {
                continue;
            }

            CompoundTag channelTag = root.getCompound(channel.getSerializedName());
            for (Direction direction : Direction.values()) {
                if (!channelTag.contains(direction.getName())) {
                    continue;
                }

                setMode(
                        channel,
                        direction,
                        SideMode.fromSerializedName(channelTag.getString(direction.getName()))
                );
            }
        }
    }

    public void reset() {
        for (ResourceChannel channel : supportedChannels) {
            setAll(channel, SideMode.DISABLED);
        }
    }
}
