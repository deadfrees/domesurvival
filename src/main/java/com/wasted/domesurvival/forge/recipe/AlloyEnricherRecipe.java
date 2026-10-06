package com.wasted.domesurvival.forge.recipe;

import com.google.gson.JsonObject;
import com.google.gson.JsonSyntaxException;
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
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.Fluids;
import net.minecraftforge.common.crafting.CraftingHelper;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.registries.ForgeRegistries;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public final class AlloyEnricherRecipe implements Recipe<SimpleContainer> {
    public static final int DEFAULT_PROCESSING_TIME = 220;
    public static final int DEFAULT_ENERGY = 25_000;
    public static final int DEFAULT_FLUID_AMOUNT = 250;

    private final ResourceLocation id;
    private final Ingredient ingredient;
    private final int inputCount;
    private final FluidStack fluidInput;
    private final ItemStack result;
    private final int processingTime;
    private final int energy;

    public AlloyEnricherRecipe(ResourceLocation id, Ingredient ingredient, int inputCount,
                               FluidStack fluidInput, ItemStack result,
                               int processingTime, int energy) {
        this.id = id;
        this.ingredient = ingredient;
        this.inputCount = Math.max(1, inputCount);
        this.fluidInput = fluidInput.copy();
        this.result = result.copy();
        this.processingTime = Math.max(1, processingTime);
        this.energy = Math.max(1, energy);
    }

    @Override
    public boolean matches(SimpleContainer container, Level level) {
        ItemStack input = container.getItem(0);
        return ingredient.test(input) && input.getCount() >= inputCount;
    }

    public boolean acceptsIngredient(ItemStack stack) {
        return !stack.isEmpty() && ingredient.test(stack);
    }

    @Override
    public @NotNull ItemStack assemble(SimpleContainer container, RegistryAccess registryAccess) {
        return result.copy();
    }

    @Override public boolean canCraftInDimensions(int width, int height) { return true; }
    @Override public @NotNull ItemStack getResultItem(RegistryAccess registryAccess) { return result.copy(); }
    @Override public @NotNull ResourceLocation getId() { return id; }
    @Override public @NotNull RecipeSerializer<?> getSerializer() { return ModRecipes.ALLOY_ENRICHER_SERIALIZER.get(); }
    @Override public @NotNull RecipeType<?> getType() { return ModRecipes.ALLOY_ENRICHER_TYPE.get(); }
    @Override public boolean isSpecial() { return true; }

    public Ingredient getIngredient() { return ingredient; }
    public int getInputCount() { return inputCount; }
    public FluidStack getFluidInput() { return fluidInput.copy(); }
    public ItemStack getResult() { return result.copy(); }
    public int getProcessingTime() { return processingTime; }
    public int getEnergy() { return energy; }

    public static final class Serializer implements RecipeSerializer<AlloyEnricherRecipe> {
        @Override
        public @NotNull AlloyEnricherRecipe fromJson(@NotNull ResourceLocation recipeId,
                                                       @NotNull JsonObject json) {
            Ingredient ingredient = Ingredient.fromJson(GsonHelper.getAsJsonObject(json, "ingredient"));
            int inputCount = GsonHelper.getAsInt(json, "input_count", 1);
            ItemStack result = CraftingHelper.getItemStack(GsonHelper.getAsJsonObject(json, "result"), true);

            JsonObject fluidJson = GsonHelper.getAsJsonObject(json, "fluid_input");
            ResourceLocation fluidId = ResourceLocation.tryParse(GsonHelper.getAsString(fluidJson, "fluid"));
            if (fluidId == null) {
                throw new JsonSyntaxException("Invalid alloy enricher fluid id in " + recipeId);
            }
            Fluid fluid = ForgeRegistries.FLUIDS.getValue(fluidId);
            if (fluid == null || fluid == Fluids.EMPTY) {
                throw new JsonSyntaxException("Unknown alloy enricher fluid '" + fluidId + "' in " + recipeId);
            }
            int fluidAmount = Math.max(1, GsonHelper.getAsInt(fluidJson, "amount", DEFAULT_FLUID_AMOUNT));
            FluidStack fluidInput = new FluidStack(fluid, fluidAmount);

            int processingTime = GsonHelper.getAsInt(json, "processing_time", DEFAULT_PROCESSING_TIME);
            int energy = GsonHelper.getAsInt(json, "energy", DEFAULT_ENERGY);
            return new AlloyEnricherRecipe(recipeId, ingredient, inputCount, fluidInput,
                    result, processingTime, energy);
        }

        @Override
        public @Nullable AlloyEnricherRecipe fromNetwork(@NotNull ResourceLocation recipeId,
                                                           @NotNull FriendlyByteBuf buffer) {
            Ingredient ingredient = Ingredient.fromNetwork(buffer);
            int inputCount = buffer.readVarInt();
            ResourceLocation fluidId = buffer.readResourceLocation();
            Fluid fluid = ForgeRegistries.FLUIDS.getValue(fluidId);
            int fluidAmount = buffer.readVarInt();
            ItemStack result = buffer.readItem();
            int processingTime = buffer.readVarInt();
            int energy = buffer.readVarInt();
            if (fluid == null || fluid == Fluids.EMPTY) {
                throw new IllegalStateException("Unknown alloy enricher fluid from network: " + fluidId);
            }
            return new AlloyEnricherRecipe(recipeId, ingredient, inputCount,
                    new FluidStack(fluid, fluidAmount), result, processingTime, energy);
        }

        @Override
        public void toNetwork(@NotNull FriendlyByteBuf buffer, @NotNull AlloyEnricherRecipe recipe) {
            recipe.ingredient.toNetwork(buffer);
            buffer.writeVarInt(recipe.inputCount);
            ResourceLocation fluidId = ForgeRegistries.FLUIDS.getKey(recipe.fluidInput.getFluid());
            if (fluidId == null) {
                throw new IllegalStateException("Unregistered alloy enricher fluid: " + recipe.fluidInput.getFluid());
            }
            buffer.writeResourceLocation(fluidId);
            buffer.writeVarInt(recipe.fluidInput.getAmount());
            buffer.writeItem(recipe.result);
            buffer.writeVarInt(recipe.processingTime);
            buffer.writeVarInt(recipe.energy);
        }
    }
}
