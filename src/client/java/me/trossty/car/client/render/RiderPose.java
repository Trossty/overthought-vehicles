package me.trossty.car.client.render;

import me.trossty.car.entity.CarEntity;
import me.trossty.car.vehicle.VehicleDefinition;
import net.minecraft.world.entity.Entity;

/**
 * What a rider's drawing needs to know about the vehicle under them: how far
 * the bike leans and about what, and where their hands go.
 *
 * @param lean       degrees of lean about the vehicle's own forward axis
 * @param leanPivot  height of the lean's pivot above the rider's feet, in blocks
 */
public record RiderPose(int grip, float steer, float lean, float leanPivot, float vehicleYaw) {

    /** Not at the controls: drawn the vanilla way. */
    public static final int GRIP_NONE = 0;
    /** Hands on a steering wheel. */
    public static final int GRIP_WHEEL = 1;
    /** Hands on handlebars, knees in. */
    public static final int GRIP_BARS = 2;

    public static final RiderPose NONE = new RiderPose(GRIP_NONE, 0.0F, 0.0F, 0.0F, 0.0F);

    public static RiderPose of(Entity rider, float tickProgress) {
        if (!(rider.getVehicle() instanceof CarEntity vehicle)) {
            return NONE;
        }
        boolean bike = vehicle.chassis() == VehicleDefinition.Chassis.MOTORCYCLE;
        float steer = vehicle.getSteerAngle(tickProgress);
        boolean driving = vehicle.getControllingPassenger() == rider;
        int grip = !driving ? GRIP_NONE : bike ? GRIP_BARS : GRIP_WHEEL;
        float lean = bike ? CarEntityRenderer.leanDegrees(steer, vehicle.getEngineSpeed()) : 0.0F;
        float pivot = CarEntityRenderer.LEAN_PIVOT
                - (float) (rider.getPosition(tickProgress).y - vehicle.getPosition(tickProgress).y);
        return new RiderPose(grip, steer, lean, pivot, vehicle.getViewYRot(tickProgress));
    }
}
