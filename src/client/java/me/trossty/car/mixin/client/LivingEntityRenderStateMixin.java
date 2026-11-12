package me.trossty.car.mixin.client;

import me.trossty.car.client.render.RiderPose;
import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;

/** Room on every living thing's render state for {@link RiderPose}. */
@Mixin(LivingEntityRenderState.class)
public abstract class LivingEntityRenderStateMixin implements RiderPose {

    @Unique
    private int car$grip;
    @Unique
    private float car$steer;
    @Unique
    private float car$lean;
    @Unique
    private float car$leanPivot;
    @Unique
    private float car$vehicleYaw;

    @Override
    public void car$setPose(int grip, float steer, float lean, float leanPivot, float vehicleYaw) {
        this.car$grip = grip;
        this.car$steer = steer;
        this.car$lean = lean;
        this.car$leanPivot = leanPivot;
        this.car$vehicleYaw = vehicleYaw;
    }

    @Override
    public int car$grip() {
        return this.car$grip;
    }

    @Override
    public float car$steer() {
        return this.car$steer;
    }

    @Override
    public float car$lean() {
        return this.car$lean;
    }

    @Override
    public float car$leanPivot() {
        return this.car$leanPivot;
    }

    @Override
    public float car$vehicleYaw() {
        return this.car$vehicleYaw;
    }
}
