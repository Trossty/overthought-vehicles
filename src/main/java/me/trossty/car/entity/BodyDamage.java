package me.trossty.car.entity;

import me.trossty.car.block.VehicleLiftBlock;
import me.trossty.car.network.CarNetworking;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.phys.Vec3;

/**
 * The body and what hits it: how much of it is left, telling a crash from a
 * scrape, what a crash does to the car and the people in it, wrecking it when
 * nothing is left, and beating the dents back out with metal.
 */
public final class BodyDamage {

    private final CarEntity car;

    BodyDamage(CarEntity car) {
        this.car = car;
    }

    /** A crash has to be at least this bad to hole the tank... */
    private static final float PUNCTURE_MIN_DAMAGE = 4.0F;

    /** ...and its chance rises by one for every this much worse, to {@link #PUNCTURE_MAX_CHANCE}. */
    private static final float PUNCTURE_SPREAD = 10.0F;

    private static final float PUNCTURE_MAX_CHANCE = 0.6F;

    /** How much harder a crash hits once the springs have given up. */
    private static final float WORN_SPRING_HARSHNESS = 1.6F;

    /** Condition the springs lose per point of damage they soak up. */
    private static final float SUSPENSION_WEAR_PER_DAMAGE = 1.4F;

    /**
     * How much of a tick's travel an obstacle has to swallow before it counts
     * as a crash rather than a scrape. Glancing off a wall costs almost
     * nothing — at 30 degrees into it a flat-out car loses 0.08 of its 0.62 —
     * so this leaves kerb-scraping free and still catches a head-on hit.
     */
    private static final float MIN_CRASH_SPEED = 0.15F;

    /** Condition lost per block-per-tick of travel swallowed beyond that. */
    private static final float CRASH_DAMAGE_PER_SPEED = 18.0F;

    /** Ticks of grace after an impact, so one wall cannot bill you twice. */
    private static final int CRASH_COOLDOWN = 10;

    /** Condition one iron ingot beats back out. */
    public static final float REPAIR_PER_INGOT = 5.0F;

    /** Iron left in the road when a car is wrecked rather than picked up. */
    private static final int WRECK_SCRAP = 2;

    public float getHealth() {
        return this.car.getEntityData().get(CarEntity.HEALTH);
    }

    public float getHealthFraction() {
        return Mth.clamp(this.getHealth() / CarEntity.MAX_HEALTH, 0.0F, 1.0F);
    }

    public void setHealth(float health) {
        this.car.getEntityData().set(CarEntity.HEALTH, Mth.clamp(health, 0.0F, CarEntity.MAX_HEALTH));
    }

    /**
     * Turns an obstacle into crash damage, on whichever side is simulating
     * the car.
     *
     * <p>Only the simulating side sees the collision at all: while somebody
     * drives, the physics run on that player's client and the server receives
     * nothing but the resulting position. An earlier version tried to infer
     * impacts on the server from how much speed vanished in one tick, which
     * broke badly — vehicle position packets do not land exactly one per
     * server tick, so a tick that received none looked like a dead stop and
     * wrecked cars on straight, empty road. {@code horizontalCollision} is the
     * real signal, so the driving client reports it and the server, which owns
     * the condition, applies it.
     *
     * <p>Severity is how much of the requested travel the obstacle swallowed,
     * which grades the hit for free: a head-on impact loses everything, a
     * glancing scrape along a wall loses almost nothing.
     *
     * @param requested the movement asked of {@link CarEntity#move}
     * @param from      where the car stood before that move
     */
    void checkForCrash(Vec3 requested, Vec3 from) {
        if (this.car.crashCooldown > 0 || !this.car.horizontalCollision) {
            return;
        }
        double covered = this.car.position().subtract(from).horizontalDistance();
        float blocked = (float) (requested.horizontalDistance() - covered);
        if (blocked < MIN_CRASH_SPEED) {
            return;
        }
        // Dead stop. Without it the throttle keeps shoving the car into the
        // wall and the same impact is billed again on the following tick.
        this.car.flyDist = 0.0F;
        this.car.setDeltaMovement(0.0, this.car.getDeltaMovement().y, 0.0);
        this.car.crashCooldown = CRASH_COOLDOWN;

        this.reportImpact((blocked - MIN_CRASH_SPEED) * CRASH_DAMAGE_PER_SPEED * this.car.crashFragility());
    }

    /**
     * Bills an impact the simulating side saw: applied here on the server,
     * reported to it from a driving client.
     */
    protected void reportImpact(float amount) {
        if (amount <= 0.0F) {
            return;
        }
        this.car.crashCooldown = CRASH_COOLDOWN;
        if (this.car.level() instanceof ServerLevel world) {
            // The people first: a wreck throws them out, and then nobody is inside to hurt.
            this.hurtOccupants(amount);
            this.applyCarDamage(world, amount);
        } else {
            CarNetworking.reportCrash(amount);
        }
    }

    /** True while a crash was billed a moment ago: the same impact is not billed twice. */
    protected boolean recentlyCrashed() {
        return this.car.crashCooldown > 0;
    }

    /**
     * Applies a crash a driving client reported. The severity arrives from a
     * client, so it is capped: the worst a tampered-with client can do is wreck
     * the car it is sitting in, which it could manage by driving into a wall.
     */
    public void applyReportedCrash(ServerLevel world, float amount) {
        if (this.car.crashCooldown > 0) {
            return;
        }
        this.car.crashCooldown = CRASH_COOLDOWN;
        float capped = Mth.clamp(amount, 0.0F, CarEntity.MAX_HEALTH);
        this.hurtOccupants(capped);
        this.applyCarDamage(world, capped);
    }

    /** Below this much crash damage to the car, the people inside only feel a jolt. */
    private static final float OCCUPANT_HARMLESS = 2.0F;

    /** Of the crash damage past that, the share each person inside takes. */
    private static final float OCCUPANT_SHARE = 0.6F;

    /** A hard crash shakes up whoever is inside, belted in or not. */
    private void hurtOccupants(float amount) {
        float hurt = (amount - OCCUPANT_HARMLESS) * OCCUPANT_SHARE;
        if (hurt <= 0.0F) {
            return;
        }
        for (Entity passenger : this.car.getPassengers()) {
            if (passenger instanceof LivingEntity living) {
                living.hurt(this.car.damageSources().flyIntoWall(), hurt);
            }
        }
    }

    /**
     * Wears the car down by {@code amount} and wrecks it at zero. The wobble is
     * the same one vanilla vehicles use, but the car's own condition replaces
     * vanilla's rule of breaking once the wobble passes 40, which would have
     * healed itself a point a tick.
     */
    /**
     * Metal on whatever it hit: sparks off the front corner, and on a hard
     * enough knock glass and a puff of dust and smoke with them.
     */
    private void throwSparks(ServerLevel world, float amount) {
        float yawRad = this.car.getYRot() * ((float) Math.PI / 180.0F);
        double reach = this.car.rearAxle()[0] + 0.5;
        double x = this.car.getX() - Mth.sin(yawRad) * reach;
        double z = this.car.getZ() + Mth.cos(yawRad) * reach;
        double y = this.car.getY() + 0.5;
        int sparks = Mth.clamp((int) (amount * 3.0F), 8, 40);
        world.sendParticles(ParticleTypes.CRIT, x, y, z, sparks, 0.5, 0.3, 0.5, 0.6);
        world.sendParticles(ParticleTypes.ELECTRIC_SPARK, x, y, z, sparks, 0.5, 0.25, 0.5, 0.4);
        world.sendParticles(ParticleTypes.LAVA, x, y, z, Math.max(2, sparks / 4), 0.4, 0.2, 0.4, 0.0);
        if (amount >= PUNCTURE_MIN_DAMAGE) {
            world.sendParticles(new net.minecraft.core.particles.BlockParticleOption(ParticleTypes.BLOCK,
                            net.minecraft.world.level.block.Blocks.GLASS.defaultBlockState()),
                    x, y + 0.3, z, 16, 0.5, 0.3, 0.5, 0.15);
            world.sendParticles(ParticleTypes.POOF, x, y, z, 6, 0.4, 0.2, 0.4, 0.03);
        }
    }

    void applyCarDamage(ServerLevel world, float amount) {
        if (amount <= 0.0F || this.car.isRemoved()) {
            return;
        }
        // Springs are what an impact is supposed to go through. Tired ones pass
        // more of it into the bodyshell, and take some of it themselves — which
        // is the only way suspension ever wears, since miles do not touch it.
        // Read the springs before this impact tires them: they soak up the hit
        // in the state they were in when it arrived, not the state it left them.
        amount *= this.car.parts().partFactor(CarPart.SUSPENSION, WORN_SPRING_HARSHNESS);
        this.car.parts().wear(CarPart.SUSPENSION, amount * SUSPENSION_WEAR_PER_DAMAGE);
        float knock = amount * Upkeep.ALIGNMENT_PER_DAMAGE;
        float drift = this.car.upkeep().getAlignment();
        this.car.upkeep().setAlignment(drift + (drift != 0.0F ? Math.signum(drift) : this.car.getRandom().nextBoolean() ? 1.0F : -1.0F) * knock);
        this.car.rockFromHit(amount);
        this.throwSparks(world, amount);

        // A hard enough knock holes the tank. It is not the end of the car,
        // but it is the end of the journey unless somebody patches it.
        // One that wrecks it outright needs no word about the tank.
        if (!this.car.isLeaking() && this.car.getMaxFuel() > 0.0F && amount >= PUNCTURE_MIN_DAMAGE
                && this.getHealth() - amount > 0.0F
                && this.car.getRandom().nextFloat() < Math.min(PUNCTURE_MAX_CHANCE,
                        (amount - PUNCTURE_MIN_DAMAGE + 1.0F) / PUNCTURE_SPREAD)) {
            this.car.setLeaking(true);
            world.playSound(null, this.car.getX(), this.car.getY() + 0.5, this.car.getZ(),
                    SoundEvents.ITEM_BREAK, SoundSource.NEUTRAL, 0.9F, 0.6F);
            if (this.car.getControllingPassenger() instanceof Player driver) {
                driver.displayClientMessage(Component.translatable("message.car.tank_holed"), true);
            }
        }

        float health = this.getHealth() - amount;
        this.setHealth(health);
        world.playSound(null, this.car.getX(), this.car.getY() + 0.8, this.car.getZ(),
                SoundEvents.IRON_GOLEM_DAMAGE, SoundSource.NEUTRAL, 0.8F, 1.3F);
        if (health <= 0.0F) {
            this.wreck(world);
        }
    }

    /**
     * The car falls apart. It leaves scrap rather than the car item, so wrecking
     * one actually costs you something.
     */
    private void wreck(ServerLevel world) {
        world.playSound(null, this.car.getX(), this.car.getY() + 0.8, this.car.getZ(),
                SoundEvents.ANVIL_DESTROY, SoundSource.NEUTRAL, 0.9F, 0.8F);
        world.sendParticles(ParticleTypes.LARGE_SMOKE,
                this.car.getX(), this.car.getY() + 0.9, this.car.getZ(), 30, 0.5, 0.4, 0.5, 0.02);
        this.car.wreckEffects(world);
        this.car.kill();
        if (world.getGameRules().getBoolean(GameRules.RULE_DOENTITYDROPS)) {
            this.car.spawnAtLocation(new ItemStack(Items.IRON_INGOT, WRECK_SCRAP));
        }
    }

    /**
     * Beats one ingot's worth of dents out. Returns PASS on a sound car so the
     * click falls through to mounting and the iron is not wasted.
     */
    InteractionResult repairWith(Player player, ItemStack ingots) {
        if (this.getHealth() >= CarEntity.MAX_HEALTH) {
            return InteractionResult.PASS;
        }
        if (!VehicleLiftBlock.isServicing(this.car.level(), this.car)) {
            // PASS, not FAIL: the click should still fall through to mounting, so
            // an ingot in hand never stops you getting in and driving to a garage.
            if (player.level().isClientSide()) {
                player.displayClientMessage(VehicleLiftBlock.needsLift(this.car), true);
            }
            return InteractionResult.PASS;
        }
        if (this.car.level().isClientSide()) {
            return InteractionResult.SUCCESS;
        }
        this.setHealth(this.getHealth() + REPAIR_PER_INGOT);
        if (!player.isCreative()) {
            ingots.shrink(1);
        }
        this.car.level().playSound(
                null,
                this.car.getX(), this.car.getY() + 0.8, this.car.getZ(),
                SoundEvents.ANVIL_USE,
                SoundSource.NEUTRAL,
                0.8F,
                1.1F);
        return InteractionResult.SUCCESS;
    }
}

