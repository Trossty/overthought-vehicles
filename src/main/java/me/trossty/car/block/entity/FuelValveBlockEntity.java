package me.trossty.car.block.entity;

import me.trossty.car.block.FuelValveBlock;
import me.trossty.car.fluid.Liquids;
import me.trossty.car.registry.ModBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;

/**
 * The valve: the one place a line pulls from.
 *
 * <p>A line of pipe draws off what machines make on its own, and gives what a
 * tank holds only to a machine that wants it. To move liquid anywhere else —
 * one tank into another, a drum into a line, one line into the next — you put
 * a valve where it should come from. Each tick it draws {@link #PUMP_RATE} out
 * of whatever is behind it and pushes it into whatever is in front, and never
 * the other way.
 *
 * <p>It works on anything with a liquid door — a pipe (which draws on the whole
 * line), a tank, a pump, a machine, another mod's block — and holds nothing
 * itself. Closing the handle or powering it with redstone stops it; with a
 * comparator on a tank that is a routing rule built out of blocks rather than a
 * settings screen.
 */
public class FuelValveBlockEntity extends BlockEntity {

    /** What a valve moves a tick, in millibuckets. */
    public static final int PUMP_RATE = 100;

    public FuelValveBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.FUEL_VALVE, pos, state);
    }

    public static void tick(Level world, BlockPos pos, BlockState state, FuelValveBlockEntity valve) {
        boolean shut = world.hasNeighborSignal(pos);
        if (state.getValue(FuelValveBlock.POWERED) != shut) {
            world.setBlock(pos, state.setValue(FuelValveBlock.POWERED, shut), Block.UPDATE_ALL);
        }
        if (shut || !state.getValue(FuelValveBlock.OPEN)) {
            return;
        }
        Direction facing = state.getValue(FuelValveBlock.FACING);
        IFluidHandler ahead = world.getCapability(Capabilities.FluidHandler.BLOCK,
                pos.relative(facing), facing.getOpposite());
        IFluidHandler behind = world.getCapability(Capabilities.FluidHandler.BLOCK,
                pos.relative(facing.getOpposite()), facing);
        if (ahead != null && behind != null) {
            Liquids.move(behind, ahead, null, PUMP_RATE);
        }
    }
}
