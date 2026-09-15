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
    private static final int MAX_VISUALS = 256;
    private static final Map<UUID, Visual> VISUALS = new LinkedHashMap<>();
    private static final Map<BlockPos, List<Sample>> SAMPLES = new HashMap<>();
    private static long sampledTick = Long.MIN_VALUE;
    private static float sampledPartialTick;
    private static ClientLevel owner;
    private record Visual(ItemPipeVisualNetwork.Travel state, double[] distances) { }
    private record Sample(Vec3 point, ItemStack stack) { }
    private ItemPipeTravelVisuals() { }

    public static void accept(ItemPipeVisualNetwork.Travel message) {
        ClientLevel level = Minecraft.getInstance().level;
        if (level != owner) { VISUALS.clear(); SAMPLES.clear(); owner = level; }
        if (level == null || !level.dimension().location().equals(message.dimension())) return;
        if (message.stack().isEmpty()) {
            VISUALS.remove(message.id()); sampledTick = Long.MIN_VALUE; return;
        }
        if (!VISUALS.containsKey(message.id()) && VISUALS.size() >= MAX_VISUALS) VISUALS.remove(VISUALS.keySet().iterator().next());
        double[] distances = new double[message.points().size()];
        for (int i=1;i<distances.length;i++) distances[i]=distances[i-1]+message.points().get(i-1).distanceTo(message.points().get(i));
        VISUALS.put(message.id(), new Visual(message, distances));
        sampledTick = Long.MIN_VALUE;
    }

    @SubscribeEvent public static void tick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        ClientLevel level = Minecraft.getInstance().level;
        if (level != owner) { VISUALS.clear(); SAMPLES.clear(); owner = level; sampledTick = Long.MIN_VALUE; }
        // The server removes delivered/refunded stacks. A full receiver keeps the
        // same packet stationary at the pipe outlet instead of disappearing on a timer.
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
        for (Visual visual : VISUALS.values()) {
            var state = visual.state;
            double elapsed = state.elapsed() + (state.moving() ? Math.max(0, gameTime-state.serverTime()+partialTick) : 0);
            double progress = Math.max(0, Math.min(1, elapsed / state.duration()));
            double distance = progress * visual.distances[visual.distances.length-1];
            int index = Arrays.binarySearch(visual.distances, distance);
            int segment = Math.max(0, Math.min(visual.distances.length-2, index >= 0 ? index : -index-2));
            double length = visual.distances[segment+1]-visual.distances[segment];
            double fraction = length <= 0 ? 0 : (distance-visual.distances[segment])/length;
            Vec3 point = state.points().get(segment).lerp(state.points().get(segment+1), fraction);
            SAMPLES.computeIfAbsent(BlockPos.containing(point), ignored -> new ArrayList<>()).add(new Sample(point, state.stack()));
        }
    }
}
