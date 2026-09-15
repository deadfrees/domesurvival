package com.wasted.domesurvival.forge.itempipe;

import com.wasted.domesurvival.forge.DomeSurvival;
import com.wasted.domesurvival.forge.client.itempipe.ItemPipeTravelVisuals;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
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

/** Server-timed views of persistent in-flight items; never an inventory authority. */
@Mod.EventBusSubscriber(modid = DomeSurvival.MOD_ID, bus = Mod.EventBusSubscriber.Bus.MOD)
public final class ItemPipeVisualNetwork {
    private static final int MAX_POINTS = 4098;
    private static final SimpleChannel CHANNEL = NetworkRegistry.newSimpleChannel(
            new ResourceLocation(DomeSurvival.MOD_ID, "item_pipe_visual"), () -> "2", "2"::equals, "2"::equals);

    private ItemPipeVisualNetwork() { }

    @SubscribeEvent public static void setup(FMLCommonSetupEvent event) {
        CHANNEL.registerMessage(0, Travel.class, Travel::encode, Travel::decode, Travel::handle,
                Optional.of(NetworkDirection.PLAY_TO_CLIENT));
    }

    public static void send(ServerLevel level, Travel message) {
        if (message.points.size() < 2 || message.points.size() > MAX_POINTS || message.stack.isEmpty()) return;
        // Include observers along the route, without loading any chunks.
        for (var player : level.players()) {
            if (message.points.stream().anyMatch(p -> p.distanceToSqr(player.position()) < 64 * 64)) sendTo(player, message);
        }
    }

    public static void sendTo(ServerPlayer player, Travel message) {
        CHANNEL.send(PacketDistributor.PLAYER.with(() -> player), message);
    }

    public static void remove(ServerLevel level, UUID id) {
        Travel removed = new Travel(id, level.dimension().location(), List.of(), ItemStack.EMPTY, 1, 0, level.getGameTime(), false);
        for (ServerPlayer player : level.players()) sendTo(player, removed);
    }

    public record Travel(UUID id, ResourceLocation dimension, List<Vec3> points, ItemStack stack,
                         int duration, int elapsed, long serverTime, boolean moving) {
        public static void encode(Travel message, FriendlyByteBuf buffer) {
            buffer.writeUUID(message.id);
            buffer.writeResourceLocation(message.dimension);
            buffer.writeItem(message.stack);
            buffer.writeVarInt(message.duration); buffer.writeVarInt(message.elapsed);
            buffer.writeLong(message.serverTime); buffer.writeBoolean(message.moving);
            buffer.writeVarInt(message.points.size());
            for (Vec3 point : message.points) {
                buffer.writeDouble(point.x); buffer.writeDouble(point.y); buffer.writeDouble(point.z);
            }
        }
        public static Travel decode(FriendlyByteBuf buffer) {
            UUID id = buffer.readUUID();
            ResourceLocation dimension = buffer.readResourceLocation();
            ItemStack stack = buffer.readItem();
            int duration = buffer.readVarInt(), elapsed = buffer.readVarInt();
            long serverTime = buffer.readLong(); boolean moving = buffer.readBoolean();
            if (duration < 1 || elapsed < 0 || elapsed > duration) throw new IllegalArgumentException("Invalid item transit time");
            int count = buffer.readVarInt();
            if ((count < 2 && !(count == 0 && stack.isEmpty())) || count > MAX_POINTS) throw new IllegalArgumentException("Invalid pipe visual path size");
            List<Vec3> points = new ArrayList<>(count);
            for (int i = 0; i < count; i++) {
                Vec3 point = new Vec3(buffer.readDouble(), buffer.readDouble(), buffer.readDouble());
                if (!Double.isFinite(point.x) || !Double.isFinite(point.y) || !Double.isFinite(point.z))
                    throw new IllegalArgumentException("Non-finite pipe visual point");
                points.add(point);
            }
            return new Travel(id, dimension, List.copyOf(points), stack, duration, elapsed, serverTime, moving);
        }
        public static void handle(Travel message, Supplier<NetworkEvent.Context> supplier) {
            var context = supplier.get();
            context.enqueueWork(() -> DistExecutor.unsafeRunWhenOn(Dist.CLIENT,
                    () -> () -> ItemPipeTravelVisuals.accept(message)));
            context.setPacketHandled(true);
        }
    }
}
