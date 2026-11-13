package me.trossty.car.block;

import com.mojang.serialization.MapCodec;
import java.util.EnumMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import me.trossty.car.block.entity.EnergyCableBlockEntity;
import me.trossty.car.registry.ModBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.util.StringRepresentable;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * Power line: an insulated cable from a generator to the machines it runs.
 *
 * <p>Laid like the fuel pipe and shaped like one, only thinner: it reaches an
 * arm towards every other cable and every block with a power door on that
 * face — this mod's generators and machines, and any other mod's cables,
 * batteries and machines that speak the same energy API. Where it meets a
 * machine rather than more cable it ends in a flat terminal plate bolted to
 * the casing, so it reads as plugged in rather than just touching. A copper
 * junction marks the bends, tees and ends. What moves is decided in
 * {@link EnergyCableBlockEntity}; right-clicking says what is in that length.
 */
public class EnergyCableBlock extends ModelEntityBlock {

    public static final MapCodec<EnergyCableBlock> CODEC = simpleCodec(EnergyCableBlock::new);

    /** What a side of a cable meets. */
    public enum Side implements StringRepresentable {
        NONE("none"),
        CABLE("cable"),
        DEVICE("device");

        private final String name;

        Side(String name) {
            this.name = name;
        }

        @Override
        public String getSerializedName() {
            return this.name;
        }

        public boolean connected() {
            return this != NONE;
        }
    }

    public static final Map<Direction, EnumProperty<Side>> SIDES = new EnumMap<>(Direction.class);

    static {
        for (Direction side : Direction.values()) {
            SIDES.put(side, EnumProperty.create(side.getSerializedName(), Side.class));
        }
    }

    /** Whether this length wears a junction: everywhere but the middle of a straight run. */
    public static final BooleanProperty FITTING = BooleanProperty.create("fitting");

    /** Lit while the run carries power: a thin copper stripe glows along it. */
    public static final BooleanProperty POWERED = net.minecraft.world.level.block.state.properties.BlockStateProperties.POWERED;

    /** Where the cable starts, in sixteenths: a 4x4 run, centred. */
    private static final double EDGE = 6.0;
    private static final double FAR = 16.0 - EDGE;

    /** The terminal plate: 10x10, a sixteenth and a half thick, against the machine. */
    private static final double PLATE_EDGE = 3.0;
    private static final double PLATE_DEPTH = 1.5;

    private static final VoxelShape CORE = Block.box(EDGE, EDGE, EDGE, FAR, FAR, FAR);
    private static final VoxelShape[] ARMS = new VoxelShape[6];
    private static final VoxelShape[] PLATES = new VoxelShape[6];

    static {
        for (Direction side : Direction.values()) {
            double x = side.getStepX();
            double y = side.getStepY();
            double z = side.getStepZ();
            ARMS[side.ordinal()] = Block.box(
                    armMin(x), armMin(y), armMin(z), armMax(x), armMax(y), armMax(z));
            PLATES[side.ordinal()] = Block.box(
                    plateMin(x), plateMin(y), plateMin(z), plateMax(x), plateMax(y), plateMax(z));
        }
    }

    /** Shapes by the twelve bits of arms and plates, made when first needed. */
    private static final Map<Integer, VoxelShape> SHAPES = new ConcurrentHashMap<>();

    public EnergyCableBlock(Properties settings) {
        super(settings);
        BlockState state = this.getStateDefinition().any();
        for (EnumProperty<Side> side : SIDES.values()) {
            state = state.setValue(side, Side.NONE);
        }
        this.registerDefaultState(state.setValue(FITTING, true).setValue(POWERED, false));
    }

    @Override
    protected MapCodec<? extends BaseEntityBlock> codec() {
        return CODEC;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        for (Direction side : Direction.values()) {
            builder.add(SIDES.get(side));
        }
        builder.add(FITTING, POWERED);
    }

    // ---- joining up ----

    /** What a cable meets on one side: more cable, a machine with a power door there, or nothing. */
    private static Side sideTowards(LevelReader world, BlockPos pos, Direction side) {
        BlockState state = world.getBlockState(pos);
        if (state.getBlock() instanceof EnergyCableBlock) {
            return Side.CABLE;
        }
        if (world instanceof Level level
                && level.getCapability(net.neoforged.neoforge.capabilities.Capabilities.EnergyStorage.BLOCK, pos, side.getOpposite()) != null) {
            return Side.DEVICE;
        }
        return Side.NONE;
    }

    private static BlockState connect(BlockState state, LevelReader world, BlockPos pos) {
        BlockState joined = state;
        for (Direction side : Direction.values()) {
            joined = joined.setValue(SIDES.get(side), sideTowards(world, pos.relative(side), side));
        }
        return withFitting(joined);
    }

    private static BlockState withFitting(BlockState state) {
        int count = 0;
        Direction first = null;
        Direction second = null;
        for (Direction side : Direction.values()) {
            if (state.getValue(SIDES.get(side)).connected()) {
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
        return withFitting(state.setValue(SIDES.get(direction), sideTowards(world, neighborPos, direction)));
    }

    // ---- the hitbox follows the arms and plates ----

    private static double armMin(double offset) {
        return offset < 0 ? 0.0 : EDGE;
    }

    private static double armMax(double offset) {
        return offset > 0 ? 16.0 : FAR;
    }

    private static double plateMin(double offset) {
        return offset < 0 ? 0.0 : offset > 0 ? 16.0 - PLATE_DEPTH : PLATE_EDGE;
    }

    private static double plateMax(double offset) {
        return offset > 0 ? 16.0 : offset < 0 ? PLATE_DEPTH : 16.0 - PLATE_EDGE;
    }

    private static VoxelShape shapeOf(BlockState state) {
        int key = 0;
        for (Direction side : Direction.values()) {
            Side meets = state.getValue(SIDES.get(side));
            if (meets.connected()) {
                key |= 1 << side.ordinal();
            }
            if (meets == Side.DEVICE) {
                key |= 1 << (side.ordinal() + 6);
            }
        }
        return SHAPES.computeIfAbsent(key, bits -> {
            VoxelShape shape = CORE;
            for (Direction side : Direction.values()) {
                if ((bits & (1 << side.ordinal())) != 0) {
                    shape = Shapes.or(shape, ARMS[side.ordinal()]);
                }
                if ((bits & (1 << (side.ordinal() + 6))) != 0) {
                    shape = Shapes.or(shape, PLATES[side.ordinal()]);
                }
            }
            return shape;
        });
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter world, BlockPos pos, CollisionContext context) {
        return shapeOf(state);
    }

    @Override
    protected VoxelShape getCollisionShape(BlockState state, BlockGetter world, BlockPos pos, CollisionContext context) {
        return shapeOf(state);
    }

    // ---- what is in it ----

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new EnergyCableBlockEntity(pos, state);
    }

    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(
            Level world, BlockState state, BlockEntityType<T> type) {
        if (world.isClientSide()) {
            return null;
        }
        return createTickerHelper(type, ModBlockEntities.ENERGY_CABLE, EnergyCableBlockEntity::tick);
    }

    /** Right-clicking a length says how much charge is standing in it. */
    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level world, BlockPos pos,
                                 Player player, BlockHitResult hit) {
        if (world.isClientSide()) {
            return InteractionResult.SUCCESS;
        }
        if (world.getBlockEntity(pos) instanceof EnergyCableBlockEntity cable) {
            player.displayClientMessage(Component.translatable("message.car.cable_level",
                    cable.getEnergy(), EnergyCableBlockEntity.CAPACITY), true);
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
        if (!(world.getBlockEntity(pos) instanceof EnergyCableBlockEntity cable) || cable.getEnergy() <= 0) {
            return 0;
        }
        return Mth.clamp(1 + cable.getEnergy() * 14 / EnergyCableBlockEntity.CAPACITY, 1, 15);
    }
}
