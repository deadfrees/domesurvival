package com.wasted.domesurvival.forge.recipe;

import com.google.gson.JsonObject;
import com.wasted.domesurvival.forge.machine.forming.FormingOperation;
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

public final class FormingPressRecipe implements Recipe<SimpleContainer> {
    public static final int DEFAULT_PROCESSING_TIME = 100;
    public static final int DEFAULT_ENERGY = 2_000;
    public static final int DEFAULT_INPUT_COUNT = 1;

    private final ResourceLocation id;
    private final Ingredient ingredient;
    private final ItemStack result;
    private final int inputCount;
    private final int processingTime;
    private final int energy;
    private final FormingOperation operation;

    public FormingPressRecipe(ResourceLocation id, Ingredient ingredient, ItemStack result,
                              int inputCount, int processingTime, int energy, FormingOperation operation) {
        this.id = id;
        this.ingredient = ingredient;
        this.result = result.copy();
        this.inputCount = Math.max(1, inputCount);
        this.processingTime = Math.max(1, processingTime);
        this.energy = Math.max(1, energy);
        this.operation = operation == null ? FormingOperation.PRESS : operation;
    }

    /** Legacy constructor: old Java callers remain one-item PRESS recipes. */
    public FormingPressRecipe(ResourceLocation id, Ingredient ingredient, ItemStack result,
                              int processingTime, int energy) {
        this(id, ingredient, result, DEFAULT_INPUT_COUNT, processingTime, energy, FormingOperation.PRESS);
    }

    @Override
    public boolean matches(SimpleContainer container, Level level) {
        ItemStack input = container.getItem(0);
        return input.getCount() >= inputCount && ingredient.test(input);
    }

    public boolean matches(SimpleContainer container, FormingOperation selectedOperation) {
        return operation == selectedOperation && matches(container, null);
    }

    @Override
    public @NotNull ItemStack assemble(SimpleContainer container, RegistryAccess registryAccess) {
        return result.copy();
    }

    @Override
    public boolean canCraftInDimensions(int width, int height) {
        return true;
    }

    @Override
    public @NotNull ItemStack getResultItem(RegistryAccess registryAccess) {
        return result.copy();
    }

    @Override
    public @NotNull ResourceLocation getId() {
        return id;
    }

    @Override
    public @NotNull RecipeSerializer<?> getSerializer() {
        return ModRecipes.FORMING_SERIALIZER.get();
    }

    @Override
    public @NotNull RecipeType<?> getType() {
        return ModRecipes.FORMING_TYPE.get();
    }

    @Override
    public boolean isSpecial() {
        return true;
    }

    public ItemStack getResult() {
        return result.copy();
    }

    public int getInputCount() {
        return inputCount;
    }

    public int getProcessingTime() {
        return processingTime;
    }

    public int getEnergy() {
        return energy;
    }

    public FormingOperation getOperation() {
        return operation;
    }

    public static final class Serializer implements RecipeSerializer<FormingPressRecipe> {
        @Override
        public @NotNull FormingPressRecipe fromJson(@NotNull ResourceLocation recipeId, @NotNull JsonObject json) {
            Ingredient ingredient = Ingredient.fromJson(GsonHelper.getAsJsonObject(json, "ingredient"));
            ItemStack result = CraftingHelper.getItemStack(GsonHelper.getAsJsonObject(json, "result"), true);
            int inputCount = GsonHelper.getAsInt(json, "inputCount", DEFAULT_INPUT_COUNT);
            int processingTime = GsonHelper.getAsInt(json, "processingTime", DEFAULT_PROCESSING_TIME);
            int energy = GsonHelper.getAsInt(json, "energy", DEFAULT_ENERGY);
            FormingOperation operation = FormingOperation.fromSerializedName(
                    GsonHelper.getAsString(json, "operation", FormingOperation.PRESS.getSerializedName())
            );
            return new FormingPressRecipe(recipeId, ingredient, result,
                    inputCount, processingTime, energy, operation);
        }

        @Override
        public @Nullable FormingPressRecipe fromNetwork(@NotNull ResourceLocation recipeId,
                                                         @NotNull FriendlyByteBuf buffer) {
            Ingredient ingredient = Ingredient.fromNetwork(buffer);
            ItemStack result = buffer.readItem();
            int inputCount = buffer.readVarInt();
            int processingTime = buffer.readVarInt();
            int energy = buffer.readVarInt();
            FormingOperation operation = FormingOperation.fromOrdinal(buffer.readVarInt());
            return new FormingPressRecipe(recipeId, ingredient, result,
                    inputCount, processingTime, energy, operation);
        }

        @Override
        public void toNetwork(@NotNull FriendlyByteBuf buffer, @NotNull FormingPressRecipe recipe) {
            recipe.ingredient.toNetwork(buffer);
            buffer.writeItem(recipe.result);
            buffer.writeVarInt(recipe.inputCount);
            buffer.writeVarInt(recipe.processingTime);
            buffer.writeVarInt(recipe.energy);
            buffer.writeVarInt(recipe.operation.ordinal());
        }
    }
}
