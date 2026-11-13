package me.trossty.car.block;

import com.mojang.serialization.MapCodec;
import me.trossty.car.block.entity.ChargingPostBlockEntity;
import me.trossty.car.machine.MachineScreenHandler;
import me.trossty.car.registry.ModBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * A kerbside post with a screen and a plug in its holster. Fed by a cable, it
 * charges the nearest electric vehicle parked beside it; the screen lights
 * while one is drawing.
 */
public class ChargingPostBlock extends ModelEntityBlock {

    public static final MapCodec<ChargingPostBlock> CODEC = simpleCodec(ChargingPostBlock::new);
    public static final EnumProperty<Direction> FACING = BlockStateProperties.HORIZONTAL_FACING;
    public static final BooleanProperty LIT = BlockStateProperties.LIT;

    private static final VoxelShape SHAPE = box(3.5, 0, 3.5, 12.5, 23.8, 12.5);

    public ChargingPostBlock(Properties settings) {
        super(settings);
        this.registerDefaultState(this.getStateDefinition().any().setValue(FACING, Direction.NORTH).setValue(LIT, false));
    }

    @Override
    protected MapCodec<? extends BaseEntityBlock> codec() {
        return CODEC;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING, LIT);
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        return this.defaultBlockState().setValue(FACING, context.getHorizontalDirection().getOpposite());
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter world, BlockPos pos, CollisionContext context) {
        return SHAPE;
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new ChargingPostBlockEntity(pos, state);
    }

    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level world, BlockState state,
                                                                  BlockEntityType<T> type) {
        return world.isClientSide() ? null : createTickerHelper(type, ModBlockEntities.CHARGING_POST, ChargingPostBlockEntity::tick);
    }

    /** A spark at the plug now and then while a car is drawing. */
    @Override
    public void animateTick(BlockState state, Level world, BlockPos pos, RandomSource random) {
        if (!state.getValue(LIT)) {
            return;
        }
        world.playLocalSound(pos.getX() + 0.5, pos.getY() + 0.7, pos.getZ() + 0.5,
                me.trossty.car.sound.ModSounds.CHARGER_HUM, net.minecraft.sounds.SoundSource.BLOCKS, 0.3F, 1.0F, false);
        if (random.nextInt(3) != 0) {
            return;
        }
        Direction front = state.getValue(FACING);
        world.addParticle(ParticleTypes.ELECTRIC_SPARK,
                pos.getX() + 0.5 + front.getStepX() * 0.3, pos.getY() + 0.55,
                pos.getZ() + 0.5 + front.getStepZ() * 0.3, 0.0, 0.02, 0.0);
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level world, BlockPos pos, Player player, BlockHitResult hit) {
        if (world.isClientSide()) {
            return InteractionResult.SUCCESS;
        }
        if (world.getBlockEntity(pos) instanceof ChargingPostBlockEntity post) {
            player.openMenu(new SimpleMenuProvider(
                    (syncId, inventory, who) -> MachineScreenHandler.of(syncId, inventory, post, null),
                    this.getName()));
        }
        return InteractionResult.CONSUME;
    }
}
