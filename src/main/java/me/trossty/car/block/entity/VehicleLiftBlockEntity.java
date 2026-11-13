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
 * <h2>Two ways up</h2>
 * Redstone holds it up for as long as it is powered, which is what you want in
 * a built garage. A sneaking right click latches it up or down by hand, which
 * is what you want everywhere else. The latch is remembered; the redstone is
 * only ever an override on top of it, so a lift that was left up stays up when
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

    /** Quarter-blocks of travel. Four of them is a block, which is all a single
     * block model may stick up above itself. */
    public static final int MAX_LEVEL = 4;

    /** How far one level lifts the car, in blocks. */
    public static final double RAISE_PER_LEVEL = 0.25;

    /** Ticks per quarter-block. A second from floor to full height. */
    private static final int TICKS_PER_STEP = 5;

    /**
     * How far from the middle of the ramp a car still counts as being on it:
     * the platform is three blocks long, the way the lift faces, and two wide.
     */
    private static final double HOLD_LENGTH = 1.5;
    private static final double HOLD_WIDTH = 1.0;

    /** Latched by hand; redstone is an override on top of this. */
    private boolean wantUp;

    private int stepTimer;

    public VehicleLiftBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.VEHICLE_LIFT, pos, state);
    }

    public boolean isWantedUp() {
        return this.wantUp;
    }

    /** Flips the hand latch and reports which way it now points. */
    public boolean toggle() {
        this.wantUp = !this.wantUp;
        this.setChanged();
        return this.wantUp;
    }

    public static void tick(Level world, BlockPos pos, BlockState state, VehicleLiftBlockEntity lift) {
        int level = state.getValue(VehicleLiftBlock.LEVEL);
        int target = lift.wantUp || world.hasNeighborSignal(pos) ? MAX_LEVEL : 0;

        if (level != target) {
            lift.stepTimer++;
            if (lift.stepTimer >= TICKS_PER_STEP) {
                lift.stepTimer = 0;
                level += Integer.signum(target - level);
                world.setBlock(pos, state.setValue(VehicleLiftBlock.LEVEL, level), Block.UPDATE_ALL);
                world.playSound(null, pos,
                        target > 0 ? SoundEvents.PISTON_EXTEND : SoundEvents.PISTON_CONTRACT,
                        SoundSource.BLOCKS, 0.3F, 0.6F);
            }
        } else {
            lift.stepTimer = 0;
        }

        if (level > 0) {
            hold(world, pos, level, state.getValue(VehicleLiftBlock.FACING));
        }
    }

    /**
     * Puts whatever is parked on the ramp exactly where the platform is. Called
     * every tick rather than once per step, because a car that is only moved
     * when the platform moves drifts away between steps under its own gravity.
     */
    private static void hold(Level world, BlockPos pos, int level, net.minecraft.core.Direction facing) {
        double top = pos.getY() + 1.0 + RAISE_PER_LEVEL * level;
        boolean alongX = facing.getAxis() == net.minecraft.core.Direction.Axis.X;
        double reachX = alongX ? HOLD_LENGTH : HOLD_WIDTH;
        double reachZ = alongX ? HOLD_WIDTH : HOLD_LENGTH;
        AABB over = new AABB(
                pos.getX() + 0.5 - reachX, pos.getY(), pos.getZ() + 0.5 - reachZ,
                pos.getX() + 0.5 + reachX, pos.getY() + 3.0, pos.getZ() + 0.5 + reachZ);

        // A car counts as on the platform when its middle is over it, so one
        // parked alongside is left where it is.
        List<CarEntity> parked = world.getEntitiesOfClass(CarEntity.class, over,
                car -> !car.isRemoved() && over.contains(car.getX(), over.minY + 0.1, car.getZ()));
        for (CarEntity car : parked) {
            pin(car, top);
        }
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
        this.wantUp = view.getBooleanOr("WantUp", false);
    }

    @Override
    protected void saveAdditional(net.minecraft.nbt.CompoundTag tag, net.minecraft.core.HolderLookup.Provider registries) {
        ValueOutput view = ValueOutput.of(tag, registries);
        super.saveAdditional(tag, registries);
        view.putBoolean("WantUp", this.wantUp);
    }
}
