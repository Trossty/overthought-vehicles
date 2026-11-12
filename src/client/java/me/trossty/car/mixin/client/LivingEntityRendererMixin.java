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
 * it, about the same line it leans about.
 */
@Mixin(LivingEntityRenderer.class)
public abstract class LivingEntityRendererMixin {

    @Inject(method = "setupRotations", at = @At("HEAD"))
    private void car$leanWithBike(LivingEntity rider, PoseStack matrices, float bob, float bodyYaw,
                                  float tickProgress, float scale, CallbackInfo ci) {
        RiderPose pose = RiderPose.of(rider, tickProgress);
        float lean = pose.lean();
        if (Math.abs(lean) < 0.01F) {
            return;
        }
        // Into the bike's own frame, the same turn its renderer makes, lean
        // there, and back out; the rider's own turn comes after as usual.
        float facing = 180.0F - pose.vehicleYaw();
        float pivot = pose.leanPivot();
        matrices.mulPose(Axis.YP.rotationDegrees(facing));
        matrices.translate(0.0F, pivot, 0.0F);
        matrices.mulPose(Axis.ZP.rotationDegrees(lean));
        matrices.translate(0.0F, -pivot, 0.0F);
        matrices.mulPose(Axis.YP.rotationDegrees(-facing));
    }
}
