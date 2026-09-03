package com.wasted.domesurvival.forge.mixin;

import net.minecraft.client.gui.screens.LevelLoadingScreen;
import net.minecraft.server.level.progress.StoringChunkProgressListener;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

/**
 * Read-only accessor for Minecraft's real spawn-chunk loading progress.
 * No state is changed and no vanilla method is cancelled.
 */
@Mixin(LevelLoadingScreen.class)
public interface LevelLoadingScreenAccessor {
    @Accessor("progressListener")
    StoringChunkProgressListener domesurvival$getProgressListener();
}
