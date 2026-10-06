package com.wasted.domesurvival.forge.client.jei;

import com.wasted.domesurvival.forge.fluid.ModFluids;
import com.wasted.domesurvival.forge.machine.organic.OrganicProcessorRegistry;
import com.wasted.domesurvival.forge.recipe.OrganicProcessorRecipe;
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

/**
 * JEI view of the Organic Processor in the same industrial GUI family as the
 * Forming Press, including a real JEI purified-water ingredient.
 */
final class OrganicProcessorRecipeCategory implements IRecipeCategory<OrganicProcessorRecipe> {
    private static final int WIDTH = 180;
    private static final int HEIGHT = 118;

    private static final int PRIMARY_X = 13;
    private static final int PRIMARY_Y = 18;
    private static final int ADDITIVE_X = 13;
    private static final int ADDITIVE_Y = 55;
    private static final int WATER_X = 48;
    private static final int WATER_Y = 37;
    private static final int OUTPUT_X = 147;
    private static final int OUTPUT_Y = 37;

    private static final int PROGRESS_X = 78;
    private static final int PROGRESS_Y = 38;
    private static final int PROGRESS_W = 50;
    private static final int PROGRESS_H = 14;

    private final RecipeType<OrganicProcessorRecipe> recipeType;
    private final IDrawable icon;

    OrganicProcessorRecipeCategory(IGuiHelper guiHelper, RecipeType<OrganicProcessorRecipe> recipeType) {
        this.recipeType = recipeType;
        this.icon = guiHelper.createDrawableItemStack(
                new ItemStack(OrganicProcessorRegistry.ORGANIC_PROCESSOR_ITEM.get())
        );
    }

    @Override
    public RecipeType<OrganicProcessorRecipe> getRecipeType() {
        return recipeType;
    }

    @Override
    public Component getTitle() {
        return Component.translatable(OrganicProcessorRegistry.ORGANIC_PROCESSOR.get().getDescriptionId());
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
    public void setRecipe(IRecipeLayoutBuilder builder, OrganicProcessorRecipe recipe, IFocusGroup focuses) {
        builder.addInputSlot(PRIMARY_X, PRIMARY_Y)
                .setStandardSlotBackground()
                .addItemStacks(withCount(recipe.getPrimary().getItems(), recipe.getPrimaryCount()));

        builder.addInputSlot(ADDITIVE_X, ADDITIVE_Y)
                .setStandardSlotBackground()
                .addItemStacks(withCount(recipe.getAdditive().getItems(), recipe.getAdditiveCount()));

        builder.addInputSlot(WATER_X, WATER_Y)
                .setStandardSlotBackground()
                .setFluidRenderer(Math.max(1, recipe.getWaterMb()), true, 16, 16)
                .addFluidStack(ModFluids.PURIFIED_WATER.get(), recipe.getWaterMb());

        builder.addOutputSlot(OUTPUT_X, OUTPUT_Y)
                .setOutputSlotBackground()
                .addItemStack(recipe.getResult());
    }

    @Override
    public void draw(OrganicProcessorRecipe recipe, IRecipeSlotsView recipeSlotsView,
                     GuiGraphics graphics, double mouseX, double mouseY) {
        DomeJeiStyle.drawIndustrialPanel(graphics, 0, 0, WIDTH, HEIGHT, DomeJeiStyle.PANEL_FILL);
        DomeJeiStyle.drawThinFrame(graphics, 5, 6, WIDTH - 10, 75, DomeJeiStyle.PANEL_ALT);

        DomeJeiStyle.drawSlot(graphics, PRIMARY_X, PRIMARY_Y, false, false);
        DomeJeiStyle.drawSlot(graphics, ADDITIVE_X, ADDITIVE_Y, false, false);
        DomeJeiStyle.drawSlot(graphics, WATER_X, WATER_Y, false, true);
        DomeJeiStyle.drawSlot(graphics, OUTPUT_X, OUTPUT_Y, true, false);

        float progress = DomeJeiStyle.animationFraction(recipe.getProcessingTime());
        DomeJeiStyle.drawProgress(
                graphics, PROGRESS_X, PROGRESS_Y, PROGRESS_W, PROGRESS_H,
                progress, DomeJeiStyle.PROCESS, DomeJeiStyle.PROCESS_LIGHT
        );

        drawOrganicChamber(graphics, progress);

        DomeJeiStyle.drawCenteredClamped(
                graphics,
                Component.literal(recipe.getWaterMb() + " mB"),
                WATER_X + 8,
                62,
                46,
                DomeJeiStyle.TEXT_MUTED
        );

        Component statistics = Component.translatable(
                "jei.domesurvival.statistics_powered",
                seconds(recipe.getProcessingTime()),
                energyPerTick(recipe.getEnergy(), recipe.getProcessingTime())
        );
        DomeJeiStyle.drawCenteredClamped(
                graphics, statistics, WIDTH / 2, 103, WIDTH - 16, DomeJeiStyle.TEXT_DIM
        );
    }

    private static void drawOrganicChamber(GuiGraphics graphics, float progress) {
        int x = 82;
        int y = 58;
        int width = 42;
        int height = 27;

        DomeJeiStyle.drawThinFrame(graphics, x, y, width, height, 0xFF171C20);
        graphics.fill(x + 6, y + 5, x + width - 6, y + height - 5, 0xFF16282A);

        int liquidTop = y + 13;
        graphics.fill(x + 7, liquidTop, x + width - 7, y + height - 6, 0xFF365C59);
        graphics.fill(x + 7, liquidTop, x + width - 7, liquidTop + 2, 0xFF6C9690);

        // Simple animated mixer shaft and two bubbles: distinct from the crusher process.
        int sweep = Math.min(12, Math.max(0, Math.round(progress * 12.0F)));
        int centerX = x + width / 2;
        graphics.fill(centerX - 1, y + 6, centerX + 1, y + 21, DomeJeiStyle.METAL);
        graphics.fill(centerX - 7 + sweep / 2, y + 19, centerX + 7 - sweep / 2, y + 21, DomeJeiStyle.METAL_LIGHT);

        int bubbleY = liquidTop + 7 - Math.min(6, Math.round(progress * 6.0F));
        graphics.fill(x + 11, bubbleY, x + 13, bubbleY + 2, 0xFF91BDB5);
        graphics.fill(x + 29, liquidTop + 3 + Math.min(4, Math.round(progress * 4.0F)),
                x + 31, liquidTop + 5 + Math.min(4, Math.round(progress * 4.0F)), 0xFF91BDB5);
    }

    private static List<ItemStack> withCount(ItemStack[] candidates, int count) {
        List<ItemStack> stacks = new ArrayList<>();
        for (ItemStack candidate : candidates) {
            ItemStack stack = candidate.copy();
            stack.setCount(Math.max(1, count));
            stacks.add(stack);
        }
        if (stacks.isEmpty()) {
            stacks.add(new ItemStack(Items.BARRIER));
        }
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
}
