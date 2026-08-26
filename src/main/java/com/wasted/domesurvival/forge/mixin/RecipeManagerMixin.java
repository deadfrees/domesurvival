package com.wasted.domesurvival.forge.mixin;

import com.mojang.datafixers.util.Pair;
import com.wasted.domesurvival.forge.technology.TechnologyUnlockService;
import net.minecraft.world.Container;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeManager;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.List;
import java.util.Optional;
import java.util.ArrayList;

@Mixin(RecipeManager.class)
public abstract class RecipeManagerMixin {
    @Inject(
            method = {
                    "getRecipeFor(Lnet/minecraft/world/item/crafting/RecipeType;Lnet/minecraft/world/Container;Lnet/minecraft/world/level/Level;)Ljava/util/Optional;",
                    "m_44015_(Lnet/minecraft/world/item/crafting/RecipeType;Lnet/minecraft/world/Container;Lnet/minecraft/world/level/Level;)Ljava/util/Optional;"
            },
            at = @At("RETURN"),
            cancellable = true,
            remap = false
    )
    private <C extends Container, T extends Recipe<C>> void domesurvival$filterRecipe(
            RecipeType<T> type,
            C container,
            Level level,
            CallbackInfoReturnable<Optional<T>> callback
    ) {
        Optional<T> result = callback.getReturnValue();
        if (result.isPresent() && !TechnologyUnlockService.isUnlocked(
                level, result.get().getResultItem(level.registryAccess()))) {
            callback.setReturnValue(Optional.empty());
        }
    }

    @Inject(
            method = {
                    "getRecipeFor(Lnet/minecraft/world/item/crafting/RecipeType;Lnet/minecraft/world/Container;Lnet/minecraft/world/level/Level;Lnet/minecraft/resources/ResourceLocation;)Ljava/util/Optional;",
                    "m_220248_(Lnet/minecraft/world/item/crafting/RecipeType;Lnet/minecraft/world/Container;Lnet/minecraft/world/level/Level;Lnet/minecraft/resources/ResourceLocation;)Ljava/util/Optional;"
            },
            at = @At("RETURN"),
            cancellable = true,
            remap = false
    )
    private <C extends Container, T extends Recipe<C>> void domesurvival$filterRecipeWithHint(
            RecipeType<T> type,
            C container,
            Level level,
            net.minecraft.resources.ResourceLocation hint,
            CallbackInfoReturnable<Optional<Pair<net.minecraft.resources.ResourceLocation, T>>> callback
    ) {
        Optional<Pair<net.minecraft.resources.ResourceLocation, T>> result = callback.getReturnValue();
        if (result.isPresent() && !TechnologyUnlockService.isUnlocked(
                level, result.get().getSecond().getResultItem(level.registryAccess()))) {
            callback.setReturnValue(Optional.empty());
        }
    }

    @Inject(
            method = {
                    "getRecipesFor(Lnet/minecraft/world/item/crafting/RecipeType;Lnet/minecraft/world/Container;Lnet/minecraft/world/level/Level;)Ljava/util/List;",
                    "m_44056_(Lnet/minecraft/world/item/crafting/RecipeType;Lnet/minecraft/world/Container;Lnet/minecraft/world/level/Level;)Ljava/util/List;"
            },
            at = @At("RETURN"),
            cancellable = true,
            remap = false
    )
    private <C extends Container, T extends Recipe<C>> void domesurvival$filterRecipeList(
            RecipeType<T> type,
            C container,
            Level level,
            CallbackInfoReturnable<List<T>> callback
    ) {
        List<T> filtered = new ArrayList<>(callback.getReturnValue());
        filtered.removeIf(recipe -> !TechnologyUnlockService.isUnlocked(
                level, recipe.getResultItem(level.registryAccess())));
        callback.setReturnValue(filtered);
    }
}
