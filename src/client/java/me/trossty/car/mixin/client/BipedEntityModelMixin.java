package me.trossty.car.mixin.client;

import me.trossty.car.client.render.RiderPose;
import net.minecraft.client.model.ModelPart;
import net.minecraft.client.render.entity.model.BipedEntityModel;
import net.minecraft.client.render.entity.state.BipedEntityRenderState;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * The driver's hands go on the controls and follow them: on a wheel one hand
 * rises as the other drops, on handlebars one pushes as the other pulls. A
 * rider straddles the bike with knees in rather than sitting as on a horse.
 */
@Mixin(BipedEntityModel.class)
public abstract class BipedEntityModelMixin {

    @Shadow
    @Final
    public ModelPart rightArm;
    @Shadow
    @Final
    public ModelPart leftArm;
    @Shadow
    @Final
    public ModelPart rightLeg;
    @Shadow
    @Final
    public ModelPart leftLeg;

    @Inject(method = "setAngles(Lnet/minecraft/client/render/entity/state/BipedEntityRenderState;)V",
            at = @At("TAIL"))
    private void car$handsOnControls(BipedEntityRenderState state, CallbackInfo ci) {
        if (!(state instanceof RiderPose pose) || pose.car$grip() == RiderPose.GRIP_NONE
                || state.isUsingItem || state.handSwingProgress > 0.0F) {
            return;
        }
        float steer = pose.car$steer() * ((float) Math.PI / 180.0F);
        if (pose.car$grip() == RiderPose.GRIP_WHEEL) {
            // Reaching forward to the rim, a little in; turning right lifts
            // the left hand and lowers the right.
            this.rightArm.pitch = -1.05F + steer * 0.45F;
            this.leftArm.pitch = -1.05F - steer * 0.45F;
            this.rightArm.yaw = -0.32F;
            this.leftArm.yaw = 0.32F;
            this.rightArm.roll = 0.0F;
            this.leftArm.roll = 0.0F;
        } else {
            // Out wide to the grips, lower; the bar swings, so one arm
            // reaches as the other draws back.
            this.rightArm.pitch = -1.2F - steer * 0.35F;
            this.leftArm.pitch = -1.2F + steer * 0.35F;
            this.rightArm.yaw = 0.12F;
            this.leftArm.yaw = -0.12F;
            this.rightArm.roll = 0.0F;
            this.leftArm.roll = 0.0F;
            this.rightLeg.pitch = -1.1F;
            this.leftLeg.pitch = -1.1F;
            this.rightLeg.yaw = 0.12F;
            this.leftLeg.yaw = -0.12F;
            this.rightLeg.roll = 0.05F;
            this.leftLeg.roll = -0.05F;
        }
    }
}
