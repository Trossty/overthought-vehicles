package me.trossty.car.entity;

/**
 * The parts that sit in more than one place on a car, each place with its own
 * wear: a tyre, a caliper, a pad, a disc, a spring and a ball joint at every
 * wheel, a plug, a coil and an injector at every cylinder.
 *
 * <p>The car still answers {@link CarEntity#getCondition(CarPart)} for the set
 * as a whole, the average of its places, so everything that asks how good the
 * brakes are goes on working. What the places add is the difference between
 * them: the front pads go first because the front does most of the stopping,
 * the side a car is knocked out of true on eats its tyres, one seized caliper
 * drags its own corner, and one fouled plug, tired coil or sticking injector
 * misfires in its own cylinder.
 *
 * <p>The server keeps each place to the decimal, since wear comes off a few
 * thousandths at a time. Clients get them in half-per-cent steps, eight to a
 * {@code long}: enough for a reader or a pull to the left, and a car's worth
 * costs nine numbers on the wire.
 */
public final class PartPositions {

    /** The most places a part can have: an eight-wheeled lorry, a V8. */
    public static final int MAX = 8;

    /** The parts that have places, in the order their numbers are kept. */
    private static final CarPart[] SPREAD = {
            CarPart.WHEELS, CarPart.BRAKE_PADS, CarPart.BRAKE_DISCS, CarPart.SUSPENSION, CarPart.SPARK_PLUGS,
            CarPart.IGNITION_COILS, CarPart.INJECTORS, CarPart.BRAKE_CALIPERS, CarPart.BALL_JOINTS};

    public static final int PARTS = SPREAD.length;

    /** Every place of every part brand new. */
    public static final long ALL_NEW = pack(filled(CarPart.MAX_CONDITION), MAX);

    private PartPositions() {
    }

    /** Where this part's places are kept, or -1 when it is one thing in one place. */
    public static int slot(CarPart part) {
        for (int i = 0; i < SPREAD.length; i++) {
            if (SPREAD[i] == part) {
                return i;
            }
        }
        return -1;
    }

    public static boolean isSpread(CarPart part) {
        return slot(part) >= 0;
    }

    /** The places that sit at the wheels, rather than in the cylinders. */
    public static boolean atWheels(CarPart part) {
        return isSpread(part) && !inCylinders(part);
    }

    /** A plug, a coil and an injector to every cylinder. */
    public static boolean inCylinders(CarPart part) {
        return part == CarPart.SPARK_PLUGS || part == CarPart.IGNITION_COILS || part == CarPart.INJECTORS;
    }

    public static CarPart part(int slot) {
        return SPREAD[slot];
    }

    /** The first {@code count} places, as a byte each: condition in half-per-cent steps. */
    public static long pack(float[] conditions, int count) {
        long packed = 0L;
        for (int i = 0; i < Math.min(count, MAX); i++) {
            long half = Math.round(Math.max(0.0F, Math.min(CarPart.MAX_CONDITION, conditions[i])) * 2.0F);
            if (conditions[i] > 0.0F) {
                // A part worn to the bone is still on: it must not read as gone.
                half = Math.max(1L, half);
            }
            packed |= (half & 0xFFL) << (i * 8);
        }
        return packed;
    }

    public static float unpack(long packed, int place) {
        return ((packed >>> (place * 8)) & 0xFFL) / 2.0F;
    }

    public static float[] filled(float condition) {
        float[] all = new float[MAX];
        java.util.Arrays.fill(all, condition);
        return all;
    }

    /**
     * A number in -1..1 that depends only on the car, the part and the place:
     * no two calipers grip quite alike and no two plugs burn quite alike, and
     * that never changes for a given car.
     */
    public static float bias(java.util.UUID car, CarPart part, int place) {
        long mixed = car.getLeastSignificantBits() ^ car.getMostSignificantBits()
                + (part.ordinal() * 31L + place) * 0x9E3779B97F4A7C15L;
        mixed = (mixed ^ (mixed >>> 33)) * 0xFF51AFD7ED558CCDL;
        mixed ^= mixed >>> 33;
        return ((mixed & 0xFFFF) / 32767.5F) - 1.0F;
    }
}
