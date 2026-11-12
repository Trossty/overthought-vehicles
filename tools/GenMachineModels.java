import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Shapes the station and production blocks — pump, refinery, generator, yard
 * tank — as real 3D block models instead of plain cubes: a pump with a plinth,
 * a head and a hose; a refinery with its column; a generator on a skid with an
 * exhaust stack; a round tank on legs.
 *
 * <p>UVs are left out on purpose, so the game maps each face from the element's
 * own position on the 16x16 texture: a small box shows the matching small part
 * of the painted face, which is what keeps the pictures on the faces lined up.
 *
 * <p>Run from the project root: {@code java tools/GenMachineModels.java}
 */
public class GenMachineModels {

    static final String DIR = "src/main/resources/assets/car/models/block/";
    static final String[] ALL = {"north", "south", "west", "east", "up", "down"};

    /** One box; faces maps a face name to a texture variable, missing faces are not drawn. */
    record Box(float[] from, float[] to, Map<String, String> faces, String axis, float angle, float[] origin) {
    }

    static final class Model {
        final Map<String, String> textures = new LinkedHashMap<>();
        final List<Box> boxes = new ArrayList<>();

        Model tex(String key, String value) {
            this.textures.put(key, value);
            return this;
        }

        /** A box with the same texture on every face. */
        Model box(float x1, float y1, float z1, float x2, float y2, float z2, String texture) {
            Map<String, String> faces = new LinkedHashMap<>();
            for (String side : ALL) {
                faces.put(side, texture);
            }
            return this.box(x1, y1, z1, x2, y2, z2, faces);
        }

        /** A box with its own texture per face; pass pairs "north", "#front", ... with "*" as the default. */
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
            return this.box(x1, y1, z1, x2, y2, z2, faces);
        }

        Model box(float x1, float y1, float z1, float x2, float y2, float z2, Map<String, String> faces) {
            this.boxes.add(new Box(new float[]{x1, y1, z1}, new float[]{x2, y2, z2}, faces, null, 0, null));
            return this;
        }

        String json() {
            StringBuilder b = new StringBuilder("{\n  \"parent\": \"minecraft:block/block\",\n  \"textures\": {\n");
            int i = 0;
            for (Map.Entry<String, String> e : this.textures.entrySet()) {
                b.append("    \"").append(e.getKey()).append("\": \"").append(e.getValue()).append('"')
                        .append(++i < this.textures.size() ? ",\n" : "\n");
            }
            b.append("  },\n  \"elements\": [\n");
            for (int k = 0; k < this.boxes.size(); k++) {
                Box box = this.boxes.get(k);
                b.append("    { \"from\": ").append(vec(box.from())).append(", \"to\": ").append(vec(box.to()))
                        .append(", \"faces\": {");
                int f = 0;
                for (Map.Entry<String, String> face : box.faces().entrySet()) {
                    b.append(f++ == 0 ? " " : ", ").append('"').append(face.getKey())
                            .append("\": { \"texture\": \"").append(face.getValue()).append("\" }");
                }
                b.append(" } }").append(k < this.boxes.size() - 1 ? ",\n" : "\n");
            }
            return b.append("  ]\n}\n").toString();
        }
    }

    static String vec(float[] v) {
        return String.format(Locale.ROOT, "[%s, %s, %s]", num(v[0]), num(v[1]), num(v[2]));
    }

    static String num(float f) {
        return f == (int) f ? Integer.toString((int) f) : String.format(Locale.ROOT, "%.2f", f);
    }

    static void write(String name, Model model) throws IOException {
        Files.writeString(Path.of(DIR + name + ".json"), model.json());
        System.out.println("wrote " + name);
    }

    // Vanilla textures for the hardware, so the machines sit well beside vanilla blocks.
    static final String STONE = "minecraft:block/smooth_stone";
    static final String IRON = "minecraft:block/iron_block";
    static final String DARK = "minecraft:block/black_concrete";
    static final String GREY = "minecraft:block/gray_concrete";
    static final String COPPER = "minecraft:block/cut_copper";
    static final String RED = "minecraft:block/red_concrete";
    static final String GLASS = "minecraft:block/light_blue_stained_glass";

    // ---- the pump: two blocks tall, front to the north ----

    static Model pumpLower() {
        return new Model()
                .tex("particle", "car:block/fuel_pump_bottom_side")
                .tex("front", "car:block/fuel_pump_bottom_front")
                .tex("side", "car:block/fuel_pump_bottom_side")
                .tex("stone", STONE).tex("dark", DARK).tex("grey", GREY).tex("iron", IRON)
                // concrete island the pump is bolted to
                .box(0, 0, 1, 16, 2, 15, "#stone")
                // the cabinet
                .box(2, 2, 3, 14, 16, 13, "*", "#side", "north", "#front", "up", "-", "down", "-")
                // the nozzle in its holster on the front, and its hose looping down
                .box(9, 8, 1.5F, 13, 11, 3, "#dark")
                .box(10, 6, 1.75F, 11.5F, 8, 2.75F, "#grey")
                .box(10.25F, 9, 0.75F, 11.25F, 10, 1.5F, "#iron")
                .box(12, 3, 2, 13, 8, 3, "#dark")
                .box(4, 3, 2, 13, 4, 3, "#dark")
                .box(4, 4, 2, 5, 9, 3, "#dark")
                // kick plate
                .box(2, 2, 2.5F, 14, 3, 3, "#grey");
    }

    static Model pumpUpper() {
        return new Model()
                .tex("particle", "car:block/fuel_pump_top_side")
                .tex("front", "car:block/fuel_pump_top_front")
                .tex("side", "car:block/fuel_pump_top_side")
                .tex("top", "car:block/fuel_pump_top")
                .tex("lower", "car:block/fuel_pump_bottom_side")
                .tex("grey", GREY).tex("glass", GLASS)
                // the cabinet carries on up
                .box(2, 0, 3, 14, 7, 13, "*", "#lower", "up", "-", "down", "-")
                // the head: wider, with the display on the front
                .box(1, 7, 2, 15, 14, 14, "*", "#side", "north", "#front", "south", "#front", "up", "#top")
                // a glass visor over the display
                .box(2, 13, 1, 14, 14, 2, "#grey")
                // the price sign on top
                .box(3, 14, 7, 13, 16, 9, "*", "#top", "up", "#grey", "down", "-");
    }

    // ---- the refinery: housing, distillation column, chimney ----

    static Model refinery(boolean lit) {
        return new Model()
                .tex("particle", "car:block/fuel_refinery_side")
                .tex("front", lit ? "car:block/fuel_refinery_front_lit" : "car:block/fuel_refinery_front")
                .tex("side", "car:block/fuel_refinery_side")
                .tex("top", "car:block/fuel_refinery_top")
                .tex("stone", STONE).tex("iron", IRON).tex("copper", COPPER).tex("dark", DARK)
                .box(0, 0, 0, 16, 2, 16, "#stone")
                .box(1, 2, 1, 15, 11, 15, "*", "#side", "north", "#front", "up", "#top", "down", "-")
                // the column, banded in copper
                .box(9, 11, 8, 14, 16, 13, "*", "#iron", "down", "-")
                .box(8.5F, 12, 7.5F, 14.5F, 13, 13.5F, "#copper")
                .box(8.5F, 14.5F, 7.5F, 14.5F, 15.5F, 13.5F, "#copper")
                // the flue
                .box(2.5F, 11, 9.5F, 5, 16, 12, "*", "#dark", "down", "-")
                // a feed line across the top
                .box(5, 12, 10, 9, 13, 11, "#copper");
    }

    // ---- the generator: an engine on a skid ----

    static Model generator(boolean lit) {
        return new Model()
                .tex("particle", "car:block/generator_side")
                .tex("front", lit ? "car:block/generator_front_lit" : "car:block/generator_front")
                .tex("side", "car:block/generator_side")
                .tex("top", "car:block/generator_top")
                .tex("dark", DARK).tex("iron", IRON).tex("red", RED)
                // the skid
                .box(0, 0, 1, 16, 1, 15, "#dark")
                .box(0, 1, 1, 1, 2, 15, "#dark")
                .box(15, 1, 1, 16, 2, 15, "#dark")
                // the engine and alternator in one casing
                .box(1, 1, 2, 15, 11, 14, "*", "#side", "north", "#front", "up", "#top", "down", "-")
                // exhaust stack with a rain cap
                .box(11, 11, 10, 13, 15, 12, "*", "#iron", "down", "-")
                .box(10.5F, 15, 9.5F, 13.5F, 16, 12.5F, "#dark")
                // fuel filler
                .box(3, 11, 4, 5, 12, 6, "#red")
                // lifting eye
                .box(7, 11, 7.5F, 9, 13, 8.5F, "#iron");
    }

    // ---- the yard tank: a short round tank on four legs ----

    static Model tank(int level) {
        String side = "car:block/fuel_tank_side_" + level;
        return new Model()
                .tex("particle", side)
                .tex("side", side)
                .tex("top", "car:block/fuel_tank_top")
                .tex("bottom", "car:block/fuel_tank_bottom")
                .tex("iron", IRON).tex("dark", DARK)
                // legs
                .box(2, 0, 2, 4, 2, 4, "#dark")
                .box(12, 0, 2, 14, 2, 4, "#dark")
                .box(2, 0, 12, 4, 2, 14, "#dark")
                .box(12, 0, 12, 14, 2, 14, "#dark")
                // the shell, an octagon of two crossed boxes
                .box(3, 2, 1, 13, 15, 15, "*", "#side", "up", "#top", "down", "#bottom")
                .box(1, 2, 3, 15, 14.9F, 13, "*", "#side", "up", "#top", "down", "#bottom")
                // the manhole on top
                .box(5, 15, 5, 11, 16, 11, "*", "#iron", "down", "-");
    }

    public static void main(String[] args) throws IOException {
        // The refinery and the generators are drawn by GenPlantModels now.
        write("fuel_pump_lower", pumpLower());
        write("fuel_pump_upper", pumpUpper());
        for (int level = 0; level <= 4; level++) {
            write("fuel_tank_" + level, tank(level));
        }
    }
}
