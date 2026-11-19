package me.trossty.car.entity;

import net.minecraft.util.Mth;
import java.util.List;

/**
 * The automatic gearbox and the engine turning behind it: the vehicle's own
 * gears from its definition, how hard each pulls through the engine's torque
 * curve, when it changes up and down, and what the engine is doing meanwhile.
 *
 * <p>The gears follow the road speed. The engine has revs, a throttle and a
 * turbo shaft of its own, which chase the wheels rather than snap to them:
 * changing up, the throttle is cut and the revs fall by themselves to the
 * next gear's; changing down, a blip of throttle spins them up to the lower
 * gear's; pulling away, the converter lets them flare above the wheels. The
 * rev counter and the engine note read these.
 *
 * <p>Flat out it holds each gear to near the redline. Once the car is up to
 * its speed and holding it, nobody keeps an engine screaming: the driver
 * eases to the little throttle the road takes, and the box changes up into a
 * tall overdrive gear, so a car cruising at the game's top speed ticks along
 * at under half its redline, as a real one does at that road speed. Fall
 * back off the pace with the pedal down and it kicks down again.
 *
 * <p>Each side runs it from the speed it sees and the pedal it is told about
 * ({@link CarEntity#throttlePedal}), so the driver's needle, their engine
 * note and a bystander's all change gear together. Whether there is a box at
 * all is the vehicle's to say ({@link CarEntity#hasGearbox}).
 */
public final class Gearbox {

    private final CarEntity car;

    Gearbox(CarEntity car) {
        this.car = car;
    }

    /**
     * How hard each of the usual five gears pulls, against the plain
     * throttle step: first shoves, fifth leans. A gear of any other box pulls
     * what one this long would, read off between them; see {@link #pullOf}.
     */
    private static final float[] FIVE_SPEED_PULL = {1.45F, 1.1F, 0.85F, 0.68F, 0.55F};

    /** How much harder a gear half as long pulls, past either end of the five. */
    private static final float PULL_SLOPE = 0.68F;

    private static final float UPSHIFT = CarSetup.UPSHIFT;

    private static final float DOWNSHIFT = CarSetup.DOWNSHIFT;

    /** How long a change takes, in ticks; the drive is mostly cut while it happens. */
    private static final int SHIFT_TICKS = 6;

    private static final float SHIFT_DRIVE = 0.2F;

    /** The pull of a single fixed ratio: about what the five gears give over a run to top speed. */
    private static final float DIRECT_PULL = 1.0F;

    /**
     * How fast the revs fall with the throttle shut and the clutch out, per
     * tick, as a share of the redline: the engine's own friction and pumping,
     * some eight thousand rpm a second in a seven-thousand-rpm car.
     */
    private static final float FREE_FALL = 0.06F;

    /** How fast a blip of throttle spins a free engine up, per tick, as a share of the redline. */
    private static final float FREE_RISE = 0.1F;

    /** How tightly the clutch, once in, ties the engine to the wheels: the share of the gap closed each tick. */
    private static final float BITE = 0.6F;

    /** How far a blip overshoots the revs the lower gear needs, so the clutch takes up without a jolt. */
    private static final float BLIP_MATCH = 1.04F;

    /**
     * How far above the wheels the revs flare pulling away on full throttle,
     * as a share of the band from idle to redline: the torque converter's
     * stall speed.
     */
    private static final float STALL = 0.3F;

    /** How much of the way to the pedal the throttle opens each tick. It shuts at once, on its spring. */
    private static final float THROTTLE_OPEN = 0.35F;

    /**
     * How much taller the overdrive is than the top gear: cruising in it at
     * the top gear's top speed the engine turns at about 1 / OVERDRIVE of
     * its band above idle.
     */
    private static final float OVERDRIVE = 2.4F;

    /** The share of the top speed it has to be holding to count as cruising. */
    private static final float CRUISE_PACE = 0.97F;

    /** How long, in ticks, it holds that pace before the driver eases off and the box changes up into overdrive. */
    private static final int CRUISE_TICKS = 20;

    /** Below this share of the top speed with the pedal down, it kicks down out of overdrive for the pull. */
    private static final float KICKDOWN_PACE = 0.9F;

    /** The throttle it takes to hold the speed, cruising: the road's load, not the engine's all. */
    private static final float CRUISE_THROTTLE = 0.3F;

    /** How much of the gap the turbo shaft closes each tick spooling up, and spinning down on its bearings. */
    private static final float SPOOL_UP = 0.12F;
    private static final float SPOOL_DOWN = 0.05F;

    /** Revs, as a share of the band from idle, by which the exhaust carries enough to spin the turbo flat out. */
    private static final float BOOST_REVS = 0.45F;

    /** The forward gear the box is in, 1 to {@link #gearCount()}. Each side keeps its own. */
    private int driveGear = 1;

    private int shiftTicks;

    /** How long the car has held its pace on the pedal, for {@link #CRUISE_TICKS}. */
    private int cruiseTicks;

    /** Whether the change under way is up a gear, which cuts the throttle, or down, which blips it. */
    private boolean upshifting;

    /** What the engine is doing: revs, throttle opening 0 to 1, and the turbo shaft's speed 0 to 1. */
    private float rpm;
    private float throttle;
    private float turboSpeed;

    /** The engine in the bay, read once a tick; see {@link CarEntity#engineSpec}. */
    private CarSetup.EngineSpec spec;

    /** The engine in the bay: cylinders, idle and redline. */
    public CarSetup.EngineSpec getEngineSpec() {
        if (this.spec == null) {
            this.spec = this.car.engineSpec();
        }
        return this.spec;
    }

    /** Where the rev counter's red band starts: a diesel runs out of breath long before a petrol. */
    public float getRedlineRpm() {
        return this.getEngineSpec().redlineRpm();
    }

    /** What the engine ticks over at with nobody on the throttle. */
    public float getIdleRpm() {
        return this.getEngineSpec().idleRpm();
    }

    /** The gear in, 1 to {@link #gearCount()}; -1 in reverse, 0 when the engine is off. */
    public int getGear() {
        if (!this.car.isEngineRunning()) {
            return 0;
        }
        if (this.car.getEngineSpeed() < -0.005F) {
            return -1;
        }
        // A belt drive's overdrive is the belt riding out, not a gear of its own.
        return this.gearCount() == 1 ? 1 : this.forwardGear();
    }

    /**
     * How much of the top speed each forward gear reaches at the redline, as
     * the vehicle's definition gives them: six close ones in a sports car,
     * eight short ones in a lorry, one in a scooter's belt drive.
     */
    public java.util.List<Float> gearTops() {
        return this.car.tuning().gears();
    }

    public int gearCount() {
        return this.car.hasGearbox() ? this.gearTops().size() : 1;
    }

    /**
     * The gear the box is in, kept inside the box it has now: a reload can
     * hand a car a shorter one. One past the top is the overdrive.
     */
    private int forwardGear() {
        return Mth.clamp(this.driveGear, 1, this.gearCount() + (this.hasOverdrive() ? 1 : 0));
    }

    /** Whether it has a cruising gear: a road vehicle's box does; a boat's or an aircraft's propeller does not. */
    private boolean hasOverdrive() {
        var chassis = this.car.chassis();
        return this.car.hasGearbox() && chassis != me.trossty.car.vehicle.VehicleDefinition.Chassis.BOAT
                && chassis != me.trossty.car.vehicle.VehicleDefinition.Chassis.PLANE
                && chassis != me.trossty.car.vehicle.VehicleDefinition.Chassis.HELICOPTER;
    }

    /** How much of the top speed {@code gear} reaches at the redline; the overdrive's is past the end. */
    private float topOf(int gear) {
        int count = this.gearCount();
        java.util.List<Float> tops = this.gearTops();
        return gear > count ? tops.get(count - 1) * OVERDRIVE : tops.get(Mth.clamp(gear, 1, count) - 1);
    }

    /**
     * How hard a gear reaching {@code top} of the top speed pulls. Read off
     * the five-speed's own gears, so a car with that box drives as it always
     * has; a shorter first than its first, or a longer top, carries on the
     * same way.
     */
    static float pullOf(float top) {
        java.util.List<Float> five = me.trossty.car.vehicle.VehicleDefinition.Performance.FIVE_SPEED;
        if (top <= five.get(0)) {
            return FIVE_SPEED_PULL[0] * (float) Math.pow(top / five.get(0), -PULL_SLOPE);
        }
        for (int i = 1; i < five.size(); i++) {
            if (top <= five.get(i)) {
                // Between two of them, evenly in the logarithms, as gearing goes.
                float along = (float) (Math.log(top / five.get(i - 1)) / Math.log(five.get(i) / five.get(i - 1)));
                return (float) Math.exp(Mth.lerp(along,
                        Math.log(FIVE_SPEED_PULL[i - 1]), Math.log(FIVE_SPEED_PULL[i])));
            }
        }
        return FIVE_SPEED_PULL[FIVE_SPEED_PULL.length - 1];
    }

    /** How far through a gear change the box is: 1 as it starts, 0 when it is done or not changing. */
    public float getShift() {
        return (float) this.shiftTicks / SHIFT_TICKS;
    }

    /**
     * How hard the engine can push right now, against the plain throttle
     * step: what the gear gives, times where the engine is on its torque
     * curve. A boat's or a plane's propeller takes what it is given.
     */
    float gearPull() {
        if (!this.car.hasGearbox()) {
            return this.car.isElectric()
                    ? DIRECT_PULL * CarSetup.torque(this.car.getLayout(), CarSetup.Fuel.ELECTRIC, this.revsIn(1))
                    : DIRECT_PULL;
        }
        int gear = this.forwardGear();
        float pull = this.gearCount() == 1 ? DIRECT_PULL : pullOf(this.topOf(gear));
        pull *= CarSetup.torque(this.car.getLayout(), this.car.getEngineFuel(), this.revsIn(gear));
        return this.shiftTicks > 0 ? pull * SHIFT_DRIVE : pull;
    }

    /** Revs in {@code gear} as a share of the redline. */
    private float revsIn(int gear) {
        return this.rpmIn(gear) / this.getRedlineRpm();
    }

    /** Engine revs in {@code gear} at the speed the car is doing. */
    private float rpmIn(int gear) {
        float fraction = Math.abs(this.car.getEngineSpeed()) / Math.max(0.01F, this.car.getRatedTopSpeed());
        float redline = this.getRedlineRpm();
        float top = this.car.hasGearbox() ? this.topOf(gear) : 1.0F;
        float idle = this.getIdleRpm();
        return Mth.clamp(idle + (redline - idle) * fraction / top, idle, redline * 1.04F);
    }

    /**
     * Changes up near the redline and down when the revs sag, like an
     * automatic, and takes a moment over each change; then runs the engine.
     * Run on every side from the speed that side sees, so the driver's
     * needle, their engine note and a bystander's all change gear together.
     */
    void tick() {
        this.spec = this.car.engineSpec();
        if (this.shiftTicks > 0) {
            this.shiftTicks--;
        } else {
            this.chooseGear();
        }
        this.runEngine();
    }

    private void chooseGear() {
        if (!this.car.isEngineRunning() || this.car.getEngineSpeed() < -0.005F || !this.car.hasGearbox()) {
            this.driveGear = 1;
            this.cruiseTicks = 0;
            return;
        }
        this.driveGear = this.forwardGear();
        int top = this.gearCount();
        boolean pedal = this.car.throttlePedal();
        float pace = Math.abs(this.car.getEngineSpeed()) / Math.max(0.01F, this.car.getMaxSpeed());
        this.cruiseTicks = pedal && pace >= CRUISE_PACE ? this.cruiseTicks + 1 : 0;
        float revs = this.revsIn(this.driveGear);
        if (this.driveGear > top) {
            // Out of overdrive: kicked down for the pull, or slowed until it would lug.
            if (pedal && pace < KICKDOWN_PACE || revs < DOWNSHIFT) {
                this.changeGear(top);
            }
            return;
        }
        if (revs >= UPSHIFT && this.driveGear < top) {
            this.changeGear(this.driveGear + 1);
        } else if (this.driveGear == top && this.cruiseTicks >= CRUISE_TICKS && this.hasOverdrive()) {
            this.changeGear(top + 1);
        } else if (this.driveGear > 1 && this.rpmIn(this.driveGear - 1) / this.getRedlineRpm() < UPSHIFT - 0.1F
                && revs < DOWNSHIFT) {
            this.changeGear(this.driveGear - 1);
        }
    }

    private void changeGear(int gear) {
        this.upshifting = gear > this.driveGear;
        this.driveGear = gear;
        this.shiftTicks = SHIFT_TICKS;
    }

    /**
     * One tick of the engine. The wheels set the revs the clutch would hold
     * it to in the gear it is in; the engine gets there as an engine would.
     */
    private void runEngine() {
        float redline = this.getRedlineRpm();
        if (!this.car.isEngineRunning()) {
            // Key off: it runs down and the turbo spins down; nothing pushes.
            this.throttle = 0.0F;
            this.rpm = Math.max(0.0F, this.rpm - FREE_FALL * redline);
            this.turboSpeed -= this.turboSpeed * SPOOL_DOWN;
            return;
        }
        float idle = this.getIdleRpm();
        // Cruising in overdrive the driver holds only what the road takes.
        float pedal = !this.car.throttlePedal() ? 0.0F
                : this.forwardGear() > this.gearCount() ? CRUISE_THROTTLE : 1.0F;
        float wheels = this.rpmIn(this.getGear() < 0 ? 1 : this.forwardGear());
        float shift = this.getShift();
        if (shift > 0.0F && this.upshifting) {
            // Changing up: throttle cut, clutch out, and the revs fall by
            // themselves until they meet the next gear's.
            this.throttle = 0.0F;
            this.rpm = Math.max(wheels, this.rpm - FREE_FALL * redline);
        } else if (shift > 0.0F) {
            // Changing down: the clutch is out and a stab of throttle spins
            // the engine up to what the lower gear will turn it at.
            this.throttle = shift > 0.5F ? 1.0F : pedal;
            float match = Math.min(wheels * BLIP_MATCH, redline);
            this.rpm = this.rpm < match
                    ? Math.min(match, this.rpm + FREE_RISE * redline)
                    : Math.max(match, this.rpm - FREE_FALL * redline);
        } else {
            this.throttle = pedal > this.throttle ? Math.min(pedal, this.throttle + THROTTLE_OPEN) : pedal;
            // In gear the engine turns with the wheels, but pulling away the
            // converter lets it flare above them. A motor has no converter.
            float flare = this.car.isElectric() ? idle : idle + STALL * (redline - idle) * this.throttle;
            this.rpm += (Math.max(wheels, flare) - this.rpm) * BITE;
        }
        this.rpm = Mth.clamp(this.rpm, 0.0F, redline * 1.04F);
        // The exhaust drives the turbine: harder the more gas goes through,
        // which is the throttle and the revs. The shaft takes a moment to
        // spool, and a while to spin down.
        float exhaust = this.car.hasTurbo()
                ? this.throttle * Mth.clamp((this.rpm - idle) / (BOOST_REVS * (redline - idle)), 0.0F, 1.0F)
                : 0.0F;
        this.turboSpeed += (exhaust - this.turboSpeed) * (exhaust > this.turboSpeed ? SPOOL_UP : SPOOL_DOWN);
    }

    /**
     * Engine revs. They climb through each gear to the change, fall as the
     * throttle is cut for the next — quickly, but not in one frame — and
     * climb again; a change down blips them up. Reverse is geared like first.
     */
    public float getEngineRpm() {
        return this.getGear() == 0 ? 0.0F : this.rpm;
    }

    /** How far open the throttle is, 0 to 1: the pedal, less the cut of a change up, plus a change down's blip. */
    public float getThrottle() {
        return this.throttle;
    }

    /** How fast the turbo shaft spins, 0 to 1 of flat out; always 0 without a turbo once it has spun down. */
    public float getTurboSpeed() {
        return this.turboSpeed;
    }

    /**
     * Boost, 0 to 1 of the most the turbo makes: the compressor's pressure
     * goes with the square of the shaft's speed, and is only held while the
     * throttle is open. Shut it and the blow-off valve lets it go.
     */
    public float getBoost() {
        return this.turboSpeed * this.turboSpeed * this.throttle;
    }
}
