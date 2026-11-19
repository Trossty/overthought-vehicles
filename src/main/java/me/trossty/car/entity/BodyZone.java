package me.trossty.car.entity;

import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

/**
 * The four sides of a body a knock can land on. Each keeps its own dents and
 * shelters its own parts: the radiator sits behind the front, the exhaust and
 * the tank at the back, and the wheels at either end and on either side, so
 * where a car was hit decides what it now needs.
 */
public enum BodyZone {
    FRONT("front"),
    REAR("rear"),
    LEFT("left"),
    RIGHT("right");

    /** How much longer than wide a car is, near enough, when telling an end from a side. */
    public static final float LENGTH_TO_WIDTH = 2.0F;

    private final String key;

    BodyZone(String key) {
        this.key = key;
    }

    public String key() {
        return this.key;
    }

    public Component displayName() {
        return Component.translatable("zone.car." + this.key);
    }

    /**
     * The side of a car turned to {@code yaw} that faces {@code direction},
     * a world-space vector from its middle: towards a point, or the way it
     * was travelling when it hit something.
     *
     * <p>{@code aspect} is how much longer than wide to take the car to be.
     * A point beside the front wheel is nearer the front than the side in
     * plain distance, but it is the side that faces it, so a point is judged
     * with {@link #LENGTH_TO_WIDTH}; a direction of travel is judged as it is.
     *
     * @return null when the direction has no sideways part at all, straight
     *         down or straight up
     */
    public static @Nullable BodyZone facing(float yaw, Vec3 direction, float aspect) {
        float yawRad = yaw * ((float) Math.PI / 180.0F);
        // The vehicle's own frame: +z ahead, +x to its left.
        double ahead = -Mth.sin(yawRad) * direction.x + Mth.cos(yawRad) * direction.z;
        double left = Mth.cos(yawRad) * direction.x + Mth.sin(yawRad) * direction.z;
        if (Math.abs(ahead) < 1.0E-4 && Math.abs(left) < 1.0E-4) {
            return null;
        }
        if (Math.abs(ahead) / aspect >= Math.abs(left)) {
            return ahead >= 0.0 ? FRONT : REAR;
        }
        return left >= 0.0 ? LEFT : RIGHT;
    }

    /** The side of the car facing a point in the world, or null for one right above or below it. */
    public static @Nullable BodyZone facing(CarEntity car, Vec3 point) {
        return facing(car.getYRot(), point.subtract(car.position()), LENGTH_TO_WIDTH);
    }

    /**
     * Whether a wheel sits in this zone: the front pair at the front, the
     * left wheels on the left. A middle axle is only on the sides, and a
     * wheel on the middle line, like a motorcycle's, on both of them.
     */
    boolean holds(CarParts parts, int wheel) {
        return switch (this) {
            case FRONT -> parts.axle(wheel) > 0;
            case REAR -> parts.axle(wheel) < 0;
            case LEFT -> parts.lateral(wheel) <= 0;
            case RIGHT -> parts.lateral(wheel) >= 0;
        };
    }

    /** Which way a knock here bends the wheels out of true: towards the side that took it. */
    float pullSide() {
        return switch (this) {
            case LEFT -> -1.0F;
            case RIGHT -> 1.0F;
            default -> 0.0F;
        };
    }
}
