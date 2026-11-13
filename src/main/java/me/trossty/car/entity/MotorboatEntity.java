package me.trossty.car.entity;

import me.trossty.car.registry.ModItems;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.tags.FluidTags;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

/**
 * A boat with an outboard on the transom. Underneath it is the car — engine,
 * fuel, key, paint, the service — with a hull for wheels: it floats at its
 * waterline, the propeller drives it at full speed through water a car could
 * only wade, and the water does not drown an engine that hangs off the back.
 * Run aground, it can only be shoved along a crawl.
 *
 * <p>It has no wheels, brakes or springs to look after; the motor, its
 * plugs, filters, battery and the drive leg are what wear.
 */
public class MotorboatEntity extends CarEntity {

    /** How deep the hull sits, in blocks below the waterline. */
    private static final double DRAFT = 0.35;
    /** How hard the water pushes it back to its waterline, per block off it. */
    private static final double BUOYANCY = 0.1;
    /** Up-and-down speed kept each tick in the water: the hull settles, it does not bounce. */
    private static final double HEAVE_DAMPING = 0.7;
    /** Speed it can be pushed along on dry land, blocks a tick. */
    private static final float LAND_SPEED = 0.03F;

    public MotorboatEntity(EntityType<?> type, Level world) {
        super(type, world);
    }

    @Override
    protected int plainSeatCount() {
        return 4;
    }

    @Override
    protected Item getDropItem() {
        return ModItems.MOTORBOAT;
    }

    @Override
    public me.trossty.car.vehicle.VehicleDefinition.Chassis chassis() {
        return me.trossty.car.vehicle.VehicleDefinition.Chassis.BOAT;
    }

    /**
     * No wheels, so nothing that sits behind one either; and an outboard is
     * cooled by the water it runs in and engages its gear by a dog clutch in
     * the leg, so no radiator and no clutch.
     */
    @Override
    public boolean hasPart(CarPart part) {
        return super.hasPart(part) && part != CarPart.WHEELS && part != CarPart.BRAKE_PADS
                && part != CarPart.BRAKE_DISCS && part != CarPart.SUSPENSION
                && part != CarPart.RADIATOR && part != CarPart.CLUTCH;
    }

    /** Forward, neutral and reverse in the leg: nothing to shift through. */
    @Override
    public boolean hasGearbox() {
        return false;
    }

    @Override
    protected boolean underway() {
        return this.onGround() || this.afloat();
    }

    @Override
    protected boolean floods() {
        return false;
    }

    /** Afloat, nothing holds it back but its own drag; aground, it barely moves. */
    @Override
    protected float limitSpeed(float speed) {
        if (this.afloat()) {
            return speed;
        }
        return this.onGround() ? Mth.clamp(speed, -LAND_SPEED, LAND_SPEED) : speed;
    }

    /** The hull grips the water the way a tyre grips the road. */
    @Override
    protected boolean hasGrip(boolean onGround) {
        return onGround || this.afloat();
    }

    /** Pushed back up to its waterline, settling rather than bobbing about it. */
    @Override
    protected double verticalMotion(boolean onGround) {
        double depth = this.getFluidHeight(FluidTags.WATER);
        if (depth <= 0.0) {
            return super.verticalMotion(onGround);
        }
        double dy = this.getDeltaMovement().y * HEAVE_DAMPING + (depth - DRAFT) * BUOYANCY;
        return Mth.clamp(dy, -0.2, 0.2);
    }

    /** The outboard breathes and exhausts through its own leg, just above the water. */
    @Override
    protected Vec3 exhaustOffset() {
        Vec3 tail = super.exhaustOffset();
        return new Vec3(0.0, 0.55, tail.z + 0.1);
    }

    @Override
    public void tick() {
        super.tick();
        if (this.level().isClientSide() && this.afloat()) {
            this.wake();
        }
    }

    private boolean afloat() {
        return this.isInWater() && this.getFluidHeight(FluidTags.WATER) > 0.05;
    }

    /** Spray off the bow and churned water behind the propeller, more the faster it goes. */
    private void wake() {
        float speed = Math.abs(this.getEngineSpeed());
        if (speed < 0.08F) {
            return;
        }
        float yawRad = this.getYRot() * ((float) Math.PI / 180.0F);
        double half = this.getBbWidth() / 2.0;
        double surface = this.getY() + DRAFT;
        Vec3 stern = this.position().add(this.exhaustOffset().yRot(-yawRad));
        for (int i = 0; i < 2; i++) {
            this.level().addParticle(ParticleTypes.BUBBLE, stern.x, this.getY(), stern.z,
                    (this.random.nextDouble() - 0.5) * 0.1, 0.05, (this.random.nextDouble() - 0.5) * 0.1);
        }
        if (this.random.nextFloat() < speed * 1.5F) {
            for (int side = -1; side <= 1; side += 2) {
                Vec3 bow = this.position().add(new Vec3(side * half, 0.0, half).yRot(-yawRad));
                this.level().addParticle(ParticleTypes.SPLASH, bow.x, surface, bow.z,
                        side * -Mth.cos(yawRad) * 0.1, 0.1, side * -Mth.sin(yawRad) * 0.1);
            }
            this.level().addParticle(ParticleTypes.SPLASH, stern.x, surface, stern.z, 0.0, 0.1, 0.0);
        }
    }

    /**
     * A boat wrecked on the water goes down: a burst of spray, the hull
     * filling with a gurgle of bubbles, and the wreck slipping under. One
     * wrecked ashore just breaks up like anything else.
     */
    @Override
    protected void wreckEffects(net.minecraft.server.level.ServerLevel world) {
        if (!this.afloat()) {
            return;
        }
        world.playSound(null, this.getX(), this.getY(), this.getZ(),
                net.minecraft.sounds.SoundEvents.GENERIC_SPLASH, net.minecraft.sounds.SoundSource.NEUTRAL, 1.0F, 0.7F);
        world.playSound(null, this.getX(), this.getY(), this.getZ(),
                net.minecraft.sounds.SoundEvents.BUBBLE_COLUMN_UPWARDS_AMBIENT, net.minecraft.sounds.SoundSource.NEUTRAL, 1.0F, 0.8F);
        world.sendParticles(ParticleTypes.SPLASH, this.getX(), this.getY() + 0.6, this.getZ(),
                80, 1.2, 0.2, 1.8, 0.2);
        world.sendParticles(ParticleTypes.BUBBLE_COLUMN_UP, this.getX(), this.getY() - 0.3, this.getZ(),
                60, 1.0, 0.3, 1.6, 0.05);
        world.sendParticles(ParticleTypes.BUBBLE, this.getX(), this.getY() - 0.6, this.getZ(),
                40, 1.0, 0.4, 1.6, 0.05);
    }
}
