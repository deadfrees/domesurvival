package com.wasted.domesurvival.forge.client.jei;

import com.wasted.domesurvival.forge.machine.forming.FormingPressRegistry;
import com.wasted.domesurvival.forge.recipe.FormingPressRecipe;
import mezz.jei.api.gui.builder.IRecipeLayoutBuilder;
import mezz.jei.api.gui.drawable.IDrawable;
import mezz.jei.api.gui.ingredient.IRecipeSlotsView;
import mezz.jei.api.helpers.IGuiHelper;
import mezz.jei.api.recipe.IFocusGroup;
import mezz.jei.api.recipe.RecipeType;
import mezz.jei.api.recipe.category.IRecipeCategory;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/** JEI view of the forming press using the same visual language as the in-game machine GUI. */
final class FormingPressRecipeCategory implements IRecipeCategory<FormingPressRecipe> {
    private static final int WIDTH = 180;
    private static final int HEIGHT = 128;

    private static final int INPUT_X = 17;
    private static final int OUTPUT_X = 147;
    private static final int SLOT_Y = 48;

    private final RecipeType<FormingPressRecipe> recipeType;
    private final IDrawable icon;

    FormingPressRecipeCategory(IGuiHelper guiHelper, RecipeType<FormingPressRecipe> recipeType) {
        this.recipeType = recipeType;
        this.icon = guiHelper.createDrawableItemStack(new ItemStack(FormingPressRegistry.FORMING_PRESS_ITEM.get()));
    }

    @Override
    public RecipeType<FormingPressRecipe> getRecipeType() {
        return recipeType;
    }

    @Override
    public Component getTitle() {
        return Component.translatable(FormingPressRegistry.FORMING_PRESS.get().getDescriptionId());
    }

    @Override
    public int getWidth() {
        return WIDTH;
    }

    @Override
    public int getHeight() {
        return HEIGHT;
    }

    @Override
    public IDrawable getIcon() {
        return icon;
    }

    @Override
    public void setRecipe(IRecipeLayoutBuilder builder, FormingPressRecipe recipe, IFocusGroup focuses) {
        List<ItemStack> inputs = new ArrayList<>();
        for (ItemStack candidate : recipe.getIngredient().getItems()) {
            ItemStack stack = candidate.copy();
            stack.setCount(recipe.getInputCount());
            inputs.add(stack);
        }
        if (inputs.isEmpty()) {
            inputs.add(new ItemStack(Items.BARRIER));
        }

        builder.addInputSlot(INPUT_X, SLOT_Y)
                .addItemStacks(inputs);
        builder.addOutputSlot(OUTPUT_X, SLOT_Y)
                .addItemStack(recipe.getResult());
    }

    @Override
    public void draw(FormingPressRecipe recipe, IRecipeSlotsView recipeSlotsView,
                     GuiGraphics graphics, double mouseX, double mouseY) {
        RefinedMachineJeiArt.panel(graphics, "forming", Component.translatable(
                "gui.domesurvival.forming_press.operation." + recipe.getOperation().getSerializedName()));
        RefinedMachineJeiArt.machine(graphics,FormingPressRegistry.FORMING_PRESS.get().defaultBlockState(),true);
        RefinedMachineJeiArt.progress(graphics,49,85,82,recipe.getProcessingTime());

        RefinedMachineJeiArt.text(graphics,RefinedMachineJeiArt.t("jei.forming",recipe.getEnergy(),
                String.format(Locale.ROOT,"%.1f",recipe.getProcessingTime()/20.0)),12,101,156,DomeJeiStyle.TEXT_MUTED);
        RefinedMachineJeiArt.text(graphics,RefinedMachineJeiArt.t("jei.base"),12,113,156,DomeJeiStyle.TEXT_DIM);
    }
}
