package com.wasted.domesurvival.forge.item;

import com.wasted.domesurvival.forge.bio.BioModuleClientState;
import com.wasted.domesurvival.forge.bio.BioModuleData;
import com.wasted.domesurvival.forge.bio.BioLootData;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import net.minecraftforge.registries.ForgeRegistries;

import javax.annotation.Nullable;
import java.util.List;

/**
 * Archive genetic sample. These items intentionally have no recipe.
 */
public final class CryocapsuleItem extends Item {
    private final ResourceLocation entityId;
    private final boolean damaged;

    public CryocapsuleItem(ResourceLocation entityId, boolean damaged, Properties properties) {
        super(properties);
        this.entityId = entityId;
        this.damaged = damaged;
    }

    @Override
    public Component getName(ItemStack stack) {
        if (!BioModuleData.isIdentificationUnlockedOnClient()) {
            return Component.translatable("item.domesurvival.unknown_bio_module");
        }
        var type = ForgeRegistries.ENTITY_TYPES.getValue(entityId);
        if (type == null || !BioModuleClientState.isAllowed(entityId)) {
            return Component.translatable("item.domesurvival.invalid_bio_module");
        }
        return Component.translatable(
                damaged
                        ? "item.domesurvival.damaged_bio_module"
                        : "item.domesurvival.bio_module_identified",
                type.getDescription()
        );
    }

    @Override
    public void appendHoverText(
            ItemStack stack,
            @Nullable Level level,
            List<Component> tooltip,
            TooltipFlag flag
    ) {
        if (!BioModuleData.isIdentificationUnlocked(level)) {
            tooltip.add(Component.translatable("tooltip.domesurvival.bio.unknown_content")
                    .withStyle(ChatFormatting.GRAY));
            tooltip.add(Component.translatable("tooltip.domesurvival.bio.unknown_condition")
                    .withStyle(ChatFormatting.GRAY));
            tooltip.add(Component.translatable("tooltip.domesurvival.bio.database_locked")
                    .withStyle(ChatFormatting.DARK_AQUA));
            return;
        }

        var type = ForgeRegistries.ENTITY_TYPES.getValue(entityId);
        if (type == null) {
            tooltip.add(Component.translatable("tooltip.domesurvival.bio.unsupported_entity")
                    .withStyle(ChatFormatting.RED));
            return;
        }
        tooltip.add(Component.translatable("tooltip.domesurvival.bio.content", type.getDescription())
                .withStyle(ChatFormatting.AQUA));
        tooltip.add(Component.translatable(
                damaged
                        ? "tooltip.domesurvival.bio.condition_damaged"
                        : "tooltip.domesurvival.bio.condition_viable"
        ).withStyle(damaged ? ChatFormatting.RED : ChatFormatting.GREEN));
        BioLootData.Species species = level != null && level.isClientSide
                ? BioModuleClientState.species(entityId)
                : BioLootData.species(entityId);
        if (species != null) {
            tooltip.add(Component.translatable(
                    "tooltip.domesurvival.bio.nutrient_required", species.feedCount()
            ).withStyle(ChatFormatting.GOLD));
            tooltip.add(Component.translatable("tooltip.domesurvival.bio.rarity." + species.rarity())
                    .withStyle(ChatFormatting.DARK_GRAY));
            tooltip.add(Component.translatable("tooltip.domesurvival.bio.group." + species.lootGroup())
                    .withStyle(ChatFormatting.DARK_GRAY));
        }
    }
}
