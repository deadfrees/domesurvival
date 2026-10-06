package com.wasted.domesurvival.forge.client.jei;

import com.mojang.logging.LogUtils;
import com.wasted.domesurvival.forge.DomeSurvival;
import com.wasted.domesurvival.forge.machine.alloy.AlloyEnricherRegistry;
import com.wasted.domesurvival.forge.recipe.AlloyEnricherRecipe;
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

@JeiPlugin
public final class AlloyEnricherJeiPlugin implements IModPlugin {
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final ResourceLocation PLUGIN_ID =
            ResourceLocation.fromNamespaceAndPath(DomeSurvival.MOD_ID, "jei_alloy_enricher");

    public static final RecipeType<AlloyEnricherRecipe> ALLOY_ENRICHER =
            RecipeType.create(DomeSurvival.MOD_ID, "alloy_enricher", AlloyEnricherRecipe.class);

    @Override
    public ResourceLocation getPluginUid() {
        return PLUGIN_ID;
    }

    @Override
    public void registerCategories(IRecipeCategoryRegistration registration) {
        registration.addRecipeCategories(new AlloyEnricherRecipeCategory(
                registration.getJeiHelpers().getGuiHelper(), ALLOY_ENRICHER
        ));
        LOGGER.info("[DomeSurvival/JEI] Registered Alloy Enricher category");
    }

    @Override
    public void registerRecipes(IRecipeRegistration registration) {
        var level = Minecraft.getInstance().level;
        if (level == null) {
            LOGGER.warn("[DomeSurvival/JEI] Client level unavailable while registering Alloy Enricher recipes");
            return;
        }

        List<AlloyEnricherRecipe> recipes = level.getRecipeManager()
                .getAllRecipesFor(ModRecipes.ALLOY_ENRICHER_TYPE.get());
        registration.addRecipes(ALLOY_ENRICHER, recipes);
        LOGGER.info("[DomeSurvival/JEI] Registered {} Alloy Enricher recipes", recipes.size());
    }

    @Override
    public void registerRecipeCatalysts(IRecipeCatalystRegistration registration) {
        registration.addRecipeCatalyst(AlloyEnricherRegistry.ALLOY_ENRICHER_ITEM.get(), ALLOY_ENRICHER);
    }
}
