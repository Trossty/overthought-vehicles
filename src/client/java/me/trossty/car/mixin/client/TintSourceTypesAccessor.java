package me.trossty.car.mixin.client;

import com.mojang.serialization.MapCodec;
import net.minecraft.client.render.item.tint.TintSource;
import net.minecraft.client.render.item.tint.TintSourceTypes;
import net.minecraft.util.Identifier;
import net.minecraft.util.dynamic.Codecs;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

/** Vanilla keeps its list of item tint types private; this is how car:paint joins it. */
@Mixin(TintSourceTypes.class)
public interface TintSourceTypesAccessor {

    @Accessor("ID_MAPPER")
    static Codecs.IdMapper<Identifier, MapCodec<? extends TintSource>> car$getIdMapper() {
        throw new AssertionError();
    }
}
