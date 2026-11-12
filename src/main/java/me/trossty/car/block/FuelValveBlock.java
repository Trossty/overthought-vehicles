package me.trossty.car.block;

import com.mojang.serialization.MapCodec;
import me.trossty.car.block.entity.FuelValveBlockEntity;
import me.trossty.car.registry.ModBlockEntities;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.BlockWithEntity;
import net.minecraft.block.ShapeContext;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.block.entity.BlockEntityTicker;
import net.minecraft.block.entity.BlockEntityType;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemPlacementContext;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.state.StateManager;
import net.minecraft.state.property.BooleanProperty;
import net.minecraft.state.property.EnumProperty;
import net.minecraft.state.property.Properties;
import net.minecraft.text.Text;
import net.minecraft.util.ActionResult;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.shape.VoxelShape;
import net.minecraft.util.shape.VoxelShapes;
import net.minecraft.world.BlockView;
import net.minecraft.world.World;

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
public class FuelValveBlock extends BlockWithEntity {

    public static final MapCodec<FuelValveBlock> CODEC = createCodec(FuelValveBlock::new);

    public static final EnumProperty<Direction> FACING = Properties.FACING;
    public static final BooleanProperty POWERED = Properties.POWERED;

    /** The valve body, a little fatter than the pipe it sits in. */
    private static final VoxelShape BODY = Block.createCuboidShape(4.0, 4.0, 4.0, 12.0, 12.0, 12.0);

    public FuelValveBlock(Settings settings) {
        super(settings);
        this.setDefaultState(this.getStateManager().getDefaultState()
                .with(FACING, Direction.NORTH)
                .with(POWERED, false));
    }

    @Override
    protected MapCodec<? extends BlockWithEntity> getCodec() {
        return CODEC;
    }

    @Override
    protected void appendProperties(StateManager.Builder<Block, BlockState> builder) {
        builder.add(FACING, POWERED);
    }

    /** Points at the block you clicked, so you aim it at what you want filled. */
    @Override
    public BlockState getPlacementState(ItemPlacementContext context) {
        return this.getDefaultState().with(FACING, context.getSide().getOpposite());
    }

    @Override
    protected VoxelShape getOutlineShape(BlockState state, BlockView world, BlockPos pos, ShapeContext context) {
        Direction facing = state.get(FACING);
        return VoxelShapes.union(BODY, collar(facing), collar(facing.getOpposite()));
    }

    /** The stub of pipe sticking out of one face. */
    private static VoxelShape collar(Direction side) {
        double x = side.getOffsetX();
        double y = side.getOffsetY();
        double z = side.getOffsetZ();
        return Block.createCuboidShape(
                x < 0 ? 0.0 : 5.0, y < 0 ? 0.0 : 5.0, z < 0 ? 0.0 : 5.0,
                x > 0 ? 16.0 : 11.0, y > 0 ? 16.0 : 11.0, z > 0 ? 16.0 : 11.0);
    }

    @Override
    public BlockEntity createBlockEntity(BlockPos pos, BlockState state) {
        return new FuelValveBlockEntity(pos, state);
    }

    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(
            World world, BlockState state, BlockEntityType<T> type) {
        if (world.isClient()) {
            return null;
        }
        return validateTicker(type, ModBlockEntities.FUEL_VALVE, FuelValveBlockEntity::tick);
    }

    /** A bare click turns the valve; aiming one is not worth a dedicated tool. */
    @Override
    protected ActionResult onUse(BlockState state, World world, BlockPos pos,
                                 PlayerEntity player, BlockHitResult hit) {
        if (world.isClient()) {
            return ActionResult.SUCCESS;
        }
        Direction turned = next(state.get(FACING));
        world.setBlockState(pos, state.with(FACING, turned), Block.NOTIFY_ALL);
        world.playSound(null, pos, SoundEvents.BLOCK_LEVER_CLICK, SoundCategory.BLOCKS, 0.4F, 1.4F);

        player.sendMessage(Text.translatable(
                state.get(POWERED) ? "message.car.valve_shut" : "message.car.valve_open",
                Text.translatable("direction.car." + turned.asString())), true);
        return ActionResult.CONSUME;
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
