import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;

/**
 * Paints the wheels: one sheet per kind of tyre, so that what is fitted can
 * be seen from the kerb.
 *
 * The wheel is built in layers of boxes (see DefinedVehicleModel): the tyre's
 * face, a bulging sidewall ring, the tread, and a thick toothed hub standing
 * out of the middle. Its depth comes from the boxes, so the paint is plain:
 * black rubber, notched at the edge on the grippy kinds, and a flat grey hub.
 *
 * The model maps the sheet as 64x64; it is painted at 128x128 in doubled
 * pixels, so it reads as ordinary 16-pixel art.
 *
 *   (0,16) and (20,16), 16x16   the tyre's two faces
 *   (40,0) onwards              tread slab; strips at (43,0) and (47,0), 4x3
 *   (40,22) onwards             sidewall slab, all rubber
 *   (0,32) to (40,64)           hub and teeth, all grey
 *
 * (coordinates in the model's 64x64; double them for the image)
 *
 * Run from the project root:  java tools/GenWheelTextures.java
 */
public class GenWheelTextures {

    static final int RUBBER = 0xFF202023;
    static final int RUBBER_LIGHT = 0xFF34343A;
    static final int GROOVE = 0xFF111113;
    static final int FROST = 0xFFD6DCE4;

    public static void main(String[] args) throws Exception {
        for (String kind : new String[] {"road", "dirt", "snow", "sport"}) {
            BufferedImage image = new BufferedImage(128, 128, BufferedImage.TYPE_INT_ARGB);
            // tread slab and sidewall ring: rubber all over
            for (int x = 80; x < 128; x++) {
                for (int y = 0; y < 82; y++) {
                    image.setRGB(x, y, RUBBER);
                }
            }
            // the tyre's faces, in doubled pixels
            for (int face = 0; face < 2; face++) {
                for (int px = 0; px < 16; px++) {
                    for (int py = 0; py < 16; py++) {
                        int colour = tyre(kind, px - 7.5, py - 7.5);
                        if (colour != 0) {
                            fill(image, (face * 20 + px) * 2, (16 + py) * 2, 2, colour);
                        }
                    }
                }
            }
            // the hub and its teeth: one flat grey, the model's own shading does the rest
            for (int x = 0; x < 80; x++) {
                for (int y = 64; y < 128; y++) {
                    image.setRGB(x, y, hubColour(kind));
                }
            }
            for (int across = 0; across < 8; across++) {
                for (int along = 0; along < 3; along++) {
                    fill(image, (43 + across) * 2, along * 2, 2, tread(kind, across % 4, along));
                }
            }
            File file = new File("src/main/resources/assets/car/textures/entity/wheel/" + kind + ".png");
            ImageIO.write(image, "PNG", file);
            System.out.println("wrote " + file.getPath());
        }
    }

    static void fill(BufferedImage image, int x, int y, int size, int colour) {
        for (int dx = 0; dx < size; dx++) {
            for (int dy = 0; dy < size; dy++) {
                image.setRGB(x + dx, y + dy, colour);
            }
        }
    }

    /** The tyre, in 16-pixel units: black, and notched where it grips. */
    static int tyre(String kind, double x, double y) {
        double r = Math.hypot(x, y);
        if (r > 7.9) {
            return 0;
        }
        double angle = Math.toDegrees(Math.atan2(y, x)) + 180.0;
        boolean edge = r > 6.9;
        return switch (kind) {
            case "dirt" -> edge && ((int) (angle / 30.0)) % 2 == 0 ? RUBBER_LIGHT : RUBBER;
            case "snow" -> edge && ((int) (angle / 30.0)) % 2 == 0 ? FROST : RUBBER;
            default -> RUBBER;
        };
    }

    static int hubColour(String kind) {
        return switch (kind) {
            case "snow" -> 0xFF6A6E75;
            case "sport" -> 0xFFC6C9CD;
            default -> 0xFFA9ACB0;
        };
    }

    static int tread(String kind, int across, int along) {
        return switch (kind) {
            case "dirt" -> (across + along) % 2 == 0 ? RUBBER_LIGHT : GROOVE;
            case "snow" -> (across + along) % 3 == 0 ? FROST : RUBBER_LIGHT;
            case "sport" -> RUBBER;
            default -> across == 1 ? GROOVE : RUBBER;
        };
    }
}
