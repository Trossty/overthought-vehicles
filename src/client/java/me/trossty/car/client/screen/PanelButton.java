package me.trossty.car.client.screen;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.network.chat.Component;
import net.minecraft.util.FormattedCharSequence;

/**
 * A push button for the mod's windows, drawn like the rest of them from
 * rectangles: a steel key with rounded corners and a bevel, or, for the one
 * thing a window is for, a key in the window's own colour. A key that stands
 * for a choice can be held {@linkplain #setSelected selected}, shown pressed in.
 */
final class PanelButton extends Button {

    private static final int STEEL_FACE = 0xFF6E7884;
    private static final int DEAD_FACE = 0xFF50565D;

    private final int face;
    private boolean selected;

    private PanelButton(int x, int y, int width, int height, Component message, OnPress onPress, int face) {
        super(x, y, width, height, message, onPress, DEFAULT_NARRATION);
        this.face = face;
    }

    /** A plain steel key. */
    static PanelButton steel(int x, int y, int width, int height, Component message, OnPress onPress) {
        return new PanelButton(x, y, width, height, message, onPress, STEEL_FACE);
    }

    /** The key a window is for, in {@code colour}. */
    static PanelButton action(int x, int y, int width, int height, Component message, OnPress onPress, int colour) {
        return new PanelButton(x, y, width, height, message, onPress, colour);
    }

    /** Held in and rimmed in the workshop's orange: "this is the one", not "unavailable". */
    void setSelected(boolean selected) {
        this.selected = selected;
    }

    @Override
    protected void renderWidget(GuiGraphics context, int mouseX, int mouseY, float partialTick) {
        int x = this.getX();
        int y = this.getY();
        int w = this.width;
        int h = this.height;
        boolean down = this.selected;
        int body = !this.active ? DEAD_FACE : this.isHoveredOrFocused() ? GuiStyle.scale(this.face, 1.14F) : this.face;

        int rim = down ? GuiStyle.ACCENT : GuiStyle.OUTLINE;
        context.fill(x + 1, y, x + w - 1, y + h, rim);
        context.fill(x, y + 1, x + w, y + h - 1, rim);
        context.fillGradient(x + 1, y + 1, x + w - 1, y + h - 1,
                down ? GuiStyle.darker(body) : body, down ? body : GuiStyle.scale(body, 0.82F));
        int light = GuiStyle.lighter(body);
        int shade = GuiStyle.scale(body, 0.6F);
        context.fill(x + 1, y + 1, x + w - 1, y + 2, down ? shade : light);
        context.fill(x + 1, y + 2, x + 2, y + h - 2, down ? shade : light);
        context.fill(x + 1, y + h - 2, x + w - 1, y + h - 1, down ? light : shade);
        context.fill(x + w - 2, y + 2, x + w - 1, y + h - 2, down ? light : shade);
        if (this.active && this.isFocused()) {
            context.fill(x + 2, y + h - 3, x + w - 2, y + h - 2, 0x80FFFFFF);
        }

        Font font = Minecraft.getInstance().font;
        FormattedCharSequence line = GuiStyle.fit(font, this.getMessage(), w - 6);
        int tx = x + (w - font.width(line) + 1) / 2;
        int ty = y + (h - 7) / 2 + (down ? 1 : 0);
        context.drawString(font, line, tx, ty, this.active ? 0xFFFFFFFF : 0xFF9EA4AB, this.active);
    }
}
