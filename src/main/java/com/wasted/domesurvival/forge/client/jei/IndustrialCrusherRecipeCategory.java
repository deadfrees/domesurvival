package com.wasted.domesurvival.forge.client.jei;

import com.wasted.domesurvival.forge.machine.crusher.IndustrialCrusherRegistry;
import com.wasted.domesurvival.forge.recipe.IndustrialCrusherRecipe;
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

/** Single data-driven JEI category for the Industrial Crusher. */
final class IndustrialCrusherRecipeCategory implements IRecipeCategory<IndustrialCrusherRecipe> {
    private static final int WIDTH = 180;
    private static final int HEIGHT = 142;

    private static final int INPUT_X = 17;
    private static final int PRIMARY_OUTPUT_X = 147;
    private static final int PRIMARY_Y = 25;
    private static final int BYPRODUCT_X = 147;
    private static final int BYPRODUCT_Y = 58;

    private static final int PROGRESS_X = 48;
    private static final int PROGRESS_Y = 26;
    private static final int PROGRESS_W = 80;
    private static final int PROGRESS_H = 14;

    private static final int GAS_X = 17;
    private static final int GAS_Y = 105;
    private static final int GAS_W = 146;
    private static final int GAS_H = 10;

    private final RecipeType<IndustrialCrusherRecipe> recipeType;
    private final IDrawable icon;

    IndustrialCrusherRecipeCategory(IGuiHelper guiHelper, RecipeType<IndustrialCrusherRecipe> recipeType) {
        this.recipeType = recipeType;
        this.icon = guiHelper.createDrawableItemStack(new ItemStack(IndustrialCrusherRegistry.INDUSTRIAL_CRUSHER_ITEM.get()));
    }

    @Override public RecipeType<IndustrialCrusherRecipe> getRecipeType() { return recipeType; }
    @Override public Component getTitle() { return Component.translatable(IndustrialCrusherRegistry.INDUSTRIAL_CRUSHER.get().getDescriptionId()); }
    @Override public int getWidth() { return WIDTH; }
    @Override public int getHeight() { return HEIGHT; }
    @Override public IDrawable getIcon() { return icon; }

    @Override
    public void setRecipe(IRecipeLayoutBuilder builder, IndustrialCrusherRecipe recipe, IFocusGroup focuses) {
        builder.addInputSlot(INPUT_X, PRIMARY_Y)
                .setStandardSlotBackground()
                .addItemStacks(withCount(recipe.getIngredient().getItems(), recipe.getInputCount()));

        ItemStack result = recipe.getResult();
        if (!result.isEmpty()) {
            builder.addOutputSlot(PRIMARY_OUTPUT_X, PRIMARY_Y)
                    .setOutputSlotBackground()
                    .addItemStack(result);
        }

        ItemStack byproduct = recipe.getByproduct();
        if (!byproduct.isEmpty() && recipe.getByproductChancePerTenThousand() > 0) {
            builder.addOutputSlot(BYPRODUCT_X, BYPRODUCT_Y)
                    .setOutputSlotBackground()
                    .addItemStack(byproduct);
        }
    }

    @Override
    public void draw(IndustrialCrusherRecipe recipe, IRecipeSlotsView recipeSlotsView,
                     GuiGraphics graphics, double mouseX, double mouseY) {
        DomeJeiStyle.drawIndustrialPanel(graphics, 0, 0, WIDTH, HEIGHT, DomeJeiStyle.PANEL_FILL);
        DomeJeiStyle.drawThinFrame(graphics, 5, 6, WIDTH - 10, 44, DomeJeiStyle.PANEL_ALT);

        DomeJeiStyle.drawSlot(graphics, INPUT_X, PRIMARY_Y, false, false);
        if (!recipe.getResult().isEmpty()) DomeJeiStyle.drawSlot(graphics, PRIMARY_OUTPUT_X, PRIMARY_Y, true, false);

        float progress = DomeJeiStyle.animationFraction(recipe.getProcessingTime());
        DomeJeiStyle.drawProgress(graphics, PROGRESS_X, PROGRESS_Y, PROGRESS_W, PROGRESS_H,
                progress, DomeJeiStyle.PROCESS, DomeJeiStyle.PROCESS_LIGHT);
        drawCrusherProcess(graphics, progress);

        ItemStack byproduct = recipe.getByproduct();
        if (!byproduct.isEmpty() && recipe.getByproductChancePerTenThousand() > 0) {
            DomeJeiStyle.drawSlot(graphics, BYPRODUCT_X, BYPRODUCT_Y, true, false);
            DomeJeiStyle.drawCenteredClamped(
                    graphics,
                    Component.literal(chanceText(recipe.getByproductChancePerTenThousand())),
                    BYPRODUCT_X + 8, 83, 42, DomeJeiStyle.TEXT_MUTED);
        }

        if (recipe.hasGasResult()) {
            drawGasOutput(graphics, recipe);
        }

        Component statistics = Component.translatable(
                "jei.domesurvival.statistics_powered",
                seconds(recipe.getProcessingTime()),
                energyPerTick(recipe.getEnergy(), recipe.getProcessingTime()));
        DomeJeiStyle.drawCenteredClamped(graphics, statistics, WIDTH / 2, 130, WIDTH - 16, DomeJeiStyle.TEXT_DIM);
    }

    private static void drawGasOutput(GuiGraphics graphics, IndustrialCrusherRecipe recipe) {
        DomeJeiStyle.drawThinFrame(graphics, GAS_X, GAS_Y, GAS_W, GAS_H, 0xFF151B1E);
        graphics.fill(GAS_X + 2, GAS_Y + 2, GAS_X + GAS_W - 2, GAS_Y + GAS_H - 2, 0xFF5E9CA8);

        DomeJeiStyle.drawCenteredClamped(
                graphics,
                Component.translatable("gas.domesurvival.mineral_gas"),
                58, 93, 80, DomeJeiStyle.TEXT_MUTED);
        DomeJeiStyle.drawCenteredClamped(
                graphics,
                Component.translatable("jei.domesurvival.gas_amount", recipe.getGasAmount()),
                132, 93, 54, DomeJeiStyle.TEXT);
    }

    private static void drawCrusherProcess(GuiGraphics graphics, float progress) {
        int x = 54;
        int y = 57;
        int width = 76;
        int height = 32;

        DomeJeiStyle.drawThinFrame(graphics, x, y, width, height, 0xFF171C20);
        graphics.fill(x + 5, y + 5, x + 18, y + 9, DomeJeiStyle.METAL);
        graphics.fill(x + 8, y + 9, x + 15, y + 14, 0xFF30383D);

        int rollerY = y + 11;
        graphics.fill(x + 25, rollerY, x + 38, rollerY + 13, 0xFF232A2E);
        graphics.fill(x + 40, rollerY, x + 53, rollerY + 13, 0xFF232A2E);
        graphics.fill(x + 27, rollerY + 2, x + 36, rollerY + 11, DomeJeiStyle.METAL);
        graphics.fill(x + 42, rollerY + 2, x + 51, rollerY + 11, DomeJeiStyle.METAL);

        int phase = Math.min(6, Math.max(0, Math.round(progress * 6.0F)));
        graphics.fill(x + 28 + phase, rollerY + 3, x + 30 + phase, rollerY + 10, DomeJeiStyle.METAL_LIGHT);
        graphics.fill(x + 48 - phase, rollerY + 3, x + 50 - phase, rollerY + 10, DomeJeiStyle.METAL_LIGHT);

        graphics.fill(x + 57, y + 20, x + 70, y + 24, DomeJeiStyle.METAL);
        graphics.fill(x + 61, y + 24, x + 68, y + 27, 0xFF30383D);
    }

    private static List<ItemStack> withCount(ItemStack[] candidates, int count) {
        List<ItemStack> stacks = new ArrayList<>();
        for (ItemStack candidate : candidates) {
            ItemStack stack = candidate.copy();
            stack.setCount(Math.max(1, count));
            stacks.add(stack);
        }
        if (stacks.isEmpty()) stacks.add(new ItemStack(Items.BARRIER));
        return stacks;
    }

    private static int energyPerTick(int totalEnergy, int ticks) {
        int safeTicks = Math.max(1, ticks);
        long safeEnergy = Math.max(0L, totalEnergy);
        long average = (safeEnergy + safeTicks - 1L) / safeTicks;
        return (int) Math.min(Integer.MAX_VALUE, average);
    }

    private static String seconds(int ticks) {
        return String.format(Locale.ROOT, "%.1f s", Math.max(0, ticks) / 20.0D);
    }

    private static String chanceText(int chancePerTenThousand) {
        double percent = Math.max(0, Math.min(10_000, chancePerTenThousand)) / 100.0D;
        if (Math.abs(percent - Math.rint(percent)) < 0.0001D) return String.format(Locale.ROOT, "%.0f%%", percent);
        return String.format(Locale.ROOT, "%.1f%%", percent);
    }
}
