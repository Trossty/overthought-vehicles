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
    /** How quickly the vertical speed follows the controls each tick. */
    private static final double RESPONSE = 0.12;
    /** Speed bled off each tick with the throttle closed. */
    private static final float GLIDE_DRAG = 0.996F;
    /** Steepest pitch drawn, degrees. */
    private static final float MAX_PITCH = 25.0F;
    /** Bank per degree of turn a tick. */
    private static final float BANK_PER_TURN = 6.0F;
    private static final float MAX_BANK = 35.0F;

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

    @Override
    protected float airspeed(float speed, boolean throttle) {
        float lift = this.lift();
        if (throttle) {
            speed = Math.min(speed + this.throttleStep() * 0.5F, this.getMaxSpeed());
        } else {
            speed *= GLIDE_DRAG;
        }
        if (lift >= 1.0F && this.pressingBrake) {
            // Climbing trades speed for height...
            speed -= 0.0015F;
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
        float lift = this.lift();
        double target;
        if (lift >= 1.0F) {
            target = this.pressingBrake ? CLIMB_RATE : this.pressingBack ? -DIVE_RATE : 0.0;
        } else {
            // Below flying speed the wings give up a little at a time.
            target = -STALL_SINK * (1.0F - lift) - 0.02;
        }
        return Mth.lerp(RESPONSE, this.getDeltaMovement().y, target);
    }

    @Override
    public void tick() {
        double yBefore = this.getY();
        float yawBefore = this.getYRot();
        super.tick();
        // A glide is not a fall: only coming down without the wings counts.
        if (!this.onGround() && this.lift() >= 0.8F) {
            this.resetFallDistance();
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
