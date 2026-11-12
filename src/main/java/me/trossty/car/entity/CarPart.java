package me.trossty.car.entity;

import me.trossty.car.registry.ModItems;
import net.minecraft.item.Item;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;

import java.util.ArrayList;
import java.util.List;

/**
 * Everything on a car that wears out, can be taken off and can be put back on.
 *
 * <p>Repair with an iron ingot beats the dents out of the bodyshell. It does
 * nothing for what is underneath, which is the gap this fills: a car that has
 * never been crashed still gets tired, and what it needs then is not a hammer
 * but a diagnosis.
 *
 * <p>Three things make this more than a list of wear bars.
 *
 * <p><b>Nothing is reached for free.</b> Every part names the one part that sits
 * on top of it ({@link #parent()}). The brake pads are behind the wheel, the
 * discs are behind the pads, the clutch is behind the gearbox. A part can only
 * be touched once everything above it has been taken off, so working on a car
 * is a sequence rather than a menu.
 *
 * <p><b>A part can be absent.</b> Condition 0 means the slot is empty, not that
 * the part is merely tired — wear bottoms out at 1. That is what makes a
 * teardown possible at all, and it is why {@link #essential()} exists: a car
 * missing its driveshaft does not limp, it sits there.
 *
 * <p><b>Each one does its own job.</b> {@link CarEntity} holds the arithmetic;
 * the short version is in {@code part.car.<key>.effect} so the car can explain
 * itself in the diagnosis readout.
 */
public enum CarPart {

    // ---- engine bay ----

    /** The airbox, on top of everything. Nothing is in the way of this one. */
    AIR_FILTER("air_filter", Group.ENGINE_BAY, 1, 0.0018F, null, false),

    /** Under the airbox. Four of them, and a tired set misfires. */
    SPARK_PLUGS("spark_plugs", Group.ENGINE_BAY, 4, 0.0026F, "air_filter", true),

    /** Under the plugs, and the single heaviest thing on the car. */
    ENGINE("engine", Group.ENGINE_BAY, 1, 0.0030F, "spark_plugs", true),

    /** Off to the side, reachable any time. Clean oil is what saves the rest. */
    OIL_FILTER("oil_filter", Group.ENGINE_BAY, 1, 0.0016F, null, false),

    /** At the front, reachable any time. Without it the engine cooks itself. */
    RADIATOR("radiator", Group.ENGINE_BAY, 1, 0.0014F, null, false),

    // ---- electrics ----

    /** On top of the alternator, and the reason a car starts at all. */
    BATTERY("battery", Group.ELECTRICS, 1, 0.0F, null, true),

    /** Behind the battery. Its job is to keep the battery from going flat. */
    ALTERNATOR("alternator", Group.ELECTRICS, 1, 0.0020F, "battery", false),

    // ---- drivetrain ----

    /** Underneath, reachable the moment the car is in the air. */
    DRIVESHAFT("driveshaft", Group.DRIVETRAIN, 1, 0.0022F, null, true),

    /** Behind the driveshaft. Acceleration and reverse. */
    TRANSMISSION("transmission", Group.DRIVETRAIN, 1, 0.0025F, "driveshaft", true),

    /** Behind the gearbox: the deepest part on the car, and the worst job. */
    CLUTCH("clutch", Group.DRIVETRAIN, 1, 0.0028F, "transmission", true),

    // ---- running gear ----

    /** Four of them, and in the way of everything else at that corner. */
    WHEELS("wheels", Group.RUNNING_GEAR, 4, 0.0045F, null, true),

    /** Behind the wheels. What actually stops the car. */
    BRAKE_PADS("brake_pads", Group.RUNNING_GEAR, 4, 0.0038F, "wheels", false),

    /** Behind the pads. Worn pads eat these, which is the whole lesson. */
    BRAKE_DISCS("brake_discs", Group.RUNNING_GEAR, 4, 0.0012F, "brake_pads", false),

    /**
     * Also behind the wheels. This one does not wear with use at all: it is
     * beaten up by the impacts it absorbs, which is exactly what springs are
     * for.
     */
    SUSPENSION("suspension", Group.RUNNING_GEAR, 4, 0.0F, "wheels", false),

    /** Underneath, at the front. Without it the wheels point where they like. */
    STEERING_RACK("steering_rack", Group.RUNNING_GEAR, 1, 0.0019F, null, true),

    // ---- body ----

    /** The one part you can hear from outside. */
    EXHAUST("exhaust", Group.BODY, 1, 0.0021F, null, false);

    /** Condition runs 0 to this; a fresh part is at the top of it. */
    public static final float MAX_CONDITION = 100.0F;

    /**
     * Wear stops here rather than at zero. Zero is reserved for "this part is
     * not on the car", and a part that wore itself into non-existence would be
     * indistinguishable from one somebody unbolted.
     */
    public static final float WORN_OUT = 1.0F;

    /**
     * What a part is made of, the way a pickaxe is: iron, gold, diamond or
     * netherite. The same gearbox can be any of them, and the difference is
     * how well it does its job and how long it keeps doing it.
     *
     * <p>{@link #IRON} is what the bench has always made and what a car rolls
     * off the line with; a part with no tier on it at all is iron. The others
     * cost the plain recipe plus their own material, and behave like the tools
     * of the same name: gold is quick and soft, diamond is good and lasts,
     * netherite is the best of everything.
     */
    public enum Quality {

        /** The plain part. The yardstick everything else is measured against. */
        IRON("iron", 1.00F, 1.00F, net.minecraft.util.Formatting.GRAY, 0xFFD8D8D8),

        /** Like a golden pickaxe: lively while it lasts, and it does not last. */
        GOLD("gold", 1.80F, 1.08F, net.minecraft.util.Formatting.GOLD, 0xFFFFC832),

        /** Better at the job and roughly twice the life. */
        DIAMOND("diamond", 0.55F, 1.05F, net.minecraft.util.Formatting.AQUA, 0xFF4FE3D6),

        /** The best of both, at the price of a netherite ingot. */
        NETHERITE("netherite", 0.40F, 1.10F, net.minecraft.util.Formatting.DARK_PURPLE, 0xFF8A6F7A);

        /** What a part carries when nothing says otherwise. */
        public static final Quality DEFAULT = IRON;

        private final String key;
        private final float wearFactor;
        private final float performance;
        private final net.minecraft.util.Formatting formatting;
        private final int argb;

        Quality(String key, float wearFactor, float performance,
                net.minecraft.util.Formatting formatting, int argb) {
            this.key = key;
            this.wearFactor = wearFactor;
            this.performance = performance;
            this.formatting = formatting;
            this.argb = argb;
        }

        /** The name used in {@code quality.car.<key>}. */
        public String key() {
            return this.key;
        }

        /** What wear is multiplied by. Above one wears faster, below one slower. */
        public float wearFactor() {
            return this.wearFactor;
        }

        /**
         * What the part's own job is multiplied by: a diamond gearbox puts
         * more of the engine on the road, gold pads bite harder.
         */
        public float performance() {
            return this.performance;
        }

        /** The colour its name is written in, as an item's rarity is. */
        public net.minecraft.util.Formatting formatting() {
            return this.formatting;
        }

        /** The same colour for a screen. */
        public int argb() {
            return this.argb;
        }

        /** What the bench shows on the tier's button, and charges on top of the plain recipe. */
        public net.minecraft.item.Item material() {
            return switch (this) {
                case IRON -> net.minecraft.item.Items.IRON_INGOT;
                case GOLD -> net.minecraft.item.Items.GOLD_INGOT;
                case DIAMOND -> net.minecraft.item.Items.DIAMOND;
                case NETHERITE -> net.minecraft.item.Items.NETHERITE_INGOT;
            };
        }

        /**
         * What this tier adds to a recipe that takes {@code plain} items in
         * all: nothing for iron, a gold ingot for every four, a diamond for
         * every six, and for netherite the diamonds and one ingot on top.
         */
        public java.util.List<net.minecraft.item.ItemStack> extra(int plain) {
            int quarter = Math.max(1, (plain + 3) / 4);
            int sixth = Math.max(1, (plain + 5) / 6);
            return switch (this) {
                case IRON -> java.util.List.of();
                case GOLD -> java.util.List.of(new net.minecraft.item.ItemStack(
                        net.minecraft.item.Items.GOLD_INGOT, quarter));
                case DIAMOND -> java.util.List.of(new net.minecraft.item.ItemStack(
                        net.minecraft.item.Items.DIAMOND, sixth));
                case NETHERITE -> java.util.List.of(
                        new net.minecraft.item.ItemStack(net.minecraft.item.Items.DIAMOND, sixth),
                        new net.minecraft.item.ItemStack(net.minecraft.item.Items.NETHERITE_INGOT, 1));
            };
        }

        /** Whether this is better than that, for "is it worth swapping". */
        public boolean betterThan(Quality other) {
            return this.rank() > other.rank();
        }

        /** Gold is quick but short-lived, so for "which is better" it sits under iron. */
        public int rank() {
            return switch (this) {
                case GOLD -> 0;
                case IRON -> 1;
                case DIAMOND -> 2;
                case NETHERITE -> 3;
            };
        }

        /** The next one along, or iron again once past the top. */
        public Quality next() {
            return values()[(this.ordinal() + 1) % values().length];
        }

        /** Whatever arrived from the network or an old save, made safe. */
        public static Quality byOrdinal(int index) {
            return index < 0 || index >= values().length ? DEFAULT : values()[index];
        }

        /**
         * The tiers used to be economy, standard and performance, saved as 0,
         * 1 and 2. Economy and standard parts both become iron and
         * performance becomes diamond.
         */
        public static Quality fromOldGrade(int index) {
            return index == 2 ? DIAMOND : IRON;
        }
    }

    /**
     * What a refurbished part comes back at. Deliberately short of new: a
     * rebuild is the cheap way out of a worn part, not a way to never buy one
     * again, and a car that has been kept going on rebuilds is a car at sixty
     * percent.
     */
    public static final float REFURBISHED = 60.0F;

    /** The systems a diagnosis groups parts under, in the order it prints them. */
    public enum Group {
        ENGINE_BAY("engine_bay"),
        ELECTRICS("electrics"),
        DRIVETRAIN("drivetrain"),
        RUNNING_GEAR("running_gear"),
        BODY("body");

        private final String key;

        Group(String key) {
            this.key = key;
        }

        /** The name used in {@code group.car.<key>}. */
        public String key() {
            return this.key;
        }
    }

    private final String key;
    private final Group group;
    private final int count;
    private final float wearPerTick;
    private final String parentKey;
    private final boolean essential;

    CarPart(String key, Group group, int count, float wearPerTick, String parentKey, boolean essential) {
        this.key = key;
        this.group = group;
        this.count = count;
        this.wearPerTick = wearPerTick;
        // Enum constants cannot name each other in their own arguments, so the
        // part above is stored as its key and looked up on demand.
        this.parentKey = parentKey;
        this.essential = essential;
    }

    /** The name used in save data and in {@code part.car.*} lines. */
    public String key() {
        return this.key;
    }

    /** Which system a diagnosis files this part under. */
    public Group group() {
        return this.group;
    }

    /** How many of the item a full set is. Four at the corners, one elsewhere. */
    public int count() {
        return this.count;
    }

    /** Wear per tick at full throttle, before each part's own rule scales it. */
    public float wearPerTick() {
        return this.wearPerTick;
    }

    /**
     * Whether a car can move at all without this one. The rest only make it
     * worse at moving.
     */
    public boolean essential() {
        return this.essential;
    }

    /** The part sitting on top of this one, or null if nothing is in the way. */
    public CarPart parent() {
        return this.parentKey == null ? null : byKey(this.parentKey);
    }

    /** The parts this one is in the way of. */
    public List<CarPart> children() {
        List<CarPart> under = new ArrayList<>(2);
        for (CarPart part : values()) {
            if (part.parent() == this) {
                under.add(part);
            }
        }
        return under;
    }

    /**
     * Everything that has to come off before this part can be touched, outermost
     * first. Empty for the parts you can simply reach.
     */
    public List<CarPart> blockedBy() {
        List<CarPart> chain = new ArrayList<>(3);
        for (CarPart above = this.parent(); above != null; above = above.parent()) {
            chain.add(0, above);
        }
        return chain;
    }

    /** How deep in the car this part sits. Zero is "straight at it". */
    public int depth() {
        return this.blockedBy().size();
    }

    /**
     * Where this part actually sits on the car, in blocks, relative to the
     * car's own position and before its yaw is applied: x to the right, y up,
     * z backwards — which is the model's own sense, where the bonnet is at
     * negative z.
     *
     * <p>This is what makes the workshop a place rather than a menu. The
     * numbers are read off {@code CarEntityModel}: the floor pan runs y 0.375
     * to 0.75 and z -1.25 to 1.25, the wheels are at x ±0.6875 and z ∓0.875,
     * and the cabin starts at z -0.75. So everything in front of the cabin is
     * engine bay, everything under the pan is drivetrain, and the corners are
     * running gear.
     *
     * @param size half-width of the box you aim at, in blocks
     */
    public record Anchor(float x, float y, float z, float size) {

        /** The middle of the box, which is where a label is hung. */
        public float centreY() {
            return this.y;
        }

        /**
         * Where this part is in the world, for a car standing at {@code pos}
         * and turned to {@code yaw}.
         *
         * <p>This lives here, in code both sides can see, rather than in the
         * renderer that draws the boxes: a test can then check that two parts
         * you can get at in the same breath are not sitting in the same place,
         * which is the one thing that would make pointing at them a lottery.
         *
         * <p>The model is built facing negative z and the renderer turns it by
         * {@code 180 - yaw}, so after that flip the car's own forward is
         * positive z and its right negative x, which is why both are negated
         * before the yaw is applied.
         */
        public Box box(Vec3d pos, float yaw) {
            Vec3d offset = new Vec3d(-this.x, this.y, -this.z)
                    .rotateY(-yaw * MathHelper.RADIANS_PER_DEGREE);
            double side = this.size * 2.0;
            return Box.of(pos.add(offset), side, side, side);
        }
    }

    /**
     * The spot on the car you point at to work on this part. A switch rather
     * than four more arguments on every constant, so the list above stays
     * readable.
     */
    public Anchor anchor() {
        return switch (this) {
            // Engine bay, down the middle: airbox against the bulkhead, plugs
            // along the top of the head in front of it, the block below both.
            case AIR_FILTER -> new Anchor(0.00F, 1.00F, -0.76F, 0.12F);
            case SPARK_PLUGS -> new Anchor(0.00F, 0.94F, -1.02F, 0.11F);
            case ENGINE -> new Anchor(0.00F, 0.62F, -0.84F, 0.17F);

            // Behind the grille, in front of the block.
            case RADIATOR -> new Anchor(0.00F, 0.66F, -1.20F, 0.12F);

            // The near wing of the bay, stacked: battery on top where you can
            // get the clamps off, alternator below it, oil filter at the
            // bottom where the tray goes.
            case BATTERY -> new Anchor(-0.40F, 1.00F, -0.84F, 0.12F);
            case ALTERNATOR -> new Anchor(-0.40F, 0.72F, -0.84F, 0.11F);
            case OIL_FILTER -> new Anchor(-0.40F, 0.44F, -0.84F, 0.10F);

            // Under the floor, front to back: clutch at the bellhousing,
            // gearbox behind it, propshaft running to the back axle.
            case CLUTCH -> new Anchor(0.00F, 0.30F, -0.45F, 0.15F);
            case TRANSMISSION -> new Anchor(0.00F, 0.30F, -0.05F, 0.18F);
            case DRIVESHAFT -> new Anchor(0.00F, 0.30F, 0.40F, 0.15F);

            // The front corner. The wheel is outermost; inside it the pads at
            // caliper height and the disc below them; the strut above the lot.
            case WHEELS -> new Anchor(0.72F, 0.28F, -0.88F, 0.20F);
            case SUSPENSION -> new Anchor(0.52F, 0.68F, -0.88F, 0.12F);
            case BRAKE_PADS -> new Anchor(0.34F, 0.42F, -0.88F, 0.11F);
            case BRAKE_DISCS -> new Anchor(0.34F, 0.16F, -0.88F, 0.11F);

            // Across the front, under the engine.
            case STEERING_RACK -> new Anchor(0.00F, 0.26F, -0.95F, 0.14F);

            // Out of the back, down the near side.
            case EXHAUST -> new Anchor(-0.30F, 0.26F, 1.05F, 0.13F);
        };
    }

    /**
     * The spare. Looked up rather than stored, because the items register after
     * this enum's constants would be built.
     */
    public Item item() {
        return switch (this) {
            case AIR_FILTER -> ModItems.AIR_FILTER;
            case SPARK_PLUGS -> ModItems.SPARK_PLUGS;
            case ENGINE -> ModItems.ENGINE;
            case OIL_FILTER -> ModItems.OIL_FILTER;
            case RADIATOR -> ModItems.RADIATOR;
            case BATTERY -> ModItems.BATTERY;
            case ALTERNATOR -> ModItems.ALTERNATOR;
            case DRIVESHAFT -> ModItems.DRIVESHAFT;
            case TRANSMISSION -> ModItems.TRANSMISSION;
            case CLUTCH -> ModItems.CLUTCH;
            case WHEELS -> ModItems.WHEEL;
            case BRAKE_PADS -> ModItems.BRAKE_PADS;
            case BRAKE_DISCS -> ModItems.BRAKE_DISCS;
            case SUSPENSION -> ModItems.SUSPENSION;
            case STEERING_RACK -> ModItems.STEERING_RACK;
            case EXHAUST -> ModItems.EXHAUST;
        };
    }

    /** The part this item is a spare for, or null if it is not a part at all. */
    public static CarPart forItem(Item item) {
        for (CarPart part : values()) {
            if (part.item() == item) {
                return part;
            }
        }
        return null;
    }

    /** The part with this save key, or null. */
    public static CarPart byKey(String key) {
        for (CarPart part : values()) {
            if (part.key.equals(key)) {
                return part;
            }
        }
        return null;
    }

    /** The parts in one system, in declaration order. */
    public static List<CarPart> of(Group group) {
        List<CarPart> parts = new ArrayList<>();
        for (CarPart part : values()) {
            if (part.group == group) {
                parts.add(part);
            }
        }
        return parts;
    }
}
