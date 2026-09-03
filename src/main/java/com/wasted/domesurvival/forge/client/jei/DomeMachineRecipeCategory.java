package com.wasted.domesurvival.forge.client.jei;

import mezz.jei.api.gui.builder.IRecipeLayoutBuilder;
import mezz.jei.api.gui.drawable.IDrawable;
import mezz.jei.api.gui.ingredient.IRecipeSlotsView;
import mezz.jei.api.helpers.IGuiHelper;
import mezz.jei.api.recipe.IFocusGroup;
import mezz.jei.api.recipe.RecipeType;
import mezz.jei.api.recipe.category.IRecipeCategory;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.fluids.FluidStack;

import java.util.List;
import java.util.Locale;

final class DomeMachineRecipeCategory implements IRecipeCategory<DomeMachineRecipe> {
    private static final int WIDTH = 180;
    private static final int HEIGHT = 70;
    private static final int SLOT_Y = 17;

    private final RecipeType<DomeMachineRecipe> recipeType;
    private final Component title;
    private final IDrawable background;
    private final IDrawable icon;

    DomeMachineRecipeCategory(IGuiHelper guiHelper, RecipeType<DomeMachineRecipe> recipeType,
                              Component title, ItemStack icon) {
        this.recipeType = recipeType;
        this.title = title;
        this.background = guiHelper.createBlankDrawable(WIDTH, HEIGHT);
        this.icon = guiHelper.createDrawableItemStack(icon);
    }

    @Override
    public RecipeType<DomeMachineRecipe> getRecipeType() {
        return recipeType;
    }

    @Override
    public Component getTitle() {
        return title;
    }

    @Override
    public IDrawable getBackground() {
        return background;
    }

    @Override
    public IDrawable getIcon() {
        return icon;
    }

    @Override
    public void setRecipe(IRecipeLayoutBuilder builder, DomeMachineRecipe recipe, IFocusGroup focuses) {
        SlotLayout positions = SlotLayout.forKind(recipe.layout());
        addItemSlots(builder, recipe.itemInputs(), positions.itemInputs(), false, List.of());
        addFluidSlots(builder, recipe.fluidInputs(), positions.fluidInputs(), false);
        addItemSlots(builder, recipe.itemOutputs(), positions.itemOutputs(), true, recipe.outputNotes());
        addFluidSlots(builder, recipe.fluidOutputs(), positions.fluidOutputs(), true);
    }

    private static void addItemSlots(IRecipeLayoutBuilder builder, List<List<ItemStack>> ingredients,
                                     List<Position> positions, boolean output, List<Component> notes) {
        int count = Math.min(ingredients.size(), positions.size());
        for (int index = 0; index < count; index++) {
            Position position = positions.get(index);
            var slot = output
                    ? builder.addOutputSlot(position.x(), position.y()).setOutputSlotBackground()
                    : builder.addInputSlot(position.x(), position.y()).setStandardSlotBackground();
            slot.addItemStacks(ingredients.get(index));
            if (output && !notes.isEmpty()) {
                slot.addTooltipCallback((view, tooltip) -> tooltip.addAll(notes));
            }
        }
    }

    private static void addFluidSlots(IRecipeLayoutBuilder builder, List<FluidStack> fluids,
                                      List<Position> positions, boolean output) {
        int count = Math.min(fluids.size(), positions.size());
        for (int index = 0; index < count; index++) {
            Position position = positions.get(index);
            FluidStack fluid = fluids.get(index);
            var slot = output
                    ? builder.addOutputSlot(position.x(), position.y()).setOutputSlotBackground()
                    : builder.addInputSlot(position.x(), position.y()).setStandardSlotBackground();
            slot.setFluidRenderer(Math.max(1, fluid.getAmount()), true, 16, 16)
                    .addFluidStack(fluid.getFluid(), fluid.getAmount(), fluid.getTag());
        }
    }

    @Override
    public void draw(DomeMachineRecipe recipe, IRecipeSlotsView recipeSlotsView,
                     GuiGraphics graphics, double mouseX, double mouseY) {
        int arrowStart = switch (recipe.layout()) {
            case BIO_REPAIR -> 119;
            case SAND_SIEVE -> 96;
            default -> 104;
        };
        int arrowEnd = switch (recipe.layout()) {
            case BIO_REPAIR -> 145;
            case SAND_SIEVE -> 119;
            default -> 139;
        };
        graphics.fill(arrowStart, 24, arrowEnd, 27, 0xFF65737A);
        graphics.fill(arrowEnd - 4, 21, arrowEnd + 1, 30, 0xFF65737A);
        graphics.fill(arrowEnd - 1, 23, arrowEnd + 3, 28, 0xFF9AB7C2);

        var font = Minecraft.getInstance().font;
        String note = recipe.note().getString();
        if (!note.isBlank()) {
            graphics.drawCenteredString(font, recipe.note(), WIDTH / 2, 42, 0xFFB8C6CB);
        }

        Component statistics;
        if (recipe.processTicks() > 0 && recipe.energyPerTick() > 0) {
            statistics = Component.translatable(
                    "jei.domesurvival.statistics_powered",
                    seconds(recipe.processTicks()), recipe.energyPerTick()
            );
        } else if (recipe.processTicks() > 0) {
            statistics = Component.translatable("jei.domesurvival.statistics_time", seconds(recipe.processTicks()));
        } else {
            statistics = Component.empty();
        }
        if (!statistics.getString().isBlank()) {
            graphics.drawCenteredString(font, statistics, WIDTH / 2, 57, 0xFF84959C);
        }
    }

    private static String seconds(int ticks) {
        return String.format(Locale.ROOT, "%.1f s", ticks / 20.0D);
    }

    private record Position(int x, int y) { }

    private record SlotLayout(List<Position> itemInputs, List<Position> fluidInputs,
                              List<Position> itemOutputs, List<Position> fluidOutputs) {
        private static final Position I0 = new Position(7, SLOT_Y);
        private static final Position I1 = new Position(31, SLOT_Y);
        private static final Position I2 = new Position(55, SLOT_Y);
        private static final Position I3 = new Position(79, SLOT_Y);
        private static final Position I4 = new Position(97, SLOT_Y);
        private static final Position O0 = new Position(153, SLOT_Y);
        private static final Position O1 = new Position(129, SLOT_Y);

        static SlotLayout forKind(DomeMachineRecipe.Layout kind) {
            return switch (kind) {
                case COKE_OVEN -> new SlotLayout(
                        List.of(new Position(19, SLOT_Y), new Position(49, SLOT_Y)), List.of(),
                        List.of(O0), List.of());
                case SHAFT_FURNACE -> new SlotLayout(
                        List.of(new Position(19, SLOT_Y), new Position(49, SLOT_Y)), List.of(),
                        List.of(O1, O0), List.of());
                case WATER_PURIFIER -> new SlotLayout(
                        List.of(new Position(23, SLOT_Y)), List.of(new Position(53, SLOT_Y)),
                        List.of(), List.of(O0));
                case OXYGEN_ELECTROLYZER -> new SlotLayout(
                        List.of(), List.of(new Position(45, SLOT_Y)), List.of(), List.of());
                case OXYGEN_FILLER -> new SlotLayout(
                        List.of(new Position(43, SLOT_Y)), List.of(), List.of(O0), List.of());
                case BIO_REPAIR -> new SlotLayout(
                        List.of(I0, I1, I2, I3), List.of(I4), List.of(O0), List.of());
                case BIO_INCUBATION -> new SlotLayout(
                        List.of(new Position(25, SLOT_Y), new Position(52, SLOT_Y)),
                        List.of(new Position(79, SLOT_Y)), List.of(O0), List.of());
                case SAND_SIEVE -> new SlotLayout(
                        List.of(new Position(19, SLOT_Y), new Position(45, SLOT_Y)),
                        List.of(new Position(71, SLOT_Y)), List.of(O1, O0), List.of());
            };
        }
    }
}
