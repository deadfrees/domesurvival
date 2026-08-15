package com.wasted.domesurvival.forge.machine.copper;

import com.wasted.domesurvival.forge.registry.ModBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.FurnaceMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.AbstractFurnaceBlockEntity;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Early-game copper furnace balanced after IndustrialCraft's Iron Furnace:
 * 8 seconds per vanilla 10-second smelt and 25% longer fuel duration.
 *
 * <p>The vanilla/Forge furnace pipeline is retained for recipe and fuel compatibility.</p>
 */
public final class CopperFurnaceBlockEntity extends AbstractFurnaceBlockEntity {
    private static final int SPEED_NUMERATOR = 4;
    private static final int SPEED_DENOMINATOR = 5;
    private static final int FUEL_NUMERATOR = 5;
    private static final int FUEL_DENOMINATOR = 4;

    public CopperFurnaceBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.COPPER_FURNACE.get(), pos, state, RecipeType.SMELTING);
    }

    @Override
    protected Component getDefaultName() {
        return Component.translatable("container.domesurvival.copper_furnace");
    }

    @Override
    protected AbstractContainerMenu createMenu(int containerId, Inventory playerInventory) {
        return new FurnaceMenu(containerId, playerInventory, this, this.dataAccess);
    }

    /**
     * IndustrialCraft-style fuel economy: one fuel item burns 25% longer.
     * Example: vanilla coal 1600 ticks -> 2000 ticks, enough for 10 copper-furnace smelts.
     */
    @Override
    protected int getBurnDuration(ItemStack fuel) {
        int baseDuration = super.getBurnDuration(fuel);
        if (baseDuration <= 0) {
            return 0;
        }
        return (int) Math.min(Integer.MAX_VALUE,
                ((long) baseDuration * FUEL_NUMERATOR) / FUEL_DENOMINATOR);
    }

    /**
     * Runs the normal Forge furnace tick and then adjusts the active recipe's cook time to 80%.
     * This preserves modded smelting recipes with custom cook times instead of hardcoding 160 ticks.
     */
    public static void serverTick(Level level, BlockPos pos, BlockState state, CopperFurnaceBlockEntity furnace) {
        AbstractFurnaceBlockEntity.serverTick(level, pos, state, furnace);

        level.getRecipeManager()
                .getRecipeFor(RecipeType.SMELTING, furnace, level)
                .ifPresent(recipe -> {
                    // Minecraft 1.20.1: getRecipeFor returns the smelting recipe directly.
                    int baseCookTime = recipe.getCookingTime();
                    int scaledCookTime = Math.max(1,
                            (baseCookTime * SPEED_NUMERATOR + SPEED_DENOMINATOR - 1) / SPEED_DENOMINATOR);
                    if (furnace.dataAccess.get(3) != scaledCookTime) {
                        furnace.dataAccess.set(3, scaledCookTime);
                        furnace.setChanged();
                    }
                });
    }
}
