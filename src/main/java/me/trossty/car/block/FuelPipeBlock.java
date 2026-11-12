package me.trossty.car.block;

import com.mojang.serialization.MapCodec;
import me.trossty.car.block.entity.FuelPipeBlockEntity;
import me.trossty.car.registry.ModBlockEntities;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.BlockWithEntity;
import net.minecraft.block.ConnectingBlock;
import net.minecraft.block.ShapeContext;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.block.entity.BlockEntityTicker;
import net.minecraft.block.entity.BlockEntityType;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemPlacementContext;
import net.minecraft.state.StateManager;
import net.minecraft.state.property.BooleanProperty;
import net.minecraft.text.Text;
import net.minecraft.util.ActionResult;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.random.Random;
import net.minecraft.util.shape.VoxelShape;
import net.minecraft.util.shape.VoxelShapes;
import net.minecraft.world.BlockView;
import net.minecraft.world.World;
import net.minecraft.world.WorldView;
import net.minecraft.world.tick.ScheduledTickView;

/**
 * The fuel line between a refinery and a pump.
 *
 * <p>It is shaped like a pipe and behaves like one: it grows an arm towards
 * every neighbour worth joining — another pipe, a refinery, either half of a
 * pump — so a run of them reads as plumbing rather than as a row of boxes. The
 * six connection flags are only the look and the hitbox; what actually moves is
 * decided tick by tick in {@link FuelPipeBlockEntity}.
 *
 * <p>Right-clicking one says how much petrol is standing in that segment, which
 * is the only way to see a line working from the outside.
 */
public class FuelPipeBlock extends BlockWithEntity {

    public static final MapCodec<FuelPipeBlock> CODEC = createCodec(FuelPipeBlock::new);

    /**
     * Whether this pipe wears a fitting: everywhere except the middle of a
     * straight run, so a line looks like one tube and the collars mark the
     * places something happens — a bend, a tee, an end, a machine.
     */
    public static final BooleanProperty FITTING = BooleanProperty.of("fitting");

    /** Half-width of the pipe, in sixteenths. A 6x6 bore, centred. */
    private static final double EDGE = 5.0;
    private static final double FAR = 16.0 - EDGE;

    private static final VoxelShape CORE =
            Block.createCuboidShape(EDGE, EDGE, EDGE, FAR, FAR, FAR);

    /** One shape per combination of the six arms, built once at registration. */
    private final VoxelShape[] shapes = buildShapes();

    public FuelPipeBlock(Settings settings) {
        super(settings);
        BlockState state = this.getStateManager().getDefaultState();
        for (BooleanProperty connected : ConnectingBlock.FACING_PROPERTIES.values()) {
            state = state.with(connected, false);
        }
        this.setDefaultState(state.with(FITTING, true));
    }

    @Override
    protected MapCodec<? extends BlockWithEntity> getCodec() {
        return CODEC;
    }

    @Override
    protected void appendProperties(StateManager.Builder<Block, BlockState> builder) {
        for (BooleanProperty connected : ConnectingBlock.FACING_PROPERTIES.values()) {
            builder.add(connected);
        }
        builder.add(FITTING);
    }

    // ---- joining up ----

    /**
     * What a pipe will reach out to. Anything that holds petrol: its own kind,
     * the refinery that makes it and the pump that hands it out. Notably not
     * solid blocks — a pipe through a wall should look like a pipe through a
     * wall, not like it has latched onto the stone.
     */
    private static boolean connectsTo(WorldView world, BlockPos pos, Direction side) {
        BlockState state = world.getBlockState(pos);
        if (me.trossty.car.fluid.ForeignFluids.isOurs(state.getBlock())) {
            return true;
        }
        // Anything from another mod with a fluid door on this face: its pipes,
        // its tanks, its machines.
        return me.trossty.car.fluid.ForeignFluids.connects(world, pos, side);
    }

    private static BlockState connect(BlockState state, WorldView world, BlockPos pos) {
        BlockState joined = state;
        for (Direction side : Direction.values()) {
            joined = joined.with(ConnectingBlock.FACING_PROPERTIES.get(side),
                    connectsTo(world, pos.offset(side), side));
        }
        return withFitting(joined);
    }

    private static BlockState withFitting(BlockState state) {
        int count = 0;
        Direction first = null;
        Direction second = null;
        for (Direction side : Direction.values()) {
            if (state.get(ConnectingBlock.FACING_PROPERTIES.get(side))) {
                count++;
                if (first == null) {
                    first = side;
                } else {
                    second = side;
                }
            }
        }
        boolean straight = count == 2 && first.getOpposite() == second;
        return state.with(FITTING, !straight);
    }

    @Override
    public BlockState getPlacementState(ItemPlacementContext context) {
        return connect(this.getDefaultState(), context.getWorld(), context.getBlockPos());
    }

    @Override
    protected BlockState getStateForNeighborUpdate(BlockState state, WorldView world,
                                                   ScheduledTickView tickView, BlockPos pos,
                                                   Direction direction, BlockPos neighborPos,
                                                   BlockState neighborState, Random random) {
        return withFitting(state.with(ConnectingBlock.FACING_PROPERTIES.get(direction),
                connectsTo(world, neighborPos, direction)));
    }

    // ---- the hitbox follows the arms ----

    private static VoxelShape[] buildShapes() {
        VoxelShape[] arms = new VoxelShape[Direction.values().length];
        for (Direction side : Direction.values()) {
            double x = side.getOffsetX();
            double y = side.getOffsetY();
            double z = side.getOffsetZ();
            // An arm runs from the core out to the face it points at.
            arms[side.ordinal()] = Block.createCuboidShape(
                    armMin(x), armMin(y), armMin(z), armMax(x), armMax(y), armMax(z));
        }

        VoxelShape[] shapes = new VoxelShape[1 << arms.length];
        for (int mask = 0; mask < shapes.length; mask++) {
            VoxelShape shape = CORE;
            for (Direction side : Direction.values()) {
                if ((mask & (1 << side.ordinal())) != 0) {
                    shape = VoxelShapes.union(shape, arms[side.ordinal()]);
                }
            }
            shapes[mask] = shape;
        }
        return shapes;
    }

    private static double armMin(double offset) {
        return offset < 0 ? 0.0 : EDGE;
    }

    private static double armMax(double offset) {
        return offset > 0 ? 16.0 : FAR;
    }

    private static int mask(BlockState state) {
        int mask = 0;
        for (Direction side : Direction.values()) {
            if (state.get(ConnectingBlock.FACING_PROPERTIES.get(side))) {
                mask |= 1 << side.ordinal();
            }
        }
        return mask;
    }

    @Override
    protected VoxelShape getOutlineShape(BlockState state, BlockView world, BlockPos pos, ShapeContext context) {
        return this.shapes[mask(state)];
    }

    @Override
    protected VoxelShape getCollisionShape(BlockState state, BlockView world, BlockPos pos, ShapeContext context) {
        return this.shapes[mask(state)];
    }

    // ---- what is in it ----

    @Override
    public BlockEntity createBlockEntity(BlockPos pos, BlockState state) {
        return new FuelPipeBlockEntity(pos, state);
    }

    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(
            World world, BlockState state, BlockEntityType<T> type) {
        if (world.isClient()) {
            return null;
        }
        return validateTicker(type, ModBlockEntities.FUEL_PIPE, FuelPipeBlockEntity::tick);
    }

    /** A pipe has no window; knocking on it says what is standing inside. */
    @Override
    protected ActionResult onUse(BlockState state, World world, BlockPos pos,
                                 PlayerEntity player, BlockHitResult hit) {
        if (world.isClient()) {
            return ActionResult.SUCCESS;
        }
        if (world.getBlockEntity(pos) instanceof FuelPipeBlockEntity pipe) {
            player.sendMessage(Text.translatable("message.car.pipe_level",
                    pipe.getPetrol(), pipe.getCapacity()), true);
        }
        return ActionResult.CONSUME;
    }

    // ---- redstone ----

    @Override
    protected boolean hasComparatorOutput(BlockState state) {
        return true;
    }

    @Override
    protected int getComparatorOutput(BlockState state, World world, BlockPos pos, Direction direction) {
        if (!(world.getBlockEntity(pos) instanceof FuelPipeBlockEntity pipe) || pipe.getPetrol() <= 0) {
            return 0;
        }
        return MathHelper.clamp(1 + pipe.getPetrol() * 14 / pipe.getCapacity(), 1, 15);
    }
}
