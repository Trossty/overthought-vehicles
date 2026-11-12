package me.trossty.car.registry;

import me.trossty.car.CarMain;
import me.trossty.car.fluid.TankOnlyFluid;
import net.fabricmc.fabric.api.transfer.v1.fluid.base.EmptyItemFluidStorage;
import net.fabricmc.fabric.api.transfer.v1.fluid.base.FullItemFluidStorage;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.material.Fluid;
import net.fabricmc.fabric.api.transfer.v1.fluid.FluidConstants;
import net.fabricmc.fabric.api.transfer.v1.fluid.FluidStorage;
import net.fabricmc.fabric.api.transfer.v1.fluid.FluidVariant;
import net.fabricmc.fabric.api.transfer.v1.fluid.FluidVariantAttributeHandler;
import net.fabricmc.fabric.api.transfer.v1.fluid.FluidVariantAttributes;

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
    public static final Fluid MILK = tankOnly("milk");
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
    public static final long HONEY_BOTTLE = FluidConstants.BUCKET / 4;

    /**
     * One of this mod's petrol units in the transfer API's own measure. A car's
     * tank is a thousand units, so a unit is a millibucket and a car holds one
     * bucket — which is the size other mods will expect a bucket to be.
     */
    public static final long UNIT = FluidConstants.BUCKET / 1000;

    private ModFluids() {
    }

    private static Fluid tankOnly(String name) {
        return Registry.register(BuiltInRegistries.FLUID, CarMain.id(name), new TankOnlyFluid());
    }

    /** The fuel a fluid is, or null for anything that will not run an engine. */
    public static me.trossty.car.entity.CarSetup.@org.jspecify.annotations.Nullable Fuel fuelOf(Fluid fluid) {
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

    public static void register() {
        named(PETROL, "petrol");
        named(DIESEL, "diesel");
        named(MILK, "milk");
        named(HONEY, "honey");
        named(CRUDE_OIL, "crude_oil");
        named(RAW_PETROL, "raw_petrol");
        named(RAW_DIESEL, "raw_diesel");
        named(BIOFUEL, "biofuel");

        // A milk bucket and a honey bottle are containers of those two, both
        // ways, for anything that moves liquid through the transfer API.
        FluidStorage.combinedItemApiProvider(Items.MILK_BUCKET).register(context ->
                new FullItemFluidStorage(context, Items.BUCKET, FluidVariant.of(MILK), FluidConstants.BUCKET));
        FluidStorage.combinedItemApiProvider(Items.BUCKET).register(context ->
                new EmptyItemFluidStorage(context, Items.MILK_BUCKET, MILK, FluidConstants.BUCKET));
        FluidStorage.combinedItemApiProvider(Items.HONEY_BOTTLE).register(context ->
                new FullItemFluidStorage(context, Items.GLASS_BOTTLE, FluidVariant.of(HONEY), HONEY_BOTTLE));
        FluidStorage.combinedItemApiProvider(Items.GLASS_BOTTLE).register(context ->
                new EmptyItemFluidStorage(context, Items.HONEY_BOTTLE, HONEY, HONEY_BOTTLE));

        FluidStorage.SIDED.registerForBlockEntity(
                (tank, side) -> tank.getFluidStorage(), ModBlockEntities.FUEL_TANK);
        FluidStorage.SIDED.registerForBlockEntity(
                (drum, side) -> drum.getFluidStorage(), ModBlockEntities.FUEL_DRUM);

        // The production line, every cell of every machine: crude into a
        // refinery and its product out, raw fuel into a hydrotreater and clean
        // fuel out, crude out of a pumpjack, liquid fuel into a generator.
        FluidStorage.SIDED.registerForBlocks((world, pos, state, blockEntity, side) ->
                me.trossty.car.block.MultiblockBlock.controllerEntity(world, pos)
                        instanceof me.trossty.car.block.entity.FuelRefineryBlockEntity refinery
                        ? refinery.getFluidStorage() : null, ModBlocks.FUEL_REFINERY);
        FluidStorage.SIDED.registerForBlocks((world, pos, state, blockEntity, side) ->
                me.trossty.car.block.MultiblockBlock.controllerEntity(world, pos)
                        instanceof me.trossty.car.block.entity.HydrotreaterBlockEntity plant
                        ? plant.getFluidStorage() : null, ModBlocks.HYDROTREATER);
        FluidStorage.SIDED.registerForBlocks((world, pos, state, blockEntity, side) ->
                me.trossty.car.block.MultiblockBlock.controllerEntity(world, pos)
                        instanceof me.trossty.car.block.entity.PumpjackBlockEntity jack
                        ? jack.getFluidStorage() : null, ModBlocks.PUMPJACK);
        FluidStorage.SIDED.registerForBlockEntity((generator, side) ->
                generator.getKind().burnsLiquid() ? generator.getTank() : null, ModBlockEntities.GENERATOR);

        // ...and the station's own plumbing is open to other mods' pipes all
        // the way along: a Create pipe can feed a pump, draw off a line, or
        // stand in for a run of this mod's pipe entirely.
        FluidStorage.SIDED.registerForBlockEntity(
                (pipe, side) -> pipe.getFluidStorage(), ModBlockEntities.FUEL_PIPE);
        FluidStorage.SIDED.registerForBlockEntity(
                (valve, side) -> valve.getFluidStorage(), ModBlockEntities.FUEL_VALVE);
        // Either half of a pump: the upper half has no block entity of its own.
        FluidStorage.SIDED.registerForBlocks((world, pos, state, blockEntity, side) -> {
            var pump = me.trossty.car.block.FuelPumpBlock.tankAt(world, pos);
            return pump == null ? null : pump.getFluidStorage();
        }, ModBlocks.FUEL_PUMP);

        // ...and another mod's cable can power one, or be fed by a generator.
        team.reborn.energy.api.EnergyStorage.SIDED.registerForBlocks((world, pos, state, blockEntity, side) ->
                me.trossty.car.block.MultiblockBlock.controllerEntity(world, pos)
                        instanceof me.trossty.car.block.entity.FuelRefineryBlockEntity refinery
                        ? refinery.getEnergyStorage() : null, ModBlocks.FUEL_REFINERY);
        team.reborn.energy.api.EnergyStorage.SIDED.registerForBlocks((world, pos, state, blockEntity, side) ->
                me.trossty.car.block.MultiblockBlock.controllerEntity(world, pos)
                        instanceof me.trossty.car.block.entity.HydrotreaterBlockEntity plant
                        ? plant.getEnergyStorage() : null, ModBlocks.HYDROTREATER);
        team.reborn.energy.api.EnergyStorage.SIDED.registerForBlocks((world, pos, state, blockEntity, side) ->
                me.trossty.car.block.MultiblockBlock.controllerEntity(world, pos)
                        instanceof me.trossty.car.block.entity.PumpjackBlockEntity jack
                        ? jack.getEnergyStorage() : null, ModBlocks.PUMPJACK);
        team.reborn.energy.api.EnergyStorage.SIDED.registerForBlockEntity(
                (generator, side) -> generator.getEnergyStorage(), ModBlockEntities.GENERATOR);
        team.reborn.energy.api.EnergyStorage.SIDED.registerForBlockEntity(
                (cable, side) -> cable.getEnergyStorage(), ModBlockEntities.ENERGY_CABLE);
        team.reborn.energy.api.EnergyStorage.SIDED.registerForBlockEntity(
                (bank, side) -> bank.sideStorage(side == null ? net.minecraft.core.Direction.UP : side),
                ModBlockEntities.BATTERY_BANK);
        team.reborn.energy.api.EnergyStorage.SIDED.registerForBlockEntity(
                (post, side) -> post.getEnergyStorage(), ModBlockEntities.CHARGING_POST);
    }

    private static void named(Fluid fluid, String key) {
        // Without this the fluid would be called "Air" in every other mod's
        // tooltips, after the block it does not have.
        FluidVariantAttributes.register(fluid, new FluidVariantAttributeHandler() {
            @Override
            public Component getName(FluidVariant variant) {
                return Component.translatable("fluid.car." + key);
            }
        });
    }
}
