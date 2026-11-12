package me.trossty.car.client.render;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

/** What every block entity renderer here reads off its block before drawing. */
public class BlockEntityRenderState {
    public BlockState blockState = Blocks.AIR.defaultBlockState();
    public BlockPos blockPos = BlockPos.ZERO;
    public int lightCoords;
}
