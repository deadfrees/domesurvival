package com.wasted.domesurvival.forge.item;

import com.wasted.domesurvival.forge.bio.BioLootData;
import com.wasted.domesurvival.forge.bio.BioModuleClientState;
import com.wasted.domesurvival.forge.bio.BioModuleData;
import net.minecraft.ChatFormatting;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import net.minecraftforge.registries.ForgeRegistries;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/** One physical item for every whitelisted species; identity lives only in server NBT. */
public final class BioModuleItem extends Item {
    public static final String NBT_ENTITY_ID = "EntityId";
    public static final String NBT_DAMAGED = "Damaged";
    public static final String NBT_VERSION = "Version";
    public static final int CURRENT_VERSION = 1;

    public BioModuleItem(Properties properties) {
        super(properties);
    }

    public static ItemStack create(ResourceLocation entityId, boolean damaged) {
        ItemStack stack = new ItemStack(ModItems.BIO_MODULE.get());
        CompoundTag tag = stack.getOrCreateTag();
        tag.putString(NBT_ENTITY_ID, entityId.toString());
        tag.putBoolean(NBT_DAMAGED, damaged);
        tag.putInt(NBT_VERSION, CURRENT_VERSION);
        return stack;
    }

    @Nullable
    public static ResourceLocation entityId(ItemStack stack) {
        if (!stack.is(ModItems.BIO_MODULE.get()) || !stack.hasTag()) return null;
        return ResourceLocation.tryParse(stack.getTag().getString(NBT_ENTITY_ID));
    }

    public static boolean isWhitelisted(ItemStack stack) {
        ResourceLocation entityId = entityId(stack);
        return entityId != null && BioLootData.isAllowed(entityId);
    }

    public static boolean hasDamagedGenome(ItemStack stack) {
        return stack.hasTag() && stack.getTag().getBoolean(NBT_DAMAGED);
    }

    @Override
    public Component getName(ItemStack stack) {
        if (!BioModuleData.isIdentificationUnlockedOnClient()) {
            return Component.translatable("item.domesurvival.unknown_bio_module");
        }
        return identifiedName(stack);
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level,
                                List<Component> tooltip, TooltipFlag flag) {
        if (!BioModuleData.isIdentificationUnlocked(level)) {
            tooltip.add(Component.translatable("tooltip.domesurvival.bio.unknown_content")
                    .withStyle(ChatFormatting.GRAY));
            tooltip.add(Component.translatable("tooltip.domesurvival.bio.unknown_condition")
                    .withStyle(ChatFormatting.GRAY));
            tooltip.add(Component.translatable("tooltip.domesurvival.bio.database_locked")
                    .withStyle(ChatFormatting.DARK_AQUA));
            return;
        }

        ResourceLocation entityId = entityId(stack);
        var entityType = entityId == null ? null : ForgeRegistries.ENTITY_TYPES.getValue(entityId);
        BioLootData.Species species = entityId == null ? null
                : level != null && level.isClientSide
                ? BioModuleClientState.species(entityId)
                : BioLootData.species(entityId);

        if (entityType == null || species == null) {
            tooltip.add(Component.translatable("tooltip.domesurvival.bio.unsupported_entity")
                    .withStyle(ChatFormatting.RED));
            return;
        }

        tooltip.add(Component.translatable("tooltip.domesurvival.bio.content", entityType.getDescription())
                .withStyle(ChatFormatting.AQUA));
        tooltip.add(Component.translatable(
                hasDamagedGenome(stack)
                        ? "tooltip.domesurvival.bio.condition_damaged"
                        : "tooltip.domesurvival.bio.condition_viable"
        ).withStyle(hasDamagedGenome(stack) ? ChatFormatting.RED : ChatFormatting.GREEN));
        tooltip.add(Component.translatable(
                "tooltip.domesurvival.bio.nutrient_required", species.feedCount()
        ).withStyle(ChatFormatting.GOLD));
        tooltip.add(Component.translatable("tooltip.domesurvival.bio.rarity." + species.rarity())
                .withStyle(ChatFormatting.DARK_GRAY));
        tooltip.add(Component.translatable("tooltip.domesurvival.bio.group." + species.lootGroup())
                .withStyle(ChatFormatting.DARK_GRAY));
    }

    private static Component identifiedName(ItemStack stack) {
        ResourceLocation entityId = entityId(stack);
        var entityType = entityId == null ? null : ForgeRegistries.ENTITY_TYPES.getValue(entityId);
        if (entityType == null || !BioModuleClientState.isAllowed(entityId)) {
            return Component.translatable("item.domesurvival.invalid_bio_module");
        }
        return Component.translatable(
                hasDamagedGenome(stack)
                        ? "item.domesurvival.damaged_bio_module"
                        : "item.domesurvival.bio_module_identified",
                entityType.getDescription()
        );
    }
}
