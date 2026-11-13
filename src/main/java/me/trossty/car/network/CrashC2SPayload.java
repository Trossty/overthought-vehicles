package me.trossty.car.network;

import me.trossty.car.CarMain;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

/**
 * Sent by a driving client when its car hits something.
 *
 * <p>The collision only exists in that client's simulation — the server is sent
 * positions, not physics — so this is the only way it can learn about one. The
 * payload carries the severity in condition points; the server clamps it and
 * accepts it only from the car's own driver.
 */
public record CrashC2SPayload(float severity) implements CustomPacketPayload {

    public static final CustomPacketPayload.Type<CrashC2SPayload> ID =
            new CustomPacketPayload.Type<>(CarMain.id("crash"));

    public static final StreamCodec<RegistryFriendlyByteBuf, CrashC2SPayload> CODEC =
            StreamCodec.composite(ByteBufCodecs.FLOAT, CrashC2SPayload::severity, CrashC2SPayload::new);

    @Override
    public CustomPacketPayload.Type<? extends CustomPacketPayload> type() {
        return ID;
    }
}
