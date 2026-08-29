package com.wasted.domesurvival.forge.enchantment;

import com.wasted.domesurvival.forge.DomeSurvival;
import com.wasted.domesurvival.forge.machine.energy.EnergyBufferBlockItem;
import com.wasted.domesurvival.forge.machine.energy.EnergyBufferCapacity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraftforge.event.entity.EntityJoinLevelEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * Normal block loot can copy BlockEntity NBT but cannot conveniently rebuild a
 * variable-level enchantment list. Convert that tiny persisted marker into the
 * real enchantment once the dropped item entity is created.
 */
@Mod.EventBusSubscriber(modid = DomeSurvival.MOD_ID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class EnergyBufferCapacityEvents {
    private EnergyBufferCapacityEvents() {
    }

    @SubscribeEvent
    public static void onItemEntityJoin(EntityJoinLevelEvent event) {
        if (event.getLevel().isClientSide()) return;
        if (!(event.getEntity() instanceof ItemEntity itemEntity)) return;

        ItemStack stack = itemEntity.getItem();
        if (!(stack.getItem() instanceof EnergyBufferBlockItem)) return;

        int level = EnergyBufferCapacity.getLevel(stack);
        if (level <= 0) return;

        int actualEnchantLevel = EnchantmentHelper.getItemEnchantmentLevel(ModEnchantments.CAPACITY.get(), stack);
        if (actualEnchantLevel >= level) return;

        EnergyBufferCapacity.applyToItem(stack, level);
        itemEntity.setItem(stack);
    }
}
