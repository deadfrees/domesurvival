package com.wasted.domesurvival.forge.itempipe;

import com.wasted.domesurvival.forge.DomeSurvival;
import com.wasted.domesurvival.forge.client.itempipe.ItemPipeTravelVisuals;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.network.*;
import net.minecraftforge.network.simple.SimpleChannel;
import java.util.*;
import java.util.function.Supplier;

/** One cosmetic packet per successful transfer. Inventory authority stays on the server. */
@Mod.EventBusSubscriber(modid = DomeSurvival.MOD_ID, bus = Mod.EventBusSubscriber.Bus.MOD)
public final class ItemPipeVisualNetwork {
    private static final int MAX_POINTS = 4098;
    private static final SimpleChannel CHANNEL = NetworkRegistry.newSimpleChannel(
            new ResourceLocation(DomeSurvival.MOD_ID, "item_pipe_visual"), () -> "1", "1"::equals, "1"::equals);

    private ItemPipeVisualNetwork() { }

    @SubscribeEvent public static void setup(FMLCommonSetupEvent event) {
        CHANNEL.registerMessage(0, Travel.class, Travel::encode, Travel::decode, Travel::handle,
                Optional.of(NetworkDirection.PLAY_TO_CLIENT));
    }

    public static void send(ServerLevel level, List<Vec3> points, ItemStack stack) {
        if (points.size() < 2 || points.size() > MAX_POINTS || stack.isEmpty()) return;
        Travel message = new Travel(level.dimension().location(), List.copyOf(points), stack.copy());
        // Include observers along the route, without loading any chunks.
        for (var player : level.players()) {
            if (points.stream().anyMatch(p -> p.distanceToSqr(player.position()) < 64 * 64))
                CHANNEL.send(PacketDistributor.PLAYER.with(() -> player), message);
        }
    }

    public record Travel(ResourceLocation dimension, List<Vec3> points, ItemStack stack) {
        public static void encode(Travel message, FriendlyByteBuf buffer) {
            buffer.writeResourceLocation(message.dimension);
            buffer.writeItem(message.stack);
            buffer.writeVarInt(message.points.size());
            for (Vec3 point : message.points) {
                buffer.writeDouble(point.x); buffer.writeDouble(point.y); buffer.writeDouble(point.z);
            }
        }
        public static Travel decode(FriendlyByteBuf buffer) {
            ResourceLocation dimension = buffer.readResourceLocation();
            ItemStack stack = buffer.readItem();
            int count = buffer.readVarInt();
            if (count < 2 || count > MAX_POINTS) throw new IllegalArgumentException("Invalid pipe visual path size");
            List<Vec3> points = new ArrayList<>(count);
            for (int i = 0; i < count; i++) {
                Vec3 point = new Vec3(buffer.readDouble(), buffer.readDouble(), buffer.readDouble());
                if (!Double.isFinite(point.x) || !Double.isFinite(point.y) || !Double.isFinite(point.z))
                    throw new IllegalArgumentException("Non-finite pipe visual point");
                points.add(point);
            }
            return new Travel(dimension, List.copyOf(points), stack);
        }
        public static void handle(Travel message, Supplier<NetworkEvent.Context> supplier) {
            var context = supplier.get();
            context.enqueueWork(() -> DistExecutor.unsafeRunWhenOn(Dist.CLIENT,
                    () -> () -> ItemPipeTravelVisuals.accept(message)));
            context.setPacketHandled(true);
        }
    }
}
