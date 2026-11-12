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
import net.minecraft.world.phys.shapes.Shapes;
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
 * <p>A plain right click opens the window. A sneaking one works the lift, which
 * is the one control it has besides redstone;
 * {@link VehicleLiftBlockEntity} explains how the two get along.
 */
public class VehicleLiftBlock extends BaseEntityBlock {

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

    /** The floor plate, which is the whole block when the lift is down. */
    private static final VoxelShape PLATE = Block.box(0.0, 0.0, 0.0, 16.0, 16.0, 16.0);

    private final VoxelShape[] shapes = buildShapes();

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

    /**
     * One shape per level: the plate, the ram that comes out of it and the deck
     * on top. The deck is what a car — or a player who climbed up — stands on,
     * so it has to be in the collision shape and not just in the model.
     */
    private static VoxelShape[] buildShapes() {
        VoxelShape[] shapes = new VoxelShape[VehicleLiftBlockEntity.MAX_LEVEL + 1];
        shapes[0] = PLATE;
        for (int level = 1; level < shapes.length; level++) {
            double deck = 12.0 + 4.0 * level;
            shapes[level] = Shapes.or(
                    PLATE,
                    Block.box(6.0, 16.0, 6.0, 10.0, deck, 10.0),
                    Block.box(1.0, deck, 1.0, 15.0, deck + 4.0, 15.0));
        }
        return shapes;
    }

    @Override
    public BlockState getStateForPlacement(net.minecraft.world.item.context.BlockPlaceContext context) {
        return this.defaultBlockState().setValue(FACING, context.getHorizontalDirection());
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter world, BlockPos pos, CollisionContext context) {
        return this.shapes[state.getValue(LEVEL)];
    }

    @Override
    protected VoxelShape getCollisionShape(BlockState state, BlockGetter world, BlockPos pos, CollisionContext context) {
        return this.shapes[state.getValue(LEVEL)];
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
        return createTickerHelper(type, ModBlockEntities.VEHICLE_LIFT, VehicleLiftBlockEntity::tick);
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level world, BlockPos pos,
                                 Player player, BlockHitResult hit) {
        if (world.isClientSide()) {
            return InteractionResult.SUCCESS;
        }
        return working(world, pos, player);
    }

    /** A click is the lift's lever: latch it up, latch it down. It has no window. */
    private static InteractionResult working(Level world, BlockPos pos, Player player) {
        if (!(world.getBlockEntity(pos) instanceof VehicleLiftBlockEntity lift)) {
            return InteractionResult.CONSUME;
        }
        boolean up = lift.toggle();
        world.playSound(null, pos, SoundEvents.LEVER_CLICK, SoundSource.BLOCKS, 0.4F, up ? 1.1F : 0.8F);
        player.displayClientMessage(Component.translatable(up ? "message.car.lift_up" : "message.car.lift_down"), true);
        return InteractionResult.CONSUME;
    }

    /**
     * True when there is a lift close enough under a vehicle to work on it, and
     * that lift has the car in the air. A car on its wheels is a car you cannot
     * get under, which is the point of owning the lift in the first place.
     */
    public static boolean isServicing(Level world, Entity vehicle) {
        BlockPos at = vehicle.blockPosition();
        for (BlockPos pos : BlockPos.betweenClosed(
                at.offset(-REACH, -3, -REACH), at.offset(REACH, 0, REACH))) {
            BlockState state = world.getBlockState(pos);
            if (state.is(ModBlocks.VEHICLE_LIFT) && state.getValue(LEVEL) > 0) {
                return true;
            }
        }
        return false;
    }
}
