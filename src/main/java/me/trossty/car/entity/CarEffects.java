package me.trossty.car.entity;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;

/**
 * What a running car gives off, so that its state can be seen and heard
 * before it is read: exhaust that thickens with the throttle, oil dripping
 * from a leak, steam from a boiling radiator, smoke from a battered body, and
 * the coughs and pops of an engine that wants seeing to.
 */
public final class CarEffects {

    private final CarEntity car;

    CarEffects(CarEntity car) {
        this.car = car;
    }

    /** Below this engine temperature the exhaust steams white. */
    private static final float COLD_EXHAUST = 0.2F;

    /** Speed gained in a tick, blocks per tick, that has a diesel blowing soot. */
    private static final float DIESEL_SOOT_GAIN = 0.008F;

    /** Engine speed when the exhaust last looked, to see the car pulling hard. */
    private float exhaustSpeed;

    private int faultNoiseCooldown;

    /**
     * The exhaust pipe, at the back on the left, puffing whenever the engine is
     * actually turning. It idles lazily and works hard under power, which is the
     * only outward sign that a parked car with somebody in it is still running.
     */
    void emitExhaust() {
        float speed = Math.abs(this.car.getEngineSpeed());
        float gained = speed - this.exhaustSpeed;
        this.exhaustSpeed = speed;
        if (!this.car.isEngineRunning() || this.car.isElectric()) {
            // A battery and a motor have no tailpipe.
            return;
        }
        float yawRad = this.car.getYRot() * ((float) Math.PI / 180.0F);
        Vec3 at = this.car.position().add(this.car.exhaustOffset().yRot(-yawRad));
        // Trails out behind the car rather than hanging where it was made.
        double drift = -0.04 - speed * 0.5;
        double driftX = -Mth.sin(yawRad) * drift;
        double driftZ = Mth.cos(yawRad) * drift;

        // A diesel worked hard off the line blows a cloud of soot.
        if (gained > DIESEL_SOOT_GAIN && !this.car.getEngineFuel().needsSpark() && this.car.getRandom().nextInt(2) == 0) {
            for (int i = 0; i < 2; i++) {
                this.car.level().addParticle(ParticleTypes.LARGE_SMOKE, at.x, at.y, at.z,
                        driftX + (this.car.getRandom().nextDouble() - 0.5) * 0.04, 0.03,
                        driftZ + (this.car.getRandom().nextDouble() - 0.5) * 0.04);
            }
        }
        if (this.car.getRandom().nextInt(speed < 0.05F ? 7 : 2) != 0) {
            return;
        }
        // The colour says what is wrong, the way a mechanic reads it from the
        // kerb: white and thick is coolant through a blown head gasket, blue
        // is oil burning past worn rings, black is fuel with too little air.
        // Cold, the water in the exhaust comes out as white steam until it warms through.
        boolean cold = this.car.getEngineTemp() < COLD_EXHAUST;
        if (this.car.upkeep().isGasketBlown()) {
            this.car.level().addParticle(ParticleTypes.WHITE_SMOKE, at.x, at.y, at.z, driftX, 0.02, driftZ);
            this.car.level().addParticle(ParticleTypes.CLOUD, at.x, at.y, at.z, driftX, 0.01, driftZ);
            return;
        }
        if (this.car.upkeep().isBurningOil()) {
            this.car.level().addParticle(OIL_SMOKE, at.x, at.y + 0.05, at.z, 0.0, 0.0, 0.0);
            this.car.level().addParticle(ParticleTypes.SMOKE, at.x, at.y, at.z, driftX, 0.015, driftZ);
            return;
        }
        if (this.car.upkeep().isRunningRich()) {
            this.car.level().addParticle(ParticleTypes.LARGE_SMOKE, at.x, at.y, at.z, driftX, 0.02, driftZ);
            return;
        }
        this.car.level().addParticle(
                cold ? ParticleTypes.WHITE_SMOKE
                        : speed > 0.25F ? ParticleTypes.SMOKE : ParticleTypes.CAMPFIRE_COSY_SMOKE,
                at.x, at.y, at.z, driftX, 0.015, driftZ);
    }

    /** The blue-grey haze of burning oil. */
    private static final net.minecraft.core.particles.DustParticleOptions OIL_SMOKE =
            new net.minecraft.core.particles.DustParticleOptions(new org.joml.Vector3f(0.45F, 0.55F, 0.78F), 2.2F);

    /**
     * Oil dripping under the engine, in the middle up front: amber like the
     * fuel leak but under the bonnet rather than at the sill, so the two are
     * told apart by where the puddle is.
     */
    void emitOilDrip() {
        float yawRad = this.car.getYRot() * ((float) Math.PI / 180.0F);
        double front = 0.9;
        me.trossty.car.vehicle.VehicleDefinition defined = this.car.getDefinition();
        if (defined != null && !defined.wheels().isEmpty()) {
            front = -Double.MAX_VALUE;
            for (me.trossty.car.vehicle.VehicleDefinition.Wheel wheel : defined.wheels()) {
                front = Math.max(front, wheel.at().z() / 16.0);
            }
            front -= 0.3;
        }
        Vec3 at = this.car.position().add(new Vec3((this.car.getRandom().nextDouble() - 0.5) * 0.3, 0.3, front).yRot(-yawRad));
        this.car.level().addParticle(ParticleTypes.DRIPPING_DRIPSTONE_LAVA, at.x, at.y, at.z, 0.0, 0.0, 0.0);
        this.car.level().addParticle(ParticleTypes.LANDING_HONEY,
                at.x + (this.car.getRandom().nextDouble() - 0.5) * 0.3, this.car.getY() + 0.02,
                at.z + (this.car.getRandom().nextDouble() - 0.5) * 0.3, 0.0, 0.0, 0.0);
    }

    /**
     * Coolant weeping from a tired water pump's seal: clear drops off the
     * front of the engine, on the far side from the oil filter, so a
     * mechanic tells the two puddles apart by colour and by side.
     */
    void emitCoolantDrip() {
        float yawRad = this.car.getYRot() * ((float) Math.PI / 180.0F);
        double front = 0.9;
        me.trossty.car.vehicle.VehicleDefinition defined = this.car.getDefinition();
        if (defined != null && !defined.wheels().isEmpty()) {
            front = -Double.MAX_VALUE;
            for (me.trossty.car.vehicle.VehicleDefinition.Wheel wheel : defined.wheels()) {
                front = Math.max(front, wheel.at().z() / 16.0);
            }
        }
        Vec3 at = this.car.position().add(new Vec3(-0.25, 0.35, front).yRot(-yawRad));
        this.car.level().addParticle(ParticleTypes.DRIPPING_WATER, at.x, at.y, at.z, 0.0, 0.0, 0.0);
        this.car.level().addParticle(ParticleTypes.SPLASH, at.x, this.car.getY() + 0.05, at.z, 0.0, 0.0, 0.0);
    }

    /**
     * What a sick car sounds like from outside: a knock from a dry or worn-out
     * engine in time with the revs, and a clunk from tired springs over the
     * road. Played by the server so everybody near hears it.
     */
    void complainAboutTheEngine() {
        if (this.faultNoiseCooldown > 0) {
            this.faultNoiseCooldown--;
            return;
        }
        float speed = Math.abs(this.car.getEngineSpeed());
        if (this.car.isEngineRunning() && !this.car.isElectric() && this.car.upkeep().isKnocking()) {
            this.car.level().playSound(null, this.car.getX(), this.car.getY() + 0.6, this.car.getZ(),
                    SoundEvents.NOTE_BLOCK_BASEDRUM.value(), SoundSource.NEUTRAL, 0.7F, 0.5F + speed);
            this.faultNoiseCooldown = Math.max(3, 9 - Math.round(speed * 12.0F));
            return;
        }
        if (speed > 0.15F && this.car.hasPart(CarPart.SUSPENSION) && this.car.parts().isFitted(CarPart.SUSPENSION)
                && this.car.parts().worstFraction(CarPart.SUSPENSION) <= FaultCodes.WARNING
                && this.car.getRandom().nextInt(30) == 0) {
            this.car.level().playSound(null, this.car.getX(), this.car.getY() + 0.3, this.car.getZ(),
                    SoundEvents.IRON_TRAPDOOR_CLOSE, SoundSource.NEUTRAL, 0.5F, 0.6F);
            this.faultNoiseCooldown = 10;
            return;
        }
        // A worn ball joint clunks as the weight shifts across it: sharper
        // and higher than a tired spring's thump.
        float play = this.car.parts().play();
        if (speed > 0.1F && play > 0.3F && this.car.getRandom().nextFloat() < play / 25.0F) {
            this.car.level().playSound(null, this.car.getX(), this.car.getY() + 0.3, this.car.getZ(),
                    SoundEvents.IRON_DOOR_CLOSE, SoundSource.NEUTRAL, 0.45F, 1.5F);
            this.faultNoiseCooldown = 8;
        }
    }

    /** A black puff out of the tailpipe: the engine coughing on air. */
    void coughExhaust() {
        float yawRad = this.car.getYRot() * ((float) Math.PI / 180.0F);
        Vec3 at = this.car.position().add(this.car.exhaustOffset().yRot(-yawRad));
        for (int i = 0; i < 4; i++) {
            this.car.level().addParticle(ParticleTypes.LARGE_SMOKE, at.x, at.y, at.z,
                    (this.car.getRandom().nextDouble() - 0.5) * 0.05, 0.02, (this.car.getRandom().nextDouble() - 0.5) * 0.05);
        }
    }

    /**
     * Fuel dripping out of the sill on the left, where the tank sits: just
     * ahead of the rear axle, or under the back seat on a vehicle nobody
     * described. Out at the side rather than under the middle, where the body
     * and the wheels would hide it.
     */
    void emitLeak() {
        double back = -0.9;
        me.trossty.car.vehicle.VehicleDefinition defined = this.car.getDefinition();
        if (defined != null && !defined.wheels().isEmpty()) {
            back = Double.MAX_VALUE;
            for (me.trossty.car.vehicle.VehicleDefinition.Wheel wheel : defined.wheels()) {
                back = Math.min(back, wheel.at().z() / 16.0);
            }
            // Clear of the tyre, so the drip is not hidden behind it.
            back += 0.6;
        }
        double side = this.car.getBbWidth() / 2.0 - 0.12;
        float yawRad = this.car.getYRot() * ((float) Math.PI / 180.0F);
        Vec3 at = this.car.position().add(new Vec3(
                side, 0.45, back + (this.car.getRandom().nextDouble() - 0.5) * 0.3).yRot(-yawRad));
        this.car.level().addParticle(ParticleTypes.FALLING_HONEY, at.x, at.y, at.z, 0.0, 0.0, 0.0);
        // A steady drip leaves a wet patch behind it.
        if (this.car.getRandom().nextInt(4) == 0) {
            this.car.level().addParticle(ParticleTypes.LANDING_HONEY,
                    at.x + (this.car.getRandom().nextDouble() - 0.5) * 0.4, this.car.getY() + 0.02,
                    at.z + (this.car.getRandom().nextDouble() - 0.5) * 0.4, 0.0, 0.0, 0.0);
        }
    }

    /** Smoke from under the bonnet once the car is badly knocked about. */
    /**
     * Steam off the bonnet. Deliberately a different particle from the smoke a
     * dented car gives off: the two faults want different parts, so they should
     * not look the same from the driver's seat.
     */
    void emitOverheatSteam() {
        float yawRad = this.car.getYRot() * ((float) Math.PI / 180.0F);
        this.car.level().addParticle(ParticleTypes.CLOUD,
                this.car.getX() - Mth.sin(yawRad) * 1.3 + (this.car.getRandom().nextDouble() - 0.5) * 0.6,
                this.car.getY() + 1.0,
                this.car.getZ() + Mth.cos(yawRad) * 1.3 + (this.car.getRandom().nextDouble() - 0.5) * 0.6,
                0.0, 0.06, 0.0);
    }

    /** Smoke off the worst-battered side: over the bonnet after a head-on, over the boot after backing into something. */
    void emitDamageSmoke() {
        // Partway out from the middle towards that side, over the body rather than past the bumper.
        Vec3 side = this.car.damage().pointOf(this.car.damage().worstZone());
        this.car.level().addParticle(
                this.car.damage().getHealthFraction() < 0.15F ? ParticleTypes.LARGE_SMOKE : ParticleTypes.SMOKE,
                Mth.lerp(SMOKE_OUT, this.car.getX(), side.x),
                this.car.getY() + 0.8,
                Mth.lerp(SMOKE_OUT, this.car.getZ(), side.z),
                0.0, 0.03, 0.0);
    }

    /** How far out towards the battered side the smoke rises, from the middle (0) to the bumper (1). */
    private static final double SMOKE_OUT = 0.55;
}
