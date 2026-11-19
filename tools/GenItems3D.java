import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;

/**
 * Textures for the solid (3D) spray can and car key item models. Each is a
 * small atlas: the models' faces point their UVs at the patches below.
 *
 * <pre>
 * spray_can_3d: metal 0-3/0-3, label 4-7/0-11 (tinted), cap 8-11/0-3 (tinted),
 *               nozzle 12-13/0-1, top 0-3/4-7
 * car_key_3d:   plastic 0-3/0-3, blade 4-7/0-3, red button 8-9/0-1,
 *               grey button 10-11/0-1, ring 12-13/0-1, groove 14-15/0-1
 * </pre>
 */
public class GenItems3D {

    static BufferedImage img;

    static void rect(int x, int y, int w, int h, int c) {
        for (int i = x; i < x + w; i++) {
            for (int j = y; j < y + h; j++) {
                img.setRGB(i, j, c);
            }
        }
    }

    public static void main(String[] args) throws Exception {
        String out = "src/main/resources/assets/overthought_vehicles/textures/item";

        img = new BufferedImage(16, 16, BufferedImage.TYPE_INT_ARGB);
        int[] metal = {0xFFD9DDE1, 0xFFB7BCC2, 0xFF9DA3AA, 0xFF7E848B};
        for (int x = 0; x < 4; x++) {
            rect(x, 0, 1, 4, metal[x]);
            rect(x, 4, 1, 4, metal[Math.min(3, x + 1)]);
        }
        rect(1, 5, 2, 2, 0xFF6A7077);
        // the label: near white so the dye tint reads true, shaded round the can
        int[] label = {0xFFFFFFFF, 0xFFF0F0F0, 0xFFDCDCDC, 0xFFC4C4C4};
        for (int x = 0; x < 4; x++) {
            rect(4 + x, 0, 1, 12, label[x]);
        }
        rect(4, 3, 4, 1, 0xFFA8A8A8);
        rect(4, 8, 4, 1, 0xFFA8A8A8);
        rect(5, 5, 2, 2, 0xFFFFFFFF);
        int[] cap = {0xFFFFFFFF, 0xFFE6E6E6, 0xFFD0D0D0, 0xFFB8B8B8};
        for (int x = 0; x < 4; x++) {
            rect(8 + x, 0, 1, 4, cap[x]);
        }
        rect(12, 0, 2, 2, 0xFF1B1B1D);
        ImageIO.write(img, "png", new File(out, "spray_can_3d.png"));

        img = new BufferedImage(16, 16, BufferedImage.TYPE_INT_ARGB);
        rect(0, 0, 4, 4, 0xFF232326);
        rect(0, 0, 4, 1, 0xFF3A3A3F);
        rect(0, 0, 1, 4, 0xFF3A3A3F);
        rect(3, 0, 1, 4, 0xFF151517);
        rect(4, 0, 4, 4, 0xFFC6CBD0);
        rect(4, 0, 4, 1, 0xFFE4E8EB);
        rect(4, 3, 4, 1, 0xFF9AA0A6);
        rect(8, 0, 2, 2, 0xFFC92A22);
        rect(8, 0, 1, 1, 0xFFE8524A);
        rect(10, 0, 2, 2, 0xFF6A6E74);
        rect(10, 0, 1, 1, 0xFF8C9096);
        rect(12, 0, 2, 2, 0xFFE0E4E8);
        rect(14, 0, 2, 2, 0xFF55595E);
        ImageIO.write(img, "png", new File(out, "car_key_3d.png"));
    }
}
