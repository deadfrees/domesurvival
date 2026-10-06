package com.wasted.domesurvival.forge.machine.module;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/**
 * Physical item wrapper for a logical machine module definition.
 */
public final class MachineModuleItem extends Item {
    private final MachineModule module;

    public MachineModuleItem(MachineModule module, Properties properties) {
        super(properties);
        if (module == null) {
            throw new IllegalArgumentException("module");
        }
        this.module = module;
    }

    public MachineModule module() {
        return module;
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level,
                                List<Component> tooltip, TooltipFlag flag) {
        super.appendHoverText(stack, level, tooltip, flag);
        tooltip.add(Component.translatable("tooltip.domesurvival.module.install").withStyle(ChatFormatting.GRAY));
        tooltip.add(Component.translatable("tooltip.domesurvival.module.install_action").withStyle(ChatFormatting.GRAY));

        String baseKey = "tooltip.domesurvival.module." + module.id().getPath();
        tooltip.add(Component.translatable(baseKey + ".title")
                .withStyle(titleColor(module.type())));
        tooltip.add(Component.translatable(baseKey + ".line1")
                .withStyle(ChatFormatting.GRAY));
        tooltip.add(Component.translatable(baseKey + ".line2")
                .withStyle(ChatFormatting.DARK_GRAY));

        if (module.type() == MachineModuleType.EFFICIENCY
                || module.type() == MachineModuleType.OVERDRIVE) {
            tooltip.add(Component.translatable(baseKey + ".line3")
                    .withStyle(ChatFormatting.RED));
        }
    }

    private static ChatFormatting titleColor(MachineModuleType type) {
        return switch (type) {
            case EFFICIENCY -> ChatFormatting.GREEN;
            case OVERDRIVE -> ChatFormatting.GOLD;
            case BUFFER -> ChatFormatting.AQUA;
            case AUTOMATION -> ChatFormatting.LIGHT_PURPLE;
            case EMERGENCY_PROTECTION -> ChatFormatting.RED;
            case COMMUNICATION -> ChatFormatting.BLUE;
        };
    }
}
