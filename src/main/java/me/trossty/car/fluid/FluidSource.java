package me.trossty.car.fluid;

import me.trossty.car.block.MultiblockBlock;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;

/**
 * A machine liquid comes <em>out</em> of: a pumpjack, a refinery, a
 * hydrotreater. A tanker's hose on its automatic setting loads from these and
 * unloads into everything else, which is what keeps it from emptying a
 * refinery back into the pumpjack it came from.
 */
public interface FluidSource {

    /** Whether the block at {@code pos} — any cell of a machine — is one. */
    static boolean at(Level world, BlockPos pos) {
        return MultiblockBlock.controllerEntity(world, pos) instanceof FluidSource;
    }
}
