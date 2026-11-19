package me.trossty.car.block;

import com.mojang.serialization.MapCodec;
import me.trossty.car.block.entity.ServiceStandBlockEntity;
import me.trossty.car.entity.CarEntity;
import me.trossty.car.registry.ModBlockEntities;
import me.trossty.car.vehicle.VehicleDefinition;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
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
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.EntityCollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jetbrains.annotations.Nullable;

/**
 * A boat cradle: the padded bunks a hull is set down on out of the water,
 * which is where a boat is worked on, since it cannot be driven up on to a
 * car lift. A boat whose middle is over the cradle is held on its bunks, so
 * {@link VehicleLiftBlock#isServicing} lets its motor be looked after there.
 *
 * <p>It is got there off a flatbed: backed up to the cradle, the winch lets
 * the boat down on to it ({@link #near}), and winches it back off the same way.
 * A boat item can also be set straight down on it, or a beached boat shoved on.
 *
 * <p>The aircraft jack ({@link AircraftJackBlock}) is the same thing for a
 * plane, with a pump to raise it.
 */
public class ServiceStandBlock extends ModelEntityBlock {

    public static final MapCodec<ServiceStandBlock> CODEC = simpleCodec(ServiceStandBlock::new);

    /** The bunks run the way the player looked placing it: the way the boat lies. */
    public static final EnumProperty<Direction> FACING = BlockStateProperties.HORIZONTAL_FACING;

    /** How far a vehicle's middle may be from the stand's and still be on it, in blocks. */
    private static final double REACH = 2.0;

    private static final VoxelShape CRADLE = Block.box(0.0, 0.0, 0.0, 16.0, 8.0, 16.0);

    public ServiceStandBlock(Properties settings) {
        super(settings);
        this.registerDefaultState(this.getStateDefinition().any().setValue(FACING, Direction.NORTH));
    }

    @Override
    protected MapCodec<? extends BaseEntityBlock> codec() {
        return CODEC;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING);
    }

    @Override
    public BlockState getStateForPlacement(net.minecraft.world.item.context.BlockPlaceContext context) {
        return this.defaultBlockState().setValue(FACING, context.getHorizontalDirection());
    }

    /** What sort of vehicle sits on it. */
    public VehicleDefinition.Chassis takes() {
        return VehicleDefinition.Chassis.BOAT;
    }

    /**
     * How far above the block's floor a vehicle on it sits, or NaN while it
     * holds nothing up. A hull's keel rests on the bunks' tops.
     */
    public double seat(BlockState state) {
        return 0.5;
    }

    /**
     * How far ahead of a vehicle's own middle the part that rests on the stand
     * is, in blocks: a hull is longest behind, round the motor.
     */
    public double under() {
        return -0.5;
    }

    /** The shape a player walks into. */
    protected VoxelShape body(BlockState state) {
        return CRADLE;
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter world, BlockPos pos, CollisionContext context) {
        return this.body(state);
    }

    /**
     * Vehicles go through it: the one on it is held where it sits, and one
     * rolling over it to get there must not climb it first.
     */
    @Override
    protected VoxelShape getCollisionShape(BlockState state, BlockGetter world, BlockPos pos, CollisionContext context) {
        if (context instanceof EntityCollisionContext entity && entity.getEntity() instanceof CarEntity) {
            return Shapes.empty();
        }
        return this.body(state);
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new ServiceStandBlockEntity(pos, state);
    }

    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level world, BlockState state,
                                                                  BlockEntityType<T> type) {
        return world.isClientSide() ? null
                : createTickerHelper(type, ModBlockEntities.SERVICE_STAND, ServiceStandBlockEntity::tick);
    }

    /** Whether this stand at this state holds this vehicle up off the ground. */
    public boolean holds(BlockState state, BlockPos pos, CarEntity vehicle) {
        // One being driven is passing by, not parked on it: a boat cruising
        // past a cradle on the shore, a plane flying low over a jack.
        if (vehicle.chassis() != this.takes() || Double.isNaN(this.seat(state))
                || vehicle.getControllingPassenger() != null) {
            return false;
        }
        double dx = vehicle.getX() - (pos.getX() + 0.5);
        double dz = vehicle.getZ() - (pos.getZ() + 0.5);
        double dy = vehicle.getY() - pos.getY();
        return dx * dx + dz * dz <= REACH * REACH && dy > -0.5 && dy < 3.0;
    }

    /** Whether something already sits on this stand. */
    private boolean occupied(Level world, BlockPos pos, BlockState state) {
        return !world.getEntitiesOfClass(CarEntity.class, new net.minecraft.world.phys.AABB(pos).inflate(3.0),
                vehicle -> vehicle.chassis() == this.takes() && !vehicle.isRemoved()
                        && vehicle.getControllingPassenger() == null
                        && vehicle.distanceToSqr(Vec3.atBottomCenterOf(pos)) <= (REACH + 0.5) * (REACH + 0.5)).isEmpty();
    }

    /** True when a boat or plane is up on a stand that takes it. */
    public static boolean isServicing(Level world, CarEntity vehicle) {
        BlockPos at = vehicle.blockPosition();
        for (BlockPos pos : BlockPos.betweenClosed(at.offset(-2, -2, -2), at.offset(2, 0, 2))) {
            BlockState state = world.getBlockState(pos);
            if (state.getBlock() instanceof ServiceStandBlock stand && stand.holds(state, pos, vehicle)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Where a vehicle facing this way goes to sit on the nearest stand within
     * a few blocks of this spot that takes it, or null: where a trailer sets
     * its load down when it is backed up to one.
     */
    public static @Nullable Vec3 near(Level world, Vec3 spot, float yaw, CarEntity vehicle) {
        BlockPos at = BlockPos.containing(spot);
        Vec3 best = null;
        double bestDistance = 3.0 * 3.0;
        for (BlockPos pos : BlockPos.betweenClosed(at.offset(-3, -1, -3), at.offset(3, 1, 3))) {
            BlockState state = world.getBlockState(pos);
            if (state.getBlock() instanceof ServiceStandBlock stand && stand.takes() == vehicle.chassis()
                    && !stand.occupied(world, pos.immutable(), state)) {
                float rad = yaw * net.minecraft.util.Mth.DEG_TO_RAD;
                Vec3 middle = Vec3.atBottomCenterOf(pos).add(net.minecraft.util.Mth.sin(rad) * stand.under(), 0.0,
                        -net.minecraft.util.Mth.cos(rad) * stand.under());
                double distance = middle.distanceToSqr(spot.x, pos.getY(), spot.z);
                if (distance < bestDistance) {
                    best = middle;
                    bestDistance = distance;
                }
            }
        }
        return best;
    }
}
