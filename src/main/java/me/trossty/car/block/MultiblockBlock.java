package me.trossty.car.block;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import org.jspecify.annotations.Nullable;

/**
 * A machine bigger than a block.
 *
 * <p>The structure is a list of cells, each a block of its own with a
 * {@code part} number. Part 0 is the controller: the block that was placed,
 * the only one with a block entity, and the only one that drops anything.
 * The rest are placed with it and carry nothing but their share of the model,
 * which is cut from the whole machine one block at a time.
 *
 * <p>Cells are given in the machine's own frame: {@code x} to the right as
 * seen from the side the machine faces out of, {@code y} up, {@code z} back,
 * away from the front. With the front to the north that is east, up, south —
 * the same frame the block models are drawn in — so a cell list and a model
 * agree without any turning.
 *
 * <p>Breaking any part breaks the machine. The controller keeps an eye on its
 * parts as it ticks, so one blown away by a creeper takes the rest with it
 * rather than leaving half a refinery standing.
 */
public abstract class MultiblockBlock extends BaseEntityBlock {

    public static final EnumProperty<Direction> FACING = BlockStateProperties.HORIZONTAL_FACING;
    public static final BooleanProperty LIT = BlockStateProperties.LIT;

    protected MultiblockBlock(Properties settings) {
        super(settings);
    }

    /** The cells, part 0 first and at the origin. */
    public abstract int[][] cells();

    /** The part number, which has to cover every cell (and any render-only pieces). */
    public abstract IntegerProperty partProperty();

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING, LIT, this.partProperty());
    }

    public int partOf(BlockState state) {
        return state.getValue(this.partProperty());
    }

    public boolean isController(BlockState state) {
        return this.partOf(state) == 0;
    }

    /** Where a cell of a machine standing at {@code controller} is. */
    public static BlockPos cellPos(BlockPos controller, Direction facing, int[] cell) {
        return controller.relative(facing.getClockWise(), cell[0]).above(cell[1])
                .relative(facing.getOpposite(), cell[2]);
    }

    /** Where the controller of the machine this cell belongs to is. */
    public BlockPos controllerOf(BlockPos pos, BlockState state) {
        int part = this.partOf(state);
        if (part >= this.cells().length) {
            return pos;
        }
        int[] cell = this.cells()[part];
        Direction facing = state.getValue(FACING);
        return pos.relative(facing.getClockWise(), -cell[0]).below(cell[1])
                .relative(facing.getOpposite(), -cell[2]);
    }

    /** The controller's block entity, from any of its cells; null if there is none. */
    public static @Nullable BlockEntity controllerEntity(Level world, BlockPos pos) {
        BlockState state = world.getBlockState(pos);
        if (state.getBlock() instanceof MultiblockBlock machine) {
            return world.getBlockEntity(machine.controllerOf(pos, state));
        }
        return world.getBlockEntity(pos);
    }

    @Override
    public @Nullable BlockState getStateForPlacement(BlockPlaceContext context) {
        Direction facing = context.getHorizontalDirection().getOpposite();
        BlockPos origin = context.getClickedPos();
        Level world = context.getLevel();
        int[][] cells = this.cells();
        for (int i = 1; i < cells.length; i++) {
            BlockPos at = cellPos(origin, facing, cells[i]);
            if (!world.isInWorldBounds(at) || !world.getBlockState(at).canBeReplaced(context)) {
                return null;
            }
        }
        return this.defaultBlockState().setValue(FACING, facing).setValue(LIT, false).setValue(this.partProperty(), 0);
    }

    @Override
    public void setPlacedBy(Level world, BlockPos pos, BlockState state, @Nullable LivingEntity placer, ItemStack stack) {
        super.setPlacedBy(world, pos, state, placer, stack);
        if (world.isClientSide() || !this.isController(state)) {
            return;
        }
        this.placeParts(world, pos, state);
    }

    /** Puts every cell but the controller down, for placing by hand or by command. */
    public void placeParts(Level world, BlockPos pos, BlockState controller) {
        Direction facing = controller.getValue(FACING);
        int[][] cells = this.cells();
        for (int i = 1; i < cells.length; i++) {
            world.setBlock(cellPos(pos, facing, cells[i]),
                    controller.setValue(this.partProperty(), i), Block.UPDATE_ALL);
        }
    }

    /** Whether every cell is still standing. */
    public boolean isIntact(Level world, BlockPos controller, BlockState state) {
        Direction facing = state.getValue(FACING);
        int[][] cells = this.cells();
        for (int i = 1; i < cells.length; i++) {
            BlockState there = world.getBlockState(cellPos(controller, facing, cells[i]));
            if (!there.is(this) || this.partOf(there) != i || there.getValue(FACING) != facing) {
                return false;
            }
        }
        return true;
    }

    /**
     * Lights the whole machine, or puts it out: the parts carry the flag too,
     * so a chimney knows to smoke and a lamp three blocks up knows to glow.
     */
    public void setLit(Level world, BlockPos controller, BlockState state, boolean lit) {
        if (state.getValue(LIT) == lit) {
            return;
        }
        Direction facing = state.getValue(FACING);
        int[][] cells = this.cells();
        for (int i = 0; i < cells.length; i++) {
            BlockPos at = cellPos(controller, facing, cells[i]);
            BlockState there = world.getBlockState(at);
            if (there.is(this)) {
                world.setBlock(at, there.setValue(LIT, lit), Block.UPDATE_CLIENTS);
            }
        }
    }

    /** A part broken by hand takes the controller, and so the item, with it. */
    @Override
    public BlockState playerWillDestroy(Level world, BlockPos pos, BlockState state, Player player) {
        if (!world.isClientSide() && !this.isController(state)) {
            BlockPos controller = this.controllerOf(pos, state);
            BlockState there = world.getBlockState(controller);
            if (there.is(this) && this.isController(there)) {
                world.destroyBlock(controller, !player.isCreative(), player);
            }
        }
        return super.playerWillDestroy(world, pos, state, player);
    }

    /**
     * The controller going takes every part with it. Subclasses spill their
     * contents first and then call this.
     */
    @Override
    protected void affectNeighborsAfterRemoval(BlockState state, ServerLevel world, BlockPos pos, boolean moved) {
        if (this.isController(state)) {
            Direction facing = state.getValue(FACING);
            int[][] cells = this.cells();
            for (int i = 1; i < cells.length; i++) {
                BlockPos at = cellPos(pos, facing, cells[i]);
                BlockState there = world.getBlockState(at);
                if (there.is(this) && this.partOf(there) == i) {
                    world.setBlock(at, net.minecraft.world.level.block.Blocks.AIR.defaultBlockState(), Block.UPDATE_ALL);
                }
            }
        }
        super.affectNeighborsAfterRemoval(state, world, pos, moved);
    }

    @Override
    protected net.minecraft.world.level.block.RenderShape getRenderShape(BlockState state) {
        return net.minecraft.world.level.block.RenderShape.MODEL;
    }

    /**
     * For the controller's ticker: a machine missing a part is a broken
     * machine, and goes the way a broken one would, item and all.
     */
    public static boolean checkIntact(Level world, BlockPos pos, BlockState state) {
        if (world.getGameTime() % 20 != (pos.asLong() & 15)) {
            return true;
        }
        if (state.getBlock() instanceof MultiblockBlock machine && !machine.isIntact(world, pos, state)) {
            world.destroyBlock(pos, true);
            return false;
        }
        return true;
    }
}
