import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;

/**
 * Generates the car key's 16x16 item texture: a black plastic head with a
 * red button, a key ring, and a toothed steel blade running down-right.
 */
public class GenCarKey {

    static final int OUTLINE = 0xFF151518;
    static final int HEAD = 0xFF33343A;
    static final int HEAD_LIGHT = 0xFF55575F;
    static final int BUTTON = 0xFFD8322B;
    static final int STEEL = 0xFFC9CDD2;
    static final int STEEL_DARK = 0xFF7E848C;
    static final int RING = 0xFFB89A3E;

    static BufferedImage img = new BufferedImage(16, 16, BufferedImage.TYPE_INT_ARGB);

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

    public static void main(String[] args) throws Exception {
        String out = args.length > 0 ? args[0] : "src/main/resources/assets/car/textures/item";
        // Ring, top left.
        int[][] ring = {{1, 2}, {2, 1}, {3, 1}, {4, 2}, {1, 3}, {1, 4}, {2, 5}, {4, 3}};
        for (int[] p : ring) {
            px(p[0], p[1], RING);
        }
        // Head: a rounded block.
        rect(4, 3, 6, 6, OUTLINE);
        rect(3, 4, 8, 4, OUTLINE);
        rect(5, 4, 4, 4, HEAD);
        rect(4, 5, 6, 2, HEAD);
        rect(5, 4, 2, 1, HEAD_LIGHT);
        rect(4, 5, 1, 1, HEAD_LIGHT);
        px(7, 5, BUTTON);
        px(8, 5, BUTTON);
        px(7, 6, 0xFF9E1E19);
        px(8, 6, 0xFF9E1E19);
        // Blade, diagonally down and right, with its cuts.
        for (int i = 0; i < 6; i++) {
            px(9 + i, 8 + i, STEEL);
            px(10 + i, 8 + i, STEEL_DARK);
            px(9 + i, 9 + i, OUTLINE);
        }
        px(10, 10, STEEL_DARK);
        px(12, 12, STEEL_DARK);
        px(15, 13, OUTLINE);
        px(14, 14, OUTLINE);
        ImageIO.write(img, "png", new File(out, "car_key.png"));
    }
}
