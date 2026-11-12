package me.trossty.car.registry;

import me.trossty.car.CarMain;
import me.trossty.car.block.entity.FuelPipeBlockEntity;
import me.trossty.car.block.entity.FuelPumpBlockEntity;
import me.trossty.car.block.entity.FuelRefineryBlockEntity;
import me.trossty.car.block.entity.FuelTankBlockEntity;
import me.trossty.car.block.entity.FuelValveBlockEntity;
import me.trossty.car.block.entity.VehicleLiftBlockEntity;
import net.fabricmc.fabric.api.object.builder.v1.block.entity.FabricBlockEntityTypeBuilder;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.level.block.entity.BlockEntityType;

public final class ModBlockEntities {

    public static final BlockEntityType<FuelRefineryBlockEntity> FUEL_REFINERY = Registry.register(
            BuiltInRegistries.BLOCK_ENTITY_TYPE,
            CarMain.id("fuel_refinery"),
            FabricBlockEntityTypeBuilder.create(FuelRefineryBlockEntity::new, ModBlocks.FUEL_REFINERY).build());

    public static final BlockEntityType<FuelPumpBlockEntity> FUEL_PUMP = Registry.register(
            BuiltInRegistries.BLOCK_ENTITY_TYPE,
            CarMain.id("fuel_pump"),
            FabricBlockEntityTypeBuilder.create(FuelPumpBlockEntity::new, ModBlocks.FUEL_PUMP).build());

    public static final BlockEntityType<FuelPipeBlockEntity> FUEL_PIPE = Registry.register(
            BuiltInRegistries.BLOCK_ENTITY_TYPE,
            CarMain.id("fuel_pipe"),
            FabricBlockEntityTypeBuilder.create(FuelPipeBlockEntity::new, ModBlocks.FUEL_PIPE).build());

    public static final BlockEntityType<me.trossty.car.block.entity.EnergyCableBlockEntity> ENERGY_CABLE = Registry.register(
            BuiltInRegistries.BLOCK_ENTITY_TYPE,
            CarMain.id("energy_cable"),
            FabricBlockEntityTypeBuilder.create(me.trossty.car.block.entity.EnergyCableBlockEntity::new,
                    ModBlocks.ENERGY_CABLE).build());

    public static final BlockEntityType<FuelTankBlockEntity> FUEL_TANK = Registry.register(
            BuiltInRegistries.BLOCK_ENTITY_TYPE,
            CarMain.id("fuel_tank"),
            FabricBlockEntityTypeBuilder.create(FuelTankBlockEntity::new, ModBlocks.FUEL_TANK).build());

    public static final BlockEntityType<me.trossty.car.block.entity.FuelDrumBlockEntity> FUEL_DRUM = Registry.register(
            BuiltInRegistries.BLOCK_ENTITY_TYPE,
            CarMain.id("fuel_drum"),
            FabricBlockEntityTypeBuilder.create(me.trossty.car.block.entity.FuelDrumBlockEntity::new,
                    ModBlocks.FUEL_DRUM).build());

    public static final BlockEntityType<FuelValveBlockEntity> FUEL_VALVE = Registry.register(
            BuiltInRegistries.BLOCK_ENTITY_TYPE,
            CarMain.id("fuel_valve"),
            FabricBlockEntityTypeBuilder.create(FuelValveBlockEntity::new, ModBlocks.FUEL_VALVE).build());

    public static final BlockEntityType<VehicleLiftBlockEntity> VEHICLE_LIFT = Registry.register(
            BuiltInRegistries.BLOCK_ENTITY_TYPE,
            CarMain.id("vehicle_lift"),
            FabricBlockEntityTypeBuilder.create(VehicleLiftBlockEntity::new, ModBlocks.VEHICLE_LIFT).build());

    public static final BlockEntityType<me.trossty.car.block.entity.GeneratorBlockEntity> GENERATOR =
            Registry.register(
                    BuiltInRegistries.BLOCK_ENTITY_TYPE,
                    CarMain.id("generator"),
                    FabricBlockEntityTypeBuilder.create(
                            me.trossty.car.block.entity.GeneratorBlockEntity::new,
                            ModBlocks.GENERATOR, ModBlocks.BIOFUEL_GENERATOR, ModBlocks.DIESEL_GENERATOR).build());

    public static final BlockEntityType<me.trossty.car.block.entity.HydrotreaterBlockEntity> HYDROTREATER =
            Registry.register(
                    BuiltInRegistries.BLOCK_ENTITY_TYPE,
                    CarMain.id("hydrotreater"),
                    FabricBlockEntityTypeBuilder.create(
                            me.trossty.car.block.entity.HydrotreaterBlockEntity::new,
                            ModBlocks.HYDROTREATER).build());

    public static final BlockEntityType<me.trossty.car.block.entity.PumpjackBlockEntity> PUMPJACK =
            Registry.register(
                    BuiltInRegistries.BLOCK_ENTITY_TYPE,
                    CarMain.id("pumpjack"),
                    FabricBlockEntityTypeBuilder.create(
                            me.trossty.car.block.entity.PumpjackBlockEntity::new,
                            ModBlocks.PUMPJACK).build());

    public static final BlockEntityType<me.trossty.car.block.entity.BatteryBankBlockEntity> BATTERY_BANK =
            Registry.register(
                    BuiltInRegistries.BLOCK_ENTITY_TYPE,
                    CarMain.id("battery_bank"),
                    FabricBlockEntityTypeBuilder.create(
                            me.trossty.car.block.entity.BatteryBankBlockEntity::new,
                            ModBlocks.BATTERY_BANK).build());

    public static final BlockEntityType<me.trossty.car.block.entity.ChargingPostBlockEntity> CHARGING_POST =
            Registry.register(
                    BuiltInRegistries.BLOCK_ENTITY_TYPE,
                    CarMain.id("charging_post"),
                    FabricBlockEntityTypeBuilder.create(
                            me.trossty.car.block.entity.ChargingPostBlockEntity::new,
                            ModBlocks.CHARGING_POST).build());

    private ModBlockEntities() {
    }

    /** Forces class loading so the static fields above are registered. */
    public static void register() {
    }
}
