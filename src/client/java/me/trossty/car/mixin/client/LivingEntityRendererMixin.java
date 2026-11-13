package me.trossty.car.mixin.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import me.trossty.car.client.render.RiderPose;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Riders go with their vehicle: whoever sits on a motorbike leans over with
 * it, about the same line it leans about, and an aircraft's crew pitch and
 * bank with it.
 */
@Mixin(LivingEntityRenderer.class)
public abstract class LivingEntityRendererMixin {

    @Inject(method = "setupRotations", at = @At("HEAD"))
    private void car$leanWithVehicle(LivingEntity rider, PoseStack matrices, float bob, float bodyYaw,
                                     float tickProgress, float scale, CallbackInfo ci) {
        RiderPose pose = RiderPose.of(rider, tickProgress);
        float lean = pose.lean();
        boolean tilted = Math.abs(pose.pitch()) >= 0.01F || Math.abs(pose.roll()) >= 0.01F;
        if (Math.abs(lean) < 0.01F && !tilted) {
            return;
        }
        // Into the vehicle's own frame, the same turn its renderer makes, lean
        // there, and back out; the rider's own turn comes after as usual.
        float facing = 180.0F - pose.vehicleYaw();
        matrices.mulPose(Axis.YP.rotationDegrees(facing));
        if (Math.abs(lean) >= 0.01F) {
            float pivot = pose.leanPivot();
            matrices.translate(0.0F, pivot, 0.0F);
            matrices.mulPose(Axis.ZP.rotationDegrees(lean));
            matrices.translate(0.0F, -pivot, 0.0F);
        }
        if (tilted && rider.getVehicle() != null) {
            // An aircraft's seat has already been carried round with it; the
            // rider tips with it about the hips, where they sit on that seat.
            float hips = (float) rider.getVehicleAttachmentPoint(rider.getVehicle()).y / scale;
            matrices.translate(0.0F, hips, 0.0F);
            matrices.mulPose(Axis.XP.rotationDegrees(pose.pitch()));
            matrices.mulPose(Axis.ZP.rotationDegrees(-pose.roll()));
            matrices.translate(0.0F, -hips, 0.0F);
        }
        matrices.mulPose(Axis.YP.rotationDegrees(-facing));
    }
}
