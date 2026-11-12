package me.trossty.car.network;

import me.trossty.car.CarMain;
import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.codec.PacketCodecs;
import net.minecraft.network.packet.CustomPayload;

/**
 * Sent by a driving client when its car hits something.
 *
 * <p>The collision only exists in that client's simulation — the server is sent
 * positions, not physics — so this is the only way it can learn about one. The
 * payload carries the severity in condition points; the server clamps it and
 * accepts it only from the car's own driver.
 */
public record CrashC2SPayload(float severity) implements CustomPayload {

    public static final CustomPayload.Id<CrashC2SPayload> ID =
            new CustomPayload.Id<>(CarMain.id("crash"));

    public static final PacketCodec<RegistryByteBuf, CrashC2SPayload> CODEC =
            PacketCodec.tuple(PacketCodecs.FLOAT, CrashC2SPayload::severity, CrashC2SPayload::new);

    @Override
    public CustomPayload.Id<? extends CustomPayload> getId() {
        return ID;
    }
}
