package me.trossty.car.client.render;

import me.trossty.car.fluid.Liquids;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.world.level.material.Fluid;
import net.neoforged.neoforge.client.extensions.common.IClientFluidTypeExtensions;
import org.jetbrains.annotations.Nullable;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import com.mojang.blaze3d.platform.NativeImage;
import java.util.Map;
import java.util.WeakHashMap;

/**
 * One colour for a liquid, the one you would call it: its texture's average
 * times its tint. The tint alone is not enough — lava and most other mods'
 * liquids are tinted white and keep their colour in the texture.
 */
public final class LiquidColours {

    /** Keyed by sprite, so a resource reload, which makes new sprites, starts afresh. */
    private static final Map<TextureAtlasSprite, Integer> AVERAGES = new WeakHashMap<>();

    private LiquidColours() {
    }

    /** RGB, no alpha; {@code fallback} for nothing at all. */
    public static int of(@Nullable Fluid liquid, int fallback) {
        if (liquid == null || Liquids.isBlank(liquid)) {
            return fallback;
        }
        TextureAtlasSprite sprite = sprite(liquid);
        int base = sprite == null ? 0xFFFFFF : average(sprite);
        return multiply(base, tint(liquid));
    }

    /** The still texture a liquid is drawn with, or null for nothing at all. */
    public static @Nullable TextureAtlasSprite sprite(Fluid liquid) {
        if (Liquids.isBlank(liquid)) {
            return null;
        }
        var texture = IClientFluidTypeExtensions.of(liquid).getStillTexture();
        return texture == null ? null
                : Minecraft.getInstance().getTextureAtlas(TextureAtlas.LOCATION_BLOCKS).apply(texture);
    }

    /** The tint over that texture, RGB. */
    public static int tint(Fluid liquid) {
        return IClientFluidTypeExtensions.of(liquid).getTintColor() & 0xFFFFFF;
    }

    private static synchronized int average(TextureAtlasSprite sprite) {
        return AVERAGES.computeIfAbsent(sprite, LiquidColours::measure);
    }

    private static int measure(TextureAtlasSprite sprite) {
        NativeImage[] images = sprite.contents().byMipLevel;
        if (images == null || images.length == 0 || images[0] == null) {
            return 0xFFFFFF;
        }
        NativeImage image = images[0];
        // Only the first frame of an animated strip: the rest are the same liquid.
        int width = sprite.contents().width();
        int height = Math.min(sprite.contents().height(), image.getHeight());
        long r = 0, g = 0, b = 0, n = 0;
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                int argb = net.minecraft.util.FastColor.ABGR32.fromArgb32(image.getPixelRGBA(x, y)); // the same swap both ways
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
