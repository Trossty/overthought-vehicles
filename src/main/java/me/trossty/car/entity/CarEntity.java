package me.trossty.car.entity;

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
import net.minecraft.core.BlockPos;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityDimensions;
import net.minecraft.world.entity.EntitySelector;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.vehicle.VehicleEntity;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.DyeItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import me.trossty.car.util.ValueInput;
import me.trossty.car.util.ValueOutput;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

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
     * What is left of each side of the body, from MAX_HEALTH down to 0, in
     * {@link BodyZone} order. Tracked because the driver's client caps its own
     * top speed from the worst of them and every client draws the warning lamp.
     */
    static final List<EntityDataAccessor<Float>> BODY = registerBody();

    private static List<EntityDataAccessor<Float>> registerBody() {
        List<EntityDataAccessor<Float>> entries = new java.util.ArrayList<>(BodyZone.values().length);
        for (int index = 0; index < BodyZone.values().length; index++) {
            entries.add(SynchedEntityData.defineId(CarEntity.class, EntityDataSerializers.FLOAT));
        }
        return List.copyOf(entries);
    }

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
    /** How much of the extra weight towed shows up as extra fuel burnt. */
    private static final float TOWING_THIRST = 0.6F;
    /** A holed tank loses this much a tick: a family car's tank in under three minutes. */
    private static final float LEAK_PER_TICK = 0.03F;
    /** Entity status: the engine coughed on an empty pickup. */
    public static final byte STATUS_SPUTTER = 103;
    /** Entity status: a worn plug missed, and the exhaust popped. */
    public static final byte STATUS_MISFIRE = 104;

    public static final float MAX_HEALTH = 20.0F;
    /** What a player's own blow counts for, against the damage it says: eighty punches to a wreck. */
    private static final float PLAYER_BLOW = 0.25F;
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

    /** Engine wear at a standstill, as a share of its full-throttle rate. */
    static final float IDLE_WEAR_SHARE = 0.35F;

    // What each of the other parts is worth, as the multiplier left when it is
    // completely worn out and, where it differs, when it is not on the car at
    // all. A missing part is always worse than a finished one: a bald tyre is
    // still a tyre.

    /** Throttle response left on a set of plugs that is past it. */
    private static final float WORN_PLUGS_THROTTLE = 0.75F;
    /** Speed kept through a misfire. */
    private static final float MISFIRE_STUMBLE = 0.82F;
    /** Heat added, at full load, by oil that has stopped doing its job. */
    private static final float DIRTY_OIL_HEAT = 0.25F;

    private static final float WORN_FILTER_SPEED = 0.92F;
    private static final float MISSING_FILTER_SPEED = 0.84F;
    /** A blocked filter makes the engine drink; no filter is worse still. */
    private static final float WORN_FILTER_THIRST = 1.35F;
    private static final float MISSING_FILTER_THIRST = 1.70F;

    /** Cooling left with a furred-up radiator, and with none. */
    private static final float WORN_RADIATOR_COOLING = 0.55F;
    private static final float MISSING_RADIATOR_COOLING = 0.15F;
    /** Above this the engine is boiling and pulls like it means it. */
    private static final float OVERHEAT_AT = 0.85F;
    private static final float OVERHEATING_POWER = 0.55F;
    /** How fast the temperature chases where it is headed, up and down. */
    private static final float TEMP_RISE = 0.012F;
    private static final float TEMP_FALL = 0.020F;

    /** At or below this the battery is flat and the car will not start. */
    private static final float FLAT_BATTERY = 2.0F;
    private static final float WORN_CLUTCH_DRIVE = 0.60F;
    private static final float WORN_SHAFT_DRIVE = 0.70F;

    static final float WORN_PADS_BRAKE = 0.45F;
    static final float MISSING_PADS_BRAKE = 0.22F;
    static final float WORN_DISCS_BRAKE = 0.70F;
    static final float MISSING_DISCS_BRAKE = 0.40F;
    private static final float WORN_RACK_STEER = 0.50F;
    private static final float MISSING_RACK_STEER = 0.15F;

    private static final float WORN_EXHAUST_SPEED = 0.95F;
    private static final float MISSING_EXHAUST_SPEED = 0.90F;
    private static final float SAFE_FALL_BLOCKS = 3.0F;
    private static final float FALL_DAMAGE_PER_BLOCK = 1.5F;
    /**
     * Paint colour, as a {@code DyeColor} index. Tracked because the renderer on
     * every nearby client picks its texture from it.
     */
    /**
     * Condition of each fitted part, {@link CarPart#MAX_CONDITION} down to 0,
     * indexed by {@link CarPart#ordinal()}. Tracked rather than kept to the
     * server because the physics these feed runs on the driver's own client.
     */
    static final List<EntityDataAccessor<Float>> PART_CONDITION = registerPartConditions();

    private static List<EntityDataAccessor<Float>> registerPartConditions() {
        List<EntityDataAccessor<Float>> entries = new java.util.ArrayList<>(CarPart.values().length);
        for (int index = 0; index < CarPart.values().length; index++) {
            entries.add(SynchedEntityData.defineId(CarEntity.class, EntityDataSerializers.FLOAT));
        }
        return List.copyOf(entries);
    }

    /**
     * Each wheel's tyre, pad, disc and spring and each cylinder's plug, one
     * packed number per such part as {@link PartPositions} lays them out. The
     * set's average stays in {@link #PART_CONDITION}; these are the
     * differences inside it, which a reader and a pull to one side need.
     */
    static final List<EntityDataAccessor<Long>> PART_PLACES = registerPartPlaces();

    private static List<EntityDataAccessor<Long>> registerPartPlaces() {
        List<EntityDataAccessor<Long>> entries = new java.util.ArrayList<>(PartPositions.PARTS);
        for (int index = 0; index < PartPositions.PARTS; index++) {
            entries.add(SynchedEntityData.defineId(CarEntity.class, EntityDataSerializers.LONG));
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
    static final EntityDataAccessor<Integer> PART_QUALITY =
            SynchedEntityData.defineId(CarEntity.class, EntityDataSerializers.INT);

    /** Two bits per part, so each part has room for one {@code Quality}. */
    static final int QUALITY_BITS = 2;
    static final int QUALITY_MASK = (1 << QUALITY_BITS) - 1;

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
    /** Blocks driven in this car's life, for the odometer and for how fast it wears. */
    static final EntityDataAccessor<Float> ODOMETER =
            SynchedEntityData.defineId(CarEntity.class, EntityDataSerializers.FLOAT);

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

    /**
     * Water got into the engine: it will not run until it has dried out.
     * Worked out on the server; tracked, because the engine runs on the
     * driver's client.
     */
    private static final EntityDataAccessor<Boolean> FLOODED =
            SynchedEntityData.defineId(CarEntity.class, EntityDataSerializers.BOOLEAN);
    /** The driver has the brake on; see {@link #areBrakeLampsOn}. */
    private static final EntityDataAccessor<Boolean> BRAKE_LAMPS =
            SynchedEntityData.defineId(CarEntity.class, EntityDataSerializers.BOOLEAN);

    /**
     * How deep the water has to be, in blocks, to reach the air intake: a
     * ford a block deep is crossed at a crawl, anything deeper drowns it.
     */
    private static final double FLOOD_DEPTH = 1.3;
    /** How long a flooded engine takes to dry out once on land: 15 seconds. */
    private static final int DRY_OUT_TICKS = 300;
    /** Speed kept per tick with the wheels in water. */
    private static final float WATER_DRAG = 0.86F;
    /** Fastest a car wades, in blocks per tick. */
    private static final float WADE_SPEED = 0.12F;

    /** Ticks left before a flooded engine has dried out. Server only. */
    private int floodedTicks;

    /** The number this car's keys carry; 0 for a car that has never had one. Server only. */
    private long lockCode;

    /**
     * Which way the car is actually travelling, in degrees, on the side that
     * drives it. Normally it follows the nose almost at once; in a handbrake
     * turn it lags, and the difference is the slide.
     */
    private float travelYaw = Float.NaN;
    private int skidSoundCooldown;
    /** Where each rear wheel last left a mark, so the next joins on; null when it left none. */
    private final Vec3[] lastMarks = new Vec3[2];
    /** Below this speed, in blocks a tick, a wheel neither marks nor throws anything. */
    private static final double MARK_MIN_SPEED = 0.03;
    private static final double SPRAY_MIN_SPEED = 0.12;
    private static final float SKID_MARK_DARKNESS = 0.7F;
    private static final float RUT_DARKNESS = 0.35F;

    /**
     * Which data-pack vehicle this is, as its id; empty for the plain machine.
     * Tracked, because the client draws it and drives it from the same file.
     */
    private static final EntityDataAccessor<String> MODEL =
            SynchedEntityData.defineId(CarEntity.class, EntityDataSerializers.STRING);

    /**
     * What is in the chest a bed or box carries, by item, so the client can
     * draw it sitting there; nothing for a vehicle without one.
     */
    private static final EntityDataAccessor<net.minecraft.nbt.CompoundTag> SHOWN_LOAD =
            SynchedEntityData.defineId(CarEntity.class, EntityDataSerializers.COMPOUND_TAG);
    /** The same for what is in the boot, drawn in it while the lid is up. */
    private static final EntityDataAccessor<net.minecraft.nbt.CompoundTag> SHOWN_BOOT =
            SynchedEntityData.defineId(CarEntity.class, EntityDataSerializers.COMPOUND_TAG);

    /** Which wheel is flat, by its place in the vehicle's list of wheels; -1 for none. */
    static final EntityDataAccessor<Byte> FLAT_TYRE =
            SynchedEntityData.defineId(CarEntity.class, EntityDataSerializers.BYTE);
    /** One bit a wheel, by its place in the list: set where the tyre has been taken off. */
    static final EntityDataAccessor<Byte> TYRES_OFF =
            SynchedEntityData.defineId(CarEntity.class, EntityDataSerializers.BYTE);
    /** How full the sump is, 0 dry to 1 to the top mark. */
    static final EntityDataAccessor<Float> OIL_LEVEL =
            SynchedEntityData.defineId(CarEntity.class, EntityDataSerializers.FLOAT);
    /** How full the coolant bottle is, 0 empty to 1 full. */
    static final EntityDataAccessor<Float> COOLANT_LEVEL =
            SynchedEntityData.defineId(CarEntity.class, EntityDataSerializers.FLOAT);
    /** How far the oil has gone black, 0 fresh to 1 sludge. */
    static final EntityDataAccessor<Float> OIL_AGE =
            SynchedEntityData.defineId(CarEntity.class, EntityDataSerializers.FLOAT);
    /** How full the brake fluid reservoir is, 0 dry to 1 full. */
    static final EntityDataAccessor<Float> BRAKE_FLUID =
            SynchedEntityData.defineId(CarEntity.class, EntityDataSerializers.FLOAT);
    /** How far a knock has put the wheels out of true, -1 to 1, signed by the side it pulls to. */
    static final EntityDataAccessor<Float> ALIGNMENT =
            SynchedEntityData.defineId(CarEntity.class, EntityDataSerializers.FLOAT);
    /** How far through its life the timing belt is, 0 new to 1 snapped. */
    static final EntityDataAccessor<Float> BELT_WEAR =
            SynchedEntityData.defineId(CarEntity.class, EntityDataSerializers.FLOAT);
    /** Each cylinder's rings, and each cylinder's valves, packed the way {@link #PART_PLACES} are: see {@link EngineInternals}. */
    static final EntityDataAccessor<Long> ENGINE_RINGS =
            SynchedEntityData.defineId(CarEntity.class, EntityDataSerializers.LONG);
    static final EntityDataAccessor<Long> ENGINE_VALVES =
            SynchedEntityData.defineId(CarEntity.class, EntityDataSerializers.LONG);
    /** The head gasket and the crank's bearings, packed the same way. */
    static final EntityDataAccessor<Long> ENGINE_HEAD =
            SynchedEntityData.defineId(CarEntity.class, EntityDataSerializers.LONG);
    /** How far soot and oil have choked the catalytic converter, 0 clean to 1 blocked. */
    static final EntityDataAccessor<Float> CAT_CLOG =
            SynchedEntityData.defineId(CarEntity.class, EntityDataSerializers.FLOAT);
    /** Air in the tyres, 1 at the pressure on the door pillar, 0 down on the rims. */
    static final EntityDataAccessor<Float> TYRE_PRESSURE =
            SynchedEntityData.defineId(CarEntity.class, EntityDataSerializers.FLOAT);
    /** Mud and dust on the bodywork, 0 clean to 1 caked. */
    static final EntityDataAccessor<Float> DIRT =
            SynchedEntityData.defineId(CarEntity.class, EntityDataSerializers.FLOAT);
    /** Which lids stand open: {@link #BONNET_BIT}, {@link #BOOT_BIT}. */
    private static final EntityDataAccessor<Byte> LIDS =
            SynchedEntityData.defineId(CarEntity.class, EntityDataSerializers.BYTE);

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

    /** Where the server last put the car, eased into over a few ticks on clients. */
    private double lerpX;
    private double lerpY;
    private double lerpZ;
    private float lerpYRot;
    private float lerpXRot;
    private int lerpSteps;

    protected boolean pressingForward;
    protected boolean pressingBack;
    protected boolean pressingLeft;
    protected boolean pressingRight;
    protected boolean pressingBrake;

    /** Signed speed, only meaningful on the logical side. */
    float flyDist;
    private me.trossty.car.vehicle.VehicleDefinition exhaustFor;
    private Vec3 exhaustAt = new Vec3(EXHAUST_SIDEWAYS, EXHAUST_HEIGHT, EXHAUST_BACK);
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
    /** The raw travel of the last two ticks, newest first, on the server. */
    private final float[] travelledBefore = new float[2];
    /** Condition lost going under, before the part's own quality and age: see {@link #drown}. */
    private static final float DROWNED_FILTER = 40.0F;
    private static final float DROWNED_PLUGS = 30.0F;
    private static final float DROWNED_ENGINE = 25.0F;
    private static final float DROWNED_BATTERY = 35.0F;
    /**
     * Degrees a tick a car pulls to one side at full grip, with the rack or
     * the springs completely worn: a tired car needs a hand on the wheel to
     * go straight. Nothing below half condition, and growing from there.
     */
    static final float MAX_PULL = 0.9F;
    private boolean hasLastTickPos;
    private boolean wasLogicalSide;

    /** The trailer behind this, while it keeps saying so; see {@link #towedBy}. */
    private @Nullable TrailerEntity towed;
    private int towingTicks;
    private final FarmWork farmWork = new FarmWork();
    private int hornCooldown;
    int crashCooldown;

    /** The bonnet and the boot, which hang out past the square hit box. */
    private final CarEnd[] ends;

    public CarEntity(EntityType<?> type, Level world) {
        super(type, world);
        this.ends = new CarEnd[] {new CarEnd(this, true), new CarEnd(this, false)};
        // The ends' ids follow the car's, on the server and every client alike.
        this.setId(ENTITY_COUNTER.getAndAdd(this.ends.length + 1) + 1);
        // The size is the body's, and the body is known from the first tick.
        this.refreshDimensions();
    }

    @Override
    public void setId(int id) {
        super.setId(id);
        if (this.ends != null) {
            for (int i = 0; i < this.ends.length; i++) {
                this.ends[i].setId(id + i + 1);
            }
        }
    }

    @Override
    public boolean isMultipartEntity() {
        return true;
    }

    @Override
    public net.neoforged.neoforge.entity.PartEntity<?>[] getParts() {
        return this.ends;
    }

    /** The car a crosshair is on, whether it found the car or one of its ends. */
    @Nullable
    public static CarEntity of(@Nullable Entity entity) {
        if (entity instanceof CarEnd end) {
            return end.getParent();
        }
        return entity instanceof CarEntity car ? car : null;
    }

    @Override
    public void setPos(double x, double y, double z) {
        super.setPos(x, y, z);
        // Every move, lerp and teleport comes through here; the constructor's
        // first call comes before the ends exist.
        if (this.ends != null) {
            this.placeEnds();
        }
    }

    private void placeEnds() {
        this.measureReach();
        float yawRad = this.getYRot() * ((float) Math.PI / 180.0F);
        double fx = -Mth.sin(yawRad);
        double fz = Mth.cos(yawRad);
        this.ends[0].place(this.getX(), this.getY(), this.getZ(), fx, fz, this.frontReach,
                this.getBbWidth(), this.getBbHeight());
        this.ends[1].place(this.getX(), this.getY(), this.getZ(), fx, fz, this.rearReach,
                this.getBbWidth(), this.getBbHeight());
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
        builder.define(FLAT_TYRE, (byte) -1);
        builder.define(TYRES_OFF, (byte) 0);
        builder.define(LIDS, (byte) 0);
        builder.define(OIL_LEVEL, 1.0F);
        builder.define(COOLANT_LEVEL, 1.0F);
        builder.define(OIL_AGE, 0.0F);
        builder.define(BRAKE_FLUID, 1.0F);
        builder.define(ALIGNMENT, 0.0F);
        builder.define(BELT_WEAR, 0.0F);
        builder.define(ENGINE_RINGS, PartPositions.ALL_NEW);
        builder.define(ENGINE_VALVES, PartPositions.ALL_NEW);
        builder.define(ENGINE_HEAD, EngineInternals.newHead());
        builder.define(CAT_CLOG, 0.0F);
        builder.define(TYRE_PRESSURE, 1.0F);
        builder.define(DIRT, 0.0F);
        builder.define(KEY_MISSING, false);
        builder.define(FLOODED, false);
        builder.define(BRAKE_LAMPS, false);
        for (EntityDataAccessor<Float> side : BODY) {
            builder.define(side, MAX_HEALTH);
        }
        for (CarPart part : CarPart.values()) {
            builder.define(PART_CONDITION.get(part.ordinal()), CarPart.MAX_CONDITION);
        }
        for (EntityDataAccessor<Long> places : PART_PLACES) {
            builder.define(places, PartPositions.ALL_NEW);
        }
        builder.define(ENGINE_TEMP, 0.0F);
        builder.define(PART_QUALITY, defaultQualityBits());
        builder.define(SETUP, CarSetup.DEFAULT);
        builder.define(MODEL, "");
        builder.define(SHOWN_LOAD, new net.minecraft.nbt.CompoundTag());
        builder.define(SHOWN_BOOT, new net.minecraft.nbt.CompoundTag());
        builder.define(ODOMETER, 0.0F);
        builder.define(DIRTY_FUEL, 0.0F);
    }

    private final CarParts parts = new CarParts(this);
    private final Gearbox gearbox = new Gearbox(this);
    private final Upkeep upkeep = new Upkeep(this);
    private final Workshop workshop = new Workshop(this);
    private final BodyDamage damage = new BodyDamage(this);
    private final CarEffects effects = new CarEffects(this);

    /** How worn each part is, place by place, how it wears and what that costs on the road. */
    public CarParts parts() {
        return this.parts;
    }

    /** The automatic gearbox: which gear it is in, the revs, and how hard each gear pulls. */
    public Gearbox gearbox() {
        return this.gearbox;
    }

    /** What runs out or goes off besides the parts: oil, coolant, brake fluid, the belt, the catalyst, tyre pressure, alignment, dirt. */
    public Upkeep upkeep() {
        return this.upkeep;
    }

    /** Taking parts off and putting them on: what is in the way, what can be seen, and the job itself. */
    public Workshop workshop() {
        return this.workshop;
    }

    /** Dents and crashes: how much body is left, what a hit does to it and to the people inside, and beating it back out. */
    public BodyDamage damage() {
        return this.damage;
    }

    /** What a car gives off as it runs: exhaust, drips, steam, smoke and the noises of a tired engine. */
    public CarEffects effects() {
        return this.effects;
    }
    // ---- what a data pack says this vehicle is ----

    /** The id of the vehicle this is, or null for the plain machine. */
    /** What to call this car: its name tag if it has one, else what model it is ("Jeep Wrangler"). */
    public Component getVehicleName() {
        return this.hasCustomName() && this.getCustomName() != null
                ? this.getCustomName()
                : me.trossty.car.vehicle.VehicleDefinition.nameOf(this.getModelId());
    }

    public net.minecraft.resources.ResourceLocation getModelId() {
        String key = this.entityData.get(MODEL);
        return key.isEmpty()
                ? me.trossty.car.vehicle.VehicleDefinition.defaultFor(this.chassis())
                : net.minecraft.resources.ResourceLocation.tryParse(key);
    }

    public void setModel(net.minecraft.resources.ResourceLocation id) {
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
            net.minecraft.resources.ResourceLocation id = key.isEmpty()
                    ? me.trossty.car.vehicle.VehicleDefinition.defaultFor(this.chassis())
                    : net.minecraft.resources.ResourceLocation.tryParse(key);
            if (id != null) {
                this.definition = this.level().registryAccess()
                        .registry(me.trossty.car.vehicle.VehicleDefinition.KEY)
                        .map(registry -> registry.get(id))
                        .orElse(null);
            }
        }
        return this.definition;
    }

    /** The part map for the definition it was worked out from. */
    private me.trossty.car.vehicle.VehicleDefinition layoutFor;
    private java.util.Map<CarPart, CarPart.Anchor> layout;
    private java.util.Map<CarPart, java.util.List<CarPart.Anchor>> everywhere;
    private float roof;
    private float sill;

    /** Where this part sits on this vehicle: its own wheel, its own engine bay. */
    public CarPart.Anchor anchorOf(CarPart part) {
        this.refreshLayout();
        return this.layout.get(part);
    }

    /** Every spot this part can be got at: each wheel for a tyre, one spot for most parts. */
    public java.util.List<CarPart.Anchor> anchorsOf(CarPart part) {
        this.refreshLayout();
        return this.everywhere.getOrDefault(part, java.util.List.of());
    }

    /**
     * The car is drawn far past the square it collides with — the nose, the
     * boot, a lorry's whole length — so it is culled by all of that. Culled
     * by the square alone, it vanished when you stood over the engine bay
     * looking down into it, with its middle out of view.
     */
    @Override
    public net.minecraft.world.phys.AABB getBoundingBoxForCulling() {
        return this.getBoundingBox().inflate(3.0, 0.5, 3.0).expandTowards(0.0, this.roofHeight(), 0.0);
    }

    /**
     * How far the bottom of the bodywork is above the ground, in blocks:
     * where a lift's arms meet the car.
     */
    public float sillHeight() {
        this.refreshLayout();
        return this.sill;
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
            this.everywhere = PartLayout.everywhere(definition, this.layout);
            this.roof = PartLayout.roof(definition);
            this.sill = PartLayout.sill(definition);
        }
    }

    /** Which machine this is underneath, whatever body it wears. */
    public me.trossty.car.vehicle.VehicleDefinition.Chassis chassis() {
        return me.trossty.car.vehicle.VehicleDefinition.Chassis.CAR;
    }

    protected me.trossty.car.vehicle.VehicleDefinition.Performance tuning() {
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

    /**
     * The short way round. A car's yaw is kept between -180 and 180, so
     * heading north it flips from one to the other, and vanilla's straight
     * lerp would spin the car through south for a frame.
     */
    @Override
    public float getViewYRot(float partialTick) {
        return partialTick == 1.0F ? this.getYRot() : Mth.rotLerp(partialTick, this.yRotO, this.getYRot());
    }

    @Override
    public void lerpTo(double x, double y, double z, float yRot, float xRot, int steps) {
        this.lerpX = x;
        this.lerpY = y;
        this.lerpZ = z;
        this.lerpYRot = yRot;
        this.lerpXRot = xRot;
        this.lerpSteps = 3;
    }

    @Override
    public double lerpTargetX() {
        return this.lerpSteps > 0 ? this.lerpX : this.getX();
    }

    @Override
    public double lerpTargetY() {
        return this.lerpSteps > 0 ? this.lerpY : this.getY();
    }

    @Override
    public double lerpTargetZ() {
        return this.lerpSteps > 0 ? this.lerpZ : this.getZ();
    }

    @Override
    public float lerpTargetYRot() {
        return this.lerpSteps > 0 ? this.lerpYRot : this.getYRot();
    }

    @Override
    public float lerpTargetXRot() {
        return this.lerpSteps > 0 ? this.lerpXRot : this.getXRot();
    }

    /** Takes one step towards where the server put the car. */
    private void stepLerp() {
        if (this.lerpSteps > 0) {
            this.lerpPositionAndRotationStep(this.lerpSteps, this.lerpX, this.lerpY, this.lerpZ,
                    this.lerpYRot, this.lerpXRot);
            this.lerpSteps--;
        }
    }

    @Override
    public void tick() {
        if (this.level().isClientSide()) {
            this.prevRestPose = this.restPose;
            this.restPose += (this.restPoseTarget() - this.restPose) * 0.18F;
            this.prevBonnetSwing = this.bonnetSwing;
            this.bonnetSwing = swingTowards(this.bonnetSwing, this.isLidOpen(BONNET_BIT));
            this.prevBootSwing = this.bootSwing;
            this.bootSwing = swingTowards(this.bootSwing, this.isLidOpen(BOOT_BIT));
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

        double yBefore = this.getY();
        super.tick();
        this.stepLerp();

        if (!this.level().isClientSide()) {
            if (this.getControllingPassenger() == null && this.entityData.get(BRAKE_LAMPS)) {
                this.setBrakeLamps(false);
            }
            this.parts.wearParts();
            this.tickPuncture();
            this.tickTemperature();
            this.upkeep.tickFluids();
            this.effects.complainAboutTheEngine();
            this.tickWater();
        }

        boolean logicalSide = this.isControlledByLocalInstance();
        if (logicalSide) {
            // Control just moved to this side (somebody mounted or dismounted), so
            // pick up the speed the other side had instead of lurching to a stop.
            if (!this.wasLogicalSide) {
                this.flyDist = this.entityData.get(ENGINE_SPEED);
            }
            if (this.level().isClientSide() && !this.worldLoadedAhead()) {
                // The ground here has not reached this client yet: with nothing
                // to stand on it would drop straight through, so it waits, speed
                // kept, until it has.
                this.setDeltaMovement(Vec3.ZERO);
            } else {
                float yawBefore = this.getYRot();
                this.updateDriving();
                if (this.getYRot() != yawBefore && !this.endsClear(this.getYRot(), 0.0, 0.0)
                        && this.endsClear(yawBefore, 0.0, 0.0)) {
                    // The turn would swing the nose or tail into a wall.
                    this.setYRot(yawBefore);
                }
                Vec3 requested = this.getDeltaMovement();
                if (this.upkeep.isOnDyno()) {
                    // On the rollers: the wheels turn, the car stays where it is.
                    this.setYRot(yawBefore);
                    requested = new Vec3(0.0, requested.y, 0.0);
                    this.setDeltaMovement(requested);
                }
                Vec3 from = this.position();
                boolean grounded = this.onGround();
                this.moveKeepingEndsOut(requested);
                if (grounded && !this.onGround() && requested.y <= 0.0) {
                    this.followGroundDown();
                }
                this.damage.checkForCrash(requested, from);
            }
        } else {
            this.setDeltaMovement(Vec3.ZERO);
            this.travelYaw = Float.NaN;
        }
        this.wasLogicalSide = logicalSide;
        this.gearbox.tick();
        if (this.level().isClientSide()) {
            this.easeSteps(yBefore);
        }

        if (!this.level().isClientSide()) {
            // Position packets do not land one per tick: a tick with none reads
            // as a dead stop and the next as twice the speed. Three ticks
            // averaged are what the car actually did.
            float raw = this.measureTravelledSpeed();
            float travelled = (raw + this.travelledBefore[0] + this.travelledBefore[1]) / 3.0F;
            this.travelledBefore[1] = this.travelledBefore[0];
            this.travelledBefore[0] = raw;
            this.entityData.set(ENGINE_SPEED, travelled);
            // On the road only: a car carried off by water or shoved about by
            // a piston has not been driven.
            if (this.underway() && this.getControllingPassenger() != null) {
                this.parts.setOdometer(this.parts.getOdometer() + Math.abs(travelled));
            }
            // Braking only exists on the driver's client, so the brakes are
            // charged for the speed the car was actually seen to lose.
            this.parts.wearBrakes(travelled);
            this.upkeep.weather(travelled);
            this.entityData.set(KEY_MISSING, this.lockCode != 0
                    && this.getControllingPassenger() instanceof Player driver
                    && !driver.isCreative() && !carriesKey(driver, this.lockCode));
            this.warnTheDriver();
            // A tired set of plugs stumbles: decided here, so everybody near
            // hears the pop and sees the puff, and the driver feels the jolt.
            if (this.isEngineRunning() && this.parts.misfiring()) {
                this.level().broadcastEntityEvent(this, STATUS_MISFIRE);
            }
            // Burned on the server, which owns the tank, from the distance actually
            // covered: while somebody drives, the throttle only exists on their client.
            this.burnFuel(travelled);
            this.farmWork.tick(this);
        } else {
            if (this.damage.getHealthFraction() < DAMAGE_WARNING_FRACTION && this.random.nextInt(4) == 0) {
                this.effects.emitDamageSmoke();
            }
            if (this.isOverheating() && this.random.nextInt(2) == 0) {
                this.effects.emitOverheatSteam();
            }
            this.effects.emitExhaust();
            this.emitSkid();
            if (this.isLeaking() && this.hasFuel()) {
                this.effects.emitLeak();
            }
            if (this.upkeep.isOilLeaking() && this.random.nextInt(3) == 0) {
                this.effects.emitOilDrip();
            }
        }
        this.lastTickX = this.getX();
        this.lastTickZ = this.getZ();
        this.hasLastTickPos = true;

        this.animateWheels();
        this.checkInsideBlocks();
        this.pushNearbyEntities();
        this.hitWhatIsInTheWay();

        // The trailer may already have ticked this tick, a step behind where
        // this has just got to: bring it along now, so it never lags.
        if (this.towed != null && !this.towed.isRemoved() && this.towed.getTractor() == this) {
            this.towed.follow(this);
        }
    }

    // ---- towing ----

    /**
     * Whether a trailer may go on this: a land vehicle with a tow ball. A
     * boat or a plane would drag one across the water or up into the sky.
     */
    public boolean canTow() {
        return switch (this.chassis()) {
            case BOAT, PLANE, TRAILER -> false;
            default -> true;
        };
    }

    /**
     * A chest carried on this vehicle. Its window closes once the vehicle is
     * gone or out of reach, so nobody keeps taking from a load that has been
     * winched on to a flatbed and saved away with it, or has driven off.
     */
    protected net.minecraft.world.SimpleContainer carriedChest(int size) {
        net.minecraft.world.SimpleContainer chest = new net.minecraft.world.SimpleContainer(size) {
            @Override
            public boolean stillValid(Player player) {
                return !CarEntity.this.isRemoved() && CarEntity.this.distanceToSqr(player) < 8.0 * 8.0;
            }
        };
        chest.addListener(this::showLoad);
        return chest;
    }

    /**
     * Tells the client what is in a carried chest, item by item, so it can be
     * drawn in the bed: called whenever the chest changes, and after loading.
     */
    protected void showLoad(net.minecraft.world.Container chest) {
        this.show(chest, SHOWN_LOAD);
    }

    private void show(net.minecraft.world.Container chest,
                      EntityDataAccessor<net.minecraft.nbt.CompoundTag> where) {
        if (this.level().isClientSide()) {
            return;
        }
        net.minecraft.nbt.ListTag items = new net.minecraft.nbt.ListTag();
        for (int slot = 0; slot < chest.getContainerSize(); slot++) {
            ItemStack stack = chest.getItem(slot);
            if (!stack.isEmpty()) {
                items.add(net.minecraft.nbt.StringTag.valueOf(
                        net.minecraft.core.registries.BuiltInRegistries.ITEM.getKey(stack.getItem()).toString()));
            }
        }
        net.minecraft.nbt.CompoundTag shown = new net.minecraft.nbt.CompoundTag();
        shown.put("Items", items);
        this.entityData.set(where, shown);
    }

    private net.minecraft.nbt.CompoundTag shownTag;
    private java.util.List<ItemStack> shown = java.util.List.of();
    private net.minecraft.nbt.CompoundTag shownBootTag;
    private java.util.List<ItemStack> shownBoot = java.util.List.of();

    /** The carried chest's contents as the client knows them: one of each occupied slot, in order. */
    public java.util.List<ItemStack> shownLoad() {
        net.minecraft.nbt.CompoundTag tag = this.entityData.get(SHOWN_LOAD);
        if (tag != this.shownTag) {
            this.shown = itemsShown(tag);
            this.shownTag = tag;
        }
        return this.shown;
    }

    /** The boot's contents as the client knows them, the same way. */
    public java.util.List<ItemStack> shownBoot() {
        net.minecraft.nbt.CompoundTag tag = this.entityData.get(SHOWN_BOOT);
        if (tag != this.shownBootTag) {
            this.shownBoot = itemsShown(tag);
            this.shownBootTag = tag;
        }
        return this.shownBoot;
    }

    private static java.util.List<ItemStack> itemsShown(net.minecraft.nbt.CompoundTag tag) {
        java.util.List<ItemStack> stacks = new java.util.ArrayList<>();
        net.minecraft.nbt.ListTag items = tag.getList("Items", net.minecraft.nbt.Tag.TAG_STRING);
        for (int i = 0; i < items.size(); i++) {
            net.minecraft.resources.ResourceLocation id =
                    net.minecraft.resources.ResourceLocation.tryParse(items.getString(i));
            net.minecraft.world.item.Item item = id == null ? null
                    : net.minecraft.core.registries.BuiltInRegistries.ITEM.get(id);
            if (item != null && item != net.minecraft.world.item.Items.AIR) {
                stacks.add(new ItemStack(item));
            }
        }
        return stacks;
    }

    /** A trailer says this every tick it is hitched on. */
    void towedBy(TrailerEntity trailer) {
        this.towed = trailer;
        this.towingTicks = 3;
    }

    /** The trailer hitched behind this, if one still is. */
    @Nullable TrailerEntity towed() {
        return this.isTowing(null) ? this.towed : null;
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
        float roof = this.getBbHeight() * 16.0F;
        for (me.trossty.car.vehicle.VehicleDefinition.Box box : defined.shape()) {
            // Rotor blades and propellers sweep past the ends, and anything
            // wholly above the hitbox clears what the ends are checked for.
            if (box.spins() || Math.min(box.from().y(), box.to().y()) >= roof) {
                continue;
            }
            front = Math.max(front, Math.max(box.from().z(), box.to().z()));
            rear = Math.min(rear, Math.min(box.from().z(), box.to().z()));
        }
        this.frontReach = front / 16.0;
        this.rearReach = -rear / 16.0;
    }

    /**
     * The hitbox is a square as wide as the car, so the bonnet and the boot
     * hang out past it, front and back. This checks those overhangs against
     * the blocks, a block-deep slice at a time, above the step the wheels can
     * ride up. Where a slice finds that step, ground rising like a hillside,
     * the slice past it allows one more, so the car still climbs a hill a
     * step per block while a wall stops the bumper instead of swallowing it.
     *
     * @return true when, turned to {@code yaw} and moved by {@code dx, dz},
     *         neither overhang is inside a block
     */
    private boolean endsClear(float yaw, double dx, double dz) {
        this.measureReach();
        float yawRad = yaw * ((float) Math.PI / 180.0F);
        double fx = -Mth.sin(yawRad);
        double fz = Mth.cos(yawRad);
        double half = this.getBbWidth() / 2.0;
        // Narrower than the car, so scraping along a wall is not a head-on hit.
        double side = this.getBbWidth() * 0.8 / 2.0;
        double step = this.maxUpStep();
        double top = this.getY() + this.getBbHeight();
        for (int end = 0; end < 2; end++) {
            double sign = end == 0 ? 1.0 : -1.0;
            double overhang = (end == 0 ? this.frontReach : this.rearReach) - half;
            double lift = step;
            for (int k = 0; k < overhang; k++) {
                double near = half + k;
                double far = half + Math.min(overhang, k + 1.0);
                double mid = (near + far) / 2.0 * sign;
                double length = (far - near) / 2.0;
                double cx = this.getX() + dx + fx * mid;
                double cz = this.getZ() + dz + fz * mid;
                double ex = Math.abs(fx) * length + Math.abs(fz) * side;
                double ez = Math.abs(fz) * length + Math.abs(fx) * side;
                double bottom = this.getY() + lift + 0.01;
                if (bottom < top && !this.level().noBlockCollision(this, new net.minecraft.world.phys.AABB(
                        cx - ex, bottom, cz - ez, cx + ex, top, cz + ez))) {
                    return false;
                }
                if (!this.level().noBlockCollision(this, new net.minecraft.world.phys.AABB(
                        cx - ex, this.getY() + 0.01, cz - ez, cx + ex, bottom, cz + ez))) {
                    lift += step;
                }
            }
        }
        return true;
    }

    /**
     * Moves the car, then pulls it back as far as it takes to get the nose and
     * tail out of a wall. Checked after the move so a kerb the wheels just
     * climbed counts, and the bonnet is judged from the height it now rides
     * at. A car that started out overlapping a block (placed against one, or
     * pushed into one) is left to drive itself out.
     */
    private void moveKeepingEndsOut(Vec3 move) {
        Vec3 from = this.position();
        boolean wasClear = move.horizontalDistanceSqr() < 1.0E-8 || this.endsClear(this.getYRot(), 0.0, 0.0);
        this.move(MoverType.SELF, move);
        if (!wasClear || this.endsClear(this.getYRot(), 0.0, 0.0)) {
            return;
        }
        double clear = 0.0;
        double blocked = 1.0;
        for (int i = 0; i < 4; i++) {
            double mid = (clear + blocked) / 2.0;
            this.setPos(from);
            this.move(MoverType.SELF, new Vec3(move.x * mid, move.y, move.z * mid));
            if (this.endsClear(this.getYRot(), 0.0, 0.0)) {
                clear = mid;
            } else {
                blocked = mid;
            }
        }
        this.setPos(from);
        this.move(MoverType.SELF, new Vec3(move.x * clear, move.y, move.z * clear));
        this.horizontalCollision = true;
    }

    /** From the middle to the very front of the body, in blocks. */
    protected double frontReach() {
        this.measureReach();
        return this.frontReach;
    }

    /** From the middle to the very back of the body, in blocks. */
    protected double rearReach() {
        this.measureReach();
        return this.rearReach;
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

        if (driver != null && this.isFlying()) {
            // Off the ground the wheel brakes grip nothing: only the throttle counts.
            this.flyDist = this.airspeed(this.flyDist, this.pressingForward && this.hasFuel() && this.canRun());
        } else if (driver != null) {
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
                        : Math.min(this.flyDist + this.throttleStep() * this.gearbox.gearPull(), max);
            } else if (this.pressingBack) {
                this.flyDist = Math.max(this.flyDist - this.throttleStep(), -this.maxReverseSpeed());
            } else {
                this.flyDist *= ENGINE_DRAG;
            }

            if (this.pressingBrake) {
                // Handbrake into a corner at speed: the back steps out
                // instead of the car stopping.
                boolean turning = this.pressingLeft != this.pressingRight;
                float brake = turning && onGround && this.canDrift() && Math.abs(this.flyDist) > DRIFT_MIN_SPEED
                        ? this.brakeStrength() * DRIFT_BRAKE
                        : this.brakeStrength();
                this.flyDist = approachZero(this.flyDist, brake);
            }

        } else {
            this.flyDist *= onGround ? ROLLING_FRICTION : AIR_DRAG;
        }

        this.flyDist = this.limitSpeed(this.flyDist);

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

        boolean drifting = driver != null && this.pressingBrake && onGround && this.canDrift() && steerInput != 0.0F
                && Math.abs(this.flyDist) > DRIFT_MIN_SPEED;
        float turn = this.turnRate(steerInput, drifting);
        if (turn != 0.0F) {
            this.setYRot(Mth.wrapDegrees(this.getYRot() + turn));
        }

        float pull = this.pull() + (driver != null && this.brakePedal() ? this.parts.brakePull() : 0.0F);
        if (pull != 0.0F && onGround && this.flyDist != 0.0F) {
            float grip = Math.min(1.0F, Math.abs(this.flyDist) / FULL_GRIP_SPEED);
            this.setYRot(Mth.wrapDegrees(this.getYRot() + pull * grip * Math.signum(this.flyDist)));
        }

        // The car goes where it is travelling, which in a slide is not where
        // it is pointing. In the air nothing turns the travel at all.
        if (Float.isNaN(this.travelYaw) || Math.abs(this.flyDist) < 0.05F) {
            this.travelYaw = this.getYRot();
        } else if (this.hasGrip(onGround)) {
            this.travelYaw = Mth.rotLerp(
                    drifting ? DRIFT_GRIP : NORMAL_GRIP, this.travelYaw, this.getYRot());
        }
        float yawRad = this.travelYaw * ((float) Math.PI / 180.0F);
        double velocityY = this.verticalMotion(onGround);
        this.setDeltaMovement(
                -Mth.sin(yawRad) * this.flyDist,
                velocityY,
                Mth.cos(yawRad) * this.flyDist);

        if (this.horizontalCollision) {
            this.flyDist *= 0.5F;
        }
    }

    /** Off the ground and held up by the air: only a plane ever is. */
    protected boolean isFlying() {
        return false;
    }

    /** Speed in the air, throttle open or not: only asked while {@link #isFlying}. */
    protected float airspeed(float speed, boolean throttle) {
        return speed;
    }

    /** What the surroundings allow of this speed: anything on wheels only wades through water. */
    protected float limitSpeed(float speed) {
        if (this.isInWater()) {
            // Wading: the water holds the car back, hard.
            return Mth.clamp(speed * WATER_DRAG, -WADE_SPEED, WADE_SPEED);
        }
        return speed;
    }

    /** Whether it goes where it points: tyres on the ground do that, nothing in the air does. */
    protected boolean hasGrip(boolean onGround) {
        return onGround;
    }

    /** Up and down this tick: gravity, and the ground holding it up. */
    protected double verticalMotion(boolean onGround) {
        // 0.98 is vanilla's vertical drag; without it a long fall accelerates forever.
        double velocityY = (this.getDeltaMovement().y - this.getGravity()) * 0.98;
        if (onGround && velocityY < 0.0) {
            // Keep the car pinned to the ground instead of jittering on it.
            velocityY = -0.04;
        }
        return velocityY;
    }

    /** Whether deep water drowns it: anything with its intake in the hull, not a boat. */
    protected boolean floods() {
        return true;
    }

    /** Whether the handbrake kicks the back out: only something with a back axle to lock. */
    protected boolean canDrift() {
        return true;
    }

    /**
     * Whether it is going somewhere under its own power rather than being
     * carried: what the odometer counts. On wheels that means on the ground.
     */
    protected boolean underway() {
        return this.onGround();
    }

    /**
     * What the tyres do to the road and the road to the tyres, seen from where
     * the car went: smoke and squeal and rubber left behind whenever it slides
     * sideways to the way it points, ruts cut in soft ground, dirt thrown up
     * off it, and spray off a wet road or out of a puddle. Worked out from the
     * movement itself, so every client sees every slide, not only the driver.
     */
    private void emitSkid() {
        if (this.skidSoundCooldown > 0) {
            this.skidSoundCooldown--;
        }
        if (!this.hasLastTickPos || !this.onGround()) {
            this.lastMarks[0] = this.lastMarks[1] = null;
            return;
        }
        double dx = this.getX() - this.lastTickX;
        double dz = this.getZ() - this.lastTickZ;
        double moved = Math.sqrt(dx * dx + dz * dz);
        if (moved < MARK_MIN_SPEED) {
            this.lastMarks[0] = this.lastMarks[1] = null;
            return;
        }
        float yawRad = this.getYRot() * ((float) Math.PI / 180.0F);
        double forwardX = -Mth.sin(yawRad);
        double forwardZ = Mth.cos(yawRad);
        double along = Math.abs(dx * forwardX + dz * forwardZ) / moved;
        boolean sliding = moved >= DRIFT_MIN_SPEED * 0.9 && along <= 0.97;

        double[] rear = this.rearAxle();
        double back = rear[0];
        double track = rear[1];
        int wheels = track < 0.05 ? 1 : 2;
        long now = this.level().getGameTime();
        for (int wheel = 0; wheel < wheels; wheel++) {
            int side = wheels == 1 ? 0 : wheel * 2 - 1;
            double x = this.getX() - forwardX * back + forwardZ * side * track;
            double z = this.getZ() - forwardZ * back - forwardX * side * track;
            BlockPos under = BlockPos.containing(x, this.getY() - 0.2, z);
            net.minecraft.world.level.block.state.BlockState ground = this.level().getBlockState(under);
            boolean soft = isSoftGround(ground);
            boolean wet = this.level().isRainingAt(under.above())
                    || this.level().getFluidState(BlockPos.containing(x, this.getY() + 0.05, z))
                            .is(net.minecraft.tags.FluidTags.WATER);

            if (sliding) {
                this.level().addParticle(wet ? ParticleTypes.SPLASH : ParticleTypes.CLOUD, x, this.getY() + 0.15, z,
                        (this.random.nextDouble() - 0.5) * 0.04, 0.02, (this.random.nextDouble() - 0.5) * 0.04);
            }

            // Rubber on the road when it slides, a rut in soft ground whenever it
            // rolls over it; a wet road takes neither.
            Vec3 here = new Vec3(x, this.getY(), z);
            float dark = sliding && !wet ? SKID_MARK_DARKNESS : soft ? RUT_DARKNESS : 0.0F;
            if (dark > 0.0F) {
                Vec3 last = this.lastMarks[wheel];
                if (last != null && last.distanceToSqr(here) < 4.0) {
                    TyreMarks.add(new TyreMarks.Mark(last.x, last.y, last.z, x, this.getY(), z,
                            wheels == 1 ? 0.18F : 0.3F, dark, now));
                }
                this.lastMarks[wheel] = here;
            } else {
                this.lastMarks[wheel] = null;
            }

            // Thrown up behind the wheel, more the faster it goes.
            if (moved > SPRAY_MIN_SPEED && this.random.nextFloat() < moved * 2.5) {
                double up = 0.08 + moved * 0.3;
                double vx = -dx * 0.6 + (this.random.nextDouble() - 0.5) * 0.08;
                double vz = -dz * 0.6 + (this.random.nextDouble() - 0.5) * 0.08;
                if (wet) {
                    this.level().addParticle(ParticleTypes.SPLASH, x, this.getY() + 0.1, z, vx, up, vz);
                    this.level().addParticle(ParticleTypes.FALLING_WATER, x, this.getY() + 0.3, z, vx, up * 0.5, vz);
                } else if (soft) {
                    this.level().addParticle(new net.minecraft.core.particles.BlockParticleOption(
                            ParticleTypes.BLOCK, ground), x, this.getY() + 0.1, z, vx, up, vz);
                }
            }
        }

        if (sliding && this.skidSoundCooldown == 0) {
            this.skidSoundCooldown = 9;
            this.level().playLocalSound(this.getX(), this.getY(), this.getZ(),
                    ModSounds.TYRE_SKID, SoundSource.NEUTRAL, 0.55F,
                    0.9F + this.random.nextFloat() * 0.2F, false);
        }
    }

    /**
     * The back wheels, in blocks: how far behind the middle they are, and how
     * far out to either side — nought for a single track, like a bike's.
     */
    double[] rearAxle() {
        me.trossty.car.vehicle.VehicleDefinition defined = this.getDefinition();
        if (defined == null || defined.wheels().isEmpty()) {
            return new double[] {this.getBbWidth() * 0.45, Math.min(0.8, this.getBbWidth() * 0.35)};
        }
        float rearZ = Float.MAX_VALUE;
        for (me.trossty.car.vehicle.VehicleDefinition.Wheel wheel : defined.wheels()) {
            rearZ = Math.min(rearZ, wheel.at().z());
        }
        float side = 0.0F;
        for (me.trossty.car.vehicle.VehicleDefinition.Wheel wheel : defined.wheels()) {
            if (wheel.at().z() < rearZ + 1.0F) {
                side = Math.max(side, Math.abs(wheel.at().x()));
            }
        }
        return new double[] {-rearZ / 16.0, side / 16.0};
    }

    /** Ground a wheel sinks into, cutting a rut and throwing it up behind. */
    static boolean isSoftGround(net.minecraft.world.level.block.state.BlockState ground) {
        return ground.is(net.minecraft.tags.BlockTags.DIRT) || ground.is(net.minecraft.tags.BlockTags.SAND)
                || ground.is(net.minecraft.world.level.block.Blocks.GRAVEL)
                || ground.is(net.minecraft.tags.BlockTags.SNOW)
                || ground.is(net.minecraft.world.level.block.Blocks.SOUL_SAND)
                || ground.is(net.minecraft.world.level.block.Blocks.SOUL_SOIL);
    }

    private static float approachZero(float value, float amount) {
        if (value > 0.0F) {
            return Math.max(0.0F, value - amount);
        }
        return Math.min(0.0F, value + amount);
    }

    /** Slowest speed, in blocks per tick, at which hitting somebody hurts them. */
    private static final float MIN_IMPACT_SPEED = 0.12F;
    /** Health taken from whoever is hit, per block per tick of speed. */
    private static final float IMPACT_DAMAGE_PER_SPEED = 22.0F;
    /** Of the speed, how much is lost per cubic block of whatever is hit, and the most lost to one hit. */
    private static final float IMPACT_SLOWING = 0.35F;
    private static final float MAX_IMPACT_SLOWING = 0.7F;
    /** Of the damage dealt, the share the car takes in dents. */
    private static final float IMPACT_SHARE_TO_CAR = 0.15F;
    private static final net.minecraft.resources.ResourceKey<net.minecraft.world.damagesource.DamageType> RUN_OVER =
            net.minecraft.resources.ResourceKey.create(net.minecraft.core.registries.Registries.DAMAGE_TYPE,
                    net.minecraft.resources.ResourceLocation.fromNamespaceAndPath("car", "run_over"));

    /** Who this car hit lately, and when: one blow each, not one a tick. */
    private final java.util.Map<Integer, Integer> recentlyHit = new java.util.HashMap<>();

    /**
     * Whoever stands in the way of a moving car is struck: thrown aside and
     * hurt by how fast it was going, and the car loses speed for it and takes
     * a dent, more the bigger they were. Both sides look: the side simulating
     * the car feels the slowing, the server deals the damage.
     */
    private void hitWhatIsInTheWay() {
        this.recentlyHit.values().removeIf(at -> this.tickCount - at > 20);
        float speed = this.level().isClientSide() || this.isControlledByLocalInstance()
                ? this.flyDist : this.getEngineSpeed();
        if (Math.abs(speed) < MIN_IMPACT_SPEED) {
            return;
        }
        net.minecraft.world.phys.AABB zone = this.getBoundingBox()
                .minmax(this.ends[speed > 0.0F ? 0 : 1].getBoundingBox()).inflate(0.15, 0.0, 0.15);
        float yawRad = this.getYRot() * ((float) Math.PI / 180.0F);
        Vec3 forward = new Vec3(-Mth.sin(yawRad), 0.0, Mth.cos(yawRad)).scale(Math.signum(speed));
        for (LivingEntity victim : this.level().getEntitiesOfClass(LivingEntity.class, zone,
                e -> e.isAlive() && !e.isSpectator() && !e.isPassenger() && !this.hasPassenger(e))) {
            // Only what is ahead of the middle: a car does not run over what it is leaving behind.
            if (victim.position().subtract(this.position()).dot(forward) <= 0.0
                    || this.recentlyHit.putIfAbsent(victim.getId(), this.tickCount) != null) {
                continue;
            }
            float bulk = victim.getBbWidth() * victim.getBbWidth() * victim.getBbHeight();
            if (this.isControlledByLocalInstance()) {
                this.flyDist *= 1.0F - Math.min(MAX_IMPACT_SLOWING, bulk * IMPACT_SLOWING);
            }
            if (this.level() instanceof ServerLevel world) {
                float damage = (Math.abs(speed) - MIN_IMPACT_SPEED * 0.5F) * IMPACT_DAMAGE_PER_SPEED;
                Entity driver = this.getControllingPassenger();
                victim.hurt(this.damageSources().source(RUN_OVER, this, driver), damage);
                victim.push(forward.x * Math.abs(speed) * 1.6, 0.25 + Math.abs(speed) * 0.4,
                        forward.z * Math.abs(speed) * 1.6);
                victim.hurtMarked = true;
                this.damage.applyCarDamage(world, damage * IMPACT_SHARE_TO_CAR * Math.min(1.0F, bulk),
                        speed > 0.0F ? BodyZone.FRONT : BodyZone.REAR);
                world.playSound(null, victim.getX(), victim.getY(), victim.getZ(),
                        SoundEvents.PLAYER_ATTACK_KNOCKBACK, SoundSource.NEUTRAL, 1.0F, 0.7F);
            }
        }
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

    /**
     * Whether the brake lamps are lit: the pedal, or reverse held while still
     * rolling forward, which is braking too. The driving client knows that
     * itself; everybody else has it from the server, which the driver tells.
     */
    public boolean areBrakeLampsOn() {
        if (this.getControllingPassenger() == null) {
            return false;
        }
        return this.isControlledByLocalInstance() ? this.brakePedal() : this.entityData.get(BRAKE_LAMPS);
    }

    /** The driver's feet, on the driving client. */
    public boolean brakePedal() {
        return this.pressingBrake || this.pressingBack && this.flyDist > SPEED_EPSILON;
    }

    public void setBrakeLamps(boolean on) {
        this.entityData.set(BRAKE_LAMPS, on);
    }

    /** Called on the controlling client every tick with that player's held keys. */
    public void setInputs(boolean forward, boolean back, boolean left, boolean right, boolean brake) {
        this.pressingForward = forward;
        this.pressingBack = back;
        this.pressingLeft = left;
        this.pressingRight = right;
        this.pressingBrake = brake;
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
        return this.isTowing(null) ? 1.0F + (1.0F / this.towRatio() - 1.0F) * TOWING_THIRST : 1.0F;
    }

    /** What this weighs, in kilograms, as its definition gives it. */
    public float massKg() {
        me.trossty.car.vehicle.VehicleDefinition defined = this.getDefinition();
        return defined == null ? 1300.0F : defined.massKg();
    }

    /**
     * The share of the engine's pull left for this vehicle itself: 1 with
     * nothing behind, its own weight over its own and the trailer's (and
     * whatever that carries) when towing. A family car pulling another car
     * on a flat bed gets away about half as hard.
     */
    public float towRatio() {
        if (!this.isTowing(null)) {
            return 1.0F;
        }
        float own = this.massKg();
        return own / (own + this.towed.towedMass());
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
    /** The odometer, always in kiloblocks: two decimals, so it moves every ten blocks. */
    public static Component formatOdometer(float blocks) {
        float kb = blocks / 1000.0F;
        return Component.translatable("unit.car.kiloblocks",
                String.format(java.util.Locale.ROOT, kb < 100.0F ? "%.2f" : "%.1f", kb));
    }

    public static Component formatDistance(float blocks) {
        // Blocks up to a thousand, then kiloblocks: 1 kb = 1000 blocks.
        if (blocks < 1000.0F) {
            return Component.translatable("unit.car.blocks", (int) blocks);
        }
        float kb = blocks / 1000.0F;
        return Component.translatable("unit.car.kiloblocks", kb < 100.0F
                ? String.format(java.util.Locale.ROOT, "%.1f", kb)
                : String.valueOf(Math.round(kb)));
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
        return this.isControlledByLocalInstance()
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

    // ---- the bonnet and the boot ----

    public static final int BONNET_BIT = 1;
    public static final int BOOT_BIT = 2;
    /** How far each lid swings up about its hinge, fully open. */
    public static final float BONNET_SWING_DEGREES = 55.0F;
    public static final float BOOT_SWING_DEGREES = 60.0F;
    /** A boot holds two rows: the shopping, a toolbox, not a pickup's bed. */
    public static final int BOOT_SLOTS = 18;
    /** How far a lid gets in a tick, as a share of the way: about half a second end to end. */
    private static final float SWING_STEP = 0.1F;
    /** How open each lid is drawn, 0 to 1; client only. */
    private float bonnetSwing;
    private float prevBonnetSwing;
    private float bootSwing;
    private float prevBootSwing;
    /**
     * What is in the boot. Not shown in a bed the way a pickup's load is,
     * so it is a plain chest rather than a {@link #carriedChest}.
     */
    private final net.minecraft.world.SimpleContainer boot = new net.minecraft.world.SimpleContainer(BOOT_SLOTS) {
        @Override
        public boolean stillValid(Player player) {
            return !CarEntity.this.isRemoved() && CarEntity.this.isLidOpen(BOOT_BIT)
                    && CarEntity.this.distanceToSqr(player) < 8.0 * 8.0;
        }

        @Override
        public void setChanged() {
            super.setChanged();
            CarEntity.this.show(this, SHOWN_BOOT);
        }
    };

    private static float swingTowards(float swing, boolean open) {
        return open ? Math.min(1.0F, swing + SWING_STEP) : Math.max(0.0F, swing - SWING_STEP);
    }

    public float getBonnetSwing(float tickProgress) {
        return Mth.lerp(tickProgress, this.prevBonnetSwing, this.bonnetSwing);
    }

    public float getBootSwing(float tickProgress) {
        return Mth.lerp(tickProgress, this.prevBootSwing, this.bootSwing);
    }

    public boolean isLidOpen(int lid) {
        return (this.entityData.get(LIDS) & lid) != 0;
    }

    public void setLidOpen(int lid, boolean open) {
        int lids = this.entityData.get(LIDS);
        this.entityData.set(LIDS, (byte) (open ? lids | lid : lids & ~lid));
    }

    public net.minecraft.world.Container getBoot() {
        return this.boot;
    }

    private static String lidName(int lid) {
        return lid == BONNET_BIT ? me.trossty.car.vehicle.VehicleDefinition.Box.BONNET : me.trossty.car.vehicle.VehicleDefinition.Box.BOOT;
    }

    /** Whether this vehicle's shape has that lid at all. */
    public boolean hasLid(int lid) {
        me.trossty.car.vehicle.VehicleDefinition defined = this.getDefinition();
        return defined != null && defined.lidBounds(lidName(lid)) != null;
    }

    /**
     * Where the player points on a lid: at the lid itself, or, with it up,
     * into the hole it covers. Worked out in the vehicle's own pixels.
     */
    private record LidAim(int lid, boolean inside) {
    }

    /** Where the middle of the bonnet is, in the world; null for a vehicle without one. */
    public @Nullable Vec3 bonnetCentre() {
        me.trossty.car.vehicle.VehicleDefinition defined = this.getDefinition();
        net.minecraft.world.phys.AABB bonnet = defined == null ? null : defined.lidBounds(lidName(BONNET_BIT));
        if (bonnet == null) {
            return null;
        }
        return this.position().add(bonnet.getCenter().scale(1.0 / 16.0)
                .yRot((float) -(this.getYRot() * Mth.DEG_TO_RAD)));
    }

    private @Nullable LidAim aimedLid(Player player) {
        me.trossty.car.vehicle.VehicleDefinition defined = this.getDefinition();
        if (defined == null || player.isPassenger()) {
            return null;
        }
        double yaw = this.getYRot() * Mth.DEG_TO_RAD;
        Vec3 from = player.getEyePosition().subtract(this.position()).yRot((float) yaw).scale(16.0);
        Vec3 to = from.add(player.getViewVector(1.0F).yRot((float) yaw)
                .scale((player.entityInteractionRange() + 1.0) * 16.0));
        LidAim best = null;
        double nearest = Double.MAX_VALUE;
        for (int lid : new int[] {BONNET_BIT, BOOT_BIT}) {
            net.minecraft.world.phys.AABB closed = defined.lidBounds(lidName(lid));
            if (closed == null) {
                continue;
            }
            boolean open = this.isLidOpen(lid);
            // Shut, the lid is the hole; up, the hole is still where it was.
            net.minecraft.world.phys.AABB hole = closed.inflate(0.5).expandTowards(0.0, 1.5, 0.0);
            java.util.Optional<Vec3> hit = hole.clip(from, to);
            if (hit.isPresent() && hit.get().distanceToSqr(from) < nearest) {
                nearest = hit.get().distanceToSqr(from);
                best = new LidAim(lid, open);
            }
            if (open) {
                hit = raisedLid(closed, lid).inflate(0.5).clip(from, to);
                if (hit.isPresent() && hit.get().distanceToSqr(from) < nearest) {
                    nearest = hit.get().distanceToSqr(from);
                    best = new LidAim(lid, false);
                }
            }
        }
        return best;
    }

    /**
     * The box a lid fills once swung up about its hinge: the bonnet's at its
     * back edge by the windscreen, the boot's at its front by the rear window,
     * both along the top.
     */
    public static net.minecraft.world.phys.AABB raisedLid(net.minecraft.world.phys.AABB closed, int lid) {
        boolean bonnet = lid == BONNET_BIT;
        double hingeY = closed.maxY;
        double hingeZ = bonnet ? closed.minZ : closed.maxZ;
        double angle = (bonnet ? BONNET_SWING_DEGREES : BOOT_SWING_DEGREES) * Mth.DEG_TO_RAD;
        double up = bonnet ? 1.0 : -1.0;
        double cos = Math.cos(angle);
        double sin = Math.sin(angle);
        double minY = Double.MAX_VALUE, maxY = -Double.MAX_VALUE, minZ = Double.MAX_VALUE, maxZ = -Double.MAX_VALUE;
        for (double y : new double[] {closed.minY, closed.maxY}) {
            for (double z : new double[] {closed.minZ, closed.maxZ}) {
                double dy = y - hingeY;
                double dz = z - hingeZ;
                double turnedY = hingeY + dy * cos + up * dz * sin;
                double turnedZ = hingeZ + dz * cos - up * dy * sin;
                minY = Math.min(minY, turnedY);
                maxY = Math.max(maxY, turnedY);
                minZ = Math.min(minZ, turnedZ);
                maxZ = Math.max(maxZ, turnedZ);
            }
        }
        return new net.minecraft.world.phys.AABB(closed.minX, minY, minZ, closed.maxX, maxY, maxZ);
    }

    /**
     * An empty hand on a lid lifts it, or lets it down if it is up; into the
     * open boot it reaches for what is in there, into the bay for the parts.
     */
    /** Slower than this, blocks a tick, a car counts as standing still. */
    private static final float LID_STILL = 0.02F;

    private InteractionResult workLid(Player player, LidAim aim) {
        if (aim.inside()) {
            if (aim.lid() == BONNET_BIT) {
                return this.workshop.openService(player);
            }
            if (!this.level().isClientSide()) {
                player.openMenu(new net.minecraft.world.SimpleMenuProvider(
                        (syncId, inventory, ignored) ->
                                new net.minecraft.world.inventory.ChestMenu(net.minecraft.world.inventory.MenuType.GENERIC_9x2, syncId, inventory, this.boot, 2),
                        Component.translatable("container.car.boot")));
            }
            return InteractionResult.sidedSuccess(this.level().isClientSide());
        }
        // Nobody pops the bonnet of a car that is being driven or is still
        // rolling: on the road it would stop the engine under the driver.
        if (!this.isLidOpen(aim.lid())
                && (this.getControllingPassenger() != null || Math.abs(this.getEngineSpeed()) > LID_STILL)) {
            if (!this.level().isClientSide()) {
                player.displayClientMessage(Component.translatable("message.car.lid_moving"), true);
            }
            return InteractionResult.sidedSuccess(this.level().isClientSide());
        }
        if (!this.level().isClientSide()) {
            boolean open = !this.isLidOpen(aim.lid());
            this.setLidOpen(aim.lid(), open);
            this.level().playSound(null, this.getX(), this.getY() + 0.8, this.getZ(),
                    open ? SoundEvents.IRON_TRAPDOOR_OPEN : SoundEvents.IRON_TRAPDOOR_CLOSE,
                    SoundSource.NEUTRAL, 0.8F, aim.lid() == BONNET_BIT ? 0.8F : 1.0F);
        }
        return InteractionResult.sidedSuccess(this.level().isClientSide());
    }

    /**
     * A slab or a path's edge lifts the whole box in one tick; drawn like
     * that, the car hops. The body, and whoever rides in it, are drawn this
     * far below the box instead and rise to it over a few frames. Client only.
     */
    private float stepOffset;
    private float prevStepOffset;
    /** Which way it was going when it took the step: 1 forwards, -1 in reverse. */
    private float stepHeading = 1.0F;

    /** How much of the step is still to rise each tick: lower is quicker. */
    private static final float STEP_EASE = 0.72F;
    /**
     * Degrees of nose-up per block of step still to climb: the leading
     * wheels go up the kerb first, so the car tips up and settles level.
     */
    private static final float STEP_TILT = 14.0F;

    private void easeSteps(double yBefore) {
        this.prevStepOffset = this.stepOffset;
        double rise = this.getY() - yBefore;
        float offset = this.stepOffset;
        // A step up, or a step down the car was kept on the ground over. A
        // fall is already gradual, and a teleport is left alone.
        if (rise > 0.02 && rise <= 1.1 || this.followedDown > 0.0F) {
            offset -= (float) rise;
            this.stepHeading = this.flyDist < 0.0F ? -1.0F : 1.0F;
        }
        this.followedDown = 0.0F;
        this.stepOffset = Math.abs(offset) < 0.002F ? 0.0F : offset * STEP_EASE;
    }

    /** How far the car was set down a step this tick; read by {@link #easeSteps}. */
    private float followedDown;

    /** Off the edge of a step, a block or less: a car's wheels stay on the road. */
    private static final double STEP_DOWN = 1.05;

    /**
     * Rolled off the edge of a step: set the car down on the ground below,
     * the way its wheels would drop onto it, instead of launching it off
     * every block of a hillside. A real drop is left to fall.
     */
    private void followGroundDown() {
        net.minecraft.world.phys.AABB below = this.getBoundingBox().expandTowards(0.0, -STEP_DOWN, 0.0);
        if (this.level().noBlockCollision(this, below)) {
            return;
        }
        double from = this.getY();
        this.move(MoverType.SELF, new Vec3(0.0, -STEP_DOWN, 0.0));
        this.followedDown = (float) (from - this.getY());
    }

    /**
     * How far the steering turns it this tick, degrees, right positive: the
     * front wheels' share of the speed, the tyres' condition, and a slide
     * swinging the nose round further.
     */
    protected float turnRate(float steerInput, boolean drifting) {
        if (this.flyDist == 0.0F || steerInput == 0.0F) {
            return 0.0F;
        }
        float grip = Math.min(1.0F, Math.abs(this.flyDist) / FULL_GRIP_SPEED);
        float direction = this.flyDist < 0.0F ? -1.0F : 1.0F;
        return steerInput * MAX_TURN_RATE * this.steeringFactor() * grip * direction
                * (drifting ? DRIFT_TURN : 1.0F);
    }

    /**
     * Where a point given in the vehicle's own frame (left, up, forward)
     * ends up once the body is pitched and banked as drawn, about the same
     * middle the renderer turns it about.
     */
    public Vec3 withBodyAttitude(Vec3 local, float tickProgress) {
        float pitch = this.getBodyPitch(tickProgress);
        float roll = this.getBodyRoll(tickProgress);
        if (pitch == 0.0F && roll == 0.0F) {
            return local;
        }
        double middle = this.getBbHeight() / 2.0;
        return local.subtract(0.0, middle, 0.0)
                .zRot(-roll * Mth.DEG_TO_RAD)
                .xRot(pitch * Mth.DEG_TO_RAD)
                .add(0.0, middle, 0.0);
    }

    /** Whether the chunks under it and a few ticks ahead of it are loaded here. */
    private boolean worldLoadedAhead() {
        float yawRad = this.getYRot() * Mth.DEG_TO_RAD;
        double reach = (Math.abs(this.flyDist) * 4.0 + 1.0) * (this.flyDist < 0.0F ? -1.0 : 1.0);
        int aheadX = Mth.floor(this.getX() - Mth.sin(yawRad) * reach);
        int aheadZ = Mth.floor(this.getZ() + Mth.cos(yawRad) * reach);
        return this.level().hasChunkAt(this.getBlockX(), this.getBlockZ())
                && this.level().hasChunkAt(aheadX, aheadZ);
    }

    /**
     * Nose up, degrees, as drawn: level for anything on wheels, but for the
     * moment it takes a step, tipped up the kerb and settling as it rises.
     */
    public float getBodyPitch(float tickProgress) {
        return Mth.clamp(-this.getStepOffset(tickProgress) * STEP_TILT * this.stepHeading, -15.0F, 15.0F);
    }

    /** Banked to the right, degrees, as drawn: level for anything on wheels. */
    public float getBodyRoll(float tickProgress) {
        return 0.0F;
    }

    public float getStepOffset(float tickProgress) {
        return Mth.lerp(tickProgress, this.prevStepOffset, this.stepOffset);
    }

    @Override
    protected void positionRider(Entity passenger, Entity.MoveFunction callback) {
        float lift = this.level().isClientSide() ? this.stepOffset : 0.0F;
        if (lift == 0.0F) {
            super.positionRider(passenger, callback);
        } else {
            super.positionRider(passenger, (rider, x, y, z) -> callback.accept(rider, x, y + lift, z));
        }
        this.faceForward(passenger);
    }

    @Override
    public void onPassengerTurned(Entity passenger) {
        this.faceForward(passenger);
    }

    /** How far round a rider can look from the way the vehicle points, either side. */
    private static final float LOOK_ROUND = 105.0F;

    /**
     * Sat in a seat, a rider's body faces the way the vehicle does, as in a
     * boat, so their legs stay in the footwell whichever way they look; the
     * head turns, but not round past the shoulder.
     */
    private void faceForward(Entity passenger) {
        passenger.setYBodyRot(this.getYRot());
        float turned = Mth.wrapDegrees(passenger.getYRot() - this.getYRot());
        float allowed = Mth.clamp(turned, -LOOK_ROUND, LOOK_ROUND);
        passenger.yRotO += allowed - turned;
        passenger.setYRot(passenger.getYRot() + allowed - turned);
        passenger.setYHeadRot(passenger.getYRot());
    }

    @Override
    public void handleEntityEvent(byte status) {
        if (status == STATUS_LOCKED || status == STATUS_UNLOCKED) {
            this.lockFlashTicks = LOCK_FLASH_TICKS;
            this.lockFlashTwice = status == STATUS_UNLOCKED;
            return;
        }
        if (status == STATUS_MISFIRE) {
            if (this.isControlledByLocalInstance()) {
                this.flyDist *= MISFIRE_STUMBLE;
            }
            this.effects.coughExhaust();
            this.level().playLocalSound(this.getX(), this.getY() + 0.5, this.getZ(),
                    ModSounds.BACKFIRE, SoundSource.NEUTRAL, 0.7F,
                    0.85F + this.random.nextFloat() * 0.3F, false);
            return;
        }
        if (status == STATUS_SPUTTER) {
            // The driver's client is the one moving the car: it feels the cough.
            if (this.isControlledByLocalInstance()) {
                this.flyDist *= STARVE_STUMBLE;
            }
            this.effects.coughExhaust();
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
                new net.minecraft.core.particles.DustParticleOptions(net.minecraft.world.phys.Vec3.fromRGB24(0xFFA51F).toVector3f(), 0.8F);
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
        return this.parts.isFitted(CarPart.AIR_FILTER) && CarSetup.openFilter(this.getSetup());
    }

    /** A turbo is bolted to the engine, so without an engine there is none. */
    public boolean hasTurbo() {
        return this.parts.isFitted(CarPart.ENGINE) && CarSetup.turbo(this.getSetup()) && !this.isElectric();
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
        return this.hasFuel() && this.parts.isFitted(CarPart.ENGINE)
                && !this.getEngineFuel().burns(this.getTankFuel());
    }

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

    // ---- punctures ----

    /** What a flat leaves of the top speed. */
    private static final float FLAT_SPEED = 0.45F;
    /** Degrees a tick a flat pulls the car towards its side, at full grip. */
    private static final float FLAT_PULL = 0.7F;
    /** Below this much tread left, a tyre may go on its own. */
    private static final float BALD = 0.15F;
    /** The chance a tick that a bald tyre goes, at a car's cruising speed. */
    private static final float BALD_PUNCTURE_CHANCE = 0.0006F;
    /** The chance a tick that rolling over a cactus, a thorn bush or a spike lets a tyre down. */
    private static final float SHARP_PUNCTURE_CHANCE = 0.3F;

    public boolean hasFlatTyre() {
        return this.entityData.get(FLAT_TYRE) >= 0;
    }

    /** Which wheel is flat, by its place in the list of wheels, or -1. */
    public int getFlatTyre() {
        return this.entityData.get(FLAT_TYRE);
    }

    /** Lets a tyre down: which one, or -1 to have one changed by magic (commands, tests). */
    public void setFlatTyre(int wheel) {
        this.entityData.set(FLAT_TYRE, (byte) Mth.clamp(wheel, -1, this.tyreCount() - 1));
    }

    // ---- one tyre at a time ----

    /** How many wheels there are to put a tyre on: the vehicle's own, or a plain car's four. */
    public int tyreCount() {
        me.trossty.car.vehicle.VehicleDefinition defined = this.getDefinition();
        if (defined != null && !defined.wheels().isEmpty()) {
            return Math.min(8, defined.wheels().size());
        }
        return this.chassis() == me.trossty.car.vehicle.VehicleDefinition.Chassis.MOTORCYCLE ? 2 : 4;
    }

    /** Which wheels have had their tyres taken off, a bit each. */
    public int tyresOff() {
        return this.parts.isFitted(CarPart.WHEELS) ? this.entityData.get(TYRES_OFF) & 0xFF : (1 << this.tyreCount()) - 1;
    }

    /** Whether this wheel has its tyre on. */
    public boolean isTyreOn(int wheel) {
        return (this.tyresOff() >> wheel & 1) == 0;
    }

    /** How many wheels have their tyres on. */
    public int tyresOn() {
        return this.tyreCount() - Integer.bitCount(this.tyresOff() & ((1 << this.tyreCount()) - 1));
    }

    /** The first wheel without a tyre while others still have theirs, or -1: the car would sit on that hub. */
    private int missingTyre() {
        if (!this.parts.isFitted(CarPart.WHEELS)) {
            return -1;
        }
        for (int wheel = 0; wheel < this.tyreCount(); wheel++) {
            if (!this.isTyreOn(wheel)) {
                return wheel;
            }
        }
        return -1;
    }

    /** The wheel the car limps on: a flat one, or a bare hub; -1 if it rolls on four good tyres. */
    private int limpingWheel() {
        return this.hasFlatTyre() ? this.getFlatTyre() : this.missingTyre();
    }

    /** Which way a wheel's side drags the car: the right side right, the left side left. */
    private float sideOf(int wheel) {
        me.trossty.car.vehicle.VehicleDefinition defined = this.getDefinition();
        if (defined != null && wheel < defined.wheels().size()) {
            // The model's minus x is the vehicle's right.
            return defined.wheels().get(wheel).at().x() < 0.0F ? 1.0F : -1.0F;
        }
        return wheel % 2 == 0 ? -1.0F : 1.0F;
    }

    /** Where a wheel's middle is in the world. */
    public Vec3 wheelSpot(int wheel) {
        return this.position().add(this.parts.wheelLocal(wheel).yRot(-this.getYRot() * Mth.DEG_TO_RAD));
    }

    /**
     * The wheel the player is pointing at, or nearest it: among those with
     * a tyre on, those without, or any (null).
     */
    public int aimedWheel(Player player, @Nullable Boolean tyreOn) {
        Vec3 eye = player.getEyePosition();
        Vec3 look = player.getViewVector(1.0F);
        int best = -1;
        double bestMiss = Double.MAX_VALUE;
        for (int wheel = 0; wheel < this.tyreCount(); wheel++) {
            if (tyreOn != null && this.isTyreOn(wheel) != tyreOn) {
                continue;
            }
            Vec3 to = this.wheelSpot(wheel).subtract(eye);
            double along = Math.max(0.0, to.dot(look));
            double miss = to.subtract(look.scale(along)).lengthSqr();
            if (miss < bestMiss) {
                best = wheel;
                bestMiss = miss;
            }
        }
        return best;
    }

    /** Whether a block will cut a tyre that rolls over it. */
    private static boolean isSharp(net.minecraft.world.level.block.state.BlockState state) {
        return state.is(net.minecraft.world.level.block.Blocks.CACTUS)
                || state.is(net.minecraft.world.level.block.Blocks.SWEET_BERRY_BUSH)
                || state.is(net.minecraft.world.level.block.Blocks.POINTED_DRIPSTONE);
    }

    /**
     * Tyres on the road go flat: rolled over something sharp, or worn down
     * to the canvas. The car limps and pulls to that side until the wheel is
     * changed — a spare wheel clicked on it, anywhere it stopped.
     */
    private void tickPuncture() {
        if (this.hasFlatTyre() || !this.hasPart(CarPart.WHEELS) || !this.parts.isFitted(CarPart.WHEELS)
                || !this.onGround() || Math.abs(this.flyDist) < 0.05F) {
            return;
        }
        boolean sharp = false;
        net.minecraft.world.phys.AABB box = this.getBoundingBox();
        for (BlockPos pos : BlockPos.betweenClosed(BlockPos.containing(box.minX, box.minY - 0.1, box.minZ),
                BlockPos.containing(box.maxX, box.minY + 0.5, box.maxZ))) {
            if (isSharp(this.level().getBlockState(pos))) {
                sharp = true;
                break;
            }
        }
        float chance = sharp ? SHARP_PUNCTURE_CHANCE : 0.0F;
        if (this.parts.getConditionFraction(CarPart.WHEELS) < BALD) {
            chance += BALD_PUNCTURE_CHANCE * Math.min(2.0F, Math.abs(this.flyDist) / 0.4F);
        }
        if (chance <= 0.0F || this.random.nextFloat() >= chance) {
            return;
        }
        int wheel = this.random.nextInt(this.tyreCount());
        if (!this.isTyreOn(wheel)) {
            return;
        }
        this.setFlatTyre(wheel);
        this.playSound(SoundEvents.FIREWORK_ROCKET_BLAST, 1.2F, 0.5F);
        if (this.getControllingPassenger() instanceof Player driver) {
            driver.displayClientMessage(Component.translatable("message.car.tyre_flat"), true);
        }
    }

    /** Whether rain is falling on the ground under the car: under a roof, it stays dry. */
    public boolean isOnWetGround() {
        return this.level().isRainingAt(this.blockPosition().above());
    }

    /** How well the fitted tyres hold the ground the car is on. */
    public float getGrip() {
        // No tyres on anything: a hull in the water, or wheels up in the air.
        if (!this.hasPart(CarPart.WHEELS) || this.isFlying()) {
            return 1.0F;
        }
        CarSetup.Surface surface = this.getSurface();
        float grip = this.getTyre().grip(surface, this.isOnWetGround());
        float offroad = this.tuning().offroad();
        if (surface != CarSetup.Surface.ASPHALT && offroad > 0.0F && grip < 1.0F) {
            // Ground clearance and four driven wheels give back what the tyre loses.
            grip = Mth.lerp(offroad, grip, 1.0F);
        }
        return grip * this.upkeep.softTyreGrip();
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
                    && this.parts.isFitted(CarPart.ENGINE)) {
                // A diesel lights itself.
                continue;
            }
            if (part.essential() && this.hasPart(part) && !this.parts.isFitted(part)) {
                return part;
            }
        }
        return null;
    }

    /** A flat battery is not a missing one, and stops the car just as dead. */
    public boolean isBatteryFlat() {
        return this.parts.getCondition(CarPart.BATTERY) <= FLAT_BATTERY;
    }

    /** Whether the engine can turn at all, fuel aside. */
    public boolean canRun() {
        return this.missingEssential() == null && !this.isBatteryFlat() && !this.hasWrongFuel() && !this.upkeep.isBeltSnapped()
                && !this.entityData.get(KEY_MISSING) && !this.isFlooded() && !this.isAnyLidOpen();
    }

    /** With the bonnet or the boot up there is no driving off: the lid is in the way. */
    public boolean isAnyLidOpen() {
        return this.entityData.get(LIDS) != 0;
    }

    /** Water in the engine: it will not start until it dries out. */
    public boolean isFlooded() {
        return this.entityData.get(FLOODED);
    }

    /**
     * Water deep enough to reach the intake drowns the engine, and it stays
     * drowned for a while after the car is back on land. Anybody whose head
     * goes under gets out and swims for it: NeoForge leaves that to the
     * vehicle, and a car is not something to sit in at the bottom of a lake.
     */
    private void tickWater() {
        if (this.floods() && this.getFluidHeight(net.minecraft.tags.FluidTags.WATER) >= FLOOD_DEPTH) {
            this.floodedTicks = DRY_OUT_TICKS;
        } else if (this.floodedTicks > 0) {
            this.floodedTicks--;
        }
        boolean flooded = this.floodedTicks > 0;
        if (flooded != this.isFlooded()) {
            if (flooded) {
                this.drown(this.isEngineRunning());
            }
            this.entityData.set(FLOODED, flooded);
            if (flooded) {
                this.level().playSound(null, this.getX(), this.getY() + 0.5, this.getZ(),
                        SoundEvents.FIRE_EXTINGUISH, SoundSource.NEUTRAL, 0.8F, 0.6F);
            }
        }
        for (Entity passenger : List.copyOf(this.getPassengers())) {
            if (passenger instanceof LivingEntity living && eyeUnderWater(living)
                    && !living.canBreatheUnderwater()) {
                living.stopRiding();
            }
        }
    }

    /**
     * Whether the water is over this rider's eyes where they sit now. Their
     * own flag is from their last tick, before they climbed in: somebody
     * swimming up to a boat would be thrown straight back out by it.
     */
    private static boolean eyeUnderWater(LivingEntity living) {
        Vec3 eye = living.getEyePosition();
        net.minecraft.core.BlockPos at = net.minecraft.core.BlockPos.containing(eye);
        net.minecraft.world.level.material.FluidState fluid = living.level().getFluidState(at);
        return fluid.is(net.minecraft.tags.FluidTags.WATER)
                && eye.y < at.getY() + fluid.getHeight(living.level(), at);
    }

    /**
     * What going under does to the car, once, as the water comes in. The
     * airbox soaks either way. A running engine also breathes the water in,
     * and water does not compress: the plugs and the engine take a beating
     * that drying out does not undo. An electric car shorts its battery.
     */
    private void drown(boolean running) {
        if (this.isElectric()) {
            this.parts.wear(CarPart.BATTERY, DROWNED_BATTERY);
            return;
        }
        this.parts.wear(CarPart.AIR_FILTER, DROWNED_FILTER);
        if (running) {
            this.parts.wear(CarPart.SPARK_PLUGS, DROWNED_PLUGS);
            this.parts.wear(CarPart.ENGINE, DROWNED_ENGINE);
        }
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
        } else if (this.isAnyLidOpen()) {
            driver.displayClientMessage(Component.translatable(this.isLidOpen(BONNET_BIT)
                    ? "message.car.bonnet_open" : "message.car.boot_open"), true);
        } else if (this.isFlooded()) {
            driver.displayClientMessage(Component.translatable("message.car.flooded"), true);
        } else if (this.upkeep.isBeltSnapped()) {
            driver.displayClientMessage(Component.translatable("message.car.belt_snapped"), true);
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
        } else if (this.upkeep.hasOil() && this.upkeep.getOilLevel() < Upkeep.FLUID_LOW) {
            driver.displayClientMessage(Component.translatable("message.car.oil_low"), true);
        } else if (this.hasPart(CarPart.RADIATOR) && this.upkeep.getCoolantLevel() < Upkeep.FLUID_LOW) {
            driver.displayClientMessage(Component.translatable("message.car.coolant_low"), true);
        } else if (this.hasPart(CarPart.BRAKE_PADS) && this.upkeep.getBrakeFluid() < Upkeep.FLUID_LOW) {
            driver.displayClientMessage(Component.translatable("message.car.brake_fluid_low"), true);
        } else if (this.isLeaking() && this.hasFuel()) {
            driver.displayClientMessage(Component.translatable("message.car.tank_leaking"), true);
        } else if (this.upkeep.tyreSoftness() > 0.0F) {
            driver.displayClientMessage(Component.translatable("message.car.tyres_soft"), true);
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
            float cooling = this.parts.partFactor(
                    CarPart.RADIATOR, WORN_RADIATOR_COOLING, MISSING_RADIATOR_COOLING)
                    * this.upkeep.coolantCooling();
            float heat = this.getLayout().heat() * (this.hasTurbo() ? CarSetup.TURBO_HEAT : 1.0F);
            // Old oil stops carrying the heat away: the gauge creeps up.
            float oil = 1.0F - Math.min(1.0F,
                    this.parts.getConditionFraction(CarPart.OIL_FILTER) / FaultCodes.WARNING);
            target = Math.min(1.0F,
                    (0.25F + (0.40F * heat + DIRTY_OIL_HEAT * oil) * load) / (cooling * this.parts.grade(CarPart.RADIATOR)));
        }
        float rate = target > temp ? TEMP_RISE : TEMP_FALL;
        this.entityData.set(ENGINE_TEMP, Mth.lerp(rate, temp, target));
    }

    // ---- oil and coolant ----

    // ---- what each part is worth on the road ----

    /** How much speed one tick of throttle adds, through the whole driveline. */
    public float throttleStep() {
        return ACCELERATION * this.tuning().acceleration() * this.towRatio()
                * this.parts.partFactor(CarPart.TRANSMISSION, WORN_GEARBOX_DRIVE)
                * this.parts.partFactor(CarPart.CLUTCH, WORN_CLUTCH_DRIVE)
                * this.parts.partFactor(CarPart.DRIVESHAFT, WORN_SHAFT_DRIVE)
                * this.parts.partFactor(CarPart.ENGINE, 0.8F)
                * this.parts.firing()
                * this.parts.grade(CarPart.ENGINE)
                * this.parts.grade(CarPart.TRANSMISSION)
                * this.parts.grade(CarPart.CLUTCH)
                * this.parts.minorGrade(CarPart.DRIVESHAFT)
                * (this.getEngineFuel().needsSpark()
                        ? this.parts.partFactor(CarPart.SPARK_PLUGS, WORN_PLUGS_THROTTLE) : 1.0F)
                * this.getLayout().pull()
                * this.getEngineFuel().pull()
                * (this.hasTurbo() ? CarSetup.TURBO_PULL : 1.0F)
                * (this.hasOpenFilter() ? CarSetup.OPEN_FILTER_PULL : 1.0F)
                * this.upkeep.catFactor()
                * this.traction()
                * this.loadFactor();
    }

    protected float maxReverseSpeed() {
        return MAX_REVERSE_SPEED * this.parts.partFactor(CarPart.TRANSMISSION, WORN_GEARBOX_REVERSE);
    }

    /**
     * Braking, which is three parts deep: the pads do the work, the discs they
     * press on matter, and whatever either of them manages still has to reach
     * the road through the tyres.
     */
    public float brakeStrength() {
        return BRAKE_STRENGTH * this.tuning().braking()
                * this.parts.partFactor(CarPart.BRAKE_PADS, WORN_PADS_BRAKE, MISSING_PADS_BRAKE)
                * this.parts.partFactor(CarPart.BRAKE_DISCS, WORN_DISCS_BRAKE, MISSING_DISCS_BRAKE)
                * this.parts.partFactor(CarPart.WHEELS, WORN_TYRE_GRIP)
                * this.parts.grade(CarPart.BRAKE_PADS)
                * this.parts.grade(CarPart.BRAKE_DISCS)
                * this.upkeep.brakeFluidFactor()
                * this.parts.minorGrade(CarPart.WHEELS)
                * this.getGrip();
    }

    /**
     * How hard this car pulls off a straight line, degrees a tick, signed by
     * side. A worn rack or tired springs both do it; which way is the car's
     * own, fixed by its identity, as a real one always drifts the same way.
     */
    public float pull() {
        // A flat drags the car hard over to its own side, whatever else is true.
        int limping = this.limpingWheel();
        float flat = limping >= 0 ? this.sideOf(limping) * FLAT_PULL : 0.0F;
        return flat + this.parts.wornPull() + this.upkeep.getAlignment() * MAX_PULL;
    }

    /** Turn rate: the rack points the wheels, the tyres decide what happens. */
    public float steeringFactor() {
        return this.tuning().steering() * this.parts.partFactor(CarPart.WHEELS, WORN_TYRE_GRIP)
                * this.parts.partFactor(CarPart.STEERING_RACK, WORN_RACK_STEER, MISSING_RACK_STEER)
                * this.parts.grade(CarPart.STEERING_RACK)
                * this.parts.minorGrade(CarPart.WHEELS)
                * this.getGrip();
    }

    /** How much more fuel this car drinks than a clean one would. */
    public float thirstFactor() {
        return this.tuning().fuelUse() * this.parts.partFactor(CarPart.AIR_FILTER, WORN_FILTER_THIRST, MISSING_FILTER_THIRST)
                * this.getLayout().thirst()
                * this.getEngineFuel().thirst()
                // Biofuel is weaker stuff: whatever the engine, more of it goes.
                * (this.getTankFuel() == CarSetup.Fuel.BIOFUEL ? CarSetup.Fuel.BIOFUEL.thirst() : 1.0F)
                * (this.hasTurbo() ? CarSetup.TURBO_THIRST : 1.0F)
                * (this.hasOpenFilter() ? CarSetup.OPEN_FILTER_THIRST : 1.0F)
                // Soft tyres drag.
                * (1.0F + 0.15F * this.upkeep.tyreSoftness());
    }

    // ---- working on it ----

    /**
     * Whether this kind of vehicle has the part at all. A car has every one;
     * a trailer has no engine, gearbox or exhaust to service, only what it
     * rolls and stops on. A motor burns nothing, so an electric car has no
     * exhaust, plugs, filters or clutch to wear out.
     */
    public boolean hasPart(CarPart part) {
        return !this.isElectric() || switch (part) {
            case EXHAUST, SPARK_PLUGS, AIR_FILTER, OIL_FILTER, CLUTCH -> false;
            default -> true;
        };
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
        if (!(this instanceof TrailerEntity)) {
            // Walking a trailer up by its drawbar: a click drops it on the tow ball.
            TrailerEntity dragged = TrailerEntity.heldBy(player);
            if (dragged != null) {
                return dragged.hitchOnto(this, player);
            }
        }
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
        net.minecraft.resources.ResourceLocation model = this.getModelId();
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
            return this.workshop.openService(player);
        }

        ItemStack held = player.getItemInHand(hand);
        if (this.isLeaking() && (held.is(Items.SLIME_BALL) || held.is(Items.HONEYCOMB))) {
            return this.patchTank(player, held);
        }

        // A jerry can or the pump's nozzle held to the filler cap is poured by
        // keeping the button down, which is the item's business, not the
        // car's: PASS hands the click on to it (JerryCanItem, FuelNozzleItem).
        // Anywhere else on the car they are just something in your hand.
        if ((held.is(ModItems.JERRY_CAN) || held.is(ModItems.FUEL_NOZZLE) || held.is(ModItems.CHARGING_PLUG))
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
            return this.workshop.openService(player);
        }
        if (held.is(ModItems.OBD_SCANNER) && this.hasPart(CarPart.ENGINE)) {
            // Something with an engine computer to read: not a trailer.
            me.trossty.car.item.ObdScannerItem.pair(held, this, player);
            return InteractionResult.SUCCESS;
        }
        if (held.is(ModItems.COMPRESSION_TESTER)) {
            return me.trossty.car.item.CompressionTesterItem.test(this, player);
        }
        if (held.is(ModItems.TURBO)) {
            return this.workshop.fitTurbo(player, held);
        }
        if (held.is(ModItems.WRENCH)) {
            return this.upkeep.alignWheels(player);
        }
        if (held.is(ModItems.TIMING_BELT)) {
            return this.upkeep.fitBelt(player, held);
        }
        if (held.is(ModItems.CATALYTIC_CONVERTER)) {
            return this.upkeep.fitCat(player, held);
        }
        if (held.is(ModItems.TYRE_PUMP)) {
            return this.upkeep.pumpTyres(player);
        }
        if (held.is(ModItems.JUMPER_CABLES)) {
            InteractionResult jumped = this.upkeep.jumpWith(player, held);
            if (jumped != InteractionResult.PASS) {
                return jumped;
            }
        }
        InteractionResult washed = this.upkeep.wash(player, held);
        if (washed != InteractionResult.PASS) {
            return washed;
        }
        InteractionResult topped = this.upkeep.topUp(player, held);
        if (topped != InteractionResult.PASS) {
            return topped;
        }
        CarPart spare = CarPart.forItem(held.getItem());
        if (spare != null) {
            return this.workshop.fitPart(player, held, spare);
        }
        if (held.is(Items.IRON_INGOT)) {
            InteractionResult repaired = this.damage.repairWith(player, held);
            if (repaired != InteractionResult.PASS) {
                return repaired;
            }
        }
        if (held.isEmpty()) {
            LidAim lid = this.aimedLid(player);
            if (lid != null) {
                return this.workLid(player, lid);
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
        //
        // An aircraft carries its seats with it as it noses up or banks, so
        // the crew stay in the cabin rather than the cabin swinging round them.
        Vec3 seat = this.seatPosition(Math.max(0, this.getPassengers().indexOf(passenger)));
        return this.withBodyAttitude(seat, 1.0F).yRot(-this.getYRot() * ((float) Math.PI / 180.0F));
    }

    @Override
    protected void removePassenger(Entity passenger) {
        super.removePassenger(passenger);
        // While somebody drove, their client moved the car and ignored the
        // server's small "moved this much since last time" updates, so it
        // still counts them from wherever the car was when they got in. Sent
        // now, the car's exact position resets that, or the car would show
        // back there after they got out until the next full sync.
        if (this.level() instanceof net.minecraft.server.level.ServerLevel server) {
            server.getChunkSource().broadcast(this,
                    new net.minecraft.network.protocol.game.ClientboundTeleportEntityPacket(this));
        }
    }

    /**
     * Out through the door beside the seat, onto the ground, the way a boat
     * puts you on the bank. Vanilla would stand you on the roof. If that side
     * is walled in, the other door, then out the back or the front.
     */
    @Override
    public Vec3 getDismountLocationForPassenger(LivingEntity passenger) {
        Vec3 seat = this.seatPosition(Math.max(0, this.getPassengers().indexOf(passenger)));
        float yawRad = this.getYRot() * ((float) Math.PI / 180.0F);
        double door = seat.x < 0.0 ? -1.0 : 1.0;
        Vec3[] ways = {new Vec3(door, 0.0, 0.0), new Vec3(-door, 0.0, 0.0),
                new Vec3(0.0, 0.0, -1.0), new Vec3(0.0, 0.0, 1.0)};
        for (Vec3 way : ways) {
            Vec3 out = way.yRot(-yawRad);
            // Just clear of the car's (square, unturned) box, whichever way it faces.
            double reach = (this.getBbWidth() + passenger.getBbWidth() + 0.1) / 2.0
                    / Math.max(Math.abs(out.x), Math.abs(out.z));
            // Level with the seat when getting out of a door.
            Vec3 along = way.x != 0.0 ? new Vec3(0.0, 0.0, seat.z).yRot(-yawRad) : Vec3.ZERO;
            for (Vec3 offset : new Vec3[]{out.scale(reach).add(along), out.scale(reach)}) {
                Vec3 found = this.dismountSpot(passenger, this.getX() + offset.x, this.getZ() + offset.z);
                if (found != null) {
                    return found;
                }
            }
        }
        return super.getDismountLocationForPassenger(passenger);
    }

    /** Somewhere to stand at (x, z), near the car's own height and clear of it, or null. */
    private Vec3 dismountSpot(LivingEntity passenger, double x, double z) {
        BlockPos base = BlockPos.containing(x, this.getY() + 0.5, z);
        for (BlockPos pos : new BlockPos[]{base, base.below(), base.above()}) {
            double floor = this.level().getBlockFloorHeight(pos);
            if (!net.minecraft.world.entity.vehicle.DismountHelper.isBlockFloorValid(floor)) {
                continue;
            }
            Vec3 at = new Vec3(x, pos.getY() + floor, z);
            for (net.minecraft.world.entity.Pose pose : passenger.getDismountPoses()) {
                net.minecraft.world.phys.AABB box = passenger.getLocalBoundsForPose(pose).move(at);
                if (!box.intersects(this.getBoundingBox())
                        && net.minecraft.world.entity.vehicle.DismountHelper.canDismountTo(
                                this.level(), passenger, box)) {
                    passenger.setPose(pose);
                    return at;
                }
            }
        }
        return null;
    }

    @Override
    protected double getDefaultGravity() {
        // Entity.getGravity() is 0 by default; vehicles have to opt in, as boats do.
        return 0.04;
    }

    @Override
    public float maxUpStep() {
        // A block, as a wheel this size rides up a kerb: the land is made of them.
        return 1.0F;
    }

    @Override
    public boolean canBeCollidedWith() {
        return true;
    }

    @Override
    public boolean canCollideWith(Entity entity) {
        // A car and the trailer on its hitch overlap on a tight turn; they
        // must not shove each other apart.
        return !this.hitchedWith(entity) && super.canCollideWith(entity);
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
                    new net.minecraft.core.particles.DustParticleOptions(net.minecraft.world.phys.Vec3.fromRGB24(colour).toVector3f(), 1.4F);
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
    public void remove(Entity.RemovalReason reason) {
        if (reason.shouldDestroy() && this.level() instanceof ServerLevel world) {
            net.minecraft.world.Containers.dropContents(world, (Entity) this, this.boot);
            this.boot.clearContent();
            this.spillCargo(world);
        }
        super.remove(reason);
    }

    /** Puts whatever the vehicle carries on the ground. Nothing, for a plain car. */
    protected void spillCargo(ServerLevel world) {
    }

    /** The four conditions as whole percents, in {@link CarPart} order. */
    public List<Integer> partConditions() {
        List<Integer> conditions = new java.util.ArrayList<>(CarPart.values().length);
        for (CarPart part : CarPart.values()) {
            conditions.add(Math.round(this.parts.getConditionFraction(part) * 100.0F));
        }
        return List.copyOf(conditions);
    }

    /** Puts a saved set of conditions back on, ignoring anything malformed. */
    public void setPartConditions(List<Integer> conditions) {
        CarPart[] parts = CarPart.values();
        for (int index = 0; index < Math.min(parts.length, conditions.size()); index++) {
            this.parts.setCondition(parts[index], conditions.get(index) / 100.0F * CarPart.MAX_CONDITION);
        }
    }

    /**
     * Top speed for the car's current condition. A battered car still drives,
     * just not as briskly; it would be down to WRECKED_SPEED_FACTOR at zero,
     * though it falls apart before it gets there.
     */
    public float getMaxSpeed() {
        // Weight behind takes less off the top speed than off the pull away.
        return (0.5F + 0.5F * this.towRatio())
                * this.tuning().speed() * Mth.lerp(this.damage.getHealthFraction(), MAX_SPEED * WRECKED_SPEED_FACTOR, MAX_SPEED)
                * this.parts.partFactor(CarPart.ENGINE, WORN_ENGINE_SPEED)
                * this.parts.firing()
                * this.parts.partFactor(CarPart.AIR_FILTER, WORN_FILTER_SPEED, MISSING_FILTER_SPEED)
                * this.parts.partFactor(CarPart.EXHAUST, WORN_EXHAUST_SPEED, MISSING_EXHAUST_SPEED)
                * this.parts.grade(CarPart.ENGINE)
                * this.parts.minorGrade(CarPart.EXHAUST)
                * this.parts.minorGrade(CarPart.AIR_FILTER)
                * this.getLayout().speed()
                * this.getEngineFuel().speed()
                * (this.hasTurbo() ? CarSetup.TURBO_SPEED : 1.0F)
                * (this.hasOpenFilter() ? CarSetup.OPEN_FILTER_SPEED : 1.0F)
                // Wheels that are slipping do not reach the speed the engine could.
                * (0.7F + 0.3F * this.traction())
                * (this.isOverheating() ? OVERHEATING_POWER : 1.0F)
                * this.upkeep.catFactor()
                // On a flat it limps: driven faster, the rim would cut the tyre up.
                * (this.hasFlatTyre() ? FLAT_SPEED : 1.0F)
                // On a bare hub it scrapes along at a crawl.
                * (this.missingTyre() >= 0 ? FLAT_SPEED * 0.4F : 1.0F);
    }

    // ---- automatic gearbox ----

    /**
     * What this car is built to do flat out: its body, engine, fuel, turbo and
     * graded parts, but none of its wear. The speedometer is printed to this, so
     * a tuned car gets a dial that reads higher and a lorry one that reads lower.
     */
    public float getRatedTopSpeed() {
        return this.tuning().speed() * MAX_SPEED
                * this.parts.grade(CarPart.ENGINE)
                * this.parts.minorGrade(CarPart.EXHAUST)
                * this.parts.minorGrade(CarPart.AIR_FILTER)
                * this.getLayout().speed()
                * this.getEngineFuel().speed()
                * (this.hasTurbo() ? CarSetup.TURBO_SPEED : 1.0F)
                * (this.hasOpenFilter() ? CarSetup.OPEN_FILTER_SPEED : 1.0F);
    }

    /**
     * Whether there are gears to change at all. An electric motor pulls from
     * nothing to flat out on one ratio, a propeller sits on the crankshaft and
     * an outboard has forward and reverse only: none of them ever shifts.
     */
    public boolean hasGearbox() {
        return !this.isElectric();
    }

    /**
     * A car cannot be picked back up once it is put down. Every blow dents
     * it — a player's, a blast's, a mob's — and a creative player's clears it
     * away without giving anything back.
     */
    @Override
    public boolean hurt(DamageSource source, float amount) {
        if (!(this.level() instanceof ServerLevel world) || this.isRemoved()) {
            return true;
        }
        if (this.isInvulnerableTo(source)) {
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
        // A fist or a sword is no crowbar: it takes a good while to beat a car
        // to scrap by hand, where a blast or a falling anvil does it quickly.
        if (source.isDirect() && source.getEntity() instanceof Player) {
            amount *= PLAYER_BLOW;
        }
        // A blow from somewhere lands on the side facing it: an arrow, a fist,
        // a blast. Fire, cactus or the void come from no side and dent it all.
        Vec3 from = source.getSourcePosition();
        this.damage.applyCarDamage(world, amount, from == null ? null : BodyZone.facing(this, from));
        return true;
    }

    @Override
    public boolean causeFallDamage(float fallDistance, float damagePerDistance, DamageSource damageSource) {
        if (fallDistance > SAFE_FALL_BLOCKS && this.level() instanceof ServerLevel world) {
            this.damage.applyCarDamage(world, (float) (fallDistance - SAFE_FALL_BLOCKS) * FALL_DAMAGE_PER_BLOCK);
        }
        // Still hands the drop on to whoever was inside, as vanilla vehicles do.
        return super.causeFallDamage(fallDistance, damagePerDistance, damageSource);
    }

    /**
     * Rocks the body the way a vanilla boat or minecart rocks when struck, and
     * tells nearby clients at once so the jolt lands with the hit.
     */
    void rockFromHit(float amount) {
        this.setHurtDir(-this.getHurtDir());
        this.setHurtTime(10);
        this.setDamage(Math.min(this.getDamage() + amount * 10.0F, 40.0F));
        this.markHurt();
    }

    /** How much worse than a car's a crash is for this one: a light airframe crumples. */
    protected float crashFragility() {
        return 1.0F;
    }

    /** Anything more a wreck makes of itself than smoke and a crunch: nothing, for a car. */
    protected void wreckEffects(ServerLevel world) {
    }

    /**
     * Where the tailpipe ends, in the vehicle's own frame (x across, z towards
     * the front, in blocks): just behind its own tail, so a long lorry does not
     * puff from the middle of its load bed and a scooter not from thin air.
     */
    protected Vec3 exhaustOffset() {
        me.trossty.car.vehicle.VehicleDefinition defined = this.getDefinition();
        if (defined != this.exhaustFor) {
            this.exhaustFor = defined;
            this.exhaustAt = new Vec3(EXHAUST_SIDEWAYS, EXHAUST_HEIGHT, EXHAUST_BACK);
            if (defined != null) {
                float rear = Float.MAX_VALUE;
                for (me.trossty.car.vehicle.VehicleDefinition.Box box : defined.shape()) {
                    rear = Math.min(rear, Math.min(box.from().z(), box.to().z()));
                }
                double side = Math.min(EXHAUST_SIDEWAYS, defined.width() / 2.0 - 0.15);
                this.exhaustAt = new Vec3(side, EXHAUST_HEIGHT, rear / 16.0 - 0.05);
            }
        }
        return this.exhaustAt;
    }

    @Override
    protected void readAdditionalSaveData(net.minecraft.nbt.CompoundTag tag) {
        ValueInput view = ValueInput.of(tag, this.registryAccess());
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
        this.entityData.set(FLAT_TYRE, (byte) Mth.clamp(view.getIntOr("FlatTyre", -1), -1, 7));
        this.entityData.set(TYRES_OFF, (byte) view.getIntOr("TyresOff", 0));
        this.entityData.set(LIDS, (byte) (view.getIntOr("Lids", 0) & (BONNET_BIT | BOOT_BIT)));
        this.upkeep.load(view);
        if (view.tag().contains("Boot")) {
            net.minecraft.world.ContainerHelper.loadAllItems(
                    view.tag().getCompound("Boot"), this.boot.getItems(), view.registries());
        }
        this.show(this.boot, SHOWN_BOOT);
        this.damage.load(view);
        this.parts.load(view);
        this.setSetup(view.getIntOr("Setup", CarSetup.DEFAULT));
        this.setDirtyFuel(view.getFloatOr("DirtyFuel", 0.0F));
    }

    @Override
    protected void addAdditionalSaveData(net.minecraft.nbt.CompoundTag tag) {
        ValueOutput view = ValueOutput.of(tag, this.registryAccess());
        view.putFloat("Fuel", this.getFuel());
        view.putBoolean("Leaking", this.isLeaking());
        view.putInt("Color", this.getColor().getId());
        view.putInt("Paint", this.getPaint());
        view.putLong("LockCode", this.lockCode);
        view.putBoolean("Locked", this.isLocked());
        if (this.hasFlatTyre()) {
            view.putInt("FlatTyre", this.entityData.get(FLAT_TYRE));
        }
        if (this.parts.isFitted(CarPart.WHEELS) && this.tyresOff() != 0) {
            view.putInt("TyresOff", this.tyresOff());
        }
        if (this.entityData.get(LIDS) != 0) {
            view.putInt("Lids", this.entityData.get(LIDS));
        }
        this.upkeep.save(view);
        if (!this.boot.isEmpty()) {
            view.tag().put("Boot", net.minecraft.world.ContainerHelper.saveAllItems(
                    new net.minecraft.nbt.CompoundTag(), this.boot.getItems(), view.registries()));
        }
        this.damage.save(view);
        this.parts.save(view);
        view.putInt("Setup", this.getSetup());
        view.putFloat("DirtyFuel", this.getDirtyFuel());
        if (!this.entityData.get(MODEL).isEmpty()) {
            view.putString("Model", this.entityData.get(MODEL));
        }
    }
}
