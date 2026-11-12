package me.trossty.car.block;

import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.BlockWithEntity;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemPlacementContext;
import net.minecraft.item.ItemStack;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.state.StateManager;
import net.minecraft.state.property.BooleanProperty;
import net.minecraft.state.property.EnumProperty;
import net.minecraft.state.property.IntProperty;
import net.minecraft.state.property.Properties;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.world.World;
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
public abstract class MultiblockBlock extends BlockWithEntity {

    public static final EnumProperty<Direction> FACING = Properties.HORIZONTAL_FACING;
    public static final BooleanProperty LIT = Properties.LIT;

    protected MultiblockBlock(Settings settings) {
        super(settings);
    }

    /** The cells, part 0 first and at the origin. */
    public abstract int[][] cells();

    /** The part number, which has to cover every cell (and any render-only pieces). */
    public abstract IntProperty partProperty();

    @Override
    protected void appendProperties(StateManager.Builder<Block, BlockState> builder) {
        builder.add(FACING, LIT, this.partProperty());
    }

    public int partOf(BlockState state) {
        return state.get(this.partProperty());
    }

    public boolean isController(BlockState state) {
        return this.partOf(state) == 0;
    }

    /** Where a cell of a machine standing at {@code controller} is. */
    public static BlockPos cellPos(BlockPos controller, Direction facing, int[] cell) {
        return controller.offset(facing.rotateYClockwise(), cell[0]).up(cell[1])
                .offset(facing.getOpposite(), cell[2]);
    }

    /** Where the controller of the machine this cell belongs to is. */
    public BlockPos controllerOf(BlockPos pos, BlockState state) {
        int part = this.partOf(state);
        if (part >= this.cells().length) {
            return pos;
        }
        int[] cell = this.cells()[part];
        Direction facing = state.get(FACING);
        return pos.offset(facing.rotateYClockwise(), -cell[0]).down(cell[1])
                .offset(facing.getOpposite(), -cell[2]);
    }

    /** The controller's block entity, from any of its cells; null if there is none. */
    public static @Nullable BlockEntity controllerEntity(World world, BlockPos pos) {
        BlockState state = world.getBlockState(pos);
        if (state.getBlock() instanceof MultiblockBlock machine) {
            return world.getBlockEntity(machine.controllerOf(pos, state));
        }
        return world.getBlockEntity(pos);
    }

    @Override
    public @Nullable BlockState getPlacementState(ItemPlacementContext context) {
        Direction facing = context.getHorizontalPlayerFacing().getOpposite();
        BlockPos origin = context.getBlockPos();
        World world = context.getWorld();
        int[][] cells = this.cells();
        for (int i = 1; i < cells.length; i++) {
            BlockPos at = cellPos(origin, facing, cells[i]);
            if (!world.isInBuildLimit(at) || !world.getBlockState(at).canReplace(context)) {
                return null;
            }
        }
        return this.getDefaultState().with(FACING, facing).with(LIT, false).with(this.partProperty(), 0);
    }

    @Override
    public void onPlaced(World world, BlockPos pos, BlockState state, @Nullable LivingEntity placer, ItemStack stack) {
        super.onPlaced(world, pos, state, placer, stack);
        if (world.isClient() || !this.isController(state)) {
            return;
        }
        this.placeParts(world, pos, state);
    }

    /** Puts every cell but the controller down, for placing by hand or by command. */
    public void placeParts(World world, BlockPos pos, BlockState controller) {
        Direction facing = controller.get(FACING);
        int[][] cells = this.cells();
        for (int i = 1; i < cells.length; i++) {
            world.setBlockState(cellPos(pos, facing, cells[i]),
                    controller.with(this.partProperty(), i), Block.NOTIFY_ALL);
        }
    }

    /** Whether every cell is still standing. */
    public boolean isIntact(World world, BlockPos controller, BlockState state) {
        Direction facing = state.get(FACING);
        int[][] cells = this.cells();
        for (int i = 1; i < cells.length; i++) {
            BlockState there = world.getBlockState(cellPos(controller, facing, cells[i]));
            if (!there.isOf(this) || this.partOf(there) != i || there.get(FACING) != facing) {
                return false;
            }
        }
        return true;
    }

    /**
     * Lights the whole machine, or puts it out: the parts carry the flag too,
     * so a chimney knows to smoke and a lamp three blocks up knows to glow.
     */
    public void setLit(World world, BlockPos controller, BlockState state, boolean lit) {
        if (state.get(LIT) == lit) {
            return;
        }
        Direction facing = state.get(FACING);
        int[][] cells = this.cells();
        for (int i = 0; i < cells.length; i++) {
            BlockPos at = cellPos(controller, facing, cells[i]);
            BlockState there = world.getBlockState(at);
            if (there.isOf(this)) {
                world.setBlockState(at, there.with(LIT, lit), Block.NOTIFY_LISTENERS);
            }
        }
    }

    /** A part broken by hand takes the controller, and so the item, with it. */
    @Override
    public BlockState onBreak(World world, BlockPos pos, BlockState state, PlayerEntity player) {
        if (!world.isClient() && !this.isController(state)) {
            BlockPos controller = this.controllerOf(pos, state);
            BlockState there = world.getBlockState(controller);
            if (there.isOf(this) && this.isController(there)) {
                world.breakBlock(controller, !player.isCreative(), player);
            }
        }
        return super.onBreak(world, pos, state, player);
    }

    /**
     * The controller going takes every part with it. Subclasses spill their
     * contents first and then call this.
     */
    @Override
    protected void onStateReplaced(BlockState state, ServerWorld world, BlockPos pos, boolean moved) {
        if (this.isController(state)) {
            Direction facing = state.get(FACING);
            int[][] cells = this.cells();
            for (int i = 1; i < cells.length; i++) {
                BlockPos at = cellPos(pos, facing, cells[i]);
                BlockState there = world.getBlockState(at);
                if (there.isOf(this) && this.partOf(there) == i) {
                    world.setBlockState(at, net.minecraft.block.Blocks.AIR.getDefaultState(), Block.NOTIFY_ALL);
                }
            }
        }
        super.onStateReplaced(state, world, pos, moved);
    }

    @Override
    protected net.minecraft.block.BlockRenderType getRenderType(BlockState state) {
        return net.minecraft.block.BlockRenderType.MODEL;
    }

    /**
     * For the controller's ticker: a machine missing a part is a broken
     * machine, and goes the way a broken one would, item and all.
     */
    public static boolean checkIntact(World world, BlockPos pos, BlockState state) {
        if (world.getTime() % 20 != (pos.asLong() & 15)) {
            return true;
        }
        if (state.getBlock() instanceof MultiblockBlock machine && !machine.isIntact(world, pos, state)) {
            world.breakBlock(pos, true);
            return false;
        }
        return true;
    }
}
