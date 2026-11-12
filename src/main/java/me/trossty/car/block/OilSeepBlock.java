package me.trossty.car.block;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.ScheduledTickAccess;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * A black, glistening puddle where crude has worked its way up to the
 * surface: the sign that there is a deposit underneath. It bubbles now and
 * then. A pumpjack can be set straight down on top of it.
 */
public class OilSeepBlock extends Block {

    public static final MapCodec<OilSeepBlock> CODEC = simpleCodec(OilSeepBlock::new);

    private static final VoxelShape SHAPE = Block.box(0, 0, 0, 16, 1, 16);

    public OilSeepBlock(Properties settings) {
        super(settings);
    }

    @Override
    protected MapCodec<? extends Block> codec() {
        return CODEC;
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter world, BlockPos pos, CollisionContext context) {
        return SHAPE;
    }

    @Override
    protected boolean canSurvive(BlockState state, LevelReader world, BlockPos pos) {
        return world.getBlockState(pos.below()).isFaceSturdy(world, pos.below(), Direction.UP);
    }

    @Override
    protected BlockState updateShape(BlockState state, LevelReader world, ScheduledTickAccess tickView,
                                                   BlockPos pos, Direction direction, BlockPos neighborPos,
                                                   BlockState neighborState, RandomSource random) {
        return direction == Direction.DOWN && !this.canSurvive(state, world, pos)
                ? net.minecraft.world.level.block.Blocks.AIR.defaultBlockState()
                : super.updateShape(state, world, tickView, pos, direction, neighborPos, neighborState, random);
    }

    @Override
    public void animateTick(BlockState state, net.minecraft.world.level.Level world, BlockPos pos, RandomSource random) {
        if (random.nextInt(4) == 0) {
            world.addParticle(ParticleTypes.SMOKE,
                    pos.getX() + 0.2 + random.nextDouble() * 0.6, pos.getY() + 0.1,
                    pos.getZ() + 0.2 + random.nextDouble() * 0.6, 0.0, 0.01, 0.0);
        }
    }
}
