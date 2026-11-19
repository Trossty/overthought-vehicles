import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Shapes the production line: the refinery plant, the hydrotreater, the
 * pumpjack and the three generators, plus the oil deposit and seep.
 *
 * <p>A machine bigger than a block is drawn whole, in its own frame — x to the
 * east, y up, z to the south with the front to the north, a block being 16 —
 * and then cut into one model per cell: every box is clipped to the cell, and
 * a face that lies on a cut is dropped so neighbouring cells meet without a
 * seam. The items get the whole machine shrunk to fit a hand. Blockstates are
 * written too, every facing, every part, lit or not.
 *
 * <p>Run from the project root: {@code java tools/GenPlantModels.java}.
 */
public final class GenPlantModels {

    static final String ROOT = "src/main/resources/assets/overthought_vehicles/";
    static final String[] ALL = {"north", "south", "west", "east", "up", "down"};

    record Box(float[] from, float[] to, Map<String, String> faces) {
    }

    static final class Model {
        final Map<String, String> textures = new LinkedHashMap<>();
        final List<Box> boxes = new ArrayList<>();
        boolean explicitUv;
        String display;

        Model tex(String key, String value) {
            this.textures.put(key, value);
            return this;
        }

        Model box(float x1, float y1, float z1, float x2, float y2, float z2, String texture) {
            return this.box(x1, y1, z1, x2, y2, z2, "*", texture);
        }

        /** Faces as pairs "north", "#front", ..., "*" the default and "-" for none. */
        Model box(float x1, float y1, float z1, float x2, float y2, float z2, String... pairs) {
            Map<String, String> faces = new LinkedHashMap<>();
            String fallback = null;
            for (int i = 0; i < pairs.length; i += 2) {
                if (pairs[i].equals("*")) {
                    fallback = pairs[i + 1];
                }
            }
            for (String side : ALL) {
                faces.put(side, fallback);
            }
            for (int i = 0; i < pairs.length; i += 2) {
                if (!pairs[i].equals("*")) {
                    faces.put(pairs[i], pairs[i + 1]);
                }
            }
            faces.values().removeIf(v -> v == null || v.equals("-"));
            this.boxes.add(new Box(new float[]{x1, y1, z1}, new float[]{x2, y2, z2}, faces));
            return this;
        }

        /** Every box stretched upward from the floor by {@code k}. */
        Model taller(float k) {
            Model out = this.copyHeader();
            for (Box b : this.boxes) {
                out.boxes.add(new Box(new float[]{b.from()[0], b.from()[1] * k, b.from()[2]},
                        new float[]{b.to()[0], b.to()[1] * k, b.to()[2]}, b.faces()));
            }
            return out;
        }

        /** Every box moved by (dx, dy, dz). */
        Model shifted(float dx, float dy, float dz) {
            Model out = this.copyHeader();
            for (Box b : this.boxes) {
                out.boxes.add(new Box(
                        new float[]{b.from()[0] + dx, b.from()[1] + dy, b.from()[2] + dz},
                        new float[]{b.to()[0] + dx, b.to()[1] + dy, b.to()[2] + dz}, b.faces()));
            }
            return out;
        }

        Model add(Model other) {
            this.textures.putAll(other.textures);
            this.boxes.addAll(other.boxes);
            return this;
        }

        Model copyHeader() {
            Model out = new Model();
            out.textures.putAll(this.textures);
            out.explicitUv = this.explicitUv;
            out.display = this.display;
            return out;
        }

        /** The part of the whole that falls in one cell, moved to that cell's own frame. */
        Model cell(int cx, int cy, int cz) {
            Model out = this.copyHeader();
            float[] lo = {cx * 16, cy * 16, cz * 16};
            float[] hi = {lo[0] + 16, lo[1] + 16, lo[2] + 16};
            for (Box b : this.boxes) {
                float[] f = new float[3];
                float[] t = new float[3];
                boolean empty = false;
                for (int a = 0; a < 3; a++) {
                    f[a] = Math.max(b.from()[a], lo[a]);
                    t[a] = Math.min(b.to()[a], hi[a]);
                    boolean flat = b.from()[a] == b.to()[a];
                    if (flat ? (f[a] > t[a] || b.from()[a] < lo[a] || b.from()[a] >= hi[a]) : f[a] >= t[a]) {
                        empty = true;
                    }
                }
                if (empty) {
                    continue;
                }
                Map<String, String> faces = new LinkedHashMap<>(b.faces());
                if (f[0] > b.from()[0]) faces.remove("west");
                if (t[0] < b.to()[0]) faces.remove("east");
                if (f[1] > b.from()[1]) faces.remove("down");
                if (t[1] < b.to()[1]) faces.remove("up");
                if (f[2] > b.from()[2]) faces.remove("north");
                if (t[2] < b.to()[2]) faces.remove("south");
                if (faces.isEmpty()) {
                    continue;
                }
                for (int a = 0; a < 3; a++) {
                    f[a] -= lo[a];
                    t[a] -= lo[a];
                }
                out.boxes.add(new Box(f, t, faces));
            }
            return out;
        }

        /** The whole thing shrunk around its middle to fit {@code size} across, for an item. */
        Model shrunk(float size) {
            float[] min = {Float.MAX_VALUE, Float.MAX_VALUE, Float.MAX_VALUE};
            float[] max = {-Float.MAX_VALUE, -Float.MAX_VALUE, -Float.MAX_VALUE};
            for (Box b : this.boxes) {
                for (int a = 0; a < 3; a++) {
                    min[a] = Math.min(min[a], b.from()[a]);
                    max[a] = Math.max(max[a], b.to()[a]);
                }
            }
            float extent = Math.max(max[0] - min[0], Math.max(max[1] - min[1], max[2] - min[2]));
            float s = size / extent;
            Model out = this.copyHeader();
            out.explicitUv = true;
            float bottom = 8 - (max[1] - min[1]) * s / 2;
            for (Box b : this.boxes) {
                float[] f = new float[3];
                float[] t = new float[3];
                for (int a = 0; a < 3; a++) {
                    float mid = (min[a] + max[a]) / 2;
                    f[a] = a == 1 ? bottom + (b.from()[a] - min[a]) * s : 8 + (b.from()[a] - mid) * s;
                    t[a] = a == 1 ? bottom + (b.to()[a] - min[a]) * s : 8 + (b.to()[a] - mid) * s;
                }
                // Hairline pieces vanish when shrunk, so keep them at least a sliver thick.
                for (int a = 0; a < 3; a++) {
                    if (t[a] - f[a] < 0.15F && b.to()[a] > b.from()[a]) {
                        t[a] = f[a] + 0.15F;
                    }
                }
                out.boxes.add(new Box(f, t, b.faces()));
            }
            float k = 16 / size;
            out.display = String.format(Locale.ROOT, """
                      "display": {
                        "gui": { "rotation": [30, 225, 0], "translation": [0, 0, 0], "scale": [%1$s, %1$s, %1$s] },
                        "ground": { "rotation": [0, 0, 0], "translation": [0, 3, 0], "scale": [%2$s, %2$s, %2$s] },
                        "fixed": { "rotation": [0, 0, 0], "translation": [0, 0, 0], "scale": [%3$s, %3$s, %3$s] },
                        "thirdperson_righthand": { "rotation": [75, 45, 0], "translation": [0, 2.5, 0], "scale": [%4$s, %4$s, %4$s] },
                        "firstperson_righthand": { "rotation": [0, 45, 0], "translation": [0, 0, 0], "scale": [%5$s, %5$s, %5$s] },
                        "firstperson_lefthand": { "rotation": [0, 225, 0], "translation": [0, 0, 0], "scale": [%5$s, %5$s, %5$s] }
                      },
                    """, num(0.625F * k), num(0.25F * k), num(0.5F * k), num(0.375F * k), num(0.4F * k));
            return out;
        }

        String json() {
            StringBuilder b = new StringBuilder("{\n  \"parent\": \"minecraft:block/block\",\n");
            if (this.display != null) {
                b.append(this.display);
            }
            b.append("  \"textures\": {\n");
            int i = 0;
            for (Map.Entry<String, String> e : this.textures.entrySet()) {
                b.append("    \"").append(e.getKey()).append("\": \"").append(e.getValue()).append('"')
                        .append(++i < this.textures.size() ? ",\n" : "\n");
            }
            b.append("  },\n  \"elements\": [\n");
            for (int k = 0; k < this.boxes.size(); k++) {
                Box box = this.boxes.get(k);
                b.append("    { \"from\": ").append(vec(box.from())).append(", \"to\": ").append(vec(box.to()));
                // A running lamp gives light of its own, so it reads at night too.
                if (box.faces().containsValue("#lamp") && LAMP_ON.equals(this.textures.get("lamp"))) {
                    b.append(", \"light_emission\": 12");
                }
                b.append(", \"faces\": {");
                int f = 0;
                for (Map.Entry<String, String> face : box.faces().entrySet()) {
                    b.append(f++ == 0 ? " " : ", ").append('"').append(face.getKey())
                            .append("\": { ");
                    // A box past the block would take its UVs past the texture's edge,
                    // into the neighbouring sprites, so it gets them written out.
                    if (this.explicitUv || outside(box)) {
                        b.append("\"uv\": ").append(uv(box, face.getKey())).append(", ");
                    }
                    b.append("\"texture\": \"").append(face.getValue()).append("\" }");
                }
                b.append(" } }").append(k < this.boxes.size() - 1 ? ",\n" : "\n");
            }
            return b.append("  ]\n}\n").toString();
        }
    }

    static boolean outside(Box box) {
        for (int i = 0; i < 3; i++) {
            if (box.from()[i] < 0 || box.to()[i] > 16) {
                return true;
            }
        }
        return false;
    }

    /** A face's own size, from the corner of the texture, never past its edge. */
    static String uv(Box box, String face) {
        float dx = box.to()[0] - box.from()[0];
        float dy = box.to()[1] - box.from()[1];
        float dz = box.to()[2] - box.from()[2];
        float w;
        float h;
        switch (face) {
            case "north", "south" -> { w = dx; h = dy; }
            case "east", "west" -> { w = dz; h = dy; }
            default -> { w = dx; h = dz; }
        }
        return String.format(Locale.ROOT, "[0, 0, %s, %s]", num(Math.min(16, Math.max(0.1F, w))),
                num(Math.min(16, Math.max(0.1F, h))));
    }

    static String vec(float[] v) {
        return String.format(Locale.ROOT, "[%s, %s, %s]", num(v[0]), num(v[1]), num(v[2]));
    }

    static String num(float f) {
        return f == (int) f ? Integer.toString((int) f) : String.format(Locale.ROOT, "%.3f", f);
    }

    static void write(String path, String text) throws IOException {
        Path file = Path.of(ROOT + path);
        Files.createDirectories(file.getParent());
        Files.writeString(file, text);
    }

    static void model(String name, Model model) throws IOException {
        write("models/block/" + name + ".json", model.json());
    }

    // ---- textures, all vanilla but the deposit's ----

    static final String STONE = "minecraft:block/smooth_stone";
    static final String IRON = "minecraft:block/iron_block";
    static final String DARK = "minecraft:block/black_concrete";
    static final String GREY = "minecraft:block/gray_concrete";
    static final String LIGHT = "minecraft:block/light_gray_concrete";
    static final String WHITE = "minecraft:block/white_concrete";
    static final String COPPER = "minecraft:block/cut_copper";
    static final String RED = "minecraft:block/red_concrete";
    static final String BRICK = "minecraft:block/bricks";
    static final String YELLOW = "minecraft:block/yellow_concrete";
    static final String GREEN = "minecraft:block/green_concrete";
    static final String LIME = "minecraft:block/lime_concrete";
    static final String COAL = "minecraft:block/coal_block";
    static final String MAGMA = "minecraft:block/magma";
    static final String LAMP = "minecraft:block/redstone_lamp";
    static final String LAMP_ON = "minecraft:block/redstone_lamp_on";
    static final String PLANKS = "minecraft:block/spruce_planks";

    // ---- the refinery plant: 4 x 6 x 4 ----

    static final String WINDOW = "minecraft:block/light_blue_concrete";

    /** An upright cylinder, as an octagon of two crossed boxes, from y1 to y2 about (cx, cz). */
    static void drum(Model m, float cx, float cz, float r, float y1, float y2, String... faces) {
        float k = r * 0.7F;
        m.box(cx - r, y1, cz - k, cx + r, y2, cz + k, faces);
        m.box(cx - k, y1, cz - r, cx + k, y2, cz + r, faces);
    }

    /**
     * Four blocks by four of plant on one concrete pad. Across the front the
     * boiler house (left) and the control room (right); behind them the crude
     * tank; at the back the distillation column, six blocks high with two
     * platforms and a ladder, the stripper beside it on its pipe rack, and the
     * brick stack in the far corner. Copper lines tie it all together.
     */
    static Model refinery(boolean lit) {
        Model m = new Model()
                .tex("particle", "overthought_vehicles:block/fuel_refinery_side")
                .tex("front", lit ? "overthought_vehicles:block/fuel_refinery_front_lit" : "overthought_vehicles:block/fuel_refinery_front")
                .tex("side", "overthought_vehicles:block/fuel_refinery_side")
                .tex("top", "overthought_vehicles:block/fuel_refinery_top")
                .tex("stone", STONE).tex("iron", IRON).tex("copper", COPPER).tex("dark", DARK)
                .tex("grey", GREY).tex("brick", BRICK).tex("light", LIGHT).tex("red", RED)
                .tex("white", WHITE).tex("window", WINDOW);
        String glow = lit ? "#red" : "#dark";
        // the pad, with a kerb
        m.box(0, 0, 0, 64, 2, 64, "#stone");
        m.box(0, 2, 0, 64, 2.5F, 1, "#grey");
        // the boiler house, front left: two bays of furnace front
        m.box(1, 2, 1, 32, 15, 22, "*", "#side", "north", "#front", "up", "-", "down", "-");
        m.box(0.5F, 15, 0.5F, 32.5F, 16.5F, 22.5F, "#grey");
        m.box(2, 16.5F, 2, 31, 18, 21, "#top");
        // the control room, front right: windows, a door and its lamp
        m.box(34, 2, 1, 63, 13, 16, "*", "#light", "up", "-", "down", "-");
        m.box(33.5F, 13, 0.5F, 63.5F, 14.5F, 16.5F, "#grey");
        m.box(37, 6, 0.6F, 48, 10, 1, "#window");
        m.box(42.25F, 6, 0.4F, 42.75F, 10, 0.6F, "#grey");
        m.box(53, 2, 0.6F, 58, 10, 1, "#dark");
        m.box(54.5F, 10.5F, 0.6F, 56.5F, 11.5F, 1, glow);
        m.box(45, 14.5F, 6, 49, 18, 10, "*", "#iron", "down", "-");
        // the crude tank behind the control room, banded, with its roof
        drum(m, 48, 34, 12, 2, 30, "*", "#white", "down", "-");
        drum(m, 48, 34, 12.3F, 14, 15, "*", "#grey", "down", "-", "up", "-");
        drum(m, 48, 34, 10, 30, 32, "*", "#grey", "down", "-");
        m.box(47, 32, 33, 49, 34, 35, "#dark");
        m.box(36.5F, 2, 33, 37, 30, 35, "#dark");
        // the reboiler under the column, its firebox door glowing while it runs
        m.box(14, 2, 28, 34, 12, 52, "*", "#iron", "down", "-");
        m.box(20, 3, 27.5F, 28, 8, 28, glow);
        // the distillation column, back left of centre
        drum(m, 24, 40, 7, 12, 92, "*", "#iron", "down", "-");
        for (float y : new float[]{24, 40, 56, 72, 86}) {
            drum(m, 24, 40, 7.5F, y, y + 1, "*", "#copper", "down", "-");
        }
        drum(m, 24, 40, 6, 92, 94, "*", "#light", "down", "-");
        m.box(23, 94, 39, 25, 96, 41, "*", "#dark", "down", "-");
        // two working platforms with their rails
        for (float y : new float[]{50, 74}) {
            m.box(15, y, 31, 33, y + 1, 49, "#grey");
            m.box(15, y + 1, 31, 15.5F, y + 4, 49, "#dark");
            m.box(32.5F, y + 1, 31, 33, y + 4, 49, "#dark");
            m.box(15, y + 1, 48.5F, 33, y + 4, 49, "#dark");
            m.box(15, y + 3.5F, 31, 33, y + 4, 31.5F, "#dark");
        }
        // the ladder up the front of the column, from the boiler house roof
        m.box(21.5F, 18, 32.2F, 22, 78, 32.7F, "#dark");
        m.box(26, 18, 32.2F, 26.5F, 78, 32.7F, "#dark");
        for (int y = 20; y < 78; y += 3) {
            m.box(22, y, 32.3F, 26, y + 0.5F, 32.6F, "#dark");
        }
        // the stripper, back left, and the rack its lines run along
        drum(m, 8, 56, 5, 2, 66, "*", "#iron", "down", "-");
        for (float y : new float[]{20, 40, 58}) {
            drum(m, 8, 56, 5.5F, y, y + 1, "*", "#copper", "down", "-");
        }
        drum(m, 8, 56, 4, 66, 68, "*", "#light", "down", "-");
        for (float x : new float[]{20, 30, 40}) {
            m.box(x, 2, 56.5F, x + 1, 20, 57.5F, "#dark");
        }
        // the brick stack, far right corner: tapering, banded, blackened at the top
        m.box(50, 2, 50, 62, 64, 62, "*", "#brick", "down", "-");
        m.box(52, 64, 52, 60, 86, 60, "*", "#brick", "down", "-");
        m.box(49.5F, 40, 49.5F, 62.5F, 41, 62.5F, "#dark");
        m.box(51.5F, 86, 51.5F, 60.5F, 88, 60.5F, "*", "#dark", "up", "-");
        m.box(53, 86, 53, 59, 87.5F, 59, "up", "#dark");
        m.box(54, 3, 49.5F, 58, 8, 50, glow);
        // the lines: house to reboiler, tank to house, column to stripper,
        // stripper back to the tank along the rack, and the overhead to the stack
        m.box(23, 6, 22, 25, 8, 28, "#copper");
        m.box(33, 8, 22, 35, 10, 32, "#copper");
        m.box(33, 8, 30, 37, 10, 32, "#copper");
        m.box(9, 30, 43, 17, 32, 45, "#copper");
        m.box(9, 30, 45, 11, 32, 51, "#copper");
        m.box(13, 20, 56, 47, 22, 58, "#copper");
        m.box(45, 20, 46, 47, 22, 56, "#copper");
        m.box(31, 70, 40, 56, 72, 42, "#copper");
        m.box(54, 70, 42, 56, 72, 52, "#copper");
        return m;
    }

    /** Every cell of a whole that has something in it, the controller's first. */
    static List<int[]> occupied(Model whole, int nx, int ny, int nz) {
        List<int[]> out = new ArrayList<>();
        out.add(new int[]{0, 0, 0});
        for (int y = 0; y < ny; y++) {
            for (int z = 0; z < nz; z++) {
                for (int x = 0; x < nx; x++) {
                    if ((x != 0 || y != 0 || z != 0) && !whole.cell(x, y, z).boxes.isEmpty()) {
                        out.add(new int[]{x, y, z});
                    }
                }
            }
        }
        return out;
    }

    /** The box round what a cell holds, in that cell's sixteenths. */
    static float[] bounds(Model cell) {
        float[] b = {16, 16, 16, 0, 0, 0};
        for (Box box : cell.boxes) {
            for (int a = 0; a < 3; a++) {
                b[a] = Math.min(b[a], box.from()[a]);
                b[a + 3] = Math.max(b[a + 3], box.to()[a]);
            }
        }
        for (int a = 0; a < 3; a++) {
            if (b[a + 3] - b[a] < 1) {
                float mid = (b[a] + b[a + 3]) / 2;
                b[a] = Math.max(0, mid - 0.5F);
                b[a + 3] = Math.min(16, b[a] + 1);
            }
        }
        return b;
    }

    /** Writes the refinery's cells, outlines and lit parts where the block reads them. */
    static void refineryLayout(int[][] cells, List<float[]> outlines, int columnTop, int stackTop) throws IOException {
        StringBuilder b = new StringBuilder();
        b.append("package me.trossty.car.block;\n\n");
        b.append("/** Written by tools/GenPlantModels.java from the refinery's model; do not edit. */\n");
        b.append("final class FuelRefineryLayout {\n\n");
        b.append("    /** Every cell with something in it, the controller first; machine frame. */\n");
        b.append("    static final int[][] CELLS = {\n");
        for (int[] c : cells) {
            b.append(String.format(Locale.ROOT, "            {%d, %d, %d},\n", c[0], c[1], c[2]));
        }
        b.append("    };\n\n");
        b.append("    /** What each cell holds, as a box in sixteenths, facing north. */\n");
        b.append("    static final double[][] OUTLINES = {\n");
        for (float[] o : outlines) {
            b.append(String.format(Locale.ROOT, "            {%s, %s, %s, %s, %s, %s},\n",
                    num(o[0]), num(o[1]), num(o[2]), num(o[3]), num(o[4]), num(o[5])));
        }
        b.append("    };\n\n");
        b.append("    /** The top of the column, which steams, and of the stack, which smokes. */\n");
        b.append("    static final int COLUMN_TOP = ").append(columnTop).append(";\n");
        b.append("    static final int STACK_TOP = ").append(stackTop).append(";\n\n");
        b.append("    private FuelRefineryLayout() {\n    }\n}\n");
        Files.writeString(Path.of("src/main/java/me/trossty/car/block/FuelRefineryLayout.java"), b.toString());
    }

    // ---- the hydrotreater: 1 x 2 x 1 ----

    static Model hydrotreater(boolean lit) {
        Model m = new Model()
                .tex("particle", IRON)
                .tex("iron", IRON).tex("dark", DARK).tex("copper", COPPER).tex("white", WHITE)
                .tex("red", RED).tex("grey", GREY).tex("lamp", lit ? LAMP_ON : LAMP).tex("light", LIGHT);
        // the skid and the saddles
        m.box(0, 0, 0.5F, 16, 1, 15.5F, "#dark");
        m.box(3, 1, 3, 13, 3, 5, "#grey");
        m.box(3, 1, 11, 13, 3, 13, "#grey");
        // the reactor: an octagon, banded
        m.box(3, 2, 4.5F, 13, 26, 11.5F, "*", "#light", "down", "-");
        m.box(4.5F, 2, 3, 11.5F, 26, 13, "*", "#light", "down", "-");
        for (float y : new float[]{6, 14, 22}) {
            m.box(2.5F, y, 4, 13.5F, y + 1, 12, "#iron");
            m.box(4, y, 2.5F, 12, y + 1, 13.5F, "#iron");
        }
        // the domed head with its relief valve
        m.box(4.5F, 26, 5, 11.5F, 28, 11, "#light");
        m.box(6, 28, 6.5F, 10, 29, 9.5F, "#light");
        m.box(7, 29, 7, 9, 31, 9, "#red");
        // the hydrogen bottle strapped on the right, white with a red neck
        m.box(13, 1, 12, 15.5F, 19, 14.5F, "#white");
        m.box(13.5F, 19, 12.5F, 15, 21, 14, "#red");
        m.box(12.5F, 15, 11.5F, 13, 16, 15, "#dark");
        // the raw feed coming in on the left, and the clean fuel going out low
        m.box(0.5F, 1, 7, 2.5F, 20, 9, "#copper");
        m.box(0.5F, 18, 7, 4.5F, 20, 9, "#copper");
        m.box(11.5F, 4, 7, 15.5F, 6, 9, "#copper");
        // the panel on the front, with its running lamp
        m.box(5, 8, 1.5F, 11, 13, 3, "*", "#grey");
        m.box(6, 10, 1.2F, 8, 12, 1.5F, "#lamp");
        m.box(9, 10, 1.2F, 10, 11, 1.5F, "#white");
        return m;
    }

    // ---- the pumpjack: 1 x 3 x 3, beam and crank drawn on their own ----

    static Model pumpjack() {
        return pumpjack(false);
    }

    static Model pumpjack(boolean lit) {
        Model m = new Model()
                .tex("particle", YELLOW)
                .tex("stone", STONE).tex("dark", DARK).tex("iron", IRON).tex("grey", GREY)
                .tex("yellow", YELLOW).tex("red", RED).tex("copper", COPPER)
                .tex("lamp", lit ? LAMP_ON : LAMP);
        // the motor's starter box on its right-hand side, with the running lamp
        m.box(12, 7, 37.5F, 13.5F, 12, 41.5F, "#grey");
        m.box(13.5F, 9.5F, 38.5F, 13.8F, 11, 40, "#lamp");
        m.box(13.5F, 8, 40.5F, 13.8F, 9, 41, "#red");
        // the foundation and the base rails
        m.box(0, 0, 0, 16, 2, 48, "#stone");
        m.box(2, 2, 12, 4, 4, 46, "#dark");
        m.box(12, 2, 12, 14, 4, 46, "#dark");
        m.box(2, 2, 20, 14, 3.5F, 22, "#dark");
        // the wellhead: flange, casing, tee and stuffing box, and the polished rod
        m.box(3, 2, 3, 13, 3, 13, "#iron");
        m.box(5.5F, 3, 5.5F, 10.5F, 8, 10.5F, "#dark");
        m.box(2, 5.5F, 7, 14, 7.5F, 9, "#iron");
        m.box(1, 4.5F, 6, 2, 8.5F, 10, "#red");
        m.box(6, 8, 6, 10, 11, 10, "#iron");
        m.box(7.5F, 11, 7.5F, 8.5F, 28, 8.5F, "#iron");
        // the Samson post, a tripod: a leg either side leaning in from the
        // front, one down the middle from the back (inside the counterweights'
        // swing), stepped in to meet under the bearing, braced across
        for (int step = 0; step < 10; step++) {
            float y = 3 + step * 3.5F;
            float front = 12.5F + step;
            m.box(1.5F, y, front, 4, y + 3.5F, front + 3.5F, "#yellow");
            m.box(12, y, front, 14.5F, y + 3.5F, front + 3.5F, "#yellow");
            float back = 31.5F - step;
            m.box(6.5F, y, back, 9.5F, y + 3.5F, back + 3, "#yellow");
        }
        m.box(4, 14, 16, 12, 15.5F, 18, "#yellow");
        m.box(4, 26, 19, 12, 27.5F, 21, "#yellow");
        m.box(2, 2, 30, 14, 3.5F, 35, "#dark");
        m.box(1, 37, 21.5F, 15, 42, 26.5F, "#dark");
        // the gearbox and the motor at the back
        m.box(4, 4, 35, 12, 16, 45, "*", "#grey", "down", "-");
        m.box(4.5F, 16, 37, 11.5F, 17, 43, "#grey");
        m.box(5, 4, 45, 11, 10, 48, "#red");
        m.box(6, 5, 47.5F, 10, 9, 48, "#dark");
        m.box(5.5F, 10, 40, 6.5F, 14, 45.5F, "#dark");
        return m;
    }

    /** The walking beam, in the frame of the bearing's cell: pivot at its middle. */
    static Model beam() {
        Model m = new Model()
                .tex("particle", YELLOW)
                .tex("yellow", YELLOW).tex("dark", DARK).tex("iron", IRON).tex("grey", GREY);
        m.explicitUv = true;
        // the beam itself, an I-section over the bearing
        m.box(5.5F, 7, -6, 10.5F, 7.6F, 31, "#yellow");
        m.box(6.5F, 7.6F, -6, 9.5F, 9.9F, 31, "#yellow");
        m.box(5.5F, 9.9F, -6, 10.5F, 10.5F, 31, "#yellow");
        m.box(5.5F, 6, 6, 10.5F, 7, 10, "#dark");
        // the horse head over the well, rounded at the front
        m.box(4.5F, -1, -12.5F, 11.5F, 14, -5, "#yellow");
        m.box(4.5F, 1, -14, 11.5F, 12, -12.5F, "#yellow");
        m.box(4.5F, 3, -15, 11.5F, 10, -14, "#yellow");
        // the bridle and carrier bar down to the rod
        m.box(6.6F, -6, -8.4F, 7, -1, -7.6F, "#iron");
        m.box(9, -6, -8.4F, 9.4F, -1, -7.6F, "#iron");
        m.box(6, -7, -9, 10, -6, -7, "#dark");
        // the equaliser and the pitman arms down to the crank
        m.box(0.5F, 5, 23, 15.5F, 7, 25, "#dark");
        m.box(0.5F, -8, 22.75F, 1.75F, 5, 25.25F, "#grey");
        m.box(14.25F, -8, 22.75F, 15.5F, 5, 25.25F, "#grey");
        return m;
    }

    /** The crank and its counterweights, in the frame of its cell: shaft at (8, 2, 8). */
    static Model crank() {
        Model m = new Model()
                .tex("particle", DARK)
                .tex("dark", DARK).tex("iron", IRON).tex("red", RED).tex("grey", GREY);
        m.explicitUv = true;
        m.box(1.5F, 1, 7, 14.5F, 3, 9, "#iron");
        for (float x : new float[]{1.75F, 12.25F}) {
            m.box(x, 0, 6.5F, x + 2, 9, 9.5F, "#grey");
            // a heavy plate, its outer edge rounded off in a step
            m.box(x - 0.25F, -9, 2, x + 2.25F, -1, 14, "#red");
            m.box(x - 0.25F, -10.5F, 4.5F, x + 2.25F, -9, 11.5F, "#red");
            m.box(x - 0.5F, -2.5F, 6.5F, x + 2.5F, 0, 9.5F, "#dark");
        }
        m.box(0.5F, 7, 7, 1.75F, 9, 9, "#iron");
        m.box(14.25F, 7, 7, 15.5F, 9, 9, "#iron");
        return m;
    }

    // ---- the generators: one block each, portable site sets in a tube frame ----

    /** How much taller than the first cut the sets stand: they top out just under a block. */
    static final float GENERATOR_STRETCH = 1.22F;

    static final String LIGHT_BLUE = "minecraft:block/light_blue_concrete";
    static final String ORANGE = "minecraft:block/orange_concrete";

    /**
     * The set they all share, after a site generator: a rounded tube frame
     * on rubber feet, the fuel tank across the top inside it, the alternator
     * and its black control panel on the right as you face it — sockets,
     * voltmeter, switch, the running lamp — and the engine with its recoil
     * starter on the left, its muffler at the very end.
     */
    static Model portable(String body, String accent, boolean lit) {
        Model m = new Model()
                .tex("particle", body)
                .tex("body", body).tex("accent", accent).tex("frame", GREY).tex("dark", DARK)
                .tex("iron", IRON).tex("engine", LIGHT).tex("socket", LIGHT_BLUE).tex("white", WHITE)
                .tex("switch", LIME).tex("lamp", lit ? LAMP_ON : LAMP);
        // the frame: a rounded loop at the front and one at the back...
        for (float z : new float[]{1.5F, 13.5F}) {
            m.box(2.5F, 11, z, 13.5F, 12, z + 1, "#frame");
            m.box(1.5F, 10, z, 2.5F, 11, z + 1, "#frame");
            m.box(13.5F, 10, z, 14.5F, 11, z + 1, "#frame");
            m.box(0.5F, 3, z, 1.5F, 10, z + 1, "#frame");
            m.box(14.5F, 3, z, 15.5F, 10, z + 1, "#frame");
            m.box(1.5F, 2, z, 2.5F, 3, z + 1, "#frame");
            m.box(13.5F, 2, z, 14.5F, 3, z + 1, "#frame");
            m.box(2.5F, 1, z, 13.5F, 2, z + 1, "#frame");
        }
        // ...tied across at the top ends for the handles and below for the feet
        m.box(1.5F, 10, 2.5F, 2.5F, 11, 13.5F, "#frame");
        m.box(13.5F, 10, 2.5F, 14.5F, 11, 13.5F, "#frame");
        m.box(3, 1, 2.5F, 4, 2, 13.5F, "#frame");
        m.box(12, 1, 2.5F, 13, 2, 13.5F, "#frame");
        for (float[] f : new float[][]{{3, 2.5F}, {12, 2.5F}, {3, 12.5F}, {12, 12.5F}}) {
            m.box(f[0], 0, f[1], f[0] + 1, 1, f[1] + 1, "#dark");
        }
        // the bed the works sit on
        m.box(3, 2, 3, 13, 2.5F, 13, "#dark");
        // the alternator and its control panel, right of centre as you face it
        m.box(2.5F, 2.5F, 3.2F, 8.8F, 9, 12.5F, "*", "#dark", "down", "-");
        m.box(2.9F, 8.2F, 3, 8.4F, 8.5F, 3.2F, "#accent");
        m.box(2.9F, 3, 3, 3.2F, 8.2F, 3.2F, "#accent");
        m.box(8.1F, 3, 3, 8.4F, 8.2F, 3.2F, "#accent");
        m.box(3.6F, 6.2F, 2.9F, 4.4F, 7.4F, 3.2F, "#switch");
        m.box(3.6F, 4.4F, 2.9F, 4.4F, 5.2F, 3.2F, "#lamp");
        m.box(5, 6, 2.9F, 6.4F, 7.5F, 3.2F, "#white");
        m.box(4.9F, 3.6F, 2.8F, 6.3F, 5.2F, 3.2F, "#socket");
        m.box(6.7F, 3.6F, 2.8F, 8.1F, 5.2F, 3.2F, "#socket");
        m.box(6.9F, 6.2F, 2.9F, 7.7F, 7.2F, 3.2F, "#iron");
        // the engine, left of centre: crankcase, recoil starter, cylinder head
        m.box(8.8F, 2.5F, 4, 12.6F, 7.5F, 12, "#body");
        m.box(9.6F, 3.8F, 3.5F, 11.8F, 6.2F, 4, "#frame");
        m.box(10.1F, 3.3F, 3.5F, 11.3F, 6.7F, 4, "#frame");
        m.box(10.3F, 4.6F, 3.15F, 11.1F, 5.4F, 3.5F, "#iron");
        m.box(9.3F, 7.5F, 6, 12.1F, 9, 10, "#engine");
        // the muffler at the far end, its outlet up the back corner
        m.box(12.6F, 3, 4.5F, 14.2F, 8.5F, 11, "#dark");
        m.box(12.9F, 8.5F, 9.5F, 13.9F, 10, 10.5F, "*", "#iron", "up", "-");
        // the tank across the top, its crown and its cap
        m.box(3, 9, 3, 13, 11.2F, 13, "*", "#body", "down", "-");
        m.box(3.5F, 11.2F, 3.5F, 12.5F, 11.8F, 12.5F, "*", "#body", "down", "-");
        m.box(3, 9.5F, 2.8F, 13, 10.3F, 3, "#dark");
        return m;
    }

    /** The coal one: red, an open hopper of coal for a tank and a firebox for an engine. */
    static Model coalGenerator(boolean lit) {
        Model m = portable(RED, ORANGE, lit).tex("coal", COAL).tex("fire", lit ? MAGMA : COAL);
        m.box(5, 11.8F, 5, 11, 12.4F, 11, "*", "#dark", "up", "-");
        m.box(5.5F, 12, 5.5F, 10.5F, 12.2F, 10.5F, "up", "#coal");
        // the firebox door where the starter would be, glowing while it burns
        m.box(9.5F, 3.6F, 3.2F, 11.9F, 6.4F, 3.4F, "#fire");
        return m.taller(GENERATOR_STRETCH);
    }

    /** The biofuel one: green, with the filler cap and a fuel gauge on the tank. */
    static Model bioGenerator(boolean lit) {
        Model m = portable(LIME, GREEN, lit);
        m.box(5, 11.8F, 7, 7, 12.4F, 9, "#iron");
        m.box(5.5F, 11.8F, 6.5F, 6.5F, 12.4F, 9.5F, "#iron");
        m.box(9, 11.8F, 7.5F, 11, 12, 8.5F, "#white");
        return m.taller(GENERATOR_STRETCH);
    }

    /** The diesel one: yellow, air-cooled, finned head and a bigger cap. */
    static Model dieselGenerator(boolean lit) {
        Model m = portable(YELLOW, ORANGE, lit);
        m.box(4.5F, 11.8F, 6.5F, 7.5F, 12.5F, 9.5F, "#iron");
        m.box(5.5F, 12.5F, 7.5F, 6.5F, 12.8F, 8.5F, "#dark");
        for (float y = 7.7F; y < 9; y += 0.6F) {
            m.box(9.1F, y, 5.6F, 12.3F, y + 0.3F, 10.4F, "#iron");
        }
        return m.taller(GENERATOR_STRETCH);
    }

    // ---- the battery bank and the charging post ----

    /**
     * A battery cabinet on a plinth: a grey case with a door, a window of four
     * charge bars that light from the bottom up, the two terminals on top and
     * the output socket low on the door — the one face it gives power from.
     */
    static Model batteryBank(int level) {
        Model m = new Model().tex("particle", GREY)
                .tex("case", GREY).tex("door", LIGHT).tex("dark", DARK).tex("on", LIME).tex("off", GREEN)
                .tex("plus", RED).tex("copper", COPPER).tex("warn", YELLOW).tex("socket", LIGHT_BLUE);
        m.box(1, 0, 1, 15, 1, 15, "#dark");
        m.box(1.5F, 1, 2, 14.5F, 14, 15, "#case");
        m.box(2.5F, 2, 1.6F, 13.5F, 13, 2, "*", "#door", "south", "-");
        m.box(4, 4.5F, 1.4F, 12, 12, 1.6F, "*", "#dark", "south", "-");
        for (int i = 0; i < 4; i++) {
            float y = 5.2F + i * 1.7F;
            m.box(5, y, 1.2F, 11, y + 1.1F, 1.4F, "*", i < level ? "#on" : "#off", "south", "-");
        }
        m.box(4, 12.3F, 1.4F, 12, 12.7F, 1.6F, "*", "#warn", "south", "-");
        m.box(6, 2.4F, 1.3F, 10, 3.8F, 1.6F, "*", "#socket", "south", "-");
        // the lid overhanging the case, a vent grille on it at the back
        m.box(1.1F, 14, 1.6F, 14.9F, 14.6F, 15.4F, "#dark");
        for (float x = 4; x < 12; x += 1.5F) {
            m.box(x, 14.6F, 10.5F, x + 0.7F, 14.9F, 14, "#case");
        }
        // terminals: red for plus, black for minus, copper caps and a bus bar
        m.box(3.5F, 14.6F, 5, 5.5F, 15.6F, 7, "#plus");
        m.box(10.5F, 14.6F, 5, 12.5F, 15.6F, 7, "#dark");
        m.box(3.9F, 15.6F, 5.4F, 5.1F, 16, 6.6F, "#copper");
        m.box(10.9F, 15.6F, 5.4F, 12.1F, 16, 6.6F, "#copper");
        m.box(5.5F, 14.6F, 5.6F, 10.5F, 15, 6.4F, "#copper");
        // front corner posts framing the door, its hinges and a pull handle
        m.box(1.2F, 0.95F, 1.5F, 2.4F, 14.05F, 2.6F, "#dark");
        m.box(13.6F, 0.95F, 1.5F, 14.8F, 14.05F, 2.6F, "#dark");
        m.box(2.4F, 3.5F, 1.4F, 3, 4.7F, 1.7F, "#dark");
        m.box(2.4F, 10.5F, 1.4F, 3, 11.7F, 1.7F, "#dark");
        m.box(12.3F, 6.5F, 1.0F, 12.9F, 9.5F, 1.4F, "#warn");
        // a hazard plate on each side, between the slats and the front
        m.box(1.3F, 9, 2.8F, 1.5F, 12, 4.6F, "*", "#warn", "east", "-");
        m.box(14.5F, 9, 2.8F, 14.7F, 12, 4.6F, "*", "#warn", "west", "-");
        // cooling slats down both sides
        for (float y = 4; y < 12; y += 1.6F) {
            m.box(1.3F, y, 5, 1.5F, y + 0.6F, 12, "*", "#dark", "east", "-");
            m.box(14.5F, y, 5, 14.7F, y + 0.6F, 12, "*", "#dark", "west", "-");
        }
        return m;
    }

    /**
     * A pedestal charger, a block and a half tall so it stands about level
     * with a car's roof: a plinth, a slim white column with a green stripe up
     * its face, the head with the screen and the status light, the plug
     * docked in its holster on the right and the cable looping down to it.
     */
    static Model chargingPost(boolean lit) {
        Model m = new Model().tex("particle", WHITE)
                .tex("body", WHITE).tex("base", GREY).tex("dark", DARK).tex("accent", GREEN)
                .tex("screen", lit ? LIGHT_BLUE : DARK).tex("lamp", lit ? LIME : GREEN)
                .tex("bezel", GREY);
        // plinth and its kerb
        m.box(3.5F, 0, 4.5F, 12.5F, 1, 11.5F, "#base");
        m.box(4.5F, 1, 5.5F, 11.5F, 1.6F, 10.5F, "#dark");
        // the column, the stripe up its face
        m.box(5.5F, 1.6F, 6.5F, 10.5F, 14, 10.5F, "#body");
        m.box(7.2F, 2.5F, 6.3F, 8.8F, 13.5F, 6.5F, "*", "#accent", "south", "-");
        // the head, a darker collar under it and a green cap over it
        m.box(5, 13.4F, 6, 11, 14.05F, 11, "#dark");
        m.box(4.5F, 14, 5.5F, 11.5F, 23, 11, "#body");
        m.box(4.2F, 23, 5.2F, 11.8F, 23.8F, 11.3F, "#accent");
        // the screen in its bezel, the light under it
        m.box(5.3F, 17, 5.3F, 10.7F, 22, 5.5F, "*", "#bezel", "south", "-");
        m.box(5.8F, 17.5F, 5.1F, 10.2F, 21.5F, 5.3F, "*", "#screen", "south", "-");
        m.box(7.3F, 15.2F, 5.2F, 8.7F, 16.4F, 5.5F, "*", "#lamp", "south", "-");
        // the holster on the right side, the plug's grip standing out of it
        m.box(11.5F, 9, 7, 13, 13.2F, 9.6F, "#dark");
        m.box(11.8F, 13.2F, 7.6F, 12.7F, 14.8F, 9, "#dark");
        m.box(11.6F, 14.8F, 7.4F, 12.9F, 15.4F, 9.2F, "#accent");
        // the cable: out from under the head, down, a loop at the foot, up into the holster
        m.box(10.5F, 4, 9.6F, 11.3F, 14, 10.4F, "#dark");
        m.box(10.5F, 3.2F, 8.6F, 13.6F, 4, 10.4F, "#dark");
        m.box(12.8F, 4, 8.6F, 13.6F, 9, 9.4F, "#dark");
        return m;
    }

    static void batteryStates() throws IOException {
        List<String> lines = new ArrayList<>();
        for (int f = 0; f < 4; f++) {
            for (int level = 0; level <= 4; level++) {
                lines.add(variant("facing=" + FACINGS[f] + ",level=" + level,
                        "overthought_vehicles:block/battery_bank_" + level, f * 90));
            }
        }
        write("blockstates/battery_bank.json", "{\n  \"variants\": {\n" + String.join(",\n", lines) + "\n  }\n}\n");
    }

    // ---- the deposit and the seep ----

    static String cube(String texture) {
        return "{\n  \"parent\": \"minecraft:block/cube_all\",\n  \"textures\": { \"all\": \"" + texture + "\" }\n}\n";
    }

    // ---- blockstates ----

    static final String[] FACINGS = {"north", "east", "south", "west"};

    static String variant(String key, String model, int y) {
        return "    \"" + key + "\": { \"model\": \"" + model + "\"" + (y == 0 ? "" : ", \"y\": " + y) + " }";
    }

    interface PartModel {
        String of(int part, boolean lit);
    }

    static void multiblockStates(String block, int parts, PartModel models) throws IOException {
        List<String> lines = new ArrayList<>();
        for (int f = 0; f < 4; f++) {
            for (boolean lit : new boolean[]{false, true}) {
                for (int part = 0; part < parts; part++) {
                    lines.add(variant("facing=" + FACINGS[f] + ",lit=" + lit + ",part=" + part,
                            models.of(part, lit), f * 90));
                }
            }
        }
        write("blockstates/" + block + ".json", "{\n  \"variants\": {\n" + String.join(",\n", lines) + "\n  }\n}\n");
    }

    static void singleStates(String block, String model) throws IOException {
        List<String> lines = new ArrayList<>();
        for (int f = 0; f < 4; f++) {
            for (boolean lit : new boolean[]{false, true}) {
                lines.add(variant("facing=" + FACINGS[f] + ",lit=" + lit,
                        "overthought_vehicles:block/" + model + (lit ? "_lit" : ""), f * 90));
            }
        }
        write("blockstates/" + block + ".json", "{\n  \"variants\": {\n" + String.join(",\n", lines) + "\n  }\n}\n");
    }

    /** A block model taller than a block, shrunk to fit a slot and a hand. */
    static String tallItem(String parent) {
        return "{\n  \"parent\": \"" + parent + "\",\n  \"display\": {\n"
                + "    \"gui\": { \"rotation\": [30, 225, 0], \"translation\": [0, -3, 0], \"scale\": [0.45, 0.45, 0.45] },\n"
                + "    \"ground\": { \"translation\": [0, 2, 0], \"scale\": [0.25, 0.25, 0.25] },\n"
                + "    \"fixed\": { \"translation\": [0, -2, 0], \"scale\": [0.4, 0.4, 0.4] },\n"
                + "    \"thirdperson_righthand\": { \"rotation\": [75, 45, 0], \"translation\": [0, 1.5, 0], \"scale\": [0.3, 0.3, 0.3] },\n"
                + "    \"thirdperson_lefthand\": { \"rotation\": [75, 45, 0], \"translation\": [0, 1.5, 0], \"scale\": [0.3, 0.3, 0.3] },\n"
                + "    \"firstperson_righthand\": { \"rotation\": [0, 45, 0], \"scale\": [0.3, 0.3, 0.3] },\n"
                + "    \"firstperson_lefthand\": { \"rotation\": [0, 225, 0], \"scale\": [0.3, 0.3, 0.3] }\n"
                + "  }\n}\n";
    }

    /**
     * The item's look. An item draws models/item/<name> on 1.21.1, so one that
     * shows a block model gets a model there that is just that block's.
     */
    static void item(String name, String model) throws IOException {
        if (!model.equals("overthought_vehicles:item/" + name)) {
            write("models/item/" + name + ".json", "{\n  \"parent\": \"" + model + "\"\n}\n");
        }
    }

    static void cells(String dir, Model whole, int[][] cells) throws IOException {
        for (int i = 0; i < cells.length; i++) {
            model(dir + "/p" + i, whole.cell(cells[i][0], cells[i][1], cells[i][2]));
        }
    }

    public static void main(String[] args) throws IOException {
        // The refinery: the cells come from the model, and so do the lit ones.
        Model refinery = refinery(false);
        Model refineryLit = refinery(true);
        List<int[]> refineryList = occupied(refinery, 4, 6, 4);
        int[][] refineryCells = refineryList.toArray(new int[0][]);
        java.util.Set<Integer> litParts = new java.util.HashSet<>();
        List<float[]> outlines = new ArrayList<>();
        int columnTop = 0;
        int stackTop = 0;
        try (var old = Files.list(Path.of(ROOT + "models/block/fuel_refinery"))) {
            for (Path file : old.toList()) {
                Files.delete(file);
            }
        }
        for (int i = 0; i < refineryCells.length; i++) {
            int[] c = refineryCells[i];
            Model cell = refinery.cell(c[0], c[1], c[2]);
            model("fuel_refinery/p" + i, cell);
            outlines.add(bounds(cell));
            String litJson = refineryLit.cell(c[0], c[1], c[2]).json();
            String plain = cell.json();
            boolean glows = !litJson.substring(litJson.indexOf("\"elements\""))
                    .equals(plain.substring(plain.indexOf("\"elements\""))) || plain.contains("\"#front\"");
            if (glows) {
                litParts.add(i);
                write("models/block/fuel_refinery/p" + i + "_lit.json", litJson);
            }
            if (c[0] == 1 && c[1] == 5 && c[2] == 2) {
                columnTop = i;
            }
            if (c[0] == 3 && c[1] == 5 && c[2] == 3) {
                stackTop = i;
            }
        }
        multiblockStates("fuel_refinery", refineryCells.length, (part, lit) ->
                "overthought_vehicles:block/fuel_refinery/p" + part + (lit && litParts.contains(part) ? "_lit" : ""));
        write("models/item/fuel_refinery.json", refinery.shrunk(24).json());
        item("fuel_refinery", "overthought_vehicles:item/fuel_refinery");
        refineryLayout(refineryCells, outlines, columnTop, stackTop);
        System.out.println("refinery: " + refineryCells.length + " cells, lit " + litParts
                + ", column top " + columnTop + ", stack top " + stackTop);

        int[][] hydroCells = {{0, 0, 0}, {0, 1, 0}};
        cells("hydrotreater", hydrotreater(false), hydroCells);
        model("hydrotreater/p0_lit", hydrotreater(true).cell(0, 0, 0));
        multiblockStates("hydrotreater", 2, (part, lit) ->
                "overthought_vehicles:block/hydrotreater/p" + part + (lit && part == 0 ? "_lit" : ""));
        write("models/item/hydrotreater.json", hydrotreater(false).shrunk(20).json());
        item("hydrotreater", "overthought_vehicles:item/hydrotreater");

        int[][] jackCells = {
                {0, 0, 0}, {0, 0, 1}, {0, 0, 2},
                {0, 1, 0}, {0, 1, 1}, {0, 1, 2},
                {0, 2, 1},
        };
        cells("pumpjack", pumpjack(), jackCells);
        // The motor's cell again, with its lamp on.
        model("pumpjack/p2_lit", pumpjack(true).cell(0, 0, 2));
        model("pumpjack/beam", beam());
        model("pumpjack/crank", crank());
        multiblockStates("pumpjack", jackCells.length + 2, (part, lit) ->
                part == jackCells.length ? "overthought_vehicles:block/pumpjack/beam"
                        : part == jackCells.length + 1 ? "overthought_vehicles:block/pumpjack/crank"
                        : "overthought_vehicles:block/pumpjack/p" + part + (lit && part == 2 ? "_lit" : ""));
        Model whole = pumpjack().add(beam().shifted(0, 32, 16)).add(crank().shifted(0, 16, 32));
        write("models/item/pumpjack.json", whole.shrunk(24).json());
        item("pumpjack", "overthought_vehicles:item/pumpjack");

        model("generator", coalGenerator(false));
        model("generator_lit", coalGenerator(true));
        model("biofuel_generator", bioGenerator(false));
        model("biofuel_generator_lit", bioGenerator(true));
        model("diesel_generator", dieselGenerator(false));
        model("diesel_generator_lit", dieselGenerator(true));
        singleStates("generator", "generator");
        singleStates("biofuel_generator", "biofuel_generator");
        singleStates("diesel_generator", "diesel_generator");
        item("generator", "overthought_vehicles:block/generator");
        item("biofuel_generator", "overthought_vehicles:block/biofuel_generator");
        item("diesel_generator", "overthought_vehicles:block/diesel_generator");

        for (int level = 0; level <= 4; level++) {
            model("battery_bank_" + level, batteryBank(level));
        }
        batteryStates();
        item("battery_bank", "overthought_vehicles:block/battery_bank_4");
        model("charging_post", chargingPost(false));
        model("charging_post_lit", chargingPost(true));
        singleStates("charging_post", "charging_post");
        item("charging_post", "overthought_vehicles:item/charging_post");
        write("models/item/charging_post.json", tallItem("overthought_vehicles:block/charging_post"));

        write("models/block/oil_deposit.json", cube("overthought_vehicles:block/oil_deposit"));
        write("blockstates/oil_deposit.json",
                "{\n  \"variants\": {\n    \"\": { \"model\": \"overthought_vehicles:block/oil_deposit\" }\n  }\n}\n");
        item("oil_deposit", "overthought_vehicles:block/oil_deposit");
        // The oil seep's models and item are tools/gen_paint_tar.py's; only its blockstate is here.
        write("blockstates/oil_seep.json",
                "{\n  \"variants\": {\n    \"\": { \"model\": \"overthought_vehicles:block/oil_seep\" }\n  }\n}\n");
        System.out.println("done");
    }
}
