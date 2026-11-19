package me.trossty.car.client.render;

import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.VertexFormat;
import net.minecraft.Util;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.resources.ResourceLocation;

import java.util.function.Function;

/**
 * Vehicle glass: blended like any see-through entity, but leaving the depth
 * buffer alone. Entities are drawn before the world's water, so glass that
 * wrote depth would hide every bit of water behind it; this way the water is
 * still drawn, and the glass's tint stays under it.
 */
final class VehicleRenderTypes extends RenderType {

    private static final Function<ResourceLocation, RenderType> GLASS = Util.memoize(texture -> create(
            "car_vehicle_glass", DefaultVertexFormat.NEW_ENTITY, VertexFormat.Mode.QUADS, 1536, true, true,
            CompositeState.builder()
                    .setShaderState(RENDERTYPE_ENTITY_TRANSLUCENT_SHADER)
                    .setTextureState(new TextureStateShard(texture, false, false))
                    .setTransparencyState(TRANSLUCENT_TRANSPARENCY)
                    .setCullState(NO_CULL)
                    .setLightmapState(LIGHTMAP)
                    .setOverlayState(OVERLAY)
                    .setWriteMaskState(COLOR_WRITE)
                    .createCompositeState(true)));

    private VehicleRenderTypes() {
        super("car_unused", DefaultVertexFormat.NEW_ENTITY, VertexFormat.Mode.QUADS, 0, false, false, () -> { }, () -> { });
    }

    static RenderType glass(ResourceLocation texture) {
        return GLASS.apply(texture);
    }
}
