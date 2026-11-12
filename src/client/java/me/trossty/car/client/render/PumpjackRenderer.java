package me.trossty.car.client.render;

import me.trossty.car.block.MultiblockBlock;
import me.trossty.car.block.PumpjackBlock;
import me.trossty.car.block.entity.PumpjackBlockEntity;
import net.minecraft.block.BlockState;
import net.minecraft.client.render.OverlayTexture;
import net.minecraft.client.render.WorldRenderer;
import net.minecraft.client.render.block.entity.BlockEntityRenderer;
import net.minecraft.client.render.block.entity.BlockEntityRendererFactory;
import net.minecraft.client.render.block.entity.state.BlockEntityRenderState;
import net.minecraft.client.render.command.ModelCommandRenderer;
import net.minecraft.client.render.command.OrderedRenderCommandQueue;
import net.minecraft.client.render.state.CameraRenderState;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;
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

    public PumpjackRenderer(BlockEntityRendererFactory.Context context) {
    }

    @Override
    public State createRenderState() {
        return new State();
    }

    @Override
    public void updateRenderState(PumpjackBlockEntity jack, State state, float tickProgress, Vec3d cameraPos,
                                  ModelCommandRenderer.@Nullable CrumblingOverlayCommand crumbling) {
        BlockEntityRenderer.super.updateRenderState(jack, state, tickProgress, cameraPos, crumbling);
        BlockState block = state.blockState;
        if (!(block.getBlock() instanceof PumpjackBlock)) {
            state.beam = null;
            return;
        }
        state.facing = block.get(MultiblockBlock.FACING);
        state.beam = block.with(PumpjackBlock.PART, PumpjackBlock.BEAM);
        state.crank = block.with(PumpjackBlock.PART, PumpjackBlock.CRANK);
        boolean running = block.get(MultiblockBlock.LIT);
        float time = jack.getWorld() == null ? 0 : jack.getWorld().getTime() + tickProgress;
        state.phase = running ? time * SPEED : 0;
        BlockPos bearing = MultiblockBlock.cellPos(jack.getPos(), state.facing, PumpjackBlock.CELLS[6]);
        state.beamLight = jack.getWorld() == null ? state.lightmapCoordinates
                : WorldRenderer.getLightmapCoordinates(jack.getWorld(), bearing);
    }

    @Override
    public void render(State state, MatrixStack matrices, OrderedRenderCommandQueue queue, CameraRenderState camera) {
        if (state.beam == null || state.crank == null) {
            return;
        }
        Direction right = state.facing.rotateYClockwise();
        Direction back = state.facing.getOpposite();

        // The beam rocks about the bearing on top of the Samson post: cell {0, 2, 1}.
        float swing = (float) Math.sin(state.phase) * BEAM_SWING;
        drawTurned(matrices, queue, state.beam, state.beamLight, right,
                back.getOffsetX(), 2, back.getOffsetZ(), 0.5F, swing);

        // The crank turns all the way round on its shaft in cell {0, 1, 2}.
        drawTurned(matrices, queue, state.crank, state.lightmapCoordinates, right,
                back.getOffsetX() * 2, 1, back.getOffsetZ() * 2, 0.125F, -state.phase);
    }

    /** Draws a piece's model at a cell, turned about a level axis through the cell's middle. */
    private static void drawTurned(MatrixStack matrices, OrderedRenderCommandQueue queue, BlockState piece, int light,
                                   Direction axis, int dx, int dy, int dz, float pivotY, float angle) {
        matrices.push();
        matrices.translate(dx + 0.5F, dy + pivotY, dz + 0.5F);
        matrices.multiply(new Quaternionf().rotationAxis(angle, axis.getOffsetX(), 0, axis.getOffsetZ()));
        matrices.translate(-0.5F, -pivotY, -0.5F);
        queue.submitBlock(matrices, piece, light, OverlayTexture.DEFAULT_UV, 0);
        matrices.pop();
    }

    @Override
    public boolean rendersOutsideBoundingBox() {
        return true;
    }

    @Override
    public int getRenderDistance() {
        return 96;
    }
}
