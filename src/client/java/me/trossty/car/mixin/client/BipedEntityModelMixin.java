package me.trossty.car.mixin.client;

import me.trossty.car.client.render.RiderPose;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.EntityModel;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.LivingEntity;
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
@Mixin(HumanoidModel.class)
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

    /** A tyre or brake disc is carried in front in both arms, a little in. */
    @Inject(method = "setupAnim(Lnet/minecraft/world/entity/LivingEntity;FFFFF)V", at = @At("TAIL"))
    private void car$armsRoundAPart(LivingEntity body, float limbSwing, float limbSwingAmount,
                                    float ageInTicks, float headYaw, float headPitch, CallbackInfo ci) {
        if (body.getVehicle() != null || body.isUsingItem()
                || ((EntityModel<?>) (Object) this).attackTime > 0.0F
                || !me.trossty.car.client.render.item.HeldParts.armsFull(body)) {
            return;
        }
        float bob = Mth.cos(ageInTicks * 0.09F) * 0.03F;
        this.rightArm.xRot = -0.85F + bob;
        this.leftArm.xRot = -0.85F + bob;
        this.rightArm.yRot = -0.42F;
        this.leftArm.yRot = 0.42F;
        this.rightArm.zRot = 0.0F;
        this.leftArm.zRot = 0.0F;
    }

    @Inject(method = "setupAnim(Lnet/minecraft/world/entity/LivingEntity;FFFFF)V", at = @At("TAIL"))
    private void car$handsOnControls(LivingEntity rider, float limbSwing, float limbSwingAmount,
                                     float ageInTicks, float headYaw, float headPitch, CallbackInfo ci) {
        if (rider.getVehicle() == null || rider.isUsingItem()
                || ((EntityModel<?>) (Object) this).attackTime > 0.0F) {
            return;
        }
        RiderPose pose = RiderPose.of(rider,
                Minecraft.getInstance().getTimer().getGameTimeDeltaPartialTick(true));
        if (pose.grip() == RiderPose.GRIP_NONE) {
            return;
        }
        float steer = pose.steer() * ((float) Math.PI / 180.0F);
        if (pose.grip() == RiderPose.GRIP_WHEEL) {
            // Reaching forward to the rim, a little in; turning right lifts
            // the left hand and lowers the right.
            this.rightArm.xRot = -1.05F + steer * 0.45F;
            this.leftArm.xRot = -1.05F - steer * 0.45F;
            this.rightArm.yRot = -0.32F;
            this.leftArm.yRot = 0.32F;
            this.rightArm.zRot = 0.0F;
            this.leftArm.zRot = 0.0F;
        } else {
            // Out wide to the grips, lower; the bar swings, so one arm
            // reaches as the other draws back.
            this.rightArm.xRot = -1.2F - steer * 0.35F;
            this.leftArm.xRot = -1.2F + steer * 0.35F;
            this.rightArm.yRot = 0.12F;
            this.leftArm.yRot = -0.12F;
            this.rightArm.zRot = 0.0F;
            this.leftArm.zRot = 0.0F;
            this.rightLeg.xRot = -1.1F;
            this.leftLeg.xRot = -1.1F;
            this.rightLeg.yRot = 0.12F;
            this.leftLeg.yRot = -0.12F;
            this.rightLeg.zRot = 0.05F;
            this.leftLeg.zRot = -0.05F;
        }
    }
}
