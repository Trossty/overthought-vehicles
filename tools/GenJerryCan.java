import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;

/**
 * Generates the jerry can's 16x16 item texture, so its colours can be changed
 * here instead of in an image editor. Same approach as GenCarTextures.
 */
public class GenJerryCan {

    static final int OUTLINE = 0xFF2E1210;
    static final int BODY = 0xFFB3261E;
    static final int BODY_LIGHT = 0xFFD9453C;
    static final int BODY_DARK = 0xFF7A1B16;
    static final int METAL = 0xFF9E9E9E;
    static final int METAL_DARK = 0xFF565656;

    static BufferedImage img;

    static void px(int x, int y, int c) {
        if (x >= 0 && y >= 0 && x < img.getWidth() && y < img.getHeight()) {
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

    public static void main(String[] args) throws Exception {
        img = new BufferedImage(16, 16, BufferedImage.TYPE_INT_ARGB);

        // Body: outline first, then fill one pixel inside it.
        rect(3, 4, 10, 11, OUTLINE);
        rect(4, 5, 8, 9, BODY);

        // Left edge catches the light, right edge falls into shadow.
        rect(4, 5, 1, 9, BODY_LIGHT);
        rect(11, 5, 1, 9, BODY_DARK);
        rect(4, 13, 8, 1, BODY_DARK);

        // The X brace a real jerry can has, drawn as two short diagonals so it
        // still reads at 16px.
        for (int i = 0; i < 5; i++) {
            px(5 + i, 7 + i, BODY_DARK);
            px(10 - i, 7 + i, BODY_DARK);
        }

        // Handle across the top, raised above the body.
        rect(5, 2, 7, 1, OUTLINE);
        rect(5, 3, 1, 1, OUTLINE);
        rect(11, 3, 1, 1, OUTLINE);
        rect(6, 3, 5, 1, METAL_DARK);

        // Filler cap on the top left shoulder.
        rect(2, 5, 2, 3, OUTLINE);
        px(3, 6, METAL);
        px(2, 6, METAL_DARK);

        File out = new File("src/main/resources/assets/car/textures/item/jerry_can.png");
        out.getParentFile().mkdirs();
        ImageIO.write(img, "PNG", out);
        System.out.println("wrote " + out.getPath());
    }
}
