package me.trossty.car.block;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.RandomSource;
import net.minecraft.util.StringRepresentable;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.ScheduledTickAccess;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.level.pathfinder.PathComputationType;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jspecify.annotations.Nullable;

/**
 * Laid road. It goes down a layer at a time the way snow does, an eighth of a
 * block each, so a road can climb a hill in gentle steps a car rolls up rather
 * than a block it has to jump; eight layers make a whole block.
 *
 * <p>The top can carry one painted marking, put there with a can of road
 * paint: which marking, which way it runs and in what paint are all part of
 * the block state, so a painted road is no more costly than a bare one.
 */
public class AsphaltBlock extends Block {

    public static final MapCodec<AsphaltBlock> CODEC = simpleCodec(AsphaltBlock::new);
    public static final int MAX_LAYERS = 8;
    public static final IntegerProperty LAYERS = BlockStateProperties.LAYERS;
    public static final EnumProperty<Marking> MARKING = EnumProperty.create("marking", Marking.class);
    public static final EnumProperty<Direction> FACING = BlockStateProperties.HORIZONTAL_FACING;
    public static final EnumProperty<Paint> PAINT = EnumProperty.create("paint", Paint.class);

    private static final VoxelShape[] SHAPES = Block.boxes(MAX_LAYERS,
            layers -> Block.column(16.0, 0.0, layers * 2));

    /** What can be painted on a road. The texture of each is drawn running north, the way it was painted facing. */
    public enum Marking implements StringRepresentable {
        NONE,
        DASHED,
        SOLID,
        DOUBLE,
        EDGE,
        CORNER,
        STOP,
        CROSSWALK,
        ARROW,
        ARROW_LEFT,
        ARROW_RIGHT,
        PARKING,
        HATCH;

        private final String id = this.name().toLowerCase(java.util.Locale.ROOT);

        @Override
        public String getSerializedName() {
            return this.id;
        }

        public String translationKey() {
            return "marking.car." + this.id;
        }

        public static Marking byIndex(int index) {
            Marking[] all = values();
            return all[Math.floorMod(index, all.length)];
        }
    }

    /** The road paints there are, as on real roads: white, yellow, and red and blue for the odd bay. */
    public enum Paint implements StringRepresentable {
        WHITE(0xF0F0EA),
        YELLOW(0xF2C12E),
        RED(0xC93A2F),
        BLUE(0x2F66C9);

        private final String id = this.name().toLowerCase(java.util.Locale.ROOT);
        public final int rgb;

        Paint(int rgb) {
            this.rgb = rgb;
        }

        @Override
        public String getSerializedName() {
            return this.id;
        }
    }

    public AsphaltBlock(BlockBehaviour.Properties settings) {
        super(settings);
        // A whole block by default, so a road set down by code is solid road;
        // laid by hand it goes down one layer at a time (see getPlacementState).
        this.registerDefaultState(this.stateDefinition.any()
                .setValue(LAYERS, MAX_LAYERS)
                .setValue(MARKING, Marking.NONE)
                .setValue(FACING, Direction.NORTH)
                .setValue(PAINT, Paint.WHITE));
    }

    @Override
    protected MapCodec<? extends Block> codec() {
        return CODEC;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(LAYERS, MARKING, FACING, PAINT);
    }

    /** Height of the road's top above the bottom of its block, in blocks. */
    public static double height(BlockState state) {
        return state.getValue(LAYERS) / (double) MAX_LAYERS;
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter world, BlockPos pos, CollisionContext context) {
        return SHAPES[state.getValue(LAYERS)];
    }

    // Unlike snow, a road is driven on, not waded through: what you see is what you stand on.
    @Override
    protected VoxelShape getCollisionShape(BlockState state, BlockGetter world, BlockPos pos, CollisionContext context) {
        return SHAPES[state.getValue(LAYERS)];
    }

    @Override
    protected VoxelShape getBlockSupportShape(BlockState state, BlockGetter world, BlockPos pos) {
        return SHAPES[state.getValue(LAYERS)];
    }

    @Override
    protected VoxelShape getVisualShape(BlockState state, BlockGetter world, BlockPos pos, CollisionContext context) {
        return SHAPES[state.getValue(LAYERS)];
    }

    @Override
    protected boolean useShapeForLightOcclusion(BlockState state) {
        return state.getValue(LAYERS) < MAX_LAYERS;
    }

    @Override
    protected float getShadeBrightness(BlockState state, BlockGetter world, BlockPos pos) {
        return state.getValue(LAYERS) == MAX_LAYERS ? 0.2F : 1.0F;
    }

    @Override
    protected boolean isPathfindable(BlockState state, PathComputationType type) {
        return false;
    }

    @Override
    protected boolean canSurvive(BlockState state, LevelReader world, BlockPos pos) {
        BlockState below = world.getBlockState(pos.below());
        return Block.isFaceFull(below.getCollisionShape(world, pos.below()), Direction.UP)
                || below.is(this) && below.getValue(LAYERS) == MAX_LAYERS;
    }

    @Override
    protected BlockState updateShape(BlockState state, LevelReader world, ScheduledTickAccess tickView,
                                                   BlockPos pos, Direction direction, BlockPos neighborPos,
                                                   BlockState neighborState, RandomSource random) {
        return !state.canSurvive(world, pos)
                ? Blocks.AIR.defaultBlockState()
                : super.updateShape(state, world, tickView, pos, direction, neighborPos, neighborState, random);
    }

    /** Another layer of asphalt goes on top of a part-laid block, as snow does; nothing else replaces it. */
    @Override
    protected boolean canBeReplaced(BlockState state, BlockPlaceContext context) {
        if (!context.getItemInHand().is(this.asItem()) || state.getValue(LAYERS) >= MAX_LAYERS) {
            return false;
        }
        return !context.replacingClickedOnBlock() || context.getClickedFace() == Direction.UP;
    }

    @Override
    public @Nullable BlockState getStateForPlacement(BlockPlaceContext ctx) {
        BlockState there = ctx.getLevel().getBlockState(ctx.getClickedPos());
        if (there.is(this)) {
            // Any marking rises with the new top rather than being buried.
            return there.setValue(LAYERS, Math.min(MAX_LAYERS, there.getValue(LAYERS) + 1));
        }
        return this.defaultBlockState().setValue(LAYERS, 1);
    }
}
