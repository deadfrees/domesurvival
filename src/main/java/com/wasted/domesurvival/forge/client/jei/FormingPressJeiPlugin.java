package com.wasted.domesurvival.forge.client.jei;

import com.mojang.logging.LogUtils;
import com.wasted.domesurvival.forge.DomeSurvival;
import com.wasted.domesurvival.forge.machine.forming.FormingPressRegistry;
import com.wasted.domesurvival.forge.recipe.FormingPressRecipe;
import com.wasted.domesurvival.forge.recipe.ModRecipes;
import mezz.jei.api.IModPlugin;
import mezz.jei.api.JeiPlugin;
import mezz.jei.api.recipe.RecipeType;
import mezz.jei.api.registration.IRecipeCatalystRegistration;
import mezz.jei.api.registration.IRecipeCategoryRegistration;
import mezz.jei.api.registration.IRecipeRegistration;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.ResourceLocation;
import org.slf4j.Logger;

import java.util.List;

/** Dedicated JEI plugin for the data-driven universal forming press recipes. */
@JeiPlugin
public final class FormingPressJeiPlugin implements IModPlugin {
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final ResourceLocation PLUGIN_ID =
            ResourceLocation.fromNamespaceAndPath(DomeSurvival.MOD_ID, "jei_forming_press");

    public static final RecipeType<FormingPressRecipe> FORMING_PRESS =
            RecipeType.create(DomeSurvival.MOD_ID, "forming_press", FormingPressRecipe.class);

    @Override
    public ResourceLocation getPluginUid() {
        return PLUGIN_ID;
    }

    @Override
    public void registerCategories(IRecipeCategoryRegistration registration) {
        registration.addRecipeCategories(new FormingPressRecipeCategory(
                registration.getJeiHelpers().getGuiHelper(), FORMING_PRESS
        ));
        LOGGER.info("[DomeSurvival/JEI] Registered forming-press GUI category");
    }

    @Override
    public void registerRecipes(IRecipeRegistration registration) {
        var level = Minecraft.getInstance().level;
        if (level == null) {
            LOGGER.warn("[DomeSurvival/JEI] Client level unavailable while registering forming recipes");
            return;
        }

        List<FormingPressRecipe> recipes = level.getRecipeManager()
                .getAllRecipesFor(ModRecipes.FORMING_TYPE.get());
        registration.addRecipes(FORMING_PRESS, recipes);
        LOGGER.info("[DomeSurvival/JEI] Registered {} forming-press recipes", recipes.size());
    }

    @Override
    public void registerRecipeCatalysts(IRecipeCatalystRegistration registration) {
        registration.addRecipeCatalyst(FormingPressRegistry.FORMING_PRESS_ITEM.get(), FORMING_PRESS);
    }
}
