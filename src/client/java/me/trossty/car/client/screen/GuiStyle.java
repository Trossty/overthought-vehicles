package me.trossty.car.client.screen;

import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.util.FormattedCharSequence;

/**
 * The look every window of the mod shares, drawn from rectangles so the mod
 * still ships no GUI texture: a grey panel with a dark header strip carrying
 * the title, an orange rule under it, sunken slots, and a dark read-out for
 * the one line of numbers a machine has to say.
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
    static final int HEADER_RULE = 0xFFE8781E;
    static final int TITLE = 0xFFF2F2F2;
    static final int TITLE_SOFT = 0xFFAEB6C2;
    /** The header strip's height; the title sits in it at {@link #TITLE_Y}. */
    static final int HEADER_HEIGHT = 13;
    static final int TITLE_Y = 3;

    static final int READOUT = 0xFF1C2229;
    static final int READOUT_TEXT = 0xFF8FE39A;
    static final int READOUT_HEIGHT = 11;

    static final int LAMP_ON = 0xFF3FC24A;
    static final int LAMP_OFF = 0xFFB0372B;

    private GuiStyle() {
    }

    /** The window itself: outline, panel, bevel and header. */
    static void frame(GuiGraphics context, int left, int top, int width, int height) {
        int right = left + width;
        int bottom = top + height;
        context.fill(left - 1, top - 1, right + 1, bottom + 1, OUTLINE);
        context.fill(left, top, right, bottom, PANEL);
        context.fill(left, top, right, top + 1, PANEL_LIGHT);
        context.fill(left, top, left + 1, bottom, PANEL_LIGHT);
        context.fill(left, bottom - 1, right, bottom, PANEL_SHADOW);
        context.fill(right - 1, top, right, bottom, PANEL_SHADOW);
        context.fill(left, top, right, top + HEADER_HEIGHT, HEADER);
        context.fill(left, top + HEADER_HEIGHT, right, top + HEADER_HEIGHT + 1, HEADER_RULE);
    }

    static void slot(GuiGraphics context, int x, int y) {
        context.fill(x, y, x + 18, y + 18, SLOT_BODY);
        context.fill(x, y, x + 18, y + 1, SLOT_DARK);
        context.fill(x, y, x + 1, y + 18, SLOT_DARK);
        context.fill(x + 17, y + 1, x + 18, y + 18, PANEL_LIGHT);
        context.fill(x + 1, y + 17, x + 18, y + 18, PANEL_LIGHT);
    }

    /** The title in the header, cut to leave {@code room} pixels. */
    static void title(GuiGraphics context, Font font, Component title, int x, int room) {
        context.drawString(font, fit(font, title, room), x, TITLE_Y, TITLE, false);
    }

    /**
     * A lamp and a word at the right of the header, ending at {@code right}.
     *
     * @return where the lamp starts, so the title can stop short of it
     */
    static int lamp(GuiGraphics context, Font font, boolean on, int right) {
        Component state = Component.translatable(on ? "gui.car.machine.running" : "gui.car.machine.idle");
        int x = right - font.width(state);
        context.fill(x - 8, TITLE_Y + 1, x - 3, TITLE_Y + 6, on ? LAMP_ON : LAMP_OFF);
        context.drawString(font, state, x, TITLE_Y, on ? TITLE : TITLE_SOFT, false);
        return x - 8;
    }

    /** A dark read-out box with one line of figures centred in it. */
    static void readout(GuiGraphics context, Font font, Component text, int x, int y, int width) {
        context.fill(x - 1, y - 1, x + width + 1, y + READOUT_HEIGHT + 1, SLOT_DARK);
        context.fill(x, y, x + width + 1, y + READOUT_HEIGHT + 1, PANEL_LIGHT);
        context.fill(x, y, x + width, y + READOUT_HEIGHT, READOUT);
        FormattedCharSequence line = fit(font, text, width - 6);
        context.drawString(font, line, x + (width - font.width(line)) / 2, y + 2, READOUT_TEXT, false);
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
}
