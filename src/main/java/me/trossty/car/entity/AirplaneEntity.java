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
 * puts the nose down. Close the throttle and it glides down at a safe speed,
 * the way a trimmed aircraft does; pull it up without power and the speed
 * bleeds away until the wings stop holding it up. Come down too steeply, or stall a long way up, and the
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
 * <p>In the air it turns by banking: the keys roll it over a little at a
 * time and the bank swings the nose round, the way a real one does. The
 * pitch is drawn from the climb it actually makes.
 *
 * <p>Arrive too steeply or too fast and the touchdown is a crash; a bad
 * one writes it off, and it goes up in flames.
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
    /** Speed bled off each tick with the throttle closed, down to the glide speed. */
    private static final float GLIDE_DRAG = 0.996F;
    /**
     * Share of the flying speed it settles at with the throttle closed and
     * hands off: trimmed, it lowers its nose and glides down rather than
     * slowing until it stalls. Well clear of the stall horn.
     */
    private static final float GLIDE_LIFT = 1.25F;
    /** Speed a trimmed glide gains back each tick below that, nose lowering. */
    private static final float GLIDE_RECOVER = 0.003F;
    /** How fast a trimmed glide comes down, blocks a tick: a landing, not a crash. */
    private static final double GLIDE_SINK = 0.07;
    /** Under full power a climb costs no speed below this share of the flying speed: it climbs slower instead. */
    private static final float POWERED_CLIMB_LIFT = 1.3F;
    /** Steepest pitch drawn, degrees. */
    private static final float MAX_PITCH = 25.0F;
    /** Steepest bank the ailerons hold, degrees. */
    private static final float MAX_BANK = 30.0F;
    /** How fast the ailerons roll it into a bank and out again, degrees a tick. */
    private static final float ROLL_RATE = 2.5F;
    /**
     * Turn a bank makes, degrees a tick: tan(bank) over the speed, as a real
     * coordinated turn, scaled so thirty degrees at cruise is a full circle
     * in about seven seconds.
     */
    private static final float BANK_TURN = 2.7F;
    /** Fastest it turns however slow and steep, degrees a tick. */
    private static final float MAX_AIR_TURN = 4.0F;
    /** Sink at touchdown a landing gear takes without harm, blocks a tick. */
    private static final double SAFE_SINK = 0.2;
    /** Damage for each block a tick of sink past that. */
    private static final float SINK_DAMAGE = 120.0F;
    /** Share of the flying speed past which touching down is flying into the ground. */
    private static final float SAFE_TOUCHDOWN_LIFT = 1.35F;
    /** Damage for each block a tick of speed past that. */
    private static final float FAST_TOUCHDOWN_DAMAGE = 90.0F;
    /** How far a stalled wing drops, degrees, and how fast. */
    private static final float STALL_BANK = 45.0F;
    private static final float STALL_ROLL_RATE = 3.5F;
    /** The bank flown, as the side flying it has it: what everyone else draws. */
    private static final net.minecraft.network.syncher.EntityDataAccessor<Float> DATA_BANK =
            net.minecraft.network.syncher.SynchedEntityData.defineId(AirplaneEntity.class,
                    net.minecraft.network.syncher.EntityDataSerializers.FLOAT);
    /** Off the ground at least this many ticks, it was flying, not bouncing. */
    private static final int FLOWN_TICKS = 20;
    /** A light airframe against a car's steel: how much more a crash costs it. */
    private static final float FRAGILITY = 3.0F;

    private boolean stalled;
    /** Which way a stall turns it: the wing that went first. */
    private float spinDirection = 1.0F;
    /** The bank the ailerons have put in, degrees, right wing down positive: the flying side's own. */
    private float flownBank;
    /** Ticks since its wheels last touched anything. */
    private int ticksAirborne;
    /** Height at the end of the last tick, for the climb drawn. */
    private double lastTickY = Double.NaN;
    private float pitch;
    private float prevPitch;
    private float bank;
    private float prevBank;

    public AirplaneEntity(EntityType<?> type, Level world) {
        super(type, world);
    }

    @Override
    protected void defineSynchedData(net.minecraft.network.syncher.SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(DATA_BANK, 0.0F);
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
        return Mth.lerp(Math.min(1.0F, Math.abs(this.flownBank) / MAX_BANK), STALL_LIFT, STALL_LIFT * TURN_STALL);
    }

    /**
     * In the air the keys work the ailerons, not a wheel: they roll it into
     * a bank a little at a time, and the bank is what turns it — slowly at
     * speed, tighter slow. Let go and it rolls back level and flies straight.
     * On the ground it steers on its nose wheel like anything else.
     */
    @Override
    protected float turnRate(float steerInput, boolean drifting) {
        boolean inAir = this.isFlying() && !this.onGround();
        if (inAir && this.stalled) {
            // The wing that stalled first drops, whatever the ailerons say;
            // the turn is the stall's own, worked out in updateStall.
            this.flownBank += Mth.clamp(this.spinDirection * STALL_BANK - this.flownBank,
                    -STALL_ROLL_RATE, STALL_ROLL_RATE);
            return 0.0F;
        }
        float target = inAir ? steerInput * MAX_BANK : 0.0F;
        this.flownBank += Mth.clamp(target - this.flownBank, -ROLL_RATE, ROLL_RATE);
        if (!inAir) {
            return super.turnRate(steerInput, drifting);
        }
        float speed = Math.max(Math.abs(this.getEngineSpeed()), 0.2F);
        float turn = BANK_TURN * (float) Math.tan(this.flownBank * Mth.DEG_TO_RAD) / speed;
        return Mth.clamp(turn, -MAX_AIR_TURN, MAX_AIR_TURN);
    }

    @Override
    protected float crashFragility() {
        return FRAGILITY;
    }

    /**
     * Touching down: a gentle arrival is a landing, coming down too steeply
     * or flying into the ground too fast is a crash, and a bad enough one is
     * the end of the aircraft.
     */
    private void checkTouchdown(double sink) {
        if (this.damage().recentlyCrashed()) {
            return;
        }
        float tooSteep = (float) Math.max(0.0, sink - SAFE_SINK) * SINK_DAMAGE;
        // Rolling fast down the runway over a dip is a bump, not flying into
        // the ground: only an arrival from the air is billed for its speed.
        float tooFast = this.ticksAirborne < FLOWN_TICKS ? 0.0F
                : Math.max(0.0F, Math.abs(this.getEngineSpeed()) - SAFE_TOUCHDOWN_LIFT * this.takeoffSpeed())
                        * FAST_TOUCHDOWN_DAMAGE;
        this.damage().reportImpact(tooSteep + tooFast);
    }

    /** A crashed aircraft goes up: its fuel catches. Nothing round it is blown apart. */
    @Override
    protected void wreckEffects(net.minecraft.server.level.ServerLevel world) {
        world.playSound(null, this.getX(), this.getY() + 0.5, this.getZ(),
                net.minecraft.sounds.SoundEvents.GENERIC_EXPLODE.value(),
                net.minecraft.sounds.SoundSource.NEUTRAL, 1.2F, 0.9F);
        world.sendParticles(net.minecraft.core.particles.ParticleTypes.EXPLOSION_EMITTER,
                this.getX(), this.getY() + 0.8, this.getZ(), 1, 0.0, 0.0, 0.0, 0.0);
        world.sendParticles(net.minecraft.core.particles.ParticleTypes.FLAME,
                this.getX(), this.getY() + 0.6, this.getZ(), 60, 1.6, 0.4, 1.6, 0.03);
        world.sendParticles(net.minecraft.core.particles.ParticleTypes.CAMPFIRE_COSY_SMOKE,
                this.getX(), this.getY() + 1.0, this.getZ(), 20, 1.2, 0.5, 1.2, 0.02);
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
            // The stalled wing drops and the aircraft turns about itself:
            // opposite rudder takes the turn out, rudder with it makes it worse.
            float spin = SPIN_RATE * (rudder == -this.spinDirection ? 0.0F : rudder == this.spinDirection ? 1.5F : 1.0F);
            this.setYRot(Mth.wrapDegrees(this.getYRot() + this.spinDirection * spin));
        }
    }

    @Override
    protected float airspeed(float speed, boolean throttle) {
        float glide = GLIDE_LIFT * this.takeoffSpeed();
        if (throttle) {
            speed = Math.min(speed + this.throttleStep() * 0.5F, this.getMaxSpeed());
        } else if (speed > glide) {
            speed = Math.max(glide, speed * GLIDE_DRAG);
        } else if (!this.stalled && !this.pressingBrake) {
            // Hands off, power off: the nose drops to keep the wing flying.
            speed = Math.min(glide, speed + GLIDE_RECOVER);
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
            // Climbing trades speed for height, and without power, fast; under
            // full power it only climbs as steeply as the engine can hold...
            if (!throttle) {
                speed -= GLIDE_CLIMB_COST;
            } else if (speed > POWERED_CLIMB_LIFT * this.takeoffSpeed()) {
                speed -= CLIMB_COST;
            }
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
        boolean power = this.pressingForward && this.hasFuel() && this.canRun();
        if (lift >= 1.0F) {
            target = this.pressingBrake ? CLIMB_RATE * Mth.clamp((lift - 1.0F) / (POWERED_CLIMB_LIFT - 1.0F), 0.35F, 1.0F)
                    : this.pressingBack ? -DIVE_RATE
                    : power ? 0.0
                    : -GLIDE_SINK;
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
        boolean airborne = !this.onGround() && this.getControllingPassenger() != null;
        double sink = -this.getDeltaMovement().y;
        super.tick();
        if (airborne && this.onGround() && this.isControlledByLocalInstance()) {
            this.checkTouchdown(sink);
        }
        this.ticksAirborne = this.onGround() ? 0 : this.ticksAirborne + 1;
        // A glide is not a fall: only coming down without the wings counts,
        // and a stalled wing coming down that fast is not holding anything.
        double sinceLastTick = Double.isNaN(this.lastTickY) ? 0.0 : this.getY() - this.lastTickY;
        if (!this.onGround() && this.lift() >= 0.8F && sinceLastTick > -HARD_SINK) {
            this.resetFallDistance();
        }
        if (this.level().isClientSide() && this.isControlledByLocalInstance()
                && this.stallWarning() && this.tickCount % 4 == 0) {
            // The stall horn: a reed in the wing root that wails as the air slows.
            this.level().playLocalSound(this.getX(), this.getY(), this.getZ(),
                    net.minecraft.sounds.SoundEvents.NOTE_BLOCK_FLUTE.value(),
                    net.minecraft.sounds.SoundSource.NEUTRAL, 0.6F, this.stalled ? 1.6F : 2.0F, false);
        }
        // Worked out on every side, the server included, so that it seats the
        // crew where their own screens show them. Measured tick to tick rather
        // than across this tick's own move: the server moves a flown aircraft
        // when its pilot's packets arrive, between ticks.
        this.prevPitch = this.pitch;
        this.prevBank = this.bank;
        float climb = 0.0F;
        if (!this.onGround() && !Double.isNaN(this.lastTickY)) {
            double speed = Math.max(0.05, Math.abs(this.getEngineSpeed()));
            climb = (float) Math.toDegrees(Math.atan2(this.getY() - this.lastTickY, speed));
        }
        this.lastTickY = this.getY();
        this.pitch += (Mth.clamp(climb, -MAX_PITCH, MAX_PITCH) - this.pitch) * 0.2F;
        // Whoever flies it sees the bank they put in; the server has it from
        // them, or puts it in itself with nobody at the controls, and hands it
        // on to everyone else.
        float flown;
        if (this.isControlledByLocalInstance()) {
            flown = this.flyingBank();
            if (!this.level().isClientSide()) {
                this.entityData.set(DATA_BANK, flown);
            }
        } else {
            flown = this.entityData.get(DATA_BANK);
        }
        this.bank += (flown - this.bank) * 0.3F;
        if (this.level().isClientSide() && !this.onGround() && this.getControllingPassenger() != null) {
            this.emitVapour();
        }
    }

    /** Above this height the air is cold enough for a trail off the wingtips in level flight. */
    private static final double CONTRAIL_HEIGHT = 160.0;
    /** Banked this hard, the tips pull a trail of vapour at any height. */
    private static final float VAPOUR_BANK = 28.0F;

    /** Where the wingtips are, in blocks out from the middle; worked out once. */
    private double wingtip = Double.NaN;

    /**
     * Wisps of vapour off the wingtips: always high up where the air is cold,
     * and lower down whenever a hard turn pulls the air into them.
     */
    private void emitVapour() {
        boolean cold = this.getY() > CONTRAIL_HEIGHT;
        boolean pulling = Math.abs(this.bank) > VAPOUR_BANK;
        if (!cold && !pulling || this.lift() < 0.9F) {
            return;
        }
        if (Double.isNaN(this.wingtip)) {
            me.trossty.car.vehicle.VehicleDefinition defined = this.getDefinition();
            double tip = this.getBbWidth() / 2.0;
            if (defined != null) {
                for (me.trossty.car.vehicle.VehicleDefinition.Box box : defined.shape()) {
                    tip = Math.max(tip, Math.max(Math.abs(box.from().x()), Math.abs(box.to().x())) / 16.0);
                }
            }
            this.wingtip = tip;
        }
        float yawRad = this.getYRot() * ((float) Math.PI / 180.0F);
        double rightX = Mth.cos(yawRad);
        double rightZ = Mth.sin(yawRad);
        double rise = Math.sin(Math.toRadians(this.bank)) * this.wingtip;
        double height = this.getY() + 1.4;
        for (int side = -1; side <= 1; side += 2) {
            double x = this.getX() + rightX * side * this.wingtip;
            double z = this.getZ() + rightZ * side * this.wingtip;
            this.level().addParticle(net.minecraft.core.particles.ParticleTypes.CLOUD,
                    x, height + side * rise, z, 0.0, 0.0, 0.0);
        }
    }

    /** The bank the ailerons hold now, as the side flying it has it: level on the ground. */
    public float flyingBank() {
        return this.onGround() ? 0.0F : this.flownBank;
    }

    /** The bank the pilot reports flying, kept by the server for everyone else. */
    public void setReportedBank(float bank) {
        if (Float.isFinite(bank)) {
            this.entityData.set(DATA_BANK, Mth.clamp(bank, -STALL_BANK, STALL_BANK));
        }
    }

    /** Nose up, degrees, as drawn. */
    @Override
    public float getBodyPitch(float tickProgress) {
        return Mth.lerp(tickProgress, this.prevPitch, this.pitch);
    }

    /** Into the turn, right wing down positive, degrees, as drawn. */
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
