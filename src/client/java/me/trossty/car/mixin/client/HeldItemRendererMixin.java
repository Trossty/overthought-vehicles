package me.trossty.car.mixin.client;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.llamalad7.mixinextras.sugar.Local;
import com.mojang.blaze3d.vertex.PoseStack;
import me.trossty.car.client.render.ObdTabletRenderer;
import me.trossty.car.registry.ModItems;
import net.minecraft.client.renderer.ItemInHandRenderer;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * The OBD tablet is held up the way a map is: in both hands when the other
 * hand is free, tilting up into view as you look down at it. The game decides
 * "hold it like a map" by asking whether the item is a map; the tablet says
 * yes, and its screen is drawn where the map's paper would have been.
 */
@Mixin(ItemInHandRenderer.class)
abstract class HeldItemRendererMixin {

    @ModifyExpressionValue(method = "renderArmWithItem",
            at = @At(value = "INVOKE",
                    target = "Lnet/minecraft/world/item/ItemStack;has(Lnet/minecraft/core/component/DataComponentType;)Z",
                    ordinal = 0))
    private boolean car$holdTabletLikeMap(boolean isMap, @Local(argsOnly = true) ItemStack item) {
        return isMap || item.is(ModItems.OBD_SCANNER);
    }

    @Inject(method = "renderMap", at = @At("HEAD"), cancellable = true)
    private void car$drawTablet(PoseStack matrices, SubmitNodeCollector queue, int light, ItemStack stack,
                                CallbackInfo ci) {
        if (stack.is(ModItems.OBD_SCANNER)) {
            ObdTabletRenderer.draw(matrices, queue, light, stack);
            ci.cancel();
        }
    }
}
