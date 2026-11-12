package me.trossty.car.registry;

import me.trossty.car.CarMain;
import me.trossty.car.block.FuelPipeBlock;
import me.trossty.car.block.FuelPumpBlock;
import me.trossty.car.block.FuelRefineryBlock;
import me.trossty.car.block.FuelTankBlock;
import me.trossty.car.block.FuelValveBlock;
import me.trossty.car.block.VehicleLiftBlock;
import me.trossty.car.block.VehicleWorkbenchBlock;
import net.minecraft.block.AbstractBlock;
import net.minecraft.block.Block;
import net.minecraft.item.BlockItem;
import net.minecraft.item.Item;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.sound.BlockSoundGroup;

public final class ModBlocks {

    public static final Block VEHICLE_WORKBENCH = register(
            "vehicle_workbench",
            VehicleWorkbenchBlock::new,
            AbstractBlock.Settings.create().strength(3.0F, 6.0F).requiresTool().sounds(BlockSoundGroup.METAL));

    /** The ramp cars are assembled on, and the only place they can be repaired. */
    public static final Block VEHICLE_LIFT = register(
            "vehicle_lift",
            VehicleLiftBlock::new,
            AbstractBlock.Settings.create().strength(3.0F, 6.0F).requiresTool().sounds(BlockSoundGroup.METAL));

    /** The plant that cracks biomass and crude. Its boiler house glows while it works. */
    public static final Block FUEL_REFINERY = register(
            "fuel_refinery",
            FuelRefineryBlock::new,
            AbstractBlock.Settings.create()
                    .strength(3.5F, 6.0F)
                    .requiresTool()
                    .nonOpaque()
                    .sounds(BlockSoundGroup.METAL)
                    .luminance(state -> state.get(FuelRefineryBlock.LIT)
                            && state.get(FuelRefineryBlock.PART) == 0 ? 13 : 0));

    /** Cleans raw petrol and raw diesel into fuel an engine will take. */
    public static final Block HYDROTREATER = register(
            "hydrotreater",
            me.trossty.car.block.HydrotreaterBlock::new,
            AbstractBlock.Settings.create()
                    .strength(3.5F, 6.0F)
                    .requiresTool()
                    .nonOpaque()
                    .sounds(BlockSoundGroup.METAL));

    /** The nodding donkey: brings crude up out of an oil deposit. */
    public static final Block PUMPJACK = register(
            "pumpjack",
            me.trossty.car.block.PumpjackBlock::new,
            AbstractBlock.Settings.create()
                    .strength(3.5F, 6.0F)
                    .requiresTool()
                    .nonOpaque()
                    .sounds(BlockSoundGroup.METAL));

    /** Oil-bearing rock, deep down. A pumpjack over it drains it block by block. */
    public static final Block OIL_DEPOSIT = register(
            "oil_deposit",
            Block::new,
            AbstractBlock.Settings.create()
                    .mapColor(net.minecraft.block.MapColor.BLACK)
                    .strength(2.5F, 6.0F)
                    .requiresTool()
                    .sounds(BlockSoundGroup.STONE));

    /** Crude at the surface: where to look. */
    public static final Block OIL_SEEP = register(
            "oil_seep",
            me.trossty.car.block.OilSeepBlock::new,
            AbstractBlock.Settings.create()
                    .mapColor(net.minecraft.block.MapColor.BLACK)
                    .strength(0.3F)
                    .replaceable()
                    .noCollision()
                    .nonOpaque()
                    .velocityMultiplier(0.6F)
                    .sounds(BlockSoundGroup.MUD));

    /** Burns fuel for power, for the refinery or anything else on a cable. */
    public static final Block GENERATOR = register(
            "generator",
            settings -> new me.trossty.car.block.GeneratorBlock(
                    me.trossty.car.block.entity.GeneratorBlockEntity.Kind.COAL, settings),
            AbstractBlock.Settings.create()
                    .strength(3.5F, 6.0F)
                    .requiresTool()
                    .nonOpaque()
                    .sounds(BlockSoundGroup.METAL)
                    .luminance(state -> state.get(me.trossty.car.block.GeneratorBlock.LIT) ? 10 : 0));

    /** Runs on biofuel: more power than coal, and it drinks. */
    public static final Block BIOFUEL_GENERATOR = register(
            "biofuel_generator",
            settings -> new me.trossty.car.block.GeneratorBlock(
                    me.trossty.car.block.entity.GeneratorBlockEntity.Kind.BIOFUEL, settings),
            AbstractBlock.Settings.create()
                    .strength(3.5F, 6.0F)
                    .requiresTool()
                    .nonOpaque()
                    .sounds(BlockSoundGroup.METAL)
                    .luminance(state -> state.get(me.trossty.car.block.GeneratorBlock.LIT) ? 6 : 0));

    /** Runs on clean diesel: the most power, the furthest on a tank. */
    public static final Block DIESEL_GENERATOR = register(
            "diesel_generator",
            settings -> new me.trossty.car.block.GeneratorBlock(
                    me.trossty.car.block.entity.GeneratorBlockEntity.Kind.DIESEL, settings),
            AbstractBlock.Settings.create()
                    .strength(3.5F, 6.0F)
                    .requiresTool()
                    .nonOpaque()
                    .sounds(BlockSoundGroup.METAL)
                    .luminance(state -> state.get(me.trossty.car.block.GeneratorBlock.LIT) ? 6 : 0));

    /** Stores power: in on five sides, out of the socket on the front. */
    public static final Block BATTERY_BANK = register(
            "battery_bank",
            me.trossty.car.block.BatteryBankBlock::new,
            AbstractBlock.Settings.create()
                    .strength(3.5F, 6.0F)
                    .requiresTool()
                    .nonOpaque()
                    .sounds(BlockSoundGroup.METAL)
                    .luminance(state -> state.get(me.trossty.car.block.BatteryBankBlock.LEVEL) > 0 ? 3 : 0));

    /** Charges the electric vehicle parked beside it. */
    public static final Block CHARGING_POST = register(
            "charging_post",
            me.trossty.car.block.ChargingPostBlock::new,
            AbstractBlock.Settings.create()
                    .strength(3.0F, 6.0F)
                    .requiresTool()
                    .nonOpaque()
                    .sounds(BlockSoundGroup.METAL)
                    .luminance(state -> state.get(me.trossty.car.block.ChargingPostBlock.LIT) ? 7 : 2));

    /** The forecourt pump: two blocks tall, fed by any refinery it touches. */
    public static final Block FUEL_PUMP = register(
            "fuel_pump",
            FuelPumpBlock::new,
            AbstractBlock.Settings.create()
                    .strength(3.0F, 6.0F)
                    .requiresTool()
                    .nonOpaque()
                    .sounds(BlockSoundGroup.METAL));

    /** The line that carries petrol from a refinery to a pump. */
    public static final Block FUEL_PIPE = register(
            "fuel_pipe",
            FuelPipeBlock::new,
            AbstractBlock.Settings.create()
                    .strength(2.5F, 6.0F)
                    .requiresTool()
                    .nonOpaque()
                    .sounds(BlockSoundGroup.COPPER));

    /** The power line from a generator to the machines. */
    public static final Block ENERGY_CABLE = register(
            "energy_cable",
            me.trossty.car.block.EnergyCableBlock::new,
            AbstractBlock.Settings.create()
                    .strength(0.8F)
                    .nonOpaque()
                    .sounds(BlockSoundGroup.WOOL));

    /** Where petrol waits: the buffer between a refinery and a station. */
    public static final Block FUEL_TANK = register(
            "fuel_tank",
            FuelTankBlock::new,
            AbstractBlock.Settings.create()
                    .strength(3.5F, 9.0F)
                    .requiresTool()
                    .nonOpaque()
                    .sounds(BlockSoundGroup.METAL));

    /** A steel drum of fuel, or of anything else that pours: carried away full when broken. */
    public static final Block FUEL_DRUM = register(
            "fuel_drum",
            me.trossty.car.block.FuelDrumBlock::new,
            AbstractBlock.Settings.create()
                    .strength(2.5F, 6.0F)
                    .requiresTool()
                    .nonOpaque()
                    .sounds(BlockSoundGroup.METAL));

    /** One-way, and faster than levelling: how a line is given a destination. */
    public static final Block FUEL_VALVE = register(
            "fuel_valve",
            FuelValveBlock::new,
            AbstractBlock.Settings.create()
                    .strength(2.5F, 6.0F)
                    .requiresTool()
                    .nonOpaque()
                    .sounds(BlockSoundGroup.COPPER));

    /** Where whole vehicles are built: pick one, pick a paint, build it. */
    public static final Block VEHICLE_WORKSTATION = register(
            "vehicle_workstation",
            me.trossty.car.block.VehicleWorkstationBlock::new,
            AbstractBlock.Settings.create().strength(3.0F, 6.0F).requiresTool().nonOpaque()
                    .sounds(BlockSoundGroup.METAL));

    /**
     * Road made from the refinery's leftovers: tar bound with gravel and sand,
     * laid a layer at a time like snow and painted with road paint. Plain
     * stone would do for a car to drive on; a laid road is kinder to tyres and
     * steering, which is the point of laying one.
     */
    public static final Block ASPHALT = register(
            "asphalt",
            me.trossty.car.block.AsphaltBlock::new,
            AbstractBlock.Settings.create()
                    .mapColor(net.minecraft.block.MapColor.BLACK)
                    .strength(1.5F, 6.0F)
                    .requiresTool()
                    .sounds(BlockSoundGroup.STONE));

    /** Whether this is laid road. */
    public static boolean isAsphalt(net.minecraft.block.BlockState state) {
        return state.isOf(ASPHALT);
    }

    private ModBlocks() {
    }

    private static Block register(String name,
                                  java.util.function.Function<AbstractBlock.Settings, Block> factory,
                                  AbstractBlock.Settings settings) {
        RegistryKey<Block> blockKey = RegistryKey.of(RegistryKeys.BLOCK, CarMain.id(name));
        Block block = Registry.register(
                Registries.BLOCK, blockKey, factory.apply(settings.registryKey(blockKey)));

        // Every block here is meant to be carried, so it always gets an item.
        RegistryKey<Item> itemKey = RegistryKey.of(RegistryKeys.ITEM, CarMain.id(name));
        Registry.register(Registries.ITEM, itemKey,
                new BlockItem(block, new Item.Settings().registryKey(itemKey).useBlockPrefixedTranslationKey()));
        return block;
    }

    /** Forces class loading so the static fields above are registered. */
    public static void register() {
    }
}
