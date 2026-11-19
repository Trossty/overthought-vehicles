package me.trossty.car.client.screen;

import net.minecraft.Util;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.world.item.ItemStack;

/**
 * The look every window of the mod shares, drawn from rectangles so the mod
 * still ships no GUI texture. A window is a grey panel like vanilla's with a
 * dark nameplate across the top, screwed on at both ends and underlined in the
 * colour of the machine's family. A machine's working parts sit on a recessed
 * steel faceplate beneath it, each with its name stamped under it: sight-glass
 * tanks with their scale, a stack of LEDs for the energy, pipes and cables
 * whose chevrons run while something flows, a dial, a lamp, and a green LCD
 * for the one line of figures a machine has to say.
 *
 * <p>Text that may not fit is cut with an ellipsis rather than squeezed or
 * run into its neighbour, so nothing on these windows overlaps.
 */
final class GuiStyle {

    static final int PANEL = 0xFFC6C6C6;
    static final int PANEL_LIGHT = 0xFFFFFFFF;
    static final int PANEL_SHADOW = 0xFF555555;
    static final int OUTLINE = 0xFF161616;
    static final int SLOT_BODY = 0xFF8B8B8B;
    static final int SLOT_DARK = 0xFF373737;
    static final int TEXT = 0xFF404040;

    static final int HEADER = 0xFF2B323C;
    static final int HEADER_LIGHT = 0xFF4A5564;
    static final int HEADER_DARK = 0xFF1C2128;
    /** The workshop's orange, the rule under every window that is not a machine's. */
    static final int ACCENT = 0xFFE8781E;
    static final int TITLE = 0xFFF2F2F2;
    static final int TITLE_SOFT = 0xFFAEB6C2;
    /** The nameplate's height; the title sits in it at {@link #TITLE_X}, {@link #TITLE_Y}. */
    static final int HEADER_HEIGHT = 13;
    static final int TITLE_X = 10;
    static final int TITLE_Y = 3;

    static final int PLATE = 0xFF444B54;
    static final int CAPTION = 0xFFD2D9E1;
    static final int CAPTION_SHADOW = 0xFF1E2329;

    static final int STEEL = 0xFF8E96A0;
    static final int STEEL_LIGHT = 0xFFC3CAD2;
    static final int STEEL_DARK = 0xFF5B636C;
    static final int GLASS_BACK = 0xFF15191E;

    static final int READOUT = 0xFF0E1813;
    static final int READOUT_TEXT = 0xFF7CF08A;
    static final int READOUT_HEIGHT = 11;

    static final int LAMP_ON = 0xFF4BD457;
    static final int LAMP_OFF = 0xFFB0372B;

    static final int LED_GREEN = 0xFF46D85A;
    static final int LED_AMBER = 0xFFF2B33A;
    static final int LED_RED = 0xFFE5483A;
    static final int LED_OFF = 0xFF242A31;

    static final int BLUEPRINT = 0xFF1F4F8F;
    static final int BLUEPRINT_GRID = 0x18FFFFFF;
    static final int BLUEPRINT_MAJOR = 0x30FFFFFF;
    static final int BLUEPRINT_INK = 0xA0E4EEFA;
    static final int PIN = 0xFFD23B2E;
    static final int TRACK = 0xFF22272D;
    static final int COUNT_OK = 0xFFFFFFFF;
    static final int COUNT_MISSING = 0xFFFF6A5A;

    static final int DIAL_FACE = 0xFFE6E0CF;
    static final int NEEDLE = 0xFFC8302A;

    private GuiStyle() {
    }

    /** Game ticks on a clock that wraps every few minutes, to animate by. */
    static float ticks() {
        return (Util.getMillis() % 360_000L) / 50.0F;
    }

    // ---- the window -------------------------------------------------------------------

    /** The window in the workshop's colours. */
    static void frame(GuiGraphics context, int left, int top, int width, int height) {
        frame(context, left, top, width, height, ACCENT);
    }

    /** The window itself: a rounded outline, the panel and its bevel, and the nameplate. */
    static void frame(GuiGraphics context, int left, int top, int width, int height, int accent) {
        int right = left + width;
        int bottom = top + height;
        context.fill(left, top - 1, right, top, OUTLINE);
        context.fill(left, bottom, right, bottom + 1, OUTLINE);
        context.fill(left - 1, top, left, bottom, OUTLINE);
        context.fill(right, top, right + 1, bottom, OUTLINE);
        context.fill(left, top, right, bottom, PANEL);
        context.fill(left, top, left + 1, bottom, PANEL_LIGHT);
        context.fill(left + 1, top, left + 2, bottom - 1, PANEL_LIGHT);
        context.fill(left + 1, bottom - 1, right, bottom, PANEL_SHADOW);
        context.fill(right - 1, top, right, bottom, PANEL_SHADOW);
        context.fill(right - 2, top + 1, right - 1, bottom - 1, 0xFFA0A0A0);

        context.fill(left, top, right, top + HEADER_HEIGHT, HEADER);
        context.fill(left, top, right, top + 1, HEADER_LIGHT);
        context.fill(left, top + HEADER_HEIGHT - 1, right, top + HEADER_HEIGHT, HEADER_DARK);
        context.fill(left, top + HEADER_HEIGHT, right, top + HEADER_HEIGHT + 1, accent);
        context.fill(left + 2, top + HEADER_HEIGHT + 1, right - 2, top + HEADER_HEIGHT + 2, 0x30000000);
        screw(context, left + 3, top + 5);
        screw(context, right - 6, top + 5);
    }

    /** A slotted screw head, three pixels across, lit from the top left. */
    static void screw(GuiGraphics context, int x, int y) {
        context.fill(x, y, x + 3, y + 3, 0xFF7E868F);
        dot(context, x, y, 0xFFCDD4DC);
        dot(context, x + 2, y, 0xFF3A4048);
        dot(context, x + 1, y + 1, 0xFF3A4048);
        dot(context, x, y + 2, 0xFF3A4048);
        dot(context, x + 2, y + 2, 0xFF5A626B);
    }

    /** A vanilla slot: sunken, dark above and left, light below and right. */
    static void slot(GuiGraphics context, int x, int y) {
        context.fill(x, y, x + 18, y + 18, SLOT_BODY);
        context.fill(x, y, x + 18, y + 1, SLOT_DARK);
        context.fill(x, y, x + 1, y + 18, SLOT_DARK);
        context.fill(x + 17, y + 1, x + 18, y + 18, PANEL_LIGHT);
        context.fill(x + 1, y + 17, x + 18, y + 18, PANEL_LIGHT);
    }

    /**
     * What belongs in an empty slot, faded into it, so a slot says what it is
     * for without a word. Call it for the slot's item position (inside the frame).
     */
    static void ghost(GuiGraphics context, ItemStack stack, int x, int y) {
        context.renderFakeItem(stack, x, y);
        context.pose().pushPose();
        context.pose().translate(0.0F, 0.0F, 200.0F);
        context.fill(x, y, x + 16, y + 16, 0x988B8B8B);
        context.pose().popPose();
    }

    /** A sunken box filled with {@code inside}, as a well for something to sit in. */
    static void inset(GuiGraphics context, int x, int y, int width, int height, int inside) {
        context.fill(x - 1, y - 1, x + width + 1, y + height + 1, PANEL_LIGHT);
        context.fill(x - 1, y - 1, x + width, y + height, SLOT_DARK);
        context.fill(x, y, x + width, y + height, inside);
    }

    /**
     * The recessed steel faceplate a machine's working parts sit on, a screw
     * in each corner and a faint brushed grain.
     */
    static void plate(GuiGraphics context, int x, int y, int width, int height) {
        inset(context, x, y, width, height, PLATE);
        context.fill(x, y, x + width, y + 1, 0xFF353B42);
        for (int row = y + 2; row < y + height; row += 3) {
            context.fill(x, row, x + width, row + 1, 0x08FFFFFF);
        }
        screw(context, x + 2, y + 2);
        screw(context, x + width - 5, y + 2);
        screw(context, x + 2, y + height - 5);
        screw(context, x + width - 5, y + height - 5);
    }

    // ---- words ------------------------------------------------------------------------

    /** The title on the nameplate, cut to leave {@code room} pixels. */
    static void title(GuiGraphics context, Font font, Component title, int room) {
        FormattedCharSequence line = fit(font, title, room);
        context.drawString(font, line, TITLE_X, TITLE_Y + 1, HEADER_DARK, false);
        context.drawString(font, line, TITLE_X, TITLE_Y, TITLE, false);
    }

    /** A name stamped into the faceplate, centred on {@code centre}. */
    static void caption(GuiGraphics context, Font font, Component text, int centre, int y, int room) {
        FormattedCharSequence line = fit(font, text, room);
        int x = centre - font.width(line) / 2;
        context.drawString(font, line, x, y + 1, CAPTION_SHADOW, false);
        context.drawString(font, line, x, y, CAPTION, false);
    }

    /**
     * A lamp and a word at the right of the nameplate, ending at {@code right}:
     * green and glowing while the machine runs, a dull red while it stands.
     *
     * @return where the lamp starts, so the title can stop short of it
     */
    static int lamp(GuiGraphics context, Font font, boolean on, int right) {
        Component state = Component.translatable(on ? "gui.car.machine.running" : "gui.car.machine.idle");
        int x = right - font.width(state);
        int lx = x - 9;
        int ly = TITLE_Y;
        if (on) {
            context.fill(lx, ly, lx + 7, ly + 7, 0x284BD457);
            context.fill(lx + 1, ly - 1, lx + 6, ly + 8, 0x284BD457);
        }
        context.fill(lx + 1, ly + 1, lx + 6, ly + 6, OUTLINE);
        context.fill(lx + 2, ly, lx + 5, ly + 7, OUTLINE);
        context.fill(lx + 2, ly + 1, lx + 5, ly + 6, on ? LAMP_ON : 0xFF7A2620);
        context.fill(lx + 1, ly + 2, lx + 6, ly + 5, on ? LAMP_ON : 0xFF7A2620);
        dot(context, lx + 2, ly + 2, on ? 0xFFD2F8D6 : 0xFFB4554C);
        context.drawString(font, state, x, TITLE_Y + 1, HEADER_DARK, false);
        context.drawString(font, state, x, TITLE_Y, on ? TITLE : TITLE_SOFT, false);
        return lx;
    }

    /** A green LCD in a sunken bezel with one line of figures centred in it. */
    static void readout(GuiGraphics context, Font font, Component text, int x, int y, int width) {
        readout(context, font, text, x, y, width, READOUT_TEXT);
    }

    /** The LCD with its figures in {@code colour}, red or amber for a warning. */
    static void readout(GuiGraphics context, Font font, Component text, int x, int y, int width, int colour) {
        inset(context, x, y, width, READOUT_HEIGHT, READOUT);
        context.fill(x, y, x + width, y + 1, 0xFF1C2A22);
        FormattedCharSequence line = fit(font, text, width - 6);
        int tx = x + (width - font.width(line)) / 2;
        context.drawString(font, line, tx, y + 3, scale(colour, 0.25F), false);
        context.drawString(font, line, tx, y + 2, colour, false);
        context.fill(x + 1, y + 1, x + width - 1, y + 2, 0x14FFFFFF);
    }

    /** The text as it is if it fits, else cut with an ellipsis. */
    static FormattedCharSequence fit(Font font, Component text, int room) {
        if (font.width(text) <= room) {
            return text.getVisualOrderText();
        }
        String dots = "…";
        String cut = font.plainSubstrByWidth(text.getString(), Math.max(0, room - font.width(dots)));
        return Component.literal(cut.stripTrailing() + dots).withStyle(text.getStyle()).getVisualOrderText();
    }

    // ---- gauges -----------------------------------------------------------------------

    /**
     * A sight-glass tank: a steel casing with bolted caps round a glass that
     * shows the liquid, its surface catching the light, a scale down the side
     * and, while the machine works, bubbles rising through it.
     */
    static void tank(GuiGraphics context, int x, int y, int width, int height, float fill,
                     int body, int light, boolean busy) {
        casing(context, x, y, width, height);
        for (int bolt : new int[]{x + 2, x + width - 3}) {
            dot(context, bolt, y + 1, STEEL_DARK);
            dot(context, bolt, y + height - 2, STEEL_DARK);
        }
        int gx = x + 2;
        int gy = y + 3;
        int gw = width - 4;
        int gh = height - 6;
        context.fill(gx - 1, gy - 1, gx + gw + 1, gy + gh + 1, STEEL_DARK);
        context.fill(gx, gy, gx + gw, gy + gh, GLASS_BACK);
        int filled = Math.round(clamp(fill) * gh);
        if (filled > 0) {
            int surface = gy + gh - filled;
            context.fill(gx, surface, gx + gw, gy + gh, body);
            context.fill(gx + gw - 2, surface, gx + gw, gy + gh, darker(body));
            context.fill(gx, surface, gx + gw, surface + 1, light);
            if (busy && filled > 3) {
                float time = ticks();
                for (int bubble = 0; bubble < 3; bubble++) {
                    float rise = (time * 0.7F + bubble * filled / 3.0F) % (filled - 1);
                    int by = gy + gh - 1 - (int) rise;
                    int bx = gx + 2 + (bubble * 4 + (int) (rise / 5)) % Math.max(1, gw - 4);
                    dot(context, bx, by, light);
                }
            }
        }
        for (int quarter = 1; quarter < 4; quarter++) {
            int tick = gy + gh - quarter * gh / 4;
            context.fill(gx + gw - (quarter == 2 ? 5 : 3), tick, gx + gw, tick + 1, 0x90FFFFFF);
        }
        context.fill(gx + 1, gy + 1, gx + 2, gy + gh - 1, 0x40FFFFFF);
        context.fill(gx + 2, gy + 1, gx + 3, gy + gh / 3, 0x1CFFFFFF);
    }

    /**
     * A column of LEDs for stored energy, lit from the bottom: green when
     * there is plenty, amber when it runs down, red when it is nearly gone.
     */
    static void energy(GuiGraphics context, int x, int y, int width, int height, float fill) {
        casing(context, x, y, width, height);
        int ix = x + 2;
        int iy = y + 2;
        int iw = width - 4;
        int ih = height - 4;
        context.fill(ix - 1, iy - 1, ix + iw + 1, iy + ih + 1, STEEL_DARK);
        context.fill(ix, iy, ix + iw, iy + ih, 0xFF101316);
        int segments = (ih + 1) / 4;
        float level = clamp(fill);
        int lit = level <= 0.0F ? 0 : Math.max(1, Math.round(level * segments));
        int colour = level > 0.5F ? LED_GREEN : level > 0.2F ? LED_AMBER : LED_RED;
        for (int segment = 0; segment < segments; segment++) {
            int sy = iy + ih - (segment + 1) * 4 + 1;
            if (segment < lit) {
                context.fill(ix + 1, sy, ix + iw - 1, sy + 3, colour);
                context.fill(ix + 1, sy, ix + iw - 1, sy + 1, lighter(colour));
            } else {
                context.fill(ix + 1, sy, ix + iw - 1, sy + 3, LED_OFF);
            }
        }
        context.fill(ix, iy, ix + 1, iy + ih, 0x18FFFFFF);
    }

    private static final String[] BOLT = {
            "...##",
            "..##.",
            ".##..",
            "#####",
            "..##.",
            ".##..",
            "##...",
    };

    /**
     * A lightning bolt five pixels across, outlined, to mark an energy gauge
     * without a word.
     */
    static void bolt(GuiGraphics context, int x, int y) {
        for (int pass = 0; pass < 2; pass++) {
            for (int row = 0; row < BOLT.length; row++) {
                for (int column = 0; column < BOLT[row].length(); column++) {
                    if (BOLT[row].charAt(column) != '#') {
                        continue;
                    }
                    if (pass == 0) {
                        context.fill(x + column - 1, y + row - 1, x + column + 2, y + row + 2, CAPTION_SHADOW);
                    } else {
                        dot(context, x + column, y + row, row < 3 ? 0xFFFFE680 : 0xFFF2C230);
                    }
                }
            }
        }
    }

    /** A little picture drawn from rows of text, a pixel for every {@code #}. */
    static void pixels(GuiGraphics context, String[] rows, int x, int y, int colour) {
        for (int row = 0; row < rows.length; row++) {
            for (int column = 0; column < rows[row].length(); column++) {
                if (rows[row].charAt(column) == '#') {
                    dot(context, x + column, y + row, colour);
                }
            }
        }
    }

    /** A steel casing with a bevel and a dark outline, for a gauge to sit in. */
    static void casing(GuiGraphics context, int x, int y, int width, int height) {
        context.fill(x - 1, y - 1, x + width + 1, y + height + 1, OUTLINE);
        context.fill(x, y, x + width, y + height, STEEL);
        context.fill(x, y, x + width, y + 1, STEEL_LIGHT);
        context.fill(x, y, x + 1, y + height, STEEL_LIGHT);
        context.fill(x, y + height - 1, x + width, y + height, STEEL_DARK);
        context.fill(x + width - 1, y, x + width, y + height, STEEL_DARK);
    }

    /**
     * A pipe, eight high from {@code y}, flanged at both ends. What flows in
     * it fills it from the left as far as {@code progress}; while it moves,
     * chevrons run along it in a lighter shade.
     */
    static void flow(GuiGraphics context, int x, int y, int length, int colour, float progress, boolean moving) {
        context.fill(x, y, x + length, y + 8, OUTLINE);
        context.fill(x, y + 1, x + length, y + 2, STEEL_LIGHT);
        context.fill(x, y + 2, x + length, y + 6, 0xFF14181C);
        context.fill(x, y + 6, x + length, y + 7, STEEL_DARK);
        int run = Math.round(clamp(progress) * length);
        if (run > 0) {
            context.fill(x, y + 2, x + run, y + 6, darker(colour));
            if (moving) {
                int bright = lighter(colour);
                int shift = (int) (ticks() * 0.6F) % 7;
                for (int start = x - 7 + shift; start < x + run; start += 7) {
                    chevronColumn(context, start, y + 2, new int[]{0, 3}, x, x + run, bright);
                    chevronColumn(context, start + 1, y + 2, new int[]{0, 1, 2, 3}, x, x + run, bright);
                    chevronColumn(context, start + 2, y + 2, new int[]{1, 2}, x, x + run, bright);
                }
            }
        }
        flange(context, x + 1, y);
        flange(context, x + length - 3, y);
    }

    private static void chevronColumn(GuiGraphics context, int column, int top, int[] rows,
                                      int from, int to, int colour) {
        if (column < from || column >= to) {
            return;
        }
        for (int row : rows) {
            dot(context, column, top + row, colour);
        }
    }

    private static void flange(GuiGraphics context, int x, int y) {
        context.fill(x - 1, y - 2, x + 3, y + 10, OUTLINE);
        context.fill(x, y - 1, x + 2, y + 9, STEEL);
        context.fill(x, y - 1, x + 1, y + 9, STEEL_LIGHT);
    }

    /**
     * A round dial on a steel bezel: ticks over three quarters of a turn with
     * the last fifth in red, and a needle at {@code value} of the way round.
     */
    static void dial(GuiGraphics context, int cx, int cy, int radius, float value) {
        disc(context, cx, cy, radius + 1, OUTLINE);
        disc(context, cx, cy, radius, STEEL);
        disc(context, cx - 1, cy - 1, radius - 2, STEEL_LIGHT);
        disc(context, cx, cy, radius - 2, DIAL_FACE);
        for (int mark = 0; mark <= 10; mark++) {
            double angle = Math.toRadians(225 - mark * 27);
            int colour = mark >= 8 ? NEEDLE : 0xFF2E2E2E;
            int length = mark % 5 == 0 ? 3 : 2;
            for (int step = 0; step < length; step++) {
                double at = radius - 3 - step;
                dot(context, (int) Math.round(cx + Math.cos(angle) * at),
                        (int) Math.round(cy - Math.sin(angle) * at), colour);
            }
        }
        double angle = Math.toRadians(225 - clamp(value) * 270);
        line(context, cx, cy, cx + Math.cos(angle) * (radius - 4), cy - Math.sin(angle) * (radius - 4), NEEDLE);
        context.fill(cx - 1, cy - 1, cx + 2, cy + 2, 0xFF2A2A2A);
        dot(context, cx - 1, cy - 1, 0xFF7A7A7A);
        for (int step = 0; step < 4; step++) {
            dot(context, cx - radius / 2 - 2 + step, cy - radius / 2 - 1 - step / 2, 0x50FFFFFF);
        }
    }

    // ---- workshop paper and lists ------------------------------------------------------

    /**
     * A blueprint pinned to the panel: blue paper casting a shadow, a fine grid
     * with a heavier line every third square, a ruled border a pixel in and a
     * red drawing pin in each top corner.
     */
    static void blueprint(GuiGraphics context, int x, int y, int width, int height) {
        context.fill(x + 1, y + height, x + width + 1, y + height + 1, 0x60000000);
        context.fill(x + width, y + 1, x + width + 1, y + height, 0x60000000);
        context.fill(x, y, x + width, y + height, BLUEPRINT);
        for (int gx = 3; gx < width - 1; gx += 4) {
            context.fill(x + gx, y + 2, x + gx + 1, y + height - 2,
                    (gx + 1) % 12 == 0 ? BLUEPRINT_MAJOR : BLUEPRINT_GRID);
        }
        for (int gy = 3; gy < height - 1; gy += 4) {
            context.fill(x + 2, y + gy, x + width - 2, y + gy + 1,
                    (gy + 1) % 12 == 0 ? BLUEPRINT_MAJOR : BLUEPRINT_GRID);
        }
        context.fill(x + 1, y + 1, x + width - 1, y + 2, BLUEPRINT_INK);
        context.fill(x + 1, y + height - 2, x + width - 1, y + height - 1, BLUEPRINT_INK);
        context.fill(x + 1, y + 2, x + 2, y + height - 2, BLUEPRINT_INK);
        context.fill(x + width - 2, y + 2, x + width - 1, y + height - 2, BLUEPRINT_INK);
        context.fillGradient(x, y, x + width, y + height, 0x18FFFFFF, 0x00FFFFFF);
        pin(context, x + 1, y);
        pin(context, x + width - 4, y);
    }

    /** A round-headed drawing pin, three across, lit from the top left. */
    private static void pin(GuiGraphics context, int x, int y) {
        context.fill(x, y + 1, x + 3, y + 2, PIN);
        context.fill(x + 1, y, x + 2, y + 3, PIN);
        dot(context, x + 1, y + 1, lighter(PIN));
        dot(context, x + 2, y + 2, 0x60000000);
    }

    /**
     * One material on a blueprint: a dashed cell eighteen across with the item
     * in it and how many the job takes. A cell the player cannot fill yet is
     * washed red, so what is missing shows before the build key says so.
     */
    static void material(GuiGraphics context, Font font, ItemStack stack, int x, int y,
                         int countColour, boolean missing) {
        if (missing) {
            context.fill(x + 1, y + 1, x + 17, y + 17, 0x38FF4A3A);
        }
        int dash = missing ? 0xD0FF6A5A : BLUEPRINT_INK;
        for (int step = 0; step < 18; step += 2) {
            dot(context, x + step, y, dash);
            dot(context, x + step + 1, y + 17, dash);
            dot(context, x, y + step + 1, dash);
            dot(context, x + 17, y + step, dash);
        }
        context.renderItem(stack, x + 1, y + 1);
        String count = String.valueOf(stack.getCount());
        context.pose().pushPose();
        context.pose().translate(0.0F, 0.0F, 200.0F);
        context.drawString(font, count, x + 18 - font.width(count), y + 10, countColour);
        context.pose().popPose();
    }

    /** How tall a scrollbar's knob is when {@code visible} of {@code total} rows show. */
    static int knob(int trackHeight, int visible, int total) {
        return Math.max(8, trackHeight * visible / Math.max(visible, total));
    }

    /** The row a knob dragged to {@code along} pixels down its track puts at the top. */
    static int scrollFor(double along, int trackHeight, int knob, int max) {
        double travel = Math.max(1, trackHeight - knob);
        return Math.max(0, Math.min(max, (int) Math.round((along - knob / 2.0) / travel * max)));
    }

    /**
     * A steel knob with three grip ridges in a dark track. With nothing to
     * scroll ({@code max} 0) the knob fills the track and is dimmed, the way a
     * scrollbar says "that is all there is".
     */
    static void scrollbar(GuiGraphics context, int x, int y, int width, int height, int knob, int at, int max) {
        context.fill(x, y, x + width, y + height, TRACK);
        context.fill(x, y, x + width, y + 1, OUTLINE);
        int top = max == 0 ? y : y + (height - knob) * at / max;
        int body = max == 0 ? STEEL_DARK : STEEL;
        context.fillGradient(x, top, x + width, top + knob, lighter(body), darker(body));
        context.fill(x, top, x + 1, top + knob, max == 0 ? body : STEEL_LIGHT);
        context.fill(x + width - 1, top, x + width, top + knob, darker(body));
        context.fill(x, top + knob - 1, x + width, top + knob, darker(body));
        int grip = top + knob / 2 - 2;
        for (int ridge = 0; ridge < 3; ridge++) {
            context.fill(x + 1, grip + ridge * 2, x + width - 1, grip + ridge * 2 + 1, darker(darker(body)));
        }
    }

    /** A dark steel well for a list or a catalogue, with the plate's faint grain. */
    static void well(GuiGraphics context, int x, int y, int width, int height) {
        inset(context, x, y, width, height, PLATE);
        context.fill(x, y, x + width, y + 1, 0xFF353B42);
        for (int row = y + 2; row < y + height; row += 3) {
            context.fill(x, row, x + width, row + 1, 0x08FFFFFF);
        }
    }

    /** The orange ring and warm wash that mark the chosen entry of a catalogue. */
    static void chosen(GuiGraphics context, int x, int y, int width, int height) {
        context.fill(x - 1, y - 1, x + width + 1, y, ACCENT);
        context.fill(x - 1, y + height, x + width + 1, y + height + 1, ACCENT);
        context.fill(x - 1, y, x, y + height, ACCENT);
        context.fill(x + width, y, x + width + 1, y + height, ACCENT);
        context.fill(x + 1, y + 1, x + width - 1, y + height - 1, 0x48E8781E);
    }

    // ---- pixels -----------------------------------------------------------------------

    static void dot(GuiGraphics context, int x, int y, int colour) {
        context.fill(x, y, x + 1, y + 1, colour);
    }

    /** A filled circle of {@code radius} round the pixel at {@code cx, cy}. */
    static void disc(GuiGraphics context, int cx, int cy, int radius, int colour) {
        for (int dy = -radius; dy <= radius; dy++) {
            int half = (int) Math.floor(Math.sqrt((radius + 0.5) * (radius + 0.5) - dy * dy));
            context.fill(cx - half, cy + dy, cx + half + 1, cy + dy + 1, colour);
        }
    }

    /** A one-pixel line, stepped pixel by pixel so it stays pixel art. */
    static void line(GuiGraphics context, double x0, double y0, double x1, double y1, int colour) {
        beam(context, x0, y0, x1, y1, 1, colour);
    }

    /** A line {@code thickness} pixels thick, stepped pixel by pixel. */
    static void beam(GuiGraphics context, double x0, double y0, double x1, double y1, int thickness, int colour) {
        int steps = (int) Math.ceil(Math.max(Math.abs(x1 - x0), Math.abs(y1 - y0)) * 2);
        int back = thickness / 2;
        for (int step = 0; step <= steps; step++) {
            double t = steps == 0 ? 0 : (double) step / steps;
            int x = (int) Math.floor(x0 + (x1 - x0) * t) - back;
            int y = (int) Math.floor(y0 + (y1 - y0) * t) - back;
            context.fill(x, y, x + thickness, y + thickness, colour);
        }
    }

    static float clamp(float value) {
        return Math.min(1.0F, Math.max(0.0F, value));
    }

    static int darker(int colour) {
        return scale(colour, 0.7F);
    }

    static int lighter(int colour) {
        int r = colour >> 16 & 0xFF;
        int g = colour >> 8 & 0xFF;
        int b = colour & 0xFF;
        r += (255 - r) * 2 / 5;
        g += (255 - g) * 2 / 5;
        b += (255 - b) * 2 / 5;
        return colour & 0xFF000000 | r << 16 | g << 8 | b;
    }

    static int scale(int colour, float by) {
        int r = Math.min(255, (int) ((colour >> 16 & 0xFF) * by));
        int g = Math.min(255, (int) ((colour >> 8 & 0xFF) * by));
        int b = Math.min(255, (int) ((colour & 0xFF) * by));
        return colour & 0xFF000000 | r << 16 | g << 8 | b;
    }

    /** The colour with its alpha multiplied by {@code alpha}, for things drawn as a ghost. */
    static int fade(int colour, float alpha) {
        int a = (int) ((colour >>> 24) * alpha);
        return a << 24 | colour & 0xFFFFFF;
    }
}
