package me.trossty.car.mixin.client;

import me.trossty.car.client.render.HeadlightGlow;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Mobs and players caught in a car's head lamps are drawn lit by them. */
@Mixin(EntityRenderer.class)
public abstract class EntityRendererMixin {

    @Inject(method = "getBlockLightLevel", at = @At("RETURN"), cancellable = true)
    private void car$headlights(Entity entity, BlockPos pos, CallbackInfoReturnable<Integer> cir) {
        // A car is not lit by its own lamps, nor is anybody sitting in it.
        Entity car = entity.getRootVehicle();
        if (car instanceof me.trossty.car.entity.CarEntity lit
                && me.trossty.car.client.render.CarEntityRenderer.lampsOn(lit)) {
            return;
        }
        int extra = HeadlightGlow.lightAt(pos);
        if (extra > cir.getReturnValueI()) {
            cir.setReturnValue(extra);
        }
    }
}
