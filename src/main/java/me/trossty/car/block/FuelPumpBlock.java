package me.trossty.car.block;

import com.mojang.serialization.MapCodec;
import me.trossty.car.block.entity.FuelPumpBlockEntity;
import me.trossty.car.entity.CarEntity;
import me.trossty.car.registry.ModBlockEntities;
import me.trossty.car.registry.ModItems;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.phys.BlockHitResult;


/**
 * The pump on the forecourt. Two blocks tall, because one is a vending machine.
 *
 * <p>It has no window. Everything it does is a right click, and which one
 * depends on what is in your hand:
 *
 * <ul>
 *   <li>a <b>full jerry can</b> — empties into the pump, for stocking it by hand
 *   <li>an <b>empty jerry can</b> — fills from the pump, for carrying fuel away
 *   <li>a <b>bare hand</b> — takes the nozzle off its hook. The hose follows you
 *   to the car, and holding the nozzle to the filler cap with the button down
 *   fills it, the way a real one is filled
 *   <li><b>the nozzle</b> — hangs it back up
 * </ul>
 *
 * <p>Every one of those also says what is left in the tank on the action bar,
 * which is the whole user interface and all it needs to be.
 */
public class FuelPumpBlock extends ModelEntityBlock {

    public static final MapCodec<FuelPumpBlock> CODEC = simpleCodec(FuelPumpBlock::new);

    public static final EnumProperty<DoubleBlockHalf> HALF = BlockStateProperties.DOUBLE_BLOCK_HALF;
    public static final EnumProperty<Direction> FACING = BlockStateProperties.HORIZONTAL_FACING;

    /** Whether the nozzle is on its hook. Off it, the lower half is drawn without one. */
    public static final net.minecraft.world.level.block.state.properties.BooleanProperty NOZZLE =
            net.minecraft.world.level.block.state.properties.BooleanProperty.create("nozzle");

    public FuelPumpBlock(Properties settings) {
        super(settings);
        this.registerDefaultState(this.getStateDefinition().any()
                .setValue(HALF, DoubleBlockHalf.LOWER)
                .setValue(FACING, Direction.NORTH)
                .setValue(NOZZLE, true));
    }

    @Override
    protected MapCodec<? extends BaseEntityBlock> codec() {
        return CODEC;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(HALF, FACING, NOZZLE);
    }

    // ---- the two halves ----

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        BlockPos pos = context.getClickedPos();
        if (!context.getLevel().getBlockState(pos.above()).canBeReplaced()) {
            return null;
        }
        return this.defaultBlockState()
                .setValue(FACING, context.getHorizontalDirection().getOpposite());
    }

    @Override
    public void setPlacedBy(Level world, BlockPos pos, BlockState state, LivingEntity placer, ItemStack stack) {
        world.setBlock(pos.above(), state.setValue(HALF, DoubleBlockHalf.UPPER), Block.UPDATE_ALL);
    }

    /**
     * Breaking the top half in creative would leave the bottom to follow on its
     * own, and the bottom carries the loot: it is taken away here first, with
     * no drops, the way a door's is.
     */
    @Override
    public BlockState playerWillDestroy(Level world, BlockPos pos, BlockState state, Player player) {
        if (!world.isClientSide() && player.isCreative() && state.getValue(HALF) == DoubleBlockHalf.UPPER) {
            BlockPos below = pos.below();
            BlockState base = world.getBlockState(below);
            if (base.is(this) && base.getValue(HALF) == DoubleBlockHalf.LOWER) {
                world.setBlock(below, Blocks.AIR.defaultBlockState(), Block.UPDATE_ALL | Block.UPDATE_SUPPRESS_DROPS);
                world.levelEvent(player, net.minecraft.world.level.block.LevelEvent.PARTICLES_DESTROY_BLOCK, below,
                        Block.getId(base));
            }
        }
        return super.playerWillDestroy(world, pos, state, player);
    }

    @Override
    protected boolean canSurvive(BlockState state, LevelReader world, BlockPos pos) {
        if (state.getValue(HALF) == DoubleBlockHalf.UPPER) {
            BlockState below = world.getBlockState(pos.below());
            return below.is(this) && below.getValue(HALF) == DoubleBlockHalf.LOWER;
        }
        return super.canSurvive(state, world, pos);
    }

    /**
     * Half a pump cannot stand. Whichever end goes, the other sees the gap on the
     * next neighbour update and follows — the same way a door does, and for the
     * same reason: it catches every way a block can leave, not just mining.
     *
     * <p>Only the lower half carries loot, so exactly one pump drops whichever
     * end was hit: the mined half drops it, or the half that vanishes does.
     */
    @Override
    protected BlockState updateShape(BlockState state, Direction direction, BlockState neighborState,
                                     net.minecraft.world.level.LevelAccessor world, BlockPos pos, BlockPos neighborPos) {
        DoubleBlockHalf half = state.getValue(HALF);
        boolean towardsOtherHalf = direction.getAxis() == Direction.Axis.Y
                && (half == DoubleBlockHalf.LOWER) == (direction == Direction.UP);
        if (towardsOtherHalf && (!neighborState.is(this) || neighborState.getValue(HALF) == half)) {
            return Blocks.AIR.defaultBlockState();
        }
        return super.updateShape(state, direction, neighborState, world, pos, neighborPos);
    }

    // ---- the block entity lives downstairs ----

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return state.getValue(HALF) == DoubleBlockHalf.LOWER ? new FuelPumpBlockEntity(pos, state) : null;
    }

    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(
            Level world, BlockState state, BlockEntityType<T> type) {
        if (world.isClientSide() || state.getValue(HALF) != DoubleBlockHalf.LOWER) {
            return null;
        }
        return createTickerHelper(type, ModBlockEntities.FUEL_PUMP, FuelPumpBlockEntity::tick);
    }

    /** The tank, wherever you knocked on the pump. */
    private static FuelPumpBlockEntity tankOf(Level world, BlockPos pos, BlockState state) {
        BlockPos base = state.getValue(HALF) == DoubleBlockHalf.UPPER ? pos.below() : pos;
        return world.getBlockEntity(base) instanceof FuelPumpBlockEntity pump ? pump : null;
    }

    /**
     * The tank of the pump standing at {@code pos}, whichever half is there, or
     * null if that is not a pump at all. This is what a fuel pipe asks when it
     * wants to know whether the block it is pointing at can be filled.
     */
    public static FuelPumpBlockEntity tankAt(Level world, BlockPos pos) {
        BlockState state = world.getBlockState(pos);
        return state.getBlock() instanceof FuelPumpBlock ? tankOf(world, pos, state) : null;
    }

    // ---- using it ----

    /** A bare hand takes the nozzle off its hook. */
    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level world, BlockPos pos,
                                 Player player, BlockHitResult hit) {
        if (!player.getMainHandItem().isEmpty()) {
            return InteractionResult.PASS;
        }
        if (world.isClientSide()) {
            return InteractionResult.SUCCESS;
        }
        FuelPumpBlockEntity pump = tankOf(world, pos, state);
        if (pump == null) {
            return InteractionResult.CONSUME;
        }
        if (pump.takeNozzle(player)) {
            report(player, pump);
        }
        return InteractionResult.CONSUME;
    }

    @Override
    protected net.minecraft.world.ItemInteractionResult useItemOn(ItemStack held, BlockState state, Level world, BlockPos pos,
                                         Player player, InteractionHand hand, BlockHitResult hit) {
        if (held.is(ModItems.FUEL_NOZZLE)) {
            BlockPos base = state.getValue(HALF) == DoubleBlockHalf.UPPER ? pos.below() : pos;
            if (!me.trossty.car.item.FuelNozzleItem.isFrom(held, base)) {
                return net.minecraft.world.ItemInteractionResult.SKIP_DEFAULT_BLOCK_INTERACTION;
            }
            if (!world.isClientSide()) {
                FuelPumpBlockEntity pump = tankOf(world, pos, state);
                if (pump != null) {
                    pump.hangUpNozzle(false);
                    report(player, pump);
                }
            }
            return net.minecraft.world.ItemInteractionResult.SUCCESS;
        }
        if (!held.is(ModItems.JERRY_CAN)) {
            return net.minecraft.world.ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;   // a bare hand reaches onUse
        }
        if (world.isClientSide()) {
            return net.minecraft.world.ItemInteractionResult.SUCCESS;
        }
        FuelPumpBlockEntity pump = tankOf(world, pos, state);
        if (pump == null) {
            return net.minecraft.world.ItemInteractionResult.CONSUME;
        }

        if (me.trossty.car.entity.CarEntity.holdsDiesel(held)) {
            player.displayClientMessage(Component.translatable("message.car.petrol_only"), true);
            return net.minecraft.world.ItemInteractionResult.CONSUME;
        }
        if (!me.trossty.car.item.JerryCans.isEmpty(held) && me.trossty.car.entity.CarEntity.isRaw(held)) {
            // Raw petrol goes through the hydrotreater, not into a station's tank.
            player.displayClientMessage(Component.translatable("message.car.pump_raw"), true);
            return net.minecraft.world.ItemInteractionResult.CONSUME;
        }

        if (!me.trossty.car.item.JerryCans.isFull(held)) {
            // A can with room in it: top it up out of the pump.
            if (me.trossty.car.item.JerryCans.topUp(held, pump.getPetrol(), pump::drain) > 0) {
                me.trossty.car.entity.CarEntity.markFuel(held, me.trossty.car.entity.CarSetup.Fuel.PETROL);
                playPumpSound(world, pos, 1.1F);
            }
        } else if (me.trossty.car.item.JerryCans.emptyInto(
                held, pump.getCapacity() - pump.getPetrol(), pump::fill) > 0) {
            // A full can: tip it in, as far as the pump has room.
            playPumpSound(world, pos, 0.8F);
        }
        report(player, pump);
        return net.minecraft.world.ItemInteractionResult.CONSUME;
    }

    /**
     * Where the hose leaves the pump, in the world: low on its front, where
     * the coil hangs in the model. The renderer draws the hose from here and
     * the pump measures its reach from here, so the two always agree.
     */
    public static net.minecraft.world.phys.Vec3 hoseOutlet(BlockPos base, Direction facing) {
        // In the model, which faces north: the hose at the holster on the right-hand side (west), 2 px across, 20 up, 9.5 in.
        double x = 2.1 / 16.0;
        double z = 9.5 / 16.0;
        double rx;
        double rz;
        switch (facing) {
            case EAST -> { rx = 1.0 - z; rz = x; }
            case SOUTH -> { rx = 1.0 - x; rz = 1.0 - z; }
            case WEST -> { rx = z; rz = 1.0 - x; }
            default -> { rx = x; rz = z; }
        }
        return new net.minecraft.world.phys.Vec3(base.getX() + rx, base.getY() + 20.5 / 16.0, base.getZ() + rz);
    }

    /** The hose outlet of the pump whose lower half is at {@code base}, or null if it is gone. */
    public static net.minecraft.world.phys.@org.jetbrains.annotations.Nullable Vec3 hoseOutlet(
            net.minecraft.world.level.BlockGetter world, BlockPos base) {
        BlockState state = world.getBlockState(base);
        if (!(state.getBlock() instanceof FuelPumpBlock)) {
            return null;
        }
        return hoseOutlet(base, state.getValue(FACING));
    }

    /** Hangs the nozzle back on, or takes it off: both halves, so neither looks wrong. */
    public static void setNozzleHung(Level world, BlockPos base, boolean hung) {
        for (BlockPos at : new BlockPos[] {base, base.above()}) {
            BlockState state = world.getBlockState(at);
            if (state.getBlock() instanceof FuelPumpBlock && state.getValue(NOZZLE) != hung) {
                world.setBlock(at, state.setValue(NOZZLE, hung), Block.UPDATE_CLIENTS);
            }
        }
    }

    /** The action bar is the pump's only display, so every click updates it. */
    private static void report(Player player, FuelPumpBlockEntity pump) {
        player.displayClientMessage(Component.translatable("message.car.pump_level",
                pump.getPetrol(), pump.getCapacity()), true);
    }

    private static void playPumpSound(Level world, BlockPos pos, float pitch) {
        world.playSound(null, pos, SoundEvents.BUCKET_EMPTY, SoundSource.BLOCKS, 0.7F, pitch);
    }

    // ---- redstone ----

    @Override
    protected boolean hasAnalogOutputSignal(BlockState state) {
        return true;
    }

    @Override
    protected int getAnalogOutputSignal(BlockState state, Level world, BlockPos pos) {
        FuelPumpBlockEntity pump = tankOf(world, pos, state);
        if (pump == null || pump.getPetrol() <= 0) {
            return 0;
        }
        return Mth.clamp(1 + pump.getPetrol() * 14 / pump.getCapacity(), 1, 15);
    }
}
