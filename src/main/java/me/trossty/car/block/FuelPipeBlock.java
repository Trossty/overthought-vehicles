package me.trossty.car.block;

import com.mojang.serialization.MapCodec;
import me.trossty.car.block.entity.FuelPipeBlockEntity;
import me.trossty.car.registry.ModBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
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
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

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
public class FuelPipeBlock extends ModelEntityBlock {

    public static final MapCodec<FuelPipeBlock> CODEC = simpleCodec(FuelPipeBlock::new);

    /**
     * Whether this pipe wears a fitting: everywhere except the middle of a
     * straight run, so a line looks like one tube and the collars mark the
     * places something happens — a bend, a tee, an end, a machine.
     */
    public static final BooleanProperty FITTING = BooleanProperty.create("fitting");

    /** Half-width of the pipe, in sixteenths. A 6x6 bore, centred. */
    private static final double EDGE = 5.0;
    private static final double FAR = 16.0 - EDGE;

    private static final VoxelShape CORE =
            Block.box(EDGE, EDGE, EDGE, FAR, FAR, FAR);

    /** One shape per combination of the six arms, built once at registration. */
    private final VoxelShape[] shapes = buildShapes();

    public FuelPipeBlock(Properties settings) {
        super(settings);
        BlockState state = this.getStateDefinition().any();
        for (BooleanProperty connected : PipeBlock.PROPERTY_BY_DIRECTION.values()) {
            state = state.setValue(connected, false);
        }
        this.registerDefaultState(state.setValue(FITTING, true));
    }

    @Override
    protected MapCodec<? extends BaseEntityBlock> codec() {
        return CODEC;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        for (BooleanProperty connected : PipeBlock.PROPERTY_BY_DIRECTION.values()) {
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
    private static boolean connectsTo(LevelReader world, BlockPos pos, Direction side) {
        BlockState state = world.getBlockState(pos);
        if (me.trossty.car.fluid.ForeignFluids.isOurs(state.getBlock())) {
            return true;
        }
        // Anything from another mod with a fluid door on this face: its pipes,
        // its tanks, its machines.
        return me.trossty.car.fluid.ForeignFluids.connects(world, pos, side);
    }

    private static BlockState connect(BlockState state, LevelReader world, BlockPos pos) {
        BlockState joined = state;
        for (Direction side : Direction.values()) {
            joined = joined.setValue(PipeBlock.PROPERTY_BY_DIRECTION.get(side),
                    connectsTo(world, pos.relative(side), side));
        }
        return withFitting(joined);
    }

    private static BlockState withFitting(BlockState state) {
        int count = 0;
        Direction first = null;
        Direction second = null;
        for (Direction side : Direction.values()) {
            if (state.getValue(PipeBlock.PROPERTY_BY_DIRECTION.get(side))) {
                count++;
                if (first == null) {
                    first = side;
                } else {
                    second = side;
                }
            }
        }
        boolean straight = count == 2 && first.getOpposite() == second;
        return state.setValue(FITTING, !straight);
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        return connect(this.defaultBlockState(), context.getLevel(), context.getClickedPos());
    }

    @Override
    protected BlockState updateShape(BlockState state, Direction direction, BlockState neighborState,
                                     net.minecraft.world.level.LevelAccessor world, BlockPos pos, BlockPos neighborPos) {
        return withFitting(state.setValue(PipeBlock.PROPERTY_BY_DIRECTION.get(direction),
                connectsTo(world, neighborPos, direction)));
    }

    // ---- the hitbox follows the arms ----

    private static VoxelShape[] buildShapes() {
        VoxelShape[] arms = new VoxelShape[Direction.values().length];
        for (Direction side : Direction.values()) {
            double x = side.getStepX();
            double y = side.getStepY();
            double z = side.getStepZ();
            // An arm runs from the core out to the face it points at.
            arms[side.ordinal()] = Block.box(
                    armMin(x), armMin(y), armMin(z), armMax(x), armMax(y), armMax(z));
        }

        VoxelShape[] shapes = new VoxelShape[1 << arms.length];
        for (int mask = 0; mask < shapes.length; mask++) {
            VoxelShape shape = CORE;
            for (Direction side : Direction.values()) {
                if ((mask & (1 << side.ordinal())) != 0) {
                    shape = Shapes.or(shape, arms[side.ordinal()]);
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
            if (state.getValue(PipeBlock.PROPERTY_BY_DIRECTION.get(side))) {
                mask |= 1 << side.ordinal();
            }
        }
        return mask;
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter world, BlockPos pos, CollisionContext context) {
        return this.shapes[mask(state)];
    }

    @Override
    protected VoxelShape getCollisionShape(BlockState state, BlockGetter world, BlockPos pos, CollisionContext context) {
        return this.shapes[mask(state)];
    }

    // ---- what is in it ----

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new FuelPipeBlockEntity(pos, state);
    }

    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(
            Level world, BlockState state, BlockEntityType<T> type) {
        if (world.isClientSide()) {
            return null;
        }
        return createTickerHelper(type, ModBlockEntities.FUEL_PIPE, FuelPipeBlockEntity::tick);
    }

    /** A pipe has no window; knocking on it says what is standing inside. */
    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level world, BlockPos pos,
                                 Player player, BlockHitResult hit) {
        if (world.isClientSide()) {
            return InteractionResult.SUCCESS;
        }
        if (world.getBlockEntity(pos) instanceof FuelPipeBlockEntity pipe) {
            player.displayClientMessage(Component.translatable("message.car.pipe_level",
                    pipe.getPetrol(), pipe.getCapacity()), true);
        }
        return InteractionResult.CONSUME;
    }

    // ---- redstone ----

    @Override
    protected boolean hasAnalogOutputSignal(BlockState state) {
        return true;
    }

    @Override
    protected int getAnalogOutputSignal(BlockState state, Level world, BlockPos pos) {
        if (!(world.getBlockEntity(pos) instanceof FuelPipeBlockEntity pipe) || pipe.getPetrol() <= 0) {
            return 0;
        }
        return Mth.clamp(1 + pipe.getPetrol() * 14 / pipe.getCapacity(), 1, 15);
    }
}
