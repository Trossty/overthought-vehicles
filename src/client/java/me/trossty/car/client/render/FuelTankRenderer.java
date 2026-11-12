package me.trossty.car.client.render;

import me.trossty.car.block.FuelTankBlock;
import me.trossty.car.block.entity.FuelTankBlockEntity;
import me.trossty.car.entity.TankerEntity;
import net.fabricmc.fabric.api.transfer.v1.client.fluid.FluidVariantRendering;
import net.fabricmc.fabric.api.transfer.v1.fluid.FluidVariant;
import net.minecraft.block.BlockState;
import net.minecraft.client.render.LightmapTextureManager;
import net.minecraft.client.render.OverlayTexture;
import net.minecraft.client.render.RenderLayers;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.block.entity.BlockEntityRenderer;
import net.minecraft.client.render.block.entity.BlockEntityRendererFactory;
import net.minecraft.client.render.block.entity.state.BlockEntityRenderState;
import net.minecraft.client.render.command.ModelCommandRenderer;
import net.minecraft.client.render.command.OrderedRenderCommandQueue;
import net.minecraft.client.render.state.CameraRenderState;
import net.minecraft.client.texture.Sprite;
import net.minecraft.client.texture.SpriteAtlasTexture;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.math.Vec3d;
import org.jspecify.annotations.Nullable;

/**
 * The liquid behind a tank's glass. Each cell draws the part of it that is in
 * that cell, and leaves out the faces it shares with another cell, so a
 * stack or a floor of them shows one body of liquid with one surface.
 */
public class FuelTankRenderer implements BlockEntityRenderer<FuelTankBlockEntity, FuelTankRenderer.State> {

    /** The glass sits half a pixel in, the lid a pixel and a half down. */
    private static final float WALL = 0.6F / 16.0F;
    private static final float FLOOR = 1.5F / 16.0F;
    private static final float CEILING = 14.5F / 16.0F;

    public static class State extends BlockEntityRenderState {
        FluidVariant liquid = FluidVariant.blank();
        float fill;
        boolean north;
        boolean south;
        boolean east;
        boolean west;
        boolean up;
        boolean down;
    }

    public FuelTankRenderer(BlockEntityRendererFactory.Context context) {
    }

    @Override
    public State createRenderState() {
        return new State();
    }

    @Override
    public void updateRenderState(FuelTankBlockEntity tank, State state, float tickProgress, Vec3d cameraPos,
                                  ModelCommandRenderer.@Nullable CrumblingOverlayCommand crumbling) {
        BlockEntityRenderer.super.updateRenderState(tank, state, tickProgress, cameraPos, crumbling);
        state.liquid = tank.getFluid();
        state.fill = tank.getFill();
        BlockState block = state.blockState;
        if (block.getBlock() instanceof FuelTankBlock) {
            state.north = block.get(FuelTankBlock.NORTH);
            state.south = block.get(FuelTankBlock.SOUTH);
            state.east = block.get(FuelTankBlock.EAST);
            state.west = block.get(FuelTankBlock.WEST);
            state.up = block.get(FuelTankBlock.UP);
            state.down = block.get(FuelTankBlock.DOWN);
        }
    }

    @Override
    public void render(State state, MatrixStack matrices, OrderedRenderCommandQueue queue, CameraRenderState camera) {
        if (state.liquid.isBlank() || state.fill <= 0.0F) {
            return;
        }
        Sprite sprite = FluidVariantRendering.getSprite(state.liquid);
        if (sprite == null) {
            return;
        }
        int colour = 0xFF000000 | FluidVariantRendering.getColor(state.liquid);
        int light = TankerEntity.glows(state.liquid) ? LightmapTextureManager.MAX_LIGHT_COORDINATE
                : state.lightmapCoordinates;

        float x1 = state.west ? 0.0F : WALL;
        float x2 = state.east ? 1.0F : 1.0F - WALL;
        float z1 = state.north ? 0.0F : WALL;
        float z2 = state.south ? 1.0F : 1.0F - WALL;
        float y1 = state.down ? 0.0F : FLOOR;
        float top = state.up ? 1.0F : CEILING;
        float y2 = y1 + (top - y1) * Math.min(1.0F, state.fill);
        // The surface shows unless the cell is full to a cell above it.
        boolean surface = !(state.up && state.fill >= 0.999F);

        queue.submitCustom(matrices, RenderLayers.entityCutoutNoCull(SpriteAtlasTexture.BLOCK_ATLAS_TEXTURE),
                (entry, consumer) -> {
                    float u1 = sprite.getMinU();
                    float u2 = sprite.getMaxU();
                    float v1 = sprite.getMinV();
                    float vSide = sprite.getFrameV(y2 - y1);
                    float vTop = sprite.getMaxV();
                    if (surface) {
                        quad(consumer, entry, colour, light, 0, 1, 0,
                                x1, y2, z1, u1, v1, x1, y2, z2, u1, vTop, x2, y2, z2, u2, vTop, x2, y2, z1, u2, v1);
                    }
                    if (!state.down) {
                        quad(consumer, entry, colour, light, 0, -1, 0,
                                x1, y1, z1, u1, v1, x2, y1, z1, u2, v1, x2, y1, z2, u2, vTop, x1, y1, z2, u1, vTop);
                    }
                    if (!state.north) {
                        quad(consumer, entry, colour, light, 0, 0, -1,
                                x2, y2, z1, u1, v1, x2, y1, z1, u1, vSide, x1, y1, z1, u2, vSide, x1, y2, z1, u2, v1);
                    }
                    if (!state.south) {
                        quad(consumer, entry, colour, light, 0, 0, 1,
                                x1, y2, z2, u1, v1, x1, y1, z2, u1, vSide, x2, y1, z2, u2, vSide, x2, y2, z2, u2, v1);
                    }
                    if (!state.west) {
                        quad(consumer, entry, colour, light, -1, 0, 0,
                                x1, y2, z1, u1, v1, x1, y1, z1, u1, vSide, x1, y1, z2, u2, vSide, x1, y2, z2, u2, v1);
                    }
                    if (!state.east) {
                        quad(consumer, entry, colour, light, 1, 0, 0,
                                x2, y2, z2, u1, v1, x2, y1, z2, u1, vSide, x2, y1, z1, u2, vSide, x2, y2, z1, u2, v1);
                    }
                });
    }

    private static void quad(VertexConsumer consumer, MatrixStack.Entry entry, int colour, int light,
                             float nx, float ny, float nz,
                             float ax, float ay, float az, float au, float av,
                             float bx, float by, float bz, float bu, float bv,
                             float cx, float cy, float cz, float cu, float cv,
                             float dx, float dy, float dz, float du, float dv) {
        vertex(consumer, entry, colour, light, nx, ny, nz, ax, ay, az, au, av);
        vertex(consumer, entry, colour, light, nx, ny, nz, bx, by, bz, bu, bv);
        vertex(consumer, entry, colour, light, nx, ny, nz, cx, cy, cz, cu, cv);
        vertex(consumer, entry, colour, light, nx, ny, nz, dx, dy, dz, du, dv);
    }

    private static void vertex(VertexConsumer consumer, MatrixStack.Entry entry, int colour, int light,
                               float nx, float ny, float nz, float x, float y, float z, float u, float v) {
        consumer.vertex(entry, x, y, z)
                .color(colour)
                .texture(u, v)
                .overlay(OverlayTexture.DEFAULT_UV)
                .light(light)
                .normal(entry, nx, ny, nz);
    }
}
