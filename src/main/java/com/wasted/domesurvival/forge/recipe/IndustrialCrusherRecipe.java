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

    public IndustrialCrusherRecipe(ResourceLocation id, Ingredient ingredient, ItemStack result,
                                    ItemStack byproduct, int byproductChancePerTenThousand,
                                    int inputCount, int processingTime, int energy) {
        this.id = id;
        this.ingredient = ingredient;
        this.result = result.copy();
        this.byproduct = byproduct.copy();
        this.byproductChancePerTenThousand = Math.max(0, Math.min(10_000, byproductChancePerTenThousand));
        this.inputCount = Math.max(1, inputCount);
        this.processingTime = Math.max(1, processingTime);
        this.energy = Math.max(1, energy);
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

    public static final class Serializer implements RecipeSerializer<IndustrialCrusherRecipe> {
        @Override
        public @NotNull IndustrialCrusherRecipe fromJson(@NotNull ResourceLocation recipeId, @NotNull JsonObject json) {
            Ingredient ingredient = Ingredient.fromJson(GsonHelper.getAsJsonObject(json, "ingredient"));
            ItemStack result = CraftingHelper.getItemStack(GsonHelper.getAsJsonObject(json, "result"), true);
            ItemStack byproduct = json.has("byproduct")
                    ? CraftingHelper.getItemStack(GsonHelper.getAsJsonObject(json, "byproduct"), true)
                    : ItemStack.EMPTY;
            int chance = GsonHelper.getAsInt(json, "byproductChance", byproduct.isEmpty() ? 0 : 1_000);
            int inputCount = GsonHelper.getAsInt(json, "inputCount", 1);
            int processingTime = GsonHelper.getAsInt(json, "processingTime", DEFAULT_PROCESSING_TIME);
            int energy = GsonHelper.getAsInt(json, "energy", DEFAULT_ENERGY);
            return new IndustrialCrusherRecipe(recipeId, ingredient, result, byproduct, chance,
                    inputCount, processingTime, energy);
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
            return new IndustrialCrusherRecipe(recipeId, ingredient, result, byproduct, chance,
                    inputCount, processingTime, energy);
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
        }
    }
}
