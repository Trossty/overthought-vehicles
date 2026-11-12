package me.trossty.car.mixin.client;

import com.mojang.blaze3d.platform.NativeImage;
import net.minecraft.client.renderer.texture.SpriteContents;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

/** A sprite's pixels, so a liquid's colour can be read off its texture: see LiquidColours. */
@Mixin(SpriteContents.class)
public interface SpriteContentsAccessor {

    @Accessor("byMipLevel")
    NativeImage[] car$getImages();
}
