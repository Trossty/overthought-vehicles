package me.trossty.car.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import me.trossty.car.block.VehicleCrateBlock;
import me.trossty.car.block.entity.VehicleCrateBlockEntity;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.util.Mth;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;

/**
 * Draws a crate's sides and lid over the pallet the chunk draws. Shut, the
 * lid lifts a little at the front for every turn of the wrench. Opening, the
 * lid swings up on its back edge and falls behind, the sides drop outward one
 * after another, front first, and the vehicle inside is seen growing to its
 * full size before it takes the crate's place.
 *
 * <p>Everything is worked out in the crate's own frame, where north is the
 * back of the vehicle and the lid's hinge.
 */
public class VehicleCrateRenderer implements BlockEntityRenderer<VehicleCrateBlockEntity> {

    private static final float PX = 1.0F / 16.0F;
    /** How far each turn of the wrench lifts the lid's front edge. */
    private static final float PRY_DEGREES = 7.0F;

    public VehicleCrateRenderer(BlockEntityRendererProvider.Context context) {
    }

    @Override
    public void render(VehicleCrateBlockEntity crate, float tickProgress, PoseStack matrices,
                       MultiBufferSource buffers, int light, int overlay) {
        BlockState state = crate.getBlockState();
        if (!(state.getBlock() instanceof VehicleCrateBlock)) {
            return;
        }
        BlockState wall = state.setValue(VehicleCrateBlock.PIECE, VehicleCrateBlock.WALL);
        BlockState lid = state.setValue(VehicleCrateBlock.PIECE, VehicleCrateBlock.LID);
        // How far through opening it is, 0 to 1, or below 0 while shut.
        float open = crate.getOpening() < 0 ? -1.0F
                : Math.min(1.0F, (crate.getOpening() + tickProgress) / VehicleCrateBlockEntity.OPEN_TICKS);

        matrices.pushPose();
        matrices.translate(0.5F, 0.0F, 0.5F);
        matrices.mulPose(Axis.YP.rotationDegrees(-state.getValue(VehicleCrateBlock.FACING).toYRot()));
        matrices.translate(-0.5F, 0.0F, -0.5F);

        if (open >= 0.0F) {
            this.drawVehicle(crate.getVehicle(), open, matrices, buffers, light, crate);
        }

        // The sides: the north one turned a quarter at a time. Front first,
        // then the two sides together, the back last.
        for (int quarter = 0; quarter < 4; quarter++) {
            float start = switch (quarter) {
                case 2 -> 0.22F;
                case 0 -> 0.40F;
                default -> 0.30F;
            };
            float fall = open < 0.0F ? 0.0F : ease(Mth.clamp((open - start) / 0.35F, 0.0F, 1.0F));
            matrices.pushPose();
            matrices.translate(0.5F, 0.0F, 0.5F);
            matrices.mulPose(Axis.YP.rotationDegrees(-90.0F * quarter));
            matrices.translate(-0.5F, 0.0F, -0.5F);
            if (fall > 0.0F) {
                // Over on its bottom outside edge, and down onto the ground.
                matrices.translate(0.0F, 2.0F * PX - 1.5F * PX * fall, 0.0F);
                matrices.mulPose(Axis.XP.rotationDegrees(-90.0F * fall));
                matrices.translate(0.0F, -2.0F * PX, 0.0F);
            }
            draw(wall, matrices, buffers, light);
            matrices.popPose();
        }

        // The lid, hinged on its back top edge.
        float angle;
        float drop = 0.0F;
        if (open < 0.0F) {
            angle = crate.getPries() * PRY_DEGREES;
        } else {
            float pried = VehicleCrateBlockEntity.PRIES * PRY_DEGREES;
            float up = Mth.clamp(open / 0.3F, 0.0F, 1.0F);
            float over = ease(Mth.clamp((open - 0.45F) / 0.45F, 0.0F, 1.0F));
            angle = Mth.lerp(1.0F - (1.0F - up) * (1.0F - up), pried, 100.0F) + 80.0F * over;
            drop = 13.5F * PX * over;
        }
        matrices.pushPose();
        matrices.translate(0.0F, 1.0F - drop, 0.0F);
        matrices.mulPose(Axis.XP.rotationDegrees(-angle));
        matrices.translate(0.0F, -1.0F, 0.0F);
        draw(lid, matrices, buffers, light);
        matrices.popPose();

        matrices.popPose();
    }

    /** The vehicle inside, seen once the lid is off, growing as the sides come down. */
    private void drawVehicle(ItemStack vehicle, float open, PoseStack matrices, MultiBufferSource buffers,
                             int light, VehicleCrateBlockEntity crate) {
        if (vehicle.isEmpty()) {
            return;
        }
        float grow = 1.0F - (1.0F - open) * (1.0F - open);
        float scale = Mth.lerp(grow, 0.9F, 2.2F);
        matrices.pushPose();
        matrices.translate(0.5F, 2.0F * PX + 0.5F * scale * 0.7F, 0.5F);
        matrices.scale(scale, scale, scale);
        Minecraft.getInstance().getItemRenderer().renderStatic(vehicle, ItemDisplayContext.FIXED, light,
                OverlayTexture.NO_OVERLAY, matrices, buffers, crate.getLevel(), 0);
        matrices.popPose();
    }

    private static void draw(BlockState piece, PoseStack matrices, MultiBufferSource buffers, int light) {
        Minecraft.getInstance().getBlockRenderer().renderSingleBlock(piece, matrices, buffers, light,
                OverlayTexture.NO_OVERLAY);
    }

    /** Falling: slow to start, quick at the end. */
    private static float ease(float t) {
        return t * t;
    }

    @Override
    public AABB getRenderBoundingBox(VehicleCrateBlockEntity crate) {
        // The sides fall a block out, and the vehicle grows past the crate.
        return new AABB(crate.getBlockPos()).inflate(1.5);
    }
}
