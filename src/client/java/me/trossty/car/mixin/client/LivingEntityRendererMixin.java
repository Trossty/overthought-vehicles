package me.trossty.car.mixin.client;

import me.trossty.car.client.render.CarEntityRenderer;
import me.trossty.car.client.render.RiderPose;
import me.trossty.car.entity.CarEntity;
import me.trossty.car.vehicle.VehicleDefinition;
import net.minecraft.client.render.entity.LivingEntityRenderer;
import net.minecraft.client.render.entity.state.LivingEntityRenderState;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.LivingEntity;
import net.minecraft.util.math.RotationAxis;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Riders go with their vehicle: whoever sits on a motorbike leans over with
 * it, about the same line it leans about, and whoever is at the controls has
 * their hands on them.
 */
@Mixin(LivingEntityRenderer.class)
public abstract class LivingEntityRendererMixin {

    @Inject(method = "updateRenderState(Lnet/minecraft/entity/LivingEntity;Lnet/minecraft/client/render/entity/state/LivingEntityRenderState;F)V",
            at = @At("TAIL"))
    private void car$riderPose(LivingEntity rider, LivingEntityRenderState state, float tickProgress,
                               CallbackInfo ci) {
        RiderPose pose = (RiderPose) state;
        if (!(rider.getVehicle() instanceof CarEntity vehicle)) {
            pose.car$setPose(RiderPose.GRIP_NONE, 0.0F, 0.0F, 0.0F, 0.0F);
            return;
        }
        boolean bike = vehicle.chassis() == VehicleDefinition.Chassis.MOTORCYCLE;
        float steer = vehicle.getSteerAngle(tickProgress);
        boolean driving = vehicle.getControllingPassenger() == rider;
        int grip = !driving ? RiderPose.GRIP_NONE : bike ? RiderPose.GRIP_BARS : RiderPose.GRIP_WHEEL;
        float lean = bike ? CarEntityRenderer.leanDegrees(steer, vehicle.getEngineSpeed()) : 0.0F;
        float pivot = CarEntityRenderer.LEAN_PIVOT
                - (float) (rider.getLerpedPos(tickProgress).y - vehicle.getLerpedPos(tickProgress).y);
        pose.car$setPose(grip, steer, lean, pivot, vehicle.getLerpedYaw(tickProgress));
    }

    @Inject(method = "setupTransforms", at = @At("HEAD"))
    private void car$leanWithBike(LivingEntityRenderState state, MatrixStack matrices, float bodyYaw,
                                  float baseHeight, CallbackInfo ci) {
        RiderPose pose = (RiderPose) state;
        float lean = pose.car$lean();
        if (Math.abs(lean) < 0.01F) {
            return;
        }
        // Into the bike's own frame, the same turn its renderer makes, lean
        // there, and back out; the rider's own turn comes after as usual.
        float facing = 180.0F - pose.car$vehicleYaw();
        float pivot = pose.car$leanPivot();
        matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(facing));
        matrices.translate(0.0F, pivot, 0.0F);
        matrices.multiply(RotationAxis.POSITIVE_Z.rotationDegrees(lean));
        matrices.translate(0.0F, -pivot, 0.0F);
        matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(-facing));
    }
}
