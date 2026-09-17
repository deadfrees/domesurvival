package com.wasted.domesurvival.forge.recipe;

import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
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

public final class IndustrialCrusherRecipe implements Recipe<SimpleContainer> {
    public static final int DEFAULT_PROCESSING_TIME = 160;
    public static final int DEFAULT_ENERGY = 4_800;

    private final ResourceLocation id;
    private final Ingredient ingredient;
    private final ItemStack result;
    private final ItemStack byproduct;
    private final int byproductChancePerTenThousand;
    private final int inputCount;
    private final int processingTime;
    private final int energy;
    @Nullable private final ResourceLocation gasResult;
    private final int gasAmount;

    public IndustrialCrusherRecipe(ResourceLocation id, Ingredient ingredient, ItemStack result,
                                    ItemStack byproduct, int byproductChancePerTenThousand,
                                    int inputCount, int processingTime, int energy,
                                    @Nullable ResourceLocation gasResult, int gasAmount) {
        this.id = id;
        this.ingredient = ingredient;
        this.result = result.copy();
        this.byproduct = byproduct.copy();
        this.byproductChancePerTenThousand = Math.max(0, Math.min(10_000, byproductChancePerTenThousand));
        this.inputCount = Math.max(1, inputCount);
        this.processingTime = Math.max(1, processingTime);
        this.energy = Math.max(1, energy);
        this.gasResult = gasAmount > 0 ? gasResult : null;
        this.gasAmount = gasResult == null ? 0 : Math.max(0, gasAmount);
    }

    @Override
    public boolean matches(SimpleContainer container, Level level) {
        ItemStack input = container.getItem(0);
        return input.getCount() >= inputCount && ingredient.test(input);
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
    @Override public @NotNull RecipeSerializer<?> getSerializer() { return ModRecipes.INDUSTRIAL_CRUSHER_SERIALIZER.get(); }
    @Override public @NotNull RecipeType<?> getType() { return ModRecipes.INDUSTRIAL_CRUSHER_TYPE.get(); }
    @Override public boolean isSpecial() { return true; }

    public Ingredient getIngredient() { return ingredient; }
    public ItemStack getResult() { return result.copy(); }
    public ItemStack getByproduct() { return byproduct.copy(); }
    public int getByproductChancePerTenThousand() { return byproductChancePerTenThousand; }
    public int getInputCount() { return inputCount; }
    public int getProcessingTime() { return processingTime; }
    public int getEnergy() { return energy; }
    public boolean hasGasResult() { return gasResult != null && gasAmount > 0; }
    public @Nullable ResourceLocation getGasResult() { return gasResult; }
    public int getGasAmount() { return gasAmount; }

    public static final class Serializer implements RecipeSerializer<IndustrialCrusherRecipe> {
        @Override
        public @NotNull IndustrialCrusherRecipe fromJson(@NotNull ResourceLocation recipeId, @NotNull JsonObject json) {
            Ingredient ingredient = Ingredient.fromJson(GsonHelper.getAsJsonObject(json, "ingredient"));
            ItemStack result = json.has("result")
                    ? CraftingHelper.getItemStack(GsonHelper.getAsJsonObject(json, "result"), true)
                    : ItemStack.EMPTY;
            ItemStack byproduct = json.has("byproduct")
                    ? CraftingHelper.getItemStack(GsonHelper.getAsJsonObject(json, "byproduct"), true)
                    : ItemStack.EMPTY;

            int chance = intField(json, "byproduct_chance", "byproductChance", byproduct.isEmpty() ? 0 : 1_000);
            int inputCount = intField(json, "input_count", "inputCount", 1);
            int processingTime = intField(json, "processing_time", "processingTime", DEFAULT_PROCESSING_TIME);
            int energy = GsonHelper.getAsInt(json, "energy", DEFAULT_ENERGY);

            ResourceLocation gasResult = null;
            int gasAmount = 0;
            if (json.has("gas_result")) {
                JsonObject gas = GsonHelper.getAsJsonObject(json, "gas_result");
                gasResult = ResourceLocation.tryParse(GsonHelper.getAsString(gas, "gas"));
                gasAmount = GsonHelper.getAsInt(gas, "amount", 0);
                if (gasResult == null) {
                    throw new JsonParseException("Invalid industrial crusher gas id in " + recipeId);
                }
                if (gasAmount <= 0) {
                    throw new JsonParseException("Industrial crusher gas_result amount must be positive in " + recipeId);
                }
            }

            if (result.isEmpty() && byproduct.isEmpty() && gasAmount <= 0) {
                throw new JsonParseException("Industrial crusher recipe has no output: " + recipeId);
            }

            return new IndustrialCrusherRecipe(recipeId, ingredient, result, byproduct, chance,
                    inputCount, processingTime, energy, gasResult, gasAmount);
        }

        private static int intField(JsonObject json, String canonical, String legacy, int fallback) {
            if (json.has(canonical)) return GsonHelper.getAsInt(json, canonical);
            return GsonHelper.getAsInt(json, legacy, fallback);
        }

        @Override
        public @Nullable IndustrialCrusherRecipe fromNetwork(@NotNull ResourceLocation recipeId,
                                                               @NotNull FriendlyByteBuf buffer) {
            Ingredient ingredient = Ingredient.fromNetwork(buffer);
            ItemStack result = buffer.readItem();
            ItemStack byproduct = buffer.readItem();
            int chance = buffer.readVarInt();
            int inputCount = buffer.readVarInt();
            int processingTime = buffer.readVarInt();
            int energy = buffer.readVarInt();
            ResourceLocation gasResult = null;
            int gasAmount = 0;
            if (buffer.readBoolean()) {
                gasResult = buffer.readResourceLocation();
                gasAmount = buffer.readVarInt();
            }
            return new IndustrialCrusherRecipe(recipeId, ingredient, result, byproduct, chance,
                    inputCount, processingTime, energy, gasResult, gasAmount);
        }

        @Override
        public void toNetwork(@NotNull FriendlyByteBuf buffer, @NotNull IndustrialCrusherRecipe recipe) {
            recipe.ingredient.toNetwork(buffer);
            buffer.writeItem(recipe.result);
            buffer.writeItem(recipe.byproduct);
            buffer.writeVarInt(recipe.byproductChancePerTenThousand);
            buffer.writeVarInt(recipe.inputCount);
            buffer.writeVarInt(recipe.processingTime);
            buffer.writeVarInt(recipe.energy);
            buffer.writeBoolean(recipe.hasGasResult());
            if (recipe.hasGasResult()) {
                buffer.writeResourceLocation(recipe.gasResult);
                buffer.writeVarInt(recipe.gasAmount);
            }
        }
    }
}
