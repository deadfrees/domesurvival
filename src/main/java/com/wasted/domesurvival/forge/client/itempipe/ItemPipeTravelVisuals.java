package com.wasted.domesurvival.forge.client.itempipe;

import com.mojang.blaze3d.vertex.PoseStack;
import com.wasted.domesurvival.forge.DomeSurvival;
import com.wasted.domesurvival.forge.itempipe.ItemPipeVisualNetwork;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.core.BlockPos;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import java.util.*;

/** Bounded client-only animation; these are never world item entities or inventory contents. */
@Mod.EventBusSubscriber(modid = DomeSurvival.MOD_ID, value = Dist.CLIENT)
public final class ItemPipeTravelVisuals {
    private static final int MAX_VISUALS = 64;
    private static final int TICKS_PER_SEGMENT = 8;
    private static final ArrayDeque<Visual> VISUALS = new ArrayDeque<>();
    private static final Map<BlockPos, List<Sample>> SAMPLES = new HashMap<>();
    private static long sampledTick = Long.MIN_VALUE;
    private static float sampledPartialTick;
    private static ClientLevel owner;
    private record Visual(List<Vec3> points, ItemStack stack, long startTick) { }
    private record Sample(Vec3 point, ItemStack stack) { }
    private ItemPipeTravelVisuals() { }

    public static void accept(ItemPipeVisualNetwork.Travel message) {
        ClientLevel level = Minecraft.getInstance().level;
        if (level != owner) { VISUALS.clear(); SAMPLES.clear(); owner = level; }
        if (level == null || !level.dimension().location().equals(message.dimension()) || message.stack().isEmpty()) return;
        while (VISUALS.size() >= MAX_VISUALS) VISUALS.removeFirst();
        VISUALS.addLast(new Visual(message.points(), message.stack().copy(), level.getGameTime()));
        sampledTick = Long.MIN_VALUE;
    }

    @SubscribeEvent public static void tick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        ClientLevel level = Minecraft.getInstance().level;
        if (level != owner) { VISUALS.clear(); SAMPLES.clear(); owner = level; sampledTick = Long.MIN_VALUE; }
        if (level != null) VISUALS.removeIf(v -> level.getGameTime() - v.startTick >= (v.points.size() - 1L) * TICKS_PER_SEGMENT);
    }

    public static void render(BlockPos pipe, float partialTick, PoseStack pose,
                              MultiBufferSource buffers, int light, int overlay) {
        var mc = Minecraft.getInstance();
        if (mc.level == null || mc.level != owner || VISUALS.isEmpty()) return;
        // Interpolate once per frame, then each visible pipe only looks up its own items.
        // Empty pipes do not scan every animation or allocate temporary positions.
        if (sampledTick != mc.level.getGameTime() || sampledPartialTick != partialTick) sample(mc.level.getGameTime(), partialTick);
        var samples = SAMPLES.get(pipe);
        if (samples == null) return;
        for (Sample sample : samples) {
            Vec3 point = sample.point;
            pose.pushPose();
            pose.translate(point.x - pipe.getX(), point.y - pipe.getY(), point.z - pipe.getZ());
            pose.mulPose(mc.getEntityRenderDispatcher().cameraOrientation());
            pose.scale(0.22F, 0.22F, 0.22F);
            mc.getItemRenderer().renderStatic(sample.stack, ItemDisplayContext.FIXED, light, overlay,
                    pose, buffers, mc.level, 0);
            pose.popPose();
        }
    }

    private static void sample(long gameTime, float partialTick) {
        SAMPLES.clear(); sampledTick = gameTime; sampledPartialTick = partialTick;
        for (Visual visual : VISUALS) {
            double progress = (gameTime - visual.startTick + partialTick) / TICKS_PER_SEGMENT;
            if (progress < 0 || progress >= visual.points.size() - 1) continue;
            int segment = (int) progress;
            Vec3 point = visual.points.get(segment).lerp(visual.points.get(segment + 1), progress - segment);
            SAMPLES.computeIfAbsent(BlockPos.containing(point), ignored -> new ArrayList<>()).add(new Sample(point, visual.stack));
        }
    }
}
