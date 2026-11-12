import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

/**
 * Finds translation keys that are missing somewhere, before a player sees a
 * raw key on screen:
 *
 * <ul>
 *   <li>keys one language file has and another lacks;</li>
 *   <li>literal keys in the code ({@code Text.translatable("...")}) that no
 *       language file has;</li>
 *   <li>blocks and items registered by name with no {@code block.car.} or
 *       {@code item.car.} entry;</li>
 *   <li>placeholders that differ between languages ({@code %s} counts).</li>
 * </ul>
 *
 * Keys built at run time ({@code "fuel.car." + key}) cannot be seen here; the
 * known families of them are checked against their enums' keys by hand below.
 * Run from the project root; exits 1 when anything is missing.
 */
public class LangCheck {

    static final Path LANG = Path.of("src/main/resources/assets/car/lang");
    static final Pattern ENTRY = Pattern.compile("^\\s*\"([^\"]+)\"\\s*:\\s*\"((?:[^\"\\\\]|\\\\.)*)\"", Pattern.MULTILINE);
    static final Pattern TRANSLATABLE = Pattern.compile("translatable\\(\\s*\"([a-z0-9_.]+)\"");
    static final Pattern BLOCK = Pattern.compile("Block [A-Z_]+ = register\\(\\s*\"([a-z0-9_]+)\"");
    static final Pattern ITEM = Pattern.compile("Item [A-Z_]+ =\\s*(?:register|part)\\(\\s*\"([a-z0-9_]+)\"");
    static final Pattern PLACEHOLDER = Pattern.compile("%(?:\\d+\\$)?s");

    public static void main(String[] args) throws IOException {
        Map<String, Map<String, String>> languages = new TreeMap<>();
        try (Stream<Path> files = Files.list(LANG)) {
            for (Path file : files.filter(p -> p.toString().endsWith(".json")).toList()) {
                Map<String, String> entries = new TreeMap<>();
                Matcher m = ENTRY.matcher(Files.readString(file));
                while (m.find()) {
                    entries.put(m.group(1), m.group(2));
                }
                languages.put(file.getFileName().toString().replace(".json", ""), entries);
            }
        }
        Set<String> all = new TreeSet<>();
        languages.values().forEach(entries -> all.addAll(entries.keySet()));
        List<String> problems = new ArrayList<>();

        for (var language : languages.entrySet()) {
            for (String key : all) {
                if (!language.getValue().containsKey(key)) {
                    problems.add(language.getKey() + " lacks " + key);
                }
            }
        }
        Map<String, String> english = languages.get("en_us");
        for (var language : languages.entrySet()) {
            for (var entry : language.getValue().entrySet()) {
                String other = english.get(entry.getKey());
                if (other != null && count(other) != count(entry.getValue())) {
                    problems.add(language.getKey() + " has " + count(entry.getValue()) + " placeholders in "
                            + entry.getKey() + ", en_us has " + count(other));
                }
            }
        }

        Set<String> used = new TreeSet<>();
        List<Path> sources;
        try (Stream<Path> walk = Files.walk(Path.of("src"))) {
            sources = walk.filter(p -> p.toString().endsWith(".java")).toList();
        }
        for (Path source : sources) {
            String code = Files.readString(source);
            collect(TRANSLATABLE, code, used, "");
            if (source.endsWith("ModBlocks.java")) {
                collect(BLOCK, code, used, "block.car.");
            }
            if (source.endsWith("ModItems.java")) {
                collect(ITEM, code, used, "item.car.");
            }
        }
        // A literal that ends in a dot or underscore is the front of a key built
        // at run time ("fuel.car." + fuel.key()), not a key.
        used.removeIf(key -> key.endsWith(".") || key.endsWith("_"));
        for (String key : used) {
            if (!english.containsKey(key) && !key.startsWith("container.") && key.contains(".car.")
                    || key.startsWith("block.car.") && !english.containsKey(key)
                    || key.startsWith("item.car.") && !english.containsKey(key)) {
                problems.add("no language has " + key);
            }
        }

        // The families the code builds from an enum's key.
        for (String fuel : List.of("petrol", "diesel", "biofuel", "electric")) {
            need(english, problems, "fuel.car." + fuel, "fuel.car." + fuel + ".short");
        }
        for (String engine : List.of("inline4", "inline6", "v6", "v8", "electric")) {
            need(english, problems, "engine.car." + engine);
        }

        problems.forEach(System.out::println);
        System.out.println(problems.isEmpty()
                ? "lang ok: " + all.size() + " keys in " + languages.keySet()
                : problems.size() + " problems");
        System.exit(problems.isEmpty() ? 0 : 1);
    }

    static int count(String text) {
        Matcher m = PLACEHOLDER.matcher(text.replace("%%", ""));
        int n = 0;
        while (m.find()) {
            n++;
        }
        return n;
    }

    static void collect(Pattern pattern, String code, Set<String> into, String prefix) {
        Matcher m = pattern.matcher(code);
        while (m.find()) {
            into.add(prefix + m.group(1));
        }
    }

    static void need(Map<String, String> english, List<String> problems, String... keys) {
        for (String key : keys) {
            if (!english.containsKey(key)) {
                problems.add("no language has " + key);
            }
        }
    }
}
