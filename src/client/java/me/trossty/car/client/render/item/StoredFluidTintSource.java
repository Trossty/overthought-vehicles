package me.trossty.car.client.render.item;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import me.trossty.car.client.render.LiquidColours;
import me.trossty.car.registry.ModComponents;
import net.minecraft.client.color.item.ItemTintSource;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.Nullable;

/**
 * The colour of the liquid a drum item is carrying ({@code "type": "car:stored_fluid"}),
 * or {@code default} when it is empty: a drum of lava in your hand is the
 * colour of lava, the same as when it stands on the ground.
 */
public record StoredFluidTintSource(int defaultColor) implements ItemTintSource {

    public static final MapCodec<StoredFluidTintSource> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
            Codec.INT.optionalFieldOf("default", 0xFFFFFF).forGetter(StoredFluidTintSource::defaultColor)
    ).apply(instance, StoredFluidTintSource::new));

    @Override
    public int calculate(ItemStack stack, @Nullable ClientLevel world, @Nullable LivingEntity user) {
        ModComponents.StoredFluid held = stack.get(ModComponents.STORED_FLUID);
        return 0xFF000000 | LiquidColours.of(held == null ? null : held.fluid(), this.defaultColor);
    }

    @Override
    public MapCodec<StoredFluidTintSource> type() {
        return CODEC;
    }
}
