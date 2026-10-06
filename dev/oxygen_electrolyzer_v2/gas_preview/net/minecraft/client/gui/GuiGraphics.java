package net.minecraft.client.gui;
public final class GuiGraphics {
    private final java.awt.Graphics2D graphics;
    public GuiGraphics(java.awt.Graphics2D graphics) { this.graphics = graphics; }
    public void fill(int x1, int y1, int x2, int y2, int color) {
        graphics.setColor(new java.awt.Color(color, true));
        graphics.fillRect(x1, y1, x2 - x1, y2 - y1);
    }
}
