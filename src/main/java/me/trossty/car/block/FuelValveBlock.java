package me.trossty.car.block;

import com.mojang.serialization.MapCodec;
import me.trossty.car.block.entity.FuelValveBlockEntity;
import me.trossty.car.registry.ModBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * The valve that gives a line of pipe a destination.
 *
 * <p>It points somewhere, and that is the whole interface: fuel goes in the
 * back and out the front, never the other way, and faster than pipe levelling
 * can move it. {@link FuelValveBlockEntity} explains why that amounts to
 * priority.
 *
 * <p>Placed against a block it feeds that block, the way a hopper does, so you
 * aim it by clicking the thing you want filled. A bare right click turns it
 * through the six directions if you aimed wrong, and says which way it is
 * pointing and whether redstone currently has it shut.
 */
public class FuelValveBlock extends ModelEntityBlock {

    public static final MapCodec<FuelValveBlock> CODEC = simpleCodec(FuelValveBlock::new);

    public static final EnumProperty<Direction> FACING = BlockStateProperties.FACING;
    public static final BooleanProperty POWERED = BlockStateProperties.POWERED;

    /** The valve body, a little fatter than the pipe it sits in. */
    private static final VoxelShape BODY = Block.box(4.0, 4.0, 4.0, 12.0, 12.0, 12.0);

    public FuelValveBlock(Properties settings) {
        super(settings);
        this.registerDefaultState(this.getStateDefinition().any()
                .setValue(FACING, Direction.NORTH)
                .setValue(POWERED, false));
    }

    @Override
    protected MapCodec<? extends BaseEntityBlock> codec() {
        return CODEC;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING, POWERED);
    }

    /** Points at the block you clicked, so you aim it at what you want filled. */
    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        return this.defaultBlockState().setValue(FACING, context.getClickedFace().getOpposite());
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter world, BlockPos pos, CollisionContext context) {
        Direction facing = state.getValue(FACING);
        return Shapes.or(BODY, collar(facing), collar(facing.getOpposite()));
    }

    /** The stub of pipe sticking out of one face. */
    private static VoxelShape collar(Direction side) {
        double x = side.getStepX();
        double y = side.getStepY();
        double z = side.getStepZ();
        return Block.box(
                x < 0 ? 0.0 : 5.0, y < 0 ? 0.0 : 5.0, z < 0 ? 0.0 : 5.0,
                x > 0 ? 16.0 : 11.0, y > 0 ? 16.0 : 11.0, z > 0 ? 16.0 : 11.0);
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new FuelValveBlockEntity(pos, state);
    }

    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(
            Level world, BlockState state, BlockEntityType<T> type) {
        if (world.isClientSide()) {
            return null;
        }
        return createTickerHelper(type, ModBlockEntities.FUEL_VALVE, FuelValveBlockEntity::tick);
    }

    /** A bare click turns the valve; aiming one is not worth a dedicated tool. */
    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level world, BlockPos pos,
                                 Player player, BlockHitResult hit) {
        if (world.isClientSide()) {
            return InteractionResult.SUCCESS;
        }
        Direction turned = next(state.getValue(FACING));
        world.setBlock(pos, state.setValue(FACING, turned), Block.UPDATE_ALL);
        world.playSound(null, pos, SoundEvents.LEVER_CLICK, SoundSource.BLOCKS, 0.4F, 1.4F);

        player.displayClientMessage(Component.translatable(
                state.getValue(POWERED) ? "message.car.valve_shut" : "message.car.valve_open",
                Component.translatable("direction.car." + turned.getSerializedName())), true);
        return InteractionResult.CONSUME;
    }

    /** North, east, south, west, up, down, and round again. */
    private static Direction next(Direction facing) {
        return switch (facing) {
            case NORTH -> Direction.EAST;
            case EAST -> Direction.SOUTH;
            case SOUTH -> Direction.WEST;
            case WEST -> Direction.UP;
            case UP -> Direction.DOWN;
            default -> Direction.NORTH;
        };
    }
}
