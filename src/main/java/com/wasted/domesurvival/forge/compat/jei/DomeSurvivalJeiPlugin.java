package com.wasted.domesurvival.forge.compat.jei;

import com.wasted.domesurvival.forge.DomeSurvival;
import com.wasted.domesurvival.forge.machine.crusher.IndustrialCrusherRegistry;
import com.wasted.domesurvival.forge.recipe.IndustrialCrusherRecipe;
import com.wasted.domesurvival.forge.recipe.ModRecipes;
import mezz.jei.api.IModPlugin;
import mezz.jei.api.JeiPlugin;
import mezz.jei.api.recipe.RecipeType;
import mezz.jei.api.registration.IRecipeCatalystRegistration;
import mezz.jei.api.registration.IRecipeCategoryRegistration;
import mezz.jei.api.registration.IRecipeRegistration;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.ResourceLocation;

@JeiPlugin
public final class DomeSurvivalJeiPlugin implements IModPlugin {
    private static final ResourceLocation UID =
            ResourceLocation.fromNamespaceAndPath(DomeSurvival.MOD_ID, "jei_plugin");

    public static final RecipeType<IndustrialCrusherRecipe> INDUSTRIAL_CRUSHING =
            RecipeType.create(DomeSurvival.MOD_ID, "industrial_crushing", IndustrialCrusherRecipe.class);

    @Override
    public ResourceLocation getPluginUid() {
        return UID;
    }

    @Override
    public void registerCategories(IRecipeCategoryRegistration registration) {
        registration.addRecipeCategories(
                new IndustrialCrusherJeiCategory(registration.getJeiHelpers().getGuiHelper())
        );
    }

    @Override
    public void registerRecipes(IRecipeRegistration registration) {
        var level = Minecraft.getInstance().level;
        if (level == null) {
            return;
        }
        registration.addRecipes(
                INDUSTRIAL_CRUSHING,
                level.getRecipeManager().getAllRecipesFor(ModRecipes.INDUSTRIAL_CRUSHER_TYPE.get())
        );
    }

    @Override
    public void registerRecipeCatalysts(IRecipeCatalystRegistration registration) {
        registration.addRecipeCatalysts(
                INDUSTRIAL_CRUSHING,
                IndustrialCrusherRegistry.INDUSTRIAL_CRUSHER.get()
        );
    }
}
