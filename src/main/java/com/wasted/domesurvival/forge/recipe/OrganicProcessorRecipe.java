package com.wasted.domesurvival.forge.recipe;

import com.google.gson.JsonObject;
import net.minecraft.core.RegistryAccess;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.GsonHelper;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.Level;
import net.minecraftforge.common.crafting.CraftingHelper;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public final class OrganicProcessorRecipe implements Recipe<SimpleContainer> {
    public static final int DEFAULT_PROCESSING_TIME = 140;
    public static final int DEFAULT_ENERGY = 3_600;
    public static final int DEFAULT_WATER = 250;

    private final ResourceLocation id;
    private final Ingredient primary;
    private final Ingredient additive;
    private final ItemStack result;
    private final int primaryCount;
    private final int additiveCount;
    private final int waterMb;
    private final int processingTime;
    private final int energy;

    public OrganicProcessorRecipe(ResourceLocation id, Ingredient primary, Ingredient additive,
                                  ItemStack result, int primaryCount, int additiveCount,
                                  int waterMb, int processingTime, int energy) {
        this.id = id;
        this.primary = primary;
        this.additive = additive;
        this.result = result.copy();
        this.primaryCount = Math.max(1, primaryCount);
        this.additiveCount = Math.max(1, additiveCount);
        this.waterMb = Math.max(1, waterMb);
        this.processingTime = Math.max(1, processingTime);
        this.energy = Math.max(1, energy);
    }

    @Override
    public boolean matches(SimpleContainer container, Level level) {
        ItemStack primaryStack = container.getItem(0);
        ItemStack additiveStack = container.getItem(1);
        return primary.test(primaryStack)
                && additive.test(additiveStack)
                && primaryStack.getCount() >= primaryCount
                && additiveStack.getCount() >= additiveCount;
    }

    public boolean acceptsPrimary(ItemStack stack) {
        return !stack.isEmpty() && primary.test(stack);
    }

    public boolean acceptsAdditive(ItemStack stack) {
        return !stack.isEmpty() && additive.test(stack);
    }

    @Override
    public @NotNull ItemStack assemble(SimpleContainer container, RegistryAccess registryAccess) {
        return result.copy();
    }

    @Override public boolean canCraftInDimensions(int width, int height) { return true; }
    @Override public @NotNull ItemStack getResultItem(RegistryAccess registryAccess) { return result.copy(); }
    @Override public @NotNull ResourceLocation getId() { return id; }
    @Override public @NotNull RecipeSerializer<?> getSerializer() { return ModRecipes.ORGANIC_PROCESSOR_SERIALIZER.get(); }
    @Override public @NotNull RecipeType<?> getType() { return ModRecipes.ORGANIC_PROCESSOR_TYPE.get(); }
    @Override public boolean isSpecial() { return true; }

    public Ingredient getPrimary() { return primary; }
    public Ingredient getAdditive() { return additive; }
    public ItemStack getResult() { return result.copy(); }
    public int getPrimaryCount() { return primaryCount; }
    public int getAdditiveCount() { return additiveCount; }
    public int getWaterMb() { return waterMb; }
    public int getProcessingTime() { return processingTime; }
    public int getEnergy() { return energy; }

    public static final class Serializer implements RecipeSerializer<OrganicProcessorRecipe> {
        @Override
        public @NotNull OrganicProcessorRecipe fromJson(@NotNull ResourceLocation recipeId, @NotNull JsonObject json) {
            Ingredient primary = Ingredient.fromJson(GsonHelper.getAsJsonObject(json, "primary"));
            Ingredient additive = Ingredient.fromJson(GsonHelper.getAsJsonObject(json, "additive"));
            ItemStack result = CraftingHelper.getItemStack(GsonHelper.getAsJsonObject(json, "result"), true);
            int primaryCount = GsonHelper.getAsInt(json, "primaryCount", 1);
            int additiveCount = GsonHelper.getAsInt(json, "additiveCount", 1);
            int waterMb = GsonHelper.getAsInt(json, "water", DEFAULT_WATER);
            int processingTime = GsonHelper.getAsInt(json, "processingTime", DEFAULT_PROCESSING_TIME);
            int energy = GsonHelper.getAsInt(json, "energy", DEFAULT_ENERGY);
            return new OrganicProcessorRecipe(recipeId, primary, additive, result,
                    primaryCount, additiveCount, waterMb, processingTime, energy);
        }

        @Override
        public @Nullable OrganicProcessorRecipe fromNetwork(@NotNull ResourceLocation recipeId,
                                                             @NotNull FriendlyByteBuf buffer) {
            Ingredient primary = Ingredient.fromNetwork(buffer);
            Ingredient additive = Ingredient.fromNetwork(buffer);
            ItemStack result = buffer.readItem();
            int primaryCount = buffer.readVarInt();
            int additiveCount = buffer.readVarInt();
            int waterMb = buffer.readVarInt();
            int processingTime = buffer.readVarInt();
            int energy = buffer.readVarInt();
            return new OrganicProcessorRecipe(recipeId, primary, additive, result,
                    primaryCount, additiveCount, waterMb, processingTime, energy);
        }

        @Override
        public void toNetwork(@NotNull FriendlyByteBuf buffer, @NotNull OrganicProcessorRecipe recipe) {
            recipe.primary.toNetwork(buffer);
            recipe.additive.toNetwork(buffer);
            buffer.writeItem(recipe.result);
            buffer.writeVarInt(recipe.primaryCount);
            buffer.writeVarInt(recipe.additiveCount);
            buffer.writeVarInt(recipe.waterMb);
            buffer.writeVarInt(recipe.processingTime);
            buffer.writeVarInt(recipe.energy);
        }
    }
}
