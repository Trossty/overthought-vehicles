package me.trossty.car;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonParseException;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import net.neoforged.fml.loading.FMLPaths;

/**
 * The few balance knobs a server owner might want to turn, in
 * {@code config/car.json}. Written out with the defaults the first time the
 * game starts; a value missing from an older file keeps its default, and a
 * file that does not parse is left alone (and logged) rather than replaced.
 *
 * <p>Every multiplier is 1.0 by default, which is the game as tested.
 */
public final class CarConfig {

    /** How fast parts wear, for every reason they wear. 0 turns wear off. */
    public double partWear = 1.0;

    /** How much fuel or charge a vehicle uses. */
    public double fuelUse = 1.0;

    /** How much power a generator makes for each unit it burns. */
    public double generatorOutput = 1.0;

    /** Energy a cable passes along per tick. */
    public int cableRate = 250;

    /**
     * One newly generated chunk in this many has an abandoned vehicle on it;
     * 0 never places one.
     */
    public int abandonedVehicleRarity = 350;

    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static CarConfig current = new CarConfig();

    public static CarConfig get() {
        return current;
    }

    /** Reads the file, or writes one with the defaults when there is none. */
    public static void load() {
        Path file = FMLPaths.CONFIGDIR.get().resolve("car.json");
        try {
            if (Files.exists(file)) {
                CarConfig read = GSON.fromJson(Files.readString(file), CarConfig.class);
                if (read != null) {
                    current = read.sane();
                }
            }
            // Written back either way, so options added since show up in the file.
            Files.createDirectories(file.getParent());
            Files.writeString(file, GSON.toJson(current));
        } catch (IOException | JsonParseException e) {
            CarMain.LOGGER.warn("Could not read {}, using the defaults: {}", file, e.getMessage());
        }
    }

    private CarConfig sane() {
        // A hand-edited file can say NaN or Infinity; neither is a rate.
        this.partWear = rate(this.partWear);
        this.fuelUse = rate(this.fuelUse);
        this.generatorOutput = rate(this.generatorOutput);
        this.cableRate = Math.max(1, this.cableRate);
        this.abandonedVehicleRarity = Math.max(0, this.abandonedVehicleRarity);
        return this;
    }

    /** A multiplier: finite and not below zero, or the default of 1. */
    private static double rate(double value) {
        return Double.isFinite(value) ? Math.max(0.0, value) : 1.0;
    }
}
