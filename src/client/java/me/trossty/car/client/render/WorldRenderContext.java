package me.trossty.car.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;

/**
 * What the world-space overlays get once the entities are drawn: the matrices
 * (positions camera-relative), the buffers, where the camera is, and the
 * matrix that turns a camera-relative point into view space.
 */
public record WorldRenderContext(PoseStack matrices, MultiBufferSource consumers, Vec3 camera, Matrix4f view) {

    /** The edges of a box given in world coordinates, in one ARGB colour. */
    public void outline(VertexConsumer lines, AABB box, int argb) {
        LevelRenderer.renderLineBox(this.matrices, lines, box.move(this.camera.reverse()),
                (argb >> 16 & 0xFF) / 255.0F, (argb >> 8 & 0xFF) / 255.0F,
                (argb & 0xFF) / 255.0F, (argb >>> 24) / 255.0F);
    }
}
