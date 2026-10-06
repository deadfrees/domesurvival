package com.wasted.domesurvival.forge.client.gui;

import net.minecraft.Util;
import net.minecraft.client.gui.GuiGraphics;

/** Detailed, drifting vapour shared by the machine buffer and its JEI preview. */
public final class OxygenGasGauge {
    private static final long START = Util.getMillis();

    private OxygenGasGauge() {}

    public static void draw(GuiGraphics g, int x, int y, int width, int height, int amount, int capacity) {
        if (amount <= 0 || capacity <= 0 || width <= 0 || height <= 0) return;
        double filled = height * Math.min(1.0, (double) amount / capacity);
        double top = height - filled;
        double time = (Util.getMillis() - START) / 1000.0;
        // The level remains quantitative; only the vapour inside it drifts.
        for (int row = Math.max(0, (int) top); row < height; row++) {
            // Fractional coverage only at the measured level, never a faded rim.
            double coverage = Math.min(1.0, row + 1.0 - top);
            for (int col = 0; col < width; col++) {
                double rise = row + time * 2.4;
                double bend = (noise(col / 5.0 + 17, rise / 6.0) - .5) * 3.0;
                double broad = noise((col + bend) / 4.0, rise / 5.0);
                double curls = noise((col + bend) / 2.0 + 31, rise / 2.7 - time * .08);
                double detail = noise(col / 1.2 + 73, rise / 1.6);
                double cloud = Math.max(0, Math.min(1, (broad * .50 + curls * .35 + detail * .15 - .24) * 1.9));
                // A continuous body keeps even the darker eddies visibly filled.
                int alpha = (int) ((190 + cloud * 55) * coverage);
                int shade = (int) (111 + cloud * 108);
                int color = (alpha << 24) | (shade << 16) | ((shade + 3) << 8) | (shade + 5);
                g.fill(x + col, y + row, x + col + 1, y + row + 1, color);
            }
        }
    }

    private static double noise(double x, double y) {
        int ix = (int) Math.floor(x), iy = (int) Math.floor(y);
        double u = smooth(x - ix), v = smooth(y - iy);
        return mix(mix(sample(ix, iy), sample(ix + 1, iy), u),
                mix(sample(ix, iy + 1), sample(ix + 1, iy + 1), u), v);
    }

    private static double smooth(double value) {
        return value * value * value * (value * (value * 6 - 15) + 10);
    }

    private static double mix(double a, double b, double t) { return a + (b - a) * t; }

    private static double sample(int x, int y) {
        int hash = x * 374761393 + y * 668265263;
        hash = (hash ^ (hash >>> 13)) * 1274126177;
        return ((hash ^ (hash >>> 16)) & 0xffff) / 65535.0;
    }
}
