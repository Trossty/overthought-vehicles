package me.trossty.car.mixin.client;

import me.trossty.car.client.sound.RoadSurface;
import me.trossty.car.entity.CarEntity;
import net.minecraft.client.Camera;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.phys.Vec3;
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
    protected abstract void setPosition(Vec3 pos);

    @Shadow
    public abstract Vec3 getPosition();

    @Inject(method = "setup", at = @At("TAIL"))
    private void car$roadShake(BlockGetter world, Entity focused, boolean thirdPerson, boolean inverseView,
                               float tickProgress, CallbackInfo ci) {
        if (focused == null || !(focused.getVehicle() instanceof CarEntity car)) {
            return;
        }
        float shake = RoadSurface.shake(car);
        if (shake <= 0.01F) {
            return;
        }
        float time = car.tickCount + tickProgress;
        // Two out-of-step waves read as a rough surface rather than a wobble.
        float bump = Mth.sin(time * 2.7F) * 0.6F + Mth.sin(time * 6.1F + 1.3F) * 0.4F;
        this.setPosition(this.getPosition().add(0.0, bump * shake * 0.035, 0.0));
    }
}
