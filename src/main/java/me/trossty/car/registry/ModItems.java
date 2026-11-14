package me.trossty.car.registry;

import me.trossty.car.CarMain;
import me.trossty.car.item.CarItem;
import me.trossty.car.item.VehiclePartItem;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.Item;
import java.util.function.Function;

public final class ModItems {

    public static final Item CAR = register("car", CarItem::new, new Item.Properties().stacksTo(1));

    /** The same item, pointed at the tanker. Not dyeable: a tanker wears steel. */
    public static final Item FUEL_TANKER = register(
            "fuel_tanker",
            settings -> new CarItem(settings, ModEntities.FUEL_TANKER),
            new Item.Properties().stacksTo(1));

    /** The same item again, pointed at the pickup. */
    public static final Item PICKUP = register(
            "pickup",
            settings -> new CarItem(settings, ModEntities.PICKUP),
            new Item.Properties().stacksTo(1));

    /** And at the trailer, which has nothing to start. */
    public static final Item TRAILER = register(
            "trailer",
            settings -> new CarItem(settings, ModEntities.TRAILER),
            new Item.Properties().stacksTo(1));

    /** And at the two-wheelers. */
    public static final Item MOTORCYCLE = register(
            "motorcycle",
            settings -> new CarItem(settings, ModEntities.MOTORCYCLE),
            new Item.Properties().stacksTo(1));

    /** And at the water... */
    public static final Item MOTORBOAT = register(
            "motorboat",
            settings -> new CarItem(settings, ModEntities.MOTORBOAT),
            new Item.Properties().stacksTo(1));

    /** ...and the air. */
    public static final Item AIRPLANE = register(
            "airplane",
            settings -> new CarItem(settings, ModEntities.AIRPLANE),
            new Item.Properties().stacksTo(1));

    /**
     * Portable fuel. Durability is the fuel gauge, read backwards, in tank
     * units — see {@link me.trossty.car.item.JerryCans}. The can itself is
     * never consumed, because an empty can is something you take back to the
     * refinery, not rubbish.
     */
    public static final Item JERRY_CAN = register(
            "jerry_can", me.trossty.car.item.JerryCanItem::new, new Item.Properties().durability(me.trossty.car.item.JerryCans.CAPACITY));

    /**
     * A fuel pump's nozzle, on the end of its hose. Not made, not carried
     * away: taken off the pump with a bare hand and hung back on it, and it
     * goes back by itself the moment it leaves the hand or the hose runs out.
     */
    public static final Item FUEL_NOZZLE = register(
            "fuel_nozzle", me.trossty.car.item.FuelNozzleItem::new, new Item.Properties().stacksTo(1));

    /**
     * The parts a car is made of. Each is built at the vehicle workbench and is
     * useless on its own: only the lift turns a set of them into a car.
     */
    public static final Item CAR_BODY = register("car_body", Item::new, new Item.Properties().stacksTo(16));

    /**
     * Everything that wears out and can be swapped at a lift, one item per
     * {@link me.trossty.car.entity.CarPart}. They are {@link VehiclePartItem}s
     * only so a used one can say how used it is; a new one is indistinguishable
     * from the plain item it used to be.
     *
     * <p>The lift only ever asks for the big five. The rest come fitted in a new
     * car and are bought one at a time, when a diagnosis says which one is the
     * problem — which is the whole of the workshop loop.
     */
    public static final Item ENGINE = part("engine");
    public static final Item TRANSMISSION = part("transmission");
    public static final Item SUSPENSION = part("suspension");
    public static final Item WHEEL = part("wheel");

    public static final Item AIR_FILTER = part("air_filter");
    public static final Item SPARK_PLUGS = part("spark_plugs");
    public static final Item OIL_FILTER = part("oil_filter");
    public static final Item RADIATOR = part("radiator");
    public static final Item BATTERY = part("battery");
    public static final Item ALTERNATOR = part("alternator");
    public static final Item DRIVESHAFT = part("driveshaft");
    public static final Item CLUTCH = part("clutch");
    public static final Item BRAKE_PADS = part("brake_pads");
    public static final Item BRAKE_DISCS = part("brake_discs");
    public static final Item STEERING_RACK = part("steering_rack");
    public static final Item EXHAUST = part("exhaust");

    /**
     * The clipboard. Held against a car it reads out every system; it is the
     * only way to find out what is wrong without taking the car apart.
     */
    public static final Item DIAGNOSTIC_SHEET =
            register("diagnostic_sheet", Item::new, new Item.Properties().stacksTo(1));

    /**
     * A handheld code reader: plugged into a car (or used from the driver's
     * seat) it reads out the fault codes, worst first, and the mileage.
     */
    /** The heavy end of crude oil, left in the refinery's column: what roads are bound with. */
    public static final Item TAR = register("tar", Item::new, new Item.Properties());

    /** Road paint, a tin per colour: see {@link me.trossty.car.item.RoadPaintItem}. */
    public static final Item WHITE_ROAD_PAINT = roadPaint(me.trossty.car.block.AsphaltBlock.Paint.WHITE);
    public static final Item YELLOW_ROAD_PAINT = roadPaint(me.trossty.car.block.AsphaltBlock.Paint.YELLOW);
    public static final Item RED_ROAD_PAINT = roadPaint(me.trossty.car.block.AsphaltBlock.Paint.RED);
    public static final Item BLUE_ROAD_PAINT = roadPaint(me.trossty.car.block.AsphaltBlock.Paint.BLUE);

    private static Item roadPaint(me.trossty.car.block.AsphaltBlock.Paint paint) {
        return register(paint.getSerializedName() + "_road_paint",
                settings -> new me.trossty.car.item.RoadPaintItem(paint, settings),
                new Item.Properties().durability(me.trossty.car.item.RoadPaintItem.CHARGES));
    }

    public static final Item OBD_SCANNER =
            register("obd_scanner", me.trossty.car.item.ObdScannerItem::new, new Item.Properties().stacksTo(1));

    /** The tanker's barrel. A bench part like any other: the lift bolts it on. */
    public static final Item TANKER_BARREL =
            register("tanker_barrel", Item::new, new Item.Properties().stacksTo(16));

    /** The pickup's bed: planks and an iron frame, bolted on at the lift. */
    public static final Item PICKUP_BED =
            register("pickup_bed", Item::new, new Item.Properties().stacksTo(16));

    /**
     * A turbocharger. Not a slot on the car: it bolts to whatever engine is
     * there, at the lift, and comes away with that engine.
     */
    public static final Item TURBO =
            register("turbo", me.trossty.car.item.TurboItem::new, new Item.Properties().stacksTo(16));

    /** A bottle of motor oil, poured in under the bonnet to top up the sump. */
    public static final Item MOTOR_OIL =
            register("motor_oil", Item::new, new Item.Properties().stacksTo(16));

    /** A bottle of brake fluid, poured into the reservoir under the bonnet. */
    public static final Item BRAKE_FLUID =
            register("brake_fluid", Item::new, new Item.Properties().stacksTo(16));

    /** A spanner: sets the tracking of a car on the lift straight, and opens an engine on its stand. */
    public static final Item WRENCH = register("wrench", Item::new, new Item.Properties().stacksTo(1));

    /** Jump leads: from a car with a good battery to one with a flat. */
    public static final Item JUMPER_CABLES =
            register("jumper_cables", Item::new, new Item.Properties().stacksTo(1));

    /** A new timing belt, for an engine whose old one is due or gone. */
    public static final Item TIMING_BELT =
            register("timing_belt", Item::new, new Item.Properties().stacksTo(16));

    /** A new catalytic converter, for an exhaust choked with soot. */
    public static final Item CATALYTIC_CONVERTER =
            register("catalytic_converter", Item::new, new Item.Properties().stacksTo(16));

    /** A foot pump: a stroke of air into every tyre. */
    public static final Item TYRE_PUMP =
            register("tyre_pump", Item::new, new Item.Properties().stacksTo(1));

    /** The insides of an engine, as they come out of one opened up on its stand. */
    public static final Item PISTONS = part("pistons");
    public static final Item HEAD_GASKET = part("head_gasket");
    public static final Item CRANKSHAFT = part("crankshaft");

    /** The insides of an engine in the order the stand keeps them. */
    public static Item[] engineInternals() {
        return new Item[] {PISTONS, HEAD_GASKET, CRANKSHAFT};
    }

    /** Car paint in a can, any colour the dyes mixed into it make. */
    public static final Item SPRAY_CAN = register("spray_can", me.trossty.car.item.SprayCanItem::new,
            new Item.Properties().durability(me.trossty.car.item.SprayCanItem.CHARGES));

    /** A car key: blank until it is held against a car. */
    public static final Item CAR_KEY = register("car_key", me.trossty.car.item.CarKeyItem::new,
            new Item.Properties().stacksTo(16));

    private ModItems() {
    }

    /** A spare part: stacks like one, and remembers how worn it is. */
    private static Item part(String name) {
        return register(name, VehiclePartItem::new, new Item.Properties().stacksTo(16));
    }

    private static Item register(String name, Function<Item.Properties, Item> factory, Item.Properties settings) {
        ResourceKey<Item> key = ResourceKey.create(Registries.ITEM, CarMain.id(name));
        return Registry.register(BuiltInRegistries.ITEM, key, factory.apply(settings));
    }

    /** The mod's own tabs in the creative inventory: vehicles, then parts and tools, then machines and roads. */
    public static final ResourceKey<net.minecraft.world.item.CreativeModeTab> GROUP =
            ResourceKey.create(Registries.CREATIVE_MODE_TAB, CarMain.id("vehicles"));
    public static final ResourceKey<net.minecraft.world.item.CreativeModeTab> PARTS_GROUP =
            ResourceKey.create(Registries.CREATIVE_MODE_TAB, CarMain.id("parts"));
    public static final ResourceKey<net.minecraft.world.item.CreativeModeTab> MACHINES_GROUP =
            ResourceKey.create(Registries.CREATIVE_MODE_TAB, CarMain.id("machines"));

    /**
     * Just enough of a tab's entries to add stacks and items to. A tab refuses
     * the same stack twice, and a default can may well equal a full one, so
     * everything goes through here and a repeat is quietly skipped.
     */
    private static final class Entries {
        private final net.minecraft.world.item.CreativeModeTab.Output output;
        private final java.util.List<net.minecraft.world.item.ItemStack> added = new java.util.ArrayList<>();

        Entries(net.minecraft.world.item.CreativeModeTab.Output output) {
            this.output = output;
        }

        void add(net.minecraft.world.item.ItemStack stack) {
            for (net.minecraft.world.item.ItemStack there : this.added) {
                if (net.minecraft.world.item.ItemStack.isSameItemSameComponents(there, stack)) {
                    return;
                }
            }
            this.added.add(stack);
            this.output.accept(stack);
        }

        void add(net.minecraft.world.level.ItemLike... items) {
            for (net.minecraft.world.level.ItemLike item : items) {
                this.add(new net.minecraft.world.item.ItemStack(item));
            }
        }
    }

    public static void register() {
        Registry.register(BuiltInRegistries.RECIPE_SERIALIZER, CarMain.id("spray_can_refill"),
                me.trossty.car.recipe.SprayCanRefillRecipe.SERIALIZER);
        Registry.register(BuiltInRegistries.CREATIVE_MODE_TAB, GROUP,
                net.minecraft.world.item.CreativeModeTab.builder()
                        .icon(() -> new net.minecraft.world.item.ItemStack(CAR))
                        .title(net.minecraft.network.chat.Component.translatable("itemGroup.car.vehicles"))
                        .withTabsBefore(net.minecraft.world.item.CreativeModeTabs.SPAWN_EGGS)
                        .displayItems((context, output) -> vehicles(context, new Entries(output)))
                        .build());
        Registry.register(BuiltInRegistries.CREATIVE_MODE_TAB, PARTS_GROUP,
                net.minecraft.world.item.CreativeModeTab.builder()
                        .icon(() -> new net.minecraft.world.item.ItemStack(ENGINE))
                        .title(net.minecraft.network.chat.Component.translatable("itemGroup.car.parts"))
                        .withTabsBefore(GROUP)
                        .displayItems((context, output) -> parts(new Entries(output)))
                        .build());
        Registry.register(BuiltInRegistries.CREATIVE_MODE_TAB, MACHINES_GROUP,
                net.minecraft.world.item.CreativeModeTab.builder()
                        .icon(() -> new net.minecraft.world.item.ItemStack(ModBlocks.PUMPJACK))
                        .title(net.minecraft.network.chat.Component.translatable("itemGroup.car.machines"))
                        .withTabsBefore(PARTS_GROUP)
                        .displayItems((context, output) -> machines(new Entries(output)))
                        .build());
    }

    /**
     * Every vehicle the loaded data packs describe, as it leaves the factory,
     * kind by kind — cars, pickups, tankers, trailers, motorbikes, boats,
     * aircraft — then the key and full cans to get going with.
     */
    private static void vehicles(net.minecraft.world.item.CreativeModeTab.ItemDisplayParameters context,
                                 Entries entries) {
        context.holders()
                .lookup(me.trossty.car.vehicle.VehicleDefinition.KEY)
                .ifPresent(registry -> registry.listElements()
                        .sorted(java.util.Comparator
                                .<net.minecraft.core.Holder.Reference<me.trossty.car.vehicle.VehicleDefinition>>comparingInt(
                                        entry -> entry.value().chassis().ordinal())
                                .thenComparing(entry -> entry.key().location().getPath()))
                        .forEach(entry -> entries.add(me.trossty.car.item.CarItem.stackOf(
                                entry.key().location(), entry.value()))));
        entries.add(CAR_KEY);
        for (int kind = 0; kind < 3; kind++) {
            entries.add(jerryCan(kind, me.trossty.car.item.JerryCans.CAPACITY));
        }
    }

    /** Tools first, then every part in teardown order, each with its other kinds beside it. */
    private static void parts(Entries entries) {
        entries.add(OBD_SCANNER, DIAGNOSTIC_SHEET, CAR_KEY);
        for (int kind = 0; kind < 3; kind++) {
            entries.add(jerryCan(kind, me.trossty.car.item.JerryCans.CAPACITY));
        }
        entries.add(jerryCan(0, 0));
        entries.add(SPRAY_CAN);
        for (net.minecraft.world.item.DyeColor dye : net.minecraft.world.item.DyeColor.values()) {
            net.minecraft.world.item.ItemStack can = new net.minecraft.world.item.ItemStack(SPRAY_CAN);
            can.set(net.minecraft.core.component.DataComponents.DYED_COLOR,
                    new net.minecraft.world.item.component.DyedItemColor(dye.getTextureDiffuseColor() & 0xFFFFFF, true));
            entries.add(can);
        }
        entries.add(CAR_BODY);
        for (me.trossty.car.entity.CarPart part : me.trossty.car.entity.CarPart.values()) {
            entries.add(part.item());
            for (int variant = 1; variant < me.trossty.car.entity.CarSetup.variantsOf(part); variant++) {
                net.minecraft.world.item.ItemStack stack = new net.minecraft.world.item.ItemStack(part.item());
                stack.set(ModComponents.PART_VARIANT, variant);
                entries.add(stack);
            }
        }
        entries.add(TURBO, MOTOR_OIL, BRAKE_FLUID, WRENCH, JUMPER_CABLES, TIMING_BELT, CATALYTIC_CONVERTER, TYRE_PUMP, PISTONS, HEAD_GASKET, CRANKSHAFT,
                TANKER_BARREL, PICKUP_BED);
    }

    /** The garage, then oil from the ground to the pump, then power, then road building. */
    private static void machines(Entries entries) {
        entries.add(ModBlocks.VEHICLE_WORKSTATION, ModBlocks.VEHICLE_WORKBENCH, ModBlocks.VEHICLE_LIFT,
                ModBlocks.ENGINE_STAND, ModBlocks.ENGINE_HOIST, ModBlocks.DRAIN_PAN, ModBlocks.DYNO,
                ModBlocks.TOOLBOX, ModBlocks.BOAT_CRADLE, ModBlocks.AIRCRAFT_JACK);
        entries.add(ModBlocks.OIL_DEPOSIT, ModBlocks.OIL_SEEP, ModBlocks.PUMPJACK, ModBlocks.FUEL_REFINERY,
                ModBlocks.HYDROTREATER, ModBlocks.FUEL_TANK, ModBlocks.FUEL_DRUM, ModBlocks.FUEL_PIPE,
                ModBlocks.FUEL_VALVE, ModBlocks.FUEL_PUMP, FUEL_NOZZLE);
        entries.add(ModBlocks.GENERATOR, ModBlocks.BIOFUEL_GENERATOR, ModBlocks.DIESEL_GENERATOR,
                ModBlocks.BATTERY_BANK);
        net.minecraft.world.item.ItemStack charged = new net.minecraft.world.item.ItemStack(ModBlocks.BATTERY_BANK);
        charged.set(ModComponents.STORED_ENERGY, me.trossty.car.block.entity.BatteryBankBlockEntity.CAPACITY);
        entries.add(charged);
        entries.add(ModBlocks.ENERGY_CABLE, ModBlocks.CHARGING_POST);
        entries.add(TAR, ModBlocks.ASPHALT, WHITE_ROAD_PAINT, YELLOW_ROAD_PAINT, RED_ROAD_PAINT, BLUE_ROAD_PAINT);
    }

    /** A jerry can of one fuel (0 petrol, 1 diesel, 2 biofuel) holding this much. */
    private static net.minecraft.world.item.ItemStack jerryCan(int kind, int units) {
        net.minecraft.world.item.ItemStack can = new net.minecraft.world.item.ItemStack(JERRY_CAN);
        can.set(ModComponents.FUEL_KIND, kind);
        me.trossty.car.item.JerryCans.setUnits(can, units);
        return can;
    }
}
