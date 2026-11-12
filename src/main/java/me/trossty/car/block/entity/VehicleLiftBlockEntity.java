package me.trossty.car.block.entity;

import me.trossty.car.block.VehicleLiftBlock;
import me.trossty.car.entity.CarEntity;
import me.trossty.car.registry.ModBlockEntities;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.storage.ReadView;
import net.minecraft.storage.WriteView;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;

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
        this.markDirty();
        return this.wantUp;
    }

    public static void tick(World world, BlockPos pos, BlockState state, VehicleLiftBlockEntity lift) {
        int level = state.get(VehicleLiftBlock.LEVEL);
        int target = lift.wantUp || world.isReceivingRedstonePower(pos) ? MAX_LEVEL : 0;

        if (level != target) {
            lift.stepTimer++;
            if (lift.stepTimer >= TICKS_PER_STEP) {
                lift.stepTimer = 0;
                level += Integer.signum(target - level);
                world.setBlockState(pos, state.with(VehicleLiftBlock.LEVEL, level), Block.NOTIFY_ALL);
                world.playSound(null, pos,
                        target > 0 ? SoundEvents.BLOCK_PISTON_EXTEND : SoundEvents.BLOCK_PISTON_CONTRACT,
                        SoundCategory.BLOCKS, 0.3F, 0.6F);
            }
        } else {
            lift.stepTimer = 0;
        }

        if (level > 0) {
            hold(world, pos, level, state.get(VehicleLiftBlock.FACING));
        }
    }

    /**
     * Puts whatever is parked on the ramp exactly where the platform is. Called
     * every tick rather than once per step, because a car that is only moved
     * when the platform moves drifts away between steps under its own gravity.
     */
    private static void hold(World world, BlockPos pos, int level, net.minecraft.util.math.Direction facing) {
        double top = pos.getY() + 1.0 + RAISE_PER_LEVEL * level;
        boolean alongX = facing.getAxis() == net.minecraft.util.math.Direction.Axis.X;
        double reachX = alongX ? HOLD_LENGTH : HOLD_WIDTH;
        double reachZ = alongX ? HOLD_WIDTH : HOLD_LENGTH;
        Box over = new Box(
                pos.getX() + 0.5 - reachX, pos.getY(), pos.getZ() + 0.5 - reachZ,
                pos.getX() + 0.5 + reachX, pos.getY() + 3.0, pos.getZ() + 0.5 + reachZ);

        // A car counts as on the platform when its middle is over it, so one
        // parked alongside is left where it is.
        List<CarEntity> parked = world.getEntitiesByClass(CarEntity.class, over,
                car -> !car.isRemoved() && over.contains(car.getX(), over.minY + 0.1, car.getZ()));
        for (CarEntity car : parked) {
            // Nobody drives a car that is up on a lift. While somebody sits
            // at the wheel their game moves the car, not the server, so the
            // lift could not hold it: they are asked to get out.
            if (car.getControllingPassenger() != null) {
                car.removeAllPassengers();
            }
            car.setPosition(car.getX(), top, car.getZ());
            car.setVelocity(Vec3d.ZERO);
            car.fallDistance = 0.0;
            // Wheels off the ground: the engine can run, but nothing happens.
            car.setSpeed(0.0F);
        }
    }

    @Override
    protected void readData(ReadView view) {
        super.readData(view);
        this.wantUp = view.getBoolean("WantUp", false);
    }

    @Override
    protected void writeData(WriteView view) {
        super.writeData(view);
        view.putBoolean("WantUp", this.wantUp);
    }
}
