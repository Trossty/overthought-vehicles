package me.trossty.car.block;

import com.mojang.serialization.MapCodec;
import me.trossty.car.entity.CarEntity;
import me.trossty.car.registry.ModBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * A folding engine crane: legs on castors, a mast and a boom with a chain
 * and hook. An engine comes out of the top of a car with one of these
 * standing at its nose and the bonnet up, without a lift; nothing else
 * that heavy is lifted out by hand.
 */
public class EngineHoistBlock extends HorizontalDirectionalBlock {

    public static final MapCodec<EngineHoistBlock> CODEC = simpleCodec(EngineHoistBlock::new);

    /** How far from the middle of the bonnet a hoist still reaches the engine, in blocks. */
    private static final double REACH = 2.5;

    /** The legs and the mast; the boom hangs over the car and stops nothing. */
    private static final VoxelShape SHAPE = Shapes.or(
            Block.box(0.0, 0.0, 0.0, 16.0, 3.0, 16.0),
            Block.box(6.0, 3.0, 6.0, 10.0, 16.0, 10.0));

    public EngineHoistBlock(Properties settings) {
        super(settings);
        this.registerDefaultState(this.getStateDefinition().any().setValue(FACING, Direction.NORTH));
    }

    @Override
    protected MapCodec<? extends HorizontalDirectionalBlock> codec() {
        return CODEC;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING);
    }

    /** The boom points the way the player looks: at the car they are standing in front of. */
    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        return this.defaultBlockState().setValue(FACING, context.getHorizontalDirection());
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter world, BlockPos pos, CollisionContext context) {
        return SHAPE;
    }

    /** Whether a hoist stands close enough to the car's open engine bay to lift its engine. */
    public static boolean isNear(Level world, CarEntity car) {
        Vec3 bay = car.bonnetCentre();
        if (bay == null) {
            return false;
        }
        BlockPos at = BlockPos.containing(bay);
        for (BlockPos pos : BlockPos.betweenClosed(at.offset(-3, -2, -3), at.offset(3, 1, 3))) {
            if (world.getBlockState(pos).is(ModBlocks.ENGINE_HOIST)
                    && Vec3.atBottomCenterOf(pos).distanceToSqr(bay.x, pos.getY(), bay.z) <= REACH * REACH) {
                return true;
            }
        }
        return false;
    }
}
