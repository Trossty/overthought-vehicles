package me.trossty.car.client.render;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import me.trossty.car.entity.CarEntity;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.core.SectionPos;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;

/**
 * The head lamps' light in the world itself: blocks and mobs in front of a
 * car with its lamps on are drawn as if a torch stood there, which is what
 * the pool painted on the road cannot do. Nothing is placed in the world and
 * the server never hears of it; the client simply draws the block light a
 * little higher round a few points down the beam.
 *
 * <p>The points are worked out once a tick, on the client thread. Chunk
 * meshes are built on worker threads and read them while they do, so the
 * list is only ever swapped whole, never changed in place. A mesh bakes the
 * light it was built with, so wherever a point arrives or leaves, the
 * sections round it are built again.
 */
public final class HeadlightGlow {

    /** How bright the light is right at a point, like a torch's 14. */
    private static final float BRIGHTNESS = 14.0F;
    /** Block light lost per block away from a point. */
    private static final float FALLOFF = 1.6F;
    /** How far a point reaches before it is gone, in blocks. */
    private static final double REACH = BRIGHTNESS / FALLOFF;
    /** Where the points sit down the beam, as shares of its length. */
    private static final float[] ALONG = {0.15F, 0.45F, 0.8F};
    /** Moved less than this, a point does not have the chunks built again. */
    private static final double SETTLE = 0.6;

    private static volatile List<Vec3> points = List.of();
    /** The same points by the car they shine from: the client thread's own. */
    private static Map<Integer, List<Vec3>> byCar = Map.of();

    private HeadlightGlow() {
    }

    /**
     * The extra block light this position gets from the lamps, 0 when none:
     * safe from any thread.
     */
    public static int lightAt(BlockPos pos) {
        List<Vec3> now = points;
        if (now.isEmpty()) {
            return 0;
        }
        double x = pos.getX() + 0.5;
        double y = pos.getY() + 0.5;
        double z = pos.getZ() + 0.5;
        double best = 0.0;
        for (Vec3 point : now) {
            double dx = point.x - x;
            double dy = point.y - y;
            double dz = point.z - z;
            double distance = Math.sqrt(dx * dx + dy * dy + dz * dz);
            best = Math.max(best, BRIGHTNESS - distance * FALLOFF);
        }
        return (int) Math.max(0.0, best);
    }

    /** Folds the lamps' light into a packed light value, block part only. */
    public static int brighten(int packed, BlockPos pos) {
        int extra = lightAt(pos);
        int block = net.minecraft.client.renderer.LightTexture.block(packed);
        if (extra <= block) {
            return packed;
        }
        return net.minecraft.client.renderer.LightTexture.pack(extra,
                net.minecraft.client.renderer.LightTexture.sky(packed));
    }

    /**
     * Once a client tick: where the lamps shine now, and which chunks that
     * changes. Each car's points are kept under its own id, so only the
     * chunks round a car whose beam actually moved are built again, however
     * the world happens to list its entities this tick.
     */
    public static void tick(Minecraft client) {
        Map<Integer, List<Vec3>> before = byCar;
        if (client.level == null || client.player == null) {
            byCar = Map.of();
            points = List.of();
            return;
        }
        Map<Integer, List<Vec3>> after = new HashMap<>();
        List<Vec3> stale = new ArrayList<>();
        for (Entity entity : client.level.entitiesForRendering()) {
            if (entity instanceof CarEntity car && car.distanceToSqr(client.player) < 96.0 * 96.0
                    && CarEntityRenderer.lampsOn(car)) {
                List<Vec3> was = before.get(car.getId());
                List<Vec3> now = pointsFor(car);
                if (was != null && !moved(was, now)) {
                    // Settled: keep the points the chunks were built with.
                    after.put(car.getId(), was);
                    continue;
                }
                after.put(car.getId(), now);
                if (was != null) {
                    stale.addAll(was);
                }
                stale.addAll(now);
            }
        }
        for (Map.Entry<Integer, List<Vec3>> gone : before.entrySet()) {
            if (!after.containsKey(gone.getKey())) {
                stale.addAll(gone.getValue());
            }
        }
        byCar = after;
        if (!stale.isEmpty()) {
            // The new points first, so a chunk built from here on sees them.
            List<Vec3> all = new ArrayList<>();
            after.values().forEach(all::addAll);
            points = List.copyOf(all);
            stale.forEach(point -> rebuild(client, point));
        }
    }

    private static List<Vec3> pointsFor(CarEntity car) {
        me.trossty.car.vehicle.VehicleDefinition definition = car.getDefinition();
        float length = definition == null ? me.trossty.car.vehicle.VehicleDefinition.Lights.PLAIN.length()
                : definition.lights().length();
        float yawRad = car.getYRot() * ((float) Math.PI / 180.0F);
        double dirX = -Mth.sin(yawRad);
        double dirZ = Mth.cos(yawRad);
        double front = car.getBbWidth() / 2.0 + 1.0;
        List<Vec3> list = new ArrayList<>(ALONG.length);
        for (float share : ALONG) {
            double d = front + length * share;
            list.add(new Vec3(car.getX() + dirX * d, car.getY() + 1.0, car.getZ() + dirZ * d));
        }
        return list;
    }

    private static boolean moved(List<Vec3> before, List<Vec3> after) {
        if (before.size() != after.size()) {
            return true;
        }
        for (int i = 0; i < before.size(); i++) {
            if (before.get(i).distanceToSqr(after.get(i)) > SETTLE * SETTLE) {
                return true;
            }
        }
        return false;
    }

    private static void rebuild(Minecraft client, Vec3 point) {
        int minX = SectionPos.blockToSectionCoord(Mth.floor(point.x - REACH));
        int maxX = SectionPos.blockToSectionCoord(Mth.floor(point.x + REACH));
        int minY = SectionPos.blockToSectionCoord(Mth.floor(point.y - REACH));
        int maxY = SectionPos.blockToSectionCoord(Mth.floor(point.y + REACH));
        int minZ = SectionPos.blockToSectionCoord(Mth.floor(point.z - REACH));
        int maxZ = SectionPos.blockToSectionCoord(Mth.floor(point.z + REACH));
        for (int x = minX; x <= maxX; x++) {
            for (int y = minY; y <= maxY; y++) {
                for (int z = minZ; z <= maxZ; z++) {
                    client.levelRenderer.setSectionDirty(x, y, z);
                }
            }
        }
    }
}
