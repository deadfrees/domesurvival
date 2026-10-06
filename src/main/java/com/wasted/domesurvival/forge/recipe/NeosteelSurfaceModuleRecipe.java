package com.wasted.domesurvival.forge.recipe;

import com.wasted.domesurvival.forge.item.ModItems;
import com.wasted.domesurvival.forge.item.NeosteelArmorItem;
import net.minecraft.core.RegistryAccess;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.inventory.CraftingContainer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.CraftingBookCategory;
import net.minecraft.world.item.crafting.CustomRecipe;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.NotNull;

/**
 * Installs one Surface Protection Module into one Neosteel armor piece while
 * preserving the armor ItemStack NBT (most importantly its current FE charge).
 */
public final class NeosteelSurfaceModuleRecipe extends CustomRecipe {
    public NeosteelSurfaceModuleRecipe(ResourceLocation id, CraftingBookCategory category) {
        super(id, category);
    }

    @Override
    public boolean matches(CraftingContainer container, Level level) {
        ItemStack armor = ItemStack.EMPTY;
        boolean module = false;

        for (int i = 0; i < container.getContainerSize(); i++) {
            ItemStack stack = container.getItem(i);
            if (stack.isEmpty()) continue;

            if (stack.getItem() instanceof NeosteelArmorItem) {
                if (!armor.isEmpty() || NeosteelArmorItem.hasSurfaceProtectionModule(stack)) return false;
                armor = stack;
            } else if (stack.is(ModItems.SURFACE_PROTECTION_MODULE.get())) {
                if (module) return false;
                module = true;
            } else {
                return false;
            }
        }

        return !armor.isEmpty() && module;
    }

    @Override
    public @NotNull ItemStack assemble(CraftingContainer container, RegistryAccess registryAccess) {
        for (int i = 0; i < container.getContainerSize(); i++) {
            ItemStack stack = container.getItem(i);
            if (stack.getItem() instanceof NeosteelArmorItem
                    && !NeosteelArmorItem.hasSurfaceProtectionModule(stack)) {
                return NeosteelArmorItem.installSurfaceProtectionModule(stack);
            }
        }
        return ItemStack.EMPTY;
    }

    @Override
    public boolean canCraftInDimensions(int width, int height) {
        return width * height >= 2;
    }

    @Override
    public @NotNull RecipeSerializer<?> getSerializer() {
        return ModRecipes.NEOSTEEL_SURFACE_MODULE_SERIALIZER.get();
    }
}
