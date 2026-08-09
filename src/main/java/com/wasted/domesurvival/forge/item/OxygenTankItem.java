package com.wasted.domesurvival.forge.item;

import com.wasted.domesurvival.forge.DomeSurvival;
import com.wasted.domesurvival.forge.client.model.OxygenEquipmentModelCache;
import net.minecraft.ChatFormatting;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import net.minecraftforge.client.extensions.common.IClientItemExtensions;

import java.util.List;
import java.util.function.Consumer;

public final class OxygenTankItem extends ArmorItem {
    private static final String OXYGEN_KEY = "domesurvival_oxygen";
    private final int capacity;

    public OxygenTankItem(int capacity, Properties properties) {
        super(OxygenEquipmentMaterial.INSTANCE, Type.CHESTPLATE, properties);
        this.capacity = Math.max(1, capacity);
    }

    public int capacity() {
        return capacity;
    }

    public int getOxygen(ItemStack stack) {
        CompoundTag tag = stack.getTag();
        if (tag == null || !tag.contains(OXYGEN_KEY)) {
            return capacity;
        }
        return Math.max(0, Math.min(capacity, tag.getInt(OXYGEN_KEY)));
    }

    public void setOxygen(ItemStack stack, int oxygen) {
        stack.getOrCreateTag().putInt(OXYGEN_KEY, Math.max(0, Math.min(capacity, oxygen)));
    }

    public boolean consume(ItemStack stack, int amount) {
        int current = getOxygen(stack);
        if (current <= 0 || amount <= 0) {
            return false;
        }
        setOxygen(stack, current - amount);
        return true;
    }

    @Override
    public boolean isBarVisible(ItemStack stack) {
        return getOxygen(stack) < capacity;
    }

    @Override
    public int getBarWidth(ItemStack stack) {
        return Math.round(13.0F * getOxygen(stack) / capacity);
    }

    @Override
    public int getBarColor(ItemStack stack) {
        return 0x55D8FF;
    }

    @Override
    public void appendHoverText(ItemStack stack, Level level, List<Component> tooltip, TooltipFlag flag) {
        int oxygen = getOxygen(stack);
        tooltip.add(Component.translatable("tooltip.domesurvival.oxygen_tank.amount", oxygen, capacity)
                .withStyle(oxygen > 0 ? ChatFormatting.AQUA : ChatFormatting.RED));
        tooltip.add(Component.translatable("tooltip.domesurvival.oxygen_tank.time", oxygen)
                .withStyle(ChatFormatting.GRAY));
        tooltip.add(Component.translatable("tooltip.domesurvival.oxygen_tank.requires_mask")
                .withStyle(ChatFormatting.DARK_GRAY));
        super.appendHoverText(stack, level, tooltip, flag);
    }

    @Override
    public String getArmorTexture(ItemStack stack, Entity entity, EquipmentSlot slot, String type) {
        return DomeSurvival.MOD_ID + ":textures/models/armor/oxygen_tank.png";
    }

    @Override
    public void initializeClient(Consumer<IClientItemExtensions> consumer) {
        consumer.accept(new IClientItemExtensions() {
            @SuppressWarnings({"rawtypes", "unchecked"})
            @Override
            public HumanoidModel<?> getHumanoidArmorModel(
                    LivingEntity livingEntity,
                    ItemStack itemStack,
                    EquipmentSlot equipmentSlot,
                    HumanoidModel<?> original
            ) {
                HumanoidModel model = OxygenEquipmentModelCache.tankModel();
                original.copyPropertiesTo(model);
                return model;
            }
        });
    }
}
