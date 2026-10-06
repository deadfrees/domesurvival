package com.wasted.domesurvival.forge.item;

import com.google.common.collect.ImmutableMultimap;
import com.google.common.collect.Multimap;
import com.wasted.domesurvival.forge.DomeSurvival;
import net.minecraft.ChatFormatting;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import net.minecraftforge.common.capabilities.ICapabilityProvider;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.UUID;
import java.util.function.Consumer;

/**
 * Powered Neosteel armor.
 *
 * <p>The ItemStack FE store is the armor's service state: a charged piece exposes its
 * powered armor attributes, while an empty piece falls back to a deliberately weak
 * passive shell. Vanilla durability is never allowed to destroy the armor; durability
 * wear is translated into FE drain instead.</p>
 */
public final class NeosteelArmorItem extends ArmorItem {
    public static final int HELMET_CAPACITY = 100_000;
    public static final int CHESTPLATE_CAPACITY = 300_000;
    public static final int LEGGINGS_CAPACITY = 200_000;
    public static final int BOOTS_CAPACITY = 100_000;
    public static final int MAX_CHARGE_RATE = 4_096;

    /** FE paid for each point of vanilla durability wear that the armor intercepts. */
    public static final int FE_PER_DURABILITY_POINT = 1_200;

    private static final String SURFACE_MODULE_KEY = "DomeSurfaceProtectionModule";

    private static final String LAYER_1 =
            DomeSurvival.MOD_ID + ":textures/models/armor/surface_suit_blue_layer_1.png";
    private static final String LAYER_2 =
            DomeSurvival.MOD_ID + ":textures/models/armor/surface_suit_blue_layer_2.png";

    private static final UUID HELMET_MODIFIER = UUID.fromString("cfbd7ea2-6ba0-4ea9-9df3-ad2069a82331");
    private static final UUID CHEST_MODIFIER = UUID.fromString("ac2a46c0-bc8b-4e66-afdd-14a31fc31852");
    private static final UUID LEGS_MODIFIER = UUID.fromString("25c5af64-b8ab-44df-ab32-767098665e1e");
    private static final UUID BOOTS_MODIFIER = UUID.fromString("042b6b71-5ee0-44ba-932b-d27f0b2fcc07");

    private final Type armorType;
    private final int energyCapacity;

    public NeosteelArmorItem(Type type, int energyCapacity, Properties properties) {
        super(NeosteelArmorMaterial.INSTANCE, type, properties.stacksTo(1));
        this.armorType = type;
        this.energyCapacity = Math.max(1, energyCapacity);
    }

    public Type armorType() {
        return armorType;
    }

    public int energyCapacity() {
        return energyCapacity;
    }

    @Nullable
    @Override
    public ICapabilityProvider initCapabilities(ItemStack stack, @Nullable CompoundTag nbt) {
        // External automation may charge armor but cannot use worn armor as a portable power source.
        return new StackEnergyProvider(stack, energyCapacity, MAX_CHARGE_RATE, 0);
    }

    @Override
    public String getArmorTexture(ItemStack stack, Entity entity, EquipmentSlot slot, String type) {
        return slot == EquipmentSlot.LEGS ? LAYER_2 : LAYER_1;
    }

    /**
     * Forge 47.4.x calls this ItemStack-sensitive hook from ItemStack#getAttributeModifiers.
     * Keeping the low values in NeosteelArmorMaterial provides a safe fallback for any code
     * path that asks only for the vanilla defaults.
     */
    @Override
    public Multimap<Attribute, AttributeModifier> getAttributeModifiers(EquipmentSlot slot, ItemStack stack) {
        if (slot != armorType.getSlot()) {
            return ImmutableMultimap.of();
        }

        boolean powered = getEnergy(stack) > 0;
        int armor = powered ? poweredDefense(armorType) : passiveDefense(armorType);
        double toughness = powered ? 1.0D : 0.0D;
        double knockback = powered ? 0.02D : 0.0D;
        UUID modifierId = modifierId(armorType);

        ImmutableMultimap.Builder<Attribute, AttributeModifier> builder = ImmutableMultimap.builder();
        if (armor > 0) {
            builder.put(Attributes.ARMOR, new AttributeModifier(
                    modifierId, "Neosteel armor", armor, AttributeModifier.Operation.ADDITION));
        }
        if (toughness > 0.0D) {
            builder.put(Attributes.ARMOR_TOUGHNESS, new AttributeModifier(
                    modifierId, "Neosteel toughness", toughness, AttributeModifier.Operation.ADDITION));
        }
        if (knockback > 0.0D) {
            builder.put(Attributes.KNOCKBACK_RESISTANCE, new AttributeModifier(
                    modifierId, "Neosteel knockback resistance", knockback, AttributeModifier.Operation.ADDITION));
        }
        return builder.build();
    }

    /**
     * Do not let vanilla durability destroy powered armor. Wear drains the FE buffer instead;
     * once FE reaches zero the item remains equipped and its attribute hook falls back to the
     * weak passive shell.
     */
    @Override
    public <T extends LivingEntity> int damageItem(ItemStack stack, int amount, T entity, Consumer<T> onBroken) {
        if (amount <= 0) return 0;
        long requested = (long) amount * FE_PER_DURABILITY_POINT;
        int drain = (int) Math.min(Integer.MAX_VALUE, requested);
        consumeEnergy(stack, drain, false);
        if (stack.getDamageValue() != 0) stack.setDamageValue(0);
        return 0;
    }

    @Override
    public boolean isBarVisible(ItemStack stack) {
        return true;
    }

    @Override
    public int getBarWidth(ItemStack stack) {
        return Math.round(13.0F * getEnergy(stack) / energyCapacity);
    }

    @Override
    public int getBarColor(ItemStack stack) {
        return getEnergy(stack) > 0 ? 0x59D9FF : 0x55585A;
    }

    @Override
    public boolean isFoil(ItemStack stack) {
        return hasSurfaceProtectionModule(stack) || super.isFoil(stack);
    }

    @Override
    public void appendHoverText(ItemStack stack, Level level, List<Component> tooltip, TooltipFlag flag) {
        int energy = getEnergy(stack);
        tooltip.add(Component.translatable("tooltip.domesurvival.energy_item.charge", energy, energyCapacity)
                .withStyle(energy > 0 ? ChatFormatting.AQUA : ChatFormatting.RED));
        tooltip.add(Component.translatable(energy > 0
                        ? "tooltip.domesurvival.neosteel_armor.state_powered"
                        : "tooltip.domesurvival.neosteel_armor.state_depleted")
                .withStyle(energy > 0 ? ChatFormatting.GREEN : ChatFormatting.RED));
        tooltip.add(Component.translatable("tooltip.domesurvival.neosteel_armor.non_breaking")
                .withStyle(ChatFormatting.DARK_GRAY));

        String functionKey = switch (armorType) {
            case HELMET -> "tooltip.domesurvival.neosteel_armor.helmet_projectile";
            case CHESTPLATE -> "tooltip.domesurvival.neosteel_armor.chest_shield";
            case LEGGINGS -> "tooltip.domesurvival.neosteel_armor.legs_servo";
            case BOOTS -> "tooltip.domesurvival.neosteel_armor.boots_fall";
        };
        tooltip.add(Component.translatable(functionKey).withStyle(ChatFormatting.BLUE));

        tooltip.add(Component.translatable(hasSurfaceProtectionModule(stack)
                        ? "tooltip.domesurvival.neosteel_armor.surface_module_installed"
                        : "tooltip.domesurvival.neosteel_armor.surface_module_missing")
                .withStyle(hasSurfaceProtectionModule(stack) ? ChatFormatting.GREEN : ChatFormatting.DARK_GRAY));
        super.appendHoverText(stack, level, tooltip, flag);
    }

    public static int getEnergy(ItemStack stack) {
        if (!(stack.getItem() instanceof NeosteelArmorItem armor)) return 0;
        return StackEnergyProvider.getStored(stack, armor.energyCapacity);
    }

    public static int consumeEnergy(ItemStack stack, int amount, boolean simulate) {
        if (!(stack.getItem() instanceof NeosteelArmorItem armor) || amount <= 0) return 0;
        return StackEnergyProvider.consumeInternal(stack, armor.energyCapacity, amount, simulate);
    }

    public static boolean hasSurfaceProtectionModule(ItemStack stack) {
        return stack.getItem() instanceof NeosteelArmorItem
                && stack.hasTag()
                && stack.getTag().getBoolean(SURFACE_MODULE_KEY);
    }

    /** Installs the environmental module while preserving every other ItemStack tag, including FE. */
    public static ItemStack installSurfaceProtectionModule(ItemStack stack) {
        if (!(stack.getItem() instanceof NeosteelArmorItem)) return ItemStack.EMPTY;
        ItemStack result = stack.copy();
        result.setCount(1);
        result.getOrCreateTag().putBoolean(SURFACE_MODULE_KEY, true);
        result.setDamageValue(0);
        return result;
    }

    public static int poweredDefense(Type type) {
        return switch (type) {
            case HELMET -> 3;
            case CHESTPLATE -> 7;
            case LEGGINGS -> 5;
            case BOOTS -> 3;
        };
    }

    public static int passiveDefense(Type type) {
        return switch (type) {
            case HELMET -> 1;
            case CHESTPLATE -> 2;
            case LEGGINGS -> 1;
            case BOOTS -> 1;
        };
    }

    private static UUID modifierId(Type type) {
        return switch (type) {
            case HELMET -> HELMET_MODIFIER;
            case CHESTPLATE -> CHEST_MODIFIER;
            case LEGGINGS -> LEGS_MODIFIER;
            case BOOTS -> BOOTS_MODIFIER;
        };
    }
}
