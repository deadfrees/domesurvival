package com.wasted.domesurvival.forge.metro.dome;

import com.mojang.logging.LogUtils;
import com.wasted.domesurvival.core.dome.DomeSpec;
import com.wasted.domesurvival.forge.DomeSurvival;
import com.wasted.domesurvival.forge.data.DomeSavedData;
import com.wasted.domesurvival.forge.metro.network.MetroNetworkService;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;

import java.util.UUID;

/** Stage-4 server API for the Dome metro unlock and one-time station construction. */
public final class DomeMetroService {
    private static final Logger LOGGER = LogUtils.getLogger();
    public static final ResourceLocation DOME_NODE_ID = new ResourceLocation(DomeSurvival.MOD_ID, "dome_central");

    private DomeMetroService() {
    }

    public static boolean unlock(MinecraftServer server, @Nullable UUID actor) {
        ServerLevel level = server.overworld();
        DomeSpec spec = DomeSavedData.get(level).domeSpec();
        BlockPos domeAnchor = new BlockPos(spec.centerX(), spec.baseY(), spec.centerZ());
        return unlock(server, level, domeAnchor, actor);
    }

    /**
     * Story integration entry point when the Joseph/quest system knows the canonical Dome anchor.
     * The persistent SavedData flag remains the source of truth.
     */
    public static boolean unlock(MinecraftServer server,
                                 ServerLevel level,
                                 BlockPos domeAnchor,
                                 @Nullable UUID actor) {
        DomeMetroSavedData data = DomeMetroSavedData.get(server);
        boolean newlyUnlocked = data.unlock(
                level.dimension().location().toString(),
                domeAnchor,
                actor,
                level.getGameTime()
        );
        boolean built = ensureBuilt(server);
        if (data.isBuilt()) MetroNetworkService.synchronize(server);
        return newlyUnlocked || built || data.isBuilt();
    }

    /** Recovery-safe one-shot construction. Safe to call after every server start. */
    public static boolean ensureBuilt(MinecraftServer server) {
        DomeMetroSavedData data = DomeMetroSavedData.get(server);
        if (!data.needsBuild()) return false;

        ServerLevel level = server.overworld();
        DomeSpec spec = DomeSavedData.get(level).domeSpec();
        BlockPos domeAnchor = new BlockPos(spec.centerX(), spec.baseY(), spec.centerZ());

        try {
            DomeMetroBuilder.BuildResult result = DomeMetroBuilder.build(level, domeAnchor);
            data.markBuilt(
                    DomeMetroSavedData.CURRENT_DOME_METRO_VERSION,
                    result.entrance(),
                    result.stationAnchor(),
                    result.trainConsolePos(),
                    result.forward(),
                    level.getGameTime()
            );
            MetroNetworkService.synchronize(server);
            LOGGER.info("Dome metro built: node={} entrance={} station={} trainConsole={}",
                    DOME_NODE_ID,
                    result.entrance().toShortString(),
                    result.stationAnchor().toShortString(),
                    result.trainConsolePos().toShortString());
            return true;
        }
        catch (RuntimeException exception) {
            LOGGER.error("Failed to build Dome metro", exception);
            String detail = exception.getMessage();
            if (detail == null || detail.isBlank()) detail = exception.getClass().getSimpleName();
            server.getPlayerList().broadcastSystemMessage(
                    Component.literal("[METRO] Build failed: " + detail)
                            .withStyle(ChatFormatting.RED),
                    false
            );
            return false;
        }
    }
}
