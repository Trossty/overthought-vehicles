import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;

/**
 * The OBD tablet: the frame it is drawn in when held up like a map (a rugged
 * workshop tablet, rubber corners and a dark glass screen), a plain white
 * patch the screen's bars are tinted from, and the screen in the item's
 * face. Run from the project root.
 */
public class GenTablet {

    static final String TEXTURES = "src/main/resources/assets/overthought_vehicles/textures/";

    static BufferedImage img;

    static void px(int x, int y, int c) {
        if (x >= 0 && y >= 0 && x < img.getWidth() && y < img.getHeight()) {
            img.setRGB(x, y, c);
        }
    }

    static void rect(int x0, int y0, int x1, int y1, int c) {
        for (int x = x0; x <= x1; x++) {
            for (int y = y0; y <= y1; y++) {
                px(x, y, c);
            }
        }
    }

    static void write(String path) throws Exception {
        File file = new File(TEXTURES + path + ".png");
        file.getParentFile().mkdirs();
        ImageIO.write(img, "png", file);
    }

    /** Whether (x, y) is inside a square of this size with corners rounded to this radius. */
    static boolean inside(int x, int y, int size, int radius) {
        int cx = Math.max(radius, Math.min(size - 1 - radius, x));
        int cy = Math.max(radius, Math.min(size - 1 - radius, y));
        double dx = x - cx;
        double dy = y - cy;
        return dx * dx + dy * dy <= radius * radius + 0.5;
    }

    /** A little pseudo-random grain, the same every run. */
    static int grain(int x, int y) {
        int h = x * 73856093 ^ y * 19349663;
        h ^= h >>> 13;
        return (h & 7) - 3;
    }

    static int shade(int argb, int by) {
        int r = Math.max(0, Math.min(255, ((argb >> 16) & 0xFF) + by));
        int g = Math.max(0, Math.min(255, ((argb >> 8) & 0xFF) + by));
        int b = Math.max(0, Math.min(255, (argb & 0xFF) + by));
        return (argb & 0xFF000000) | r << 16 | g << 8 | b;
    }

    /**
     * The back, as seen from behind when the tablet is held up: grained dark
     * plastic, the same rubber corners, a camera, a grip band and a label.
     */
    static void back(int bezel, int rubber, int rubberDark) throws Exception {
        int size = 72;
        img = new BufferedImage(size, size, BufferedImage.TYPE_INT_ARGB);
        int plastic = 0xFF2C3036;
        for (int x = 0; x < size; x++) {
            for (int y = 0; y < size; y++) {
                if (!inside(x, y, size, 6)) {
                    continue;
                }
                boolean edge = !inside(x - 1, y, size, 6) || !inside(x + 1, y, size, 6)
                        || !inside(x, y - 1, size, 6) || !inside(x, y + 1, size, 6);
                boolean corner = (x < 11 || x >= size - 11) && (y < 11 || y >= size - 11);
                int c = corner ? shade(rubber, grain(x, y)) : shade(plastic, grain(x, y));
                if (edge) {
                    c = corner ? rubberDark : 0xFF16181C;
                }
                px(x, y, c);
            }
        }
        // A raised rim just inside the edge.
        for (int i = 4; i < size - 4; i++) {
            if (i >= 11 && i < size - 11) {
                px(i, 3, 0xFF3A3F46);
                px(i, size - 4, 0xFF1F2226);
                px(3, i, 0xFF3A3F46);
                px(size - 4, i, 0xFF1F2226);
            }
        }
        // Screws by each bumper.
        int[][] screws = {{13, 13}, {58, 13}, {13, 58}, {58, 58}};
        for (int[] sc : screws) {
            px(sc[0], sc[1], 0xFF8C939C);
            px(sc[0] + 1, sc[1], 0xFF5D636B);
            px(sc[0], sc[1] + 1, 0xFF5D636B);
            px(sc[0] + 1, sc[1] + 1, 0xFF3C4147);
        }
        // Camera module, top left as you look at the back.
        rect(8, 8, 21, 21, 0xFF15171A);
        rect(9, 9, 20, 20, 0xFF202328);
        for (int x = 10; x <= 19; x++) {
            for (int y = 10; y <= 19; y++) {
                double d = Math.hypot(x - 14.5, y - 14.5);
                if (d <= 4.6) {
                    px(x, y, d <= 2.2 ? 0xFF0A2A3C : d <= 3.4 ? 0xFF050608 : 0xFF6A7078);
                }
            }
        }
        px(13, 13, 0xFF7FC8E8);
        rect(23, 10, 25, 12, 0xFFF4E9C8); // flash
        // Rubber grip band across the middle, ridged.
        for (int x = 4; x < size - 4; x++) {
            for (int y = 30; y <= 41; y++) {
                int c = (y % 3 == 0) ? 0xFF1B1D21 : 0xFF25282D;
                px(x, y, shade(c, grain(x, y) / 2));
            }
        }
        rect(4, 29, size - 5, 29, 0xFF3A3F46);
        rect(4, 42, size - 5, 42, 0xFF16181C);
        // A workshop label: orange stripe, "OBD-II" in blocks of print, a barcode.
        rect(24, 50, 47, 62, 0xFFE8E4DA);
        rect(24, 50, 47, 52, rubber);
        int[][] print = {{26, 55}, {27, 55}, {29, 55}, {30, 55}, {32, 55}, {33, 55}, {35, 55}, {37, 55}, {38, 55}};
        for (int[] pt : print) {
            px(pt[0], pt[1], 0xFF2C3036);
            px(pt[0], pt[1] + 1, 0xFF2C3036);
        }
        for (int x = 26; x <= 45; x++) {
            if ((x * 7) % 3 != 0) {
                rect(x, 58, x, 60, 0xFF2C3036);
            }
        }
        write("gui/obd_tablet_back");
    }

    /**
     * The screen in the face of the tablet as an item (its model is drawn in
     * tools/items/tools.js): the fault list, in the colours the held tablet uses.
     */
    static void screen() throws Exception {
        img = new BufferedImage(16, 16, BufferedImage.TYPE_INT_ARGB);
        rect(0, 0, 15, 15, 0xFF0A1820);
        rect(1, 1, 7, 1, 0xFF4FD6E8);
        px(14, 1, 0xFF5BE36B);
        rect(1, 3, 14, 3, 0xFF1E3A47);
        rect(1, 5, 9, 5, 0xFF132A34);
        rect(1, 5, 7, 5, 0xFF4FD6E8);
        rect(1, 7, 9, 7, 0xFF132A34);
        rect(1, 7, 5, 7, 0xFFF0A53A);
        rect(1, 9, 14, 9, 0xFF1E3A47);
        int[][] codes = {{11, 0xFFE8483B}, {13, 0xFFF0A53A}};
        for (int[] code : codes) {
            rect(1, code[0], 2, code[0], code[1]);
            rect(4, code[0], 12, code[0], 0xFF8A99A3);
        }
        write("item/obd_tablet_screen");
    }

    public static void main(String[] args) throws Exception {
        // ---- the frame, 72 px across: the screen is the 64 px in the middle ----
        int size = 72;
        img = new BufferedImage(size, size, BufferedImage.TYPE_INT_ARGB);
        int bezel = 0xFF23262B;
        int bezelLight = 0xFF33373E;
        int bezelDark = 0xFF16181C;
        int rubber = 0xFFD9822B;
        int rubberDark = 0xFFA85F1A;
        for (int x = 0; x < size; x++) {
            for (int y = 0; y < size; y++) {
                if (!inside(x, y, size, 6)) {
                    continue;
                }
                boolean edge = !inside(x - 1, y, size, 6) || !inside(x + 1, y, size, 6)
                        || !inside(x, y - 1, size, 6) || !inside(x, y + 1, size, 6);
                // Rubber bumpers on the corners, the way a shop tablet is armoured.
                boolean corner = (x < 11 || x >= size - 11) && (y < 11 || y >= size - 11);
                int c = corner ? rubber : bezel;
                if (edge) {
                    c = corner ? rubberDark : bezelDark;
                } else if (y == 1 || x == 1) {
                    c = corner ? 0xFFE9A050 : bezelLight;
                }
                px(x, y, c);
            }
        }
        // Screen: dark glass, a touch lighter at the top where the light catches it.
        for (int x = 4; x < 68; x++) {
            for (int y = 4; y < 68; y++) {
                int c = y < 6 ? 0xFF112630 : 0xFF0A1820;
                if ((x + y) % 23 == 0 && y < 20) {
                    c = 0xFF14303C;
                }
                px(x, y, c);
            }
        }
        rect(3, 3, 68, 3, bezelDark);
        rect(3, 68, 68, 68, bezelDark);
        rect(3, 3, 3, 68, bezelDark);
        rect(68, 3, 68, 68, bezelDark);
        // Camera at the top and a home button at the bottom.
        px(35, 1, 0xFF0B0C0E);
        px(36, 1, 0xFF0B0C0E);
        px(36, 2, 0xFF3A6A8A);
        rect(33, 69, 38, 70, bezelLight);
        rect(34, 69, 37, 69, 0xFF41464E);
        write("gui/obd_tablet");

        // ---- a white patch the screen's bars are coloured from ----
        img = new BufferedImage(4, 4, BufferedImage.TYPE_INT_ARGB);
        rect(0, 0, 3, 3, 0xFFFFFFFF);
        write("gui/obd_tablet_fill");

        // ---- the inventory icon: the same tablet, with a fault list on it ----
        img = new BufferedImage(16, 16, BufferedImage.TYPE_INT_ARGB);
        int outline = 0xFF101114;
        rect(2, 1, 13, 14, bezel);
        rect(2, 1, 13, 1, outline);
        rect(2, 14, 13, 14, outline);
        rect(1, 2, 1, 13, outline);
        rect(14, 2, 14, 13, outline);
        px(2, 1, 0);
        px(13, 1, 0);
        px(2, 14, 0);
        px(13, 14, 0);
        px(2, 2, rubber);
        px(13, 2, rubber);
        px(2, 13, rubber);
        px(13, 13, rubber);
        px(3, 2, rubber);
        px(12, 2, rubber);
        px(3, 13, rubberDark);
        px(12, 13, rubberDark);
        rect(3, 3, 12, 12, 0xFF0A1820);
        // Header line, then codes in red, amber and green.
        rect(4, 4, 8, 4, 0xFF4FD6E8);
        px(11, 4, 0xFF5BE36B);
        rect(4, 6, 6, 6, 0xFFE8483B);
        rect(8, 6, 11, 6, 0xFF8A99A3);
        rect(4, 8, 6, 8, 0xFFF0A53A);
        rect(8, 8, 10, 8, 0xFF8A99A3);
        rect(4, 10, 6, 10, 0xFFF0D84A);
        rect(8, 10, 11, 10, 0xFF8A99A3);
        px(7, 15, 0);
        write("item/obd_scanner");

        back(bezel, rubber, rubberDark);
        screen();

        System.out.println("wrote obd tablet frame, back, fill and screen");
    }
}
