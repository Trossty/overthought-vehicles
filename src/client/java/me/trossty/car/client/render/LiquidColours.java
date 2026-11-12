package me.trossty.car.client.render;

import me.trossty.car.mixin.client.SpriteContentsAccessor;
import net.fabricmc.fabric.api.transfer.v1.client.fluid.FluidVariantRendering;
import net.fabricmc.fabric.api.transfer.v1.fluid.FluidVariant;
import net.minecraft.client.texture.NativeImage;
import net.minecraft.client.texture.Sprite;

import java.util.Map;
import java.util.WeakHashMap;

/**
 * One colour for a liquid, the one you would call it: its texture's average
 * times its tint. The tint alone is not enough — lava and most other mods'
 * liquids are tinted white and keep their colour in the texture.
 */
public final class LiquidColours {

    /** Keyed by sprite, so a resource reload, which makes new sprites, starts afresh. */
    private static final Map<Sprite, Integer> AVERAGES = new WeakHashMap<>();

    private LiquidColours() {
    }

    /** RGB, no alpha; {@code fallback} for nothing at all. */
    public static int of(FluidVariant liquid, int fallback) {
        if (liquid == null || liquid.isBlank()) {
            return fallback;
        }
        int tint = FluidVariantRendering.getColor(liquid);
        Sprite sprite = FluidVariantRendering.getSprite(liquid);
        int base = sprite == null ? 0xFFFFFF : average(sprite);
        return multiply(base, tint);
    }

    private static synchronized int average(Sprite sprite) {
        return AVERAGES.computeIfAbsent(sprite, LiquidColours::measure);
    }

    private static int measure(Sprite sprite) {
        NativeImage[] images = ((SpriteContentsAccessor) sprite.getContents()).car$getImages();
        if (images == null || images.length == 0 || images[0] == null) {
            return 0xFFFFFF;
        }
        NativeImage image = images[0];
        // Only the first frame of an animated strip: the rest are the same liquid.
        int width = sprite.getContents().getWidth();
        int height = Math.min(sprite.getContents().getHeight(), image.getHeight());
        long r = 0, g = 0, b = 0, n = 0;
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                int argb = image.getColorArgb(x, y);
                int alpha = argb >>> 24;
                if (alpha < 16) {
                    continue;
                }
                r += (argb >> 16) & 0xFF;
                g += (argb >> 8) & 0xFF;
                b += argb & 0xFF;
                n++;
            }
        }
        if (n == 0) {
            return 0xFFFFFF;
        }
        return (int) (r / n) << 16 | (int) (g / n) << 8 | (int) (b / n);
    }

    private static int multiply(int a, int b) {
        int r = ((a >> 16) & 0xFF) * ((b >> 16) & 0xFF) / 255;
        int g = ((a >> 8) & 0xFF) * ((b >> 8) & 0xFF) / 255;
        int bl = (a & 0xFF) * (b & 0xFF) / 255;
        return r << 16 | g << 8 | bl;
    }
}
