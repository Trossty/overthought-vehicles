package me.trossty.car.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import me.trossty.car.block.FuelTankBlock;
import me.trossty.car.block.entity.FuelTankBlockEntity;
import me.trossty.car.entity.TankerEntity;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

/**
 * The liquid behind a tank's glass. Each cell draws the part of it that is in
 * that cell, and leaves out the faces it shares with another cell, so a
 * stack or a floor of them shows one body of liquid with one surface.
 */
public class FuelTankRenderer implements BlockEntityRenderer<FuelTankBlockEntity> {

    /** The glass sits half a pixel in, the lid a pixel and a half down. */
    private static final float WALL = 0.6F / 16.0F;
    private static final float FLOOR = 1.5F / 16.0F;
    private static final float CEILING = 14.5F / 16.0F;

    public static class State extends BlockEntityRenderState {
        net.minecraft.world.level.material.Fluid liquid = net.minecraft.world.level.material.Fluids.EMPTY;
        float fill;
        boolean north;
        boolean south;
        boolean east;
        boolean west;
        boolean up;
        boolean down;
    }

    public FuelTankRenderer(BlockEntityRendererProvider.Context context) {
    }

    public State createRenderState() {
        return new State();
    }

    private void extractRenderState(FuelTankBlockEntity tank, State state, float tickProgress) {
        state.liquid = tank.getFluid();
        state.fill = tank.getFill();
        BlockState block = state.blockState;
        if (block.getBlock() instanceof FuelTankBlock) {
            state.north = block.getValue(FuelTankBlock.NORTH);
            state.south = block.getValue(FuelTankBlock.SOUTH);
            state.east = block.getValue(FuelTankBlock.EAST);
            state.west = block.getValue(FuelTankBlock.WEST);
            state.up = block.getValue(FuelTankBlock.UP);
            state.down = block.getValue(FuelTankBlock.DOWN);
        }
    }

    @Override
    public void render(FuelTankBlockEntity blockEntity, float tickProgress, PoseStack matrices, MultiBufferSource buffers,
                       int light, int overlay) {
        State state = this.createRenderState();
        state.blockState = blockEntity.getBlockState();
        state.blockPos = blockEntity.getBlockPos();
        state.lightCoords = light;
        this.extractRenderState(blockEntity, state, tickProgress);
        this.submit(state, matrices, buffers);
    }

    private void submit(State state, PoseStack matrices, MultiBufferSource queue) {
        if (me.trossty.car.fluid.Liquids.isBlank(state.liquid) || state.fill <= 0.0F) {
            return;
        }
        TextureAtlasSprite sprite = LiquidColours.sprite(state.liquid);
        if (sprite == null) {
            return;
        }
        int colour = 0xFF000000 | LiquidColours.tint(state.liquid);
        int light = TankerEntity.glows(state.liquid) ? LightTexture.FULL_BRIGHT
                : state.lightCoords;

        float x1 = state.west ? 0.0F : WALL;
        float x2 = state.east ? 1.0F : 1.0F - WALL;
        float z1 = state.north ? 0.0F : WALL;
        float z2 = state.south ? 1.0F : 1.0F - WALL;
        float y1 = state.down ? 0.0F : FLOOR;
        float top = state.up ? 1.0F : CEILING;
        float y2 = y1 + (top - y1) * Math.min(1.0F, state.fill);
        // The surface shows unless the cell is full to a cell above it.
        boolean surface = !(state.up && state.fill >= 0.999F);

        {
                PoseStack.Pose entry = matrices.last();
                com.mojang.blaze3d.vertex.VertexConsumer consumer = queue.getBuffer(RenderType.entityCutoutNoCull(TextureAtlas.LOCATION_BLOCKS));
                    float u1 = sprite.getU0();
                    float u2 = sprite.getU1();
                    float v1 = sprite.getV0();
                    float vSide = sprite.getV(y2 - y1);
                    float vTop = sprite.getV1();
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
                }
    }

    private static void quad(VertexConsumer consumer, PoseStack.Pose entry, int colour, int light,
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

    private static void vertex(VertexConsumer consumer, PoseStack.Pose entry, int colour, int light,
                               float nx, float ny, float nz, float x, float y, float z, float u, float v) {
        consumer.addVertex(entry, x, y, z)
                .setColor(colour)
                .setUv(u, v)
                .setOverlay(OverlayTexture.NO_OVERLAY)
                .setLight(light)
                .setNormal(entry, nx, ny, nz);
    }
}
