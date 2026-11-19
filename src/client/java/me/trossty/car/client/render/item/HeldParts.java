package me.trossty.car.client.render.item;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import me.trossty.car.registry.ModItems;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.player.PlayerRenderer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;

/**
 * The bulky parts are carried in the hands rather than floating beside the
 * view: a tyre or a brake disc in both hands with its face towards you, a
 * jerry can by its handle in one. In first person they ride on the game's
 * own map hold (see HeldItemRendererMixin): one-handed it draws the arm and
 * this the part where the map's paper would be; two-handed this draws it all. In third person the
 * arms reach forward round a tyre (see BipedEntityModelMixin).
 */
public final class HeldParts {

    private HeldParts() {
    }

    /** Carried in both hands, face on, whenever the other hand is free. */
    public static boolean twoHanded(ItemStack stack) {
        return stack.is(ModItems.WHEEL) || stack.is(ModItems.BRAKE_DISCS);
    }

    /** Hangs from one hand by a handle. */
    public static boolean oneHanded(ItemStack stack) {
        return stack.is(ModItems.JERRY_CAN);
    }

    /** Held the way a map is, so the arm or arms show. */
    public static boolean carried(ItemStack stack) {
        return twoHanded(stack) || oneHanded(stack);
    }

    /** Whether this body has a tyre or disc in its arms, the other hand free. */
    public static boolean armsFull(LivingEntity entity) {
        return twoHanded(entity.getMainHandItem()) && entity.getOffhandItem().isEmpty();
    }

    /**
     * A part in one hand, drawn where a held map's paper would be: a can
     * hangs from the fist by its handle, side on; anything else faces you.
     */
    public static void draw(PoseStack pose, MultiBufferSource buffers, int light, ItemStack stack) {
        pose.pushPose();
        if (oneHanded(stack)) {
            pose.translate(0.0F, -0.3F, 0.0F);
            pose.mulPose(Axis.YP.rotationDegrees(-90.0F));
            pose.scale(0.36F, 0.36F, 0.36F);
        } else {
            pose.scale(0.3F, 0.3F, 0.3F);
            faceYou(pose);
        }
        part(pose, buffers, light, stack);
        pose.popPose();
    }

    /**
     * A tyre or disc held out low in front in both hands, face to you, a
     * fist on each side of it. Drawn in place of the two-handed map hold,
     * whose hands sit below the screen's edge.
     */
    public static void drawInArms(PoseStack pose, MultiBufferSource buffers, int light, float equip,
                                  float swing, ItemStack stack) {
        Minecraft client = Minecraft.getInstance();
        float bob = -0.25F * Mth.sin(Mth.sqrt(swing) * (float) Math.PI);
        pose.pushPose();
        pose.translate(0.0F, equip * -0.6F, bob);
        if (client.player != null && !client.player.isInvisible()
                && client.getEntityRenderDispatcher().getRenderer(client.player) instanceof PlayerRenderer hands) {
            fist(pose, buffers, light, hands, client, HumanoidArm.RIGHT);
            fist(pose, buffers, light, hands, client, HumanoidArm.LEFT);
        }
        pose.translate(0.0F, -0.27F, -0.8F);
        pose.scale(0.52F, 0.52F, 0.52F);
        faceYou(pose);
        part(pose, buffers, light, stack);
        pose.popPose();
    }

    /** One arm as the game draws an empty hand, brought in to grip the part's side. */
    private static void fist(PoseStack pose, MultiBufferSource buffers, int light, PlayerRenderer hands,
                             Minecraft client, HumanoidArm arm) {
        float f = arm == HumanoidArm.RIGHT ? 1.0F : -1.0F;
        pose.pushPose();
        pose.translate(f * (0.64F - INWARD), -0.6F + RAISE, -0.72F);
        pose.mulPose(Axis.YP.rotationDegrees(f * 45.0F));
        pose.translate(f * -1.0F, 3.6F, 3.5F);
        pose.mulPose(Axis.ZP.rotationDegrees(f * 120.0F));
        pose.mulPose(Axis.XP.rotationDegrees(200.0F));
        pose.mulPose(Axis.YP.rotationDegrees(f * -135.0F));
        pose.translate(f * 5.6F, 0.0F, 0.0F);
        if (arm == HumanoidArm.RIGHT) {
            hands.renderRightHand(pose, buffers, light, client.player);
        } else {
            hands.renderLeftHand(pose, buffers, light, client.player);
        }
        pose.popPose();
    }

    /** How far each fist comes in from where an empty hand hangs, and up. */
    private static final float INWARD = 0.24F;
    private static final float RAISE = 0.16F;

    /** A wheel or disc is drawn with its face to the north, as its icon shows it: turned round, that face is towards you. */
    private static void faceYou(PoseStack pose) {
        pose.mulPose(Axis.YP.rotationDegrees(180.0F));
    }

    private static void part(PoseStack pose, MultiBufferSource buffers, int light, ItemStack stack) {
        Minecraft client = Minecraft.getInstance();
        client.getItemRenderer().renderStatic(stack, ItemDisplayContext.FIXED, light,
                OverlayTexture.NO_OVERLAY, pose, buffers, client.level, 0);
    }

    /** How far a one-handed carry lifts the arm, so a can hanging from it is still in view. */
    public static float lift(ItemStack stack) {
        return oneHanded(stack) ? 0.22F : 0.0F;
    }
}
