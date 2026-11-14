package me.trossty.car.block;

import com.mojang.serialization.MapCodec;
import me.trossty.car.block.entity.FuelTankBlockEntity;
import me.trossty.car.registry.ModBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.PipeBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.phys.BlockHitResult;
import org.jetbrains.annotations.Nullable;

/**
 * A tank cell: a steel frame round glass walls, and whatever liquid it is
 * given showing through them.
 *
 * <p>Cells put up against each other are one tank. Each face knows whether
 * another cell is on the far side of it, and the frame and the glass are only
 * drawn on the faces that are the outside of the whole thing, so a stack of
 * them reads as one tall vessel and a floor of them as one wide one.
 *
 * <p>No window: a bucket, a bottle, a jerry can or anything else that carries
 * liquid pours in or draws off with a right click, an empty hand reads what is
 * in it, and a comparator reads how full it is.
 */
public class FuelTankBlock extends ModelEntityBlock {

    public static final MapCodec<FuelTankBlock> CODEC = simpleCodec(FuelTankBlock::new);

    public static final BooleanProperty NORTH = PipeBlock.NORTH;
    public static final BooleanProperty EAST = PipeBlock.EAST;
    public static final BooleanProperty SOUTH = PipeBlock.SOUTH;
    public static final BooleanProperty WEST = PipeBlock.WEST;
    public static final BooleanProperty UP = PipeBlock.UP;
    public static final BooleanProperty DOWN = PipeBlock.DOWN;

    public FuelTankBlock(Properties settings) {
        super(settings);
        BlockState state = this.getStateDefinition().any();
        for (BooleanProperty side : PipeBlock.PROPERTY_BY_DIRECTION.values()) {
            state = state.setValue(side, false);
        }
        this.registerDefaultState(state);
    }

    @Override
    protected MapCodec<? extends BaseEntityBlock> codec() {
        return CODEC;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(NORTH, EAST, SOUTH, WEST, UP, DOWN);
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new FuelTankBlockEntity(pos, state);
    }

    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(
            Level world, BlockState state, BlockEntityType<T> type) {
        if (world.isClientSide()) {
            return null;
        }
        return createTickerHelper(type, ModBlockEntities.FUEL_TANK, FuelTankBlockEntity::tick);
    }

    /** The tank standing at {@code pos}, if that is one. What a pipe asks. */
    public static FuelTankBlockEntity tankAt(Level world, BlockPos pos) {
        return world.getBlockEntity(pos) instanceof FuelTankBlockEntity tank ? tank : null;
    }

    // ---- joining up ----

    /** The cell's faces, each marked joined where another cell is on the far side. */
    private BlockState joined(BlockState state, LevelReader world, BlockPos pos) {
        for (Direction side : Direction.values()) {
            state = state.setValue(PipeBlock.PROPERTY_BY_DIRECTION.get(side),
                    world.getBlockState(pos.relative(side)).is(this));
        }
        return state;
    }

    @Override
    public @Nullable BlockState getStateForPlacement(BlockPlaceContext context) {
        Level world = context.getLevel();
        BlockPos pos = context.getClickedPos();
        // A cell between two tanks of different liquids would have to make
        // one tank of them; it is refused instead.
        if (!world.isClientSide()) {
            net.minecraft.world.level.material.Fluid liquid = net.minecraft.world.level.material.Fluids.EMPTY;
            for (Direction side : Direction.values()) {
                FuelTankBlockEntity next = tankAt(world, pos.relative(side));
                if (next == null) {
                    continue;
                }
                net.minecraft.world.level.material.Fluid held = next.getGroup().getResource();
                if (me.trossty.car.fluid.Liquids.isBlank(held)) {
                    continue;
                }
                if (!me.trossty.car.fluid.Liquids.isBlank(liquid) && liquid != held) {
                    if (context.getPlayer() != null) {
                        context.getPlayer().displayClientMessage(Component.translatable("message.car.tank_mixed"), true);
                    }
                    return null;
                }
                liquid = held;
            }
        }
        return this.joined(this.defaultBlockState(), world, pos);
    }

    @Override
    protected BlockState updateShape(BlockState state, Direction direction, BlockState neighborState,
                                     net.minecraft.world.level.LevelAccessor world, BlockPos pos, BlockPos neighborPos) {
        return state.setValue(PipeBlock.PROPERTY_BY_DIRECTION.get(direction), neighborState.is(this));
    }

    @Override
    protected void onPlace(BlockState state, Level world, BlockPos pos, BlockState oldState, boolean notify) {
        super.onPlace(state, world, pos, oldState, notify);
        FuelTankBlockEntity.reshape();
    }

    @Override
    protected void onRemove(BlockState state, net.minecraft.world.level.Level world, BlockPos pos,
                            BlockState newState, boolean moved) {
        // Only when the block really goes, not when its state changes.
        if (!world.isClientSide() && !state.is(newState.getBlock())) {
            // Breaking a cell out of a tank loses only the share that was in that
            // cell, which is what was really in it.
            FuelTankBlockEntity.reshape();
        }
        super.onRemove(state, world, pos, newState, moved);
    }

    // ---- using it ----

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level world, BlockPos pos,
                                 Player player, BlockHitResult hit) {
        // Only a sneaking click reads the gauge, so working around a tank does not keep printing it.
        if (!player.isShiftKeyDown()) {
            return InteractionResult.PASS;
        }
        if (world.isClientSide()) {
            return InteractionResult.SUCCESS;
        }
        if (world.getBlockEntity(pos) instanceof FuelTankBlockEntity tank) {
            report(player, tank);
        }
        return InteractionResult.CONSUME;
    }

    @Override
    protected net.minecraft.world.ItemInteractionResult useItemOn(ItemStack held, BlockState state, Level world, BlockPos pos,
                                         Player player, InteractionHand hand, BlockHitResult hit) {
        if (world.isClientSide()) {
            return held.isEmpty() ? net.minecraft.world.ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION : net.minecraft.world.ItemInteractionResult.SUCCESS;
        }
        if (!(world.getBlockEntity(pos) instanceof FuelTankBlockEntity tank)) {
            return net.minecraft.world.ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
        }
        if (LiquidStores.useWith(world, pos, player, hand, held, tank.getFluidStorage())) {
            return net.minecraft.world.ItemInteractionResult.CONSUME;
        }
        return net.minecraft.world.ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;   // falls through to onUse, which reads the gauge
    }

    /** "Petrol · 12.5 / 32 B · 2 cells", or "Empty tank · 32 B · 2 cells". */
    private static void report(Player player, FuelTankBlockEntity tank) {
        FuelTankBlockEntity.Group whole = tank.getGroup();
        LiquidStores.report(player, whole, "message.car.tank_empty", "message.car.tank_contents", whole.size());
    }

    // ---- redstone ----

    @Override
    protected boolean hasAnalogOutputSignal(BlockState state) {
        return true;
    }

    @Override
    protected int getAnalogOutputSignal(BlockState state, Level world, BlockPos pos) {
        FuelTankBlockEntity tank = tankAt(world, pos);
        return tank == null ? 0 : tank.comparatorOutput();
    }
}
