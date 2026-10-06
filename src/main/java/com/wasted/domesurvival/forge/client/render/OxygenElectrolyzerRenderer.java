package com.wasted.domesurvival.forge.client.render;

import com.google.gson.JsonParser;
import com.mojang.blaze3d.vertex.*;
import com.mojang.math.Axis;
import com.wasted.domesurvival.forge.machine.oxygen.*;
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
public final class OxygenElectrolyzerRenderer implements BlockEntityRenderer<OxygenElectrolyzerBlockEntity> {
    private static final ResourceLocation ATLAS=new ResourceLocation("domesurvival","textures/block/coal_generator_v2/satin_atlas.png");
    private record Cube(float[] lo,float[] hi,float[] uv) { }
    private final List<Cube> cubes=new ArrayList<>();
    public OxygenElectrolyzerRenderer(BlockEntityRendererProvider.Context context) {
        var resource=new ResourceLocation("domesurvival","models/block/oxygen_electrolyzer_bubbles.json");
        try(var reader=Minecraft.getInstance().getResourceManager().openAsReader(resource)) {
            for(var element:JsonParser.parseReader(reader).getAsJsonObject().getAsJsonArray("elements")) {
                var e=element.getAsJsonObject();
                cubes.add(new Cube(array(e.getAsJsonArray("from")),array(e.getAsJsonArray("to")),array(e.getAsJsonObject("faces").getAsJsonObject("north").getAsJsonArray("uv"))));
            }
        } catch(java.io.IOException ex) { throw new IllegalStateException("Missing electrolyzer bubble geometry",ex); }
    }
    private static float[] array(com.google.gson.JsonArray values) {
        float[] result=new float[values.size()];for(int i=0;i<result.length;i++)result[i]=values.get(i).getAsFloat()/16;return result;
    }
    @SubscribeEvent public static void register(EntityRenderersEvent.RegisterRenderers event) {
        event.registerBlockEntityRenderer(com.wasted.domesurvival.forge.registry.ModBlockEntities.OXYGEN_ELECTROLYZER.get(),OxygenElectrolyzerRenderer::new);
    }
    @Override public void render(OxygenElectrolyzerBlockEntity press,float partialTick,PoseStack pose,MultiBufferSource buffers,int light,int overlay) {
        renderAssembly(press.animationPhase(partialTick),press.getMachineFacing(),press.waterAmount(),press.oxygenAmount(),pose,buffers,light,overlay);
    }
    /** Same exported moving assembly for world rendering and the live JEI recipe preview. */
    public void renderAssembly(float rotation,net.minecraft.core.Direction facing,int raw,int purified,PoseStack pose,MultiBufferSource buffers,int light,int overlay) {
        pose.pushPose();pose.translate(.5,0,.5);
        float angle=switch(facing){case EAST->-90;case SOUTH->180;case WEST->90;default->0;};
        pose.mulPose(Axis.YP.rotationDegrees(angle));pose.translate(-.5,0,-.5);
        var consumer=buffers.getBuffer(RenderType.entityCutoutNoCull(ATLAS));
        // Gas bubbles have real depth and freeze with the block's working state.
        if(raw>0) for(int i=0;i<cubes.size();i++) {
            Cube cube=cubes.get(i);float phase=(rotation+i*.167F)%1;
            pose.pushPose();pose.translate(0,(3.9F+phase*6.7F)/16-cube.lo[1],0);
            box(consumer,pose.last(),cube,light,overlay);pose.popPose();
        }
        alpha=78;
        liquid(buffers.getBuffer(RenderType.entityTranslucent(ATLAS)),pose.last(),3.6F,raw,light,overlay);
        alpha=255;pose.popPose();
    }
    private int alpha=255;

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
        if(waterUv!=null)box(consumer,pose,new Cube(new float[]{x/16,3.5F/16,3.20F/16},new float[]{(x+8.8F)/16,(3.5F+7.8F*Math.min(4000,amount)/4000)/16,7.4F/16},waterUv),light,overlay);
    }
    private void box(VertexConsumer c,PoseStack.Pose p,Cube cube,int light,int overlay) {
        float x0=cube.lo[0],y0=cube.lo[1],z0=cube.lo[2],x1=cube.hi[0],y1=cube.hi[1],z1=cube.hi[2];
        quad(c,p,cube.uv,new float[]{x1,y0,z0,x0,y0,z0,x0,y1,z0,x1,y1,z0},0,0,-1,light,overlay);
        quad(c,p,cube.uv,new float[]{x0,y0,z1,x1,y0,z1,x1,y1,z1,x0,y1,z1},0,0,1,light,overlay);
        quad(c,p,cube.uv,new float[]{x0,y0,z0,x0,y0,z1,x0,y1,z1,x0,y1,z0},-1,0,0,light,overlay);
        quad(c,p,cube.uv,new float[]{x1,y0,z1,x1,y0,z0,x1,y1,z0,x1,y1,z1},1,0,0,light,overlay);
        quad(c,p,cube.uv,new float[]{x0,y0,z1,x1,y0,z1,x1,y0,z0,x0,y0,z0},0,-1,0,light,overlay);
        quad(c,p,cube.uv,new float[]{x0,y1,z0,x1,y1,z0,x1,y1,z1,x0,y1,z1},0,1,0,light,overlay);
    }
    private void quad(VertexConsumer c,PoseStack.Pose p,float[] uv,float[] xyz,float nx,float ny,float nz,int light,int overlay) {
        for(int i=0;i<4;i++)c.vertex(p.pose(),xyz[i*3],xyz[i*3+1],xyz[i*3+2]).color(255,255,255,alpha)
                .uv(i==0||i==3?uv[2]:uv[0],i<2?uv[3]:uv[1]).overlayCoords(overlay).uv2(light)
                .normal(p.normal(),nx,ny,nz).endVertex();
    }
}
