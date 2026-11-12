package me.trossty.car.mixin.client;

import me.trossty.car.client.sound.RoadSurface;
import me.trossty.car.entity.CarEntity;
import net.minecraft.client.render.Camera;
import net.minecraft.entity.Entity;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Lets the driver feel the road: on gravel or a field the view jiggles with
 * the car, on a stone road it barely moves.
 */
@Mixin(Camera.class)
public abstract class CameraMixin {

    @Shadow
    protected abstract void setPos(Vec3d pos);

    @Shadow
    public abstract Vec3d getCameraPos();

    @Inject(method = "update", at = @At("TAIL"))
    private void car$roadShake(World world, Entity focused, boolean thirdPerson, boolean inverseView,
                               float tickProgress, CallbackInfo ci) {
        if (focused == null || !(focused.getVehicle() instanceof CarEntity car)) {
            return;
        }
        float shake = RoadSurface.shake(car);
        if (shake <= 0.01F) {
            return;
        }
        float time = car.age + tickProgress;
        // Two out-of-step waves read as a rough surface rather than a wobble.
        float bump = MathHelper.sin(time * 2.7F) * 0.6F + MathHelper.sin(time * 6.1F + 1.3F) * 0.4F;
        this.setPos(this.getCameraPos().add(0.0, bump * shake * 0.035, 0.0));
    }
}
