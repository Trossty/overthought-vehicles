package me.trossty.car.client.render;

import me.trossty.car.block.VehicleLiftBlock;
import me.trossty.car.entity.CarEntity;
import me.trossty.car.entity.CarPart;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.network.chat.Component;
import net.minecraft.Util;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.Vec3;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * The workshop as a place rather than a menu.
 *
 * <p>When a car is up on its ramp, every part you could get at right now is
 * marked where it really is on the car — the radiator at the nose, the gearbox
 * under the floor, the brakes inside the front wheel — as a pane of light with
 * its name hanging over it in the air, the way the game marks things out in
 * creative mode. The one under your crosshair lights up, and right clicking it
 * takes it off or puts one on. The list window is still there for reading the
 * whole car at a glance, but the spanner work happens on the car.
 *
 * <p>Only reachable parts are marked, which is the teardown order made visible:
 * the engine is not something you can point at until the plugs and the airbox
 * are off, and until then there is nothing there to aim at.
 *
 * <p>Nothing lights up unless the game's own crosshair is on that car. The
 * marks can be seen from across the garage, but a part only lights up when a
 * right click would really reach it, so a lit mark always means "this will
 * work" rather than "this is the one I would have picked".
 */
public final class CarServiceOverlay {

    /** How far away the marks are drawn at all. */
    private static final double RANGE = 8.0;

    /** How far the aiming ray is cast, past anything within arm's reach. */
    private static final double REACH = 9.0;

    /** How far over its own mark a name plate floats. */
    private static final double LABEL_LIFT = 0.22;

    /** Leaders are drawn thinner than an outline: they are not the subject. */

    // Create marks the thing you can set on a block with a frame and a label,
    // not with a pane of light, and these are its colours: the label white of
    // its value boxes, the red it uses for something that will not work, and
    // plain white for the one under the crosshair.
    private static final int LINE_FITTED = 0x99EDEDED;
    private static final int LINE_ABSENT = 0xCCFF5D6C;
    private static final int LINE_AIMED = 0xFFFFFFFF;

    /**
     * How far the frame under the crosshair starts out from its mark, and how
     * long it takes to close in. Create's outlines chase the thing they are
     * on rather than snapping to it, and that small settle is most of why its
     * boxes read as picked rather than merely recoloured.
     */
    private static final double CHASE_GROW = 0.09;
    private static final double CHASE_MS = 220.0;

    private static final int GOOD = 0xFF8CE06A;
    private static final int FAIR = 0xFFE8C04A;
    private static final int BAD = 0xFFE06C6C;
    private static final int ABSENT = 0xFFD08C8C;

    /** What the crosshair is on right now, recomputed every frame. */
    private static CarEntity aimedCar;
    private static CarPart aimedPart;

    /** The mark the frame is closing in on, and when it started. */
    private static CarPart chased;
    private static long chasedSince;

    /** When the crosshair came to rest on the part it is on now. */
    public static long aimedSince() {
        return chasedSince;
    }

    /** How far out the frame still is from the mark it is settling onto. */
    private static double chase(CarPart part) {
        long now = Util.getMillis();
        if (part != chased) {
            chased = part;
            chasedSince = now;
        }
        double left = 1.0 - Math.min(1.0, (now - chasedSince) / CHASE_MS);
        return CHASE_GROW * left * left * left;
    }

    private CarServiceOverlay() {
    }

    /** The car under the crosshair, for the HUD and for the click. */
    public static CarEntity aimedCar() {
        return aimedCar != null && !aimedCar.isRemoved() ? aimedCar : null;
    }

    /** The part under the crosshair, or null if none is in reach. */
    public static CarPart aimedPart() {
        return aimedCar() == null ? null : aimedPart;
    }

    /** Draws the marks and works out which one is being aimed at. */
    public static void render(WorldRenderContext context) {
        aimedCar = null;
        aimedPart = null;

        Minecraft client = Minecraft.getInstance();
        LocalPlayer player = client.player;
        if (player == null || client.level == null || client.options.hideGui
                || player.isSpectator()) {
            return;
        }

        CarEntity car = nearestCar(player);
        // Not the one you are sitting in: the dashboard is the instrument for
        // that, and a plate hanging over the bonnet at speed is just in the way.
        if (car == null || player.getVehicle() == car) {
            return;
        }
        // Off a ramp there is no spanner work to be done, but there is still
        // a car to read: how beaten up it is and what is in the tank. Those
        // two come up whenever you look at one, so that walking up to your car
        // tells you what a menu used to have to.
        boolean servicing = VehicleLiftBlock.isServicing(player.level(), car);

        float tickProgress = client.getTimer().getGameTimeDeltaPartialTick(true);

        // The game's own pick decides whether anything can be aimed at. It is
        // the same pick the right click will use, so a lit mark is a promise.
        boolean inReach = client.hitResult instanceof EntityHitResult hit
                && hit.getEntity() == car;
        if (!servicing && !inReach) {
            return;
        }

        // Pick first, draw second: the mark being aimed at is drawn in another
        // colour, and which one that is cannot be known until all are known.
        // The car's own hit box is a square round its middle, and most of a
        // car is outside it — the nose, the front wheels, the boot — so the
        // marks are aimed at by themselves, as far as the first solid block.
        CarPart aiming = null;
        AABB aimingBox = null;
        if (servicing) {
            Vec3 eye = player.getEyePosition();
            double reach = REACH;
            if (client.hitResult instanceof net.minecraft.world.phys.BlockHitResult block
                    && block.getType() == net.minecraft.world.phys.HitResult.Type.BLOCK) {
                reach = Math.min(reach, block.getLocation().distanceTo(eye));
            }
            Vec3 end = eye.add(player.getViewVector(tickProgress).scale(reach));
            double nearest = Double.MAX_VALUE;
            for (CarPart part : CarPart.values()) {
                if (!car.canReach(part)) {
                    continue;
                }
                for (AABB box : boxesOf(car, part, tickProgress)) {
                    Optional<Vec3> hit = box.clip(eye, end);
                    if (hit.isPresent()) {
                        double distance = hit.get().distanceToSqr(eye);
                        if (distance < nearest) {
                            nearest = distance;
                            aiming = part;
                            aimingBox = box;
                        }
                    }
                }
            }
        }

        if (aiming == null) {
            chased = null;
        }

        MultiBufferSource consumers = context.consumers();
        if (consumers != null) {
            Vec3 camera = context.camera();
            PoseStack matrices = context.matrices();
            for (CarPart part : CarPart.values()) {
                if (!servicing || !car.canReach(part)) {
                    continue;
                }
                boolean fitted = car.isFitted(part);
                for (AABB box : boxesOf(car, part, tickProgress)) {
                    boolean lit = part == aiming && box.equals(aimingBox);
                    AABB frame = lit ? box.inflate(chase(part)) : box;
                    context.outline(consumers.getBuffer(RenderType.lines()), frame,
                            lit ? LINE_AIMED : (fitted ? LINE_FITTED : LINE_ABSENT));
                }
            }
            // Each plate hangs on a short leader from its own mark; the car's
            // renderer writes the plate itself (see labelsFor).
            Vec3 pos = car.getPosition(tickProgress);
            for (PartLabel label : labelsFor(car, tickProgress)) {
                if (label.mark() != null) {
                    drawLeader(matrices, consumers.getBuffer(RenderType.lines()),
                            pos.add(label.mark()), pos.add(label.offset()), camera, label.line());
                }
            }
        }

        aimedCar = car;
        aimedPart = aiming;
    }

    /** Every box you can aim at for one part, in world space: one per wheel for a tyre. */
    public static List<AABB> boxesOf(CarEntity car, CarPart part, float tickProgress) {
        Vec3 pos = car.getPosition(tickProgress);
        float yaw = car.getViewYRot(tickProgress);
        List<AABB> boxes = new ArrayList<>();
        for (CarPart.Anchor anchor : car.anchorsOf(part)) {
            boxes.add(anchor.box(pos, yaw));
        }
        return boxes;
    }

    /** The box you aim at for one part, in world space. */
    public static AABB boxOf(CarEntity car, CarPart part, float tickProgress) {
        return car.anchorOf(part).box(
                car.getPosition(tickProgress), car.getViewYRot(tickProgress));
    }

    /** One plate hanging over a car: where, relative to the car, and what it says. */
    public record PartLabel(Vec3 offset, Component text, int colour, Vec3 mark, int line) {
        PartLabel(Vec3 offset, Component text, int colour) {
            this(offset, text, colour, null, 0);
        }
    }

    /** Plates are written this much smaller than a player's name over their head. */
    public static final float PLATE_SCALE = 0.0125F;

    /** How far apart two plates have to be, up and down, not to overlap. */
    private static final double PLATE_STEP = 0.17;

    /**
     * The plates over this car this frame, as offsets from where it is drawn,
     * for its renderer to write into the world beside it. Written there, in
     * the world, they stay put on the car however the camera bobs or the
     * field of view breathes, which a picture projected onto the screen
     * afterwards never quite did.
     *
     * <p>The part being aimed at has none: the box beside the crosshair says
     * more about it than its name. Nor has a part that is fitted and sound —
     * its green mark says as much — so what is written over the car is what
     * needs doing. Plates that would sit on top of each other are stacked,
     * by where they are on the car and not where they fall on the screen, so
     * the stack does not shuffle as you walk round.
     */
    public static List<PartLabel> labelsFor(CarEntity car, float tickProgress) {
        Minecraft client = Minecraft.getInstance();
        LocalPlayer player = client.player;
        if (player == null || client.level == null || client.options.hideGui
                || player.isSpectator() || player.getVehicle() == car || nearestCar(player) != car) {
            return List.of();
        }
        boolean servicing = VehicleLiftBlock.isServicing(player.level(), car);
        boolean inReach = client.hitResult instanceof EntityHitResult hit && hit.getEntity() == car;
        if (!servicing && !inReach) {
            return List.of();
        }
        Vec3 pos = car.getPosition(tickProgress);
        float yaw = car.getViewYRot(tickProgress);
        List<PartLabel> labels = new ArrayList<>();
        readings(car, pos, yaw, labels);
        if (servicing) {
            CarPart aimed = aimedCar() == car ? aimedPart : null;
            for (CarPart part : CarPart.values()) {
                if (part == aimed || !car.canReach(part)) {
                    continue;
                }
                int colour = colourOf(car, part);
                if (colour == GOOD) {
                    continue;
                }
                AABB box = boxOf(car, part, tickProgress);
                Vec3 mark = new Vec3(box.getCenter().x, box.maxY, box.getCenter().z).subtract(pos);
                Vec3 at = mark.add(0.0, LABEL_LIFT, 0.0);
                Component name = nameOf(car, part);
                double width = widthOf(name);
                while (crowded(labels, at, width)) {
                    at = at.add(0.0, PLATE_STEP, 0.0);
                }
                labels.add(new PartLabel(at, name, colour, mark,
                        car.isFitted(part) ? LINE_FITTED : LINE_ABSENT));
            }
        }
        return labels;
    }

    /** Adds a plate, lifted clear of any it would overlap. */
    private static void stack(List<PartLabel> labels, PartLabel label) {
        Vec3 at = label.offset();
        double width = widthOf(label.text());
        while (crowded(labels, at, width)) {
            at = at.add(0.0, PLATE_STEP, 0.0);
        }
        labels.add(new PartLabel(at, label.text(), label.colour(), label.mark(), label.line()));
    }

    /** How wide a plate is, in blocks. */
    private static double widthOf(Component text) {
        return Minecraft.getInstance().font.width(text) * PLATE_SCALE + 0.1;
    }

    /**
     * Whether a plate here could overlap one already placed, seen from
     * anywhere: two plates closer together than half their widths added up
     * can always be lined up by walking round, so they are kept apart in
     * height instead.
     */
    private static boolean crowded(List<PartLabel> labels, Vec3 at, double width) {
        for (PartLabel other : labels) {
            Vec3 o = other.offset();
            double across = Math.hypot(o.x - at.x, o.z - at.z);
            if (across < (width + widthOf(other.text())) / 2.0 && Math.abs(o.y - at.y) < PLATE_STEP) {
                return true;
            }
        }
        return false;
    }

    /**
     * The two things about a car that are not a part: how beaten up the body
     * is, and what is in the tank. They hang where you would look for them —
     * over the front wing, and over the back one where the filler is — so
     * that the whole of what the service window used to tell you is out here
     * on the car.
     */
    private static void readings(CarEntity car, Vec3 pos, float yaw, List<PartLabel> labels) {
        // Over this car's own roof, clear of the plates over its parts: the
        // body and what it will do over the front half, the tank over the back.
        CarPart.Anchor wheel = car.anchorOf(CarPart.WHEELS);
        float y = car.roofHeight() + 0.3F;
        float front = wheel.z() * 0.45F;
        CarPart.Anchor bodyAt = new CarPart.Anchor(0.0F, y, front, 0.01F);
        CarPart.Anchor specAt = new CarPart.Anchor(0.0F, y + (float) PLATE_STEP, front, 0.01F);
        CarPart.Anchor tankAt = new CarPart.Anchor(0.0F, y, -front, 0.01F);

        float body = car.getHealthFraction();
        stack(labels, new PartLabel(bodyAt.box(pos, yaw).getCenter().subtract(pos),
                Component.translatable("gui.car.plate_body", Math.round(body * 100.0F)),
                body >= 0.75F ? GOOD : (body > 0.35F ? FAIR : BAD)));

        if (car instanceof me.trossty.car.entity.TrailerEntity trailer) {
            // No engine and no tank: what it is carrying instead.
            stack(labels, new PartLabel(specAt.box(pos, yaw).getCenter().subtract(pos),
                    Component.translatable("gui.car.plate_load", Math.round(trailer.getLoadFraction() * 100.0F)),
                    0xFFE8E8E8));
            return;
        }
        // What the car as fitted will actually do, so that a part going on
        // or coming off shows as a number changing in front of you.
        stack(labels, new PartLabel(specAt.box(pos, yaw).getCenter().subtract(pos),
                Component.translatable("gui.car.plate_spec",
                        String.format(java.util.Locale.ROOT, "%.1f", car.getMaxSpeed() * 20.0F),
                        String.format(java.util.Locale.ROOT, "%.2f",
                                car.throttleStep() / CarEntity.ACCELERATION),
                        String.format(java.util.Locale.ROOT, "%.2f", car.thirstFactor())),
                0xFFE8E8E8));

        float fuel = car.getFuelFraction();
        Component tank = car.isOverheating()
                // One line, and the thing that will stop you first goes on it.
                ? Component.translatable("message.car.overheating")
                : car.isLeaking() && car.hasFuel()
                ? Component.translatable("gui.car.plate_leaking", Math.round(fuel * 100.0F))
                : car.hasWrongFuel()
                ? Component.translatable("gui.car.plate_wrong_fuel",
                        Component.translatable("fuel.car." + car.getTankFuel().key() + ".short"))
                : Component.translatable("gui.car.plate_fuel_kind",
                        Component.translatable("fuel.car." + car.getTankFuel().key() + ".short"),
                        Math.round(fuel * 100.0F));
        stack(labels, new PartLabel(tankAt.box(pos, yaw).getCenter().subtract(pos), tank,
                car.isOverheating() || car.hasWrongFuel() || car.isLeaking() ? BAD
                        : (fuel >= 0.3F ? GOOD : (fuel > 0.1F ? FAIR : BAD))));
    }

    /** What the plate over a mark says: the part, and how worn it is. */
    private static Component nameOf(CarEntity car, CarPart part) {
        Component name = Component.translatable("part.car." + part.key());
        if (!car.isFitted(part)) {
            return Component.translatable("gui.car.hologram_absent", name);
        }
        if (me.trossty.car.entity.CarSetup.variantsOf(part) > 1) {
            // Which engine, which tyres: the plate says what is actually there.
            name = me.trossty.car.item.VehiclePartItem.nameOf(part, car.getVariant(part));
            if (part == CarPart.ENGINE && car.hasTurbo()) {
                name = Component.translatable("gui.car.with_turbo", name);
            }
        }
        return Component.translatable("gui.car.hologram_worn", name,
                Math.round(car.getConditionFraction(part) * 100.0F));
    }

    private static int colourOf(CarEntity car, CarPart part) {
        if (!car.isFitted(part)) {
            return ABSENT;
        }
        float fraction = car.getConditionFraction(part);
        return fraction >= 0.75F ? GOOD : (fraction > 0.35F ? FAIR : BAD);
    }

    /**
     * A name plate hanging in the air over a mark, turned to face the camera
     * the way a player's own name is.
     */
    /** The leader line from a mark up to its plate. */
    private static void drawLeader(PoseStack matrices, VertexConsumer lines,
                                   Vec3 from, Vec3 to, Vec3 camera, int colour) {
        PoseStack.Pose entry = matrices.last();
        lines.addVertex(entry, (float) (from.x - camera.x), (float) (from.y - camera.y),
                        (float) (from.z - camera.z))
                .setColor(colour).setNormal(entry, 0.0F, 1.0F, 0.0F);
        lines.addVertex(entry, (float) (to.x - camera.x), (float) (to.y - camera.y),
                        (float) (to.z - camera.z))
                .setColor(colour).setNormal(entry, 0.0F, 1.0F, 0.0F);
    }

    /** The nearest car in sight, or null. */
    private static CarEntity nearestCar(LocalPlayer player) {
        CarEntity best = null;
        double nearest = RANGE * RANGE;
        for (CarEntity car : player.level().getEntitiesOfClass(
                CarEntity.class, player.getBoundingBox().inflate(RANGE), each -> true)) {
            double distance = car.distanceToSqr(player);
            if (distance < nearest) {
                best = car;
                nearest = distance;
            }
        }
        return best;
    }
}
