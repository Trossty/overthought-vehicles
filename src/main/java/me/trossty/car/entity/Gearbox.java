package me.trossty.car.entity;

import net.minecraft.util.Mth;
import java.util.List;

/**
 * The automatic gearbox and the revs: the vehicle's own gears from its
 * definition, how hard each pulls through the engine's torque curve, when it
 * changes up and down, and what the rev counter reads on the way.
 *
 * <p>Each side runs it from the speed it sees, so the driver's needle, their
 * engine note and a bystander's all change gear together. Whether there is a
 * box at all is the vehicle's to say ({@link CarEntity#hasGearbox}).
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

    private static final float IDLE_RPM = 850.0F;

    /** The pull of a single fixed ratio: about what the five gears give over a run to top speed. */
    private static final float DIRECT_PULL = 1.0F;

    /** The forward gear the box is in, 1 to {@link #gearCount()}. Each side keeps its own. */
    private int driveGear = 1;

    private int shiftFromGear = 1;

    private int shiftTicks;

    /** Where the rev counter's red band starts: a diesel runs out of breath long before a petrol. */
    public float getRedlineRpm() {
        float redline = this.car.getEngineFuel().needsSpark() ? 7000.0F : 4500.0F;
        return this.car.getLayout() == CarSetup.Layout.V8 ? redline - 500.0F : redline;
    }

    /** The gear in, 1 to {@link #gearCount()}; -1 in reverse, 0 when the engine is off. */
    public int getGear() {
        if (!this.car.isEngineRunning()) {
            return 0;
        }
        return this.car.getEngineSpeed() < -0.005F ? -1 : this.forwardGear();
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

    /** The gear the box is in, kept inside the box it has now: a reload can hand a car a shorter one. */
    private int forwardGear() {
        return Mth.clamp(this.driveGear, 1, this.gearCount());
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
        float pull = this.gearCount() == 1 ? DIRECT_PULL : pullOf(this.gearTops().get(gear - 1));
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
        float top = this.car.hasGearbox() ? this.gearTops().get(Mth.clamp(gear, 1, this.gearCount()) - 1) : 1.0F;
        return Mth.clamp(IDLE_RPM + (redline - IDLE_RPM) * fraction / top,
                IDLE_RPM, redline * 1.04F);
    }

    /**
     * Changes up near the redline and down when the revs sag, like an
     * automatic, and takes a moment over each change. Run on every side from
     * the speed that side sees, so the driver's needle, their engine note and
     * a bystander's all change gear together.
     */
    void tick() {
        if (this.shiftTicks > 0) {
            this.shiftTicks--;
            return;
        }
        if (!this.car.isEngineRunning() || this.car.getEngineSpeed() < -0.005F || !this.car.hasGearbox()) {
            this.driveGear = 1;
            return;
        }
        this.driveGear = this.forwardGear();
        float revs = this.revsIn(this.driveGear);
        if (revs >= UPSHIFT && this.driveGear < this.gearCount()) {
            this.changeGear(this.driveGear + 1);
        } else if (this.driveGear > 1 && this.rpmIn(this.driveGear - 1) / this.getRedlineRpm() < UPSHIFT - 0.1F
                && revs < DOWNSHIFT) {
            this.changeGear(this.driveGear - 1);
        }
    }

    private void changeGear(int gear) {
        this.shiftFromGear = this.driveGear;
        this.driveGear = gear;
        this.shiftTicks = SHIFT_TICKS;
    }

    /**
     * Engine revs. They climb through each gear to the change, fall as the
     * next goes in — quickly, but not in one frame — and climb again; reverse
     * is geared like first.
     */
    public float getEngineRpm() {
        int gear = this.getGear();
        if (gear == 0) {
            return 0.0F;
        }
        if (gear < 0) {
            return this.rpmIn(1);
        }
        float rpm = this.rpmIn(gear);
        if (this.shiftTicks > 0) {
            // Over the first half of the change the revs swing to the new gear's.
            float left = Math.min(1.0F, (this.shiftTicks - SHIFT_TICKS / 2.0F) / (SHIFT_TICKS / 2.0F));
            rpm = Mth.lerp(Math.max(0.0F, left), rpm, this.rpmIn(this.shiftFromGear));
        }
        return rpm;
    }
}
