package me.trossty.car.mixin.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import me.trossty.car.entity.CarEntity;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * From the cockpit the horizon tilts as the aircraft banks: the view rolls
 * with the seat it is looked at from.
 */
@Mixin(GameRenderer.class)
public abstract class GameRendererMixin {

    @Inject(method = "bobHurt", at = @At("HEAD"))
    private void car$rollWithBank(PoseStack matrices, float tickProgress, CallbackInfo ci) {
        Minecraft client = Minecraft.getInstance();
        Entity viewer = client.getCameraEntity();
        if (viewer == null || !client.options.getCameraType().isFirstPerson()
                || !(viewer.getVehicle() instanceof CarEntity vehicle)) {
            return;
        }
        float roll = vehicle.getBodyRoll(tickProgress);
        if (Math.abs(roll) >= 0.01F) {
            matrices.mulPose(Axis.ZP.rotationDegrees(roll));
        }
    }
}
