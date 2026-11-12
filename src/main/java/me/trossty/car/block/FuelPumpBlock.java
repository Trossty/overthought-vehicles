package me.trossty.car.block;

import com.mojang.serialization.MapCodec;
import me.trossty.car.block.entity.FuelPumpBlockEntity;
import me.trossty.car.entity.CarEntity;
import me.trossty.car.registry.ModBlockEntities;
import me.trossty.car.registry.ModItems;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.BlockWithEntity;
import net.minecraft.block.Blocks;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.block.entity.BlockEntityTicker;
import net.minecraft.block.entity.BlockEntityType;
import net.minecraft.block.enums.DoubleBlockHalf;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemPlacementContext;
import net.minecraft.item.ItemStack;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.state.StateManager;
import net.minecraft.state.property.EnumProperty;
import net.minecraft.state.property.Properties;
import net.minecraft.text.Text;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.random.Random;
import net.minecraft.world.tick.ScheduledTickView;
import net.minecraft.world.World;
import net.minecraft.world.WorldView;


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
public class FuelPumpBlock extends BlockWithEntity {

    public static final MapCodec<FuelPumpBlock> CODEC = createCodec(FuelPumpBlock::new);

    public static final EnumProperty<DoubleBlockHalf> HALF = Properties.DOUBLE_BLOCK_HALF;
    public static final EnumProperty<Direction> FACING = Properties.HORIZONTAL_FACING;

    /** Whether the nozzle is on its hook. Off it, the lower half is drawn without one. */
    public static final net.minecraft.state.property.BooleanProperty NOZZLE =
            net.minecraft.state.property.BooleanProperty.of("nozzle");

    public FuelPumpBlock(Settings settings) {
        super(settings);
        this.setDefaultState(this.getStateManager().getDefaultState()
                .with(HALF, DoubleBlockHalf.LOWER)
                .with(FACING, Direction.NORTH)
                .with(NOZZLE, true));
    }

    @Override
    protected MapCodec<? extends BlockWithEntity> getCodec() {
        return CODEC;
    }

    @Override
    protected void appendProperties(StateManager.Builder<Block, BlockState> builder) {
        builder.add(HALF, FACING, NOZZLE);
    }

    // ---- the two halves ----

    @Override
    public BlockState getPlacementState(ItemPlacementContext context) {
        BlockPos pos = context.getBlockPos();
        if (!context.getWorld().getBlockState(pos.up()).isReplaceable()) {
            return null;
        }
        return this.getDefaultState()
                .with(FACING, context.getHorizontalPlayerFacing().getOpposite());
    }

    @Override
    public void onPlaced(World world, BlockPos pos, BlockState state, LivingEntity placer, ItemStack stack) {
        world.setBlockState(pos.up(), state.with(HALF, DoubleBlockHalf.UPPER), Block.NOTIFY_ALL);
    }

    /**
     * Breaking the top half in creative would leave the bottom to follow on its
     * own, and the bottom carries the loot: it is taken away here first, with
     * no drops, the way a door's is.
     */
    @Override
    public BlockState onBreak(World world, BlockPos pos, BlockState state, PlayerEntity player) {
        if (!world.isClient() && player.isCreative() && state.get(HALF) == DoubleBlockHalf.UPPER) {
            BlockPos below = pos.down();
            BlockState base = world.getBlockState(below);
            if (base.isOf(this) && base.get(HALF) == DoubleBlockHalf.LOWER) {
                world.setBlockState(below, Blocks.AIR.getDefaultState(), Block.NOTIFY_ALL | Block.SKIP_DROPS);
                world.syncWorldEvent(player, net.minecraft.world.WorldEvents.BLOCK_BROKEN, below,
                        Block.getRawIdFromState(base));
            }
        }
        return super.onBreak(world, pos, state, player);
    }

    @Override
    protected boolean canPlaceAt(BlockState state, WorldView world, BlockPos pos) {
        if (state.get(HALF) == DoubleBlockHalf.UPPER) {
            BlockState below = world.getBlockState(pos.down());
            return below.isOf(this) && below.get(HALF) == DoubleBlockHalf.LOWER;
        }
        return super.canPlaceAt(state, world, pos);
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
    protected BlockState getStateForNeighborUpdate(BlockState state, WorldView world,
                                                   ScheduledTickView tickView, BlockPos pos,
                                                   Direction direction, BlockPos neighborPos,
                                                   BlockState neighborState, Random random) {
        DoubleBlockHalf half = state.get(HALF);
        boolean towardsOtherHalf = direction.getAxis() == Direction.Axis.Y
                && (half == DoubleBlockHalf.LOWER) == (direction == Direction.UP);
        if (towardsOtherHalf && (!neighborState.isOf(this) || neighborState.get(HALF) == half)) {
            return Blocks.AIR.getDefaultState();
        }
        return super.getStateForNeighborUpdate(
                state, world, tickView, pos, direction, neighborPos, neighborState, random);
    }

    // ---- the block entity lives downstairs ----

    @Override
    public BlockEntity createBlockEntity(BlockPos pos, BlockState state) {
        return state.get(HALF) == DoubleBlockHalf.LOWER ? new FuelPumpBlockEntity(pos, state) : null;
    }

    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(
            World world, BlockState state, BlockEntityType<T> type) {
        if (world.isClient() || state.get(HALF) != DoubleBlockHalf.LOWER) {
            return null;
        }
        return validateTicker(type, ModBlockEntities.FUEL_PUMP, FuelPumpBlockEntity::tick);
    }

    /** The tank, wherever you knocked on the pump. */
    private static FuelPumpBlockEntity tankOf(World world, BlockPos pos, BlockState state) {
        BlockPos base = state.get(HALF) == DoubleBlockHalf.UPPER ? pos.down() : pos;
        return world.getBlockEntity(base) instanceof FuelPumpBlockEntity pump ? pump : null;
    }

    /**
     * The tank of the pump standing at {@code pos}, whichever half is there, or
     * null if that is not a pump at all. This is what a fuel pipe asks when it
     * wants to know whether the block it is pointing at can be filled.
     */
    public static FuelPumpBlockEntity tankAt(World world, BlockPos pos) {
        BlockState state = world.getBlockState(pos);
        return state.getBlock() instanceof FuelPumpBlock ? tankOf(world, pos, state) : null;
    }

    // ---- using it ----

    /** A bare hand takes the nozzle off its hook. */
    @Override
    protected ActionResult onUse(BlockState state, World world, BlockPos pos,
                                 PlayerEntity player, BlockHitResult hit) {
        if (!player.getMainHandStack().isEmpty()) {
            return ActionResult.PASS;
        }
        if (world.isClient()) {
            return ActionResult.SUCCESS;
        }
        FuelPumpBlockEntity pump = tankOf(world, pos, state);
        if (pump == null) {
            return ActionResult.CONSUME;
        }
        if (pump.takeNozzle(player)) {
            report(player, pump);
        }
        return ActionResult.CONSUME;
    }

    @Override
    protected ActionResult onUseWithItem(ItemStack held, BlockState state, World world, BlockPos pos,
                                         PlayerEntity player, Hand hand, BlockHitResult hit) {
        if (held.isOf(ModItems.FUEL_NOZZLE)) {
            BlockPos base = state.get(HALF) == DoubleBlockHalf.UPPER ? pos.down() : pos;
            if (!me.trossty.car.item.FuelNozzleItem.isFrom(held, base)) {
                return ActionResult.PASS;
            }
            if (!world.isClient()) {
                FuelPumpBlockEntity pump = tankOf(world, pos, state);
                if (pump != null) {
                    pump.hangUpNozzle(false);
                    report(player, pump);
                }
            }
            return ActionResult.SUCCESS;
        }
        if (!held.isOf(ModItems.JERRY_CAN)) {
            return ActionResult.PASS_TO_DEFAULT_BLOCK_ACTION;   // a bare hand reaches onUse
        }
        if (world.isClient()) {
            return ActionResult.SUCCESS;
        }
        FuelPumpBlockEntity pump = tankOf(world, pos, state);
        if (pump == null) {
            return ActionResult.CONSUME;
        }

        if (me.trossty.car.entity.CarEntity.holdsDiesel(held)) {
            player.sendMessage(Text.translatable("message.car.petrol_only"), true);
            return ActionResult.CONSUME;
        }
        if (!me.trossty.car.item.JerryCans.isEmpty(held) && me.trossty.car.entity.CarEntity.isRaw(held)) {
            // Raw petrol goes through the hydrotreater, not into a station's tank.
            player.sendMessage(Text.translatable("message.car.pump_raw"), true);
            return ActionResult.CONSUME;
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
        return ActionResult.CONSUME;
    }

    /**
     * Where the hose leaves the pump, in the world: low on its front, where
     * the coil hangs in the model. The renderer draws the hose from here and
     * the pump measures its reach from here, so the two always agree.
     */
    public static net.minecraft.util.math.Vec3d hoseOutlet(BlockPos base, Direction facing) {
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
        return new net.minecraft.util.math.Vec3d(base.getX() + rx, base.getY() + 20.5 / 16.0, base.getZ() + rz);
    }

    /** The hose outlet of the pump whose lower half is at {@code base}, or null if it is gone. */
    public static net.minecraft.util.math.@org.jspecify.annotations.Nullable Vec3d hoseOutlet(
            net.minecraft.world.BlockView world, BlockPos base) {
        BlockState state = world.getBlockState(base);
        if (!(state.getBlock() instanceof FuelPumpBlock)) {
            return null;
        }
        return hoseOutlet(base, state.get(FACING));
    }

    /** Hangs the nozzle back on, or takes it off: both halves, so neither looks wrong. */
    public static void setNozzleHung(World world, BlockPos base, boolean hung) {
        for (BlockPos at : new BlockPos[] {base, base.up()}) {
            BlockState state = world.getBlockState(at);
            if (state.getBlock() instanceof FuelPumpBlock && state.get(NOZZLE) != hung) {
                world.setBlockState(at, state.with(NOZZLE, hung), Block.NOTIFY_LISTENERS);
            }
        }
    }

    /** The action bar is the pump's only display, so every click updates it. */
    private static void report(PlayerEntity player, FuelPumpBlockEntity pump) {
        player.sendMessage(Text.translatable("message.car.pump_level",
                pump.getPetrol(), pump.getCapacity()), true);
    }

    private static void playPumpSound(World world, BlockPos pos, float pitch) {
        world.playSound(null, pos, SoundEvents.ITEM_BUCKET_EMPTY, SoundCategory.BLOCKS, 0.7F, pitch);
    }

    // ---- redstone ----

    @Override
    protected boolean hasComparatorOutput(BlockState state) {
        return true;
    }

    @Override
    protected int getComparatorOutput(BlockState state, World world, BlockPos pos, Direction direction) {
        FuelPumpBlockEntity pump = tankOf(world, pos, state);
        if (pump == null || pump.getPetrol() <= 0) {
            return 0;
        }
        return MathHelper.clamp(1 + pump.getPetrol() * 14 / pump.getCapacity(), 1, 15);
    }
}
