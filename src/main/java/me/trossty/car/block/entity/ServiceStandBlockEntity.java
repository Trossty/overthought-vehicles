package me.trossty.car.block.entity;

import me.trossty.car.block.ServiceStandBlock;
import me.trossty.car.entity.CarEntity;
import me.trossty.car.registry.ModBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;

/** Keeps the boat or plane on a stand where the stand holds it, every tick. */
public class ServiceStandBlockEntity extends BlockEntity {

    public ServiceStandBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.SERVICE_STAND, pos, state);
    }

    public static void tick(Level world, BlockPos pos, BlockState state, ServiceStandBlockEntity entity) {
        if (!(state.getBlock() instanceof ServiceStandBlock stand)) {
            return;
        }
        double seat = stand.seat(state);
        if (Double.isNaN(seat)) {
            return;
        }
        for (CarEntity vehicle : world.getEntitiesOfClass(CarEntity.class, new AABB(pos).inflate(3.0),
                vehicle -> !vehicle.isRemoved() && stand.holds(state, pos, vehicle))) {
            VehicleLiftBlockEntity.pin(vehicle, pos.getY() + seat);
        }
    }
}
