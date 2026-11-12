import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.PrintWriter;

/**
 * Draws the icons for the vehicle parts and the lift, and writes their models.
 *
 * <p>Run with {@code java tools/GenPartTextures.java} from the project root.
 * Kept apart from GenCarTextures because none of this is painted per dye: a
 * gearbox is a gearbox whatever colour the car ends up.
 */
public class GenPartTextures {

    static final String ASSETS = "src/main/resources/assets/car/";

    static final int CLEAR = 0x00000000;
    static final int STEEL = 0xFF8A8F94;
    static final int STEEL_LIGHT = 0xFFB9BEC3;
    static final int STEEL_DARK = 0xFF4E5358;
    static final int COPPER = 0xFFC46A3B;
    static final int COPPER_DARK = 0xFF8A472A;
    static final int RUBBER = 0xFF181818;
    static final int RUBBER_LIGHT = 0xFF2C2C2C;
    static final int GLASS = 0xFF35505C;
    static final int PAINT = 0xFFC0392B;
    static final int PAINT_DARK = 0xFF7E2619;
    static final int HAZARD = 0xFFE8B021;
    static final int HAZARD_DARK = 0xFF8A6512;
    static final int OIL = 0xFF2A2E31;
    static final int WOOD = 0xFF9A6B3F;
    static final int WOOD_LIGHT = 0xFFB98A55;
    static final int WOOD_DARK = 0xFF6B4A2C;
    static final int CERAMIC = 0xFFE6E1D8;
    static final int CERAMIC_DARK = 0xFFB4AEA2;
    static final int PAPER = 0xFFEDE8D9;
    static final int PAPER_DARK = 0xFFC2BBA7;
    static final int BLUE = 0xFF2F6E9E;
    static final int BLUE_DARK = 0xFF1C4A6D;
    static final int GREEN = 0xFF4C9A43;
    static final int BRASS = 0xFFB08D4A;

    static BufferedImage img;

    static void px(int x, int y, int c) {
        if (c != CLEAR && x >= 0 && y >= 0 && x < img.getWidth() && y < img.getHeight()) {
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

    static void fresh() {
        img = new BufferedImage(16, 16, BufferedImage.TYPE_INT_ARGB);
    }

    static void save(String path) throws Exception {
        File out = new File(path);
        out.getParentFile().mkdirs();
        ImageIO.write(img, "PNG", out);
        System.out.println("wrote " + path);
    }

    // ---- the icons ----

    /** A bare shell: bodywork and glass, and conspicuously no wheels. */
    static void carBody() {
        fresh();
        rect(2, 7, 12, 4, PAINT);
        rect(2, 10, 12, 1, PAINT_DARK);
        rect(4, 4, 7, 3, PAINT);
        rect(5, 5, 5, 2, GLASS);
        rect(4, 4, 7, 1, PAINT_DARK);
        rect(2, 7, 12, 1, 0xFFD9594A);
        // the empty hubs, so it reads as unfinished
        rect(3, 11, 3, 1, STEEL_DARK);
        rect(10, 11, 3, 1, STEEL_DARK);
    }

    /** A block with a cam cover, a pulley and an exhaust stub. */
    static void engine() {
        fresh();
        rect(3, 5, 10, 8, STEEL);
        rect(3, 5, 10, 1, STEEL_LIGHT);
        rect(3, 12, 10, 1, STEEL_DARK);
        rect(4, 3, 7, 2, STEEL_DARK);       // cam cover
        rect(4, 3, 7, 1, STEEL);
        for (int i = 0; i < 3; i++) {
            rect(5 + i * 2, 1, 1, 2, STEEL_LIGHT);  // plug leads
        }
        rect(11, 7, 2, 4, COPPER);          // pulley
        rect(11, 8, 2, 1, COPPER_DARK);
        rect(1, 9, 2, 2, STEEL_DARK);       // exhaust stub
        rect(5, 8, 4, 3, OIL);              // sump window
    }

    /** A bell housing tapering back to the output shaft. */
    static void transmission() {
        fresh();
        rect(2, 4, 4, 8, STEEL);            // bell housing
        rect(2, 4, 4, 1, STEEL_LIGHT);
        rect(2, 11, 4, 1, STEEL_DARK);
        rect(6, 5, 4, 6, STEEL);            // case
        rect(6, 5, 4, 1, STEEL_LIGHT);
        rect(6, 10, 4, 1, STEEL_DARK);
        rect(10, 7, 3, 2, STEEL_DARK);      // tail shaft
        rect(13, 7, 2, 2, STEEL_LIGHT);
        rect(7, 2, 1, 3, STEEL_DARK);       // gear lever
        rect(6, 1, 3, 1, COPPER);
        rect(3, 6, 2, 4, OIL);
    }

    /** A coil over a damper, which is the one part you can draw unmistakably. */
    static void suspension() {
        fresh();
        rect(7, 1, 2, 2, STEEL_DARK);       // top mount
        rect(6, 2, 4, 1, STEEL_LIGHT);
        for (int i = 0; i < 5; i++) {       // the coils
            int y = 3 + i * 2;
            rect(4, y, 8, 1, COPPER);
            rect(4, y + 1, 1, 1, COPPER_DARK);
            rect(11, y + 1, 1, 1, COPPER_DARK);
        }
        rect(7, 3, 2, 10, STEEL);           // damper body through the middle
        rect(7, 3, 1, 10, STEEL_LIGHT);
        rect(6, 13, 4, 2, STEEL_DARK);      // bottom mount
        rect(7, 15, 2, 1, STEEL);
    }

    /** Tyre, rim, nuts. */
    static void wheel() {
        fresh();
        for (int x = 0; x < 16; x++) {
            for (int y = 0; y < 16; y++) {
                double d = Math.sqrt((x - 7.5) * (x - 7.5) + (y - 7.5) * (y - 7.5));
                if (d > 7.6) {
                    continue;
                }
                if (d > 5.2) {
                    px(x, y, y < 7 ? RUBBER_LIGHT : RUBBER);     // tyre
                } else if (d > 4.4) {
                    px(x, y, STEEL_DARK);                        // rim lip
                } else if (d > 1.6) {
                    px(x, y, y < 7 ? STEEL_LIGHT : STEEL);       // face
                } else {
                    px(x, y, STEEL_DARK);                        // hub
                }
            }
        }
        // tread blocks, so the tyre is not a flat ring
        for (int i = 0; i < 16; i += 3) {
            px(i, 1, RUBBER_LIGHT);
            px(i, 14, RUBBER_LIGHT);
        }
        px(7, 4, STEEL_DARK);
        px(4, 9, STEEL_DARK);
        px(11, 9, STEEL_DARK);
    }

    /** Lift deck: steel chequer plate with hazard stripes down two edges. */
    static void liftTop() {
        fresh();
        rect(0, 0, 16, 16, STEEL);
        for (int x = 0; x < 16; x++) {
            for (int y = 0; y < 16; y++) {
                if ((x + y) % 4 == 0) {
                    px(x, y, STEEL_LIGHT);
                } else if ((x + y) % 4 == 2) {
                    px(x, y, STEEL_DARK);
                }
            }
        }
        for (int x = 0; x < 16; x++) {
            int c = ((x / 2) % 2 == 0) ? HAZARD : HAZARD_DARK;
            rect(x, 0, 1, 2, c);
            rect(x, 14, 1, 2, c);
        }
    }

    /** Lift side: the ram and the hydraulic lines. */
    static void liftSide() {
        fresh();
        rect(0, 0, 16, 16, STEEL_DARK);
        rect(0, 0, 16, 2, STEEL);
        rect(0, 2, 16, 1, 0xFF32363A);
        rect(6, 3, 4, 13, STEEL);           // the ram
        rect(6, 3, 1, 13, STEEL_LIGHT);
        rect(9, 3, 1, 13, 0xFF32363A);
        rect(5, 5, 6, 1, HAZARD);
        rect(5, 9, 6, 1, HAZARD);
        rect(2, 11, 2, 5, COPPER_DARK);     // hydraulic line
        rect(12, 11, 2, 5, COPPER_DARK);
        rect(0, 15, 16, 1, 0xFF26292C);
    }

    static void liftBottom() {
        fresh();
        rect(0, 0, 16, 16, 0xFF3A3E42);
        for (int x = 0; x < 16; x += 4) {
            rect(x, 0, 1, 16, 0xFF2A2D30);
        }
    }

    /** Refinery top: a tank hatch and two pipe stubs. */
    static void refineryTop() {
        fresh();
        rect(0, 0, 16, 16, STEEL_DARK);
        rect(1, 1, 14, 14, STEEL);
        rect(1, 1, 14, 1, STEEL_LIGHT);
        rect(4, 4, 8, 8, 0xFF3A3E42);       // hatch
        rect(5, 5, 6, 6, STEEL_DARK);
        rect(7, 2, 2, 2, COPPER);           // pipe stubs
        rect(7, 12, 2, 2, COPPER_DARK);
        rect(2, 7, 2, 2, STEEL_LIGHT);
        rect(12, 7, 2, 2, STEEL_LIGHT);
    }

    /**
     * Refinery front. {@code lit} swaps the burner window from cold to burning,
     * which is the only thing the block's LIT state changes.
     */
    static void refineryFront(boolean lit) {
        fresh();
        rect(0, 0, 16, 16, STEEL_DARK);
        rect(1, 1, 14, 14, STEEL);
        rect(1, 1, 14, 1, STEEL_LIGHT);
        rect(1, 14, 14, 1, 0xFF3A3E42);

        rect(2, 2, 5, 7, GLASS);            // biomass sight glass
        rect(3, 5, 3, 3, 0xFF5FA03A);
        rect(9, 2, 5, 7, GLASS);            // petrol sight glass
        rect(10, 4, 3, 4, 0xFFD9A62B);

        rect(4, 10, 8, 4, lit ? 0xFF1A0E06 : OIL);   // burner window
        if (lit) {
            rect(5, 11, 6, 3, 0xFFE8761B);
            rect(7, 12, 2, 2, 0xFFF7D44C);
        }
        rect(4, 10, 8, 1, STEEL_DARK);
    }

    /** Refinery sides: ribbed casing and a copper line. */
    static void refinerySide() {
        fresh();
        rect(0, 0, 16, 16, STEEL_DARK);
        rect(1, 1, 14, 14, STEEL);
        rect(1, 1, 14, 1, STEEL_LIGHT);
        for (int y = 3; y < 14; y += 3) {
            rect(2, y, 12, 1, STEEL_DARK);
        }
        rect(11, 2, 2, 12, COPPER_DARK);
        rect(11, 2, 1, 12, COPPER);
    }


    // ---- the fuel line ----

    /**
     * The pipe's skin. Every face of every pipe box samples the same 6x6 patch
     * at the middle of the texture, so the tube reads the same whichever way the
     * arm points; the rest of the sheet is painted to match for the particles.
     */
    /**
     * The pipe's skin. The shading runs across the rows and is the same all
     * the way along, so a pipe reads as one unbroken tube however many blocks
     * long it is; every face of an arm is mapped with its length along the
     * texture's width.
     */
    static void pipeSkin() {
        fresh();
        rect(0, 0, 16, 16, COPPER);
        for (int x = 0; x < 16; x++) {
            px(x, 5, 0xFFE09265);           // the highlight on top of the tube
            px(x, 6, 0xFFD5825A);
            px(x, 9, COPPER_DARK);          // and its shadow underneath
            px(x, 10, 0xFF6E3620);
        }
    }

    /**
     * A fitting: the steel collar where a pipe turns, branches, ends or meets a
     * machine. Straight runs have none, so a line is a line, not a row of beads.
     */
    static void pipeCore() {
        fresh();
        rect(0, 0, 16, 16, STEEL);
        rect(4, 4, 8, 1, STEEL_LIGHT);
        rect(4, 4, 1, 8, STEEL_LIGHT);
        rect(4, 11, 8, 1, STEEL_DARK);
        rect(11, 4, 1, 8, STEEL_DARK);
        for (int[] bolt : new int[][]{{5, 5}, {10, 5}, {5, 10}, {10, 10}}) {
            px(bolt[0], bolt[1], STEEL_DARK);
        }
        rect(6, 6, 4, 4, 0xFF6E3620);       // the bore, copper showing through
        rect(7, 7, 2, 2, COPPER_DARK);
    }

    /** The barrel on its own, as a bench part: a drum with a painted band. */
    static void barrelIcon() {
        fresh();
        rect(3, 2, 10, 12, STEEL);
        rect(3, 2, 10, 1, STEEL_LIGHT);
        rect(3, 13, 10, 1, STEEL_DARK);
        rect(3, 2, 1, 12, STEEL_LIGHT);
        rect(12, 2, 1, 12, STEEL_DARK);
        rect(3, 5, 10, 1, STEEL_DARK);          // hoops
        rect(3, 10, 10, 1, STEEL_DARK);
        rect(3, 7, 10, 2, PAINT);               // the painted band
        rect(3, 7, 10, 1, 0xFFD9594A);
        rect(6, 1, 4, 1, STEEL_DARK);           // filler neck
        rect(7, 0, 2, 1, COPPER);
    }

    /** The bed on its own, as a bench part: planks in an iron frame. */
    static void bedIcon() {
        fresh();
        rect(1, 4, 14, 9, WOOD);
        rect(1, 4, 14, 1, WOOD_LIGHT);              // the top edge catching light
        rect(1, 12, 14, 1, WOOD_DARK);
        for (int y = 6; y < 12; y += 2) {           // the plank joints
            rect(2, y, 12, 1, WOOD_DARK);
        }
        rect(1, 4, 1, 9, STEEL);                    // the frame down each side
        rect(14, 4, 1, 9, STEEL_DARK);
        rect(1, 3, 14, 1, STEEL);                   // and the rail along the top
        rect(1, 13, 14, 1, STEEL_DARK);
    }

    /** The pickup item: the same cab, with an open bed behind it. */
    static void pickupIcon() {
        fresh();
        rect(1, 11, 14, 2, STEEL_DARK);             // chassis rail
        rect(1, 5, 6, 6, PAINT);                    // the cab
        rect(1, 5, 6, 1, 0xFFD9594A);
        rect(2, 6, 4, 3, GLASS);
        rect(7, 8, 8, 3, PAINT);                    // the bed's outer wall
        rect(7, 8, 8, 1, 0xFFD9594A);
        rect(8, 7, 6, 1, WOOD);                     // the load showing over it
        rect(8, 6, 3, 1, WOOD_DARK);
        rect(14, 7, 1, 4, PAINT_DARK);              // the tailgate
        for (int x : new int[] {2, 11}) {           // wheels
            rect(x, 13, 3, 2, RUBBER);
            px(x + 1, 13, RUBBER_LIGHT);
        }
    }

    /** The tanker item: a cab with a barrel on the back, seen from the side. */
    static void tankerIcon() {
        fresh();
        rect(1, 11, 14, 2, STEEL_DARK);             // chassis rail
        rect(1, 5, 6, 6, PAINT);                    // the cab
        rect(1, 5, 6, 1, 0xFFD9594A);
        rect(2, 6, 4, 3, GLASS);
        rect(7, 6, 8, 5, STEEL);                    // the barrel
        rect(7, 6, 8, 1, STEEL_LIGHT);
        rect(7, 10, 8, 1, STEEL_DARK);
        rect(9, 6, 1, 5, STEEL_DARK);               // ribs
        rect(12, 6, 1, 5, STEEL_DARK);
        rect(7, 8, 8, 1, PAINT);                    // the painted stripe
        rect(10, 4, 3, 2, STEEL_DARK);              // filler cap
        for (int x : new int[] {2, 8, 12}) {        // wheels
            rect(x, 13, 3, 2, RUBBER);
            px(x + 1, 13, RUBBER_LIGHT);
        }
    }


    // ---- the one-way valve ----

    /** The valve body: a ribbed casing with a hand wheel on the side. */
    static void valveBody() {
        fresh();
        rect(0, 0, 16, 16, STEEL_DARK);
        rect(1, 1, 14, 14, STEEL);
        rect(1, 1, 14, 1, STEEL_LIGHT);
        rect(4, 4, 8, 8, COPPER_DARK);
        rect(5, 5, 6, 6, COPPER);
        rect(5, 5, 6, 1, 0xFFE09265);
        // The wheel: a ring with four spokes, in the middle of the casing.
        rect(7, 4, 2, 1, STEEL_LIGHT);
        rect(7, 11, 2, 1, STEEL_LIGHT);
        rect(4, 7, 1, 2, STEEL_LIGHT);
        rect(11, 7, 1, 2, STEEL_LIGHT);
        rect(7, 7, 2, 2, STEEL_DARK);
    }

    /**
     * The mouth. The chevrons point out of the block, which is the only way to
     * read a valve's direction once it is in a wall of pipe.
     */
    static void valveFront(boolean shut) {
        fresh();
        rect(0, 0, 16, 16, STEEL_DARK);
        rect(1, 1, 14, 14, STEEL);
        rect(1, 1, 14, 1, STEEL_LIGHT);
        int mark = shut ? 0xFFB3261E : HAZARD;
        int markDark = shut ? 0xFF6E1710 : HAZARD_DARK;
        rect(3, 3, 10, 10, markDark);
        rect(4, 4, 8, 8, mark);
        if (shut) {
            // A bar across it: redstone has this one closed.
            rect(3, 7, 10, 2, markDark);
            rect(3, 7, 10, 1, 0xFFE8766B);
        } else {
            for (int i = 0; i < 3; i++) {
                rect(5 + i, 5 - i + 2, 1, 1, markDark);
                rect(5 + i, 10 + i - 2, 1, 1, markDark);
                rect(8 - i + 2, 5 - i + 2, 1, 1, markDark);
                rect(8 - i + 2, 10 + i - 2, 1, 1, markDark);
            }
            rect(6, 7, 4, 2, markDark);
        }
    }

    /**
     * One valve model, pointing north, open or shut: the pipe straight
     * through with a bolted flange at each end, a gate body on it, the bonnet
     * and stem on top and a handwheel, yellow open and red shut (the colour
     * comes off the front texture, which is yellow or red at its corner).
     */
    static String valveModel(boolean shut) {
        String front = shut ? "car:block/fuel_valve_front_shut" : "car:block/fuel_valve_front";
        StringBuilder b = new StringBuilder();
        b.append("{\n  \"parent\": \"minecraft:block/block\",\n  \"textures\": {\n")
                .append("    \"particle\": \"car:block/fuel_valve\",\n")
                .append("    \"body\": \"car:block/fuel_valve\",\n")
                .append("    \"front\": \"").append(front).append("\",\n")
                .append("    \"pipe\": \"car:block/fuel_pipe\",\n")
                .append("    \"core\": \"car:block/fuel_pipe_core\"\n  },\n  \"elements\": [\n");
        String steel = "[1, 4, 3, 6]";
        String wheel = "[4, 4, 5, 5]";
        java.util.List<String> els = new java.util.ArrayList<>();
        els.add(tube(1, 15, false));
        els.add(flange(0, "north"));
        els.add(flange(15, "south"));
        // the gate body and its cap, faced with the valve's own plate
        els.add(cuboid(4.5, 4.5, 5, 11.5, 11.5, 11, "#body", "[4, 4, 12, 12]", null));
        els.add(cuboid(5.5, 11.5, 5.5, 10.5, 12.5, 10.5, "#body", steel, null));
        els.add(cuboid(6.5, 12.5, 6.5, 9.5, 13.5, 9.5, "#body", steel, null));
        els.add(cuboid(7.5, 13.5, 7.5, 8.5, 14.6, 8.5, "#core", "[0, 0, 1, 1]", null));
        // the handwheel: a rim, two spokes and the nut
        els.add(cuboid(4.5, 14.6, 4.5, 11.5, 15.4, 5.3, "#front", wheel, null));
        els.add(cuboid(4.5, 14.6, 10.7, 11.5, 15.4, 11.5, "#front", wheel, null));
        els.add(cuboid(4.5, 14.6, 5.3, 5.3, 15.4, 10.7, "#front", wheel, null));
        els.add(cuboid(10.7, 14.6, 5.3, 11.5, 15.4, 10.7, "#front", wheel, null));
        els.add(cuboid(5.3, 14.8, 7.6, 10.7, 15.2, 8.4, "#front", wheel, null));
        els.add(cuboid(7.6, 14.8, 5.3, 8.4, 15.2, 10.7, "#front", wheel, null));
        els.add(cuboid(7.2, 14.6, 7.2, 8.8, 15.8, 8.8, "#core", "[0, 0, 1, 1]", null));
        b.append(String.join(",\n", els));
        return b.append("\n  ]\n}").toString();
    }

    /** A bolted flange one pixel thick at {@code z}, its outer face culled against {@code cull}. */
    static String flange(double z, String cull) {
        return cuboid(4.5, 4.5, z, 11.5, 11.5, z + 1, "#core", "[0, 0, 2, 2]", cull);
    }

    /** A box with one small patch of texture on all six faces. */
    static String cuboid(double x1, double y1, double z1, double x2, double y2, double z2,
                         String texture, String uv, String cull) {
        StringBuilder faces = new StringBuilder();
        for (String side : new String[]{"north", "south", "west", "east", "up", "down"}) {
            if (faces.length() > 0) {
                faces.append(",\n");
            }
            faces.append("        \"").append(side).append("\": { \"uv\": ").append(uv)
                    .append(", \"texture\": \"").append(texture).append("\"")
                    .append(side.equals(cull) ? ", \"cullface\": \"" + cull + "\"" : "").append(" }");
        }
        return "    {\n      \"from\": [" + n(x1) + ", " + n(y1) + ", " + n(z1) + "],\n      \"to\": ["
                + n(x2) + ", " + n(y2) + ", " + n(z2) + "],\n      \"faces\": {\n" + faces + "\n      }\n    }";
    }

    static String n(double v) {
        return v == Math.rint(v) ? Integer.toString((int) v) : Double.toString(v);
    }

    /**
     * Six directions times open and shut. The x turns are the pipe's own:
     * x:270 takes a north-pointing model to up, x:90 takes it to down.
     */
    static String valveBlockstate() {
        String[][] turns = {
                {"north", ""},
                {"east", ", \"y\": 90"},
                {"south", ", \"y\": 180"},
                {"west", ", \"y\": 270"},
                {"up", ", \"x\": 270"},
                {"down", ", \"x\": 90"},
        };
        StringBuilder b = new StringBuilder("{\n  \"variants\": {\n");
        boolean first = true;
        for (String[] turn : turns) {
            for (boolean shut : new boolean[] {false, true}) {
                if (!first) {
                    b.append(",\n");
                }
                first = false;
                b.append("    \"facing=").append(turn[0]).append(",powered=").append(shut)
                        .append("\": { \"model\": \"car:block/fuel_valve")
                        .append(shut ? "_shut" : "").append("\"").append(turn[1]).append(" }");
            }
        }
        return b.append("\n  }\n}").toString();
    }

    // ---- the yard tank ----

    static final int PETROL = 0xFFD9A441;
    static final int PETROL_DARK = 0xFFA97A24;
    static final int GLASS_DARK = 0xFF23282B;

    /**
     * The tank's side, with the sight glass filled to {@code level} of
     * {@code marks}. Five of these are drawn and the blockstate picks one, so
     * the level is a plain model swap rather than anything rendered per frame.
     */

    // ---- the parts a diagnosis sends you shopping for ----

    /** A pleated paper element in a steel can: the airbox. */
    static void airFilter() {
        fresh();
        for (int x = 0; x < 16; x++) {
            for (int y = 0; y < 16; y++) {
                double d = Math.sqrt((x - 7.5) * (x - 7.5) + (y - 7.5) * (y - 7.5));
                if (d > 7.4) {
                    continue;
                }
                if (d > 6.0) {
                    px(x, y, y < 7 ? STEEL_LIGHT : STEEL);
                } else if (d > 2.6) {
                    // the pleats: alternating columns of paper and shadow
                    px(x, y, x % 2 == 0 ? PAPER : PAPER_DARK);
                } else {
                    px(x, y, y < 7 ? STEEL : STEEL_DARK);
                }
            }
        }
        rect(7, 0, 2, 2, STEEL_DARK);       // intake neck
    }

    /** Four plugs in a row, ceramic over thread. */
    static void sparkPlugs() {
        fresh();
        for (int i = 0; i < 4; i++) {
            int x = 1 + i * 4;
            rect(x, 1, 2, 2, STEEL_LIGHT);      // terminal
            rect(x, 3, 3, 5, CERAMIC);          // insulator
            px(x + 2, 3, CERAMIC_DARK);
            rect(x, 7, 3, 1, CERAMIC_DARK);
            rect(x, 8, 3, 3, STEEL);            // hex and thread
            px(x + 2, 9, STEEL_DARK);
            px(x + 2, 10, STEEL_DARK);
            rect(x, 11, 2, 3, STEEL_DARK);      // electrode
            px(x, 14, STEEL_LIGHT);
        }
    }

    /** A spin-on can, ribbed at the top, dirty oil inside. */
    static void oilFilter() {
        fresh();
        rect(4, 2, 8, 2, STEEL);            // sealing plate
        rect(4, 2, 8, 1, STEEL_LIGHT);
        rect(5, 4, 6, 1, STEEL_DARK);
        rect(4, 5, 8, 9, BLUE);             // the can
        rect(4, 5, 1, 9, BLUE_DARK);
        rect(11, 5, 1, 9, BLUE_DARK);
        rect(4, 13, 8, 1, BLUE_DARK);
        for (int y = 6; y < 13; y += 2) {
            rect(5, y, 6, 1, BLUE_DARK);    // the ribs you grip
        }
        rect(6, 8, 4, 3, OIL);              // the label window
        px(7, 9, HAZARD);
        px(8, 9, HAZARD);
    }

    /** A core of fins in a frame, with a hose stub at each corner. */
    static void radiator() {
        fresh();
        rect(1, 2, 14, 12, STEEL_DARK);     // frame
        rect(2, 3, 12, 10, BLUE_DARK);      // core
        for (int x = 3; x < 14; x += 2) {
            rect(x, 3, 1, 10, BLUE);        // fins
        }
        rect(1, 2, 14, 1, STEEL_LIGHT);     // top tank
        rect(1, 13, 14, 1, STEEL);          // bottom tank
        rect(0, 3, 1, 3, STEEL);            // inlet
        rect(15, 10, 1, 3, STEEL);          // outlet
        rect(6, 0, 3, 2, COPPER);           // filler cap
        px(7, 1, COPPER_DARK);
    }

    /** A black box with two terminals, one of them unmistakably positive. */
    static void battery() {
        fresh();
        rect(2, 4, 12, 10, RUBBER_LIGHT);
        rect(2, 4, 12, 1, STEEL_DARK);
        rect(2, 13, 12, 1, RUBBER);
        rect(3, 6, 10, 6, RUBBER);          // the cell block
        rect(4, 7, 4, 4, GREEN);            // charge window
        rect(9, 7, 3, 4, HAZARD_DARK);      // the label
        rect(3, 2, 3, 2, PAINT);            // + terminal
        px(4, 1, PAINT);
        rect(10, 2, 3, 2, STEEL_LIGHT);     // - terminal
        px(11, 1, STEEL_LIGHT);
        px(4, 3, PAINT_DARK);
        px(11, 3, STEEL);
    }

    /** A drum of copper windings with a pulley on the nose. */
    static void alternator() {
        fresh();
        for (int x = 0; x < 16; x++) {
            for (int y = 0; y < 16; y++) {
                double d = Math.sqrt((x - 6.0) * (x - 6.0) + (y - 7.5) * (y - 7.5));
                if (d > 6.2) {
                    continue;
                }
                if (d > 4.8) {
                    px(x, y, y < 7 ? STEEL_LIGHT : STEEL);      // housing
                } else if (d > 2.0) {
                    px(x, y, x % 2 == 0 ? COPPER : COPPER_DARK); // windings
                } else {
                    px(x, y, STEEL_DARK);                        // shaft
                }
            }
        }
        rect(12, 5, 3, 6, STEEL);           // pulley
        rect(12, 7, 3, 2, STEEL_DARK);
        rect(0, 6, 2, 2, BRASS);            // the plug
        px(1, 9, BRASS);
    }

    /** A tube with a universal joint at each end, laid corner to corner. */
    static void driveshaft() {
        fresh();
        for (int i = 0; i < 12; i++) {
            int x = 2 + i;
            int y = 12 - i;
            rect(x, y, 2, 2, i % 2 == 0 ? STEEL : STEEL_LIGHT);
        }
        rect(1, 11, 3, 3, STEEL_DARK);      // gearbox joint
        px(2, 12, COPPER);
        rect(12, 1, 3, 3, STEEL_DARK);      // axle joint
        px(13, 2, COPPER);
        rect(6, 6, 3, 3, STEEL_DARK);       // centre bearing
    }

    /** A friction disc, splined in the middle, with its springs showing. */
    static void clutch() {
        fresh();
        for (int x = 0; x < 16; x++) {
            for (int y = 0; y < 16; y++) {
                double d = Math.sqrt((x - 7.5) * (x - 7.5) + (y - 7.5) * (y - 7.5));
                if (d > 7.4) {
                    continue;
                }
                if (d > 4.6) {
                    px(x, y, y < 7 ? WOOD_LIGHT : WOOD);        // the lining
                } else if (d > 3.6) {
                    px(x, y, STEEL_DARK);
                } else if (d > 1.4) {
                    px(x, y, y < 7 ? STEEL_LIGHT : STEEL);      // hub plate
                } else {
                    px(x, y, STEEL_DARK);                        // splines
                }
            }
        }
        // rivets around the lining, which is how you tell a disc from a disc
        px(7, 1, STEEL_DARK);
        px(2, 4, STEEL_DARK);
        px(13, 4, STEEL_DARK);
        px(2, 11, STEEL_DARK);
        px(13, 11, STEEL_DARK);
        px(7, 14, STEEL_DARK);
        rect(6, 6, 4, 1, WOOD_DARK);
    }

    /** Two pads face to face, friction material inwards, as they sit on a disc. */
    static void brakePads() {
        fresh();
        // the near pad
        rect(2, 2, 4, 12, STEEL);           // backing plate
        rect(2, 2, 4, 1, STEEL_LIGHT);
        rect(2, 13, 4, 1, STEEL_DARK);
        rect(6, 3, 3, 10, WOOD);            // friction material
        rect(6, 3, 1, 10, WOOD_LIGHT);
        rect(8, 3, 1, 10, WOOD_DARK);
        px(3, 5, STEEL_DARK);               // the pin holes
        px(3, 10, STEEL_DARK);
        // the far pad, offset so the pair reads as a pair
        rect(13, 4, 3, 10, STEEL_DARK);
        rect(13, 4, 3, 1, STEEL);
        rect(10, 5, 3, 8, WOOD_DARK);
        rect(12, 5, 1, 8, WOOD);
        px(14, 7, STEEL);
        px(14, 11, STEEL);
    }

    /** A vented disc: two faces, drilled, with a hub in the middle. */
    static void brakeDiscs() {
        fresh();
        for (int x = 0; x < 16; x++) {
            for (int y = 0; y < 16; y++) {
                double d = Math.sqrt((x - 7.5) * (x - 7.5) + (y - 7.5) * (y - 7.5));
                if (d > 7.5) {
                    continue;
                }
                if (d > 6.6) {
                    px(x, y, STEEL_DARK);                        // outer edge
                } else if (d > 3.2) {
                    px(x, y, y < 7 ? STEEL_LIGHT : STEEL);       // swept face
                } else if (d > 2.4) {
                    px(x, y, STEEL_DARK);
                } else {
                    px(x, y, y < 7 ? STEEL : STEEL_DARK);        // hub
                }
            }
        }
        // drilled holes, in a ring
        px(7, 2, STEEL_DARK);
        px(12, 5, STEEL_DARK);
        px(11, 11, STEEL_DARK);
        px(4, 11, STEEL_DARK);
        px(3, 5, STEEL_DARK);
        px(6, 7, OIL);
        px(9, 8, OIL);
    }

    /** A rack across the car, a tie rod end at each side, pinion on top. */
    static void steeringRack() {
        fresh();
        rect(1, 7, 14, 3, STEEL);
        rect(1, 7, 14, 1, STEEL_LIGHT);
        rect(1, 9, 14, 1, STEEL_DARK);
        for (int x = 4; x < 12; x += 2) {
            px(x, 7, STEEL_DARK);           // the teeth
        }
        rect(6, 3, 4, 4, STEEL_DARK);       // pinion housing
        rect(7, 1, 2, 2, COPPER);           // the column stub
        rect(0, 6, 2, 5, RUBBER);           // boot
        rect(14, 6, 2, 5, RUBBER);
        px(1, 8, RUBBER_LIGHT);
        px(14, 8, RUBBER_LIGHT);
        rect(2, 11, 2, 2, STEEL_LIGHT);     // tie rod ends
        rect(12, 11, 2, 2, STEEL_LIGHT);
    }

    /** A silencer with a pipe in and a tailpipe out. */
    static void exhaust() {
        fresh();
        rect(0, 3, 6, 2, STEEL);            // downpipe
        rect(0, 3, 6, 1, STEEL_LIGHT);
        rect(5, 4, 2, 4, STEEL);
        rect(3, 6, 11, 6, STEEL_DARK);      // the box
        rect(3, 6, 11, 1, STEEL);
        rect(3, 11, 11, 1, OIL);
        for (int x = 5; x < 13; x += 3) {
            rect(x, 8, 1, 2, STEEL);        // the seams
        }
        rect(13, 12, 3, 2, STEEL);          // tailpipe
        rect(14, 13, 2, 1, OIL);
        px(15, 12, STEEL_LIGHT);
    }

    /**
     * The clipboard. Paper, a clip, and three bars that are the whole reason
     * the item exists.
     */
    static void diagnosticSheet() {
        fresh();
        rect(2, 1, 12, 14, WOOD_DARK);      // the board
        rect(3, 3, 10, 11, PAPER);
        rect(3, 13, 10, 1, PAPER_DARK);
        rect(5, 0, 6, 3, STEEL);            // the clip
        rect(5, 0, 6, 1, STEEL_LIGHT);
        rect(6, 2, 4, 1, STEEL_DARK);
        // the readout: a full bar, a half one, and one that is nearly gone
        rect(4, 5, 8, 1, PAPER_DARK);
        rect(4, 5, 8, 1, GREEN);
        rect(4, 8, 8, 1, PAPER_DARK);
        rect(4, 8, 4, 1, HAZARD);
        rect(4, 11, 8, 1, PAPER_DARK);
        rect(4, 11, 2, 1, PAINT);
    }

    // ---- models and blockstates ----

    static void write(String path, String body) throws Exception {
        File out = new File(path);
        out.getParentFile().mkdirs();
        try (PrintWriter w = new PrintWriter(out, "UTF-8")) {
            w.println(body);
        }
        System.out.println("wrote " + path);
    }



    // ---- the pipe's boxes ----

    static final String PIPE_TEXTURES = "    \"particle\": \"car:block/fuel_pipe\",\n"
            + "    \"pipe\": \"car:block/fuel_pipe\",\n"
            + "    \"core\": \"car:block/fuel_pipe_core\"\n";

    /**
     * A length of tube pointing north, from {@code z1} to {@code z2}. Its four
     * long faces take their length along the skin's width (the up and down
     * faces turned a quarter to do it); {@code caps} adds end faces.
     */
    static String tube(int z1, int z2, boolean caps) {
        int len = z2 - z1;
        String side = "{ \"uv\": [0, 5, " + len + ", 11], \"texture\": \"#pipe\" }";
        String flat = "{ \"uv\": [0, 5, " + len + ", 11], \"rotation\": 90, \"texture\": \"#pipe\" }";
        String cap = "{ \"uv\": [5, 5, 11, 11], \"texture\": \"#core\" }";
        return "    {\n      \"from\": [5, 5, " + z1 + "],\n      \"to\": [11, 11, " + z2 + "],\n"
                + "      \"faces\": {\n"
                + "        \"west\": " + side + ",\n"
                + "        \"east\": " + side + ",\n"
                + "        \"up\": " + flat + ",\n"
                + "        \"down\": " + flat
                + (caps ? ",\n        \"north\": " + cap + ",\n        \"south\": " + cap : "")
                + "\n      }\n    }";
    }

    static String fittingBox() {
        StringBuilder faces = new StringBuilder();
        for (String side : new String[]{"north", "south", "west", "east", "up", "down"}) {
            if (faces.length() > 0) {
                faces.append(",\n");
            }
            faces.append("        \"").append(side).append("\": { \"uv\": [4, 4, 12, 12], \"texture\": \"#core\" }");
        }
        return "    {\n      \"from\": [4.5, 4.5, 4.5],\n      \"to\": [11.5, 11.5, 11.5],\n      \"faces\": {\n"
                + faces + "\n      }\n    }";
    }

    static String opposite(String side) {
        switch (side) {
            case "north": return "south";
            case "south": return "north";
            case "west": return "east";
            case "east": return "west";
            case "up": return "down";
            default: return "up";
        }
    }

    static String blockModel(String... elements) {
        return "{\n  \"parent\": \"minecraft:block/block\",\n  \"textures\": {\n" + PIPE_TEXTURES
                + "  },\n  \"elements\": [\n" + String.join(",\n", elements) + "\n  ]\n}";
    }

    /**
     * The pipe's blockstate: an arm from the middle out to every side that
     * connected, and a fitting wherever the pipe is not simply running
     * straight through (FuelPipeBlock works that out). An arm is modelled
     * pointing north once and turned; x:270 takes it to up and x:90 to down.
     */
    static String pipeBlockstate() {
        StringBuilder b = new StringBuilder("{\n  \"multipart\": [\n");
        b.append("    { \"when\": { \"fitting\": true }, \"apply\": { \"model\": \"car:block/fuel_pipe_core\" } }");
        String[][] arms = {
                {"north", ""},
                {"east", ", \"y\": 90"},
                {"south", ", \"y\": 180"},
                {"west", ", \"y\": 270"},
                {"up", ", \"x\": 270"},
                {"down", ", \"x\": 90"},
        };
        for (String[] arm : arms) {
            b.append(",\n    { \"when\": { \"").append(arm[0]).append("\": true }, ")
                    .append("\"apply\": { \"model\": \"car:block/fuel_pipe_arm\"")
                    .append(arm[1]).append(" } }");
        }
        return b.append("\n  ]\n}").toString();
    }


    // ---- the lift, which now actually lifts ----

    /** Must match {@code VehicleLiftBlockEntity.MAX_LEVEL}. */
    static final int LIFT_LEVELS = 4;

    /** One cube element, letting Minecraft work the UVs out from the box. */
    static String liftBox(double x0, double y0, double z0, double x1, double y1, double z1,
                          String side, String top, String bottom) {
        return """
                    {
                      "from": [%s, %s, %s],
                      "to": [%s, %s, %s],
                      "faces": {
                        "down":  {"texture": "#%s"},
                        "up":    {"texture": "#%s"},
                        "north": {"texture": "#%s"},
                        "south": {"texture": "#%s"},
                        "west":  {"texture": "#%s"},
                        "east":  {"texture": "#%s"}
                      }
                    }"""
                .formatted(trim(x0), trim(y0), trim(z0), trim(x1), trim(y1), trim(z1),
                        bottom, top, side, side, side, side);
    }

    static String trim(double value) {
        return value == Math.rint(value) ? String.valueOf((int) value) : String.valueOf(value);
    }


    public static void main(String[] args) throws Exception {
        carBody();
        save(ASSETS + "textures/item/car_body.png");
        engine();
        save(ASSETS + "textures/item/engine.png");
        transmission();
        save(ASSETS + "textures/item/transmission.png");
        suspension();
        save(ASSETS + "textures/item/suspension.png");
        wheel();
        save(ASSETS + "textures/item/wheel.png");
        airFilter();
        save(ASSETS + "textures/item/air_filter.png");
        sparkPlugs();
        save(ASSETS + "textures/item/spark_plugs.png");
        oilFilter();
        save(ASSETS + "textures/item/oil_filter.png");
        radiator();
        save(ASSETS + "textures/item/radiator.png");
        battery();
        save(ASSETS + "textures/item/battery.png");
        alternator();
        save(ASSETS + "textures/item/alternator.png");
        driveshaft();
        save(ASSETS + "textures/item/driveshaft.png");
        clutch();
        save(ASSETS + "textures/item/clutch.png");
        brakePads();
        save(ASSETS + "textures/item/brake_pads.png");
        brakeDiscs();
        save(ASSETS + "textures/item/brake_discs.png");
        steeringRack();
        save(ASSETS + "textures/item/steering_rack.png");
        exhaust();
        save(ASSETS + "textures/item/exhaust.png");
        diagnosticSheet();
        save(ASSETS + "textures/item/diagnostic_sheet.png");
        // The models are not written here: tools/GenItemModels.java builds
        // the items in 3D, and these icons are kept only as the flat originals.

        liftTop();
        save(ASSETS + "textures/block/vehicle_lift_top.png");
        liftSide();
        save(ASSETS + "textures/block/vehicle_lift_side.png");
        liftBottom();
        save(ASSETS + "textures/block/vehicle_lift_bottom.png");

        // The lift's blockstate and models are tools/gen_pump_lift.py's.
        refineryTop();
        save(ASSETS + "textures/block/fuel_refinery_top.png");
        refineryFront(false);
        save(ASSETS + "textures/block/fuel_refinery_front.png");
        refineryFront(true);
        save(ASSETS + "textures/block/fuel_refinery_front_lit.png");
        refinerySide();
        save(ASSETS + "textures/block/fuel_refinery_side.png");


        pipeSkin();
        save(ASSETS + "textures/block/fuel_pipe.png");
        pipeCore();
        save(ASSETS + "textures/block/fuel_pipe_core.png");

        write(ASSETS + "models/block/fuel_pipe_core.json", blockModel(fittingBox()));
        write(ASSETS + "models/block/fuel_pipe_arm.json", blockModel(tube(1, 8, false), flange(0, "north")));
        write(ASSETS + "models/block/fuel_pipe_inventory.json",
                blockModel(tube(1, 15, false), flange(0, "north"), flange(15, "south")));
        write(ASSETS + "blockstates/fuel_pipe.json", pipeBlockstate());
        write(ASSETS + "items/fuel_pipe.json",
                "{\n  \"model\": {\n    \"type\": \"minecraft:model\",\n"
                        + "    \"model\": \"car:block/fuel_pipe_inventory\"\n  }\n}");

        // The yard tank is tools/gen_tank.py's.

        barrelIcon();
        save(ASSETS + "textures/item/tanker_barrel.png");

        valveBody();
        save(ASSETS + "textures/block/fuel_valve.png");
        valveFront(false);
        save(ASSETS + "textures/block/fuel_valve_front.png");
        valveFront(true);
        save(ASSETS + "textures/block/fuel_valve_front_shut.png");
        write(ASSETS + "models/block/fuel_valve.json", valveModel(false));
        write(ASSETS + "models/block/fuel_valve_shut.json", valveModel(true));
        write(ASSETS + "blockstates/fuel_valve.json", valveBlockstate());
        write(ASSETS + "items/fuel_valve.json",
                "{\n  \"model\": {\n    \"type\": \"minecraft:model\",\n"
                        + "    \"model\": \"car:block/fuel_valve\"\n  }\n}");

        tankerIcon();
        save(ASSETS + "textures/item/fuel_tanker.png");

        bedIcon();
        save(ASSETS + "textures/item/pickup_bed.png");

        pickupIcon();
        save(ASSETS + "textures/item/pickup.png");

        write(ASSETS + "items/vehicle_lift.json",
                "{\n  \"model\": {\n    \"type\": \"minecraft:model\",\n"
                        + "    \"model\": \"car:block/vehicle_lift\"\n  }\n}");
    }
}
