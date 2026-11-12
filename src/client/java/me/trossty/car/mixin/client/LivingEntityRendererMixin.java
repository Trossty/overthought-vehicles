package me.trossty.car.mixin.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import me.trossty.car.client.render.CarEntityRenderer;
import me.trossty.car.client.render.RiderPose;
import me.trossty.car.entity.CarEntity;
import me.trossty.car.vehicle.VehicleDefinition;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;
import net.minecraft.world.entity.LivingEntity;
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

    @Inject(method = "extractRenderState(Lnet/minecraft/world/entity/LivingEntity;Lnet/minecraft/client/renderer/entity/state/LivingEntityRenderState;F)V",
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
                - (float) (rider.getPosition(tickProgress).y - vehicle.getPosition(tickProgress).y);
        pose.car$setPose(grip, steer, lean, pivot, vehicle.getYRot(tickProgress));
    }

    @Inject(method = "setupRotations", at = @At("HEAD"))
    private void car$leanWithBike(LivingEntityRenderState state, PoseStack matrices, float bodyYaw,
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
        matrices.mulPose(Axis.YP.rotationDegrees(facing));
        matrices.translate(0.0F, pivot, 0.0F);
        matrices.mulPose(Axis.ZP.rotationDegrees(lean));
        matrices.translate(0.0F, -pivot, 0.0F);
        matrices.mulPose(Axis.YP.rotationDegrees(-facing));
    }
}
