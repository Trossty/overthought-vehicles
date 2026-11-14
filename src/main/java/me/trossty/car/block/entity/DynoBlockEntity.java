package me.trossty.car.block.entity;

import me.trossty.car.block.DynoBlock;
import me.trossty.car.entity.CarEntity;
import me.trossty.car.registry.ModBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

/**
 * The rolling road's rollers. It keeps nothing: on the client it turns them
 * as fast as the wheels of the car standing on it, for the renderer to draw.
 */
public class DynoBlockEntity extends BlockEntity {

    /** Roller radius in blocks: a wheel's speed over this is the roller's turn. */
    private static final float RADIUS = 1.4F / 16.0F;

    private float angle;
    private float lastAngle;

    public DynoBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.DYNO, pos, state);
    }

    public static void clientTick(Level world, BlockPos pos, BlockState state, DynoBlockEntity dyno) {
        dyno.lastAngle = dyno.angle;
        CarEntity car = DynoBlock.carOn(world, pos);
        float speed = car == null || !car.isOnDyno() ? 0.0F : car.getEngineSpeed();
        // Capped so a car flat out reads as a blur rather than as standing still.
        dyno.angle += Math.max(-0.9F, Math.min(0.9F, speed / RADIUS));
    }

    public float getAngle(float tickProgress) {
        return this.lastAngle + (this.angle - this.lastAngle) * tickProgress;
    }
}
