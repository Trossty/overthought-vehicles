package me.trossty.car.registry;

import me.trossty.car.CarMain;
import me.trossty.car.fluid.TankOnlyFluid;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.level.material.Fluid;

/**
 * Petrol, and the door it comes and goes through.
 *
 * <p>This is the whole of what makes the fuel yard something another mod's
 * pipework can join on to. Create's pipes and pumps, and everything else on
 * Fabric that moves liquid, ask the block next door one question — "have you
 * a fluid storage on this face?" — and a tank that answers it is plumbed in
 * without either mod having heard of the other.
 */
public final class ModFluids {

    public static final Fluid PETROL = tankOnly("petrol");

    /** The other fuel, for the engines that light themselves. */
    public static final Fluid DIESEL = tankOnly("diesel");

    /**
     * Milk and honey: liquids Minecraft keeps only in buckets and bottles. As
     * fluids they can go in a tanker, and come back out into a bucket or a
     * bottle at the other end.
     */
    public static final Fluid MILK = tankOnly("milk", net.minecraft.world.item.Items.MILK_BUCKET);
    public static final Fluid HONEY = tankOnly("honey");

    /**
     * The production line's own liquids. Crude oil is what a pumpjack draws
     * out of a deposit; the refinery cracks it into raw petrol or raw diesel,
     * which no engine will run on until the hydrotreater has cleaned it.
     * Biofuel comes from the other end of the refinery, out of organic matter,
     * and is ready to burn as it is.
     */
    public static final Fluid CRUDE_OIL = tankOnly("crude_oil");
    public static final Fluid RAW_PETROL = tankOnly("raw_petrol");
    public static final Fluid RAW_DIESEL = tankOnly("raw_diesel");
    public static final Fluid BIOFUEL = tankOnly("biofuel");

    /** A honey bottle is a quarter of a honey block, so a quarter bucket. */
    public static final int HONEY_BOTTLE = 250;

    /**
     * One of this mod's petrol units in the transfer API's own measure. A car's
     * tank is a thousand units, so a unit is a millibucket and a car holds one
     * bucket — which is the size other mods will expect a bucket to be.
     */
    public static final int UNIT = 1;

    private ModFluids() {
    }

    private static Fluid tankOnly(String name) {
        return tankOnly(name, net.minecraft.world.item.Items.AIR);
    }

    private static Fluid tankOnly(String name, net.minecraft.world.item.Item bucket) {
        TankOnlyFluid fluid = Registry.register(BuiltInRegistries.FLUID, CarMain.id(name), new TankOnlyFluid(name, bucket));
        Registry.register(net.neoforged.neoforge.registries.NeoForgeRegistries.FLUID_TYPES, CarMain.id(name),
                fluid.getFluidType());
        return fluid;
    }

    /** The fuel a fluid is, or null for anything that will not run an engine. */
    public static me.trossty.car.entity.CarSetup.@org.jetbrains.annotations.Nullable Fuel fuelOf(Fluid fluid) {
        if (fluid == PETROL) {
            return me.trossty.car.entity.CarSetup.Fuel.PETROL;
        }
        if (fluid == DIESEL) {
            return me.trossty.car.entity.CarSetup.Fuel.DIESEL;
        }
        if (fluid == BIOFUEL) {
            return me.trossty.car.entity.CarSetup.Fuel.BIOFUEL;
        }
        return null;
    }

    /** The fluid a fuel is. */
    public static Fluid fluidOf(me.trossty.car.entity.CarSetup.Fuel fuel) {
        return switch (fuel) {
            case DIESEL -> DIESEL;
            case BIOFUEL -> BIOFUEL;
            default -> PETROL;
        };
    }

    /** Forces class loading so the static fields above are registered. */
    public static void register() {
    }

    /**
     * Every liquid door of the mod's blocks, and the containers the mod pours
     * from, through NeoForge's capabilities: other mods' pipes, cables and
     * buckets reach them the same way the mod's own blocks do.
     */
    public static void registerCapabilities(net.neoforged.neoforge.capabilities.RegisterCapabilitiesEvent event) {
        var fluids = net.neoforged.neoforge.capabilities.Capabilities.FluidHandler.BLOCK;
        var energy = net.neoforged.neoforge.capabilities.Capabilities.EnergyStorage.BLOCK;
        var items = net.neoforged.neoforge.capabilities.Capabilities.FluidHandler.ITEM;

        // A milk bucket and a honey bottle are containers of those two, both
        // ways. An empty bucket fills with milk by itself: milk's bucket is the
        // milk bucket.
        event.registerItem(items, (stack, context) -> new me.trossty.car.fluid.FixedContainer(
                stack, net.minecraft.world.item.Items.BUCKET, MILK, 1000), net.minecraft.world.item.Items.MILK_BUCKET);
        event.registerItem(items, (stack, context) -> new me.trossty.car.fluid.FixedContainer(
                stack, net.minecraft.world.item.Items.GLASS_BOTTLE, HONEY, HONEY_BOTTLE), net.minecraft.world.item.Items.HONEY_BOTTLE);
        event.registerItem(items, (stack, context) -> me.trossty.car.fluid.FixedContainer.empty(
                stack, net.minecraft.world.item.Items.HONEY_BOTTLE, HONEY, HONEY_BOTTLE), net.minecraft.world.item.Items.GLASS_BOTTLE);

        event.registerBlockEntity(fluids, ModBlockEntities.FUEL_TANK, (tank, side) -> tank.getFluidStorage());
        event.registerBlockEntity(fluids, ModBlockEntities.FUEL_DRUM, (drum, side) -> drum.getFluidStorage());

        // The production line, every cell of every machine: crude into a
        // refinery and its product out, raw fuel into a hydrotreater and clean
        // fuel out, crude out of a pumpjack, liquid fuel into a generator.
        event.registerBlock(fluids, (world, pos, state, blockEntity, side) ->
                me.trossty.car.block.MultiblockBlock.controllerEntity(world, pos)
                        instanceof me.trossty.car.block.entity.FuelRefineryBlockEntity refinery
                        ? refinery.getFluidStorage() : null, ModBlocks.FUEL_REFINERY);
        event.registerBlock(fluids, (world, pos, state, blockEntity, side) ->
                me.trossty.car.block.MultiblockBlock.controllerEntity(world, pos)
                        instanceof me.trossty.car.block.entity.HydrotreaterBlockEntity plant
                        ? plant.getFluidStorage() : null, ModBlocks.HYDROTREATER);
        event.registerBlock(fluids, (world, pos, state, blockEntity, side) ->
                me.trossty.car.block.MultiblockBlock.controllerEntity(world, pos)
                        instanceof me.trossty.car.block.entity.PumpjackBlockEntity jack
                        ? jack.getFluidStorage() : null, ModBlocks.PUMPJACK);
        event.registerBlockEntity(fluids, ModBlockEntities.GENERATOR, (generator, side) ->
                generator.getKind().burnsLiquid() ? generator.getTank() : null);

        // ...and the station's own plumbing is open to other mods' pipes all
        // the way along: a Create pipe can feed a pump, draw off a line, or
        // stand in for a run of this mod's pipe entirely.
        event.registerBlockEntity(fluids, ModBlockEntities.FUEL_PIPE, (pipe, side) -> pipe.getFluidStorage());
        event.registerBlockEntity(fluids, ModBlockEntities.FUEL_VALVE, (valve, side) -> valve.getFluidStorage());
        // Either half of a pump: the upper half has no block entity of its own.
        event.registerBlock(fluids, (world, pos, state, blockEntity, side) -> {
            var pump = me.trossty.car.block.FuelPumpBlock.tankAt(world, pos);
            return pump == null ? null : pump.getFluidStorage();
        }, ModBlocks.FUEL_PUMP);

        // ...and another mod's cable can power one, or be fed by a generator.
        event.registerBlock(energy, (world, pos, state, blockEntity, side) ->
                me.trossty.car.block.MultiblockBlock.controllerEntity(world, pos)
                        instanceof me.trossty.car.block.entity.FuelRefineryBlockEntity refinery
                        ? refinery.getEnergyStorage() : null, ModBlocks.FUEL_REFINERY);
        event.registerBlock(energy, (world, pos, state, blockEntity, side) ->
                me.trossty.car.block.MultiblockBlock.controllerEntity(world, pos)
                        instanceof me.trossty.car.block.entity.HydrotreaterBlockEntity plant
                        ? plant.getEnergyStorage() : null, ModBlocks.HYDROTREATER);
        event.registerBlock(energy, (world, pos, state, blockEntity, side) ->
                me.trossty.car.block.MultiblockBlock.controllerEntity(world, pos)
                        instanceof me.trossty.car.block.entity.PumpjackBlockEntity jack
                        ? jack.getEnergyStorage() : null, ModBlocks.PUMPJACK);
        event.registerBlockEntity(energy, ModBlockEntities.GENERATOR, (generator, side) -> generator.getEnergyStorage());
        event.registerBlockEntity(energy, ModBlockEntities.ENERGY_CABLE, (cable, side) -> cable.getEnergyStorage());
        event.registerBlockEntity(energy, ModBlockEntities.BATTERY_BANK,
                (bank, side) -> bank.sideStorage(side == null ? net.minecraft.core.Direction.UP : side));
        event.registerBlockEntity(energy, ModBlockEntities.CHARGING_POST, (post, side) -> post.getEnergyStorage());
    }
}
