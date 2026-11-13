package me.trossty.car.network;

import me.trossty.car.CarMain;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

/**
 * Sent by a flying client whenever its aircraft's bank changes. The keys that
 * roll it only reach the pilot's own client, so the server learns the bank
 * from them, seats the crew by it and hands it on to everybody watching.
 */
public record BankC2SPayload(float bank) implements CustomPacketPayload {

    public static final CustomPacketPayload.Type<BankC2SPayload> ID =
            new CustomPacketPayload.Type<>(CarMain.id("bank"));

    public static final StreamCodec<RegistryFriendlyByteBuf, BankC2SPayload> CODEC =
            StreamCodec.composite(ByteBufCodecs.FLOAT, BankC2SPayload::bank, BankC2SPayload::new);

    @Override
    public CustomPacketPayload.Type<? extends CustomPacketPayload> type() {
        return ID;
    }
}
