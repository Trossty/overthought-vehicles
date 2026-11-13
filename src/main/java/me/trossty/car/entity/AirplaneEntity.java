package me.trossty.car.entity;

import me.trossty.car.registry.ModItems;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

/**
 * A light aircraft. On the ground it is the car — it taxies on its three
 * wheels, steers with the nose wheel and stops on its brakes. Fast enough
 * down the runway and the brake key pulls back on the yoke instead: it lifts
 * off. In the air the throttle holds its speed, the brake key climbs, back
 * puts the nose down; let the speed bleed away and the wings stop holding
 * it up, and it sinks. Come down too steeply, or stall a long way up, and the
 * landing is a crash like any other fall.
 *
 * <p>Slow down too far and the wing stalls: it stops carrying the aircraft
 * all at once rather than a little at a time, the nose drops, one wing
 * goes first and it starts to turn about itself. Pulling back only holds it
 * in the stall. Out of it the way a pilot is taught: nose down to get the
 * air flowing over the wing again, and opposite rudder against the turn.
 * A horn sounds in the cockpit while the speed is getting near it, and
 * banking into a turn brings the stall on sooner.
 *
 * <p>The pitch and bank are drawn, not simulated: the climb and the turn
 * are worked out from what it actually does.
 */
public class AirplaneEntity extends CarEntity {

    /** Share of the top speed at which the wings hold the aircraft up. */
    private static final float TAKEOFF_SHARE = 0.55F;
    /** Climb rate with the yoke back, blocks a tick. */
    private static final double CLIMB_RATE = 0.12;
    /** Descent rate with the nose pushed down, blocks a tick. */
    private static final double DIVE_RATE = 0.18;
    /** How fast it sinks once the wings stop flying, blocks a tick at a standstill. */
    private static final double STALL_SINK = 0.45;
    /** Lift share below which the wing stalls, flying straight. */
    private static final float STALL_LIFT = 0.92F;
    /** How much sooner it stalls banked into a turn, against straight and level. */
    private static final float TURN_STALL = 1.08F;
    /** Lift share it needs back before the wing flies again. */
    private static final float RECOVER_LIFT = 1.05F;
    /** Lift share under which the stall horn sounds. */
    private static final float WARN_LIFT = 1.12F;
    /** How fast it falls stalled, blocks a tick, and how quickly it gets there. */
    private static final double STALL_DROP = 0.42;
    private static final double STALL_RESPONSE = 0.2;
    /** Speed gained each tick falling stalled, and the extra with the nose pushed down. */
    private static final float STALL_GAIN = 0.004F;
    private static final float NOSE_DOWN_GAIN = 0.006F;
    /** Speed a climb costs each tick with the throttle open, and with it closed. */
    private static final float CLIMB_COST = 0.0015F;
    private static final float GLIDE_CLIMB_COST = 0.005F;
    /** How fast a stalled wing turns it about itself, degrees a tick. */
    private static final float SPIN_RATE = 3.5F;
    /** Faster than this down, the wings are not what is holding it: a fall, blocks a tick. */
    private static final double HARD_SINK = 0.3;
    /** How quickly the vertical speed follows the controls each tick. */
    private static final double RESPONSE = 0.12;
    /** Speed bled off each tick with the throttle closed. */
    private static final float GLIDE_DRAG = 0.996F;
    /** Steepest pitch drawn, degrees. */
    private static final float MAX_PITCH = 25.0F;
    /** Bank per degree of turn a tick. */
    private static final float BANK_PER_TURN = 6.0F;
    private static final float MAX_BANK = 35.0F;

    private boolean stalled;
    /** Which way a stall turns it: the wing that went first. */
    private float spinDirection = 1.0F;
    private float pitch;
    private float prevPitch;
    private float bank;
    private float prevBank;

    public AirplaneEntity(EntityType<?> type, Level world) {
        super(type, world);
    }

    @Override
    protected int plainSeatCount() {
        return 4;
    }

    @Override
    protected Item getDropItem() {
        return ModItems.AIRPLANE;
    }

    @Override
    public me.trossty.car.vehicle.VehicleDefinition.Chassis chassis() {
        return me.trossty.car.vehicle.VehicleDefinition.Chassis.PLANE;
    }

    /**
     * A light aircraft's propeller is bolted to the crankshaft and its engine
     * is cooled by the air: no gearbox, clutch, shaft or radiator.
     */
    @Override
    public boolean hasPart(CarPart part) {
        return super.hasPart(part) && part != CarPart.TRANSMISSION && part != CarPart.CLUTCH
                && part != CarPart.DRIVESHAFT && part != CarPart.RADIATOR;
    }

    @Override
    public boolean hasGearbox() {
        return false;
    }

    /** A propeller only pulls: backing up is for a tug. */
    @Override
    protected float maxReverseSpeed() {
        return 0.0F;
    }

    /** Three wheels and a nose that steers: it does not slide its tail out. */
    @Override
    protected boolean canDrift() {
        return false;
    }

    @Override
    protected boolean underway() {
        return this.onGround() || this.isFlying();
    }

    /** Navigation lights burn steady; there are no brake lamps on a wing. */
    @Override
    public boolean areBrakeLampsOn() {
        return false;
    }

    /** Speed at which the wings carry it, blocks a tick. */
    public float takeoffSpeed() {
        return TAKEOFF_SHARE * this.tuning().speed() * MAX_SPEED;
    }

    /** How much of its weight the wings are carrying: 1 and over, it flies. */
    public float lift() {
        return Math.abs(this.getEngineSpeed()) / this.takeoffSpeed();
    }

    @Override
    protected boolean isFlying() {
        if (this.getControllingPassenger() == null) {
            return false;
        }
        return !this.onGround() || this.pressingBrake && this.lift() >= 1.0F;
    }

    /** True while the wing is stalled: as worked out by whoever is flying it. */
    public boolean isStalled() {
        return this.stalled;
    }

    /** True while the stall horn should sound: in the air, slow and getting slower to the stall. */
    public boolean stallWarning() {
        return this.getControllingPassenger() != null && !this.onGround()
                && (this.stalled || this.lift() < WARN_LIFT);
    }

    /** The lift share it stalls under: higher banked into a turn, where the wing carries more. */
    private float stallLift() {
        return this.pressingLeft != this.pressingRight ? STALL_LIFT * TURN_STALL : STALL_LIFT;
    }

    /**
     * Whether the wing is stalled, worked out each tick by the side flying
     * it, and the turn a stalled wing puts in: the rudder against it stops
     * it, the rudder with it makes it worse.
     */
    private void updateStall() {
        if (this.onGround() || !this.isFlying()) {
            this.stalled = false;
            return;
        }
        float lift = this.lift();
        if (!this.stalled && lift < this.stallLift()) {
            this.stalled = true;
            // The wing that goes first: the low one in a turn, either one straight.
            this.spinDirection = this.pressingLeft != this.pressingRight
                    ? (this.pressingLeft ? -1.0F : 1.0F)
                    : (this.random.nextBoolean() ? 1.0F : -1.0F);
        } else if (this.stalled && lift >= RECOVER_LIFT) {
            this.stalled = false;
        }
        if (this.stalled) {
            float rudder = (this.pressingRight ? 1.0F : 0.0F) - (this.pressingLeft ? 1.0F : 0.0F);
            // Steering already turns it by the rudder's own amount; this is
            // the stall's turn on top, which opposite rudder takes out.
            float spin = SPIN_RATE * (rudder == -this.spinDirection ? 0.0F : rudder == this.spinDirection ? 1.5F : 1.0F);
            this.setYRot(Mth.wrapDegrees(this.getYRot() + this.spinDirection * spin));
        }
    }

    @Override
    protected float airspeed(float speed, boolean throttle) {
        if (throttle) {
            speed = Math.min(speed + this.throttleStep() * 0.5F, this.getMaxSpeed());
        } else {
            speed *= GLIDE_DRAG;
        }
        if (this.stalled) {
            // Falling gives back the speed the wing needs, faster nose down;
            // holding the yoke back keeps it stalled.
            speed += STALL_GAIN;
            if (this.pressingBack) {
                speed += NOSE_DOWN_GAIN;
            } else if (this.pressingBrake) {
                speed -= CLIMB_COST;
            }
        } else if (this.pressingBrake) {
            // Climbing trades speed for height, and without power, fast...
            speed -= throttle ? CLIMB_COST : GLIDE_CLIMB_COST;
        } else if (this.pressingBack) {
            // ...and diving the other way round.
            speed += 0.003F;
        }
        return Mth.clamp(speed, 0.0F, MAX_SPEED * 1.8F);
    }

    /** Flying, it goes where its nose points. */
    @Override
    protected boolean hasGrip(boolean onGround) {
        return onGround || this.isFlying();
    }

    @Override
    protected double verticalMotion(boolean onGround) {
        if (!this.isFlying()) {
            return super.verticalMotion(onGround);
        }
        if (this.stalled) {
            double drop = this.pressingBack ? STALL_DROP + 0.08 : STALL_DROP;
            return Mth.lerp(STALL_RESPONSE, this.getDeltaMovement().y, -drop);
        }
        float lift = this.lift();
        double target;
        if (lift >= 1.0F) {
            target = this.pressingBrake ? CLIMB_RATE : this.pressingBack ? -DIVE_RATE : 0.0;
        } else {
            // Just under flying speed the wings sink it a little at a time,
            // until the stall takes them altogether.
            target = -STALL_SINK * (1.0F - lift) - 0.02;
        }
        return Mth.lerp(RESPONSE, this.getDeltaMovement().y, target);
    }

    @Override
    public void tick() {
        if (this.isControlledByLocalInstance()) {
            this.updateStall();
        } else if (this.getControllingPassenger() == null || this.onGround()) {
            this.stalled = false;
        }
        double yBefore = this.getY();
        float yawBefore = this.getYRot();
        super.tick();
        // A glide is not a fall: only coming down without the wings counts,
        // and a stalled wing coming down that fast is not holding anything.
        if (!this.onGround() && this.lift() >= 0.8F && this.getY() - yBefore > -HARD_SINK) {
            this.resetFallDistance();
        }
        if (this.level().isClientSide() && this.isControlledByLocalInstance()
                && this.stallWarning() && this.tickCount % 4 == 0) {
            // The stall horn: a reed in the wing root that wails as the air slows.
            this.level().playLocalSound(this.getX(), this.getY(), this.getZ(),
                    net.minecraft.sounds.SoundEvents.NOTE_BLOCK_FLUTE.value(),
                    net.minecraft.sounds.SoundSource.NEUTRAL, 0.6F, this.stalled ? 1.6F : 2.0F, false);
        }
        if (this.level().isClientSide()) {
            this.prevPitch = this.pitch;
            this.prevBank = this.bank;
            float climb = 0.0F;
            float turn = 0.0F;
            if (!this.onGround()) {
                double speed = Math.max(0.05, Math.abs(this.getEngineSpeed()));
                climb = (float) Math.toDegrees(Math.atan2(this.getY() - yBefore, speed));
                turn = Mth.wrapDegrees(this.getYRot() - yawBefore);
            }
            this.pitch += (Mth.clamp(climb, -MAX_PITCH, MAX_PITCH) - this.pitch) * 0.2F;
            this.bank += (Mth.clamp(turn * BANK_PER_TURN, -MAX_BANK, MAX_BANK) - this.bank) * 0.15F;
        }
    }

    /** Nose up, degrees, as drawn. */
    @Override
    public float getBodyPitch(float tickProgress) {
        return Mth.lerp(tickProgress, this.prevPitch, this.pitch);
    }

    /** Into the turn, degrees, as drawn. */
    @Override
    public float getBodyRoll(float tickProgress) {
        return Mth.lerp(tickProgress, this.prevBank, this.bank);
    }

    /** The engine is in the nose, and so is its exhaust: under the cowling, on the right. */
    @Override
    protected Vec3 exhaustOffset() {
        me.trossty.car.vehicle.VehicleDefinition defined = this.getDefinition();
        float front = 2.5F * 16.0F;
        if (defined != null) {
            front = -Float.MAX_VALUE;
            for (me.trossty.car.vehicle.VehicleDefinition.Box box : defined.shape()) {
                front = Math.max(front, Math.max(box.from().z(), box.to().z()));
            }
        }
        return new Vec3(-0.45, 0.95, front / 16.0 - 0.8);
    }

    /** The wings reach far past the body it collides with: draw it whenever any of it shows. */
    @Override
    public AABB getBoundingBoxForCulling() {
        return this.getBoundingBox().inflate(6.0, 1.5, 6.0);
    }
}
