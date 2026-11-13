package me.trossty.car.network;

import me.trossty.car.CarMain;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

/**
 * Sent by a driving client whenever the brake pedal goes down or comes up, so
 * everybody else sees the brake lamps. Only the driver's client knows the
 * pedal; the server would otherwise have to guess it from the car slowing.
 */
public record BrakeC2SPayload(boolean pressed) implements CustomPacketPayload {

    public static final CustomPacketPayload.Type<BrakeC2SPayload> ID =
            new CustomPacketPayload.Type<>(CarMain.id("brake"));

    public static final StreamCodec<RegistryFriendlyByteBuf, BrakeC2SPayload> CODEC =
            ByteBufCodecs.BOOL.<RegistryFriendlyByteBuf>cast().map(BrakeC2SPayload::new, BrakeC2SPayload::pressed);

    @Override
    public CustomPacketPayload.Type<? extends CustomPacketPayload> type() {
        return ID;
    }
}
