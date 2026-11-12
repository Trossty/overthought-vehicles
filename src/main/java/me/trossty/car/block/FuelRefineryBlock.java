package me.trossty.car.block;

import com.mojang.serialization.MapCodec;
import me.trossty.car.block.entity.FuelRefineryBlockEntity;
import me.trossty.car.registry.ModBlockEntities;
import net.minecraft.block.BlockState;
import net.minecraft.block.BlockWithEntity;
import net.minecraft.block.ShapeContext;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.block.entity.BlockEntityTicker;
import net.minecraft.block.entity.BlockEntityType;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.state.property.IntProperty;
import net.minecraft.util.ActionResult;
import net.minecraft.util.ItemScatterer;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.random.Random;
import net.minecraft.util.shape.VoxelShape;
import net.minecraft.world.BlockView;
import net.minecraft.world.World;
import org.jspecify.annotations.Nullable;

/**
 * The refinery: a plant, not a block.
 *
 * <p>Four blocks by four on a concrete pad and six high: the boiler house and
 * the control room across the front, the crude tank behind, and at the back
 * the distillation column with its platforms and ladder, the stripper on its
 * pipe rack and the brick stack in the far corner. The stack smokes and the
 * column steams while it works. Any part opens the window; any part broken
 * takes the whole plant down and gives the one item back.
 *
 * <p>The cells and their outlines are read from the model by
 * {@code tools/GenPlantModels.java} into {@link FuelRefineryLayout}.
 */
public class FuelRefineryBlock extends MultiblockBlock {

    public static final MapCodec<FuelRefineryBlock> CODEC = createCodec(FuelRefineryBlock::new);

    public static final int[][] CELLS = FuelRefineryLayout.CELLS;

    public static final int COLUMN_TOP = FuelRefineryLayout.COLUMN_TOP;
    public static final int STACK_TOP = FuelRefineryLayout.STACK_TOP;

    public static final IntProperty PART = IntProperty.of("part", 0, CELLS.length - 1);

    /** Each part's outline, turned for each way the plant can face. */
    private static final VoxelShape[][] SHAPES = new VoxelShape[4][CELLS.length];

    static {
        for (net.minecraft.util.math.Direction facing : net.minecraft.util.math.Direction.Type.HORIZONTAL) {
            for (int part = 0; part < CELLS.length; part++) {
                double[] o = FuelRefineryLayout.OUTLINES[part];
                double x1 = o[0], z1 = o[2], x2 = o[3], z2 = o[5];
                double[] turned = switch (facing) {
                    case SOUTH -> new double[]{16 - x2, 16 - z2, 16 - x1, 16 - z1};
                    case EAST -> new double[]{16 - z2, x1, 16 - z1, x2};
                    case WEST -> new double[]{z1, 16 - x2, z2, 16 - x1};
                    default -> new double[]{x1, z1, x2, z2};
                };
                SHAPES[facing.getHorizontalQuarterTurns()][part] = BlockWithEntity.createCuboidShape(
                        turned[0], o[1], turned[1], turned[2], o[4], turned[3]);
            }
        }
    }

    public FuelRefineryBlock(Settings settings) {
        super(settings);
        this.setDefaultState(this.getStateManager().getDefaultState()
                .with(LIT, false).with(FACING, net.minecraft.util.math.Direction.NORTH).with(PART, 0));
    }

    @Override
    protected MapCodec<? extends BlockWithEntity> getCodec() {
        return CODEC;
    }

    @Override
    public int[][] cells() {
        return CELLS;
    }

    @Override
    public IntProperty partProperty() {
        return PART;
    }

    @Override
    protected VoxelShape getOutlineShape(BlockState state, BlockView world, BlockPos pos, ShapeContext context) {
        return SHAPES[state.get(FACING).getHorizontalQuarterTurns()][state.get(PART)];
    }

    @Override
    public @Nullable BlockEntity createBlockEntity(BlockPos pos, BlockState state) {
        return state.get(PART) == 0 ? new FuelRefineryBlockEntity(pos, state) : null;
    }

    @Override
    public <T extends BlockEntity> @Nullable BlockEntityTicker<T> getTicker(
            World world, BlockState state, BlockEntityType<T> type) {
        if (world.isClient() || state.get(PART) != 0) {
            return null;
        }
        return validateTicker(type, ModBlockEntities.FUEL_REFINERY, FuelRefineryBlockEntity::tick);
    }

    @Override
    protected ActionResult onUse(BlockState state, World world, BlockPos pos,
                                 PlayerEntity player, BlockHitResult hit) {
        if (world.isClient()) {
            return ActionResult.SUCCESS;
        }
        if (world.getBlockEntity(this.controllerOf(pos, state)) instanceof FuelRefineryBlockEntity refinery) {
            player.openHandledScreen(refinery);
        }
        return ActionResult.CONSUME;
    }

    /** Smoke out of the stack and a breath of steam off the column, while it works. */
    @Override
    public void randomDisplayTick(BlockState state, World world, BlockPos pos, Random random) {
        if (!state.get(LIT)) {
            return;
        }
        int part = state.get(PART);
        if (part == STACK_TOP) {
            for (int i = 0; i < 2; i++) {
                world.addImportantParticleClient(ParticleTypes.CAMPFIRE_COSY_SMOKE, true,
                        pos.getX() + 0.5 + random.nextGaussian() * 0.06, pos.getY() + 0.6,
                        pos.getZ() + 0.5 + random.nextGaussian() * 0.06,
                        0.0, 0.05 + random.nextDouble() * 0.03, 0.0);
            }
            if (random.nextInt(3) == 0) {
                world.addParticleClient(ParticleTypes.LARGE_SMOKE,
                        pos.getX() + 0.5, pos.getY() + 0.65, pos.getZ() + 0.5, 0.0, 0.04, 0.0);
            }
        } else if (part == COLUMN_TOP && random.nextInt(4) == 0) {
            world.addParticleClient(ParticleTypes.CLOUD,
                    pos.getX() + 0.5, pos.getY() + 1.05, pos.getZ() + 0.5, 0.0, 0.03, 0.0);
        } else if (part == 0) {
            if (random.nextInt(5) == 0) {
                world.addParticleClient(ParticleTypes.SMOKE,
                        pos.getX() + 0.5, pos.getY() + 0.6, pos.getZ() + 0.5, 0.0, 0.02, 0.0);
            }
            world.playSoundClient(pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5,
                    me.trossty.car.sound.ModSounds.REFINERY_BUBBLE, net.minecraft.sound.SoundCategory.BLOCKS,
                    0.5F, 0.85F + random.nextFloat() * 0.2F, false);
        }
    }

    @Override
    protected void onStateReplaced(BlockState state, ServerWorld world, BlockPos pos, boolean moved) {
        if (state.get(PART) == 0 && world.getBlockEntity(pos) instanceof FuelRefineryBlockEntity refinery) {
            ItemScatterer.spawn(world, pos, refinery);
        }
        super.onStateReplaced(state, world, pos, moved);
    }
}
