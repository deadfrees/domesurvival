package com.wasted.domesurvival.forge.machine.forming;

import com.wasted.domesurvival.forge.DomeSurvival;


import com.wasted.domesurvival.forge.machine.side.RelativeSide;
import com.wasted.domesurvival.forge.machine.side.SideMode;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;

import java.util.EnumMap;
import java.util.List;
import java.util.Locale;

/** Graphite instrument panel, retaining the menu's authoritative vanilla slots. */
public final class FormingPressScreen extends AbstractContainerScreen<FormingPressMenu> {
    private static final ResourceLocation PANEL = texture("panel");
    private static final ResourceLocation CONFIGURATION = texture("configuration");
    private static final ResourceLocation MODULES = texture("modules");
    private static final ResourceLocation WIDGETS = texture("widgets");
    private static final String KEY = "gui.domesurvival.forming_press_v2.";
    private static final int TEXT = 0xFFCAD2D4;
    private static final int MUTED = 0xFF98A5AB;
    private static final int BLUE = 0xFF83B8D2;
    private static final int AMBER = 0xFFE0BC7E;
    private static final Rect SETTINGS = new Rect(192, 6, 20, 20);
    private static final Rect UPGRADES = new Rect(168, 6, 20, 20);
    private static final EnumMap<RelativeSide, Rect> SIDES = createSideRects();


    public FormingPressScreen(FormingPressMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
        imageWidth = 220;
        imageHeight = 266;
    }

    private static ResourceLocation texture(String name) {
        return new ResourceLocation(DomeSurvival.MOD_ID, "textures/gui/forming_press_v2/" + name + ".png");
    }

    private static EnumMap<RelativeSide, Rect> createSideRects() {
        EnumMap<RelativeSide, Rect> result = new EnumMap<>(RelativeSide.class);
        result.put(RelativeSide.TOP, new Rect(46, 47, 20, 20));
        result.put(RelativeSide.LEFT, new Rect(22, 71, 20, 20));
        result.put(RelativeSide.FRONT, new Rect(46, 71, 20, 20));
        result.put(RelativeSide.RIGHT, new Rect(70, 71, 20, 20));
        result.put(RelativeSide.BOTTOM, new Rect(46, 95, 20, 20));
        result.put(RelativeSide.BACK, new Rect(70, 95, 20, 20));
        return result;
    }

    // Front-facing diagram: viewer left corresponds to machine right.
    private static RelativeSide machineSide(RelativeSide visual) {
        return switch (visual) {
            case LEFT -> RelativeSide.RIGHT;
            case RIGHT -> RelativeSide.LEFT;
            default -> visual;
        };
    }

    private boolean inside(double mouseX, double mouseY, Rect rect) {
        return rect.contains(mouseX - leftPos, mouseY - topPos);
    }

    private RelativeSide hoveredSide(double mouseX, double mouseY) {
        for (var entry : SIDES.entrySet()) {
            if (inside(mouseX, mouseY, entry.getValue())) return entry.getKey();
        }
        return null;
    }

    @Override public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button == 0 && inside(mouseX,mouseY,SETTINGS)) { setTab(menu.isSidePanelOpen()?FormingPressMenu.MAIN_TAB:FormingPressMenu.SIDE_TAB); return true; }
        if (button == 0 && inside(mouseX,mouseY,UPGRADES)) { setTab(menu.isModulePanelOpen()?FormingPressMenu.MAIN_TAB:FormingPressMenu.MODULE_TAB); return true; }
        if (button == 0 && menu.isSidePanelOpen()) {
            RelativeSide side=hoveredSide(mouseX,mouseY);
            if(side!=null){if(FormingPressBlockEntity.isConfigurableSide(side))send(FormingPressMenu.sideButtonId(machineSide(side)));return true;}
        }
        if(button==0&&menu.isMainPanelOpen())for(FormingOperation operation:FormingOperation.values()) {
            if(inside(mouseX,mouseY,new Rect(24+36*operation.ordinal(),108,30,26))){send(FormingPressMenu.operationButtonId(operation));return true;}
        }
        return super.mouseClicked(mouseX,mouseY,button);
    }
    private void send(int id){if(minecraft!=null&&minecraft.gameMode!=null)minecraft.gameMode.handleInventoryButtonClick(menu.containerId,id);}
    private void setTab(int tab){menu.setTab(tab);send(tab);}

    /** Artwork is baked at 4x GUI resolution. Vanilla alone renders stacks and counts. */
    private void widget(GuiGraphics graphics, int x, int y, int width, int height,
                        int u, int v, int sourceWidth, int sourceHeight) {
        graphics.blit(WIDGETS, leftPos + x, topPos + y, width, height,
                u * 4.0F, v * 4.0F, sourceWidth * 4, sourceHeight * 4, 512, 256);
    }

    @Override
    protected void renderBg(GuiGraphics graphics, float partialTick, int mouseX, int mouseY) {
        graphics.blit(PANEL, leftPos, topPos, 220, 266, 0, 0, 880, 1064, 880, 1064);
        widget(graphics, SETTINGS.x, SETTINGS.y, 20, 20, 0, 0, 20, 20);
        widget(graphics, UPGRADES.x, UPGRADES.y, 20, 20, 88, 0, 20, 20);
        if (inside(mouseX, mouseY, UPGRADES) || menu.isModulePanelOpen()) {
            graphics.renderOutline(leftPos + UPGRADES.x, topPos + UPGRADES.y, 20, 20, AMBER);
        }
        if (inside(mouseX, mouseY, SETTINGS) || menu.isSidePanelOpen()) {
            graphics.renderOutline(leftPos + SETTINGS.x, topPos + SETTINGS.y, 20, 20, BLUE);
        }
        if (menu.isModulePanelOpen()) {
            graphics.blit(MODULES, leftPos + 8, topPos + 25, 204, 111, 0, 0, 816, 444, 816, 444);
        } else if (menu.isSidePanelOpen()) {
            graphics.blit(CONFIGURATION, leftPos + 8, topPos + 25, 204, 111, 0, 0, 816, 444, 816, 444);
            for (var entry : SIDES.entrySet()) {
                RelativeSide visual = entry.getKey();
                Rect rect = entry.getValue();
                SideMode mode = menu.getSideMode(machineSide(visual));
                int tile = mode == SideMode.INPUT ? 20 : mode.allowsOutput() ? 40 : 0;
                widget(graphics, rect.x, rect.y, 20, 20, tile, 24, 20, 20);
                if (visual != RelativeSide.FRONT && inside(mouseX, mouseY, rect)) {
                    graphics.renderOutline(leftPos + rect.x, topPos + rect.y, 20, 20, TEXT);
                }
            }
        } else {
            int energyHeight = (int) Math.min(47, (long) menu.energyStored() * 47 / Math.max(1, menu.energyCapacity()));
            if (energyHeight > 0) widget(graphics, 17, 87 - energyHeight, 12, energyHeight, 64, 47 - energyHeight, 12, energyHeight);
            int width=menu.progressMax()<=0?0:Math.min(85,menu.progress()*85/menu.progressMax());
            if(width>0){graphics.enableScissor(leftPos+82,topPos+76,leftPos+82+width,topPos+84);widget(graphics,82,76,85,8,0,48,64,8);graphics.disableScissor();}
            String[] icons={"steel_plate","steel_gear","steel_rod","steel_wire","steel_tube"};
            for(FormingOperation operation:FormingOperation.values()){
                int x=24+36*operation.ordinal();
                var item=net.minecraftforge.registries.ForgeRegistries.ITEMS.getValue(new ResourceLocation(DomeSurvival.MOD_ID,icons[operation.ordinal()]));
                if(item!=null)graphics.renderItem(new ItemStack(item),leftPos+x+7,topPos+113);
                if(operation==menu.operation())graphics.renderOutline(leftPos+x,topPos+108,30,26,AMBER);
            }
        }
    }

    @Override
    protected void renderLabels(GuiGraphics graphics, int mouseX, int mouseY) {
        text(graphics, title, 13, 10, 146, 0xFF201E19);
        if (menu.isModulePanelOpen()) {
            text(graphics, Component.translatable("gui.domesurvival.upgrade_modules"), 15, 30, 187, TEXT);
            text(graphics, Component.translatable(KEY+"modules_allowed"),59,51,143,AMBER);
            text(graphics, Component.translatable(KEY+"modules_allowed2"),59,64,143,TEXT);
            text(graphics, Component.translatable(KEY+"modules_idle"),59,83,143,MUTED);
            text(graphics, Component.translatable(KEY+"modules_conflict"),59,97,143,MUTED);
        } else if (menu.isSidePanelOpen()) {
            text(graphics, Component.translatable(KEY + "routing_title"), 15, 30, 187, TEXT);
            text(graphics, Component.translatable("gui.domesurvival.side_state.input"), 111, 47, 89, BLUE);
            text(graphics, Component.translatable(KEY + "input_short"), 111, 59, 89, TEXT);
            text(graphics, Component.translatable("gui.domesurvival.side_state.output"), 111, 73, 89, AMBER);
            text(graphics, Component.translatable(KEY + "output_only_short"), 111, 85, 89, TEXT);
            for (var entry : SIDES.entrySet()) {
                Rect rect = entry.getValue();
                String side = entry.getKey().name().toLowerCase(Locale.ROOT);
                Component label = Component.translatable("gui.domesurvival.coal_generator.side_letter." + side);
                graphics.drawCenteredString(font, label, rect.x + 10, rect.y + 5,
                        entry.getKey() == RelativeSide.FRONT ? MUTED : TEXT);
            }
        } else {
            text(graphics, Component.translatable(KEY + "energy_section"), 14, 28, 75, MUTED);
            Component energy = Component.translatable(KEY + "energy_compact_spaced", compact(menu.energyStored()), compact(menu.energyCapacity()));
            graphics.drawCenteredString(font, energy, 125, 42, AMBER);
            text(graphics,Component.translatable(KEY+"raw"),43,57,33,MUTED);
            text(graphics,Component.translatable(KEY+"product"),172,57,35,MUTED);
            String[] states={"ready","forming","no_energy","no_recipe","output_full","not_enough_input"};
            text(graphics,Component.translatable("gui.domesurvival.forming_press.status."+states[Math.max(0,Math.min(5,menu.status()))],menu.inputCount(),menu.requiredInputCount()),42,96,166,TEXT);
        }
        text(graphics,Component.translatable(KEY+"cycle",menu.recipeEnergy(),menu.progressMax()/20.0),14,139,195,MUTED);
        text(graphics,playerInventoryTitle,14,149,194,TEXT);
    }

    private void text(GuiGraphics graphics, Component component, int x, int y, int maxWidth, int color) {
        String value = component.getString();
        if (font.width(value) > maxWidth) value = font.plainSubstrByWidth(value, maxWidth - font.width("...")) + "...";
        graphics.drawString(font, value, x, y, color, false);
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        renderBackground(graphics);
        super.render(graphics, mouseX, mouseY, partialTick);
        renderTooltip(graphics, mouseX, mouseY);
        if (inside(mouseX, mouseY, new Rect(9, 7, 154, 14))) {
            graphics.renderTooltip(font, title, mouseX, mouseY);
        } else if (inside(mouseX, mouseY, UPGRADES)) {
            graphics.renderTooltip(font, Component.translatable("gui.domesurvival.upgrade_modules.tooltip"), mouseX, mouseY);
        } else if (inside(mouseX, mouseY, SETTINGS)) {
            graphics.renderTooltip(font, Component.translatable("gui.domesurvival.side_config"), mouseX, mouseY);
        } else if (menu.isModulePanelOpen()) {
            if ((hoveredSlot == null || !hoveredSlot.hasItem()) && inside(mouseX, mouseY, new Rect(18, 45, 185, 67))) {
                graphics.renderTooltip(font, font.split(Component.translatable(KEY + "module_slot_tooltip"), 220), mouseX, mouseY);
            }
        } else if (menu.isSidePanelOpen()) {
            RelativeSide side = hoveredSide(mouseX, mouseY);
            if (side != null) {
                SideMode mode = menu.getSideMode(machineSide(side));
                Component detail = side == RelativeSide.FRONT ? Component.translatable(KEY + "front_reserved")
                        : Component.translatable(mode == SideMode.INPUT ? KEY + "input_tooltip"
                        : mode.allowsOutput() ? KEY + "output_only_tooltip" : "gui.domesurvival.side_state.disabled");
                graphics.renderComponentTooltip(font, List.of(Component.translatable("gui.domesurvival.side." + side.name().toLowerCase(Locale.ROOT)), detail), mouseX, mouseY, ItemStack.EMPTY);
            }
        } else if (menu.isMainPanelOpen()) {
            for(FormingOperation operation:FormingOperation.values())if(inside(mouseX,mouseY,new Rect(24+36*operation.ordinal(),108,30,26)))
                graphics.renderTooltip(font,Component.translatable("gui.domesurvival.forming_press.operation."+operation.getSerializedName()),mouseX,mouseY);
            if (inside(mouseX,mouseY,new Rect(14,37,18,53)) || inside(mouseX,mouseY,new Rect(42,38,166,15)))

            graphics.renderTooltip(font, Component.translatable(KEY + "energy_tooltip", menu.energyStored(), menu.energyCapacity()), mouseX, mouseY);
        }
    }

    private static String compact(int value) {
        if (value < 1000) return Integer.toString(value);
        return value % 1000 == 0 ? (value / 1000) + "k" : String.format(Locale.ROOT, "%.1fk", value / 1000.0);
    }

    private record Rect(int x, int y, int width, int height) {
        boolean contains(double px, double py) { return px >= x && px < x + width && py >= y && py < y + height; }
    }
}
