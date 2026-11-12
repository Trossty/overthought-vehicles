package me.trossty.car.world;

import me.trossty.car.CarConfig;
import me.trossty.car.entity.CarEntity;
import me.trossty.car.entity.CarPart;
import me.trossty.car.registry.ModEntities;
import me.trossty.car.vehicle.VehicleDefinition;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerChunkEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap;
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
    private static final Map<ServerLevel, ArrayDeque<ChunkPos>> PENDING = new IdentityHashMap<>();

    /** Most chunks dealt with a tick, so a burst of exploring never stalls one. */
    private static final int PER_TICK = 4;

    private AbandonedVehicles() {
    }

    public static void register() {
        ServerChunkEvents.CHUNK_GENERATE.register((world, chunk) -> {
            int rarity = CarConfig.get().abandonedVehicleRarity;
            if (rarity <= 0 || world.dimension() != Level.OVERWORLD
                    || world.getRandom().nextInt(rarity) != 0) {
                return;
            }
            PENDING.computeIfAbsent(world, w -> new ArrayDeque<>()).add(chunk.getPos());
        });
        ServerTickEvents.END_WORLD_TICK.register(world -> {
            ArrayDeque<ChunkPos> queue = PENDING.get(world);
            for (int i = 0; queue != null && i < PER_TICK && !queue.isEmpty(); i++) {
                ChunkPos chunk = queue.poll();
                if (world.hasChunk(chunk.x, chunk.z)) {
                    place(world, chunk, world.getRandom());
                }
            }
        });
        // Worlds are made afresh on every server start; never hold on to the old ones.
        net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents.SERVER_STOPPED.register(server -> PENDING.clear());
    }

    /** Puts an abandoned car somewhere dry and open in the chunk; null when it found nowhere. */
    public static @Nullable CarEntity place(ServerLevel world, ChunkPos chunk, RandomSource random) {
        for (int attempt = 0; attempt < 6; attempt++) {
            int x = chunk.getMinBlockX() + 2 + random.nextInt(12);
            int z = chunk.getMinBlockZ() + 2 + random.nextInt(12);
            int y = world.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, x, z);
            BlockPos ground = new BlockPos(x, y - 1, z);
            BlockState below = world.getBlockState(ground);
            if (!below.getFluidState().isEmpty() || !below.isFaceSturdy(world, ground, net.minecraft.core.Direction.UP)) {
                continue;
            }
            CarEntity car = abandoned(world, random);
            if (car == null) {
                return null;
            }
            car.snapTo(x + 0.5, y, z + 0.5, random.nextFloat() * 360.0F, 0.0F);
            if (!world.noCollision(car, car.getBoundingBox())) {
                continue;
            }
            world.addFreshEntity(car);
            return car;
        }
        return null;
    }

    /** An old car, worn as if it had sat out for years, not yet placed anywhere. */
    public static @Nullable CarEntity abandoned(ServerLevel world, RandomSource random) {
        CarEntity car = ModEntities.CAR.create(world, EntitySpawnReason.CHUNK_GENERATION);
        if (car == null) {
            return null;
        }
        List<Identifier> cars = world.registryAccess().lookup(VehicleDefinition.KEY)
                .map(registry -> registry.keySet().stream()
                        .filter(id -> {
                            VehicleDefinition defined = registry.getValue(id);
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
                car.refreshDimensions();
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
    public static void wear(CarEntity car, RandomSource random) {
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
