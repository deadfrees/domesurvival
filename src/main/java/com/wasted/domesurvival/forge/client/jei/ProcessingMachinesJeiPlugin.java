package com.wasted.domesurvival.forge.client.jei;

import com.mojang.logging.LogUtils;
import com.wasted.domesurvival.forge.DomeSurvival;
import com.wasted.domesurvival.forge.machine.crusher.IndustrialCrusherRegistry;
import com.wasted.domesurvival.forge.machine.organic.OrganicProcessorRegistry;
import com.wasted.domesurvival.forge.recipe.IndustrialCrusherRecipe;
import com.wasted.domesurvival.forge.recipe.ModRecipes;
import com.wasted.domesurvival.forge.recipe.OrganicProcessorRecipe;
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

/**
 * Dedicated JEI plugin for the data-driven Industrial Crusher and Organic
 * Processor recipes. Recipes are sourced directly from RecipeManager so new
 * datapack/material recipes appear automatically without Java-side duplication.
 */
@JeiPlugin
public final class ProcessingMachinesJeiPlugin implements IModPlugin {
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final ResourceLocation PLUGIN_ID =
            ResourceLocation.fromNamespaceAndPath(DomeSurvival.MOD_ID, "jei_processing_machines");

    public static final RecipeType<IndustrialCrusherRecipe> INDUSTRIAL_CRUSHER =
            RecipeType.create(DomeSurvival.MOD_ID, "industrial_crusher", IndustrialCrusherRecipe.class);

    public static final RecipeType<OrganicProcessorRecipe> ORGANIC_PROCESSOR =
            RecipeType.create(DomeSurvival.MOD_ID, "organic_processor", OrganicProcessorRecipe.class);

    @Override
    public ResourceLocation getPluginUid() {
        return PLUGIN_ID;
    }

    @Override
    public void registerCategories(IRecipeCategoryRegistration registration) {
        var guiHelper = registration.getJeiHelpers().getGuiHelper();
        registration.addRecipeCategories(
                new IndustrialCrusherRecipeCategory(guiHelper, INDUSTRIAL_CRUSHER),
                new OrganicProcessorRecipeCategory(guiHelper, ORGANIC_PROCESSOR)
        );
        LOGGER.info("[DomeSurvival/JEI] Registered Industrial Crusher and Organic Processor GUI categories");
    }

    @Override
    public void registerRecipes(IRecipeRegistration registration) {
        var level = Minecraft.getInstance().level;
        if (level == null) {
            LOGGER.warn("[DomeSurvival/JEI] Client level unavailable while registering processing recipes");
            return;
        }

        List<IndustrialCrusherRecipe> crusherRecipes = level.getRecipeManager()
                .getAllRecipesFor(ModRecipes.INDUSTRIAL_CRUSHER_TYPE.get());
        List<OrganicProcessorRecipe> organicRecipes = level.getRecipeManager()
                .getAllRecipesFor(ModRecipes.ORGANIC_PROCESSOR_TYPE.get());

        registration.addRecipes(INDUSTRIAL_CRUSHER, crusherRecipes);
        registration.addRecipes(ORGANIC_PROCESSOR, organicRecipes);

        LOGGER.info(
                "[DomeSurvival/JEI] Registered {} crusher recipes and {} organic processor recipes",
                crusherRecipes.size(), organicRecipes.size()
        );
    }

    @Override
    public void registerRecipeCatalysts(IRecipeCatalystRegistration registration) {
        registration.addRecipeCatalyst(
                IndustrialCrusherRegistry.INDUSTRIAL_CRUSHER_ITEM.get(),
                INDUSTRIAL_CRUSHER
        );
        registration.addRecipeCatalyst(
                OrganicProcessorRegistry.ORGANIC_PROCESSOR_ITEM.get(),
                ORGANIC_PROCESSOR
        );
    }
}
