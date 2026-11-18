package me.trossty.car.vehicle;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import me.trossty.car.CarMain;
import me.trossty.car.entity.CarSetup;
import net.minecraft.core.Registry;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.resources.ResourceKey;
import net.minecraft.util.ExtraCodecs;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

/**
 * A vehicle somebody described in a data pack.
 *
 * <p>The mod has three machines written in Java — the car, the pickup and the
 * tanker — and those decide what a vehicle can <em>do</em>: carry people,
 * carry a chest's worth in a bed, carry fuel. A definition picks one of them
 * as its {@link Chassis} and then says everything else: what the thing looks
 * like, how big it is, where people sit, where the wheels are, how it drives
 * and what it leaves the factory with under the bonnet.
 *
 * <p>Definitions live in {@code data/<namespace>/car/vehicle/<name>.json} and
 * are a synced registry, so a server's pack is all a client needs: the shape
 * is drawn from the boxes in the file, with no model or texture to ship.
 *
 * <p>Every length is in sixteenths of a block — pixels, the unit a Minecraft
 * model is drawn in — with x across the car, y up from the ground and z
 * towards the front.
 *
 * <p>{@code tank} is the fuel tank, in litres: eight for a scooter, fifty for
 * a family car, two hundred for a lorry. A trailer has none.
 *
 * <p>{@code lights} is how far the head lamps throw their light down the
 * road, and how wide it has spread at the far end, both in blocks.
 */
public record VehicleDefinition(
        Chassis chassis,
        float width,
        float height,
        Optional<DyeColor> color,
        List<Vec3> seats,
        List<Wheel> wheels,
        Performance performance,
        Factory factory,
        List<Box> shape,
        List<Cost> materials,
        float tank,
        Lights lights,
        float mass,
        Carrying carrying) {

    /**
     * What it weighs, in kilograms, when the definition does not say:
     * {@code mass} is what a towing vehicle feels of it. {@code deck} is how
     * high a flat bed stands, in the model's pixels, on a trailer that carries
     * a vehicle; 0 for anything else. {@code dolly} makes that bed a tow
     * dolly: it takes only a car's front wheels, and the back ones roll on
     * the road behind. {@code takes} is what kinds of vehicle the bed or
     * dolly will carry; left out, any.
     */
    public float massKg() {
        if (this.mass > 0.0F) {
            return this.mass;
        }
        return switch (this.chassis) {
            case PICKUP -> 2000.0F;
            case TANKER -> 7500.0F;
            case TRAILER -> 400.0F;
            case MOTORCYCLE -> 180.0F;
            case BOAT -> 1300.0F;
            case PLANE -> 750.0F;
            default -> 1300.0F;
        };
    }

    /** Whether this is a flat bed another vehicle rides on. */
    public boolean carriesVehicles() {
        return this.deck() > 0.0F;
    }

    public float deck() {
        return this.carrying.deck();
    }

    public boolean dolly() {
        return this.carrying.dolly();
    }

    public List<Chassis> takes() {
        return this.carrying.takes();
    }

    /** Where a bed's or box's loose load is shown, if it has one. */
    public Optional<Area> load() {
        return this.carrying.load();
    }

    /**
     * The closed lid's boxes, all of them in one, in pixels with +z to the
     * front; null when this vehicle has no such lid.
     */
    public @Nullable AABB lidBounds(String lid) {
        AABB bounds = null;
        for (Box box : this.shape) {
            if (box.opens().equals(lid)) {
                AABB one = new AABB(
                        box.from().x(), box.from().y(), box.from().z(),
                        box.to().x(), box.to().y(), box.to().z());
                bounds = bounds == null ? one : bounds.minmax(one);
            }
        }
        return bounds;
    }

    /** Whether there are beds inside: a caravan, slept in at night. */
    public boolean camper() {
        return this.carrying.camper();
    }

    /** The only vehicles that may pull this, by id; empty when anything with a tow ball may. */
    public List<net.minecraft.resources.ResourceLocation> pulledBy() {
        return this.carrying.pulledBy();
    }

    /**
     * What it carries, written flat in the file beside everything else:
     * {@code deck}, {@code dolly} and {@code takes} for a bed that carries a
     * vehicle, and {@code load} — a {@code from}/{@code to} box, in pixels —
     * for the open bed or box whose chest contents are shown sitting in it.
     * {@code pulled_by} keeps a trailer to the trucks built for it: a wheel
     * lift goes only on the back of a tow truck. {@code camper} is a
     * caravan's: there are beds in it.
     */
    public record Carrying(float deck, boolean dolly, List<Chassis> takes, Optional<Area> load,
                           List<net.minecraft.resources.ResourceLocation> pulledBy, boolean camper) {
        public static final com.mojang.serialization.MapCodec<Carrying> MAP_CODEC = RecordCodecBuilder.mapCodec(
                instance -> instance.group(
                        Codec.floatRange(0.0F, 64.0F).optionalFieldOf("deck", 0.0F).forGetter(Carrying::deck),
                        Codec.BOOL.optionalFieldOf("dolly", false).forGetter(Carrying::dolly),
                        Chassis.CODEC.listOf().optionalFieldOf("takes", List.of()).forGetter(Carrying::takes),
                        Area.CODEC.optionalFieldOf("load").forGetter(Carrying::load),
                        net.minecraft.resources.ResourceLocation.CODEC.listOf()
                                .optionalFieldOf("pulled_by", List.of()).forGetter(Carrying::pulledBy),
                        Codec.BOOL.optionalFieldOf("camper", false).forGetter(Carrying::camper)
                ).apply(instance, Carrying::new));
    }

    /** A box in the model's pixels, corner to corner. */
    public record Area(org.joml.Vector3f from, org.joml.Vector3f to) {
        public static final Codec<Area> CODEC = RecordCodecBuilder.create(instance -> instance.group(
                ExtraCodecs.VECTOR3F.fieldOf("from").forGetter(Area::from),
                ExtraCodecs.VECTOR3F.fieldOf("to").forGetter(Area::to)
        ).apply(instance, Area::new));
    }

    /** A tank nobody wrote down: a family car's, fifty litres. */
    public static final float DEFAULT_TANK_LITRES = 50.0F;

    /**
     * Something the vehicle workstation takes to build this vehicle, on top of
     * the parts every vehicle of its chassis is assembled from.
     */
    public record Cost(net.minecraft.world.item.Item item, int count) {
        public static final Codec<Cost> CODEC = RecordCodecBuilder.create(instance -> instance.group(
                net.minecraft.core.registries.BuiltInRegistries.ITEM.byNameCodec().fieldOf("item").forGetter(Cost::item),
                Codec.intRange(1, 64).optionalFieldOf("count", 1).forGetter(Cost::count)
        ).apply(instance, Cost::new));
    }

    public static final ResourceKey<Registry<VehicleDefinition>> KEY =
            ResourceKey.createRegistryKey(CarMain.id("vehicle"));

    /** A sane ceiling on how much a pack can ask a client to draw. */
    public static final int MAX_BOXES = 512;

    /** Which of the mod's machines this vehicle is underneath. */
    public enum Chassis {
        CAR,
        PICKUP,
        TANKER,
        /** Pulled, not driven: no engine, no seats, a hitch at the front. */
        TRAILER,
        /** Two wheels, a rider and a pillion. */
        MOTORCYCLE,
        /** Floats: driven on water by a propeller, stranded on land. */
        BOAT,
        /** Taxies on its wheels, lifts off with enough speed and flies. */
        PLANE;

        public static final Codec<Chassis> CODEC = Codec.STRING.xmap(
                name -> switch (name.toLowerCase(Locale.ROOT)) {
                    case "pickup" -> PICKUP;
                    case "tanker" -> TANKER;
                    case "trailer" -> TRAILER;
                    case "motorcycle" -> MOTORCYCLE;
                    case "boat" -> BOAT;
                    case "plane" -> PLANE;
                    default -> CAR;
                },
                chassis -> chassis.name().toLowerCase(Locale.ROOT));
    }

    /** One wheel: where its centre is, how big it is, and whether it steers. */
    public record Wheel(org.joml.Vector3f at, float radius, float width, boolean steers) {
        public static final Codec<Wheel> CODEC = RecordCodecBuilder.create(instance -> instance.group(
                ExtraCodecs.VECTOR3F.fieldOf("at").forGetter(Wheel::at),
                Codec.floatRange(0.5F, 16.0F).optionalFieldOf("radius", 3.0F).forGetter(Wheel::radius),
                Codec.floatRange(0.5F, 16.0F).optionalFieldOf("width", 3.0F).forGetter(Wheel::width),
                Codec.BOOL.optionalFieldOf("steers", false).forGetter(Wheel::steers)
        ).apply(instance, Wheel::new));
    }

    /** The head lamps' pool of light on the road: its length and how far it fans out past the sides. */
    public record Lights(float length, float spread) {

        public static final Lights PLAIN = new Lights(11.0F, 3.0F);

        public static final Codec<Lights> CODEC = RecordCodecBuilder.create(instance -> instance.group(
                Codec.floatRange(2.0F, 32.0F).optionalFieldOf("length", PLAIN.length()).forGetter(Lights::length),
                Codec.floatRange(0.0F, 8.0F).optionalFieldOf("spread", PLAIN.spread()).forGetter(Lights::spread)
        ).apply(instance, Lights::new));
    }

    /**
     * How it drives, each as a multiple of the plain car's figure, so 1 is
     * "the same" everywhere. {@code offroad} is different: it is how much of
     * the grip a tyre loses off the road this vehicle gets back, from none (0)
     * to all of it (1).
     */
    public record Performance(float speed, float acceleration, float fuelUse,
                              float braking, float steering, float offroad, List<Float> gears) {

        /**
         * The gearbox a definition gets when it does not give one: five
         * forward, short low gears and long high ones. Each number is how much
         * of the top speed that gear reaches at the redline, so the last is
         * always 1. A single 1 is one fixed ratio, a scooter's belt drive.
         */
        public static final List<Float> FIVE_SPEED = List.of(0.24F, 0.42F, 0.61F, 0.81F, 1.0F);

        public static final Performance PLAIN = new Performance(1.0F, 1.0F, 1.0F, 1.0F, 1.0F, 0.0F, FIVE_SPEED);

        private static final Codec<Float> FACTOR = Codec.floatRange(0.25F, 3.0F);

        private static final Codec<List<Float>> GEARS = Codec.floatRange(0.05F, 1.0F).listOf(1, 12)
                .validate(gears -> {
                    for (int i = 1; i < gears.size(); i++) {
                        if (gears.get(i) <= gears.get(i - 1)) {
                            return com.mojang.serialization.DataResult.error(
                                    () -> "gears must climb from first to top: " + gears);
                        }
                    }
                    return gears.get(gears.size() - 1) == 1.0F ? com.mojang.serialization.DataResult.success(gears)
                            : com.mojang.serialization.DataResult.error(() -> "top gear must reach 1: " + gears);
                });

        public static final Codec<Performance> CODEC = RecordCodecBuilder.create(instance -> instance.group(
                FACTOR.optionalFieldOf("speed", 1.0F).forGetter(Performance::speed),
                FACTOR.optionalFieldOf("acceleration", 1.0F).forGetter(Performance::acceleration),
                FACTOR.optionalFieldOf("fuel_use", 1.0F).forGetter(Performance::fuelUse),
                FACTOR.optionalFieldOf("braking", 1.0F).forGetter(Performance::braking),
                FACTOR.optionalFieldOf("steering", 1.0F).forGetter(Performance::steering),
                Codec.floatRange(0.0F, 1.0F).optionalFieldOf("offroad", 0.0F).forGetter(Performance::offroad),
                GEARS.optionalFieldOf("gears", FIVE_SPEED).forGetter(Performance::gears)
        ).apply(instance, Performance::new));
    }

    /** What it is fitted with when it is new: the engine, the fuel, the tyres. */
    public record Factory(String engine, String fuel, String tyres, boolean turbo) {

        public static final Factory PLAIN = new Factory("inline4", "petrol", "road", false);

        public static final Codec<Factory> CODEC = RecordCodecBuilder.create(instance -> instance.group(
                Codec.STRING.optionalFieldOf("engine", "inline4").forGetter(Factory::engine),
                Codec.STRING.optionalFieldOf("fuel", "petrol").forGetter(Factory::fuel),
                Codec.STRING.optionalFieldOf("tyres", "road").forGetter(Factory::tyres),
                Codec.BOOL.optionalFieldOf("turbo", false).forGetter(Factory::turbo)
        ).apply(instance, Factory::new));

        /** The same thing as the one number a car keeps its fittings in. */
        public int setup() {
            CarSetup.Layout layout = CarSetup.Layout.INLINE4;
            for (CarSetup.Layout candidate : CarSetup.Layout.values()) {
                if (candidate.key().equalsIgnoreCase(this.engine)) {
                    layout = candidate;
                }
            }
            CarSetup.Fuel burns = "diesel".equalsIgnoreCase(this.fuel)
                    ? CarSetup.Fuel.DIESEL : CarSetup.Fuel.PETROL;
            CarSetup.Tyre tyre = CarSetup.Tyre.ROAD;
            for (CarSetup.Tyre candidate : CarSetup.Tyre.values()) {
                if (candidate.key().equalsIgnoreCase(this.tyres)) {
                    tyre = candidate;
                }
            }
            int setup = CarSetup.DEFAULT;
            boolean electric = "electric".equalsIgnoreCase(this.fuel);
            if (electric) {
                burns = CarSetup.Fuel.ELECTRIC;
            }
            setup = CarSetup.withVariant(setup, me.trossty.car.entity.CarPart.ENGINE,
                    electric ? CarSetup.ELECTRIC_MOTOR : CarSetup.engineVariant(layout, burns));
            setup = CarSetup.withVariant(setup, me.trossty.car.entity.CarPart.WHEELS, tyre.ordinal());
            setup = CarSetup.withTurbo(setup, this.turbo);
            return CarSetup.withTank(setup, burns);
        }
    }

    /**
     * One box of bodywork. {@code color} is {@code paint} (whatever the car is
     * dyed), {@code paint_dark}, {@code paint_light}, a fixed
     * {@code #RRGGBB}, or a see-through {@code #AARRGGBB} for glass. {@code tilt} leans the box about its own middle, in
     * degrees: positive tips its top towards the back of the car, which is
     * what a windscreen does, and negative towards the front. {@code steers}
     * turns the box with the front wheel — a fork, handlebars, a headset.
     * {@code roll} leans it about its own middle the other way, about the
     * line from back to front, in degrees: positive lifts its left-hand end
     * (+x) — a wing strut, a landing-gear leg. {@code spins} turns it with
     * the engine about the middle of the first box that spins: a propeller.
     * {@code yaw} turns it about its own upright, in degrees: positive swings
     * its front towards the left (+x) — a panel along a tapering tail. A box
     * is tilted first, then turned, then rolled.
     */
    public record Box(org.joml.Vector3f from, org.joml.Vector3f to, String color, float tilt, boolean steers,
                      float roll, boolean spins, float yaw, String opens) {
        /** The lids a box can belong to: it swings up with that lid. */
        public static final String BONNET = "bonnet";
        public static final String BOOT = "boot";

        public static final Codec<Box> CODEC = RecordCodecBuilder.create(instance -> instance.group(
                ExtraCodecs.VECTOR3F.fieldOf("from").forGetter(Box::from),
                ExtraCodecs.VECTOR3F.fieldOf("to").forGetter(Box::to),
                Codec.STRING.optionalFieldOf("color", "paint").forGetter(Box::color),
                Codec.floatRange(-90.0F, 90.0F).optionalFieldOf("tilt", 0.0F).forGetter(Box::tilt),
                Codec.BOOL.optionalFieldOf("steers", false).forGetter(Box::steers),
                Codec.floatRange(-90.0F, 90.0F).optionalFieldOf("roll", 0.0F).forGetter(Box::roll),
                Codec.BOOL.optionalFieldOf("spins", false).forGetter(Box::spins),
                Codec.floatRange(-90.0F, 90.0F).optionalFieldOf("yaw", 0.0F).forGetter(Box::yaw),
                Codec.STRING.validate(lid -> lid.isEmpty() || lid.equals(BONNET) || lid.equals(BOOT)
                        ? com.mojang.serialization.DataResult.success(lid)
                        : com.mojang.serialization.DataResult.error(() -> "opens must be bonnet or boot: " + lid))
                        .optionalFieldOf("opens", "").forGetter(Box::opens)
        ).apply(instance, Box::new));
    }

    public static final Codec<VehicleDefinition> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Chassis.CODEC.optionalFieldOf("chassis", Chassis.CAR).forGetter(VehicleDefinition::chassis),
            Codec.floatRange(0.5F, 4.0F).optionalFieldOf("width", 1.6F).forGetter(VehicleDefinition::width),
            Codec.floatRange(0.5F, 4.0F).optionalFieldOf("height", 2.1875F).forGetter(VehicleDefinition::height),
            DyeColor.CODEC.optionalFieldOf("color").forGetter(VehicleDefinition::color),
            Vec3.CODEC.listOf(0, 16).optionalFieldOf("seats", List.of()).forGetter(VehicleDefinition::seats),
            Wheel.CODEC.listOf(0, 12).optionalFieldOf("wheels", List.of()).forGetter(VehicleDefinition::wheels),
            Performance.CODEC.optionalFieldOf("performance", Performance.PLAIN)
                    .forGetter(VehicleDefinition::performance),
            Factory.CODEC.optionalFieldOf("factory", Factory.PLAIN).forGetter(VehicleDefinition::factory),
            Box.CODEC.listOf(1, MAX_BOXES).fieldOf("shape").forGetter(VehicleDefinition::shape),
            Cost.CODEC.listOf(0, 16).optionalFieldOf("materials", List.of()).forGetter(VehicleDefinition::materials),
            Codec.floatRange(0.0F, 1000.0F).optionalFieldOf("tank", DEFAULT_TANK_LITRES)
                    .forGetter(VehicleDefinition::tank),
            Lights.CODEC.optionalFieldOf("lights", Lights.PLAIN).forGetter(VehicleDefinition::lights),
            Codec.floatRange(0.0F, 100000.0F).optionalFieldOf("mass", 0.0F).forGetter(VehicleDefinition::mass),
            Carrying.MAP_CODEC.forGetter(VehicleDefinition::carrying)
    ).apply(instance, VehicleDefinition::new));

    /**
     * What each machine is when nothing says otherwise. There is no "plain"
     * car any more: a car is a Corolla, a pickup a Hilux, a tanker an Atego.
     */
    public static ResourceLocation defaultFor(Chassis chassis) {
        return CarMain.id(switch (chassis) {
            case PICKUP -> "toyota_hilux";
            case TANKER -> "mercedes_atego";
            case TRAILER -> "box_trailer";
            case MOTORCYCLE -> "honda_cbr600rr";
            case CAR -> "toyota_corolla";
            case BOAT -> "bayliner_bowrider";
            case PLANE -> "cessna_172";
        });
    }

    /** The key a vehicle's name is looked up under, and what to call it otherwise. */
    public static net.minecraft.network.chat.Component nameOf(ResourceLocation id) {
        StringBuilder plain = new StringBuilder();
        for (String word : id.getPath().split("[_/]")) {
            if (word.isEmpty()) {
                continue;
            }
            if (plain.length() > 0) {
                plain.append(' ');
            }
            plain.append(Character.toUpperCase(word.charAt(0))).append(word.substring(1));
        }
        return net.minecraft.network.chat.Component.translatableWithFallback(
                "vehicle." + id.getNamespace() + "." + id.getPath().replace('/', '.'), plain.toString());
    }

    /** Resolves a box's colour against the paint the car is wearing, as ARGB. */
    public static int resolve(String color, int paint) {
        return switch (color) {
            case "paint" -> 0xFF000000 | paint;
            case "paint_dark" -> shade(paint, 0.72F);
            case "paint_light" -> shade(paint, 1.18F);
            default -> {
                if (color.startsWith("#") && color.length() == 7) {
                    try {
                        yield 0xFF000000 | Integer.parseInt(color.substring(1), 16);
                    } catch (NumberFormatException ignored) {
                        // falls through to the loud colour below
                    }
                }
                // #AARRGGBB: see-through, for glass and the glint on it.
                if (color.startsWith("#") && color.length() == 9) {
                    try {
                        yield (int) Long.parseLong(color.substring(1), 16);
                    } catch (NumberFormatException ignored) {
                        // falls through to the loud colour below
                    }
                }
                // Magenta: a colour nobody chose, so a typo is seen at once.
                yield 0xFFFF00FF;
            }
        };
    }

    private static int shade(int rgb, float factor) {
        int red = Math.min(255, Math.round(((rgb >> 16) & 0xFF) * factor));
        int green = Math.min(255, Math.round(((rgb >> 8) & 0xFF) * factor));
        int blue = Math.min(255, Math.round((rgb & 0xFF) * factor));
        return 0xFF000000 | red << 16 | green << 8 | blue;
    }
}
