package me.trossty.car.registry;

import com.mojang.serialization.Codec;
import me.trossty.car.CarMain;
import net.minecraft.component.ComponentType;
import net.minecraft.network.codec.PacketCodecs;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;

/**
 * Item data components this mod adds.
 */
public final class ModComponents {

    /**
     * The paint on a car item, as a {@code DyeColor} index. Without this the
     * colour would be lost the moment a painted car was picked back up.
     */
    public static final ComponentType<Integer> CAR_COLOR = register("color",
            ComponentType.<Integer>builder()
                    .codec(Codec.intRange(0, 15))
                    .packetCodec(PacketCodecs.VAR_INT)
                    .build());

    /**
     * How battered a car item is, so a dented car you picked up comes back
     * dented instead of factory fresh. The range matches CarEntity.MAX_HEALTH;
     * it is written as a literal to keep this class free of entity imports.
     */
    public static final ComponentType<Float> CAR_HEALTH = register("health",
            ComponentType.<Float>builder()
                    .codec(Codec.floatRange(0.0F, 20.0F))
                    .packetCodec(PacketCodecs.FLOAT)
                    .build());

    /**
     * The load on a tanker item. A tanker you pocket half full comes back half
     * full; otherwise the only safe way to move one would be to empty it first.
     */
    public static final ComponentType<Integer> TANKER_CARGO = register("cargo",
            ComponentType.<Integer>builder()
                    .codec(Codec.intRange(0, 8000))
                    .packetCodec(PacketCodecs.VAR_INT)
                    .build());

    /** What that load is: any fluid at all. A load with no liquid named is petrol, as every load once was. */
    public static final ComponentType<net.fabricmc.fabric.api.transfer.v1.fluid.FluidVariant> TANKER_LIQUID =
            register("liquid", ComponentType.<net.fabricmc.fabric.api.transfer.v1.fluid.FluidVariant>builder()
                    .codec(net.fabricmc.fabric.api.transfer.v1.fluid.FluidVariant.CODEC)
                    .packetCodec(net.fabricmc.fabric.api.transfer.v1.fluid.FluidVariant.PACKET_CODEC)
                    .build());

    /**
     * How worn a spare part is, as a whole percent. A part with none of this on
     * it is a new one, which is what everything the workbench makes is.
     */
    public static final ComponentType<Integer> PART_CONDITION = register("part_condition",
            ComponentType.<Integer>builder()
                    .codec(Codec.intRange(0, 100))
                    .packetCodec(PacketCodecs.VAR_INT)
                    .build());

    /**
     * What a spare part was built to, as a {@code CarPart.Quality} ordinal. A
     * part without it is a standard one, which is what a car comes out of the
     * lift with.
     */
    public static final ComponentType<Integer> PART_QUALITY = register("part_quality",
            ComponentType.<Integer>builder()
                    .codec(Codec.intRange(0, 2))
                    .packetCodec(PacketCodecs.VAR_INT)
                    .build());

    /**
     * Every fitted part's condition on a pocketed car, in {@code CarPart}
     * order. Without it a tired car could be picked up and put down with a
     * fresh engine in it.
     */
    public static final ComponentType<java.util.List<Integer>> CAR_PARTS = register("parts",
            ComponentType.<java.util.List<Integer>>builder()
                    .codec(Codec.intRange(0, 100).listOf())
                    .packetCodec(PacketCodecs.VAR_INT.collect(PacketCodecs.toList()))
                    .build());

    /**
     * Every fitted part's quality on a pocketed car, two bits each, packed the
     * same way the entity tracks them. One number rather than a list because
     * quality cannot be anything but one of three things.
     */
    public static final ComponentType<Integer> CAR_PART_QUALITY = register("part_quality_bits",
            ComponentType.<Integer>builder()
                    .codec(Codec.INT)
                    .packetCodec(PacketCodecs.INTEGER)
                    .build());

    /**
     * Which kind of a part this is: which engine, which tyre, which filter.
     * See {@code CarSetup}. A part without it is the plain kind.
     */
    public static final ComponentType<Integer> PART_VARIANT = register("part_variant",
            ComponentType.<Integer>builder()
                    .codec(Codec.intRange(0, 15))
                    .packetCodec(PacketCodecs.VAR_INT)
                    .build());

    /**
     * Which pump a fuel nozzle was taken off. The nozzle is only ever on the
     * end of that pump's hose, so it goes back the moment it is out of reach.
     */
    public static final ComponentType<net.minecraft.util.math.BlockPos> PUMP = register("pump",
            ComponentType.<net.minecraft.util.math.BlockPos>builder()
                    .codec(net.minecraft.util.math.BlockPos.CODEC)
                    .packetCodec(net.minecraft.util.math.BlockPos.PACKET_CODEC)
                    .build());

    /** A pocketed car's {@code CarSetup}: its engine, tyres, filter, turbo and tank. */
    public static final ComponentType<Integer> CAR_SETUP = register("setup",
            ComponentType.<Integer>builder()
                    .codec(Codec.INT)
                    .packetCodec(PacketCodecs.INTEGER)
                    .build());

    /** What a jerry can holds, as a {@code CarSetup.Fuel} ordinal. Absent is petrol. */
    /** Which data-pack vehicle a car item is; absent for the plain machines. */
    public static final ComponentType<net.minecraft.util.Identifier> CAR_VEHICLE = register("vehicle",
            ComponentType.<net.minecraft.util.Identifier>builder()
                    .codec(net.minecraft.util.Identifier.CODEC)
                    .packetCodec(net.minecraft.util.Identifier.PACKET_CODEC)
                    .build());

    public static final ComponentType<Integer> FUEL_KIND = register("fuel_kind",
            ComponentType.<Integer>builder()
                    .codec(Codec.intRange(0, 2))
                    .packetCodec(PacketCodecs.VAR_INT)
                    .build());

    /** On a jerry can: what is in it came straight off the refinery, unhydrotreated. */
    public static final ComponentType<Boolean> FUEL_RAW = register("fuel_raw",
            ComponentType.<Boolean>builder()
                    .codec(Codec.BOOL)
                    .packetCodec(PacketCodecs.BOOLEAN)
                    .build());

    private ModComponents() {
    }

    /**
     * Paint from a spray can: any colour at all, as 0xRRGGBB. Wins over
     * {@link #CAR_COLOR} when both are there; a dye on the car clears it.
     */
    public static final ComponentType<Integer> CAR_PAINT = register("paint",
            ComponentType.<Integer>builder()
                    .codec(Codec.intRange(0, 0xFFFFFF))
                    .packetCodec(PacketCodecs.VAR_INT)
                    .build());

    /**
     * A lock and the key that opens it share this number. On a key it says
     * which vehicle it opens; on a vehicle item it is that vehicle's lock, so
     * its keys still work after it has been pocketed and put down again.
     */
    public static final ComponentType<Long> VEHICLE_KEY = register("key",
            ComponentType.<Long>builder()
                    .codec(Codec.LONG)
                    .packetCodec(PacketCodecs.VAR_LONG)
                    .build());

    /** The charge a battery bank carried when it was broken, so picking one up does not empty it. */
    public static final ComponentType<Long> STORED_ENERGY = register("stored_energy",
            ComponentType.<Long>builder()
                    .codec(Codec.LONG)
                    .packetCodec(PacketCodecs.VAR_LONG)
                    .build());

    /** On a tin of road paint: which {@code AsphaltBlock.Marking} it paints, as an ordinal. */
    public static final ComponentType<Integer> ROAD_MARKING = register("road_marking",
            ComponentType.<Integer>builder()
                    .codec(Codec.intRange(0, 15))
                    .packetCodec(PacketCodecs.VAR_INT)
                    .build());

    /** On an OBD tablet: the car it was last plugged into. */
    public static final ComponentType<java.util.UUID> OBD_LINK = register("obd_link",
            ComponentType.<java.util.UUID>builder()
                    .codec(net.minecraft.util.Uuids.CODEC)
                    .packetCodec(net.minecraft.util.Uuids.PACKET_CODEC)
                    .build());

    /**
     * What a drum held when it was broken: a drum is carried full, the way a
     * shulker box is carried with its things in it.
     */
    public record StoredFluid(net.fabricmc.fabric.api.transfer.v1.fluid.FluidVariant fluid, long amount) {
        public static final Codec<StoredFluid> CODEC =
                com.mojang.serialization.codecs.RecordCodecBuilder.create(instance -> instance.group(
                        net.fabricmc.fabric.api.transfer.v1.fluid.FluidVariant.CODEC.fieldOf("fluid")
                                .forGetter(StoredFluid::fluid),
                        Codec.LONG.fieldOf("amount").forGetter(StoredFluid::amount))
                        .apply(instance, StoredFluid::new));
        public static final net.minecraft.network.codec.PacketCodec<net.minecraft.network.RegistryByteBuf, StoredFluid>
                PACKET_CODEC = net.minecraft.network.codec.PacketCodec.tuple(
                        net.fabricmc.fabric.api.transfer.v1.fluid.FluidVariant.PACKET_CODEC, StoredFluid::fluid,
                        PacketCodecs.VAR_LONG, StoredFluid::amount,
                        StoredFluid::new);
    }

    public static final ComponentType<StoredFluid> STORED_FLUID = register("stored_fluid",
            ComponentType.<StoredFluid>builder()
                    .codec(StoredFluid.CODEC)
                    .packetCodec(StoredFluid.PACKET_CODEC)
                    .build());

    private static <T> ComponentType<T> register(String name, ComponentType<T> type) {
        return Registry.register(Registries.DATA_COMPONENT_TYPE, CarMain.id(name), type);
    }

    /** Forces class loading so the static fields above are registered. */
    public static void register() {
    }
}
