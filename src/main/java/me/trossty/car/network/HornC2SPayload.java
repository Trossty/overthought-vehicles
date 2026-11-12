package me.trossty.car.network;

import me.trossty.car.CarMain;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

/**
 * Sent by a driving client when the horn key is pressed. It carries no data:
 * the server already knows which car the sending player is driving.
 */
public record HornC2SPayload() implements CustomPacketPayload {

    public static final HornC2SPayload INSTANCE = new HornC2SPayload();

    public static final CustomPacketPayload.Type<HornC2SPayload> ID =
            new CustomPacketPayload.Type<>(CarMain.id("horn"));

    public static final StreamCodec<RegistryFriendlyByteBuf, HornC2SPayload> CODEC =
            StreamCodec.unit(INSTANCE);

    @Override
    public CustomPacketPayload.Type<? extends CustomPacketPayload> type() {
        return ID;
    }
}
