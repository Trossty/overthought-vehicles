package me.trossty.car.workbench;

import me.trossty.car.entity.CarPart;
import me.trossty.car.entity.CarSetup;
import me.trossty.car.registry.ModItems;
import net.minecraft.world.item.Items;
import java.util.List;

/**
 * Everything the two benches can build, in the order their windows list it.
 *
 * <p>The split is the point. The workbench makes <em>parts</em> — a body, an
 * engine, a gearbox, suspension, wheels — and none of them is a vehicle. Only
 * the lift puts them together, so a car is never something you hold in your hand
 * until it has been on a ramp, the same way a real one is not.
 *
 * <p>This is also how everything here becomes obtainable in survival: none of
 * these items has a normal crafting recipe.
 */
public final class ModVehicleRecipes {

    /** The parts bench: raw materials in, components out. */
    public static final List<VehicleRecipe> WORKBENCH = withRefurbishes(workbench(List.of(
            new VehicleRecipe(ModItems.CAR_BODY, List.of(
                    new VehicleRecipe.Material(Items.IRON_INGOT, 6),
                    new VehicleRecipe.Material(Items.GLASS, 2))),
            new VehicleRecipe(ModItems.ENGINE, List.of(
                    new VehicleRecipe.Material(Items.IRON_INGOT, 4),
                    new VehicleRecipe.Material(Items.COPPER_INGOT, 2),
                    new VehicleRecipe.Material(Items.REDSTONE_BLOCK, 1))),
            new VehicleRecipe(ModItems.TRANSMISSION, List.of(
                    new VehicleRecipe.Material(Items.IRON_INGOT, 3),
                    new VehicleRecipe.Material(Items.COPPER_INGOT, 2),
                    new VehicleRecipe.Material(Items.REDSTONE, 2))),
            new VehicleRecipe(ModItems.SUSPENSION, List.of(
                    new VehicleRecipe.Material(Items.IRON_INGOT, 3),
                    new VehicleRecipe.Material(Items.COPPER_INGOT, 1),
                    new VehicleRecipe.Material(Items.SLIME_BALL, 2))),
            new VehicleRecipe(ModItems.WHEEL, List.of(
                    new VehicleRecipe.Material(Items.BLACK_WOOL, 3),
                    new VehicleRecipe.Material(Items.IRON_INGOT, 1))),
            // ---- the service parts ----
            //
            // None of these is in the lift's recipe: a car is built with them
            // already in it. They are here because a diagnosis sends you to the
            // bench for one specific thing, which is the whole workshop loop —
            // find out what is tired, build that, and nothing else.
            new VehicleRecipe(ModItems.AIR_FILTER, List.of(
                    new VehicleRecipe.Material(Items.PAPER, 4),
                    new VehicleRecipe.Material(Items.IRON_INGOT, 1))),
            new VehicleRecipe(ModItems.SPARK_PLUGS, List.of(
                    new VehicleRecipe.Material(Items.QUARTZ, 2),
                    new VehicleRecipe.Material(Items.COPPER_INGOT, 1))),
            new VehicleRecipe(ModItems.OIL_FILTER, List.of(
                    new VehicleRecipe.Material(Items.IRON_INGOT, 2),
                    new VehicleRecipe.Material(Items.PAPER, 2))),
            new VehicleRecipe(ModItems.RADIATOR, List.of(
                    new VehicleRecipe.Material(Items.COPPER_INGOT, 5),
                    new VehicleRecipe.Material(Items.IRON_INGOT, 1))),
            new VehicleRecipe(ModItems.BATTERY, List.of(
                    new VehicleRecipe.Material(Items.COPPER_INGOT, 2),
                    new VehicleRecipe.Material(Items.REDSTONE, 6),
                    new VehicleRecipe.Material(Items.IRON_INGOT, 1))),
            new VehicleRecipe(ModItems.ALTERNATOR, List.of(
                    new VehicleRecipe.Material(Items.COPPER_INGOT, 4),
                    new VehicleRecipe.Material(Items.IRON_INGOT, 2),
                    new VehicleRecipe.Material(Items.REDSTONE, 1))),
            new VehicleRecipe(ModItems.DRIVESHAFT, List.of(
                    new VehicleRecipe.Material(Items.IRON_INGOT, 5))),
            new VehicleRecipe(ModItems.CLUTCH, List.of(
                    new VehicleRecipe.Material(Items.IRON_INGOT, 3),
                    new VehicleRecipe.Material(Items.LEATHER, 2),
                    new VehicleRecipe.Material(Items.COPPER_INGOT, 1))),
            new VehicleRecipe(ModItems.BRAKE_PADS, List.of(
                    new VehicleRecipe.Material(Items.IRON_INGOT, 1),
                    new VehicleRecipe.Material(Items.COAL, 3))),
            new VehicleRecipe(ModItems.BRAKE_DISCS, List.of(
                    new VehicleRecipe.Material(Items.IRON_INGOT, 4))),
            new VehicleRecipe(ModItems.STEERING_RACK, List.of(
                    new VehicleRecipe.Material(Items.IRON_INGOT, 3),
                    new VehicleRecipe.Material(Items.COPPER_INGOT, 2))),
            new VehicleRecipe(ModItems.EXHAUST, List.of(
                    new VehicleRecipe.Material(Items.IRON_INGOT, 4),
                    new VehicleRecipe.Material(Items.COPPER_INGOT, 1))),
            new VehicleRecipe(ModItems.WATER_PUMP, List.of(
                    new VehicleRecipe.Material(Items.IRON_INGOT, 2),
                    new VehicleRecipe.Material(Items.COPPER_INGOT, 1))),
            // The wax pellet that opens it as the coolant warms.
            new VehicleRecipe(ModItems.THERMOSTAT, List.of(
                    new VehicleRecipe.Material(Items.COPPER_INGOT, 1),
                    new VehicleRecipe.Material(Items.HONEYCOMB, 1))),
            new VehicleRecipe(ModItems.TANK_PUMP, List.of(
                    new VehicleRecipe.Material(Items.IRON_INGOT, 2),
                    new VehicleRecipe.Material(Items.COPPER_INGOT, 1),
                    new VehicleRecipe.Material(Items.REDSTONE, 2))),
            new VehicleRecipe(ModItems.INJECTORS, List.of(
                    new VehicleRecipe.Material(Items.IRON_INGOT, 2),
                    new VehicleRecipe.Material(Items.REDSTONE, 2))),
            new VehicleRecipe(ModItems.IGNITION_COILS, List.of(
                    new VehicleRecipe.Material(Items.COPPER_INGOT, 3),
                    new VehicleRecipe.Material(Items.REDSTONE, 2))),
            new VehicleRecipe(ModItems.BRAKE_CALIPERS, List.of(
                    new VehicleRecipe.Material(Items.IRON_INGOT, 3))),
            // Leather for the boot that keeps the grease in.
            new VehicleRecipe(ModItems.BALL_JOINTS, List.of(
                    new VehicleRecipe.Material(Items.IRON_INGOT, 2),
                    new VehicleRecipe.Material(Items.LEATHER, 1))),
            new VehicleRecipe(ModItems.MUFFLER, List.of(
                    new VehicleRecipe.Material(Items.IRON_INGOT, 3))),
            // The clipboard. Cheap on purpose: a workshop that cannot find out
            // what is wrong is just a shop that sells gearboxes.
            new VehicleRecipe(ModItems.DIAGNOSTIC_SHEET, List.of(
                    new VehicleRecipe.Material(Items.PAPER, 3),
                    new VehicleRecipe.Material(Items.COPPER_INGOT, 1))),
            // A sealed drum: more metal than anything else on the bench, which
            // is why a tanker costs what it costs.
            new VehicleRecipe(ModItems.TANKER_BARREL, List.of(
                    new VehicleRecipe.Material(Items.IRON_INGOT, 6),
                    new VehicleRecipe.Material(Items.COPPER_INGOT, 2))),
            // Planks on an iron frame. Wood because a bed is the one part of a
            // lorry you are expected to dent.
            new VehicleRecipe(ModItems.PICKUP_BED, List.of(
                    new VehicleRecipe.Material(Items.OAK_PLANKS, 6),
                    new VehicleRecipe.Material(Items.IRON_INGOT, 3))),
            // Built empty: the can is sheet metal, and the refinery is the only
            // thing that puts petrol in it.
            new VehicleRecipe(ModItems.JERRY_CAN, me.trossty.car.item.JerryCans.CAPACITY, List.of(
                    new VehicleRecipe.Material(Items.IRON_INGOT, 3),
                    new VehicleRecipe.Material(Items.COPPER_INGOT, 1))))));

    /**
     * The plain parts, then every other kind of the ones that come in kinds.
     *
     * <p>The kinds go on the end rather than beside the part they are a kind
     * of, so that the first engine in the list is still the engine, the first
     * wheel still the wheel, and everything that looks a recipe up by what it
     * makes still finds the plain one.
     */
    /**
     * The bench's catalogue followed by a refurbish for every part it makes:
     * the tired part plus half of a new one's materials gives it back at
     * {@link CarPart#REFURBISHED}. It used to live on the lift's own window;
     * the lift is for raising cars, and the bench is where parts are made.
     */
    private static List<VehicleRecipe> withRefurbishes(List<VehicleRecipe> bench) {
        List<VehicleRecipe> entries = new java.util.ArrayList<>(bench);
        for (CarPart part : CarPart.values()) {
            VehicleRecipe fresh = byResult(bench, part.item());
            if (fresh != null) {
                entries.add(VehicleRecipe.refurbish(part, fresh));
            }
        }
        return List.copyOf(entries);
    }

    private static List<VehicleRecipe> workbench(List<VehicleRecipe> plain) {
        List<VehicleRecipe> entries = new java.util.ArrayList<>(plain);
        for (CarSetup.Fuel fuel : CarSetup.Fuel.values()) {
            if (!fuel.isEngineFuel() || fuel.isElectric()) {
                continue;
            }
            for (CarSetup.Layout layout : CarSetup.Layout.values()) {
                int variant = CarSetup.engineVariant(layout, fuel);
                if (variant != 0) {
                    entries.add(VehicleRecipe.ofKind(ModItems.ENGINE, variant, engine(layout, fuel)));
                }
            }
        }
        // The electric motor: copper windings, a little iron, and the battery
        // pack's redstone. No layouts — one motor, one variant.
        entries.add(VehicleRecipe.ofKind(ModItems.ENGINE, CarSetup.ELECTRIC_MOTOR, List.of(
                new VehicleRecipe.Material(Items.COPPER_INGOT, 10),
                new VehicleRecipe.Material(Items.IRON_INGOT, 4),
                new VehicleRecipe.Material(Items.REDSTONE_BLOCK, 2))));
        // A tyre is a road tyre with something done to the tread.
        entries.add(VehicleRecipe.ofKind(ModItems.WHEEL, CarSetup.Tyre.DIRT.ordinal(), List.of(
                new VehicleRecipe.Material(Items.BLACK_WOOL, 3),
                new VehicleRecipe.Material(Items.IRON_INGOT, 1),
                new VehicleRecipe.Material(Items.LEATHER, 2))));
        entries.add(VehicleRecipe.ofKind(ModItems.WHEEL, CarSetup.Tyre.SNOW.ordinal(), List.of(
                new VehicleRecipe.Material(Items.BLACK_WOOL, 3),
                new VehicleRecipe.Material(Items.IRON_INGOT, 1),
                new VehicleRecipe.Material(Items.IRON_NUGGET, 4))));
        entries.add(VehicleRecipe.ofKind(ModItems.WHEEL, CarSetup.Tyre.SPORT.ordinal(), List.of(
                new VehicleRecipe.Material(Items.BLACK_WOOL, 3),
                new VehicleRecipe.Material(Items.IRON_INGOT, 1),
                new VehicleRecipe.Material(Items.SLIME_BALL, 2))));
        // A cone of gauze on a metal neck: less paper, more metal.
        entries.add(VehicleRecipe.ofKind(ModItems.AIR_FILTER, 1, List.of(
                new VehicleRecipe.Material(Items.IRON_INGOT, 2),
                new VehicleRecipe.Material(Items.RED_WOOL, 1),
                new VehicleRecipe.Material(Items.STRING, 2))));
        entries.add(new VehicleRecipe(ModItems.TURBO, List.of(
                new VehicleRecipe.Material(Items.IRON_INGOT, 4),
                new VehicleRecipe.Material(Items.COPPER_INGOT, 2),
                new VehicleRecipe.Material(Items.REDSTONE, 2))));
        return List.copyOf(entries);
    }

    /**
     * What an engine costs: the four's metal times the size of the thing, and
     * a diesel is a heavier block with nothing to spark, so more iron and
     * coal where the redstone would have gone.
     */
    private static List<VehicleRecipe.Material> engine(CarSetup.Layout layout, CarSetup.Fuel fuel) {
        int size = layout.costFactor();
        boolean diesel = fuel == CarSetup.Fuel.DIESEL;
        return List.of(
                new VehicleRecipe.Material(Items.IRON_INGOT, 4 * size + (diesel ? 2 : 0)),
                new VehicleRecipe.Material(Items.COPPER_INGOT, 2 * size),
                diesel ? new VehicleRecipe.Material(Items.COAL_BLOCK, 1)
                        : new VehicleRecipe.Material(Items.REDSTONE_BLOCK, 1));
    }

    /** What the ramp can put together from the bench's components. */
    private static final List<VehicleRecipe> VEHICLES = List.of(
            new VehicleRecipe(ModItems.CAR, List.of(
                    new VehicleRecipe.Material(ModItems.CAR_BODY, 1),
                    new VehicleRecipe.Material(ModItems.ENGINE, 1),
                    new VehicleRecipe.Material(ModItems.TRANSMISSION, 1),
                    new VehicleRecipe.Material(ModItems.SUSPENSION, 4),
                    new VehicleRecipe.Material(ModItems.WHEEL, 4))),
            // Six wheels, two sets of springs and a barrel: a tanker is a car
            // that has been asked to carry something heavy.
            new VehicleRecipe(ModItems.FUEL_TANKER, List.of(
                    new VehicleRecipe.Material(ModItems.CAR_BODY, 1),
                    new VehicleRecipe.Material(ModItems.ENGINE, 1),
                    new VehicleRecipe.Material(ModItems.TRANSMISSION, 1),
                    new VehicleRecipe.Material(ModItems.SUSPENSION, 6),
                    new VehicleRecipe.Material(ModItems.WHEEL, 6),
                    new VehicleRecipe.Material(ModItems.TANKER_BARREL, 1))),
            // A car with a bed where the boot would be: same running gear, one
            // extra part.
            new VehicleRecipe(ModItems.PICKUP, List.of(
                    new VehicleRecipe.Material(ModItems.CAR_BODY, 1),
                    new VehicleRecipe.Material(ModItems.ENGINE, 1),
                    new VehicleRecipe.Material(ModItems.TRANSMISSION, 1),
                    new VehicleRecipe.Material(ModItems.SUSPENSION, 4),
                    new VehicleRecipe.Material(ModItems.WHEEL, 4),
                    new VehicleRecipe.Material(ModItems.PICKUP_BED, 1))));

    /**
     * The parts every vehicle of this chassis is assembled from, before
     * whatever its own design adds. The workstation starts from this.
     */
    public static List<VehicleRecipe.Material> chassisParts(
            me.trossty.car.vehicle.VehicleDefinition.Chassis chassis) {
        if (chassis == me.trossty.car.vehicle.VehicleDefinition.Chassis.TRAILER) {
            // One axle and its springs; the box itself is in the definition.
            return List.of(new VehicleRecipe.Material(ModItems.SUSPENSION, 2));
        }
        if (chassis == me.trossty.car.vehicle.VehicleDefinition.Chassis.MOTORCYCLE) {
            // An engine and a gearbox in a frame, a fork and a shock, two wheels.
            return List.of(
                    new VehicleRecipe.Material(ModItems.ENGINE, 1),
                    new VehicleRecipe.Material(ModItems.TRANSMISSION, 1),
                    new VehicleRecipe.Material(ModItems.SUSPENSION, 2),
                    new VehicleRecipe.Material(ModItems.WHEEL, 2));
        }
        if (chassis == me.trossty.car.vehicle.VehicleDefinition.Chassis.BOAT) {
            // A hull and an outboard: the motor and its gearbox, nothing to roll on.
            return List.of(
                    new VehicleRecipe.Material(ModItems.CAR_BODY, 1),
                    new VehicleRecipe.Material(ModItems.ENGINE, 1),
                    new VehicleRecipe.Material(ModItems.TRANSMISSION, 1));
        }
        if (chassis == me.trossty.car.vehicle.VehicleDefinition.Chassis.PLANE) {
            // A fuselage, an engine with the propeller on its crankshaft, and
            // three legs of landing gear.
            return List.of(
                    new VehicleRecipe.Material(ModItems.CAR_BODY, 1),
                    new VehicleRecipe.Material(ModItems.ENGINE, 1),
                    new VehicleRecipe.Material(ModItems.SUSPENSION, 3),
                    new VehicleRecipe.Material(ModItems.WHEEL, 3));
        }
        if (chassis == me.trossty.car.vehicle.VehicleDefinition.Chassis.HELICOPTER) {
            // A cabin, the engine and the gearbox that turns the rotors. It
            // stands on skids: nothing to roll on, nothing to spring.
            return List.of(
                    new VehicleRecipe.Material(ModItems.CAR_BODY, 1),
                    new VehicleRecipe.Material(ModItems.ENGINE, 1),
                    new VehicleRecipe.Material(ModItems.TRANSMISSION, 1));
        }
        net.minecraft.world.item.Item item = me.trossty.car.item.CarItem.itemFor(chassis);
        for (VehicleRecipe recipe : VEHICLES) {
            if (recipe.result() == item) {
                return recipe.materials();
            }
        }
        return VEHICLES.get(0).materials();
    }

    /** The recipe that makes this item, or null. */
    public static VehicleRecipe byResult(List<VehicleRecipe> recipes, net.minecraft.world.item.Item item) {
        for (VehicleRecipe recipe : recipes) {
            if (recipe.result() == item && !recipe.refurbished() && recipe.variant() == 0) {
                return recipe;
            }
        }
        return null;
    }

    private ModVehicleRecipes() {
    }

    /** Clamps an index from the network to something that really exists. */
    public static VehicleRecipe byIndex(List<VehicleRecipe> recipes, int index) {
        if (index < 0 || index >= recipes.size()) {
            return recipes.get(0);
        }
        return recipes.get(index);
    }
}
