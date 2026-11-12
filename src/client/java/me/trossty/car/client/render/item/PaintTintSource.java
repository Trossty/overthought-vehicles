package me.trossty.car.client.render.item;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import me.trossty.car.registry.ModComponents;
import me.trossty.car.vehicle.VehicleDefinition;
import net.minecraft.client.color.item.ItemTintSource;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.Nullable;

/**
 * The paint on a vehicle item ({@code "type": "car:paint"} in an item
 * definition's tints). The model draws its paint white; this colours it with
 * the dye the car was wearing when it was picked up, or the factory paint
 * ({@code default}) if it never was. {@code shade} is {@code paint},
 * {@code paint_dark} or {@code paint_light}, the same three tones the car
 * itself is drawn in.
 */
public record PaintTintSource(String shade, int defaultColor) implements ItemTintSource {

    public static final MapCodec<PaintTintSource> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
            Codec.STRING.optionalFieldOf("shade", "paint").forGetter(PaintTintSource::shade),
            Codec.INT.optionalFieldOf("default", 0xFFFFFF).forGetter(PaintTintSource::defaultColor)
    ).apply(instance, PaintTintSource::new));

    @Override
    public int calculate(ItemStack stack, @Nullable ClientLevel world, @Nullable LivingEntity user) {
        Integer sprayed = stack.get(ModComponents.CAR_PAINT);
        Integer index = stack.get(ModComponents.CAR_COLOR);
        int paint = sprayed != null ? sprayed
                : index == null ? this.defaultColor : DyeColor.byId(index).getTextureDiffuseColor();
        return 0xFF000000 | VehicleDefinition.resolve(this.shade, paint & 0xFFFFFF);
    }

    @Override
    public MapCodec<PaintTintSource> type() {
        return CODEC;
    }
}
