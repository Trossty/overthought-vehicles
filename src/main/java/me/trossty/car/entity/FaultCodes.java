package me.trossty.car.entity;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import org.jetbrains.annotations.Nullable;

/**
 * What a code reader plugged into a car finds: one fault code for every part
 * that is missing or getting tired, and a few for the car as a whole, worst
 * first, as the OBD tablet shows them. The codes are in the shape a real reader shows (P for the engine and
 * drivetrain, C for the chassis, B for the body), so the job reads like a
 * workshop's: scan, look the code up, fix what it points at.
 */
public final class FaultCodes {

    /** At or under this a part is reported at all. */
    public static final float ADVISORY = 0.50F;
    public static final float WARNING = 0.35F;
    public static final float CRITICAL = 0.15F;

    public enum Severity {
        CRITICAL(ChatFormatting.RED),
        WARNING(ChatFormatting.GOLD),
        ADVISORY(ChatFormatting.YELLOW);

        public final ChatFormatting colour;

        Severity(ChatFormatting colour) {
            this.colour = colour;
        }

        static Severity of(float fraction) {
            return fraction <= FaultCodes.CRITICAL ? CRITICAL : fraction <= FaultCodes.WARNING ? WARNING : ADVISORY;
        }
    }

    /**
     * One stored code: the part it points at (null for whole-car faults), how
     * bad, and the part's condition in percent (-1 when that means nothing).
     */
    public record Fault(String code, @Nullable CarPart part, String key, Severity severity, int percent) {

        public Component describe() {
            Component what = this.part == null
                    ? Component.translatable(this.key)
                    : Component.translatable(this.key, Component.translatable("part.car." + this.part.key()), this.percent);
            return Component.literal(this.code + "  ").withStyle(ChatFormatting.BOLD, this.severity.colour)
                    .append(what.copy().withStyle(ChatFormatting.RESET, this.severity.colour));
        }
    }

    private FaultCodes() {
    }

    /** The code a reader shows for trouble with this part, on this kind of car. */
    public static String codeFor(CarPart part, boolean electric) {
        return switch (part) {
            case AIR_FILTER -> "P0101";
            case SPARK_PLUGS -> "P0300";
            case ENGINE -> electric ? "P0A1A" : "P0521";
            case OIL_FILTER -> "P0520";
            case RADIATOR -> "P0128";
            case BATTERY -> electric ? "P0AFA" : "P0562";
            case ALTERNATOR -> "P0620";
            case DRIVESHAFT -> "P0720";
            case TRANSMISSION -> "P0700";
            case CLUTCH -> "P0805";
            case WHEELS -> "C0750";
            case BRAKE_PADS -> "C0040";
            case BRAKE_DISCS -> "C0045";
            case SUSPENSION -> "C0690";
            case STEERING_RACK -> "C0460";
            case EXHAUST -> "P0420";
        };
    }

    /** Whether this part matters at all on this car: an electric one has no plugs, airbox or exhaust to read. */
    static boolean relevant(CarPart part, CarEntity car) {
        if (!car.hasPart(part)) {
            return false;
        }
        if (car.isElectric()) {
            return part != CarPart.SPARK_PLUGS && part != CarPart.AIR_FILTER && part != CarPart.EXHAUST
                    && part != CarPart.OIL_FILTER;
        }
        return part != CarPart.SPARK_PLUGS || car.getEngineFuel().needsSpark();
    }

    /** Everything stored, worst first. */
    public static List<Fault> read(CarEntity car) {
        List<Fault> faults = new ArrayList<>();
        boolean electric = car.isElectric();
        for (CarPart part : CarPart.values()) {
            if (!relevant(part, car)) {
                continue;
            }
            if (!car.isFitted(part)) {
                faults.add(new Fault(codeFor(part, electric), part, "obd.car.absent", Severity.CRITICAL, 0));
                continue;
            }
            float fraction = car.getConditionFraction(part);
            if (fraction <= ADVISORY) {
                faults.add(new Fault(codeFor(part, electric), part, "obd.car.worn", Severity.of(fraction),
                        Math.round(fraction * 100.0F)));
            }
        }
        if (car.isOverheating()) {
            faults.add(new Fault("P0217", null, "obd.car.overheat", Severity.CRITICAL, -1));
        }
        if (car.isLeaking()) {
            faults.add(new Fault("P0455", null, "obd.car.leak", Severity.WARNING, -1));
        }
        if (!electric && !car.getEngineFuel().burns(car.getTankFuel()) && car.hasFuel()) {
            faults.add(new Fault("P0170", null, "obd.car.wrong_fuel", Severity.CRITICAL, -1));
        }
        if (!electric && car.hasFuel() && car.getDirtyFuel() >= 0.25F) {
            faults.add(new Fault("P0172", null, "obd.car.dirty_fuel",
                    car.getDirtyFuel() >= 0.75F ? Severity.CRITICAL : Severity.WARNING, -1));
        }
        if (car.getHealthFraction() < CarEntity.DAMAGE_WARNING_FRACTION) {
            faults.add(new Fault("B1000", null, "obd.car.body", Severity.WARNING, -1));
        }
        faults.sort(Comparator.comparing(Fault::severity)
                .thenComparingInt(fault -> fault.percent() < 0 ? 0 : fault.percent()));
        return faults;
    }
}
