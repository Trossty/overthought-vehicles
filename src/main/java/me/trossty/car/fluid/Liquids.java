package me.trossty.car.fluid;

import com.mojang.serialization.Codec;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.Fluids;
import net.neoforged.neoforge.common.SoundActions;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;

/**
 * A liquid as the mod stores it: just the fluid, since none of the mod's
 * liquids carry extra data, with {@link Fluids#EMPTY} for "nothing". Amounts
 * are millibuckets, the same unit NeoForge's fluid handlers count in.
 */
public final class Liquids {

    public static final Codec<Fluid> CODEC = BuiltInRegistries.FLUID.byNameCodec();
    public static final StreamCodec<RegistryFriendlyByteBuf, Fluid> STREAM_CODEC =
            ByteBufCodecs.registry(Registries.FLUID);

    private Liquids() {
    }

    public static boolean isBlank(Fluid fluid) {
        return fluid == null || fluid == Fluids.EMPTY;
    }

    public static Component name(Fluid fluid) {
        return fluid.getFluidType().getDescription();
    }

    public static SoundEvent fillSound(Fluid fluid) {
        SoundEvent sound = fluid.getFluidType().getSound(SoundActions.BUCKET_FILL);
        return sound == null ? SoundEvents.BUCKET_FILL : sound;
    }

    public static SoundEvent emptySound(Fluid fluid) {
        SoundEvent sound = fluid.getFluidType().getSound(SoundActions.BUCKET_EMPTY);
        return sound == null ? SoundEvents.BUCKET_EMPTY : sound;
    }

    public static int luminance(Fluid fluid) {
        return isBlank(fluid) ? 0 : fluid.getFluidType().getLightLevel();
    }

    /** The first liquid a handler holds, or {@link Fluids#EMPTY}. */
    public static Fluid storedIn(IFluidHandler handler) {
        for (int tank = 0; tank < handler.getTanks(); tank++) {
            FluidStack stack = handler.getFluidInTank(tank);
            if (!stack.isEmpty()) {
                return stack.getFluid();
            }
        }
        return Fluids.EMPTY;
    }

    /**
     * Moves up to {@code max} millibuckets of whatever {@code from} gives first
     * (or of {@code only}, when set) into {@code to}; how much went.
     */
    public static int move(IFluidHandler from, IFluidHandler to, @org.jetbrains.annotations.Nullable Fluid only, int max) {
        if (max <= 0) {
            return 0;
        }
        FluidStack offered = only == null
                ? from.drain(max, IFluidHandler.FluidAction.SIMULATE)
                : from.drain(new FluidStack(only, max), IFluidHandler.FluidAction.SIMULATE);
        if (offered.isEmpty()) {
            return 0;
        }
        int accepted = to.fill(offered, IFluidHandler.FluidAction.SIMULATE);
        if (accepted <= 0) {
            return 0;
        }
        FluidStack taken = from.drain(offered.copyWithAmount(accepted), IFluidHandler.FluidAction.EXECUTE);
        return to.fill(taken, IFluidHandler.FluidAction.EXECUTE);
    }
}
