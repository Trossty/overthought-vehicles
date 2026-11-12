package me.trossty.car.client.render;

import me.trossty.car.entity.CarEntity;
import me.trossty.car.entity.TankerEntity;
import me.trossty.car.vehicle.VehicleDefinition;
import net.fabricmc.fabric.api.transfer.v1.client.fluid.FluidVariantRendering;
import net.fabricmc.fabric.api.transfer.v1.fluid.FluidVariant;
import net.minecraft.client.render.LightmapTextureManager;
import net.minecraft.client.render.OverlayTexture;
import net.minecraft.client.render.RenderLayers;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.command.OrderedRenderCommandQueue;
import net.minecraft.client.render.entity.EntityRendererFactory;
import net.minecraft.client.texture.Sprite;
import net.minecraft.client.texture.SpriteAtlasTexture;
import net.minecraft.client.util.math.MatrixStack;

/**
 * The tanker is a car with a barrel on it, and draws as one, with one thing
 * more: whatever it is carrying, in the barrel's glass section, filled to the
 * level it is at. The liquid is the fluid's own still texture and tint, so
 * water is water, lava glows, and another mod's fluid looks as that mod draws it.
 *
 * <p>Where it goes is the definition's business: every shape box coloured
 * {@code "cargo"} is a piece of the barrel's inside. The level is shared
 * across all of them, as a liquid's would be.
 */
public class TankerEntityRenderer extends CarEntityRenderer {

    public TankerEntityRenderer(EntityRendererFactory.Context context) {
        super(context, ModEntityModelLayers.FUEL_TANKER);
    }

    @Override
    public CarRenderState createRenderState() {
        return new TankerRenderState();
    }

    @Override
    public void updateRenderState(CarEntity car, CarRenderState state, float tickProgress) {
        super.updateRenderState(car, state, tickProgress);
        if (car instanceof TankerEntity tanker && state instanceof TankerRenderState load) {
            load.liquid = tanker.getLiquid();
            load.fill = tanker.getCargo() / (float) tanker.getCargoCapacity();
        }
    }

    @Override
    protected void submitExtras(CarRenderState state, MatrixStack matrices, OrderedRenderCommandQueue queue) {
        if (!(state instanceof TankerRenderState load) || load.liquid.isBlank() || load.fill <= 0.0F
                || state.vehicle == null) {
            return;
        }
        float bottom = Float.MAX_VALUE;
        float top = -Float.MAX_VALUE;
        java.util.List<VehicleDefinition.Box> inside = new java.util.ArrayList<>();
        for (VehicleDefinition.Box box : state.vehicle.shape()) {
            if (DefinedVehicleModel.isCargo(box.color())) {
                inside.add(box);
                bottom = Math.min(bottom, Math.min(box.from().y(), box.to().y()));
                top = Math.max(top, Math.max(box.from().y(), box.to().y()));
            }
        }
        if (inside.isEmpty()) {
            return;
        }
        float level = bottom + (top - bottom) * Math.min(1.0F, load.fill);
        // The surface is drawn once, by the widest piece it crosses; the
        // others' would lie in the same plane and fight it.
        VehicleDefinition.Box surface = null;
        for (VehicleDefinition.Box box : inside) {
            float low = Math.min(box.from().y(), box.to().y());
            float high = Math.max(box.from().y(), box.to().y());
            if (low < level && level <= high + 0.001F
                    && (surface == null || width(box) > width(surface))) {
                surface = box;
            }
        }

        Sprite sprite = FluidVariantRendering.getSprite(load.liquid);
        if (sprite == null) {
            return;
        }
        int colour = 0xFF000000 | FluidVariantRendering.getColor(load.liquid);
        int light = TankerEntity.glows(load.liquid) ? LightmapTextureManager.MAX_LIGHT_COORDINATE : state.light;
        VehicleDefinition.Box drawnSurface = surface;
        float drawnLevel = level;

        matrices.push();
        DefinedVehicleModel.applyBody(matrices, state);
        queue.submitCustom(matrices, RenderLayers.entityCutoutNoCull(SpriteAtlasTexture.BLOCK_ATLAS_TEXTURE),
                (entry, consumer) -> {
                    int order = 0;
                    for (VehicleDefinition.Box box : inside) {
                        float low = Math.min(box.from().y(), box.to().y());
                        float high = Math.min(Math.max(box.from().y(), box.to().y()), drawnLevel);
                        if (high > low) {
                            // Each piece's ends sit a hair further in than the last's, so
                            // where they overlap one face shows rather than two flickering.
                            float inset = 0.02F * order++;
                            liquid(consumer, entry, sprite, colour, light, box, low, high, inset,
                                    box == drawnSurface);
                        }
                    }
                });
        matrices.pop();
    }

    private static float width(VehicleDefinition.Box box) {
        return Math.abs(box.to().x() - box.from().x());
    }

    /** One piece of the load: a box in the definition's pixels, cut off at the level. */
    private static void liquid(VertexConsumer consumer, MatrixStack.Entry entry, Sprite sprite, int colour,
                               int light, VehicleDefinition.Box box, float low, float high, float inset,
                               boolean surface) {
        float x1 = Math.min(box.from().x(), box.to().x());
        float x2 = Math.max(box.from().x(), box.to().x());
        float z1 = Math.min(box.from().z(), box.to().z()) + inset;
        float z2 = Math.max(box.from().z(), box.to().z()) - inset;
        // The definition's pixels to the model's: y and z turned over, sixteen to a block.
        float ax = x1 / 16.0F;
        float bx = x2 / 16.0F;
        float ay = -low / 16.0F;
        float by = -high / 16.0F;
        float az = -z1 / 16.0F;
        float bz = -z2 / 16.0F;

        float u1 = sprite.getMinU();
        float v1 = sprite.getMinV();
        float uSide = sprite.getFrameU(Math.min(1.0F, (z2 - z1) / 16.0F));
        float uEnd = sprite.getFrameU(Math.min(1.0F, (x2 - x1) / 16.0F));
        float vUp = sprite.getFrameV(Math.min(1.0F, (high - low) / 16.0F));
        float vTop = sprite.getFrameV(Math.min(1.0F, (z2 - z1) / 16.0F));

        // Model y points down here, so "up" is minus y.
        if (surface) {
            quad(consumer, entry, colour, light, 0, -1, 0,
                    ax, by, az, u1, v1, bx, by, az, uEnd, v1, bx, by, bz, uEnd, vTop, ax, by, bz, u1, vTop);
        }
        quad(consumer, entry, colour, light, 0, 1, 0,
                ax, ay, az, u1, v1, ax, ay, bz, u1, vTop, bx, ay, bz, uEnd, vTop, bx, ay, az, uEnd, v1);
        quad(consumer, entry, colour, light, 1, 0, 0,
                bx, by, az, u1, v1, bx, ay, az, u1, vUp, bx, ay, bz, uSide, vUp, bx, by, bz, uSide, v1);
        quad(consumer, entry, colour, light, -1, 0, 0,
                ax, by, bz, u1, v1, ax, ay, bz, u1, vUp, ax, ay, az, uSide, vUp, ax, by, az, uSide, v1);
        quad(consumer, entry, colour, light, 0, 0, -1,
                ax, by, az, u1, v1, ax, ay, az, u1, vUp, bx, ay, az, uEnd, vUp, bx, by, az, uEnd, v1);
        quad(consumer, entry, colour, light, 0, 0, 1,
                bx, by, bz, u1, v1, bx, ay, bz, u1, vUp, ax, ay, bz, uEnd, vUp, ax, by, bz, uEnd, v1);
    }

    private static void quad(VertexConsumer consumer, MatrixStack.Entry entry, int colour, int light,
                             float nx, float ny, float nz,
                             float x1, float y1, float z1, float u1, float v1,
                             float x2, float y2, float z2, float u2, float v2,
                             float x3, float y3, float z3, float u3, float v3,
                             float x4, float y4, float z4, float u4, float v4) {
        vertex(consumer, entry, colour, light, nx, ny, nz, x1, y1, z1, u1, v1);
        vertex(consumer, entry, colour, light, nx, ny, nz, x2, y2, z2, u2, v2);
        vertex(consumer, entry, colour, light, nx, ny, nz, x3, y3, z3, u3, v3);
        vertex(consumer, entry, colour, light, nx, ny, nz, x4, y4, z4, u4, v4);
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

    /** A car's render state, with what is in the barrel. */
    public static class TankerRenderState extends CarRenderState {
        public FluidVariant liquid = FluidVariant.blank();
        public float fill;
    }
}
