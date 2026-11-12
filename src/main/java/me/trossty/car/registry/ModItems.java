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

    /** The mod's own tab in the creative inventory, with a car on it. */
    public static final ResourceKey<net.minecraft.world.item.CreativeModeTab> GROUP =
            ResourceKey.create(Registries.CREATIVE_MODE_TAB, CarMain.id("vehicles"));

    /** Just enough of a tab's entries to add stacks and items to. */
    private interface Entries {
        void add(net.minecraft.world.item.ItemStack stack);

        default void add(net.minecraft.world.level.ItemLike item) {
            this.add(new net.minecraft.world.item.ItemStack(item));
        }
    }

    /** The testing tab: every item the mod has, every kind of each, and what the machines eat. */
    public static final ResourceKey<net.minecraft.world.item.CreativeModeTab> TEST_GROUP =
            ResourceKey.create(Registries.CREATIVE_MODE_TAB, CarMain.id("everything"));

    public static void register() {
        Registry.register(BuiltInRegistries.RECIPE_SERIALIZER, CarMain.id("spray_can_refill"),
                me.trossty.car.recipe.SprayCanRefillRecipe.SERIALIZER);
        Registry.register(BuiltInRegistries.CREATIVE_MODE_TAB, GROUP,
                net.minecraft.world.item.CreativeModeTab.builder()
                        .icon(() -> new net.minecraft.world.item.ItemStack(CAR))
                        .title(net.minecraft.network.chat.Component.translatable("itemGroup.car.vehicles"))
                        .displayItems(ModItems::vehicles)
                        .build());
        Registry.register(BuiltInRegistries.CREATIVE_MODE_TAB, TEST_GROUP,
                net.minecraft.world.item.CreativeModeTab.builder()
                        .icon(() -> new net.minecraft.world.item.ItemStack(ModBlocks.FUEL_REFINERY))
                        .title(net.minecraft.network.chat.Component.translatable("itemGroup.car.everything"))
                        .displayItems(ModItems::everything)
                        .build());
    }

    /** The vehicles tab: every vehicle the data packs describe, then parts, tools and blocks. */
    private static void vehicles(net.minecraft.world.item.CreativeModeTab.ItemDisplayParameters context,
                                 net.minecraft.world.item.CreativeModeTab.Output entries) {
        {
            // Every vehicle the loaded data packs describe, as it leaves the factory.
            context.holders()
                    .lookup(me.trossty.car.vehicle.VehicleDefinition.KEY)
                    .ifPresent(registry -> registry.listElements().forEach(entry ->
                            entries.accept(me.trossty.car.item.CarItem.stackOf(
                                    entry.key().location(), entry.value()))));
            entries.accept(JERRY_CAN);
            entries.accept(CAR_KEY);
            entries.accept(SPRAY_CAN);
            for (net.minecraft.world.item.DyeColor dye : net.minecraft.world.item.DyeColor.values()) {
                net.minecraft.world.item.ItemStack can = new net.minecraft.world.item.ItemStack(SPRAY_CAN);
                can.set(net.minecraft.core.component.DataComponents.DYED_COLOR,
                        new net.minecraft.world.item.component.DyedItemColor(dye.getTextureDiffuseColor() & 0xFFFFFF, true));
                entries.accept(can);
            }
            entries.accept(CAR_BODY);
            entries.accept(DIAGNOSTIC_SHEET);
            entries.accept(OBD_SCANNER);
            // In teardown order, so the group reads the way a car comes apart.
            for (me.trossty.car.entity.CarPart part : me.trossty.car.entity.CarPart.values()) {
                entries.accept(part.item());
            }
            // Then every other kind of the parts that come in kinds.
            for (me.trossty.car.entity.CarPart part : me.trossty.car.entity.CarPart.values()) {
                for (int variant = 1;
                     variant < me.trossty.car.entity.CarSetup.variantsOf(part); variant++) {
                    net.minecraft.world.item.ItemStack stack = new net.minecraft.world.item.ItemStack(part.item());
                    stack.set(ModComponents.PART_VARIANT, variant);
                    entries.accept(stack);
                }
            }
            entries.accept(TURBO);
            net.minecraft.world.item.ItemStack diesel = new net.minecraft.world.item.ItemStack(JERRY_CAN);
            diesel.set(ModComponents.FUEL_KIND, 1);
            entries.accept(diesel);
            net.minecraft.world.item.ItemStack bio = new net.minecraft.world.item.ItemStack(JERRY_CAN);
            bio.set(ModComponents.FUEL_KIND, 2);
            entries.accept(bio);
            entries.accept(TANKER_BARREL);
            entries.accept(PICKUP_BED);
            entries.accept(ModBlocks.VEHICLE_WORKSTATION);
            entries.accept(ModBlocks.VEHICLE_WORKBENCH);
            entries.accept(ModBlocks.VEHICLE_LIFT);
            entries.accept(ModBlocks.PUMPJACK);
            entries.accept(ModBlocks.FUEL_REFINERY);
            entries.accept(ModBlocks.HYDROTREATER);
            entries.accept(ModBlocks.GENERATOR);
            entries.accept(ModBlocks.BIOFUEL_GENERATOR);
            entries.accept(ModBlocks.DIESEL_GENERATOR);
            entries.accept(ModBlocks.ENERGY_CABLE);
            entries.accept(ModBlocks.BATTERY_BANK);
            net.minecraft.world.item.ItemStack charged = new net.minecraft.world.item.ItemStack(ModBlocks.BATTERY_BANK);
            charged.set(ModComponents.STORED_ENERGY, me.trossty.car.block.entity.BatteryBankBlockEntity.CAPACITY);
            entries.accept(charged);
            entries.accept(ModBlocks.CHARGING_POST);
            entries.accept(ModBlocks.OIL_DEPOSIT);
            entries.accept(ModBlocks.OIL_SEEP);
            entries.accept(ModBlocks.FUEL_PUMP);
            entries.accept(ModBlocks.FUEL_PIPE);
            entries.accept(ModBlocks.FUEL_VALVE);
            entries.accept(ModBlocks.FUEL_TANK);
            entries.accept(ModBlocks.FUEL_DRUM);
            entries.accept(TAR);
            entries.accept(ModBlocks.ASPHALT);
            entries.accept(WHITE_ROAD_PAINT);
            entries.accept(YELLOW_ROAD_PAINT);
            entries.accept(RED_ROAD_PAINT);
            entries.accept(BLUE_ROAD_PAINT);
        }
    }

    /**
     * The testing tab's contents, so nothing has to be crafted to try it:
     * every vehicle, every item and block the mod registers (read from the
     * registry, so a new one turns up without being listed), each kind of the
     * things that come in kinds, full and empty cans of every fuel, and the
     * vanilla things the machines run on.
     */
    private static void everything(net.minecraft.world.item.CreativeModeTab.ItemDisplayParameters context,
                                   net.minecraft.world.item.CreativeModeTab.Output group) {
        // A tab refuses the same stack twice, and a default can may well equal a
        // full one, so everything goes through here first.
        java.util.List<net.minecraft.world.item.ItemStack> added = new java.util.ArrayList<>();
        Entries entries = new Entries() {
            @Override
            public void add(net.minecraft.world.item.ItemStack stack) {
                for (net.minecraft.world.item.ItemStack there : added) {
                    if (net.minecraft.world.item.ItemStack.isSameItemSameComponents(there, stack)) {
                        return;
                    }
                }
                added.add(stack);
                group.accept(stack);
            }
        };
        context.holders().lookup(me.trossty.car.vehicle.VehicleDefinition.KEY)
                .ifPresent(registry -> registry.listElements().forEach(entry ->
                        entries.add(me.trossty.car.item.CarItem.stackOf(
                                entry.key().location(), entry.value()))));
        // Every item of ours, in the order they were registered.
        for (Item item : BuiltInRegistries.ITEM) {
            if (BuiltInRegistries.ITEM.getKey(item).getNamespace().equals(CarMain.MOD_ID) && item != CAR) {
                entries.add(item);
            }
        }
        // Cans of every fuel, full and empty.
        for (int kind = 0; kind < 3; kind++) {
            net.minecraft.world.item.ItemStack full = new net.minecraft.world.item.ItemStack(JERRY_CAN);
            full.set(ModComponents.FUEL_KIND, kind);
            me.trossty.car.item.JerryCans.setUnits(full, me.trossty.car.item.JerryCans.CAPACITY);
            entries.add(full);
        }
        net.minecraft.world.item.ItemStack empty = new net.minecraft.world.item.ItemStack(JERRY_CAN);
        me.trossty.car.item.JerryCans.setUnits(empty, 0);
        entries.add(empty);
        // Every other kind of the parts that come in kinds, and every paint.
        for (me.trossty.car.entity.CarPart part : me.trossty.car.entity.CarPart.values()) {
            for (int variant = 1; variant < me.trossty.car.entity.CarSetup.variantsOf(part); variant++) {
                net.minecraft.world.item.ItemStack stack = new net.minecraft.world.item.ItemStack(part.item());
                stack.set(ModComponents.PART_VARIANT, variant);
                entries.add(stack);
            }
        }
        for (net.minecraft.world.item.DyeColor dye : net.minecraft.world.item.DyeColor.values()) {
            net.minecraft.world.item.ItemStack can = new net.minecraft.world.item.ItemStack(SPRAY_CAN);
            can.set(net.minecraft.core.component.DataComponents.DYED_COLOR,
                    new net.minecraft.world.item.component.DyedItemColor(dye.getTextureDiffuseColor() & 0xFFFFFF, true));
            entries.add(can);
        }
        // What the machines eat: generator fuel, refinery catalysts and organic matter, crude by the bucket.
        for (Item item : new Item[]{
                net.minecraft.world.item.Items.COAL, net.minecraft.world.item.Items.CHARCOAL, net.minecraft.world.item.Items.COAL_BLOCK,
                net.minecraft.world.item.Items.LAVA_BUCKET, net.minecraft.world.item.Items.REDSTONE,
                net.minecraft.world.item.Items.BLAZE_POWDER, net.minecraft.world.item.Items.GLOWSTONE_DUST,
                net.minecraft.world.item.Items.SUGAR_CANE, net.minecraft.world.item.Items.KELP, net.minecraft.world.item.Items.WHEAT,
                net.minecraft.world.item.Items.BAMBOO, net.minecraft.world.item.Items.CACTUS, net.minecraft.world.item.Items.MELON_SLICE,
                net.minecraft.world.item.Items.COPPER_INGOT, net.minecraft.world.item.Items.IRON_INGOT,
                net.minecraft.world.item.Items.DRIED_KELP, net.minecraft.world.item.Items.BUCKET,
                net.minecraft.world.item.Items.COMPARATOR}) {
            entries.add(item);
        }
    }
}
