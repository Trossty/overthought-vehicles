package me.trossty.car.registry;

import me.trossty.car.CarMain;
import me.trossty.car.item.CarItem;
import me.trossty.car.item.VehiclePartItem;
import net.fabricmc.fabric.api.itemgroup.v1.ItemGroupEvents;
import net.minecraft.item.Item;
import net.minecraft.item.ItemGroups;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryKeys;

import java.util.function.Function;

public final class ModItems {

    public static final Item CAR = register("car", CarItem::new, new Item.Settings().maxCount(1));

    /** The same item, pointed at the tanker. Not dyeable: a tanker wears steel. */
    public static final Item FUEL_TANKER = register(
            "fuel_tanker",
            settings -> new CarItem(settings, ModEntities.FUEL_TANKER),
            new Item.Settings().maxCount(1));

    /** The same item again, pointed at the pickup. */
    public static final Item PICKUP = register(
            "pickup",
            settings -> new CarItem(settings, ModEntities.PICKUP),
            new Item.Settings().maxCount(1));

    /** And at the trailer, which has nothing to start. */
    public static final Item TRAILER = register(
            "trailer",
            settings -> new CarItem(settings, ModEntities.TRAILER),
            new Item.Settings().maxCount(1));

    /** And at the two-wheelers. */
    public static final Item MOTORCYCLE = register(
            "motorcycle",
            settings -> new CarItem(settings, ModEntities.MOTORCYCLE),
            new Item.Settings().maxCount(1));

    /**
     * Portable fuel. Durability is the fuel gauge, read backwards, in tank
     * units — see {@link me.trossty.car.item.JerryCans}. The can itself is
     * never consumed, because an empty can is something you take back to the
     * refinery, not rubbish.
     */
    public static final Item JERRY_CAN = register(
            "jerry_can", me.trossty.car.item.JerryCanItem::new, new Item.Settings().maxDamage(me.trossty.car.item.JerryCans.CAPACITY));

    /**
     * A fuel pump's nozzle, on the end of its hose. Not made, not carried
     * away: taken off the pump with a bare hand and hung back on it, and it
     * goes back by itself the moment it leaves the hand or the hose runs out.
     */
    public static final Item FUEL_NOZZLE = register(
            "fuel_nozzle", me.trossty.car.item.FuelNozzleItem::new, new Item.Settings().maxCount(1));

    /**
     * The parts a car is made of. Each is built at the vehicle workbench and is
     * useless on its own: only the lift turns a set of them into a car.
     */
    public static final Item CAR_BODY = register("car_body", Item::new, new Item.Settings().maxCount(16));

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
            register("diagnostic_sheet", Item::new, new Item.Settings().maxCount(1));

    /**
     * A handheld code reader: plugged into a car (or used from the driver's
     * seat) it reads out the fault codes, worst first, and the mileage.
     */
    /** The heavy end of crude oil, left in the refinery's column: what roads are bound with. */
    public static final Item TAR = register("tar", Item::new, new Item.Settings());

    /** Road paint, a tin per colour: see {@link me.trossty.car.item.RoadPaintItem}. */
    public static final Item WHITE_ROAD_PAINT = roadPaint(me.trossty.car.block.AsphaltBlock.Paint.WHITE);
    public static final Item YELLOW_ROAD_PAINT = roadPaint(me.trossty.car.block.AsphaltBlock.Paint.YELLOW);
    public static final Item RED_ROAD_PAINT = roadPaint(me.trossty.car.block.AsphaltBlock.Paint.RED);
    public static final Item BLUE_ROAD_PAINT = roadPaint(me.trossty.car.block.AsphaltBlock.Paint.BLUE);

    private static Item roadPaint(me.trossty.car.block.AsphaltBlock.Paint paint) {
        return register(paint.asString() + "_road_paint",
                settings -> new me.trossty.car.item.RoadPaintItem(paint, settings),
                new Item.Settings().maxDamage(me.trossty.car.item.RoadPaintItem.CHARGES));
    }

    public static final Item OBD_SCANNER =
            register("obd_scanner", me.trossty.car.item.ObdScannerItem::new, new Item.Settings().maxCount(1));

    /** The tanker's barrel. A bench part like any other: the lift bolts it on. */
    public static final Item TANKER_BARREL =
            register("tanker_barrel", Item::new, new Item.Settings().maxCount(16));

    /** The pickup's bed: planks and an iron frame, bolted on at the lift. */
    public static final Item PICKUP_BED =
            register("pickup_bed", Item::new, new Item.Settings().maxCount(16));

    /**
     * A turbocharger. Not a slot on the car: it bolts to whatever engine is
     * there, at the lift, and comes away with that engine.
     */
    public static final Item TURBO =
            register("turbo", me.trossty.car.item.TurboItem::new, new Item.Settings().maxCount(16));

    /** Car paint in a can, any colour the dyes mixed into it make. */
    public static final Item SPRAY_CAN = register("spray_can", me.trossty.car.item.SprayCanItem::new,
            new Item.Settings().maxDamage(me.trossty.car.item.SprayCanItem.CHARGES));

    /** A car key: blank until it is held against a car. */
    public static final Item CAR_KEY = register("car_key", me.trossty.car.item.CarKeyItem::new,
            new Item.Settings().maxCount(16));

    private ModItems() {
    }

    /** A spare part: stacks like one, and remembers how worn it is. */
    private static Item part(String name) {
        return register(name, VehiclePartItem::new, new Item.Settings().maxCount(16));
    }

    private static Item register(String name, Function<Item.Settings, Item> factory, Item.Settings settings) {
        RegistryKey<Item> key = RegistryKey.of(RegistryKeys.ITEM, CarMain.id(name));
        return Registry.register(Registries.ITEM, key, factory.apply(settings.registryKey(key)));
    }

    /** The mod's own tab in the creative inventory, with a car on it. */
    public static final RegistryKey<net.minecraft.item.ItemGroup> GROUP =
            RegistryKey.of(RegistryKeys.ITEM_GROUP, CarMain.id("vehicles"));

    /** Just enough of a tab's entries to add stacks and items to. */
    private interface Entries {
        void add(net.minecraft.item.ItemStack stack);

        default void add(net.minecraft.item.ItemConvertible item) {
            this.add(new net.minecraft.item.ItemStack(item));
        }
    }

    /** The testing tab: every item the mod has, every kind of each, and what the machines eat. */
    public static final RegistryKey<net.minecraft.item.ItemGroup> TEST_GROUP =
            RegistryKey.of(RegistryKeys.ITEM_GROUP, CarMain.id("everything"));

    public static void register() {
        Registry.register(Registries.RECIPE_SERIALIZER, CarMain.id("spray_can_refill"),
                me.trossty.car.recipe.SprayCanRefillRecipe.SERIALIZER);
        Registry.register(Registries.ITEM_GROUP, GROUP,
                net.fabricmc.fabric.api.itemgroup.v1.FabricItemGroup.builder()
                        .icon(() -> new net.minecraft.item.ItemStack(CAR))
                        .displayName(net.minecraft.text.Text.translatable("itemGroup.car.vehicles"))
                        .build());
        Registry.register(Registries.ITEM_GROUP, TEST_GROUP,
                net.fabricmc.fabric.api.itemgroup.v1.FabricItemGroup.builder()
                        .icon(() -> new net.minecraft.item.ItemStack(ModBlocks.FUEL_REFINERY))
                        .displayName(net.minecraft.text.Text.translatable("itemGroup.car.everything"))
                        .entries(ModItems::everything)
                        .build());
        ItemGroupEvents.modifyEntriesEvent(GROUP).register(entries -> {
            // Every vehicle the loaded data packs describe, as it leaves the factory.
            entries.getContext().lookup()
                    .getOptional(me.trossty.car.vehicle.VehicleDefinition.KEY)
                    .ifPresent(registry -> registry.streamEntries().forEach(entry ->
                            entries.add(me.trossty.car.item.CarItem.stackOf(
                                    entry.registryKey().getValue(), entry.value()))));
            entries.add(JERRY_CAN);
            entries.add(CAR_KEY);
            entries.add(SPRAY_CAN);
            for (net.minecraft.util.DyeColor dye : net.minecraft.util.DyeColor.values()) {
                net.minecraft.item.ItemStack can = new net.minecraft.item.ItemStack(SPRAY_CAN);
                can.set(net.minecraft.component.DataComponentTypes.DYED_COLOR,
                        new net.minecraft.component.type.DyedColorComponent(dye.getEntityColor() & 0xFFFFFF));
                entries.add(can);
            }
            entries.add(CAR_BODY);
            entries.add(DIAGNOSTIC_SHEET);
            entries.add(OBD_SCANNER);
            // In teardown order, so the group reads the way a car comes apart.
            for (me.trossty.car.entity.CarPart part : me.trossty.car.entity.CarPart.values()) {
                entries.add(part.item());
            }
            // Then every other kind of the parts that come in kinds.
            for (me.trossty.car.entity.CarPart part : me.trossty.car.entity.CarPart.values()) {
                for (int variant = 1;
                     variant < me.trossty.car.entity.CarSetup.variantsOf(part); variant++) {
                    net.minecraft.item.ItemStack stack = new net.minecraft.item.ItemStack(part.item());
                    stack.set(ModComponents.PART_VARIANT, variant);
                    entries.add(stack);
                }
            }
            entries.add(TURBO);
            net.minecraft.item.ItemStack diesel = new net.minecraft.item.ItemStack(JERRY_CAN);
            diesel.set(ModComponents.FUEL_KIND, 1);
            entries.add(diesel);
            net.minecraft.item.ItemStack bio = new net.minecraft.item.ItemStack(JERRY_CAN);
            bio.set(ModComponents.FUEL_KIND, 2);
            entries.add(bio);
            entries.add(TANKER_BARREL);
            entries.add(PICKUP_BED);
            entries.add(ModBlocks.VEHICLE_WORKSTATION);
            entries.add(ModBlocks.VEHICLE_WORKBENCH);
            entries.add(ModBlocks.VEHICLE_LIFT);
            entries.add(ModBlocks.PUMPJACK);
            entries.add(ModBlocks.FUEL_REFINERY);
            entries.add(ModBlocks.HYDROTREATER);
            entries.add(ModBlocks.GENERATOR);
            entries.add(ModBlocks.BIOFUEL_GENERATOR);
            entries.add(ModBlocks.DIESEL_GENERATOR);
            entries.add(ModBlocks.ENERGY_CABLE);
            entries.add(ModBlocks.BATTERY_BANK);
            net.minecraft.item.ItemStack charged = new net.minecraft.item.ItemStack(ModBlocks.BATTERY_BANK);
            charged.set(ModComponents.STORED_ENERGY, me.trossty.car.block.entity.BatteryBankBlockEntity.CAPACITY);
            entries.add(charged);
            entries.add(ModBlocks.CHARGING_POST);
            entries.add(ModBlocks.OIL_DEPOSIT);
            entries.add(ModBlocks.OIL_SEEP);
            entries.add(ModBlocks.FUEL_PUMP);
            entries.add(ModBlocks.FUEL_PIPE);
            entries.add(ModBlocks.FUEL_VALVE);
            entries.add(ModBlocks.FUEL_TANK);
            entries.add(ModBlocks.FUEL_DRUM);
            entries.add(TAR);
            entries.add(ModBlocks.ASPHALT);
            entries.add(WHITE_ROAD_PAINT);
            entries.add(YELLOW_ROAD_PAINT);
            entries.add(RED_ROAD_PAINT);
            entries.add(BLUE_ROAD_PAINT);
        });
    }

    /**
     * The testing tab's contents, so nothing has to be crafted to try it:
     * every vehicle, every item and block the mod registers (read from the
     * registry, so a new one turns up without being listed), each kind of the
     * things that come in kinds, full and empty cans of every fuel, and the
     * vanilla things the machines run on.
     */
    private static void everything(net.minecraft.item.ItemGroup.DisplayContext context,
                                   net.minecraft.item.ItemGroup.Entries group) {
        // A tab refuses the same stack twice, and a default can may well equal a
        // full one, so everything goes through here first.
        java.util.List<net.minecraft.item.ItemStack> added = new java.util.ArrayList<>();
        Entries entries = new Entries() {
            @Override
            public void add(net.minecraft.item.ItemStack stack) {
                for (net.minecraft.item.ItemStack there : added) {
                    if (net.minecraft.item.ItemStack.areItemsAndComponentsEqual(there, stack)) {
                        return;
                    }
                }
                added.add(stack);
                group.add(stack);
            }
        };
        context.lookup().getOptional(me.trossty.car.vehicle.VehicleDefinition.KEY)
                .ifPresent(registry -> registry.streamEntries().forEach(entry ->
                        entries.add(me.trossty.car.item.CarItem.stackOf(
                                entry.registryKey().getValue(), entry.value()))));
        // Every item of ours, in the order they were registered.
        for (Item item : Registries.ITEM) {
            if (Registries.ITEM.getId(item).getNamespace().equals(CarMain.MOD_ID) && item != CAR) {
                entries.add(item);
            }
        }
        // Cans of every fuel, full and empty.
        for (int kind = 0; kind < 3; kind++) {
            net.minecraft.item.ItemStack full = new net.minecraft.item.ItemStack(JERRY_CAN);
            full.set(ModComponents.FUEL_KIND, kind);
            me.trossty.car.item.JerryCans.setUnits(full, me.trossty.car.item.JerryCans.CAPACITY);
            entries.add(full);
        }
        net.minecraft.item.ItemStack empty = new net.minecraft.item.ItemStack(JERRY_CAN);
        me.trossty.car.item.JerryCans.setUnits(empty, 0);
        entries.add(empty);
        // Every other kind of the parts that come in kinds, and every paint.
        for (me.trossty.car.entity.CarPart part : me.trossty.car.entity.CarPart.values()) {
            for (int variant = 1; variant < me.trossty.car.entity.CarSetup.variantsOf(part); variant++) {
                net.minecraft.item.ItemStack stack = new net.minecraft.item.ItemStack(part.item());
                stack.set(ModComponents.PART_VARIANT, variant);
                entries.add(stack);
            }
        }
        for (net.minecraft.util.DyeColor dye : net.minecraft.util.DyeColor.values()) {
            net.minecraft.item.ItemStack can = new net.minecraft.item.ItemStack(SPRAY_CAN);
            can.set(net.minecraft.component.DataComponentTypes.DYED_COLOR,
                    new net.minecraft.component.type.DyedColorComponent(dye.getEntityColor() & 0xFFFFFF));
            entries.add(can);
        }
        // What the machines eat: generator fuel, refinery catalysts and organic matter, crude by the bucket.
        for (Item item : new Item[]{
                net.minecraft.item.Items.COAL, net.minecraft.item.Items.CHARCOAL, net.minecraft.item.Items.COAL_BLOCK,
                net.minecraft.item.Items.LAVA_BUCKET, net.minecraft.item.Items.REDSTONE,
                net.minecraft.item.Items.BLAZE_POWDER, net.minecraft.item.Items.GLOWSTONE_DUST,
                net.minecraft.item.Items.SUGAR_CANE, net.minecraft.item.Items.KELP, net.minecraft.item.Items.WHEAT,
                net.minecraft.item.Items.BAMBOO, net.minecraft.item.Items.CACTUS, net.minecraft.item.Items.MELON_SLICE,
                net.minecraft.item.Items.COPPER_INGOT, net.minecraft.item.Items.IRON_INGOT,
                net.minecraft.item.Items.DRIED_KELP, net.minecraft.item.Items.BUCKET,
                net.minecraft.item.Items.COMPARATOR}) {
            entries.add(item);
        }
    }
}
