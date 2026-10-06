package com.wasted.domesurvival.forge.client.render;

import com.mojang.blaze3d.platform.Lighting;
import com.mojang.math.Axis;
import com.wasted.domesurvival.forge.block.ModBlocks;
import com.wasted.domesurvival.forge.machine.energy.AdamantiumEnergyBufferBlock;
import com.wasted.domesurvival.forge.machine.energy.EnergyBufferCapacity;
import com.wasted.domesurvival.forge.machine.energy.AdamantiumEnergyBufferBlockEntity;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.item.ItemProperties;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;

@Mod.EventBusSubscriber(modid="domesurvival",bus=Mod.EventBusSubscriber.Bus.MOD,value=Dist.CLIENT)
public final class AdamantiumBufferPreview {
    private AdamantiumBufferPreview() { }
    @SubscribeEvent public static void setup(FMLClientSetupEvent event){
        event.enqueueWork(()->ItemProperties.register(ModBlocks.ENERGY_BUFFER_ADAMANTIUM.get().asItem(),new ResourceLocation("domesurvival","buffer_charge"),(stack,level,entity,seed)->{
            var nbt=stack.getTagElement("BlockEntityTag");if(nbt==null)return 0;
            int energy=Math.max(0,nbt.getInt("Energy"));
            int capacity=Math.max(energy,EnergyBufferCapacity.apply(AdamantiumEnergyBufferBlockEntity.ENERGY_CAPACITY,EnergyBufferCapacity.getLevel(stack)));
            return Math.min(1F,energy/(float)Math.max(1,capacity));
        }));
    }
    public static void draw(GuiGraphics g,int x,int y,int scale,int energy,int capacity){
        int level=(int)Math.max(0,Math.min(4,(long)energy*4/Math.max(1,capacity)));
        var state=ModBlocks.ENERGY_BUFFER_ADAMANTIUM.get().defaultBlockState().setValue(AdamantiumEnergyBufferBlock.ENERGY_LEVEL,level);
        g.flush();var pose=g.pose();pose.pushPose();pose.translate(x,y,150);pose.scale(scale,-scale,scale);
        pose.mulPose(Axis.XP.rotationDegrees(20));pose.mulPose(Axis.YP.rotationDegrees(150));pose.translate(-.5,-.5,-.5);
        Lighting.setupFor3DItems();Minecraft.getInstance().getBlockRenderer().renderSingleBlock(state,pose,g.bufferSource(),15728880,OverlayTexture.NO_OVERLAY);
        g.flush();pose.popPose();Lighting.setupFor3DItems();
    }
}
