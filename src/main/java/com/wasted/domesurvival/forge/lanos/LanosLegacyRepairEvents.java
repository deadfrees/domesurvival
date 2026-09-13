package com.wasted.domesurvival.forge.lanos;

import com.wasted.domesurvival.forge.DomeSurvival;
import com.wasted.domesurvival.forge.block.DecorativeLanosBlock;
import com.wasted.domesurvival.forge.block.ModBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/** Repairs cars already saved before the linked hitbox/trunk cells existed. */
@Mod.EventBusSubscriber(modid = DomeSurvival.MOD_ID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class LanosLegacyRepairEvents {
    private static final int HORIZONTAL_RADIUS = 12;
    private static final int VERTICAL_RADIUS = 4;

    private LanosLegacyRepairEvents() {
    }

    @SubscribeEvent
    public static void onPlayerTick(TickEvent.PlayerTickEvent event) {
        if (event.phase != TickEvent.Phase.END
                || !(event.player instanceof ServerPlayer player)
                || player.tickCount % 40 != 0) {
            return;
        }

        BlockPos center = player.blockPosition();
        BlockPos.MutableBlockPos cursor = new BlockPos.MutableBlockPos();
        for (int y = -VERTICAL_RADIUS; y <= VERTICAL_RADIUS; y++) {
            for (int z = -HORIZONTAL_RADIUS; z <= HORIZONTAL_RADIUS; z++) {
                for (int x = -HORIZONTAL_RADIUS; x <= HORIZONTAL_RADIUS; x++) {
                    cursor.set(center.getX() + x, center.getY() + y, center.getZ() + z);
                    BlockState state = player.serverLevel().getBlockState(cursor);
                    if (state.is(ModBlocks.LANOS_DECORATIVE.get())
                            || state.is(ModBlocks.LANOS_ABANDONED.get())) {
                        DecorativeLanosBlock.ensureFootprint(player.serverLevel(), cursor.immutable(), state);
                    }
                }
            }
        }
    }
}
