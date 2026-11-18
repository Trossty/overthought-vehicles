package me.trossty.car.entity;

import me.trossty.car.block.VehicleLiftBlock;
import me.trossty.car.network.CarNetworking;
import me.trossty.car.util.ValueInput;
import me.trossty.car.util.ValueOutput;
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
import org.jetbrains.annotations.Nullable;

/**
 * The body and what hits it: how much of each side is left, telling a crash
 * from a scrape, what a crash does to the car and the people in it, wrecking
 * it when one side has nothing left to give, and beating the dents back out
 * with metal.
 *
 * <p>Where a knock lands matters as much as how hard it is. A head-on hit
 * crumples the front and the radiator behind it; backing into a wall dents
 * the rear, bends the exhaust and may hole the tank; a side swipe takes the
 * tyres and springs on that side. A knock with no side to it, a fall or a
 * fire, lands on the whole body as it always did.
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

    /** Condition the springs lose per point of damage they soak up, when the knock has no side to it. */
    private static final float SUSPENSION_WEAR_PER_DAMAGE = 1.4F;

    /** The same for the springs at the side that was hit, which take it all between them. */
    private static final float STRUCK_SPRING_WEAR = 2.2F;

    /** Radiator condition lost per point of damage to the front: a full-speed head-on all but finishes it. */
    private static final float RADIATOR_PER_DAMAGE = 6.0F;

    /** Exhaust condition lost per point of damage to the rear. */
    private static final float EXHAUST_PER_DAMAGE = 5.0F;

    /** Tyre condition lost per point of damage to a side, for the tyres on that side. */
    private static final float KERBED_TYRE_WEAR = 3.0F;

    /** How much of a side swipe reaches the tank, which sits towards the back between the wheels. */
    private static final float SIDE_TANK_EXPOSURE = 0.5F;

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

    /** What is left of one side, MAX_HEALTH fresh to 0 stove in. */
    public float getZone(BodyZone zone) {
        return this.car.getEntityData().get(CarEntity.BODY.get(zone.ordinal()));
    }

    public void setZone(BodyZone zone, float health) {
        this.car.getEntityData().set(CarEntity.BODY.get(zone.ordinal()), Mth.clamp(health, 0.0F, CarEntity.MAX_HEALTH));
    }

    public float zoneFraction(BodyZone zone) {
        return Mth.clamp(this.getZone(zone) / CarEntity.MAX_HEALTH, 0.0F, 1.0F);
    }

    /** The side in the worst state; the front when they are all alike. */
    public BodyZone worstZone() {
        BodyZone worst = BodyZone.FRONT;
        for (BodyZone zone : BodyZone.values()) {
            if (this.getZone(zone) < this.getZone(worst)) {
                worst = zone;
            }
        }
        return worst;
    }

    /**
     * The body as a whole: its worst side. One side stove in is a wreck
     * however straight the others are, so this is the number that slows the
     * car, lights the lamp and, at nothing, scraps it.
     */
    public float getHealth() {
        return this.getZone(this.worstZone());
    }

    public float getHealthFraction() {
        return Mth.clamp(this.getHealth() / CarEntity.MAX_HEALTH, 0.0F, 1.0F);
    }

    /** Every side to the same state: a car built, spawned or loaded whole. */
    public void setHealth(float health) {
        for (BodyZone zone : BodyZone.values()) {
            this.setZone(zone, health);
        }
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
     * glancing scrape along a wall loses almost nothing. The side that hit is
     * the one the car was travelling towards.
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

        BodyZone struck = BodyZone.facing(this.car.getYRot(), new Vec3(requested.x, 0.0, requested.z), 1.0F);
        this.reportImpact((blocked - MIN_CRASH_SPEED) * CRASH_DAMAGE_PER_SPEED * this.car.crashFragility(), struck);
    }

    /** Bills an impact with no side to it, such as an aircraft landing too hard. */
    protected void reportImpact(float amount) {
        this.reportImpact(amount, null);
    }

    /**
     * Bills an impact the simulating side saw: applied here on the server,
     * reported to it from a driving client.
     */
    protected void reportImpact(float amount, @Nullable BodyZone zone) {
        if (amount <= 0.0F) {
            return;
        }
        this.car.crashCooldown = CRASH_COOLDOWN;
        if (this.car.level() instanceof ServerLevel world) {
            // The people first: a wreck throws them out, and then nobody is inside to hurt.
            this.hurtOccupants(amount);
            this.applyCarDamage(world, amount, zone);
        } else {
            CarNetworking.reportCrash(amount, zone);
        }
    }

    /** True while a crash was billed a moment ago: the same impact is not billed twice. */
    protected boolean recentlyCrashed() {
        return this.car.crashCooldown > 0;
    }

    public void applyReportedCrash(ServerLevel world, float amount) {
        this.applyReportedCrash(world, amount, null);
    }

    /**
     * Applies a crash a driving client reported. The severity arrives from a
     * client, so it is capped: the worst a tampered-with client can do is wreck
     * the car it is sitting in, which it could manage by driving into a wall.
     */
    public void applyReportedCrash(ServerLevel world, float amount, @Nullable BodyZone zone) {
        if (this.car.crashCooldown > 0) {
            return;
        }
        this.car.crashCooldown = CRASH_COOLDOWN;
        float capped = Mth.clamp(amount, 0.0F, CarEntity.MAX_HEALTH);
        this.hurtOccupants(capped);
        this.applyCarDamage(world, capped, zone);
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
     * Where a side of the car is in the world, at the height of the bumpers:
     * just past the nose or the tail, or out at a sill. The middle of the car
     * for a knock with no side.
     */
    public Vec3 pointOf(@Nullable BodyZone zone) {
        double[] axle = this.car.rearAxle();
        double ahead = 0.0;
        double left = 0.0;
        if (zone != null) {
            switch (zone) {
                case FRONT -> ahead = axle[0] + 0.5;
                case REAR -> ahead = -(axle[0] + 0.5);
                case LEFT -> left = axle[1] + 0.4;
                case RIGHT -> left = -(axle[1] + 0.4);
            }
        }
        float yawRad = this.car.getYRot() * ((float) Math.PI / 180.0F);
        return new Vec3(
                this.car.getX() - Mth.sin(yawRad) * ahead + Mth.cos(yawRad) * left,
                this.car.getY() + 0.5,
                this.car.getZ() + Mth.cos(yawRad) * ahead + Mth.sin(yawRad) * left);
    }

    /**
     * Metal on whatever it hit: sparks off the side that hit it, and on a hard
     * enough knock glass and a puff of dust and smoke with them.
     */
    private void throwSparks(ServerLevel world, float amount, @Nullable BodyZone zone) {
        Vec3 at = this.pointOf(zone);
        int sparks = Mth.clamp((int) (amount * 3.0F), 8, 40);
        world.sendParticles(ParticleTypes.CRIT, at.x, at.y, at.z, sparks, 0.5, 0.3, 0.5, 0.6);
        world.sendParticles(ParticleTypes.ELECTRIC_SPARK, at.x, at.y, at.z, sparks, 0.5, 0.25, 0.5, 0.4);
        world.sendParticles(ParticleTypes.LAVA, at.x, at.y, at.z, Math.max(2, sparks / 4), 0.4, 0.2, 0.4, 0.0);
        if (amount >= PUNCTURE_MIN_DAMAGE) {
            world.sendParticles(new net.minecraft.core.particles.BlockParticleOption(ParticleTypes.BLOCK,
                            net.minecraft.world.level.block.Blocks.GLASS.defaultBlockState()),
                    at.x, at.y + 0.3, at.z, 16, 0.5, 0.3, 0.5, 0.15);
            world.sendParticles(ParticleTypes.POOF, at.x, at.y, at.z, 6, 0.4, 0.2, 0.4, 0.03);
        }
    }

    /** A knock with no side to it: a fall, a fire, a punch from nowhere in particular. */
    void applyCarDamage(ServerLevel world, float amount) {
        this.applyCarDamage(world, amount, null);
    }

    /**
     * Dents a side of the car by {@code amount}, or every side when there is
     * none to blame, hurts what sits behind it, and wrecks the car once a side
     * has nothing left. The wobble is the same one vanilla vehicles use, but
     * the car's own condition replaces vanilla's rule of breaking once the
     * wobble passes 40, which would have healed itself a point a tick.
     */
    void applyCarDamage(ServerLevel world, float amount, @Nullable BodyZone zone) {
        if (amount <= 0.0F || this.car.isRemoved()) {
            return;
        }
        // Springs are what an impact is supposed to go through. Tired ones pass
        // more of it into the bodyshell, and take some of it themselves — which
        // is the only way suspension ever wears, since miles do not touch it.
        // Read the springs before this impact tires them: they soak up the hit
        // in the state they were in when it arrived, not the state it left them.
        amount *= this.car.parts().partFactor(CarPart.SUSPENSION, WORN_SPRING_HARSHNESS);
        this.hitPartsBehind(zone, amount);
        this.knockOutOfTrue(zone, amount);
        this.car.rockFromHit(amount);
        this.throwSparks(world, amount, zone);

        float left = (zone == null ? this.getHealth() : this.getZone(zone)) - amount;
        this.holeTheTank(world, zone, amount, left);
        if (zone == null) {
            for (BodyZone each : BodyZone.values()) {
                this.setZone(each, this.getZone(each) - amount);
            }
        } else {
            this.setZone(zone, this.getZone(zone) - amount);
        }
        world.playSound(null, this.car.getX(), this.car.getY() + 0.8, this.car.getZ(),
                SoundEvents.IRON_GOLEM_DAMAGE, SoundSource.NEUTRAL, 0.8F, 1.3F);
        if (left <= 0.0F) {
            this.wreck(world);
        }
    }

    /**
     * What sits behind a side takes its share: the springs at that end or on
     * that side, and the radiator behind the grille, the exhaust under the
     * tail, or the tyres along a sill.
     */
    private void hitPartsBehind(@Nullable BodyZone zone, float amount) {
        CarParts parts = this.car.parts();
        if (zone == null) {
            parts.wear(CarPart.SUSPENSION, amount * SUSPENSION_WEAR_PER_DAMAGE);
            return;
        }
        parts.wearAt(CarPart.SUSPENSION, zone, amount * STRUCK_SPRING_WEAR);
        switch (zone) {
            case FRONT -> parts.wear(CarPart.RADIATOR, amount * RADIATOR_PER_DAMAGE);
            case REAR -> parts.wear(CarPart.EXHAUST, amount * EXHAUST_PER_DAMAGE);
            case LEFT, RIGHT -> parts.wearAt(CarPart.WHEELS, zone, amount * KERBED_TYRE_WEAR);
        }
    }

    /**
     * A knock at the front or a side bends the steering out of true: towards
     * a side that was hit, and otherwise further the way it already pulls. The
     * back wheels do not steer, so a knock there leaves the line alone.
     */
    private void knockOutOfTrue(@Nullable BodyZone zone, float amount) {
        if (zone == BodyZone.REAR) {
            return;
        }
        float drift = this.car.upkeep().getAlignment();
        float way = zone != null && zone.pullSide() != 0.0F ? zone.pullSide()
                : drift != 0.0F ? Math.signum(drift)
                : this.car.getRandom().nextBoolean() ? 1.0F : -1.0F;
        this.car.upkeep().setAlignment(drift + way * amount * Upkeep.ALIGNMENT_PER_DAMAGE);
    }

    /** How much of a knock on this side reaches the tank: all from behind, none through the engine bay. */
    private static float tankExposure(@Nullable BodyZone zone) {
        if (zone == null) {
            return 1.0F;
        }
        return switch (zone) {
            case FRONT -> 0.0F;
            case REAR -> 1.0F;
            case LEFT, RIGHT -> SIDE_TANK_EXPOSURE;
        };
    }

    /**
     * A hard enough knock near the tank holes it. It is not the end of the
     * car, but it is the end of the journey unless somebody patches it. One
     * that wrecks it outright needs no word about the tank.
     */
    private void holeTheTank(ServerLevel world, @Nullable BodyZone zone, float amount, float left) {
        float exposure = tankExposure(zone);
        if (this.car.isLeaking() || this.car.getMaxFuel() <= 0.0F || exposure <= 0.0F
                || amount < PUNCTURE_MIN_DAMAGE || left <= 0.0F
                || this.car.getRandom().nextFloat() >= exposure * Math.min(PUNCTURE_MAX_CHANCE,
                        (amount - PUNCTURE_MIN_DAMAGE + 1.0F) / PUNCTURE_SPREAD)) {
            return;
        }
        this.car.setLeaking(true);
        world.playSound(null, this.car.getX(), this.car.getY() + 0.5, this.car.getZ(),
                SoundEvents.ITEM_BREAK, SoundSource.NEUTRAL, 0.9F, 0.6F);
        if (this.car.getControllingPassenger() instanceof Player driver) {
            driver.displayClientMessage(Component.translatable("message.car.tank_holed"), true);
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
     * The side a player with a hammer gets at: the dented one they are
     * standing by, or else the worst one; null when nothing is dented.
     */
    public @Nullable BodyZone sideToBeatOut(Player player) {
        BodyZone near = BodyZone.facing(this.car, player.position());
        if (near != null && this.getZone(near) < CarEntity.MAX_HEALTH) {
            return near;
        }
        BodyZone worst = this.worstZone();
        return this.getZone(worst) < CarEntity.MAX_HEALTH ? worst : null;
    }

    /**
     * Beats one ingot's worth of dents out of the side the player is at.
     * Returns PASS on a sound car so the click falls through to mounting and
     * the iron is not wasted.
     */
    InteractionResult repairWith(Player player, ItemStack ingots) {
        BodyZone side = this.sideToBeatOut(player);
        if (side == null) {
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
        this.setZone(side, this.getZone(side) + REPAIR_PER_INGOT);
        if (!player.isCreative()) {
            ingots.shrink(1);
        }
        player.displayClientMessage(Component.translatable("message.car.body_beaten",
                side.displayName(), Math.round(this.zoneFraction(side) * 100.0F)), true);
        this.car.level().playSound(
                null,
                this.car.getX(), this.car.getY() + 0.8, this.car.getZ(),
                SoundEvents.ANVIL_USE,
                SoundSource.NEUTRAL,
                0.8F,
                1.1F);
        return InteractionResult.SUCCESS;
    }

    /** Reads back what {@link #save} wrote; a car saved with one number for its body gets it on every side. */
    void load(ValueInput view) {
        int[] saved = view.tag().getIntArray("Body");
        if (saved.length == BodyZone.values().length) {
            for (BodyZone zone : BodyZone.values()) {
                this.setZone(zone, Float.intBitsToFloat(saved[zone.ordinal()]));
            }
        } else {
            this.setHealth(view.getFloatOr("Health", CarEntity.MAX_HEALTH));
        }
    }

    void save(ValueOutput view) {
        int[] bits = new int[BodyZone.values().length];
        for (BodyZone zone : BodyZone.values()) {
            bits[zone.ordinal()] = Float.floatToIntBits(this.getZone(zone));
        }
        view.tag().putIntArray("Body", bits);
    }
}
