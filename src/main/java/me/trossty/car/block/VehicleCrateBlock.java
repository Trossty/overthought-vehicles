package me.trossty.car.block;

import com.mojang.serialization.MapCodec;
import me.trossty.car.block.entity.VehicleCrateBlockEntity;
import me.trossty.car.registry.ModBlockEntities;
import me.trossty.car.registry.ModItems;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
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
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jetbrains.annotations.Nullable;

/**
 * The wooden crate a new vehicle leaves the workstation in. It is set down
 * like any block, facing the way the vehicle inside will; a few turns of a
 * wrench take the lid off, the sides fall away and the vehicle is left
 * standing where the crate was. Broken instead, it is carried off with the
 * vehicle still in it.
 *
 * <p>Only the pallet it stands on is drawn with the chunk. The sides and the
 * lid are pieces the renderer draws, so that they can come off: {@link #PIECE}
 * names which, and only {@link #BASE} is ever put in the world.
 */
public class VehicleCrateBlock extends ModelEntityBlock {

    public static final MapCodec<VehicleCrateBlock> CODEC = simpleCodec(VehicleCrateBlock::new);
    public static final EnumProperty<Direction> FACING = BlockStateProperties.HORIZONTAL_FACING;

    public static final int BASE = 0;
    public static final int WALL = 1;
    public static final int LID = 2;
    public static final IntegerProperty PIECE = IntegerProperty.create("piece", BASE, LID);

    private static final VoxelShape SHAPE = Block.box(0.0, 0.0, 0.0, 16.0, 16.0, 16.0);

    public VehicleCrateBlock(Properties settings) {
        super(settings);
        this.registerDefaultState(this.getStateDefinition().any()
                .setValue(FACING, Direction.NORTH).setValue(PIECE, BASE));
    }

    @Override
    protected MapCodec<? extends BaseEntityBlock> codec() {
        return CODEC;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING, PIECE);
    }

    /** The vehicle faces away from whoever put the crate down, as it would off the item. */
    @Override
    public @Nullable BlockState getStateForPlacement(BlockPlaceContext context) {
        return this.defaultBlockState().setValue(FACING, context.getHorizontalDirection());
    }

    @Override
    protected BlockState rotate(BlockState state, net.minecraft.world.level.block.Rotation rotation) {
        return state.setValue(FACING, rotation.rotate(state.getValue(FACING)));
    }

    @Override
    protected BlockState mirror(BlockState state, net.minecraft.world.level.block.Mirror mirror) {
        return state.rotate(mirror.getRotation(state.getValue(FACING)));
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter world, BlockPos pos, CollisionContext context) {
        return SHAPE;
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new VehicleCrateBlockEntity(pos, state);
    }

    @Override
    public <T extends BlockEntity> @Nullable BlockEntityTicker<T> getTicker(
            Level world, BlockState state, BlockEntityType<T> type) {
        return createTickerHelper(type, ModBlockEntities.VEHICLE_CRATE, VehicleCrateBlockEntity::tick);
    }

    @Override
    protected ItemInteractionResult useItemOn(ItemStack held, BlockState state, Level world, BlockPos pos,
                                              Player player, InteractionHand hand, BlockHitResult hit) {
        if (!held.is(ModItems.WRENCH)) {
            return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
        }
        if (world.isClientSide()) {
            return ItemInteractionResult.SUCCESS;
        }
        if (world.getBlockEntity(pos) instanceof VehicleCrateBlockEntity crate) {
            crate.pry(player);
        }
        return ItemInteractionResult.CONSUME;
    }

    @Override
    protected net.minecraft.world.InteractionResult useWithoutItem(BlockState state, Level world, BlockPos pos,
                                                                   Player player, BlockHitResult hit) {
        if (!world.isClientSide()) {
            player.displayClientMessage(net.minecraft.network.chat.Component.translatable(
                    "message.car.crate_needs_wrench"), true);
        }
        return net.minecraft.world.InteractionResult.sidedSuccess(world.isClientSide());
    }
}
