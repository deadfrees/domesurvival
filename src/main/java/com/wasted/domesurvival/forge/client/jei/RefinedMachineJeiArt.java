package com.wasted.domesurvival.forge.client.jei;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;

/** Baked Blender artwork shared with the approved machine interfaces. */
final class RefinedMachineJeiArt {
    private static final ResourceLocation WIDGETS=new ResourceLocation("domesurvival","textures/gui/coal_generator_v2/widgets.png");
    static void panel(GuiGraphics g,String kind,Component title){
        g.blit(new ResourceLocation("domesurvival","textures/gui/copper_furnace_v2/jei_"+kind+".png"),0,0,180,128,0,0,720,512,720,512);
        text(g,title,12,9,156,0xFF251C13);
    }
    static void text(GuiGraphics g,Component c,int x,int y,int width,int color){
        var font=Minecraft.getInstance().font;String s=c.getString();
        if(font.width(s)>width)s=font.plainSubstrByWidth(s,width-font.width("..."))+"...";
        g.drawString(font,s,x,y,color,false);
    }
    static void progress(GuiGraphics g,int x,int y,int w,int ticks){
        int filled=Math.max(1,(int)(w*DomeJeiStyle.animationFraction(ticks)));
        g.blit(WIDGETS,x,y,filled,8,0,192,Math.max(1,256*filled/w),32,512,256);
    }
    static Component t(String key,Object...args){return Component.translatable("gui.domesurvival.copper_v2."+key,args);}
    static void machine(GuiGraphics g,net.minecraft.world.level.block.state.BlockState state,boolean press){
        var mc=Minecraft.getInstance();g.flush();
        // The projected cube stays above y=79; the progress well starts at 83.
        var pose=g.pose();pose.pushPose();pose.translate(90,52,150);pose.scale(30,-30,30);
        pose.mulPose(com.mojang.math.Axis.XP.rotationDegrees(25));
        pose.mulPose(com.mojang.math.Axis.YP.rotationDegrees(135));pose.translate(-.5,-.5,-.5);
        com.mojang.blaze3d.platform.Lighting.setupFor3DItems();
        mc.getBlockRenderer().renderSingleBlock(state,pose,g.bufferSource(),15728880,net.minecraft.client.renderer.texture.OverlayTexture.NO_OVERLAY);
        if(press){
            var blockEntity=new com.wasted.domesurvival.forge.machine.forming.FormingPressBlockEntity(net.minecraft.core.BlockPos.ZERO,state);
            var renderer=mc.getBlockEntityRenderDispatcher().getRenderer(blockEntity);
            float ticks=(net.minecraft.Util.getMillis()%2000)/50F;
            float offset=(float)(-(1-Math.cos(ticks*Math.PI/20))*1.5/16);
            if(renderer instanceof com.wasted.domesurvival.forge.client.render.FormingPressRenderer tool)
                tool.renderTool(offset,net.minecraft.core.Direction.NORTH,pose,g.bufferSource(),15728880,net.minecraft.client.renderer.texture.OverlayTexture.NO_OVERLAY);
        }
        g.flush();pose.popPose();com.mojang.blaze3d.platform.Lighting.setupFor3DItems();
    }
}
