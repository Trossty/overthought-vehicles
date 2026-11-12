package me.trossty.car.fluid;

import me.trossty.car.block.FuelPipeBlock;
import me.trossty.car.block.FuelPumpBlock;
import me.trossty.car.block.HydrotreaterBlock;
import me.trossty.car.block.FuelTankBlock;
import me.trossty.car.block.FuelValveBlock;
import me.trossty.car.registry.ModFluids;
import net.fabricmc.fabric.api.transfer.v1.fluid.FluidStorage;
import net.fabricmc.fabric.api.transfer.v1.fluid.FluidVariant;
import net.fabricmc.fabric.api.transfer.v1.storage.Storage;
import net.fabricmc.fabric.api.transfer.v1.storage.StorageUtil;
import net.minecraft.block.Block;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.world.World;
import net.minecraft.world.WorldView;

/**
 * The other side of the bargain: this mod's pipes and valves working with
 * another mod's tanks, machines and pipes, through Fabric's transfer API.
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
    public static Storage<FluidVariant> at(World world, BlockPos pos, Direction side) {
        BlockPos next = pos.offset(side);
        if (isOurs(world.getBlockState(next).getBlock())) {
            return null;
        }
        return FluidStorage.SIDED.find(world, next, side.getOpposite());
    }

    /** Whether a pipe should reach out to whatever is at {@code pos}: anything with a fluid door on that face. */
    public static boolean connects(WorldView view, BlockPos pos, Direction from) {
        return view instanceof World world
                && !world.isClient()
                && FluidStorage.SIDED.find(world, pos, from.getOpposite()) != null;
    }

    /** Moves up to {@code units} of petrol from one storage to another; reports units moved. */
    public static int move(Storage<FluidVariant> from, Storage<FluidVariant> to, int units) {
        if (units <= 0) {
            return 0;
        }
        long moved = StorageUtil.move(from, to, variant -> variant.isOf(ModFluids.PETROL),
                units * ModFluids.UNIT, null);
        return (int) (moved / ModFluids.UNIT);
    }
}
