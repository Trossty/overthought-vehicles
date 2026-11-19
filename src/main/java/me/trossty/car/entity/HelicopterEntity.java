package me.trossty.car.entity;

import me.trossty.car.registry.ModItems;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

/**
 * A light helicopter. It sits on its skids and goes nowhere on the ground:
 * the jump key pulls up the collective and it lifts straight off. In the air
 * forward and back tip the rotor and it flies that way, left and right turn
 * it on the spot with the tail rotor, and the jump key climbs. Let go of it
 * and it settles slowly down, gently enough to land on whatever is under it.
 *
 * <p>Lose the engine in the air and the rotor keeps turning in the wind
 * coming up through it: it comes down faster, but under control, and an
 * autorotation onto flat ground only bends the skids a little. Come down
 * too hard or too fast and the landing is a crash like any other.
 *
 * <p>Its nose dips as it gathers speed and it banks into its turns. Both
 * are worked out from what every side already knows, the speed and the turn
 * it makes, so everyone sees it fly the same.
 */
public class HelicopterEntity extends CarEntity {

    /** Climb with the collective up, blocks a tick. */
    private static final double CLIMB_RATE = 0.22;
    /** How fast it settles with the collective down, blocks a tick: a landing, not a fall. */
    private static final double SETTLE_RATE = 0.07;
    /** How fast it comes down with no engine, the rotor windmilling, blocks a tick. */
    private static final double AUTOROTATE_SINK = 0.28;
    /** Faster than this down, the rotor is not what is holding it: a fall. */
    private static final double HARD_SINK = 0.36;
    /** How quickly the vertical speed follows the collective each tick. */
    private static final double RESPONSE = 0.12;
    /** Speed kept each tick in the hover with nothing pressed. */
    private static final float HOVER_DRAG = 0.95F;
    /** Share of the throttle step a tipped rotor adds each tick. */
    private static final float TIP_PULL = 0.6F;
    /** Backwards is slow: share of top speed. */
    private static final float BACK_SHARE = 0.35F;
    /** How fast the pedals turn it, degrees a tick, with power and without. */
    private static final float YAW_RATE = 3.2F;
    private static final float WINDMILL_YAW = 1.2F;
    /** Skids on the ground: share of the speed kept each tick. */
    private static final float SKID_DRAG = 0.6F;
    /** Slower than this on the skids, it has stopped. */
    private static final float SKID_STOP = 0.08F;
    /** Sink at touchdown the skids take without harm, blocks a tick. */
    private static final double SAFE_SINK = 0.3;
    private static final float SINK_DAMAGE = 120.0F;
    /** Speed along the ground at touchdown past which it is flying into it. */
    private static final float SAFE_TOUCHDOWN_SPEED = 0.3F;
    private static final float FAST_TOUCHDOWN_DAMAGE = 90.0F;
    /** Nose down at full speed, degrees, and the steepest bank it is drawn in. */
    private static final float FULL_SPEED_PITCH = 10.0F;
    private static final float MAX_BANK = 18.0F;
    /** Bank for each degree a tick it turns. */
    private static final float BANK_PER_TURN = 4.0F;
    /** A light airframe against a car's steel. */
    private static final float FRAGILITY = 2.5F;

    private float pitch;
    private float prevPitch;
    private float bank;
    private float prevBank;
    private float lastYaw = Float.NaN;
    private double lastTickY = Double.NaN;

    public HelicopterEntity(EntityType<?> type, Level world) {
        super(type, world);
    }

    @Override
    protected int plainSeatCount() {
        return 4;
    }

    @Override
    protected Item getDropItem() {
        return ModItems.HELICOPTER;
    }

    @Override
    public me.trossty.car.vehicle.VehicleDefinition.Chassis chassis() {
        return me.trossty.car.vehicle.VehicleDefinition.Chassis.HELICOPTER;
    }

    /**
     * An air-cooled piston engine turns the rotors through a belt clutch, the
     * main gearbox and a long shaft down the boom to the tail: those it has.
     * No radiator, and skids, not wheels, so nothing to brake, spring or steer.
     */
    @Override
    public boolean hasPart(CarPart part) {
        return super.hasPart(part) && part != CarPart.RADIATOR && part != CarPart.WHEELS
                && part != CarPart.BRAKE_PADS && part != CarPart.BRAKE_DISCS && part != CarPart.SUSPENSION
                && part != CarPart.STEERING_RACK;
    }

    @Override
    public boolean hasGearbox() {
        return false;
    }

    @Override
    protected boolean canDrift() {
        return false;
    }

    @Override
    protected boolean underway() {
        return this.onGround() || this.isFlying();
    }

    @Override
    public boolean areBrakeLampsOn() {
        return false;
    }

    private boolean powered() {
        return this.hasFuel() && this.canRun();
    }

    /** Off its skids with somebody at the controls, or lifting off them. */
    @Override
    protected boolean isFlying() {
        if (this.getControllingPassenger() == null) {
            return false;
        }
        return !this.onGround() || this.pressingBrake && this.powered();
    }

    /** Flying, it goes where its nose points. */
    @Override
    protected boolean hasGrip(boolean onGround) {
        return onGround || this.isFlying();
    }

    @Override
    protected float airspeed(float speed, boolean throttle) {
        boolean power = this.powered();
        float max = this.getMaxSpeed();
        if (power && this.pressingForward) {
            speed = Math.min(speed + this.throttleStep() * TIP_PULL, max);
        } else if (power && this.pressingBack) {
            speed = Math.max(speed - this.throttleStep() * TIP_PULL, -max * BACK_SHARE);
        } else {
            speed *= HOVER_DRAG;
        }
        return speed;
    }

    /**
     * On its skids it only slides to a stop: there is nothing to drive it
     * along the ground, so the little a tick of throttle would add is gone
     * before it moves it.
     */
    @Override
    protected float limitSpeed(float speed) {
        if (this.onGround() && !this.isFlying()) {
            return Math.abs(speed) < SKID_STOP ? 0.0F : speed * SKID_DRAG;
        }
        return super.limitSpeed(speed);
    }

    /** The pedals turn it on the spot in the air, whatever its speed; on the ground, not at all. */
    @Override
    protected float turnRate(float steerInput, boolean drifting) {
        if (!this.isFlying() || this.onGround()) {
            return 0.0F;
        }
        return steerInput * (this.powered() ? YAW_RATE : WINDMILL_YAW) * this.steeringFactor();
    }

    @Override
    protected double verticalMotion(boolean onGround) {
        boolean power = this.powered();
        if (this.getControllingPassenger() == null || onGround && !(this.pressingBrake && power)) {
            return super.verticalMotion(onGround);
        }
        double target = !power ? -AUTOROTATE_SINK
                : this.pressingBrake ? CLIMB_RATE
                : -SETTLE_RATE;
        return Mth.lerp(RESPONSE, this.getDeltaMovement().y, target);
    }

    @Override
    protected float crashFragility() {
        return FRAGILITY;
    }

    /** Down too hard, or flown into the ground: a crash. A gentle arrival is a landing. */
    private void checkTouchdown(double sink, float speed) {
        if (this.damage().recentlyCrashed()) {
            return;
        }
        float tooHard = (float) Math.max(0.0, sink - SAFE_SINK) * SINK_DAMAGE;
        float tooFast = Math.max(0.0F, Math.abs(speed) - SAFE_TOUCHDOWN_SPEED) * FAST_TOUCHDOWN_DAMAGE;
        this.damage().reportImpact(tooHard + tooFast);
    }

    /** A crashed helicopter goes up: its fuel catches. */
    @Override
    protected void wreckEffects(net.minecraft.server.level.ServerLevel world) {
        world.playSound(null, this.getX(), this.getY() + 0.5, this.getZ(),
                net.minecraft.sounds.SoundEvents.GENERIC_EXPLODE.value(),
                net.minecraft.sounds.SoundSource.NEUTRAL, 1.2F, 0.9F);
        world.sendParticles(net.minecraft.core.particles.ParticleTypes.EXPLOSION_EMITTER,
                this.getX(), this.getY() + 0.8, this.getZ(), 1, 0.0, 0.0, 0.0, 0.0);
        world.sendParticles(net.minecraft.core.particles.ParticleTypes.FLAME,
                this.getX(), this.getY() + 0.6, this.getZ(), 50, 1.2, 0.4, 1.2, 0.03);
        world.sendParticles(net.minecraft.core.particles.ParticleTypes.CAMPFIRE_COSY_SMOKE,
                this.getX(), this.getY() + 1.0, this.getZ(), 20, 1.0, 0.5, 1.0, 0.02);
    }

    @Override
    public void tick() {
        boolean airborne = !this.onGround() && this.getControllingPassenger() != null;
        double sink = -this.getDeltaMovement().y;
        float speed = this.getEngineSpeed();
        super.tick();
        if (airborne && this.onGround() && this.isControlledByLocalInstance()) {
            this.checkTouchdown(sink, speed);
        }
        // Coming down on the rotor is not a fall: only coming down faster than
        // the rotor ever lets it is.
        double sinceLastTick = Double.isNaN(this.lastTickY) ? 0.0 : this.getY() - this.lastTickY;
        this.lastTickY = this.getY();
        if (!this.onGround() && this.getControllingPassenger() != null && sinceLastTick > -HARD_SINK) {
            this.resetFallDistance();
        }
        if (!this.level().isClientSide() && this.isEngineRunning() && !this.onGround() && this.tickCount % 3 == 0) {
            // The downwash: dust and spray kicked up off whatever is just below.
            this.downwash();
        }

        // Drawn from what every side knows: the speed, and the turn it makes.
        this.prevPitch = this.pitch;
        this.prevBank = this.bank;
        float max = Math.max(0.05F, this.getMaxSpeed());
        float tipped = this.onGround() ? 0.0F : -FULL_SPEED_PITCH * Mth.clamp(this.getEngineSpeed() / max, -1.0F, 1.0F);
        this.pitch += (tipped - this.pitch) * 0.15F;
        float turn = Float.isNaN(this.lastYaw) ? 0.0F : Mth.wrapDegrees(this.getYRot() - this.lastYaw);
        this.lastYaw = this.getYRot();
        float into = this.onGround() ? 0.0F
                : Mth.clamp(turn * BANK_PER_TURN * (0.3F + 0.7F * Math.min(1.0F, Math.abs(this.getEngineSpeed()) / max)),
                        -MAX_BANK, MAX_BANK);
        this.bank += (into - this.bank) * 0.2F;
    }

    /** A low hover blows up the ground under it. */
    private void downwash() {
        if (!(this.level() instanceof net.minecraft.server.level.ServerLevel world)) {
            return;
        }
        net.minecraft.core.BlockPos.MutableBlockPos at = this.blockPosition().mutable();
        for (int down = 0; down < 6; down++) {
            at.move(net.minecraft.core.Direction.DOWN);
            net.minecraft.world.level.block.state.BlockState ground = world.getBlockState(at);
            if (!ground.isAir()) {
                double y = at.getY() + 1.05;
                if (!world.getFluidState(at).isEmpty()) {
                    world.sendParticles(net.minecraft.core.particles.ParticleTypes.SPLASH,
                            this.getX(), y, this.getZ(), 12, 2.5, 0.05, 2.5, 0.1);
                } else {
                    world.sendParticles(new net.minecraft.core.particles.BlockParticleOption(
                                    net.minecraft.core.particles.ParticleTypes.BLOCK, ground),
                            this.getX(), y, this.getZ(), 6, 2.5, 0.05, 2.5, 0.05);
                }
                return;
            }
        }
    }

    /** Nose up, degrees, as drawn: down as it flies forward. */
    @Override
    public float getBodyPitch(float tickProgress) {
        return Mth.lerp(tickProgress, this.prevPitch, this.pitch);
    }

    /** Into the turn, right side down positive, degrees, as drawn. */
    @Override
    public float getBodyRoll(float tickProgress) {
        return Mth.lerp(tickProgress, this.prevBank, this.bank);
    }

    /** The exhaust comes out low on the right, behind the engine bay. */
    @Override
    protected Vec3 exhaustOffset() {
        return new Vec3(-0.53, 1.06, -1.72);
    }

    /** The rotor reaches far past the cabin it collides with: draw it whenever any of it shows. */
    @Override
    public AABB getBoundingBoxForCulling() {
        return this.getBoundingBox().inflate(5.5, 1.5, 5.5);
    }
}
