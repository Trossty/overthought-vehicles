package me.trossty.car.block;

import com.mojang.serialization.MapCodec;
import me.trossty.car.block.entity.FuelRefineryBlockEntity;
import me.trossty.car.registry.ModBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.Containers;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
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

    public static final MapCodec<FuelRefineryBlock> CODEC = simpleCodec(FuelRefineryBlock::new);

    public static final int[][] CELLS = FuelRefineryLayout.CELLS;

    public static final int COLUMN_TOP = FuelRefineryLayout.COLUMN_TOP;
    public static final int STACK_TOP = FuelRefineryLayout.STACK_TOP;

    public static final IntegerProperty PART = IntegerProperty.create("part", 0, CELLS.length - 1);

    /** Each part's outline, turned for each way the plant can face. */
    private static final VoxelShape[][] SHAPES = new VoxelShape[4][CELLS.length];

    static {
        for (net.minecraft.core.Direction facing : net.minecraft.core.Direction.Plane.HORIZONTAL) {
            for (int part = 0; part < CELLS.length; part++) {
                double[] o = FuelRefineryLayout.OUTLINES[part];
                double x1 = o[0], z1 = o[2], x2 = o[3], z2 = o[5];
                double[] turned = switch (facing) {
                    case SOUTH -> new double[]{16 - x2, 16 - z2, 16 - x1, 16 - z1};
                    case EAST -> new double[]{16 - z2, x1, 16 - z1, x2};
                    case WEST -> new double[]{z1, 16 - x2, z2, 16 - x1};
                    default -> new double[]{x1, z1, x2, z2};
                };
                SHAPES[facing.get2DDataValue()][part] = BaseEntityBlock.box(
                        turned[0], o[1], turned[1], turned[2], o[4], turned[3]);
            }
        }
    }

    public FuelRefineryBlock(Properties settings) {
        super(settings);
        this.registerDefaultState(this.getStateDefinition().any()
                .setValue(LIT, false).setValue(FACING, net.minecraft.core.Direction.NORTH).setValue(PART, 0));
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

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter world, BlockPos pos, CollisionContext context) {
        return SHAPES[state.getValue(FACING).get2DDataValue()][state.getValue(PART)];
    }

    @Override
    public @Nullable BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return state.getValue(PART) == 0 ? new FuelRefineryBlockEntity(pos, state) : null;
    }

    @Override
    public <T extends BlockEntity> @Nullable BlockEntityTicker<T> getTicker(
            Level world, BlockState state, BlockEntityType<T> type) {
        if (world.isClientSide() || state.getValue(PART) != 0) {
            return null;
        }
        return createTickerHelper(type, ModBlockEntities.FUEL_REFINERY, FuelRefineryBlockEntity::tick);
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level world, BlockPos pos,
                                 Player player, BlockHitResult hit) {
        if (world.isClientSide()) {
            return InteractionResult.SUCCESS;
        }
        if (world.getBlockEntity(this.controllerOf(pos, state)) instanceof FuelRefineryBlockEntity refinery) {
            player.openMenu(refinery);
        }
        return InteractionResult.CONSUME;
    }

    /** Smoke out of the stack and a breath of steam off the column, while it works. */
    @Override
    public void animateTick(BlockState state, Level world, BlockPos pos, RandomSource random) {
        if (!state.getValue(LIT)) {
            return;
        }
        int part = state.getValue(PART);
        if (part == STACK_TOP) {
            for (int i = 0; i < 2; i++) {
                world.addAlwaysVisibleParticle(ParticleTypes.CAMPFIRE_COSY_SMOKE, true,
                        pos.getX() + 0.5 + random.nextGaussian() * 0.06, pos.getY() + 0.6,
                        pos.getZ() + 0.5 + random.nextGaussian() * 0.06,
                        0.0, 0.05 + random.nextDouble() * 0.03, 0.0);
            }
            if (random.nextInt(3) == 0) {
                world.addParticle(ParticleTypes.LARGE_SMOKE,
                        pos.getX() + 0.5, pos.getY() + 0.65, pos.getZ() + 0.5, 0.0, 0.04, 0.0);
            }
        } else if (part == COLUMN_TOP && random.nextInt(4) == 0) {
            world.addParticle(ParticleTypes.CLOUD,
                    pos.getX() + 0.5, pos.getY() + 1.05, pos.getZ() + 0.5, 0.0, 0.03, 0.0);
        } else if (part == 0) {
            if (random.nextInt(5) == 0) {
                world.addParticle(ParticleTypes.SMOKE,
                        pos.getX() + 0.5, pos.getY() + 0.6, pos.getZ() + 0.5, 0.0, 0.02, 0.0);
            }
            world.playLocalSound(pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5,
                    me.trossty.car.sound.ModSounds.REFINERY_BUBBLE, net.minecraft.sounds.SoundSource.BLOCKS,
                    0.5F, 0.85F + random.nextFloat() * 0.2F, false);
        }
    }

    @Override
    protected void affectNeighborsAfterRemoval(BlockState state, ServerLevel world, BlockPos pos, boolean moved) {
        if (state.getValue(PART) == 0 && world.getBlockEntity(pos) instanceof FuelRefineryBlockEntity refinery) {
            Containers.dropContents(world, pos, refinery);
        }
        super.affectNeighborsAfterRemoval(state, world, pos, moved);
    }
}
