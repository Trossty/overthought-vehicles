package me.trossty.car.network;

import me.trossty.car.CarMain;
import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.codec.PacketCodecs;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.math.BlockPos;

/**
 * Sent when a player presses the build button on a bench top.
 *
 * <p>Which recipe is showing on a bench is the business of whoever is looking
 * at it — two people at one bench can each be leafing through the catalogue —
 * so the choice lives on the client and travels with the press. The server
 * believes none of it: the block is looked up and has to be a bench or a ramp,
 * the player has to be standing at it, the recipe has to be one that kind of
 * bench makes, and the materials are counted in the player's real pockets.
 *
 * @param bench   where the bench is
 * @param recipe  which line of that bench's catalogue
 * @param quality the grade to build to, as a {@code CarPart.Quality} ordinal
 */
public record BenchBuildC2SPayload(BlockPos bench, int recipe, int quality)
        implements CustomPayload {

    public static final CustomPayload.Id<BenchBuildC2SPayload> ID =
            new CustomPayload.Id<>(CarMain.id("bench_build"));

    public static final PacketCodec<RegistryByteBuf, BenchBuildC2SPayload> CODEC =
            PacketCodec.tuple(
                    BlockPos.PACKET_CODEC, BenchBuildC2SPayload::bench,
                    PacketCodecs.VAR_INT, BenchBuildC2SPayload::recipe,
                    PacketCodecs.VAR_INT, BenchBuildC2SPayload::quality,
                    BenchBuildC2SPayload::new);

    @Override
    public CustomPayload.Id<? extends CustomPayload> getId() {
        return ID;
    }
}
