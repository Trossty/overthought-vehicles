package me.trossty.car.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import me.trossty.car.block.EngineHoistBlock;
import me.trossty.car.block.entity.EngineHoistBlockEntity;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.Direction;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.AABB;

/** The engine hanging from a hoist's hook, out along the boom. The hoist itself is the block model. */
public class EngineHoistRenderer implements BlockEntityRenderer<EngineHoistBlockEntity> {

    /** How big the engine is drawn, and how far its middle hangs below the hook, in blocks. */
    private static final float ENGINE_SCALE = 0.6F;
    private static final float BELOW_HOOK = 0.24F;

    public EngineHoistRenderer(BlockEntityRendererProvider.Context context) {
    }

    @Override
    public void render(EngineHoistBlockEntity hoist, float tickProgress, PoseStack pose, MultiBufferSource buffers,
                       int light, int overlay) {
        ItemStack engine = hoist.getEngine();
        if (engine.isEmpty()) {
            return;
        }
        Direction facing = hoist.getBlockState().getBlock() instanceof EngineHoistBlock
                ? hoist.getBlockState().getValue(EngineHoistBlock.FACING) : Direction.NORTH;
        pose.pushPose();
        pose.translate(0.5F, 0.0F, 0.5F);
        // The north-facing model's boom runs to -z; turn it the way this one points.
        pose.mulPose(Axis.YP.rotationDegrees(-facing.toYRot() + 180.0F));
        pose.translate(0.0F, (float) EngineHoistBlock.HOOK_HEIGHT - BELOW_HOOK, (float) -EngineHoistBlock.HOOK_OUT);
        pose.scale(ENGINE_SCALE, ENGINE_SCALE, ENGINE_SCALE);
        Minecraft.getInstance().getItemRenderer().renderStatic(engine, ItemDisplayContext.FIXED, light,
                OverlayTexture.NO_OVERLAY, pose, buffers, hoist.getLevel(), 0);
        pose.popPose();
    }

    /** The engine hangs over the next block, so the hoist is not culled by its own block alone. */
    @Override
    public AABB getRenderBoundingBox(EngineHoistBlockEntity hoist) {
        return new AABB(hoist.getBlockPos()).inflate(1.5);
    }
}
