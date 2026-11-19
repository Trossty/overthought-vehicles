package me.trossty.car.entity;

import me.trossty.car.sound.ModSounds;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import me.trossty.car.util.ValueInput;
import me.trossty.car.util.ValueOutput;
import net.minecraft.world.phys.Vec3;
import java.util.List;

/**
 * The parts of one car as they stand: how worn each is, and for the parts
 * that sit in more than one place (see {@link PartPositions}) each place of
 * it; what each was built to; how fast they wear as the miles go on; and what
 * a tired one does to the car, a pull to one side or a misfire.
 *
 * <p>The car owns the synced numbers; this is the logic over them, so that
 * {@link CarEntity} itself is about driving and being a vehicle.
 */
public final class CarParts {

    private final CarEntity car;

    /** What is inside the engine, which its condition is the average of. */
    private final EngineInternals engine;

    CarParts(CarEntity car) {
        this.car = car;
        this.engine = new EngineInternals(car);
    }

    public EngineInternals engine() {
        return this.engine;
    }

    /** Chance per tick of a stumble, with plugs at nothing. */
    private static final float MISFIRE_CHANCE = 0.05F;

    /** How much faster the engine and gearbox wear on dirty oil. */
    private static final float WORN_OIL_WEAR = 2.2F;

    private static final float MISSING_OIL_WEAR = 3.2F;

    /** Battery condition lost per tick with no charge coming in at all. */
    private static final float BATTERY_DRAIN = 0.0040F;

    /** What turning the engine over on the starter takes out of the battery, out of 100. */
    public static final float STARTER_DRAW = 1.5F;

    /** What a sound alternator puts back of that a tick, once the engine runs. */
    private static final float STARTER_RECHARGE = 0.01F;

    /** How much faster worn pads chew through the discs behind them. */
    private static final float PADS_EAT_DISCS = 4.0F;

    /** Brake wear per unit of speed scrubbed off. */
    private static final float BRAKE_WEAR_PER_DECEL = 0.9F;

    /** At a tank of pure raw fuel the engine wears this many times again as fast... */
    public static final float DIRTY_ENGINE_WEAR = 2.0F;

    /** How much faster an engine boiling over cooks its head gasket. */
    private static final float OVERHEAT_GASKET_WEAR = 12.0F;

    /** ...and the plugs foul faster still. */
    public static final float DIRTY_PLUG_WEAR = 3.0F;

    /**
     * The mileage at which a car counts as high-mileage: by then every part
     * wears half as fast again as it did new, the way an old car eats
     * consumables however well it is kept.
     */
    public static final float HIGH_MILEAGE = 50_000.0F;

    /** How much faster than new everything wears at {@link #HIGH_MILEAGE}. */
    public static final float MILEAGE_WEAR = 0.5F;

    /** How much faster than fresh a part wears when it is nearly finished. */
    public static final float TIRED_WEAR = 0.5F;

    /** Last tick's tracked speed, so the server can see braking happen. */
    private float lastTrackedSpeed;

    private int brakeNoiseCooldown;

    /** Speed lost in a tick that only the brakes take off. */
    private static final float BRAKING_SCRUB = 0.02F;

    private static final int BRAKE_NOISE_COOLDOWN = 25;

    /**
     * Degrees a tick the car is dragged towards the side whose brakes bite
     * harder, with one side's pads and discs gone and the other's like new.
     */
    private static final float BRAKE_PULL = 1.2F;

    // How a set's wear is shared out among its places; see wearShares.

    /** How far apart two places of the same set wear, either way, from the car's own make. */
    private static final float PLACE_SPREAD = 0.12F;

    /** The front brakes' share of the stopping, and the back's: about seventy and thirty. */
    private static final float FRONT_BRAKE_SHARE = 1.4F;

    private static final float REAR_BRAKE_SHARE = 0.6F;

    /** The steered tyres scrub through every turn. */
    private static final float FRONT_TYRE_SHARE = 1.25F;

    /** Extra front tyre wear with the wheels as far out of line as they go. */
    private static final float MISALIGNED_TYRE_WEAR = 3.0F;

    /** How much faster a tyre goes driven on flat. */
    private static final float FLAT_TYRE_WEAR = 8.0F;

    /** How much faster the plugs either side of a blown gasket foul. */
    private static final float GASKET_PLUG_WEAR = 3.0F;

    /** How much faster dirty fuel clogs the tank pump's strainer... */
    private static final float DIRTY_PUMP_WEAR = 1.5F;

    /** ...and gums up the injectors' nozzles. */
    private static final float DIRTY_INJECTOR_WEAR = 3.0F;

    /** A seized caliper's pad, dragged along at full speed, against what the same pad wears braking. */
    private static final float DRAG_PAD_WEAR = 6.0F;

    /** How much of a full-throttle thirst a tank pump at its last still feeds. */
    private static final float WORN_PUMP_FEED = 0.35F;

    /** Degrees a tick a car with its ball joints gone wanders off line, at the top of each swing. */
    private static final float WANDER = 0.6F;

    /**
     * Condition of one part, {@link CarPart#MAX_CONDITION} down to 0.
     *
     * <p>Zero is not "worn out", it is "not on the car". Wear bottoms out at
     * {@link CarPart#WORN_OUT}, so the two are never confused: a finished
     * gearbox is still a gearbox, and the car still has one.
     */
    public float getCondition(CarPart part) {
        return this.car.getEntityData().get(CarEntity.PART_CONDITION.get(part.ordinal()));
    }

    /**
     * Sets the part, and for a part with places every place of it: a set put
     * on is a set of the same.
     */
    public void setCondition(CarPart part, float condition) {
        float clamped = Mth.clamp(condition, 0.0F, CarPart.MAX_CONDITION);
        this.car.getEntityData().set(CarEntity.PART_CONDITION.get(part.ordinal()), clamped);
        if (part == CarPart.ENGINE) {
            this.engine.fill(clamped);
        }
        int slot = PartPositions.slot(part);
        if (slot >= 0) {
            java.util.Arrays.fill(this.places[slot], clamped);
            this.car.getEntityData().set(CarEntity.PART_PLACES.get(slot), PartPositions.pack(this.places[slot], PartPositions.MAX));
        }
    }

    /**
     * Each place of the parts that have them, to the decimal, on the server.
     * A client reads them from {@link CarEntity#PART_PLACES} instead.
     */
    private final float[][] places = newPlaces();

    private static float[][] newPlaces() {
        float[][] all = new float[PartPositions.PARTS][];
        for (int slot = 0; slot < PartPositions.PARTS; slot++) {
            all[slot] = PartPositions.filled(CarPart.MAX_CONDITION);
        }
        return all;
    }

    /** How many places this part has here: one per wheel, one per cylinder, or just the one. */
    public int placesOf(CarPart part) {
        if (!PartPositions.isSpread(part)) {
            return 1;
        }
        return Math.min(PartPositions.MAX,
                PartPositions.inCylinders(part) ? this.car.getLayout().cylinders() : this.car.tyreCount());
    }

    /** How many of this part a job on it takes: one for every cylinder, else the usual set. */
    public int countOf(CarPart part) {
        return PartPositions.inCylinders(part) ? this.placesOf(part) : part.count();
    }

    /** Whether this place is part of the set now: a wheel with its tyre off has no tyre there. */
    private boolean counts(CarPart part, int place) {
        return part != CarPart.WHEELS || this.car.isTyreOn(place);
    }

    /** One place of a part: the tyre at this wheel, the plug in this cylinder. */
    public float getCondition(CarPart part, int place) {
        int slot = PartPositions.slot(part);
        if (slot < 0) {
            return this.getCondition(part);
        }
        return this.car.level().isClientSide()
                ? PartPositions.unpack(this.car.getEntityData().get(CarEntity.PART_PLACES.get(slot)), place)
                : this.places[slot][place];
    }

    public void setCondition(CarPart part, int place, float condition) {
        int slot = PartPositions.slot(part);
        if (slot < 0) {
            this.setCondition(part, condition);
            return;
        }
        this.places[slot][place] = Mth.clamp(condition, 0.0F, CarPart.MAX_CONDITION);
        this.settlePlaces(part);
    }

    /**
     * After a place changed: the set's average and the packed copy follow.
     * A set that is not on the car stays off, whatever its places say.
     */
    void settlePlaces(CarPart part) {
        int slot = PartPositions.slot(part);
        float sum = 0.0F;
        int counted = 0;
        for (int place = 0; place < this.placesOf(part); place++) {
            if (this.counts(part, place)) {
                sum += this.places[slot][place];
                counted++;
            }
        }
        if (counted > 0 && this.isFitted(part)) {
            this.car.getEntityData().set(CarEntity.PART_CONDITION.get(part.ordinal()), Math.max(CarPart.WORN_OUT, sum / counted));
        }
        this.car.getEntityData().set(CarEntity.PART_PLACES.get(slot), PartPositions.pack(this.places[slot], PartPositions.MAX));
    }

    /** The place of this part in the worst state, or -1 if it has none or is not on. */
    public int worstPlace(CarPart part) {
        if (!PartPositions.isSpread(part) || !this.isFitted(part)) {
            return -1;
        }
        int worst = -1;
        for (int place = 0; place < this.placesOf(part); place++) {
            if (this.counts(part, place)
                    && (worst < 0 || this.getCondition(part, place) < this.getCondition(part, worst))) {
                worst = place;
            }
        }
        return worst;
    }

    /** The worst place of a part, 0 to 1: the set's own condition for a part that has only the one. */
    public float worstFraction(CarPart part) {
        int worst = this.worstPlace(part);
        return worst < 0 ? this.getConditionFraction(part)
                : this.getCondition(part, worst) / CarPart.MAX_CONDITION;
    }

    /** One place's share of the job, 1 fresh and {@code worn} at its last; {@code missing} when the set is off. */
    private float placeFactor(CarPart part, int place, float worn, float missing) {
        if (!this.isFitted(part)) {
            return missing;
        }
        return Mth.lerp(this.getCondition(part, place) / CarPart.MAX_CONDITION, worn, 1.0F);
    }

    /**
     * Where a wheel sits on the car, in blocks, before the car is turned:
     * +z to the front, -x to the right.
     */
    Vec3 wheelLocal(int wheel) {
        me.trossty.car.vehicle.VehicleDefinition defined = this.car.getDefinition();
        if (defined != null && wheel < defined.wheels().size()) {
            org.joml.Vector3f at = defined.wheels().get(wheel).at();
            return new Vec3(at.x() / 16.0, at.y() / 16.0, at.z() / 16.0);
        }
        java.util.List<CarPart.Anchor> anchors = this.car.anchorsOf(CarPart.WHEELS);
        CarPart.Anchor anchor = anchors.get(Math.min(wheel, anchors.size() - 1));
        return new Vec3(-anchor.x(), anchor.y(), -anchor.z());
    }

    /** Which side of the car a wheel is on: 1 right, -1 left, 0 on the middle line, like a motorcycle's. */
    public int lateral(int wheel) {
        double x = this.wheelLocal(wheel).x;
        return Math.abs(x) < 0.1 ? 0 : x < 0.0 ? 1 : -1;
    }

    /** Which end a wheel is at: 1 the front, -1 the back, 0 a middle axle. */
    public int axle(int wheel) {
        int wheels = this.car.tyreCount();
        double front = -Double.MAX_VALUE;
        double back = Double.MAX_VALUE;
        for (int each = 0; each < wheels; each++) {
            double z = this.wheelLocal(each).z;
            front = Math.max(front, z);
            back = Math.min(back, z);
        }
        double z = this.wheelLocal(wheel).z;
        if (front - back < 0.1) {
            return 0;
        }
        return z > front - 0.1 ? 1 : z < back + 0.1 ? -1 : 0;
    }

    /** What to call a place on the reader and the service plate: "front left", "cylinder 3". */
    public Component placeName(CarPart part, int place) {
        return PartPositions.inCylinders(part)
                ? Component.translatable("place.overthought_vehicles.cylinder", place + 1)
                : Component.translatable("place.overthought_vehicles." + this.cornerKey(place));
    }

    /** The same, as short as a service plate needs it: "FL", "3". */
    public Component placeShort(CarPart part, int place) {
        return PartPositions.inCylinders(part)
                ? Component.literal(String.valueOf(place + 1))
                : Component.translatable("place.overthought_vehicles.short." + this.cornerKey(place));
    }

    /** "front_left", "rear", "middle_right": the end of the car a wheel is at, then its side. */
    private String cornerKey(int wheel) {
        String end = switch (this.axle(wheel)) {
            case 1 -> "front";
            case -1 -> "rear";
            default -> "middle";
        };
        return switch (this.lateral(wheel)) {
            case 1 -> end + "_right";
            case -1 -> end + "_left";
            default -> end;
        };
    }

    /** Whether the part is on the car at all. */
    public boolean isFitted(CarPart part) {
        return this.getCondition(part) > 0.0F;
    }

    /** 0 for a dead or absent part, 1 for a fresh one. */
    public float getConditionFraction(CarPart part) {
        return this.getCondition(part) / CarPart.MAX_CONDITION;
    }

    /** What the part in this slot was built to. */
    public CarPart.Quality getQuality(CarPart part) {
        long bits = this.car.getEntityData().get(CarEntity.PART_QUALITY);
        return CarPart.Quality.byOrdinal((int) ((bits >>> (part.ordinal() * CarEntity.QUALITY_BITS)) & CarEntity.QUALITY_MASK));
    }

    public void setQuality(CarPart part, CarPart.Quality quality) {
        this.car.getEntityData().set(CarEntity.PART_QUALITY,
                withQuality(this.car.getEntityData().get(CarEntity.PART_QUALITY), part, quality));
    }

    /**
     * The same packing, without a car. The lift needs it: it stamps the grades
     * of the parts it swallowed onto the car it hands back, before that car is
     * anything but an item.
     */
    public static long withQuality(long bits, CarPart part, CarPart.Quality quality) {
        int shift = part.ordinal() * CarEntity.QUALITY_BITS;
        return (bits & ~(CarEntity.QUALITY_MASK << shift)) | ((long) quality.ordinal() << shift);
    }

    /** Every slot's quality at once, for saving and for pocketing. */
    public long getQualityBits() {
        return this.car.getEntityData().get(CarEntity.PART_QUALITY);
    }

    public void setQualityBits(long bits) {
        this.car.getEntityData().set(CarEntity.PART_QUALITY, bits);
    }

    /** Whether the engine ran last tick, so a start can be told from running on. */
    private boolean ranLastTick;

    /** Charge the starter has taken that the alternator has yet to put back. */
    private float starterDebt;

    /**
     * Every start turns the engine over on the battery, and the alternator puts
     * the charge back over the next few seconds of running. A battery with
     * little left can manage one turn of the key and no more; one that keeps
     * being started without a run in between goes flat.
     */
    private void tickStarter() {
        boolean running = this.car.isEngineRunning();
        if (running && !this.ranLastTick && !this.car.isElectric() && this.car.hasPart(CarPart.BATTERY)
                && this.isFitted(CarPart.BATTERY)) {
            float before = this.getCondition(CarPart.BATTERY);
            float after = Math.max(0.0F, before - STARTER_DRAW);
            this.setCondition(CarPart.BATTERY, after);
            this.starterDebt += before - after;
        }
        if (running && this.starterDebt > 0.0F && this.isFitted(CarPart.BATTERY)) {
            float back = Math.min(this.starterDebt, STARTER_RECHARGE * this.getConditionFraction(CarPart.ALTERNATOR));
            this.setCondition(CarPart.BATTERY, Math.min(CarPart.MAX_CONDITION, this.getCondition(CarPart.BATTERY) + back));
            this.starterDebt -= back;
        }
        // Read again: a start on a battery with nothing to spare leaves it flat and the engine dead.
        this.ranLastTick = this.car.isEngineRunning();
    }

    public float getStarterDebt() {
        return this.starterDebt;
    }

    /**
     * Wear, which is the one place quality is felt. Every rule above hands its
     * number to this method, so a cheap part wears faster at everything it does
     * without a single one of those rules having to know about quality.
     */
    void wear(CarPart part, float amount) {
        if (amount <= 0.0F || !this.car.hasPart(part) || !this.isFitted(part)) {
            return;
        }
        float scale = amount * this.getQuality(part).wearFactor() * (float) me.trossty.car.CarConfig.get().partWear;
        if (part == CarPart.ENGINE) {
            // Wear with no cause of its own, a drowning or a knock, takes from all of the engine alike.
            this.engine.wear(scale, scale, scale, scale);
            return;
        }
        int slot = PartPositions.slot(part);
        if (slot < 0) {
            this.setCondition(part, Math.max(CarPart.WORN_OUT, this.getCondition(part) - scale * this.ageFactor(part)));
            return;
        }
        // A set wears place by place, each at its own share of the work.
        float[] here = this.places[slot];
        float[] shares = this.wearShares(part);
        for (int place = 0; place < this.placesOf(part); place++) {
            if (this.counts(part, place) && here[place] > 0.0F) {
                here[place] = Math.max(CarPart.WORN_OUT,
                        here[place] - scale * shares[place] * this.ageFactor(here[place] / CarPart.MAX_CONDITION));
            }
        }
        this.settlePlaces(part);
    }

    /**
     * Wears only the places of a set that sit at one side of the body, each by
     * the full {@code amount}: the springs at the end that hit the wall, the
     * tyres along the sill that scraped it. A part in one place, or a set with
     * no place on that side, wears as a whole.
     */
    void wearAt(CarPart part, BodyZone zone, float amount) {
        if (!PartPositions.atWheels(part)) {
            this.wear(part, amount);
            return;
        }
        if (amount <= 0.0F || !this.car.hasPart(part) || !this.isFitted(part)) {
            return;
        }
        int slot = PartPositions.slot(part);
        boolean[] struck = new boolean[PartPositions.MAX];
        boolean any = false;
        for (int place = 0; place < this.placesOf(part); place++) {
            struck[place] = this.counts(part, place) && this.places[slot][place] > 0.0F && zone.holds(this, place);
            any |= struck[place];
        }
        if (!any) {
            this.wear(part, amount);
            return;
        }
        float scale = amount * this.getQuality(part).wearFactor() * (float) me.trossty.car.CarConfig.get().partWear;
        float[] here = this.places[slot];
        for (int place = 0; place < this.placesOf(part); place++) {
            if (struck[place]) {
                here[place] = Math.max(CarPart.WORN_OUT,
                        here[place] - scale * this.ageFactor(here[place] / CarPart.MAX_CONDITION));
            }
        }
        this.settlePlaces(part);
    }

    /**
     * How much of a set's wear each of its places takes, averaging 1 before
     * anything has gone wrong. No two places are quite alike; the front
     * brakes do most of the stopping; the steered tyres scrub in every turn,
     * far more so knocked out of line, and a flat is chewed to pieces; and
     * the plugs either side of a blown gasket foul in the coolant. A knock
     * on one side of the body wears that side's places through {@link #wearAt}.
     */
    private float[] wearShares(CarPart part) {
        float[] shares = new float[PartPositions.MAX];
        int count = this.placesOf(part);
        float total = 0.0F;
        int counted = 0;
        for (int place = 0; place < count; place++) {
            if (!this.counts(part, place)) {
                continue;
            }
            float end = 1.0F;
            if (PartPositions.atWheels(part)) {
                int axle = this.axle(place);
                end = switch (part) {
                    case BRAKE_PADS, BRAKE_DISCS -> axle > 0 ? FRONT_BRAKE_SHARE : axle < 0 ? REAR_BRAKE_SHARE : 1.0F;
                    case WHEELS -> axle > 0 ? FRONT_TYRE_SHARE : 1.0F;
                    // A knock with a side to it goes through wearAt instead.
                    default -> 1.0F;
                };
            }
            shares[place] = end * (1.0F + PLACE_SPREAD * PartPositions.bias(this.car.getUUID(), part, place));
            total += shares[place];
            counted++;
        }
        if (total <= 0.0F) {
            return shares;
        }
        int gasket = part == CarPart.SPARK_PLUGS
                ? me.trossty.car.item.CompressionTesterItem.gasketCylinder(this.car) : -1;
        int flat = part == CarPart.WHEELS ? this.car.getFlatTyre() : -1;
        for (int place = 0; place < count; place++) {
            shares[place] *= counted / total;
            if (part == CarPart.WHEELS && this.axle(place) > 0) {
                shares[place] *= 1.0F + MISALIGNED_TYRE_WEAR * Math.abs(this.car.upkeep().getAlignment());
            }
            if (place == flat) {
                shares[place] *= FLAT_TYRE_WEAR;
            }
            if (gasket >= 0 && (place == gasket || place == gasket + 1)) {
                shares[place] *= GASKET_PLUG_WEAR;
            }
        }
        return shares;
    }

    /**
     * How much faster than new this part wears now: an old car wears
     * everything faster, and a part on its last legs goes quicker still, so
     * putting off a service costs more than the service would have.
     */
    public float ageFactor(CarPart part) {
        return this.ageFactor(this.getConditionFraction(part));
    }

    float ageFactor(float fraction) {
        return mileageFactor(this.getOdometer()) * (1.0F + TIRED_WEAR * (1.0F - fraction));
    }

    /** 1 new, rising to {@code 1 + MILEAGE_WEAR} at {@link #HIGH_MILEAGE}, and no further. */
    public static float mileageFactor(float odometer) {
        return 1.0F + MILEAGE_WEAR * Mth.clamp(odometer / HIGH_MILEAGE, 0.0F, 1.0F);
    }

    /** Blocks driven in this car's life. */
    public float getOdometer() {
        return this.car.getEntityData().get(CarEntity.ODOMETER);
    }

    public void setOdometer(float blocks) {
        this.car.getEntityData().set(CarEntity.ODOMETER, Math.max(0.0F, blocks));
    }

    /** 1 with a fresh part, {@code worn} with a dead one, a line between. */
    float partFactor(CarPart part, float worn) {
        return this.partFactor(part, worn, worn);
    }

    /**
     * The same, with its own answer for a part that is not there. Used for
     * penalties as well as for losses: a value above 1 reads as "this costs
     * you more", which is what a blocked air filter does to the fuel bill.
     */
    float partFactor(CarPart part, float worn, float missing) {
        if (!this.isFitted(part)) {
            return missing;
        }
        return Mth.lerp(this.getConditionFraction(part), worn, 1.0F);
    }

    /**
     * What the grade of a fitted part does to the job that part does. A grade
     * is not only how long a part lasts: a performance engine is a faster
     * engine, and economy pads are worse brakes from the day they go on.
     */
    public float grade(CarPart part) {
        return this.isFitted(part) ? this.getQuality(part).performance() : 1.0F;
    }

    /** The same at half strength, for the parts that only help the job along. */
    float minorGrade(CarPart part) {
        return 1.0F + (this.grade(part) - 1.0F) * 0.5F;
    }

    /** Whether the grade of this part changes how the car drives, not just how long it lasts. */
    public static boolean gradeIsFelt(CarPart part) {
        return switch (part) {
            case ENGINE, EXHAUST, AIR_FILTER, TRANSMISSION, CLUTCH, DRIVESHAFT,
                 BRAKE_PADS, BRAKE_DISCS, WHEELS, STEERING_RACK, RADIATOR -> true;
            default -> false;
        };
    }

    /** Tyres and steering on a laid road wear at this much of what they do on anything else. */
    public static final float ASPHALT_WEAR = 0.6F;

    /**
     * A tired rack drifts the car its own way, fixed by its identity; a
     * tired spring sags its own corner and the car leans off towards it; a
     * seized caliper drags its corner and the car swings towards that.
     */
    float wornPull() {
        float pull = 0.0F;
        if (this.car.hasPart(CarPart.STEERING_RACK) && this.isFitted(CarPart.STEERING_RACK)) {
            float side = (this.car.getUUID().getLeastSignificantBits() & 1L) == 0L ? -1.0F : 1.0F;
            pull += side * tiredPull(this.getConditionFraction(CarPart.STEERING_RACK));
        }
        int spring = this.car.hasPart(CarPart.SUSPENSION) ? this.worstPlace(CarPart.SUSPENSION) : -1;
        if (spring >= 0) {
            pull += this.lateral(spring) * tiredPull(this.worstFraction(CarPart.SUSPENSION));
        }
        // A seized caliper holds its corner back, and the car swings round it,
        // brake pedal or not.
        for (int wheel = 0; wheel < this.car.tyreCount(); wheel++) {
            pull += this.lateral(wheel) * CarEntity.MAX_PULL * this.dragAt(wheel);
        }
        return Mth.clamp(pull, -CarEntity.MAX_PULL, CarEntity.MAX_PULL);
    }

    /** Nothing above half condition, {@link CarEntity#MAX_PULL} at nothing, a line between. */
    private static float tiredPull(float fraction) {
        return fraction >= FaultCodes.ADVISORY ? 0.0F
                : CarEntity.MAX_PULL * (FaultCodes.ADVISORY - fraction) / FaultCodes.ADVISORY;
    }

    /**
     * Which way the brakes drag the car while they are on, degrees a tick:
     * towards the side whose pads and discs bite harder. Even brakes, none.
     */
    public float brakePull() {
        if (!this.car.hasPart(CarPart.BRAKE_PADS)) {
            return 0.0F;
        }
        float leaning = 0.0F;
        float total = 0.0F;
        for (int wheel = 0; wheel < this.car.tyreCount(); wheel++) {
            float bite = this.placeFactor(CarPart.BRAKE_PADS, wheel, CarEntity.WORN_PADS_BRAKE, CarEntity.MISSING_PADS_BRAKE)
                    * this.placeFactor(CarPart.BRAKE_DISCS, wheel, CarEntity.WORN_DISCS_BRAKE, CarEntity.MISSING_DISCS_BRAKE);
            leaning += this.lateral(wheel) * bite;
            total += bite;
        }
        return total <= 0.0F ? 0.0F : BRAKE_PULL * leaning / total;
    }

    /** How much faster dirty oil wears the things it is supposed to protect. */
    private float oilWearFactor() {
        return this.partFactor(CarPart.OIL_FILTER, WORN_OIL_WEAR, MISSING_OIL_WEAR);
    }

    /**
     * Miles wear everything that turns, and the engine wears even at a
     * standstill because it is still running. Clean oil is what decides how
     * fast the two expensive items go, which is the point of a filter that
     * costs two ingots and some paper.
     */
    void wearParts() {
        this.tickStarter();
        float load = Math.min(1.0F, Math.abs(this.car.getEngineSpeed()) / CarEntity.MAX_SPEED);
        float oil = this.oilWearFactor();
        if (this.car.isEngineRunning()) {
            float running = CarEntity.IDLE_WEAR_SHARE + (1.0F - CarEntity.IDLE_WEAR_SHARE) * load;
            // Boost and unfiltered air are both paid for out of the engine.
            float hardLife = (this.car.hasTurbo() ? CarSetup.TURBO_ENGINE_WEAR : 1.0F)
                    * (this.car.hasOpenFilter() ? CarSetup.OPEN_FILTER_ENGINE_WEAR : 1.0F);
            float dirty = this.car.getDirtyFuel();
            float base = CarPart.ENGINE.wearPerTick() * running * hardLife;
            float fuel = 1.0F + DIRTY_ENGINE_WEAR * dirty;
            float lube = oil * this.car.upkeep().lowOilWear();
            // Grit in the oil scores the bores and the bearings, dirty fuel cokes
            // the rings and the valves, and an engine boiling over lifts its gasket.
            this.wearEngine(base * lube * fuel, base * fuel,
                    base * (this.car.isOverheating() ? OVERHEAT_GASKET_WEAR : 1.0F), base * lube);
            if (this.car.getEngineFuel().needsSpark()) {
                this.wear(CarPart.SPARK_PLUGS, CarPart.SPARK_PLUGS.wearPerTick() * running
                        * (1.0F + DIRTY_PLUG_WEAR * dirty));
            }
            this.wear(CarPart.AIR_FILTER, CarPart.AIR_FILTER.wearPerTick() * running
                    * (this.car.hasOpenFilter() ? CarSetup.OPEN_FILTER_OWN_WEAR : 1.0F));
            this.wear(CarPart.OIL_FILTER, CarPart.OIL_FILTER.wearPerTick() * running);
            this.wear(CarPart.EXHAUST, CarPart.EXHAUST.wearPerTick() * running);
            this.wear(CarPart.MUFFLER, CarPart.MUFFLER.wearPerTick() * running);
            // The pump turns on the belt whatever the load; the thermostat
            // opens and shuts with every warm-up.
            this.wear(CarPart.WATER_PUMP, CarPart.WATER_PUMP.wearPerTick() * running);
            this.wear(CarPart.THERMOSTAT, CarPart.THERMOSTAT.wearPerTick() * running);
            // Grit in the fuel goes through the pump first and the nozzles last.
            this.wear(CarPart.TANK_PUMP, CarPart.TANK_PUMP.wearPerTick() * running
                    * (1.0F + DIRTY_PUMP_WEAR * dirty));
            this.wear(CarPart.INJECTORS, CarPart.INJECTORS.wearPerTick() * running
                    * (1.0F + DIRTY_INJECTOR_WEAR * dirty));
            // A coil breaks down in the heat: a hot engine kills them.
            this.wear(CarPart.IGNITION_COILS, CarPart.IGNITION_COILS.wearPerTick() * running
                    * (1.0F + this.car.getEngineTemp()));
            this.wear(CarPart.ALTERNATOR, CarPart.ALTERNATOR.wearPerTick() * running);
            // A radiator that is already struggling cooks itself faster.
            this.wear(CarPart.RADIATOR,
                    CarPart.RADIATOR.wearPerTick() * running * (1.0F + this.car.getEngineTemp()));
            // The battery only holds its charge while something is charging it.
            this.wear(CarPart.BATTERY,
                    BATTERY_DRAIN * (1.0F - this.getConditionFraction(CarPart.ALTERNATOR)));
        }

        // Everything that turns wears whenever it turns. A car being pushed
        // down the road is still using its tyres.
        if (load > 0.0F) {
            this.wear(CarPart.TRANSMISSION, CarPart.TRANSMISSION.wearPerTick() * load * oil);
            this.wear(CarPart.CLUTCH, CarPart.CLUTCH.wearPerTick() * load);
            this.wear(CarPart.DRIVESHAFT, CarPart.DRIVESHAFT.wearPerTick() * load);
            // Wheels up, nothing rolls and nothing steers them.
            if (!this.car.isFlying()) {
                float road = this.car.onAsphalt() ? ASPHALT_WEAR : 1.0F;
                this.wear(CarPart.WHEELS, CarPart.WHEELS.wearPerTick() * load * this.car.getTyre().wear() * road
                        * (1.0F + 2.0F * this.car.upkeep().tyreSoftness()));
                this.wear(CarPart.STEERING_RACK, CarPart.STEERING_RACK.wearPerTick() * load * road);
                this.wear(CarPart.BALL_JOINTS, CarPart.BALL_JOINTS.wearPerTick() * load * road);
                this.dragPads(load);
            }
        }
    }

    /** A seized caliper keeps its pad on the disc while the car rolls: that pad goes, the others do not. */
    private void dragPads(float load) {
        if (!this.car.hasPart(CarPart.BRAKE_PADS) || !this.isFitted(CarPart.BRAKE_PADS)) {
            return;
        }
        int slot = PartPositions.slot(CarPart.BRAKE_PADS);
        float scale = CarPart.BRAKE_PADS.wearPerTick() * DRAG_PAD_WEAR * load
                * this.getQuality(CarPart.BRAKE_PADS).wearFactor() * (float) me.trossty.car.CarConfig.get().partWear;
        boolean any = false;
        for (int wheel = 0; wheel < this.placesOf(CarPart.BRAKE_PADS); wheel++) {
            float drag = this.dragAt(wheel);
            if (drag > 0.0F && this.places[slot][wheel] > 0.0F) {
                this.places[slot][wheel] = Math.max(CarPart.WORN_OUT, this.places[slot][wheel] - scale * drag);
                any = true;
            }
        }
        if (any) {
            this.settlePlaces(CarPart.BRAKE_PADS);
        }
    }

    /**
     * How hard one corner's caliper drags, 0 free to 1 seized. A caliper does
     * its job until it is tired; then its piston sticks out further the worse
     * it gets, and holds the pad on the disc as the car rolls.
     */
    public float dragAt(int wheel) {
        if (!this.car.hasPart(CarPart.BRAKE_CALIPERS) || !this.isFitted(CarPart.BRAKE_CALIPERS)
                || wheel >= this.placesOf(CarPart.BRAKE_CALIPERS)) {
            return 0.0F;
        }
        float left = this.getCondition(CarPart.BRAKE_CALIPERS, wheel) / CarPart.MAX_CONDITION;
        return left >= FaultCodes.WARNING ? 0.0F : (FaultCodes.WARNING - left) / FaultCodes.WARNING;
    }

    /** The worst corner's drag, 0 to 1: what a seized caliper costs the car flat out. */
    public float caliperDrag() {
        float worst = 0.0F;
        for (int wheel = 0; wheel < this.car.tyreCount(); wheel++) {
            worst = Math.max(worst, this.dragAt(wheel));
        }
        return worst;
    }

    /**
     * How much slack the worst ball joint leaves in the steering, 0 tight to 1
     * gone: none while they are sound, more the worse the worst one gets, and
     * all of it with none fitted.
     */
    public float play() {
        if (!this.car.hasPart(CarPart.BALL_JOINTS)) {
            return 0.0F;
        }
        if (!this.isFitted(CarPart.BALL_JOINTS)) {
            return 1.0F;
        }
        float worst = this.worstFraction(CarPart.BALL_JOINTS);
        return worst >= FaultCodes.ADVISORY ? 0.0F : (FaultCodes.ADVISORY - worst) / FaultCodes.ADVISORY;
    }

    /**
     * Which way slack in the steering lets the car wander this tick, degrees,
     * signed by side. Never the same way for long: that is how it is told from
     * a tired rack, which always drifts the one way.
     */
    float wander() {
        float play = this.play();
        if (play <= 0.0F) {
            return 0.0F;
        }
        float time = this.car.tickCount;
        float phase = 3.0F * PartPositions.bias(this.car.getUUID(), CarPart.BALL_JOINTS, 0);
        return WANDER * play * Mth.sin(time * 0.11F + phase) * (0.6F + 0.4F * Mth.sin(time * 0.037F));
    }

    /**
     * How much of a full-throttle thirst the tank pump keeps up with, 0 to 1:
     * all of it new, a cruise's worth once it is tired. With none at all the
     * car does not start, which is {@link CarPart#essential()}'s business.
     */
    public float pumpFeed() {
        if (!this.car.hasPart(CarPart.TANK_PUMP) || !this.isFitted(CarPart.TANK_PUMP)) {
            return 1.0F;
        }
        return Mth.lerp(this.getConditionFraction(CarPart.TANK_PUMP), WORN_PUMP_FEED, 1.0F);
    }

    /** How short of fuel the engine runs at this load, 0 fed to 1 starved: the pump behind the demand. */
    public float starvation(float load) {
        float feed = this.pumpFeed();
        return load <= feed ? 0.0F : Math.min(1.0F, (load - feed) / Math.max(0.05F, 1.0F - feed));
    }

    /**
     * How a tired thermostat has stuck: 1 shut, -1 open, 0 working. Which way
     * is the car's own luck, the same every time; one that is not there at all
     * is as good as stuck open, all the coolant through the radiator all the time.
     */
    public int thermostatStuck() {
        if (!this.car.hasPart(CarPart.THERMOSTAT)) {
            return 0;
        }
        if (!this.isFitted(CarPart.THERMOSTAT)) {
            return -1;
        }
        if (this.getConditionFraction(CarPart.THERMOSTAT) > FaultCodes.WARNING) {
            return 0;
        }
        return PartPositions.bias(this.car.getUUID(), CarPart.THERMOSTAT, 0) >= 0.0F ? 1 : -1;
    }

    /**
     * How much of the coolant the water pump moves, 0 to 1. A tired pump
     * still turns (it weeps instead, see {@link #pumpWeep}); without one the
     * coolant only stands in the engine and boils.
     */
    float circulation() {
        if (!this.car.hasPart(CarPart.WATER_PUMP)) {
            return 1.0F;
        }
        return this.isFitted(CarPart.WATER_PUMP) ? 1.0F : CarEntity.MISSING_PUMP_COOLING;
    }

    /** How badly a tired water pump's seal weeps coolant, 0 dry to 1 pouring. */
    public float pumpWeep() {
        if (!this.car.hasPart(CarPart.WATER_PUMP) || !this.isFitted(CarPart.WATER_PUMP)) {
            return 0.0F;
        }
        float left = this.getConditionFraction(CarPart.WATER_PUMP);
        return left >= FaultCodes.ADVISORY ? 0.0F : (FaultCodes.ADVISORY - left) / FaultCodes.ADVISORY;
    }

    /** The engine's running wear, each of its insides by what it minds: see {@link EngineInternals}. */
    private void wearEngine(float rings, float valves, float gasket, float bearings) {
        if (!this.car.hasPart(CarPart.ENGINE) || !this.isFitted(CarPart.ENGINE)) {
            return;
        }
        float scale = this.getQuality(CarPart.ENGINE).wearFactor() * (float) me.trossty.car.CarConfig.get().partWear;
        this.engine.wear(rings * scale, valves * scale, gasket * scale, bearings * scale);
    }

    /**
     * Brake wear, taken from the speed the car actually scrubbed off rather
     * than from the brake key, which only exists on the driver's own client.
     *
     * <p>Worn pads eat the discs behind them several times faster. That is the
     * one lesson this whole system is built to teach: the cheap part you did
     * not change is what ruins the expensive one behind it.
     *
     * <p>Public so a test can hand it two speeds and watch the brakes pay for
     * the difference; nothing else outside the tick loop calls it.
     */
    public void wearBrakes(float speedNow) {
        float scrubbed = Math.abs(this.lastTrackedSpeed) - Math.abs(speedNow);
        this.lastTrackedSpeed = speedNow;
        // Still rolling, so this was braking rather than hitting something;
        // and in the air the speed goes into the climb, not into the pads.
        if (scrubbed <= 0.0F || Math.abs(speedNow) < 0.05F || this.car.isFlying()) {
            return;
        }
        this.complainAboutBrakes(scrubbed);
        float work = scrubbed * BRAKE_WEAR_PER_DECEL;
        this.car.upkeep().setBrakeFluid(this.car.upkeep().getBrakeFluid() - work * Upkeep.BRAKE_FLUID_USE);
        this.wear(CarPart.BRAKE_PADS, CarPart.BRAKE_PADS.wearPerTick() * work);
        this.wear(CarPart.BRAKE_CALIPERS, CarPart.BRAKE_CALIPERS.wearPerTick() * work);
        float padsLeft = this.getConditionFraction(CarPart.BRAKE_PADS);
        this.wear(CarPart.BRAKE_DISCS, CarPart.BRAKE_DISCS.wearPerTick() * work
                * Mth.lerp(padsLeft, PADS_EAT_DISCS, 1.0F));
    }

    /**
     * What tired brakes sound like from the road: a thin squeal once the pads
     * are getting low, and metal on metal once they are gone and the discs are
     * paying for it. Everyone nearby hears it, which is the point: the car
     * says what it needs before anybody plugs a reader in.
     */
    private void complainAboutBrakes(float scrubbed) {
        if (this.brakeNoiseCooldown > 0) {
            this.brakeNoiseCooldown--;
            return;
        }
        // Only real braking: engine drag alone never takes off this much a tick.
        if (scrubbed < BRAKING_SCRUB || !this.car.hasPart(CarPart.BRAKE_PADS)) {
            return;
        }
        // One corner down to its backing plate is enough to be heard.
        float pads = this.worstFraction(CarPart.BRAKE_PADS);
        if (pads > FaultCodes.WARNING) {
            return;
        }
        boolean grinding = pads <= FaultCodes.CRITICAL;
        this.car.level().playSound(null, this.car.getX(), this.car.getY() + 0.3, this.car.getZ(),
                grinding ? SoundEvents.GRINDSTONE_USE : me.trossty.car.sound.ModSounds.TYRE_SKID,
                SoundSource.NEUTRAL, grinding ? 0.8F : 0.35F, grinding ? 0.7F : 2.0F);
        this.brakeNoiseCooldown = BRAKE_NOISE_COOLDOWN;
    }

    /**
     * Whether the engine stumbles this tick. Fresh parts never do; a finished
     * set of plugs does about one tick in twenty, idling or not, and so does a
     * cylinder with a sticking injector. A tired coil only misses when the
     * engine is pulling.
     */
    boolean misfiring() {
        if (this.car.isElectric()) {
            return false;
        }
        float load = Math.min(1.0F, Math.abs(this.car.getEngineSpeed()) / CarEntity.MAX_SPEED);
        int cylinders = this.placesOf(CarPart.INJECTORS);
        float rough = 0.0F;
        for (int cylinder = 0; cylinder < cylinders; cylinder++) {
            rough += this.misfireIn(cylinder, load);
        }
        return rough > 0.0F && this.car.getRandom().nextFloat() < MISFIRE_CHANCE * rough / cylinders;
    }

    /**
     * How badly one cylinder misfires, 0 to 1, by what the engine's misfire
     * counter has seen: under load, so a tired coil shows up here although it
     * fires cleanly at a standstill.
     */
    public float misfireIn(int cylinder) {
        return this.misfireIn(cylinder, 1.0F);
    }

    /**
     * How badly one cylinder misfires at this load, 0 to 1: a cylinder that
     * cannot hold its squeeze fires badly whatever is feeding it; a worn plug
     * fires weakly; a tired coil cannot make the spark the engine asks for
     * when it is pulling; a sticking injector dribbles. A diesel has no
     * spark to lose: its plugs only warm it to start.
     */
    public float misfireIn(int cylinder, float load) {
        if (this.car.isElectric()) {
            return 0.0F;
        }
        float worst = me.trossty.car.item.CompressionTesterItem.compressionLoss(this.car, cylinder);
        if (this.car.getEngineFuel().needsSpark()) {
            if (this.isFitted(CarPart.SPARK_PLUGS)) {
                worst = Math.max(worst, 1.0F - this.getCondition(CarPart.SPARK_PLUGS, cylinder) / CarPart.MAX_CONDITION);
            }
            if (this.car.hasPart(CarPart.IGNITION_COILS) && this.isFitted(CarPart.IGNITION_COILS)) {
                worst = Math.max(worst, load
                        * (1.0F - this.getCondition(CarPart.IGNITION_COILS, cylinder) / CarPart.MAX_CONDITION));
            }
        }
        if (this.car.hasPart(CarPart.INJECTORS) && this.isFitted(CarPart.INJECTORS)) {
            worst = Math.max(worst, 1.0F - this.getCondition(CarPart.INJECTORS, cylinder) / CarPart.MAX_CONDITION);
        }
        return worst;
    }

    /**
     * How much of the engine's squeeze there is to push with, 0 to 1: a
     * cylinder with bent valves, or with a blown gasket beside it, adds next
     * to nothing. A motor has no cylinders to lose.
     */
    public float firing() {
        if (this.car.isElectric() || !this.car.hasPart(CarPart.ENGINE) || !this.isFitted(CarPart.ENGINE)) {
            return 1.0F;
        }
        int count = this.engine.cylinders();
        float lost = 0.0F;
        for (int cylinder = 0; cylinder < count; cylinder++) {
            lost += me.trossty.car.item.CompressionTesterItem.compressionLoss(this.car, cylinder);
        }
        return 1.0F - lost / count;
    }

    /**
     * Reads back what {@link #save} wrote. A car saved before each wheel and
     * cylinder kept its own gets the whole set alike, and one saved before
     * parts were iron, gold, diamond and netherite has its old grades turned
     * into tiers.
     */
    void load(ValueInput view) {
        for (CarPart part : CarPart.values()) {
            this.setCondition(part, view.getFloatOr("Part" + part.key(), CarPart.MAX_CONDITION));
        }
        for (int slot = 0; slot < PartPositions.PARTS; slot++) {
            CarPart part = PartPositions.part(slot);
            int[] saved = view.tag().getIntArray("Places" + part.key());
            for (int place = 0; place < Math.min(saved.length, PartPositions.MAX); place++) {
                this.places[slot][place] = Mth.clamp(Float.intBitsToFloat(saved[place]), 0.0F, CarPart.MAX_CONDITION);
            }
            if (saved.length > 0) {
                this.settlePlaces(part);
            }
        }
        this.engine.load(view);
        // Saved as an int while every tier fitted in one: read either.
        long tiers = view.getLongOr("PartQuality", CarEntity.defaultQualityBits());
        if (view.getIntOr("PartTiers", 0) == 0) {
            long converted = 0L;
            for (CarPart part : CarPart.values()) {
                int old = (int) ((tiers >>> (part.ordinal() * CarEntity.QUALITY_BITS)) & CarEntity.QUALITY_MASK);
                converted = withQuality(converted, part, CarPart.Quality.fromOldGrade(old));
            }
            tiers = converted;
        }
        this.setQualityBits(tiers);
        this.setOdometer(view.getFloatOr("Odometer", 0.0F));
        this.starterDebt = Math.max(0.0F, view.getFloatOr("StarterDebt", 0.0F));
    }

    /** Writes each part's wear, place by place, its tier, the miles and what the starter is owed. */
    void save(ValueOutput view) {
        for (CarPart part : CarPart.values()) {
            view.putFloat("Part" + part.key(), this.getCondition(part));
        }
        for (int slot = 0; slot < PartPositions.PARTS; slot++) {
            int[] bits = new int[PartPositions.MAX];
            for (int place = 0; place < PartPositions.MAX; place++) {
                bits[place] = Float.floatToIntBits(this.places[slot][place]);
            }
            view.tag().putIntArray("Places" + PartPositions.part(slot).key(), bits);
        }
        this.engine.save(view);
        view.putLong("PartQuality", this.getQualityBits());
        view.putInt("PartTiers", 1);
        view.putFloat("Odometer", this.getOdometer());
        if (this.starterDebt > 0.0F) {
            view.putFloat("StarterDebt", this.starterDebt);
        }
    }
}
