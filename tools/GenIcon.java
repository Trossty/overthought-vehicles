import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;

/** Builds the 64x64 mod icon from the item texture. Run from the project root. */
public class GenIcon {
    public static void main(String[] a) throws Exception {
        BufferedImage src = ImageIO.read(new File("src/main/resources/assets/car/textures/item/car.png"));
        BufferedImage out = new BufferedImage(64, 64, BufferedImage.TYPE_INT_ARGB);
        for (int x = 0; x < 64; x++)
            for (int y = 0; y < 64; y++)
                out.setRGB(x, y, 0xFF1B1B1F);
        for (int x = 0; x < 64; x++)
            for (int y = 0; y < 64; y++) {
                int argb = src.getRGB(x / 4, y / 4);
                if ((argb >>> 24) != 0) out.setRGB(x, y, argb);
            }
        File f = new File("src/main/resources/assets/car/icon.png");
        ImageIO.write(out, "PNG", f);
        System.out.println("wrote " + f);
    }
}
