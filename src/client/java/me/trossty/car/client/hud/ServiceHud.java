package me.trossty.car.client.hud;

import me.trossty.car.client.render.CarServiceOverlay;
import me.trossty.car.client.render.WorldLabels;
import me.trossty.car.entity.CarEntity;
import me.trossty.car.entity.CarPart;
import me.trossty.car.entity.Workshop;
import me.trossty.car.workbench.BenchWork;
import me.trossty.car.workbench.VehicleRecipe;
import net.minecraft.client.gui.LayeredDraw;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.Util;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector4f;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * What is written on the screen while you are working at a car or a bench by
 * hand, in the manner of the Create mod, because that is the mod this one is
 * meant to sit beside in a garage.
 *
 * <p>Two things. Every mark in the world has its name over it, written the way
 * Create labels a value box: light letters with a dark edge and nothing behind
 * them. And whatever is under the crosshair gets what Create's goggles give a
 * machine — a tooltip box slid in beside the crosshair, with the thing itself
 * drawn at its head, saying what state it is in and what the click will do.
 */
public class ServiceHud implements LayeredDraw.Layer {

    /** The edge Create gives its value-box labels. */
    private static final int LABEL_EDGE = 0xFF333333;
    /** Create's accent gold, for the tooltip's heading. */
    private static final int ACCENT = 0xFFF1DD79;
    private static final int GREY = 0xFFAAAAAA;
    private static final int HINT = 0xFF808080;
    private static final int GOOD = 0xFF8CE06A;
    private static final int FAIR = 0xFFE8C04A;
    private static final int BAD = 0xFFFF5D6C;

    // The goggle tooltip's own defaults: a vanilla tooltip at three quarters
    // strength, standing twenty pixels off the crosshair.
    private static final int BOX_BACKGROUND = 0xB4100010;
    private static final int BOX_BORDER_TOP = 0x505000FF;
    private static final int BOX_BORDER_BOTTOM = 0x5028007F;
    private static final int BOX_OFFSET = 20;
    /** It slides the last eight pixels into place over twenty-four ticks. */
    private static final double SLIDE_MS = 1200.0;
    private static final int SLIDE_PIXELS = 8;

    private static final int ICON = 16;
    private static final int LINE = 10;

    /** A name plate and the clear air under it, in screen pixels. */
    private static final int PLATE_HEIGHT = 12;

    /** One line of the tooltip. */
    private record Row(Component text, int colour) {
    }

    /**
     * The box beside the crosshair.
     *
     * @param icon  the thing it is about, drawn at its head
     * @param title its name
     * @param rows  what there is to say about it, the last being what a click does
     * @param since when the crosshair came to rest on it, for the slide
     */
    private record Tooltip(ItemStack icon, Component title, List<Row> rows, long since) {
    }

    @Override
    public void render(GuiGraphics context, DeltaTracker tickCounter) {
        Minecraft client = Minecraft.getInstance();
        if (client.player == null || client.options.hideGui) {
            return;
        }

        Tooltip tooltip = partTooltip(client);
        if (tooltip == null) {
            this.drawLabels(context, client, null);
            return;
        }

        int width = ICON + 4 + client.font.width(tooltip.title());
        for (Row row : tooltip.rows()) {
            width = Math.max(width, client.font.width(row.text()));
        }
        int height = ICON + 2 + tooltip.rows().size() * LINE - 2;

        // Beside the crosshair rather than under it, where the goggles put
        // theirs: the thing and its label are at the crosshair already, and a
        // box on top of them would hide what it is describing.
        int x = Math.min(context.guiWidth() / 2 + BOX_OFFSET,
                context.guiWidth() - width - 20);
        int y = Math.min(context.guiHeight() / 2 - height / 2,
                context.guiHeight() - height - 20);

        // The labels go down first, and keep out from under the box: it is
        // see-through, and a name showing through it reads as part of it.
        this.drawLabels(context, client,
                new int[] {x - 5, y - 5, x + width + 5 + SLIDE_PIXELS, y + height + 5});

        double left = 1.0 - Math.min(1.0,
                (Util.getMillis() - tooltip.since()) / SLIDE_MS);
        context.pose().pushPose();
        context.pose().translate((float) (left * left * left * SLIDE_PIXELS), 0.0F, 0.0F);

        tooltipBox(context, x, y, width, height);
        // The thing itself at the head of the box, where the goggles draw
        // themselves: you are told about it by being shown it.
        context.renderItem(tooltip.icon(), x, y);
        context.drawString(client.font, tooltip.title(), x + ICON + 4, y + 4, ACCENT, true);
        int line = y + ICON + 2;
        for (Row row : tooltip.rows()) {
            context.drawString(client.font, row.text(), x, line, row.colour(), true);
            line += LINE;
        }

        context.pose().popPose();
    }

    /**
     * Each place of a part that has several, coloured as the condition row is:
     * "FL 82  FR 80  RL 91  RR 90", or a plug a cylinder, "1 90  2 88".
     */
    private static Component places(CarEntity car, CarPart part) {
        net.minecraft.network.chat.MutableComponent line = Component.empty();
        for (int place = 0; place < car.parts().placesOf(part); place++) {
            if (part == CarPart.WHEELS && !car.isTyreOn(place)) {
                continue;
            }
            if (!line.getSiblings().isEmpty()) {
                line.append("  ");
            }
            float fraction = car.parts().getCondition(part, place) / CarPart.MAX_CONDITION;
            int colour = fraction >= 0.75F ? GOOD : (fraction > 0.35F ? FAIR : BAD);
            line.append(Component.translatable("tooltip.overthought_vehicles.place", car.parts().placeShort(part, place),
                    Math.round(fraction * 100.0F)).withColor(colour & 0xFFFFFF));
        }
        return line;
    }

    /** The part of a car under the crosshair, or null if there is none. */
    private static Tooltip partTooltip(Minecraft client) {
        CarEntity car = CarServiceOverlay.aimedCar();
        CarPart part = CarServiceOverlay.aimedPart();
        if (car == null || part == null) {
            return null;
        }

        boolean fitted = car.parts().isFitted(part);
        ItemStack held = client.player.getMainHandItem();
        boolean offering = held.is(part.item());
        List<Row> rows = new ArrayList<>();
        CarPart hidden = fitted ? car.workshop().hiddenBy(part) : null;
        if (fitted && hidden != null) {
            rows.add(new Row(Component.translatable("gui.overthought_vehicles.status_hidden",
                    Component.translatable("part.overthought_vehicles." + hidden.key())), GREY));
        } else if (fitted) {
            float fraction = car.parts().getConditionFraction(part);
            rows.add(new Row(
                    Component.translatable("tooltip.overthought_vehicles.condition", Math.round(fraction * 100.0F)),
                    fraction >= 0.75F ? GOOD : (fraction > 0.35F ? FAIR : BAD)));
            if (car.parts().placesOf(part) > 1) {
                rows.add(new Row(places(car, part), GREY));
            }
        }
        if (fitted) {
            rows.add(new Row(
                    Component.translatable("quality.overthought_vehicles." + car.parts().getQuality(part).key()), GREY));
        } else {
            rows.add(new Row(Component.translatable("gui.overthought_vehicles.part_missing"), BAD));
        }
        if (offering) {
            rows.add(new Row(Component.translatable("gui.overthought_vehicles.spare",
                    Math.round(Workshop.conditionOf(held) / CarPart.MAX_CONDITION * 100.0F) + "%"), GOOD));
        }
        rows.add(new Row(Component.translatable(fitted
                        ? (offering ? "gui.overthought_vehicles.service_swap" : "gui.overthought_vehicles.service_remove")
                        : (offering ? "gui.overthought_vehicles.service_fit" : "gui.overthought_vehicles.service_hold")),
                HINT));
        return new Tooltip(new ItemStack(part.item()),
                Component.translatable("part.overthought_vehicles." + part.key()), rows,
                CarServiceOverlay.aimedSince());
    }

    /** A vanilla tooltip's frame, drawn by hand so that it can be see-through. */
    private static void tooltipBox(GuiGraphics context, int x, int y, int width, int height) {
        int x1 = x - 3;
        int y1 = y - 3;
        int x2 = x + width + 3;
        int y2 = y + height + 3;
        context.fill(x1, y1 - 1, x2, y1, BOX_BACKGROUND);
        context.fill(x1, y2, x2, y2 + 1, BOX_BACKGROUND);
        context.fill(x1 - 1, y1, x1, y2, BOX_BACKGROUND);
        context.fill(x2, y1, x2 + 1, y2, BOX_BACKGROUND);
        context.fill(x1, y1, x2, y2, BOX_BACKGROUND);
        context.fillGradient(x1, y1 + 1, x1 + 1, y2 - 1, BOX_BORDER_TOP, BOX_BORDER_BOTTOM);
        context.fillGradient(x2 - 1, y1 + 1, x2, y2 - 1, BOX_BORDER_TOP, BOX_BORDER_BOTTOM);
        context.fill(x1, y1, x2, y1 + 1, BOX_BORDER_TOP);
        context.fill(x1, y2 - 1, x2, y2, BOX_BORDER_BOTTOM);
    }

    /** Letters with a dark edge all round, readable over sky and bodywork alike. */
    private static void outlined(GuiGraphics context, Minecraft client,
                                 Component text, int x, int y, int colour) {
        context.drawString(client.font, text, x - 1, y, LABEL_EDGE, false);
        context.drawString(client.font, text, x + 1, y, LABEL_EDGE, false);
        context.drawString(client.font, text, x, y - 1, LABEL_EDGE, false);
        context.drawString(client.font, text, x, y + 1, LABEL_EDGE, false);
        context.drawString(client.font, text, x, y, colour, false);
    }

    /**
     * The labels hanging in the world, drawn flat on the screen at the point
     * each one projects to — see {@link WorldLabels} for why here and not out
     * there.
     *
     * @param box the tooltip's rectangle to keep clear of, or null if there is none
     */
    private void drawLabels(GuiGraphics context, Minecraft client, int[] box) {
        List<WorldLabels.Label> labels = WorldLabels.labels();
        if (labels.isEmpty()) {
            return;
        }

        int width = context.guiWidth();
        int height = context.guiHeight();
        // The game's own projection lives on the graphics card now, so the
        // labels are projected the long way round: the view matrix kept from
        // the world pass, then the field of view, which is all a perspective
        // projection is.
        double tangent = Math.tan(Math.toRadians(client.options.fov().get()) / 2.0);
        double aspect = (double) width / height;
        Vec3 camera = WorldLabels.camera();

        // Project first, then draw: where two labels land on top of each other
        // they have to be told apart, and that cannot be known one at a time.
        record Plate(int x, int y, WorldLabels.Label label) {
        }
        List<Plate> plates = new ArrayList<>();
        for (WorldLabels.Label label : labels) {
            Vector4f point = new Vector4f(
                    (float) (label.at().x - camera.x),
                    (float) (label.at().y - camera.y),
                    (float) (label.at().z - camera.z),
                    1.0F);
            WorldLabels.view().transform(point);
            if (point.z >= -0.05F) {
                // Behind the camera, or in it.
                continue;
            }
            double depth = -point.z;
            plates.add(new Plate(
                    (int) Math.round((point.x / depth / (tangent * aspect) * 0.5 + 0.5) * width),
                    (int) Math.round((0.5 - point.y / depth / tangent * 0.5) * height),
                    label));
        }

        // Nudge overlapping names apart, nearest the top first. Two parts a
        // hand's width apart on the car can be a pixel apart on the screen,
        // and two names written over each other are worse than one. Pictures
        // stay where they are: an item drawn off its button points at nothing.
        plates.sort(Comparator.comparingInt(Plate::y));
        List<Plate> placed = new ArrayList<>();
        for (Plate plate : plates) {
            int x = plate.x();
            int y = plate.y();
            if (plate.label().icon().isEmpty()) {
                int half = client.font.width(plate.label().text()) / 2;
                boolean moved = true;
                while (moved) {
                    moved = false;
                    for (Plate other : placed) {
                        if (!other.label().icon().isEmpty()) {
                            continue;
                        }
                        int otherHalf = client.font.width(other.label().text()) / 2;
                        if (Math.abs(y - other.y()) < PLATE_HEIGHT
                                && x - half < other.x() + otherHalf
                                && x + half > other.x() - otherHalf) {
                            y = other.y() + PLATE_HEIGHT;
                            moved = true;
                        }
                    }
                }
            }
            placed.add(new Plate(x, y, plate.label()));
        }

        for (Plate plate : placed) {
            WorldLabels.Label label = plate.label();
            int x = plate.x();
            int y = plate.y();
            if (label.lit()) {
                // The box beside the crosshair is this one's label now, and
                // says a good deal more than its name.
                continue;
            }
            boolean pictured = !label.icon().isEmpty();
            int half = pictured ? ICON / 2 : client.font.width(label.text()) / 2;
            int top = pictured ? y - ICON : y - 1;
            if (box != null && x + half > box[0] && x - half < box[2]
                    && y + 9 > box[1] && top < box[3]) {
                continue;
            }
            if (pictured) {
                // Standing on the point, not centred on it: the point is the
                // top of a button and the thing should look set down there.
                context.renderItem(label.icon(), x - ICON / 2, y - ICON);
            } else {
                // No box behind it: Create writes its labels straight onto the
                // world with a dark edge round the letters.
                outlined(context, client, label.text(), x - half, y - 8, label.colour());
            }
        }
    }
}
