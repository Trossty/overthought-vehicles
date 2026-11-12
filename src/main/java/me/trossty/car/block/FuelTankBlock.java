package me.trossty.car.block;

import com.mojang.serialization.MapCodec;
import me.trossty.car.block.entity.FuelTankBlockEntity;
import me.trossty.car.registry.ModBlockEntities;
import net.fabricmc.fabric.api.transfer.v1.fluid.FluidVariant;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.BlockWithEntity;
import net.minecraft.block.ConnectingBlock;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.block.entity.BlockEntityTicker;
import net.minecraft.block.entity.BlockEntityType;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemPlacementContext;
import net.minecraft.item.ItemStack;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.state.StateManager;
import net.minecraft.state.property.BooleanProperty;
import net.minecraft.text.Text;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.random.Random;
import net.minecraft.world.World;
import net.minecraft.world.WorldView;
import net.minecraft.world.tick.ScheduledTickView;
import org.jspecify.annotations.Nullable;

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
public class FuelTankBlock extends BlockWithEntity {

    public static final MapCodec<FuelTankBlock> CODEC = createCodec(FuelTankBlock::new);

    public static final BooleanProperty NORTH = ConnectingBlock.NORTH;
    public static final BooleanProperty EAST = ConnectingBlock.EAST;
    public static final BooleanProperty SOUTH = ConnectingBlock.SOUTH;
    public static final BooleanProperty WEST = ConnectingBlock.WEST;
    public static final BooleanProperty UP = ConnectingBlock.UP;
    public static final BooleanProperty DOWN = ConnectingBlock.DOWN;

    public FuelTankBlock(Settings settings) {
        super(settings);
        BlockState state = this.getStateManager().getDefaultState();
        for (BooleanProperty side : ConnectingBlock.FACING_PROPERTIES.values()) {
            state = state.with(side, false);
        }
        this.setDefaultState(state);
    }

    @Override
    protected MapCodec<? extends BlockWithEntity> getCodec() {
        return CODEC;
    }

    @Override
    protected void appendProperties(StateManager.Builder<Block, BlockState> builder) {
        builder.add(NORTH, EAST, SOUTH, WEST, UP, DOWN);
    }

    @Override
    public BlockEntity createBlockEntity(BlockPos pos, BlockState state) {
        return new FuelTankBlockEntity(pos, state);
    }

    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(
            World world, BlockState state, BlockEntityType<T> type) {
        if (world.isClient()) {
            return null;
        }
        return validateTicker(type, ModBlockEntities.FUEL_TANK, FuelTankBlockEntity::tick);
    }

    /** The tank standing at {@code pos}, if that is one. What a pipe asks. */
    public static FuelTankBlockEntity tankAt(World world, BlockPos pos) {
        return world.getBlockEntity(pos) instanceof FuelTankBlockEntity tank ? tank : null;
    }

    // ---- joining up ----

    /** The cell's faces, each marked joined where another cell is on the far side. */
    private BlockState joined(BlockState state, WorldView world, BlockPos pos) {
        for (Direction side : Direction.values()) {
            state = state.with(ConnectingBlock.FACING_PROPERTIES.get(side),
                    world.getBlockState(pos.offset(side)).isOf(this));
        }
        return state;
    }

    @Override
    public @Nullable BlockState getPlacementState(ItemPlacementContext context) {
        World world = context.getWorld();
        BlockPos pos = context.getBlockPos();
        // A cell between two tanks of different liquids would have to make
        // one tank of them; it is refused instead.
        if (!world.isClient()) {
            FluidVariant liquid = FluidVariant.blank();
            for (Direction side : Direction.values()) {
                FuelTankBlockEntity next = tankAt(world, pos.offset(side));
                if (next == null) {
                    continue;
                }
                FluidVariant held = next.getGroup().getResource();
                if (held.isBlank()) {
                    continue;
                }
                if (!liquid.isBlank() && !liquid.equals(held)) {
                    if (context.getPlayer() != null) {
                        context.getPlayer().sendMessage(Text.translatable("message.car.tank_mixed"), true);
                    }
                    return null;
                }
                liquid = held;
            }
        }
        return this.joined(this.getDefaultState(), world, pos);
    }

    @Override
    protected BlockState getStateForNeighborUpdate(BlockState state, WorldView world,
                                                   ScheduledTickView tickView, BlockPos pos,
                                                   Direction direction, BlockPos neighborPos,
                                                   BlockState neighborState, Random random) {
        return state.with(ConnectingBlock.FACING_PROPERTIES.get(direction), neighborState.isOf(this));
    }

    @Override
    protected void onBlockAdded(BlockState state, World world, BlockPos pos, BlockState oldState, boolean notify) {
        super.onBlockAdded(state, world, pos, oldState, notify);
        FuelTankBlockEntity.reshape();
    }

    @Override
    protected void onStateReplaced(BlockState state, ServerWorld world, BlockPos pos, boolean moved) {
        // Breaking a cell out of a tank loses only the share that was in that
        // cell, which is what was really in it.
        FuelTankBlockEntity.reshape();
        super.onStateReplaced(state, world, pos, moved);
    }

    // ---- using it ----

    @Override
    protected ActionResult onUse(BlockState state, World world, BlockPos pos,
                                 PlayerEntity player, BlockHitResult hit) {
        if (world.isClient()) {
            return ActionResult.SUCCESS;
        }
        if (world.getBlockEntity(pos) instanceof FuelTankBlockEntity tank) {
            report(player, tank);
        }
        return ActionResult.CONSUME;
    }

    @Override
    protected ActionResult onUseWithItem(ItemStack held, BlockState state, World world, BlockPos pos,
                                         PlayerEntity player, Hand hand, BlockHitResult hit) {
        if (world.isClient()) {
            return held.isEmpty() ? ActionResult.PASS : ActionResult.SUCCESS;
        }
        if (!(world.getBlockEntity(pos) instanceof FuelTankBlockEntity tank)) {
            return ActionResult.PASS;
        }
        if (LiquidStores.useWith(world, pos, player, hand, held, tank.getFluidStorage())) {
            report(player, tank);
            return ActionResult.CONSUME;
        }
        return ActionResult.PASS;   // falls through to onUse, which reads the gauge
    }

    /** "Petrol · 12.5 / 32 B · 2 cells", or "Empty tank · 32 B · 2 cells". */
    private static void report(PlayerEntity player, FuelTankBlockEntity tank) {
        FuelTankBlockEntity.Group whole = tank.getGroup();
        LiquidStores.report(player, whole, "message.car.tank_empty", "message.car.tank_contents", whole.size());
    }

    // ---- redstone ----

    @Override
    protected boolean hasComparatorOutput(BlockState state) {
        return true;
    }

    @Override
    protected int getComparatorOutput(BlockState state, World world, BlockPos pos, Direction direction) {
        FuelTankBlockEntity tank = tankAt(world, pos);
        return tank == null ? 0 : tank.comparatorOutput();
    }
}
