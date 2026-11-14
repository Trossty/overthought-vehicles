package me.trossty.car.block.entity;

import me.trossty.car.block.VehicleLiftBlock;
import me.trossty.car.entity.CarEntity;
import me.trossty.car.registry.ModBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import me.trossty.car.util.ValueInput;
import me.trossty.car.util.ValueOutput;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import java.util.List;

/**
 * The half of the lift that actually lifts.
 *
 * <p>Until now the ramp was a gate: the game asked "is there a lift under this
 * car" and nothing moved. A lift that does not lift is a table. This raises the
 * car {@link #MAX_LEVEL} quarter-blocks into the air, which is the whole reason
 * garages own one — you cannot get under a car that is sitting on its wheels.
 *
 * <h2>Three stops</h2>
 * The lift stops at three heights, the way a garage lift is used: down on the
 * floor (where nothing can be worked on), {@link #STOPS a little way up} to
 * get at the wheels and under the bonnet at a comfortable height, and right
 * up to get underneath. A right click takes it up a stop, a sneaking one down
 * a stop. Redstone holds it right up for as long as it is powered; the stop
 * chosen by hand is remembered underneath, so the lift goes back to it when
 * the lever goes off.
 *
 * <h2>What it does to the car</h2>
 * While the platform is anywhere above the floor it owns whatever vehicle is
 * standing on it: the car tracks the platform exactly, up and down, and its
 * engine speed is held at zero because its wheels are off the ground. That is
 * also what stops a car rolling off a platform it is two feet above.
 *
 * <p>Nothing is pinned once the platform is down: at level 0 the lift lets go
 * entirely, so driving across a lowered lift is driving across a metal plate.
 */
public class VehicleLiftBlockEntity extends BlockEntity {

    /**
     * Steps of travel. The lift is a two-post garage lift standing on the
     * floor, and at the top its arms are two blocks up, which is as high as a
     * block model may reach and high enough to walk under the car.
     */
    public static final int MAX_LEVEL = 4;

    /** How far one level lifts the car, in blocks. */
    public static final double RAISE_PER_LEVEL = 7.5 / 16.0;

    /**
     * How far one level lifts a motorbike's table: it tops out about waist
     * high, where a bike is worked on, not over your head.
     */
    public static final double BIKE_RAISE_PER_LEVEL = 3.75 / 16.0;

    /** How high the arms' pads sit with the lift right down: on the base plate. */
    public static final double FLOOR = 2.0 / 16.0;

    /** Ticks per step. Two seconds from the floor to the top. */
    private static final int TICKS_PER_STEP = 10;

    /**
     * How far from the middle of the ramp a car still counts as being on it:
     * the platform is three blocks long, the way the lift faces, and two wide.
     */
    private static final double HOLD_LENGTH = 1.5;
    private static final double HOLD_WIDTH = 1.0;
    /** A bike's table is a narrow one, so a car parked alongside is left be. */
    private static final double BIKE_HOLD_WIDTH = 0.5;

    /** A bike goes on the bike lift and anything else with wheels on the car lift. */
    private static boolean fits(CarEntity car, boolean bike) {
        return (car.chassis() == me.trossty.car.vehicle.VehicleDefinition.Chassis.MOTORCYCLE) == bike;
    }

    /** Whether this is the motorbike lift, which takes bikes and only bikes. */
    public static boolean forBikes(BlockState state) {
        return state.getBlock() instanceof me.trossty.car.block.MotorcycleLiftBlock;
    }

    /** The heights the lift stops at, in levels: on the floor, a little way up, right up. */
    public static final int[] STOPS = {0, 1, MAX_LEVEL};

    /** The stop chosen by hand, an index into {@link #STOPS}; redstone is an override on top of this. */
    private int stop;

    private int stepTimer;

    public VehicleLiftBlockEntity(BlockPos pos, BlockState state) {
        super(forBikes(state) ? ModBlockEntities.MOTORCYCLE_LIFT : ModBlockEntities.VEHICLE_LIFT, pos, state);
    }

    public int getStop() {
        return this.stop;
    }

    /**
     * Moves the hand-chosen stop one up or down and reports the stop it is now
     * at; at the top or the bottom it stays where it is.
     */
    public int step(boolean up) {
        this.stop = Math.max(0, Math.min(STOPS.length - 1, this.stop + (up ? 1 : -1)));
        this.setChanged();
        return this.stop;
    }

    public static void tick(Level world, BlockPos pos, BlockState state, VehicleLiftBlockEntity lift) {
        int level = state.getValue(VehicleLiftBlock.LEVEL);
        int target = world.hasNeighborSignal(pos) ? MAX_LEVEL : STOPS[lift.stop];

        if (level == 0 && target > 0 && lift.stepTimer == 0) {
            board(world, pos, state.getValue(VehicleLiftBlock.FACING), forBikes(state));
        }
        if (level != target) {
            lift.stepTimer++;
            if (lift.stepTimer >= TICKS_PER_STEP) {
                lift.stepTimer = 0;
                int way = Integer.signum(target - level);
                level += way;
                world.setBlock(pos, state.setValue(VehicleLiftBlock.LEVEL, level), Block.UPDATE_ALL);
                world.playSound(null, pos,
                        way > 0 ? SoundEvents.PISTON_EXTEND : SoundEvents.PISTON_CONTRACT,
                        SoundSource.BLOCKS, 0.3F, 0.6F);
            }
        } else {
            lift.stepTimer = 0;
        }

        if (level > 0) {
            hold(world, pos, level, state.getValue(VehicleLiftBlock.FACING), forBikes(state));
        }
    }

    /**
     * Puts whatever is parked on the ramp exactly where the platform is. Called
     * every tick rather than once per step, because a car that is only moved
     * when the platform moves drifts away between steps under its own gravity.
     */
    private static void hold(Level world, BlockPos pos, int level, net.minecraft.core.Direction facing,
                             boolean bike) {
        double top = deckTop(pos, level, bike);
        AABB over = platform(pos, facing, bike);
        List<CarEntity> parked = world.getEntitiesOfClass(CarEntity.class, over,
                car -> !car.isRemoved() && over(over, car) && fits(car, bike));
        for (CarEntity car : parked) {
            // The arms take it under the sills, not under the tyres: it rises
            // only once they have come up to meet the body.
            // A bike stands on its table on its own wheels.
            pin(car, bike ? top : Math.max(pos.getY() + FLOOR, top - car.sillHeight()));
        }
    }

    /** How far from the lift a car is fetched on to it when it starts going up. */
    public static final double FETCH_REACH = 3.5;

    /**
     * Gets the car on to the platform as the lift starts up, the way a
     * mechanic would wave it on: the nearest car within {@link #FETCH_REACH},
     * or the one already roughly over it, is set down square in the middle and
     * turned to run the way the lift does. Nobody has to park to the block,
     * nor climb the plate's step.
     */
    private static void board(Level world, BlockPos pos, net.minecraft.core.Direction facing, boolean bike) {
        AABB around = new AABB(pos).inflate(FETCH_REACH, 1.0, FETCH_REACH);
        net.minecraft.world.phys.Vec3 middle = new net.minecraft.world.phys.Vec3(pos.getX() + 0.5, pos.getY() + FLOOR, pos.getZ() + 0.5);
        CarEntity nearest = null;
        double best = Double.MAX_VALUE;
        for (CarEntity car : world.getEntitiesOfClass(CarEntity.class, around, car -> !car.isRemoved()
                && car.chassis() != me.trossty.car.vehicle.VehicleDefinition.Chassis.BOAT
                && car.chassis() != me.trossty.car.vehicle.VehicleDefinition.Chassis.PLANE
                && fits(car, bike))) {
            double distance = car.position().distanceToSqr(middle);
            if (distance < best) {
                best = distance;
                nearest = car;
            }
        }
        if (nearest == null) {
            return;
        }
        if (nearest.isVehicle()) {
            nearest.ejectPassengers();
        }
        float along = facing.toYRot();
        float yaw = Math.abs(net.minecraft.util.Mth.wrapDegrees(nearest.getYRot() - along)) <= 90.0F
                ? along : along + 180.0F;
        nearest.moveTo(middle.x, deckTop(pos, 0, bike), middle.z, net.minecraft.util.Mth.wrapDegrees(yaw), nearest.getXRot());
        nearest.setYBodyRot(nearest.getYRot());
        nearest.setDeltaMovement(Vec3.ZERO);
        nearest.setSpeed(0.0F);
    }

    /** Where the deck is at this level: what a car on it stands on. */
    private static double deckTop(BlockPos pos, int level, boolean bike) {
        return pos.getY() + FLOOR + (bike ? BIKE_RAISE_PER_LEVEL : RAISE_PER_LEVEL) * level;
    }

    /** The space over the platform, three blocks long the way the lift faces and two wide. */
    private static AABB platform(BlockPos pos, net.minecraft.core.Direction facing, boolean bike) {
        boolean alongX = facing.getAxis() == net.minecraft.core.Direction.Axis.X;
        double width = bike ? BIKE_HOLD_WIDTH : HOLD_WIDTH;
        double reachX = alongX ? HOLD_LENGTH : width;
        double reachZ = alongX ? width : HOLD_LENGTH;
        return new AABB(
                pos.getX() + 0.5 - reachX, pos.getY(), pos.getZ() + 0.5 - reachZ,
                pos.getX() + 0.5 + reachX, pos.getY() + 3.0, pos.getZ() + 0.5 + reachZ);
    }

    /** A car counts as on the platform when its middle is over it, so one parked alongside is left be. */
    private static boolean over(AABB platform, net.minecraft.world.entity.Entity car) {
        return platform.contains(car.getX(), platform.minY + 0.1, car.getZ());
    }

    /**
     * Whether this lift has the car up in the air on its platform: raised,
     * the car's middle over it and the car standing on the deck. A car
     * parked alongside a raised lift is still on its wheels.
     */
    public static boolean holdsUp(BlockState state, BlockPos pos, net.minecraft.world.entity.Entity car) {
        int level = state.getValue(VehicleLiftBlock.LEVEL);
        boolean bike = forBikes(state);
        return level > 0 && car instanceof CarEntity vehicle && fits(vehicle, bike) && over(platform(pos, state.getValue(VehicleLiftBlock.FACING), bike), car)
                && car.getY() > pos.getY() - 0.1 && car.getY() < deckTop(pos, level, bike) + 0.3;
    }

    /**
     * Holds a vehicle up at this height with its wheels off the ground. Nobody
     * drives a car that is up on a lift: while somebody sits at the wheel their
     * game moves the car, not the server, so it could not be held, and they
     * are asked to get out. The engine can run, but nothing happens.
     */
    public static void pin(CarEntity car, double top) {
        if (car.getControllingPassenger() != null) {
            car.ejectPassengers();
        }
        car.setPos(car.getX(), top, car.getZ());
        car.setDeltaMovement(Vec3.ZERO);
        car.fallDistance = 0.0F;
        car.setSpeed(0.0F);
    }

    @Override
    protected void loadAdditional(net.minecraft.nbt.CompoundTag tag, net.minecraft.core.HolderLookup.Provider registries) {
        ValueInput view = ValueInput.of(tag, registries);
        super.loadAdditional(tag, registries);
        // Saved before the lift had stops: up meant right up.
        int saved = tag.contains("Stop") ? view.getIntOr("Stop", 0) : view.getBooleanOr("WantUp", false) ? STOPS.length - 1 : 0;
        this.stop = Math.max(0, Math.min(STOPS.length - 1, saved));
    }

    @Override
    protected void saveAdditional(net.minecraft.nbt.CompoundTag tag, net.minecraft.core.HolderLookup.Provider registries) {
        ValueOutput view = ValueOutput.of(tag, registries);
        super.saveAdditional(tag, registries);
        view.putInt("Stop", this.stop);
    }
}
