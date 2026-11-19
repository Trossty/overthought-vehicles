import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;

/**
 * Generates the spray can's two 16x16 item layers: the bare tin, and a
 * greyscale cap and label that the dye tint colours with the paint inside.
 * Same approach as GenJerryCan.
 */
public class GenSprayCan {

    static final int OUTLINE = 0xFF1E1E22;
    static final int TIN = 0xFFB8BCC2;
    static final int TIN_LIGHT = 0xFFE4E7EA;
    static final int TIN_DARK = 0xFF7D828A;
    static final int NOZZLE = 0xFF3A3A3F;

    static BufferedImage img;

    static void px(int x, int y, int c) {
        if (x >= 0 && y >= 0 && x < 16 && y < 16) {
            img.setRGB(x, y, c);
        }
    }

    static void rect(int x, int y, int w, int h, int c) {
        for (int i = x; i < x + w; i++) {
            for (int j = y; j < y + h; j++) {
                px(i, j, c);
            }
        }
    }

    /** One shaded column band, light on the left and dark on the right. */
    static void shaded(int y, int h, int light, int mid, int dark) {
        rect(5, y, 1, h, light);
        rect(6, y, 4, h, mid);
        rect(10, y, 1, h, dark);
    }

    public static void main(String[] args) throws Exception {
        String out = args.length > 0 ? args[0] : "src/main/resources/assets/overthought_vehicles/textures/item";

        // Layer 0: the tin, with its outline, rim and the nozzle.
        img = new BufferedImage(16, 16, BufferedImage.TYPE_INT_ARGB);
        rect(4, 5, 8, 11, OUTLINE);
        shaded(6, 9, TIN_LIGHT, TIN, TIN_DARK);
        rect(5, 14, 6, 1, TIN_DARK);
        // Shoulder and the rim the cap sits on.
        rect(5, 4, 6, 1, OUTLINE);
        rect(6, 5, 4, 1, TIN_LIGHT);
        // Cap outline (its colour is layer 1).
        rect(5, 1, 6, 4, OUTLINE);
        // Nozzle sticking out of the cap.
        rect(9, 0, 3, 1, OUTLINE);
        px(11, 1, NOZZLE);
        px(12, 1, OUTLINE);
        ImageIO.write(img, "png", new File(out, "spray_can.png"));

        // Layer 1: cap and label in greys, so the tint gives three tones.
        img = new BufferedImage(16, 16, BufferedImage.TYPE_INT_ARGB);
        rect(6, 2, 4, 2, 0xFFE6E6E6);
        px(6, 2, 0xFFFFFFFF);
        px(9, 3, 0xFFB0B0B0);
        shaded(8, 5, 0xFFFFFFFF, 0xFFDCDCDC, 0xFFA8A8A8);
        // A drip of paint down the label.
        px(7, 13, 0xFFDCDCDC);
        ImageIO.write(img, "png", new File(out, "spray_can_paint.png"));
    }
}
