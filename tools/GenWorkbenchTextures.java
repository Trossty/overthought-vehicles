import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;

/**
 * Generates the vehicle workbench's block textures (top, side, bottom).
 * Same code-driven approach as GenCarTextures, so the palette can be retuned
 * here instead of in an image editor.
 */
public class GenWorkbenchTextures {

    static final int FRAME = 0xFF32373A;
    static final int METAL_DARK = 0xFF4A5255;
    static final int METAL = 0xFF6B7579;
    static final int METAL_LIGHT = 0xFF8C989D;
    static final int HANDLE = 0xFFB9C3C7;
    static final int RED = 0xFFB3261E;
    static final int RED_DARK = 0xFF7A1B16;
    static final int OIL = 0xFF2B2F31;

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

    static void outline(int x, int y, int w, int h, int c) {
        rect(x, y, w, 1, c);
        rect(x, y + h - 1, w, 1, c);
        rect(x, y, 1, h, c);
        rect(x + w - 1, y, 1, h, c);
    }

    static void start() {
        img = new BufferedImage(16, 16, BufferedImage.TYPE_INT_ARGB);
    }

    static void save(String name) throws Exception {
        File out = new File("src/main/resources/assets/overthought_vehicles/textures/block/" + name + ".png");
        out.getParentFile().mkdirs();
        ImageIO.write(img, "PNG", out);
        System.out.println("wrote " + out.getPath());
    }

    public static void main(String[] args) throws Exception {
        // --- top: a scratched steel bench surface with bolted corners ---
        start();
        rect(0, 0, 16, 16, METAL);
        outline(0, 0, 16, 16, FRAME);
        rect(2, 2, 12, 12, METAL_DARK);
        outline(2, 2, 12, 12, METAL_LIGHT);
        // Corner bolts.
        for (int[] b : new int[][]{{3, 3}, {12, 3}, {3, 12}, {12, 12}}) {
            px(b[0], b[1], HANDLE);
        }
        // Scuffs and an oil stain, so the surface does not read as flat plastic.
        px(6, 5, METAL); px(7, 5, METAL); px(9, 8, METAL);
        px(5, 10, METAL); px(10, 11, METAL);
        rect(7, 9, 3, 2, OIL);
        px(6, 10, OIL); px(10, 9, OIL);

        save("vehicle_workbench_top");

        // --- side: a cabinet with two drawers and a red accent band ---
        start();
        rect(0, 0, 16, 16, METAL);
        outline(0, 0, 16, 16, FRAME);
        // Bench lip along the top.
        rect(1, 1, 14, 2, METAL_LIGHT);
        // Accent band in the car's own red.
        rect(1, 3, 14, 1, RED);
        px(1, 3, RED_DARK);
        px(14, 3, RED_DARK);
        // Two drawers.
        for (int drawerY : new int[]{5, 10}) {
            rect(2, drawerY, 12, 4, METAL_DARK);
            outline(2, drawerY, 12, 4, FRAME);
            rect(6, drawerY + 1, 4, 1, HANDLE);
        }
        // Feet.
        rect(1, 15, 3, 1, FRAME);
        rect(12, 15, 3, 1, FRAME);

        save("vehicle_workbench_side");

        // --- bottom: plain plate ---
        start();
        rect(0, 0, 16, 16, METAL_DARK);
        outline(0, 0, 16, 16, FRAME);
        px(4, 4, METAL); px(11, 4, METAL); px(4, 11, METAL); px(11, 11, METAL);

        save("vehicle_workbench_bottom");
    }
}
