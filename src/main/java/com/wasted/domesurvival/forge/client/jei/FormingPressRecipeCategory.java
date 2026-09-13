package com.wasted.domesurvival.forge.client.jei;

import com.wasted.domesurvival.forge.DomeSurvival;
import com.wasted.domesurvival.forge.machine.forming.FormingOperation;
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
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraftforge.registries.ForgeRegistries;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Locale;

/** JEI view of the forming press using the same visual language as the in-game machine GUI. */
final class FormingPressRecipeCategory implements IRecipeCategory<FormingPressRecipe> {
    private static final int WIDTH = 180;
    private static final int HEIGHT = 118;

    private static final int INPUT_X = 17;
    private static final int OUTPUT_X = 147;
    private static final int SLOT_Y = 25;

    private static final int PROGRESS_X = 48;
    private static final int PROGRESS_Y = 26;
    private static final int PROGRESS_W = 80;
    private static final int PROGRESS_H = 14;

    private static final int OPERATION_X = 18;
    private static final int OPERATION_Y = 57;
    private static final int OPERATION_W = 24;
    private static final int OPERATION_H = 24;
    private static final int OPERATION_STEP = 29;

    private final RecipeType<FormingPressRecipe> recipeType;
    private final IDrawable icon;
    private final EnumMap<FormingOperation, ItemStack> operationIcons = new EnumMap<>(FormingOperation.class);

    FormingPressRecipeCategory(IGuiHelper guiHelper, RecipeType<FormingPressRecipe> recipeType) {
        this.recipeType = recipeType;
        this.icon = guiHelper.createDrawableItemStack(new ItemStack(FormingPressRegistry.FORMING_PRESS_ITEM.get()));

        operationIcons.put(FormingOperation.PRESS, stackPrefer("steel_plate", "copper_plate"));
        operationIcons.put(FormingOperation.GEAR, stackPrefer("steel_gear", "copper_gear"));
        operationIcons.put(FormingOperation.ROD, stackPrefer("steel_rod", "copper_rod"));
        operationIcons.put(FormingOperation.WIRE, stackPrefer("steel_wire", "copper_wire"));
        operationIcons.put(FormingOperation.TUBE, stackPrefer("steel_tube", "copper_tube"));
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
                .setStandardSlotBackground()
                .addItemStacks(inputs);
        builder.addOutputSlot(OUTPUT_X, SLOT_Y)
                .setOutputSlotBackground()
                .addItemStack(recipe.getResult());
    }

    @Override
    public void draw(FormingPressRecipe recipe, IRecipeSlotsView recipeSlotsView,
                     GuiGraphics graphics, double mouseX, double mouseY) {
        DomeJeiStyle.drawIndustrialPanel(graphics, 0, 0, WIDTH, HEIGHT, DomeJeiStyle.PANEL_FILL);
        DomeJeiStyle.drawThinFrame(graphics, 5, 6, WIDTH - 10, 44, DomeJeiStyle.PANEL_ALT);
        DomeJeiStyle.drawSlot(graphics, INPUT_X, SLOT_Y, false, false);
        DomeJeiStyle.drawSlot(graphics, OUTPUT_X, SLOT_Y, true, false);

        float progress = DomeJeiStyle.animationFraction(recipe.getProcessingTime());
        DomeJeiStyle.drawProgress(
                graphics, PROGRESS_X, PROGRESS_Y, PROGRESS_W, PROGRESS_H,
                progress, DomeJeiStyle.PROCESS, DomeJeiStyle.PROCESS_LIGHT
        );

        for (FormingOperation operation : FormingOperation.values()) {
            int x = OPERATION_X + operation.ordinal() * OPERATION_STEP;
            boolean active = operation == recipe.getOperation();
            DomeJeiStyle.drawThinFrame(
                    graphics, x, OPERATION_Y, OPERATION_W, OPERATION_H,
                    active ? DomeJeiStyle.ACTIVE : DomeJeiStyle.PANEL_ALT
            );

            ItemStack operationIcon = operationIcons.get(operation);
            if (operationIcon != null && !operationIcon.isEmpty()) {
                graphics.renderItem(operationIcon, x + 4, OPERATION_Y + 4);
            }
            if (active) {
                graphics.fill(x + 3, OPERATION_Y + OPERATION_H - 4,
                        x + OPERATION_W - 3, OPERATION_Y + OPERATION_H - 3,
                        DomeJeiStyle.ENERGY);
            }
        }

        Component chain = Component.translatable(
                "gui.domesurvival.forming_press.chain." + recipe.getOperation().getSerializedName()
        );
        DomeJeiStyle.drawCenteredClamped(
                graphics, chain, WIDTH / 2, 88, WIDTH - 16, DomeJeiStyle.TEXT_MUTED
        );

        Component statistics = Component.translatable(
                "jei.domesurvival.statistics_powered",
                seconds(recipe.getProcessingTime()), energyPerTick(recipe)
        );
        DomeJeiStyle.drawCenteredClamped(
                graphics, statistics, WIDTH / 2, 103, WIDTH - 16, DomeJeiStyle.TEXT_DIM
        );
    }

    private static int energyPerTick(FormingPressRecipe recipe) {
        int ticks = Math.max(1, recipe.getProcessingTime());
        long totalEnergy = Math.max(0L, recipe.getEnergy());
        long average = (totalEnergy + ticks - 1L) / ticks;
        return (int) Math.min(Integer.MAX_VALUE, average);
    }

    private static String seconds(int ticks) {
        return String.format(Locale.ROOT, "%.1f s", Math.max(0, ticks) / 20.0D);
    }

    private static ItemStack stackPrefer(String primary, String fallback) {
        Item item = ForgeRegistries.ITEMS.getValue(
                ResourceLocation.fromNamespaceAndPath(DomeSurvival.MOD_ID, primary)
        );
        if (item != null && item != Items.AIR) {
            return new ItemStack(item);
        }
        return stack(fallback);
    }

    private static ItemStack stack(String path) {
        Item item = ForgeRegistries.ITEMS.getValue(
                ResourceLocation.fromNamespaceAndPath(DomeSurvival.MOD_ID, path)
        );
        return item == null || item == Items.AIR
                ? new ItemStack(Items.BARRIER)
                : new ItemStack(item);
    }
}
