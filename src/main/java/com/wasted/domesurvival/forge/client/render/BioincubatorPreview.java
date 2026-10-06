package com.wasted.domesurvival.forge.client.render;

import com.mojang.blaze3d.platform.Lighting;
import com.mojang.math.Axis;
import com.wasted.domesurvival.forge.block.ModBlocks;
import com.wasted.domesurvival.forge.machine.bio.*;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.*;

/** The same exported incubation chamber and double helix in the GUI and JEI. */
public final class BioincubatorPreview {
    private BioincubatorPreview() {}
    public static void draw(GuiGraphics g,int x,int y,int scale,boolean active,int mode,net.minecraft.world.item.ItemStack sample) {
        var mc=Minecraft.getInstance();var state=com.wasted.domesurvival.forge.block.ModBlocks.BIOINCUBATOR.get().defaultBlockState().setValue(BioincubatorBlock.LIT,active);
        g.flush();var pose=g.pose();pose.pushPose();pose.translate(x,y,150);pose.scale(scale,-scale,scale);
        pose.mulPose(Axis.XP.rotationDegrees(20));pose.mulPose(Axis.YP.rotationDegrees(150));pose.translate(-.5,-.5,-.5);
        Lighting.setupFor3DItems();mc.getBlockRenderer().renderSingleBlock(state,pose,g.bufferSource(),15728880,OverlayTexture.NO_OVERLAY);
        var renderer=mc.getBlockEntityRenderDispatcher().getRenderer(new BioincubatorBlockEntity(BlockPos.ZERO,state));
        if(renderer instanceof BioincubatorRenderer dna) dna.renderAssembly(active?(net.minecraft.Util.getMillis()%12000)/12000F:0,Direction.NORTH,0,0,pose,g.bufferSource(),15728880,OverlayTexture.NO_OVERLAY);
        BioincubatorRenderer.renderSample(sample,Direction.NORTH,pose,g.bufferSource(),15728880,OverlayTexture.NO_OVERLAY);
        g.flush();pose.popPose();Lighting.setupFor3DItems();
    }
}
