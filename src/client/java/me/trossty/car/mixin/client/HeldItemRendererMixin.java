package me.trossty.car.mixin.client;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.llamalad7.mixinextras.sugar.Local;
import com.mojang.blaze3d.vertex.PoseStack;
import me.trossty.car.client.render.ObdTabletRenderer;
import me.trossty.car.client.render.item.HeldParts;
import me.trossty.car.registry.ModItems;
import net.minecraft.client.renderer.ItemInHandRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * The OBD tablet is held up the way a map is: in both hands when the other
 * hand is free, tilting up into view as you look down at it. The game decides
 * "hold it like a map" by asking whether the item is a map; the tablet says
 * yes, and its screen is drawn where the map's paper would have been. The
 * bulky parts ride the same hold so the hands show round them (HeldParts).
 */
@Mixin(ItemInHandRenderer.class)
abstract class HeldItemRendererMixin {

    @Shadow
    private ItemStack mainHandItem;
    @Shadow
    private ItemStack offHandItem;

    @ModifyExpressionValue(method = "renderArmWithItem",
            at = @At(value = "CONSTANT", args = "classValue=net/minecraft/world/item/MapItem", ordinal = 0))
    private boolean car$holdTabletLikeMap(boolean isMap, @Local(argsOnly = true) ItemStack item) {
        return isMap || item.is(ModItems.OBD_SCANNER) || HeldParts.carried(item);
    }

    /** A jerry can hangs from one hand even with the other one free. */
    @ModifyExpressionValue(method = "renderArmWithItem",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/world/item/ItemStack;isEmpty()Z", ordinal = 1))
    private boolean car$canInOneHand(boolean otherHandFree, @Local(argsOnly = true) ItemStack item) {
        return otherHandFree && !HeldParts.oneHanded(item);
    }

    @Inject(method = "renderTwoHandedMap", at = @At("HEAD"), cancellable = true)
    private void car$tyreInArms(PoseStack matrices, MultiBufferSource queue, int light, float pitch,
                                float equip, float swing, CallbackInfo ci) {
        if (HeldParts.twoHanded(this.mainHandItem)) {
            HeldParts.drawInArms(matrices, queue, light, equip, swing, this.mainHandItem);
            ci.cancel();
        }
    }

    @Inject(method = "renderOneHandedMap", at = @At("HEAD"))
    private void car$liftForCan(PoseStack matrices, MultiBufferSource queue, int light, float equip,
                                net.minecraft.world.entity.HumanoidArm arm, float swing, ItemStack stack,
                                CallbackInfo ci) {
        matrices.translate(0.0F, HeldParts.lift(stack), 0.0F);
    }

    @Inject(method = "renderMap", at = @At("HEAD"), cancellable = true)
    private void car$drawTablet(PoseStack matrices, MultiBufferSource queue, int light, ItemStack stack,
                                CallbackInfo ci) {
        if (stack.is(ModItems.OBD_SCANNER)) {
            ObdTabletRenderer.draw(matrices, queue, light, stack);
            ci.cancel();
        } else if (HeldParts.carried(stack)) {
            HeldParts.draw(matrices, queue, light, stack);
            ci.cancel();
        }
    }
}
