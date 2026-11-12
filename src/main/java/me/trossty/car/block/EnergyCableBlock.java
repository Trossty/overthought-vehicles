package me.trossty.car.block;

import com.mojang.serialization.MapCodec;
import java.util.EnumMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import me.trossty.car.block.entity.EnergyCableBlockEntity;
import me.trossty.car.registry.ModBlockEntities;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.BlockWithEntity;
import net.minecraft.block.ShapeContext;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.block.entity.BlockEntityTicker;
import net.minecraft.block.entity.BlockEntityType;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemPlacementContext;
import net.minecraft.state.StateManager;
import net.minecraft.state.property.BooleanProperty;
import net.minecraft.state.property.EnumProperty;
import net.minecraft.text.Text;
import net.minecraft.util.ActionResult;
import net.minecraft.util.StringIdentifiable;
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
public class EnergyCableBlock extends BlockWithEntity {

    public static final MapCodec<EnergyCableBlock> CODEC = createCodec(EnergyCableBlock::new);

    /** What a side of a cable meets. */
    public enum Side implements StringIdentifiable {
        NONE("none"),
        CABLE("cable"),
        DEVICE("device");

        private final String name;

        Side(String name) {
            this.name = name;
        }

        @Override
        public String asString() {
            return this.name;
        }

        public boolean connected() {
            return this != NONE;
        }
    }

    public static final Map<Direction, EnumProperty<Side>> SIDES = new EnumMap<>(Direction.class);

    static {
        for (Direction side : Direction.values()) {
            SIDES.put(side, EnumProperty.of(side.asString(), Side.class));
        }
    }

    /** Whether this length wears a junction: everywhere but the middle of a straight run. */
    public static final BooleanProperty FITTING = BooleanProperty.of("fitting");

    /** Lit while the run carries power: a thin copper stripe glows along it. */
    public static final BooleanProperty POWERED = net.minecraft.state.property.Properties.POWERED;

    /** Where the cable starts, in sixteenths: a 4x4 run, centred. */
    private static final double EDGE = 6.0;
    private static final double FAR = 16.0 - EDGE;

    /** The terminal plate: 10x10, a sixteenth and a half thick, against the machine. */
    private static final double PLATE_EDGE = 3.0;
    private static final double PLATE_DEPTH = 1.5;

    private static final VoxelShape CORE = Block.createCuboidShape(EDGE, EDGE, EDGE, FAR, FAR, FAR);
    private static final VoxelShape[] ARMS = new VoxelShape[6];
    private static final VoxelShape[] PLATES = new VoxelShape[6];

    static {
        for (Direction side : Direction.values()) {
            double x = side.getOffsetX();
            double y = side.getOffsetY();
            double z = side.getOffsetZ();
            ARMS[side.ordinal()] = Block.createCuboidShape(
                    armMin(x), armMin(y), armMin(z), armMax(x), armMax(y), armMax(z));
            PLATES[side.ordinal()] = Block.createCuboidShape(
                    plateMin(x), plateMin(y), plateMin(z), plateMax(x), plateMax(y), plateMax(z));
        }
    }

    /** Shapes by the twelve bits of arms and plates, made when first needed. */
    private static final Map<Integer, VoxelShape> SHAPES = new ConcurrentHashMap<>();

    public EnergyCableBlock(Settings settings) {
        super(settings);
        BlockState state = this.getStateManager().getDefaultState();
        for (EnumProperty<Side> side : SIDES.values()) {
            state = state.with(side, Side.NONE);
        }
        this.setDefaultState(state.with(FITTING, true).with(POWERED, false));
    }

    @Override
    protected MapCodec<? extends BlockWithEntity> getCodec() {
        return CODEC;
    }

    @Override
    protected void appendProperties(StateManager.Builder<Block, BlockState> builder) {
        for (Direction side : Direction.values()) {
            builder.add(SIDES.get(side));
        }
        builder.add(FITTING, POWERED);
    }

    // ---- joining up ----

    /** What a cable meets on one side: more cable, a machine with a power door there, or nothing. */
    private static Side sideTowards(WorldView world, BlockPos pos, Direction side) {
        BlockState state = world.getBlockState(pos);
        if (state.getBlock() instanceof EnergyCableBlock) {
            return Side.CABLE;
        }
        if (world instanceof World level
                && team.reborn.energy.api.EnergyStorage.SIDED.find(level, pos, side.getOpposite()) != null) {
            return Side.DEVICE;
        }
        return Side.NONE;
    }

    private static BlockState connect(BlockState state, WorldView world, BlockPos pos) {
        BlockState joined = state;
        for (Direction side : Direction.values()) {
            joined = joined.with(SIDES.get(side), sideTowards(world, pos.offset(side), side));
        }
        return withFitting(joined);
    }

    private static BlockState withFitting(BlockState state) {
        int count = 0;
        Direction first = null;
        Direction second = null;
        for (Direction side : Direction.values()) {
            if (state.get(SIDES.get(side)).connected()) {
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
        return withFitting(state.with(SIDES.get(direction), sideTowards(world, neighborPos, direction)));
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
            Side meets = state.get(SIDES.get(side));
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
                    shape = VoxelShapes.union(shape, ARMS[side.ordinal()]);
                }
                if ((bits & (1 << (side.ordinal() + 6))) != 0) {
                    shape = VoxelShapes.union(shape, PLATES[side.ordinal()]);
                }
            }
            return shape;
        });
    }

    @Override
    protected VoxelShape getOutlineShape(BlockState state, BlockView world, BlockPos pos, ShapeContext context) {
        return shapeOf(state);
    }

    @Override
    protected VoxelShape getCollisionShape(BlockState state, BlockView world, BlockPos pos, ShapeContext context) {
        return shapeOf(state);
    }

    // ---- what is in it ----

    @Override
    public BlockEntity createBlockEntity(BlockPos pos, BlockState state) {
        return new EnergyCableBlockEntity(pos, state);
    }

    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(
            World world, BlockState state, BlockEntityType<T> type) {
        if (world.isClient()) {
            return null;
        }
        return validateTicker(type, ModBlockEntities.ENERGY_CABLE, EnergyCableBlockEntity::tick);
    }

    /** Right-clicking a length says how much charge is standing in it. */
    @Override
    protected ActionResult onUse(BlockState state, World world, BlockPos pos,
                                 PlayerEntity player, BlockHitResult hit) {
        if (world.isClient()) {
            return ActionResult.SUCCESS;
        }
        if (world.getBlockEntity(pos) instanceof EnergyCableBlockEntity cable) {
            player.sendMessage(Text.translatable("message.car.cable_level",
                    cable.getEnergy(), EnergyCableBlockEntity.CAPACITY), true);
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
        if (!(world.getBlockEntity(pos) instanceof EnergyCableBlockEntity cable) || cable.getEnergy() <= 0) {
            return 0;
        }
        return MathHelper.clamp(1 + cable.getEnergy() * 14 / EnergyCableBlockEntity.CAPACITY, 1, 15);
    }
}
