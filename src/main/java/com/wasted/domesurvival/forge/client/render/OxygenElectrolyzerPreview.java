package com.wasted.domesurvival.forge.client.render;

import com.mojang.blaze3d.platform.Lighting;
import com.mojang.math.Axis;
import com.wasted.domesurvival.forge.block.ModBlocks;
import com.wasted.domesurvival.forge.machine.oxygen.*;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.*;

/** The same exported model and electrode bath in the machine GUI and JEI. */
public final class OxygenElectrolyzerPreview {
    private OxygenElectrolyzerPreview() {}
    public static void draw(GuiGraphics g,int x,int y,int scale,boolean active,int water,int oxygen) {
        var mc=Minecraft.getInstance();var state=ModBlocks.OXYGEN_ELECTROLYZER.get().defaultBlockState().setValue(OxygenElectrolyzerBlock.LIT,active);
        g.flush();var pose=g.pose();pose.pushPose();pose.translate(x,y,150);pose.scale(scale,-scale,scale);
        pose.mulPose(Axis.XP.rotationDegrees(20));pose.mulPose(Axis.YP.rotationDegrees(150));pose.translate(-.5,-.5,-.5);
        Lighting.setupFor3DItems();mc.getBlockRenderer().renderSingleBlock(state,pose,g.bufferSource(),15728880,OverlayTexture.NO_OVERLAY);
        var renderer=mc.getBlockEntityRenderDispatcher().getRenderer(new OxygenElectrolyzerBlockEntity(BlockPos.ZERO,state));
        if(renderer instanceof OxygenElectrolyzerRenderer pump) pump.renderAssembly(active?(net.minecraft.Util.getMillis()%4000)/4000F:0,Direction.NORTH,water,oxygen,pose,g.bufferSource(),15728880,OverlayTexture.NO_OVERLAY);
        g.flush();pose.popPose();Lighting.setupFor3DItems();
    }
}
