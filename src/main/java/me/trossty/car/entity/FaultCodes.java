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
     * The percent only orders the list: a reader knows a sensor is out of
     * range, not how many millimetres of pad are left, so it never prints it.
     *
     * <p>{@code suspects} is what could be behind a code that more than one
     * part can set — a misfire is a plug or the cylinder it sits in — and is
     * empty when the code names its part outright. {@code at} is the wheel or
     * cylinder it was read at, or null.
     */
    public record Fault(String code, @Nullable CarPart part, String key, Severity severity, int percent,
                       List<CarPart> suspects, @Nullable Component at) {

        public Fault(String code, @Nullable CarPart part, String key, Severity severity, int percent) {
            this(code, part, key, severity, percent, List.of(), null);
        }

        /**
         * The code and what it means. A part's name is the first thing handed
         * to the line and the place the second, so a line can use either.
         */
        public Component describe() {
            Component what;
            if (this.part == null) {
                what = Component.translatable(this.key);
            } else {
                Component name = Component.translatable("part.car." + this.part.key());
                what = this.at == null ? Component.translatable(this.key, name)
                        : Component.translatable(this.key, name, this.at);
            }
            return Component.literal(this.code + "  ").withStyle(ChatFormatting.BOLD, this.severity.colour)
                    .append(what.copy().withStyle(ChatFormatting.RESET, this.severity.colour));
        }

        /**
         * Where on the car to look for it, or null when it is nowhere in
         * particular: for a code with more than one cause, what those are.
         */
        public @Nullable Component where() {
            if (this.suspects.size() > 1) {
                net.minecraft.network.chat.MutableComponent names = Component.empty();
                for (int i = 0; i < this.suspects.size(); i++) {
                    if (i > 0) {
                        names.append(", ");
                    }
                    names.append(Component.translatable("part.car." + this.suspects.get(i).key()));
                }
                return Component.translatable("obd.car.suspects", names);
            }
            CarPart at = this.place();
            return at == null ? null : Component.translatable("obd.car.where." + at.group().key());
        }

        /** Every part worth a look for this code: its suspects, else the one place it points at. */
        public List<CarPart> lookAt() {
            if (!this.suspects.isEmpty()) {
                return this.suspects;
            }
            CarPart at = this.place();
            return at == null ? List.of() : List.of(at);
        }

        /**
         * The part to go and look at for this fault: the one it is about, or
         * for a fluid or a setting the part it lives next to. Null for what has
         * no one place on the car, like the fuel in the tank.
         */
        public @Nullable CarPart place() {
            if (this.part != null) {
                return this.part;
            }
            return switch (this.key) {
                case "obd.car.oil_low", "obd.car.oil_old", "obd.car.oil_leak" -> CarPart.OIL_FILTER;
                case "obd.car.overheat", "obd.car.coolant_low" -> CarPart.RADIATOR;
                case "obd.car.brake_fluid_low" -> CarPart.BRAKE_PADS;
                case "obd.car.alignment" -> CarPart.STEERING_RACK;
                case "obd.car.belt_snapped", "obd.car.belt_due" -> CarPart.ENGINE;
                case "obd.car.cat_clogged" -> CarPart.EXHAUST;
                case "obd.car.tyres_soft" -> CarPart.WHEELS;
                default -> null;
            };
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

    /**
     * Codes one look at a single part cannot settle: a gearbox that slips is
     * read the same whether the box or the clutch behind it is going.
     */
    private static final List<CarPart> SLIP = List.of(CarPart.TRANSMISSION, CarPart.CLUTCH);
    private static final List<CarPart> JUDDER = List.of(CarPart.BRAKE_DISCS, CarPart.BRAKE_PADS);
    private static final List<CarPart> MISFIRE = List.of(CarPart.SPARK_PLUGS, CarPart.ENGINE);

    /** A cylinder misfiring at least this badly sets its own code. */
    private static final float MISFIRE_CODE = 1.0F - ADVISORY;
    /** This many cylinders at once read as one random misfire, as a real reader gives up and says. */
    private static final int MANY_MISFIRES = 3;

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
            if (part == CarPart.SPARK_PLUGS || part == CarPart.CLUTCH && fraction(car, CarPart.TRANSMISSION) <= 1.0F) {
                // Read by what they do rather than what they are: plugs as
                // misfires below, a clutch through the gearbox it drives.
                continue;
            }
            float fraction = part == CarPart.TRANSMISSION
                    ? Math.min(fraction(car, CarPart.TRANSMISSION), fraction(car, CarPart.CLUTCH))
                    : car.worstFraction(part);
            if (fraction > ADVISORY) {
                continue;
            }
            int percent = Math.round(fraction * 100.0F);
            int worst = car.worstPlace(part);
            Component at = worst < 0 ? null : car.placeName(part, worst);
            switch (part) {
                case TRANSMISSION -> faults.add(new Fault("P0730", part, "obd.car.gear_ratio",
                        Severity.of(fraction), percent, SLIP, null));
                case BRAKE_DISCS -> faults.add(new Fault(codeFor(part, electric), part, "obd.car.brake_judder",
                        Severity.of(fraction), percent, JUDDER, at));
                default -> faults.add(new Fault(codeFor(part, electric), part,
                        at == null ? "obd.car.worn" : "obd.car.worn_at", Severity.of(fraction), percent, List.of(), at));
            }
        }
        readMisfires(car, faults);
        if (car.isOverheating()) {
            faults.add(new Fault("P0217", null, "obd.car.overheat", Severity.CRITICAL, -1));
        }
        if (car.isLeaking()) {
            faults.add(new Fault("P0455", null, "obd.car.leak", Severity.WARNING, -1));
        }
        if (car.hasOil() && car.getOilLevel() < CarEntity.FLUID_LOW) {
            faults.add(new Fault("P0524", null, "obd.car.oil_low", Severity.CRITICAL, -1));
        } else if (car.hasOil() && car.getOilAge() >= 0.7F) {
            faults.add(new Fault("P0196", null, "obd.car.oil_old", Severity.WARNING, -1));
        }
        if (car.isOilLeaking()) {
            faults.add(new Fault("P0520", null, "obd.car.oil_leak", Severity.WARNING, -1));
        }
        if (car.hasPart(CarPart.RADIATOR) && car.getCoolantLevel() < CarEntity.FLUID_LOW) {
            faults.add(new Fault("P2560", null, "obd.car.coolant_low", Severity.WARNING, -1));
        }
        if (car.hasPart(CarPart.BRAKE_PADS) && car.getBrakeFluid() < CarEntity.FLUID_LOW) {
            faults.add(new Fault("C1513", null, "obd.car.brake_fluid_low", Severity.CRITICAL, -1));
        }
        if (car.isBeltSnapped()) {
            faults.add(new Fault("P0016", null, "obd.car.belt_snapped", Severity.CRITICAL, -1));
        } else if (car.hasOil() && car.getBeltWear() >= CarEntity.BELT_DUE) {
            faults.add(new Fault("P0016", null, "obd.car.belt_due", Severity.WARNING, -1));
        }
        if (car.hasCat() && car.getCatClog() >= CarEntity.CAT_CHOKED) {
            faults.add(new Fault("P0421", null, "obd.car.cat_clogged",
                    car.getCatClog() >= 0.8F ? Severity.CRITICAL : Severity.WARNING, -1));
        }
        if (car.hasPart(CarPart.WHEELS) && car.isFitted(CarPart.WHEELS) && car.tyreSoftness() > 0.0F) {
            faults.add(new Fault("C0755", null, "obd.car.tyres_soft",
                    car.tyreSoftness() >= 0.5F ? Severity.CRITICAL : Severity.WARNING, -1));
        }
        if (Math.abs(car.getAlignment()) >= 0.15F) {
            faults.add(new Fault("C0455", null, "obd.car.alignment", Severity.ADVISORY, -1));
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

    /**
     * A part's condition, 0 to 1, for reading it alongside another: 2 when
     * there is none on the car to read, so it never decides anything (a part
     * that is missing has a code of its own).
     */
    private static float fraction(CarEntity car, CarPart part) {
        return relevant(part, car) && car.isFitted(part) ? car.getConditionFraction(part) : 2.0F;
    }

    /**
     * What the engine's misfire counters show: the cylinder that keeps
     * missing, which a plug or a leaking cylinder can be behind alike, or
     * when too many miss at once, only that the engine runs rough.
     */
    private static void readMisfires(CarEntity car, List<Fault> faults) {
        if (!relevant(CarPart.SPARK_PLUGS, car) || !car.isFitted(CarPart.SPARK_PLUGS)) {
            return;
        }
        List<Integer> missing = new ArrayList<>();
        float worst = 0.0F;
        for (int cylinder = 0; cylinder < car.placesOf(CarPart.SPARK_PLUGS); cylinder++) {
            float misfire = car.misfireIn(cylinder);
            if (misfire >= MISFIRE_CODE) {
                missing.add(cylinder);
                worst = Math.max(worst, misfire);
            }
        }
        if (missing.size() >= MANY_MISFIRES) {
            faults.add(new Fault("P0300", CarPart.SPARK_PLUGS, "obd.car.misfire_many",
                    Severity.of(1.0F - worst), Math.round((1.0F - worst) * 100.0F), MISFIRE, null));
            return;
        }
        for (int cylinder : missing) {
            float misfire = car.misfireIn(cylinder);
            faults.add(new Fault("P030" + (cylinder + 1), CarPart.SPARK_PLUGS, "obd.car.misfire",
                    Severity.of(1.0F - misfire), Math.round((1.0F - misfire) * 100.0F), MISFIRE,
                    car.placeName(CarPart.SPARK_PLUGS, cylinder)));
        }
    }
}
