import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;

/**
 * Generates the vehicle workstation's block textures: a blueprint of a car
 * pinned to the top, a red steel cabinet front with drawers, plain steel sides.
 */
public class GenWorkstationTextures {

    static final int FRAME = 0xFF32373A;
    static final int STEEL = 0xFF6B7579;
    static final int STEEL_LIGHT = 0xFF8C989D;
    static final int STEEL_DARK = 0xFF4A5255;
    static final int RED = 0xFFB3261E;
    static final int RED_LIGHT = 0xFFD43C33;
    static final int RED_DARK = 0xFF7A1B16;
    static final int HANDLE = 0xFFC9D1D4;
    static final int PAPER = 0xFF2D5B9A;
    static final int GRID = 0xFF3B6CAE;
    static final int INK = 0xFFE8F1FF;

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

    static void frame() {
        rect(0, 0, 16, 16, FRAME);
    }

    public static void main(String[] args) throws Exception {
        String out = args.length > 0 ? args[0] : "src/main/resources/assets/overthought_vehicles/textures/block";

        // Top: a blueprint on a steel bench top.
        img = new BufferedImage(16, 16, BufferedImage.TYPE_INT_ARGB);
        frame();
        rect(1, 1, 14, 14, STEEL);
        rect(2, 3, 12, 10, PAPER);
        for (int i = 2; i < 14; i += 3) {
            rect(i, 3, 1, 10, GRID);
        }
        for (int j = 3; j < 13; j += 3) {
            rect(2, j, 12, 1, GRID);
        }
        // The car, side on: roof, body, two wheels.
        rect(6, 6, 4, 1, INK);
        px(5, 7, INK);
        px(10, 7, INK);
        rect(3, 8, 10, 1, INK);
        px(3, 9, INK);
        px(12, 9, INK);
        rect(4, 10, 8, 1, INK);
        px(5, 11, INK);
        px(10, 11, INK);
        // Pins in the corners.
        px(2, 3, RED_LIGHT);
        px(13, 3, RED_LIGHT);
        px(2, 12, RED_LIGHT);
        px(13, 12, RED_LIGHT);
        // A light sweeps the blueprint, as if it were on a lit drawing board,
        // and stops a while at the end of each pass.
        BufferedImage still = img;
        int frames = 16;
        BufferedImage sheet = new BufferedImage(16, 16 * frames, BufferedImage.TYPE_INT_ARGB);
        for (int frame = 0; frame < frames; frame++) {
            img = new BufferedImage(16, 16, BufferedImage.TYPE_INT_ARGB);
            img.getGraphics().drawImage(still, 0, 0, null);
            int column = 2 + frame;
            if (column < 14) {
                for (int y = 3; y < 13; y++) {
                    int c = img.getRGB(column, y);
                    img.setRGB(column, y, c == INK ? 0xFFFFFFFF : c == PAPER || c == GRID ? 0xFF5A8FD6 : c);
                    if (column > 2) {
                        int behind = img.getRGB(column - 1, y);
                        img.setRGB(column - 1, y, behind == PAPER ? 0xFF3D70B5 : behind);
                    }
                }
            }
            sheet.getGraphics().drawImage(img, 0, 16 * frame, null);
        }
        ImageIO.write(sheet, "png", new File(out, "vehicle_workstation_top.png"));
        java.nio.file.Files.writeString(new File(out, "vehicle_workstation_top.png.mcmeta").toPath(),
                "{ \"animation\": { \"frametime\": 3 } }\n");
        img = still;

        // Front: a red tool cabinet with three drawers.
        img = new BufferedImage(16, 16, BufferedImage.TYPE_INT_ARGB);
        frame();
        rect(1, 1, 14, 2, STEEL_LIGHT);
        for (int d = 0; d < 3; d++) {
            int y = 4 + d * 4;
            rect(1, y, 14, 3, RED);
            rect(1, y, 14, 1, RED_LIGHT);
            rect(1, y + 2, 14, 1, RED_DARK);
            rect(6, y + 1, 4, 1, HANDLE);
        }
        ImageIO.write(img, "png", new File(out, "vehicle_workstation_front.png"));

        // Sides: steel with a pegboard of holes.
        img = new BufferedImage(16, 16, BufferedImage.TYPE_INT_ARGB);
        frame();
        rect(1, 1, 14, 2, STEEL_LIGHT);
        rect(1, 3, 14, 12, STEEL);
        for (int x = 3; x < 14; x += 3) {
            for (int y = 5; y < 14; y += 3) {
                px(x, y, STEEL_DARK);
            }
        }
        rect(1, 14, 14, 1, STEEL_DARK);
        ImageIO.write(img, "png", new File(out, "vehicle_workstation_side.png"));
    }
}
