package me.trossty.car.mixin.client;

import net.minecraft.client.texture.NativeImage;
import net.minecraft.client.texture.SpriteContents;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

/** A sprite's pixels, so a liquid's colour can be read off its texture: see LiquidColours. */
@Mixin(SpriteContents.class)
public interface SpriteContentsAccessor {

    @Accessor("mipmapLevelsImages")
    NativeImage[] car$getImages();
}
