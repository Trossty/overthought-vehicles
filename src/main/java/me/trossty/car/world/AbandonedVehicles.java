package me.trossty.car.world;

import me.trossty.car.CarConfig;
import me.trossty.car.entity.CarEntity;
import me.trossty.car.entity.CarPart;
import me.trossty.car.registry.ModEntities;
import me.trossty.car.vehicle.VehicleDefinition;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerChunkEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.block.BlockState;
import net.minecraft.entity.SpawnReason;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.DyeColor;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.util.math.random.Random;
import net.minecraft.world.Heightmap;
import net.minecraft.world.World;
import org.jspecify.annotations.Nullable;

import java.util.ArrayDeque;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;

/**
 * Now and then a newly generated chunk has an old car left on it: rusted
 * through, its parts tired, a few missing, a splash of stale fuel in the
 * tank. Something to find, haul home and bring back to life on the lift,
 * which is the mechanic's half of the mod in one find.
 *
 * <p>How rare is {@link CarConfig#abandonedVehicleRarity}: one chunk in that
 * many, and 0 turns it off. The car is put down on the tick after the chunk
 * is generated, not during it, so nothing is added to a chunk mid-load.
 */
public final class AbandonedVehicles {

    /** Chunks waiting for their car, per world. Server thread only. */
    private static final Map<ServerWorld, ArrayDeque<ChunkPos>> PENDING = new IdentityHashMap<>();

    /** Most chunks dealt with a tick, so a burst of exploring never stalls one. */
    private static final int PER_TICK = 4;

    private AbandonedVehicles() {
    }

    public static void register() {
        ServerChunkEvents.CHUNK_GENERATE.register((world, chunk) -> {
            int rarity = CarConfig.get().abandonedVehicleRarity;
            if (rarity <= 0 || world.getRegistryKey() != World.OVERWORLD
                    || world.getRandom().nextInt(rarity) != 0) {
                return;
            }
            PENDING.computeIfAbsent(world, w -> new ArrayDeque<>()).add(chunk.getPos());
        });
        ServerTickEvents.END_WORLD_TICK.register(world -> {
            ArrayDeque<ChunkPos> queue = PENDING.get(world);
            for (int i = 0; queue != null && i < PER_TICK && !queue.isEmpty(); i++) {
                ChunkPos chunk = queue.poll();
                if (world.isChunkLoaded(chunk.x, chunk.z)) {
                    place(world, chunk, world.getRandom());
                }
            }
        });
        // Worlds are made afresh on every server start; never hold on to the old ones.
        net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents.SERVER_STOPPED.register(server -> PENDING.clear());
    }

    /** Puts an abandoned car somewhere dry and open in the chunk; null when it found nowhere. */
    public static @Nullable CarEntity place(ServerWorld world, ChunkPos chunk, Random random) {
        for (int attempt = 0; attempt < 6; attempt++) {
            int x = chunk.getStartX() + 2 + random.nextInt(12);
            int z = chunk.getStartZ() + 2 + random.nextInt(12);
            int y = world.getTopY(Heightmap.Type.MOTION_BLOCKING_NO_LEAVES, x, z);
            BlockPos ground = new BlockPos(x, y - 1, z);
            BlockState below = world.getBlockState(ground);
            if (!below.getFluidState().isEmpty() || !below.isSideSolidFullSquare(world, ground, net.minecraft.util.math.Direction.UP)) {
                continue;
            }
            CarEntity car = abandoned(world, random);
            if (car == null) {
                return null;
            }
            car.refreshPositionAndAngles(x + 0.5, y, z + 0.5, random.nextFloat() * 360.0F, 0.0F);
            if (!world.isSpaceEmpty(car, car.getBoundingBox())) {
                continue;
            }
            world.spawnEntity(car);
            return car;
        }
        return null;
    }

    /** An old car, worn as if it had sat out for years, not yet placed anywhere. */
    public static @Nullable CarEntity abandoned(ServerWorld world, Random random) {
        CarEntity car = ModEntities.CAR.create(world, SpawnReason.CHUNK_GENERATION);
        if (car == null) {
            return null;
        }
        List<Identifier> cars = world.getRegistryManager().getOptional(VehicleDefinition.KEY)
                .map(registry -> registry.getIds().stream()
                        .filter(id -> {
                            VehicleDefinition defined = registry.get(id);
                            return defined != null && defined.chassis() == VehicleDefinition.Chassis.CAR;
                        })
                        .sorted(java.util.Comparator.comparing(Identifier::toString))
                        .toList())
                .orElse(List.of());
        if (!cars.isEmpty()) {
            car.setModel(cars.get(random.nextInt(cars.size())));
            VehicleDefinition defined = car.getDefinition();
            if (defined != null) {
                car.setSetup(defined.factory().setup());
                car.calculateDimensions();
            }
        }
        // The colours old cars are left in: nobody abandons the bright pink one.
        DyeColor[] faded = {DyeColor.WHITE, DyeColor.LIGHT_GRAY, DyeColor.GRAY, DyeColor.BROWN,
                DyeColor.GREEN, DyeColor.BLACK, DyeColor.RED, DyeColor.BLUE};
        car.setColor(faded[random.nextInt(faded.length)]);
        wear(car, random);
        return car;
    }

    /** Years out in the weather: dents, tired parts, a couple gone, stale fuel. */
    public static void wear(CarEntity car, Random random) {
        car.setHealth(CarEntity.MAX_HEALTH * (0.3F + 0.4F * random.nextFloat()));
        car.setOdometer(CarEntity.HIGH_MILEAGE * (1.2F + 2.8F * random.nextFloat()));
        for (CarPart part : CarPart.values()) {
            if (part == CarPart.BATTERY) {
                // Flat, never missing: the one part a find is sure to need, and the cheapest.
                car.setCondition(part, CarPart.MAX_CONDITION * 0.05F);
                continue;
            }
            float left = 0.12F + 0.5F * random.nextFloat();
            car.setCondition(part, CarPart.MAX_CONDITION * left);
        }
        // A couple of parts somebody already had off it.
        CarPart[] strippable = {CarPart.SPARK_PLUGS, CarPart.AIR_FILTER, CarPart.OIL_FILTER,
                CarPart.EXHAUST, CarPart.BRAKE_PADS, CarPart.ALTERNATOR};
        int taken = random.nextInt(3);
        for (int i = 0; i < taken; i++) {
            car.setCondition(strippable[random.nextInt(strippable.length)], 0.0F);
        }
        if (!car.isElectric()) {
            // A new car comes off the line full; this one has been siphoned dry but for a splash.
            car.setFuel(0.0F);
            car.addFuel(2.0F + random.nextInt(6), car.getEngineFuel(), false);
            // Old fuel goes off: it counts as dirty as raw fuel does.
            car.setDirtyFuel(0.6F + 0.4F * random.nextFloat());
        }
    }
}
