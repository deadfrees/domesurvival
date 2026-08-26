package com.wasted.domesurvival.forge.item;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;

import javax.annotation.Nullable;
import java.util.List;

/**
 * Archive genetic sample. These items intentionally have no recipe.
 */
public final class CryocapsuleItem extends Item {
    private final boolean damaged;

    public CryocapsuleItem(boolean damaged, Properties properties) {
        super(properties);
        this.damaged = damaged;
    }

    @Override
    public void appendHoverText(
            ItemStack stack,
            @Nullable Level level,
            List<Component> tooltip,
            TooltipFlag flag
    ) {
        tooltip.add(Component.translatable(
                damaged
                        ? "tooltip.domesurvival.cryocapsule.damaged"
                        : "tooltip.domesurvival.cryocapsule.viable"
        ).withStyle(damaged ? ChatFormatting.DARK_RED : ChatFormatting.AQUA));

        tooltip.add(Component.translatable(
                "tooltip.domesurvival.cryocapsule.archive"
        ).withStyle(ChatFormatting.DARK_GRAY));
    }
}
