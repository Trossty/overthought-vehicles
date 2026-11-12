package me.trossty.car.block;

import com.mojang.serialization.MapCodec;
import me.trossty.car.block.entity.ChargingPostBlockEntity;
import me.trossty.car.machine.MachineScreenHandler;
import me.trossty.car.registry.ModBlockEntities;
import net.minecraft.block.Block;
import net.minecraft.block.BlockRenderType;
import net.minecraft.block.BlockState;
import net.minecraft.block.BlockWithEntity;
import net.minecraft.block.ShapeContext;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.block.entity.BlockEntityTicker;
import net.minecraft.block.entity.BlockEntityType;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemPlacementContext;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.screen.SimpleNamedScreenHandlerFactory;
import net.minecraft.state.StateManager;
import net.minecraft.state.property.BooleanProperty;
import net.minecraft.state.property.EnumProperty;
import net.minecraft.state.property.Properties;
import net.minecraft.util.ActionResult;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.random.Random;
import net.minecraft.util.shape.VoxelShape;
import net.minecraft.world.BlockView;
import net.minecraft.world.World;

/**
 * A kerbside post with a screen and a plug in its holster. Fed by a cable, it
 * charges the nearest electric vehicle parked beside it; the screen lights
 * while one is drawing.
 */
public class ChargingPostBlock extends BlockWithEntity {

    public static final MapCodec<ChargingPostBlock> CODEC = createCodec(ChargingPostBlock::new);
    public static final EnumProperty<Direction> FACING = Properties.HORIZONTAL_FACING;
    public static final BooleanProperty LIT = Properties.LIT;

    private static final VoxelShape SHAPE = createCuboidShape(3.5, 0, 3.5, 12.5, 23.8, 12.5);

    public ChargingPostBlock(Settings settings) {
        super(settings);
        this.setDefaultState(this.getStateManager().getDefaultState().with(FACING, Direction.NORTH).with(LIT, false));
    }

    @Override
    protected MapCodec<? extends BlockWithEntity> getCodec() {
        return CODEC;
    }

    @Override
    protected void appendProperties(StateManager.Builder<Block, BlockState> builder) {
        builder.add(FACING, LIT);
    }

    @Override
    public BlockState getPlacementState(ItemPlacementContext context) {
        return this.getDefaultState().with(FACING, context.getHorizontalPlayerFacing().getOpposite());
    }

    @Override
    protected VoxelShape getOutlineShape(BlockState state, BlockView world, BlockPos pos, ShapeContext context) {
        return SHAPE;
    }

    @Override
    protected BlockRenderType getRenderType(BlockState state) {
        return BlockRenderType.MODEL;
    }

    @Override
    public BlockEntity createBlockEntity(BlockPos pos, BlockState state) {
        return new ChargingPostBlockEntity(pos, state);
    }

    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(World world, BlockState state,
                                                                  BlockEntityType<T> type) {
        return world.isClient() ? null : validateTicker(type, ModBlockEntities.CHARGING_POST, ChargingPostBlockEntity::tick);
    }

    /** A spark at the plug now and then while a car is drawing. */
    @Override
    public void randomDisplayTick(BlockState state, World world, BlockPos pos, Random random) {
        if (!state.get(LIT)) {
            return;
        }
        world.playSoundClient(pos.getX() + 0.5, pos.getY() + 0.7, pos.getZ() + 0.5,
                me.trossty.car.sound.ModSounds.CHARGER_HUM, net.minecraft.sound.SoundCategory.BLOCKS, 0.3F, 1.0F, false);
        if (random.nextInt(3) != 0) {
            return;
        }
        Direction front = state.get(FACING);
        world.addParticleClient(ParticleTypes.ELECTRIC_SPARK,
                pos.getX() + 0.5 + front.getOffsetX() * 0.3, pos.getY() + 0.55,
                pos.getZ() + 0.5 + front.getOffsetZ() * 0.3, 0.0, 0.02, 0.0);
    }

    @Override
    protected ActionResult onUse(BlockState state, World world, BlockPos pos, PlayerEntity player, BlockHitResult hit) {
        if (world.isClient()) {
            return ActionResult.SUCCESS;
        }
        if (world.getBlockEntity(pos) instanceof ChargingPostBlockEntity post) {
            player.openHandledScreen(new SimpleNamedScreenHandlerFactory(
                    (syncId, inventory, who) -> MachineScreenHandler.of(syncId, inventory, post, null),
                    this.getName()));
        }
        return ActionResult.CONSUME;
    }
}
