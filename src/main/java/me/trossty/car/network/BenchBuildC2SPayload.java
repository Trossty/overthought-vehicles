package me.trossty.car.network;

import me.trossty.car.CarMain;
import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

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
        implements CustomPacketPayload {

    public static final CustomPacketPayload.Type<BenchBuildC2SPayload> ID =
            new CustomPacketPayload.Type<>(CarMain.id("bench_build"));

    public static final StreamCodec<RegistryFriendlyByteBuf, BenchBuildC2SPayload> CODEC =
            StreamCodec.composite(
                    BlockPos.STREAM_CODEC, BenchBuildC2SPayload::bench,
                    ByteBufCodecs.VAR_INT, BenchBuildC2SPayload::recipe,
                    ByteBufCodecs.VAR_INT, BenchBuildC2SPayload::quality,
                    BenchBuildC2SPayload::new);

    @Override
    public CustomPacketPayload.Type<? extends CustomPacketPayload> type() {
        return ID;
    }
}
