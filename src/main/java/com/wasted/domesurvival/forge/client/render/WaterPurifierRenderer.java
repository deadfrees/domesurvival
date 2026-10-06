package com.wasted.domesurvival.forge.client.render;

import com.google.gson.JsonParser;
import com.mojang.blaze3d.vertex.*;
import com.mojang.math.Axis;
import com.wasted.domesurvival.forge.machine.water.*;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.*;
import net.minecraft.client.renderer.blockentity.*;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.EntityRenderersEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import java.util.*;

/** Renders the exact Blender-exported tool with real depth and server-active motion. */
@Mod.EventBusSubscriber(modid="domesurvival",bus=Mod.EventBusSubscriber.Bus.MOD,value=Dist.CLIENT)
public final class WaterPurifierRenderer implements BlockEntityRenderer<WaterPurifierBlockEntity> {
    private static final ResourceLocation ATLAS=new ResourceLocation("domesurvival","textures/block/coal_generator_v2/satin_atlas.png");
    private record Cube(float[] lo,float[] hi,float[] uv) { }
    private final List<Cube> cubes=new ArrayList<>();
    public WaterPurifierRenderer(BlockEntityRendererProvider.Context context) {
        var resource=new ResourceLocation("domesurvival","models/block/water_purifier_pump.json");
        try(var reader=Minecraft.getInstance().getResourceManager().openAsReader(resource)) {
            for(var element:JsonParser.parseReader(reader).getAsJsonObject().getAsJsonArray("elements")) {
                var e=element.getAsJsonObject();
                cubes.add(new Cube(array(e.getAsJsonArray("from")),array(e.getAsJsonArray("to")),array(e.getAsJsonObject("faces").getAsJsonObject("north").getAsJsonArray("uv"))));
            }
        } catch(java.io.IOException ex) { throw new IllegalStateException("Missing forming press tool geometry",ex); }
    }
    private static float[] array(com.google.gson.JsonArray values) {
        float[] result=new float[values.size()];for(int i=0;i<result.length;i++)result[i]=values.get(i).getAsFloat()/16;return result;
    }
    @SubscribeEvent public static void register(EntityRenderersEvent.RegisterRenderers event) {
        event.registerBlockEntityRenderer(com.wasted.domesurvival.forge.registry.ModBlockEntities.WATER_PURIFIER.get(),WaterPurifierRenderer::new);
    }
    @Override public void render(WaterPurifierBlockEntity press,float partialTick,PoseStack pose,MultiBufferSource buffers,int light,int overlay) {
        renderAssembly(press.pumpAngle(partialTick),press.getMachineFacing(),press.rawAmount(),press.purifiedAmount(),pose,buffers,light,overlay);
    }
    /** Same exported moving assembly for world rendering and the live JEI recipe preview. */
    public void renderAssembly(float rotation,net.minecraft.core.Direction facing,int raw,int purified,PoseStack pose,MultiBufferSource buffers,int light,int overlay) {
        pose.pushPose();pose.translate(.5,0,.5);
        float angle=switch(facing){case EAST->-90;case SOUTH->180;case WEST->90;default->0;};
        pose.mulPose(Axis.YP.rotationDegrees(angle));pose.translate(-.5,0,-.5);
        var consumer=buffers.getBuffer(RenderType.entityCutoutNoCull(ATLAS));
        // Tank fill is actual stored water, not a permanently full decoration.
        liquid(consumer,pose.last(),3.45F,raw,light,overlay);liquid(consumer,pose.last(),10.45F,purified,light,overlay);
        pose.pushPose();pose.translate(.5,5.25/16,3.8/16);pose.mulPose(Axis.ZP.rotationDegrees(rotation));pose.translate(-.5,-5.25/16,-3.8/16);
        for(Cube cube:cubes)box(consumer,pose.last(),cube,light,overlay);pose.popPose();
        pose.popPose();
    }
    private float[] waterUv;
    private void liquid(VertexConsumer consumer,PoseStack.Pose pose,float x,int amount,int light,int overlay){
        if(amount<=0)return;
        if(waterUv==null){
            try(var reader=Minecraft.getInstance().getResourceManager().openAsReader(new ResourceLocation("domesurvival","models/block/coal_generator_input_port_north.json"))){
                for(var element:JsonParser.parseReader(reader).getAsJsonObject().getAsJsonArray("elements"))for(var entry:element.getAsJsonObject().getAsJsonObject("faces").entrySet()){
                    var face=entry.getValue().getAsJsonObject();if(face.get("texture").getAsString().equals("#input"))waterUv=array(face.getAsJsonArray("uv"));
                }
            }catch(java.io.IOException ex){throw new IllegalStateException(ex);}
        }
        if(waterUv!=null)box(consumer,pose,new Cube(new float[]{x/16,4.05F/16,2.85F/16},new float[]{(x+2.05F)/16,(4.05F+7.3F*Math.min(4000,amount)/4000)/16,5.4F/16},waterUv),light,overlay);
    }
    private static void box(VertexConsumer c,PoseStack.Pose p,Cube cube,int light,int overlay) {
        float x0=cube.lo[0],y0=cube.lo[1],z0=cube.lo[2],x1=cube.hi[0],y1=cube.hi[1],z1=cube.hi[2];
        quad(c,p,cube.uv,new float[]{x1,y0,z0,x0,y0,z0,x0,y1,z0,x1,y1,z0},0,0,-1,light,overlay);
        quad(c,p,cube.uv,new float[]{x0,y0,z1,x1,y0,z1,x1,y1,z1,x0,y1,z1},0,0,1,light,overlay);
        quad(c,p,cube.uv,new float[]{x0,y0,z0,x0,y0,z1,x0,y1,z1,x0,y1,z0},-1,0,0,light,overlay);
        quad(c,p,cube.uv,new float[]{x1,y0,z1,x1,y0,z0,x1,y1,z0,x1,y1,z1},1,0,0,light,overlay);
        quad(c,p,cube.uv,new float[]{x0,y0,z1,x1,y0,z1,x1,y0,z0,x0,y0,z0},0,-1,0,light,overlay);
        quad(c,p,cube.uv,new float[]{x0,y1,z0,x1,y1,z0,x1,y1,z1,x0,y1,z1},0,1,0,light,overlay);
    }
    private static void quad(VertexConsumer c,PoseStack.Pose p,float[] uv,float[] xyz,float nx,float ny,float nz,int light,int overlay) {
        for(int i=0;i<4;i++)c.vertex(p.pose(),xyz[i*3],xyz[i*3+1],xyz[i*3+2]).color(255,255,255,255)
                .uv(i==0||i==3?uv[2]:uv[0],i<2?uv[3]:uv[1]).overlayCoords(overlay).uv2(light)
                .normal(p.normal(),nx,ny,nz).endVertex();
    }
}
