package me.trossty.car.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import me.trossty.car.block.MultiblockBlock;
import me.trossty.car.block.PumpjackBlock;
import me.trossty.car.block.entity.PumpjackBlockEntity;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.blockentity.state.BlockEntityRenderState;
import net.minecraft.client.renderer.state.CameraRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import org.joml.Quaternionf;
import org.jspecify.annotations.Nullable;

/**
 * Draws the two moving pieces of a pumpjack: the walking beam rocking on its
 * bearing, horse head up and down, and the crank turning its counterweights
 * round behind it. The rest of the jack is ordinary block models.
 */
public class PumpjackRenderer implements BlockEntityRenderer<PumpjackBlockEntity, PumpjackRenderer.State> {

    /** One stroke every four seconds, about what a real one does. */
    private static final float SPEED = (float) (Math.PI * 2 / 80);
    private static final float BEAM_SWING = (float) Math.toRadians(13);

    public static class State extends BlockEntityRenderState {
        float phase;
        @Nullable BlockState beam;
        @Nullable BlockState crank;
        Direction facing = Direction.NORTH;
        int beamLight;
    }

    public PumpjackRenderer(BlockEntityRendererProvider.Context context) {
    }

    @Override
    public State createRenderState() {
        return new State();
    }

    @Override
    public void extractRenderState(PumpjackBlockEntity jack, State state, float tickProgress, Vec3 cameraPos,
                                  net.minecraft.client.renderer.feature.ModelFeatureRenderer.@Nullable CrumblingOverlay crumbling) {
        BlockEntityRenderer.super.extractRenderState(jack, state, tickProgress, cameraPos, crumbling);
        BlockState block = state.blockState;
        if (!(block.getBlock() instanceof PumpjackBlock)) {
            state.beam = null;
            return;
        }
        state.facing = block.getValue(MultiblockBlock.FACING);
        state.beam = block.setValue(PumpjackBlock.PART, PumpjackBlock.BEAM);
        state.crank = block.setValue(PumpjackBlock.PART, PumpjackBlock.CRANK);
        boolean running = block.getValue(MultiblockBlock.LIT);
        float time = jack.getLevel() == null ? 0 : jack.getLevel().getGameTime() + tickProgress;
        state.phase = running ? time * SPEED : 0;
        BlockPos bearing = MultiblockBlock.cellPos(jack.getBlockPos(), state.facing, PumpjackBlock.CELLS[6]);
        state.beamLight = jack.getLevel() == null ? state.lightCoords
                : LevelRenderer.getLightColor(jack.getLevel(), bearing);
    }

    @Override
    public void submit(State state, PoseStack matrices, SubmitNodeCollector queue, CameraRenderState camera) {
        if (state.beam == null || state.crank == null) {
            return;
        }
        Direction right = state.facing.getClockWise();
        Direction back = state.facing.getOpposite();

        // The beam rocks about the bearing on top of the Samson post: cell {0, 2, 1}.
        float swing = (float) Math.sin(state.phase) * BEAM_SWING;
        drawTurned(matrices, queue, state.beam, state.beamLight, right,
                back.getStepX(), 2, back.getStepZ(), 0.5F, swing);

        // The crank turns all the way round on its shaft in cell {0, 1, 2}.
        drawTurned(matrices, queue, state.crank, state.lightCoords, right,
                back.getStepX() * 2, 1, back.getStepZ() * 2, 0.125F, -state.phase);
    }

    /** Draws a piece's model at a cell, turned about a level axis through the cell's middle. */
    private static void drawTurned(PoseStack matrices, SubmitNodeCollector queue, BlockState piece, int light,
                                   Direction axis, int dx, int dy, int dz, float pivotY, float angle) {
        matrices.pushPose();
        matrices.translate(dx + 0.5F, dy + pivotY, dz + 0.5F);
        matrices.mulPose(new Quaternionf().rotationAxis(angle, axis.getStepX(), 0, axis.getStepZ()));
        matrices.translate(-0.5F, -pivotY, -0.5F);
        queue.submitBlock(matrices, piece, light, OverlayTexture.NO_OVERLAY, 0);
        matrices.popPose();
    }

    @Override
    public boolean shouldRenderOffScreen() {
        return true;
    }

    @Override
    public int getViewDistance() {
        return 96;
    }
}
