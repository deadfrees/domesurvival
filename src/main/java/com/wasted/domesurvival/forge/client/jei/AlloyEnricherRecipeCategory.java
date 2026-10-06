package com.wasted.domesurvival.forge.client.jei;

import com.wasted.domesurvival.forge.machine.alloy.AlloyEnricherBlockEntity;
import com.wasted.domesurvival.forge.machine.alloy.AlloyEnricherRegistry;
import com.wasted.domesurvival.forge.recipe.AlloyEnricherRecipe;
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

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

final class AlloyEnricherRecipeCategory implements IRecipeCategory<AlloyEnricherRecipe> {
    private static final int WIDTH = 180;
    private static final int HEIGHT = 108;
    private static final int INPUT_X = 18;
    private static final int OUTPUT_X = 147;
    private static final int SLOT_Y = 28;
    private static final int FLUID_X = 49;
    private static final int FLUID_Y = 17;
    private static final int FLUID_W = 14;
    private static final int FLUID_H = 38;
    private static final int PROGRESS_X = 76;
    private static final int PROGRESS_Y = 30;
    private static final int PROGRESS_W = 52;
    private static final int PROGRESS_H = 14;

    private final RecipeType<AlloyEnricherRecipe> recipeType;
    private final IDrawable icon;

    AlloyEnricherRecipeCategory(IGuiHelper guiHelper, RecipeType<AlloyEnricherRecipe> recipeType) {
        this.recipeType = recipeType;
        this.icon = guiHelper.createDrawableItemStack(new ItemStack(AlloyEnricherRegistry.ALLOY_ENRICHER_ITEM.get()));
    }

    @Override public RecipeType<AlloyEnricherRecipe> getRecipeType() { return recipeType; }
    @Override public Component getTitle() { return Component.translatable("block.domesurvival.alloy_enricher"); }
    @Override public int getWidth() { return WIDTH; }
    @Override public int getHeight() { return HEIGHT; }
    @Override public IDrawable getIcon() { return icon; }

    @Override
    public void setRecipe(IRecipeLayoutBuilder builder, AlloyEnricherRecipe recipe, IFocusGroup focuses) {
        List<ItemStack> inputs = new ArrayList<>();
        for (ItemStack candidate : recipe.getIngredient().getItems()) {
            ItemStack stack = candidate.copy();
            stack.setCount(recipe.getInputCount());
            inputs.add(stack);
        }

        builder.addInputSlot(INPUT_X, SLOT_Y)
                .setStandardSlotBackground()
                .addItemStacks(inputs);

        var fluid = recipe.getFluidInput();
        builder.addInputSlot(FLUID_X, FLUID_Y)
                .setFluidRenderer(AlloyEnricherBlockEntity.NEOFLUX_CAPACITY, false, FLUID_W, FLUID_H)
                .addFluidStack(fluid.getFluid(), fluid.getAmount());

        builder.addOutputSlot(OUTPUT_X, SLOT_Y)
                .setOutputSlotBackground()
                .addItemStack(recipe.getResult());
    }

    @Override
    public void draw(AlloyEnricherRecipe recipe, IRecipeSlotsView recipeSlotsView,
                     GuiGraphics graphics, double mouseX, double mouseY) {
        DomeJeiStyle.drawIndustrialPanel(graphics, 0, 0, WIDTH, HEIGHT, DomeJeiStyle.PANEL_FILL);
        DomeJeiStyle.drawThinFrame(graphics, 5, 6, WIDTH - 10, 55, DomeJeiStyle.PANEL_ALT);
        DomeJeiStyle.drawSlot(graphics, INPUT_X, SLOT_Y, false, false);
        DomeJeiStyle.drawSlot(graphics, OUTPUT_X, SLOT_Y, true, false);
        DomeJeiStyle.drawThinFrame(graphics, FLUID_X - 2, FLUID_Y - 2, FLUID_W + 4, FLUID_H + 4, 0xFF151B1E);

        DomeJeiStyle.drawProgress(
                graphics, PROGRESS_X, PROGRESS_Y, PROGRESS_W, PROGRESS_H,
                DomeJeiStyle.animationFraction(recipe.getProcessingTime()),
                DomeJeiStyle.PROCESS, DomeJeiStyle.PROCESS_LIGHT
        );

        DomeJeiStyle.drawCenteredClamped(
                graphics,
                Component.translatable("fluid.domesurvival.neoflux"),
                WIDTH / 2, 68, WIDTH - 16, DomeJeiStyle.TEXT_MUTED
        );
        DomeJeiStyle.drawCenteredClamped(
                graphics,
                Component.translatable("jei.domesurvival.fluid_amount", recipe.getFluidInput().getAmount()),
                WIDTH / 2, 81, WIDTH - 16, DomeJeiStyle.TEXT_DIM
        );
        DomeJeiStyle.drawCenteredClamped(
                graphics,
                Component.translatable("jei.domesurvival.statistics_powered",
                        seconds(recipe.getProcessingTime()), energyPerTick(recipe)),
                WIDTH / 2, 94, WIDTH - 16, DomeJeiStyle.TEXT_DIM
        );
    }

    private static int energyPerTick(AlloyEnricherRecipe recipe) {
        int ticks = Math.max(1, recipe.getProcessingTime());
        long totalEnergy = Math.max(0L, recipe.getEnergy());
        return (int) Math.min(Integer.MAX_VALUE, (totalEnergy + ticks - 1L) / ticks);
    }

    private static String seconds(int ticks) {
        return String.format(Locale.ROOT, "%.1f s", Math.max(0, ticks) / 20.0D);
    }
}
