package me.trossty.car.fluid;

import me.trossty.car.block.FuelPipeBlock;
import me.trossty.car.block.FuelPumpBlock;
import me.trossty.car.block.HydrotreaterBlock;
import me.trossty.car.block.FuelTankBlock;
import me.trossty.car.block.FuelValveBlock;
import me.trossty.car.registry.ModFluids;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;

/**
 * The other side of the bargain: this mod's pipes and valves working with
 * another mod's tanks, machines and pipes, through NeoForge's fluid capability.
 */
public final class ForeignFluids {

    private ForeignFluids() {
    }

    /** Whether a block is one of this mod's own petrol blocks, which are plumbed directly. */
    public static boolean isOurs(Block block) {
        return block instanceof FuelPipeBlock || block instanceof FuelValveBlock
                || block instanceof FuelPumpBlock || block instanceof FuelTankBlock
                || block instanceof HydrotreaterBlock;
    }

    /**
     * Another mod's fluid storage on the far side of {@code side}, or null. Our
     * own blocks are left out: they are reached by their own code.
     */
    public static IFluidHandler at(Level world, BlockPos pos, Direction side) {
        BlockPos next = pos.relative(side);
        if (isOurs(world.getBlockState(next).getBlock())) {
            return null;
        }
        return world.getCapability(Capabilities.FluidHandler.BLOCK, next, side.getOpposite());
    }

    /** Whether a pipe should reach out to whatever is at {@code pos}: anything with a fluid door on that face. */
    public static boolean connects(LevelReader view, BlockPos pos, Direction from) {
        return view instanceof Level world
                && !world.isClientSide()
                && world.getCapability(Capabilities.FluidHandler.BLOCK, pos, from.getOpposite()) != null;
    }

    /** Moves up to {@code units} of petrol from one storage to another; reports units moved. */
    public static int move(IFluidHandler from, IFluidHandler to, int units) {
        return Liquids.move(from, to, ModFluids.PETROL, units);
    }
}
