package com.wasted.domesurvival.forge.client.render;

import com.wasted.domesurvival.forge.DomeSurvival;
import com.wasted.domesurvival.forge.client.model.OxygenEquipmentModelCache;
import com.wasted.domesurvival.forge.item.ModItems;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import top.theillusivec4.curios.api.SlotContext;
import top.theillusivec4.curios.api.client.ICurioRenderer;

/** Client-only Curios renderer for the oxygen tank already used by DomeSurvival. */
public final class OxygenTankCurioRenderer implements ICurioRenderer.HumanoidRender {
    private static final ResourceLocation TEXTURE =
            new ResourceLocation(DomeSurvival.MOD_ID, "textures/models/armor/oxygen_tank.png");

    @Override
    public HumanoidModel<LivingEntity> getModel(ItemStack stack, SlotContext slotContext) {
        if (stack.is(ModItems.LARGE_OXYGEN_TANK.get())) {
            return OxygenEquipmentModelCache.largeTankModel();
        }
        if (stack.is(ModItems.MEDIUM_OXYGEN_TANK.get())) {
            return OxygenEquipmentModelCache.mediumTankModel();
        }
        return OxygenEquipmentModelCache.smallTankModel();
    }

    @Override
    public ResourceLocation getModelTexture(ItemStack stack, SlotContext slotContext) {
        return TEXTURE;
    }
}
