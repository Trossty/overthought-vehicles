package me.trossty.car.item;

import me.trossty.car.entity.CarEntity;
import me.trossty.car.entity.CarPart;
import me.trossty.car.entity.CarSetup;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;

import java.util.Locale;

/**
 * The compression tester: a gauge on a hose that screws into a plug hole.
 * The engine is turned over on the starter and each cylinder reads what it
 * squeezes. It shows what the tablet cannot see, the inside of the engine:
 * low all round is worn rings, two side by side is the head gasket between
 * them, next to nothing is valves bent by a snapped belt.
 *
 * <p>The readings come from the car as it is, the same every time it is
 * tested, so a second go confirms the first rather than rolling new numbers.
 */
public final class CompressionTesterItem {

    /** What a healthy petrol cylinder reads, in bar. */
    public static final float PETROL_SPEC = 12.0F;
    /** A diesel squeezes far harder: it lights its fuel by the heat of it. */
    public static final float DIESEL_SPEC = 30.0F;
    /** Below this share of spec a cylinder is low. */
    public static final float LOW = 0.75F;
    /** What a cylinder next to a blown gasket keeps. */
    private static final float GASKET = 0.3F;
    /** What a cylinder with bent valves keeps. */
    private static final float BENT = 0.1F;

    /** What the readings add up to. */
    public enum Verdict {
        GOOD("good"), WORN("worn"), GASKET("gasket"), BENT_VALVES("bent");

        private final String key;

        Verdict(String key) {
            this.key = key;
        }

        public String key() {
            return this.key;
        }
    }

    public record Reading(float spec, float[] cylinders, Verdict verdict) {
    }

    private CompressionTesterItem() {
    }

    public static float spec(CarEntity car) {
        return car.getEngineFuel() == CarSetup.Fuel.DIESEL ? DIESEL_SPEC : PETROL_SPEC;
    }

    /** What each cylinder of this car's engine reads. */
    public static Reading read(CarEntity car) {
        float spec = spec(car);
        int count = car.getLayout().cylinders();
        float[] bar = new float[count];
        long seed = car.getUUID().getLeastSignificantBits() ^ car.getUUID().getMostSignificantBits();
        float health = 0.45F + 0.55F * car.getConditionFraction(CarPart.ENGINE);
        for (int i = 0; i < count; i++) {
            // No two cylinders read quite the same: a few per cent either way.
            bar[i] = spec * health * (1.0F + 0.04F * jitter(seed, i));
        }
        Verdict verdict;
        if (car.isBeltSnapped()) {
            for (int i = 0; i < count; i++) {
                // Most valves are bent; the odd one escapes.
                if (i == 0 || jitter(seed, i + 31) < 0.5F) {
                    bar[i] = spec * BENT * (1.0F + 0.5F * jitter(seed, i + 7));
                }
            }
            verdict = Verdict.BENT_VALVES;
        } else if (car.isGasketBlown()) {
            int first = gasketCylinder(car);
            bar[first] = spec * GASKET * (1.0F + 0.1F * jitter(seed, 50));
            bar[first + 1] = spec * GASKET * (1.0F + 0.1F * jitter(seed, 51));
            verdict = Verdict.GASKET;
        } else {
            verdict = health < LOW ? Verdict.WORN : Verdict.GOOD;
        }
        return new Reading(spec, bar, verdict);
    }

    /**
     * The first of the two cylinders a blown gasket sits between, counting
     * from 0, or -1 while it holds. Always the same pair on the same car.
     */
    public static int gasketCylinder(CarEntity car) {
        if (!car.isGasketBlown()) {
            return -1;
        }
        int count = car.getLayout().cylinders();
        long seed = car.getUUID().getLeastSignificantBits() ^ car.getUUID().getMostSignificantBits();
        return (int) Math.floorMod(seed, (long) (count - 1));
    }

    /**
     * How much of its squeeze this cylinder has lost, 0 for a sound one: what
     * makes it misfire however good its plug.
     */
    public static float compressionLoss(CarEntity car, int cylinder) {
        int gasket = gasketCylinder(car);
        return gasket >= 0 && (cylinder == gasket || cylinder == gasket + 1) ? 1.0F - GASKET : 0.0F;
    }

    /** A number in -1..1 that only depends on the car and the cylinder. */
    private static float jitter(long seed, int salt) {
        long mixed = seed + salt * 0x9E3779B97F4A7C15L;
        mixed = (mixed ^ (mixed >>> 33)) * 0xFF51AFD7ED558CCDL;
        mixed ^= mixed >>> 33;
        return ((mixed & 0xFFFF) / 32767.5F) - 1.0F;
    }

    /** Held against a car: tests it if it can be tested, else says what is in the way. */
    public static InteractionResult test(CarEntity car, Player player) {
        if (car.level().isClientSide()) {
            return InteractionResult.SUCCESS;
        }
        Component problem = problem(car);
        if (problem != null) {
            player.displayClientMessage(problem, true);
            return InteractionResult.CONSUME;
        }
        Reading reading = read(car);
        car.level().playSound(null, car.getX(), car.getY() + 0.8, car.getZ(),
                SoundEvents.MINECART_RIDING, SoundSource.PLAYERS, 0.4F, 1.6F);
        player.sendSystemMessage(Component.translatable("message.car.compression_header",
                car.getVehicleName(), bar(reading.spec())).withStyle(ChatFormatting.GOLD));
        MutableComponent line = Component.empty();
        for (int i = 0; i < reading.cylinders().length; i++) {
            float value = reading.cylinders()[i];
            ChatFormatting colour = value >= reading.spec() * 0.9F ? ChatFormatting.GREEN
                    : value >= reading.spec() * LOW ? ChatFormatting.YELLOW : ChatFormatting.RED;
            if (i > 0) {
                line.append(Component.literal("  "));
            }
            line.append(Component.literal((i + 1) + ": " + bar(value)).withStyle(colour));
        }
        player.sendSystemMessage(line);
        player.sendSystemMessage(Component.translatable("message.car.compression_" + reading.verdict().key())
                .withStyle(reading.verdict() == Verdict.GOOD ? ChatFormatting.GREEN : ChatFormatting.RED));
        return InteractionResult.CONSUME;
    }

    /** Why this car cannot be tested now, or null if it can. */
    private static Component problem(CarEntity car) {
        if (!car.hasPart(CarPart.ENGINE) || car.isElectric()) {
            return Component.translatable("message.car.compression_no_engine");
        }
        if (!car.isFitted(CarPart.ENGINE)) {
            return Component.translatable("message.car.compression_no_engine");
        }
        if (!car.isWorkable(CarPart.SPARK_PLUGS)) {
            return car.cannotWork(CarPart.SPARK_PLUGS);
        }
        if (car.isEngineRunning()) {
            return Component.translatable("message.car.compression_running");
        }
        if (car.hasPart(CarPart.SPARK_PLUGS) && car.isFitted(CarPart.SPARK_PLUGS)) {
            return Component.translatable("message.car.compression_plugs_in");
        }
        if (car.isBatteryFlat()) {
            return Component.translatable("message.car.compression_flat");
        }
        return null;
    }

    private static String bar(float value) {
        return String.format(Locale.ROOT, "%.1f", value);
    }
}
