package com.wasted.domesurvival.forge.compat.jei;

import com.wasted.domesurvival.forge.fluid.ModFluids;
import com.wasted.domesurvival.forge.machine.organic.OrganicProcessorRegistry;
import com.wasted.domesurvival.forge.recipe.OrganicProcessorRecipe;
import mezz.jei.api.gui.builder.IRecipeLayoutBuilder;
import mezz.jei.api.gui.drawable.IDrawable;
import mezz.jei.api.gui.ingredient.IRecipeSlotsView;
import mezz.jei.api.helpers.IGuiHelper;
import mezz.jei.api.recipe.IFocusGroup;
import mezz.jei.api.recipe.RecipeIngredientRole;
import mezz.jei.api.recipe.category.IRecipeCategory;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.NotNull;

import java.util.Arrays;
import java.util.List;

public final class OrganicProcessorJeiCategory implements IRecipeCategory<OrganicProcessorRecipe> {
    private static final int WIDTH = 154;
    private static final int HEIGHT = 64;

    private final IDrawable icon;

    public OrganicProcessorJeiCategory(IGuiHelper guiHelper) {
        this.icon = guiHelper.createDrawableItemLike(OrganicProcessorRegistry.ORGANIC_PROCESSOR.get());
    }

    @Override
    public @NotNull mezz.jei.api.recipe.RecipeType<OrganicProcessorRecipe> getRecipeType() {
        return DomeSurvivalJeiPlugin.ORGANIC_PROCESSING;
    }

    @Override
    public @NotNull Component getTitle() {
        return Component.literal("Органическая переработка");
    }

    @Override public int getWidth() { return WIDTH; }
    @Override public int getHeight() { return HEIGHT; }
    @Override public IDrawable getIcon() { return icon; }

    @Override
    public void setRecipe(IRecipeLayoutBuilder builder, OrganicProcessorRecipe recipe, IFocusGroup focuses) {
        builder.addSlot(RecipeIngredientRole.INPUT, 8, 18)
                .setStandardSlotBackground()
                .addItemStacks(withCount(recipe.getPrimary().getItems(), recipe.getPrimaryCount()));

        builder.addSlot(RecipeIngredientRole.INPUT, 34, 18)
                .setStandardSlotBackground()
                .addItemStacks(withCount(recipe.getAdditive().getItems(), recipe.getAdditiveCount()));

        builder.addSlot(RecipeIngredientRole.INPUT, 60, 18)
                .setStandardSlotBackground()
                .setFluidRenderer(Math.max(1, recipe.getWaterMb()), true, 16, 16)
                .addFluidStack(ModFluids.PURIFIED_WATER.get(), recipe.getWaterMb());

        builder.addSlot(RecipeIngredientRole.OUTPUT, 126, 18)
                .setOutputSlotBackground()
                .addItemStack(recipe.getResult());
    }

    @Override
    public void draw(OrganicProcessorRecipe recipe, IRecipeSlotsView recipeSlotsView,
                     GuiGraphics graphics, double mouseX, double mouseY) {
        graphics.fill(84, 24, 116, 32, 0xFF151A1E);
        graphics.fill(86, 26, 114, 30, 0xFF6FA66F);

        var font = Minecraft.getInstance().font;
        graphics.drawString(font,
                Component.literal(recipe.getEnergy() + " FE"),
                84, 39, 0xFFD8DEE3, false);
        graphics.drawString(font,
                Component.literal(formatSeconds(recipe.getProcessingTime())),
                84, 50, 0xFFAEB9C2, false);
    }

    private static List<ItemStack> withCount(ItemStack[] stacks, int count) {
        return Arrays.stream(stacks)
                .map(stack -> stack.copyWithCount(Math.max(1, count)))
                .toList();
    }

    private static String formatSeconds(int ticks) {
        int safeTicks = Math.max(0, ticks);
        int whole = safeTicks / 20;
        int tenths = (safeTicks % 20) / 2;
        return whole + "." + tenths + " s";
    }
}
