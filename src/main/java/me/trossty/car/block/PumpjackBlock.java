package me.trossty.car.block;

import com.mojang.serialization.MapCodec;
import me.trossty.car.block.entity.PumpjackBlockEntity;
import me.trossty.car.registry.ModBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.util.RandomSource;
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
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jetbrains.annotations.Nullable;

/**
 * The pumpjack: the nodding donkey that brings crude up out of an oil deposit.
 *
 * <p>Three blocks long and three high. The wellhead is at the front, and is
 * the block that goes down over the deposit; the Samson post stands in the
 * middle and carries the walking beam; the motor and the crank with its
 * counterweights are at the back. The beam and the crank are drawn turning by
 * a renderer while it pumps; two part numbers past the real cells are those
 * two moving pieces' models, never placed in the world.
 */
public class PumpjackBlock extends MultiblockBlock {

    public static final MapCodec<PumpjackBlock> CODEC = simpleCodec(PumpjackBlock::new);

    public static final int[][] CELLS = {
            {0, 0, 0}, {0, 0, 1}, {0, 0, 2},
            {0, 1, 0}, {0, 1, 1}, {0, 1, 2},
            {0, 2, 1},
    };

    /** The walking beam's model, drawn about the bearing in cell {0, 2, 1}. */
    public static final int BEAM = CELLS.length;
    /** The crank's model, drawn about its shaft in cell {0, 1, 2}. */
    public static final int CRANK = CELLS.length + 1;

    public static final IntegerProperty PART = IntegerProperty.create("part", 0, CRANK);

    private static final VoxelShape WELLHEAD = Shapes.or(
            BaseEntityBlock.box(3, 0, 3, 13, 10, 13),
            BaseEntityBlock.box(0, 0, 0, 16, 2, 16));
    private static final VoxelShape ROD = BaseEntityBlock.box(6, 0, 6, 10, 16, 10);
    private static final VoxelShape POST = BaseEntityBlock.box(2, 0, 4, 14, 16, 12);

    public PumpjackBlock(Properties settings) {
        super(settings);
        this.registerDefaultState(this.getStateDefinition().any()
                .setValue(LIT, false).setValue(FACING, Direction.NORTH).setValue(PART, 0));
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
        return switch (state.getValue(PART)) {
            case 0 -> WELLHEAD;
            case 3 -> ROD;
            case 4, 6 -> POST;
            default -> super.getShape(state, world, pos, context);
        };
    }

    @Override
    public @Nullable BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return state.getValue(PART) == 0 ? new PumpjackBlockEntity(pos, state) : null;
    }

    @Override
    public <T extends BlockEntity> @Nullable BlockEntityTicker<T> getTicker(
            Level world, BlockState state, BlockEntityType<T> type) {
        if (world.isClientSide() || state.getValue(PART) != 0) {
            return null;
        }
        return createTickerHelper(type, ModBlockEntities.PUMPJACK, PumpjackBlockEntity::tick);
    }

    /** An empty hand reads the gauges, and says what is under it. */
    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level world, BlockPos pos,
                                 Player player, BlockHitResult hit) {
        if (world.isClientSide()) {
            return InteractionResult.SUCCESS;
        }
        if (world.getBlockEntity(this.controllerOf(pos, state)) instanceof PumpjackBlockEntity jack) {
            player.openMenu(new net.minecraft.world.SimpleMenuProvider(
                    (syncId, inventory, who) -> me.trossty.car.machine.MachineScreenHandler.of(syncId, inventory,
                            jack, null),
                    this.getName()));
        }
        return InteractionResult.CONSUME;
    }

    /** A little black smoke off the motor while it pumps. */
    @Override
    public void animateTick(BlockState state, Level world, BlockPos pos, RandomSource random) {
        if (!state.getValue(LIT) || state.getValue(PART) != 2) {
            return;
        }
        if (random.nextInt(4) == 0) {
            world.addParticle(ParticleTypes.SMOKE,
                    pos.getX() + 0.5, pos.getY() + 0.9, pos.getZ() + 0.5, 0.0, 0.02, 0.0);
        }
        world.playLocalSound(pos.getX() + 0.5, pos.getY() + 1.0, pos.getZ() + 0.5,
                me.trossty.car.sound.ModSounds.PUMPJACK_CREAK, net.minecraft.sounds.SoundSource.BLOCKS,
                0.5F, 0.9F + random.nextFloat() * 0.15F, false);
    }
}
