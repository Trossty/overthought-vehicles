package me.trossty.car.block;

import com.mojang.serialization.MapCodec;
import me.trossty.car.block.entity.VehicleLiftBlockEntity;
import me.trossty.car.registry.ModBlockEntities;
import me.trossty.car.registry.ModBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * The ramp a car is assembled on, the only place one can be worked on
 * afterwards, and — since it is a lift — the thing that puts the car in the air
 * to do it.
 *
 * <p>Assembly happens in its window, which is the workbench's window fed a
 * different recipe list, and that works with the platform down: you build a car
 * standing on the floor. Repair is the other way about. Beating dents out is
 * still a matter of walking up with an iron ingot, but
 * {@link #isServicing(Level, Entity)} wants a lift that is actually
 * <em>up</em>, so the job happens where a garage would do it, on a car you can
 * get under.
 *
 * <p>A right click takes the lift up a stop and a sneaking one down a stop,
 * the one control it has besides redstone;
 * {@link VehicleLiftBlockEntity} explains how the two get along.
 */
public class VehicleLiftBlock extends ModelEntityBlock {

    public static final MapCodec<VehicleLiftBlock> CODEC = simpleCodec(VehicleLiftBlock::new);

    /** Quarter-blocks of travel, 0 being flat on the floor. */
    public static final IntegerProperty LEVEL =
            IntegerProperty.create("level", 0, VehicleLiftBlockEntity.MAX_LEVEL);

    /**
     * Which way the platform runs. It is longer than the block — a car is —
     * so it is laid along the way the player was looking when it went down,
     * which is the way a car will be driven on to it.
     */
    public static final net.minecraft.world.level.block.state.properties.EnumProperty<net.minecraft.core.Direction> FACING =
            net.minecraft.world.level.block.state.properties.BlockStateProperties.HORIZONTAL_FACING;

    /**
     * How far from a lift a car still counts as being on it. The car is 2.5
     * blocks long and nobody parks to the block, so this is deliberately
     * forgiving: anywhere over the ramp or a block either side of it.
     */
    private static final int REACH = 2;

    /**
     * The base plate a car is driven on to. The posts and arms are only
     * drawn: the car is held by the lift itself, not stood on a shape, and
     * the space under a raised car is left clear to walk into.
     */
    private static final VoxelShape PLATE = Block.box(0.0, 0.0, 0.0, 16.0, 2.0, 16.0);

    public VehicleLiftBlock(Properties settings) {
        super(settings);
        this.registerDefaultState(this.getStateDefinition().any().setValue(LEVEL, 0)
                .setValue(FACING, net.minecraft.core.Direction.NORTH));
    }

    @Override
    protected MapCodec<? extends BaseEntityBlock> codec() {
        return CODEC;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(LEVEL, FACING);
    }

    @Override
    public BlockState getStateForPlacement(net.minecraft.world.item.context.BlockPlaceContext context) {
        return this.defaultBlockState().setValue(FACING, context.getHorizontalDirection());
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter world, BlockPos pos, CollisionContext context) {
        return PLATE;
    }

    @Override
    protected VoxelShape getCollisionShape(BlockState state, BlockGetter world, BlockPos pos, CollisionContext context) {
        return PLATE;
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new VehicleLiftBlockEntity(pos, state);
    }

    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(
            Level world, BlockState state, BlockEntityType<T> type) {
        if (world.isClientSide()) {
            return null;
        }
        return createTickerHelper(type, VehicleLiftBlockEntity.forBikes(state)
                ? ModBlockEntities.MOTORCYCLE_LIFT : ModBlockEntities.VEHICLE_LIFT, VehicleLiftBlockEntity::tick);
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level world, BlockPos pos,
                                 Player player, BlockHitResult hit) {
        if (world.isClientSide()) {
            return InteractionResult.SUCCESS;
        }
        return working(world, pos, player);
    }

    /**
     * A click is the lift's lever: up a stop, or down one when sneaking. It
     * has no window.
     */
    private static InteractionResult working(Level world, BlockPos pos, Player player) {
        if (!(world.getBlockEntity(pos) instanceof VehicleLiftBlockEntity lift)) {
            return InteractionResult.CONSUME;
        }
        boolean up = !player.isShiftKeyDown();
        int before = lift.getStop();
        int stop = lift.step(up);
        world.playSound(null, pos, SoundEvents.LEVER_CLICK, SoundSource.BLOCKS, 0.4F, up ? 1.1F : 0.8F);
        Component where = Component.translatable("message.car.lift_stop_" + stop);
        player.displayClientMessage(stop == before
                ? Component.translatable(up ? "message.car.lift_at_top" : "message.car.lift_at_bottom", where)
                : Component.translatable("message.car.lift_stop", where), true);
        return InteractionResult.CONSUME;
    }

    /**
     * True when a lift under a vehicle has it up in the air on its platform;
     * a car parked beside a raised lift is not on it. A car on its wheels is a car you cannot
     * get under, which is the point of owning the lift in the first place.
     * A boat or a plane, which no lift takes, is up on its cradle or jack.
     */
    public static boolean isServicing(Level world, Entity vehicle) {
        if (vehicle instanceof me.trossty.car.entity.CarEntity car && ServiceStandBlock.isServicing(world, car)) {
            return true;
        }
        BlockPos at = vehicle.blockPosition();
        for (BlockPos pos : BlockPos.betweenClosed(
                at.offset(-REACH, -3, -REACH), at.offset(REACH, 0, REACH))) {
            BlockState state = world.getBlockState(pos);
            if ((state.is(ModBlocks.VEHICLE_LIFT) || state.is(ModBlocks.MOTORCYCLE_LIFT))
                    && VehicleLiftBlockEntity.holdsUp(state, pos, vehicle)) {
                return true;
            }
        }
        return false;
    }

    /** What to tell somebody working on a vehicle that is not up where it can be. */
    public static Component needsLift(me.trossty.car.entity.CarEntity vehicle) {
        return Component.translatable(switch (vehicle.chassis()) {
            case BOAT -> "message.car.needs_cradle";
            case PLANE -> "message.car.needs_jack";
            case MOTORCYCLE -> "message.car.needs_bike_lift";
            default -> "message.car.needs_lift";
        });
    }
}
