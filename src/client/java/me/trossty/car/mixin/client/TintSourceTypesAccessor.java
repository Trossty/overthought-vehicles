package me.trossty.car.mixin.client;

import com.mojang.serialization.MapCodec;
import net.minecraft.client.color.item.ItemTintSource;
import net.minecraft.client.color.item.ItemTintSources;
import net.minecraft.resources.Identifier;
import net.minecraft.util.ExtraCodecs;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

/** Vanilla keeps its list of item tint types private; this is how car:paint joins it. */
@Mixin(ItemTintSources.class)
public interface TintSourceTypesAccessor {

    @Accessor("ID_MAPPER")
    static ExtraCodecs.LateBoundIdMapper<Identifier, MapCodec<? extends ItemTintSource>> car$getIdMapper() {
        throw new AssertionError();
    }
}
