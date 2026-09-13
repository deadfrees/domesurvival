package com.wasted.domesurvival.forge.compat.jei;

import com.wasted.domesurvival.forge.machine.crusher.IndustrialCrusherRegistry;
import com.wasted.domesurvival.forge.recipe.IndustrialCrusherRecipe;
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

/** JEI view intentionally mirrors the crusher's input -> progress -> two-output workflow. */
public final class IndustrialCrusherJeiCategory implements IRecipeCategory<IndustrialCrusherRecipe> {
    private static final int WIDTH = 154;
    private static final int HEIGHT = 64;

    private final IDrawable icon;

    public IndustrialCrusherJeiCategory(IGuiHelper guiHelper) {
        this.icon = guiHelper.createDrawableItemLike(IndustrialCrusherRegistry.INDUSTRIAL_CRUSHER.get());
    }

    @Override
    public @NotNull mezz.jei.api.recipe.RecipeType<IndustrialCrusherRecipe> getRecipeType() {
        return DomeSurvivalJeiPlugin.INDUSTRIAL_CRUSHING;
    }

    @Override
    public @NotNull Component getTitle() {
        return Component.translatable("jei.domesurvival.industrial_crushing");
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
    public void setRecipe(IRecipeLayoutBuilder builder, IndustrialCrusherRecipe recipe, IFocusGroup focuses) {
        builder.addSlot(RecipeIngredientRole.INPUT, 8, 20)
                .setStandardSlotBackground()
                .addIngredients(recipe.getIngredient());

        builder.addSlot(RecipeIngredientRole.OUTPUT, 92, 20)
                .setOutputSlotBackground()
                .addItemStack(recipe.getResult());

        ItemStack byproduct = recipe.getByproduct();
        if (!byproduct.isEmpty()) {
            builder.addSlot(RecipeIngredientRole.OUTPUT, 126, 20)
                    .setStandardSlotBackground()
                    .addItemStack(byproduct)
                    .addRichTooltipCallback((view, tooltip) -> tooltip.add(
                            Component.translatable(
                                    "jei.domesurvival.industrial_crushing.byproduct_chance",
                                    formatChance(recipe.getByproductChancePerTenThousand())
                            )
                    ));
        }
    }

    @Override
    public void draw(IndustrialCrusherRecipe recipe, IRecipeSlotsView recipeSlotsView,
                     GuiGraphics graphics, double mouseX, double mouseY) {
        graphics.fill(32, 25, 82, 33, 0xFF151A1E);
        graphics.fill(34, 27, 80, 31, 0xFFB96A24);

        var font = Minecraft.getInstance().font;
        graphics.drawString(font,
                Component.translatable("jei.domesurvival.industrial_crushing.energy", recipe.getEnergy()),
                32, 39, 0xFFD8DEE3, false);
        graphics.drawString(font,
                Component.translatable(
                        "jei.domesurvival.industrial_crushing.time",
                        formatSecondsValue(recipe.getProcessingTime())
                ),
                32, 50, 0xFFAEB9C2, false);
    }

    private static String formatChance(int chancePerTenThousand) {
        int whole = chancePerTenThousand / 100;
        int fraction = chancePerTenThousand % 100;
        return fraction == 0 ? whole + "%" : whole + "." + (fraction < 10 ? "0" : "") + fraction + "%";
    }

    private static String formatSecondsValue(int ticks) {
        int safeTicks = Math.max(0, ticks);
        int whole = safeTicks / 20;
        int tenths = (safeTicks % 20) / 2;
        return whole + "." + tenths;
    }
}
