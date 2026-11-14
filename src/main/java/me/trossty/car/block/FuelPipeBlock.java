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
 * <p>Sneak and right-click one to see what is standing in it; right-click an
 * arm with a wrench to cut it or join it again.
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
     * What a pipe will reach out to: anything that holds a liquid, unless a
     * wrench has cut that arm. Notably not
     * solid blocks — a pipe through a wall should look like a pipe through a
     * wall, not like it has latched onto the stone.
     */
    private static boolean connectsTo(LevelReader world, BlockPos pos, Direction side) {
        if (world.getBlockEntity(pos) instanceof FuelPipeBlockEntity self && self.isBlocked(side)) {
            return false;
        }
        BlockPos next = pos.relative(side);
        BlockState state = world.getBlockState(next);
        if (world.getBlockEntity(next) instanceof FuelPipeBlockEntity other && other.isBlocked(side.getOpposite())) {
            return false;
        }
        if (state.getBlock() instanceof FuelValveBlock) {
            // A valve only has two ends: in at the back, out at the front.
            return state.getValue(FuelValveBlock.FACING).getAxis() == side.getAxis();
        }
        if (me.trossty.car.fluid.ForeignFluids.isOurs(state.getBlock())) {
            return true;
        }
        // Anything with a fluid door on this face: the mod's machines, and
        // other mods' pipes, tanks and machines.
        return me.trossty.car.fluid.ForeignFluids.connects(world, next, side);
    }

    static BlockState connect(BlockState state, LevelReader world, BlockPos pos) {
        BlockState joined = state;
        for (Direction side : Direction.values()) {
            joined = joined.setValue(PipeBlock.PROPERTY_BY_DIRECTION.get(side), connectsTo(world, pos, side));
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
                connectsTo(world, pos, direction)));
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
        return createTickerHelper(type, ModBlockEntities.FUEL_PIPE, me.trossty.car.network.NetworkBlockEntity::tick);
    }

    /**
     * A wrench cuts the arm you click, or joins it again — the way to keep two
     * lines carrying different liquids apart where they run side by side.
     */
    @Override
    protected net.minecraft.world.ItemInteractionResult useItemOn(net.minecraft.world.item.ItemStack held, BlockState state,
            Level world, BlockPos pos, Player player, net.minecraft.world.InteractionHand hand, BlockHitResult hit) {
        if (!held.is(me.trossty.car.registry.ModItems.WRENCH)) {
            return net.minecraft.world.ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
        }
        if (world.isClientSide()) {
            return net.minecraft.world.ItemInteractionResult.SUCCESS;
        }
        if (!(world.getBlockEntity(pos) instanceof FuelPipeBlockEntity pipe)) {
            return net.minecraft.world.ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
        }
        Direction side = clickedArm(pos, hit);
        boolean cut;
        if (pipe.isBlocked(side)) {
            pipe.setBlocked(side, false);
            cut = false;
        } else if (state.getValue(PipeBlock.PROPERTY_BY_DIRECTION.get(side))) {
            pipe.setBlocked(side, true);
            cut = true;
        } else {
            return net.minecraft.world.ItemInteractionResult.CONSUME;
        }
        BlockState joined = connect(state, world, pos);
        world.setBlock(pos, joined, Block.UPDATE_ALL);
        world.sendBlockUpdated(pos, state, joined, Block.UPDATE_ALL);
        BlockPos next = pos.relative(side);
        BlockState there = world.getBlockState(next);
        if (there.getBlock() instanceof FuelPipeBlock) {
            world.setBlock(next, connect(there, world, next), Block.UPDATE_ALL);
        }
        world.playSound(null, pos, net.minecraft.sounds.SoundEvents.IRON_TRAPDOOR_CLOSE,
                net.minecraft.sounds.SoundSource.BLOCKS, 0.5F, cut ? 1.4F : 1.8F);
        player.displayClientMessage(Component.translatable(cut ? "message.car.pipe_cut" : "message.car.pipe_joined"), true);
        return net.minecraft.world.ItemInteractionResult.CONSUME;
    }

    /** The arm under the cursor: whichever way the hit point lies furthest from the middle. */
    private static Direction clickedArm(BlockPos pos, BlockHitResult hit) {
        net.minecraft.world.phys.Vec3 at = hit.getLocation().subtract(pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5);
        return Direction.getNearest(at.x, at.y, at.z);
    }

    /** Sneaking and knocking on a pipe says what the line is carrying. */
    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level world, BlockPos pos,
                                 Player player, BlockHitResult hit) {
        if (!player.isShiftKeyDown()) {
            return InteractionResult.PASS;
        }
        if (world.isClientSide()) {
            return InteractionResult.SUCCESS;
        }
        if (world.getBlockEntity(pos) instanceof FuelPipeBlockEntity pipe) {
            player.displayClientMessage(pipe.getFlow() <= 0
                    ? Component.translatable("message.car.pipe_empty")
                    : Component.translatable("message.car.pipe_flow",
                            me.trossty.car.fluid.Liquids.name(pipe.getFluid()), pipe.getFlow()), true);
        }
        return InteractionResult.CONSUME;
    }
}
