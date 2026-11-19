package me.trossty.car.network;

import me.trossty.car.CarMain;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

/**
 * Sent by a driving client whenever the brake or the throttle goes down or
 * comes up, so everybody else sees the brake lamps and hears the engine pull,
 * lift and blow off. Only the driver's client knows the pedals; the server
 * would otherwise have to guess them from the car speeding up or slowing.
 */
public record PedalsC2SPayload(boolean brake, boolean throttle) implements CustomPacketPayload {

    public static final CustomPacketPayload.Type<PedalsC2SPayload> ID =
            new CustomPacketPayload.Type<>(CarMain.id("pedals"));

    public static final StreamCodec<RegistryFriendlyByteBuf, PedalsC2SPayload> CODEC = StreamCodec.composite(
            ByteBufCodecs.BOOL, PedalsC2SPayload::brake,
            ByteBufCodecs.BOOL, PedalsC2SPayload::throttle,
            PedalsC2SPayload::new);

    @Override
    public CustomPacketPayload.Type<? extends CustomPacketPayload> type() {
        return ID;
    }
}
