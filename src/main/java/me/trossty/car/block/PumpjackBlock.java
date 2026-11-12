package me.trossty.car.block;

import com.mojang.serialization.MapCodec;
import me.trossty.car.block.entity.PumpjackBlockEntity;
import me.trossty.car.registry.ModBlockEntities;
import net.minecraft.block.BlockState;
import net.minecraft.block.BlockWithEntity;
import net.minecraft.block.ShapeContext;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.block.entity.BlockEntityTicker;
import net.minecraft.block.entity.BlockEntityType;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.state.property.IntProperty;
import net.minecraft.text.Text;
import net.minecraft.util.ActionResult;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.random.Random;
import net.minecraft.util.shape.VoxelShape;
import net.minecraft.util.shape.VoxelShapes;
import net.minecraft.world.BlockView;
import net.minecraft.world.World;
import org.jspecify.annotations.Nullable;

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

    public static final MapCodec<PumpjackBlock> CODEC = createCodec(PumpjackBlock::new);

    public static final int[][] CELLS = {
            {0, 0, 0}, {0, 0, 1}, {0, 0, 2},
            {0, 1, 0}, {0, 1, 1}, {0, 1, 2},
            {0, 2, 1},
    };

    /** The walking beam's model, drawn about the bearing in cell {0, 2, 1}. */
    public static final int BEAM = CELLS.length;
    /** The crank's model, drawn about its shaft in cell {0, 1, 2}. */
    public static final int CRANK = CELLS.length + 1;

    public static final IntProperty PART = IntProperty.of("part", 0, CRANK);

    private static final VoxelShape WELLHEAD = VoxelShapes.union(
            BlockWithEntity.createCuboidShape(3, 0, 3, 13, 10, 13),
            BlockWithEntity.createCuboidShape(0, 0, 0, 16, 2, 16));
    private static final VoxelShape ROD = BlockWithEntity.createCuboidShape(6, 0, 6, 10, 16, 10);
    private static final VoxelShape POST = BlockWithEntity.createCuboidShape(2, 0, 4, 14, 16, 12);

    public PumpjackBlock(Settings settings) {
        super(settings);
        this.setDefaultState(this.getStateManager().getDefaultState()
                .with(LIT, false).with(FACING, Direction.NORTH).with(PART, 0));
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
        return switch (state.get(PART)) {
            case 0 -> WELLHEAD;
            case 3 -> ROD;
            case 4, 6 -> POST;
            default -> super.getOutlineShape(state, world, pos, context);
        };
    }

    @Override
    public @Nullable BlockEntity createBlockEntity(BlockPos pos, BlockState state) {
        return state.get(PART) == 0 ? new PumpjackBlockEntity(pos, state) : null;
    }

    @Override
    public <T extends BlockEntity> @Nullable BlockEntityTicker<T> getTicker(
            World world, BlockState state, BlockEntityType<T> type) {
        if (world.isClient() || state.get(PART) != 0) {
            return null;
        }
        return validateTicker(type, ModBlockEntities.PUMPJACK, PumpjackBlockEntity::tick);
    }

    /** An empty hand reads the gauges, and says what is under it. */
    @Override
    protected ActionResult onUse(BlockState state, World world, BlockPos pos,
                                 PlayerEntity player, BlockHitResult hit) {
        if (world.isClient()) {
            return ActionResult.SUCCESS;
        }
        if (world.getBlockEntity(this.controllerOf(pos, state)) instanceof PumpjackBlockEntity jack) {
            player.openHandledScreen(new net.minecraft.screen.SimpleNamedScreenHandlerFactory(
                    (syncId, inventory, who) -> me.trossty.car.machine.MachineScreenHandler.of(syncId, inventory,
                            jack, null),
                    this.getName()));
        }
        return ActionResult.CONSUME;
    }

    /** A little black smoke off the motor while it pumps. */
    @Override
    public void randomDisplayTick(BlockState state, World world, BlockPos pos, Random random) {
        if (!state.get(LIT) || state.get(PART) != 2) {
            return;
        }
        if (random.nextInt(4) == 0) {
            world.addParticleClient(ParticleTypes.SMOKE,
                    pos.getX() + 0.5, pos.getY() + 0.9, pos.getZ() + 0.5, 0.0, 0.02, 0.0);
        }
        world.playSoundClient(pos.getX() + 0.5, pos.getY() + 1.0, pos.getZ() + 0.5,
                me.trossty.car.sound.ModSounds.PUMPJACK_CREAK, net.minecraft.sound.SoundCategory.BLOCKS,
                0.5F, 0.9F + random.nextFloat() * 0.15F, false);
    }
}
