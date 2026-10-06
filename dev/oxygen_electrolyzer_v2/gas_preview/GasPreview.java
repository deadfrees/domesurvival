import com.wasted.domesurvival.forge.client.gui.OxygenGasGauge;
import net.minecraft.Util;
import net.minecraft.client.gui.GuiGraphics;
import java.awt.*;
import java.awt.image.BufferedImage;
import javax.imageio.ImageIO;
import java.io.File;

/** Render the production gauge at fixed times without starting a second game client. */
public class GasPreview {
    public static void main(String[] args) throws Exception {
        BufferedImage image = new BufferedImage(600, 290, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = image.createGraphics();
        g.setColor(new Color(28, 33, 35)); g.fillRect(0, 0, 600, 290);
        g.setColor(new Color(205, 212, 213)); g.drawString("Oxygen buffer / 0 s, 1 s, 2 s / 25%, 100%", 18, 22);
        for (int frame = 0; frame < 3; frame++) {
            Util.millis = frame * 1000;
            for (int level = 0; level < 2; level++) {
                Graphics2D tile = (Graphics2D) g.create();
                tile.translate(20 + frame * 198 + level * 88, 52); tile.scale(3, 4);
                tile.setColor(new Color(76, 88, 93)); tile.fillRect(-2, -2, 28, 56);
                tile.setColor(new Color(30, 38, 41)); tile.fillRect(0, 0, 24, 52);
                OxygenGasGauge.draw(new GuiGraphics(tile), 0, 0, 24, 52, level == 0 ? 1000 : 4000, 4000);
                tile.dispose();
            }
        }
        g.dispose();
        ImageIO.write(image, "png", new File(args[0]));
    }
}
