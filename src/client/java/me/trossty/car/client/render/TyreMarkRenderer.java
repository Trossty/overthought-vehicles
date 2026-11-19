package me.trossty.car.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import me.trossty.car.entity.TyreMarks;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.phys.Vec3;

/**
 * The tyre marks laid flat on the ground: dark strips that fade out over the
 * last third of their life, lit like the ground under them.
 */
public final class TyreMarkRenderer {

    private static final RenderType MARK = RenderType.entityTranslucent(
            ResourceLocation.withDefaultNamespace("textures/misc/white.png"));
    /** Drawn this far above the ground, so the road does not show through. */
    private static final double LIFT = 0.012;
    /** Marks further than this from the camera, in blocks, are not drawn. */
    private static final double RANGE = 96.0;

    private static ClientLevel level;

    private TyreMarkRenderer() {
    }

    public static void render(WorldRenderContext context) {
        ClientLevel world = Minecraft.getInstance().level;
        if (world != level) {
            // Another world, or the same one joined again: none of the old marks belong.
            TyreMarks.clear();
            level = world;
        }
        if (world == null) {
            return;
        }
        long now = world.getGameTime();
        Vec3 camera = context.camera();
        PoseStack.Pose pose = context.matrices().last();
        VertexConsumer out = context.consumers().getBuffer(MARK);
        TyreMarks.forEach(now, mark -> {
            if (camera.distanceToSqr(mark.x2(), mark.y2(), mark.z2()) > RANGE * RANGE) {
                return;
            }
            long age = now - mark.born();
            float fade = Math.min(1.0F, (TyreMarks.LIFETIME - age) / (TyreMarks.LIFETIME / 3.0F));
            int alpha = (int) (mark.dark() * fade * 255.0F);
            if (alpha <= 2) {
                return;
            }
            int colour = alpha << 24 | 0x141414;
            double dx = mark.x2() - mark.x1();
            double dz = mark.z2() - mark.z1();
            double length = Math.sqrt(dx * dx + dz * dz);
            if (length < 1.0E-4) {
                return;
            }
            // Half the width out to each side, square to the way the wheel went.
            double sx = -dz / length * mark.width() / 2.0;
            double sz = dx / length * mark.width() / 2.0;
            int light = LevelRenderer.getLightColor(world, BlockPos.containing(mark.x2(), mark.y2() + 0.1, mark.z2()));
            float y1 = (float) (mark.y1() + LIFT - camera.y);
            float y2 = (float) (mark.y2() + LIFT - camera.y);
            vertex(pose, out, colour, light, mark.x1() - sx - camera.x, y1, mark.z1() - sz - camera.z);
            vertex(pose, out, colour, light, mark.x1() + sx - camera.x, y1, mark.z1() + sz - camera.z);
            vertex(pose, out, colour, light, mark.x2() + sx - camera.x, y2, mark.z2() + sz - camera.z);
            vertex(pose, out, colour, light, mark.x2() - sx - camera.x, y2, mark.z2() - sz - camera.z);
        });
    }

    private static void vertex(PoseStack.Pose pose, VertexConsumer out, int colour, int light,
                               double x, float y, double z) {
        org.joml.Vector3f at = pose.pose().transformPosition((float) x, y, (float) z, new org.joml.Vector3f());
        out.addVertex(at.x(), at.y(), at.z(), colour, 0.5F, 0.5F, OverlayTexture.NO_OVERLAY, light, 0.0F, 1.0F, 0.0F);
    }
}
