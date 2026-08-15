package com.wasted.domesurvival.forge.oxygen.room;

import com.wasted.domesurvival.forge.DomeSurvival;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.piston.PistonStructureResolver;
import net.minecraftforge.common.util.BlockSnapshot;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.living.LivingDestroyBlockEvent;
import net.minecraftforge.event.level.BlockEvent;
import net.minecraftforge.event.level.ChunkEvent;
import net.minecraftforge.event.level.ExplosionEvent;
import net.minecraftforge.event.level.LevelEvent;
import net.minecraftforge.event.level.PistonEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * Event-driven invalidation for V60 room geometry caches.
 *
 * Every handler is a reverse-index lookup. No handler starts a room scan itself;
 * the next ventilation-machine query performs a single bounded rebuild if needed.
 */
@Mod.EventBusSubscriber(
        modid = DomeSurvival.MOD_ID,
        bus = Mod.EventBusSubscriber.Bus.FORGE
)
public final class SealedRoomEvents {
    private SealedRoomEvents() {
    }

    @SubscribeEvent
    public static void onBlockBreak(BlockEvent.BreakEvent event) {
        if (event.getLevel() instanceof ServerLevel level) {
            SealedRoomManager.invalidateAround(level, event.getPos());
        }
    }

    @SubscribeEvent
    public static void onBlockPlace(BlockEvent.EntityPlaceEvent event) {
        if (event.getLevel() instanceof ServerLevel level) {
            SealedRoomManager.invalidateAround(level, event.getPos());
        }
    }

    @SubscribeEvent
    public static void onMultiPlace(BlockEvent.EntityMultiPlaceEvent event) {
        if (!(event.getLevel() instanceof ServerLevel level)) return;
        for (BlockSnapshot snapshot : event.getReplacedBlockSnapshots()) {
            SealedRoomManager.invalidateAround(level, snapshot.getPos());
        }
    }

    @SubscribeEvent
    public static void onFluidPlace(BlockEvent.FluidPlaceBlockEvent event) {
        if (event.getLevel() instanceof ServerLevel level) {
            SealedRoomManager.invalidateAround(level, event.getPos());
        }
    }

    /**
     * Server-only Forge event fired for block physics notifications. It covers
     * many state-only changes (doors/trapdoors/redstone-driven blocks) that do
     * not create a placement/break event.
     */
    @SubscribeEvent
    public static void onNeighborNotify(BlockEvent.NeighborNotifyEvent event) {
        if (!(event.getLevel() instanceof ServerLevel level)) return;

        // The event position is the block whose state/physics update originated.
        // Adjacent blocks that actually change state will emit their own notification.
        SealedRoomManager.invalidateAt(level, event.getPos());
    }

    @SubscribeEvent
    public static void onLivingDestroyBlock(LivingDestroyBlockEvent event) {
        if (event.getEntity().level() instanceof ServerLevel level) {
            SealedRoomManager.invalidateAround(level, event.getPos());
        }
    }

    @SubscribeEvent
    public static void onExplosion(ExplosionEvent.Detonate event) {
        if (!(event.getLevel() instanceof ServerLevel level)) return;
        for (BlockPos pos : event.getAffectedBlocks()) {
            SealedRoomManager.invalidateAround(level, pos);
        }
    }

    /**
     * Invalidate piston source/destination cells before movement. This mirrors
     * Forge's own PistonStructureResolver usage and runs only on piston actions.
     */
    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void onPistonPre(PistonEvent.Pre event) {
        if (event.isCanceled() || !(event.getLevel() instanceof ServerLevel level)) return;

        PistonStructureResolver resolver = event.getStructureHelper();
        if (resolver == null || !resolver.resolve()) return;

        for (BlockPos pos : resolver.getToPush()) {
            SealedRoomManager.invalidateAround(level, pos);
            SealedRoomManager.invalidateAround(level, pos.relative(event.getDirection()));
        }
        for (BlockPos pos : resolver.getToDestroy()) {
            SealedRoomManager.invalidateAround(level, pos);
        }
    }

    @SubscribeEvent
    public static void onChunkLoad(ChunkEvent.Load event) {
        if (event.getLevel() instanceof ServerLevel level) {
            SealedRoomManager.invalidateChunk(level, event.getChunk().getPos());
        }
    }

    @SubscribeEvent
    public static void onChunkUnload(ChunkEvent.Unload event) {
        if (event.getLevel() instanceof ServerLevel level) {
            SealedRoomManager.invalidateChunk(level, event.getChunk().getPos());
        }
    }

    @SubscribeEvent
    public static void onLevelTick(TickEvent.LevelTickEvent event) {
        if (event.phase == TickEvent.Phase.END && event.level instanceof ServerLevel level) {
            SealedRoomManager.tickLevel(level);
        }
    }

    @SubscribeEvent
    public static void onLevelUnload(LevelEvent.Unload event) {
        if (event.getLevel() instanceof ServerLevel level) {
            SealedRoomManager.clearLevel(level);
        }
    }
}
