package me.trossty.car.block;

import com.mojang.serialization.MapCodec;
import net.minecraft.block.AbstractBlock;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.block.ShapeContext;
import net.minecraft.entity.ai.pathing.NavigationType;
import net.minecraft.item.ItemPlacementContext;
import net.minecraft.state.StateManager;
import net.minecraft.state.property.EnumProperty;
import net.minecraft.state.property.IntProperty;
import net.minecraft.state.property.Properties;
import net.minecraft.util.StringIdentifiable;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.random.Random;
import net.minecraft.util.shape.VoxelShape;
import net.minecraft.world.BlockView;
import net.minecraft.world.WorldView;
import net.minecraft.world.tick.ScheduledTickView;
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

    public static final MapCodec<AsphaltBlock> CODEC = createCodec(AsphaltBlock::new);
    public static final int MAX_LAYERS = 8;
    public static final IntProperty LAYERS = Properties.LAYERS;
    public static final EnumProperty<Marking> MARKING = EnumProperty.of("marking", Marking.class);
    public static final EnumProperty<Direction> FACING = Properties.HORIZONTAL_FACING;
    public static final EnumProperty<Paint> PAINT = EnumProperty.of("paint", Paint.class);

    private static final VoxelShape[] SHAPES = Block.createShapeArray(MAX_LAYERS,
            layers -> Block.createColumnShape(16.0, 0.0, layers * 2));

    /** What can be painted on a road. The texture of each is drawn running north, the way it was painted facing. */
    public enum Marking implements StringIdentifiable {
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
        public String asString() {
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
    public enum Paint implements StringIdentifiable {
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
        public String asString() {
            return this.id;
        }
    }

    public AsphaltBlock(AbstractBlock.Settings settings) {
        super(settings);
        // A whole block by default, so a road set down by code is solid road;
        // laid by hand it goes down one layer at a time (see getPlacementState).
        this.setDefaultState(this.stateManager.getDefaultState()
                .with(LAYERS, MAX_LAYERS)
                .with(MARKING, Marking.NONE)
                .with(FACING, Direction.NORTH)
                .with(PAINT, Paint.WHITE));
    }

    @Override
    protected MapCodec<? extends Block> getCodec() {
        return CODEC;
    }

    @Override
    protected void appendProperties(StateManager.Builder<Block, BlockState> builder) {
        builder.add(LAYERS, MARKING, FACING, PAINT);
    }

    /** Height of the road's top above the bottom of its block, in blocks. */
    public static double height(BlockState state) {
        return state.get(LAYERS) / (double) MAX_LAYERS;
    }

    @Override
    protected VoxelShape getOutlineShape(BlockState state, BlockView world, BlockPos pos, ShapeContext context) {
        return SHAPES[state.get(LAYERS)];
    }

    // Unlike snow, a road is driven on, not waded through: what you see is what you stand on.
    @Override
    protected VoxelShape getCollisionShape(BlockState state, BlockView world, BlockPos pos, ShapeContext context) {
        return SHAPES[state.get(LAYERS)];
    }

    @Override
    protected VoxelShape getSidesShape(BlockState state, BlockView world, BlockPos pos) {
        return SHAPES[state.get(LAYERS)];
    }

    @Override
    protected VoxelShape getCameraCollisionShape(BlockState state, BlockView world, BlockPos pos, ShapeContext context) {
        return SHAPES[state.get(LAYERS)];
    }

    @Override
    protected boolean hasSidedTransparency(BlockState state) {
        return state.get(LAYERS) < MAX_LAYERS;
    }

    @Override
    protected float getAmbientOcclusionLightLevel(BlockState state, BlockView world, BlockPos pos) {
        return state.get(LAYERS) == MAX_LAYERS ? 0.2F : 1.0F;
    }

    @Override
    protected boolean canPathfindThrough(BlockState state, NavigationType type) {
        return false;
    }

    @Override
    protected boolean canPlaceAt(BlockState state, WorldView world, BlockPos pos) {
        BlockState below = world.getBlockState(pos.down());
        return Block.isFaceFullSquare(below.getCollisionShape(world, pos.down()), Direction.UP)
                || below.isOf(this) && below.get(LAYERS) == MAX_LAYERS;
    }

    @Override
    protected BlockState getStateForNeighborUpdate(BlockState state, WorldView world, ScheduledTickView tickView,
                                                   BlockPos pos, Direction direction, BlockPos neighborPos,
                                                   BlockState neighborState, Random random) {
        return !state.canPlaceAt(world, pos)
                ? Blocks.AIR.getDefaultState()
                : super.getStateForNeighborUpdate(state, world, tickView, pos, direction, neighborPos, neighborState, random);
    }

    /** Another layer of asphalt goes on top of a part-laid block, as snow does; nothing else replaces it. */
    @Override
    protected boolean canReplace(BlockState state, ItemPlacementContext context) {
        if (!context.getStack().isOf(this.asItem()) || state.get(LAYERS) >= MAX_LAYERS) {
            return false;
        }
        return !context.canReplaceExisting() || context.getSide() == Direction.UP;
    }

    @Override
    public @Nullable BlockState getPlacementState(ItemPlacementContext ctx) {
        BlockState there = ctx.getWorld().getBlockState(ctx.getBlockPos());
        if (there.isOf(this)) {
            // Any marking rises with the new top rather than being buried.
            return there.with(LAYERS, Math.min(MAX_LAYERS, there.get(LAYERS) + 1));
        }
        return this.getDefaultState().with(LAYERS, 1);
    }
}
