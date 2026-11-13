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

        put(map, CarPart.RADIATOR, 0.0F, bay + 0.5F * tall, front - 2.5F, 2.0F * scale);
        put(map, CarPart.ENGINE, 0.0F, bay + 0.4F * tall, engineZ, 3.2F * scale);
        put(map, CarPart.SPARK_PLUGS, 0.0F, bay + 0.88F * tall, engineZ, 1.7F * scale);
        put(map, CarPart.AIR_FILTER, 0.0F, bay + 0.8F * tall, frontAxle - 0.25F * nose, 1.9F * scale);
        put(map, CarPart.BATTERY, side, bay + 0.8F * tall, engineZ, 1.9F * scale);
        put(map, CarPart.ALTERNATOR, side, bay + 0.4F * tall, engineZ, 1.7F * scale);
        put(map, CarPart.OIL_FILTER, side, bay, engineZ, 1.5F * scale);

        // Under the floor, between the axles.
        float under = Math.max(1.8F, Math.min(bottom, axleY) - 1.5F);
        put(map, CarPart.STEERING_RACK, 0.0F, under, frontAxle, 2.2F * scale);
        put(map, CarPart.CLUTCH, 0.0F, under, frontAxle - 0.15F * wheelbase, 2.4F * scale);
        put(map, CarPart.TRANSMISSION, 0.0F, under, frontAxle - 0.3F * wheelbase, 2.8F * scale);
        put(map, CarPart.DRIVESHAFT, 0.0F, under, frontAxle - 0.6F * wheelbase, 2.4F * scale);
        put(map, CarPart.EXHAUST, 0.3F * halfWidth, under, rear + 3.0F, 2.0F * scale);

        corner(map, wheel);
        return map;
    }

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
        float inboard = x + inward * (wheel.width() / 2.0F + 2.2F);
        put(map, CarPart.WHEELS, x, y, z, Math.min(radius, 7.0F) * 0.85F);
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
