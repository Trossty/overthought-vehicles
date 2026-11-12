package me.trossty.car.entity;

import me.trossty.car.registry.ModItems;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

/**
 * Two wheels: a scooter or a motorbike. Underneath it is the car — the same
 * engine, fuel, parts, key and paint — in a narrower frame with a rider and
 * a pillion instead of a cabin. It leans into the turns; that is drawn, not
 * simulated, so it handles the way the car does, only quicker.
 */
public class MotorcycleEntity extends CarEntity {

    public MotorcycleEntity(EntityType<?> type, Level world) {
        super(type, world);
    }

    @Override
    protected int plainSeatCount() {
        return 2;
    }

    /** Rider in front, pillion behind, both on the centre line. */
    @Override
    protected Vec3 plainSeatPosition(int seat) {
        return new Vec3(0.0, 0.75, seat == 0 ? -0.15 : -0.6);
    }

    @Override
    protected Item getDropItem() {
        return ModItems.MOTORCYCLE;
    }

    /** Nobody aboard: down on its side stand. */
    @Override
    protected float restPoseTarget() {
        return this.isVehicle() ? 0.0F : 1.0F;
    }

    @Override
    public me.trossty.car.vehicle.VehicleDefinition.Chassis chassis() {
        return me.trossty.car.vehicle.VehicleDefinition.Chassis.MOTORCYCLE;
    }
}
