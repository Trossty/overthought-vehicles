package me.trossty.car.entity;

import me.trossty.car.vehicle.VehicleDefinition;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

/**
 * Where each part sits on one particular vehicle, worked out from its own
 * definition: the tyre box is on its real front wheel, the engine bay is
 * between its front axle and its nose, the gearbox under its floor between its
 * axles, the exhaust at its own tail. A long van, a short hatch, a cab-over
 * lorry and a motorbike each get their own map, so pointing at the wheel of a
 * long van is pointing at the wheel.
 *
 * <p>Worked in the definition's pixels — x across, y up, z towards the front —
 * and handed out as {@link CarPart.Anchor}s in blocks, in the model's sense
 * (x and z the other way round), which is what the aiming code expects.
 */
public final class PartLayout {

    /** The Corolla's half-width: parts are sized for it and scaled for the rest. */
    private static final float REFERENCE_HALF_WIDTH = 17.36F;

    private PartLayout() {
    }

    /** The map for this vehicle, or the plain one when there is no definition. */
    public static Map<CarPart, CarPart.Anchor> of(VehicleDefinition definition) {
        Map<CarPart, CarPart.Anchor> map = new EnumMap<>(CarPart.class);
        if (definition == null || definition.wheels().isEmpty()) {
            for (CarPart part : CarPart.values()) {
                map.put(part, part.anchor());
            }
            return map;
        }

        // The body's extent, glass and cargo left out.
        float front = -Float.MAX_VALUE;
        float rear = Float.MAX_VALUE;
        float halfWidth = 0.0F;
        float bottom = Float.MAX_VALUE;
        for (VehicleDefinition.Box box : definition.shape()) {
            if (!solid(box)) {
                continue;
            }
            front = Math.max(front, Math.max(box.from().z(), box.to().z()));
            rear = Math.min(rear, Math.min(box.from().z(), box.to().z()));
            halfWidth = Math.max(halfWidth, Math.max(Math.abs(box.from().x()), Math.abs(box.to().x())));
            bottom = Math.min(bottom, Math.min(box.from().y(), box.to().y()));
        }
        if (front == -Float.MAX_VALUE) {
            return of(null);
        }

        // The front wheel on the near side is the one with the tyre box; the
        // rear axle is the furthest back.
        VehicleDefinition.Wheel wheel = null;
        float rearAxle = Float.MAX_VALUE;
        float frontAxle = -Float.MAX_VALUE;
        for (VehicleDefinition.Wheel each : definition.wheels()) {
            frontAxle = Math.max(frontAxle, each.at().z());
            rearAxle = Math.min(rearAxle, each.at().z());
        }
        for (VehicleDefinition.Wheel each : definition.wheels()) {
            if (each.at().z() < frontAxle - 0.5F) {
                continue;
            }
            if (wheel == null || each.at().x() < wheel.at().x()) {
                wheel = each;
            }
        }
        float wheelbase = Math.max(20.0F, frontAxle - rearAxle);
        float radius = wheel.radius();
        float axleY = wheel.at().y();
        float scale = Math.max(0.6F, Math.min(1.3F, halfWidth / REFERENCE_HALF_WIDTH));

        if (definition.chassis() == VehicleDefinition.Chassis.MOTORCYCLE) {
            motorcycle(map, wheel, frontAxle, rearAxle);
            return map;
        }

        // The engine bay: from a bulkhead behind the front axle to the nose,
        // from just under the axle line to the bonnet — or, under a cab, to
        // where an engine would fit.
        float nose = Math.max(8.0F, front - frontAxle);
        float bay = axleY - 1.0F;
        float bonnet = Math.min(heightAt(definition, frontAxle, front - 2.0F), axleY + 1.7F * radius);
        float tall = Math.max(6.0F, bonnet - bay);
        float engineZ = frontAxle + 0.2F * nose;
        float side = 0.55F * halfWidth;

        net.minecraft.world.phys.AABB hollow = me.trossty.car.vehicle.LidCavity.of(
                definition, VehicleDefinition.Box.BONNET);
        if (hollow != null) {
            // An engine bay under a bonnet: each box goes exactly where the
            // part is drawn in it, so what you see is what you click.
            for (BaySpot spot : BAY) {
                float[] at = spot.centre(hollow);
                put(map, spot.part(), at[0], at[1], at[2], at[3]);
            }
        } else {
            put(map, CarPart.RADIATOR, 0.0F, bay + 0.5F * tall, front - 2.5F, 2.0F * scale);
            put(map, CarPart.ENGINE, 0.0F, bay + 0.4F * tall, engineZ, 3.2F * scale);
            put(map, CarPart.SPARK_PLUGS, 0.0F, bay + 0.88F * tall, engineZ, 1.7F * scale);
            put(map, CarPart.AIR_FILTER, 0.0F, bay + 0.8F * tall, frontAxle - 0.25F * nose, 1.9F * scale);
            put(map, CarPart.BATTERY, side, bay + 0.8F * tall, engineZ, 1.9F * scale);
            put(map, CarPart.ALTERNATOR, side, bay + 0.4F * tall, engineZ, 1.7F * scale);
            put(map, CarPart.OIL_FILTER, side, bay, engineZ, 1.5F * scale);
        }

        // Under the floor, between the axles — and still under the body where
        // there is only the one axle, as on a short trailer.
        float under = Math.max(1.8F, Math.min(bottom, axleY) - 1.5F);
        float tail = rear + 1.0F;
        put(map, CarPart.STEERING_RACK, 0.0F, under, frontAxle, 2.2F * scale);
        put(map, CarPart.CLUTCH, 0.0F, under, Math.max(tail, frontAxle - 0.15F * wheelbase), 2.4F * scale);
        put(map, CarPart.TRANSMISSION, 0.0F, under, Math.max(tail, frontAxle - 0.3F * wheelbase), 2.8F * scale);
        put(map, CarPart.DRIVESHAFT, 0.0F, under, Math.max(tail, frontAxle - 0.6F * wheelbase), 2.4F * scale);
        put(map, CarPart.EXHAUST, 0.3F * halfWidth, under, rear + 3.0F, 2.0F * scale);

        corner(map, wheel);
        return map;
    }

    /**
     * Where a part stands in an engine bay: across it from -1 (the bay's -x)
     * to 1, along it from 0 at the windscreen to 1 at the grille, its size as
     * a share of the bay's width, the lowest and highest pixel of its item
     * model, and how far it is turned. The renderer stands each part's model
     * here and the aiming box is put round the same spot, so the two cannot
     * drift apart.
     */
    public record BaySpot(CarPart part, float across, float along, float size,
                          float bottom, float top, float turn) {

        /** How big the part's model is drawn in this bay, in blocks a model block. */
        public float scale(net.minecraft.world.phys.AABB bay) {
            float width = (float) (bay.maxX - bay.minX) / 16.0F;
            float deep = (float) (bay.maxY - bay.minY) / 16.0F * 0.92F;
            float tall = (this.top - this.bottom) / 16.0F;
            return Math.min(width * this.size, deep / tall);
        }

        /** The middle of the drawn part, in bay pixels, and the half-size of the box round it. */
        float[] centre(net.minecraft.world.phys.AABB bay) {
            float scale = this.scale(bay);
            float x = (float) ((bay.minX + bay.maxX) / 2.0 + this.across * (bay.maxX - bay.minX) / 2.0);
            float z = (float) (bay.minZ + this.along * (bay.maxZ - bay.minZ));
            float y = (float) bay.minY + scale * (this.top - this.bottom) / 2.0F;
            float half = Math.max(1.2F, Math.min(3.5F, scale * 16.0F * 0.32F));
            return new float[]{x, y, z, half};
        }
    }

    /**
     * Laid out as under a real bonnet: the engine across the middle, the
     * radiator upright behind the grille, the battery and the air box either
     * side behind it, the alternator and the oil filter low on the engine's
     * flanks, the plugs along its back.
     */
    public static final List<BaySpot> BAY = List.of(
            new BaySpot(CarPart.ENGINE, 0.0F, 0.48F, 0.40F, 2.2F, 13.8F, 0.0F),
            new BaySpot(CarPart.RADIATOR, 0.0F, 0.92F, 0.62F, 1.4F, 14.6F, 0.0F),
            new BaySpot(CarPart.BATTERY, -0.7F, 0.2F, 0.24F, 3.25F, 12.75F, 90.0F),
            new BaySpot(CarPart.AIR_FILTER, 0.7F, 0.22F, 0.24F, 5.3F, 10.7F, 0.0F),
            new BaySpot(CarPart.ALTERNATOR, 0.5F, 0.66F, 0.18F, 1.8F, 14.2F, 0.0F),
            new BaySpot(CarPart.OIL_FILTER, -0.5F, 0.66F, 0.14F, 3.95F, 12.05F, 0.0F),
            new BaySpot(CarPart.SPARK_PLUGS, 0.0F, 0.16F, 0.26F, 3.5F, 12.5F, 0.0F));

    /** A tyre, and the brakes and strut behind it: one at every wheel, not only the first. */
    private static boolean atEveryCorner(CarPart part) {
        return part == CarPart.WHEELS || part == CarPart.BRAKE_PADS
                || part == CarPart.BRAKE_DISCS || part == CarPart.SUSPENSION;
    }

    /**
     * Every place you can get at each part: {@code primary}'s one spot for
     * most, and for the wheel and what sits behind it, the same spot at every
     * wheel the vehicle has, so a tyre comes off whichever side you stand.
     */
    public static Map<CarPart, List<CarPart.Anchor>> everywhere(VehicleDefinition definition,
                                                              Map<CarPart, CarPart.Anchor> primary) {
        Map<CarPart, List<CarPart.Anchor>> all = new EnumMap<>(CarPart.class);
        primary.forEach((part, anchor) -> all.computeIfAbsent(part, p -> new ArrayList<>()).add(anchor));
        // The plain map (no definition, or one with no body): its wheels are
        // the one it has mirrored to the other three corners.
        if (definition == null || definition.wheels().isEmpty()
                || CarPart.ENGINE.anchor().equals(primary.get(CarPart.ENGINE))) {
            for (CarPart part : CarPart.values()) {
                CarPart.Anchor a = primary.get(part);
                if (a == null || !atEveryCorner(part)) {
                    continue;
                }
                for (float[] sign : new float[][]{{-1, 1}, {1, -1}, {-1, -1}}) {
                    add(all, part, new CarPart.Anchor(a.x() * sign[0], a.y(), a.z() * sign[1], a.size()));
                }
            }
            return all;
        }
        for (VehicleDefinition.Wheel wheel : definition.wheels()) {
            Map<CarPart, CarPart.Anchor> here = new EnumMap<>(CarPart.class);
            corner(here, wheel);
            here.forEach((part, anchor) -> add(all, part, anchor));
        }
        return all;
    }

    private static void add(Map<CarPart, List<CarPart.Anchor>> all, CarPart part, CarPart.Anchor anchor) {
        List<CarPart.Anchor> list = all.computeIfAbsent(part, p -> new ArrayList<>());
        if (!list.contains(anchor)) {
            list.add(anchor);
        }
    }

    /** The wheel, and inboard of it the brakes and the strut above them. */
    private static void corner(Map<CarPart, CarPart.Anchor> map, VehicleDefinition.Wheel wheel) {
        float x = wheel.at().x();
        float y = wheel.at().y();
        float z = wheel.at().z();
        float radius = wheel.radius();
        // Towards the middle of the car; a wheel on the middle line has its
        // brakes on the right.
        float inward = x < -0.5F ? 1.0F : x > 0.5F ? -1.0F : 1.0F;
        float tyre = Math.min(radius, 7.0F) * 0.85F;
        // Clear of the tyre's box too, which on a tall, narrow tyre reaches
        // further in than the tyre does.
        float inboard = x + inward * Math.max(wheel.width() / 2.0F + 2.2F, tyre - 1.0F);
        put(map, CarPart.WHEELS, x, y, z, tyre);
        put(map, CarPart.BRAKE_PADS, inboard, y + 0.4F * radius, z, 1.6F);
        put(map, CarPart.BRAKE_DISCS, inboard, y - 0.4F * radius, z, 1.6F);
        put(map, CarPart.SUSPENSION, inboard, y + radius + 2.0F, z, 1.8F);
    }

    /** A motorbike: the engine hangs between the wheels, the rest round it. */
    private static void motorcycle(Map<CarPart, CarPart.Anchor> map, VehicleDefinition.Wheel wheel,
                                   float frontAxle, float rearAxle) {
        float radius = wheel.radius();
        float axleY = wheel.at().y();
        float middle = (frontAxle + rearAxle) / 2.0F;
        float engineY = axleY + 0.6F * radius + 3.0F;
        put(map, CarPart.ENGINE, 0.0F, engineY, middle, 3.0F);
        put(map, CarPart.SPARK_PLUGS, 0.0F, engineY + 5.0F, middle + 1.0F, 1.5F);
        put(map, CarPart.AIR_FILTER, 0.0F, engineY + 5.0F, middle - 4.0F, 1.6F);
        put(map, CarPart.RADIATOR, 0.0F, engineY + 1.0F, middle + 5.2F, 1.8F);
        put(map, CarPart.BATTERY, 0.0F, engineY + 4.0F, middle - 8.0F, 1.6F);
        put(map, CarPart.ALTERNATOR, 4.5F, engineY, middle, 1.4F);
        put(map, CarPart.CLUTCH, -4.5F, engineY, middle, 1.4F);
        put(map, CarPart.OIL_FILTER, 0.0F, engineY - 4.5F, middle + 1.0F, 1.4F);
        put(map, CarPart.TRANSMISSION, 0.0F, engineY - 1.0F, middle - 5.5F, 2.0F);
        put(map, CarPart.DRIVESHAFT, -2.5F, axleY, (rearAxle + middle - 5.0F) / 2.0F, 1.5F);
        put(map, CarPart.STEERING_RACK, 0.0F, axleY + 2.2F * radius, frontAxle - 2.0F, 1.6F);
        put(map, CarPart.EXHAUST, 3.5F, axleY, rearAxle + radius + 1.0F, 1.6F);
        corner(map, wheel);
    }

    /** How high the vehicle stands, in blocks, glass included: where a plate over its roof goes. */
    public static float roof(VehicleDefinition definition) {
        if (definition == null) {
            return 1.5F;
        }
        float top = 0.0F;
        for (VehicleDefinition.Box box : definition.shape()) {
            if (!"cargo".equals(box.color())) {
                top = Math.max(top, Math.max(box.from().y(), box.to().y()));
            }
        }
        return top > 0.0F ? top / 16.0F : 1.5F;
    }

    /** How high the bottom of the bodywork is, in blocks: what a lift's arms lift by. */
    public static float sill(VehicleDefinition definition) {
        if (definition == null) {
            return 0.3F;
        }
        float bottom = Float.MAX_VALUE;
        for (VehicleDefinition.Box box : definition.shape()) {
            if (solid(box)) {
                bottom = Math.min(bottom, Math.min(box.from().y(), box.to().y()));
            }
        }
        return bottom == Float.MAX_VALUE ? 0.3F : Math.max(0.0F, bottom / 16.0F);
    }

    /** How high the bodywork reaches over this stretch of the car's length, along its middle. */
    private static float heightAt(VehicleDefinition definition, float fromZ, float toZ) {
        float top = 0.0F;
        for (VehicleDefinition.Box box : definition.shape()) {
            if (!solid(box)) {
                continue;
            }
            float minZ = Math.min(box.from().z(), box.to().z());
            float maxZ = Math.max(box.from().z(), box.to().z());
            float minX = Math.min(box.from().x(), box.to().x());
            float maxX = Math.max(box.from().x(), box.to().x());
            if (maxZ < fromZ || minZ > toZ || minX > 2.0F || maxX < -2.0F) {
                continue;
            }
            top = Math.max(top, Math.max(box.from().y(), box.to().y()));
        }
        return top;
    }

    /** Bodywork, not glass or a load. */
    private static boolean solid(VehicleDefinition.Box box) {
        String colour = box.color();
        return !"cargo".equals(colour) && !(colour.startsWith("#") && colour.length() == 9);
    }

    /** One part, from definition pixels to an anchor in blocks the model's way round. */
    private static void put(Map<CarPart, CarPart.Anchor> map, CarPart part,
                            float x, float y, float z, float half) {
        map.put(part, new CarPart.Anchor(-x / 16.0F, y / 16.0F, -z / 16.0F, half / 16.0F));
    }
}
