import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

/**
 * The electric drivetrain's items: the old OBD code reader's 16x16 texture (no longer
 * written; see GenTablet),
 * and the electric motor's 3D item model with its own little colour sheet.
 *
 * <p>The motor is a finned silver can, black end caps, the shaft out of the
 * front, an inverter box on top with copper terminals and the orange
 * high-voltage lead every real EV wears. Run from the project root.
 */
public class GenEvItems {

    static final String TEXTURES = "src/main/resources/assets/car/textures/item/";
    static final String MODELS = "src/main/resources/assets/car/models/item/";

    // ---- the code reader ----

    static final int OUTLINE = 0xFF141416;
    static final int BUMPER = 0xFFE8B320;
    static final int BUMPER_DARK = 0xFFB0841A;
    static final int BODY = 0xFF2E3035;
    static final int SCREEN = 0xFF14361F;
    static final int SCREEN_TEXT = 0xFF5CF07A;
    static final int SCREEN_RED = 0xFFF0504A;
    static final int BUTTON = 0xFF70757D;
    static final int BUTTON_OK = 0xFF3FA45A;
    static final int CABLE = 0xFF1D1E22;
    static final int PLUG = 0xFF3A6FC4;
    static final int PLUG_DARK = 0xFF27508F;

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

    static void scanner() throws Exception {
        img = new BufferedImage(16, 16, BufferedImage.TYPE_INT_ARGB);
        // The lead: out of the top of the reader, looping up to the plug.
        px(8, 3, CABLE);
        px(9, 2, CABLE);
        px(10, 2, CABLE);
        px(11, 1, CABLE);
        // The blue 16-pin plug, top right.
        rect(12, 0, 4, 3, PLUG);
        rect(12, 2, 4, 1, PLUG_DARK);
        px(15, 0, OUTLINE);
        px(15, 2, OUTLINE);
        // Yellow rubber bumper round the handset.
        rect(3, 4, 10, 12, OUTLINE);
        rect(4, 4, 8, 11, BUMPER);
        rect(4, 14, 8, 1, BUMPER_DARK);
        rect(11, 5, 1, 9, BUMPER_DARK);
        // The dark handset inside it.
        rect(5, 5, 6, 9, BODY);
        // The screen, with two lines of code and one red warning.
        rect(5, 5, 6, 4, OUTLINE);
        rect(6, 6, 4, 2, SCREEN);
        px(6, 6, SCREEN_TEXT);
        px(7, 6, SCREEN_TEXT);
        px(9, 6, SCREEN_RED);
        px(6, 7, SCREEN_TEXT);
        px(8, 7, SCREEN_TEXT);
        // Buttons: arrows and a green OK.
        px(6, 10, BUTTON);
        px(8, 10, BUTTON_OK);
        px(9, 10, BUTTON_OK);
        px(6, 12, BUTTON);
        px(7, 11, BUTTON);
        px(7, 13, BUTTON);
        px(9, 12, BUTTON);
        ImageIO.write(img, "png", new File(TEXTURES + "obd_scanner.png"));
        Files.writeString(Path.of(MODELS + "obd_scanner.json"), """
                {
                  "parent": "minecraft:item/handheld",
                  "textures": {
                    "layer0": "car:item/obd_scanner"
                  }
                }
                """);
        Files.writeString(Path.of("src/main/resources/assets/car/items/obd_scanner.json"), """
                {
                  "model": {
                    "type": "minecraft:model",
                    "model": "car:item/obd_scanner"
                  }
                }
                """);
    }

    // ---- the electric motor ----

    /** Colour cells on the motor's sheet, four by four texels each. */
    enum Cell {
        HOUSING(0, 0), FINS(1, 0), CAP(2, 0), SHAFT(3, 0),
        INVERTER(0, 1), COPPER(1, 1), CABLE(2, 1), LABEL(3, 1);

        final int cx;
        final int cy;

        Cell(int cx, int cy) {
            this.cx = cx;
            this.cy = cy;
        }

        String uv() {
            // In from the edges so neighbouring cells never bleed in.
            return String.format("[%.1f, %.1f, %.1f, %.1f]",
                    this.cx * 4 + 0.5, this.cy * 4 + 0.5, this.cx * 4 + 3.5, this.cy * 4 + 3.5);
        }
    }

    static void motorSheet() throws Exception {
        img = new BufferedImage(16, 16, BufferedImage.TYPE_INT_ARGB);
        // Housing: brushed silver with darker fin grooves across it.
        rect(0, 0, 4, 4, 0xFFA7AEB5);
        rect(0, 1, 4, 1, 0xFF7E858D);
        rect(0, 3, 4, 1, 0xFF7E858D);
        rect(4, 0, 4, 4, 0xFF6B7178);
        rect(8, 0, 4, 4, 0xFF2A2C30);
        rect(12, 0, 4, 4, 0xFFD3D7DB);
        rect(0, 4, 4, 4, 0xFF3F5872);
        rect(0, 5, 4, 1, 0xFF4E6A88);
        rect(4, 4, 4, 4, 0xFFC77B3A);
        rect(8, 4, 4, 4, 0xFFF07A1A);
        rect(12, 4, 4, 4, 0xFFF2D13A);
        ImageIO.write(img, "png", new File(TEXTURES + "electric_motor.png"));
    }

    static String box(String name, double[] from, double[] to, Cell cell, Cell ends) {
        StringBuilder faces = new StringBuilder();
        String[] sides = {"north", "south", "east", "west", "up", "down"};
        for (int i = 0; i < sides.length; i++) {
            Cell c = i < 2 ? ends : cell;
            faces.append(String.format("        \"%s\": {\"uv\": %s, \"texture\": \"#sheet\"}%s%n",
                    sides[i], c.uv(), i < sides.length - 1 ? "," : ""));
        }
        return String.format("""
                    {
                      "name": "%s",
                      "from": [%s, %s, %s],
                      "to": [%s, %s, %s],
                      "faces": {
                %s      }
                    }""", name, from[0], from[1], from[2], to[0], to[1], to[2], faces);
    }

    static void motorModel() throws Exception {
        List<String> parts = new ArrayList<>();
        // The can, an octagon from three crossed boxes, finned all round.
        parts.add(box("housing", new double[]{4, 3, 3}, new double[]{12, 11, 13}, Cell.HOUSING, Cell.CAP));
        parts.add(box("housing_wide", new double[]{3, 4, 3.5}, new double[]{13, 10, 12.5}, Cell.FINS, Cell.CAP));
        parts.add(box("housing_tall", new double[]{5, 2, 3.5}, new double[]{11, 12, 12.5}, Cell.FINS, Cell.CAP));
        // End caps and the shaft out of the front.
        parts.add(box("front_cap", new double[]{5, 4, 2}, new double[]{11, 10, 3}, Cell.CAP, Cell.CAP));
        parts.add(box("rear_cap", new double[]{5, 4, 13}, new double[]{11, 10, 14}, Cell.CAP, Cell.CAP));
        parts.add(box("shaft", new double[]{7.25, 6.25, 0}, new double[]{8.75, 7.75, 2}, Cell.SHAFT, Cell.SHAFT));
        // The inverter on top, its terminals and its warning label.
        parts.add(box("inverter", new double[]{5, 11, 5}, new double[]{11, 13.5, 11}, Cell.INVERTER, Cell.INVERTER));
        parts.add(box("label", new double[]{6.5, 11.5, 4.9}, new double[]{9.5, 13, 5}, Cell.LABEL, Cell.LABEL));
        parts.add(box("terminal_pos", new double[]{6, 13.5, 8}, new double[]{7, 14.25, 9}, Cell.COPPER, Cell.COPPER));
        parts.add(box("terminal_neg", new double[]{9, 13.5, 8}, new double[]{10, 14.25, 9}, Cell.COPPER, Cell.COPPER));
        // The orange high-voltage lead, over the side and down.
        parts.add(box("hv_lead_top", new double[]{11, 12, 7}, new double[]{13.5, 13, 8}, Cell.CABLE, Cell.CABLE));
        parts.add(box("hv_lead_side", new double[]{12.5, 5, 7}, new double[]{13.5, 12, 8}, Cell.CABLE, Cell.CABLE));
        String json = """
                {
                  "textures": {
                    "particle": "car:item/electric_motor",
                    "sheet": "car:item/electric_motor"
                  },
                  "elements": [
                %s
                  ],
                  "display": {
                    "gui": {"rotation": [30, 225, 0], "translation": [0, 0, 0], "scale": [0.95, 0.95, 0.95]},
                    "ground": {"translation": [0, 3, 0], "scale": [0.548, 0.548, 0.548]},
                    "fixed": {"scale": [1, 1, 1]},
                    "head": {"scale": [1, 1, 1]},
                    "thirdperson_righthand": {"rotation": [75, 45, 0], "translation": [0, 2.5, 0], "scale": [0.493, 0.493, 0.493]},
                    "thirdperson_lefthand": {"rotation": [75, 45, 0], "translation": [0, 2.5, 0], "scale": [0.493, 0.493, 0.493]},
                    "firstperson_righthand": {"rotation": [0, 45, 0], "translation": [0, 3.5, 0], "scale": [0.329, 0.329, 0.329]},
                    "firstperson_lefthand": {"rotation": [0, 225, 0], "translation": [0, 3.5, 0], "scale": [0.329, 0.329, 0.329]}
                  }
                }
                """.formatted(String.join(",\n", parts));
        Files.writeString(Path.of(MODELS + "engine_electric.json"), json);
    }

    public static void main(String[] args) throws Exception {
        // The reader became the OBD tablet: its icon is now drawn by GenTablet.
        // scanner();
        motorSheet();
        motorModel();
        System.out.println("wrote obd_scanner and engine_electric");
    }
}
