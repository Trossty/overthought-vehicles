package me.trossty.car.block;

import com.mojang.serialization.MapCodec;
import me.trossty.car.block.entity.VehicleLiftBlockEntity;
import me.trossty.car.registry.ModBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
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
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jetbrains.annotations.Nullable;

import java.util.IdentityHashMap;
import java.util.Map;

/**
 * The ramp a car is assembled on, the only place one can be worked on
 * afterwards, and — since it is a lift — the thing that puts the car in the air
 * to do it.
 *
 * <p>Assembly happens in its window, which is the workbench's window fed a
 * different recipe list, and that works with the platform down: you build a car
 * standing on the floor. Repair is the other way about. Beating dents out is
 * still a matter of walking up with an iron ingot, but
 * {@link #isServicing(Level, Entity)} wants a lift that is actually
 * <em>up</em>, so the job happens where a garage would do it, on a car you can
 * get under.
 *
 * <p>A right click takes the lift up a stop and a sneaking one down a stop,
 * the one control it has besides redstone;
 * {@link VehicleLiftBlockEntity} explains how the two get along.
 *
 * <p>It is wider than a block, so it is built like the other machines: the
 * plate is the controller, and each column stands in a block of its own, two
 * high, that can be aimed at, clicked and broken like the plate. The parts
 * carry the lift's height as well, so a deck that rises through them is
 * where the cursor finds it.
 */
public class VehicleLiftBlock extends MultiblockBlock {

    public static final MapCodec<VehicleLiftBlock> CODEC = simpleCodec(VehicleLiftBlock::new);

    /** Quarter-blocks of travel, 0 being flat on the floor. */
    public static final IntegerProperty LEVEL =
            IntegerProperty.create("level", 0, VehicleLiftBlockEntity.MAX_LEVEL);

    /** The plate, the column either side of it, and the tops of the two columns. */
    public static final IntegerProperty PART = IntegerProperty.create("part", 0, 4);

    private static final int[][] CELLS = {{0, 0, 0}, {-1, 0, 0}, {1, 0, 0}, {-1, 1, 0}, {1, 1, 0}};

    /**
     * How far from a lift a car still counts as being on it. The car is 2.5
     * blocks long and nobody parks to the block, so this is deliberately
     * forgiving: anywhere over the ramp or a block either side of it.
     */
    private static final int REACH = 2;

    /**
     * What of the lift a body bumps into, in pixels with the lift facing
     * north, one box a row: the plate a car is driven on to, the two
     * columns, and the power unit on the east one. The arms are only drawn:
     * the car is held by the lift itself, not stood on a shape, and the space
     * under a raised car is left clear to walk into.
     */
    private static final double[][] SOLID = {
            {0, 0, 0, 16, 2, 16},
            {-15.2, 0, 4.8, -10.3, 31.6, 11.2},
            {26.3, 0, 4.8, 31.2, 31.6, 11.2},
            {27.6, 16, 11, 30.8, 25.2, 14.4}};

    /** And what the cursor finds: that, the columns' feet and the cable covers a wheel rolls over. */
    private static final double[][] OUTLINE = java.util.stream.Stream.concat(java.util.Arrays.stream(SOLID),
            java.util.stream.Stream.of(
                    new double[] {-16, 0, 2.6, -9, 1.1, 13.4},
                    new double[] {25, 0, 2.6, 32, 1.1, 13.4},
                    new double[] {-7.2, 0, 6.6, 0, 0.4, 9.4},
                    new double[] {16, 0, 6.6, 23.2, 0.4, 9.4})).toArray(double[][]::new);

    private final Map<BlockState, VoxelShape> outlines = new IdentityHashMap<>();
    private final Map<BlockState, VoxelShape> solids = new IdentityHashMap<>();

    public VehicleLiftBlock(Properties settings) {
        super(settings);
        this.registerDefaultState(this.getStateDefinition().any().setValue(LEVEL, 0)
                .setValue(FACING, Direction.NORTH).setValue(this.partProperty(), 0));
        // every cell's share of the lift, at every height and facing, worked out once
        for (BlockState state : this.getStateDefinition().getPossibleStates()) {
            int[] cell = this.cells()[this.partOf(state)];
            Direction facing = state.getValue(FACING);
            int level = state.getValue(LEVEL);
            this.outlines.put(state, this.cut(this.drawing(level, false), cell, facing));
            this.solids.put(state, this.cut(this.drawing(level, true), cell, facing));
        }
    }

    @Override
    protected MapCodec<? extends BaseEntityBlock> codec() {
        return CODEC;
    }

    @Override
    public int[][] cells() {
        return CELLS;
    }

    @Override
    public IntegerProperty partProperty() {
        return PART;
    }

    /** A lift has no light to put on; it has its height instead. */
    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING, LEVEL, this.partProperty());
    }

    /**
     * The lift as it is drawn facing north, in pixels from the plate's corner,
     * at a height: what the cursor finds on it, or with {@code solid} what a
     * body bumps into. It is cut up between the cells by {@link #cut}.
     */
    protected double[][] drawing(int level, boolean solid) {
        return solid ? SOLID : OUTLINE;
    }

    /**
     * One cell's share of the drawing, in that cell's own pixels and turned
     * to the facing. A cell with no cell of the lift above it keeps whatever
     * rises out of its top, such as a deck going up, so the cursor still
     * finds that from above.
     */
    private VoxelShape cut(double[][] boxes, int[] cell, Direction facing) {
        boolean roofed = false;
        for (int[] other : this.cells()) {
            roofed |= other[0] == cell[0] && other[1] == cell[1] + 1 && other[2] == cell[2];
        }
        double ox = cell[0] * 16.0;
        double oy = cell[1] * 16.0;
        double oz = cell[2] * 16.0;
        VoxelShape shape = Shapes.empty();
        for (double[] box : boxes) {
            double x1 = Math.max(box[0], ox) - ox;
            double x2 = Math.min(box[3], ox + 16.0) - ox;
            double y1 = Math.max(box[1], oy) - oy;
            double y2 = (roofed ? Math.min(box[4], oy + 16.0) : box[4]) - oy;
            double z1 = Math.max(box[2], oz) - oz;
            double z2 = Math.min(box[5], oz + 16.0) - oz;
            if (x1 < x2 && y1 < y2 && z1 < z2) {
                shape = Shapes.or(shape, turned(facing, x1, y1, z1, x2, y2, z2));
            }
        }
        return shape.optimize();
    }

    /** A box drawn with the lift facing north, turned to {@code facing} about the middle of its block. */
    private static VoxelShape turned(Direction facing, double x1, double y1, double z1, double x2, double y2, double z2) {
        // each quarter turn clockwise takes (x, z) to (16 - z, x), as the blockstate turns the model
        for (int turn = 0; turn < ((facing.get2DDataValue() + 2) & 3); turn++) {
            double nx1 = 16.0 - z2;
            double nx2 = 16.0 - z1;
            z1 = x1;
            z2 = x2;
            x1 = nx1;
            x2 = nx2;
        }
        return Block.box(x1, y1, z1, x2, y2, z2);
    }

    /**
     * The platform is longer than the block — a car is — so it is laid along
     * the way the player was looking when it went down, which is the way a
     * car will be driven on to it.
     */
    @Override
    protected Direction facingFor(BlockPlaceContext context) {
        return context.getHorizontalDirection();
    }

    /**
     * Puts down whichever part is missing: around a lift placed by hand, one
     * set down by a command, or one standing in a world from before its
     * columns were blocks of their own. A cell something else already stands
     * in is left alone; that lift goes without a part there rather than
     * knocking a wall down.
     */
    @Override
    public void placeParts(Level world, BlockPos pos, BlockState controller) {
        Direction facing = controller.getValue(FACING);
        int[][] cells = this.cells();
        for (int i = 1; i < cells.length; i++) {
            BlockPos at = cellPos(pos, facing, cells[i]);
            BlockState there = world.getBlockState(at);
            if (!this.isPart(there, i, facing) && there.canBeReplaced() && world.isInWorldBounds(at)) {
                world.setBlock(at, controller.setValue(this.partProperty(), i), Block.UPDATE_ALL);
            }
        }
    }

    /** Takes the lift, every cell of it, to a height. */
    public void setLevel(Level world, BlockPos controller, BlockState state, int level) {
        this.setOnEveryCell(world, controller, state, LEVEL, level);
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter world, BlockPos pos, CollisionContext context) {
        return this.outlines.get(state);
    }

    @Override
    protected VoxelShape getCollisionShape(BlockState state, BlockGetter world, BlockPos pos, CollisionContext context) {
        return this.solids.get(state);
    }

    @Override
    public @Nullable BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return this.isController(state) ? new VehicleLiftBlockEntity(pos, state) : null;
    }

    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(
            Level world, BlockState state, BlockEntityType<T> type) {
        if (world.isClientSide()) {
            return null;
        }
        return createTickerHelper(type, VehicleLiftBlockEntity.forBikes(state)
                ? ModBlockEntities.MOTORCYCLE_LIFT : ModBlockEntities.VEHICLE_LIFT, VehicleLiftBlockEntity::tick);
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level world, BlockPos pos,
                                 Player player, BlockHitResult hit) {
        if (world.isClientSide()) {
            return InteractionResult.SUCCESS;
        }
        return working(world, this.controllerOf(pos, state), player);
    }

    /**
     * A click is the lift's lever: up a stop, or down one when sneaking. It
     * has no window.
     */
    private static InteractionResult working(Level world, BlockPos pos, Player player) {
        if (!(world.getBlockEntity(pos) instanceof VehicleLiftBlockEntity lift)) {
            return InteractionResult.CONSUME;
        }
        boolean up = !player.isShiftKeyDown();
        int before = lift.getStop();
        int stop = lift.step(up);
        world.playSound(null, pos, SoundEvents.LEVER_CLICK, SoundSource.BLOCKS, 0.4F, up ? 1.1F : 0.8F);
        Component where = Component.translatable("message.overthought_vehicles.lift_stop_" + stop);
        player.displayClientMessage(stop == before
                ? Component.translatable(up ? "message.overthought_vehicles.lift_at_top" : "message.overthought_vehicles.lift_at_bottom", where)
                : Component.translatable("message.overthought_vehicles.lift_stop", where), true);
        return InteractionResult.CONSUME;
    }

    /**
     * True when a lift under a vehicle has it up in the air on its platform;
     * a car parked beside a raised lift is not on it. A car on its wheels is a car you cannot
     * get under, which is the point of owning the lift in the first place.
     * A boat or a plane, which no lift takes, is up on its cradle or jack.
     */
    public static boolean isServicing(Level world, Entity vehicle) {
        if (vehicle instanceof me.trossty.car.entity.CarEntity car && ServiceStandBlock.isServicing(world, car)) {
            return true;
        }
        BlockPos at = vehicle.blockPosition();
        for (BlockPos pos : BlockPos.betweenClosed(
                at.offset(-REACH, -3, -REACH), at.offset(REACH, 0, REACH))) {
            BlockState state = world.getBlockState(pos);
            if (state.getBlock() instanceof VehicleLiftBlock lift && lift.isController(state)
                    && VehicleLiftBlockEntity.holdsUp(state, pos, vehicle)) {
                return true;
            }
        }
        return false;
    }

    /** What to tell somebody working on a vehicle that is not up where it can be. */
    public static Component needsLift(me.trossty.car.entity.CarEntity vehicle) {
        return Component.translatable(switch (vehicle.chassis()) {
            case BOAT -> "message.overthought_vehicles.needs_cradle";
            case PLANE -> "message.overthought_vehicles.needs_jack";
            case MOTORCYCLE -> "message.overthought_vehicles.needs_bike_lift";
            default -> "message.overthought_vehicles.needs_lift";
        });
    }
}
