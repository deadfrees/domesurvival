package com.wasted.domesurvival.forge.client.jei;

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
import net.minecraftforge.fluids.FluidStack;

import java.util.List;
import java.util.Locale;

final class DomeMachineRecipeCategory implements IRecipeCategory<DomeMachineRecipe> {
    private static final int WIDTH = 180;
    private static final int HEIGHT = 98;

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
                    ? builder.addOutputSlot(position.x(), position.y())
                    : builder.addInputSlot(position.x(), position.y());
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
                    ? builder.addOutputSlot(position.x(), position.y())
                    : builder.addInputSlot(position.x(), position.y());
            slot.setFluidRenderer(Math.max(1, fluid.getAmount()), true, 16, 16)
                    .addFluidStack(fluid.getFluid(), fluid.getAmount(), fluid.getTag());
        }
    }

    @Override
    public void draw(DomeMachineRecipe recipe, IRecipeSlotsView recipeSlotsView,
                     GuiGraphics graphics, double mouseX, double mouseY) {
        SlotLayout positions = SlotLayout.forKind(recipe.layout());

        DomeJeiStyle.drawIndustrialPanel(graphics, 0, 0, WIDTH, HEIGHT, DomeJeiStyle.PANEL_FILL);
        DomeJeiStyle.drawThinFrame(graphics, 5, 6, WIDTH - 10, 55, DomeJeiStyle.PANEL_ALT);

        drawMachineGraphic(graphics, recipe);
        drawSlotFrames(graphics, positions.itemInputs(), recipe.itemInputs().size(), false, false);
        drawSlotFrames(graphics, positions.fluidInputs(), recipe.fluidInputs().size(), false, true);
        drawSlotFrames(graphics, positions.itemOutputs(), recipe.itemOutputs().size(), true, false);
        drawSlotFrames(graphics, positions.fluidOutputs(), recipe.fluidOutputs().size(), true, true);

        if (!recipe.note().getString().isBlank()) {
            DomeJeiStyle.drawCenteredClamped(
                    graphics, recipe.note(), WIDTH / 2, 67, WIDTH - 16, DomeJeiStyle.TEXT_MUTED
            );
        }

        Component statistics = statistics(recipe);
        if (!statistics.getString().isBlank()) {
            DomeJeiStyle.drawCenteredClamped(
                    graphics, statistics, WIDTH / 2, 83, WIDTH - 16, DomeJeiStyle.TEXT_DIM
            );
        }
    }

    private static void drawSlotFrames(GuiGraphics graphics, List<Position> positions, int ingredientCount,
                                       boolean output, boolean fluid) {
        int count = Math.min(positions.size(), ingredientCount);
        for (int index = 0; index < count; index++) {
            Position position = positions.get(index);
            DomeJeiStyle.drawSlot(graphics, position.x(), position.y(), output, fluid);
        }
    }

    private static void drawMachineGraphic(GuiGraphics graphics, DomeMachineRecipe recipe) {
        float progress = DomeJeiStyle.animationFraction(recipe.processTicks());

        switch (recipe.layout()) {
            case COKE_OVEN -> {
                DomeJeiStyle.drawThinFrame(graphics, 78, 13, 48, 39, 0xFF171C20);
                graphics.fill(84, 20, 120, 45, 0xFF111719);
                graphics.fill(91, 31, 113, 44, 0xFF4B2D20);
                graphics.fill(95, 35, 109, 44, 0xFF744128);
                DomeJeiStyle.drawArrow(graphics, 126, 145, 32);
            }
            case SHAFT_FURNACE -> {
                DomeJeiStyle.drawThinFrame(graphics, 78, 10, 34, 45, 0xFF171C20);
                graphics.fill(85, 16, 105, 50, 0xFF111719);
                int hotHeight = 8 + Math.round(20 * progress);
                graphics.fill(89, 50 - hotHeight, 101, 50, 0xFF744128);
                graphics.fill(91, 50 - hotHeight, 99, Math.min(50, 53 - hotHeight), 0xFF925034);
                DomeJeiStyle.drawArrow(graphics, 112, 120, 32);
            }
            case WATER_PURIFIER -> {
                DomeJeiStyle.drawThinFrame(graphics, 80, 13, 48, 39, 0xFF171C20);
                graphics.fill(87, 19, 98, 46, 0xFF3D5963);
                graphics.fill(101, 19, 108, 46, 0xFF686558);
                graphics.fill(111, 19, 121, 46, 0xFF4B7888);
                DomeJeiStyle.drawArrow(graphics, 128, 145, 32);
            }
            case OXYGEN_ELECTROLYZER -> {
                DomeJeiStyle.drawThinFrame(graphics, 66, 12, 86, 41, 0xFF171C20);
                graphics.fill(74, 19, 101, 46, 0xFF24424C);
                graphics.fill(105, 19, 144, 46, 0xFF16272D);
                int bubble = 21 + Math.round(17 * progress);
                graphics.fill(116, bubble, 119, bubble + 3, 0xFF8FBBC8);
                int bubble2 = 42 - Math.round(13 * progress);
                graphics.fill(129, bubble2, 132, bubble2 + 3, 0xFF8FBBC8);
            }
            case OXYGEN_FILLER -> {
                DomeJeiStyle.drawThinFrame(graphics, 69, 14, 63, 37, 0xFF171C20);
                graphics.fill(81, 19, 91, 46, 0xFF4B5359);
                graphics.fill(84, 22, 88, 43, 0xFF6E7B80);
                graphics.fill(91, 27, 118, 30, 0xFF65737A);
                graphics.fill(116, 27, 121, 38, 0xFF9AB7C2);
                DomeJeiStyle.drawArrow(graphics, 132, 145, 32);
            }
            case BIO_REPAIR -> {
                DomeJeiStyle.drawThinFrame(graphics, 133, 11, 16, 44, 0xFF171C20);
                graphics.fill(137, 16, 145, 50, 0xFF31545C);
                int pulseY = 47 - Math.round(25 * progress);
                graphics.fill(138, pulseY, 144, pulseY + 3, 0xFF84AEB8);
                DomeJeiStyle.drawArrow(graphics, 148, 151, 34);
            }
            case BIO_INCUBATION -> {
                DomeJeiStyle.drawThinFrame(graphics, 104, 11, 34, 44, 0xFF171C20);
                graphics.fill(111, 17, 131, 48, 0xFF27474F);
                graphics.fill(115, 30, 127, 44, 0xFF5F7E77);
                DomeJeiStyle.drawArrow(graphics, 138, 145, 32);
            }
            case SAND_SIEVE -> {
                DomeJeiStyle.drawThinFrame(graphics, 96, 14, 23, 36, 0xFF171C20);
                for (int x = 100; x <= 114; x += 4) {
                    graphics.fill(x, 19, x + 1, 45, 0xFF697278);
                }
                for (int y = 21; y <= 43; y += 5) {
                    graphics.fill(100, y, 115, y + 1, 0xFF4B5359);
                }
                DomeJeiStyle.drawArrow(graphics, 119, 122, 32);
            }
        }
    }

    private static Component statistics(DomeMachineRecipe recipe) {
        if (recipe.processTicks() > 0 && recipe.energyPerTick() > 0) {
            return Component.translatable(
                    "jei.domesurvival.statistics_powered",
                    seconds(recipe.processTicks()), recipe.energyPerTick()
            );
        }
        if (recipe.processTicks() > 0) {
            return Component.translatable("jei.domesurvival.statistics_time", seconds(recipe.processTicks()));
        }
        return Component.empty();
    }

    private static String seconds(int ticks) {
        return String.format(Locale.ROOT, "%.1f s", ticks / 20.0D);
    }

    private record Position(int x, int y) { }

    private record SlotLayout(List<Position> itemInputs, List<Position> fluidInputs,
                              List<Position> itemOutputs, List<Position> fluidOutputs) {
        private static final int SLOT_Y = 24;
        private static final Position O0 = new Position(156, SLOT_Y);
        private static final Position O1 = new Position(130, SLOT_Y);

        static SlotLayout forKind(DomeMachineRecipe.Layout kind) {
            return switch (kind) {
                case COKE_OVEN -> new SlotLayout(
                        List.of(new Position(18, SLOT_Y), new Position(47, SLOT_Y)), List.of(),
                        List.of(O0), List.of());
                case SHAFT_FURNACE -> new SlotLayout(
                        List.of(new Position(18, SLOT_Y), new Position(47, SLOT_Y)), List.of(),
                        List.of(O1, O0), List.of());
                case WATER_PURIFIER -> new SlotLayout(
                        List.of(new Position(18, SLOT_Y)), List.of(new Position(47, SLOT_Y)),
                        List.of(), List.of(O0));
                case OXYGEN_ELECTROLYZER -> new SlotLayout(
                        List.of(), List.of(new Position(28, SLOT_Y)), List.of(), List.of());
                case OXYGEN_FILLER -> new SlotLayout(
                        List.of(new Position(28, SLOT_Y)), List.of(), List.of(O0), List.of());
                case BIO_REPAIR -> new SlotLayout(
                        List.of(new Position(8, 16), new Position(34, 16), new Position(60, 16), new Position(86, 16)),
                        List.of(new Position(112, 16)), List.of(new Position(156, 31)), List.of());
                case BIO_INCUBATION -> new SlotLayout(
                        List.of(new Position(18, SLOT_Y), new Position(47, SLOT_Y)),
                        List.of(new Position(76, SLOT_Y)), List.of(O0), List.of());
                case SAND_SIEVE -> new SlotLayout(
                        List.of(new Position(18, SLOT_Y), new Position(47, SLOT_Y)),
                        List.of(new Position(76, SLOT_Y)), List.of(O1, O0), List.of());
                default -> new SlotLayout(                         List.of(new Position(18, SLOT_Y), new Position(47, SLOT_Y)),                         List.of(new Position(76, SLOT_Y)),                         List.of(O1, O0),                         List.of());
            };
        }
    }
}
