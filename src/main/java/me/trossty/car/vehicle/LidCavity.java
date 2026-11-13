package me.trossty.car.vehicle;

import net.minecraft.world.phys.AABB;
import org.jetbrains.annotations.Nullable;
import org.joml.Vector3f;

import java.util.ArrayList;
import java.util.List;

/**
 * The hollow under a bonnet or a boot lid. A shape is built of solid boxes,
 * so under each lid there is just more car; this cuts a tub out of it, with
 * a floor and four walls, for the engine and the luggage to sit in.
 */
public final class LidCavity {
    /** How much bodywork is left round the hole, in pixels. */
    private static final double WALL = 1.5;
    /** How deep the hole goes at most, below the lid. */
    private static final double DEPTH = 8.0;
    /** How much floor is left under it at least. */
    private static final double FLOOR = 1.5;
    /** The tub's own skin, and the hair it stands clear of what was cut. */
    private static final float SKIN = 0.4F;
    private static final double CLEAR = 0.05;
    /** What the floor of a hollow is, and the inside of a boot: black, as carpet and the underside. */
    private static final String INSIDE = "#2A2A2C";
    /**
     * The inner wings and the bulkhead of an engine bay: the body colour, a
     * shade down, as they are pressed from the same steel and painted with it.
     */
    private static final String BAY_WALL = "paint_dark";
    /** Black plastic: the scuttle panel, the fuse box, the caps. */
    private static final String TRIM = "#1C1C1E";

    private LidCavity() {
    }

    /**
     * The hollow under a lid, in pixels with +z to the front, its bottom the
     * floor things stand on; null with no such lid, or too little car under
     * it to hollow out.
     */
    public static @Nullable AABB of(VehicleDefinition definition, String lid) {
        AABB cover = definition.lidBounds(lid);
        if (cover == null) {
            return null;
        }
        double minX = cover.minX + WALL;
        double maxX = cover.maxX - WALL;
        double minZ = cover.minZ + WALL;
        double maxZ = cover.maxZ - WALL;
        // The bottom of the bodywork that comes up to the lid: the hole stops
        // short of it, so there is no seeing through to the road.
        double bottom = Double.MAX_VALUE;
        for (VehicleDefinition.Box box : definition.shape()) {
            if (!box.opens().isEmpty() || !plain(box)) {
                continue;
            }
            double x = (box.from().x() + box.to().x()) / 2.0;
            double z = (box.from().z() + box.to().z()) / 2.0;
            double top = Math.max(box.from().y(), box.to().y());
            if (x >= cover.minX && x <= cover.maxX && z >= cover.minZ && z <= cover.maxZ
                    && top >= cover.minY - 1.0) {
                bottom = Math.min(bottom, Math.min(box.from().y(), box.to().y()));
            }
        }
        if (bottom == Double.MAX_VALUE || minX >= maxX || minZ >= maxZ) {
            return null;
        }
        double floor = Math.max(cover.minY - DEPTH, bottom + FLOOR);
        if (floor > cover.minY - 2.0) {
            return null;
        }
        return new AABB(minX, floor, minZ, maxX, cover.minY, maxZ);
    }

    /**
     * The shape with a tub under each lid: whatever was in the hollow cut
     * away round it, and a floor and walls of its own put in, so nothing is
     * open to the road whatever was there before.
     */
    public static List<VehicleDefinition.Box> carve(VehicleDefinition definition) {
        List<VehicleDefinition.Box> shape = definition.shape();
        for (String lid : List.of(VehicleDefinition.Box.BONNET, VehicleDefinition.Box.BOOT)) {
            AABB hollow = of(definition, lid);
            if (hollow == null) {
                continue;
            }
            // Cut up to the top of the lid, so nothing under it is left
            // poking through; the tub stands a hair inside the cut.
            AABB cut = new AABB(hollow.minX - CLEAR, hollow.minY - CLEAR, hollow.minZ - CLEAR,
                    hollow.maxX + CLEAR, definition.lidBounds(lid).maxY, hollow.maxZ + CLEAR);
            List<VehicleDefinition.Box> carved = new ArrayList<>();
            for (VehicleDefinition.Box box : shape) {
                if (!box.opens().isEmpty() || !plain(box)) {
                    carved.add(box);
                } else {
                    subtract(box, cut, carved);
                }
            }
            float x0 = (float) hollow.minX;
            float x1 = (float) hollow.maxX;
            float y0 = (float) hollow.minY;
            float y1 = (float) hollow.maxY;
            float z0 = (float) hollow.minZ;
            float z1 = (float) hollow.maxZ;
            String wall = VehicleDefinition.Box.BONNET.equals(lid) ? BAY_WALL : INSIDE;
            carved.add(tub(x0, y0 - SKIN, z0, x1, y0, z1, INSIDE));
            carved.add(tub(x0, y0, z0, x0 + SKIN, y1, z1, wall));
            carved.add(tub(x1 - SKIN, y0, z0, x1, y1, z1, wall));
            carved.add(tub(x0 + SKIN, y0, z0, x1 - SKIN, y1, z0 + SKIN, wall));
            carved.add(tub(x0 + SKIN, y0, z1 - SKIN, x1 - SKIN, y1, z1, wall));
            if (VehicleDefinition.Box.BONNET.equals(lid)) {
                fixtures(hollow, carved);
            }
            shape = carved;
        }
        return shape;
    }

    /** A box that can be cut: square to the car, not glass, not turning with anything. */
    private static boolean plain(VehicleDefinition.Box box) {
        return box.tilt() == 0.0F && box.roll() == 0.0F && box.yaw() == 0.0F
                && !box.steers() && !box.spins() && !glass(box.color());
    }

    private static boolean glass(String colour) {
        if (!colour.startsWith("#") || colour.length() != 9) {
            return false;
        }
        try {
            return Integer.parseInt(colour.substring(1, 3), 16) < 0xFF;
        } catch (NumberFormatException ignored) {
            return false;
        }
    }

    private static VehicleDefinition.Box tub(float x0, float y0, float z0, float x1, float y1, float z1,
                                             String colour) {
        return new VehicleDefinition.Box(new Vector3f(x0, y0, z0), new Vector3f(x1, y1, z1),
                colour, 0.0F, false, 0.0F, false, 0.0F, "");
    }

    /**
     * What every engine bay has whatever engine is in it, laid out round the
     * parts the renderer stands in the middle: a strut tower on each inner
     * wing, the scuttle along the bulkhead, the slam panel over the radiator,
     * the coolant and the washer bottles in the front corners, the brake
     * master cylinder and the fuse box against the bulkhead. All in shares of
     * the bay, so a small car's are small.
     */
    private static void fixtures(AABB bay, List<VehicleDefinition.Box> into) {
        float x0 = (float) bay.minX + SKIN;
        float x1 = (float) bay.maxX - SKIN;
        float y0 = (float) bay.minY;
        float y1 = (float) bay.maxY;
        float z0 = (float) bay.minZ + SKIN;
        float z1 = (float) bay.maxZ - SKIN;
        float width = x1 - x0;
        float length = z1 - z0;
        float deep = y1 - y0;
        // Strut towers: a column on each inner wing, about halfway along,
        // capped with the top mount a little proud of it.
        float towerWide = width * 0.08F;
        float towerLong = length * 0.15F;
        float towerZ = z0 + length * 0.5F;
        float towerTop = y0 + deep * 0.72F;
        for (int side = -1; side <= 1; side += 2) {
            float inner = side < 0 ? x0 + towerWide : x1 - towerWide;
            float outer = side < 0 ? x0 : x1;
            into.add(tub(Math.min(inner, outer), y0, towerZ - towerLong / 2.0F,
                    Math.max(inner, outer), towerTop, towerZ + towerLong / 2.0F, BAY_WALL));
            float cap = towerWide * 0.6F;
            float capX = (inner + outer) / 2.0F;
            into.add(tub(capX - cap / 2.0F, towerTop, towerZ - cap / 2.0F,
                    capX + cap / 2.0F, towerTop + Math.min(0.6F, deep * 0.08F), towerZ + cap / 2.0F, TRIM));
        }
        // The scuttle: black plastic across the top of the bulkhead, where
        // the wipers sit.
        float scuttle = Math.min(1.6F, length * 0.08F);
        into.add(tub(x0, y1 - Math.min(1.2F, deep * 0.15F), z0, x1, y1 - 0.1F, z0 + scuttle, TRIM));
        // The slam panel: the cross member over the radiator the bonnet
        // latches to.
        into.add(tub(x0, y1 - Math.min(0.8F, deep * 0.1F), z1 - Math.min(1.0F, length * 0.05F),
                x1, y1 - 0.1F, z1, BAY_WALL));
        // Coolant bottle, front left; washer bottle, front right: each a
        // squat tank with a cap on top.
        float tankWide = width * 0.11F;
        float tankLong = length * 0.17F;
        float tankZ = z0 + length * 0.78F;
        bottle(x0 + towerWide * 0.2F, tankWide, tankZ, tankLong, y0, deep * 0.62F, "#E6E2CF", "#2E8B57", into);
        bottle(x1 - towerWide * 0.2F - tankWide, tankWide, tankZ, tankLong, y0, deep * 0.58F,
                "#3C78C8", "#1E4E8C", into);
        // Against the bulkhead: the brake master cylinder on the driver's
        // side, the fuse box on the other.
        float servo = width * 0.13F;
        into.add(tub(x0 + towerWide * 1.2F, y0 + deep * 0.35F, z0, x0 + towerWide * 1.2F + servo,
                y0 + deep * 0.7F, z0 + length * 0.12F, "#3A3A3E"));
        into.add(tub(x0 + towerWide * 1.2F + servo * 0.3F, y0 + deep * 0.7F, z0 + length * 0.03F,
                x0 + towerWide * 1.2F + servo * 0.7F, y0 + deep * 0.78F, z0 + length * 0.09F, "#D8B23A"));
        float fuse = width * 0.14F;
        into.add(tub(x1 - towerWide * 1.2F - fuse, y0 + deep * 0.3F, z0,
                x1 - towerWide * 1.2F, y0 + deep * 0.65F, z0 + length * 0.1F, TRIM));
    }

    /** A plastic tank standing on the floor, with a cap on top in another colour. */
    private static void bottle(float x, float wide, float midZ, float along, float floor, float tall,
                               String body, String cap, List<VehicleDefinition.Box> into) {
        into.add(tub(x, floor, midZ - along / 2.0F, x + wide, floor + tall, midZ + along / 2.0F, body));
        float capWide = Math.min(wide, along) * 0.45F;
        float mid = x + wide / 2.0F;
        into.add(tub(mid - capWide / 2.0F, floor + tall, midZ - capWide / 2.0F,
                mid + capWide / 2.0F, floor + tall + Math.max(0.3F, tall * 0.1F), midZ + capWide / 2.0F, cap));
    }

    /** What is left of a box with the cut taken out of it: up to six boxes. */
    private static void subtract(VehicleDefinition.Box box, AABB cut, List<VehicleDefinition.Box> into) {
        double ax0 = Math.min(box.from().x(), box.to().x()), ax1 = Math.max(box.from().x(), box.to().x());
        double ay0 = Math.min(box.from().y(), box.to().y()), ay1 = Math.max(box.from().y(), box.to().y());
        double az0 = Math.min(box.from().z(), box.to().z()), az1 = Math.max(box.from().z(), box.to().z());
        if (ax1 <= cut.minX || ax0 >= cut.maxX || ay1 <= cut.minY || ay0 >= cut.maxY
                || az1 <= cut.minZ || az0 >= cut.maxZ) {
            into.add(box);
            return;
        }
        // Below and above the cut, full size; then either side of it in x
        // between those; then in front of and behind it in z, inside that.
        double y0 = Math.max(ay0, cut.minY), y1 = Math.min(ay1, cut.maxY);
        double x0 = Math.max(ax0, cut.minX), x1 = Math.min(ax1, cut.maxX);
        piece(box, ax0, ay0, az0, ax1, y0, az1, into);
        piece(box, ax0, y1, az0, ax1, ay1, az1, into);
        piece(box, ax0, y0, az0, x0, y1, az1, into);
        piece(box, x1, y0, az0, ax1, y1, az1, into);
        piece(box, x0, y0, az0, x1, y1, Math.max(az0, cut.minZ), into);
        piece(box, x0, y0, Math.min(az1, cut.maxZ), x1, y1, az1, into);
    }

    private static void piece(VehicleDefinition.Box like, double x0, double y0, double z0,
                              double x1, double y1, double z1, List<VehicleDefinition.Box> into) {
        if (x1 - x0 > 1.0E-3 && y1 - y0 > 1.0E-3 && z1 - z0 > 1.0E-3) {
            into.add(new VehicleDefinition.Box(new Vector3f((float) x0, (float) y0, (float) z0),
                    new Vector3f((float) x1, (float) y1, (float) z1),
                    like.color(), 0.0F, false, 0.0F, false, 0.0F, ""));
        }
    }
}
