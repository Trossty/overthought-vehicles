package me.trossty.car.network;

import me.trossty.car.CarMain;
import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.codec.PacketCodecs;
import net.minecraft.network.packet.CustomPayload;

/**
 * Sent when a player points at a part on the car itself and right clicks it.
 *
 * <p>The server cannot work out which part was meant: the whole car is one
 * entity, and vanilla's interaction packet says only "that entity". The aiming
 * happens on the client, so the client is the only one that knows the answer
 * and has to say it out loud.
 *
 * <p>It carries nothing the server trusts, either. The car id is looked up and
 * checked for range, and the part key is checked against the enum — everything
 * after that is the same {@code removePart} and {@code fitPart} a click in the
 * service window goes through, with the same rules about lifts and about what
 * is bolted on top of what.
 *
 * @param car  the car's entity id
 * @param part the part's {@code CarPart} key
 */
public record ServicePartC2SPayload(int car, String part) implements CustomPayload {

    public static final CustomPayload.Id<ServicePartC2SPayload> ID =
            new CustomPayload.Id<>(CarMain.id("service_part"));

    public static final PacketCodec<RegistryByteBuf, ServicePartC2SPayload> CODEC =
            PacketCodec.tuple(
                    PacketCodecs.VAR_INT, ServicePartC2SPayload::car,
                    PacketCodecs.STRING, ServicePartC2SPayload::part,
                    ServicePartC2SPayload::new);

    @Override
    public CustomPayload.Id<? extends CustomPayload> getId() {
        return ID;
    }
}
