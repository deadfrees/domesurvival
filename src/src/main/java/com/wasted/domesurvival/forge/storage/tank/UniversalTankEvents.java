package com.wasted.domesurvival.forge.storage.tank;

import com.wasted.domesurvival.forge.DomeSurvival;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.level.BlockEvent;
import net.minecraftforge.event.level.LevelEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.WeakHashMap;

/** One lightweight server tick registry for active reservoir masters. */
@Mod.EventBusSubscriber(modid = DomeSurvival.MOD_ID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class UniversalTankEvents {
    /** One position per connected reservoir structure, never one ticker per cell. */
    private static final Map<ServerLevel, Set<Long>> ACTIVE_MASTERS = new WeakHashMap<>();

    private UniversalTankEvents() {
    }

    static void trackMaster(ServerLevel level, BlockPos masterPos) {
        ACTIVE_MASTERS
                .computeIfAbsent(level, ignored -> new HashSet<>())
                .add(masterPos.asLong());
    }

    @SubscribeEvent
    public static void onLevelTick(TickEvent.LevelTickEvent event) {
        if (event.phase != TickEvent.Phase.END || !(event.level instanceof ServerLevel level)) return;

        Set<Long> masters = ACTIVE_MASTERS.get(level);
        if (masters == null || masters.isEmpty()) return;

        Iterator<Long> iterator = masters.iterator();
        while (iterator.hasNext()) {
            BlockPos pos = BlockPos.of(iterator.next());
            if (!level.hasChunkAt(pos)) continue;

            BlockEntity blockEntity = level.getBlockEntity(pos);
            if (!(blockEntity instanceof UniversalTankBlockEntity tank)
                    || !tank.isMaster()) {
                iterator.remove();
                continue;
            }

            tank.serverMasterTick();
        }
    }

    /**
     * Guaranteed normal survival drop for reservoir cells.
     *
     * This listener runs at LOWEST priority so an earlier protection/cancel
     * listener gets the final say first. Creative breaking intentionally gives
     * no item. The tank's getDrops() is empty, therefore this produces exactly
     * one plain cell and cannot double with vanilla block loot.
     */
    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void onBreak(BlockEvent.BreakEvent event) {
        if (event.isCanceled()) return;
        if (!(event.getLevel() instanceof ServerLevel level)) return;
        if (!(event.getState().getBlock() instanceof UniversalTankBlock)) return;
        if (event.getPlayer() == null || event.getPlayer().isCreative()) return;

        ItemStack drop = new ItemStack(UniversalTankRegistry.UNIVERSAL_TANK_ITEM.get());
        ItemEntity itemEntity = new ItemEntity(
                level,
                event.getPos().getX() + 0.5D,
                event.getPos().getY() + 0.5D,
                event.getPos().getZ() + 0.5D,
                drop
        );
        itemEntity.setDefaultPickUpDelay();
        level.addFreshEntity(itemEntity);
    }

    @SubscribeEvent
    public static void onLevelUnload(LevelEvent.Unload event) {
        if (event.getLevel() instanceof ServerLevel level) {
            ACTIVE_MASTERS.remove(level);
        }
    }

}
