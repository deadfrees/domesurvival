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
    private static final int HEIGHT = 128;

    private static final int PRIMARY_X = 17;
    private static final int PRIMARY_Y = 33;
    private static final int ADDITIVE_X = 17;
    private static final int ADDITIVE_Y = 66;
    private static final int WATER_X = 47;
    private static final int WATER_Y = 35;
    private static final int OUTPUT_X = 147;
    private static final int OUTPUT_Y = 66;

    private static final int PROGRESS_X = 75;
    private static final int PROGRESS_Y = 90;
    private static final int PROGRESS_W = 89;
    private static final int PROGRESS_H = 6;

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

                .addItemStacks(withCount(recipe.getPrimary().getItems(), recipe.getPrimaryCount()));

        builder.addInputSlot(ADDITIVE_X, ADDITIVE_Y)

                .addItemStacks(withCount(recipe.getAdditive().getItems(), recipe.getAdditiveCount()));

        builder.addInputSlot(WATER_X, WATER_Y)

                .setFluidRenderer(Math.max(1, recipe.getWaterMb()), true, 12, 42)
                .addFluidStack(ModFluids.PURIFIED_WATER.get(), recipe.getWaterMb());

        builder.addOutputSlot(OUTPUT_X, OUTPUT_Y)

                .addItemStack(recipe.getResult());
    }

    @Override
    public void draw(OrganicProcessorRecipe recipe, IRecipeSlotsView recipeSlotsView,
                     GuiGraphics graphics, double mouseX, double mouseY) {
        var panel = new net.minecraft.resources.ResourceLocation("domesurvival","textures/gui/organic_processor_v2/jei.png");
        graphics.blit(panel,0,0,180,128,0,0,720,512,720,512);
        DomeJeiStyle.drawCenteredClamped(graphics,getTitle(),90,10,160,DomeJeiStyle.TEXT);
        com.wasted.domesurvival.forge.client.render.OrganicProcessorPreview.draw(graphics,104,57,25,true,0,0);
        int filled=(int)(PROGRESS_W*DomeJeiStyle.animationFraction(recipe.getProcessingTime()));
        if(filled>0){
            // JEI translates the pose; crop UVs instead of using screen-space scissoring.
            graphics.blit(new net.minecraft.resources.ResourceLocation("domesurvival","textures/gui/coal_generator_v2/widgets.png"),PROGRESS_X,PROGRESS_Y,filled,PROGRESS_H,0,192,Math.max(1,256*filled/PROGRESS_W),32,512,256);
        }
        DomeJeiStyle.drawCenteredClamped(graphics,Component.literal(recipe.getEnergy()+" FE · "+seconds(recipe.getProcessingTime())),90,111,158,DomeJeiStyle.TEXT_DIM);
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
