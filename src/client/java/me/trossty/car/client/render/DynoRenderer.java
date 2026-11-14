package me.trossty.car.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import me.trossty.car.block.DynoBlock;
import me.trossty.car.block.entity.DynoBlockEntity;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.world.level.block.state.BlockState;
import org.joml.Quaternionf;

/** Draws the dyno's two rollers, turning with the wheels of the car on them. */
public class DynoRenderer implements BlockEntityRenderer<DynoBlockEntity> {

    /** Where each roller's axle runs across the block, in sixteenths. */
    private static final float[] AXLES = {5.5F, 10.5F};
    private static final float AXLE_HEIGHT = 3.0F / 16.0F;

    public DynoRenderer(BlockEntityRendererProvider.Context context) {
    }

    @Override
    public void render(DynoBlockEntity dyno, float tickProgress, PoseStack matrices, MultiBufferSource buffers,
                       int light, int overlay) {
        BlockState state = dyno.getBlockState();
        if (!(state.getBlock() instanceof DynoBlock)) {
            return;
        }
        BlockState roller = state.setValue(DynoBlock.ROLLER, true);
        float angle = dyno.getAngle(tickProgress);
        for (float axle : AXLES) {
            matrices.pushPose();
            matrices.translate(0.0F, AXLE_HEIGHT, axle / 16.0F);
            matrices.mulPose(new Quaternionf().rotationX(angle));
            // The roller model is built round the block's middle; move it to this axle.
            matrices.translate(0.0F, -AXLE_HEIGHT, -0.5F);
            Minecraft.getInstance().getBlockRenderer().renderSingleBlock(roller, matrices, buffers, light,
                    OverlayTexture.NO_OVERLAY);
            matrices.popPose();
        }
    }
}
