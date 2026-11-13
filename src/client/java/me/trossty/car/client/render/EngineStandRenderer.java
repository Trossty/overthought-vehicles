package me.trossty.car.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import me.trossty.car.block.EngineStandBlock;
import me.trossty.car.block.entity.EngineStandBlockEntity;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.Direction;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;

/** The engine bolted to a stand, hanging out in front of its mounting plate. Everything else is the block model. */
public class EngineStandRenderer implements BlockEntityRenderer<EngineStandBlockEntity> {

    /** Where the engine's middle sits, in the north-facing model's pixels, and how big it is drawn. */
    private static final float ENGINE_Y = 10.0F / 16;
    private static final float ENGINE_Z = 4.6F / 16;
    private static final float ENGINE_SCALE = 0.78F;

    public EngineStandRenderer(BlockEntityRendererProvider.Context context) {
    }

    @Override
    public void render(EngineStandBlockEntity stand, float tickProgress, PoseStack pose, MultiBufferSource buffers,
                       int light, int overlay) {
        ItemStack engine = stand.getEngine();
        if (engine.isEmpty()) {
            return;
        }
        Direction facing = stand.getBlockState().getBlock() instanceof EngineStandBlock
                ? stand.getBlockState().getValue(EngineStandBlock.FACING) : Direction.NORTH;
        pose.pushPose();
        pose.translate(0.5F, 0.0F, 0.5F);
        pose.mulPose(Axis.YP.rotationDegrees(-facing.toYRot() + 180.0F));
        pose.translate(0.0F, ENGINE_Y, ENGINE_Z - 0.5F);
        Minecraft client = Minecraft.getInstance();
        if (stand.isStripped()) {
            // Opened up: the insides laid out in a row on top of the block.
            for (int slot = 0; slot < 3; slot++) {
                ItemStack inside = stand.getInternals().get(slot);
                if (inside.isEmpty()) {
                    continue;
                }
                pose.pushPose();
                pose.translate((slot - 1) * 0.3F, 0.42F, 0.0F);
                pose.scale(0.32F, 0.32F, 0.32F);
                client.getItemRenderer().renderStatic(inside, ItemDisplayContext.FIXED, light,
                        OverlayTexture.NO_OVERLAY, pose, buffers, stand.getLevel(), slot + 1);
                pose.popPose();
            }
        }
        pose.scale(ENGINE_SCALE, ENGINE_SCALE, ENGINE_SCALE);
        client.getItemRenderer().renderStatic(engine, ItemDisplayContext.FIXED, light, OverlayTexture.NO_OVERLAY,
                pose, buffers, stand.getLevel(), 0);
        pose.popPose();
    }
}
