package me.trossty.car.block;

import com.mojang.serialization.MapCodec;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.ShapeContext;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.random.Random;
import net.minecraft.util.shape.VoxelShape;
import net.minecraft.world.BlockView;
import net.minecraft.world.WorldAccess;
import net.minecraft.world.WorldView;
import net.minecraft.world.tick.ScheduledTickView;

/**
 * A black, glistening puddle where crude has worked its way up to the
 * surface: the sign that there is a deposit underneath. It bubbles now and
 * then. A pumpjack can be set straight down on top of it.
 */
public class OilSeepBlock extends Block {

    public static final MapCodec<OilSeepBlock> CODEC = createCodec(OilSeepBlock::new);

    private static final VoxelShape SHAPE = Block.createCuboidShape(0, 0, 0, 16, 1, 16);

    public OilSeepBlock(Settings settings) {
        super(settings);
    }

    @Override
    protected MapCodec<? extends Block> getCodec() {
        return CODEC;
    }

    @Override
    protected VoxelShape getOutlineShape(BlockState state, BlockView world, BlockPos pos, ShapeContext context) {
        return SHAPE;
    }

    @Override
    protected boolean canPlaceAt(BlockState state, WorldView world, BlockPos pos) {
        return world.getBlockState(pos.down()).isSideSolidFullSquare(world, pos.down(), Direction.UP);
    }

    @Override
    protected BlockState getStateForNeighborUpdate(BlockState state, WorldView world, ScheduledTickView tickView,
                                                   BlockPos pos, Direction direction, BlockPos neighborPos,
                                                   BlockState neighborState, Random random) {
        return direction == Direction.DOWN && !this.canPlaceAt(state, world, pos)
                ? net.minecraft.block.Blocks.AIR.getDefaultState()
                : super.getStateForNeighborUpdate(state, world, tickView, pos, direction, neighborPos, neighborState, random);
    }

    @Override
    public void randomDisplayTick(BlockState state, net.minecraft.world.World world, BlockPos pos, Random random) {
        if (random.nextInt(4) == 0) {
            world.addParticleClient(ParticleTypes.SMOKE,
                    pos.getX() + 0.2 + random.nextDouble() * 0.6, pos.getY() + 0.1,
                    pos.getZ() + 0.2 + random.nextDouble() * 0.6, 0.0, 0.01, 0.0);
        }
    }
}
