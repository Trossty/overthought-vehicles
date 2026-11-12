package me.trossty.car.network;

import me.trossty.car.CarMain;
import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.codec.PacketCodecs;
import net.minecraft.network.packet.CustomPayload;

/**
 * Sent when a marking is picked from a road paint tin's menu. The server
 * believes only the choice: the tin has to really be in that hand.
 *
 * @param offHand whether the tin is in the off hand
 * @param marking the {@code AsphaltBlock.Marking} ordinal picked
 */
public record RoadMarkingC2SPayload(boolean offHand, int marking) implements CustomPayload {

    public static final CustomPayload.Id<RoadMarkingC2SPayload> ID =
            new CustomPayload.Id<>(CarMain.id("road_marking"));

    public static final PacketCodec<RegistryByteBuf, RoadMarkingC2SPayload> CODEC =
            PacketCodec.tuple(
                    PacketCodecs.BOOLEAN, RoadMarkingC2SPayload::offHand,
                    PacketCodecs.VAR_INT, RoadMarkingC2SPayload::marking,
                    RoadMarkingC2SPayload::new);

    @Override
    public CustomPayload.Id<? extends CustomPayload> getId() {
        return ID;
    }
}
