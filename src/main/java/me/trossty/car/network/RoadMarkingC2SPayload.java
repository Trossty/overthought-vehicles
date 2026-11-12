package me.trossty.car.network;

import me.trossty.car.CarMain;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

/**
 * Sent when a marking is picked from a road paint tin's menu. The server
 * believes only the choice: the tin has to really be in that hand.
 *
 * @param offHand whether the tin is in the off hand
 * @param marking the {@code AsphaltBlock.Marking} ordinal picked
 */
public record RoadMarkingC2SPayload(boolean offHand, int marking) implements CustomPacketPayload {

    public static final CustomPacketPayload.Type<RoadMarkingC2SPayload> ID =
            new CustomPacketPayload.Type<>(CarMain.id("road_marking"));

    public static final StreamCodec<RegistryFriendlyByteBuf, RoadMarkingC2SPayload> CODEC =
            StreamCodec.composite(
                    ByteBufCodecs.BOOL, RoadMarkingC2SPayload::offHand,
                    ByteBufCodecs.VAR_INT, RoadMarkingC2SPayload::marking,
                    RoadMarkingC2SPayload::new);

    @Override
    public CustomPacketPayload.Type<? extends CustomPacketPayload> type() {
        return ID;
    }
}
