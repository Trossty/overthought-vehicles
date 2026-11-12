import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * The road chain's assets: tar, asphalt laid in eighth-of-a-block layers like
 * snow, the road markings painted on top of it, and the tins of road paint
 * that paint them; plus models, the multipart blockstate, item definitions,
 * the loot table and recipes. Run from the project root.
 *
 * <p>Markings are drawn white and tinted by the block colour provider, so one
 * texture serves every paint. Each is drawn running north (arrows point
 * north); the blockstate turns it to the way it was painted.
 */
public class GenAsphalt {

    static final String ASSETS = "src/main/resources/assets/car/";
    static final String DATA = "src/main/resources/data/car/";

    static final String[] MARKINGS = {"dashed", "solid", "double", "edge", "corner", "stop", "crosswalk",
            "arrow", "arrow_left", "arrow_right", "parking", "hatch"};
    static final String[] PAINTS = {"white", "yellow", "red", "blue"};
    static final String[] DYES = {"white_dye", "yellow_dye", "red_dye", "blue_dye"};
    static final String[] FACINGS = {"north", "east", "south", "west"};

    static BufferedImage img;

    static void px(int x, int y, int c) {
        if (x >= 0 && y >= 0 && x < 16 && y < 16) {
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

    /** Asphalt: dark, with a fixed scatter of lighter and darker stones in it. */
    static void asphalt(Random random) {
        img = new BufferedImage(16, 16, BufferedImage.TYPE_INT_ARGB);
        int[] base = {0xFF2B2C2E, 0xFF2E2F31, 0xFF303133, 0xFF292A2C};
        for (int x = 0; x < 16; x++) {
            for (int y = 0; y < 16; y++) {
                px(x, y, base[random.nextInt(base.length)]);
            }
        }
        for (int i = 0; i < 22; i++) {
            int c = random.nextInt(3) == 0 ? 0xFF55575A : random.nextBoolean() ? 0xFF404245 : 0xFF1F2022;
            px(random.nextInt(16), random.nextInt(16), c);
        }
    }

    static final int PAINT = 0xFFFFFFFF;

    /** One marking, in white on clear, running north. */
    static void marking(String name) {
        img = new BufferedImage(16, 16, BufferedImage.TYPE_INT_ARGB);
        switch (name) {
            case "dashed" -> {
                rect(7, 0, 8, 4, PAINT);
                rect(7, 8, 8, 12, PAINT);
            }
            case "solid" -> rect(7, 0, 8, 15, PAINT);
            case "double" -> {
                rect(5, 0, 6, 15, PAINT);
                rect(9, 0, 10, 15, PAINT);
            }
            case "edge" -> rect(1, 0, 2, 15, PAINT);
            case "corner" -> {
                // A centre line coming in from the south and leaving to the east,
                // round a quarter circle about the south-east corner.
                for (int x = 0; x < 16; x++) {
                    for (int y = 0; y < 16; y++) {
                        double d = Math.hypot(16 - (x + 0.5), 16 - (y + 0.5));
                        if (d >= 7.0 && d <= 9.0) {
                            px(x, y, PAINT);
                        }
                    }
                }
            }
            case "stop" -> rect(0, 1, 15, 4, PAINT);
            case "crosswalk" -> {
                rect(1, 0, 4, 15, PAINT);
                rect(9, 0, 12, 15, PAINT);
            }
            case "arrow" -> {
                for (int r = 0; r <= 5; r++) {
                    rect(7 - r, 1 + r, 8 + r, 1 + r, PAINT);
                }
                rect(7, 7, 8, 14, PAINT);
            }
            case "arrow_left" -> {
                for (int c = 0; c <= 4; c++) {
                    rect(1 + c, 5 - c, 1 + c, 6 + c, PAINT);
                }
                rect(6, 5, 10, 6, PAINT);
                rect(9, 7, 10, 14, PAINT);
            }
            case "arrow_right" -> {
                for (int c = 0; c <= 4; c++) {
                    rect(14 - c, 5 - c, 14 - c, 6 + c, PAINT);
                }
                rect(5, 5, 9, 6, PAINT);
                rect(5, 7, 6, 14, PAINT);
            }
            case "parking" -> {
                rect(4, 2, 5, 13, PAINT);
                rect(6, 2, 9, 3, PAINT);
                rect(6, 7, 9, 8, PAINT);
                rect(10, 3, 11, 7, PAINT);
                px(10, 2, 0x00000000);
                px(10, 8, 0x00000000);
                px(11, 3, 0x00000000);
                px(11, 7, 0x00000000);
                px(10, 3, PAINT);
                px(10, 7, PAINT);
            }
            case "hatch" -> {
                for (int x = 0; x < 16; x++) {
                    for (int y = 0; y < 16; y++) {
                        if (Math.floorMod(x + y, 8) < 3) {
                            px(x, y, PAINT);
                        }
                    }
                }
            }
            default -> throw new IllegalArgumentException(name);
        }
        // Road paint wears: a duller fleck here and there, the odd chip gone.
        Random random = new Random(name.hashCode());
        for (int x = 0; x < 16; x++) {
            for (int y = 0; y < 16; y++) {
                if ((img.getRGB(x, y) >>> 24) == 0) {
                    continue;
                }
                int roll = random.nextInt(40);
                if (roll == 0) {
                    img.setRGB(x, y, 0);
                } else if (roll < 6) {
                    img.setRGB(x, y, 0xFFD6D6D2);
                } else if (roll < 9) {
                    img.setRGB(x, y, 0xFFE8E8E4);
                }
            }
        }
    }

    static int shade(int rgb, double f) {
        int r = (int) Math.min(255, ((rgb >> 16) & 0xFF) * f);
        int g = (int) Math.min(255, ((rgb >> 8) & 0xFF) * f);
        int b = (int) Math.min(255, (rgb & 0xFF) * f);
        return 0xFF000000 | r << 16 | g << 8 | b;
    }

    static void write(String path) throws Exception {
        File file = new File(ASSETS + "textures/" + path + ".png");
        file.getParentFile().mkdirs();
        ImageIO.write(img, "png", file);
    }

    static void json(String path, String text) throws Exception {
        Path file = Path.of(path);
        Files.createDirectories(file.getParent());
        Files.writeString(file, text);
    }

    static void delete(String path) throws Exception {
        Files.deleteIfExists(Path.of(path));
    }


    /** Asphalt this many sixteenths high, with its sides cut to fit. */
    static String layerModel(int h) {
        return """
                {
                  "textures": {
                    "particle": "car:block/asphalt",
                    "texture": "car:block/asphalt"
                  },
                  "elements": [
                    {
                      "from": [0, 0, 0],
                      "to": [16, %1$d, 16],
                      "faces": {
                        "down":  {"uv": [0, 0, 16, 16], "texture": "#texture", "cullface": "down"},
                        "up":    {"uv": [0, 0, 16, 16], "texture": "#texture"},
                        "north": {"uv": [0, %2$d, 16, 16], "texture": "#texture", "cullface": "north"},
                        "south": {"uv": [0, %2$d, 16, 16], "texture": "#texture", "cullface": "south"},
                        "west":  {"uv": [0, %2$d, 16, 16], "texture": "#texture", "cullface": "west"},
                        "east":  {"uv": [0, %2$d, 16, 16], "texture": "#texture", "cullface": "east"}
                      }
                    }
                  ]
                }
                """.formatted(h, 16 - h);
    }

    /** A marking lying just above a road's top at this height, tinted with the paint. */
    static String markingModel(String name, int h) {
        String y = String.format(java.util.Locale.ROOT, "%.2f", h + 0.05);
        String cull = h == 16 ? ", \"cullface\": \"up\"" : "";
        return """
                {
                  "ambientocclusion": false,
                  "textures": {
                    "particle": "car:block/marking/%1$s",
                    "marking": "car:block/marking/%1$s"
                  },
                  "elements": [
                    {
                      "from": [0, %2$s, 0],
                      "to": [16, %2$s, 16],
                      "shade": false,
                      "faces": {
                        "up": {"uv": [0, 0, 16, 16], "texture": "#marking", "tintindex": 0%3$s}
                      }
                    }
                  ]
                }
                """.formatted(name, y, cull);
    }

    public static void main(String[] args) throws Exception {
        // ---- textures ----
        asphalt(new Random(7));
        write("block/asphalt");
        for (String name : MARKINGS) {
            marking(name);
            write("block/marking/" + name);
        }
        // The paint tins and the tar, their textures, models and item
        // definitions, and the asphalt item, are tools/gen_paint_tar.py's.

        // ---- models ----
        String models = ASSETS + "models/";
        json(models + "block/asphalt.json", """
                {
                  "parent": "minecraft:block/cube_all",
                  "textures": {"all": "car:block/asphalt"}
                }
                """);
        for (int layers = 1; layers < 8; layers++) {
            json(models + "block/asphalt_height" + layers * 2 + ".json", layerModel(layers * 2));
        }
        for (String name : MARKINGS) {
            for (int layers = 1; layers <= 8; layers++) {
                json(models + "block/marking/" + name + "_" + layers + ".json", markingModel(name, layers * 2));
            }
        }
        // ---- blockstate: the layer, then the marking over it ----
        List<String> parts = new ArrayList<>();
        for (int layers = 1; layers <= 8; layers++) {
            String model = layers == 8 ? "car:block/asphalt" : "car:block/asphalt_height" + layers * 2;
            parts.add("    {\"when\": {\"layers\": \"%d\"}, \"apply\": {\"model\": \"%s\"}}".formatted(layers, model));
        }
        for (String name : MARKINGS) {
            for (int layers = 1; layers <= 8; layers++) {
                for (int f = 0; f < FACINGS.length; f++) {
                    String rot = f == 0 ? "" : ", \"y\": " + f * 90;
                    parts.add(("    {\"when\": {\"marking\": \"%s\", \"layers\": \"%d\", \"facing\": \"%s\"},"
                            + " \"apply\": {\"model\": \"car:block/marking/%s_%d\"%s}}")
                            .formatted(name, layers, FACINGS[f], name, layers, rot));
                }
            }
        }
        json(ASSETS + "blockstates/asphalt.json", "{\n  \"multipart\": [\n" + String.join(",\n", parts) + "\n  ]\n}\n");

        // ---- item definitions ----

        // ---- loot: a layer back for every layer laid ----
        List<String> counts = new ArrayList<>();
        for (int layers = 2; layers <= 8; layers++) {
            counts.add("""
                                {
                                  "function": "minecraft:set_count",
                                  "add": false,
                                  "count": %d.0,
                                  "conditions": [
                                    {
                                      "condition": "minecraft:block_state_property",
                                      "block": "car:asphalt",
                                      "properties": {"layers": "%d"}
                                    }
                                  ]
                                }""".formatted(layers, layers));
        }
        json(DATA + "loot_table/blocks/asphalt.json", """
                {
                  "type": "minecraft:block",
                  "pools": [
                    {
                      "bonus_rolls": 0.0,
                      "conditions": [{"condition": "minecraft:survives_explosion"}],
                      "entries": [
                        {
                          "type": "minecraft:item",
                          "name": "car:asphalt",
                          "functions": [
                %s
                          ]
                        }
                      ],
                      "rolls": 1.0
                    }
                  ],
                  "random_sequence": "car:blocks/asphalt"
                }
                """.formatted(String.join(",\n", counts)));

        // ---- recipes ----
        // Eight sixteenths of road go into a block: a batch lays two whole blocks.
        json(DATA + "recipe/asphalt.json", """
                {
                  "type": "minecraft:crafting_shaped",
                  "category": "building",
                  "key": {
                    "G": "minecraft:gravel",
                    "S": "#minecraft:sand",
                    "T": "car:tar"
                  },
                  "pattern": [
                    "GSG",
                    "STS",
                    "GSG"
                  ],
                  "result": {
                    "count": 16,
                    "id": "car:asphalt"
                  }
                }
                """);
        for (int i = 0; i < PAINTS.length; i++) {
            json(DATA + "recipe/" + PAINTS[i] + "_road_paint.json", """
                    {
                      "type": "minecraft:crafting_shapeless",
                      "category": "tools",
                      "ingredients": [
                        "minecraft:iron_ingot",
                        "minecraft:%1$s",
                        "minecraft:%1$s"
                      ],
                      "result": {
                        "count": 1,
                        "id": "car:%2$s_road_paint"
                      }
                    }
                    """.formatted(DYES[i], PAINTS[i]));
        }

        // ---- what the old line block and slab left behind ----
        for (String gone : new String[]{"asphalt_line", "asphalt_slab", "asphalt_slab_top"}) {
            delete(ASSETS + "models/block/" + gone + ".json");
            delete(ASSETS + "blockstates/" + gone + ".json");
            delete(ASSETS + "items/" + gone + ".json");
            delete(DATA + "loot_table/blocks/" + gone + ".json");
            delete(DATA + "recipe/" + gone + ".json");
        }
        delete(ASSETS + "textures/block/asphalt_line.png");
        delete(DATA + "recipe/asphalt_slab_from_stonecutting.json");

        System.out.println("wrote asphalt layers, " + MARKINGS.length + " markings, "
                + PAINTS.length + " road paints and tar (" + parts.size() + " blockstate parts)");
    }

}
