package com.wasted.domesurvival.forge.client.oxygen;

import com.mojang.blaze3d.vertex.*;
import com.wasted.domesurvival.forge.DomeSurvival;
import com.wasted.domesurvival.forge.machine.oxygen.*;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.*;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RenderLevelStageEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import java.util.*;

/** Soft grey gas inside active glass pipes. No particles can escape the bore. */
@Mod.EventBusSubscriber(modid=DomeSurvival.MOD_ID,value=Dist.CLIENT)
public final class OxygenGasFlowRenderer {
    private static final int MAX_FLOWS=64,MAX_SEGMENTS=512;
    private static final Map<Key,Active> ACTIVE=new LinkedHashMap<>();
    private static final MultiBufferSource.BufferSource BUFFERS=MultiBufferSource.immediate(new BufferBuilder(32768));
    private static ClientLevel owner;
    private record Key(BlockPos source,BlockPos sink){}
    private record Active(List<BlockPos> path,long expires){}
    private OxygenGasFlowRenderer(){}
    public static void accept(OxygenFlowNetwork.Flow message){
        var level=Minecraft.getInstance().level;if(level!=owner){ACTIVE.clear();owner=level;}
        if(level==null||!level.dimension().location().equals(message.dimension()))return;
        var path=message.path();Key key=new Key(path.get(0),path.get(path.size()-1));
        if(ACTIVE.size()>=MAX_FLOWS&&!ACTIVE.containsKey(key))ACTIVE.remove(ACTIVE.keySet().iterator().next());
        ACTIVE.put(key,new Active(path,level.getGameTime()+12));
    }
    @SubscribeEvent public static void tick(TickEvent.ClientTickEvent event){
        if(event.phase!=TickEvent.Phase.END)return;var level=Minecraft.getInstance().level;
        if(level!=owner){ACTIVE.clear();owner=level;}
        if(level!=null)ACTIVE.values().removeIf(flow->flow.expires<=level.getGameTime());
    }
    @SubscribeEvent public static void render(RenderLevelStageEvent event){
        if(event.getStage()!=RenderLevelStageEvent.Stage.AFTER_BLOCK_ENTITIES)return;
        var mc=Minecraft.getInstance();var level=mc.level;if(level==null||level!=owner||ACTIVE.isEmpty())return;
        var pose=event.getPoseStack();Vec3 camera=event.getCamera().getPosition();pose.pushPose();pose.translate(-camera.x,-camera.y,-camera.z);
        var out=BUFFERS.getBuffer(GasType.TYPE);double now=level.getGameTime()+event.getPartialTick();int drawn=0;
        outer:for(var flow:ACTIVE.values()){
            float fade=(float)Math.max(0,Math.min(1,(flow.expires-now)/4));if(fade<=0)continue;double distance=0;
            for(int i=0;i<flow.path.size()-1;i++){
                BlockPos pa=flow.path.get(i),pb=flow.path.get(i+1);Vec3 a=Vec3.atCenterOf(pa),b=Vec3.atCenterOf(pb);double length=a.distanceTo(b);
                double startDistance=distance;distance+=length;
                if(a.lerp(b,.5).distanceToSqr(camera)>32*32||!level.hasChunkAt(pa)||!level.hasChunkAt(pb))continue;
                boolean pipeA=level.getBlockState(pa).getBlock() instanceof OxygenPipeBlock,pipeB=level.getBlockState(pb).getBlock() instanceof OxygenPipeBlock;
                if((!pipeA&&i!=0)||(!pipeB&&i!=flow.path.size()-2)||(!pipeA&&!pipeB))continue;
                if(!pipeA){a=a.lerp(b,.5);startDistance+=length*.5;}
                if(!pipeB)b=b.lerp(a,.5);
                Vec3 delta=b.subtract(a);if(delta.lengthSqr()<.00001)continue;
                // Two crossed soft ribbons, 1.44 model units wide, inside the 2-unit glass bore.
                Vec3 s1=Math.abs(delta.y)>.001?new Vec3(.045,0,0):new Vec3(0,.045,0);
                Vec3 s2=Math.abs(delta.z)>.001?new Vec3(.045,0,0):new Vec3(0,0,.045);
                int light=LevelRenderer.getLightColor(level,pipeA?pa:pb);
                float v0=(float)(startDistance-now*.05),v1=v0+(float)a.distanceTo(b);
                ribbon(out,pose.last(),a,b,s1,v0,v1,fade*.52F,light);ribbon(out,pose.last(),a,b,s2,v0,v1,fade*.52F,light);
                if(++drawn>=MAX_SEGMENTS)break outer;
            }
        }
        BUFFERS.endBatch(GasType.TYPE);pose.popPose();
    }
    private static void ribbon(VertexConsumer out,PoseStack.Pose pose,Vec3 a,Vec3 b,Vec3 side,float v0,float v1,float alpha,int light){
        Vec3 normal=b.subtract(a).cross(side).normalize();
        vertex(out,pose,a.subtract(side),0,v0,alpha,light,normal);vertex(out,pose,a.add(side),1,v0,alpha,light,normal);
        vertex(out,pose,b.add(side),1,v1,alpha,light,normal);vertex(out,pose,b.subtract(side),0,v1,alpha,light,normal);
    }
    private static void vertex(VertexConsumer out,PoseStack.Pose pose,Vec3 p,float u,float v,float alpha,int light,Vec3 normal){
        out.vertex(pose.pose(),(float)p.x,(float)p.y,(float)p.z).color(.50F,.52F,.54F,alpha).uv(u,v).overlayCoords(OverlayTexture.NO_OVERLAY).uv2(light).normal(pose.normal(),(float)normal.x,(float)normal.y,(float)normal.z).endVertex();
    }
    private static final class GasType extends RenderType {
        private static final RenderType TYPE=create("domesurvival_oxygen_gas",DefaultVertexFormat.NEW_ENTITY,VertexFormat.Mode.QUADS,32768,false,true,
                CompositeState.builder().setShaderState(RENDERTYPE_ENTITY_TRANSLUCENT_SHADER)
                        .setTextureState(new TextureStateShard(new ResourceLocation(DomeSurvival.MOD_ID,"textures/block/oxygen_glass/oxygen_gas_flow.png"),true,false))
                        .setTransparencyState(TRANSLUCENT_TRANSPARENCY).setCullState(NO_CULL).setWriteMaskState(COLOR_WRITE)
                        .setLightmapState(LIGHTMAP).setOverlayState(OVERLAY).createCompositeState(false));
        private GasType(){super("oxygen_gas",DefaultVertexFormat.NEW_ENTITY,VertexFormat.Mode.QUADS,32768,false,true,()->{},()->{});}
    }
}
