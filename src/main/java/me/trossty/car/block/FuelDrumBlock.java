package me.trossty.car.block;

import com.mojang.serialization.MapCodec;
import me.trossty.car.block.entity.FuelDrumBlockEntity;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.BlockWithEntity;
import net.minecraft.block.ShapeContext;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemPlacementContext;
import net.minecraft.item.ItemStack;
import net.minecraft.state.StateManager;
import net.minecraft.state.property.EnumProperty;
import net.minecraft.state.property.Properties;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.shape.VoxelShape;
import net.minecraft.world.BlockView;
import net.minecraft.world.World;
import org.jspecify.annotations.Nullable;

/**
 * A steel drum. Right click with a jerry can, a bucket or anything else that
 * carries liquid to pour in or draw off; an empty hand reads what is in it; a
 * comparator reads how full it is. Broken, it is carried away with its liquid
 * still in it.
 *
 * <p>It faces the way it was put down only so the filler cap on its lid is on
 * the side nearest you.
 */
public class FuelDrumBlock extends BlockWithEntity {

    public static final MapCodec<FuelDrumBlock> CODEC = createCodec(FuelDrumBlock::new);
    public static final EnumProperty<Direction> FACING = Properties.HORIZONTAL_FACING;

    /** The drum is round, fourteen pixels across and a block tall; this is the square round it. */
    private static final VoxelShape SHAPE = Block.createCuboidShape(1.0, 0.0, 1.0, 15.0, 16.0, 15.0);

    public FuelDrumBlock(Settings settings) {
        super(settings);
        this.setDefaultState(this.getStateManager().getDefaultState().with(FACING, Direction.NORTH));
    }

    @Override
    protected MapCodec<? extends BlockWithEntity> getCodec() {
        return CODEC;
    }

    @Override
    protected void appendProperties(StateManager.Builder<Block, BlockState> builder) {
        builder.add(FACING);
    }

    @Override
    public @Nullable BlockState getPlacementState(ItemPlacementContext context) {
        return this.getDefaultState().with(FACING, context.getHorizontalPlayerFacing().getOpposite());
    }

    @Override
    protected BlockState rotate(BlockState state, net.minecraft.util.BlockRotation rotation) {
        return state.with(FACING, rotation.rotate(state.get(FACING)));
    }

    @Override
    protected BlockState mirror(BlockState state, net.minecraft.util.BlockMirror mirror) {
        return state.rotate(mirror.getRotation(state.get(FACING)));
    }

    @Override
    protected VoxelShape getOutlineShape(BlockState state, BlockView world, BlockPos pos, ShapeContext context) {
        return SHAPE;
    }

    @Override
    public BlockEntity createBlockEntity(BlockPos pos, BlockState state) {
        return new FuelDrumBlockEntity(pos, state);
    }

    @Override
    protected ActionResult onUse(BlockState state, World world, BlockPos pos,
                                 PlayerEntity player, BlockHitResult hit) {
        if (world.isClient()) {
            return ActionResult.SUCCESS;
        }
        if (world.getBlockEntity(pos) instanceof FuelDrumBlockEntity drum) {
            report(player, drum);
        }
        return ActionResult.CONSUME;
    }

    @Override
    protected ActionResult onUseWithItem(ItemStack held, BlockState state, World world, BlockPos pos,
                                         PlayerEntity player, Hand hand, BlockHitResult hit) {
        if (world.isClient()) {
            return held.isEmpty() ? ActionResult.PASS : ActionResult.SUCCESS;
        }
        if (!(world.getBlockEntity(pos) instanceof FuelDrumBlockEntity drum)) {
            return ActionResult.PASS;
        }
        if (LiquidStores.useWith(world, pos, player, hand, held, drum.getFluidStorage())) {
            report(player, drum);
            return ActionResult.CONSUME;
        }
        return ActionResult.PASS;   // falls through to onUse, which reads it
    }

    private static void report(PlayerEntity player, FuelDrumBlockEntity drum) {
        LiquidStores.report(player, drum.getFluidStorage(), "message.car.drum_empty", "message.car.drum_contents");
    }

    @Override
    protected boolean hasComparatorOutput(BlockState state) {
        return true;
    }

    @Override
    protected int getComparatorOutput(BlockState state, World world, BlockPos pos, Direction direction) {
        return world.getBlockEntity(pos) instanceof FuelDrumBlockEntity drum ? drum.comparatorOutput() : 0;
    }
}
