import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Font;
import java.awt.GradientPaint;
import java.awt.Graphics2D;
import java.awt.RadialGradientPaint;
import java.awt.RenderingHints;
import java.awt.font.TextAttribute;
import java.awt.geom.Path2D;
import java.awt.image.BufferedImage;
import java.io.File;
import java.util.Map;
import javax.imageio.ImageIO;

/**
 * Renders the launcher artwork:
 * - banner.png: 16:9 Android TV banner (640x360 px = 320x180 dp at xhdpi), used by Google TV launchers.
 * - ic_launcher.png: 512x512 icon with a transparent background. Fire TV draws sideloaded apps' icons
 *   in a square centered on a gray 16:9 tile (and squeezes non-square ones), so only the lockup shows.
 *
 * Java2D cannot use variable fonts, so pass a static Space Grotesk Bold
 * (github.com/floriankarsten/space-grotesk, fonts/ttf/static/SpaceGrotesk-Bold.ttf):
 *
 *   java tools/RenderBranding.java SpaceGrotesk-Bold.ttf \
 *       app/src/main/res/drawable-xhdpi/banner.png app/src/main/res/drawable-nodpi/ic_launcher.png
 */
public class RenderBranding {
    private static final int WIDTH = 640;
    private static final int HEIGHT = 360;
    private static final int ICON = 512;
    private static final Color TEAL = new Color(0x2FD8B4);
    private static final Color VIOLET = new Color(0x8C7CF0);

    // Same heartbeat as the in-app PulseGlyph, in a unit box.
    private static final float[][] HEARTBEAT = {
        {0.04f, 0.56f}, {0.30f, 0.56f}, {0.40f, 0.26f}, {0.52f, 0.90f}, {0.64f, 0.10f}, {0.73f, 0.56f}, {0.96f, 0.56f},
    };

    public static void main(String[] args) throws Exception {
        Font bold = Font.createFont(Font.TRUETYPE_FONT, new File(args[0]));
        write(banner(bold), args[1]);
        write(icon(bold), args[2]);
    }

    private static BufferedImage banner(Font bold) {
        BufferedImage image = new BufferedImage(WIDTH, HEIGHT, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = graphics(image);
        g.setPaint(new GradientPaint(0, 0, new Color(0x151C33), WIDTH, HEIGHT, new Color(0x080A10)));
        g.fillRect(0, 0, WIDTH, HEIGHT);
        glow(g, 110, 30, 340, TEAL, 0.24f);
        glow(g, 580, 340, 320, VIOLET, 0.22f);
        lockup(g, bold, WIDTH, HEIGHT, 120f, 150f, 108f, 14f, 30f);
        g.dispose();
        return image;
    }

    /** Transparent square with the lockup filling its width, as Fire TV shows sideloaded icons on gray. */
    private static BufferedImage icon(Font bold) {
        BufferedImage image = new BufferedImage(ICON, ICON, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = graphics(image);
        lockup(g, bold, ICON, ICON, 118f, 146f, 106f, 15f, 26f);
        g.dispose();
        return image;
    }

    private static Graphics2D graphics(BufferedImage image) {
        Graphics2D g = image.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        g.setRenderingHint(RenderingHints.KEY_STROKE_CONTROL, RenderingHints.VALUE_STROKE_PURE);
        g.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
        return g;
    }

    /** The heartbeat glyph followed by the "Pulse" wordmark, centered in a width x height canvas. */
    private static void lockup(
        Graphics2D g, Font bold, int width, int height,
        float textSize, float glyphWidth, float glyphHeight, float stroke, float gap
    ) {
        Font wordmark = bold.deriveFont(Map.of(TextAttribute.SIZE, textSize, TextAttribute.TRACKING, -0.02f));
        g.setFont(wordmark);
        var metrics = g.getFontMetrics();
        int textWidth = metrics.stringWidth("Pulse");
        float left = (width - (glyphWidth + gap + textWidth)) / 2f;

        Path2D.Float heartbeat = new Path2D.Float();
        float glyphTop = (height - glyphHeight) / 2f;
        for (int i = 0; i < HEARTBEAT.length; i++) {
            float x = left + HEARTBEAT[i][0] * glyphWidth;
            float y = glyphTop + HEARTBEAT[i][1] * glyphHeight;
            if (i == 0) heartbeat.moveTo(x, y); else heartbeat.lineTo(x, y);
        }
        g.setPaint(new GradientPaint(left, 0, TEAL, left + glyphWidth, 0, VIOLET));
        g.setStroke(new BasicStroke(stroke, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        g.draw(heartbeat);

        g.setColor(new Color(0xF2F3F7));
        int baseline = (height - metrics.getHeight()) / 2 + metrics.getAscent();
        g.drawString("Pulse", left + glyphWidth + gap, baseline);
    }

    private static void glow(Graphics2D g, float x, float y, float radius, Color color, float alpha) {
        Color center = new Color(color.getRed(), color.getGreen(), color.getBlue(), Math.round(alpha * 255));
        Color edge = new Color(color.getRed(), color.getGreen(), color.getBlue(), 0);
        g.setPaint(new RadialGradientPaint(x, y, radius, new float[] {0f, 1f}, new Color[] {center, edge}));
        g.fillOval(Math.round(x - radius), Math.round(y - radius), Math.round(radius * 2), Math.round(radius * 2));
    }

    private static void write(BufferedImage image, String path) throws Exception {
        File out = new File(path);
        out.getParentFile().mkdirs();
        ImageIO.write(image, "png", out);
    }
}
