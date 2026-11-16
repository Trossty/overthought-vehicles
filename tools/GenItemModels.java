import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.List;
import java.util.Locale;

/**
 * Builds the items as things rather than as pictures of things.
 *
 * <p><b>Superseded for the parts and tools:</b> the engines, engine parts,
 * chassis parts, wheels and tools are now drawn in Blockbench by
 * {@code tools/items/*.js} (see {@code tools/blocks/kit.js}). Running this
 * would overwrite those models with the old box versions.
 *
 * <p>Every part, the jerry can, the clipboard and the three vehicles get a
 * model made of boxes, so that an engine in the hand is an engine and not a
 * card with an engine printed on it. Run from the project root:
 *
 * <pre>java tools/GenItemModels.java [preview.png]</pre>
 *
 * <p>With a file name it also draws every model the way the inventory will,
 * which is the quick way to look at a change: the game takes minutes to start
 * and this takes a second.
 *
 * <p>All the models share one sheet of materials, {@code item/materials.png}:
 * a grid of 16x16 swatches, one per material, and every face of every box
 * takes a piece of its material's swatch the size of the face. So a texel is
 * a sixteenth of a block here as it is everywhere else in the game, and the
 * parts sit next to vanilla's blocks without looking like they came from
 * somewhere else.
 *
 * <p>The models are written facing north: what you would call the front of
 * the thing is on its -z side, which is the face the inventory shows on the
 * right.
 */
public class GenItemModels {

    static final String ASSETS = "src/main/resources/assets/car/";

    /** One swatch is one block face's worth of texels. */
    static final int SWATCH = 16;
    static final int COLUMNS = 8;
    static final int SHEET = SWATCH * COLUMNS;

    /** How a swatch is filled in. */
    enum Fill {
        /** One colour, roughened. */
        PLAIN,
        /** Two colours alternating along u: pleats, fins, tread. */
        ACROSS,
        /** Two colours alternating along v: threads, bellows, planks. */
        DOWN
    }

    enum Mat {
        STEEL(0x8A8F94),
        STEEL_LIGHT(0xB9BEC3),
        STEEL_DARK(0x4E5358),
        COPPER(0xC46A3B),
        COPPER_DARK(0x8A472A),
        RUBBER(0x181818),
        RUBBER_LIGHT(0x2C2C2C),
        GLASS(0x35505C),
        PAINT(0xC0392B),
        PAINT_DARK(0x7E2619),
        HAZARD(0xE8B021),
        OIL(0x1F2326),
        WOOD(0x9A6B3F),
        WOOD_DARK(0x6B4A2C),
        CERAMIC(0xE6E1D8),
        PAPER(0xEDE8D9),
        PAPER_DARK(0xC2BBA7),
        BLUE(0x2F6E9E),
        BLUE_DARK(0x1C4A6D),
        GREEN(0x4C9A43),
        CASE(0x23262A),
        CASE_LIGHT(0x3A3E43),
        FRICTION(0x8C6A45),
        FRICTION_DARK(0x4F4034),
        LAMP(0xFFE9A8),
        TAIL(0xB3261E),
        TANK(0x9AA0A6),
        TANK_LIGHT(0xC3C8CC),
        /** Left nearly white, because the game paints it per stack. */
        TINTED(0xECECEC),

        PLEATS(0xE6E1D8, 0xA9A294, Fill.ACROSS),
        FINS(0x2F6E9E, 0x1C4A6D, Fill.ACROSS),
        WINDING(0xC46A3B, 0x8A472A, Fill.ACROSS),
        TREAD(0x242424, 0x0E0E0E, Fill.ACROSS),
        VENTS(0x4E5358, 0x1F2326, Fill.ACROSS),
        THREAD(0x6A6F74, 0x3C4045, Fill.DOWN),
        RIBBED(0xE6E1D8, 0xB4AEA2, Fill.DOWN),
        BELLOWS(0x2C2C2C, 0x101010, Fill.DOWN),
        PLANKS(0x9A6B3F, 0x7A5330, Fill.DOWN),
        COIL(0xD9742F, 0x9C4E1F, Fill.ACROSS),
        HAZARD_DARK(0xA87A12),
        SIPES(0x3A3A3A, 0xC8D4DA, Fill.ACROSS),
        GAUZE(0xC0392B, 0x7E2619, Fill.ACROSS);

        final int rgb;
        final int other;
        final Fill fill;

        Mat(int rgb) {
            this(rgb, rgb, Fill.PLAIN);
        }

        Mat(int rgb, int other, Fill fill) {
            this.rgb = rgb;
            this.other = other;
            this.fill = fill;
        }
    }

    // Faces, in the order the arrays below are indexed.
    static final int DOWN = 0;
    static final int UP = 1;
    static final int NORTH = 2;
    static final int SOUTH = 3;
    static final int WEST = 4;
    static final int EAST = 5;
    static final String[] FACE_NAMES = {"down", "up", "north", "south", "west", "east"};

    /** One box, with a material for each face it shows. */
    static class Box {
        double[] from = new double[3];
        double[] to = new double[3];
        Mat[] faces = new Mat[6];
        boolean[] turned = new boolean[6];
        char axis = 0;
        double angle;
        double[] origin = new double[3];

        Box face(int which, Mat mat) {
            this.faces[which] = mat;
            return this;
        }

        /** Turns the box about its own middle. Only ±22.5 and ±45 exist. */
        Box rot(char axis, double angle) {
            this.axis = axis;
            this.angle = angle;
            for (int i = 0; i < 3; i++) {
                this.origin[i] = (this.from[i] + this.to[i]) / 2.0;
            }
            return this;
        }
    }

    static class Model {
        final String name;
        final List<Box> boxes = new ArrayList<>();
        /** How the inventory turns it. Blocks use 30, 225. */
        double[] gui = {30, 225, 0};
        /** Whether any face takes the stack's paint. */
        boolean tinted;
        int seed;

        Model(String name) {
            this.name = name;
        }

        /** Faces the inventory more squarely, for things that are a face. */
        Model facing() {
            this.gui = new double[] {18, 203, 0};
            return this;
        }

        /** A box by its middle and its size. */
        Box b(double cx, double cy, double cz, double sx, double sy, double sz, Mat mat) {
            Box box = new Box();
            box.from = new double[] {cx - sx / 2, cy - sy / 2, cz - sz / 2};
            box.to = new double[] {cx + sx / 2, cy + sy / 2, cz + sz / 2};
            for (int i = 0; i < 6; i++) {
                box.faces[i] = mat;
            }
            this.boxes.add(box);
            return box;
        }

        /**
         * A cylinder, as near as boxes get: slabs through the middle, each
         * turned a little further round than the last, whose narrow ends make
         * the sides of a polygon. Eight sides is enough for a pipe; sixteen is
         * worth it for something you look at the round face of.
         *
         * <p>The rim's material runs with u going round and v going along, so
         * ACROSS is pleats or tread and DOWN is bands.
         */
        void cyl(char axis, double cx, double cy, double cz, double radius, double length,
                 Mat rim, Mat cap, int sides) {
            double side = 2.0 * radius * Math.tan(Math.PI / sides);
            double[] angles = sides == 16
                    ? new double[] {0, 22.5, 45, -22.5}
                    : new double[] {0, 45};
            int slab = 0;
            for (double angle : angles) {
                for (int swapped = 0; swapped < 2; swapped++) {
                    // A hair's difference in length, so that the ends of the
                    // slabs are not all in one plane fighting over it.
                    double len = length + slab * 0.012;
                    slab++;
                    double p = swapped == 0 ? radius * 2 : side;
                    double q = swapped == 0 ? side : radius * 2;
                    Box box;
                    int[] caps;
                    int[] narrow;
                    int[] wide;
                    switch (axis) {
                        case 'x' -> {
                            box = b(cx, cy, cz, len, p, q, rim);
                            caps = new int[] {WEST, EAST};
                            narrow = swapped == 0 ? new int[] {DOWN, UP} : new int[] {NORTH, SOUTH};
                            wide = swapped == 0 ? new int[] {NORTH, SOUTH} : new int[] {DOWN, UP};
                            box.turned[DOWN] = box.turned[UP] = true;
                            box.turned[NORTH] = box.turned[SOUTH] = true;
                        }
                        case 'y' -> {
                            box = b(cx, cy, cz, p, len, q, rim);
                            caps = new int[] {DOWN, UP};
                            narrow = swapped == 0 ? new int[] {WEST, EAST} : new int[] {NORTH, SOUTH};
                            wide = swapped == 0 ? new int[] {NORTH, SOUTH} : new int[] {WEST, EAST};
                        }
                        default -> {
                            box = b(cx, cy, cz, p, q, len, rim);
                            caps = new int[] {NORTH, SOUTH};
                            narrow = swapped == 0 ? new int[] {WEST, EAST} : new int[] {DOWN, UP};
                            wide = swapped == 0 ? new int[] {DOWN, UP} : new int[] {WEST, EAST};
                            box.turned[WEST] = box.turned[EAST] = true;
                        }
                    }
                    // The wide faces are inside the polygon and never seen.
                    box.faces[wide[0]] = null;
                    box.faces[wide[1]] = null;
                    box.faces[caps[0]] = cap;
                    box.faces[caps[1]] = cap;
                    if (angle != 0) {
                        box.rot(axis, angle);
                    }
                }
            }
        }
    }

    static final List<Model> MODELS = new ArrayList<>();

    static Model model(String name) {
        Model model = new Model(name);
        model.seed = name.hashCode();
        MODELS.add(model);
        return model;
    }

    // ---- the parts ----

    static void airFilter() {
        Model m = model("air_filter");
        m.cyl('y', 0, 0.5, 0, 6.0, 1.0, Mat.RUBBER_LIGHT, Mat.STEEL_DARK, 16);
        m.cyl('y', 0, 2.75, 0, 5.4, 3.5, Mat.PLEATS, Mat.PLEATS, 16);
        m.cyl('y', 0, 5.0, 0, 6.0, 1.0, Mat.STEEL, Mat.STEEL, 16);
        m.cyl('y', 0, 5.6, 0, 2.4, 0.3, Mat.STEEL_DARK, Mat.OIL, 8);
        m.b(0, 5.9, 0, 0.9, 0.5, 0.9, Mat.STEEL_LIGHT);
    }

    /** The open one: a cone of red gauze on a short metal neck. */
    static void airFilterOpen() {
        Model m = model("air_filter_open");
        m.cyl('y', 0, 0.75, 0, 2.4, 1.5, Mat.STEEL, Mat.OIL, 8);
        m.cyl('y', 0, 1.8, 0, 5.2, 0.6, Mat.STEEL_LIGHT, Mat.STEEL_LIGHT, 16);
        double[] radii = {5.0, 4.5, 4.0, 3.5, 3.0};
        for (int i = 0; i < radii.length; i++) {
            m.cyl('y', 0, 2.85 + i * 1.5, 0, radii[i], 1.5, Mat.GAUZE, Mat.GAUZE, 16);
        }
        m.cyl('y', 0, 9.9, 0, 3.1, 0.6, Mat.STEEL_LIGHT, Mat.STEEL, 16);
    }

    /** Two snails back to back: the cold one bright, the hot one dark. */
    static void turbo() {
        Model m = model("turbo");
        m.cyl('x', -2.6, 0, 0, 4.2, 2.6, Mat.STEEL_LIGHT, Mat.STEEL, 16);
        m.cyl('x', -4.3, 0, 0, 2.3, 1.0, Mat.STEEL, Mat.OIL, 8);
        m.b(-2.6, 3.4, -2.6, 2.2, 3.4, 2.2, Mat.STEEL_LIGHT);
        m.b(-2.6, 5.3, -2.6, 2.8, 0.5, 2.8, Mat.STEEL);
        m.cyl('x', 0, 0, 0, 2.0, 2.8, Mat.STEEL_DARK, Mat.STEEL_DARK, 8);
        m.b(0, 2.2, 0, 1.0, 0.8, 1.0, Mat.COPPER);
        m.cyl('x', 2.6, 0, 0, 3.8, 2.6, Mat.COPPER_DARK, Mat.STEEL_DARK, 16);
        m.cyl('x', 4.3, 0, 0, 2.1, 1.0, Mat.STEEL_DARK, Mat.OIL, 8);
        m.b(2.6, -3.2, 2.2, 2.4, 3.0, 2.4, Mat.COPPER_DARK);
        m.b(2.6, -4.9, 2.2, 3.2, 0.5, 3.2, Mat.STEEL_DARK);
    }

    static void sparkPlugs() {
        Model m = model("spark_plugs");
        for (int i = 0; i < 4; i++) {
            double x = i * 3.2;
            m.b(x, -0.2, 0, 0.4, 0.6, 0.4, Mat.STEEL_LIGHT);
            m.b(x, 1.25, 0, 1.1, 2.5, 1.1, Mat.THREAD);
            m.b(x, 3.1, 0, 2.0, 1.2, 2.0, Mat.STEEL);
            m.b(x, 5.7, 0, 1.4, 4.0, 1.4, Mat.RIBBED);
            m.b(x, 8.2, 0, 0.8, 1.0, 0.8, Mat.STEEL_LIGHT);
        }
    }

    static void oilFilter() {
        Model m = model("oil_filter");
        m.cyl('y', 0, 0.4, 0, 3.7, 0.8, Mat.STEEL, Mat.STEEL_DARK, 16);
        m.cyl('y', 0, 4.3, 0, 3.4, 7.0, Mat.BLUE, Mat.BLUE, 16);
        m.cyl('y', 0, 4.0, 0, 3.5, 1.8, Mat.HAZARD, Mat.HAZARD, 16);
        m.cyl('y', 0, 8.3, 0, 2.5, 1.0, Mat.BLUE_DARK, Mat.BLUE_DARK, 8);
    }

    static void radiator() {
        Model m = model("radiator").facing();
        m.b(0, 5.5, 0, 12, 8, 1.6, Mat.FINS);
        m.b(0, 10.25, 0, 13, 1.5, 2.6, Mat.STEEL);
        m.b(0, 0.75, 0, 13, 1.5, 2.6, Mat.STEEL_DARK);
        m.b(-6.25, 5.5, 0, 0.5, 8, 2.0, Mat.STEEL);
        m.b(6.25, 5.5, 0, 0.5, 8, 2.0, Mat.STEEL);
        m.cyl('y', -3.5, 11.4, 0, 0.9, 0.8, Mat.COPPER, Mat.COPPER, 8);
        m.b(4.5, 10.25, -1.8, 1.2, 1.2, 1.2, Mat.STEEL_DARK);
        m.b(-4.5, 0.75, -1.8, 1.2, 1.2, 1.2, Mat.STEEL_DARK);
    }

    static void battery() {
        Model m = model("battery");
        m.b(0, 3.5, 0, 11, 7, 6.5, Mat.CASE);
        m.b(0, 7.5, 0, 11.4, 1.0, 6.9, Mat.CASE_LIGHT);
        m.b(-3.6, 8.5, -1.2, 1.4, 1.0, 1.4, Mat.PAINT);
        m.b(3.6, 8.5, -1.2, 1.4, 1.0, 1.4, Mat.STEEL_LIGHT);
        for (int i = -1; i <= 1; i++) {
            m.b(i * 2.2, 8.1, 1.6, 1.2, 0.25, 1.2, Mat.STEEL_DARK);
        }
        m.b(-2.5, 3.8, -3.3, 4, 3, 0.15, Mat.GREEN);
        m.b(2.5, 3.8, -3.3, 4, 3, 0.15, Mat.HAZARD);
        m.b(5.55, 3.8, 0, 0.15, 3, 4, Mat.GREEN);
    }

    static void alternator() {
        Model m = model("alternator");
        m.cyl('z', 0, 0, 2.2, 5.0, 1.8, Mat.STEEL, Mat.STEEL, 16);
        m.cyl('z', 0, 0, 0, 4.6, 2.6, Mat.WINDING, Mat.COPPER_DARK, 16);
        m.cyl('z', 0, 0, -2.2, 5.0, 1.8, Mat.STEEL, Mat.STEEL_LIGHT, 16);
        m.cyl('z', 0, 0, -3.6, 2.8, 1.0, Mat.STEEL_DARK, Mat.STEEL_DARK, 8);
        m.cyl('z', 0, 0, -4.5, 1.9, 0.9, Mat.RUBBER, Mat.STEEL_DARK, 8);
        m.b(0, 0, -5.1, 0.8, 0.8, 0.5, Mat.STEEL_LIGHT);
        m.b(0, 5.4, 0, 2.0, 1.6, 1.6, Mat.STEEL);
        m.b(0, -5.4, 0, 2.0, 1.6, 1.6, Mat.STEEL);
        m.b(2.5, 2.5, 3.3, 1.5, 1.5, 0.6, Mat.COPPER);
    }

    static void driveshaft() {
        Model m = model("driveshaft");
        m.cyl('x', 0, 0, 0, 1.0, 11, Mat.STEEL_DARK, Mat.STEEL_DARK, 8);
        m.b(1.5, 1.1, 0, 1.5, 0.3, 0.8, Mat.STEEL_LIGHT);
        for (int side = -1; side <= 1; side += 2) {
            m.b(side * 6.3, 0, 0, 1.6, 2.4, 2.4, Mat.STEEL);
            m.b(side * 7.5, 0, 0, 0.9, 1.4, 1.4, Mat.COPPER);
            m.cyl('x', side * 8.4, 0, 0, 2.2, 0.8, Mat.STEEL_LIGHT, Mat.STEEL, 8);
        }
    }

    static void clutch() {
        Model m = model("clutch").facing();
        m.cyl('z', 0, 0, 0, 6.5, 0.8, Mat.FRICTION, Mat.FRICTION, 16);
        m.cyl('z', 0, 0, 0, 4.2, 1.1, Mat.STEEL, Mat.STEEL, 16);
        m.cyl('z', 0, 0, 0, 2.2, 1.9, Mat.STEEL_DARK, Mat.STEEL_DARK, 8);
        m.cyl('z', 0, 0, 0, 1.0, 2.1, Mat.OIL, Mat.OIL, 8);
        m.b(3.1, 0, 0, 0.9, 1.6, 1.5, Mat.COPPER);
        m.b(-3.1, 0, 0, 0.9, 1.6, 1.5, Mat.COPPER);
        m.b(0, 3.1, 0, 1.6, 0.9, 1.5, Mat.COPPER);
        m.b(0, -3.1, 0, 1.6, 0.9, 1.5, Mat.COPPER);
    }

    static void brakePads() {
        Model m = model("brake_pads");
        for (int side = -1; side <= 1; side += 2) {
            m.b(0, 0, side * 2.3, 8, 5, 0.8, Mat.STEEL);
            m.b(0, 0, side * 1.4, 7, 4.2, 1.0, Mat.FRICTION_DARK);
            m.b(4.4, 1.5, side * 2.3, 0.8, 1.5, 0.8, Mat.STEEL);
            m.b(-4.4, 1.5, side * 2.3, 0.8, 1.5, 0.8, Mat.STEEL);
            m.b(0, 2.8, side * 2.3, 2, 0.6, 0.9, Mat.COPPER);
        }
    }

    static void brakeDiscs() {
        Model m = model("brake_discs").facing();
        m.cyl('z', 0, 0, -0.6, 6.5, 0.5, Mat.STEEL_LIGHT, Mat.STEEL_LIGHT, 16);
        m.cyl('z', 0, 0, 0.6, 6.5, 0.5, Mat.STEEL_LIGHT, Mat.STEEL_LIGHT, 16);
        m.cyl('z', 0, 0, 0, 6.0, 0.7, Mat.VENTS, Mat.OIL, 16);
        m.cyl('z', 0, 0, -1.3, 3.2, 1.6, Mat.STEEL_DARK, Mat.STEEL_DARK, 16);
        m.cyl('z', 0, 0, -2.13, 1.2, 0.1, Mat.OIL, Mat.OIL, 8);
        m.b(1.95, 0, -2.13, 0.6, 0.6, 0.1, Mat.OIL);
        m.b(-1.95, 0, -2.13, 0.6, 0.6, 0.1, Mat.OIL);
        m.b(0, 1.95, -2.13, 0.6, 0.6, 0.1, Mat.OIL);
        m.b(0, -1.95, -2.13, 0.6, 0.6, 0.1, Mat.OIL);
    }

    static void steeringRack() {
        Model m = model("steering_rack");
        m.cyl('x', 0, 0, 0, 0.9, 9, Mat.STEEL, Mat.STEEL, 8);
        m.b(-2, 0, 0, 3, 2.4, 2.4, Mat.STEEL_DARK);
        m.b(-2, 2.2, 0, 1.4, 2.2, 1.4, Mat.STEEL_DARK);
        m.b(-2, 3.7, 0, 0.8, 0.9, 0.8, Mat.COPPER);
        m.b(1.6, -1.0, 0, 1.0, 0.8, 2.6, Mat.STEEL_DARK);
        m.b(3.6, -1.0, 0, 1.0, 0.8, 2.6, Mat.STEEL_DARK);
        for (int side = -1; side <= 1; side += 2) {
            m.cyl('x', side * 5.6, 0, 0, 1.5, 2.4, Mat.BELLOWS, Mat.RUBBER, 8);
            m.cyl('x', side * 7.6, 0, 0, 0.5, 1.8, Mat.STEEL_LIGHT, Mat.STEEL_LIGHT, 8);
            m.b(side * 8.9, 0, 0, 1.2, 1.2, 1.2, Mat.STEEL);
            m.b(side * 8.9, 1.0, 0, 0.6, 0.9, 0.6, Mat.STEEL_LIGHT);
        }
    }

    static void exhaust() {
        Model m = model("exhaust");
        m.cyl('x', 0, 0, 0, 2.6, 7, Mat.STEEL, Mat.STEEL_DARK, 8);
        m.cyl('x', 3.3, 0, 0, 2.8, 0.5, Mat.STEEL_DARK, Mat.STEEL_DARK, 8);
        m.cyl('x', -3.3, 0, 0, 2.8, 0.5, Mat.STEEL_DARK, Mat.STEEL_DARK, 8);
        m.cyl('x', 6.5, 0, 0, 0.8, 6, Mat.STEEL_DARK, Mat.OIL, 8);
        m.cyl('x', -5.0, -0.8, 0, 0.9, 3.5, Mat.STEEL_DARK, Mat.OIL, 8);
        m.cyl('x', -7.2, -0.8, 0, 1.2, 1.6, Mat.STEEL_LIGHT, Mat.OIL, 8);
        m.b(-2, 3.1, 0, 0.5, 1.2, 0.5, Mat.RUBBER_LIGHT);
        m.b(2, 3.1, 0, 0.5, 1.2, 0.5, Mat.RUBBER_LIGHT);
    }

    static void diagnosticSheet() {
        Model m = model("diagnostic_sheet").facing();
        m.b(0, 0, 0, 9, 12, 0.6, Mat.WOOD);
        m.b(0, -0.4, -0.4, 7.6, 10.2, 0.2, Mat.PAPER);
        m.b(0, 5.3, -0.5, 3.6, 1.4, 0.6, Mat.STEEL);
        m.b(0, 6.1, -0.3, 2.0, 0.8, 0.6, Mat.STEEL_DARK);
        m.b(-0.8, 2.6, -0.52, 5, 0.7, 0.06, Mat.GREEN);
        m.b(-1.3, 0.9, -0.52, 4, 0.7, 0.06, Mat.HAZARD);
        m.b(-0.3, -0.8, -0.52, 6, 0.7, 0.06, Mat.PAPER_DARK);
        m.b(-1.8, -2.5, -0.52, 3, 0.7, 0.06, Mat.PAINT);
        m.b(-0.3, -4.2, -0.52, 6, 0.7, 0.06, Mat.PAPER_DARK);
    }

    /**
     * An engine, by how many cylinders, how they are laid out and what it
     * burns. A straight one is a long block with a cover on top; a vee is two
     * short banks leaning apart with the intake in the valley. A diesel wears
     * green, and copper injector lines where the plug caps would be.
     */
    static void engine(String name, int cylinders, boolean vee, boolean diesel) {
        Model m = model(name);
        Mat cover = diesel ? Mat.GREEN : Mat.PAINT;
        Mat caps = diesel ? Mat.COPPER : Mat.STEEL_DARK;
        int perBank = vee ? cylinders / 2 : cylinders;
        double len = 1.2 + perBank * 2.2;
        double half = len / 2;
        m.b(0, 1.0, 0, 5, 2, len - 1, Mat.OIL);
        m.b(0, 4.5, 0, 7, 5, len, Mat.STEEL);
        if (!vee) {
            m.b(0, 8.0, 0, 6.4, 2, len, Mat.STEEL_LIGHT);
            m.b(0, 9.6, 0, 5, 1.2, len - 0.8, cover);
            for (int i = 0; i < perBank; i++) {
                m.b(0, 10.6, -(perBank - 1) * 1.1 + i * 2.2, 1.0, 0.8, 1.0, caps);
            }
            // Exhaust side, intake side.
            m.b(4.1, 7.0, 0, 1.2, 1.4, len - 1.4, Mat.COPPER);
            m.b(4.1, 5.0, half - 1.6, 1.0, 2.8, 1.0, Mat.COPPER_DARK);
            m.b(-4.0, 7.6, 0, 1.4, 1.6, len - 2, Mat.STEEL_DARK);
            m.b(-5.2, 8.2, 0, 1.2, 1.6, 2.2, Mat.STEEL_LIGHT);
        } else {
            for (int side = -1; side <= 1; side += 2) {
                m.b(side * 2.6, 8.0, 0, 3.4, 3.4, len, Mat.STEEL_LIGHT).rot('z', -side * 22.5);
                m.b(side * 3.4, 9.9, 0, 3.0, 1.0, len - 0.8, cover).rot('z', -side * 22.5);
                for (int i = 0; i < perBank; i++) {
                    m.b(side * 3.7, 10.6, -(perBank - 1) * 1.1 + i * 2.2, 0.9, 0.7, 0.9, caps)
                            .rot('z', -side * 22.5);
                }
                m.b(side * 5.3, 6.4, 0, 1.0, 1.2, len - 1.4, Mat.COPPER);
                m.b(side * 5.3, 4.6, half - 1.6, 0.9, 2.6, 0.9, Mat.COPPER_DARK);
            }
            // The intake sits in the valley between the banks.
            m.b(0, 9.2, 0, 2.2, 1.8, len - 2, Mat.STEEL_DARK);
            m.b(0, 10.5, -1.0, 1.8, 0.9, 3.0, Mat.STEEL_LIGHT);
        }
        // Pulleys and the belt round them, on the nose.
        m.cyl('z', 0, 3.0, -half - 0.4, 1.8, 0.8, Mat.RUBBER, Mat.STEEL_DARK, 8);
        m.cyl('z', 2.0, 7.2, -half - 0.4, 1.2, 0.8, Mat.RUBBER, Mat.STEEL_DARK, 8);
        m.cyl('z', -2.0, 6.4, -half - 0.4, 1.0, 0.8, Mat.RUBBER, Mat.STEEL_DARK, 8);
        // Flywheel on the tail.
        m.cyl('z', 0, 4.5, half + 0.5, 3.8, 1.0, Mat.STEEL_DARK, Mat.STEEL_DARK, 16);
        m.cyl('x', -4.3, 3.2, -half + 2.5, 1.1, 1.6, Mat.BLUE, Mat.BLUE_DARK, 8);
    }

    /** Layout names in CarSetup.Layout order: the low two bits of the variant. */
    static final String[] LAYOUTS = {"inline4", "inline6", "v6", "v8"};

    /** All eight engines; variant 0, the petrol four, keeps the plain name. */
    static void engines() {
        int[] cylinders = {4, 6, 6, 8};
        boolean[] vee = {false, false, true, true};
        Map<Integer, String> kinds = new LinkedHashMap<>();
        for (int variant = 0; variant < 8; variant++) {
            int layout = variant & 3;
            boolean diesel = variant >= 4;
            String name = variant == 0 ? "engine"
                    : "engine_" + LAYOUTS[layout] + (diesel ? "_diesel" : "");
            engine(name, cylinders[layout], vee[layout], diesel);
            if (variant != 0) {
                kinds.put(variant, name);
            }
        }
        KINDS.put("engine", kinds);
        COMPONENT.put("engine", "car:part_variant");
    }

    /** base item -> (component value -> the model that value shows). */
    static final Map<String, Map<Integer, String>> KINDS = new LinkedHashMap<>();
    static final Map<String, String> COMPONENT = new LinkedHashMap<>();

    static boolean isKind(String name) {
        for (Map<Integer, String> kinds : KINDS.values()) {
            if (kinds.containsValue(name)) {
                return true;
            }
        }
        return false;
    }

    static void transmission() {
        Model m = model("transmission");
        m.cyl('z', 0, 0, -4.5, 5.0, 2.4, Mat.STEEL, Mat.STEEL_DARK, 16);
        m.cyl('z', 0, 0, -2.6, 4.0, 1.6, Mat.STEEL, Mat.STEEL, 8);
        m.b(0, 0, 0.9, 5.6, 5.6, 5.6, Mat.STEEL);
        m.b(0, 1.2, 0.9, 5.9, 0.7, 4.6, Mat.STEEL_DARK);
        m.b(0, -1.2, 0.9, 5.9, 0.7, 4.6, Mat.STEEL_DARK);
        m.b(0, 2.95, 0.9, 4, 0.4, 4, Mat.STEEL_LIGHT);
        m.b(0, -0.3, 5.4, 3.6, 3.6, 3.6, Mat.STEEL_DARK);
        m.cyl('z', 0, -0.3, 7.8, 1.4, 1.4, Mat.STEEL_LIGHT, Mat.OIL, 8);
        m.b(0, 4.9, 1.5, 0.6, 3.8, 0.6, Mat.STEEL_DARK);
        m.b(0, 7.2, 1.5, 1.4, 1.2, 1.4, Mat.COPPER);
        m.b(0, -2.9, 0.9, 0.8, 0.3, 0.8, Mat.COPPER);
    }

    static void suspension() {
        Model m = model("suspension");
        m.cyl('z', 0, 0.9, 0, 1.3, 1.6, Mat.STEEL_DARK, Mat.OIL, 8);
        m.cyl('y', 0, 4.5, 0, 1.5, 6, Mat.STEEL_DARK, Mat.STEEL_DARK, 8);
        m.cyl('y', 0, 4.2, 0, 3.3, 0.6, Mat.STEEL, Mat.STEEL, 16);
        m.cyl('y', 0, 9.5, 0, 0.6, 5, Mat.STEEL_LIGHT, Mat.STEEL_LIGHT, 8);
        for (int i = 0; i < 6; i++) {
            m.cyl('y', 0, 5.2 + i * 1.25, 0, 2.9, 0.65, Mat.COIL, Mat.COPPER_DARK, 16);
        }
        m.cyl('y', 0, 12.4, 0, 3.3, 0.8, Mat.STEEL, Mat.STEEL, 16);
        m.b(0, 13.3, 0, 0.8, 1.0, 0.8, Mat.STEEL_LIGHT);
    }

    static void wheel() {
        Model m = model("wheel").facing();
        m.cyl('z', 0, 0, 0, 7.0, 4.6, Mat.TREAD, Mat.RUBBER, 16);
        rim(m, 4.6, Mat.STEEL, Mat.STEEL_LIGHT);
    }

    static void rim(Model m, double radius, Mat rim, Mat spokes) {
        m.cyl('z', 0, 0, 0, radius, 4.8, rim, spokes, 16);
        m.cyl('z', 0, 0, 0, radius - 1.0, 4.9, Mat.OIL, Mat.OIL, 16);
        m.b(0, 0, 0, radius * 1.5, 1.3, 5.0, spokes);
        m.b(0, 0, 0, 1.3, radius * 1.5, 5.0, spokes);
        m.cyl('z', 0, 0, 0, 1.6, 5.3, rim, Mat.STEEL_DARK, 8);
    }

    /** Knobbly: eight lugs standing out of the tread, on a dark steel rim. */
    static void wheelDirt() {
        Model m = model("wheel_dirt").facing();
        m.cyl('z', 0, 0, 0, 6.6, 5.0, Mat.TREAD, Mat.RUBBER, 16);
        for (int i = 0; i < 4; i++) {
            double a = Math.toRadians(i * 90);
            m.b(Math.cos(a) * 6.7, Math.sin(a) * 6.7, 0, 1.8, 1.8, 5.4, Mat.RUBBER_LIGHT);
            double d = Math.toRadians(i * 90 + 45);
            m.b(Math.cos(d) * 6.7, Math.sin(d) * 6.7, 0, 1.8, 1.8, 5.4, Mat.RUBBER_LIGHT)
                    .rot('z', 45);
        }
        rim(m, 4.0, Mat.STEEL_DARK, Mat.STEEL);
    }

    /** Siped: pale cuts across the tread, the way packed snow shows in them. */
    static void wheelSnow() {
        Model m = model("wheel_snow").facing();
        m.cyl('z', 0, 0, 0, 7.0, 4.6, Mat.SIPES, Mat.RUBBER, 16);
        rim(m, 4.4, Mat.STEEL, Mat.BLUE);
    }

    /** Slick, wide and low: mostly rim, with a painted centre. */
    static void wheelSport() {
        Model m = model("wheel_sport").facing();
        m.cyl('z', 0, 0, 0, 7.0, 5.6, Mat.RUBBER, Mat.RUBBER_LIGHT, 16);
        m.cyl('z', 0, 0, 0, 5.8, 5.8, Mat.STEEL_LIGHT, Mat.STEEL_LIGHT, 16);
        m.cyl('z', 0, 0, 0, 5.0, 5.9, Mat.OIL, Mat.OIL, 16);
        m.b(0, 0, 0, 10.4, 1.0, 6.0, Mat.STEEL_LIGHT);
        m.b(0, 0, 0, 1.0, 10.4, 6.0, Mat.STEEL_LIGHT);
        m.b(0, 0, 0, 10.4, 1.0, 6.0, Mat.STEEL_LIGHT).rot('z', 45);
        m.b(0, 0, 0, 1.0, 10.4, 6.0, Mat.STEEL_LIGHT).rot('z', 45);
        m.cyl('z', 0, 0, 0, 1.7, 6.3, Mat.PAINT, Mat.PAINT_DARK, 8);
    }

    // ---- the vehicles and what the lift bolts onto them ----

    /** One road wheel on its side of the car, with a bright cap facing out. */
    static void roadWheel(Model m, double x, double y, double z, double radius) {
        m.cyl('x', x, y, z, radius, 1.2, Mat.RUBBER, Mat.RUBBER_LIGHT, 8);
        m.b(x + Math.signum(x) * 0.62, y, z, 0.1, radius, radius, Mat.STEEL_LIGHT);
    }

    /** The saloon's shell: used bare as the body part, and dressed as the car. */
    static void shell(Model m, Mat paint) {
        m.b(0, 2.4, 0, 7, 3.2, 15, paint);
        m.b(0, 5.3, 1.0, 6.0, 2.6, 7.0, Mat.GLASS);
        m.b(0, 6.9, 1.0, 6.4, 0.6, 7.4, paint);
        for (int side = -1; side <= 1; side += 2) {
            m.b(side * 2.9, 5.3, -2.4, 0.5, 2.6, 0.5, paint);
            m.b(side * 2.9, 5.3, 4.4, 0.5, 2.6, 0.5, paint);
            m.b(side * 3.0, 5.3, 1.0, 0.3, 2.6, 0.6, paint);
            // The arches, cut out of the sill.
            m.b(side * 3.46, 1.55, -4.6, 0.2, 1.5, 2.8, Mat.OIL);
            m.b(side * 3.46, 1.55, 4.6, 0.2, 1.5, 2.8, Mat.OIL);
        }
    }

    static void carBody() {
        Model m = model("car_body");
        shell(m, Mat.PAINT);
        // Where the lamps will go.
        m.b(2.3, 2.8, -7.46, 1.4, 0.9, 0.15, Mat.OIL);
        m.b(-2.3, 2.8, -7.46, 1.4, 0.9, 0.15, Mat.OIL);
    }

    static void car() {
        Model m = model("car");
        m.tinted = true;
        shell(m, Mat.TINTED);
        for (int side = -1; side <= 1; side += 2) {
            roadWheel(m, side * 3.3, 1.0, -4.6, 1.5);
            roadWheel(m, side * 3.3, 1.0, 4.6, 1.5);
            m.b(side * 2.3, 2.8, -7.5, 1.4, 0.9, 0.15, Mat.LAMP);
            m.b(side * 2.5, 2.9, 7.5, 1.2, 0.8, 0.15, Mat.TAIL);
        }
        m.b(0, 2.6, -7.5, 2.6, 0.9, 0.12, Mat.OIL);
        m.b(0, 1.3, -7.6, 7.2, 0.7, 0.5, Mat.STEEL_DARK);
        m.b(0, 1.3, 7.6, 7.2, 0.7, 0.5, Mat.STEEL_DARK);
    }

    /** The pickup's bed, about a given floor height and the middle of its length. */
    static void bed(Model m, double y, double z) {
        m.b(0, y, z, 7, 0.5, 7.6, Mat.WOOD_DARK);
        m.b(3.3, y + 1.2, z, 0.4, 2.0, 7.6, Mat.PLANKS);
        m.b(-3.3, y + 1.2, z, 0.4, 2.0, 7.6, Mat.PLANKS);
        m.b(0, y + 1.2, z - 3.6, 6.2, 2.0, 0.4, Mat.PLANKS);
        m.b(0, y + 1.2, z + 3.6, 6.2, 2.0, 0.4, Mat.PLANKS);
        m.b(3.3, y + 2.3, z, 0.6, 0.3, 7.8, Mat.STEEL);
        m.b(-3.3, y + 2.3, z, 0.6, 0.3, 7.8, Mat.STEEL);
        m.b(0, y + 2.3, z - 3.6, 6.0, 0.3, 0.6, Mat.STEEL);
        m.b(0, y + 2.3, z + 3.6, 6.0, 0.3, 0.6, Mat.STEEL);
    }

    static void pickupBed() {
        bed(model("pickup_bed"), 0, 0);
    }

    static void pickup() {
        Model m = model("pickup");
        m.b(0, 1.3, 0, 6.6, 0.8, 15, Mat.STEEL_DARK);
        m.b(0, 3.0, -5.3, 7, 2.6, 4.4, Mat.PAINT);
        m.b(0, 3.0, -1.7, 7, 2.6, 2.8, Mat.PAINT);
        m.b(0, 5.3, -1.7, 6.6, 2.2, 2.6, Mat.GLASS);
        m.b(0, 6.6, -1.7, 7, 0.5, 3.0, Mat.PAINT);
        for (int side = -1; side <= 1; side += 2) {
            m.b(side * 3.25, 5.3, -3.0, 0.5, 2.2, 0.5, Mat.PAINT);
            m.b(side * 3.25, 5.3, -0.4, 0.5, 2.2, 0.5, Mat.PAINT);
            roadWheel(m, side * 3.3, 1.0, -5.0, 1.5);
            roadWheel(m, side * 3.3, 1.0, 4.6, 1.5);
            m.b(side * 2.3, 3.3, -7.52, 1.4, 0.9, 0.15, Mat.LAMP);
            m.b(side * 2.6, 2.3, 7.52, 1.0, 0.7, 0.15, Mat.TAIL);
        }
        m.b(0, 3.0, -7.52, 2.6, 1.0, 0.12, Mat.OIL);
        m.b(0, 1.4, -7.6, 7.2, 0.7, 0.5, Mat.STEEL);
        bed(m, 1.95, 3.7);
    }

    /** The tanker's barrel, lying the way it rides. */
    static void barrel(Model m, double y, double z, double radius, double length) {
        m.cyl('z', 0, y, z, radius, length, Mat.TANK, Mat.TANK_LIGHT, 16);
        m.cyl('z', 0, y, z, radius + 0.1, length * 0.14, Mat.PAINT, Mat.PAINT, 16);
        m.cyl('z', 0, y, z - length * 0.45, radius + 0.1, 0.4, Mat.STEEL_DARK, Mat.STEEL_DARK, 16);
        m.cyl('z', 0, y, z + length * 0.45, radius + 0.1, 0.4, Mat.STEEL_DARK, Mat.STEEL_DARK, 16);
        m.b(0, y + radius + 0.2, z - length * 0.25, 1.6, 0.5, 1.6, Mat.STEEL_DARK);
        m.b(0, y + radius + 0.6, z - length * 0.25, 1.0, 0.4, 1.0, Mat.COPPER);
    }

    static void tankerBarrel() {
        Model m = model("tanker_barrel");
        barrel(m, 0, 0, 4.2, 11);
        m.b(0, -4.2, -3.5, 6, 1.2, 1.2, Mat.STEEL_DARK);
        m.b(0, -4.2, 3.5, 6, 1.2, 1.2, Mat.STEEL_DARK);
    }

    static void fuelTanker() {
        Model m = model("fuel_tanker");
        m.b(0, 1.3, 0, 6.2, 0.8, 16, Mat.STEEL_DARK);
        // A cab that sits over its front axle.
        m.b(0, 3.4, -6.2, 7, 3.4, 3.6, Mat.PAINT);
        m.b(0, 5.9, -6.2, 6.6, 1.8, 3.2, Mat.GLASS);
        m.b(0, 7.0, -6.2, 7, 0.5, 3.6, Mat.PAINT);
        for (int side = -1; side <= 1; side += 2) {
            m.b(side * 3.25, 5.9, -7.75, 0.5, 1.8, 0.5, Mat.PAINT);
            m.b(side * 3.25, 5.9, -4.65, 0.5, 1.8, 0.5, Mat.PAINT);
            roadWheel(m, side * 3.3, 1.0, -6.0, 1.5);
            roadWheel(m, side * 3.3, 1.0, 3.2, 1.5);
            roadWheel(m, side * 3.3, 1.0, 6.4, 1.5);
            m.b(side * 2.4, 2.6, -8.02, 1.2, 0.8, 0.15, Mat.LAMP);
            m.b(side * 2.6, 1.9, 8.02, 1.0, 0.6, 0.15, Mat.TAIL);
        }
        m.b(0, 1.4, -8.1, 7.2, 0.7, 0.5, Mat.STEEL);
        barrel(m, 4.7, 2.0, 3.0, 11.6);
    }

    static void jerryCan() {
        jerryCan("jerry_can", Mat.PAINT, Mat.PAINT_DARK);
        // Diesel goes in the yellow one, so the two are never confused.
        jerryCan("jerry_can_diesel", Mat.HAZARD, Mat.HAZARD_DARK);
        KINDS.put("jerry_can", Map.of(1, "jerry_can_diesel"));
        COMPONENT.put("jerry_can", "car:fuel_kind");
    }

    static void jerryCan(String name, Mat paint, Mat dark) {
        Model m = model(name);
        m.b(0, 5, 0, 8, 10, 3.6, paint);
        for (int side = -1; side <= 1; side += 2) {
            m.b(0, 4.8, side * 1.83, 6.0, 7.0, 0.1, dark);
            m.b(0, 4.8, side * 1.9, 7.4, 0.8, 0.1, paint).rot('z', 45);
            m.b(0, 4.8, side * 1.9, 7.4, 0.8, 0.1, paint).rot('z', -45);
        }
        m.b(-0.8, 10.4, 0, 5.4, 0.8, 3.0, paint);
        m.b(-2.7, 11.3, 0, 0.7, 1.2, 2.6, dark);
        m.b(1.1, 11.3, 0, 0.7, 1.2, 2.6, dark);
        m.b(-0.8, 12.1, 0, 4.5, 0.6, 2.6, dark);
        m.cyl('y', 3.1, 10.9, 0, 0.9, 1.8, Mat.STEEL_DARK, Mat.STEEL, 8);
    }

    // ---- geometry ----

    static double[] turn(double[] p, char axis, double degrees) {
        double a = Math.toRadians(degrees);
        double c = Math.cos(a);
        double s = Math.sin(a);
        return switch (axis) {
            case 'x' -> new double[] {p[0], p[1] * c - p[2] * s, p[1] * s + p[2] * c};
            case 'y' -> new double[] {p[0] * c + p[2] * s, p[1], -p[0] * s + p[2] * c};
            default -> new double[] {p[0] * c - p[1] * s, p[0] * s + p[1] * c, p[2]};
        };
    }

    /** A point of a box, after the box's own turn. */
    static double[] placed(Box box, double[] p) {
        if (box.axis == 0) {
            return p;
        }
        double[] rel = {p[0] - box.origin[0], p[1] - box.origin[1], p[2] - box.origin[2]};
        double[] out = turn(rel, box.axis, box.angle);
        return new double[] {out[0] + box.origin[0], out[1] + box.origin[1], out[2] + box.origin[2]};
    }

    static List<double[]> corners(Model model) {
        List<double[]> out = new ArrayList<>();
        for (Box box : model.boxes) {
            for (int i = 0; i < 8; i++) {
                out.add(placed(box, new double[] {
                        (i & 1) == 0 ? box.from[0] : box.to[0],
                        (i & 2) == 0 ? box.from[1] : box.to[1],
                        (i & 4) == 0 ? box.from[2] : box.to[2]}));
            }
        }
        return out;
    }

    /** Moves a model so that it is centred on the middle of its block. */
    static double centre(Model model) {
        double[] min = {1e9, 1e9, 1e9};
        double[] max = {-1e9, -1e9, -1e9};
        for (double[] p : corners(model)) {
            for (int i = 0; i < 3; i++) {
                min[i] = Math.min(min[i], p[i]);
                max[i] = Math.max(max[i], p[i]);
            }
        }
        double largest = 0;
        for (int i = 0; i < 3; i++) {
            double shift = 8.0 - (min[i] + max[i]) / 2.0;
            largest = Math.max(largest, max[i] - min[i]);
            for (Box box : model.boxes) {
                box.from[i] += shift;
                box.to[i] += shift;
                box.origin[i] += shift;
            }
        }
        return largest;
    }

    /** Model space to the screen's: x right, y up, z out towards you. */
    static double[] shown(double[] p, double[] rotation, double scale) {
        double[] v = {(p[0] - 8) * scale, (p[1] - 8) * scale, (p[2] - 8) * scale};
        v = turn(v, 'z', rotation[2]);
        v = turn(v, 'y', rotation[1]);
        v = turn(v, 'x', rotation[0]);
        return v;
    }

    /** The inventory's view: how big the model can be drawn, and where. */
    static double[] fit(Model model) {
        double minX = 1e9, maxX = -1e9, minY = 1e9, maxY = -1e9;
        for (double[] p : corners(model)) {
            double[] v = shown(p, model.gui, 1.0);
            minX = Math.min(minX, v[0]);
            maxX = Math.max(maxX, v[0]);
            minY = Math.min(minY, v[1]);
            maxY = Math.max(maxY, v[1]);
        }
        double scale = Math.min(1.6, 15.0 / Math.max(maxX - minX, maxY - minY));
        return new double[] {scale, -(minX + maxX) / 2 * scale, -(minY + maxY) / 2 * scale};
    }

    // ---- faces and their piece of the sheet ----

    /** A face as a corner and two edges: u runs along the first, v along the second. */
    static double[][] quad(Box b, int face) {
        double[] f = b.from;
        double[] t = b.to;
        double dx = t[0] - f[0], dy = t[1] - f[1], dz = t[2] - f[2];
        return switch (face) {
            case NORTH -> new double[][] {{t[0], t[1], f[2]}, {-dx, 0, 0}, {0, -dy, 0}};
            case SOUTH -> new double[][] {{f[0], t[1], t[2]}, {dx, 0, 0}, {0, -dy, 0}};
            case WEST -> new double[][] {{f[0], t[1], f[2]}, {0, 0, dz}, {0, -dy, 0}};
            case EAST -> new double[][] {{t[0], t[1], t[2]}, {0, 0, -dz}, {0, -dy, 0}};
            case UP -> new double[][] {{f[0], t[1], f[2]}, {dx, 0, 0}, {0, 0, dz}};
            default -> new double[][] {{f[0], f[1], t[2]}, {dx, 0, 0}, {0, 0, -dz}};
        };
    }

    static double length(double[] v) {
        return Math.sqrt(v[0] * v[0] + v[1] * v[1] + v[2] * v[2]);
    }

    static int hash(int a, int b, int c) {
        int h = a * 0x9E3779B1 + b * 0x85EBCA77 + c * 0xC2B2AE3D;
        h ^= h >>> 15;
        h *= 0x2C1B3C6D;
        h ^= h >>> 12;
        return h & 0x7FFFFFFF;
    }

    /** The texels a face takes from the sheet: x, y, width, height. */
    static int[] region(Model model, int boxIndex, Box box, int face) {
        Mat mat = box.faces[face];
        double[][] q = quad(box, face);
        int w = (int) Math.max(1, Math.min(SWATCH, Math.round(length(q[1]))));
        int h = (int) Math.max(1, Math.min(SWATCH, Math.round(length(q[2]))));
        if (box.turned[face]) {
            int swap = w;
            w = h;
            h = swap;
        }
        int ox = 0;
        int oy = 0;
        if (mat.fill == Fill.PLAIN) {
            ox = hash(model.seed, boxIndex, face) % (SWATCH - w + 1);
            oy = hash(model.seed, boxIndex, face + 7) % (SWATCH - h + 1);
        }
        int index = mat.ordinal();
        return new int[] {(index % COLUMNS) * SWATCH + ox, (index / COLUMNS) * SWATCH + oy, w, h};
    }

    // ---- the sheet ----

    static int shade(int rgb, double by) {
        int r = (int) Math.max(0, Math.min(255, ((rgb >> 16) & 0xFF) * by));
        int g = (int) Math.max(0, Math.min(255, ((rgb >> 8) & 0xFF) * by));
        int b = (int) Math.max(0, Math.min(255, (rgb & 0xFF) * by));
        return 0xFF000000 | (r << 16) | (g << 8) | b;
    }

    static BufferedImage sheet() {
        BufferedImage img = new BufferedImage(SHEET, SHEET, BufferedImage.TYPE_INT_ARGB);
        for (Mat mat : Mat.values()) {
            int x0 = (mat.ordinal() % COLUMNS) * SWATCH;
            int y0 = (mat.ordinal() / COLUMNS) * SWATCH;
            for (int y = 0; y < SWATCH; y++) {
                for (int x = 0; x < SWATCH; x++) {
                    int along = mat.fill == Fill.ACROSS ? x : y;
                    int base = mat.fill != Fill.PLAIN && (along & 1) == 1 ? mat.other : mat.rgb;
                    // Roughened, but only just: these are small faces, and a
                    // loud grain on a face four texels wide is just noise.
                    int grain = hash(mat.ordinal(), x, y) % 100;
                    double by = grain < 12 ? 0.90 : (grain < 30 ? 0.96 : (grain > 90 ? 1.06 : 1.0));
                    img.setRGB(x0 + x, y0 + y, shade(base, by));
                }
            }
        }
        return img;
    }

    // ---- writing ----

    static String num(double value) {
        String s = String.format(Locale.ROOT, "%.3f", value);
        s = s.replaceAll("0+$", "").replaceAll("\\.$", "");
        return s.equals("-0") ? "0" : s;
    }

    static String triple(double a, double b, double c) {
        return "[" + num(a) + ", " + num(b) + ", " + num(c) + "]";
    }

    static void write(String path, String body) throws Exception {
        File out = new File(path);
        out.getParentFile().mkdirs();
        try (PrintWriter w = new PrintWriter(out, "UTF-8")) {
            w.println(body);
        }
    }

    static String json(Model model, double largest, double[] fit) {
        StringBuilder out = new StringBuilder();
        out.append("{\n");
        out.append("  \"textures\": {\n");
        out.append("    \"particle\": \"car:item/materials\",\n");
        out.append("    \"sheet\": \"car:item/materials\"\n");
        out.append("  },\n");
        out.append("  \"elements\": [\n");
        for (int i = 0; i < model.boxes.size(); i++) {
            Box box = model.boxes.get(i);
            out.append("    {\n");
            out.append("      \"from\": ").append(triple(box.from[0], box.from[1], box.from[2])).append(",\n");
            out.append("      \"to\": ").append(triple(box.to[0], box.to[1], box.to[2])).append(",\n");
            if (box.axis != 0) {
                out.append("      \"rotation\": {\"origin\": ")
                        .append(triple(box.origin[0], box.origin[1], box.origin[2]))
                        .append(", \"axis\": \"").append(box.axis)
                        .append("\", \"angle\": ").append(num(box.angle)).append("},\n");
            }
            out.append("      \"faces\": {\n");
            boolean first = true;
            for (int face = 0; face < 6; face++) {
                Mat mat = box.faces[face];
                if (mat == null) {
                    continue;
                }
                if (!first) {
                    out.append(",\n");
                }
                first = false;
                int[] r = region(model, i, box, face);
                double unit = 16.0 / SHEET;
                // Kept a whisker inside its texels, so that a face never picks
                // up the edge of the swatch next door.
                double inset = 0.01;
                out.append("        \"").append(FACE_NAMES[face]).append("\": {\"uv\": [")
                        .append(num(r[0] * unit + inset)).append(", ")
                        .append(num(r[1] * unit + inset)).append(", ")
                        .append(num((r[0] + r[2]) * unit - inset)).append(", ")
                        .append(num((r[1] + r[3]) * unit - inset)).append("], \"texture\": \"#sheet\"");
                if (box.turned[face]) {
                    out.append(", \"rotation\": 90");
                }
                if (mat == Mat.TINTED) {
                    out.append(", \"tintindex\": 0");
                }
                out.append("}");
            }
            out.append("\n      }\n");
            out.append("    }").append(i == model.boxes.size() - 1 ? "" : ",").append("\n");
        }
        out.append("  ],\n");

        // In the hand and on the floor everything is about as big as a block
        // is there, whatever size it was drawn at.
        double held = Math.max(0.35, Math.min(0.75, 6.5 / largest));
        double framed = Math.min(1.0, held * 2.0);
        double third = held * 0.9;
        // Smaller again in your own hand: these are long things, and at a
        // block's size the far end of an engine is in your face.
        double first = held * 0.6;
        out.append("  \"display\": {\n");
        out.append("    \"gui\": {\"rotation\": ").append(triple(model.gui[0], model.gui[1], model.gui[2]))
                .append(", \"translation\": ").append(triple(fit[1], fit[2], 0))
                .append(", \"scale\": ").append(triple(fit[0], fit[0], fit[0])).append("},\n");
        out.append("    \"ground\": {\"translation\": [0, 3, 0], \"scale\": ")
                .append(triple(held, held, held)).append("},\n");
        out.append("    \"fixed\": {\"scale\": ").append(triple(framed, framed, framed)).append("},\n");
        out.append("    \"head\": {\"scale\": ").append(triple(framed, framed, framed)).append("},\n");
        out.append("    \"thirdperson_righthand\": {\"rotation\": [75, 45, 0], \"translation\": [0, 2.5, 0], \"scale\": ")
                .append(triple(third, third, third)).append("},\n");
        out.append("    \"thirdperson_lefthand\": {\"rotation\": [75, 45, 0], \"translation\": [0, 2.5, 0], \"scale\": ")
                .append(triple(third, third, third)).append("},\n");
        out.append("    \"firstperson_righthand\": {\"rotation\": [0, 45, 0], \"translation\": [0, 3.5, 0], \"scale\": ")
                .append(triple(first, first, first)).append("},\n");
        out.append("    \"firstperson_lefthand\": {\"rotation\": [0, 225, 0], \"translation\": [0, 3.5, 0], \"scale\": ")
                .append(triple(first, first, first)).append("}\n");
        out.append("  }\n");
        out.append("}");
        return out.toString();
    }

    /** Vanilla dye names and their diffuse colours, in DyeColor order. */
    static final Object[][] DYES = {
            {"white", 0xF9FFFE}, {"orange", 0xF9801D}, {"magenta", 0xC74EBD},
            {"light_blue", 0x3AB3DA}, {"yellow", 0xFED83D}, {"lime", 0x80C71F},
            {"pink", 0xF38BAA}, {"gray", 0x474F52}, {"light_gray", 0x9D9D97},
            {"cyan", 0x169C9C}, {"purple", 0x8932B8}, {"blue", 0x3C44AA},
            {"brown", 0x835432}, {"green", 0x5E7C16}, {"red", 0xB02E26},
            {"black", 0x1D1D21},
    };

    /** What an unpainted car comes out as: the index into DYES. */
    static final int DEFAULT_DYE = 14;

    static String definition(Model model) {
        Map<Integer, String> kinds = KINDS.get(model.name);
        if (kinds != null) {
            // One item, several models: the stack's component says which.
            StringBuilder cases = new StringBuilder();
            int left = kinds.size();
            for (Map.Entry<Integer, String> kind : kinds.entrySet()) {
                cases.append("      {\n        \"when\": ").append(kind.getKey())
                        .append(",\n        \"model\": { \"type\": \"minecraft:model\", ")
                        .append("\"model\": \"car:item/").append(kind.getValue()).append("\" }\n      }")
                        .append(--left == 0 ? "" : ",").append("\n");
            }
            return "{\n  \"model\": {\n    \"type\": \"minecraft:select\",\n"
                    + "    \"property\": \"minecraft:component\",\n"
                    + "    \"component\": \"" + COMPONENT.get(model.name) + "\",\n"
                    + "    \"cases\": [\n" + cases + "    ],\n"
                    + "    \"fallback\": { \"type\": \"minecraft:model\", \"model\": \"car:item/"
                    + model.name + "\" }\n  }\n}";
        }
        if (!model.tinted) {
            return "{\n  \"model\": {\n    \"type\": \"minecraft:model\",\n"
                    + "    \"model\": \"car:item/" + model.name + "\"\n  }\n}";
        }
        // One model, painted by the stack: car:color holds a DyeColor index,
        // and each case hands the model that dye as its tint.
        StringBuilder cases = new StringBuilder();
        for (int i = 0; i < DYES.length; i++) {
            cases.append("      {\n        \"when\": ").append(i).append(",\n        \"model\": ")
                    .append(painted(model, (int) DYES[i][1])).append("\n      }")
                    .append(i == DYES.length - 1 ? "" : ",").append("\n");
        }
        return "{\n  \"model\": {\n    \"type\": \"minecraft:select\",\n"
                + "    \"property\": \"minecraft:component\",\n"
                + "    \"component\": \"car:color\",\n"
                + "    \"cases\": [\n" + cases + "    ],\n"
                + "    \"fallback\": " + painted(model, (int) DYES[DEFAULT_DYE][1]) + "\n  }\n}";
    }

    static String painted(Model model, int rgb) {
        return "{ \"type\": \"minecraft:model\", \"model\": \"car:item/" + model.name
                + "\", \"tints\": [{ \"type\": \"minecraft:constant\", \"value\": " + rgb + " }] }";
    }

    // ---- the preview ----

    /** How bright each face is drawn here; near enough to the inventory's. */
    static final double[] LIGHT = {0.5, 1.0, 0.62, 0.62, 0.8, 0.8};

    static void draw(BufferedImage out, int left, int top, int size, Model model, double[] fit,
                     BufferedImage sheet, int tint) {
        double perUnit = size / 16.0;
        double[] depth = new double[size * size];
        java.util.Arrays.fill(depth, -1e9);
        for (int i = 0; i < model.boxes.size(); i++) {
            Box box = model.boxes.get(i);
            for (int face = 0; face < 6; face++) {
                Mat mat = box.faces[face];
                if (mat == null) {
                    continue;
                }
                double[][] q = quad(box, face);
                int[] r = region(model, i, box, face);
                double[][] screen = new double[4][];
                double[][] st = {{0, 0}, {1, 0}, {1, 1}, {0, 1}};
                for (int k = 0; k < 4; k++) {
                    double[] p = {
                            q[0][0] + st[k][0] * q[1][0] + st[k][1] * q[2][0],
                            q[0][1] + st[k][0] * q[1][1] + st[k][1] * q[2][1],
                            q[0][2] + st[k][0] * q[1][2] + st[k][1] * q[2][2]};
                    double[] v = shown(placed(box, p), model.gui, fit[0]);
                    screen[k] = new double[] {
                            size / 2.0 + (v[0] + fit[1]) * perUnit,
                            size / 2.0 - (v[1] + fit[2]) * perUnit,
                            v[2]};
                }
                for (int[] tri : new int[][] {{0, 1, 2}, {0, 2, 3}}) {
                    double[] a = screen[tri[0]], b = screen[tri[1]], c = screen[tri[2]];
                    double area = (b[0] - a[0]) * (c[1] - a[1]) - (c[0] - a[0]) * (b[1] - a[1]);
                    if (Math.abs(area) < 1e-9) {
                        continue;
                    }
                    int x0 = (int) Math.max(0, Math.floor(Math.min(a[0], Math.min(b[0], c[0]))));
                    int x1 = (int) Math.min(size - 1, Math.ceil(Math.max(a[0], Math.max(b[0], c[0]))));
                    int y0 = (int) Math.max(0, Math.floor(Math.min(a[1], Math.min(b[1], c[1]))));
                    int y1 = (int) Math.min(size - 1, Math.ceil(Math.max(a[1], Math.max(b[1], c[1]))));
                    for (int y = y0; y <= y1; y++) {
                        for (int x = x0; x <= x1; x++) {
                            double px = x + 0.5, py = y + 0.5;
                            double wa = ((b[0] - px) * (c[1] - py) - (c[0] - px) * (b[1] - py)) / area;
                            double wb = ((c[0] - px) * (a[1] - py) - (a[0] - px) * (c[1] - py)) / area;
                            double wc = 1 - wa - wb;
                            if (wa < 0 || wb < 0 || wc < 0) {
                                continue;
                            }
                            double z = wa * a[2] + wb * b[2] + wc * c[2];
                            if (z <= depth[y * size + x]) {
                                continue;
                            }
                            double s = wa * st[tri[0]][0] + wb * st[tri[1]][0] + wc * st[tri[2]][0];
                            double t = wa * st[tri[0]][1] + wb * st[tri[1]][1] + wc * st[tri[2]][1];
                            if (box.turned[face]) {
                                double keep = s;
                                s = t;
                                t = 1 - keep;
                            }
                            int tx = r[0] + (int) Math.min(r[2] - 1, Math.max(0, s * r[2]));
                            int ty = r[1] + (int) Math.min(r[3] - 1, Math.max(0, t * r[3]));
                            int rgb = sheet.getRGB(tx, ty);
                            if (mat == Mat.TINTED) {
                                int tr = ((rgb >> 16) & 0xFF) * ((tint >> 16) & 0xFF) / 255;
                                int tg = ((rgb >> 8) & 0xFF) * ((tint >> 8) & 0xFF) / 255;
                                int tb = (rgb & 0xFF) * (tint & 0xFF) / 255;
                                rgb = (tr << 16) | (tg << 8) | tb;
                            }
                            depth[y * size + x] = z;
                            out.setRGB(left + x, top + y, shade(rgb, LIGHT[face]));
                        }
                    }
                }
            }
        }
    }

    static void preview(String path, List<double[]> fits, BufferedImage sheet) throws Exception {
        int big = 160;
        int small = 32;
        int grow = 2;
        int columns = 6;
        int cellW = big + small * grow + 12;
        int cellH = big + 8;
        int rows = (MODELS.size() + columns - 1) / columns;
        BufferedImage out = new BufferedImage(columns * cellW, rows * cellH, BufferedImage.TYPE_INT_ARGB);
        for (int y = 0; y < out.getHeight(); y++) {
            for (int x = 0; x < out.getWidth(); x++) {
                out.setRGB(x, y, 0xFF8B8B8B);
            }
        }
        for (int i = 0; i < MODELS.size(); i++) {
            Model model = MODELS.get(i);
            int left = (i % columns) * cellW + 4;
            int top = (i / columns) * cellH + 4;
            int tint = (int) DYES[i % 2 == 0 ? 11 : DEFAULT_DYE][1];
            draw(out, left, top, big, model, fits.get(i), sheet, tint);
            // And at the size an inventory slot really is, blown back up.
            BufferedImage slot = new BufferedImage(small, small, BufferedImage.TYPE_INT_ARGB);
            draw(slot, 0, 0, small, model, fits.get(i), sheet, tint);
            for (int y = 0; y < small * grow; y++) {
                for (int x = 0; x < small * grow; x++) {
                    int argb = slot.getRGB(x / grow, y / grow);
                    if ((argb >>> 24) != 0) {
                        out.setRGB(left + big + 4 + x, top + y, argb);
                    }
                }
            }
        }
        ImageIO.write(out, "PNG", new File(path));
        System.out.println("preview " + path);
    }

    /**
     * Items whose definitions other tools have taken over: the three vehicles
     * pick their model per vehicle in tools/GenVehicles.java, the engine has the
     * electric motor as variant 8 (tools/GenEvItems.java) and the jerry can a
     * biofuel case of its own. Their models are still written here.
     */
    static final java.util.Set<String> DEFINED_ELSEWHERE =
            java.util.Set.of("car", "pickup", "fuel_tanker", "engine", "jerry_can");

    public static void main(String[] args) throws Exception {
        airFilter();
        airFilterOpen();
        KINDS.put("air_filter", Map.of(1, "air_filter_open"));
        COMPONENT.put("air_filter", "car:part_variant");
        turbo();
        sparkPlugs();
        oilFilter();
        radiator();
        battery();
        alternator();
        driveshaft();
        clutch();
        brakePads();
        brakeDiscs();
        steeringRack();
        exhaust();
        diagnosticSheet();
        engines();
        transmission();
        suspension();
        wheel();
        wheelDirt();
        wheelSnow();
        wheelSport();
        KINDS.put("wheel", new LinkedHashMap<>(Map.of(1, "wheel_dirt")));
        KINDS.get("wheel").put(2, "wheel_snow");
        KINDS.get("wheel").put(3, "wheel_sport");
        COMPONENT.put("wheel", "car:part_variant");
        carBody();
        car();
        pickup();
        pickupBed();
        fuelTanker();
        tankerBarrel();
        jerryCan();

        BufferedImage sheet = sheet();
        File sheetFile = new File(ASSETS + "textures/item/materials.png");
        ImageIO.write(sheet, "PNG", sheetFile);

        List<double[]> fits = new ArrayList<>();
        int boxes = 0;
        for (Model model : MODELS) {
            double largest = centre(model);
            double[] fit = fit(model);
            fits.add(fit);
            boxes += model.boxes.size();
            write(ASSETS + "models/item/" + model.name + ".json", json(model, largest, fit));
            if (!isKind(model.name) && !DEFINED_ELSEWHERE.contains(model.name)) {
                write(ASSETS + "items/" + model.name + ".json", definition(model));
            }
        }
        System.out.println("wrote " + MODELS.size() + " models, " + boxes + " boxes");

        if (args.length > 0) {
            preview(args[0], fits, sheet);
        }
    }
}
