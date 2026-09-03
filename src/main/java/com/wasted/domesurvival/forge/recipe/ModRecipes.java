package com.wasted.domesurvival.forge.recipe;

import com.wasted.domesurvival.forge.DomeSurvival;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public final class ModRecipes {
    public static final DeferredRegister<RecipeType<?>> RECIPE_TYPES =
            DeferredRegister.create(ForgeRegistries.RECIPE_TYPES, DomeSurvival.MOD_ID);
    public static final DeferredRegister<RecipeSerializer<?>> RECIPE_SERIALIZERS =
            DeferredRegister.create(ForgeRegistries.RECIPE_SERIALIZERS, DomeSurvival.MOD_ID);

    public static final RegistryObject<RecipeType<FormingPressRecipe>> FORMING_TYPE =
            RECIPE_TYPES.register("forming", () -> new RecipeType<>() {
                @Override
                public String toString() {
                    return DomeSurvival.MOD_ID + ":forming";
                }
            });

    public static final RegistryObject<RecipeSerializer<FormingPressRecipe>> FORMING_SERIALIZER =
            RECIPE_SERIALIZERS.register("forming", FormingPressRecipe.Serializer::new);

    public static void register(IEventBus eventBus) {
        RECIPE_TYPES.register(eventBus);
        RECIPE_SERIALIZERS.register(eventBus);
    }

    private ModRecipes() {
    }
}
