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
 * payload carries the severity in condition points and the side of the body
 * that hit, as a {@link me.trossty.car.entity.BodyZone} ordinal or -1 for none;
 * the server clamps both and accepts them only from the car's own driver.
 */
public record CrashC2SPayload(float severity, byte zone) implements CustomPacketPayload {

    public static final CustomPacketPayload.Type<CrashC2SPayload> ID =
            new CustomPacketPayload.Type<>(CarMain.id("crash"));

    public static final StreamCodec<RegistryFriendlyByteBuf, CrashC2SPayload> CODEC =
            StreamCodec.composite(ByteBufCodecs.FLOAT, CrashC2SPayload::severity,
                    ByteBufCodecs.BYTE, CrashC2SPayload::zone, CrashC2SPayload::new);

    /** The side that hit, or null for none or for a number no side has. */
    public @org.jetbrains.annotations.Nullable me.trossty.car.entity.BodyZone side() {
        me.trossty.car.entity.BodyZone[] zones = me.trossty.car.entity.BodyZone.values();
        return this.zone >= 0 && this.zone < zones.length ? zones[this.zone] : null;
    }

    @Override
    public CustomPacketPayload.Type<? extends CustomPacketPayload> type() {
        return ID;
    }
}
