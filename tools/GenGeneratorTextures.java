import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;

/** Generates the generator's block textures: front (cold and lit), side, top. */
public class GenGeneratorTextures {

    static final int FRAME = 0xFF2B3033;
    static final int BODY = 0xFF3F6B4A;
    static final int BODY_DARK = 0xFF2F5238;
    static final int BODY_LIGHT = 0xFF57886A;
    static final int METAL = 0xFF6B7579;
    static final int METAL_LIGHT = 0xFF9AA5AA;
    static final int VENT = 0xFF1B1E20;
    static final int COPPER = 0xFFC87A4B;
    static final int COPPER_DARK = 0xFF8E5231;
    static final int FIRE = 0xFFE8761B;
    static final int FIRE_CORE = 0xFFF7D44C;
    static final int LAMP_OFF = 0xFF5A2A26;
    static final int LAMP_ON = 0xFF7DF06A;

    static BufferedImage img;

    static void rect(int x, int y, int w, int h, int c) {
        for (int i = x; i < x + w; i++) {
            for (int j = y; j < y + h; j++) {
                if (i >= 0 && j >= 0 && i < 16 && j < 16) {
                    img.setRGB(i, j, c);
                }
            }
        }
    }

    static void outline(int x, int y, int w, int h, int c) {
        rect(x, y, w, 1, c);
        rect(x, y + h - 1, w, 1, c);
        rect(x, y, 1, h, c);
        rect(x + w - 1, y, 1, h, c);
    }

    static void shell() {
        img = new BufferedImage(16, 16, BufferedImage.TYPE_INT_ARGB);
        rect(0, 0, 16, 16, BODY);
        rect(1, 1, 14, 1, BODY_LIGHT);
        rect(1, 14, 14, 1, BODY_DARK);
        outline(0, 0, 16, 16, FRAME);
        for (int[] bolt : new int[][] {{1, 1}, {14, 1}, {1, 14}, {14, 14}}) {
            rect(bolt[0], bolt[1], 1, 1, METAL_LIGHT);
        }
    }

    static void save(String name) throws Exception {
        File out = new File("src/main/resources/assets/overthought_vehicles/textures/block/" + name + ".png");
        ImageIO.write(img, "PNG", out);
        System.out.println("wrote " + out.getPath());
    }

    static void front(boolean lit) throws Exception {
        shell();
        // the firebox door
        rect(3, 8, 10, 6, METAL);
        outline(3, 8, 10, 6, FRAME);
        rect(5, 10, 6, 3, lit ? FIRE : VENT);
        if (lit) {
            rect(6, 11, 4, 2, FIRE_CORE);
            rect(7, 10, 1, 1, FIRE_CORE);
        }
        // the gauge and the lamp
        rect(3, 3, 6, 3, VENT);
        rect(4, 4, lit ? 4 : 1, 1, lit ? LAMP_ON : LAMP_OFF);
        rect(11, 3, 2, 2, lit ? LAMP_ON : LAMP_OFF);
        save(lit ? "generator_front_lit" : "generator_front");
    }

    public static void main(String[] args) throws Exception {
        front(false);
        front(true);

        // side: cooling slats and a copper terminal
        shell();
        for (int y = 3; y <= 9; y += 2) {
            rect(3, y, 10, 1, VENT);
        }
        rect(6, 11, 4, 3, COPPER);
        outline(6, 11, 4, 3, COPPER_DARK);
        rect(7, 12, 2, 1, FIRE_CORE);
        save("generator_side");

        // top: an exhaust stack and a carrying handle
        shell();
        rect(3, 3, 5, 5, METAL);
        outline(3, 3, 5, 5, FRAME);
        rect(4, 4, 3, 3, VENT);
        rect(10, 4, 3, 8, METAL);
        outline(10, 4, 3, 8, FRAME);
        rect(3, 11, 5, 2, COPPER);
        save("generator_top");
    }
}
