package me.trossty.car.entity;

import me.trossty.car.block.VehicleLiftBlock;
import me.trossty.car.network.CarNetworking;
import me.trossty.car.registry.ModComponents;
import me.trossty.car.registry.ModItems;
import me.trossty.car.sound.ModSounds;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityDimensions;
import net.minecraft.world.entity.EntitySelector;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.InterpolationHandler;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.entity.player.Input;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.vehicle.VehicleEntity;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.DyeItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.gamerules.GameRules;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

import java.util.List;

/**
 * A drivable car. Movement is simulated on whichever side is logical for the
 * entity (the controlling player's client while somebody drives, otherwise the
 * server), exactly like vanilla boats, so vanilla's own vehicle-move packets
 * keep position and yaw in sync and no custom networking is needed.
 */
public class CarEntity extends VehicleEntity {

    /** Signed horizontal speed, tracked so every client can animate the wheels. */
    private static final EntityDataAccessor<Float> ENGINE_SPEED =
            SynchedEntityData.defineId(CarEntity.class, EntityDataSerializers.FLOAT);

    /**
     * Fuel left in the tank. Tracked because the driver's client decides whether
     * the throttle responds and draws the gauge, while the server owns the value.
     */
    private static final EntityDataAccessor<Float> FUEL =
            SynchedEntityData.defineId(CarEntity.class, EntityDataSerializers.FLOAT);

    /**
     * A holed tank, dripping. Tracked because every client draws the drip and
     * the driver's dashboard says so.
     */
    private static final EntityDataAccessor<Boolean> LEAKING =
            SynchedEntityData.defineId(CarEntity.class, EntityDataSerializers.BOOLEAN);

    /**
     * What {@link #FUEL} holds for a vehicle nobody has burned anything in yet:
     * "as full as its tank goes", whatever size the tank turns out to be. The
     * definition that says how big that is is not known when the tracker is
     * built, so a number there would be wrong for every vehicle but one.
     */
    private static final float FACTORY_FULL = -1.0F;

    /**
     * Condition, from MAX_HEALTH down to 0. Tracked because the driver's client
     * caps its own top speed from it and every client draws the warning lamp.
     */
    private static final EntityDataAccessor<Float> HEALTH =
            SynchedEntityData.defineId(CarEntity.class, EntityDataSerializers.FLOAT);

    /**
     * Seat placement, in blocks from the car's own position. Half the cabin's
     * interior width is 0.5, so the two seats sit far enough apart for both
     * torsos to fit; the occupants' arms still overlap the doors a little.
     */
    private static final double SEAT_SIDEWAYS = 0.22;
    private static final double SEAT_HEIGHT = 0.6;
    private static final double SEAT_FORWARD = -0.25;

    /**
     * A family car's tank, in fuel units — fifty litres, one full jerry can,
     * roughly eight minutes at full throttle. A vehicle's own tank comes from
     * its definition ({@link #getMaxFuel}); this is what one without a
     * definition gets.
     */
    public static final float MAX_FUEL = 100.0F;

    /** What share of an idle tick's worth an electric car wins back while it slows. */
    public static final float REGEN_SHARE = 0.6F;

    /** Energy a charging post spends on one unit of an electric car's gauge. */
    public static final int ENERGY_PER_CHARGE_UNIT = 400;
    /** Burned per tick with the engine on but standing still (~55 minutes idling). */
    private static final float FUEL_BURN_IDLE = 0.0015F;
    /** Extra burn per tick at full speed, on top of the idle burn. */
    private static final float FUEL_BURN_DRIVING = 0.009F;
    /** Fuel one pour from a jerry can transfers. */
    public static final float JERRY_CAN_UNITS = me.trossty.car.item.JerryCans.POUR;

    /** Below this share of the tank the dashboard chimes, once. */
    public static final float LOW_FUEL_FRACTION = 0.1F;
    /** Above this the chime is re-armed, so a top-up and a second run-down chime again. */
    private static final float LOW_FUEL_REARM = 0.15F;
    /**
     * Below this share the pickup sucks air on corners and the engine coughs.
     * It is the last few litres, and it is the warning you get when you have
     * ignored the chime.
     */
    private static final float STARVING_FRACTION = 0.04F;
    /** Chance per tick of a cough while starving. */
    private static final float STARVE_CHANCE = 0.03F;
    /** Speed left after a cough. */
    private static final float STARVE_STUMBLE = 0.75F;
    /** Losing speed faster than this is the foot off the pedal: only idle is burned. */
    private static final float COAST_MARGIN = 0.004F;
    /** Extra load per block per tick of climb, smoothed. A 1-in-4 at full speed is half again. */
    private static final float CLIMB_LOAD = 4.0F;
    /** Extra thirst towing a trailer. */
    private static final float TOWING_THIRST = 1.3F;
    /** A holed tank loses this much a tick: a family car's tank in under three minutes. */
    private static final float LEAK_PER_TICK = 0.03F;
    /** A crash has to be at least this bad to hole the tank... */
    private static final float PUNCTURE_MIN_DAMAGE = 4.0F;
    /** ...and its chance rises by one for every this much worse, to {@link #PUNCTURE_MAX_CHANCE}. */
    private static final float PUNCTURE_SPREAD = 10.0F;
    private static final float PUNCTURE_MAX_CHANCE = 0.6F;
    /** Entity status: the engine coughed on an empty pickup. */
    public static final byte STATUS_SPUTTER = 103;

    public static final float MAX_HEALTH = 20.0F;
    /** Below this much condition the car smokes and the dashboard lamp lights. */
    public static final float DAMAGE_WARNING_FRACTION = 0.35F;
    /** Top speed multiplier at zero condition; a sound car keeps all of MAX_SPEED. */
    private static final float WRECKED_SPEED_FACTOR = 0.5F;

    /** Top speed left in a car whose engine is completely worn out. */
    private static final float WORN_ENGINE_SPEED = 0.68F;

    /** Acceleration left when the gearbox is finished. */
    private static final float WORN_GEARBOX_DRIVE = 0.55F;

    /** Reverse speed left when the gearbox is finished. */
    private static final float WORN_GEARBOX_REVERSE = 0.65F;

    /** Grip left on bald tyres: it steers and brakes about half as well. */
    private static final float WORN_TYRE_GRIP = 0.55F;

    /** How much harder a crash hits once the springs have given up. */
    private static final float WORN_SPRING_HARSHNESS = 1.6F;

    /** Condition the springs lose per point of damage they soak up. */
    private static final float SUSPENSION_WEAR_PER_DAMAGE = 1.4F;

    /** Engine wear at a standstill, as a share of its full-throttle rate. */
    private static final float IDLE_WEAR_SHARE = 0.35F;

    // What each of the other parts is worth, as the multiplier left when it is
    // completely worn out and, where it differs, when it is not on the car at
    // all. A missing part is always worse than a finished one: a bald tyre is
    // still a tyre.

    /** Throttle response left on a set of plugs that is past it. */
    private static final float WORN_PLUGS_THROTTLE = 0.75F;
    /** Chance per tick of a stumble, with plugs at nothing. */
    private static final float MISFIRE_CHANCE = 0.05F;

    private static final float WORN_FILTER_SPEED = 0.92F;
    private static final float MISSING_FILTER_SPEED = 0.84F;
    /** A blocked filter makes the engine drink; no filter is worse still. */
    private static final float WORN_FILTER_THIRST = 1.35F;
    private static final float MISSING_FILTER_THIRST = 1.70F;

    /** How much faster the engine and gearbox wear on dirty oil. */
    private static final float WORN_OIL_WEAR = 2.2F;
    private static final float MISSING_OIL_WEAR = 3.2F;

    /** Cooling left with a furred-up radiator, and with none. */
    private static final float WORN_RADIATOR_COOLING = 0.55F;
    private static final float MISSING_RADIATOR_COOLING = 0.15F;
    /** Above this the engine is boiling and pulls like it means it. */
    private static final float OVERHEAT_AT = 0.85F;
    private static final float OVERHEATING_POWER = 0.55F;
    /** How fast the temperature chases where it is headed, up and down. */
    private static final float TEMP_RISE = 0.012F;
    private static final float TEMP_FALL = 0.020F;

    /** Battery condition lost per tick with no charge coming in at all. */
    private static final float BATTERY_DRAIN = 0.0040F;
    /** At or below this the battery is flat and the car will not start. */
    private static final float FLAT_BATTERY = 2.0F;

    private static final float WORN_CLUTCH_DRIVE = 0.60F;
    private static final float WORN_SHAFT_DRIVE = 0.70F;

    private static final float WORN_PADS_BRAKE = 0.45F;
    private static final float MISSING_PADS_BRAKE = 0.22F;
    private static final float WORN_DISCS_BRAKE = 0.70F;
    private static final float MISSING_DISCS_BRAKE = 0.40F;
    /** How much faster worn pads chew through the discs behind them. */
    private static final float PADS_EAT_DISCS = 4.0F;
    /** Brake wear per unit of speed scrubbed off. */
    private static final float BRAKE_WEAR_PER_DECEL = 0.9F;

    private static final float WORN_RACK_STEER = 0.50F;
    private static final float MISSING_RACK_STEER = 0.15F;

    private static final float WORN_EXHAUST_SPEED = 0.95F;
    private static final float MISSING_EXHAUST_SPEED = 0.90F;
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
    private static final float SAFE_FALL_BLOCKS = 3.0F;
    private static final float FALL_DAMAGE_PER_BLOCK = 1.5F;
    /** Condition one iron ingot beats back out. */
    public static final float REPAIR_PER_INGOT = 5.0F;
    /** Iron left in the road when a car is wrecked rather than picked up. */
    private static final int WRECK_SCRAP = 2;

    /**
     * Paint colour, as a {@code DyeColor} index. Tracked because the renderer on
     * every nearby client picks its texture from it.
     */
    /**
     * Condition of each fitted part, {@link CarPart#MAX_CONDITION} down to 0,
     * indexed by {@link CarPart#ordinal()}. Tracked rather than kept to the
     * server because the physics these feed runs on the driver's own client.
     */
    private static final List<EntityDataAccessor<Float>> PART_CONDITION = registerPartConditions();

    private static List<EntityDataAccessor<Float>> registerPartConditions() {
        List<EntityDataAccessor<Float>> entries = new java.util.ArrayList<>(CarPart.values().length);
        for (int index = 0; index < CarPart.values().length; index++) {
            entries.add(SynchedEntityData.defineId(CarEntity.class, EntityDataSerializers.FLOAT));
        }
        return List.copyOf(entries);
    }

    /**
     * What each fitted part was built to, two bits per part packed into one
     * number, in {@link CarPart#ordinal()} order. Packed rather than sixteen
     * more tracked entries because quality is one of three things and the whole
     * car fits in a single int: sixteen parts times two bits is exactly thirty
     * two. A seventeenth part would need a long here.
     */
    private static final EntityDataAccessor<Integer> PART_QUALITY =
            SynchedEntityData.defineId(CarEntity.class, EntityDataSerializers.INT);

    /** Two bits per part, so each part has room for one {@code Quality}. */
    private static final int QUALITY_BITS = 2;
    private static final int QUALITY_MASK = (1 << QUALITY_BITS) - 1;

    /**
     * How hot the engine is running, 0 at ambient and 1 at boiling. Tracked
     * because the temperature gauge on the dashboard is drawn client side.
     */
    private static final EntityDataAccessor<Float> ENGINE_TEMP =
            SynchedEntityData.defineId(CarEntity.class, EntityDataSerializers.FLOAT);

    /**
     * What kind of engine, tyres and filter are fitted, whether there is a
     * turbo, and what is in the tank. See {@link CarSetup}.
     */
    private static final EntityDataAccessor<Integer> SETUP =
            SynchedEntityData.defineId(CarEntity.class, EntityDataSerializers.INT);

    private static final EntityDataAccessor<Integer> COLOR =
            SynchedEntityData.defineId(CarEntity.class, EntityDataSerializers.INT);

    /**
     * How much of what is in the tank is raw fuel, 0 to 1, mixed by volume.
     * Raw fuel burns, but its sulphur and gum eat the engine and foul the plugs.
     */
    private static final EntityDataAccessor<Float> DIRTY_FUEL =
            SynchedEntityData.defineId(CarEntity.class, EntityDataSerializers.FLOAT);
    /** At a tank of pure raw fuel the engine wears this many times again as fast... */
    public static final float DIRTY_ENGINE_WEAR = 2.0F;
    /** ...and the plugs foul faster still. */
    public static final float DIRTY_PLUG_WEAR = 3.0F;

    /** Blocks driven in this car's life, for the odometer and for how fast it wears. */
    private static final EntityDataAccessor<Float> ODOMETER =
            SynchedEntityData.defineId(CarEntity.class, EntityDataSerializers.FLOAT);

    /**
     * The mileage at which a car counts as high-mileage: by then every part
     * wears half as fast again as it did new, the way an old car eats
     * consumables however well it is kept.
     */
    public static final float HIGH_MILEAGE = 50_000.0F;
    /** How much faster than new everything wears at {@link #HIGH_MILEAGE}. */
    public static final float MILEAGE_WEAR = 0.5F;
    /** How much faster than fresh a part wears when it is nearly finished. */
    public static final float TIRED_WEAR = 0.5F;

    /**
     * A sprayed-on colour as 0xRRGGBB, or -1 when the body is still the plain
     * dye colour. A spray can mixes dyes the way leather armour does, so this
     * can be any colour at all.
     */
    private static final EntityDataAccessor<Integer> PAINT =
            SynchedEntityData.defineId(CarEntity.class, EntityDataSerializers.INT);

    /** Locked with its key: nobody gets in, opens it up or picks it up. */
    private static final EntityDataAccessor<Boolean> LOCKED =
            SynchedEntityData.defineId(CarEntity.class, EntityDataSerializers.BOOLEAN);

    /**
     * The car has a key and the driver is not carrying it, so the engine will
     * not turn. Worked out on the server, which knows the lock; tracked, because
     * the engine runs on the driver's client.
     */
    private static final EntityDataAccessor<Boolean> KEY_MISSING =
            SynchedEntityData.defineId(CarEntity.class, EntityDataSerializers.BOOLEAN);

    /** The number this car's keys carry; 0 for a car that has never had one. Server only. */
    private long lockCode;

    /**
     * Which way the car is actually travelling, in degrees, on the side that
     * drives it. Normally it follows the nose almost at once; in a handbrake
     * turn it lags, and the difference is the slide.
     */
    private float travelYaw = Float.NaN;
    private int skidSoundCooldown;

    /**
     * Which data-pack vehicle this is, as its id; empty for the plain machine.
     * Tracked, because the client draws it and drives it from the same file.
     */
    private static final EntityDataAccessor<String> MODEL =
            SynchedEntityData.defineId(CarEntity.class, EntityDataSerializers.STRING);

    private String definitionKey = "";
    private me.trossty.car.vehicle.VehicleDefinition definition;

    public static final DyeColor DEFAULT_COLOR = DyeColor.RED;

    /** Top forward speed in blocks per tick (~12.4 blocks/s). */
    public static final float MAX_SPEED = 0.62F;
    /** Top reverse speed in blocks per tick. */
    public static final float MAX_REVERSE_SPEED = 0.22F;

    public static final float ACCELERATION = 0.022F;
    private static final float BRAKE_STRENGTH = 0.06F;
    /** Speed retained per tick while coasting with the engine running. */
    private static final float ENGINE_DRAG = 0.986F;
    /** Speed retained per tick with nobody driving. */
    private static final float ROLLING_FRICTION = 0.94F;
    private static final float AIR_DRAG = 0.995F;
    /** Degrees of yaw change per tick at full steering lock. */
    private static final float MAX_TURN_RATE = 3.4F;
    /** Speed at which steering reaches full effectiveness. */
    private static final float FULL_GRIP_SPEED = 0.18F;

    /** Below this speed the handbrake just stops the car; above it, it slides it. */
    public static final float DRIFT_MIN_SPEED = 0.22F;
    /** How much of the brakes a handbrake turn uses: the back locks, the front rolls. */
    private static final float DRIFT_BRAKE = 0.25F;
    /** How much faster the nose comes round with the back end loose. */
    private static final float DRIFT_TURN = 1.7F;
    /** How quickly the direction of travel catches up with the nose, per tick. */
    private static final float DRIFT_GRIP = 0.10F;
    private static final float NORMAL_GRIP = 0.55F;

    /** Visual lock of the front wheels, in degrees. */
    private static final float MAX_STEER_ANGLE = 28.0F;
    /** Wheel radius in blocks (3 model units), used to roll the wheels. */
    private static final float WHEEL_RADIUS = 0.1875F;
    private static final float SPEED_EPSILON = 0.002F;
    /** Where the exhaust pipe sits, in the car's own (sideways, up, forward) space. */
    private static final double EXHAUST_SIDEWAYS = 0.34;
    private static final double EXHAUST_HEIGHT = 0.3;
    private static final double EXHAUST_BACK = -1.3;

    /** Minimum ticks between two horn blasts. */
    private static final int HORN_COOLDOWN_TICKS = 12;

    private final InterpolationHandler interpolator = new InterpolationHandler(this, 3);

    private boolean pressingForward;
    private boolean pressingBack;
    private boolean pressingLeft;
    private boolean pressingRight;
    private boolean pressingBrake;

    /** Signed speed, only meaningful on the logical side. */
    private float flyDist;
    private float wheelAngle;
    private float lastWheelAngle;
    private float steerAngle;
    private float lastSteerAngle;
    private float acceleration;
    private float lastAcceleration;
    private float lastRollingSpeed;

    // What the fuel is burned from, server side.
    private float lastBurnSpeed;
    private double lastBurnY = Double.NaN;
    private float climbRate;
    private boolean lowFuelWarned;
    private double lastTickX;
    private double lastTickZ;
    /** Last tick's tracked speed, so the server can see braking happen. */
    private float lastTrackedSpeed;
    private boolean hasLastTickPos;
    private boolean wasLogicalSide;

    /** The trailer behind this, while it keeps saying so; see {@link #towedBy}. */
    private @Nullable TrailerEntity towed;
    private int towingTicks;
    private int hornCooldown;
    private int crashCooldown;

    public CarEntity(EntityType<?> type, Level world) {
        super(type, world);
        // The size is the body's, and the body is known from the first tick.
        this.refreshDimensions();
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(ENGINE_SPEED, 0.0F);
        builder.define(FUEL, FACTORY_FULL);
        builder.define(LEAKING, false);
        builder.define(COLOR, DEFAULT_COLOR.getId());
        builder.define(PAINT, -1);
        builder.define(LOCKED, false);
        builder.define(KEY_MISSING, false);
        builder.define(HEALTH, MAX_HEALTH);
        for (CarPart part : CarPart.values()) {
            builder.define(PART_CONDITION.get(part.ordinal()), CarPart.MAX_CONDITION);
        }
        builder.define(ENGINE_TEMP, 0.0F);
        builder.define(PART_QUALITY, defaultQualityBits());
        builder.define(SETUP, CarSetup.DEFAULT);
        builder.define(MODEL, "");
        builder.define(ODOMETER, 0.0F);
        builder.define(DIRTY_FUEL, 0.0F);
    }

    // ---- what a data pack says this vehicle is ----

    /** The id of the vehicle this is, or null for the plain machine. */
    /** What to call this car: its name tag if it has one, else what model it is ("Jeep Wrangler"). */
    public Component getVehicleName() {
        return this.hasCustomName() && this.getCustomName() != null
                ? this.getCustomName()
                : me.trossty.car.vehicle.VehicleDefinition.nameOf(this.getModelId());
    }

    public net.minecraft.resources.Identifier getModelId() {
        String key = this.entityData.get(MODEL);
        return key.isEmpty()
                ? me.trossty.car.vehicle.VehicleDefinition.defaultFor(this.chassis())
                : net.minecraft.resources.Identifier.tryParse(key);
    }

    public void setModel(net.minecraft.resources.Identifier id) {
        this.entityData.set(MODEL, id == null ? "" : id.toString());
    }

    /**
     * The definition behind {@link #getModelId}, or null — for the plain
     * machine, and for a vehicle whose pack has since been taken out, which
     * then simply drives and looks like the machine underneath it.
     */
    public me.trossty.car.vehicle.VehicleDefinition getDefinition() {
        String key = this.entityData.get(MODEL);
        if (!key.equals(this.definitionKey) || (this.definition == null && this.tickCount < 40)) {
            this.definitionKey = key;
            this.definition = null;
            net.minecraft.resources.Identifier id = key.isEmpty()
                    ? me.trossty.car.vehicle.VehicleDefinition.defaultFor(this.chassis())
                    : net.minecraft.resources.Identifier.tryParse(key);
            if (id != null) {
                this.definition = this.level().registryAccess()
                        .lookup(me.trossty.car.vehicle.VehicleDefinition.KEY)
                        .map(registry -> registry.getValue(id))
                        .orElse(null);
            }
        }
        return this.definition;
    }

    /** The part map for the definition it was worked out from. */
    private me.trossty.car.vehicle.VehicleDefinition layoutFor;
    private java.util.Map<CarPart, CarPart.Anchor> layout;
    private float roof;

    /** Where this part sits on this vehicle: its own wheel, its own engine bay. */
    public CarPart.Anchor anchorOf(CarPart part) {
        this.refreshLayout();
        return this.layout.get(part);
    }

    /** How tall this vehicle stands, in blocks. */
    public float roofHeight() {
        this.refreshLayout();
        return this.roof;
    }

    private void refreshLayout() {
        me.trossty.car.vehicle.VehicleDefinition definition = this.getDefinition();
        if (this.layout == null || definition != this.layoutFor) {
            this.layoutFor = definition;
            this.layout = PartLayout.of(definition);
            this.roof = PartLayout.roof(definition);
        }
    }

    /** Which machine this is underneath, whatever body it wears. */
    public me.trossty.car.vehicle.VehicleDefinition.Chassis chassis() {
        return me.trossty.car.vehicle.VehicleDefinition.Chassis.CAR;
    }

    private me.trossty.car.vehicle.VehicleDefinition.Performance tuning() {
        me.trossty.car.vehicle.VehicleDefinition defined = this.getDefinition();
        return defined == null
                ? me.trossty.car.vehicle.VehicleDefinition.Performance.PLAIN : defined.performance();
    }

    @Override
    public void onSyncedDataUpdated(EntityDataAccessor<?> data) {
        super.onSyncedDataUpdated(data);
        if (MODEL.equals(data)) {
            // A different vehicle is a different size.
            this.refreshDimensions();
        }
    }

    @Override
    public EntityDimensions getDimensions(net.minecraft.world.entity.Pose pose) {
        me.trossty.car.vehicle.VehicleDefinition defined = this.getDefinition();
        EntityDimensions plain = super.getDimensions(pose);
        if (defined == null) {
            return plain;
        }
        return EntityDimensions.fixed(defined.width(), defined.height())
                .withEyeHeight(plain.eyeHeight());
    }

    /** A car off the lift, with a standard part in every slot. */
    public static int defaultQualityBits() {
        int bits = 0;
        for (CarPart part : CarPart.values()) {
            bits |= CarPart.Quality.DEFAULT.ordinal() << (part.ordinal() * QUALITY_BITS);
        }
        return bits;
    }

    @Override
    public InterpolationHandler getInterpolation() {
        return this.interpolator;
    }

    @Override
    public void tick() {
        if (this.level().isClientSide()) {
            this.prevRestPose = this.restPose;
            this.restPose += (this.restPoseTarget() - this.restPose) * 0.18F;
            if (this.lockFlashTicks > 0) {
                if (this.lockFlashTicks == LOCK_FLASH_TICKS
                        || this.lockFlashTicks == LOCK_FLASH_TICKS / 2 && this.lockFlashTwice) {
                    this.flashIndicators();
                }
                this.lockFlashTicks--;
            }
        }
        if (this.getHurtTime() > 0) {
            this.setHurtTime(this.getHurtTime() - 1);
        }
        if (this.getDamage() > 0.0F) {
            this.setDamage(this.getDamage() - 1.0F);
        }
        if (this.hornCooldown > 0) {
            this.hornCooldown--;
        }
        if (this.crashCooldown > 0) {
            this.crashCooldown--;
        }
        if (this.towingTicks > 0) {
            this.towingTicks--;
        } else {
            this.towed = null;
        }

        super.tick();
        this.interpolator.interpolate();

        if (!this.level().isClientSide()) {
            this.wearParts();
            this.tickTemperature();
        }

        boolean logicalSide = this.isLocalInstanceAuthoritative();
        if (logicalSide) {
            // Control just moved to this side (somebody mounted or dismounted), so
            // pick up the speed the other side had instead of lurching to a stop.
            if (!this.wasLogicalSide) {
                this.flyDist = this.entityData.get(ENGINE_SPEED);
            }
            this.updateDriving();
            Vec3 requested = this.getDeltaMovement();
            Vec3 from = this.position();
            this.move(MoverType.SELF, requested);
            this.checkForCrash(requested, from);
        } else {
            this.setDeltaMovement(Vec3.ZERO);
            this.travelYaw = Float.NaN;
        }
        this.wasLogicalSide = logicalSide;

        if (!this.level().isClientSide()) {
            float travelled = this.measureTravelledSpeed();
            this.entityData.set(ENGINE_SPEED, travelled);
            // On the road only: a car carried off by water or shoved about by
            // a piston has not been driven.
            if (this.onGround() && this.getControllingPassenger() != null) {
                this.setOdometer(this.getOdometer() + Math.abs(travelled));
            }
            // Braking only exists on the driver's client, so the brakes are
            // charged for the speed the car was actually seen to lose.
            this.wearBrakes(travelled);
            this.entityData.set(KEY_MISSING, this.lockCode != 0
                    && this.getControllingPassenger() instanceof Player driver
                    && !driver.isCreative() && !carriesKey(driver, this.lockCode));
            this.warnTheDriver();
            // Burned on the server, which owns the tank, from the distance actually
            // covered: while somebody drives, the throttle only exists on their client.
            this.burnFuel(travelled);
        } else {
            if (this.getHealthFraction() < DAMAGE_WARNING_FRACTION && this.random.nextInt(4) == 0) {
                this.emitDamageSmoke();
            }
            if (this.isOverheating() && this.random.nextInt(2) == 0) {
                this.emitOverheatSteam();
            }
            this.emitExhaust();
            this.emitSkid();
            if (this.isLeaking() && this.hasFuel()) {
                this.emitLeak();
            }
        }
        this.lastTickX = this.getX();
        this.lastTickZ = this.getZ();
        this.hasLastTickPos = true;

        this.animateWheels();
        this.applyEffectsFromBlocks();
        this.pushNearbyEntities();

        // The trailer may already have ticked this tick, a step behind where
        // this has just got to: bring it along now, so it never lags.
        if (this.towed != null && !this.towed.isRemoved() && this.towed.getTractor() == this) {
            this.towed.follow(this);
        }
    }

    // ---- towing ----

    /** A trailer says this every tick it is hitched on. */
    void towedBy(TrailerEntity trailer) {
        this.towed = trailer;
        this.towingTicks = 3;
    }

    /** True when something other than {@code other} is hitched behind this. */
    public boolean isTowing(@Nullable TrailerEntity other) {
        return this.towed != null && this.towed != other && this.towingTicks > 0 && !this.towed.isRemoved();
    }

    /** Whether this and {@code other} are a car and the trailer on it, either way round. */
    protected boolean hitchedWith(Entity other) {
        return other instanceof TrailerEntity trailer && trailer.hitchedWith(this);
    }

    private me.trossty.car.vehicle.VehicleDefinition reachOf;
    private double frontReach = 2.4;
    private double rearReach = 2.4;

    private void measureReach() {
        me.trossty.car.vehicle.VehicleDefinition defined = this.getDefinition();
        if (defined == this.reachOf) {
            return;
        }
        this.reachOf = defined;
        if (defined == null || defined.shape().isEmpty()) {
            this.frontReach = 2.4;
            this.rearReach = 2.4;
            return;
        }
        float front = 0.0F;
        float rear = 0.0F;
        for (me.trossty.car.vehicle.VehicleDefinition.Box box : defined.shape()) {
            front = Math.max(front, Math.max(box.from().z(), box.to().z()));
            rear = Math.min(rear, Math.min(box.from().z(), box.to().z()));
        }
        this.frontReach = front / 16.0;
        this.rearReach = -rear / 16.0;
    }

    /** From the middle to the very front of the body, in blocks. */
    protected double frontReach() {
        this.measureReach();
        return this.frontReach;
    }

    /** Where a trailer's coupler sits: on the tow ball just behind the rear bumper. */
    public Vec3 hitchPoint() {
        this.measureReach();
        float yaw = this.getYRot() * ((float) Math.PI / 180.0F);
        double back = this.rearReach + 0.1;
        return new Vec3(this.getX() + Mth.sin(yaw) * back, this.getY(),
                this.getZ() - Mth.cos(yaw) * back);
    }

    /**
     * Derives the signed speed from how far the car actually moved. The server
     * needs this because while a player drives, movement is simulated on that
     * player's client and only the resulting position reaches the server.
     */
    private float measureTravelledSpeed() {
        if (!this.hasLastTickPos) {
            return 0.0F;
        }
        double dx = this.getX() - this.lastTickX;
        double dz = this.getZ() - this.lastTickZ;
        float yawRad = this.getYRot() * ((float) Math.PI / 180.0F);
        double forwardX = -Mth.sin(yawRad);
        double forwardZ = Mth.cos(yawRad);
        // Clamped so a teleport or a chunk reload cannot report an absurd speed.
        return Mth.clamp(
                (float) (dx * forwardX + dz * forwardZ), -MAX_SPEED * 2.5F, MAX_SPEED * 2.5F);
    }

    private void animateWheels() {
        this.lastWheelAngle = this.wheelAngle;
        this.lastSteerAngle = this.steerAngle;
        this.lastAcceleration = this.acceleration;

        float rollingSpeed = this.getEngineSpeed();
        this.wheelAngle = Mth.wrapDegrees(
                this.wheelAngle + rollingSpeed / (2.0F * (float) Math.PI * WHEEL_RADIUS) * 360.0F);

        // Smoothed, because the raw per-tick difference is noisy enough to make
        // the bodyshell flutter rather than squat.
        this.acceleration = Mth.lerp(
                0.3F, this.acceleration, rollingSpeed - this.lastRollingSpeed);
        this.lastRollingSpeed = rollingSpeed;
    }

    /** Smoothed change in speed, in blocks per tick squared. */
    public float getAcceleration(float tickProgress) {
        return Mth.lerp(tickProgress, this.lastAcceleration, this.acceleration);
    }

    /** True while the engine is turning: somebody is aboard and there is fuel. */
    public boolean isEngineRunning() {
        return this.getControllingPassenger() != null && this.hasFuel() && this.canRun();
    }

    private void updateDriving() {
        boolean onGround = this.onGround();
        LivingEntity driver = this.getControllingPassenger();

        if (driver != null) {
            if (!this.hasFuel() || !this.canRun()) {
                // Dry tank, a missing essential or a flat battery: the throttle
                // does nothing, but the car still rolls and steers, so you can
                // coast to the roadside instead of stopping dead.
                this.flyDist *= ENGINE_DRAG;
            } else if (this.pressingForward) {
                float max = this.getMaxSpeed();
                // Past top speed (downhill, say), the engine cannot hold it there.
                this.flyDist = this.flyDist > max
                        ? Math.max(max, this.flyDist * 0.985F)
                        : Math.min(this.flyDist + this.throttleStep(), max);
            } else if (this.pressingBack) {
                this.flyDist = Math.max(this.flyDist - this.throttleStep(), -this.maxReverseSpeed());
            } else {
                this.flyDist *= ENGINE_DRAG;
            }

            if (this.pressingBrake) {
                // Handbrake into a corner at speed: the back steps out
                // instead of the car stopping.
                boolean turning = this.pressingLeft != this.pressingRight;
                float brake = turning && onGround && Math.abs(this.flyDist) > DRIFT_MIN_SPEED
                        ? this.brakeStrength() * DRIFT_BRAKE
                        : this.brakeStrength();
                this.flyDist = approachZero(this.flyDist, brake);
            }

            // A tired set of plugs stumbles. It is brief and it is random, and
            // it is the only symptom on the car you can feel before you can
            // measure it.
            if (this.isEngineRunning() && this.misfiring()) {
                this.flyDist *= 0.82F;
            }
        } else {
            this.flyDist *= onGround ? ROLLING_FRICTION : AIR_DRAG;
        }

        if (Math.abs(this.flyDist) < SPEED_EPSILON) {
            this.flyDist = 0.0F;
        }

        float steerInput = 0.0F;
        if (driver != null) {
            if (this.pressingLeft) {
                steerInput -= 1.0F;
            }
            if (this.pressingRight) {
                steerInput += 1.0F;
            }
        }
        this.steerAngle = Mth.lerp(0.35F, this.steerAngle, steerInput * MAX_STEER_ANGLE);

        boolean drifting = driver != null && this.pressingBrake && onGround && steerInput != 0.0F
                && Math.abs(this.flyDist) > DRIFT_MIN_SPEED;
        if (this.flyDist != 0.0F && steerInput != 0.0F) {
            float grip = Math.min(1.0F, Math.abs(this.flyDist) / FULL_GRIP_SPEED);
            float direction = this.flyDist < 0.0F ? -1.0F : 1.0F;
            float tyres = this.steeringFactor();
            this.setYRot(Mth.wrapDegrees(
                    this.getYRot() + steerInput * MAX_TURN_RATE * tyres * grip * direction
                            * (drifting ? DRIFT_TURN : 1.0F)));
        }

        // The car goes where it is travelling, which in a slide is not where
        // it is pointing. In the air nothing turns the travel at all.
        if (Float.isNaN(this.travelYaw) || Math.abs(this.flyDist) < 0.05F) {
            this.travelYaw = this.getYRot();
        } else if (onGround) {
            this.travelYaw = Mth.rotLerp(
                    drifting ? DRIFT_GRIP : NORMAL_GRIP, this.travelYaw, this.getYRot());
        }
        float yawRad = this.travelYaw * ((float) Math.PI / 180.0F);
        // 0.98 is vanilla's vertical drag; without it a long fall accelerates forever.
        double velocityY = (this.getDeltaMovement().y - this.getGravity()) * 0.98;
        if (onGround && velocityY < 0.0) {
            // Keep the car pinned to the ground instead of jittering on it.
            velocityY = -0.04;
        }
        this.setDeltaMovement(
                -Mth.sin(yawRad) * this.flyDist,
                velocityY,
                Mth.cos(yawRad) * this.flyDist);

        if (this.horizontalCollision) {
            this.flyDist *= 0.5F;
        }
    }

    /**
     * Tyre smoke and squeal whenever the car is seen moving sideways to the
     * way it points. Worked out from where it went, so every client sees
     * every slide, not only the driver.
     */
    private void emitSkid() {
        if (this.skidSoundCooldown > 0) {
            this.skidSoundCooldown--;
        }
        if (!this.hasLastTickPos || !this.onGround()) {
            return;
        }
        double dx = this.getX() - this.lastTickX;
        double dz = this.getZ() - this.lastTickZ;
        double moved = Math.sqrt(dx * dx + dz * dz);
        if (moved < DRIFT_MIN_SPEED * 0.9) {
            return;
        }
        float yawRad = this.getYRot() * ((float) Math.PI / 180.0F);
        double forwardX = -Mth.sin(yawRad);
        double forwardZ = Mth.cos(yawRad);
        double along = Math.abs(dx * forwardX + dz * forwardZ) / moved;
        if (along > 0.97) {
            return;
        }
        double back = this.getBbWidth() * 0.45;
        for (int side = -1; side <= 1; side += 2) {
            double x = this.getX() - forwardX * back + forwardZ * side * 0.7;
            double z = this.getZ() - forwardZ * back - forwardX * side * 0.7;
            this.level().addParticle(ParticleTypes.CLOUD, x, this.getY() + 0.15, z,
                    (this.random.nextDouble() - 0.5) * 0.04, 0.02, (this.random.nextDouble() - 0.5) * 0.04);
        }
        if (this.skidSoundCooldown == 0) {
            this.skidSoundCooldown = 9;
            this.level().playLocalSound(this.getX(), this.getY(), this.getZ(),
                    ModSounds.TYRE_SKID, SoundSource.NEUTRAL, 0.55F,
                    0.9F + this.random.nextFloat() * 0.2F, false);
        }
    }

    private static float approachZero(float value, float amount) {
        if (value > 0.0F) {
            return Math.max(0.0F, value - amount);
        }
        return Math.min(0.0F, value + amount);
    }

    private void pushNearbyEntities() {
        List<Entity> nearby = this.level().getEntities(
                this,
                this.getBoundingBox().inflate(0.2F, -0.01F, 0.2F),
                EntitySelector.pushableBy(this));
        for (Entity entity : nearby) {
            if (!entity.hasPassenger(this) && !this.hitchedWith(entity)) {
                this.push(entity);
            }
        }
    }

    /** Called on the controlling client every tick with that player's held keys. */
    public void setInputs(Input input) {
        this.pressingForward = input.forward();
        this.pressingBack = input.backward();
        this.pressingLeft = input.left();
        this.pressingRight = input.right();
        this.pressingBrake = input.jump();
    }

    /**
     * Sounds the horn for everyone nearby. Server-side; rate limited so holding
     * the key down cannot spam the sound engine.
     */
    public void honk() {
        if (this.hornCooldown > 0 || this.level().isClientSide()) {
            return;
        }
        this.hornCooldown = HORN_COOLDOWN_TICKS;
        this.level().playSound(
                null,
                this.getX(), this.getY() + 0.8, this.getZ(),
                ModSounds.CAR_HORN,
                SoundSource.NEUTRAL,
                1.4F,
                1.0F);
    }

    /**
     * Burns fuel for one tick. Only an occupied car burns anything, so a parked
     * car keeps its tank indefinitely — unless the tank is holed, which drips
     * whoever is aboard.
     *
     * <p>What it burns follows what the engine is being asked for. The
     * throttle only exists on the driver's client, so the server reads it off
     * the car: a car losing speed has the foot off the pedal and burns idle; a
     * car going uphill is working harder than its speed says; a trailer on the
     * hitch or a load in the back is weight the engine is pulling.
     */
    private void burnFuel(float travelledSpeed) {
        float fuel = this.getFuel();
        double y = this.getY();
        if (Double.isNaN(this.lastBurnY)) {
            this.lastBurnY = y;
        }
        // Smoothed, because a step up is a whole half-block in one tick.
        this.climbRate += ((float) (y - this.lastBurnY) - this.climbRate) * 0.15F;
        this.lastBurnY = y;
        boolean slowing = Math.abs(travelledSpeed) < Math.abs(this.lastBurnSpeed) - COAST_MARGIN;
        this.lastBurnSpeed = travelledSpeed;

        if (this.isLeaking() && fuel > 0.0F && !this.isElectric()) {
            fuel -= LEAK_PER_TICK;
            this.setFuel(fuel);
        }
        if (this.getControllingPassenger() == null || fuel <= 0.0F) {
            return;
        }
        if (this.isElectric() && slowing && this.climbRate <= 0.0F) {
            // A motor turns into a dynamo off the throttle: no idle to pay for,
            // and a little back into the pack every time it slows.
            this.setFuel(fuel + FUEL_BURN_IDLE * REGEN_SHARE);
            return;
        }
        float burn = slowing && this.climbRate <= 0.0F
                ? FUEL_BURN_IDLE * this.thirstFactor() * this.loadThirst()
                : this.fuelPerTick(travelledSpeed, this.climbRate);
        this.setFuel(fuel - burn * (float) me.trossty.car.CarConfig.get().fuelUse);
        this.watchTheGauge();
    }

    /** Fuel a tick, at this speed and this rate of climb, with the car as it is. */
    public float fuelPerTick(float speed, float climb) {
        float load = Math.min(1.0F, Math.abs(speed) / MAX_SPEED + Math.max(0.0F, climb) * CLIMB_LOAD);
        return (FUEL_BURN_IDLE + FUEL_BURN_DRIVING * load) * this.thirstFactor() * this.loadThirst();
    }

    /**
     * How much the weight the engine is pulling adds: a trailer here, a load
     * in the back of the machines that carry one.
     */
    public float loadThirst() {
        return this.towed != null ? TOWING_THIRST : 1.0F;
    }

    /**
     * How far the fuel in the tank goes, in blocks, at this speed on the flat.
     * Standing still counts as cruising at three fifths of top speed — "how far
     * could I get", not "how long can I sit here".
     */
    public float rangeBlocks(float speed) {
        float cruise = Math.max(Math.abs(speed), this.getRatedTopSpeed() * 0.6F);
        float perBlock = this.fuelPerTick(cruise, 0.0F) / Math.max(0.01F, cruise);
        return this.getFuel() / Math.max(1.0E-6F, perBlock);
    }

    /**
     * The chime as the needle drops into the last tenth, and the cough once
     * the pickup is sucking air.
     */
    private void watchTheGauge() {
        float fraction = this.getFuelFraction();
        if (fraction > LOW_FUEL_REARM) {
            this.lowFuelWarned = false;
        } else if (fraction < LOW_FUEL_FRACTION && !this.lowFuelWarned) {
            this.lowFuelWarned = true;
            this.level().playSound(null, this.getX(), this.getY() + 1.0, this.getZ(),
                    SoundEvents.NOTE_BLOCK_CHIME.value(), SoundSource.NEUTRAL, 0.9F, 1.6F);
            if (this.getControllingPassenger() instanceof Player driver) {
                driver.displayClientMessage(Component.translatable("message.car.low_fuel",
                        formatDistance(this.rangeBlocks(this.getEngineSpeed()))), true);
            }
        }
        if (fraction < STARVING_FRACTION && this.isEngineRunning()
                && this.random.nextFloat() < STARVE_CHANCE) {
            this.level().broadcastEntityEvent(this, STATUS_SPUTTER);
            this.level().playSound(null, this.getX(), this.getY() + 0.6, this.getZ(),
                    SoundEvents.FIRE_EXTINGUISH, SoundSource.NEUTRAL, 0.35F, 0.55F);
        }
    }

    /**
     * How far a new one of these goes on a full tank, cruising at three fifths
     * of its top speed: the figure on the showroom card. A real car drifts off
     * it as its filter clogs and its owner fits a V8.
     */
    public static float estimateRange(me.trossty.car.vehicle.VehicleDefinition defined) {
        int setup = defined.factory().setup();
        float thirst = defined.performance().fuelUse()
                * CarSetup.layout(setup).thirst()
                * CarSetup.engineFuel(setup).thirst()
                * (defined.factory().turbo() ? CarSetup.TURBO_THIRST : 1.0F);
        float cruise = Math.min(MAX_SPEED, MAX_SPEED * defined.performance().speed()) * 0.6F;
        float perTick = (FUEL_BURN_IDLE + FUEL_BURN_DRIVING * cruise / MAX_SPEED) * thirst;
        return defined.tank() / me.trossty.car.item.JerryCans.LITRES_PER_UNIT / (perTick / cruise);
    }

    /** A distance in blocks, as a person would say it: metres, then kilometres. */
    public static Component formatDistance(float blocks) {
        if (blocks < 1000.0F) {
            return Component.translatable("unit.car.metres", Math.round(blocks / 10.0F) * 10);
        }
        float km = blocks / 1000.0F;
        return Component.translatable("unit.car.km", km < 10.0F
                ? String.format(java.util.Locale.ROOT, "%.1f", km)
                : String.valueOf(Math.round(km)));
    }

    /** The tank, in fuel units: from the definition's litres, or a family car's. */
    public float getMaxFuel() {
        me.trossty.car.vehicle.VehicleDefinition defined = this.getDefinition();
        return defined == null ? MAX_FUEL : defined.tank() / me.trossty.car.item.JerryCans.LITRES_PER_UNIT;
    }

    public float getFuel() {
        float fuel = this.entityData.get(FUEL);
        return fuel < 0.0F ? this.getMaxFuel() : fuel;
    }

    public void setFuel(float fuel) {
        this.entityData.set(FUEL, Mth.clamp(fuel, 0.0F, this.getMaxFuel()));
        if (this.getFuel() <= 0.0F) {
            this.setDirtyFuel(0.0F);   // an empty tank holds nothing, clean or dirty
        }
    }

    /** How much of the tank is raw fuel, 0 to 1. */
    public float getDirtyFuel() {
        return this.entityData.get(DIRTY_FUEL);
    }

    public void setDirtyFuel(float fraction) {
        this.entityData.set(DIRTY_FUEL, Mth.clamp(fraction, 0.0F, 1.0F));
    }

    public boolean hasFuel() {
        return this.getFuel() > 0.0F;
    }

    public boolean isLeaking() {
        return this.entityData.get(LEAKING);
    }

    public void setLeaking(boolean leaking) {
        this.entityData.set(LEAKING, leaking);
    }

    /** Fraction of a full tank, for the gauge. */
    public float getFuelFraction() {
        float max = this.getMaxFuel();
        return max <= 0.0F ? 0.0F : this.getFuel() / max;
    }

    /**
     * Pours fuel in and reports how much the tank actually took, so a jerry can
     * is not spent on a tank that is already full.
     */
    public float addFuel(float amount) {
        return this.addFuel(amount, CarSetup.Fuel.PETROL);
    }

    /**
     * The same, saying what is being poured. A tank holds one thing: into an
     * empty one anything goes, and from then on only more of the same does,
     * so the two are never mixed. What the engine makes of it is the engine's
     * business — see {@link #hasWrongFuel()}.
     */
    public float addFuel(float amount, CarSetup.Fuel fuel) {
        return this.addFuel(amount, fuel, false);
    }

    /** The same, saying whether it is raw: what goes in mixes with what is there by volume. */
    public float addFuel(float amount, CarSetup.Fuel fuel, boolean raw) {
        float before = this.getFuel();
        float dirtyBefore = this.getDirtyFuel();
        float accepted = this.addFuelOf(amount, fuel);
        if (accepted > 0.0F) {
            this.setDirtyFuel((dirtyBefore * before + (raw ? accepted : 0.0F)) / (before + accepted));
        }
        return accepted;
    }

    private float addFuelOf(float amount, CarSetup.Fuel fuel) {
        if (this.hasFuel() && this.getTankFuel() != fuel) {
            return 0.0F;
        }
        // An electric car has a charging socket and no filler neck, and the
        // other way round: charge only goes into a battery, liquid only into a tank.
        if (fuel.isElectric() != this.isElectric()) {
            return 0.0F;
        }
        float accepted = Math.min(amount, this.getMaxFuel() - this.getFuel());
        if (accepted > 0.0F) {
            this.setSetup(CarSetup.withTank(this.getSetup(), fuel));
            this.setFuel(this.getFuel() + accepted);
        }
        return accepted;
    }

    /** What a jerry can holds; a can that says nothing holds petrol. */
    public static CarSetup.Fuel fuelIn(ItemStack can) {
        Integer stored = can.get(ModComponents.FUEL_KIND);
        return stored == null ? CarSetup.Fuel.PETROL : CarSetup.Fuel.byOrdinal(stored);
    }

    /**
     * A can with something other than petrol in it — diesel or biofuel — which
     * nothing at a petrol station will take.
     */
    public static boolean holdsDiesel(ItemStack can) {
        return can.is(ModItems.JERRY_CAN) && can.getDamageValue() < can.getMaxDamage()
                && fuelIn(can) != CarSetup.Fuel.PETROL;
    }

    /** Whether a can holds raw fuel, straight off the refinery. */
    public static boolean isRaw(ItemStack can) {
        return Boolean.TRUE.equals(can.get(ModComponents.FUEL_RAW));
    }

    /** Marks a can raw, or clean. */
    public static void markRaw(ItemStack can, boolean raw) {
        if (raw) {
            can.set(ModComponents.FUEL_RAW, true);
        } else {
            can.remove(ModComponents.FUEL_RAW);
        }
    }

    /** Marks a can with what has just gone into it: clean fuel, unless {@link #markRaw} says otherwise. */
    public static void markFuel(ItemStack can, CarSetup.Fuel fuel) {
        can.remove(ModComponents.FUEL_RAW);
        if (fuel == CarSetup.Fuel.PETROL) {
            can.remove(ModComponents.FUEL_KIND);
        } else {
            can.set(ModComponents.FUEL_KIND, fuel.ordinal());
        }
    }

    public DyeColor getColor() {
        return DyeColor.byId(this.entityData.get(COLOR));
    }

    public void setColor(DyeColor color) {
        this.entityData.set(COLOR, color.getId());
        this.entityData.set(PAINT, -1);
    }

    /** The sprayed colour as 0xRRGGBB, or -1 for the plain dye colour. */
    public int getPaint() {
        return this.entityData.get(PAINT);
    }

    public void setPaint(int rgb) {
        this.entityData.set(PAINT, rgb < 0 ? -1 : rgb & 0xFFFFFF);
    }

    /** Signed speed in blocks per tick, valid on both sides. */
    public float getEngineSpeed() {
        return this.isLocalInstanceAuthoritative()
                ? this.flyDist
                : this.entityData.get(ENGINE_SPEED);
    }

    /**
     * Sets the car rolling at {@code speed} blocks a tick along its own heading.
     *
     * <p>Only meaningful on the side simulating the car, which is the driver's
     * client while somebody is aboard and the server otherwise. It exists for
     * the tests, which need a car already up to speed without a driver to hold
     * the throttle — the same state a car is left in when its driver hops out
     * at speed.
     */
    public void setSpeed(float speed) {
        this.flyDist = Mth.clamp(speed, -MAX_REVERSE_SPEED, MAX_SPEED);
    }

    public float getWheelAngle(float tickProgress) {
        return Mth.lerp(tickProgress, this.lastWheelAngle, this.wheelAngle);
    }

    /** Client status: the car was locked, and blinks once. */
    public static final byte STATUS_LOCKED = 101;
    /** Client status: the car was unlocked, and blinks twice. */
    public static final byte STATUS_UNLOCKED = 102;
    private static final int LOCK_FLASH_TICKS = 10;
    private static final String INDICATOR_COLOUR = "#E8901B";
    private int lockFlashTicks;
    private boolean lockFlashTwice;
    /** How far into its parked pose the vehicle has settled, 0 to 1; client only. */
    private float restPose;
    private float prevRestPose;

    /**
     * Where the parked pose is heading: 1 when the vehicle should sit the way
     * it does when nobody is using it (a bike on its stand, a trailer down on
     * its jockey wheel), 0 otherwise. Plain cars have no such pose.
     */
    protected float restPoseTarget() {
        return 0.0F;
    }

    public float getRestPose(float tickProgress) {
        return Mth.lerp(tickProgress, this.prevRestPose, this.restPose);
    }

    @Override
    public void handleEntityEvent(byte status) {
        if (status == STATUS_LOCKED || status == STATUS_UNLOCKED) {
            this.lockFlashTicks = LOCK_FLASH_TICKS;
            this.lockFlashTwice = status == STATUS_UNLOCKED;
            return;
        }
        if (status == STATUS_SPUTTER) {
            // The driver's client is the one moving the car: it feels the cough.
            if (this.isLocalInstanceAuthoritative()) {
                this.flyDist *= STARVE_STUMBLE;
            }
            this.coughExhaust();
            return;
        }
        super.handleEntityEvent(status);
    }

    /**
     * The indicators blink, the way a car answers its remote: a burst of
     * orange at every amber lamp its design has, or at its four corners if it
     * has none.
     */
    private void flashIndicators() {
        Level world = this.level();
        float yawRad = this.getYRot() * ((float) Math.PI / 180.0F);
        net.minecraft.core.particles.DustParticleOptions amber =
                new net.minecraft.core.particles.DustParticleOptions(0xFFA51F, 0.8F);
        List<Vec3> lamps = new java.util.ArrayList<>();
        me.trossty.car.vehicle.VehicleDefinition definition = this.getDefinition();
        if (definition != null) {
            for (me.trossty.car.vehicle.VehicleDefinition.Box box : definition.shape()) {
                if (INDICATOR_COLOUR.equalsIgnoreCase(box.color())) {
                    lamps.add(new Vec3(
                            (box.from().x() + box.to().x()) / 32.0,
                            (box.from().y() + box.to().y()) / 32.0,
                            (box.from().z() + box.to().z()) / 32.0));
                }
            }
        }
        if (lamps.isEmpty()) {
            double side = this.getBbWidth() / 2.0;
            double reach = Math.max(0.5, this.frontReach());
            for (int corner = 0; corner < 4; corner++) {
                lamps.add(new Vec3(corner % 2 == 0 ? side : -side, 0.7, corner < 2 ? reach : -reach));
            }
        }
        for (Vec3 lamp : lamps) {
            Vec3 at = this.position().add(lamp.yRot(-yawRad));
            for (int puff = 0; puff < 4; puff++) {
                world.addParticle(amber,
                        at.x + (this.random.nextDouble() - 0.5) * 0.12,
                        at.y + (this.random.nextDouble() - 0.5) * 0.12,
                        at.z + (this.random.nextDouble() - 0.5) * 0.12,
                        0.0, 0.0, 0.0);
            }
            world.addParticle(ParticleTypes.WAX_ON, at.x, at.y, at.z, 0.0, 0.0, 0.0);
        }
    }

    public float getSteerAngle(float tickProgress) {
        return Mth.lerp(tickProgress, this.lastSteerAngle, this.steerAngle);
    }

    // ---- the parts underneath ----

    /**
     * Condition of one part, {@link CarPart#MAX_CONDITION} down to 0.
     *
     * <p>Zero is not "worn out", it is "not on the car". Wear bottoms out at
     * {@link CarPart#WORN_OUT}, so the two are never confused: a finished
     * gearbox is still a gearbox, and the car still has one.
     */
    public float getCondition(CarPart part) {
        return this.entityData.get(PART_CONDITION.get(part.ordinal()));
    }

    public void setCondition(CarPart part, float condition) {
        this.entityData.set(PART_CONDITION.get(part.ordinal()),
                Mth.clamp(condition, 0.0F, CarPart.MAX_CONDITION));
    }

    /** Whether the part is on the car at all. */
    public boolean isFitted(CarPart part) {
        return this.getCondition(part) > 0.0F;
    }

    /** 0 for a dead or absent part, 1 for a fresh one. */
    public float getConditionFraction(CarPart part) {
        return this.getCondition(part) / CarPart.MAX_CONDITION;
    }

    /** What the part in this slot was built to. */
    public CarPart.Quality getQuality(CarPart part) {
        int bits = this.entityData.get(PART_QUALITY);
        return CarPart.Quality.byOrdinal((bits >>> (part.ordinal() * QUALITY_BITS)) & QUALITY_MASK);
    }

    public void setQuality(CarPart part, CarPart.Quality quality) {
        this.entityData.set(PART_QUALITY,
                withQuality(this.entityData.get(PART_QUALITY), part, quality));
    }

    /**
     * The same packing, without a car. The lift needs it: it stamps the grades
     * of the parts it swallowed onto the car it hands back, before that car is
     * anything but an item.
     */
    public static int withQuality(int bits, CarPart part, CarPart.Quality quality) {
        int shift = part.ordinal() * QUALITY_BITS;
        return (bits & ~(QUALITY_MASK << shift)) | (quality.ordinal() << shift);
    }

    /** Every slot's quality at once, for saving and for pocketing. */
    public int getQualityBits() {
        return this.entityData.get(PART_QUALITY);
    }

    public void setQualityBits(int bits) {
        this.entityData.set(PART_QUALITY, bits);
    }

    /**
     * Wear, which is the one place quality is felt. Every rule above hands its
     * number to this method, so a cheap part wears faster at everything it does
     * without a single one of those rules having to know about quality.
     */
    private void wear(CarPart part, float amount) {
        if (amount > 0.0F && this.isFitted(part)) {
            this.setCondition(part, Math.max(CarPart.WORN_OUT,
                    this.getCondition(part) - amount * this.getQuality(part).wearFactor() * this.ageFactor(part)
                            * (float) me.trossty.car.CarConfig.get().partWear));
        }
    }

    /**
     * How much faster than new this part wears now: an old car wears
     * everything faster, and a part on its last legs goes quicker still, so
     * putting off a service costs more than the service would have.
     */
    public float ageFactor(CarPart part) {
        return mileageFactor(this.getOdometer()) * (1.0F + TIRED_WEAR * (1.0F - this.getConditionFraction(part)));
    }

    /** 1 new, rising to {@code 1 + MILEAGE_WEAR} at {@link #HIGH_MILEAGE}, and no further. */
    public static float mileageFactor(float odometer) {
        return 1.0F + MILEAGE_WEAR * Mth.clamp(odometer / HIGH_MILEAGE, 0.0F, 1.0F);
    }

    /** Blocks driven in this car's life. */
    public float getOdometer() {
        return this.entityData.get(ODOMETER);
    }

    public void setOdometer(float blocks) {
        this.entityData.set(ODOMETER, Math.max(0.0F, blocks));
    }

    /** 1 with a fresh part, {@code worn} with a dead one, a line between. */
    private float partFactor(CarPart part, float worn) {
        return this.partFactor(part, worn, worn);
    }

    /**
     * The same, with its own answer for a part that is not there. Used for
     * penalties as well as for losses: a value above 1 reads as "this costs
     * you more", which is what a blocked air filter does to the fuel bill.
     */
    private float partFactor(CarPart part, float worn, float missing) {
        if (!this.isFitted(part)) {
            return missing;
        }
        return Mth.lerp(this.getConditionFraction(part), worn, 1.0F);
    }

    /**
     * What the grade of a fitted part does to the job that part does. A grade
     * is not only how long a part lasts: a performance engine is a faster
     * engine, and economy pads are worse brakes from the day they go on.
     */
    public float grade(CarPart part) {
        return this.isFitted(part) ? this.getQuality(part).performance() : 1.0F;
    }

    /** The same at half strength, for the parts that only help the job along. */
    private float minorGrade(CarPart part) {
        return 1.0F + (this.grade(part) - 1.0F) * 0.5F;
    }

    /** Whether the grade of this part changes how the car drives, not just how long it lasts. */
    public static boolean gradeIsFelt(CarPart part) {
        return switch (part) {
            case ENGINE, EXHAUST, AIR_FILTER, TRANSMISSION, CLUTCH, DRIVESHAFT,
                 BRAKE_PADS, BRAKE_DISCS, WHEELS, STEERING_RACK, RADIATOR -> true;
            default -> false;
        };
    }

    // ---- what kind of car this is ----

    public int getSetup() {
        return this.entityData.get(SETUP);
    }

    public void setSetup(int setup) {
        this.entityData.set(SETUP, setup);
    }

    /** Which kind of part is in this slot; 0 for the parts that have no kinds. */
    public int getVariant(CarPart part) {
        return CarSetup.variant(this.getSetup(), part);
    }

    public void setVariant(CarPart part, int variant) {
        this.setSetup(CarSetup.withVariant(this.getSetup(), part, variant));
    }

    public CarSetup.Layout getLayout() {
        return CarSetup.layout(this.getSetup());
    }

    /** What the engine burns. */
    public CarSetup.Fuel getEngineFuel() {
        return CarSetup.engineFuel(this.getSetup());
    }

    /** What is actually in the tank. */
    public CarSetup.Fuel getTankFuel() {
        return CarSetup.tank(this.getSetup());
    }

    /** Whether this runs on a battery pack: an electric motor in the bay. */
    public boolean isElectric() {
        return CarSetup.electric(this.getSetup());
    }

    /**
     * Charge from a post: units of the same gauge fuel is measured in, so the
     * dash, the range and the service screen read a battery like a tank.
     */
    public float charge(float units) {
        return this.addFuel(units, CarSetup.Fuel.ELECTRIC);
    }

    public CarSetup.Tyre getTyre() {
        return CarSetup.tyre(this.getSetup());
    }

    public boolean hasOpenFilter() {
        return this.isFitted(CarPart.AIR_FILTER) && CarSetup.openFilter(this.getSetup());
    }

    /** A turbo is bolted to the engine, so without an engine there is none. */
    public boolean hasTurbo() {
        return this.isFitted(CarPart.ENGINE) && CarSetup.turbo(this.getSetup()) && !this.isElectric();
    }

    public void setTurbo(boolean fitted) {
        this.setSetup(CarSetup.withTurbo(this.getSetup(), fitted));
    }

    /** A spare with no variant on it is the plain kind. */
    public static int variantOf(ItemStack spare) {
        Integer stored = spare.get(ModComponents.PART_VARIANT);
        return stored == null ? 0 : stored;
    }

    /** The wrong stuff in the tank: there is fuel, and the engine cannot burn it. */
    public boolean hasWrongFuel() {
        return this.hasFuel() && this.isFitted(CarPart.ENGINE)
                && !this.getEngineFuel().burns(this.getTankFuel());
    }

    /** Tyres and steering on a laid road wear at this much of what they do on anything else. */
    public static final float ASPHALT_WEAR = 0.6F;

    /** Whether the wheels are on laid asphalt; a slab counts from inside its own block. */
    public boolean onAsphalt() {
        return me.trossty.car.registry.ModBlocks.isAsphalt(this.level().getBlockState(this.blockPosition()))
                || me.trossty.car.registry.ModBlocks.isAsphalt(this.level().getBlockState(this.blockPosition().below()));
    }

    /** What the wheels are on right now. */
    public CarSetup.Surface getSurface() {
        // A layer of snow is a block the car stands in, not on.
        net.minecraft.world.level.block.state.BlockState around = this.level().getBlockState(this.blockPosition());
        if (around.is(net.minecraft.tags.BlockTags.SNOW)) {
            return CarSetup.Surface.SNOW;
        }
        return CarSetup.Surface.of(
                this.level().getBlockState(this.blockPosition().below()));
    }

    /** How well the fitted tyres hold the ground the car is on. */
    public float getGrip() {
        CarSetup.Surface surface = this.getSurface();
        float grip = this.getTyre().grip(surface);
        float offroad = this.tuning().offroad();
        if (surface != CarSetup.Surface.ASPHALT && offroad > 0.0F && grip < 1.0F) {
            // Ground clearance and four driven wheels give back what the tyre loses.
            grip = Mth.lerp(offroad, grip, 1.0F);
        }
        return grip;
    }

    /** Grip as far as putting power down goes: a sticky tyre adds none. */
    private float traction() {
        return Math.min(1.0F, this.getGrip());
    }

    /** What everybody aboard but the driver costs in acceleration. */
    private float loadFactor() {
        int riders = Math.max(0, this.getPassengers().size() - 1);
        return (float) Math.pow(CarSetup.PULL_PER_PASSENGER, riders);
    }

    /**
     * Where seat number {@code seat} is, as (sideways, up, forward) in the
     * car's own frame. Seat 0 is the driver's.
     */
    protected Vec3 seatPosition(int seat) {
        me.trossty.car.vehicle.VehicleDefinition defined = this.getDefinition();
        if (defined != null && !defined.seats().isEmpty()) {
            Vec3 at = defined.seats().get(Math.min(seat, defined.seats().size() - 1));
            return at.scale(1.0 / 16.0);
        }
        return this.plainSeatPosition(seat);
    }

    /** Where this machine seats people when no data pack says otherwise. */
    protected Vec3 plainSeatPosition(int seat) {
        return new Vec3(seat == 0 ? -SEAT_SIDEWAYS : SEAT_SIDEWAYS, SEAT_HEIGHT, SEAT_FORWARD);
    }

    /** How many can ride, the driver included. */
    protected int seatCount() {
        me.trossty.car.vehicle.VehicleDefinition defined = this.getDefinition();
        if (defined != null && !defined.seats().isEmpty()) {
            return defined.seats().size();
        }
        return this.plainSeatCount();
    }

    protected int plainSeatCount() {
        return 2;
    }

    // ---- what a car needs before it will go anywhere ----

    /** The first essential part this car has not got, or null. */
    public CarPart missingEssential() {
        for (CarPart part : CarPart.values()) {
            if (part == CarPart.SPARK_PLUGS && !this.getEngineFuel().needsSpark()
                    && this.isFitted(CarPart.ENGINE)) {
                // A diesel lights itself.
                continue;
            }
            if (part.essential() && !this.isFitted(part)) {
                return part;
            }
        }
        return null;
    }

    /** A flat battery is not a missing one, and stops the car just as dead. */
    public boolean isBatteryFlat() {
        return this.getCondition(CarPart.BATTERY) <= FLAT_BATTERY;
    }

    /** Whether the engine can turn at all, fuel aside. */
    public boolean canRun() {
        return this.missingEssential() == null && !this.isBatteryFlat() && !this.hasWrongFuel()
                && !this.entityData.get(KEY_MISSING);
    }

    /**
     * Says the one thing worth saying, on the action bar, and not often. A
     * driver who can feel the car misbehaving should be told what it is before
     * they have to go and find a clipboard.
     */
    private void warnTheDriver() {
        if (!(this.getControllingPassenger() instanceof Player driver)
                || this.tickCount % 80 != 0) {
            return;
        }
        CarPart missing = this.missingEssential();
        if (missing != null) {
            driver.displayClientMessage(Component.translatable("message.car.wont_start",
                    Component.translatable("part.car." + missing.key())), true);
        } else if (this.entityData.get(KEY_MISSING)) {
            driver.displayClientMessage(Component.translatable("message.car.need_key"), true);
        } else if (this.isBatteryFlat()) {
            driver.displayClientMessage(Component.translatable("gui.car.flat_battery"), true);
        } else if (this.hasWrongFuel()) {
            driver.displayClientMessage(Component.translatable("message.car.wrong_fuel",
                    Component.translatable("fuel.car." + this.getTankFuel().key()),
                    Component.translatable("fuel.car." + this.getEngineFuel().key())), true);
        } else if (this.isOverheating()) {
            driver.displayClientMessage(Component.translatable("message.car.overheating"), true);
        } else if (this.isLeaking() && this.hasFuel()) {
            driver.displayClientMessage(Component.translatable("message.car.tank_leaking"), true);
        }
    }

    /** How hot the engine is: 0 cold, 1 boiling. */
    public float getEngineTemp() {
        return this.entityData.get(ENGINE_TEMP);
    }

    public boolean isOverheating() {
        return this.getEngineTemp() >= OVERHEAT_AT;
    }

    /**
     * The temperature chases where the load and the radiator say it belongs. A
     * sound radiator settles a flat-out car at about two thirds; a furred one
     * cannot, which is the only way overheating ever happens.
     */
    private void tickTemperature() {
        float temp = this.getEngineTemp();
        float target = 0.0F;
        if (this.isEngineRunning()) {
            float load = Math.min(1.0F, Math.abs(this.getEngineSpeed()) / MAX_SPEED);
            float cooling = this.partFactor(
                    CarPart.RADIATOR, WORN_RADIATOR_COOLING, MISSING_RADIATOR_COOLING);
            float heat = this.getLayout().heat() * (this.hasTurbo() ? CarSetup.TURBO_HEAT : 1.0F);
            target = Math.min(1.0F,
                    (0.25F + 0.40F * load * heat) / (cooling * this.grade(CarPart.RADIATOR)));
        }
        float rate = target > temp ? TEMP_RISE : TEMP_FALL;
        this.entityData.set(ENGINE_TEMP, Mth.lerp(rate, temp, target));
    }

    // ---- what each part is worth on the road ----

    /** How much speed one tick of throttle adds, through the whole driveline. */
    public float throttleStep() {
        return ACCELERATION * this.tuning().acceleration()
                * this.partFactor(CarPart.TRANSMISSION, WORN_GEARBOX_DRIVE)
                * this.partFactor(CarPart.CLUTCH, WORN_CLUTCH_DRIVE)
                * this.partFactor(CarPart.DRIVESHAFT, WORN_SHAFT_DRIVE)
                * this.partFactor(CarPart.ENGINE, 0.8F)
                * this.grade(CarPart.ENGINE)
                * this.grade(CarPart.TRANSMISSION)
                * this.grade(CarPart.CLUTCH)
                * this.minorGrade(CarPart.DRIVESHAFT)
                * (this.getEngineFuel().needsSpark()
                        ? this.partFactor(CarPart.SPARK_PLUGS, WORN_PLUGS_THROTTLE) : 1.0F)
                * this.getLayout().pull()
                * this.getEngineFuel().pull()
                * (this.hasTurbo() ? CarSetup.TURBO_PULL : 1.0F)
                * (this.hasOpenFilter() ? CarSetup.OPEN_FILTER_PULL : 1.0F)
                * this.traction()
                * this.loadFactor();
    }

    private float maxReverseSpeed() {
        return MAX_REVERSE_SPEED * this.partFactor(CarPart.TRANSMISSION, WORN_GEARBOX_REVERSE);
    }

    /**
     * Braking, which is three parts deep: the pads do the work, the discs they
     * press on matter, and whatever either of them manages still has to reach
     * the road through the tyres.
     */
    public float brakeStrength() {
        return BRAKE_STRENGTH * this.tuning().braking()
                * this.partFactor(CarPart.BRAKE_PADS, WORN_PADS_BRAKE, MISSING_PADS_BRAKE)
                * this.partFactor(CarPart.BRAKE_DISCS, WORN_DISCS_BRAKE, MISSING_DISCS_BRAKE)
                * this.partFactor(CarPart.WHEELS, WORN_TYRE_GRIP)
                * this.grade(CarPart.BRAKE_PADS)
                * this.grade(CarPart.BRAKE_DISCS)
                * this.minorGrade(CarPart.WHEELS)
                * this.getGrip();
    }

    /** Turn rate: the rack points the wheels, the tyres decide what happens. */
    public float steeringFactor() {
        return this.tuning().steering() * this.partFactor(CarPart.WHEELS, WORN_TYRE_GRIP)
                * this.partFactor(CarPart.STEERING_RACK, WORN_RACK_STEER, MISSING_RACK_STEER)
                * this.grade(CarPart.STEERING_RACK)
                * this.minorGrade(CarPart.WHEELS)
                * this.getGrip();
    }

    /** How much more fuel this car drinks than a clean one would. */
    public float thirstFactor() {
        return this.tuning().fuelUse() * this.partFactor(CarPart.AIR_FILTER, WORN_FILTER_THIRST, MISSING_FILTER_THIRST)
                * this.getLayout().thirst()
                * this.getEngineFuel().thirst()
                // Biofuel is weaker stuff: whatever the engine, more of it goes.
                * (this.getTankFuel() == CarSetup.Fuel.BIOFUEL ? CarSetup.Fuel.BIOFUEL.thirst() : 1.0F)
                * (this.hasTurbo() ? CarSetup.TURBO_THIRST : 1.0F)
                * (this.hasOpenFilter() ? CarSetup.OPEN_FILTER_THIRST : 1.0F);
    }

    /** How much faster dirty oil wears the things it is supposed to protect. */
    private float oilWearFactor() {
        return this.partFactor(CarPart.OIL_FILTER, WORN_OIL_WEAR, MISSING_OIL_WEAR);
    }

    /**
     * Miles wear everything that turns, and the engine wears even at a
     * standstill because it is still running. Clean oil is what decides how
     * fast the two expensive items go, which is the point of a filter that
     * costs two ingots and some paper.
     */
    private void wearParts() {
        float load = Math.min(1.0F, Math.abs(this.getEngineSpeed()) / MAX_SPEED);
        float oil = this.oilWearFactor();
        if (this.isEngineRunning()) {
            float running = IDLE_WEAR_SHARE + (1.0F - IDLE_WEAR_SHARE) * load;
            // Boost and unfiltered air are both paid for out of the engine.
            float hardLife = (this.hasTurbo() ? CarSetup.TURBO_ENGINE_WEAR : 1.0F)
                    * (this.hasOpenFilter() ? CarSetup.OPEN_FILTER_ENGINE_WEAR : 1.0F);
            float dirty = this.getDirtyFuel();
            this.wear(CarPart.ENGINE, CarPart.ENGINE.wearPerTick() * running * oil * hardLife
                    * (1.0F + DIRTY_ENGINE_WEAR * dirty));
            if (this.getEngineFuel().needsSpark()) {
                this.wear(CarPart.SPARK_PLUGS, CarPart.SPARK_PLUGS.wearPerTick() * running
                        * (1.0F + DIRTY_PLUG_WEAR * dirty));
            }
            this.wear(CarPart.AIR_FILTER, CarPart.AIR_FILTER.wearPerTick() * running
                    * (this.hasOpenFilter() ? CarSetup.OPEN_FILTER_OWN_WEAR : 1.0F));
            this.wear(CarPart.OIL_FILTER, CarPart.OIL_FILTER.wearPerTick() * running);
            this.wear(CarPart.EXHAUST, CarPart.EXHAUST.wearPerTick() * running);
            this.wear(CarPart.ALTERNATOR, CarPart.ALTERNATOR.wearPerTick() * running);
            // A radiator that is already struggling cooks itself faster.
            this.wear(CarPart.RADIATOR,
                    CarPart.RADIATOR.wearPerTick() * running * (1.0F + this.getEngineTemp()));
            // The battery only holds its charge while something is charging it.
            this.wear(CarPart.BATTERY,
                    BATTERY_DRAIN * (1.0F - this.getConditionFraction(CarPart.ALTERNATOR)));
        }

        // Everything that turns wears whenever it turns. A car being pushed
        // down the road is still using its tyres.
        if (load > 0.0F) {
            this.wear(CarPart.TRANSMISSION, CarPart.TRANSMISSION.wearPerTick() * load * oil);
            this.wear(CarPart.CLUTCH, CarPart.CLUTCH.wearPerTick() * load);
            this.wear(CarPart.DRIVESHAFT, CarPart.DRIVESHAFT.wearPerTick() * load);
            float road = this.onAsphalt() ? ASPHALT_WEAR : 1.0F;
            this.wear(CarPart.WHEELS, CarPart.WHEELS.wearPerTick() * load * this.getTyre().wear() * road);
            this.wear(CarPart.STEERING_RACK, CarPart.STEERING_RACK.wearPerTick() * load * road);
        }
    }

    /**
     * Brake wear, taken from the speed the car actually scrubbed off rather
     * than from the brake key, which only exists on the driver's own client.
     *
     * <p>Worn pads eat the discs behind them several times faster. That is the
     * one lesson this whole system is built to teach: the cheap part you did
     * not change is what ruins the expensive one behind it.
     *
     * <p>Public so a test can hand it two speeds and watch the brakes pay for
     * the difference; nothing else outside the tick loop calls it.
     */
    public void wearBrakes(float speedNow) {
        float scrubbed = Math.abs(this.lastTrackedSpeed) - Math.abs(speedNow);
        this.lastTrackedSpeed = speedNow;
        // Still rolling, so this was braking rather than hitting something.
        if (scrubbed <= 0.0F || Math.abs(speedNow) < 0.05F) {
            return;
        }
        float work = scrubbed * BRAKE_WEAR_PER_DECEL;
        this.wear(CarPart.BRAKE_PADS, CarPart.BRAKE_PADS.wearPerTick() * work);
        float padsLeft = this.getConditionFraction(CarPart.BRAKE_PADS);
        this.wear(CarPart.BRAKE_DISCS, CarPart.BRAKE_DISCS.wearPerTick() * work
                * Mth.lerp(padsLeft, PADS_EAT_DISCS, 1.0F));
    }

    // ---- working on it ----

    /**
     * The part in the way of this one, or null if it can be reached. Only the
     * outermost one is named: there is no point telling somebody to take the
     * clutch out when the driveshaft is still bolted on.
     */
    public CarPart blockedBy(CarPart part) {
        // blockedBy() lists them outermost first, and the outermost is the one
        // worth naming: there is no point telling somebody to take the clutch
        // out while the driveshaft is still bolted on.
        for (CarPart above : part.blockedBy()) {
            if (this.isFitted(above)) {
                return above;
            }
        }
        return null;
    }

    /** Whether this part can be touched at all right now. */
    public boolean canReach(CarPart part) {
        return this.hasPart(part) && this.blockedBy(part) == null;
    }

    /**
     * Whether this kind of vehicle has the part at all. A car has every one;
     * a trailer has no engine, gearbox or exhaust to service, only what it
     * rolls and stops on.
     */
    public boolean hasPart(CarPart part) {
        return true;
    }

    /** The stack a part in the given condition comes off as. */
    private static ItemStack asStack(CarPart part, float condition, CarPart.Quality quality,
                                     int variant) {
        ItemStack stack = new ItemStack(part.item(), part.count());
        if (variant != 0) {
            stack.set(ModComponents.PART_VARIANT, variant);
        }
        if (condition < CarPart.MAX_CONDITION) {
            stack.set(ModComponents.PART_CONDITION, Math.max(1, Math.round(condition)));
        }
        if (quality != CarPart.Quality.DEFAULT) {
            stack.set(ModComponents.PART_QUALITY, quality.ordinal());
        }
        return stack;
    }

    /**
     * Takes a part off and hands it over. The car keeps nothing: whatever state
     * it was in is what comes out, and the slot is left empty, which is what
     * makes the part behind it reachable.
     */
    public InteractionResult removePart(Player player, CarPart part) {
        if (!this.hasPart(part)) {
            // Not a part this vehicle has: a trailer has no engine to fit.
            return InteractionResult.PASS;
        }
        if (this.level().isClientSide()) {
            return InteractionResult.SUCCESS;
        }
        if (this.isLocked()) {
            // Whichever window or click it came from, a locked car keeps its parts.
            player.displayClientMessage(Component.translatable("message.car.locked"), true);
            return InteractionResult.CONSUME;
        }
        Component name = Component.translatable("part.car." + part.key());
        if (!VehicleLiftBlock.isServicing(this.level(), this)) {
            player.displayClientMessage(Component.translatable("message.car.needs_lift"), true);
            return InteractionResult.CONSUME;
        }
        if (!this.isFitted(part)) {
            player.displayClientMessage(Component.translatable("message.car.part_absent", name), true);
            return InteractionResult.CONSUME;
        }
        CarPart inTheWay = this.blockedBy(part);
        if (inTheWay != null) {
            player.displayClientMessage(Component.translatable("message.car.part_blocked",
                    Component.translatable("part.car." + inTheWay.key()), name), true);
            return InteractionResult.CONSUME;
        }

        float condition = this.getCondition(part);
        CarPart.Quality quality = this.getQuality(part);
        this.setCondition(part, 0.0F);
        // An empty slot has no quality to speak of, so it goes back to the
        // yardstick rather than keeping the last part's grade.
        this.setQuality(part, CarPart.Quality.DEFAULT);
        int variant = this.getVariant(part);
        boolean turbo = part == CarPart.ENGINE && CarSetup.turbo(this.getSetup());
        this.setVariant(part, 0);
        this.handAside(player, asStack(part, condition, quality, variant));
        if (turbo) {
            // The turbo is bolted to the engine, so it comes away with it.
            this.setTurbo(false);
            this.handAside(player, new ItemStack(ModItems.TURBO));
        }
        this.playWrench();
        player.displayClientMessage(Component.translatable("message.car.part_removed", name,
                Math.round(condition / CarPart.MAX_CONDITION * 100.0F)), true);
        return InteractionResult.CONSUME;
    }

    /**
     * Puts what came off into the player's pockets, but not into the hand they
     * are working with: a part that landed in an empty hand would go straight
     * back on at the next click, and the click after that would take it off
     * again, which feels like the car never let go of it. What does not fit
     * is dropped at the car.
     */
    private void handAside(Player player, ItemStack stack) {
        net.minecraft.world.entity.player.Inventory inventory = player.getInventory();
        int hand = inventory.getSelectedSlot();
        int size = net.minecraft.world.entity.player.Inventory.INVENTORY_SIZE;
        // Onto a stack of the same first, then into the first empty pocket,
        // the backpack before the hotbar.
        for (int slot = 0; slot < size && !stack.isEmpty(); slot++) {
            ItemStack there = inventory.getItem(slot);
            if (slot != hand && ItemStack.isSameItemSameComponents(there, stack)
                    && there.getCount() < there.getMaxStackSize()) {
                int moved = Math.min(stack.getCount(), there.getMaxStackSize() - there.getCount());
                there.grow(moved);
                stack.shrink(moved);
            }
        }
        for (int i = 0; i < size && !stack.isEmpty(); i++) {
            int slot = (i + net.minecraft.world.entity.player.Inventory.SELECTION_SIZE) % size;
            if (slot != hand && inventory.getItem(slot).isEmpty()) {
                inventory.setItem(slot, stack.copy());
                stack.setCount(0);
            }
        }
        if (!stack.isEmpty() && this.level() instanceof ServerLevel world) {
            this.spawnAtLocation(world, stack);
        }
    }

    /**
     * Fitting a part, which is the other half of what a lift is for. Away from
     * one the click is a question — it reads the fitted part's condition back —
     * and over a raised one it is the job.
     *
     * <p>Into an empty slot it simply goes in. Onto an occupied one it is a
     * swap, and the old part comes out in whatever state it was in, so you can
     * refit it later if you are the sort who keeps a shed.
     */
    public InteractionResult fitPart(Player player, ItemStack spares, CarPart part) {
        if (!this.hasPart(part)) {
            // Not a part this vehicle has: a trailer has no engine to fit.
            return InteractionResult.PASS;
        }
        if (this.level().isClientSide()) {
            return InteractionResult.SUCCESS;
        }
        if (this.isLocked()) {
            // Whichever window or click it came from, a locked car keeps its parts.
            player.displayClientMessage(Component.translatable("message.car.locked"), true);
            return InteractionResult.CONSUME;
        }
        Component name = Component.translatable("part.car." + part.key());
        int fittedNow = Math.round(this.getConditionFraction(part) * 100.0F);

        if (!VehicleLiftBlock.isServicing(this.level(), this)) {
            player.displayClientMessage(Component.translatable("message.car.part_reading", name, fittedNow), true);
            return InteractionResult.CONSUME;
        }
        CarPart inTheWay = this.blockedBy(part);
        if (inTheWay != null) {
            player.displayClientMessage(Component.translatable("message.car.part_blocked",
                    Component.translatable("part.car." + inTheWay.key()), name), true);
            return InteractionResult.CONSUME;
        }
        if (spares.getCount() < part.count()) {
            player.displayClientMessage(
                    Component.translatable("message.car.part_needs", part.count(), name), true);
            return InteractionResult.CONSUME;
        }

        float removed = this.getCondition(part);
        CarPart.Quality removedQuality = this.getQuality(part);
        float fitting = conditionOf(spares);
        CarPart.Quality fittingQuality = qualityOf(spares);
        int removedVariant = this.getVariant(part);
        int fittingVariant = variantOf(spares);
        if (removed >= CarPart.MAX_CONDITION
                && fittingQuality == removedQuality
                // A different kind of part is a reason to swap a sound one.
                && fittingVariant == removedVariant) {
            // Nothing to gain and a part to lose: a misclick in the garage
            // should not cost you an engine. A better grade is something to
            // gain, though, so that one goes through — otherwise the only way
            // to put a good gearbox in a sound car would be to wear the
            // standard one out first.
            player.displayClientMessage(Component.translatable("message.car.part_already_new", name), true);
            return InteractionResult.CONSUME;
        }
        this.setCondition(part, fitting);
        this.setQuality(part, fittingQuality);
        this.setVariant(part, fittingVariant);
        if (!player.isCreative()) {
            spares.shrink(part.count());
        }

        int fittedPercent = Math.round(fitting / CarPart.MAX_CONDITION * 100.0F);
        if (removed <= 0.0F) {
            // The slot was empty, so there is nothing to hand back.
            this.playWrench();
            player.displayClientMessage(
                    Component.translatable("message.car.part_fitted_new", name, fittedPercent), true);
            return InteractionResult.CONSUME;
        }
        // The old one goes into your pockets rather than on the floor.
        this.handAside(player, asStack(part, removed, removedQuality, removedVariant));
        this.playWrench();
        player.displayClientMessage(Component.translatable("message.car.part_fitted", name,
                fittedNow, fittedPercent), true);
        return InteractionResult.CONSUME;
    }

    /**
     * Bolts a turbo to the engine. It is not a slot of its own: it goes on
     * the engine that is there, and it comes off when that engine does.
     */
    public InteractionResult fitTurbo(Player player, ItemStack turbo) {
        if (!this.hasPart(CarPart.ENGINE)) {
            return InteractionResult.PASS;
        }
        if (this.level().isClientSide()) {
            return InteractionResult.SUCCESS;
        }
        if (!VehicleLiftBlock.isServicing(this.level(), this)) {
            player.displayClientMessage(Component.translatable("message.car.needs_lift"), true);
            return InteractionResult.CONSUME;
        }
        if (!this.isFitted(CarPart.ENGINE)) {
            player.displayClientMessage(Component.translatable("message.car.turbo_no_engine"), true);
            return InteractionResult.CONSUME;
        }
        if (this.hasTurbo()) {
            player.displayClientMessage(Component.translatable("message.car.turbo_already"), true);
            return InteractionResult.CONSUME;
        }
        this.setTurbo(true);
        if (!player.isCreative()) {
            turbo.shrink(1);
        }
        this.playWrench();
        player.displayClientMessage(Component.translatable("message.car.turbo_fitted"), true);
        return InteractionResult.CONSUME;
    }

    /**
     * The best spare the player is carrying for this part, or an empty stack.
     * Grade first, then condition: a good part that has done some work beats a
     * cheap one in its wrapper, because the cheap one will be worn out again
     * before the good one has got going.
     */
    public static ItemStack bestSpare(Player player, CarPart part) {
        ItemStack best = ItemStack.EMPTY;
        int bestGrade = -1;
        float bestCondition = -1.0F;
        for (int slot = 0; slot < player.getInventory().getContainerSize(); slot++) {
            ItemStack stack = player.getInventory().getItem(slot);
            if (!stack.is(part.item()) || stack.getCount() < part.count()) {
                continue;
            }
            int grade = qualityOf(stack).rank();
            float condition = conditionOf(stack);
            if (grade > bestGrade || (grade == bestGrade && condition > bestCondition)) {
                best = stack;
                bestGrade = grade;
                bestCondition = condition;
            }
        }
        return best;
    }

    /**
     * One part, one click, no window: what is there comes off, what is missing
     * goes on. This is what the hands-on service mode calls when the player
     * points at a part on the car itself and right clicks it.
     *
     * <p>It decides nothing of its own — the lift, the teardown order and what
     * counts as a spare are all still {@link #removePart} and {@link #fitPart}.
     */
    public InteractionResult serviceByHand(Player player, CarPart part) {
        // A locked car keeps its parts: the service packet is as much a way in
        // as a click is, so it is turned away at the same door.
        if (this.isLocked()) {
            if (!this.level().isClientSide()) {
                player.displayClientMessage(Component.translatable("message.car.locked"), true);
            }
            return InteractionResult.CONSUME;
        }
        // What goes on is what is in your hand: an empty slot stays empty
        // until you pick the part up and offer it, so a slot you have just
        // emptied cannot quietly fill itself again on the next click.
        ItemStack held = player.getMainHandItem();
        boolean offering = held.is(part.item());
        if (this.isFitted(part)) {
            // A spare in hand on a fitted part is a swap; an empty hand takes it off.
            return offering ? this.fitPart(player, held, part) : this.removePart(player, part);
        }
        if (!offering) {
            if (!this.level().isClientSide()) {
                player.displayClientMessage(Component.translatable("message.car.part_hold_spare",
                        Component.translatable("part.car." + part.key())), true);
            }
            return InteractionResult.CONSUME;
        }
        return this.fitPart(player, held, part);
    }

    /**
     * Opens the service window on this car. The clipboard is the proper way in;
     * crouching at it is the shortcut for somebody already holding a spanner's
     * worth of parts.
     */
    public InteractionResult openService(Player player) {
        if (this.level().isClientSide()) {
            return InteractionResult.SUCCESS;
        }
        player.openMenu(new net.minecraft.world.SimpleMenuProvider(
                (syncId, inventory, opener) ->
                        new me.trossty.car.service.CarServiceScreenHandler(syncId, inventory, this.getId()),
                this.getDisplayName()));
        return InteractionResult.CONSUME;
    }

    private void playWrench() {
        this.level().playSound(null, this.getX(), this.getY() + 0.8, this.getZ(),
                SoundEvents.ANVIL_USE, SoundSource.NEUTRAL, 0.7F, 1.5F);
    }

    /** A spare with no condition on it is a new one. */
    public static float conditionOf(ItemStack spare) {
        Integer stored = spare.get(ModComponents.PART_CONDITION);
        return stored == null ? CarPart.MAX_CONDITION : stored;
    }

    /** A spare with no grade on it is a standard one. */
    public static CarPart.Quality qualityOf(ItemStack spare) {
        Integer stored = spare.get(ModComponents.PART_QUALITY);
        return stored == null ? CarPart.Quality.DEFAULT : CarPart.Quality.byOrdinal(stored);
    }

    // ---- keys and the lock ----

    public boolean isLocked() {
        return this.entityData.get(LOCKED);
    }

    public long getLockCode() {
        return this.lockCode;
    }

    public void setLockCode(long code) {
        this.lockCode = code;
        if (code == 0) {
            this.entityData.set(LOCKED, false);
        }
    }

    /** Whether this player has a key with this number anywhere on them. */
    public static boolean carriesKey(Player player, long code) {
        var inventory = player.getInventory();
        for (int slot = 0; slot < inventory.getContainerSize(); slot++) {
            ItemStack stack = inventory.getItem(slot);
            if (stack.is(ModItems.CAR_KEY) && Long.valueOf(code).equals(stack.get(ModComponents.VEHICLE_KEY))) {
                return true;
            }
        }
        return false;
    }

    /**
     * Keys first, then the lock, then everything else a click can mean. A
     * subclass that does something of its own with a click overrides
     * {@link #interactUnlocked} instead, so a locked car stays locked.
     */
    @Override
    public InteractionResult interact(Player player, InteractionHand hand) {
        ItemStack held = player.getItemInHand(hand);
        if (held.is(ModItems.CAR_KEY)) {
            InteractionResult keyed = this.useKey(player, held);
            if (keyed != InteractionResult.PASS) {
                return keyed;
            }
        }
        if (this.isLocked()) {
            if (!this.level().isClientSide()) {
                player.displayClientMessage(Component.translatable("message.car.locked"), true);
                this.level().playSound(null, this.getX(), this.getY() + 0.8, this.getZ(),
                        SoundEvents.CHEST_LOCKED, SoundSource.NEUTRAL, 0.8F, 1.0F);
            }
            return InteractionResult.SUCCESS;
        }
        return this.interactUnlocked(player, hand);
    }

    /**
     * What a key does held against this car. A blank key is cut for a car that
     * has none yet, or copied from a key you are already carrying; the right
     * key, crouching, locks and unlocks, and held normally lets you straight in
     * whether the car is locked or not.
     */
    private InteractionResult useKey(Player player, ItemStack key) {
        Level world = this.level();
        Long code = key.get(ModComponents.VEHICLE_KEY);
        if (world.isClientSide()) {
            // The client does not know the lock; the server sorts it out.
            return code == null || player.isSecondaryUseActive()
                    ? InteractionResult.SUCCESS : InteractionResult.PASS;
        }
        if (code == null) {
            if (this.lockCode == 0) {
                this.lockCode = this.random.nextLong() | 1L;
                this.cutKey(player, key, "message.car.key_bound");
            } else if (!this.isLocked() && carriesKey(player, this.lockCode)) {
                this.cutKey(player, key, "message.car.key_copied");
            } else {
                player.displayClientMessage(Component.translatable("message.car.key_taken"), true);
            }
            return InteractionResult.SUCCESS;
        }
        if (code != this.lockCode) {
            if (player.isSecondaryUseActive()) {
                player.displayClientMessage(Component.translatable("message.car.wrong_key"), true);
                return InteractionResult.SUCCESS;
            }
            return InteractionResult.PASS;
        }
        if (player.isSecondaryUseActive()) {
            boolean locked = !this.isLocked();
            this.entityData.set(LOCKED, locked);
            world.broadcastEntityEvent(this, locked ? STATUS_LOCKED : STATUS_UNLOCKED);
            player.displayClientMessage(Component.translatable(locked ? "message.car.locked_now" : "message.car.unlocked_now"), true);
            world.playSound(null, this.getX(), this.getY() + 0.8, this.getZ(),
                    locked ? SoundEvents.IRON_TRAPDOOR_CLOSE : SoundEvents.IRON_TRAPDOOR_OPEN,
                    SoundSource.NEUTRAL, 0.7F, 1.6F);
            return InteractionResult.SUCCESS;
        }
        // The right key walks you past the lock.
        return this.interactUnlocked(player, hand(player, key));
    }

    private static InteractionHand hand(Player player, ItemStack held) {
        return player.getItemInHand(InteractionHand.OFF_HAND) == held ? InteractionHand.OFF_HAND : InteractionHand.MAIN_HAND;
    }

    /** Makes one of the blank keys in this stack open this car. */
    private void cutKey(Player player, ItemStack blanks, String message) {
        ItemStack cut = blanks.split(1);
        cut.set(ModComponents.VEHICLE_KEY, this.lockCode);
        net.minecraft.resources.Identifier model = this.getModelId();
        if (model != null) {
            cut.set(ModComponents.CAR_VEHICLE, model);
        }
        if (blanks.isEmpty()) {
            player.setItemInHand(hand(player, blanks), cut);
        } else if (!player.addItem(cut)) {
            player.drop(cut, false);
        }
        player.displayClientMessage(Component.translatable(message), true);
        this.level().playSound(null, this.getX(), this.getY() + 0.8, this.getZ(),
                SoundEvents.CHAIN_PLACE, SoundSource.NEUTRAL, 0.8F, 1.4F);
    }

    protected InteractionResult interactUnlocked(Player player, InteractionHand hand) {
        InteractionResult result = super.interact(player, hand);
        if (result != InteractionResult.PASS) {
            return result;
        }
        // Crouching is the gesture for "work on it" rather than "get in it",
        // and it is the only one a bare hand has spare.
        if (player.isSecondaryUseActive()) {
            return this.openService(player);
        }

        ItemStack held = player.getItemInHand(hand);
        if (this.isLeaking() && (held.is(Items.SLIME_BALL) || held.is(Items.HONEYCOMB))) {
            return this.patchTank(player, held);
        }

        // A jerry can or the pump's nozzle held to the filler cap is poured by
        // keeping the button down, which is the item's business, not the
        // car's: PASS hands the click on to it (JerryCanItem, FuelNozzleItem).
        // Anywhere else on the car they are just something in your hand.
        if ((held.is(ModItems.JERRY_CAN) || held.is(ModItems.FUEL_NOZZLE))
                && this.isFillerInSight(player)) {
            return InteractionResult.PASS;
        }
        if (held.is(ModItems.SPRAY_CAN)) {
            return this.sprayWith(player, held);
        }
        if (held.getItem() instanceof DyeItem dye) {
            InteractionResult painted = this.paintWith(player, held, dye.getDyeColor());
            if (painted != InteractionResult.PASS) {
                return painted;
            }
        }
        if (held.is(ModItems.DIAGNOSTIC_SHEET)) {
            return this.openService(player);
        }
        if (held.is(ModItems.OBD_SCANNER) && this.hasPart(CarPart.ENGINE)) {
            // Something with an engine computer to read: not a trailer.
            me.trossty.car.item.ObdScannerItem.pair(held, this, player);
            return InteractionResult.SUCCESS;
        }
        if (held.is(ModItems.TURBO)) {
            return this.fitTurbo(player, held);
        }
        CarPart spare = CarPart.forItem(held.getItem());
        if (spare != null) {
            return this.fitPart(player, held, spare);
        }
        if (held.is(Items.IRON_INGOT)) {
            InteractionResult repaired = this.repairWith(player, held);
            if (repaired != InteractionResult.PASS) {
                return repaired;
            }
        }
        if (!this.level().isClientSide() && !player.startRiding(this)) {
            return InteractionResult.PASS;
        }
        return InteractionResult.SUCCESS;
    }

    @Override
    public @Nullable LivingEntity getControllingPassenger() {
        return this.getFirstPassenger() instanceof LivingEntity living
                ? living
                : super.getControllingPassenger();
    }

    @Override
    protected boolean canAddPassenger(Entity passenger) {
        return this.getPassengers().size() < this.seatCount();
    }

    @Override
    protected Vec3 getPassengerAttachmentPoint(Entity passenger, EntityDimensions dimensions, float scaleFactor) {
        // Local space is (sideways, up, forward); rotateY maps it into world space,
        // the same way vanilla boats place their seats.
        //
        // This point is where the rider's HIPS go, not their feet:
        // Entity.updatePassengerPosition subtracts the passenger's own VEHICLE
        // attachment from it, and for a player that is
        // PlayerLikeEntity.VEHICLE_ATTACHMENT = (0, 0.6, 0). Everything below
        // is measured from this point:
        //
        //     feet -0.60   hips +0.15   shoulders +0.90
        //     eyes +1.02   top of head +1.40
        //
        // At 0.6 the 0.6 cancels out: the rider's feet land on the car's own
        // base, their hips sit level with the cabin floor at 0.75 so the legs
        // rest on it, their eyes reach 1.62 — standing height, square in the
        // window band — and the head tops out at 2.0, under the 2.125 ceiling.
        return this.seatPosition(Math.max(0, this.getPassengers().indexOf(passenger)))
                .yRot(-this.getYRot() * ((float) Math.PI / 180.0F));
    }

    @Override
    protected double getDefaultGravity() {
        // Entity.getGravity() is 0 by default; vehicles have to opt in, as boats do.
        return 0.04;
    }

    @Override
    public float maxUpStep() {
        return 0.6F;
    }

    @Override
    public boolean canBeCollidedWith(@Nullable Entity entity) {
        // A car and the trailer on its hitch overlap on a tight turn; they
        // must not shove each other apart.
        return entity == null || !this.hitchedWith(entity);
    }

    @Override
    public boolean isPushable() {
        return true;
    }

    @Override
    public boolean isPickable() {
        return true;
    }

    @Override
    protected Entity.MovementEmission getMovementEmission() {
        return Entity.MovementEmission.EVENTS;
    }

    @Override
    protected Item getDropItem() {
        return ModItems.CAR;
    }

    // ---- filling up ----
    //
    // The way MrCrayfish's vehicles are filled: not a click anywhere on the
    // car but the filler cap, and not a lump per click but a flow for as long
    // as the button is held. The can or nozzle is pointed at the cap, the
    // fuel runs, and letting go or looking away stops it.

    /** Units a jerry can pours a tick while held to the cap: ten litres a second. */
    public static final int CAN_FLOW = 1;

    /** Units the pump's nozzle gives a tick: twenty litres a second. */
    public static final int NOZZLE_FLOW = 2;

    /** How near the crosshair has to pass the filler cap, in blocks. */
    public static final double FILLER_RADIUS = 0.5;

    private me.trossty.car.vehicle.@Nullable VehicleDefinition fillerOf;
    private boolean fillerKnown;
    private @Nullable Vec3 filler;

    /**
     * Where the filler cap is, in the car's own frame (+x left, +z forward,
     * blocks), or null for something with no tank of its own, a trailer.
     *
     * <p>Worked out from the body rather than written into every vehicle file:
     * a car's is on the left rear quarter, just above the back wheel; a lorry's
     * is on the saddle tank ahead of its back wheels; a bike's is on top of its
     * tank, in front of the rider.
     */
    public @Nullable Vec3 fillerOffset() {
        me.trossty.car.vehicle.VehicleDefinition defined = this.getDefinition();
        if (!this.fillerKnown || defined != this.fillerOf) {
            this.fillerOf = defined;
            this.fillerKnown = true;
            this.filler = findFiller(defined, this.getBbWidth());
        }
        return this.filler;
    }

    private static @Nullable Vec3 findFiller(me.trossty.car.vehicle.@Nullable VehicleDefinition defined,
                                              float width) {
        if (defined == null || defined.wheels().isEmpty()) {
            return new Vec3(width / 2.0, 0.8, -0.9);
        }
        if (defined.tank() <= 0.0F) {
            return null;
        }
        double rear = Double.MAX_VALUE;
        double front = -Double.MAX_VALUE;
        double wheelTop = 0.5;
        double hub = 0.3;
        for (me.trossty.car.vehicle.VehicleDefinition.Wheel wheel : defined.wheels()) {
            double z = wheel.at().z() / 16.0;
            if (z < rear) {
                rear = z;
                wheelTop = (wheel.at().y() + wheel.radius()) / 16.0;
                hub = wheel.at().y() / 16.0;
            }
            front = Math.max(front, z);
        }

        if (defined.chassis() == me.trossty.car.vehicle.VehicleDefinition.Chassis.MOTORCYCLE) {
            double z = defined.seats().isEmpty()
                    ? (front + rear) / 2.0
                    : defined.seats().get(0).z / 16.0 + 0.3;
            double top = -1.0;
            for (me.trossty.car.vehicle.VehicleDefinition.Box box : defined.shape()) {
                if (spans(box.from().x(), box.to().x(), 0.0) && spans(box.from().z(), box.to().z(), z * 16.0)) {
                    top = Math.max(top, Math.max(box.from().y(), box.to().y()) / 16.0);
                }
            }
            return new Vec3(0.0, (top > 0.0 ? top : wheelTop + 0.3) + 0.03, z);
        }

        double z;
        double y;
        if (defined.chassis() == me.trossty.car.vehicle.VehicleDefinition.Chassis.TANKER) {
            z = rear + 0.9;
            y = hub + 0.15;
        } else {
            z = rear - 0.3;
            y = wheelTop + 0.2;
        }
        double side = -1.0;
        for (me.trossty.car.vehicle.VehicleDefinition.Box box : defined.shape()) {
            if (spans(box.from().z(), box.to().z(), z * 16.0) && spans(box.from().y(), box.to().y(), y * 16.0)) {
                side = Math.max(side, Math.max(Math.abs(box.from().x()), Math.abs(box.to().x())) / 16.0);
            }
        }
        return new Vec3((side > 0.0 ? side : width / 2.0) + 0.02, y, z);
    }

    private static boolean spans(float a, float b, double value) {
        return Math.min(a, b) <= value && value <= Math.max(a, b);
    }

    /** The filler cap in the world, or null when there is none. */
    public @Nullable Vec3 fillerPos() {
        Vec3 offset = this.fillerOffset();
        return offset == null ? null : this.position().add(
                offset.yRot(-this.getYRot() * ((float) Math.PI / 180.0F)));
    }

    /** Whether the player is pointing at this car's filler cap from within reach. */
    public boolean isFillerInSight(Player player) {
        Vec3 cap = this.fillerPos();
        if (cap == null || player.isPassenger()) {
            return false;
        }
        Vec3 eye = player.getEyePosition();
        Vec3 look = player.getLookAngle();
        Vec3 toCap = cap.subtract(eye);
        double along = toCap.dot(look);
        if (along < 0.0 || along > player.entityInteractionRange() + 0.5) {
            return false;
        }
        return toCap.subtract(look.scale(along)).lengthSqr() <= FILLER_RADIUS * FILLER_RADIUS;
    }

    /** The car whose filler cap the player is pointing at, the nearest if two. */
    public static @Nullable CarEntity fillerInSight(Player player) {
        double reach = player.entityInteractionRange() + 3.0;
        CarEntity best = null;
        double nearest = Double.MAX_VALUE;
        for (CarEntity car : player.level().getEntitiesOfClass(
                CarEntity.class, player.getBoundingBox().inflate(reach), CarEntity::isAlive)) {
            if (car.isFillerInSight(player)) {
                double distance = car.fillerPos().distanceToSqr(player.getEyePosition());
                if (distance < nearest) {
                    nearest = distance;
                    best = car;
                }
            }
        }
        return best;
    }

    /** Whether the tank has no room for another unit. */
    public boolean isTankFull() {
        return this.getFuel() > this.getMaxFuel() - 1.0F;
    }

    /**
     * One tick of a jerry can held to the cap. Pours {@link #CAN_FLOW}, or the
     * other way, draws it off into an empty can, which is how the wrong fuel
     * comes back out. Returns the units moved; nothing means stop.
     */
    public int pourFrom(Player player, ItemStack can, boolean siphon) {
        if (this.isElectric()) {
            player.displayClientMessage(Component.translatable("message.car.no_filler"), true);
            return 0;
        }
        if (siphon) {
            boolean wasEmpty = me.trossty.car.item.JerryCans.isEmpty(can);
            int drawn = me.trossty.car.item.JerryCans.fill(can, Math.min(CAN_FLOW, (int) this.getFuel()));
            if (drawn <= 0) {
                return 0;
            }
            if (wasEmpty) {
                markFuel(can, this.getTankFuel());
                markRaw(can, this.getDirtyFuel() >= 0.5F);
            }
            this.setFuel(this.getFuel() - drawn);
            return drawn;
        }
        int pour = Math.min(CAN_FLOW, me.trossty.car.item.JerryCans.units(can));
        if (pour <= 0) {
            return 0;
        }
        float accepted = this.addFuel(pour, fuelIn(can), isRaw(can));
        if (accepted <= 0.0F) {
            if (this.hasFuel() && this.getTankFuel() != fuelIn(can)) {
                player.displayClientMessage(Component.translatable("message.car.tank_holds",
                        Component.translatable("fuel.car." + this.getTankFuel().key())), true);
            }
            return 0;
        }
        if (!player.isCreative()) {
            me.trossty.car.item.JerryCans.drain(can, Math.max(1, Math.round(accepted)));
        }
        return pour;
    }

    /**
     * What a tick of filling sounds and looks like: a thin amber stream into
     * the cap, a glug every few ticks, the gauge on the action bar.
     */
    public void fillingTick(Player player, int ticks, boolean siphon) {
        Vec3 cap = this.fillerPos();
        if (cap == null || !(this.level() instanceof ServerLevel world)) {
            return;
        }
        if (!siphon) {
            world.sendParticles(ParticleTypes.FALLING_HONEY,
                    cap.x, cap.y + 0.14, cap.z, 1, 0.02, 0.03, 0.02, 0.0);
        }
        if (ticks % 4 == 0) {
            world.playSound(null, cap.x, cap.y, cap.z,
                    siphon ? SoundEvents.BOTTLE_FILL : SoundEvents.BOTTLE_EMPTY,
                    SoundSource.NEUTRAL, 0.45F, 0.8F + this.random.nextFloat() * 0.35F);
        }
        if (ticks % 5 == 0) {
            player.displayClientMessage(Component.translatable("message.car.tank_level", this.tankReading()), true);
        }
    }

    /** The flow stopped by itself: says why, with a different note for a full tank. */
    public void fillingStopped(Player player) {
        Vec3 cap = this.fillerPos();
        if (this.isTankFull()) {
            player.displayClientMessage(Component.translatable("message.car.tank_full", this.tankReading()), true);
            if (cap != null) {
                this.level().playSound(null, cap.x, cap.y, cap.z,
                        SoundEvents.NOTE_BLOCK_BELL.value(), SoundSource.NEUTRAL, 0.5F, 1.5F);
            }
        } else {
            player.displayClientMessage(Component.translatable("message.car.tank_level", this.tankReading()), true);
        }
    }

    /** "34 / 50 L", for the action bar. */
    public Component tankReading() {
        return Component.translatable("unit.car.litres_of",
                me.trossty.car.item.JerryCans.litres(this.getFuel()),
                me.trossty.car.item.JerryCans.litres(this.getMaxFuel()));
    }

    /**
     * A holed tank, stopped with what a Minecraft mechanic has to hand: a
     * slime ball or a piece of honeycomb pressed over the hole. It holds.
     */
    private InteractionResult patchTank(Player player, ItemStack patch) {
        if (this.level().isClientSide()) {
            return InteractionResult.SUCCESS;
        }
        this.setLeaking(false);
        if (!player.isCreative()) {
            patch.shrink(1);
        }
        this.level().playSound(null, this.getX(), this.getY() + 0.5, this.getZ(),
                patch.is(Items.HONEYCOMB) ? SoundEvents.HONEYCOMB_WAX_ON : SoundEvents.SLIME_BLOCK_PLACE,
                SoundSource.NEUTRAL, 0.9F, 1.0F);
        player.displayClientMessage(Component.translatable("message.car.tank_patched"), true);
        return InteractionResult.SUCCESS;
    }

    /**
     * Repaints the car. Returns PASS when it is already that colour, so the
     * click falls through to mounting and the dye is not wasted.
     */
    private InteractionResult paintWith(Player player, ItemStack dyeStack, DyeColor color) {
        if (this.getColor() == color && this.getPaint() < 0) {
            return InteractionResult.PASS;
        }
        if (this.level().isClientSide()) {
            return InteractionResult.SUCCESS;
        }
        this.setColor(color);
        if (!player.isCreative()) {
            dyeStack.shrink(1);
        }
        this.level().playSound(
                null,
                this.getX(), this.getY() + 0.8, this.getZ(),
                SoundEvents.DYE_USE,
                SoundSource.NEUTRAL,
                1.0F,
                1.0F);
        return InteractionResult.SUCCESS;
    }

    /**
     * Sprays the whole body in the can's colour. Unlike a dye this always
     * claims the click, so an empty can tells you so instead of seating you.
     */
    private InteractionResult sprayWith(Player player, ItemStack can) {
        Level world = this.level();
        Integer colour = me.trossty.car.item.SprayCanItem.colourOf(can);
        if (colour == null || me.trossty.car.item.SprayCanItem.isEmpty(can)) {
            if (!world.isClientSide()) {
                player.displayClientMessage(Component.translatable(colour == null
                        ? "message.car.spray_no_colour" : "message.car.spray_empty"), true);
            }
            return InteractionResult.SUCCESS;
        }
        if (this.getPaint() == colour) {
            return InteractionResult.PASS;
        }
        if (world.isClientSide()) {
            return InteractionResult.SUCCESS;
        }
        this.setPaint(colour);
        if (!player.isCreative()) {
            me.trossty.car.item.SprayCanItem.use(can);
        }
        if (world instanceof ServerLevel server) {
            net.minecraft.core.particles.DustParticleOptions dust =
                    new net.minecraft.core.particles.DustParticleOptions(colour, 1.4F);
            server.sendParticles(dust, this.getX(), this.getY() + this.getBbHeight() * 0.6,
                    this.getZ(), 40, this.getBbWidth() * 0.45, this.getBbHeight() * 0.35,
                    this.getBbWidth() * 0.45, 0.0);
        }
        world.playSound(null, this.getX(), this.getY() + 0.8, this.getZ(),
                SoundEvents.GENERIC_EXTINGUISH_FIRE, SoundSource.NEUTRAL, 0.5F, 1.8F);
        return InteractionResult.SUCCESS;
    }

    /**
     * However the vehicle goes for good — wrecked, cleared away in creative,
     * killed by command — whatever it carries tips out where it
     * stood. Unloading and changing dimension keep it, as they should.
     */
    @Override
    public void onRemoval(Entity.RemovalReason reason) {
        if (reason.shouldDestroy() && this.level() instanceof ServerLevel world) {
            this.spillCargo(world);
        }
        super.onRemoval(reason);
    }

    /** Puts whatever the vehicle carries on the ground. Nothing, for a plain car. */
    protected void spillCargo(ServerLevel world) {
    }

    /** The four conditions as whole percents, in {@link CarPart} order. */
    public List<Integer> partConditions() {
        List<Integer> conditions = new java.util.ArrayList<>(CarPart.values().length);
        for (CarPart part : CarPart.values()) {
            conditions.add(Math.round(this.getConditionFraction(part) * 100.0F));
        }
        return List.copyOf(conditions);
    }

    /** Puts a saved set of conditions back on, ignoring anything malformed. */
    public void setPartConditions(List<Integer> conditions) {
        CarPart[] parts = CarPart.values();
        for (int index = 0; index < Math.min(parts.length, conditions.size()); index++) {
            this.setCondition(parts[index], conditions.get(index) / 100.0F * CarPart.MAX_CONDITION);
        }
    }

    public float getHealth() {
        return this.entityData.get(HEALTH);
    }

    public float getHealthFraction() {
        return Mth.clamp(this.getHealth() / MAX_HEALTH, 0.0F, 1.0F);
    }

    public void setHealth(float health) {
        this.entityData.set(HEALTH, Mth.clamp(health, 0.0F, MAX_HEALTH));
    }

    /**
     * Top speed for the car's current condition. A battered car still drives,
     * just not as briskly; it would be down to WRECKED_SPEED_FACTOR at zero,
     * though it falls apart before it gets there.
     */
    public float getMaxSpeed() {
        return (this.towingTicks > 0 && this.towed != null ? this.towed.towFactor() : 1.0F)
                * this.tuning().speed() * Mth.lerp(this.getHealthFraction(), MAX_SPEED * WRECKED_SPEED_FACTOR, MAX_SPEED)
                * this.partFactor(CarPart.ENGINE, WORN_ENGINE_SPEED)
                * this.partFactor(CarPart.AIR_FILTER, WORN_FILTER_SPEED, MISSING_FILTER_SPEED)
                * this.partFactor(CarPart.EXHAUST, WORN_EXHAUST_SPEED, MISSING_EXHAUST_SPEED)
                * this.grade(CarPart.ENGINE)
                * this.minorGrade(CarPart.EXHAUST)
                * this.minorGrade(CarPart.AIR_FILTER)
                * this.getLayout().speed()
                * this.getEngineFuel().speed()
                * (this.hasTurbo() ? CarSetup.TURBO_SPEED : 1.0F)
                * (this.hasOpenFilter() ? CarSetup.OPEN_FILTER_SPEED : 1.0F)
                // Wheels that are slipping do not reach the speed the engine could.
                * (0.7F + 0.3F * this.traction())
                * (this.isOverheating() ? OVERHEATING_POWER : 1.0F);
    }

    // ---- gearbox, as the dashboard and the engine note see it ----

    /** Forward gears. The driveline itself is continuous; the gears are what the driver hears and reads. */
    public static final int GEARS = 5;
    /** Revs the box changes up at, as a share of the redline. */
    private static final float UPSHIFT = 0.92F;
    private static final float IDLE_RPM = 850.0F;

    /**
     * What this car is built to do flat out: its body, engine, fuel, turbo and
     * graded parts, but none of its wear. The speedometer is printed to this, so
     * a tuned car gets a dial that reads higher and a lorry one that reads lower.
     */
    public float getRatedTopSpeed() {
        return this.tuning().speed() * MAX_SPEED
                * this.grade(CarPart.ENGINE)
                * this.minorGrade(CarPart.EXHAUST)
                * this.minorGrade(CarPart.AIR_FILTER)
                * this.getLayout().speed()
                * this.getEngineFuel().speed()
                * (this.hasTurbo() ? CarSetup.TURBO_SPEED : 1.0F)
                * (this.hasOpenFilter() ? CarSetup.OPEN_FILTER_SPEED : 1.0F);
    }

    /** Where the rev counter's red band starts: a diesel runs out of breath long before a petrol. */
    public float getRedlineRpm() {
        float redline = this.getEngineFuel().needsSpark() ? 7000.0F : 4500.0F;
        return this.getLayout() == CarSetup.Layout.V8 ? redline - 500.0F : redline;
    }

    /** The gear in, 1 to {@link #GEARS}; -1 in reverse, 0 when the engine is off. */
    public int getGear() {
        if (!this.isEngineRunning()) {
            return 0;
        }
        float speed = this.getEngineSpeed();
        if (speed < -0.005F) {
            return -1;
        }
        float fraction = Math.abs(speed) / Math.max(0.01F, this.getRatedTopSpeed());
        for (int gear = 1; gear < GEARS; gear++) {
            if (fraction <= UPSHIFT * gear / GEARS) {
                return gear;
            }
        }
        return GEARS;
    }

    /**
     * Engine revs. Each gear spans an even slice of the top speed, so the needle
     * climbs to the change-up point, drops as the next gear goes in, and climbs
     * again; reverse is geared like first.
     */
    public float getEngineRpm() {
        int gear = this.getGear();
        if (gear == 0) {
            return 0.0F;
        }
        float fraction = Math.abs(this.getEngineSpeed()) / Math.max(0.01F, this.getRatedTopSpeed());
        float span = (float) Math.max(1, Math.abs(gear)) / GEARS;
        float redline = this.getRedlineRpm();
        return Mth.clamp(IDLE_RPM + (redline - IDLE_RPM) * fraction / span, IDLE_RPM, redline * 1.04F);
    }

    /**
     * Whether the engine stumbles this tick. Fresh plugs never do; a finished
     * set does about one tick in twenty.
     */
    private boolean misfiring() {
        if (!this.getEngineFuel().needsSpark()) {
            return false;
        }
        float worn = 1.0F - this.getConditionFraction(CarPart.SPARK_PLUGS);
        return worn > 0.0F && this.random.nextFloat() < MISFIRE_CHANCE * worn;
    }

    /**
     * A car cannot be picked back up once it is put down. Every blow dents
     * it — a player's, a blast's, a mob's — and a creative player's clears it
     * away without giving anything back.
     */
    @Override
    public boolean hurtServer(ServerLevel world, DamageSource source, float amount) {
        if (this.isRemoved()) {
            return true;
        }
        if (this.isInvulnerableToBase(source)) {
            return false;
        }
        // A car once put down stays a car: nobody picks it back up. A
        // player's blows dent it like anything else, and enough of them wreck
        // it for scrap. Only a creative player can clear one away, and even
        // then nothing comes back as an item.
        if (source.isDirect() && source.getEntity() instanceof Player player && player.isCreative()) {
            if (!this.isVehicle()) {
                this.discard();
            }
            return true;
        }
        this.applyCarDamage(world, amount);
        return true;
    }

    @Override
    public boolean causeFallDamage(double fallDistance, float damagePerDistance, DamageSource damageSource) {
        if (fallDistance > SAFE_FALL_BLOCKS && this.level() instanceof ServerLevel world) {
            this.applyCarDamage(world, (float) (fallDistance - SAFE_FALL_BLOCKS) * FALL_DAMAGE_PER_BLOCK);
        }
        // Still hands the drop on to whoever was inside, as vanilla vehicles do.
        return super.causeFallDamage(fallDistance, damagePerDistance, damageSource);
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
     * @param requested the movement asked of {@link #move}
     * @param from      where the car stood before that move
     */
    private void checkForCrash(Vec3 requested, Vec3 from) {
        if (this.crashCooldown > 0 || !this.horizontalCollision) {
            return;
        }
        double covered = this.position().subtract(from).horizontalDistance();
        float blocked = (float) (requested.horizontalDistance() - covered);
        if (blocked < MIN_CRASH_SPEED) {
            return;
        }
        // Dead stop. Without it the throttle keeps shoving the car into the
        // wall and the same impact is billed again on the following tick.
        this.flyDist = 0.0F;
        this.setDeltaMovement(0.0, this.getDeltaMovement().y, 0.0);
        this.crashCooldown = CRASH_COOLDOWN;

        float amount = (blocked - MIN_CRASH_SPEED) * CRASH_DAMAGE_PER_SPEED;
        if (this.level() instanceof ServerLevel world) {
            this.applyCarDamage(world, amount);
        } else {
            CarNetworking.reportCrash(amount);
        }
    }

    /**
     * Applies a crash a driving client reported. The severity arrives from a
     * client, so it is capped: the worst a tampered-with client can do is wreck
     * the car it is sitting in, which it could manage by driving into a wall.
     */
    public void applyReportedCrash(ServerLevel world, float amount) {
        if (this.crashCooldown > 0) {
            return;
        }
        this.crashCooldown = CRASH_COOLDOWN;
        this.applyCarDamage(world, Mth.clamp(amount, 0.0F, MAX_HEALTH));
    }

    /**
     * Wears the car down by {@code amount} and wrecks it at zero. The wobble is
     * the same one vanilla vehicles use, but the car's own condition replaces
     * vanilla's rule of breaking once the wobble passes 40, which would have
     * healed itself a point a tick.
     */
    private void applyCarDamage(ServerLevel world, float amount) {
        if (amount <= 0.0F || this.isRemoved()) {
            return;
        }
        // Springs are what an impact is supposed to go through. Tired ones pass
        // more of it into the bodyshell, and take some of it themselves — which
        // is the only way suspension ever wears, since miles do not touch it.
        // Read the springs before this impact tires them: they soak up the hit
        // in the state they were in when it arrived, not the state it left them.
        amount *= this.partFactor(CarPart.SUSPENSION, WORN_SPRING_HARSHNESS);
        this.wear(CarPart.SUSPENSION, amount * SUSPENSION_WEAR_PER_DAMAGE);
        this.setHurtDir(-this.getHurtDir());
        this.setHurtTime(10);
        this.setDamage(Math.min(this.getDamage() + amount * 10.0F, 40.0F));
        this.markHurt();

        // A hard enough knock holes the tank. It is not the end of the car,
        // but it is the end of the journey unless somebody patches it.
        if (!this.isLeaking() && this.getMaxFuel() > 0.0F && amount >= PUNCTURE_MIN_DAMAGE
                && this.random.nextFloat() < Math.min(PUNCTURE_MAX_CHANCE,
                        (amount - PUNCTURE_MIN_DAMAGE + 1.0F) / PUNCTURE_SPREAD)) {
            this.setLeaking(true);
            world.playSound(null, this.getX(), this.getY() + 0.5, this.getZ(),
                    SoundEvents.ITEM_BREAK.value(), SoundSource.NEUTRAL, 0.9F, 0.6F);
            if (this.getControllingPassenger() instanceof Player driver) {
                driver.displayClientMessage(Component.translatable("message.car.tank_holed"), true);
            }
        }

        float health = this.getHealth() - amount;
        this.setHealth(health);
        world.playSound(null, this.getX(), this.getY() + 0.8, this.getZ(),
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
        world.playSound(null, this.getX(), this.getY() + 0.8, this.getZ(),
                SoundEvents.ANVIL_DESTROY, SoundSource.NEUTRAL, 0.9F, 0.8F);
        world.sendParticles(ParticleTypes.LARGE_SMOKE,
                this.getX(), this.getY() + 0.9, this.getZ(), 30, 0.5, 0.4, 0.5, 0.02);
        this.kill(world);
        if (world.getGameRules().get(GameRules.ENTITY_DROPS)) {
            this.spawnAtLocation(world, new ItemStack(Items.IRON_INGOT, WRECK_SCRAP));
        }
    }

    /**
     * The exhaust pipe, at the back on the left, puffing whenever the engine is
     * actually turning. It idles lazily and works hard under power, which is the
     * only outward sign that a parked car with somebody in it is still running.
     */
    private void emitExhaust() {
        if (!this.isEngineRunning()) {
            return;
        }
        float speed = Math.abs(this.getEngineSpeed());
        if (this.random.nextInt(speed < 0.05F ? 7 : 2) != 0) {
            return;
        }

        float yawRad = this.getYRot() * ((float) Math.PI / 180.0F);
        Vec3 at = this.position()
                .add(new Vec3(EXHAUST_SIDEWAYS, EXHAUST_HEIGHT, EXHAUST_BACK).yRot(-yawRad));
        // Trails out behind the car rather than hanging where it was made.
        double drift = -0.04 - speed * 0.5;
        this.level().addParticle(
                speed > 0.25F ? ParticleTypes.SMOKE : ParticleTypes.CAMPFIRE_COSY_SMOKE,
                at.x, at.y, at.z,
                -Mth.sin(yawRad) * drift, 0.015, Mth.cos(yawRad) * drift);
    }

    /** A black puff out of the tailpipe: the engine coughing on air. */
    private void coughExhaust() {
        float yawRad = this.getYRot() * ((float) Math.PI / 180.0F);
        Vec3 at = this.position()
                .add(new Vec3(EXHAUST_SIDEWAYS, EXHAUST_HEIGHT, EXHAUST_BACK).yRot(-yawRad));
        for (int i = 0; i < 4; i++) {
            this.level().addParticle(ParticleTypes.LARGE_SMOKE, at.x, at.y, at.z,
                    (this.random.nextDouble() - 0.5) * 0.05, 0.02, (this.random.nextDouble() - 0.5) * 0.05);
        }
    }

    /**
     * Fuel dripping out of the sill on the left, where the tank sits: just
     * ahead of the rear axle, or under the back seat on a vehicle nobody
     * described. Out at the side rather than under the middle, where the body
     * and the wheels would hide it.
     */
    private void emitLeak() {
        double back = -0.9;
        me.trossty.car.vehicle.VehicleDefinition defined = this.getDefinition();
        if (defined != null && !defined.wheels().isEmpty()) {
            back = Double.MAX_VALUE;
            for (me.trossty.car.vehicle.VehicleDefinition.Wheel wheel : defined.wheels()) {
                back = Math.min(back, wheel.at().z() / 16.0);
            }
            // Clear of the tyre, so the drip is not hidden behind it.
            back += 0.6;
        }
        double side = this.getBbWidth() / 2.0 - 0.12;
        float yawRad = this.getYRot() * ((float) Math.PI / 180.0F);
        Vec3 at = this.position().add(new Vec3(
                side, 0.45, back + (this.random.nextDouble() - 0.5) * 0.3).yRot(-yawRad));
        this.level().addParticle(ParticleTypes.FALLING_HONEY, at.x, at.y, at.z, 0.0, 0.0, 0.0);
        // A steady drip leaves a wet patch behind it.
        if (this.random.nextInt(4) == 0) {
            this.level().addParticle(ParticleTypes.LANDING_HONEY,
                    at.x + (this.random.nextDouble() - 0.5) * 0.4, this.getY() + 0.02,
                    at.z + (this.random.nextDouble() - 0.5) * 0.4, 0.0, 0.0, 0.0);
        }
    }

    /** Smoke from under the bonnet once the car is badly knocked about. */
    /**
     * Steam off the bonnet. Deliberately a different particle from the smoke a
     * dented car gives off: the two faults want different parts, so they should
     * not look the same from the driver's seat.
     */
    private void emitOverheatSteam() {
        float yawRad = this.getYRot() * ((float) Math.PI / 180.0F);
        this.level().addParticle(ParticleTypes.CLOUD,
                this.getX() - Mth.sin(yawRad) * 1.3 + (this.random.nextDouble() - 0.5) * 0.6,
                this.getY() + 1.0,
                this.getZ() + Mth.cos(yawRad) * 1.3 + (this.random.nextDouble() - 0.5) * 0.6,
                0.0, 0.06, 0.0);
    }

    private void emitDamageSmoke() {
        float yawRad = this.getYRot() * ((float) Math.PI / 180.0F);
        this.level().addParticle(
                this.getHealthFraction() < 0.15F ? ParticleTypes.LARGE_SMOKE : ParticleTypes.SMOKE,
                this.getX() - Mth.sin(yawRad) * 0.9,
                this.getY() + 0.8,
                this.getZ() + Mth.cos(yawRad) * 0.9,
                0.0, 0.03, 0.0);
    }

    /**
     * Beats one ingot's worth of dents out. Returns PASS on a sound car so the
     * click falls through to mounting and the iron is not wasted.
     */
    private InteractionResult repairWith(Player player, ItemStack ingots) {
        if (this.getHealth() >= MAX_HEALTH) {
            return InteractionResult.PASS;
        }
        if (!VehicleLiftBlock.isServicing(this.level(), this)) {
            // PASS, not FAIL: the click should still fall through to mounting, so
            // an ingot in hand never stops you getting in and driving to a garage.
            if (player.level().isClientSide()) {
                player.displayClientMessage(Component.translatable("message.car.needs_lift"), true);
            }
            return InteractionResult.PASS;
        }
        if (this.level().isClientSide()) {
            return InteractionResult.SUCCESS;
        }
        this.setHealth(this.getHealth() + REPAIR_PER_INGOT);
        if (!player.isCreative()) {
            ingots.shrink(1);
        }
        this.level().playSound(
                null,
                this.getX(), this.getY() + 0.8, this.getZ(),
                SoundEvents.ANVIL_USE,
                SoundSource.NEUTRAL,
                0.8F,
                1.1F);
        return InteractionResult.SUCCESS;
    }

    @Override
    protected void readAdditionalSaveData(ValueInput view) {
        // The model first: it says how big the tank is that the fuel goes into.
        this.entityData.set(MODEL, view.getStringOr("Model", ""));
        float savedFuel = view.getFloatOr("Fuel", FACTORY_FULL);
        if (savedFuel < 0.0F) {
            this.entityData.set(FUEL, FACTORY_FULL);
        } else {
            this.setFuel(savedFuel);
        }
        this.setLeaking(view.getBooleanOr("Leaking", false));
        this.entityData.set(COLOR,
                Mth.clamp(view.getIntOr("Color", DEFAULT_COLOR.getId()), 0, 15));
        this.setPaint(view.getIntOr("Paint", -1));
        this.lockCode = view.getLongOr("LockCode", 0L);
        this.entityData.set(LOCKED, this.lockCode != 0 && view.getBooleanOr("Locked", false));
        this.setHealth(view.getFloatOr("Health", MAX_HEALTH));
        for (CarPart part : CarPart.values()) {
            this.setCondition(part, view.getFloatOr("Part" + part.key(), CarPart.MAX_CONDITION));
        }
        int tiers = view.getIntOr("PartQuality", defaultQualityBits());
        if (view.getIntOr("PartTiers", 0) == 0) {
            // Saved before parts were iron, gold, diamond and netherite.
            int converted = 0;
            for (CarPart part : CarPart.values()) {
                int old = (tiers >>> (part.ordinal() * QUALITY_BITS)) & QUALITY_MASK;
                converted = withQuality(converted, part, CarPart.Quality.fromOldGrade(old));
            }
            tiers = converted;
        }
        this.setQualityBits(tiers);
        this.setSetup(view.getIntOr("Setup", CarSetup.DEFAULT));
        this.setOdometer(view.getFloatOr("Odometer", 0.0F));
        this.setDirtyFuel(view.getFloatOr("DirtyFuel", 0.0F));
    }

    @Override
    protected void addAdditionalSaveData(ValueOutput view) {
        view.putFloat("Fuel", this.getFuel());
        view.putBoolean("Leaking", this.isLeaking());
        view.putInt("Color", this.getColor().getId());
        view.putInt("Paint", this.getPaint());
        view.putLong("LockCode", this.lockCode);
        view.putBoolean("Locked", this.isLocked());
        view.putFloat("Health", this.getHealth());
        for (CarPart part : CarPart.values()) {
            view.putFloat("Part" + part.key(), this.getCondition(part));
        }
        view.putInt("PartQuality", this.getQualityBits());
        view.putInt("PartTiers", 1);
        view.putInt("Setup", this.getSetup());
        view.putFloat("Odometer", this.getOdometer());
        view.putFloat("DirtyFuel", this.getDirtyFuel());
        if (!this.entityData.get(MODEL).isEmpty()) {
            view.putString("Model", this.entityData.get(MODEL));
        }
    }
}
