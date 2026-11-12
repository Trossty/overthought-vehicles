package me.trossty.car.network;

import me.trossty.car.CarMain;
import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.packet.CustomPayload;

/**
 * Sent by a driving client when the horn key is pressed. It carries no data:
 * the server already knows which car the sending player is driving.
 */
public record HornC2SPayload() implements CustomPayload {

    public static final HornC2SPayload INSTANCE = new HornC2SPayload();

    public static final CustomPayload.Id<HornC2SPayload> ID =
            new CustomPayload.Id<>(CarMain.id("horn"));

    public static final PacketCodec<RegistryByteBuf, HornC2SPayload> CODEC =
            PacketCodec.unit(INSTANCE);

    @Override
    public CustomPayload.Id<? extends CustomPayload> getId() {
        return ID;
    }
}
