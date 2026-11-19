package me.trossty.car.entity;

import net.minecraft.util.Mth;

import net.minecraft.tags.BlockTags;
import net.minecraft.world.level.block.state.BlockState;

/**
 * What kind of car this is, as opposed to what state it is in.
 *
 * <p>{@link CarPart} says which slots a car has and how worn each one is.
 * This says what is in three of them: which engine, which tyres, which air
 * filter — and whether a turbo has been bolted to the engine, and what the
 * tank was last filled with. It is one small number on the car, packed the
 * way the grades are, because every one of these is a choice from a short
 * list.
 *
 * <p>A part's own kind travels on the item as {@code car:part_variant}: for
 * an engine that is {@link #engineVariant}, for a wheel a {@link Tyre}
 * ordinal, for an air filter 0 or 1. A part with no variant on it is the
 * plain one — a petrol four, a road tyre, a paper filter — which is what
 * every car in an old save has in it.
 */
public final class CarSetup {

    /** How the cylinders are laid out, which is how big the engine is. */
    public enum Layout {

        /** The yardstick: what a car has always had. */
        INLINE4("inline4", 4, 1.00F, 1.00F, 1.00F, 1.00F, 1),

        /** Smooth and long. More of everything, a little more thirst. */
        INLINE6("inline6", 6, 1.12F, 1.10F, 1.25F, 1.08F, 2),

        /** Short and eager: pulls harder than the six for its size. */
        V6("v6", 6, 1.15F, 1.18F, 1.35F, 1.12F, 2),

        /** The big one. Fast, thirsty, hot. */
        V8("v8", 8, 1.30F, 1.35F, 1.70F, 1.25F, 3);

        private final String key;
        private final int cylinders;
        private final float speed;
        private final float pull;
        private final float thirst;
        private final float heat;
        private final int costFactor;

        Layout(String key, int cylinders, float speed, float pull, float thirst, float heat,
               int costFactor) {
            this.key = key;
            this.cylinders = cylinders;
            this.speed = speed;
            this.pull = pull;
            this.thirst = thirst;
            this.heat = heat;
            this.costFactor = costFactor;
        }

        /** The name used in {@code engine.car.<key>}. */
        public String key() {
            return this.key;
        }

        public int cylinders() {
            return this.cylinders;
        }

        /** What top speed is multiplied by. */
        public float speed() {
            return this.speed;
        }

        /** What acceleration is multiplied by. */
        public float pull() {
            return this.pull;
        }

        /** What fuel burn is multiplied by. */
        public float thirst() {
            return this.thirst;
        }

        /** How much faster it warms up under load. */
        public float heat() {
            return this.heat;
        }

        /** What the bench multiplies the engine's metal by. */
        public int costFactor() {
            return this.costFactor;
        }

        /**
         * Where in the rev range this engine pulls hardest, as a share of the
         * redline, and how sharply it falls away either side: a four wants
         * revving, a six is broad and even, an eight shoves from low down.
         */
        float torquePeak() {
            return switch (this) {
                case INLINE4 -> 0.7F;
                case INLINE6, V6 -> 0.6F;
                case V8 -> 0.5F;
            };
        }

        float torqueFall() {
            return switch (this) {
                case INLINE4 -> 1.6F;
                case INLINE6 -> 1.0F;
                case V6 -> 1.3F;
                case V8 -> 1.1F;
            };
        }
    }

    /** Revs, as a share of the redline, an automatic changes up at. */
    public static final float UPSHIFT = 0.9F;
    /** And down at. */
    public static final float DOWNSHIFT = 0.36F;
    /** However far off its peak, an engine never pulls less than this share of it. */
    private static final float TORQUE_FLOOR = 0.45F;
    /** Revs past which an electric motor runs out of current and its push falls away. */
    private static final float ELECTRIC_KNEE = 0.45F;

    /**
     * How hard this engine pulls at these revs (a share of the redline), against
     * its own average: above 1 at its peak, below it at either end. Averaged
     * over the revs an automatic keeps it between it comes to 1, so a car
     * covers the same ground on the whole, only differently: a diesel shoves
     * early and runs out, a four is lazy until it is wound up.
     *
     * <p>An electric motor, with no gears, sees its whole range: full push
     * from standstill, then the push falls away as its power runs out.
     */
    public static float torque(Layout layout, Fuel fuel, float revs) {
        if (fuel == Fuel.ELECTRIC) {
            return electricTorque(revs) / ELECTRIC_MEAN;
        }
        float sum = 0.0F;
        int samples = 16;
        for (int i = 0; i < samples; i++) {
            sum += rawTorque(layout, fuel, Mth.lerp((i + 0.5F) / samples, DOWNSHIFT, UPSHIFT));
        }
        return rawTorque(layout, fuel, revs) / (sum / samples);
    }

    private static float rawTorque(Layout layout, Fuel fuel, float revs) {
        float peak = fuel == Fuel.DIESEL ? 0.4F : layout.torquePeak();
        float fall = fuel == Fuel.DIESEL ? 2.2F : layout.torqueFall();
        float off = revs - peak;
        return Math.max(TORQUE_FLOOR, 1.0F - fall * off * off);
    }

    private static float electricTorque(float revs) {
        return revs <= ELECTRIC_KNEE ? 1.0F : ELECTRIC_KNEE / revs;
    }

    /** What {@link #electricTorque} averages from standstill to flat out. */
    private static final float ELECTRIC_MEAN = ELECTRIC_KNEE + ELECTRIC_KNEE * (float) Math.log(1.0 / ELECTRIC_KNEE);

    /** What the engine burns, and so what has to be in the tank. */
    public enum Fuel {

        /** 98 unleaded. Revs, and needs a spark to do it. */
        PETROL("petrol", 1.00F, 1.00F, 1.00F),

        /**
         * Slower at the top, stronger off the line, and a good deal further on
         * a tank. No spark plugs: it lights itself.
         */
        DIESEL("diesel", 0.92F, 1.15F, 0.72F),

        /**
         * Grown rather than pumped: what the refinery makes of organic matter.
         * Any engine runs on it — it is never an engine of its own, only
         * something in the tank — but it has less in it, so a car on it burns
         * a good deal more for the same road. Its numbers are what it does to
         * the engine's own, not a replacement for them.
         */
        BIOFUEL("biofuel", 0.97F, 0.96F, 1.40F),

        /**
         * Charge in a battery pack rather than anything in a tank: an electric
         * motor's whole torque from standstill, a touch more top end, and it is
         * filled at a charging post, not a pump. Only an electric motor runs on
         * it, and an electric motor runs on nothing else.
         */
        ELECTRIC("electric", 1.04F, 1.30F, 1.00F);

        private final String key;
        private final float speed;
        private final float pull;
        private final float thirst;

        Fuel(String key, float speed, float pull, float thirst) {
            this.key = key;
            this.speed = speed;
            this.pull = pull;
            this.thirst = thirst;
        }

        /** The name used in {@code fuel.car.<key>}. */
        public String key() {
            return this.key;
        }

        public float speed() {
            return this.speed;
        }

        public float pull() {
            return this.pull;
        }

        public float thirst() {
            return this.thirst;
        }

        /** Whether an engine can be built for it; biofuel is only ever in a tank. */
        public boolean isEngineFuel() {
            return this != BIOFUEL;
        }

        /** Whether an engine on this fuel runs on what is in the tank. */
        public boolean burns(Fuel tank) {
            if (this == ELECTRIC || tank == ELECTRIC) {
                return tank == this;
            }
            return tank == this || tank == BIOFUEL;
        }

        /** Whether this is charge in a battery rather than a liquid. */
        public boolean isElectric() {
            return this == ELECTRIC;
        }

        /** Whether an engine on this fuel has plugs to misfire. */
        public boolean needsSpark() {
            return this == PETROL;
        }

        public static Fuel byOrdinal(int index) {
            return switch (index) {
                case 1 -> DIESEL;
                case 2 -> BIOFUEL;
                case 3 -> ELECTRIC;
                default -> PETROL;
            };
        }
    }

    /** What the car is standing on, as far as a tyre is concerned. */
    public enum Surface {
        ASPHALT,
        DIRT,
        SNOW;

        /** Reads the ground from the block under the wheels. */
        public static Surface of(BlockState ground) {
            if (ground.is(BlockTags.SNOW) || ground.is(BlockTags.ICE)) {
                return SNOW;
            }
            if (ground.is(BlockTags.DIRT) || ground.is(BlockTags.SAND)
                    || ground.is(net.minecraft.world.level.block.Blocks.GRAVEL)
                    || ground.is(net.minecraft.world.level.block.Blocks.CLAY)
                    || ground.is(net.minecraft.world.level.block.Blocks.SOUL_SAND)
                    || ground.is(net.minecraft.world.level.block.Blocks.SOUL_SOIL)
                    || ground.is(net.minecraft.world.level.block.Blocks.DIRT_PATH)
                    || ground.is(net.minecraft.world.level.block.Blocks.FARMLAND)) {
                return DIRT;
            }
            return ASPHALT;
        }
    }

    /**
     * What the wheels are shod with. No tyre is best everywhere, which is the
     * whole point of there being four: each is the right answer on one kind of
     * ground and a compromise on the rest.
     */
    public enum Tyre {

        /** What a car comes with. Fine on a road, out of its depth off one. */
        ROAD("road", 1.00F, 0.75F, 0.55F, 1.0F, 0.80F),

        /** Knobbly. At home in a field, a little vague on tarmac. */
        DIRT("dirt", 0.88F, 1.00F, 0.75F, 1.1F, 0.85F),

        /** Soft and siped: the only one that bites on snow and ice. */
        SNOW("snow", 0.85F, 0.85F, 1.00F, 1.2F, 0.90F),

        /** Sticky on a hard road, wears fast, and useless on anything loose. */
        SPORT("sport", 1.15F, 0.60F, 0.40F, 1.5F, 0.65F);

        private final String key;
        private final float asphalt;
        private final float dirt;
        private final float snow;
        private final float wear;
        private final float wet;

        Tyre(String key, float asphalt, float dirt, float snow, float wear, float wet) {
            this.key = key;
            this.asphalt = asphalt;
            this.dirt = dirt;
            this.snow = snow;
            this.wear = wear;
            this.wet = wet;
        }

        /** The name used in {@code tyre.car.<key>}. */
        public String key() {
            return this.key;
        }

        /** Grip on this ground: 1 is a road tyre on a road. */
        public float grip(Surface surface) {
            return switch (surface) {
                case ASPHALT -> this.asphalt;
                case DIRT -> this.dirt;
                case SNOW -> this.snow;
            };
        }

        /**
         * Of its grip, what it keeps in the rain: a road tyre's grooves clear
         * water, a slick sport tyre aquaplanes, a snow tyre's sipes hold on.
         * Rain on dirt is mud, and loses another share on top.
         */
        public float grip(Surface surface, boolean wet) {
            if (!wet || surface == Surface.SNOW) {
                return this.grip(surface);
            }
            return this.grip(surface) * this.wet * (surface == Surface.DIRT ? MUD : 1.0F);
        }

        /** What turns wet dirt to mud: what is left of a tyre's wet grip there. */
        private static final float MUD = 0.85F;

        /** What the tyres' own wear is multiplied by. */
        public float wear() {
            return this.wear;
        }

        public static Tyre byOrdinal(int index) {
            return index < 0 || index >= values().length ? ROAD : values()[index];
        }
    }

    // A turbo: more of everything, including heat and what it costs to run.
    public static final float TURBO_SPEED = 1.12F;
    public static final float TURBO_PULL = 1.25F;
    public static final float TURBO_THIRST = 1.20F;
    public static final float TURBO_HEAT = 1.20F;
    public static final float TURBO_ENGINE_WEAR = 1.30F;

    // An open filter breathes better and filters worse.
    public static final float OPEN_FILTER_SPEED = 1.04F;
    public static final float OPEN_FILTER_PULL = 1.06F;
    public static final float OPEN_FILTER_THIRST = 1.05F;
    public static final float OPEN_FILTER_OWN_WEAR = 2.0F;
    public static final float OPEN_FILTER_ENGINE_WEAR = 1.15F;

    /** Every passenger after the driver is weight the engine has to pull. */
    public static final float PULL_PER_PASSENGER = 0.95F;

    private static final int ENGINE_MASK = 0b111;
    private static final int TYRE_SHIFT = 3;
    private static final int TYRE_MASK = 0b11;
    private static final int OPEN_FILTER_BIT = 1 << 5;
    private static final int TURBO_BIT = 1 << 6;
    private static final int TANK_SHIFT = 7;
    /** Two bits: petrol, diesel, biofuel or electric charge. */
    private static final int TANK_MASK = 0b11;
    /** Set when the engine bay holds an electric motor instead of any of the eight engines. */
    private static final int ELECTRIC_BIT = 1 << 9;

    /** The engine item variant that is the electric motor, after the eight combustion engines. */
    public static final int ELECTRIC_MOTOR = 8;

    /** A petrol four on road tyres with a paper filter and petrol in the tank. */
    public static final int DEFAULT = 0;

    private CarSetup() {
    }

    // ---- what a part item carries ----

    /** The variant an engine item carries for this layout and fuel. */
    public static int engineVariant(Layout layout, Fuel fuel) {
        return layout.ordinal() | (fuel.ordinal() << 2);
    }

    public static Layout layoutOf(int engineVariant) {
        return engineVariant == ELECTRIC_MOTOR ? Layout.INLINE4 : Layout.values()[engineVariant & 0b11];
    }

    public static Fuel fuelOf(int engineVariant) {
        return engineVariant == ELECTRIC_MOTOR ? Fuel.ELECTRIC : Fuel.byOrdinal((engineVariant >> 2) & 1);
    }

    /** The key under {@code engine.car.} an engine of this variant is called by. */
    public static String engineKey(int engineVariant) {
        return engineVariant == ELECTRIC_MOTOR ? "electric" : layoutOf(engineVariant).key();
    }

    /** How many variants a part comes in; one for the parts that have no kinds. */
    public static int variantsOf(CarPart part) {
        return switch (part) {
            case ENGINE -> 9;
            case WHEELS -> 4;
            case AIR_FILTER -> 2;
            default -> 1;
        };
    }

    // ---- what a car carries ----

    /** The variant of the part in this slot, read out of a car's setup. */
    public static int variant(int setup, CarPart part) {
        return switch (part) {
            case ENGINE -> (setup & ELECTRIC_BIT) != 0 ? ELECTRIC_MOTOR : setup & ENGINE_MASK;
            case WHEELS -> (setup >> TYRE_SHIFT) & TYRE_MASK;
            case AIR_FILTER -> (setup & OPEN_FILTER_BIT) != 0 ? 1 : 0;
            default -> 0;
        };
    }

    /** The same setup with a different part in one slot. */
    public static int withVariant(int setup, CarPart part, int variant) {
        int safe = Math.max(0, Math.min(variantsOf(part) - 1, variant));
        return switch (part) {
            // The motor and the battery come together: an electric car's "tank"
            // is its charge, and a combustion engine put back gets a petrol one.
            case ENGINE -> safe == ELECTRIC_MOTOR
                    ? withTank((setup & ~ENGINE_MASK) | ELECTRIC_BIT, Fuel.ELECTRIC)
                    : (setup & ELECTRIC_BIT) != 0
                            ? withTank((setup & ~ENGINE_MASK & ~ELECTRIC_BIT) | safe, Fuel.PETROL)
                            : (setup & ~ENGINE_MASK) | safe;
            case WHEELS -> (setup & ~(TYRE_MASK << TYRE_SHIFT)) | (safe << TYRE_SHIFT);
            case AIR_FILTER -> safe == 1 ? setup | OPEN_FILTER_BIT : setup & ~OPEN_FILTER_BIT;
            default -> setup;
        };
    }

    public static Layout layout(int setup) {
        return layoutOf(variant(setup, CarPart.ENGINE));
    }

    public static Fuel engineFuel(int setup) {
        return fuelOf(variant(setup, CarPart.ENGINE));
    }

    /** Whether the engine bay holds the electric motor. */
    public static boolean electric(int setup) {
        return (setup & ELECTRIC_BIT) != 0;
    }

    public static Tyre tyre(int setup) {
        return Tyre.byOrdinal((setup >> TYRE_SHIFT) & TYRE_MASK);
    }

    public static boolean openFilter(int setup) {
        return (setup & OPEN_FILTER_BIT) != 0;
    }

    public static boolean turbo(int setup) {
        return (setup & TURBO_BIT) != 0;
    }

    public static int withTurbo(int setup, boolean fitted) {
        return fitted ? setup | TURBO_BIT : setup & ~TURBO_BIT;
    }

    /** What is in the tank, which need not be what the engine wants. */
    public static Fuel tank(int setup) {
        return Fuel.byOrdinal((setup >> TANK_SHIFT) & TANK_MASK);
    }

    public static int withTank(int setup, Fuel fuel) {
        return (setup & ~(TANK_MASK << TANK_SHIFT)) | (fuel.ordinal() << TANK_SHIFT);
    }
}
