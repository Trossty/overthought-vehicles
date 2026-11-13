package me.trossty.car.registry;

import me.trossty.car.CarMain;
import me.trossty.car.block.entity.FuelPipeBlockEntity;
import me.trossty.car.block.entity.FuelPumpBlockEntity;
import me.trossty.car.block.entity.FuelRefineryBlockEntity;
import me.trossty.car.block.entity.FuelTankBlockEntity;
import me.trossty.car.block.entity.FuelValveBlockEntity;
import me.trossty.car.block.entity.VehicleLiftBlockEntity;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.level.block.entity.BlockEntityType;

public final class ModBlockEntities {

    public static final BlockEntityType<FuelRefineryBlockEntity> FUEL_REFINERY = Registry.register(
            BuiltInRegistries.BLOCK_ENTITY_TYPE,
            CarMain.id("fuel_refinery"),
            BlockEntityType.Builder.of(FuelRefineryBlockEntity::new, ModBlocks.FUEL_REFINERY).build(null));

    public static final BlockEntityType<FuelPumpBlockEntity> FUEL_PUMP = Registry.register(
            BuiltInRegistries.BLOCK_ENTITY_TYPE,
            CarMain.id("fuel_pump"),
            BlockEntityType.Builder.of(FuelPumpBlockEntity::new, ModBlocks.FUEL_PUMP).build(null));

    public static final BlockEntityType<FuelPipeBlockEntity> FUEL_PIPE = Registry.register(
            BuiltInRegistries.BLOCK_ENTITY_TYPE,
            CarMain.id("fuel_pipe"),
            BlockEntityType.Builder.of(FuelPipeBlockEntity::new, ModBlocks.FUEL_PIPE).build(null));

    public static final BlockEntityType<me.trossty.car.block.entity.EnergyCableBlockEntity> ENERGY_CABLE = Registry.register(
            BuiltInRegistries.BLOCK_ENTITY_TYPE,
            CarMain.id("energy_cable"),
            BlockEntityType.Builder.of(me.trossty.car.block.entity.EnergyCableBlockEntity::new,
                    ModBlocks.ENERGY_CABLE).build(null));

    public static final BlockEntityType<FuelTankBlockEntity> FUEL_TANK = Registry.register(
            BuiltInRegistries.BLOCK_ENTITY_TYPE,
            CarMain.id("fuel_tank"),
            BlockEntityType.Builder.of(FuelTankBlockEntity::new, ModBlocks.FUEL_TANK).build(null));

    public static final BlockEntityType<me.trossty.car.block.entity.FuelDrumBlockEntity> FUEL_DRUM = Registry.register(
            BuiltInRegistries.BLOCK_ENTITY_TYPE,
            CarMain.id("fuel_drum"),
            BlockEntityType.Builder.of(me.trossty.car.block.entity.FuelDrumBlockEntity::new,
                    ModBlocks.FUEL_DRUM).build(null));

    public static final BlockEntityType<FuelValveBlockEntity> FUEL_VALVE = Registry.register(
            BuiltInRegistries.BLOCK_ENTITY_TYPE,
            CarMain.id("fuel_valve"),
            BlockEntityType.Builder.of(FuelValveBlockEntity::new, ModBlocks.FUEL_VALVE).build(null));

    public static final BlockEntityType<me.trossty.car.block.entity.DrainPanBlockEntity> DRAIN_PAN = Registry.register(
            BuiltInRegistries.BLOCK_ENTITY_TYPE,
            CarMain.id("drain_pan"),
            BlockEntityType.Builder.of(me.trossty.car.block.entity.DrainPanBlockEntity::new,
                    ModBlocks.DRAIN_PAN).build(null));

    public static final BlockEntityType<me.trossty.car.block.entity.ToolboxBlockEntity> TOOLBOX = Registry.register(
            BuiltInRegistries.BLOCK_ENTITY_TYPE,
            CarMain.id("toolbox"),
            BlockEntityType.Builder.of(me.trossty.car.block.entity.ToolboxBlockEntity::new,
                    ModBlocks.TOOLBOX).build(null));

    public static final BlockEntityType<me.trossty.car.block.entity.EngineStandBlockEntity> ENGINE_STAND =
            Registry.register(BuiltInRegistries.BLOCK_ENTITY_TYPE, CarMain.id("engine_stand"),
                    BlockEntityType.Builder.of(me.trossty.car.block.entity.EngineStandBlockEntity::new,
                            ModBlocks.ENGINE_STAND).build(null));

    public static final BlockEntityType<me.trossty.car.block.entity.ServiceStandBlockEntity> SERVICE_STAND =
            Registry.register(BuiltInRegistries.BLOCK_ENTITY_TYPE, CarMain.id("service_stand"),
                    BlockEntityType.Builder.of(me.trossty.car.block.entity.ServiceStandBlockEntity::new,
                            ModBlocks.BOAT_CRADLE, ModBlocks.AIRCRAFT_JACK).build(null));

    public static final BlockEntityType<VehicleLiftBlockEntity> VEHICLE_LIFT = Registry.register(
            BuiltInRegistries.BLOCK_ENTITY_TYPE,
            CarMain.id("vehicle_lift"),
            BlockEntityType.Builder.of(VehicleLiftBlockEntity::new, ModBlocks.VEHICLE_LIFT).build(null));

    public static final BlockEntityType<me.trossty.car.block.entity.GeneratorBlockEntity> GENERATOR =
            Registry.register(
                    BuiltInRegistries.BLOCK_ENTITY_TYPE,
                    CarMain.id("generator"),
                    BlockEntityType.Builder.of(
                            me.trossty.car.block.entity.GeneratorBlockEntity::new,
                            ModBlocks.GENERATOR, ModBlocks.BIOFUEL_GENERATOR, ModBlocks.DIESEL_GENERATOR).build(null));

    public static final BlockEntityType<me.trossty.car.block.entity.HydrotreaterBlockEntity> HYDROTREATER =
            Registry.register(
                    BuiltInRegistries.BLOCK_ENTITY_TYPE,
                    CarMain.id("hydrotreater"),
                    BlockEntityType.Builder.of(
                            me.trossty.car.block.entity.HydrotreaterBlockEntity::new,
                            ModBlocks.HYDROTREATER).build(null));

    public static final BlockEntityType<me.trossty.car.block.entity.PumpjackBlockEntity> PUMPJACK =
            Registry.register(
                    BuiltInRegistries.BLOCK_ENTITY_TYPE,
                    CarMain.id("pumpjack"),
                    BlockEntityType.Builder.of(
                            me.trossty.car.block.entity.PumpjackBlockEntity::new,
                            ModBlocks.PUMPJACK).build(null));

    public static final BlockEntityType<me.trossty.car.block.entity.BatteryBankBlockEntity> BATTERY_BANK =
            Registry.register(
                    BuiltInRegistries.BLOCK_ENTITY_TYPE,
                    CarMain.id("battery_bank"),
                    BlockEntityType.Builder.of(
                            me.trossty.car.block.entity.BatteryBankBlockEntity::new,
                            ModBlocks.BATTERY_BANK).build(null));

    public static final BlockEntityType<me.trossty.car.block.entity.ChargingPostBlockEntity> CHARGING_POST =
            Registry.register(
                    BuiltInRegistries.BLOCK_ENTITY_TYPE,
                    CarMain.id("charging_post"),
                    BlockEntityType.Builder.of(
                            me.trossty.car.block.entity.ChargingPostBlockEntity::new,
                            ModBlocks.CHARGING_POST).build(null));

    private ModBlockEntities() {
    }

    /** Forces class loading so the static fields above are registered. */
    public static void register() {
    }
}
