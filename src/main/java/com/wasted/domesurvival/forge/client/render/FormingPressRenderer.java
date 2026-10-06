package com.wasted.domesurvival.forge.client.render;

import com.google.gson.JsonParser;
import com.mojang.blaze3d.vertex.*;
import com.mojang.math.Axis;
import com.wasted.domesurvival.forge.machine.forming.*;
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
public final class FormingPressRenderer implements BlockEntityRenderer<FormingPressBlockEntity> {
    private static final ResourceLocation ATLAS=new ResourceLocation("domesurvival","textures/block/coal_generator_v2/satin_atlas.png");
    private record Cube(float[] lo,float[] hi,float[] uv) { }
    private final List<Cube> cubes=new ArrayList<>();
    public FormingPressRenderer(BlockEntityRendererProvider.Context context) {
        var resource=new ResourceLocation("domesurvival","models/block/forming_press_tool.json");
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
        event.registerBlockEntityRenderer(FormingPressRegistry.FORMING_PRESS_BLOCK_ENTITY.get(),FormingPressRenderer::new);
    }
    @Override public void render(FormingPressBlockEntity press,float partialTick,PoseStack pose,MultiBufferSource buffers,int light,int overlay) {
        renderTool(press.toolOffset(partialTick),press.getMachineFacing(),pose,buffers,light,overlay);
    }
    /** Same exported moving assembly for world rendering and the live JEI recipe preview. */
    public void renderTool(float offset,net.minecraft.core.Direction facing,PoseStack pose,MultiBufferSource buffers,int light,int overlay) {
        pose.pushPose();pose.translate(.5,offset,.5);
        float angle=switch(facing){case EAST->-90;case SOUTH->180;case WEST->90;default->0;};
        pose.mulPose(Axis.YP.rotationDegrees(angle));pose.translate(-.5,0,-.5);
        var consumer=buffers.getBuffer(RenderType.entityCutoutNoCull(ATLAS));
        for(Cube cube:cubes)box(consumer,pose.last(),cube,light,overlay);
        pose.popPose();
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
