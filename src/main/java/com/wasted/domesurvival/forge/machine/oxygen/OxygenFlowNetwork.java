package com.wasted.domesurvival.forge.machine.oxygen;

import com.wasted.domesurvival.forge.DomeSurvival;
import com.wasted.domesurvival.forge.client.oxygen.OxygenGasFlowRenderer;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.network.*;
import net.minecraftforge.network.simple.SimpleChannel;
import java.util.*;
import java.util.function.Supplier;

/** Rate-limited cosmetic updates; called only after oxygen was accepted by its sink. */
@Mod.EventBusSubscriber(modid=DomeSurvival.MOD_ID,bus=Mod.EventBusSubscriber.Bus.MOD)
public final class OxygenFlowNetwork {
    private static final int MAX_POINTS=2050;
    private static final SimpleChannel CHANNEL=NetworkRegistry.newSimpleChannel(new ResourceLocation(DomeSurvival.MOD_ID,"oxygen_flow"),()->"1","1"::equals,"1"::equals);
    private static final Map<ServerLevel,Map<Key,Long>> SENT=new WeakHashMap<>();
    private record Key(BlockPos source,BlockPos sink){}
    private OxygenFlowNetwork(){}
    @SubscribeEvent public static void setup(FMLCommonSetupEvent event){CHANNEL.registerMessage(0,Flow.class,Flow::encode,Flow::decode,Flow::handle,Optional.of(NetworkDirection.PLAY_TO_CLIENT));}

    public static void send(ServerLevel level,BlockPos source,BlockPos sink,BlockPos firstPipe,Map<BlockPos,BlockPos> towardsSink){
        if(firstPipe==null)return;
        long now=level.getGameTime();var sent=SENT.computeIfAbsent(level,l->new HashMap<>());Key key=new Key(source,sink);
        if(now-sent.getOrDefault(key,Long.MIN_VALUE/2)<4)return;
        sent.entrySet().removeIf(e->now-e.getValue()>40);
        if(sent.size()>=1024&&!sent.containsKey(key))return;
        List<BlockPos> path=new ArrayList<>();path.add(source);Set<BlockPos> visited=new HashSet<>();BlockPos p=firstPipe;
        while(p!=null&&!p.equals(sink)&&path.size()<MAX_POINTS-1){if(!visited.add(p))return;path.add(p);p=towardsSink.get(p);}
        if(!sink.equals(p))return;
        path.add(sink);sent.put(key,now);Flow message=new Flow(level.dimension().location(),List.copyOf(path));
        for(var player:level.players())if(path.stream().anyMatch(pos->Vec3.atCenterOf(pos).distanceToSqr(player.position())<48*48))CHANNEL.send(PacketDistributor.PLAYER.with(()->player),message);
    }
    public record Flow(ResourceLocation dimension,List<BlockPos> path){
        public static void encode(Flow message,FriendlyByteBuf buffer){buffer.writeResourceLocation(message.dimension);buffer.writeVarInt(message.path.size());for(var p:message.path)buffer.writeBlockPos(p);}
        public static Flow decode(FriendlyByteBuf buffer){var dimension=buffer.readResourceLocation();int n=buffer.readVarInt();if(n<3||n>MAX_POINTS)throw new IllegalArgumentException("Invalid oxygen path size");List<BlockPos> path=new ArrayList<>(n);for(int i=0;i<n;i++)path.add(buffer.readBlockPos());return new Flow(dimension,List.copyOf(path));}
        public static void handle(Flow message,Supplier<NetworkEvent.Context> supplier){var context=supplier.get();context.enqueueWork(()->DistExecutor.unsafeRunWhenOn(Dist.CLIENT,()->()->OxygenGasFlowRenderer.accept(message)));context.setPacketHandled(true);}
    }
}
